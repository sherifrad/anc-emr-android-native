# Slice 21 Cleanup — Team A Revised Report (Round 2 Checker: A2)

Audited against branch `redesign-2026-11-15` across `app/src/main/java`, `app/src/main/res`, `app/src/test`, `app/src/androidTest`, `app/src/debug`, and `AndroidManifest.xml`.
Every row from `findings_A1.md` was independently verified with adversarial grep/symbol scans to prevent build breaks or regressions.

---

## Round 2 Checker Verification Summary

- **Total rows audited from `findings_A1.md`**: 76 rows (62 candidate rows + 14 protected report-only rows)
- **Surviving rows (AGREE)**: **71 rows** (57 actionable/L3 candidate rows + 14 protected report-only rows)
- **Rejected rows (REJECT)**: **4 rows** (C2-12, C2-13, C2-14, C3-07 — all have active callers or violate architectural invariants)
- **Downgraded rows (DOWNGRADE)**: **1 row** (C4-11 — duplicate asset requiring AAPT2 packaging proof)
- **New rows added (NEW)**: **2 rows** (NEW-01: 3 unreferenced `AttentionColors` getter aliases; NEW-02: 2 unreferenced private chip helpers in `MedicationTapToAddSection.kt`)

---

## Adversarial Verdict on Teammate Findings (`findings_A1.md`)

