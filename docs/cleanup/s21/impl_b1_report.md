# Slice 21 Cleanup: Batch 1 Implementation Report

Branch: `redesign-2026-11-15`
Scope: Batch 1 pure unused imports and dead private/unreferenced members under `app/src/main/java/com/sherifradwan/ancnative/ui/`.

## Items Done (Verified Removed)

All 9 Batch 1 target files are verified in the clean post-edit state (`Components.kt` edited directly; the other 8 Batch 1 target files were already updated on disk at 15:37-15:38 and verified absent by `rg` and file inspection):

1. `app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt` (L3 pure unused imports):
   - Removed unused imports `Spacer`, `width`, `OutlinedButton` (3 imports).
2. `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalBadges.kt` (L3 pure unused imports):
   - Removed unused imports `Column`, `fillMaxWidth`, `Color`, `luminance` (4 imports).
3. `app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt` (L3 pure unused imports):
   - Removed 22 unused imports: `background`, `clickable`, `FlowRow`, `height`, `RoundedCornerShape`, `AccountCircle`, `Add`, `Edit`, `EditNote`, `MoreVert`, `Unarchive`, `Warning`, `CardDefaults`, `HorizontalDivider`, `ModalBottomSheet`, `OutlinedButton`, `Surface`, `clip`, `Color`, `lerp`, `luminance`, `SevereBpAttention`.
4. `app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt`:
   - Removed 6 unused imports: `background`, `clickable`, `fillMaxSize`, `height`, `AccountCircle`, `TextAlign`.
5. `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryCard.kt`:
   - Removed unused import `Text`.
6. `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryMetricsRow.kt`:
   - Removed unused import `Color`.
7. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/VisitsTab.kt`:
   - Removed 3 unused imports: `Row`, `fillMaxWidth`, `padding`.
8. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt`:
   - Removed unreferenced `AttentionColors` getters `containerColor`, `textColor`, `iconColor`, `borderColor` (C1-11, NEW-01), `typealias ClinicalAttentionColors`, and `@Composable fun clinicalAttentionColors`.
   - Verified 0 usages in `app/src` via `rg -n '\bAttentionColors\b' app/src`, `rg -n '\.(containerColor|textColor|borderColor|iconColor)\b' app/src` (only hit is `normal.containerColor` on M3 `AssistChipColors` in `MedicationTapToAddSection.kt:391`), and `rg -n '\b(ClinicalAttentionColors|clinicalAttentionColors)\b' app/src` (0 hits after removal).
9. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt`:
   - Removed unreferenced property `CompositeTestGroup.summaryPillText` (C1-12). Verified 0 hits in `app/src` via `rg -n '\bsummaryPillText\b' app/src`.

## Items Skipped (With Evidence / Reason)

1. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt` (`pickerFilterChipColors`, `pickerFilterChipBorder` - Team A `NEW-02`):
   - **Skipped (live callers exist)**.
   - `rg -n '\bpickerFilterChip(Colors|Border)\b' app/src` found active call sites at:
     - `MedicationTapToAddSection.kt:262`: `colors = pickerFilterChipColors(state.source == PICKER_SOURCE_COMMON, dimmed),`
     - `MedicationTapToAddSection.kt:263`: `border = pickerFilterChipBorder(state.source == PICKER_SOURCE_COMMON, dimmed),`
     - `MedicationTapToAddSection.kt:271`: `colors = pickerFilterChipColors(state.source == PICKER_SOURCE_RECENT, dimmed),`
     - `MedicationTapToAddSection.kt:272`: `border = pickerFilterChipBorder(state.source == PICKER_SOURCE_RECENT, dimmed),`
   - Their callers are the two `FilterChip` calls at lines 258 and 267, which are out of scope for Batch 1.
2. `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt:10-11` (`GestationalAgeEngine`, `PreviousPregnancyFormRules` listed in `docs/cleanup/s21/tool_unused.txt`):
   - **Skipped (protected file)**. Matches `*Generator*` hard-rule exclusion (and `ui/report/*` level 3).
