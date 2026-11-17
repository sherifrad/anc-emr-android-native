# Slice 21 Cleanup Findings — Team B, Member B1

Branch: `redesign-2026-11-15` | Scope: `app/src/main/java`, `app/src/main/res`, `app/src/test`, `app/src/androidTest`, `app/src/debug`, `AndroidManifest.xml` (READ-ONLY analysis).

## Verification Notes on Tool Candidates
- **`tool_unused.txt` false positives (58 imports)**: All `import androidx.compose.runtime.getValue` (29) and `setValue` (29) entries flagged by `find_unused.py` are **active** Kotlin property-delegate operator imports (`by remember`, `by rememberSaveable`, `by collectAsState`). Verified via regex `r"\b(val|var)\s+\S+\s+by\b"` across all 29 files (0 genuinely unused delegate imports).
- **`FactRow` (`ui/Components.kt:356,371`)**: Still actively called by `MedicationCard.kt:182-189` (slice 9 was tune-only); **keep**.
- **`res/values/ic_launcher_background.xml`**: Referenced as `@color/ic_launcher_background` in `mipmap-anydpi-v26/ic_launcher*.xml`; **keep** (only `drawable/ic_launcher_background.xml` is unused).

---

## Category 1: Unused Imports, Unreferenced Declarations & Dead Branches

