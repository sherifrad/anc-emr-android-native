# Redesign Slice 15: Ultrasound Editor & Photo Viewer Tests Report

## Summary
Created [UltrasoundEditorRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/UltrasoundEditorRedesignTest.kt) implementing comprehensive UI, accessibility, and behavioral verification for Redesign Slice 15 (`UltrasoundEditor`, `ZoomableImage` reset chip, and `UltrasoundPhotoViewerDialog`).

The tests strictly adhere to the project style templates ([HistoryEditorsRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt), [RegistrationRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationRedesignTest.kt)):
- Single `setContent` per test method.
- Narrow `360.dp x 640.dp` viewport container with clipping boundary checks.
- Dynamic `fontScale` overrides (`1.0f`, `1.3f`, `2.0f`) via `LocalDensity` and Android configuration.
- `useUnmergedTree = true` for semantic traversal where labels/dots are cleared by `clearAndSetSemantics`.
- `performScrollTo()` before assertions on scrollable items.
- Node selection primarily via explicit `testTag`s rather than ambiguous text.
- Minimum touch target tolerance `>= 47.99.dp`.
- Complete independence of every test case.

## Coverage Matrix

### 1. Typography & Legacy Removal (No Text < 12sp & Clean Hierarchy)
- `ultrasoundEditor_noNumberedTitles_noTriangles_noLockFooter_cleanHeader`: Verifies clean subtitle (patient display name without hardcoded `" · Ultrasound Examination"`), sentence-case section card headings (`ultrasound_timing_section`, `ultrasound_section_fetus_placenta_liquor`, `ultrasound_biometry_section`, `scan_impression`, `ultrasound_more`), and asserts complete absence of legacy patterns (`"1. "`, `"2. "`, `"3. "`, `"4. "`, `"▲"`, `"▼"`, `"AES-256"`, `"Hardware Vault"`, `"Take Photo"`, `"Choose Image"`, `"BPD, HC, AC, FL, FHR & EFW"`).
- `ultrasoundEditor_noTextBelow12sp_fontScale1_0`: Walks the entire semantics tree of `UltrasoundEditor` with biometry and more sections expanded; asserts zero text layout results have `fontSize < 12.sp` (checking `>= 11.99.sp`).
- `ultrasoundEditor_noTextBelow12sp_fontScale1_3`: Verifies all text nodes remain `>= 12.sp` at `1.3f` font scale across all cards.
- `ultrasoundPhotoViewerDialog_layoutAndTypography_noTextBelow12sp`: Verifies clean header (`"Ultrasound scan, \u2066<date>\u2069"`), subtitle (`"Photo 1 of 2, \u206620+0 wks\u2069"`), `PlainField` biometrics grid (`"148 bpm"`, `"380 g"`, `"49 mm"`, `"34 mm"`, `"Cephalic"`), absence of legacy badges (`"FactBadge"`, `"Close Viewer"`, `"Ultrasound Scan: "`), and zero text nodes under `12.sp`.

### 2. Live Summary Card & Guided Finding Auto-Advance
- `ultrasoundEditor_liveSummary_blankInitial_fillNormalScan_clearFindings`: Verifies initial blank report message (`ultrasound_nothing_chosen_yet`), 1-tap `fillNormalScan` applying `NORMAL_FETAL_NUMBER` ("Singleton"), `NORMAL_PRESENTATION` ("Cephalic"), and `NORMAL_AMNIOTIC_FLUID` ("Normal"), opening the next blank finding (`findingOpen-placenta`), updating live summary text ("Singleton pregnancy, ..."), and `clearFindings` restoring blank state and opening `findingOpen-fetalNumber`.
- `ultrasoundEditor_guidedFlow_choicesAutoAdvance_andSummaryRow_noChevron`: Verifies selecting `fetalNumberChoice-1` ("Singleton") dispatches `AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.FETAL_NUMBER, "Singleton")`, automatically advances to `findingOpen-presentation`, collapses `fetalNumber` into `findingRow-fetalNumber` displaying "Singleton" without downward chevron icon, and verifies clicking a closed row reopens it.

