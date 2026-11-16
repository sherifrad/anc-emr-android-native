package com.sherifradwan.ancnative.ui

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.FhrLevel
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.GestationalAgeResult
import com.sherifradwan.ancnative.model.LastVisitVitals
import com.sherifradwan.ancnative.model.VisitFormValues
import com.sherifradwan.ancnative.model.bpLevel
import com.sherifradwan.ancnative.model.catalog.CustomChoiceItem
import com.sherifradwan.ancnative.model.catalog.CustomChoiceList
import com.sherifradwan.ancnative.model.detectFlaggedFindings
import com.sherifradwan.ancnative.model.fhrLevel
import com.sherifradwan.ancnative.model.fillNormalVisitFindings
import com.sherifradwan.ancnative.model.formatVisitSummary
import com.sherifradwan.ancnative.model.isHighBloodPressure
import com.sherifradwan.ancnative.state.DraftField
import com.sherifradwan.ancnative.state.DraftValues
import com.sherifradwan.ancnative.ui.components.BandChip
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.FormFieldLabel
import com.sherifradwan.ancnative.ui.components.LocalFieldFamily
import com.sherifradwan.ancnative.ui.components.PatientNameText
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusKind
import com.sherifradwan.ancnative.ui.components.StatusTag
import com.sherifradwan.ancnative.ui.components.StatusTone
import com.sherifradwan.ancnative.ui.components.StatusWord
import com.sherifradwan.ancnative.ui.components.currentFieldFamily
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.components.parityText
import java.time.LocalDate
import java.util.Locale

