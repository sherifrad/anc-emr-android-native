You are an independent design and copy auditor. READ-ONLY: do not edit, create or delete any file; do not run gradle or git commands that change anything.
Audit a printed clinical document against the "antislop" rules. Read these rule files first:
- /Users/sherifgamalradwan/.claude/skills/antislop/SKILL.md (core; the Delivery Gate at its end is the checklist)
- /Users/sherifgamalradwan/.claude/skills/antislop-copywriting/SKILL.md
- /Users/sherifgamalradwan/.claude/skills/antislop-ui/SKILL.md
- /Users/sherifgamalradwan/.claude/skills/antislop-human/SKILL.md
- /Users/sherifgamalradwan/.claude/skills/antislop-layoutmobile/SKILL.md
- /Users/sherifgamalradwan/.claude/skills/antislop-code/SKILL.md
Then audit:
- the generated sample page app/build/print-sample/report.html (a two-page A4 patient record; it is also previewed on a 360 dp phone inside the app; there is a `@media screen and (max-width: 540px)` block),
- its source app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt, plus ReportSections.kt and PatientShareText.kt in the same folder (comments and printed wording),
- the in-app preview dialog app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientTwoPageReportDialog.kt (buttons, labels, content descriptions).
Context you must respect (owner's direction, not slop): this is a medical record for an obstetric clinic, calm and dense by design (dials ENERGY 1 / RHYTHM 1 / MOTION 1); the palette is navy, rose and teal from the clinic logo; the owner asked for the Arabic signature name and title in a calligraphic font, the clinic phone 01201720808, and the word "Specialist". It is a printed document, so rules about hover, keyboard focus, loading states, navigation and animation apply only to the in-app preview dialog.
Write the Delivery Gate report exactly as the core skill demands: for every gate item, one line `R-xx PASS|FAIL|N/A: evidence` (quote the line or CSS rule you checked). For contrast, compute the ratio for every text colour and background pair you find in the CSS and list each pair with its ratio. Then a numbered list of FAIL findings only, most serious first, each with the exact file, line and the fix in one line. Do not rewrite the document; do not praise.
