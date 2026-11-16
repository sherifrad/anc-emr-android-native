## Test Review: Medication Section Revision (Stage 1)

### Blocker Gaps

| # | Design Behaviour (from spec) | Test Coverage | Gap |
|---|------------------------------|---------------|-----|
| 1 | **CUJ 1: one-tap stop with 10 s Undo bar** — "The drug leaves the active list and sits under 'Show past', an Undo bar shows for 10 seconds, and tapping Undo puts the drug back to its earlier status and end date. The tab does not reload and keeps its scroll position." | `MedicationStopViewModelTest.stopSavesTheStoredRowAndUndoSavesItBackWithTheNewRevision()` covers the save/undo path through the ViewModel. `MedsTabSheetTest.stopIsOneTapWithoutASheetAndUndoPutsTheRowBack()` covers the UI flow. `MedicationStopBarTest.theBarExpiresAfterTheInjectedWindow()` covers the 10 s expiry. | **No test verifies that the Undo bar counts from the commit timestamp (`committedAtMillis`) across recomposition/rotation/tab-switch.** `MedicationStopBarTest.showingTheBarAgainDoesNotStartAFreshWindow()` exists but only tests leave/return on the *same* composition; it does not test process death / config change where the bar is recreated from the stored `committedAtMillis`. |
| 2 | **CUJ 2: add from collapsed picker — three taps first time, two while picker stays open** — "Adding a catalog drug takes three taps the first time (open the picker, tile, Save) and two while the picker stays open: the tile opens the sheet already filled in with the saved dose, route and frequency, and Save adds it." | `MedsTabSheetTest.tappingATileOpensTheSheetWithoutLeavingTheTabAndKeepsTheSearchText()` verifies the sheet opens with prefill and search text survives. | **No test verifies the "two taps while picker stays open" path** — i.e. open picker → tap tile → Save → picker still open → tap another tile → Save. The current test closes the sheet after the first save. |
| 3 | **CUJ 3: edit in place** — "The same row now shows twice daily. The earlier frequency is not kept." | `MedsTabSheetTest.theListKeepsItsScrollPositionThroughEditStopAndAdd()` edits a row and checks scroll position. | **No test asserts that the edited row's values actually change in the list** (only scroll position is checked). The design explicitly says "the earlier frequency is not kept" — a regression would not be caught. |
| 4 | **Enter moves focus to next field; focused field stays visible above keyboard** — §6, §131, §153. | `MedsTabSheetTest` has no test for keyboard navigation or `imePadding`/`bringIntoView`. `MedicationEntryDesignTest.t405_cancelSaveAndSaveAndAddAnotherAreAtLeast48dpAndStayVisible()` checks button height only. | **Zero coverage for the Enter-to-next and keyboard-avoidance behaviour.** |
| 5 | **Failed save keeps sheet open with doctor's values and shows existing `SaveUiError` message** — §34, §158–159. | `MedsTabSheetTest.aFailedSheetSaveKeepsTheSheetOpenWithTheValuesAndTheMessage()` covers this for the edit sheet. `MedsTabSheetTest.aFailedStopLeavesTheRowActiveAndShowsTheCause()` covers the stop bar. | **No test for a failed *add* (blank custom drug) save** — the design says "the sheet stays open with the doctor's values". The blank-custom path is not exercised. |
| 6 | **Draft recovery reopens the sheet on the Meds tab, not a full-screen editor** — §170. | `MedicationSheetStateTest.recoveringAnAddOrEditDraftReopensTheSheetOnTheTab()` asserts `route == medsTab`. | **No test for recovering a *stop* draft** — the design says "recovery for add, stop and edit drafts" (§335). The one-tap stop uses `MedicationQuickSave`, not `MedicationDraft`; recovery of a stop-in-flight is not tested. |
| 7 | **Layout at 360 dp with keyboard open** — §130, §173, §343. | No test runs at 360 dp with IME visible. `MedicationEntryDesignTest` uses `LayoutFixture.narrowPhone()` but does not show the keyboard. | **No evidence the sheet fits, scrolls, and keeps Save visible at 360 dp with keyboard.** |

---

### Major Gaps