| ID | File:Line | What | Cat | Exact Grep / Command & Result | Risk & Why | Protected / L3 | Proposed Action | Status |
|---|---|---|---|---|---|---|---|---|
| U-01 | `ui/AncApp.kt:10,17,30` | Unused imports `Spacer`, `width`, `OutlinedButton` | 1 | Python symbol scan on file: `hits=0` outside import lines | Low (dead imports) | L3 (pure unused import) | Delete | CONFIRMED |
| U-02 | `ui/ClinicalBadges.kt:8,10,24,25` | Unused imports `Column`, `fillMaxWidth`, `Color`, `luminance` | 1 | Python symbol scan on file: `hits=0` outside import lines | Low (dead imports) | L3 (`PLAN.md`, pure unused import) | Delete | CONFIRMED |
| U-03 | `ui/Components.kt:6,7,15,17,27,58` | Unused imports `background`, `clickable`, `fillMaxSize`, `height`, `AccountCircle`, `TextAlign` | 1 | Python symbol scan on file: `hits=0` outside import lines | Low (dead imports) | No | Delete | CONFIRMED |
| U-04 | `ui/patientdirectory/PatientDirectoryCard.kt:24` | Unused import `Text` | 1 | Python symbol scan on file: `hits=0` outside import line | Low | No | Delete | CONFIRMED |
| U-05 | `ui/patientdirectory/PatientDirectoryMetricsRow.kt:16` | Unused import `Color` | 1 | Python symbol scan on file: `hits=0` outside import line | Low | No | Delete | CONFIRMED |
| U-06 | `ui/patienthub/tabs/VisitsTab.kt:6,8,9` | Unused imports `Row`, `fillMaxWidth`, `padding` | 1 | Python symbol scan on file: `hits=0` outside import lines | Low | No | Delete | CONFIRMED |
| U-07 | `ui/DesignSystem.kt:57` | Unreferenced `SectionCard` composable | 1 | `rg -n "\bSectionCard\b" app/src` -> 1 hit (`DesignSystem.kt:57`) | Low (all cards moved to `RecordSectionCard`) | No | Delete | CONFIRMED |
| U-08 | `ui/DesignSystem.kt:253` | Unreferenced `NoticeCard` composable | 1 | `rg -n "\bNoticeCard\b" app/src` -> 1 hit (`DesignSystem.kt:253`) | Low (no callers) | No | Delete | CONFIRMED |
| U-09 | `ui/DesignSystem.kt:488` | Unreferenced `SectionHeader` composable | 1 | `rg -n "\bSectionHeader\b" app/src` -> 1 hit (`DesignSystem.kt:488`) | Low (no callers) | No | Delete | CONFIRMED |
| U-10 | `ui/PatientEditor.kt:619` | Unreferenced `internal fun EditorActions` | 1 | `rg -n "\bEditorActions\b" app/src` -> 1 hit (`PatientEditor.kt:619`) | Low (no callers) | L3 (`PLAN.md`) | Report only (L3) | CONFIRMED |
| U-11 | `ui/TimelineSpine.kt:17` | Unreferenced `TimelineSpineItem` (entire file) | 1 | `rg -n "\bTimelineSpineItem\b" app/src` -> 1 hit (`TimelineSpine.kt:17`) | Low (VisitsTab uses `RecordBandCard`) | No | Delete file | CONFIRMED |
| U-12 | `ui/patienthub/components/ClinicalAttentionBanner.kt:94,98,101` | Unreferenced `ClinicalAttentionColors` alias, `clinicalAttentionColors()`, `AttentionColors.iconColor` | 1 | `rg -n "\b(ClinicalAttentionColors\|clinicalAttentionColors\|iconColor)\b" app/src` -> 1 hit each | Low (superseded by `AttentionColors`/`attentionColors`) | No | Delete | CONFIRMED |
| U-13 | `ui/ClinicalPresets.kt:67-77` | Unreferenced `object ClinicalPresets` (`commonBloodPressures`, `commonVisitFindings`) | 1 | `rg -n "\b(ClinicalPresets\|commonBloodPressures\|commonVisitFindings)\b" app/src` -> 1 hit each | Low (no callers) | L3 (`BRIEF.md`) | Report only (L3) | CONFIRMED |
| U-14 | `ui/Components.kt:79-353` | `ClinicalPatientCard` & `PatientCardVariant` unused in production | 1 | `rg -n "\bClinicalPatientCard\b" app/src` -> 0 `main` callers (only `ClinicalPatientCardTest`, `DatingGaMarkerSurfacesTest`) | Medium (update/remove 2 androidTests) | No | Delete after T-01/T-02 | CONFIRMED |
| U-15 | `ui/DesignSystem.kt:98,346,539,658,714` | Dead `useRecordStyle = false` branches in `SummaryRow`, `ValueStepper`, `BatchInfoBar`, `BatchSegmentedProgress`, `QualitativeChoiceButton` | 1 | `rg -n "useRecordStyle" app/src/main/java/.../ui/` -> all callers pass `useRecordStyle = true` | Medium (check default args in tests) | No | Remove `useRecordStyle` param & `false` branch | CONFIRMED |
| U-16 | `ui/Components.kt:519-526`, `ui/ClinicalPresets.kt:95-142`, `ui/PatientEditor.kt:401-484` | Dead `useRecordStyle = false` branches in `GenericEditorErrorCard`, `QuickPresetChips`, `EditorPage`/`EditorPageHeader` | 1 | `rg -n "useRecordStyle" app/src/main/java/.../ui/` -> all callers pass `useRecordStyle = true` | Medium (verify test callers) | No (`Components.kt`), L3 (`ClinicalPresets`, `PatientEditor`) | Delete `false` branch in `Components.kt`; report only for L3 | CONFIRMED |
| U-17 | `ui/patienthub/components/ClinicalAttentionBanner.kt:92,93,95` | `AttentionColors.containerColor`, `textColor`, `borderColor` alias getters | 1 | `rg -n "\b(containerColor\|textColor\|borderColor)\b" app/src` -> 65 / 10 / 12 hits (name collisions with M3/other classes) | Low | No | Verify receiver types before delete | UNCERTAIN |

---

## Category 2: Old Design-System Leftovers & Local Duplicates of `ui/components`

