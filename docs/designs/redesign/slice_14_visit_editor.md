# Slice 14: Visit editor

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s14_visit_editor.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt` (`RecordType`, `RecordShapes`, `RecordSpacing`, `FieldFamilies`, `LocalFieldFamily`, `ltrIsolate`), `ui/components/RecordSectionCard.kt`, `ui/components/FormFieldLabel.kt`, `ui/components/RecordFilterChip.kt`, `ui/components/StatusTag.kt` (`StatusTag`, `StatusKind`, `StatusWord`, `BandChip`, `StatusTone`), `ui/components/PatientNameText.kt`, `ui/components/MetricCell.kt` (`parityText`), `ui/PatientEditor.kt` (`EditorPage` with `useRecordStyle = true` from Slice 12), `ui/Components.kt` (`EditorField` with `externalLabel`, `shape`, `textStyle`, `editorFieldColors`, `minimumTouchTarget`), and `ui/AncDatePicker.kt` (`ClinicalDateField` with `externalLabel`, `useRecordStyle` from Slice 13). Read them; do not re-spec them.

- **Owner direction**: `antislop: DURING`, dials ENERGY 1 / RHYTHM 1 / MOTION 1 (pressed and focus states only, no new animation). Data entry must be fast, easy on the eyes, and not tiring (fewer taps, large targets, calm hierarchy).
- **Branch hygiene**: run `.agents/scripts/git_audit.sh` before starting and before pushing; update `docs/PROJECT_STATE.json` + run `.agents/scripts/project_state.py render` + update `.agents/pipeline/HANDOFF_AGY.md` in the same commit on `redesign-2026-11-15`.
- **Never touch**:
  - `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `storage/SaveVisitCommand.kt` (level 3)
  - `state/UndoCoordinator.kt`, `state/ClinicalDraftReducers.kt`, `state/DraftMapping.kt`, `state/AncState.kt`, `state/AncEvent.kt`, `state/AncViewModel.kt`
  - `model/ClinicalModels.kt`, `model/VisitEntryLogic.kt`, `model/VisitVitals.kt` (level 3), `model/GestationalAgeEngine.kt`, `model/catalog/**`, `*Generator*`
  - `ui/ClinicalBadges.kt` (level 3), `ui/ClinicalFormat.kt` (level 3), `ui/PatientEditor.kt` (level 3; `EditorPage` already supports `useRecordStyle = true`), `ui/Components.kt` (`ClinicalPatientCard` stays untouched until Slice 16), `ui/AncDatePicker.kt`, `ui/ClinicalPresets.kt`
- **Level 3 & clinical safety invariants**:
  - No level-3 file is modified by this slice; `VisitEditor` reads level-3 helpers (`VisitVitals.bpLevel`, `ClinicalFormat.formatBloodGroup`, `ClinicalFormat.isRhNegative`, `ClinicalFormat.isHighRisk`, `ClinicalBadges.BpAlertLabel`) and relies on level-3 `SaveVisitCommand.prepare` and `PatientEditor.EditorPage`. Newly extracted `ui/VisitEditorSteps.kt` counts as level 2 (unmapped) and holds bedside clinical alerts (`highBpWarning`, `fhrLevel`, `detectFlaggedFindings`), so keep frontier/clinical review rigor for Slice 14.
  - **What must not change**:
    - Validation and errors: `DraftError.DATE_REQUIRED`, `INVALID_DATE`, `DATE_IN_FUTURE`, `BLOOD_PRESSURE_INVALID`, `WEIGHT_INVALID`, `STALE_REVISION`, `dateError`, `bpError`, `weightError`, `staleError`, `saveStatus`, and the rule that high BP never blocks saving.
    - Save paths and dispatches: `onSave`, `onEvent(AncEvent.CancelEditor)`, every `onEvent(AncEvent.DraftChanged(DraftField.*, ...))`, `catalogRepo.addCustomChoice(...)` and custom-choice dialog validation/dispatch, and `SavePhase != SavePhase.IDLE` fallback layout and `editingEnabled` disable gating.
    - Calculations and thresholds: `GestationalAgeEngine.calculateGestationalAgeAt`, `resolveLastVisitVitals`, `fillNormalVisitFindings` (strictly fills ONLY blank oedema, fetal movements, urine protein; never touches BP, weight, fundal height, date, or notes), `bpLevel` (>=160/110 severe, >=140/90 raised), `isHighBloodPressure`, `fhrLevel` (<110 bradycardia, >160 tachycardia), `detectFlaggedFindings`, `formatVisitSummary`, and silent round-trip of `DraftValues.fundalHeight`.
    - Stored canonical codes: `"None"` / `"Mild"` / `"Marked"`, `"Good"` / `"Reduced"` / `"Absent"`, `"Negative"` / `"Trace"` / `"+"` / `"++"`, ISO `YYYY-MM-DD` dates, `"110/70"` BP, `"74.5"` weight, `"144"` FHR, `linkedUltrasoundId`.
    - Error-phase subtitle invariant: keep `subtitle = "${patient.displayName} · Routine Antenatal Visit & Vitals"` whenever `state.savePhase != SavePhase.IDLE` so `AncWorkflowTest.reviewedVisitEditorLayoutsKeepStatusDraftAndControlsAccessible` (`TEST_PATIENT_IDENTITY`) keeps passing unchanged.
- **Visual only**: layout, typography, Patient-family (`FieldFamilies.patient()`) visual grouping, labelled 4-step chrome, chip controls, 52 dp steppers, review rows, and pinned bar styling. No schema change, no new clinical input fields, and no calculation/validation/reducer changes.

