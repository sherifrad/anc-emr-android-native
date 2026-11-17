# Slice 21 Cleanup — Revised Plan (Team B)

**Author:** Checker B2 (adversarial audit of B1 findings, cross-checked against codebase)  
**Branch:** `redesign-2026-11-15` | **Scope:** Read-Only Verification across `app/src/` (`main`, `test`, `androidTest`, `debug`), `res/`, `AndroidManifest.xml`

---

## 1. Audit Summary & Verdict Statistics

Every row from `findings_B1.md` (63 total: 17 unused, 12 design leftovers, 5 duplicates, 2 resources, 3 comments, 4 tests, 20 protected) was independently audited with adversarial grep checks.

- **AGREE (60 rows):** Independent reference checks confirm 0 live callers or confirm valid replacement/report-only status.
- **REJECT (1 row):** `D-05 (ActionRose)` — proposed deletion of `ActionRose` rejected; `ActionRose` is live in `LightColorScheme.secondary` (`Theme.kt:236`) and `PatientDirectoryMetricsRow.kt:172`.
- **DOWNGRADE (2 rows):** `D-09` (`pickerFilterChipColors` in `MedicationTapToAddSection`) and `D-10` (`BatchTabChip` in `LabBatchScreen`) downgraded to UNCERTAIN; both feature intentional bespoke behavior/styling (sheet dimming and 2-line primary tab) that standard `RecordFilterChip` does not support without risk of regression.
- **NEW (8 rows):** 8 significant findings missed by B1 discovered and verified (`PatientListScreen.kt` 22 unused imports, `SegmentedProgressBar` unreferenced composable, `summaryPillText`, 5 launcher foreground PNGs, `narrowWindow` dead test helper, `composedNotes` dead unit test helper, 2 unreferenced Room DAO queries, and changelog comment in `strings.xml`).

---

## 2. Numbered Batch Plan Ordered by Risk

### Batch 1: Pure Unused Imports & Dead Private Members (Lowest Risk)
*Compile verification command:* `./gradlew compileDebugKotlin compileDebugUnitTest compileDebugAndroidTest`

| ID | File:Line | Target Symbol(s) | Evidence Command & Result | Proposed Action |
|---|---|---|---|---|
| U-01 | `ui/AncApp.kt:10,17,30` | `Spacer`, `width`, `OutlinedButton` | Body scan: 0 refs outside imports (L3 pure import) | Delete imports |
| U-02 | `ui/ClinicalBadges.kt:8,10,24,25` | `Column`, `fillMaxWidth`, `Color`, `luminance` | Body scan: 0 refs outside imports (L3 pure import) | Delete imports |
| U-03 | `ui/Components.kt:6,7,15,17,27,58` | `background`, `clickable`, `fillMaxSize`, `height`, `AccountCircle`, `TextAlign` | Body scan: 0 refs outside imports | Delete imports |
| U-04 | `ui/patientdirectory/PatientDirectoryCard.kt:24` | `Text` | Body scan: 0 refs outside import | Delete import |
| U-05 | `ui/patientdirectory/PatientDirectoryMetricsRow.kt:16` | `Color` | Body scan: 0 refs outside import | Delete import |
| U-06 | `ui/patienthub/tabs/VisitsTab.kt:6,8,9` | `Row`, `fillMaxWidth`, `padding` | Body scan: 0 refs outside imports | Delete imports |
| NEW-01 | `ui/PatientListScreen.kt:4..82` | 22 unused imports (`background`, `clickable`, `FlowRow`, `height`, `RoundedCornerShape`, `AccountCircle`, `Add`, `Edit`, `EditNote`, `MoreVert`, `Unarchive`, `Warning`, `CardDefaults`, `HorizontalDivider`, `ModalBottomSheet`, `OutlinedButton`, `Surface`, `clip`, `Color`, `lerp`, `luminance`, `SevereBpAttention`) | Body scan: 0 refs outside imports for all 22 (L3 pure imports) | Delete imports |
| U-12 | `ui/patienthub/components/ClinicalAttentionBanner.kt:94,98,101` | `ClinicalAttentionColors` alias, `clinicalAttentionColors()`, `AttentionColors.iconColor` | `rg -n` across `app/src`: 1 hit each (declaration only) | Delete declarations |
| U-17 | `ui/patienthub/components/ClinicalAttentionBanner.kt:92,93,95` | `AttentionColors.containerColor`, `textColor`, `borderColor` | `rg -n "\.(containerColor\|textColor\|borderColor)\b" app/src`: 0 hits on `AttentionColors` (only hit on M3 `ChipColors`) | Delete accessor getters |
| NEW-02 | `ui/DesignSystem.kt:292` | `fun SegmentedProgressBar` | `rg -n "\bSegmentedProgressBar\b" app/src`: 0 call sites across codebase (superseded by `VisitStepHeader`) | Delete composable |
| NEW-03 | `ui/patienthub/model/LabReportGrouping.kt:22` | `CompositeTestGroup.summaryPillText` | `rg -n "\bsummaryPillText\b" app/src`: 1 hit (declaration only) | Delete property |
| D-05 (part) | `ui/Theme.kt:25` | `val ClinicalRose` | `rg -n "\bClinicalRose\b" app/src`: 1 hit (`Theme.kt:25` declaration) | Delete token |
| DUP-05 | `ui/patienthub/components/DeliveryOutcomeUi.kt:25-27` | `deliveryModeLabel`, `deliveryResultLabel` | `rg -n "delivery(Mode\|Result)Label"`: 3 hits each (1-line pass-through wrappers to `deliveryModeText`/`deliveryResultText`) | Inline & delete wrappers |

