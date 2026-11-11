# Design: registration flow state and driver (slice S5), revision 3

* **Author:** sherifrad
* **Date:** 2026-11-10
* **Status:** revision 3 adds four findings from agy Argon (age on a legacy patient in step 1, exit on the last step, no-op events, flow editors started outside the flow). Revised after independent review of revision 1 (`.agents/fleet-runs/s5_design_v1.md`; reports: EGM implementation readiness, agy Argon, Alphacode in `.agents/fleet-runs/s5_design_*`). Parent spec: `.agents/SPEC_REGISTRATION_FLOW.md`. Pure logic committed: `state/RegistrationFlow.kt`.

## Overview

The registration flow walks a patient through five editors: Identity, Dating, Obstetric history and risk, Histories, Previous pregnancies. S5 wires it into the app state: Next saves the current editor and opens the next, Skip and Back move without saving, Finish returns to the patient hub (`AncRoute.Workspace(patientId)`; this document calls it "the hub"). S5 adds no screens (S6 adds header, footer, hub button and the count prompt screen), no schema change, and changes no command, validation rule or storage.

Today each editor saves itself and `RecordCommitted` (`AncState.kt:1020`) clears drafts and sets the route to the hub. The flow changes only where the user goes after a save.

## Goals and Non-Goals

* **Goals:** every flow move is one atomic reducer transition (flow, route and editor draft change together or not at all); the flow advances only after a committed save for the editor that started it; unsaved edits are never dropped without the discard dialog; a stale flow cannot outlive its editors.
* **Non-goals:** UI (S6); removing the old intake-dating code (separate slice, listed as S5b in the spec); persisting the flow across restart (the patient shows as Incomplete and resumes from the hub); any change to commands or storage.

## Design

### Principles (answers to review blockers)
1. **Atomic transitions.** The reducer never changes the flow without also starting the target editor. A single pure function `startFlowEditor(state, flow, ids): AncState?` applies the flow change and runs the matching existing `start*Editor` logic; it returns null when that logic returns the state unchanged (saving, dirty draft, undo running, patient missing or archived). Only when it returns a state is the new flow committed. If null after a commit (for example the patient was archived), the flow is cleared and the route is the hub.
2. **Ids come from outside.** `AncReducer` stays pure. Every event that can start an editor carries the ids it needs, generated in the ViewModel with `UUID.randomUUID()`: `editorSessionId`, and `newPregnancyId` for the pregnancy step. No `IdSource` type.
3. **Advance in the commit transition.** The ViewModel does not dispatch a second event after commit. `RegistrationNext` stores `awaitingAdvance = AwaitingAdvance(fromSessionId, nextIds)` in the state. In the `RecordCommitted` branch, when the committed token's `editorSessionId == awaitingAdvance.fromSessionId`, the reducer applies the Next transition and `startFlowEditor` in the same pass, so the hub is never shown between editors. `awaitingAdvance` is cleared on `SaveFailed`, `PatientValidationFailed`, `ClinicalValidationFailed`, Back, Skip, `ConfirmDiscard`, a new `RegistrationNext`, and by the lifetime rule below.
4. **Flow lifetime is tied to the route and to the editor the flow started.** `RegistrationFlow` carries `activeEditorSessionId`, set by every flow-started editor. A post-pass in the reducer (next to `withDeleteUiConsistent`) clears `registrationFlow` and `awaitingAdvance` unless the route is a flow editor for `flow.patientId` (`PatientEditor`, `DatingEditor`, `OtherHistoriesEditor`, `PreviousPregnancyEditor`, or `PregnancyCountPrompt`) AND the open draft's `editorSessionId` equals `flow.activeEditorSessionId` (so a hub-started editor for the same patient, such as editing an existing pregnancy, ends the flow). System Back, `CancelEditor`, `OpenWorkspace` and any other route change therefore end the flow. `clearDrafts()` does not clear it.

### State and events
* `AncState`: new fields `registrationFlow: RegistrationFlow?` and `awaitingAdvance: AwaitingAdvance?`. New route `AncRoute.PregnancyCountPrompt(patientId)` for the count screen (S5 adds the route and reducer behaviour; S6 renders it; until S6 the `AncApp` mapping shows the hub placeholder).
* Events (all carry ids where needed):
  * `Flow(event: FlowEvent, editorSessionId, newPregnancyId)` for Begin, Back, Skip, Finish, PregnancyCountChosen.
  * `RegistrationNext(editorSessionId, newPregnancyId)`.
  * `PendingAction.FlowMove(event, editorSessionId, newPregnancyId)` so the discard dialog can apply a Back or Skip (pattern: `PendingAction.StartVisit`, `AncState.kt:59-63`). Flow moves never use `PendingAction.Navigate`.