| ID | File:Line | What | Cat | Exact Grep / Command & Result | Risk & Why | Protected / L3 | Proposed Action | Status |
|---|---|---|---|---|---|---|---|---|
| D-01 | `ui/ClinicalBadges.kt:121,185` | `ClinicalTagBadge` & `ClinicalRiskBadge` replaced by `StatusTag` | 2 | `rg -n "\bClinicalTagBadge\b" app/src` -> only `ClinicalRiskBadge`; `rg -n "\bClinicalRiskBadge\b" app/src` -> only dead `ClinicalPatientCard` | Low | L3 (`PLAN.md`) | Report only (L3) | CONFIRMED |
| D-02 | `ui/ClinicalBadges.kt:43,246,288,345,359` | `PatientAgeBadge`, `BloodGroupBadge`, `GestationalAgeBadge`, `ObstetricParityBadge` unused in prod | 2 | `rg -n` for each across `app/src` -> 0 `main` callers outside dead `ClinicalPatientCard` (`GestationalAgeBadge` in `OverviewTabSectionsTest`) | Medium (test T-03 depends on `GestationalAgeBadge`) | L3 (`PLAN.md`) | Report only (L3) | CONFIRMED |
| D-03 | `ui/Theme.kt:30-116` | 48 old `Tag*` colour tokens (`TagObstetric*`, `TagGestationalAge*`, `TagBlood*`, `TagAttention*`, `TagHighRisk*`, `TagModerateRisk*`, `TagNormalRisk*`, `TagAge*`) | 2 | `rg -n "\bTag(Obstetric\|GestationalAge\|Blood\|Attention\|HighRisk\|ModerateRisk\|NormalRisk\|Age)" app/src` -> `TagModerateRisk*` 0 hits; `TagAttention*` only `ThemeContrastTest`; rest only dead `ClinicalBadges` + `ThemeContrastTest` | Medium (breaks `ThemeContrastTest` until T-04 updated) | No | Delete with D-01/D-02 & T-04 | CONFIRMED |
| D-04 | `ui/Theme.kt:162-222` | 40 unused `ClinicalCardTokens.*` (`Dating*` 8, `Parity*` 8, `Scan*` 8, `Allergy*` 8, `CriticalBg/Border/Text` 6, `WarningBorder` 2) | 2 | `rg -n "ClinicalCardTokens\.(Dating\|Parity\|Scan\|Allergy\|Critical\|Warning)" app/src` -> 0 `main` callers (`OverviewSections` uses `WarningBg/Accent/Text`; `GaDiscordanceMarker` uses `CriticalAccent`) | Medium (tested in `ThemeContrastTest`) | No | Delete 40 unused tokens; update T-04 | CONFIRMED |
| D-05 | `ui/Theme.kt:25-26` & `ui/patienthub/PatientHubHeader.kt:221,471` | Unused `ClinicalRose` token (`Theme.kt:25`) and legacy `ActionRose` tint on attention warning icons in `PatientHubHeader` | 2 | `rg -n "\bClinicalRose\b" app/src` -> 1 hit (`Theme.kt:25`); `rg -n "\bActionRose\b" app/src` -> only dead `DesignSystem.kt` branches + `PatientHubHeader.kt:221,471` | Low | No | Delete `ClinicalRose`; replace `ActionRose` with `colorScheme.error`, then delete `ActionRose` | CONFIRMED |
| D-06 | `ui/AddAttachmentDialog.kt:170,190` | Raw Material3 `FilterChip` for attachment category & quality presets (no `minimumTouchTarget()`) | 2 | `rg -n "FilterChip\(" app/src/main/java/.../ui/` -> `AddAttachmentDialog.kt:170,190` | Low (visual & 48dp target consistency) | No | Replace with `RecordFilterChip` | CONFIRMED |
| D-07 | `ui/patienthub/components/MedicationForm.kt:171,192,203,265` | `ChoiceGroup` without `useRecordChip = true`, raw `FilterChip` for frequency, `ClinicalDateField` without `useRecordStyle = true` | 2 | `rg -n "useRecordChip\|FilterChip\|ClinicalDateField\(" app/src/main/.../ui/` -> sole caller omitting `useRecordChip`/`useRecordStyle` | Low | No | Pass `useRecordStyle = true` / `useRecordChip = true`; replace raw `FilterChip` with `RecordFilterChip` | CONFIRMED |
| D-08 | `ui/patienthub/components/DeliveryPlanSheet.kt:284,316` | Raw Material3 `FilterChip` for delivery mode & outcome (no `minimumTouchTarget()`) | 2 | `rg -n "FilterChip\(" app/src/main/.../ui/` -> `DeliveryPlanSheet.kt:284,316` | Low | No | Replace with `RecordFilterChip` | CONFIRMED |
| D-09 | `ui/patienthub/components/MedicationTapToAddSection.kt:258,267,481-500` | Raw `FilterChip` with custom `pickerFilterChipColors`/`pickerFilterChipBorder` (`secondaryContainer`) | 2 | `rg -n "pickerFilterChip\|FilterChip\(" app/src/main/.../ui/` -> only in `MedicationTapToAddSection.kt` | Low (check disabled-under-sheet requirement) | No | Replace with `RecordFilterChip` (or keep disabled color param) | CONFIRMED |
| D-10 | `ui/LabBatchScreen.kt:313-360` | Local `BatchTabChip` (`Surface` chip) similar to `RecordFilterChip` | 2 | `rg -n "\bBatchTabChip\b" app/src` -> `LabBatchScreen.kt:256,264,272,313` | Low | No | Evaluate replacing with `RecordFilterChip` | UNCERTAIN |
| D-11 | `ui/CatalogSettingsSections.kt:1198` & `ui/SecuritySettingsDialog.kt:919` | Local `CatalogLabeledField` & `SecurityLabeledField` duplicating `EditorField(externalLabel = true)` | 2 | `rg -n "(Catalog\|Security)LabeledField" app/src` -> 10 & 8 hits locally | Low | No (`Catalog`), L3 (`Security`) | Replace `CatalogLabeledField` with `EditorField`; report L3 | CONFIRMED |
| D-12 | `ui/AncApp.kt:172,186` & `ui/ClinicalBadges.kt:112,230` | Sub-12sp `labelSmall` (`AncApp:186`, `BpAlertChip:112`, `MedicationStatusBadge:230`) & hardcoded `"Clinic Logo"` (`AncApp:172` vs `R.string.clinic_logo_cd`) | 2 | `rg -n "labelSmall" app/src/main/.../ui/` -> active hits at `AncApp:186`, `ClinicalBadges:112,230` | Low | L3 (`AncApp`, `ClinicalBadges`) | Report only (L3) | CONFIRMED |

