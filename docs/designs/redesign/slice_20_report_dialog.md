# Slice 20: Report dialog (Level 3)

Status: Ready for owner decisions -> builder brief.  
Level: **Level 3** (`clinical_report_content`: frontier-tier review + independent test review + `git diff -w` invariant verification required per `PLAN.md` Section 0.13).  
Branch: `redesign-2026-11-15`  
Mock: [mock_s20_report_dialog.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/mocks/mock_s20_report_dialog.html)  
Shared components reused (no edits): `RecordSectionCard`, `LabeledValue`, `LabeledValueRow`, `PlainField`, `StatusTag`, `StatusWord`, `BandChip`, `PatientNameText`, `RecordFilterChip`, `FieldFamilies`, `RecordType`, `RecordShapes`, `RecordSpacing`, `ltrIsolate`, `Modifier.minimumTouchTarget()`.

> **Owner direction & design dials:** Presentation-only pass on the in-app two-page clinical report dialog (`antislop: DURING`, `ENERGY 1 / RHYTHM 1 / MOTION 1`, no new animation beyond standard M3 press/focus feedback, no em dashes anywhere, sentence case for new chrome, `10.dp` / `16.dp` radii, `0.dp` elevation, no colored left stripes, no template cards).
>
> **Protected / Never touch:**
> - `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt` (protected `*Generator*`; A4 HTML, CSS, table layout, dashed `Stamp` box, and print HTML titles remain untouched).
> - `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintManager.kt` (out of scope; known `PrintManager` `Activity` context crash is tracked in its own separate task and must not be redesigned around here).
> - `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientShareText.kt` (out of scope unless an owner decision explicitly authorizes a copy edit).
> - `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubScreen.kt` (sole caller at lines 349-355; keep `PatientTwoPageReportDialog` signature identical so the caller needs zero changes).
> - `model/**`, `storage/**`, `state/**`, Room schema / migrations, `ui/Theme.kt`, `ui/Components.kt`, `ui/components/**`, `ui/ClinicalFormat.kt`.

---

## LEVEL 3: What must NOT change (`git diff -w` invariants)

A clinical and code reviewer verifies Slice 20 against clean `redesign-2026-11-15` using `git diff -w`. Every rule below is mandatory:

### 1. Entry point, callbacks, and arguments
- **Dialog composable signature (`ui/report/PatientTwoPageReportDialog.kt`):**
  ```kotlin
  @Composable
  fun PatientTwoPageReportDialog(
      patient: DemoPatient,
      securitySettings: SecuritySettingsState? = null,
      onDismiss: () -> Unit,
  )
  ```
- **Dialog properties:** `Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true))`.
- **Share action (`shareReportButton`):**
  1. Calls `val summary = patientShareText(patient)` (`ui/report/PatientShareText.kt`).
  2. Builds `Intent(Intent.ACTION_SEND)` with `type = "text/plain"`, `putExtra(Intent.EXTRA_SUBJECT, "Clinical Record - ${patient.displayName}")`, `putExtra(Intent.EXTRA_TEXT, summary)`.
  3. Launches `context.startActivity(Intent.createChooser(shareIntent, "Share Clinical Record"))`.
  (If chooser/subject strings move to `strings.xml`, they must resolve to the exact identical strings `"Clinical Record - %1$s"` and `"Share Clinical Record"`.)
- **Print action (`printReportButton`):**
  Calls `PatientPrintManager.printPatientReport(context = context, patient = patient, settings = securitySettings)` verbatim. Keep existing behavior regardless of whether `context` is an `Activity` or wrapped context; do not modify `PatientPrintManager.kt` or add an `Activity` unwrapper inside Slice 20.

### 2. Pure functions & types in `ui/report/ReportSections.kt` (immutable behavior)
`ReportSectionsTest.kt`, `ExportFixRoundTwoTest.kt`, `PatientPrintHtmlGenerator.kt`, and `PatientShareText.kt` directly call the following existing symbols. Keep every name, `internal` visibility, signature, sorting order, cap math, and returned clinical string untouched:
- `NOT_RECORDED = "Not recorded"`
- `Limited<T>(val rows: List<T>, val hidden: Int)`
- `ReportMedications(val current: List<DemoMedicationRecord>, val stopped: List<DemoMedicationRecord>)`
- `ReportRisk` (`HIGH("High risk")`, `MEDIUM("Medium risk")`, `LOW("Low risk")`, `UNRECORDED("Risk not recorded")`)
- `ReportRow(val label: String, val value: String)`
- `reportSortKey(date: String?): LocalDate`
- `reportVisits(patient: DemoPatient, limit: Int): Limited<DemoVisit>`
- `reportScans(patient: DemoPatient, limit: Int): Limited<DemoUltrasoundScan>`
- `reportLabs(patient: DemoPatient, limit: Int): Limited<DemoLabInvestigation>`
- `reportMedications(patient: DemoPatient): ReportMedications`
- `reportRisk(riskLevel: String?): ReportRisk`
- `reportRiskLabel(riskLevel: String?): String`
- `reportStatusLine(patient: DemoPatient): String?`
- `reportGestationalAge(patient: DemoPatient, asOf: LocalDate): String?`
- `reportDate(iso: String?): String`
- `visitText(visit: DemoVisit): String`
- `scanGestationalAge(scan: DemoUltrasoundScan): String`
- `scanNotes(scan: DemoUltrasoundScan): String`
- `scanWeight(scan: DemoUltrasoundScan): String`
- `medicationDetail(m: DemoMedicationRecord): String`
- `medicationDates(m: DemoMedicationRecord): String`
- `labResult(lab: DemoLabInvestigation): String`
- `labExtra(lab: DemoLabInvestigation): String`
- `deliveryPlanRows(patient: DemoPatient): List<ReportRow>`
- `reportDeliveryRows(patient: DemoPatient, eddText: String?): List<ReportRow>`

