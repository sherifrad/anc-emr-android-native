# Slice 19b Test Fix Report: Date Picker and Visit Editor Assertions

## Summary of Changes

Updated `androidTest` suites to align with slice 19 ("every date field is picker-only, never typed") and slice 14 (`VisitEditor` patient banner and title structure). No production code, git commands, or adb commands were executed.

### 1. DeliveryPlanSheetTest (`app/src/androidTest/java/com/sherifradwan/ancnative/DeliveryPlanSheetTest.kt`)
- **`deliveredSwitchRevealsTheFieldsAndSaveWritesTheFourValuesAndTheCardAndHeaderShowThem`**:
  - Replaced `type("deliveryOutcomeDateField", "2026-11-05")` with picker interaction: scrolls to `deliveryOutcomeDateField`, taps `deliveryOutcomeDateFieldCalendarButton`, confirms `ancDatePickerDialog` is displayed, and clicks `datePickerConfirm`.
  - Asserts that the stored date command equals `LocalDate.now().toString()`, the command is dispatched with all four values, and the card/header correctly reflect the saved state.
- **`turningDeliveredOffClearsTheOtherThreeValues`**:
  - Replaced `type("deliveryOutcomeDateField", "2026-11-05")` with `deliveryOutcomeDateFieldTodayButton` click.
  - Asserts that turning `deliveryOutcomeSwitch` off clears `deliveryDate` and `deliveryMode` in `patientDraft`, disables the save button, and removes the date field from composition.
- **`aBadDateKeepsTheSheetOpenAndShowsTheDateMessage`**:
  - Because `deliveryOutcomeDateField` is `readOnly = true` and cannot be typed into, direct text input is unreachable in the UI.
  - Replaced typing with dual coverage:
    1. Verifies that clicking the read-only date field opens `ancDatePickerDialog` (and dismisses cleanly with `datePickerDismiss`).
    2. Injects a malformed date (`"not-a-date"`) into the draft (simulating a restored or legacy malformed draft) and asserts that attempting to save is rejected by `SavePatientProfileCommand.prepare`, keeps `deliveryPlanSheet` displayed, shows the date validation error message (`"Enter the date as YYYY-MM-DD, for example 2026-11-05."`), and does not dispatch any command. Full clinical validation coverage is preserved.

### 2. MedicationFormTest (`app/src/androidTest/java/com/sherifradwan/ancnative/MedicationFormTest.kt`)
- **`togglingToStoppedSwapsDateLabelAndTypingWritesEndDate`**:
  - Replaced `performTextInput("1")` and `performTextInput("2")` on `medDate` with `medDateTodayButton.performClick()`.
  - Asserts that clicking the Today chip populates `START_DATE` with `LocalDate.now().toString()`, and after toggling status to `Discontinued`, populates `END_DATE` with `LocalDate.now().toString()`.
- **`stoppedBackToActiveWritesStartDateAgain`**:
  - Replaced `performTextInput("1")` on `medDate` with `medDateTodayButton.performClick()`.
  - Asserts that toggling back to `Active` populates `START_DATE` with `LocalDate.now().toString()` and leaves `END_DATE` empty.

### 3. AncWorkflowTest (`app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt`)
- **`reviewedVisitEditorLayoutsKeepStatusDraftAndControlsAccessible`**:
  - Redesign slice 14 removed the composite subtitle `"Maya Example · Routine Antenatal Visit & Vitals"` from `EditorPage` (subtitle is empty) and rendered patient identity inside the dedicated `VisitPatientBanner`.
  - Updated `TEST_PATIENT_IDENTITY` / `TEST_PATIENT_NAME` to `"Maya Example"`.
  - Updated the test assertions to check that the localized editor title (`R.string.add_visit`: `"Add Visit"` / `"تسجيل زيارة"`) and the patient banner (`"Maya Example"`) are displayed and fully visible within the viewport across all 6 responsive layout fixtures (`narrowPhone`, `largeFont`, `landscape`, `tablet`, `darkTheme`, `arabicRtl`).

## Production Defect Assessment
No real production defects were uncovered. The failures were entirely due to outdated test assertions expecting deprecated keyboard text input on date fields (superseded by slice 19 picker-only date fields) and a removed composite subtitle string (superseded by slice 14's `VisitPatientBanner`).

## Verification
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: Exit code `0` (clean compilation).
- `./gradlew --offline :app:testDebugUnitTest -q`: Exit code `0` (`BUILD SUCCESSFUL`).
