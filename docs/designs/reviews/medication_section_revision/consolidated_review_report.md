# Consolidated Review Report: Medication Section Revision (Stage 1)

**Target:** `docs/designs/medication_section_revision.md` (372 lines; feature range 1-214, implementation range 215-372)
**Date:** 2026-11-10
**Mode:** Consolidated (all five stages, no stops). Remediation was not run.
**Raw reports:** the stage files in this folder.

## Reviewer status

| Stage | Reviewer | Lane (model) | Code access | Status |
| --- | --- | --- | --- | --- |
| 5 | `egm-zoom-out` | AlphaCode CLI, `auto` provider (underlying model not shown) | none (document pasted) | completed on 2nd run (1st run returned tool-call markup, 427 bytes) |
| 5 | `egm-inventive-catalyst` | Gemini 3.8 Flash via `agy`, read-only | yes | completed; was given lines 1-351 of 372 |
| 6 | `egm-comprehension` | GLM 5.3 Flash (opencode free) | none | completed |
| 7 | `egm-design` | Claude Sonnet 5.5 subagent | yes | completed |
| 7 | `egm-structural` | GLM 5.3 Flash (opencode free) | none | completed |
| 7 | `egm-style` | AlphaCode CLI, `auto` provider | none | completed |
| 8 | `egm-zoom-out` | Gemini 3.8 Flash via `agy` | yes | completed |
| 9 | `egm-implementation-readiness` | Claude Sonnet 5.5 subagent | yes | completed |
| 9 | `egm-codebase-details` | Gemini 3.8 Flash via `agy` | yes | completed |
| 9 | `egm-style` | GLM 5.3 Flash (opencode free) | none | completed |

Argon was not used (quota exhausted, resets in about 98 hours); Gemini 3.8 Flash took its place. Reviewers without code access marked code claims as "not verified". Reviews were read-only; no design or source file was changed by a reviewer.

## Overall result

Two reviewers (Stage 7 design, Stage 9 implementation readiness) say the design is not ready to implement. The architecture direction (sheet instead of route swap) was not challenged. The gaps are in the details of the new commands, the undo, the build order, and places the "one shared active check" does not reach. Two Gemini/AlphaCode zoom-out reviews also push back on the scope the author chose (see Contested).

## Blockers

1. **The build order breaks the build.** Phase 2 removes the two-argument `isOngoingMedication` and changes `medicationStatusLabel`, and phase 3 deletes `AncRoute.MedicationEditor`, while callers and `AncApp.kt:238` still use them until phase 5. The sheet does not exist until phase 4. Fix: re-cut the phases so every one compiles, with a verification command per phase (add new helpers beside the old ones, migrate all callers, then remove; add the sheet; then flip route and delete the editor with the test rewrites in one step).
2. **The undo for stop and change cannot be built as written.**
   - `UndoOffer` carries `DeletableType`, `UndoAction` has only `DELETE` and `SAVE`, and the notice comes from `DeletableRegistry`. There is no notice for "stop undone" or "change undone", and the open-edit refusal is keyed on one record id while a change touches two.
   - `offerMedicationChangeUndo(patientId, receipt, command)` cannot build the undo command: neither the command nor the receipt holds the old row's earlier status, earlier end date or content hash. The plan must capture these before the save.
   - `ClinicalValidationFailed` (`AncState.kt:972-990`) ends in `error("Unhandled non-deletable clinical record kind")`; the new `MEDICATION_UNDO_CHANGE` kind would crash it.
   - The plan does not say which id the undo command's receipt `recordId` is, how `withPatientRevision` works for a command with several row revisions, or what a second quick action does to a pending undo (the coordinator holds one `pendingUndo`).
   - `DeleteUndoUi.kt`, `UndoAction`, `UndoOffer` and the `AncState.kt` edits are missing from the file list.
