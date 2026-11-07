package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.sherifradwan.ancnative.state.LabDraftValues
import com.sherifradwan.ancnative.state.SavePhase

private val DipstickChoices = listOf("Normal", "Trace", "+", "++", "+++")
private val DipstickNameHints = listOf("protein", "glucose", "ketone")

private fun loadLabItemIntoDraft(item: LabCodexItem, onEvent: (AncEvent) -> Unit) {
    onEvent(AncEvent.LabDraftChanged(LabDraftField.CATEGORY, item.category))
    onEvent(AncEvent.LabDraftChanged(LabDraftField.NAME, item.testName))
    onEvent(AncEvent.LabDraftChanged(LabDraftField.RESULT, item.defaultResult))
    onEvent(AncEvent.LabDraftChanged(LabDraftField.UNIT, item.unit))
    onEvent(AncEvent.LabDraftChanged(LabDraftField.REFERENCE_RANGE, item.referenceRange))
    val flag = labResultFlag(item.defaultResult, item.referenceRange)
    onEvent(AncEvent.LabAbnormalChanged(flag != LabResultFlag.UNKNOWN && flag != LabResultFlag.NORMAL))
}

private fun isDipstickTest(values: LabDraftValues): Boolean {
    if (values.testCategory.trim().equals("Urinalysis", ignoreCase = true)) return true
    val name = values.testName.lowercase()
    return DipstickNameHints.any { name.contains(it) }
}

