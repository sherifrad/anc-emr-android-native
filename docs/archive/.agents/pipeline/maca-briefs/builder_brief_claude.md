# MACA Implementation Brief: Single Laboratory Investigation Redesign

## Objective
Refactor `app/src/main/java/com/sherifradwan/ancnative/ui/LabEditor.kt` to match the approved story design in `.agents/pipeline/design/rev-b/single-lab.html`.

## Requirements
1. **Correlate with Batch Story Appearance**:
   - Use `BatchInfoBar` (Date · GA, Lab Name, Change ▾).
   - Display a clean clinical story card:
     - Uppercase small category tag (e.g. `HEMATOLOGY`, `URINALYSIS`)
     - Large bold test name title (`labTestTitle`)
     - Summary subtitle: `<unit> · ref <range> (from report)`
     - Prominent clinical story display box showing the formatted entered value (e.g. `10.8 g/dL`) with diagnostic badge (`Low ▼`, `Normal ✓`, `High ▲` derived from `labResultFlag`).
     - Core inputs: Result (`labResult`), Unit (`labUnit`), Reference Range (`labRefRange`).
     - Abnormal switch (`labAbnormal`) auto-flagged from reference range.
     - Optional notes field (`labNotes`).
2. **Eliminate Obsolete Preset Clutter**:
   - Do NOT display visible hardcoded preset buttons for "Negative", "Non-reactive", "Normal", "Positive".
   - For dipstick urinalysis (e.g. Urine Protein), provide big qualitative choice buttons (`Normal`, `Trace`, `+`, `++`, `+++`) matching Slice 4 of the batch flow.
3. **Preserve Static Check Contracts (`check_r4d.py`)**:
   - `labResultFlag(` and `resolveLabPanel(` must be called.
   - `EditorPage(bottomBar = ...)` must be used with `cancelLab`, `addAnotherLab`, and `saveLab`.
   - Every tag in `TAGS` must be present:
     `["labNameField", "labDate", "labName", "labResult", "labUnit", "labRefRange", "labAbnormal", "labNotes", "allLabTestsChip", "labGroupChip-", "labPreset", "labResultPreset", "labCategoryChoice", "saveToLabCodexBanner", "saveToLabCodexButton", "cancelLab", "addAnotherLab", "saveLab"]`
   - No visible "Codex" text in user-facing strings.
4. **Clean Code & Build Gate**:
   - Must compile cleanly with `./gradlew --offline :app:compileDebugKotlin`.
