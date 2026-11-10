# Medication Section Revision (Stage 1)

*   **Author:** sherifrad
*   **Status:** Proposed
*   **Date:** 2026-11-10 (revised the same day after the Goldfish review)

--------------------------------------------------------------------------------

## Overview

The Meds tab in the patient workspace is where one doctor records and reviews a patient's medications on a phone. Today, tapping a drug opens a separate full-screen editor and then returns, which rebuilds the whole tab. The quick-add list also fills the screen with one large card per drug.

This design changes the Meds tab so that adding, stopping and editing a medication happen in a bottom sheet on top of the tab, without leaving it. The tab is review-first, because at follow-up visits the doctor mostly checks existing medications.

Stage 1 is deliberately small: it fixes the refresh and the long list, and it uses the storage, save rules and active/stopped logic that already exist. Course length, change history, undo for stop and change, favourites, variants and the settings screen are Stage 2 (see Non-Goals and Future Improvements). The review found these larger features need schema or save-path work that is not worth the risk in Stage 1.

A clickable 360dp HTML mock is a deliverable and is approved before any code is written.

## The Problem

1. **The tab refreshes on every tap.** Tapping a drug tile starts an editor event, up to five field events and a save event. The first one changes the app's navigation route to a full-screen medication editor, so the workspace is replaced. After saving, the app navigates back and builds the Meds tab again. Scroll position, search text and open cards are lost.
2. **The add list is too long.** Every catalog drug is drawn as a full-width card (at least 48dp tall). On a 360dp phone this pushes the patient's active prescriptions off the screen.
3. **Data entry is tiring.** The doctor moves between fields by hand, and fields can be hidden behind the keyboard.
4. **Review is not the main path.** In pregnancy follow-ups the doctor mostly reviews the existing list, but the screen is built around adding.

## Goals

*   Adding, stopping and editing a medication never leaves the Meds tab. Scroll position, search text and the "Show past" toggle survive every action.
*   Review-first list: active drugs first, a stop symbol on each active row, stopped drugs behind a "Show past" toggle.
*   One collapsed "Add medication" picker with search replaces the always-open tile list. Adding a catalog drug takes three taps the first time (open the picker, tile, Save) and two while the picker stays open: the tile opens the sheet already filled in with the saved dose, route and frequency, and Save adds it.
*   Stopping a drug takes one tap: the stop symbol saves the drug as Stopped with today's date at once, with no sheet. The row moves to "Show past" and an Undo bar stays for 10 seconds. For a different stop date the doctor opens the row and edits the date (author's decision, 2026-11-10).
*   Editing a drug (for example changing its dose or frequency) updates that same row. The sheet has one date field that defaults to today, with the existing "Today" shortcut and date picker.
*   Pressing Enter moves focus to the next field, and the focused field stays fully visible above the keyboard.
*   When a save fails, the sheet stays open with the doctor's values and shows the cause using the app's existing save-error messages. There is no automatic retry.
*   One medication form is used by the sheet. No second medication editor exists.
*   Patient name, gestational age, blood pressure and the high-risk alert stay readable at 360dp.
*   The clickable 360dp mock is reviewed and approved first.

## Non-Goals

*   No Room schema change. Schema v6 stays frozen.
*   No change to the save command, the repository, the DAO or the undo code. Stage 1 does not touch `ClinicalCommands.kt`, `RecordsRepository.kt`, `RecordsDao.kt` or `UndoCoordinator.kt`.
*   Deferred to Stage 2 (decided after the review):
    *   Course length and automatic end of a short course.
    *   Change history. Editing a dose updates the row in place, so the earlier dose is not kept as a separate record. A proper history needs a schema change and clinical sign-off.
    *   The "Changed" mark and any change text in `notes`.
    *   Undo for a dose edit. The undo the app already offers after "Save and add another" stays. Undo for the one-tap stop is a small exception, described in Technical Plan Section 1 (Stop).
    *   The "3 days ago" date shortcut.
    *   A shared Enter-to-next helper for all editors. Stage 1 uses the standard Compose focus tools inside the medication form only.
    *   Favourites, dose/frequency variants, a default variant and the medication settings screen.
