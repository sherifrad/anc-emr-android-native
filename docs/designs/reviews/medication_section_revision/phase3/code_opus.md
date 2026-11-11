I'll read the design and patch first.
Patch read in full. Now checking context in the working tree: how the sheet is hosted, the file sizes, and helper semantics.
I made no edits and didn't write a plan artifact, because you asked for a read-only review. Line numbers below are lines in `main.patch` (P:n) unless a file is named. I also read `PatientHubScreen.kt`, `MedicationSheet.kt`, `MedsTab.kt`, `AncViewModel.kt` and `ClinicalCommands.kt` in the working tree to check context.

## Blocker

None found.

## Major

**1. Keeping the keyboard up on "Save and add another" depends on timing, not on structure (P:358-371; `PatientHubScreen.kt:274`).**
- `openedWithKey = remember { draftKey }` only works if `MedicationSheet` stays on screen between the two drafts.
- The hub creates the sheet under `if (medicationDraft != null)`.
- After the save commits, `AncViewModel.kt:766-834` updates state with `RecordCommitted`, which clears the draft, and only later dispatches `StartMedicationEditor`. Between the two it calls `undoCoordinator.offerSaveUndo` and `sessionStillCurrent`.
- Today nothing in that gap pauses (suspends), so Compose only ever sees the final state. If either call ever suspends, or a recomposition runs between the two updates:
  - The sheet closes and reopens with its slide animation.
  - `openedWithKey` becomes the new key, so `focusNameOnStart` is false.
  - The name field never takes focus and the keyboard drops. Nothing reports the failure.
- The design's mitigation (§Risks, "Sheet flicker") was to tie the sheet's visibility to the draft's existence so it stays on screen. That was not done.
- The trigger should be state-driven, for example a "chained" flag on the draft, not a key remembered once.

**2. Clinical rules live inside composables (AGENTS rule 2: "no business logic in composables").**
- **The duplicate-prescription guard** (P:666-669): `isActive = patient.medications.any { name.equals(…, ignoreCase) && isOngoingMedication(…) }`. It decides whether a tile may open the sheet, it runs inside `forEach` in the UI, and it has no unit test.
- **The other derivations:** P:587-614 merges the catalog and custom tiles, and P:615 builds `pastNames`.
- **The two rules disagree on name matching:**
  - The Recent filter trims and lower-cases names (P:866-867).
  - The active check only ignores case and does not trim (P:668).
  - `SaveMedicationCommand.prepare` trims names before storing them (`ClinicalCommands.kt` around line 430). So a catalog or custom tile whose name has stray spaces shows under Recent, but gets no check mark and still opens the sheet.
  - Result: the drug can be prescribed twice while one is already active. The design says that must not happen (§5).
- **Fix:** put both rules in a pure function next to `filterMedicationTiles` and test it.

## Minor

1. **Frequency labels have a second source of truth (P:14 vs P:21-28).** The `when` repeats the English strings already in `COMMON_MEDICATION_FREQUENCIES`. If a value is added to the list but not to the `when`, it silently shows untranslated. Drive the list from one map of value to label.
2. **Hard-coded join and order (P:794).** `joinToString(" · ")` fixes the separator and the dose/route/frequency order in code, so translations can't reorder them. Use a formatted string resource.
3. **One string quotes another (P:1235).** "No match. Use Prescribe for a custom drug." hard-codes the button label, so it goes stale if `prescribe_medication` changes.
4. **Dead string (P:1245).** `todays_list` now has 0 uses (I grepped `R.string.todays_list`). Design §15 says strings used only by deleted code are removed.
5. **Counts are not plurals (P:1237, 1243-1244).** "Active Prescriptions (%1$d)" and "Show past (%1$d)" should be `<plurals>`.
6. **Accessibility of the toggles:**
   - **PickerToggle (P:485-492):** it announces "Open"/"Closed" through `stateDescription` instead of the standard expand/collapse semantics, and both icons have no description.
   - **ShowPastToggle (P:1198-1221):** it exposes no expanded state.
   - **MedicationCard's details toggle row (P:133-139):** it has no `role` or `onClickLabel`.
7. **Disabled picker row is only partly dimmed (P:497-500).** The alpha is applied to the inner `Row`, so the border and background still look enabled.
8. **Picker list scrolling (P:658-666):**
   - It is a plain `verticalScroll` `Column` inside a `LazyColumn` item, so every tile is built at once.
   - Its scroll position is not saved, so it resets on rotation or collapse.
   - At 360dp in landscape with the keyboard open, the 304dp cap (P:431) plus the search field can be taller than the visible area of the list it sits in, so the inner list captures the drag.
9. **Too many "add" buttons when there are no drugs (P:989-1012, 1031-1034, 1182-1190).** With an empty list, the header "Prescribe", the empty-state "Prescribe the first medication" and the picker all show together: two buttons doing the same thing on one screen.
10. **Header button can clip (P:1011).** "Prescribe" has `softWrap = false` and shares a row with a weighted title. At a large font scale or with a longer translation at 360dp, the label can be cut off.
11. **Picker source is a bare string (P:438-447).** `source` is a `String` constant restored by unchecked casts. An enum with a typed `Saver` would remove the cast risk.
12. **Scope note.** The `PatientHubScreen` swipe-sync fix (P:55-63, `rememberUpdatedState`) is correct. It just isn't listed for Phase 3 in the design.

## Checks that passed

- **Colours:** none are hard-coded; all come from `MaterialTheme.colorScheme`.
- **Hit areas:** every new control is at least 48dp (`minimumTouchTarget`; the picker row is at least 56dp).
- **File size:** every touched file is under 1,500 lines; the largest is `PatientHubScreen.kt` at 365.
- **RTL:** only start/end padding is used.
- **State across rotation and tab swipes:** the picker state, "Show past" and the open card are kept per patient with `rememberSaveable(patient.id)`, and the list items have stable keys.
- **Strings:** the strings that used to be hard-coded in `MedsTab` ("Active Prescriptions", "Prescribe", "No records") are now resources.
