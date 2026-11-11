# Independent Test Review — Phase 3 (Medication Section Revision, Stage 1)

## Blocker

**1. The one-tap stop's core contract is almost untested.** The design's Stop rule (Section 4) and CUJ 1 specify: stop stores `Discontinued` with end date = today; the row moves to "Show past"; an Undo bar shows for 10 seconds; **tapping Undo restores the earlier status and end date via a second save using the row's new revision**; the bar is dropped on any other action or on leaving the patient; **a failed stop save leaves the row active and shows the cause**. This patch asserts only that `medStopUndoBar` appears (`thePickerStaysOpenWithItsSearch...`) and that the row loses its stop symbol (`showPastStaysOpen...`). No test anywhere in the patch asserts the stored status/end date, that Undo click actually restores the *original* values (not defaults), the 10-second expiry, bar dismissal on other actions, or stop failure with `holdSaves` (which the harness already supports). A regression that stops with the wrong date, or whose Undo writes blank defaults, would pass this suite.

**2. Draft recovery is absent.** The design promises "a test covers recovery of add, stop and edit drafts [that] reopen the sheet on the Meds tab and not navigate" (Risks, Tests list). No recovery test appears. The `MedsTabRestorationTest` covers Compose saved state only — a recovered persisted draft is a different path (crash → restore → sheet reopens, no route change). Untested.

## Major

**3. Failed-save behaviour has no test.** Section 7: a rejected save keeps the sheet open with the doctor's values and shows the existing `SaveUiError` message; Save can be pressed again; no auto-retry. Nothing here covers any of that (nor validation errors beside their fields). Given this is the clinical write path, this is the largest single omission after stop/undo.

**4. Back with a draft open, and the touched/Cancel rules, are untested.** Design: Back closes the sheet through cancel/discard logic and does not leave the workspace; Cancel prompts only after the doctor edits (the `touched` flag). `MedicationEntryDesignTest`'s only change is re-scoping the "Prophylaxis" assertion to the sheet — a scoping fix, not coverage. No test asserts Cancel on an untouched prefill closes immediately, or that Back does not leave the patient.

**5. Route read-only on edit is never asserted.** `routesStayChoosable...` (add path) asserts `medRouteReadOnly` does **not** exist on add. No test opens an existing row and asserts the route is read-only there. `thePickerStaysOpen...` edits `ui-m2` dose but never checks the route control. A regression making route editable on existing rows would pass.

**6. The `PatientHubScreen` pager fix has no test.** `main.patch` changes real behavior (the settled-page effect now reads the latest route, fixing "return to the opening tab was never reported") with zero accompanying test — unit or UI. This is exactly the kind of regression-prone async fix the design's Verification Plan claims to gate.

**7. Date field rules untested.** No test asserts the date defaults to today, that the label switches between Start/Stop date, or that the Today shortcut works in the sheet. The keyboard test only proves `medDate` can receive focus.

## Minor

**8. Weak / conditional asserts:**
- `titlesAndLabelsComeFromTheStringsFile`: `assertTrue(str(id).isNotBlank())` for nine IDs is tautological against the UI — it only proves strings.xml is non-empty, not that the screen shows them. Only three tags are actually text-asserted.
- `thePickerStartsAsOneRow...`: the scroll assertion is inside `if (catalog().size > 5)` — if the demo catalog is ≤5, a broken inner-scroll silently passes.
- `assertFocused`/`assertAboveKeyboard` use `runCatching { ... }.isSuccess` retry loops before a final assert; acceptable, but the waitUntil windows (5s/10s) are generous and the loops can hide intermittent ordering bugs rather than fail on them.

**9. Flaky-timing risks:** the IME tests depend on emulator keyboard animation (`awaitKeyboard` up to 10s, `WindowInspector` polling) and are `@SdkSuppress(minSdkVersion = 30)` — they **silently skip** on an older CI emulator, removing keyboard coverage without any failure signal. The suite also hard-fails unless `set_emulator_360dp.sh` was run — environment-dependent, fine locally, fragile in CI. `showTile`'s manual `ScrollBy` + bounds arithmetic can race with recomposition on slow devices.

**10. Missing negative cases:**
- No test that a tile with blank frequency shows "Choose frequency" and that saving without choosing is blocked by validation (only `readyTiles` — frequency-prepopulated — are exercised for add).
- The "Recent with no history" empty message (`no_recent_medications` branch) is never shown by any test.
- The disabled-while-saving checks don't confirm `medShowPastToggle` *stays enabled* (the code comment claims it does) — an accidental disabling would pass.
- Disabled state while a *draft is open* (vs. during a save) is never asserted — the sheet scrim makes taps unreachable, but the design explicitly requires visible disabling.
- `MedsTabRestorationTest` never exercises `expandedMedId` or the picker's source chip in restoration, and passes `onEvent = {}`, so it can't catch a restore that resurrects stale draft state.

## Verdict

Phase 3's picker, Show-past, strings, 360dp typography and keyboard/focus coverage is solid and well-instrumented. But the patch adds no coverage for the three highest-risk behaviours the design names for this phase's neighbours: stop persistence + undo restoration + undo expiry, failed saves, and draft recovery. Those were deferred in earlier phases at best; as a phase gate, **do not accept until Blockers 1–2 and Majors 3–5 have named, failing-if-broken tests.**
