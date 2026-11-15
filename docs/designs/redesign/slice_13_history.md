# Slice 13: History and dating editors

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s13_history.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt` (`RecordType`, `RecordShapes`, `RecordSpacing`, `FieldFamilies`, `LocalFieldFamily`), `ui/components/RecordSectionCard.kt`, `ui/components/FormFieldLabel.kt`, `ui/components/RecordFilterChip.kt`, `ui/components/StatusTag.kt` (`StatusWord`), `ui/patienthub/components/EditorSheetParts.kt` (`SheetCancelSaveRow`), and Slice 12 optional params on `EditorPage`, `EditorField`, `GenericEditorErrorCard`, `ClinicalDropdownField` (if Slice 12 has not landed, add those exact optional parameters and note it in the PR). Read them; do not re-spec them.

- **Owner direction**: `antislop: DURING`, dials ENERGY 1 / RHYTHM 1 / MOTION 1 (pressed and focus states only, no new animation).
- **Branch hygiene**: run `.agents/scripts/git_audit.sh` before starting and before pushing; update `.agents/pipeline/HANDOFF_AGY.md` in the same commit on `redesign-2026-11-15`.
- **Never touch**: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `state/ClinicalDraftReducers.kt`, `state/DatingDraftRule.kt` (`applyDatingChange`, `datingDraftToDating`, `isManualAgreedEdd`), `model/ClinicalModels.kt`, `model/PreviousPregnancyFormRules.kt` (`hasDeliveryMode`, `deliveryColumns`, `modeOf`, `clinicianIdFor`, `placeAndDoctorRequired`), `model/GestationalAgeEngine.kt` (`calculateEstimatedDueDate`, `calculate`, `dueDateForMethod`), `*Generator*`, `ui/DatingFieldVisibility.kt` truth table (`DatingFieldVisibilityTest`), existing history/outcome/delivery stored values, and `EditorActions` defaults for later slices (Visits/Scans/Labs).
- **Level 3**: `ui/PreviousPregnancyEditor.kt` and `ui/DatingEditor.kt` are both level 3 (`clinical_safety`, `data_schema`). Both need a frontier-tier review and an independent test review before merge. Keep validation, conditional field rules, save paths, draft clearing on outcome switches, dating EDD auto-following, and all existing `testTag`s identical.
- **Visual only**: layout, typography, field-family colour roles, chips, labels, date/GA formatting on preview tiles, and standalone action bars. No schema, calculation, validation, or reducer changes.

## Decisions for the owner (this slice)

1. **Human clinical labels on `PreviousPregnancyEditor` chips**: Map `ChoiceGroup` display labels in `PreviousPregnancyEditor` to existing `R.string.outcome_*` strings (`"Live birth"`, `"Stillbirth / IUFD"`, `"Early pregnancy loss"`, `"Termination of pregnancy"`) and `"Cesarean"` for `"CS"`, while dispatching the canonical stored codes (`LiveBirth`, `Stillbirth`, `EarlyPregnancyLoss`, `TerminationOfPregnancy`, `CS`) unchanged to state and database. **Recommended**: accept human display labels with canonical stored codes unchanged.
2. **`DatingEditor` calculated preview card and empty visibility**: Replace Card 2 (`"2. Dating Method & GA Calculation"`, nested `12.dp` `primaryContainer` box showing raw ISO dates and `27w 2d`) with a calm `RecordSectionCard` (`FieldFamilies.obstetric()`) holding two flat `FieldObstetricBg` (`#E3F0EE` light / `#1D3836` dark) tiles formatted via `formatDmy(calculatedEdd)` (`"11/02/2027"`) and `calculatedGa.shortWeeksText` (`"27+2 wks"`), and hide that card completely when both `calculatedEdd` and `calculatedGa` are null. **Recommended**: hide when empty and align formats with Overview and Plan.
3. **Remove repetitive `11.sp` vault Lock footers** (shared with Slice 12 Decision 3): Drop the decorative padlock `Lock` rows (`"AES-256 GCM · Local Vault Security"` / `"Tamper-Evident Local Storage"`) at the bottom of `OtherHistoriesEditor`, `PreviousPregnancyEditor`, and `DatingEditor`.

## Lessons (apply all)

