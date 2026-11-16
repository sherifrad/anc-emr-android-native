# Slice 15: Ultrasound editor and photo viewer

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s15_ultrasound_editor.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt` (`RecordType`, `RecordShapes`, `RecordSpacing`, `FieldFamilies`, `LocalFieldFamily`, `ltrIsolate`), `ui/components/RecordSectionCard.kt` (`RecordSectionCard`, `SectionTextAction`), `ui/components/FormFieldLabel.kt`, `ui/components/RecordFilterChip.kt`, `ui/components/MetricCell.kt` (`PlainField`), `ui/patienthub/components/EditorSheetParts.kt` (`SheetCancelSaveRow`), `ui/Components.kt` (`EditorField`, `GenericEditorErrorCard`, `minimumTouchTarget`), `ui/PatientEditor.kt` (`EditorPage`, `ChoiceGroup`), `ui/AncDatePicker.kt` (`ClinicalDateField`), and `ui/DesignSystem.kt` (`SummaryRow` with Slice 14 `useRecordStyle` extension). Read them; do not re-spec them.

- **Owner direction**: `antislop: DURING`, dials ENERGY 1 / RHYTHM 1 / MOTION 1 (pressed and focus states only, no new animation). Data entry must be fast, easy on the eyes, and not tiring (fewer taps, large targets, calm hierarchy).
- **Branch hygiene**: run `.agents/scripts/git_audit.sh` before starting and before pushing; update `docs/PROJECT_STATE.json` + run `.agents/scripts/project_state.py render` + update `.agents/pipeline/HANDOFF_AGY.md` in the same commit on `redesign-2026-11-15`.
- **Never touch**:
  - `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`
  - `storage/ClinicalCommands.kt` (`SaveUltrasoundScanCommand`), `storage/PatientAttachmentVault.kt`, `storage/BoundedBitmap.kt`, `storage/CameraCaptureHelper.kt`, `storage/ScanPhotoRef.kt`, `storage/AttachmentOrphanSweeper.kt`, `storage/UltrasoundDeleteAdapter.kt`
  - `state/UndoCoordinator.kt`, `state/ClinicalDraftReducers.kt` (`startUltrasoundEditor`, `updateUltrasoundDraft`), `state/DraftMapping.kt`, `state/AncState.kt`, `state/AncEvent.kt`, `state/AncViewModel.kt` (`importScanPhoto`)
  - `model/ClinicalModels.kt` (`ClinicalVocabulary`, `NORMAL_FETAL_NUMBER`, `NORMAL_PRESENTATION`, `NORMAL_AMNIOTIC_FLUID`, `ultrasoundImpressionFromChoices`, `mergeImpression`), `model/GestationalAgeEngine.kt`, `model/Redating.kt` (`redatingAdvice`), `*Generator*`
  - `ui/ClinicalAttachmentViewerDialog.kt` (level 3, already redesigned in Slice 10), `ui/InAppCameraDialog.kt` (Slice 19), `ui/AncDatePicker.kt`, `ui/Components.kt`, `ui/PatientEditor.kt` (level 3), `ui/patienthub/tabs/ScansTab.kt`, `ui/patienthub/components/BiometryGrid.kt`
- **Level 3 & clinical safety invariants**:
  - No file edited by Slice 15 is marked level 3 in `PLAN.md` / `.agents/PROJECT_MAP.json`. `UltrasoundEditor.kt` reads level-3 `PatientEditor.kt` (`EditorPage`, `ChoiceGroup`) without editing it, and level-3 `ClinicalAttachmentViewerDialog.kt` consumes `ZoomableImage` without being edited. Keep clinical-safety and independent test review rigor because `UltrasoundEditor.kt` handles fetal biometry, GA/redating warnings, vocabulary findings, and scan photo URIs.
  - **What must not change**:
    - **Validation and error highlighting**: `ClinicalValidationError.REQUIRED_DATE` / `INVALID_DATE` error state on `scanDate`, `INVALID_GA_WEEKS` on `gaWeeks`, `INVALID_GA_DAYS` on `gaDays`, and `GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)`.
    - **Save paths and event dispatches**: `onEvent(AncEvent.SubmitUltrasound)`, `onEvent(AncEvent.CancelEditor)`, every `onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.*, ...))`, `onEvent(AncEvent.ImportScanPhoto(route.patientId, uri))` from both `photoPickerLauncher` and `InAppCameraDialog`, `updated.joinToString(",")` on photo removal, `mergeImpression(draft.values.impression, generatedImpression)`, and `enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR` disable gating across all controls.
    - **Calculations and thresholds**: `GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.values.date)`, `calculatedGa.isPostConception`, `isDifferent` comparison on `gaWeeks`/`gaDays`, `redatingAdvice(calculatedGa.totalDays, weeks * 7L + days)` (`abs(advice.differenceDays)`, `advice.thresholdDays`), `ultrasoundImpressionFromChoices(...)`, `nextBlankFinding(...)` guided auto-advance (`afterKey`), `hasMoreInitial`, `hasBiometry`, and `ZoomableImage` math (`ZOOM_MIN`, `ZOOM_MAX`, `clampZoom`, `clampOffset`, `nextZoomOnDoubleTap`, double-tap zoom, two-finger pinch/pan, single-finger zoomed pan, and `LaunchedEffect(resetKey)`).
    - **Stored canonical codes & field completeness**: All 16 `UltrasoundDraftValues` fields remain present and editable (`date`, `gaWeeks`, `gaDays`, `fetalNumber`, `presentation`, `placentalSite`, `amnioticFluid`, `fetalHeartRate`, `biometryBpd`, `biometryHc`, `biometryAc`, `biometryFl`, `estimatedFetalWeight`, `impression`, `linkedVisitId`, `imageUris`). All `ClinicalVocabulary` stored strings (`"Singleton"`, `"Twins"`, `"Higher order"`, `"Cephalic"`, `"Breech"`, `"Transverse"`, `"Variable"`, `"Anterior"`, `"Posterior"`, `"Fundal"`, `"Low-lying"`, `"Previa"`, `"Normal"`, `"Oligohydramnios"`, `"Polyhydramnios"`, `""` for Unknown) stay untouched.
    - **Photo viewer guard & decoding**: `UltrasoundPhotoViewerDialog` keeps `if (scan.imageUris.isEmpty()) return`, `currentIndex` state keyed on `scan.id`, `loadedBitmap` `remember(currentUriString)` (`scanPhotoFileName`, `PatientAttachmentVault.resolveAttachmentFile`, `BoundedBitmap.decode...` at `2048`), and `DialogProperties(usePlatformDefaultWidth = false)`.
- **Visual only**: Obstetric-family (`FieldFamilies.obstetric()`) section cards and live summary panel, external `12.sp` labels, `10.dp`/`16.dp` corners, `48.dp` touch targets, `27+2 wks` GA formatting, clean sentence-case titles, stacked photo source buttons, and restyled zoom reset & photo viewer chrome. No schema change, no new fields, and no reducer/validation/calculation changes.

## Decisions for the owner (this slice)

1. **Keep Card 0 (`ultrasoundLiveSummary`) at the top as a calm `FieldObstetricBg` panel with demoted Clear button**:
   Today `ultrasoundLiveSummary` sits at the top in a plain white card with `13.sp` body text and two identical pill buttons (`Fill normal scan` and `Clear findings`). **Recommended**: keep `ultrasoundLiveSummary` at the top so 1-tap `Fill normal scan` stays immediately reachable, style it in `FieldFamilies.obstetric().container` (`#E3F0EE` light / `#1D3836` dark) with `16.sp` live summary text (italic `Nothing chosen yet` when blank), make `fillNormalScan` a `48.dp` `RecordShapes.Small` `OutlinedButton` on `surface`, and render `clearFindings` as a `48.dp` `TextButton` so Clear is never tapped by mistake.
