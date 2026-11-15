# Slice 12: Registration flow and patient editor

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s12_registration.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt` (`RecordType`, `RecordShapes`, `RecordSpacing`, `FieldFamilies`, `LocalFieldFamily`), `ui/components/RecordSectionCard.kt`, `ui/components/FormFieldLabel.kt`, `ui/components/StatusTag.kt`, `ui/components/PatientNameText.kt` (`isRtlText`), `ui/patienthub/components/EditorSheetParts.kt` (`SheetCancelSaveRow`). Read them; do not re-spec them.

- **Owner direction**: `antislop: DURING`, dials ENERGY 1 / RHYTHM 1 / MOTION 1 (pressed and focus states only, no new animation).
- **Branch hygiene**: run `.agents/scripts/git_audit.sh` before starting and before pushing; update `.agents/pipeline/HANDOFF_AGY.md` in the same commit on `redesign-2026-11-15`.
- **Never touch**: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `state/RegistrationFlow.kt`, `model/ClinicalModels.kt`, `*Generator*`, `ui/PatientEditorGroups.kt` (must stay untouched for `PatientEditorGroupsTest`), `RegistrationStepText.kt` string/glyph mappings (`RegistrationStepTextTest`), `flowMoveEvent` / `registrationNextEvent` in `RegistrationFlowScaffold.kt`, validation rules (`validationErrors()`), step navigation, draft and resume logic, and `EditorActions` defaults for later slices (Visits/Scans/Labs).
- **Level 3**: `ui/PatientEditor.kt` is level 3 (`clinical_safety`, `data_schema`). Needs a frontier-tier review and an independent test review before merge. Keep all existing `testTag`s, error triggers, draft field bindings, and save dispatch paths identical.
- **Visual only**: layout, typography, spacing, colour roles, borders, RTL text direction on name input, and component swap to design-system pieces. No schema, navigation, validation, or reducer changes.

## Decisions for the owner (this slice)

1. **Arabic name input alignment (`patientName`)**: Apply `isRtlText(draft.values.displayName)` from `PatientNameText.kt` so an Arabic name such as `منى رجب` starts from the **right edge** (`TextDirection.Rtl`, `TextAlign.Right`) while keeping the field label (`Full name (required)`) anchored at the UI locale start edge; English names stay flush left. **Recommended**: accept right-edge alignment via `isRtlText`.
2. **Step header sub-labels >= 12 sp**: Raise the segment glyph (`✓`, `−`, `○`), status word (`Done`, `Skipped`, `To do`), and `Current` marker from `11.sp` / `labelSmall` to `RecordType.Tag` (`12.sp`), keeping `WORDS_MAX_FONT_SCALE = 1.3f` (tested by `RegistrationFlowUiTest`). **Recommended**: accept `12.sp` with the `1.3f` threshold unchanged.
3. **Unencrypted photo storage notice**: Keep `R.string.photos_stored_on_phone_notice` (`"Photos are stored on this phone (not encrypted separately)"`), remove its misleading padlock `Lock` icon, and render it as `14.sp` `RecordType.Secondary` (`onSurfaceVariant`). **Recommended**: remove the padlock icon next to an unencrypted-storage notice.

## Lessons (apply all)

- Minimum `12.sp` on every text node across all densities (`PLAN.md` 0, 2). Every sub-12 sp style (`labelSmall`, `labelMedium`, hardcoded `11.sp`) in slice 12 must become at least `12.sp` (`RecordType.Tag` / `RecordType.FieldLabel`).
- Minimum `48.dp` width and height (`minimumTouchTarget()`) on every interactive target: footer buttons, standalone `SheetCancelSaveRow`, dropdown field triggers and icon buttons.
- Two corner radii only: `RecordShapes.Small` (`10.dp`) on controls, buttons, step bars, and text boxes; `RecordShapes.Large` (`16.dp`) on cards and banners. Replace `RoundedCornerShape(3.dp)` and M3 default pills / `4.dp` fields.
- Use `outline` (`#79747E` light / `#8E95A5` dark) on input borders (`4.55:1` / `5.42:1`) and `outlineVariant` (`#CAC4D0` / `#3A4459`) strictly for card hairlines and bar dividers (remove `.copy(alpha = 0.5f)` alpha dilution).
- All controls must fit within `360.dp` at `1.3f` AND wrap cleanly without clipping at `2.0f` font scale (`RegistrationFlowUiTest` tests footer buttons up to `2.0f`).
- Every existing `testTag` stays on the same semantic node with the exact spelling verified in `androidTest`. Note that age uses `patientAgeText` (not `patientAge`).

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Lines | Change |
|---|---:|---|
| `res/values/strings.xml` | 785 | Add `saving_label`; keep existing keys and values unchanged |
| `ui/components/RegistrationFlowScaffold.kt` | 60 | Ensure root background matches `MaterialTheme.colorScheme.background`; keep `flowMoveEvent`, `registrationNextEvent`, and IME / inset wiring untouched |
| `ui/components/RegistrationStepHeader.kt` | 170 | Restyle on `Surface` + bottom `HorizontalDivider(outlineVariant)`, `RecordType.SectionTitle` / `RecordType.Secondary`, `12.sp` `RecordType.Tag` segment labels, `RecordShapes.Small` segment bars with `>= 3:1` strokes |
| `ui/components/RegistrationStepFooter.kt` | 75 | Restyle with full-contrast top `HorizontalDivider`, `RecordShapes.Small` buttons, `RecordType.Action` text; keep `FlowRow` and `minimumTouchTarget()` |
| `ui/components/RegistrationStepText.kt` | 42 | Keep all title resources, status words, and Unicode glyphs untouched (`RegistrationStepTextTest`) |
| `ui/Components.kt` | 449 | Add backward-compatible optional styling params to `EditorField` (`externalLabel`, `shape`, `textStyle`) and `errorStyle: Boolean = false` on `GenericEditorErrorCard` so Slices 14 to 17 do not shift |
| `ui/PatientEditor.kt` (level 3) | 510 | Restyle `PatientEditor` and `PatientStatusEditor` using `RecordSectionCard`, `FormFieldLabel`, `RecordShapes.Small` inputs, `isRtlText` on `patientName`, `SheetCancelSaveRow` standalone, and `StatusTag` on status editor; keep `EditorActions` intact for later slices |
| `ui/PatientEditorGroups.kt` | 34 | Do not touch |

