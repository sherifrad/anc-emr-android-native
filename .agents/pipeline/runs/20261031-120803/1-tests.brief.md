# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3p. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3p": oracles Q94-Q95, S78-S79) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. NEVER write dead code, extra XML/files or comments only to satisfy a static check or a test; if a check looks wrong, implement the spec and say so in your final message. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: TEST DESIGNER for ROUND 3p (SPEC_FIX_R3.md section "Round 3p"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3p.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintSectionTitlesTest.kt (NEW).
1. check_r3p.py: S78 and S79 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL per S; exit 1 on FAIL; style of .agents/checks/check_r3o.py; each S must FAIL on today's code. Read the generator and the dialog first so the patterns match the real text.
2. PrintSectionTitlesTest per the "Unit" paragraph (read PatientPrintHtmlGeneratorTest.kt for the builders; do not edit it). Check the real printed wording of each title in the generator before asserting.
3. TEST_MANIFEST.json: add STATIC-r3p and UNIT-print-titles (cmd, deps incl. both source files, the test and the check, covers). Keep others unchanged.
Run check_r3p.py and report real results; the unit test must fail on today's code for the duplicate-title assertions.


ALLOWED (machine-enforced): .agents/checks/check_r3p.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintSectionTitlesTest.kt
