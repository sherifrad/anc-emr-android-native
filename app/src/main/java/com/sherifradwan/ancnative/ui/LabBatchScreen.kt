package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.model.codex.LabCodexItem
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.LabBatchDraft
import com.sherifradwan.ancnative.state.LabBatchItem
import com.sherifradwan.ancnative.state.LabBatchTab
import java.util.UUID

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

    Column(Modifier.fillMaxSize().testTag("labBatchScreen")) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onEvent(AncEvent.CancelLabBatch) }, modifier = Modifier.testTag("labBatchBack")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column {
                Text("Laboratory results", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                Text(
                    patient.displayName,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        BatchHeader(draft, onEvent)

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BatchTab("Add", LabBatchTab.ADD, draft, onEvent)
            BatchTab("Enter ${draft.filledCount}/${draft.items.size}", LabBatchTab.ENTER, draft, onEvent)
            BatchTab("Review ${draft.filledCount}", LabBatchTab.REVIEW, draft, onEvent)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (draft.tab) {
                LabBatchTab.ADD -> AddTab(codexGroups, codexItems, draft, onEvent)
                LabBatchTab.ENTER -> EnterTab(draft, onEvent)
                LabBatchTab.REVIEW -> ReviewTab(draft, onEvent)
            }
        }

        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onEvent(AncEvent.CancelLabBatch) }, modifier = Modifier.testTag("labBatchCancel")) {
                Text("Cancel")
            }
            Button(
                onClick = { onEvent(AncEvent.SubmitLabBatch) },
                enabled = draft.filledCount > 0,
                modifier = Modifier.weight(1f).testTag("labBatchSave"),
            ) {
                Text(if (draft.filledCount == 1) "Save 1 result" else "Save ${draft.filledCount} results")
            }
        }
    }
}

@Composable
private fun BatchHeader(draft: LabBatchDraft, onEvent: (AncEvent) -> Unit) {
    var date by remember(draft.date) { mutableStateOf(draft.date) }
    var lab by remember(draft.labName) { mutableStateOf(draft.labName.orEmpty()) }
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = date,
            onValueChange = { date = it; onEvent(AncEvent.LabBatchDateChanged(it)) },
            label = { Text("Date") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("labBatchDate"),
        )
        OutlinedTextField(
            value = lab,
            onValueChange = { lab = it; onEvent(AncEvent.LabBatchLabNameChanged(it.ifBlank { null })) },
            label = { Text("Laboratory") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("labBatchLabName"),
        )
    }
}

@Composable
private fun BatchTab(label: String, tab: LabBatchTab, draft: LabBatchDraft, onEvent: (AncEvent) -> Unit) {
    FilterChip(
        selected = draft.tab == tab,
        onClick = { onEvent(AncEvent.LabBatchTabSelected(tab)) },
        label = { Text(label) },
        modifier = Modifier.testTag("labBatchTab-" + tab.name),
    )
}

