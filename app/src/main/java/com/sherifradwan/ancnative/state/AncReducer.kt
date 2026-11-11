package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.storage.ClinicalRecordKind
import com.sherifradwan.ancnative.storage.DeletableRegistry
import com.sherifradwan.ancnative.storage.DraftScreenKind
import com.sherifradwan.ancnative.storage.VaultState

object AncReducer {
    fun reduce(state: AncState, event: AncEvent): AncState {
        val next = reduceInternal(state, event).withDeleteUiConsistent().withMedicationStopUiKept(event)
        val prompt = next.draftRecoveryPrompt
        return if (prompt != null && activeEditorSessionId(next) != prompt.editorSessionId) {
            next.copy(draftRecoveryPrompt = null)
        } else {
            next
        }
    }

    private fun isValueChangeEvent(event: AncEvent): Boolean = when (event) {
        is AncEvent.DraftChanged,
        is AncEvent.PatientDraftChanged,
        is AncEvent.HistoriesDraftChanged,
        is AncEvent.PregnancyDraftChanged,
        is AncEvent.StatusReasonChanged,
        is AncEvent.DatingDraftChanged,
        is AncEvent.UltrasoundDraftChanged,
        is AncEvent.LabDraftChanged,
        is AncEvent.LabAbnormalChanged,
        is AncEvent.MedicationDraftChanged,
        is AncEvent.ScanPhotoImported -> true
        else -> false
    }

