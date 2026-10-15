package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.model.PatientVocabulary
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PatientDraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.SaveUiError

@Composable
fun PatientEditor(
    state: AncState,
    route: AncRoute.PatientEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val draft = state.patientDraft?.takeIf { it.patientId == route.patientId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR
    EditorPage(
        title = if (route.isNew) stringResource(R.string.register_patient) else stringResource(R.string.edit_patient),
        subtitle = stringResource(R.string.synthetic_patient_editor_notice),
        tag = "patientEditor",
    ) {
        EditorErrorCard(state, draft.errors)

        // Auto-generate Case ID if registering a new patient and caseId is empty
        LaunchedEffect(route.isNew, draft.values.caseId) {
            if (route.isNew && draft.values.caseId.isBlank()) {
                val year = java.time.LocalDate.now().year
                val nextSeq = (state.patients.size + 1).toString().padStart(3, '0')
                onEvent(AncEvent.PatientDraftChanged(PatientDraftField.CASE_ID, "ANC-$year-$nextSeq"))
            }
        }

        // 1. Full Name
        EditorField(
            value = draft.values.displayName,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.DISPLAY_NAME, it)) },
            label = stringResource(R.string.full_name_required),
            tag = "patientName",
            enabled = enabled,
            error = PatientValidationError.FULL_NAME_REQUIRED in draft.errors,
        )

        // 2. Case ID (Auto-generated with clear label)
        EditorField(
            value = draft.values.caseId,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.CASE_ID, it)) },
            label = "كود الحالة (تلقائي) · Case ID",
            tag = "patientCaseId",
            enabled = enabled,
            error = PatientValidationError.CASE_ID_REQUIRED in draft.errors ||
                PatientValidationError.CASE_ID_CONFLICT in draft.errors,
        )

        // 3. Compact Demographics Row: Age + Phone
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EditorField(
                value = draft.values.ageText,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.AGE_TEXT, it)) },
                label = stringResource(R.string.age_text),
                tag = "patientAgeText",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            EditorField(
                value = draft.values.phone,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.PHONE, it)) },
                label = stringResource(R.string.phone),
                tag = "patientPhone",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.weight(1f),
            )
        }

        // 4. Compact Demographics Row: Date of Birth + Address
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ClinicalDateField(
                value = draft.values.dateOfBirth,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.DATE_OF_BIRTH, it)) },
                label = stringResource(R.string.date_of_birth),
                tag = "patientDateOfBirth",
                enabled = enabled,
                showTodayButton = false,
                modifier = Modifier.weight(1f),
            )
            EditorField(
                value = draft.values.addressText,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.ADDRESS_TEXT, it)) },
                label = stringResource(R.string.address),
                tag = "patientAddress",
                enabled = enabled,
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        // 5. Professional Dropdowns: Blood Group + Risk Level
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ClinicalDropdownField(
                label = stringResource(R.string.blood_group),
                selectedValue = draft.values.bloodGroupText,
                options = PatientVocabulary.bloodGroups.toList(),
                onSelect = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.BLOOD_GROUP, it)) },
                enabled = enabled,
                tag = "bloodGroupDropdown",
                modifier = Modifier.weight(1f),
            )
            ClinicalDropdownField(
                label = stringResource(R.string.manual_risk),
                selectedValue = draft.values.riskLevel,
                options = PatientVocabulary.riskLevels.toList(),
                onSelect = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.RISK_LEVEL, it)) },
                enabled = enabled,
                tag = "riskDropdown",
                modifier = Modifier.weight(1f),
            )
        }

        // 6. GPA Matrix on ONE Single Row (3 Compact Boxes Side-by-Side)
        Text(
            text = "الصيغة الولادية (GPA) · Gravida, Parity, Abortion",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EditorField(
                value = draft.values.gravida,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.GRAVIDA, it)) },
                label = "G (مرات الحمل)",
                tag = "patientGravida",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                error = draft.errors.any { it == PatientValidationError.GPA_ALL_OR_NONE || it == PatientValidationError.INVALID_GPA },
            )
            EditorField(
                value = draft.values.parity,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.PARITY, it)) },
                label = "P (الولادات)",
                tag = "patientParity",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                error = draft.errors.any { it == PatientValidationError.GPA_ALL_OR_NONE || it == PatientValidationError.INVALID_GPA },
            )
            EditorField(
                value = draft.values.abortion,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.ABORTION, it)) },
                label = "A (الإجهاض)",
                tag = "patientAbortion",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                error = draft.errors.any { it == PatientValidationError.GPA_ALL_OR_NONE || it == PatientValidationError.INVALID_GPA },
            )
        }

        // 7. Previous Pregnancies & Obstetric History Bridge
        val existingPatient = state.patient(route.patientId)
        if (existingPatient != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "سجل الحمول والولادات السابقة (${existingPatient.previousPregnancies.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        OutlinedButton(
                            onClick = {
                                onEvent(
                                    AncEvent.StartPregnancyEditor(
                                        patientId = existingPatient.id,
                                        pregnancyId = null,
                                        newPregnancyId = java.util.UUID.randomUUID().toString(),
                                        editorSessionId = java.util.UUID.randomUUID().toString(),
                                    ),
                                )
                            },
                            modifier = Modifier.minimumTouchTarget(),
                        ) {
                            Text("+ إضافة حمل سابق")
                        }
                    }
                    if (existingPatient.previousPregnancies.isEmpty()) {
                        Text(
                            text = "لم يتم تسجيل ولادات سابقة (بكرية)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        existingPatient.previousPregnancies.forEach { pp ->
                            Text(
                                text = "• ${pp.dateText ?: "سابق"} : ${pp.birthMethod ?: pp.outcomeText ?: "ولادة"} (${pp.outcome ?: "مولود حي"})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // 8. Obstetric Notes
        EditorField(
            value = draft.values.obstetricNotes,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.OBSTETRIC_NOTES, it)) },
            label = stringResource(R.string.obstetric_notes),
            tag = "patientObstetricNotes",
            enabled = enabled,
        )

        // 9. Clinical Attention & High-Risk Alerts
        EditorField(
            value = draft.values.clinicalAttention,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.CLINICAL_ATTENTION, it)) },
            label = "تنبيهات إكلينيكية هامة (Clinical Attention / Alerts) - سطر لكل تنبيه",
            tag = "patientClinicalAttention",
            enabled = enabled,
        )
        EditorActions(
            enabled = enabled,
            saveTag = "savePatient",
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitPatientProfile) },
        )
    }
}

