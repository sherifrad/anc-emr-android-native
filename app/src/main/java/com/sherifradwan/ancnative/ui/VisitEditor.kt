package com.sherifradwan.ancnative.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.FhrLevel
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.VisitFormValues
import com.sherifradwan.ancnative.model.bpLevel
import com.sherifradwan.ancnative.model.fhrLevel
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.model.codex.CustomChoiceList
import com.sherifradwan.ancnative.model.codex.CustomChoiceValidationResult
import com.sherifradwan.ancnative.model.detectFlaggedFindings
import com.sherifradwan.ancnative.model.estimateFundalHeightForGa
import com.sherifradwan.ancnative.model.fillNormalVisitFindings
import com.sherifradwan.ancnative.model.formatVisitSummary
import com.sherifradwan.ancnative.model.isHighBloodPressure
import com.sherifradwan.ancnative.model.resolveLastVisitVitals
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DraftError
import com.sherifradwan.ancnative.state.DraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.stringRes
import java.time.LocalDate

@Composable
fun VisitEditor(
    state: AncState,
    route: AncRoute.VisitEditor,
    onEvent: (AncEvent) -> Unit,
    onSave: () -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.draft?.takeIf { it.patientId == route.patientId } ?: return
    val editingEnabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR
    val context = LocalContext.current
    val codexRepo = remember { ClinicalCodexRepository.getInstance(context) }

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 4

    var showCustomDialog by remember { mutableStateOf<CustomChoiceList?>(null) }
    var customInput by remember { mutableStateOf("") }
    var customInputError by remember { mutableStateOf<String?>(null) }

    val calculatedGa = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.values.date)
    val gaText = if (calculatedGa != null && calculatedGa.isPostConception) calculatedGa.displayText else ""

    val subtitle = if (state.savePhase != SavePhase.IDLE) {
        "${patient.displayName} · Routine Antenatal Visit & Vitals"
    } else if (gaText.isNotBlank()) {
        "${patient.displayName} · GA $gaText"
    } else {
        "${patient.displayName} · Routine Antenatal Visit & Vitals"
    }

    val lastVitals = remember(patient.visits, draft.visitId) {
        resolveLastVisitVitals(patient.visits, draft.visitId)
    }

    val stepTitles = listOf(
        R.string.step_date_bp,
        R.string.step_wt_fh,
        R.string.step_findings,
        R.string.step_review_save,
    )

    val dateError = when {
        DraftError.DATE_REQUIRED in draft.errors -> stringResource(R.string.required_date)
        DraftError.INVALID_DATE in draft.errors -> stringResource(R.string.invalid_date)
        DraftError.DATE_IN_FUTURE in draft.errors -> stringResource(R.string.visit_date_in_future)
        else -> null
    }
    val bpError = when {
        DraftError.BLOOD_PRESSURE_INVALID in draft.errors -> stringResource(R.string.blood_pressure_invalid)
        else -> null
    }
    val weightError = when {
        DraftError.WEIGHT_INVALID in draft.errors -> stringResource(R.string.weight_invalid)
        else -> null
    }

    val filledNormalTemplate = stringResource(R.string.filled_normal_toast)
    val nothingLeftToFill = stringResource(R.string.nothing_left_to_fill)
    val customBlankError = stringResource(R.string.custom_drug_blank_error)
    val customTooLongError = stringResource(R.string.custom_drug_too_long_error)
    val customDuplicateError = stringResource(R.string.custom_drug_duplicate_error)
    val customInvalidCharsError = stringResource(R.string.custom_drug_invalid_chars_error)
    val customNotFoundError = stringResource(R.string.custom_drug_not_found_error)

    val bpChoices by codexRepo.customChoices(CustomChoiceList.VISIT_BP).collectAsState(emptyList())
    val weightChoices by codexRepo.customChoices(CustomChoiceList.VISIT_WEIGHT).collectAsState(emptyList())
    val oedemaChoices by codexRepo.customChoices(CustomChoiceList.VISIT_OEDEMA).collectAsState(emptyList())
    val movementsChoices by codexRepo.customChoices(CustomChoiceList.VISIT_MOVEMENTS).collectAsState(emptyList())
    val urineChoices by codexRepo.customChoices(CustomChoiceList.VISIT_URINE).collectAsState(emptyList())

    val pinnedBottomBar: @Composable () -> Unit = {
        if (state.savePhase != SavePhase.IDLE) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { onEvent(AncEvent.CancelEditor) },
                    enabled = editingEnabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("cancelVisit"),
                ) {
                    Text(stringResource(R.string.cancel), maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = onSave,
                    enabled = editingEnabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("saveVisit"),
                ) {
                    Text(stringResource(R.string.save), maxLines = 1, softWrap = false)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (currentStep) {
                    0 -> {
                        OutlinedButton(
                            onClick = { onEvent(AncEvent.CancelEditor) },
                            enabled = editingEnabled,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("cancelVisit"),
                        ) {
                            Text(stringResource(R.string.cancel), maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = { currentStep = 1 },
                            enabled = editingEnabled,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("nextStep"),
                        ) {
                            Text(stringResource(R.string.next_step), maxLines = 1, softWrap = false)
                        }
                    }
                    in 1..2 -> {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            enabled = editingEnabled,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("backStep"),
                        ) {
                            Text(stringResource(R.string.back_step), maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = { currentStep++ },
                            enabled = editingEnabled,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("nextStep"),
                        ) {
                            Text(stringResource(R.string.next_step), maxLines = 1, softWrap = false)
                        }
                    }
                    else -> {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            enabled = editingEnabled,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("backStep"),
                        ) {
                            Text(stringResource(R.string.back_step), maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = onSave,
                            enabled = editingEnabled,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("saveVisit"),
                        ) {
                            Text(stringResource(R.string.save_visit), maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }

    EditorPage(
        title = if (draft.visitId == null) stringResource(R.string.add_visit) else stringResource(R.string.edit_visit),
        subtitle = subtitle,
        tag = "visitEditor",
        pinnedBottomBar,
    ) {
        ClinicalPatientCard(
            patient = patient,
            variant = PatientCardVariant.EDITOR_SUMMARY,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (DraftError.STALE_REVISION in draft.errors) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("staleError"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    text = stringResource(R.string.stale_visit),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        if (state.savePhase != SavePhase.IDLE) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("saveStatus"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.savePhase == SavePhase.ERROR || state.savePhase == SavePhase.UNRESOLVED) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    },
                ),
            ) {
                Text(
                    text = stringResource(state.saveStatusResource()),
                    color = if (state.savePhase == SavePhase.ERROR || state.savePhase == SavePhase.UNRESOLVED) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        if (state.savePhase != SavePhase.IDLE) {
            // Accessible fallback/review layout while saving or on save error
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClinicalDateField(
                        value = draft.values.date,
                        onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.DATE, it)) },
                        label = stringResource(R.string.visit_date),
                        tag = "visitDate",
                        enabled = editingEnabled,
                        error = dateError != null,
                        errorMessage = dateError,
                        showTodayButton = true,
                    )
                    EditorField(
                        value = draft.values.bloodPressure,
                        onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, it)) },
                        label = "${stringResource(R.string.blood_pressure)} (${stringResource(R.string.mmhg)})",
                        tag = "visitBloodPressure",
                        errorMessage = bpError,
                        singleLine = true,
                        enabled = editingEnabled,
                    )
                    if (bpLevel(draft.values.bloodPressure) != BpLevel.NORMAL) {
                        BpAlertLabel(draft.values.bloodPressure)
                    }
                    EditorField(
                        value = draft.values.weight,
                        onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.WEIGHT, it)) },
                        label = "${stringResource(R.string.weight)} (kg)",
                        tag = "visitWeight",
                        errorMessage = weightError,
                        singleLine = true,
                        enabled = editingEnabled,
                    )
                    EditorField(
                        value = draft.values.findings,
                        onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.FINDINGS, it)) },
                        label = stringResource(R.string.findings),
                        tag = "visitFindings",
                        enabled = editingEnabled,
                    )
                    EditorField(
                        value = draft.values.notes,
                        onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.NOTES, it)) },
                        label = stringResource(R.string.notes),
                        tag = "visitNotes",
                        enabled = editingEnabled,
                    )
                }
            }
        } else {
            // 4-STEP WIZARD
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.step_n_of_m, currentStep + 1, totalSteps),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(stepTitles[currentStep]),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            SegmentedProgressBar(
                totalSteps = totalSteps,
                currentStep = currentStep,
                onStepClick = { if (editingEnabled) currentStep = it },
            )

            when (currentStep) {
                0 -> {
                    // STEP 0: DATE & BLOOD PRESSURE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.visit_date),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            val todayStr = remember { LocalDate.now().toString() }
                            val yestStr = remember { LocalDate.now().minusDays(1).toString() }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(
                                    selected = draft.values.date == todayStr,
                                    onClick = { onEvent(AncEvent.DraftChanged(DraftField.DATE, todayStr)) },
                                    label = { Text(stringResource(R.string.today)) },
                                    enabled = editingEnabled,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                                FilterChip(
                                    selected = draft.values.date == yestStr,
                                    onClick = { onEvent(AncEvent.DraftChanged(DraftField.DATE, yestStr)) },
                                    label = { Text(stringResource(R.string.yesterday)) },
                                    enabled = editingEnabled,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                            ClinicalDateField(
                                value = draft.values.date,
                                onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.DATE, it)) },
                                label = stringResource(R.string.other_date),
                                tag = "visitDate",
                                enabled = editingEnabled,
                                error = dateError != null,
                                errorMessage = dateError,
                                showTodayButton = false,
                            )

                            val gaAtSelectedDate = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.values.date)
                            if (gaAtSelectedDate != null && gaAtSelectedDate.isPostConception) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ClinicalTeal.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, ClinicalTeal.copy(alpha = 0.3f)),
                                    modifier = Modifier.padding(top = 2.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = "Gestational Age at visit: ${gaAtSelectedDate.displayText}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ClinicalTeal,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.blood_pressure),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (lastVitals.bloodPressure != null) {
                                    val lastBp = lastVitals.bloodPressure!!
                                    FilterChip(
                                        selected = draft.values.bloodPressure == lastBp,
                                        onClick = { onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, lastBp)) },
                                        label = { Text(stringResource(R.string.same_as_last_bp, lastBp)) },
                                        enabled = editingEnabled,
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("bpPreset-sameLast"),
                                    )
                                }
                                FilterChip(
                                    selected = draft.values.bloodPressure == "110/70",
                                    onClick = { onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, "110/70")) },
                                    label = { Text(stringResource(R.string.normal_bp_preset)) },
                                    enabled = editingEnabled,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                                bpChoices.forEach { item ->
                                    FilterChip(
                                        selected = draft.values.bloodPressure == item.label,
                                        onClick = { onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, item.label)) },
                                        label = { Text(item.label) },
                                        enabled = editingEnabled,
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    )
                                }
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        showCustomDialog = CustomChoiceList.VISIT_BP
                                        customInput = ""
                                        customInputError = null
                                    },
                                    label = { Text(stringResource(R.string.add_other_choice)) },
                                    enabled = editingEnabled,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = draft.values.systolic,
                                    onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.SYSTOLIC, it)) },
                                    label = { Text(stringResource(R.string.systolic_label)) },
                                    placeholder = { Text("—") },
                                    singleLine = true,
                                    enabled = editingEnabled,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("visitSystolic"),
                                )
                                Text(
                                    text = "/",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                OutlinedTextField(
                                    value = draft.values.diastolic,
                                    onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.DIASTOLIC, it)) },
                                    label = { Text(stringResource(R.string.diastolic_label)) },
                                    placeholder = { Text("—") },
                                    singleLine = true,
                                    enabled = editingEnabled,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("visitDiastolic"),
                                )
                            }

                            EditorField(
                                value = draft.values.bloodPressure,
                                onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, it)) },
                                label = "${stringResource(R.string.blood_pressure)} (${stringResource(R.string.mmhg)})",
                                tag = "visitBloodPressure",
                                errorMessage = bpError,
                                singleLine = true,
                                enabled = editingEnabled,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            )

                            val isHighBp = isHighBloodPressure(draft.values.systolic.toIntOrNull(), draft.values.diastolic.toIntOrNull()) ||
                                bpLevel(draft.values.bloodPressure) != BpLevel.NORMAL
                            if (isHighBp) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("highBpWarning"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                ) {
                                    Text(
                                        text = stringResource(R.string.high_bp_flagged_warning),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // STEP 1: WEIGHT & FUNDAL HEIGHT
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.weight),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (lastVitals.weightKg != null) {
                                    val lastWt = lastVitals.weightKg!!
                                    FilterChip(
                                        selected = draft.values.weight.startsWith(lastWt),
                                        onClick = { onEvent(AncEvent.DraftChanged(DraftField.WEIGHT, lastWt)) },
                                        label = { Text(stringResource(R.string.same_as_last_wt, lastWt)) },
                                        enabled = editingEnabled,
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("weightPreset-sameLast"),
                                    )
                                }
                                weightChoices.forEach { item ->
                                    FilterChip(
                                        selected = draft.values.weight == item.label,
                                        onClick = { onEvent(AncEvent.DraftChanged(DraftField.WEIGHT, item.label)) },
                                        label = { Text("${item.label} kg") },
                                        enabled = editingEnabled,
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    )
                                }
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        showCustomDialog = CustomChoiceList.VISIT_WEIGHT
                                        customInput = ""
                                        customInputError = null
                                    },
                                    label = { Text(stringResource(R.string.add_other_choice)) },
                                    enabled = editingEnabled,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                            ValueStepper(
                                value = draft.values.weight,
                                unit = "kg",
                                enabled = editingEnabled,
                                onDecrease = {
                                    val cur = draft.values.weight.replace("kg", "").trim().toDoubleOrNull() ?: 70.0
                                    val updated = String.format(java.util.Locale.US, "%.1f", maxOf(30.0, cur - 0.1))
                                    onEvent(AncEvent.DraftChanged(DraftField.WEIGHT, updated))
                                },
                                onIncrease = {
                                    val cur = draft.values.weight.replace("kg", "").trim().toDoubleOrNull() ?: 70.0
                                    val updated = String.format(java.util.Locale.US, "%.1f", minOf(250.0, cur + 0.1))
                                    onEvent(AncEvent.DraftChanged(DraftField.WEIGHT, updated))
                                },
                                onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.WEIGHT, it)) },
                                inputTag = "visitWeight",
                            )
                            if (weightError != null) {
                                Text(
                                    text = weightError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Fetal heart rate (FHR)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (lastVitals.fetalHeartRateBpm != null) {
                                    val lastFhr = lastVitals.fetalHeartRateBpm!!
                                    FilterChip(
                                        selected = draft.values.fetalHeartRate == lastFhr,
                                        onClick = { onEvent(AncEvent.DraftChanged(DraftField.FETAL_HEART_RATE, lastFhr)) },
                                        label = { Text("Same as last ($lastFhr bpm)") },
                                        enabled = editingEnabled,
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("fhrPreset-sameLast"),
                                    )
                                }
                                listOf("135", "140", "145", "150").forEach { preset ->
                                    FilterChip(
                                        selected = draft.values.fetalHeartRate == preset,
                                        onClick = { onEvent(AncEvent.DraftChanged(DraftField.FETAL_HEART_RATE, preset)) },
                                        label = { Text("$preset bpm") },
                                        enabled = editingEnabled,
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    )
                                }
                            }
                            ValueStepper(
                                value = draft.values.fetalHeartRate,
                                unit = "bpm",
                                enabled = editingEnabled,
                                keyboardType = KeyboardType.Number,
                                onDecrease = {
                                    val cur = draft.values.fetalHeartRate.replace("bpm", "").trim().toIntOrNull() ?: 140
                                    val updated = maxOf(60, cur - 2).toString()
                                    onEvent(AncEvent.DraftChanged(DraftField.FETAL_HEART_RATE, updated))
                                },
                                onIncrease = {
                                    val cur = draft.values.fetalHeartRate.replace("bpm", "").trim().toIntOrNull() ?: 140
                                    val updated = minOf(220, cur + 2).toString()
                                    onEvent(AncEvent.DraftChanged(DraftField.FETAL_HEART_RATE, updated))
                                },
                                onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.FETAL_HEART_RATE, it)) },
                                inputTag = "visitFetalHeartRate",
                            )

                            val fhrNum = draft.values.fetalHeartRate.toIntOrNull()
                            val fhrLevelResult = fhrLevel(fhrNum)
                            if (fhrLevelResult != null) {
                                val (badgeText, badgeColor, badgeBg) = when (fhrLevelResult) {
                                    FhrLevel.NORMAL -> Triple("Normal baseline (110–160 bpm) ✓", ClinicalTeal, ClinicalTeal.copy(alpha = 0.12f))
                                    FhrLevel.BRADYCARDIA -> Triple("Bradycardia (<110 bpm) ⚠️", MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer)
                                    FhrLevel.TACHYCARDIA -> Triple("Tachycardia (>160 bpm) ⚠️", MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeBg,
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // STEP 2: FINDINGS
                    Button(
                        onClick = {
                            val initial = VisitFormValues(
                                date = draft.values.date,
                                systolic = draft.values.systolic,
                                diastolic = draft.values.diastolic,
                                weight = draft.values.weight,
                                fundalHeight = "",
                                fetalHeartRate = draft.values.fetalHeartRate,
                                oedema = draft.values.oedema,
                                fetalMovements = draft.values.fetalMovements,
                                urineProtein = draft.values.urineProtein,
                                notes = draft.values.notes,
                            )
                            val result = fillNormalVisitFindings(initial)
                            if (result.values.oedema != draft.values.oedema) {
                                onEvent(AncEvent.DraftChanged(DraftField.OEDEMA, result.values.oedema))
                            }
                            if (result.values.fetalMovements != draft.values.fetalMovements) {
                                onEvent(AncEvent.DraftChanged(DraftField.FETAL_MOVEMENTS, result.values.fetalMovements))
                            }
                            if (result.values.urineProtein != draft.values.urineProtein) {
                                onEvent(AncEvent.DraftChanged(DraftField.URINE_PROTEIN, result.values.urineProtein))
                            }
                            val msg = if (result.filledCount > 0) {
                                String.format(filledNormalTemplate, result.filledCount)
                            } else {
                                nothingLeftToFill
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("fillNormalValues"),
                    ) {
                        Text(
                            text = stringResource(R.string.fill_normal_values),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = stringResource(R.string.fill_normal_values_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    // Oedema Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.oedema),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            listOf("None", "Mild", "Marked").forEach { choice ->
                                val isFlagged = choice.equals("Marked", ignoreCase = true)
                                val isSelected = draft.values.oedema.equals(choice, ignoreCase = true)
                                BigChoiceButton(
                                    label = if (isFlagged) "$choice (Flag)" else choice,
                                    selected = isSelected,
                                    tag = "oedemaChoice-$choice",
                                    onClick = { onEvent(AncEvent.DraftChanged(DraftField.OEDEMA, choice)) },
                                    enabled = editingEnabled,
                                    modifier = if (isFlagged && isSelected) {
                                        Modifier.padding(vertical = 2.dp)
                                    } else Modifier.padding(vertical = 2.dp),
                                )
                            }
                            oedemaChoices.forEach { item ->
                                BigChoiceButton(
                                    label = item.label,
                                    selected = draft.values.oedema.equals(item.label, ignoreCase = true),
                                    tag = "oedemaChoice-${item.label}",
                                    onClick = { onEvent(AncEvent.DraftChanged(DraftField.OEDEMA, item.label)) },
                                    enabled = editingEnabled,
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = {
                                    showCustomDialog = CustomChoiceList.VISIT_OEDEMA
                                    customInput = ""
                                    customInputError = null
                                },
                                label = { Text(stringResource(R.string.add_other_choice)) },
                                enabled = editingEnabled,
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }

                    // Fetal Movements Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.fetal_movements),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            listOf("Good", "Reduced", "Absent").forEach { choice ->
                                val isFlagged = !choice.equals("Good", ignoreCase = true)
                                val isSelected = draft.values.fetalMovements.equals(choice, ignoreCase = true)
                                BigChoiceButton(
                                    label = if (isFlagged) "$choice (Flag)" else choice,
                                    selected = isSelected,
                                    tag = "movementsChoice-$choice",
                                    onClick = { onEvent(AncEvent.DraftChanged(DraftField.FETAL_MOVEMENTS, choice)) },
                                    enabled = editingEnabled,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                            movementsChoices.forEach { item ->
                                BigChoiceButton(
                                    label = item.label,
                                    selected = draft.values.fetalMovements.equals(item.label, ignoreCase = true),
                                    tag = "movementsChoice-${item.label}",
                                    onClick = { onEvent(AncEvent.DraftChanged(DraftField.FETAL_MOVEMENTS, item.label)) },
                                    enabled = editingEnabled,
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = {
                                    showCustomDialog = CustomChoiceList.VISIT_MOVEMENTS
                                    customInput = ""
                                    customInputError = null
                                },
                                label = { Text(stringResource(R.string.add_other_choice)) },
                                enabled = editingEnabled,
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }

                    // Urine Protein Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.urine_protein),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            listOf("Negative", "Trace", "+", "++").forEach { choice ->
                                val isFlagged = !choice.equals("Negative", ignoreCase = true)
                                val isSelected = draft.values.urineProtein.equals(choice, ignoreCase = true)
                                BigChoiceButton(
                                    label = if (isFlagged) "$choice (Flag)" else choice,
                                    selected = isSelected,
                                    tag = "urineChoice-$choice",
                                    onClick = { onEvent(AncEvent.DraftChanged(DraftField.URINE_PROTEIN, choice)) },
                                    enabled = editingEnabled,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                            urineChoices.forEach { item ->
                                BigChoiceButton(
                                    label = item.label,
                                    selected = draft.values.urineProtein.equals(item.label, ignoreCase = true),
                                    tag = "urineChoice-${item.label}",
                                    onClick = { onEvent(AncEvent.DraftChanged(DraftField.URINE_PROTEIN, item.label)) },
                                    enabled = editingEnabled,
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = {
                                    showCustomDialog = CustomChoiceList.VISIT_URINE
                                    customInput = ""
                                    customInputError = null
                                },
                                label = { Text(stringResource(R.string.add_other_choice)) },
                                enabled = editingEnabled,
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.findings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            EditorField(
                                value = draft.values.findings,
                                onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.FINDINGS, it)) },
                                label = stringResource(R.string.findings),
                                tag = "visitFindings",
                                enabled = editingEnabled,
                            )
                        }
                    }
                }

                3 -> {
                    // STEP 3: REVIEW AND SAVE
                    val summaryText = formatVisitSummary(
                        dateText = draft.values.date,
                        gaText = gaText,
                        systolic = draft.values.systolic,
                        diastolic = draft.values.diastolic,
                        weightKg = draft.values.weight,
                        fundalHeightCm = null,
                        oedema = draft.values.oedema,
                        fetalMovements = draft.values.fetalMovements,
                        urineProtein = draft.values.urineProtein,
                        fetalHeartRate = draft.values.fetalHeartRate,
                    )
                    val flags = detectFlaggedFindings(
                        systolic = draft.values.systolic,
                        diastolic = draft.values.diastolic,
                        oedema = draft.values.oedema,
                        fetalMovements = draft.values.fetalMovements,
                        urineProtein = draft.values.urineProtein,
                        fetalHeartRate = draft.values.fetalHeartRate,
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("visitSummaryCard"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (flags.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    flags.forEach { flag ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ) {
                                            Text(
                                                text = flag,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ) {
                                    Text(
                                        text = stringResource(R.string.nothing_flagged),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }

                    // 7 Review Rows
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            SummaryRow(
                                label = stringResource(R.string.visit_date),
                                value = draft.values.date,
                                tag = "summaryRow-date",
                                onClick = { currentStep = 0 },
                            )
                            SummaryRow(
                                label = stringResource(R.string.blood_pressure),
                                value = draft.values.bloodPressure,
                                tag = "summaryRow-bp",
                                onClick = { currentStep = 0 },
                            )
                            SummaryRow(
                                label = stringResource(R.string.weight),
                                value = if (draft.values.weight.isNotBlank()) "${draft.values.weight} kg" else "",
                                tag = "summaryRow-weight",
                                onClick = { currentStep = 1 },
                            )
                            SummaryRow(
                                label = "Fetal Heart Rate",
                                value = if (draft.values.fetalHeartRate.isNotBlank()) "${draft.values.fetalHeartRate} bpm" else "",
                                tag = "summaryRow-fhr",
                                onClick = { currentStep = 1 },
                            )
                            SummaryRow(
                                label = stringResource(R.string.oedema),
                                value = draft.values.oedema,
                                tag = "summaryRow-oedema",
                                onClick = { currentStep = 2 },
                            )
                            SummaryRow(
                                label = stringResource(R.string.fetal_movements),
                                value = draft.values.fetalMovements,
                                tag = "summaryRow-movements",
                                onClick = { currentStep = 2 },
                            )
                            SummaryRow(
                                label = stringResource(R.string.urine_protein),
                                value = draft.values.urineProtein,
                                tag = "summaryRow-urine",
                                onClick = { currentStep = 2 },
                            )
                        }
                    }

                    // Linked Ultrasound Section
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("visitLinkedUltrasoundCard"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.linked_ultrasound),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (draft.values.linkedUltrasoundId.isNotBlank()) {
                                val linkedScan = patient.ultrasoundScans.firstOrNull { it.id == draft.values.linkedUltrasoundId }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val scanGaText = if (linkedScan?.gestationalAgeWeeks != null) {
                                            " (${linkedScan.gestationalAgeWeeks}w ${linkedScan.gestationalAgeDays ?: 0}d)"
                                        } else ""
                                        Text(
                                            text = stringResource(R.string.ultrasound) + ": " + (linkedScan?.date ?: draft.values.linkedUltrasoundId) + scanGaText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        if (linkedScan?.impression?.isNotBlank() == true) {
                                            Text(
                                                text = linkedScan.impression,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onEvent(AncEvent.DraftChanged(DraftField.LINKED_ULTRASOUND, "")) },
                                        enabled = editingEnabled,
                                        modifier = Modifier.minimumTouchTarget().testTag("unlinkUltrasound"),
                                    ) {
                                        Text(stringResource(R.string.unlink_ultrasound))
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.link_to_ultrasound),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (patient.ultrasoundScans.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        patient.ultrasoundScans.forEach { scan ->
                                            val scanLabel = "${scan.date}" + (scan.gestationalAgeWeeks?.let { " (${it}w)" } ?: "")
                                            FilterChip(
                                                selected = false,
                                                onClick = { onEvent(AncEvent.DraftChanged(DraftField.LINKED_ULTRASOUND, scan.id)) },
                                                enabled = editingEnabled,
                                                label = { Text(scanLabel) },
                                                modifier = Modifier.minimumTouchTarget().testTag("linkScan-${scan.id}"),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notes Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.notes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            EditorField(
                                value = draft.values.notes,
                                onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.NOTES, it)) },
                                label = stringResource(R.string.notes),
                                tag = "visitNotes",
                                enabled = editingEnabled,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            )
                        }
                    }
                }
            }
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
                text = "Visit records and examinations encrypted locally (AES-256 GCM) · Hardware Vault Security",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showCustomDialog != null) {
        val list = showCustomDialog!!
        val titleRes = when (list) {
            CustomChoiceList.VISIT_BP -> R.string.custom_bp_title
            CustomChoiceList.VISIT_WEIGHT -> R.string.custom_weight_title
            CustomChoiceList.VISIT_FUNDAL_HEIGHT -> R.string.custom_fh_title
            CustomChoiceList.VISIT_OEDEMA -> R.string.custom_oedema_title
            CustomChoiceList.VISIT_MOVEMENTS -> R.string.custom_movements_title
            CustomChoiceList.VISIT_URINE -> R.string.custom_urine_title
            else -> R.string.add_other_choice
        }
        AlertDialog(
            onDismissRequest = { showCustomDialog = null },
            title = { Text(stringResource(titleRes)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = {
                            customInput = it
                            customInputError = null
                        },
                        label = { Text(stringResource(titleRes)) },
                        singleLine = true,
                        isError = customInputError != null,
                        modifier = Modifier.fillMaxWidth().testTag("customChoiceInput"),
                    )
                    if (customInputError != null) {
                        Text(
                            text = customInputError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = customInput.trim()
                        val res = codexRepo.addCustomChoice(list, trimmed, emptyList())
                        when (res) {
                            is CustomChoiceValidationResult.Valid -> {
                                val label = res.sanitizedLabel
                                when (list) {
                                    CustomChoiceList.VISIT_BP -> onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, label))
                                    CustomChoiceList.VISIT_WEIGHT -> onEvent(AncEvent.DraftChanged(DraftField.WEIGHT, label))
                                    CustomChoiceList.VISIT_FUNDAL_HEIGHT -> onEvent(AncEvent.DraftChanged(DraftField.FUNDAL_HEIGHT, label))
                                    CustomChoiceList.VISIT_OEDEMA -> onEvent(AncEvent.DraftChanged(DraftField.OEDEMA, label))
                                    CustomChoiceList.VISIT_MOVEMENTS -> onEvent(AncEvent.DraftChanged(DraftField.FETAL_MOVEMENTS, label))
                                    CustomChoiceList.VISIT_URINE -> onEvent(AncEvent.DraftChanged(DraftField.URINE_PROTEIN, label))
                                    else -> Unit
                                }
                                showCustomDialog = null
                            }
                            is CustomChoiceValidationResult.Invalid -> {
                                customInputError = when (res.reason) {
                                    CustomChoiceValidationResult.Invalid.Reason.BLANK -> customBlankError
                                    CustomChoiceValidationResult.Invalid.Reason.TOO_LONG -> customTooLongError
                                    CustomChoiceValidationResult.Invalid.Reason.DUPLICATE -> customDuplicateError
                                    CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS -> customInvalidCharsError
                                    CustomChoiceValidationResult.Invalid.Reason.NOT_FOUND -> customNotFoundError
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("customChoiceConfirm"),
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCustomDialog = null },
                    modifier = Modifier.testTag("customChoiceCancel"),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun AncState.saveStatusResource(): Int = when (savePhase) {
    SavePhase.IDLE -> R.string.save
    SavePhase.PENDING -> R.string.save_pending
    SavePhase.RECONCILING -> R.string.save_reconciling
    SavePhase.UNRESOLVED -> R.string.save_unresolved
    SavePhase.ERROR -> saveError.stringRes()
}
