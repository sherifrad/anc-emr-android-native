# Last full gate and push (2026-11-18, print v2)
- Gate (`push_gate.sh pre`) run by a Flash lane on the headless emulator: build pass, unit tests 1707 pass, lint failed once with 2 `LocalContext.current` errors in `PatientTwoPageReportDialog.kt` (fixed by switching to `LocalResources.current`, then 0 errors, 88 warnings), androidTest compile pass, emulator suite 975 of 977.
- The 2 emulator failures passed when their classes were rerun alone (2 runs each): `DeliveryPlanSheetTest` 17 of 17 (known order-dependent flake: `aRejectedSaveKeepsTheSheetTheValuesAndShowsTheMessage`) and `ScansTabRedesignTest` 21 of 21 (`measurements_threePerRow_wordsTwoPerRow`).
- Protected files changed since the last push: only `PatientPrintHtmlGenerator.kt` and its test (owner approved). The script lists more files when `STAGE_BASE` is left at its old default; use `STAGE_BASE=<last pushed commit>`.
- Push: `1154d6b..86c6259` (plain push, 21 commits) by a Flash lane on the owner's explicit word; `push_gate.sh post` 3 PASS. QA copy `com.sherifradwan.anc.qa` 1.0.5-qa installed on the phone at 192.168.1.4:43109; the clinic app was untouched.
- Not verified: a real print and PDF share on a device, a second full gate after the lint fix.
- Earlier phone smoke test (18 pass, read only): `docs/qa/prepush/REPORT.md`.
