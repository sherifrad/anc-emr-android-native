# Independent Test Review — Medication Form/Sheet (Stage 1)

**Scope note:** the pasted evidence only covers `MedicationForm.kt`, `MedicationSheet.kt`, and `MedicationFormTest.kt`. CUJ 1 (one-tap stop + Undo), the route-stays-put rule, the picker, and draft recovery are claimed to live in `MedsTabSheetTest` / `MedicationSheetStateTest`, which were not provided — so this review can only confirm correctness of the form and sheet shell.

## Gaps (behaviours with no failing test)

**Blocker**
- **Save emits nothing, as far as any test says.** No test presses `saveMedication` or `addAnotherMedication` and asserts `AncEvent.SubmitMedication` / `SubmitMedicationAndAddAnother`. Every save test only checks enable/disable or render. A regression wiring Save to `CancelEditor` (or to `onEvent({})`) passes the whole suite. This is the core function of the sheet and is completely untested.
- **The "prompt only after the doctor edits" contract is unfalsifiable here.** `touched` is a parameter the test hardcodes; nothing proves the first field change sets it or that it resets on "Save and add another". If the reducer never sets `touched`, all cancel tests still pass.

**Major**
- **Dismissing a sheet during a running save is untested, and the code looks leaky.** `onDismissRequest = { if (enabled) requestCancel() }` gates only the callback, not the swipe. In Material 3, a downward swipe dismisses the sheet regardless, so during `PENDING` the CSS—analogous bug is that the sheet can vanish mid-save while the save continues with no visible surface to report the result. `MedsTabSheetTest` disables the entry points, but nothing attempts scrim/back/swipe dismissal under `PENDING`.
- **Only 1 of the ~6 save errors is exercised.** `STALE_OR_CONFLICT` only. The design names archived patient, cross-patient, storage error, result unknown; `SavePhase.UNRESOLVED` and `RECONCILING` branches in `SaveMessage` have zero tests. A crash in `stringRes()` mapping for any other code is invisible.
- **Validation error rendering is untested.** `medFreqError` exists in the form; no test references it. `REQUIRED_NAME`, `REQUIRED_DOSAGE`, `REQUIRED_DATE`/`INVALID_DATE`, `REQUIRED_FREQUENCY` presentation and the `hasFieldErrors` message path are never asserted. Design explicitly requires "validation errors show next to their fields".
- **"Focused field stays fully visible above the keyboard" is never verified.** `revealOnFocus` and `imePadding` have no test here; the 360dp/keyboard layout test is deferred to a file not pasted. If deferred tests don't exist, this Goal has no coverage.
- **Back gesture closing the sheet (not the workspace)** is in the design's Technical Plan §1 and test plan, but absent from the pasted files.

**Minor**
- `stoppedStatusRelabelsTheDateAsStopDate` only mounts with already-stopped values; it never toggles Active→Stopped in-session and checks the label/date swap, so the `dateField` switching logic is half-covered (the `medicationStatusAfterChoice` "change only when `next != status`" branch is also untested — clicking an already-selected button emitting nothing is not asserted).
- No test that typing into `medDate` emits `START_DATE`/`END_DATE` (the fake reducer maps it, but the form→event path for dates is never exercised).
- `medSaveSpinner` tag is never asserted; only the linear bar.

## Weak or tautological assertions
- `addModeRendersEveryFieldAndAllThreeButtons` mixes `assertIsDisplayed` (first two) with `assertExists` for the rest. `assertExists` passes for zero-sized or clipped nodes, so "every field renders at 360dp" is not actually claimed.
- `routeIsChoosableWhenAdding` asserts a `ROUTE` event exists but not its value — a route chip firing with an empty/wrong string passes.
- `otherFrequencyShowsTextBox…` uses `endTime.endsWith("Every 12 hours")`; the `endsWith` implies the event may carry composed content not checked. A prefix corruption slips through.
- `buttonsAreEnabledWhenIdle` checks only the Save button and no progress; it would not catch Cancel/addAnother stuck disabled in IDLE.
- `mount()` reimplements the reducer locally, so tests verify event emission only — fine as a form contract, but it means any reducer-side bug in these mappings is structurally invisible to this suite.

## Flaky / timing risks
- The Enter-focus chain runs through `FocusRequester.requestFocus()` without `waitUntil`; on slower emulators `assertIsFocused` can race composition.
- `focusOtherBox` swallows `IllegalStateException` "box not attached yet". On a slow first composition the request is dropped silently, so `medFreqOther.assertIsFocused()` in the Other-frequency test may pass only because the click also focused it — the `LaunchedEffect` path itself can be a persistent no-op that no test isolates.
- Live-region `SaveMessage` with `LiveRegionMode.Polite` under `assertIsDisplayed` is fine, but repeated Save Message state churn isn't tested for stale text retention.

## Missing negative cases
- Save pressed with empty name/dose/frequency → field errors asserted? None.
- Enter on last-field Done behaviour: design says last field is Done; form keeps `ImeAction.Next` on date and the dialog/date-field is where "Today" picker lives — no negative test that Enter in notes doesn't advance (correctly, it inserts a newline; untested).
- Cancel while an error message is showing (sheet should keep error cleared or retain; unspecified but untested).
- Save tapped twice quickly in the narrow window before phase flips to `PENDING` — no double-submit guard test.
- Editing an existing row and touching nothing, then Stop-date path (status change to `Discontinued` requiring `END_DATE`) mid-sheet — half-specified, untested.

## Verdict
The sheet's shell (render, disable, error display, cancel flow) is decently covered; its save contract is not. Two Blockers must close before this is acceptable: Save/add-another event emission, and dismissal-during-save. The unpasted files must actually exist with the behaviours claimed in the test plan, or Blocks 1 spillover remain unresolved.
