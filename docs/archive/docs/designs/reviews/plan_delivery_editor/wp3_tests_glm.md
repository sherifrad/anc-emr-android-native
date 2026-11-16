# Test Review: Delivery Plan Editor (WP3)

## Blocker

**B1. The two unit test suites the design promises do not exist in this patch.** Section 9 of the design specifies `DeliveryNotesComposeTest.kt` (compose normalisation, pinned D4 counterexamples, Theorem A/B property tests, idempotence, `isComposedDeliveryNotes`) and `DeliveryPlanReducerTest.kt` (guards, route preservation, field-change semantics, submit/cancel/failure, draft clearing on route/patient change, dating rule A13). Neither file is in the patch. Every parser and reducer behaviour in the design is therefore currently untested: if the compose rule, the strict-label regex, or any reducer branch (including "success keeps the route") regresses, **no test fails**. The instrumented suite exercises the happy path only and cannot substitute for these.

**B2. No round-trip proof at all.** The entire safety argument of the design rests on Theorem A/B and Corollary C. Without `parse(compose(U)) == N(U)` property tests, a regex regression (e.g. `DOT_MATCHES_ALL` removed, a label made a prefix of another) silently corrupts clinical notes on the first save — the exact defect class this design exists to prevent. The instrumented tests only assert one fees line survives; they would pass while hospital, plan, or postpartum round-trips break.

## Major

**M1. Reducer guard behaviours have no test anywhere.** D10 (no-op when `blocksEditing()`, `hasDirtyDraft`, archived/unknown patient) and A8's "draft cleared on route change / patient change" tests are absent from both the missing reducer file and the instrumented suite. A regression that lets the pencil open an editor on an archived patient, or leaves a stale draft that blocks every other editor (the A-draft-blocks-editors failure mode), passes CI.

**M2. Write-back fidelity to other columns is untested.** The harness's `finish()` commits only `obstetricNotes` and never inspects the rest of the captured command. D2's claim ("a sheet save writes the same non-notes fields as today's form") and A13's dating rule (`intakeDatingUpdate == null`) are pinned only by reducer tests that don't exist. Add one assertion that the captured `SavePatientProfileCommand`'s other fields equal the stored patient's.

**M3. The 360 dp / font-scale 1.3 layout test promised in section 9 is missing.** No test renders the sheet at 360 dp, and nothing asserts label text from semantics (A8 explicitly required "assert the full text from semantics, not only no ellipsis"). Truncation of a clinical field label regresses undetected. This is the AGENTS.md viewport invariant.

**M4. Notice-dismissal and idempotence sequence untested.** The design's own verification step 6.6 ("reopen the sheet and confirm there is no notice") has no automated test, and `isComposedDeliveryNotes` correctness — which gates whether the notice ever shows — has no unit test. A regression where the notice shows forever (or never shows) passes.

**M5. No test that an untouched save writes nothing.** A15/A16 changed the mechanism twice (disabled button vs. `CancelEditor`). `saveIsDisabledUntilAFieldChanges` covers disabled state, but nothing asserts that a save triggered through any route with `touched == false` issues no command (the harness's `commands` list would catch this cheaply). Also missing per A6: type-then-delete must leave Save disabled (normalised touched comparison).

**M6. The field-error / profile-invalid path (A5) is untested.** `SheetSaveMessage` has a `fieldErrorMessage` branch for `draft.errors` non-empty, but the failing-save test only uses `STALE_OR_CONFLICT`. The "profile has a problem in a field not shown here" message — the doctor's only recovery hint — can break silently. Same for RECONCILING/UNRESOLVED messages in this sheet.

**M7. The keyboard/IME "focused field stays visible" requirement (Goal 6) has zero coverage**, not even the promised manual checklist item is recorded as an instrumented test. `revealOnFocus` is wired but unverified for this sheet.

## Minor

**m1. `exists()` is a weak oracle.** `fetchSemanticsNodes().isNotEmpty()` matches nodes that exist but are invisible, and is used for "sheet is gone" after actions that animate. `runOnIdle { finish(...) }` followed by `waitForIdle` then an existence check is mildly flaky against the sheet's hide animation; an `assertDoesNotExist` after settling or an explicit animation wait would be sturdier. All *positive* sheet assertions correctly use `assertIsDisplayed`.

**m2. `deliveryPlanSheetDraft(patient.id)` patient-matching is untested.** The harness has exactly one patient; a two-patient state with a draft for patient A open while viewing B would expose the id-guard. Cheap to add.

**m3. Spinner tag never asserted.** Design promises "a spinner plus Saving label appears while saving"; the held-save test checks `deliveryPlanSaveProgress` only, never `deliveryPlanSaveSpinner`.

**m4. Notice content drifts unchecked.** Only the tag is asserted; A2's specific disclosed-losses wording is not. A translation or edit that drops the "words such as 'agreed' are dropped" sentence passes.

**m5. Undo-bar behaviour around the sheet is untested.** The AncApp change (hide `UndoBarHost` while the sheet is open) plus the `footer = { UndoBarHost(...) }` wiring has no assertion that the bar appears once inside the sheet and not twice.

**m6. Back-press while saving (A8/A17) has no instrumented or reducer test** in what exists here; it is promised for the missing reducer file only.

## What the existing tests do catch

To be fair: the pencil→sheet-over-tab/no-route-change test, card fees update after save, untouched/touched cancel with discard dialog, disabled-while-saving, and the stored-labels-equal-card-titles test (`theStoredLabelsAreTheCardTitles`, a good drift guard) would each fail on a real regression in their scope, and they run through the real `AncApp` + reducer. The gap is everything outside that narrow happy path — chiefly the parser/compose round trip and all reducer semantics (B1/B2), which are the behaviours the design itself calls the hard part.
