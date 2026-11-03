# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 4a. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R4.md (section "Round 4a": oracles Q200-Q208, S110-S114) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. NEVER write dead code, extra XML/files or comments only to satisfy a static check or a test; if a check looks wrong, implement the spec and say so in your final message. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Product rule for this round: the stored isProphylaxis field/column stays (data kept); only UI, print badge and summary prefix go. Owner wording: plain English, no "Codex" in visible text.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: CORE BUILDER for ROUND 4a. Implement Q200, Q201, Q202, Q203 and the model/summary part of Q207. ALLOWED files: app/src/main/java/com/sherifradwan/ancnative/model/ClinicalModels.kt, app/src/main/java/com/sherifradwan/ancnative/model/ClinicalSummaryGenerator.kt, app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt, app/src/main/java/com/sherifradwan/ancnative/state/AncViewModel.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalPresets.kt.
Add the two functions and the constant once, in ClinicalModels.kt. Remove MedicationProphylaxisChanged everywhere it is handled in your files (the UI lane removes its callers; until then the debug compile may fail only in MedicationEditor.kt: say so). Keep isProphylaxis in the data classes and drafts. Smallest edits; no tests or checks. Run ./gradlew --offline :app:compileDebugKotlin (failures only in UI files are expected), the new unit tests when they exist, python3 .agents/checks/check_r4a.py (UI-dependent S may fail). Report real results.


ALLOWED (machine-enforced): app/src/main/java/com/sherifradwan/ancnative/model/ClinicalModels.kt, app/src/main/java/com/sherifradwan/ancnative/model/ClinicalSummaryGenerator.kt, app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt, app/src/main/java/com/sherifradwan/ancnative/state/AncViewModel.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalPresets.kt

REPAIR ROUND: these selected tests still fail. Fix the production code only.
ATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE

> Task :app:compileDebugKotlin FAILED
20 actionable tasks: 1 executed, 19 up-to-date
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:121:46 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:190:50 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:358:74 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:380:66 Unresolved reference 'MedicationProphylaxisChanged'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 3s

ATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE

> Task :app:compileDebugKotlin FAILED
20 actionable tasks: 1 executed, 19 up-to-date
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:121:46 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:190:50 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:358:74 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:380:66 Unresolved reference 'MedicationProphylaxisChanged'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 3s

ATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE

> Task :app:compileDebugKotlin FAILED
20 actionable tasks: 1 executed, 19 up-to-date
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:121:46 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:190:50 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:358:74 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:380:66 Unresolved reference 'MedicationProphylaxisChanged'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 2s

-DATE
> Task :app:processDebugManifestForPackage UP-TO-DATE
> Task :app:processDebugResources UP-TO-DATE
> Task :app:kspDebugKotlin UP-TO-DATE

> Task :app:compileDebugKotlin FAILED
18 actionable tasks: 1 executed, 17 up-to-date
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:121:46 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:190:50 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:358:74 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:380:66 Unresolved reference 'MedicationProphylaxisChanged'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 2s

ATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE

> Task :app:compileDebugKotlin FAILED
20 actionable tasks: 1 executed, 19 up-to-date
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:121:46 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:190:50 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:358:74 Unresolved reference 'MedicationProphylaxisChanged'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt:380:66 Unresolved reference 'MedicationProphylaxisChanged'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 2s
