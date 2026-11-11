# Code Review: Stage B2 patch (frequency-required rule)

## Blocker

**B1. The frequency rule now lives in two places that can disagree — and the form's inline error is keyed to the wrong one.**
`MedicationForm.kt:216` gates the new error message on `frequencyMissing`, a flag that existed *before* this patch — i.e., it was written when the design still said frequency was optional (old comment in `ClinicalCommands.kt:445`: "Frequency is optional: the editor no longer asks for it"). The command-side rule is different and new: blank frequency **on a new medication only** (`baseMedicationRevision == null`, `ClinicalCommands.kt:449-451`). Two concrete failures follow from whichever way `frequencyMissing` is computed:

- If `frequencyMissing` is simply `values.frequency.isBlank()`: editing a **stored row with no frequency** shows a red error state plus the new hard message "Frequency required" (`MedicationForm.kt:216`) — while Save succeeds, because the command exempts updates. The doctor sees a required-field error on a field the design explicitly says is not required for updates (Decision, design line 67: "An update of a stored row does not require it").
- If `frequencyMissing` means "Other chosen but custom text blank" (the plausible original meaning): a **new** medication with a fully blank frequency is rejected by `SaveMedicationCommand.prepare`, but the form shows *nothing* next to the frequency field, violating design §7 line 160: "shown next to the frequency field".

Either way, the patch adds the message without reconciling the flag, and design Alternative B's rejected risk ("two places for a validation bug") is realized inside one file pair. The fix is to derive the inline error from the same rule the command enforces (new-vs-update discriminator shared from state, not recomputed in the composable — which would also violate "no business logic in composables").

## Major

**M1. "Other chosen and no text" is only covered if the custom entry writes into `values.frequency` directly.**
The decision (design line 67) requires rejecting Other-with-no-text. The check `frequency.isEmpty()` (`ClinicalCommands.kt:449-451`) only catches that if the custom text *is* the frequency string. If the fixed list stores a sentinel (e.g., the literal "Other") with the custom text elsewhere, a new medication with Other selected and no text passes validation and saves a frequency-less drug — the exact case the owner approved the rule for. Verify the draft mapping; if a sentinel exists, the check must be `frequency.isEmpty() || (isOther && customText.isBlank())`.

**M2. No test for the new branch.**
The patch adds a conditional branch in a level-3 save path with zero test changes. The design's own Verification Plan (line 347) requires "every new branch in the `StartMedicationEditor` handling … has a unit or UI test", and `MedicationSheetStateTest.kt` (line 336) lists the draft rules. Missing cases: new med + blank frequency rejected; new med + Other/no-text rejected; **update of a stored blank-frequency row accepted** (the one-tap stop and Undo paths depend on this exemption — if `baseMedicationRevision` is not set on the stop/undo command builds, one-tap stop is now *broken* for frequency-less rows, regressing CUJ 1).

**M3. `REQUIRED_FREQUENCY` provenance and error mapping unverified.**
The patch does not show where `ClinicalValidationError.REQUIRED_FREQUENCY` is declared. If it is newly added and lives in `ClinicalModels.kt`, that violates the design's "must not change" list (line 331). If it is newly added anywhere, the sheet's field-error mapping must learn it; otherwise the rejection surfaces as a generic `SaveUiError` banner, not "next to the frequency field" (design line 160).

## Minor

**m1. String resource not touched in this patch.** `MedicationForm.kt:216` references `R.string.medication_frequency_required`, but the patch contains no `strings.xml` change. Either the string pre-exists (possibly with stale wording from the optional-frequency era — check it says the new rule) or the build breaks. Design's target-file list (line 324-327) expects string work in this stage; confirm which.

**m2. Multi-error ordering.** `ClinicalCommands.kt:449-451` appends after `INVALID_ROUTE`; if both route and frequency are invalid, whichever the UI displays first may mask the frequency error. Harmless but worth pinning in the test from M2.

**m3. Stale comment removed cleanly — good** — but grep for other comments/tests still asserting "frequency is optional" (the old `MedicationEditor` was deleted; its tests may survive in `AncWorkflowTest.kt`).

**m4. Process:** `ClinicalCommands.kt` is a level-3 path; per AGENTS.md this edit requires a frontier-tier review and an independent test review before acceptance — this patch ships neither, and M2 shows why the test review matters.

## What checks out

- The update/new discriminator correctly exempts the one-tap stop and Undo **if** those command builds carry `baseMedicationRevision` (verify under M2).
- No hard-coded colours, no layout changes, no parallel component, no route or schema changes; design-doc edits (lines 39-40, 48, 56, 64-65) are internally consistent with the Decisions section.
- The `errorMessage` uses a string resource, not hard-coded text — correct per AGENTS.md; the concern in B1 is logic, not i18n.