---

## Category 3: Duplicate Code (`tool_duplicates.txt` & Verified)

| ID | File:Line | What | Cat | Exact Grep / Command & Result | Risk & Why | Protected / L3 | Proposed Action | Status |
|---|---|---|---|---|---|---|---|---|
| DUP-01 | `ui/LabEditorComponents.kt:49-100` & `ui/VisitEditorSteps.kt:91-140` | `LabPatientBanner` & `VisitPatientBanner` duplicate `Surface` + `PatientNameText` + `StatusTag` + `BandChip` flow (Cluster C1) | 3 | Inspected both files: 18+ identical lines differing only in `FieldFamily` and `asOfDate` | Low | No | Extract canonical `PatientBannerCard` to `ui/components/` | CONFIRMED |
| DUP-02 | `ui/ClinicalAttachmentViewerDialog.kt:94-110` & `ui/UltrasoundPhotoViewerDialog.kt:109-124` | Duplicate full-width `Dialog`/`Card`/`verticalScroll` header structure (Cluster C4) | 3 | Inspected both files: identical 16-line card/header container | Low | L3 (`ClinicalAttachmentViewerDialog.kt`) | Report only (L3) | CONFIRMED |
| DUP-03 | `ui/patienthub/components/DeliveryPlanSheet.kt:107-122` & `ui/patienthub/components/MedicationSheet.kt:96-112` | Duplicate `ModalBottomSheet` header + scrollable body + `SheetSaveMessage` shell (Cluster C7) | 3 | Inspected both files: 15 identical structural lines | Low | No | Extract `EditorBottomSheetBody` into `EditorSheetParts.kt` | CONFIRMED |
| DUP-04 | `ui/CatalogSettingsSections.kt:1132-1197` & `ui/SecuritySettingsDialog.kt:842-918` | Duplicate `CatalogFormDialog` & `SecurityFormDialog` wrapper + `SheetCancelSaveRow` (Cluster C13) | 3 | Inspected both files: identical `Dialog`/`Surface`/scroll/`SheetCancelSaveRow` frame | Low | L3 (`SecuritySettingsDialog.kt`) | Report only (L3) | CONFIRMED |
| DUP-05 | `ui/patienthub/components/DeliveryOutcomeUi.kt:25-27` | `deliveryModeLabel` & `deliveryResultLabel` are trivial 1-line pass-throughs to `deliveryModeText` / `deliveryResultText` | 3 | `rg -n "delivery(Mode\|Result)Label" app/src` -> 3 hits each (`DeliveryOutcomeUi`, `DeliveryPlanSheet`) | Low | No | Inline `deliveryModeText`/`deliveryResultText` & delete wrappers | CONFIRMED |