## 1. Strings (`app/src/main/res/values/strings.xml`)

Add only:
```xml
<string name="saving_label">Saving…</string>
```
Do not alter `registration_step_identity`, `registration_step_histories`, `registration_header`, `registration_status_skipped`, `registration_status_pending`, `registration_current`, `registration_segment_cd`, `registration_segment_current_cd`, `registration_pregnancy_progress`, `step_n_of_m`, `flow_skip`, `flow_finish`, `next_step`, `back_step`, `done`, `photos_stored_on_phone_notice`, `status_reason_notice`, `full_name_required`, `age_required`, or any error string.

## 2. `RegistrationStepHeader.kt` (same signature)

1. Wrap `RegistrationStepHeader` root `Column(modifier.fillMaxWidth().testTag("registrationStepHeader"))` in `Surface(color = MaterialTheme.colorScheme.surface)` followed by `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)` (drop no-surface background bleed).
2. Inside the header `Column`, use `Modifier.padding(horizontal = 16.dp, vertical = 12.dp)` (or `10.dp` vertical when `compact`), `verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp)`.
3. Step headline (`registrationHeaderText(flow)`): switch from `titleMedium` (`15.sp`) to `style = RecordType.SectionTitle` (`18.sp` Bold), `color = MaterialTheme.colorScheme.primary` (light `#0D1B3D`, dark `#B4C5FF`), keeping `Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite }`.
4. Pregnancy counter (`flow.current == RegistrationStep.PREGNANCIES && flow.pregnancyTarget != null`): switch from `bodyMedium` (`13.sp`) to `style = RecordType.Secondary` (`14.sp`), `color = MaterialTheme.colorScheme.onSurfaceVariant`.
5. Segment row (`!compact`): keep `WORDS_MAX_FONT_SCALE = 1.3f`, `Arrangement.spacedBy(4.dp)`, `testTag("registrationSegment-${index + 1}")`, and exact merged semantics (`contentDescription = description`, `selected = isCurrent`). Keep `animateProgressSpec` off (no new animation, MOTION 1).
6. `RegistrationSegment`:
   - `SegmentBar(status, isCurrent)`: keep height `8.dp` when `isCurrent`, `6.dp` otherwise, with `val shape = RecordShapes.Small` (`10.dp`, replaces `3.dp`). Colours:
     - `StepStatus.DONE`: solid `MaterialTheme.colorScheme.primary` (`16.92:1` on light surface, `9.59:1` dark).
     - `StepStatus.SKIPPED`: `1.5.dp` dashed stroke in `MaterialTheme.colorScheme.onSurfaceVariant` (`10.00:1` light, `9.47:1` dark), `dashPathEffect(floatArrayOf(6f, 4f))`.
     - `StepStatus.PENDING`: if `isCurrent`, fill `MaterialTheme.colorScheme.primaryContainer` + `1.5.dp` border `MaterialTheme.colorScheme.primary` (`12.78:1` light, `5.73:1` dark); else `1.5.dp` border `MaterialTheme.colorScheme.primary` on `surface`.
   - Below `SegmentBar`: raise glyph `Text(status.glyph())` from `labelSmall` (`11.sp`) to `style = RecordType.Tag`, keep `clearAndSetSemantics {}`.
   - When `showWords` (`fontScale <= 1.3f`):
     - Status word `Text(statusWord)`: raise `fontSize = 11.sp` to `style = RecordType.Tag.copy(fontWeight = FontWeight.Medium)`, `color = MaterialTheme.colorScheme.onSurfaceVariant`, `textAlign = TextAlign.Center`.
     - If `isCurrent`: `Text(stringResource(R.string.registration_current))` uses `style = RecordType.Tag.copy(fontWeight = FontWeight.Bold)`, `color = MaterialTheme.colorScheme.onSurface`, `textAlign = TextAlign.Center`.

