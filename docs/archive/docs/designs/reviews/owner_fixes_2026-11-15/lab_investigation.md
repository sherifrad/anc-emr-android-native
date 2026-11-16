### Bug A — `Haemoglobin (Hb)` Listed Under `INDIVIDUAL INVESTIGATIONS` Instead of `Complete Blood Count (CBC)`

#### 1. Root Cause (`file:line`)

1. **No panel ID or catalog ID is stored on saved lab rows**:
   - Room entity [RecordEntities.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordEntities.kt#L253-L266) (`LabInvestigationEntity`), batch entry [LabBatchCommands.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/LabBatchCommands.kt#L13-L26) (`LabBatchEntry`), and UI model [ClinicalModels.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/ClinicalModels.kt#L160-L173) (`DemoLabInvestigation`) store only `id`, `patientId`, `date`, `testCategory`, `testName`, `result`, `unit`, `referenceRange`, `isAbnormal`, `notes`, `labName`, and `revision`.
   - While [LabBatchScreen.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/LabBatchScreen.kt#L899) (`groupLabBatchItems` in [LabBatchState.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/LabBatchState.kt#L107-L132)) and [LabEditor.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/LabEditor.kt#L214-L221) (`resolveLabPanel` in [ClinicalCatalogModels.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogModels.kt#L51-L72)) group unsaved tests by matching `testName` against `ClinicalCatalogRepository.labCategoryGroups`, the **Labs tab** does not use `ClinicalCatalogRepository`.
2. **Exact grouping code on the Labs tab**:
   - [LabsTab.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/LabsTab.kt#L46-L48) calls `groupLabsIntoReports(patient.labInvestigations)` ([LabReportGrouping.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt#L100-L113)), grouping rows by `date to labName`.
   - [LabReportItem.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/LabReportItem.kt#L67) calls `partitionReportIntoCompositeGroups(report.items)` ([LabReportGrouping.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt#L39-L98)), which partitions saved tests into hardcoded composite panels using substring keyword lists ([LabReportGrouping.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt#L33-L60)):
     ```kotlin
     // LabReportGrouping.kt:33-34
     private val CBC_KEYWORDS = listOf("hemoglobin", "platelet", "wbc", "white blood", "rbc", "red blood", "hematocrit", "hct", "mcv", "mch", "mchc")
     private val URINE_KEYWORDS = listOf("urine", "pus cells", "nitrite", "leukocyte", "protein")
     ```
     ```kotlin
     // LabReportGrouping.kt:49-60
     items.forEach { lab ->
         val nameLower = lab.testName.lowercase()
         val catLower = lab.testCategory.lowercase()
         when {
             CBC_KEYWORDS.any { nameLower.contains(it) } -> cbcItems.add(lab)
             catLower.contains("urine") || URINE_KEYWORDS.any { nameLower.contains(it) } -> urineItems.add(lab)
             catLower.contains("stool") || STOOL_KEYWORDS.any { nameLower.contains(it) } -> stoolItems.add(lab)
             LFT_KEYWORDS.any { nameLower.contains(it) } -> lftItems.add(lab)
             KFT_KEYWORDS.any { nameLower.contains(it) } -> kftItems.add(lab)
             else -> standaloneItems.add(lab)
         }
     }
     ```
3. **Why `Haemoglobin (Hb)` falls through AND why the counts show `CBC (6) Normal` and `Complete Urine Analysis (7)`**:
   - **Spelling/keyword mismatch on `Haemoglobin (Hb)`**: In [ClinicalCatalogRepository.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepository.kt#L535-L542) and [ClinicalCatalogRepository.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepository.kt#L637), the canonical catalog test name is `"Haemoglobin (Hb)"` (British `ae` spelling + `(Hb)`). `CBC_KEYWORDS` ([LabReportGrouping.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt#L33)) only contains American `"hemoglobin"` and omits `"haemoglobin"` and `"hb"`. (By contrast, `"Haematocrit (Hct)"` also misses `"hematocrit"` and `"White Cell Count (WBC)"` misses `"white blood"`, but both match via `"hct"` and `"wbc"` in `CBC_KEYWORDS`.) Thus `"haemoglobin (hb)"` matches no branch in [LabReportGrouping.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt#L52-L57) and falls into `else -> standaloneItems.add(lab)` ([LabReportGrouping.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt#L58)), becoming `"Individual Investigations"` ([LabReportGrouping.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt#L94)), rendered as `"INDIVIDUAL INVESTIGATIONS"` in [LabReportItem.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/LabReportItem.kt#L259).
   - **Branch-order collision with `"RBCs (Urine)"` explaining `(6)` and `(7)`**: `URINE_ELEMENTS` in [ClinicalCatalogRepository.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepository.kt#L545-L554) has **8** tests, the 8th being `"RBCs (Urine)"` (`testCategory = "Urinalysis"`, [ClinicalCatalogRepository.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepository.kt#L653)). Because [LabReportGrouping.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt#L53) evaluates `CBC_KEYWORDS` (`"rbc"`) **before** line 54's urine check, `"rbcs (urine)"` matches `"rbc"` and is hijacked into `cbcItems`.
   - **Concrete example from the 17-test report** (6 CBC tests + 8 Urine Routine tests + 3 other tests = 17 tests):
     - `cbcItems` receives 5 CBC tests (`Haematocrit (Hct)`, `MCV`, `MCH`, `White Cell Count (WBC)`, `Platelets (PLT)`) + 1 hijacked `"RBCs (Urine)"` = **6 items**. Because the abnormal `Haemoglobin (Hb) 10.2 g/dL` was dropped into `standaloneItems` and the hijacked `"RBCs (Urine)"` was normal, the CBC header falsely displays **`Complete Blood Count (CBC) (6) Normal`** instead of **`1 Abnormal`**.
     - `urineItems` receives the remaining 7 of 8 urine tests = **`Complete Urine Analysis (7) 2 Abnormal`**.
     - `standaloneItems` receives **`Haemoglobin (Hb) 10.2 g/dL`** + the 3 other tests under **`INDIVIDUAL INVESTIGATIONS`**.
   - **Additional flaws in `partitionReportIntoCompositeGroups` ([LabReportGrouping.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt#L33-L58))**:
     - Line 54 checks `catLower.contains("urine")`, which is `false` for the canonical category `"Urinalysis"` (`"urinalysis".contains("urine") == false`).
     - Line 36 `LFT_KEYWORDS` includes `"ast"`, which is a substring of `"Fasting Blood Sugar (FBS)"` (`"fasting"` contains `"ast"`). If a report contains `"Fasting Blood Sugar (FBS)"` alongside any LFT test (`ALT`/`AST`), FBS is misclassified into `"Liver Function Panel (LFT)"`.

#### 2. Smallest Fix Plan (Bug A)

- **File**: [LabReportGrouping.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt#L33-L60)
- **Functions/Constants to change**: `CBC_KEYWORDS`, `URINE_KEYWORDS`, `LFT_KEYWORDS`, and `partitionReportIntoCompositeGroups`:
  1. Add British spellings and catalog tokens to `CBC_KEYWORDS`: `"haemoglobin"`, `"hemoglobin"`, `"hb"`, `"haematocrit"`, `"hematocrit"`, `"hct"`, `"mcv"`, `"mch"`, `"mchc"`, `"white cell"`, `"white blood"`, `"wbc"`, `"platelet"`, `"plt"`, `"rbc"`, `"red blood"`.
  2. Evaluate Urine and Stool before CBC (or guard CBC with `catLower != "urinalysis" && !nameLower.contains("urine") && !catLower.contains("stool")`), and check `catLower == "urinalysis" || catLower.contains("urine")` so `"RBCs (Urine)"` (category `"Urinalysis"`) goes to `urineItems`, not `cbcItems`. Exclude `"urine culture"` from `Complete Urine Analysis` if `Urine Culture & Sensitivity` should remain an individual test (or keep dipstick/microscopy keywords).
  3. Fix `"ast"` / `"alt"` matching in `LFT_KEYWORDS` so `"Fasting Blood Sugar (FBS)"` (`"fasting"`) is never matched by `"ast"` (match word/token boundaries or exact catalog terms: `"alanine aminotransferase"`, `"aspartate aminotransferase"`, `"(alt)"`, `"(ast)"`, or regex `\b(alt|ast)\b`).

#### 3. Tests to Prove It (Bug A)

- **Unit tests** in [PatientHubPureModelTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/patienthub/model/PatientHubPureModelTest.kt):
  1. `partitionReportIntoCompositeGroups_allSixCatalogCbcAndEightUrineElementsGroupedCorrectly`: Pass all 6 default `CBC_ELEMENTS` (with `"Haemoglobin (Hb)"` = `"10.2"`, `isAbnormal = true`, others normal) + all 8 `URINE_ELEMENTS` (including `"RBCs (Urine)"`, 2 abnormal) + 3 individual tests (`"Blood Grouping & Rh Factor"`, `"Fasting Blood Sugar (FBS)"`, `"Thyroid Stimulating Hormone (TSH)"`) = 17 tests. Assert:
     - `Complete Blood Count (CBC)` has `size == 6`, contains `"Haemoglobin (Hb)"`, does **not** contain `"RBCs (Urine)"`, `isAnyAbnormal == true`, `abnormalCount == 1`.
     - `Complete Urine Analysis` has `size == 8`, contains `"RBCs (Urine)"`, `abnormalCount == 2`.
     - `Individual Investigations` has `size == 3` and does **not** contain `"Haemoglobin (Hb)"`.
  2. `partitionReportIntoCompositeGroups_fastingBloodSugarNotGroupedIntoLft`: Pass `"Fasting Blood Sugar (FBS)"` + `"Alanine Aminotransferase (ALT)"` + `"Aspartate Aminotransferase (AST)"`; assert FBS is in `Individual Investigations` and LFT has only the 2 liver tests.
- **UI test** in `app/src/androidTest/java/com/sherifradwan/ancnative/` (e.g. `LabBatchUiTest.kt` or `LabsTabUiTest.kt`):
  - Render `LabsTab` / `LabReportItem` with the 17-test report; assert `"Complete Blood Count (CBC)"` and `"(6)"` and `"1 Abnormal"` are displayed, `"Complete Urine Analysis"` and `"(8)"` are displayed, and `"Haemoglobin (Hb)"` is inside the CBC group card rather than under `"INDIVIDUAL INVESTIGATIONS"`.

#### 4. Data-Migration Risk (Bug A)

- **Zero Room schema or data-migration risk**: Grouping is computed purely in-memory at render time in `partitionReportIntoCompositeGroups(report.items)`. Existing saved database rows with `testName = "Haemoglobin (Hb)"` and `testName = "RBCs (Urine)"` immediately group into the right panels without touching Room (schema remains v7).

---

### Bug B — Clinical Panels (and Medications) in Catalog Settings Cannot Be Edited

#### 1. Root Cause (`file:line`)

1. **Lab Clinical Panels (`LabCategoryGroup`) — Create/Delete only, no Edit path**:
   - **UI state & list card** ([CatalogSettingsSections.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/CatalogSettingsSections.kt#L67-L76), [CatalogSettingsSections.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/CatalogSettingsSections.kt#L307-L375)):
     - Individual tests have `var itemToEdit by remember { mutableStateOf<LabCatalogItem?>(null) }` (line 72) and an Edit `IconButton` (`testTag("editLabCodexBtn-${lab.id}")`, lines 252–264).
     - Clinical panels only have `showAddGroupDialog` (line 73) and `groupToDelete` (line 75); there is **no `groupToEdit` state**. Each panel card (`testTag("labCategoryGroup-${group.id}")`, line 311) renders only the Delete `IconButton` (`testTag("deleteLabGroupBtn-${group.id}")`, lines 335–345) and no Edit button.
   - **Create dialog & delete flow** ([CatalogSettingsSections.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/CatalogSettingsSections.kt#L420-L430), [CatalogSettingsSections.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/CatalogSettingsSections.kt#L456-L478), [CatalogSettingsSections.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/CatalogSettingsSections.kt#L648-L770)):
     - `AddLabCategoryGroupDialog` (lines 648–770) has no `initial: LabCategoryGroup?` parameter, initializes `groupName = ""`, `groupDescription = ""`, `selectedTests = emptySet()` (lines 653–655), hardcodes `"Create New Diagnostic Panel"` (line 666), and always constructs a new `LabCategoryGroup` with a random UUID (lines 750–755).
     - Delete confirmation dialog (lines 456–478) calls `catalogDelegate.deleteLabCategoryGroup(it.id)` (line 465).
   - **Delegate & Repository** ([ClinicalCatalogModels.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogModels.kt#L152-L157), [ClinicalCatalogRepository.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepository.kt#L136-L169)):
     - **No update path exists** for panels: `LabCatalogDelegate` has `addLabCategoryGroup`, `deleteLabCategoryGroup`, `getLabCategoryGroup`, `resetLabCategoryGroupDefaults`—no `updateLabCategoryGroup`.
     - `ClinicalCatalogRepository.addLabCategoryGroup` (lines 136–149) matches by `it.name.equals(group.name.trim(), ignoreCase = true)` (line 139), not by `id`, so renaming a panel via `addLabCategoryGroup` appends a duplicate instead of updating in place.

2. **Medication Catalog (`MedicationCatalogItem`) — Exact same gap**:
   - **UI** ([CatalogSettingsSections.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/CatalogSettingsSections.kt#L773-L1128)):
     - Has `showAddDialog` (line 779) and `itemToDelete` (line 780), but **no `itemToEdit` state**.
     - Each medication card (`testTag("medCodexItem-${med.id}")`, line 863) has only a Delete `IconButton` (`testTag("deleteMedCodexBtn-${med.id}")`, lines 916–926) and no Edit button.
     - `AddMedicationDialog` (lines 1005–1128) has no `initial: MedicationCatalogItem?` parameter, initializes empty fields (lines 1009–1013), hardcodes `isProphylaxis = false` (line 1110), and calls `catalogDelegate.addMedicationCatalogItem(newItem)` (line 949).
   - **Delegate & Repository** ([ClinicalCatalogModels.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogModels.kt#L159-L166), [ClinicalCatalogRepository.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepository.kt#L175-L216)):
     - **No update path exists**: `MedicationCatalogDelegate` and `ClinicalCatalogRepository` lack `updateMedicationCatalogItem`. `addMedicationCatalogItem` (lines 175–191) matches by `name + dosage` (lines 178–181), not by `id`, so changing name or dosage creates a duplicate entry.

3. **Hidden reload bug in `ClinicalCatalogRepository.loadCatalogData()` ([ClinicalCatalogRepository.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepository.kt#L38-L63))**:
   - Unlike `loadMedicationItems()` (lines 65–72, which returns `loadedMeds` when non-empty), `loadCatalogData()` for labs (lines 45–49) and panels (lines 58–62) executes:
     ```kotlin
     val defaults = defaultLabCategoryGroups()
     val customGroups = loadedGroups.filter { it.isCustom }
     val merged = defaults + customGroups
     ```
   - When a default test (`id = "lab_cbc_hb"`) or default panel (`id = "group_cbc"`) is edited (`isCustom = true`), on the next app launch `defaults + customGroups` re-adds the unedited default item/panel **and** appends the edited custom copy with the **same `id`**, duplicating the entry and reverting the first copy to defaults (and if `group.isCustom` stayed `false` when `applyLabItemEdit` cascaded a test rename at [ClinicalCatalogModels.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogModels.kt#L122-L133), the rename is discarded on restart).

#### 2. Smallest Fix Plan (Bug B)

1. **[ClinicalCatalogModels.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogModels.kt)**:
   - Add pure helper `applyLabCategoryGroupEdit(groups: List<LabCategoryGroup>, edited: LabCategoryGroup): List<LabCategoryGroup>?`:
     - Trim `name` and `description`; trim and deduplicate `testNames` (reject if `name` is blank or `testNames` is empty).
     - Find existing index by `it.id == edited.id` (return `null` if not found).
     - Reject duplicate name on a different group: `if (groups.any { it.id != edited.id && it.name.trim().equals(trimmedName, ignoreCase = true) }) return null`.
     - Replace at `existingIndex` with `edited.copy(name = trimmedName, description = edited.description.trim(), testNames = cleanedTestNames, isCustom = true)`.
   - Add pure helper `applyMedicationItemEdit(items: List<MedicationCatalogItem>, edited: MedicationCatalogItem): List<MedicationCatalogItem>?`:
     - Trim `name`, `dosage`, `route`, `frequency`, `instructions` (reject if `name` is blank).
     - Find existing index by `it.id == edited.id` (return `null` if not found).
     - Reject duplicate `name + dosage` on a different item: `if (items.any { it.id != edited.id && it.name.trim().equals(trimmedName, ignoreCase = true) && it.dosage.trim().equals(trimmedDosage, ignoreCase = true) }) return null`.
     - Replace at `existingIndex` with `edited.copy(..., isCustom = true)`.
   - Add `fun updateLabCategoryGroup(group: LabCategoryGroup): Boolean` to `LabCatalogDelegate` (line 153).
   - Add `fun updateMedicationCatalogItem(item: MedicationCatalogItem): Boolean` to `MedicationCatalogDelegate` (line 161).

2. **[ClinicalCatalogRepository.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepository.kt)**:
   - Implement `override fun updateLabCategoryGroup(group: LabCategoryGroup): Boolean` using `applyLabCategoryGroupEdit(_labCategoryGroups.value, group)`, updating `_labCategoryGroups.value` and calling `saveLabCategoryGroups(updated)`.
   - Implement `override fun updateMedicationCatalogItem(item: MedicationCatalogItem): Boolean` using `applyMedicationItemEdit(_medicationCatalogItems.value, item)`, updating `_medicationCatalogItems.value` and calling `saveMedicationItems(updated)`.
   - In `loadCatalogData()` (lines 39–63), stop duplicating edited default items/groups via `defaults + custom`: when `loadedLabs` / `loadedGroups` is non-empty, use the stored list (deduplicating by `id` keeping the last entry to heal any existing duplicate prefs), matching `loadMedicationItems()` (lines 65–72).

3. **[CatalogSettingsSections.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/CatalogSettingsSections.kt)**:
   - **Lab Panels (`LabCatalogSettingsSection` & `AddLabCategoryGroupDialog`)**:
     - Add `var groupToEdit by remember { mutableStateOf<LabCategoryGroup?>(null) }` (around line 75).
     - In the panel card row (lines 335–346), add an Edit `IconButton` (`Icons.Default.Edit`, `contentDescription = "Edit Panel"`, `testTag("editLabGroupBtn-${group.id}")`) next to Delete that sets `groupToEdit = group`.
     - Update `AddLabCategoryGroupDialog` (lines 648–770) to accept `initial: LabCategoryGroup? = null` and `onSave: (LabCategoryGroup) -> Boolean`:
       - Initialize `groupName`, `groupDescription`, and `selectedTests` from `initial` (`remember(initial)`).
       - Include any `initial.testNames` not currently in `availableTests` as selectable chips so custom/legacy member names can still be deselected.
       - Title: `if (initial != null) "Edit Diagnostic Panel" else "Create New Diagnostic Panel"`.
       - On confirm, build `initial?.copy(...) ?: LabCategoryGroup(...)`, call `onSave`, and show `"Panel name already used or invalid"` if `onSave` returns `false`.
     - Wire `if (groupToEdit != null)` to `AddLabCategoryGroupDialog(initial = groupToEdit, ...)` calling `catalogDelegate.updateLabCategoryGroup(editedGroup)`.
   - **Medications (`MedicationCatalogSettingsSection` & `AddMedicationDialog`)**:
     - Add `var itemToEdit by remember { mutableStateOf<MedicationCatalogItem?>(null) }` (around line 780).
     - In the medication card row (lines 916–926), add an Edit `IconButton` (`Icons.Default.Edit`, `contentDescription = "Edit Medication"`, `testTag("editMedCodexBtn-${med.id}")`) next to Delete that sets `itemToEdit = med`.
     - Update `AddMedicationDialog` (lines 1005–1128) to accept `initial: MedicationCatalogItem? = null` and `onSave: (MedicationCatalogItem) -> Boolean`:
       - Initialize `name`, `dosage`, `selectedRoute`, `frequency`, `instructions` from `initial` (`remember(initial)`), preserving `initial.isProphylaxis` on edit.
       - Title: `if (initial != null) "Edit medication" else "Add new medication"`; confirm button text: `if (initial != null) "Save" else "Add to my list"`.
       - Wire `if (itemToEdit != null)` to `AddMedicationDialog(initial = itemToEdit, ...)` calling `catalogDelegate.updateMedicationCatalogItem(editedMed)`.

#### 3. Tests to Prove It (Bug B)

- **Unit tests**:
  1. [LabItemEditTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/model/catalog/LabItemEditTest.kt) (or new `CatalogEditTest.kt`):
     - `applyLabCategoryGroupEdit`: renames panel and updates `testNames` while preserving `id` and list position; rejects blank name, empty `testNames`, unknown `id`, or name collision with another panel (case-insensitive); allows case-only rename on the same panel; marks `isCustom = true`.
     - `applyMedicationItemEdit`: updates name, dosage, route, frequency, instructions while preserving `id`, position, and `isProphylaxis`; rejects blank name, unknown `id`, or colliding `name + dosage` on another item.
  2. [ClinicalCatalogRepositoryTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepositoryTest.kt) / [CustomChoicesLogicTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/model/catalog/CustomChoicesLogicTest.kt):
     - Using `FakeSharedPreferences`, verify `updateLabCategoryGroup`, `updateLabCatalogItem`, and `updateMedicationCatalogItem` persist across `ClinicalCatalogRepository(fakePrefs)` re-instantiation without duplicating default IDs (`group_cbc`, `lab_cbc_hb`, `med_folic`).
- **UI tests** (Compose instrumented test in `app/src/androidTest/java/com/sherifradwan/ancnative/`):
  1. Render `LabCatalogSettingsSection`, switch to `labCategoryGroupsTab`, click `editLabGroupBtn-group_cbc`, change panel name and toggle a member test chip (`selectTestChip-...`), click `saveLabGroupConfirmBtn`, and assert the updated name and member count render in `labCategoryGroup-group_cbc`.
  2. Render `MedicationCatalogSettingsSection`, click `editMedCodexBtn-med_folic`, edit dosage/frequency, click `saveMedCodexConfirmBtn`, and assert the updated label and frequency render in `medCodexItem-med_folic`.

#### 4. Data-Migration Risk (Bug B)

- **Zero Room schema risk**: Catalog items, clinical panels, and medication catalog items are stored in `SharedPreferences` (`anc_clinical_catalog_storage` JSON keys `catalog_lab_items`, `catalog_lab_groups`, `catalog_med_items` in [ClinicalCatalogRepository.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepository.kt#L517-L523)), not in Room.
- **SharedPreferences compatibility**: Existing JSON schemas in `loadLabCategoryGroups()` and `loadMedicationItems()` (`id`, `name`, `description`, `testNames`, `isCustom` and `id`, `name`, `dosage`, `route`, `frequency`, `isProphylaxis`, `instructions`, `isCustom`) are unchanged. Deduplicating by `id` (keeping the last entry) in `loadCatalogData()` cleanly heals any duplicate default entries created by the prior `defaults + custom` merge logic.
