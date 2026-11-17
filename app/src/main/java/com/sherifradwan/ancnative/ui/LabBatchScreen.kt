package com.sherifradwan.ancnative.ui

import java.time.LocalDate
import java.util.UUID
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.catalog.ClinicalCatalogRepository
import com.sherifradwan.ancnative.model.catalog.LabCatalogItem
import com.sherifradwan.ancnative.model.catalog.LabCategoryGroup
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.LabBatchDraft
import com.sherifradwan.ancnative.state.LabBatchItem
import com.sherifradwan.ancnative.state.LabBatchTab
import com.sherifradwan.ancnative.state.groupLabBatchItems
import com.sherifradwan.ancnative.ui.components.BandChip
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.FormFieldLabel
import com.sherifradwan.ancnative.ui.components.LocalFieldFamily
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusTone
import com.sherifradwan.ancnative.ui.components.StatusWord
import com.sherifradwan.ancnative.ui.components.ltrIsolate

/**
 * The laboratory batch: choose tests (Add), enter results (Enter), check them (Review) and save once.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabBatchScreen(state: AncState, route: AncRoute.LabBatchEditor, onEvent: (AncEvent) -> Unit) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.labBatchDraft ?: return
    val context = LocalContext.current
    val resources = LocalResources.current
    val catalogRepo = remember { ClinicalCatalogRepository.getInstance(context) }
    val catalogItems by catalogRepo.labCatalogItems.collectAsState()
    val catalogGroups by catalogRepo.labCategoryGroups.collectAsState()

    val bgFamily = FieldFamilies.background()
    val narrowStacked = LocalDensity.current.fontScale > 1.3f
    val calculatedGa = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.date)
        ?.takeIf { it.isPostConception }

    var showMetadataDialog by remember { mutableStateOf(false) }
    var choosingGroup by remember { mutableStateOf<LabCategoryGroup?>(null) }

    if (showMetadataDialog) {
        var editDate by remember(draft.date) { mutableStateOf(draft.date) }
        var editLab by remember(draft.labName) { mutableStateOf(draft.labName.orEmpty()) }
        LabMetadataDialog(
            title = stringResource(R.string.lab_batch_edit_metadata),
            dateValue = editDate,
            onDateChange = {
                editDate = it
                onEvent(AncEvent.LabBatchDateChanged(it))
            },
            dateTag = "labBatchDate",
            labNameValue = editLab,
            onLabNameChange = {
                editLab = it
                onEvent(AncEvent.LabBatchLabNameChanged(it.ifBlank { null }))
            },
            labNameTag = "labBatchLabName",
            labPresetTagPrefix = "labBatchLabPreset",
            onDismiss = { showMetadataDialog = false },
            dateError = draft.errors.contains(ClinicalValidationError.REQUIRED_DATE) ||
                draft.errors.contains(ClinicalValidationError.INVALID_DATE),
            dateFirst = true,
        )
    }

    if (choosingGroup != null) {
        val group = choosingGroup!!
        ChoosePanelDialog(
            group = group,
            catalogItems = catalogItems,
            onDismiss = { choosingGroup = null },
            onAddSelected = { selectedItems ->
                onEvent(AncEvent.LabBatchItemsAdded(selectedItems))
                choosingGroup = null
            },
            onSaveAsCustomPanel = { name, testNames ->
                val newGroup = LabCategoryGroup(
                    id = "custom_group_" + UUID.randomUUID().toString(),
                    name = name.trim(),
                    description = resources.getString(R.string.lab_batch_elements_count, testNames.size),
                    testNames = testNames,
                    isCustom = true,
                )
                catalogRepo.addLabCategoryGroup(newGroup)
            },
        )
    }

    val pinnedBottomBar: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onEvent(AncEvent.CancelLabBatch) },
                shape = RecordShapes.Small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .minimumTouchTarget()
                    .testTag("labBatchCancel"),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = RecordType.Action,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
            val saveText = when (draft.filledCount) {
                0 -> stringResource(R.string.lab_batch_review_zero_results)
                1 -> stringResource(R.string.lab_batch_save_one)
                else -> stringResource(R.string.lab_batch_save_n, draft.filledCount)
            }
            Button(
                onClick = { onEvent(AncEvent.SubmitLabBatch) },
                enabled = draft.filledCount > 0,
                shape = RecordShapes.Small,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1.6f)
                    .heightIn(min = 48.dp)
                    .minimumTouchTarget()
                    .testTag("labBatchSave"),
            ) {
                Text(
                    text = saveText,
                    style = RecordType.Action,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    EditorPage(
        title = stringResource(R.string.lab_batch_title),
        subtitle = "",
        tag = "labBatchScreen",
        bottomBar = pinnedBottomBar,
        useRecordStyle = true,
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)

        LabPatientBanner(
            patient = patient,
            asOfDate = parseIsoDateOrNull(draft.date) ?: LocalDate.now(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = { onEvent(AncEvent.CancelLabBatch) },
                modifier = Modifier.minimumTouchTarget().testTag("labBatchBack"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            BatchInfoBar(
                dateText = draft.date,
                gaText = calculatedGa?.shortWeeksText,
                labName = draft.labName,
                onChangeClick = { showMetadataDialog = true },
                modifier = Modifier.weight(1f),
                useRecordStyle = true,
            )
        }

        val addLabel = stringResource(R.string.lab_batch_tab_add)
        val enterLabel = if (draft.items.isEmpty()) {
            stringResource(R.string.lab_batch_tab_enter)
        } else {
            "${stringResource(R.string.lab_batch_tab_enter)} (${draft.filledCount}/${draft.items.size})"
        }
        val reviewLabel = "${stringResource(R.string.lab_batch_tab_review)} (${draft.filledCount})"

        val renderTabs: @Composable (Modifier) -> Unit = { tabModifier ->
            BatchTabChip(
                label = addLabel,
                selected = draft.tab == LabBatchTab.ADD,
                enabled = true,
                tag = "labBatchTab-ADD",
                onClick = { onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ADD)) },
                modifier = tabModifier,
            )
            BatchTabChip(
                label = enterLabel,
                selected = draft.tab == LabBatchTab.ENTER,
                enabled = draft.items.isNotEmpty(),
                tag = "labBatchTab-ENTER",
                onClick = { onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ENTER)) },
                modifier = tabModifier,
            )
            BatchTabChip(
                label = reviewLabel,
                selected = draft.tab == LabBatchTab.REVIEW,
                enabled = draft.items.isNotEmpty(),
                tag = "labBatchTab-REVIEW",
                onClick = { onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.REVIEW)) },
                modifier = tabModifier,
            )
        }

        if (narrowStacked) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                renderTabs(Modifier)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                renderTabs(Modifier.weight(1f))
            }
        }

        when (draft.tab) {
            LabBatchTab.ADD -> AddTab(
                groups = catalogGroups,
                catalogItems = catalogItems,
                onEvent = onEvent,
                onChooseClick = { choosingGroup = it },
            )
            LabBatchTab.ENTER -> EnterTab(draft, onEvent)
            LabBatchTab.REVIEW -> ReviewTab(draft, catalogGroups, onEvent)
        }
    }
}

@Composable
private fun BatchTabChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RecordShapes.Small,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            when {
                selected -> MaterialTheme.colorScheme.primary
                enabled -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .minimumTouchTarget()
            .semantics { this.selected = selected }
            .testTag(tag),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = RecordType.Action,
                maxLines = 2,
                textAlign = TextAlign.Center,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    enabled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTab(
    groups: List<LabCategoryGroup>,
    catalogItems: List<LabCatalogItem>,
    onEvent: (AncEvent) -> Unit,
    onChooseClick: (LabCategoryGroup) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val bgFamily = FieldFamilies.background()

    fun itemsFor(names: List<String>): List<LabBatchItem> = names.mapNotNull { name ->
        catalogItems.firstOrNull { it.testName == name }?.let { item ->
            LabBatchItem(
                labId = UUID.randomUUID().toString(),
                testName = item.testName,
                category = item.category,
                unit = item.unit,
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("labBatchAddList"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val visibleGroups = groups.filter {
            query.isBlank() || it.name.contains(query, true) || it.testNames.any { n -> n.contains(query, true) }
        }

        if (visibleGroups.isNotEmpty()) {
            RecordSectionCard(
                title = stringResource(R.string.lab_batch_panels_section),
                family = bgFamily,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    var expandedGroupId by remember { mutableStateOf<String?>(null) }
                    visibleGroups.forEachIndexed { index, group ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        val available = itemsFor(group.testNames)
                        val isExpanded = expandedGroupId == group.id
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if (index > 0) 10.dp else 0.dp,
                                    bottom = if (index < visibleGroups.lastIndex) 10.dp else 0.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumTouchTarget()
                                    .clickable { expandedGroupId = if (isExpanded) null else group.id }
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = group.name,
                                    style = RecordType.CardName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val preview = group.testNames.take(4).joinToString(", ") +
                                    if (group.testNames.size > 4) "..." else ""
                                Text(
                                    text = stringResource(R.string.lab_batch_panel_summary, group.testNames.size, preview),
                                    style = RecordType.Secondary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onChooseClick(group) },
                                    shape = RecordShapes.Small,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).minimumTouchTarget(),
                                ) {
                                    Text(
                                        text = stringResource(R.string.lab_batch_choose),
                                        style = RecordType.Action,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Button(
                                    onClick = { onEvent(AncEvent.LabBatchItemsAdded(available)) },
                                    enabled = available.isNotEmpty(),
                                    shape = RecordShapes.Small,
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).minimumTouchTarget().testTag("labBatchAdd-${group.id}"),
                                ) {
                                    Text(
                                        text = stringResource(R.string.lab_batch_add_n, available.size),
                                        style = RecordType.Action,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            if (isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    val selectedTestNames = remember(group.id) {
                                        mutableStateListOf<String>().apply { addAll(group.testNames) }
                                    }
                                    group.testNames.forEach { testName ->
                                        val isChecked = selectedTestNames.contains(testName)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .clickable {
                                                    if (isChecked) selectedTestNames.remove(testName) else selectedTestNames.add(testName)
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = testName,
                                                style = RecordType.FieldValue,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                            )
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedTestNames.add(testName) else selectedTestNames.remove(testName)
                                                },
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val filtered = itemsFor(selectedTestNames.toList())
                                            onEvent(AncEvent.LabBatchItemsAdded(filtered))
                                            expandedGroupId = null
                                        },
                                        enabled = selectedTestNames.isNotEmpty(),
                                        shape = RecordShapes.Small,
                                        modifier = Modifier.fillMaxWidth().minimumTouchTarget().padding(top = 4.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.lab_batch_add_n_selected, selectedTestNames.size),
                                            style = RecordType.Action,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        RecordSectionCard(
            title = stringResource(R.string.lab_batch_add_single_test),
            family = bgFamily,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.lab_batch_search_placeholder), style = RecordType.Secondary) },
                singleLine = true,
                shape = RecordShapes.Small,
                textStyle = RecordType.FieldValue,
                colors = editorFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
                    .testTag("labBatchSearch"),
            )

            if (query.trim().isNotEmpty()) {
                val matches = catalogItems.filter { it.testName.contains(query, true) }.take(15)
                if (matches.isEmpty()) {
                    Text(
                        text = stringResource(R.string.lab_batch_no_search_matches),
                        style = RecordType.Secondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        matches.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.testName,
                                        style = RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (item.unit.isNotBlank()) {
                                        Text(
                                            text = item.unit,
                                            style = RecordType.Secondary,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        onEvent(
                                            AncEvent.LabBatchItemsAdded(
                                                listOf(LabBatchItem(UUID.randomUUID().toString(), item.testName, item.category, item.unit)),
                                            ),
                                        )
                                    },
                                    shape = RecordShapes.Small,
                                    modifier = Modifier.minimumTouchTarget().testTag("labBatchAddOne-${item.id}"),
                                ) {
                                    Text(stringResource(R.string.lab_batch_add), style = RecordType.Action)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormFieldLabel(stringResource(R.string.lab_batch_often_added))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        listOf("Ferritin", "TSH", "Vitamin D").forEachIndexed { index, name ->
                            RecordFilterChip(
                                selected = false,
                                label = name,
                                onClick = {
                                    val match = catalogItems.firstOrNull { it.testName.contains(name, true) }
                                    val item = if (match != null) {
                                        LabBatchItem(UUID.randomUUID().toString(), match.testName, match.category, match.unit)
                                    } else {
                                        LabBatchItem(UUID.randomUUID().toString(), name, "Biochemistry", "")
                                    }
                                    onEvent(AncEvent.LabBatchItemsAdded(listOf(item)))
                                },
                                tag = "labBatchOften-$index",
                                wrap = true,
                                minTouchTarget = true,
                            )
                        }
                        RecordFilterChip(
                            selected = false,
                            label = stringResource(R.string.lab_batch_add_other),
                            onClick = {
                                query = ""
                                searchFocusRequester.requestFocus()
                            },
                            tag = "labBatchAddOtherChip",
                            wrap = true,
                            minTouchTarget = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoosePanelDialog(
    group: LabCategoryGroup,
    catalogItems: List<LabCatalogItem>,
    onDismiss: () -> Unit,
    onAddSelected: (List<LabBatchItem>) -> Unit,
    onSaveAsCustomPanel: (String, List<String>) -> Unit,
) {
    val selectedNames = remember { mutableStateListOf<String>().apply { addAll(group.testNames) } }
    var showSavePanelDialog by remember { mutableStateOf(false) }
    val bgFamily = FieldFamilies.background()

    if (showSavePanelDialog) {
        var panelName by remember { mutableStateOf(group.name + " (Custom)") }
        AlertDialog(
            onDismissRequest = { showSavePanelDialog = false },
            shape = RecordShapes.Large,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.lab_batch_save_selection_as_panel),
                    style = RecordType.SectionTitle,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            text = {
                CompositionLocalProvider(LocalFieldFamily provides bgFamily) {
                    EditorField(
                        value = panelName,
                        onValueChange = { panelName = it },
                        label = stringResource(R.string.lab_batch_panel_name),
                        tag = "labBatchCustomPanelName",
                        singleLine = true,
                        externalLabel = true,
                        shape = RecordShapes.Small,
                        textStyle = RecordType.FieldValue,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (panelName.isNotBlank()) {
                            onSaveAsCustomPanel(panelName, selectedNames.toList())
                            showSavePanelDialog = false
                        }
                    },
                    enabled = panelName.isNotBlank(),
                    shape = RecordShapes.Small,
                    modifier = Modifier.minimumTouchTarget(),
                ) {
                    Text(stringResource(R.string.save), style = RecordType.Action)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSavePanelDialog = false },
                    shape = RecordShapes.Small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.minimumTouchTarget(),
                ) {
                    Text(stringResource(R.string.cancel), style = RecordType.Action)
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RecordShapes.Large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.lab_batch_choose_tests),
                    style = RecordType.SectionTitle,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${group.name}${stringResource(R.string.detail_separator)}${stringResource(R.string.lab_batch_elements_count, group.testNames.size)}",
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.lab_batch_choose_hint),
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                group.testNames.forEach { testName ->
                    val isChecked = selectedNames.contains(testName)
                    val matchingItem = catalogItems.firstOrNull { it.testName == testName }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                if (isChecked) selectedNames.remove(testName) else selectedNames.add(testName)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = testName,
                                style = RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            matchingItem?.unit?.takeIf { it.isNotBlank() }?.let { unit ->
                                Text(
                                    text = unit,
                                    style = RecordType.Secondary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedNames.add(testName) else selectedNames.remove(testName)
                            },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { showSavePanelDialog = true },
                    shape = RecordShapes.Small,
                    modifier = Modifier.fillMaxWidth().minimumTouchTarget(),
                ) {
                    Text(
                        text = stringResource(R.string.lab_batch_save_selection_as_panel),
                        style = RecordType.Action,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val items = selectedNames.map { name ->
                        val item = catalogItems.firstOrNull { it.testName == name }
                        LabBatchItem(
                            labId = UUID.randomUUID().toString(),
                            testName = name,
                            category = item?.category ?: "Hematology",
                            unit = item?.unit.orEmpty(),
                        )
                    }
                    onAddSelected(items)
                },
                enabled = selectedNames.isNotEmpty(),
                shape = RecordShapes.Small,
                modifier = Modifier.minimumTouchTarget(),
            ) {
                Text(stringResource(R.string.lab_batch_add_n_selected, selectedNames.size), style = RecordType.Action)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RecordShapes.Small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.minimumTouchTarget(),
            ) {
                Text(stringResource(R.string.cancel), style = RecordType.Action)
            }
        },
    )
}

private fun qualitativeChoicesFor(testName: String): List<String>? {
    val name = testName.lowercase()
    return when {
        name.contains("protein") -> listOf("Normal", "Trace", "+", "++", "+++")
        name.contains("glucose") || name.contains("ketone") || name.contains("nitrite") -> listOf("Negative", "Trace", "+", "++", "+++")
        name.contains("blood") && !name.contains("sugar") && !name.contains("group") -> listOf("Negative", "Trace", "+", "++", "+++")
        name.contains("leukocyte") || name.contains("esterase") -> listOf("Negative", "Trace", "+", "++")
        name.contains("pus") || name.contains("rbc") -> listOf("0–2", "3–5", "6–10", ">10 /hpf")
        name.contains("hbsag") || name.contains("vdrl") || name.contains("tpha") -> listOf("Non-reactive", "Reactive")
        name.contains("hiv") || name.contains("hcv") -> listOf("Non-reactive", "Reactive")
        else -> null
    }
}

private fun isChoiceAbnormal(choice: String): Boolean {
    val c = choice.lowercase().trim()
    return c !in setOf("normal", "negative", "non-reactive", "0–2", "0-2")
}

@Composable
private fun EnterTab(draft: LabBatchDraft, onEvent: (AncEvent) -> Unit) {
    val bgFamily = FieldFamilies.background()
    val item = draft.currentItem
    if (item == null) {
        RecordSectionCard(
            title = stringResource(R.string.lab_batch_tab_enter),
            family = bgFamily,
        ) {
            Text(
                text = stringResource(R.string.lab_batch_empty_add_first),
                style = RecordType.FieldValue,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("labBatchEnter"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BatchSegmentedProgress(
            total = draft.items.size,
            current = draft.cursor,
            useRecordStyle = true,
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RecordShapes.Large,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            CompositionLocalProvider(LocalFieldFamily provides bgFamily) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            FormFieldLabel(
                                stringResource(
                                    R.string.lab_batch_enter_progress,
                                    item.category.ifBlank { stringResource(R.string.lab_batch_default_category) },
                                    draft.cursor + 1,
                                    draft.items.size,
                                ),
                            )
                            Text(
                                text = item.testName,
                                style = RecordType.SectionTitle,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        IconButton(
                            onClick = { onEvent(AncEvent.LabBatchItemRemoved(item.labId)) },
                            modifier = Modifier.minimumTouchTarget().testTag("labBatchDeleteItem"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.lab_batch_remove_item, item.testName),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val qualitativeChoices = qualitativeChoicesFor(item.testName)

                        if (qualitativeChoices != null) {
                            Text(
                                text = if (item.category.contains("Urine", true)) {
                                    stringResource(R.string.lab_dipstick)
                                } else {
                                    item.category
                                },
                                style = RecordType.Secondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                qualitativeChoices.forEach { choice ->
                                    val isSelected = item.result == choice
                                    QualitativeChoiceButton(
                                        label = choice,
                                        isSelected = isSelected,
                                        onClick = {
                                            onEvent(AncEvent.LabBatchResultChanged(item.labId, choice))
                                            onEvent(AncEvent.LabBatchFlagChanged(item.labId, isChoiceAbnormal(choice)))
                                        },
                                        useRecordStyle = true,
                                    )
                                }
                            }
                        } else {
                            val sub = if (item.unit.isNotBlank()) {
                                stringResource(
                                    R.string.lab_batch_unit_range,
                                    item.unit,
                                    item.referenceRange.ifBlank { stringResource(R.string.lab_batch_from_report) },
                                )
                            } else {
                                stringResource(R.string.lab_batch_from_report)
                            }
                            Text(
                                text = sub,
                                style = RecordType.Secondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (item.result.isNotBlank()) {
                                LabReadoutStrip(
                                    resultText = item.result,
                                    unitText = item.unit,
                                    flagContent = {
                                        BandChip(
                                            text = stringResource(
                                                if (item.isAbnormal) R.string.lab_abnormal else R.string.lab_group_normal,
                                            ),
                                            tone = if (item.isAbnormal) StatusTone.High else StatusTone.Neutral,
                                        )
                                    },
                                )
                            }

                            EditorField(
                                value = item.result,
                                onValueChange = { onEvent(AncEvent.LabBatchResultChanged(item.labId, it)) },
                                label = stringResource(R.string.test_result),
                                tag = "labBatchResult",
                                singleLine = true,
                                keyboardType = KeyboardType.Decimal,
                                externalLabel = true,
                                shape = RecordShapes.Small,
                                textStyle = RecordType.KeyValue,
                            )

                            EditorField(
                                value = item.referenceRange,
                                onValueChange = { onEvent(AncEvent.LabBatchRangeChanged(item.labId, it)) },
                                label = stringResource(
                                    R.string.lab_batch_reference_range_hint,
                                    stringResource(R.string.lab_batch_from_report),
                                ),
                                tag = "labBatchRange",
                                singleLine = true,
                                externalLabel = true,
                                shape = RecordShapes.Small,
                                textStyle = RecordType.FieldValue,
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onEvent(AncEvent.LabBatchFlagChanged(item.labId, !item.isAbnormal)) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.lab_batch_abnormal),
                                style = RecordType.FieldValue,
                                fontWeight = if (item.isAbnormal) FontWeight.Bold else FontWeight.Medium,
                                color = if (item.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = item.isAbnormal,
                                onCheckedChange = { onEvent(AncEvent.LabBatchFlagChanged(item.labId, it)) },
                                modifier = Modifier.testTag("labBatchFlag"),
                            )
                        }

                        EditorField(
                            value = item.notes,
                            onValueChange = { onEvent(AncEvent.LabBatchNotesChanged(item.labId, it)) },
                            label = stringResource(R.string.notes),
                            tag = "labBatchNote",
                            singleLine = false,
                            externalLabel = true,
                            shape = RecordShapes.Small,
                            textStyle = RecordType.FieldValue,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onEvent(AncEvent.LabBatchCursorMoved(draft.cursor - 1)) },
                                enabled = draft.cursor > 0,
                                shape = RecordShapes.Small,
                                border = BorderStroke(
                                    1.dp,
                                    if (draft.cursor > 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .minimumTouchTarget()
                                    .testTag("labBatchPrev"),
                            ) {
                                Text(
                                    text = stringResource(R.string.back_step),
                                    style = RecordType.Action,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Button(
                                onClick = {
                                    if (draft.cursor < draft.items.lastIndex) {
                                        onEvent(AncEvent.LabBatchCursorMoved(draft.cursor + 1))
                                    } else {
                                        onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.REVIEW))
                                    }
                                },
                                shape = RecordShapes.Small,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .minimumTouchTarget()
                                    .testTag("labBatchNext"),
                            ) {
                                Text(
                                    text = stringResource(
                                        if (draft.cursor < draft.items.lastIndex) R.string.next_step else R.string.lab_batch_tab_review,
                                    ),
                                    style = RecordType.Action,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = { onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ADD)) },
            shape = RecordShapes.Small,
            modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("labBatchAddInline"),
        ) {
            Text(
                text = stringResource(R.string.add_test_to_report),
                style = RecordType.Action,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReviewTab(
    draft: LabBatchDraft,
    groups: List<LabCategoryGroup>,
    onEvent: (AncEvent) -> Unit,
) {
    val grouped = groupLabBatchItems(draft.items, groups)
    val bgFamily = FieldFamilies.background()

    Column(
        modifier = Modifier.fillMaxWidth().testTag("labBatchReview"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        grouped.forEach { section ->
            RecordSectionCard(
                title = section.title,
                family = bgFamily,
                action = {
                    Text(
                        text = stringResource(R.string.lab_batch_elements_count, section.items.size),
                        style = RecordType.FieldLabel,
                        color = bgFamily.label,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    section.items.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        val jumpToItem = {
                            val itemIndex = draft.items.indexOf(item)
                            if (itemIndex >= 0) {
                                onEvent(AncEvent.LabBatchCursorMoved(itemIndex))
                                onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ENTER))
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(onClick = jumpToItem)
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.testName,
                                    style = RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                item.unit.takeIf { it.isNotBlank() }?.let { unit ->
                                    Text(
                                        text = unit,
                                        style = RecordType.Secondary,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f, fill = false),
                                horizontalAlignment = Alignment.End,
                            ) {
                                if (item.result.isBlank()) {
                                    StatusWord(
                                        text = stringResource(R.string.lab_batch_pending),
                                        tone = StatusTone.Moderate,
                                    )
                                } else {
                                    val resultDisplay = if (item.unit.isNotBlank()) "${item.result} ${item.unit}" else item.result
                                    Text(
                                        text = ltrIsolate(resultDisplay),
                                        style = RecordType.MetricValue.copy(fontWeight = FontWeight.Bold),
                                        color = if (item.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.End,
                                    )
                                    StatusWord(
                                        text = stringResource(
                                            if (item.isAbnormal) R.string.lab_abnormal else R.string.lab_group_normal,
                                        ),
                                        tone = if (item.isAbnormal) StatusTone.High else StatusTone.Neutral,
                                    )
                                }
                            }
                            TextButton(
                                onClick = jumpToItem,
                                shape = RecordShapes.Small,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.minimumTouchTarget().testTag("labBatchEdit-${item.labId}"),
                            ) {
                                Text(stringResource(R.string.lab_batch_edit), style = RecordType.Action)
                            }
                        }
                    }
                }
            }
        }

        val emptyQualitative = draft.items.filter { it.result.isBlank() && (it.unit.isBlank() || it.category.contains("Urine", true)) }
        if (emptyQualitative.isNotEmpty()) {
            OutlinedCard(
                shape = RecordShapes.Large,
                colors = CardDefaults.outlinedCardColors(containerColor = bgFamily.container),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.lab_batch_mark_normal_hint),
                        style = RecordType.Secondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            emptyQualitative.forEach { item ->
                                val defaultValue = if (item.testName.lowercase().contains("protein")) "Normal" else "Negative"
                                onEvent(AncEvent.LabBatchResultChanged(item.labId, defaultValue))
                            }
                        },
                        shape = RecordShapes.Small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("labBatchMarkNormal"),
                    ) {
                        Text(
                            text = stringResource(R.string.lab_batch_mark_empty_normal, emptyQualitative.size),
                            style = RecordType.Action,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