## 3. `RegistrationStepFooter.kt` (same signature)

1. `Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface)`: replace `outlineVariant.copy(alpha = 0.5f)` with `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)`.
2. Keep `FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp))`.
3. Update all three buttons to `shape = RecordShapes.Small` and `RecordType.Action` text (`14.sp` SemiBold), keeping `minimumTouchTarget()` and all tags:
   - `OutlinedButton("flowBack")`: `shape = RecordShapes.Small`, `border = BorderStroke(1.dp, if (backEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant)`.
   - `Spacer(Modifier.weight(1f))`
   - `TextButton("flowSkip")` (when `showsSkip(flow)`): `shape = RecordShapes.Small`.
   - `Button("flowNext")`: `shape = RecordShapes.Small`, `colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)`.

## 4. Backward-compatible shared field helpers (`ui/Components.kt` and `ui/PatientEditor.kt`)

Do NOT break callers in Visits, Scans, Labs, or Medications (Slices 11 and 14 to 17). Add optional trailing parameters with defaults preserving current behaviour:

1. In `ui/Components.kt`:
   - `EditorField`: add optional trailing parameters:
     - `shape: Shape = OutlinedTextFieldDefaults.shape`
     - `textStyle: TextStyle = LocalTextStyle.current`
     - `externalLabel: Boolean = false`
     When `externalLabel == true`: wrap in `Column(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp))`; render `if (showError) Text(text = label, style = RecordType.FieldLabel, color = MaterialTheme.colorScheme.error) else FormFieldLabel(text = label)`. Keep `FormFieldLabel.kt` unchanged. On `OutlinedTextField`: omit floating `label`, set `shape = shape`, `textStyle = textStyle`, keep `supportingText` in `RecordType.Tag` (`12.sp`) when `showError`, `colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedBorderColor = MaterialTheme.colorScheme.primary, errorBorderColor = MaterialTheme.colorScheme.error)`, and place `Modifier.fillMaxWidth().testTag(tag)` directly on `OutlinedTextField` (not the outer `Column`, so Compose focus and text input semantics stay attached to the `TextField` node, identical to `DeliveryPlanSheet.PlanField`).
   - `GenericEditorErrorCard`: add `useRecordStyle: Boolean = false`. When `true`, render `OutlinedCard(shape = RecordShapes.Large, border = BorderStroke(1.dp, borderColor), colors = CardDefaults.outlinedCardColors(containerColor = bgColor), modifier = Modifier.fillMaxWidth().testTag("editorError"))` where errors/UNRESOLVED use `errorContainer` + `onErrorContainer` + `error` border (`13.26:1` light / `10.33:1` dark), and PENDING/RECONCILING use `surface` + `onSurface` + `outlineVariant` border, with `RecordType.Action` (`14.sp` SemiBold) padding `horizontal = 14.dp, vertical = 12.dp`.

