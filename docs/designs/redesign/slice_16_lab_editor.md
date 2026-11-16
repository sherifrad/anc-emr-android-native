# Slice 16: Lab editor and lab batch

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s16_lab_editor.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt` (`RecordType`, `RecordShapes`, `RecordSpacing`, `FieldFamilies`, `LocalFieldFamily`, `ltrIsolate`), `ui/components/RecordSectionCard.kt` (`RecordSectionCard`, `SectionTextAction`), `ui/components/FormFieldLabel.kt`, `ui/components/RecordFilterChip.kt`, `ui/components/StatusTag.kt` (`StatusTag`, `StatusKind`, `StatusWord`, `BandChip`, `StatusTone`), `ui/components/PatientNameText.kt`, `ui/components/MetricCell.kt` (`parityText`), `ui/patienthub/components/EditorSheetParts.kt` (`SheetCancelSaveRow`), `ui/Components.kt` (`EditorField`, `GenericEditorErrorCard`, `editorFieldColors`, `minimumTouchTarget`), `ui/PatientEditor.kt` (`EditorPage` with `useRecordStyle = true` from Slice 12, `ChoiceGroup` with `useRecordChip = true` from Slice 13), and `ui/AncDatePicker.kt` (`ClinicalDateField` with `externalLabel`, `useRecordStyle` from Slice 13). Read them; do not re-spec them.

- **Owner direction**: `antislop: DURING`, dials ENERGY 1 / RHYTHM 1 / MOTION 1 (pressed and focus states only, no new animation). Data entry must be fast, easy on the eyes, and not tiring (fewer taps, large targets, calm hierarchy).
- **Branch hygiene**: run `.agents/scripts/git_audit.sh` before starting and before pushing; update `docs/PROJECT_STATE.json` + run `.agents/scripts/project_state.py render` + update `.agents/pipeline/HANDOFF_AGY.md` in the same commit on `redesign-2026-11-15`.
- **Never touch**:
  - `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`
  - `storage/ClinicalCommands.kt` (`SaveLabInvestigationCommand`), `storage/LabBatchCommands.kt` (`SaveLabBatchCommand`, `DeleteLabBatchCommand`, `RestoreLabBatchCommand`)
  - `state/UndoCoordinator.kt`, `state/AncReducer.kt`, `state/AncState.kt`, `state/AncEvent.kt`, `state/AncViewModel.kt`, `state/ClinicalDraftReducers.kt` (`startLabEditor`, `updateLabDraft`), `state/LabBatchState.kt` (`LabBatchDraft`, `LabBatchItem`, `LabBatchTab`, `groupLabBatchItems`), `state/DraftMapping.kt`
  - `model/ClinicalModels.kt` (`ClinicalVocabulary.testCategories`, `ClinicalVocabulary.labNamePresets`), `model/LabResultFlag.kt` (`LabResultFlag`, `labResultFlag`), `model/GestationalAgeEngine.kt`, `model/catalog/ClinicalCatalogModels.kt` (`resolveLabPanel`, `nextLabPanelTest`), `model/catalog/ClinicalCatalogRepository.kt`, `*Generator*`
  - `ui/patienthub/tabs/LabsTab.kt`, `ui/patienthub/components/LabReportItem.kt`, `ui/patienthub/model/LabReportGrouping.kt`
  - `ui/Components.kt` (`ClinicalPatientCard`), `ui/PatientEditor.kt` (level 3), `ui/AncDatePicker.kt`, `ui/ClinicalBadges.kt` (level 3), `ui/ClinicalFormat.kt` (level 3)
- **Level 3 & clinical safety invariants**:
  - **`ui/ClinicalPresets.kt` (140 lines) is Level 3 (`clinical_safety`, `PLAN.md` 0.13).** Display-only edit in this slice: restyle only the collapsible `TextButton` inside `QuickPresetChips` when `useRecordStyle == true` (`shape = RecordShapes.Small`, `RecordType.Action`). Do NOT modify `MEDICATION_FREQUENCY_LABELS`, `COMMON_MEDICATION_FREQUENCIES`, `medicationFrequencyLabelRes`, `medicationFrequencyLabel`, `medicationDoseRouteFrequency`, `PRESAVED_COLLAPSE_AFTER`, or `ClinicalPresets` object (`ClinicalPresetsTest` and all medication callers stay untouched).
  - `LabEditor.kt` and `LabBatchScreen.kt` read level-3 helpers (`ClinicalFormat.formatBloodGroup`, `ClinicalFormat.isRhNegative`, `ClinicalFormat.isHighRisk`, `ClinicalBadges.parseRiskTier`, `PatientEditor.EditorPage`, `PatientEditor.ChoiceGroup`, `ClinicalPresets.QuickPresetChips`) without altering any clinical rule. Because Slice 16 touches level-3 `ClinicalPresets.kt` and high-risk clinical lab entry, route the PR through frontier-tier clinical safety review and independent test review before merge.
  - **What must not change**:
    - **Validation and error highlighting**: `SaveLabInvestigationCommand.prepare` (`REQUIRED_DATE`, `INVALID_DATE`, `REQUIRED_CATEGORY`, `INVALID_CATEGORY`, `REQUIRED_NAME`, `REQUIRED_RESULT`), `LabBatchDraft.validationErrors()`, `REQUIRED_NAME` error state on `labName`, `REQUIRED_RESULT` error state on `labResult`, `REQUIRED_DATE` / `INVALID_DATE` error state on `labDate`, `GenericEditorErrorCard`, and `enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR` disable gating.
    - **Save paths and event dispatches**: `AncEvent.SubmitLab`, `AncEvent.SubmitLabAndAddAnother`, `AncEvent.CancelEditor`, `AncEvent.LabDraftChanged(LabDraftField.*, ...)`, `AncEvent.LabAbnormalChanged`, `catalogRepo.addLabCatalogItem(...)`, `catalogRepo.addLabCategoryGroup(...)`, `AncEvent.SubmitLabBatch`, `AncEvent.CancelLabBatch`, `AncEvent.LabBatchItemsAdded`, `AncEvent.LabBatchItemRemoved`, `AncEvent.LabBatchResultChanged`, `AncEvent.LabBatchRangeChanged`, `AncEvent.LabBatchFlagChanged`, `AncEvent.LabBatchNotesChanged`, `AncEvent.LabBatchTabSelected`, `AncEvent.LabBatchCursorMoved`, `AncEvent.LabBatchDateChanged`, `AncEvent.LabBatchLabNameChanged`.
    - **Calculations and queue behaviour**: `labResultFlag(result, referenceRange)`, `LaunchedEffect(draft.labId, draft.values.result, draft.values.referenceRange)` auto-abnormal flagging in `LabEditor`, `loadLabItemIntoDraft`, `isDipstickTest`, `DipstickChoices`, `panelRemainingIds` / `nextLabPanelTest` / `resolveLabPanel` chained queue progression (`clearPanelQueue()`), `batchInfoOpen` initial state `rememberSaveable { mutableStateOf(draft.values.date.isBlank()) }` (required by `AncWorkflowTest` so `labDate` is immediately visible on a new empty lab), `testSelectorOpen` initial state `rememberSaveable { mutableStateOf(draft.values.testName.isBlank()) }`, `qualitativeChoicesFor(testName)`, `isChoiceAbnormal(choice)`, `groupLabBatchItems(draft.items, groups)`, `emptyQualitative` filter and `"Normal"` / `"Negative"` defaults in `labBatchMarkNormal`, and `GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, ...)` (`isPostConception`).
    - **Stored canonical codes**: `ClinicalVocabulary.testCategories` (`"Hematology"`, `"Urinalysis"`, `"Biochemistry"`, `"Serology"`, `"Other"`), `ClinicalVocabulary.labNamePresets`, `DipstickChoices` (`"Normal"`, `"Trace"`, `"+"`, `"++"`, `"+++"`), `qualitativeChoicesFor` choices (`"Negative"`, `"Non-reactive"`, `"Reactive"`, `"0–2"`, `"3–5"`, `"6–10"`, `">10 /hpf"`), `LabBatchTab` values (`ADD`, `ENTER`, `REVIEW`), and catalog item/group IDs (`group_cbc`, `group_urine`, etc.).
- **Visual only**: Background-family (`FieldFamilies.background()`) visual grammar matching `LabsTab`, shared sand `LabPatientBanner`, unified `BatchInfoBar`, external `12.sp` labels, `10.dp`/`16.dp` corners, `48.dp` targets, `27+2 wks` GA formatting, clean sentence-case copy without decorative glyphs/ALL-CAPS/triangles, 2-row `LabEditor` footer, stacked panel buttons, and `StatusWord`/`RecordSectionCard` review rows. No schema change, no dropped fields, and no validation/save/calculation changes.

## Decisions for the owner (this slice)

1. **Shared `FieldBackgroundBg` `LabPatientBanner` and unified `BatchInfoBar` (replacing `ClinicalPatientCard(EDITOR_SUMMARY)` and duplicated header subtitles)**:
   Today both `LabEditor` and `LabBatchScreen` repeat `patient.displayName` twice, render 5 sub-`12.sp` rainbow pills (`ClinicalPatientCard(EDITOR_SUMMARY)` omits `A2` so `G4 P1 A2` becomes `G4 P1` and shows green `"Normal Risk"`), print `"GA 27w 2d"`, and duplicate `BatchInfoBar` (one private copy in `LabEditor.kt`, one in `DesignSystem.kt` with `ActionRose` `"Change ▾"`). **Recommended**: extract shared `LabPatientBanner` (`ui/LabEditorComponents.kt`) in `FieldBackgroundBg` (`#F1EDE4` light / `#36301F` dark) using `PatientNameText`, `StatusTag` only for Moderate/High risk, and `BandChip`s for `27+2 wks` (`shortWeeksText`), `G4 P1 A2` (`parityText`), and `A+` (`formatBloodGroup` + `StatusTone.High` if Rh-); pass empty `subtitle` to `EditorPage`; and unify on `DesignSystem.BatchInfoBar(..., useRecordStyle = true)` with `27+2 wks` and a plain `"Change"` `RecordType.Action` button (removing the private copy in `LabEditor.kt`).
