# Polish batch test report

Branch: `redesign-2026-11-15`

## Verification

- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: passed (exit code 0)
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q`: passed (exit code 0)
- `./gradlew --offline :app:testDebugUnitTest -q`: passed (exit code 0)

## Test Updates & Findings

### 1. ReportDialogRedesignTest
- Renamed `visitsCard_visitWithNoComputableGa_showsDashGaChip` to `visitsCard_visitWithNoComputableGa_showsNotRecordedGaChip`.
- Updated GA chip assertion from literal `"-"` to `str(R.string.not_recorded)` ("Not recorded"), preserving full assertion rigor.

### 2. MedsTabSheetTest (`thePickerStaysOpenWithItsSearchThroughAddStopEditAndTabSwipes`)
- Conducted full semantic and layout analysis of `thePickerStaysOpenWithItsSearchThroughAddStopEditAndTabSwipes` against all polish changes:
  1. Removed `Icon(Icons.Default.Add)` and spacer from `medPickerToggle`: `openPicker()` targets `medPickerToggle` by `testTag` and toggles state via clickable semantics. No assertion checked icon presence, spacer presence, or toggle dimensions.
  2. `add_other_choice` string (`"+ Add other"` -> `"Add other"`): Not referenced or asserted in this test.
  3. `active_prescriptions_title` sentence case: Plural title is not asserted in this test (asserted in `theScreenShowsItsTitlesLabelsAndFrequenciesFromTheStringsFile`, which dynamically queries `R.plurals.active_prescriptions_title`).
- Findings: None of the assertions in `thePickerStaysOpenWithItsSearchThroughAddStopEditAndTabSwipes` depend on text or geometry that changed intentionally. The test logic and state preservation across add, stop, edit, and tab switches remain valid with no underlying defect. The assertions were preserved as-is without weakening.

### 3. PolishBatchRedesignTest (New Suite)
Created `app/src/androidTest/java/com/sherifradwan/ancnative/PolishBatchRedesignTest.kt` with 8 independent tests under 360dp narrow viewport and unmerged tree semantics:
- **(a) Scan GA chip and EFW in native report dialog**: `reportDialog_scanWithoutGaAndEfw_showsNotRecorded_andScanWithGaAndEfw_showsValues` verifies that a scan without GA/EFW renders `"Not recorded"` (`R.string.not_recorded`) for both GA chip and EFW field, while a scan with GA and EFW displays the formatted values (`"32w + 4d"`, `"1950 g"`).
- **(b) PatientHubHeader Arabic & English name alignment**:
  - `patientHubHeader_hero_arabicPatientName_drawsRightAligned_within24dpOfRightPadding`: Arabic name (`"منى رجب"`) renders with `TextAlign.Right`, `TextDirection.Rtl`, right edge within 24dp of screen right padding, and line right edge aligned to node width.
  - `patientHubHeader_hero_englishPatientName_drawsLeftAligned_nearLeftPadding`: English name (`"Mona Ragab"`) renders with `TextAlign.Left`, `TextDirection.Ltr`, left edge within 24dp of left padding, and line left edge at 0.
  - `patientHubHeader_collapsedTopBar_arabicNameRightAligned_andEnglishNameLeftAligned`: Collapsed sticky top bar renders Arabic name with `TextAlign.Right` and `TextDirection.Rtl`.
- **(c) EditorPage subtitle Arabic name alignment**: `editorPage_subtitle_arabicPatientName_drawsRightAligned` verifies that `PatientStatusEditor` (`EditorPage` subtitle) renders an Arabic patient name right-aligned (`TextAlign.Right`, `TextDirection.Rtl`, within 24dp of right container margin).
- **(d) Date labels without "YYYY"**: `dateLabels_visitScanDelivery_doNotContainYyyy` asserts that `visit_date`, `scan_date`, and `delivery_outcome_date_label` strings equal `"Visit date"`, `"Scan date"`, and `"Delivery date"` without `"YYYY"`, and verifies `UltrasoundEditor` UI displays `"Scan date"` without format suffix.
- **(e) Meds tab sentence case & button touch targets**:
  - `medsTab_activePrescriptionsTitleIsSentenceCase_andAddCustomMedicationBtnHasNoPlus_andMinTouchTarget`: `medsActiveTitle` starts with `"Active prescription"` with lowercase 'p', `addCustomMedicationBtn` is >= 48dp in width and height, retains testTag, and contains no `"+"` text.
  - `medsTab_emptyStatePrescribeButtonHasNoPlus_andMinTouchTarget`: `medEmptyPrescribe` is >= 48dp in width and height, retains testTag, and contains no `"+"` text.
