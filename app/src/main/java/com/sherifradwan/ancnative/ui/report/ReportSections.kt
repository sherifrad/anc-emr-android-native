package com.sherifradwan.ancnative.ui.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.GestationalAgeResult
import com.sherifradwan.ancnative.model.MEDICATION_LABEL_ACTIVE
import com.sherifradwan.ancnative.model.bpLevel
import com.sherifradwan.ancnative.model.datingDiscordanceLine
import com.sherifradwan.ancnative.model.datingSummaryOf
import com.sherifradwan.ancnative.model.datingTextRows
import com.sherifradwan.ancnative.model.deliveryOutcomeLine
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.model.isEarlyLoss
import com.sherifradwan.ancnative.model.isStillbirth
import com.sherifradwan.ancnative.model.medicationInstructions
import com.sherifradwan.ancnative.model.medicationStatusLabel
import com.sherifradwan.ancnative.model.parseDeliveryNotes
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.ui.AllergyState
import com.sherifradwan.ancnative.ui.PatientRiskTier
import com.sherifradwan.ancnative.ui.StatusIncompleteBg
import com.sherifradwan.ancnative.ui.StatusIncompleteBorder
import com.sherifradwan.ancnative.ui.StatusIncompleteDarkBg
import com.sherifradwan.ancnative.ui.StatusIncompleteDarkBorder
import com.sherifradwan.ancnative.ui.StatusIncompleteDarkText
import com.sherifradwan.ancnative.ui.StatusIncompleteText
import com.sherifradwan.ancnative.ui.StatusModerateBg
import com.sherifradwan.ancnative.ui.StatusModerateBorder
import com.sherifradwan.ancnative.ui.StatusModerateDarkBg
import com.sherifradwan.ancnative.ui.StatusModerateDarkBorder
import com.sherifradwan.ancnative.ui.StatusModerateDarkText
import com.sherifradwan.ancnative.ui.StatusModerateText
import com.sherifradwan.ancnative.ui.classifyAllergy
import com.sherifradwan.ancnative.ui.components.BandChip
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.LabeledValue
import com.sherifradwan.ancnative.ui.components.LabeledValueRow
import com.sherifradwan.ancnative.ui.components.PatientNameText
import com.sherifradwan.ancnative.ui.components.PlainField
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordSpacing
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusKind
import com.sherifradwan.ancnative.ui.components.StatusTag
import com.sherifradwan.ancnative.ui.components.StatusTone
import com.sherifradwan.ancnative.ui.components.StatusWord
import com.sherifradwan.ancnative.ui.components.currentFieldFamily
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.formatAgeYears
import com.sherifradwan.ancnative.ui.formatWeightKg
import com.sherifradwan.ancnative.ui.isDarkTheme
import com.sherifradwan.ancnative.ui.isRhNegative
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

internal fun reportSortKey(date: String?): java.time.LocalDate =
    parseIsoDateOrNull(date) ?: LocalDate.MAX

/** Newest kept oldest first; hidden counts the older rows left out. */
private fun <T> keepNewest(rows: List<T>, limit: Int, dateOf: (T) -> String?): Limited<T> {
    val ordered = rows.sortedBy { reportSortKey(dateOf(it)) }
    if (ordered.size <= limit) return Limited(ordered, 0)
    return Limited(ordered.takeLast(limit), ordered.size - limit)
}

internal fun reportVisits(patient: DemoPatient, limit: Int): Limited<DemoVisit> =
    keepNewest(patient.visits, limit) { it.date }

internal fun reportScans(patient: DemoPatient, limit: Int): Limited<DemoUltrasoundScan> =
    keepNewest(patient.ultrasoundScans, limit) { it.date }

internal fun reportLabs(patient: DemoPatient, limit: Int): Limited<DemoLabInvestigation> {
    val ordered = patient.labInvestigations.sortedByDescending { reportSortKey(it.date) }
    if (ordered.size <= limit) return Limited(ordered, 0)
    return Limited(ordered.take(limit), ordered.size - limit)
}

internal fun reportMedications(patient: DemoPatient): ReportMedications {
    val (current, stopped) = patient.medications.partition {
        medicationStatusLabel(it.status, it.isNonCompliant) == MEDICATION_LABEL_ACTIVE
    }
    return ReportMedications(
        current = current.sortedByDescending { reportSortKey(it.startDate) },
        stopped = stopped.sortedByDescending { reportSortKey(it.startDate) },
    )
}