### 3. Section ordering, displayed values, sources, limits, and clinical wording
Every on-screen report block in Page 1 and Page 2 must preserve the exact section order, row ordering, record caps, fallback strings, and field sources used by `ReportSections.kt` and `PatientPrintHtmlGenerator.kt`:

| Page | Order | Block / Field | Exact Source / Rule | Clinical Wording & Cap Invariants |
|---|---|---|---|---|
| Page 1 | 1 | Clinic meta & status | Clinic title (`app_name` / `securitySettings`), subtitle (`clinic_default_subtitle` / `PatientPrintHtmlGenerator` default `"Maternal-Fetal Medicine & Advanced Obstetrics"`), `patient.id`, print date `formatDmy(asOf)`, `reportStatusLine(patient)` | Print date always via `formatDmy`; `reportStatusLine` banner shown only when non-null (archived/delivered outcome). |
| Page 1 | 2 | Patient identity | `patient.displayName`, `reportRisk(patient.riskLevel)`, `isRhNegative(patient.bloodGroup)`, `patient.age`, `patient.bloodGroup`, `patient.phone`, `patient.address`, `patient.clinicalAttentionNote` | Risk labels verbatim (`High risk`, `Medium risk`, `Low risk`, `Risk not recorded`). Conditional `Rh negative` tag. Blank phone displays `"Unspecified"`. Conditional `Clinical attention` callout shown only when non-blank. |
| Page 1 | 3 | Pregnancy dating | `datingSummary = datingSummaryOf(patient)`, GA (`reportGestationalAge` / Decision 2 `shortWeeksText`), `datingTextRows(datingSummary)`, `datingDiscordanceLine(patient.dating, datingSummary)` | When absent / not active pregnancy per report logic, displays `"Gestational age: not recorded"`. All dates via `formatDmy` (Owner Date Rule: no typed dates). |
| Page 1 | 4 | Obstetric and medical history | Obstetric summary (`obstetricSummary` / GPA formula & `pastDeliverySummary`), `patient.allergyHistory` (`classifyAllergy`), `patient.medicalHistory`, `patient.surgicalHistory`, `patient.familyHistory` | Blank histories MUST explicitly show `"Not recorded"` (`NOT_RECORDED`), unlike editable Overview cards. `AllergyState.Recorded` renders in `errorContainer` alert box; never omit `"Not recorded"` for blank histories. |
| Page 1 | 5 | Previous pregnancies & obstetric outcomes | `patient.pastPregnancies` formatted with `formatDmy(pp.dateText) ?: "-"`, outcome, plurality, mode, hospital, notes | When empty: exact string `"No previous pregnancies or deliveries recorded ($pastDeliverySummary)."` |
| Page 1 | 6 | Antenatal care visits log | `reportVisits(patient, MAX_REPORT_VISITS)` (`limit = 12`), row index `idx + 1 + visits.hidden`, `reportDate(visit.date)`, GA, `visit.bloodPressure`, `formatWeightKg(visit.weightKg) ?: "-"`, `visitText(visit)` | Limit `12` (oldest-first of most recent 12). When `visits.hidden > 0`: `"${visits.hidden} earlier visits not shown"`. BP alerts verbatim (`"Raised BP - recheck"` / `"Severe-range BP - urgent assessment"`). Empty: `"No clinical visits recorded yet"`. |
| Page 2 | 1 | Continued strip | `"Scans, laboratory results, medications and delivery plan"`, `patient.id`, `formatDmy(asOf)`, `"Continued"` chip | Matches print Page 2 continuation header without repeating Page 1 sections. |
| Page 2 | 2 | Ultrasound biometry & fetal surveillance | `reportScans(patient, MAX_REPORT_SCANS)` (`limit = 7`), `reportDate(scan.date)`, `scanGestationalAge(scan)`, `scan.imagePaths.size`, presentation, placenta, amniotic fluid, `scanWeight(scan)`, `scanNotes(scan)` | Limit `7` (oldest-first of most recent 7). When `scans.hidden > 0`: `"${scans.hidden} earlier scans not shown"`. Photos badge `"${count} photos in the clinic record"` when non-empty. Empty: `"No ultrasound scans recorded yet"`. |
| Page 2 | 3 | Laboratory investigations | `reportLabs(patient, MAX_REPORT_LABS)` (`limit = 10`), `lab.testName`, `reportDate(lab.date)`, `labResult(lab)`, `lab.referenceRange`, `labExtra(lab)` | Limit `10` (newest-first). When `labs.hidden > 0`: `"${labs.hidden} older results not shown"`. Abnormal labs show `error` ink paired with the word `"Abnormal"` (`lab_abnormal`). Empty: `"No laboratory test results recorded yet"`. |
| Page 2 | 4 | Current medications | `reportMedications(patient).current`, `m.name`, `medicationDetail(m)`, `medicationDates(m)` | Uses `medicationInstructions(m.dosage, m.route, m.notes)` + `"Prophylaxis"` + `"Adverse: ..."`. **Stored `frequency` is NEVER rendered.** Dates use `reportDate` / `formatDmy`. Empty: `"No current medications recorded"`. |
| Page 2 | 5 | Stopped medications (conditional) | `reportMedications(patient).stopped`, `m.name`, `medicationDetail(m)`, `medicationDates(m)` | **Section is ONLY rendered when `stopped.isNotEmpty()`.** Hidden completely when empty. |
| Page 2 | 6 | Delivery plan | `reportDeliveryRows(patient, datingSummary.edd?.let(::formatDmy))` | Exact `ReportRow.label` / `ReportRow.value` pairs (`Delivery Outcome`, `Planned delivery`, `Hospital`, `Postpartum`, `Blood bank precautions` = `"Anti-D Prophylaxis + Matched Packed RBCs Cross-match"` if `isRhNegative`, `Estimated due date`, `Plan` = `"Not recorded"`). **Delivery fees NEVER rendered.** |
| Page 2 | 7 | Clinician attestation | `Dr. Sherif Radwan`, `Specialist of Obstetrics, Gynecology & Laparoscopy`, `شريف جمال رضوان`, `أخصائي التوليد وأمراض النساء وتأخر الحمل`, `Dr. Sherif Radwan Clinic`, `Tel 01201720808` | Exact clinician credentials and bilingual attribution. |