| ID | Finding in `findings_A1.md` | Checker Verdict | Adversarial Evidence & Justification |
|---|---|---|---|
| **C1-01** | `ui/AncApp.kt`: 3 unused imports | **AGREE** | `git grep` confirms 0 body references in `AncApp.kt` for `Spacer`, `width`, `OutlinedButton`. L3 pure unused import. |
| **C1-02** | `ui/ClinicalBadges.kt`: 4 unused imports | **AGREE** | 0 body references in file for `Column`, `fillMaxWidth`, `Color`, `luminance`. L3 pure unused import. |
| **C1-03** | `ui/PatientListScreen.kt`: 22 unused imports | **AGREE** | 0 body references for all 22 symbols in file. L3 pure unused import. |
| **C1-04** | `ui/Components.kt`: 6 unused imports | **AGREE** | 0 body references for `background`, `clickable`, `fillMaxSize`, `height`, `AccountCircle`, `TextAlign`. |
| **C1-05** | `ui/patientdirectory/PatientDirectoryCard.kt`: unused `Text` | **AGREE** | 0 Compose `Text` references in body. |
| **C1-06** | `ui/patientdirectory/PatientDirectoryMetricsRow.kt`: unused `Color` | **AGREE** | 0 `Color` references in body. |
| **C1-07** | `ui/patienthub/tabs/VisitsTab.kt`: 3 unused imports | **AGREE** | 0 body references for `Row`, `fillMaxWidth`, `padding`. |
| **C1-08** | `ui/TimelineSpine.kt`: `TimelineSpineItem` | **AGREE** | `git grep -n "\bTimelineSpineItem\b" app/src/` yields exactly 1 hit (declaration `:17`). Entire file is dead. |
| **C1-09** | `ui/PatientEditor.kt:619`: `EditorActions` | **AGREE** | `git grep -n "\bEditorActions\b" app/src/` yields 0 callers outside `:619`. L3 file (requires L3 sign-off). |
| **C1-10** | `ui/ClinicalPresets.kt:67-77`: `object ClinicalPresets` | **AGREE** | `git grep` yields 0 callers for `commonBloodPressures` and `commonVisitFindings`. L3 file (report only). |
| **C1-11** | `ui/patienthub/components/ClinicalAttentionBanner.kt:94,98,101` | **AGREE** | `ClinicalAttentionColors`, `clinicalAttentionColors()`, `AttentionColors.iconColor` have 0 external callers. Extended with NEW-01. |
| **C1-12** | `ui/patienthub/model/LabReportGrouping.kt:22-30`: `summaryPillText` | **AGREE** | `git grep -n "\bsummaryPillText\b" app/src/` yields 0 usages. |
| **C1-13** | `ui/Theme.kt`: 15 unused color vals | **AGREE** | `ClinicalRose` has 0 hits anywhere. `TagBloodNeutral*` (6) and `TagAttention*` (8) have 0 `main` usages. Tied to C6-09. |
| **C1-14** | `ui/Theme.kt`: 14 unused `ClinicalCardTokens.*` | **AGREE** | 0 usages in `main`, `test`, `androidTest`, or `res`. (Distinct from the tokens checked in `ThemeContrastTest.kt`). |
| **C2-01** | `ui/Components.kt:79-353`: `ClinicalPatientCard` | **AGREE** | `git grep -n "\bClinicalPatientCard\b" app/src/main/` yields 0 callers in `main`. Survives only in dead tests C6-01/C6-02. |
| **C2-02** | `ui/ClinicalBadges.kt`: 6 replaced badges | **AGREE** | In `main`, called ONLY inside dead `ClinicalPatientCard`. L3 file (requires L3 sign-off). |
| **C2-03** | `ui/DesignSystem.kt:57,253,292,488`: 4 composables | **AGREE** | `SectionCard`, `NoticeCard`, `SegmentedProgressBar`, `SectionHeader` have 0 callers in `app/src/`. |
| **C2-04** | `ui/DesignSystem.kt`: 5 dead `useRecordStyle = false` branches | **AGREE** | `git grep "useRecordStyle" app/src/main/` proves 100% of callers pass `true`. 0 callers pass `false`. |
| **C2-05** | `GenericEditorErrorCard`, `EditorPage`, `ClinicalDropdownField` branches | **AGREE** | All live callers pass `useRecordStyle = true` and `externalLabel = true`. `PatientEditor.kt` is L3. |
| **C2-06** | `ui/ClinicalPresets.kt`: `QuickPresetChips` branches | **AGREE** | All callers pass `useRecordStyle = true`. L3 file (report only). |
| **C2-07** | `ui/Theme.kt`: 50 old `Tag*` tokens | **AGREE** | Used in `main` ONLY by the 6 dead badges in C2-02. Tested in `ThemeContrastTest.kt` (C6-09). |
| **C2-08** | `ui/Theme.kt`: 25 unused `ClinicalCardTokens.*` tokens | **AGREE** | 0 `main` callers (live code uses only `Warning*` and `CriticalAccent`). Tied to C6-09. |
| **C2-09** | Raw `FilterChip` / `BatchTabChip` calls | **AGREE** | 8 raw `FilterChip` calls violate canonical `RecordFilterChip` usage and lack `minimumTouchTarget()`. Extended with NEW-02. |
| **C2-10** | Raw `Text(patient.displayName)` | **AGREE** | 4 call sites bypass canonical `PatientNameText` and risk RTL truncation/misalignment. `ClinicalAttachmentViewer` is L3. |
| **C2-11** | One-off local wrappers (`negativeHistoriesCard`, `PlanField`, etc.) | **AGREE** | Duplicate canonical `RecordSectionCard` and `EditorField(externalLabel=true)`. |
| **C2-12** | Active Meds callers of `FactRow`, `MedicationStatusBadge`, etc. | **REJECT** | **Active callers in live UI**: `MedicationCard.kt:182-189` calls `FactRow`; `MedicationCard.kt:147` calls `MedicationStatusBadge`; `MedicationForm.kt:171,250,265` calls `BigChoiceButton`, `ChoiceGroup`, and `ClinicalDateField`. Deleting these breaks compilation immediately. |
| **C2-13** | `EditorField(externalLabel = false)` floating-label callers | **REJECT** | **Active callers in live UI**: `MedicationForm.kt:140,153,220,279`, `MedicationTapToAddSection.kt:241`, and `DatingEditor.kt:258` call `EditorField(externalLabel = false)` or bare `EditorField`. Removing breaks compilation. |
| **C2-14** | `ui/report/ReportSections.kt:1188-1274`: notice card helpers | **REJECT** | **Active callers in live report UI**: `ReportErrorAlertBox`, `ReportModerateNoteBox`, `ReportHiddenCountNotice` are called by `PatientTwoPageReportDialog.kt`. Additionally protected Level 3 per `BRIEF.md:8`. |
| **C3-01** | `LabPatientBanner` vs `VisitPatientBanner` duplicate | **AGREE** | Structural duplicate (18+ lines) differing only in `FieldFamilies` and GA parameter. |
| **C3-02** | Viewer dialog chrome duplicate | **AGREE** | `ClinicalAttachmentViewerDialog.kt:94-110` and `UltrasoundPhotoViewerDialog.kt:109-124` share identical dialog card/scroll frame. L3 file. |
| **C3-03** | Bottom-sheet progress/chrome duplicate | **AGREE** | `DeliveryPlanSheet.kt:94-122` and `MedicationSheet.kt:85-112` share identical progress/header/scroll layout. |
| **C3-04** | `CatalogFormDialog` vs `SecurityFormDialog` duplicate | **AGREE** | ~65-line near-identical dialog container. `SecuritySettingsDialog.kt` is L3. |
| **C3-05** | `CatalogLabeledField` vs `SecurityLabeledField` duplicate | **AGREE** | Reimplement canonical `EditorField(externalLabel=true)`. `SecuritySettingsDialog.kt` is L3. |
| **C3-06** | Duplicate branches in `DesignSystem.kt` & `Components.kt` | **AGREE** | Live inside dead code (C2-01 & C2-04); resolved by deleting dead code. |
| **C3-07** | `narrowWindow()` duplicate across 7 test files | **REJECT** | **Breaks test isolation invariant**: `narrowWindow()` is actively called in 6 test files (`EditorSheetsRedesignTest.kt:151`, `CatalogSettingsRedesignTest.kt:97`, etc.). `AGENTS.md` forbids sharing test helpers across files. The single unused declaration in `LabEditorRedesignTest.kt` is dead code already handled in C6-04. |
| **C4-01** | `res/values/strings.xml`: 25 prototype/shell strings | **AGREE** | `git grep` across all code, XML, and manifest yields 0 references (`R.string.*` / `@string/*`). 0 `getIdentifier`. |
| **C4-02** | `res/values/strings.xml`: 23 synthetic-editor strings | **AGREE** | 0 references across all scopes. |
| **C4-03** | `res/values/strings.xml`: 31 old hub/badge strings | **AGREE** | 0 references across all scopes. |
| **C4-04** | `res/values/strings.xml`: 13 scan/badge/med strings | **AGREE** | 0 references across all scopes. |
| **C4-05** | `res/values/strings.xml`: 12 lab/preset strings | **AGREE** | 0 references across all scopes. |
| **C4-06** | `res/values/strings.xml`: 11 med/visit strings | **AGREE** | 0 references across all scopes. |
| **C4-07** | `res/values/strings.xml`: 6 strings + 1 plural | **AGREE** | 0 references for all 6 strings and `<plurals name="registration_count_existing">`. |
| **C4-08** | `res/values/strings.xml`: 3 attention dialog strings | **AGREE** | 0 references across all scopes. |
| **C4-09** | `res/drawable/ic_launcher_background.xml` vector | **AGREE** | 0 references. (Mipmaps reference `@color/ic_launcher_background` in `values/`, not the drawable). |
| **C4-10** | `res/mipmap-*/ic_launcher_foreground.png` (5 files) | **AGREE** | 0 references in `app/src/`. Mipmap XMLs reference `@drawable/clinic_logo_foreground`. |
| **C4-11** | `res/drawable/` vs `res/drawable-nodpi/` `clinic_logo_foreground.png` | **DOWNGRADE** | **Uncertain AAPT2 density packaging behavior**: Both files share SHA-256 `5e97bd2ea644c1e0948650976a6fa67d1155e6c83e6849d76d4a8cb38645edb4`. Removing `drawable/` could affect bitmap density scaling across devices; requires AAPT2 build proof. |
| **C5-01** | `ui/PatientListScreen.kt`: 6 section comments | **AGREE** | Pure narration comments restating obvious Compose structure; lines 338 omits Delivered/Archived filters. L3 file. |
| **C5-02** | `PatientHubHeader.kt` / `PatientHubScreen.kt`: 9 comments | **AGREE** | Generic restating comments (`// Top Action Bar`, `// Patient Name`, `// Facts row`, etc.). |
| **C5-03** | `ui/Components.kt`: 9 numbered card comments | **AGREE** | Restating comments inside dead `ClinicalPatientCard`. |
| **C5-04** | `ui/ClinicalBadges.kt:82,211-216`: spec tags & stale KDoc | **AGREE** | Spec tags `(Q87)`/`(Q205)` and false KDoc claiming non-compliant medication rows are colored error-red. L3 file. |
| **C5-05** | `ui/LabEditor.kt:114,122`: spec tag `(Q254)` & restating | **AGREE** | Spec-ID tag and generic narration comment. L3 file. |
| **C5-06** | `ui/ClinicalFormat.kt:85,101`: spec tags `(Q73)`/`(Q78)` | **AGREE** | Spec-ID tags in public docstrings. L3 file. |
| **C5-07** | `res/values/strings.xml:1032`: Slice 20 changelog | **AGREE** | Dead changelog comment `<!-- Slice 20: Two-page clinical report dialog -->`. |
| **C6-01** | `androidTest/ClinicalPatientCardTest.kt:1-74` | **AGREE** | Tests dead `ClinicalPatientCard` exclusively. 0 production callers. |
| **C6-02** | `androidTest/DatingGaMarkerSurfacesTest.kt:115-120` | **AGREE** | Tests dead `ClinicalPatientCard` variants. Remaining tests in file exercise live surfaces. |
| **C6-03** | `androidTest/OverviewTabSectionsTest.kt:477-497` | **AGREE** | Tests legacy `GestationalAgeBadge` directly. 0 production callers. |
| **C6-04** | `androidTest/LabEditorRedesignTest.kt:275-298`: `narrowWindow` | **AGREE** | Helper declared `:275` but called 0 times in file; `savedConfig`/`savedMetrics` unused; `restoreResources()` no-op. |
| **C6-05** | `test/state/DeliveryOutcomeStateTest.kt:23-30`: `composedNotes` | **AGREE** | Private helper declared `:23` has 0 references in file. |
| **C6-06** | `androidTest/` (25 files): 86 unused imports | **AGREE** | Full token scan confirms 0 body usages across all 25 files (`getValue`/`setValue` excluded). |
| **C6-07** | `test/` (11 files): 14 unused imports | **AGREE** | Full token scan confirms 0 body usages across all 11 files. |
| **C6-08** | `androidTest/VisitEntryDesignTest.kt:111`: stale comment | **AGREE** | References dead `SegmentedProgressBar`; live UI uses `VisitStepHeader`. |
| **C6-09** | `test/ui/ThemeContrastTest.kt`: coupled token assertions | **AGREE** | Source-reading regex tests coupled to dead tokens; must be aligned when tokens are deleted. |
| **P-01..P-14**| Protected report-only findings (14 items) | **AGREE** | Confirmed dead imports, unreferenced decls, and duplicates in protected files. Kept as report-only per `BRIEF.md:7`. |