## Decisions for the owner (this slice)

1. **Replace `ClinicalPatientCard(EDITOR_SUMMARY)` and redundant `IDLE` subtitle with `VisitPatientBanner`**:
   Today the top of `VisitEditor` repeats `patient.displayName` twice, renders 5 sub-12 sp rainbow pills (and `ObstetricParityBadge` drops `A2`), and stacks 5 header blocks (~240 dp) before the first input. **Recommended**: in `SavePhase.IDLE`, pass an empty `subtitle` to `EditorPage` and render one calm `FieldPatientBg` `VisitPatientBanner` (`PatientNameText`, `StatusTag` only for Moderate/High risk, and `BandChip`s for `27+2 wks`, `G4 P1 A2` via `parityText`, and `A+` with `Rh negative` tag if Rh-); keep the `savePhase != SavePhase.IDLE` subtitle unchanged for `AncWorkflowTest`.
2. **Labelled 4-tab `VisitStepHeader` replacing anonymous 4 dp `SegmentedProgressBar`**:
   Today `SegmentedProgressBar` draws 4 thin 4 dp lines with invisible tap targets and no labels. **Recommended**: replace `VisitEditor`'s call with a dedicated `VisitStepHeader` `Surface` keeping `"Step X of 4"`, the step title, and 4 tappable 48 dp `stepSegment-0..3` targets with `RecordShapes.Small` bars and 12 sp short labels (`Date & BP`, `Wt & FHR`, `Findings`, `Review`, hidden above `1.3f` font scale while preserving `onClickLabel`). Leave `SegmentedProgressBar` in `DesignSystem.kt` untouched until Slice 21.
3. **Compact 1-tap `RecordFilterChip` rows on Step 2 replacing 10 stacked `BigChoiceButton`s**:
   Today Step 2 stacks 10 full-width 56 dp `BigChoiceButton`s across 3 cards (~1,100 dp of vertical scroll) with noisy `"(Flag)"` suffixes and a pink `"Fill normal values"` pill. **Recommended**: keep the `"Fill normal values"` quick-fill banner as a calm `FieldPatientBg` panel (`fillNormalValues`), then group Oedema, Fetal movements, and Urine protein into one `"Bedside findings"` `RecordSectionCard` using `FormFieldLabel` + `RecordFilterChip` `FlowRow`s (`48.dp` targets, `10.dp` radius, keeping exact `oedemaChoice-*`, `movementsChoice-*`, `urineChoice-*` tags). Drop `"(Flag)"` from unselected chip labels and show `StatusWord("Flagged", High/Moderate)` beside the group label when an abnormal option is selected (zero tests assert `"(Flag)"`).
4. **Backward-compatible `useRecordStyle: Boolean = false` on `ValueStepper` and `SummaryRow` (`DesignSystem.kt`)**:
   Today `ValueStepper` puts borderless `−` / `+` buttons around a fixed `140.dp` field, and `SummaryRow` renders `13.sp` labels, Title Case `"Fetal Heart Rate"`, and a misleading downward `KeyboardArrowDown` icon on jump-to-edit rows. **Recommended**: add `useRecordStyle: Boolean = false` (+ `decreaseLabel` / `increaseLabel` / `decreaseTag` / `increaseTag` on `ValueStepper`; + `trailingActionText: String? = null` + `showDivider: Boolean = false` on `SummaryRow`), keeping defaults identical so `UltrasoundEditor` and `LabEditor` do not shift before Slices 15 and 16.
5. **Keep fundal height entry omitted, align warning/summary colours, and drop the `11.sp` Lock footer**:
   `VisitEditor.kt` removed fundal height UI in commit `180da0d` (`DraftValues.fundalHeight` only round-trips silently for legacy visits). **Recommended**: keep the exact current field set (do not invent a fundal height input card), move `highBpWarning` and Step 3 flag pills from pink `secondaryContainer` / bright cyan `tertiaryContainer` to `StatusModerate*` / `StatusHigh*` / `StatusWord`, format all GA strings as `shortWeeksText` (`"27+2 wks"`), and remove the `11.sp` Lock footer (consistent with Slices 12 and 13).

## Lessons (apply all)

