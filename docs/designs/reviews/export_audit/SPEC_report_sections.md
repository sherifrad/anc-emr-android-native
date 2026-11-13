# Report sections: the one set of rules for the PDF and the share text

New file `app/src/main/java/com/sherifradwan/ancnative/ui/report/ReportSections.kt`, package `com.sherifradwan.ancnative.ui.report`. Pure Kotlin: no Android import, no HTML. Everything `internal`. Existing helpers to REUSE (do not copy their logic):
- `com.sherifradwan.ancnative.ui.parseRiskTier(risk: String?): PatientRiskTier` with `enum class PatientRiskTier { HIGH, MEDIUM, LOW, UNRECORDED }` (package `com.sherifradwan.ancnative.ui`).
- `com.sherifradwan.ancnative.model.medicationStatusLabel(status: String, isNonCompliant: Boolean): String`, `MEDICATION_LABEL_ACTIVE`, `medicationInstructions(dosage: String?, route: String?, notes: String?): String`.
- `com.sherifradwan.ancnative.model.formatDmy(iso: String?): String?` (dd/MM/yyyy; returns the trimmed input when it is not an ISO date; null when blank).
- `com.sherifradwan.ancnative.model.deliveryOutcomeLine()` extension on `DemoPatient` (null when not delivered).
- `com.sherifradwan.ancnative.model.parseDeliveryNotes(notes: String?)` returning `DeliveryNoteSections(hospital, fees, plan, postpartum)` (all nullable Strings; `internal` in the same module).
- `patient.dating?.calculateGestationalAgeResult(asOf: LocalDate)?.formatted` (String).
Model classes (package `com.sherifradwan.ancnative.model`): `DemoPatient`, `DemoVisit(date, bloodPressure, weight, findings, notes)`, `DemoUltrasoundScan(date, gestationalAgeWeeks: Int?, gestationalAgeDays: Int?, fetalNumber, presentation, placentalSite, amnioticFluid, fetalHeartRate, biometryBpd, biometryHc, biometryAc, biometryFl, estimatedFetalWeight, impression, imageUris)`, `DemoLabInvestigation(date, testName, result, unit, referenceRange, isAbnormal, notes, labName)`, `DemoMedicationRecord(name, dosage, route, frequency, startDate, endDate, status, notes, isProphylaxis, isNonCompliant, adverseNotes)`. All dates are ISO text `yyyy-MM-dd`. Nullable Strings may also be blank: treat blank as missing.

## API and oracles
```kotlin
internal const val NOT_RECORDED = "Not recorded"
internal data class Limited<T>(val rows: List<T>, val hidden: Int)
```
- ORACLE-S1 `reportVisits(patient: DemoPatient, limit: Int): Limited<DemoVisit>`: visits sorted by date oldest first; when there are more than `limit`, the NEWEST `limit` are kept (still oldest first) and `hidden` is the number of older ones left out. 15 visits, limit 12: rows are visits 4 to 15, hidden 3. 5 visits: all 5, hidden 0.
- ORACLE-S2 `reportScans(patient, limit): Limited<DemoUltrasoundScan>`: same rule as S1.
- ORACLE-S3 `reportLabs(patient, limit): Limited<DemoLabInvestigation>`: sorted NEWEST first, first `limit` kept, `hidden` = older ones left out.
- ORACLE-S4 `data class ReportMedications(val current: List<DemoMedicationRecord>, val stopped: List<DemoMedicationRecord>)`, `reportMedications(patient): ReportMedications`: `current` holds the medications whose `medicationStatusLabel(status, isNonCompliant)` equals `MEDICATION_LABEL_ACTIVE`, `stopped` the rest; each list sorted by `startDate` newest first. Nothing is dropped.
- ORACLE-S5 `enum class ReportRisk(val label: String) { HIGH("High risk"), MEDIUM("Medium risk"), LOW("Low risk"), UNRECORDED("Risk not recorded") }`, `reportRisk(riskLevel: String?): ReportRisk` mapped one to one from `parseRiskTier`. "moderate" gives MEDIUM, "high" gives HIGH, "low" gives LOW, null or blank gives UNRECORDED.
- ORACLE-S6 `reportStatusLine(patient): String?`: null for an active, not delivered patient. Delivered: the text of `patient.deliveryOutcomeLine()`. Archived (`status == "archived"`): "Archived record". Both: "Archived record · " followed by the delivery line.
- ORACLE-S7 `reportGestationalAge(patient, asOf: LocalDate): String?`: null when `patient.delivered` is true or there is no dating or no result; otherwise the formatted gestational age. A delivered patient NEVER gets a current gestational age.
- ORACLE-S8 `reportDate(iso: String?): String`: `formatDmy(iso) ?: NOT_RECORDED`. "2026-03-01" gives "01/03/2026".
- ORACLE-S9 `visitText(visit): String`: findings and notes, each trimmed, blank ones left out, joined with " · "; NOT_RECORDED when both are missing. Both present: both printed.
- ORACLE-S10 `scanGestationalAge(scan): String`: weeks and days "12w + 3d"; weeks only "12w"; days only "3d"; neither "-".
- ORACLE-S11 `scanNotes(scan): String`: parts in this order, missing ones left out, joined with " · ": "Fetuses: <fetalNumber>", "FHR: <fetalHeartRate>", the impression, the biometry ("BPD: x", "HC: x", "AC: x", "FL: x", each only when present). NOT_RECORDED when nothing is present. An impression never hides the biometry.
- ORACLE-S12 `scanWeight(scan): String`: the estimated fetal weight; when the text has no letter in it, " g" is appended ("1200" gives "1200 g", "1.2 kg" stays); "-" when missing.
- ORACLE-S13 `medicationDetail(m): String`: `medicationInstructions(m.dosage, m.route, m.notes)` then, when present, the frequency, "Prophylaxis" when `isProphylaxis`, and "Adverse: <adverseNotes>", joined with " · " (blank parts left out; may be empty).
- ORACLE-S14 `medicationDates(m): String`: "From 01/03/2026" when there is no end date; "01/03/2026 to 15/04/2026" when both; "" when the start date is blank.
- ORACLE-S15 `labResult(lab): String`: result and unit joined by one space, unit left out when blank (no trailing space). `labExtra(lab): String`: lab name and notes joined with " · ", "" when both missing.
- ORACLE-S16 `data class ReportRow(val label: String, val value: String)`, `deliveryPlanRows(patient): List<ReportRow>`: from `parseDeliveryNotes(patient.obstetricNotes)`, rows "Planned delivery" (plan), "Hospital" (hospital), "Postpartum" (postpartum), each only when present, in that order. Fees are never included. Empty list when nothing is recorded.
Comments: at most one short KDoc line per function, only where the rule is not obvious from the name. No em dash character anywhere.
