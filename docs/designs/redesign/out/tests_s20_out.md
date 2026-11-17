The test suite for redesign Slice 20 (Two-Page Clinical Report Dialog) has been authored and verified.

### Deliverables
- **New Test File**: [ReportDialogRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/ReportDialogRedesignTest.kt) (15 tests, 704 lines)
- **Test Author Report**: [tests_s20_report.md](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/out/tests_s20_report.md)

### Coverage Summary
1. **Kept Tags & Page Switching**: Asserts `patientTwoPageReportDialog`, `closeReportDialog`, `shareReportButton`, `printReportButton`, `reportPageSwitcher` (`report_pages_tab_group` cd), `reportPage1Chip`, `reportPage2Chip`, and `reportPageContent`. Verifies switching between Page 1 and Page 2 and that `closeReportDialog` dispatches the dismissal callback.
2. **Touch Targets (`>= 47.99dp`)**: Measures unclipped bounds for `closeReportDialog`, `shareReportButton`, `printReportButton`, `reportPage1Chip`, and `reportPage2Chip` to ensure full compliance with the 48dp minimum touch target.
3. **Level 3 Clinical Parity & Negative Invariants**: Verifies all clinical patient identity, history, dating, visits, scans, labs, meds, delivery plan, and attestation fields. Explicitly verifies that **delivery fees** (e.g., `12000`, `15000`) and **medication frequency** (`FREQ_SECRET_Q8H`) NEVER appear in the UI, and the physical dashed `Stamp` box is omitted.
4. **Clamping & Notice Banners**: Verifies that 15 visits clamp to 12 with `"3 earlier visits not shown"` (hiding visits `#1`..`#3`), 9 scans clamp to 7 with `"2 earlier scans not shown"`, and 12 labs clamp to 10 with `"2 older results not shown"`.
5. **Conditional Alerts & Flags**: Asserts `reportStatusLine` banner, `"High risk"` tag, `"Rh negative"` tag, `"Clinical attention"` alert note, recorded drug allergies in an `errorContainer` alert box, `"Severe-range BP - urgent assessment"`, `"Abnormal"` lab indicator, `"Anti-D Prophylaxis + Matched Packed RBCs Cross-match"` blood bank precautions, and the conditional `"Stopped medications"` section.
6. **Raised BP & Stopped Meds Omission**: Verifies `"Raised BP - recheck"` on borderline systolic/diastolic values, and verifies `"Stopped medications"` is completely omitted when empty.
7. **Empty State & Blank Histories**: Verifies `"Unspecified"` phone, `"Gestational age: not recorded"`, explicit `"Not recorded"` across blank history fields, and fallback strings for previous pregnancies, visits, scans, labs, and medications.
8. **Share Intent Parity**: Intercepts `shareReportButton` via `Instrumentation.ActivityMonitor` to verify `Intent.ACTION_CHOOSER` is launched with title `"Share Clinical Record"` wrapping `Intent.ACTION_SEND` (`type = "text/plain"`, `EXTRA_SUBJECT = "Clinical Record - Mona Ragab"`, and `EXTRA_TEXT = patientShareText(patient)`).
9. **Typography**: Verifies zero text nodes under `patientTwoPageReportDialog` measure below `12.sp` across both Page 1 and Page 2 at `fontScale = 1.0f` and `fontScale = 1.3f`.
10. **Narrow 360dp & Extreme Scaling (2.0f)**: Verifies no horizontal overflow, vertical clipping, or text truncation on 360dp-wide displays at `fontScale = 1.3f` and `fontScale = 2.0f`.
11. **RTL Arabic & Dark Theme**: Tests Arabic name `"منى رجب"` with `darkTheme = true`, `LayoutDirection.Rtl`, and `fontScale = 1.3f`.
12. **PatientHub Integration**: Verifies opening `patientTwoPageReportDialog` from `PatientHubScreen` via `workspaceReportButton` and dismissing back to the hub.

### Build Verification
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q` passed with exit code `0`.
- `./gradlew --offline :app:testDebugUnitTest -q` passed with exit code `0`.
