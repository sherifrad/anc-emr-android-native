ROLE: UI BUILDER for ROUND 3d. Implement Q33, Q36, Q39 (UI parts) and Q40 from the spec section "Round 3d" of SPEC_FIX_R3.md.
ALLOWED files (app/src/main/java/com/sherifradwan/ancnative/ui/): SecuritySettingsDialog.kt, report/PatientTwoPageReportDialog.kt, WorkspaceScreen.kt, PatientEditor.kt; app/src/main/res/values/strings.xml (only if you move a string there).
Keep every existing testTag. Match the surrounding code style. The restore button must keep working exactly as today once "Replace all" is pressed. The dialog sits inside SecuritySettingsDialog, which is already hidden while locked - do not add a new top-level window elsewhere.
Run: python3 .agents/checks/check_r3d.py (all S17-S22 must pass once core is done) and ./gradlew --offline :app:compileDebugKotlin. Report real results.