---

## Decisions for the owner (max 5, match the mock)

1. **Native Compose Page 1 & Page 2 body vs keeping `AndroidView(WebView)` (`reportWebView`):**
   Today `PatientTwoPageReportDialog` renders only a light-only `WebView` with `min-width: 600px` HTML tables inside the dialog. Grep confirms zero tests in `app/src/test` or `app/src/androidTest` assert `reportWebView`.  
   - **Recommend:** Render Page 1 and Page 2 natively in Compose (switched by `Page 1` / `Page 2` `RecordFilterChip`s) and retire `reportWebView` from the dialog, keeping `PatientPrintHtmlGenerator` untouched for `Print PDF`.  
   - **Alternative:** Keep `reportWebView` mounted behind a 3rd `"Print sheet"` chip in the switcher if on-screen A4 layout inspection is needed before printing.

2. **Native dialog gestational age format (`27+2 wks` vs `27w 2d`):**
   `PLAN.md` Decision 9 standardizes on `27+2 wks` (`shortWeeksText`), whereas `ReportSections.kt`'s `reportGestationalAge()` returns `"27w 2d"` (asserted in `ReportSectionsTest.kt`) and `scanGestationalAge()` returns `"12w + 3d"`.  
   - **Recommend:** Keep `reportGestationalAge()` and `scanGestationalAge()` byte-for-byte untouched for `ReportSectionsTest`, `PatientPrintHtmlGenerator`, and `PatientShareText`, and display `shortWeeksText` (`27+2 wks`) for current pregnancy GA and visit chips in the native Page 1 UI cards (while keeping `scanGestationalAge(scan)` in scans so ultrasound rows stay aligned with print).

3. **Sentence-case native UI headings vs print HTML Title Case:**
   `PatientPrintHtmlGenerator.kt` uses Title Case headings (`Ultrasound Biometry & Fetal Surveillance`, `Patient Identity`) and ALL-CAPS badges (`CLINICAL RECORD`, `CONTINUED`).  
   - **Recommend:** Use sentence case for native section headings and `"Clinical record"` / `"Continued"` chips while keeping every clinical value, `ReportRow.label` (`Delivery Outcome`, `Planned delivery`, `Hospital`, `Postpartum`, `Blood bank precautions`, `Estimated due date`, `Plan`), and `ReportRisk` label (`High risk`, `Medium risk`, `Low risk`, `Risk not recorded`) verbatim.

4. **Clinician attestation block on Page 2 without physical dashed `Stamp` box:**
   A4 print HTML renders an empty dashed rectangle labeled `Stamp` for ink stamping on paper.  
   - **Recommend:** Render the clinician attestation card (`Dr. Sherif Radwan` / Arabic signature / clinic & `Tel 01201720808`) on Page 2 without the empty physical `Stamp` box (leaving `Stamp` in `PatientPrintHtmlGenerator.kt` only).

5. **Known `PrintManager` `Activity` context crash:**
   - **Recommend:** Keep `PatientPrintManager.printPatientReport(context = context, patient = patient, settings = securitySettings)` verbatim inside `printReportButton`; fix the `Activity` context requirement in its dedicated separate task.

---

## Lessons & Design Invariants

- **Minimum type scale:** Never use `labelSmall` (`11.sp`) in this dialog. Smallest allowed label/tag/caption is `RecordType.FieldLabel` / `RecordType.Tag` / `RecordType.MetricLabel` (`12.sp`).
- **Minimum touch target:** All interactive controls (`closeReportDialog`, `shareReportButton`, `printReportButton`, `Page 1` chip, `Page 2` chip) MUST meet `48.dp x 48.dp` via `.minimumTouchTarget()` or `minTouchTarget = true` on `RecordFilterChip`.
- **Contrast:** Replace BEFORE's `secondary` (`#934F5B`) on `ClinicalNavy` (`#0D1B3D`) (`2.83:1` fail) with `MaterialTheme.colorScheme.surface` header (`0.dp` shadow) + `MaterialTheme.colorScheme.primary` / `onPrimary` `Print PDF` button (`16.92:1` light, `7.72:1` dark).
- **Dark theme parity:** All dialog surfaces and cards read semantic `MaterialTheme.colorScheme.*` and `FieldFamilies.*` tokens (`AncTheme(darkTheme = ...)`), eliminating light-only HTML glare in Dark Theme.
- **360dp narrow viewport + `fontScale >= 1.3f`:** No multi-column scrollable HTML tables; use `LabeledValueRow` (automatically stacks 2-up pairs vertically above `fontScale > 1.3f`) and `PlainField` 2-column / 1-column adaptive blocks (`wrapContentHeight`). Never force `maxLines = 1` on `patient.displayName`, card titles, or clinical values.
- **RTL & Bidirectionality:** Always render `patient.displayName` with `PatientNameText(..., style = RecordType.HeaderName)` so Arabic names like `منى رجب` align end/right cleanly without disrupting LTR clinical UI, and wrap numeric/date/GA/BP/GPA/phone/ID strings (`27+2 wks`, `11/02/2027`, `110/70`, `G4 P1 + A2`, `A+`, `01201720808`) with `ltrIsolate(...)`.
- **Owner Date Rule:** Every date shown (`asOf` print date, LMP/US dates, delivery date, visit date, scan date, lab date, medication start/end dates, past pregnancy dates) uses the app's existing date formatters (`formatDmy` / `reportDate` / `formatDeliveryDate`). Zero typed date inputs.
- **No color alone:** Abnormal lab results pair `error` ink with the text word `"Abnormal"`; elevated BP values pair severity with `"Raised BP - recheck"` / `"Severe-range BP - urgent assessment"`; allergies/Rh precautions use structured `errorContainer` callout blocks with clear labels.
- **File size ceiling:** Keep `PatientTwoPageReportDialog.kt` around `200-320` lines and `ReportSections.kt` around `550-750` lines (well below the `1,500`-line limit).
- **Zero hardcoded user-facing strings** in new Compose code; use `stringResource` / `pluralStringResource`.

