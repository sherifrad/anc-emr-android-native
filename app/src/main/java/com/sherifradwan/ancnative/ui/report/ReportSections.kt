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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.sherifradwan.ancnative.ui.StatusHighBorder
import com.sherifradwan.ancnative.ui.StatusHighDarkBorder
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
private const val ROW_STACK_FONT_SCALE = 1.3f

internal data class ReportAttestationValues(
    val doctorNameEn: String,
    val doctorSpecialtyEn: String,
    val doctorNameAr: String,
    val doctorSpecialtyAr: String,
    val clinicPhone: String,
)

internal fun SecuritySettingsState?.resolvedAttestation(): ReportAttestationValues = ReportAttestationValues(
    doctorNameEn = this?.doctorNameEn?.takeUnless(String::isBlank)
        ?: SecuritySettingsState.DEFAULT_DOCTOR_NAME_EN,
    doctorSpecialtyEn = this?.doctorSpecialtyEn?.takeUnless(String::isBlank)
        ?: SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_EN,
    doctorNameAr = this?.doctorNameAr?.takeUnless(String::isBlank)
        ?: SecuritySettingsState.DEFAULT_DOCTOR_NAME_AR,
    doctorSpecialtyAr = this?.doctorSpecialtyAr?.takeUnless(String::isBlank)
        ?: SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_AR,
    clinicPhone = this?.clinicPhone?.takeUnless(String::isBlank)
        ?: SecuritySettingsState.DEFAULT_CLINIC_PHONE,
)

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
    figures: List<Figure> = emptyList(),
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
    ReportVisitsCard(patient = patient, figures = figures)
}

@Composable
internal fun ReportPageTwoSections(
    patient: DemoPatient,
    securitySettings: SecuritySettingsState?,
    asOfDate: LocalDate = LocalDate.now(),
    figures: List<Figure> = emptyList(),
) {
    val shownVisits = remember(patient) { reportVisits(patient, MAX_REPORT_VISITS).rows }
    ReportPageTwoHeaderCard(patient = patient, asOfDate = asOfDate)
    ReportOtherScansCard(patient = patient, shownVisits = shownVisits, figures = figures)
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
private fun ReportVisitsCard(
    patient: DemoPatient,
    figures: List<Figure> = emptyList(),
) {
    val visits = remember(patient) { reportVisits(patient, MAX_REPORT_VISITS) }

    RecordSectionCard(title = stringResource(R.string.report_section_visits), family = FieldFamilies.patient()) {
        if (visits.rows.isEmpty()) {
            Text(stringResource(R.string.report_empty_visits), style = RecordType.Missing, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            if (visits.hidden > 0) {
                ReportHiddenCountNotice(text = stringResource(R.string.report_hidden_visits_format, visits.hidden))
            }
            ReportHairlineList(items = visits.rows) { idx, visit ->
                val visitNumber = stringResource(R.string.report_visit_number_format, idx + 1 + visits.hidden)
                val visitGa = remember(patient.dating, visit.date) {
                    parseIsoDateOrNull(visit.date)?.let { patient.dating?.calculateGestationalAgeResult(it) }
                }?.let { reportShortGestationalAge(it) }
                val bpRaw = visit.bloodPressure?.trim()?.takeIf { it.isNotEmpty() }
                val bpExtra: (@Composable () -> Unit)? = when (bpLevel(visit.bloodPressure)) {
                    BpLevel.SEVERE -> { { StatusWord(stringResource(R.string.report_bp_severe), StatusTone.High) } }
                    BpLevel.RAISED -> { { StatusWord(stringResource(R.string.report_bp_raised), StatusTone.Moderate) } }
                    BpLevel.NORMAL -> null
                }
                val weightDisplay = formatWeightKg(visit.weight)?.let(::ltrIsolate)
                val linkedScan = remember(patient, visit) { latestScanForVisit(patient, visit) }
                val combinedFigRefs = remember(linkedScan, visit.id, figures) {
                    (linkedScan?.let { figureRefsFor(it.id, figures) }.orEmpty() + figureRefsFor(visit.id, figures))
                        .distinct().sorted()
                }
                val dash = stringResource(R.string.report_dash)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        BandChip(text = ltrIsolate(visitNumber), tone = null, tinted = true, wrap = true)
                        Text(ltrIsolate(reportDate(visit.date)), style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface)
                    }
                    BandChip(text = visitGa?.let(::ltrIsolate) ?: stringResource(R.string.not_recorded), tone = null, tinted = true, wrap = true)
                }
                ReportTwoColumnPlainFields(
                    first = { fieldModifier ->
                        PlainField(stringResource(R.string.report_bp_label), bpRaw?.let(::ltrIsolate) ?: dash, fieldModifier, emphasis = bpRaw != null, extra = bpExtra)
                    },
                    second = { fieldModifier ->
                        PlainField(stringResource(R.string.report_weight_label), weightDisplay ?: dash, fieldModifier, emphasis = weightDisplay != null)
                    },
                )
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("reportScanOutcome_${visit.id}").semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(stringResource(R.string.report_scan_outcome_label), style = RecordType.MetricLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ReportScanOutcomeContent(scan = linkedScan, figRefs = combinedFigRefs, visitNote = visitNote(visit))
                }
            }
        }
    }
}