2. **Compact sand readout strip, `RecordFilterChip` dipstick row, and collapsed test presets in `LabEditor`**:
   Today `LabEditor` draws a tall `surfaceVariant` box with a `36.sp` duplicate number + `999.dp` `11.sp` `LabResultFlagBadge` (`"Normal ✓"` / `"High ▲"` / `"Low ▼"`), stacks 5 full-width `56.dp` `BigChoiceButton`s (`~304 dp`) for dipstick tests, and shows all 28 catalog `QuickPresetChips` when `testSelectorOpen` is true (pushing `"Result"` far down the `360.dp` screen). **Recommended**: keep `labStoryDisplay` / `labStoryValue` / `labResultFlag` as a compact `FieldBackgroundBg` (`10.dp`) readout row with `22.sp` value + `BandChip` (`"Normal"` / `"High"` / `"Low"` without `"✓"`/`"▲"`/`"▼"`), replace the 5 stacked dipstick `BigChoiceButton`s with a `FlowRow` of `48.dp`-target `RecordFilterChip`s (`labResultPreset-0..4`), and pass `collapseAfter = PRESAVED_COLLAPSE_AFTER` (5) to `labPreset` `QuickPresetChips` when `selectedGroupName == null` (styling the `Show all 28` / `Show fewer` `TextButton` in level-3 `ClinicalPresets.kt` with `RecordShapes.Small` and `RecordType.Action`).
3. **2-row `MedicationSheet`-style bottom bar in `LabEditor`**:
   Today `LabEditor`'s `bottomBar` squeezes `Cancel`, `+ Add investigation (+)`, and `Save` into one `328.dp` row with `softWrap = false`, clipping `"Save and next test"` and `"+ Add investigation (+)"` at `1.3f`/`2.0f` font scale. **Recommended**: use `SheetCancelSaveRow` (`cancelLab` / `saveLab` / `labSaveSpinner`, 50/50 equal width, `48.dp` min height, `RecordShapes.Small`) above a full-width `48.dp` `OutlinedButton` (`addAnotherLab`) reading `"Save and add another"` (or `"Save and next test"` when `panelRemainingIds.isNotEmpty()`), matching `MedicationSheet` (Slice 11).
4. **Stacked `48.dp` panel actions on `LabBatchScreen` Add tab, plus 1-tap `labNamePresets` and `ClinicalDateField` in the batch metadata dialog**:
   Today `AddTab` places `Choose` (`40.dp` high) and `+ Add 20` (`40.dp` high) beside panel names (leaving `~110 dp` for `"First Trimester Routine Booking"`), overflows the non-wrapping `"Often added"` `Row` at `1.3f`, and gives the batch metadata dialog bare `OutlinedTextField`s without a calendar picker, `Today` chip, or lab presets. **Recommended**: give each panel a full-width header (title + count + comma preview) above a 50/50 `Row` of `48.dp` `RecordShapes.Small` buttons (`Choose` + `+ Add N`), wrap `"Often added"` in `FlowRow` with `RecordFilterChip` (`48.dp`), add an honest empty search message (`"No matching tests found"`), and upgrade the batch metadata dialog to `ClinicalDateField(tag = "labBatchDate", externalLabel = true, useRecordStyle = true, showTodayButton = true)` + `EditorField(tag = "labBatchLabName", externalLabel = true)` + `QuickPresetChips(labNamePresets, useRecordStyle = true)`.
5. **Sand/Primary styling across `EnterTab`, `ReviewTab`, `ChoosePanelDialog`, `48.dp` delete, and `LabBatchDraft.errors` visibility**:
   Today `LabBatchScreen` uses `ActionRose` on tabs/progress/links, ALL-CAPS `"HEMATOLOGY · TEST 1 OF 6"` (`11.sp`) and `SectionHeader` stripes, a `32.dp` remove button (`labBatchDeleteItem`), `999.dp` `11.sp` `LabResultPill`s, `"◀"`/`"▶"` triangles, and renders nothing when `LabBatchDraft.errors` is non-empty, while `LabEditor` appends the `11.sp` AES-256 Lock footer. **Recommended**: restyle tabs, `BatchSegmentedProgress` (`useRecordStyle = true`), `QualitativeChoiceButton` (`useRecordStyle = true`, `primaryContainer` + `1.5.dp` `primary` border), and `ReviewTab` (`RecordSectionCard`, `StatusWord("Pending" / "Normal" / "Abnormal")`, `48.dp` `Edit` `TextButton`, calm sand panel for `labBatchMarkNormal`); raise `labBatchDeleteItem` to `48.dp`; drop triangles, ALL-CAPS, `ActionRose`, and the `11.sp` Lock footer; keep `"${group.name} · ${group.testNames.size} elements"` in `ChoosePanelDialog` (`LabBatchUiTest` line 142); and add `GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)` at the top of `LabBatchScreen`.

## Lessons (apply all)

