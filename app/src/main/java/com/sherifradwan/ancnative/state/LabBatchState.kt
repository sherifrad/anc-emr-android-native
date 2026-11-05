package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.storage.LabBatchEntry
import com.sherifradwan.ancnative.storage.SaveLabBatchCommand

/** Which of the three batch tabs is open. A new batch starts on Add (nothing to enter yet). */
enum class LabBatchTab { ADD, ENTER, REVIEW }

/** One row of the batch while it is still in memory. Nothing here is written until Save. */
data class LabBatchItem(
    val labId: String,
    val testName: String,
    val category: String,
    val unit: String,
    val result: String = "",
    val referenceRange: String = "",
    val isAbnormal: Boolean = false,
    val notes: String = "",
) {
    val isFilled: Boolean get() = result.isNotBlank()
}

/**
 * The whole laboratory batch before it is saved: one date and one lab for every row, and a cursor for the Enter tab.
 * It lives in [AncState] so a recomposition never loses it; it is cleared when the batch is saved or cancelled.
 */
data class LabBatchDraft(
    val patientId: String,
    val editorSessionId: String,
    val basePatientRevision: Long,
    val date: String,
    val labName: String?,
    val items: List<LabBatchItem> = emptyList(),
    val cursor: Int = 0,
    val tab: LabBatchTab = LabBatchTab.ADD,
    val errors: Set<ClinicalValidationError> = emptySet(),
) {
    val filledCount: Int get() = items.count { it.isFilled }

    val currentItem: LabBatchItem? get() = items.getOrNull(cursor)

    fun item(labId: String): LabBatchItem? = items.firstOrNull { it.labId == labId }

    fun withItem(labId: String, transform: (LabBatchItem) -> LabBatchItem): LabBatchDraft =
        copy(items = items.map { if (it.labId == labId) transform(it) else it })

    /** Adding to an empty batch moves the clinician straight to Enter, where the first test is waiting. */
    fun add(added: List<LabBatchItem>): LabBatchDraft {
        if (added.isEmpty()) return this
        val next = (items + added).distinctBy { it.labId }
        val tab = if (items.isEmpty() && next.isNotEmpty()) LabBatchTab.ENTER else tab
        return copy(items = next, cursor = cursor.coerceIn(0, (next.size - 1).coerceAtLeast(0)), tab = tab)
    }

    fun remove(labId: String): LabBatchDraft {
        val next = items.filterNot { it.labId == labId }
        return copy(items = next, cursor = cursor.coerceIn(0, (next.size - 1).coerceAtLeast(0)))
    }

    fun moveCursor(index: Int): LabBatchDraft =
        copy(cursor = index.coerceIn(0, (items.size - 1).coerceAtLeast(0)))

    fun toEntries(): List<LabBatchEntry> = items.map { item ->
        LabBatchEntry(
            labId = item.labId,
            testName = item.testName,
            testCategory = item.category,
            result = item.result.trim(),
            unit = item.unit.ifBlank { null },
            referenceRange = item.referenceRange.trim().ifBlank { null },
            isAbnormal = item.isAbnormal,
            notes = item.notes.trim().ifBlank { null },
        )
    }
    /** What Save would reject, so a bad date or an empty row never reaches the command. */
    fun validationErrors(): Set<ClinicalValidationError> = buildSet {
        if (date.isBlank()) add(ClinicalValidationError.REQUIRED_DATE)
        else if (!ClinicalVocabulary.isValidIsoDate(date)) add(ClinicalValidationError.INVALID_DATE)
        if (items.isEmpty()) add(ClinicalValidationError.REQUIRED_RESULT)
        if (items.any { it.result.isBlank() }) add(ClinicalValidationError.REQUIRED_RESULT)
        if (items.any { it.testName.isBlank() }) add(ClinicalValidationError.REQUIRED_NAME)
        if (items.any { it.category !in ClinicalVocabulary.testCategories }) add(ClinicalValidationError.INVALID_CATEGORY)
    }

    fun toSaveCommand(operationUuid: String, expectedPatientRevision: Long): SaveLabBatchCommand =
        SaveLabBatchCommand(
            editorSessionId = editorSessionId,
            operationUuid = operationUuid,
            patientId = patientId,
            expectedPatientRevision = expectedPatientRevision,
            date = date,
            labName = labName?.trim()?.ifEmpty { null },
            entries = toEntries(),
        )
}
