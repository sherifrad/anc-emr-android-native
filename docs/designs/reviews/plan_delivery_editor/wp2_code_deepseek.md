## Blocker

**B1. The patch does not deliver the feature; the reported defect still reproduces.** The diff (patch L1–237) touches only `state/`. There is no `DeliveryPlanSheet.kt`, no parser move/compose, no strings, no `PlanTab`/`PatientHubScreen` hosting, and no change to `DeliveryPlanningCard.onEditPatient`. So tapping the pencil still emits `AncEvent.StartPatientEditor` and still sets `AncRoute.PatientEditor` — the exact bug the design exists to fix. `PatientDraft.deliveryPlan` (patch L106) has no production setter, so it is dead state; `deliveryPlanSheetDraft` (patch L154) has no caller. As a work package this is fine only if labelled "state only — do not ship".

**B2. Back during an in-flight save can discard the draft.** Patch L63–72 (`AncReducer.kt`) returns early for any `patientDraft.deliveryPlan != null` without checking `state.savePhase.blocksEditing()`. Scenario: touched fields, slow save (the design's own "delayed fake save" test, §9), hardware Back → `showDiscardDialog = true`; confirm → `clearDrafts()` while `UpdatePatientCommand` is in flight. The sheet vanishes mid-save, the draft is gone, and a subsequent failure sets `saveError` against a draft that no longer exists. Design §5.5 states back is blocked while saving.

**B3. No tests for level-3 state changes.** `state/AncState.kt`, `AncReducer.kt`, `ClinicalDraftReducers.kt` are all in the level-3 map. The patch adds zero unit tests while design §9 specifies `DeliveryPlanReducerTest` (start guards, untouched submit, failure retention, cancel, route unchanged). AGENTS.md requires an independent test review for these paths; there is nothing to review.

## Major

**M1. Amendment A11 is dropped.** `startDeliveryPlanEditor` (patch L181–200) copies `clearDrafts()` plus `savedNotice`/`notice` only — unlike `updateDeliveryPlanField` (patch L220–221) it never sets `savePhase = IDLE` / `saveError = null`. Open the sheet straight after a failed save elsewhere: either the sheet shows a stale failure message before the doctor touches anything, or (if `ERROR` blocks editing) the pencil silently does nothing.

**M2. The untouched-save rule was moved into the composable.** A15: "the sheet's Save button sends `CancelEditor` when `touched` is false, so the reducer needs no special case". That is business logic in UI (AGENTS.md rule 2), untestable at the reducer, and it makes any other trigger — TalkBack activate, hardware Enter on the focused Save, a future caller — submit and rewrite the notes. It also contradicts the `rewritesNotes` notice ("Saving will rewrite…"), which is shown and then never acted on by a no-edit Save.

**M3. Route preservation is bolted onto the wrong branch.** Patch L49–55 keys `route = state.route` on `state.patientDraft?.deliveryPlan != null` inside the patients-reload branch, not the submit-success branch. If profile-save success does not pass through this hunk (design step 6/7: "not verified"), the sheet still resets the route and the Plan tab, and nothing in the patch detects it.

**M4. Two discard dialogs, one with a no-op pending action.** Patch L70 sets `PendingAction.Navigate(state.route)` — navigating to the route you are already on — while design §5.5 specifies the sheet's own `deliveryPlanDiscardDialog`. Confirm semantics of the reducer path are unverified; the UI path is unspecified in the patch. Expect either a double prompt or a confirm that clears nothing.

**M5. Keyboard/IME layout will break at 360 dp.** `imePadding()` on the sheet column plus `ModalBottomSheet`'s own IME/content insets (design §5.5) double-applies, and `weight(1f, fill = false)` on the scrolling body stops it shrinking. At 360 dp with font scale 1.3 and the keyboard up, the Cancel/Save row is pushed off-screen. Separately, all four fields are multi-line with no `KeyboardActions`/`focusRequester` chain, so Enter inserts a newline (needed for Hospital, wrong for the others) and keyboard users get no field advance.

**M6. Stored English labels are user-visible, hard-coded and untranslatable.** `LABEL_*` live in the model but surface in the patient form, print and clinical summary. In an RTL locale an Arabic `obstetricNotes` field containing `Agreed Delivery Fees: 5000 EGP` renders bidi-mangled, and translating the labels would silently stop the strict rule from matching. The guard the design promises (A8: assert labels equal `strings.xml` titles) is absent.

## Minor

**M7.** `it.status != "archived"` (patch L177) duplicates a magic string instead of a shared constant.

**M8.** `DeliveryPlanFields.touched` (patch L139–142) re-composes the entire note on every read; reading it for Save-enabled state does O(n) string work on every recomposition of a 5,000-character note.

**M9.** `values.obstetricNotes` flips between `compose(...)` and `draft.original.obstetricNotes` per keystroke (patch L209–213). Any later consumer reading `values` mid-edit sees the *old* notes while the sheet shows the new text.

**M10.** `updateDeliveryPlanField` clears `errors = emptySet()` unconditionally, wiping unrelated profile-field errors the doctor has not seen.

**M11.** `isValueChangeEvent` now includes `DeliveryPlanFieldChanged` (patch L26–31); combined with M10, a debounce/save path presumably shared with the profile draft can be triggered by sheet typing. No test pins the interaction.

**M12.** Untouched dismiss also forces `savedNotice = false` (patch L68), dropping a just-shown save confirmation when the doctor reopens and closes the sheet.
