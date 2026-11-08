package com.sherifradwan.ancnative.model.catalog

import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Enumeration of all choice lists in the ANC app that allow user-defined choices via "+ Add other".
 *
 * NOTE (Clinical safety, Section 2.3):
 * Dating methods (LMP, IVF, Scan, Clinical) are strictly fixed and must NEVER be added to this enum.
 */
enum class CustomChoiceList(val storageKey: String, val displayName: String) {
    MEDICATION_NAME("custom_choice_med_name", "Medication Names"),
    MEDICATION_DOSE("custom_choice_med_dose", "Medication Dosages"),
    MEDICATION_ROUTE("custom_choice_med_route", "Medication Routes"),
    LAB_TEST("custom_choice_lab_test", "Lab Tests"),
    LAB_RESULT("custom_choice_lab_result", "Lab Result Presets"),
    LAB_CATEGORY("custom_choice_lab_category", "Lab Categories"),
    ULTRASOUND_PRESENTATION("custom_choice_us_presentation", "Fetal Presentations"),
    ULTRASOUND_PLACENTA("custom_choice_us_placenta", "Placenta Sites"),
    ULTRASOUND_LIQUOR("custom_choice_us_liquor", "Amniotic Fluid / Liquor"),
    ULTRASOUND_FINDING("custom_choice_us_finding", "Ultrasound Findings"),
    PREVIOUS_PREGNANCY_OUTCOME("custom_choice_prev_preg_outcome", "Pregnancy Outcomes"),
    PREVIOUS_PREGNANCY_MODE("custom_choice_prev_preg_mode", "Delivery Modes"),
    PREVIOUS_PREGNANCY_COMPLICATION("custom_choice_prev_preg_complication", "Pregnancy Complications"),
    CLINICAL_ATTENTION_PHRASE("custom_choice_attention_phrase", "Clinical Attention Phrases"),
    VISIT_OEDEMA("custom_choice_visit_oedema", "Oedema Findings"),
    VISIT_MOVEMENTS("custom_choice_visit_movements", "Fetal Movements"),
    VISIT_URINE("custom_choice_visit_urine", "Urinalysis Findings"),
    VISIT_BP("custom_choice_visit_bp", "Blood Pressure Presets"),
    VISIT_WEIGHT("custom_choice_visit_weight", "Weight Presets"),
    VISIT_FUNDAL_HEIGHT("custom_choice_visit_fundal_height", "Fundal Height Presets"),
}

data class CustomChoiceItem(
    val id: String = UUID.randomUUID().toString(),
    val list: CustomChoiceList,
    val label: String,
    val isCustom: Boolean = true,
)

sealed interface CustomChoiceValidationResult {
    data class Valid(val sanitizedLabel: String) : CustomChoiceValidationResult
    data class Invalid(val reason: Reason) : CustomChoiceValidationResult {
        enum class Reason {
            BLANK,
            TOO_LONG,
            DUPLICATE,
            INVALID_CHARACTERS,
            NOT_FOUND,
        }
    }
}

fun maxCustomChoiceLength(list: CustomChoiceList): Int =
    if (list == CustomChoiceList.CLINICAL_ATTENTION_PHRASE) 140 else 80

data class RemovedCustomChoice(
    val item: CustomChoiceItem,
    val originalIndex: Int,
)

/**
 * Single pure validation function for custom choice inputs.
 * Enforces:
 * - Rejects strings containing control characters (C0, C1, NUL, tabs, newlines, line/paragraph separators).
 * - Trims leading/trailing whitespace.
 * - Rejects null, blank, or empty strings.
 * - Rejects strings exceeding [maxLength] characters.
 * - Case-insensitive duplicate check against [existingLabels].
 */
fun validateCustomChoice(
    raw: String?,
    existingLabels: Collection<String>,
    maxLength: Int = 80,
): CustomChoiceValidationResult {
    if (raw == null) {
        return CustomChoiceValidationResult.Invalid(CustomChoiceValidationResult.Invalid.Reason.BLANK)
    }
    // Reject newline, carriage return, line separators, paragraph separators, or any ISO control character
    if (raw.any { it.isISOControl() || it == '\n' || it == '\r' || it == '\u2028' || it == '\u2029' }) {
        return CustomChoiceValidationResult.Invalid(CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS)
    }
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) {
        return CustomChoiceValidationResult.Invalid(CustomChoiceValidationResult.Invalid.Reason.BLANK)
    }
    if (trimmed.length > maxLength) {
        return CustomChoiceValidationResult.Invalid(CustomChoiceValidationResult.Invalid.Reason.TOO_LONG)
    }
    val isDuplicate = existingLabels.any { existing ->
        existing.trim().equals(trimmed, ignoreCase = true)
    }
    if (isDuplicate) {
        return CustomChoiceValidationResult.Invalid(CustomChoiceValidationResult.Invalid.Reason.DUPLICATE)
    }
    return CustomChoiceValidationResult.Valid(trimmed)
}

interface CustomChoicesDelegate {
    fun customChoices(list: CustomChoiceList): StateFlow<List<CustomChoiceItem>>

    fun addCustomChoice(
        list: CustomChoiceList,
        label: String,
        existingDefaults: Collection<String> = emptyList(),
    ): CustomChoiceValidationResult

    fun renameCustomChoice(
        list: CustomChoiceList,
        id: String,
        newLabel: String,
        existingDefaults: Collection<String> = emptyList(),
    ): CustomChoiceValidationResult

    fun removeCustomChoice(
        list: CustomChoiceList,
        id: String,
    ): RemovedCustomChoice?

    fun restoreCustomChoice(
        removed: RemovedCustomChoice,
    ): Boolean

    fun restoreCustomChoice(
        item: CustomChoiceItem,
    ): Boolean = restoreCustomChoice(RemovedCustomChoice(item, -1))

    fun clearCustomChoices(
        list: CustomChoiceList,
    )

    fun getAllCustomChoices(): Map<CustomChoiceList, List<CustomChoiceItem>>

    fun restoreAllCustomChoices(
        data: Map<CustomChoiceList, List<CustomChoiceItem>>,
    )
}
