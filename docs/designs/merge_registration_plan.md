# Plan: bring the registration flow branch into main

Branch: `claude/clever-goldwasser-e086ce` (tip b07e772, 15 commits on b4337fc). Main: bace905 (42 commits on b4337fc).
Checked against the real repository with `git merge-tree --write-tree` (writes objects only) and `git show` / `git diff`.
Nothing in the repo was changed except this file.

## 0. What the check found (read this first)

The dry run reports 8 content conflicts, but the textual conflict list hides the real work.

1. Main split `state/AncState.kt` into `AncState.kt` (807 lines), `AncEvent.kt`, `AncReducer.kt`,
   `ClinicalDraftReducers.kt`, `MedicationReducer.kt`, `ReducerSupport.kt`. The branch still edits the old
   1,871-line `AncState.kt` (events and reducer inside it). Git cannot move those edits. Conflict hunk 5 of
   `AncState.kt` (merged file lines 824 to 2003) is the branch's whole old copy of `AncEvent` plus `AncReducer`.
   Taking it ("branch wins") would put a second `AncEvent` and a second `AncReducer` next to the new files and
   not compile. The right resolution takes main's side and re-applies the branch's reducer edits by hand in the
   new files (section 3.1, list R1 to R9).
2. The branch's `RegistrationFlowDriver.kt` calls `AncReducer.startPatientEditor`, `AncReducer.startDatingEditor`,
   `AncReducer.startHistoriesEditor`, `AncReducer.startPregnancyEditor`, `AncReducer.requestBack(state)` and
   `with(AncReducer) { clearDrafts() }`. On main the first four plus `clearDrafts` are top-level `internal`
   functions in other files, and `requestBack(state, event)` is private with a different signature. This file
   compiles on neither side until adapted.
3. The branch makes patient age required on create (`PatientProfileRules.ageRequired`, used by
   `SavePatientProfileCommand.prepare` and by `isStructurallyValid`). Main's tests build a new patient with
   `StartPatientEditor(null, ...)` plus name and case id, then call `prepare` directly, with no age:
   `DeliveryPlanViewModelTest`, `MedicationStopViewModelTest` (androidTest), `NegativeControlAndClinicalValidationTest`,
   and any other seed helper. They would be rejected with `AGE_REQUIRED`. Not a textual conflict.
4. The branch deletes `state/IntakeDatingRule.kt` and four tests. Main's `DeliveryPlanReducerTest` (line 259, test
   "the sheet never asks for a dating update") still calls `intakeDatingUpdate`. Compile break in unit tests.
5. Goal check: on the branch, only a NEW patient starts the flow (`startPatientEditor` calls `withNewPatientFlow` only when
   `patient == null`). `PatientHubScreen`, `OverviewTab` and `PlanTab` are unchanged on the branch, so the header
   pencil still sends `StartPatientEditor(existing id)` and opens the OLD editor. No UI sends `AncEvent.Flow(FlowEvent.Begin)`
   for an existing patient, and the directory "Incomplete" chip is display only. Merging does not deliver "the pencil
   opens the new flow". See owner question 1.
6. Main is one local commit ahead of `origin/main` (bace905 vs 93da37d). `docs/designs/reviews/merge_registration/` is
   untracked in the main tree. `.agents/scripts/push_gate.sh pre` compares protected files against
   `STAGE_BASE` (default `c9c1a82~1`) and its pattern includes `Generator`; this merge changes
   `PatientPrintHtmlGenerator.kt` (branch: signature line), so the gate needs `STAGE_BASE` set deliberately (step 14).

Corrections to the draft plan (alphacode): see section 9.

## 1. Strategy and why

Chosen: merge the branch into an integration branch cut from main, in a separate git worktree. Main stays at bace905
until the integration branch is fully green and the owner approves; then `git merge --ff-only` main to it.

    git worktree add -b integration/registration-merge /Users/sherifgamalradwan/anc-emr-integration main
    cd /Users/sherifgamalradwan/anc-emr-integration
    git merge --no-ff --no-commit claude/clever-goldwasser-e086ce

| Option | History | Conflicts | WIP commit | Rollback | Bisect |
|---|---|---|---|---|---|
| Rebase branch onto main (replay 15 commits) | Rewrites 15 reviewed commits, loses their hashes and the review trail | Every commit that touches the old `AncState.kt` conflicts again (about 8 of 15), each needs the manual port | WIP commit replayed too; squashing needs interactive rebase, not available here | Reset to tag, but the branch hashes are gone unless tagged | Looks linear, but each replayed commit needs its own port to compile; intermediate commits will not build |
| Merge main into the branch itself | Keeps hashes, adds a merge commit on the branch | One pass | WIP stays | Revert or reset on the branch, which is the only copy | Fine, but touches the fallback copy |
| Merge branch into integration branch from main (chosen) | Keeps 15 commits and main's 42 intact; one merge commit | One pass, one place | WIP stays as history; fine | Delete the integration branch and worktree; branch and main untouched | `git bisect --first-parent` on main; the commits inside the branch keep their own (pre-port) state |

