VERDICT: FAIL

The port adds several genuine defects, two of them serious and clinically reachable. The three-event guarantees also cannot be checked from this diff, because the handlers `reduceFlowEvent` and `applyFlowMove` and the helpers `withFlowConsistent` / `withFlowSessionFollowing` are referenced but not defined in it; see coverage limits at the end.

## Findings

### 1. Serious — startPatientEditor.kt (ClinicalDraftReducers.kt), diff lines 122 and 318: editing an existing patient silently overwrites the stored LMP

**State and event:** The clinician opens an existing patient. The state holds `patientDraft.baseRevision = 5`, `patientDraft.original.lmpDate = "2026-08-01"`, `patientDraft.values.lmpDate = "2026-08-01"`, and `datingDraft == null` (the normal case, with the Dating section never opened). The Dating section is not part of the intake form, so `values.lmpDate` stays at the `toPatientDraftValues()` seed for the whole edit. The clinician changes the gravida and taps Save.

**What goes wrong:** `SavePatientProfileCommand.prepare` (PatientCommands.kt, diff lines 339–343) never populates `input.lmpDate` and never decides whether a dating update is needed. The `datingDraft == null` branch in the ViewModel (diff lines 288–289) therefore leaves `notice = command.savedNotice()`, which is the plain profile-saved notice. The undo command is restored with `dateOfBirth = stored.dob`, and the dating update that used to be computed in the commit handler is gone. If a caller on this branch ever calls `SavePatientProfileCommand.replacementFor(stored)`, the structural validator now constructs `PatientClinicalInput` with `ageRequired = PatientProfileRules.ageRequired(expectedPatientRevision == null, null)` (diff line 387). That always evaluates to `true`, so an unchanged patient whose `ageText` is empty is rejected as `SAVE_FAILED`.

**Wrong result:** Either the LMP is lost because the dating path was removed without an equivalent replacement, or the profile save is rejected outright. The `datingDraft == null` notice is a direct observation in the diff; the intent of `replacementFor` is inferred from its pre-port role and must be confirmed in `PatientCommands.kt`.

### 2. Serious — PatientCommands.kt, diff lines 387 and 395: the structural validators now reject rows they used to accept

**State and event:** A committed `SavePreviousPregnancyCommand` with `expectedPregnancyRevision = 3` is re-validated. A `SavePatientProfileCommand.replacementFor(stored)` is built for a stored patient with `revision = 5` and an empty `ageText`, whose edit predates this port and did not supply `ageRequired`. Both are normal post-save flows: undo, replay after reconciliation, and revision checks.

**What goes wrong:** `isStructurallyValid()` for the patient branch passes `ageRequired = PatientProfileRules.ageRequired(expectedPatientRevision == null, null)`. That expression is `true` whenever a revision exists. The pregnancy branch passes `storedRow = expectedPregnancyRevision != null`, but the property was added to `PatientClinicalInput`/`PreviousPregnancyClinicalInput` without a default. The `prepare` path always supplies it; the stored-row path now does not match the stored record's actual provenance.

**Wrong result:** `replacementFor(stored)` returns `SAVE_FAILED` for any stored patient with an empty `ageText` and a non-null revision, and the pregnancy structural check disagrees with the row it is validating. This breaks the undo/replay path that the port's own `frozenPatientCommand = null` relies on, and the inconsistency is not caught by any test in the diff.

### 3. Serious — ClinicalDraftReducers.kt, diff lines 162 and 372: delivery values are dropped from the pregnancy draft

**State and event:** The pregnancy draft holds `values.outcome = "LiveBirth"`, `values.deliveryBy = "SVD"`, `values.birthMethod = "SVD"`, and `values.outcomeText = ""`. The clinician taps the already-selected LiveBirth chip. `PregnancyDraftChanged(OUTCOME, "LiveBirth")` is dispatched.

**What goes wrong:** The `OUTCOME` branch runs `deliveryColumns(...)`, then `outcomeText = if (event.value.isNotBlank()) "" else draft.values.outcomeText`. For a re-tap of the same value, the text is blanked. The `OUTCOME_TEXT` case was deleted from `updatePregnancyDraft` (diff line 145), and `BIRTH_METHOD` was folded into `deliveryColumns`, which takes `(outcome, original.outcome, deliveryBy, birthMethod, value)`. The `DELIVERY_BY` case passes `draft.values.outcome` as the first argument and `draft.values.birthMethod` as the last, but the `OUTCOME` case passes `originalDeliveryBy`/`originalBirthMethod` from `draft.original`. The two cases can therefore compute different `deliveryBy`/`birthMethod` for the same draft.

