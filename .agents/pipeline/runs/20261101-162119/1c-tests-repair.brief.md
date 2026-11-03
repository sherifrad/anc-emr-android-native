# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 4g. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R4.md (section "Round 4g": oracles Q270-Q275, S170-S175) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles still hold. If the spec is ambiguous choose the SAFE behaviour (never overwrite the doctor's text, never invent a clinical finding) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files, gradle properties, or any test file you do not own.
4. NEVER write dead code, extra XML/files or comments only to satisfy a static check or a test; if a check looks wrong, implement the spec and say so. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside scope. Do not change any clinical threshold, wording or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: TEST DESIGNER for ROUND 4g (SPEC_FIX_R4.md section "Round 4g"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r4g.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/EddFromScanTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/state/PatientDatingDraftTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt (UPDATE).
1. check_r4g.py and the TEST_MANIFEST.json entries ALREADY EXIST (written by the orchestrator): read them, do NOT rewrite; only fix a clear bug in them and say so. Each S must FAIL on today's code.
2. Write the unit tests per the "Unit" paragraph with its exact numbers. IntakeDatingRuleTest.kt: move to the new signature `intakeDatingUpdate(existing, PatientDraftValues(...))` and the new IntakeDating fields, KEEPING every existing case and its expected outcome (LMP and existing-dating guards); the LMP expectations now include the note and method. Call the real functions (internal is usable from tests in the same module); never copy production logic; fix asOf dates, never LocalDate.now(). For the draft save/restore test look at DraftMappingTest.kt for how drafts are built, saved and restored.
Run check_r4g.py and report real results (unit tests will not compile until the core lane has run: say so).


ALLOWED (machine-enforced): .agents/checks/check_r4g.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/EddFromScanTest.kt, app/src/test/java/com/sherifradwan/ancnative/state/PatientDatingDraftTest.kt, app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt

REPAIR ROUND: your test sources do not compile (new-API tests were parked, so this error is elsewhere). Fix compile errors in YOUR test files only; keep every assertion unchanged.
ntakeDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 643ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 679ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 642ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 610ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 577ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 612ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 555ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 597ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 553ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 535ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 571ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 531ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 534ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 556ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 504ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 511ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 545ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 525ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 512ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 526ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 547ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 573ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 511ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 531ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 490ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 505ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 516ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 532ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 529ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 631ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 634ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 562ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 518ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 518ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 585ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 502ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 493ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 609ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 543ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 625ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 507ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 571ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

BUILD FAILED in 540ms

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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

FAIL S170 one scan due-date function: eddFromScan not defined exactly once in IsoDates.kt
FAIL S171 new-patient draft carries the dating choice fields: PatientDraftValues lacks datingMethod; PatientDraftValues lacks transferDate; PatientDraftValues lacks embryoAgeDays; PatientDraftValues lacks agreedEdd; PatientDraftValues lacks scanDate; PatientDraftValues lacks scanGaWeeks
FAIL S172 intake dating goes through the engine rule: intakeDatingUpdate does not take PatientDraftValues; does not use dueDateForMethod; does not use eddFromScan; still calls eddFromLmp(; intakeDatingPreview not defined once in IntakeDatingRule.kt
FAIL S173 post-save dating write uses the rule result: hard-coded LMP method; passes only the LMP to the rule
FAIL S174 new fields saved and restored with the patient draft: not saved and restored: scanDate, scanGaWeeks, scanGaDays
FAIL S175 form calls the preview; LMP-only engine function gone: PatientEditor.kt does not call intakeDatingPreview; PatientEditor.kt still calls eddFromLmp(; PatientEditor.kt still calls calculateForLmp(; calculateForLmp still exists in main sources

keDatingRuleTest.kt:170:38 Unresolved reference 'dueDate'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:172:34 Unresolved reference 'gestationalAge'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:176:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:177:20 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:36 Unresolved reference 'intakeDatingPreview'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt:181:78 Unresolved reference 'dueDate'.

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
