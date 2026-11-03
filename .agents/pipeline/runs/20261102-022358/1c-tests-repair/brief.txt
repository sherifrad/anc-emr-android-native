# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 6a (a LOGIC round on clinical records: risk level 3). An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_LOGIC_R6.md (items D600-D613, S300-S303) and .agents/pipeline/OWNER_DECISIONS.md. If ambiguous choose the SAFE behaviour (never delete or overwrite a clinical record unless the spec says so; fail closed with the existing failure codes) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files or gradle properties.
4. NEVER write dead code, extra files, or comments only to satisfy a check or a test. REUSE PRINCIPLE: one function per rule; call the existing helper (SaveMedicationCommand patterns, incrementPatientRevision, clinicalCommandReceipt replay, controller.execute), never write a second copy. Simplest thing that meets the spec; no new libraries. No Room schema or version change. No UI files.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md.
6. Final message (plain text, <= 25 lines): files changed, what you did per D item, assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: TEST DESIGNER for ROUND 6a (SPEC_LOGIC_R6.md). Tests only; no production code.
ALLOWED files: .agents/checks/check_r6a.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalCommandsTest.kt (UPDATE: add, keep every existing case), app/src/test/java/com/sherifradwan/ancnative/state/AncDeleteUndoStateTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt (NEW).
1. check_r6a.py and the TEST_MANIFEST.json entry ALREADY EXIST (written by the orchestrator): do NOT rewrite; only fix a clear bug and say so. Each S must FAIL on today's code.
2. Write the unit tests listed in the spec's "Unit tests" paragraph with exact assertions: command validation (revision 0, blank id, bad hash), fingerprints bound to every field and to the source delete, content hash ignores revision but covers every field (medication and lab), state rules (archived, dirty draft, stale token, rebasing of the blank chained draft, submit ignored while undo RUNNING, lock reset, newer offer replaces older), and `ClinicalDeleteRule.evaluateRestore(receipt, latestReceipt, existingRow, cmd): ClinicalCommandFailure?` (wrong kind, fingerprint mismatch, not latest, row present, hash mismatch, success). Use the real functions and the names in the spec; build state through the real reducer. Never copy production logic into a test.
Run check_r6a.py and report real results (unit tests will not compile until the core lane has run: say so).


ALLOWED (machine-enforced): .agents/checks/check_r6a.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalCommandsTest.kt, app/src/test/java/com/sherifradwan/ancnative/state/AncDeleteUndoStateTest.kt, app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt

REPAIR ROUND: your test sources do not compile (new-API tests were parked, so this error is elsewhere). Fix compile errors in YOUR test files only; keep every assertion unchanged.
ence 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 4s

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 614ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 651ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 583ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 536ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 567ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 528ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 630ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 523ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 539ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 522ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 509ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 537ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 492ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 506ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 484ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 515ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 482ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 481ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 509ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 497ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 488ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 479ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 480ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 473ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 498ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 467ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 478ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 494ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 508ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 474ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 461ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 467ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 487ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 456ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 459ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 482ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 467ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 469ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 453ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 509ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 473ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 451ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 456ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 457ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 476ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 462ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 468ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 485ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 444ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 448ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 481ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 441ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 468ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 454ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 496ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 457ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 454ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 458ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 443ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 455ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 454ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 479ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 442ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 439ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 446ms

e 'RestoreLabInvestigationCommand'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:142:45 Unresolved reference 'LAB_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:148:20 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:149:70 Unresolved reference 'MEDICATION_DELETE'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:152:13 Unresolved reference 'ClinicalDeleteRule'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRuleTest.kt:156:13 Unresolved reference 'ClinicalDeleteRule'.

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

BUILD FAILED in 462ms

FAIL S300 delete and restore commands, kinds and hashes defined: missing DeleteMedicationCommand, RestoreMedicationCommand, DeleteLabInvestigationCommand, RestoreLabInvestigationCommand, MEDICATION_DELETE, MEDICATION_RESTORE, LAB_DELETE, LAB_RESTORE, medicationContentHash, labContentHash
FAIL S301 DAO delete and latest-receipt queries present: missing deleteMedication, deleteLabInvestigation, latestClinicalReceiptForRecord
FAIL S302 delete/undo events, state fields and 8 s timer present: missing RequestDeleteMedication, RequestDeleteLab, ConfirmDelete, UndoRequested, UndoExpired, undoOffer, deleteConfirmation, 8_000
PASS S303 no ui, schema or migration file changed