Why not rebase: the draft's own command (`git checkout -b X main` then `git rebase claude/...`) replays main's 42
commits on top of the feature branch, which is the wrong direction. A correct rebase (branch onto main) would
resolve the same state-file split 8 or more times.

Rule inside the merge: the merge commit is committed once the conflicts are resolved and the port is written, even if it
does not yet compile (labelled WIP, AGENTS.md allows labelled WIP commits; work must not live only in a worktree).
Fix-up commits follow, one per verified round. The first merge commit is not buildable; mark it `git bisect skip`
material in its message. The integration branch is never pushed. Only `main` is pushed, and only on the owner's word.

## 2. Pre-merge safety

Run from the main tree `/Users/sherifgamalradwan/anc-emr-android-native`, read-only first:

1. `.agents/scripts/git_audit.sh`. Expected output and what to do with each finding:
   - TRACKED `claude/clever-goldwasser-e086ce` (listed in `docs/BRANCHES.md`): expected.
   - UNMERGED lines for the other worktree branches (`claude/sharp-fermi-1e2fbe` at b4337fc = nothing to merge;
     `claude/upbeat-jemison-05fc1d` at 764e242): record in the roadmap log, do not touch.
   - UNCLEAN main tree: the untracked `docs/designs/reviews/merge_registration/` (this review). Commit it on
     the integration branch in step 1; do not delete.
   - UNPUSHED: main ahead of `origin/main` by 1 (bace905). Expected. It goes out with the final push (or the owner
     pushes it earlier, owner's call).
   - ROADMAP STALE and UNREACHABLE: record, fix the roadmap in the final commit (rule in AGENTS.md).
2. Confirm the branch worktree is clean and tip is b07e772, read-only:
   `git -C .claude/worktrees/clever-goldwasser-e086ce status --porcelain` must print nothing; `git rev-parse claude/clever-goldwasser-e086ce`.
   If it prints changes, stop and commit them on the branch first (owner decision).
3. Tags (annotated, local, never pushed):
   `git tag -a pre-registration-merge-main bace905 -m "main before the registration flow merge"`
   `git tag -a pre-registration-merge-branch b07e772 -m "registration flow branch tip before the merge"`
   (The branch tip is already a commit; no worktree copy is needed. Do not `cp`/`rsync` the worktree into
   `.claude/worktrees/`: it creates stray files the audit flags.)
4. Save evidence: `git merge-tree --write-tree --name-only main claude/clever-goldwasser-e086ce > <scratchpad>/dry_run_before.txt`.
5. Baseline: on a clean main checkout run `./gradlew :app:testDebugUnitTest` once and keep the counts (so a later
   failure is attributable). Do not rerun the 9-minute suite on main unless the owner wants a fresh baseline; the
   last full run is in the roadmap (`.agents/pipeline/HANDOFF_AGY.md`, top section).
6. Check what gradle needs that is not tracked (for example `local.properties`, signing or keystore settings) and copy
   only what the build needs into the new worktree. Never copy `keystore/` contents into a commit.
7. Fallback: delete the worktree (`git worktree remove`), delete `integration/registration-merge`, keep both tags.
   Main and the branch were never modified, so nothing else needs undoing.

## 3. Resolution rules

General rule: main's delivery outcome, delivery plan sheet, medication sheet, Meds stop/undo and AncState split are
kept whole. The branch's registration flow, removed dating fields, age rule, previous-pregnancy form and copy cleanup are
kept whole. Where both touch one line, keep both effects. Anything the branch removed that main's new code still uses is a
compile error to fix on main's side, never a reason to restore the branch's removed code.

### 3.1 `state/AncState.kt` (5 hunks, highest risk, level 3)

| Hunk | Resolution |
|---|---|
| 1 imports | Keep both sets. `composeDeliveryNotes` (main) plus the branch's five imports. Then let the compiler and an unused-import pass remove imports that moved to the new files with the code. |
| 2 `clearedForLock()` | Keep all four: `medicationStopUndo = null, medicationQuickSave = null` (main) and `registrationFlow = null, awaitingAdvance = null` (branch). A vault lock must clear all of them (no clinical state survives a lock). |
| 3 `PatientDraftValues` | Keep main's four: `delivered`, `deliveryDate`, `deliveryMode`, `deliveryOutcome`. Delete the eight dating fields: `lmpDate`, `datingMethod`, `transferDate`, `embryoAgeDays`, `agreedEdd`, `scanDate`, `scanGaWeeks`, `scanGaDays`. Final class ends: `... clinicalAttention`, `delivered`, `deliveryDate`, `deliveryMode`, `deliveryOutcome`. (The draft plan kept dating fields "if still used"; none is used, the rule file is deleted.) |
| 4 `AncState` data class fields | Keep all four: `medicationQuickSave` (with main's doc comment), `medicationStopUndo`, `registrationFlow`, `awaitingAdvance`. |
| 5 `AncEvent` + `AncReducer` block | Take main's side only: keep `AncState.deliveryPlanSheetDraft(...)` and whatever main has after it. Delete the branch's whole copy (about 1,180 lines). Then do R1 to R9 below. Never leave both. |

Non-conflicting branch edits in this file that merged cleanly and must be kept (verify they are present):
`AncRoute.PregnancyCountPrompt`, `PendingAction.FlowMove`, enum entries removed from `PatientDraftField` (8 dating)
and `PregnancyDraftField` (`OUTCOME_TEXT`, `BIRTH_METHOD`, `CLINICIAN_ID`), `SavedNotice.PATIENT_DATING_NOT_UPDATED`
removed, `patientIdOrNull()` case for `PregnancyCountPrompt`, new `AncState` fields.

Port list (branch edits made in the old file, to re-apply in main's new files). Source: `git diff b4337fc claude/clever-goldwasser-e086ce -- app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt`.

- R1 `AncEvent.kt`: add `Flow(event: FlowEvent, editorSessionId, newPregnancyId)` and `RegistrationNext(editorSessionId, newPregnancyId)`.
- R2 `AncReducer.kt` `reduce`: chain `.withFlowConsistent(event)` next to main's `.withMedicationStopUiKept(event)`; keep both, in a fixed order (delete-ui, medication-stop, flow) and add a test that both still run.
- R3 `AncReducer.kt` `reduceInternal`: add `is AncEvent.Flow -> reduceFlowEvent(...)` and `is AncEvent.RegistrationNext -> reduceRegistrationNext(...)`; in the save-committed case add `.advanceFlowAfterCommit(event.token)` to the cleared state; in `AcceptDraftRecovery` add `.withFlowSessionFollowing(prompt.editorSessionId)`.
- R4 `AncReducer.kt` `requestBack` (main's has an `event` parameter and the sheet branches for medication and delivery plan): add `is AncRoute.PregnancyCountPrompt -> AncRoute.Workspace(route.patientId)`; in the dirty-draft branch add `awaitingAdvance = null`. Keep the medication-sheet and delivery-sheet early returns above the route `when`. Make it callable by the driver: either make `requestBack` `internal` and call `AncReducer.requestBack(state, AncEvent.Back)` in the driver, or move it as a top-level internal; one choice, one function.
- R5 `AncReducer.kt` `confirmDiscard`: add `awaitingAdvance = null` to the discarded state and the `PendingAction.FlowMove -> applyFlowMove(discarded, action.event, FlowIds(...))` branch. Main's `when (action)` is exhaustive, so it will not compile until added.
- R6 `ClinicalDraftReducers.kt` `startPatientEditor`: build `started`, then `return if (patient == null) started.withNewPatientFlow(patientId, event.editorSessionId) else started`. Keep main's existing early-return and delivery handling unchanged.
- R7 `ClinicalDraftReducers.kt` `updatePatientDraft`: delete the eight dating cases (lines 348 to 355 on the merged tree). `updatePregnancyDraft`: delete `OUTCOME_TEXT`, `BIRTH_METHOD`, `CLINICIAN_ID`; replace `DELIVERY_BY` and `OUTCOME` with the branch's versions that call `PreviousPregnancyFormRules.deliveryColumns(...)` (copy exact text from the branch diff, do not rewrite).
- R8 `ClinicalDraftReducers.kt` `DemoPatient.toPatientDraftValues()`: delete the `lmpDate = dating?.lmpDate.orEmpty()` line and keep main's delivery field mapping.
- R9 `RegistrationFlowDriver.kt`: replace `AncReducer.startPatientEditor/startDatingEditor/startHistoriesEditor/startPregnancyEditor` with the top-level functions, `with(AncReducer){ clearDrafts() }` with `clearDrafts()` from `ReducerSupport.kt`, and `AncReducer.requestBack(state)` per R4.

Also in main files that auto-merged: grep for other `when (route)` or `when (event)` that must now cover `PregnancyCountPrompt`, `Flow`, `RegistrationNext` (the compiler finds them; do not add `else` branches to silence it).

### 3.2 `storage/PatientCommands.kt` (imports only; body auto-merged)

Keep all four: `DeliveryOutcomeFields`, `normalized`, `validateDeliveryOutcome` (main) and `PreviousPregnancyFormRules` (branch).
The merged `prepare` already has `forceAgeRequired` (branch) and the changed-only delivery check (main). Do not "improve" it.
Check the file's other branch edit (previous-pregnancy `prepare`, `isStructurallyValid` line about 347) survived.

### 3.3 `ui/AncApp.kt` (imports only)

Keep main's `medicationSheetDraft` and all five branch imports. Body merged cleanly: the `when (route)` has
`RegistrationFlowScaffold` around `PatientEditor`, `OtherHistoriesEditor`, `PreviousPregnancyEditor`, `DatingEditor`, and
`UndoBarHost` is hidden while a sheet is open. Verify both survived. `AncRoute.MedicationEditor` is gone on main; after the
port no reference to it may remain (`git grep AncRoute.MedicationEditor` must be empty).

### 3.4 `ui/AncDatePicker.kt`

Result: `label = { Text(stringResource(R.string.today) + " (" + LocalDate.now().toString() + ")") }` (no emoji, the
branch removed it for the owner's no-emoji decision) and `modifier = Modifier.minimumTouchTarget().testTag("${tag}TodayButton")`
(main's 48 dp target). The draft plan kept the emoji ("if intentional"); the branch removed it deliberately.

### 3.5 `ui/PatientListScreen.kt` (2 hunks in `PatientDashboardRowProjection` and `projectDashboardRow`)

Keep all three fields: `isDelivered`, `deliveryDate` (main) and `isIncomplete` (branch), in both the class and the builder.
Keep main's `dashboardPatients(...)`, Delivered filter chip and counts. Keep the branch import
`com.sherifradwan.ancnative.state.isRegistrationIncomplete`. Check the three sets of fields line up in every construction of
`PatientDashboardRowProjection(...)` in tests.

### 3.6 `ui/patienthub/PatientHubHeader.kt`

Keep BOTH: main's wrapper and the branch's call:

    Box(modifier = Modifier.testTag("hubRiskBadge")) {
        ClinicalRiskBadge(
            isHighRisk = isHighRisk(patient.riskLevel),
            isModerate = parseRiskTier(patient.riskLevel) == PatientRiskTier.MEDIUM,
        )
    }

`PatientHubDesignTest` (androidTest, line 126) asserts the `hubRiskBadge` tag. The draft's "branch wins" would drop that tag.
`ClinicalRiskBadge(isHighRisk, modifier, isModerate = false)` already has the default, so other callers are safe.
Confirm `parseRiskTier` and `PatientRiskTier` imports are present.

### 3.7 `res/values/strings.xml`

Keep both blocks, main's first. Verified: no key is added by both sides; the merged file has no duplicate `name=`.
The branch removed 9 keys (`age_text`, `birth_method`, `clinician_id_required`, `date_of_birth`, `dating_method_select_hint`,
`entered_outcome_text`, `historical_delivery_mode`, `patient_saved_dating_not_updated`, `phone`); the merged tree has no
`R.string.<those>` use. After resolving, still run a duplicate-key check, an unused/missing-resource check (lint), and make sure the
branch hunk (shown truncated in the brief) ended up complete, including its `<plurals>` block.

### 3.8 `.agents/scripts/README.md`

Keep both table rows (main added 2 rows, branch added 1: `antislop_mechanical_scan.sh`). Make sure that script file is
present after the merge and executable.

### 3.9 `concepts_brief.md` (modify/delete)

Main deleted it (commit de856d9, "clear the main worktree"; `design_draft.md` went too). Keep deleted. The branch edit (+55/-42) is
a working draft; if the owner wants it, save it as `docs/designs/concepts_brief_registration.md` from
`git show claude/clever-goldwasser-e086ce:concepts_brief.md`. `push_gate.sh` already ignores these two old drafts.
Do `git rm concepts_brief.md`.

### 3.10 Auto-merged files that need a hand check (no textual conflict)

| File | What to verify |
|---|---|
| `state/AncViewModel.kt` | Main: `internal constructor(application, controller)`, `saveMedicationQuick`, `StopMedication`/`UndoMedicationStop` dispatch, `committedAtMillis`, `clearsFrozenPatient` with the three delivery events and `StartDeliveryPlanEditor`. Branch: `registrationNext(...)`, `forceAgeRequired = current.forcesAgeRequired()`, `draftRecordBelongsToOpenEditor` check, and the removed dating-after-save block. Both sets must be present. The dating block must not return (`git grep intakeDatingUpdate profileSavedNotice` empty in main code). Consider adding `AncEvent.Flow` and `AncEvent.RegistrationNext` to `clearsFrozenPatient()` (see trap T5; reviewer decides). |
| `state/ReducerSupport.kt` `startsAnotherAction()` | Main-only list (used by `withMedicationStopUiKept` to drop the Undo bar and a failed stop message). Branch has no such list. Decide and test: a flow move (`AncEvent.Flow`, `RegistrationNext`) leaves the Meds screen, so add them to the list; otherwise a stale Undo bar can survive into the registration flow. |
| `state/DraftMapping.kt` | Net unchanged on main since the base (commit 79c9cc3 removed delivery from recovered profile drafts). Branch's version (8 dating keys removed, `draftRecordBelongsToOpenEditor`) merges cleanly. Verify a recovered profile draft keeps delivery fields from the freshly built draft (`v`), never from the record. |
| `state/DatingDraftRule.kt`, `DraftRecovery.kt`, `EditorHooks.kt` | `EditorHooks`: main changed `MedicationEditorHook.closeEditor` (no route change). Branch did not touch it. Confirm unchanged. `DatingDraftRule` still serves the separate Dating editor (keeps `DatingDraftField` values); only the patient-draft dating fields were removed. |
| `model/DemoModels.kt` | Both added enum entries to `PatientValidationError` (main: 5 `DELIVERY_*`; branch: `AGE_REQUIRED`, `PLACE_AND_DOCTOR_REQUIRED`) plus new input fields. Check every exhaustive `when` on `PatientValidationError` (`PatientEditorGroups.kt`, `DeliveryPlanSheet.kt`, `PreviousPregnancyEditor.kt`, `PatientEditor.kt`). |
| `ui/ClinicalBadges.kt` | Main removed `KeyFactTile` (no remaining use in the merged tree); branch added `isModerate` to `ClinicalRiskBadge`. Present together. |
| `ui/ClinicalPresets.kt` | Both edited; open the three-way diff and confirm both edits. |
| `ui/report/PatientPrintHtmlGenerator.kt` | Main: `deliveryOutcomeLine()` row and `model.parseDeliveryNotes`. Branch: signature name uses `PreviousPregnancyFormRules.DEFAULT_CLINICIAN_ID`. Both present; render a sample report and compare (this file matches the push gate's protected pattern). |
| `androidTest/.../AncWorkflowTest.kt`, `storage/EncryptedRecordsTest.kt` | Both sides edited. Main's edits are about the medication and delivery work; the branch's are about the registration editor. Recompile the androidTest source set and run both classes. |
| `AGENTS.md` | Auto-merged. The risk map block is generated by `scripts/project_map.py`; regenerate it after the merge, do not hand edit. |

## 4. Semantic traps (check each after the textual merge)

| # | Trap | Check |
|---|---|---|
| T1 | `PatientDraftValues` has four delivery fields and none of the eight dating fields | `git grep -nE "PatientDraftField\.(LMP_DATE\|DATING_METHOD\|TRANSFER_DATE\|EMBRYO_AGE_DAYS\|AGREED_EDD\|SCAN_DATE\|SCAN_GA)" app/src` empty; `PregnancyDraftField.(OUTCOME_TEXT\|BIRTH_METHOD\|CLINICIAN_ID)` empty. Dating values on `DemoPatientDating`, `SaveDatingCommand`, `DatingDraftValues` are NOT removed and must stay (many hits there are correct). |
| T2 | Delivery data survives a profile save through the flow editor | New test: a delivered patient (all four fields set) is edited through `StartPatientEditor`/flow identity and obstetric steps and saved; stored `delivered`, `deliveryDate`, `deliveryMode`, `deliveryOutcome` are unchanged and `prepare` raises no delivery error (it checks only when the delivery fields changed). |
| T3 | `intakeDatingUpdate` is gone and nothing needs it | The dating update at first save no longer exists on the branch (design decision in commits 01344b6 and e137af1). Remove the `DeliveryPlanReducerTest` "never asks for a dating update" test (the compiler now enforces it), or replace it with a test that saving the Plan sheet changes no dating row. Owner confirms (question 3). |
| T4 | `SavePatientProfileCommand.prepare` and fingerprint | `prepare(state, uuid, submittedAt, forceAgeRequired)` has both the branch's age rule and main's delivery check; the `fingerprint` includes the four delivery fields and still ignores nothing else. Run `PatientCommandsTest` and `DeliveryOutcomeStateTest`; add one test that two prepares with the same draft give the same fingerprint, and that a changed delivery field changes it. |
| T5 | Frozen command and Undo state | `clearsFrozenPatient()` (VM) and `startsAnotherAction()` (ReducerSupport) were each extended by only one side. Registration flow moves open new editors, so they should be in both lists (R-decision in 3.10). Add a test: a failed save, then a flow move, must not resend the old command. |
| T6 | Draft recovery | Cold start with an open new-patient registration draft: `draftRecordFor` writes no dating keys; `applyDraftRecord` restores name, age, gravida; pregnancy recovery only for the same pregnancy (`draftRecordBelongsToOpenEditor`). Also confirm a stored old draft record that still has `lmpDate`/`datingMethod` keys is ignored without a crash (fields map is read by key, so extra keys are ignored; test it). |
| T7 | `AncRoute` `when`s | `AncRoute.MedicationEditor` is removed on main; `PregnancyCountPrompt` is added on the branch. Every exhaustive `when (route)` compiles without `else`. `patientIdOrNull`, `requestBack`, `AncApp`, `PatientHubScreen` all handle the new route. |
| T8 | Sheets over tabs and Back | `systemBackKind(state)` returns `KEEP_EDITING` while the discard dialog shows, `FLOW_BACK` only if the open route is the flow step, else `BACK`. Check by test: Meds sheet open (no flow): Back = `AncEvent.Back` and main's `requestBack` sheet branch runs; Plan sheet the same; flow step open: Back = flow back; flow Identity step Back exits to the list. The `BackHandler` condition `route != PatientList \|\| showDiscardDialog` still lets a sheet on the Workspace route close with the system gesture. |
| T9 | Age required on create | Every main test or helper that creates a patient through `prepare` or the editor needs `ageText` (or the helper for a stored patient). Files seen: `DeliveryPlanViewModelTest`, `MedicationStopViewModelTest`, `NegativeControlAndClinicalValidationTest`, `DraftRecoveryTest`, `DeliveryOutcomeStateTest`, `DeliveryPlanReducerTest`, `AncWorkflowTest`, `DeliveryPlanSheetTest`. Fix the seed helpers (one shared seed helper if more than two copies exist), not the production rule. Also: pending create commands saved before the merge without age will fail `isStructurallyValid` on retry (risk R4). |
| T10 | Pencil target | After the merge, header pencil (`PatientHubScreen` line ~193), OverviewTab, ClinicalAttention dialog (`StartPatientEditor` then `SubmitPatientProfile` on an existing patient) and `PatientActionsModalSheet` all send `StartPatientEditor(existing id)`: old editor, no flow, and age is NOT forced (only `registrationFlow.current == IDENTITY` forces it). The ClinicalAttention dialog must keep using the old path; it must never start a flow. New patient button sends `StartPatientEditor(null)`: flow. Test both. |
| T11 | Strings | No duplicate key (done: none added by both); no removed key used. Lint "MissingTranslation"/unused checks clean. |
| T12 | Removed or moved files | `ui/MedicationEditor.kt` and `ui/DeliveryNotesParser.kt` (moved to `model/DeliveryNotes.kt`) were not touched by the branch. `git grep "ancnative.ui.parseDeliveryNotes\|ui.MedicationEditor"` empty. |
| T13 | Schema frozen | `git diff main..integration/registration-merge -- app/schemas app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDatabase.kt RecordsMigration.kt RecordEntities.kt` must be empty (the branch changes none of them; schema v7 stays). If anything shows, stop. |
| T14 | 360 dp and file size | `wc -l` of every changed UI file under 1,500 (`AncState.kt` should now be about 850 to 900, not 2,000). 360 dp pass on directory card (name, GA, BP, risk badges), hub header, flow header, footer. |
| T15 | One canonical component | The flow scaffold, `PatientEditorGroups`, `ClinicalRiskBadge` and the main sheets must not create parallel versions (`.agents/scripts/find_duplicates.py` exists on main; run it on the changed files). |

## 5. Ordered steps

Roles: **B** = builder lane (writes SEARCH/REPLACE blocks or edits, runs gradle); **R** = reviewer lane, a different
model family than B, reads only, never edits; **O** = owner. Use the lane assignment in the owner's lane regulation
memory (`lane-regulation-2026-10-30.md`); this plan does not name models. Level-3 rule: every edit under `app/src` needs a
frontier-tier review plus an independent test review (R and a second reviewer or test author that is not B).

| # | Step | Verification | Who |
|---|---|---|---|
| 1 | Run section 2 (audit, tags, dry-run evidence, baseline). Create the integration worktree. Commit the review docs dir there. | audit output saved; tags exist; `git status` clean in the new worktree | B, owner sees output |
| 2 | `git merge --no-ff --no-commit claude/clever-goldwasser-e086ce`. Resolve the 8 conflicts per 3.2 to 3.9 and 3.1 hunks 1 to 4; for hunk 5 keep main's side. | `git grep -nE '^(<<<<<<<\|=======\|>>>>>>>)' -- 'app/src' '.agents/scripts' '*.xml' '*.md'` empty (docs review dirs contain marker text on purpose; exclude `docs/designs/reviews`) | B |
| 3 | Port R1 to R9, then delete `IntakeDatingRule.kt` dependents (the file is already deleted by the merge) and the four deleted tests. | `./gradlew :app:compileDebugKotlin` | B |
| 4 | Commit as the labelled WIP merge commit. Message lists every conflict rule applied and says "may not build". | `git log --graph -3` shows two parents | B |
| 5 | Review the merge diff against both parents, with this plan open: `git diff main..HEAD`, `git diff claude/clever-goldwasser-e086ce..HEAD`, and `git diff --cc` for each conflicted file. Check 3.x and T1 to T15. Written verdict per file. | checklist signed per file | R (different family from B) |
| 6 | Compile all source sets, fix test seeds (T9), delete or rewrite the obsolete dating test (T3), add the new tests listed in T2, T4, T5, T6, T8, T10. A test author that is not B writes the tests for T2, T4, T8 and T10 from the spec above. | `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin` | B (tests: independent author) |
| 7 | Unit tests, full. Compare with the step 1 baseline: expected new tests = branch's 23 test files minus the 4 deleted plus new ones; no test removed except the 4 branch deletions and the T3 test. | `./gradlew :app:testDebugUnitTest` 0 failures; counts recorded | B |
| 8 | Lint and static scans. | `./gradlew :app:lintDebug` no new issue vs main (compare counts); `.agents/scripts/find_duplicates.py`, `find_unused.py` on changed files; T13 and T14 commands | B |
| 9 | Targeted emulator classes (use `.agents/scripts/run_emulator_test_class.sh` and `set_emulator_360dp.sh`): `RegistrationFlowUiTest`, `DraftPersistenceIntegrationTest`, `AncWorkflowTest`, `EncryptedRecordsTest`, `PatientHubDesignTest`, `DeliveryPlanSheetTest`, `DeliveryPlanViewModelTest`, `MedicationStopViewModelTest`, `MedsTabSheetTest`, `MedicationFormTest`, `PatientListScreen*` classes. | all pass; log saved | B |
| 10 | Second review: the whole result for T1 to T15 and the fix-up commits; independent test review of the new tests (do they fail when the rule is broken? mutate one rule per test). | written sign-off | R and test reviewer |
| 11 | Full emulator suite (`.agents/scripts/run_emulator_all_tests.sh`, about 9 minutes). Fresh run, not a reused result, because every source set changed. | `failures=0`; total is main's 456 plus the branch's new instrumented tests minus deleted | B |
| 12 | Update `docs/BRANCHES.md` (remove the `clever-goldwasser` line, no new line needed), `.agents/pipeline/HANDOFF_AGY.md` (mission table, artifact index, log), regenerate the project risk map. Commit. | `.agents/scripts/git_audit.sh` prints only expected lines | B |
| 13 | Owner phone QA (section 7), on a debug QA build, not the clinic app. | checklist all passed | O |
| 14 | Owner says "push". Then, in the main tree: `git merge --ff-only integration/registration-merge`, `STAGE_BASE=bace905 .agents/scripts/push_gate.sh pre` (the protected-file check then lists only `PatientPrintHtmlGenerator.kt`; owner accepts that line), `git push origin main` (normal push, never force), `.agents/scripts/push_gate.sh post`. | gate passes; remote equals local HEAD | O approves; B runs |
| 15 | Clean up: remove the worktree and `integration/registration-merge`; keep both tags and the original branch until the owner says to delete them. | audit clean | B |

Each round that passes is committed locally on the integration branch (standing rule: commit each verified round, never push).

## 6. Stop conditions

Stop, do not push, and tell the owner when any of these happens. Fall back = leave main and the branch untouched, keep the
integration branch for study, and choose again.

1. Step 3 compile needs a change outside the files in section 3, `RegistrationFlow*.kt`, `ClinicalDraftReducers.kt`,
   `AncReducer.kt`, `AncEvent.kt`, `ReducerSupport.kt` and test seeds. That means the split is deeper than assessed.
2. T13 shows any schema, entity, DAO or migration difference. Schema v7 is frozen.
3. A previously green main test fails and the cause is not one of T3, T9 or the four deleted tests. Two fix attempts without
   a clear cause: stop.
4. Lint adds new errors in level-3 files, or a file passes 1,500 lines.
5. The reviewer lane refuses sign-off (steps 5 or 10), or the independent test review finds a test that cannot fail.
6. The full suite has a failure that also fails on a clean main checkout run once more: record as pre-existing, do not block;
   any other failure blocks. One re-run is allowed to separate a flaky test; the re-run result is recorded.
7. Phone QA shows lost data, a crash, a changed delivery outcome, or a draft that does not come back.
8. More than two review rounds without convergence: abandon the merge and write the port as a fresh branch from main (cherry-pick
   the self-contained branch commits `RegistrationFlow.kt`, `RegistrationFlowDriver.kt`, UI files, model rules and tests, port the
   three reducer edits by hand). The branch stays tagged.

## 7. Phone QA checklist (owner, QA debug copy only)

Use the QA build with demo data. Never the clinic app with real patients.

1. New patient: tap New patient. The Identity step opens (name, age, phone, and so on). Leave age empty and tap Next: an age error shows.
   Enter name, age, gravida, Next. The obstetric step follows; finish through the remaining steps (skip works on every step except Identity).
2. Back gesture on the Identity step returns to the directory (discard question if you typed). Back on a later step returns to the previous step.
3. Kill the app on the Identity step with text typed; reopen. The draft comes back and the typed text is restored. Repeat on the pregnancy
   step (one pregnancy, one outcome, one delivery mode).
4. Directory card: new patient shows the amber Incomplete chip until all steps are done; severe BP and high or moderate risk badges show; 360 dp layout has no cut text.
5. Header pencil on an existing patient: it opens the OLD editor today (see question 1). Save a change; nothing in the delivery outcome changes.
   The Clinical attention edit still saves.
6. Delivery outcome: open a patient, Plan tab, delivery sheet, set Delivered, date, mode, outcome, save. Headline shows "Delivered ...". Open the
   patient's profile through the pencil, change the phone, save: delivery fields are unchanged. Print the report: Delivery Outcome row is present.
7. Dashboard: Delivered filter shows only delivered patients; the main list hides them; searching by name still finds a delivered patient; counts match.
8. Meds tab: add a medication through the sheet (Save and add another); stop one with one tap and Undo it; fail case: airplane mode is not
   relevant (local vault), so check that Undo bar goes away when you switch to another patient or open the registration flow.
9. Back from the Meds sheet and from the Plan sheet closes the sheet and stays on the tab; with edits it asks before discarding.
10. Lock and unlock the vault with a flow open and with a sheet open: no flow or Undo state remains after unlock.
11. Dating editor (separate screen) still opens from its section and saves; the profile editor no longer shows dating fields.
12. 360 dp (use `set_emulator_360dp.sh` or the phone at narrow width): name, GA, BP and High risk on the hub header, directory card, flow header and footer buttons fit without truncation.

## 8. Risks, ranked

| Rank | Risk | Mitigation |
|---|---|---|
| 1 | Wrong resolution of `AncState.kt` hunk 5 (duplicate events and reducer, or lost branch reducer edits R1 to R9) | Section 3.1 port list; reviewer diffs against both parents; compile and the registration reducer tests (`RegistrationFlowStateTest`, `RegistrationFlowDriverTest`) |
| 2 | Delivery outcome lost or overwritten through the registration editor or recovered drafts (clinical data loss) | T2, T4, T6 tests; phone QA 6 |
| 3 | A shared reducer list or chain extended by one side only (`startsAnotherAction`, `clearsFrozenPatient`, `reduce` post-processing, `clearedForLock`) | 3.1 hunk 2, 3.10, T5; mutation check in step 10 |
| 4 | Age rule rejects main's test seeds and any stored create command that has no age | T9; owner decides whether a stored pre-merge create command can exist (question 4) |
| 5 | Pencil still opens the old editor, so the stated goal is not met by this merge | Question 1; separate small change after the merge, with its own plan and review |
| 6 | Back handling with sheets and the flow together (`systemBackKind` knows the flow, not the sheets) | T8 tests and phone QA 2, 9 |
| 7 | Compile-clean but wrong exhaustive `when` or `else` added to silence errors | Reviewer rejects `else` branches added in this merge |
| 8 | Push gate protected-file check trips on `PatientPrintHtmlGenerator.kt` | Step 14 `STAGE_BASE`; owner accepts the line |
| 9 | Tests that depend on removed strings or tags (`hubRiskBadge`, emoji label, `PATIENT_DATING_NOT_UPDATED`) | 3.4, 3.6, 3.7; androidTest compile and targeted classes |
| 10 | A long uncommitted merge worktree | WIP merge commit at step 4; nothing stays only in the tree |

Not pasted and still to read during step 5 (all in the repo): `ui/ClinicalPresets.kt` (three-way diff),
`ui/patientdirectory/PatientDirectoryCard.kt` (Incomplete chip and main's delivered badge together),
`state/RegistrationFlowDriver.kt` `startEditorFor`/`editorEventFor` against main's `startDeliveryPlanEditor`, and the full
merged `strings.xml` branch hunk (the brief shows it cut off).

## 9. Corrections to the draft plan

1. Strategy: the draft's rebase command replays the wrong side, and it needs `git rebase -i` to squash the WIP commit (not available).
   It also proposed `cp -r`/`rsync` of the worktree into `.claude/worktrees/` (stray files). Replaced by merge on an integration worktree plus tags.
2. `AncState.kt` hunk 5: the draft said "branch wins" and keep main's extension. That duplicates `AncEvent` and `AncReducer`,
   because main moved them to `AncEvent.kt`/`AncReducer.kt`. Correct: main's side, then port the branch's reducer edits (R1 to R9). The draft
   also guessed at files that do not exist (`model/PatientDraftValues.kt`, `model/DatingRules.kt`, `storage/CommandFingerprint.kt`,
   `storage/PatientDao.kt`, `storage/AppDatabase.kt`, `ui/components/ClinicalRiskBadge.kt`); `PatientDraftValues` lives in `state/AncState.kt`,
   `intakeDatingUpdate` was in `state/IntakeDatingRule.kt` and is deleted by the branch.
3. `PatientDraftValues`: the draft said keep dating fields "if still used". None is; delete all eight and keep four delivery fields.
4. `PatientHubHeader`: the draft dropped main's `hubRiskBadge` test tag; keep both.
5. `AncDatePicker`: the draft kept the emoji; the branch removed it on purpose; keep main's touch target only.
6. Missed: the driver does not compile against main (R9); age-required breaks main's test seeds (T9); `DeliveryPlanReducerTest` uses the deleted
   rule (T3); pencil goal is not delivered by the branch (question 1); `clearedForLock` needs all four fields; `push_gate.sh` and `STAGE_BASE`;
   origin/main is one commit behind local main; `concepts_brief.md` was deleted by main on purpose.
7. Step order in the draft ran a full emulator suite before review and a "squash WIP" after QA; here review comes before the long run, and the WIP commit stays as history.

## 10. Questions for the owner (max 5)

1. Pencil: the branch opens the flow only for a new patient; the header pencil, the profile card and the directory sheet still open the old
   editor, and nothing starts the flow for an existing "Incomplete" patient. Should this merge ship as is (flow for New patient only) and the
   pencil/Incomplete-chip wiring be a separate small change afterwards?
   Recommended: yes. Ship the merge first; then one follow-up: tapping an Incomplete chip or the pencil on an incomplete patient sends
   `AncEvent.Flow(FlowEvent.Begin(patientId))`; the pencil on a complete patient keeps the plain editor; the Clinical attention dialog stays on the old path.
2. Is it right that the registration flow's Identity step forces age, and the old editor needs age only for new patients and for patients that already have one?
   Recommended: yes, as the branch has it (decision already reviewed on the branch).
3. The dating update at first save is gone (the branch removed it) and main's test "the sheet never asks for a dating update" becomes meaningless.
   Delete that test, or replace it with "saving the Plan sheet changes no dating row"? Recommended: replace it (one small test), so the behaviour stays guarded.
4. Can there be saved but not yet committed new-patient create commands on any device (the journal) from before the merge that carry no age?
   They would be rejected as invalid on retry. Recommended: confirm there are none on the clinic phone (it still runs an older release); if any, open and save them before updating.
5. Push order: main is already one commit ahead of `origin/main`. Push that doc commit on its own now, or let it go out with the merge?
   Recommended: let it go out with the merge, after the full suite and phone QA, as one push.
