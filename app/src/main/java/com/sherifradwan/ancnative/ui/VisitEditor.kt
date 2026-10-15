package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DraftError
import com.sherifradwan.ancnative.state.DraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.SaveUiError

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
                text = if (draft.visitId == null) stringResource(R.string.add_visit) else stringResource(R.string.edit_visit),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }.testTag("visitEditor"),
            )
            Text("${patient.displayName} · ${patient.caseId}", color = MaterialTheme.colorScheme.tertiary)

            if (DraftError.STALE_REVISION in draft.errors) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.stale_visit),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp).testTag("staleError"),
                    )
                }
            }

            if (state.savePhase != SavePhase.IDLE) {
                Card(modifier = Modifier.fillMaxWidth().testTag("saveStatus")) {
                    Text(
                        text = stringResource(state.saveStatusResource()),
                        color = if (state.savePhase == SavePhase.ERROR || state.savePhase == SavePhase.UNRESOLVED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.unsaved_drafts_editor_policy),
                color = MaterialTheme.colorScheme.tertiary,
            )

            val dateError = when {
                DraftError.DATE_REQUIRED in draft.errors -> stringResource(R.string.required_date)
                DraftError.INVALID_DATE in draft.errors -> stringResource(R.string.invalid_date)
                else -> null
            }
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

            val calculatedGa = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.values.date)
            if (calculatedGa != null && calculatedGa.isPostConception) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("visitCalculatedGaCard"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.calculated_ga_at_visit, calculatedGa.displayText),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            val weightFocusRequester = remember { FocusRequester() }
            val findingsFocusRequester = remember { FocusRequester() }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 340.dp
                if (isNarrow) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisitField(
                            value = draft.values.bloodPressure,
                            onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, it)) },
                            label = "${stringResource(R.string.blood_pressure)} (mmHg)",
                            tag = "visitBloodPressure",
                            singleLine = true,
                            enabled = editingEnabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { weightFocusRequester.requestFocus() }),
                        )
                        VisitField(
                            value = draft.values.weight,
                            onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.WEIGHT, it)) },
                            label = "${stringResource(R.string.weight)} (kg)",
                            tag = "visitWeight",
                            singleLine = true,
                            enabled = editingEnabled,
                            modifier = Modifier.focusRequester(weightFocusRequester),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { findingsFocusRequester.requestFocus() }),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        VisitField(
                            value = draft.values.bloodPressure,
                            onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, it)) },
                            label = "${stringResource(R.string.blood_pressure)} (mmHg)",
                            tag = "visitBloodPressure",
                            singleLine = true,
                            enabled = editingEnabled,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { weightFocusRequester.requestFocus() }),
                        )
                        VisitField(
                            value = draft.values.weight,
                            onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.WEIGHT, it)) },
                            label = "${stringResource(R.string.weight)} (kg)",
                            tag = "visitWeight",
                            singleLine = true,
                            enabled = editingEnabled,
                            modifier = Modifier.weight(1f).focusRequester(weightFocusRequester),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { findingsFocusRequester.requestFocus() }),
                        )
                    }
                }
            }

            QuickPresetChips(
                title = stringResource(R.string.blood_pressure),
                items = ClinicalPresets.commonBloodPressures,
                enabled = editingEnabled,
                onSelect = { onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, it)) },
                tagPrefix = "bpPreset",
            )
            VisitField(
                value = draft.values.findings,
                onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.FINDINGS, it)) },
                label = stringResource(R.string.findings),
                tag = "visitFindings",
                modifier = Modifier.focusRequester(findingsFocusRequester),
                enabled = editingEnabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            )
            QuickPresetChips(
                title = stringResource(R.string.findings),
                items = ClinicalPresets.commonVisitFindings,
                enabled = editingEnabled,
                onSelect = { onEvent(AncEvent.DraftChanged(DraftField.FINDINGS, it)) },
                tagPrefix = "findingsPreset",
            )

            // Linked Ultrasound Scan Section
            Card(
                modifier = Modifier.fillMaxWidth().testTag("visitLinkedUltrasoundCard"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.linked_ultrasound),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (draft.values.linkedUltrasoundId.isNotBlank()) {
                        val linkedScan = patient.ultrasoundScans.firstOrNull { it.id == draft.values.linkedUltrasoundId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val gaText = if (linkedScan?.gestationalAgeWeeks != null) {
                                    " (${linkedScan.gestationalAgeWeeks}w ${linkedScan.gestationalAgeDays ?: 0}d)"
                                } else ""
                                Text(
                                    text = stringResource(R.string.ultrasound) + ": " + (linkedScan?.date ?: draft.values.linkedUltrasoundId) + gaText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (linkedScan?.impression?.isNotBlank() == true) {
                                    Text(
                                        text = linkedScan.impression,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
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
                            color = MaterialTheme.colorScheme.tertiary,
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

            VisitField(
                value = draft.values.notes,
                onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.NOTES, it)) },
                label = stringResource(R.string.notes),
                tag = "visitNotes",
                enabled = editingEnabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onEvent(AncEvent.CancelEditor) },
                    enabled = editingEnabled,
                    modifier = Modifier.minimumTouchTarget().testTag("cancelVisit"),
                ) {
                    Text(stringResource(R.string.cancel), maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = onSave,
                    enabled = editingEnabled,
                    modifier = Modifier.minimumTouchTarget().testTag("saveVisit"),
                ) {
                    Text(stringResource(R.string.save), maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun VisitField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = error?.let { message -> ({ Text(message) }) },
        isError = error != null,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth().testTag(tag),
    )
}

private fun AncState.saveStatusResource(): Int = when (savePhase) {
    SavePhase.IDLE -> R.string.save
    SavePhase.PENDING -> R.string.save_pending
    SavePhase.RECONCILING -> R.string.save_reconciling
    SavePhase.UNRESOLVED -> R.string.save_unresolved
    SavePhase.ERROR -> when (saveError) {
        SaveUiError.CONFIRMED_NOT_SAVED -> R.string.save_confirmed_not_saved
        SaveUiError.STALE_OR_CONFLICT -> R.string.save_stale_conflict
        SaveUiError.CROSS_PATIENT_REJECTED -> R.string.save_cross_patient_rejected
        SaveUiError.DUPLICATE_OPERATION_MISMATCH -> R.string.save_operation_mismatch
        SaveUiError.PATIENT_ARCHIVED -> R.string.save_patient_archived
        SaveUiError.CASE_ID_CONFLICT -> R.string.save_case_id_conflict
        SaveUiError.STORAGE_ERROR, null -> R.string.save_storage_error
        SaveUiError.OUTCOME_UNRESOLVED -> R.string.save_unresolved
    }
}
