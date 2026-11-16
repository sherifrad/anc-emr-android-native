Both deliverables for **Slice 16 (Lab editor and lab batch)** are complete, with no production files touched.

## File paths

- [mock_s16_lab_editor.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/mocks/mock_s16_lab_editor.html)
- [slice_16_lab_editor.md](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/slice_16_lab_editor.md)

## What the mock shows

Using `FieldFamilies.background()` (sand: `#F1EDE4` / `#594A2C` light, `#36301F` / `#E0CFA6` dark) to match [LabReportItem.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/LabReportItem.kt), with `360px` **Before** (faithful to [LabEditor.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/LabEditor.kt) and [LabBatchScreen.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/LabBatchScreen.kt)) and **After** frames on real sample data (`Mona Ragab` / `منى رجب`, `27+2 wks`, `G4 P1 A2`, `A+`, `Hb 11.2`, `EDD 11/02/2027`):

1. **Single lab entry (`LabEditor.kt`)**:
   - Replaces the duplicated subtitle and `ClinicalPatientCard(EDITOR_SUMMARY)` (which omitted `A2` and showed `Normal Risk` and `11 sp` pills) with one sand `LabPatientBanner` and a unified `BatchInfoBar` formatted as `27+2 wks` with a plain `Change` button.
   - Replaces the `36 sp` pink `surfaceVariant` box and `11 sp` `Normal ✓` pill with a compact sand `LabReadoutStrip` + `BandChip`, moves field labels to `12 sp` `FormFieldLabel`, collapses 5 stacked `56 dp` dipstick `BigChoiceButton`s into one `48 dp` `RecordFilterChip` row, collapses the 28-item unfiltered preset list to 5 + `Show all 28`, removes the `11 sp` AES-256 Lock footer, and switches the squeezed 3-button bottom row to a 2-row `MedicationSheet`-style footer.
2. **Lab batch: Add (`LabBatchScreen.kt` `AddTab`)**:
   - Places `Choose` and `+ Add N` (`48 dp` height, `10 dp` radius) in a 50/50 row below full-width panel titles so long panel names (`First Trimester Routine Booking`, `Urine Routine & Microscopic`) do not crush into narrow lines.
   - Replaces the `ActionRose` stripe and ALL-CAPS `ADD A SINGLE TEST` / `OFTEN ADDED` labels with `RecordSectionCard`, wraps `Often added` chips in `FlowRow`, and adds an empty search state.
3. **Lab batch: Choose panel & Metadata dialogs (`ChoosePanelDialog`, `showMetadataDialog`)**:
   - Restyles `ChoosePanelDialog` onto `16 dp` `surface` with `48 dp` checklist rows, `primary` `"Save this selection as a panel…"`, `10 dp` buttons, and preserved `"CBC · 6 elements"` / `"Add 5 selected"` strings for [LabBatchUiTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/LabBatchUiTest.kt).
   - Upgrades `Batch details` with `ClinicalDateField` (calendar + `Today`) and `labNamePresets` while keeping `labBatchDate` and `labBatchLabName` tags on the `OutlinedTextField`s.
4. **Lab batch: Enter results (`EnterTab`)**:
   - Restyles `BatchSegmentedProgress` (`10 dp`, `primary`), raises `labBatchDeleteItem` from `32 dp` to `48 dp`, shows quantitative results with `LabReadoutStrip` + `BandChip` and external `12 sp` labels, updates `QualitativeChoiceButton` to `primaryContainer` + `1.5 dp` `primary` border, and drops `◀`/`▶` triangles and double `+ +`.
5. **Lab batch: Review (`ReviewTab`)**:
   - Uses `RecordSectionCard` per group with `LabReportItem`-aligned rows, `StatusWord` (`Normal`, `Pending`, `Abnormal`), `48 dp` `Edit` actions, and a sand quick-action card around `labBatchMarkNormal`.