@Composable
private fun ReportPageTwoHeaderCard(patient: DemoPatient, asOfDate: LocalDate) {
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
private fun ReportOtherScansCard(
    patient: DemoPatient,
    shownVisits: List<DemoVisit>,
    figures: List<Figure> = emptyList(),
) {
    val unlinked = remember(patient, shownVisits) { unlinkedScans(patient, shownVisits) }
    if (unlinked.isEmpty()) return

    RecordSectionCard(
        title = stringResource(R.string.report_section_other_scans),
        family = FieldFamilies.obstetric(),
        modifier = Modifier.testTag("reportOtherScans"),
    ) {
        ReportHairlineList(items = unlinked) { _, scan ->
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ltrIsolate(reportDate(scan.date)), style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface)
                val scanGa = scanGestationalAge(scan)
                BandChip(
                    text = if (scanGa == "-") stringResource(R.string.not_recorded) else ltrIsolate(scanGa),
                    tone = null,
                    tinted = true,
                    wrap = true,
                )
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ReportScanOutcomeContent(scan = scan, figRefs = figureRefsFor(scan.id, figures))
            }
        }
    }
}

/** Shared compact scan outcome layout used in both Visits and Other scans cards. */
@Composable
private fun ReportScanOutcomeContent(
    scan: DemoUltrasoundScan?,
    figRefs: List<Int>,
    visitNote: String? = null,
) {
    val outcome = remember(scan) { scan?.let(::scanOutcomeFor) }
    val detailSep = stringResource(R.string.detail_separator)
    val figRefsText = if (figRefs.isNotEmpty()) {
        figRefs.map { stringResource(R.string.report_figure_ref_format, it) }.joinToString(", ")
    } else {
        null
    }
    val hasPrimaryLine = (outcome != null && (outcome.typeLabel != null || outcome.presentation != null)) || figRefsText != null
    val hasMeasurements = outcome != null && outcome.measurements.isNotEmpty()
    val hasExtras = outcome != null && outcome.extras.isNotEmpty()

    if (!hasPrimaryLine && !hasMeasurements && !hasExtras) {
        Text(stringResource(R.string.report_dash), style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        if (hasPrimaryLine) {
            val primaryText = buildAnnotatedString {
                var hasPrevious = false
                outcome?.typeLabel?.let {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it) }
                    hasPrevious = true
                }
                outcome?.presentation?.let {
                    if (hasPrevious) append(", ")
                    append(it)
                    hasPrevious = true
                }
                if (figRefsText != null) {
                    if (hasPrevious) append(detailSep)
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(figRefsText) }
                }
            }
            Text(primaryText, style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurface)
        }
        if (hasMeasurements) {
            val measurementsLine = outcome!!.measurements.joinToString(", ") { (label, value) -> ltrIsolate("$label $value") }
            Text(measurementsLine, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (hasExtras) {
            Text(outcome!!.extras.joinToString(detailSep), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    visitNote?.let { note ->
        Text(stringResource(R.string.report_visit_note_format, note), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReportLabsCard(patient: DemoPatient) {
    val report = remember(patient) { labReport(patient) }
    val comparisonGroups = remember(report) { groupLabComparisonsByCategory(report.comparisons) }

    RecordSectionCard(title = stringResource(R.string.report_section_labs), family = FieldFamilies.background()) {
        if (comparisonGroups.isEmpty() && report.singles.isEmpty()) {
            Text(stringResource(R.string.report_empty_labs), style = RecordType.Missing, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            comparisonGroups.forEachIndexed { index, group ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ReportLabComparisonGroup(group = group)
            }
            if (report.singles.isNotEmpty()) {
                if (comparisonGroups.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ReportSingleLabsSection(singles = report.singles, notes = report.notes)
            }
            Text(stringResource(R.string.report_labs_legend), style = RecordType.FieldLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportLabComparisonGroup(group: LabComparisonGroup) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("reportLabComparison_${group.category}"),
        verticalArrangement = Arrangement.spacedBy(RecordSpacing.S),
    ) {
        Text(
            text = group.category,
            style = RecordType.CardName,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        ReportHairlineList(items = group.rows, itemSpacing = RecordSpacing.XS) { _, row ->
            Text(row.label, style = RecordType.KeyValue, color = MaterialTheme.colorScheme.onSurface)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RecordSpacing.S),
                verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS),
            ) {
                row.entries.forEach { (dateLabel, cell) -> ReportLabDateValueChip(dateLabel = dateLabel, cell = cell) }
            }
        }
        if (group.notes.isNotEmpty()) ReportLabNotesBlock(notes = group.notes)
    }
}

@Composable
private fun ReportLabDateValueChip(dateLabel: String, cell: LabCell) {
    val family = currentFieldFamily()
    val hasAbnormal = cell.flag != null || cell.abnormalNoDirection ||
        cell.entries.any { it.flag != null || it.abnormalNoDirection }
    val dark = isDarkTheme()
    val dash = stringResource(R.string.report_dash)

    Surface(
        shape = RecordShapes.Small,
        color = family.container,
        border = if (hasAbnormal) BorderStroke(1.dp, if (dark) StatusHighDarkBorder else StatusHighBorder) else null,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (dateLabel.isBlank()) dash else ltrIsolate(dateLabel), style = RecordType.FieldLabel, color = family.label)
            ReportLabCellValueText(cell = cell, style = RecordType.Secondary)
        }
    }
}

@Composable
private fun ReportLabCellValueText(cell: LabCell, style: TextStyle) {
    val entries = cell.entries.ifEmpty {
        listOf(LabCellEntry(value = cell.value, flag = cell.flag, abnormalNoDirection = cell.abnormalNoDirection))
    }
    val errorColor = MaterialTheme.colorScheme.error
    val normalColor = MaterialTheme.colorScheme.onSurface
    val annotated = buildAnnotatedString {
        append("\u2066")
        entries.forEachIndexed { index, entry ->
            if (index > 0) withStyle(SpanStyle(color = normalColor)) { append(" / ") }
            val mark = entry.flagMark()
            if (mark != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = errorColor)) { append("${entry.value} $mark") }
            } else {
                withStyle(SpanStyle(color = normalColor)) { append(entry.value) }
            }
        }
        append("\u2069")
    }
    Text(text = annotated, style = style)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportSingleLabsSection(singles: List<SingleLab>, notes: List<LabNote>) {
    val dash = stringResource(R.string.report_dash)
    val validNotes = remember(notes) { notes.filter { it.text.isNotBlank() } }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RecordSpacing.S)) {
        Text(
            text = stringResource(R.string.report_labs_single_tests),
            style = RecordType.CardName,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        ReportHairlineList(items = singles, itemSpacing = RecordSpacing.XS) { _, single ->
            val resultWithUnit = if (!single.unit.isNullOrBlank()) "${single.result} ${single.unit}" else single.result
            val displayValue = if (single.flagMark != null) "$resultWithUnit ${single.flagMark}" else resultWithUnit
            val dateText = single.date.trim()

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = single.test,
                    style = RecordType.KeyValue,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = RecordSpacing.S),
                )
                Text(
                    text = ltrIsolate(displayValue),
                    style = if (single.flagMark != null) RecordType.FieldValue.copy(fontWeight = FontWeight.Bold) else RecordType.FieldValue,
                    color = if (single.flagMark != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = if (dateText.isEmpty()) dash else ltrIsolate(dateText),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (validNotes.isNotEmpty()) ReportLabNotesBlock(notes = validNotes)
    }
}

@Composable
private fun ReportLabNotesBlock(notes: List<LabNote>) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("reportLabNotes"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.report_lab_notes_title),
            style = RecordType.FieldLabel.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        notes.forEach { note ->
            val datePart = note.date.trim()
            val testName = note.test.trim()
            val text = note.text.trim()
            val labName = note.labName?.trim()?.takeIf { it.isNotEmpty() }
            val body = if (labName != null) {
                stringResource(R.string.report_lab_note_with_lab_format, testName, text, labName)
            } else {
                stringResource(R.string.report_lab_note_format, testName, text)
            }
            val line = if (datePart.isNotEmpty()) {
                stringResource(R.string.report_lab_note_dated_format, ltrIsolate(datePart), body)
            } else {
                body
            }
            Text(text = line, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReportMedicationSections(patient: DemoPatient) {
    val meds = remember(patient) { reportMedications(patient) }

    RecordSectionCard(
        title = stringResource(R.string.report_section_current_meds_count_format, stringResource(R.string.report_section_current_meds), meds.current.size),
        family = FieldFamilies.background(),
    ) {
        if (meds.current.isEmpty()) {
            Text(stringResource(R.string.report_empty_current_meds), style = RecordType.Missing, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            ReportMedicationRows(meds = meds.current)
        }
    }

    if (meds.stopped.isNotEmpty()) {
        RecordSectionCard(title = stringResource(R.string.report_section_stopped_meds), family = FieldFamilies.background()) {
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
    val attestation = securitySettings.resolvedAttestation()
    RecordSectionCard(
        title = stringResource(R.string.report_section_attestation),
        family = FieldFamilies.patient(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS),
        ) {
            Text(
                text = attestation.doctorNameEn,
                style = RecordType.CardName,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = attestation.doctorSpecialtyEn,
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PatientNameText(
                name = attestation.doctorNameAr,
                style = RecordType.CardName,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
            PatientNameText(
                name = attestation.doctorSpecialtyAr,
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.report_confidential_footer_format,
                    clinicTitle,
                    ltrIsolate(attestation.clinicPhone),
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
