# Device test plan (phase D5) - 2026-10-30
Devices: Honor 400 (DNY-NX9, Android 16, real phone) and emulator Pixel 7 API 36; API 26 emulator for the Android 8 crash (#10).
App under test: QA copy com.sherifradwan.anc.qa (debug build, side by side). The clinic app com.sherifradwan.anc is never touched.
Data: demo patients only. No real patient appears in any screenshot.
Evidence: screenshots, uiautomator dumps, gfxinfo, logcat in .agents/pipeline/device/ (local only).

## F - Functional
F1 Cold start -> vault setup / unlock -> patient list. No crash, no endless "Blocked" retry (#4) in logcat.
F2 Open a patient profile -> workspace -> delivery card. Notes split into hospital/fees/plan/postpartum as the parser tests say.
F3 Every pencil edit button opens the right editor (delivery plan, clinical alerts, history, medication, previous pregnancy correction).
F4 Print report renders; age "n yrs"; Rh and risk badges match the screen.
F5 Lock / background / relaunch behaviour (records what really happens; #7 in the audit).
F6 Android 8 (API 26): open Settings and the lock screen; record a crash (#10) if any.

## G - Graphic / UI
G1 Blood-group tile and list badge: "O- (Rh negative)" fits without truncation at font scale 1.0 and 1.3.
G2 Touch targets: every clickable node >= 48 x 48 dp (uiautomator bounds / density).
G3 TalkBack labels: every icon button has a content description (uiautomator content-desc); no emoji read aloud.
G4 Dark theme: all screens readable, cards distinct, status bar icons visible (light + dark).
G5 Layout: no clipped text, no overlap, consistent spacing and alignment on 6.5" phone width.

## T - Transitions and performance
T1 Navigation list -> profile -> workspace -> back: smooth, no flicker. gfxinfo janky frames < 10%.
T2 Dialog open/close (edit sheets): animation present, no layout jump.
T3 Scrolling long lists/profile: gfxinfo 90th percentile frame time < 16 ms target (60 Hz) / note the phone's 120 Hz.

## Output
Each item PASS / FAIL / NOT RUN with evidence; failures become spec items for the builder lanes (tests first).
