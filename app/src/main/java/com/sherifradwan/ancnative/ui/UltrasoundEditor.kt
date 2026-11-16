package com.sherifradwan.ancnative.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.NORMAL_AMNIOTIC_FLUID
import com.sherifradwan.ancnative.model.NORMAL_FETAL_NUMBER
import com.sherifradwan.ancnative.model.NORMAL_PRESENTATION
import com.sherifradwan.ancnative.model.mergeImpression
import com.sherifradwan.ancnative.model.redatingAdvice
import com.sherifradwan.ancnative.model.ultrasoundImpressionFromChoices
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.UltrasoundDraftField
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.LocalFieldFamily
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordSpacing
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.SectionTextAction
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.patienthub.components.SheetCancelSaveRow
import kotlin.math.abs

private val GUIDED_FINDING_KEYS = listOf("fetalNumber", "presentation", "placenta", "liquor")

private fun nextBlankFinding(
    fetalNumber: String,
    presentation: String,
    placenta: String,
    liquor: String,
    afterKey: String? = null,
): String? {
    val values = mapOf(
        "fetalNumber" to fetalNumber,
        "presentation" to presentation,
        "placenta" to placenta,
        "liquor" to liquor,
    )
    val startIndex = if (afterKey != null) {
        val idx = GUIDED_FINDING_KEYS.indexOf(afterKey)
        if (idx >= 0) idx + 1 else 0
    } else {
        0
    }
    for (i in startIndex until GUIDED_FINDING_KEYS.size) {
        val key = GUIDED_FINDING_KEYS[i]
        if (values[key].isNullOrBlank()) {
            return key
        }
    }
    return null
}

private data class GuidedFinding(
    val key: String,
    val label: String,
    val value: String,
    val choices: List<String>,
    val tagPrefix: String,
    val field: UltrasoundDraftField,
)

