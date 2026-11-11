package com.sherifradwan.ancnative.state

internal fun AncEvent.startsAnotherAction(): Boolean = when (this) {
    is AncEvent.OpenWorkspace,
    is AncEvent.SelectSection,
    is AncEvent.StartVisit,
    is AncEvent.StartPatientEditor,
    is AncEvent.Flow,
    is AncEvent.RegistrationNext,
    is AncEvent.StartDeliveryPlanEditor,
    is AncEvent.StartHistoriesEditor,
    is AncEvent.StartPregnancyEditor,
    is AncEvent.StartStatusEditor,
    is AncEvent.StartDatingEditor,
    is AncEvent.StartUltrasoundEditor,
    is AncEvent.StartLabEditor,
    is AncEvent.StartMedicationEditor,
    is AncEvent.StopMedication,
    is AncEvent.StartLabBatch,
    is AncEvent.StartLabBatchRequest,
    is AncEvent.RequestDelete,
    is AncEvent.UndoRequested,
    is AncEvent.AddAttachment,
    is AncEvent.ImportScanPhoto,
    AncEvent.Back,
    AncEvent.CancelEditor,
    -> true
    else -> false
}

internal fun today(): String = java.time.LocalDate.now().toString()

internal fun AncState.clearDrafts(): AncState = copy(
    draft = null,
    patientDraft = null,
    historiesDraft = null,
    pregnancyDraft = null,
    statusDraft = null,
    datingDraft = null,
    ultrasoundDraft = null,
    labDraft = null,
    medicationDraft = null,
    labBatchDraft = null,
    medicationQuickSave = null,
    showDiscardDialog = false,
    pendingAction = null,
    savePhase = SavePhase.IDLE,
    saveError = null,
    pendingSave = null,
    draftRecoveryPrompt = null,
)