2. In `ui/PatientEditor.kt`:
   - `EditorPage`: add `useRecordStyle: Boolean = false`. When `true`, `EditorPageHeader` uses `RecordType.HeaderName` (`22.sp` Bold, `color = MaterialTheme.colorScheme.onSurface`, keeping `.semantics { heading() }.testTag(tag)`) and subtitle uses `RecordType.Secondary` (`14.sp`, `color = MaterialTheme.colorScheme.onSurfaceVariant`); `Column` spacing changes from `14.dp` to `RecordSpacing.M` (`12.dp`) with bottom padding `16.dp` so `SheetCancelSaveRow` never kisses the system bar.
   - `ClinicalDropdownField`: add `shape: Shape = OutlinedTextFieldDefaults.shape`, `externalLabel: Boolean = false`. When `externalLabel == true`: place `FormFieldLabel(label)` above `OutlinedTextField(shape = shape, textStyle = RecordType.FieldValue, colors = same outline/surface colors)` inside `Column(spacedBy(4.dp))`, keep `testTag(tag)` on `OutlinedTextField` and `.minimumTouchTarget()` on the trailing dropdown `IconButton`. Keep `DropdownMenuItem` text `RecordType.FieldValue` (`16.sp`).

## 5. `PatientEditor` (level 3, `ui/PatientEditor.kt`)

Pass `useRecordStyle = true` to `EditorPage`, `GenericEditorErrorCard`, `EditorField`, and `ClinicalDropdownField` inside `PatientEditor`:

1. **Identity section (`IdentityFields`)**:
   - Replace private `EditorSectionCard(title)` with `RecordSectionCard(title = stringResource(R.string.identity_section_title), family = FieldFamilies.patient())` (`FieldPatientLabel` `#23407A` light / `#AFC6F2` dark).
   - **Full name (`patientName`) Arabic right-edge rule**:
     ```kotlin
     val isArabicName = isRtlText(draft.values.displayName)
     ```
     Pass:
     ```kotlin
     textStyle = RecordType.FieldValue.copy(
         textDirection = if (isArabicName) TextDirection.Rtl else TextDirection.Ltr,
         textAlign = if (isArabicName) TextAlign.Right else TextAlign.Left,
     )
     ```
     and `externalLabel = true`, `shape = RecordShapes.Small`. Keep `singleLine = true`, `imeAction = ImeAction.Next`, `error = PatientValidationError.FULL_NAME_REQUIRED in draft.errors`, `testTag("patientName")`.
   - `patientPhone` (`phone_optional`), `patientAgeText` (`age_required`, keep `errorMessage = stringResource(R.string.age_required_error)` when `AGE_REQUIRED`), `patientAddress` (`address`): all use `externalLabel = true`, `shape = RecordShapes.Small`, `textStyle = RecordType.FieldValue`.
2. **Photo storage notice (`PatientValidationError` / `PatientEditorGroup.IDENTITY in groups`)**:
   - Remove the `Row` with `Icons.Default.Lock` and `Spacer`; render `Text(text = stringResource(R.string.photos_stored_on_phone_notice), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`. (Notice `Lock` icon is removed, honest notice stays.)
