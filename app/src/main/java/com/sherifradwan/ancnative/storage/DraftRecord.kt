package com.sherifradwan.ancnative.storage

const val NEW_PATIENT_DRAFT_KEY = "__NEW_PATIENT__"

enum class DraftScreenKind {
    VISIT,
    PATIENT_PROFILE,
    HISTORIES,
    PREVIOUS_PREGNANCY,
    PATIENT_STATUS,
    DATING,
    ULTRASOUND,
    LAB,
    MEDICATION,
    LAB_BATCH,
}

enum class DraftEditorKind {
    CREATE,
    EDIT,
}

data class DraftRecord(
    val patientId: String,
    val screenKind: DraftScreenKind,
    val editorKind: DraftEditorKind,
    val recordId: String?,
    val baseRevision: Long?,
    val fields: Map<String, String>,
    val updatedAtMillis: Long,
)