- **File size ceiling**: `VisitEditor.kt` is 1,299 lines today (`PLAN.md` ceiling 1,500 lines). Split the step composables and header components into `ui/VisitEditorSteps.kt` in the same slice so both `VisitEditor.kt` and `VisitEditorSteps.kt` stay under ~500 lines.
- **Minimum `12.sp` everywhere** (`PLAN.md` 0, 2): eliminate all `labelSmall` (`11.sp`), `labelMedium` (`11.sp`), `ClinicalPatientCard` 11 sp badges, `11.sp` GA/FHR badges, and the `11.sp` Lock footer.
- **Minimum `48.dp` width and height** (`minimumTouchTarget()`) on every interactive target: `stepSegment-0..3`, `Today` / `Yesterday` / BP / Weight / FHR / Findings / Scan `RecordFilterChip`s, `visitDateCalendarButton`, `ValueStepper` `−` and `+` buttons (`52.dp`), `fillNormalValues`, `SummaryRow`s, `unlinkUltrasound`, and pinned bottom bar buttons.
- **Two corner radii only**: `RecordShapes.Small` (`10.dp`) on chips, inputs, stepper buttons, metric tiles, warning boxes, and footer buttons; `RecordShapes.Large` (`16.dp`) on `VisitPatientBanner`, `VisitStepHeader`, `RecordSectionCard`, `qpanel`, and error/status `OutlinedCard`s. Drop `2.dp`, `6.dp`, `8.dp`, `12.dp`, `28.dp`, and `999.dp` pill shapes.
- **360 dp at `1.3f` and `2.0f` font scale**: pinned bottom bar buttons allow `maxLines = 2` (`textAlign = TextAlign.Center`) so `"Save visit"` / `"Cancel"` never clip; `VisitStepHeader` hides short segment labels above `1.3f` (like `RegistrationStepHeader` `WORDS_MAX_FONT_SCALE`) while keeping spoken `onClickLabel`s; `ValueStepper` uses `Modifier.weight(1f)` on the input instead of fixed `140.dp`; BP `Systolic / Diastolic` keeps `Modifier.weight(1f)` inside `Row`.
- **Keep every existing `testTag` on the exact semantic node**: keep `.testTag("visitDate")`, `.testTag("visitSystolic")`, `.testTag("visitDiastolic")`, `.testTag("visitBloodPressure")`, `.testTag("visitWeight")`, `.testTag("visitFetalHeartRate")`, `.testTag("visitFindings")`, and `.testTag("visitNotes")` directly on the `OutlinedTextField` nodes (not outer wrapper `Column`s).

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Lines | Change |
|---|---:|---|
| `res/values/strings.xml` | 789 | Normalize `filled_normal_toast` (drop `"✔ "`); add 16 strings in section 1 |
| `ui/DesignSystem.kt` | 529 | Add backward-compatible `useRecordStyle: Boolean = false` params on `ValueStepper` and `SummaryRow`; leave `BigChoiceButton`, `SectionCard`, `SegmentedProgressBar`, and other helpers unchanged |
| `ui/VisitEditor.kt` | 1299 | Split step bodies out to `VisitEditorSteps.kt`; switch `EditorPage` to `useRecordStyle = true`, restyle `pinnedBottomBar`, `staleError` / `saveStatus`, `savePhase != IDLE` fallback card, custom-choice `AlertDialog`, and drop `Lock` footer |
| `ui/VisitEditorSteps.kt` | new | Extract `VisitPatientBanner`, `VisitStepHeader`, `VisitStepDateAndBp`, `VisitStepWeightAndFhr`, `VisitStepFindings`, and `VisitStepReview` using `FieldFamilies.patient()`, `RecordSectionCard`, `FormFieldLabel`, `RecordFilterChip`, `BandChip`, `StatusTag`, `StatusWord` |

## 1. Strings (`app/src/main/res/values/strings.xml`)

Change value (key unchanged):
- `filled_normal_toast`: `"✔ Populated %1$d standard findings"` -> `"Populated %1$d standard findings"`

Add:
```xml
<string name="visit_date_section">Visit date</string>
<string name="visit_step_short_date_bp">Date &amp; BP</string>
<string name="visit_step_short_wt_fhr">Wt &amp; FHR</string>
<string name="visit_step_short_findings">Findings</string>
<string name="visit_step_short_review">Review</string>
<string name="visit_quick_presets">Quick presets</string>
<string name="visit_ga_at_visit">Gestational age at visit</string>
<string name="visit_fhr_section">Fetal heart rate (FHR)</string>
<string name="visit_fhr_same_as_last">Same as last (%1$s bpm)</string>
<string name="visit_fhr_preset_bpm">%1$s bpm</string>
<string name="visit_weight_preset_kg">%1$s kg</string>
<string name="visit_fhr_normal_baseline">Normal baseline (110-160 bpm)</string>
<string name="visit_fhr_bradycardia">Bradycardia (&lt;110 bpm)</string>
<string name="visit_fhr_tachycardia">Tachycardia (&gt;160 bpm)</string>
<string name="visit_bedside_findings_section">Bedside findings</string>
<string name="visit_finding_flagged">Flagged</string>
<string name="visit_summary_section">Summary</string>
<string name="visit_recorded_values_section">Recorded values</string>
<string name="visit_stepper_decrease">Decrease %1$s</string>
<string name="visit_stepper_increase">Increase %1$s</string>
```
Do not alter `step_n_of_m`, `step_date_bp`, `step_wt_fh`, `step_findings`, `step_review_save`, `high_bp_flagged_warning`, `visit_date`, `other_date`, `add_other_choice`, `fetal_heart_rate`, `finding_not_set`, `nothing_flagged`, or any error string.

## 2. Backward-compatible shared helpers (`ui/DesignSystem.kt`)

Do NOT change behaviour for `UltrasoundEditor` or `LabEditor` (Slices 15 and 16). Both changes below add trailing parameters with defaults preserving current behaviour:

1. **`ValueStepper` (`ui/DesignSystem.kt`)**:
   - Add optional trailing params:
     - `useRecordStyle: Boolean = false`
     - `decreaseLabel: String = "Decrease"` (callers pass `stringResource(R.string.visit_stepper_decrease, ...)` when styled)
     - `increaseLabel: String = "Increase"` (callers pass `stringResource(R.string.visit_stepper_increase, ...)`)
     - `decreaseTag: String? = null`
     - `increaseTag: String? = null`
   - When `useRecordStyle == false`: exact existing UI unchanged.
   - When `useRecordStyle == true`:
     - `Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically)`
     - Decrease button: `OutlinedButton(onClick = onDecrease, enabled = enabled, shape = RecordShapes.Small, contentPadding = PaddingValues(0.dp), border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary), modifier = Modifier.size(52.dp).minimumTouchTarget().semantics { contentDescription = decreaseLabel }.let { if (decreaseTag != null) it.testTag(decreaseTag) else it })` rendering `Text("−", style = RecordType.HeaderName, fontWeight = FontWeight.Bold)`.
     - Input: `OutlinedTextField(..., shape = RecordShapes.Small, textStyle = RecordType.KeyValue, colors = editorFieldColors(), suffix = if (unit.isNotBlank()) { { Text(unit, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant) } } else null, modifier = Modifier.weight(1f).heightIn(min = 52.dp).let { if (inputTag != null) it.testTag(inputTag) else it })`.
     - Increase button: matching `52.dp` `OutlinedButton` with `increaseLabel` / `increaseTag` and `"+"`.

