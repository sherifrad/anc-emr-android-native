# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3e. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3e": oracles Q41-Q46, S23-S28) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: UI BUILDER for ROUND 3e. Implement Q42, Q43, Q44, Q45, Q46 from the spec section "Round 3e".
ALLOWED files (app/src/main/java/com/sherifradwan/ancnative/ui/): AppLockScreen.kt, UltrasoundPhotoViewerDialog.kt, ClinicalAttachmentViewerDialog.kt, Theme.kt, PatientListScreen.kt, ProfileScreen.kt, AddAttachmentDialog.kt; app/src/main/res/values/strings.xml.
Keep every existing testTag and every behaviour not named in the spec. Q44: change ONLY the text colour of a pair that fails 4.5:1.
Run: python3 .agents/checks/check_r3e.py (S25-S28 are yours), ./gradlew --offline :app:testDebugUnitTest --tests '*ThemeContrastTest', ./gradlew --offline :app:compileDebugKotlin. Report real results.


ALLOWED (machine-enforced): app/src/main/java/com/sherifradwan/ancnative/ui/AppLockScreen.kt, app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt, app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt, app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt, app/src/main/java/com/sherifradwan/ancnative/ui/AddAttachmentDialog.kt, app/src/main/res/values/strings.xml

REPAIR ROUND: these checks still fail.
FAIL S5a LaunchedEffect(state.vaultState) found
PASS S6a deleteOrphanAliases() call sites present
PASS S6b call 1 guarded by hasVaultDatabase
PASS S6b call 2 guarded by hasVaultDatabase
PASS S6b call 3 guarded by hasVaultDatabase
PASS S7 cleanupUnpublishedArtifacts calls hasVaultDatabase
