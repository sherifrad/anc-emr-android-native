# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 2. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R2.md (oracles Q1-Q7, U1-U6, G1-G3) and .agents/pipeline/OWNER_DECISIONS.md. Round-1 oracles in .agents/pipeline/SPEC_FIX.md still hold unless R2 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files, gradle properties, or any test file you do not own.
4. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, package com.sherifradwan.ancnative.ui. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

CURRENT TASK = ROUND 2b: implement Q8-Q12 of the Round 2b addendum in DeliveryNotesParser.kt only. ClinicalFormat.kt needs no change.
ROLE: CORE LOGIC BUILDER. Make the tests pass without editing them (tests are owned by another agent and are the contract).
ALLOWED files:
- app/src/main/java/com/sherifradwan/ancnative/ui/DeliveryNotesParser.kt   (Q1-Q4: segment split, lossless rejoin with original separator, tense markers, labelled + unlabelled hospital rules)
- app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalFormat.kt   (Q5 formatAgeYears, Q6 isRhPositive + formatBloodGroup; pure Kotlin, no Compose)
Read the tests under app/src/test/java/com/sherifradwan/ancnative/ui/ first. Keep the parser a pure function and readable (named regex constants, short KDoc like today). Word-boundary regexes only.
Run: ./gradlew --offline :app:testDebugUnitTest --tests '*ParseDeliveryNotesSpecTest' --tests '*DeliveryNotesParserTest' --tests '*ClinicalFormatTest' --tests '*ClinicalFormatR2Test' and report the real result.


ALLOWED (machine-enforced): app/src/main/java/com/sherifradwan/ancnative/ui/DeliveryNotesParser.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalFormat.kt

REPAIR ROUND: these selected tests still fail. Fix the production code only.
ionError at ParseDeliveryNotesSpecTest.kt:597

ParseDeliveryNotesSpecTest > r2_q8_historyAndCurrentSegments_onlyCurrentNameExtracted FAILED
    org.junit.ComparisonFailure at ParseDeliveryNotesSpecTest.kt:546

ParseDeliveryNotesSpecTest > r2_q11_undecidedListIsCaseInsensitive FAILED
    java.lang.AssertionError at ParseDeliveryNotesSpecTest.kt:604

ParseDeliveryNotesSpecTest > r2_q12_lowercaseSplit_sendsFeeToItsHome FAILED
    org.junit.ComparisonFailure at ParseDeliveryNotesSpecTest.kt:619

ParseDeliveryNotesSpecTest > r2_q9_noRealNameBeyondGenerics_isRejected FAILED
    java.lang.AssertionError at ParseDeliveryNotesSpecTest.kt:575

ParseDeliveryNotesSpecTest > r2_q10_multiplePhrasesInOneSegment_allCollectedInOrder FAILED
    org.junit.ComparisonFailure at ParseDeliveryNotesSpecTest.kt:583

ParseDeliveryNotesSpecTest > r2_q11_abbreviatedDotInsideName_notCut FAILED
    org.junit.ComparisonFailure at ParseDeliveryNotesSpecTest.kt:591

ParseDeliveryNotesSpecTest > r2_q8_historyHospital_notExtracted FAILED
    java.lang.AssertionError at ParseDeliveryNotesSpecTest.kt:539
26 actionable tasks: 1 executed, 25 up-to-date

77 tests completed, 10 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:testDebugUnitTest'.
> There were failing tests. See the report at: file:///Users/sherifgamalradwan/anc-emr-android-native/app/build/reports/tests/testDebugUnitTest/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 1s