internal fun reportRisk(riskLevel: String?): ReportRisk =
    when (parseRiskTier(riskLevel)) {
        PatientRiskTier.HIGH -> ReportRisk.HIGH
        PatientRiskTier.MEDIUM -> ReportRisk.MEDIUM
        PatientRiskTier.LOW -> ReportRisk.LOW
        PatientRiskTier.UNRECORDED -> ReportRisk.UNRECORDED
    }

/** A risk text the app cannot place in a tier is printed as written, never as "not recorded". */
internal fun reportRiskLabel(riskLevel: String?): String {
    val risk = reportRisk(riskLevel)
    val written = riskLevel?.trim().orEmpty()
    return if (risk == ReportRisk.UNRECORDED && written.isNotEmpty()) written else risk.label
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

/** The stored frequency is a legacy default and is never printed; the note carries the real schedule. */
internal fun medicationDetail(m: DemoMedicationRecord): String {
    val parts = medicationInstructions(m.dosage, m.route, m.notes)
        .split(" · ")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toMutableList()
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
    if (result.isNullOrBlank()) return NOT_RECORDED
    val unit = lab.unit?.trim()
    return if (!unit.isNullOrBlank()) "$result $unit" else result
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

internal fun reportDeliveryRows(patient: DemoPatient, eddText: String?): List<ReportRow> = buildList {
    if (patient.delivered) {
        patient.deliveryOutcomeLine()?.let { add(ReportRow("Delivery Outcome", it)) }
    }
    val planRows = deliveryPlanRows(patient)
    addAll(planRows)
    if (isRhNegative(patient.bloodGroupText)) {
        add(ReportRow("Blood bank precautions", "Anti-D Prophylaxis + Matched Packed RBCs Cross-match"))
    }
    if (!patient.delivered && !eddText.isNullOrBlank()) {
        add(ReportRow("Estimated due date", eddText))
    }
    if (!patient.delivered && planRows.isEmpty()) {
        add(ReportRow("Plan", NOT_RECORDED))
    }
}

internal const val MAX_REPORT_VISITS = 12
internal const val MAX_REPORT_SCANS = 7
internal const val MAX_REPORT_LABS = 10
private const val CLINIC_PHONE = "01201720808"
private const val ROW_STACK_FONT_SCALE = 1.3f

internal fun reportPreviousPregnancyOutcome(pp: DemoPreviousPregnancy): Pair<String, Boolean> {
    val outcomeText = pp.outcomeText?.takeUnless(String::isBlank)
    val outcomeLabel = when (pp.outcome) {
        "Stillbirth" -> outcomeText ?: "Stillbirth / IUFD"
        "EarlyPregnancyLoss" -> outcomeText ?: "Miscarriage"
        "TerminationOfPregnancy" -> outcomeText ?: "Therapeutic Termination"
        "LiveBirth" -> outcomeText ?: "Live Birth"
        else -> outcomeText ?: pp.outcome?.takeUnless(String::isBlank) ?: "-"
    }
    val isLoss = pp.outcome == "TerminationOfPregnancy" || isStillbirth(pp) || isEarlyLoss(pp)
    return outcomeLabel to isLoss
}

internal fun reportPreviousPregnancyTypeAndMode(pp: DemoPreviousPregnancy): String {
    val methodText = pp.birthMethod?.takeUnless(String::isBlank)
        ?: pp.deliveryBy?.takeUnless(String::isBlank)
        ?: pp.lossManagement?.takeUnless(String::isBlank)
        ?: pp.terminationMethod?.takeUnless(String::isBlank)
        ?: "-"
    return listOfNotNull(
        pp.pregnancyType?.takeUnless(String::isBlank),
        methodText.takeUnless { it == "-" },
    ).joinToString(" · ").ifEmpty { "-" }
}

internal fun reportPreviousPregnancyNotes(pp: DemoPreviousPregnancy): String =
    listOfNotNull(
        pp.placeAndDoctor?.takeUnless(String::isBlank),
        pp.otherDetails?.takeUnless(String::isBlank),
    ).joinToString(" · ").ifEmpty { NOT_RECORDED }

@Composable
internal fun ReportPageOneSections(
    patient: DemoPatient,
    securitySettings: SecuritySettingsState?,
    asOfDate: LocalDate = LocalDate.now(),
) {
    ReportClinicMetaCard(
        patient = patient,
        securitySettings = securitySettings,
        asOfDate = asOfDate,
    )
    ReportPatientIdentityCard(patient = patient)
    ReportPregnancyDatingCard(patient = patient, asOfDate = asOfDate)
    ReportHistoryCard(patient = patient)
    ReportPreviousPregnanciesCard(patient = patient)
    ReportVisitsCard(patient = patient)
}

@Composable
internal fun ReportPageTwoSections(
    patient: DemoPatient,
    securitySettings: SecuritySettingsState?,
    asOfDate: LocalDate = LocalDate.now(),
) {
    ReportPageTwoHeaderCard(patient = patient, asOfDate = asOfDate)
    ReportScansCard(patient = patient)
    ReportLabsCard(patient = patient)
    ReportMedicationSections(patient = patient)
    ReportDeliveryPlanCard(patient = patient, asOfDate = asOfDate)
    ReportAttestationCard(securitySettings = securitySettings)
}

@Composable
private fun reportClinicTitle(securitySettings: SecuritySettingsState?): String =
    securitySettings?.clinicName?.takeUnless { it.isBlank() } ?: stringResource(R.string.app_name)

@Composable
private fun reportShortGestationalAge(result: GestationalAgeResult): String =
    if (result.isPostTerm) {
        stringResource(R.string.report_ga_post_term_format, result.shortWeeksText)
    } else {
        result.shortWeeksText
    }

@Composable
private fun ReportClinicMetaCard(
    patient: DemoPatient,
    securitySettings: SecuritySettingsState?,
    asOfDate: LocalDate,
) {
    val clinicTitle = reportClinicTitle(securitySettings)
    val clinicSubtitle = stringResource(R.string.report_clinic_subtitle_full)
    val printDate = remember(asOfDate) { formatDmy(asOfDate) }
    val statusLine = remember(patient) { reportStatusLine(patient) }

    RecordSectionCard(title = null, family = FieldFamilies.patient()) {
        ReportHeaderStripRow(
            title = clinicTitle,
            subtitle = clinicSubtitle,
            badgeText = stringResource(R.string.report_badge_clinical_record),
        )
        ReportTwoUpLabeledValues(
            firstLabel = stringResource(R.string.report_patient_id_label),
            firstValue = ltrIsolate(patient.caseId),
            secondLabel = stringResource(R.string.report_print_date_label),
            secondValue = ltrIsolate(printDate),
        )
        statusLine?.let { status ->
            Surface(
                shape = RecordShapes.Small,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = status,
                    style = RecordType.Action,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportPatientIdentityCard(patient: DemoPatient) {
    val risk = remember(patient.riskLevel) { reportRisk(patient.riskLevel) }
    val riskLabel = remember(patient.riskLevel) { reportRiskLabel(patient.riskLevel) }
    val riskKind = when (risk) {
        ReportRisk.HIGH -> StatusKind.HighRisk
        ReportRisk.MEDIUM -> StatusKind.ModerateRisk
        ReportRisk.LOW, ReportRisk.UNRECORDED -> StatusKind.Incomplete
    }
    val rhNeg = remember(patient.bloodGroupText) { isRhNegative(patient.bloodGroupText) }
    val trimmedAge = patient.ageText?.trim()?.takeIf { it.isNotEmpty() }
    val ageYears = trimmedAge?.let(::formatAgeYears)?.toIntOrNull()
    val ageDisplay = when {
        ageYears != null -> ltrIsolate(stringResource(R.string.report_age_years_format, ageYears))
        trimmedAge != null -> ltrIsolate(trimmedAge)
        else -> null
    }
    val bloodGroupDisplay = patient.bloodGroupText?.trim()?.takeIf { it.isNotEmpty() }?.let(::ltrIsolate)
    val phoneDisplay = patient.phone?.trim()?.takeIf { it.isNotEmpty() }?.let(::ltrIsolate)

    RecordSectionCard(
        title = stringResource(R.string.report_section_patient_identity),
        family = FieldFamilies.patient(),
    ) {
        PatientNameText(
            name = patient.displayName,
            style = RecordType.SectionTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusTag(kind = riskKind, text = riskLabel, wrap = true)
            if (rhNeg) {
                StatusTag(
                    kind = StatusKind.HighRisk,
                    text = stringResource(R.string.rh_negative_tag),
                    wrap = true,
                )
            }
        }
        ReportTwoUpLabeledValues(
            firstLabel = stringResource(R.string.age_label),
            firstValue = ageDisplay,
            secondLabel = stringResource(R.string.blood_group),
            secondValue = bloodGroupDisplay,
            secondEmphasis = true,
        )
        LabeledValue(
            label = stringResource(R.string.report_phone_label),
            value = phoneDisplay,
            missingText = stringResource(R.string.report_phone_unspecified),
            modifier = Modifier.fillMaxWidth(),
        )
        if (!patient.addressText.isNullOrBlank()) {
            LabeledValue(
                label = stringResource(R.string.address),
                value = patient.addressText.trim(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!patient.dateOfBirth.isNullOrBlank()) {
            LabeledValue(
                label = stringResource(R.string.report_dob_label),
                value = ltrIsolate(reportDate(patient.dateOfBirth)),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!patient.clinicalAttention.isNullOrBlank()) {
            ReportModerateNoteBox(
                label = stringResource(R.string.clinical_attention),
                value = patient.clinicalAttention.trim(),
            )
        }
    }
}

@Composable
private fun ReportPregnancyDatingCard(
    patient: DemoPatient,
    asOfDate: LocalDate,
) {
    val datingSummary = remember(patient, asOfDate) { datingSummaryOf(patient, asOfDate) }
    val gaResult = remember(patient.dating, patient.delivered, asOfDate) {
        if (patient.delivered) null else patient.dating?.calculateGestationalAgeResult(asOfDate)
    }
    val discordance = remember(patient.dating, datingSummary) {
        datingDiscordanceLine(patient.dating, datingSummary)
    }

    RecordSectionCard(
        title = stringResource(R.string.report_section_pregnancy_dating),
        family = FieldFamilies.patient(),
    ) {
        when {
            gaResult != null -> {
                LabeledValue(
                    label = stringResource(R.string.report_ga_today_label),
                    value = ltrIsolate(reportShortGestationalAge(gaResult)),
                    emphasis = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            patient.delivered -> {
                LabeledValue(
                    label = stringResource(R.string.delivery_outcome_section),
                    value = patient.deliveryOutcomeLine(),
                    emphasis = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {
                LabeledValue(
                    label = stringResource(R.string.gestational_age),
                    value = null,
                    missingText = stringResource(R.string.report_ga_not_recorded),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (patient.dating != null || !patient.delivered) {
            val rows = datingTextRows(datingSummary)
            val originRow = rows.firstOrNull { !it.isEdd && it.label != "Method" }
            val eddRow = rows.firstOrNull { it.isEdd }
            val methodRow = rows.firstOrNull { it.label == "Method" }

            if (originRow != null && eddRow != null) {
                ReportTwoUpLabeledValues(
                    firstLabel = originRow.label,
                    firstValue = originRow.value?.let(::ltrIsolate),
                    firstEmphasis = originRow.isEdd,
                    secondLabel = eddRow.label,
                    secondValue = eddRow.value?.let(::ltrIsolate),
                    secondEmphasis = eddRow.isEdd,
                )
            } else if (eddRow != null) {
                LabeledValue(
                    label = eddRow.label,
                    value = eddRow.value?.let(::ltrIsolate),
                    emphasis = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            methodRow?.let { row ->
                LabeledValue(
                    label = row.label,
                    value = row.value,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            discordance?.let { line ->
                ReportModerateNoteBox(value = ltrIsolate(line))
            }
        }
    }
}

@Composable
private fun ReportHistoryCard(patient: DemoPatient) {
    val g = patient.gravida ?: "-"
    val p = patient.parity ?: "-"
    val a = patient.abortion ?: "-"
    val pastDeliverySummary = remember(patient) { previousDeliverySummary(patient) }
    val gpaFormula = stringResource(
        R.string.report_gpa_value_format,
        ltrIsolate("G$g P$p + A$a"),
        ltrIsolate(pastDeliverySummary),
    )
    val allergyState = remember(patient.allergyHistory) { classifyAllergy(patient.allergyHistory) }

    RecordSectionCard(
        title = stringResource(R.string.report_section_history),
        family = FieldFamilies.background(),
    ) {
        LabeledValue(
            label = stringResource(R.string.report_gpa_label),
            value = gpaFormula,
            emphasis = true,
            family = FieldFamilies.obstetric(),
            modifier = Modifier.fillMaxWidth(),
        )
        when (allergyState) {
            is AllergyState.Recorded -> ReportErrorAlertBox(
                label = stringResource(R.string.report_drug_allergies_label),
                value = allergyState.text,
            )
            AllergyState.ExplicitlyNone -> LabeledValue(
                label = stringResource(R.string.report_drug_allergies_label),
                value = patient.allergyHistory?.trim(),
                modifier = Modifier.fillMaxWidth(),
            )
            AllergyState.NotRecorded -> LabeledValue(
                label = stringResource(R.string.report_drug_allergies_label),
                value = null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LabeledValue(
            label = stringResource(R.string.medical_history),
            value = patient.medicalHistory?.trim()?.takeIf { it.isNotEmpty() },
            modifier = Modifier.fillMaxWidth(),
        )
        LabeledValue(
            label = stringResource(R.string.surgical_history),
            value = patient.surgicalHistory?.trim()?.takeIf { it.isNotEmpty() },
            modifier = Modifier.fillMaxWidth(),
        )
        LabeledValue(
            label = stringResource(R.string.family_history),
            value = patient.familyHistory?.trim()?.takeIf { it.isNotEmpty() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportPreviousPregnanciesCard(patient: DemoPatient) {
    val pastSummary = remember(patient) { previousDeliverySummary(patient) }

    RecordSectionCard(
        title = stringResource(R.string.report_section_previous_pregnancies),
        family = FieldFamilies.obstetric(),
    ) {
        if (patient.previousPregnancies.isEmpty()) {
            Text(
                text = stringResource(R.string.report_empty_previous_pregnancies_format, pastSummary),
                style = RecordType.Missing,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ReportHairlineList(items = patient.previousPregnancies) { _, pp ->
                val (outcomeLabel, isLoss) = reportPreviousPregnancyOutcome(pp)
                val dateFormatted = formatDmy(pp.dateText) ?: "-"
                val typeAndMode = reportPreviousPregnancyTypeAndMode(pp)
                val notesLine = reportPreviousPregnancyNotes(pp)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ltrIsolate(dateFormatted),
                        style = RecordType.CardName,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    BandChip(
                        text = outcomeLabel,
                        tone = if (isLoss) StatusTone.High else null,
                        tinted = true,
                        wrap = true,
                    )
                }
                Text(
                    text = typeAndMode,
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = notesLine,
                    style = if (notesLine == NOT_RECORDED) {
                        RecordType.Secondary.copy(fontStyle = FontStyle.Italic)
                    } else {
                        RecordType.Secondary
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportVisitsCard(patient: DemoPatient) {
    val visits = remember(patient) { reportVisits(patient, MAX_REPORT_VISITS) }

    RecordSectionCard(
        title = stringResource(R.string.report_section_visits),
        family = FieldFamilies.patient(),
    ) {
        if (visits.rows.isEmpty()) {
            Text(
                text = stringResource(R.string.report_empty_visits),
                style = RecordType.Missing,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            if (visits.hidden > 0) {
                ReportHiddenCountNotice(
                    text = stringResource(R.string.report_hidden_visits_format, visits.hidden),
                )
            }
            ReportHairlineList(items = visits.rows) { idx, visit ->
                val visitNumber = stringResource(R.string.report_visit_number_format, idx + 1 + visits.hidden)
                val visitGaResult = remember(patient.dating, visit.date) {
                    parseIsoDateOrNull(visit.date)?.let { vDate ->
                        patient.dating?.calculateGestationalAgeResult(vDate)
                    }
                }
                val visitGa = visitGaResult?.let { reportShortGestationalAge(it) }
                val bpRaw = visit.bloodPressure?.trim()?.takeIf { it.isNotEmpty() }
                val bpExtra: (@Composable () -> Unit)? = when (bpLevel(visit.bloodPressure)) {
                    BpLevel.SEVERE -> {
                        { StatusWord(stringResource(R.string.report_bp_severe), StatusTone.High) }
                    }
                    BpLevel.RAISED -> {
                        { StatusWord(stringResource(R.string.report_bp_raised), StatusTone.Moderate) }
                    }
                    BpLevel.NORMAL -> null
                }
                val weightDisplay = formatWeightKg(visit.weight)?.let(::ltrIsolate)
                val findingsText = visitText(visit)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BandChip(
                            text = ltrIsolate(visitNumber),
                            tone = null,
                            tinted = true,
                            wrap = true,
                        )
                        Text(
                            text = ltrIsolate(reportDate(visit.date)),
                            style = RecordType.CardName,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    BandChip(
                        text = visitGa?.let(::ltrIsolate) ?: stringResource(R.string.not_recorded),
                        tone = null,
                        tinted = true,
                        wrap = true,
                    )
                }
                ReportTwoColumnPlainFields(
                    first = { fieldModifier ->
                        PlainField(
                            label = stringResource(R.string.report_bp_label),
                            value = bpRaw?.let(::ltrIsolate) ?: "-",
                            modifier = fieldModifier,
                            emphasis = bpRaw != null,
                            extra = bpExtra,
                        )
                    },
                    second = { fieldModifier ->
                        PlainField(
                            label = stringResource(R.string.report_weight_label),
                            value = weightDisplay ?: "-",
                            modifier = fieldModifier,
                            emphasis = weightDisplay != null,
                        )
                    },
                )
                PlainField(
                    label = stringResource(R.string.report_findings_label),
                    value = if (findingsText == NOT_RECORDED) null else findingsText,
                    missingText = stringResource(R.string.not_recorded),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReportPageTwoHeaderCard(
    patient: DemoPatient,
    asOfDate: LocalDate,
) {
    val printDate = remember(asOfDate) { formatDmy(asOfDate) }
    val metaLine = listOf(
        stringResource(R.string.report_meta_pair_format, stringResource(R.string.report_patient_id_label), ltrIsolate(patient.caseId)),
        stringResource(R.string.report_meta_pair_format, stringResource(R.string.report_print_date_label), ltrIsolate(printDate)),
    ).joinToString(stringResource(R.string.detail_separator))

    RecordSectionCard(title = null, family = FieldFamilies.patient()) {
        ReportHeaderStripRow(
            title = stringResource(R.string.report_page2_subtitle),
            subtitle = metaLine,
            badgeText = stringResource(R.string.report_badge_continued),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportScansCard(patient: DemoPatient) {
    val scans = remember(patient) { reportScans(patient, MAX_REPORT_SCANS) }

    RecordSectionCard(
        title = stringResource(R.string.report_section_scans),
        family = FieldFamilies.obstetric(),
    ) {
        if (scans.rows.isEmpty()) {
            Text(
                text = stringResource(R.string.report_empty_scans),
                style = RecordType.Missing,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            if (scans.hidden > 0) {
                ReportHiddenCountNotice(
                    text = stringResource(R.string.report_hidden_scans_format, scans.hidden),
                )
            }
            ReportHairlineList(items = scans.rows) { _, scan ->
                val efw = scanWeight(scan)
                val notesText = scanNotes(scan)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ltrIsolate(reportDate(scan.date)),
                        style = RecordType.CardName,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS),
                    ) {
                        val scanGa = scanGestationalAge(scan)
                        BandChip(
                            text = if (scanGa == "-") stringResource(R.string.not_recorded) else ltrIsolate(scanGa),
                            tone = null,
                            tinted = true,
                            wrap = true,
                        )
                        if (scan.imageUris.isNotEmpty()) {
                            BandChip(
                                text = stringResource(R.string.report_scan_photos_format, scan.imageUris.size),
                                tone = null,
                                tinted = true,
                                wrap = true,
                            )
                        }
                    }
                }
                ReportTwoColumnPlainFields(
                    first = { fieldModifier ->
                        PlainField(
                            label = stringResource(R.string.report_presentation_label),
                            value = scan.presentation?.takeUnless(String::isBlank),
                            missingText = stringResource(R.string.not_recorded),
                            modifier = fieldModifier,
                        )
                    },
                    second = { fieldModifier ->
                        PlainField(
                            label = stringResource(R.string.report_placenta_label),
                            value = scan.placentalSite?.takeUnless(String::isBlank),
                            missingText = stringResource(R.string.not_recorded),
                            modifier = fieldModifier,
                        )
                    },
                )
                ReportTwoColumnPlainFields(
                    first = { fieldModifier ->
                        PlainField(
                            label = stringResource(R.string.report_amniotic_fluid_label),
                            value = scan.amnioticFluid?.takeUnless(String::isBlank),
                            missingText = stringResource(R.string.not_recorded),
                            modifier = fieldModifier,
                        )
                    },
                    second = { fieldModifier ->
                        PlainField(
                            label = stringResource(R.string.report_efw_label),
                            value = if (efw == "-") null else ltrIsolate(efw),
                            missingText = stringResource(R.string.not_recorded),
                            emphasis = efw != "-",
                            modifier = fieldModifier,
                        )
                    },
                )
                PlainField(
                    label = stringResource(R.string.report_scan_notes_label),
                    value = if (notesText == NOT_RECORDED) null else notesText,
                    missingText = stringResource(R.string.not_recorded),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReportLabsCard(patient: DemoPatient) {
    val labs = remember(patient) { reportLabs(patient, MAX_REPORT_LABS) }

    RecordSectionCard(
        title = stringResource(R.string.report_section_labs),
        family = FieldFamilies.background(),
    ) {
        if (labs.rows.isEmpty()) {
            Text(
                text = stringResource(R.string.report_empty_labs),
                style = RecordType.Missing,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            if (labs.hidden > 0) {
                ReportHiddenCountNotice(
                    text = stringResource(R.string.report_hidden_labs_format, labs.hidden),
                )
            }
            ReportHairlineList(items = labs.rows, itemSpacing = RecordSpacing.XS) { _, lab ->
                val resultText = labResult(lab)
                val isMissingResult = resultText == NOT_RECORDED
                val datePart = ltrIsolate(reportDate(lab.date))
                val refPart = lab.referenceRange?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    stringResource(R.string.report_lab_ref_format, ltrIsolate(it))
                }
                val dateRefLine = listOfNotNull(datePart, refPart).joinToString(stringResource(R.string.detail_separator))
                val extra = labExtra(lab)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RecordSpacing.S),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = lab.testName,
                        style = RecordType.CardName,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = if (isMissingResult) stringResource(R.string.not_recorded) else ltrIsolate(resultText),
                            style = if (isMissingResult) RecordType.Missing else RecordType.KeyValue,
                            color = when {
                                lab.isAbnormal -> MaterialTheme.colorScheme.error
                                isMissingResult -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            textAlign = TextAlign.End,
                        )
                        if (lab.isAbnormal) {
                            StatusWord(
                                text = stringResource(R.string.lab_abnormal),
                                tone = StatusTone.High,
                            )
                        }
                    }
                }
                Text(
                    text = dateRefLine,
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (extra.isNotBlank()) {
                    Text(
                        text = extra,
                        style = RecordType.Secondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportMedicationSections(patient: DemoPatient) {
    val meds = remember(patient) { reportMedications(patient) }

    RecordSectionCard(
        title = stringResource(R.string.report_section_current_meds),
        family = FieldFamilies.background(),
    ) {
        if (meds.current.isEmpty()) {
            Text(
                text = stringResource(R.string.report_empty_current_meds),
                style = RecordType.Missing,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ReportMedicationRows(meds = meds.current)
        }
    }

    if (meds.stopped.isNotEmpty()) {
        RecordSectionCard(
            title = stringResource(R.string.report_section_stopped_meds),
            family = FieldFamilies.background(),
        ) {
            ReportMedicationRows(meds = meds.stopped)
        }
    }
}

@Composable
private fun ReportMedicationRows(meds: List<DemoMedicationRecord>) {
    val labelColor = currentFieldFamily().label
    ReportHairlineList(items = meds, itemSpacing = RecordSpacing.XS) { _, med ->
        val detail = medicationDetail(med)
        val dates = medicationDates(med)

        Text(
            text = med.name,
            style = RecordType.CardName,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (detail.isNotEmpty()) {
            Text(
                text = detail,
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (dates.isNotEmpty()) {
            Text(
                text = ltrIsolate(dates),
                style = RecordType.FieldLabel,
                color = labelColor,
            )
        }
    }
}

@Composable
private fun ReportDeliveryPlanCard(
    patient: DemoPatient,
    asOfDate: LocalDate,
) {
    val datingSummary = remember(patient, asOfDate) { datingSummaryOf(patient, asOfDate) }
    val rows = remember(patient, datingSummary) {
        reportDeliveryRows(patient, datingSummary.edd?.let(::formatDmy))
    }

    RecordSectionCard(
        title = stringResource(R.string.report_section_delivery_plan),
        family = FieldFamilies.delivery(),
    ) {
        rows.forEach { row ->
            if (row.label == "Blood bank precautions") {
                ReportErrorAlertBox(
                    label = row.label,
                    value = row.value,
                )
            } else {
                val value = when {
                    row.value == NOT_RECORDED -> null
                    row.label == "Estimated due date" -> ltrIsolate(row.value)
                    else -> row.value
                }
                LabeledValue(
                    label = row.label,
                    value = value,
                    emphasis = row.label == "Estimated due date" || row.label == "Delivery Outcome",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReportAttestationCard(securitySettings: SecuritySettingsState?) {
    val clinicTitle = reportClinicTitle(securitySettings)
    RecordSectionCard(
        title = stringResource(R.string.report_section_attestation),
        family = FieldFamilies.patient(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS),
        ) {
            Text(
                text = stringResource(R.string.report_doctor_name_en),
                style = RecordType.CardName,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.report_doctor_specialty_en),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PatientNameText(
                name = stringResource(R.string.report_doctor_name_ar),
                style = RecordType.CardName,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
            PatientNameText(
                name = stringResource(R.string.report_doctor_specialty_ar),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.report_confidential_footer_format,
                    clinicTitle,
                    ltrIsolate(CLINIC_PHONE),
                ),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ReportTwoUpLabeledValues(
    firstLabel: String,
    firstValue: String?,
    firstEmphasis: Boolean = false,
    secondLabel: String,
    secondValue: String?,
    secondEmphasis: Boolean = false,
) {
    if (LocalDensity.current.fontScale > ROW_STACK_FONT_SCALE) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RecordSpacing.S),
        ) {
            LabeledValue(
                label = firstLabel,
                value = firstValue,
                emphasis = firstEmphasis,
                modifier = Modifier.fillMaxWidth(),
            )
            LabeledValue(
                label = secondLabel,
                value = secondValue,
                emphasis = secondEmphasis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        LabeledValueRow {
            LabeledValue(
                label = firstLabel,
                value = firstValue,
                emphasis = firstEmphasis,
                stretch = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            LabeledValue(
                label = secondLabel,
                value = secondValue,
                emphasis = secondEmphasis,
                stretch = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ReportTwoColumnPlainFields(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    if (LocalDensity.current.fontScale > ROW_STACK_FONT_SCALE) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RecordSpacing.S),
        ) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RecordSpacing.M),
        ) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReportErrorAlertBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RecordShapes.Small,
        color = scheme.errorContainer,
        border = BorderStroke(1.5.dp, scheme.error),
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = RecordType.FieldLabel.copy(fontWeight = FontWeight.Bold),
                color = scheme.error,
            )
            Text(
                text = value,
                style = RecordType.KeyValue,
                color = scheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun ReportModerateNoteBox(
    value: String,
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    val dark = isDarkTheme()
    val bg = if (dark) StatusModerateDarkBg else StatusModerateBg
    val border = if (dark) StatusModerateDarkBorder else StatusModerateBorder
    val fg = if (dark) StatusModerateDarkText else StatusModerateText
    Surface(
        shape = RecordShapes.Small,
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (label != null) {
                Text(
                    text = label,
                    style = RecordType.FieldLabel.copy(fontWeight = FontWeight.Bold),
                    color = fg,
                )
            }
            Text(
                text = value,
                style = if (label != null) RecordType.FieldValue else RecordType.Action,
                color = fg,
            )
        }
    }
}

@Composable
private fun ReportHiddenCountNotice(text: String) {
    val dark = isDarkTheme()
    Surface(
        shape = RecordShapes.Small,
        color = if (dark) StatusIncompleteDarkBg else StatusIncompleteBg,
        border = BorderStroke(1.dp, if (dark) StatusIncompleteDarkBorder else StatusIncompleteBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = RecordType.FieldLabel,
            color = if (dark) StatusIncompleteDarkText else StatusIncompleteText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ReportHeaderStripRow(
    title: String,
    subtitle: String,
    badgeText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RecordSpacing.S),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = RecordType.CardName,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BandChip(
            text = badgeText,
            tone = null,
            tinted = true,
            wrap = true,
        )
    }
}

@Composable
private fun <T> ReportHairlineList(
    items: List<T>,
    itemSpacing: Dp = RecordSpacing.S,
    itemContent: @Composable (Int, T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { idx, item ->
            if (idx > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            val topPadding = if (idx == 0) 0.dp else 10.dp
            val bottomPadding = if (idx == items.lastIndex) 0.dp else 10.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPadding, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                itemContent(idx, item)
            }
        }
    }
}