*(Clusters C20 in `DesignSystem.kt` and C21 in `Components.kt` are self-duplicates inside dead code already covered by U-14 and U-15.)*

---

## Category 4: Unused Resources (`app/src/main/res`)

| ID | File:Line | What | Cat | Exact Grep / Command & Result | Risk & Why | Protected / L3 | Proposed Action | Status |
|---|---|---|---|---|---|---|---|---|
| R-01 | `res/values/strings.xml` (125 entries, lines 4–706) | 124 unused `<string>` + 1 unused `<plurals name="registration_count_existing">` (`demo_banner`, `workspace`, `profile`, `case_id`, `no_visits`, `recorded_visits`, `read_only`, `read_only_profile`, `prototype_placeholder`, `placeholder_body`, `obstetric_history`, `other_histories`, `episode_overview`, `visits`, `laboratory`, `medications`, `medications_tab`, `attachments`, `synthetic_edit`, `visit_editor`, `identity`, `secure_demo_storage`, `lock_demo_storage`, `credential_prompt_title`, `credential_prompt_subtitle`, `synthetic_patient_editor_notice`, `full_name`, `case_id_required`, `gpa_all_or_none`, `archived`, `archived_read_only_notice`, `no_previous_pregnancies`, `no_date_inference_notice`, `dating_editor_notice`, `add_ultrasound`, `ga_at_scan`, `no_ultrasound_recorded`, `ultrasound_editor_notice`, `active_pregnancy_episode`, `milestones`, `first_trimester`, `second_trimester`, `third_trimester`, `latest_scan`, `latest_visit`, `total_visits`, `no_lab_recorded`, `no_medications_recorded`, `invalid_ga`, `add_lab`, `edit_lab`, `lab_editor_notice`, `abnormal_badge`, `normal_badge`, `medication_editor_notice`, `end_date`, `clinical_summary`, `copy_clinical_summary`, `summary_copied`, `quick_actions`, `delivery_planning_notice`, `parity_format`, `normal_risk`, `create_visit_for_scan`, `add_scan_photo`, `detailed_biometry_toggle`, `add_another_medication`, `critical_badge`, `alert_badge`, `note_badge`, `standalone_scan`, `view_linked_ultrasound`, `view_linked_visit`, `calculated_ga_label`, `simulated_photo`, `advanced_med_options`, `patient_age_years`, `ga_prefix`, `edd_prefix`, `unrecorded_risk`, `edd_pending`, `expand_details`, `collapse_details`, `recent_visits`, `recent_scans`, `recent_labs`, `active_medications`, `stopped_medications`, `parity_summary`, `no_record`, `edit_history`, `attachment_save_failed`, `flag`, `flag_abnormal`, `note`, `note_filled`, `lab_preset_negative`, `lab_preset_non_reactive`, `lab_preset_normal`, `lab_preset_positive`, `collapse_all`, `expand_all`, `stopped_in_this_session`, `restart_medication`, `custom_drug_title`, `custom_drug_label`, `active_count_summary`, `other_date`, `same_as_last_fh`, `matches_ga_fh`, `fundal_height`, `flag_badge`, `lab_batch_review_n_results`, `lab_batch_review_one_result`, `delivered_patients_empty`, `registration_count_title`, `registration_count_label`, `registration_count_hint`, `registration_count_existing` [plurals], `registration_count_error`, `registration_resume_body`, `registration_resume_button`, `delete_attention_entry`, `attention_new_label`, `attention_add_button`) | 4 | Python scan across all `.kt`/`.java`/`.xml` in `app/src` (`main`, `test`, `androidTest`, `debug`, `res`, `AndroidManifest.xml`): 0 `R.string.*`/`R.plurals.*`/`@string/*`/`@plurals/*` hits; `rg -n "getIdentifier" app/src` -> 0 hits | Low (compile-checked on build) | No | Delete all 125 entries | CONFIRMED |
| R-02 | `res/drawable/ic_launcher_background.xml:1-10` | Unused vector drawable `ic_launcher_background` | 4 | `rg -n "ic_launcher_background" app/src` -> only `@color/ic_launcher_background` in `mipmap-anydpi-v26` & `values/ic_launcher_background.xml`; 0 `drawable` refs | Low | No | Delete | CONFIRMED |

