package com.sherifradwan.ancnative.ui

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.model.codex.MedicationCodexItem
import com.sherifradwan.ancnative.model.effectiveMedicationStatus
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.MedicationDraftField
import com.sherifradwan.ancnative.state.SavePhase

@Composable
fun MedicationEditor(
    state: AncState,
    route: AncRoute.MedicationEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.medicationDraft?.takeIf { it.patientId == route.patientId && it.medicationId == route.medicationId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR

    val dosageFocusRequester = remember { FocusRequester() }
    val frequencyFocusRequester = remember { FocusRequester() }

    var showAdvanced by remember {
        mutableStateOf(
            draft.values.endDate.isNotEmpty() ||
                draft.values.adverseNotes.isNotEmpty() ||
                draft.values.notes.isNotEmpty(),
        )
    }

    EditorPage(
        title = stringResource(R.string.medication_editor),
        subtitle = "${patient.displayName} · Pregnancy Medication Management & Prescription",
        tag = "medicationEditor",
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state)

        // 1-Tap Presets from Medication Codex Delegate
        val context = LocalContext.current
        val codexRepo = remember { ClinicalCodexRepository.getInstance(context) }
        val codexMedItems by codexRepo.medicationCodexItems.collectAsState()

        // SINGLE CARD: MEDICATION (Design B)
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
                    text = "Medication",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                QuickPresetChips(
                    title = stringResource(R.string.common_medications),
                    items = codexMedItems.map { it.displayLabel },
                    enabled = enabled,
                    onSelect = { selectedLabel ->
                        val preset = codexMedItems.firstOrNull { it.displayLabel == selectedLabel }
                        if (preset != null) {
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.NAME, preset.name))
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.DOSAGE, preset.dosage))
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ROUTE, preset.route))
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.FREQUENCY, preset.frequency))
                            if (draft.values.startDate.isEmpty()) {
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.START_DATE, java.time.LocalDate.now().toString()))
                            }
                        }
                    },
                    tagPrefix = "medPreset",
                )

                // Save current medication to my list if it's new
                val trimmedMedName = draft.values.name.trim()
                val isAlreadyInMedCodex = codexMedItems.any { it.name.equals(trimmedMedName, ignoreCase = true) }
                if (trimmedMedName.isNotBlank() && !isAlreadyInMedCodex) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("saveToMedCodexBanner"),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "New medication: add to my medication list?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            TextButton(
                                onClick = {
                                    codexRepo.addMedicationCodexItem(
                                        MedicationCodexItem(
                                            name = trimmedMedName,
                                            dosage = draft.values.dosage.trim(),
                                            route = draft.values.route.ifBlank { "Oral" },
                                            frequency = draft.values.frequency.ifBlank { "Once daily" },
                                            isProphylaxis = false,
                                            isCustom = true,
                                        ),
                                    )
                                },
                                modifier = Modifier.minimumTouchTarget().testTag("saveToMedCodexButton"),
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Add to my list")
                            }
                        }
                    }
                }

                // Patient's previous medications suggestion
                val previousMedNames = patient.medications.map { it.name }.distinct().filter { it.isNotBlank() }
                if (previousMedNames.isNotEmpty()) {
                    QuickPresetChips(
                        title = stringResource(R.string.patient_previous_meds),
                        items = previousMedNames,
                        enabled = enabled,
                        onSelect = { medName ->
                            val prior = patient.medications.firstOrNull { it.name == medName }
                            if (prior != null) {
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.NAME, prior.name))
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.DOSAGE, prior.dosage))
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.FREQUENCY, prior.frequency))
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ROUTE, prior.route))
                                // Safety Reset Rule: sets status "Active", wipes adverse notes, end date, and notes
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, "Active"))
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ADVERSE_NOTES, ""))
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.END_DATE, ""))
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.NOTES, ""))
                            }
                        },
                        tagPrefix = "patientPrevMed",
                    )
                }

                // Side-by-side: Medication Name & Dosage with Focus Traversal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1.3f)) {
                        EditorField(
                            value = draft.values.name,
                            onValueChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.NAME, it)) },
                            label = stringResource(R.string.medication_name),
                            tag = "medName",
                            enabled = enabled,
                            error = draft.errors.contains(ClinicalValidationError.REQUIRED_NAME),
                            singleLine = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            keyboardActions = KeyboardActions(onNext = { dosageFocusRequester.requestFocus() }),
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.dosage,
                            onValueChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.DOSAGE, it)) },
                            label = stringResource(R.string.dosage),
                            tag = "medDosage",
                            enabled = enabled,
                            modifier = Modifier.focusRequester(dosageFocusRequester),
                            error = draft.errors.contains(ClinicalValidationError.REQUIRED_DOSAGE),
                            singleLine = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            keyboardActions = KeyboardActions(onNext = { frequencyFocusRequester.requestFocus() }),
                        )
                    }
                }

                // Route Choice
                ChoiceGroup(
                    label = stringResource(R.string.route),
                    selected = draft.values.route.ifEmpty { "Oral" },
                    choices = ClinicalVocabulary.medicationRoutes.toList(),
                    enabled = enabled,
                    tagPrefix = "medRouteChoice",
                    onSelected = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ROUTE, it)) },
                )

                // Frequency presets & custom input
                QuickPresetChips(
                    title = stringResource(R.string.frequency),
                    items = COMMON_MEDICATION_FREQUENCIES,
                    enabled = enabled,
                    onSelect = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.FREQUENCY, it)) },
                    tagPrefix = "freqPreset",
                )

                EditorField(
                    value = draft.values.frequency,
                    onValueChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.FREQUENCY, it)) },
                    label = stringResource(R.string.frequency),
                    tag = "medFrequency",
                    enabled = enabled,
                    modifier = Modifier.focusRequester(frequencyFocusRequester),
                    error = draft.errors.contains(ClinicalValidationError.REQUIRED_FREQUENCY),
                    singleLine = true,
                    keyboardType = KeyboardType.Text,
                )

                // Start date with today button
                ClinicalDateField(
                    value = draft.values.startDate,
                    onValueChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.START_DATE, it)) },
                    label = stringResource(R.string.start_date),
                    tag = "medStartDate",
                    enabled = enabled,
                    error = draft.errors.contains(ClinicalValidationError.REQUIRED_DATE) ||
                        draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                    showTodayButton = true,
                )

                // Status ChoiceGroup showing exactly the four statuses
                ChoiceGroup(
                    label = stringResource(R.string.status),
                    selected = effectiveMedicationStatus(draft.values.status.ifEmpty { "Active" }, draft.values.isNonCompliant),
                    choices = ClinicalVocabulary.medicationStatuses,
                    enabled = enabled,
                    tagPrefix = "medStatusChoice",
                    onSelected = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, it)) },
                    includeUnknown = false,
                )

                // Collapsible "More" section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "More (End date, Adverse effects & Notes)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            TextButton(
                                onClick = { showAdvanced = !showAdvanced },
                                modifier = Modifier.minimumTouchTarget(),
                            ) {
                                Text(if (showAdvanced) "▲" else "▼")
                            }
                        }

                        if (showAdvanced) {
                            ClinicalDateField(
                                value = draft.values.endDate,
                                onValueChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.END_DATE, it)) },
                                label = stringResource(R.string.end_date),
                                tag = "medEndDate",
                                enabled = enabled,
                                error = draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                                showTodayButton = false,
                            )

                            EditorField(
                                value = draft.values.adverseNotes,
                                onValueChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ADVERSE_NOTES, it)) },
                                label = stringResource(R.string.adverse_effects),
                                tag = "medAdverseNotes",
                                enabled = enabled,
                                singleLine = false,
                            )

                            EditorField(
                                value = draft.values.notes,
                                onValueChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.NOTES, it)) },
                                label = stringResource(R.string.notes),
                                tag = "medNotes",
                                enabled = enabled,
                                singleLine = false,
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
                text = "Medication and prescription records encrypted locally (AES-256 GCM) · Local Vault Security",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Actions: Cancel, Save & Add Another Medication, Save
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onEvent(AncEvent.CancelEditor) },
                enabled = enabled,
                modifier = Modifier.minimumTouchTarget().testTag("cancelMedication"),
            ) {
                Text(stringResource(R.string.cancel))
            }
            OutlinedButton(
                onClick = { onEvent(AncEvent.SubmitMedicationAndAddAnother) },
                enabled = enabled,
                modifier = Modifier.minimumTouchTarget().testTag("addAnotherMedication"),
            ) {
                Text(stringResource(R.string.save_and_add_another_med))
            }
            Button(
                onClick = { onEvent(AncEvent.SubmitMedication) },
                enabled = enabled,
                modifier = Modifier.minimumTouchTarget().testTag("saveMedication"),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
