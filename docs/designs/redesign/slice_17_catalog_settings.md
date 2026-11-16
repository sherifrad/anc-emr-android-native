# Slice 17: Catalog settings

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s17_catalog_settings.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt` (`RecordType`, `RecordShapes`, `RecordSpacing`, `FieldFamilies`, `LocalFieldFamily`), `ui/components/RecordSectionCard.kt` (`RecordSectionCard`), `ui/components/RecordBandCard.kt` (`RecordBandCard`), `ui/components/StatusTag.kt` (`BandChip`), `ui/components/MetricCell.kt` (`PlainField`), `ui/components/FormFieldLabel.kt` (`FormFieldLabel`), `ui/components/RecordFilterChip.kt` (`RecordFilterChip`), `ui/patienthub/components/EditorSheetParts.kt` (`SheetCancelSaveRow`), `ui/Components.kt` (`editorFieldColors`, `minimumTouchTarget`), and `ui/ClinicalPresets.kt` (`COMMON_MEDICATION_FREQUENCIES`, `medicationFrequencyLabel`, read-only). Read them; do not re-spec them.

- **Owner direction**: `antislop: DURING`, dials ENERGY 1 / RHYTHM 1 / MOTION 1 (pressed and focus states only, no new animation). Catalog management inside settings must stay uncrushed at 360 dp (including the ~298 dp inner column of `SecuritySettingsDialog`), keep every tap target at least 48 dp, and match the Background (sand) visual language of Labs and Medications.
- **Branch hygiene**: run `.agents/scripts/git_audit.sh` before starting and before pushing; update `docs/PROJECT_STATE.json` + run `.agents/scripts/project_state.py render` + update `.agents/pipeline/HANDOFF_AGY.md` in the same commit on `redesign-2026-11-15`.
- **Never touch**:
  - `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `storage/VaultController.kt`
  - `model/catalog/ClinicalCatalogModels.kt` (`LabCatalogItem`, `MedicationCatalogItem`, `LabCategoryGroup`, `LabCatalogDelegate`, `MedicationCatalogDelegate`, `applyLabItemEdit`, `applyLabCategoryGroupEdit`, `applyMedicationItemEdit`), `model/catalog/ClinicalCatalogRepository.kt` (`CustomChoicesStore` / `CustomChoicesDelegate`, JSON persistence, `defaultLabCatalogItems`, `defaultLabCategoryGroups`, `defaultMedicationCatalogItems`), `model/catalog/CustomChoiceModels.kt`, `model/ClinicalModels.kt` (`ClinicalVocabulary.testCategories`, `ClinicalVocabulary.medicationRoutes`)
  - `state/UndoCoordinator.kt`, `state/AncReducer.kt`, `state/AncState.kt`, `state/AncEvent.kt`, `state/AncViewModel.kt`, `state/ClinicalDraftReducers.kt`, `state/DraftMapping.kt`, `*Generator*`
  - `ui/SecuritySettingsDialog.kt` (level 3, Slice 18), `ui/ClinicalPresets.kt` (level 3, read-only), `ui/PatientEditor.kt` (level 3), `ui/Components.kt`, `ui/patienthub/components/EditorSheetParts.kt`, `ui/components/**`
  - `ui/LabEditor.kt`, `ui/LabBatchScreen.kt`, `ui/VisitEditor.kt`, `ui/patienthub/components/MedicationTapToAddSection.kt`, `ui/patienthub/components/MedicationForm.kt`, `ui/patienthub/model/MedicationTiles.kt`
- **Level 3 and catalog store invariants**:
  - `ui/CatalogSettingsSections.kt` is not a level-3 file. It is hosted inside `SecuritySettingsDialog.kt` (level 3, Slice 18, untouched) and reads `COMMON_MEDICATION_FREQUENCIES` and `medicationFrequencyLabel` from `ClinicalPresets.kt` (level 3, untouched).
  - **What must not change**:
    - **Delegate save, edit, delete, and reset paths**:
      - `catalogDelegate.addLabCatalogItem(newItem)`, `catalogDelegate.updateLabCatalogItem(editedItem)`, `catalogDelegate.deleteLabCatalogItem(it.id)`, and reset calling both `catalogDelegate.resetLabDefaults()` and `catalogDelegate.resetLabCategoryGroupDefaults()`.
      - `catalogDelegate.addLabCategoryGroup(newGroup)`, `catalogDelegate.updateLabCategoryGroup(editedGroup)`, `catalogDelegate.deleteLabCategoryGroup(it.id)`.
      - `catalogDelegate.addMedicationCatalogItem(newItem)`, `catalogDelegate.updateMedicationCatalogItem(editedItem)`, `catalogDelegate.deleteMedicationCatalogItem(it.id)`, `catalogDelegate.resetMedicationDefaults()`.
    - **Custom-choice store behaviour**: `CustomChoicesDelegate`, `CustomChoiceList`, `validateCustomChoice`, and `ClinicalCatalogRepository` custom-choice persistence (`CustomChoicesStoreInstrumentedTest`) remain untouched.
    - **Field copying, trimming, and validation rules**:
      - `AddLabTestDialog`: blank `testName.trim()` displays inline error without invoking `onSave`; editing uses `initial.copy(testName = trimmed, category = selectedCategory, unit = unit, referenceRange = referenceRange, defaultResult = defaultResult)`; creating constructs `LabCatalogItem(testName = trimmed, category = selectedCategory, unit = unit.trim(), referenceRange = referenceRange.trim(), defaultResult = defaultResult.trim(), isCustom = true)`; returning `false` from `onSave` shows inline error without dismissing.
      - `AddLabCategoryGroupDialog`: keeps `allSelectableTests` merging `availableTests` plus any legacy `initial?.testNames` missing from `availableTests` (`LabCatalogItem(id = "custom-$it", testName = it, category = "Other", isCustom = true)`), and keeps case-insensitive initial selection mapping against `availableTests`; validates non-blank `groupName.trim()` and non-empty `selectedTests.value`; sets `isCustom = true` on create and edit; returning `false` from `onSave` shows inline error without dismissing.
      - `AddMedicationDialog`: keeps `selectedRoute` defaulting to `initial?.route?.takeIf { it.isNotBlank() } ?: ClinicalVocabulary.medicationRoutes.first()`, `frequency` defaulting to `initial?.frequency?.takeIf { it.isNotBlank() } ?: "Once daily"`, chip list `(COMMON_MEDICATION_FREQUENCIES + listOfNotNull(initial?.frequency?.takeIf { it.isNotBlank() })).distinct()`, non-blank `name.trim()` validation, `isProphylaxis = false` on create (or `initial.copy(...)` on edit), `isCustom = true`, and inline error without dismissing when `onSave` returns `false`.
    - **Search filtering**: `LabCatalogSettingsSection` filters items on `testName` or `category` and groups on `name`, `description`, or `testNames.any` (case-insensitive); `MedicationCatalogSettingsSection` filters items on `name`, `dosage`, or `instructions` (case-insensitive); `AddLabCategoryGroupDialog` filters `allSelectableTests` on `testName` (case-insensitive).