- Minimum `12.sp` everywhere (`PLAN.md` 0, 2): eliminate `labelMedium` (`11.sp`) in `QuickPresetChips` and `DatingEditor` (`embryo_age`), and eliminate `labelSmall` (`11.sp`) Lock footers across all three editors.
- Minimum `48.dp` width and height (`minimumTouchTarget()`) on every interactive target: add explicit `.minimumTouchTarget()` on `fillAllNegativeHistories`, all `QuickPresetChips`, all `ChoiceGroup` chips, `embryoDay3` / `embryoDay5` / `embryoDay6`, `ClinicalDateField` calendar `IconButton` and today `FilterChip`, and `SheetCancelSaveRow`.
- Two corner radii only: `RecordShapes.Small` (`10.dp`) on controls, chips, buttons, metric tiles, and text boxes; `RecordShapes.Large` (`16.dp`) on cards and banners. Replace `12.dp` box-inside-box in `DatingEditor` Card 2 and `8.dp` M3 chips.
- Remove hardcoded English card prefixes (`"1. "`, `"2. "`, `"3. "`, `"4. "`), hardcoded English subtitles with `" · "` middle dots, and `"✓ "` decorative glyph in `fillAllNegativeHistories` label.
- Prevent `360.dp` clipping at `1.3f` and `2.0f` font scale: keep `FlowRow` on chip groups and allow multi-line wrap inside long preset chips (`"No significant medical history"`, `"Previous Cesarean Section"`, `"Family history of Hypertension"`) when `2.0f` text widens beyond single line.
- Keep every `testTag` on the exact semantic node asserted by tests (`negativeHistoriesCard`, `fillAllNegativeHistories`, `medicalHistory`, `medHistoryPreset-$index`, `pregnancyOutcome-$index`, `deliveryBy-$index`, `datingMethodDropdown`, `lmpDate`, `ultrasoundEdd`, `ivfTransferDate`, `embryoDay3`/`5`/`6`, `agreedEdd`, `datingNotes`, `saveHistories`, `savePregnancy`, `saveDating`, `editorError`).

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Lines | Change |
|---|---:|---|
| `res/values/strings.xml` | 785 | Add `dating_baseline_section`, `dating_calculated_section`, `delivery_mode_vaginal`, `delivery_mode_cesarean`; normalize `negative_histories_title` to sentence case (`"Explicit negative histories"`); add `saving_label` if Slice 12 has not landed |
| `ui/AncDatePicker.kt` | 159 | Add backward-compatible `externalLabel: Boolean = false`, `shape: Shape = OutlinedTextFieldDefaults.shape`, `textStyle: TextStyle = LocalTextStyle.current` on `ClinicalDateField`; enforce `.minimumTouchTarget()` on `${tag}CalendarButton` when styled and apply `RecordShapes.Small` + `RecordType.Action` to `${tag}TodayButton` |
| `ui/ClinicalPresets.kt` | 121 | Add `useRecordStyle: Boolean = false` on `QuickPresetChips` using `FormFieldLabel` (`12.sp`) and `10.dp` `48.dp`-target filter chips |
| `ui/PatientEditor.kt` (shared helper section only) | 510 | Add optional `labelOf: (String) -> String = { it }` and `useRecordChip: Boolean = false` to `ChoiceGroup`; keep all existing `MedicationForm` / Visit / Scan / Lab callers working identically |
| `ui/OtherHistoriesEditor.kt` | 254 | Restyle with `FieldFamilies.background()`, `RecordSectionCard`, clean subtitles, `negativeHistoriesCard` banner without `"✓ "` prefix, `48.dp` `fillAllNegativeHistories`, `SheetCancelSaveRow` standalone, drop Lock footer |
| `ui/PreviousPregnancyEditor.kt` (level 3) | 241 | Restyle with `FieldFamilies.obstetric()`, `RecordSectionCard`, `FormFieldLabel`, mapped human labels on `ChoiceGroup` keeping stored codes intact, `RecordType.Secondary` notes/errors, `SheetCancelSaveRow` standalone, drop Lock footer |
| `ui/DatingEditor.kt` (level 3) | 338 | Restyle with `FieldFamilies.obstetric()`, `RecordSectionCard`, localized titles (no numbers), `FormFieldLabel` + `RecordShapes.Small` chips on `embryoDay3/5/6`, flat `FieldObstetricBg` tiles (`11/02/2027` + `27+2 wks`) hidden when empty, `SheetCancelSaveRow` standalone, drop Lock footer |
| `ui/DatingFieldVisibility.kt` | 24 | Do not touch (`DatingFieldVisibilityTest`) |

