package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MedicationOnOff
import com.sherifradwan.ancnative.model.medicationEditorChoice
import com.sherifradwan.ancnative.model.medicationStatusAfterChoice
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.model.codex.MedicationCodexItem
import com.sherifradwan.ancnative.model.effectiveMedicationStatus
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.MedicationDraftField
import com.sherifradwan.ancnative.state.SavePhase
import java.time.LocalDate

private fun firstBlankRow(dosage: String, route: String): String? {
    if (dosage.isBlank()) return "dose"
    if (route.isBlank()) return "route"
    return null
}

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
    // Row key whose field should take focus once that row has been composed (IME Next only).
    var pendingFocusKey by remember(draft.medicationId) { mutableStateOf<String?>(null) }

    var openRowKey by rememberSaveable(draft.medicationId) {
        mutableStateOf(
            firstBlankRow(draft.values.dosage, draft.values.route),
        )
    }

    LaunchedEffect(openRowKey, pendingFocusKey) {
        val target = pendingFocusKey
        if (target == "dose" && target == openRowKey) {
            try {
                dosageFocusRequester.requestFocus()
            } catch (_: IllegalStateException) {
                // Field not attached (row closed or not composed yet): skip programmatic focus.
            }
        }
        pendingFocusKey = null
    }

    EditorPage(
        title = stringResource(R.string.medication_editor),
        subtitle = "${patient.displayName} · Pregnancy Medication Management & Prescription",
        tag = "medicationEditor",
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { onEvent(AncEvent.CancelEditor) },
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).minimumTouchTarget().testTag("cancelMedication"),
                ) {
                    Text(stringResource(R.string.cancel), maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = { onEvent(AncEvent.SubmitMedication) },
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).minimumTouchTarget().testTag("saveMedication"),
                ) {
                    Text(stringResource(R.string.save), maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = { onEvent(AncEvent.SubmitMedicationAndAddAnother) },
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1.6f).minimumTouchTarget().testTag("addAnotherMedication"),
                ) {
                    Text(stringResource(R.string.save_and_add_another_med), maxLines = 2, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state)

        // 1-Tap Presets from Medication Codex Delegate
        val context = LocalContext.current
        val codexRepo = remember { ClinicalCodexRepository.getInstance(context) }
        val codexMedItems by codexRepo.medicationCodexItems.collectAsState()

        // CARD 1: MEDICATION (M401, tag medPickCard)
        SectionCard(
            title = stringResource(R.string.medication),
            tag = "medPickCard",
        ) {
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
                keyboardActions = KeyboardActions(onNext = {
                    openRowKey = firstBlankRow(draft.values.dosage, draft.values.route)
                    pendingFocusKey = openRowKey
                }),
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
                        // The start date is not asked for; a new entry starts today.
                        if (draft.values.startDate.isEmpty()) {
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.START_DATE, LocalDate.now().toString()))
                        }
                        openRowKey = firstBlankRow(preset.dosage, preset.route)
                    }
                },
                tagPrefix = "medPreset",
                collapseAfter = PRESAVED_COLLAPSE_AFTER,
                toggleTag = "presavedToggle",
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
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                codexRepo.addMedicationCodexItem(
                                    MedicationCodexItem(
                                        name = trimmedMedName,
                                        dosage = draft.values.dosage.trim(),
                                        route = draft.values.route.ifBlank { "Oral" },
                                        frequency = draft.values.frequency.trim(),
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
                            Text("Add to my list", maxLines = 1, softWrap = false)
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
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ROUTE, prior.route))
                            // Safety Reset Rule: sets status "Active", wipes adverse notes, end date, and notes
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, MEDICATION_STATUS_ACTIVE))
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ADVERSE_NOTES, ""))
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.END_DATE, ""))
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.NOTES, ""))
                            openRowKey = firstBlankRow(prior.dosage, prior.route)
                        }
                    },
                    tagPrefix = "patientPrevMed",
                    collapseAfter = PRESAVED_COLLAPSE_AFTER,
                )
            }
        }

        // CARD 2: DOSE AND ROUTE (M402, tag medDetailsCard)
        SectionCard(
            title = stringResource(R.string.medication_dose_and_schedule),
            tag = "medDetailsCard",
        ) {
            // Row 1: dose
            if (openRowKey == "dose") {
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("medOpen-dose"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                        keyboardActions = KeyboardActions(onNext = {
                            openRowKey = firstBlankRow(draft.values.dosage, draft.values.route)
                            pendingFocusKey = openRowKey
                        }),
                    )
                }
            } else {
                SummaryRow(
                    label = stringResource(R.string.dosage),
                    value = draft.values.dosage,
                    tag = "medRow-dose",
                    onClick = { openRowKey = "dose" },
                )
            }

            // Row 2: route
            if (openRowKey == "route") {
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("medOpen-route"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChoiceGroup(
                        label = stringResource(R.string.route),
                        selected = draft.values.route.ifEmpty { "Oral" },
                        choices = ClinicalVocabulary.medicationRoutes.toList(),
                        enabled = enabled,
                        tagPrefix = "medRouteChoice",
                        rowGap = 0.dp,
                        onSelected = { selectedRoute ->
                            onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ROUTE, selectedRoute))
                            openRowKey = firstBlankRow(draft.values.dosage, selectedRoute)
                        },
                    )
                }
            } else {
                SummaryRow(
                    label = stringResource(R.string.route),
                    value = draft.values.route.ifBlank { "Oral" },
                    tag = "medRow-route",
                    onClick = { openRowKey = "route" },
                )
            }
        }

        // CARD 3: STATUS AND NOTE (M403, tag medStatusCard). The editor offers On (stored Active) and Stopped (stored Discontinued).
        SectionCard(
            title = stringResource(R.string.status),
            tag = "medStatusCard",
        ) {
            val currentStatus = effectiveMedicationStatus(
                draft.values.status.ifEmpty { MEDICATION_STATUS_ACTIVE },
                draft.values.isNonCompliant,
            )
            val currentChoice = medicationEditorChoice(currentStatus)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    MedicationOnOff.ON to stringResource(R.string.medication_status_active),
                    MedicationOnOff.STOPPED to stringResource(R.string.medication_status_stopped),
                ).forEachIndexed { index, (choice, label) ->
                    BigChoiceButton(
                        label = label,
                        selected = currentChoice == choice,
                        tag = "medStatusChoice-$index",
                        onClick = {
                            val next = medicationStatusAfterChoice(currentStatus, choice)
                            if (next != currentStatus) {
                                onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, next))
                            }
                        },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            EditorField(
                value = draft.values.notes,
                onValueChange = { onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.NOTES, it)) },
                label = stringResource(R.string.medication_note),
                tag = "medNotes",
                enabled = enabled,
                singleLine = false,
            )
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
    }
}