- **Minimum `12.sp` everywhere** (`PLAN.md` 0, 2): eliminate `labelSmall` (`11.sp`) in `LabEditor` (`pinnedLabHeaderTag`, AES-256 Lock footer), `LabBatchScreen` (`LabResultPill`, `"OFTEN ADDED"`, `EnterTab` header), `DesignSystem.SectionHeader` / `BatchInfoBar` (`labelMedium` `11.sp`), `LabEditor.LabResultFlagBadge` (`11.sp`), `LabEditor.labTestProgress` (`11.sp`), `LabEditor.labEditDetailsToggle` (`11.sp`), `LabEditor.labAddTestsCard` `"Filter by Category:"` (`11.sp`), and `ClinicalPatientCard(EDITOR_SUMMARY)` (`11.sp` badges).
- **Minimum `48.dp` width and height** (`minimumTouchTarget()`) on every interactive target: `labBatchChange`, `labBatchDone`, `cancelLab`, `saveLab`, `addAnotherLab`, `labEditDetailsToggle`, `labPreset-*`, `labPresetToggle`, `labResultPreset-*`, `labFlagToggle`, `labAddTestsToggle`, `allLabTestsChip`, `labGroupChip-*`, `saveToLabCodexButton`, `labCategoryChoice-*`, `labBatchBack`, `labBatchTab-ADD/ENTER/REVIEW`, `Choose` and `labBatchAdd-*` buttons, inline panel checklist rows, `"Often added"` chips, `labBatchAddOne-*`, `ChoosePanelDialog` checklist rows & buttons, `labBatchDeleteItem` (raised from `32.dp`), `QualitativeChoiceButton` (`52.dp`), `labBatchFlag` row, `labBatchPrev`, `labBatchNext`, `labBatchAddInline`, `labBatchEdit-*`, `labBatchMarkNormal`, `labBatchCancel`, `labBatchSave`.
- **Two corner radii only**: `RecordShapes.Small` (`10.dp`) on inputs, chips, tabs, buttons, progress bars, readout strip, and banners; `RecordShapes.Large` (`16.dp`) on `LabPatientBanner`, `BatchInfoBar`, `RecordSectionCard`, `morebar`, quick-action panels, error cards, and dialogs. Remove `2.dp`, `8.dp`, `12.dp`, `14.dp`, `20.dp`, `28.dp`, and `999.dp` pill shapes.
- **360 dp at `1.3f` and `2.0f` font scale**: stack `labUnit` and `labRefRange` vertically when `LocalDensity.current.fontScale > 1.3f`; allow `maxLines = 2` on `SheetCancelSaveRow`, `addAnotherLab`, `labBatchCancel`, `labBatchSave`, and batch tabs; keep `FlowRow` on all chip groups (`wrap = true` on `RecordFilterChip` where long test/panel names appear); place `Choose` and `+ Add N` below panel titles in a 50/50 row so panel titles never crush.
- **Keep every existing `testTag` on the exact semantic node**: keep `.testTag(...)` directly on `OutlinedTextField` inside `EditorField` (`labNameField`, `labName`, `labResult`, `labUnit`, `labRefRange`, `labNotes`, `labBatchLabName`) and `ClinicalDateField` (`labDate`, `labBatchDate`), and preserve `labDateCalendarButton`, `labDateTodayButton`, `labBatchDateCalendarButton`, `labBatchDateTodayButton`, and all existing batch/single tags.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Lines | Change |
|---|---:|---|
| `res/values/strings.xml` | 789 | Normalize 4 single-caller values (`lab_editor`, `add_another_test`, `lab_batch_change`, `lab_batch_pending`); add 19 strings in section 1 (plus `saving_label` if Slice 12/13/15 has not landed) |
| `ui/ClinicalPresets.kt` (level 3) | 140 | Display-only update on `QuickPresetChips` collapsible `TextButton` when `useRecordStyle == true` (`RecordShapes.Small`, `RecordType.Action`) |
| `ui/DesignSystem.kt` | 529 | Add backward-compatible `useRecordStyle: Boolean = false` params on `BatchInfoBar`, `BatchSegmentedProgress`, and `QualitativeChoiceButton` (keep defaults identical for Slice 21 cleanup) |
| `ui/LabEditorComponents.kt` | new | Extract shared `LabPatientBanner`, `LabMetadataDialog` (or shared lab metadata helper), and `LabReadoutStrip` using `FieldFamilies.background()`, `PatientNameText`, `StatusTag`, `BandChip`, `ClinicalDateField`, `QuickPresetChips` |
| `ui/LabEditor.kt` | 778 | Restyle with `EditorPage(..., useRecordStyle = true)`, `LabPatientBanner`, unified `BatchInfoBar(..., useRecordStyle = true)`, `RecordSectionCard(FieldFamilies.background())`, compact `labStoryDisplay`, `RecordFilterChip` dipstick row, collapsed `labPreset`, `ChoiceGroup(..., useRecordChip = true)`, 2-row footer (`SheetCancelSaveRow` + `addAnotherLab`), and drop Lock footer and private `BatchInfoBar` |
| `ui/LabBatchScreen.kt` | 987 | Restyle with `EditorPage(..., useRecordStyle = true)`, `GenericEditorErrorCard`, `LabPatientBanner`, `48.dp` `labBatchBack` + `BatchInfoBar(..., useRecordStyle = true)`, `RecordShapes.Small` 3-tab strip, stacked `48.dp` panel actions, `FlowRow` `"Often added"`, restyled `ChoosePanelDialog` + upgraded metadata dialog, `BatchSegmentedProgress(..., useRecordStyle = true)`, `48.dp` `labBatchDeleteItem`, `QualitativeChoiceButton(..., useRecordStyle = true)`, `RecordSectionCard` + `StatusWord` review rows, sand `labBatchMarkNormal` card, and `RecordShapes.Small` footer |

## 1. Strings (`app/src/main/res/values/strings.xml`)

Change values (keys unchanged; grep confirmed zero tests assert the old values and all four keys are used only in `LabEditor` / `LabBatchScreen` / `DesignSystem.BatchInfoBar`):
- `lab_editor`: `"Laboratory Investigation"` -> `"Laboratory investigation"`
- `add_another_test`: `"Add investigation (+)"` -> `"Save and add another"`
- `lab_batch_change`: `"Change ▾"` -> `"Change"`
- `lab_batch_pending`: `"pending"` -> `"Pending"`

Add:
```xml
<string name="lab_save_and_next">Save and next test</string>
<string name="lab_chained_batch">Chained batch</string>
<string name="lab_common_tests_group">Common tests: %1$s</string>
<string name="lab_dipstick">Dipstick</string>
<string name="lab_dipstick_result">Dipstick result</string>
<string name="lab_flag_normal">Normal</string>
<string name="lab_flag_high">High</string>
<string name="lab_flag_low">Low</string>
<string name="lab_dipstick_auto_flag_hint">Trace or higher marked abnormal</string>
<string name="lab_range_auto_flag_hint">Auto-flagged from reference range</string>
<string name="lab_filter_by_category">Filter by category</string>
<string name="lab_all_tests_count">All (%1$d)</string>
<string name="lab_save_template_prompt">New test template: save to test list?</string>
<string name="lab_add_template">Add template</string>
<string name="lab_set_date">Set date</string>
<string name="lab_ref_from_report">%1$s, ref %2$s (from report)</string>
<string name="lab_ref_only_from_report">ref %1$s (from report)</string>
<string name="lab_batch_panels_section">Common panels</string>
<string name="lab_batch_panel_summary">%1$d elements: %2$s</string>
<string name="lab_batch_no_search_matches">No matching tests found</string>
<string name="lab_batch_panel_name">Panel name</string>
<string name="lab_batch_enter_progress">%1$s, test %2$d of %3$d</string>
<string name="lab_batch_default_category">Test</string>
<string name="lab_batch_unit_range">%1$s, range %2$s</string>
<string name="lab_batch_reference_range_hint">Reference range (%1$s)</string>
<string name="lab_batch_remove_item">Remove %1$s</string>
<string name="lab_batch_mark_normal_hint">Remaining dipstick or qualitative tests without a value can be set to normal in one tap.</string>
```
If Slice 12/13/15 has not landed yet, also ensure `<string name="saving_label">Saving…</string>` is present.

**Do NOT alter** `lab_batch_title`, `lab_batch_tab_add`, `lab_batch_tab_enter`, `lab_batch_tab_review`, `lab_batch_save_n`, `lab_batch_save_one`, `lab_batch_review_zero_results`, `lab_batch_choose`, `lab_batch_choose_tests`, `lab_batch_choose_hint`, `lab_batch_save_selection_as_panel`, `lab_batch_add_n`, `lab_batch_add_n_selected`, `lab_batch_mark_empty_normal`, `lab_batch_elements_count`, `lab_batch_from_report`, `lab_batch_often_added`, `lab_batch_add_other`, `lab_batch_add_single_test`, `lab_batch_search_placeholder`, `lab_batch_edit_metadata`, `lab_batch_empty_add_first`, `lab_batch_add`, `lab_batch_edit`, `lab_batch_abnormal`, `lab_batch_no_lab`, `add_test_to_report`, `test_date`, `test_name`, `test_result`, `unit`, `reference_range`, `is_abnormal`, `lab_name`, `common_tests`, `test_category`, `new_test`, `edit_test_details`, `add_tests`, `more_open`, `more_close`, `lab_test_progress`, `lab_test_progress_category`, `lab_abnormal`, `lab_group_normal`, `show_all_count`, or `show_fewer`.

