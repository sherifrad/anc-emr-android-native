package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.model.PatientVocabulary
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.activeFlowForRoute
import com.sherifradwan.ancnative.state.PatientDraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.PatientDraft
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.FormFieldLabel
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordSpacing
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusKind
import com.sherifradwan.ancnative.ui.components.StatusTag
import com.sherifradwan.ancnative.ui.components.isRtlText
import com.sherifradwan.ancnative.ui.patienthub.components.SheetCancelSaveRow

@Composable
fun PatientEditor(
    state: AncState,
    route: AncRoute.PatientEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val draft = state.patientDraft?.takeIf { it.patientId == route.patientId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR

    val flow = state.activeFlowForRoute()
    val groups = patientEditorGroups(flow?.current, draft.errors)

    EditorPage(
        title = if (route.isNew) stringResource(R.string.register_patient) else stringResource(R.string.edit_patient),
        subtitle = stringResource(R.string.patient_editor_subtitle),
        tag = "patientEditor",
        showHeader = flow == null,
        useRecordStyle = true,
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)
        groups.forEach { group ->
            when (group) {
                PatientEditorGroup.IDENTITY -> IdentityFields(draft, enabled, onEvent)
                PatientEditorGroup.OBSTETRIC -> ObstetricFields(draft, enabled, onEvent)
            }
        }

        if (PatientEditorGroup.IDENTITY in groups) {
            Text(
                text = stringResource(R.string.photos_stored_on_phone_notice),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (flow == null) {
            SheetCancelSaveRow(
                enabled = enabled,
                saving = state.savePhase == SavePhase.PENDING,
                saveEnabled = enabled,
                savingLabel = stringResource(
                    if (state.savePhase == SavePhase.PENDING) R.string.saving_label else R.string.save,
                ),
                onCancel = { onEvent(AncEvent.CancelEditor) },
                onSave = { onEvent(AncEvent.SubmitPatientProfile) },
                cancelTag = "cancelPatient",
                saveTag = "savePatient",
                spinnerTag = "patientSaveSpinner",
            )
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
    val isArabicName = isRtlText(draft.values.displayName)
    RecordSectionCard(
        title = stringResource(R.string.identity_section_title),
        family = FieldFamilies.patient(),
    ) {
        EditorField(
            value = draft.values.displayName,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.DISPLAY_NAME, it)) },
            label = stringResource(R.string.full_name_required),
            tag = "patientName",
            enabled = enabled,
            singleLine = true,
            imeAction = ImeAction.Next,
            error = PatientValidationError.FULL_NAME_REQUIRED in draft.errors,
            shape = RecordShapes.Small,
            textStyle = RecordType.FieldValue.copy(
                textDirection = if (isArabicName) TextDirection.Rtl else TextDirection.Ltr,
                textAlign = if (isArabicName) TextAlign.Right else TextAlign.Left,
            ),
            externalLabel = true,
        )
        EditorField(
            value = draft.values.phone,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.PHONE, it)) },
            label = stringResource(R.string.phone_optional),
            tag = "patientPhone",
            enabled = enabled,
            singleLine = true,
            keyboardType = KeyboardType.Phone,
            shape = RecordShapes.Small,
            textStyle = RecordType.FieldValue,
            externalLabel = true,
        )
        EditorField(
            value = draft.values.ageText,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.AGE_TEXT, it)) },
            label = stringResource(R.string.age_required),
            tag = "patientAgeText",
            enabled = enabled,
            singleLine = true,
            keyboardType = KeyboardType.Number,
            errorMessage = if (PatientValidationError.AGE_REQUIRED in draft.errors) {
                stringResource(R.string.age_required_error)
            } else {
                null
            },
            shape = RecordShapes.Small,
            textStyle = RecordType.FieldValue,
            externalLabel = true,
        )
        EditorField(
            value = draft.values.addressText,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.ADDRESS_TEXT, it)) },
            label = stringResource(R.string.address),
            tag = "patientAddress",
            enabled = enabled,
            singleLine = true,
            shape = RecordShapes.Small,
            textStyle = RecordType.FieldValue,
            externalLabel = true,
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
    RecordSectionCard(
        title = stringResource(R.string.obstetric_risk_section_title),
        family = FieldFamilies.obstetric(),
    ) {
        val gpaError = draft.errors.any {
            it == PatientValidationError.GPA_ALL_OR_NONE || it == PatientValidationError.INVALID_GPA
        }
        val gpaFields: @Composable (Modifier) -> Unit = { itemModifier ->
            EditorField(
                value = draft.values.gravida,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.GRAVIDA, it)) },
                label = stringResource(R.string.gravida_short),
                tag = "patientGravida",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = itemModifier,
                error = gpaError,
                shape = RecordShapes.Small,
                textStyle = RecordType.FieldValue,
                externalLabel = true,
            )
            EditorField(
                value = draft.values.parity,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.PARITY, it)) },
                label = stringResource(R.string.parity_short),
                tag = "patientParity",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = itemModifier,
                error = gpaError,
                shape = RecordShapes.Small,
                textStyle = RecordType.FieldValue,
                externalLabel = true,
            )
            EditorField(
                value = draft.values.abortion,
                onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.ABORTION, it)) },
                label = stringResource(R.string.abortion_short),
                tag = "patientAbortion",
                enabled = enabled,
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = itemModifier,
                error = gpaError,
                shape = RecordShapes.Small,
                textStyle = RecordType.FieldValue,
                externalLabel = true,
            )
        }
        if (LocalDensity.current.fontScale > 1.3f) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                gpaFields(Modifier)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                gpaFields(Modifier.weight(1f))
            }
        }
        EditorField(
            value = draft.values.obstetricNotes,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.OBSTETRIC_NOTES, it)) },
            label = stringResource(R.string.obstetric_notes),
            tag = "patientObstetricNotes",
            enabled = enabled,
            shape = RecordShapes.Small,
            textStyle = RecordType.FieldValue,
            externalLabel = true,
        )
        ClinicalDropdownField(
            label = stringResource(R.string.manual_risk),
            selectedValue = draft.values.riskLevel,
            options = PatientVocabulary.riskLevels.toList(),
            onSelect = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.RISK_LEVEL, it)) },
            enabled = enabled,
            tag = "riskDropdown",
            shape = RecordShapes.Small,
            externalLabel = true,
        )
        ClinicalDropdownField(
            label = stringResource(R.string.blood_group),
            selectedValue = draft.values.bloodGroupText,
            options = PatientVocabulary.bloodGroups.toList(),
            onSelect = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.BLOOD_GROUP, it)) },
            enabled = enabled,
            tag = "bloodGroupDropdown",
            shape = RecordShapes.Small,
            externalLabel = true,
        )
        EditorField(
            value = draft.values.clinicalAttention,
            onValueChange = { onEvent(AncEvent.PatientDraftChanged(PatientDraftField.CLINICAL_ATTENTION, it)) },
            label = stringResource(R.string.clinical_attention_label),
            tag = "patientClinicalAttention",
            enabled = enabled,
            shape = RecordShapes.Small,
            textStyle = RecordType.FieldValue,
            externalLabel = true,
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
        useRecordStyle = true,
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)
        RecordSectionCard(
            title = stringResource(R.string.patient_status),
            family = FieldFamilies.patient(),
            action = if (route.targetStatus == "archived") {
                {
                    StatusTag(
                        kind = StatusKind.Incomplete,
                        text = stringResource(R.string.status_will_be_archived),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            } else {
                null
            },
        ) {
            if (route.targetStatus != "archived") {
                Text(
                    text = stringResource(R.string.unarchive_patient_subtitle),
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.status_reason_notice),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EditorField(
                value = draft.reason,
                onValueChange = { onEvent(AncEvent.StatusReasonChanged(it)) },
                label = stringResource(R.string.reason_required),
                tag = "statusReason",
                enabled = enabled,
                error = PatientValidationError.ARCHIVE_REASON_REQUIRED in draft.errors,
                shape = RecordShapes.Small,
                textStyle = RecordType.FieldValue,
                externalLabel = true,
            )
        }
        SheetCancelSaveRow(
            enabled = enabled,
            saving = state.savePhase == SavePhase.PENDING,
            saveEnabled = enabled,
            savingLabel = stringResource(
                if (state.savePhase == SavePhase.PENDING) R.string.saving_label else R.string.save,
            ),
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitPatientStatus) },
            cancelTag = "cancelPatientStatus",
            saveTag = "savePatientStatus",
            spinnerTag = "patientStatusSaveSpinner",
        )
    }
}

