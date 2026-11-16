# Design: edit the delivery plan in place on the Plan tab

## 1. Overview

The pencil on the Delivery Planning card (tag `editDeliveryPlan`) currently sends `AncEvent.StartPatientEditor`. `startPatientEditor` in `state/ClinicalDraftReducers.kt` sets `route = AncRoute.PatientEditor(...)`. That swaps the Plan tab for the full-screen `PatientEditor.kt`, where only one field matters: `PatientDraftField.OBSTETRIC_NOTES`.

The change replaces this with a bottom sheet over the Plan tab, the same pattern as `MedicationSheet`. The sheet has four fields: hospital, fees, plan and postpartum note. On Save, a new pure function writes the four fields back into the single `obstetricNotes` string. The write goes through the existing `SubmitPatientProfile` path. The route does not change and there is no schema change.

The hard part is storage. There is one free-text string, and the four boxes are a heuristic reading of it by `parseDeliveryNotes`. The design therefore centres on a compose rule whose output the parser reads back as exactly what the doctor saw. It also centres on a guard that refuses to save when that cannot be shown.

### Findings from the pasted files

These affect the design and are stated up front.

1. **The Plan label is not stripped.**
   - The brief says "plan" is a recognised and stripped label. In `place`, only `Label.POSTPARTUM` and `Label.FEE` call `stripLabel`.
   - Line 139 is `label != null -> Placement(Home.PLAN, text)`. So "Plan: x" shows as "Plan: x" in the Plan box.
2. **A "Hospital:" line is also shown in the Plan box.**
   - Such a line has the same line-139 route as the Plan label.
   - Any "Hospital: X" line written to set the hospital box would therefore also appear in the Plan box.
   - No compose rule can round-trip that without a parser change.
3. **The box titles are hard-coded English literals** in `DeliveryPlanningCard.kt` (lines 159, 166, 173, 180). The sheet needs the same labels, so they become string resources.
4. **`toPatientDraftValues()` fills only part of `PatientDraftValues`.** It leaves `datingMethod`, `transferDate`, `embryoAgeDays`, `agreedEdd`, `scanDate`, `scanGaWeeks` and `scanGaDays` at defaults.
   - If `SubmitPatientProfile` writes those to the patient, a sheet save could overwrite them.
   - How the submit handler uses them is **not verified**. This is a risk and a test (see section 7).

## 2. Goals

- The pencil opens a bottom sheet on the Plan tab. There is no route change, so the Plan tab does not reload and keeps its scroll position.
- The sheet edits the four boxes: Agreed Delivery Hospital, Agreed Delivery Fees, Delivery Plan, Postpartum Note.
- Save writes back to `obstetricNotes` through `SubmitPatientProfile` and the existing `UpdatePatientCommand` path. No new command.
- After a successful save, `parseDeliveryNotes(saved)` equals the four boxes the doctor saw.
- Text the parser cannot file in the box where the doctor put it is refused with a message. It is never silently moved.
- One canonical component for the delivery plan, shared labels, 360 dp first, no file over 1,500 lines, and unidirectional MVI.

## 3. Non-Goals

- No schema change and no migration (Room schema v6 stays frozen).
- No structured delivery fields.
- No improvement to the parser heuristics beyond the two narrow changes in Decision 3.
- No new registration flow and no patient form redesign (a separate later task).
- No refactor of `MedicationSheet` or `MedicationSheetButtons`.
- No Undo for the delivery plan (whether profile saves have Undo is not verified).

## 4. Decisions

1. **Reuse `patientDraft`, with a nested sheet slice.**
   - `PatientDraft` gets one nullable field `sheet: DeliverySheetDraft?`. A non-null value means "delivery sheet mode".
   - Reason: the revision, `createdAt`, save phase and error plumbing already exist, and the brief asks for reuse. A separate draft type would duplicate that and add a second path into profile save.
2. **Hospital edits write a "Hospital:" line. A "Hospital: <name>" segment whose value runs to the end of the segment is consumed into the hospital list only.**
   - It is no longer also placed in a home section (a narrow change in `parseDeliveryNotes`).
   - Reason: finding 2. Without this, the hospital box could not be set independently of the Plan box, and the round trip would fail.
   - Names that are not whole-value (for example "Hospital: Cairo, 3 nights") stay in the plan as today.
