# Design: registration flow state and driver (slice S5)

* **Author:** sherifrad
* **Date:** 2026-11-10
* **Status:** draft for review. Parent spec: `.agents/SPEC_REGISTRATION_FLOW.md`. Pure logic already committed: `state/RegistrationFlow.kt` (steps, statuses, reduce, derive, incomplete rule).

## Overview

The registration flow walks a patient through five editors: Identity, Dating, Obstetric history and risk, Histories, Previous pregnancies. S5 wires the flow into the app state so that pressing Next saves the current editor and opens the next one, Skip and Back move without saving, and Finish returns to the patient hub. S5 adds no screens (S6 adds the header, footer and hub button) and no schema change.

Today each editor saves itself and, on success, the reducer sends the route to the patient hub (`RecordCommitted`, `AncState.kt:1020`). The flow must change only where the user goes after a save, never what is saved.

## Goals and Non-Goals

* **Goals:** one new field in `AncState`; Next, Skip, Back and Finish behave as in the spec; a flow move never loses unsaved edits without a confirmation; no advance on a failed, ambiguous or in-flight save; the flow ends cleanly on lock, on Back from step 1, and on Finish.
* **Non-goals:** UI (S6); removing the old intake-dating path (separate slice S5b); persisting the flow across an app restart (the patient shows as Incomplete and the hub button resumes it); any change to a command, validation rule or storage.

## Design

### State
* `AncState.registrationFlow: RegistrationFlow?` (type already exists). One field plus a few lines in the reducer; `AncState.kt` is over the 1,500-line ceiling, so all new logic lives in new files.
* New file `state/RegistrationFlowDriver.kt` (pure, no Android): `fun editorStartFor(flow: RegistrationFlow, patient: DemoPatient?, ids: IdSource): AncEvent?` maps a step to `StartPatientEditor` (Identity and Obstetric), `StartDatingEditor`, `StartHistoriesEditor`, or `StartPregnancyEditor(pregnancyId = null, newPregnancyId = fresh)`; each with a fresh `editorSessionId`. Also the rule `flowAfterCommit(flow, step)`.
* New events: `AncEvent.Flow(FlowEvent)` for Begin, Back, Skip, Finish, PregnancyCountChosen; `AncEvent.RegistrationNext` meaning "save this editor, then advance".
* New `PendingAction.FlowMove(FlowEvent)` so a Back or Skip with a dirty draft shows the existing discard dialog and, on `ConfirmDiscard`, applies the move. Flow moves never use `PendingAction.Navigate`, because Navigate sets a route without creating the target draft and the editor renders blank.

### Lifecycle
* **Begin.** A new patient: `startPatientEditor(patient = null)` also sets `registrationFlow = Begin(newPatientId, IDENTITY)`. An existing patient: the hub's "Continue registration" button (S6) sends `Flow(Begin(patientId, firstNotDone))`, where `firstNotDone` comes from `deriveStepStatuses`.
* **Clear.** `clearDrafts()` must NOT clear the flow, because the flow outlives each editor. The flow is cleared on Finish, on Back from step 1, and in `clearedForLock`.
* **Next.** The ViewModel runs the existing `Submit*` for the current editor. The flow advances only when `RecordCommitted` arrives for the matching `PendingSaveToken`. Then the status becomes DONE (or SKIPPED when the step had no data, via `Next(hasData)`), and the driver dispatches the next `Start*` event. A rejected validation, a failed save, an ambiguous or unresolved save, or an in-flight save (`savePhase.blocksEditing()`) leaves the flow where it is and shows the existing error.
* **Skip.** Not offered on Identity. If the draft is dirty, show the discard dialog first. Marks SKIPPED and starts the next editor.
* **Back.** Moves one step back without changing statuses; with a dirty draft, the discard dialog first. On step 1, Back leaves the flow through the existing `requestBack` (new unsaved patient: patient list; saved patient: hub).
* **Finish.** After the last step, clear the flow and set the route to `Workspace(patientId)`.
* **Previous pregnancies.** The first screen is a count prompt held in flow state (`pregnancyTarget`, default `suggestedPregnancyCount`). Choosing 0 finishes. Otherwise it opens `StartPregnancyEditor` once per pregnancy; each successful save increments `pregnancyIndex`; when `pregnancyIndex == pregnancyTarget` the flow finishes. Pregnancies that already exist reduce the number walked to `max(target - existing, 0)`.
* **After the first save of a new patient.** The patient id is the route's `newPatientId`; the flow carries the same id, so the next `Start*` finds the committed patient and takes its fresh revision.

