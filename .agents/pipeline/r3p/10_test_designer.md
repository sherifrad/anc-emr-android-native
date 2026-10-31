ROLE: TEST DESIGNER for ROUND 3p (SPEC_FIX_R3.md section "Round 3p"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3p.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintSectionTitlesTest.kt (NEW).
1. check_r3p.py: S78 and S79 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL per S; exit 1 on FAIL; style of .agents/checks/check_r3o.py; each S must FAIL on today's code. Read the generator and the dialog first so the patterns match the real text.
2. PrintSectionTitlesTest per the "Unit" paragraph (read PatientPrintHtmlGeneratorTest.kt for the builders; do not edit it). Check the real printed wording of each title in the generator before asserting.
3. TEST_MANIFEST.json: add STATIC-r3p and UNIT-print-titles (cmd, deps incl. both source files, the test and the check, covers). Keep others unchanged.
Run check_r3p.py and report real results; the unit test must fail on today's code for the duplicate-title assertions.
