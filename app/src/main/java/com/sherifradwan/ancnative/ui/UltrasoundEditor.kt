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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    val context = LocalContext.current

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
        subtitle = "${patient.displayName} · Ultrasound Examination & Fetal Biometry",
        tag = "ultrasoundEditor",
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state)

        // CARD 0: REPORT SO FAR / LIVE SUMMARY
        val liveReport = ultrasoundImpressionFromChoices(
            draft.values.fetalNumber,
            draft.values.presentation,
            draft.values.placentalSite,
            draft.values.amnioticFluid,
        )
        val reportDisplayText = liveReport.ifBlank { stringResource(R.string.ultrasound_nothing_chosen_yet) }

        SectionCard(
            title = stringResource(R.string.ultrasound_report_so_far),
            tag = "ultrasoundLiveSummary",
        ) {
            Text(
                text = reportDisplayText,
                style = MaterialTheme.typography.bodyMedium,
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
                    modifier = Modifier.minimumTouchTarget().testTag("fillNormalScan"),
                ) {
                    Text(stringResource(R.string.fill_normal_scan))
                }
                OutlinedButton(
                    onClick = {
                        onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.FETAL_NUMBER, ""))
                        onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.PRESENTATION, ""))
                        onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.PLACENTAL_SITE, ""))
                        onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.AMNIOTIC_FLUID, ""))
                        openFindingKey = nextBlankFinding("", "", "", "")
                    },
                    enabled = enabled,
                    modifier = Modifier.minimumTouchTarget().testTag("clearFindings"),
                ) {
                    Text(stringResource(R.string.clear_findings))
                }
            }
        }

        // CARD 1: SCAN TIMING & GESTATIONAL AGE
        SectionCard(
            title = "1. Scan Timing & Gestational Age",
        ) {
            // Date selection
            ClinicalDateField(
                value = draft.values.date,
                onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.DATE, it)) },
                label = stringResource(R.string.scan_date),
                tag = "scanDate",
                enabled = enabled,
                error = draft.errors.contains(ClinicalValidationError.REQUIRED_DATE) ||
                    draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                showTodayButton = true,
            )

            // Automatic Gestational Age calculation suggestion
            val calculatedGa = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.values.date)
            if (calculatedGa != null && calculatedGa.isPostConception) {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("scanCalculatedGaCard"),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.calculated_ga_at_visit, calculatedGa.displayText),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        val weeks = draft.values.gaWeeks.trim().toIntOrNull()
                        val days = draft.values.gaDays.trim().toIntOrNull()
                        if (weeks != null && weeks >= 0 && days != null && days in 0..6) {
                            val advice = redatingAdvice(calculatedGa.totalDays, weeks * 7L + days)
                            if (advice != null) {
                                Text(
                                    text = stringResource(
                                        R.string.redating_hint,
                                        abs(advice.differenceDays),
                                        advice.thresholdDays,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.testTag("redatingHint"),
                                )
                            }
                        }
                        val isDifferent = draft.values.gaWeeks != calculatedGa.weeks.toString() ||
                            draft.values.gaDays != calculatedGa.days.toString()
                        if (isDifferent) {
                            TextButton(
                                onClick = {
                                    onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_WEEKS, calculatedGa.weeks.toString()))
                                    onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_DAYS, calculatedGa.days.toString()))
                                },
                                enabled = enabled,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("applyScanGa"),
                            ) {
                                Text(stringResource(R.string.apply_calculated_ga, calculatedGa.displayText))
                            }
                        }
                    }
                }
            }

            // Side-by-side Gestational Age inputs (Weeks & Days)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    EditorField(
                        value = draft.values.gaWeeks,
                        onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_WEEKS, it)) },
                        label = stringResource(R.string.ga_weeks),
                        tag = "gaWeeks",
                        enabled = enabled,
                        error = draft.errors.contains(ClinicalValidationError.INVALID_GA_WEEKS),
                        singleLine = true,
                        keyboardType = KeyboardType.Number,
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    EditorField(
                        value = draft.values.gaDays,
                        onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_DAYS, it)) },
                        label = stringResource(R.string.ga_days),
                        tag = "gaDays",
                        enabled = enabled,
                        error = draft.errors.contains(ClinicalValidationError.INVALID_GA_DAYS),
                        singleLine = true,
                        keyboardType = KeyboardType.Number,
                    )
                }
            }
        }

        // CARD 2: FETUS, PLACENTA & LIQUOR (GUIDED FINDINGS)
        SectionCard(
            title = stringResource(R.string.ultrasound_section_fetus_placenta_liquor),
            tag = "ultrasoundFindingsCard",
        ) {
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

            findings.forEachIndexed { index, finding ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                        )
                    }
                } else {
                    SummaryRow(
                        label = finding.label,
                        value = finding.value,
                        tag = "findingRow-${finding.key}",
                        onClick = { openFindingKey = finding.key },
                    )
                }
            }
        }

        // CARD 3: FETAL BIOMETRY (MEASUREMENTS)
        val hasBiometry = draft.values.fetalHeartRate.isNotBlank() ||
            draft.values.biometryBpd.isNotBlank() ||
            draft.values.biometryHc.isNotBlank() ||
            draft.values.biometryAc.isNotBlank() ||
            draft.values.biometryFl.isNotBlank() ||
            draft.values.estimatedFetalWeight.isNotBlank()
        var biometryExpanded by remember { mutableStateOf(hasBiometry) }

        SectionCard(
            title = "",
            tag = "biometryCard",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "3. Fetal Biometry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "BPD, HC, AC, FL, FHR & EFW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { biometryExpanded = !biometryExpanded },
                    modifier = Modifier.minimumTouchTarget().testTag("toggleBiometry"),
                ) {
                    Text(if (biometryExpanded) "▲ Collapse" else "▼ Expand Biometry")
                }
            }
            if (biometryExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.fetalHeartRate,
                            onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.FETAL_HEART_RATE, it)) },
                            label = stringResource(R.string.fetal_heart_rate),
                            tag = "fetalHeartRate",
                            enabled = enabled,
                            singleLine = true,
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.estimatedFetalWeight,
                            onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.ESTIMATED_FETAL_WEIGHT, it)) },
                            label = stringResource(R.string.estimated_fetal_weight),
                            tag = "efw",
                            enabled = enabled,
                            singleLine = true,
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.biometryBpd,
                            onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.BIOMETRY_BPD, it)) },
                            label = stringResource(R.string.biometry_bpd),
                            tag = "biometryBpd",
                            enabled = enabled,
                            singleLine = true,
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.biometryHc,
                            onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.BIOMETRY_HC, it)) },
                            label = stringResource(R.string.biometry_hc),
                            tag = "biometryHc",
                            enabled = enabled,
                            singleLine = true,
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.biometryAc,
                            onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.BIOMETRY_AC, it)) },
                            label = stringResource(R.string.biometry_ac),
                            tag = "biometryAc",
                            enabled = enabled,
                            singleLine = true,
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.biometryFl,
                            onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.BIOMETRY_FL, it)) },
                            label = stringResource(R.string.biometry_fl),
                            tag = "biometryFl",
                            enabled = enabled,
                            singleLine = true,
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                }
            }
        }

        // CARD 4: CLINICAL IMPRESSION
        SectionCard(
            title = "4. Clinical Impression",
        ) {
            EditorField(
                value = draft.values.impression,
                onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMPRESSION, it)) },
                label = stringResource(R.string.scan_impression),
                tag = "impression",
                enabled = enabled,
                singleLine = false,
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
                modifier = Modifier
                    .minimumTouchTarget()
                    .testTag("impressionFromChoices"),
            ) {
                Text(stringResource(R.string.impression_from_choices))
            }

            // Security Vault Indicator
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Ultrasound records and scans encrypted locally (AES-256 GCM) · Hardware Vault Security",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // CARD 5: MORE (PHOTOS & LINKED VISIT)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            tonalElevation = 1.dp,
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                SummaryRow(
                    label = stringResource(R.string.ultrasound_more),
                    value = if (moreExpanded) stringResource(R.string.more_close) else stringResource(R.string.more_open),
                    tag = "ultrasoundMoreToggle",
                    onClick = { moreExpanded = !moreExpanded },
                )
            }
        }

        if (moreExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth().testTag("ultrasoundMoreSection"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Attachments / Scan Photos Card
                SectionCard(
                    title = stringResource(R.string.scan_attachments),
                    tag = "scanAttachmentsCard",
                ) {
                    if (draft.values.imageUris.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_photos_attached),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            draft.values.imageUris.forEachIndexed { index, _ ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("attachmentItem-$index"),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = "Photo ${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                        IconButton(
                                            onClick = {
                                                val updated = draft.values.imageUris.toMutableList().apply { removeAt(index) }
                                                onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMAGE_URIS, updated.joinToString(",")))
                                            },
                                            enabled = enabled,
                                            modifier = Modifier.minimumTouchTarget().testTag("removePhoto-$index"),
                                        ) {
                                            Text("✕", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { showCamera = true },
                            enabled = enabled,
                            modifier = Modifier.minimumTouchTarget().testTag("captureScanPhotoCamera"),
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo", maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            enabled = enabled,
                            modifier = Modifier.minimumTouchTarget().testTag("addScanPhoto"),
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Image", maxLines = 1, softWrap = false)
                        }
                    }
                }

                // Link with Visit Card
                SectionCard(
                    title = stringResource(R.string.link_to_visit),
                    tag = "linkedVisitCard",
                ) {
                    if (draft.values.linkedVisitId.isNotBlank()) {
                        val linkedVisit = patient.visits.firstOrNull { it.id == draft.values.linkedVisitId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.link_to_visit) + ": " + (linkedVisit?.date ?: draft.values.linkedVisitId),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (linkedVisit?.bloodPressure != null || linkedVisit?.weight != null) {
                                    Text(
                                        text = "BP: ${linkedVisit.bloodPressure ?: "-"} · Wt: ${formatWeightKg(linkedVisit.weight) ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, "")) },
                                enabled = enabled,
                                modifier = Modifier.minimumTouchTarget().testTag("unlinkVisit"),
                            ) {
                                Text(stringResource(R.string.unlink_visit))
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.no_linked_visit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (patient.visits.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                patient.visits.forEach { visit ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, visit.id)) },
                                        enabled = enabled,
                                        label = { Text(visit.date) },
                                        modifier = Modifier.minimumTouchTarget().testTag("linkVisit-${visit.id}"),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        EditorActions(
            enabled = enabled,
            saveTag = "saveUltrasound",
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitUltrasound) },
        )
    }
}
