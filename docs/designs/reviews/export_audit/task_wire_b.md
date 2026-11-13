Edit `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt`, the PAGE TEMPLATE part (the HTML string, shown below) and a few CSS rules. The data part above it is edited by someone else at the same time and WILL define these vals; use them by these exact names: `clinicName: String` (already HTML-escaped), `clinicSpecialty`, `doctorOfficialTitle`, `printDateStr`, `currentGa: String?`, `statusLine: String?`, `risk: ReportRisk`, `isRhNegative: Boolean`, `datingMatrixHtml`, `datingSummary`, `pastDeliverySummary`, `prevPregRows`, `visitsRowsHtml`, `scansRowsHtml`, `labsRowsHtml`, `currentMedsRowsHtml: String`, `stoppedMedsRowsHtml: String` ("" when there are none), `logoImgTag`, `ageFormatted`, and the constant `CLINIC_PHONE`. These no longer exist and must not be used: `clinicNameEn`, `totalPages`, `page3Html`, `riskLevel`, `isHighRisk`, `currentGaFormatted`, `medsRowsHtml`.

Rules already built in `ui/report/ReportSections.kt` (same package as the generator, so no import is needed). USE them; do not re-implement any of this inside the generator:
- `Limited<T>(rows: List<T>, hidden: Int)`; `reportVisits(patient, limit)`, `reportScans(patient, limit)` (newest kept, listed oldest first, `hidden` = older rows left out); `reportLabs(patient, limit)` (newest first, `hidden` = older rows left out).
- `reportMedications(patient): ReportMedications(current, stopped)`.
- `reportRisk(riskLevel): ReportRisk` with `.label` ("High risk", "Medium risk", "Low risk", "Risk not recorded") and values HIGH, MEDIUM, LOW, UNRECORDED.
- `reportStatusLine(patient): String?` (archived and/or delivered line, null for an active pregnancy).
- `reportGestationalAge(patient, asOf): String?` (null for a delivered patient).
- `reportDate(iso: String?): String` (dd/MM/yyyy or "Not recorded").
- `visitText(visit)`, `scanGestationalAge(scan)`, `scanNotes(scan)`, `scanWeight(scan)`, `medicationDetail(m)`, `medicationDates(m)`, `labResult(lab)`, `labExtra(lab)`, `deliveryPlanRows(patient): List<ReportRow(label, value)>`.
Every patient text value that goes into HTML must be wrapped in `escapeHtml(...)`. No emoji. No em dash character. No new comments except where a rule is not obvious; delete comments that only restate the code.

Changes to the template:
1. Everywhere: `$clinicNameEn` becomes `$clinicName`. The `<title>` becomes `Clinical Record - ${escapeHtml(patient.displayName)} - $clinicName`.
2. Page 1 header meta: delete the row "Type: Certified Record". The page row becomes `Page:` `1`. Page 2 badge text becomes `Page 2`.
3. Directly after the page 1 `</header>`: when `statusLine != null`, `<div class="status-banner">${escapeHtml(statusLine)}</div>`.
4. Patient card: "Rh- Negative" becomes "Rh negative". Under the phone line add, when `patient.addressText` is not blank, `<div class="pt-phone">Address: ...</div>`, and when `patient.dateOfBirth` is not blank add `<div class="pt-phone">Date of birth: ${escapeHtml(reportDate(patient.dateOfBirth))}</div>`. The risk badge becomes one `when (risk)`: HIGH `<span class="badge badge-danger">`, MEDIUM `<span class="badge badge-warning">`, LOW `<span class="badge badge-success">`, UNRECORDED `<span class="badge">`, each holding `${risk.label}`.
5. Directly after the patient card `</section>`: when `patient.clinicalAttention` is not blank, `<div class="attention-box"><span class="k">Clinical attention</span><span class="v">${escapeHtml(patient.clinicalAttention)}</span></div>`.
6. The `ga-highlight` box: when `currentGa != null` print `Gestational age today: $currentGa`; else when `patient.delivered` print `${escapeHtml(statusLine)}`; else print `Gestational age: not recorded`.
7. History list: wrap `pastDeliverySummary` in `escapeHtml(...)`. After "Surgical History" add a "Family History:" row built the same way from `patient.familyHistory`.
8. Medications: the section title "Active Medications" becomes "Current Medications", the table header second column becomes "Dates" (widths 70% and 30%), body `$currentMedsRowsHtml`. After that section, when `stoppedMedsRowsHtml.isNotEmpty()`, add a second identical section titled "Stopped Medications" with body `$stoppedMedsRowsHtml`.
9. Last section: the title becomes "Delivery Plan". Rows inside `history-list`, in this order: the existing "Delivery Outcome" row (keep); one row per `deliveryPlanRows(patient)` entry (`<span class="k">${escapeHtml(row.label)}:</span><span class="v">${escapeHtml(row.value)}</span>`); when `!patient.delivered` and `datingSummary.edd != null`, a row "Estimated due date:" with `formatDmy(datingSummary.edd)`. DELETE the rows "Planned Delivery Mode", "Target Delivery Timing", "Hospital & Blood Bank Precautions" and "Special Recommendations". When the patient is not delivered and `deliveryPlanRows(patient)` is empty, add a row `<span class="k">Plan:</span><span class="v">Not recorded</span>`.
10. Signature box: `<div class="doc-name">Dr. Sherif Radwan</div>`, then `<div class="doc-sign" lang="ar" dir="rtl">شريف رضوان</div>`, then the existing doc-title, then `<div class="stamp-placeholder">Stamp</div>`. Do not use `PreviousPregnancyFormRules.DEFAULT_CLINICIAN_ID` any more.
11. Footers: page 1 `<span>Confidential medical record · $clinicName · $doctorOfficialTitle · Tel $CLINIC_PHONE</span><span>Page 1</span>`; page 2 `<span>$clinicName · $doctorOfficialTitle · Tel $CLINIC_PHONE</span><span>Page 2</span>`. Delete `$page3Html` and the page 3 comment.
12. Page 2 header subtitle becomes "Scans, laboratory results, medications and delivery plan".
CSS (second excerpt): after the `.badge-navy { ... }` rule add
`.badge-warning { background: #fef3c7; color: #92400e; border: 1px solid #f59e0b; }`
`.status-banner { background: #1e293b; color: #ffffff; font-weight: 700; font-size: 10pt; padding: 5px 10px; border-radius: 4px; margin-bottom: 6px; }`
`.attention-box { background: #fff7ed; border: 1px solid #c2410c; color: #7c2d12; font-size: 9pt; padding: 5px 10px; border-radius: 4px; margin-bottom: 6px; display: flex; gap: 8px; }`
`.attention-box .k { font-weight: 700; white-space: nowrap; }`
`.doc-sign { font-family: 'Aref Ruqaa', 'Noto Naskh Arabic', serif; font-size: 20pt; line-height: 1.3; color: #1e293b; }`
and add at the very top of the `<style>` block: `@font-face { font-family: 'Aref Ruqaa'; src: url('fonts/ArefRuqaa-Regular.ttf'); }`.
Delete the CSS rules `.photos-grid`, `.photo-card`, `.photo-preview-placeholder`, `.photo-caption` (keep `.photo-badge`).
Output SEARCH/REPLACE blocks only. Each SEARCH must be copied exactly from the excerpt and be unique in the file (the header and footer of page 1 and page 2 look alike: include enough neighbouring lines). Prefer several small blocks. Close every block with `>>>>>>> REPLACE`.