---

## Files

| File | Action | Notes |
|---|---|---|
| `app/src/main/res/values/strings.xml` | Modify (additions only) | Add `report_*` strings below; reuse existing `app_name`, `close`, `print_pdf`, etc. |
| `app/src/main/java/com/sherifradwan/ancnative/ui/report/ReportSections.kt` | Modify (additive UI sections only) | Keep ALL existing pure helpers byte-for-byte intact; add Page 1 & Page 2 Record-system composables. |
| `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientTwoPageReportDialog.kt` | Modify | Replace navy WebView container with `surface` header, full-width `PatientNameText`, `RecordFilterChip` Page 1 / Page 2 switcher, native scroll body dispatching `ReportSections`. |
| `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt` | **DO NOT TOUCH** | Protected `*Generator*`. |
| `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintManager.kt` | **DO NOT TOUCH** | Separate PrintManager Activity task. |
| `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientShareText.kt` | **DO NOT TOUCH** | Locked by Level-3 rule unless owner authorizes copy edit. |

---

## 1. Strings (`app/src/main/res/values/strings.xml`)

### Existing strings reused verbatim (do NOT duplicate)
- `app_name` (`"Dr. Sherif Radwan Clinic"`)
- `clinic_default_subtitle` (`"Maternal-Fetal Medicine & Obstetrics"`)
- `close` (`"Close"`)
- `print_pdf` (`"Print PDF"`)
- `not_recorded` (`"Not recorded"`)
- `not_specified` (`"Not specified"`)
- `notes` (`"Notes"`)
- `clinical_attention` (`"Clinical attention"`)
- `high_risk` (`"High risk"`)
- `rh_negative_tag` (`"Rh negative"`)
- `status_delivered` (`"Delivered"`)
- `blood_group` (`"Blood group"`)
- `age_label` (`"Age"`)
- `address` (`"Address"`)
- `gestational_age` (`"Gestational age"`)
- `no_dating_recorded` (`"No dating recorded"`)
- `lab_abnormal` (`"Abnormal"`)
- `previous_pregnancies` (`"Previous pregnancies"`)
- `date_not_recorded` (`"Date not recorded"`)
- `medical_history`, `surgical_history`, `family_history`, `allergy_history`

### New strings to add (check `strings.xml` before inserting in case an identical key already exists)

```xml
<!-- Slice 20: Two-page clinical report dialog -->
<string name="report_dialog_kicker">Clinical medical record</string>
<string name="report_page_1">Page 1</string>
<string name="report_page_2">Page 2</string>
<string name="report_page_3_print_sheet">Print sheet</string>
<string name="report_pages_tab_group">Report pages</string>
<string name="report_share_cd">Share clinical record</string>
<string name="report_share_chooser_title">Share Clinical Record</string>
<string name="report_share_subject_format">Clinical Record - %1$s</string>

<string name="report_badge_clinical_record">Clinical record</string>
<string name="report_badge_continued">Continued</string>
<string name="report_page2_subtitle">Scans, laboratory results, medications and delivery plan</string>
<string name="report_clinic_subtitle_full">Maternal-Fetal Medicine &amp; Advanced Obstetrics</string>
<string name="report_patient_id_label">Patient ID</string>
<string name="report_print_date_label">Print date</string>
<string name="report_meta_pair_format">%1$s: %2$s</string>

<string name="report_section_patient_identity">Patient identity</string>
<string name="report_section_pregnancy_dating">Pregnancy dating</string>
<string name="report_section_history">Obstetric and medical history</string>
<string name="report_section_previous_pregnancies">Previous pregnancies and obstetric outcomes</string>
<string name="report_section_visits">Antenatal care visits log</string>
<string name="report_section_scans">Ultrasound biometry and fetal surveillance</string>
<string name="report_section_labs">Laboratory investigations</string>
<string name="report_section_current_meds">Current medications</string>
<string name="report_section_stopped_meds">Stopped medications</string>
<string name="report_section_delivery_plan">Delivery plan</string>
<string name="report_section_attestation">Clinician attestation</string>

<string name="report_phone_label">Phone</string>
<string name="report_phone_unspecified">Unspecified</string>
<string name="report_age_years_format">%1$d yrs</string>
<string name="report_ga_today_label">Gestational age today</string>
<string name="report_ga_not_recorded">Gestational age: not recorded</string>
<string name="report_gpa_label">GPA formula</string>
<string name="report_drug_allergies_label">Drug allergies</string>

<string name="report_bp_label">Blood pressure</string>
<string name="report_weight_label">Weight</string>
<string name="report_findings_label">Clinical findings and observations</string>
<string name="report_visit_number_format">#%1$d</string>
<string name="report_bp_raised">Raised BP - recheck</string>
<string name="report_bp_severe">Severe-range BP - urgent assessment</string>

<string name="report_presentation_label">Presentation</string>
<string name="report_placenta_label">Placenta</string>
<string name="report_amniotic_fluid_label">Amniotic fluid</string>
<string name="report_efw_label">EFW (g)</string>
<string name="report_scan_notes_label">Clinical notes</string>
<string name="report_scan_photos_format">%1$d photos in the clinic record</string>
<string name="report_lab_ref_format">Ref: %1$s</string>

<string name="report_hidden_visits_format">%1$d earlier visits not shown</string>
<string name="report_hidden_scans_format">%1$d earlier scans not shown</string>
<string name="report_hidden_labs_format">%1$d older results not shown</string>

<string name="report_empty_previous_pregnancies_format">No previous pregnancies or deliveries recorded (%1$s).</string>
<string name="report_empty_visits">No clinical visits recorded yet</string>
<string name="report_empty_scans">No ultrasound scans recorded yet</string>
<string name="report_empty_labs">No laboratory test results recorded yet</string>
<string name="report_empty_current_meds">No current medications recorded</string>

<string name="report_doctor_name_en">Dr. Sherif Radwan</string>
<string name="report_doctor_specialty_en">Specialist of Obstetrics, Gynecology &amp; Laparoscopy</string>
<string name="report_doctor_name_ar">شريف جمال رضوان</string>
<string name="report_doctor_specialty_ar">أخصائي التوليد وأمراض النساء وتأخر الحمل</string>
<string name="report_confidential_footer_format">Confidential medical record · %1$s · Tel %2$s</string>
```