---

## New Findings Missed by Finder (`NEW`)

| ID | File:Line | What | Cat | Exact Grep / Evidence Command & Result | Risk | Protected / L3 | Proposed Action |
|---|---|---|---|---|---|---|---|
| **NEW-01** | `ui/patienthub/components/ClinicalAttentionBanner.kt:92,93,95` | Unreferenced getter aliases `AttentionColors.containerColor`, `textColor`, `borderColor` | Cat 1 | `git grep -n "\b(containerColor\|textColor\|borderColor)\b" app/src/` -> 0 usages in `main` or tests on `AttentionColors` (callers use `.container`, `.content`, `.border`) | Low | No | Delete alongside `iconColor` (C1-11) |
| **NEW-02** | `ui/patienthub/components/MedicationTapToAddSection.kt:481-505` | Unreferenced private helpers `pickerFilterChipColors` and `pickerFilterChipBorder` | Cat 2 | `git grep -n "pickerFilterChip" app/src/` -> lines 260, 269, 481, 500. Sole callers are raw `FilterChip` calls (:258, :267) | Low | No | Delete when raw chips are migrated to canonical `RecordFilterChip` (C2-09) |

---

## Numbered Execution Plan Ordered by Risk

### Batch 1: Pure Unused Imports & Private Unreferenced Declarations (Lowest Risk)
- **Scope**: Dead imports across main source tree and dead private helpers/getters.
- **Exact Files & Symbols**:
  - `app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt`: `Spacer`, `width`, `OutlinedButton` (3 imports) *(Level 3 pure import rule)*
  - `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalBadges.kt`: `Column`, `fillMaxWidth`, `Color`, `luminance` (4 imports) *(Level 3 pure import rule)*
  - `app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt`: `background`, `clickable`, `FlowRow`, `height`, `RoundedCornerShape`, `AccountCircle`, `Add`, `Edit`, `EditNote`, `MoreVert`, `Unarchive`, `Warning`, `CardDefaults`, `HorizontalDivider`, `ModalBottomSheet`, `OutlinedButton`, `Surface`, `clip`, `Color`, `lerp`, `luminance`, `SevereBpAttention` (22 imports) *(Level 3 pure import rule)*
  - `app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt`: `background`, `clickable`, `fillMaxSize`, `height`, `AccountCircle`, `TextAlign` (6 imports)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryCard.kt`: `Text` (1 import)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryMetricsRow.kt`: `Color` (1 import)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/VisitsTab.kt`: `Row`, `fillMaxWidth`, `padding` (3 imports)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt`: `AttentionColors.containerColor`, `textColor`, `iconColor`, `borderColor`, typealias `ClinicalAttentionColors`, function `clinicalAttentionColors()` (6 declarations; C1-11, NEW-01)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt`: property `CompositeTestGroup.summaryPillText` (1 property; C1-12)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt`: 14 unused `ClinicalCardTokens.*` tokens (`DatingBorder`, `DatingText`, `DatingDarkBorder`, `ParityBorder`, `ParityText`, `ParityDarkBorder`, `ScanBorder`, `ScanText`, `ScanDarkBorder`, `CriticalBorder`, `CriticalDarkBorder`, `AllergyBorder`, `AllergyText`, `AllergyDarkBorder`) (C1-14)
  - *Reported for Level 3 review before deletion*:
    - `app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt:619`: `internal fun EditorActions` (C1-09)
    - `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalPresets.kt:67-77`: `object ClinicalPresets` (`commonBloodPressures`, `commonVisitFindings`) (C1-10)
