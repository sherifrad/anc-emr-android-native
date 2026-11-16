# Independent Test Review: Medication Section Revision (Phase 3)

The test suite in [`tests.patch`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch) adds substantial coverage for the collapsed picker, 360dp responsiveness, and sheet restoration. However, it harbors test flakiness on CI, conditional tautologies that silently skip assertions, and several significant blind spots around edge cases and regression fixes introduced in [`main.patch`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/main.patch).

---

## 1. Gaps by Severity

### Blocker

* **Environment-Gated Test Abort (Hard Failure on standard CI / Non-360dp devices)**:
  In [`MedsTabSheetTest.kt` lines 610–613](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L610-L613), `withTheKeyboardOpenAt360dpTheFocusedFieldAndSaveStayVisibleAndEnterMovesOn` runs an explicit assertion:
  ```kotlin
  assertTrue("run .agents/scripts/set_emulator_360dp.sh first",
      compose.activity.resources.configuration.screenWidthDp <= 360)
  ```
  If tests run on any default emulator, tablet, or standard device configuration, the test fails immediately due to an environment prerequisite rather than a code defect.
* **Flaky WindowInsets Timing**:
  [`assertAboveKeyboard`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L591-L598) polls `WindowInspector.getGlobalWindowViews()` for `WindowInsets.Type.ime()`. On headless emulator runs where software keyboards do not trigger software insets without hardware keyboard detachment, this 5-second `waitUntil` will intermittently time out.

### Major

