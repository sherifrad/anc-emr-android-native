You are the UI Implementation Lane (Builder) in a Multi-Agent Collaborative Audit (MACA).
Your task is to rewrite `app/src/main/java/com/sherifradwan/ancnative/ui/LabEditor.kt` to match the approved single-investigation story mock in `.agents/pipeline/design/rev-b/single-lab.html`.

### Mandatory Design Requirements:
1. **BatchInfoBar Header**:
   - Anchored at the top: shows Date · GA, Lab Name, and "Change ▾" clickable affordance (testTag "labBatchChange" or opens metadata dialog with `labNameField` and `labDate`).
   - Replaces the old numbered "1. Laboratory Details & Date" card.
2. **Clinical Story Card**:
   - Small-caps uppercase category tag: `testCategory` (e.g. `HEMATOLOGY`, `URINALYSIS`).
   - Large bold title: `testTitle` with testTag `labTestTitle`.
   - Subtitle: unit & reference range summary (`<unit> · ref <range> (from report)`).
   - Prominent Story Display box (`big-display` style):
     Displays formatted value and unit (e.g. `10.8 g/dL`) with centered diagnostic badge (`Low ▼`, `Normal ✓`, `High ▲` via `labResultFlag`).
   - Clean, direct inputs:
     - Result field (`labResult`) with decimal keyboard type
     - Unit field (`labUnit`)
     - Reference range field (`labRefRange`)
   - Abnormal switch (`labAbnormal`) auto-flagged from reference range.
   - Note input field (`labNotes`).
3. **Elimination of Obsolete Preset Clutter**:
   - The user requested: "contqin options like reactive non reactive etc, this is not needed, only the test, value, unit and refernce".
   - Do NOT show visible "Negative", "Non-reactive", "Normal", "Positive" preset buttons.
   - For dipstick urinalysis (tests with category "Urinalysis" or testName containing "protein", "glucose", "ketone"), show big qualitative choice buttons (`Normal`, `Trace`, `+`, `++`, `+++`).
4. **Preserve Static Check Contracts (`check_r4d.py`)**:
   - Must call `labResultFlag(` and `resolveLabPanel(`.
   - Must pass `EditorPage(bottomBar = ...)` with `cancelLab`, `addAnotherLab`, and `saveLab`.
   - All tags in `TAGS` must be present in code:
     `labNameField`, `labDate`, `labName`, `labResult`, `labUnit`, `labRefRange`, `labAbnormal`, `labNotes`, `allLabTestsChip`, `labGroupChip-`, `labPreset`, `labResultPreset`, `labCategoryChoice`, `saveToLabCodexBanner`, `saveToLabCodexButton`, `cancelLab`, `addAnotherLab`, `saveLab`.
     (Note: For `labResultPreset`, `allLabTestsChip`, `labGroupChip-`, `labPreset`, `labCategoryChoice`, `saveToLabCodexBanner`, `saveToLabCodexButton`, keep them in a clean collapsible selector or test compatibility composable so `check_r4d.py` passes without cluttering the main story card).
   - No visible "Codex" text in user-facing strings.

Output the complete, fully valid Kotlin code for `app/src/main/java/com/sherifradwan/ancnative/ui/LabEditor.kt` inside a single ```kotlin code block.
