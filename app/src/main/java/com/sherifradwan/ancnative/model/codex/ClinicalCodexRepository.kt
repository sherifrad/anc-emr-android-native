package com.sherifradwan.ancnative.model.codex

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ClinicalCodexRepository private constructor(context: Context) :
    LabCodexDelegate, MedicationCodexDelegate {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val _labCodexItems = MutableStateFlow<List<LabCodexItem>>(emptyList())
    override val labCodexItems: StateFlow<List<LabCodexItem>> = _labCodexItems.asStateFlow()

    private val _labCategoryGroups = MutableStateFlow<List<LabCategoryGroup>>(emptyList())
    override val labCategoryGroups: StateFlow<List<LabCategoryGroup>> = _labCategoryGroups.asStateFlow()

    private val _medicationCodexItems = MutableStateFlow<List<MedicationCodexItem>>(emptyList())
    override val medicationCodexItems: StateFlow<List<MedicationCodexItem>> = _medicationCodexItems.asStateFlow()

    init {
        loadCodexData()
    }

    private fun loadCodexData() {
        val loadedLabs = loadLabItems()
        _labCodexItems.value = if (loadedLabs.isEmpty()) {
            val defaults = defaultLabCodexItems()
            saveLabItems(defaults)
            defaults
        } else {
            loadedLabs
        }

        val loadedGroups = loadLabCategoryGroups()
        _labCategoryGroups.value = if (loadedGroups.isEmpty()) {
            val defaults = defaultLabCategoryGroups()
            saveLabCategoryGroups(defaults)
            defaults
        } else {
            loadedGroups
        }

        val loadedMeds = loadMedicationItems()
        _medicationCodexItems.value = if (loadedMeds.isEmpty()) {
            val defaults = defaultMedicationCodexItems()
            saveMedicationItems(defaults)
            defaults
        } else {
            loadedMeds
        }
    }

    // ==========================================
    // Lab Codex Delegate Implementation
    // ==========================================

    override fun addLabCodexItem(item: LabCodexItem): Boolean {
        if (item.testName.isBlank()) return false
        val current = _labCodexItems.value.toMutableList()
        // Check if item with same name exists
        val existingIndex = current.indexOfFirst { it.testName.equals(item.testName.trim(), ignoreCase = true) }
        val finalItem = item.copy(testName = item.testName.trim(), isCustom = true)
        if (existingIndex >= 0) {
            current[existingIndex] = finalItem
        } else {
            current.add(finalItem)
        }
        _labCodexItems.value = current
        saveLabItems(current)
        return true
    }

    override fun deleteLabCodexItem(itemId: String): Boolean {
        val current = _labCodexItems.value.toMutableList()
        val removed = current.removeAll { it.id == itemId }
        if (removed) {
            _labCodexItems.value = current
            saveLabItems(current)
        }
        return removed
    }

    override fun getLabCodexItem(itemId: String): LabCodexItem? {
        return _labCodexItems.value.firstOrNull { it.id == itemId }
    }

    override fun findLabCodexItemByName(name: String): LabCodexItem? {
        if (name.isBlank()) return null
        return _labCodexItems.value.firstOrNull { it.testName.equals(name.trim(), ignoreCase = true) }
    }

    override fun resetLabDefaults() {
        val defaults = defaultLabCodexItems()
        _labCodexItems.value = defaults
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
    // Medication Codex Delegate Implementation
    // ==========================================

    override fun addMedicationCodexItem(item: MedicationCodexItem): Boolean {
        if (item.name.isBlank()) return false
        val current = _medicationCodexItems.value.toMutableList()
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
        _medicationCodexItems.value = current
        saveMedicationItems(current)
        return true
    }

    override fun deleteMedicationCodexItem(itemId: String): Boolean {
        val current = _medicationCodexItems.value.toMutableList()
        val removed = current.removeAll { it.id == itemId }
        if (removed) {
            _medicationCodexItems.value = current
            saveMedicationItems(current)
        }
        return removed
    }

    override fun getMedicationCodexItem(itemId: String): MedicationCodexItem? {
        return _medicationCodexItems.value.firstOrNull { it.id == itemId }
    }

    override fun findMedicationCodexItemByName(name: String): MedicationCodexItem? {
        if (name.isBlank()) return null
        return _medicationCodexItems.value.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
    }

    override fun resetMedicationDefaults() {
        val defaults = defaultMedicationCodexItems()
        _medicationCodexItems.value = defaults
        saveMedicationItems(defaults)
    }

    // ==========================================
    // JSON Storage Helpers
    // ==========================================

    private fun loadLabItems(): List<LabCodexItem> {
        val jsonString = prefs.getString(KEY_LAB_ITEMS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<LabCodexItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LabCodexItem(
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

    private fun saveLabItems(items: List<LabCodexItem>) {
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
        val jsonString = prefs.getString(KEY_LAB_GROUPS, null) ?: return emptyList()
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

    private fun loadMedicationItems(): List<MedicationCodexItem> {
        val jsonString = prefs.getString(KEY_MED_ITEMS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<MedicationCodexItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MedicationCodexItem(
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

    private fun saveMedicationItems(items: List<MedicationCodexItem>) {
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

    companion object {
        private const val PREFS_NAME = "anc_clinical_codex_storage"
        private const val KEY_LAB_ITEMS = "codex_lab_items"
        private const val KEY_LAB_GROUPS = "codex_lab_groups"
        private const val KEY_MED_ITEMS = "codex_med_items"

        @Volatile
        private var instance: ClinicalCodexRepository? = null

        fun getInstance(context: Context): ClinicalCodexRepository {
            return instance ?: synchronized(this) {
                instance ?: ClinicalCodexRepository(context.applicationContext).also { instance = it }
            }
        }

        fun defaultLabCategoryGroups(): List<LabCategoryGroup> = listOf(
            LabCategoryGroup(
                id = "group_first_trimester",
                name = "فحوصات الثلث الأول الروتينية (First Trimester Booking)",
                description = "الفحوصات الأساسية الإلزامية في أول زيارة حمل",
                testNames = listOf(
                    "Complete Blood Count (CBC)",
                    "Blood Grouping & Rh Factor",
                    "Fasting Blood Sugar (FBS)",
                    "Urine Routine & Microscopic",
                    "Hepatitis B Surface Antigen (HBsAg)",
                    "HIV 1 & 2 Antibody",
                    "Rubella Antibody IgG",
                    "Thyroid Stimulating Hormone (TSH)",
                ),
                isCustom = false,
            ),
            LabCategoryGroup(
                id = "group_preeclampsia",
                name = "فحوصات ما قبل تسمم الحمل (Preeclampsia Workup)",
                description = "تقييم وظائف الكلى والكبد والصفائح الدموية",
                testNames = listOf(
                    "Complete Blood Count (CBC)",
                    "Serum Creatinine",
                    "Serum Uric Acid",
                    "Alanine Aminotransferase (ALT)",
                    "Aspartate Aminotransferase (AST)",
                    "Urine Routine & Microscopic",
                ),
                isCustom = false,
            ),
            LabCategoryGroup(
                id = "group_gdm",
                name = "مسح سكري الحمل (GDM Screening 24-28 W)",
                description = "فحص تحمل الجلوكوز وتحليل البول الأسبوع 24-28",
                testNames = listOf(
                    "Fasting Blood Sugar (FBS)",
                    "Oral Glucose Tolerance Test (OGTT 75g)",
                    "Urine Routine & Microscopic",
                ),
                isCustom = false,
            ),
            LabCategoryGroup(
                id = "group_anemia",
                name = "متابعة فقر الدم ومخزون الحديد (Anemia Workup)",
                description = "صورة دم كاملة ومخزون الفيريتين",
                testNames = listOf(
                    "Complete Blood Count (CBC)",
                    "Serum Ferritin",
                ),
                isCustom = false,
            ),
            LabCategoryGroup(
                id = "group_torch",
                name = "مسح العدوى الفيروسية (Serology Panel)",
                description = "مسح الأجسام المضادة للفيروسات والزهري",
                testNames = listOf(
                    "Hepatitis B Surface Antigen (HBsAg)",
                    "HIV 1 & 2 Antibody",
                    "Syphilis Serology (VDRL)",
                    "Rubella Antibody IgG",
                ),
                isCustom = false,
            ),
        )

        fun defaultLabCodexItems(): List<LabCodexItem> = listOf(
            LabCodexItem("lab_cbc", "Complete Blood Count (CBC)", "Hematology", "g/dL", "11.0 - 15.0"),
            LabCodexItem("lab_blood_group", "Blood Grouping & Rh Factor", "Hematology", "", ""),
            LabCodexItem("lab_fbs", "Fasting Blood Sugar (FBS)", "Biochemistry", "mg/dL", "70 - 95"),
            LabCodexItem("lab_ogtt_75", "Oral Glucose Tolerance Test (OGTT 75g)", "Biochemistry", "mg/dL", "< 140"),
            LabCodexItem("lab_urinalysis", "Urine Routine & Microscopic", "Biochemistry", "", "", "Nil Albumin, Nil Sugar"),
            LabCodexItem("lab_hbsag", "Hepatitis B Surface Antigen (HBsAg)", "Serology", "", "", "Negative"),
            LabCodexItem("lab_hiv", "HIV 1 & 2 Antibody", "Serology", "", "", "Non-reactive"),
            LabCodexItem("lab_vdrl", "Syphilis Serology (VDRL)", "Serology", "", "", "Non-reactive"),
            LabCodexItem("lab_rubella", "Rubella Antibody IgG", "Serology", "", "", "Positive (Immune)"),
            LabCodexItem("lab_tsh", "Thyroid Stimulating Hormone (TSH)", "Biochemistry", "mIU/L", "0.2 - 2.5"),
            LabCodexItem("lab_ferritin", "Serum Ferritin", "Hematology", "ng/mL", "30 - 150"),
            LabCodexItem("lab_creatinine", "Serum Creatinine", "Biochemistry", "mg/dL", "0.5 - 0.9"),
            LabCodexItem("lab_uric_acid", "Serum Uric Acid", "Biochemistry", "mg/dL", "2.5 - 5.5"),
            LabCodexItem("lab_alt", "Alanine Aminotransferase (ALT)", "Biochemistry", "U/L", "7 - 35"),
            LabCodexItem("lab_ast", "Aspartate Aminotransferase (AST)", "Biochemistry", "U/L", "10 - 35"),
            LabCodexItem("lab_urine_culture", "Urine Culture & Sensitivity", "Urinalysis", "", "", "No growth after 48 hrs"),
        )

        fun defaultMedicationCodexItems(): List<MedicationCodexItem> = listOf(
            MedicationCodexItem("med_folic", "Folic Acid", "5 mg", "Oral", "Once daily", true, "Neural tube defect prophylaxis"),
            MedicationCodexItem("med_iron", "Ferrous Sulfate", "200 mg", "Oral", "Once daily", true, "Maternal iron deficiency prevention"),
            MedicationCodexItem("med_calcium", "Calcium Carbonate + D3", "500 mg / 400 IU", "Oral", "Twice daily", true, "Bone mineralization and preeclampsia prophylaxis"),
            MedicationCodexItem("med_multivitamin", "Prenatal Multivitamins", "1 tablet", "Oral", "Once daily", true, "Comprehensive micronutrient support"),
            MedicationCodexItem("med_aspirin", "Low-Dose Aspirin", "150 mg", "Oral", "Once daily at bedtime", true, "Preeclampsia risk reduction from 12 weeks"),
            MedicationCodexItem("med_paracetamol", "Paracetamol", "500 mg", "Oral", "As needed (PRN)", false, "Analgesic & antipyretic for pregnancy"),
            MedicationCodexItem("med_progesterone", "Micronized Progesterone", "200 mg", "Oral", "Once daily at bedtime", true, "Luteal phase support"),
            MedicationCodexItem("med_enoxaparin", "Enoxaparin (LMWH)", "40 mg (4000 IU)", "Subcutaneous", "Once daily", true, "VTE thromboprophylaxis"),
            MedicationCodexItem("med_labetalol", "Labetalol", "100 mg", "Oral", "Twice daily", false, "First-line antihypertensive in pregnancy"),
            MedicationCodexItem("med_methyldopa", "Methyldopa", "250 mg", "Oral", "Three times daily", false, "Antihypertensive management"),
        )
    }
}
