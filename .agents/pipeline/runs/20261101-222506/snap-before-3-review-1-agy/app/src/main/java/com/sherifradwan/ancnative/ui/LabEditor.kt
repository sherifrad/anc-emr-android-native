package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.sherifradwan.ancnative.model.LabResultFlag
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.model.codex.LabCodexItem
import com.sherifradwan.ancnative.model.codex.nextLabPanelTest
import com.sherifradwan.ancnative.model.codex.resolveLabPanel
import com.sherifradwan.ancnative.model.labResultFlag
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.LabDraftField
import com.sherifradwan.ancnative.state.SavePhase

private fun loadLabItemIntoDraft(item: LabCodexItem, onEvent: (AncEvent) -> Unit) {
    onEvent(AncEvent.LabDraftChanged(LabDraftField.CATEGORY, item.category))
    onEvent(AncEvent.LabDraftChanged(LabDraftField.NAME, item.testName))
    onEvent(AncEvent.LabDraftChanged(LabDraftField.RESULT, item.defaultResult))
    onEvent(AncEvent.LabDraftChanged(LabDraftField.UNIT, item.unit))
    onEvent(AncEvent.LabDraftChanged(LabDraftField.REFERENCE_RANGE, item.referenceRange))
    val flag = labResultFlag(item.defaultResult, item.referenceRange)
    onEvent(AncEvent.LabAbnormalChanged(flag != LabResultFlag.UNKNOWN && flag != LabResultFlag.NORMAL))
}