## 1. Strings (`app/src/main/res/values/strings.xml`)

Change value (key unchanged):
- `negative_histories_title`: `"Explicit Negative Histories"` -> `"Explicit negative histories"`

Add:
```xml
<string name="dating_baseline_section">Baseline dating</string>
<string name="dating_calculated_section">Calculated dating</string>
<string name="delivery_mode_vaginal">Vaginal</string>
<string name="delivery_mode_cesarean">Cesarean</string>
```
If Slice 12 has not landed, also add `<string name="saving_label">Saving…</string>`.

## 2. Backward-compatible helper extensions (`AncDatePicker.kt`, `ClinicalPresets.kt`, `ChoiceGroup`)

1. **`ClinicalDateField` (`ui/AncDatePicker.kt`)**:
   - Add optional trailing params: `externalLabel: Boolean = false`, `shape: Shape = OutlinedTextFieldDefaults.shape`, `textStyle: TextStyle = LocalTextStyle.current`, `useRecordStyle: Boolean = false`.
   - When `externalLabel == true`: wrap field in `Column(spacedBy(4.dp))` with `FormFieldLabel(label)` (or `error`-tinted `RecordType.FieldLabel` when `error == true`) above `OutlinedTextField` (`shape = if (useRecordStyle) RecordShapes.Small else shape`, `textStyle = if (useRecordStyle) RecordType.FieldValue else textStyle`, border colours `outline` / `primary` / `error`, surface fill). Keep `Modifier.fillMaxWidth().testTag(tag)` on `OutlinedTextField`.
   - When `useRecordStyle == true`: add `.minimumTouchTarget()` to `${tag}CalendarButton`, and style `${tag}TodayButton` `FilterChip` with `shape = RecordShapes.Small`, `FilterChipDefaults.filterChipColors(containerColor = surface, labelColor = onSurface)`, `border = BorderStroke(1.dp, outline)`, and `Text(..., style = RecordType.Action)` (`14.sp`).
2. **`QuickPresetChips` (`ui/ClinicalPresets.kt`)**:
   - Add `useRecordStyle: Boolean = false`.
   - When `useRecordStyle == true`:
     - Replace `Text(title, style = MaterialTheme.typography.labelMedium, color = primary)` (`11.sp`) with `FormFieldLabel(title)`.
     - Replace each `FilterChip` with a `FilterChip` styled like `RecordFilterChip`: `shape = RecordShapes.Small`, `colors = FilterChipDefaults.filterChipColors(containerColor = surface, labelColor = onSurface)`, `border = FilterChipDefaults.filterChipBorder(enabled = enabled, selected = false, borderColor = outline)`, `label = { Text(text = item, style = RecordType.Action.copy(fontWeight = FontWeight.Medium)) }` (allowing natural wrap above `1.5f` scale so long medical/surgical preset strings do not clip on `360.dp`), and `modifier = Modifier.minimumTouchTarget().testTag("$tagPrefix-$index")`.
3. **`ChoiceGroup` (`ui/PatientEditor.kt`)**:
   - Add optional trailing params: `labelOf: (String) -> String = { it }`, `useRecordChip: Boolean = false`.
   - Pass `labelOf(choice)` to chip display text while keeping `onSelected(choice)` passing the raw stored choice unchanged.
   - When `useRecordChip == true`: use `RecordShapes.Small`, `RecordType.Action`, selected fill `primary` / `onPrimary`, unselected border `outline`, `minimumTouchTarget().testTag("$tagPrefix-$index")` (wrap enabled for `Termination of pregnancy` / `Stillbirth / IUFD` at `2.0f` scale).

## 3. `OtherHistoriesEditor` (`ui/OtherHistoriesEditor.kt`, same signature)