### 3. Calculated GA, Redating Hint, & Today Date Button
- `ultrasoundEditor_calculatedGa_formatsShortWeeks_andApplyScanGaDispatchesEvents`: Verifies `scanCalculatedGaCard` displays `"27+2 wks"` (`shortWeeksText`) and asserts old `"27w 2d"` format does not appear. Verifies clicking `applyScanGa` dispatches `GA_WEEKS = "27"` and `GA_DAYS = "2"`, updates draft values, and dismisses `applyScanGa`.
- `ultrasoundEditor_redatingHint_shownWhenMismatchExceedsThreshold`: With dating GA at 27+2 wks (191 days, threshold 14 days) and draft GA at 20+0 wks (140 days), verifies `redatingHint` renders with `str(R.string.redating_hint, 51, 14)` ("Difference of 51 days exceeds 14-day threshold..."), and disappears when GA matches.
- `ultrasoundEditor_scanDateTodayButton_populatesToday`: Verifies clicking `scanDateTodayButton` populates today's ISO date and dispatches `AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.DATE, ...)`.

### 4. Biometry Card & Clinical Impression
- `ultrasoundEditor_biometryCard_toggleExpandsAndCollapses_andFieldsRetainValues`: Verifies biometry card starts collapsed when fields are blank, toggle button shows sentence-case `action_expand` ("Expand"), clicking toggle expands all 6 biometry fields (`fetalHeartRate`, `efw`, `biometryBpd`, `biometryHc`, `biometryAc`, `biometryFl`), entering numeric values dispatches corresponding `AncEvent.UltrasoundDraftChanged` events, and clicking `toggleBiometry` collapses the section.
- `ultrasoundEditor_impressionFromChoices_mergesWithExistingImpression`: Verifies `impressionFromChoices` button is enabled when findings are selected, clicking it generates impression from choices and merges cleanly with pre-existing impression text via `mergeImpression`.

### 5. More Section, Attachments, & Linked Visit
- `ultrasoundEditor_moreSection_toggleAndAttachmentsAndLinkedVisit`: Verifies `ultrasoundMoreToggle` starts closed when unlinked and photo-free, toggle button shows `more_open` ("Open"), clicking opens `ultrasoundMoreSection` showing `scanAttachmentsCard` ("No photos attached") and `linkedVisitCard` ("No linked visit"). Verifies clicking `linkVisit-${visit.id}` links the visit, displays visit date and vitals (`"BP 118/76, weight 74.5 kg"`), displays `unlinkVisit`, and clicking `unlinkVisit` clears `linkedVisitId`.
- `ultrasoundEditor_attachments_showsAttachmentRows_removePhotoDispatchesEvent`: Verifies draft with attached photos displays `attachmentItem-$index` rows with `"Photo 1"`, and clicking `removePhoto-0` dispatches updated `IMAGE_URIS` without photo 0.

### 6. Validation Errors & Save Phase Gating
- `ultrasoundEditor_validationErrors_showsErrorCardAndHighlightsFields`: Verifies `draft.errors` containing `REQUIRED_DATE` and `INVALID_GA_WEEKS` displays `editorError` card and error styling on `scanDate` and `gaWeeks`.
- `ultrasoundEditor_cancelAndSave_emitEvents`: Verifies `cancelUltrasound` emits `AncEvent.CancelEditor` and `saveUltrasound` emits `AncEvent.SubmitUltrasound`.
- `ultrasoundEditor_savingState_disablesControls_showsSpinnerAndSavingLabel`: Verifies `SavePhase.PENDING` disables save and cancel buttons, displays `ultrasoundSaveSpinner`, and shows `saving_label` ("Saving…").

