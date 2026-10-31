ROLE: TEST DESIGNER for ROUND 3r (SPEC_FIX_R3.md section "Round 3r"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3r.py (NEW), .agents/TEST_MANIFEST.json, app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt (only the FLAG_SECURE assertion block in recordsWindowKeepsSecureFlagAndGateExplainsDraftPolicy).
1. check_r3r.py: S83 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL; exit 1 on FAIL; style of .agents/checks/check_r3q.py; it must FAIL on today's code.
2. Update the flag assertion per Q101 (assert the secure flag is NOT set in every build; remove the BuildConfig.DEBUG branch). Do not touch anything else in that test or file.
3. TEST_MANIFEST.json: add STATIC-r3r (cmd, deps = MainActivity.kt and the check, covers S83).
Run check_r3r.py and ./gradlew --offline :app:compileDebugAndroidTestKotlin. Report real results.