*   No drug-interaction checks, prescription printing or desktop port.
*   A different administration route (for example an injection) is a separate drug and not a variant. This rule is recorded now and applied in Stage 2.

## Open Questions

*   None. The questions raised during drafting and review were answered by the author on 2026-11-10 (see Decisions).

### Decisions

*   **Schema stays frozen** (author, 2026-11-10). The earlier idea of a line of text in `notes` to record changes was dropped after the review.
*   **Hidden dates, auto-stamped.** The start date defaults to today. One date field shows it and the doctor can change it.
*   **"Active until stopped".** A drug stays active until the doctor stops it. This is the existing behavior.
*   **Bottom sheet for every action** (author's choice between three options; see Alternatives). Adding takes two or three taps (see Goals); stopping takes one tap and has no sheet.
*   **Mock approved** (author, 2026-11-10): layout of `docs/mocks/medication-section.html` approved with three changes: one-tap stop with a 10-second Undo (no stop sheet), first add with a collapsed picker takes three taps, Cancel prompts only after the doctor edits.
*   **Scope reduced after the Goldfish review** (author, 2026-11-10): the author accepted the reviewers' advice to remove course length, the change record, the two-row replace command, undo for stop and change, the "3 days ago" shortcut and the shared Enter helper from Stage 1.
*   **Failed save:** show the cause using the existing messages; no automatic retry.

## Terms Used in This Document

*   **Navigation route** (`AncRoute`): which screen the app is showing. The current bug is a navigation route change.
*   **Administration route** (or just "route" on a drug): how the drug is given (Oral, IM, IV, Topical, Subcutaneous).
*   **Stored status** values are `Active`, `Completed`, `Discontinued` and `Patient non-compliant`. The screen shows two labels: **Active** (stored Active or Patient non-compliant) and **Stopped** (stored Completed or Discontinued). Stopping a drug stores `Discontinued` and sets the end date.
*   **Draft** (`medicationDraft`): the values the doctor is editing before saving. It holds the saved values (`original`) and the edited values (`values`). A draft is "dirty" when they differ.
*   **Save command, revision, operation id, receipt:** a save is one command that carries the patient's and the medication's revision numbers (to refuse a write based on stale data) and an operation id (so a repeat is recognized). Stage 1 uses the existing command unchanged.

## Critical User Journeys (CUJs)

### CUJ 1: Review a returning patient and stop one drug

*   **Given**: A patient with 4 active drugs and 2 stopped drugs; the doctor is on the Meds tab.
*   **When**: The doctor taps the stop symbol on one drug.
*   **Then**: No sheet opens. The drug leaves the active list and sits under "Show past", an Undo bar shows for 10 seconds, and tapping Undo puts the drug back to its earlier status and end date. The tab does not reload and keeps its scroll position.

### CUJ 2: Add a known drug

*   **Given**: The doctor opens the collapsed "Add medication" picker and types part of a name.
*   **When**: The doctor taps the drug. The sheet opens with the saved dose, route and frequency filled in, and the doctor taps Save (or presses Enter on the last field).
*   **Then**: The drug appears in the active list with today as its start date. The tab keeps its scroll position and search text. If the doctor chooses "Save and add another", the sheet opens blank for the next drug.

### CUJ 3: Change a dose

*   **Given**: A patient is on iron once daily.
*   **When**: The doctor opens that row, changes the frequency to twice daily and taps Save.
*   **Then**: The same row now shows twice daily. The earlier frequency is not kept (Stage 2 adds history).

## Technical Plan (Architecture)

### Summary

Every medication action opens a bottom sheet over the Meds tab. The sheet shows one shared medication form bound to the existing `medicationDraft`. Saving uses the existing command, validation and autosave unchanged. The only structural change is that starting a medication draft no longer changes the navigation route, and the full-screen editor is deleted.

```text
Before:  Meds tab --tap--> AncRoute.MedicationEditor (full screen) --save--> Workspace (rebuilt)

After:   Workspace(MEDICATIONS)      the navigation route never changes
           |- PatientHubScreen hosts MedicationSheet (shown while medicationDraft != null)
           |    \- MedicationForm (one shared form)  --events--> medicationDraft --Save--> existing save command
           \- MedsTab (review list, collapsed picker, Show past toggle)
```

### 1. State: the draft stays, the route does not

*   Starting a medication draft creates `medicationDraft` and leaves the navigation route as it is. `AncRoute.MedicationEditor` is deleted with its uses (back handling and the patient lookup).
*   The start event gets one new optional input, `prefill` (a set of draft values). One reducer step builds the whole draft. The old sequence of start plus several field events is removed.
*   What `original` and `values` hold at start:
    *   **Add from a catalog tile:** `original` is the blank new-drug defaults (Oral route, Active, start date today). `values` is the tile's name, dose, route and frequency on top of those defaults. The draft is therefore dirty on open, so it is recovered after a crash. The discard prompt on Cancel is separate: it shows only after the doctor edits something, tracked by a "touched" flag in the draft that is set by the first field change, so Cancel on an untouched form closes at once (author's decision, 2026-11-10).
    *   **Stop:** there is no draft and no sheet (one-tap stop, see the Stop rule below). Only the edit sheet is used to give a different stop date.
    *   **Edit:** `original` and `values` are both the stored row.
    *   **Blank custom drug ("Prescribe" button):** both are the blank defaults. The "Prescribe" button stays and opens the sheet.