3. **The Plan label is stripped like the Fees and Postpartum labels.**
   - Reason: finding 1. It gives one forcing channel, "Plan: <text>". With it, any text can be filed in the Plan box with exactly the displayed text.
   - It also makes the code match the brief's description.
4. **The compose rule is per segment and uses minimal labels.**
   - The labelled-segment form is chosen so each segment is placed by `place`, which is a pure function of the segment text.
   - A label is added only when the parser would not already place that segment in that box with identical text.
   - Reason: prose already placed in the plan keeps its exact text, so the first edit does not litter the notes with "Plan:" on every line.
5. **A save is allowed only if `parse(compose(boxes))` equals the normalized boxes.**
   - If it does not, the sheet shows an error on the offending boxes and nothing is written.
   - Reason: the proof in Decision 6 covers text the parser produced. Text the doctor types can be anything. The guard turns "may not round trip" into a refusal. It removes the need for a proof over all inputs.
6. **Order in the stored notes:** hospital lines, then fees lines, then plan lines, then postpartum lines, matching the card order. The original interleaving of sections across lines is not recoverable and is not preserved.
7. **Save is disabled until the boxes differ from the originals.** An untouched sheet never rewrites the notes. A rewrite happens only on a real edit, and the sheet warns about it before Save (Decision 9).
8. **The parser and composer move to the `model` package** as `model/DeliveryNotes.kt`.
   - Reason: reducers in `state/` already call pure helpers from `model` (for example `parseBloodPressure`). A `state` to `ui` import would invert the layering.
   - Importers beyond `DeliveryPlanningCard.kt` are not verified. The compiler will list them. The move is a pure rename plus package line, in its own commit before the behaviour changes.
9. **The rewrite notice is computed in the reducer.**
   - `DeliverySheetDraft.rewritesNotes` is true when `compose(parse(stored notes))` differs from the stored notes normalized to trimmed, non-empty lines.
   - The sheet shows a banner when it is true. This keeps logic out of composables.
10. **Save sends the existing `AncEvent.SubmitPatientProfile`.** The submit step gets one pre-step for sheet drafts (see Technical Plan). Reason: the brief prefers reuse over a new command.
11. **Dirty rule.** The sheet is dirty when `sheet.values != sheet.original`. `hasDirtyDraft` must include this (Decision 12).
12. **State edits to `hasDirtyDraft`, `CancelEditor` and the save-success path** so that a sheet draft never changes the route. Their current code is **not verified** (see Risks).
13. **Box titles become string resources** shared by the card and the sheet.
14. **The stored labels are fixed English tokens** ("Hospital:", "Fees:", "Plan:", "Postpartum:"). They are never localized, because the parser recognises only English labels.

## 5. Technical Plan

### State

```
enum DeliveryBox { HOSPITAL, FEES, PLAN, POSTPARTUM }          // model/DeliveryNotes.kt

data class DeliveryBoxValues(hospital, fees, plan, postpartum: String = "")

data class DeliverySheetDraft(
  values: DeliveryBoxValues,
  original: DeliveryBoxValues,
  rejected: Set<DeliveryBox> = {},
  rewritesNotes: Boolean,
)

PatientDraft(... existing fields ..., sheet: DeliverySheetDraft? = null)
```

`AncRoute` is not touched.

### Events

```
AncEvent.StartDeliveryPlanEditor(patientId, editorSessionId)
AncEvent.DeliveryBoxChanged(box: DeliveryBox, value: String)
// Save: existing AncEvent.SubmitPatientProfile
// Close: existing AncEvent.CancelEditor
```

### Reducer

