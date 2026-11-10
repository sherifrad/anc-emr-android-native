# Stage 7: Design Critic (Claude Sonnet subagent, egm:egm-design), lines 1-214

Standard applied: feature-spec-standard.md. Verdict from the reviewer: do not approve; B1 to B3 need document changes before implementation begins.

## Blockers

**B1. Alternatives section (L169-214).**
- Alternatives A, B, D, E, F and G are rejected with "Rejected by author". The standard asks for a technical or scope reason, and no attribution to a person without a link. Several already carry a real reason in the Cons (A L172, B L178, D L188, F L200); the rejection line should state it. Alternative E gives only the frozen schema, which is a policy.
- Alternative H is a staging note, not an alternative. Alternative C has no pros or cons of its own.
- Missing alternatives: a non-modal inline expanding row; a dose change as a status edit plus a new row through two existing commands (avoids a new atomic command, a new undo command and two enum values); undo of a change through the existing delete and restore commands.

**B2. "All screens agree on active" is contradicted by the plan (L60, L121).**
- `medicationStatusLabel(status, isNonCompliant)` is also called in `ClinicalSummaryGenerator.kt:94` and `PatientPrintHtmlGenerator.kt:229`, which produce clinical output. The plan removes the two-argument form and lists neither file.
- `medicationEditorChoice` (`ClinicalModels.kt:42-46`) and `MedicationEditor.kt:341` read the stored status: a course-ended drug shows as stopped in the list but opens in the sheet as "On", and saving without touching status writes it back as Active. `PatientHubHeader.kt:407` uses `isNonCompliant` for the tab badge independent of the end date.
- The change rule (L126, "active row") does not say whether it uses the stored or the derived status.
- Recommendation: add the summary and print generators, the editor-choice logic and the badge to the scope, or state why stored status is safe there. Test a course-ended drug through the sheet, the summary and the print output.

**B3. Retry and idempotency of the replace command is not designed (L127, L158, file 7).**
- `AncViewModel.kt:670-671` freezes a command per editor session and casts it to `SaveMedicationCommand`; `:713-714` reuses it only if the fingerprint matches. For `ReplaceMedicationCommand` the cast returns null, so a retry after an ambiguous result creates a new operation id and a new row id. If the first attempt committed, this creates a second replacement.
- Recommendation: freeze and reuse the replace command including the new row id; choose `recordKind` at `AncViewModel.kt:673-677` for the replace path; test retry after an ambiguous result.

## Major

- **M1. Undo against concurrent edits.** The refusal message is not specified. `armUndo` (`UndoCoordinator.kt:106-112`) holds one `pendingUndo`; a second stop or change silently replaces the first undo. State the supersede rule and the refusal message; test undo after a second action.
- **M2. Failure messages are coarser than the spec assumes.** `SaveUiError` (`AncState.kt:195-204`) has 8 values; the mapper (`AncViewModel.kt:940-949`) merges stale revision, conflict and revision-exhausted, and merges invalid command, patient not found and storage locked. `RECORD_NOT_FOUND` has no UI mapping. `ClinicalCommandFailure` has 11 values. Add `SaveUiError` and its mapping to the scope. Say how "specific cause" differs when both revisions are stale.
- **M3. Course-length semantics.** Inclusive or exclusive end date is not stated (a 3-day course shows active for 4 days under "before today counts as stopped"). The clock is the device clock; a screen open past midnight does not update, and a backdated clock reactivates stopped drugs. Define inclusivity, the clock, and test the midnight boundary and clock skew.
- **M4. Silent drops.** `startMedicationEditor` (`AncState.kt:1471`) ignores a start when `savePhase.blocksEditing()` or `hasDirtyDraft` (9 other drafts, `AncState.kt:683-695`). The plan disables only picker tiles; row stop symbols, the Add row and other entry points are not covered. "Save and add another" (`AncViewModel.kt:789-797`) closes and reopens the sheet through `clearDrafts()`: flicker, lost focus and keyboard dismissal are not addressed.
- **M5. "Quick actions run one at a time" is asserted, not designed.** After Save the sheet closes while the undo bar shows; nothing serializes the next action against it. The rejection of Alternative A rests on the sheet blocking a second action, but it only holds while the sheet is open.
- **M6. Notes-based change marker.** `medicationInstructions(dosage, route, notes)` (`ClinicalModels.kt:32-33`) joins notes into the instructions line, so the "Changed from ..." line will show wherever instructions or notes are rendered (card, summary, print: print exposure not verified). A second change appends a second line and the plan does not say which one the mark uses. The line has no link to the old row id.
- **M7. Draft recovery is deferred to "verify".** A recovered stop or replace draft must keep `baseMedicationRevision` and the stop context, or Save could issue a plain update. State what the draft slot persists; test recovery for add, stop and change.
- **M8. No measurement for the success claims** ("two taps", "does not reload"). Define pass criteria for the mock review and the CUJs.

## Nits

- L19 "up to five field-change events": cite the call site (`MedicationTapToAddSection.kt:64`).
- `PatientListScreen.kt:381` (`skipPartiallyExpanded = true`) and `PatientActionsModalSheet.kt:59` are two sheet configurations; state which the new sheet reuses (IME behavior differs).
- L125 "as agreed when Option C was chosen": the Options naming (A to H) is not defined in the document.
- L54 says the open questions were "raised in the Technical Plan" but they are not listed; move them into Decisions.
- CUJ 1: state scroll behavior when the removed row is above the viewport.
- CUJ 3 (L84) and L34 show different formats of the change line; make them agree with L126 and L238.
- Coverage requirement lists new branches only, not the deleted-route cleanup.
- No effort estimate and no rollout or fallback plan for a level-3 change.

## Verified against code
`AncState.kt:53, 357, 979, 1470-1496 (route at 1483), 1716`; `AncApp.kt:238`; `EditorHooks.kt:12-24`; `RecordsRepository.kt:353, 1453-1519`; `ClinicalCommands.kt:384-489, 493-495`; `MedsTab.kt:54-57`; `PatientHubHeader.kt:399`; `MedicationCard.kt:103`; `UndoCoordinator.kt:106-112, 179-213`. All match the design.