1. `EditorPage(title = stringResource(R.string.edit_other_histories), subtitle = patient.displayName, tag = "otherHistoriesEditor", showHeader = !inFlow, useRecordStyle = true)`.
2. `GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)`.
3. **`negativeHistoriesCard`**:
   - Replace translucent `primaryContainer.copy(alpha = 0.45f)` card with `OutlinedCard(modifier = Modifier.fillMaxWidth().testTag("negativeHistoriesCard"), shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = FieldFamilies.background().container), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))`.
   - Title: `Text(stringResource(R.string.negative_histories_title), style = RecordType.CardName, color = FieldFamilies.background().label)`.
   - Button: `OutlinedButton(onClick = { ... }, enabled = enabled, shape = RecordShapes.Small, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("fillAllNegativeHistories"))` with `Text(text = stringResource(R.string.fill_all_negative), style = RecordType.Action)` (drop `"✓ "` prefix; keep exact click side effects setting all four fields!).
4. Replace the four numbered elevated cards (`"1. "`, `"2. "`, `"3. "`, `"4. "`) with four `RecordSectionCard(title = stringResource(...), family = FieldFamilies.background())`:
   - `R.string.medical_history`, `R.string.surgical_history`, `R.string.family_history`, `R.string.allergy_history`.
   - Inside each card:
     - `EditorField` (`externalLabel = false` with no floating label when `label.isEmpty()`, `shape = RecordShapes.Small`, `textStyle = RecordType.FieldValue`, `modifier = Modifier.semantics { contentDescription = sectionTitle }` on the `OutlinedTextField`) so the `RecordSectionCard` heading provides the single visible label without repeating `"Medical history"` twice inside the card. Keep `.testTag("medicalHistory")` / `"surgicalHistory"` / `"familyHistory"` / `"allergyHistory"` on the `OutlinedTextField`, and preserve `imeAction` and focus traversal (`medFocus -> surgFocus -> famFocus -> allergyFocus`).
     - `QuickPresetChips(..., tagPrefix = "medHistoryPreset" / "surgHistoryPreset" / "famHistoryPreset" / "allergyHistoryPreset", useRecordStyle = true)`.
5. Delete the bottom `Icons.Default.Lock` row and hardcoded `"Medical and surgical histories encrypted locally..."` text.
6. When `!inFlow`: replace `EditorActions` with `SheetCancelSaveRow(enabled = enabled, saving = state.savePhase == SavePhase.PENDING, saveEnabled = enabled, savingLabel = stringResource(if (state.savePhase == SavePhase.PENDING) R.string.saving_label else R.string.save), onCancel = { onEvent(AncEvent.CancelEditor) }, onSave = { onEvent(AncEvent.SubmitOtherHistories) }, cancelTag = "cancelHistories", saveTag = "saveHistories", spinnerTag = "historiesSaveSpinner")`.

## 4. `PreviousPregnancyEditor` (level 3, `ui/PreviousPregnancyEditor.kt`, same signature)

1. `EditorPage(title = stringResource(if (isEdit) R.string.correct_previous_pregnancy else R.string.add_previous_pregnancy), subtitle = patient.displayName, tag = "previousPregnancyEditor", showHeader = !inFlow, useRecordStyle = true)`.
2. `GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)`.
3. **Card 1**: `RecordSectionCard(title = stringResource(R.string.pregnancy_core_facts_title), family = FieldFamilies.obstetric())`:
   - `ClinicalDateField(..., tag = "pregnancyDateText", showTodayButton = false, externalLabel = true, useRecordStyle = true)` (error mapping for `DELIVERY_DATE_INVALID` / `DELIVERY_DATE_IN_FUTURE` / `DELIVERY_DATE_BEFORE_DATING` unchanged).
   - `ChoiceGroup` (`pregnancyOutcome`): pass `labelStyle = RecordType.FieldLabel`, `labelColor = FieldFamilies.obstetric().label`, `useRecordChip = true`, and `labelOf = { outcome -> when (outcome) { "LiveBirth" -> liveBirthLabel; "Stillbirth" -> stillbirthLabel; "EarlyPregnancyLoss" -> earlyLossLabel; "TerminationOfPregnancy" -> terminationLabel; else -> outcome } }`. Keep `tagPrefix = "pregnancyOutcome"` and raw `outcome` passed to `AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, it)`.
   - Conditional helper note (`earlier_outcome_note`) & `stored_delivery_mode`: change `bodySmall` to `RecordType.Secondary`, `color = MaterialTheme.colorScheme.onSurfaceVariant`.
   - Conditional `Delivery mode` (`deliveryBy`, when `PreviousPregnancyFormRules.hasDeliveryMode(draft.values.outcome)`): `ChoiceGroup` with `labelStyle = RecordType.FieldLabel`, `labelColor = FieldFamilies.obstetric().label`, `useRecordChip = true`, `labelOf = { mode -> when (mode) { PreviousPregnancyFormRules.MODE_CS -> cesareanLabel; PreviousPregnancyFormRules.MODE_VAGINAL -> vaginalLabel; else -> mode } }`.
   - Conditional `Loss management` (`lossManagement`, when `draft.values.outcome == "EarlyPregnancyLoss"`) and `Termination method` (`terminationMethod`, when `draft.values.outcome == "TerminationOfPregnancy"`): `ChoiceGroup` with `labelStyle = RecordType.FieldLabel`, `labelColor = FieldFamilies.obstetric().label`, `useRecordChip = true`.
   - Conditional `OUTCOME_METHOD_MISMATCH` error: `RecordType.Action`, `color = MaterialTheme.colorScheme.error`.