2. **Format calculated GA in `scanCalculatedGaCard` and `applyScanGa` as `27+2 wks` (`shortWeeksText`)**:
   Today lines 246 and 281 pass `calculatedGa.displayText` (`"27w 2d"`). Owner Decision 9 (`PLAN.md` section 7) standardized `"27+2 wks"` everywhere (`shortWeeksText`). **Recommended**: switch `scanCalculatedGaCard` and `applyScanGa` to `calculatedGa.shortWeeksText`, restyle `scanCalculatedGaCard` from bright blue `primaryContainer` to `FieldObstetricBg` (`RecordShapes.Small`), and place `redatingHint` in a semantic `StatusModerate*` amber box directly below it when `advice != null`. Zero tests assert `"27w 2d"` inside `UltrasoundEditor`.
3. **Reuse Slice 14's backward-compatible `SummaryRow(..., useRecordStyle = true)` on collapsed guided findings, and drop the downward chevron on `ultrasoundMoreToggle`**:
   Today `SummaryRow` in `DesignSystem.kt` draws `13.sp` labels and a downward `KeyboardArrowDown` arrow on both `findingRow-*` and `ultrasoundMoreToggle` (even when `ultrasoundMoreToggle` says `"Close"`). **Recommended**: pass `useRecordStyle = true` to `SummaryRow` on `findingRow-*` with `trailingActionText = "Choose"` / `"Edit"` (omitted above `1.3f` font scale so long values like `"Oligohydramnios"` never squeeze the label), and draw `ultrasoundMoreToggle` as an `OutlinedCard` (`16.dp`, `56.dp` height) with `"Open"` / `"Close"` in `RecordType.Action` (no downward chevron). If Slice 14 has not landed first, add the exact Slice 14 optional params to `SummaryRow` in `DesignSystem.kt` (`useRecordStyle = false` default, leaving `LabEditor` untouched) and note it in the PR.
4. **Include `UltrasoundPhotoViewerDialog.kt` alongside `UltrasoundEditor.kt` and `ZoomableImage.kt`, and stack photo source buttons full-width**:
   Today `UltrasoundEditor` puts `"Take Photo"` and `"Choose Image"` side by side with `softWrap = false` and icons (clipping risk on `360.dp` at `1.3f`/`2.0f` scale), draws `attachmentItem-$index` at `11.sp`, `ZoomableImage`'s `1x` reset chip uses `11.sp` (`labelMedium`, `8.dp`, `0.9f` alpha), and `UltrasoundPhotoViewerDialog` uses a `20.dp` elevated card, decorative `PhotoCamera` circle, `11.sp` `FactBadge` pills, and `8.dp` clickable dots (`< 48.dp` targets). **Recommended**: stack full-width `48.dp` `"Take photo"` and `"Choose from gallery"` buttons in `scanAttachmentsCard`, raise `attachmentItem-$index` to `16.sp` with a `48.dp` Remove `IconButton`, upgrade `ZoomableImage`'s `1x` reset button (`10.dp`, solid `surface`, `outline` border, `14.sp`), and restyle `UltrasoundPhotoViewerDialog` (`16.dp` scrollable card, clean header, non-clickable indicator dots, `PlainField` biometry summary).
5. **Drop the `11.sp` vault Lock footer, normalize numbered card titles, and place inline `SheetCancelSaveRow` at the bottom**:
   Today Cards 1–4 use hardcoded `"1. "`/`"3. "`/`"4. "` or string `"2. "` Title Case titles, Card 3 uses `"▲ Collapse"` / `"▼ Expand Biometry"` with `11.sp` subtitle, Card 4 ends with the `11.sp` AES-256 Lock footer, and the bottom uses uneven pill `EditorActions`. **Recommended**: remove the `11.sp` Lock footer (consistent with Slices 12, 13, 14), move all titles to sentence-case `strings.xml` entries without numbers or triangles, and use inline `SheetCancelSaveRow` at the bottom of the scroll column (matching `DatingEditor` / `PreviousPregnancyEditor` so the soft keyboard on `360x640` preserves scroll space).

