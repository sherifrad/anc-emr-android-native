You are an independent reviewer of the reworked PDF export of an antenatal clinic app (Android Kotlin). You did not write this code. The code below builds the rows of a patient report that is printed and handed to patients and hospitals. Find real defects only: a place where the printed report would be wrong, misleading, or would drop or invent clinical data. Give each finding a concrete patient example.

Intended rules:
- Visits and scans: when there are more than the print limit, the NEWEST are kept, listed oldest first, and a row says how many earlier ones are hidden. Labs: newest first, older ones hidden.
- Medications: split into current and stopped by the app's own status rule; nothing dropped. The stored `frequency` field is a legacy default and is deliberately NOT printed.
- A delivered patient never gets a current gestational age; the status line says "Delivered <date> · <mode> · <outcome>"; an archived patient gets "Archived record".
- Risk: High, Medium, Low; a risk text the app cannot place in a tier is printed as written; blank gives "Risk not recorded".
- Dates print as dd/MM/yyyy; a date that is not ISO text prints as stored.
- Nothing is invented: a missing value prints "Not recorded" or the line is left out. Fees are never printed.
- Every patient text put into HTML is escaped exactly once.
Check in particular: sorting when a date is blank or not ISO text (text sort of mixed formats); ties; a visit or scan with a date in the future; `hidden` counts; the true visit number in the `#` column; anything escaped twice or not at all; a status text with different capital letters ("active" versus "Active", "Archived"); null handling.
Answer: 1. VERDICT PASS or FAIL. 2. Findings: severity (serious / moderate / minor), the function or line, the concrete input, the wrong output. 3. Anything you checked and found correct.
