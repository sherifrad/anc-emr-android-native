### READ-ONLY REUSE AUDITOR REPORT (MACA a10)

**R-1** | `HIGH` | GA & EDD Countdown / Term Arithmetic outside Engine
- Copy 1: [GestationalBanner.kt:97-98](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/GestationalBanner.kt#L97-L98) `val isPostTerm = gaResult.isPostTerm || gaResult.weeks >= 42; val isLateTerm = !isPostTerm && gaResult.weeks == 41`
- Copy 2: [GestationalBanner.kt:146](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/GestationalBanner.kt#L146) `val daysDiff = ChronoUnit.DAYS.between(asOfDate, edd)`
- Engine: [GestationalAgeEngine.kt:90, 134](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/GestationalAgeEngine.kt#L90) `val daysToEdd = ChronoUnit.DAYS.between(asOfDate, edd)`; `val isPostTerm = weeks >= 42`
- Single function: `GestationalAgeResult.daysToEdd(asOf: LocalDate)` & `isLateTerm` property in [`model/GestationalAgeEngine.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/GestationalAgeEngine.kt).
- Risk: Countdown logic and late/post-term classifications diverge between banner UI and core clinical calculations.

**R-2** | `LOW` | Date Formatting outside IsoDates.kt
- Copy 1: [PatientPrintHtmlGenerator.kt:41](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt#L41) `val printDateStr = asOfDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))`
- Canonical: [IsoDates.kt:1-30](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/IsoDates.kt) (standard ISO parser & formatting module)
- Single function: `formatIsoDate(date: LocalDate): String = date.toString()` in [`model/IsoDates.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/IsoDates.kt).
- Risk: Pattern re-allocation on print and bypass of centralized ISO-8601 string conversions.

**R-3** | `HIGH` | Required ISO Clinical Date Validation Rule Duplicated
- Copy 1: [ClinicalCommands.kt:203-207](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalCommands.kt#L203-L207) `if (date.isEmpty()) errors.add(ClinicalValidationError.REQUIRED_DATE) else if (!ClinicalVocabulary.isValidIsoDate(date)) errors.add(ClinicalValidationError.INVALID_DATE)`
- Copy 2: [ClinicalCommands.kt:319-323](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalCommands.kt#L319-L323) identical date check for Labs
- Copy 3: [ClinicalCommands.kt:406-410](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalCommands.kt#L406-L410) identical date check for Medications
- Divergence: [SaveVisitCommand.kt:43-47](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/SaveVisitCommand.kt#L43-L47) also rejects future dates (`parsedDate.isAfter(today)`), unlike ClinicalCommands.
- Single function: `validateClinicalDate(date: String): Set<ClinicalValidationError>` in [`storage/ClinicalCommands.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalCommands.kt).
- Risk: Disparate date validation rules across clinical entities (visits reject future dates; scans/labs allow them).

**R-4** | `HIGH` | Obstetric History / Delivery Counting & Summary Duplicated
- Copy 1: [PatientListScreen.kt:99-116](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt#L99-L116) `it.birthMethod.equals("Cesarean", ignoreCase = true) || it.deliveryBy.equals("CS", ignoreCase = true)` -> `"Prev $prevCS CS, $prevVaginal SVD"`
- Copy 2: [ProfileScreen.kt:377-408](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt#L377-L408) counts CS/SVD/Stillbirth/Miscarriage -> `"Prev $prevCS CS, $prevVaginal SVD · 1 IUFD"`
- Copy 3: [PatientPrintHtmlGenerator.kt:49-63](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt#L49-L63) `it.birthMethod?.contains("Cesarean", ignoreCase = true) == true` -> `"Prev $prevCsCount CS + $prevSvdCount SVD"`
- Single function: `formatObstetricHistorySummary(patient: DemoPatient): String` in [`model/ClinicalSummaryGenerator.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/ClinicalSummaryGenerator.kt).
- Risk: Conflicting obstetric history and parity summaries displayed between patient list, profile, and print report.

**R-5** | `MED` | Risk Badge Styling & Theme Decision Logic Split
- Copy 1: [PatientListScreen.kt:831-860](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt#L831-L860) `ClinicalRiskBadge` using hardcoded `TagHighRiskDarkBg / TagHighRiskBg` tokens
- Copy 2: [ProfileScreen.kt:338-360](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt#L338-L360) inline Surface using `MaterialTheme.colorScheme.errorContainer` vs `tertiaryContainer`
- Single function: Unified `ClinicalRiskBadge(isHighRisk: Boolean, modifier: Modifier)` in [`ui/ClinicalBadges.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalBadges.kt).
- Risk: Inconsistent color coding and visual hierarchy for high-risk maternal alerts across app surfaces.

**R-6** | `MED` | SaveUiError to String Resource Mapping Duplicated
- Copy 1: [PatientEditor.kt:677-686](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt#L677-L686) `AncState.patientEditorErrorResource(): Int = when (saveError) { ... }`
- Copy 2: [VisitEditor.kt:481-490](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt#L481-L490) `SavePhase.ERROR -> when (saveError) { ... }`
- Single function: `SaveUiError.toStringRes(): Int` in [`state/AncState.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt).
- Risk: Adding new save error states leaves one editor silently falling back to a generic storage error.

**R-7** | `MED` | Gestational Age Display Formatter Duplicated
- Copy 1: [PatientListScreen.kt:121-125](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt#L121-L125) `"${gaResult.weeks}+${gaResult.days} wks"`
- Copy 2: [ProfileScreen.kt:375](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt#L375) `"${gaResult.weeks}+${gaResult.days} wks"`
- Engine standard: [GestationalAgeEngine.kt:10](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/GestationalAgeEngine.kt#L10) `"${weeks}w ${days}d"`
- Single function: `val shortWeeksFormat: String get() = "${weeks}+${days} wks"` on `GestationalAgeResult` in [`model/GestationalAgeEngine.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/GestationalAgeEngine.kt).
- Risk: Ad-hoc GA notation string interpolations proliferate and contradict canonical engine formatting.

**R-8** | `LOW` | Linked Ultrasound Metrics Display Line Duplicated
- Copy 1: [ProfileScreen.kt:1133](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt#L1133) `"FHR: ${linkedScan.fetalHeartRate ?: "-"} bpm · EFW: ${linkedScan.estimatedFetalWeight ?: "-"} g"`
- Copy 2: [WorkspaceScreen.kt:916](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt#L916) `"FHR: ${linkedScan.fetalHeartRate ?: "-"} bpm · EFW: ${linkedScan.estimatedFetalWeight ?: "-"} g"`
- Single function: `formatFetalMetrics(fhr: String?, efw: String?): String` in [`ui/ClinicalFormat.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalFormat.kt).
- Risk: Inconsistent placeholder / unit formatting across clinical overview cards.

**R-9** | `MED` | Editor Field OutlinedTextField Wrapper Cloned
- Copy 1: [VisitEditor.kt:449-474](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt#L449-L474) `private fun VisitField(...)`
- Copy 2: [PatientEditor.kt:500-526](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt#L500-L526) `internal fun EditorField(...)`
- Single function: Promote `EditorField` to public composable in [`ui/Components.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt).
- Risk: Touch-target sizing, accessibility semantics, and error text styling drift between editors.

**R-10** | `LOW` | Editor Error Cards Cloned with Cross-Domain Leak
- Copy 1: [PatientEditor.kt:634-653](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt#L634-L653) `EditorErrorCard(state, validationErrors: Set<PatientValidationError>)`
- Copy 2: [PatientEditor.kt:656-675](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt#L656-L675) `ClinicalEditorErrorCard(state, validationErrors: Set<ClinicalValidationError>)` (accidentally calls `patientEditorErrorResource()`)
- Single function: `GenericEditorErrorCard(hasErrors: Boolean, state: AncState)` in [`ui/Components.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt).
- Risk: Clinical editor displays patient-domain error text due to copy-paste bug.

**R-11** | `MED` | Vocabulary Lists & Medication Frequencies Redefined
- Copy 1: [MedicationEditor.kt:273](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt#L273) `items = listOf("Once daily", "Twice daily", "Three times daily", "As needed (PRN)")`
- Copy 2: [CodexSettingsSections.kt:968](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/CodexSettingsSections.kt#L968) `val commonFrequencies = listOf("Once daily", "Twice daily", "Three times daily", "At bedtime", "As needed (PRN)")`
- Single constant: `ClinicalPresets.commonMedicationFrequencies` in [`ui/ClinicalPresets.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalPresets.kt).
- Risk: Doctors see different frequency autocomplete presets in settings vs clinical entry.

**R-12** | `HIGH` | Test Suite Copies Production Progress & Countdown Formulas
- Copy 1: [GestationalBannerLogicTest.kt:38, 68, 96, 171](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt#L38) `val progress = (result.totalDays / 280f).coerceIn(0f, 1f)`
- Copy 2: [GestationalBannerLogicTest.kt:42, 72, 101, 133](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt#L42) `val daysDiff = ChronoUnit.DAYS.between(asOf, edd)`
- Production: [GestationalBanner.kt:129, 146](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/GestationalBanner.kt#L129)
- Single function: Production should expose `progressFraction` and `daysToEdd` on `GestationalAgeResult`; tests must assert against production calls.
- Risk: Tests test their own local copies of math rather than production code, staying green even if production calculation breaks.

---

### Audit Findings Summary

| ID | Severity | Category | Target Location |
|:---|:---|:---|:---|
| **R-1** | HIGH | (1) GA / EDD arithmetic | `model/GestationalAgeEngine.kt` |
| **R-3** | HIGH | (3) Validation rules | `storage/ClinicalCommands.kt` |
| **R-4** | HIGH | (4) Obstetric history summary | `model/ClinicalSummaryGenerator.kt` |
| **R-12** | HIGH | (8) Production math in tests | `model/GestationalAgeEngine.kt` |
| **R-5** | MED | (4) Risk badge styling | `ui/ClinicalBadges.kt` |
| **R-6** | MED | (4) SaveUiError mapping | `state/AncState.kt` |
| **R-7** | MED | (5) Gestational age display text | `model/GestationalAgeEngine.kt` |
| **R-9** | MED | (6) Cloned editor text fields | `ui/Components.kt` |
| **R-11** | MED | (7) Redefined vocabulary lists | `ui/ClinicalPresets.kt` |
| **R-2** | LOW | (2) Date formatting | `model/IsoDates.kt` |
| **R-8** | LOW | (5) Fetal metrics formatting | `ui/ClinicalFormat.kt` |
| **R-10** | LOW | (6) Error card composable | `ui/Components.kt` |

### Top 3 to Fix First
1. **R-4 (Obstetric History & Delivery Summary)**: Prev CS/SVD parsing directly impacts clinical labor management decisions (trial of labor after cesarean vs repeat elective CS); having three divergent text formats across list, profile, and print is a high clinical safety risk.
2. **R-1 & R-12 (Gestational Age / EDD Countdown & Banner Math)**: Extract progress fraction and countdown days into `GestationalAgeEngine.kt` so UI components and unit tests invoke the single canonical engine calculation instead of mirroring ad-hoc math.
3. **R-3 (Clinical Date Validation)**: Unify date checks across `ClinicalCommands` and align policy with `SaveVisitCommand` to eliminate inconsistencies around empty, invalid, or future dates.
