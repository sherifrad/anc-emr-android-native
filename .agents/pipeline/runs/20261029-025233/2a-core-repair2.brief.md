# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX.md (and ../maca-run-2026-10-29/SPEC.md for background). The spec's oracles P1-P10 are the contract. If the spec is ambiguous choose the SAFE behaviour (never hide, drop or relabel clinical/administrative data) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files, gradle properties, or any test file you do not own.
4. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec.
5. Do not read other agents' reviews or reports under .agents/maca-run-2026-10-29/ other than SPEC.md, .agents/fleet-runs/, or .agents/backup-*. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (P-number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, package com.sherifradwan.ancnative.ui. Unit tests command: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: CORE LOGIC BUILDER. Make the tests pass without editing them (tests are owned by another agent and are the contract).
ALLOWED files:
- app/src/main/java/com/sherifradwan/ancnative/ui/DeliveryNotesParser.kt   (NEW: move DeliveryNoteSections and parseDeliveryNotes here, fix per P1-P5)
- app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalFormat.kt   (NEW: isRhNegative, isHighRisk, formatAgeYears per P6-P8; pure Kotlin, no Compose)
- app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt   (ONLY: delete the old DeliveryNoteSections/parseDeliveryNotes definitions, and replace the inline Rh/risk logic in DeliveryPlanningCard with isRhNegative(...)/isHighRisk(...) calls. No UI/layout/text changes.)
Read the tests under app/src/test/java/com/sherifradwan/ancnative/ui/ first. Use Regex with word boundaries. Keep the parser a pure function.
Run: ./gradlew --offline :app:testDebugUnitTest --tests '*ParseDeliveryNotesSpecTest' --tests '*ClinicalFormatTest' and report the real result.


ALLOWED (machine-enforced): app/src/main/java/com/sherifradwan/ancnative/ui/DeliveryNotesParser.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalFormat.kt, app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt

REPAIR ROUND: these selected tests still fail. Fix the production code only.
TE
> Task :app:processDebugResources UP-TO-DATE
> Task :app:kspDebugKotlin UP-TO-DATE
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:bundleDebugClassesToRuntimeJar UP-TO-DATE
> Task :app:bundleDebugClassesToCompileJar UP-TO-DATE
> Task :app:kspDebugUnitTestKotlin UP-TO-DATE
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
> Task :app:processDebugJavaRes UP-TO-DATE

> Task :app:compileDebugUnitTestKotlin FAILED
24 actionable tasks: 1 executed, 23 up-to-date
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt:115:32 Unresolved reference 'ushr'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt:116:80 Operator '==' cannot be applied to 'BigInteger' and 'Int'.

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

TE
> Task :app:processDebugResources UP-TO-DATE
> Task :app:kspDebugKotlin UP-TO-DATE
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:bundleDebugClassesToRuntimeJar UP-TO-DATE
> Task :app:bundleDebugClassesToCompileJar UP-TO-DATE
> Task :app:kspDebugUnitTestKotlin UP-TO-DATE
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
> Task :app:processDebugJavaRes UP-TO-DATE

> Task :app:compileDebugUnitTestKotlin FAILED
24 actionable tasks: 1 executed, 23 up-to-date
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt:115:32 Unresolved reference 'ushr'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt:116:80 Operator '==' cannot be applied to 'BigInteger' and 'Int'.

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