```
startDeliveryPlanEditor(state, e):
  if state.savePhase.blocksEditing() || state.hasDirtyDraft: return state
  patient = state.patient(e.patientId) ?: return state
  if patient.status == "archived": return state
  boxes = parseDeliveryNotes(patient.obstetricNotes).toBoxValues()
  values = patient.toPatientDraftValues()
  return state.clearDrafts().copy(          // route untouched
    patientDraft = PatientDraft(e.editorSessionId, patient.id, patient.revision,
        patient.createdAt, original = values, values = values,
        sheet = DeliverySheetDraft(boxes, boxes, rewritesNotes = ...)),
    savedNotice = false, notice = null)

updateDeliveryBox(state, e):
  if state.savePhase.blocksEditing(): return state
  sheet = state.patientDraft?.sheet ?: return state
  new = sheet.copy(values = sheet.values.with(e.box, e.value),
                   rejected = sheet.rejected - e.box)
  return state.copy(patientDraft = draft.copy(sheet = new),
                    savePhase = IDLE, saveError = null)

prepareSheetSubmit(draft):              // pre-step inside the existing submit path
  sheet = draft.sheet ?: return Proceed(draft)
  if sheet.values == sheet.original: return CloseOnly
  when (r = composeDeliveryNotes(sheet.values.toSections())):
    Composed -> Proceed(draft.copy(values = draft.values.copy(obstetricNotes = r.notes.orEmpty())))
    Rejected -> Stay(draft.copy(sheet = sheet.copy(rejected = r.boxes)))   // no save
```

The final path from `Proceed` onward is the unchanged `SubmitPatientProfile` handler, which builds `UpdatePatientCommand`. On success the draft clears and the sheet disappears because `patientDraft` becomes null. The route is never read or written.

The reducer is pure. Compose runs inside it, not inside a composable.

### The compose rule and the round-trip rule

The rule uses these existing parser pieces: `splitSegments`, `Segment.separatorBefore`, `place` and `Placement`. They must be reachable from `composeDeliveryNotes`, so the composer lives in the same file and they stay private.

```
normalize(box text): lines trimmed, empty lines dropped, joined "\n"; null if nothing left
normalize(hospital): same, plus duplicate lines dropped (first kept)

fmt(box, seg):
  if place(seg.text) == Placement(homeOf(box), seg.text): seg.text      // already right
  else label(box) + ": " + seg.text    // Fees: / Plan: / Postpartum:

line(box, L): segments = splitSegments(L)
  fmt(seg0) + for k>=1: seg[k].separatorBefore + fmt(seg[k])

hospitalLine(n) = "Hospital: " + n     // only if hospitalNames("Hospital: "+n) == [n]

compose(S):
  lines = [hospitalLine(n) for n in S.hospital lines that qualify]
        ++ [line(FEES, L) for L in lines(S.fees)]
        ++ [line(PLAN, L) for L in lines(S.plan)]
        ++ [line(POSTPARTUM, L) for L in lines(S.postpartum)]
  text = lines.joinToString("\n").ifBlank { null }
  r = parseDeliveryNotes(text)
  bad = boxes where normalize(S.box) != r.box      // hospital compared as a set
  return if bad.isEmpty() Composed(text) else Rejected(bad)
```

**Round-trip claim.** For every `S = parseDeliveryNotes(n)`, `parseDeliveryNotes(compose(S).notes) == S`. Hospital order is cosmetic. It is equal as a list whenever every hospital name qualifies for a hospital line, and equal as a set always.

**Proof sketch** (the property tests in section 7 are the executable check):

- **L1, segment fidelity.**
  - A label is added only at the start of a segment.
  - The label starts with a letter and contains no ';' and no '.'.
  - The splitter splits at ';' and at '.' followed by spaces, a letter and no abbreviation. The abbreviation test reads the token before the dot. That token cannot cross a space or a colon, so it never sees the new prefix.
  - Splits at ';' do not depend on the following character. A '.' split depends only on the next character being a letter, and a labelled segment still starts with a letter. A segment that starts with a non-letter after a '.' never existed as a separate segment.
  - So the composed line splits into the same segments with the same separators.
- **L2, placement.** `place` depends only on the segment text.
  - A verbatim segment is placed correctly by the guard in `fmt`.
  - A labelled Postpartum segment always lands in postpartum with the text after the label.
  - A labelled Plan segment always lands in plan, with the label stripped (Decision 3).
  - A labelled Fees segment lands in fees only if the text has no quoted, quote, estimate, estimated, proposed, proposal, discussed, undecided or "not agreed".
  - Every fee segment the parser produced passes that test. A fee run was placed only when `UNCONFIRMED_FEE` did not match. Stripping a label cannot create a new match, and the label "Fees" adds none.
  - So `place(fmt(box, seg)) == (home(box), seg.text)`.
