# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3t. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3t": oracles Q104-Q106, S86-S88) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. NEVER write dead code, extra XML/files or comments only to satisfy a static check or a test; if a check looks wrong, implement the spec and say so in your final message. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: TEST DESIGNER for ROUND 3t (SPEC_FIX_R3.md section "Round 3t"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3t.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/GestationalDisplayTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt (rewrite per Q106).
1. check_r3t.py: S86-S88 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL; exit 1 on FAIL; style of .agents/checks/check_r3s.py; each S must FAIL on today's code.
2. GestationalDisplayTest per the "Unit" paragraph with exact values. Rewrite GestationalBannerLogicTest so every scenario calls the production members (read the file first; keep each scenario's inputs and expected numbers; replace local formula copies by the production calls; list in your final message what you replaced).
3. TEST_MANIFEST.json: add STATIC-r3t and UNIT-gestational-display (cmd, deps, covers); the banner logic test already has an entry: make sure its deps include GestationalAgeEngine.kt.
Run check_r3t.py and report real results (the unit tests will not compile until core is done).


ALLOWED (machine-enforced): .agents/checks/check_r3t.py, .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/GestationalDisplayTest.kt, app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt

REPAIR ROUND: your test sources do not compile (new-API tests were parked, so this error is elsewhere). Fix compile errors in YOUR test files only; keep every assertion unchanged.
ionalBannerLogicTest.kt:97:27 Unresolved reference 'isLateTerm'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:100:31 Unresolved reference 'progressFraction'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:105:24 Unresolved reference 'daysToEdd'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:113:32 Unresolved reference 'isLateTerm'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:136:24 Unresolved reference 'daysToEdd'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:180:11 Unresolved reference 'progressFraction'.

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

BUILD FAILED in 2s

alBannerLogicTest.kt:97:27 Unresolved reference 'isLateTerm'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:100:31 Unresolved reference 'progressFraction'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:105:24 Unresolved reference 'daysToEdd'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:113:32 Unresolved reference 'isLateTerm'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:136:24 Unresolved reference 'daysToEdd'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:180:11 Unresolved reference 'progressFraction'.

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

alBannerLogicTest.kt:97:27 Unresolved reference 'isLateTerm'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:100:31 Unresolved reference 'progressFraction'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:105:24 Unresolved reference 'daysToEdd'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:113:32 Unresolved reference 'isLateTerm'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:136:24 Unresolved reference 'daysToEdd'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:180:11 Unresolved reference 'progressFraction'.

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

FAIL S86 GestationalAgeEngine.kt defines isLateTerm, progressFraction, shortWeeksText, daysToEdd and calculateForEdd calls daysToEdd: GestationalAgeEngine.kt lacks isLateTerm; GestationalAgeEngine.kt lacks progressFraction; GestationalAgeEngine.kt lacks shortWeeksText; GestationalAgeEngine.kt lacks fun daysToEdd(; calculateForEdd does not call daysToEdd
FAIL S87 GestationalBanner has no ChronoUnit/weeks>=42/FULL_TERM_DAYS, screens have no hand-formatted wks and reference shortWeeksText/progressFraction/daysToEdd: GestationalBanner.kt contains ChronoUnit; GestationalBanner.kt contains weeks >= 42; GestationalBanner.kt contains FULL_TERM_DAYS; GestationalBanner.kt does not reference progressFraction; GestationalBanner.kt does not call daysToEdd; PatientListScreen.kt contains +${...} wks hand formatting; PatientListScreen.kt does not reference shortWeeksText; ProfileScreen.kt contains +${...} wks hand formatting; ProfileScreen.kt does not reference shortWeeksText
PASS S88 GestationalBannerLogicTest.kt contains no 280f, no ChronoUnit and calls progressFraction and daysToEdd

alBannerLogicTest.kt:97:27 Unresolved reference 'isLateTerm'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:100:31 Unresolved reference 'progressFraction'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:105:24 Unresolved reference 'daysToEdd'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:113:32 Unresolved reference 'isLateTerm'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:136:24 Unresolved reference 'daysToEdd'.
e: file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt:180:11 Unresolved reference 'progressFraction'.

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

BUILD FAILED in 544ms
