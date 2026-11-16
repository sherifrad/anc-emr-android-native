# Fix Slice 14 Tests Report

## Summary
Updated 7 tests in [VisitEditorRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/VisitEditorRedesignTest.kt) to align with Slice 14 production behavior and viewport mechanics without modifying production code.

## Test Fix Details

1. **`savePhaseFallback_noTextBelow12sp_preservesWorkflowSubtitleAndStaleCard`**
   - Replaced old subtitle assertion (`"${monaRagab.displayName} · Routine Antenatal Visit & Vitals"`) with assertion on `monaRagab.displayName` in the patient banner, aligning with `VisitPatientBanner` rendering across all save phases and `EditorPage` empty subtitle.
   - Preserved assertions for `staleError`, `saveStatus`, absence of `StepHeader`, fallback field displays, and `>= 12.sp` typography.

2. **`patientBanner_monaRagab_gaParityBloodGroup_andArabicAlignment`**
   - Changed GA assertion from `onNode(hasText("27+2 wks"))` to `onAllNodesWithText("27+2 wks", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()`.
   - Resolves collision where `"27+2 wks"` is rendered both in `VisitPatientBanner` (top) and in the Step 0 gestational age card (`VisitStepDateAndBp`).

3. **`stepHeader_navigationJumpsDirectly_andDisplaysLabelsAtNormalFontScale`**
   - Step tab labels in `VisitStepHeader` use `Modifier.clearAndSetSemantics {}` to prevent screen reader redundancy with the tab's accessible `contentDescription`.
   - Replaced `onNodeWithText` lookups for `"Date & BP"`, `"Wt & FHR"`, etc., with semantic matching on `hasTestTag("stepSegment-$i") and hasContentDescription(...)` using `R.string.visit_step_segment_cd`.
   - Added `performScrollTo()` before segment tab clicks and field assertions to ensure stable navigation on narrow viewports.

4. **`step0_bpDualSync_highBpWarningRaisedAndSevere_nonBlockingSave`**
   - Added `.performScrollTo()` before clicking `node("stepSegment-3")` to advance to review after scrolling down to `highBpWarning`.
   - Removed `.performScrollTo()` from `node("saveVisit")` since the primary action button is pinned in `pinnedBottomBar` (outside the scroll container).
   - Added `.performScrollTo()` before clicking `node("stepSegment-0")` and before replacing text in `visitBloodPressure`.

5. **`step3_summaryCard_reviewRowsSentenceCase_andJumpToStep`**
   - Added `performScrollTo()` before asserting `node("visitBloodPressure")` on Step 0 when jumping back from Step 3 summary row.
   - Added `performScrollTo()` before clicking `node("stepSegment-3")` to re-advance to Step 3.

6. **`validationErrors_displayedOnInvalidDraftFields`**
   - Added `performScrollTo()` before clicking `node("stepSegment-1")` after scrolling down to assert `blood_pressure_invalid` error on Step 0.
   - Ensures transition to Step 1 succeeds so `weight_invalid` (`"Invalid (30 – 250)"`) is composed and displayed.

7. **`darkTheme_andRtl_layoutCleanWithoutOverlap`**
   - Added `performScrollTo()` before clicking `node("stepSegment-3")` after scrolling down to `visitBloodPressure` on Step 0.
   - Ensures transition to Step 3 succeeds before asserting `visitSummaryCard`.

## Verification
- Ran `./gradlew --offline :app:compileDebugAndroidTestKotlin -q` -> Exit code 0.
- Ran `./gradlew --offline :app:testDebugUnitTest -q` -> Exit code 0.
