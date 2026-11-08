package com.sherifradwan.ancnative.ui

import java.time.LocalDate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.model.codex.LabCategoryGroup
import com.sherifradwan.ancnative.model.codex.LabCodexItem
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.LabBatchDraft
import com.sherifradwan.ancnative.state.LabBatchItem
import com.sherifradwan.ancnative.state.LabBatchTab
import com.sherifradwan.ancnative.state.groupLabBatchItems
import java.util.UUID

enum class LabResultPillStatus {
    NORMAL,
    ABNORMAL,
    PENDING,
}

@Composable
fun LabResultPill(
    text: String,
    status: LabResultPillStatus,
    modifier: Modifier = Modifier,
) {
    val (bg, textColor) = when (status) {
        LabResultPillStatus.NORMAL -> TagNormalRiskBg to TagNormalRiskText
        LabResultPillStatus.ABNORMAL -> TagHighRiskBg to TagHighRiskText
        LabResultPillStatus.PENDING -> TagAttentionBg to TagAttentionText
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/**
 * The laboratory batch: choose tests (Add), enter results (Enter), check them (Review) and save once.
 * Layout follows the approved mock (design/rev-b/lab-batch.html); the catalogue supplies the panels.
 */
@Composable
fun LabBatchScreen(state: AncState, route: AncRoute.LabBatchEditor, onEvent: (AncEvent) -> Unit) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.labBatchDraft ?: return
    val context = LocalContext.current
    val codexRepo = remember { ClinicalCodexRepository.getInstance(context) }
    val codexItems by codexRepo.labCodexItems.collectAsState()
    val codexGroups by codexRepo.labCategoryGroups.collectAsState()

    val calculatedGa = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.date)
    val gaText = calculatedGa?.displayText?.let { "GA $it" }
    val subtitle = if (gaText != null) "${patient.displayName} · $gaText" else patient.displayName

    var showMetadataDialog by remember { mutableStateOf(false) }
    var choosingGroup by remember { mutableStateOf<LabCategoryGroup?>(null) }

    if (showMetadataDialog) {
        var editDate by remember(draft.date) { mutableStateOf(draft.date) }
        var editLab by remember(draft.labName) { mutableStateOf(draft.labName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showMetadataDialog = false },
            title = { Text(stringResource(R.string.lab_batch_edit_metadata)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editDate,
                        onValueChange = {
                            editDate = it
                            onEvent(AncEvent.LabBatchDateChanged(it))
                        },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("labBatchDate"),
                    )
                    OutlinedTextField(
                        value = editLab,
                        onValueChange = {
                            editLab = it
                            onEvent(AncEvent.LabBatchLabNameChanged(it.ifBlank { null }))
                        },
                        label = { Text("Laboratory") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("labBatchLabName"),
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showMetadataDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            },
        )
    }

    if (choosingGroup != null) {
        val group = choosingGroup!!
        ChoosePanelDialog(
            group = group,
            codexItems = codexItems,
            onDismiss = { choosingGroup = null },
            onAddSelected = { selectedItems ->
                onEvent(AncEvent.LabBatchItemsAdded(selectedItems))
                choosingGroup = null
            },
            onSaveAsCustomPanel = { name, testNames ->
                val newGroup = LabCategoryGroup(
                    id = "custom_group_" + UUID.randomUUID().toString(),
                    name = name.trim(),
                    description = "${testNames.size} elements",
                    testNames = testNames,
                    isCustom = true,
                )
                codexRepo.addLabCategoryGroup(newGroup)
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
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("labBatchCancel"),
            ) {
                Text(stringResource(R.string.cancel))
            }
            val saveText = when (draft.filledCount) {
                0 -> stringResource(R.string.lab_batch_review_zero_results)
                1 -> stringResource(R.string.lab_batch_save_one)
                else -> stringResource(R.string.lab_batch_save_n, draft.filledCount)
            }
            Button(
                onClick = { onEvent(AncEvent.SubmitLabBatch) },
                enabled = draft.filledCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(2f).heightIn(min = 48.dp).testTag("labBatchSave"),
            ) {
                Text(saveText, fontWeight = FontWeight.Bold)
            }
        }
    }

    EditorPage(
        title = stringResource(R.string.lab_batch_title),
        subtitle = subtitle,
        tag = "labBatchScreen",
        pinnedBottomBar,
    ) {
        ClinicalPatientCard(
            patient = patient,
            variant = PatientCardVariant.EDITOR_SUMMARY,
            asOfDate = runCatching { LocalDate.parse(draft.date) }.getOrNull() ?: LocalDate.now(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = { onEvent(AncEvent.CancelLabBatch) },
                modifier = Modifier.testTag("labBatchBack"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            BatchInfoBar(
                dateText = draft.date,
                gaText = gaText,
                labName = draft.labName,
                onChangeClick = { showMetadataDialog = true },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = draft.tab == LabBatchTab.ADD,
                onClick = { onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ADD)) },
                label = { Text(stringResource(R.string.lab_batch_tab_add)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ActionRose.copy(alpha = 0.15f),
                    selectedLabelColor = ActionRose,
                ),
                modifier = Modifier.testTag("labBatchTab-ADD"),
            )
            val enterLabel = if (draft.items.isEmpty()) {
                stringResource(R.string.lab_batch_tab_enter)
            } else {
                "${stringResource(R.string.lab_batch_tab_enter)} (${draft.filledCount}/${draft.items.size})"
            }
            FilterChip(
                selected = draft.tab == LabBatchTab.ENTER,
                onClick = { onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ENTER)) },
                label = { Text(enterLabel) },
                enabled = draft.items.isNotEmpty(),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ActionRose.copy(alpha = 0.15f),
                    selectedLabelColor = ActionRose,
                ),
                modifier = Modifier.testTag("labBatchTab-ENTER"),
            )
            val reviewLabel = "${stringResource(R.string.lab_batch_tab_review)} (${draft.filledCount})"
            FilterChip(
                selected = draft.tab == LabBatchTab.REVIEW,
                onClick = { onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.REVIEW)) },
                label = { Text(reviewLabel) },
                enabled = draft.items.isNotEmpty(),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ActionRose.copy(alpha = 0.15f),
                    selectedLabelColor = ActionRose,
                ),
                modifier = Modifier.testTag("labBatchTab-REVIEW"),
            )
        }

        when (draft.tab) {
            LabBatchTab.ADD -> AddTab(
                groups = codexGroups,
                codexItems = codexItems,
                draft = draft,
                onEvent = onEvent,
                onChooseClick = { choosingGroup = it },
            )
            LabBatchTab.ENTER -> EnterTab(draft, onEvent)
            LabBatchTab.REVIEW -> ReviewTab(draft, codexGroups, onEvent)
        }
    }
}