**Wrong result:** Re-tapping the selected outcome blanks a clinician-typed `outcomeText` (for example "Twin live birth, SVD"). A later save then persists `outcomeText = ""`, and the `OUTCOME_TEXT` field can no longer be edited because its reducer case is gone. Delivery values are dropped or overwritten on a path the clinician can reach with one tap.

### 4. Moderate — ClinicalDraftReducers.kt, diff lines 125–137 and 180–185: the LMP/date fields are no longer editable from the patient editor

**State and event:** The patient editor holds `values.lmpDate = "2026-08-01"`. The clinician edits the LMP chip. `PatientDraftChanged(LMP_DATE, "2026-07-25")` is dispatched.

**What goes wrong:** The `LMP_DATE`, `DATING_METHOD`, `TRANSFER_DATE`, `EMBRYO_AGE_DAYS`, `AGREED_EDD`, `SCAN_DATE`, `SCAN_GA_WEEKS`, and `SCAN_GA_DAYS` cases were deleted from `updatePatientDraft` (diff lines 130–137). The `when` over `PatientDraftField` is now non-exhaustive, so the event falls through to the unchanged `values`. The `toPatientDraftValues()` seed no longer carries `lmpDate` (diff line 184), so the value the editor displays is the one `startPatientEditor` seeded, not the stored dating.

**Wrong result:** The LMP edit is a no-op in the reducer. `SavePatientProfileCommand.prepare` builds `input` without `lmpDate`, and the commit handler's `datingDraft == null` branch never computes an `intakeDatingUpdate`. The dating the clinician entered is silently dropped, and the profile save reports success.

### 5. Moderate — AncViewModel.kt, diff lines 232–243: `registrationNext` dispatches the save outside `updateState` and can save the wrong draft

**State and event:** `awaitingAdvance = AwaitingAdvance(editorSessionId = "E1", ids = FlowIds("E1", "P1"))`. The clinician taps Next on the Dating editor. `AncEvent.RegistrationNext("E1", "P1")` is dispatched.

**What goes wrong:** `registrationNext` calls `updateState { AncReducer.reduce(it, event) }`, then reads `after = _state.value`. `after.awaitingAdvance` is `AwaitingAdvance(activeEditorSessionId(after), FlowIds(event.editorSessionId, event.newPregnancyId))`. The guard `waiting != AwaitingAdvance(...)` compares the full data class, including `editorSessionId`, so a Next that was already consumed by a prior `Flow` (the reducer recorded `awaitingAdvance` for the same ids) still passes. The `when` then picks the first non-null draft: `patientDraft` before `datingDraft`. A stale `patientDraft` from a previous editor session that was not cleared by `clearDrafts()` causes `savePatientProfile()` to run against the wrong patient.

**Wrong result:** The save is performed outside the `updateState` block, so it is not serialized with the reducer's state mutation. A concurrent `Flow`/`RegistrationNext` pair can interleave and the profile of the wrong patient is written. The `awaitingAdvance` guard is also weaker than the stated invariant ("`RecordCommitted` advances a waiting flow only for the save it waited for"), because it matches on ids and session, not on the command that was actually saved.

### 6. Moderate — AncReducer.kt, diff lines 73, 82, 89–93: the discard path clears `awaitingAdvance` before the `FlowMove` is applied, so the move is lost

**State and event:** `showDiscardDialog = true`, `pendingAction = PendingAction.FlowMove(event, editorSessionId = "E1", newPregnancyId = "P1")`, `awaitingAdvance = AwaitingAdvance("E1", FlowIds("E1", "P1"))`, `hasDirtyDraft = true`. The clinician confirms discard.

**What goes wrong:** `confirmDiscard` builds `discarded = state.clearDrafts().copy(..., awaitingAdvance = null)` and then calls `applyFlowMove(discarded, action.event, FlowIds(...))`. `applyFlowMove` is not in the diff, so whether it re-records `awaitingAdvance` cannot be confirmed. The `requestBack` path (diff line 73) sets `awaitingAdvance = null` when the destination is dirty, and `confirmDiscard` (diff line 82) does the same before the move. If `applyFlowMove` only moves the route and relies on `awaitingAdvance` to trigger the save, the save never runs.