@Composable
private fun AddTab(
    groups: List<com.sherifradwan.ancnative.model.codex.LabCategoryGroup>,
    codexItems: List<LabCodexItem>,
    draft: LabBatchDraft,
    onEvent: (AncEvent) -> Unit,
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
    LazyColumn(state = rememberLazyListState(), modifier = Modifier.fillMaxSize().testTag("labBatchAddList")) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search a test") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp).testTag("labBatchSearch"),
            )
        }
        val visibleGroups = groups.filter { query.isBlank() || it.name.contains(query, true) || it.testNames.any { n -> n.contains(query, true) } }
        items(visibleGroups, key = { it.id }) { group ->
            val available = itemsFor(group.testNames)
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(group.name, fontWeight = FontWeight.Bold)
                        Text(group.testNames.size.toString() + " elements", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { onEvent(AncEvent.LabBatchItemsAdded(available)) },
                        enabled = available.isNotEmpty(),
                        modifier = Modifier.testTag("labBatchAdd-" + group.id),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add ${available.size}")
                    }
                }
            }
        }
        if (query.isNotBlank()) {
            val matches = codexItems.filter { it.testName.contains(query, true) }.take(20)
            items(matches, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.testName, Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                onEvent(
                                    AncEvent.LabBatchItemsAdded(
                                        listOf(LabBatchItem(UUID.randomUUID().toString(), item.testName, item.category, item.unit)),
                                    ),
                                )
                            },
                            modifier = Modifier.testTag("labBatchAddOne-" + item.id),
                        ) { Text("Add") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun EnterTab(draft: LabBatchDraft, onEvent: (AncEvent) -> Unit) {
    val item = draft.currentItem
    if (item == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Add tests first") }
        return
    }
    Column(Modifier.fillMaxSize().padding(12.dp).testTag("labBatchEnter")) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            draft.items.forEachIndexed { index, row ->
                Box(
                    Modifier.weight(1f).height(4.dp).background(
                        when {
                            index < draft.cursor -> MaterialTheme.colorScheme.primary
                            index == draft.cursor -> MaterialTheme.colorScheme.secondary
                            row.isFilled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                        RoundedCornerShape(2.dp),
                    ),
                )
            }
        }
        Text(
            (item.category.ifBlank { "Test" }) + " · Test " + (draft.cursor + 1) + " of " + draft.items.size,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(item.testName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (item.unit.isNotBlank()) Text(item.unit, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = item.result,
            onValueChange = { onEvent(AncEvent.LabBatchResultChanged(item.labId, it)) },
            label = { Text("Result") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("labBatchResult"),
        )
        OutlinedTextField(
            value = item.referenceRange,
            onValueChange = { onEvent(AncEvent.LabBatchRangeChanged(item.labId, it)) },
            label = { Text("Reference range (from the report)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("labBatchRange"),
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Abnormal", Modifier.weight(1f))
            Switch(
                checked = item.isAbnormal,
                onCheckedChange = { onEvent(AncEvent.LabBatchFlagChanged(item.labId, it)) },
                modifier = Modifier.testTag("labBatchFlag"),
            )
        }
        OutlinedTextField(
            value = item.notes,
            onValueChange = { onEvent(AncEvent.LabBatchNotesChanged(item.labId, it)) },
            label = { Text("Note") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("labBatchNote"),
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onEvent(AncEvent.LabBatchCursorMoved(draft.cursor - 1)) },
                enabled = draft.cursor > 0,
                modifier = Modifier.weight(1f).testTag("labBatchPrev"),
            ) { Text("Back") }
            Button(
                onClick = {
                    if (draft.cursor < draft.items.lastIndex) onEvent(AncEvent.LabBatchCursorMoved(draft.cursor + 1))
                    else onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.REVIEW))
                },
                modifier = Modifier.weight(1f).testTag("labBatchNext"),
            ) { Text(if (draft.cursor < draft.items.lastIndex) "Next" else "Review") }
        }
    }
}

@Composable
private fun ReviewTab(draft: LabBatchDraft, onEvent: (AncEvent) -> Unit) {
    LazyColumn(state = rememberLazyListState(), modifier = Modifier.fillMaxSize().testTag("labBatchReview")) {
        items(draft.items, key = { it.labId }) { item ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.testName, fontWeight = FontWeight.SemiBold)
                        Text(item.unit.ifBlank { item.category }, style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.result.isBlank()) {
                        Text("pending", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(item.result + if (item.isAbnormal) " ▲" else "", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { onEvent(AncEvent.LabBatchCursorMoved(draft.items.indexOf(item))); onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ENTER)) },
                        modifier = Modifier.testTag("labBatchEdit-" + item.labId),
                    ) { Text("Edit") }
                }
            }
        }
        item {
            val empty = draft.items.count { it.result.isBlank() }
            if (empty > 0) {
                TextButton(
                    onClick = {
                        draft.items.filter { it.result.isBlank() && it.unit.isBlank() }
                            .forEach { onEvent(AncEvent.LabBatchResultChanged(it.labId, "Negative")) }
                    },
                    modifier = Modifier.padding(12.dp).testTag("labBatchMarkNormal"),
                ) { Text("Mark $empty empty as normal") }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