2. **`SummaryRow` (`ui/DesignSystem.kt`)**:
   - Add optional trailing params:
     - `useRecordStyle: Boolean = false`
     - `trailingActionText: String? = null`
     - `showDivider: Boolean = false`
   - When `useRecordStyle == false`: exact existing UI unchanged (`UltrasoundEditor` and `LabEditor` toggles stay intact).
   - When `useRecordStyle == true`:
     - Wrap in `Column(Modifier.fillMaxWidth())`: if `showDivider`, render `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)` above the row.
     - `Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).testTag(tag).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically)`:
       - Left label: `Text(label, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(end = 8.dp))`
       - Right group (`Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp))`):
         - Value: `Text(text = displayText, style = if (isNotSet) RecordType.Missing else RecordType.KeyValue, color = if (isNotSet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)`
         - Trailing cue: if `trailingActionText != null`, render `Text(trailingActionText, style = RecordType.Action, color = MaterialTheme.colorScheme.primary)` (replaces downward `KeyboardArrowDown`!).

## 3. Top chrome, fallback & dialog (`ui/VisitEditor.kt`)

1. **`EditorPage`**:
   - Pass `useRecordStyle = true` and `tag = "visitEditor"`.
   - `subtitle`:
     ```kotlin
     val subtitle = if (state.savePhase != SavePhase.IDLE) {
         "${patient.displayName} · Routine Antenatal Visit & Vitals"
     } else {
         ""
     }
     ```
     (Empty subtitle in `IDLE` removes the duplicated name and `"27w 2d"` middle-dot line; `savePhase != IDLE` keeps `TEST_PATIENT_IDENTITY` intact for `AncWorkflowTest`.)
2. **Top banner (`IDLE` only)**:
   - Render `VisitPatientBanner(patient = patient, gaResult = calculatedGa)` when `state.savePhase == SavePhase.IDLE` (replaces `ClinicalPatientCard(EDITOR_SUMMARY)` inside `VisitEditor`).
3. **Stale revision & SavePhase banners**:
   - Replace `RoundedCornerShape(12.dp)` `Card`s for `staleError` and `saveStatus` with `OutlinedCard(shape = RecordShapes.Large, border = BorderStroke(1.dp, borderColor), colors = CardDefaults.outlinedCardColors(containerColor = bgColor), modifier = Modifier.fillMaxWidth().testTag(...))`:
     - `staleError`, `SavePhase.ERROR`, `SavePhase.UNRESOLVED`: `errorContainer`, `onErrorContainer`, `error` border.
     - `SavePhase.PENDING`, `SavePhase.RECONCILING`: `surface`, `onSurface`, `outlineVariant` border.
     - Text style: `RecordType.Action` (`14.sp` SemiBold), padding `horizontal = 14.dp, vertical = 12.dp`.
4. **`SavePhase != SavePhase.IDLE` fallback form**:
   - Wrap inside `RecordSectionCard(title = stringResource(R.string.visit_recorded_values_section), family = FieldFamilies.patient())`:
     - `ClinicalDateField(..., tag = "visitDate", externalLabel = true, useRecordStyle = true, showTodayButton = true)`
     - `EditorField(..., tag = "visitBloodPressure", externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`
     - `if (bpLevel(draft.values.bloodPressure) != BpLevel.NORMAL) BpAlertLabel(draft.values.bloodPressure)`
     - `EditorField(..., tag = "visitWeight", externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`
     - `EditorField(..., tag = "visitFindings", externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`
     - `EditorField(..., tag = "visitNotes", externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`
5. **`pinnedBottomBar`**:
   - Keep exact step and `savePhase` branching (`cancelVisit`, `nextStep`, `backStep`, `saveVisit`) and exact `enabled = editingEnabled` gating.
   - Update all buttons in `pinnedBottomBar` to `shape = RecordShapes.Small`, `contentPadding = PaddingValues(horizontal = 8.dp)`, `Modifier.weight(1f).heightIn(min = 48.dp).minimumTouchTarget().testTag(...)`, `OutlinedButton` border `BorderStroke(1.dp, if (editingEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant)`, and label `Text(..., style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center)`.
6. **Custom-choice `AlertDialog`**:
   - Restyle with `shape = RecordShapes.Large`, `containerColor = MaterialTheme.colorScheme.surface`, `title` `RecordType.SectionTitle`, `OutlinedTextField` with `shape = RecordShapes.Small`, `textStyle = RecordType.FieldValue`, `editorFieldColors()`, keeping `testTag("customChoiceInput")`, `testTag("customChoiceConfirm")`, `testTag("customChoiceCancel")` with `RecordShapes.Small` + `minimumTouchTarget()`.
7. **Delete the bottom `Icons.Default.Lock` row** (`"Visit records and examinations encrypted locally (AES-256 GCM) · Hardware Vault Security"`).

## 4. Extracted step composables (`ui/VisitEditorSteps.kt`)

