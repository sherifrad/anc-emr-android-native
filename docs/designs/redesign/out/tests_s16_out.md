I have authored the test suite for redesign slice 16 and verified compilation against the offline build tasks.

### Summary of Changes

1. **New Test Suite**: [LabEditorRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/LabEditorRedesignTest.kt) (733 lines)
   - Follows the style template from `HistoryEditorsRedesignTest.kt` and `RegistrationRedesignTest.kt` with a single `setContent` per test in a narrow `360dp x 640dp` container.
   - **Typography & Legacy Removal**: Verifies no text layouts under `12sp` (`>= 11.99sp`) across `LabEditor` and `LabBatchScreen` (`ADD`, `ENTER`, `REVIEW` tabs) and asserts zero occurrences of legacy glyphs/strings (`"AES-256"`, `"Normal Risk"`, `"27w 2d"`, `"✓"`, `"▲"`, `"▼"`, `"◀"`, `"▶"`, `"▾"`, `"+ +"`).
   - **Shared `LabPatientBanner` & `BatchInfoBar`**: Verifies left-aligned English name, right-aligned Arabic name (`"منى رجب"`), `BandChip`s for GA (`"27+2 wks"`), Parity (`"G4 P1 A2"`, keeping abortion count A2), Blood Group (`"A+"`), `StatusTag` for High Risk, no low-risk pill, and unified `BatchInfoBar` with `"Change"` in `RecordType.Action`.
   - **Touch Targets (>= 47.99dp)**: Verifies touch target bounds on all interactive elements listed in the build report (including `labBatchDeleteItem` raised from 32dp to 48dp, `QualitativeChoiceButton` >= 52dp, `labBatchChange`, `labFlagToggle`, `labAddTestsToggle`, bottom bars, tabs, dipstick chips, panel action buttons, and dialog controls).
   - **Font Scaling (1.3f and 2.0f) Ergonomics**: Verifies vertical stacking of `labUnit` / `labRefRange` and `FlowRow` for the 3-tab bar when `fontScale > 1.3f`, multi-line button labels (`maxLines = 2`), and zero horizontal overflow beyond 360dp.
   - **Behavioral Parity & Flow**:
     - `LabEditor`: Auto-flagging (`Normal` -> `Low` -> `High`) and abnormal switch updates, dipstick preset selections ("Trace", "Normal"), preset list collapsing (`collapseAfter = 5`) with toggle ("Show all 28" / "Show fewer"), action dispatches (`CancelEditor`, `SubmitLab`, `SubmitLabAndAddAnother`), pending save spinner, validation error card, and custom test template prompt.
     - `LabBatchScreen`: Metadata dialog editing (date and lab presets), validation error card display, empty search feedback ("No matching tests found"), panel selection with count ("CBC · 6 elements", "Add 5 selected"), qualitative "Mark empty as normal" flow, and batch save gating (`filledCount > 0`).
   - **Dark Theme & RTL**: Tested under `AncTheme(darkTheme = true)` and `LayoutDirection.Rtl` without clipping or overflow.
   - **Tag Preservation**: Verified all existing and newly added tags.

2. **Existing Test Minimal Update**: [LabBatchUiTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/LabBatchUiTest.kt#L101)
   - Updated assertion on line 101 to `compose.onNodeWithText("10.8", substring = true).assertIsDisplayed()` to match the redesigned `ReviewTab` format displaying isolated metric strings (`ltrIsolate("${item.result} ${item.unit}")`).

3. **Report**: [tests_s16_report.md](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/out/tests_s16_report.md)

### Verification
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q` passed with exit code 0.
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest -q` passed with exit code 0.
