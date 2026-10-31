# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3b. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3b": oracles Q22-Q24, S5-S7, Gates 3b) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: TEST DESIGNER (tests first). Write tests from the "Round 3b" section of SPEC_FIX_R3.md only. You may read model/ (DemoPatient, DemoPreviousPregnancy, DemoFixtures, PatientVocabulary) and storage/VaultState.kt and storage/SaveVisitCommand.kt (RecordSnapshot) to build inputs; do not read the bodies of RecordsRepository.kt, VaultController.kt, VaultFiles.kt or MainActivity.kt beyond what S5-S7 need. findBlockingRecordProblems / findRecordAnomalies / RecordAnomaly / vaultAutoAction / VaultAutoAction / hasVaultDatabase do not exist yet: compile failures of your three new test files are expected ONLY for those names.
ALLOWED files:
- app/src/test/java/com/sherifradwan/ancnative/storage/RecordAnomaliesTest.kt   (NEW: every Q22 example exactly; every anomaly field once; every blocking rule once; DemoFixtures.patients -> both lists empty; order rule)
- app/src/test/java/com/sherifradwan/ancnative/storage/VaultAutoActionTest.kt   (NEW: Q23 - every VaultState kind, every VaultBlockReason -> NONE)
- app/src/test/java/com/sherifradwan/ancnative/storage/VaultCleanupRuleTest.kt   (NEW: Q24 hasVaultDatabase with JUnit TemporaryFolder: every true/false case in the spec)
- .agents/checks/check_r3b.py   (NEW: S5-S7, stdlib Python, PASS/FAIL per sub-check, exit 1 if any FAIL; must FAIL today on S5-S7)
- .agents/TEST_MANIFEST.json   (keep every entry; ADD exactly UNIT-anomalies, UNIT-vault-auto, UNIT-vault-cleanup (tier 1, --tests as in the spec, deps = the new production file + the test) and STATIC-r3b (tier 1, deps MainActivity.kt, VaultController.kt, VaultFiles.kt, the check))
Do not implement production code. Report which tests/checks fail today and that no compile error has another cause.


ALLOWED (machine-enforced): app/src/test/java/com/sherifradwan/ancnative/storage/RecordAnomaliesTest.kt, app/src/test/java/com/sherifradwan/ancnative/storage/VaultAutoActionTest.kt, app/src/test/java/com/sherifradwan/ancnative/storage/VaultCleanupRuleTest.kt, .agents/checks/check_r3b.py, .agents/TEST_MANIFEST.json
