# MACA Test Designer Brief: Single Laboratory Investigation Test Suite

## Context
The single laboratory investigation entry screen (`LabEditor.kt`) is being redesigned to match the approved story design (`rev-b/single-lab.html`).
- The clunky accordion toggles ("Edit test details") and obsolete hardcoded presets ("Negative", "Non-reactive", "Normal", "Positive") are removed from the main view.
- Direct clinical fields are present: `labTestTitle`, `labResult`, `labUnit`, `labRefRange`, `labAbnormal`, `labNotes`.
- Urinalysis / Dipstick tests render `QualitativeChoiceButton`s (`Normal`, `Trace`, `+`, `++`, `+++`).
- Bottom bar retains `saveLab`, `cancelLab`, `addAnotherLab`.

## Task for OpenCode
Review `app/src/androidTest/java/com/sherifradwan/ancnative/LabEntryDesignTest.kt` and output the updated test class that verifies the new story design oracles without obsolete preset assertions.
Ensure:
1. `t300`: verifies story card displays `labTestTitle`, `labResult`, `labUnit`, `labRefRange`.
2. `t301`: verifies loaded test shows title with unit and reference range.
3. `t304`: entering a result below reference range (e.g. "9" for range "11-14") automatically sets `labAbnormal` to ON.
4. `t305`: typing notes in `labNotes` reaches `state.labDraft.values.notes`.
5. `t306`: verifies action bar buttons `saveLab`, `cancelLab`, `addAnotherLab` are displayed.
6. `t307`: verifies dipstick qualitative choice buttons (e.g. Trace) set the result and auto-flag abnormal.