### `VisitPatientBanner(patient, gaResult)`
- `Surface(modifier = Modifier.fillMaxWidth(), shape = RecordShapes.Large, color = FieldFamilies.patient().container, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))`
- Inside `Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp))`:
  - Top `Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp))`:
    - `PatientNameText(name = patient.displayName, style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))`
    - Risk tag ONLY if High or Moderate (`val isHigh = isHighRisk(patient.riskLevel); val isModerate = !isHigh && parseRiskTier(patient.riskLevel) == PatientRiskTier.MEDIUM`): `StatusTag(kind = if (isHigh) StatusKind.HighRisk else StatusKind.ModerateRisk)` (no badge for Low/Normal risk).
  - Bottom `FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp))` with `CompositionLocalProvider(LocalFieldFamily provides FieldFamilies.patient())`:
    - If `gaResult != null && gaResult.isPostConception`: `BandChip(ltrIsolate(gaResult.shortWeeksText), tone = null)` (`"27+2 wks"`).
    - `BandChip(ltrIsolate(parityText(patient.gravida, patient.parity, patient.abortion)), tone = null)` (`"G4 P1 A2"`).
    - `formatBloodGroup(patient.bloodGroupText)?.let { bg -> BandChip(ltrIsolate(bg), tone = if (isRhNegative(patient.bloodGroupText)) StatusTone.High else null) }`.

### `VisitStepHeader(currentStep, totalSteps, stepTitleRes, editingEnabled, onStepClick)`
- `Surface(modifier = Modifier.fillMaxWidth(), shape = RecordShapes.Large, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))`
- Inside `Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp))`:
  - `Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically)`:
    - `Text(stringResource(R.string.step_n_of_m, currentStep + 1, totalSteps), style = RecordType.FieldLabel, color = FieldFamilies.patient().label)` (`"Step 1 of 4"` kept for `VisitEntryDesignTest`)
    - `Text(stringResource(stepTitleRes), style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface)`
  - `val showLabels = LocalDensity.current.fontScale <= 1.3f`
  - `Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp))` over indices `0..3` (`shortLabels`: `visit_step_short_date_bp`, `visit_step_short_wt_fhr`, `visit_step_short_findings`, `visit_step_short_review`):
    - Each segment: `Column(modifier = Modifier.weight(1f).heightIn(min = 48.dp).clip(RecordShapes.Small).clickable(enabled = editingEnabled, role = Role.Tab, onClickLabel = stringResource(stepTitles[i])) { onStepClick(i) }.testTag("stepSegment-$i").padding(vertical = 6.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center)`:
      - Indicator bar: `Box(Modifier.fillMaxWidth().height(if (i == currentStep) 8.dp else 6.dp).clip(RecordShapes.Small).then(...))`:
        - `i <= currentStep`: solid `MaterialTheme.colorScheme.primary`
        - `i > currentStep`: `1.5.dp` border `MaterialTheme.colorScheme.primary` on `surface`
      - `if (showLabels)`: `Spacer(Modifier.height(4.dp))` + `Text(stringResource(shortLabels[i]), style = RecordType.Tag.copy(fontWeight = if (i == currentStep) FontWeight.Bold else FontWeight.Medium), color = if (i == currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)`

### Step 0 (`VisitStepDateAndBp`)
- **Card 1 (`RecordSectionCard(stringResource(R.string.visit_date_section), FieldFamilies.patient())`)**:
  - `FlowRow(spacedBy(8.dp))`: `RecordFilterChip` for `Today` and `Yesterday` (`minTouchTarget = true`, tags optional/non-colliding, exact existing `onClick` setting `DraftField.DATE`).
  - `ClinicalDateField(..., label = stringResource(R.string.visit_date), tag = "visitDate", externalLabel = true, useRecordStyle = true, showTodayButton = false)` (keep exact `dateError` mapping).
  - When `gaAtSelectedDate != null && gaAtSelectedDate.isPostConception`: flat `Surface(shape = RecordShapes.Small, color = FieldFamilies.patient().container)` row showing `FormFieldLabel(stringResource(R.string.visit_ga_at_visit))` and `Text(ltrIsolate(gaAtSelectedDate.shortWeeksText), style = RecordType.KeyValue, color = MaterialTheme.colorScheme.onSurface)`.
- **Card 2 (`RecordSectionCard(stringResource(R.string.blood_pressure), FieldFamilies.patient())`)**:
  - `FormFieldLabel(stringResource(R.string.visit_quick_presets))`
  - `FlowRow(spacedBy(8.dp))`: `RecordFilterChip` (`minTouchTarget = true`) for:
    - `lastVitals.bloodPressure`: `stringResource(R.string.same_as_last_bp, lastBp)`, `tag = "bpPreset-sameLast"`
    - `"110/70"`: `stringResource(R.string.normal_bp_preset)`
    - `bpChoices`: `item.label`
    - `+ Add other`: `stringResource(R.string.add_other_choice)` opening `CustomChoiceList.VISIT_BP`
  - `Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom)`:
    - `Column(Modifier.weight(1f))`: `FormFieldLabel(stringResource(R.string.systolic_label))` + `OutlinedTextField` (`shape = RecordShapes.Small`, `textStyle = RecordType.KeyValue`, `colors = editorFieldColors()`, `Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("visitSystolic")`)
    - `Text("/", style = RecordType.HeaderName, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 12.dp))`
    - `Column(Modifier.weight(1f))`: `FormFieldLabel(stringResource(R.string.diastolic_label))` + `OutlinedTextField` (`shape = RecordShapes.Small`, `textStyle = RecordType.KeyValue`, `colors = editorFieldColors()`, `Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("visitDiastolic")`)
  - `EditorField(..., label = "${stringResource(R.string.blood_pressure)} (${stringResource(R.string.mmhg)})", tag = "visitBloodPressure", errorMessage = bpError, singleLine = true, externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`
  - `if (isHighBp)`: semantic `OutlinedCard(shape = RecordShapes.Small, border = BorderStroke(1.dp, borderColor), colors = outlinedCardColors(containerColor = bgColor), modifier = Modifier.fillMaxWidth().testTag("highBpWarning"))` using `StatusHigh*` when `bpLevel == BpLevel.SEVERE` (or systolic >= 160 / diastolic >= 110) and `StatusModerate*` otherwise, containing `StatusWord(if (isSevere) stringResource(R.string.band_severe_bp) else stringResource(R.string.band_raised_bp), ...)` and `Text(stringResource(R.string.high_bp_flagged_warning), style = RecordType.Secondary, color = textColor)`.

