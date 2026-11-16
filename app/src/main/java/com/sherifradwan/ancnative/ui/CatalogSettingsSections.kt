package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.catalog.LabCatalogDelegate
import com.sherifradwan.ancnative.model.catalog.LabCatalogItem
import com.sherifradwan.ancnative.model.catalog.LabCategoryGroup
import com.sherifradwan.ancnative.model.catalog.MedicationCatalogDelegate
import com.sherifradwan.ancnative.model.catalog.MedicationCatalogItem
import com.sherifradwan.ancnative.ui.components.BandChip
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.FormFieldLabel
import com.sherifradwan.ancnative.ui.components.LocalFieldFamily
import com.sherifradwan.ancnative.ui.components.PlainField
import com.sherifradwan.ancnative.ui.components.RecordBandCard
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.patienthub.components.SheetCancelSaveRow

enum class LabCatalogSubView {
    INDIVIDUAL_TESTS,
    CATEGORY_GROUPS,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabCatalogSettingsSection(
    catalogDelegate: LabCatalogDelegate,
    modifier: Modifier = Modifier,
) {
    val items by catalogDelegate.labCatalogItems.collectAsState()
    val categoryGroups by catalogDelegate.labCategoryGroups.collectAsState()
    var currentSubView by remember { mutableStateOf(LabCatalogSubView.INDIVIDUAL_TESTS) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddTestDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<LabCatalogItem?>(null) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<LabCategoryGroup?>(null) }
    var itemToDelete by remember { mutableStateOf<LabCatalogItem?>(null) }
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

    val bgFamily = FieldFamilies.background()
    val testsSummary = pluralStringResource(R.plurals.catalog_panel_tests_count, items.size, items.size)
    val panelsSummary = pluralStringResource(R.plurals.catalog_panels_count, categoryGroups.size, categoryGroups.size)

    CompositionLocalProvider(LocalFieldFamily provides bgFamily) {
        // SecuritySettingsDialog owns the outer verticalScroll; keep lists as plain Columns.
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatalogSectionHeader(
                title = stringResource(R.string.catalog_lab_section_title),
                summary = stringResource(R.string.catalog_lab_summary, testsSummary, panelsSummary),
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RecordFilterChip(
                    selected = currentSubView == LabCatalogSubView.INDIVIDUAL_TESTS,
                    label = pluralStringResource(R.plurals.catalog_tab_individual_tests, items.size, items.size),
                    onClick = { currentSubView = LabCatalogSubView.INDIVIDUAL_TESTS },
                    tag = "labIndividualTestsTab",
                    wrap = true,
                    minTouchTarget = true,
                )
                RecordFilterChip(
                    selected = currentSubView == LabCatalogSubView.CATEGORY_GROUPS,
                    label = pluralStringResource(R.plurals.catalog_tab_panels, categoryGroups.size, categoryGroups.size),
                    onClick = { currentSubView = LabCatalogSubView.CATEGORY_GROUPS },
                    tag = "labCategoryGroupsTab",
                    wrap = true,
                    minTouchTarget = true,
                )
            }

            if (currentSubView == LabCatalogSubView.INDIVIDUAL_TESTS) {
                CatalogPrimaryButton(
                    label = stringResource(R.string.add_test_action),
                    tag = "addNewLabTestBtn",
                    onClick = { showAddTestDialog = true },
                )
            } else {
                CatalogPrimaryButton(
                    label = stringResource(R.string.catalog_create_panel),
                    tag = "addNewLabGroupBtn",
                    onClick = { showAddGroupDialog = true },
                )
            }

            CatalogSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(
                    if (currentSubView == LabCatalogSubView.INDIVIDUAL_TESTS) R.string.catalog_search_lab_tests
                    else R.string.catalog_search_panels,
                ),
                tag = "searchLabCodexField",
                clearTag = "clearSearchLabCodexBtn",
            )

            if (currentSubView == LabCatalogSubView.INDIVIDUAL_TESTS) {
                if (filteredItems.isEmpty()) {
                    CatalogListEmptyOrNoMatch(
                        searchQuery = searchQuery,
                        noMatchTitle = stringResource(R.string.catalog_no_tests_match, searchQuery),
                        onClearSearch = { searchQuery = "" },
                        clearSearchTag = "clearLabSearchAction",
                        emptyTitle = stringResource(R.string.catalog_empty_labs_title),
                        emptyHint = stringResource(R.string.catalog_empty_labs_hint),
                        emptyPrimaryLabel = stringResource(R.string.add_test_action),
                        onEmptyPrimary = { showAddTestDialog = true },
                        emptyPrimaryTag = "emptyAddLabTestBtn",
                        onEmptyReset = { showResetDialog = true },
                        emptyResetTag = "emptyResetLabTestsBtn",
                    )
                } else {
                    val cardTitle = if (searchQuery.isBlank()) {
                        stringResource(R.string.catalog_tests_list_title)
                    } else {
                        pluralStringResource(R.plurals.catalog_tests_matches_title, filteredItems.size, filteredItems.size)
                    }
                    RecordSectionCard(title = cardTitle, family = bgFamily) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            filteredItems.forEachIndexed { index, lab ->
                                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                CatalogListRow(
                                    title = lab.testName,
                                    primaryMeta = lab.category,
                                    secondaryMeta = labTestMetaLine(lab),
                                    itemTag = "labCodexItem-${lab.id}",
                                    editTag = "editLabCodexBtn-${lab.id}",
                                    editCd = stringResource(R.string.catalog_edit_test_cd, lab.testName),
                                    onEdit = { itemToEdit = lab },
                                    deleteTag = "deleteLabCodexBtn-${lab.id}",
                                    deleteCd = stringResource(R.string.delete_test, lab.testName),
                                    onDelete = { itemToDelete = lab },
                                )
                            }
                        }
                    }
                }
            } else if (filteredGroups.isEmpty()) {
                CatalogListEmptyOrNoMatch(
                    searchQuery = searchQuery,
                    noMatchTitle = stringResource(R.string.catalog_no_panels_match, searchQuery),
                    onClearSearch = { searchQuery = "" },
                    clearSearchTag = "clearLabSearchAction",
                    emptyTitle = stringResource(R.string.catalog_empty_panels_title),
                    emptyHint = stringResource(R.string.catalog_empty_panels_hint),
                    emptyPrimaryLabel = stringResource(R.string.catalog_create_panel),
                    onEmptyPrimary = { showAddGroupDialog = true },
                    emptyPrimaryTag = "emptyCreateLabGroupBtn",
                    onEmptyReset = { showResetDialog = true },
                    emptyResetTag = "emptyResetLabGroupsBtn",
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredGroups.forEach { group ->
                        RecordBandCard(
                            title = group.name,
                            family = bgFamily,
                            subtitle = group.description.takeIf { it.isNotBlank() },
                            modifier = Modifier.testTag("labCategoryGroup-${group.id}"),
                            chips = {
                                BandChip(
                                    text = pluralStringResource(
                                        R.plurals.catalog_panel_tests_count,
                                        group.testNames.size,
                                        group.testNames.size,
                                    ),
                                    tone = null,
                                )
                            },
                            actions = {
                                CatalogRowActions(
                                    editTag = "editLabGroupBtn-${group.id}",
                                    editCd = stringResource(R.string.edit_diagnostic_panel),
                                    onEdit = { groupToEdit = group },
                                    deleteTag = "deleteLabGroupBtn-${group.id}",
                                    deleteCd = stringResource(R.string.catalog_delete_panel_cd, group.name),
                                    onDelete = { groupToDelete = group },
                                )
                            },
                        ) {
                            PlainField(
                                label = stringResource(R.string.catalog_included_tests),
                                value = group.testNames.joinToString(", "),
                            )
                        }
                    }
                }
            }

            CatalogResetButton(
                label = stringResource(R.string.catalog_reset_labs),
                tag = "resetLabCatalogBtn",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { showResetDialog = true },
            )
        }
    }

    if (showAddTestDialog) {
        AddLabTestDialog(
            onDismiss = { showAddTestDialog = false },
            onSave = { newItem ->
                catalogDelegate.addLabCatalogItem(newItem).also { if (it) showAddTestDialog = false }
            },
        )
    }

    if (itemToEdit != null) {
        AddLabTestDialog(
            initial = itemToEdit,
            onDismiss = { itemToEdit = null },
            onSave = { editedItem ->
                catalogDelegate.updateLabCatalogItem(editedItem).also { if (it) itemToEdit = null }
            },
        )
    }

    if (showAddGroupDialog) {
        AddLabCategoryGroupDialog(
            availableTests = items,
            onDismiss = { showAddGroupDialog = false },
            onSave = { newGroup ->
                catalogDelegate.addLabCategoryGroup(newGroup).also { if (it) showAddGroupDialog = false }
            },
        )
    }

    if (groupToEdit != null) {
        AddLabCategoryGroupDialog(
            initial = groupToEdit,
            availableTests = items,
            onDismiss = { groupToEdit = null },
            onSave = { editedGroup ->
                catalogDelegate.updateLabCategoryGroup(editedGroup).also { if (it) groupToEdit = null }
            },
        )
    }

    if (itemToDelete != null) {
        CatalogConfirmDialog(
            title = stringResource(R.string.catalog_delete_test_title),
            body = stringResource(R.string.catalog_delete_test_body, itemToDelete?.testName.orEmpty()),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                itemToDelete?.let { catalogDelegate.deleteLabCatalogItem(it.id) }
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null },
            dialogTag = "deleteLabConfirmDialog",
            confirmTag = "deleteLabConfirmBtn",
            dismissTag = "cancelDeleteLabBtn",
        )
    }

    if (groupToDelete != null) {
        CatalogConfirmDialog(
            title = stringResource(R.string.catalog_delete_panel_title),
            body = stringResource(R.string.catalog_delete_panel_body, groupToDelete?.name.orEmpty()),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                groupToDelete?.let { catalogDelegate.deleteLabCategoryGroup(it.id) }
                groupToDelete = null
            },
            onDismiss = { groupToDelete = null },
            dialogTag = "deleteLabGroupConfirmDialog",
            confirmTag = "deleteLabGroupConfirmBtn",
            dismissTag = "cancelDeleteLabGroupBtn",
        )
    }

    if (showResetDialog) {
        CatalogConfirmDialog(
            title = stringResource(R.string.catalog_reset_labs_title),
            body = stringResource(R.string.catalog_reset_labs_body),
            confirmLabel = stringResource(R.string.catalog_reset_action),
            onConfirm = {
                catalogDelegate.resetLabDefaults()
                catalogDelegate.resetLabCategoryGroupDefaults()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false },
            dialogTag = "resetLabConfirmDialog",
            confirmTag = "resetLabConfirmBtn",
            dismissTag = "cancelResetLabBtn",
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddLabTestDialog(
    initial: LabCatalogItem? = null,
    onDismiss: () -> Unit,
    onSave: (LabCatalogItem) -> Boolean,
) {
    var testName by remember(initial) { mutableStateOf(initial?.testName ?: "") }
    var selectedCategory by remember(initial) {
        mutableStateOf(
            initial?.category?.takeIf { it.isNotBlank() } ?: ClinicalVocabulary.testCategories.first(),
        )
    }
    var unit by remember(initial) { mutableStateOf(initial?.unit ?: "") }
    var referenceRange by remember(initial) { mutableStateOf(initial?.referenceRange ?: "") }
    var defaultResult by remember(initial) { mutableStateOf(initial?.defaultResult ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val narrowStacked = LocalDensity.current.fontScale > 1.3f

    val blankError = stringResource(R.string.catalog_error_test_name_blank)
    val duplicateError = stringResource(R.string.catalog_error_test_duplicate)
    val saveError = stringResource(R.string.catalog_error_test_save)

    CatalogFormDialog(
        title = stringResource(if (initial != null) R.string.catalog_edit_lab_test_title else R.string.catalog_add_lab_test_title),
        confirmLabel = stringResource(R.string.save),
        confirmTag = "saveLabCodexConfirmBtn",
        cancelTag = "cancelLabCodexBtn",
        dialogTag = "addLabTestDialog",
        errorMessage = errorMessage,
        errorTag = "addLabError",
        onDismiss = onDismiss,
        onConfirm = {
            val trimmed = testName.trim()
            if (trimmed.isBlank()) {
                errorMessage = blankError
            } else {
                val itemToSave = if (initial != null) {
                    initial.copy(
                        testName = trimmed,
                        category = selectedCategory,
                        unit = unit,
                        referenceRange = referenceRange,
                        defaultResult = defaultResult,
                    )
                } else {
                    LabCatalogItem(
                        testName = trimmed,
                        category = selectedCategory,
                        unit = unit.trim(),
                        referenceRange = referenceRange.trim(),
                        defaultResult = defaultResult.trim(),
                        isCustom = true,
                    )
                }
                if (!onSave(itemToSave)) {
                    errorMessage = if (initial != null) duplicateError else saveError
                }
            }
        },
    ) {
        CatalogLabeledField(
            value = testName,
            onValueChange = {
                testName = it
                errorMessage = null
            },
            label = stringResource(R.string.catalog_test_name_required),
            placeholder = stringResource(R.string.catalog_test_name_placeholder),
            isError = errorMessage != null && testName.isBlank(),
            tag = "addLabNameField",
        )

        CatalogChipGroup(label = stringResource(R.string.test_category)) {
            ClinicalVocabulary.testCategories.forEachIndexed { index, cat ->
                RecordFilterChip(
                    selected = selectedCategory == cat,
                    label = cat,
                    onClick = { selectedCategory = cat },
                    tag = "labCodexCategoryChoice-$index",
                    wrap = true,
                    minTouchTarget = true,
                )
            }
        }

        CatalogFieldPair(
            narrowStacked = narrowStacked,
            firstWeight = 1f,
            secondWeight = 1.5f,
            first = { fieldModifier ->
                CatalogLabeledField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = stringResource(R.string.unit),
                    placeholder = stringResource(R.string.catalog_unit_placeholder),
                    tag = "addLabUnitField",
                    modifier = fieldModifier,
                )
            },
            second = { fieldModifier ->
                CatalogLabeledField(
                    value = referenceRange,
                    onValueChange = { referenceRange = it },
                    label = stringResource(R.string.reference_range),
                    placeholder = stringResource(R.string.catalog_ref_range_placeholder),
                    tag = "addLabRefRangeField",
                    modifier = fieldModifier,
                )
            },
        )

        CatalogLabeledField(
            value = defaultResult,
            onValueChange = { defaultResult = it },
            label = stringResource(R.string.catalog_default_result_optional),
            placeholder = stringResource(R.string.catalog_default_result_placeholder),
            tag = "addLabDefaultResultField",
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddLabCategoryGroupDialog(
    initial: LabCategoryGroup? = null,
    availableTests: List<LabCatalogItem>,
    onDismiss: () -> Unit,
    onSave: (LabCategoryGroup) -> Boolean,
) {
    var groupName by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var groupDescription by remember(initial) { mutableStateOf(initial?.description ?: "") }
    val selectedTests = remember(initial, availableTests) {
        mutableStateOf(
            initial?.testNames.orEmpty().map { raw ->
                availableTests.firstOrNull { it.testName.trim().equals(raw.trim(), ignoreCase = true) }?.testName ?: raw.trim()
            }.toSet(),
        )
    }
    var testSearch by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val allSelectableTests = remember(availableTests, initial) {
        val existingNames = availableTests.map { it.testName.trim().lowercase() }.toSet()
        val legacy = initial?.testNames.orEmpty()
            .filter { it.trim().lowercase() !in existingNames }
            .map { LabCatalogItem(id = "custom-$it", testName = it, category = "Other", isCustom = true) }
        availableTests + legacy
    }

    val filteredTests = remember(allSelectableTests, testSearch) {
        if (testSearch.isBlank()) allSelectableTests
        else allSelectableTests.filter { it.testName.contains(testSearch, ignoreCase = true) }
    }

    val blankError = stringResource(R.string.catalog_error_panel_name_blank)
    val noTestsError = stringResource(R.string.catalog_error_panel_no_tests)
    val duplicateError = stringResource(R.string.catalog_error_panel_duplicate)
    val saveError = stringResource(R.string.catalog_error_panel_save)

    CatalogFormDialog(
        title = stringResource(if (initial != null) R.string.edit_diagnostic_panel else R.string.create_diagnostic_panel),
        confirmLabel = stringResource(if (initial != null) R.string.save else R.string.catalog_save_panel),
        confirmTag = "saveLabGroupConfirmBtn",
        cancelTag = "cancelLabGroupBtn",
        dialogTag = "addLabGroupDialog",
        errorMessage = errorMessage,
        errorTag = "addLabGroupError",
        onDismiss = onDismiss,
        onConfirm = {
            val trimmed = groupName.trim()
            if (trimmed.isBlank()) {
                errorMessage = blankError
            } else if (selectedTests.value.isEmpty()) {
                errorMessage = noTestsError
            } else {
                val groupToSave = if (initial != null) {
                    initial.copy(
                        name = trimmed,
                        description = groupDescription.trim(),
                        testNames = selectedTests.value.toList(),
                        isCustom = true,
                    )
                } else {
                    LabCategoryGroup(
                        name = trimmed,
                        description = groupDescription.trim(),
                        testNames = selectedTests.value.toList(),
                        isCustom = true,
                    )
                }
                if (!onSave(groupToSave)) {
                    errorMessage = if (initial != null) duplicateError else saveError
                }
            }
        },
    ) {
        CatalogLabeledField(
            value = groupName,
            onValueChange = {
                groupName = it
                errorMessage = null
            },
            label = stringResource(R.string.catalog_panel_name_required),
            placeholder = stringResource(R.string.catalog_panel_name_placeholder),
            isError = errorMessage != null && groupName.isBlank(),
            tag = "addLabGroupNameField",
        )

        CatalogLabeledField(
            value = groupDescription,
            onValueChange = { groupDescription = it },
            label = stringResource(R.string.catalog_panel_desc_optional),
            placeholder = stringResource(R.string.catalog_panel_desc_placeholder),
            tag = "addLabGroupDescField",
        )

        FormFieldLabel(stringResource(R.string.catalog_select_tests_count, selectedTests.value.size))

        CatalogSearchField(
            query = testSearch,
            onQueryChange = { testSearch = it },
            placeholder = stringResource(R.string.catalog_search_available_tests),
            tag = "searchLabGroupTestsField",
            clearTag = "clearLabGroupTestsSearchBtn",
        )

        if (filteredTests.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.catalog_no_available_tests_match, testSearch),
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { testSearch = "" },
                    shape = RecordShapes.Small,
                    modifier = Modifier.minimumTouchTarget().testTag("clearLabGroupTestsEmptySearchBtn"),
                ) {
                    Text(
                        text = stringResource(R.string.clear_search),
                        style = RecordType.Action,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                filteredTests.forEach { test ->
                    val isSelected = selectedTests.value.contains(test.testName)
                    RecordFilterChip(
                        selected = isSelected,
                        label = test.testName,
                        onClick = {
                            val current = selectedTests.value.toMutableSet()
                            if (isSelected) current.remove(test.testName) else current.add(test.testName)
                            selectedTests.value = current
                        },
                        tag = "selectTestChip-${test.id}",
                        wrap = true,
                        minTouchTarget = true,
                    )
                }
            }
        }
    }
}

@Composable
fun MedicationCatalogSettingsSection(
    catalogDelegate: MedicationCatalogDelegate,
    modifier: Modifier = Modifier,
) {
    val items by catalogDelegate.medicationCatalogItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MedicationCatalogItem?>(null) }
    var itemToDelete by remember { mutableStateOf<MedicationCatalogItem?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.dosage.contains(searchQuery, ignoreCase = true) ||
                it.instructions.contains(searchQuery, ignoreCase = true)
        }
    }

    val bgFamily = FieldFamilies.background()

    CompositionLocalProvider(LocalFieldFamily provides bgFamily) {
        // SecuritySettingsDialog owns the outer verticalScroll; keep lists as plain Columns.
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatalogSectionHeader(
                title = stringResource(R.string.catalog_med_section_title),
                summary = pluralStringResource(R.plurals.catalog_med_summary, items.size, items.size),
            )

            CatalogPrimaryButton(
                label = stringResource(R.string.add_medication),
                tag = "addNewMedBtn",
                onClick = { showAddDialog = true },
            )

            CatalogSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.catalog_search_meds),
                tag = "searchMedCodexField",
                clearTag = "clearSearchMedCodexBtn",
            )

            if (filteredItems.isEmpty()) {
                CatalogListEmptyOrNoMatch(
                    searchQuery = searchQuery,
                    noMatchTitle = stringResource(R.string.catalog_no_meds_match, searchQuery),
                    onClearSearch = { searchQuery = "" },
                    clearSearchTag = "clearMedSearchAction",
                    emptyTitle = stringResource(R.string.catalog_empty_meds_title),
                    emptyHint = stringResource(R.string.catalog_empty_meds_hint),
                    emptyPrimaryLabel = stringResource(R.string.add_medication),
                    onEmptyPrimary = { showAddDialog = true },
                    emptyPrimaryTag = "emptyAddMedBtn",
                    onEmptyReset = { showResetDialog = true },
                    emptyResetTag = "emptyResetMedsBtn",
                )
            } else {
                val cardTitle = if (searchQuery.isBlank()) {
                    stringResource(R.string.catalog_meds_list_title)
                } else {
                    pluralStringResource(R.plurals.catalog_meds_matches_title, filteredItems.size, filteredItems.size)
                }
                RecordSectionCard(title = cardTitle, family = bgFamily) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filteredItems.forEachIndexed { index, med ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            CatalogListRow(
                                title = med.displayLabel,
                                primaryMeta = stringResource(
                                    R.string.catalog_med_route_frequency,
                                    med.route,
                                    medicationFrequencyLabel(med.frequency),
                                ),
                                secondaryMeta = med.instructions.takeIf { it.isNotBlank() },
                                itemTag = "medCodexItem-${med.id}",
                                editTag = "editMedCodexBtn-${med.id}",
                                editCd = stringResource(R.string.edit_medication_description, med.displayLabel),
                                onEdit = { itemToEdit = med },
                                deleteTag = "deleteMedCodexBtn-${med.id}",
                                deleteCd = stringResource(R.string.catalog_delete_med_cd, med.displayLabel),
                                onDelete = { itemToDelete = med },
                            )
                        }
                    }
                }
            }

            CatalogResetButton(
                label = stringResource(R.string.catalog_reset_meds),
                tag = "resetMedCatalogBtn",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { showResetDialog = true },
            )
        }
    }

    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                catalogDelegate.addMedicationCatalogItem(newItem).also { if (it) showAddDialog = false }
            },
        )
    }

    if (itemToEdit != null) {
        AddMedicationDialog(
            initial = itemToEdit,
            onDismiss = { itemToEdit = null },
            onSave = { editedItem ->
                catalogDelegate.updateMedicationCatalogItem(editedItem).also { if (it) itemToEdit = null }
            },
        )
    }

    if (itemToDelete != null) {
        CatalogConfirmDialog(
            title = stringResource(R.string.catalog_delete_med_title),
            body = stringResource(R.string.catalog_delete_med_body, itemToDelete?.displayLabel.orEmpty()),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                itemToDelete?.let { catalogDelegate.deleteMedicationCatalogItem(it.id) }
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null },
            dialogTag = "deleteMedConfirmDialog",
            confirmTag = "deleteMedConfirmBtn",
            dismissTag = "cancelDeleteMedBtn",
        )
    }

    if (showResetDialog) {
        CatalogConfirmDialog(
            title = stringResource(R.string.catalog_reset_meds_title),
            body = stringResource(R.string.catalog_reset_meds_body),
            confirmLabel = stringResource(R.string.catalog_reset_action),
            onConfirm = {
                catalogDelegate.resetMedicationDefaults()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false },
            dialogTag = "resetMedConfirmDialog",
            confirmTag = "resetMedConfirmBtn",
            dismissTag = "cancelResetMedBtn",
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMedicationDialog(
    initial: MedicationCatalogItem? = null,
    onDismiss: () -> Unit,
    onSave: (MedicationCatalogItem) -> Boolean,
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var dosage by remember(initial) { mutableStateOf(initial?.dosage ?: "") }
    var selectedRoute by remember(initial) {
        mutableStateOf(
            initial?.route?.takeIf { it.isNotBlank() } ?: ClinicalVocabulary.medicationRoutes.first(),
        )
    }
    var frequency by remember(initial) {
        mutableStateOf(initial?.frequency?.takeIf { it.isNotBlank() } ?: "Once daily")
    }
    var instructions by remember(initial) { mutableStateOf(initial?.instructions ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val narrowStacked = LocalDensity.current.fontScale > 1.3f

    val frequencies = remember(initial) {
        (COMMON_MEDICATION_FREQUENCIES + listOfNotNull(initial?.frequency?.takeIf { it.isNotBlank() })).distinct()
    }

    val blankError = stringResource(R.string.catalog_error_med_name_blank)
    val duplicateError = stringResource(R.string.catalog_error_med_duplicate)
    val saveError = stringResource(R.string.catalog_error_med_save)

    CatalogFormDialog(
        title = stringResource(if (initial != null) R.string.edit_medication_title else R.string.add_medication),
        confirmLabel = stringResource(if (initial != null) R.string.save else R.string.catalog_add_to_my_list),
        confirmTag = "saveMedCodexConfirmBtn",
        cancelTag = "cancelMedCodexBtn",
        dialogTag = "addMedDialog",
        errorMessage = errorMessage,
        errorTag = "addMedError",
        onDismiss = onDismiss,
        onConfirm = {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                errorMessage = blankError
            } else {
                val itemToSave = if (initial != null) {
                    initial.copy(
                        name = trimmedName,
                        dosage = dosage.trim(),
                        route = selectedRoute,
                        frequency = frequency.trim(),
                        instructions = instructions.trim(),
                        isCustom = true,
                    )
                } else {
                    MedicationCatalogItem(
                        name = trimmedName,
                        dosage = dosage.trim(),
                        route = selectedRoute,
                        frequency = frequency.trim(),
                        isProphylaxis = false,
                        instructions = instructions.trim(),
                        isCustom = true,
                    )
                }
                if (!onSave(itemToSave)) {
                    errorMessage = if (initial != null) duplicateError else saveError
                }
            }
        },
    ) {
        CatalogFieldPair(
            narrowStacked = narrowStacked,
            firstWeight = 1.5f,
            secondWeight = 1f,
            first = { fieldModifier ->
                CatalogLabeledField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = stringResource(R.string.catalog_med_name_required),
                    placeholder = stringResource(R.string.catalog_med_name_placeholder),
                    isError = errorMessage != null && name.isBlank(),
                    tag = "addMedNameField",
                    modifier = fieldModifier,
                )
            },
            second = { fieldModifier ->
                CatalogLabeledField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = stringResource(R.string.dosage),
                    placeholder = stringResource(R.string.catalog_med_dosage_placeholder),
                    tag = "addMedDosageField",
                    modifier = fieldModifier,
                )
            },
        )

        CatalogChipGroup(label = stringResource(R.string.catalog_med_route_label)) {
            ClinicalVocabulary.medicationRoutes.forEachIndexed { index, r ->
                RecordFilterChip(
                    selected = selectedRoute == r,
                    label = r,
                    onClick = { selectedRoute = r },
                    tag = "medCodexRouteChoice-$index",
                    wrap = true,
                    minTouchTarget = true,
                )
            }
        }

        CatalogChipGroup(label = stringResource(R.string.frequency)) {
            frequencies.forEachIndexed { index, freq ->
                RecordFilterChip(
                    selected = frequency == freq,
                    label = medicationFrequencyLabel(freq),
                    onClick = { frequency = freq },
                    tag = "medCodexFreqChoice-$index",
                    wrap = true,
                    minTouchTarget = true,
                )
            }
        }

        CatalogLabeledField(
            value = instructions,
            onValueChange = { instructions = it },
            label = stringResource(R.string.catalog_med_instructions_optional),
            placeholder = stringResource(R.string.catalog_med_instructions_placeholder),
            tag = "addMedInstructionsField",
        )
    }
}

@Composable
private fun CatalogSectionHeader(title: String, summary: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = RecordType.SectionTitle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = summary, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CatalogPrimaryButton(label: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RecordShapes.Small,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag(tag),
    ) {
        Text(text = label, style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CatalogResetButton(label: String, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        shape = RecordShapes.Small,
        modifier = modifier.minimumTouchTarget().testTag(tag),
    ) {
        Text(text = label, style = RecordType.Action, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CatalogListEmptyOrNoMatch(
    searchQuery: String,
    noMatchTitle: String,
    onClearSearch: () -> Unit,
    clearSearchTag: String,
    emptyTitle: String,
    emptyHint: String,
    emptyPrimaryLabel: String,
    onEmptyPrimary: () -> Unit,
    emptyPrimaryTag: String,
    onEmptyReset: () -> Unit,
    emptyResetTag: String,
) {
    if (searchQuery.isNotBlank()) {
        CatalogEmptyCard(
            title = noMatchTitle,
            body = stringResource(R.string.catalog_search_no_match_hint),
            primaryLabel = stringResource(R.string.clear_search),
            onPrimaryClick = onClearSearch,
            primaryTag = clearSearchTag,
        )
    } else {
        CatalogEmptyCard(
            title = emptyTitle,
            body = emptyHint,
            primaryLabel = emptyPrimaryLabel,
            onPrimaryClick = onEmptyPrimary,
            primaryTag = emptyPrimaryTag,
            secondaryLabel = stringResource(R.string.catalog_reset_to_defaults_action),
            onSecondaryClick = onEmptyReset,
            secondaryTag = emptyResetTag,
        )
    }
}

@Composable
private fun CatalogListRow(
    title: String,
    primaryMeta: String,
    secondaryMeta: String?,
    itemTag: String,
    editTag: String,
    editCd: String,
    onEdit: () -> Unit,
    deleteTag: String,
    deleteCd: String,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag(itemTag),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FormFieldLabel(primaryMeta)
                if (!secondaryMeta.isNullOrBlank()) {
                    Text(text = secondaryMeta, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            CatalogRowActions(
                editTag = editTag,
                editCd = editCd,
                onEdit = onEdit,
                deleteTag = deleteTag,
                deleteCd = deleteCd,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun CatalogRowActions(
    editTag: String,
    editCd: String,
    onEdit: () -> Unit,
    deleteTag: String,
    deleteCd: String,
    onDelete: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onEdit, modifier = Modifier.minimumTouchTarget().testTag(editTag)) {
            Icon(Icons.Default.Edit, contentDescription = editCd, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.minimumTouchTarget().testTag(deleteTag)) {
            Icon(Icons.Default.Delete, contentDescription = deleteCd, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CatalogFieldPair(
    narrowStacked: Boolean,
    firstWeight: Float,
    secondWeight: Float,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    if (narrowStacked) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            first(Modifier)
            second(Modifier)
        }
    } else {
        // Bottom alignment keeps input boxes aligned even if one label wraps.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            first(Modifier.weight(firstWeight))
            second(Modifier.weight(secondWeight))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogChipGroup(label: String, content: @Composable FlowRowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FormFieldLabel(label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun CatalogDialogError(message: String?, tag: String) {
    if (message != null) {
        Text(
            text = message,
            style = RecordType.Action,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun labTestMetaLine(lab: LabCatalogItem): String? {
    val rangeText = lab.referenceRange.trim()
    val unitText = lab.unit.trim()
    val rangeOrUnit = when {
        rangeText.isNotEmpty() && unitText.isNotEmpty() -> "$rangeText $unitText"
        rangeText.isNotEmpty() -> rangeText
        unitText.isNotEmpty() -> stringResource(R.string.catalog_test_unit_line, unitText)
        else -> null
    }
    val defaultText = lab.defaultResult.trim()
        .takeIf { it.isNotEmpty() }
        ?.let { stringResource(R.string.catalog_test_default_line, it) }
    return when {
        rangeOrUnit != null && defaultText != null -> stringResource(R.string.catalog_test_meta_pair, rangeOrUnit, defaultText)
        else -> rangeOrUnit ?: defaultText
    }
}

@Composable
private fun CatalogFormDialog(
    title: String,
    confirmLabel: String,
    confirmTag: String,
    cancelTag: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogTag: String? = null,
    errorMessage: String? = null,
    errorTag: String = "",
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp - 32).dp
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = maxHeight)
                .let { if (dialogTag != null) it.testTag(dialogTag) else it },
            shape = RecordShapes.Large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            CompositionLocalProvider(LocalFieldFamily provides FieldFamilies.background()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = RecordType.SectionTitle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).semantics { heading() },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (errorMessage != null) {
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                            CatalogDialogError(message = errorMessage, tag = errorTag)
                        }
                    }
                    SheetCancelSaveRow(
                        enabled = true,
                        saving = false,
                        saveEnabled = true,
                        savingLabel = confirmLabel,
                        onCancel = onDismiss,
                        onSave = onConfirm,
                        cancelTag = cancelTag,
                        saveTag = confirmTag,
                        spinnerTag = "${confirmTag}Spinner",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogLabeledField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    tag: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.clearAndSetSemantics {}) {
            if (isError) {
                Text(text = label, style = RecordType.FieldLabel, color = MaterialTheme.colorScheme.error)
            } else {
                FormFieldLabel(label)
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let {
                { Text(text = it, style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            isError = isError,
            singleLine = singleLine,
            shape = RecordShapes.Small,
            textStyle = RecordType.FieldValue,
            colors = editorFieldColors(),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }.testTag(tag),
        )
    }
}

@Composable
private fun CatalogSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    tag: String,
    clearTag: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = placeholder, style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = if (query.isNotBlank()) {
            {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.minimumTouchTarget().testTag(clearTag)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RecordShapes.Small,
        textStyle = RecordType.FieldValue,
        colors = editorFieldColors(),
        modifier = modifier.fillMaxWidth().testTag(tag),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogEmptyCard(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryTag: String? = null,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    secondaryTag: String? = null,
) {
    OutlinedCard(
        shape = RecordShapes.Large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface)
            Text(text = body, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onPrimaryClick,
                    shape = RecordShapes.Small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.minimumTouchTarget().let { if (primaryTag != null) it.testTag(primaryTag) else it },
                ) {
                    Text(text = primaryLabel, style = RecordType.Action, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                }
                if (secondaryLabel != null && onSecondaryClick != null) {
                    TextButton(
                        onClick = onSecondaryClick,
                        shape = RecordShapes.Small,
                        modifier = Modifier.minimumTouchTarget().let { if (secondaryTag != null) it.testTag(secondaryTag) else it },
                    ) {
                        Text(text = secondaryLabel, style = RecordType.Action, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dialogTag: String? = null,
    confirmTag: String? = null,
    dismissTag: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RecordShapes.Large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                style = RecordType.SectionTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(text = body, style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                shape = RecordShapes.Small,
                modifier = Modifier.minimumTouchTarget().let { if (confirmTag != null) it.testTag(confirmTag) else it },
            ) {
                Text(text = confirmLabel, style = RecordType.Action, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RecordShapes.Small,
                modifier = Modifier.minimumTouchTarget().let { if (dismissTag != null) it.testTag(dismissTag) else it },
            ) {
                Text(text = stringResource(R.string.cancel), style = RecordType.Action, color = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = if (dialogTag != null) Modifier.testTag(dialogTag) else Modifier,
    )
}
