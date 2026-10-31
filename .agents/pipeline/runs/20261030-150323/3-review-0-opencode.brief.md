# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3a. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (oracles Q19-Q21, S1-S4, G1-G3) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

CURRENT TASK = ROUND 3a-2: review Q19(h)-(k) first, then search the WHOLE generator again for any remaining fallback that states a clinical finding, and re-check Q19(a)-(g), Q20, Q21 for regressions.
ROLE: INDEPENDENT REVIEWER (read-only). You did not write this code. ALLOWED files: none. Reply with text only; do not modify anything.
Review the CURRENT source against SPEC_FIX_R3.md (Q19-Q21, S1-S4) and OWNER_DECISIONS.md (D8): ui/report/PatientPrintHtmlGenerator.kt, state/IntakeDatingRule.kt, state/AncViewModel.kt (profile-save branch), storage/RecordsRepository.kt (seed function), storage/VaultController.kt (seed call), app/build.gradle.kts, the tests under app/src/test/ and .agents/checks/check_r3.py (open it with your read tool; if you cannot, say so).
For each oracle: PASS/FAIL with a concrete input and your mental output for FAIL. Then hunt for NEW defects: any remaining invented clinical default on the printed report (search the whole generator for "?: \"" fallbacks that state a finding), a path where profile save can still change an ultrasound/IVF EDD, a release vault that would still get demo rows or fail validation with zero patients, a debug build that lost its demo patients, any change to the Q17 Rh/risk behaviour. Say whether any test was weakened (the single Q17 null-risk assertion change is mandated). Severity: Hard Blocker / Should-fix / Nit. End with what you could not verify. Max 60 lines.


ALLOWED (machine-enforced): NONE (read-only)
