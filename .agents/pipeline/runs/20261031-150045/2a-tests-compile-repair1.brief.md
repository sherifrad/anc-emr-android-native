# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3s. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3s": oracles Q102-Q103, S84-S85) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. NEVER write dead code, extra XML/files or comments only to satisfy a static check or a test; if a check looks wrong, implement the spec and say so in your final message. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: TEST DESIGNER for ROUND 3s (SPEC_FIX_R3.md section "Round 3s"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3s.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/ObstetricHistoryTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt (NEW), and ONLY if an existing test pins the old print wording ("Prev N CS + M SVD" or "Primigravida / N/A"): that single expectation in app/src/test/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGeneratorTest.kt.
1. check_r3s.py: S84-S85 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL; exit 1 on FAIL; style of .agents/checks/check_r3q.py; each S must FAIL on today's code.
2. The unit tests per the "Unit" paragraph with exact strings and numbers. Build DemoPreviousPregnancy/DemoPatient values like PatientPrintHtmlGeneratorTest does (DemoPreviousPregnancy has many required fields: copy a builder shape). The cross-screen test calls the real projectDashboardRow (ui/PatientListScreen.kt) and the real print generator.
3. TEST_MANIFEST.json: add STATIC-r3s, UNIT-obstetric-history, UNIT-print-obstetric (cmd, deps, covers).
Run check_r3s.py and report real results; list any old test expectation you had to change.


ALLOWED (machine-enforced): .agents/checks/check_r3s.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/ObstetricHistoryTest.kt, app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt, app/src/test/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGeneratorTest.kt

REPAIR ROUND: the unit-test source set does not compile because of YOUR test files. Fix only that; keep every assertion unchanged.
 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:107:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:130:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:151:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:173:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:194:43 Unresolved reference 'prevSummary'.

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

BUILD FAILED in 773ms

 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:107:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:130:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:151:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:173:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:194:43 Unresolved reference 'prevSummary'.

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

BUILD FAILED in 765ms

 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:107:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:130:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:151:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:173:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:194:43 Unresolved reference 'prevSummary'.

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

BUILD FAILED in 593ms

 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:107:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:130:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:151:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:173:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:194:43 Unresolved reference 'prevSummary'.

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

BUILD FAILED in 626ms

 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:107:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:130:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:151:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:173:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:194:43 Unresolved reference 'prevSummary'.

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

 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:107:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:130:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:151:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:173:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:194:43 Unresolved reference 'prevSummary'.

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

BUILD FAILED in 700ms

 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:107:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:130:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:151:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:173:43 Unresolved reference 'prevSummary'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt:194:43 Unresolved reference 'prevSummary'.

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

BUILD FAILED in 586ms