3. **Obstetric section (`ObstetricFields`)**:
   - Replace `EditorSectionCard` with `RecordSectionCard(title = stringResource(R.string.obstetric_risk_section_title), family = FieldFamilies.obstetric())` (`FieldObstetricLabel` `#1D5149` light / `#9FD6CC` dark).
   - `G / P / A` inputs (`patientGravida`, `patientParity`, `patientAbortion`):
     - At `LocalDensity.current.fontScale <= 1.3f`: 3-column `Row(horizontalArrangement = Arrangement.spacedBy(8.dp))` with `Modifier.weight(1f)`.
     - Above `1.3f` (e.g. `2.0f` accessibility zoom): stack vertically in `Column(verticalArrangement = Arrangement.spacedBy(8.dp))` so `"Gravida (G)"`, `"Parity (P)"`, `"Abortion (A)"` never wrap to 3 cramped lines or clip inside `90.dp`.
     - Note: keep `gpaError` (`GPA_ALL_OR_NONE` or `INVALID_GPA`) visual highlighting (`isError = gpaError`) on each box.
   - `patientObstetricNotes`, `riskDropdown`, `bloodGroupDropdown`, `patientClinicalAttention`: all use `externalLabel = true`, `shape = RecordShapes.Small`, `textStyle = RecordType.FieldValue`. Note `"Clinical attention and high-risk alerts (one alert per line)"` now renders above the box at `FormFieldLabel` (`12.sp` SemiBold) with full wrapping and zero notch truncation.
4. **Standalone actions (`flow == null`)**:
   - Replace `EditorActions(enabled = enabled, saveTag = "savePatient", ...)` with `SheetCancelSaveRow(enabled = enabled, saving = state.savePhase == SavePhase.PENDING, saveEnabled = enabled, savingLabel = stringResource(if (state.savePhase == SavePhase.PENDING) R.string.saving_label else R.string.save), onCancel = { onEvent(AncEvent.CancelEditor) }, onSave = { onEvent(AncEvent.SubmitPatientProfile) }, cancelTag = "cancelPatient", saveTag = "savePatient", spinnerTag = "patientSaveSpinner")`.
   - Keep `EditorActions` helper untouched in `PatientEditor.kt` for later slices (Visits/Scans/Labs). Keep private `EditorSectionCard` only if other callers exist; delete if unused after verifying `rg`.

## 6. `PatientStatusEditor` (`ui/PatientEditor.kt`, same signature)

1. Use `EditorPage(..., tag = "patientStatusEditor", subtitle = patient.displayName, useRecordStyle = true)`.
2. `GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)`.
3. Wrap the body in `RecordSectionCard(title = stringResource(R.string.patient_status), family = FieldFamilies.patient(), action = if (route.targetStatus == "archived") ({ StatusTag(kind = StatusKind.Incomplete, text = stringResource(R.string.archived), modifier = Modifier.padding(end = 8.dp)) }) else null)`. When unarchiving (`route.targetStatus != "archived"`), render `Text(text = stringResource(R.string.unarchive_patient_subtitle), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)` above the notice without extra tag noise.
4. Replace bare `Text(status_reason_notice, color = tertiary)` with `Text(text = stringResource(R.string.status_reason_notice), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`.
5. `EditorField(..., tag = "statusReason", label = stringResource(R.string.reason_required), externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`.
6. Replace `EditorActions(..., saveTag = "savePatientStatus", ...)` with `SheetCancelSaveRow(enabled = enabled, saving = state.savePhase == SavePhase.PENDING, saveEnabled = enabled, savingLabel = stringResource(if (state.savePhase == SavePhase.PENDING) R.string.saving_label else R.string.save), onCancel = { onEvent(AncEvent.CancelEditor) }, onSave = { onEvent(AncEvent.SubmitPatientStatus) }, cancelTag = "cancelPatientStatus", saveTag = "savePatientStatus", spinnerTag = "patientStatusSaveSpinner")`.

## Acceptance checklist