## 2. Shared helpers (`ClinicalPresets.kt` [level 3], `DesignSystem.kt`, `LabEditorComponents.kt`)

### 2.1 `QuickPresetChips` (`ui/ClinicalPresets.kt`, level 3, display only)
Keep signature, `MEDICATION_FREQUENCY_LABELS`, `COMMON_MEDICATION_FREQUENCIES`, `medicationFrequencyLabel*`, `medicationDoseRouteFrequency`, `PRESAVED_COLLAPSE_AFTER`, and `ClinicalPresets` untouched.
Inside `if (collapsible)`:
- `TextButton(onClick = { expanded = !expanded }, shape = if (useRecordStyle) RecordShapes.Small else ButtonDefaults.textShape, modifier = Modifier.minimumTouchTarget().testTag(toggleTag))`
- Inside label: `Text(text = ..., style = if (useRecordStyle) RecordType.Action else LocalTextStyle.current)` (when `useRecordStyle == false`, renders identically to today).

### 2.2 Backward-compatible updates (`ui/DesignSystem.kt`)
Add trailing `useRecordStyle: Boolean = false` to these three composables (keep `useRecordStyle == false` rendering byte-for-byte identical):
1. **`BatchInfoBar`**:
   - Add `useRecordStyle: Boolean = false`, `enabled: Boolean = true`.
   - When `useRecordStyle == true`:
     - `OutlinedCard(onClick = onChangeClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).let { if (tag != null) it.testTag(tag) else it }, shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))`
     - Inside `Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically)`:
       - Left `Column(Modifier.weight(1f).padding(end = 8.dp), spacedBy(2.dp))`:
         - Top line: `val dateDisplay = dateText.ifBlank { stringResource(R.string.lab_set_date) }`; `val topText = if (!gaText.isNullOrBlank()) "$dateDisplay, ${ltrIsolate(gaText)}" else dateDisplay`; `Text(topText, style = RecordType.KeyValue, color = MaterialTheme.colorScheme.onSurface)` (`maxLines = 2`, wraps cleanly at `2.0f`).
         - Second line: `Text(labName?.ifBlank { null } ?: stringResource(R.string.lab_batch_no_lab), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`.
       - Right action: `Box(Modifier.minimumTouchTarget().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.lab_batch_change), style = RecordType.Action, color = MaterialTheme.colorScheme.primary) }` (no downward chevron icon, no `ActionRose`).
2. **`BatchSegmentedProgress`**:
   - Add `useRecordStyle: Boolean = false`.
   - When `useRecordStyle == true`: `Row(modifier.fillMaxWidth().padding(vertical = 4.dp).clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically)` drawing `Box` segments with `height(if (i == current) 8.dp else 6.dp)`, `clip(RecordShapes.Small)`, color `if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant` (no `ActionRose`, no `2.dp` radius).
3. **`QualitativeChoiceButton`**:
   - Add `useRecordStyle: Boolean = false`, `enabled: Boolean = true`.
   - When `useRecordStyle == true`: `Surface(onClick = onClick, enabled = enabled, shape = RecordShapes.Small, color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), modifier = modifier.fillMaxWidth().heightIn(min = 52.dp).let { if (tag != null) it.testTag(tag) else it })` with `RecordType.FieldValue.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)` in `if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface` and `Icons.Default.Check` tinted `onPrimaryContainer` when selected.

### 2.3 Shared Lab UI (`ui/LabEditorComponents.kt`, new)
- **`LabPatientBanner(patient: DemoPatient, asOfDate: LocalDate, modifier: Modifier = Modifier)`**:
  - `val bgFamily = FieldFamilies.background()`
  - `Surface(modifier = modifier.fillMaxWidth(), shape = RecordShapes.Large, color = bgFamily.container, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))`
  - Inside `Column(Modifier.padding(12.dp), spacedBy(8.dp))`:
    - Top `Row(Modifier.fillMaxWidth(), Alignment.CenterVertically, spacedBy(8.dp))`:
      - `PatientNameText(name = patient.displayName, style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))`
      - Risk tag ONLY if High or Moderate (`val isHigh = isHighRisk(patient.riskLevel); val isModerate = !isHigh && parseRiskTier(patient.riskLevel) == PatientRiskTier.MEDIUM`): `StatusTag(kind = if (isHigh) StatusKind.HighRisk else StatusKind.ModerateRisk)`. Never render `"Normal risk"`.
    - Bottom `FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp))` inside `CompositionLocalProvider(LocalFieldFamily provides bgFamily)`:
      - `val gaResult = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, asOfDate)?.takeIf { it.isPostConception } ?: patient.dating?.calculateGestationalAgeResult()?.takeIf { it.isPostConception }`
      - If `gaResult != null`: `BandChip(ltrIsolate(gaResult.shortWeeksText), tone = null)` (`"27+2 wks"`).
      - `BandChip(ltrIsolate(parityText(patient.gravida, patient.parity, patient.abortion)), tone = null)` (`"G4 P1 A2"`).
      - `formatBloodGroup(patient.bloodGroupText)?.let { bg -> BandChip(ltrIsolate(bg), tone = if (isRhNegative(patient.bloodGroupText)) StatusTone.High else null) }`.
- **`LabReadoutStrip(resultText: String, unitText: String, flagContent: (@Composable () -> Unit)? = null, modifier: Modifier = Modifier, valueTag: String? = null)`**:
  - `Surface(modifier = modifier.fillMaxWidth(), shape = RecordShapes.Small, color = FieldFamilies.background().container)`
  - `Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically)`:
    - Left `Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f).padding(end = 8.dp))`:
      - `Text(text = resultText.trim().ifBlank { "-" }, style = RecordType.HeaderName, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.let { if (valueTag != null) it.testTag(valueTag) else it })`
      - If `unitText.isNotBlank()`: `Spacer(Modifier.width(4.dp))` + `Text(text = unitText, style = RecordType.Secondary, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))`
    - Right: `flagContent?.invoke()`

## 3. Single lab editor (`ui/LabEditor.kt`, same signature)

Keep `panelRemainingIds`, `panelTotalCount`, `currentPanelIndex`, `currentPanelItemId`, `currentLabId`, `selectedGroupName`, `batchInfoOpen`, `testSelectorOpen`, `clearPanelQueue()`, `LaunchedEffect` auto-flagger, `LaunchedEffect(draft.labId)` chained queue, `loadLabItemIntoDraft`, `isDipstickTest`, and `DipstickChoices` intact. Define `val bgFamily = FieldFamilies.background()` and `val narrowStacked = LocalDensity.current.fontScale > 1.3f`. Delete the private `BatchInfoBar` composable and `LabResultFlagBadge` helper from `LabEditor.kt`.

1. **Page header, patient banner, metadata bar & errors**:
   - `EditorPage(title = stringResource(R.string.lab_editor), subtitle = "", tag = "labEditor", useRecordStyle = true, bottomBar = { ... })`.
   - `GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)`.
   - `LabPatientBanner(patient = patient, asOfDate = parseIsoDateOrNull(draft.values.date) ?: LocalDate.now())`.
   - Unified `BatchInfoBar(dateText = draft.values.date, gaText = calculatedGa?.shortWeeksText, labName = draft.values.labName, onChangeClick = { batchInfoOpen = true }, modifier = Modifier.fillMaxWidth(), tag = "labBatchChange", useRecordStyle = true, enabled = enabled)` (never pass `displayText` `"27w 2d"`!).
