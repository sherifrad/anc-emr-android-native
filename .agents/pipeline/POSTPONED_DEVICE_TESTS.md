# Postponed device tests (owner decision 2026-10-31: live phone testing postponed)
Run on the Honor 400, QA copy `com.sherifradwan.anc.qa` only (PIN 121212), demo data. Never the clinic app. Each item: what to do -> what must happen.

## Round 3n (IVF day 6 and redating hint) - built and unit-tested, NOT seen on a device
1. Dating editor, method IVF transfer: a third chip "Day 6" is visible, wraps cleanly at large font; pick it, set a transfer date, save -> due date = transfer + 260 days; day 3 = +263, day 5 = +261.
2. Gestational age on the transfer date shows 2w3d / 2w5d / 2w6d for day 3 / 5 / 6.
3. Ultrasound editor with a dating present: scan GA 10+ days away from the calculated GA -> hint "Scan GA differs from the current dating by N days (more than X)..." appears; a close value (within the limit) shows no hint; blank or invalid GA shows no hint and no crash. Saving is never blocked.

## Round 3m (visit checks) - phone-verified except
4. Blood pressure 140-159 / 90-109 (for example 145/92) shows the RAISED hint while typing and on the saved visit (only the severe level 165/105 was seen).
5. Large font scale (1.3): the new field errors and the alert label stay readable, nothing clipped.

## Round 3l (save correctness) - phone-verified except
6. The "due date was not updated" notice cannot be triggered by hand; covered by a unit test only. Optional: force a failure with a debug build.
7. androidTest ClinicalSaveAtomicityTest: written but the whole androidTest folder does not compile (tracker a08). After a08, run it on the QA copy. Note it passes without the fix (stale-revision case only).

## Earlier "device pass 2" items (tracker)
8. TalkBack walk-through of the patient list, workspace and visit editor.
9. Fingerprint unlock on Android 11 and later.
10. Restore confirmation dialog, then backup, restore and print-to-PDF end to end.
11. UI/UX cluster items still open: UIUX_BACKLOG.md DV-7, DV-9, DV-11, DV-12, DV-14, N-4, N-6 to N-9 (see that file).

## Rounds 3o and 3p (print) - built, unit-tested, NOT seen on a device
12. Print preview of a patient whose visit note is "Hb <10, BP >140/90 & stable": the text shows in full (no cut at "<"), and Print PDF produces the same text.
13. Print preview: each section title appears once (no capital duplicate beside it); the dialog title "Clinical Medical Record" is fully visible (two lines allowed) and the header buttons still fit.

## RESULTS of the phone pass on 2026-10-31 (Honor 400, QA copy, demo data)
- Item 1 PARTLY: Day 6 chip is visible and wraps below Day 3/Day 5 cleanly. The due date for a day-6 transfer could not be entered reliably by hand (typing into the form was flaky); covered by unit tests only. Still open: run once more by hand.
- Item 3 DONE: the redating hint shows at scan GA 25w0d against a 21w5d dating (23 days off, limit 10) and disappears at 22w0d.
- Item 4 DONE: 145/92 shows "Raised blood pressure (140/90 or above) - recheck" while typing.
- Items 12 and 13 DONE: a visit note containing < > & prints in full; every section title appears once; the dialog title "Clinical Medical Record" is fully visible on two lines; header buttons fit.
- NEW findings from the print preview (not part of those rounds): the visits table breaks words mid-word in narrow columns ("Gestati onal Age", "Pressu re", "Weigh t", "120/8 0", "165/1 05") and the medications header "Frequen cy"; "Official Stamp & Signature" is printed twice in the attestation box. Added to UIUX_BACKLOG.md as P-1 and P-2.
- Not done yet: items 2, 5-11 (GA on the transfer date, large font checks, TalkBack, fingerprint, restore dialog, backup/restore/print end to end).


## Built and tested, NOT seen on a device (added 2026-10-31, later rounds)
14. Severe blood pressure: after saving a visit with 165/105, the patient list card shows a "Severe BP" badge and a line, the workspace attention banner shows the same line (even if no attention note was typed), and the printed report shows the BP label; 145/92 shows the raised label on the print only.
15. The same patient shows the same previous-delivery history ("Prev 1 CS, 2 SVD", IUFD count, singular "1 Prior Miscarriage") on the list, the profile and the printed report.
16. Release build: no screen-capture block (a screenshot works), no demo patients, PIN setup on first start.
