Running `PrintSampleDumpTest` to refresh the sample report...
Running the full unit test suite...
Files changed:
- app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt
- app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintLayoutTest.kt

What was done per oracle:
- Q115 (Narrow screen layout & nowrap/wrap fixes): Wrapped each of the five clinical tables in `<div class="table-scroll">`. In `@media screen and (max-width: 540px)`, configured `.table-scroll` with `overflow-x: auto; -webkit-overflow-scrolling: touch;` and `.clinical-table` with `min-width: 600px;`; removed redundant narrow font-squeezing rules.
- Q115 (Free-text cascade): Increased specificity for `.clinical-table td.notes-cell` and `.clinical-table.mini-table td:nth-child(4)` with `overflow-wrap: anywhere; word-break: normal;` so free-text breaking wins over `.clinical-table td`.
- Q115 (Cell nowrap separation): Removed `.clinical-table td.text-navy { white-space: nowrap; }`. Assigned `nowrap-cell` directly to BP and EFW cells; lab test names and medication names wrap at spaces.
- Q116-Q117 / S97-S98: Preserved single stamp label, escapeHtml everywhere, MAX_PRINT_* constants, and hiddenRowsHtml logic.
- PrintLayoutTest: Added unit tests verifying all 5 tables reside in `.table-scroll`, narrow block contains scroll/min-width while print rules do not, no `td.text-navy` nowrap rule exists, and lab/medication names wrap without `nowrap`.

Assumptions:
- Applied `min-width: 600px` for `.clinical-table` in the narrow screen media query to safely fit all dates, "Gestational", "Presentation", and "Prophylaxis" badges.

Verification results:
- python3 .agents/checks/check_r3x.py: S97 and S98 passed.
- ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin :app:compileDebugAndroidTestKotlin: passed.
- ./gradlew --offline :app:testDebugUnitTest --tests '*Print*': passed.
- ./gradlew --offline :app:testDebugUnitTest --tests '*PrintSampleDumpTest' --rerun-tasks: passed; sample report refreshed.
- ./gradlew --offline :app:testDebugUnitTest (full suite): passed.
- Visual inspection in browser: not run.