2. **Metadata dialog (`if (batchInfoOpen)`)**:
   - `AlertDialog(onDismissRequest = { batchInfoOpen = false }, shape = RecordShapes.Large, containerColor = MaterialTheme.colorScheme.surface, title = { Text(stringResource(R.string.lab_name), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.primary) }, ...)`
   - Inside `CompositionLocalProvider(LocalFieldFamily provides bgFamily)`:
     - `EditorField(..., tag = "labNameField", externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`
     - `QuickPresetChips(title = stringResource(R.string.common_presets), items = ClinicalVocabulary.labNamePresets, enabled = enabled, onSelect = { ... }, tagPrefix = "labNamePreset", useRecordStyle = true)`
     - `ClinicalDateField(..., label = stringResource(R.string.test_date), tag = "labDate", enabled = enabled, error = <unchanged>, showTodayButton = true, externalLabel = true, useRecordStyle = true)`
     - If `calculatedGa != null`: `Surface(color = bgFamily.container, shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().testTag("labCalculatedGaCard"))` showing `Text(stringResource(R.string.calculated_ga_at_visit, ltrIsolate(calculatedGa.shortWeeksText)), style = RecordType.KeyValue, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))`.
   - Confirm button: `Button(onClick = { batchInfoOpen = false }, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("labBatchDone")) { Text(stringResource(R.string.done), style = RecordType.Action) }`.
3. **2-row `bottomBar` (matching `MedicationSheet`)**:
   - `Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp))`:
     - `SheetCancelSaveRow(enabled = enabled, saving = state.savePhase == SavePhase.PENDING, saveEnabled = enabled, savingLabel = stringResource(if (state.savePhase == SavePhase.PENDING) R.string.saving_label else R.string.save), onCancel = { onEvent(AncEvent.CancelEditor) }, onSave = { onEvent(AncEvent.SubmitLab) }, cancelTag = "cancelLab", saveTag = "saveLab", spinnerTag = "labSaveSpinner")`
     - `OutlinedButton(onClick = { onEvent(AncEvent.SubmitLabAndAddAnother) }, enabled = enabled, shape = RecordShapes.Small, border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumTouchTarget().testTag("addAnotherLab")) { Text(if (panelRemainingIds.isNotEmpty()) stringResource(R.string.lab_save_and_next) else stringResource(R.string.add_another_test), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) }`
4. **Main test card (`labTestCard`)**:
   - Build `labTestCard` with `OutlinedCard(shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().testTag("labTestCard"))` and `CompositionLocalProvider(LocalFieldFamily provides bgFamily)` (matching `RecordSectionCard` paddings `start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp`) so `.testTag("labTestTitle")` stays directly on the title `Text` (`RecordType.SectionTitle`, `primary`, `.semantics { heading() }`, `LabEntryDesignTest` lines 108 to 113), paired with `SectionTextAction(text = if (testSelectorOpen) stringResource(R.string.more_close) else stringResource(R.string.edit_test_details), contentDescription = ..., onClick = { testSelectorOpen = !testSelectorOpen }, modifier = Modifier.testTag("labEditDetailsToggle"))`.
   - Inside card top (above fields):
     - Optional chained-batch `Row` (when `panelRemainingIds.isNotEmpty()` or alongside `progressText`): `BandChip(text = stringResource(R.string.lab_chained_batch), tone = null, modifier = Modifier.testTag("pinnedLabHeaderTag"))`.
     - `progressText` (using sentence-case `draft.values.testCategory.trim()`, never `.uppercase()`): `Text(text = progressText, style = RecordType.FieldLabel, color = bgFamily.label, modifier = Modifier.testTag("labTestProgress"))`.
     - If `subtitle.isNotEmpty() && !testSelectorOpen`: `Text(text = subtitle, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("labTestSubtitle"))` (using `lab_ref_from_report` / `lab_ref_only_from_report` / `lab_dipstick` without middle dots).
   - Inside `if (testSelectorOpen)`:
     - `EditorField(..., tag = "labName", externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`
     - `QuickPresetChips(title = if (selectedGroupName != null) stringResource(R.string.lab_common_tests_group, selectedGroupName!!) else stringResource(R.string.common_tests), items = displayedItems.map { it.testName }, enabled = enabled, onSelect = <unchanged>, tagPrefix = "labPreset", collapseAfter = if (selectedGroupName == null) PRESAVED_COLLAPSE_AFTER else null, useRecordStyle = true)`
   - Inside `if (isDipstick)`:
     - `FormFieldLabel(stringResource(R.string.lab_dipstick_result))`
     - `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp))`: `DipstickChoices.forEachIndexed { index, choice -> RecordFilterChip(selected = draft.values.result.trim() == choice, label = choice, onClick = <unchanged>, tag = "labResultPreset-$index", enabled = enabled, minTouchTarget = true) }`
   - Inside `else` (quantitative):
     - `LabReadoutStrip(resultText = draft.values.result, unitText = unitText, modifier = Modifier.testTag("labStoryDisplay"), valueTag = "labStoryValue", flagContent = if (resultFlag != LabResultFlag.UNKNOWN) { { BandChip(text = when (resultFlag) { LabResultFlag.NORMAL -> stringResource(R.string.lab_flag_normal); LabResultFlag.HIGH -> stringResource(R.string.lab_flag_high); LabResultFlag.LOW -> stringResource(R.string.lab_flag_low); LabResultFlag.UNKNOWN -> "" }, tone = if (resultFlag == LabResultFlag.NORMAL) StatusTone.Neutral else StatusTone.High, modifier = Modifier.testTag("labResultFlag")) } } else null)`
     - `EditorField(..., tag = "labResult", externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.KeyValue, keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)`
     - `labUnit` and `labRefRange`: both `EditorField(..., externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`; render side-by-side (`Row(spacedBy(8.dp))`, `Modifier.weight(1f)`) when `!narrowStacked`, and stacked vertically (`Column(spacedBy(12.dp))`) when `narrowStacked` (`fontScale > 1.3f`).
   - **Abnormal toggle (`labFlagToggle`, `labAbnormal`)**:
     - `Row(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = enabled) { onEvent(AncEvent.LabAbnormalChanged(!draft.values.isAbnormal)) }.padding(vertical = 4.dp).testTag("labFlagToggle"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp))`:
       - `Column(Modifier.weight(1f))`: `Text(stringResource(R.string.is_abnormal), style = RecordType.FieldValue, fontWeight = if (draft.values.isAbnormal) FontWeight.Bold else FontWeight.Normal, color = if (draft.values.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)` + `Text(stringResource(if (isDipstick) R.string.lab_dipstick_auto_flag_hint else R.string.lab_range_auto_flag_hint), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`
       - `Switch(checked = draft.values.isAbnormal, onCheckedChange = { onEvent(AncEvent.LabAbnormalChanged(it)) }, enabled = enabled, modifier = Modifier.testTag("labAbnormal"))`
   - **Notes (`labNoteToggle`, `labNotes`)**:
     - `Box(modifier = Modifier.fillMaxWidth().testTag("labNoteToggle")) { EditorField(..., tag = "labNotes", externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue, singleLine = false) }`
5. **Add tests / Catalog filter card (`labAddTestsCard`, `labAddTestsToggle`) & Lock footer removal**:
   - `OutlinedCard(shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().testTag("labAddTestsCard"))`:
     - Header `Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { testSelectorOpen = !testSelectorOpen }.padding(horizontal = 16.dp).testTag("labAddTestsToggle"), Arrangement.SpaceBetween, Alignment.CenterVertically)`: `Text(stringResource(R.string.add_tests), style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface)` + `Text(stringResource(if (testSelectorOpen) R.string.more_close else R.string.more_open), style = RecordType.Action, color = MaterialTheme.colorScheme.primary)`.
     - Inside `if (testSelectorOpen)` (`Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), spacedBy(12.dp))` with `LocalFieldFamily provides bgFamily`):
       - `FormFieldLabel(stringResource(R.string.lab_filter_by_category))` + `FlowRow(spacedBy(8.dp))` of `RecordFilterChip`s (`allLabTestsChip`, `labGroupChip-${grp.id}`, `wrap = true`, `minTouchTarget = true`).
       - `saveToLabCodexBanner`: `Surface(color = bgFamily.container, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().testTag("saveToLabCodexBanner"))` holding `Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically)` with `RecordType.Secondary` text (`onSurface`) and `TextButton(shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("saveToLabCodexButton")) { Text(stringResource(R.string.lab_add_template), style = RecordType.Action) }`.
       - `ChoiceGroup(label = stringResource(R.string.test_category), selected = draft.values.testCategory, choices = ClinicalVocabulary.testCategories.toList(), enabled = enabled, tagPrefix = "labCategoryChoice", onSelected = <unchanged>, labelStyle = RecordType.FieldLabel, labelColor = bgFamily.label, useRecordChip = true)`.
   - **Delete** the bottom `Icons.Default.Lock` row (`"Lab records and reports encrypted locally (AES-256 GCM) · Local Vault Security"`).

