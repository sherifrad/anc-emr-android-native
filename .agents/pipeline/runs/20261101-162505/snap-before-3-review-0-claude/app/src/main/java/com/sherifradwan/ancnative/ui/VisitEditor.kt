package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.bpLevel
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DraftError
import com.sherifradwan.ancnative.state.DraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.stringRes

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

    EditorPage(
        title = if (draft.visitId == null) stringResource(R.string.add_visit) else stringResource(R.string.edit_visit),
        subtitle = "${patient.displayName} · Routine Antenatal Visit & Vitals",
        tag = "visitEditor",
    ) {
        if (DraftError.STALE_REVISION in draft.errors) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    text = stringResource(R.string.stale_visit),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp).testTag("staleError"),
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

        // CARD 1: VISIT TIMING & GESTATIONAL AGE
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
                    text = "1. Visit Timing & Gestational Age",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                val dateError = when {
                    DraftError.DATE_REQUIRED in draft.errors -> stringResource(R.string.required_date)
                    DraftError.INVALID_DATE in draft.errors -> stringResource(R.string.invalid_date)
                    DraftError.DATE_IN_FUTURE in draft.errors -> stringResource(R.string.visit_date_in_future)
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
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
            }
        }

        // CARD 2: VITALS & CLINICAL EXAMINATION
        val weightFocusRequester = remember { FocusRequester() }
        val findingsFocusRequester = remember { FocusRequester() }

        val bpError = when {
            DraftError.BLOOD_PRESSURE_INVALID in draft.errors -> stringResource(R.string.blood_pressure_invalid)
            else -> null
        }
        val weightError = when {
            DraftError.WEIGHT_INVALID in draft.errors -> stringResource(R.string.weight_invalid)
            else -> null
        }

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
                    text = "2. Vitals & Physical Examination",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < 340.dp
                    if (isNarrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditorField(
                                value = draft.values.bloodPressure,
                                onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, it)) },
                                label = "${stringResource(R.string.blood_pressure)} (mmHg)",
                                tag = "visitBloodPressure",
                                errorMessage = bpError,
                                singleLine = true,
                                enabled = editingEnabled,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { weightFocusRequester.requestFocus() }),
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
                                modifier = Modifier.focusRequester(weightFocusRequester),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { findingsFocusRequester.requestFocus() }),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                EditorField(
                                    value = draft.values.bloodPressure,
                                    onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, it)) },
                                    label = "${stringResource(R.string.blood_pressure)} (mmHg)",
                                    tag = "visitBloodPressure",
                                    errorMessage = bpError,
                                    singleLine = true,
                                    enabled = editingEnabled,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { weightFocusRequester.requestFocus() }),
                                )
                                if (bpLevel(draft.values.bloodPressure) != BpLevel.NORMAL) {
                                    BpAlertLabel(draft.values.bloodPressure)
                                }
                            }
                            EditorField(
                                value = draft.values.weight,
                                onValueChange = { onEvent(AncEvent.DraftChanged(DraftField.WEIGHT, it)) },
                                label = "${stringResource(R.string.weight)} (kg)",
                                tag = "visitWeight",
                                errorMessage = weightError,
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

                EditorField(
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
            }
        }

        // CARD 3: LINKED ULTRASOUND & NOTES
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
                    text = "3. Linked Scans & Clinical Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Linked Ultrasound Scan Section
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

        // Form action buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onEvent(AncEvent.CancelEditor) },
                enabled = editingEnabled,
                modifier = Modifier.weight(1f).minimumTouchTarget().testTag("cancelVisit"),
            ) {
                Text(stringResource(R.string.cancel), maxLines = 1, softWrap = false)
            }
            Button(
                onClick = onSave,
                enabled = editingEnabled,
                modifier = Modifier.weight(1f).minimumTouchTarget().testTag("saveVisit"),
            ) {
                Text(stringResource(R.string.save), maxLines = 1, softWrap = false)
            }
        }
    }
}

private fun AncState.saveStatusResource(): Int = when (savePhase) {
    SavePhase.IDLE -> R.string.save
    SavePhase.PENDING -> R.string.save_pending
    SavePhase.RECONCILING -> R.string.save_reconciling
    SavePhase.UNRESOLVED -> R.string.save_unresolved
    SavePhase.ERROR -> saveError.stringRes()
}
