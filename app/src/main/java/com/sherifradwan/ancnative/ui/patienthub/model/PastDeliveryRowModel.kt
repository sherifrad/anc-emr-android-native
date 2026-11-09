package com.sherifradwan.ancnative.ui.patienthub.model

import androidx.annotation.StringRes
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy

enum class OutcomeTone { Neutral, Caution, Severe }

enum class RowExtraKind { PregnancyType, LossManagement, TerminationMethod }

/** Display-ready, Compose-free view of one previous pregnancy. */
data class PastDeliveryRow(
    val year: String?,
    val dateTextFallback: String?,
    /** Free text (outcomeText, else the raw outcome value); render [outcomeLabelRes] first when set. */
    val outcomeLabel: String?,
    @StringRes val outcomeLabelRes: Int?,
    /** Clinician-entered outcomeText when a structured outcome is also present and the text differs. */
    val outcomeNote: String?,
    val outcomeTone: OutcomeTone,
    val mode: String?,
    val placeAndDoctor: String?,
    val notes: String?,
    val extras: List<Pair<RowExtraKind, String>>,
)

private val YearRegex = Regex("""(?<!\d)(?:19|20)\d{2}(?!\d)""")

private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

@StringRes
private fun enumOutcomeLabelRes(outcome: String?): Int? = when (outcome?.trim()) {
    "LiveBirth" -> R.string.outcome_live_birth
    "Stillbirth" -> R.string.outcome_stillbirth
    "EarlyPregnancyLoss" -> R.string.outcome_early_loss
    "TerminationOfPregnancy" -> R.string.outcome_termination
    else -> null
}

fun pastDeliveryRow(p: DemoPreviousPregnancy): PastDeliveryRow {
    val dateText = p.dateText.clean()
    val year = dateText?.let { YearRegex.find(it)?.value }
    val outcomeEnum = p.outcome.clean()
    val outcomeRes = enumOutcomeLabelRes(outcomeEnum)
    val outcomeText = p.outcomeText.clean()
    return PastDeliveryRow(
        year = year,
        dateTextFallback = if (year == null) dateText else null,
        outcomeLabel = if (outcomeRes != null) outcomeEnum else outcomeText ?: outcomeEnum,
        outcomeLabelRes = outcomeRes,
        outcomeNote = if (outcomeRes != null && outcomeText != null && !outcomeText.equals(outcomeEnum, true)) outcomeText else null,
        outcomeTone = when (outcomeEnum) {
            "Stillbirth" -> OutcomeTone.Severe
            "EarlyPregnancyLoss" -> OutcomeTone.Caution
            else -> OutcomeTone.Neutral
        },
        mode = p.birthMethod.clean() ?: p.deliveryBy.clean(),
        placeAndDoctor = p.placeAndDoctor.clean(),
        notes = p.otherDetails.clean(),
        extras = listOfNotNull(
            p.pregnancyType.clean()?.let { RowExtraKind.PregnancyType to it },
            p.lossManagement.clean()?.let { RowExtraKind.LossManagement to it },
            p.terminationMethod.clean()?.let { RowExtraKind.TerminationMethod to it },
        ),
    )
}
