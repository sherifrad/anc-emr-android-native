ROLE: TEST DESIGNER for ROUND 3t (SPEC_FIX_R3.md section "Round 3t"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3t.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/GestationalDisplayTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt (rewrite per Q106).
1. check_r3t.py: S86-S88 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL; exit 1 on FAIL; style of .agents/checks/check_r3s.py; each S must FAIL on today's code.
2. GestationalDisplayTest per the "Unit" paragraph with exact values. Rewrite GestationalBannerLogicTest so every scenario calls the production members (read the file first; keep each scenario's inputs and expected numbers; replace local formula copies by the production calls; list in your final message what you replaced).
3. TEST_MANIFEST.json: add STATIC-r3t and UNIT-gestational-display (cmd, deps, covers); the banner logic test already has an entry: make sure its deps include GestationalAgeEngine.kt.
Run check_r3t.py and report real results (the unit tests will not compile until core is done).
