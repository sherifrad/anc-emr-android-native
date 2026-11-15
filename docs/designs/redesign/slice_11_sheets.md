# Slice 11: Editor sheets (medication and delivery plan)

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s11_sheets.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt` (`RecordType`, `RecordShapes`, `FieldFamilies`, `LocalFieldFamily`), `FormFieldLabel.kt` (added by slice 10; if slice 10 has not landed, add it exactly as in `slice_10_plan.md` section 1.2 and say so in the PR). Read them; do not re-spec them.

**Scope (PLAN slice 11):** sheet surface and header, field and group labels at 12 sp, the Cancel / Save button row, the discard dialog. Chrome only.

**Not in scope:** the M3 `FilterChip`s (route, frequency, mode, outcome, Today), the `BigChoiceButton` status pair (`ui/DesignSystem.kt`, radius 12, check icon), `EditorField` and `ClinicalDateField` (floating labels, shared by every editor), the Meds tab itself (slice 9), the shell's own discard dialog in `AncApp.kt` (slice 19). They change in the editor slices.

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`. Also not touched: `state/**` (`AncViewModel`, drafts, `SavePhase`), `model/DeliveryOutcome.kt`, `model/DeliveryNotes*` (note keys and parsing), `ui/Components.kt` (`EditorField`), `ui/AncDatePicker.kt`, `ui/DesignSystem.kt`, `ui/ClinicalPresets.kt` (level 3), `MedicationTapToAddSection.kt`, `MedsTab.kt`, `PatientHubScreen.kt` (sheet hosting).

**Save paths must not move.** In `MedicationSheet`: `onEvent(AncEvent.SubmitMedication)` and `onEvent(AncEvent.SubmitMedicationAndAddAnother)` with `saveAttempts++` before each, `requestCancel`, `rememberGuardedSheetState(...)` arguments, `onDismissRequest = { if (enabled) requestCancel() }`, `confirmDiscard` and its `onDiscard` / `onKeep`, `draftKey`, `nameFocusedFor`, `focusNameOnStart`, `revealToken`, `footer()` placement (last child of the sheet column). In `DeliveryPlanSheet`: `onEvent(AncEvent.SubmitPatientProfile)`, `saveEnabled = draft.deliveryPlanTouched`, `requestCancel`, the guarded sheet state, every `DeliveryPlanFieldChanged`, `DeliveryOutcomeToggled`, `DeliveryOutcomeFieldChanged` call and its arguments (including the toggle-off `""` on a selected chip), `deliveryErrors` and the `fieldErrorMessage` expression, `footer()`. The reviewer diffs these lines.

**Level 3:** `ui/PatientEditor.kt` only if the owner says yes to decision 3 (two default parameters on `ChoiceGroup`, no other line). Frontier-tier review plus an independent test review. If that review is not available, skip it: Route keeps its 15 sp label and the PR lists it as open.

**Visual only.** Same fields, same order inside the medication form, same validation, same enabled and saving rules, same tags.

## Decisions for the owner (this slice)

3. **Route label.** Recommended: `ChoiceGroup(..., labelStyle: TextStyle = MaterialTheme.typography.titleMedium, labelColor: Color = Color.Unspecified)` in level-3 `PatientEditor.kt`; every other caller keeps the defaults, so nothing else changes. `MedicationForm` passes `RecordType.FieldLabel` and `currentFieldFamily().label`.
4. **Delivery sheet order.** Recommended: the four plan boxes first, then "Delivery outcome" (today the outcome block is first). Moving the `DeliveryOutcomeSection(...)` call below the last `PlanField` is the whole change; the rewrite notice stays directly above the first plan box. If not approved, keep today's order.

## Lessons (apply all)

