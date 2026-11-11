# Medication Section Revision

* **Author:** sherifrad
* **Date:** 2026-11-10

## Overview

Tapping a medication tile today opens the full-screen medication editor, saves, and returns to the workspace. The Meds tab is rebuilt each time, so scroll position, search text and open cards are lost. That is the "refresh on every click". The quick-add list also shows every catalog drug as a full-width card, which pushes the patient's actual prescriptions off a 360dp screen.

The revision makes the Meds tab a review-first list for one doctor entering 2-4 drugs per visit on a phone. Most follow-up visits only review existing drugs. A mobile-first clickable HTML mock comes before any code.

## Analysis

Seven reviewers (Gemini 3.8 Flash through `agy`, read-only; no Claude credits) checked the idea against the code. I verified the main claims against the code myself. Their raw reports are in the session scratchpad.

**What holds up**
- The refresh cause is confirmed. `startMedicationEditor` (`AncState.kt:1470`) switches the route and `AncApp.kt:238` swaps the workspace for the editor. Six of seven reviewers independently found the same cause.
- The medication catalog is stored as JSON in SharedPreferences, not Room, so favourites and dose/frequency variants need no database migration. They do need versioned parsing so existing saved catalogs are not wiped.

**Objections that change the plan**
1. **Hidden dates (6 of 7 reviewers).** If the dates are hidden and auto-stamped with today's date, you cannot record a drug the patient started before the visit or stopped earlier. In pregnancy, timing against gestational age matters. You accepted this earlier; the reviewers' fix keeps your speed: stamp today, show it as a small read-only chip, and allow a one-tap change (today / -3 days / earlier). This is a decision for you below.
2. **"Temporary" and "changed on" have nowhere to live.** I checked `MedicationEntity`. It has no temporary flag and no changed-date column. Room v6 is frozen. Options that stay inside the schema: a temporary drug gets an automatic end date from a short course length (for example 3 / 5 / 7 days) held in the existing `end_date`. "Changed" would use the existing `revision` plus a note, or be dropped. Putting flags into the free-text `notes` field was rejected by every reviewer who raised it.
3. **"Active until stopped" leaves old short courses open.** With no end date, a cold medicine would stay active forever. A course length for temporary drugs fixes this.
4. **Overwriting the dose erases history.** Changing iron from once to twice daily should not look like it was always twice daily. Cheapest fix inside the schema: a dose or frequency change stops the old row and creates a new one, which also gives the "changed" mark for free. The cost is one extra row per change.
5. **Saving rules.** `SaveMedicationCommand` requires the current patient and medication revision. Two quick taps in a row (stop one drug, then edit another) can be rejected if the second tap uses an outdated revision. Quick actions must wait for the previous save to finish, or use one queue.
6. **No second editor.** The project rule is one canonical implementation. Adding inline editing while keeping the full-screen editor creates two. The editor's form should be extracted and reused in both places, or the full-screen route retired for routine use.
7. **Scope.** One reviewer argued most of the pain comes from two small problems: the route swap and the long list. A one-step "add this catalog drug" action plus a collapsed picker would remove both without a settings screen. The variants, favourites and settings screen are the larger part of the work. They match what you asked for, but they could ship after the first fix.

## Goals vs. Non-Goals

* **In-Scope Goals:**
  - Adding, stopping and editing a drug never leaves the Meds tab; scroll and search state survive.
  - Review-first list: active drugs on top, stop symbol per row, "changed" mark, "show past" toggle for stopped drugs.
  - Collapsed picker showing favourites only, with search for everything else.
  - Several dose/frequency variants per drug with one default; a different route is a separate drug.
  - Frequency from a short fixed list plus custom entry.
  - Medication settings: manage favourites, variants, default variant.
  - Enter moves to the next field and keeps that field visible above the keyboard (medications first, then app-wide).
  - Clickable 360dp mobile-first HTML mock.
* **Out-of-Scope Non-Goals:** Room schema change, drug-interaction checks, prescription printing, desktop port, new "temporary" database column.

## Alternatives Weighed

- **Edit the dose in place on the same row.** Rejected: it erases when the dose changed. Stop-and-recreate keeps history.
- **Store the "temporary" flag in the `notes` text.** Rejected by the reviewers: fragile parsing and weak audit trail.
- **Add a Room column for temporary/changed.** Rejected: schema v6 is frozen without clinical sign-off.
- **Keep the full-screen editor for every action.** Rejected: it is the source of the refresh.
- **Fix only the route swap and the long list, no settings screen.** Not rejected. Proposed as a first stage in Key Decisions.

## Key Decisions & Staging for Next Steps

Decided with the author (2026-11-10):
1. **Dates:** auto-stamped today, shown as a small chip; one tap picks another day (today / 3 days ago / earlier).
2. **No "temporary" mark.** Instead, any drug can have an optional course length (3 / 5 / 7 days or custom) that sets the existing end date. When the course ends the drug moves to past automatically. Drugs without a course length stay active until stopped.
3. **Dose or frequency change:** stop the old row and create a new one. The new row carries the "changed" mark and a changed-on hint.
4. **Stop symbol** stops a drug. "Show past" toggle lists stopped drugs. A different route is a new drug, not a variant.
5. **Quick actions run one at a time:** each save finishes before the next starts, so the revision check does not reject rapid taps.
6. **One medication form:** extract the editor form and reuse it inline. Do not build a second editor.
7. **Enter moves to the next field and keeps it visible above the keyboard.**
8. **Staging:** Stage 1 now = fix the refresh (add, stop, edit without leaving the Meds tab), collapsed picker, show-past toggle, date chip, course length, Enter-to-next. Stage 2 later = favourites, dose/frequency variants, default variant and the medication settings screen. The Stage 1 mock should leave room for variants.

Risks to handle in the draft:
- Level-3 paths (`app/src`, clinical safety): the implementation needs a frontier-tier review and an independent test review.
- "Changed" and course-end behaviour must work with the frozen v6 schema (existing `end_date`, `status`, `revision`).
- Test coverage on a 360dp screen for the date chip, keyboard visibility and rapid taps.

## Update after the Goldfish review (2026-11-10)

The review reports are in `docs/designs/reviews/medication_section_revision/`. The author accepted the reviewers' advice to make Stage 1 smaller. Course length, the "Changed" mark and note line, the two-row replace command, undo for stop and change, the "3 days ago" shortcut and a shared Enter helper moved to Stage 2. A dose edit now updates the row in place; change history waits for a schema change with clinical sign-off. Decisions 2, 3 and 6 above are superseded by `docs/designs/medication_section_revision.md`.