- **Visual only**: Background-family (`FieldFamilies.background()`) styling, `12.sp` minimum typography (`RecordType.*`), `10.dp`/`16.dp` radii (`RecordShapes.Small`/`Large`), `48.dp` touch targets (`minimumTouchTarget()`), full-width item titles above metadata and actions, `RecordSectionCard` hairline rows for tests and medications, `RecordBandCard` + `BandChip` + `PlainField` for lab panels, scrollable `0.92f` form dialogs with pinned `SheetCancelSaveRow`, `AlertDialog` confirmations with `error` destructive buttons, empty/search recovery actions, and full `strings.xml` localization. No schema change, no store change, and no validation/save changes.

## Decisions for the owner (this slice)

1. **Form dialog container (`Dialog` with pinned header and `SheetCancelSaveRow` vs M3 `AlertDialog`)**:
   Today `AddLabTestDialog` has no `verticalScroll` (clipping on `360.dp` at `1.3f`/`2.0f` scale) and M3 `AlertDialog` squeezes inner width to `~264.dp` on `360.dp` screens, cramping side-by-side `Medication name` + `Dosage`, `Unit` + `Reference range`, and 28 panel test chips. **Recommended**: render the three Add/Edit dialogs with `Dialog(usePlatformDefaultWidth = false)` + `Surface(Modifier.fillMaxWidth(0.92f).heightIn(max = (screenHeightDp - 32).dp), shape = RecordShapes.Large, color = surface, border = BorderStroke(1.dp, outlineVariant))` using a fixed `RecordType.SectionTitle` header, scrollable body (`verticalScroll`), and pinned `HorizontalDivider` + `SheetCancelSaveRow` footer (`48.dp`, `RecordShapes.Small`), while keeping the 5 short Delete/Reset confirmations as `AlertDialog(shape = RecordShapes.Large, containerColor = surface)` (matching `DiscardChangesDialog` in Slice 11).
2. **Quiet white `PlainField` test list inside `RecordBandCard` for Lab Panels (replacing 20 grey pills per card)**:
   Today each `labCategoryGroup-*` card renders up to 20 separate `6.dp` `surfaceVariant` `11.sp` chips ("too many squares"). **Recommended**: render each panel as a `RecordBandCard(family = FieldFamilies.background())` whose sand header holds `BandChip("N tests")`, `group.name`, `48.dp` Edit/Delete buttons, and optional `group.description`, and whose quiet white body holds one `PlainField("Included tests", group.testNames.joinToString(", "))` (per `PLAN.md` Section 8: no box inside a box, no filled pill inside the body). Interactive multi-select test chips (`RecordFilterChip`) stay inside `AddLabCategoryGroupDialog`.
3. **Full-width primary action button and compact switcher chips**:
   Today `"Add Test"` / `"Create Panel"` / `"Add Medication"` (`40.dp`, `softWrap = false`) sit beside the title, squeezing `"My medication list"` inside `SecuritySettingsDialog`'s `~298.dp` inner width, while `"Individual Tests (28)"` and `"Panels & Profiles (7)"` sit in a non-wrapping `Row`. **Recommended**: give the section title and count summary full width, place `"Individual tests (%1$d)"` and `"Panels (%1$d)"` in a wrapping `FlowRow` of `48.dp` `RecordFilterChip`s, and place `"Add test"` / `"Create panel"` / `"Add medication"` below as a full-width `48.dp` `RecordShapes.Small` primary `Button` (dropping the decorative `+` icon, matching `RecordListHeader`).