*   The editor hook (`MedicationEditorHook`) closes the draft only; its branch that reset the navigation route is removed.
*   Back with a draft open closes the sheet through the existing cancel and discard-changes logic. It does not leave the workspace.

### 2. The sheet and the shared form

*   `MedicationSheet` is a `ModalBottomSheet` using the same configuration as the one in `PatientListScreen` (fully expanded). It is hosted in `PatientHubScreen`, above the tab pager, and not inside `MedsTab`. The modal scrim blocks tab swipes while it is open, so the sheet cannot be orphaned by a tab change.
*   `PatientHubScreen` passes `medicationDraft` and the save phase down to `MedsTab` as immutable values. Composables send only `AncEvent`s.
*   `MedicationForm` is extracted from the current editor and is the only medication form. It shows name, dose, route (choosable only when adding; read-only on an existing row), frequency, the date field, the Active/Stopped control and notes.
*   Frequency: a short fixed list plus a custom entry. The fixed values are the ones the current editor and catalog already offer.
*   Cancel and Save sit at the bottom of the sheet; "Save and add another" stays and uses the existing path. While a save is running they are disabled with a progress indicator.
*   At 360dp the sheet is limited to the screen height minus the keyboard. The form scrolls inside the sheet and the Save button stays visible.

### 3. Dates

*   One date field, labelled by the action: Start date when adding or editing an active drug; Stop date when stopping.
*   It reuses the existing `AncDatePickerField` (with its "Today" shortcut) and `AncDatePickerDialog`. It defaults to today. There is no new shortcut and no new date logic.
*   The active/stopped decision stays exactly as today (`isOngoingMedication` and `medicationStatusLabel` are not changed). Because of that, the clinical summary, the print output, the header count and the stop symbol keep agreeing without any change.

### 4. Stop and edit

