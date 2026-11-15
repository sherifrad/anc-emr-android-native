# Slice 9: Meds tab TUNE (no redesign)

Builder brief, self-contained. Owner direction (2026-11-15): the medication section is **tuned, not redesigned**. Today's layout and look stay: the tinted medication cards, the status-and-actions line, the expand behaviour, the tap-to-add picker, the stop bar. This slice only swaps in slice 0 type tokens where they drop in directly, aligns radii and spacing, lifts text that is under 12 sp, and removes dead space. No new components, no new strings, no layout change, no behaviour change.

Out of scope here (handled separately by the coordinator): the two functional defects, "the tab refreshes on every click" and "the add-medication list is too long". Do not touch `PICKER_LIST_MAX_HEIGHT`, the picker filtering, keys, state hoisting or recomposition.

The earlier redesign briefs and mocks for Meds (`slice_09_meds.md`, `mocks/mock_s09_meds*.html`) are withdrawn.

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`, `ui/patienthub/model/MedicationTiles.kt`, `MedicationSheet.kt`, `MedicationForm.kt`, `ui/ClinicalPresets.kt` (level 3).

**Level 3:** item 6 below is the only change in a level-3 file (`ui/ClinicalBadges.kt`, two style values in `MedicationStatusBadge`). It needs a frontier-tier review and an independent test review; if that review is not available, skip item 6 and leave the badge as it is.

## Lessons (apply all)

1. First (outermost) `testTag` wins; no tag moves in this slice.
2. Size assertions allow 0.1 dp (`>= 47.9.dp`).
3. One `setContent` per test; `performScrollTo()` before asserting below the fold.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | What changes |
|---|---|
| `ui/patienthub/components/MedicationCard.kt` | text styles, Stop label size |
| `ui/patienthub/tabs/MedsTab.kt` | list spacing, header top padding, title style, empty-state radius and title |
| `ui/patienthub/components/MedicationTapToAddSection.kt` | radii, tile "Active" label size, tile name style |
| `ui/patienthub/components/MedicationStopBar.kt` | radii |
| `ui/ClinicalBadges.kt` (level 3, optional) | `MedicationStatusBadge` text style and radius |

## The tune list (each one a direct swap)

1. **Type tokens** (`ui/components/RecordTokens.kt`, built in slice 0):
   - `MedicationCard`: name `titleMedium` (15 sp) + Bold -> `RecordType.CardName` (16 sp Bold); dose-route-frequency line `bodyMedium` (13 sp) -> `RecordType.Secondary` (14 sp); closed notes line `bodySmall` -> `RecordType.Secondary`. Colours unchanged.
   - `MedsTab` header title (`medsActiveTitle`) `titleMedium` + Bold -> `RecordType.SectionTitle` (18 sp Bold), the same size as the other tab titles. Text unchanged.
   - `MedsTab` empty state title `titleMedium` -> `RecordType.CardName`; hint `bodyMedium` -> `RecordType.FieldValue` (16 sp).
   - Show past row: `titleMedium` + Bold -> `RecordType.FieldValue.copy(fontWeight = FontWeight.Bold)`.
   - Picker toggle text `titleMedium` -> `RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold)`; tile name `titleMedium` + Bold -> `RecordType.CardName`.
2. **Text under 12 sp lifted to 12 sp** (the theme's `labelMedium` and M3 `labelSmall` are 11 sp):
   - `StopControl` label (`stopMedicationLabel-<id>`) `labelMedium` -> `RecordType.Tag.copy(fontWeight = FontWeight.Bold)` (12 sp). Colour and the 48 dp target unchanged.
   - Picker tile "Active" label `labelMedium` -> `RecordType.Tag`.
3. **Radii to the two-value system** (10 and 16): picker toggle 14 -> `RecordShapes.Large` (16, it is a card-like row); tiles 14 -> `RecordShapes.Small` (10); empty state 12 -> `RecordShapes.Large`; stop bar undo and error 12 -> `RecordShapes.Small`. Keep the matching `.clip(...)` in sync with each `shape`.
4. **Spacing and dead space**: `MedsTab` `LazyColumn` `spacedBy(14.dp)` -> `spacedBy(12.dp)` (the 12 dp record gap used elsewhere); remove the header `FlowRow`'s `padding(top = 6.dp)` (the content padding already gives 16 dp); empty state inner padding vertical 24 -> 16 dp. Card internal paddings stay.
5. **Status contrast check (no change needed, verified):** status badge text `#0D1B3D` on `#EFE9E4` 14.05:1 (dark 9.94); Stop `#BA1A1A` on the card `#F9F5F3` 5.96:1 (dark `#FFB4AB` on `#141924` 10.36); dose line `#4C3F43` on `#F9F5F3` 9.23:1 (dark 10.23); active tile label `#0D1B3D` on `#F3E9E7` 14.20:1. Disabled controls at 0.38 alpha are exempt (WCAG inactive components).
6. **(Level 3, optional)** `MedicationStatusBadge` in `ClinicalBadges.kt`: `style = MaterialTheme.typography.labelSmall` (11 sp) -> `RecordType.Tag` (12 sp), `RoundedCornerShape(6.dp)` -> `RecordShapes.Small`. Colours, padding, tag `medicationStatusBadge`, `maxLines` unchanged.
7. **Touch targets:** reading the code, every control already reaches 48 dp (`medicationToggle-`, `stopMedication-`, `editMedication-`, delete, `addCustomMedicationBtn`, `medPickerToggle` 56 dp, `medTile-`, `medTab-*`, `medShowPastToggle`, `medStopUndo`, `medStopErrorDismiss`). The tests below confirm it; if one fails, add `Modifier.minimumTouchTarget()` to that control only.

