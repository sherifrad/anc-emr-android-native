# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3f-1. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3f-1": oracles Q47-Q53, S29-S35) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: INDEPENDENT REVIEWER (read-only). ALLOWED files: none. Reply with text only, max 45 lines.
Review the Round 3f-1 changes (SPEC_FIX_R3.md section "Round 3f-1"; `git diff` against HEAD). Questions: (1) Q47: is any hash output different from before (framing, charset, case)? (2) Q48: does the vault still produce the same image bytes for the same input; any path that can now throw or return a blank image; does any caller handle null? (3) Q49: is the order and the guard identical to the three removed blocks; any caller left with a copy? (4) Q50: is Base64 output compatible with already-stored PIN hashes; any behaviour change from the clock? (5) Q51/Q52: can a failure still be silent; is a cancelled coroutine reported as an error; is the banner shown in every screen where attachments can fail; is the delete order right? (6) Do the new tests really exercise production code (no re-implemented formulas, no tautologies)? Mark each finding Hard Blocker / Should-fix / Nit and quote the line. Do not report things outside the diff.


ALLOWED (machine-enforced): NONE (read-only)
