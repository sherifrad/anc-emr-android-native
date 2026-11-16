## 1. VERDICT

**FAIL — but not on the evidence you probably expect.** The excerpt is truncated: it opens mid-function (line 1 is a bare `}`) and cuts off at line 204 inside `watermarkImg`. Every function that actually implements your intended rules — `reportVisits`, `reportScans`, `reportLabs`, `reportMedications`, `reportStatusLine`, `reportRisk`, `reportGestationalAge`, `formatDmy`/`reportDate`, `escapeHtml`, `hiddenRowsHtml`, `medicationDetail`, `scanGestationalAge` — is **absent** from the text supplied. So I can certify the row-building layer only. I will not invent findings about code I cannot see; the checks you asked for (mixed-format sorting, blank dates, ties, future dates, `hidden` arithmetic, delivered-GA suppression, `active`/`Active` status split) live entirely in the missing functions and are therefore **unaudited**.

What *is* visible contains one rule violation and two latent wrong-output paths, below.

## 2. Findings

**F1 — moderate (serious if the field is user-typed) — line 88, `scanGestationalAge(s)`; same class of issue at line 62, `${vGa}`**
Unexcaped clinical value interpolated into HTML. These are the *only* two dynamic clinical strings in the whole excerpt that bypass `escapeHtml`; every other cell (BP, weight, notes, presentation, placental site, fluid, test name, result, range, med name, detail, dates, outcome, place/doctor, previous-pregnancy fields) is escaped.
Concrete input: a scan whose stored GA text is `<12 weeks` (or a sonographer typing `31+2 & 3d`). Wrong output: everything from `<` onward is parsed as a tag and **silently disappears from the printed row** — the patient's report shows a blank/GA-less scan. `&` renders as a broken or spurious entity.
At line 62 `vGa` comes from `calculateGestationalAgeResult(...).formatted`, so it is probably app-generated and benign; line 88 is only benign if `scanGestationalAge` returns purely computed text. Either way it contradicts "escaped exactly once" (here: zero times). Fix: wrap both in `escapeHtml`, and make `scanGestationalAge` return a plain string.

**F2 — minor — line 171, `else -> pp.outcomeText ?: pp.outcome ?: "-"`**
Raw internal outcome code printed to a patient-facing report. The `else` branch is reachable by construction (the `when` enumerates only four labels), so any outcome stored under another key leaks verbatim.
Concrete input: previous pregnancy with `outcome = "MolarPregnancy"` (or `"EctopicPregnancy"`) and blank `outcomeText`. Wrong output: the "Outcome & Status" cell prints `MolarPregnancy` on a document handed to the patient and the hospital. Secondary effect at lines 173-175: such a loss is not in `isLoss`, so it is printed in the teal "good outcome" style instead of the red badge.

**F3 — minor — line 184, `escapeHtml(pp.dateText ?: "-")`**
Previous-pregnancy date is printed verbatim, which is right for free text (`"2019"`, `"April 2019"`) but wrong when the stored text is already ISO, which your own rule says must be rendered `dd/MM/yyyy`.
Concrete input: previous pregnancy with `dateText = "2019-04-12"`. Wrong output: the Year/Date column prints `2019-04-12`, while every other date on the same page prints `12/04/2019`. Otherwise consistent for non-ISO text.

**F4 — minor/conditional — line 147, `val stoppedMedsRowsHtml: String = medRows(meds.stopped)`**
No placeholder is emitted for an empty stopped list, unlike `current` (line 144). If the outer template prints a "Stopped medications" heading unconditionally (not visible in the excerpt), a patient on only current medication gets an empty section titled "Stopped medications", which reads as "this patient has stopped drugs" with nothing under it. Verify in the section-2 HTML.

**F5 — minor/consistency — lines 29-31, 63, 64, 124**
Missing values print a bare `-` (gravida/parity/abortion, BP, weight, reference range), while the scans table and the dating matrix use `"Not recorded"`. Your rule sanctions only "Not recorded" or omitting the line. Concrete input: visit with `bloodPressure = null`. Wrong output: `-` in the BP column — not inventing data, but it is a third convention the rule does not allow and it differs from the scan table on the same page.

