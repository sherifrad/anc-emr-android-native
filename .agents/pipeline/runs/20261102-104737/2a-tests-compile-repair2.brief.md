# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND r6b2 (a LOGIC round on clinical records: risk level 3). An orchestrator planned the work and will verify it independently. Scope: STATE AND UNDO COORDINATOR (SPEC_LOGIC_R6B.md section 6, R6b-2; round r6b1 storage generalisation is already merged in the working tree). Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_LOGIC_R6B.md (the items listed below) and .agents/pipeline/OWNER_DECISIONS.md, plus the round 6a spec .agents/pipeline/SPEC_LOGIC_R6.md for the frozen behaviour. If ambiguous choose the SAFE behaviour (never delete or overwrite a clinical record unless the spec says so; fail closed with the existing failure codes) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files or gradle properties.
4. NEVER write dead code, extra files, or comments only to satisfy a check or a test. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec; no new libraries. No Room schema or version change (app/schemas, RecordsDatabase, RecordsDao, entities stay untouched).
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md.
6. Final message (plain text, <= 25 lines): files changed, what you did per D item, assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, instrumented tests under app/src/androidTest (emulator only). Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---
SPEC EXCERPT (items D625-D627, D651 removal, D653 part 2; read the full text in SPEC_LOGIC_R6B.md and the frozen rules D608-D613 in SPEC_LOGIC_R6.md):
- D625 State. PendingDeletion(patientId, type, recordId); UndoOffer(token, patientId, type, action: DELETE|SAVE, label, phase) (no clinical payload beyond the label). DeletionKind and the kind part of UndoKind (DELETE_MED, DELETE_LAB, SAVE_MED) are replaced by DeletableType. Events collapse to RequestDelete(patientId, type, recordId) (replaces RequestDeleteMedication and RequestDeleteLab); CancelDelete, ConfirmDelete, DeleteCommitted, DeleteFailed, UndoRequested, UndoFailed, UndoExpired are unchanged. Editor-specific reducer code moves behind EditorHook in the state package (new state/EditorHooks.kt): isOpenFor(state, recordId), closeEditor(state, patientId): AncState, rebaseCreateDraft(state, patients): AncState. The medication and lab hooks wrap today's medicationDraft/labDraft code; the reducer reaches them through the registry (adapter.editorHook), so no if (kind == ...) remains. SavedNotice aliases are replaced by adapter.deletedNotice etc.
- D626 UndoCoordinator (new state/UndoCoordinator.kt, plain class, NO Android types) owns everything the ViewModel does now for Undo: pending token/command, the single 8000 ms timer (injectable CoroutineScope so tests use virtual time), restart-once (MAX_UNDO_RESTARTS), ambiguous replay of the exact command (same operationUuid), STALE_PATIENT_REVISION retry after a fresh snapshot, stale-token no-op, newer offer replaces and finalises the older, lock/epoch clear and sessionStillCurrent check. Public surface: beginDelete(type, patientId, recordId), offerSaveUndo(type, patientId, recordId, receipt, savedRecord), request(token), clear(). It talks to the outside through three lambdas only: execute(cmd, epoch), dispatch(AncEvent), currentState(). The ViewModel keeps one field and four delegating lines (confirmDelete, requestUndo, the post-save hook, clearFrozenCommands). confirmDelete's two branches become one: find the record via adapter.record(...), build DeleteRecordCommand and the matching RestoreRecordCommand, run, arm the offer. withPatientRevision comes from RecordUndoCommand (no when over four classes).
- D627 Behaviour is frozen bit for bit: dirty/archived/saving ignored, ConfirmDelete closes a clean editor on the same record, undo rebase of the blank chained create draft, SubmitMedication* ignored while undo RUNNING, session epoch clear, 8 s then gone, exactly one restart, newer offer finalises the older. Only the mechanism moves. No Room entity, DAO, schema or DB version change.
- D651 (end of r6b2): remove the factory functions/typealiases DeleteMedicationCommand, DeleteLabInvestigationCommand, RestoreMedicationCommand, RestoreLabInvestigationCommand from storage/ClinicalCommands.kt; the ViewModel and tests use DeleteRecordCommand/RestoreRecordCommand.
- D653 static check .agents/checks/check_r6b2.py (S410-S416): none of the banned identifiers left in app/src/main; no DeletableType branch in AncViewModel/AncState/UndoCoordinator; one deleteRecord and one restoreRecord in the repository; app/schemas and database version unchanged; the 8000 ms literal appears once.
- UI: if (and only if) ui/ references a removed event or class, change just ui/MedicationEditor.kt, ui/LabEditor.kt, ui/WorkspaceScreen.kt for the rename only; otherwise leave ui/ alone. Exit criteria: all unit tests green, ClinicalDeleteRestoreDbTest (46 cases) green on the emulator.
- D654 Isolation (SPEC_LOGIC_R6B.md): the coordinator/ViewModel must only ever hand the confirmed (type, patientId, recordId) delete command and its matching restore to execute; never another kind's or patient's record.

