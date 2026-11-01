ROLE: TEST DESIGNER for ROUND 4d (SPEC_FIX_R4.md section "Round 4d"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r4d.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/LabResultFlagTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/model/codex/LabPanelResolveTest.kt (NEW).
1. check_r4d.py and the TEST_MANIFEST.json entries ALREADY EXIST (written by the orchestrator): read them, do NOT rewrite; only fix a clear bug in them and say so. Each S must FAIL on today's code.
2. The two unit test files per the "Unit" paragraph; call the real labResultFlag / resolveLabPanel (packages com.sherifradwan.ancnative.model and ...model.codex); never copy production logic.
Run check_r4d.py and report real results (the unit tests will not compile until the core lane has run: say so).
