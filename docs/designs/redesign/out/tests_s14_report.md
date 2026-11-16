# Redesign Slice 14: Visit Editor Tests Report

## Summary
Created [VisitEditorRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/VisitEditorRedesignTest.kt) implementing comprehensive UI, accessibility, and behavioral verification for Redesign Slice 14 (`VisitEditor`, `VisitEditorSteps`, `VisitPatientBanner`, and `VisitStepHeader`).

The tests follow the project test style (`RegistrationRedesignTest.kt`, `HistoryEditorsRedesignTest.kt`):
- Single `setContent` per test method.
- Narrow `360.dp x 640.dp` viewport container with clipping boundary checks.
- Dynamic `fontScale` overrides (`1.0f`, `1.3f`, `2.0f`).
- `useUnmergedTree = true` for semantic traversal where labels are cleared by `clearAndSetSemantics`.
- `performScrollTo()` before assertions on scrollable items.
- Node selection primarily via explicit `testTag`s.
- Minimum touch target tolerance `>= 47.99.dp`.

## Coverage Matrix

### 1. Typography (No Text < 12sp & Clean Hierarchy)
- `walkSemanticsTree_noTextBelow12sp_andNoLegacyStrings_atFontScale1_0`: Walks Steps 0, 1, 2, and 3 verifying zero text nodes under `12.sp` and verifying that legacy strings (`"AES-256"`, `"Tamper-Evident"`, `"Normal Risk"`, `"(Flag)"`, `"27w 2d"`) are absent.
- `walkSemanticsTree_noTextBelow12sp_atFontScale1_3`: Verifies all text styles remain `>= 12.sp` at `1.3f` font scale across all steps.
- `savePhaseFallback_noTextBelow12sp_preservesWorkflowSubtitleAndStaleCard`: Verifies `SavePhase.ERROR` fallback preserves `"${patient.displayName} · Routine Antenatal Visit & Vitals"` for `AncWorkflowTest`, renders `staleError` and `saveStatus`, and maintains all text `>= 12.sp`.

### 2. Patient Banner (`VisitPatientBanner`)
- `patientBanner_monaRagab_gaParityBloodGroup_andArabicAlignment`: Verifies display of `"27+2 wks"`, `"G4 P1 A2"`, `"A+"`, absence of risk tags for low/normal risk, and verified RTL right-edge alignment (`TextAlign.Right`, `TextDirection.Rtl`, `ResolvedTextDirection.Rtl`) for Arabic name `"منى رجب"`.
- `patientBanner_englishName_alignedLeftEdge`: Verifies LTR left-edge alignment for English name `"Mona Ragab"`.
- `patientBanner_moderateAndHighRiskTags_andRhNegative`: Verifies `StatusTag` for `HighRisk`, and `BandChip` with `StatusTone.High` for `"Rh negative"`.
- `patientBanner_moderateRiskTag_displayed`: Verifies `StatusTag` for `ModerateRisk`.

### 3. Step Header (`VisitStepHeader`)
- `stepHeader_navigationJumpsDirectly_andDisplaysLabelsAtNormalFontScale`: Verifies segment labels (`"Date & BP"`, `"Wt & FHR"`, `"Findings"`, `"Review"`) at `1.0f` scale and direct jump navigation between step segments (`stepSegment-3` -> Step 4 of 4).
- `stepHeader_largeFontScale2_0_hidesShortLabelsPreservesTabs`: Verifies that short labels are cleanly omitted above `1.3f` font scale while tabs and indicator bars remain accessible and clickable.