---

### Batch 2: Unused Resources (Low Risk)
*Compile verification command:* `./gradlew mergeDebugResources compileDebugSources`

| ID | File:Line | Target Resource(s) | Evidence Command & Result | Proposed Action |
|---|---|---|---|---|
| R-01 | `res/values/strings.xml:4..706` | 124 `<string>` + 1 `<plurals name="registration_count_existing">` (including `demo_banner`, `workspace`, `case_id`, `no_visits`, `read_only`, `prototype_placeholder`, `synthetic_patient_editor_notice`, `invalid_ga`, `parity_format`, `advanced_med_options`, etc.) | Regex scan across all `.kt`, `.java`, `.xml` in `app/src`: 0 `R.string.*` / `R.plurals.*` / `@string/*` / `@plurals/*` hits; `rg -n "getIdentifier"` -> 0 hits | Delete all 125 entries from `strings.xml` |
| R-02 | `res/drawable/ic_launcher_background.xml` | Vector template `ic_launcher_background` | `rg -n "ic_launcher_background" app/src`: mipmaps use `@color/ic_launcher_background`, 0 refs to `@drawable/` or `R.drawable.` | Delete file |
| NEW-04 | `res/mipmap-{hdpi,mdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_foreground.png` | 5 launcher foreground PNGs | `rg -n "ic_launcher_foreground" app/src`: 0 refs (adaptive icons use `@drawable/clinic_logo_foreground`) | Delete 5 PNG files |

---

### Batch 3: Old Design Leftovers & Duplicate Components (Medium Risk)
*Compile verification command:* `./gradlew compileDebugKotlin testDebugUnitTest`

