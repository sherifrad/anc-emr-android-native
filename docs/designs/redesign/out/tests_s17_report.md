# Redesign Slice 17: Catalog Settings Tests Report

## Summary
Created [CatalogSettingsRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/CatalogSettingsRedesignTest.kt) implementing comprehensive UI, accessibility, ergonomics, and behavioral parity verification for Redesign Slice 17 (`LabCatalogSettingsSection`, `MedicationCatalogSettingsSection`, `AddLabTestDialog`, `AddLabCategoryGroupDialog`, `AddMedicationDialog`, confirmation dialogs, and integration within `SecuritySettingsDialog`).

No existing test files required edits (repository grep confirmed zero tests asserted the replaced hardcoded strings or removed Title Case text).

The test suite strictly adheres to the project style templates ([HistoryEditorsRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt), [RegistrationRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationRedesignTest.kt)):
- Single `setContent` per test method.
- Isolated in-memory `FakeCatalogDelegate` with deterministic state flows.
- Narrow `360.dp x 640.dp` viewport host with unclipped boundary checks.
- Verification within `SecuritySettingsDialog`'s ~`298.dp` inner column width.
- Dynamic `fontScale` overrides (`1.0f`, `1.3f`, `2.0f`) via `LocalDensity` and configuration manipulation (`narrowWindow`).
- `useUnmergedTree = true` for semantic traversal where labels are cleared by `clearAndSetSemantics`.
- `performScrollTo()` before assertions on scrollable elements.
- Node selection via explicit `testTag`s on exact semantic nodes rather than ambiguous text.
- Minimum touch target tolerance `>= 47.99.dp`.
- Complete independence of every test case.

## Coverage Matrix (29 Tests)

### 1. Typography & Legacy Glyphs Removal (Zero Text < 12sp & Clean Hierarchy)
- `typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_labIndividualTestsSection`: Verifies all text nodes in the lab individual tests subview are `>= 12.sp` (`fontSize.value >= 11.99.sp`), checks localized header, summary (`"28 tests, 7 panels"`), switcher chips, and asserts zero `"·"` or legacy titles.
- `typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_labPanelsSection`: Verifies all text nodes in the diagnostic panels subview are `>= 12.sp`, checks `BandChip` count (`"6 tests"`), `RecordBandCard` layout, `PlainField` label (`"Included tests"`), and asserts zero `"·"` or legacy titles.
- `typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_medicationSection`: Verifies all text nodes in the medication catalog are `>= 12.sp`, checks header, summary (`"10 medications, available for all patient plans"`), row metadata, and asserts zero `"·"` or legacy titles.
- `typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_addLabTestDialog`: Opens `AddLabTestDialog`, verifies all field and button texts are `>= 12.sp`, and asserts zero `"·"`.
- `typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_addLabCategoryGroupDialog`: Opens `AddLabCategoryGroupDialog`, verifies normalized title `"Create diagnostic panel"` (sentence case, eliminating `"Create New Diagnostic Panel"`), and asserts all text nodes are `>= 12.sp`.
- `typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_addMedicationDialog`: Opens `AddMedicationDialog`, verifies route and frequency filter chips are `>= 12.sp` (resolving the legacy `11.sp` issue), and asserts zero `"·"`.

### 2. Touch Targets (>= 47.99dp on Every Interactive Element) & 360dp Containment
- `touchTargets_labCatalogSection_allInteractiveTargetsAtLeast48dp`: Verifies `>= 47.99.dp` width & height and 360dp viewport containment on `addNewLabTestBtn`, `labIndividualTestsTab`, `labCategoryGroupsTab`, `searchLabCodexField`, `editLabCodexBtn-lab_cbc_hb`, `deleteLabCodexBtn-lab_cbc_hb`, `resetLabCatalogBtn`, `addNewLabGroupBtn`, `editLabGroupBtn-group_cbc`, and `deleteLabGroupBtn-group_cbc`.
- `touchTargets_medicationCatalogSection_allInteractiveTargetsAtLeast48dp`: Verifies `>= 47.99.dp` width & height and 360dp viewport containment on `addNewMedBtn`, `searchMedCodexField`, `editMedCodexBtn-med_paracetamol`, `deleteMedCodexBtn-med_paracetamol`, and `resetMedCatalogBtn`.
- `touchTargets_addLabTestDialog_controlsAndChipsAtLeast48dp`: Verifies `>= 47.99.dp` on `saveLabCodexConfirmBtn`, `cancelLabCodexBtn`, and category chips `labCodexCategoryChoice-0`, `labCodexCategoryChoice-1`.
- `touchTargets_addLabCategoryGroupDialog_controlsAndChipsAtLeast48dp`: Verifies `>= 47.99.dp` on `saveLabGroupConfirmBtn`, `cancelLabGroupBtn`, and selectable test chips `selectTestChip-lab_cbc_hb`, `selectTestChip-lab_cbc_hct`.
- `touchTargets_addMedicationDialog_controlsAndChipsAtLeast48dp`: Verifies `>= 47.99.dp` on `saveMedCodexConfirmBtn`, `cancelMedCodexBtn`, route chips `medCodexRouteChoice-0`, and frequency chips `medCodexFreqChoice-0`.
- `touchTargets_confirmationAndEmptyDialogButtons_allAtLeast48dp`: Verifies `>= 47.99.dp` on confirmation dialog buttons (`deleteLabConfirmBtn`, `cancelDeleteLabBtn`, `resetLabConfirmBtn`, `cancelResetLabBtn`) and search clear controls (`clearSearchLabCodexBtn`, `clearLabSearchAction`).