1. First (outermost) `testTag` wins.
2. Size assertions allow 0.1 dp (`>= 47.9.dp`).
3. One `setContent` per test; `performScrollTo()` before asserting below the fold.
4. The sheet and the dialog are separate windows: find nodes with `useUnmergedTree = true`.
5. `DeliveryPlanSheetTest` was flaky once: wait for idle after opening the sheet; do not add sleeps.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/components/RecordTokens.kt` | add `RecordShapes.Sheet` |
| `ui/patienthub/components/EditorSheetParts.kt` (185 lines) | restyle `SheetSaveMessage`, `SheetCancelSaveRow` (+ `modifier`), `DiscardChangesDialog` |
| `ui/patienthub/components/MedicationSheet.kt` (192 lines) | surface, shape, family, title, footer divider, button text |
| `ui/patienthub/components/MedicationForm.kt` (350 lines) | exactly the four edits in section 4, nothing else |
| `ui/patienthub/components/DeliveryPlanSheet.kt` (351 lines) | surface, shape, family, title, labels, placeholder, outcome heading, footer |
| `ui/patienthub/components/PencilEditButton.kt` (43 lines) | delete (no caller in `app/src`, grep checked 2026-11-15) |
| `ui/PatientEditor.kt` (level 3, decision 3 only) | two default parameters on `ChoiceGroup` |
| `app/src/main/res/values/strings.xml` | change 5 values |

## 1. Token

`RecordTokens.kt`, inside `object RecordShapes`: `val Sheet = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)`.

## 2. Strings (change values, keys unchanged)

- `add_medication` "Add Medication" -> "Add medication". Also the Meds tab picker toggle text; `MedsTabSheetTest` reads it through `str(R.string.add_medication)`, so it keeps passing.
- `edit_medication` "Edit Medication" -> "Edit medication" (`MedicationFormTest` line 161 reads it through `str`).
- `edit_delivery_plan` "Edit Delivery Plan" -> "Edit delivery plan" (also the card's Edit content description).
- `save_and_add_another_med` "Save &amp; add another" -> "Save and add another".
- `delivery_fees_hint` "... Put quotes or estimates in Delivery Plan." -> "... in Delivery plan."

Do not change `delivery_hospital_title`, `delivery_fees_title`, `delivery_plan_title`, `delivery_postpartum_title` (`translatable="false"`): they are the labels written into the obstetric notes, and `DeliveryPlanSheetTest` line 380 asserts them. The sheet stops displaying them (section 5).

## 3. `EditorSheetParts.kt`

### `SheetSaveMessage` (same signature)
- Failed title: `style = RecordType.Action.copy(fontWeight = FontWeight.Bold)` (was `titleSmall`), colour unchanged (error).
- Message: `style = RecordType.Secondary` (was `bodyMedium` 13 sp), colour rule unchanged.
- `liveRegion`, `revealWhen(isFailure)`, tag, early `return` unchanged.

### `SheetCancelSaveRow` (one new trailing parameter `modifier: Modifier = Modifier`, applied to the `Row` before `fillMaxWidth()`)
- Both buttons: `shape = RecordShapes.Small`, `Modifier.weight(1f).heightIn(min = 48.dp).minimumTouchTarget().testTag(tag)` (keep the tag last as today), `contentPadding` unchanged.
- Labels: `style = RecordType.Action`, `maxLines = 2`, `textAlign = Center` unchanged; spinner unchanged.
- Enabled rules unchanged. This row is also used by the add attachment dialog (slice 10), which follows.

### `DiscardChangesDialog` (same signature)
```kotlin
AlertDialog(
    onDismissRequest = onKeep,
    shape = RecordShapes.Large,
    containerColor = MaterialTheme.colorScheme.surface,
    title = { Text(stringResource(R.string.discard_changes), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.onSurface) },
    text = { Text(stringResource(R.string.discard_body), style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) },
    confirmButton = { TextButton(onClick = onDiscard, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag(confirmTag)) { Text(stringResource(R.string.discard), style = RecordType.Action, color = MaterialTheme.colorScheme.error) } },
    dismissButton = { TextButton(onClick = onKeep, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag(keepTag)) { Text(stringResource(R.string.keep_editing), style = RecordType.Action, color = MaterialTheme.colorScheme.primary) } },
    modifier = Modifier.testTag(dialogTag),
)
```
Same strings, same order, same tags.

### `rememberGuardedSheetState`: not touched.

## 4. Medication sheet

### `MedicationSheet` (same signature)
- `ModalBottomSheet(onDismissRequest = <unchanged>, sheetState = <unchanged>, containerColor = MaterialTheme.colorScheme.surface, shape = RecordShapes.Sheet, modifier = Modifier.testTag("medicationSheet"))`.
- Wrap the sheet's inner `Column` in `CompositionLocalProvider(LocalFieldFamily provides FieldFamilies.background()) { ... }` (medications belong to the Background family, PLAN 2.1).
- Title: `Text(..., style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp).semantics { heading() })` (was `titleLarge`, `vertical = 8.dp`). Same strings.
- Between the scrolling form and `MedicationSheetButtons(...)`: `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)`.
- Progress bar, `SheetSaveMessage`, `MedicationForm(...)` call and every argument: unchanged.

### `MedicationSheetButtons` (same signature)
- Outer `Column` padding unchanged (16 / 12).
- "Save and add another" `OutlinedButton`: `shape = RecordShapes.Small`, `Modifier.fillMaxWidth().heightIn(min = 48.dp).minimumTouchTarget().testTag("addAnotherMedication")`; label `style = RecordType.Action`, `maxLines = 2`, centred. `enabled`, `onClick`, `isExistingRow` gate unchanged.

### `MedicationForm`: exactly these edits, nothing else
1. Line 188, `Text(stringResource(R.string.frequency), style = MaterialTheme.typography.titleMedium)` -> `FormFieldLabel(stringResource(R.string.frequency))`.
2. Line 246, `Text(stringResource(R.string.status), style = MaterialTheme.typography.titleMedium)` -> `FormFieldLabel(stringResource(R.string.status))`.
3. `RouteReadOnly`: label `Text(label, titleMedium)` -> `FormFieldLabel(label)`; remove the `Row` with the `Lock` `Icon` and the `Spacer`, leaving `Text(route, style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.testTag("medRouteValue"))`; note `bodySmall` -> `RecordType.Secondary` (colour unchanged). Tag `medRouteReadOnly` stays on the column. Remove the now unused imports (`Icons`, `Lock`, `Icon`, `Spacer`, `width`, `size` if unused).
4. Decision 3 only: the `ChoiceGroup(...)` call gets `labelStyle = RecordType.FieldLabel, labelColor = currentFieldFamily().label`.

Fields, focus requesters, `revealOnFocus`, `revealWhen`, the two `LaunchedEffect`s, `otherMode`, chips, `BigChoiceButton`s, `ClinicalDateField`, notes field: unchanged.

## 5. Delivery plan sheet (`DeliveryPlanSheet`, same signature)

- `ModalBottomSheet(... containerColor = surface, shape = RecordShapes.Sheet, modifier = Modifier.testTag("deliveryPlanSheet"))`; wrap the inner `Column` in `CompositionLocalProvider(LocalFieldFamily provides FieldFamilies.delivery())`.
- Title as the medication sheet (`RecordType.SectionTitle`, primary, start/end 16, bottom 8, heading).
- Rewrite notice: `style = RecordType.Secondary` (was `bodySmall`), colour, tag and condition unchanged.
- `PlanField` labels: pass `stringResource(R.string.delivery_hospital_label)`, `delivery_fees_label`, `delivery_plan_label`, `delivery_postpartum_label` (the card's sentence-case labels) instead of the `delivery_*_title` strings. Field tags, `minLines`, hints and `onValueChange` events unchanged.
- `PlanField` body: label `Text(label, titleSmall)` -> `FormFieldLabel(label)`; remove `placeholder = { Text(stringResource(R.string.no_record)) }` (an empty box is empty; `no_record` stays in `strings.xml` for its other callers). `semantics(mergeDescendants = true)`, `revealOnFocus`, `KeyboardCapitalization.Sentences` unchanged.
- `DeliveryOutcomeSection`:
  - heading `Text(delivery_outcome_section, titleMedium)` -> `Text(..., style = RecordType.CardName, color = onSurface, modifier = Modifier.fillMaxWidth().semantics { heading() })`;
  - switch row: add `.heightIn(min = 56.dp)` to the `Row` modifier chain (after `fillMaxWidth()`); "Delivered" `bodyLarge` -> `RecordType.FieldValue`; `Switch` unchanged (tag `deliveryOutcomeSwitch`);
  - "Mode of delivery" and "Outcome" `Text(..., titleSmall)` -> `FormFieldLabel(...)`;
  - date field, chips, error texts: unchanged.
- Decision 4 only: move the `DeliveryOutcomeSection(...)` call to after the postpartum `PlanField`.
- Footer: `HorizontalDivider(color = outlineVariant)` then `SheetCancelSaveRow(..., modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))` (today it has no padding and touches the sheet edges). Arguments unchanged.

## 6. `PencilEditButton.kt`

Delete the file: `CanonicalPencilEditButton` has no caller in `app/src/main` or the tests. If the build finds a caller, keep the file untouched and list it in the PR.

## Acceptance checklist

1. Tags unchanged: `medicationSheet`, `medSaveProgress`, `medSaveMessage`, `medicationForm`, `medName`, `medDosage`, `medRouteChoice-<i>`, `medRouteReadOnly`, `medRouteValue`, `medFreqChoice-<i>`, `medFreqChoice-other`, `medFreqOther`, `medFreqError`, `medStatusChoice-<i>`, `medDate` (and `medDateCalendarButton`, `medDateTodayButton`), `medNotes`, `cancelMedication`, `saveMedication`, `medSaveSpinner`, `addAnotherMedication`, `medDiscardDialog`, `medDiscardConfirm`, `medDiscardKeep`, `deliveryPlanSheet`, `deliveryPlanSaveProgress`, `deliveryPlanSaveMessage`, `deliveryPlanRewriteNotice`, `deliveryPlanHospitalField`, `deliveryPlanFeesField`, `deliveryPlanPlanField`, `deliveryPlanPostpartumField`, `deliveryOutcomeSwitch`, `deliveryOutcomeDateField`, `deliveryModeChip-<mode>`, `deliveryOutcomeChip-<outcome>`, `cancelDeliveryPlan`, `saveDeliveryPlan`, `deliveryPlanSaveSpinner`, `deliveryPlanDiscardDialog`, `deliveryPlanDiscardConfirm`, `deliveryPlanDiscardKeep`.
2. The save-path lines listed at the top show no diff (reviewer checks `git diff -U0`).
3. `MedicationForm.kt` diff contains only the four listed edits plus imports.
4. No text under 12 sp in either sheet or the dialog; group and field labels are 12 sp in the family label colour (sand in the medication sheet, plum in the delivery sheet).
5. Sheet container is `surface` with 16 dp top corners; buttons radius 10, at least 47.9 dp high; delivery footer has 16 dp side padding.
6. The delivery sheet shows "Agreed delivery hospital" etc. (sentence case); saving still writes "Agreed Delivery Hospital: ..." into the notes (`DeliveryPlanSheetTest` line 222 style assertion).
7. Discard dialog: white, radius 16, "Discard" in the error colour, same strings and tags.
8. 360 dp, font scale 1.3: Cancel and Save side by side, labels at most 2 lines, inside 360 dp (`MedicationFormTest` 509 to 538 already checks the medication row); "Save and add another" on its own row.
9. Dark theme: labels `#E0CFA6` / `#CDBDF0` on `#1A202E`; Discard `#FFB4AB`.
10. RTL: labels and title at the right; Cancel right of Save (mirrored row).

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. String value changes are read through `str(R.string...)` in `MedicationFormTest`, `MedsTabSheetTest` and `MedicationEntryDesignTest`. If any test finds a delivery field by the text "Agreed Delivery Hospital" on screen (grep found none), it moves to the field tag.