| # | Design Behaviour | Test Coverage | Gap |
|---|------------------|---------------|-----|
| 8 | **"Show past" toggle keeps its state across actions** — §29, §148, §335. | `MedsTabSheetTest` does not interact with "Show past" at all. | Toggle state survival through add/stop/edit is untested. |
| 9 | **Search text survives every action** — §28, §335. | `MedsTabSheetTest.tappingATileOpensTheSheetWithoutLeavingTheTabAndKeepsTheSearchText()` checks search text after *one* add. Not checked after stop, edit, or "Save and add another". |
| 10 | **Scroll position survives add, stop, edit** — §28, §335. | `MedsTabSheetTest.theListKeepsItsScrollPositionThroughEditStopAndAdd()` covers edit → stop → add scroll positions. **Missing**: scroll after "Save and add another" (sheet closes and reopens), and after a failed save. |
| 11 | **Every entry point visibly disabled while a save runs or a draft is open** — §149, §1539. | `MedsTabSheetTest.everyEntryPointIsDisabledWhileAStopSaves()` and `theSheetButtonsAreDisabledWithProgressWhileADraftSaves()` cover stop-save and draft-save. **Missing**: disable state during *add* save (picker tiles, Prescribe button, stop symbols, edit icons) — the test only checks the sheet's own buttons. |
| 12 | **"Save and add another" does not lose focus or the keyboard** — §129, §335. | No test exercises `SubmitMedicationAndAddAnother`. |
| 13 | **Stop overwrites an existing end date with today; Undo restores the old end date** — §140, §1904. | `MedicationSheetStateTest.stopOverwritesAnEndDateTheActiveRowAlreadyHad()` and `undoIsASecondSaveOfTheSameRowWithItsNewRevision()` cover the reducer logic. **No UI test** verifies the displayed end date changes in the list. |
| 14 | **Route field read-only on edit, choosable on add** — §127, §1013–1015. | `MedsTabSheetTest.theEditSheetShowsTheRouteReadOnly()` checks the edit sheet. **No test** verifies the route *is* choosable on a blank custom drug or a catalog tile add. |
| 15 | **Tile for an already-active drug shows check mark and does nothing** — §147, §956–965. | `MedsTabSheetTest.aTileForAnActiveDrugShowsItsCheckAndDoesNothing()` exists. **Missing**: the tile's `enabled` state and semantics (it should be disabled, not just no-op on click). |
| 16 | **Cancel on untouched sheet closes at once, no discard prompt** — §116, §1593–1602. | `MedicationSheetStateTest.cancelClosesTheSheetWithoutASecondPromptAndStaysOnTheTab()` and `MedicationEntryDesignTest.t401_cancelOnAnUntouchedSheetClosesItAndStaysOnTheTab()` cover this. **Missing**: Cancel on a *touched* sheet shows the prompt **once** and Discard stays on the tab — covered by `t402_cancelAfterAnEditAsksOnceAndDiscardStaysOnTheTab()` and `onlyOneDiscardPromptIsEverShownWhenBackIsPressedOverTheSheetPrompt()`. **OK**. |
| 17 | **Back with a draft open closes the sheet, never leaves the workspace** — §121, §1606–1630. | `MedicationSheetStateTest.backOnAnUntouchedDraftClosesTheSheetAndNotTheWorkspace()` and `backOnATouchedDraftAsksAndDiscardStaysOnTheTab()` cover this. **OK**. |
| 18 | **Undo bar dropped when another action starts or patient changes** — §140, §1859–1880. | `MedicationSheetStateTest.theUndoBarIsDroppedWhenAnotherActionStartsOrThePatientChanges()` covers the reducer. **No UI test** verifies the bar disappears in the Meds tab when the doctor taps a different tile, switches tab, or opens another patient. |
| 19 | **Failed stop message can be dismissed** — §1060–1069. | `MedsTabSheetTest.aFailedStopMessageCanBeDismissed()` covers this. **OK**. |
| 20 | **A second stop tap while one saves is ignored** — §1045–1057. | `MedsTabSheetTest.aSecondStopTapOnTheSameRowWhileItSavesIsIgnored()` covers this. **OK**. |