- **Compile Verification Command**:
  `./gradlew compileDebugKotlin`

---

### Batch 2: Unused Resources in `res/` (Low Risk)
- **Scope**: Dead XML strings, plurals, drawables, and mipmaps.
- **Exact Files & Symbols**:
  - `app/src/main/res/values/strings.xml`: 124 dead strings and 1 dead plural:
    - *Shell/prototype (25)*: `demo_banner`, `workspace`, `profile`, `case_id`, `no_visits`, `recorded_visits`, `read_only`, `read_only_profile`, `prototype_placeholder`, `placeholder_body`, `obstetric_history`, `other_histories`, `episode_overview`, `visits`, `laboratory`, `medications`, `medications_tab`, `attachments`, `synthetic_edit`, `visit_editor`, `identity`, `secure_demo_storage`, `lock_demo_storage`, `credential_prompt_title`, `credential_prompt_subtitle` (C4-01)
    - *Synthetic editor (23)*: `synthetic_patient_editor_notice`, `full_name`, `case_id_required`, `gpa_all_or_none`, `archived`, `archived_read_only_notice`, `no_previous_pregnancies`, `no_date_inference_notice`, `dating_editor_notice`, `add_ultrasound`, `ga_at_scan`, `no_ultrasound_recorded`, `ultrasound_editor_notice`, `invalid_ga`, `add_lab`, `edit_lab`, `lab_editor_notice`, `medication_editor_notice`, `end_date`, `delivery_planning_notice`, `calculated_ga_label`, `simulated_photo`, `attachment_save_failed` (C4-02)
    - *Old hub/overview/badge (31)*: `active_pregnancy_episode`, `milestones`, `first_trimester`, `second_trimester`, `third_trimester`, `latest_scan`, `latest_visit`, `total_visits`, `no_lab_recorded`, `no_medications_recorded`, `clinical_summary`, `copy_clinical_summary`, `summary_copied`, `quick_actions`, `parity_format`, `normal_risk`, `patient_age_years`, `ga_prefix`, `edd_prefix`, `unrecorded_risk`, `edd_pending`, `expand_details`, `collapse_details`, `recent_visits`, `recent_scans`, `recent_labs`, `active_medications`, `stopped_medications`, `parity_summary`, `no_record`, `edit_history` (C4-03)
    - *Badge/scan-link/med-option (13)*: `abnormal_badge`, `normal_badge`, `create_visit_for_scan`, `add_scan_photo`, `detailed_biometry_toggle`, `add_another_medication`, `critical_badge`, `alert_badge`, `note_badge`, `standalone_scan`, `view_linked_ultrasound`, `view_linked_visit`, `advanced_med_options` (C4-04)
    - *Lab/preset/collapse (12)*: `flag`, `flag_abnormal`, `note`, `note_filled`, `lab_preset_negative`, `lab_preset_non_reactive`, `lab_preset_normal`, `lab_preset_positive`, `collapse_all`, `expand_all`, `lab_batch_review_n_results`, `lab_batch_review_one_result` (C4-05)
    - *Med session / visit quick (11)*: `stopped_in_this_session`, `restart_medication`, `custom_drug_title`, `custom_drug_label`, `active_count_summary`, `other_date`, `same_as_last_fh`, `matches_ga_fh`, `fundal_height`, `flag_badge`, `delivered_patients_empty` (C4-06)
    - *Registration counts (6 strings + 1 plural)*: `registration_count_title`, `registration_count_label`, `registration_count_hint`, `<plurals name="registration_count_existing">`, `registration_count_error`, `registration_resume_body`, `registration_resume_button` (C4-07)
    - *Attention dialog (3)*: `delete_attention_entry`, `attention_new_label`, `attention_add_button` (C4-08)
  - `app/src/main/res/drawable/ic_launcher_background.xml`: unreferenced vector drawable (C4-09)
  - `app/src/main/res/mipmap-{hdpi,mdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_foreground.png`: 5 unreferenced launcher foreground PNG assets (C4-10)