## Lessons (apply all)

- **Minimum `12.sp` everywhere** (`PLAN.md` 0, 2): eliminate `labelSmall` (`11.sp`) in Card 3's subtitle (`BPD, HC, AC, FL, FHR & EFW`) and Card 4's Lock footer, `labelMedium` (`11.sp`) in `attachmentItem-$index` (`Photo 1`) and `ZoomableImage`'s `1x` reset button, `labelSmall` (`11.sp`) in `UltrasoundPhotoViewerDialog`'s `FactBadge` pills, and `bodyMedium` (`13.sp`) in Card 0 and `SummaryRow`.
- **Minimum `48.dp` width and height** (`minimumTouchTarget()`) on every interactive target: `fillNormalScan`, `clearFindings`, `scanDateCalendarButton`, `scanDateTodayButton`, `applyScanGa`, all `ChoiceGroup` chips (`RecordFilterChip` with `minTouchTarget = true`), `findingRow-*`, `toggleBiometry`, `impressionFromChoices`, `ultrasoundMoreToggle`, `removePhoto-$index`, `captureScanPhotoCamera`, `addScanPhoto`, `unlinkVisit`, `linkVisit-*`, `SheetCancelSaveRow`, `zoomResetButton`, `closePhotoViewer`, `prevPhotoButton`, `nextPhotoButton`, and `dismissPhotoViewer`. Remove `.clickable` from the `8.dp` pager indicator dots in `UltrasoundPhotoViewerDialog`.
- **Two corner radii only**: `RecordShapes.Small` (`10.dp`) on inputs, chips, buttons, GA/warning tiles, attachment rows, viewport box, and `1x` reset chip; `RecordShapes.Large` (`16.dp`) on cards and dialogs. Remove `8.dp`, `14.dp`, `20.dp`, and `999.dp` pill shapes.
- **360 dp at `1.3f` and `2.0f` font scale**: external `FormFieldLabel`s eliminate notch truncation on `"GA weeks (0–45)"` and `"Estimated fetal weight (EFW)"`; stack `gaWeeks`/`gaDays` and the 3 biometry `Row`s vertically when `LocalDensity.current.fontScale > 1.3f`; stack `captureScanPhotoCamera` and `addScanPhoto` full-width; omit `SummaryRow`'s `"Edit"`/`"Choose"` trailing action above `1.3f` font scale so long values like `"Oligohydramnios"` do not squeeze labels; add `.verticalScroll(rememberScrollState())` and `.heightIn(max = (LocalConfiguration.current.screenHeightDp - 32).dp)` to `UltrasoundPhotoViewerDialog`.
- **Keep every existing `testTag` on the exact semantic node**: keep `.testTag("zoomResetButton")` outermost (first) on `ZoomableImage`'s reset `Surface`, and keep `.testTag(...)` directly on `OutlinedTextField` inside `EditorField` and `ClinicalDateField` (never on outer wrapper `Column`s).

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Lines | Change |
|---|---:|---|
| `res/values/strings.xml` | 789 | Normalize `ultrasound_section_fetus_placenta_liquor` (`"Fetus, placenta and amniotic fluid"`); add 13 strings in section 1; add `saving_label` if Slice 12/13 has not landed |
| `ui/DesignSystem.kt` | 529 | Only if Slice 14 has not landed yet: add backward-compatible `useRecordStyle: Boolean = false`, `trailingActionText: String? = null`, `showDivider: Boolean = false` on `SummaryRow` (`useRecordStyle = false` default keeps `LabEditor` unchanged) |
| `ui/UltrasoundEditor.kt` | 745 | Restyle with `EditorPage(..., useRecordStyle = true)`, `GenericEditorErrorCard(..., useRecordStyle = true)`, `FieldFamilies.obstetric()` `RecordSectionCard`s, `FieldObstetricBg` live summary & GA tile, `shortWeeksText`, amber `redatingHint`, `ChoiceGroup(..., useRecordChip = true)`, `SummaryRow(..., useRecordStyle = true)`, external-label biometry fields, full-width photo buttons, `RecordFilterChip` visit links, `SheetCancelSaveRow`, and drop Lock footer |
| `ui/components/ZoomableImage.kt` | 197 | Restyle `zoomResetButton` (`1x` chip): `RecordShapes.Small`, solid `surface`, `outline` border, `RecordType.Action` (`14.sp`); keep all zoom/pan math and `.testTag("zoomResetButton")` outermost unchanged |
| `ui/UltrasoundPhotoViewerDialog.kt` | 297 | Restyle dialog to `RecordShapes.Large`, elevation `0.dp`, scrollable column, clean header (no camera circle, comma separator), `RecordShapes.Small` black viewport, `minimumTouchTarget()` on header/pager buttons, non-clickable indicator dots, `PlainField` biometry grid (drop `FactBadge`), and `RecordShapes.Small` Close button |

## 1. Strings (`app/src/main/res/values/strings.xml`)

Change value (key unchanged; grep verified zero tests assert the old `"2. "` string):
- `ultrasound_section_fetus_placenta_liquor`: `"2. Fetus, Placenta &amp; Amniotic Fluid"` -> `"Fetus, placenta and amniotic fluid"`