6. **Dark theme (`منى رجب`) + Contrast table + Compose mapping**:
   - Includes a `360px` dark `EnterTab` frame showing Arabic RTL name alignment via [PatientNameText.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/components/PatientNameText.kt), a Python-computed WCAG 2.1 contrast table (all text pairs `>= 6.46:1`, borders `>= 4.55:1`), and exact Compose token/component mappings.

## Decisions for the owner to confirm

1. **Shared `FieldBackgroundBg` `LabPatientBanner` and unified `BatchInfoBar`**: Replace `ClinicalPatientCard(EDITOR_SUMMARY)`, duplicated header subtitles, and `LabEditor`'s private `BatchInfoBar` with one shared `LabPatientBanner` (`27+2 wks`, `G4 P1 A2`, `A+`, Moderate/High risk only) and `DesignSystem.BatchInfoBar(..., useRecordStyle = true)`. **Recommend: accept.**
2. **Compact sand readout strip, 1-row `RecordFilterChip` dipstick choices, and collapsed `labPreset` list (`PRESAVED_COLLAPSE_AFTER = 5`)**: Keep `labStoryDisplay`/`labStoryValue`/`labResultFlag` as a compact sand row, move dipstick choices to one `FlowRow` of `48 dp` chips, and style the `Show all 28` `TextButton` in level-3 [ClinicalPresets.kt](file:///Users/sherifradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalPresets.kt) (`useRecordStyle = true` display-only change). **Recommend: accept.**
3. **2-row `MedicationSheet`-style bottom bar in `LabEditor`**: Place `SheetCancelSaveRow` (`cancelLab` / `saveLab`) above full-width `Save and add another` (`addAnotherLab`) so `Save and next test` never clips at `1.3x` or `2.0x` scale on `360 dp`. **Recommend: accept.**
4. **Stacked `48 dp` panel actions and upgraded batch metadata dialog in `LabBatchScreen`**: Move `Choose` and `+ Add N` below panel titles in a 50/50 row, wrap `Often added` in `FlowRow`, and add `ClinicalDateField` + `labNamePresets` to `Batch details` (keeping `labBatchDate` and `labBatchLabName` tags). **Recommend: accept.**
5. **Sand/Primary styling across `EnterTab`/`ReviewTab`, `48 dp` delete, Lock footer removal, and `GenericEditorErrorCard` in `LabBatchScreen`**: Drop `ActionRose`, `11 sp` pills, triangles, and the `11 sp` AES-256 Lock footer, raise `labBatchDeleteItem` to `48 dp`, and surface `LabBatchDraft.errors` via `GenericEditorErrorCard` (keeping `"CBC · 6 elements"` for `LabBatchUiTest`). **Recommend: accept.**

## Delivery Gate summary

- **R-02 PASS**: `0` em dashes (`U+2014`) and `0` double-hyphen prose dashes across both files (verified by Python scan after final edit).
- **R-03 / R-32 / R-39 PASS**: `360px` responsive frames with wrapping chips/buttons, `>= 48px` mock tap targets, `<html lang="en">`, landmark `<main>`, heading order `h1 -> h2`, `cursor: pointer`, and `:focus-visible` outlines.
- **R-17 / R-38 PASS**: Only Mona Ragab's sample (`Mona Ragab` / `منى رجب`, `27+2 wks`, `G4 P1 A2`, `A+`, `Hb 11.2`, `EDD 11/02/2027`), app strings, `ClinicalCatalogRepository` names, and bracketed placeholders (`[report date]`, `[lab name]`, `[range]`, `[Trace]`).
- **R-25 PASS**: Computed WCAG 2.1 ratios via Python: light text `>= 6.46:1`, dark text `>= 7.53:1`, `outline` borders `4.55:1` light / `5.42:1` dark.
- **R-35 PASS (static deliverable)**: HTML tag balance check passed cleanly (`stack=[]`) and `git status --short` confirmed no production code or other files were modified; runtime/Gradle/emulator execution was skipped per safety rules.
