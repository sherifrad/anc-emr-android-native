# Independent Test Review — Medication Sheet / One-Tap Stop (phase2)

**Verdict:** The reducer-level suite (`MedicationSheetStateTest`) is genuinely strong — most of it would fail if the level-3 state logic broke. The gaps are concentrated in the UI layer and the save wiring; several headline design behaviours have **no test at all**, and the UI harness re-implements the save path it claims to exercise.

## Blocker

1. **The real save wiring is never tested.** In `MedsTabSheetTest.onEvent`, `StopMedication`, `UndoMedicationStop` and `SubmitMedication` are hand-routed into a local `save()` that fabricates a command and commits it. `AncViewModel.saveMedicationQuick` (the actual production path added in `main.patch`) is never executed. If the view model failed to dispatch the quick save (e.g., event handler missing), **every UI test still passes**. No test would fail. This needs one instrumented test driving the real `AncViewModel`.
2. **"Scroll position survives add/stop/edit" — untested.** This is the design's Problem #1 and Goal #1. The test plan (design §Tests) promises it in `MedsTabSheetTest`; the delivered file has no scroll-position assertion. A regression that rebuilds the tab after save would pass everything. Similarly, **"Show past" toggle state surviving actions** is untested — and unimplemented in this phase.
3. **Failed sheet save (design §7)** — untested. `aFailedStopLeavesTheRowActiveAndShowsTheCause` covers stop only. No test submits through the sheet, rejects, and asserts the sheet stays open with the doctor's values and the existing `SaveUiError` message. If the sheet closed or wiped values on rejection, nothing fails.

## Major

4. **Enter-to-next-field & keyboard visibility (Goals §6)** — completely absent from `MedsTabSheetTest`, despite being promised in the test plan. Likewise **"Save and add another" keeps focus/keyboard** (the documented flicker risk has a named mitigation test that does not exist).
5. **Sheet shows during save / disabled controls**: no test that sheet Cancel/Save are disabled with a progress indicator while a draft save runs (`assertIsNotEnabled` is used only for tab entry points). Also, **modal scrim blocks tab swipes** (§2) is asserted nowhere.
6. **Route read-only on an existing row** (§2: "choosable only when adding"): `t400` checks the choosable path; no test opens an *edit* sheet and asserts no route choice group appears. A regression enabling route editing on stored rows passes.
7. **Date field (§3)**: no UI test that the sheet's one date field is displayed, labelled (Start/Stop date), defaults to today's *displayed* value, or that the "Today" shortcut works. Only draft *state* (`startDate == today`) is asserted.
8. **Collapse the picker / new three-tap flow (§5)**: neither implemented nor tested — `medSearchField` here is the old always-open picker. Acceptable for phase 2 scope, but the design does not mark these as deferred; flag it so phase 3 owns them.
9. **Undo bar's 10-second expiry timer is untested.** `LaunchedEffect(undo.token) { delay(10_000) }` is only covered at state level (`MedicationStopUndoExpired(token)` cleared / stale token kept). If the delay were wrong, mis-keyed (never fires), or fired for the wrong token, no test fails. Inject the duration (design constant exists) or test expiry in UI.
10. **360dp layout / keyboard-open sheet height (§2, Risks)** — promised ("layout at 360dp") but `MedsTabSheetTest` mounts no narrow fixture. Only the old `LayoutFixture` test in `AncWorkflowTest` covers narrow mounting of the *sheet* tags.

## Tests that would fail on real defects (the good part)

- Route never changes: `startingADraftNeverChangesTheRoute`, `t400`, `stopIsOneTapWithoutASheetAndUndoPutsTheRowBack` — would fail.
- original/values rules incl. dirty-on-open, touched: `addFromTileStartsFromBlankDefaultsWithTheTileOnTop`, `prefillIsIgnoredWhenEditingAStoredRow`, `firstFieldChangeSetsTouchedWithoutAffectingDirty`.
- Stop command correctness (revision, end date, flag resets, other fields preserved): `stopOpensNoDraftAndBuildsTheStoredRowAsStoppedTodayAtItsRevision` — strongly failing.
- Undo as second save at the post-stop revision: `undoIsASecondSaveOfTheSameRowWithItsNewRevision` — strong.
- Under-fire/ignore rules: `startIsIgnoredWhileASaveRuns`, `whileTheStopSavesEveryOtherStartIsIgnored`, `stopIsIgnoredWhenItCannotRun` — thorough negative coverage.
- Error/UNRESOLVED state machine: `stopFailureLeavesTheRowActiveAndShowsTheCause`, `unresolvedSaveKeepsMedicationEditingBlocked`.
- Undo-bar lifecycle drops: `theUndoBarIsDroppedWhenAnotherActionStartsOrThePatientChanges` — would fail on leaks.
- Entry-point disabling during a stop save: `everyEntryPointIsDisabledWhileAStopSaves` — would fail.
- Tile-for-active-drug no-op: `aTileForAnActiveDrugShowsItsCheckAndDoesNothing`.

## Weak / tautological asserts

- Undo bar 10s: `undetermined expiry` noted above.
- `MedicationSheetStateTest` commits via its own `committed()`/`commit()` reimplementations of the store. Fine for reducer tests (documented), but two different fakes in two suites mean store-semantics drift is uncatchable.
- `t404` only asserts absent tags + one hidden value; it wouldn't catch a newly broken visible field.
- `PatientHubPureModelTest` changes only constructor args; the new `frequency` field's propagation into prefill is covered only via `MedicationSheetStateTest` (prefill given directly) and `MedsTabSheetTest` (tile → values), which is adequate.

## Flaky timing

- **Midnight race:** `today` is captured in a class field (`MedicationSheetStateTest`) at first use while the reducer calls `LocalDate.now()` per event; a test straddling midnight fails intermittently. `MedsTabSheetTest` calls `LocalDate.now()` at multiple instants across one test. Inject a clock or capture once per assertion group.
- `waitForIdle()` around sheet dismissal and `MedicationStopBar` transitions is fine; no sleeps observed. The `delay`-driven bar could theoretically fire `MedicationStopUndoExpired` mid-test if a suite left it mounted >10s — not currently triggered.

## Missing negative cases

- Submit sheet with blank name/dose → validation errors shown next to fields (§7 "validation errors still show next to their fields") — untested.
- Stop tap twice in rapid succession (double-tap guard) — only tested as state-level `assertSame`, never from UI.
- Cancel pressed while save pending (should be disabled) — untested.
- Stop a Patient-non-compliant row's *end date* preservation on undo — covered; on stop, `endDate` overwrite when one already existed — not covered.
- Recovery of a medication draft on a *different* patient/route — reducer guards exist but recovery is only tested on the happy path.
