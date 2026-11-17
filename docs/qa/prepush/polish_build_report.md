# Polish batch build report

Branch: `redesign-2026-11-15`

## Verification

- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q`: passed (exit code 0)
- `./gradlew --offline :app:testDebugUnitTest -q`: passed (exit code 0)

## Files changed

1. `app/src/main/res/values/strings.xml`
2. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt`
3. `app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt`
4. `app/src/main/java/com/sherifradwan/ancnative/ui/AddAttachmentDialog.kt`
5. `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt`
6. `app/src/main/java/com/sherifradwan/ancnative/ui/report/ReportSections.kt`
7. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt`
8. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt`

Notes:
- `ui/AncApp.kt` was checked and required no changes.
- Forbidden paths (`state/`, `storage/`, `model/`, `security/`, `schemas/`, `ui/DatingFieldVisibility.kt`, `PatientPrintHtmlGenerator.kt`, `PatientPrintManager.kt`, `PatientShareText.kt`) were not modified.

## Strings changed (old to new)

In `app/src/main/res/values/strings.xml`:

| Resource | Old | New |
|---|---|---|
| `visit_date` (line 25) | `Visit date (YYYY-MM-DD)` | `Visit date` |
| `scan_date` (line 168) | `Scan date (YYYY-MM-DD)` | `Scan date` |
| `delivery_outcome_date_label` (line 552) | `Delivery date (YYYY-MM-DD)` | `Delivery date` |
| `active_prescriptions_title` (`one`, line 448) | `Active Prescription (%1$d)` | `Active prescription (%1$d)` |
| `active_prescriptions_title` (`other`, line 449) | `Active Prescriptions (%1$d)` | `Active prescriptions (%1$d)` |
| `add_other_choice` (line 470) | `+ Add other` | `Add other` |
| `lab_batch_add_other` (line 519) | `+ Add other` | `Add other` |

Preserved unchanged:
- Validation strings with format guidance: `invalid_date` (`Enter a real calendar date as YYYY-MM-DD.`), `delivery_error_date_invalid` (`Enter the date as YYYY-MM-DD, for example 2026-11-05.`).
- `security_settings_dialog_title` (`Clinic & System Settings`).

## Call sites changed

### Task 1: Date labels
- Consumers of `R.string.visit_date` (`VisitEditor.kt`, `VisitEditorSteps.kt`), `R.string.scan_date` (`UltrasoundEditor.kt`), and `R.string.delivery_outcome_date_label` (`DeliveryPlanSheet.kt`) inherit the updated labels from `strings.xml`.

### Task 2: Arabic right alignment (`PatientNameText`)
Switched raw `Text(...)` rendering of patient names to `PatientNameText(...)` (`ui/components/PatientNameText.kt`, keeping existing `style`, `color`, and `testTag` values):
- `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt` line 255: expanded hero header (`hubPatientName`).
- `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt` line 429: collapsed sticky top bar (`maxLines = 1`, `Modifier.weight(1f)`).
- `app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt` line 479: `EditorPageHeader` subtitle (`Modifier.fillMaxWidth()` on header column), covering `PatientStatusEditor`, `DatingEditor`, `OtherHistoriesEditor`, `PreviousPregnancyEditor`, `UltrasoundEditor`, and `PatientEditor`.
- `app/src/main/java/com/sherifradwan/ancnative/ui/AddAttachmentDialog.kt` line 142: patient name subtitle under dialog title.
- `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt` line 111: patient name in viewer header.

Existing `PatientNameText` call sites verified unchanged:
- `PatientDirectoryCard.kt` (line 83), `PatientActionsModalSheet.kt` (line 70), `VisitEditorSteps.kt` (line 114), `LabEditorComponents.kt` (line 73), `PatientTwoPageReportDialog.kt` (line 195), `ReportSections.kt` (lines 414, 1087, 1093).

### Task 3: Native report empty GA and EFW (`ReportSections.kt`)
- `ReportVisitsCard` line 732: visit GA `BandChip` uses `visitGa?.let(::ltrIsolate) ?: stringResource(R.string.not_recorded)` instead of `"-"`.
- `ReportScansCard` lines 828-834: scan GA `BandChip` uses `stringResource(R.string.not_recorded)` when `scanGestationalAge(scan) == "-"`, otherwise `ltrIsolate(scanGa)`.
- `ReportScansCard` lines 872-878: EFW `PlainField` passes `value = if (efw == "-") null else ltrIsolate(efw)` and `missingText = stringResource(R.string.not_recorded)`.
- `scanGestationalAge` and `scanWeight` functions stay untouched so `PatientPrintHtmlGenerator.kt`, `PatientShareText.kt`, and `ReportSectionsTest.kt` are unaffected.

