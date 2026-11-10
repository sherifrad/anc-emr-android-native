package com.sherifradwan.ancnative.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sherifradwan.ancnative.storage.AuthenticationRequest
import com.sherifradwan.ancnative.storage.ChangePatientStatusCommand
import com.sherifradwan.ancnative.storage.PatientCommand
import com.sherifradwan.ancnative.storage.PatientCommandFailure
import com.sherifradwan.ancnative.storage.ClinicalCommand
import com.sherifradwan.ancnative.storage.ClinicalCommandFailure
import com.sherifradwan.ancnative.storage.ClinicalCommandOutcome
import com.sherifradwan.ancnative.storage.ClinicalCommandPreparation
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.storage.ClinicalCommandReceipt
import com.sherifradwan.ancnative.storage.ClinicalRecordKind
import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.PatientCommandOutcome
import com.sherifradwan.ancnative.storage.PatientCommandPreparation
import com.sherifradwan.ancnative.storage.SaveDatingCommand
import com.sherifradwan.ancnative.storage.SaveFailure
import com.sherifradwan.ancnative.storage.SaveLabInvestigationCommand
import com.sherifradwan.ancnative.storage.SaveMedicationCommand
import com.sherifradwan.ancnative.storage.SaveOtherHistoriesCommand
import com.sherifradwan.ancnative.storage.SaveOutcome
import com.sherifradwan.ancnative.storage.SavePatientProfileCommand
import com.sherifradwan.ancnative.storage.SavePreparation
import com.sherifradwan.ancnative.storage.SavePreviousPregnancyCommand
import com.sherifradwan.ancnative.storage.SaveUltrasoundScanCommand
import com.sherifradwan.ancnative.storage.SaveVisitCommand
import com.sherifradwan.ancnative.storage.VaultController
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.storage.DeletableType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class AncViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = VaultController.get(application)
    private val _state = MutableStateFlow(
        AncState(patients = emptyList(), vaultState = VaultState.Inspecting),
    )
    val state: StateFlow<AncState> = _state.asStateFlow()

    private var frozenVisitCommand: SaveVisitCommand? = null
    private var frozenPatientCommand: PatientCommand? = null
    private var frozenClinicalCommand: ClinicalCommand? = null

    private data class PendingChainedLab(
        val operationUuid: String,
        val editorSessionId: String,
        val date: String,
        val labName: String,
    )

    private data class PendingChainedMedication(
        val operationUuid: String,
        val editorSessionId: String,
        val startDate: String,
    )

    private var pendingChainedLab: PendingChainedLab? = null
    private var pendingChainedMedication: PendingChainedMedication? = null

    private val undoCoordinator = UndoCoordinator(
        scope = viewModelScope,
        execute = { cmd, epoch -> controller.execute(cmd, epoch) {} },
        reduce = { event -> updateState { AncReducer.reduce(it, event) } },
        currentState = { _state.value },
        snapshot = { controller.view.value.snapshot },
        finalizePayload = { type, record ->
            val ctx = getApplication<android.app.Application>()
            val adapter = com.sherifradwan.ancnative.storage.DeletableRegistry.of<com.sherifradwan.ancnative.storage.DeletableRecord>(type) as? com.sherifradwan.ancnative.storage.PayloadFileAdapter
            if (adapter != null && adapter.hasPayloadFile) {
                adapter.finalizeDelete(record, ctx)
            } else com.sherifradwan.ancnative.storage.FinalizeResult.Success
        },
    )

    private fun sessionStillCurrent(epoch: Long): Boolean =
        (_state.value.vaultState as? VaultState.Unlocked)?.sessionEpoch == epoch

    private val draftStore = object : DraftStore {
        override suspend fun save(record: DraftRecord): Boolean = controller.saveDraft(record)
        override suspend fun delete(slot: DraftSlot) {
            controller.deleteDraft(slot.patientId, slot.kind)
        }
        override suspend fun load(slot: DraftSlot): DraftRecord? =
            controller.loadDraft(slot.patientId, slot.kind)
    }
    internal val autosaver = DraftAutosaver(viewModelScope, draftStore)
    private val lifecycleMutex = Mutex()
    private var isAcceptingRecovery = false
    internal var recoveredSessionId: String? = null

    private inline fun updateState(crossinline transform: (AncState) -> AncState) {
        var before: AncState? = null
        var after: AncState? = null
        _state.update { current ->
            before = current
            val updated = transform(current)
            after = updated
            updated
        }
        val b = before
        val a = after
        if (b != null && a != null && b !== a) {
            if (isAcceptingRecovery) {
                recoveredSessionId = activeEditorSessionId(a)
                isAcceptingRecovery = false
            }
            if (a.undoOffer == null) undoCoordinator.clear()
            autosaver.onStateChanged(b, a)
            checkEditorSessionChanged(b, a)
        }
    }

    private fun checkEditorSessionChanged(before: AncState, after: AncState) {
        val beforeSession = activeEditorSessionId(before)
        val afterSession = activeEditorSessionId(after)
        if (afterSession != null && afterSession != beforeSession) {
            if (afterSession == recoveredSessionId) {
                recoveredSessionId = null
                return
            }
            val slot = activeDraftSlot(after) ?: return
            autosaver.holdSlot(slot, afterSession)
            viewModelScope.launch {
                val record = try {
                    autosaver.load(slot)
                } catch (_: Throwable) {
                    null
                }
                val stateAfterLoad = _state.value
                val currentSessionId = activeEditorSessionId(stateAfterLoad)
                val currentSlot = activeDraftSlot(stateAfterLoad)
                if (record != null && currentSessionId == afterSession && currentSlot == slot &&
                    draftRecordBelongsToOpenEditor(stateAfterLoad, record)
                ) {
                    val isStale = isDraftStale(record, activeEditorBaseRevision(stateAfterLoad))
                    val prompt = DraftRecoveryPrompt(
                        slot = slot,
                        editorSessionId = afterSession,
                        record = record,
                        isStale = isStale,
                    )
                    updateState { AncReducer.reduce(it, AncEvent.DraftRecoveryFound(prompt)) }
                    if (_state.value.draftRecoveryPrompt == null) {
                        autosaver.releaseSlot(slot, afterSession)
                    }
                } else {
                    autosaver.releaseSlot(slot, afterSession)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            controller.view.collectLatest { sessionView ->
                val vaultState = sessionView.state
                val snapshot = sessionView.snapshot
                if (vaultState is VaultState.Unlocked && snapshot != null) {
                    _state.update { current ->
                        current.copy(patients = snapshot.patients, vaultState = vaultState)
                    }
                } else {
                    clearFrozenCommands()
                    _state.value = AncState(patients = emptyList(), vaultState = vaultState)
                    autosaver.cancelPending()
                }
            }
        }
    }

    fun dispatch(event: AncEvent) {
        if (_state.value.vaultState !is VaultState.Unlocked) return
        when (event) {
            AncEvent.SubmitPatientProfile -> savePatientProfile()
            is AncEvent.RegistrationNext -> registrationNext(event)
            AncEvent.SubmitOtherHistories -> saveOtherHistories()
            AncEvent.SubmitPreviousPregnancy -> savePreviousPregnancy()
            AncEvent.SubmitPatientStatus -> savePatientStatus()
            AncEvent.SubmitDating -> saveDating()
            AncEvent.SubmitUltrasound -> saveUltrasound()
            AncEvent.SubmitLab -> saveLab(isChained = false)
            AncEvent.SubmitLabBatch -> saveLabBatch()
            is AncEvent.StartLabBatchRequest -> startLabBatch(
                patientId = event.patientId,
                date = event.date,
                labName = event.labName,
            )
            AncEvent.SubmitLabAndAddAnother -> saveLab(isChained = true)
            AncEvent.SubmitMedication -> {
                if (!_state.value.undoRunning) {
                    saveMedication(isChained = false)
                }
            }
            AncEvent.SubmitMedicationAndAddAnother -> {
                if (!_state.value.undoRunning) {
                    saveMedication(isChained = true)
                }
            }
            AncEvent.ConfirmDelete -> confirmDelete()
            is AncEvent.UndoRequested -> requestUndo(event.token)
            is AncEvent.AddAttachment -> addAttachment(event)
            is AncEvent.ImportScanPhoto -> importScanPhoto(event)
            is AncEvent.AcceptDraftRecovery -> {
                val prompt = _state.value.draftRecoveryPrompt
                val slot = prompt?.slot
                val oldSession = prompt?.editorSessionId
                isAcceptingRecovery = true
                updateState { AncReducer.reduce(it, event) }
                val after = _state.value
                if (slot != null && oldSession != null) {
                    if (!after.hasDirtyDraft) {
                        viewModelScope.launch {
                            try {
                                controller.deleteDraft(slot.patientId, slot.kind)
                            } finally {
                                autosaver.releaseSlot(slot, oldSession)
                            }
                        }
                    } else {
                        autosaver.releaseSlot(slot, oldSession)
                    }
                }
            }
            is AncEvent.DiscardDraftRecovery -> {
                val prompt = _state.value.draftRecoveryPrompt
                val slot = prompt?.slot
                val session = prompt?.editorSessionId
                updateState { AncReducer.reduce(it, event) }
                if (slot != null && session != null) {
                    viewModelScope.launch {
                        try {
                            controller.deleteDraft(slot.patientId, slot.kind)
                        } finally {
                            autosaver.releaseSlot(slot, session)
                        }
                    }
                }
            }
            else -> {
                if (event.clearsFrozenVisit()) frozenVisitCommand = null
                if (event.clearsFrozenPatient()) frozenPatientCommand = null
                if (event.clearsFrozenClinical()) {
                    frozenClinicalCommand = null
                    pendingChainedLab = null
                    pendingChainedMedication = null
                }
                updateState { AncReducer.reduce(it, event) }
            }
        }
    }

    /** Deletes leftover camera files older than an hour that no saved scan (or open draft) still points at. */
    private fun sweepStaleCaptures() {
        val current = _state.value
        val referenced = current.patients.flatMap { p -> p.ultrasoundScans.flatMap { it.imageUris } }.toSet() +
            current.ultrasoundDraft?.values?.imageUris.orEmpty()
        com.sherifradwan.ancnative.storage.CameraCaptureHelper.clearStaleCaptures(getApplication(), 60L * 60 * 1000, referenced)
    }

    private fun importScanPhoto(event: AncEvent.ImportScanPhoto) {
        if (_state.value.vaultState !is VaultState.Unlocked) {
            com.sherifradwan.ancnative.storage.CameraCaptureHelper.deleteIfCaptureTemp(getApplication(), event.sourceUri)
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                sweepStaleCaptures()
                val saved = com.sherifradwan.ancnative.storage.PatientAttachmentVault.saveAttachment(
                    context = getApplication(),
                    patientId = event.patientId,
                    sourceUri = event.sourceUri,
                    preset = event.preset,
                )
                dispatch(AncEvent.ScanPhotoImported(event.patientId, com.sherifradwan.ancnative.storage.scanPhotoRef(saved.fileName)))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("AncViewModel", "Failed to import scan photo", e)
                dispatch(AncEvent.AttachmentFailed(AttachmentError.SAVE_FAILED))
            } finally {
                com.sherifradwan.ancnative.storage.CameraCaptureHelper.deleteIfCaptureTemp(getApplication(), event.sourceUri)
            }
        }
    }

    private fun addAttachment(event: AncEvent.AddAttachment) {
        val unlocked = _state.value.vaultState as? VaultState.Unlocked
        if (unlocked == null) {
            com.sherifradwan.ancnative.storage.CameraCaptureHelper.deleteIfCaptureTemp(getApplication(), event.sourceUri)
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                sweepStaleCaptures()
                val savedResult = com.sherifradwan.ancnative.storage.PatientAttachmentVault.saveAttachment(
                    context = getApplication(),
                    patientId = event.patientId,
                    sourceUri = event.sourceUri,
                    preset = event.preset,
                )
                val demoAttachment = com.sherifradwan.ancnative.model.DemoAttachment(
                    id = savedResult.attachmentId,
                    patientId = savedResult.patientId,
                    fileName = savedResult.fileName,
                    filePath = savedResult.relativePath,
                    category = event.category,
                    mimeType = savedResult.mimeType,
                    fileSizeBytes = savedResult.fileSizeBytes,
                    sha256Hash = savedResult.sha256Hash,
                    title = event.title,
                    capturedAt = savedResult.capturedAt,
                    linkedRecordId = event.linkedRecordId,
                    syncStatus = com.sherifradwan.ancnative.storage.AttachmentSyncStatus.LOCAL_ONLY,
                )
                if (!controller.saveAttachment(demoAttachment, unlocked.sessionEpoch)) {
                    com.sherifradwan.ancnative.storage.PatientAttachmentVault.deleteAttachment(
                        context = getApplication(),
                        patientId = savedResult.patientId,
                        fileName = savedResult.fileName,
                    )
                    dispatch(AncEvent.AttachmentFailed(AttachmentError.SAVE_FAILED))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("AncViewModel", "Failed to save attachment", e)
                dispatch(AncEvent.AttachmentFailed(AttachmentError.SAVE_FAILED))
            } finally {
                com.sherifradwan.ancnative.storage.CameraCaptureHelper.deleteIfCaptureTemp(getApplication(), event.sourceUri)
            }
        }
    }

    /**
     * Runs the current editor's own save, but only when the reducer accepted Next (it recorded the advance for this
     * editor session). A Next that was ignored, or that already moved the flow, saves nothing.
     */
    private fun registrationNext(event: AncEvent.RegistrationNext) {
        updateState { AncReducer.reduce(it, event) }
        val after = _state.value
        val waiting = after.awaitingAdvance ?: return
        if (waiting != AwaitingAdvance(activeEditorSessionId(after) ?: return, FlowIds(event.editorSessionId, event.newPregnancyId))) return
        when {
            after.patientDraft != null -> savePatientProfile()
            after.datingDraft != null -> saveDating()
            after.historiesDraft != null -> saveOtherHistories()
            after.pregnancyDraft != null -> savePreviousPregnancy()
        }
    }

    fun startVisit(patientId: String, visitId: String? = null) {
        frozenVisitCommand = null
        dispatch(AncEvent.StartVisit(patientId, visitId, UUID.randomUUID().toString()))
    }

    fun saveVisit() {
        val current = _state.value
        val unlocked = current.vaultState as? VaultState.Unlocked ?: return
        val frozen = frozenVisitCommand?.takeIf { it.editorSessionId == current.draft?.editorSessionId }
        val operationUuid = frozen?.operationUuid ?: UUID.randomUUID().toString()
        val newVisitId = frozen?.visitId ?: UUID.randomUUID().toString()
        when (val prepared = SaveVisitCommand.prepare(current, operationUuid, newVisitId)) {
            is SavePreparation.Rejected -> dispatch(AncEvent.SaveValidationFailed(prepared.errors))
            is SavePreparation.Ready -> {
                val command = frozen?.takeIf { it.fingerprint == prepared.command.fingerprint } ?: prepared.command
                frozenVisitCommand = command
                submitVisit(command, unlocked.sessionEpoch)
            }
        }
    }

    private fun savePatientProfile() {
        val current = _state.value
        val unlocked = current.vaultState as? VaultState.Unlocked ?: return
        val frozen = frozenPatientCommand as? SavePatientProfileCommand
        val same = frozen?.takeIf { it.editorSessionId == current.patientDraft?.editorSessionId }
        val timestamp = same?.createdAt ?: Instant.now().toString()
        when (val prepared = SavePatientProfileCommand.prepare(
            current,
            same?.operationUuid ?: UUID.randomUUID().toString(),
            timestamp,
            forceAgeRequired = current.forcesAgeRequired(),
        )) {
            is PatientCommandPreparation.Rejected -> dispatch(
                AncEvent.PatientValidationFailed(PatientEditorKind.PROFILE, prepared.errors),
            )
            is PatientCommandPreparation.Ready -> submitPreparedPatientCommand(
                existing = same,
                prepared = prepared.command,
                sessionEpoch = unlocked.sessionEpoch,
            )
        }
    }

    private fun saveOtherHistories() {
        val current = _state.value
        val unlocked = current.vaultState as? VaultState.Unlocked ?: return
        val frozen = frozenPatientCommand as? SaveOtherHistoriesCommand
        val same = frozen?.takeIf { it.editorSessionId == current.historiesDraft?.editorSessionId }
        when (val prepared = SaveOtherHistoriesCommand.prepare(
            current,
            same?.operationUuid ?: UUID.randomUUID().toString(),
        )) {
            is PatientCommandPreparation.Rejected -> dispatch(
                AncEvent.PatientValidationFailed(PatientEditorKind.HISTORIES, prepared.errors),
            )
            is PatientCommandPreparation.Ready -> submitPreparedPatientCommand(
                existing = same,
                prepared = prepared.command,
                sessionEpoch = unlocked.sessionEpoch,
            )
        }
    }

    private fun savePreviousPregnancy() {
        val current = _state.value
        val unlocked = current.vaultState as? VaultState.Unlocked ?: return
        val frozen = frozenPatientCommand as? SavePreviousPregnancyCommand
        val same = frozen?.takeIf { it.editorSessionId == current.pregnancyDraft?.editorSessionId }
        val timestamp = same?.updatedAt ?: Instant.now().toString()
        when (val prepared = SavePreviousPregnancyCommand.prepare(
            current,
            same?.operationUuid ?: UUID.randomUUID().toString(),
            timestamp,
        )) {
            is PatientCommandPreparation.Rejected -> dispatch(
                AncEvent.PatientValidationFailed(PatientEditorKind.PREGNANCY, prepared.errors),
            )
            is PatientCommandPreparation.Ready -> submitPreparedPatientCommand(
                existing = same,
                prepared = prepared.command,
                sessionEpoch = unlocked.sessionEpoch,
            )
        }
    }

    private fun savePatientStatus() {
        val current = _state.value
        val unlocked = current.vaultState as? VaultState.Unlocked ?: return
        val frozen = frozenPatientCommand as? ChangePatientStatusCommand
        val same = frozen?.takeIf { it.editorSessionId == current.statusDraft?.editorSessionId }
        val timestamp = same?.changedAt ?: Instant.now().toString()
        when (val prepared = ChangePatientStatusCommand.prepare(
            current,
            same?.operationUuid ?: UUID.randomUUID().toString(),
            timestamp,
        )) {
            is PatientCommandPreparation.Rejected -> dispatch(
                AncEvent.PatientValidationFailed(PatientEditorKind.STATUS, prepared.errors),
            )
            is PatientCommandPreparation.Ready -> submitPreparedPatientCommand(
                existing = same,
                prepared = prepared.command,
                sessionEpoch = unlocked.sessionEpoch,
            )
        }
    }

    private fun submitPreparedPatientCommand(
        existing: PatientCommand?,
        prepared: PatientCommand,
        sessionEpoch: Long,
    ) {
        val command = existing?.takeIf { it.fingerprint == prepared.fingerprint } ?: prepared
        frozenPatientCommand = command
        val token = PendingSaveToken(command.operationUuid, command.editorSessionId)
        dispatch(AncEvent.SaveStarted(token))
        if (_state.value.pendingSave != token) return
        viewModelScope.launch {
            val result = controller.execute(command, sessionEpoch) {
                updateState { AncReducer.reduce(it, AncEvent.SaveReconciling(token)) }
            }
            when (result) {
                is PatientCommandOutcome.Committed -> {
                    frozenPatientCommand = null
                    val patients = result.snapshot.patients
                    val notice = command.savedNotice()
                    updateState {
                        AncReducer.reduce(
                            it,
                            AncEvent.RecordCommitted(
                                token = token,
                                patients = patients,
                                destination = command.destination(),
                                notice = notice,
                            ),
                        )
                    }
                }
                is PatientCommandOutcome.Rejected -> updateState {
                    AncReducer.reduce(
                        it,
                        AncEvent.SaveFailed(token, result.failure.toUiError(), unresolved = false),
                    )
                }
                PatientCommandOutcome.Ambiguous -> updateState {
                    AncReducer.reduce(
                        it,
                        AncEvent.SaveFailed(token, SaveUiError.OUTCOME_UNRESOLVED, unresolved = true),
                    )
                }
                PatientCommandOutcome.Obsolete -> Unit
            }
        }
    }

    private fun saveDating() {
        val current = _state.value
        val unlocked = current.vaultState as? VaultState.Unlocked ?: return
        val frozen = frozenClinicalCommand as? SaveDatingCommand
        val same = frozen?.takeIf { it.editorSessionId == current.datingDraft?.editorSessionId }
        when (val prepared = SaveDatingCommand.prepare(
            current,
            same?.operationUuid ?: UUID.randomUUID().toString(),
        )) {
            is ClinicalCommandPreparation.Rejected -> dispatch(
                AncEvent.ClinicalValidationFailed(ClinicalRecordKind.DATING, prepared.errors),
            )
            is ClinicalCommandPreparation.Ready -> submitPreparedClinicalCommand(
                existing = same,
                prepared = prepared.command,
                sessionEpoch = unlocked.sessionEpoch,
            )
        }
    }

    private fun saveUltrasound() {
        val current = _state.value
        val unlocked = current.vaultState as? VaultState.Unlocked ?: return
        val frozen = frozenClinicalCommand as? SaveUltrasoundScanCommand
        val same = frozen?.takeIf { it.editorSessionId == current.ultrasoundDraft?.editorSessionId }
        val scanId = same?.scanId ?: current.ultrasoundDraft?.scanId ?: UUID.randomUUID().toString()
        val recordKind = if (same?.expectedScanRevision == null && current.ultrasoundDraft?.baseScanRevision == null) {
            ClinicalRecordKind.ULTRASOUND_CREATE
        } else {
            ClinicalRecordKind.ULTRASOUND_UPDATE
        }
        when (val prepared = SaveUltrasoundScanCommand.prepare(
            current,
            same?.operationUuid ?: UUID.randomUUID().toString(),
            scanId,
        )) {
            is ClinicalCommandPreparation.Rejected -> dispatch(
                AncEvent.ClinicalValidationFailed(recordKind, prepared.errors),
            )
            is ClinicalCommandPreparation.Ready -> submitPreparedClinicalCommand(
                existing = same,
                prepared = prepared.command,
                sessionEpoch = unlocked.sessionEpoch,
            )
        }
    }

    /** Opens a unified laboratory investigation flow; optional date and labName pre-fill when adding to existing report. */
    fun startLabBatch(patientId: String, date: String? = null, labName: String? = null) {
        val patient = _state.value.patient(patientId)?.takeIf { it.status != "archived" } ?: return
        if (_state.value.savePhase.blocksEditing() || _state.value.hasDirtyDraft) return
        dispatch(
            AncEvent.StartLabBatch(
                patientId = patientId,
                sessionId = UUID.randomUUID().toString(),
                basePatientRevision = patient.revision,
                date = date ?: LocalDate.now().toString(),
                labName = labName,
            ),
        )
    }

    private fun saveLabBatch() {
        val current = _state.value
        val unlocked = current.vaultState as? VaultState.Unlocked ?: return
        val draft = current.labBatchDraft ?: return
        val errors = draft.validationErrors()
        if (errors.isNotEmpty()) {
            dispatch(AncEvent.ClinicalValidationFailed(ClinicalRecordKind.LAB_BATCH_SAVE, errors))
            return
        }
        val command = draft.toSaveCommand(
            operationUuid = UUID.randomUUID().toString(),
            expectedPatientRevision = draft.basePatientRevision,
        )
        submitPreparedClinicalCommand(
            existing = null,
            prepared = command,
            sessionEpoch = unlocked.sessionEpoch,
            onCommitted = { receipt ->
                val label = getApplication<Application>().resources.getQuantityString(
                    R.plurals.lab_batch_results,
                    command.entries.size,
                    command.entries.size,
                )
                undoCoordinator.offerLabBatchSaveUndo(
                    patientId = command.patientId,
                    batchId = command.operationUuid,
                    entries = command.entries,
                    receipt = receipt,
                    type = DeletableType.LAB,
                    label = label,
                )
            },
        )
    }

    private fun saveLab(isChained: Boolean = false) {
        val current = _state.value
        val unlocked = current.vaultState as? VaultState.Unlocked ?: return
        val frozen = frozenClinicalCommand as? SaveLabInvestigationCommand
        val same = frozen?.takeIf { it.editorSessionId == current.labDraft?.editorSessionId }
        val labId = same?.labId ?: current.labDraft?.labId ?: UUID.randomUUID().toString()
        val recordKind = if (same?.expectedLabRevision == null && current.labDraft?.baseLabRevision == null) {
            ClinicalRecordKind.LAB_CREATE
        } else {
            ClinicalRecordKind.LAB_UPDATE
        }
        when (val prepared = SaveLabInvestigationCommand.prepare(
            current,
            same?.operationUuid ?: UUID.randomUUID().toString(),
            labId,
        )) {
            is ClinicalCommandPreparation.Rejected -> {
                pendingChainedLab = null
                dispatch(
                    AncEvent.ClinicalValidationFailed(recordKind, prepared.errors),
                )
            }
            is ClinicalCommandPreparation.Ready -> {
                val labCmd = prepared.command as? SaveLabInvestigationCommand
                pendingChainedLab = if (isChained && labCmd != null) {
                    PendingChainedLab(
                        operationUuid = labCmd.operationUuid,
                        editorSessionId = labCmd.editorSessionId,
                        date = labCmd.date,
                        labName = labCmd.labName.orEmpty(),
                    )
                } else null
                submitPreparedClinicalCommand(
                    existing = same,
                    prepared = prepared.command,
                    sessionEpoch = unlocked.sessionEpoch,
                )
            }
        }
    }

    private fun saveMedication(isChained: Boolean = false) {
        val current = _state.value
        val unlocked = current.vaultState as? VaultState.Unlocked ?: return
        val frozen = frozenClinicalCommand as? SaveMedicationCommand
        val same = frozen?.takeIf { it.editorSessionId == current.medicationDraft?.editorSessionId }
        val medicationId = same?.medicationId ?: current.medicationDraft?.medicationId ?: UUID.randomUUID().toString()
        val recordKind = if (same?.expectedMedicationRevision == null && current.medicationDraft?.baseMedicationRevision == null) {
            ClinicalRecordKind.MEDICATION_CREATE
        } else {
            ClinicalRecordKind.MEDICATION_UPDATE
        }
        when (val prepared = SaveMedicationCommand.prepare(
            current,
            same?.operationUuid ?: UUID.randomUUID().toString(),
            medicationId,
        )) {
            is ClinicalCommandPreparation.Rejected -> {
                pendingChainedMedication = null
                dispatch(
                    AncEvent.ClinicalValidationFailed(recordKind, prepared.errors),
                )
            }
            is ClinicalCommandPreparation.Ready -> {
                val medCmd = prepared.command as? SaveMedicationCommand
                pendingChainedMedication = if (isChained && medCmd != null) {
                    PendingChainedMedication(
                        operationUuid = medCmd.operationUuid,
                        editorSessionId = medCmd.editorSessionId,
                        startDate = medCmd.startDate,
                    )
                } else null
                submitPreparedClinicalCommand(
                    existing = same,
                    prepared = prepared.command,
                    sessionEpoch = unlocked.sessionEpoch,
                )
            }
        }
    }

    private fun submitPreparedClinicalCommand(
        existing: ClinicalCommand?,
        prepared: ClinicalCommand,
        sessionEpoch: Long,
        onCommitted: ((ClinicalCommandReceipt) -> Unit)? = null,
    ) {
        val command = existing?.takeIf { it.computeFingerprint() == prepared.computeFingerprint() } ?: prepared
        frozenClinicalCommand = command
        val token = PendingSaveToken(command.operationUuid, command.editorSessionId)
        dispatch(AncEvent.SaveStarted(token))
        if (_state.value.pendingSave != token) {
            pendingChainedLab = null
            pendingChainedMedication = null
            return
        }
        viewModelScope.launch {
            val result = controller.execute(command, sessionEpoch) {
                updateState { AncReducer.reduce(it, AncEvent.SaveReconciling(token)) }
            }
            when (result) {
                is ClinicalCommandOutcome.Committed -> {
                    frozenClinicalCommand = null
                    updateState {
                        AncReducer.reduce(
                            it,
                            AncEvent.RecordCommitted(
                                token = token,
                                patients = result.snapshot.patients,
                                destination = command.destination(),
                                notice = command.savedNotice(),
                            ),
                        )
                    }
                    onCommitted?.invoke(result.receipt)
                    val chainedLab = pendingChainedLab?.takeIf {
                        it.operationUuid == command.operationUuid && it.editorSessionId == command.editorSessionId
                    }
                    pendingChainedLab = null
                    if (chainedLab != null) {
                        dispatch(
                            AncEvent.StartLabEditor(
                                patientId = command.patientId,
                                labId = null,
                                newLabId = UUID.randomUUID().toString(),
                                editorSessionId = UUID.randomUUID().toString(),
                                defaultDate = chainedLab.date,
                                defaultLabName = chainedLab.labName,
                            ),
                        )
                    }
                    val chainedMed = pendingChainedMedication?.takeIf {
                        it.operationUuid == command.operationUuid && it.editorSessionId == command.editorSessionId
                    }
                    pendingChainedMedication = null
                    if (chainedMed != null) {
                        if (command is SaveMedicationCommand && command.expectedMedicationRevision == null &&
                            sessionStillCurrent(sessionEpoch)
                        ) {
                            val savedMed = DemoMedicationRecord(
                                id = command.medicationId,
                                patientId = command.patientId,
                                name = command.name,
                                dosage = command.dosage,
                                route = command.route,
                                frequency = command.frequency,
                                startDate = command.startDate,
                                endDate = command.endDate,
                                status = command.status,
                                notes = command.notes,
                                isProphylaxis = command.isProphylaxis,
                                isNonCompliant = command.isNonCompliant,
                                adverseNotes = command.adverseNotes,
                                revision = result.receipt.committedRevision,
                            )
                            undoCoordinator.offerSaveUndo(
                                type = DeletableType.MEDICATION,
                                patientId = command.patientId,
                                recordId = command.medicationId,
                                receipt = result.receipt,
                                savedRecord = savedMed,
                            )
                        }
                        dispatch(
                            AncEvent.StartMedicationEditor(
                                patientId = command.patientId,
                                medicationId = null,
                                newMedicationId = UUID.randomUUID().toString(),
                                editorSessionId = UUID.randomUUID().toString(),
                                keepSavedNotice = true,
                            ),
                        )
                    }
                }
                is ClinicalCommandOutcome.Rejected -> {
                    pendingChainedLab = null
                    pendingChainedMedication = null
                    updateState {
                        AncReducer.reduce(
                            it,
                            AncEvent.SaveFailed(token, result.failure.toUiError(), unresolved = false),
                        )
                    }
                }
                ClinicalCommandOutcome.Ambiguous -> {
                    pendingChainedLab = null
                    pendingChainedMedication = null
                    updateState {
                        AncReducer.reduce(
                            it,
                            AncEvent.SaveFailed(token, SaveUiError.OUTCOME_UNRESOLVED, unresolved = true),
                        )
                    }
                }
                ClinicalCommandOutcome.Obsolete -> {
                    pendingChainedLab = null
                    pendingChainedMedication = null
                }
            }
        }
    }

    private fun submitVisit(command: SaveVisitCommand, sessionEpoch: Long) {
        val token = PendingSaveToken(command.operationUuid, command.editorSessionId)
        dispatch(AncEvent.SaveStarted(token))
        if (_state.value.pendingSave != token) return
        viewModelScope.launch {
            val result = controller.save(command, sessionEpoch) {
                updateState { AncReducer.reduce(it, AncEvent.SaveReconciling(token)) }
            }
            when (result) {
                is SaveOutcome.Committed -> {
                    frozenVisitCommand = null
                    updateState {
                        AncReducer.reduce(
                            it,
                            AncEvent.SaveCommitted(token, result.snapshot.patients, command.patientId),
                        )
                    }
                }
                is SaveOutcome.Rejected -> updateState {
                    AncReducer.reduce(
                        it,
                        AncEvent.SaveFailed(token, result.failure.toUiError(), unresolved = false),
                    )
                }
                SaveOutcome.Ambiguous -> updateState {
                    AncReducer.reduce(
                        it,
                        AncEvent.SaveFailed(token, SaveUiError.OUTCOME_UNRESOLVED, unresolved = true),
                    )
                }
                SaveOutcome.Obsolete -> Unit
            }
        }
    }

    fun beginInitialization() = controller.beginInitialization()
    fun beginUnlock() = controller.beginUnlock()
    fun completeAuthentication(request: AuthenticationRequest) = controller.completeAuthentication(request.id)
    fun cancelAuthentication(request: AuthenticationRequest) = controller.cancelAuthentication(request.id)
    fun authenticationDetached(request: AuthenticationRequest) = controller.authenticationDetached(request.id)
    fun deviceCredentialUnavailable() = controller.deviceCredentialUnavailable()
    fun retryStorageInspection() = controller.retryInspection()
    fun manualLock() {
        clearFrozenCommands()
        viewModelScope.launch {
            lifecycleMutex.withLock {
                try {
                    flushDraftsBounded()
                } finally {
                    controller.lock()
                }
            }
        }
    }
    fun lockVault() = manualLock()
    fun onForegrounded() {
        viewModelScope.launch {
            lifecycleMutex.withLock {
                controller.onForegrounded()
            }
        }
    }
    fun onBackgrounded(authenticationOverlayActive: Boolean) {
        if (!authenticationOverlayActive) clearFrozenCommands()
        viewModelScope.launch {
            lifecycleMutex.withLock {
                try {
                    flushDraftsBounded()
                } finally {
                    controller.onBackgrounded(authenticationOverlayActive)
                }
            }
        }
    }

    private suspend fun flushDraftsBounded() {
        val job = viewModelScope.launch {
            autosaver.flush()
        }
        withTimeoutOrNull(2000L) {
            job.join()
        }
    }

    private fun clearFrozenCommands() {
        frozenVisitCommand = null
        frozenPatientCommand = null
        frozenClinicalCommand = null
        pendingChainedLab = null
        pendingChainedMedication = null
        undoCoordinator.clear()
        _state.update { it.clearedForLock() }
    }

    private fun PatientCommand.destination(): AncRoute = when (this) {
        is SavePatientProfileCommand -> AncRoute.Workspace(patientId)
        is SaveOtherHistoriesCommand -> AncRoute.Workspace(patientId, WorkspaceSection.OTHER_HISTORIES)
        is SavePreviousPregnancyCommand -> AncRoute.Workspace(patientId, WorkspaceSection.OBSTETRIC_HISTORY)
        is ChangePatientStatusCommand -> AncRoute.PatientList
    }

    private fun PatientCommand.savedNotice(): SavedNotice = when (this) {
        is SavePatientProfileCommand -> SavedNotice.PATIENT
        is SaveOtherHistoriesCommand -> SavedNotice.HISTORIES
        is SavePreviousPregnancyCommand -> SavedNotice.PREGNANCY
        is ChangePatientStatusCommand -> if (targetStatus == "archived") {
            SavedNotice.ARCHIVED
        } else {
            SavedNotice.UNARCHIVED
        }
    }

    private fun SaveFailure.toUiError(): SaveUiError = when (this) {
        SaveFailure.CONFIRMED_NOT_SAVED -> SaveUiError.CONFIRMED_NOT_SAVED
        SaveFailure.STALE_REVISION, SaveFailure.VISIT_CONFLICT, SaveFailure.REVISION_EXHAUSTED ->
            SaveUiError.STALE_OR_CONFLICT
        SaveFailure.CROSS_PATIENT_WRITE -> SaveUiError.CROSS_PATIENT_REJECTED
        SaveFailure.DUPLICATE_OPERATION_MISMATCH -> SaveUiError.DUPLICATE_OPERATION_MISMATCH
        SaveFailure.PATIENT_ARCHIVED -> SaveUiError.PATIENT_ARCHIVED
        SaveFailure.INVALID_COMMAND, SaveFailure.PATIENT_NOT_FOUND, SaveFailure.STORAGE_LOCKED ->
            SaveUiError.STORAGE_ERROR
    }

    private fun PatientCommandFailure.toUiError(): SaveUiError = when (this) {
        PatientCommandFailure.CONFIRMED_NOT_SAVED -> SaveUiError.CONFIRMED_NOT_SAVED
        PatientCommandFailure.STALE_PATIENT_REVISION,
        PatientCommandFailure.STALE_RECORD_REVISION,
        PatientCommandFailure.RECORD_CONFLICT,
        PatientCommandFailure.REVISION_EXHAUSTED,
        -> SaveUiError.STALE_OR_CONFLICT
        PatientCommandFailure.CROSS_PATIENT_WRITE -> SaveUiError.CROSS_PATIENT_REJECTED
        PatientCommandFailure.DUPLICATE_OPERATION_MISMATCH -> SaveUiError.DUPLICATE_OPERATION_MISMATCH
        PatientCommandFailure.PATIENT_ARCHIVED -> SaveUiError.PATIENT_ARCHIVED
        PatientCommandFailure.CASE_ID_CONFLICT -> SaveUiError.CASE_ID_CONFLICT
        PatientCommandFailure.INVALID_COMMAND, PatientCommandFailure.PATIENT_NOT_FOUND -> SaveUiError.STORAGE_ERROR
    }



    private fun AncEvent.clearsFrozenVisit(): Boolean =
        this is AncEvent.DraftChanged || this is AncEvent.StartVisit || this is AncEvent.ConfirmDiscard

    private fun AncEvent.clearsFrozenPatient(): Boolean = when (this) {
        is AncEvent.PatientDraftChanged,
        is AncEvent.HistoriesDraftChanged,
        is AncEvent.PregnancyDraftChanged,
        is AncEvent.StatusReasonChanged,
        is AncEvent.StartPatientEditor,
        is AncEvent.StartHistoriesEditor,
        is AncEvent.StartPregnancyEditor,
        is AncEvent.StartStatusEditor,
        AncEvent.ConfirmDiscard,
        -> true
        else -> false
    }

    private fun confirmDelete() {
        val current = _state.value
        val pending = current.deleteConfirmation ?: return
        updateState { AncReducer.reduce(it, AncEvent.ConfirmDelete) }
        if (_state.value.deleteConfirmation != null) return // reducer ignored it (dirty draft or saving)
        undoCoordinator.beginDelete(pending.type, pending.patientId, pending.recordId)
    }

    private fun requestUndo(token: String) = undoCoordinator.request(token)

    private fun AncEvent.clearsFrozenClinical(): Boolean = when (this) {
        is AncEvent.DatingDraftChanged,
        is AncEvent.UltrasoundDraftChanged,
        is AncEvent.LabDraftChanged,
        is AncEvent.LabAbnormalChanged,
        is AncEvent.MedicationDraftChanged,
        is AncEvent.StartDatingEditor,
        is AncEvent.StartUltrasoundEditor,
        is AncEvent.StartLabEditor,
        is AncEvent.StartMedicationEditor,
        AncEvent.ConfirmDiscard,
        -> true
        else -> false
    }

    fun exportBackup(passphrase: CharArray, onResult: (ByteArray?) -> Unit) {
        viewModelScope.launch {
            val bytes = controller.exportEncryptedBackup(passphrase)
            onResult(bytes)
        }
    }

    fun restoreBackup(
        backupBytes: ByteArray,
        passphrase: CharArray,
        onResult: (com.sherifradwan.ancnative.storage.BackupInspectionResult) -> Unit,
    ) {
        viewModelScope.launch {
            val result = controller.restoreEncryptedBackup(backupBytes, passphrase)
            onResult(result)
        }
    }
}

internal fun ClinicalCommandFailure.toUiError(): SaveUiError = when (this) {
    ClinicalCommandFailure.CONFIRMED_NOT_SAVED -> SaveUiError.CONFIRMED_NOT_SAVED
    ClinicalCommandFailure.STALE_REVISION,
    ClinicalCommandFailure.STALE_PATIENT_REVISION,
    ClinicalCommandFailure.RECORD_CONFLICT,
    ClinicalCommandFailure.REVISION_EXHAUSTED,
    -> SaveUiError.STALE_OR_CONFLICT
    ClinicalCommandFailure.CROSS_PATIENT_WRITE -> SaveUiError.CROSS_PATIENT_REJECTED
    ClinicalCommandFailure.DUPLICATE_OPERATION_MISMATCH -> SaveUiError.DUPLICATE_OPERATION_MISMATCH
    ClinicalCommandFailure.PATIENT_ARCHIVED -> SaveUiError.PATIENT_ARCHIVED
    ClinicalCommandFailure.INVALID_COMMAND,
    ClinicalCommandFailure.PATIENT_NOT_FOUND,
    ClinicalCommandFailure.RECORD_NOT_FOUND,
    -> SaveUiError.STORAGE_ERROR
}
