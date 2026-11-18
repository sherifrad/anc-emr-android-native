# Polish Batch Redesign Test Fix Report

## Summary of Changes

Updated `app/src/androidTest/java/com/sherifradwan/ancnative/PolishBatchRedesignTest.kt` and `app/src/androidTest/java/com/sherifradwan/ancnative/ReportDialogRedesignTest.kt` to align failing tests with actual production behavior in `PatientTwoPageReportDialog` and `PatientHubHeader`. Production code was not modified, and no git or adb commands were executed.

### 1. `reportDialog_scanWithoutGaAndEfw_showsNotRecorded_andScanWithGaAndEfw_showsValues` (`PolishBatchRedesignTest.kt`)
- **Root Cause**: `PatientTwoPageReportDialog` organizes report content across two pages: Page 1 (`ReportPageOneSections`: identity, medical & obstetric history, visits) and Page 2 (`ReportPageTwoSections`: ultrasound scans, lab investigations, current medications, delivery plan, attestation). The dialog initially opens on Page 1 (`selectedPage == 0`). The test attempted to immediately scroll to `report_section_scans` (`"Ultrasound biometry and fetal surveillance"`), which does not exist on Page 1.
- **Fix**: Added `compose.onNodeWithTag("reportPage2Chip", useUnmergedTree = true).performClick()` and `compose.waitForIdle()` prior to scrolling to `report_section_scans`. Then verified both the populated scan (`"15/03/2026"`, `"32w + 4d"`, `"1950 g"`) and empty scan (`"01/03/2026"`, `"Not recorded"` GA chip and EFW field) inside `ReportScansCard`.

### 2. `patientHubHeader_collapsedTopBar_arabicNameRightAligned_andEnglishNameLeftAligned` (`PolishBatchRedesignTest.kt`)
- **Root Causes**:
  1. **Header Collapse Geometry**: `PatientHubHeaderState` in production (`PatientHubScreen`) initializes `collapsedHeightPx` with `56.dp.toPx()`. The test initialized `PatientHubHeaderState(56f)` using a raw pixel value of `56f` rather than density-scaled pixels, and set `expandedHeightPx = 300f` with `offsetPx = -244f`. When `PatientHubHeader` is composed, `onSizeChanged` measures the actual hero column height (~600px) and dynamically overwrites `headerState.expandedHeightPx`. With `offsetPx = -244f`, `collapseFraction` dropped below `0.55f`, causing `stickyAlpha` to be `0.0f` and `isStickyActive = headerState.stickyAlpha > 0.05f` to evaluate to `false`. Consequently, the collapsed top bar (and its `stickyBackButton`) was not composed or displayed.
  2. **Dual Name Node Semantics**: In `PatientHubHeader`, both Layer A (Hero Column) and Layer B (Sticky Top Bar) render `PatientNameText(name = patient.displayName)`. Layer A has `testTag = "hubPatientName"`, whereas Layer B does not. Calling `textLayoutOf(hasText("منى رجب"))` with `useUnmergedTree = true` targets multiple nodes unless disambiguated.
  3. **Missing English Alignment Assertion**: The test method was named `patientHubHeader_collapsedTopBar_arabicNameRightAligned_andEnglishNameLeftAligned` and declared `val englishPatient = basePatient.copy(displayName = "Mona Ragab")`, but only asserted the Arabic layout.
- **Fix**:
  1. Configured `collapsedHeightPx = 56f * base` and `offsetPx = -10_000f` so `collapseFraction` reaches `1.0f`, `stickyAlpha` is `1.0f`, and the collapsed top bar fills the `56.dp` height.
  2. Disambiguated the collapsed top bar name query using `hasText(...) and !hasTestTag("hubPatientName")`.
  3. Maintained a single `setContent` call per test by binding `PatientHubHeader` to `var currentPatient by mutableStateOf(arabicPatient)`. Asserted Arabic collapsed layout (`TextAlign.Right`, `TextDirection.Rtl`, `ResolvedTextDirection.Rtl`, right edge aligned to node width), then updated `currentPatient` to `englishPatient` via `compose.activity.runOnUiThread`, and asserted English collapsed layout (`TextAlign.Left`, `TextDirection.Ltr`, `ResolvedTextDirection.Ltr`, left edge at 0).

### 3. `visitsCard_visitWithNoComputableGa_showsNotRecordedGaChip` (`ReportDialogRedesignTest.kt`)
- **Root Cause**: On Page 1 (`ReportPageOneSections`), when `patient.dating` is null, both the Pregnancy Dating section (`ReportPregnancyDatingCard` EDD field) and the Visits section (`ReportVisitsCard` visit GA chip) display fallback `stringResource(R.string.not_recorded)` ("Not recorded"). The test called `compose.onNodeWithText(str(R.string.not_recorded), useUnmergedTree = true).performScrollTo().assertIsDisplayed()`, which threw `java.lang.AssertionError: Action performScrollTo() failed. Reason: Expected exactly '1' node but found '2' nodes that satisfy: (Text + InputText + EditableText contains 'Not recorded')`.
- **Fix**: Disambiguated the selection by using `compose.onAllNodesWithText(str(R.string.not_recorded), useUnmergedTree = true).onLast().performScrollTo().assertIsDisplayed()`, correctly targeting the visit GA chip in `ReportVisitsCard` (Node 1) instead of failing on the presence of the preceding EDD node (Node 0).

## Production Defect Assessment
No production defects were identified. All components behave strictly according to their architectural contracts:
- `PatientTwoPageReportDialog` correctly partitions obstetric and fetal surveillance sections into Page 2.
- `PatientHubHeader` correctly handles collapse state via dynamic layout measurement and density scaling.
- `ReportVisitsCard` and `ReportPregnancyDatingCard` correctly fall back to "Not recorded" when dating data is unavailable.

## Verification
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: Exit code `0` (clean compilation).
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q`: Exit code `0`.
- `./gradlew --offline :app:testDebugUnitTest -q`: Exit code `0` (`BUILD SUCCESSFUL`).
