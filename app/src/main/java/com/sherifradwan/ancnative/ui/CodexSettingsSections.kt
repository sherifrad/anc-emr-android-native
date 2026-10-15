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
            Column {
                Text(
                    text = "دليل الفحوصات والتحاليل الطبية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${items.size} فحصاً · ${categoryGroups.size} باقات/مجموعات سريرية",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
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
                        Text("إضافة فحص", maxLines = 1, softWrap = false)
                    }
                } else {
                    Button(
                        onClick = { showAddGroupDialog = true },
                        modifier = Modifier.testTag("addNewLabGroupBtn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إنشاء باقة/مجموعة", maxLines = 1, softWrap = false)
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
                label = { Text("الفحوصات الفردية (${items.size})") },
                modifier = Modifier.testTag("labIndividualTestsTab"),
            )
            FilterChip(
                selected = currentSubView == LabCodexSubView.CATEGORY_GROUPS,
                onClick = { currentSubView = LabCodexSubView.CATEGORY_GROUPS },
                label = { Text("مجموعات وباقات التحاليل (${categoryGroups.size})") },
                modifier = Modifier.testTag("labCategoryGroupsTab"),
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    if (currentSubView == LabCodexSubView.INDIVIDUAL_TESTS) "بحث في الفحوصات أو التصنيفات..."
                    else "بحث في مجموعات وباقات التحاليل..."
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
                            text = if (searchQuery.isNotBlank()) "لا توجد نتائج تطابق: $searchQuery"
                            else "دليل التحاليل فارغ حالياً.",
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
                                        contentDescription = "حذف الفحص",
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
                            text = if (searchQuery.isNotBlank()) "لا توجد مجموعات تطابق: $searchQuery"
                            else "لا توجد باقات أو مجموعات مسجلة.",
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
                                            contentDescription = "حذف المجموعة",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }

                                // Tests included in this category group
                                Text(
                                    text = "الفحوصات المتضمنة (${group.testNames.size}):",
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
            Text("استعادة التحاليل والباقات الافتراضية للعيادة")
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
            title = { Text("تأكيد حذف الفحص من الدليل") },
            text = { Text("هل أنت متأكد من حذف فحص '${itemToDelete?.testName}' نهائياً من دليل التحاليل العام للعيادة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { codexDelegate.deleteLabCodexItem(it.id) }
                        itemToDelete = null
                    },
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("إلغاء")
                }
            },
        )
    }

    // Delete Confirmation Dialog for Group
    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("تأكيد حذف مجموعة التحاليل") },
            text = { Text("هل أنت متأكد من حذف باقة '${groupToDelete?.name}' نهائياً من دليل التحاليل للعيادة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        groupToDelete?.let { codexDelegate.deleteLabCategoryGroup(it.id) }
                        groupToDelete = null
                    },
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("إلغاء")
                }
            },
        )
    }

    // Reset Defaults Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("استعادة الفحوصات والباقات الافتراضية") },
            text = { Text("سيؤدي هذا إلى إعادة ضبط دليل التحاليل ومجموعات التحاليل إلى القوائم الطبية القياسية الشائعة في عيادة الحوامل.") },
            confirmButton = {
                Button(
                    onClick = {
                        codexDelegate.resetLabDefaults()
                        codexDelegate.resetLabCategoryGroupDefaults()
                        showResetDialog = false
                    },
                ) {
                    Text("استعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء")
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
        title = { Text("إضافة فحص مختبري جديد للدليل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = testName,
                    onValueChange = { testName = it },
                    label = { Text("اسم الفحص (مطلوب)") },
                    placeholder = { Text("مثال: Complete Blood Count أو TSH") },
                    modifier = Modifier.fillMaxWidth().testTag("addLabNameField"),
                    singleLine = true,
                )

                Text(
                    text = "تصنيف الفحص:",
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
                        label = { Text("الوحدة") },
                        placeholder = { Text("g/dL") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = referenceRange,
                        onValueChange = { referenceRange = it },
                        label = { Text("النطاق الطبيعي") },
                        placeholder = { Text("11.0 - 15.0") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                    )
                }

                OutlinedTextField(
                    value = defaultResult,
                    onValueChange = { defaultResult = it },
                    label = { Text("نتيجة افتراضية / ملاحظة (اختياري)") },
                    placeholder = { Text("مثال: Negative أو Nil") },
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
                        errorMessage = "يرجى إدخال اسم الفحص الطبي!"
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
                Text("حفظ في الدليل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
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
        title = { Text("إنشاء باقة / مجموعة تحاليل جديدة") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("اسم الباقة / المجموعة (مطلوب)") },
                    placeholder = { Text("مثال: فحوصات متابعة الضغط أو مسح السكري") },
                    modifier = Modifier.fillMaxWidth().testTag("addLabGroupNameField"),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = groupDescription,
                    onValueChange = { groupDescription = it },
                    label = { Text("الوصف أو دواعي الطلب (اختياري)") },
                    placeholder = { Text("مثال: باقة تحاليل تُطلب في الأسبوع 28") },
                    modifier = Modifier.fillMaxWidth().testTag("addLabGroupDescField"),
                    singleLine = true,
                )

                Text(
                    text = "اختر الفحوصات المتضمنة في الباقة (${selectedTests.value.size} محدد):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                OutlinedTextField(
                    value = testSearch,
                    onValueChange = { testSearch = it },
                    placeholder = { Text("بحث في الفحوصات المتاحة...") },
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
                        errorMessage = "يرجى إدخال اسم المجموعة أو الباقة!"
                    } else if (selectedTests.value.isEmpty()) {
                        errorMessage = "يرجى اختيار فحص واحد على الأقل للمجموعة!"
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
                Text("حفظ الباقة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
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
            Column {
                Text(
                    text = "دليل الأدوية والبروتوكولات العلاجية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${items.size} دواء مسجلاً · متاح لخطط جميع المريضات",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("addNewMedBtn"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة دواء", maxLines = 1, softWrap = false)
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث في الأدوية أو الجرعات...") },
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
                        text = if (searchQuery.isNotBlank()) "لا توجد نتائج تطابق: $searchQuery"
                        else "دليل الأدوية فارغ حالياً.",
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
                                                text = "وقائي للحمل",
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
                                    contentDescription = "حذف الدواء",
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
            Text("استعادة الأدوية الافتراضية للعيادة")
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
            title = { Text("تأكيد حذف الدواء من الدليل") },
            text = { Text("هل أنت متأكد من حذف دواء '${itemToDelete?.displayLabel}' نهائياً من دليل الأدوية العام للعيادة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { codexDelegate.deleteMedicationCodexItem(it.id) }
                        itemToDelete = null
                    },
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("إلغاء")
                }
            },
        )
    }

    // Reset Defaults Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("استعادة الأدوية الافتراضية") },
            text = { Text("سيؤدي هذا إلى إعادة ضبط دليل الأدوية إلى قائمة الأدوية القياسية الموصى بها في رعاية الأمومة.") },
            confirmButton = {
                Button(
                    onClick = {
                        codexDelegate.resetMedicationDefaults()
                        showResetDialog = false
                    },
                ) {
                    Text("استعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء")
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
        title = { Text("إضافة دواء جديد للدليل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الدواء (مطلوب)") },
                        placeholder = { Text("مثال: Folic Acid") },
                        modifier = Modifier.weight(1.5f).testTag("addMedNameField"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("الجرعة") },
                        placeholder = { Text("5 mg") },
                        modifier = Modifier.weight(1f).testTag("addMedDosageField"),
                        singleLine = true,
                    )
                }

                Text(
                    text = "طريق الإعطاء:",
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
                    text = "التكرار اليومي:",
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
                        text = "علاج وقائي للحمل (Prophylactic)",
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
                    label = { Text("تعليمات الاستخدام أو دواعي الاستعمال") },
                    placeholder = { Text("مثال: للوقاية من تشوهات الأنبوب العصبي") },
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
                        errorMessage = "يرجى إدخال اسم الدواء!"
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
                Text("حفظ في الدليل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}