@Composable
fun PatientStatusEditor(
    state: AncState,
    route: AncRoute.StatusEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.statusDraft?.takeIf { it.patientId == route.patientId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR
    EditorPage(
        title = if (route.targetStatus == "archived") {
            stringResource(R.string.archive_patient)
        } else {
            stringResource(R.string.unarchive_patient)
        },
        subtitle = "${patient.displayName} · ${patient.caseId}",
        tag = "patientStatusEditor",
    ) {
        EditorErrorCard(state, draft.errors)
        Text(stringResource(R.string.status_reason_notice), color = MaterialTheme.colorScheme.tertiary)
        EditorField(
            value = draft.reason,
            onValueChange = { onEvent(AncEvent.StatusReasonChanged(it)) },
            label = stringResource(R.string.reason_required),
            tag = "statusReason",
            enabled = enabled,
            error = PatientValidationError.ARCHIVE_REASON_REQUIRED in draft.errors,
        )
        EditorActions(
            enabled = enabled,
            saveTag = "savePatientStatus",
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitPatientStatus) },
        )
    }
}

@Composable
internal fun EditorPage(
    title: String,
    subtitle: String,
    tag: String,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = maxOf(16.dp, (maxWidth - 720.dp) / 2)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }.testTag(tag),
            )
            Text(subtitle, color = MaterialTheme.colorScheme.tertiary)
            Text(stringResource(R.string.unsaved_drafts_editor_policy), color = MaterialTheme.colorScheme.tertiary)
            content()
        }
    }
}