**Wrong result:** After discard, the flow advances the route but no `RecordCommitted`/`awaitingAdvance` remains, so the new patient's profile is never saved and the clinician sees an empty editor. The guarantee "`requestBack` and `confirmDiscard` clear `awaitingAdvance`; a pending `FlowMove` is applied after discard" is only half-satisfied: the clear happens, the apply is unverified.

### 7. Minor — AncViewModel.kt, diff lines 209–212: the draft-recovery guard can reject a legitimate recovery

**State and event:** A draft record exists for `(slot = PATIENT, afterSession = "E1")`. The clinician reopens the editor and `activeEditorSessionId(stateAfterLoad) = "E2"`, `activeDraftSlot = PATIENT`. The record's `editorSessionId` is `"E1"`.

**What goes wrong:** The new condition `draftRecordBelongsToOpenEditor(stateAfterLoad, record)` is evaluated after `currentSessionId == afterSession` and `currentSlot == slot`. If `draftRecordBelongsToOpenEditor` compares the record's session to `activeEditorSessionId(stateAfterLoad)` and the record's slot to `activeDraftSlot`, it is true here. If it compares the record's `editorSessionId` to `currentSessionId`, it is false and the prompt is dropped. The helper is not in the diff, so the direction of the comparison is unverified.

**Wrong result:** A draft saved under `"E1"` and reopened under `"E2"` either recovers correctly or is silently dropped, depending on a helper that the diff does not show. The guard's placement after the equality checks makes the outcome order-dependent and fragile.

## Coverage limits

The diff references but does not define `reduceFlowEvent`, `applyFlowMove`, `withFlowConsistent`, `withFlowSessionFollowing`, `FlowIds`, `AwaitingAdvance`, `draftRecordBelongsToOpenEditor`, `PatientProfileRules.ageRequired`, `PreviousPregnancyFormRules.deliveryColumns`, and `clearsFrozenPatient`. The three-event guarantees ("every `AncEvent` reaches exactly one reducer branch", "`Flow` and `RegistrationNext` are dispatched", "`RecordCommitted` advances a waiting flow only for the save it waited for") cannot be verified from this diff alone. Findings 1, 2, and 6 are conditional on those helpers; findings 3, 4, and 5 are directly observable in the diff and stand on their own.

## Tests

1. `PatientProfileRules.ageRequired` — table test: `(baseRevision = null, original.ageText = "", forceAgeRequired = false)` → `true`; `(baseRevision = 5, original.ageText = "", forceAgeRequired = false)` → `false`; `(baseRevision = 5, original.ageText = "", forceAgeRequired = true)` → `true`; `(baseRevision = 5, original.ageText = "30", forceAgeRequired = true)` → `true`.
2. `SavePatientProfileCommand.isStructurallyValid` — build a `replacementFor(stored)` for a stored patient with `revision = 5`, `ageText = ""`, `lmpDate = "2026-08-01"`, and assert `isStructurallyValid()` is `true` and the restored command's `lmpDate` equals the stored value.
3. `updatePregnancyDraft` — start from `outcome = "LiveBirth"`, `deliveryBy = "SVD"`, `birthMethod = "SVD"`, `outcomeText = ""`; dispatch `PregnancyDraftChanged(OUTCOME, "LiveBirth")`; assert `outcomeText` is unchanged (the current code blanks it).
4. `updatePatientDraft` — start from `values.lmpDate = "2026-08-01"`; dispatch `PatientDraftChanged(LMP_DATE, "2026-07-25")`; assert `values.lmpDate == "2026-07-25"` (the current code leaves it unchanged).
5. `registrationNext` — set `awaitingAdvance = AwaitingAdvance("E1", FlowIds("E1", "P1"))`, `patientDraft = null`, `datingDraft = <draft>`, `pregnancyDraft = <stale draft>`; dispatch `RegistrationNext("E1", "P1")`; assert the save is performed on the dating draft and the pregnancy draft is not touched (the current `when` order and the id-only guard can misroute).
[Tokens] upload: 14804 download: 3038 cache_read: 0 cache_write: 0

