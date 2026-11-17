Added 4 independent instrumentation tests to [ReportDialogRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/ReportDialogRedesignTest.kt) (no other files modified, no production edits, no git, no adb):

1. **`pregnancyDating_ga42WeeksOrMore_showsPostTermFormat_and40WeeksShowsNoPostTerm`**:
   - Evaluates a patient with GA $\ge 42$ weeks (`42+0 wks`), verifying that `ReportPregnancyDatingCard` renders the short weeks format followed by `(Post-term)` via string resource `R.string.report_ga_post_term_format`.
   - On the same single `setContent`, transitions state to a 40-week patient (`40+0 wks`) and asserts that `(Post-term)` is absent.
2. **`visitsCard_visitWithNoComputableGa_showsDashGaChip`**:
   - Evaluates a patient whose visit has no computable gestational age, verifying that `ReportVisitsCard` renders a `"-"` GA chip for the visit item.
3. **`pregnancyDating_nonDeliveredPatientWithNullDating_showsEddRowWithNotRecorded`**:
   - Evaluates a non-delivered patient with `dating == null`, verifying that `ReportPregnancyDatingCard` renders an `"EDD"` row with `"Not recorded"`.
4. **`clinicMetaCard_customClinicSubtitle_stillShowsFixedFullSubtitleString`**:
   - Evaluates `ReportClinicMetaCard` when `SecuritySettingsState.clinicSubtitle` is customized, verifying that it continues to display the fixed clinic subtitle `R.string.report_clinic_subtitle_full` and never the custom subtitle string.

### Compilation Verification
- Ran `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: Exit code `0` (clean compilation).
