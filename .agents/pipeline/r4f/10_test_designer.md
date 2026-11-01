ROLE: TEST DESIGNER for ROUND 4f (SPEC_FIX_R4.md section "Round 4f"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r4f.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/DueDatesByMethodTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/state/DatingDraftRuleTest.kt (NEW).
1. check_r4f.py and the TEST_MANIFEST.json entries ALREADY EXIST (written by the orchestrator): read them, do NOT rewrite; only fix a clear bug in them and say so. Each S must FAIL on today's code.
2. The two unit test files per the "Unit" paragraph, using the exact numbers and cases listed there; call the real functions (packages com.sherifradwan.ancnative.model and ...state; the state functions are `internal`, usable from the same module's tests); never copy production logic. Fix asOf dates explicitly, never LocalDate.now().
Run check_r4f.py and report real results (the unit tests will not compile until the core lane has run: say so).