* `state/RegistrationFlowDriver.kt` (pure): `startFlowEditor`, `editorEventFor(step, patient, ids)` (Identity and Obstetric both use `StartPatientEditor` with the existing patient id; Dating, Histories, Pregnancies use their `Start*`; Pregnancies returns null while `pregnancyTarget == null`, meaning the count prompt), and `pregnancyWalk` helpers.
* `RegistrationFlow.kt` changes: `Begin` seeds statuses from `deriveStepStatuses(patient)`; new `FlowEvent.PregnancyCommitted`; `Next` on the last step produces Finish; one pregnancy rule (below).

### Lifecycle
* **No-op events first.** For `Flow` events the reducer computes `reduce(flow, event)` before any dirty-draft check. If the result is `Continue` with an unchanged flow (Skip on Identity, Finish before the last step, `PregnancyCountChosen` outside step 5), the state is returned unchanged: no discard dialog, no editor restart, no new session id.
* **Exit is one path.** Every `FlowResult.Exit` (Skip or Next on the last step, `PregnancyCountChosen` with remaining 0, Finish) is handled the same way: `clearDrafts()`, flow cleared, route `Workspace(patientId)`, no notice. Skip on the last step therefore leaves for the hub; it does not strand the user on the pregnancy editor.
* **Age in the Identity step.** In the flow's Identity step age is required for every patient, including a legacy patient with no stored age: the ViewModel passes `forceAgeRequired = true` to `SavePatientProfileCommand.prepare` when `flow.current == IDENTITY` (new parameter, default false, `PatientProfileRules.ageRequired(isNew, storedAge)` becomes `isNew || forceAgeRequired || storedAge non-blank`). Outside the flow the S4 rule is unchanged. Without this a legacy patient could press Next with a blank age, commit, and stay on step 1 with no error.
* **Next needs a matching route.** `RegistrationNext` is ignored unless `state.route` is the editor for `flow.current` and the draft session equals `flow.activeEditorSessionId`.
* **Begin, new patient.** `startPatientEditor(patient = null)` sets `Begin(newId, IDENTITY)` only when `registrationFlow == null` and the draft was created (success path). Hub edit entry points pass an existing id and never start a flow.
* **Begin, existing patient.** Hub button (S6): `Flow(Begin(patientId, firstNotDone))`, statuses from `deriveStepStatuses`. If every step is done, no flow starts.
* **Next (`RegistrationNext`).** Guard in the reducer: ignored when `savePhase.blocksEditing()` or `pendingSave != null` (covers in-flight, reconciling, unresolved; a double tap changes nothing). Then:
  * Draft not dirty: no save. Apply `Next(hasData = deriveStepStatuses(patient)[step] == DONE)` and `startFlowEditor` immediately.
  * Draft dirty: the ViewModel runs the existing save for that editor (`savePatientProfile` for Identity and Obstetric, dating, histories, pregnancy save). The reducer has already stored `awaitingAdvance`. The ViewModel submits only if the reducer accepted the event (state check after dispatch). Advance happens in `RecordCommitted` as in principle 3. A rejected or failed save leaves flow and editor untouched.
  * On the last step the UI also sends `RegistrationNext`; Finish is applied inside the commit transition. The UI never sends `Flow(Finish)` mid-edit.
