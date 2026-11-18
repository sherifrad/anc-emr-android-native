You are the BUILDER for print v2 slice S1 (repo /Users/sherifgamalradwan/anc-emr-android-native). Read docs/designs/reviews/export_print_v2/BUILD_PLAN.md first and follow its "Data rules". Create ONLY app/src/main/java/com/sherifradwan/ancnative/ui/report/ReportData.kt (new, pure Kotlin, no Android or Compose imports, package com.sherifradwan.ancnative.ui.report). Read, do not edit: model/ClinicalModels.kt (DemoUltrasoundScan, DemoLabInvestigation, DemoAttachment), model/DemoModels.kt (DemoPatient, DemoVisit), model/LabResultFlag.kt, ui/report/ReportSections.kt and ui/report/PatientPrintHtmlGenerator.kt (see how dates are formatted and how visits and scans are currently listed; REUSE their date, GA and parsing helpers by calling them, never copy them; if a needed helper is private, make a minimal note in your reply instead of editing the file).
API (immutable data classes plus top-level pure functions, KDoc one line each):
 data class ScanOutcome(val typeLabel: String?, val presentation: String?, val measurements: List<Pair<String,String>>, val extras: List<String>)
 fun scanOutcomeFor(scan: DemoUltrasoundScan): ScanOutcome
 fun latestScanForVisit(patient: DemoPatient, visit: DemoVisit): DemoUltrasoundScan?
 fun unlinkedScans(patient: DemoPatient): List<DemoUltrasoundScan>
 data class LabCell(val value: String, val flag: LabResultFlag?, val abnormalNoDirection: Boolean)
 data class LabComparisonTable(val category: String, val dates: List<String>, val rows: List<LabRow>) with LabRow(val label: String, val cells: List<LabCell?>)
 data class SingleLab(val test: String, val date: String, val result: String, val flagMark: String?)
 data class LabReport(val comparisons: List<LabComparisonTable>, val singles: List<SingleLab>)
 fun labReport(patient: DemoPatient): LabReport
 data class PictureCandidate(val key: String, val sourceLabel: String, val date: String, val linkedRecordId: String?, val location: String)
 fun pictureCandidates(patient: DemoPatient): List<PictureCandidate>
 data class Figure(val number: Int, val candidate: PictureCandidate)
 fun figuresFor(candidates: List<PictureCandidate>, selectedKeys: Set<String>): List<Figure>
 fun figureRefsFor(recordId: String, figures: List<Figure>): List<Int>
Rules: blank or whitespace values are dropped; stable ordering; never throw on odd data (unparseable dates sort last, keep the original text); dates are the stored ISO text unless the existing helper formats them. No UI strings here except neutral labels already in the model; the caller localises. No em dashes, antislop, no dead code.
Then run ./gradlew --offline :app:compileDebugKotlin -q . Final reply: 4 lines (what exists, any private helper you could not reuse).
