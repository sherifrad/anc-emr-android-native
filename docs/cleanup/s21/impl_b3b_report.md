# Slice 21 Cleanup — Batch 3b Implementation Report

## 1. Items Done

### 1.1 Dead Colour Tokens Removed (`app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt`)
Verified 0 references in `app/src/main` outside `Theme.kt`, 0 internal references from kept tokens in `Theme.kt`, and 0 references in `app/src/debug` or `app/src/androidTest` before deletion:
- **Top-level token (1):** `ClinicalRose`.
- **Top-level `Tag*` tokens (65 total, plus their 4 section/inline comments):**
  - Light (33): `TagObstetric{Bg,Border,Text}`, `TagGestationalAge{Bg,Border,Text}`, `TagBloodPositive{Bg,Border,Text}`, `TagBloodNegative{Bg,Border,Text}`, `TagBloodNeutral{Bg,Border,Text}`, `TagAttention{Bg,Border,Icon,Text}`, `TagHighRisk{Bg,Border,Text}`, `TagModerateRisk{Bg,Border,Text}`, `TagNormalRisk{Bg,Border,Text}`, `TagAge{Bg,Border,Text,LabelBg,LabelText}`.
  - Dark (32): `TagObstetricDark{Bg,Border,Text}`, `TagGestationalAgeDark{Bg,Border,Text}`, `TagBloodPositiveDark{Bg,Border,Text}`, `TagBloodNegativeDark{Bg,Border,Text}`, `TagBloodNeutralDark{Bg,Border,Text}`, `TagAttentionDark{Bg,Border,Icon,Text}`, `TagHighRiskDark{Bg,Border,Text}`, `TagModerateRiskDark{Bg,Border,Text}`, `TagNormalRiskDark{Bg,Border,Text}`, `TagAgeDark{Bg,Border,Text}`, `TagAgeLabelDarkBg`.
- **`object ClinicalCardTokens` members (40 total, plus `Dating`/`Parity`/`Scan`/`Allergy` group comments):**
  - `Dating{Bg,Border,Accent,Text,DarkBg,DarkBorder,DarkAccent,DarkText}` (8)
  - `Parity{Bg,Border,Accent,Text,DarkBg,DarkBorder,DarkAccent,DarkText}` (8)
  - `Scan{Bg,Border,Accent,Text,DarkBg,DarkBorder,DarkAccent,DarkText}` (8)
  - `Critical{Bg,Border,Text,DarkBg,DarkBorder,DarkText}` (6)
  - `WarningBorder`, `WarningDarkBorder` (2)
  - `Allergy{Bg,Border,Accent,Text,DarkBg,DarkBorder,DarkAccent,DarkText}` (8)

### 1.2 Contrast Test Alignment (`app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt`)
- Updated top KDoc and `expectedPairs()` KDoc to remove stale `age badge` / `PatientAgeBadge` references.
- Updated `parseColorTokens()` sanity check from `"TagAgeText"` to live `"FieldDeliveryLabel"`.
- Removed deleted `TagAge*` entries from `expectedPairs()`, leaving live `FieldDeliveryLabel`/`FieldDeliveryBg` and `FieldDeliveryDarkLabel`/`FieldDeliveryDarkBg`.
- Removed `statusTagPairs()`, `statusTags_everyTextBackgroundPair_meetsWcagAa_4_5()`, and `highRiskTag_lightText_meetsWcagAa_4_5()` (all referenced `Tag*` tokens were deleted).
- Filtered `clinicalCardTokens_everyTextContainerPair_meetsWcagAa_4_5()` to retain only live token pairs (`WarningText`/`WarningBg`, `WarningDarkText`/`WarningDarkBg`, `WarningDarkAccent`/`WarningDarkBg`) and removed pairs referencing deleted tokens.

### 1.3 Orphan Strings (`app/src/main/res/values/strings.xml`)
Verified 0 references across `app/src` (including `main`, `test`, `androidTest`, `debug`) and 0 `getIdentifier` calls:
- Deleted `<string name="normal_risk_filter">Normal Risk</string>` and `<string name="print_patient_report">Print Patient Report</string>`.

