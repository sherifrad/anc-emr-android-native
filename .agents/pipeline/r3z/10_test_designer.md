ROLE: TEST DESIGNER for ROUND 3z (SPEC_FIX_R3.md section "Round 3z"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3z.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/FormatIsoDateTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/state/SaveUiErrorTextTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/ui/ClinicalFormatTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/ui/ClinicalPresetsTest.kt (NEW).
1. check_r3z.py: S101-S102 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL; exit 1 on FAIL; style of .agents/checks/check_r3x.py; each S must FAIL on today's code.
2. The four unit test files per the "Unit" paragraph (they call formatIsoDate, SaveUiError?.stringRes, formatFetalMetrics, the frequencies constant; use the real SaveUiError enum and R.string ids; do not copy production logic).
3. TEST_MANIFEST.json: add STATIC-r3z and UNIT-r3z-small (runs the four classes, deps, covers Q120-Q125, S101-S102).
Run check_r3z.py and report real results.