4. **`RecordSectionCard` hairline rows with full-width titles for Individual tests and Medications**:
   Today 28 tests and 10 medications each render as a `12.dp` grey `surfaceContainerLow` card with `96.dp` of Edit/Delete icons narrowing long names like `"Hepatitis B Surface Antigen (HBsAg)"` and `11.sp` rose category/route chips. **Recommended**: group tests and medications inside a `RecordSectionCard(family = FieldFamilies.background())` with hairline-separated rows: full-width `RecordType.CardName` title on line 1, paired below with `12.sp` sand `FormFieldLabel` + `14.sp` `RecordType.Secondary` details on the left and `48.dp` Edit/Delete `IconButton`s on the right (matching `MedicationCard`'s full-width-title layout).
5. **Recovery actions on empty/no-result states, red destructive actions, and string normalization**:
   Today empty and search-no-result cards show passive text with no `"Clear search"` or `"Add"` button, Delete/Reset confirmations use primary navy `"Delete"`, `create_diagnostic_panel` reads `"Create New Diagnostic Panel"`, and nearly all text in `CatalogSettingsSections.kt` is hardcoded English with `"·"` separators. **Recommended**: add `"Clear search"` (`OutlinedButton`, `48.dp`) on search-no-result states (plus `"Add ..."` / `"Reset to defaults"` when empty, and inline `"Clear search"` inside `AddLabCategoryGroupDialog` when test search misses); color `"Delete"` and `"Reset"` in `MaterialTheme.colorScheme.error`; normalize `create_diagnostic_panel` to `"Create diagnostic panel"`; replace `"·"` with `", "`; and move all UI text into `strings.xml`.

## Lessons (apply all)

- **File size ceiling**: `CatalogSettingsSections.kt` is `1,286` lines today (`PLAN.md` ceiling `1,500`). Consolidating repeated dialog/card/confirm boilerplate into private helpers (`CatalogFormDialog`, `CatalogLabeledField`, `CatalogSearchField`, `CatalogEmptyCard`, `CatalogConfirmDialog`) keeps the file around `950-1,100` lines, safely under `1,500`.
- **Minimum `12.sp` everywhere** (`PLAN.md` 0, 2): eliminate all `labelSmall` (`11.sp`), `labelMedium` (`11.sp`), and the hardcoded `fontSize = 11.sp` on `AddMedicationDialog`'s frequency chips (line 1214).
- **Minimum `48.dp` width and height** (`minimumTouchTarget()`) on every interactive target: `addNewLabTestBtn`, `addNewLabGroupBtn`, `addNewMedBtn`, `labIndividualTestsTab`, `labCategoryGroupsTab`, search clear buttons, `editLabCodexBtn-*`, `deleteLabCodexBtn-*`, `editLabGroupBtn-*`, `deleteLabGroupBtn-*`, `editMedCodexBtn-*`, `deleteMedCodexBtn-*`, `Reset ... to defaults`, empty/no-result recovery buttons, all category/route/frequency/test `RecordFilterChip`s, `SheetCancelSaveRow` buttons, and Delete/Reset `AlertDialog` buttons.
- **Two corner radii only**: `RecordShapes.Small` (`10.dp`) on search fields, inputs, chips, buttons, and `BandChip`s; `RecordShapes.Large` (`16.dp`) on `RecordSectionCard`, `RecordBandCard`, empty/no-result `OutlinedCard`s, and dialogs. Remove `4.dp`, `6.dp`, `8.dp`, `12.dp`, `28.dp`, and `999.dp` pill shapes.
- **Never use `LazyColumn` inside `LabCatalogSettingsSection` or `MedicationCatalogSettingsSection`**: `SecuritySettingsDialog` renders them inside a `verticalScroll(rememberScrollState())` `Column`; keep plain `Column` lists to avoid nested-scroll crashes.
- **Add `verticalScroll(rememberScrollState())` to `AddLabTestDialog`**: `AddLabTestDialog` lacks vertical scroll today and risks clipping at `360.dp` with `1.3f`/`2.0f` font scale or soft keyboard open; sharing `CatalogFormDialog` fixes all three form dialogs uniformly.
- **360 dp at `1.3f` and `2.0f` font scale**: in `AddLabTestDialog` (`Unit` + `Reference range`) and `AddMedicationDialog` (`Medication name` + `Dosage`), keep fields side-by-side at `<= 1.3f` font scale and stack vertically (`Column(spacedBy(12.dp))`) when `LocalDensity.current.fontScale > 1.3f`. Always wrap chip rows in `FlowRow` with `RecordFilterChip(..., wrap = true, minTouchTarget = true)`.
- **Keep every existing `testTag` on the exact semantic node**: keep `.testTag(...)` directly on `OutlinedTextField` (NOT on external-label wrapper `Column`s) for `searchLabCodexField`, `searchMedCodexField`, `addLabNameField`, `addLabGroupNameField`, `addLabGroupDescField`, `addMedNameField`, and `addMedDosageField`, and preserve every list/item/button/chip tag listed in the Acceptance checklist.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Lines | Change |
|---|---:|---|
| `res/values/strings.xml` | 851 | Normalize 1 value (`create_diagnostic_panel`); add strings in section 1 (no hardcoded UI text left in `CatalogSettingsSections.kt`) |
| `ui/CatalogSettingsSections.kt` | 1,286 | Restyle `LabCatalogSettingsSection`, `MedicationCatalogSettingsSection`, `AddLabTestDialog`, `AddLabCategoryGroupDialog`, `AddMedicationDialog`, and delete/reset `AlertDialog`s using `FieldFamilies.background()`, `RecordType`, `RecordShapes`, `RecordSectionCard`, `RecordBandCard`, `BandChip`, `PlainField`, `FormFieldLabel`, `RecordFilterChip`, `SheetCancelSaveRow`, `editorFieldColors()`, `minimumTouchTarget()`, and `medicationFrequencyLabel()` |

No edits needed to `ui/components/*`, `ui/Components.kt`, `ui/patienthub/components/EditorSheetParts.kt`, `ui/ClinicalPresets.kt`, or `ui/SecuritySettingsDialog.kt`.

## 1. Strings (`app/src/main/res/values/strings.xml`)

Reuse existing keys directly (do NOT duplicate or modify their values):
- `clear_search`, `save`, `cancel`, `delete`, `add_test_action`, `delete_test`, `test_name`, `test_category`, `unit`, `reference_range`, `add_medication`, `edit_medication_title`, `edit_medication_description`, `medication_name`, `dosage`, `route`, `frequency`, `edit_diagnostic_panel`, `lab_batch_panel_name`.

Change value (key unchanged; grep confirmed zero tests assert the old Title Case value):
- `create_diagnostic_panel`: `"Create New Diagnostic Panel"` -> `"Create diagnostic panel"`

Add (all sentence case, English default, no middle dots or em dashes):
```xml
<string name="catalog_lab_section_title">Laboratory tests</string>
<string name="catalog_lab_summary">%1$d tests, %2$d panels</string>
<string name="catalog_create_panel">Create panel</string>
<string name="catalog_tab_individual_tests">Individual tests (%1$d)</string>
<string name="catalog_tab_panels">Panels (%1$d)</string>
<string name="catalog_search_lab_tests">Search tests or categories</string>
<string name="catalog_search_panels">Search diagnostic panels</string>
<string name="catalog_tests_list_title">Tests</string>
<string name="catalog_tests_matches_title">Tests (%1$d matches)</string>
<string name="catalog_test_unit_line">Unit: %1$s</string>
<string name="catalog_test_default_line">Default: %1$s</string>
<string name="catalog_test_meta_pair">%1$s, %2$s</string>
<string name="catalog_edit_test_cd">Edit %1$s</string>
<string name="catalog_delete_panel_cd">Delete panel %1$s</string>
<string name="catalog_panel_tests_count">%1$d tests</string>
<string name="catalog_included_tests">Included tests</string>
<string name="catalog_reset_labs">Reset lab tests to defaults</string>
<string name="catalog_empty_labs_title">No lab tests in the catalog</string>
<string name="catalog_empty_labs_hint">Add a test or restore the default clinic tests.</string>
<string name="catalog_empty_panels_title">No lab panels yet</string>
<string name="catalog_empty_panels_hint">Create a panel to group tests that are ordered together.</string>
<string name="catalog_no_tests_match">No tests match \"%1$s\"</string>
<string name="catalog_no_panels_match">No panels match \"%1$s\"</string>
<string name="catalog_search_no_match_hint">Check the spelling or clear the search filter.</string>
<string name="catalog_reset_to_defaults_action">Reset to defaults</string>

<string name="catalog_med_section_title">My medication list</string>
<string name="catalog_med_summary">%1$d medications, available for all patient plans</string>
<string name="catalog_search_meds">Search medications or dosages</string>
<string name="catalog_meds_list_title">Medications</string>
<string name="catalog_meds_matches_title">Medications (%1$d matches)</string>
<string name="catalog_med_route_frequency">%1$s, %2$s</string>
<string name="catalog_delete_med_cd">Delete %1$s</string>
<string name="catalog_reset_meds">Reset medication list to defaults</string>
<string name="catalog_empty_meds_title">No medications in your list</string>
<string name="catalog_empty_meds_hint">Add a medication or restore the standard clinical defaults.</string>
<string name="catalog_no_meds_match">No medications match \"%1$s\"</string>

<string name="catalog_delete_test_title">Delete lab test?</string>
<string name="catalog_delete_test_body">Delete \"%1$s\" from the clinic lab catalog?</string>
<string name="catalog_delete_panel_title">Delete lab panel?</string>
<string name="catalog_delete_panel_body">Delete panel \"%1$s\" from the clinic lab catalog?</string>
<string name="catalog_reset_labs_title">Reset lab catalog?</string>
<string name="catalog_reset_labs_body">This restores the clinic lab tests and panels to standard antenatal defaults.</string>
<string name="catalog_delete_med_title">Delete medication?</string>
<string name="catalog_delete_med_body">Delete \"%1$s\" from the medication list?</string>
<string name="catalog_reset_meds_title">Reset medications?</string>
<string name="catalog_reset_meds_body">This restores the medication list to standard maternal care defaults.</string>
<string name="catalog_reset_action">Reset</string>

<string name="catalog_add_lab_test_title">Add lab test</string>
<string name="catalog_edit_lab_test_title">Edit lab test</string>
<string name="catalog_test_name_required">Test name (required)</string>
<string name="catalog_test_name_placeholder">e.g. Haemoglobin (Hb) or TSH</string>
<string name="catalog_unit_placeholder">g/dL</string>
<string name="catalog_ref_range_placeholder">11.0 - 15.0</string>
<string name="catalog_default_result_optional">Default result (optional)</string>
<string name="catalog_default_result_placeholder">e.g. Negative or Normal</string>
<string name="catalog_error_test_name_blank">Please enter a test name.</string>
<string name="catalog_error_test_duplicate">Test name already used or invalid.</string>
<string name="catalog_error_test_save">Unable to save test.</string>

<string name="catalog_panel_name_required">Panel name (required)</string>
<string name="catalog_panel_name_placeholder">e.g. Hypertension follow-up or GDM screening</string>
<string name="catalog_panel_desc_optional">Description or indication (optional)</string>
<string name="catalog_panel_desc_placeholder">e.g. Panel requested at 28 weeks gestation</string>
<string name="catalog_select_tests_count">Select tests (%1$d selected)</string>
<string name="catalog_search_available_tests">Search available tests</string>
<string name="catalog_no_available_tests_match">No available tests match \"%1$s\"</string>
<string name="catalog_save_panel">Save panel</string>
<string name="catalog_error_panel_name_blank">Please enter a panel name.</string>
<string name="catalog_error_panel_no_tests">Please select at least one test.</string>
<string name="catalog_error_panel_duplicate">Panel name already used or invalid.</string>
<string name="catalog_error_panel_save">Unable to save panel.</string>

<string name="catalog_med_name_required">Medication name (required)</string>
<string name="catalog_med_name_placeholder">e.g. Paracetamol</string>
<string name="catalog_med_dosage_placeholder">e.g. 500 mg</string>
<string name="catalog_med_route_label">Administration route</string>
<string name="catalog_med_instructions_optional">Instructions or indication (optional)</string>
<string name="catalog_med_instructions_placeholder">e.g. Analgesic for pregnancy</string>
<string name="catalog_add_to_my_list">Add to my list</string>
<string name="catalog_error_med_name_blank">Please enter a medication name.</string>
<string name="catalog_error_med_duplicate">Medication already exists or is invalid.</string>
<string name="catalog_error_med_save">Unable to save medication.</string>
```
Check singular/plural formatting: for `1 match` vs `N matches`, either add `<plurals name="catalog_matches_count">` (`%d match` / `%d matches`) or use `<plurals>` for `catalog_tests_matches_title`, `catalog_meds_matches_title`, `catalog_tab_individual_tests`, `catalog_tab_panels`, `catalog_panel_tests_count` so singular `1` never reads `"1 matches"` or `"1 tests"` (per `PLAN.md` Decision 8 lesson!). Recommended: use `<plurals>` for all count strings (`Tests (%d match)` / `Tests (%d matches)`, `%d test` / `%d tests`, `%d panel` / `%d panels`, `%d medication` / `%d medications`).

## 2. Shared private helpers (`ui/CatalogSettingsSections.kt`)

Add these private helpers inside `CatalogSettingsSections.kt` (all wrapped in `FieldFamilies.background()`):

1. **`CatalogFormDialog`**:
   - Uses `Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false))`.
   - `val maxHeight = (LocalConfiguration.current.screenHeightDp - 32).dp`
   - `Surface(modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = maxHeight).let { if (dialogTag != null) it.testTag(dialogTag) else it }, shape = RecordShapes.Large, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))`
   - Inside `CompositionLocalProvider(LocalFieldFamily provides FieldFamilies.background())`:
     - `Column(Modifier.fillMaxWidth())`:
       - Fixed header: `Text(text = title, style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).semantics { heading() })`
       - `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)`
       - Scrollable body: `Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)`
       - `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)`
       - Pinned footer: `SheetCancelSaveRow(enabled = true, saving = false, saveEnabled = true, savingLabel = confirmLabel, onCancel = onDismiss, onSave = onConfirm, cancelTag = cancelTag, saveTag = confirmTag, spinnerTag = "${confirmTag}Spinner", modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))`

2. **`CatalogLabeledField`**:
   - Wraps `Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp))`:
     - `Box(modifier = Modifier.clearAndSetSemantics {}) { if (isError) Text(label, style = RecordType.FieldLabel, color = MaterialTheme.colorScheme.error) else FormFieldLabel(label) }`
     - `OutlinedTextField(value = value, onValueChange = onValueChange, placeholder = placeholder?.let { { Text(it, style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, isError = isError, singleLine = singleLine, shape = RecordShapes.Small, textStyle = RecordType.FieldValue, colors = editorFieldColors(), modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }.testTag(tag))`
   - Note: `.testTag(tag)` stays directly on `OutlinedTextField` (NOT on the wrapper `Column`).

3. **`CatalogSearchField`**:
   - `OutlinedTextField(value = query, onValueChange = onQueryChange, placeholder = { Text(placeholder, style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }, trailingIcon = if (query.isNotBlank()) { { IconButton(onClick = { onQueryChange("") }, modifier = Modifier.minimumTouchTarget().testTag(clearTag)) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search), tint = MaterialTheme.colorScheme.onSurfaceVariant) } } } else null, singleLine = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue, colors = editorFieldColors(), modifier = modifier.fillMaxWidth().testTag(tag))`

4. **`CatalogEmptyCard`**:
   - `OutlinedCard(shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = modifier.fillMaxWidth())`
   - Inside `Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp))`:
     - `Text(title, style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface)`
     - `Text(body, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`
     - `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp))` with primary recovery `OutlinedButton(shape = RecordShapes.Small, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.minimumTouchTarget())` and optional secondary `TextButton(shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget())`.

5. **`CatalogConfirmDialog`**:
   - Matches `DiscardChangesDialog` (`EditorSheetParts.kt`, Slice 11):
   - `AlertDialog(onDismissRequest = onDismiss, shape = RecordShapes.Large, containerColor = MaterialTheme.colorScheme.surface, title = { Text(title, style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.onSurface) }, text = { Text(body, style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) }, confirmButton = { TextButton(onClick = onConfirm, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().let { if (confirmTag != null) it.testTag(confirmTag) else it }) { Text(confirmLabel, style = RecordType.Action, color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = onDismiss, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().let { if (dismissTag != null) it.testTag(dismissTag) else it }) { Text(stringResource(R.string.cancel), style = RecordType.Action, color = MaterialTheme.colorScheme.primary) } }, modifier = if (dialogTag != null) Modifier.testTag(dialogTag) else Modifier)`

## 3. `LabCatalogSettingsSection` (`ui/CatalogSettingsSections.kt`, same signature)

Keep `LabCatalogSubView`, `items`, `categoryGroups`, `currentSubView`, `searchQuery`, `filteredItems`, `filteredGroups`, and all dialog states (`showAddTestDialog`, `itemToEdit`, `showAddGroupDialog`, `groupToEdit`, `itemToDelete`, `groupToDelete`, `showResetDialog`). Wrap content in `CompositionLocalProvider(LocalFieldFamily provides FieldFamilies.background())`:

1. **Section header**:
   - `Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp))`:
     - `Text(stringResource(R.string.catalog_lab_section_title), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })`
     - `Text(summary, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)` (using comma separator, no middle dot `"·"`).
2. **Subview switcher (`FlowRow`, `spacedBy(8.dp)`)**:
   - `RecordFilterChip(selected = currentSubView == LabCatalogSubView.INDIVIDUAL_TESTS, label = pluralStringResource(R.plurals.catalog_tab_individual_tests, items.size, items.size), onClick = { currentSubView = LabCatalogSubView.INDIVIDUAL_TESTS }, tag = "labIndividualTestsTab", wrap = true, minTouchTarget = true)`
   - `RecordFilterChip(selected = currentSubView == LabCatalogSubView.CATEGORY_GROUPS, label = pluralStringResource(R.plurals.catalog_tab_panels, categoryGroups.size, categoryGroups.size), onClick = { currentSubView = LabCatalogSubView.CATEGORY_GROUPS }, tag = "labCategoryGroupsTab", wrap = true, minTouchTarget = true)`
3. **Primary CTA (`48.dp` full-width `Button`)**:
   - If `INDIVIDUAL_TESTS`: `Button(onClick = { showAddTestDialog = true }, shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("addNewLabTestBtn")) { Text(stringResource(R.string.add_test_action), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) }`
   - Else: `Button(onClick = { showAddGroupDialog = true }, shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("addNewLabGroupBtn")) { Text(stringResource(R.string.catalog_create_panel), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) }`
4. **Search field**:
   - `CatalogSearchField(query = searchQuery, onQueryChange = { searchQuery = it }, placeholder = stringResource(if (currentSubView == LabCatalogSubView.INDIVIDUAL_TESTS) R.string.catalog_search_lab_tests else R.string.catalog_search_panels), tag = "searchLabCodexField", clearTag = "clearSearchLabCodexBtn")`
5. **Individual tests list (`INDIVIDUAL_TESTS`)**:
   - When `filteredItems.isEmpty()`: `CatalogEmptyCard`:
     - If `searchQuery.isNotBlank()`: title `stringResource(R.string.catalog_no_tests_match, searchQuery)`, body `catalog_search_no_match_hint`, button `clear_search` (`searchQuery = ""`, `testTag("clearLabSearchAction")`).
     - Else: title `catalog_empty_labs_title`, body `catalog_empty_labs_hint`, buttons `add_test_action` (`showAddTestDialog = true`) + `catalog_reset_to_defaults_action` (`showResetDialog = true`).
   - Else: `RecordSectionCard(title = ..., family = bgFamily)` (title shows `Tests` or `Tests (N matches)` when searching) containing each `lab` with `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)` between rows (`index > 0`):
     - `Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("labCodexItem-${lab.id}"), verticalArrangement = Arrangement.spacedBy(4.dp))`:
       - Top line (full width): `Text(lab.testName, style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface)`
       - Bottom `Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically)`:
         - Left `Column(Modifier.weight(1f), Arrangement.spacedBy(2.dp))`:
           - `FormFieldLabel(lab.category)` (`12.sp` SemiBold `#594A2C` / `#E0CFA6`)
           - Optional secondary line in `RecordType.Secondary` (`14.sp`, `onSurfaceVariant`) built from `referenceRange` / `unit` (e.g. `"70 - 95 mg/dL"` or `"Unit: g/dL"`) and `defaultResult` (e.g. `"Default: Negative"`), joined by `", "` (zero `"·"`).
         - Right `Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically)`:
           - `IconButton(onClick = { itemToEdit = lab }, modifier = Modifier.minimumTouchTarget().testTag("editLabCodexBtn-${lab.id}"))` (`Icons.Default.Edit`, tint `primary`, `contentDescription = stringResource(R.string.catalog_edit_test_cd, lab.testName)`)
           - `IconButton(onClick = { itemToDelete = lab }, modifier = Modifier.minimumTouchTarget().testTag("deleteLabCodexBtn-${lab.id}"))` (`Icons.Default.Delete`, tint `error`, `contentDescription = stringResource(R.string.delete_test, lab.testName)`)
6. **Category groups / Panels list (`CATEGORY_GROUPS`)**:
   - When `filteredGroups.isEmpty()`: `CatalogEmptyCard` (no-match with `Clear search` or empty with `Create panel` + `Reset to defaults`).
   - Else: `Column(verticalArrangement = Arrangement.spacedBy(12.dp))` of `filteredGroups.forEach { group -> ... }`:
     - `RecordBandCard(title = group.name, family = bgFamily, subtitle = group.description.takeIf { it.isNotBlank() }, modifier = Modifier.testTag("labCategoryGroup-${group.id}"), chips = { BandChip(text = pluralStringResource(R.plurals.catalog_panel_tests_count, group.testNames.size, group.testNames.size), tone = null) }, actions = { editLabGroupBtn + deleteLabGroupBtn })`:
       - Inside body: `PlainField(label = stringResource(R.string.catalog_included_tests), value = group.testNames.joinToString(", "))`.
7. **Reset button**:
   - `TextButton(onClick = { showResetDialog = true }, shape = RecordShapes.Small, modifier = Modifier.align(Alignment.CenterHorizontally).minimumTouchTarget().testTag("resetLabCatalogBtn")) { Text(stringResource(R.string.catalog_reset_labs), style = RecordType.Action, color = MaterialTheme.colorScheme.primary) }` (drops decorative `RestartAlt` icon).
8. **Confirmations (`CatalogConfirmDialog`)**:
   - `itemToDelete != null`: `deleteLabConfirmDialog`, `deleteLabConfirmBtn` (`Delete` in `error`).
   - `groupToDelete != null`: `deleteLabGroupConfirmDialog`, `deleteLabGroupConfirmBtn` (`Delete` in `error`).
   - `showResetDialog`: `resetLabConfirmDialog`, `resetLabConfirmBtn` (`Reset` in `error`), calling both `resetLabDefaults()` and `resetLabCategoryGroupDefaults()`.

## 4. `MedicationCatalogSettingsSection` (`ui/CatalogSettingsSections.kt`, same signature)

Keep `items`, `searchQuery`, `filteredItems`, `showAddDialog`, `itemToEdit`, `itemToDelete`, `showResetDialog`. Wrap in `CompositionLocalProvider(LocalFieldFamily provides FieldFamilies.background())`:

1. **Section header**: `RecordType.SectionTitle` (`My medication list`, `primary`, `.semantics { heading() }`) + `RecordType.Secondary` summary (`%d medications, available for all patient plans`, comma instead of `"·"`).
2. **Primary CTA**: full-width `Button(onClick = { showAddDialog = true }, shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("addNewMedBtn")) { Text(stringResource(R.string.add_medication), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) }`.
3. **Search field**: `CatalogSearchField(..., tag = "searchMedCodexField", clearTag = "clearSearchMedCodexBtn")`.
4. **Medications list**:
   - When `filteredItems.isEmpty()`: `CatalogEmptyCard` (no-match with `Clear search` or empty with `Add medication` + `Reset to defaults`).
   - Else: `RecordSectionCard(title = ..., family = bgFamily)` with hairline-separated rows (`medCodexItem-${med.id}`):
     - Top line (full width): `Text(med.displayLabel, style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface)` (`"Paracetamol 500 mg"`).
     - Bottom `Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically)`:
       - Left `Column(Modifier.weight(1f), Arrangement.spacedBy(2.dp))`:
         - `FormFieldLabel(stringResource(R.string.catalog_med_route_frequency, med.route, medicationFrequencyLabel(med.frequency)))` (`12.sp` SemiBold `#594A2C` / `#E0CFA6`, e.g. `"Oral, As needed (PRN)"`)
         - If `med.instructions.isNotBlank()`: `Text(med.instructions, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`
       - Right `Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically)`:
         - `IconButton(editMedCodexBtn-${med.id}, minimumTouchTarget(), edit_medication_description)`
         - `IconButton(deleteMedCodexBtn-${med.id}, minimumTouchTarget(), catalog_delete_med_cd, tint = error)`
5. **Reset button**: `TextButton(..., shape = RecordShapes.Small, modifier = Modifier.align(Alignment.CenterHorizontally).minimumTouchTarget().testTag("resetMedCatalogBtn"))`.
6. **Confirmations (`CatalogConfirmDialog`)**: `itemToDelete` (`deleteMedConfirmDialog`, `deleteMedConfirmBtn`) and `showResetDialog` (`resetMedConfirmDialog`, `resetMedConfirmBtn`).

## 5. Three Add/Edit Dialogs (`ui/CatalogSettingsSections.kt`)

Define `val narrowStacked = LocalDensity.current.fontScale > 1.3f` inside `AddLabTestDialog` and `AddMedicationDialog`.

### 5.1 `AddLabTestDialog`
- Uses `CatalogFormDialog(title = stringResource(if (initial != null) R.string.catalog_edit_lab_test_title else R.string.catalog_add_lab_test_title), confirmLabel = stringResource(R.string.save), confirmTag = "saveLabCodexConfirmBtn", cancelTag = "cancelLabCodexBtn", dialogTag = "addLabTestDialog", onDismiss = onDismiss, onConfirm = <unchanged validation & onSave>)`.
- Inside scrollable body:
  1. `CatalogLabeledField(value = testName, onValueChange = { testName = it; errorMessage = null }, label = stringResource(R.string.catalog_test_name_required), placeholder = stringResource(R.string.catalog_test_name_placeholder), isError = errorMessage != null && testName.isBlank(), tag = "addLabNameField")`.
  2. `FormFieldLabel(stringResource(R.string.test_category))` + `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp))` of `RecordFilterChip(selected = selectedCategory == cat, label = cat, onClick = { selectedCategory = cat }, tag = "labCodexCategoryChoice-$index", wrap = true, minTouchTarget = true)` for `ClinicalVocabulary.testCategories`.
  3. `Unit` (`tag = "addLabUnitField"`) and `Reference range` (`tag = "addLabRefRangeField"`):
     - If `!narrowStacked`: `Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp))` with `Modifier.weight(1f)` on `Unit` and `Modifier.weight(1.5f)` on `Reference range`.
     - Else (`fontScale > 1.3f`): stacked vertically in `Column(spacedBy(12.dp))`.
  4. `CatalogLabeledField(value = defaultResult, ..., label = stringResource(R.string.catalog_default_result_optional), placeholder = stringResource(R.string.catalog_default_result_placeholder), tag = "addLabDefaultResultField")`.
  5. If `errorMessage != null`: `Text(errorMessage!!, style = RecordType.Action, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("addLabError"))`.

### 5.2 `AddLabCategoryGroupDialog`
- Uses `CatalogFormDialog(title = stringResource(if (initial != null) R.string.edit_diagnostic_panel else R.string.create_diagnostic_panel), confirmLabel = stringResource(if (initial != null) R.string.save else R.string.catalog_save_panel), confirmTag = "saveLabGroupConfirmBtn", cancelTag = "cancelLabGroupBtn", dialogTag = "addLabGroupDialog", onDismiss = onDismiss, onConfirm = <unchanged validation & onSave>)`.
- Inside scrollable body:
  1. `CatalogLabeledField(value = groupName, ..., label = stringResource(R.string.catalog_panel_name_required), placeholder = stringResource(R.string.catalog_panel_name_placeholder), isError = errorMessage != null && groupName.isBlank(), tag = "addLabGroupNameField")`.
  2. `CatalogLabeledField(value = groupDescription, ..., label = stringResource(R.string.catalog_panel_desc_optional), placeholder = stringResource(R.string.catalog_panel_desc_placeholder), tag = "addLabGroupDescField")`.
  3. `FormFieldLabel(stringResource(R.string.catalog_select_tests_count, selectedTests.value.size))`.
  4. `CatalogSearchField(query = testSearch, onQueryChange = { testSearch = it }, placeholder = stringResource(R.string.catalog_search_available_tests), tag = "searchLabGroupTestsField", clearTag = "clearLabGroupTestsSearchBtn")`.
  5. If `filteredTests.isEmpty()`: `Text(stringResource(R.string.catalog_no_available_tests_match, testSearch), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)` + `TextButton(onClick = { testSearch = "" }, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget()) { Text(stringResource(R.string.clear_search), style = RecordType.Action) }`.
  6. Else: `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth())` of `RecordFilterChip(selected = isSelected, label = test.testName, onClick = <unchanged toggle>, tag = "selectTestChip-${test.id}", wrap = true, minTouchTarget = true)`.
  7. If `errorMessage != null`: `Text(errorMessage!!, style = RecordType.Action, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("addLabGroupError"))`.

### 5.3 `AddMedicationDialog`
- Uses `CatalogFormDialog(title = stringResource(if (initial != null) R.string.edit_medication_title else R.string.add_medication), confirmLabel = stringResource(if (initial != null) R.string.save else R.string.catalog_add_to_my_list), confirmTag = "saveMedCodexConfirmBtn", cancelTag = "cancelMedCodexBtn", dialogTag = "addMedDialog", onDismiss = onDismiss, onConfirm = <unchanged validation & onSave>)`.
- Inside scrollable body:
  1. `Medication name (required)` (`tag = "addMedNameField"`) and `Dosage` (`tag = "addMedDosageField"`):
     - If `!narrowStacked` (`fontScale <= 1.3f`): `Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp))` with `Modifier.weight(1.5f)` on `Medication name` and `Modifier.weight(1f)` on `Dosage` (side by side).
     - Else (`fontScale > 1.3f`): stacked vertically in `Column(spacedBy(12.dp))`.
  2. `FormFieldLabel(stringResource(R.string.catalog_med_route_label))` + `FlowRow(spacedBy(8.dp))` of `RecordFilterChip(selected = selectedRoute == r, label = r, onClick = { selectedRoute = r }, tag = "medCodexRouteChoice-$index", wrap = true, minTouchTarget = true)` for `ClinicalVocabulary.medicationRoutes`.
  3. `FormFieldLabel(stringResource(R.string.frequency))` + `FlowRow(spacedBy(8.dp))` of `RecordFilterChip(selected = frequency == freq, label = medicationFrequencyLabel(freq), onClick = { frequency = freq }, tag = "medCodexFreqChoice-$index", wrap = true, minTouchTarget = true)` for `(COMMON_MEDICATION_FREQUENCIES + listOfNotNull(initial?.frequency?.takeIf { it.isNotBlank() })).distinct()`.
  4. `CatalogLabeledField(value = instructions, ..., label = stringResource(R.string.catalog_med_instructions_optional), placeholder = stringResource(R.string.catalog_med_instructions_placeholder), tag = "addMedInstructionsField")`.
  5. If `errorMessage != null`: `Text(errorMessage!!, style = RecordType.Action, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("addMedError"))`.

## Acceptance checklist

1. Every existing `testTag` in `CatalogSettingsSections.kt` (all 25) is kept on its exact semantic node:
   - `addNewLabTestBtn`, `addNewLabGroupBtn`, `labIndividualTestsTab`, `labCategoryGroupsTab`, `searchLabCodexField`, `labCodexItem-${lab.id}`, `editLabCodexBtn-${lab.id}`, `deleteLabCodexBtn-${lab.id}`, `labCategoryGroup-${group.id}`, `editLabGroupBtn-${group.id}`, `deleteLabGroupBtn-${group.id}`, `addLabNameField`, `saveLabCodexConfirmBtn`, `addLabGroupNameField`, `addLabGroupDescField`, `selectTestChip-${test.id}`, `saveLabGroupConfirmBtn`, `addNewMedBtn`, `searchMedCodexField`, `medCodexItem-${med.id}`, `editMedCodexBtn-${med.id}`, `deleteMedCodexBtn-${med.id}`, `addMedNameField`, `addMedDosageField`, `saveMedCodexConfirmBtn`.
2. Zero text nodes under `12.sp` across all catalog views and dialogs; zero hardcoded user-facing strings; zero `"·"` middle dots; zero Title Case field/button labels; zero `4.dp`, `6.dp`, `8.dp`, `12.dp`, `28.dp`, or `999.dp` pill shapes.
3. Every interactive target (`addNewLabTestBtn`, `addNewLabGroupBtn`, `addNewMedBtn`, `labIndividualTestsTab`, `labCategoryGroupsTab`, clear search buttons, `edit*`/`delete*` icon buttons, `RecordFilterChip`s, `SheetCancelSaveRow` buttons, confirmation buttons, reset buttons, and empty-state buttons) measures `>= 48.dp` (`>= 47.9.dp` in instrumented checks) and fits cleanly inside both a `360.dp` standalone host and `SecuritySettingsDialog`'s `~298.dp` content width at `1.0f`, `1.3f`, and `2.0f` font scale with zero clipped text.
4. `AddMedicationDialog` keeps `addMedNameField` and `addMedDosageField` side by side at `1.0f` and `1.3f` font scale, and stacks vertically above `1.3f`. All three form dialogs scroll safely with pinned `SheetCancelSaveRow` footers.
5. `LabCatalogDelegate`, `MedicationCatalogDelegate`, `CustomChoicesDelegate`, duplicate-name protection, legacy panel test ID mapping (`custom-$it`), and reset-to-defaults behaviour work identically to today.
6. Dark theme (`AncTheme(darkTheme = true)`) and `LayoutDirection.Rtl` render cleanly with WCAG AA contrast verified across Background (sand) tokens.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. Repo grep confirmed zero existing tests assert the old hardcoded English strings or `create_diagnostic_panel` Title Case value.

### Must pass unchanged

- `app/src/androidTest/java/com/sherifradwan/ancnative/CustomChoicesStoreInstrumentedTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/SecurityWorkflowTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/MedsTuneTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/MedicationFormTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/LabBatchUiTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/LabEntryDesignTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/catalog/CustomChoicesLogicTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogRepositoryTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/catalog/CatalogEditHelpersTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/catalog/LabItemEditTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/catalog/LabPanelResolveTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/catalog/LabPanelQueueTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/RecordTokensContrastTest.kt`

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/CatalogSettingsRedesignTest.kt`

Mount `LabCatalogSettingsSection`, `MedicationCatalogSettingsSection`, and `SecuritySettingsDialog` through `MainActivity` + `AncTheme` inside a `360.dp x 640.dp` host (matching `LabEditorRedesignTest` and `ComposeTextAsserts`, single `setContent` per test):
- **Typography & legacy removal (`1.0f` and `1.3f`)**: walk semantics trees of `LabCatalogSettingsSection` (both `INDIVIDUAL_TESTS` and `CATEGORY_GROUPS`), `MedicationCatalogSettingsSection`, and all three Add/Edit dialogs; assert zero `TextLayoutResult` nodes have `fontSize < 12.sp`, and no `"·"` or `"Create New Diagnostic Panel"` appears.
- **360 dp (`298 dp` inside `SecuritySettingsDialog`), `1.3f`, and `2.0f` touch targets**: assert `addNewLabTestBtn`, `addNewLabGroupBtn`, `addNewMedBtn`, `labIndividualTestsTab`, `labCategoryGroupsTab`, `editLabCodexBtn-*`, `deleteLabCodexBtn-*`, `editLabGroupBtn-*`, `deleteLabGroupBtn-*`, `editMedCodexBtn-*`, `deleteMedCodexBtn-*`, `saveLabCodexConfirmBtn`, `saveLabGroupConfirmBtn`, `saveMedCodexConfirmBtn`, and `selectTestChip-*` all measure `>= 47.9.dp` and stay within the `360.dp` host bounds with zero clipped text.
- **Individual tests & Panels flow**: verify `Haemoglobin (Hb)` and `CBC` render; test `searchLabCodexField` filtering (`"Hb"`), no-match empty state (`"Vitamin B12"`) with `"Clear search"` restoring items, empty catalog state showing `"Add test"` / `"Reset to defaults"`; verify `AddLabTestDialog` validation (blank name error), create, edit (`editLabCodexBtn-*`), delete confirmation (`deleteLabCodexBtn-*`), and reset confirmation.
- **Panels & test chips flow**: open `AddLabCategoryGroupDialog`, verify `selectTestChip-*` toggle, test search + no-match `"Clear search"`, blank name / zero-test validation errors, save (`saveLabGroupConfirmBtn`), and `PlainField` test list inside `labCategoryGroup-*`.
- **Medication catalog & side-by-side layout**: verify `Paracetamol 500 mg` renders; open `AddMedicationDialog` at `1.0f` and assert `addMedNameField` and `addMedDosageField` sit side by side (`left` of dosage >= `right` of name, top edges aligned), and at `2.0f` stack vertically (`top` of dosage >= `bottom` of name); test route/frequency `RecordFilterChip` selection, save (`saveMedCodexConfirmBtn`), edit, delete confirmation, and empty/no-match states.
- **Dark theme & RTL**: mount under `AncTheme(darkTheme = true)` and `LayoutDirection.Rtl` with zero overlap or horizontal overflow.

## Risk

**Low-medium (`CatalogSettingsSections.kt` is not Level 3; `ClinicalPresets.kt` and `SecuritySettingsDialog.kt` are Level 3 and untouched).** Wrapping `OutlinedTextField` in `CatalogLabeledField` without keeping `.testTag(...)` directly on `OutlinedTextField`, adding a `LazyColumn` inside `SecuritySettingsDialog`'s `verticalScroll`, or changing `AddLabCategoryGroupDialog`'s legacy `custom-$it` test chip IDs will break semantics queries or runtime scrolling. Keep all 25 existing `testTag`s on their exact semantic nodes, preserve all `LabCatalogDelegate` / `MedicationCatalogDelegate` / `CustomChoicesDelegate` calls verbatim, and keep the file under `1,500` lines.
