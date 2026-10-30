ROLE: TEST DESIGNER for ROUND 3h (SPEC_FIX_R3.md section "Round 3h"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3h.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/storage/CaptureTempFileTest.kt (NEW).
1. check_r3h.py: S50 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL; exit 1 on FAIL; style of check_r3g.py; it must FAIL on today's code. Grep the older checks (.agents/checks/check_r3*.py) for any shape of deleteIfCaptureTemp / canonicalFile in CameraCaptureHelper.kt and list them in your final message.
2. CaptureTempFileTest per the "Unit" paragraph (JUnit4, TemporaryFolder rule, only APIs named in the spec).
3. TEST_MANIFEST.json: add STATIC-r3h and UNIT-capture-temp-file (cmd, deps, covers); edit directly; keep others unchanged.
Run check_r3h.py; report real results (the unit test will not compile until core is done).
