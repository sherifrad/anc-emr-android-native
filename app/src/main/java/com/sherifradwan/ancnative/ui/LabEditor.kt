package com.sherifradwan.ancnative.ui

import java.time.LocalDate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.LabResultFlag
import com.sherifradwan.ancnative.model.catalog.ClinicalCatalogRepository
import com.sherifradwan.ancnative.model.catalog.LabCatalogItem
import com.sherifradwan.ancnative.model.catalog.nextLabPanelTest
import com.sherifradwan.ancnative.model.catalog.resolveLabPanel
import com.sherifradwan.ancnative.model.labResultFlag
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.LabDraftField
import com.sherifradwan.ancnative.state.LabDraftValues
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.ui.components.BandChip
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.FormFieldLabel
import com.sherifradwan.ancnative.ui.components.LocalFieldFamily
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordSpacing
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.SectionTextAction
import com.sherifradwan.ancnative.ui.components.StatusTone
import com.sherifradwan.ancnative.ui.patienthub.components.SheetCancelSaveRow

private val DipstickChoices = listOf("Normal", "Trace", "+", "++", "+++")
private val DipstickNameHints = listOf("protein", "glucose", "ketone")

private fun loadLabItemIntoDraft(item: LabCatalogItem, onEvent: (AncEvent) -> Unit) {
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

private val StringListSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabEditor(
    state: AncState,
    route: AncRoute.LabEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.labDraft?.takeIf { it.patientId == route.patientId && it.labId == route.labId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR
    val bgFamily = FieldFamilies.background()
    val narrowStacked = LocalDensity.current.fontScale > 1.3f

    // Panel Queue State (Q254)
    var panelRemainingIds by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(emptyList<String>()) }
    var panelTotalCount by rememberSaveable { mutableStateOf(0) }
    var currentPanelIndex by rememberSaveable { mutableStateOf(0) }
    var currentPanelItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentLabId by rememberSaveable { mutableStateOf(draft.labId) }
    var selectedGroupName by rememberSaveable { mutableStateOf<String?>(null) }

    // Test list and groups
    val context = LocalContext.current
    val catalogRepo = remember { ClinicalCatalogRepository.getInstance(context) }
    val catalogLabItems by catalogRepo.labCatalogItems.collectAsState()
    val catalogGroups by catalogRepo.labCategoryGroups.collectAsState()

    val displayedItems = remember(catalogLabItems, catalogGroups, selectedGroupName) {
        val grp = catalogGroups.firstOrNull { it.name == selectedGroupName }
        if (grp != null) {
            resolveLabPanel(grp, catalogLabItems).tests
        } else {
            catalogLabItems
        }
    }

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
                val step = nextLabPanelTest(panelRemainingIds, catalogLabItems)
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
        LabMetadataDialog(
            title = stringResource(R.string.lab_name),
            dateValue = draft.values.date,
            onDateChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.DATE, it)) },
            dateTag = "labDate",
            labNameValue = draft.values.labName,
            onLabNameChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.LAB_NAME, it)) },
            labNameTag = "labNameField",
            labPresetTagPrefix = "labNamePreset",
            onDismiss = { batchInfoOpen = false },
            enabled = enabled,
            dateError = draft.errors.contains(ClinicalValidationError.REQUIRED_DATE) ||
                draft.errors.contains(ClinicalValidationError.INVALID_DATE),
            dateFirst = false,
            calculatedGaShortText = calculatedGa?.shortWeeksText,
            doneTag = "labBatchDone",
        )
    }

    EditorPage(
        title = stringResource(R.string.lab_editor),
        subtitle = "",
        tag = "labEditor",
        useRecordStyle = true,
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SheetCancelSaveRow(
                    enabled = enabled,
                    saving = state.savePhase == SavePhase.PENDING,
                    saveEnabled = enabled,
                    savingLabel = stringResource(
                        if (state.savePhase == SavePhase.PENDING) R.string.saving_label else R.string.save,
                    ),
                    onCancel = { onEvent(AncEvent.CancelEditor) },
                    onSave = { onEvent(AncEvent.SubmitLab) },
                    cancelTag = "cancelLab",
                    saveTag = "saveLab",
                    spinnerTag = "labSaveSpinner",
                )
                OutlinedButton(
                    onClick = { onEvent(AncEvent.SubmitLabAndAddAnother) },
                    enabled = enabled,
                    shape = RecordShapes.Small,
                    border = BorderStroke(
                        1.dp,
                        if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .minimumTouchTarget()
                        .testTag("addAnotherLab"),
                ) {
                    Text(
                        text = if (panelRemainingIds.isNotEmpty()) {
                            stringResource(R.string.lab_save_and_next)
                        } else {
                            stringResource(R.string.add_another_test)
                        },
                        style = RecordType.Action,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)

        LabPatientBanner(
            patient = patient,
            asOfDate = parseIsoDateOrNull(draft.values.date) ?: LocalDate.now(),
        )

        BatchInfoBar(
            dateText = draft.values.date,
            gaText = calculatedGa?.shortWeeksText,
            labName = draft.values.labName,
            onChangeClick = { batchInfoOpen = true },
            modifier = Modifier.fillMaxWidth(),
            tag = "labBatchChange",
            useRecordStyle = true,
            enabled = enabled,
        )

        val isDipstick = isDipstickTest(draft.values)

        OutlinedCard(
            shape = RecordShapes.Large,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().testTag("labTestCard"),
        ) {
            CompositionLocalProvider(LocalFieldFamily provides bgFamily) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(RecordSpacing.M),
                ) {
                    val detailsToggleText = stringResource(
                        if (testSelectorOpen) R.string.more_close else R.string.edit_test_details,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = draft.values.testName.ifBlank { stringResource(R.string.new_test) },
                            style = RecordType.SectionTitle,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 6.dp)
                                .semantics { heading() }
                                .testTag("labTestTitle"),
                        )
                        SectionTextAction(
                            text = detailsToggleText,
                            contentDescription = detailsToggleText,
                            onClick = { testSelectorOpen = !testSelectorOpen },
                            modifier = Modifier.testTag("labEditDetailsToggle"),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(RecordSpacing.M),
                    ) {
                        val categoryText = draft.values.testCategory.trim()
                        val progressText = when {
                            panelTotalCount > 1 && categoryText.isNotBlank() ->
                                stringResource(R.string.lab_test_progress_category, currentPanelIndex, panelTotalCount, categoryText)
                            panelTotalCount > 1 -> stringResource(R.string.lab_test_progress, currentPanelIndex, panelTotalCount)
                            else -> categoryText
                        }
                        val unitText = draft.values.unit.trim()
                        val refText = draft.values.referenceRange.trim()
                        val subtitle = when {
                            unitText.isNotEmpty() && refText.isNotEmpty() ->
                                stringResource(R.string.lab_ref_from_report, unitText, refText)
                            unitText.isNotEmpty() -> unitText
                            refText.isNotEmpty() -> stringResource(R.string.lab_ref_only_from_report, refText)
                            isDipstick -> stringResource(R.string.lab_dipstick)
                            else -> ""
                        }
                        val showSubtitle = subtitle.isNotEmpty() && !testSelectorOpen
                        val showHeaderMeta = panelRemainingIds.isNotEmpty() || progressText.isNotBlank() || showSubtitle

                        if (showHeaderMeta) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (panelRemainingIds.isNotEmpty() || progressText.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (progressText.isNotBlank()) {
                                            Text(
                                                text = progressText,
                                                style = RecordType.FieldLabel,
                                                color = bgFamily.label,
                                                modifier = Modifier.weight(1f).testTag("labTestProgress"),
                                            )
                                        } else {
                                            Spacer(Modifier.weight(1f))
                                        }
                                        if (panelRemainingIds.isNotEmpty()) {
                                            BandChip(
                                                text = stringResource(R.string.lab_chained_batch),
                                                tone = null,
                                                modifier = Modifier.testTag("pinnedLabHeaderTag"),
                                            )
                                        }
                                    }
                                }
                                if (showSubtitle) {
                                    Text(
                                        text = subtitle,
                                        style = RecordType.Secondary,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("labTestSubtitle"),
                                    )
                                }
                            }
                        }

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
                                externalLabel = true,
                                shape = RecordShapes.Small,
                                textStyle = RecordType.FieldValue,
                            )

                            QuickPresetChips(
                                title = if (selectedGroupName != null) {
                                    stringResource(R.string.lab_common_tests_group, selectedGroupName!!)
                                } else {
                                    stringResource(R.string.common_tests)
                                },
                                items = displayedItems.map { it.testName },
                                enabled = enabled,
                                onSelect = { selectedName ->
                                    clearPanelQueue()
                                    val item = catalogLabItems.firstOrNull { it.testName.equals(selectedName, ignoreCase = true) }
                                    if (item != null) {
                                        loadLabItemIntoDraft(item, onEvent)
                                    }
                                },
                                tagPrefix = "labPreset",
                                collapseAfter = if (selectedGroupName == null) PRESAVED_COLLAPSE_AFTER else null,
                                useRecordStyle = true,
                            )
                        }

                        if (isDipstick) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FormFieldLabel(stringResource(R.string.lab_dipstick_result))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    DipstickChoices.forEachIndexed { index, choice ->
                                        RecordFilterChip(
                                            selected = draft.values.result.trim() == choice,
                                            label = choice,
                                            onClick = {
                                                onEvent(AncEvent.LabDraftChanged(LabDraftField.RESULT, choice))
                                                onEvent(AncEvent.LabAbnormalChanged(choice != "Normal"))
                                            },
                                            tag = "labResultPreset-$index",
                                            enabled = enabled,
                                            minTouchTarget = true,
                                        )
                                    }
                                }
                            }
                        } else {
                            LabReadoutStrip(
                                resultText = draft.values.result,
                                unitText = unitText,
                                modifier = Modifier.testTag("labStoryDisplay"),
                                valueTag = "labStoryValue",
                                flagContent = if (resultFlag != LabResultFlag.UNKNOWN) {
                                    {
                                        BandChip(
                                            text = when (resultFlag) {
                                                LabResultFlag.NORMAL -> stringResource(R.string.lab_flag_normal)
                                                LabResultFlag.HIGH -> stringResource(R.string.lab_flag_high)
                                                LabResultFlag.LOW -> stringResource(R.string.lab_flag_low)
                                                LabResultFlag.UNKNOWN -> ""
                                            },
                                            tone = if (resultFlag == LabResultFlag.NORMAL) StatusTone.Neutral else StatusTone.High,
                                            modifier = Modifier.testTag("labResultFlag"),
                                        )
                                    }
                                } else {
                                    null
                                },
                            )

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
                                externalLabel = true,
                                shape = RecordShapes.Small,
                                textStyle = RecordType.KeyValue,
                            )

                            val unitAndRefFields: @Composable (Modifier) -> Unit = { fieldModifier ->
                                EditorField(
                                    value = draft.values.unit,
                                    onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.UNIT, it)) },
                                    label = stringResource(R.string.unit),
                                    tag = "labUnit",
                                    modifier = fieldModifier,
                                    enabled = enabled,
                                    singleLine = true,
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next,
                                    externalLabel = true,
                                    shape = RecordShapes.Small,
                                    textStyle = RecordType.FieldValue,
                                )
                                EditorField(
                                    value = draft.values.referenceRange,
                                    onValueChange = { onEvent(AncEvent.LabDraftChanged(LabDraftField.REFERENCE_RANGE, it)) },
                                    label = stringResource(R.string.reference_range),
                                    tag = "labRefRange",
                                    modifier = fieldModifier,
                                    enabled = enabled,
                                    singleLine = true,
                                    keyboardType = KeyboardType.Text,
                                    externalLabel = true,
                                    shape = RecordShapes.Small,
                                    textStyle = RecordType.FieldValue,
                                )
                            }

                            if (narrowStacked) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    unitAndRefFields(Modifier)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    unitAndRefFields(Modifier.weight(1f))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable(enabled = enabled) {
                                    onEvent(AncEvent.LabAbnormalChanged(!draft.values.isAbnormal))
                                }
                                .padding(vertical = 4.dp)
                                .testTag("labFlagToggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.is_abnormal),
                                    style = RecordType.FieldValue,
                                    fontWeight = if (draft.values.isAbnormal) FontWeight.Bold else FontWeight.Normal,
                                    color = if (draft.values.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(
                                        if (isDipstick) R.string.lab_dipstick_auto_flag_hint else R.string.lab_range_auto_flag_hint,
                                    ),
                                    style = RecordType.Secondary,
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
                                externalLabel = true,
                                shape = RecordShapes.Small,
                                textStyle = RecordType.FieldValue,
                            )
                        }
                    }
                }
            }
        }

        OutlinedCard(
            shape = RecordShapes.Large,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().testTag("labAddTestsCard"),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable { testSelectorOpen = !testSelectorOpen }
                        .padding(horizontal = 16.dp)
                        .testTag("labAddTestsToggle"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.add_tests),
                        style = RecordType.CardName,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Text(
                        text = stringResource(if (testSelectorOpen) R.string.more_close else R.string.more_open),
                        style = RecordType.Action,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (testSelectorOpen) {
                    CompositionLocalProvider(LocalFieldFamily provides bgFamily) {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (catalogGroups.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FormFieldLabel(stringResource(R.string.lab_filter_by_category))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        val labItemCount = catalogLabItems.size
                                        RecordFilterChip(
                                            selected = selectedGroupName == null,
                                            label = stringResource(R.string.lab_all_tests_count, labItemCount),
                                            onClick = {
                                                selectedGroupName = null
                                                clearPanelQueue()
                                            },
                                            tag = "allLabTestsChip",
                                            wrap = true,
                                            minTouchTarget = true,
                                        )
                                        catalogGroups.forEach { grp ->
                                            RecordFilterChip(
                                                selected = selectedGroupName == grp.name,
                                                label = grp.name,
                                                onClick = {
                                                    if (selectedGroupName == grp.name) {
                                                        selectedGroupName = null
                                                        clearPanelQueue()
                                                    } else {
                                                        selectedGroupName = grp.name
                                                    }
                                                },
                                                tag = "labGroupChip-${grp.id}",
                                                wrap = true,
                                                minTouchTarget = true,
                                            )
                                        }
                                    }
                                }
                            }

                            val trimmedTestName = draft.values.testName.trim()
                            val isAlreadyInCatalog = catalogLabItems.any { it.testName.equals(trimmedTestName, ignoreCase = true) }
                            if (trimmedTestName.isNotBlank() && !isAlreadyInCatalog) {
                                Surface(
                                    color = bgFamily.container,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RecordShapes.Small,
                                    modifier = Modifier.fillMaxWidth().testTag("saveToLabCodexBanner"),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.lab_save_template_prompt),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                            style = RecordType.Secondary,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        TextButton(
                                            onClick = {
                                                catalogRepo.addLabCatalogItem(
                                                    LabCatalogItem(
                                                        testName = trimmedTestName,
                                                        category = draft.values.testCategory.ifBlank { "Hematology" },
                                                        unit = draft.values.unit.trim(),
                                                        referenceRange = draft.values.referenceRange.trim(),
                                                        defaultResult = draft.values.result.trim(),
                                                        isCustom = true,
                                                    ),
                                                )
                                            },
                                            shape = RecordShapes.Small,
                                            modifier = Modifier.minimumTouchTarget().testTag("saveToLabCodexButton"),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.lab_add_template),
                                                style = RecordType.Action,
                                            )
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
                                labelStyle = RecordType.FieldLabel,
                                labelColor = bgFamily.label,
                                useRecordChip = true,
                            )
                        }
                    }
                }
            }
        }
    }
}