---

## 2. Per-element spec: `ui/report/ReportSections.kt`

Keep every existing pure function in `ReportSections.kt` completely intact (above the composables or cleanly separated in the bottom half of the file). Add the following internal composables using the Record system:

| Block | Component & Family | Layout & Typography | Data Binding & Behavior |
|---|---|---|---|
| **Page 1 Card 1: Clinic Meta** | `RecordSectionCard` (`FieldFamilies.patient()`), internal padding `16.dp` | Row with clinic title (`RecordType.CardName`) + subtitle (`RecordType.Secondary`, `onSurfaceVariant`) + `BandChip(text = stringResource(R.string.report_badge_clinical_record))` (`FlowRow` or `Row` with `weight(1f)` so subtitle wraps cleanly on `360dp`); below, `LabeledValueRow` for `Patient ID` and `Print date`; conditional full-width `primaryContainer` status banner (`RoundedCornerShape(10.dp)`, `border = 1.dp primary`, `onPrimaryContainer`) | Clinic title/subtitle resolved identically to existing report header (`securitySettings` overrides if set, otherwise `app_name` / `report_clinic_subtitle_full`). `Patient ID` wrapped with `ltrIsolate(patient.id)`. `Print date` formatted via `formatDmy(asOf)` wrapped with `ltrIsolate`. Status banner shown when `reportStatusLine(patient) != null`. |
| **Page 1 Card 2: Patient Identity** | `RecordSectionCard(title = stringResource(R.string.report_section_patient_identity), family = FieldFamilies.patient())` | Full-width `PatientNameText(patient.displayName, style = RecordType.SectionTitle, color = onSurface)` (wraps multi-line, auto RTL alignment); `FlowRow` of `StatusTag` chips; `LabeledValueRow` (`Age`, `Blood group`); `LabeledValue` (`Phone`, optional `Address`); conditional `Clinical attention` `Surface` (`StatusModerateBg`, `border = 1.dp StatusModerateBorder`, `RecordShapes.Small`) | Risk tag mapped via `reportRisk(patient.riskLevel)`: `HIGH -> StatusKind.HighRisk`, `MEDIUM -> StatusKind.ModerateRisk`, `LOW / UNRECORDED -> StatusKind.Incomplete`, explicit `text = risk.label` so `"Risk not recorded"` and `"Low risk"` keep exact wording. Add `StatusTag(StatusKind.HighRisk, text = stringResource(R.string.rh_negative_tag))` when `isRhNegative(patient.bloodGroup)`. `Age`: if `patient.age > 0`, `report_age_years_format` else `not_recorded`. `Phone`: blank -> `report_phone_unspecified` (`"Unspecified"`). `Clinical attention` callout shown only when `patient.clinicalAttentionNote.isNotBlank()`. |
| **Page 1 Card 3: Pregnancy Dating** | `RecordSectionCard(title = stringResource(R.string.report_section_pregnancy_dating), family = FieldFamilies.patient())` | When active GA exists: prominent `LabeledValue(label = report_ga_today_label, value = ..., emphasis = true)` followed by `datingTextRows(datingSummary)` rendered as `LabeledValue` / `LabeledValueRow` pairs, plus conditional `StatusModerateBg` box for `datingDiscordanceLine(...)`. When missing/unrecorded: single `LabeledValue(label = gestational_age, value = report_ga_not_recorded)` (`RecordType.Missing`). | Compute `val datingSummary = datingSummaryOf(patient)` and `val discordance = datingDiscordanceLine(patient.dating, datingSummary)`. Under Decision 2, format GA with `ltrIsolate(shortWeeksText(ga))` (or `"Gestational age: not recorded"` matching report empty rule). Date rows come from `datingTextRows(datingSummary)`. |
| **Page 1 Card 4: Obstetric & Medical History** | `RecordSectionCard(title = stringResource(R.string.report_section_history), family = FieldFamilies.background())` | GPA `LabeledValue` (using `FieldFamilies.obstetric()`) showing GPA formula + `pastDeliverySummary` (`emphasis = true`); then 4 stacked `LabeledValue` fields (`Drug allergies`, `Medical history`, `Surgical history`, `Family history`). When `classifyAllergy(patient.allergyHistory) is AllergyState.Recorded`, render `Drug allergies` in an `errorContainer` (`1.5.dp error` border, `10.dp` radius, `onErrorContainer`) alert card instead of a muted `LabeledValue`. | Blank history fields explicitly pass `NOT_RECORDED` (`"Not recorded"`). Never collapse or hide empty history fields. |
| **Page 1 Card 5: Previous Pregnancies** | `RecordSectionCard(title = stringResource(R.string.report_section_previous_pregnancies), family = FieldFamilies.obstetric())` | Empty state: `Text(report_empty_previous_pregnancies_format, style = RecordType.Missing, color = onSurfaceVariant)`. Non-empty: hairline-separated (`HorizontalDivider(color = outlineVariant)` between rows, **0 nested card boxes**) rows containing `Row` (`formatDmy(pp.dateText) ?: "-"` in `RecordType.CardName` + `BandChip(outcome)`), `RecordType.Secondary` details (`plurality · mode`, hospital, notes). | Dates strictly via `formatDmy(pp.dateText) ?: "-"`, wrapped in `ltrIsolate`. Order matches `patient.pastPregnancies` report ordering. |
| **Page 1 Card 6: Antenatal Care Visits Log** | `RecordSectionCard(title = stringResource(R.string.report_section_visits), family = FieldFamilies.patient())` | Empty: `RecordType.Missing` `"No clinical visits recorded yet"`. Non-empty: if `visits.hidden > 0`, show subtle `StatusIncompleteBg` (`1.dp StatusIncompleteBorder`, `10.dp` radius) banner `"${visits.hidden} earlier visits not shown"`; then hairline-separated rows: top `FlowRow`/`Row` (`BandChip("#${idx + 1 + visits.hidden}")`, `reportDate(visit.date)` wrapped in `ltrIsolate`, GA `BandChip`), 2-col `PlainField` grid (`Blood pressure` with conditional `StatusWord` in `error`/`StatusModerateText` for `"Raised BP - recheck"` / `"Severe-range BP - urgent assessment"`, and `Weight` = `formatWeightKg(visit.weightKg) ?: "-"`), and full-width `PlainField` (`Clinical findings and observations`, `visitText(visit)`). | Calls `reportVisits(patient, limit = 12)` (`MAX_REPORT_VISITS = 12`). Keeps oldest-first slice of most recent 12 visits. Zero horizontal scrolling. |
| **Page 2 Card 1: Continued Header** | `RecordSectionCard(family = FieldFamilies.patient())` | `Row`/`FlowRow` with `report_page2_subtitle` (`RecordType.CardName`), `BandChip(report_badge_continued)`, and single metadata line `Patient ID: ... · Print date: ...` (`RecordType.Secondary`). | Mirrors Page 2 HTML header cleanly in compact space (`~72dp` height). |
| **Page 2 Card 2: Ultrasound Biometry & Fetal Surveillance** | `RecordSectionCard(title = stringResource(R.string.report_section_scans), family = FieldFamilies.obstetric())` | Empty: `"No ultrasound scans recorded yet"`. Non-empty: hidden notice if `scans.hidden > 0`; hairline-separated scan blocks: top row (`reportDate(scan.date)`, `BandChip(scanGestationalAge(scan))`, optional `BandChip("${scan.imagePaths.size} photos in the clinic record")`), 2-col `PlainField` grid (`Presentation`, `Placenta`, `Amniotic fluid`, `EFW (g)` via `scanWeight(scan)`), full-width `PlainField` (`Clinical notes`, `scanNotes(scan)`). | Calls `reportScans(patient, limit = 7)` (`MAX_REPORT_SCANS = 7`). Replaces wide multi-column HTML table with zero horizontal scroll on `360dp`. |
| **Page 2 Card 3: Laboratory Investigations** | `RecordSectionCard(title = stringResource(R.string.report_section_labs), family = FieldFamilies.background())` | Empty: `"No laboratory test results recorded yet"`. Non-empty: hidden notice if `labs.hidden > 0`; hairline-separated lab rows: top `Row` (`lab.testName` `weight(1f)` + end-aligned result column `labResult(lab)` in `RecordType.KeyValue` + paired `StatusWord(lab_abnormal, StatusTone.High)` below result when `lab.isAbnormal`), secondary line (`reportDate(lab.date)` + optional `Ref: ${lab.referenceRange}`), optional extra line (`labExtra(lab)`). | Calls `reportLabs(patient, limit = 10)` (`MAX_REPORT_LABS = 10`). Never relies on color alone for abnormal results (`lab_abnormal` word always paired with `error` ink). |
| **Page 2 Card 4 & 5: Current & Stopped Medications** | Two `RecordSectionCard`s (`FieldFamilies.background()`) | **Current:** calls `meds = reportMedications(patient)`. Empty: `"No current medications recorded"`. Non-empty: hairline-separated rows (`m.name` in `RecordType.CardName`, `medicationDetail(m)` in `RecordType.Secondary`, `medicationDates(m)` in `RecordType.FieldLabel` wrapped in `ltrIsolate`). **Stopped:** rendered ONLY when `meds.stopped.isNotEmpty()`, identical row layout. | Uses existing `medicationDetail(m)` and `medicationDates(m)` so `frequency` stays excluded and dates match print/share formatting. |
| **Page 2 Card 6: Delivery Plan** | `RecordSectionCard(title = stringResource(R.string.report_section_delivery_plan), family = FieldFamilies.delivery())` | Calls `val rows = reportDeliveryRows(patient, datingSummary.edd?.let(::formatDmy))`. Each `ReportRow`: if `row.label == "Blood bank precautions"`, render in `errorContainer` (`1.5.dp error`, `10.dp` radius); otherwise render `LabeledValue(label = row.label, value = row.value, emphasis = row.label == "Estimated due date" || row.label == "Delivery Outcome")`. | Keeps exact `ReportRow.label` strings and ordering returned by `reportDeliveryRows`. Delivery fees never shown. |
| **Page 2 Card 7: Clinician Attestation** | `RecordSectionCard(title = stringResource(R.string.report_section_attestation), family = FieldFamilies.patient())` | English name & specialty (`RecordType.CardName` / `RecordType.Secondary`), Arabic signature block right-aligned via `PatientNameText` / explicit RTL paragraph (`شريف جمال رضوان` / `أخصائي التوليد وأمراض النساء وتأخر الحمل`), confidential footer (`Dr. Sherif Radwan Clinic · Tel 01201720808`). | Under Decision 4, no empty dashed `Stamp` box in interactive Compose UI. |