Add:
```xml
<string name="ultrasound_timing_section">Scan timing and gestational age</string>
<string name="ultrasound_biometry_section">Fetal biometry</string>
<string name="ultrasound_biometry_summary">BPD, HC, AC, FL, FHR and EFW</string>
<string name="ultrasound_findings_progress">%1$d of %2$d set</string>
<string name="ultrasound_finding_choose">Choose</string>
<string name="ultrasound_linked_visit_title">Linked visit: %1$s</string>
<string name="ultrasound_linked_visit_vitals">BP %1$s, weight %2$s</string>
<string name="ultrasound_viewer_title">Ultrasound scan, %1$s</string>
<string name="ultrasound_viewer_photo_of">Photo %1$d of %2$d</string>
<string name="ultrasound_viewer_photo_of_ga">Photo %1$d of %2$d, %3$s</string>
<string name="ultrasound_viewer_photo_cd">Ultrasound scan photo %1$d</string>
<string name="previous_photo">Previous photo</string>
<string name="next_photo">Next photo</string>
```
If Slice 12/13 has not landed yet, also ensure `<string name="saving_label">Saving…</string>` is present.

Do not alter `ultrasound_editor`, `ultrasound_report_so_far`, `ultrasound_nothing_chosen_yet`, `fill_normal_scan`, `clear_findings`, `scan_date`, `calculated_ga_at_visit`, `redating_hint`, `apply_calculated_ga`, `ga_weeks`, `ga_days`, `fetal_number`, `fetal_presentation`, `placental_site`, `amniotic_fluid`, `fetal_heart_rate`, `estimated_fetal_weight`, `biometry_bpd`, `biometry_hc`, `biometry_ac`, `biometry_fl`, `scan_impression`, `impression_from_choices`, `ultrasound_more`, `more_open`, `more_close`, `scan_attachments`, `no_photos_attached`, `remove_photo`, `attachment_take_photo`, `attachment_gallery`, `link_to_visit`, `unlink_visit`, `no_linked_visit`, `finding_not_set`, `zoom_reset`, or `image_unavailable`.

## 2. Backward-compatible `SummaryRow` (`ui/DesignSystem.kt`)

If Slice 14 has already landed, reuse its `SummaryRow(..., useRecordStyle = true, trailingActionText = ..., showDivider = ...)` as built. Otherwise, add the exact Slice 14 optional trailing parameters to `SummaryRow`:
- `useRecordStyle: Boolean = false`
- `trailingActionText: String? = null`
- `showDivider: Boolean = false`
When `useRecordStyle == false`, keep existing UI unchanged (`LabEditor` does not shift). When `useRecordStyle == true`: render optional `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)`, `Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).testTag(tag).padding(vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically)`, left label in `RecordType.Secondary` (`onSurfaceVariant`, `Modifier.weight(1f).padding(end = 8.dp)`), right value in `RecordType.Missing` (`onSurfaceVariant`) or `RecordType.KeyValue` (`onSurface`), and optional `trailingActionText` in `RecordType.Action` (`primary`) with `10.dp` gap (no `KeyboardArrowDown` icon).

## 3. `UltrasoundEditor.kt` (same signature)

Keep `openFindingKey`, `moreExpanded`, `biometryExpanded`, `showCamera`, `photoPickerLauncher`, `InAppCameraDialog`, and all `onEvent(...)` / `nextBlankFinding(...)` / `mergeImpression(...)` logic intact. Define `val obstetricFamily = FieldFamilies.obstetric()` and `val narrowStacked = LocalDensity.current.fontScale > 1.3f`.

1. **Page header & error card**:
   - `EditorPage(title = stringResource(R.string.ultrasound_editor), subtitle = patient.displayName, tag = "ultrasoundEditor", useRecordStyle = true)` (drops hardcoded `" · Ultrasound Examination & Fetal Biometry"`).
   - `GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)`.

2. **Card 0 (`ultrasoundLiveSummary`)**:
   - Replace `SectionCard` with `OutlinedCard(modifier = Modifier.fillMaxWidth().testTag("ultrasoundLiveSummary"), shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = obstetricFamily.container), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))`.
   - Inside `Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp))`:
     - `Text(stringResource(R.string.ultrasound_report_so_far), style = RecordType.CardName, color = obstetricFamily.label, modifier = Modifier.semantics { heading() })`
     - `Text(text = reportDisplayText, style = if (liveReport.isBlank()) RecordType.Missing else RecordType.FieldValue, color = if (liveReport.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)`
     - `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp))`:
       - `OutlinedButton` (`tag = "fillNormalScan"`): keep exact `onClick`, `enabled = enabled`, `shape = RecordShapes.Small`, `border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant)`, `colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)`, `modifier = Modifier.minimumTouchTarget().testTag("fillNormalScan")`, label `Text(stringResource(R.string.fill_normal_scan), style = RecordType.Action)`.
       - `TextButton` (`tag = "clearFindings"`): keep exact `onClick`, `enabled = enabled`, `shape = RecordShapes.Small`, `modifier = Modifier.minimumTouchTarget().testTag("clearFindings")`, label `Text(stringResource(R.string.clear_findings), style = RecordType.Action, color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)`.

