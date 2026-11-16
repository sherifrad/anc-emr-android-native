# Redesign Slice 16: Lab Editor & Lab Batch Tests Report

## Summary
Created [LabEditorRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/LabEditorRedesignTest.kt) implementing comprehensive UI, accessibility, ergonomics, and behavioral parity verification for Redesign Slice 16 (`LabEditor`, `LabBatchScreen`, shared `LabPatientBanner`, `LabMetadataDialog`, `LabReadoutStrip`, and collapsible `QuickPresetChips`).

Also updated line 101 in [LabBatchUiTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/LabBatchUiTest.kt#L101) with the minimal assertion change (`substring = true`) to accommodate the redesigned `ReviewTab` format displaying isolated metric strings (`ltrIsolate("${item.result} ${item.unit}")` / `"\u206610.8 g/dL\u2069"`).

The tests strictly adhere to the project style templates ([HistoryEditorsRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt), [RegistrationRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationRedesignTest.kt)):
- Single `setContent` per test method.
- Narrow `360.dp x 640.dp` viewport container with host clipping bounds checks.
- Dynamic `fontScale` overrides (`1.0f`, `1.3f`, `2.0f`) via `LocalDensity` and Android configuration.
- `useUnmergedTree = true` for semantic traversal where labels are cleared by `clearAndSetSemantics`.
- `performScrollTo()` before assertions on scrollable items.
- Node selection primarily via explicit `testTag`s rather than ambiguous text.
- Minimum touch target tolerance `>= 47.99.dp`.
- Complete independence of every test case.

## Coverage Matrix

### 1. Typography & Legacy Removal (No Text < 12sp & Clean Hierarchy)
- `labEditor_typography_noTextBelow12sp_andNoLegacyGlyphsOrLockFooter`: Opens details toggle and add tests card; verifies all text layout results have `fontSize >= 12.sp` (checking `>= 11.99.sp`), and asserts complete absence of legacy patterns (`"AES-256"`, `"Normal Risk"`, `"27w 2d"`, `"✓"`, `"▲"`, `"▼"`, `"◀"`, `"▶"`, `"▾"`, `"+ +"`, `"Normal ✓"`, `"High ▲"`, `"Low ▼"`).
- `labBatchScreen_typography_noTextBelow12sp_addTab`: Verifies all text nodes on the `ADD` tab are `>= 12.sp`, with zero legacy glyphs or symbols.
- `labBatchScreen_typography_noTextBelow12sp_enterTab`: Verifies all text nodes on the `ENTER` tab (with items) are `>= 12.sp`, with zero legacy glyphs or symbols.
- `labBatchScreen_typography_noTextBelow12sp_reviewTab`: Verifies all text nodes on the `REVIEW` tab are `>= 12.sp`, with zero legacy glyphs or symbols.

### 2. Patient Banner & GA & RTL Alignment
- `patientBanner_englishName_leftAligned_ga_parity_bloodGroup`: Verifies `PatientNameText` displays English name left-aligned (`TextAlign.Left`, `ResolvedTextDirection.Ltr`), renders `BandChip`s for GA (`"27+2 wks"`, `shortWeeksText`), Parity (`"G4 P1 A2"`, keeping abortion count A2), and Blood Group (`"A+"`), omits any risk badge for low/normal risk patients, and renders `BatchInfoBar` with clean `"Change"` action.
- `patientBanner_arabicName_rightAligned`: Verifies Arabic patient name (`"منى رجب"`) renders right-aligned (`TextAlign.Right`, `ResolvedTextDirection.Rtl`).
- `patientBanner_highRisk_andRhNegative_tagAndTone`: Verifies High Risk patient renders `StatusTag` with `stringResource(R.string.high_risk)` and displays `StatusTone.High` on Rh-negative blood group chip (`"A-"`).

### 3. Touch Targets (>= 47.99dp) & 360dp Ergonomics
- `labEditor_touchTargets_atLeast48dp_inside360dpHost`: Verifies `>= 47.99.dp` width & height across all interactive controls in `LabEditor`:
  `cancelLab`, `saveLab`, `addAnotherLab`, `labBatchChange` (verifying height `>= 55.99.dp`), `labEditDetailsToggle`, `labFlagToggle` (verifying height `>= 55.99.dp`), `labAbnormal`, `labAddTestsToggle` (verifying height `>= 55.99.dp`), `allLabTestsChip`, and `labCategoryChoice-0`.
- `labEditor_dipstickPresets_touchTargets_atLeast48dp`: Verifies dipstick presets `labResultPreset-0` through `labResultPreset-4` each measure `>= 47.99.dp`.
- `labEditor_metadataDialog_touchTargets_atLeast48dp`: Verifies `labBatchDone`, `labDateTodayButton`, `labDateCalendarButton`, and `labNamePreset-0` in `LabMetadataDialog` each measure `>= 47.99.dp`.
- `labBatchScreen_touchTargets_headerTabsAndPanels_atLeast48dp`: Verifies `>= 47.99.dp` on `labBatchBack`, `labBatchCancel`, `labBatchSave`, `labBatchTab-ADD`, `labBatchTab-ENTER`, `labBatchTab-REVIEW`, panel `Choose` buttons, `labBatchAdd-group_cbc`, `labBatchOften-0..2`, and `labBatchAddOtherChip`.
- `labBatchScreen_touchTargets_enterAndReviewTabs_atLeast48dp`: Verifies `labBatchDeleteItem` (width & height `>= 47.99.dp`, raised from 32dp), `QualitativeChoiceButton` (height `>= 51.99.dp`), `labBatchFlag`, `labBatchPrev`, `labBatchNext`, `labBatchAddInline`, `labBatchEdit-*`, and `labBatchMarkNormal`.

### 4. Font Scaling (1.3f and 2.0f) Ergonomics & No Clipping
- `labEditor_fontScale2_0_verticalStacking_noHorizontalOverflow`: Verifies that at `fontScale = 2.0f` (`narrowStacked = true`), `labUnit` and `labRefRange` stack vertically (`Column`) without horizontal overflow beyond the 360dp host container, and 2-row footer buttons wrap with `maxLines = 2` without clipping.
- `labBatchScreen_fontScale2_0_flowRowTabs_noHorizontalOverflow`: Verifies that at `fontScale = 2.0f` (`narrowStacked = true`), the 3-tab bar switches from 3-column `Row` to `FlowRow` without horizontal overflow, and footer buttons wrap with `maxLines = 2`.

### 5. LabEditor Behaviour Parity & Flow
- `labEditor_quantitativeAutoFlag_normalLowHigh`: Verifies auto-flagging via `LaunchedEffect`: result "11.2" (range "11.0 - 15.0") displays flag "Normal" with abnormal switch OFF; entering "9.8" updates flag to "Low" and turns abnormal switch ON; entering "16.5" updates flag to "High".
- `labEditor_dipstickChoices_updatesDraftAndAbnormal`: Verifies selecting "Trace" (`labResultPreset-1`) sets draft result to "Trace", turns abnormal flag ON, and dispatches `LabDraftChanged` + `LabAbnormalChanged(true)`. Selecting "Normal" (`labResultPreset-0`) restores "Normal" and turns abnormal flag OFF.
- `labEditor_presetCollapse_collapsesAfterFiveUntilToggled`: Verifies catalog test presets collapse to 5 chips + `labPresetToggle` (`"Show all 28"`) when no group filter is selected, hiding `labPreset-5`; tapping toggle expands all presets and updates label to `"Show fewer"`.
- `labEditor_actionDispatches_andValidationErrors`: Verifies `cancelLab` emits `AncEvent.CancelEditor`, `saveLab` emits `AncEvent.SubmitLab`, and `addAnotherLab` emits `AncEvent.SubmitLabAndAddAnother`.
- `labEditor_saveSpinner_whenPendingSave`: Verifies `SavePhase.PENDING` displays `labSaveSpinner` and disables `saveLab`.
- `labEditor_validationErrorCard_displayedWhenErrorsPresent`: Verifies `draft.errors` containing `REQUIRED_NAME` and `REQUIRED_RESULT` displays `editorError` card.
- `labEditor_saveToLabCodex_shownForNewCustomTest`: Verifies typing an uncataloged custom test name exposes `saveToLabCodexBanner` and `saveToLabCodexButton`.

### 6. LabBatchScreen Behaviour Parity & Flow
- `labBatchScreen_metadataDialog_dispatchesDateAndLabName`: Verifies tapping `"Change"` opens `LabMetadataDialog`, tapping `labBatchDateTodayButton` dispatches `LabBatchDateChanged`, tapping `labBatchLabPreset-0` dispatches `LabBatchLabNameChanged`, and tapping `"Done"` dismisses the dialog.
- `labBatchScreen_validationErrorCard_displayedWhenErrorsPresent`: Verifies `draft.errors` containing `REQUIRED_DATE` displays `editorError` card on `LabBatchScreen`.
- `labBatchScreen_addTab_searchNoMatches_andOftenAdded`: Verifies search query with no match displays honest message `"No matching tests found"` (`R.string.lab_batch_no_search_matches`), and clicking `labBatchOften-0` ("Ferritin") dispatches `LabBatchItemsAdded`.
- `labBatchScreen_choosePanelDialog_elementsCountAndDeselect`: Verifies opening `ChoosePanelDialog` on CBC displays `"CBC · 6 elements"`, deselecting MCV updates button to `"Add 5 selected"`, and clicking it dispatches `LabBatchItemsAdded` for the 5 selected items.
- `labBatchScreen_enterAndReviewFlow_markEmptyNormal`: Verifies entering result/range on ENTER tab, navigating to REVIEW tab, clicking `labBatchMarkNormal` card (`"Mark N empty as normal"`) dispatches `LabBatchResultChanged("Negative")`, `labBatchCancel` emits `AncEvent.CancelLabBatch`, and `labBatchSave` emits `AncEvent.SubmitLabBatch`.

### 7. Dark Theme & RTL Layout
- `labEditor_darkTheme_and_rtl_rendersCleanly`: Mounts `LabEditor` under `darkTheme = true` and `LayoutDirection.Rtl`, asserting no clipping or horizontal overflow.
- `labBatchScreen_darkTheme_and_rtl_rendersCleanly`: Mounts `LabBatchScreen` on REVIEW tab under `darkTheme = true` and `LayoutDirection.Rtl`, asserting no clipping or horizontal overflow.

### 8. Existing Tags Still Present
- `existingTags_labEditor_allPresent`: Asserts existence of all `LabEditor` tags: `labEditor`, `labBatchChange`, `cancelLab`, `saveLab`, `addAnotherLab`, `labTestCard`, `labTestTitle`, `labEditDetailsToggle`, `labStoryDisplay`, `labStoryValue`, `labResultFlag`, `labResult`, `labUnit`, `labRefRange`, `labFlagToggle`, `labAbnormal`, `labNoteToggle`, `labNotes`, `labAddTestsCard`, `labAddTestsToggle`, `allLabTestsChip`, `labCategoryChoice-0`, `labName`, and `labPreset-0`.
- `existingTags_labBatchScreen_allPresent`: Asserts existence of all `LabBatchScreen` tags: `labBatchScreen`, `labBatchBack`, `labBatchCancel`, `labBatchSave`, `labBatchTab-ADD`, `labBatchTab-ENTER`, `labBatchTab-REVIEW`, `labBatchEnter`, `labBatchDeleteItem`, `labBatchResult`, `labBatchRange`, `labBatchFlag`, `labBatchNote`, `labBatchPrev`, `labBatchNext`, `labBatchAddInline`, `labBatchReview`, `labBatchEdit-*`, `labBatchAddList`, `labBatchAdd-group_cbc`, `labBatchSearch`, `labBatchOften-0`, and `labBatchAddOtherChip`.

## Verification Status
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: Exit code `0` (Success).
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest -q`: Exit code `0` (Success).
- Line count in `LabEditorRedesignTest.kt`: 733 lines (well under the 1,500-line ceiling).