* **Zero Tests for `PatientHubScreen.kt` Pager Sync Fix**:
  [`main.patch` lines 57–64](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/main.patch#L57-L64) changes pager sync to `val currentRoute by rememberUpdatedState(route)` because "return to the opening tab was never reported." There is **no test** in `tests.patch` verifying that returning to the opening tab via settled page synchronization emits `SelectSection`. If this breaks, no test fails.
* **Untested ShowPast Availability During In-Flight Saves**:
  [`main.patch` line 1196](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/main.patch#L1196) and the design state that "Viewing stopped drugs starts no action, so it stays usable while a save runs." While [`everyEntryPointIsDisabledWhileAStopSaves`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L165-L184) and [`everyEntryPointIsDisabledWhileAnAddSaves`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L225-L242) assert that tiles, edit, and stop buttons are disabled, **neither test asserts that `medShowPastToggle` remains enabled**. If `medShowPastToggle` is inadvertently disabled during saves, all tests still pass.
* **Missing Negative Cases in Picker Empty States**:
  [`main.patch` lines 647–651](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/main.patch#L647-L651) contains distinct branches: `R.string.no_recent_medications` when `source == RECENT && query.isBlank()`, versus `R.string.medication_picker_no_match`.
  In [`MedsTabSheetTest.kt` lines 301–316](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L301-L316), only the `"zzqx-no-drug"` search under Common is tested. The `RECENT` empty state string branch is never tested in UI or unit tests.
* **Missing Tile Fallback Metadata Tests**:
  [`MedicationTapToAddSection.kt` lines 721–726](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/main.patch#L721-L726) formats tiles without dosage (`R.string.set_dose_in_editor`) or without frequency (`R.string.medication_choose_frequency`).
  [`aTileShowsItsNameAndDoseRouteAndFrequency`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L318-L327) only tests `readyTiles().first()`, where dosage and frequency are guaranteed non-blank ([line 273](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L273)). Neither fallback branch is tested.
* **Cross-Patient State Leakage Untested**:
  [`MedsTab.kt` lines 929–931](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/main.patch#L929-L931) keys `expandedMedId`, `showPast`, and `pickerState` to `patient.id`. No test verifies that switching between patients isolates or resets these states.

### Minor

* **Tautological Assertions on Android Strings**:
  In [`MedsTabSheetTest.kt` lines 473–482](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L473-L482), `titlesAndLabelsComeFromTheStringsFile` asserts:
  ```kotlin
  for (id in listOf(...)) assertTrue(str(id).isNotBlank())
  for (id in listOf(...)) assertTrue(str(id, 7).contains("7"))
  ```
  This tests `Context.getString` against constant XML IDs, not whether UI composables bind or display them.
* **Silent Conditional Skip on Scrollability**:
  In [`thePickerStartsAsOneRowAndOpensToSearchChipsAndAListThatScrollsInsideItself` lines 290–293](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L290-L293):
  ```kotlin
  if (catalog().size > 5) {
      val range = grid.config[SemanticsProperties.VerticalScrollAxisRange]
      assertTrue("a long list scrolls inside the picker", range.maxValue() > 0f)
  }
  ```
  If test catalog fixtures ever have 5 or fewer items, the scrollability assert is bypassed silently without failure.
* **Partial State Restoration Verification**:
  In [`MedsTabRestorationTest.kt` lines 782–796](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L782-L796), `pickerState.source` ("recent" vs "common") and `expandedMedId` are not verified after restoration.
* **Stale Coordinate Measurement Risk in Layout Test**:
  In [`rowsKeepTheirTextWholeAndTheirButtonsAt48dpAt360dpAnd200PercentText` lines 541–546](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L541-L546), `textBottom` is captured before calling `scrollTo(tag + med.id)`. If `scrollTo` shifts the LazyColumn, comparing scrolled button `top` against pre-scroll `textBottom` produces an invalid offset comparison.
* **Missing Negative Case for Empty State on Archived Patient**:
  In [`aPatientWithNoMedicationsSeesTheEmptyStateAndItsMainAction`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/tests.patch#L489-L503), only `canEdit = true` is tested. No test checks that `medEmptyPrescribe` is hidden when `canEdit = false` ([`main.patch` line 1181](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/medication_section_revision/phase3/main.patch#L1181)).

---

## 2. Design Behaviour Coverage Breakdown

| Design / Implementation Requirement | Covering Test Name | Fails If Broken? | Notes / Defect Potential |
| :--- | :--- | :--- | :--- |
| **Collapsed Picker (Starts 1 row, 48dp+ tap target)** | `thePickerStartsAsOneRow...` | **Yes** | Fails if height < 48dp or children visible initially |
| **Picker Internal Scroll (304dp max height)** | `thePickerStartsAsOneRow...` | **Partial** | Silently skips scroll assert if catalog <= 5 items |
| **Picker Filter (Search & Case-insensitivity)** | `searchKeepsOnlyMatchingTiles...`, `recentMatchesStoredNames...` | **Yes** | Fails if non-matching tiles shown or empty banner missing |
| **Empty Search State: "No recent" vs "No match"** | `searchKeepsOnlyMatchingTiles...` | **No** | Missing test for `R.string.no_recent_medications` |
| **Tile Display: Dose · Route · Freq** | `aTileShowsItsNameAndDose...` | **Partial** | Only tests full data; fallbacks (no dose, no freq) untested |
| **Active Drug Tile Checkmark & Disabled** | `aTileForAnActiveDrugShowsItsCheck...` | **Yes** | Fails if clickable or check missing |
| **Tap Counts (3 closed, 2 open)** | `addingTakesThreeTaps...` | **Yes** | Counts and asserts exact tap numbers |
| **Picker State Persistence (Add/Stop/Edit/Swipes)** | `thePickerStaysOpenWithItsSearch...` | **Yes** | Verifies text retention across all user actions |
| **Show Past Toggle & Stopped Row Hide/Show** | `stoppedDrugsSitBehindShowPast...` | **Yes** | Fails if stopped drug visible without toggling |
| **No Stop Symbol on Stopped Rows** | `stoppedDrugsSitBehindShowPast...` | **Yes** | Asserts `stopMedication-` does not exist |
| **Show Past Usable During In-Flight Save** | *(None)* | **No** | Untested; regression won't be caught |
| **Empty State vs No-Active State** | `aPatientWithNoMedications...`, `onlyStoppedDrugs...` | **Yes** | Fails if wrong banner or prescribe button shown |
| **360dp Layout (No text truncation / 48dp buttons)** | `rowsKeepTheirTextWhole...`, `thePickerKeepsItsTextWhole...` | **Yes** | Fails if text clipped or width > 360dp |
| **Enter Moves Focus & Sheet Avoids Keyboard** | `withTheKeyboardOpenAt360dp...` | **Flaky** | Fails immediately on non-360dp emulator |
| **Save & Add Another Refocuses Name Field** | `saveAndAddAnotherKeepsTheKeyboard...` | **Yes** | Fails if name field not refocused on new draft |
| **Pager SettledPage Route Synchronization** | *(None)* | **No** | Untested; fix in `PatientHubScreen.kt` has no test |
