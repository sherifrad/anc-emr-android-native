Created [`RegistrationRedesignTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationRedesignTest.kt) to test redesign slice 12 (registration flow and patient editor).

### Tests Written (29 test cases)

1. **Step Header**:
   - `stepHeader_title18sp_counter14sp_segments12sp_surfaceAndDivider`: Verifies step headline is `18.sp` Bold primary (`RecordType.SectionTitle`), pregnancy counter is `14.sp` Secondary (`RecordType.Secondary`), status words ("Done", "Skipped") and "Current" marker are `12.sp` (`RecordType.Tag`), and header sits on a `Surface` with divider.
   - `stepHeader_wordsVisibleAtFontScale1_3`: Verifies status words and "Current" marker are visible at font scale 1.3, and `registrationSegment-N` carries `contentDescription` and `selected` semantics.
   - `stepHeader_wordsHiddenAtFontScale1_4_segmentsKeepContentDescriptionAndSelected`: Verifies status words and "Current" text nodes are dropped from the header at font scale 1.4 while `registrationSegment-N` preserves `contentDescription` and `selected` semantics.
   - `stepHeader_segmentBarHeights_current8dp_other6dp`: Verifies at font scale 1.4 (where words are omitted) that the current segment bar is 8dp tall and other bars are 6dp tall (2dp height delta).

2. **Footer Buttons**:
   - `stepFooter_buttonsMeet48dpMinTouchTarget`: Verifies `flowBack`, `flowSkip`, and `flowNext` all meet the `>= 47.99dp` touch target minimum in height and width, with `14.sp` SemiBold (`RecordType.Action`) text.
   - `stepFooter_buttonsWrapAtFontScale2_0_withoutClipping`: Verifies buttons wrap cleanly in `FlowRow` at font scale 2.0 on 360dp width without text clipping.

3. **Identity Step**:
   - `identity_externalLabels12sp_fieldsAcceptInput`: Verifies external `FormFieldLabel` (12sp SemiBold `RecordType.FieldLabel`) sits above each field (`patientName`, `patientPhone`, `patientAgeText`, `patientAddress`), fields accept input via `performTextInput` / `performTextReplacement`, and the draft/state values are updated.

4. **Arabic vs English Name Alignment**:
   - `identity_arabicName_alignedRightEdge_preservesExactString`: Types Arabic `"منى رجب"` into `patientName`, verifying the exact string is stored in state, the text style resolves `TextDirection.Rtl` and `TextAlign.Right`, paragraph direction is `ResolvedTextDirection.Rtl`, and the line right edge aligns near the layout content width.
   - `identity_englishName_alignedLeftEdge`: Types English `"Mona Ragab"`, verifying `TextDirection.Ltr`, `TextAlign.Left`, `ResolvedTextDirection.Ltr`, and left-edge alignment near 0.

5. **Obstetric Step GPA**:
   - `obstetric_gpaFieldsInOneRow_atFontScale1_0`: Verifies `patientGravida`, `patientParity`, and `patientAbortion` are arranged horizontally in one row at font scale 1.0 and accept edits.
   - `obstetric_gpaFieldsStacked_aboveFontScale1_3`: Verifies all three GPA fields stack vertically in a column above font scale 1.3 (at 1.4) and accept edits.

6. **Photos Storage Notice**:
   - `photosNotice_textAt14sp_andNoLockIcon`: Verifies notice `"Photos are stored on this phone (not encrypted separately)"` is rendered at `14.sp` `RecordType.Secondary` (`onSurfaceVariant`) and no padlock lock icon or lock description is present.

7. **PatientEditor Standalone**:
   - `patientEditorStandalone_cancelAndSaveButtons_equalWidth_emitsSubmit`: Verifies `cancelPatient` and `savePatient` meet `>= 47.99dp`, share equal width in `SheetCancelSaveRow`, and emit `AncEvent.SubmitPatientProfile` and `AncEvent.CancelEditor`.
   - `patientEditorStandalone_pendingSave_disablesButtons_showsSavingLabelAndSpinner`: Verifies during `SavePhase.PENDING`, Save and Cancel are disabled, the save button shows `'Saving…'` (`R.string.saving_label`), and `patientSaveSpinner` is displayed.

8. **PatientStatusEditor**:
   - `patientStatusEditor_archive_showsStatusTag_cancelAndSave_emitsEvents`: Verifies `StatusTag` with `"Archived"` (`12.sp`), `statusReason` input, touch targets `>= 47.99dp`, and emission of `SubmitPatientStatus` / `CancelEditor`.
   - `patientStatusEditor_archiveReasonRequired_showsErrorCardAndFieldError`: Verifies `editorError` card is displayed when `ARCHIVE_REASON_REQUIRED` error is present.

9. **Error Card**:
   - `errorCard_stylingDoesNotHideMessage`: Verifies `editorError` card displays `"Please correct highlighted fields before saving."` at `14.sp` SemiBold (`RecordType.Action`) without horizontal or vertical clipping.

10. **Font Scale Floor (>= 12sp)**:
    - `noTextBelow12sp_step1_identity_fontScale1_0`
    - `noTextBelow12sp_step1_identity_fontScale1_3`
    - `noTextBelow12sp_step3_obstetric_fontScale1_0`
    - `noTextBelow12sp_step3_obstetric_fontScale1_3`
    - `noTextBelow12sp_standalonePatientEditor_fontScale1_0`
    - `noTextBelow12sp_standalonePatientEditor_fontScale1_3`
    - `noTextBelow12sp_patientStatusEditor_fontScale1_0`
    - `noTextBelow12sp_patientStatusEditor_fontScale1_3`: Walks all semantics text and editable text nodes across Step 1, Step 3, standalone PatientEditor, and PatientStatusEditor at both 1.0 and 1.3 font scale, asserting zero nodes have `fontSize < 12.sp`.

11. **360dp Whole Text & RTL Mirror**:
    - `stepHeaderAndFooter_360dp_fontScale1_3_allTextWhole`
    - `stepHeaderAndFooter_360dp_fontScale2_0_allTextWhole`: Confirms no ellipsis or clipping under narrow 360dp at 1.3 and 2.0 text zoom.
    - `identity_rtlMirror_darkTheme_noClipping`: Tests Step 1 under `darkTheme = true` and `LayoutDirection.Rtl`.
    - `patientStatusEditor_rtlMirror_darkTheme_noClipping`: Tests PatientStatusEditor under `darkTheme = true` and `LayoutDirection.Rtl`.

---

### Coverage Caveats & Notes

1. **Segment glyph text node resolution**: In `RegistrationStepHeader.kt`, `Text(text = status.glyph(), modifier = Modifier.clearAndSetSemantics {})` deliberately clears all semantics on the raw Unicode glyph node so that screen readers do not verbalize raw symbol characters (`✓`, `−`, `○`) and instead read the parent segment's merged `contentDescription` (`"Identity, Done"`). Because of `clearAndSetSemantics {}`, the glyph node cannot be queried for a `TextLayoutResult` via semantics actions. Glyph mappings are verified via `RegistrationStepTextTest` and visual height contributions are verified via segment bar height testing.
2. **SegmentBar direct testTag**: `SegmentBar` does not carry its own `testTag`; its dimensions were tested via segment height deltas on `registrationSegment-N` at font scale 1.4 where text words are suppressed.

Compilation verified with `./gradlew --offline :app:compileDebugAndroidTestKotlin -q` (clean build with exit code 0).