---

## 3. Per-element spec: `ui/report/PatientTwoPageReportDialog.kt`

| Element | Spec | Target / Constraint |
|---|---|---|
| **Outer Dialog & Surface** | `Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true))` wrapping `Surface(modifier = Modifier.fillMaxSize().testTag("patientTwoPageReportDialog"), color = MaterialTheme.colorScheme.background, shadowElevation = 0.dp)`. | Keeps `patientTwoPageReportDialog` tag and fullscreen presentation on both compact and tablet windows. |
| **Pinned Top Header Surface** | `Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp)` with `statusBarsPadding()` + `Column` + `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)` along bottom edge. | Eliminates navy shadow slab; adapts to Dark Theme (`#1A202E`). |
| **Header Row 1 (Actions Bar)** | `Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically)`: start slot holds Close `IconButton(onClick = onDismiss, modifier = Modifier.minimumTouchTarget().testTag("closeReportDialog"))` (`Icons.Default.Close`, `primary` tint, `contentDescription = stringResource(R.string.close)`). End slot holds `Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically)` with Share `IconButton(modifier = Modifier.minimumTouchTarget().testTag("shareReportButton"))` and `Button(shape = RecordShapes.Small, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).testTag("printReportButton"))` containing Print icon + `stringResource(R.string.print_pdf)` (`RecordType.Action`, `maxLines = 1` or wraps cleanly at extreme scale). | All 3 controls meet `48.dp`; `Print PDF` contrast `16.92:1` light / `7.72:1` dark (replaces `2.83:1` rose pill). Exact `shareReportButton` Intent & `printReportButton` `PatientPrintManager.printPatientReport` calls preserved. |
| **Header Row 2 (Title & Patient Name)** | `Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp))` with kicker `Text(stringResource(R.string.report_dialog_kicker), style = RecordType.FieldLabel, color = FieldFamilies.patient().label)` followed by `PatientNameText(name = patient.displayName, style = RecordType.HeaderName, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth())`. | Zero ellipsis at `11.sp` (`HeaderName` `22.sp/28.sp` bold); wraps naturally across `328dp` inner width; Arabic `منى رجب` auto-aligns RTL while header buttons stay consistent. |
| **Header Row 3 (Page Switcher)** | `var selectedPage by rememberSaveable { mutableIntStateOf(0) }`. `FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp).testTag("reportPageSwitcher"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp))` containing `RecordFilterChip(selected = selectedPage == 0, label = stringResource(R.string.report_page_1), onClick = { selectedPage = 0 }, tag = "reportPage1Chip", minTouchTarget = true)` and `RecordFilterChip(..., label = report_page_2, tag = "reportPage2Chip", minTouchTarget = true)`. | `48.dp` accessible tap target height (`minTouchTarget = true`); `FlowRow` guarantees zero horizontal clipping even at `fontScale = 1.3f` or `2.0f`. |
| **Scrollable Report Body** | `val page1Scroll = rememberScrollState()`, `val page2Scroll = rememberScrollState()`. `Box(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding())` containing `Column(Modifier.fillMaxSize().verticalScroll(if (selectedPage == 0) page1Scroll else page2Scroll).padding(RecordSpacing.L).testTag("reportPageContent"), verticalArrangement = Arrangement.spacedBy(RecordSpacing.M))` rendering Page 1 composables when `selectedPage == 0` and Page 2 composables when `selectedPage == 1`. | Replacing WebView per Decision 1 removes `reportWebView` `AndroidView`; separate scroll states prevent Page 1 scroll offset leaking into Page 2. |