---

## Category 5: Code-Comment Slop

| ID | File:Line | What | Cat | Exact Grep / Command & Result | Risk & Why | Protected / L3 | Proposed Action | Status |
|---|---|---|---|---|---|---|---|---|
| C-01 | `ui/Components.kt:124,193,201,207,212,229,232,241,255` | Numbered/row narration comments inside dead `ClinicalPatientCard` | 5 | `rg -n "^\s*//\s*[0-9]+\.\s" app/src/main/.../ui/` -> 7 hits in `Components.kt` | Low | No | Delete with U-14 | CONFIRMED |
| C-02 | `ui/TimelineSpine.kt:74,77` | Restating comments (`// Spine Rail spacer...`, `// Content Card...`) in dead `TimelineSpineItem` | 5 | Read `TimelineSpine.kt:74-80` | Low | No | Delete with U-11 | CONFIRMED |
| C-03 | `ui/PatientListScreen.kt:292` & `ui/patienthub/PatientHubScreen.kt:156` | Empty category-label comments (`// Save / Archive Notices`, `// Dialog state holders`) | 5 | `rg -n` comment scan across `ui/` | Low (comments only) | L3 (`PatientListScreen.kt` per `PLAN.md`) | Delete comment in `PatientHubScreen`; report L3 | CONFIRMED |

---

## Category 6: Dead Tests / Test Helpers Referencing Removed Things