@Composable
private fun LabResultFlagBadge(flag: LabResultFlag, modifier: Modifier = Modifier) {
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
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
    ) {
        Text(
            text = when (flag) {
                LabResultFlag.NORMAL -> "Normal ✓"
                LabResultFlag.HIGH -> "High ▲"
                LabResultFlag.LOW -> "Low ▼"
                LabResultFlag.UNKNOWN -> ""
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun BatchInfoBar(
    dateText: String,
    gaText: String?,
    labName: String,
    enabled: Boolean,
    onChange: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onChange)
            .testTag("labBatchChange"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = dateText.ifBlank { "Set date" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (gaText != null) {
                    Text(
                        text = "GA $gaText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = labName.ifBlank { "Lab not set" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Change ▾",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
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

    // Panel Queue State (Q254)
    var panelRemainingIds by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(emptyList<String>()) }
    var panelTotalCount by rememberSaveable { mutableStateOf(0) }
    var currentPanelIndex by rememberSaveable { mutableStateOf(0) }
    var currentPanelItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentLabId by rememberSaveable { mutableStateOf(draft.labId) }
    var selectedGroupName by rememberSaveable { mutableStateOf<String?>(null) }

    // Test list and groups
    val context = LocalContext.current
    val codexRepo = remember { ClinicalCodexRepository.getInstance(context) }
    val codexLabItems by codexRepo.labCodexItems.collectAsState()
    val codexGroups by codexRepo.labCategoryGroups.collectAsState()

    // Batch info dialog opens at once while the date is still missing
    var batchInfoOpen by rememberSaveable { mutableStateOf(draft.values.date.isBlank()) }
    var testSelectorOpen by rememberSaveable { mutableStateOf(draft.values.testName.isBlank()) }

    val resultFlag = labResultFlag(draft.values.result, draft.values.referenceRange)

    var previousResult by remember(draft.labId) { mutableStateOf(draft.values.result) }
    var previousRefRange by remember(draft.labId) { mutableStateOf(draft.values.referenceRange) }

    fun clearPanelQueue() {
        panelRemainingIds = emptyList()
        panelTotalCount = 0
        currentPanelIndex = 0
        currentPanelItemId = null
    }

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
            testSelectorOpen = draft.values.testName.isBlank()
            if (panelRemainingIds.isNotEmpty()) {
                val step = nextLabPanelTest(panelRemainingIds, codexLabItems)
                if (step.item != null) {
                    panelRemainingIds = step.remainingIds
                    currentPanelIndex += 1
                    currentPanelItemId = step.item.id
                    loadLabItemIntoDraft(step.item, onEvent)
                    testSelectorOpen = false
                } else {
                    clearPanelQueue()
                }
            } else {
                clearPanelQueue()
            }
        }
    }

    val calculatedGa = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.values.date)
        ?.takeIf { it.isPostConception }

    if (batchInfoOpen) {
        AlertDialog(
            onDismissRequest = { batchInfoOpen = false },
            title = { Text(stringResource(R.string.lab_name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    if (calculatedGa != null) {
                        Text(
                            text = stringResource(R.string.calculated_ga_at_visit, calculatedGa.displayText),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("labCalculatedGaCard"),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { batchInfoOpen = false },
                    modifier = Modifier.minimumTouchTarget().testTag("labBatchDone"),
                ) {
                    Text("Done")
                }
            },
        )
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

        // Batch info bar replaces the old numbered details card
        BatchInfoBar(
            dateText = draft.values.date,
            gaText = calculatedGa?.displayText,
            labName = draft.values.labName,
            enabled = enabled,
            onChange = { batchInfoOpen = true },
        )

        if (panelRemainingIds.isNotEmpty()) {
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

        val isDipstick = isDipstickTest(draft.values)

        // Clinical story card: one investigation
        SectionCard(
            title = "",
            tag = "labTestCard",
        ) {
            val categoryText = draft.values.testCategory.trim().uppercase()
            val progressText = when {
                panelTotalCount > 1 && categoryText.isNotBlank() ->
                    stringResource(R.string.lab_test_progress_category, currentPanelIndex, panelTotalCount, categoryText)
                panelTotalCount > 1 -> stringResource(R.string.lab_test_progress, currentPanelIndex, panelTotalCount)
                else -> categoryText
            }
            if (progressText.isNotBlank()) {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("labTestProgress"),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = draft.values.testName.ifBlank { stringResource(R.string.new_test) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).testTag("labTestTitle"),
                )
                TextButton(
                    onClick = { testSelectorOpen = !testSelectorOpen },
                    modifier = Modifier.minimumTouchTarget().testTag("labEditDetailsToggle"),
                ) {
                    Text(
                        if (testSelectorOpen) stringResource(R.string.more_close) else stringResource(R.string.edit_test_details),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            val unitText = draft.values.unit.trim()
            val refText = draft.values.referenceRange.trim()
            val subtitle = when {
                unitText.isNotEmpty() && refText.isNotEmpty() -> "$unitText · ref $refText (from report)"
                unitText.isNotEmpty() -> unitText
                refText.isNotEmpty() -> "ref $refText (from report)"
                isDipstick -> "Dipstick"
                else -> ""
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("labTestSubtitle"),
                )
            }

            if (isDipstick) {
                // Dipstick: big qualitative choices only
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DipstickChoices.forEachIndexed { index, choice ->
                        BigChoiceButton(
                            label = choice,
                            selected = draft.values.result.trim() == choice,
                            tag = "labResultPreset-$index",
                            enabled = enabled,
                            onClick = {
                                onEvent(AncEvent.LabDraftChanged(LabDraftField.RESULT, choice))
                                onEvent(AncEvent.LabAbnormalChanged(choice != "Normal"))
                            },
                        )
                    }
                }
            } else {
                // Story display: value, unit and diagnostic badge
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("labStoryDisplay"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = draft.values.result.trim().ifBlank { "—" },
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("labStoryValue"),
                            )
                            if (unitText.isNotEmpty()) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = unitText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )
                            }
                        }
                        LabResultFlagBadge(flag = resultFlag, modifier = Modifier.testTag("labResultFlag"))
                    }
                }

                EditorField(
                    value = draft.values.result,
                    onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.RESULT, it)) },
                    label = stringResource(R.string.test_result),
                    tag = "labResult",
                    enabled = enabled,
                    error = draft.errors.contains(ClinicalValidationError.REQUIRED_RESULT),
                    singleLine = true,
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
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
                            imeAction = ImeAction.Next,
                        )
                    }
                    Box(modifier = Modifier.weight(1.5f)) {
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

            // Abnormal switch, auto-flagged from the reference range
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(enabled = enabled) { onEvent(AncEvent.LabAbnormalChanged(!draft.values.isAbnormal)) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("labFlagToggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.is_abnormal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (draft.values.isAbnormal) FontWeight.Bold else FontWeight.Normal,
                        color = if (draft.values.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isDipstick) "Trace or higher marked abnormal" else "Auto-flagged from reference range",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = draft.values.isAbnormal,
                    onCheckedChange = { onEvent(AncEvent.LabAbnormalChanged(it)) },
                    enabled = enabled,
                    modifier = Modifier.testTag("labAbnormal"),
                )
            }

            Box(modifier = Modifier.fillMaxWidth().testTag("labNoteToggle")) {
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

        // Collapsible test selector: change test, panels, quick picks, category
        SectionCard(
            title = "",
            tag = "labAddTestsCard",
        ) {
            SummaryRow(
                label = stringResource(R.string.add_tests),
                value = if (testSelectorOpen) stringResource(R.string.more_close) else stringResource(R.string.more_open),
                tag = "labAddTestsToggle",
                onClick = { testSelectorOpen = !testSelectorOpen },
            )

            if (testSelectorOpen) {
                EditorField(
                    value = draft.values.testName,
                    onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.NAME, it)) },
                    label = stringResource(R.string.test_name),
                    tag = "labName",
                    enabled = enabled,
                    error = draft.errors.contains(ClinicalValidationError.REQUIRED_NAME),
                    singleLine = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                )

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
                                    clearPanelQueue()
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
                                            clearPanelQueue()
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
                                            } else {
                                                clearPanelQueue()
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
                        clearPanelQueue()
                        val item = codexLabItems.firstOrNull { it.testName.equals(selectedName, ignoreCase = true) }
                        if (item != null) {
                            loadLabItemIntoDraft(item, onEvent)
                        }
                    },
                    tagPrefix = "labPreset",
                )

                // Offer to keep a new test in the user's test list
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
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
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
