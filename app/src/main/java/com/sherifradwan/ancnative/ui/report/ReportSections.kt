package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.MEDICATION_LABEL_ACTIVE
import com.sherifradwan.ancnative.model.deliveryOutcomeLine
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.model.medicationInstructions
import com.sherifradwan.ancnative.model.medicationStatusLabel
import com.sherifradwan.ancnative.model.parseDeliveryNotes
import com.sherifradwan.ancnative.ui.PatientRiskTier
import com.sherifradwan.ancnative.ui.parseRiskTier
import java.time.LocalDate

internal const val NOT_RECORDED = "Not recorded"

internal data class Limited<T>(val rows: List<T>, val hidden: Int)

internal data class ReportMedications(
    val current: List<DemoMedicationRecord>,
    val stopped: List<DemoMedicationRecord>,
)

internal enum class ReportRisk(val label: String) {
    HIGH("High risk"),
    MEDIUM("Medium risk"),
    LOW("Low risk"),
    UNRECORDED("Risk not recorded"),
}

internal data class ReportRow(val label: String, val value: String)

/** Newest kept oldest first; hidden counts the older rows left out. */
private fun <T> keepNewest(rows: List<T>, limit: Int, dateOf: (T) -> String?): Limited<T> {
    val ordered = rows.sortedBy { dateOf(it) ?: "" }
    if (ordered.size <= limit) return Limited(ordered, 0)
    return Limited(ordered.takeLast(limit), ordered.size - limit)
}

internal fun reportVisits(patient: DemoPatient, limit: Int): Limited<DemoVisit> =
    keepNewest(patient.visits, limit) { it.date }

internal fun reportScans(patient: DemoPatient, limit: Int): Limited<DemoUltrasoundScan> =
    keepNewest(patient.ultrasoundScans, limit) { it.date }

internal fun reportLabs(patient: DemoPatient, limit: Int): Limited<DemoLabInvestigation> {
    val ordered = patient.labInvestigations.sortedByDescending { it.date ?: "" }
    if (ordered.size <= limit) return Limited(ordered, 0)
    return Limited(ordered.take(limit), ordered.size - limit)
}

internal fun reportMedications(patient: DemoPatient): ReportMedications {
    val (current, stopped) = patient.medications.partition {
        medicationStatusLabel(it.status, it.isNonCompliant) == MEDICATION_LABEL_ACTIVE
    }
    return ReportMedications(
        current = current.sortedByDescending { it.startDate ?: "" },
        stopped = stopped.sortedByDescending { it.startDate ?: "" },
    )
}

internal fun reportRisk(riskLevel: String?): ReportRisk =
    when (parseRiskTier(riskLevel)) {
        PatientRiskTier.HIGH -> ReportRisk.HIGH
        PatientRiskTier.MEDIUM -> ReportRisk.MEDIUM
        PatientRiskTier.LOW -> ReportRisk.LOW
        PatientRiskTier.UNRECORDED -> ReportRisk.UNRECORDED
    }

internal fun reportStatusLine(patient: DemoPatient): String? {
    val archived = "Archived record".takeIf { patient.status == "archived" }
    return listOfNotNull(archived, patient.deliveryOutcomeLine()).joinToString(" · ").ifEmpty { null }
}

internal fun reportGestationalAge(patient: DemoPatient, asOf: LocalDate): String? {
    if (patient.delivered) return null
    return patient.dating?.calculateGestationalAgeResult(asOf)?.formatted
}

internal fun reportDate(iso: String?): String = formatDmy(iso) ?: NOT_RECORDED

internal fun visitText(visit: DemoVisit): String {
    val findings = visit.findings?.trim()
    val notes = visit.notes?.trim()
    val parts = buildList {
        if (!findings.isNullOrBlank()) add(findings)
        if (!notes.isNullOrBlank()) add(notes)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: NOT_RECORDED
}

internal fun scanGestationalAge(scan: DemoUltrasoundScan): String {
    val weeks = scan.gestationalAgeWeeks
    val days = scan.gestationalAgeDays
    if (weeks == null && days == null) return "-"
    val weeksPart = if (weeks != null) "${weeks}w" else ""
    val daysPart = if (days != null) "${days}d" else ""
    return if (weeks != null && days != null) "$weeksPart + $daysPart" else "$weeksPart$daysPart"
}

internal fun scanNotes(scan: DemoUltrasoundScan): String {
    val parts = buildList {
        scan.fetalNumber?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Fetuses: $it") }
        scan.fetalHeartRate?.trim()?.takeIf { it.isNotEmpty() }?.let { add("FHR: $it") }
        scan.impression?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        scan.biometryBpd?.trim()?.takeIf { it.isNotEmpty() }?.let { add("BPD: $it") }
        scan.biometryHc?.trim()?.takeIf { it.isNotEmpty() }?.let { add("HC: $it") }
        scan.biometryAc?.trim()?.takeIf { it.isNotEmpty() }?.let { add("AC: $it") }
        scan.biometryFl?.trim()?.takeIf { it.isNotEmpty() }?.let { add("FL: $it") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: NOT_RECORDED
}

internal fun scanWeight(scan: DemoUltrasoundScan): String {
    val weight = scan.estimatedFetalWeight?.trim()?.takeIf { it.isNotEmpty() } ?: return "-"
    return if (weight.any { it.isLetter() }) weight else "$weight g"
}

internal fun medicationDetail(m: DemoMedicationRecord): String {
    val parts = medicationInstructions(m.dosage, m.route, m.notes)
        .split(" · ")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toMutableList()
    m.frequency?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
    if (m.isProphylaxis) parts.add("Prophylaxis")
    m.adverseNotes?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add("Adverse: $it") }
    return parts.joinToString(" · ")
}

internal fun medicationDates(m: DemoMedicationRecord): String {
    val start = m.startDate?.trim()
    if (start.isNullOrBlank()) return ""
    val end = m.endDate?.trim()
    return if (end.isNullOrBlank()) "From ${formatDmy(start)}" else "${formatDmy(start)} to ${formatDmy(end)}"
}

internal fun labResult(lab: DemoLabInvestigation): String {
    val result = lab.result?.trim()
    val unit = lab.unit?.trim()
    return buildString {
        append(result.orEmpty())
        if (!unit.isNullOrBlank()) {
            if (isNotEmpty()) append(" ")
            append(unit)
        }
    }
}

internal fun labExtra(lab: DemoLabInvestigation): String {
    val labName = lab.labName?.trim()
    val notes = lab.notes?.trim()
    val parts = buildList {
        if (!labName.isNullOrBlank()) add(labName)
        if (!notes.isNullOrBlank()) add(notes)
    }
    return parts.joinToString(" · ")
}

/** Planned delivery, hospital and postpartum only; fees stay out of the plan rows. */
internal fun deliveryPlanRows(patient: DemoPatient): List<ReportRow> {
    val sections = parseDeliveryNotes(patient.obstetricNotes)
    return buildList {
        sections.plan?.trim()?.takeIf { it.isNotEmpty() }?.let { add(ReportRow("Planned delivery", it)) }
        sections.hospital?.trim()?.takeIf { it.isNotEmpty() }?.let { add(ReportRow("Hospital", it)) }
        sections.postpartum?.trim()?.takeIf { it.isNotEmpty() }?.let { add(ReportRow("Postpartum", it)) }
    }
}