### Step 1 (`VisitStepWeightAndFhr`)
- **Card 1 (`RecordSectionCard(stringResource(R.string.weight), FieldFamilies.patient())`)**:
  - `FlowRow(spacedBy(8.dp))`: `RecordFilterChip` (`minTouchTarget = true`) for `Same as last` (`tag = "weightPreset-sameLast"`), `weightChoices` (`visit_weight_preset_kg`), and `+ Add other`.
  - `ValueStepper(..., unit = "kg", useRecordStyle = true, decreaseLabel = stringResource(R.string.visit_stepper_decrease, stringResource(R.string.weight)), increaseLabel = stringResource(R.string.visit_stepper_increase, stringResource(R.string.weight)), inputTag = "visitWeight")` (keep exact +/- 0.1 logic and `[30.0, 250.0]` bounds!).
  - `if (weightError != null)`: `Text(weightError, style = RecordType.Tag, color = MaterialTheme.colorScheme.error)`.
- **Card 2 (`RecordSectionCard(stringResource(R.string.visit_fhr_section), FieldFamilies.patient())`)**:
  - `FormFieldLabel(stringResource(R.string.visit_quick_presets))`
  - `FlowRow(spacedBy(8.dp))`: `RecordFilterChip` (`minTouchTarget = true`) for `Same as last` (`tag = "fhrPreset-sameLast"`), `"135"`, `"140"`, `"145"`, `"150"` (`visit_fhr_preset_bpm`).
  - `ValueStepper(..., unit = "bpm", keyboardType = KeyboardType.Number, useRecordStyle = true, decreaseLabel = stringResource(R.string.visit_stepper_decrease, stringResource(R.string.fetal_heart_rate)), increaseLabel = stringResource(R.string.visit_stepper_increase, stringResource(R.string.fetal_heart_rate)), inputTag = "visitFetalHeartRate")` (keep exact +/- 2 logic and `[60, 220]` bounds!).
  - Baseline word (`fhrLevelResult != null`):
    - `FhrLevel.NORMAL`: `StatusWord(stringResource(R.string.visit_fhr_normal_baseline), StatusTone.Neutral)`
    - `FhrLevel.BRADYCARDIA`: `StatusWord(stringResource(R.string.visit_fhr_bradycardia), StatusTone.High)`
    - `FhrLevel.TACHYCARDIA`: `StatusWord(stringResource(R.string.visit_fhr_tachycardia), StatusTone.High)`

### Step 2 (`VisitStepFindings`)
- **Quick-fill panel (`OutlinedCard`, `shape = RecordShapes.Large`, `containerColor = FieldFamilies.patient().container`, `border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)`)**:
  - Inside `Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp))`:
    - `OutlinedButton(onClick = <exact existing fillNormalVisitFindings + Toast logic>, enabled = editingEnabled, shape = RecordShapes.Small, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumTouchTarget().testTag("fillNormalValues")) { Text(stringResource(R.string.fill_normal_values), style = RecordType.Action) }`
    - `Text(stringResource(R.string.fill_normal_values_sub), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`
- **Bedside findings card (`RecordSectionCard(stringResource(R.string.visit_bedside_findings_section), FieldFamilies.patient())`)**:
  - Three subsections inside one card (Oedema, Fetal movements, Urine protein):
    - **Oedema** (`None`, `Mild`, `Marked` + `oedemaChoices` + `+ Add other`): header `Row` with `FormFieldLabel(stringResource(R.string.oedema))` and `if (draft.values.oedema.equals("Marked", ignoreCase = true)) StatusWord(stringResource(R.string.visit_finding_flagged), StatusTone.High)` (or `Moderate` if mild? Keep exact existing `isFlagged`: `"Marked"` is flagged; `"Reduced"` / `"Absent"` are flagged; `"Trace"` / `"+"` / `"++"` are flagged!). `FlowRow(spacedBy(8.dp))` of `RecordFilterChip` (`minTouchTarget = true`, exact tags `"oedemaChoice-$choice"` and `"oedemaChoice-${item.label}"`) plus `+ Add other` `RecordFilterChip`.
    - **Fetal movements** (`Good`, `Reduced`, `Absent` + `movementsChoices` + `+ Add other`): `FormFieldLabel` + `StatusWord(visit_finding_flagged, StatusTone.High)` when abnormal choice selected + `FlowRow` of `RecordFilterChip`s (`"movementsChoice-$choice"`, `"movementsChoice-${item.label}"`) + `+ Add other`.
    - **Urine protein** (`Negative`, `Trace`, `+`, `++` + `urineChoices` + `+ Add other`): `FormFieldLabel` + `StatusWord(visit_finding_flagged, if (selected == "Trace") StatusTone.Moderate else StatusTone.High)` when abnormal choice selected + `FlowRow` of `RecordFilterChip`s (`"urineChoice-$choice"`, `"urineChoice-${item.label}"`) + `+ Add other`.