- Every `testTag` kept on its exact node: `registrationStepHeader`, `registrationSegment-1`..`5`, `flowBack`, `flowSkip`, `flowNext`, `patientEditor`, `editorError`, `patientName`, `patientPhone`, `patientAgeText`, `patientAddress`, `patientGravida`, `patientParity`, `patientAbortion`, `patientObstetricNotes`, `riskDropdown`, `bloodGroupDropdown`, `patientClinicalAttention`, `savePatient`, `patientStatusEditor`, `statusReason`, `savePatientStatus`.
- Zero text nodes under `12.sp` in `RegistrationStepHeader`, `RegistrationStepFooter`, `PatientEditor`, and `PatientStatusEditor`.
- Arabic input `"منى رجب"` in `patientName` starts from the right edge (`TextAlign.Right`, `TextDirection.Rtl`) while `"Mona Ragab"` starts from the left edge, and `"Full name (required)"` label stays at start.
- All step footer buttons, standalone `SheetCancelSaveRow` buttons, and dropdown icon triggers have height `>= 48.dp` and width `>= 48.dp` at `1.0f`, `1.3f`, and `2.0f` on a `360.dp` width.
- Every corner radius in Slice 12 screens is `10.dp` (`RecordShapes.Small`) or `16.dp` (`RecordShapes.Large`); `3.dp` segment bars are gone.
- Padlock `Lock` icon next to `photos_stored_on_phone_notice` is gone; the honest unencrypted-photos notice remains readable at `14.sp`.
- `RegistrationFlowUiTest` (all 8 tests), `RegistrationPencilUiTest`, `RegistrationStepTextTest`, `PatientEditorGroupsTest`, `IsRtlTextTest`, and existing validation/save suites pass unchanged.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. Double check `AncWorkflowTest` and `DraftRecoveryDialogTest` if any assertion queries the old `EditorActions` Cancel button by shape/class; none match Cancel by text-less tag today.

### Must pass unchanged

- `app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationFlowUiTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationPencilUiTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/components/RegistrationStepTextTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/PatientEditorGroupsTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/components/IsRtlTextTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/NegativeControlAndClinicalValidationTest.kt`

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationRedesignTest.kt`

Mount through `MainActivity` + `AncApp` + `AncTheme` inside a `360.dp x 640.dp` host (matching `RegistrationFlowUiTest` and `EditorSheetsRedesignTest`):
- Walk semantics trees of Step 1 (`IDENTITY`), Step 3 (`OBSTETRIC`), standalone `PatientEditor`, and `PatientStatusEditor` at `1.0f` and `1.3f` font scale: assert zero `TextLayoutResult` nodes have `fontSize < 12.sp`.
- In `PatientEditor`: type `"منى رجب"` into `patientName` and assert the text layout paragraph resolves RTL / right-edge aligned (`getLineRight` near field content end, `paragraphDirection == Rtl`); type `"Mona Ragab"` and assert left-edge LTR alignment (`getLineLeft` near content start).
- Verify all `testTag`s listed in the Acceptance checklist exist and are not clipped on `360.dp` at `1.0f`, `1.3f`, and `2.0f` font scale (with `G/P/A` stacking cleanly above `1.3f` and `FlowRow` wrapping footer buttons).
- Verify `flowBack`, `flowSkip`, `flowNext`, `savePatient`, `cancelPatient`, `savePatientStatus`, and `cancelPatientStatus` meet `>= 48.dp` height and width (`>= 47.9.dp`), and standalone `cancelPatient` and `savePatient` share equal width (`<= 1.dp` difference).
- Verify `"Photos are stored on this phone (not encrypted separately)"` is displayed and no `Lock` image/contentDescription appears beside it.
- Mount Step 1 and `PatientStatusEditor` under `darkTheme = true` and `LayoutDirection.Rtl` to verify dark tokens and RTL mirroring without overlap.

## Risk

**Medium-high (`PatientEditor.kt` is Level 3).** Changing field layout wrappers or moving `.testTag(tag)` between `Column` and `OutlinedTextField` can silently break `performTextInput`, `performClick` IME trigger (`RegistrationFlowUiTest.segmentRowHidesWhileTheKeyboardShowsButTitleAndNextStay`), or focus traversal (`imeAction = ImeAction.Next`). Always keep `.testTag(tag)` on the `OutlinedTextField` itself, leave `AncReducer` / `PatientDraft` validation and `patientEditorGroups` untouched, and require both frontier review and independent test review before merge.

## Owner decisions (2026-11-15): mocks s12 and s13 approved, all five decisions accepted as recommended
1 Arabic name starts from the right edge (isRtlText, TextDirection.Rtl, TextAlign.Right); 2 step header sub-labels 12sp with WORDS_MAX_FONT_SCALE 1.3 cap kept; 3 padlock icon removed from the photos notice and the three 11sp vault footers removed (slice 13 files); 4 human labels on PreviousPregnancyEditor chips with canonical stored values unchanged (slice 13); 5 dating preview tiles, hidden when not computable (slice 13).