## 4. Lab batch screen (`ui/LabBatchScreen.kt`, same signature)

Keep `LabBatchTab` routing, `showMetadataDialog`, `choosingGroup`, `catalogRepo`, `qualitativeChoicesFor`, `isChoiceAbnormal`, and all `AncEvent` dispatches intact. Define `val bgFamily = FieldFamilies.background()` and `val narrowStacked = LocalDensity.current.fontScale > 1.3f`. Delete private `LabResultPill` / `LabResultPillStatus`.

1. **Page header, error card, patient banner & metadata bar**:
   - `EditorPage(title = stringResource(R.string.lab_batch_title), subtitle = "", tag = "labBatchScreen", bottomBar = pinnedBottomBar, useRecordStyle = true)`.
   - `GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)`.
   - `LabPatientBanner(patient = patient, asOfDate = parseIsoDateOrNull(draft.date) ?: LocalDate.now())`.
   - `Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically)`:
     - `IconButton(onClick = { onEvent(AncEvent.CancelLabBatch) }, modifier = Modifier.minimumTouchTarget().testTag("labBatchBack")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.primary) }`
     - `BatchInfoBar(dateText = draft.date, gaText = calculatedGa?.shortWeeksText, labName = draft.labName, onChangeClick = { showMetadataDialog = true }, modifier = Modifier.weight(1f), useRecordStyle = true)`.
2. **Metadata dialog (`if (showMetadataDialog)`) & `ChoosePanelDialog`**:
   - **Metadata dialog**: `AlertDialog(shape = RecordShapes.Large, containerColor = MaterialTheme.colorScheme.surface, title = { Text(stringResource(R.string.lab_batch_edit_metadata), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.primary) }, ...)`:
     - Inside `CompositionLocalProvider(LocalFieldFamily provides bgFamily)` (`Column(spacedBy(12.dp))`):
       - `ClinicalDateField(value = editDate, onValueChange = { editDate = it; onEvent(AncEvent.LabBatchDateChanged(it)) }, label = stringResource(R.string.test_date), tag = "labBatchDate", enabled = true, showTodayButton = true, externalLabel = true, useRecordStyle = true)`
       - `EditorField(value = editLab, onValueChange = { editLab = it; onEvent(AncEvent.LabBatchLabNameChanged(it.ifBlank { null })) }, label = stringResource(R.string.lab_name), tag = "labBatchLabName", singleLine = true, externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`
       - `QuickPresetChips(title = stringResource(R.string.common_presets), items = ClinicalVocabulary.labNamePresets, enabled = true, onSelect = { editLab = it; onEvent(AncEvent.LabBatchLabNameChanged(it)) }, tagPrefix = "labBatchLabPreset", useRecordStyle = true)`
     - Confirm button: `Button(onClick = { showMetadataDialog = false }, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget()) { Text(stringResource(R.string.done), style = RecordType.Action) }`.
   - **`ChoosePanelDialog` & nested `showSavePanelDialog`**:
     - `AlertDialog(shape = RecordShapes.Large, containerColor = MaterialTheme.colorScheme.surface, ...)`.
     - Title: `Text(stringResource(R.string.lab_batch_choose_tests), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.primary)` + `Text("${group.name} · ${group.testNames.size} elements", style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)` (exact `"CBC · 6 elements"` text preserved for `LabBatchUiTest`).
     - Hint: `Text(stringResource(R.string.lab_batch_choose_hint), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`.
     - Checklist rows: `Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ... }.padding(vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically)` with `RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold)` test name, `RecordType.Secondary` unit, `Checkbox`, and `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)`.
     - `"Save this selection as a panel…"`: `TextButton(onClick = { showSavePanelDialog = true }, shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().minimumTouchTarget()) { Text(stringResource(R.string.lab_batch_save_selection_as_panel), style = RecordType.Action, color = MaterialTheme.colorScheme.primary) }` (drops `ActionRose`).
     - Confirm (`"Add %1$d selected"`) & Dismiss (`"Cancel"`): `Button` / `OutlinedButton` with `shape = RecordShapes.Small`, `Modifier.minimumTouchTarget()`, `RecordType.Action`.
     - Nested `showSavePanelDialog`: `AlertDialog(shape = RecordShapes.Large, containerColor = MaterialTheme.colorScheme.surface)` with `EditorField(..., label = stringResource(R.string.lab_batch_panel_name), externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)` and `RecordShapes.Small` `48.dp` buttons.
3. **3-Tab Bar (`labBatchTab-ADD`, `labBatchTab-ENTER`, `labBatchTab-REVIEW`) & `pinnedBottomBar`**:
   - Tabs: when `!narrowStacked`, `Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp))` with `Modifier.weight(1f)`; when `narrowStacked` (`> 1.3f`), `FlowRow(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Arrangement.spacedBy(6.dp))`. Each tab is a `Surface(onClick = ..., enabled = ..., shape = RecordShapes.Small, color = if (selected) primary else surface, border = BorderStroke(1.dp, when { selected -> primary; enabled -> outline; else -> outlineVariant }), modifier = Modifier.heightIn(min = 48.dp).minimumTouchTarget().testTag(...))` containing centered `Text(..., style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center, color = when { selected -> onPrimary; enabled -> onSurface; else -> onSurfaceVariant })`.
   - `pinnedBottomBar`:
     - `OutlinedButton(onClick = { onEvent(AncEvent.CancelLabBatch) }, shape = RecordShapes.Small, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp).minimumTouchTarget().testTag("labBatchCancel")) { Text(stringResource(R.string.cancel), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) }`
     - `Button(onClick = { onEvent(AncEvent.SubmitLabBatch) }, enabled = draft.filledCount > 0, shape = RecordShapes.Small, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.weight(1.6f).heightIn(min = 48.dp).minimumTouchTarget().testTag("labBatchSave")) { Text(saveText, style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) }`
4. **`AddTab`**:
   - **Panels card**: `RecordSectionCard(title = stringResource(R.string.lab_batch_panels_section), family = bgFamily)`:
     - Each panel row separated by `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)`:
       - Top clickable `Column(Modifier.fillMaxWidth().minimumTouchTarget().clickable { expandedGroupId = if (isExpanded) null else group.id }.padding(vertical = 4.dp))`: panel name in `RecordType.CardName` (`onSurface`) + summary line `stringResource(R.string.lab_batch_panel_summary, group.testNames.size, group.testNames.take(4).joinToString(", ") + ...)` in `RecordType.Secondary` (`onSurfaceVariant`).
       - Stacked 50/50 `Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp))`:
         - `OutlinedButton(onClick = { onChooseClick(group) }, shape = RecordShapes.Small, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.weight(1f).minimumTouchTarget()) { Text(stringResource(R.string.lab_batch_choose), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) }`
         - `Button(onClick = { onEvent(AncEvent.LabBatchItemsAdded(available)) }, enabled = available.isNotEmpty(), shape = RecordShapes.Small, modifier = Modifier.weight(1f).minimumTouchTarget().testTag("labBatchAdd-${group.id}")) { Text(stringResource(R.string.lab_batch_add_n, available.size), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) }`
       - If `isExpanded`: checkbox rows with `Modifier.fillMaxWidth().heightIn(min = 48.dp)`, `RecordType.FieldValue` text, and `Button` reading `stringResource(R.string.lab_batch_add_n_selected, selectedTestNames.size)` (no `"▶"`, `RecordShapes.Small`, `48.dp`).
   - **Single test card**: `RecordSectionCard(title = stringResource(R.string.lab_batch_add_single_test), family = bgFamily)`:
     - Search input (`OutlinedTextField` `tag = "labBatchSearch"`): `shape = RecordShapes.Small`, `textStyle = RecordType.FieldValue`, `colors = editorFieldColors()`, `placeholder = { Text(stringResource(R.string.lab_batch_search_placeholder), style = RecordType.Secondary) }`.
     - If `query.isNotBlank()`:
       - If `matches.isEmpty()`: `Text(stringResource(R.string.lab_batch_no_search_matches), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)`
       - Else: `minHeight = 48.dp` match rows with `RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold)` test name, `RecordType.Secondary` unit, `TextButton(shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("labBatchAddOne-${item.id}")) { Text(stringResource(R.string.lab_batch_add), style = RecordType.Action) }`, split by `HorizontalDivider(outlineVariant)`.
     - Else: `FormFieldLabel(stringResource(R.string.lab_batch_often_added))` (sentence case, no `.uppercase()`) + `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp))` of `RecordFilterChip`s (`"Ferritin"`, `"TSH"`, `"Vitamin D"`, `stringResource(R.string.lab_batch_add_other)`, `minTouchTarget = true`).