@Composable
internal fun EditorPage(
    title: String,
    subtitle: String,
    tag: String,
    bottomBar: (@Composable () -> Unit)? = null,
    showHeader: Boolean = true,
    useRecordStyle: Boolean = false,
    content: @Composable () -> Unit,
) {
    val itemSpacing = if (useRecordStyle) RecordSpacing.M else 14.dp
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
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                ) {
                    if (showHeader) EditorPageHeader(title, subtitle, tag, useRecordStyle)
                    content()
                }
                HorizontalDivider(
                    color = if (useRecordStyle) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    },
                )
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
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                if (showHeader) EditorPageHeader(title, subtitle, tag, useRecordStyle)
                content()
            }
        }
    }
}

@Composable
private fun EditorPageHeader(
    title: String,
    subtitle: String,
    tag: String,
    useRecordStyle: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = if (useRecordStyle) RecordType.HeaderName else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (useRecordStyle) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }.testTag(tag),
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = if (useRecordStyle) RecordType.Secondary else MaterialTheme.typography.bodyMedium,
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
    labelStyle: TextStyle = MaterialTheme.typography.titleMedium,
    labelColor: Color = Color.Unspecified,
    labelOf: (String) -> String = { it },
    useRecordChip: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = labelStyle, color = labelColor)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(rowGap),
        ) {
            (if (includeUnknown) listOf("").plus(choices) else choices).forEachIndexed { index, value ->
                val displayLabel = if (value.isEmpty()) stringResource(R.string.unknown) else labelOf(value)
                if (useRecordChip) {
                    RecordFilterChip(
                        selected = selected == value,
                        label = displayLabel,
                        onClick = { onSelected(value) },
                        tag = "$tagPrefix-$index",
                        enabled = enabled,
                        wrap = true,
                        minTouchTarget = true,
                    )
                } else {
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelected(value) },
                        enabled = enabled,
                        label = { Text(displayLabel) },
                        modifier = Modifier.minimumTouchTarget().testTag("$tagPrefix-$index"),
                    )
                }
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
    shape: Shape = OutlinedTextFieldDefaults.shape,
    externalLabel: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val dropdownBox: @Composable (Modifier) -> Unit = { boxModifier ->
        Box(modifier = boxModifier) {
            OutlinedTextField(
                value = if (selectedValue.isBlank()) stringResource(R.string.not_specified) else selectedValue,
                onValueChange = {},
                readOnly = true,
                label = if (externalLabel) null else ({ Text(label) }),
                shape = shape,
                textStyle = if (externalLabel) RecordType.FieldValue else LocalTextStyle.current,
                colors = if (externalLabel) editorFieldColors() else OutlinedTextFieldDefaults.colors(),
                trailingIcon = {
                    IconButton(
                        onClick = { if (enabled) expanded = !expanded },
                        modifier = if (externalLabel) Modifier.minimumTouchTarget() else Modifier,
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = label,
                        )
                    }
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (externalLabel) Modifier.semantics { contentDescription = label } else Modifier)
                    .testTag(tag)
                    .clickable(enabled = enabled) { expanded = !expanded },
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = {
                        val emptyText = stringResource(R.string.not_specified_empty)
                        if (externalLabel) Text(emptyText, style = RecordType.FieldValue) else Text(emptyText)
                    },
                    onClick = {
                        onSelect("")
                        expanded = false
                    },
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            if (externalLabel) Text(option, style = RecordType.FieldValue) else Text(option)
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
    if (externalLabel) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.clearAndSetSemantics {}) {
                FormFieldLabel(label)
            }
            dropdownBox(Modifier)
        }
    } else {
        dropdownBox(modifier)
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
