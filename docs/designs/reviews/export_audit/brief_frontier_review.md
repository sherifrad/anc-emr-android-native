You are the independent frontier-tier reviewer for a clinic app (Android Kotlin, antenatal care records with real patients). READ-ONLY: do not edit, create or delete any file, do not run git commands that change anything, do not run gradle. You may read files and run `git diff` / `git log` / `git show`.

Review the change `git diff main..export-rework -- app/src/main` in this repository (the PDF export and share text rework). Read the full files where needed:
- app/src/main/java/com/sherifradwan/ancnative/ui/report/ReportSections.kt (the one set of rules)
- app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt (builds the HTML that is printed as A4 PDF)
- app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientShareText.kt
- app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintManager.kt and PatientTwoPageReportDialog.kt (how it is loaded and printed)
The intended behaviour is written in docs/designs/reviews/export_audit/NEW_BEHAVIOUR.md and docs/designs/reviews/export_audit/SPEC_report_sections.md. Known and accepted: the stored medication `frequency` is deliberately not printed; fees are never printed; a bare-number fetal weight gets " g".

Find REAL defects only, each with a concrete patient example and the wrong printed output:
1. Clinical correctness: anything printed that is wrong, misleading, dropped or invented compared with the patient object (delivered and archived patients, risk tiers, newest rows kept, medication split, dates, delivery plan rows, Rh-negative line).
2. HTML safety: any patient or settings text reaching the HTML unescaped or escaped twice.
3. Android: will the WebView really load `fonts/ArslanWessamB.ttf` from assets with the base URL used in BOTH the print path and the preview dialog? Any risk from embedding the logo as base64 three times? Anything that can crash (null bitmap, missing resource)?
4. Print layout: with the CSS in the generator, can a row or section be cut across pages or overflow the A4 sheet? Does the "Page 1" / "Page 2" labelling stay true when page 1 content is long?
5. Tests: read app/src/test/java/com/sherifradwan/ancnative/ui/report/*.kt. Name behaviours that no test proves.
Output format: VERDICT PASS or FAIL; then a numbered findings list (severity serious / moderate / minor, file and line, concrete input, wrong output, suggested fix in one line); then "Checked and correct". No praise, no summary of what the code does.