@Composable
fun UltrasoundEditor(
    state: AncState,
    route: AncRoute.UltrasoundEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.ultrasoundDraft?.takeIf { it.patientId == route.patientId && it.scanId == route.scanId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR
    val obstetricFamily = FieldFamilies.obstetric()
    val narrowStacked = LocalDensity.current.fontScale > 1.3f

    var showCamera by remember { mutableStateOf(false) }

    var openFindingKey by rememberSaveable {
        mutableStateOf(
            nextBlankFinding(
                draft.values.fetalNumber,
                draft.values.presentation,
                draft.values.placentalSite,
                draft.values.amnioticFluid,
            ),
        )
    }

    val hasMoreInitial = draft.values.linkedVisitId.isNotBlank() || draft.values.imageUris.isNotEmpty()
    var moreExpanded by rememberSaveable { mutableStateOf(hasMoreInitial) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onEvent(AncEvent.ImportScanPhoto(route.patientId, uri))
        }
    }

    if (showCamera) {
        InAppCameraDialog(
            onCaptured = { uri ->
                onEvent(AncEvent.ImportScanPhoto(route.patientId, uri))
                showCamera = false
            },
            onDismiss = { showCamera = false },
        )
    }

    EditorPage(
        title = stringResource(R.string.ultrasound_editor),
        subtitle = patient.displayName,
        tag = "ultrasoundEditor",
        useRecordStyle = true,
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)

        val liveReport = ultrasoundImpressionFromChoices(
            draft.values.fetalNumber,
            draft.values.presentation,
            draft.values.placentalSite,
            draft.values.amnioticFluid,
        )
        val reportDisplayText = liveReport.ifBlank { stringResource(R.string.ultrasound_nothing_chosen_yet) }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth().testTag("ultrasoundLiveSummary"),
            shape = RecordShapes.Large,
            colors = CardDefaults.outlinedCardColors(containerColor = obstetricFamily.container),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.ultrasound_report_so_far),
                    style = RecordType.CardName,
                    color = obstetricFamily.label,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = reportDisplayText,
                    style = if (liveReport.isBlank()) RecordType.Missing else RecordType.FieldValue,
                    color = if (liveReport.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val newFn = draft.values.fetalNumber.ifBlank { NORMAL_FETAL_NUMBER }
                            val newPr = draft.values.presentation.ifBlank { NORMAL_PRESENTATION }
                            val newLq = draft.values.amnioticFluid.ifBlank { NORMAL_AMNIOTIC_FLUID }
                            if (draft.values.fetalNumber.isBlank()) {
                                onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.FETAL_NUMBER, NORMAL_FETAL_NUMBER))
                            }
                            if (draft.values.presentation.isBlank()) {
                                onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.PRESENTATION, NORMAL_PRESENTATION))
                            }
                            if (draft.values.amnioticFluid.isBlank()) {
                                onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.AMNIOTIC_FLUID, NORMAL_AMNIOTIC_FLUID))
                            }
                            openFindingKey = nextBlankFinding(newFn, newPr, draft.values.placentalSite, newLq)
                        },
                        enabled = enabled,
                        shape = RecordShapes.Small,
                        border = BorderStroke(
                            1.dp,
                            if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.minimumTouchTarget().testTag("fillNormalScan"),
                    ) {
                        Text(
                            text = stringResource(R.string.fill_normal_scan),
                            style = RecordType.Action,
                        )
                    }
                    TextButton(
                        onClick = {
                            onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.FETAL_NUMBER, ""))
                            onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.PRESENTATION, ""))
                            onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.PLACENTAL_SITE, ""))
                            onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.AMNIOTIC_FLUID, ""))
                            openFindingKey = nextBlankFinding("", "", "", "")
                        },
                        enabled = enabled,
                        shape = RecordShapes.Small,
                        modifier = Modifier.minimumTouchTarget().testTag("clearFindings"),
                    ) {
                        Text(
                            text = stringResource(R.string.clear_findings),
                            style = RecordType.Action,
                            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        RecordSectionCard(
            title = stringResource(R.string.ultrasound_timing_section),
            family = obstetricFamily,
        ) {
            ClinicalDateField(
                value = draft.values.date,
                onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.DATE, it)) },
                label = stringResource(R.string.scan_date),
                tag = "scanDate",
                enabled = enabled,
                error = draft.errors.contains(ClinicalValidationError.REQUIRED_DATE) ||
                    draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                showTodayButton = true,
                externalLabel = true,
                useRecordStyle = true,
            )

            val calculatedGa = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.values.date)
            if (calculatedGa != null && calculatedGa.isPostConception) {
                val gaShort = ltrIsolate(calculatedGa.shortWeeksText)
                val isDifferent = draft.values.gaWeeks != calculatedGa.weeks.toString() ||
                    draft.values.gaDays != calculatedGa.days.toString()
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("scanCalculatedGaCard"),
                    color = obstetricFamily.container,
                    shape = RecordShapes.Small,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.calculated_ga_at_visit, gaShort),
                            style = RecordType.KeyValue,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (isDifferent) {
                            TextButton(
                                onClick = {
                                    onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_WEEKS, calculatedGa.weeks.toString()))
                                    onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_DAYS, calculatedGa.days.toString()))
                                },
                                enabled = enabled,
                                shape = RecordShapes.Small,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .minimumTouchTarget()
                                    .testTag("applyScanGa"),
                            ) {
                                Text(
                                    text = stringResource(R.string.apply_calculated_ga, gaShort),
                                    style = RecordType.Action,
                                )
                            }
                        }
                    }
                }

                val weeks = draft.values.gaWeeks.trim().toIntOrNull()
                val days = draft.values.gaDays.trim().toIntOrNull()
                if (weeks != null && weeks >= 0 && days != null && days in 0..6) {
                    val advice = redatingAdvice(calculatedGa.totalDays, weeks * 7L + days)
                    if (advice != null) {
                        val isDark = isDarkTheme()
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isDark) StatusModerateDarkBg else StatusModerateBg,
                            border = BorderStroke(
                                1.dp,
                                if (isDark) StatusModerateDarkBorder else StatusModerateBorder,
                            ),
                            shape = RecordShapes.Small,
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.redating_hint,
                                    abs(advice.differenceDays),
                                    advice.thresholdDays,
                                ),
                                style = RecordType.Secondary,
                                color = if (isDark) StatusModerateDarkText else StatusModerateText,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("redatingHint"),
                            )
                        }
                    }
                }
            }

            AdaptiveFieldPair(
                narrowStacked = narrowStacked,
                first = {
                    EditorField(
                        value = draft.values.gaWeeks,
                        onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_WEEKS, it)) },
                        label = stringResource(R.string.ga_weeks),
                        tag = "gaWeeks",
                        enabled = enabled,
                        error = draft.errors.contains(ClinicalValidationError.INVALID_GA_WEEKS),
                        singleLine = true,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                        shape = RecordShapes.Small,
                        textStyle = RecordType.KeyValue,
                        externalLabel = true,
                    )
                },
                second = {
                    EditorField(
                        value = draft.values.gaDays,
                        onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_DAYS, it)) },
                        label = stringResource(R.string.ga_days),
                        tag = "gaDays",
                        enabled = enabled,
                        error = draft.errors.contains(ClinicalValidationError.INVALID_GA_DAYS),
                        singleLine = true,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                        shape = RecordShapes.Small,
                        textStyle = RecordType.KeyValue,
                        externalLabel = true,
                    )
                },
            )
        }

        val findings = listOf(
            GuidedFinding(
                key = "fetalNumber",
                label = stringResource(R.string.fetal_number),
                value = draft.values.fetalNumber,
                choices = ClinicalVocabulary.fetalNumbers.toList(),
                tagPrefix = "fetalNumberChoice",
                field = UltrasoundDraftField.FETAL_NUMBER,
            ),
            GuidedFinding(
                key = "presentation",
                label = stringResource(R.string.fetal_presentation),
                value = draft.values.presentation,
                choices = ClinicalVocabulary.fetalPresentations.toList(),
                tagPrefix = "presentationChoice",
                field = UltrasoundDraftField.PRESENTATION,
            ),
            GuidedFinding(
                key = "placenta",
                label = stringResource(R.string.placental_site),
                value = draft.values.placentalSite,
                choices = ClinicalVocabulary.placentalSites.toList(),
                tagPrefix = "placentaChoice",
                field = UltrasoundDraftField.PLACENTAL_SITE,
            ),
            GuidedFinding(
                key = "liquor",
                label = stringResource(R.string.amniotic_fluid),
                value = draft.values.amnioticFluid,
                choices = ClinicalVocabulary.amnioticFluids.toList(),
                tagPrefix = "liquorChoice",
                field = UltrasoundDraftField.AMNIOTIC_FLUID,
            ),
        )
        val filledCount = findings.count { it.value.isNotBlank() }

        RecordSectionCard(
            title = stringResource(R.string.ultrasound_section_fetus_placenta_liquor),
            family = obstetricFamily,
            modifier = Modifier.testTag("ultrasoundFindingsCard"),
            action = {
                Text(
                    text = stringResource(R.string.ultrasound_findings_progress, filledCount, findings.size),
                    style = RecordType.FieldLabel,
                    color = obstetricFamily.label,
                    modifier = Modifier.padding(end = 12.dp),
                )
            },
        ) {
            findings.forEachIndexed { index, finding ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (openFindingKey == finding.key) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("findingOpen-${finding.key}"),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ChoiceGroup(
                            label = finding.label,
                            selected = finding.value,
                            choices = finding.choices,
                            enabled = enabled,
                            tagPrefix = finding.tagPrefix,
                            onSelected = { choice ->
                                onEvent(AncEvent.UltrasoundDraftChanged(finding.field, choice))
                                val nextKey = when (finding.key) {
                                    "fetalNumber" -> nextBlankFinding(choice, draft.values.presentation, draft.values.placentalSite, draft.values.amnioticFluid, afterKey = finding.key)
                                    "presentation" -> nextBlankFinding(draft.values.fetalNumber, choice, draft.values.placentalSite, draft.values.amnioticFluid, afterKey = finding.key)
                                    "placenta" -> nextBlankFinding(draft.values.fetalNumber, draft.values.presentation, choice, draft.values.amnioticFluid, afterKey = finding.key)
                                    "liquor" -> nextBlankFinding(draft.values.fetalNumber, draft.values.presentation, draft.values.placentalSite, choice, afterKey = finding.key)
                                    else -> null
                                }
                                openFindingKey = nextKey
                            },
                            labelStyle = RecordType.FieldLabel,
                            labelColor = obstetricFamily.label,
                            useRecordChip = true,
                        )
                    }
                } else {
                    SummaryRow(
                        label = finding.label,
                        value = finding.value,
                        tag = "findingRow-${finding.key}",
                        onClick = { openFindingKey = finding.key },
                        useRecordStyle = true,
                        // Omit the trailing action at > 1.3x scale so long values like "Oligohydramnios" leave label room.
                        trailingActionText = if (narrowStacked) {
                            null
                        } else {
                            stringResource(
                                if (finding.value.isBlank()) R.string.ultrasound_finding_choose else R.string.action_edit,
                            )
                        },
                        showDivider = false,
                    )
                }
            }
        }

        val hasBiometry = draft.values.fetalHeartRate.isNotBlank() ||
            draft.values.biometryBpd.isNotBlank() ||
            draft.values.biometryHc.isNotBlank() ||
            draft.values.biometryAc.isNotBlank() ||
            draft.values.biometryFl.isNotBlank() ||
            draft.values.estimatedFetalWeight.isNotBlank()
        var biometryExpanded by remember { mutableStateOf(hasBiometry) }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth().testTag("biometryCard"),
            shape = RecordShapes.Large,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            CompositionLocalProvider(LocalFieldFamily provides obstetricFamily) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(RecordSpacing.M),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                            Text(
                                text = stringResource(R.string.ultrasound_biometry_section),
                                style = RecordType.SectionTitle,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                text = stringResource(R.string.ultrasound_biometry_summary),
                                style = RecordType.Secondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val toggleLabel = stringResource(
                            if (biometryExpanded) R.string.action_collapse else R.string.action_expand,
                        )
                        SectionTextAction(
                            text = toggleLabel,
                            contentDescription = toggleLabel,
                            onClick = { biometryExpanded = !biometryExpanded },
                            modifier = Modifier.testTag("toggleBiometry"),
                        )
                    }
                    if (biometryExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(RecordSpacing.M),
                        ) {
                            AdaptiveFieldPair(
                                narrowStacked = narrowStacked,
                                first = {
                                    BiometryNumberField(
                                        value = draft.values.fetalHeartRate,
                                        field = UltrasoundDraftField.FETAL_HEART_RATE,
                                        labelRes = R.string.fetal_heart_rate,
                                        tag = "fetalHeartRate",
                                        enabled = enabled,
                                        keyboardType = KeyboardType.Number,
                                        onEvent = onEvent,
                                    )
                                },
                                second = {
                                    BiometryNumberField(
                                        value = draft.values.estimatedFetalWeight,
                                        field = UltrasoundDraftField.ESTIMATED_FETAL_WEIGHT,
                                        labelRes = R.string.estimated_fetal_weight,
                                        tag = "efw",
                                        enabled = enabled,
                                        keyboardType = KeyboardType.Number,
                                        onEvent = onEvent,
                                    )
                                },
                            )
                            AdaptiveFieldPair(
                                narrowStacked = narrowStacked,
                                first = {
                                    BiometryNumberField(
                                        value = draft.values.biometryBpd,
                                        field = UltrasoundDraftField.BIOMETRY_BPD,
                                        labelRes = R.string.biometry_bpd,
                                        tag = "biometryBpd",
                                        enabled = enabled,
                                        keyboardType = KeyboardType.Decimal,
                                        onEvent = onEvent,
                                    )
                                },
                                second = {
                                    BiometryNumberField(
                                        value = draft.values.biometryHc,
                                        field = UltrasoundDraftField.BIOMETRY_HC,
                                        labelRes = R.string.biometry_hc,
                                        tag = "biometryHc",
                                        enabled = enabled,
                                        keyboardType = KeyboardType.Decimal,
                                        onEvent = onEvent,
                                    )
                                },
                            )
                            AdaptiveFieldPair(
                                narrowStacked = narrowStacked,
                                first = {
                                    BiometryNumberField(
                                        value = draft.values.biometryAc,
                                        field = UltrasoundDraftField.BIOMETRY_AC,
                                        labelRes = R.string.biometry_ac,
                                        tag = "biometryAc",
                                        enabled = enabled,
                                        keyboardType = KeyboardType.Decimal,
                                        onEvent = onEvent,
                                    )
                                },
                                second = {
                                    BiometryNumberField(
                                        value = draft.values.biometryFl,
                                        field = UltrasoundDraftField.BIOMETRY_FL,
                                        labelRes = R.string.biometry_fl,
                                        tag = "biometryFl",
                                        enabled = enabled,
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done,
                                        onEvent = onEvent,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        val impressionLabel = stringResource(R.string.scan_impression)
        RecordSectionCard(
            title = impressionLabel,
            family = obstetricFamily,
        ) {
            EditorField(
                value = draft.values.impression,
                onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMPRESSION, it)) },
                label = "",
                tag = "impression",
                modifier = Modifier.semantics { contentDescription = impressionLabel },
                enabled = enabled,
                singleLine = false,
                shape = RecordShapes.Small,
                textStyle = RecordType.FieldValue,
                externalLabel = false,
            )

            val generatedImpression = ultrasoundImpressionFromChoices(
                draft.values.fetalNumber,
                draft.values.presentation,
                draft.values.placentalSite,
                draft.values.amnioticFluid,
            )

            OutlinedButton(
                onClick = {
                    val merged = mergeImpression(draft.values.impression, generatedImpression)
                    onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMPRESSION, merged))
                },
                enabled = enabled && generatedImpression.isNotBlank(),
                shape = RecordShapes.Small,
                border = BorderStroke(
                    1.dp,
                    if (enabled && generatedImpression.isNotBlank()) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
                modifier = Modifier
                    .minimumTouchTarget()
                    .testTag("impressionFromChoices"),
            ) {
                Text(
                    text = stringResource(R.string.impression_from_choices),
                    style = RecordType.Action,
                )
            }
        }

        OutlinedCard(
            onClick = { moreExpanded = !moreExpanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("ultrasoundMoreToggle"),
            shape = RecordShapes.Large,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ultrasound_more),
                    style = RecordType.CardName,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(
                    text = stringResource(if (moreExpanded) R.string.more_close else R.string.more_open),
                    style = RecordType.Action,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (moreExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth().testTag("ultrasoundMoreSection"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RecordSectionCard(
                    title = stringResource(R.string.scan_attachments),
                    family = obstetricFamily,
                    modifier = Modifier.testTag("scanAttachmentsCard"),
                ) {
                    if (draft.values.imageUris.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_photos_attached),
                            style = RecordType.Secondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        draft.values.imageUris.forEachIndexed { index, _ ->
                            Surface(
                                color = obstetricFamily.container,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RecordShapes.Small,
                                modifier = Modifier.fillMaxWidth().testTag("attachmentItem-$index"),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .padding(start = 12.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.scan_photo_n, index + 1),
                                        style = RecordType.KeyValue,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    )
                                    IconButton(
                                        onClick = {
                                            val updated = draft.values.imageUris.toMutableList().apply { removeAt(index) }
                                            onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMAGE_URIS, updated.joinToString(",")))
                                        },
                                        enabled = enabled,
                                        modifier = Modifier.minimumTouchTarget().testTag("removePhoto-$index"),
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove_photo),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { showCamera = true },
                            enabled = enabled,
                            shape = RecordShapes.Small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .minimumTouchTarget()
                                .testTag("captureScanPhotoCamera"),
                        ) {
                            Text(
                                text = stringResource(R.string.attachment_take_photo),
                                style = RecordType.Action,
                                textAlign = TextAlign.Center,
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            enabled = enabled,
                            shape = RecordShapes.Small,
                            border = BorderStroke(
                                1.dp,
                                if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .minimumTouchTarget()
                                .testTag("addScanPhoto"),
                        ) {
                            Text(
                                text = stringResource(R.string.attachment_gallery),
                                style = RecordType.Action,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                RecordSectionCard(
                    title = stringResource(R.string.link_to_visit),
                    family = obstetricFamily,
                    modifier = Modifier.testTag("linkedVisitCard"),
                ) {
                    if (draft.values.linkedVisitId.isNotBlank()) {
                        val linkedVisit = patient.visits.firstOrNull { it.id == draft.values.linkedVisitId }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.ultrasound_linked_visit_title,
                                    ltrIsolate(linkedVisit?.date ?: draft.values.linkedVisitId),
                                ),
                                style = RecordType.KeyValue,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (linkedVisit?.bloodPressure != null || linkedVisit?.weight != null) {
                                Text(
                                    text = stringResource(
                                        R.string.ultrasound_linked_visit_vitals,
                                        ltrIsolate(linkedVisit.bloodPressure ?: "-"),
                                        ltrIsolate(formatWeightKg(linkedVisit.weight) ?: "-"),
                                    ),
                                    style = RecordType.Secondary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, "")) },
                            enabled = enabled,
                            shape = RecordShapes.Small,
                            border = BorderStroke(
                                1.dp,
                                if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier.minimumTouchTarget().testTag("unlinkVisit"),
                        ) {
                            Text(
                                text = stringResource(R.string.unlink_visit),
                                style = RecordType.Action,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.no_linked_visit),
                            style = RecordType.Secondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (patient.visits.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                patient.visits.forEach { visit ->
                                    RecordFilterChip(
                                        selected = false,
                                        label = visit.date,
                                        onClick = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, visit.id)) },
                                        tag = "linkVisit-${visit.id}",
                                        enabled = enabled,
                                        wrap = true,
                                        minTouchTarget = true,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SheetCancelSaveRow(
            enabled = enabled,
            saving = state.savePhase == SavePhase.PENDING,
            saveEnabled = enabled,
            savingLabel = stringResource(if (state.savePhase == SavePhase.PENDING) R.string.saving_label else R.string.save),
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitUltrasound) },
            cancelTag = "cancelUltrasound",
            saveTag = "saveUltrasound",
            spinnerTag = "ultrasoundSaveSpinner",
        )
    }
}

@Composable
private fun AdaptiveFieldPair(
    narrowStacked: Boolean,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    if (narrowStacked) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RecordSpacing.M),
        ) {
            first()
            second()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RecordSpacing.S),
        ) {
            Box(modifier = Modifier.weight(1f)) { first() }
            Box(modifier = Modifier.weight(1f)) { second() }
        }
    }
}

@Composable
private fun BiometryNumberField(
    value: String,
    field: UltrasoundDraftField,
    labelRes: Int,
    tag: String,
    enabled: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction = ImeAction.Next,
    onEvent: (AncEvent) -> Unit,
) {
    EditorField(
        value = value,
        onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(field, it)) },
        label = stringResource(labelRes),
        tag = tag,
        enabled = enabled,
        singleLine = true,
        keyboardType = keyboardType,
        imeAction = imeAction,
        shape = RecordShapes.Small,
        textStyle = RecordType.KeyValue,
        externalLabel = true,
    )
}
