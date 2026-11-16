# Device test report (phase D5) - 2026-10-30
Devices: Honor 400 (DNY-NX9, Android 16, 1264x2736, 560 dpi, dark theme, font 1.0) with the side-by-side QA copy com.sherifradwan.anc.qa;
emulator Pixel 7 API 36 (light theme, font 1.3); emulator API 26 (Android 8). Demo patients only. The clinic app on the phone was not touched,
and no phone system settings were changed. Evidence: .agents/pipeline/device/ (screenshots, uiautomator dumps).
Build: working tree after round 2f (247/247 unit tests).

## Result per plan item
| Item | Result | Evidence |
|---|---|---|
| F1 cold start | PASS (no crash, no retry loop) but opens straight to patient data, no vault setup/PIN | phone_01 |
| F2 delivery card | PASS: Rh text, risk, hospital "not agreed yet", fees, plan shown in the right boxes | phone_11 |
| F3 pencils | PARTIAL: pencils present with labels; editors not opened one by one (next pass) | dumps |
| F4 print | PASS for values (35 yrs, Rh, High Risk) - layout issues below | phone_12 |
| F5 lock | FAIL: no lock after background or cold start; data visible at once (audit #7) | logs |
| F6 Android 8 | FAIL: Security Settings crashes (NoClassDefFoundError BiometricManager, BiometricAuthManager.kt:12) (audit #10) | emu26 logcat |
| G1 "(Rh negative)" fits | PASS on list, profile, delivery tile at font 1.0 and 1.3 | phone_03/04/11, emu_03 |
| G2 touch targets | FAIL: patient-card Workspace/Profile/Edit icons 36x48 dp (every card); photo chip 60x40 dp | ui_audit |
| G3 TalkBack labels | PASS for icon buttons on list/workspace (labels present) | ui_audit |
| G4 dark/light | PASS mostly; light theme high-risk card shows a grey inner frame (card in card) | emu_01 |
| G5 layout | FAIL: several items below | |
| T1-T3 transitions | PASS: list->profile 7% janky, 90th 11 ms, 99th 200 ms (one visible hitch on first open); workspace 2% janky, 99th 48 ms | gfxinfo |

## Findings (new from the device)
Clinical / data
- DV-1 (High) No app lock at all on cold start or return from background (audit #7 confirmed on device).
- DV-2 (High) Android 8-10 crash opening Security Settings (audit #10 confirmed).
- DV-3 (Should) Delivery tiles clip labels silently: "Current gestational age" shows "CURRENT", "Agreed clinical EDD" shows
  "AGREED CLINICAL" (KeyFactTile maxLines = 1 with no ellipsis, ClinicalBadges.kt:133). A bare date without "EDD" is ambiguous.
- DV-4 (Should) Prior-pregnancy cards print internal enum names: "LiveBirth", "EarlyPregnancyLoss"; developer labels "Entered date text",
  "Entered outcome text", "Clinician ID".
- DV-5 (Should) Prior-pregnancy summary in the profile: "2019-04-12 · CS" runs into the outcome ("CSLive female infant") and the outcome wraps
  in a ragged narrow column; every outcome, including "Live ... healthy", is in alarm red.
UI / UX
- DV-6 Touch targets: patient-card action icons 36 dp wide (need 48), photo chip 40 dp high.
- DV-7 Cards with an Arabic name mirror the whole card (chips right, actions left); actions move between patients. Keep name alignment
  script-adaptive but keep chips/actions in one fixed place.
- DV-8 Chips touch with no gap on mirrored cards ("37+0 wks" against "A+").
- DV-9 Profile header: HIGH RISK badge crowds the centred name; AGE badge centred while chips are left-aligned.
- DV-10 "1 photos" (needs a plural resource).
- DV-11 Font 1.3: "Normal Risk (2)" filter wraps to two lines and clips.
- DV-12 Print preview: every section title shown twice ("Pregnancy Dating" + "PREGNANCY DATING"); pink "Print PDF" button with white
  text has low contrast; title "Clinical Medic..." truncated.
- DV-13 Light theme high-risk card: grey inner frame (card inside card).
- DV-14 Workspace opens scrolled so the first tab is cut ("isode Overview"); the tab name "Episode Overview" repeats as the card title.
- DV-15 Header subtitle truncated "Maternal-Fetal Medicine & Obst..." on a 6.5" phone.
Performance: no blocker. First profile open has one ~200 ms frame (likely image/list first composition) - re-measure after audit item
"image decoding on main thread".

## Not run yet
TalkBack spoken order (needs TalkBack on; is a system setting on the phone - emulator next pass), each editor dialog's open/close
animation, camera/photo flows, backup/restore, print to PDF file.
