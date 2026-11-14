You are the BUILDER for fix round 3 in this Android Kotlin repository. You may edit ONLY these four files:
app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt
app/src/main/java/com/sherifradwan/ancnative/ui/report/ReportSections.kt
app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientShareText.kt
app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientTwoPageReportDialog.kt
Do NOT edit tests or any other file, do NOT run git commands that change anything, no network, no installs. You may run `./gradlew --offline :app:compileDebugKotlin -q`.
Read docs/designs/reviews/export_audit/SPEC_fix3.md and implement every ORACLE (P1 to P8) exactly. Read the whole CSS in PatientPrintHtmlGenerator.kt first so a later rule does not undo your change. Do not change the screen layout outside `@media print` except for ORACLE-P7. Keep every patient text escaped exactly once. No em dash character, no generic comments. When finished print one line per ORACLE saying what you changed.
