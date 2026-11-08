package com.sherifradwan.ancnative.model.catalog

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ClinicalCatalogRepository internal constructor(
    private val prefs: SharedPreferences,
) : LabCatalogDelegate, MedicationCatalogDelegate, CustomChoicesDelegate {

    internal constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val customChoicesLock = Any()

    private val _labCatalogItems = MutableStateFlow<List<LabCatalogItem>>(emptyList())
    override val labCatalogItems: StateFlow<List<LabCatalogItem>> = _labCatalogItems.asStateFlow()

    private val _labCategoryGroups = MutableStateFlow<List<LabCategoryGroup>>(emptyList())
    override val labCategoryGroups: StateFlow<List<LabCategoryGroup>> = _labCategoryGroups.asStateFlow()

    private val _medicationCatalogItems = MutableStateFlow<List<MedicationCatalogItem>>(emptyList())
    override val medicationCatalogItems: StateFlow<List<MedicationCatalogItem>> = _medicationCatalogItems.asStateFlow()

    private val _customChoiceFlows: Map<CustomChoiceList, MutableStateFlow<List<CustomChoiceItem>>> =
        CustomChoiceList.entries.associateWith { MutableStateFlow<List<CustomChoiceItem>>(emptyList()) }

    init {
        loadCatalogData()
    }

    private fun loadCatalogData() {
        val loadedLabs = loadLabItems()
        _labCatalogItems.value = if (loadedLabs.isEmpty()) {
            val defaults = defaultLabCatalogItems()
            saveLabItems(defaults)
            defaults
        } else {
            val defaults = defaultLabCatalogItems()
            val customLabs = loadedLabs.filter { it.isCustom }
            val merged = defaults + customLabs
            saveLabItems(merged)
            merged
        }

        val loadedGroups = loadLabCategoryGroups()
        _labCategoryGroups.value = if (loadedGroups.isEmpty()) {
            val defaults = defaultLabCategoryGroups()
            saveLabCategoryGroups(defaults)
            defaults
        } else {
            val defaults = defaultLabCategoryGroups()
            val customGroups = loadedGroups.filter { it.isCustom }
            val merged = defaults + customGroups
            saveLabCategoryGroups(merged)
            merged
        }

        val loadedMeds = loadMedicationItems()
        _medicationCatalogItems.value = if (loadedMeds.isEmpty()) {
            val defaults = defaultMedicationCatalogItems()
            saveMedicationItems(defaults)
            defaults
        } else {
            loadedMeds
        }

        for (listKind in CustomChoiceList.entries) {
            _customChoiceFlows[listKind]?.value = loadCustomChoices(listKind)
        }
    }

    // ==========================================
    // Lab Catalog Delegate Implementation
    // ==========================================

    override fun addLabCatalogItem(item: LabCatalogItem): Boolean {
        if (item.testName.isBlank()) return false
        val current = _labCatalogItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.testName.equals(item.testName.trim(), ignoreCase = true) }
        val finalItem = item.copy(testName = item.testName.trim(), isCustom = true)
        if (existingIndex >= 0) {
            current[existingIndex] = finalItem
        } else {
            current.add(finalItem)
        }
        _labCatalogItems.value = current
        saveLabItems(current)
        return true
    }

    override fun updateLabCatalogItem(item: LabCatalogItem): Boolean {
        val edit = applyLabItemEdit(_labCatalogItems.value, _labCategoryGroups.value, item) ?: return false
        _labCatalogItems.value = edit.items
        _labCategoryGroups.value = edit.groups
        saveLabItems(edit.items)
        saveLabCategoryGroups(edit.groups)
        return true
    }

    override fun deleteLabCatalogItem(itemId: String): Boolean {
        val current = _labCatalogItems.value.toMutableList()
        val removed = current.removeAll { it.id == itemId }
        if (removed) {
            _labCatalogItems.value = current
            saveLabItems(current)
        }
        return removed
    }

    override fun getLabCatalogItem(itemId: String): LabCatalogItem? {
        return _labCatalogItems.value.firstOrNull { it.id == itemId }
    }

    override fun findLabCatalogItemByName(name: String): LabCatalogItem? {
        if (name.isBlank()) return null
        return _labCatalogItems.value.firstOrNull { it.testName.equals(name.trim(), ignoreCase = true) }
    }

    override fun resetLabDefaults() {
        val defaults = defaultLabCatalogItems()
        _labCatalogItems.value = defaults
        saveLabItems(defaults)
    }

    // ==========================================
    // Lab Category Groups (Panels) Implementation
    // ==========================================

    override fun addLabCategoryGroup(group: LabCategoryGroup): Boolean {
        if (group.name.isBlank()) return false
        val current = _labCategoryGroups.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.name.equals(group.name.trim(), ignoreCase = true) }
        val finalGroup = group.copy(name = group.name.trim(), isCustom = true)
        if (existingIndex >= 0) {
            current[existingIndex] = finalGroup
        } else {
            current.add(finalGroup)
        }
        _labCategoryGroups.value = current
        saveLabCategoryGroups(current)
        return true
    }

    override fun deleteLabCategoryGroup(groupId: String): Boolean {
        val current = _labCategoryGroups.value.toMutableList()
        val removed = current.removeAll { it.id == groupId }
        if (removed) {
            _labCategoryGroups.value = current
            saveLabCategoryGroups(current)
        }
        return removed
    }

    override fun getLabCategoryGroup(groupId: String): LabCategoryGroup? {
        return _labCategoryGroups.value.firstOrNull { it.id == groupId }
    }

    override fun resetLabCategoryGroupDefaults() {
        val defaults = defaultLabCategoryGroups()
        _labCategoryGroups.value = defaults
        saveLabCategoryGroups(defaults)
    }

    // ==========================================
    // Medication Catalog Delegate Implementation
    // ==========================================

    override fun addMedicationCatalogItem(item: MedicationCatalogItem): Boolean {
        if (item.name.isBlank()) return false
        val current = _medicationCatalogItems.value.toMutableList()
        val existingIndex = current.indexOfFirst {
            it.name.equals(item.name.trim(), ignoreCase = true) &&
                    it.dosage.equals(item.dosage.trim(), ignoreCase = true)
        }
        val finalItem = item.copy(name = item.name.trim(), dosage = item.dosage.trim(), isCustom = true)
        if (existingIndex >= 0) {
            current[existingIndex] = finalItem
        } else {
            current.add(finalItem)
        }
        _medicationCatalogItems.value = current
        saveMedicationItems(current)
        return true
    }

    override fun deleteMedicationCatalogItem(itemId: String): Boolean {
        val current = _medicationCatalogItems.value.toMutableList()
        val removed = current.removeAll { it.id == itemId }
        if (removed) {
            _medicationCatalogItems.value = current
            saveMedicationItems(current)
        }
        return removed
    }

    override fun getMedicationCatalogItem(itemId: String): MedicationCatalogItem? {
        return _medicationCatalogItems.value.firstOrNull { it.id == itemId }
    }

    override fun findMedicationCatalogItemByName(name: String): MedicationCatalogItem? {
        if (name.isBlank()) return null
        return _medicationCatalogItems.value.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
    }

    override fun resetMedicationDefaults() {
        val defaults = defaultMedicationCatalogItems()
        _medicationCatalogItems.value = defaults
        saveMedicationItems(defaults)
    }

    // ==========================================
    // Custom Choices Delegate Implementation
    // ==========================================

    override fun customChoices(list: CustomChoiceList): StateFlow<List<CustomChoiceItem>> {
        return _customChoiceFlows.getValue(list).asStateFlow()
    }

    override fun addCustomChoice(
        list: CustomChoiceList,
        label: String,
        existingDefaults: Collection<String>,
    ): CustomChoiceValidationResult = synchronized(customChoicesLock) {
        val flow = _customChoiceFlows.getValue(list)
        val currentLabels = flow.value.map { it.label } + existingDefaults
        val maxLength = maxCustomChoiceLength(list)
        val validation = validateCustomChoice(label, currentLabels, maxLength = maxLength)
        if (validation !is CustomChoiceValidationResult.Valid) return@synchronized validation

        val item = CustomChoiceItem(list = list, label = validation.sanitizedLabel, isCustom = true)
        val updated = flow.value + item
        flow.value = updated
        saveCustomChoices(list, updated)
        validation
    }

    override fun renameCustomChoice(
        list: CustomChoiceList,
        id: String,
        newLabel: String,
        existingDefaults: Collection<String>,
    ): CustomChoiceValidationResult = synchronized(customChoicesLock) {
        val flow = _customChoiceFlows.getValue(list)
        val item = flow.value.find { it.id == id }
            ?: return@synchronized CustomChoiceValidationResult.Invalid(CustomChoiceValidationResult.Invalid.Reason.NOT_FOUND)
        val otherLabels = flow.value.filter { it.id != id }.map { it.label } + existingDefaults
        val maxLength = maxCustomChoiceLength(list)
        val validation = validateCustomChoice(newLabel, otherLabels, maxLength = maxLength)
        if (validation !is CustomChoiceValidationResult.Valid) return@synchronized validation

        val updated = flow.value.map { if (it.id == id) it.copy(label = validation.sanitizedLabel) else it }
        flow.value = updated
        saveCustomChoices(list, updated)
        validation
    }

    override fun removeCustomChoice(list: CustomChoiceList, id: String): RemovedCustomChoice? = synchronized(customChoicesLock) {
        val flow = _customChoiceFlows.getValue(list)
        val index = flow.value.indexOfFirst { it.id == id }
        if (index < 0) return@synchronized null
        val item = flow.value[index]
        val updated = flow.value.filterIndexed { i, _ -> i != index }
        flow.value = updated
        saveCustomChoices(list, updated)
        RemovedCustomChoice(item, index)
    }

    override fun restoreCustomChoice(removed: RemovedCustomChoice): Boolean = synchronized(customChoicesLock) {
        val flow = _customChoiceFlows.getValue(removed.item.list)
        val current = flow.value
        if (current.any { it.id == removed.item.id || it.label.trim().equals(removed.item.label.trim(), ignoreCase = true) }) {
            return@synchronized false
        }
        val targetIndex = removed.originalIndex.coerceIn(0, current.size)
        val updated = current.toMutableList().apply { add(targetIndex, removed.item) }
        flow.value = updated
        saveCustomChoices(removed.item.list, updated)
        true
    }

    override fun clearCustomChoices(list: CustomChoiceList) = synchronized(customChoicesLock) {
        val flow = _customChoiceFlows.getValue(list)
        flow.value = emptyList()
        saveCustomChoices(list, emptyList())
    }

    override fun getAllCustomChoices(): Map<CustomChoiceList, List<CustomChoiceItem>> = synchronized(customChoicesLock) {
        _customChoiceFlows.mapValues { it.value.value }
    }

    override fun restoreAllCustomChoices(data: Map<CustomChoiceList, List<CustomChoiceItem>>) = synchronized(customChoicesLock) {
        for ((list, items) in data) {
            val flow = _customChoiceFlows[list] ?: continue
            flow.value = items
            saveCustomChoices(list, items)
        }
    }

    fun exportCustomChoicesForBackup(): Map<String, List<String>> = synchronized(customChoicesLock) {
        val map = mutableMapOf<String, List<String>>()
        for (listKind in CustomChoiceList.entries) {
            val items = _customChoiceFlows[listKind]?.value.orEmpty()
            if (items.isNotEmpty()) {
                map[listKind.storageKey] = items.map { it.label }
            }
        }
        map
    }

    fun restoreCustomChoicesFromBackup(data: Map<String, List<String>>) = synchronized(customChoicesLock) {
        for (listKind in CustomChoiceList.entries) {
            val labels = data[listKind.storageKey]
            if (labels != null) {
                val maxLen = maxCustomChoiceLength(listKind)
                val acceptedLabels = mutableListOf<String>()
                val items = mutableListOf<CustomChoiceItem>()
                for (rawLabel in labels) {
                    val validation = validateCustomChoice(rawLabel, acceptedLabels, maxLength = maxLen)
                    if (validation is CustomChoiceValidationResult.Valid) {
                        acceptedLabels.add(validation.sanitizedLabel)
                        items.add(CustomChoiceItem(list = listKind, label = validation.sanitizedLabel, isCustom = true))
                    }
                }
                val flow = _customChoiceFlows.getValue(listKind)
                flow.value = items
                saveCustomChoices(listKind, items)
            }
        }
    }

    // ==========================================
    // JSON Storage Helpers
    // ==========================================

    private fun loadLabItems(): List<LabCatalogItem> {
        val jsonString = prefs.getString(KEY_LAB_ITEMS, null)
            ?: prefs.getString(KEY_LAB_ITEMS_LEGACY, null)
            ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<LabCatalogItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LabCatalogItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        testName = obj.optString("testName", ""),
                        category = obj.optString("category", "Hematology"),
                        unit = obj.optString("unit", ""),
                        referenceRange = obj.optString("referenceRange", ""),
                        defaultResult = obj.optString("defaultResult", ""),
                        isCustom = obj.optBoolean("isCustom", false),
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLabItems(items: List<LabCatalogItem>) {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("testName", item.testName)
                put("category", item.category)
                put("unit", item.unit)
                put("referenceRange", item.referenceRange)
                put("defaultResult", item.defaultResult)
                put("isCustom", item.isCustom)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_LAB_ITEMS, jsonArray.toString()).apply()
    }

    private fun loadLabCategoryGroups(): List<LabCategoryGroup> {
        val jsonString = prefs.getString(KEY_LAB_GROUPS, null)
            ?: prefs.getString(KEY_LAB_GROUPS_LEGACY, null)
            ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<LabCategoryGroup>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val testNamesArray = obj.optJSONArray("testNames")
                val testNames = mutableListOf<String>()
                if (testNamesArray != null) {
                    for (j in 0 until testNamesArray.length()) {
                        testNames.add(testNamesArray.getString(j))
                    }
                }
                list.add(
                    LabCategoryGroup(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        description = obj.optString("description", ""),
                        testNames = testNames,
                        isCustom = obj.optBoolean("isCustom", false),
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLabCategoryGroups(groups: List<LabCategoryGroup>) {
        val jsonArray = JSONArray()
        for (group in groups) {
            val obj = JSONObject().apply {
                put("id", group.id)
                put("name", group.name)
                put("description", group.description)
                val testNamesArr = JSONArray()
                group.testNames.forEach { testNamesArr.put(it) }
                put("testNames", testNamesArr)
                put("isCustom", group.isCustom)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_LAB_GROUPS, jsonArray.toString()).apply()
    }

    private fun loadMedicationItems(): List<MedicationCatalogItem> {
        val jsonString = prefs.getString(KEY_MED_ITEMS, null)
            ?: prefs.getString(KEY_MED_ITEMS_LEGACY, null)
            ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<MedicationCatalogItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MedicationCatalogItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        dosage = obj.optString("dosage", ""),
                        route = obj.optString("route", "Oral"),
                        frequency = obj.optString("frequency", "Once daily"),
                        isProphylaxis = obj.optBoolean("isProphylaxis", true),
                        instructions = obj.optString("instructions", ""),
                        isCustom = obj.optBoolean("isCustom", false),
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveMedicationItems(items: List<MedicationCatalogItem>) {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("dosage", item.dosage)
                put("route", item.route)
                put("frequency", item.frequency)
                put("isProphylaxis", item.isProphylaxis)
                put("instructions", item.instructions)
                put("isCustom", item.isCustom)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_MED_ITEMS, jsonArray.toString()).apply()
    }

    private fun loadCustomChoices(list: CustomChoiceList): List<CustomChoiceItem> {
        val jsonString = prefs.getString(list.storageKey, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val listItems = mutableListOf<CustomChoiceItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                listItems.add(
                    CustomChoiceItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        list = list,
                        label = obj.optString("label", ""),
                        isCustom = obj.optBoolean("isCustom", true),
                    )
                )
            }
            listItems
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCustomChoices(list: CustomChoiceList, items: List<CustomChoiceItem>) {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("isCustom", item.isCustom)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(list.storageKey, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "anc_clinical_catalog_storage"
        private const val KEY_LAB_ITEMS = "catalog_lab_items"
        private const val KEY_LAB_ITEMS_LEGACY = "codex_lab_items"
        private const val KEY_LAB_GROUPS = "catalog_lab_groups"
        private const val KEY_LAB_GROUPS_LEGACY = "codex_lab_groups"
        private const val KEY_MED_ITEMS = "catalog_med_items"
        private const val KEY_MED_ITEMS_LEGACY = "codex_med_items"

        @Volatile
        private var instance: ClinicalCatalogRepository? = null

        fun getInstance(context: Context): ClinicalCatalogRepository {
            return instance ?: synchronized(this) {
                instance ?: ClinicalCatalogRepository(context.applicationContext).also { instance = it }
            }
        }

        /** CBC is six results, not one: Hb, Hct, MCV, MCH, WBC, PLT. Ranges are typed from each report. */
        private val CBC_ELEMENTS = listOf(
            "Haemoglobin (Hb)" to "g/dL",
            "Haematocrit (Hct)" to "%",
            "MCV" to "fL",
            "MCH" to "pg",
            "White Cell Count (WBC)" to "×10³/µL",
            "Platelets (PLT)" to "×10³/µL",
        )

        /** Urine routine is eight results, microscopy included (owner: pus cells and RBCs matter). */
        private val URINE_ELEMENTS = listOf(
            "Urine Protein",
            "Urine Glucose",
            "Urine Ketones",
            "Urine Blood",
            "Urine Nitrite",
            "Leukocyte Esterase",
            "Pus Cells",
            "RBCs (Urine)",
        )

        fun defaultLabCategoryGroups(): List<LabCategoryGroup> = listOf(
            LabCategoryGroup(
                id = "group_cbc",
                name = "CBC",
                description = "Haemoglobin, haematocrit, MCV, MCH, WBC, platelets",
                testNames = CBC_ELEMENTS.map { it.first },
                isCustom = false,
            ),
            LabCategoryGroup(
                id = "group_urine",
                name = "Urine Routine & Microscopic",
                description = "Dipstick chemistry with pus cells and RBCs",
                testNames = URINE_ELEMENTS,
                isCustom = false,
            ),
            LabCategoryGroup(
                id = "group_first_trimester",
                name = "First Trimester Routine Booking",
                description = "Mandatory baseline tests at initial antenatal intake",
                testNames = buildList {
                    addAll(CBC_ELEMENTS.map { it.first })
                    add("Blood Grouping & Rh Factor")
                    add("Fasting Blood Sugar (FBS)")
                    addAll(URINE_ELEMENTS)
                    add("Hepatitis B Surface Antigen (HBsAg)")
                    add("HIV 1 & 2 Antibody")
                    add("Rubella Antibody IgG")
                    add("Thyroid Stimulating Hormone (TSH)")
                },
                isCustom = false,
            ),
            LabCategoryGroup(
                id = "group_preeclampsia",
                name = "Preeclampsia Workup",
                description = "Renal and liver function evaluation and platelet surveillance",
                testNames = buildList {
                    addAll(CBC_ELEMENTS.map { it.first })
                    add("Serum Creatinine")
                    add("Serum Uric Acid")
                    add("Alanine Aminotransferase (ALT)")
                    add("Aspartate Aminotransferase (AST)")
                    addAll(URINE_ELEMENTS)
                },
                isCustom = false,
            ),
            LabCategoryGroup(
                id = "group_gdm",
                name = "GDM Screening (24-28 W)",
                description = "Oral glucose tolerance test and urinalysis at 24-28 weeks",
                testNames = buildList {
                    add("Fasting Blood Sugar (FBS)")
                    add("Oral Glucose Tolerance Test (OGTT 75g)")
                    addAll(URINE_ELEMENTS)
                },
                isCustom = false,
            ),
            LabCategoryGroup(
                id = "group_anemia",
                name = "Anemia Workup & Iron Stores",
                description = "Complete blood count and serum ferritin evaluation",
                testNames = buildList {
                    addAll(CBC_ELEMENTS.map { it.first })
                    add("Serum Ferritin")
                },
                isCustom = false,
            ),
            LabCategoryGroup(
                id = "group_torch",
                name = "Infection & Serology Screening",
                description = "Maternal viral serology and syphilis antibody screening",
                testNames = listOf(
                    "Hepatitis B Surface Antigen (HBsAg)",
                    "HIV 1 & 2 Antibody",
                    "Syphilis Serology (VDRL)",
                    "Rubella Antibody IgG",
                ),
                isCustom = false,
            ),
        )

        fun defaultLabCatalogItems(): List<LabCatalogItem> = listOf(
            LabCatalogItem("lab_cbc_hb", "Haemoglobin (Hb)", "Hematology", "g/dL", ""),
            LabCatalogItem("lab_cbc_hct", "Haematocrit (Hct)", "Hematology", "%", ""),
            LabCatalogItem("lab_cbc_mcv", "MCV", "Hematology", "fL", ""),
            LabCatalogItem("lab_cbc_mch", "MCH", "Hematology", "pg", ""),
            LabCatalogItem("lab_cbc_wbc", "White Cell Count (WBC)", "Hematology", "×10³/µL", ""),
            LabCatalogItem("lab_cbc_plt", "Platelets (PLT)", "Hematology", "×10³/µL", ""),
            LabCatalogItem("lab_blood_group", "Blood Grouping & Rh Factor", "Hematology", "", ""),
            LabCatalogItem("lab_fbs", "Fasting Blood Sugar (FBS)", "Biochemistry", "mg/dL", "70 - 95"),
            LabCatalogItem("lab_ogtt_75", "Oral Glucose Tolerance Test (OGTT 75g)", "Biochemistry", "mg/dL", "< 140"),
            LabCatalogItem("lab_urine_protein", "Urine Protein", "Urinalysis", "", ""),
            LabCatalogItem("lab_urine_glucose", "Urine Glucose", "Urinalysis", "", ""),
            LabCatalogItem("lab_urine_ketones", "Urine Ketones", "Urinalysis", "", ""),
            LabCatalogItem("lab_urine_blood", "Urine Blood", "Urinalysis", "", ""),
            LabCatalogItem("lab_urine_nitrite", "Urine Nitrite", "Urinalysis", "", ""),
            LabCatalogItem("lab_urine_le", "Leukocyte Esterase", "Urinalysis", "", ""),
            LabCatalogItem("lab_urine_pus", "Pus Cells", "Urinalysis", "", ""),
            LabCatalogItem("lab_urine_rbc", "RBCs (Urine)", "Urinalysis", "", ""),
            LabCatalogItem("lab_hbsag", "Hepatitis B Surface Antigen (HBsAg)", "Serology", "", "", "Negative"),
            LabCatalogItem("lab_hiv", "HIV 1 & 2 Antibody", "Serology", "", "", "Non-reactive"),
            LabCatalogItem("lab_vdrl", "Syphilis Serology (VDRL)", "Serology", "", "", "Non-reactive"),
            LabCatalogItem("lab_rubella", "Rubella Antibody IgG", "Serology", "", "", "Positive (Immune)"),
            LabCatalogItem("lab_tsh", "Thyroid Stimulating Hormone (TSH)", "Biochemistry", "mIU/L", "0.2 - 2.5"),
            LabCatalogItem("lab_ferritin", "Serum Ferritin", "Hematology", "ng/mL", "30 - 150"),
            LabCatalogItem("lab_creatinine", "Serum Creatinine", "Biochemistry", "mg/dL", "0.5 - 0.9"),
            LabCatalogItem("lab_uric_acid", "Serum Uric Acid", "Biochemistry", "mg/dL", "2.5 - 5.5"),
            LabCatalogItem("lab_alt", "Alanine Aminotransferase (ALT)", "Biochemistry", "U/L", "7 - 35"),
            LabCatalogItem("lab_ast", "Aspartate Aminotransferase (AST)", "Biochemistry", "U/L", "10 - 35"),
            LabCatalogItem("lab_urine_culture", "Urine Culture & Sensitivity", "Urinalysis", "", "", "No growth after 48 hrs"),
        )

        fun defaultMedicationCatalogItems(): List<MedicationCatalogItem> = listOf(
            MedicationCatalogItem("med_folic", "Folic Acid", "5 mg", "Oral", "Once daily", true, "Neural tube defect prophylaxis"),
            MedicationCatalogItem("med_iron", "Ferrous Sulfate", "200 mg", "Oral", "Once daily", true, "Maternal iron deficiency prevention"),
            MedicationCatalogItem("med_calcium", "Calcium Carbonate + D3", "500 mg / 400 IU", "Oral", "Twice daily", true, "Bone mineralization and preeclampsia prophylaxis"),
            MedicationCatalogItem("med_multivitamin", "Prenatal Multivitamins", "1 tablet", "Oral", "Once daily", true, "Comprehensive micronutrient support"),
            MedicationCatalogItem("med_aspirin", "Low-Dose Aspirin", "150 mg", "Oral", "Once daily at bedtime", true, "Preeclampsia risk reduction from 12 weeks"),
            MedicationCatalogItem("med_paracetamol", "Paracetamol", "500 mg", "Oral", "As needed (PRN)", false, "Analgesic & antipyretic for pregnancy"),
            MedicationCatalogItem("med_progesterone", "Micronized Progesterone", "200 mg", "Oral", "Once daily at bedtime", true, "Luteal phase support"),
            MedicationCatalogItem("med_enoxaparin", "Enoxaparin (LMWH)", "40 mg (4000 IU)", "Subcutaneous", "Once daily", true, "VTE thromboprophylaxis"),
            MedicationCatalogItem("med_labetalol", "Labetalol", "100 mg", "Oral", "Twice daily", false, "First-line antihypertensive in pregnancy"),
            MedicationCatalogItem("med_methyldopa", "Methyldopa", "250 mg", "Oral", "Three times daily", false, "Antihypertensive management"),
        )
    }
}