- **Findings card (`RecordSectionCard(stringResource(R.string.findings), FieldFamilies.patient())`)**:
  - `EditorField(..., label = "", tag = "visitFindings", shape = RecordShapes.Small, textStyle = RecordType.FieldValue)` with `Modifier.semantics { contentDescription = findingsLabel }` (no repeated `"Findings"` label inside the box).

### Step 3 (`VisitStepReview`)
- **Summary card (`RecordSectionCard(stringResource(R.string.visit_summary_section), FieldFamilies.patient(), modifier = Modifier.testTag("visitSummaryCard"))`)**:
  - Status header:
    - `if (flags.isNotEmpty())`: `FlowRow(spacedBy(6.dp))` rendering a `BandChip(flag, tone = if (flag == "High BP" && bpLevel(draft.values.bloodPressure) != BpLevel.SEVERE) StatusTone.Moderate else if (flag == "Oedema mild" || flag == "Urine trace") StatusTone.Moderate else StatusTone.High)` for each flag returned by `detectFlaggedFindings(...)`.
    - `else`: `StatusWord(stringResource(R.string.nothing_flagged), StatusTone.Neutral)`.
  - Summary text: call `formatVisitSummary(..., gaText = calculatedGa?.shortWeeksText, fundalHeightCm = null, ...)` so GA in the summary reads `"27+2 wks"` (keeping `formatVisitSummary` in `VisitEntryLogic.kt` untouched!), rendered in `RecordType.FieldValue` (`16.sp`), `onSurface`.
- **7 Review Rows card (`RecordSectionCard(stringResource(R.string.visit_recorded_values_section), FieldFamilies.patient())`)**:
  - 7 `SummaryRow` calls (`useRecordStyle = true`, `trailingActionText = stringResource(R.string.action_edit)`, `showDivider = true` on rows 2..7):
    - `summaryRow-date` (`R.string.visit_date`, Step 0)
    - `summaryRow-bp` (`R.string.blood_pressure`, Step 0)
    - `summaryRow-weight` (`R.string.weight`, Step 1)
    - `summaryRow-fhr` (`R.string.fetal_heart_rate`, `"Fetal heart rate"`, Step 1)
    - `summaryRow-oedema` (`R.string.oedema`, Step 2)
    - `summaryRow-movements` (`R.string.fetal_movements`, Step 2)
    - `summaryRow-urine` (`R.string.urine_protein`, Step 2)
- **Linked Ultrasound Card (`RecordSectionCard(stringResource(R.string.linked_ultrasound), FieldFamilies.patient(), modifier = Modifier.testTag("visitLinkedUltrasoundCard"))`)**:
  - When linked: scan title formatted with `gestationalAgeWeeks?.let { " (${it}+${linkedScan.gestationalAgeDays ?: 0} wks)" }`, impression in `RecordType.Secondary`, and `OutlinedButton(..., shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("unlinkUltrasound"))`.
  - When unlinked: `Text(stringResource(R.string.link_to_ultrasound), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`, plus `FlowRow(spacedBy(8.dp))` of `RecordFilterChip` (`minTouchTarget = true`, `tag = "linkScan-${scan.id}"`) with label `"${scan.date}" + (scan.gestationalAgeWeeks?.let { " (${it}+${scan.gestationalAgeDays ?: 0} wks)" } ?: "")`.
- **Notes Card (`RecordSectionCard(stringResource(R.string.notes), FieldFamilies.patient())`)**:
  - `EditorField(..., label = "", tag = "visitNotes", shape = RecordShapes.Small, textStyle = RecordType.FieldValue, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done))` with `Modifier.semantics { contentDescription = notesLabel }`.

## Acceptance checklist

1. All existing `testTag`s kept on their exact semantic nodes:
   `visitEditor`, `staleError`, `saveStatus`, `cancelVisit`, `nextStep`, `backStep`, `saveVisit`, `stepSegment-0`, `stepSegment-1`, `stepSegment-2`, `stepSegment-3`, `visitDate`, `visitDateCalendarButton`, `bpPreset-sameLast`, `visitSystolic`, `visitDiastolic`, `visitBloodPressure`, `highBpWarning`, `bpAlertLabel`, `weightPreset-sameLast`, `visitWeight`, `fhrPreset-sameLast`, `visitFetalHeartRate`, `fillNormalValues`, `oedemaChoice-*`, `movementsChoice-*`, `urineChoice-*`, `visitFindings`, `visitSummaryCard`, `summaryRow-date`, `summaryRow-bp`, `summaryRow-weight`, `summaryRow-fhr`, `summaryRow-oedema`, `summaryRow-movements`, `summaryRow-urine`, `visitLinkedUltrasoundCard`, `unlinkUltrasound`, `linkScan-*`, `visitNotes`, `customChoiceInput`, `customChoiceConfirm`, `customChoiceCancel`.
