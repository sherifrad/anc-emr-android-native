The failing `androidTest` suites have been updated to align with the slice 19 "picker-only, never typed" date field rule and the slice 14 `VisitEditor` layout structure.

### Files Updated
1. [DeliveryPlanSheetTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/DeliveryPlanSheetTest.kt#L292-L380):
   - `deliveredSwitchRevealsTheFieldsAndSaveWritesTheFourValuesAndTheCardAndHeaderShowThem`: Interacts with `deliveryOutcomeDateFieldCalendarButton` to open `ancDatePickerDialog` and confirms via `datePickerConfirm`, asserting the stored ISO date (`LocalDate.now().toString()`) and dispatch.
   - `turningDeliveredOffClearsTheOtherThreeValues`: Sets date via `deliveryOutcomeDateFieldTodayButton`, toggles switch off, and verifies that `deliveryDate` and `deliveryMode` clear to `""`.
   - `aBadDateKeepsTheSheetOpenAndShowsTheDateMessage`: Verifies that clicking the read-only date field opens `ancDatePickerDialog` (dismissed with `datePickerDismiss`), and tests that a malformed date draft (`"not-a-date"`) is rejected on save by `SavePatientProfileCommand.prepare`, keeping the sheet open and showing the validation message without dispatching commands.
2. [MedicationFormTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/MedicationFormTest.kt#L233-L249):
   - `togglingToStoppedSwapsDateLabelAndTypingWritesEndDate`: Replaced `performTextInput` with `medDateTodayButton.performClick()` for `START_DATE`, and again after toggling to Discontinued for `END_DATE`.
   - `stoppedBackToActiveWritesStartDateAgain`: Replaced `performTextInput` with `medDateTodayButton.performClick()` for `START_DATE`.
3. [AncWorkflowTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt#L237-L253):
   - `reviewedVisitEditorLayoutsKeepStatusDraftAndControlsAccessible`: Replaced the assertion on the removed composite subtitle (`"Maya Example · Routine Antenatal Visit & Vitals"`) with checks verifying both the localized `EditorPage` title (`R.string.add_visit`: `"Add Visit"` / `"تسجيل زيارة"`) and the `VisitPatientBanner` patient name (`"Maya Example"`).

### Verification
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: Exit code `0` (clean compilation).
- `./gradlew --offline :app:testDebugUnitTest -q`: Exit code `0` (`BUILD SUCCESSFUL`).
- Detailed report written to [tests_fix_s19b_report.md](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/out/tests_fix_s19b_report.md).