- **Compile Verification Command**:
  `./gradlew mergeDebugResources assembleDebug`

---

### Batch 3: Old Design Leftovers, Branches & Duplicates (Medium Risk)
- **Scope**: Dead composables, legacy parameter branches, raw chip/text replacements, and duplicate dialog/banner chrome.
- **Exact Files & Symbols**:
  - `app/src/main/java/com/sherifradwan/ancnative/ui/TimelineSpine.kt`: delete entire file (`TimelineSpineItem`) (C1-08)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt`: delete `enum class PatientCardVariant` & `ClinicalPatientCard` (lines 79-353) (C2-01)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/DesignSystem.kt`: delete unreferenced composables `SectionCard`, `NoticeCard`, `SegmentedProgressBar`, `SectionHeader` (C2-03)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/DesignSystem.kt`: delete dead `useRecordStyle = false` legacy branches in `SummaryRow`, `ValueStepper`, `BatchInfoBar`, `BatchSegmentedProgress`, `QualitativeChoiceButton` (resolves C2-04, C3-06)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt`: delete dead `useRecordStyle = false` branch in `GenericEditorErrorCard` (C2-05)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt`: delete 15 dead color tokens (`ClinicalRose`, 6 `TagBloodNeutral*`, 8 `TagAttention*`) (C1-13), 50 old `Tag*` tokens (C2-07), and 25 unused `ClinicalCardTokens.*` tokens (C2-08) *(execute in lockstep with Batch 5 `ThemeContrastTest.kt` update)*
  - `app/src/main/java/com/sherifradwan/ancnative/ui/AddAttachmentDialog.kt:170,190`, `DeliveryPlanSheet.kt:284,316`, `MedicationForm.kt:192,203`, `MedicationTapToAddSection.kt:258,267`: replace raw `FilterChip` calls with canonical `RecordFilterChip`; delete `pickerFilterChipColors` & `pickerFilterChipBorder` (C2-09, NEW-02)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt:254,433`, `AddAttachmentDialog.kt:142`: replace raw `Text(patient.displayName)` with canonical `PatientNameText` (C2-10)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/OtherHistoriesEditor.kt:112-123`, `DeliveryPlanSheet.kt:341-368`, `CatalogSettingsSections.kt:1088-1098,1269-1313`: replace local wrappers (`negativeHistoriesCard`, `PlanField`, `CatalogChipGroup`, `CatalogEmptyCard`) with canonical `RecordSectionCard`, `EditorField` (C2-11)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/LabEditorComponents.kt:49-108` vs `VisitEditorSteps.kt:91-146`: extract shared `EditorPatientBanner` in `ui/components/` (C3-01)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/DeliveryPlanSheet.kt:94-122` vs `MedicationSheet.kt:85-112`: extract shared `EditorModalSheet` in `EditorSheetParts.kt` (C3-03)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/CatalogSettingsSections.kt:1132-1195,1198-1230`: replace local `CatalogLabeledField` with `EditorField(externalLabel=true)` and extract shared `RecordFormDialog` (C3-04, C3-05)
  - *Reported for Level 3 review before edits*:
    - `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalBadges.kt`: delete 6 replaced badges (`PatientAgeBadge`, `ClinicalTagBadge`, `ClinicalRiskBadge`, `BloodGroupBadge`, `GestationalAgeBadge`, `ObstetricParityBadge`) (C2-02)
    - `app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt:404-482,553-605`: delete `useRecordStyle=false` / `externalLabel=false` branches in `EditorPage`, `EditorPageHeader`, `ClinicalDropdownField` (C2-05)
    - `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalPresets.kt:89-144`: delete `useRecordStyle=false` branch & raw chip in `QuickPresetChips` (C2-06)
    - `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt:111`: replace raw `Text` with `PatientNameText` (C2-10)
    - `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt:94-110` vs `UltrasoundPhotoViewerDialog.kt:109-124`: shared viewer frame (C3-02)
    - `app/src/main/java/com/sherifradwan/ancnative/ui/SecuritySettingsDialog.kt:842-960`: `SecurityFormDialog` & `SecurityLabeledField` (C3-04, C3-05)