### 1.4 Comment Slop (Non-Level-3 Files Only)
Removed generic restating comments without modifying any code line:
- `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt` (5 comments): `// Top Action Bar`, `// Patient Name`, `// Clinical badges / tags flow row`, `// Facts row`, `// Compact Clinical Attention Strip (36dp min height)`. Preserved WHY/architecture comments (fade math, `Modifier.layout` zero-recomposition note, intrinsic measurement note, Layer B ghost-touch note, pinned tab bar note, KDoc).
- `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubScreen.kt` (4 comments): `// Dialog state holders`, `// Collapsible Header (Hero to Sticky Bar)`, `// Horizontal Pager with 6 modular tabs`, `// Modal Dialog Hosts`. Preserved WHY/contract comments (pager settledPage note, nested scroll ordering, sheet scrim/route notes, attachment delete confirmation note).

---

## 2. Items Skipped (Live References Found or Already Absent)

- **`ActionRose` (`Theme.kt`):** Skipped — referenced in `Theme.kt` (`LightColors.secondary`), `DesignSystem.kt` (7 hits), `PatientDirectoryMetricsRow.kt` (lines 22, 171), and `PatientHubHeader.kt` (lines 58, 221, 471).
- **`Field*` and `Status*` tokens (`Theme.kt`):** Skipped — referenced in `ui/components/RecordTokens.kt`, `ui/components/StatusTag.kt`, `ui/patienthub/PatientHubHeader.kt`, `ui/VisitEditorSteps.kt`, `ui/AncApp.kt`, `ui/UltrasoundEditor.kt`, `ui/VaultGateScreen.kt`, `ui/AppLockScreen.kt`, `ui/InAppCameraDialog.kt`, `ui/report/ReportSections.kt`, `ui/patienthub/components/ClinicalAttentionBanner.kt`, `ui/patientdirectory/PatientDirectoryMetricsRow.kt`.
- **`ClinicalCardTokens.CriticalAccent` & `CriticalDarkAccent` (`Theme.kt`):** Skipped — referenced in `ui/ClinicalBadges.kt:101` (`GaDiscordanceMarker`).
- **`ClinicalCardTokens.WarningBg`, `WarningAccent`, `WarningText`, `WarningDarkBg`, `WarningDarkAccent`, `WarningDarkText` (`Theme.kt`):** Skipped — referenced in `ui/patienthub/components/OverviewSections.kt:157-159`.
- **Slice 20 changelog XML comment (`app/src/main/res/values/strings.xml`):** Not present (`rg -n 'Slice 20' app/src` returned 0 matches; only `<!-- Delivery outcome (delivered flag, date, mode, outcome) -->` at line 551 is present and kept).

---

## 3. Commands and Results

- Reference verification (before edits):
  - Python symbol scan across `app/src` (`main`, `test`, `androidTest`, `debug`) for all `Theme.kt` top-level `Tag*`/`ClinicalRose`/`ActionRose`/`Field*`/`Status*` and all 48 `ClinicalCardTokens` members -> exit 0; confirmed 0 `app/src/main` references outside `Theme.kt` for all 106 deleted tokens.
  - `rg -n '\b(Tag\w+|ClinicalRose|ClinicalCardTokens|ClinicalIvory|ClinicalTeal|ClinicalNavy)\b' app/src/test app/src/androidTest` -> exit 0; confirmed `ThemeContrastTest.kt` was the only test file referencing deleted tokens.
  - `rg -n '\b(normal_risk_filter|print_patient_report)\b|Slice 20' app/src` -> exit 0; only the 2 target declarations in `strings.xml` matched; 0 matches for `Slice 20`.
  - `rg -n 'getIdentifier' app/src` -> exit 1 (0 matches).
- Post-edit compilation:
  - `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q` -> **PASS** (exit 0).
- Post-edit unit tests:
  - `./gradlew --offline :app:testDebugUnitTest -q` -> **PASS** (exit 0).