**F6 — risk, not a proven defect — line 60, `${idx + 1 + visits.hidden}`**
The arithmetic is self-consistent *given* the contract "kept rows are the newest N, printed oldest-first, `hidden` = number of earlier ones dropped": 12 visits, limit 10 → first printed row is `#3`, last `#12`, plus "2 earlier visits hidden". **But the formula silently breaks if `reportVisits` ever drops a row for any reason other than the print limit** — e.g. an unparseable date filtered out, or duplicates collapsed. Then `#` and `hidden` no longer describe the true chronological position. This needs a unit test, not a code change, and I cannot assert it because `reportVisits` was not supplied.

**F7 — risk in the same missing function (sorting) — cannot be verified, must be re-supplied**
Your three specific traps all live in `reportVisits`/`reportScans`:
- *Blank or non-ISO date*: under a plain text sort, `""` sorts first (treated as the oldest visit, then dropped as "hidden") and a typo like `"2026-13-45"` sorts last, i.e. **kept as the newest visit** with GA `-`, pushing the real latest visit into "1 earlier visit hidden". If that is how it behaves, it is a serious finding: the printed report's last row is a garbage date and a real visit is missing.
- *Ties*: Kotlin's `sortedBy` is stable, so same-day visits keep repository order; if the repository returns newest-first, two visits on 2026-05-04 (morning 110/70, evening 160/110) print in reverse chronological order under an "oldest first" table, and nothing flags the severe one as the later reading.
- *Future date*: an unclamped `asOf` future date is treated as newest, is kept, and shows a GA far beyond the current one, while a genuine recent visit is counted as hidden.

## 3. Checked and found correct (visible code only)

- Column counts match every `colspan`: visits 6 (line 47 = line 59-66), scans 7 (line 76 = line 86-97), labs 4 (line 107 = line 120-125), meds 2 (line 145 = line 137-141).
- Hidden-row **position** is right per direction: visits, `hidden` note emitted *before* the rows (line 69-70, oldest first); labs, *after* the rows (line 128-129, newest first).
- `#` arithmetic consistent with the stated contract (F6 caveat aside): `hidden=2`, 10 kept → `3…12`.
- Escaped exactly once, no double-escape anywhere visible: clinic name (10), dating label/value (20-21), discordance (24), visit date/BP/weight/notes (61-65), scan date/presentation/placental/fluid/weight/notes (87-94), lab date/test/result/extra/range (110-124), med name/detail (136-139), past-delivery summary (151), previous-pregnancy date/mode/outcome/place (177-187). `logoBase64` (198, 204) is correctly *not* escaped — base64 in a data URI, not patient text.
- `labResult` escaped **before** the `(Abnormal)` wrapper (110-115), so the bold/red markup is not escaped into visible tags — the correct order.
- BP alert precedence (39-44, 53-57): `SEVERE` tested before `RAISED`, so a severe reading never gets the amber-only style; both classes exist when severe is present; alert divs are appended after the escaped BP and are constants, not patient text.
- Empty states for visits/scans/labs, and the "No current medications recorded" placeholder with `colspan=2`; `String.ifEmpty` on `medRows` result is valid Kotlin.
- Blank ultrasound fields fall back to `"Not recorded"` (89-91) rather than printing blank or fabricating a value.
- Gravida/parity/abortion summary and the dating matrix take `?: "Not recorded"`/`"-"` rather than inventing numbers; fees appear nowhere in the excerpt.

## To finish this audit, re-supply

`reportVisits`, `reportScans`, `reportLabs`, `reportMedications` (+ `medicationDetail`), `reportStatusLine`, `reportRisk`, `reportGestationalAge`, `formatDmy`, `reportDate`, `hiddenRowsHtml`, `escapeHtml`, `scanGestationalAge`, `labResult`/`labExtra`, `BP_ALERT_*`, `MAX_PRINT_*`, and the section-2 template that consumes `stoppedMedsRowsHtml` and `currentGa`. Until then only F1-F5 are proven, and the four checks you flagged as most dangerous (mixed/blank-date sorting, ties, future dates, delivered-patient GA) remain unverified — which is itself why this cannot return PASS.