4. **Card 2**: `RecordSectionCard(title = stringResource(R.string.pregnancy_details_title), family = FieldFamilies.obstetric())`:
   - `EditorField` (`pregnancyPlaceDoctor`): `externalLabel = true`, `shape = RecordShapes.Small`, `textStyle = RecordType.FieldValue`, error mapping `PLACE_AND_DOCTOR_REQUIRED` unchanged.
   - `ChoiceGroup` (`pregnancyType`): `labelStyle = RecordType.FieldLabel`, `labelColor = FieldFamilies.obstetric().label`, `useRecordChip = true`.
   - `EditorField` (`pregnancyOtherDetails`): `externalLabel = true`, `shape = RecordShapes.Small`, `textStyle = RecordType.FieldValue`.
5. Delete the bottom `Icons.Default.Lock` row and hardcoded `"Previous pregnancy records encrypted and digitally signed..."`.
6. When `!inFlow`: replace `EditorActions` with `SheetCancelSaveRow(..., onCancel = { onEvent(AncEvent.CancelEditor) }, onSave = { onEvent(AncEvent.SubmitPreviousPregnancy) }, cancelTag = "cancelPregnancy", saveTag = "savePregnancy", spinnerTag = "pregnancySaveSpinner")`.

## 5. `DatingEditor` (level 3, `ui/DatingEditor.kt`) & `DatingFieldVisibility.kt`

Keep `datingFieldVisibility(draft.values.datingMethod)`, `GestationalAgeEngine` calls, `applyDatingChange` draft sync, `lmpFocus`/`scanFocus`/`ivfFocus`/`agreedFocus`, and `DatingFieldVisibility.kt` logic untouched.

1. `EditorPage(title = stringResource(R.string.dating_editor), subtitle = patient.displayName, tag = "datingEditor", showHeader = !inFlow, useRecordStyle = true)`.
2. `GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)`.
3. **Card 1**: `RecordSectionCard(title = stringResource(R.string.dating_baseline_section), family = FieldFamilies.obstetric())`:
   - `ClinicalDropdownField(..., tag = "datingMethodDropdown", externalLabel = true, shape = RecordShapes.Small)`.
   - `if (visibility.showLmp)`: `ClinicalDateField(..., tag = "lmpDate", showTodayButton = true, externalLabel = true, useRecordStyle = true)`.
   - `if (visibility.showUltrasoundEdd)`: `ClinicalDateField(..., tag = "ultrasoundEdd", showTodayButton = false, externalLabel = true, useRecordStyle = true)`.
   - `if (visibility.showIvf)`:
     - `ClinicalDateField(..., tag = "ivfTransferDate", showTodayButton = false, externalLabel = true, useRecordStyle = true)`.
     - `FormFieldLabel(stringResource(R.string.embryo_age))` (replaces `11.sp` `labelMedium`).
     - `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp))` with `RecordFilterChip`-styled chips (`shape = RecordShapes.Small`, `RecordType.Action`, `outline` unselected / `primary` selected, `.minimumTouchTarget()`) preserving exact tags `"embryoDay3"`, `"embryoDay5"`, `"embryoDay6"` and exact selection logic (`isSelected = (day == 5 && (draft.values.embryoAgeDays.isEmpty() || draft.values.embryoAgeDays == "5")) || draft.values.embryoAgeDays == day.toString()`).
   - `RecordedDateNote(labelRes, isoValue)`: when visible, render `Text(text = stringResource(labelRes, isoValue), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)` (`14.sp`).
   - Always (`visibility.showAgreedEdd`): `ClinicalDateField(..., tag = "agreedEdd", showTodayButton = false, externalLabel = true, useRecordStyle = true)`.
