package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
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
                (draft.values.route.isNotEmpty() && draft.values.route != "Oral") ||
                (draft.values.status.isNotEmpty() && draft.values.status != "Active"),
        )
    }

    EditorPage(
        title = stringResource(R.string.medication_editor),
        subtitle = "${patient.displayName} · إدارة وتدوين الوصفات الدوائية للحمل",
        tag = "medicationEditor",
    ) {
        ClinicalEditorErrorCard(state, draft.errors)

        // 1-Tap Presets from Medication Codex Delegate
        val context = LocalContext.current
        val codexRepo = remember { ClinicalCodexRepository.getInstance(context) }
        val codexMedItems by codexRepo.medicationCodexItems.collectAsState()

        // CARD 1: MEDICATION SELECTION, DOSAGE & TIMING
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
                    text = "1. بيانات الدواء والجرعة وتاريخ البدء · Medication & Presets",
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
                            onEvent(AncEvent.MedicationProphylaxisChanged(preset.isProphylaxis))
                            if (draft.values.startDate.isEmpty()) {
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.START_DATE, java.time.LocalDate.now().toString()))
                            }
                        }
                    },
                    tagPrefix = "medPreset",
                )

                // Save current medication to Codex if it's new and not in Codex
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
                                text = "دواء جديد: حفظ في دليل الأدوية للمرضى؟",
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
                                            isProphylaxis = draft.values.isProphylaxis,
                                            isCustom = true,
                                        ),
                                    )
                                },
                                modifier = Modifier.testTag("saveToMedCodexButton"),
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("حفظ في الدليل")
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
                                onEvent(AncEvent.MedicationProphylaxisChanged(prior.isProphylaxis))
                                // Safety Reset Rule: Wipes non-compliance, adverse notes, end date, and notes
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.IS_NON_COMPLIANT, "false"))
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

                // Side-by-side: Frequency & Start Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1.1f)) {
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
                    }
                    Box(modifier = Modifier.weight(1f)) {
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
                    }
                }

                // Frequency shortcuts
                QuickPresetChips(
                    title = stringResource(R.string.frequency),
                    items = listOf("Once daily", "Twice daily", "Three times daily", "As needed (PRN)"),
                    enabled = enabled,
                    onSelect = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.FREQUENCY, it)) },
                    tagPrefix = "freqPreset",
                )
            }
        }

        // CARD 2: ROUTE, ADVANCED OPTIONS & ADHERENCE
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
                    text = "2. مسار الإعطاء والخيارات المتقدمة · Administration & Adherence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Collapsible Advanced Section: Route (default Oral), End Date, Status (default Active)
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
                                text = stringResource(R.string.advanced_med_options) + if (!showAdvanced) " (Route: ${draft.values.route.ifEmpty { "Oral" }})" else "",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                                Text(if (showAdvanced) "▲" else "▼")
                            }
                        }

                        if (showAdvanced) {
                            ChoiceGroup(
                                label = stringResource(R.string.route),
                                selected = draft.values.route.ifEmpty { "Oral" },
                                choices = ClinicalVocabulary.medicationRoutes.toList(),
                                enabled = enabled,
                                tagPrefix = "medRouteChoice",
                                onSelected = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ROUTE, it)) },
                            )

                            ClinicalDateField(
                                value = draft.values.endDate,
                                onValueChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.END_DATE, it)) },
                                label = stringResource(R.string.end_date),
                                tag = "medEndDate",
                                enabled = enabled,
                                error = draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                                showTodayButton = false,
                            )

                            ChoiceGroup(
                                label = stringResource(R.string.status),
                                selected = draft.values.status.ifEmpty { "Active" },
                                choices = ClinicalVocabulary.medicationStatuses.toList(),
                                enabled = enabled,
                                tagPrefix = "medStatusChoice",
                                onSelected = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, it)) },
                            )
                        }
                    }
                }

                // Prophylaxis Switch Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { onEvent(AncEvent.MedicationProphylaxisChanged(!draft.values.isProphylaxis)) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (draft.values.isProphylaxis) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.is_prophylaxis),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (draft.values.isProphylaxis) FontWeight.Bold else FontWeight.Normal,
                        )
                        Switch(
                            checked = draft.values.isProphylaxis,
                            onCheckedChange = { onEvent(AncEvent.MedicationProphylaxisChanged(it)) },
                            enabled = enabled,
                            modifier = Modifier.testTag("medProphylaxis"),
                        )
                    }
                }

                // Compliance Switch Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) {
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.IS_NON_COMPLIANT, (!draft.values.isNonCompliant).toString()))
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (draft.values.isNonCompliant) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.patient_non_compliant),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (draft.values.isNonCompliant) FontWeight.Bold else FontWeight.Normal,
                            color = if (draft.values.isNonCompliant) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = draft.values.isNonCompliant,
                            onCheckedChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.IS_NON_COMPLIANT, it.toString())) },
                            enabled = enabled,
                            modifier = Modifier.testTag("medNonCompliant"),
                        )
                    }
                }
            }
        }

        // CARD 3: ADVERSE EFFECTS & CLINICAL NOTES
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
                    text = "3. الآثار الجانبية والملاحظات السريرية · Adverse Effects & Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Adverse effects & reported side notes
                EditorField(
                    value = draft.values.adverseNotes,
                    onValueChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ADVERSE_NOTES, it)) },
                    label = stringResource(R.string.adverse_effects),
                    tag = "medAdverseNotes",
                    enabled = enabled,
                    singleLine = false,
                )

                // General notes
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
                text = "سجلات الأدوية والوصفات مشفرة محلياً (AES-256 GCM) · Local Vault Security",
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