### Edge cases the driver must handle
1. App locked during the flow: flow and drafts are cleared; nothing is saved that was not committed.
2. App killed: the flow is in memory only. A draft may be restored by `DraftRecovery`; the flow is not. The patient is listed as Incomplete and can resume.
3. Double tap on Next: the second tap is ignored while `pendingSave != null`.
4. Next on a step whose editor has no changes: allowed; advances with DONE if data exists, otherwise SKIPPED.
5. A commit for a different token (late result of an earlier editor) must not advance the flow.
6. The existing `hasDirtyDraft` rule blocks every `Start*`; the driver must start the next editor only after `RecordCommitted` has cleared the drafts.
7. The old intake-dating code in `AncViewModel.savePatientProfile` (`intakeDatingUpdate`) stays untouched in S5. Because LMP is no longer on the patient form it normally returns null.

## Alternatives Weighed
* A single pager that hosts all editors: rejected by the owner for now (much refactoring); sequential screens with a shared header chosen instead.
* Storing the flow in the database: rejected (needs a schema change and the "incomplete" rule can be derived from stored data).
* Driving the flow from the UI composables: rejected; the reducer and ViewModel own transitions so they are unit-testable.

## Oracles (all pure JVM tests on the reducer and driver)
* ORACLE-RF10: `startPatientEditor(null)` sets a flow at IDENTITY with the new patient id; `clearDrafts` and `RecordCommitted` leave it intact; `clearedForLock` clears it.
* ORACLE-RF11: Next advances only when `RecordCommitted` carries the pending token; a different token, a validation failure, an ambiguous or unresolved save leaves the flow unchanged.
* ORACLE-RF12: Back or Skip with a dirty draft sets the discard dialog and `PendingAction.FlowMove`; `ConfirmDiscard` applies it and starts the target editor with a draft; with a clean draft it moves directly.
* ORACLE-RF13: `editorStartFor` returns the right `Start*` event per step with a fresh session id each call; the Obstetric step reuses `StartPatientEditor`.
* ORACLE-RF14: pregnancy walk: target 0 finishes; target 3 with 1 existing opens 2 editors then finishes; the index increments only on a committed pregnancy save.
* ORACLE-RF15: Finish sets the route to the hub and clears the flow; Back on step 1 clears the flow and uses the existing back destination.

## Detailed Implementation Plan

1. `state/RegistrationFlowDriver.kt` with `editorStartFor`, `flowAfterCommit` and the pregnancy walk helpers, plus `RegistrationFlowDriverTest` (RF13, RF14).
2. `AncState`: add `registrationFlow`, `AncEvent.Flow`, `AncEvent.RegistrationNext`, `PendingAction.FlowMove`; in `startPatientEditor` set the flow for a new patient; keep `clearDrafts` unchanged; clear the flow in `clearedForLock`; handle `ConfirmDiscard` for `FlowMove`. A handful of lines, delegating to the new file. Tests: RF10, RF12, RF15 in a new `RegistrationFlowStateTest`.
3. `AncViewModel`: on `RegistrationNext`, run the existing `Submit*` for the current editor and remember the token; on `RecordCommitted` with that token, dispatch the flow advance and the next `Start*` event. Do not add logic to the existing save functions beyond remembering the token. Tests: RF11 through a fake controller if the ViewModel is testable; otherwise through the reducer with explicit events.
4. Run `.agents/scripts/run_build_gate.sh` and the static checks; any check that pins the route set or `AncEvent` list is updated minimally.
5. Level-3 review of `AncState.kt` and `AncViewModel.kt` changes by two reviewers from different vendors.
