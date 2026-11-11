# Independent test review — delivery plan sheet (WP2)

Scope reviewed: `wp2_tests.patch` (one new unit test file) and `wp2_main.patch` (state-layer implementation). I did not write either.

## Blockers

**B1. The entire compose/parser test suite is missing.** The design's own test plan (§9) requires `DeliveryNotesComposeTest.kt` — Theorem A, Theorem B, Corollary C, normalisation (NBSP, U+2028, U+0085, lone `\r`, tabs, `\r\n`), duplicate hospital collapse, `isComposedDeliveryNotes` true/false cases, the strict-rule pins including the deliberate behaviour change for exact-case `Postpartum Note:`/`Delivery Plan:` in older notes (D4, A3), 5,000-char values, and a label appearing inside a field value. **None of it is in the patch.** The write-back rule is the stated "hard part" of the design; as reviewed, a defect in `composeDeliveryNotes`/`COMPOSED_LINE` that corrupts or misroutes clinical notes on save would ship with zero failing tests. The `ANCReducer` tests only exercise compose indirectly through `composedNotes()` built by compose itself, so they cannot detect a broken round trip.

**B2. No test that a save writes the right text.** No test asserts the save command carries the composed `obstetricNotes`, or that the committed patient record equals what the doctor typed. `a committed sheet save keeps the route` *injects* the committed patients into the event rather than verifying what the save produced. The design's Goal 4 ("after a save, the card shows exactly what the doctor typed") is untested end to end. The ViewModel test listed in §9 ("submit sends `UpdatePatientCommand` with the composed notes") is absent.

**B3. The instrumented sheet tests (§9 `DeliveryPlanSheetTest`) are absent — and so is the UI.** `wp2_main.patch` touches only `state/`; there is no `DeliveryPlanSheet.kt`, no `EditorSheetParts.kt`, no card/PlanTab/hosting changes, no string resources. Every UI behaviour — sheet over Plan tab, tags, rewrite notice (A2 wording), disabled-while-saving, spinner, failure message, the A5 `delivery_plan_profile_invalid` case, discard dialog tags, 360 dp/font-scale-1.3 display, keyboard visibility — has no implementation and no test in the reviewed material. Either this is a partial patch (then these tests are still owed) or the feature is half-landed.

## Major

**M1. The stale-draft test promised by the amendment does not exist.** The amendment says "A reducer test still pins it (see A8)": a delivery draft cleared on route change and on patient change. The pasted file has only `the sheet draft belongs to its own patient`, which tests the `deliveryPlanSheetDraft` helper in isolation — near-tautological, and it does not test that `clearDrafts()` fires on route or patient change. If a stale draft for patient A survived while the hub shows patient B, every test here still passes; the sheet would edit the wrong patient's notes. Additionally, the new `RecordCommitted` route guard (`state.patientDraft?.deliveryPlan != null`) does not check `patientId` against the committed record — no test covers a commit for patient B while patient A's sheet draft is open.

**M2. Submit-failure behaviour untested.** §9 explicitly lists: failure keeps fields and values, sets `ERROR` and `saveError`. Missing. A regression that wiped the doctor's fields on a failed save would pass this suite.

**M3. Untouched-Save-writes-nothing (D6/A15) is untested.** A15 moved this to UI wiring ("Save sends `CancelEditor` when `touched == false`"), but the reducer no longer implements the untouched rule and no instrumented test exists to catch broken wiring. If Save sends `SubmitPatientProfile` while untouched, a silent rewrite of unedited clinical notes occurs with no failing test — the exact harm D6 exists to prevent.

**M4. Dismiss/Back while saving (A8) untested.** Guards for opening and field-change while saving are tested (`start does nothing while a save is in progress`, `a field change is ignored while a save is in progress`), but the design's "dismiss is blocked while saving" path is not.

**M5. The strings↔label-constant drift test (A8) is missing**, and no string resources appear in the main patch at all. Without it, a translated/edited card title silently desynchronises display text from the stored parser labels, breaking round-trips for every saved note.

## Minor

- **A11 untested:** `startDeliveryPlanEditor` resetting `savePhase` to IDLE and clearing `saveError` has no assertion.
- **The dating test (A13) is indirect.** `the sheet never asks for a dating update` calls `intakeDatingUpdate` on the seeded values directly; it would not catch a ViewModel that skips or misroutes the rule for sheet saves. It pins the seed, not the save flow.
- **`Back on a touched sheet asks before discarding` may be weak:** since touched ⇒ dirty, the pre-existing dirty-draft branch likely produces the same state, so the test passes whether or not the new delivery branch handles it. Behaviourally fine, but it does not discriminate the new code.
- **A14 (crash recovery of a touched sheet draft) untested.**
- **Reverse guard untested:** `StartPatientEditor` (and other editors) no-opping while a *delivery* draft is dirty.

## Tests that do earn their keep

These would fail on realistic defects: `a committed sheet save keeps the route` (route-swap regression), `Cancel closes an untouched sheet and keeps the route`, `Back on an untouched sheet closes it at once`, `start does nothing for an archived patient` / `for a patient that is not loaded` / `while a save is in progress` / `while another draft is dirty`, `a field change keeps the other three fields`, `a field change writes the composed notes into the draft`, `typing and deleting leaves the field untouched` (catches the A6/D6 typing-rewrite bug), `StartPatientEditor still opens the profile editor`, `the sheet is clean after start and dirty after a change`.

## Flaky timing

None found in the unit tests — no coroutines, delays, or clocks. The delayed-save/spinner tests belong to the missing instrumented file, where timing flakiness remains an open risk.

**Bottom line:** the reducer layer is reasonably pinned; the compose round trip, the saved output, and the entire UI are not. B1–B3 must be closed before this can be called tested.