### Task 4: Meds tab sentence case and no decorative plus
- `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt` lines 211-220 (`addCustomMedicationBtn`): removed decorative `Icon(Icons.Default.Add, ...)` and `Spacer`.
- `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt` lines 339-347 (`medEmptyPrescribe`): removed decorative `Icon(Icons.Default.Add, ...)` and `Spacer`.
- `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt` line 191 (`PickerToggle` / `medPickerToggle`): removed decorative `Icon(Icons.Default.Add, ...)`, keeping the expand/collapse chevron.

## Existing tests that assert changed texts or positions

Grep of `app/src/androidTest` and `app/src/test` (test files were not modified):

### Hardcoded assertion broken by Task 3
- `app/src/androidTest/java/com/sherifradwan/ancnative/ReportDialogRedesignTest.kt` (`visitsCard_visitWithNoComputableGa_showsDashGaChip`, lines 1014-1041): asserts `compose.onNodeWithText("-", useUnmergedTree = true)` at line 1039 for a visit without computable GA; the UI now shows `"Not recorded"` (`R.string.not_recorded`).

### Resource-backed assertions (adapt via `strings.xml`)
- `app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt`:
  - Lines 989, 1056: `node("medsActiveTitle").assertTextEquals(plural(R.plurals.active_prescriptions_title, ...))`
  - Lines 824, 990, 1114, 1269: `R.string.prescribe_medication`
  - Line 1054: `R.string.medications_empty_action`
  - Lines 783, 998, 1125: `R.string.add_medication`
- `app/src/androidTest/java/com/sherifradwan/ancnative/MedsTuneTest.kt`:
  - Line 176: `R.string.add_medication` in `medPickerToggle`
  - Lines 201-206: `medEmptyPrescribe` vertical position inside `medsEmptyState`
  - Lines 228-238: `addCustomMedicationBtn` bounds relative to `medsActiveTitle`
- `app/src/androidTest/java/com/sherifradwan/ancnative/CatalogSettingsRedesignTest.kt`:
  - Lines 350, 412: `R.string.add_medication`

### Position and display assertions on updated patient name sites
- `app/src/androidTest/java/com/sherifradwan/ancnative/PatientHubDesignTest.kt`:
  - Line 66: `compose.onNodeWithText(patient.displayName).assertIsDisplayed()`
  - Lines 118-120: `layout("hubPatientName")` checks `lineCount <= 4` and no ellipsized lines at `fontScale = 2f` (360dp).
- `app/src/androidTest/java/com/sherifradwan/ancnative/AttachmentsRedesignTest.kt`:
  - Lines 558, 752, 882: assert `base.displayName` displayed in `addAttachmentDialog` and `clinicalAttachmentViewerDialog`.
  - Lines 892-900 (`viewer_rtl360dp_buttonsOnTheLeft_andNothingClipped`): asserts `close.right <= name.left + 0.5.dp && delete.right <= name.left + 0.5.dp` in RTL with English `base.displayName`.
- `app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt` (lines 291, 450, 667) and `app/src/androidTest/java/com/sherifradwan/ancnative/UltrasoundEditorRedesignTest.kt` (lines 380, 945): assert `patient.displayName` displayed inside `EditorPage`.

### Unaffected tests referencing `"-"`, `"+ Add other"`, or date labels
- `app/src/test/java/com/sherifradwan/ancnative/ui/report/ReportSectionsTest.kt` (lines 268, 313-314): asserts `scanGestationalAge(...) == "-"` and `scanWeight(...) == "-"` (unchanged helpers; passes).
- `app/src/androidTest/java/com/sherifradwan/ancnative/SharedDialogsRedesignTest.kt` (lines 991, 995, 1000, 1011): passes literal `"Delivery date"` and `"Enter the date as YYYY-MM-DD."` in isolated test composables.
- `app/src/androidTest/java/com/sherifradwan/ancnative/VisitEditorRedesignTest.kt` (lines 521, 533, 566, 571, 577, 604) and `LabEditorRedesignTest.kt` (lines 628, 1190): target `"addOther"` chips by `testTag`.
- `app/src/test/java/com/sherifradwan/ancnative/model/catalog/CustomChoicesLogicTest.kt` (line 131): comment reference to `"+ Add other"` only.
