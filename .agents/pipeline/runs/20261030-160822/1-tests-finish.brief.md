# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3c. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3c": oracles Q25-Q29, S8-S13, Gates 3c) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: TEST DESIGNER (tests first). Write tests from the "Round 3c" section of SPEC_FIX_R3.md only. Do not read the bodies of SecuritySettingsRepository.kt, BiometricAuthManager.kt, MainActivity.kt or the UI files beyond what S8-S13 need. PinPolicy / AppLockPolicy functions and constants do not exist yet: compile failures of your two new test files are expected ONLY for those names.
ALLOWED files:
- app/src/test/java/com/sherifradwan/ancnative/security/PinPolicyTest.kt   (NEW: Q25 - every isValidNewPin example plus all ten ascending/descending runs, hashPin determinism and salt sensitivity, 32-byte output, a known PBKDF2-HMAC-SHA256 vector you compute with javax.crypto in the test itself at low iterations, pinMatches true/false, newSalt length 16 and two salts differ, every lockoutDelayMs boundary)
- app/src/test/java/com/sherifradwan/ancnative/security/AppLockPolicyTest.kt   (NEW: Q26 - every example and the exact 600_000 boundary)
- .agents/checks/check_r3c.py   (NEW: S8-S13, stdlib Python, PASS/FAIL per sub-check, exit 1 if any FAIL; must FAIL today on S8, S9, S10, S11, S12; S13 may already pass or fail - report which)
- .agents/TEST_MANIFEST.json   (keep every entry; ADD exactly UNIT-pin-policy, UNIT-lock-policy (tier 1, --tests as in the spec, deps = the new production file + its test) and STATIC-r3c (tier 1, deps SecuritySettingsRepository.kt, BiometricAuthManager.kt, MainActivity.kt, the check))
Do not implement production code. Report which tests/checks fail today and that no compile error has another cause.


ALLOWED (machine-enforced): app/src/test/java/com/sherifradwan/ancnative/security/PinPolicyTest.kt, app/src/test/java/com/sherifradwan/ancnative/security/AppLockPolicyTest.kt, .agents/checks/check_r3c.py, .agents/TEST_MANIFEST.json

FINISH-UP ROUND: your previous run timed out. Your test files already exist; do not rewrite them. Do ONLY this: Spec correction by the orchestrator: S13 is scoped to security/SecuritySettingsRepository.kt ONLY (see the amended S13 line in SPEC_FIX_R3.md). Edit .agents/checks/check_r3c.py with your file-edit tool so S13 scans only that file. Change nothing else; do not touch the tests or the manifest.