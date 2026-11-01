ROLE: TEST DESIGNER for ROUND 4b (SPEC_FIX_R4.md section "Round 4b"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r4b.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/UltrasoundImpressionTest.kt (NEW).
1. check_r4b.py and the TEST_MANIFEST.json entries ALREADY EXIST (written by the orchestrator): read them, do NOT rewrite; only fix a clear bug in them and say so. Original description: S120-S122 as the spec says; python3 stdlib only; strip Kotlin comments; PASS/FAIL; exit 1 on FAIL; style of .agents/checks/check_r4a.py; each S must FAIL on today's code.
2. UltrasoundImpressionTest.kt per the "Unit" paragraph; call the real ultrasoundImpressionFromChoices and mergeImpression (package com.sherifradwan.ancnative.model); do not copy production logic.
Run check_r4b.py and report real results (the unit test will not compile until the core lane has run: say so).