### 3. Functional Behaviour & Parity: Lab Catalog & Individual Tests
- `labCatalog_individualTestsFlow_searchFilter_andEmptyCatalog`: Tests search filtering by `"Hb"`, verifies no-match empty state on `"NonExistentTest123"`, and asserts clearing search restores items.
- `labCatalog_emptyCatalog_showsEmptyAddAndResetButtons`: Tests empty catalog display, verifying presence and touch targets of `emptyAddLabTestBtn` and `emptyResetLabTestsBtn`.
- `labCatalog_addLabTestDialog_validationAndCreation`: Tests inline validation on blank test name displaying `addLabError` (`"Please enter a test name."`), fills all fields, and verifies `addLabCatalogItem` receives `LabCatalogItem` with `isCustom = true`.
- `labCatalog_editAndDeleteAndReset_behaviourParity`: Tests editing a lab test invoking `updateLabCatalogItem`, deleting a test invoking `deleteLabCatalogItem`, and resetting catalog defaults invoking both `resetLabDefaults()` and `resetLabCategoryGroupDefaults()`.

### 4. Functional Behaviour & Parity: Lab Panels (Category Groups)
- `labPanels_bandCardLayout_plainFieldIncludedTests_andAddPanelFlow`: Verifies `RecordBandCard` layout with `BandChip` and `PlainField("Included tests", ...)`; tests `AddLabCategoryGroupDialog` validation (blank name error, no tests selected error), search filtering across available tests, empty search recovery (`clearLabGroupTestsEmptySearchBtn`), chip selection toggling, and creation with `isCustom = true`.
- `labPanels_editAndDelete_behaviourParity`: Tests editing a panel invoking `updateLabCategoryGroup` and deleting a panel invoking `deleteLabCategoryGroup`.

### 5. Functional Behaviour & Parity: Medication Catalog
- `medicationCatalog_rowLayout_searchFilter_andEmptyCatalog`: Verifies `CatalogListRow` displaying `med.displayLabel`, `med.route, medicationFrequencyLabel(med.frequency)`, and instructions; tests search filtering and empty recovery actions.
- `medicationCatalog_emptyCatalog_showsEmptyAddAndResetButtons`: Tests empty medication catalog displaying `emptyAddMedBtn` and `emptyResetMedsBtn`.
- `medicationCatalog_addMedicationDialog_validationAndCreation`: Tests validation on blank medication name displaying `addMedError` (`"Please enter a medication name."`), fills name/dosage/route/frequency/instructions, and verifies `addMedicationCatalogItem` receives `MedicationCatalogItem` with `isCustom = true, isProphylaxis = false`.
- `medicationCatalog_editAndDeleteAndReset_behaviourParity`: Tests editing medication invoking `updateMedicationCatalogItem`, deleting medication invoking `deleteMedicationCatalogItem`, and resetting medications invoking `resetMedicationDefaults()`.

### 6. Responsive Side-by-Side vs Stacked Layout (1.0f vs 2.0f Font Scale)
- `addMedicationDialog_sideBySideAt1_0`: Verifies `addMedNameField` and `addMedDosageField` sit side-by-side (`dosage.left >= name.right - 1.dp`, `abs(dosage.top - name.top) <= 4.dp`) when `fontScale <= 1.3f`.
- `addMedicationDialog_stackedAt2_0`: Verifies `addMedNameField` and `addMedDosageField` stack vertically (`dosage.top >= name.bottom - 1.dp`) when `fontScale > 1.3f`.
- `addLabTestDialog_sideBySideAt1_0`: Verifies `addLabUnitField` and `addLabRefRangeField` sit side-by-side (`refRange.left >= unit.right - 1.dp`, `abs(refRange.top - unit.top) <= 4.dp`) when `fontScale <= 1.3f`.
- `addLabTestDialog_stackedAt2_0`: Verifies `addLabUnitField` and `addLabRefRangeField` stack vertically (`refRange.top >= unit.bottom - 1.dp`) when `fontScale > 1.3f`.

