# Pre-push phone test (2026-11-18), QA copy com.sherifradwan.anc.qa on the owner's phone

Mode: testing, Full path. Builder: Hydrogen (agy). Safety reviewer: alphacode (two rounds, PASS). Visual reviewer: Gemini Flash (agy), verified by the orchestrator against the screenshots. Script: .agents/scripts/device_qa_prepush.sh (read-only: launch, list, tabs, editors, pickers, settings, report, logcat; never saves, deletes, prints or enters a PIN; QA package only).

Result of the final run: 18 PASS, 0 FAIL, 1 SKIP (12 sp text size is not measurable from UI dumps). The first run had 3 FAIL, all false alarms in the script (a card clipped by the screen edge, two id lookups), fixed and re-run.
Logcat: no FATAL EXCEPTION, ANR or AndroidRuntime lines. Phone density, size and settings unchanged; clinic app untouched.

Observations for the owner (none blocks the push):
1. Date field labels still say "(YYYY-MM-DD)" (Visit date, Scan date) although dates are picker-only now; the format hint can go.
2. Hub header and scan editor show an Arabic patient name at the left edge; the patient list, visit editor banner and report show it at the right. Slice 4 and 15 did not specify this; decide the rule.
3. Report page 2 shows a bare "-" chip and "-" for EFW when a scan has no GA or EFW (this matches the print sheet); "Not recorded" would read better.
4. Meds tab still has the old Title Case and "+ Prescribe" (Meds stage 2 is not done).
5. Settings dialog title "Clinic & System Settings" stays Title Case on purpose (a test asserts it).
Not verified on the phone: dark mode, scrolling below the fold on each screen, delete and undo flow, camera, real saves, and the print and share actions (deliberately not tapped).
