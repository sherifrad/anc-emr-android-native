package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.storage.ClinicalCommand
import com.sherifradwan.ancnative.storage.ClinicalCommandFailure
import com.sherifradwan.ancnative.storage.ClinicalCommandOutcome
import com.sherifradwan.ancnative.storage.ClinicalCommandReceipt
import com.sherifradwan.ancnative.storage.DeletableRecord
import com.sherifradwan.ancnative.storage.DeletableRegistry
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.storage.DeleteRecordCommand
import com.sherifradwan.ancnative.storage.RecordUndoCommand
import com.sherifradwan.ancnative.storage.RestoreRecordCommand
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.storage.contentHash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class UndoCoordinator(
    private val scope: CoroutineScope,
    private val execute: suspend (ClinicalCommand, Long) -> ClinicalCommandOutcome,
    private val dispatch: (AncEvent) -> Unit,
    private val currentState: () -> AncState,
) {
    private data class PendingUndo(
        val token: String,
        val command: ClinicalCommand,
        val ambiguous: Boolean = false,
        val failures: Int = 0,
    )

    private var pendingUndo: PendingUndo? = null
    private var undoJob: Job? = null

    private fun sessionStillCurrent(epoch: Long): Boolean =
        (currentState().vaultState as? VaultState.Unlocked)?.sessionEpoch == epoch

    private fun startUndoExpiry(token: String) {
        undoJob?.cancel()
        undoJob = scope.launch {
            delay(8_000L)
            if (pendingUndo?.token == token) pendingUndo = null
            undoJob = null
            dispatch(AncEvent.UndoExpired(token))
        }
    }

    private fun armUndo(token: String, command: ClinicalCommand) {
        pendingUndo = PendingUndo(token, command)
        startUndoExpiry(token)
    }

    fun beginDelete(type: DeletableType, patientId: String, recordId: String) {
        val state = currentState()
        val unlocked = state.vaultState as? VaultState.Unlocked ?: return
        val epoch = unlocked.sessionEpoch
        val patient = state.patient(patientId) ?: return
        val adapter = DeletableRegistry.of<DeletableRecord>(type)
        val record = adapter.record(patient, recordId) ?: return
        val cmd = DeleteRecordCommand(
            type = type,
            operationUuid = UUID.randomUUID().toString(),
            patientId = patient.id,
            expectedPatientRevision = patient.revision,
            recordId = record.id,
            expectedRecordRevision = record.revision,
            deletedContentHash = contentHash(record),
        )
        scope.launch {
            if (!sessionStillCurrent(epoch)) return@launch
            val result = execute(cmd, epoch)
            if (!sessionStillCurrent(epoch)) return@launch
            when (result) {
                is ClinicalCommandOutcome.Committed -> {
                    val token = UUID.randomUUID().toString()
                    val restoreCmd = RestoreRecordCommand(
                        type = type,
                        operationUuid = UUID.randomUUID().toString(),
                        patientId = patient.id,
                        expectedPatientRevision = result.receipt.committedPatientRevision,
                        record = record,
                        sourceDelete = cmd,
                    )
                    armUndo(token, restoreCmd)
                    val offer = UndoOffer(
                        token = token,
                        patientId = patient.id,
                        type = type,
                        action = UndoAction.DELETE,
                        label = adapter.label(record),
                        phase = UndoPhase.READY,
                    )
                    dispatch(
                        AncEvent.DeleteCommitted(
                            patients = result.snapshot.patients,
                            offer = offer,
                            notice = adapter.deletedNotice,
                        ),
                    )
                }
                is ClinicalCommandOutcome.Rejected -> {
                    dispatch(AncEvent.DeleteFailed(result.failure.toUiError()))
                }
                ClinicalCommandOutcome.Ambiguous,
                ClinicalCommandOutcome.Obsolete -> {
                    dispatch(AncEvent.DeleteFailed(SaveUiError.STORAGE_ERROR))
                }
            }
        }
    }

    fun offerSaveUndo(
        type: DeletableType,
        patientId: String,
        recordId: String,
        receipt: ClinicalCommandReceipt,
        savedRecord: DeletableRecord,
    ) {
        val state = currentState()
        val unlocked = state.vaultState as? VaultState.Unlocked ?: return
        val adapter = DeletableRegistry.of<DeletableRecord>(type)
        val token = UUID.randomUUID().toString()
        val undoDelete = DeleteRecordCommand(
            type = type,
            editorSessionId = "",
            operationUuid = UUID.randomUUID().toString(),
            patientId = patientId,
            expectedPatientRevision = receipt.committedPatientRevision,
            recordId = recordId,
            expectedRecordRevision = receipt.committedRevision,
            deletedContentHash = contentHash(savedRecord),
        )
        val offer = UndoOffer(
            token = token,
            patientId = patientId,
            type = type,
            action = UndoAction.SAVE,
            label = adapter.label(savedRecord),
            phase = UndoPhase.READY,
        )
        armUndo(token, undoDelete)
        dispatch(
            AncEvent.DeleteCommitted(
                patients = state.patients,
                offer = offer,
                notice = null,
            ),
        )
    }

    fun request(token: String) {
        val state = currentState()
        val unlocked = state.vaultState as? VaultState.Unlocked ?: return
        val epoch = unlocked.sessionEpoch
        if (!state.canRequestUndo(token)) return
        val offer = state.undoOffer ?: return
        val pending = pendingUndo?.takeIf { it.token == token } ?: return
        undoJob?.cancel()
        undoJob = null
        dispatch(AncEvent.UndoRequested(token))

        scope.launch {
            val currentPatient = currentState().patient(pending.command.patientId)
            if (currentPatient == null) {
                failUndo(token, SaveUiError.STORAGE_ERROR, pending.command, pending.ambiguous, pending.failures)
                return@launch
            }
            var cmd = if (pending.ambiguous) {
                pending.command
            } else {
                (pending.command as? RecordUndoCommand)?.withPatientRevision(currentPatient.revision) ?: pending.command
            }
            var result = execute(cmd, epoch)
            if (!pending.ambiguous && result is ClinicalCommandOutcome.Rejected && result.failure == ClinicalCommandFailure.STALE_PATIENT_REVISION) {
                val freshPatient = currentState().patient(cmd.patientId)
                if (freshPatient != null && freshPatient.revision != cmd.expectedPatientRevision) {
                    cmd = (cmd as? RecordUndoCommand)?.withPatientRevision(freshPatient.revision) ?: cmd
                    result = execute(cmd, epoch)
                }
            }
            if (!sessionStillCurrent(epoch)) return@launch
            when (result) {
                is ClinicalCommandOutcome.Committed -> {
                    if (pendingUndo?.token == token) {
                        pendingUndo = null
                        undoJob?.cancel()
                        undoJob = null
                    }
                    val adapter = DeletableRegistry.of<DeletableRecord>(offer.type)
                    val notice = if (offer.action == UndoAction.SAVE) adapter.saveUndoneNotice else adapter.restoredNotice
                    dispatch(AncEvent.UndoCommitted(token = token, patients = result.snapshot.patients, notice = notice))
                }
                is ClinicalCommandOutcome.Rejected -> {
                    failUndo(token, result.failure.toUiError(), cmd, ambiguous = false, failures = pending.failures)
                }
                ClinicalCommandOutcome.Ambiguous -> {
                    failUndo(token, SaveUiError.STORAGE_ERROR, cmd, ambiguous = true, failures = pending.failures)
                }
                ClinicalCommandOutcome.Obsolete -> {
                    failUndo(token, SaveUiError.STORAGE_ERROR, cmd, ambiguous = false, failures = pending.failures)
                }
            }
        }
    }

    private fun failUndo(token: String, error: SaveUiError, attempted: ClinicalCommand, ambiguous: Boolean, failures: Int) {
        dispatch(AncEvent.UndoFailed(token, error))
        val pending = pendingUndo?.takeIf { it.token == token }
        if (pending == null || currentState().undoOffer?.token != token) return
        // A failed Undo may restart its window at most MAX_UNDO_RESTARTS times.
        if (undoMayRestart(failures)) {
            pendingUndo = pending.copy(command = attempted, ambiguous = ambiguous, failures = failures + 1)
            startUndoExpiry(token)
        } else {
            pendingUndo = null
            dispatch(AncEvent.UndoExpired(token))
        }
    }

    fun clear() {
        undoJob?.cancel()
        undoJob = null
        pendingUndo = null
    }
}
