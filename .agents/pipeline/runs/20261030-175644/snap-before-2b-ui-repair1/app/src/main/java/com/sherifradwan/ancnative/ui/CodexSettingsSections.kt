package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.codex.LabCategoryGroup
import com.sherifradwan.ancnative.model.codex.LabCodexDelegate
import com.sherifradwan.ancnative.model.codex.LabCodexItem
import com.sherifradwan.ancnative.model.codex.MedicationCodexDelegate
import com.sherifradwan.ancnative.model.codex.MedicationCodexItem

enum class LabCodexSubView {
    INDIVIDUAL_TESTS,
    CATEGORY_GROUPS,
}

@Composable
fun LabCodexSettingsSection(
    codexDelegate: LabCodexDelegate,
    modifier: Modifier = Modifier,
) {
    val items by codexDelegate.labCodexItems.collectAsState()
    val categoryGroups by codexDelegate.labCategoryGroups.collectAsState()
    var currentSubView by remember { mutableStateOf(LabCodexSubView.INDIVIDUAL_TESTS) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddTestDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<LabCodexItem?>(null) }
    var groupToDelete by remember { mutableStateOf<LabCategoryGroup?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.testName.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredGroups = remember(categoryGroups, searchQuery) {
        if (searchQuery.isBlank()) categoryGroups
        else categoryGroups.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.testNames.any { name -> name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section Header & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "Laboratory Codex Directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${items.size} tests · ${categoryGroups.size} clinical panels",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (currentSubView == LabCodexSubView.INDIVIDUAL_TESTS) {
                    Button(
                        onClick = { showAddTestDialog = true },
                        modifier = Modifier.testTag("addNewLabTestBtn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Test", maxLines = 1, softWrap = false)
                    }
                } else {
                    Button(
                        onClick = { showAddGroupDialog = true },
                        modifier = Modifier.testTag("addNewLabGroupBtn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Panel", maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // Subview Switcher Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = currentSubView == LabCodexSubView.INDIVIDUAL_TESTS,
                onClick = { currentSubView = LabCodexSubView.INDIVIDUAL_TESTS },
                label = { Text("Individual Tests (${items.size})") },
                modifier = Modifier.testTag("labIndividualTestsTab"),
            )
            FilterChip(
                selected = currentSubView == LabCodexSubView.CATEGORY_GROUPS,
                onClick = { currentSubView = LabCodexSubView.CATEGORY_GROUPS },
                label = { Text("Panels & Profiles (${categoryGroups.size})") },
                modifier = Modifier.testTag("labCategoryGroupsTab"),
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    if (currentSubView == LabCodexSubView.INDIVIDUAL_TESTS) "Search tests or categories..."
                    else "Search diagnostic panels..."
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("searchLabCodexField"),
        )

        if (currentSubView == LabCodexSubView.INDIVIDUAL_TESTS) {
            // Tests List
            if (filteredItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching results for: $searchQuery"
                            else "Lab Codex directory is currently empty.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredItems.forEach { lab ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth().testTag("labCodexItem-${lab.id}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = lab.testName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                        ) {
                                            Text(
                                                text = lab.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }

                                        if (lab.unit.isNotBlank() || lab.referenceRange.isNotBlank()) {
                                            Text(
                                                text = listOf(lab.referenceRange, lab.unit).filter { it.isNotBlank() }.joinToString(" "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        if (lab.defaultResult.isNotBlank()) {
                                            Text(
                                                text = "· ${lab.defaultResult}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { itemToDelete = lab },
                                    modifier = Modifier.testTag("deleteLabCodexBtn-${lab.id}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Test",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Category Groups (Panels) List
            if (filteredGroups.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No groups matching: $searchQuery"
                            else "No registered panels or profiles.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredGroups.forEach { group ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth().testTag("labCategoryGroup-${group.id}"),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = group.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (group.description.isNotBlank()) {
                                            Text(
                                                text = group.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { groupToDelete = group },
                                        modifier = Modifier.testTag("deleteLabGroupBtn-${group.id}"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Panel",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }

                                // Tests included in this category group
                                Text(
                                    text = "Included Tests (${group.testNames.size}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    group.testNames.forEach { tName ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                        ) {
                                            Text(
                                                text = tName,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reset to Defaults Button
        TextButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset Clinic Lab Codex to Defaults")
        }
    }

    // Add Lab Test Dialog
    if (showAddTestDialog) {
        AddLabTestDialog(
            onDismiss = { showAddTestDialog = false },
            onSave = { newItem ->
                codexDelegate.addLabCodexItem(newItem)
                showAddTestDialog = false
            },
        )
    }

    // Add Category Group Dialog
    if (showAddGroupDialog) {
        AddLabCategoryGroupDialog(
            availableTests = items,
            onDismiss = { showAddGroupDialog = false },
            onSave = { newGroup ->
                codexDelegate.addLabCategoryGroup(newGroup)
                showAddGroupDialog = false
            },
        )
    }

    // Delete Confirmation Dialog for Test
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirm Delete Test") },
            text = { Text("Are you sure you want to permanently delete '${itemToDelete?.testName}' from clinic lab directory?") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { codexDelegate.deleteLabCodexItem(it.id) }
                        itemToDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Delete Confirmation Dialog for Group
    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Confirm Delete Diagnostic Panel") },
            text = { Text("Are you sure you want to permanently delete panel '${groupToDelete?.name}' from clinic directory?") },
            confirmButton = {
                Button(
                    onClick = {
                        groupToDelete?.let { codexDelegate.deleteLabCategoryGroup(it.id) }
                        groupToDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Reset Defaults Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Default Lab Codex") },
            text = { Text("This will restore the clinic lab catalog and panels back to standard antenatal clinical care defaults.") },
            confirmButton = {
                Button(
                    onClick = {
                        codexDelegate.resetLabDefaults()
                        codexDelegate.resetLabCategoryGroupDefaults()
                        showResetDialog = false
                    },
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun AddLabTestDialog(
    onDismiss: () -> Unit,
    onSave: (LabCodexItem) -> Unit,
) {
    var testName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ClinicalVocabulary.testCategories.first()) }
    var unit by remember { mutableStateOf("") }
    var referenceRange by remember { mutableStateOf("") }
    var defaultResult by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Lab Test to Codex") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = testName,
                    onValueChange = { testName = it },
                    label = { Text("Test Name (Required)") },
                    placeholder = { Text("e.g. Complete Blood Count or TSH") },
                    modifier = Modifier.fillMaxWidth().testTag("addLabNameField"),
                    singleLine = true,
                )

                Text(
                    text = "Category:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ClinicalVocabulary.testCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("g/dL") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = referenceRange,
                        onValueChange = { referenceRange = it },
                        label = { Text("Reference Range") },
                        placeholder = { Text("11.0 - 15.0") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                    )
                }

                OutlinedTextField(
                    value = defaultResult,
                    onValueChange = { defaultResult = it },
                    label = { Text("Default Result / Value (Optional)") },
                    placeholder = { Text("e.g. Negative or Nil") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (testName.trim().isBlank()) {
                        errorMessage = "Please enter test name!"
                    } else {
                        onSave(
                            LabCodexItem(
                                testName = testName.trim(),
                                category = selectedCategory,
                                unit = unit.trim(),
                                referenceRange = referenceRange.trim(),
                                defaultResult = defaultResult.trim(),
                                isCustom = true,
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("saveLabCodexConfirmBtn"),
            ) {
                Text("Save to Codex")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AddLabCategoryGroupDialog(
    availableTests: List<LabCodexItem>,
    onDismiss: () -> Unit,
    onSave: (LabCategoryGroup) -> Unit,
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    val selectedTests = remember { mutableStateOf(setOf<String>()) }
    var testSearch by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredTests = remember(availableTests, testSearch) {
        if (testSearch.isBlank()) availableTests
        else availableTests.filter { it.testName.contains(testSearch, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Diagnostic Panel") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Panel Name (Required)") },
                    placeholder = { Text("e.g. Hypertension Follow-up or Diabetes Screening") },
                    modifier = Modifier.fillMaxWidth().testTag("addLabGroupNameField"),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = groupDescription,
                    onValueChange = { groupDescription = it },
                    label = { Text("Description / Indication (Optional)") },
                    placeholder = { Text("e.g. Panel requested at 28 weeks gestation") },
                    modifier = Modifier.fillMaxWidth().testTag("addLabGroupDescField"),
                    singleLine = true,
                )

                Text(
                    text = "Select Tests to Include (${selectedTests.value.size} selected):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                OutlinedTextField(
                    value = testSearch,
                    onValueChange = { testSearch = it },
                    placeholder = { Text("Search available tests...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Multi-select test chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    filteredTests.forEach { test ->
                        val isSelected = selectedTests.value.contains(test.testName)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val current = selectedTests.value.toMutableSet()
                                if (isSelected) {
                                    current.remove(test.testName)
                                } else {
                                    current.add(test.testName)
                                }
                                selectedTests.value = current
                            },
                            label = { Text(test.testName, fontSize = 12.sp) },
                            modifier = Modifier.testTag("selectTestChip-${test.id}"),
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.trim().isBlank()) {
                        errorMessage = "Please enter panel name!"
                    } else if (selectedTests.value.isEmpty()) {
                        errorMessage = "Please select at least one test!"
                    } else {
                        onSave(
                            LabCategoryGroup(
                                name = groupName.trim(),
                                description = groupDescription.trim(),
                                testNames = selectedTests.value.toList(),
                                isCustom = true,
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("saveLabGroupConfirmBtn"),
            ) {
                Text("Save Panel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun MedicationCodexSettingsSection(
    codexDelegate: MedicationCodexDelegate,
    modifier: Modifier = Modifier,
) {
    val items by codexDelegate.medicationCodexItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<MedicationCodexItem?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.dosage.contains(searchQuery, ignoreCase = true) ||
                    it.instructions.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section Header & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "Medication Codex & Protocols",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${items.size} registered medications · available for all patient plans",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("addNewMedBtn"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Medication", maxLines = 1, softWrap = false)
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search medications or dosages...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("searchMedCodexField"),
        )

        // Medications List
        if (filteredItems.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching results for: $searchQuery"
                        else "Medication Codex directory is currently empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredItems.forEach { med ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth().testTag("medCodexItem-${med.id}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = med.displayLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            text = med.route,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ) {
                                        Text(
                                            text = med.frequency,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }

                                    if (med.isProphylaxis) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                        ) {
                                            Text(
                                                text = "Prophylactic",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }

                                if (med.instructions.isNotBlank()) {
                                    Text(
                                        text = med.instructions,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            IconButton(
                                onClick = { itemToDelete = med },
                                modifier = Modifier.testTag("deleteMedCodexBtn-${med.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Medication",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reset to Defaults Button
        TextButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset Clinic Medication Codex to Defaults")
        }
    }

    // Add Medication Dialog
    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                codexDelegate.addMedicationCodexItem(newItem)
                showAddDialog = false
            },
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirm Delete Medication") },
            text = { Text("Are you sure you want to permanently delete '${itemToDelete?.displayLabel}' from clinic medication directory?") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { codexDelegate.deleteMedicationCodexItem(it.id) }
                        itemToDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Reset Defaults Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Default Medication Codex") },
            text = { Text("This will restore the medication catalog to standard clinical maternal care guidelines.") },
            confirmButton = {
                Button(
                    onClick = {
                        codexDelegate.resetMedicationDefaults()
                        showResetDialog = false
                    },
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onSave: (MedicationCodexItem) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedRoute by remember { mutableStateOf(ClinicalVocabulary.medicationRoutes.first()) }
    var frequency by remember { mutableStateOf("Once daily") }
    var isProphylaxis by remember { mutableStateOf(true) }
    var instructions by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val commonFrequencies = listOf("Once daily", "Twice daily", "Three times daily", "At bedtime", "As needed (PRN)")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Medication to Codex") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Medication Name (Required)") },
                        placeholder = { Text("e.g. Folic Acid") },
                        modifier = Modifier.weight(1.5f).testTag("addMedNameField"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosage") },
                        placeholder = { Text("5 mg") },
                        modifier = Modifier.weight(1f).testTag("addMedDosageField"),
                        singleLine = true,
                    )
                }

                Text(
                    text = "Administration Route:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ClinicalVocabulary.medicationRoutes.forEach { r ->
                        FilterChip(
                            selected = selectedRoute == r,
                            onClick = { selectedRoute = r },
                            label = { Text(r, fontSize = 12.sp) },
                        )
                    }
                }

                Text(
                    text = "Frequency:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    commonFrequencies.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq, fontSize = 11.sp) },
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Pregnancy Prophylactic",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = isProphylaxis,
                        onCheckedChange = { isProphylaxis = it },
                    )
                }

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions / Clinical Indication") },
                    placeholder = { Text("e.g. For neural tube defect prevention") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        errorMessage = "Please enter medication name!"
                    } else {
                        onSave(
                            MedicationCodexItem(
                                name = name.trim(),
                                dosage = dosage.trim(),
                                route = selectedRoute,
                                frequency = frequency.trim(),
                                isProphylaxis = isProphylaxis,
                                instructions = instructions.trim(),
                                isCustom = true,
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("saveMedCodexConfirmBtn"),
            ) {
                Text("Save to Codex")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