3. **Card 1 (`Scan timing and gestational age`)**:
   - Wrap in `RecordSectionCard(title = stringResource(R.string.ultrasound_timing_section), family = obstetricFamily)`:
   - `ClinicalDateField(..., label = stringResource(R.string.scan_date), tag = "scanDate", enabled = enabled, error = <unchanged>, showTodayButton = true, externalLabel = true, useRecordStyle = true)`.
   - `if (calculatedGa != null && calculatedGa.isPostConception)`:
     - `val gaShort = ltrIsolate(calculatedGa.shortWeeksText)`
     - `Surface(modifier = Modifier.fillMaxWidth().testTag("scanCalculatedGaCard"), color = obstetricFamily.container, shape = RecordShapes.Small)`:
       - `Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp))`:
         - `Text(text = stringResource(R.string.calculated_ga_at_visit, gaShort), style = RecordType.KeyValue, color = MaterialTheme.colorScheme.onSurface)`
         - `if (isDifferent)`: `TextButton(onClick = <unchanged>, enabled = enabled, shape = RecordShapes.Small, modifier = Modifier.align(Alignment.End).minimumTouchTarget().testTag("applyScanGa")) { Text(stringResource(R.string.apply_calculated_ga, gaShort), style = RecordType.Action) }`
     - Outside `scanCalculatedGaCard` (still inside Card 1, avoiding box-inside-box): keep the exact `weeks` / `days` / `redatingAdvice(calculatedGa.totalDays, weeks * 7L + days)` check; when `advice != null`, render `Surface(modifier = Modifier.fillMaxWidth(), color = if (isDarkTheme()) StatusModerateDarkBg else StatusModerateBg, border = BorderStroke(1.dp, if (isDarkTheme()) StatusModerateDarkBorder else StatusModerateBorder), shape = RecordShapes.Small)` containing `Text(text = stringResource(R.string.redating_hint, abs(advice.differenceDays), advice.thresholdDays), style = RecordType.Secondary, color = if (isDarkTheme()) StatusModerateDarkText else StatusModerateText, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).testTag("redatingHint"))`.
   - `gaWeeks` and `gaDays`:
     - Both use `EditorField(..., shape = RecordShapes.Small, textStyle = RecordType.KeyValue, externalLabel = true, singleLine = true, keyboardType = KeyboardType.Number, imeAction = ImeAction.Next / Done)` keeping exact `error` flags and `tag = "gaWeeks"` / `"gaDays"`.
     - Render side-by-side (`Row(horizontalArrangement = Arrangement.spacedBy(8.dp))`, each `Box(Modifier.weight(1f))`) when `!narrowStacked`, and in `Column(verticalArrangement = Arrangement.spacedBy(12.dp))` when `narrowStacked` (`fontScale > 1.3f`).

4. **Card 2 (`Fetus, placenta and amniotic fluid`, `ultrasoundFindingsCard`)**:
   - Compute `val filledCount = findings.count { it.value.isNotBlank() }`.
   - Wrap in `RecordSectionCard(title = stringResource(R.string.ultrasound_section_fetus_placenta_liquor), family = obstetricFamily, modifier = Modifier.testTag("ultrasoundFindingsCard"), action = { Text(text = stringResource(R.string.ultrasound_findings_progress, filledCount, findings.size), style = RecordType.FieldLabel, color = obstetricFamily.label, modifier = Modifier.padding(end = 12.dp)) })`.
   - Keep `findings.forEachIndexed { index, finding -> ... }` and `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)` when `index > 0`.
   - When `openFindingKey == finding.key`:
     - `Column(modifier = Modifier.fillMaxWidth().testTag("findingOpen-${finding.key}"), verticalArrangement = Arrangement.spacedBy(6.dp))` containing:
       ```kotlin
       ChoiceGroup(
           label = finding.label,
           selected = finding.value,
           choices = finding.choices,
           enabled = enabled,
           tagPrefix = finding.tagPrefix,
           onSelected = { /* EXACT existing onEvent + nextBlankFinding + openFindingKey = nextKey */ },
           labelStyle = RecordType.FieldLabel,
           labelColor = obstetricFamily.label,
           useRecordChip = true,
       )
       ```
   - When `openFindingKey != finding.key`:
     - `SummaryRow(label = finding.label, value = finding.value, tag = "findingRow-${finding.key}", onClick = { openFindingKey = finding.key }, useRecordStyle = true, trailingActionText = if (narrowStacked) null else stringResource(if (finding.value.isBlank()) R.string.ultrasound_finding_choose else R.string.action_edit), showDivider = false)`.

5. **Card 3 (`Fetal biometry`, `biometryCard`)**:
   - Replace `SectionCard` with `OutlinedCard(modifier = Modifier.fillMaxWidth().testTag("biometryCard"), shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))`.
   - `CompositionLocalProvider(LocalFieldFamily provides obstetricFamily)` inside `Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(RecordSpacing.M))`:
     - Header `Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically)`:
       - `Column(Modifier.weight(1f).padding(vertical = 6.dp))`:
         - `Text(stringResource(R.string.ultrasound_biometry_section), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })`
         - `Text(stringResource(R.string.ultrasound_biometry_summary), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)` (replaces `11.sp` `labelSmall`!)
       - `SectionTextAction(text = stringResource(if (biometryExpanded) R.string.action_collapse else R.string.action_expand), contentDescription = stringResource(if (biometryExpanded) R.string.action_collapse else R.string.action_expand), onClick = { biometryExpanded = !biometryExpanded }, modifier = Modifier.testTag("toggleBiometry"))` (drops `"▲"` / `"▼"` triangles!).
     - Inside `if (biometryExpanded)` (`Column(Modifier.fillMaxWidth().padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(RecordSpacing.M))`):
       - Render the 3 pairs (`fetalHeartRate` + `efw`, `biometryBpd` + `biometryHc`, `biometryAc` + `biometryFl`) with `externalLabel = true`, `shape = RecordShapes.Small`, `textStyle = RecordType.KeyValue`, `singleLine = true`, `imeAction = ImeAction.Next` (and `ImeAction.Done` on `biometryFl`), preserving exact `keyboardType` (`Number` for FHR/EFW, `Decimal` for BPD/HC/AC/FL) and exact tags.
       - Render each pair side-by-side (`Row(spacedBy(8.dp))`, `Modifier.weight(1f)`) when `!narrowStacked`, and stacked vertically (`Column(spacedBy(12.dp))`) when `narrowStacked` (`fontScale > 1.3f`).