- **Compile Verification Command**:
  `./gradlew compileDebugKotlin ./gradlew testDebugUnitTest`

---

### Batch 4: Code-Comment Slop (Lowest Semantic Risk)
- **Scope**: Restating section dividers, obsolete numbered comments, and stale spec IDs.
- **Exact Files & Lines**:
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt:189,252,262,312,392` & `PatientHubScreen.kt:156,182,201,296`: 9 generic restating comments (C5-02)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt:124,193,201,207,212,229,232,241,255`: 9 numbered restating comments (C5-03, deleted with C2-01)
  - `app/src/main/res/values/strings.xml:1032`: Slice 20 changelog comment (C5-07)
  - *Reported for Level 3 review before comment cleanup*:
    - `app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt:242,292,313,338,452,487`: 6 stale/restating section comments (C5-01)
    - `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalBadges.kt:82,211-216`: spec tags `(Q87)`/`(Q205)` and stale false KDoc claiming non-compliant rows color error-red (C5-04)
    - `app/src/main/java/com/sherifradwan/ancnative/ui/LabEditor.kt:114,122`: spec tag `(Q254)` and generic comment (C5-05)
    - `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalFormat.kt:85,101`: spec tags `(Q73)`/`(Q78)` in KDoc (C5-06)
- **Compile Verification Command**:
  `./gradlew compileDebugKotlin`

---

