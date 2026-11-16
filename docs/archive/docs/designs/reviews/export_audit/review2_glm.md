1. **VERDICT: FAIL**

2. **Findings**

**F1 — SERIOUS — `keepNewest` (ReportSections.kt:37–41), `reportLabs` (:49–53), and the share-text sorts (PatientShareText.kt:48, 63, 82): dates are sorted as raw strings, so blank, non-ISO, and future dates break "newest" selection and the wrong visits/scans/labs are printed or hidden.**

Sorting `dateOf(it) ?: ""` lexically only works for uniform, well-formed ISO `yyyy-MM-dd` strings. Three concrete failures:

- **Blank date treated as oldest and hidden first.** Patient Amina, limit = 5, with 6 visits: five dated `2025-01-10` … `2025-05-12` plus the most recent visit (yesterday's, where the tablet was offline) with `date = null`. The null-date visit sorts as `""` → oldest → `takeLast(5)` drops it. Printed report: five old visits + "1 earlier visit hidden", while the clinically most important (latest) visit is silently gone. Same pattern in `reportLabs` (`?: ""` in a descending sort pushes a null-dated lab to the bottom, into the hidden set) and in `reportScans`.
- **Non-ISO date mis-ordered against ISO dates.** Patient Fatima, labs, limit = 5, six results: five ISO dates in 2026 and the most recent one stored as text `12/03/2026`. Lexically `"12/03/2026" < "2026-01-01"` (because `'1' < '2'`), so in a descending sort the *newest* result lands at the bottom and is hidden by `take(5)`; the printed "newest first" list omits the latest Hb result and hides it in the count. Conversely a lab stored as `9/10/2025` (`'9' > '2'`) sorts above all 2026 ISO dates and prints as if it were the newest result — the printed "newest first" ordering is factually wrong.
- **Future-dated visit steals a keep slot.** Patient Maryam, 6 visits, five real ones `2025-01` … `2025-06` plus one mistyped as `2026-12-25` (or a wrongly keyed 2099 date). `takeLast(5)` keeps the future-dated visit and hides the two genuinely most recent real visits (May and June 2025) as "2 earlier visits hidden". The patient's printed record drops her latest actual care because of one typo, and the hidden-count line is misleading about *which* visits were left out.

Because `patientShareText` re-sorts with the same string comparators, the plain-text summary shows the same wrong ordering (e.g., `9/10/2025` above `2026-03-01` under "newest first").

**F2 — MODERATE — `reportStatusLine` (ReportSections.kt:81): archived status matched case-sensitively, so an archived patient's status line can vanish.**

`patient.status == "archived"` only matches the exact lowercase string. A patient record imported or stored as `"Archived"` or `"ARCHIVED"` (the prompt's own hint: "active" vs "Active" vs "Archived") fails the comparison. Concrete: Halima, `status = "Archived"`, not delivered → `archived` is null, `deliveryOutcomeLine()` is null → `reportStatusLine` returns null → no status line at all. The hospital receiving the printout has no way to know the record is archived, and — since nothing else marks it — it reads as a live antenatal record. (Note the asymmetry: delivered-ness is a boolean, archive-ness is a fragile exact-string test.)

**F3 — MODERATE — `medicationDates` (ReportSections.kt:141–146): bypasses `reportDate`, so a date `formatDmy` cannot parse prints the literal text "null".**

`reportDate` (line 90) exists precisely to guard against `formatDmy` returning null, which proves null is an expected outcome for some stored values. `medicationDates` interpolates `formatDmy(start)` / `formatDmy(end)` directly. Concrete: a stopped medication with `startDate = "2026-13-45"` (invalid ISO) or `"02/2026"`, `endDate = "2025-05-01"` → printed line: **"null to 01/05/2025"**. That is invented garbage text on a clinical document, and it also breaks the intended rule that a non-ISO date prints *as stored* (nowhere in this path is the stored text substituted).

**F4 — MODERATE — `labResult` (ReportSections.kt:148–158): a missing result is not "Not recorded" and not omitted; the line prints the unit alone, or an empty value.**

- `result = null`, `unit = "g/dL"` → output `"g/dL"`. Printed line: **"Hemoglobin: g/dL (Ref: 12–16) [01/05/2026]"**. A reader can misparse the unit as the result; the intended rule ("missing value prints 'Not recorded' or the line is left out") is violated.
- `result = null`, `unit = null` → output `""` → the share-text line renders **"• Hemoglobin:  [01/05/2026]"** with nothing after the colon. The result is silently dropped rather than flagged.

**F5 — MODERATE (conditional on model code) — `patientShareText` (PatientShareText.kt:37): the delivered-patient GA guard is bypassed for the dating summary.**

`reportGestationalAge` correctly returns null for `patient.delivered`, but line 37 then calls `datingSummaryOf(patient, asOf)` unguarded and prints every `datingTextRows(summary)` row. If `datingSummaryOf` computes a GA at `asOf` (its taking `asOf: LocalDate` strongly suggests it does), a delivered patient — e.g., Salma, delivered 20/09/2026, share text generated today — gets a current GA such as "Gestational age today: 45w+" printed in the PREGNANCY DATING section, violating the rule "a delivered patient never gets a current gestational age" and printing a physiologically impossible value. Needs the `patient.delivered` guard here too (or confirmation that `datingTextRows` never emits a current GA).

**F6 — MINOR — `scanWeight` (ReportSections.kt:124–127): a unit is invented for any bare number.**

`estimatedFetalWeight = "3.5"` → `no letters → "3.5 g"`. If the field is ever stored in kg (the letter-check's existence implies mixed conventions), the printout says the estimated fetal weight is **3.5 g** instead of 3.5 kg — a 1000× error on a clinical document. Nothing in this file constrains the stored unit, so appending `" g"` is invention, not formatting.

3. **Checked and found correct (or not assessable here)**

- **Hidden-count arithmetic** (ReportSections.kt:40, :52): `ordered.size - limit` / `ordered.size - limit` is exact; `takeLast`/`take` pick the correct end of the ordered list. The count itself is right; only the ordering that feeds it is wrong (F1).
- **Visit/scan "oldest first" among kept rows** for pure ISO data: `sortedBy` + `takeLast` yields oldest-first, as intended; `sortedBy` is stable, so equal dates keep their stored order (ties are safe).
- **Medication split** (:55–63): `partition` covers every record; nothing is dropped; both lists are printed (stopped section included whenever non-empty). `MEDICATION_LABEL_ACTIVE` is the app's own status rule including the non-compliance flag. The `frequency` field is never referenced anywhere in either file — correctly not printed.
- **Delivered-patient guard in `reportGestationalAge`** (:85–88): returns null when `delivered`, and the null result omits the line in both consumers.
- **Risk handling** (:65–78): tiers map to the three labels; blank/whitespace risk → "Risk not recorded"; a tier-less non-blank text (e.g., "High risk – anemia") is printed as written, never mislabeled "not recorded". Matches the rule exactly.
- **`reportDate`** (:90): null/unparseable → "Not recorded" per rule (subject to F3's note that `medicationDates` doesn't use it).
- **Null/blank text handling**: `visitText`, `scanNotes`, `scanGestationalAge`, `labExtra`, `deliveryPlanRows` all use trim + `isNullOrBlank` guards and fall back to "Not recorded" or omit the line — no invented placeholders.
- **Fees**: never referenced; `deliveryPlanRows` builds only plan/hospital/postpartum rows.
- **Escaping / `#` visit numbers**: neither file emits HTML or renders the `#` column — both live in the HTML composition layer, which was not provided. I could therefore neither confirm nor refute "escaped exactly once" or the true visit numbering; those checks are outside the supplied code. No double- or zero-escaping is possible *within* these two files (they produce plain strings and plain text only).