    private fun reduceInternal(state: AncState, event: AncEvent): AncState {
        if (state.draftRecoveryPrompt != null && isValueChangeEvent(event)) {
            return state
        }
        return when (event) {
            is AncEvent.SearchChanged -> state.copy(searchQuery = event.query)
            AncEvent.ClearSearch -> state.copy(searchQuery = "")
            AncEvent.ShowNewPatientInfo -> state.copy(showNewPatientInfo = true)
            AncEvent.HideNewPatientInfo -> state.copy(showNewPatientInfo = false)
            is AncEvent.OpenWorkspace -> navigate(state, AncRoute.Workspace(event.patientId))
            is AncEvent.SelectSection -> {
                val route = state.route as? AncRoute.Workspace
                if (route == null) state else state.copy(route = route.copy(section = event.section), savedNotice = false, notice = null)
            }
            is AncEvent.StartVisit -> startVisit(state, event)
            is AncEvent.StartPatientEditor -> startPatientEditor(state, event)
            is AncEvent.StartHistoriesEditor -> startHistoriesEditor(state, event)
            is AncEvent.StartPregnancyEditor -> startPregnancyEditor(state, event)
            is AncEvent.StartStatusEditor -> startStatusEditor(state, event)
            is AncEvent.StartDatingEditor -> startDatingEditor(state, event)
            is AncEvent.StartUltrasoundEditor -> startUltrasoundEditor(state, event)
            is AncEvent.StartLabEditor -> startLabEditor(state, event)
            is AncEvent.StartMedicationEditor -> startMedicationEditor(state, event)
            is AncEvent.StopMedication -> stopMedication(state, event)
            is AncEvent.UndoMedicationStop -> undoMedicationStop(state, event)
            is AncEvent.MedicationStopUndoExpired ->
                if (state.medicationStopUndo?.token == event.token) state.copy(medicationStopUndo = null) else state
            AncEvent.DismissMedicationStopFailure -> state.withoutFailedMedicationQuickSave()
            is AncEvent.DraftChanged -> updateDraft(state, event)
            is AncEvent.PatientDraftChanged -> updatePatientDraft(state, event)
            is AncEvent.HistoriesDraftChanged -> updateHistoriesDraft(state, event)
            is AncEvent.PregnancyDraftChanged -> updatePregnancyDraft(state, event)
            is AncEvent.DatingDraftChanged -> updateDatingDraft(state, event)
            is AncEvent.UltrasoundDraftChanged -> updateUltrasoundDraft(state, event)
            is AncEvent.LabDraftChanged -> updateLabDraft(state, event)
            is AncEvent.LabAbnormalChanged -> updateLabAbnormal(state, event.isAbnormal)
            is AncEvent.MedicationDraftChanged -> updateMedicationDraft(state, event)
            is AncEvent.StatusReasonChanged -> state.copy(
                statusDraft = state.statusDraft?.copy(reason = event.value, errors = emptySet()),
                savePhase = SavePhase.IDLE,
                saveError = null,
            )
            AncEvent.SubmitPatientProfile,
            AncEvent.SubmitOtherHistories,
            AncEvent.SubmitPreviousPregnancy,
            AncEvent.SubmitPatientStatus,
            AncEvent.SubmitDating,
            AncEvent.SubmitUltrasound,
            AncEvent.SubmitLab,
            AncEvent.SubmitLabAndAddAnother,
            AncEvent.SubmitLabBatch,
            is AncEvent.StartLabBatchRequest,
            AncEvent.SubmitMedication,
            AncEvent.SubmitMedicationAndAddAnother,
            -> state
            is AncEvent.SaveValidationFailed -> state.copy(
                draft = state.draft?.copy(errors = event.errors),
                savePhase = SavePhase.IDLE,
                saveError = null,
            )
            is AncEvent.ClinicalValidationFailed -> if (DeletableRegistry.isDeleteOrRestoreKind(event.recordKind)) {
                // Delete and restore commands (every deletable kind) have no draft, so there is nothing to mark.
                state
            } else when (event.recordKind) {
                ClinicalRecordKind.DATING -> state.copy(datingDraft = state.datingDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
                ClinicalRecordKind.ULTRASOUND_CREATE, ClinicalRecordKind.ULTRASOUND_UPDATE -> state.copy(ultrasoundDraft = state.ultrasoundDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
                ClinicalRecordKind.LAB_CREATE, ClinicalRecordKind.LAB_UPDATE -> state.copy(labDraft = state.labDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
                ClinicalRecordKind.MEDICATION_CREATE, ClinicalRecordKind.MEDICATION_UPDATE ->
                    if (state.medicationDraft == null && state.medicationQuickSave != null) {
                        // A stored row that no longer passes the save rules cannot be stopped in one tap: nothing was written.
                        state.copy(savePhase = SavePhase.ERROR, saveError = SaveUiError.CONFIRMED_NOT_SAVED)
                    } else {
                        state.copy(medicationDraft = state.medicationDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
                    }
                // A validation failure is not a storage error: it belongs on the draft, like every other editor.
                ClinicalRecordKind.LAB_BATCH_SAVE -> state.copy(
                    labBatchDraft = state.labBatchDraft?.copy(errors = event.errors),
                    savePhase = SavePhase.IDLE,
                    saveError = null,
                )
                ClinicalRecordKind.LAB_BATCH_DELETE,
                ClinicalRecordKind.LAB_BATCH_RESTORE,
                -> state.copy(savePhase = SavePhase.IDLE, saveError = null)
                else -> error("Unhandled non-deletable clinical record kind: ${event.recordKind}")
            }
            is AncEvent.PatientValidationFailed -> validationFailed(state, event)
            is AncEvent.SaveStarted -> if (state.canAccept(event.token)) {
                state.copy(
                    savePhase = SavePhase.PENDING,
                    saveError = null,
                    pendingSave = event.token,
                    savedNotice = false,
                    notice = null,
                )
            } else state
            is AncEvent.SaveReconciling -> if (state.pendingSave == event.token) {
                state.copy(savePhase = SavePhase.RECONCILING, saveError = null)
            } else state
            is AncEvent.SaveFailed -> if (state.pendingSave == event.token) {
                state.copy(
                    savePhase = if (event.unresolved) SavePhase.UNRESOLVED else SavePhase.ERROR,
                    saveError = event.error,
                    pendingSave = if (event.unresolved) event.token else null,
                )
            } else state
            is AncEvent.SaveCommitted -> if (state.pendingSave == event.token) {
                state.clearDrafts().copy(
                    patients = event.patients,
                    route = AncRoute.Workspace(event.patientId, WorkspaceSection.VISITS),
                    savedNotice = true,
                    notice = SavedNotice.VISIT,
                    savePhase = SavePhase.IDLE,
                    saveError = null,
                    pendingSave = null,
                )
            } else state
            is AncEvent.RecordCommitted -> if (state.pendingSave == event.token) {
                val quick = state.medicationQuickSave?.takeIf { it.draft.editorSessionId == event.token.editorSessionId }
                state.clearDrafts().copy(
                    patients = event.patients,
                    // A one-tap stop or its Undo never moves the screen: the clinician may have changed tab meanwhile.
                    route = if (quick != null) state.route else event.destination,
                    savedNotice = true,
                    notice = event.notice,
                    savePhase = SavePhase.IDLE,
                    saveError = null,
                    pendingSave = null,
                    medicationStopUndo = quick?.undoOnSuccess?.copy(committedAtMillis = event.committedAtMillis)
                        ?: state.medicationStopUndo,
                )
            } else state
            AncEvent.CancelEditor, AncEvent.Back -> requestBack(state, event)
            AncEvent.ConfirmDiscard -> confirmDiscard(state)
            AncEvent.KeepEditing -> state.copy(showDiscardDialog = false, pendingAction = null)
            AncEvent.DismissNotice -> state.copy(savedNotice = false, notice = null)
            is AncEvent.AddAttachment, is AncEvent.ImportScanPhoto -> state
            is AncEvent.ScanPhotoImported -> {
                val draft = state.ultrasoundDraft
                if (draft == null || draft.patientId != event.patientId) state
                else state.copy(
                    ultrasoundDraft = draft.copy(
                        values = draft.values.copy(imageUris = (draft.values.imageUris + event.ref).distinct()),
                    ),
                )
            }
            is AncEvent.AttachmentFailed -> state.copy(attachmentError = event.error)
            AncEvent.DismissAttachmentError -> state.copy(attachmentError = null)
            is AncEvent.DraftRecoveryFound -> {
                if (state.draftRecoveryPrompt == null && activeEditorSessionId(state) == event.prompt.editorSessionId) {
                    state.copy(draftRecoveryPrompt = event.prompt)
                } else {
                    state
                }
            }
            AncEvent.AcceptDraftRecovery -> {
                val prompt = state.draftRecoveryPrompt ?: return state
                val recovered = applyDraftRecord(state, prompt.record).copy(draftRecoveryPrompt = null)
                // Recovered work counts as the clinician's own edits: the medication sheet asks before discarding it.
                if (prompt.record.screenKind == DraftScreenKind.MEDICATION) {
                    recovered.copy(medicationDraft = recovered.medicationDraft?.copy(touched = true))
                } else {
                    recovered
                }
            }
            AncEvent.DiscardDraftRecovery -> state.copy(draftRecoveryPrompt = null)
            is AncEvent.RequestDelete -> {
                val patient = state.patient(event.patientId)
                if (patient == null || patient.status == "archived" || state.hasDirtyDraft || state.savePhase.blocksEditing() || state.undoRunning || state.deleteRunning) {
                    state
                } else {
                    state.copy(
                        deleteConfirmation = PendingDeletion(
                            patientId = event.patientId,
                            type = event.type,
                            recordId = event.recordId,
                        ),
                        deleteMessage = state.deleteMessageAfterSuccessFor(event.patientId),
                    )
                }
            }
            AncEvent.CancelDelete -> state.copy(deleteConfirmation = null)
            AncEvent.ConfirmDelete -> {
                val pending = state.deleteConfirmation ?: return state
                if (state.hasDirtyDraft || state.savePhase.blocksEditing() || state.undoRunning || state.deleteRunning) return state
                val hook = EditorHooks.of(pending.type)
                val next = if (hook != null && hook.isOpenFor(state, pending.recordId)) {
                    hook.closeEditor(state, pending.patientId)
                } else {
                    state
                }
                next.copy(deleteConfirmation = null)
            }
            AncEvent.DeleteStarted -> state.copy(deleteRunning = true)
            AncEvent.DismissDeleteMessage -> state.copy(deleteMessage = null)
            is AncEvent.DeleteCommitted -> state.copy(
                patients = event.patients,
                undoOffer = event.offer,
                deleteConfirmation = null,
                deleteRunning = false,
                deleteMessage = state.deleteMessageAfterSuccessFor(event.patientId ?: event.offer?.patientId ?: ""),
                savedNotice = event.notice != null,
                notice = event.notice ?: state.notice,
                noticePatientId = if (event.notice != null) event.patientId else state.noticePatientId,
            )
            is AncEvent.DeleteFailed -> state.copy(
                deleteConfirmation = null,
                deleteRunning = false,
                saveError = event.error,
                deleteMessage = event.type?.let {
                    DeleteMessage(
                        kind = DeleteMessageKind.DELETE_FAILED,
                        patientId = event.patientId,
                        type = it,
                        label = event.label,
                        action = UndoAction.DELETE,
                        error = event.error,
                        patientName = event.patientName,
                    )
                },
            )
            is AncEvent.UndoOffered -> state.copy(undoOffer = event.offer, deleteMessage = state.deleteMessageAfterSuccessFor(event.offer.patientId))
            is AncEvent.UndoRequested -> {
                val offer = state.undoOffer
                if (offer == null || !state.canRequestUndo(event.token) || state.deleteRunning) {
                    state
                } else {
                    val hook = EditorHooks.of(offer.type)
                    val editorOpen = offer.action == UndoAction.SAVE && hook != null && hook.isOpenFor(state, offer.recordId)
                    if (editorOpen && (state.hasDirtyDraft || state.savePhase.blocksEditing())) {
                        // Undoing this save would delete a record that has unsaved edits open: refuse and say so.
                        state.copy(
                            deleteMessage = DeleteMessage(
                                kind = DeleteMessageKind.UNDO_BLOCKED,
                                patientId = offer.patientId,
                                type = offer.type,
                                label = offer.label,
                                action = offer.action,
                            ),
                        )
                    } else {
                        val closed = if (editorOpen && hook != null) hook.closeEditor(state, offer.patientId) else state
                        closed.copy(undoOffer = offer.copy(phase = UndoPhase.RUNNING), deleteMessage = state.deleteMessageAfterSuccessFor(offer.patientId))
                    }
                }
            }
            is AncEvent.UndoCommitted -> {
                var nextState = state
                for (hook in EditorHooks.all()) {
                    nextState = hook.rebaseCreateDraft(nextState, event.patients)
                }
                nextState.copy(
                    patients = event.patients,
                    undoOffer = if (nextState.undoOffer?.token == event.token) null else nextState.undoOffer,
                    savedNotice = event.notice != null,
                    notice = event.notice ?: nextState.notice,
                    noticePatientId = if (event.notice != null) event.patientId else nextState.noticePatientId,
                )
            }
            is AncEvent.UndoFailed -> {
                val offer = state.undoOffer
                if (state.vaultState !is VaultState.Unlocked) {
                    // A late failure after a lock must not write into the locked state.
                    state
                } else if (offer != null && offer.token == event.token) {
                    state.copy(
                        undoOffer = offer.copy(phase = UndoPhase.READY),
                        saveError = event.error,
                        deleteMessage = DeleteMessage(
                            kind = DeleteMessageKind.UNDO_FAILED,
                            patientId = offer.patientId,
                            type = offer.type,
                            label = offer.label,
                            action = offer.action,
                            error = event.error,
                            patientName = offer.patientName,
                        ),
                    )
                } else if (event.offer != null) {
                    // The offer was dropped (another patient opened, or a new offer replaced it): still say so.
                    val lost = event.offer
                    // saveError belongs to the open screen's own editor: do not overwrite it with another patient's failure.
                    state.copy(
                        deleteMessage = DeleteMessage(
                            kind = DeleteMessageKind.UNDO_FAILED,
                            patientId = lost.patientId,
                            type = lost.type,
                            label = lost.label,
                            action = lost.action,
                            error = event.error,
                            patientName = lost.patientName,
                        ),
                    )
                } else {
                    state
                }
            }
            is AncEvent.UndoExpired -> {
                if (state.undoOffer?.token == event.token) state.copy(undoOffer = null) else state
            }
            is AncEvent.StartLabBatch -> {
                if (state.savePhase.blocksEditing() || state.hasDirtyDraft) state
                else state.copy(
                    labBatchDraft = LabBatchDraft(
                        patientId = event.patientId,
                        editorSessionId = event.sessionId,
                        basePatientRevision = event.basePatientRevision,
                        date = event.date,
                        labName = event.labName,
                    ),
                    route = AncRoute.LabBatchEditor(event.patientId),
                )
            }
            is AncEvent.LabBatchItemsAdded -> state.copy(
                labBatchDraft = state.labBatchDraft?.add(event.items),
            )
            is AncEvent.LabBatchItemRemoved -> state.copy(
                labBatchDraft = state.labBatchDraft?.remove(event.labId),
            )
            is AncEvent.LabBatchResultChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.withItem(event.labId) { it.copy(result = event.value, isAbnormal = false) },
            )
            is AncEvent.LabBatchRangeChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.withItem(event.labId) { it.copy(referenceRange = event.value) },
            )
            is AncEvent.LabBatchFlagChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.withItem(event.labId) { it.copy(isAbnormal = event.isAbnormal) },
            )
            is AncEvent.LabBatchNotesChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.withItem(event.labId) { it.copy(notes = event.value) },
            )
            is AncEvent.LabBatchTabSelected -> state.copy(
                labBatchDraft = state.labBatchDraft?.copy(tab = event.tab),
            )
            is AncEvent.LabBatchCursorMoved -> state.copy(
                labBatchDraft = state.labBatchDraft?.moveCursor(event.index),
            )
            is AncEvent.LabBatchDateChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.copy(date = event.value),
            )
            is AncEvent.LabBatchLabNameChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.copy(labName = event.value),
            )
            is AncEvent.CancelLabBatch -> {
                // Only the batch is discarded: cancelling it must not wipe an unrelated editor draft (D9).
                val patientId = state.labBatchDraft?.patientId ?: state.route.patientIdOrNull()
                val cleared = state.copy(labBatchDraft = null, savePhase = SavePhase.IDLE, saveError = null)
                if (patientId == null) cleared else cleared.copy(route = AncRoute.Workspace(patientId, WorkspaceSection.LABORATORY))
            }
            is AncEvent.PayloadCleanupFailed -> {
                if (state.vaultState !is VaultState.Unlocked) {
                    // A late cleanup failure after a lock must not write into the locked state.
                    state
                } else {
                    val shown = state.deleteMessage
                    val sameRecordFailure = shown != null &&
                        shown.kind == DeleteMessageKind.UNDO_FAILED &&
                        shown.patientId == event.patientId &&
                        shown.type == event.type &&
                        shown.label == event.label
                    if (sameRecordFailure) {
                        // A failed Undo of this record is the more important message; the sweep retries the file.
                        state
                    } else {
                        // The delete stands; only the payload file is still on disk. Say so and let the next sweep retry.
                        state.copy(
                            deleteMessage = DeleteMessage(
                                kind = DeleteMessageKind.CLEANUP_FAILED,
                                patientId = event.patientId,
                                type = event.type,
                                label = event.label,
                                error = event.error,
                                patientName = event.patientName,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun navigate(state: AncState, destination: AncRoute): AncState {
        if (state.savePhase.blocksEditing()) return state
        return if (state.hasDirtyDraft) {
            state.copy(showDiscardDialog = true, pendingAction = PendingAction.Navigate(destination))
        } else {
            state.clearDrafts().copy(route = destination, savedNotice = false, notice = null)
        }
    }

    private fun validationFailed(state: AncState, event: AncEvent.PatientValidationFailed): AncState = when (event.editor) {
        PatientEditorKind.PROFILE -> state.copy(patientDraft = state.patientDraft?.copy(errors = event.errors))
        PatientEditorKind.HISTORIES -> state.copy(historiesDraft = state.historiesDraft?.copy(errors = event.errors))
        PatientEditorKind.PREGNANCY -> state.copy(pregnancyDraft = state.pregnancyDraft?.copy(errors = event.errors))
        PatientEditorKind.STATUS -> state.copy(statusDraft = state.statusDraft?.copy(errors = event.errors))
    }.copy(savePhase = SavePhase.IDLE, saveError = null)

    private fun requestBack(state: AncState, event: AncEvent): AncState {
        if (state.savePhase.blocksEditing()) return state
        val medicationDraft = state.medicationDraft
        if (medicationDraft != null) {
            // The sheet sits over the workspace, so closing it never leaves the screen. The sheet has already asked
            // before it sends Cancel, and an untouched form closes at once; only a Back that bypassed the sheet asks here.
            return if (event == AncEvent.CancelEditor || !medicationDraft.touched) {
                state.clearDrafts().copy(savedNotice = false, notice = null)
            } else {
                state.copy(showDiscardDialog = true, pendingAction = PendingAction.Navigate(state.route))
            }
        }
        val destination = when (val route = state.route) {
            AncRoute.PatientList -> return state
            is AncRoute.Workspace -> AncRoute.PatientList
            is AncRoute.VisitEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.VISITS)
            is AncRoute.PatientEditor -> if (route.isNew) AncRoute.PatientList else AncRoute.Workspace(route.patientId)
            is AncRoute.OtherHistoriesEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.OTHER_HISTORIES)
            is AncRoute.PreviousPregnancyEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.OBSTETRIC_HISTORY)
            is AncRoute.StatusEditor -> AncRoute.PatientList
            is AncRoute.DatingEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.DATING)
            is AncRoute.UltrasoundEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.ULTRASOUND)
            is AncRoute.LabEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.LABORATORY)
            is AncRoute.LabBatchEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.LABORATORY)
        }
        return if (state.hasDirtyDraft) {
            state.copy(showDiscardDialog = true, pendingAction = PendingAction.Navigate(destination))
        } else {
            state.clearDrafts().copy(route = destination, savedNotice = false, notice = null)
        }
    }

    private fun confirmDiscard(state: AncState): AncState {
        if (state.savePhase.blocksEditing()) return state
        val action = state.pendingAction
        val discarded = state.clearDrafts().copy(savedNotice = false, notice = null)
        return when (action) {
            is PendingAction.Navigate -> discarded.copy(route = action.destination)
            is PendingAction.StartVisit -> startVisit(
                discarded,
                AncEvent.StartVisit(action.patientId, action.visitId, action.editorSessionId),
            )
            null -> discarded
        }
    }

    private fun AncState.canAccept(token: PendingSaveToken): Boolean =
        (savePhase == SavePhase.IDLE || savePhase == SavePhase.ERROR) &&
            (activeEditorSessionId(this) == token.editorSessionId || medicationQuickSave?.draft?.editorSessionId == token.editorSessionId)
}