5. **`EnterTab`**:
   - Empty state: `RecordSectionCard` / `Text(stringResource(R.string.lab_batch_empty_add_first), style = RecordType.FieldValue)`.
   - `BatchSegmentedProgress(total = draft.items.size, current = draft.cursor, useRecordStyle = true)`.
   - Entry `OutlinedCard` (`RecordShapes.Large`, `outlineVariant` border, `CompositionLocalProvider(LocalFieldFamily provides bgFamily)`):
     - Header `Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically)`:
       - Left `Column(Modifier.weight(1f))`: `FormFieldLabel(stringResource(R.string.lab_batch_enter_progress, item.category.ifBlank { stringResource(R.string.lab_batch_default_category) }, draft.cursor + 1, draft.items.size))` (sentence case, no `.uppercase()`) + `Text(item.testName, style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.primary)`.
       - Right `IconButton(onClick = { onEvent(AncEvent.LabBatchItemRemoved(item.labId)) }, modifier = Modifier.minimumTouchTarget().testTag("labBatchDeleteItem")) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.lab_batch_remove_item, item.testName), tint = MaterialTheme.colorScheme.error) }` (raised from `32.dp` to `48.dp`!).
     - Qualitative branch (`qualitativeChoices != null`): `RecordType.Secondary` subtitle + `Column(spacedBy(8.dp))` of `QualitativeChoiceButton(..., useRecordStyle = true)`.
     - Quantitative branch: `RecordType.Secondary` range hint (`lab_batch_unit_range` / `lab_batch_from_report`, comma instead of `" · "`), conditional `LabReadoutStrip(resultText = item.result, unitText = item.unit, flagContent = { BandChip(text = stringResource(if (item.isAbnormal) R.string.lab_abnormal else R.string.lab_group_normal), tone = if (item.isAbnormal) StatusTone.High else StatusTone.Neutral) })` when `item.result.isNotBlank()`, `EditorField(..., tag = "labBatchResult", label = stringResource(R.string.test_result), externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.KeyValue, keyboardType = KeyboardType.Decimal, singleLine = true)`, `EditorField(..., tag = "labBatchRange", label = stringResource(R.string.lab_batch_reference_range_hint, stringResource(R.string.lab_batch_from_report)), externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue, singleLine = true)`.
     - Abnormal switch `Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically)`: `Text(stringResource(R.string.lab_batch_abnormal), style = RecordType.FieldValue, fontWeight = if (item.isAbnormal) FontWeight.Bold else FontWeight.Medium, color = if (item.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))` + `Switch(..., modifier = Modifier.testTag("labBatchFlag"))`.
     - Notes: `EditorField(..., tag = "labBatchNote", label = stringResource(R.string.notes), externalLabel = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue)`.
     - Prev / Next `Row(spacedBy(8.dp))`: `OutlinedButton` (`tag = "labBatchPrev"`, `RecordShapes.Small`, `48.dp`, `stringResource(R.string.back_step)`, no `"◀"`) and `Button` (`tag = "labBatchNext"`, `RecordShapes.Small`, `48.dp`, `stringResource(if (draft.cursor < draft.items.lastIndex) R.string.next_step else R.string.lab_batch_tab_review)`, no `"▶"`).
   - Inline add link: `TextButton(onClick = { onEvent(AncEvent.LabBatchTabSelected(LabBatchTab.ADD)) }, shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("labBatchAddInline")) { Text(stringResource(R.string.add_test_to_report), style = RecordType.Action) }` (drops double `"+ +"`).
6. **`ReviewTab`**:
   - Each `section` in `groupLabBatchItems(draft.items, groups)`: `RecordSectionCard(title = section.title, family = bgFamily, action = { Text(stringResource(R.string.lab_batch_elements_count, section.items.size), style = RecordType.FieldLabel, color = bgFamily.label, modifier = Modifier.padding(end = 12.dp)) })`.
   - Each row (`HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)` for `index > 0`):
     - `Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { /* jump to Enter item */ }.padding(vertical = 4.dp), Alignment.CenterVertically, Arrangement.spacedBy(8.dp))`:
       - Left `Column(Modifier.weight(1f))`: `Text(item.testName, style = RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)` + optional `item.unit.takeIf { it.isNotBlank() }?.let { Text(it, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant) }`.
       - Middle/right `Column(horizontalAlignment = Alignment.End)`:
         - If `item.result.isBlank()`: `StatusWord(stringResource(R.string.lab_batch_pending), StatusTone.Moderate)`.
         - Else: `val resultDisplay = if (item.unit.isNotBlank()) "${item.result} ${item.unit}" else item.result`; `Text(ltrIsolate(resultDisplay), style = RecordType.MetricValue.copy(fontWeight = FontWeight.Bold), color = if (item.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)` + `StatusWord(stringResource(if (item.isAbnormal) R.string.lab_abnormal else R.string.lab_group_normal), if (item.isAbnormal) StatusTone.High else StatusTone.Neutral)`.
       - Trailing `TextButton(onClick = { /* jump to Enter item */ }, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("labBatchEdit-${item.labId}")) { Text(stringResource(R.string.lab_batch_edit), style = RecordType.Action) }`.
   - `if (emptyQualitative.isNotEmpty())`: `OutlinedCard(shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = bgFamily.container), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth())` containing `Text(stringResource(R.string.lab_batch_mark_normal_hint), style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)` and `OutlinedButton(onClick = <unchanged>, shape = RecordShapes.Small, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("labBatchMarkNormal")) { Text(stringResource(R.string.lab_batch_mark_empty_normal, emptyQualitative.size), style = RecordType.Action, textAlign = TextAlign.Center) }`.

## Acceptance checklist

1. Every existing `testTag` kept on its exact semantic node:
   - `LabEditor`: `labEditor`, `editorError`, `labNameField`, `labNamePreset-*`, `labDate`, `labDateCalendarButton`, `labDateTodayButton`, `labCalculatedGaCard`, `labBatchDone`, `labBatchChange`, `cancelLab`, `addAnotherLab`, `saveLab`, `pinnedLabHeaderTag`, `labTestCard`, `labTestProgress`, `labTestTitle`, `labEditDetailsToggle`, `labName`, `labPreset-*`, `labTestSubtitle`, `labResultPreset-0..4`, `labStoryDisplay`, `labStoryValue`, `labResultFlag`, `labResult`, `labUnit`, `labRefRange`, `labFlagToggle`, `labAbnormal`, `labNoteToggle`, `labNotes`, `labAddTestsCard`, `labAddTestsToggle`, `allLabTestsChip`, `labGroupChip-*`, `saveToLabCodexBanner`, `saveToLabCodexButton`, `labCategoryChoice-*` (plus `labSaveSpinner`, `labPresetToggle`).
   - `LabBatchScreen`: `labBatchScreen`, `labBatchDate`, `labBatchDateCalendarButton`, `labBatchDateTodayButton`, `labBatchLabName`, `labBatchLabPreset-*`, `labBatchCancel`, `labBatchSave`, `labBatchBack`, `labBatchTab-ADD`, `labBatchTab-ENTER`, `labBatchTab-REVIEW`, `labBatchAddList`, `labBatchAdd-*`, `labBatchSearch`, `labBatchAddOne-*`, `labBatchEnter`, `labBatchDeleteItem`, `labBatchResult`, `labBatchRange`, `labBatchFlag`, `labBatchNote`, `labBatchPrev`, `labBatchNext`, `labBatchAddInline`, `labBatchReview`, `labBatchEdit-*`, `labBatchMarkNormal` (plus `editorError`).
