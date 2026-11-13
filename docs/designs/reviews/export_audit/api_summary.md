Rules already built in `ui/report/ReportSections.kt` (same package as the generator, so no import is needed). USE them; do not re-implement any of this inside the generator:
- `Limited<T>(rows: List<T>, hidden: Int)`; `reportVisits(patient, limit)`, `reportScans(patient, limit)` (newest kept, listed oldest first, `hidden` = older rows left out); `reportLabs(patient, limit)` (newest first, `hidden` = older rows left out).
- `reportMedications(patient): ReportMedications(current, stopped)`.
- `reportRisk(riskLevel): ReportRisk` with `.label` ("High risk", "Medium risk", "Low risk", "Risk not recorded") and values HIGH, MEDIUM, LOW, UNRECORDED.
- `reportStatusLine(patient): String?` (archived and/or delivered line, null for an active pregnancy).
- `reportGestationalAge(patient, asOf): String?` (null for a delivered patient).
- `reportDate(iso: String?): String` (dd/MM/yyyy or "Not recorded").
- `visitText(visit)`, `scanGestationalAge(scan)`, `scanNotes(scan)`, `scanWeight(scan)`, `medicationDetail(m)`, `medicationDates(m)`, `labResult(lab)`, `labExtra(lab)`, `deliveryPlanRows(patient): List<ReportRow(label, value)>`.
Every patient text value that goes into HTML must be wrapped in `escapeHtml(...)`. No emoji. No em dash character. No new comments except where a rule is not obvious; delete comments that only restate the code.
