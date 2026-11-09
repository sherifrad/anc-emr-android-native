# Overview Dating section redesign

* **Author:** sherifrad
* **Date:** 2026-11-09

## Overview

The Dating section on the patient Overview tab becomes a short, fixed list of rows for clinicians on a 360dp phone screen. It shows what the pregnancy is dated from, the due date, how it was dated, and a warning when the LMP and the earliest scan disagree beyond the allowed gap. It changes no stored data and no schema.

Rows, in order:

1. `LMP dd/mm/yyyy`, or `ET dd/mm/yyyy · D5` (embryo transfer date plus embryo age in days, one line) for IVF. If the embryo age is not recorded, show `ET dd/mm/yyyy` with no "· D5". IVF patients show no LMP. A patient dated by ultrasound or clinically who has an LMP on file still shows the LMP. The row is hidden when there is neither.
2. `EDD`. The label is `Confirmed EDD` only when LMP, ET and ultrasound EDD are all blank and an agreed EDD exists (for example a patient referred in with only a due date). Otherwise `EDD`.
3. Dating method, in the owner's wording, for example "by ultrasound". Notes the owner writes stay in the notes.
4. An inline discordance alert, not a high-risk flag, with no button. LMP-dated patients only (not IVF).
5. Notes last.

## Analysis

Decided with the owner after the Goldfish review (seven lenses):

* **Alert rule.** It compares the LMP due date with the earliest scan's due date. It measures at the earliest scan's date, using the existing tiered limits in `model/Redating.kt` (`redatingAdvice`). It shows while the gap is beyond the limit and clears once the patient is re-dated: when the method is Ultrasound, or the agreed EDD equals the scan EDD.
* **Red `??` marker.** Where the current gestational age is shown in the patient header, the patient list and the gestational banner, the number stays and a red `??` follows it, for example `32w 1d ??`. Historical GA at visits, scans and labs is unchanged.
* **Print and share.** The printed form and the share text show both due dates and the gap, for example `LMP EDD dd/mm/yyyy · US EDD dd/mm/yyyy · gap 9 d`. The IVF label shows ET, not LMP.
* **One shared rule.** A pure function in `model/` decides the EDD to use, the EDD label, the row-1 content and the discordance flag. The GA result carries the flag. The Overview section, the header, the patient list, the banner, print and share all use it. This replaces four separate EDD fallbacks (`Components.kt:231`, `PatientListScreen.kt:127`, `GestationalBanner.kt:42`, `OverviewSections.kt:83`). All gestational-age numbers already come from `GestationalAgeEngine`.
* **Early scan** means `ultrasoundEdd` is not blank. Row 1 follows `datingMethod`: IVF_TRANSFER shows ET, every other method shows the LMP if present.
* The reviewers raised that "Confirmed EDD" only fires on the blank-fields rule, because the editor and intake fill `agreedEdd` automatically. The label is therefore decided on LMP, ET and ultrasound EDD being blank. The owner accepted keeping the `Confirmed EDD` label.

## Goals vs. Non-Goals

* **In-Scope Goals:**
  * The five rows above with numeric dd/mm/yyyy dates and abbreviations.
  * One shared rule function used by the screen, header, list, banner, print and share.
  * The discordance alert reusing `redatingAdvice` and the existing tiered limits.
  * Tests for the row matrix, the EDD label rule, tier boundaries, and the 360dp, 1.3x layout.
* **Out-of-Scope Non-Goals:**
  * Any schema change or new stored field.
  * A resolve or dismiss button or any persisted acknowledgement.
  * A second flat discordance threshold.
  * New dating-method editor chips or new stored method values.
  * A second EDD row for the ultrasound date on the Overview.
  * The alert for IVF patients.
  * The red `??` on historical GA at visits, scans and labs.

## Alternatives Weighed

* Resolve check mark stored in a new `patient_dating` column. Rejected by author: needs a frozen-schema change.
* Resolve state kept in local preferences. Rejected by author: not in the encrypted backup and lost on a new phone.
* Resolve state written into the dating notes as text. Rejected by author: a clerk can edit it away and parsing it is fragile text scraping.
* A flat discordance threshold of 10 to 14 days. Rejected by author: the existing tiered rule in `model/Redating.kt` is already used by the ultrasound editor.
* Measuring the alert at today's date. Rejected by author: the allowed gap changes with gestational age, so the alert would appear and vanish as weeks pass.
* Replacing the GA number with `??`. Rejected by author: a clinician needs the number.
* A second EDD row showing the ultrasound date. Rejected by author: replaced by the alert, and print shows both dates.

## Key Decisions & Staging for Next Steps

Still to confirm in the mock, with these defaults:

* Exact method-row words for the four stored methods (LMP, Ultrasound, IVF_TRANSFER, Clinical). Default: `LMP`, `by ultrasound`, `ET`, `clinical`.
* dd/mm/yyyy applies to the Dating section, print, share, and the header and list EDD surfaces touched by the shared rule.
* Alert wording. Default: reuse the `redating_hint` numbers (gap in days, limit) in a short Overview variant.

Build order after the mock is approved: date formatter and tests, the pure rule function and JVM test matrix, rewrite `DatingSection`, the shared GA text component with `??`, move the four screens to the shared rule, update strings, update the Overview androidTest, then print and share. Edits under `app/src` are level 3 and need a frontier review and an independent test review. The mock `.agents/mocks/overview_tab_sections.html` is updated and approved by the owner before any code.