| ID | File:Line | What | Cat | Exact Grep / Command & Result | Risk & Why | Protected / L3 | Proposed Action | Status |
|---|---|---|---|---|---|---|---|---|
| T-01 | `androidTest/.../ClinicalPatientCardTest.kt:1-74` | Entire test class exercises dead `ClinicalPatientCard` & `PatientCardVariant` | 6 | `rg -n "\bClinicalPatientCard\b" app/src` -> 0 `main` callers | Medium (must remove with U-14) | No | Delete with U-14 | CONFIRMED |
| T-02 | `androidTest/.../DatingGaMarkerSurfacesTest.kt:27-28,114-122` | `headerCardShowsMarkerOnlyWhenDiscordant` tests dead `ClinicalPatientCard` | 6 | `rg -n "\bClinicalPatientCard\b" app/src` -> 0 `main` callers | Medium (rewrite to test `PatientHubHeader` if needed) | No | Remove/migrate test with U-14 | CONFIRMED |
| T-03 | `androidTest/.../OverviewTabSectionsTest.kt:31,477,489,497` | Tests `GestationalAgeBadge` directly (0 `main` callers outside dead `ClinicalPatientCard`) | 6 | `rg -n "\bGestationalAgeBadge\b" app/src` -> 0 active `main` callers | Medium | No | Remove/migrate to `BandChip` | CONFIRMED |
| T-04 | `test/.../ui/ThemeContrastTest.kt:51,75-129,156-188` | Asserts contrast on dead `Tag*` and unused `ClinicalCardTokens.*` tokens | 6 | Read `ThemeContrastTest.kt` lines 51-188 vs D-03/D-04 greps | Medium (update in lockstep with D-03/D-04) | No | Remove dead token pairs & check `Field*`/`Status*` | CONFIRMED |

---

## Protected, Report Only (No Removal/Edits Proposed)

