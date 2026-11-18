The failing test in [`ReportDialogRedesignTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/ReportDialogRedesignTest.kt) has been fixed.

### Root Cause
In [`visitsCard_visitWithNoComputableGa_showsNotRecordedGaChip`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/ReportDialogRedesignTest.kt#L1014-L1042), the test patient has `dating = null`. On Page 1 of the report dialog, two sections legitimately display the fallback string `"Not recorded"` (`R.string.not_recorded`):
1. **Pregnancy Dating Card** (`ReportPregnancyDatingCard`): The EDD row defaults to `"Not recorded"` when dating is null.
2. **Visits Card** (`ReportVisitsCard`): Visit #1 cannot compute gestational age without dating data, so its GA chip displays `"Not recorded"`.

Calling `compose.onNodeWithText(str(R.string.not_recorded), useUnmergedTree = true)` failed because exactly 2 matching nodes exist in the tree.

### Resolution
- Updated the assertion in [`ReportDialogRedesignTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/ReportDialogRedesignTest.kt#L1040-L1041) to use:
  ```kotlin
  compose.onAllNodesWithText(str(R.string.not_recorded), useUnmergedTree = true)
      .onLast().performScrollTo().assertIsDisplayed()
  ```
  This disambiguates the query and targets the visit GA chip in `ReportVisitsCard` (Node 1) rather than the preceding EDD row (Node 0).
- Production code was not modified.
- No `git` or `adb` commands were executed.
- Added summary note to [`tests_fix_spolish_report.md`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/out/tests_fix_spolish_report.md#L30-L34).

### Verification
```bash
./gradlew --offline :app:compileDebugAndroidTestKotlin -q
# Exit code 0
./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q
# Exit code 0
./gradlew --offline :app:testDebugUnitTest -q
# Exit code 0 (BUILD SUCCESSFUL)
```