---

## 4. All existing `testTag`s kept

| Existing `testTag` | Current Location (`PatientTwoPageReportDialog.kt`) | Slice 20 Status & Location |
|---|---|---|
| `patientTwoPageReportDialog` | Line 74: outer `Surface` | **KEPT** on outer `Surface` (`PatientTwoPageReportDialog.kt`). |
| `closeReportDialog` | Line 98: Close `IconButton` | **KEPT** on Close `IconButton`, `48.dp` target (`PatientTwoPageReportDialog.kt`). |
| `shareReportButton` | Line 136: Share `IconButton` | **KEPT** on Share `IconButton`, `48.dp` target (`PatientTwoPageReportDialog.kt`). |
| `printReportButton` | Line 158: Print `Button` | **KEPT** on Print `Button`, `48.dp` target (`PatientTwoPageReportDialog.kt`). |
| `reportWebView` | Line 212: `AndroidView(WebView)` | **RETIRED per Owner Decision 1** (verified via grep: zero `test` or `androidTest` assertions reference `reportWebView`). *If Owner selects the Decision 1 Alternative, keep `reportWebView` mounted behind a 3rd `reportPagePrintSheetChip` tab.* |
| *(Additive)* `reportPageSwitcher` | New | Additive `FlowRow` wrapper around page switcher chips. |
| *(Additive)* `reportPage1Chip` | New | Additive `RecordFilterChip` for Page 1. |
| *(Additive)* `reportPage2Chip` | New | Additive `RecordFilterChip` for Page 2. |
| *(Additive)* `reportPageContent` | New | Additive scrollable container for Page 1 / Page 2 sections. |

---

## 5. Acceptance checklist

- [ ] `PatientTwoPageReportDialog.kt` signature, `DialogProperties`, `shareReportButton` (`patientShareText` + `ACTION_SEND`), and `printReportButton` (`PatientPrintManager.printPatientReport`) match pre-edit behavior identically.
- [ ] All pure helpers/constants/enums/models in `ReportSections.kt` keep identical signatures, sorting, caps (`12` visits, `7` scans, `10` labs), and return strings (`git diff -w` clean on every existing pure function).
- [ ] `PatientPrintHtmlGenerator.kt`, `PatientPrintManager.kt`, `PatientShareText.kt`, and `PatientHubScreen.kt` show zero diff.
- [ ] Zero typography below `12.sp` (`labelSmall` completely removed).
- [ ] All interactive controls (`closeReportDialog`, `shareReportButton`, `printReportButton`, `reportPage1Chip`, `reportPage2Chip`) are `>= 48.dp` tall/wide.
- [ ] Dialog adapts cleanly to `AncTheme(darkTheme = true)` and `AncTheme(darkTheme = false)` with all measured contrast pairs `>= 4.5:1` text / `>= 3.0:1` non-text.
- [ ] On `360dp` width and `fontScale = 1.3f`: patient name (long English and Arabic `منى رجب`) does not clip or collide with action icons; no horizontal table overflow; all pairs stack gracefully above `1.3f`.
- [ ] `Stopped medications` card is omitted when `stopped` is empty; blank histories (`allergy`, `medical`, `surgical`, `family`) explicitly show `"Not recorded"`; delivery fees are never shown.
- [ ] Every displayed date uses `formatDmy` / `reportDate` / `formatDeliveryDate` (Owner Date Rule).
- [ ] Every existing kept `testTag` passes unchanged (`AncWorkflowTest`).
- [ ] Zero hardcoded user-facing strings in new Compose code; zero em dashes in UI copy or code comments.