- **L3, rejoin.** All segments of a composed line go to one home, so the run is rebuilt with the original separators. The line equals the original box line. Lines of one box keep their order and are joined with a newline, as before.
- **L4, hospital box.**
  - Hospital lines come first, so a name that qualifies for a hospital line is added in box order.
  - Names derived from the other boxes' text are the same names, because a label does not change phrase detection. Labels never carry a past-delivery or current-pregnancy marker, and they never create a capitalised "<Name> Hospital" phrase. So they add nothing new to the set.
  - A name that does not qualify (for example one with an inner '.' after a non-abbreviation token) is left to re-derivation from the text where it already occurs.

**Hospital list.** It is a derived set, not stored data. A name in the box becomes a "Hospital: <name>" line. Removing a name from the box has no effect if the same phrase still sits in another box. The guard then rejects the Hospital box with the message "this hospital is also written in another box".

**Text the parser placed by heuristic.** It keeps its text. If the parser already places it in that box, it is written verbatim. Otherwise it gets the label for its box. A fee segment with unconfirmed wording is plan text by the parser's own rule, so the guard rejects it from Fees.

**Out of scope for the proof:** a line with no segment (for example a lone ";"). The parser drops such a line, so the guard rejects it as a mismatch.

### UI

`DeliveryPlanSheet` is a stateless `@Composable` taking `draft: PatientDraft`, `savePhase`, `saveError`, `touched`, `onEvent` and `footer`. It mirrors `MedicationSheet`.

- `ModalBottomSheet(skipPartiallyExpanded = true)` with `confirmValueChange`: dismissal is blocked while saving. If `touched`, the discard dialog opens instead.
- Layout, single column at 360 dp:
  - Title (heading).
  - Rewrite banner (only when `rewritesNotes`).
  - A scrolling column with the save message and four `OutlinedTextField`s.
  - A fixed button row.
- The body uses `Modifier.weight(1f, fill = false).verticalScroll(...)` with `imePadding()` on the sheet column. The button row stays visible above the keyboard.
- Fields, in this order:

  | Field | Label (resource) | Behaviour | Test tag |
  |---|---|---|---|
  | Hospital | `delivery_box_hospital` | `minLines = 1`; supporting text "One hospital name per line" | `deliveryHospitalField` |
  | Fees | `delivery_box_fees` | `minLines = 2`; supporting text "Not agreed yet? Write it under Delivery Plan" | `deliveryFeesField` |
  | Plan | `delivery_box_plan` | `minLines = 2` | `deliveryPlanField` |
  | Postpartum | `delivery_box_postpartum` | `minLines = 2` | `deliveryPostpartumField` |

- Keyboard: each field uses a `BringIntoViewRequester` on focus. The save message uses `revealWhen(isFailure)` as `MedicationSheet` does.
- An empty box is an empty field with its label. The card keeps its `no_record` placeholder.
- Rejected boxes get `isError = true` and an error text.
- Buttons: Cancel and Save in one weighted row, labels `maxLines = 2`, `minimumTouchTarget()`.
  - Save is enabled only when `touched && enabled`.
  - While saving, a `LinearProgressIndicator` and the Save spinner show, and all controls are disabled. The enabled rule is the same as `MedicationSheet`: `savePhase == IDLE || ERROR`.
- Messages:
  - `RECONCILING` uses `save_reconciling`.
  - `UNRESOLVED` uses `save_unresolved`.
  - `ERROR` uses `saveError.stringRes()`.
  - Rejected boxes use the existing `correct_highlighted_fields` plus the per-box text.
  - If `errors` contains a field other than `OBSTETRIC_NOTES`, the sheet shows a new string telling the doctor to fix the profile in the full patient form. What errors the submit step can raise is not verified.
- Discard dialog: reuse `discard_changes`, `discard_body`, `discard` and `keep_editing`.
- Host: `PatientHubScreen` shows `DeliveryPlanSheet` when `state.patientDraft?.sheet != null`, next to the existing `MedicationSheet` host block. The file's current structure is not verified. The Undo bar goes in `footer`, as for the Meds sheet.

