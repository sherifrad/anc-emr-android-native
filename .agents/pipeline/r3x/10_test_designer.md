ROLE: TEST DESIGNER for ROUND 3x (SPEC_FIX_R3.md section "Round 3x"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3x.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintLayoutTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintSampleDumpTest.kt (NEW).
1. check_r3x.py: S97-S98 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL; exit 1 on FAIL; style of .agents/checks/check_r3v.py; each S must FAIL on today's code.
2. PrintLayoutTest per the "Unit" paragraph; copy the builder shapes of ui/report/PrintSectionTitlesTest.kt (do not edit that file). The tests call PatientPrintHtmlGenerator.generateHtml only. They must not hard-code the production CSS beyond the rules the spec names.
3. PrintSampleDumpTest per the spec (writes app/build/print-sample/report.html; plain JUnit, resolve the build folder from a system property or the working directory so it works under Gradle).
4. TEST_MANIFEST.json: add STATIC-r3x and UNIT-print-layout (runs PrintLayoutTest, deps, covers Q115/Q116/Q117 and S97/S98).
Run check_r3x.py and report real results.