### Must pass unchanged

`MedicationFormTest`, `MedsTabSheetTest`, `MedicationEntryDesignTest`, `DeliveryPlanSheetTest`, `DeliveryPlanTabsEditGateTest`, `DeliveryPlanningCardEditGateTest`, `DeliveryCardRedesignTest`, `DraftRecoveryDialogTest`, `MedsTuneTest`, `MedsTabRestorationTest`, `AncWorkflowTest`, `DeleteUndoUiTest`, unit tests (`DeliveryNotesComposeTest`, `ParseDeliveryNotesSpecTest`). Slice 12 and later: every editor that calls `ChoiceGroup` must look identical (decision 3 adds defaults only).

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/EditorSheetsRedesignTest.kt`

Host the sheets exactly as `MedicationFormTest` (medication sheet with a `MedicationDraft`) and `DeliveryPlanSheetTest` (delivery sheet with a `PatientDraft` that has `deliveryPlan`) do: copy their host helpers' setup, one `setContent` per test. Medication fixture values from `DemoFixtures`: name "Iron Polymaltose Complex", dosage "100 mg", route "Oral", frequency "Once daily", start "2026-04-04", notes "Routine antenatal iron prophylaxis; well tolerated". Delivery fixture: hospital "Mothercare", fees "12000".

Cases:
- medication add: title text equals `str(R.string.add_medication)` and its font size is 18 sp; the "Frequency" and "Status" label nodes are 12 sp; `cancelMedication`, `saveMedication`, `addAnotherMedication` height >= 47.9 dp; `addAnotherMedication` text equals "Save and add another".
- medication edit (existing row): `medRouteReadOnly` contains "Oral" and "A different route is a new drug."; no node with an icon content description inside `medRouteReadOnly`; `addAnotherMedication` does not exist.
- medication touched then Cancel: `medDiscardDialog` displayed; "Discard draft changes?" 18 sp; `medDiscardKeep` returns to the sheet; `medDiscardConfirm` sends `AncEvent.CancelEditor` (record events in a list).
- delivery: "Agreed delivery hospital" exists inside `deliveryPlanSheet`; "Agreed Delivery Hospital" does not exist on screen; no "No record" inside `deliveryPlanSheet`; `saveDeliveryPlan` not enabled before a change, enabled after typing in `deliveryPlanFeesField`; `cancelDeliveryPlan` left edge >= 15.9 dp from the sheet's left edge (footer padding).
- delivery save: change fees to "12000" -> "13000", click `saveDeliveryPlan`: the recorded events contain `AncEvent.SubmitPatientProfile` exactly once.
- decision 4 only: `deliveryPlanHospitalField` top is above `deliveryOutcomeSwitch` top.
- dark (`AncTheme(darkTheme = true)` as other tests do): sheet renders; label colour is not checked by pixels (covered by `ThemeContrastTest` tokens).

## Risk

Medium. Save paths must not move; review the diff of the two sheets line by line against the list at the top. `EditorSheetParts.kt` is shared by both sheets and the add attachment dialog, so one change shows in three places. Decision 3 touches a level-3 file used by five editors (defaults only).

## Owner decisions (2026-11-15): mocks s10 and s11 approved, all five decisions accepted as recommended
Decision 1 integrity wording ("Integrity not verified", amber), 2 rows not band cards, 3 route label params (level 3, frontier review), 4 delivery sheet order (plan boxes first, outcome last), 5 sentence-case category names and ", " separators (level-3 ClinicalFormat.kt and AttachmentSyncManager.kt, test edit AttachmentDisplayTitleTest) are all APPROVED.

**Owner addition: the Delivered toggle row (`deliveryOutcomeSwitch`) must be fixed and well aligned.** Fixed: the row keeps one position; switching it on or off never moves the row or the fields above it (new outcome fields appear BELOW the row; keep the row's height constant, 56 dp, with no layout shift of the row itself). Aligned: label start-aligned with the field labels and boxes above (same left edge), the switch at the end edge flush with the right edge of the field boxes (same end edge), both vertically centred in the row, same on a 360 dp screen at font scale 1.3 and 2.0 (label wraps, the switch stays vertically centred and never overlaps), mirrored in RTL. Tests: left edge of the label equals the left edge of `deliveryPlanHospitalField`; right edge of the switch row's switch equals the right edge of the fields (within 1 dp); `deliveryOutcomeSwitch` top and bottom unchanged before and after toggling; centre-y of label and switch within 1 dp; at 2.0 no overlap.
