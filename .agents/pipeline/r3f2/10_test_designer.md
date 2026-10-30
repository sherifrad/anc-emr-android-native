ROLE: TEST DESIGNER for ROUND 3f-2 (SPEC_FIX_R3.md section "Round 3f-2"). Write tests only; no production code.
ALLOWED files: .agents/checks/check_r3f2.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/storage/PhotoQualityPresetTest.kt (NEW).
1. check_r3f2.py: S37-S42 exactly as the spec; python3 stdlib only; strip Kotlin comments first; PASS/FAIL per S; exit 1 on any FAIL; style of check_r3f1.py; every S must FAIL on today's code.
2. PhotoQualityPresetTest per the "Unit:" paragraph.
3. TEST_MANIFEST.json: add STATIC-r3f2 (cmd python3 .agents/checks/check_r3f2.py, deps = every file named in S37-S42 plus the check) and UNIT-photo-quality (class *PhotoQualityPresetTest, deps PhotoQuality.kt + the test); keep other entries unchanged. Edit the file directly.
READ app/build.gradle.kts, ui/AddAttachmentDialog.kt, ui/UltrasoundEditor.kt and storage/PatientAttachmentVault.kt first. Run check_r3f2.py and report real results (the unit test will not compile until core is done; say so).
