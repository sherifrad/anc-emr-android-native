package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.datingSummaryOf
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import com.sherifradwan.ancnative.ui.formatWeightKg
import java.time.LocalDate

private const val MAX_SHARE_LINES = 15
private const val MAX_SHARE_MED_NAMES = 3
private const val MAX_SHARE_MED_CHARS = 80

private enum class ShareLineRole {
    SAFETY,
    CLINICAL,
    MEDICATIONS,
    DELIVERY_PLAN,
}

private data class ShareLine(
    val text: String,
    val role: ShareLineRole,
)

private fun isDeliverySafetyRow(label: String): Boolean =
    label == "Delivery Outcome" ||
        label == "Blood bank precautions" ||
        label.contains("blood", ignoreCase = true) ||
        label.contains("Rh", ignoreCase = true) ||
        label.contains("precaution", ignoreCase = true)

private fun formatMedicationsLine(medNames: List<String>): String {
    val shown = mutableListOf<String>()
    var totalChars = 0
    for (medName in medNames) {
        if (shown.size >= MAX_SHARE_MED_NAMES) break
        val nextChars = if (shown.isEmpty()) medName.length else totalChars + 2 + medName.length
        if (shown.isNotEmpty() && nextChars > MAX_SHARE_MED_CHARS) break
        shown.add(medName)
        totalChars = nextChars
    }
    val remaining = medNames.size - shown.size
    val medsText = if (remaining > 0) {
        "${shown.joinToString(", ")} +$remaining more"
    } else {
        shown.joinToString(", ")
    }
    return "Current medications: $medsText"
}

private fun compactShareLines(entries: List<ShareLine>): List<String> {
    if (entries.size <= MAX_SHARE_LINES) {
        return entries.map { it.text }
    }

    val working = entries.toMutableList()
    val planIndices = working.indices.filter { working[it].role == ShareLineRole.DELIVERY_PLAN }
    if (planIndices.size > 1) {
        val mergedText = "Delivery plan: ${planIndices.joinToString(" · ") { working[it].text }}"
        working[planIndices.first()] = ShareLine(mergedText, ShareLineRole.DELIVERY_PLAN)
        for (idx in planIndices.drop(1).asReversed()) {
            working.removeAt(idx)
        }
    }
    if (working.size <= MAX_SHARE_LINES) {
        return working.map { it.text }
    }

    for (roleToTrim in listOf(ShareLineRole.DELIVERY_PLAN, ShareLineRole.MEDICATIONS, ShareLineRole.CLINICAL)) {
        for (i in working.lastIndex downTo 0) {
            if (working.size <= MAX_SHARE_LINES) break
            if (working[i].role == roleToTrim) {
                working.removeAt(i)
            }
        }
        if (working.size <= MAX_SHARE_LINES) break
    }

    return working.map { it.text }
}

private fun singleLine(raw: String?): String =
    raw?.trim()
        ?.lines()
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.joinToString(" · ")
        .orEmpty()

private fun latestScan(patient: DemoPatient): DemoUltrasoundScan? =
    patient.ultrasoundScans.maxWithOrNull(
        compareBy<DemoUltrasoundScan>(
            { parseIsoDateOrNull(it.date) != null },
            { parseIsoDateOrNull(it.date) },
            { it.id },
        ),
    )