---

### Minor Gaps / Weak Assertions

| # | Issue | Location |
|---|-------|----------|
| 21 | `MedicationEntryDesignTest.t405_cancelSaveAndSaveAndAddAnotherAreAtLeast48dpAndStayVisible()` asserts button order with `cancel.right <= save.left` and `save.bottom <= another.top` — fragile layout assertions that can flake on different screen densities or themes. | `MedicationEntryDesignTest.kt:345–349` |
| 22 | `DeleteUndoUiTest.undoBarShowsInsideTheBlankMedicationSheetUnderItsButtons()` uses `hasAnyAncestor(hasTestTag("medicationSheet"))` to assert the bar is *inside* the sheet — but the design says the sheet *hosts* the app's Undo bar under its buttons (§135, §1135). The test passes if the bar is anywhere under the sheet, not specifically under the buttons. | `DeleteUndoUiTest.kt:80–81` |
| 23 | `MedsTabSheetTest` uses a hand-rolled in-memory store (`commands` list, `holdSaves`, `rejectWith`) instead of the real `VaultController` / `AncViewModel` save path. This misses integration bugs in `submitPreparedClinicalCommand`, revision checking, or `OUTCOME_UNRESOLVED` handling. Only `MedicationStopViewModelTest` uses the real stack. | `MedsTabSheetTest.kt:732–810` |
| 24 | No test for **cross-patient contamination**: start a draft for patient A, switch to patient B, verify the sheet closes and no draft leaks. The reducer has `withMedicationStopUiKept` and `medicationSheetDraft()` filtering by `patientId`, but no test exercises it. | — |
| 25 | No test for **archived patient** — the design says "patient not archived" (§1523) and the reducer returns early for archived patients (§319, §1849). No UI test verifies the Meds tab shows no entry points for an archived patient. | — |
| 26 | `MedicationStopBarTest` uses a fake clock (`clock = { now }`) but the real composable uses `SystemClock.elapsedRealtime()`. The test verifies expiry logic but not that the real clock is correctly wired in the ViewModel (`committedAtMillis = SystemClock.elapsedRealtime()` at §591). | `MedicationStopBarTest.kt:589, 610` |
| 27 | `MedicationSheetStateTest` tests `StartMedicationEditor` with `prefill` for add-from-tile, but **no test** for the "Prescribe" button (blank custom drug) path — the design says "both are the blank defaults" (§119). | — |

---

### Flaky / Timing Risks

| # | Test | Risk |
|---|------|------|
| 28 | `MedicationStopBarTest.theBarExpiresAfterTheInjectedWindow()` advances virtual clock by 1800 ms + 400 ms and expects exact expiry. Virtual clock advancement in Compose test rules can be non-deterministic under load. | Medium |
| 29 | `MedsTabSheetTest` uses `compose.waitForIdle()` after every click but the sheet animation (modal bottom sheet expand/collapse) may not be idle when the assertion runs. The test does not `waitForIdle` after `click("saveMedication")` before checking `node("medicationSheet").assertDoesNotExist()` in some cases (e.g. line 867–868). | Medium |

---

### Summary

| Severity | Count |
|----------|-------|
| Blocker  | 7 |
| Major    | 13 |
| Minor    | 7 |

**Top 3 fixes that would catch the most regressions:**

1. Add a UI test that exercises **"Save and add another"** — verifies the sheet reopens blank, keyboard/focus retained, picker stays open, search text and scroll survive.
2. Add a **360 dp + keyboard** layout test (can reuse `LayoutFixture.narrowPhone()` + `performTextInput` to force IME) asserting Save button visible, form scrolls, no truncation of name/dose/frequency.
3. Add a test for **"Show past" toggle state survival** across add/stop/edit and a test that the edited row's values actually update in the list (not just scroll position).

The reducer unit tests (`MedicationSheetStateTest`) are thorough for the state machine. The gaps are almost entirely in **UI integration tests** — the sheet hosting, keyboard, 360 dp layout, toggle persistence, and the "Save and add another" flow.
[Tokens] upload: 90978 download: 3291 cache_read: 0 cache_write: 0