@Composable
internal fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = if (error) ({ Text(stringResource(R.string.correct_highlighted_field)) }) else null,
        isError = error,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth().testTag(tag),
    )
}

@Composable
internal fun ChoiceGroup(
    label: String,
    selected: String,
    choices: List<String>,
    enabled: Boolean,
    tagPrefix: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("").plus(choices).forEachIndexed { index, value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    enabled = enabled,
                    label = { Text(if (value.isEmpty()) stringResource(R.string.unknown) else value) },
                    modifier = Modifier.minimumTouchTarget().testTag("$tagPrefix-$index"),
                )
            }
        }
    }
}

@Composable
internal fun ClinicalDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    enabled: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = if (selectedValue.isBlank()) "غير محدد" else selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { if (enabled) expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = label,
                    )
                }
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
                .clickable(enabled = enabled) { expanded = !expanded },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("غير محدد (فارغ)") },
                onClick = {
                    onSelect("")
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun EditorActions(
    enabled: Boolean,
    saveTag: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onCancel, enabled = enabled, modifier = Modifier.minimumTouchTarget()) {
            Text(stringResource(R.string.cancel))
        }
        Button(onClick = onSave, enabled = enabled, modifier = Modifier.minimumTouchTarget().testTag(saveTag)) {
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
internal fun EditorErrorCard(state: AncState, validationErrors: Set<PatientValidationError>) {
    if (validationErrors.isEmpty() && state.savePhase == SavePhase.IDLE) return
    Card(modifier = Modifier.fillMaxWidth().testTag("editorError")) {
        Text(
            text = when {
                validationErrors.isNotEmpty() -> stringResource(R.string.correct_highlighted_fields)
                state.savePhase == SavePhase.PENDING -> stringResource(R.string.save_pending)
                state.savePhase == SavePhase.RECONCILING -> stringResource(R.string.save_reconciling)
                state.savePhase == SavePhase.UNRESOLVED -> stringResource(R.string.save_unresolved)
                else -> stringResource(state.patientEditorErrorResource())
            },
            color = if (validationErrors.isNotEmpty() || state.savePhase == SavePhase.ERROR || state.savePhase == SavePhase.UNRESOLVED) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
internal fun ClinicalEditorErrorCard(state: AncState, validationErrors: Set<ClinicalValidationError>) {
    if (validationErrors.isEmpty() && state.savePhase == SavePhase.IDLE) return
    Card(modifier = Modifier.fillMaxWidth().testTag("editorError")) {
        Text(
            text = when {
                validationErrors.isNotEmpty() -> stringResource(R.string.correct_highlighted_fields)
                state.savePhase == SavePhase.PENDING -> stringResource(R.string.save_pending)
                state.savePhase == SavePhase.RECONCILING -> stringResource(R.string.save_reconciling)
                state.savePhase == SavePhase.UNRESOLVED -> stringResource(R.string.save_unresolved)
                else -> stringResource(state.patientEditorErrorResource())
            },
            color = if (validationErrors.isNotEmpty() || state.savePhase == SavePhase.ERROR || state.savePhase == SavePhase.UNRESOLVED) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun AncState.patientEditorErrorResource(): Int = when (saveError) {
    SaveUiError.CONFIRMED_NOT_SAVED -> R.string.save_confirmed_not_saved
    SaveUiError.STALE_OR_CONFLICT -> R.string.save_stale_conflict
    SaveUiError.CROSS_PATIENT_REJECTED -> R.string.save_cross_patient_rejected
    SaveUiError.DUPLICATE_OPERATION_MISMATCH -> R.string.save_operation_mismatch
    SaveUiError.PATIENT_ARCHIVED -> R.string.save_patient_archived
    SaveUiError.CASE_ID_CONFLICT -> R.string.save_case_id_conflict
    SaveUiError.OUTCOME_UNRESOLVED -> R.string.save_unresolved
    SaveUiError.STORAGE_ERROR, null -> R.string.save_storage_error
}