---

## 6. Tests

### Existing unit & instrumentation tests that MUST pass unchanged
- `app/src/test/java/com/sherifradwan/ancnative/ui/report/ReportSectionsTest.kt` (extend with any pure helper assertions if new pure view-mapping helpers are extracted, without altering existing test assertions)
- `app/src/test/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGeneratorTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlEscapeTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintBpAlertTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintLayoutTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintObstetricSummaryTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintSampleDumpTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintSectionTitlesTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/report/ExportFixRoundTwoTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/ClinicalSummaryDatingTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/storage/EncryptedBackupTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt` (lines 682-687: `patientTwoPageReportDialog`, `shareReportButton`, `printReportButton`, `closeReportDialog`)
- `app/src/androidTest/java/com/sherifradwan/ancnative/ClinicalPatientCardTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/PatientHubDesignTest.kt`

### New Compose UI test (`app/src/androidTest/java/com/sherifradwan/ancnative/ReportDialogRedesignTest.kt`)
Create an instrumentation test file verifying on Compose test rule (`360dp` phone constraints + `Dark` + `fontScale = 1.3f`):
1. **Kept tags & page switching:** asserts `patientTwoPageReportDialog`, `closeReportDialog`, `shareReportButton`, `printReportButton`, `reportPage1Chip`, and `reportPage2Chip` exist, satisfy `assertHeightIsAtLeast(48.dp)` and `assertWidthIsAtLeast(48.dp)` for buttons; clicking `reportPage2Chip` switches from Page 1 sections (`Patient identity`, `Pregnancy dating`, `Antenatal care visits log`) to Page 2 sections (`Ultrasound biometry and fetal surveillance`, `Laboratory investigations`, `Current medications`, `Delivery plan`, `Clinician attestation`), and clicking `reportPage1Chip` switches back.
2. **Page 1 & Page 2 Level-3 clinical parity:** for populated `DemoPatient` (`Mona Ragab`, `G4 P1 + A2`, `110/70`, `A+`, `EDD 11/02/2027`, `Mothercare`, delivery fees `12000`): asserts all values appear in their respective sections, `12000` (fees) and medication `frequency` NEVER appear, and `15` visits / `9` scans / `12` labs clamp to `12` / `7` / `10` with `"3 earlier visits not shown"`, `"2 earlier scans not shown"`, `"2 older results not shown"`.
3. **Conditional & flagged clinical alerts:** for high-risk / Rh-negative / allergy / severe-BP / abnormal-lab / delivered archived record (`DEMO-ANC-004` shape), asserts `"High risk"`, `"Rh negative"`, `"Severe-range BP - urgent assessment"`, `"Abnormal"`, `"Anti-D Prophylaxis + Matched Packed RBCs Cross-match"`, and `"Stopped medications"` render; verifies `"Stopped medications"` is NOT composed when stopped list is empty.
4. **Empty-section state & blank histories:** minimal patient with blank histories/lists renders `"Not recorded"` for blank histories and exact fallback sentences (`"No clinical visits recorded yet"`, `"No ultrasound scans recorded yet"`, `"No laboratory test results recorded yet"`, `"No current medications recorded"`).
5. **RTL Arabic + `360dp` + `fontScale = 1.3f` + Dark theme:** renders Arabic name `"منى رجب"` and long English name on `width = 360.dp` with `fontScale = 1.3f` and `darkTheme = true`; verifies no horizontal bounds clipping on patient name, switcher chips, and action buttons, and verifies `shareReportButton` launches `Intent.ACTION_CHOOSER` wrapping `Intent.ACTION_SEND` with `patientShareText(patient)`.

---

## 7. Review requirement & Risks

- **Required Level-3 review gate (`PLAN.md` Section 0.13):**
  1. **Frontier-tier code & design review** verifying `git diff -w` leaves `PatientPrintHtmlGenerator.kt`, `PatientPrintManager.kt`, `PatientShareText.kt`, and all existing pure functions in `ReportSections.kt` untouched, plus visual fidelity against `mock_s20_report_dialog.html`.
  2. **Independent test review** verifying `ReportSectionsTest`, `PatientPrintHtmlGeneratorTest`, `ExportFixRoundTwoTest`, `AncWorkflowTest`, and the new `ReportDialogRedesignTest` run cleanly without relaxed or skipped assertions.
- **Risks & mitigations:**
  - *Risk:* Duplicate logic between `ReportSections.kt` HTML formatting helpers and new Compose cards could cause string drift (e.g., blank phone `"Unspecified"`, photo plural wording, BP severity threshold, or empty past-pregnancy wording). *Mitigation:* Call the existing pure functions directly (`reportVisits`, `reportScans`, `reportLabs`, `reportMedications`, `reportRisk`, `reportStatusLine`, `visitText`, `scanGestationalAge`, `scanNotes`, `scanWeight`, `medicationDetail`, `medicationDates`, `labResult`, `labExtra`, `reportDeliveryRows`) instead of reimplementing formatting in composables.
  - *Risk:* Long Arabic/English names or `fontScale = 1.3f` pushing header height too tall and starving scroll body on small phones. *Mitigation:* Header wraps cleanly at natural text height (`<= 190dp` even wrapped), leaving `>= 450dp` vertical scroll viewport inside a standard `640dp`-`800dp` phone height.
  - *Risk:* Builder tries to fix the separate `PrintManager` `Activity` context crash inside Slice 20. *Mitigation:* Explicitly banned in Never touch and Owner Decision 5; `printReportButton` invokes `PatientPrintManager.printPatientReport` identically to before.