@Composable
private fun LabResultFlagPill(flag: LabResultFlag, modifier: Modifier = Modifier) {
    if (flag == LabResultFlag.UNKNOWN) return
    val isNormal = flag == LabResultFlag.NORMAL
    val containerColor = if (isNormal) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isNormal) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Text(
            text = when (flag) {
                LabResultFlag.NORMAL -> "Normal"
                LabResultFlag.HIGH -> "High"
                LabResultFlag.LOW -> "Low"
                LabResultFlag.UNKNOWN -> ""
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private val StringListSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it },
)

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

    // Panel Queue State (Q254)
    var panelRemainingIds by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(emptyList<String>()) }
    var panelTotalCount by rememberSaveable { mutableStateOf(0) }
    var currentPanelIndex by rememberSaveable { mutableStateOf(0) }
    var currentPanelItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentLabId by rememberSaveable { mutableStateOf(draft.labId) }
    var selectedGroupName by rememberSaveable { mutableStateOf<String?>(null) }

    // Codex repo and groups
    val context = LocalContext.current
    val codexRepo = remember { ClinicalCodexRepository.getInstance(context) }
    val codexLabItems by codexRepo.labCodexItems.collectAsState()
    val codexGroups by codexRepo.labCategoryGroups.collectAsState()

    // Open-on-entry rememberSaveable booleans
    val initialEditDetails = draft.values.testName.isBlank() ||
        (draft.values.unit.isBlank() && draft.values.referenceRange.isBlank())
    var editDetailsOpen by rememberSaveable { mutableStateOf(initialEditDetails) }
    var flagSectionOpen by rememberSaveable { mutableStateOf(draft.values.isAbnormal) }
    var noteSectionOpen by rememberSaveable { mutableStateOf(draft.values.notes.isNotBlank()) }

    val initialAddTests = draft.values.testName.isBlank() && panelTotalCount <= 1
    var addTestsOpen by rememberSaveable { mutableStateOf(initialAddTests) }

    // Result flag calculation
    val resultFlag = labResultFlag(draft.values.result, draft.values.referenceRange)

    var previousResult by remember(draft.labId) { mutableStateOf<String?>(null) }
    var previousRefRange by remember(draft.labId) { mutableStateOf<String?>(null) }

    // Automatic abnormal flag setting when result or reference range changes
    LaunchedEffect(draft.labId, draft.values.result, draft.values.referenceRange) {
        val currentResult = draft.values.result
        val currentRange = draft.values.referenceRange
        if (currentResult != previousResult || currentRange != previousRefRange) {
            previousResult = currentResult
            previousRefRange = currentRange
            val flag = labResultFlag(currentResult, currentRange)
            if (flag != LabResultFlag.UNKNOWN) {
                val shouldBeAbnormal = (flag != LabResultFlag.NORMAL)
                if (draft.values.isAbnormal != shouldBeAbnormal) {
                    onEvent(AncEvent.LabAbnormalChanged(shouldBeAbnormal))
                }
            }
        }
    }

    // Advance panel queue after a successful chained save
    LaunchedEffect(draft.labId) {
        if (draft.labId != currentLabId) {
            currentLabId = draft.labId
            if (panelRemainingIds.isNotEmpty()) {
                val step = nextLabPanelTest(panelRemainingIds, codexLabItems)
                if (step.item != null) {
                    panelRemainingIds = step.remainingIds
                    currentPanelIndex += 1
                    currentPanelItemId = step.item.id
                    loadLabItemIntoDraft(step.item, onEvent)
                    editDetailsOpen = step.item.unit.isBlank() && step.item.referenceRange.isBlank()
                } else {
                    panelRemainingIds = emptyList()
                    panelTotalCount = 0
                    currentPanelIndex = 0
                    currentPanelItemId = null
                }
            } else {
                panelTotalCount = 0
                currentPanelIndex = 0
                currentPanelItemId = null
            }
        }
    }

    EditorPage(
        title = stringResource(R.string.lab_editor),
        subtitle = "${patient.displayName} · Laboratory Investigations & Diagnostic Results",
        tag = "labEditor",
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
                    modifier = Modifier.weight(1f).minimumTouchTarget().testTag("cancelLab"),
                ) {
                    Text(stringResource(R.string.cancel), maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = { onEvent(AncEvent.SubmitLabAndAddAnother) },
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1.5f).minimumTouchTarget().testTag("addAnotherLab"),
                ) {
                    Text(
                        if (panelRemainingIds.isNotEmpty()) "Save and next test" else "+ " + stringResource(R.string.add_another_test),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Button(
                    onClick = { onEvent(AncEvent.SubmitLab) },
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).minimumTouchTarget().testTag("saveLab"),
                ) {
                    Text(stringResource(R.string.save), maxLines = 1, softWrap = false)
                }
            }
        },
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state)

        val isChainedHeader = route.isNew && (draft.values.date.isNotBlank() || !draft.values.labName.isNullOrBlank())

        // CARD 1: LABORATORY & DATE (Header Strip - L302)
        SectionCard(
            title = "1. Laboratory Details & Date",
        ) {
            if (isChainedHeader) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
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
                items = ClinicalVocabulary.labNamePresets,
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
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("labCalculatedGaCard"),
                    color = MaterialTheme.colorScheme.primaryContainer,
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

        // CARD 2: TEST CARD (L302, L303, L304, L305, L306)
        SectionCard(
            title = "",
            tag = "labTestCard",
        ) {
            // L303. Progress line
            val progressText = when {
                panelTotalCount > 1 -> {
                    if (draft.values.testCategory.isNotBlank()) {
                        "Test $currentPanelIndex of $panelTotalCount · ${draft.values.testCategory}"
                    } else {
                        "Test $currentPanelIndex of $panelTotalCount"
                    }
                }
                draft.values.testCategory.isNotBlank() -> draft.values.testCategory
                else -> ""
            }
            if (progressText.isNotBlank()) {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("labTestProgress"),
                )
            }

            // L303. Test name as large title
            val testTitle = draft.values.testName.ifBlank { stringResource(R.string.new_test) }
            Text(
                text = testTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("labTestTitle"),
            )

            // L303. Light unit/reference-range summary line
            val unitText = draft.values.unit.trim()
            val refText = draft.values.referenceRange.trim()
            val unitAndRangeSummary = when {
                unitText.isNotEmpty() && refText.isNotEmpty() -> "$unitText · ref $refText"
                unitText.isNotEmpty() -> unitText
                refText.isNotEmpty() -> "ref $refText"
                else -> ""
            }
            if (unitAndRangeSummary.isNotEmpty()) {
                Text(
                    text = unitAndRangeSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // L304. Editing details toggle button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                TextButton(
                    onClick = { editDetailsOpen = !editDetailsOpen },
                    modifier = Modifier.minimumTouchTarget().testTag("labEditDetailsToggle"),
                ) {
                    Text(stringResource(R.string.edit_test_details))
                }
            }

            // L304. Details group
            if (editDetailsOpen) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
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
                }
            }

            // L305. Result field (full width) + flag pill directly under it
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

            if (resultFlag != LabResultFlag.UNKNOWN) {
                LabResultFlagPill(flag = resultFlag)
            }

            // L305. Big preset choice buttons (one per row)
            val resultPresets = listOf("Negative", "Non-reactive", "Normal", "Positive")
            resultPresets.forEachIndexed { index, preset ->
                BigChoiceButton(
                    label = preset,
                    selected = draft.values.result == preset,
                    tag = "labResultPreset-$index",
                    enabled = enabled,
                    onClick = { onEvent(AncEvent.LabDraftChanged(LabDraftField.RESULT, preset)) },
                )
            }

            // L306. Two toggle chips in one row: labFlagToggle & labNoteToggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = flagSectionOpen,
                    onClick = { flagSectionOpen = !flagSectionOpen },
                    label = {
                        Text(if (draft.values.isAbnormal) stringResource(R.string.flag_abnormal) else stringResource(R.string.flag))
                    },
                    modifier = Modifier.minimumTouchTarget().testTag("labFlagToggle"),
                )
                FilterChip(
                    selected = noteSectionOpen,
                    onClick = { noteSectionOpen = !noteSectionOpen },
                    label = {
                        val noteLabel = if (draft.values.notes.isNotBlank()) "${stringResource(R.string.note)} ✓" else stringResource(R.string.note)
                        Text(noteLabel)
                    },
                    modifier = Modifier.minimumTouchTarget().testTag("labNoteToggle"),
                )
            }

            // L306. Flag section
            if (flagSectionOpen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(enabled = enabled) { onEvent(AncEvent.LabAbnormalChanged(!draft.values.isAbnormal)) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.is_abnormal),
                        style = MaterialTheme.typography.bodyMedium,
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

            // L306. Note section
            if (noteSectionOpen) {
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

        // CARD 3: ADD TESTS CARD (L302, L307)
        SectionCard(
            title = "",
            tag = "labAddTestsCard",
        ) {
            SummaryRow(
                label = stringResource(R.string.add_tests),
                value = if (addTestsOpen) stringResource(R.string.more_close) else stringResource(R.string.more_open),
                tag = "labAddTestsToggle",
                onClick = { addTestsOpen = !addTestsOpen },
            )

            if (addTestsOpen) {
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
                            val labItemCount = codexLabItems.size
                            FilterChip(
                                selected = selectedGroupName == null,
                                onClick = {
                                    selectedGroupName = null
                                    panelRemainingIds = emptyList()
                                    panelTotalCount = 0
                                    currentPanelIndex = 0
                                    currentPanelItemId = null
                                },
                                label = { Text("All Tests ($labItemCount)") },
                                modifier = Modifier.minimumTouchTarget().testTag("allLabTestsChip"),
                            )
                            codexGroups.forEach { grp ->
                                FilterChip(
                                    selected = selectedGroupName == grp.name,
                                    onClick = {
                                        if (selectedGroupName == grp.name) {
                                            selectedGroupName = null
                                            panelRemainingIds = emptyList()
                                            panelTotalCount = 0
                                            currentPanelIndex = 0
                                            currentPanelItemId = null
                                        } else {
                                            selectedGroupName = grp.name
                                            val resolution = resolveLabPanel(grp, codexLabItems)
                                            val step = nextLabPanelTest(resolution.tests.map { it.id }, codexLabItems)
                                            if (step.item != null) {
                                                panelTotalCount = resolution.tests.size
                                                currentPanelIndex = 1
                                                currentPanelItemId = step.item.id
                                                panelRemainingIds = step.remainingIds
                                                currentLabId = draft.labId
                                                loadLabItemIntoDraft(step.item, onEvent)
                                                editDetailsOpen = step.item.unit.isBlank() && step.item.referenceRange.isBlank()
                                            } else {
                                                panelTotalCount = 0
                                                currentPanelIndex = 0
                                                currentPanelItemId = null
                                                panelRemainingIds = emptyList()
                                            }
                                        }
                                    },
                                    label = { Text("${grp.name} (${grp.testNames.size})") },
                                    modifier = Modifier.minimumTouchTarget().testTag("labGroupChip-${grp.id}"),
                                )
                            }
                        }
                    }
                }

                val displayedItems = remember(codexLabItems, codexGroups, selectedGroupName) {
                    val grp = codexGroups.firstOrNull { it.name == selectedGroupName }
                    if (grp != null) {
                        resolveLabPanel(grp, codexLabItems).tests
                    } else {
                        codexLabItems
                    }
                }

                QuickPresetChips(
                    title = if (selectedGroupName != null) "Panel Tests: $selectedGroupName" else stringResource(R.string.common_tests),
                    items = displayedItems.map { it.testName },
                    enabled = enabled,
                    onSelect = { selectedName ->
                        panelRemainingIds = emptyList()
                        panelTotalCount = 0
                        currentPanelIndex = 0
                        currentPanelItemId = null
                        val item = codexLabItems.firstOrNull { it.testName.equals(selectedName, ignoreCase = true) }
                        if (item != null) {
                            loadLabItemIntoDraft(item, onEvent)
                            editDetailsOpen = item.unit.isBlank() && item.referenceRange.isBlank()
                        }
                    },
                    tagPrefix = "labPreset",
                )

                // Save current test to list if it's new and not already present
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
                                text = "New test: add it to your lab test list?",
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
                                modifier = Modifier.minimumTouchTarget().testTag("saveToLabCodexButton"),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add to list")
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
    }
}
