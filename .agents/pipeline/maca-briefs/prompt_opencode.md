You are the Test Designer / Test Auditor in a Multi-Agent Collaborative Audit (MACA) workflow for an Android EMR application (anc-emr-android-native).

# Task
Update `app/src/androidTest/java/com/sherifradwan/ancnative/LabEntryDesignTest.kt` to test the new story-like Single Laboratory Investigation screen (`LabEditor.kt`).

# Context & Requirements
1. The clunky accordion toggles and obsolete hardcoded presets ("Negative", "Non-reactive", "Normal", "Positive") have been eliminated.
2. In the new design, the clinical story card directly displays:
   - `labTestTitle`: The test name (e.g. "Hemoglobin", "Urine Protein")
   - `labStoryDisplay`: Surface containing `labStoryValue` and `labResultFlag`
   - `labResult`: The decimal input field for result value
   - `labUnit`: Text input for unit (e.g. "g/dL")
   - `labRefRange`: Text input for reference range (e.g. "11-14")
   - `labAbnormal`: Switch indicating whether result is abnormal (auto-flagged from reference range)
   - `labNotes`: Text input for notes
3. For dipstick tests (category "Urinalysis" or name containing protein/glucose/ketone):
   - Qualitative choice buttons (`labResultPreset-0` for "Normal", `labResultPreset-1` for "Trace", `labResultPreset-2` for "+", etc.) are displayed.
   - Selecting "Trace" sets result to "Trace" and auto-flags abnormal.
4. Bottom bar contains:
   - `cancelLab`
   - `addAnotherLab`
   - `saveLab`
5. Batch info bar:
   - Tag `labBatchChange`
6. Test oracles to implement:
   - `t300_newLabShowsTestTitleResultUnitRefRangeAndAddTests`: verifies that a new lab shows `labResult`, `labUnit`, `labRefRange`, and add-tests list.
   - `t301_loadedTestShowsTitleAndDirectStoryFields`: loaded test (e.g. Hemoglobin) shows `labTestTitle` ("Hemoglobin"), `labResult`, `labUnit`, `labRefRange`.
   - `t302_resultBelowRangeIsAutoFlaggedAbnormal`: entering a result outside reference range (e.g. "9" for "11-14") automatically turns `labAbnormal` switch ON.
   - `t303_dipstickQualitativePresetSetsResultAndFlagsAbnormal`: loaded dipstick test (e.g. Urine Protein) shows qualitative choices; clicking "Trace" (`labResultPreset-1`) sets result to "Trace" and flags abnormal.
   - `t304_notesFieldPersistsToDraft`: typing notes in `labNotes` updates `state.labDraft?.values?.notes`.
   - `t305_bottomBarActionsDisplayed`: `saveLab`, `cancelLab`, `addAnotherLab` are all displayed.

# Output Format
Output ONLY the complete, compile-ready Kotlin source code for `LabEntryDesignTest.kt` inside a single ```kotlin ... ``` code fence.
