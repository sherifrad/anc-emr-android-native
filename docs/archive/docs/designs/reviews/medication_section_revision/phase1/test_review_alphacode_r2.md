# Test Review: Medication Form & Sheet (Stage 1)

## Blocker Gaps

| # | Behaviour from Design | Test Coverage | Why It's a Blocker |
|---|----------------------|---------------|---------------------|
| 1 | **Route never changes** — `AncRoute.MedicationEditor` deleted; starting a draft leaves navigation on workspace | **No test asserts route stability**. `MedicationFormTest` only mounts the sheet in isolation; it never exercises `AncState` reducer or `PatientHubScreen` to verify the route stays `Workspace(MEDICATIONS)`. | The entire Stage 1 purpose is eliminating the full-screen navigation. Without a test that fails when the route changes, a regression reintroducing `AncRoute.MedicationEditor` passes all current tests. |
| 2 | **One-tap stop (no sheet)** — tapping stop symbol saves `Discontinued` + today, shows 10s Undo bar, row moves to "Show past" | **No test for one-tap stop**. `MedicationFormTest` only tests the sheet path (`togglingToStoppedSwapsDateLabelAndTypingWritesEndDate` opens the sheet and toggles status). The design explicitly says stop has **no sheet**. | If the stop symbol accidentally opens the sheet, no test catches it. The Undo bar (10s, restores previous status/endDate) is completely untested. |
| 3 | **Collapsed "Add medication" picker** — single row expands to search + height-limited list; tiles prefill dose/route/frequency | **No test for picker collapse/expand, search, tile prefill, or "Show past" toggle**. `MedicationFormTest` only covers the sheet form. | The picker replacing the always-open tile list is a primary Goal (#2, #3). No test verifies it exists, collapses, searches, or that tiles carry `frequency` into the draft. |
| 4 | **Scroll position, search text, "Show past" survive every action** | **No test**. The design lists this as Goal #1 and CUJ 1/2/3 Then-clauses. | A regression that rebuilds the tab (the original bug) passes all current tests. |
| 5 | **Entry points disabled while save runs or draft open** — picker tiles, stop symbols, edit actions, "Prescribe" button all visibly disabled | **Partial**: `buttonsAndFieldsAreDisabledWithProgressWhileSaving` and `sheetCannotBeDismissedWhileSaving` cover the sheet's own controls. **Missing**: MedsTab-level disabling of stop symbols, picker tiles, edit taps, Prescribe button. | Silent tap drops are a listed Risk (#172). No test verifies the tab disables its entry points. |
| 6 | **Draft recovery reopens sheet on Meds tab (not navigate)** — add, stop, edit drafts | **No test**. `DraftRecoveryTest.kt` is listed in the plan but not provided; `MedicationFormTest` has no recovery case. | Listed Risk #170. A recovered draft that navigates to the deleted editor would pass all current tests. |

---

## Major Gaps

| # | Behaviour | Test Status | Gap Detail |
|---|-----------|-------------|------------|
| 7 | **Enter → next field, focused field visible above keyboard** | `enterMovesFocusFromNameToDoseToDateToNotes` tests focus order but **never asserts `revealOnFocus` / `bringIntoView` actually scrolls the field into view**. It only checks `assertIsFocused`. | Design Goal #33: "focused field stays fully visible above the keyboard". No test runs with IME open at 360dp and measures visibility. |
| 8 | **"Save and add another" keeps focus & keyboard** | **No test**. Plan lists `MedsTabSheetTest` check: "Save and add another does not lose focus or the keyboard". | Risk #171 (sheet flicker). Current test `saveAndAddAnotherSendsItsOwnEvent` only checks the event. |
| 9 | **Failed save shows existing `SaveUiError` message, keeps values, enables Save/Cancel** | `rejectedSaveShowsExistingMessageAndKeepsValues` covers one error (`STALE_OR_CONFLICT`). `everySaveErrorShowsItsOwnMessage` iterates all errors but **does not assert values persist or buttons re-enable** for each. | Design §7: "sheet stays open with the doctor's values… doctor can press Save again". Only one error exercised for the full behaviour. |
| 10 | **Cancel on untouched form closes at once; on touched shows discard dialog** | Covered (`cancelOnUntouchedFormClosesAtOnce`, `cancelOnTouchedFormAsksFirstAndKeepEditingSendsNothing`, `discardingOnTouchedFormSendsCancel`). **Missing**: swipe-down dismiss on untouched vs touched (only touched tested in `swipingDownOnTouchedFormAsksInsteadOfHiding`). | Design §5: "dismiss sends CancelEditor at once when untouched, asks first when touched". Untouched swipe-down path untested. |
| 11 | **Route read-only on existing row** — shows lock icon, label, note; no chip group | `routeIsReadOnlyOnExistingRowWithNote` covers this. **Missing**: assert `medRouteChoice-*` chips **do not exist** (test does this) but **no test verifies route value cannot be changed via any interaction** (e.g., accessibility action, test tag click). | Low risk but a completeness gap. |
| 12 | **Frequency "Other" chip → text field, Enter from dose goes to Other field** | `otherFrequencyTakesFocusAndEnterFromDoseGoesThere` covers focus. **Missing**: validation error when "Other" selected but custom text empty (`REQUIRED_FREQUENCY` shows on `medFreqError` when not in otherMode — covered in `missingFrequencyShowsItsMessageWhenNoOtherBox`), but **no test for "Other" mode with empty custom text**. | The form clears frequency when switching to Other (line 180). If user taps Save without typing, validation should fire. Untested. |
| 13 | **Status toggle: clicking already-selected sends nothing** | `clickingTheAlreadySelectedStatusSendsNothing` covers this. **Missing**: status change from Active→Stopped updates date label to "Stop date" and writes to `END_DATE` — covered in `togglingToStoppedSwapsDateLabelAndTypingWritesEndDate`. **Missing**: Stopped→Active switches back to `START_DATE`. | Round-trip not tested. |
| 14 | **Date field uses `AncDatePickerField` with "Today" shortcut** | **No test exercises the date picker dialog or "Today" shortcut**. `togglingToStoppedSwapsDateLabelAndTypingWritesEndDate` types raw digits into the field. | Design §3: "reuses existing `AncDatePickerField` (with its 'Today' shortcut)". No test verifies the picker opens or Today works. |
| 15 | **Sheet height limited at 360dp with keyboard; Save button stays visible** | **No test**. Plan requires instrumented test at 360dp with keyboard shown. | Risk #173. Critical for 360dp viewport rule. |
| 16 | **Draft `touched` flag set on first field change; Cancel prompt only after edit** | `cancelOnTouchedFormAsksFirstAndKeepEditingSendsNothing` uses `touched=true` from mount. **No test verifies `touched` transitions from false→true on first `MedicationDraftChanged` event**. | Design §1: "touched flag… set by the first field change". The flag's mutation is untested. |
| 17 | **Prefill rules for add (catalog tile), edit (stored row), custom (blank defaults)** | **No test**. `MedicationFormTest` mounts with hand-crafted `values` but never exercises the `StartMedicationEditor` reducer with `prefill` to verify `original`/`values` rules (Design §1, bullets under "What original and values hold at start"). | The draft construction logic lives in `AncState.kt` (level-3). No test asserts the four prefill cases. |
| 18 | **Stop save uses row's current revision; Undo uses new revision** | **No test**. Design §4: "Undo is a second normal save… using the row's new revision". | Level-3 logic in `AncState.kt`/`EditorHooks.kt`. Untested. |

---

## Minor Gaps / Weak Assertions

| # | Test | Weakness |
|---|------|----------|
| 19 | `enterMovesFocusFromNameToDoseToDateToNotes` | Skips route field (read-only in add mode? No — route is choosable in add). Focus order: name → dose → **route** → frequency → date → notes. Test jumps dose→date, missing route and frequency. |
| 20 | `enterInNotesInsertsNewLineAndKeepsFocus` | Uses `performKeyInput { pressKey(Key.Enter) }` but **does not assert the newline is rendered in the UI**, only that the event value contains `\n`. |
| 21 | `validationErrorsShowNextToTheirFieldsAndInTheMessage` | Asserts 4 `correct_highlighted_field` messages but **does not verify each is adjacent to its field** (name, dose, Other box, date). Could be 4 messages stacked at top. |
| 22 | `everySaveErrorShowsItsOwnMessage` | Loops errors but **does not wait for recomposition between each** (`compose.runOnIdle { error = e }` + `waitForIdle` is correct), but **does not assert `saveMedication`/`cancelMedication` re-enabled for each error** (only checked in `rejectedSaveShowsExistingMessageAndKeepsValues` for one error). |
| 23 | `sheetCannotBeDismissedWhileSaving` | Sends `KEYCODE_BACK` but **does not test `onDismissRequest` lambda path** (swipe-down already tested). Back handling is covered but could be flaky on emulator. |
| 24 | `reconcilingShowsItsMessageWithControlsDisabled` | Only checks `saveMedication` and `cancelMedication` disabled. **Does not check `addAnotherMedication` or field disabled state** (unlike `buttonsAndFieldsAreDisabledWithProgressWhileSaving` which checks all). |
| 25 | `shown(tag)` helper uses `performScrollTo().assertIsDisplayed()` — **scrolls to find the node**, masking cases where the field is off-screen and *not* brought into view by `revealOnFocus`. | The helper defeats the very behaviour it should test (keyboard visibility). |

---

## Flaky Timing / Reliability Risks

| # | Test | Risk |
|---|------|------|
| 26 | `assertFocused` uses `waitUntil(5_000)` + `runCatching` — **5s timeout is long; if focus never arrives, test hangs 5s per assertion**. | Slow suite, masks real focus failures. |
| 27 | `swipingDownOnTouchedFormAsksInsteadOfHiding` — `performTouchInput { swipeDown() }` on `ModalBottomSheet` can be flaky; depends on touch slop, animation timing. | Known Compose test flake. |
| 28 | `sheetCannotBeDismissedWhileSaving` — sends hardware back key via `InstrumentationRegistry`; **requires emulator/device with focus on the sheet**. May pass on desktop test runner, fail on CI. | Environment-dependent. |
| 29 | `otherFrequencyTakesFocusAndEnterFromDoseGoesThere` — clicks `medFreqChoice-other`, then clicks `medDosage`, then `performImeAction()`. **Focus may not have settled on Other field before dose click**. | Race between `LaunchedEffect` focus request and test clicks. |

---

## Missing Negative Cases (Should Fail But Don't)

| # | Negative Scenario | Expected Test |
|---|-------------------|---------------|
| 30 | Start draft while another draft open → ignored | No test |
| 31 | Start draft while save `PENDING`/`RECONCILING` → ignored | No test |
| 32 | Tap stop symbol on already-stopped row → no-op | No test |
| 33 | Tap edit on row while sheet open → ignored | No test |
| 34 | Tap picker tile for already-active drug → shows check, no sheet | No test (Design §5: "tile for a drug that is already active shows its check mark and does not open the sheet") |
| 35 | Rapid double-tap Save → second ignored | No test |
| 36 | Back press with untouched draft → closes sheet, no dialog | Covered (`cancelOnUntouchedFormClosesAtOnce` via Cancel button; **back press path untested**) |
| 37 | Configuration change (rotation) with draft open → sheet stays, values retained | No test |
| 38 | Process death + draft recovery → sheet reopens on Meds tab | No test (Risk #170) |

---

## Summary

| Severity | Count | Key Missing Areas |
|----------|-------|-------------------|
| **Blocker** | 6 | Route stability, one-tap stop, picker/Show past, tab state survival, tab-level entry-point disabling, draft recovery |
| **Major** | 12 | Keyboard visibility, Save-and-add-another focus, full save-error matrix, untouched swipe-down, Other-frequency validation, date picker/Today, 360dp keyboard layout, touched flag mutation, prefill rules, stop/undo revision logic |
| **Minor** | 10 | Focus order completeness, newline rendering, error adjacency, back-key flakiness, helper masking bring-into-view |
| **Flaky** | 4 | 5s focus timeout, swipe-down, hardware back, focus race |

**Bottom line**: The test file thoroughly covers `MedicationForm` + `MedicationSheet` **in isolation** (field rendering, events, validation, save phases, cancel/discard, focus order). It **does not cover** the MedsTab integration, the picker, the one-tap stop, the route deletion, or any 360dp/keyboard layout behaviour — which are the **core Stage 1 deliverables**. The provided tests would pass even if the full-screen editor were still used, the picker remained an always-open tile list, or stop still opened a sheet.
[Tokens] upload: 31909 download: 3303 cache_read: 0 cache_write: 0