6. **Card 4 (`Impression`) & Lock footer removal**:
   - `RecordSectionCard(title = stringResource(R.string.scan_impression), family = obstetricFamily)`:
     - `val impressionLabel = stringResource(R.string.scan_impression)`
     - `EditorField(value = draft.values.impression, onValueChange = { ... }, label = "", tag = "impression", modifier = Modifier.semantics { contentDescription = impressionLabel }, enabled = enabled, singleLine = false, shape = RecordShapes.Small, textStyle = RecordType.FieldValue, externalLabel = false)` (avoids repeating `"Impression"` twice inside the card while preserving TalkBack and `.testTag("impression")` on `OutlinedTextField`).
     - `OutlinedButton` (`impressionFromChoices`): `shape = RecordShapes.Small`, `border = BorderStroke(1.dp, if (enabled && generatedImpression.isNotBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant)`, `Modifier.minimumTouchTarget().testTag("impressionFromChoices")`, `Text(stringResource(R.string.impression_from_choices), style = RecordType.Action)`.
   - **Delete** the bottom `Icons.Default.Lock` row and `"Ultrasound records and scans encrypted locally (AES-256 GCM) · Hardware Vault Security"` text (`11.sp` removal, matching Slices 12, 13, 14).

7. **Card 5 (`More: photos and visit link`, `ultrasoundMoreToggle`, `ultrasoundMoreSection`)**:
   - Replace the `Surface(tonalElevation = 1.dp) { SummaryRow(...) }` toggle bar with:
     ```kotlin
     OutlinedCard(
         onClick = { moreExpanded = !moreExpanded },
         modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("ultrasoundMoreToggle"),
         shape = RecordShapes.Large,
         colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
         border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
     ) {
         Row(
             modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
             horizontalArrangement = Arrangement.SpaceBetween,
             verticalAlignment = Alignment.CenterVertically,
         ) {
             Text(
                 text = stringResource(R.string.ultrasound_more),
                 style = RecordType.CardName,
                 color = MaterialTheme.colorScheme.onSurface,
                 modifier = Modifier.weight(1f).padding(end = 8.dp),
             )
             Text(
                 text = stringResource(if (moreExpanded) R.string.more_close else R.string.more_open),
                 style = RecordType.Action,
                 color = MaterialTheme.colorScheme.primary,
             )
         }
     }
     ```
   - Inside `if (moreExpanded)` (`Column(Modifier.fillMaxWidth().testTag("ultrasoundMoreSection"), verticalArrangement = Arrangement.spacedBy(12.dp))`):
     - **`scanAttachmentsCard`**: `RecordSectionCard(title = stringResource(R.string.scan_attachments), family = obstetricFamily, modifier = Modifier.testTag("scanAttachmentsCard"))`:
       - Empty: `Text(stringResource(R.string.no_photos_attached), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`.
       - Non-empty (`draft.values.imageUris.forEachIndexed`): replace `11.sp` pink chips with clean `Surface(color = obstetricFamily.container, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().testTag("attachmentItem-$index"))` holding `Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 12.dp, end = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically)`:
         - `Text(stringResource(R.string.scan_photo_n, index + 1), style = RecordType.KeyValue, color = MaterialTheme.colorScheme.onSurface)`
         - `IconButton(onClick = <exact IMAGE_URIS removal>, enabled = enabled, modifier = Modifier.minimumTouchTarget().testTag("removePhoto-$index")) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_photo), tint = MaterialTheme.colorScheme.error) }`
       - Photo source buttons (`Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp))`):
         - `Button(onClick = { showCamera = true }, enabled = enabled, shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumTouchTarget().testTag("captureScanPhotoCamera")) { Text(stringResource(R.string.attachment_take_photo), style = RecordType.Action, textAlign = TextAlign.Center) }`
         - `OutlinedButton(onClick = { photoPickerLauncher.launch(...) }, enabled = enabled, shape = RecordShapes.Small, border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumTouchTarget().testTag("addScanPhoto")) { Text(stringResource(R.string.attachment_gallery), style = RecordType.Action, textAlign = TextAlign.Center) }`
     - **`linkedVisitCard`**: `RecordSectionCard(title = stringResource(R.string.link_to_visit), family = obstetricFamily, modifier = Modifier.testTag("linkedVisitCard"))`:
       - Linked branch (`draft.values.linkedVisitId.isNotBlank()`):
         - Title: `Text(stringResource(R.string.ultrasound_linked_visit_title, ltrIsolate(linkedVisit?.date ?: draft.values.linkedVisitId)), style = RecordType.KeyValue, color = MaterialTheme.colorScheme.onSurface)`
         - Optional vitals line (when `bloodPressure != null || weight != null`): `Text(stringResource(R.string.ultrasound_linked_visit_vitals, ltrIsolate(linkedVisit.bloodPressure ?: "-"), ltrIsolate(formatWeightKg(linkedVisit.weight) ?: "-")), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)` (no middle dot!).
         - `OutlinedButton(onClick = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, "")) }, enabled = enabled, shape = RecordShapes.Small, border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.minimumTouchTarget().testTag("unlinkVisit")) { Text(stringResource(R.string.unlink_visit), style = RecordType.Action) }`.
       - Unlinked branch:
         - `Text(stringResource(R.string.no_linked_visit), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`
         - `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp))` of `RecordFilterChip(selected = false, label = visit.date, onClick = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, visit.id)) }, tag = "linkVisit-${visit.id}", enabled = enabled, minTouchTarget = true)`.

8. **Bottom actions**:
   - Replace `EditorActions(...)` with:
     ```kotlin
     SheetCancelSaveRow(
         enabled = enabled,
         saving = state.savePhase == SavePhase.PENDING,
         saveEnabled = enabled,
         savingLabel = stringResource(if (state.savePhase == SavePhase.PENDING) R.string.saving_label else R.string.save),
         onCancel = { onEvent(AncEvent.CancelEditor) },
         onSave = { onEvent(AncEvent.SubmitUltrasound) },
         cancelTag = "cancelUltrasound",
         saveTag = "saveUltrasound",
         spinnerTag = "ultrasoundSaveSpinner",
     )
     ```

## 4. `ZoomableImage.kt` (same signature)

