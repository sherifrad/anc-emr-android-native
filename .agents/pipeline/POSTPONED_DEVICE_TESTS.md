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

