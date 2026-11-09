package com.sherifradwan.ancnative.model

import java.time.LocalDate
import kotlin.math.abs

private const val NBSP = ' '

/** One labelled line of the dating block. A null [value] means "not recorded". */
data class DatingTextRow(val label: String, val value: String?, val isEdd: Boolean = false)

/**
 * Anchor for the dating discordance check: the earliest scan on or after the LMP and not after [today].
 * Malformed, pre-LMP and future scan dates are ignored; null when none is left (the check then uses today).
 */
private fun earliestScanDate(patient: DemoPatient, today: LocalDate): LocalDate? {
    val lmp = parseIsoDateOrNull(patient.dating?.lmpDate)
    return patient.ultrasoundScans
        .mapNotNull { parseIsoDateOrNull(it.date) }
        .filter { (lmp == null || !it.isBefore(lmp)) && !it.isAfter(today) }
        .minOrNull()
}

/** The dating summary of a patient as of [today]; every screen builds it through here. */
fun datingSummaryOf(patient: DemoPatient, today: LocalDate): DatingSummary =
    datingSummary(patient.dating, earliestScanDate(patient, today), today)

/** Short method label for a stored dating code; unknown codes are shown as typed. */
fun datingMethodLabel(code: String?): String? = when (val canonical = canonicalDatingMethod(code)) {
    "LMP" -> "LMP"
    "Ultrasound" -> "by ultrasound"
    "IVF_TRANSFER" -> "ET"
    "Clinical" -> "clinical"
    else -> canonical
}

/** Rows in display order: LMP or ET, EDD, Method. Dates are dd/mm/yyyy. */
fun datingTextRows(summary: DatingSummary): List<DatingTextRow> = buildList {
    when (val origin = summary.origin) {
        is DatingOrigin.Lmp -> add(DatingTextRow("LMP", formatDmy(origin.date)))
        is DatingOrigin.Transfer -> add(
            DatingTextRow(
                "ET",
                listOfNotNull(formatDmy(origin.date), origin.embryoAgeDays?.let { "D$it" })
                    .joinToString("$NBSP·$NBSP"),
            ),
        )
        DatingOrigin.None -> Unit
    }
    val eddLabel = if (summary.eddLabel == EddLabel.CONFIRMED) "Confirmed EDD" else "EDD"
    add(DatingTextRow(eddLabel, summary.edd?.let(::formatDmy), isEdd = true))
    datingMethodLabel(summary.methodCode)?.let { add(DatingTextRow("Method", it)) }
}

/** "LMP EDD dd/mm/yyyy · US EDD dd/mm/yyyy · gap 9 d" when the dating is discordant, else null. */
fun datingDiscordanceLine(dating: DemoPatientDating?, summary: DatingSummary): String? {
    val advice = summary.discordance ?: return null
    val d = dating ?: return null
    val lmpEdd = GestationalAgeEngine.dueDateForMethod(d, "LMP") ?: return null
    val scanEdd = parseIsoDateOrNull(d.ultrasoundEdd) ?: return null
    return "LMP EDD ${formatDmy(lmpEdd)} · US EDD ${formatDmy(scanEdd)} · gap ${abs(advice.differenceDays)} d"
}