4. **Card 2 (Calculated dating, NO box-inside-box, NO empty header)**:
   - Render `if (calculatedEdd != null || calculatedGa != null)` wrapping a `RecordSectionCard(title = stringResource(R.string.dating_calculated_section), family = FieldFamilies.obstetric())`. Inside, drop the `12.dp` `primaryContainer` `Card` and drop trailing colons `":"`. Use a `Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp))` of two `Modifier.weight(1f)` flat `Surface(color = FieldFamilies.obstetric().container, shape = RecordShapes.Small)` metric tiles (`padding = 12.dp`, stacking vertically above `1.5f` font scale if needed):
     - Tile 1 (if `calculatedEdd != null`): `FormFieldLabel(stringResource(R.string.calculated_edd))` over `Text(text = formatDmy(calculatedEdd), style = RecordType.KeyValue, color = MaterialTheme.colorScheme.onSurface)`.
     - Tile 2 (if `calculatedGa != null`): `FormFieldLabel(stringResource(R.string.current_gestational_age))` over `Text(text = calculatedGa.shortWeeksText, style = RecordType.KeyValue, color = if (calculatedGa.isPostTerm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)` plus, when `calculatedGa.isPostTerm`, `StatusWord(text = stringResource(R.string.post_term_badge), tone = StatusTone.High)` so post-term state never relies on colour alone.
5. **Card 3**: `RecordSectionCard(title = stringResource(R.string.notes), family = FieldFamilies.obstetric())`:
   - `EditorField(..., label = stringResource(R.string.notes), tag = "datingNotes", externalLabel = false, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)` with `Modifier.semantics { contentDescription = notesLabel }` so `"Notes"` is not duplicated twice inside the card.
6. Delete the bottom `Icons.Default.Lock` row and hardcoded `"Gestational age and dating records encrypted locally..."`.
7. When `!inFlow`: replace `EditorActions` with `SheetCancelSaveRow(..., onCancel = { onEvent(AncEvent.CancelEditor) }, onSave = { onEvent(AncEvent.SubmitDating) }, cancelTag = "cancelDating", saveTag = "saveDating", spinnerTag = "datingSaveSpinner")`.

## Acceptance checklist

- Every existing `testTag` kept: `otherHistoriesEditor`, `negativeHistoriesCard`, `fillAllNegativeHistories`, `medicalHistory`, `surgicalHistory`, `familyHistory`, `allergyHistory`, `medHistoryPreset-*`, `surgHistoryPreset-*`, `famHistoryPreset-*`, `allergyHistoryPreset-*`, `saveHistories`, `previousPregnancyEditor`, `pregnancyDateText`, `pregnancyDateTextCalendarButton`, `pregnancyOutcome-*`, `deliveryBy-*`, `lossManagement-*`, `terminationMethod-*`, `pregnancyPlaceDoctor`, `pregnancyType-*`, `pregnancyOtherDetails`, `savePregnancy`, `datingEditor`, `datingMethodDropdown`, `lmpDate*`, `ultrasoundEdd*`, `ivfTransferDate*`, `embryoDay3`, `embryoDay5`, `embryoDay6`, `agreedEdd*`, `datingNotes`, `saveDating`, `editorError`.
- Zero text nodes under `12.sp` in all three editors (`labelMedium` in `QuickPresetChips` and `embryo_age` fixed; `11.sp` vault Lock footers removed).
- Zero hardcoded `"1. "`..`"4. "` titles and zero middle-dot hardcoded English subtitles (`" · Medical, Surgical..."`, `" · Previous Pregnancy..."`, `" · Gestational Age..."`).
- `PreviousPregnancyEditor` chips display human strings (`"Live birth"`, `"Stillbirth / IUFD"`, `"Early pregnancy loss"`, `"Termination of pregnancy"`, `"Cesarean"`, `"Vaginal"`) while reducer state and persisted records receive exact canonical codes (`"LiveBirth"`, `"Stillbirth"`, `"EarlyPregnancyLoss"`, `"TerminationOfPregnancy"`, `"CS"`, `"Vaginal"`).
- `DatingEditor` shows `formatDmy(calculatedEdd)` (`"11/02/2027"`) and `calculatedGa.shortWeeksText` (`"27+2 wks"`) inside flat `FieldObstetricBg` tiles, hides Calculated dating completely when both values are null, and preserves all 4 `DatingFieldVisibility` branches + `RecordedDateNote`.
- `fillAllNegativeHistories`, all preset chips, all `ChoiceGroup` chips, `embryoDay3/5/6`, calendar/today buttons, and `SheetCancelSaveRow` meet `>= 48.dp` target height and fit on `360.dp` width at `1.0f`, `1.3f`, and `2.0f` font scale.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None found (`rg` over `androidTest` and `test` confirmed zero tests assert `"1. Baseline Dating Parameters"`, `"27w 2d"` inside `DatingEditor`, raw `"LiveBirth"` chip text in `PreviousPregnancyEditor`, or the `11.sp` Lock footers).