2. Zero text nodes under `12.sp` in `LabEditor` and `LabBatchScreen`; zero ALL-CAPS labels (`"HEMATOLOGY"`, `"ADD A SINGLE TEST"`, `"OFTEN ADDED"`, `"HEMATOLOGY · TEST 1 OF 6"`), zero decorative symbols (`"✓"`, `"▲"`, `"▼"`, `"◀"`, `"▶"`, `"▾"`, double `"+ +"`), zero `ActionRose` accents, and zero AES-256 Lock footers.
3. Zero `2.dp`, `8.dp`, `12.dp`, `14.dp`, `20.dp`, `28.dp`, or `999.dp` pill shapes in redesigned `LabEditor` and `LabBatchScreen` (all shapes `RecordShapes.Small` `10.dp` or `RecordShapes.Large` `16.dp`).
4. Every tappable target (`labBatchDeleteItem`, panel `Choose` / `+ Add N`, tabs, dipstick chips, `labEditDetailsToggle`, `labAddTestsToggle`, `labBatchEdit-*`, `labBatchMarkNormal`, `ChoosePanelDialog` rows, bottom bars) measures `>= 48.dp` (`>= 47.9.dp` in instrumented checks) and fits cleanly on `360.dp` at `1.0f`, `1.3f`, and `2.0f` font scale with zero clipped buttons or crushed panel names.
5. `LabPatientBanner` displays `PatientNameText` (right-aligned for Arabic names such as `"منى رجب"`), `27+2 wks` GA, `G4 P1 A2` parity (keeping `A2`), and `A+` (`StatusTone.High` for Rh-negative), and omits any risk badge for low/normal-risk patients.
6. `labResultFlag` auto-flagging in `LabEditor`, `panelRemainingIds` chained queue, `ChoosePanelDialog` `"CBC · 6 elements"` & `"Add 5 selected"`, `qualitativeChoicesFor`, `emptyQualitative` `"Mark N empty as normal"`, and `ClinicalPresets` medication frequency helpers all behave identically to today.
7. Dark theme (`AncTheme(darkTheme = true)`) and `LayoutDirection.Rtl` render cleanly with WCAG AA contrast verified across Background (sand) tokens.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected.
- `LabEntryDesignTest` relies on `testTag`s (`labEditDetailsToggle`, `labName`, `labResult`, `labAddTestsToggle`, `allLabTestsChip`, `labTestTitle` + `"Hemoglobin"`, `labUnit`, `labRefRange`, `labResultPreset-1`, `labAbnormal`, `labNotes`, `saveLab`, `cancelLab`, `addAnotherLab`). All kept intact.
- `LabBatchUiTest` relies on `testTag`s and exact strings (`"Choose"`, `"Choose tests"`, `"CBC · 6 elements"`, `"MCV"`, `"Add 5 selected"`, `"Save 1 result"`, `"Normal"`, `"Trace"`, `"+"`, `"Mark 7 empty as normal"`, `"CBC"`, `"10.8"`). All kept intact.
- `AncWorkflowTest` checks `labEditor`, `labDate` immediately displayed on new blank-date lab, `labName`, `labResult`, and `saveLab` at `360.dp`. All kept intact.
- `ClinicalPresetsTest` checks `COMMON_MEDICATION_FREQUENCIES` and `medicationFrequencyLabelRes`. Untouched.

### Must pass unchanged

- `app/src/androidTest/java/com/sherifradwan/ancnative/LabEntryDesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/LabBatchUiTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/LabsTabRedesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/DraftRecoveryDialogTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/DeleteUndoUiTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/storage/LabBatchDbTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/ClinicalPresetsTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/LabResultFlagTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/ClinicalConstantsTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/catalog/LabPanelResolveTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/catalog/LabPanelQueueTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/state/LabBatchDraftTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/state/DraftMappingLabBatchTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/state/LabBatchUndoTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/storage/LabBatchCommandTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/RecordTokensContrastTest.kt`

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/LabEditorRedesignTest.kt`

Mount `LabEditor` and `LabBatchScreen` through `MainActivity` + `AncTheme` inside a `360.dp x 640.dp` host (matching `LabEntryDesignTest`, `LabBatchUiTest`, and `ComposeTextAsserts`, single `setContent` per test):
- **Typography & legacy removal (`1.0f` and `1.3f`)**: walk semantics trees of `LabEditor` (with `labEditDetailsToggle` open) and `LabBatchScreen` across `ADD`, `ENTER`, and `REVIEW`; assert zero `TextLayoutResult` nodes have `fontSize < 12.sp`, and `"AES-256"`, `"Normal Risk"`, `"27w 2d"`, `"✓"`, `"▲"`, `"▼"`, `"◀"`, `"▶"`, `"▾"`, `"+ +"` never appear.
- **360 dp & `2.0f` font scale ergonomics**: verify `LabPatientBanner` renders `"Mona Ragab"`, `"27+2 wks"`, `"G4 P1 A2"`, and `"A+"`; verify `"منى رجب"` aligns right via `PatientNameText`; verify `cancelLab`, `saveLab`, `addAnotherLab`, `labBatchBack`, `labBatchTab-ADD/ENTER/REVIEW`, `labBatchAdd-group_cbc`, `labBatchDeleteItem`, `labBatchPrev`, `labBatchNext`, `labBatchMarkNormal`, and `labBatchEdit-*` all measure `>= 47.9.dp` in height and width and do not clip beyond `360.dp`.
- **`LabEditor` flow**: with `Haemoglobin (Hb)` loaded and result `"11.2"`, range `"11.0 - 15.0"`, assert `labStoryValue` displays `"11.2"` and `labResultFlag` displays `"Normal"`; enter `"9.8"` and assert `labResultFlag` displays `"Low"` and `labAbnormal` turns on; with `testSelectorOpen` and no group filter, assert `labPresetToggle` (`"Show all 28"`) collapses `labPreset-*` to 5 items until tapped.
- **`LabBatchScreen` metadata & error state**: tap `BatchInfoBar`, verify `labBatchDate`, `labBatchDateTodayButton`, `labBatchLabName`, and `labBatchLabPreset-0` (`"Al Borg Labs"`) work; verify `LabBatchDraft` with `errors = setOf(ClinicalValidationError.REQUIRED_DATE)` renders `editorError`.
- **Dark theme & RTL**: mount `LabEditor` and `LabBatchScreen` (`EnterTab` + `ReviewTab`) under `AncTheme(darkTheme = true)` and `LayoutDirection.Rtl` with zero overlap or horizontal overflow.

## Risk

**High (`ClinicalPresets.kt` is Level 3; lab result flagging and batch saves are clinical-safety critical).** Moving `.testTag(...)` off `OutlinedTextField` (`labName`, `labResult`, `labBatchDate`, `labBatchResult`, etc.), changing `batchInfoOpen` initial state, or modifying `"CBC · 6 elements"` / `"Add 5 selected"` will break `AncWorkflowTest`, `LabEntryDesignTest`, or `LabBatchUiTest`. Keep `ClinicalPresets.kt` strictly display-only on the collapsible `TextButton`, guard `DesignSystem.kt` changes behind `useRecordStyle = false`, preserve all validation/save/calculation paths verbatim, and require frontier-tier clinical safety review and independent test review before merge.
