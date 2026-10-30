ROLE: TEST DESIGNER for ROUND 3g (SPEC_FIX_R3.md section "Round 3g"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3g.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/storage/ScanPhotoRefTest.kt (NEW), storage/CameraCaptureSweepTest.kt (NEW), state/ScanPhotoImportStateTest.kt (NEW).
1. check_r3g.py: S45-S49 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL per S; exit 1 on any FAIL; style of check_r3f3.py; each S must FAIL on today's code. GREP the older checks (.agents/checks/check_r3*.py) for any code shape this round changes (AncViewModel addAttachment, UltrasoundEditor camera code, CameraCaptureHelper.clearStaleCaptures) and list them in your final message.
2. The three unit-test classes per the "Unit:" paragraph, using only the APIs named in the spec.
3. TEST_MANIFEST.json: add STATIC-r3g, UNIT-scan-photo-ref, UNIT-camera-sweep, UNIT-scan-photo-import (cmd, deps, covers); edit directly; keep other entries unchanged.
Run check_r3g.py and report real results (unit tests will not compile until core is done).
