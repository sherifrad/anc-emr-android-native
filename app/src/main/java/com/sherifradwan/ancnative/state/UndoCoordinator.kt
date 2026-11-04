package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.storage.ClinicalCommand
import com.sherifradwan.ancnative.storage.ClinicalCommandFailure
import com.sherifradwan.ancnative.storage.ClinicalCommandOutcome
import com.sherifradwan.ancnative.storage.ClinicalCommandReceipt
import com.sherifradwan.ancnative.storage.DeletableRecord
import com.sherifradwan.ancnative.storage.DeletableRegistry
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.storage.DeleteRecordCommand
import com.sherifradwan.ancnative.storage.RecordSnapshot
import com.sherifradwan.ancnative.storage.RecordUndoCommand
import com.sherifradwan.ancnative.storage.RestoreRecordCommand
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.storage.FinalizeResult
import com.sherifradwan.ancnative.storage.contentHash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class UndoCoordinator(
    private val scope: CoroutineScope,
    private val execute: suspend (ClinicalCommand, Long) -> ClinicalCommandOutcome,
    /** Reducer-only: applies the coordinator's own events to state. Never routes through the ViewModel's dispatch. */
    private val reduce: (AncEvent) -> Unit,
    private val currentState: () -> AncState,
    /** The storage snapshot (not the UI state), used to read the fresh patient revision on a stale retry. */
    private val snapshot: () -> RecordSnapshot?,
    private val finalizePayload: suspend (DeletableType, DeletableRecord) -> FinalizeResult = { _, _ -> FinalizeResult.Success },
) {
    private data class PendingUndo(
        val token: String,
        val command: ClinicalCommand,
        val ambiguous: Boolean = false,
        val failures: Int = 0,
        val epoch: Long,
        val type: DeletableType? = null,
        val record: DeletableRecord? = null,
    )

    private var pendingUndo: PendingUndo? = null
    private var undoJob: Job? = null

    private fun sessionStillCurrent(epoch: Long): Boolean =
        (currentState().vaultState as? VaultState.Unlocked)?.sessionEpoch == epoch

    /**
     * Removes the payload file of a delete that will never be undone. Every exit path of a committed delete calls this
     * one function, and every path reports a failure: the record is already gone, so a leftover file must be visible.
     */
    private fun cleanupPending(pending: PendingUndo) {
        val type = pending.type ?: return
        val record = pending.record ?: return
        // A record of another kind can never be cleaned through this type: refuse instead of deleting the wrong file.
        val label = DeletableRegistry.labelOrNull(type, record) ?: return
        val patientId = pending.command.patientId
        val patientName = currentState().patient(patientId)?.displayName?.trim().orEmpty()
        scope.launch {
            val result = try {
                finalizePayload(type, record)
            } catch (failure: Throwable) {
                FinalizeResult.Failed(failure.message ?: "cleanup threw")
            }
            if (result is FinalizeResult.Failed) {
                reduce(
                    AncEvent.PayloadCleanupFailed(
                        token = pending.token,
                        error = SaveUiError.STORAGE_ERROR,
                        patientId = patientId,
                        type = type,
                        label = label,
                        patientName = patientName,
                    ),
                )
            }
        }
    }

    private fun startUndoExpiry(token: String) {
        undoJob?.cancel()
        undoJob = scope.launch {
            delay(8_000L)
            val expired = pendingUndo?.takeIf { it.token == token }
            if (expired != null) {
                cleanupPending(expired)
                pendingUndo = null
            }
            undoJob = null
            reduce(AncEvent.UndoExpired(token))
        }
    }

    private fun armUndo(token: String, command: ClinicalCommand, type: DeletableType? = null, record: DeletableRecord? = null) {
        val epoch = (currentState().vaultState as? VaultState.Unlocked)?.sessionEpoch ?: return
        // If a previous pending undo exists, clean up its payload before replacing it.
        pendingUndo?.let { cleanupPending(it) }
        pendingUndo = PendingUndo(token, command, epoch = epoch, type = type, record = record)
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
        reduce(AncEvent.DeleteStarted)
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
                    armUndo(token, restoreCmd, type = type, record = record)
                    val offer = UndoOffer(
                        token = token,
                        patientId = patient.id,
                        type = type,
                        action = UndoAction.DELETE,
                        label = adapter.label(record),
                        phase = UndoPhase.READY,
                        patientName = patient.displayName.trim(),
                        recordId = record.id,
                    )
                    reduce(
                        AncEvent.DeleteCommitted(
                            patients = result.snapshot.patients,
                            offer = offer,
                            notice = adapter.deletedNotice,
                            patientId = patient.id,
                        ),
                    )
                }
                is ClinicalCommandOutcome.Rejected -> {
                    reduce(AncEvent.DeleteFailed(result.failure.toUiError(), patient.id, type, adapter.label(record), patient.displayName))
                }
                ClinicalCommandOutcome.Ambiguous,
                ClinicalCommandOutcome.Obsolete -> {
                    reduce(AncEvent.DeleteFailed(SaveUiError.STORAGE_ERROR, patient.id, type, adapter.label(record), patient.displayName))
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
        if (state.vaultState !is VaultState.Unlocked) return
        val adapter = DeletableRegistry.of<DeletableRecord>(type)
        val patientName = state.patient(patientId)?.displayName?.trim().orEmpty()
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
            patientName = patientName,
            recordId = recordId,
        )
        armUndo(token, undoDelete)
        reduce(AncEvent.UndoOffered(offer))
    }

    fun request(token: String) {
        val state = currentState()
        if (state.vaultState !is VaultState.Unlocked) return
        if (!state.canRequestUndo(token)) return
        val offer = state.undoOffer ?: return
        val pending = pendingUndo?.takeIf { it.token == token } ?: return
        val epoch = pending.epoch
        if (!sessionStillCurrent(epoch)) return
        reduce(AncEvent.UndoRequested(token))
        // The reducer refuses (and says why) when the record is being edited with unsaved changes.
        if (!currentState().canRunUndo(token)) return
        undoJob?.cancel()
        undoJob = null

        scope.launch {
            val currentPatient = currentState().patient(pending.command.patientId)
            if (currentPatient == null) {
                if (!sessionStillCurrent(epoch)) return@launch
                failUndo(token, offer, SaveUiError.STORAGE_ERROR, pending.command, pending.ambiguous, pending.failures)
                return@launch
            }
            var cmd = if (pending.ambiguous) {
                pending.command
            } else {
                (pending.command as? RecordUndoCommand)?.withPatientRevision(currentPatient.revision) ?: pending.command
            }
            var result = execute(cmd, epoch)
            if (!pending.ambiguous && result is ClinicalCommandOutcome.Rejected && result.failure == ClinicalCommandFailure.STALE_PATIENT_REVISION) {
                val freshPatient = snapshot()?.patients?.firstOrNull { it.id == cmd.patientId }
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
                    reduce(AncEvent.UndoCommitted(token = token, patients = result.snapshot.patients, notice = notice, patientId = offer.patientId))
                }
                is ClinicalCommandOutcome.Rejected -> {
                    failUndo(token, offer, result.failure.toUiError(), cmd, ambiguous = false, failures = pending.failures)
                }
                ClinicalCommandOutcome.Ambiguous -> {
                    failUndo(token, offer, SaveUiError.STORAGE_ERROR, cmd, ambiguous = true, failures = pending.failures)
                }
                ClinicalCommandOutcome.Obsolete -> {
                    failUndo(token, offer, SaveUiError.STORAGE_ERROR, cmd, ambiguous = false, failures = pending.failures)
                }
            }
        }
    }

    private fun failUndo(token: String, offer: UndoOffer, error: SaveUiError, attempted: ClinicalCommand, ambiguous: Boolean, failures: Int) {
        reduce(AncEvent.UndoFailed(token, error, offer))
        val pending = pendingUndo?.takeIf { it.token == token }
        if (pending == null) return
        if (currentState().undoOffer?.token != token) {
            pendingUndo = null
            return
        }
        // A failed Undo may restart its window at most MAX_UNDO_RESTARTS times.
        if (undoMayRestart(failures)) {
            pendingUndo = pending.copy(command = attempted, ambiguous = ambiguous, failures = failures + 1)
            startUndoExpiry(token)
        } else {
            cleanupPending(pending)
            pendingUndo = null
            reduce(AncEvent.UndoExpired(token))
        }
    }

    fun clear() {
        undoJob?.cancel()
        undoJob = null
        pendingUndo?.let { cleanupPending(it) }
        pendingUndo = null
    }
}
