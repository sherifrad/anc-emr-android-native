I have authored the test suite for Redesign Slice 13 and updated `RegistrationRedesignTest.kt` per your requirements.

### Changes Summary

1. **Updated [`RegistrationRedesignTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationRedesignTest.kt#L648-L651)**:
   - In `patientStatusEditor_archive_showsStatusTag_cancelAndSave_emitsEvents`, updated the tag assertion from `R.string.archived` to `R.string.status_will_be_archived` (`"Will be archived"`).

2. **Created [`HistoryEditorsRedesignTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt)** (38 independent test cases, single `setContent` each):
   - **`OtherHistoriesEditor`**:
     - No numbered titles (`"1. "`, `"2. "`, `"3. "`, `"4. "`), clean section titles, and no padlock lock footer text (`"AES-256"`, `"Tamper-Evident"`).
     - `negativeHistoriesCard` displays title `"Explicit negative histories"`.
     - `fillAllNegativeHistories` has text `"Confirm all histories negative"` without `"✓ "` prefix; clicking it populates all 4 fields with localized negative strings and emits `AncEvent.HistoriesDraftChanged`.
     - Preset chips (`medHistoryPreset-*`, `surgHistoryPreset-*`, `famHistoryPreset-*`, `allergyHistoryPreset-*`) update draft fields.
     - Interactive controls meet `>= 47.99dp` touch target minimum in height and width.
     - Standalone Save and Cancel buttons emit `SubmitOtherHistories` and `CancelEditor`; disabled with spinner during `SavePhase.PENDING`; hidden when inside a registration flow.
     - No text under `12.sp` at `fontScale = 1.0f` and `1.3f`.
     - Presets and cards wrap cleanly without clipping on 360dp at `fontScale = 2.0f`.
   - **`PreviousPregnancyEditor`**:
     - Clean titles without `"1. "` or `"2. "` and no padlock lock footer text.
     - Outcome chips show human labels (`"Unknown"`, `"Live birth"`, `"Stillbirth / IUFD"`, `"Early pregnancy loss"`, `"Termination of pregnancy"`) while dispatching canonical stored codes (`""`, `"LiveBirth"`, `"Stillbirth"`, `"EarlyPregnancyLoss"`, `"TerminationOfPregnancy"`).
     - Delivery mode chips appear for `LiveBirth` and `Stillbirth`, showing human labels (`"Unknown"`, `"Vaginal"`, `"Cesarean"`) while dispatching `"CS"` and `"Vaginal"`.
     - Conditional fields (`lossManagement-0..3` for `EarlyPregnancyLoss`, `terminationMethod-0..3` for `TerminationOfPregnancy`) appear and disappear correctly.
     - All chips, calendar button, and action buttons meet `>= 47.99dp` touch target minimum.
     - Standalone Save and Cancel buttons emit `SubmitPreviousPregnancy` and `CancelEditor`; disabled with spinner during `SavePhase.PENDING`; hidden in flow.
     - No text under `12.sp` at `1.0f` and `1.3f`.
     - No clipping at 360dp at `fontScale = 2.0f`.
   - **`DatingEditor`**:
     - Clean titles without numbers (`"Baseline dating"`, `"Notes"`) and no padlock lock footer text.
     - Calculated dating card is omitted completely when both `calculatedEdd` and `calculatedGa` are null.
     - When dates are present, calculated card displays `"Calculated dating"`, EDD formatted as `dd/MM/yyyy` (e.g., `"11/02/2027"`), gestational age in `+` / `wks` style (e.g., `"27+2 wks"`), and no raw ISO strings.
     - When post-term (>= 42 weeks), `StatusWord` with `"Post"` (`R.string.post_term_badge`) is displayed.
     - Method field visibility across `LMP` (with working `lmpDateTodayButton`), `Ultrasound`, `IVF_TRANSFER` (with working `embryoDay3/5/6` chips), and `Clinical`, plus `RecordedDateNote` verification.
     - Calendar buttons, today button, embryo chips, and action buttons meet `>= 47.99dp` touch target minimum.
     - Standalone Save and Cancel emit `SubmitDating` and `CancelEditor`; disabled with spinner during `SavePhase.PENDING`; hidden in flow.
     - No text under `12.sp` at `1.0f` and `1.3f`.
     - No clipping at 360dp at `fontScale = 2.0f`, clean RTL / dark theme rendering.

3. **Report Written**:
   - Detailed test report written to [`docs/designs/redesign/out/tests_s13_report.md`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/out/tests_s13_report.md).

### Verification
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q` passed with exit code 0.
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin -q` passed with exit code 0.
- `./gradlew --offline :app:testDebugUnitTest ...` passed (all 5 related JVM unit test suites green).