Nothing else changes: no icon added or removed, no colour token swapped for the card (`surfaceContainerLow` stays), no string, no order, no tag.

## Acceptance checklist

1. `git diff --stat` touches only the five files above (four if item 6 is skipped).
2. Every tag used by `MedsTabSheetTest`, `MedsTabRestorationTest`, `MedicationStopBarTest`, `DeleteUndoUiTest` and `AncWorkflowTest` is unchanged.
3. No text in the Meds tab under 12 sp (except item 6 if skipped: then the status badge stays 11 sp and is listed as open).
4. Radii in the four meds files are only 10 and 16 dp.
5. Medication card, picker and stop bar look the same as before apart from the slightly larger type and the radii (side-by-side screenshot in the PR at 360 dp, light and dark).
6. 360 dp, font scale 1.3: name, dose line and the Stop label are not clipped.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. `MedsTabSheetTest` checks the instructions text (unchanged), the Stop label text and contrast in both themes (colour unchanged; 12 sp is still readable inside 48 dp), names not clipped at large text, and 48 dp sizes. If a size assertion fails by a fraction of a dp, apply the 0.1 dp tolerance rather than changing layout.

### Must pass unchanged

`MedsTabSheetTest`, `MedsTabRestorationTest`, `MedicationStopBarTest`, `MedicationFormTest`, `DeleteUndoUiTest`, `AncWorkflowTest`, `PatientHubNavigation`, all unit tests (including `ThemeContrastTest`).

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/MedsTuneTest.kt`

Fixtures (exact constructors; values from `DemoFixtures`):
```kotlin
private fun med(id: String, name: String, dosage: String, frequency: String, status: String = "Active", nonCompliant: Boolean = false) =
    DemoMedicationRecord(id = id, patientId = "p-meds", name = name, dosage = dosage, route = "Oral", frequency = frequency,
        startDate = "2026-04-04", endDate = null, status = status, notes = null, isProphylaxis = true, isNonCompliant = nonCompliant)
private fun patient(vararg meds: DemoMedicationRecord) = DemoPatient(
    id = "p-meds", caseId = "M-1", displayName = "Mona Ragab", dateOfBirth = null, phone = null, visits = emptyList(), medications = meds.toList(),
)
val iron = med("m1", "Iron Polymaltose Complex", "100 mg", "Once daily")
val calcium = med("m2", "Calcium + Vitamin D3", "500 mg / 400 IU", "Twice daily", nonCompliant = true)
```
Host `MedsTab` exactly as `MedsTabRestorationTest` does (one `setContent`).

Cases:
- the font size of `medicationName-m1` text is 16 sp and of `stopMedicationLabel-m1` is 12 sp (read `SemanticsProperties.Text` plus `GetTextLayoutResult` and check `layoutInput.style.fontSize`);
- `stopMedication-m1`, `editMedication-m1`, `medicationToggle-m1`, `medPickerToggle`, `addCustomMedicationBtn` each height and width >= 47.9 dp;
- at 360 dp and font scale 1.3, `medicationName-m2` and `medicationInstructions-m2` are not clipped (existing `assertTextNotClipped` helper);
- the top of `medsActiveTitle` is within 18 dp of the top of `medsTabContent` (dead space removed: 16 dp content padding, no extra 6 dp).

## Risk

Low. Style values only, four UI files plus one optional level-3 line pair.
