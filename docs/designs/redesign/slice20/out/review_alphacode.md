REVIEW RESULT: DEFECTS FOUND — not PASS.

This is a static review of the slice-20 diff only; no tools were executed in this session. Line numbers are approximate new-file lines derived from the patch and must be confirmed against the checked-out file before filing bugs.

DEFECTS

D1 — Likely missing string resource `print_pdf` (compile-blocking / runtime crash)
- PatientTwoPageReportDialog.kt: print IconButton uses `stringResource(R.string.print_pdf)` (approx. new-file line 264).
- strings.xml: the slice adds 65 entries; `print_pdf` is not among them. The previous code hardcoded "Print PDF", so this slice defines nothing for it (it may pre-exist elsewhere — unverified).
- Scenario: `:app:compileDebugKotlin` fails on unresolved `R.string.print_pdf`; if a stale/merged R exists, `Resources.NotFoundException` when the Print PDF button composes.
- Same watch: `stringResource(R.string.close)` on the close IconButton (approx. line 158) — old code hardcoded "Close", so `close` may likewise be missing.
- Severity: High if absent.

D2 — Hardcoded attesting clinician identity and phone on every report (clinical data integrity)
- ReportSections.kt: `private const val CLINIC_PHONE = "01201720808"` (approx. line 266) used in ReportAttestationCard footer (approx. line 1099); `report_doctor_name_en/ar` and `report_doctor_specialty_en/ar` rendered hardcoded (approx. lines 1085-1090); `report_clinic_subtitle_full` is a hardcoded fallback subtitle.
- strings.xml: approx. lines 1093-1097 carry the doctor name/specialty/footer entries.
- Scenario: any clinic other than this one prints a two-page report whose attestation block names "Dr. Sherif Radwan · Tel 01201720808" — a misattributed legal clinical document. `securitySettings` is passed in (clinic name is consumed) but doctor identity and phone ignore it.
- Severity: High (misattributed medical record; legal/clinical-safety).

D3 — Patient name announced twice by screen readers (accessibility: duplicate reading)
- PatientTwoPageReportDialog.kt: header renders `PatientNameText(displayName, HeaderName)` (approx. lines 285-290).
- ReportSections.kt: ReportPatientIdentityCard renders `PatientNameText(displayName, SectionTitle)` again (approx. lines 410-414).
- Scenario: TalkBack linear exploration reads the patient's full name twice within the first report view. Contradicts the slice's stated "no duplicate reading" priority.
- Severity: Medium.

D4 — Page-switcher group contentDescription duplicates chip labels (accessibility)
- PatientTwoPageReportDialog.kt: FlowRow carries `.semantics { contentDescription = pageSwitcherDescription }` ("Report pages") plus `testTag("reportPageSwitcher")`, wrapping RecordFilterChip("Page 1")/("Page 2") (approx. lines 296-306).
- Scenario: a screen reader may announce the group description "Report pages" and then each chip label ("Page 1, tab, selected") — redundant/confusing; a contentDescription on a non-interactive container can also misroute focus.
- Severity: Low-Medium; verify with TalkBack.

D5 — Untitled RecordSectionCard branch uses uniform padding (visual inconsistency)
- RecordSectionCard.kt: the `title == null && action == null` branch pads all sides with `padding(RecordSpacing.L)` (approx. lines 79-83), while the titled branch keeps `padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp)` on the card and `padding(end = 12.dp)` on content. Slice callers passing `title = null`: ReportClinicMetaCard (approx. line 355) and ReportPageTwoHeaderCard (approx. line 777).
- Scenario: the clinic-meta card and page-2 header card render with different inner padding (e.g. top 4dp→L, end 4dp→L) than every titled section card in the same report — inconsistent section spacing.
- Severity: Low.

D6 — Removed test contract / possible dead code (test regression)
- PatientTwoPageReportDialog.kt: `testTag("reportWebView")` was removed with the WebView; content now lives under `testTag("reportPageContent")`; `PatientPrintHtmlGenerator.generateHtml` is no longer invoked from the dialog.
- Scenario: instrumented tests asserting `reportWebView` fail with NoMatchingViewException. If PatientPrintHtmlGenerator has no remaining caller, it is dead code (unverified — check callers).
- Severity: Medium (test suite); dead-code status unverified.

VERIFIED SAFE / NO DEFECT

- StatusTag.kt / BandChip: new `tinted` and `wrap` default to `false`; null-tone and Neutral branches render identically to before when called with defaults; `maxLines` stays 1 unless `wrap = true`. Existing BandChip callers are unaffected.
- RecordSectionCard.kt: `title: String?` remains source-compatible for existing String-titled callers; the titled branch keeps outer padding (16/4/4/16) and content end padding (12dp); `heading()` semantics added on the title (a11y improvement).
- strings.xml: every format specifier matches its argument count (%1$s / %2$s / %1$d); no em dashes in the added strings.
- Clinical/display parity preserved: GA/dating delivered/not-recorded branches, BP severe/raised StatusWord, abnormal-lab error coloring, allergy states, previous-pregnancy outcome mapping, and visit/scan/lab caps (12/7/10) all route through existing model helpers; nothing is dropped or made uneditable (this dialog is display-only).
- Imports in both edited Kotlin files appear consistent (`ExperimentalLayoutApi` opted wherever FlowRow is used; removed WebView/AndroidView/SuppressLint imports match the removals).

UNVERIFIED — needs compile/run before final sign-off
- `reportRisk` / `ReportRisk` resolution in ReportSections.kt (used in ReportPatientIdentityCard, approx. line 400) — confirm same-file or imported.
- Actual value of `RecordSpacing.L` (quantifies D5 impact).
- `R.string.print_pdf` / `R.string.close` existence (D1).
- TalkBack traversal of header + identity card and the page switcher (D3/D4).
- Espresso tests referencing `reportWebView` (D6).

Suggested fix order: D1 (add or confirm the string resources), D2 (drive doctor name/phone from `securitySettings` or make them configurable), D3/D4 (TalkBack verification, then remove the duplicate name announcement and/or group contentDescription), D5 (align untitled-branch padding with the titled branch), D6 (update or replace the removed test tag).
[Tokens] upload: 36766 download: 31784 cache_read: 0 cache_write: 0

