# UI/UX backlog - saved for the next problem-solving round (owner, 2026-10-31)
Rule: UI/UX issues found while testing are written here and NOT fixed inside unrelated rounds.
Status: open unless noted. Evidence = Honor 400 (QA copy) unless noted.

## New (seen 2026-10-30/31 on the phone)
Fixed in round 3j (2026-10-31, verified on the phone): N-1 GA apply button layout, N-2 readable attachment title, N-3 "1 photo" plural (3 places + 1 hard-coded string), N-5 emoji removed from Retake/Gallery buttons.
| ID | Where | Issue |
|---|---|---|
| N-1 | Ultrasound editor, calculated-GA card | "Apply calculated GA (42w 0d (Post-term))" button is squeezed into a narrow column, one word per line; the card becomes very tall. |
| N-2 | Attachments tab, attachment card | With no title typed the card title is the raw id ("att_63ec9ede-...") instead of a readable name (category + date). |
| N-3 | Scan card, workspace | "1 photos" - needs a plural resource (same as DV-10, still present). |
| N-4 | Add Attachment dialog | The preset chips wrap with "Detailed - scans" alone on a second row; the dialog is long (type chips + sizes + title + preview): consider a more compact layout. |
| N-5 | Add Attachment / camera | "📷 Retake" and "🖼️ Gallery" use emoji in button text (read aloud by TalkBack, inconsistent with the rest). |
| N-6 | In-app camera | No flash, zoom, or alignment guide; the still preview has no zoom to check sharpness; capture button has only a content description. |
| N-7 | Ultrasound editor | The numeric keyboard stays open after `Done`; scroll does not work until it is hidden; consider hiding on scroll / imePadding. |
| N-9 | Ultrasound editor | "GA weeks (0-45)" label wraps to two lines while "GA days (0-6)" does not, so the two boxes have different heights; use shorter labels or equal heights. |
| N-8 | Viewer | Dark or tiny photos give no hint (no file size/dimensions line); add the size and preset used. |

Fixed in round 3k (2026-10-31, verified on the phone): DV-3 delivery tile labels, DV-5 prior-pregnancy summary, DV-8 chip gaps on Arabic cards, DV-13 high-risk card inner frame, DV-15 header subtitle, and a new "74.5 kg kg" duplicate unit (one weight rule, formatWeightKg).
Still checked present/open: DV-7 (action icons shift between Arabic and English cards), DV-9 profile header crowding, DV-11 font 1.3 filter wrap, DV-12 print preview duplicate titles + pink Print PDF, DV-14 workspace opens scrolled, N-4, N-6, N-7, N-8, N-9. DV-4 (enum names) no longer reproduces.

## Older open items (DEVICE_TEST_REPORT.md) - re-check on the phone after 3c-3g
DV-3 clipped delivery tile labels; DV-4 internal enum names printed in prior-pregnancy cards; DV-5 prior-pregnancy summary runs together; DV-7/DV-8 mirrored Arabic-name cards and chip gaps;
DV-9 profile header crowding; DV-11 font 1.3 filter wrap; DV-12 print preview duplicate titles and pink Print PDF button; DV-13 light-theme high-risk card inner frame;
DV-14 workspace opens scrolled / repeated tab name; DV-15 truncated header subtitle. DV-6 touch targets: patient-card icons fixed in 3e (48 dp verified on the phone); the photo chip (40 dp) was not re-checked.
Done: DV-1 (lock, 3c), DV-2 (Android 8-10 settings crash, 3c).

## How to run the round
One spec section per cluster (e.g. "layouts and wrapping", "labels and plurals", "print preview"), UI lane only, tests = static checks plus ThemeContrast-style unit tests where possible, then a phone pass with screenshots at font 1.0 and 1.3, light and dark.

Fixed in round 3p (2026-10-31, NOT yet seen on a device): DV-12 duplicate section titles in the printed report and the cut-off dialog title "Clinical Medic...". The pink "Print PDF" button is NOT a contrast failure (white on #934F5B is about 6.0:1, passes AA); no change made.
Still open and needing the phone to reproduce: DV-7, DV-9, DV-11, DV-14, N-4, N-6, N-7, N-8, N-9.

Seen on the phone 2026-10-31 (print preview, QA copy):
| P-1 | Printed report tables | **FIXED in `c7f1031` (2026-10-31), verified 2026-11-05.** CSS no longer allows `break-word`; `word-break: normal` on tables, `white-space: nowrap` for short values. Measured in headless Chrome on the dumped sample (`app/build/print-sample/report.html`): every date / BP / number / count cell renders on exactly one line at both 794 px (A4 content) and 380 px (phone). Only the notes column uses `overflow-wrap: anywhere`, deliberately, for unbroken free text. Originally: "Gestati onal Age", "Pressu re", "Weigh t", "120/8 0", "2026- / 10-31". |
| P-2 | Printed report, attestation box | **FIXED in `c7f1031` (2026-10-31), verified 2026-11-05.** The generated HTML contains "Official Stamp & Signature" exactly once (grep count = 1); `check_r3x.py` S97 enforces the single occurrence. |
Round 3p verified on the phone: duplicate section titles gone, dialog title fully visible (DV-12 done).