### 4. Interactive Touch Targets (>= 47.99dp)
- `step0_and_step1_touchTargets_atLeast48dp`: Verifies `>= 47.99.dp` width & height on `stepSegment-0..3`, `visitDateToday`, `visitDateYesterday`, `visitDateCalendarButton`, `bpPreset-sameLast`, `bpPreset-normal`, `bpPreset-addOther`, `cancelVisit`, `nextStep`, `weightPreset-sameLast`, `weightPreset-addOther`, `visitWeightDecrease`, `visitWeightIncrease`, `fhrPreset-sameLast`, `fhrPreset-135..150`, `visitFetalHeartRateDecrease`, `visitFetalHeartRateIncrease`, `backStep`.
- `step2_and_step3_touchTargets_atLeast48dp`: Verifies `>= 47.99.dp` on `fillNormalValues`, `oedemaChoice-*`, `movementsChoice-*`, `urineChoice-*`, `summaryRow-date..urine`, `linkScan-*`, `unlinkUltrasound`, `saveVisit`.
- `customChoiceDialog_touchTargets_atLeast48dp_andDismisses`: Verifies `>= 47.99.dp` on `customChoiceConfirm` and `customChoiceCancel`, and verifies dialog dismiss behavior.

### 5. Behavioral Parity & Clinical Logic
- `step0_bpDualSync_highBpWarningRaisedAndSevere_nonBlockingSave`: Verifies dual sync between `visitSystolic` / `visitDiastolic` and `visitBloodPressure`, verifies `highBpWarning` shows `"Raised BP"` vs `"Severe BP"` with `"High BP is flagged on the visit. You can still save."`, and verifies that high BP never disables `saveVisit`.
- `step0_todayAndYesterdayChips_populateDate`: Verifies `visitDateToday` and `visitDateYesterday` chips dispatch `AncEvent.DraftChanged(DraftField.DATE, ...)`.
- `step1_weightStepper_incrementsDecrementsAndClamps`: Verifies `+/- 0.1` stepping, US locale format, and clamping at `[30.0, 250.0]`.
- `step1_fhrStepper_incrementsDecrementsAndClamps`: Verifies `+/- 2` stepping and clamping at `[60, 220]`.
- `step1_fhrBaselineStatusWords_noIcons`: Verifies `"Normal baseline (110-160 bpm)"`, `"Bradycardia (<110 bpm)"`, and `"Tachycardia (>160 bpm)"` render without `"✓"` or `"⚠️"`.
- `step2_fillNormalValues_andBedsideFlaggedStatusWords`: Verifies `fillNormalValues` fills only blank findings without overwriting existing data, and verifies `"Flagged"` `StatusWord` displays on abnormal selections without `"(Flag)"` on unselected chips.
- `step3_summaryCard_reviewRowsSentenceCase_andJumpToStep`: Verifies sentence-case `"Fetal heart rate"`, `"Edit"` action labels, row dividers, jump back to Step 0 on row click, and `onSave` invocation on `saveVisit` click.
- `step3_linkedUltrasound_linkAndUnlink_togglesDisplay`: Verifies scan linking with GA text `" (22+3 wks)"`, `unlinkUltrasound` display, and unlinking back to `linkScan-*`.
- `validationErrors_displayedOnInvalidDraftFields`: Verifies field error messages for `DraftError.DATE_REQUIRED`, `BLOOD_PRESSURE_INVALID`, and `WEIGHT_INVALID`.

### 6. Ergonomics & Themes
- `layout360dp_fontScale2_0_noHorizontalOverflowOrClipping`: Verifies bottom bar buttons wrap (`maxLines = 2`) and fit within 360dp, stepper buttons expand height without vertical clipping, and review rows remain within 360dp width.
- `darkTheme_andRtl_layoutCleanWithoutOverlap`: Verifies full render under `AncTheme(darkTheme = true)` and `LayoutDirection.Rtl`.

## Verification Status
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: Exit code `0` (Success).
- `./gradlew --offline :app:testDebugUnitTest -q`: Exit code `0` (Success).
- Existing `androidTest` suites (`VisitEntryDesignTest`, `AncWorkflowTest`, etc.) compile cleanly without modification because the builder preserved all existing tags and semantics.
