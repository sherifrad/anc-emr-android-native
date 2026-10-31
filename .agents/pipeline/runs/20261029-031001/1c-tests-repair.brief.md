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

ROLE: TEST DESIGNER (tests first). You have NOT seen and must not read the new implementation; write tests from the spec only. Existing production code may not even compile against these tests yet: that is expected (red phase).
ALLOWED files (create/overwrite only these):
- app/src/test/java/com/sherifradwan/ancnative/ui/ParseDeliveryNotesSpecTest.kt   (start from .agents/maca-run-2026-10-29/ParseDeliveryNotesSpecTest.kt.txt, which was written from the spec; adapt it to the interface contract: internal parseDeliveryNotes/DeliveryNoteSections, and extend it for P1-P5 incl. unconfirmed-fee wording, "10:30" colon case, hospital-on-fee-line, multiple " at ")
- app/src/test/java/com/sherifradwan/ancnative/ui/ClinicalFormatTest.kt   (P6, P7, P8; table-driven; every example from the spec plus 3 of your own boundary cases each)
- app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt   (P9: WCAG contrast function in the test; check the Theme.kt token pairs listed in the spec, light and dark; read Theme.kt for token names)
- .agents/checks/check_ui_static.py   (P10: stdlib Python, reads source files under app/src/main, prints PASS/FAIL per sub-check a-f, exit 1 if any FAIL; it must FAIL on the current code for a, b, c, d, e, f)
- .agents/TEST_MANIFEST.json   (format: skills template ~/skills/multi-agent-collaborative-audit/resources/templates/TEST_MANIFEST.template.json. Entries: UNIT-parse (tier 1), UNIT-format (tier 1), UNIT-contrast (tier 1), STATIC-ui (tier 1, cmd python3 .agents/checks/check_ui_static.py), COMPILE-app (tier 2, cmd ./gradlew --offline :app:compileDebugKotlin). Use real deps globs, e.g. app/src/main/**/ui/*.kt)
Deliver the tests so that each oracle fails on today's behaviour and passes on a correct implementation. Do not implement production code.


ALLOWED (machine-enforced): app/src/test/java/com/sherifradwan/ancnative/ui/ParseDeliveryNotesSpecTest.kt, app/src/test/java/com/sherifradwan/ancnative/ui/ClinicalFormatTest.kt, app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt, .agents/checks/check_ui_static.py, .agents/TEST_MANIFEST.json

REPAIR ROUND: your test sources do not compile, which blocks every unit test. Fix compile errors in YOUR test files only; keep each oracle's assertions and strictness unchanged.
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

BUILD FAILED in 746ms
