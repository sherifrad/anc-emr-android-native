# Export (PDF and share text) audit, 2026-11-13

Scope: `ui/report/PatientPrintHtmlGenerator.kt` (1,284 lines, one function), `PatientPrintManager.kt`, `PatientTwoPageReportDialog.kt`, `model/ClinicalSummaryGenerator.kt`.
Reviewers: alphacode, opencode GLM, opencode DeepSeek (outputs in this folder), plus the orchestrator's own read. A finding marked 3/3 was raised by all three lanes. Nothing was changed: the print and summary generators are protected files.

## A. The PDF shows wrong or misleading clinical content
| # | Finding | Agreement |
|---|---|---|
| A1 | Visits: sorted oldest first, then the first 12 are kept. With more than 12 visits the NEWEST are hidden behind "and N more not shown". Same for scans (oldest 7 kept). | 3/3 |
| A2 | A delivered patient still gets "Current Gestational Age" (keeps counting after birth), "Target Delivery Timing" and fetal kick-count advice. | 3/3 |
| A3 | Risk level: the app has low, medium, high; the PDF prints only "High Risk" or "Normal Risk", so medium prints as Normal. | 3/3 |
| A4 | "Active Medications" lists every medication, stopped ones included, in stored order, first 8 only. | 3/3 |
| A5 | Fixed text printed as if it were this patient's plan: "Special Recommendations" (kick counts...) and "Hospital & Blood Bank Precautions" (Anti-D and cross-match for every Rh-negative patient, "Routine Obstetric Hospital Protocol" for everyone else). | 3/3 |
| A6 | Placeholders in a document called "Certified Record" and "Certified electronically": contact "+20 100 000 0000", "Official Stamp & Signature", the fixed clinic name (the name from settings is read but never used), page 3 shows a grey box "[Original Ultrasound Scan Stored in Secure System]" instead of the photo. | 3/3 |
| A7 | Page numbers are fixed ("1 of 2", "2 of 2"). A long record flows onto more sheets than the label says. | 3/3 |

## B. Data the app holds that the PDF never prints
| # | Missing | Agreement |
|---|---|---|
| B1 | Clinical attention (the alert text on the patient screen) | 3/3 |
| B2 | Delivery plan: hospital, fees, postpartum (only "plan" is printed) | 3/3 |
| B3 | Visit notes when findings exist (only one of the two is printed) | 3/3 |
| B4 | Scan: fetal number, fetal heart rate; biometry is dropped whenever an impression exists | 3/3 |
| B5 | Medication: frequency, start and end date, prophylaxis mark, adverse notes | 3/3 |
| B6 | Lab: lab name, notes, category | 3/3 |
| B7 | Family history; date of birth; address | 2/3 |
| B8 | Previous pregnancy: pregnancy type; only one of place and details is printed; rows are in stored order | 2/3 |
| B9 | Archived status is not shown | 3/3 |

## C. Smaller errors
- Dates are mixed: dd/mm/yyyy in the dating box, yyyy-mm-dd everywhere else.
- "(Abnormal)" is the only abnormal mark on a lab; fine, but the unit is printed even when blank (double space).
- `pastDeliverySummary` and the GA strings are not passed through `escapeHtml` (generated text today, so low risk).
- "Rh- Negative" reads as a double negative; should be "Rh negative".
- A scan with weeks missing but days present prints "-".
- The share text (`ClinicalSummaryGenerator`) has its own copy of the same rules: it prints the gestational age for a delivered patient, ends with "Synthetic Clinical Prototype", lists stopped medications without dates, and omits allergies, histories, clinical attention and previous pregnancies.

## D. Code health
- `generateHtml` is one function of about 1,200 lines (CSS, data rules and layout mixed). The row rules cannot be unit tested without building the whole page.
- Two generators (PDF and share text) each decide what a visit, scan, lab or medication line contains. That breaks the "one rule, one place" project rule.
- Checked and correct: every patient text value goes through `escapeHtml`; labs are newest first; BP alerts use the shared `bpLevel`; delivery outcome line uses the shared function.

## Proposed fix plan (needs the owner's sign-off: protected files)
1. Correctness first (A1 to A4, A7, C): newest rows kept, delivered and archived banner, no gestational age after delivery, three risk tiers, medications split into current and stopped, real page count or no "of N", one date format.
2. Remove invented content (A5, A6): drop the fixed recommendations and precautions rows; clinic name and phone come from settings or the line is left out; "Clinical record" instead of "Certified"; photos embedded or the appendix removed.
3. Completeness (B1 to B9): add the missing fields; clinical attention at the top of page 1.
4. Structure (D): split the generator into a small data model per section (shared by PDF and share text) and the HTML template; unit tests on the section model.
Each step: builder lane, reviewer lane of another family, sample report regenerated, full gate.