*   **Stop:** the stop symbol saves the stored row at once with status Discontinued and end date today, using the existing save command for an existing row (with the row's current revision). No draft or sheet opens. While that save runs, every entry point is disabled as in any other save. On success the Meds tab shows an Undo bar for 10 seconds. Undo is not the undo coordinator: it is a second normal save of the same row that restores the earlier status and end date, using the row's new revision, so the history shows two saves. The bar is dropped when the doctor starts any other action or leaves the patient. If the stop save fails, the row stays active and the cause is shown in the same way as other save failures. The one-tap stop and its Undo are in `AncState.kt` and `EditorHooks.kt`, which are level-3 files, so they need the frontier-tier review and an independent test review.
*   **Edit:** the row opens the sheet on the stored values. Saving updates the same row through the existing command. This includes changes to dose or frequency.
*   Because dose edits happen in place, Stage 1 does not keep earlier doses. This is recorded as a known limit (see Risks and Mitigations).

### 5. Add picker and Show past

*   The always-open tile list becomes one collapsed "Add medication" row. Opening it shows the search box and a list with a limited height that scrolls inside itself, using the existing `filterMedicationTiles`. Common and Recent stay as they are.
*   A tile starts the draft with its catalog dose, route and frequency. The tile model gets a `frequency` so it can pass it on; custom-choice tiles have no saved frequency, so the frequency starts blank and the doctor picks one. A tile for a drug that is already active shows its check mark and does not open the sheet.
*   Past drugs sit behind a "Show past" toggle. The tab is no longer rebuilt, so the toggle keeps its state.
*   While a save is running or a medication draft is open, every entry point is visibly disabled: picker tiles, the stop symbol on every row, the edit action and the "Prescribe" button. A tap during a save is never dropped silently.

### 6. Enter moves to the next field

*   `MedicationForm` gives each field the Next keyboard action, and the last one Done, and moves focus with standard Compose focus requesters. The focused field is brought into view above the keyboard (the sheet uses `imePadding` and a bring-into-view request on focus).
*   The ad-hoc focus code of the old editor is removed with it. A shared helper for other editors is a Stage 2 item.

### 7. Failed saves

*   A rejected save keeps the sheet open with the doctor's values. The sheet shows the same save-error message the other editors show, taken from the existing `SaveUiError` messages (changed since opened, archived patient, cross-patient, storage error, result unknown, and so on). Validation errors still show next to their fields.
*   The doctor can press Save again. There is no automatic retry. Finer messages (for example "medication was removed") are a Stage 2 item because they need changes to the error mapping.

### 8. What is deleted

*   The full-screen medication editor, `AncRoute.MedicationEditor` with its branch in the app route host, and `dispatchMedicationDraft`. The project rule is no legacy shims.
*   Tests that assert the old route are rewritten for the sheet.

## Risks and Mitigations

*   **Level-3 files are touched** (`AncState.kt`, `EditorHooks.kt`; `AncViewModel.kt` only to confirm it needs no change). Mitigation: the change stays small; a frontier-tier review and an independent test review gate each phase.
*   **Dose edits overwrite the earlier dose.** The earlier dose is not kept. Mitigation: documented here; Stage 2 adds history with a schema change that needs clinical sign-off. The earlier dose can be found only in a backup made before the edit.
*   **Draft recovery.** A recovered medication draft must reopen the sheet on the Meds tab and not navigate. Mitigation: the draft slot code has no route reference, and a test covers recovery for add, stop and edit.
*   **Sheet flicker on "Save and add another".** The save clears the draft and the next start event creates a new one. Mitigation: a UI test checks focus and the keyboard across that step; if it flickers, the sheet's visibility is derived from the draft's existence so it stays composed.
*   **Silent drops.** The start step ignores a start while another draft is open or a save runs. Mitigation: all entry points are disabled in that state (Section 5).
*   **Sheet height at 360dp with the keyboard open.** Mitigation: the layout is tested at 360dp with the keyboard shown, and the mock shows the keyboard state.

## Alternatives Considered

### Alternative A: Keep the draft-and-save path, add an inline form, remove only the screen change

*   **Pros**: Fewest files changed. One-tap add and one-tap stop. Validation stays in one place.
*   **Cons**: The start step accepts one draft at a time and ignores a start while a save runs, so two fast taps lose one without any message. Preventing that needs a queue in the view model, and a bug in the queue would lose taps silently.
*   **Reason for Rejection**: The sheet blocks a second action while it is open, which removes the lost-tap case without a queue. The cost is one extra tap per action. Rejected by author in favour of the sheet.

### Alternative B: New direct events that build the save command without a draft

*   **Pros**: Quick actions do not depend on draft state. One-tap add and stop.
*   **Cons**: Two ways to build a save command (draft and direct) can drift apart in validation. Quick actions would skip draft autosave and recovery. It changes the most level-3 code.
*   **Reason for Rejection**: Two save paths in a clinical app give two places for a validation bug. Rejected by author in favour of one path.

### Alternative C (chosen): Always a bottom sheet with the shared form

*   **Pros**: One path for every action. Validation, autosave and recovery are reused. Easy to test. No queue. No new save command.
*   **Cons**: Adding a known drug takes two or three taps instead of one. The sheet covers part of the list and needs careful height limits at 360dp with the keyboard open.

### Alternative D: Non-modal inline expanding row instead of a modal sheet

*   **Pros**: No overlay; the list stays visible.
*   **Cons**: The row grows and pushes later rows down, so the list moves while the doctor works. The keyboard covers more of a row than of a sheet, and two rows could be open at once, which needs extra state.
*   **Reason for Rejection**: The moving list is the very behavior the doctor complained about, and two open rows add state that the single draft cannot represent.

### Alternative E: Edit a dose in place (chosen for Stage 1) versus keeping the earlier dose as a separate row

*   **Chosen for Stage 1: in place.** Smallest change; no new save command, no new undo command, no new record kinds.
*   **Not chosen: stop the old row and create a new one.** It keeps the dose history, but it needs a new two-row command inside one database transaction, matching undo, finer error mapping and a decision on which fields the new row inherits. The review found the plan for this was not complete enough to build safely. It is a Stage 2 item.
*   **Also considered: two existing commands in a row** (stop the old row, then add the new one). Rejected: if the second command fails or the app stops between them, the patient has a stopped drug and no replacement, and the retry rules for two commands are not defined.

### Alternative F: Record "changed" as a line in `notes`

*   **Pros**: No schema change.
*   **Cons**: The line is editable text, so the mark can disappear; it prints wherever notes print; a second change adds a second line and nothing says which one the mark uses; it has no link to the earlier row.
*   **Reason for Rejection**: It is not reliable enough for clinical history, and it would need to be replaced when a real schema change comes.

### Alternative G: Store change history or a link to the replaced row in new database fields

*   **Pros**: Exact data.
*   **Cons**: Needs a Room migration, but schema v6 is frozen without clinical sign-off; the schema files are level-3 paths.
*   **Reason for Rejection**: Not in Stage 1 because of the frozen schema. It is the planned route for Stage 2.

### Alternative H: Keep the full-screen editor for every action

*   **Pros**: No change to state, route or tests.
*   **Cons**: It is the cause of the refresh. Every tap rebuilds the Meds tab.
*   **Reason for Rejection**: It does not solve the main problem.

### Alternative I: Course length, undo for stop and change, a shared Enter helper and a "3 days ago" shortcut in Stage 1

*   **Pros**: More complete workflow in the first release.
*   **Cons**: Course length changes the active/stopped rule in at least eight places (the Meds tab, the header count, the stop symbol, the picker, the clinical summary, the print output, the editor's status control and tests). Undo for stop and change needs a new undo command, new offer types, new notices and changes to the undo coordinator. The shared Enter helper adds an abstraction with one user. The "3 days ago" shortcut has no documented workflow behind it and the existing date field already has a Today shortcut.
*   **Reason for Rejection**: Rejected by author on the reviewers' advice: the cost and the level-3 risk are higher than the value in Stage 1. They are Stage 2 items.

## Future Improvements

*   **Stage 2:** favourites, dose/frequency variants with a default, and the medication settings screen. A different administration route is a separate drug, not a variant.
*   **Change history** with a schema change that needs clinical sign-off: a record of each dose change and a link to the replaced row, with undo for stop and change.
*   **Course length** for short courses with an automatic end, once the active/stopped rule can be changed in one shared place.
*   **Shared Enter-to-next helper** for the other editors, adopted one editor at a time.
*   **Finer failure messages** such as "medication was removed".

--------------------------------------------------------------------------------

## Detailed Implementation Plan

### Order of work (every phase leaves the app building and the tests running)

1.  **Phase 0: mock.** Build `docs/mocks/medication-section.html` and get the author's approval, recorded as a line in this document's Decisions. No Kotlin changes before that.
2.  **Phase 1: add the new UI beside the old.** Create `MedicationForm` and `MedicationSheet`. Nothing uses them yet, so the app is unchanged. Add form-level UI tests. Verify: build and unit tests.
3.  **Phase 2: switch over in one change.** Change the state so starting a draft does not change the route, host the sheet in `PatientHubScreen`, point the picker, the rows and the "Prescribe" button at the sheet, delete the full-screen editor, `AncRoute.MedicationEditor`, its route branch and `dispatchMedicationDraft`, and rewrite the tests that asserted the old route. This must be one change because the route, its host and its tests depend on each other. Verify: build, unit tests, instrumented tests.
4.  **Phase 3: picker and list polish.** Collapse the picker, add the "Show past" toggle, the disabled-state rule and the Enter-to-next and keyboard checks, and clean up strings. Verify: build, unit tests, instrumented tests at 360dp.

### Target Files to Create/Modify/Delete

#### 1. `docs/mocks/medication-section.html` (Create)

*   **Type / Scope**: Static HTML and CSS with a small script for the clicks. Mobile-first, drawn at 360dp width. Not shipped in the app.
*   **Proposed Changes**: Clickable mock of the review list, the collapsed picker, "Show past", the sheet (add, stop, edit), the date field, the keyboard-open state and the failed-save message.

#### 2. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt` (Create)

*   **Type / Scope**: Shared form composable.
*   **Proposed Changes**: Extract the fields from the old editor: name, dose, route (read-only on an existing row), frequency (fixed list plus custom), Active/Stopped control, notes, and the date field built on `AncDatePickerField`. Emits only `AncEvent.MedicationDraftChanged`. Gives every field `ImeAction.Next` (the last `Done`) with standard focus requesters and a bring-into-view request on focus. No business logic.

#### 3. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt` (Create)

*   **Type / Scope**: Bottom sheet host.
*   **Proposed Changes**: A `ModalBottomSheet` shown while `medicationDraft` exists for the patient. Contains `MedicationForm`, Cancel, Save and "Save and add another". Applies `imePadding`. Shows the save phase and, on a rejected save, the existing `SaveUiError` message. Dismissing sends `AncEvent.CancelEditor`.

#### 4. `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt` (Modify)

*   **Type / Scope**: State, events and reducer. Level-3 path.
*   **Proposed Changes**: Delete `AncRoute.MedicationEditor` and its uses. Add `prefill` to `StartMedicationEditor` and build the draft in one step with the `original`/`values` rules in Technical Plan Section 1. `startMedicationEditor` no longer sets the route. Back with a draft open runs the existing cancel and discard logic.

#### 5. `app/src/main/java/com/sherifradwan/ancnative/state/EditorHooks.kt` (Modify)

*   **Type / Scope**: Shared editor hook. Level-3 path.
*   **Proposed Changes**: `MedicationEditorHook.closeEditor` only clears `medicationDraft`. Remove the branch that reset the route.

#### 6. `app/src/main/java/com/sherifradwan/ancnative/state/AncViewModel.kt` (Verify)

*   **Type / Scope**: Save orchestration. Level-3 path.
*   **Proposed Changes**: None expected. The save path, "Save and add another" and the existing add-undo stay. Confirm it has no reference to the deleted route.

#### 7. `app/src/main/java/com/sherifradwan/ancnative/state/DraftMapping.kt` and `DraftRecovery.kt` (Verify)

*   **Type / Scope**: Draft autosave and recovery.
*   **Proposed Changes**: They contain no route reference, so none expected. A test covers recovery of add, stop and edit drafts (Verification Plan).

#### 8. `app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt` (Modify)

*   **Type / Scope**: Route host.
*   **Proposed Changes**: Remove the `AncRoute.MedicationEditor` branch and its import.

#### 9. `app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt` (Delete)

*   **Type / Scope**: Full-screen editor.
*   **Proposed Changes**: Deleted after its fields move to `MedicationForm`. Anything else in the file that other code uses (helpers, constants, test tags) moves to `MedicationForm.kt` or `MedicationSheet.kt` first.

#### 10. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubScreen.kt` (Modify)

*   **Type / Scope**: Screen orchestrator.
*   **Proposed Changes**: Host `MedicationSheet` above the tab pager. Pass `medicationDraft` and the save phase from `AncState` to `MedsTab`.

#### 11. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt` (Modify)

*   **Type / Scope**: Tab body.
*   **Proposed Changes**: Take the draft and the save phase as parameters. Show the collapsed picker, the active list, and the "Show past" toggle. Row stop, row edit and the "Prescribe" button start the sheet. Disable every entry point while a save runs or a draft is open. Remove the call to `dispatchMedicationDraft`.

#### 12. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt` (Modify)

*   **Type / Scope**: Picker.
*   **Proposed Changes**: One collapsed row that expands to the search box and a height-limited, self-scrolling list. Keep Common, Recent and `filterMedicationTiles`. A tile starts the draft with its prefill. Delete `dispatchMedicationDraft`.

#### 13. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/MedicationTiles.kt` (Modify)

*   **Type / Scope**: Tile model.
*   **Proposed Changes**: Add `frequency` (from `MedicationCatalogItem.frequency`) so it reaches the prefill. Custom-choice tiles use a blank frequency.

#### 14. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationCard.kt` (Modify)

*   **Type / Scope**: Row card.
*   **Proposed Changes**: The stop symbol saves at once and shows the Undo bar (see Technical Plan, Stop). Compact layout at 360dp: name, dose and frequency are never truncated.

#### 15. `app/src/main/res/values/strings.xml` (Modify)

*   **Type / Scope**: Strings.
*   **Proposed Changes**: Add strings for the collapsed picker, "Show past", the date field labels (Start date, Stop date). Remove strings used only by the deleted editor.

### Files that Stage 1 must not change

`app/schemas/*`, `RecordsDatabase.kt`, `RecordEntities.kt`, `RecordsDao.kt`, `RecordsRepository.kt`, `ClinicalCommands.kt`, `UndoCoordinator.kt`, `ClinicalModels.kt`, the clinical summary and print generators. The Verification Plan checks this with a diff.

### Tests (Create/Modify)

*   `app/src/test/java/com/sherifradwan/ancnative/state/AncStateTest.kt`, `AncDeleteUndoStateTest.kt`, `DraftRecoveryTest.kt`, `MedicationStatusDraftTest.kt` (Modify): replace asserts on `AncRoute.MedicationEditor` with asserts that the route stays on the workspace and `medicationDraft` is set.
*   `app/src/test/java/com/sherifradwan/ancnative/state/MedicationSheetStateTest.kt` (Create): the `original` and `values` rules for add, stop, edit and custom; the draft is dirty on open for add and stop; Back closes the draft and not the workspace; the editor hook reports open and closes the draft; start is ignored while a save runs.
*   `app/src/androidTest/java/com/sherifradwan/ancnative/MedicationEntryDesignTest.kt`, `DeleteUndoUiTest.kt`, `AncWorkflowTest.kt` (Modify): drive the sheet instead of the screen.
*   `app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt` (Create): scroll position, search text and "Show past" survive add, stop and edit; every entry point is disabled during a save; Enter moves focus through the form and the focused field is visible with the keyboard open; "Save and add another" does not lose focus or the keyboard; a rejected save shows the existing message and keeps the values; recovery of add, stop and edit drafts reopens the sheet on the Meds tab; layout at 360dp.

### Verification Plan

*   **Compilation/Build**: `./gradlew :app:assembleDebug`
*   **Unit Tests**: `./gradlew :app:testDebugUnitTest`
*   **Static Analysis & Linting**: `./gradlew :app:lintDebug` (the project has no detekt or ktlint configuration)
*   **E2E/Integration**: `.agents/scripts/run_emulator_all_tests.sh` for the instrumented tests. Manual check of the three CUJs on a 360dp device.
*   **Schema and scope check**: `git diff --stat` shows no change in the "must not change" list and no change under `app/schemas`.
*   **Coverage**: every new branch in the `StartMedicationEditor` handling and in the entry-point disabling has a unit or UI test.
*   **Review**: the Stage 1 change touches level-3 files, so each phase needs a frontier-tier review and an independent test review before it is accepted. The review team for this project is a Claude Sonnet subagent plus the models available at that time (Gemini 3.8 Flash through `agy`, GLM through opencode), with the builder and the test designer from different vendors. Check each model with a one-line test first.

--------------------------------------------------------------------------------

<!-- Provenance: Indicates initial structural drafting via EGM. -->

*Drafted with EGM*