private const val STEP_LABELS_MAX_FONT_SCALE = 1.3f
private const val ROW_STACK_FONT_SCALE = 1.3f

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VisitPatientBanner(
    patient: DemoPatient,
    gaResult: GestationalAgeResult?,
) {
    val patientFamily = FieldFamilies.patient()
    val isHigh = isHighRisk(patient.riskLevel)
    val isModerate = !isHigh && parseRiskTier(patient.riskLevel) == PatientRiskTier.MEDIUM

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RecordShapes.Large,
        color = patientFamily.container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PatientNameText(
                    name = patient.displayName,
                    style = RecordType.CardName,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (isHigh || isModerate) {
                    StatusTag(kind = if (isHigh) StatusKind.HighRisk else StatusKind.ModerateRisk)
                }
            }
            CompositionLocalProvider(LocalFieldFamily provides patientFamily) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (gaResult != null && gaResult.isPostConception) {
                        BandChip(ltrIsolate(gaResult.shortWeeksText), tone = null)
                    }
                    BandChip(
                        text = ltrIsolate(parityText(patient.gravida, patient.parity, patient.abortion)),
                        tone = null,
                    )
                    formatBloodGroup(patient.bloodGroupText)?.let { bg ->
                        BandChip(
                            text = ltrIsolate(bg),
                            tone = if (isRhNegative(patient.bloodGroupText)) StatusTone.High else null,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VisitStepHeader(
    currentStep: Int,
    totalSteps: Int,
    @StringRes stepTitleRes: Int,
    stepTitles: List<Int>,
    editingEnabled: Boolean,
    onStepClick: (Int) -> Unit,
) {
    val showLabels = LocalDensity.current.fontScale <= STEP_LABELS_MAX_FONT_SCALE
    val shortLabels = listOf(
        R.string.visit_step_short_date_bp,
        R.string.visit_step_short_wt_fhr,
        R.string.visit_step_short_findings,
        R.string.visit_step_short_review,
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RecordShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.step_n_of_m, currentStep + 1, totalSteps),
                    style = RecordType.FieldLabel,
                    color = FieldFamilies.patient().label,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(stepTitleRes),
                    style = RecordType.CardName,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (i in 0 until totalSteps) {
                    val isCurrent = i == currentStep
                    val isDoneOrCurrent = i <= currentStep
                    val stepDescription = stringResource(
                        R.string.visit_step_segment_cd,
                        stringResource(stepTitles[i]),
                        i + 1,
                        totalSteps,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .minimumTouchTarget()
                            .clip(RecordShapes.Small)
                            .clickable(
                                enabled = editingEnabled,
                                role = Role.Tab,
                            ) { onStepClick(i) }
                            .semantics {
                                selected = isCurrent
                                contentDescription = stepDescription
                            }
                            .testTag("stepSegment-$i")
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isCurrent) 8.dp else 6.dp)
                                    .clip(RecordShapes.Small)
                                    .then(
                                        if (isDoneOrCurrent) {
                                            Modifier.background(MaterialTheme.colorScheme.primary)
                                        } else {
                                            Modifier
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RecordShapes.Small)
                                        },
                                    ),
                            )
                        }
                        if (showLabels) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(shortLabels[i]),
                                style = RecordType.Tag.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VisitStepDateAndBp(
    patient: DemoPatient,
    draftValues: DraftValues,
    lastVitals: LastVisitVitals,
    bpChoices: List<CustomChoiceItem>,
    dateError: String?,
    bpError: String?,
    editingEnabled: Boolean,
    onDraftChanged: (DraftField, String) -> Unit,
    onOpenCustomDialog: (CustomChoiceList) -> Unit,
) {
    val patientFamily = FieldFamilies.patient()

    RecordSectionCard(
        title = stringResource(R.string.visit_date_section),
        family = patientFamily,
    ) {
        val todayStr = remember { LocalDate.now().toString() }
        val yestStr = remember { LocalDate.now().minusDays(1).toString() }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecordFilterChip(
                selected = draftValues.date == todayStr,
                label = stringResource(R.string.today),
                onClick = { onDraftChanged(DraftField.DATE, todayStr) },
                tag = "visitDateToday",
                enabled = editingEnabled,
                wrap = true,
                minTouchTarget = true,
            )
            RecordFilterChip(
                selected = draftValues.date == yestStr,
                label = stringResource(R.string.yesterday),
                onClick = { onDraftChanged(DraftField.DATE, yestStr) },
                tag = "visitDateYesterday",
                enabled = editingEnabled,
                wrap = true,
                minTouchTarget = true,
            )
        }

        ClinicalDateField(
            value = draftValues.date,
            onValueChange = { onDraftChanged(DraftField.DATE, it) },
            label = stringResource(R.string.visit_date),
            tag = "visitDate",
            enabled = editingEnabled,
            error = dateError != null,
            errorMessage = dateError,
            showTodayButton = false,
            externalLabel = true,
            useRecordStyle = true,
        )

        val gaAtSelectedDate = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draftValues.date)
        if (gaAtSelectedDate != null && gaAtSelectedDate.isPostConception) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RecordShapes.Small,
                color = currentFieldFamily().container,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FormFieldLabel(
                        text = stringResource(R.string.visit_ga_at_visit),
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Text(
                        text = ltrIsolate(gaAtSelectedDate.shortWeeksText),
                        style = RecordType.KeyValue,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    RecordSectionCard(
        title = stringResource(R.string.blood_pressure),
        family = patientFamily,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FormFieldLabel(stringResource(R.string.visit_quick_presets))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (lastVitals.bloodPressure != null) {
                    val lastBp = lastVitals.bloodPressure
                    RecordFilterChip(
                        selected = draftValues.bloodPressure == lastBp,
                        label = stringResource(R.string.same_as_last_bp, lastBp),
                        onClick = { onDraftChanged(DraftField.BLOOD_PRESSURE, lastBp) },
                        tag = "bpPreset-sameLast",
                        enabled = editingEnabled,
                        wrap = true,
                        minTouchTarget = true,
                    )
                }
                RecordFilterChip(
                    selected = draftValues.bloodPressure == "110/70",
                    label = stringResource(R.string.normal_bp_preset),
                    onClick = { onDraftChanged(DraftField.BLOOD_PRESSURE, "110/70") },
                    tag = "bpPreset-normal",
                    enabled = editingEnabled,
                    wrap = true,
                    minTouchTarget = true,
                )
                bpChoices.forEach { item ->
                    RecordFilterChip(
                        selected = draftValues.bloodPressure == item.label,
                        label = item.label,
                        onClick = { onDraftChanged(DraftField.BLOOD_PRESSURE, item.label) },
                        tag = "bpPreset-custom-${item.id}",
                        enabled = editingEnabled,
                        wrap = true,
                        minTouchTarget = true,
                    )
                }
                RecordFilterChip(
                    selected = false,
                    label = stringResource(R.string.add_other_choice),
                    onClick = { onOpenCustomDialog(CustomChoiceList.VISIT_BP) },
                    tag = "bpPreset-addOther",
                    enabled = editingEnabled,
                    wrap = true,
                    minTouchTarget = true,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            BpComponentField(
                label = stringResource(R.string.systolic_label),
                value = draftValues.systolic,
                onValueChange = { onDraftChanged(DraftField.SYSTOLIC, it) },
                enabled = editingEnabled,
                tag = "visitSystolic",
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "/",
                style = RecordType.HeaderName,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            BpComponentField(
                label = stringResource(R.string.diastolic_label),
                value = draftValues.diastolic,
                onValueChange = { onDraftChanged(DraftField.DIASTOLIC, it) },
                enabled = editingEnabled,
                tag = "visitDiastolic",
                modifier = Modifier.weight(1f),
            )
        }

        EditorField(
            value = draftValues.bloodPressure,
            onValueChange = { onDraftChanged(DraftField.BLOOD_PRESSURE, it) },
            label = "${stringResource(R.string.blood_pressure)} (${stringResource(R.string.mmhg)})",
            tag = "visitBloodPressure",
            errorMessage = bpError,
            singleLine = true,
            enabled = editingEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            externalLabel = true,
            shape = RecordShapes.Small,
            textStyle = RecordType.FieldValue,
        )

        val sys = draftValues.systolic.toIntOrNull()
        val dia = draftValues.diastolic.toIntOrNull()
        val isHighBp = isHighBloodPressure(sys, dia) || bpLevel(draftValues.bloodPressure) != BpLevel.NORMAL
        if (isHighBp) {
            val isSevere = bpLevel(draftValues.bloodPressure) == BpLevel.SEVERE ||
                bpLevel(sys, dia) == BpLevel.SEVERE
            val dark = isDarkTheme()
            val (bgColor, borderColor, textColor) = if (isSevere) {
                if (dark) Triple(StatusHighDarkBg, StatusHighDarkBorder, StatusHighDarkText)
                else Triple(StatusHighBg, StatusHighBorder, StatusHighText)
            } else {
                if (dark) Triple(StatusModerateDarkBg, StatusModerateDarkBorder, StatusModerateDarkText)
                else Triple(StatusModerateBg, StatusModerateBorder, StatusModerateText)
            }
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().testTag("highBpWarning"),
                shape = RecordShapes.Small,
                border = BorderStroke(1.dp, borderColor),
                colors = CardDefaults.outlinedCardColors(containerColor = bgColor),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    StatusWord(
                        text = stringResource(if (isSevere) R.string.band_severe_bp else R.string.band_raised_bp),
                        tone = if (isSevere) StatusTone.High else StatusTone.Moderate,
                    )
                    Text(
                        text = stringResource(R.string.high_bp_flagged_warning),
                        style = RecordType.Secondary,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun BpComponentField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.clearAndSetSemantics {}) {
            FormFieldLabel(label)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("-", style = RecordType.FieldValue) },
            singleLine = true,
            enabled = enabled,
            shape = RecordShapes.Small,
            textStyle = RecordType.KeyValue,
            colors = editorFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = label }
                .testTag(tag),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VisitStepWeightAndFhr(
    draftValues: DraftValues,
    lastVitals: LastVisitVitals,
    weightChoices: List<CustomChoiceItem>,
    weightError: String?,
    editingEnabled: Boolean,
    onDraftChanged: (DraftField, String) -> Unit,
    onOpenCustomDialog: (CustomChoiceList) -> Unit,
) {
    val patientFamily = FieldFamilies.patient()

    RecordSectionCard(
        title = stringResource(R.string.weight),
        family = patientFamily,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (lastVitals.weightKg != null) {
                val lastWt = lastVitals.weightKg
                RecordFilterChip(
                    selected = draftValues.weight.startsWith(lastWt),
                    label = stringResource(R.string.same_as_last_wt, lastWt),
                    onClick = { onDraftChanged(DraftField.WEIGHT, lastWt) },
                    tag = "weightPreset-sameLast",
                    enabled = editingEnabled,
                    wrap = true,
                    minTouchTarget = true,
                )
            }
            weightChoices.forEach { item ->
                RecordFilterChip(
                    selected = draftValues.weight == item.label,
                    label = stringResource(R.string.visit_weight_preset_kg, item.label),
                    onClick = { onDraftChanged(DraftField.WEIGHT, item.label) },
                    tag = "weightPreset-custom-${item.id}",
                    enabled = editingEnabled,
                    wrap = true,
                    minTouchTarget = true,
                )
            }
            RecordFilterChip(
                selected = false,
                label = stringResource(R.string.add_other_choice),
                onClick = { onOpenCustomDialog(CustomChoiceList.VISIT_WEIGHT) },
                tag = "weightPreset-addOther",
                enabled = editingEnabled,
                wrap = true,
                minTouchTarget = true,
            )
        }

        val weightLabel = stringResource(R.string.weight)
        ValueStepper(
            value = draftValues.weight,
            unit = "kg",
            enabled = editingEnabled,
            onDecrease = {
                val cur = draftValues.weight.replace("kg", "").trim().toDoubleOrNull() ?: 70.0
                val updated = String.format(Locale.US, "%.1f", maxOf(30.0, cur - 0.1))
                onDraftChanged(DraftField.WEIGHT, updated)
            },
            onIncrease = {
                val cur = draftValues.weight.replace("kg", "").trim().toDoubleOrNull() ?: 70.0
                val updated = String.format(Locale.US, "%.1f", minOf(250.0, cur + 0.1))
                onDraftChanged(DraftField.WEIGHT, updated)
            },
            onValueChange = { onDraftChanged(DraftField.WEIGHT, it) },
            inputTag = "visitWeight",
            useRecordStyle = true,
            decreaseLabel = stringResource(R.string.visit_stepper_decrease, weightLabel),
            increaseLabel = stringResource(R.string.visit_stepper_increase, weightLabel),
            decreaseTag = "visitWeightDecrease",
            increaseTag = "visitWeightIncrease",
        )
        if (weightError != null) {
            Text(
                text = weightError,
                style = RecordType.Tag,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    RecordSectionCard(
        title = stringResource(R.string.visit_fhr_section),
        family = patientFamily,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FormFieldLabel(stringResource(R.string.visit_quick_presets))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (lastVitals.fetalHeartRateBpm != null) {
                    val lastFhr = lastVitals.fetalHeartRateBpm
                    RecordFilterChip(
                        selected = draftValues.fetalHeartRate == lastFhr,
                        label = stringResource(R.string.visit_fhr_same_as_last, lastFhr),
                        onClick = { onDraftChanged(DraftField.FETAL_HEART_RATE, lastFhr) },
                        tag = "fhrPreset-sameLast",
                        enabled = editingEnabled,
                        wrap = true,
                        minTouchTarget = true,
                    )
                }
                listOf("135", "140", "145", "150").forEach { preset ->
                    RecordFilterChip(
                        selected = draftValues.fetalHeartRate == preset,
                        label = stringResource(R.string.visit_fhr_preset_bpm, preset),
                        onClick = { onDraftChanged(DraftField.FETAL_HEART_RATE, preset) },
                        tag = "fhrPreset-$preset",
                        enabled = editingEnabled,
                        wrap = true,
                        minTouchTarget = true,
                    )
                }
            }
        }

        val fhrLabel = stringResource(R.string.fetal_heart_rate)
        ValueStepper(
            value = draftValues.fetalHeartRate,
            unit = "bpm",
            enabled = editingEnabled,
            keyboardType = KeyboardType.Number,
            onDecrease = {
                val cur = draftValues.fetalHeartRate.replace("bpm", "").trim().toIntOrNull() ?: 140
                val updated = maxOf(60, cur - 2).toString()
                onDraftChanged(DraftField.FETAL_HEART_RATE, updated)
            },
            onIncrease = {
                val cur = draftValues.fetalHeartRate.replace("bpm", "").trim().toIntOrNull() ?: 140
                val updated = minOf(220, cur + 2).toString()
                onDraftChanged(DraftField.FETAL_HEART_RATE, updated)
            },
            onValueChange = { onDraftChanged(DraftField.FETAL_HEART_RATE, it) },
            inputTag = "visitFetalHeartRate",
            useRecordStyle = true,
            decreaseLabel = stringResource(R.string.visit_stepper_decrease, fhrLabel),
            increaseLabel = stringResource(R.string.visit_stepper_increase, fhrLabel),
            decreaseTag = "visitFetalHeartRateDecrease",
            increaseTag = "visitFetalHeartRateIncrease",
        )

        val fhrLevelResult = fhrLevel(draftValues.fetalHeartRate.toIntOrNull())
        if (fhrLevelResult != null) {
            when (fhrLevelResult) {
                FhrLevel.NORMAL -> StatusWord(
                    text = stringResource(R.string.visit_fhr_normal_baseline),
                    tone = StatusTone.Neutral,
                )
                FhrLevel.BRADYCARDIA -> StatusWord(
                    text = stringResource(R.string.visit_fhr_bradycardia),
                    tone = StatusTone.High,
                )
                FhrLevel.TACHYCARDIA -> StatusWord(
                    text = stringResource(R.string.visit_fhr_tachycardia),
                    tone = StatusTone.High,
                )
            }
        }
    }
}

@Composable
internal fun VisitStepFindings(
    draftValues: DraftValues,
    oedemaChoices: List<CustomChoiceItem>,
    movementsChoices: List<CustomChoiceItem>,
    urineChoices: List<CustomChoiceItem>,
    editingEnabled: Boolean,
    onDraftChanged: (DraftField, String) -> Unit,
    onOpenCustomDialog: (CustomChoiceList) -> Unit,
) {
    val context = LocalContext.current
    val patientFamily = FieldFamilies.patient()
    val filledNormalTemplate = stringResource(R.string.filled_normal_toast)
    val nothingLeftToFill = stringResource(R.string.nothing_left_to_fill)

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RecordShapes.Large,
        colors = CardDefaults.outlinedCardColors(containerColor = patientFamily.container),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    val initial = VisitFormValues(
                        date = draftValues.date,
                        systolic = draftValues.systolic,
                        diastolic = draftValues.diastolic,
                        weight = draftValues.weight,
                        fundalHeight = "",
                        fetalHeartRate = draftValues.fetalHeartRate,
                        oedema = draftValues.oedema,
                        fetalMovements = draftValues.fetalMovements,
                        urineProtein = draftValues.urineProtein,
                        notes = draftValues.notes,
                    )
                    val result = fillNormalVisitFindings(initial)
                    if (result.values.oedema != draftValues.oedema) {
                        onDraftChanged(DraftField.OEDEMA, result.values.oedema)
                    }
                    if (result.values.fetalMovements != draftValues.fetalMovements) {
                        onDraftChanged(DraftField.FETAL_MOVEMENTS, result.values.fetalMovements)
                    }
                    if (result.values.urineProtein != draftValues.urineProtein) {
                        onDraftChanged(DraftField.URINE_PROTEIN, result.values.urineProtein)
                    }
                    val msg = if (result.filledCount > 0) {
                        String.format(filledNormalTemplate, result.filledCount)
                    } else {
                        nothingLeftToFill
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                enabled = editingEnabled,
                shape = RecordShapes.Small,
                border = BorderStroke(
                    1.dp,
                    if (editingEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .minimumTouchTarget()
                    .testTag("fillNormalValues"),
            ) {
                Text(
                    text = stringResource(R.string.fill_normal_values),
                    style = RecordType.Action,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = stringResource(R.string.fill_normal_values_sub),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    RecordSectionCard(
        title = stringResource(R.string.visit_bedside_findings_section),
        family = patientFamily,
    ) {
        BedsideFindingGroup(
            label = stringResource(R.string.oedema),
            selectedValue = draftValues.oedema,
            standardChoices = listOf("None", "Mild", "Marked"),
            customChoices = oedemaChoices,
            tagPrefix = "oedemaChoice",
            flagTone = if (draftValues.oedema.equals("Marked", ignoreCase = true)) StatusTone.High else null,
            editingEnabled = editingEnabled,
            onSelect = { onDraftChanged(DraftField.OEDEMA, it) },
            onAddOther = { onOpenCustomDialog(CustomChoiceList.VISIT_OEDEMA) },
        )
        BedsideFindingGroup(
            label = stringResource(R.string.fetal_movements),
            selectedValue = draftValues.fetalMovements,
            standardChoices = listOf("Good", "Reduced", "Absent"),
            customChoices = movementsChoices,
            tagPrefix = "movementsChoice",
            flagTone = if (
                draftValues.fetalMovements.equals("Reduced", ignoreCase = true) ||
                draftValues.fetalMovements.equals("Absent", ignoreCase = true)
            ) {
                StatusTone.High
            } else {
                null
            },
            editingEnabled = editingEnabled,
            onSelect = { onDraftChanged(DraftField.FETAL_MOVEMENTS, it) },
            onAddOther = { onOpenCustomDialog(CustomChoiceList.VISIT_MOVEMENTS) },
        )
        val urineTrimmed = draftValues.urineProtein.trim()
        BedsideFindingGroup(
            label = stringResource(R.string.urine_protein),
            selectedValue = draftValues.urineProtein,
            standardChoices = listOf("Negative", "Trace", "+", "++"),
            customChoices = urineChoices,
            tagPrefix = "urineChoice",
            flagTone = when {
                urineTrimmed.equals("Trace", ignoreCase = true) -> StatusTone.Moderate
                urineTrimmed.contains("+") -> StatusTone.High
                else -> null
            },
            editingEnabled = editingEnabled,
            onSelect = { onDraftChanged(DraftField.URINE_PROTEIN, it) },
            onAddOther = { onOpenCustomDialog(CustomChoiceList.VISIT_URINE) },
        )
    }

    val findingsLabel = stringResource(R.string.findings)
    RecordSectionCard(
        title = findingsLabel,
        family = patientFamily,
    ) {
        EditorField(
            value = draftValues.findings,
            onValueChange = { onDraftChanged(DraftField.FINDINGS, it) },
            label = "",
            tag = "visitFindings",
            enabled = editingEnabled,
            shape = RecordShapes.Small,
            textStyle = RecordType.FieldValue,
            modifier = Modifier.semantics { contentDescription = findingsLabel },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BedsideFindingGroup(
    label: String,
    selectedValue: String,
    standardChoices: List<String>,
    customChoices: List<CustomChoiceItem>,
    tagPrefix: String,
    flagTone: StatusTone?,
    editingEnabled: Boolean,
    onSelect: (String) -> Unit,
    onAddOther: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormFieldLabel(
                text = label,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            if (flagTone != null) {
                StatusWord(
                    text = stringResource(R.string.visit_finding_flagged),
                    tone = flagTone,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            standardChoices.forEach { choice ->
                RecordFilterChip(
                    selected = selectedValue.equals(choice, ignoreCase = true),
                    label = choice,
                    onClick = { onSelect(choice) },
                    tag = "$tagPrefix-$choice",
                    enabled = editingEnabled,
                    wrap = true,
                    minTouchTarget = true,
                )
            }
            customChoices.forEach { item ->
                RecordFilterChip(
                    selected = selectedValue.equals(item.label, ignoreCase = true),
                    label = item.label,
                    onClick = { onSelect(item.label) },
                    tag = "$tagPrefix-${item.label}",
                    enabled = editingEnabled,
                    wrap = true,
                    minTouchTarget = true,
                )
            }
            RecordFilterChip(
                selected = false,
                label = stringResource(R.string.add_other_choice),
                onClick = onAddOther,
                tag = "$tagPrefix-addOther",
                enabled = editingEnabled,
                wrap = true,
                minTouchTarget = true,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VisitStepReview(
    patient: DemoPatient,
    draftValues: DraftValues,
    calculatedGa: GestationalAgeResult?,
    editingEnabled: Boolean,
    onStepClick: (Int) -> Unit,
    onDraftChanged: (DraftField, String) -> Unit,
) {
    val patientFamily = FieldFamilies.patient()
    val gaShort = calculatedGa?.takeIf { it.isPostConception }?.shortWeeksText
    val summaryText = formatVisitSummary(
        dateText = draftValues.date,
        gaText = gaShort,
        systolic = draftValues.systolic,
        diastolic = draftValues.diastolic,
        weightKg = draftValues.weight,
        fundalHeightCm = null,
        oedema = draftValues.oedema,
        fetalMovements = draftValues.fetalMovements,
        urineProtein = draftValues.urineProtein,
        fetalHeartRate = draftValues.fetalHeartRate,
    )
    val flags = detectFlaggedFindings(
        systolic = draftValues.systolic,
        diastolic = draftValues.diastolic,
        oedema = draftValues.oedema,
        fetalMovements = draftValues.fetalMovements,
        urineProtein = draftValues.urineProtein,
        fetalHeartRate = draftValues.fetalHeartRate,
    )

    RecordSectionCard(
        title = stringResource(R.string.visit_summary_section),
        family = patientFamily,
        modifier = Modifier.testTag("visitSummaryCard"),
    ) {
        if (flags.isNotEmpty()) {
            val bpIsSevere = bpLevel(draftValues.bloodPressure) == BpLevel.SEVERE ||
                bpLevel(draftValues.systolic.toIntOrNull(), draftValues.diastolic.toIntOrNull()) == BpLevel.SEVERE
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                flags.forEach { flag ->
                    val tone = when {
                        flag == "High BP" && !bpIsSevere -> StatusTone.Moderate
                        flag == "Oedema mild" || flag == "Urine trace" -> StatusTone.Moderate
                        else -> StatusTone.High
                    }
                    BandChip(text = flag, tone = tone)
                }
            }
        } else {
            StatusWord(
                text = stringResource(R.string.nothing_flagged),
                tone = StatusTone.Neutral,
            )
        }
        Text(
            text = summaryText,
            style = RecordType.FieldValue,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    RecordSectionCard(
        title = stringResource(R.string.visit_recorded_values_section),
        family = patientFamily,
    ) {
        val editAction = stringResource(R.string.action_edit)
        Column(modifier = Modifier.fillMaxWidth()) {
            SummaryRow(
                label = stringResource(R.string.visit_date),
                value = draftValues.date,
                tag = "summaryRow-date",
                onClick = { onStepClick(0) },
                useRecordStyle = true,
                trailingActionText = editAction,
                showDivider = false,
            )
            SummaryRow(
                label = stringResource(R.string.blood_pressure),
                value = draftValues.bloodPressure,
                tag = "summaryRow-bp",
                onClick = { onStepClick(0) },
                useRecordStyle = true,
                trailingActionText = editAction,
                showDivider = true,
            )
            SummaryRow(
                label = stringResource(R.string.weight),
                value = if (draftValues.weight.isNotBlank()) {
                    stringResource(R.string.visit_weight_preset_kg, draftValues.weight)
                } else {
                    ""
                },
                tag = "summaryRow-weight",
                onClick = { onStepClick(1) },
                useRecordStyle = true,
                trailingActionText = editAction,
                showDivider = true,
            )
            SummaryRow(
                label = stringResource(R.string.fetal_heart_rate),
                value = if (draftValues.fetalHeartRate.isNotBlank()) {
                    stringResource(R.string.unit_bpm, draftValues.fetalHeartRate)
                } else {
                    ""
                },
                tag = "summaryRow-fhr",
                onClick = { onStepClick(1) },
                useRecordStyle = true,
                trailingActionText = editAction,
                showDivider = true,
            )
            SummaryRow(
                label = stringResource(R.string.oedema),
                value = draftValues.oedema,
                tag = "summaryRow-oedema",
                onClick = { onStepClick(2) },
                useRecordStyle = true,
                trailingActionText = editAction,
                showDivider = true,
            )
            SummaryRow(
                label = stringResource(R.string.fetal_movements),
                value = draftValues.fetalMovements,
                tag = "summaryRow-movements",
                onClick = { onStepClick(2) },
                useRecordStyle = true,
                trailingActionText = editAction,
                showDivider = true,
            )
            SummaryRow(
                label = stringResource(R.string.urine_protein),
                value = draftValues.urineProtein,
                tag = "summaryRow-urine",
                onClick = { onStepClick(2) },
                useRecordStyle = true,
                trailingActionText = editAction,
                showDivider = true,
            )
        }
    }

    RecordSectionCard(
        title = stringResource(R.string.linked_ultrasound),
        family = patientFamily,
        modifier = Modifier.testTag("visitLinkedUltrasoundCard"),
    ) {
        if (draftValues.linkedUltrasoundId.isNotBlank()) {
            val linkedScan = patient.ultrasoundScans.firstOrNull { it.id == draftValues.linkedUltrasoundId }
            val scanGaText = linkedScan?.gestationalAgeWeeks?.let {
                " (${it}+${linkedScan.gestationalAgeDays ?: 0} wks)"
            }.orEmpty()
            val scanMeta: @Composable (Modifier) -> Unit = { metaModifier ->
                Column(modifier = metaModifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.ultrasound) + ": " +
                            (linkedScan?.date ?: draftValues.linkedUltrasoundId) + scanGaText,
                        style = RecordType.CardName,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (linkedScan?.impression?.isNotBlank() == true) {
                        Text(
                            text = linkedScan.impression,
                            style = RecordType.Secondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            val unlinkButton: @Composable () -> Unit = {
                OutlinedButton(
                    onClick = { onDraftChanged(DraftField.LINKED_ULTRASOUND, "") },
                    enabled = editingEnabled,
                    shape = RecordShapes.Small,
                    border = BorderStroke(
                        1.dp,
                        if (editingEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.minimumTouchTarget().testTag("unlinkUltrasound"),
                ) {
                    Text(
                        text = stringResource(R.string.unlink_ultrasound),
                        style = RecordType.Action,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (LocalDensity.current.fontScale > ROW_STACK_FONT_SCALE) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    scanMeta(Modifier.fillMaxWidth())
                    unlinkButton()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    scanMeta(Modifier.weight(1f))
                    unlinkButton()
                }
            }
        } else {
            Text(
                text = stringResource(R.string.link_to_ultrasound),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (patient.ultrasoundScans.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    patient.ultrasoundScans.forEach { scan ->
                        val scanLabel = "${scan.date}" +
                            (scan.gestationalAgeWeeks?.let { " (${it}+${scan.gestationalAgeDays ?: 0} wks)" } ?: "")
                        RecordFilterChip(
                            selected = false,
                            label = scanLabel,
                            onClick = { onDraftChanged(DraftField.LINKED_ULTRASOUND, scan.id) },
                            tag = "linkScan-${scan.id}",
                            enabled = editingEnabled,
                            wrap = true,
                            minTouchTarget = true,
                        )
                    }
                }
            }
        }
    }

    val notesLabel = stringResource(R.string.notes)
    RecordSectionCard(
        title = notesLabel,
        family = patientFamily,
    ) {
        EditorField(
            value = draftValues.notes,
            onValueChange = { onDraftChanged(DraftField.NOTES, it) },
            label = "",
            tag = "visitNotes",
            enabled = editingEnabled,
            shape = RecordShapes.Small,
            textStyle = RecordType.FieldValue,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            modifier = Modifier.semantics { contentDescription = notesLabel },
        )
    }
}
