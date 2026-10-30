# UI/UX backlog - saved for the next problem-solving round (owner, 2026-10-31)
Rule: UI/UX issues found while testing are written here and NOT fixed inside unrelated rounds.
Status: open unless noted. Evidence = Honor 400 (QA copy) unless noted.

## New (seen 2026-10-30/31 on the phone)
| ID | Where | Issue |
|---|---|---|
| N-1 | Ultrasound editor, calculated-GA card | "Apply calculated GA (42w 0d (Post-term))" button is squeezed into a narrow column, one word per line; the card becomes very tall. |
| N-2 | Attachments tab, attachment card | With no title typed the card title is the raw id ("att_63ec9ede-...") instead of a readable name (category + date). |
| N-3 | Scan card, workspace | "1 photos" - needs a plural resource (same as DV-10, still present). |
| N-4 | Add Attachment dialog | The preset chips wrap with "Detailed - scans" alone on a second row; the dialog is long (type chips + sizes + title + preview): consider a more compact layout. |
| N-5 | Add Attachment / camera | "📷 Retake" and "🖼️ Gallery" use emoji in button text (read aloud by TalkBack, inconsistent with the rest). |
| N-6 | In-app camera | No flash, zoom, or alignment guide; the still preview has no zoom to check sharpness; capture button has only a content description. |
| N-7 | Ultrasound editor | The numeric keyboard stays open after `Done`; scroll does not work until it is hidden; consider hiding on scroll / imePadding. |
| N-8 | Viewer | Dark or tiny photos give no hint (no file size/dimensions line); add the size and preset used. |

## Older open items (DEVICE_TEST_REPORT.md) - re-check on the phone after 3c-3g
DV-3 clipped delivery tile labels; DV-4 internal enum names printed in prior-pregnancy cards; DV-5 prior-pregnancy summary runs together; DV-7/DV-8 mirrored Arabic-name cards and chip gaps;
DV-9 profile header crowding; DV-11 font 1.3 filter wrap; DV-12 print preview duplicate titles and pink Print PDF button; DV-13 light-theme high-risk card inner frame;
DV-14 workspace opens scrolled / repeated tab name; DV-15 truncated header subtitle. DV-6 touch targets: patient-card icons fixed in 3e (48 dp verified on the phone); the photo chip (40 dp) was not re-checked.
Done: DV-1 (lock, 3c), DV-2 (Android 8-10 settings crash, 3c).

## How to run the round
One spec section per cluster (e.g. "layouts and wrapping", "labels and plurals", "print preview"), UI lane only, tests = static checks plus ThemeContrast-style unit tests where possible, then a phone pass with screenshots at font 1.0 and 1.3, light and dark.
