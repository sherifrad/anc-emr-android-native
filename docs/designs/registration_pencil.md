# Pencil and Incomplete chip open the registration flow

* **Date:** 2026-11-13
* **Status:** built, in review. Builder: alphacode. Reviewers: opencode GLM and DeepSeek. Orchestrator: Claude.

## Problem

After the registration flow merge, only a new patient enters the new step-by-step flow. For a stored patient whose registration is unfinished, the header pencil, the Overview edit buttons and "Edit demographics" still opened the old single form, and the Incomplete chip on the patient list was display only.

## Rule

One function decides, `editPatientEvent(patient, editorSessionId, newPregnancyId)` in `state/PatientEditEntry.kt`:

| Patient | Result |
|---|---|
| Archived | nothing (null) |
| Registration unfinished (`isRegistrationIncomplete`: identity, dating or obstetric step not done) | the flow starts at the first unfinished step (`firstNotDoneStep`) |
| Registration finished | the plain patient editor, as before |

Callers: the header pencil (`PatientHubScreen`), the Overview tab edit buttons (`OverviewTab`), "Edit demographics" in the patient actions sheet, and the Incomplete chip on the directory card, which is now tappable with a 48 dp target and the label "Continue registration".

The Clinical attention dialog keeps the old path on purpose: it opens the editor, changes one field and saves in the same tap, so it must never start a flow.

## Not changed

No storage, schema, print or summary file. No change to the flow reducer or driver. Delivery outcome fields are not touched by this change.

## Open point for the owner's phone check

The pencil on an incomplete patient opens the first unfinished step, not always the identity step. To change the name of such a patient, press Back inside the flow to reach Identity. If the owner prefers the pencil to always open Identity, change the start step in `editPatientEvent` for that caller.

## Tests

`PatientEditEntryTest` (unit): complete, each of the three unfinished steps, archived, and both events reduced to state. Emulator test: `RegistrationPencilUiTest`.
