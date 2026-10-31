Running full unit test suite via `./gradlew --offline :app:testDebugUnitTest`...
Files changed:
- app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt
- app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintLayoutTest.kt

What was done per oracle:
- Q117: Added CSS rule `.clinical-table .more-row td { text-align: left !important; padding-left: 8px; }` in PatientPrintHtmlGenerator.kt to left-align the `<tr class="more-row">` cell with padding, winning cascade over `.clinical-table td` and `.text-center` in both narrow and A4 views.
- Q117 / Tests: Added one unit test `q117_moreRow_isLeftAligned` in PrintLayoutTest.kt verifying that the generated CSS defines a rule for `.more-row` with `text-align: left`.

Assumptions: None.
Could not do / verify: None.

Verification results:
- ./gradlew --offline :app:testDebugUnitTest --tests '*PrintLayoutTest': PASSED
- ./gradlew --offline :app:testDebugUnitTest (full suite): PASSED
- ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin :app:compileDebugAndroidTestKotlin: PASSED
- python3 .agents/checks/check_r3x.py: PASSED (S97 PASS, S98 PASS)
- ./gradlew --offline :app:testDebugUnitTest --tests '*PrintSampleDumpTest': PASSED
