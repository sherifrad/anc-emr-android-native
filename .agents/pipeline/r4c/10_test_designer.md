ROLE: TEST DESIGNER for ROUND 4c (SPEC_FIX_R4.md section "Round 4c"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r4c.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/codex/LabItemEditTest.kt (NEW).
1. check_r4c.py and the TEST_MANIFEST.json entries ALREADY EXIST (written by the orchestrator): read them, do NOT rewrite; only fix a clear bug in them and say so. Each S must FAIL on today's code.
2. LabItemEditTest.kt per the "Unit" paragraph; call the real applyLabItemEdit (package com.sherifradwan.ancnative.model.codex); do not copy production logic.
Run check_r4c.py and report real results (the unit test will not compile until the core lane has run: say so).
