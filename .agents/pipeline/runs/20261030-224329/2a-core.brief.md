# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3f-3. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3f-3": oracles Q61-Q62, S43-S44) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: CORE BUILDER for ROUND 3f-3. Implement Q61 in app/src/main/java/com/sherifradwan/ancnative/storage/BoundedBitmap.kt (and PatientAttachmentVault.kt only if it needs a change; it should not). Make the tests and checks pass without editing them.
Pure exifTransform first (it is unit-tested); then the Matrix application after the bounded decode; EXIF read failures mean normal. Do not change the decode limits or the sample-size rule.
Run: ./gradlew --offline :app:testDebugUnitTest (whole suite), python3 .agents/checks/check_r3f3.py (S43 core part), every older .agents/checks/check_r3*.py, ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin. Report real results and assumptions.


ALLOWED (machine-enforced): app/src/main/java/com/sherifradwan/ancnative/storage/BoundedBitmap.kt, app/src/main/java/com/sherifradwan/ancnative/storage/PatientAttachmentVault.kt
