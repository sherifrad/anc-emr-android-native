package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.storage.DraftRecord

data class DraftRecoveryPrompt(
    val slot: DraftSlot,
    val editorSessionId: String,
    val record: DraftRecord,
    val isStale: Boolean,
)

fun activeEditorSessionId(state: AncState): String? =
    state.draft?.editorSessionId
        ?: state.patientDraft?.editorSessionId
        ?: state.historiesDraft?.editorSessionId
        ?: state.pregnancyDraft?.editorSessionId
        ?: state.statusDraft?.editorSessionId
        ?: state.datingDraft?.editorSessionId
        ?: state.ultrasoundDraft?.editorSessionId
        ?: state.labDraft?.editorSessionId
        ?: state.medicationDraft?.editorSessionId

fun activeEditorBaseRevision(state: AncState): Long? {
    state.draft?.let { return it.baseRevision?.toLong() }
    state.patientDraft?.let { return it.baseRevision }
    state.historiesDraft?.let { return it.baseRevision }
    state.pregnancyDraft?.let { return it.basePregnancyRevision }
    state.statusDraft?.let { return it.baseRevision }
    state.datingDraft?.let { return it.basePatientRevision }
    state.ultrasoundDraft?.let { return it.baseScanRevision }
    state.labDraft?.let { return it.baseLabRevision }
    state.medicationDraft?.let { return it.baseMedicationRevision }
    return null
}

fun isDraftStale(record: DraftRecord, currentBaseRevision: Long?): Boolean {
    val recordBase = record.baseRevision ?: return false
    val currentBase = currentBaseRevision ?: return false
    return currentBase > recordBase
}

sealed interface DraftAge {
    data object LessThanMinute : DraftAge
    data class Minutes(val n: Long) : DraftAge
    data class Hours(val n: Long) : DraftAge
    data class Days(val n: Long) : DraftAge
}

fun draftAge(nowMillis: Long, savedMillis: Long): DraftAge {
    if (nowMillis <= savedMillis) return DraftAge.LessThanMinute
    val deltaMillis = nowMillis - savedMillis
    val seconds = deltaMillis / 1000L
    if (seconds < 60L) return DraftAge.LessThanMinute
    val minutes = seconds / 60L
    if (minutes < 60L) return DraftAge.Minutes(minutes)
    val hours = minutes / 60L
    if (hours < 24L) return DraftAge.Hours(hours)
    val days = hours / 24L
    return DraftAge.Days(days)
}
