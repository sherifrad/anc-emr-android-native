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

ROLE: UI/UX BUILDER. Fix the UI defects from the spec, no logic changes beyond calling the existing helpers.
ALLOWED files:
- app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalBadges.kt
- app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt
- app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt   (DeliveryPlanningCard region only)
- app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt   (age badge usage only)
- app/src/main/res/values/strings.xml   (add only: edit_delivery_plan, plus age strings if you need them)
Tasks: (1) restore the delivery_planning_notice text under the card header (P10a). (2) restore stringResource for the five labels agreed_edd, current_gestational_age, manual_risk, fetal_presentation, placental_site in the KeyFactTile calls (P10b); keep the tile layout. (3) the delivery edit icon gets contentDescription = stringResource(R.string.edit_delivery_plan) with the same text as today (P10c). (4) PatientAgeBadge: use formatAgeYears(ageText) (return nothing when null), clearAndSetSemantics with the existing description, spacer in ProfileScreen only when the badge shows (P10d). (5) Dark "AGE" label background to #4F46E5 or another token with >= 4.5:1 against white; placeholder text in DeliverySectionBox at full opacity (no alpha 0.65), use italic to mark it as a placeholder (P9).
Verify with: python3 .agents/checks/check_ui_static.py and ./gradlew --offline :app:compileDebugKotlin :app:testDebugUnitTest --tests '*ThemeContrastTest'. Report real results.


ALLOWED (machine-enforced): app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalBadges.kt, app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt, app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt, app/src/main/res/values/strings.xml

REPAIR ROUND: these checks still fail.
TO-DATE
> Task :app:processDebugManifest UP-TO-DATE
> Task :app:processDebugManifestForPackage UP-TO-DATE
> Task :app:processDebugResources UP-TO-DATE
> Task :app:kspDebugKotlin UP-TO-DATE
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:bundleDebugClassesToRuntimeJar UP-TO-DATE
> Task :app:bundleDebugClassesToCompileJar UP-TO-DATE
> Task :app:kspDebugUnitTestKotlin UP-TO-DATE
> Task :app:compileDebugUnitTestKotlin UP-TO-DATE
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugJavaRes UP-TO-DATE
> Task :app:processDebugUnitTestJavaRes UP-TO-DATE

> Task :app:testDebugUnitTest FAILED

ThemeContrastTest > deliveryBoxes_placeholder_isFullOpacityToken_notAlphaCopy FAILED
    java.lang.AssertionError at ThemeContrastTest.kt:128

ThemeContrastTest > ageBadge_andDeliveryTone_pairs_meetWcagAa_4_5 FAILED
    java.lang.AssertionError at ThemeContrastTest.kt:104
26 actionable tasks: 1 executed, 25 up-to-date

3 tests completed, 2 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:testDebugUnitTest'.
> There were failing tests. See the report at: file:///Users/sherifgamalradwan/anc-emr-android-native/app/build/reports/tests/testDebugUnitTest/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 526ms

FAIL P10(a) DeliveryPlanningCard does not reference R.string.delivery_planning_notice
FAIL P10(b) DeliveryPlanningCard missing R.string ids: agreed_edd, current_gestational_age, manual_risk, fetal_presentation, placental_site
FAIL P10(c) delivery edit button uses a contentDescription string literal: contentDescription = "
FAIL P10(d) ClinicalBadges.kt missing: clearAndSetSemantics/mergeDescendants, formatAgeYears
PASS P10(e) WorkspaceScreen.kt uses isRhNegative/isHighRisk, no inline endsWith("-") logic
PASS P10(f) parseDeliveryNotes/DeliveryNoteSections not defined in WorkspaceScreen.kt
FAIL P10 overall: 2/6 sub-checks passed
