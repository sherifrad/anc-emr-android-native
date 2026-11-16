I'll read the design and both patches first.
Main patch doesn't touch `EditorHooks.kt`. Reading main.patch now.
Main patch is a Phase‑3 delta (picker/list polish, frequency rule, chained-add focus, reveal-on-error). Now the tests.
Both patches read. One check before I write this up: is there another scroll-position test outside the diff hunks?
# Test review: medication section revision (Phase 3 delta)

**Scope:** I read the whole of `main.patch` (1–1761) and `tests.patch` (1–1610), the design's Technical Plan and Detailed Implementation sections (L97–349), and, for context, `MedsTabSheetTest.kt` L563–596 in the working tree. `main.patch` does not touch `EditorHooks.kt`, `AncApp.kt` or the route deletion, so those earlier phases are not judged here.

## Blocker

None. The one level‑3 logic change, the frequency rule at `ClinicalCommands.kt` (main L51–53), has strong positive and negative tests.

## Major

1. **"Show the cause after a rejected save" (design §7, L161) has no test that would fail.**
   - `MedicationFormTest` (tests L39–43) checks `shown("medName")`. The name is the top field, so it is visible anyway.
   - It then finds the frequency message with `.performScrollTo()`. The test does the scrolling itself, so it passes even if `revealWhen` (main L664–673) does nothing.
   - The reveal on `medSaveMessage` (main L726) is not tested at all.
   - **Missing test:** scroll the sheet down to `medNotes`, save so that only a frequency or date error is raised, then assert `medFreqOther`/`medDate` is displayed with no test-driven scroll. Do the same for a failed save (`rejectWith`) and `medSaveMessage`.
   - The field order is also unchecked: there is no case where both dose and date are in error that asserts only the dose is revealed (main L578–580).

2. **The add step of the scroll test is now vacuous.**
   - `theListKeepsItsScrollPositionThroughEditStopAndAdd` (repo L586–595) and `saveAndAddAnotherReopensABlankSheetAndKeepsTheTab` (tests L480–495) compare where `addCustomMedicationBtn` sits before and after.
   - Main L1431 moved that button into the first list item (`medsActiveHeader`). After `scrollTo` the list is at offset about 0, so a full rebuild that resets the scroll would give the same position.
   - The edit and stop steps (repo L571–583) are still real.
   - **Fix:** measure a row deep in `longList` across the add.

3. **The "Stop" contrast test checks theme colours, not the code.**
   - `theStopLabelAndIconHaveEnoughContrastInBothThemes` (tests L1177–1193) compares `colorScheme.error` with `surfaceContainerLow`.
   - It never reads the colour `StopControl` actually uses (main L484–488) or the card's real background. Changing the tint to `secondary` would still pass.
   - The disabled state (0.38 alpha) is not checked.

4. **The text-input fields are never checked during an add-save.**
   - `everyEntryPointIsDisabledWhileAnAddSaves` (tests L503–520) adds the toggle and a tile.
   - It does not check `medSearchField`, which is checked only during a stop (L372). The `medTab-common`/`medTab-recent` chips (main L963–976) are not checked during either save.

## Minor

5. **A stale comment contradicts the new rule.** `tilesWithoutADoseOrFrequency…` (tests L1036–1037) says the save rules don't require a frequency. Save should now be rejected for a tile with a blank frequency, and the test could assert that cheaply.
6. **Clearing a stored frequency on edit is allowed, and no test pins it as a decision.** An edit that blanks a stored frequency passes (main L51). The only update-path test (L1454–1465) starts from an already-blank row.
7. **The route-aware "already active" check has no UI test.**
   - Unit tests cover it well (L1591–1609).
   - In the UI, nothing shows that an IM tile is enabled and opens the sheet while the oral drug is active.
   - `aTileForAnActiveDrugShowsItsCheckAndDoesNothing` (L405–415) assumes the first catalog item has the fixture's default route; otherwise its route-aware key won't match and the test fails.
8. **The partial-blank row label is untested.**
   - A legacy row with no frequency should show "100 mg · Oral" through `medication_detail_separator` (main L103, L1718).
   - No test covers it. If the quotes in strings.xml were dropped, Android trims the spaces from the separator and nothing would catch it.
9. **The per-patient isolation test may pass regardless of the code.** `thePickerShowPastAndOpenRowOfOnePatientDoNotFollowToAnother` (L1049–1078) only proves the `rememberSaveable(patient.id)` keys (main L1385–1387, L841) matter if the composable is reused when the route changes. Confirm by removing a key: if the test still passes, it is weak.
10. **The 360 dp header test is thin.** `patientHub_header_largeText…` (L1239–1273) checks only that the GA and risk badges are displayed, not that their text is uncut. Blood pressure is not covered, though the 360 dp rule in AGENTS.md lists GA, BP and High Risk.
11. **The picker height test has no lower bound.** It checks height ≤ 304 dp (L569) but not that about five rows show, so a list squashed to one row would pass.

## Flaky timing and environment

- **Keyboard tests** (L939, L963, L1108, L1149) depend on a soft IME showing within 10 s. They fail on emulators with a hardware keyboard enabled.
- The 360 dp tests assert `screenWidthDp <= 360` and fail instead of being skipped when `set_emulator_360dp.sh` has not been run (L941, L1151).
- `theChainedNameFocusHappensOnce…` (L74–78) waits a fixed five frames, which is fine for the one retry today but breaks if the retry count grows.
- `swipingAwayAndBackToTheOpeningTabReportsIt` (L989–999) depends on gesture timing. It does detect the stale-route regression (main L213–223).
- `tilesWithoutADoseOrFrequency…` writes to the shared catalog singleton, and the `own` lookup (L1017) happens before the `try`, so a failure there leaks the added item. It also reads `.value` from a flow straight after writing, which may not have updated yet.
- `readyTiles().take(2)` (L627) depends on the demo catalog having at least two drugs with both dose and frequency.

## Covered well (would fail if broken)

- **Frequency rule:** `saveMedicationPrepareRejectsNewMedicationWithoutFrequency` checks blank and whitespace, with the exact error set. `…AcceptsUpdateOfStoredRowWithoutFrequency`, `aStoredRowWithoutFrequencyCanBeStoppedUndoneAndEditedThroughTheReducer` (which goes through the real reducer), and the UI tests at L400 and L419 cover the rest.
- **Chained add:** `onlyTheChainedBlankStartIsMarkedSoTheSheetFocusesTheName` includes negative cases for edit, tile and custom drafts. The recreation test catches a non-saved `nameFocusedFor`.
- **Picker and lists:**
  - The picker collapsing and expanding, search with a no-match case, and Recent matching regardless of case are all tested.
  - So are Show past (including while a stop saves), the empty state with a read-only negative case, and saved-state restoration (`MedsTabRestorationTest`).
  - Notes showing one line when closed and in full when open are tested, as are the frequency labels (`ClinicalPresetsTest`, with an exact-match negative).