@Composable
private fun AddTab(
    groups: List<LabCategoryGroup>,
    codexItems: List<LabCodexItem>,
    draft: LabBatchDraft,
    onEvent: (AncEvent) -> Unit,
    onChooseClick: (LabCategoryGroup) -> Unit,
) {
    var query by remember { mutableStateOf("") }

    fun itemsFor(names: List<String>): List<LabBatchItem> = names.mapNotNull { name ->
        codexItems.firstOrNull { it.testName == name }?.let { item ->
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val visibleGroups = groups.filter {
            query.isBlank() || it.name.contains(query, true) || it.testNames.any { n -> n.contains(query, true) }
        }

        if (visibleGroups.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    var expandedGroupId by remember { mutableStateOf<String?>(null) }
                    visibleGroups.forEachIndexed { index, group ->
                        val available = itemsFor(group.testNames)
                        val isExpanded = expandedGroupId == group.id
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            expandedGroupId = if (isExpanded) null else group.id
                                        },
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(group.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Text(
                                        "${group.testNames.size} elements · ${group.testNames.take(4).joinToString(", ")}${if (group.testNames.size > 4) "..." else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = { onChooseClick(group) },
                                        modifier = Modifier.heightIn(min = 40.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                                    ) {
                                        Text(stringResource(R.string.lab_batch_choose))
                                    }
                                    Button(
                                        onClick = { onEvent(AncEvent.LabBatchItemsAdded(available)) },
                                        enabled = available.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.heightIn(min = 40.dp).testTag("labBatchAdd-" + group.id),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(R.string.lab_batch_add_n, available.size))
                                    }
                                }
                            }

                            if (isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
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
                                                .clickable {
                                                    if (isChecked) selectedTestNames.remove(testName) else selectedTestNames.add(testName)
                                                }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(testName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedTestNames.add(testName) else selectedTestNames.remove(testName)
                                                },
                                                modifier = Modifier.size(24.dp),
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
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    ) {
                                        Text("Add Selected (${selectedTestNames.size} tests) ▶")
                                    }
                                }
                            }
                        }
                        if (index < visibleGroups.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        SectionHeader(title = stringResource(R.string.lab_batch_add_single_test))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.lab_batch_search_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("labBatchSearch"),
                )

                if (query.isNotBlank()) {
                    val matches = codexItems.filter { it.testName.contains(query, true) }.take(15)
                    matches.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.testName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                if (item.unit.isNotBlank()) {
                                    Text(item.unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                modifier = Modifier.testTag("labBatchAddOne-" + item.id),
                            ) {
                                Text("+ " + stringResource(R.string.lab_batch_add))
                            }
                        }
                        if (index < matches.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                } else {
                    Text(
                        stringResource(R.string.lab_batch_often_added).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("Ferritin", "TSH", "Vitamin D").forEach { name ->
                            SuggestionChip(
                                onClick = {
                                    val match = codexItems.firstOrNull { it.testName.contains(name, true) }
                                    val item = if (match != null) {
                                        LabBatchItem(UUID.randomUUID().toString(), match.testName, match.category, match.unit)
                                    } else {
                                        LabBatchItem(UUID.randomUUID().toString(), name, "Biochemistry", "")
                                    }
                                    onEvent(AncEvent.LabBatchItemsAdded(listOf(item)))
                                },
                                label = { Text(name, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(20.dp),
                            )
                        }
                        SuggestionChip(
                            onClick = { query = " " },
                            label = { Text(stringResource(R.string.lab_batch_add_other), fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
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
    codexItems: List<LabCodexItem>,
    onDismiss: () -> Unit,
    onAddSelected: (List<LabBatchItem>) -> Unit,
    onSaveAsCustomPanel: (String, List<String>) -> Unit,
) {
    val selectedNames = remember { mutableStateListOf<String>().apply { addAll(group.testNames) } }
    var showSavePanelDialog by remember { mutableStateOf(false) }

    if (showSavePanelDialog) {
        var panelName by remember { mutableStateOf(group.name + " (Custom)") }
        AlertDialog(
            onDismissRequest = { showSavePanelDialog = false },
            title = { Text(stringResource(R.string.lab_batch_save_selection_as_panel)) },
            text = {
                OutlinedTextField(
                    value = panelName,
                    onValueChange = { panelName = it },
                    label = { Text("Panel name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSavePanelDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.lab_batch_choose_tests), fontWeight = FontWeight.Bold)
                Text(
                    "${group.name} · ${group.testNames.size} elements",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.lab_batch_choose_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                group.testNames.forEach { testName ->
                    val isChecked = selectedNames.contains(testName)
                    val matchingItem = codexItems.firstOrNull { it.testName == testName }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedNames.remove(testName) else selectedNames.add(testName)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(testName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            matchingItem?.unit?.takeIf { it.isNotBlank() }?.let { unit ->
                                Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedNames.add(testName) else selectedNames.remove(testName)
                            },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { showSavePanelDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.lab_batch_save_selection_as_panel), color = ActionRose, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val items = selectedNames.map { name ->
                        val item = codexItems.firstOrNull { it.testName == name }
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(R.string.lab_batch_add_n_selected, selectedNames.size))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
    val item = draft.currentItem
    if (item == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.lab_batch_empty_add_first), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("labBatchEnter"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BatchSegmentedProgress(total = draft.items.size, current = draft.cursor)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${item.category.ifBlank { "Test" }} · Test ${draft.cursor + 1} of ${draft.items.size}".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(
                        onClick = { onEvent(AncEvent.LabBatchItemRemoved(item.labId)) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("labBatchDeleteItem"),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(26.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✕",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                }
                Text(item.testName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                val qualitativeChoices = qualitativeChoicesFor(item.testName)

                if (qualitativeChoices != null) {
                    Text(
                        if (item.category.contains("Urine", true)) "Dipstick" else item.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    qualitativeChoices.forEach { choice ->
                        val isSelected = item.result == choice
                        QualitativeChoiceButton(
                            label = choice,
                            isSelected = isSelected,
                            onClick = {
                                onEvent(AncEvent.LabBatchResultChanged(item.labId, choice))
                                onEvent(AncEvent.LabBatchFlagChanged(item.labId, isChoiceAbnormal(choice)))
                            },
                        )
                    }
                } else {
                    val sub = if (item.unit.isNotBlank()) {
                        "${item.unit} · range ${item.referenceRange.ifBlank { stringResource(R.string.lab_batch_from_report) }}"
                    } else {
                        stringResource(R.string.lab_batch_from_report)
                    }
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (item.result.isNotBlank()) {
                        Text(
                            text = "${item.result} ${item.unit}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        )
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            LabResultPill(
                                text = if (item.isAbnormal) "Abnormal ▲" else "Normal ✓",
                                status = if (item.isAbnormal) LabResultPillStatus.ABNORMAL else LabResultPillStatus.NORMAL,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = item.result,
                        onValueChange = { onEvent(AncEvent.LabBatchResultChanged(item.labId, it)) },
                        label = { Text(stringResource(R.string.test_result)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("labBatchResult"),
                    )

                    OutlinedTextField(
                        value = item.referenceRange,
                        onValueChange = { onEvent(AncEvent.LabBatchRangeChanged(item.labId, it)) },
                        label = { Text("Reference range (${stringResource(R.string.lab_batch_from_report)})") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("labBatchRange"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.lab_batch_abnormal), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = item.isAbnormal,
                        onCheckedChange = { onEvent(AncEvent.LabBatchFlagChanged(item.labId, it)) },
                        modifier = Modifier.testTag("labBatchFlag"),
                    )
                }

                OutlinedTextField(
                    value = item.notes,
                    onValueChange = { onEvent(AncEvent.LabBatchNotesChanged(item.labId, it)) },
                    label = { Text(stringResource(R.string.notes)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("labBatchNote"),
                )

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onEvent(AncEvent.LabBatchCursorMoved(draft.cursor - 1)) },
                        enabled = draft.cursor > 0,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("labBatchPrev"),
                    ) {
                        Text("◀ " + stringResource(R.string.back_step))
                    }
                    Button(
                        onClick = {
                            if (draft.cursor < draft.items.lastIndex) {
                                onEvent(AncEvent.LabBatchCursorMoved(draft.cursor + 1))
                            } else {
                                onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.REVIEW))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("labBatchNext"),
                    ) {
                        Text(
                            if (draft.cursor < draft.items.lastIndex) {
                                stringResource(R.string.next_step) + " ▶"
                            } else {
                                stringResource(R.string.lab_batch_tab_review)
                            },
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(
                onClick = { onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ADD)) },
                modifier = Modifier.testTag("labBatchAddInline"),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "+ Add Test to this Report",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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

    Column(
        modifier = Modifier.fillMaxWidth().testTag("labBatchReview"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        grouped.forEach { section ->
            SectionHeader(
                title = section.title,
                trailingText = stringResource(R.string.lab_batch_elements_count, section.items.size),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val itemIndex = draft.items.indexOf(item)
                                    if (itemIndex >= 0) {
                                        onEvent(AncEvent.LabBatchCursorMoved(itemIndex))
                                        onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ENTER))
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.testName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                if (item.unit.isNotBlank()) {
                                    Text(item.unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (item.result.isBlank()) {
                                    LabResultPill(
                                        text = stringResource(R.string.lab_batch_pending),
                                        status = LabResultPillStatus.PENDING,
                                    )
                                } else {
                                    Text(item.result, fontWeight = FontWeight.ExtraBold)
                                    LabResultPill(
                                        text = if (item.isAbnormal) "Abnormal ▲" else "Normal ✓",
                                        status = if (item.isAbnormal) LabResultPillStatus.ABNORMAL else LabResultPillStatus.NORMAL,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        val itemIndex = draft.items.indexOf(item)
                                        if (itemIndex >= 0) {
                                            onEvent(AncEvent.LabBatchCursorMoved(itemIndex))
                                            onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ENTER))
                                        }
                                    },
                                    modifier = Modifier.testTag("labBatchEdit-" + item.labId),
                                ) {
                                    Text(stringResource(R.string.lab_batch_edit))
                                }
                            }
                        }
                        if (index < section.items.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        val emptyQualitative = draft.items.filter { it.result.isBlank() && (it.unit.isBlank() || it.category.contains("Urine", true)) }
        if (emptyQualitative.isNotEmpty()) {
            TextButton(
                onClick = {
                    emptyQualitative.forEach { item ->
                        val defaultValue = if (item.testName.lowercase().contains("protein")) "Normal" else "Negative"
                        onEvent(AncEvent.LabBatchResultChanged(item.labId, defaultValue))
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("labBatchMarkNormal"),
            ) {
                Text(stringResource(R.string.lab_batch_mark_empty_normal, emptyQualitative.size), fontWeight = FontWeight.Bold)
            }
        }
    }
}
