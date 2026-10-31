ROLE: TEST DESIGNER for ROUND 3u (SPEC_FIX_R3.md section "Round 3u"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3u.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/storage/DraftCodecTest.kt (NEW), app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt (schema list, version numbers and new draft tests only).
1. check_r3u.py: S89-S91 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL; exit 1 on FAIL; style of .agents/checks/check_r3t.py; each S must FAIL on today's code.
2. DraftCodecTest per the "Unit" paragraph (it calls encodeDraftFields, decodeDraftFields, MAX_DRAFT_PAYLOAD_BYTES, DraftRecord that core will add).
3. EncryptedRecordsTest (androidTest): update the table-set and the two schema-version expectations (5 -> 6) and add the draft tests listed under "Instrumented"; reuse its withDatabase/command helpers; the file must COMPILE (./gradlew --offline :app:compileDebugAndroidTestKotlin) once core exists; you cannot run it.
4. TEST_MANIFEST.json: add STATIC-r3u and UNIT-draft-codec (cmd, deps, covers).
Run check_r3u.py and report real results.