| ID | File:Line | Target Entity / Leftover | Evidence Command & Result | Proposed Action |
|---|---|---|---|---|
| U-07 | `ui/DesignSystem.kt:57` | `fun SectionCard` | `rg -n "\bSectionCard\b" app/src`: 1 hit (declaration only; replaced by `RecordSectionCard`) | Delete composable |
| U-08 | `ui/DesignSystem.kt:253` | `fun NoticeCard` | `rg -n "\bNoticeCard\b" app/src`: 1 hit (declaration only) | Delete composable |
| U-09 | `ui/DesignSystem.kt:488` | `fun SectionHeader` | `rg -n "\bSectionHeader\b" app/src`: 1 hit (declaration only) | Delete composable |
| U-11 | `ui/TimelineSpine.kt:17` | `fun TimelineSpineItem` | `rg -n "\bTimelineSpineItem\b" app/src`: 1 hit (declaration only) | Delete file |
| U-14 | `ui/Components.kt:79-353` | `ClinicalPatientCard` & `PatientCardVariant` | `rg -n "\bClinicalPatientCard\b" app/src`: 0 `main` callers (only dead tests `ClinicalPatientCardTest`, `DatingGaMarkerSurfacesTest`) | Delete after Batch 5 (T-01, T-02) |
| U-15 | `ui/DesignSystem.kt:98,346,539,658,714` | Dead `useRecordStyle = false` branches in `SummaryRow`, `ValueStepper`, `BatchInfoBar`, `BatchSegmentedProgress`, `QualitativeChoiceButton` | `rg -n "useRecordStyle" app/src/`: 100% of live callers pass `useRecordStyle = true` | Remove `useRecordStyle` param & delete `false` branches |
| U-16 | `ui/Components.kt:519-526` | Dead `useRecordStyle = false` branch in `GenericEditorErrorCard` | `rg -n "GenericEditorErrorCard" app/src/`: all 8 callers pass `useRecordStyle = true` | Remove `useRecordStyle` param & delete `false` branch |
| D-03 | `ui/Theme.kt:30-116` | 48 old `Tag*` color tokens (`TagObstetric*`, `TagGestationalAge*`, `TagBlood*`, `TagAttention*`, `TagHighRisk*`, `TagModerateRisk*`, `TagNormalRisk*`, `TagAge*`) | `rg -n "\bTag(Obstetric\|...)" app/src`: 0 live `main` callers outside dead `ClinicalBadges` + `ThemeContrastTest` | Delete with D-01/D-02 & T-04 |
| D-04 | `ui/Theme.kt:162-222` | 40 unused `ClinicalCardTokens.*` (`Dating*` 8, `Parity*` 8, `Scan*` 8, `Allergy*` 8, `CriticalBg/Border/Text` 6, `WarningBorder` 2) | `rg -n "ClinicalCardTokens\." app/src`: 0 live `main` callers outside `ThemeContrastTest` (live code uses only `Warning*` & `CriticalAccent`) | Delete 40 tokens; update T-04 |
| D-06 | `ui/AddAttachmentDialog.kt:170,190` | Raw `FilterChip` for attachment category & quality presets | `rg -n "FilterChip\(" AddAttachmentDialog.kt`: 2 call sites without `minimumTouchTarget()` | Replace with `RecordFilterChip` |
| D-07 | `ui/patienthub/components/MedicationForm.kt:171,192,203` | `ChoiceGroup` without `useRecordChip = true`, raw `FilterChip` for frequency | Inspected lines 171-205: only `MedicationForm` omits `useRecordChip` | Set `useRecordChip = true`; replace raw `FilterChip` with `RecordFilterChip` |
| D-08 | `ui/patienthub/components/DeliveryPlanSheet.kt:284,316` | Raw `FilterChip` for delivery mode & outcome | `rg -n "FilterChip\(" DeliveryPlanSheet.kt`: 2 call sites without `minimumTouchTarget()` | Replace with `RecordFilterChip` |
| D-11 | `ui/CatalogSettingsSections.kt:1198` | `CatalogLabeledField` | Inspected lines 1198-1230: duplicates `EditorField(externalLabel = true)` | Replace with canonical `EditorField` |
| DUP-01 | `ui/LabEditorComponents.kt:49-100` vs `ui/VisitEditorSteps.kt:91-140` | `LabPatientBanner` & `VisitPatientBanner` | Inspected both files: 18+ identical lines of patient identity + badges | Extract canonical `EditorPatientBanner` in `ui/components/` |
| DUP-03 | `ui/patienthub/components/DeliveryPlanSheet.kt:107-122` vs `ui/patienthub/components/MedicationSheet.kt:96-112` | Bottom sheet header + scrollable body + `SheetSaveMessage` shell | Inspected both files: 15 identical lines of layout chrome | Extract shared bottom sheet body in `EditorSheetParts.kt` |

---

### Batch 4: Code-Comment Slop (Zero Code Risk)
*Compile verification command:* `./gradlew compileDebugKotlin`

| ID | File:Line | Target Comment(s) | Evidence Command & Result | Proposed Action |
|---|---|---|---|---|
| C-01 | `ui/Components.kt:124,193,201,207,212,229,232,241,255` | Numbered UI narration comments (`// 1. Risk Level Badge`..`// 7. Allergy Status`) | Inspected `Components.kt`: lives inside dead `ClinicalPatientCard` | Deleted automatically with U-14 |
| C-02 | `ui/TimelineSpine.kt:74,77` | Restating comments (`// Spine Rail spacer...`, `// Content Card...`) | Inspected `TimelineSpine.kt`: lives inside dead `TimelineSpineItem` | Deleted automatically with U-11 |
| C-03 | `ui/patienthub/PatientHubScreen.kt:156` | Empty category comment `// Dialog state holders` | `rg -n "Dialog state holders"` in `PatientHubScreen.kt` | Delete comment line |
| NEW-08 | `res/values/strings.xml:1032` | Changelog comment `<!-- Slice 20: Two-page clinical report dialog -->` | `rg -n "Slice 20:" app/src/main/res/values/strings.xml` | Delete comment line |

---

### Batch 5: Dead Tests & Test Helpers (Medium Risk, Run in Lockstep)
*Compile & test verification command:* `./gradlew testDebugUnitTest connectedDebugAndroidTest`