3. **Retry of the replace command can write twice.** `AncViewModel.saveMedication` freezes and reuses only a `SaveMedicationCommand`. For a `ReplaceMedicationCommand` the cast returns null, so Save after an ambiguous result creates a new operation id and a new row id. If the first attempt committed, there are two replacements. The plan must freeze and reuse the replace command including the new row id, choose `recordKind` for the replace path, and test it.
4. **"All screens agree on active" is not true as planned.** `medicationStatusLabel` is also used in `ClinicalSummaryGenerator.kt:94` and `PatientPrintHtmlGenerator.kt:229` (clinical output), neither listed. `medicationEditorChoice` and `MedicationEditor.kt:341` read the stored status, so a course-ended drug shows as stopped in the list but opens as "On" in the sheet, and saving without touching the status writes it back as Active. `PatientHubHeader.kt:407` uses `isNonCompliant` for the badge independent of the end date. The plan also does not say whether the change rule uses stored or derived status.
5. **The replace command is incomplete.** `isStructurallyValid()` takes no arguments, so "stop date not before old start date" needs `oldStartDate` in the command. The full field list, the fingerprint format, whether the new row inherits `isProphylaxis`, `isNonCompliant` and adverse notes, and what happens when name or notes change together with dose are not stated. A stop through plain `SaveMedicationCommand` has no end-before-start check.
6. **The Alternatives section does not meet the standard.** Rejections say "Rejected by author" and "the author chose Option C", which is circular; the technical reason sits in the Cons. Alternative E gives only the frozen schema, which is a policy. Alternative H is a staging note, and Alternative C has no pros and cons. Missing alternatives: a non-modal inline expanding row; a dose change as a status edit plus a new row using two existing commands; undo of a change by reusing the existing delete and restore commands.

## Major

7. **Repository change.** `dao.updateMedication` overwrites every column, so "stop the old row" needs a read inside the transaction or a new partial-update query; `RecordsDao.kt` is wrongly marked "none expected". Undo must also delete the new row (`deleteMedication` exists). Rollback testing needs fault injection and belongs in `androidTest` with Room, next to `ClinicalSaveAtomicityTest.kt`.
8. **Failure messages.** `SaveUiError` has 8 values, `toUiError()` merges several failures, and `RECORD_NOT_FOUND` has no UI value, so "This medication was removed" is not possible without changing `AncState.kt` and the mapper. When both patient and medication revisions are stale the message cannot say what to fix.
9. **Draft semantics.** `isDirty` is `values != original`, so a catalog-prefilled or stop draft with `original == values` is never dirty: no discard prompt, no recovery, and "compare with `original`" cannot detect a change on a stop. Decide `original` for add, stop and edit. Recovery must keep the stop or change context. `DraftMapping.kt` and `DraftRecovery.kt` hold no route reference, so item 9 can become a decision plus a test.
10. **Course length and clock.** Whether 3 days means start+3 or start+2 inclusive, the valid custom range, blank or invalid `endDate`, the `today` type and clock source (device clock; no midnight refresh; `PatientHubScreen` already has `asOfDate`), and what editing course length on an existing row does are all open. Midnight and clock-skew tests are missing.
11. **Silent drops remain.** `startMedicationEditor` still ignores a start while a save runs or any of 9 other drafts is dirty. The plan disables only picker tiles, not row stop symbols or the Add row. "Save and add another" closes and reopens the sheet through `clearDrafts()` (flicker, lost focus, keyboard dismissed), which touches the Enter-to-next goal. After Save the sheet closes while the undo bar shows, and nothing serializes the next action.
12. **Notes marker side effects.** `medicationInstructions(dosage, route, notes)` joins notes, so "Changed from ..." appears wherever notes print; print exposure was not checked. A second change appends a second line and the plan does not say which one the mark uses. The line has no link to the old row.
13. **Gaps in the file list.** `PatientHubScreen.kt` must pass `medicationDraft` and `savePhase` to `MedsTab`; `EditorField` is at `Components.kt:392` and is `internal` (not 403); custom-choice tiles have no frequency; the "Prescribe" button (`MedsTab.kt:93-114`, tag `addCustomMedicationBtn`) is not mentioned; `MedicationStatusDraftTest` and `UndoCoordinatorTest` need rewrites; test paths use `...` and must be exact.
14. **Test plan.** Missing: archived patient, exhausted revision, id collision, cross-patient id, `DUPLICATE_OPERATION_MISMATCH`, frozen retry after an ambiguous result, reducer tests for the new undo offer, recovery on the Meds tab without navigation, course-end boundary, stop symbol hidden for ended courses, "Changed" mark and header count assertions. The coverage requirement omits `UndoMedicationChangeCommand` and `undoMedicationChange`.
15. **Undefined or confusing terms (Stage 6).** "route" means both the navigation route and the drug route; "Stopped" and `Discontinued` are never tied together; the date chip edits the start date on add and the end date on stop; what happens when the doctor switches tab while the sheet is open and where a recovered draft reopens; save-machinery terms (revision, operation id, fingerprint, receipt, content hash) are never defined.
16. **Structure (Stage 7).** Risks sit only as subsection 10 and several have no mitigation ("accepted by the author" is not one); promote to a `## Risks & Mitigations` section.
17. **Verification gate (Stage 9 style).** Claims about existing code are stated as fact with line numbers that will shift; use names, not line numbers. The review-team sentence in the Verification Plan is an operations note, not a verification step.

