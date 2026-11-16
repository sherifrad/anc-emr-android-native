# Test Coverage Review: Medication Section Revision (Stage 1)

## Summary
**Overall: Strong test suite with significant gaps in critical behaviors.** The tests cover most happy-path CUJs well but miss several Blocker/Major behaviors around state persistence, error handling, and edge cases. 43 tests across two files, but key design invariants lack verification.

---

## Blocker Gaps (Tests that would NOT fail if the behavior broke)

### 1. **Navigation route never changes during medication actions** (Design §28, §96, §113)
- **Missing test**: No test asserts `state.route == medsTab` *throughout* a stop, edit, or add flow—including during save, after save, on error, on undo. Current tests check route only at start/end (`tappingATileOpensTheSheet...`, `editAndPrescribeOpenTheSheetOnTheTab`, `stopIsOneTapWithoutASheetAndUndoPutsTheRowBack`). If the route briefly flips to `MedicationEditor` and back, these pass.
- **Required**: A test that records route at every reducer step during a full action and asserts it never leaves `Workspace(..., MEDICATIONS)`.

### 2. **Scroll position, search text, "Show past" survive every action** (Design §28, §30, CUJ 1–3)
- **Partial coverage**: `theListKeepsItsScrollPositionThroughEditStopAndAdd` checks Y-position of three rows/buttons after each action—good.
- **Missing**: 
  - Search text survival across *stop* and *edit* (only checked for add in `tappingATileOpensTheSheet...` and `thePickerStaysOpenWithItsSearchThroughAddStopEditAndTabSwipes`).
  - "Show past" toggle state survival across *add* and *edit* (only checked for stop in `showPastStaysOpenThroughStopAddEditAndTabSwipes`).
  - Picker open/closed state survival (design says picker stays open; no test verifies it doesn't collapse after save/error/undo).

### 3. **Draft recovery reopens sheet on Meds tab, not navigation** (Design §170, Verification Plan)
- **Missing test**: No test kills the process (or clears composition) with a dirty draft and verifies `MedicationSheet` reappears on `Workspace(MEDICATIONS)`. `DraftRecoveryTest.kt` is listed in the plan but not provided; the existing tests only mount with a pre-built draft.

### 4. **One-tap stop uses row's current revision; undo uses the new revision** (Design §140)
- **Partial**: `stopIsOneTapWithoutASheetAndUndoPutsTheRowBack` asserts `expectedMedicationRevision == 2L` for stop and `3L` for undo—good.
- **Missing**: No test verifies that a *concurrent edit* on another device (stale revision) makes the stop fail with `STALE_OR_CONFLICT` and leaves the row active. `aFailedStopLeavesTheRowActiveAndShowsTheCause` uses `rejectWith = STALE_OR_CONFLICT` but doesn't prove the revision check is the cause.

### 5. **Entry points visibly disabled during save** (Design §149, §139)
- **Partial**: `everyEntryPointIsDisabledWhileAStopSaves`, `everyEntryPointIsDisabledWhileAnAddSaves`, `theSheetButtonsAreDisabledWithProgressWhileADraftSaves` cover most entry points.
- **Missing**: 
  - "Prescribe" button (`addCustomMedicationBtn`) during *edit* save (only checked during add/stop).
  - Picker toggle and search field during *edit* save.
  - Tab swipe during save (design: "modal scrim blocks tab swipes"; no test verifies swipe is ignored or disabled).

### 6. **Failed save keeps sheet open with values and shows existing error message** (Design §34, §158)
- **Partial**: `aFailedSheetSaveKeepsTheSheetOpenWithTheValuesAndTheMessage`, `aFailedAddKeepsTheSheetAndTheValues`, `aFailedStopLeavesTheRowActiveAndShowsTheCause` cover add/edit/stop.
- **Missing**: No test verifies *validation errors* (per-field) persist after a failed *server* save (i.e., validation + server error together). `rejectedSaveShowsExistingMessageAndKeepsValues` only checks server error.

---

## Major Gaps

### 7. **Enter-to-next-field with keyboard visibility** (Design §33, §153)
- **Partial**: `enterMovesFocusFromNameToDoseToDateToNotes`, `withTheKeyboardOpenAt360dpTheFocusedFieldAndSaveStayVisibleAndEnterMovesOn`, `saveAndAddAnotherKeepsTheKeyboardAndPutsFocusOnTheNextName` cover the happy path.
- **Missing**: 
  - Enter from **frequency "Other" custom field** → date (test `otherFrequencyTakesFocusAndEnterFromDoseGoesThere` checks dose→other, not other→date).
  - Enter from **route chip** (read-only in edit, choosable in add—no test for add).
  - Enter from **status toggle** → date.
  - Backward navigation (Shift+Tab or IME_ACTION_PREVIOUS)—not required but a gap if claimed.

### 8. **Date field label swaps: Start date ↔ Stop date** (Design §134, §135)
- **Partial**: `togglingToStoppedSwapsDateLabelAndTypingWritesEndDate`, `stoppedRowShowsItsStopDate`, `stoppedBackToActiveWritesStartDateAgain` cover label and field binding.
- **Missing**: No test verifies the **date picker dialog** opens with the correct default (today) and that picking a date writes to the correct field (start vs end). `AncDatePickerField`/`AncDatePickerDialog` are reused but not exercised.

### 9. **Route read-only on existing row; choosable on add** (Design §127, §147)
- **Partial**: `theEditSheetShowsTheRouteReadOnly`, `routeIsReadOnlyOnExistingRowWithNote`, `routeChipSendsItsExactValue`, `theRouteIsChoosableWhenAdding` cover basic rendering.
- **Missing**: No test verifies that **changing route on add** updates the draft and persists on save. `routeChipSendsItsExactValue` only checks the event fires.

### 10. **"Save and add another" flow** (Design §30, §129)
- **Partial**: `saveAndAddAnotherReopensABlankSheetAndKeepsTheTab`, `theChainedNameFocusHappensOnceAndNotAgainAfterRecreation`, `saveAndAddAnotherKeepsTheKeyboardAndPutsFocusOnTheNextName` cover focus/keyboard.
- **Missing**: 
  - No test verifies the **first drug is saved** before the blank sheet opens (only checks `commands.size == 2` implicitly).
  - No test for "Save and add another" **when the first save fails** (should not open blank sheet).
  - No test for "Save and add another" **with a catalog tile prefill** (design: picker stays open, next tile opens prefilled sheet).

### 11. **Undo bar: 10s timeout, dismissed by any other action, dropped on patient leave** (Design §31, §140)
- **Partial**: `stopIsOneTapWithoutASheetAndUndoPutsTheRowBack` checks undo works immediately.
- **Missing**: 
  - No test advances clock 10s and verifies bar disappears.
  - No test taps another entry point (edit, add, picker) and verifies bar drops.
  - No test switches patient and verifies bar drops (`aDraftOrAFailedStopOfOnePatientDoesNotFollowToAnother` checks error/draft don't follow, but not the undo bar specifically).

### 12. **Picker collapsed/expanded state, search, chips, empty states** (Design §146, §147)
- **Good coverage**: `thePickerStartsAsOneRowAndOpensToSearchChipsAndAListThatScrollsInsideItself`, `searchKeepsOnlyMatchingTilesAndSaysSoWhenNothingMatches`, `aTileShowsItsNameAndDoseRouteAndFrequency`, `recentListsTheDrugsThisPatientHadBefore`, `recentWithNoHistorySaysSo`, `tilesWithoutADoseOrFrequencySaySoAndABlankFrequencyOpensWithNoneChosen`.
- **Missing**: 
  - No test verifies **Common/Recent chips are mutually exclusive** (only one selected).
  - No test verifies **picker height limit** (~5 rows) with keyboard open (scrolls inside itself).
  - No test verifies **custom choice tile** ("Prescribe" in picker) opens blank sheet.

### 13. **360dp narrow viewport: text not truncated, buttons ≥48dp** (Design §36, §173)
- **Good coverage**: `rowsKeepTheirTextWholeAndTheirButtonsAt48dpAt360dpAnd200PercentText`, `theHeaderPrescribeButtonIsNotClippedAt360dpAnd200PercentText`, `thePickerKeepsItsTextWholeAt360dpAnd200PercentText`, `bottomButtonsAreNotClippedAt360dpAnd200PercentText`, `savingLabelIsNotClippedAt360dpAnd200PercentText`.
- **Missing**: 
  - No test for **sheet with keyboard at 360dp** verifying form fields + Save button fit (only `withTheKeyboardOpenAt360dpTheFocusedFieldAndSaveStayVisibleAndEnterMovesOn` checks focused field + Save, not all fields).
  - No test for **patient name, gestational age, blood pressure, high-risk alert** readability at 360dp (design §36—these are in the header, not MedsTab).

### 14. **Archived patient: no entry points** (Design §149)
- **Partial**: `anArchivedPatientShowsNoEntryPoints`, `aReadOnlyPatientWithNoMedicationsSeesTheEmptyStateWithoutItsAction` cover picker, stop, edit, delete, prescribe.
- **Missing**: No test verifies **"Show past" toggle hidden** for archived patient (only checked for empty state).

### 15. **Cross-patient isolation** (Design §149, §172)
- **Partial**: `aDraftOrAFailedStopOfOnePatientDoesNotFollowToAnother`, `thePickerShowPastAndOpenRowOfOnePatientDoNotFollowToAnother` cover draft, error, picker, show-past, open row.
- **Missing**: No test verifies **undo bar doesn't follow** to another patient.

### 16. **Stop symbol on stopped rows** (Design §29, §148)
- **Partial**: `stoppedDrugsSitBehindShowPastWithoutAStopSymbol` verifies no stop symbol on stopped row.
- **Missing**: No test verifies **edit action exists on stopped row** (design: "edit action" exists; `stoppedDrugsSitBehindShowPastWithoutAStopSymbol` checks `editMedication-ui-m3` exists—good).

### 17. **Notes: one line collapsed, full expanded** (Not explicitly in design but in implementation)
- **Covered**: `aClosedRowShowsOneLineOfNotesAndTheWholeNoteWhenOpened`—good.

### 18. **Empty state vs "no active medications"** (Design §29)
- **Covered**: `aPatientWithNoMedicationsSeesTheEmptyStateAndItsMainAction`, `onlyStoppedDrugsShowNoActiveMedicationsInsteadOfTheEmptyState`—good.

### 19. **Accessibility: toggle states, content descriptions** (Design §36 implies accessibility)
- **Covered**: `theTogglesExposeTheirStateAndAClickLabel`—good.

### 20. **Contrast: Stop label/icon ≥4.5:1** (Design §36)
- **Covered**: `theStopLabelAndIconHaveEnoughContrastInBothThemes`—good.

---

## Minor Gaps / Weak Assertions

### 21. **Tab swipe synchronization** (Design §125: "modal scrim blocks tab swipes")
- **Test**: `swipingAwayAndBackToTheOpeningTabReportsIt` verifies route changes on swipe—**but this is the opposite of the design**. The design says the sheet's scrim *blocks* swipes. If the sheet is open, this test would fail (route wouldn't change). No test verifies swipes are blocked *while sheet is open*.

### 22. **Delete action on medication row** (Not in design goals; appears in tests)
- Tests reference `delete-medication-*` tags and assert they're disabled during save. Design doesn't mention delete. If delete exists, it needs its own CUJ; if not, tests are testing unimplemented behavior.

### 23. **`MedicationQuickSave` / `medicationQuickSave` state** (Used in tests for stop/undo)
- Design §140: "Undo is not the undo coordinator: it is a second normal save... using the row's new revision." Tests use `state.medicationQuickSave` for stop/undo. No test verifies this path doesn't use `UndoCoordinator` (design §42: no change to `UndoCoordinator.kt`).

### 24. **Save phase transitions: PENDING → RECONCILING → ERROR/COMMITTED**
- **Partial**: `buttonsAndFieldsAreDisabledWithProgressWhileSaving` (PENDING), `reconcilingShowsItsMessageWithControlsDisabled`, `unresolvedShowsItsMessageAndCurrentlyDisablesEveryControl`, `rejectedSaveShowsExistingMessageAndKeepsValues`.
- **Missing**: No test verifies **RECONCILING → COMMITTED** clears draft and closes sheet. No test for **UNRESOLVED → retry** (design: "doctor can press Save again").

### 25. **Touched flag: discard prompt only after edit** (Design §116)
- **Covered**: `cancelOnUntouchedFormClosesAtOnce`, `cancelOnTouchedFormAsksFirstAndKeepEditingSendsNothing`, `discardingOnTouchedFormSendsCancel`, `swipingDownOnTouchedFormAsksInsteadOfHiding`, `swipingDownOnUntouchedFormCancelsAtOnce`, `discardPromptSurvivesRecreation`—good.

### 26. **Chained add: focus happens once, not after recreation** (Design §171)
- **Covered**: `theChainedNameFocusHappensOnceAndNotAgainAfterRecreation`—good.

### 27. **Frequency "Other" custom entry** (Design §128)
- **Partial**: `otherFrequencyTakesFocusAndEnterFromDoseGoesThere`, `otherChosenWithEmptyTextShowsTheFrequencyError`, `nextDraftWithCatalogFrequencyDoesNotKeepOtherMode`.
- **Missing**: No test verifies custom frequency **persists on save** and appears in the row.

### 28. **Status toggle: Active ↔ Discontinued only** (Design §72: stored statuses are Active, Completed, Discontinued, Patient non-compliant; screen shows Active/Stopped)
- **Partial**: `togglingToStoppedSwapsDateLabelAndTypingWritesEndDate`, `stoppedBackToActiveWritesStartDateAgain`.
- **Missing**: No test for **Patient non-compliant** or **Completed** statuses (may not be editable in sheet).

### 29. **Prophylaxis / adverse notes fields** (Not in design; appear in `MedicationDraftValues`)
- No tests exercise these fields.

---

## Flaky / Timing-Dependent Tests

| Test | Risk |
|------|------|
| `withTheKeyboardOpenAt360dpTheFocusedFieldAndSaveStayVisibleAndEnterMovesOn` | Uses `awaitKeyboard()` polling `WindowInspector`—flaky on slow CI; `SdkSuppress(minSdkVersion=30)` limits coverage. |
| `saveAndAddAnotherKeepsTheKeyboardAndPutsFocusOnTheNextName` | Same keyboard polling; also depends on `chainAfterAFrame` timing. |
| `theNextNameTakesFocusEvenWhenTheSheetClosesBetweenTheTwoDrafts` | Uses `chainAfterAFrame` + manual event injection; sensitive to frame timing. |
| `searchingThePickerWithTheKeyboardOpenKeepsTheResultsAboveTheKeyboard` | Keyboard polling + picker scroll timing. |
| `swipingAwayAndBackToTheOpeningTabReportsIt` | `performTouchInput { swipeRight() }` + `waitUntil` on route—flaky if animation duration changes. |

**Mitigation**: All keyboard tests should run on a controlled emulator (`.agents/scripts/set_emulator_360dp.sh` referenced but not enforced).

---

## Tautological / Weak Asserts

| Test | Issue |
|------|-------|
| `thePickerShowsItsTitlesLabelsAndFrequenciesFromTheStringsFile` | Only checks string resource IDs match; doesn't verify visible text isn't truncated/clipped. |
| `everySaveErrorShowsItsOwnMessage` | Iterates `SaveUiError.entries` but doesn't assert *distinct* messages—could all return same string. |
| `theStopLabelAndIconHaveEnoughContrastInBothThemes` | Reads theme colors via composition, not rendered pixels; passes if theme defines colors but they're not applied. |
| `assertButtonsUnclipped` | Measures unclipped bounds in a `Box(360dp)`—doesn't prove they're unclipped *inside the sheet* with keyboard. |

---

## Negative Cases Not Tested

1. **Rapid double-tap on stop symbol** → only one save (`aSecondStopTapWhileOneSavesIsIgnored` covers this—good).
2. **Rapid double-tap on picker tile** → only one sheet (no test).
3. **Edit sheet opened, then stop same drug from list** → stop should be disabled (no test).
4. **Stop drug, undo, stop again** → revision handling (no test).
5. **Save fails with `ARCHIVED_PATIENT`** → entry points hidden (partial: `anArchivedPatientShowsNoEntryPoints` but not during save).
6. **Save fails with `CROSS_PATIENT`** → no test.
7. **Keyboard dismissed externally (e.g., system gesture)** → sheet stays, focus lost (no test).
8. **Orientation change with sheet open** → draft survives, sheet reopens (partial: `discardPromptSurvivesRecreation`, `theChainedNameFocusHappensOnceAndNotAgainAfterRecreation`).
9. **Large frequency list scroll in sheet at 360dp** → no test.
10. **Date picker: cancel leaves field unchanged** → no test.

---

## Severity-Ranked Action List

| Severity | Gap | Required Test |
|----------|-----|---------------|
| **Blocker** | Route never changes during any medication action | Record route at every reducer step in a full add/stop/edit flow |
| **Blocker** | Draft recovery reopens sheet on Meds tab | Kill process with dirty draft; verify sheet on `Workspace(MEDICATIONS)` |
| **Blocker** | Search text & "Show past" survive stop/edit | Extend `theListKeepsItsScrollPosition...` to assert search/toggle |
| **Blocker** | Entry points disabled during *edit* save (prescribe, picker, tabs) | New test mirroring `everyEntryPointIsDisabledWhileAStopSaves` for edit |
| **Blocker** | Stale revision causes stop/edit failure with correct error | Force revision mismatch; assert `STALE_OR_CONFLICT` and row unchanged |
| **Major** | Date picker dialog integration (default today, writes correct field) | Open picker, pick date, assert draft field updated |
| **Major** | "Save and add another" failure doesn't open blank sheet | Set `rejectWith` on first save; assert sheet stays with values |
| **Major** | Undo bar: 10s timeout, dismissed by action, dropped on patient switch | Advance clock, tap another entry, switch patient; assert bar gone |
| **Major** | Tab swipe blocked while sheet open | Open sheet, swipe tab, assert route unchanged |
| **Major** | Picker height limit + keyboard scroll | At 360dp with keyboard, verify picker scrolls internally |
| **Major** | All SavePhase transitions tested (RECONCILING→COMMITTED, UNRESOLVED→retry) | Drive each transition; assert UI state |
| **Minor** | Custom frequency persists on save | Enter custom freq, save, verify row shows it |
| **Minor** | Patient non-compliant / Completed status handling | If editable, test; else verify read-only |
| **Minor** | Prophylaxis / adverse notes fields | Basic render + edit test |
| **Minor** | Contrast measured on rendered pixels, not theme | Use `PixelCopy` or screenshot diff |
| **Minor** | Header clinical data (name, GA, BP, risk) at 360dp | New test mounting full `PatientHubScreen` at 360dp |

---

## Verdict
**The test suite is thorough for happy-path CUJs but insufficient for a Level-3 (auth/security/clinical-safety) change.** The Blocker gaps mean a regression in the core invariant—"navigation route never changes"—would not be caught. The Major gaps leave error handling, undo behavior, and keyboard/layout edge cases unverified. Before frontier-tier review, add the Blocker tests and at least the Major undo/date-picker/tab-swipe tests.
[Tokens] upload: 49657 download: 4932 cache_read: 0 cache_write: 0