| ID | File:Line | What | Cat | Exact Grep / Command & Result | Protected Area | Status |
|---|---|---|---|---|---|---|
| P-01 | `model/ClinicalModels.kt:5` | Unused import `java.time.temporal.ChronoUnit` | 1 | Symbol scan on file: `hits=0` | `model/`, `ClinicalModels` | CONFIRMED |
| P-02 | `state/AncState.kt:10-14` | 5 unused imports (`PreviousPregnancyFormRules`, `effectiveMedicationStatus`, `parseBloodPressure`, `parseVisitFindings`, `ClinicalRecordKind`) | 1 | Symbol scan on file: `hits=0` | `state/` (`AncState`) | CONFIRMED |
| P-03 | `state/AncViewModel.kt:35-36` | 2 unused imports (`Job`, `delay`) | 1 | Symbol scan on file: `hits=0` | `state/` | CONFIRMED |
| P-04 | `storage/ClinicalCommands.kt:4-5` | 2 unused imports (`DemoLabInvestigation`, `DemoMedicationRecord`) | 1 | Symbol scan on file: `hits=0` | `storage/` | CONFIRMED |
| P-05 | `storage/RecordsRepository.kt:15,17,18` | 3 unused imports (`PatientVocabulary`, `LocalDate`, `DateTimeParseException`) | 1 | Symbol scan on file: `hits=0` | `storage/`, `RecordsRepository` | CONFIRMED |
| P-06 | `ui/report/PatientPrintHtmlGenerator.kt:10-11` | 2 unused imports (`GestationalAgeEngine`, `PreviousPregnancyFormRules`) | 1 | Symbol scan on file: `hits=0` | `*Generator*` | CONFIRMED |
| P-07 | `security/SecuritySettingsRepository.kt:149` | Unreferenced `setAutoLockTimeout` | 1 | `rg -n "\bsetAutoLockTimeout\b" app/src` -> 1 hit | `security/` | CONFIRMED |
| P-08 | `storage/AttachmentSyncManager.kt:26,49` | Unreferenced `localizedStatus`, `computeStats` | 1 | `rg -n "\b(localizedStatus\|computeStats)\b" app/src` -> 1 hit each | `storage/` | CONFIRMED |
| P-09 | `storage/CameraCaptureHelper.kt:26` | Unreferenced `createCameraTempUri` | 1 | `rg -n "\bcreateCameraTempUri\b" app/src` -> 1 hit | `storage/` | CONFIRMED |
| P-10 | `storage/PatientAttachmentVault.kt:96` | Unreferenced `saveAttachmentFromStream` | 1 | `rg -n "\bsaveAttachmentFromStream\b" app/src` -> 1 hit | `storage/` | CONFIRMED |
| P-11 | `storage/VaultFiles.kt:149,151,152` | Unreferenced `hasEnvelope`, `hasDatabase`, `hasIncompletePointerArtifact` | 1 | `rg -n "\b(hasEnvelope\|hasDatabase\|hasIncompletePointerArtifact)\b" app/src` -> 1 hit each | `storage/` | CONFIRMED |
| P-12 | `storage/RecordsRepository.kt` (C2,C3,C6,C9,C10,C12,C15,C16,C17) | 9 duplicate clusters (entity/domain mapping & query blocks) | 3 | `tool_duplicates.txt` clusters C2–C17 | `storage/`, `RecordsRepository` | CONFIRMED |
| P-13 | `storage/RecordsMigration.kt` (C5,C18) | 2 duplicate clusters (6-way migration queries & table DDL) | 3 | `tool_duplicates.txt` clusters C5, C18 | `storage/` | CONFIRMED |
| P-14 | `model/DemoModels.kt` / `ClinicalModels.kt` vs `storage/PatientCommands.kt` / `ClinicalCommands.kt` (C8,C11,C19) | 3 duplicate field-list clusters | 3 | `tool_duplicates.txt` clusters C8, C11, C19 | `model/`, `storage/` | CONFIRMED |
| P-15 | `state/AncViewModel.kt` vs `storage/RecordsRepository.kt` (C14,C23) | 2 duplicate medication-command mapping clusters | 3 | `tool_duplicates.txt` clusters C14, C23 | `state/`, `storage/` | CONFIRMED |
| P-16 | `ui/report/PatientPrintHtmlGenerator.kt:1119-1128,1138-1147` (C22) | Duplicate HTML medication table header | 3 | `tool_duplicates.txt` cluster C22 | `*Generator*` | CONFIRMED |
| P-17 | `storage/EncryptedBackupManager.kt:50-60,67-77` (C24) | Duplicate `canonicalEnvelope` + `encryptEnvelope` call | 3 | `tool_duplicates.txt` cluster C24 | `storage/` | CONFIRMED |
| P-18 | `model/VisitEntryLogic.kt:6,8,21,23,85,87,121,123,130,132,286,288,334,336,385,387` | 16 decorative `// ---...` separator lines | 5 | `rg -n` separator scan on `app/src/main/java` | `model/` | CONFIRMED |
| P-19 | `model/DeliveryNotes.kt:81,120,181,255` | 4 decorative `// ---...` banner comments | 5 | `rg -n` separator scan on `app/src/main/java` | `model/` | CONFIRMED |
| P-20 | `model/catalog/ClinicalCatalogRepository.kt:83,85,136,138,182,184,236,238,356,358` | 10 decorative `// ===...` separator lines | 5 | `rg -n` separator scan on `app/src/main/java` | `model/` | CONFIRMED |

---

## Not Verified
- **Gradle compilation, unit tests (`./gradlew test`), and instrumented tests (`androidTest`)**: Not run per read-only rules in `BRIEF.md`; compile-safety and test impact of deletions are inferred from `rg` reference checks.
- **Short duplicate blocks (<10 significant lines)** outside `tool_duplicates.txt` and **all private/internal functions inside `ui/` not flagged by `find_unused.py`**: Only targeted old-design-system and `ui/components/` symbols were grepped exhaustively; other local helpers in large files (`VisitEditor`, `UltrasoundEditor`, `LabBatchScreen`, `ReportSections`) may exist.
- **Line-by-line prose review of every comment/KDoc across all 166 files** for subtle over-explanation or signature echoes (beyond automated regex scans and read files).
- **Unused test helpers inside `app/src/test` and `app/src/androidTest`** that do not reference removed UI symbols (only tests referencing old/removed UI symbols were audited).
- **Kotlin reflection / R8 / ProGuard keep rules** (`proguard-rules.pro`) or Gradle build scripts (`app/build.gradle*`) outside `app/src`.
