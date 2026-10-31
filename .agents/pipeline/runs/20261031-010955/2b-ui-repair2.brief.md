# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3j. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3j": oracles Q71-Q74, S55-S58) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: UI BUILDER for ROUND 3j. Implement Q71, Q72, Q73 (callers), Q74.
ALLOWED files: app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundEditor.kt, ui/WorkspaceScreen.kt, ui/ProfileScreen.kt, ui/AddAttachmentDialog.kt, ui/ClinicalAttachmentViewerDialog.kt; app/src/main/res/values/strings.xml, app/src/main/res/values/plurals.xml (NEW). Use the Edit/Write tool directly.
Reuse attachmentDisplayTitle (written by the core lane in ui/ClinicalFormat.kt) - do not write another title rule. Keep every testTag and behaviour not named in the spec. No emoji characters anywhere in the files you touch for button labels.
Run: python3 .agents/checks/check_r3j.py, python3 .agents/checks/check_r3e.py, python3 .agents/checks/check_r3f2.py, ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin. Report real results.


ALLOWED (machine-enforced): app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundEditor.kt, app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt, app/src/main/java/com/sherifradwan/ancnative/ui/AddAttachmentDialog.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt, app/src/main/res/values/strings.xml, app/src/main/res/values/plurals.xml

REPAIR ROUND: these checks still fail.
rofileScreen.kt/edit_delivery_plan) no references (vacuous; resource must still exist for U6 where applicable)
PASS U4(ProfileScreen.kt/edit_clinical_alerts) no references (vacuous; resource must still exist for U6 where applicable)
PASS U4(WorkspaceScreen.kt/edit_dating) all 1 reference(s) are pencil-button contentDescriptions
PASS U4(WorkspaceScreen.kt/edit_history) no references (vacuous; resource must still exist for U6 where applicable)
PASS U4(WorkspaceScreen.kt/edit_ultrasound) all 2 reference(s) are pencil-button contentDescriptions
PASS U4(WorkspaceScreen.kt/edit_visit) all 1 reference(s) are pencil-button contentDescriptions
PASS U4(WorkspaceScreen.kt/edit_other_histories) all 1 reference(s) are pencil-button contentDescriptions
PASS U4(WorkspaceScreen.kt/edit_lab) all 1 reference(s) are pencil-button contentDescriptions
PASS U4(WorkspaceScreen.kt/edit_medication) all 1 reference(s) are pencil-button contentDescriptions
PASS U4(WorkspaceScreen.kt/edit_delivery_plan) all 1 reference(s) are pencil-button contentDescriptions
PASS U4(WorkspaceScreen.kt/edit_clinical_alerts) all 1 reference(s) are pencil-button contentDescriptions
PASS U5(WorkspaceScreen.kt/correct_previous_pregnancy) line 1054: Text inside TextButton
PASS U6(WorkspaceScreen.kt/edit_clinical_alerts) resource used, no literal, no size(32.dp)
FAIL U7(ProfileScreen.kt/photos_count) R.string.photos_count is never shown via stringResource(R.string.photos_count, ...)
FAIL U-r2 overall: 29/30 sub-checks passed

PASS S55 calculated-GA card stacks the Text and apply button in a Column
FAIL S56 photos_count is a plural, no string variant, four pluralStringResource call sites: photos_count lacks <item name="one">%d photo</item>; photos_count lacks <item name="other">%d photos</item>
PASS S57 attachment display title is the one helper, WorkspaceScreen calls it
PASS S58 photo-preview buttons are emoji-free Retake/Gallery string resources