### Batch 5: Dead Tests & Test Alignment (Medium-High Test Integrity Risk)
- **Scope**: Removing tests of removed code, dead test helpers, unused test imports, and updating source-reading contrast tests.
- **Exact Files & Symbols**:
  - `app/src/androidTest/java/com/sherifradwan/ancnative/ClinicalPatientCardTest.kt`: delete entire test class (74 lines; tests dead `ClinicalPatientCard` exclusively) (C6-01)
  - `app/src/androidTest/java/com/sherifradwan/ancnative/DatingGaMarkerSurfacesTest.kt:27-28,115-120`: delete dead `ClinicalPatientCard` variant loop & imports (C6-02)
  - `app/src/androidTest/java/com/sherifradwan/ancnative/OverviewTabSectionsTest.kt:31,477-497`: migrate or delete direct `GestationalAgeBadge` test assertions (C6-03)
  - `app/src/androidTest/java/com/sherifradwan/ancnative/LabEditorRedesignTest.kt:136,272,275-298`: delete dead helper `narrowWindow(fontScale)`, unused `savedConfig`/`savedMetrics`, and no-op `restoreResources()` (C6-04)
  - `app/src/test/java/com/sherifradwan/ancnative/state/DeliveryOutcomeStateTest.kt:23-30`: delete unreferenced `private fun composedNotes(...)` (C6-05)
  - `app/src/androidTest/` (25 files): delete 86 unused imports across test classes (C6-06)
  - `app/src/test/` (11 files): delete 14 unused imports across test classes (C6-07)
  - `app/src/androidTest/java/com/sherifradwan/ancnative/VisitEntryDesignTest.kt:111`: update stale comment referencing `SegmentedProgressBar` (C6-08)
  - `app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt`: update `statusTagPairs()` (:106-107) and `clinicalCardTokens_...` (:160-188) token lists to remove dead `Tag*` and dead `ClinicalCardTokens.*` while retaining live checks (`FieldDelivery*`, `Warning*`, `CriticalAccent`) (C6-09)
- **Compile & Test Verification Command**:
  `./gradlew testDebugUnitTest connectedDebugAndroidTest`

---

## Rejected and Why

| Finding ID | Candidates Proposed in `findings_A1.md` | Exact Live Call Sites Found in Codebase | Why Removal Breaks Build / Violates Invariants (REJECT Verdict) |
|---|---|---|---|
| **C2-12** | `FactRow`, `MedicationStatusBadge`, `BigChoiceButton`, `ChoiceGroup(useRecordChip=false)`, `ClinicalDateField(useRecordStyle=false)` | `ui/patienthub/components/MedicationCard.kt:147` (`MedicationStatusBadge`), `:182-189` (`FactRow`); `ui/patienthub/components/MedicationForm.kt:171` (`ChoiceGroup`), `:250` (`BigChoiceButton`), `:265` (`ClinicalDateField`) | **Breaks compilation**: These legacy components and legacy parameter branches are actively invoked by live medication cards and medication editing forms in `app/src/main/`. Removing them produces immediate unresolved reference errors. Proposing deletion without full redesign of Meds UI is rejected. |
| **C2-13** | `EditorField(externalLabel = false)` legacy floating-label branch | `ui/patienthub/components/MedicationForm.kt:140,153,220,279`, `ui/patienthub/components/MedicationTapToAddSection.kt:241`, `ui/DatingEditor.kt:258` | **Breaks compilation**: While editors inside `RecordSectionCard` pass `label = ""`, `MedicationForm` and `DatingEditor` actively pass floating labels relying on `externalLabel = false`. Removing the branch breaks active screen rendering and field sizing. |
| **C2-14** | `ReportErrorAlertBox`, `ReportModerateNoteBox`, `ReportHiddenCountNotice` | `ui/report/ReportSections.kt:1188-1274`, `ui/report/PatientTwoPageReportDialog.kt:283,310,342` | **Breaks compilation & violates L3 rules**: These notice boxes are actively used inside the clinical report dialog. Furthermore, `ui/report/*` is protected Level 3 per `BRIEF.md:8`. They are live print/report widgets with distinct layout constraints, not dead code. |
| **C3-07** | `narrowWindow()` / `restoreResources()` configuration helper duplicate | `EditorSheetsRedesignTest.kt:151`, `CatalogSettingsRedesignTest.kt:97`, `SecurityLockVaultRedesignTest.kt:111`, `SharedDialogsRedesignTest.kt:111`, etc. (6 live test files) | **Violates test isolation invariant**: `AGENTS.md` explicitly mandates avoiding cross-file test dependencies to prevent cascading test failures. The helper is active in 6 files; extracting a shared helper couples tests. The single dead declaration in `LabEditorRedesignTest.kt` is dead code already handled under C6-04. |

---

## Not Verified

1. **Gradle Build & Connected Test Execution**:
   Per `BRIEF.md:6` hard constraints (READ-ONLY analysis, no Gradle or ADB commands permitted), no `./gradlew test`, `./gradlew connectedAndroidTest`, or APK packaging was executed. All conclusions rest on exhaustive static analysis, token indexing, and regex verification across all source sets.
2. **Resource Merging & Density Fallback (C4-11 DOWNGRADE)**:
   `app/src/main/res/drawable/clinic_logo_foreground.png` and `app/src/main/res/drawable-nodpi/clinic_logo_foreground.png` are byte-identical. However, deleting the `drawable/` copy cannot be proven safe without an AAPT2 packaging inspection (`aapt2 dump resources`), as Android resource merging rules treat `drawable/` as baseline mdpi and `drawable-nodpi/` as unscaled.