### Cases the sheet handles

- **Another draft open or a save running.** `startDeliveryPlanEditor` returns the state unchanged. The pencil does nothing, the same as the other starters. A modal sheet's scrim blocks the pencil in practice. Whether a dirty draft can exist while the hub is on screen is not verified.
- **Archived patient.** `canEdit` hides the pencil (how it is computed is not verified), and the reducer also refuses `status == "archived"`.
- **Crash.**
  - The draft is in memory only, like the medication draft. After process death the sheet is gone and unsaved edits are lost. Whether drafts persist across process death is not verified.
  - The stored notes are written only by the one save command, so they are never half-written. A crash during a save falls to the existing `RECONCILING` and `UNRESOLVED` handling. How that is reconciled on relaunch is not verified.
- **Revision conflict.** `baseRevision = patient.revision`, as in `startPatientEditor`. A stale save fails through the existing `SaveUiError` message. The sheet stays open with the doctor's values.

### User-visible consequence of the rewrite

The first edit rewrites the stored notes into grouped lines. Hospital lines come first, then fees, plan and postpartum lines. Fees, postpartum text and some plan text gain a label only where the parser would otherwise move them.

- **Banner (when `rewritesNotes`):** "Saving rewrites the patient's obstetric notes as lines grouped under Hospital, Fees, Plan and Postpartum. Your text is kept; lines may change order."
- **Patient form:** shows the new multi-line text in "Obstetric notes", labels included. Whether the field is multi-line is not verified.
- **Print and clinical summary:** they read `obstetricNotes`. If they print the raw string, they show the grouped, labelled lines. If they use `parseDeliveryNotes`, they show the same four boxes as the card. Which of the two they do is not verified.
- **Existing notes display:** a "Plan:" label no longer shows in the Plan box. A whole-value "Hospital:" line no longer repeats there.

## 6. Alternatives Considered

| Alternative | Pros | Cons | Technical reason rejected |
|---|---|---|---|
| Structured delivery columns | Clean data, no parsing | Needs a schema change | Schema v6 is frozen without clinical sign-off |
| Keep the full patient form and scroll to the notes field | No new UI | Shows the whole 638-line form | It still sets `route = AncRoute.PatientEditor`, so the Plan tab is swapped and reloads |
| One raw "Obstetric notes" field in a sheet | No parse or compose risk, no rewrite | Not the four boxes. The doctor cannot target hospital or fees | The boxes are heuristic, so a free line may land anywhere. The brief asks for the four boxes |
| New command (`UpdateDeliveryPlan`) | Narrow path | Second writer of `obstetricNotes`. Duplicates revision and save plumbing. More level-3 surface | The brief asks for reuse. A second writer can race with the profile form |
| New separate delivery draft type | Isolation | Duplicates `revision`, `createdAt` and save phase | Needs a second path into profile save. Reuse of `patientDraft` is enough |
| Label every segment, including prose | Simplest compose rule | Adds "Plan:" noise to plain prose lines. The noise reaches print and the form | It rewrites more text than needed. Minimal labels give the same round trip |
| Marker or JSON block in the notes | Round trip is trivial | Print, summary and parser would need to understand it | A new storage convention, which is a legacy shim and has no clinical sign-off. Other readers are not verified |
| No parser change, hospital box read-only | Smallest change | A doctor cannot set a hospital that is not already in the text | Any new "Hospital:" line also appears in the Plan box, so a round trip is impossible. Kept only as a fallback if reviewers refuse Decision 2 |
| Compose without a guard | Less code | Silent moves of text on an unproven input | The proof covers only parser output. Doctor-typed text needs the guard |

