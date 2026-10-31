# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3d. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3d": oracles Q32-Q40, S17-S22) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: CORE BUILDER for ROUND 3d. Make the tests and checks pass without editing them (tests are the contract).
ALLOWED files: app/src/main/res/xml/file_paths.xml (Q32), app/src/main/java/com/sherifradwan/ancnative/MainActivity.kt (Q35), .../security/SecuritySettingsRepository.kt (Q38), .../ui/report/PatientPrintHtmlGenerator.kt (Q37), .../storage/AttachmentSyncManager.kt (Q39 label only).
Implement exactly the spec text for those items. Minimal, readable changes in the surrounding style. Do not touch other files.
Run: python3 .agents/checks/check_r3d.py (S17-S19 and the S22 AttachmentSyncManager part are yours), ./gradlew --offline :app:testDebugUnitTest --tests '*PatientPrintHtmlGeneratorTest' --tests '*PinPolicyTest' --tests '*AppLockPolicyTest', ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin. Report real results.


ALLOWED (machine-enforced): app/src/main/res/xml/file_paths.xml, app/src/main/java/com/sherifradwan/ancnative/MainActivity.kt, app/src/main/java/com/sherifradwan/ancnative/security/SecuritySettingsRepository.kt, app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt, app/src/main/java/com/sherifradwan/ancnative/storage/AttachmentSyncManager.kt

REPAIR ROUND: these selected tests still fail. Fix the production code only.
thJavac NO-SOURCE
> Task :app:processDebugJavaRes UP-TO-DATE
> Task :app:processDebugUnitTestJavaRes UP-TO-DATE

> Task :app:testDebugUnitTest FAILED

PatientPrintHtmlGeneratorTest > q19g_ownerWordingUnchanged FAILED
    java.lang.AssertionError at PatientPrintHtmlGeneratorTest.kt:439

PatientPrintHtmlGeneratorTest > q17_boundary_notHighRiskButRhNegative_normalBadgeWithRhBadge FAILED
    java.lang.AssertionError at PatientPrintHtmlGeneratorTest.kt:156

PatientPrintHtmlGeneratorTest > q17_validatedHighRiskRhNegative_highRiskBadgeRhBadgeAntiD FAILED
    java.lang.AssertionError at PatientPrintHtmlGeneratorTest.kt:135

PatientPrintHtmlGeneratorTest > q17_validatedLowRiskRhPositive_normalRiskBadgeOnly FAILED
    java.lang.AssertionError at PatientPrintHtmlGeneratorTest.kt:146

PatientPrintHtmlGeneratorTest > q17_rhNegative_values_showRhBadgeAndAntiD FAILED
    java.lang.AssertionError at PatientPrintHtmlGeneratorTest.kt:98

PatientPrintHtmlGeneratorTest > q17_rhPositiveOrUnknown_noRhBadgeNoAntiD FAILED
    java.lang.AssertionError at PatientPrintHtmlGeneratorTest.kt:123
28 actionable tasks: 1 executed, 27 up-to-date

45 tests completed, 6 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:testDebugUnitTest'.
> There were failing tests. See the report at: file:///Users/sherifgamalradwan/anc-emr-android-native/app/build/reports/tests/testDebugUnitTest/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 856ms