3. `getValue` / `setValue` `unused-import` candidates in `docs/cleanup/s21/tool_unused.txt` under `ui/` (29 files):
   - **Skipped (in use via Kotlin `by` property delegates)**.
   - Body scan with `rg -n '\bby\b'` across all listed `ui/` files confirmed active `val ... by` / `var ... by` property delegation in every file (`AddAttachmentDialog.kt`, `AncApp.kt`, `AncDatePicker.kt`, `AppLockScreen.kt`, `CatalogSettingsSections.kt`, `ClinicalPresets.kt`, `InAppCameraDialog.kt`, `LabBatchScreen.kt`, `LabEditor.kt`, `PatientEditor.kt`, `PatientListScreen.kt`, `SecuritySettingsDialog.kt`, `UltrasoundEditor.kt`, `UltrasoundPhotoViewerDialog.kt`, `VisitEditor.kt`, `ZoomableImage.kt`, `PatientHubHeader.kt`, `PatientHubScreen.kt`, `AttachmentsSectionCard.kt`, `ClinicalAttentionBanner.kt`, `DeliveryPlanSheet.kt`, `EditorSheetParts.kt` [`getValue` only], `LabReportItem.kt`, `MedicationForm.kt`, `MedicationSheet.kt`, `MedicationStopBar.kt` [`getValue` only], `MedicationTapToAddSection.kt`, `MedsTab.kt`, `PatientTwoPageReportDialog.kt`).
4. `state/`, `storage/`, `model/`, `security/` entries in `docs/cleanup/s21/tool_unused.txt` (lines 1-21):
   - **Skipped (protected directories per hard rules)**.

## Level 3 Report-Only Items (Not Removed)

- `app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt:619` (`internal fun EditorActions`):
  - Verified 0 references outside its declaration via `rg -n '\bEditorActions\b' app/src` (`PatientEditor.kt:619` only). Retained untouched per Level 3 report-only rule.
- `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalPresets.kt:67-77` (`object ClinicalPresets`: `commonBloodPressures`, `commonVisitFindings`):
  - Verified 0 references outside its declaration via `rg -n '\b(ClinicalPresets|commonBloodPressures|commonVisitFindings)\b' app/src` (`ClinicalPresets.kt:67,68,70` only). Retained untouched per Level 3 report-only rule.

## Commands and Results

1. Symbol and delegate verification (`rg`):
   - `rg -n '\bby\b' ...` across the 25 additional `ui/` files with `getValue`/`setValue` in `tool_unused.txt` (plus direct reads of `AncApp.kt`, `PatientListScreen.kt`, `ClinicalAttentionBanner.kt`, `MedicationTapToAddSection.kt`): exit 0, confirmed `by` delegate usage in all 29 files.
   - `rg -n "\b${sym}\b" app/src` for `containerColor`, `textColor`, `borderColor`, `iconColor`, `ClinicalAttentionColors`, `clinicalAttentionColors`, `summaryPillText`, `pickerFilterChipColors`, `pickerFilterChipBorder`, `EditorActions`, `ClinicalPresets`, `commonBloodPressures`, `commonVisitFindings`: exit 0.
   - `rg -n '\bAttentionColors\b' app/src` and `rg -n '\.(containerColor|textColor|borderColor|iconColor)\b' app/src`: exit 0, confirmed 0 usages on `AttentionColors`.
   - Post-edit `rg` check across all 9 target files and `MedicationTapToAddSection.kt`: exit 0, confirmed all 9 target files clean and `pickerFilterChipColors`/`pickerFilterChipBorder` intact.
2. Gradle compile verification:
   - `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q`: exit 0.
   - `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin --console=plain`: exit 0, `BUILD SUCCESSFUL in 11s` (`35 actionable tasks: 3 executed, 32 up-to-date`; `:app:compileDebugKotlin`, `:app:compileDebugUnitTestKotlin`, and `:app:compileDebugAndroidTestKotlin` all succeeded with 0 errors).