2. Both `VisitEditor.kt` and `VisitEditorSteps.kt` stay well under the 1,500-line ceiling.
3. Zero text nodes under `12.sp` in `VisitEditor` across all 4 steps and `SavePhase` fallback (`ClinicalPatientCard` 11 sp pills, `11.sp` GA/FHR badges, and `11.sp` Lock footer are gone).
4. Zero `2.dp`, `6.dp`, `8.dp`, `12.dp`, `28.dp`, or pill `999.dp` radii inside `VisitEditor`; every shape is `RecordShapes.Small` (`10.dp`) or `RecordShapes.Large` (`16.dp`).
5. Every tappable control (`stepSegment-0..3`, preset and finding `RecordFilterChip`s, `visitDateCalendarButton`, `ValueStepper` `−`/`+`, `fillNormalValues`, `SummaryRow`s, `unlinkUltrasound`, `cancelVisit`, `backStep`, `nextStep`, `saveVisit`) measures `>= 48.dp` height (`>= 47.9.dp` in instrumented checks) and fits cleanly on `360.dp` at `1.0f`, `1.3f`, and `2.0f` font scale.
6. `VisitPatientBanner` displays `PatientNameText` (right-aligned for Arabic names such as `"منى رجب"`), `27+2 wks` GA, `G4 P1 A2` parity (including `abortion > 0`), and `A+` (with `Rh negative` chip when Rh-), and hides the risk tag for low/normal-risk patients.
7. `highBpWarning` renders in semantic amber (`StatusModerate*`) for raised BP and red (`StatusHigh*`) for severe BP, with the exact message `"High BP is flagged on the visit. You can still save."`, and never disables `saveVisit`.
8. `UltrasoundEditor` and `LabEditor` look identical after `DesignSystem.kt` updates (`useRecordStyle = false` default).
9. Dark theme and RTL layout (`LayoutDirection.Rtl`) render cleanly without overlapping chips, steppers, or summary rows.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected.
- `VisitEntryDesignTest` keeps passing unchanged (`"Step 1 of 4"`..`"Step 4 of 4"`, `stepSegment-0`, `stepSegment-2`, `visitDate`, `visitSystolic`, `visitDiastolic`, `highBpWarning`, `"High BP is flagged on the visit. You can still save."`, `bpPreset-sameLast`, `weightPreset-sameLast`, `fhrPreset-sameLast`, `visitWeight`, `fillNormalValues`, `visitSummaryCard`, `"High BP"`, `nextStep`, `saveVisit`).
- `AncWorkflowTest.reviewedVisitEditorLayoutsKeepStatusDraftAndControlsAccessible` keeps passing unchanged because `savePhase != SavePhase.IDLE` preserves `TEST_PATIENT_IDENTITY`, `saveStatus`, `visitDate`, `visitBloodPressure`, `visitWeight`, `visitFindings`, `visitNotes`, `cancelVisit`, and `saveVisit`.

### Must pass unchanged

- `app/src/androidTest/java/com/sherifradwan/ancnative/VisitEntryDesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/storage/DraftPersistenceIntegrationTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/VisitsTabRedesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/ClinicalPatientCardTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/DatingGaMarkerSurfacesTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/VisitEntryLogicTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/VisitVitalsTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/storage/SaveVisitValidationTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/RecordTokensContrastTest.kt`

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/VisitEditorRedesignTest.kt`

Mount `VisitEditor` through `MainActivity` + `AncTheme` inside a `360.dp x 640.dp` host (matching `VisitEntryDesignTest` reducer host + `ComposeTextAsserts` helpers, single `setContent` per test):
- Walk semantics trees across Steps 0, 1, 2, 3, and `SavePhase.ERROR` fallback at `1.0f` and `1.3f` font scale: assert zero `TextLayoutResult` nodes have `fontSize < 12.sp`, and `"AES-256"` / `"Normal Risk"` / `"(Flag)"` / `"27w 2d"` do not appear anywhere in `IDLE` mode.
- Top banner: render Mona Ragab (`G4 P1 A2`, `A+`, EDD `"2027-02-11"`) and assert `"G4 P1 A2"`, `"A+"`, and `"27+2 wks"` are displayed; render `"منى رجب"` and assert RTL right-edge alignment via `PatientNameText`.
- Step 0 & Step 1 controls at `360.dp` (`1.0f`, `1.3f`, `2.0f`): assert `stepSegment-0..3`, `visitDateCalendarButton`, `bpPreset-sameLast`, `weightPreset-sameLast`, `fhrPreset-sameLast`, `visitWeightDecrease`/`Increase`, `visitFetalHeartRateDecrease`/`Increase`, `cancelVisit`, `nextStep` all meet `>= 47.9.dp` height and do not clip beyond `360.dp`; verify `stepSegment-3` jumps directly to Step 4 of 4 in one click.
- Step 1 FHR baseline & Step 2 findings: enter `"140"` -> `"Normal baseline (110-160 bpm)"` displayed without `"✓"`; enter `"100"` -> `"Bradycardia (<110 bpm)"` displayed without `"⚠️"`; on Step 2, tap `urineChoice-+` -> `"Flagged"` displayed and `AncEvent.DraftChanged(DraftField.URINE_PROTEIN, "+")` updates draft.
- Step 3 review: assert `summaryRow-fhr` displays `"Fetal heart rate"` (sentence case) and `"Edit"`, tapping `summaryRow-bp` jumps back to Step 1 of 4, and tapping `saveVisit` invokes `onSave`.
- Dark theme (`AncTheme(darkTheme = true)`) and `LayoutDirection.Rtl`: verify Steps 0 and 3 render with no overlap or horizontal overflow.

## Risk

**Medium-high.** `VisitEditor.kt` drives bedside vital charting (`visitSystolic` / `visitDiastolic` / `visitBloodPressure` dual sync, `ValueStepper` numeric bounds, `fillNormalVisitFindings` safety invariant, high-BP flagging, and `SavePhase` recovery). Modifying `DesignSystem.kt` (`ValueStepper`, `SummaryRow`) also touches a shared UI file used by `UltrasoundEditor` and `LabEditor`; guard all shared changes behind `useRecordStyle = false` defaults, split steps cleanly into `VisitEditorSteps.kt`, keep every `OutlinedTextField` `testTag` directly on the text field node, and route the PR through frontier-tier clinical safety review and independent test review before merge.