* **Skip.** Not on Identity. With a dirty draft: discard dialog with `FlowMove`; on confirm apply Skip and start the next editor.
* **Back.** One step back, statuses unchanged, with a dirty draft the discard dialog first. Back to Identity or Obstetric starts `StartPatientEditor` for the existing patient (route `PatientEditor(isNew = false)`). Back on step 1 ends the flow through the existing `requestBack` (new unsaved patient: patient list; saved patient: hub).
* **Finish.** Flow cleared, route `Workspace(patientId)`.
* **Identity and Obstetric** share one patient draft and revision. Each save is a whole-profile save through the existing `SubmitPatientProfile`. Identity, Obstetric, Back, Identity must not lose a field.
* **Previous pregnancies.** Entering the step shows `PregnancyCountPrompt` (default count `suggestedPregnancyCount`). `PregnancyCountChosen(n)` computes `remaining = max(n - existingCount, 0)` once and stores it as `pregnancyTarget`; `pregnancyIndex` starts at 0. `remaining == 0` finishes. Otherwise `StartPregnancyEditor(pregnancyId = null, newPregnancyId = fresh)` opens; each committed pregnancy save applies `PregnancyCommitted` (index + 1), and the flow finishes when `pregnancyIndex == pregnancyTarget`, otherwise opens the next pregnancy editor with a fresh id. Skip inside the walk leaves the whole step (marks it SKIPPED). Back inside the walk returns to the count prompt and keeps the index. Back from the count prompt to Histories, and entering Previous pregnancies from Histories, reset `pregnancyTarget = null` and `pregnancyIndex = 0` so the prompt always reappears. Next on an untouched pregnancy editor (draft not dirty) does not count as a pregnancy and keeps the editor open with a validation message (an empty pregnancy cannot be saved); the user chooses Skip to leave.

### Edge cases and decisions
1. **Lock.** The real wipe is `_state.value = AncState(...)` when the vault leaves Unlocked (`AncViewModel.kt:178-180`), so a fresh state has no flow. `clearedForLock` also clears the flow and `awaitingAdvance` because `clearFrozenCommands` calls it earlier (`AncViewModel.kt:919`). A commit result arriving after lock cannot advance a flow that no longer exists.
2. **App killed.** Memory only. `DraftRecovery` may restore a draft; the flow is not restored. The patient shows as Incomplete and resumes from the hub. Each flow step raises a recovery prompt if a draft exists for that editor session (`checkEditorSessionChanged`, `AncViewModel.kt:130-166`); Next is blocked while a recovery prompt shows. Pregnancy draft slots must not give pregnancy 2 the draft of pregnancy 1 (verify the slot key; oracle S5-8).
3. **Late results.** A commit whose token's `editorSessionId` differs from `awaitingAdvance.fromSessionId` never advances.
4. **Frozen commands.** `clearsFrozenPatient` and `clearsFrozenClinical` key on `Start*` event types; flow starts go through the same `start*Editor` logic and always use fresh session ids, so a frozen command from a discarded step cannot be reused. Invariant stated here and asserted in a test.
5. **Intake dating.** `intakeDatingUpdate` in `savePatientProfile` stays untouched. It returns null when `lmpDate` is blank or equals the stored dating (`IntakeDatingRule.kt:32`, `AncState.kt:1792`). The profile commit may issue a second `controller.execute` for dating (`AncViewModel.kt:477-497`); `RecordCommitted` is dispatched after both (`:500-510`), so advancing on it is safe. Removal of this path is slice S5b. Superseded by S5b (DESIGN_S6_REGISTRATION_UI.md Part 1): the path is deleted.
6. **Archived or missing patient mid-flow.** `startFlowEditor` returns null; flow cleared, route hub.
7. **System Back gesture.** It dispatches `AncEvent.Back`, which goes through `requestBack` (discard dialog when dirty). Verify the Activity back handler does the same (oracle S5-6 on the reducer; the Activity wiring is verified in S6).

## Alternatives Weighed
* A single pager hosting all editors: not chosen on 2026-11-10 by the owner for now (large refactor); sequential screens with a shared header chosen.
* Storing the flow in the database: rejected; needs a schema change and "incomplete" derives from stored data.
* Advancing from the ViewModel after commit (revision 1): rejected after review; it flashes the hub, races with `RecordCommitted`, and cannot be unit tested.

## Spec deviation
`.agents/SPEC_REGISTRATION_FLOW.md` said one field and one `AncEvent.Flow` line. This design adds two state fields, two events, one route, one pending action, a post-pass and edits in `startPatientEditor`, `clearedForLock`, `confirmDiscard` and `RecordCommitted`. All new logic is in new files and is called from those sites.