Do NOT touch `ZOOM_MIN`, `ZOOM_MAX`, `clampZoom`, `clampOffset`, `nextZoomOnDoubleTap`, `LaunchedEffect(resetKey)`, or either `pointerInput` gesture block.
Update only the `if (scale > 1f)` reset `Surface` (lines 166–195):
- `shape = RecordShapes.Small` (`10.dp`, replaces `RoundedCornerShape(8.dp)`)
- `color = MaterialTheme.colorScheme.surface` (solid surface, replaces `.surfaceContainerHigh.copy(alpha = 0.9f)`)
- `border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)` (replaces `outlineVariant`)
- Keep `.align(Alignment.BottomEnd).padding(8.dp).sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { this.contentDescription = resetLabel }.testTag("zoomResetButton").testTag("zoom_reset")` (`zoomResetButton` outermost so `AttachmentsRedesignTest` passes).
- Replace `Text(text = "1x", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)` with `Text(text = "1x", style = RecordType.Action, color = MaterialTheme.colorScheme.onSurface)`.

## 5. `UltrasoundPhotoViewerDialog.kt` (same signature)

Keep `if (scan.imageUris.isEmpty()) return`, `currentIndex`, `loadedBitmap` decoding, `Dialog` properties, `ZoomableImage(..., resetKey = currentIndex)`, and `onDismiss` unchanged.
1. `Card(..., modifier = Modifier.fillMaxWidth(0.95f).padding(vertical = 16.dp).heightIn(max = (LocalConfiguration.current.screenHeightDp - 32).dp).testTag("ultrasoundPhotoViewerDialog"), shape = RecordShapes.Large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface))` (no `elevation` argument).
2. Inner `Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp))`:
   - **Header** (`Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically)`):
     - Remove the `38.dp` `CircleShape` `PhotoCamera` surface.
     - Left `Column(Modifier.weight(1f))`:
       - `Text(text = stringResource(R.string.ultrasound_viewer_title, ltrIsolate(scan.date)), style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface)`
       - Subtitle (`RecordType.Secondary`, `onSurfaceVariant`): if `scan.gestationalAgeWeeks != null`, `stringResource(R.string.ultrasound_viewer_photo_of_ga, currentIndex + 1, scan.imageUris.size, ltrIsolate("${scan.gestationalAgeWeeks}+${scan.gestationalAgeDays ?: 0} wks"))`, else `stringResource(R.string.ultrasound_viewer_photo_of, currentIndex + 1, scan.imageUris.size)` (comma instead of middle dot!).
     - Right `IconButton(onClick = onDismiss, modifier = Modifier.minimumTouchTarget().testTag("closePhotoViewer"))` with `contentDescription = stringResource(R.string.close)`.
   - **Viewport Box**:
     - `Modifier.fillMaxWidth().height(280.dp).clip(RecordShapes.Small).background(Color.Black).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RecordShapes.Small)`.
     - Inside `ZoomableImage`: `contentDescription = stringResource(R.string.ultrasound_viewer_photo_cd, currentIndex + 1)`.
     - Unavailable branch: `Text(text = stringResource(R.string.image_unavailable), style = RecordType.FieldValue, color = Color.LightGray, modifier = Modifier.testTag("image_unavailable"))`.
   - **Pager Controls** (`if (scan.imageUris.size > 1)`):
     - `prevPhotoButton` and `nextPhotoButton` `IconButton`s add `.minimumTouchTarget()` and use `stringResource(R.string.previous_photo)` / `stringResource(R.string.next_photo)`.
     - Center indicator dots: remove `.clickable { currentIndex = idx }` and add `Modifier.clearAndSetSemantics {}` on the dots `Row` so there are no `8.dp` clickable targets.
   - **Biometrics Summary** (replacing `FactBadge` `11.sp` pills):
     - Build the exact same 5 optional items in order (FHR `unit_bpm`, EFW `"${scan.estimatedFetalWeight} g"`, BPD `unit_mm`, FL `unit_mm`, Presentation) and render them in a 3-column `Column(spacedBy(10.dp))` / `Row(spacedBy(12.dp))` grid of `PlainField(label, value, modifier = Modifier.weight(1f), emphasis = isMeasurement)` (`2` columns if `LocalDensity.current.fontScale > 1.3f`), deleting private `FactBadge`.
   - **Close Button**:
     - `Button(onClick = onDismiss, shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumTouchTarget().testTag("dismissPhotoViewer")) { Text(stringResource(R.string.close), style = RecordType.Action) }`.

## Acceptance checklist

1. Every existing `testTag` kept on its exact node:
   `ultrasoundEditor`, `editorError`, `ultrasoundLiveSummary`, `fillNormalScan`, `clearFindings`, `scanDate`, `scanDateCalendarButton`, `scanDateTodayButton`, `scanCalculatedGaCard`, `redatingHint`, `applyScanGa`, `gaWeeks`, `gaDays`, `ultrasoundFindingsCard`, `findingOpen-fetalNumber`, `findingOpen-presentation`, `findingOpen-placenta`, `findingOpen-liquor`, `fetalNumberChoice-*`, `presentationChoice-*`, `placentaChoice-*`, `liquorChoice-*`, `findingRow-fetalNumber`, `findingRow-presentation`, `findingRow-placenta`, `findingRow-liquor`, `biometryCard`, `toggleBiometry`, `fetalHeartRate`, `efw`, `biometryBpd`, `biometryHc`, `biometryAc`, `biometryFl`, `impression`, `impressionFromChoices`, `ultrasoundMoreToggle`, `ultrasoundMoreSection`, `scanAttachmentsCard`, `attachmentItem-*`, `removePhoto-*`, `captureScanPhotoCamera`, `addScanPhoto`, `linkedVisitCard`, `unlinkVisit`, `linkVisit-*`, `saveUltrasound`, `zoomResetButton`, `ultrasoundPhotoViewerDialog`, `closePhotoViewer`, `image_unavailable`, `prevPhotoButton`, `nextPhotoButton`, `dismissPhotoViewer` (plus new `cancelUltrasound`, `ultrasoundSaveSpinner`).