### 7. Interactive Touch Targets (>= 47.99dp)
- `ultrasoundEditor_touchTargets_atLeast48dp`: Verifies `>= 47.99.dp` width & height across all interactive controls in `UltrasoundEditor`:
  `fillNormalScan`, `clearFindings`, `scanDateCalendarButton`, `scanDateTodayButton`, `applyScanGa`, `fetalNumberChoice-0`, `fetalNumberChoice-1`, `findingRow-presentation`, `findingRow-placenta`, `findingRow-liquor`, `toggleBiometry`, `impressionFromChoices`, `ultrasoundMoreToggle` (verifying height `>= 55.99.dp`), `removePhoto-0`, `captureScanPhotoCamera`, `addScanPhoto`, `linkVisit-${visit.id}`, `cancelUltrasound`, and `saveUltrasound`.
- `ultrasoundEditor_unlinkVisit_touchTarget_atLeast48dp`: Verifies `>= 47.99.dp` on `unlinkVisit`.
- `ultrasoundPhotoViewerDialog_touchTargets_atLeast48dp`: Verifies `>= 47.99.dp` on `closePhotoViewer`, `prevPhotoButton`, `nextPhotoButton`, and `dismissPhotoViewer`.
- `zoomableImage_resetButton_atLeast48dp_andResetsOnTap`: Verifies `>= 47.99.dp` on `zoomResetButton` in `ZoomableImage`.

### 8. ZoomableImage & UltrasoundPhotoViewerDialog
- `zoomableImage_resetButton_atLeast48dp_andResetsOnTap`: Verifies `zoomResetButton` is hidden at 1x, double-tapping image zooms to 2x displaying `zoomResetButton`, verifies `"1x"` action text style (`>= 12.sp`), verifies `>= 47.99.dp` touch bounds, and verifies clicking reset button returns scale to 1x and removes the reset chip.
- `ultrasoundPhotoViewerDialog_pager_navigatesBetweenPhotos`: Verifies pager at index 0 has `prevPhotoButton` disabled and `nextPhotoButton` enabled, clicking next advances subtitle to `"Photo 2 of 2"`, enables prev and disables next, and clicking prev navigates back.
- `ultrasoundPhotoViewerDialog_singlePhoto_hidesPagerControls`: Verifies pager navigation buttons are omitted when scan has only 1 photo, and subtitle displays `"Photo 1 of 1"`.
- `ultrasoundPhotoViewerDialog_360dp_fontScale2_0_scrollsAndDismissReachable`: Verifies dialog at 360dp with `fontScale = 2.0f` wraps biometrics into a 2-column grid, scrolls vertically, reaches `dismissPhotoViewer`, and clicking it invokes `onDismiss`.

### 9. 360dp Ergonomics, Scaling, & RTL / Dark Theme
- `ultrasoundEditor_360dp_fontScale1_3_noClipping`: Verifies `gaWeeks` and `gaDays` in side-by-side row, `SummaryRow` shows `"Edit"` / `"Choose"`, `assertAllTextWhole("ultrasoundLiveSummary")`, and no clipping at `1.3f` font scale.
- `ultrasoundEditor_360dp_fontScale2_0_noClipping`: Verifies `gaWeeks` and `gaDays` stack vertically with `12.dp` spacing, external labels `"GA weeks (0–45)"` and `"Estimated fetal weight (EFW)"` are whole, `"Take photo"` and `"Choose from gallery"` buttons are whole, `SummaryRow` omits trailing action text, and `saveUltrasound` is displayed after scrolling.
- `ultrasoundEditor_darkTheme_rtl_noOverlapOrClipping`: Verifies dark theme and RTL layout direction with Arabic patient name (`"منى رجب"`).
- `ultrasoundPhotoViewerDialog_darkTheme_rtl_rendersCleanly`: Verifies dark theme and RTL layout direction on `UltrasoundPhotoViewerDialog`.

## Verification Status
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: Exit code `0` (Success).
- `./gradlew --offline :app:testDebugUnitTest -q`: Exit code `0` (Success).
- Existing test suites (`UltrasoundGuidedFlowTest`, `AncWorkflowTest`, `AttachmentsRedesignTest`, etc.) remain intact and compile cleanly without modification.
- Total lines in `UltrasoundEditorRedesignTest.kt`: 1,092 (well under the 1,500-line ceiling).