### Must pass unchanged

- `app/src/test/java/com/sherifradwan/ancnative/ui/DatingFieldVisibilityTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/state/DatingDraftRuleTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/PreviousPregnancyValidationTest.kt` and `app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt` (`datingAndUltrasoundWorkflowAndWorkspaceSectionsRenderAccurately`, pregnancy correction assertions)
- `app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationFlowUiTest.kt` (`saveButtonsAreAbsentInTheFlowAndPresentForHubStartedEditors`)

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt`

Mount through `MainActivity` + `AncApp` + `AncTheme` inside a `360.dp x 640.dp` host:
- Walk semantics trees of `OtherHistoriesEditor`, `PreviousPregnancyEditor`, and `DatingEditor` at `1.0f` and `1.3f`: assert zero `TextLayoutResult` nodes have `fontSize < 12.sp`.
- In `OtherHistoriesEditor`: assert `fillAllNegativeHistories`, `medHistoryPreset-0`, `cancelHistories`, and `saveHistories` are `>= 47.9.dp` tall (`>= 48.dp` width); click `fillAllNegativeHistories` and assert all four negative history fields populate cleanly; test `2.0f` font scale with no clipping beyond `360.dp`.
- In `PreviousPregnancyEditor`: assert `pregnancyOutcome-0` displays `"Live birth"` (and `"Stillbirth / IUFD"`, `"Early pregnancy loss"`, `"Termination of pregnancy"`); click `"Early pregnancy loss"` and verify `AncEvent.PregnancyDraftChanged(OUTCOME, "EarlyPregnancyLoss")` fires and `lossManagement-0..2` appear while `deliveryBy-0` disappears; click `"Live birth"` and assert `"Vaginal"` / `"Cesarean"` (`deliveryBy-0/1`) appear and selecting `"Cesarean"` dispatches `"CS"`.
- In `DatingEditor`:
  - With Ultrasound method and `agreedEdd = "2027-02-11"`, assert `"11/02/2027"` and `"27+2 wks"` (with reference clock or `LocalDate` yielding `27+2`) appear, raw `"2027-02-11"` does not appear in the calculated preview, and empty `DatingDraftValues()` hides `"Calculated dating"`.
  - Switch across `"LMP"`, `"Ultrasound"`, `"IVF_TRANSFER"`, and `"Clinical"`, asserting `lmpDate`+`lmpDateTodayButton`, `ultrasoundEdd`, `ivfTransferDate`+`embryoDay3/5/6` (`>= 47.9.dp` tall), `RecordedDateNote`, and `agreedEdd` visibility matches `DatingFieldVisibilityTest`.
  - Verify `darkTheme = true` and `LayoutDirection.Rtl` render without overlap or clipping.

## Risk

**High (`PreviousPregnancyEditor.kt` and `DatingEditor.kt` are Level 3).** Mapping display labels on `PreviousPregnancyEditor` chips must never alter the canonical stored values (`"LiveBirth"`, `"CS"`, etc.) or `PreviousPregnancyFormRules.modeOf` / `deliveryColumns`, and omitting duplicate labels inside single-purpose `RecordSectionCard` boxes must preserve `contentDescription` for TalkBack and exact `.testTag(...)` placement on `OutlinedTextField`. Require frontier-tier code review and independent test review before merge.
