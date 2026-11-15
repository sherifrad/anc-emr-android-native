You are the test author. A builder changed the Meds picker and list. Update the Compose instrumented tests so they match the NEW behaviour. Do not weaken assertions; replace the old ones with equivalent checks of the new rules.

NEW BEHAVIOUR (see the implementation files pasted below):
1. Picker, no search text: up to 8 compact name-only chips (tag medTile-<id>, check tag medTileCheck-<id> for active drugs), inside container tag medTilesGrid; a button tagged medPickerShowAll "Show all (N)" appears only when the filtered list has more than 8; pressing it shows all, button then reads "Show less". No nested scrolling list any more (the old 304dp scrolling list is gone).
2. Picker, search typed: at most 5 full rows (MedicationPickerTile, tag medTile-<id>, meta text tag medTileMeta-<id> exists ONLY in search rows), names starting with the query first, medPickerShowAll when more than 5.
3. showAll resets when the query or source chip (Common/Recent) changes; it is saved with the picker state.
4. Dimming: while the sheet covers the tab (medicationDraft != null) controls stay disabled but keep full alpha; a one-tap Stop save still dims.
5. Order: rows already shown keep their order; a newly added drug joins the END of the active list (not the top); the open picker does not move when a row is added.

TASKS:
A. Output a COMPLETE replacement for every test function and helper in MedsTabSheetTest.kt that no longer matches (list them by name first). Keep unchanged ones out of the answer. Use the file's existing helpers (node, click, scrollTo, exists, showTile, openPicker) and fix showTile for chips.
B. Add new tests in the same file: chipsAreCappedAtEightAndShowAllRevealsTheRest, searchShowsAtMostFiveRowsPrefixFirst, showAllResetsWhenTheQueryChanges, chipTapOpensThePrefilledSheet, newDrugJoinsTheEndAndPickerStaysPut (compare medTilesGrid top before/after an add), controlsKeepFullAlphaUnderTheSheet (check via semantics/enabled=false plus no alpha dimming is hard to read: assert the picker toggle still exists and is not enabled, acceptable).
C. Output a new unit test file app/src/test/java/com/sherifradwan/ancnative/ui/patienthub/model/MedicationListOrderTest.kt (stableMedicationOrder: kept order, new appended, removed dropped, empty shown) and PickerVisibleTilesTest (pickerVisibleTiles: empty query take(limit), query prefix first, showAll returns all). Check the real constructors in the pasted files; if a class is not pasted, do not guess: write a minimal local helper and state the assumption.
Format: one fenced kotlin block per file or function group, each preceded by a line "FILE: <path>" and "REPLACES: <function names>".