2. Zero text nodes under `12.sp` across `UltrasoundEditor`, `ZoomableImage` (`1x` reset), and `UltrasoundPhotoViewerDialog`; zero `"1. "`/`"2. "`/`"3. "`/`"4. "` card prefixes, zero `"▲"`/`"▼"` triangles, and zero `"AES-256"` Lock footers.
3. Zero `8.dp`, `14.dp`, `20.dp`, or pill `999.dp` radii in edited screens; every shape is `RecordShapes.Small` (`10.dp`) or `RecordShapes.Large` (`16.dp`).
4. Every tappable control measures `>= 48.dp` height (`>= 47.9.dp` in instrumented checks) and fits inside `360.dp` at `1.0f`, `1.3f`, and `2.0f` font scale without horizontal overflow or label truncation (`"Estimated fetal weight (EFW)"`, `"GA weeks (0–45)"`, `"Take photo"`, `"Choose from gallery"`).
5. `scanCalculatedGaCard` and `applyScanGa` display `27+2 wks` (`shortWeeksText`); `redatingHint` renders in semantic amber when triggered; `nextBlankFinding` guided progression and `ultrasoundImpressionFromChoices` remain exact.
6. `ZoomableImage` keeps `1x` reset visible only when `scale > 1f`, resets on click, and passes `AttachmentsRedesignTest` pinch/pan/scroll isolation unchanged.
7. Dark theme (`AncTheme(darkTheme = true)`) and `LayoutDirection.Rtl` render cleanly in both `UltrasoundEditor` and `UltrasoundPhotoViewerDialog`.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected.
- `UltrasoundGuidedFlowTest` uses `testTag`s and `ClinicalVocabulary` / `ultrasoundImpressionFromChoices` strings; all kept.
- `AncWorkflowTest` checks `ultrasoundEditor`, `scanDate`, `gaWeeks`, `gaDays`, and `saveUltrasound` at `360.dp` and RTL; all kept.
- `AttachmentsRedesignTest` checks `zoomResetButton` in `ZoomableImage`; kept outermost.

### Must pass unchanged

- `app/src/androidTest/java/com/sherifradwan/ancnative/UltrasoundGuidedFlowTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/ScansTabRedesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/AttachmentsRedesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/DeleteUndoUiTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/DatingGaMarkerSurfacesTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/OwnerFixesLogicTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/UltrasoundImpressionTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/RedatingTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalCommandsTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/state/AncStateTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/state/DraftRecoveryTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/state/DraftMappingTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/RecordTokensContrastTest.kt`

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/UltrasoundEditorRedesignTest.kt`

Mount `UltrasoundEditor` and `UltrasoundPhotoViewerDialog` through `MainActivity` + `AncTheme` inside a `360.dp x 640.dp` host (matching `HistoryEditorsRedesignTest` / `ComposeTextAsserts`, single `setContent` per test):
- **Typography & legacy removal (`1.0f` and `1.3f`)**: walk semantics trees with `ultrasoundMoreSection` and `biometryCard` expanded; assert zero `TextLayoutResult` nodes have `fontSize < 12.sp`, and `"AES-256"`, `"1. Scan"`, `"3. Fetal"`, `"4. Clinical"`, `"▲"`, `"▼"` never appear.
- **360 dp & `2.0f` font scale ergonomics**: expand biometry and More; assert `"GA weeks (0–45)"`, `"Estimated fetal weight (EFW)"`, `"Take photo"`, and `"Choose from gallery"` are not ellipsized/clipped beyond `360.dp`, and `fillNormalScan`, `clearFindings`, `toggleBiometry`, `impressionFromChoices`, `ultrasoundMoreToggle`, `captureScanPhotoCamera`, `addScanPhoto`, `cancelUltrasound`, `saveUltrasound` all have height `>= 47.9.dp`.
- **Calculated GA & redating**: with a patient dated to `27+2 wks` and empty GA fields, assert `scanCalculatedGaCard` shows `"27+2 wks"` (not `"27w 2d"`) and clicking `applyScanGa` dispatches `GA_WEEKS = "27"` and `GA_DAYS = "2"`; with mismatched `gaWeeks = "20"`, `gaDays = "0"`, assert `redatingHint` displays.
- **Guided steps & validation errors**: confirm `fetalNumberChoice-1` (`Singleton`) advances `findingOpen-presentation`, `findingRow-fetalNumber` shows `"Singleton"` without downward chevron icon, and `ClinicalValidationError.REQUIRED_DATE` / `INVALID_GA_WEEKS` render `editorError` and field error states.
- **Photo attach & `UltrasoundPhotoViewerDialog`**: with 2 attached images, assert `attachmentItem-0` displays `"Photo 1"` at `16.sp`, `removePhoto-0` meets `>= 47.9.dp` and removes item 0, and `UltrasoundPhotoViewerDialog` at `360.dp` `2.0f` font scale renders `"Ultrasound scan, 2026-05-30"`, `"Photo 1 of 2, 20+0 wks"`, `PlainField` biometry values (`"148 bpm"`, `"49 mm"`), and `dismissPhotoViewer` without clipping.
- **Dark theme & RTL**: mount `UltrasoundEditor` (`AncTheme(darkTheme = true)`, Arabic patient `"منى رجب"`, and `LayoutDirection.Rtl`) and verify zero overlap or overflow.

## Risk

**Medium.** None of the edited files are level 3, yet `UltrasoundEditor.kt` carries delicate interaction state (`openFindingKey` guided auto-advance, `biometryExpanded`, `moreExpanded`), exact `.testTag(...)` bindings on `OutlinedTextField`s, `redatingAdvice` math, and `ImportScanPhoto` / `IMAGE_URIS` wiring; and `ZoomableImage.kt` is shared with level-3 `ClinicalAttachmentViewerDialog.kt` (where keeping `.testTag("zoomResetButton")` outermost and leaving `pointerInput` untouched is critical). Route through clinical-safety review and independent test review before merge.
