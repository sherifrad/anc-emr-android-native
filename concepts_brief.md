# Delivery plan editor, delivered state, and shorter patient entry

* **Author:** sherifrad
* **Date:** 2026-11-10

## Overview

The clinician reported five problems on the QA build. The delivery plan pencil opens the whole patient form. The risk badge sits on the right of the patient card. There is no way to record that a patient has delivered. The previous-pregnancy form asks for the same facts twice. Patient registration asks for more than the clinician types at the first visit.

This brief proposes five changes. Each uses the existing editors where one exists. The owner approved a schema v7 with a tested migration for the new data.

## Analysis

Seven review lenses read the code. Findings below are worst first. Claims come from the reviewers' reads of the files named, and the orchestrator has not re-verified each one.

**Blockers**

1. **"Delivered" is not a status today.** The allowed statuses are active and archived only. Anything else is rejected, and the string "archived" is checked in about 20 places, where it means read-only. A delivered mother still needs postpartum visits, so Delivered must not reuse the archive path. The directory has no Delivered filter; its filter chips are risk levels kept in local UI state. After delivery the app must also decide what the gestational age card, EDD banners, reminders and high-risk alerts show, because a delivered patient still reading "41w" is a safety defect.
2. **The delivery plan text lives in free text today.** The card and the print report read hospital, fees, plan and postpartum from the patient's obstetric notes through a heuristic parser. New columns create a second source of truth. Do not fill the new fields by running the parser in the migration, because it can misfile clinical text. Leave the old notes untouched, show the new fields first, and fall back to the parser for old rows. Recommended storage is a separate delivery_plan table with one row per patient, so the patient fingerprint stays unchanged. One reviewer argued for no schema change at all (labelled lines inside the notes); the owner has chosen dedicated storage.
3. **The schema bump touches fail-closed version gates.** The repository refuses to open a vault unless the schema version is exactly 6. A v7 vault cannot be opened by a v6 build, and the project has no migration test helper. A hand-written migration needs tests for kept rows, a wrong vault, an unknown table set, and a match with the exported 7.json. AGENTS.md says v6 is frozen, so the owner's v7 sign-off must be written there in the same change. This is a level-3 area and needs a frontier review plus an independent test review.
4. **Backup and restore break silently or hard.** The encrypted backup has its own version (3) with an exact-match check. Bumping it orphans every old backup. Not bumping it drops the new table on export. The owner must choose: accept old backups on read (a small compatibility rule, against the "no legacy shims" rule), or discard them.
5. **Mark delivered and its Undo do not fit the existing machinery.** The undo coordinator holds one pending undo for 8 seconds and works per record. Delivered is several changes at once: a status, possibly a history record, possibly the G/P/A counts. They must commit in one transaction, with one revert. Reviewers recommend a persistent "revert delivery" action instead of an 8-second snackbar, because a mis-tap can hide a patient from the active list.

**Major**

6. **Auto-creating a previous-pregnancy record at delivery is risky.** G/P/A counts are separate columns that a history save does not update, so counts and history can disagree. A pregnancy that just ended is not "previous" until a new one starts, so print and history lists would show it as old. Manual entry would duplicate it. Recommendation: store the delivery fields on the delivered record now, and offer a one-tap "add to history" later.
7. **The previous-pregnancy form dedupe is under-specified.** It has three outcome inputs (free text, a structured choice, a loss or termination method) and two mode lists with different words (delivery mode and birth method). The clinician ID cannot simply become optional: the database column is required and the validation demands it. Options are to fill it from the logged-in clinician or to keep it required. "Place and doctor required" would invalidate old rows, so apply it to new saves only. Hide the duplicate inputs and keep the columns. Readers of old rows must keep working.
8. **Registration is one page, not a sequence.** The patient form is a single page with demographics, G/P/A and risk. Dating, histories and previous pregnancies are separate screens opened only for an existing patient. The smallest correct design is a thin step tracker that saves the profile first and then opens each existing editor, with "skip, finish later" at each step and an "incomplete registration" marker. Age without a date of birth goes stale, so store when the age was entered.
9. **The risk badge change is small.** The card already shows severe BP, then high risk, then medium risk. The work is moving the badge to the leading edge of the name (left in English, right in Arabic) and removing the low-risk badge call. A patient with severe BP and high risk currently shows only the BP chip, which needs a rule. Test the longest name with two chips at 360dp.
10. **File size.** AncState.kt is 1869 lines, over the 1500 limit. New events and reducers go in new files, not in it. The new Plan editor needs a full wiring checklist: route, event, draft, command, repository, view model, draft recovery.

**Reuse that is already there:** the attachments card already sits in the Plan tab and needs no change. Use the existing delivery section box, the existing mode and outcome validation, and a single mode and outcome vocabulary shared by the pregnancy form and Mark delivered.

## Goals vs. Non-Goals

* **In-Scope Goals:**
  - A small Plan editor: agreed hospital, agreed fees, delivery plan, postpartum note, attachments.
  - Risk badge on the leading edge of the name, high and medium only, severe BP kept.
  - Mark delivered: date, place, mode, outcome, with a revert, and a Delivered filter.
  - One outcome and one mode in the previous-pregnancy form.
  - Short registration: name, address, phone, age, then dating, histories and previous pregnancies through the existing editors.
  - Schema v7 with a tested migration and backup coverage.
* **Out-of-Scope Non-Goals:**
  - Date of birth.
  - Any field beyond the four plan fields in the Plan editor.
  - Re-parsing old notes into the new table.
  - Dropping columns from existing tables.

## Alternatives Weighed

- Labelled lines inside the existing notes text, no schema change. Rejected by the owner: dedicated storage to avoid crossing section boundaries. Reviewer risk noted: the heuristic parser could round-trip text wrongly.
- Reuse the archived status for Delivered. Rejected by review: archived means read-only, and 20 places check the literal string.
- Auto-create the history record when marking delivered. Deferred: counts can disagree and the pregnancy is not yet "previous".
- Register Mark delivered in the per-record deletable registry. Rejected by review: it is a composite, not one record.

## Key Decisions & Staging for Next Steps

Decisions made by the owner on 2026-11-10:
1. After delivery the patient is not active. They count as delivered, and gestational age, EDD, reminders and high-risk alerts are frozen as of delivery.
2. Delivered has its own state and a persistent "revert delivery" action (no 8-second Undo).
3. Old backups are dropped. The backup version moves on with the schema and nothing is kept for old backups (the owner has already restored them).
4. Clinician ID is optional in the previous-pregnancy form. The database column is currently required, so v7 must relax it, or the form must store an explicit empty value; this is a build question to settle in the spec.
5. Fees are a number with a currency.
6. Severe BP plus high risk on one card: high risk is the dominant badge.
7. Registration uses a thin step tracker that opens the existing dating, histories and previous-pregnancy editors in order.
Still open: whether an archived patient can be marked delivered, whether the delivery date may be in the future or before the LMP, and whether hospital is free text or a list.

Build order (each step gated against the current build gate and static checks):
1. Governance: record the v7 sign-off in AGENTS.md.
2. Risk badge move.
3. Previous-pregnancy form dedupe.
4. Registration step tracker.
5. v7 migration, delivery_plan table, backup policy, round-trip tests.
6. Plan editor.
7. Mark delivered with revert, Delivered filter, post-delivery screen behaviour (separate level-3 review).
8. Print and PDF output.