### 7. Host Integration: SecuritySettingsDialog (~298dp Column), 1.3f Scaling, Dark Theme & RTL
- `securitySettingsDialog_hostsCatalogSectionsAt298dpWithoutClipping`: Mounts `SecuritySettingsDialog` in a 360dp container; verifies switching to `"Lab Tests"` tab and `"Medications"` tab renders catalog sections within the ~`298.dp` inner column without horizontal overflow, measuring `>= 47.99.dp` touch targets.
- `viewportAndScaling_360dpAtFontScale1_3_noClipping`: Mounts at `fontScale = 1.3f`, asserting text is unclipped (`assertTextNotClipped`) and controls remain inside host bounds.
- `darkThemeAndRtl_rendersCleanlyWithoutOverlap`: Mounts under `AncTheme(darkTheme = true)` and `LayoutDirection.Rtl`, asserting elements remain within host bounds and render cleanly.

## Tag Coverage Checklist (All 25 Existing Tags Preserved + All Added Tags Covered)
- **Existing Tags Preserved (25/25)**: `addNewLabTestBtn`, `addNewLabGroupBtn`, `labIndividualTestsTab`, `labCategoryGroupsTab`, `searchLabCodexField`, `labCodexItem-${lab.id}`, `editLabCodexBtn-${lab.id}`, `deleteLabCodexBtn-${lab.id}`, `labCategoryGroup-${group.id}`, `editLabGroupBtn-${group.id}`, `deleteLabGroupBtn-${group.id}`, `addLabNameField`, `saveLabCodexConfirmBtn`, `addLabGroupNameField`, `addLabGroupDescField`, `selectTestChip-${test.id}`, `saveLabGroupConfirmBtn`, `addNewMedBtn`, `searchMedCodexField`, `medCodexItem-${med.id}`, `editMedCodexBtn-${med.id}`, `deleteMedCodexBtn-${med.id}`, `addMedNameField`, `addMedDosageField`, `saveMedCodexConfirmBtn`.
- **Added Tags Covered**: `clearSearchLabCodexBtn`, `clearLabSearchAction`, `emptyAddLabTestBtn`, `emptyResetLabTestsBtn`, `emptyCreateLabGroupBtn`, `emptyResetLabGroupsBtn`, `resetLabCatalogBtn`, `clearSearchMedCodexBtn`, `clearMedSearchAction`, `emptyAddMedBtn`, `emptyResetMedsBtn`, `resetMedCatalogBtn`, `addLabTestDialog`, `labCodexCategoryChoice-$index`, `addLabUnitField`, `addLabRefRangeField`, `addLabDefaultResultField`, `addLabError`, `cancelLabCodexBtn`, `saveLabCodexConfirmBtnSpinner`, `addLabGroupDialog`, `searchLabGroupTestsField`, `clearLabGroupTestsSearchBtn`, `clearLabGroupTestsEmptySearchBtn`, `addLabGroupError`, `cancelLabGroupBtn`, `saveLabGroupConfirmBtnSpinner`, `addMedDialog`, `medCodexRouteChoice-$index`, `medCodexFreqChoice-$index`, `addMedInstructionsField`, `addMedError`, `cancelMedCodexBtn`, `saveMedCodexConfirmBtnSpinner`, `deleteLabConfirmDialog`, `deleteLabConfirmBtn`, `cancelDeleteLabBtn`, `deleteLabGroupConfirmDialog`, `deleteLabGroupConfirmBtn`, `cancelDeleteLabGroupBtn`, `resetLabConfirmDialog`, `resetLabConfirmBtn`, `cancelResetLabBtn`, `deleteMedConfirmDialog`, `deleteMedConfirmBtn`, `cancelDeleteMedBtn`, `resetMedConfirmDialog`, `resetMedConfirmBtn`, `cancelResetMedBtn`.

## Verification Status
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: Exit code `0` (Success).
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest -q`: Exit code `0` (Success).
- File size of `CatalogSettingsRedesignTest.kt`: 1,158 lines (well below the 1,500-line ceiling).
- Zero production edits; zero git commands executed; no emulator/adb used.