| ID | File:Line | Target Test / Helper | Evidence Command & Result | Proposed Action |
|---|---|---|---|---|
| T-01 | `app/src/androidTest/.../ClinicalPatientCardTest.kt:1-74` | Entire test class (`ClinicalPatientCardTest`) | `rg -n "\bClinicalPatientCard\b" app/src/main/`: 0 callers | Delete test file alongside U-14 |
| T-02 | `app/src/androidTest/.../DatingGaMarkerSurfacesTest.kt:27-28,114-122` | Method `headerCardShowsMarkerOnlyWhenDiscordant` | Lines 114-122 test dead `ClinicalPatientCard` variants | Delete test method & dead imports |
| T-03 | `app/src/androidTest/.../OverviewTabSectionsTest.kt:31,477,489,497` | Assertions testing `GestationalAgeBadge` directly | `rg -n "\bGestationalAgeBadge\b" app/src/main/`: 0 live callers outside dead `ClinicalPatientCard` | Migrate to `BandChip` or remove |
| T-04 | `app/src/test/.../ui/ThemeContrastTest.kt:51,75-129,156-188` | Contrast assertions reading dead `Tag*` and `ClinicalCardTokens.*` | Checked lines 51-188: fails if D-03/D-04 tokens are deleted | Update test list to assert live tokens only |
| NEW-05 | `app/src/androidTest/.../LabEditorRedesignTest.kt:275-298` | Helper `narrowWindow(fontScale)`, fields `savedConfig`/`savedMetrics`, `restoreResources()` | `rg -n "\bnarrowWindow\b"`: 0 callers in file (`Box(requiredSize)` used instead) | Delete dead helper & fields |
| NEW-06 | `app/src/test/.../state/DeliveryOutcomeStateTest.kt:23-30` | `private fun composedNotes` | `rg -n "\bcomposedNotes\b"` in `DeliveryOutcomeStateTest.kt`: 1 hit (declaration only) | Delete unused helper |

---

## 3. Protected & Level-3 Report-Only Section (`BRIEF.md:7-8`)

*These items are confirmed dead/duplicated by audit but MUST NOT be deleted or edited per project safety rules.*

### Protected Areas (Report Only — No Edits Proposed)
- **P-01 (`model/ClinicalModels.kt:5`):** Unused import `java.time.temporal.ChronoUnit`.
- **P-02 (`state/AncState.kt:10-14`):** 5 unused imports (`PreviousPregnancyFormRules`, `effectiveMedicationStatus`, `parseBloodPressure`, `parseVisitFindings`, `ClinicalRecordKind`).
- **P-03 (`state/AncViewModel.kt:35-36`):** 2 unused imports (`Job`, `delay`).
- **P-04 (`storage/ClinicalCommands.kt:4-5`):** 2 unused imports (`DemoLabInvestigation`, `DemoMedicationRecord`).
- **P-05 (`storage/RecordsRepository.kt:15,17,18`):** 3 unused imports (`PatientVocabulary`, `LocalDate`, `DateTimeParseException`).
- **P-06 (`ui/report/PatientPrintHtmlGenerator.kt:10-11`):** 2 unused imports (`GestationalAgeEngine`, `PreviousPregnancyFormRules`).
- **P-07 (`security/SecuritySettingsRepository.kt:149`):** Unreferenced `setAutoLockTimeout` method.
- **P-08 (`storage/AttachmentSyncManager.kt:26,49`):** Unreferenced `localizedStatus`, `computeStats`.
- **P-09 (`storage/CameraCaptureHelper.kt:26`):** Unreferenced `createCameraTempUri`.
- **P-10 (`storage/PatientAttachmentVault.kt:96`):** Unreferenced `saveAttachmentFromStream`.
- **P-11 (`storage/VaultFiles.kt:149,151,152`):** Unreferenced `hasEnvelope`, `hasDatabase`, `hasIncompletePointerArtifact`.
- **NEW-07 (`storage/RecordsDao.kt:492,512`):** Unreferenced DAO queries `attachmentsForPatient`, `updateAttachmentSyncStatus`.
- **P-12 to P-17:** Storage/Model duplicate clusters (`RecordsRepository` 9 clusters, `RecordsMigration` 2 clusters, Model/Command serialization 3 clusters, ViewModel/Repository mapping 2 clusters, `PatientPrintHtmlGenerator` medication table header cluster, `EncryptedBackupManager` envelope crypto cluster).
- **P-18 to P-20:** Decorative ASCII divider comments (`VisitEntryLogic.kt` 16 dividers, `DeliveryNotes.kt` 4 banners, `ClinicalCatalogRepository.kt` 10 banners).