3. **Internal Logic of Protected Modules (P-08..P-14)**:
   Internal duplicate clusters in `RecordsRepository.kt`, `RecordsMigration.kt`, `EncryptedBackupManager.kt`, and `PatientPrintHtmlGenerator.kt` were cross-checked in `tool_duplicates.txt` and verified statically, but were not audited for refactoring safety because editing protected files is strictly prohibited by `BRIEF.md:7`.
4. **Prose Comments in Complex Editors**:
   Category 4 slop inspection focused on restating section comments, ASCII banners, and spec ID tags `(Q##)`. Multi-sentence explanatory clinical logic comments in `UltrasoundEditor.kt` and `LabBatchScreen.kt` were not flagged as slop.

---

## Protected, Report Only (`BRIEF.md:7` — Never Edit or Propose Removal)

| ID | File:Line | What | Category | Static Verification | Protected Rule |
|---|---|---|---|---|---|
| **P-01** | `model/ClinicalModels.kt:5` | Unused import `java.time.temporal.ChronoUnit` | Cat 1 | 0 body references in file | `ClinicalModels`, `model/` |
| **P-02** | `state/AncState.kt:10-14` | 5 unused imports (`PreviousPregnancyFormRules`, `effectiveMedicationStatus`, `parseBloodPressure`, `parseVisitFindings`, `ClinicalRecordKind`) | Cat 1 | 0 body references in `AncState.kt` | Reducer / `state/` |
| **P-03** | `state/AncViewModel.kt:35-36` | 2 unused imports (`Job`, `delay`) | Cat 1 | 0 body references in `AncViewModel.kt` | `state/` |
| **P-04** | `storage/ClinicalCommands.kt:4-5`, `storage/RecordsRepository.kt:15,17,18` | 5 unused imports (`DemoLabInvestigation`, `DemoMedicationRecord`, `PatientVocabulary`, `LocalDate`, `DateTimeParseException`) | Cat 1 | 0 body references in either file | `storage/`, `RecordsRepository` |
| **P-05** | `ui/report/PatientPrintHtmlGenerator.kt:10-11` | 2 unused imports (`GestationalAgeEngine`, `PreviousPregnancyFormRules`) | Cat 1 | 0 body references in file | `*Generator*`, L3 `ui/report/*` |
| **P-06** | `security/SecuritySettingsRepository.kt:149` | Unreferenced `setAutoLockTimeout` | Cat 1 | 0 callers across `app/src/` | Security / keystore |
| **P-07** | `storage/AttachmentSyncManager.kt:26,49`, `CameraCaptureHelper.kt:26`, `PatientAttachmentVault.kt:96`, `RecordsDao.kt:492,512`, `VaultFiles.kt:149,151,152` | 9 unreferenced declarations (`localizedStatus`, `computeStats`, `createCameraTempUri`, `saveAttachmentFromStream`, `attachmentsForPatient`, `updateAttachmentSyncStatus`, `hasEnvelope`, `hasDatabase`, `hasIncompletePointerArtifact`) | Cat 1 | 0 callers across `main`, `test`, `androidTest`, `debug` | `storage/`, `RecordsDao` |
| **P-08** | `storage/RecordsRepository.kt` (9 clusters) | 9 internal mapper/receipt/query duplicate clusters | Cat 3 | Verified cluster locations in `tool_duplicates.txt` | `RecordsRepository`, `storage/` |
| **P-09** | `state/AncViewModel.kt:799-810` vs `storage/RecordsRepository.kt:1494-1505` | 2 duplicate sync/write clusters | Cat 3 | Verified cluster locations in `tool_duplicates.txt` | `state/`, `RecordsRepository` |
| **P-10** | `storage/RecordsMigration.kt:449-670` (6x), `:72-84/:222-234` | 2 schema/validation duplicate clusters | Cat 3 | Verified cluster locations in `tool_duplicates.txt` | `storage/` migration |
| **P-11** | `model/DemoModels.kt:80-94` vs `storage/PatientCommands.kt:240-254` | Command/model serialization duplicate | Cat 3 | Verified cluster locations in `tool_duplicates.txt` | `model/`, `storage/` |
| **P-12** | `model/ClinicalModels.kt:144-156,178-188` vs `storage/ClinicalCommands.kt:178-190,391-401` | 2 model/command validation/mapping duplicates | Cat 3 | Verified cluster locations in `tool_duplicates.txt` | `model/`, `ClinicalModels` |
| **P-13** | `storage/EncryptedBackupManager.kt:50-60`, `ui/report/PatientPrintHtmlGenerator.kt:1119-1128` | Crypto envelope & HTML print meds duplicates | Cat 3 | Verified cluster locations in `tool_duplicates.txt` | `storage/`, `*Generator*` |
| **P-14** | `model/DeliveryNotes.kt`, `model/VisitEntryLogic.kt`, `model/catalog/ClinicalCatalogRepository.kt` | Spec-ref & ASCII decorative divider comments | Cat 5 | 30 decorative banner/divider lines | `model/` |