## Contested: reviewers challenge decisions the author already made

The author decided these; the reviewers disagree. They are listed so the author can confirm or change them.

- **Course length, the "Changed" line in notes, the two-row replace command, `FormFocusChain` and the date chip** are called scope creep or fragile (Stage 5 and Stage 8 zoom-out). The Stage 8 reviewer recommends: Stage 1 = delete the route and editor, add the sheet and shared form, collapse the picker; dose and frequency edit in place (Alternative D); no "Changed" mark; no course length; plain Compose focus handling. That would keep the level-3 changes to `AncState.kt` and `EditorHooks.kt`.
  - Counter-point: in-place edits lose the dose history the author asked to keep, and the author asked for undo and course length.
- **Two taps for add and stop** (inventive reviewer): suggests a one-tap stop with optimistic update and the existing undo bar. This is the Option B the author rejected.
- **Notes text as the change record** (inventive reviewer): suggests reading the change from `clinical_command_receipts`. Not verified; a code comment near `deleteRecord` in `RecordsRepository.kt` says receipts store a fingerprint, not content, and appear to be kept for a limited window, so a mark built from them may not last.
- **Auto-complete expired courses** (inventive reviewer, Nit): would write to the database when a file opens; the author chose to evaluate only when the list is drawn.

## Nits

Escaped quotes (`\"`) in the document; CUJ 3 and the Goals show different formats of the "Changed from" line; "Option C" naming is not defined in the document; L54 refers to questions that are not listed; "a little JavaScript" is vague; wording such as "frontier-tier review" and "the author was offered X and chose Y" can be plainer; no effort estimate or rollout and fallback plan for a level-3 change; Items 12 and 13 give no API names; `docs/mocks/` and how mock approval is recorded are not stated; Item 11 does not say what else in `MedicationEditor.kt` survives; strings to add and remove are not listed.

## Traceability

| Consolidated item | Raw findings |
| --- | --- |
| 1 | Stage 9 impl-readiness B1; Stage 9 codebase-details 3 |
| 2 | Stage 9 impl-readiness B2, M2; codebase-details 2, 6; Stage 7 design M1 |
| 3 | Stage 7 design B3; Stage 9 impl-readiness M3 |
| 4 | Stage 7 design B2; Stage 9 impl-readiness M6 |
| 5 | Stage 9 codebase-details 1; impl-readiness M5 |
| 6 | Stage 7 design B1; Stage 7 structural 2 |
| 7 | Stage 9 impl-readiness M1, M7; codebase-details 4 |
| 8 | Stage 7 design M2; Stage 9 impl-readiness M3; codebase-details 5 |
| 9 | Stage 9 impl-readiness M4; Stage 7 design M7 |
| 10 | Stage 7 design M3; Stage 9 impl-readiness M5; Stage 6 finding 5 |
| 11 | Stage 7 design M4, M5 |
| 12 | Stage 7 design M6 |
| 13 | Stage 9 impl-readiness M6; codebase-details 7 |
| 14 | Stage 9 impl-readiness M7; Stage 9 style 1 |
| 15 | Stage 6 findings 1-6 |
| 16 | Stage 7 structural 1 |
| 17 | Stage 9 style 2; Stage 7 style 13 |
| Contested | Stage 5 zoom-out 1-5; Stage 8 zoom-out 1-5; Stage 5 inventive 1-3 |