internal fun patientShareText(patient: DemoPatient, asOf: LocalDate = LocalDate.now()): String {
    val entries = buildList {
        val name = singleLine(patient.displayName)
        val caseId = singleLine(patient.caseId)
        listOfNotNull(
            name.takeIf { it.isNotEmpty() }?.let { "Patient: $it" },
            caseId.takeIf { it.isNotEmpty() }?.let { "ID: $it" },
        ).joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
            add(ShareLine(it, ShareLineRole.SAFETY))
        }

        if (!patient.riskLevel.isNullOrBlank()) {
            val riskLabel = singleLine(reportRiskLabel(patient.riskLevel))
            if (riskLabel.isNotEmpty()) {
                add(ShareLine("Risk: $riskLabel", ShareLineRole.SAFETY))
            }
        }

        val allergies = singleLine(patient.allergyHistory)
        if (allergies.isNotEmpty() && allergies != NOT_RECORDED) {
            add(ShareLine("Allergies: $allergies", ShareLineRole.SAFETY))
        }

        val attention = singleLine(patient.clinicalAttention)
        if (attention.isNotEmpty()) {
            add(ShareLine("Clinical attention: $attention", ShareLineRole.SAFETY))
        }

        val ga = singleLine(reportGestationalAge(patient, asOf))
        val summary = datingSummaryOf(patient, asOf)
        val edd = if (!patient.delivered) summary.edd?.let(::formatDmy).orEmpty() else ""
        listOfNotNull(
            ga.takeIf { it.isNotEmpty() }?.let { "GA: $it" },
            edd.takeIf { it.isNotEmpty() }?.let { "EDD: $it" },
        ).joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
            add(ShareLine(it, ShareLineRole.CLINICAL))
        }

        reportVisits(patient, 1).rows.lastOrNull()?.let { visit ->
            val visitDate = formatDmy(visit.date)?.takeIf { it.isNotEmpty() }
                ?: singleLine(visit.date).takeIf { it.isNotEmpty() }
            val bp = singleLine(visit.bloodPressure).takeIf { it.isNotEmpty() }?.let { "BP $it" }
            val weight = formatWeightKg(visit.weight)?.let(::singleLine)?.takeIf { it.isNotEmpty() }
                ?.let { "Wt $it" }
            val parts = listOfNotNull(visitDate, bp, weight)
            if (parts.isNotEmpty()) {
                add(ShareLine("Last visit: ${parts.joinToString(" · ")}", ShareLineRole.CLINICAL))
            }
        }

        latestScan(patient)?.let { scan ->
            val outcome = scanOutcomeFor(scan)
            val scanDate = formatDmy(scan.date)?.takeIf { it.isNotEmpty() }
                ?: singleLine(scan.date).takeIf { it.isNotEmpty() }
            val typeAndPres = listOfNotNull(
                outcome.typeLabel?.let(::singleLine)?.takeIf { it.isNotEmpty() },
                outcome.presentation?.let(::singleLine)?.takeIf { it.isNotEmpty() },
            ).joinToString(", ").takeIf { it.isNotEmpty() }
            val measurements = outcome.measurements
                .mapNotNull { (label, value) ->
                    singleLine(value).takeIf { it.isNotEmpty() }?.let { "$label $it" }
                }
                .joinToString(", ")
                .takeIf { it.isNotEmpty() }
            val bodyParts = listOfNotNull(typeAndPres, measurements)
            if (bodyParts.isNotEmpty()) {
                val allParts = listOfNotNull(scanDate) + bodyParts
                add(ShareLine("Latest scan: ${allParts.joinToString(" · ")}", ShareLineRole.CLINICAL))
            }
        }

        val currentMedNames = reportMedications(patient).current
            .map { singleLine(it.name) }
            .filter { it.isNotEmpty() }
        if (currentMedNames.isNotEmpty()) {
            add(ShareLine(formatMedicationsLine(currentMedNames), ShareLineRole.MEDICATIONS))
        }

        val deliveryRows = reportDeliveryRows(patient, edd.takeIf { it.isNotEmpty() })
        for (row in deliveryRows) {
            if (row.label == "Estimated due date") continue
            val value = singleLine(row.value)
            if (value.isNotEmpty() && value != NOT_RECORDED) {
                val role = if (isDeliverySafetyRow(row.label)) {
                    ShareLineRole.SAFETY
                } else {
                    ShareLineRole.DELIVERY_PLAN
                }
                add(ShareLine("${row.label}: $value", role))
            }
        }
    }
    return compactShareLines(entries).joinToString("\n")
}
