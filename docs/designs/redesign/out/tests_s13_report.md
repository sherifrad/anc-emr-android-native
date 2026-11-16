Created [`HistoryEditorsRedesignTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt) and updated [`RegistrationRedesignTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationRedesignTest.kt) to test redesign slice 13 (OtherHistoriesEditor, PreviousPregnancyEditor, DatingEditor) and the status tag update.

### Files Modified / Created

- [RegistrationRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationRedesignTest.kt#L649-L650) — updated `patientStatusEditor_archive_showsStatusTag_cancelAndSave_emitsEvents` to assert `R.string.status_will_be_archived` (`"Will be archived"`) instead of `R.string.archived`.
- [HistoryEditorsRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt) — new instrumentation test suite with 37 test cases covering `OtherHistoriesEditor`, `PreviousPregnancyEditor`, and `DatingEditor`.

---

### Tests Written (38 test cases in `HistoryEditorsRedesignTest.kt`)

#### 1. OtherHistoriesEditor (10 tests)
1. `otherHistoriesEditor_noNumberedTitles_noLockFooter_cleanHeader`: Verifies standalone header with patient name subtitle, card titles without `"1. "`, `"2. "`, `"3. "`, `"4. "` prefixes, no padlock lock footer text (`"AES-256"`, `"Tamper-Evident"`), 4 section headings (`medical_history`, `surgical_history`, `family_history`, `allergy_history`), `"Quick shortcuts"` (`common_presets`) preset heading, and existing text field tags (`medicalHistory`, `surgicalHistory`, `familyHistory`, `allergyHistory`).
2. `otherHistoriesEditor_negativeHistoriesCard_noCheckPrefix_fillAllNegative_populatesFields`: Verifies `negativeHistoriesCard` displays `"Explicit negative histories"`, `fillAllNegativeHistories` button displays `"Confirm all histories negative"` without `"✓ "` prefix, clicking populates all 4 fields with localized negative strings, and emits 4 `AncEvent.HistoriesDraftChanged` events.
3. `otherHistoriesEditor_presetChips_clickingPopulatesField`: Verifies clicking quick preset chips (`medHistoryPreset-1`, `surgHistoryPreset-1`, `famHistoryPreset-1`, `allergyHistoryPreset-1`) populates the corresponding draft fields.
4. `otherHistoriesEditor_touchTargets_atLeast48dp`: Verifies all interactive targets (`fillAllNegativeHistories`, `medHistoryPreset-0..1`, `surgHistoryPreset-0..1`, `famHistoryPreset-0..1`, `allergyHistoryPreset-0..1`, `cancelHistories`, `saveHistories`) meet the `>= 47.99dp` width and height requirement.
5. `otherHistoriesEditor_standalone_cancelAndSave_emitEvents`: Verifies `saveHistories` and `cancelHistories` emit `AncEvent.SubmitOtherHistories` and `AncEvent.CancelEditor`.
6. `otherHistoriesEditor_standalone_saving_disablesButtons_showsSpinnerAndSavingLabel`: Verifies during `SavePhase.PENDING`, Save and Cancel buttons are disabled, `historiesSaveSpinner` exists, and `"Saving…"` (`R.string.saving_label`) is displayed.
7. `otherHistoriesEditor_noTextBelow12sp_fontScale1_0`: Verifies zero text nodes under `12.sp` at font scale 1.0.
8. `otherHistoriesEditor_noTextBelow12sp_fontScale1_3`: Verifies zero text nodes under `12.sp` at font scale 1.3.
9. `otherHistoriesEditor_360dp_fontScale2_0_noClipping`: Verifies at 360dp width with font scale 2.0 that `negativeHistoriesCard` text remains whole and preset chips wrap cleanly without horizontal clipping.
10. `otherHistoriesEditor_inFlow_hidesStandaloneCancelSave`: Verifies that within a registration flow (`inFlow = true`), standalone `cancelHistories` and `saveHistories` buttons are hidden.

#### 2. PreviousPregnancyEditor (11 tests)
11. `previousPregnancyEditor_noNumberedTitles_noLockFooter_cleanCards`: Verifies clean header, card titles (`pregnancy_core_facts_title`, `pregnancy_details_title`) without `"1. "` or `"2. "`, no padlock lock footer text (`"AES-256"`, `"Tamper-Evident"`), and presence of existing tags (`pregnancyDateText`, `pregnancyDateTextCalendarButton`, `pregnancyOutcome-0`, `pregnancyPlaceDoctor`, `pregnancyType-0`, `pregnancyOtherDetails`).
12. `previousPregnancyEditor_outcomeChips_humanLabels_dispatchCanonicalCodes`: Verifies outcome chips display human clinical labels (`"Unknown"`, `"Live birth"`, `"Stillbirth / IUFD"`, `"Early pregnancy loss"`, `"Termination of pregnancy"`), and clicking dispatches canonical stored codes (`"LiveBirth"`, `"Stillbirth"`, `"EarlyPregnancyLoss"`, `"TerminationOfPregnancy"`) via `AncEvent.PregnancyDraftChanged`.
13. `previousPregnancyEditor_conditionalFields_deliveryMode_vaginalAndCesareanCodes`: Verifies for `LiveBirth` that delivery mode chips display human labels (`"Unknown"`, `"Vaginal"`, `"Cesarean"`), selecting Cesarean dispatches canonical code `"CS"`, selecting Vaginal dispatches `"Vaginal"`, and loss management / termination methods are absent.
14. `previousPregnancyEditor_conditionalFields_lossManagement_and_terminationMethod`: Verifies switching to `EarlyPregnancyLoss` hides delivery mode and reveals `lossManagement-0..3` (selecting updates draft to `"Expectant"`), and switching to `TerminationOfPregnancy` hides loss management and reveals `terminationMethod-0..3` (selecting updates draft to `"Medical"`).
15. `previousPregnancyEditor_touchTargets_atLeast48dp`: Verifies interactive controls (`pregnancyDateTextCalendarButton`, `pregnancyOutcome-0..4`, `deliveryBy-0..2`, `pregnancyType-0..1`, `cancelPregnancy`, `savePregnancy`) meet `>= 47.99dp` width and height.
16. `previousPregnancyEditor_standalone_cancelAndSave_emitEvents`: Verifies `savePregnancy` and `cancelPregnancy` emit `AncEvent.SubmitPreviousPregnancy` and `AncEvent.CancelEditor`.
17. `previousPregnancyEditor_standalone_saving_disablesButtons_showsSpinnerAndSavingLabel`: Verifies during `SavePhase.PENDING`, Save and Cancel are disabled, `pregnancySaveSpinner` exists, and `"Saving…"` is shown.
18. `previousPregnancyEditor_noTextBelow12sp_fontScale1_0`: Verifies zero text nodes under `12.sp` at font scale 1.0.
19. `previousPregnancyEditor_noTextBelow12sp_fontScale1_3`: Verifies zero text nodes under `12.sp` at font scale 1.3.
20. `previousPregnancyEditor_360dp_fontScale2_0_noClipping`: Verifies layout integrity and clean multi-line wrapping at 360dp width with font scale 2.0.
21. `previousPregnancyEditor_inFlow_hidesStandaloneCancelSave`: Verifies standalone Save and Cancel are hidden when hosted in registration flow.

#### 3. DatingEditor (17 tests)
22. `datingEditor_noNumberedTitles_noLockFooter_cleanSections`: Verifies clean header, localized section titles (`dating_baseline_section`, `notes`) without numbers, no padlock lock footer text, and existing tags (`datingMethodDropdown`, `agreedEdd`, `agreedEddCalendarButton`, `datingNotes`).
23. `datingEditor_calculatedCard_hiddenWhenEmpty_bothNull`: Verifies calculated dating card (`dating_calculated_section`, `calculated_edd`, `current_gestational_age`) is completely omitted when both `calculatedEdd` and `calculatedGa` are null.
24. `datingEditor_calculatedCard_shownWhenPresent_formatsDmyAndWeeksStyle`: Verifies when calculated values exist that the card displays `"Calculated dating"`, EDD formatted as `dd/MM/yyyy` (e.g., `"11/02/2027"`), gestational age formatted in `+` / `wks` style (e.g., `"27+2 wks"`), and raw ISO dates do not appear in the calculated card.
25. `datingEditor_calculatedCard_postTerm_showsPostBadge`: Verifies that when gestational age is post-term (>= 42 weeks), `StatusWord` with `"Post"` (`R.string.post_term_badge`) is displayed alongside the weeks text.
26. `datingEditor_fieldVisibility_lmpMethod_andTodayButton`: Verifies for `LMP` method that `lmpDate`, `lmpDateCalendarButton`, and `lmpDateTodayButton` are displayed, while `ultrasoundEdd` and `ivfTransferDate` are absent. Verifies clicking `lmpDateTodayButton` sets today's date in ISO format.
27. `datingEditor_fieldVisibility_ultrasoundMethod`: Verifies for `Ultrasound` method that `ultrasoundEdd` and `ultrasoundEddCalendarButton` are displayed, while `lmpDate` and `ivfTransferDate` are absent.
28. `datingEditor_fieldVisibility_ivfMethod_andEmbryoAgeChips`: Verifies for `IVF_TRANSFER` method that `ivfTransferDate`, `ivfTransferDateCalendarButton`, and embryo age chips (`embryoDay3`, `embryoDay5`, `embryoDay6`) are displayed, and clicking `embryoDay3` dispatches `DatingDraftChanged(EMBRYO_AGE_DAYS, "3")`.
29. `datingEditor_fieldVisibility_clinicalMethod_andRecordedDateNotes`: Verifies for `Clinical` method that both `lmpDate` and `ultrasoundEdd` are displayed, and for `Ultrasound` method with existing LMP on record that `RecordedDateNote` displays `"LMP on record: <dd/MM/yyyy>"`.
30. `datingEditor_touchTargets_atLeast48dp`: Verifies IVF interactive controls (`ivfTransferDateCalendarButton`, `embryoDay3`, `embryoDay5`, `embryoDay6`, `agreedEddCalendarButton`, `cancelDating`, `saveDating`) meet `>= 47.99dp` width and height.
31. `datingEditor_lmpTodayButton_touchTarget_atLeast48dp`: Verifies `lmpDateCalendarButton` and `lmpDateTodayButton` meet `>= 47.99dp` touch target minimum.
32. `datingEditor_standalone_cancelAndSave_emitEvents`: Verifies `saveDating` and `cancelDating` emit `AncEvent.SubmitDating` and `AncEvent.CancelEditor`.
33. `datingEditor_standalone_saving_disablesButtons_showsSpinnerAndSavingLabel`: Verifies during `SavePhase.PENDING`, Save and Cancel are disabled, `datingSaveSpinner` exists, and `"Saving…"` is shown.
34. `datingEditor_noTextBelow12sp_fontScale1_0`: Verifies zero text nodes under `12.sp` across baseline and calculated sections at font scale 1.0.
35. `datingEditor_noTextBelow12sp_fontScale1_3`: Verifies zero text nodes under `12.sp` at font scale 1.3.
36. `datingEditor_360dp_fontScale2_0_noClipping`: Verifies 360dp layout integrity with font scale 2.0 (metrics stacking and wrapping without horizontal overflow).
37. `datingEditor_rtlMirror_darkTheme_noClipping`: Verifies DatingEditor renders cleanly under `darkTheme = true` and `LayoutDirection.Rtl`.
38. `datingEditor_inFlow_hidesStandaloneCancelSave`: Verifies standalone Save and Cancel are hidden when in registration flow.

---

### Verification

1. `./gradlew --offline :app:compileDebugAndroidTestKotlin -q` — passed (`BUILD SUCCESSFUL` with exit code 0).
2. `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin -q` — passed (`BUILD SUCCESSFUL` with exit code 0).
3. `./gradlew --offline :app:testDebugUnitTest --tests "com.sherifradwan.ancnative.ui.DatingFieldVisibilityTest" --tests "com.sherifradwan.ancnative.state.DatingDraftRuleTest" --tests "com.sherifradwan.ancnative.model.PreviousPregnancyValidationTest" --tests "com.sherifradwan.ancnative.ui.ClinicalPresetsTest" --tests "com.sherifradwan.ancnative.storage.PatientCommandsTest"` — passed (`BUILD SUCCESSFUL` with 28 actionable tasks, all unit tests green).
