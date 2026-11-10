package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.model.PatientVocabulary
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PatientDraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.PatientDraft

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
        subtitle = stringResource(R.string.patient_editor_subtitle),
        tag = "patientEditor",
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state)
        IdentityFields(draft, enabled, onEvent)
        ObstetricFields(draft, enabled, onEvent)

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
                text = stringResource(R.string.photos_stored_on_phone_notice),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        EditorActions(
            enabled = enabled,
            saveTag = "savePatient",
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitPatientProfile) },
        )
    }
}

@Composable
private fun EditorSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

/** Name, phone (optional), age (required) and address. Date of birth stays in the draft untouched. */
@Composable
fun IdentityFields(
    draft: PatientDraft,
    enabled: Boolean,
    onEvent: (AncEvent) -> Unit,
) {
    EditorSectionCard(stringResource(R.string.identity_section_title)) {
        EditorField(
            value = draft.values.displayName,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.DISPLAY_NAME, it)) },
            label = stringResource(R.string.full_name_required),
            tag = "patientName",
            enabled = enabled,
            singleLine = true,
            imeAction = ImeAction.Next,
            error = PatientValidationError.FULL_NAME_REQUIRED in draft.errors,
        )
        EditorField(
            value = draft.values.phone,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.PHONE, it)) },
            label = stringResource(R.string.phone_optional),
            tag = "patientPhone",
            enabled = enabled,
            singleLine = true,
            keyboardType = KeyboardType.Phone,
        )
        EditorField(
            value = draft.values.ageText,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.AGE_TEXT, it)) },
            label = stringResource(R.string.age_required),
            tag = "patientAgeText",
            enabled = enabled,
            singleLine = true,
            keyboardType = KeyboardType.Number,
        )
        EditorField(
            value = draft.values.addressText,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.ADDRESS_TEXT, it)) },
            label = stringResource(R.string.address),
            tag = "patientAddress",
            enabled = enabled,
            singleLine = true,
        )
    }
}

/** Gravida, parity, abortion, obstetric notes, risk level, blood group and clinical attention. */
@Composable
fun ObstetricFields(
    draft: PatientDraft,
    enabled: Boolean,
    onEvent: (AncEvent) -> Unit,
) {
    EditorSectionCard(stringResource(R.string.obstetric_risk_section_title)) {
        val gpaError = draft.errors.any {
            it == PatientValidationError.GPA_ALL_OR_NONE || it == PatientValidationError.INVALID_GPA
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EditorField(
                value = draft.values.gravida,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.GRAVIDA, it)) },
                label = stringResource(R.string.gravida_short),
                tag = "patientGravida",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                error = gpaError,
            )
            EditorField(
                value = draft.values.parity,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.PARITY, it)) },
                label = stringResource(R.string.parity_short),
                tag = "patientParity",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                error = gpaError,
            )
            EditorField(
                value = draft.values.abortion,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.ABORTION, it)) },
                label = stringResource(R.string.abortion_short),
                tag = "patientAbortion",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                error = gpaError,
            )
        }
        EditorField(
            value = draft.values.obstetricNotes,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.OBSTETRIC_NOTES, it)) },
            label = stringResource(R.string.obstetric_notes),
            tag = "patientObstetricNotes",
            enabled = enabled,
        )
        ClinicalDropdownField(
            label = stringResource(R.string.manual_risk),
            selectedValue = draft.values.riskLevel,
            options = PatientVocabulary.riskLevels.toList(),
            onSelect = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.RISK_LEVEL, it)) },
            enabled = enabled,
            tag = "riskDropdown",
        )
        ClinicalDropdownField(
            label = stringResource(R.string.blood_group),
            selectedValue = draft.values.bloodGroupText,
            options = PatientVocabulary.bloodGroups.toList(),
            onSelect = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.BLOOD_GROUP, it)) },
            enabled = enabled,
            tag = "bloodGroupDropdown",
        )
        EditorField(
            value = draft.values.clinicalAttention,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.CLINICAL_ATTENTION, it)) },
            label = stringResource(R.string.clinical_attention_label),
            tag = "patientClinicalAttention",
            enabled = enabled,
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
        subtitle = patient.displayName,
        tag = "patientStatusEditor",
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state)
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
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = maxOf(16.dp, (maxWidth - 720.dp) / 2)
        if (bottomBar != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    EditorPageHeader(title, subtitle, tag)
                    content()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = horizontalPadding, vertical = 12.dp),
                    ) {
                        bottomBar()
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                EditorPageHeader(title, subtitle, tag)
                content()
            }
        }
    }
}

@Composable
private fun EditorPageHeader(title: String, subtitle: String, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }.testTag(tag),
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ChoiceGroup(
    label: String,
    selected: String,
    choices: List<String>,
    enabled: Boolean,
    tagPrefix: String,
    onSelected: (String) -> Unit,
    includeUnknown: Boolean = true,
    /** Gap between chip rows; chips already carry a 48 dp touch area, so a tight list can pass 0.dp. */
    rowGap: Dp = 8.dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(rowGap),
        ) {
            (if (includeUnknown) listOf("").plus(choices) else choices).forEachIndexed { index, value ->
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
            value = if (selectedValue.isBlank()) stringResource(R.string.not_specified) else selectedValue,
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
                text = { Text(stringResource(R.string.not_specified_empty)) },
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