## 7. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Parser behaviour changes for existing notes (Plan label, whole-value Hospital line) | Narrow rules, own commit, updated parser tests. The change is visible on the card as a removal of redundant label text. Tests asserting the old behaviour are not verified. Review all of them |
| Print or summary read the raw string or call the parser | The theorem keeps `parse` stable. The raw-string output changes only in line order and labels. A manual check of both outputs is in section 9 |
| Submit seeds dating fields from partial draft values (finding 4) | Reducer test and an instrumented check: a sheet save leaves name, phone, EDD, dating method and scan fields unchanged. If the test fails, the sheet must seed the missing fields from the patient |
| `hasDirtyDraft`, `CancelEditor` or save success change the route for a patient draft | State changes in Decision 12. Reducer tests assert the route is unchanged on start, cancel and save |
| Another consumer assumes `patientDraft != null` means the PatientEditor route | Search `patientDraft` usages before coding (not verified). Tests cover hub rendering with a sheet draft |
| Composed text exceeds a notes length cap, if one exists | Not verified. Check the submit validation and surface the existing error |
| Future parser edits break the round trip | Property tests run in CI. The guard still prevents a bad write |
| The guard rejects text the doctor thinks is valid | The message names the box and says where to move the text. Hints sit under Fees and Hospital |
| A doctor clears all four boxes | Allowed. The notes become empty. Save needs a real edit. There is no Undo (not verified) |
| 360 dp truncation or the keyboard hides the focused field | Fixed button row, scrolling body, `imePadding`, and instrumented tests at 360 dp |
| Manifest soft-input mode or edge-to-edge settings change the keyboard behaviour | Not verified. The instrumented test covers the focused field |

### Out of scope

The new registration flow, the patient form redesign, structured delivery data, better parser heuristics, Undo for the plan, and extracting shared sheet chrome with `MedicationSheet`. That last item would touch freshly changed Meds code.

## 8. Detailed Implementation Plan

### Review rule

Files in `state/`, `storage/` and `PatientHubScreen.kt` are level 3 and need an independent review. The parser move and the compose function are pure but clinical, so they get the same review.

### Prerequisite reading (not verified, must read first)

`AncState`, `PatientDraft` and `hasDirtyDraft`. The `SubmitPatientProfile` handler and what it does on success. The `CancelEditor` reducer. `PatientHubScreen`'s medication sheet host.

### Create

| File | What and why |
|---|---|
| `ui/patienthub/components/DeliveryPlanSheet.kt` | The sheet, four fields, buttons, save message, banner and discard dialog. One canonical component for the delivery plan edit. It is a single file because the form is four fields. |
| `app/src/test/.../model/DeliveryNotesComposeTest.kt` | Compose, guard and property tests |
| `app/src/test/.../state/DeliveryPlanReducerTest.kt` | Reducer tests |
| `app/src/androidTest/.../DeliveryPlanSheetTest.kt` | Instrumented sheet test |

Test directory paths are not verified.

### Move

`ui/DeliveryNotesParser.kt` becomes `model/DeliveryNotes.kt`. The `ui/DeliveryNotesParser.kt` file is deleted. First commit: package line and imports only.

### Modify

| File | Change and reason |
|---|---|
| `model/DeliveryNotes.kt` (moved) | Strip the Plan label in `place`. Consume whole-value "Hospital:" segments (they end the current run). Add `DeliveryBox`, `DeliveryComposeResult`, `composeDeliveryNotes`. Reason: Decisions 2 to 5 |
| `ui/patienthub/components/DeliveryPlanningCard.kt` | Rename `onEditPatient` to `onEditDeliveryPlan`. Replace the four title literals with the new string resources. Update the `parseDeliveryNotes` import. Reason: shared labels, same tag `editDeliveryPlan` |
| `ui/patienthub/tabs/PlanTab.kt` | The pencil sends `AncEvent.StartDeliveryPlanEditor(patient.id, UUID.randomUUID().toString())`. `StartPatientEditor` is no longer sent from here, and `newPatientId` is no longer needed |
| `ui/PatientHubScreen.kt` (level 3) | Host `DeliveryPlanSheet` when `patientDraft?.sheet != null`, like the medication sheet. Pass `touched`, `savePhase`, `saveError` and the Undo footer |
| `state/ClinicalDraftReducers.kt` (level 3) | Add `startDeliveryPlanEditor`, `updateDeliveryBox` and the box mappers. Leave `startPatientEditor` unchanged |
| State file with `AncEvent` | Add `StartDeliveryPlanEditor` and `DeliveryBoxChanged`. File path not verified |
| State file with `AncState`, `PatientDraft` | Add `DeliverySheetDraft` and `PatientDraft.sheet`. Extend `hasDirtyDraft` so a sheet is dirty when `values != original`. Not verified |
| `AncReducer.kt`, `AncViewModel.kt` (level 3) | Dispatch the two events. Add the submit pre-step. Make `CancelEditor` and the save-success path clear the draft only when `sheet != null`. Not verified |
| `res/values/strings.xml` | Add: four box labels, sheet title, rewrite banner, per-box hints, per-box rejection text, profile-problem text. Path not verified. Other locales are not verified |