### Level-3 Files (Report Only — Requires L3 Sign-Off for Code Changes)
- **U-10 (`ui/PatientEditor.kt:619`):** Unreferenced `internal fun EditorActions`.
- **U-13 (`ui/ClinicalPresets.kt:67-77`):** Unreferenced `object ClinicalPresets` (`commonBloodPressures`, `commonVisitFindings`).
- **U-16 (part, `ui/ClinicalPresets.kt:95-142` & `ui/PatientEditor.kt:401-484`):** Dead `useRecordStyle = false` branches in `QuickPresetChips` and `EditorPage`.
- **D-01 (`ui/ClinicalBadges.kt:121,185`):** `ClinicalTagBadge` and `ClinicalRiskBadge` (called only by dead `ClinicalPatientCard`).
- **D-02 (`ui/ClinicalBadges.kt:43,246,288,345,359`):** `PatientAgeBadge`, `BloodGroupBadge`, `GestationalAgeBadge`, `ObstetricParityBadge` (0 active `main` callers).
- **D-11 (part, `ui/SecuritySettingsDialog.kt:919`):** Local `SecurityLabeledField` duplicating `EditorField`.
- **D-12 (`ui/AncApp.kt:172,186` & `ui/ClinicalBadges.kt:112,230`):** Sub-12sp `labelSmall` text and hardcoded `"Clinic Logo"` string.
- **DUP-02 (`ui/ClinicalAttachmentViewerDialog.kt:94-110`):** Full-width viewer dialog card/header duplicate with `UltrasoundPhotoViewerDialog`.
- **DUP-04 (`ui/SecuritySettingsDialog.kt:842-918`):** Form dialog frame duplicate with `CatalogSettingsSections`.
- **C-03 (part, `ui/PatientListScreen.kt:292`):** Empty comment `// Save / Archive Notices`.

---

## 4. Rejected and Why

### D-05 (`ActionRose` Deletion Proposal): REJECTED
- **Finder B1 Finding:** Proposed deleting `ActionRose` (`Theme.kt:26`) alongside `ClinicalRose` and replacing its call in `PatientHubHeader.kt:221,471` with `colorScheme.error`.
- **Checker B2 Adversarial Refutation:**
  1. `rg -n "\bActionRose\b" app/src` reveals that `ActionRose` is wired directly into `LightColorScheme` in `app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt:236`:
     ```kotlin
     secondary = ActionRose,
     ```
  2. `ActionRose` is also actively used for severe blood pressure styling in `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryMetricsRow.kt:172`:
     ```kotlin
     color = if (row.isSevereBp) (if (isDarkTheme()) StatusHighDarkText else ActionRose) else MaterialTheme.colorScheme.onSurface,
     ```
  3. Removing or renaming `ActionRose` breaks the compilation of `Theme.kt` and `PatientDirectoryMetricsRow.kt` and mutates the core Material 3 light theme color hierarchy.
  4. **Verdict:** REJECT deletion of `ActionRose`. Only `ClinicalRose` (`Theme.kt:25`) has 0 references and is safe to delete.

---

## 5. Not Verified

1. **Full Gradle Compilation & Test Execution**: In accordance with the READ-ONLY mandate of `BRIEF.md:6`, no `./gradlew` build, unit test execution, or instrumentation run was performed. All reference conclusions rest on exhaustive ripgrep and AST token verification.
2. **Downgraded Rows Requiring Compile/Visual Proof**:
   - **D-09 (`ui/patienthub/components/MedicationTapToAddSection.kt:258,267`):** Uses custom `pickerFilterChipColors`/`pickerFilterChipBorder` that specifically handles the dimmed state when the medication sheet is open (`disabledContainerColor = Color.Transparent`, `disabledSelectedContainerColor = scheme.secondaryContainer`). Canonical `RecordFilterChip` lacks this support.
   - **D-10 (`ui/LabBatchScreen.kt:313-360`):** Local `BatchTabChip` implements a two-line centered layout (`maxLines = 2`, `TextAlign.Center`) and solid primary fill (`color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface`). Blindly replacing it with single-line `RecordFilterChip` risks severe visual regression on narrow 360dp viewports.
3. **Line-by-Line Body Prose Review**: Prose comments and KDoc across unaffected clinical model and storage files were not exhaustively parsed for subjective slop.
4. **Internal Logic in Protected Modules**: Code within `model/`, `storage/`, and `state/` was flagged via candidate tools and reference audits without evaluating behavioral consequences of internal refactoring.