ROLE: TEST DESIGNER for ROUND r6b2 (SPEC_LOGIC_R6B.md D625-D627, D652). Tests only; no production code. The author of these tests must not be the author of the core lane.
ALLOWED files: .agents/checks/check_r6b2.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/state/AncDeleteUndoStateTest.kt (UPDATE: assertions stay), app/src/test/java/com/sherifradwan/ancnative/state/UndoCoordinatorTest.kt (NEW).
1. check_r6b2.py and the STATIC-r6b2 TEST_MANIFEST.json entry ALREADY EXIST (written by the orchestrator): do NOT rewrite; only fix a clear bug and say so. Each S must FAIL on today's code.
2. AncDeleteUndoStateTest: RequestDeleteMedication/RequestDeleteLab become RequestDelete(patientId, type, recordId); the existing cases (archived, dirty draft, stale token, rebase of the blank chained draft, submit ignored while undo RUNNING, lock reset, newer offer replaces older) run once per type (MEDICATION, LAB) through the real reducer and the real editor hooks. Keep every assertion's meaning; use UndoOffer/PendingDeletion with type, never the old UndoKind.
3. UndoCoordinatorTest with virtual time (kotlinx-coroutines-test or the project's existing test dispatcher; injectable CoroutineScope): offer expires after exactly 8000 ms and not before (7999 ms still open); first failure restarts the window once, second failure drops the offer (MAX_UNDO_RESTARTS); ambiguous outcome replays the SAME command with the SAME operationUuid; STALE_PATIENT_REVISION retries once after a fresh snapshot with withPatientRevision and never with another record; a stale token request is a no-op (no execute call); a newer offer replaces and finalises the older (older token no longer acts); clear() (lock/epoch change) cancels the timer, drops the pending command and the offer and nothing runs afterwards; request() after the session epoch changed does not call execute; no clinical payload is dispatched except the label; delete of record A can never cause a command for record B or another patient. Drive it only through its public surface with fake lambdas execute/dispatch/currentState; never reimplement its logic in the test.
Use the real functions and the names in the spec. Run check_r6b2.py and report real results (unit tests will not compile until the core lane has run: say so).
SPEC D654 applies here too: add to UndoCoordinatorTest a case per type proving that after delete of record A (patient P1) with a same-looking record B (same id string of the other kind, and P2's record) present, every command passed to execute carries exactly A's type, patientId, recordId and the matching restore command, and that nothing is ever executed for B/P2 (including after replacement, retry, restart and clear()).


ALLOWED (machine-enforced): .agents/checks/check_r6b2.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/state/AncDeleteUndoStateTest.kt, app/src/test/java/com/sherifradwan/ancnative/state/UndoCoordinatorTest.kt

REPAIR ROUND: the unit-test source set does not compile because of YOUR test files. Fix only that; keep every assertion unchanged.
 Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
> Task :app:processDebugJavaRes UP-TO-DATE

> Task :app:compileDebugUnitTestKotlin FAILED
26 actionable tasks: 1 executed, 25 up-to-date
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:262:25 Unresolved reference 'DeleteMedicationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:269:26 Unresolved reference 'RestoreMedicationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:277:25 Unresolved reference 'DeleteLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:283:26 Unresolved reference 'RestoreLabInvestigationCommand'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugUnitTestKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 1s

 Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
> Task :app:processDebugJavaRes UP-TO-DATE

> Task :app:compileDebugUnitTestKotlin FAILED
26 actionable tasks: 1 executed, 25 up-to-date
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:262:25 Unresolved reference 'DeleteMedicationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:269:26 Unresolved reference 'RestoreMedicationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:277:25 Unresolved reference 'DeleteLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:283:26 Unresolved reference 'RestoreLabInvestigationCommand'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugUnitTestKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 1s

sk :app:javaPreCompileDebugUnitTest UP-TO-DATE
> Task :app:processDebugJavaRes UP-TO-DATE

> Task :app:compileDebugUnitTestKotlin FAILED
26 actionable tasks: 1 executed, 25 up-to-date
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:262:25 Unresolved reference 'DeleteMedicationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:269:26 Unresolved reference 'RestoreMedicationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:277:25 Unresolved reference 'DeleteLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt:283:26 Unresolved reference 'RestoreLabInvestigationCommand'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugUnitTestKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 906ms