### Delete

Nothing. `AncEvent.StartPatientEditor` stays: whether another screen uses it is not verified. If the compiler shows that no caller remains, delete it and its reducer branch, per the "no dead code" rule.

`PatientEditor.kt` (638 lines) and `app/schemas` are unchanged. No UI file approaches 1,500 lines.

## 9. Tests

### Unit: compose and round trip (`DeliveryNotesComposeTest`)

**Examples**
- Empty notes compose to null.
- One fee line becomes "Fees: ...".
- A fee with "quoted" is rejected from Fees.
- A plan line that starts with "Plan:" round-trips as "Plan: Plan: ...".
- A whole-value hospital becomes a hospital line and does not appear in the plan.
- "Hospital: Cairo, 3 nights" stays in the plan and is listed in the hospital box.
- The "Dr." abbreviation and "10:30" survive.

**Properties** (seeded `kotlin.random.Random`, about 5,000 cases from a vocabulary of fragments)
- P1: for random notes `n` and `S = parse(n)`, compose returns `Composed`, and `parse(compose(S)) == S`. Hospital is compared as a list when every name qualifies, and as a set otherwise.
- P2: idempotence, `compose(parse(compose(S))) == compose(S)`.
- P3: for random doctor-typed boxes, the result is either `Rejected` or `Composed` with exact normalized equality. It is never a silent change.
- P4: every non-blank segment of `n` appears in some box (the parser's own invariant).

A failing case prints its seed and input. A property-test library is not verified, so the plain seeded loop is the default.

### Parser tests

The new Plan-label and hospital-line rules. Update any existing assertion on the old behaviour (not verified).

### Reducer tests

- Start leaves the route unchanged.
- Start is ignored while `blocksEditing`, with a dirty draft, for an archived patient, and for an unknown patient.
- Seeds the boxes from `parseDeliveryNotes`, and `rewritesNotes` is correct.
- `DeliveryBoxChanged` updates the box, clears `rejected` for that box, and resets `savePhase` and `saveError`. It is ignored while saving.
- Submit with untouched boxes closes without saving.
- Submit with a rejected box sets `rejected` and does not save.
- Submit with a composed text sets `obstetricNotes` and leaves all other values unchanged, including the dating fields.
- `CancelEditor` leaves the route unchanged.
- `hasDirtyDraft` is true after a box edit.
- `startPatientEditor` still works and `sheet` is null.

### Instrumented (`DeliveryPlanSheetTest`)

- The pencil opens the sheet, and `planTabContent` stays displayed.
- The fields show the current boxes.
- Edit the plan and Save: the sheet closes and the card shows the new text.
- A touched Cancel shows the discard dialog.
- A failed save keeps the sheet and shows the message.
- Controls are disabled while saving.
- A rejected fee shows the field error.
- At 360 dp the title, both buttons and the focused field are displayed with the keyboard open. How existing tests force 360 dp is not verified.

## 10. Verification

```
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:compileDebugAndroidTestKotlin
.agents/scripts/run_emulator_all_tests.sh
```

Manual checks:

1. On a 360 dp emulator, open the Plan tab and tap the pencil. The Plan tab stays visible behind the sheet, with its scroll position kept.
2. Edit only the plan, then Save. Open the patient form, print and clinical summary. Confirm that notes, hospital, EDD, dating method and scan fields are unchanged apart from the rewrite.
3. Check the banner text on a note that will be rewritten.
4. Confirm `app/schemas` has no diff.
5. Obtain the independent review for the level-3 files and the independent test review.