## Oracles (pure JVM, reducer level, names S5-n; they replace RF10-RF15 of revision 1; the spec's RF11-RF15 stay with S6)
* S5-1: `startPatientEditor(null)` starts a flow at IDENTITY with the new patient id only when none is active; hub edit entry points start none; a fresh `AncState` has no flow; `clearedForLock` clears flow and awaitingAdvance. (Spec clause kept: a profile save with an empty `lmpDate` issues no dating command, already covered by `IntakeDatingRule` tests; keep them.) Superseded by S5b (DESIGN_S6_REGISTRATION_UI.md Part 1): those tests are deleted.
* S5-2: `RegistrationNext` with a dirty draft stores `awaitingAdvance`; `RecordCommitted` with the matching token applies Next and starts the next editor in one transition (route is never the hub in between: assert the single resulting state); a different token, `SaveFailed`, validation failure, ambiguous or unresolved phase leave flow and editor unchanged.
* S5-3: double `RegistrationNext` while `pendingSave != null` or `savePhase` is RECONCILING or UNRESOLVED changes nothing.
* S5-4: clean-draft Next applies `Next(hasData)` from `deriveStepStatuses` without a save: DONE when data exists, SKIPPED otherwise; Identity with no data stays.
* S5-5: Back and Skip with a dirty draft set the discard dialog and `FlowMove`; `ConfirmDiscard` applies it and the target editor has a draft; clean draft moves directly; `awaitingAdvance` is cleared.
* S5-6: lifetime: system Back, `CancelEditor`, `OpenWorkspace` and any route outside the flow editors leave `registrationFlow == null`; the flow never survives onto an unrelated editor or another patient.
* S5-7: `editorEventFor` returns the right `Start*` per step with caller-provided ids; Obstetric reuses `StartPatientEditor`; Pregnancies returns null while `pregnancyTarget == null`; archived patient mid-flow clears the flow and lands on the hub.
* S5-8: pregnancy walk: remaining 0 finishes; target 3 with 1 existing walks 2 editors; the index increments only on `PregnancyCommitted`; Skip leaves the step; Back returns to the count prompt; pregnancy 2 does not inherit pregnancy 1's draft.
* S5-10: a legacy patient with no stored age in the Identity step is rejected with AGE_REQUIRED on Next; outside the flow the same patient still saves without age (S4 rule).
* S5-11: Skip on Identity and Finish before the last step are no-ops even with a dirty draft (no dialog, session id unchanged); Skip and Next on the last step exit to the hub with drafts cleared.
* S5-12: a pregnancy editor opened from the hub for the same patient while a flow is active ends the flow; `RegistrationNext` with a route that does not match `flow.current` is ignored.
* S5-9: Identity, Obstetric, Back, Identity keeps every profile field; Finish sets the route to the hub and clears flow.

## Detailed Implementation Plan

Files (paths under `app/src/main/java/com/sherifradwan/ancnative/` unless noted; C = create, M = modify):
1. C `state/RegistrationFlowDriver.kt`: `startFlowEditor`, `editorEventFor`, pregnancy walk helpers, `AwaitingAdvance`. Test: C `app/src/test/java/com/sherifradwan/ancnative/state/RegistrationFlowDriverTest.kt` (S5-7, S5-8 helpers).
2. M `state/RegistrationFlow.kt`: add `activeEditorSessionId`, seed statuses in `Begin` (and refuse `Begin` for a missing or archived patient), `PregnancyCommitted`, last-step Next finishes, remaining-count rule in `PregnancyCountChosen`; update `RegistrationFlowTest.kt` and `RegistrationFlowDeriveTest.kt`. Checkpoint: builds, new events unused.
3. M `state/AncState.kt`: two fields, route `PregnancyCountPrompt`, events, `PendingAction.FlowMove`, post-pass lifetime rule, branches in `startPatientEditor`, `RecordCommitted`, `confirmDiscard`, `clearedForLock`; all logic delegated to the driver. Tests: C `state/RegistrationFlowStateTest.kt` (S5-1 to S5-6, S5-9).
4. M `ui/AncApp.kt`: map `PregnancyCountPrompt` to the hub placeholder only (S6 renders it) so `when` stays exhaustive.
5. M `storage/PatientCommands.kt` and `model/PatientProfileRules.kt`: the `forceAgeRequired` parameter (default false) and its tests; M `state/AncViewModel.kt`: dispatch branch for `RegistrationNext` that runs the existing save for the current editor only when the reducer accepted the event; generate ids; no other change to save functions.
6. Static checks: `rg` found no `.agents/checks` script pinning the `AncRoute` or `AncEvent` sets except what the gate exposes; if the run shows one, update it minimally and say which. Existing pinned name `patientEditorErrorResource` (check_r3z) is not touched.
7. Gate: `.agents/scripts/run_build_gate.sh` and `run_static_checks.sh` after steps 2, 3 and 5.
8. Review (AGENTS.md level-3 rule: frontier-tier review plus independent test review): Opus (code and tests), agy Argon and Alphacode (read-only), plus the EGM implementation-readiness lenses on the diff.
