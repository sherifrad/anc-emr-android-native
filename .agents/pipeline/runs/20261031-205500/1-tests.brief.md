# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3y. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3y": oracles Q118-Q119, S99-S100) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. NEVER write dead code, extra XML/files or comments only to satisfy a static check or a test; if a check looks wrong, implement the spec and say so in your final message. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: TEST DESIGNER for ROUND 3y (SPEC_FIX_R3.md section "Round 3y"). Tests and checks only; no production code.
ALLOWED files: .agents/checks/check_r3y.py (NEW), .agents/checks/check_r3w.py (only: drop values-ar from its STRINGS list and docstring), .agents/TEST_MANIFEST.json, app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt, app/src/androidTest/java/com/sherifradwan/ancnative/SecurityWorkflowTest.kt, app/src/androidTest/java/com/sherifradwan/ancnative/storage/VaultIntegrationTest.kt (edit only what Q119 lists).
1. check_r3y.py: S99-S100 as the spec says; python3 stdlib only; strip Kotlin comments (XML comments) first; PASS/FAIL; exit 1 on FAIL; style of .agents/checks/check_r3x.py; each S must FAIL on today's code (S99 passes on the folder part already: make the check meaningful through the other parts).
2. Q119 a-e exactly as written. The androidTest source set must COMPILE (./gradlew --offline :app:compileDebugAndroidTestKotlin); you cannot run it on a device. Read the code under test (VaultController, SecuritySettingsDialog, the draft recovery dialog tags draftContinue / draftDiscard) before you change an assertion; never weaken an assertion to make it pass.
3. TEST_MANIFEST.json: add STATIC-r3y and LINT-errors as the spec says.
Run check_r3y.py and report real results.


ALLOWED (machine-enforced): .agents/checks/check_r3y.py, .agents/checks/check_r3w.py, .agents/TEST_MANIFEST.json, app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt, app/src/androidTest/java/com/sherifradwan/ancnative/SecurityWorkflowTest.kt, app/src/androidTest/java/com/sherifradwan/ancnative/storage/VaultIntegrationTest.kt
