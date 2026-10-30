ROLE: TEST DESIGNER for ROUND 3i (SPEC_FIX_R3.md section "Round 3i"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3i.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/IsoDatesTest.kt (NEW).
1. check_r3i.py: S51-S54 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL per S; exit 1 on FAIL; style of check_r3h.py; each S must FAIL on today's code. GREP the older checks (.agents/checks/*.py) for any code shape this round changes (LocalDate.parse, plusDays(280), IntakeDatingRule, PatientEditor LMP preview, GestationalBanner) and list them in your final message.
2. IsoDatesTest per the "Unit" paragraph.
3. TEST_MANIFEST.json: add STATIC-r3i and UNIT-iso-dates (cmd, deps incl. every file named in S51-S54, covers); edit directly; keep other entries unchanged.
Run check_r3i.py; report real results (the unit test will not compile until core is done).
