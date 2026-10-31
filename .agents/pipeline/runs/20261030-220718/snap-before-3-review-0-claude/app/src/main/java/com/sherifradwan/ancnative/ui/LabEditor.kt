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
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.model.codex.LabCodexItem
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.LabDraftField
import com.sherifradwan.ancnative.state.SavePhase

@Composable
fun LabEditor(
    state: AncState,
    route: AncRoute.LabEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.labDraft?.takeIf { it.patientId == route.patientId && it.labId == route.labId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR
    val testNameFocusRequester = remember { FocusRequester() }
    val resultFocusRequester = remember { FocusRequester() }

    EditorPage(
        title = stringResource(R.string.lab_editor),
        subtitle = "${patient.displayName} · Laboratory Investigations & Diagnostic Results",
        tag = "labEditor",
    ) {
        ClinicalEditorErrorCard(state, draft.errors)

        val isChainedHeader = route.isNew && (draft.values.date.isNotBlank() || !draft.values.labName.isNullOrBlank())

        // CARD 1: LABORATORY & DATE
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "1. Laboratory Details & Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isChainedHeader) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("pinnedLabHeaderTag"),
                        ) {
                            Text(
                                text = "📌 Chained Batch",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // Laboratory name field with presets
                EditorField(
                    value = draft.values.labName,
                    onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.LAB_NAME, it)) },
                    label = stringResource(R.string.lab_name),
                    tag = "labNameField",
                    enabled = enabled,
                    singleLine = true,
                    keyboardType = KeyboardType.Text,
                )

                QuickPresetChips(
                    title = stringResource(R.string.lab_name),
                    items = listOf("Al Borg Labs", "El Mokhtabar", "Hospital Lab", "Alfa Labs"),
                    enabled = enabled,
                    onSelect = { onEvent(AncEvent.LabDraftChanged(LabDraftField.LAB_NAME, it)) },
                    tagPrefix = "labNamePreset",
                )

                // Date selection
                ClinicalDateField(
                    value = draft.values.date,
                    onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.DATE, it)) },
                    label = stringResource(R.string.test_date),
                    tag = "labDate",
                    enabled = enabled,
                    error = draft.errors.contains(ClinicalValidationError.REQUIRED_DATE) ||
                        draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                    showTodayButton = true,
                )

                // Auto-calculated Gestational Age for the lab test date
                val calculatedGa = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.values.date)
                if (calculatedGa != null && calculatedGa.isPostConception) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("labCalculatedGaCard"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
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

        // Quick ANC Lab Panels Presets from Codex Delegate
        val context = LocalContext.current
        val codexRepo = remember { ClinicalCodexRepository.getInstance(context) }
        val codexLabItems by codexRepo.labCodexItems.collectAsState()
        val codexGroups by codexRepo.labCategoryGroups.collectAsState()
        var selectedGroupName by remember { mutableStateOf<String?>(null) }

        // CARD 2: LAB PANELS & TEST SELECTION
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
                    text = "2. Investigation Panels & Test Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Category Groups / Clinical Panels Selection
                if (codexGroups.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            text = "📦 Diagnostic Panels:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            androidx.compose.material3.FilterChip(
                                selected = selectedGroupName == null,
                                onClick = { selectedGroupName = null },
                                label = { Text("All Tests (${codexLabItems.size})") },
                                modifier = Modifier.testTag("allLabTestsChip"),
                            )
                            codexGroups.forEach { grp ->
                                androidx.compose.material3.FilterChip(
                                    selected = selectedGroupName == grp.name,
                                    onClick = {
                                        selectedGroupName = if (selectedGroupName == grp.name) null else grp.name
                                    },
                                    label = { Text("${grp.name} (${grp.testNames.size})") },
                                    modifier = Modifier.testTag("labGroupChip-${grp.id}"),
                                )
                            }
                        }
                    }
                }

                val displayedItems = remember(codexLabItems, codexGroups, selectedGroupName) {
                    val grp = codexGroups.firstOrNull { it.name == selectedGroupName }
                    if (grp != null) {
                        codexLabItems.filter { it.testName in grp.testNames }
                    } else {
                        codexLabItems
                    }
                }

                QuickPresetChips(
                    title = if (selectedGroupName != null) "Panel Tests: $selectedGroupName" else stringResource(R.string.common_tests),
                    items = displayedItems.map { it.testName },
                    enabled = enabled,
                    onSelect = { selectedName ->
                        val item = codexLabItems.firstOrNull { it.testName == selectedName }
                        if (item != null) {
                            onEvent(AncEvent.LabDraftChanged(LabDraftField.CATEGORY, item.category))
                            onEvent(AncEvent.LabDraftChanged(LabDraftField.NAME, item.testName))
                            if (item.defaultResult.isNotEmpty()) onEvent(AncEvent.LabDraftChanged(LabDraftField.RESULT, item.defaultResult))
                            if (item.unit.isNotEmpty()) onEvent(AncEvent.LabDraftChanged(LabDraftField.UNIT, item.unit))
                            if (item.referenceRange.isNotEmpty()) onEvent(AncEvent.LabDraftChanged(LabDraftField.REFERENCE_RANGE, item.referenceRange))
                        }
                    },
                    tagPrefix = "labPreset",
                )

                // Save current test to Lab Codex if it's new and not already in Codex
                val trimmedTestName = draft.values.testName.trim()
                val isAlreadyInCodex = codexLabItems.any { it.testName.equals(trimmedTestName, ignoreCase = true) }
                if (trimmedTestName.isNotBlank() && !isAlreadyInCodex) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("saveToLabCodexBanner"),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "New Test: Save to Clinic Lab Codex?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            TextButton(
                                onClick = {
                                    codexRepo.addLabCodexItem(
                                        LabCodexItem(
                                            testName = trimmedTestName,
                                            category = draft.values.testCategory.ifBlank { "Hematology" },
                                            unit = draft.values.unit.trim(),
                                            referenceRange = draft.values.referenceRange.trim(),
                                            defaultResult = draft.values.result.trim(),
                                            isCustom = true,
                                        ),
                                    )
                                },
                                modifier = Modifier.testTag("saveToLabCodexButton"),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save to Codex")
                            }
                        }
                    }
                }

                ChoiceGroup(
                    label = stringResource(R.string.test_category),
                    selected = draft.values.testCategory,
                    choices = ClinicalVocabulary.testCategories.toList(),
                    enabled = enabled,
                    tagPrefix = "labCategoryChoice",
                    onSelected = { onEvent(AncEvent.LabDraftChanged(LabDraftField.CATEGORY, it)) },
                )
            }
        }

        // CARD 3: TEST RESULT, REFERENCE RANGE & CLINICAL NOTES
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
                    text = "3. Result, Units & Reference Range",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Paired Test Name & Result with Focus Traversal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.testName,
                            onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.NAME, it)) },
                            label = stringResource(R.string.test_name),
                            tag = "labName",
                            enabled = enabled,
                            modifier = Modifier.focusRequester(testNameFocusRequester),
                            error = draft.errors.contains(ClinicalValidationError.REQUIRED_NAME),
                            singleLine = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            keyboardActions = KeyboardActions(onNext = { resultFocusRequester.requestFocus() }),
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.result,
                            onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.RESULT, it)) },
                            label = stringResource(R.string.test_result),
                            tag = "labResult",
                            enabled = enabled,
                            modifier = Modifier.focusRequester(resultFocusRequester),
                            error = draft.errors.contains(ClinicalValidationError.REQUIRED_RESULT),
                            singleLine = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        )
                    }
                }

                QuickPresetChips(
                    title = stringResource(R.string.test_result),
                    items = listOf("Negative", "Non-reactive", "Normal", "Positive"),
                    enabled = enabled,
                    onSelect = { onEvent(AncEvent.LabDraftChanged(LabDraftField.RESULT, it)) },
                    tagPrefix = "labResultPreset",
                )

                // Paired Unit & Reference Range
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.unit,
                            onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.UNIT, it)) },
                            label = stringResource(R.string.unit),
                            tag = "labUnit",
                            enabled = enabled,
                            singleLine = true,
                            keyboardType = KeyboardType.Text,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.referenceRange,
                            onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.REFERENCE_RANGE, it)) },
                            label = stringResource(R.string.reference_range),
                            tag = "labRefRange",
                            enabled = enabled,
                            singleLine = true,
                            keyboardType = KeyboardType.Text,
                        )
                    }
                }

                // Abnormal Result Switch
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { onEvent(AncEvent.LabAbnormalChanged(!draft.values.isAbnormal)) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (draft.values.isAbnormal) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                            text = stringResource(R.string.is_abnormal),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (draft.values.isAbnormal) FontWeight.Bold else FontWeight.Normal,
                            color = if (draft.values.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = draft.values.isAbnormal,
                            onCheckedChange = { onEvent(AncEvent.LabAbnormalChanged(it)) },
                            enabled = enabled,
                            modifier = Modifier.testTag("labAbnormal"),
                        )
                    }
                }

                EditorField(
                    value = draft.values.notes,
                    onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.NOTES, it)) },
                    label = stringResource(R.string.notes),
                    tag = "labNotes",
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
                text = "Lab records and reports encrypted locally (AES-256 GCM) · Local Vault Security",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Actions: Cancel, Save & Add Another Test, Save
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onEvent(AncEvent.CancelEditor) },
                enabled = enabled,
                modifier = Modifier.minimumTouchTarget().testTag("cancelLab"),
            ) {
                Text(stringResource(R.string.cancel))
            }
            OutlinedButton(
                onClick = { onEvent(AncEvent.SubmitLabAndAddAnother) },
                enabled = enabled,
                modifier = Modifier.minimumTouchTarget().testTag("addAnotherLab"),
            ) {
                Text("+ " + stringResource(R.string.add_another_test))
            }
            Button(
                onClick = { onEvent(AncEvent.SubmitLab) },
                enabled = enabled,
                modifier = Modifier.minimumTouchTarget().testTag("saveLab"),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
