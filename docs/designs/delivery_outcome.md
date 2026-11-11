# Delivery outcome: delivered yes/no and the delivery date (real database fields)

Status: APPROVED by the owner 2026-11-12 (schema change v6 to v7 and edits to the protected files named in section 4). Section 9 holds the owner's decisions and overrides sections 2 and 8 where they differ.
Author: orchestrated by Claude from an alphacode draft (`reviews/delivery_outcome/design_alphacode.md`), corrected by Claude against the real code. Independent model review: NOT done (opencode and agy quotas are out; agy resets in about 65 hours). It is owed before merge.

## 1. Decision and scope
The owner asked (2026-11-12) for a dedicated real column or place in the database, planned first. Schema v6 is frozen by default; this plan is the proposal to unfreeze it once, for this feature: **v6 to v7, new nullable columns on `patients`.**

Why columns on `patients`, not a new table: the app keeps one patient record per case; earlier pregnancies live in `previous_pregnancies`. A one-to-one outcome table adds a table, a foreign key, DAO, backup and delete rules for no gain, and the existing patient `revision` check, command receipt and draft path already cover a patient column. If the app later needs several pregnancies per patient, that is a separate redesign.

## 2. Columns (patients table)
| Column | Type | Meaning |
|---|---|---|
| `delivered` | INTEGER NOT NULL DEFAULT 0 | 0 = not delivered or not recorded, 1 = delivered |
| `delivery_date` | TEXT NULL | ISO `YYYY-MM-DD`; may be null when delivered is 1 (date unknown, e.g. delivered elsewhere) |

OWNER DECISION (section 8, Q1): add `delivery_mode` and `delivery_outcome` now, or only the two above. Anything beyond those (baby sex, weight, place, notes) is left out: the owner asked for delivered and date only.

Rules (enforced in the save command and repository, not by SQL CHECK):
- `delivered = 0` means `delivery_date` is null.
- `delivery_date` when present: valid calendar date, not after today plus 1 day, not before the dating start. The dating start lives in the `patient_dating` table, not in `patients`, so a SQL CHECK across tables is impossible; the check is code. If no dating exists, only the "not in the future" rule applies.
- Marking "not delivered" clears the date (and mode and outcome if added) in the same save.
Corrections to the alphacode draft: it put a CHECK against `dating_start` on `patients` (that column does not exist there; the SQL would fail), added eight columns the owner did not ask for, used tri-state NULL for delivered (a NOT NULL DEFAULT 0 flag is simpler and cannot be ambiguous), and used SQL CHECKs inside ALTER TABLE ADD COLUMN, which Room does not mirror in its exported schema and would diverge from a fresh install.

## 3. Migration 6 to 7 (`RecordsMigration.from6To7`)
Same pattern as `from5To6`: validate the v6 tables and metadata, `ALTER TABLE patients ADD COLUMN delivered INTEGER NOT NULL DEFAULT 0`, `ADD COLUMN delivery_date TEXT`, then `UPDATE vault_metadata SET schema_version = 7 WHERE vault_uuid = ? AND schema_version = 6 ...`; if the update changes other than 1 row, throw `RecordsMigrationFailure`. Room runs it in one transaction. Existing rows become not delivered. No down-migration: rollback is restore from backup. `RecordsDatabase` version = 7, add the migration to the list; `app/schemas/.../7.json` is generated and committed. Need a `validateSchemaSixTables/Metadata` pair mirroring the five versions (table list taken from the real v6 schema, not invented).

## 4. Touch points that really exist (the `clinicalAttention` footprint)
- `storage/RecordEntities.kt` (PatientEntity): add the columns. PROTECTED.
- `storage/RecordsMigration.kt`, `storage/RecordsDatabase.kt`: migration, version. PROTECTED.
- `storage/RecordsDao.kt` (the profile UPDATE at about line 135, named parameters): bind the new columns. PROTECTED.
- `storage/RecordsRepository.kt` (three mapping sites near lines 421, 644, 700, 827): write and read the columns, validate. PROTECTED.
- `storage/PatientCommands.kt` (`SavePatientProfileCommand` fields, `prepare`, and the idempotency fingerprint near line 355 that hashes the profile fields: the new fields MUST be in it or a retry of a changed save could be treated as a duplicate).
- `storage/EncryptedBackupManager.kt`: backup JSON is written field by field (lines about 265, 299, 575), NOT automatic. Add write and read with a missing-field default (so v6 backups still restore). Bump nothing else unless the backup format version demands it; check.
- `model/DemoModels.kt` (the Patient model), `model/DemoFixtures.kt`: fields and demo patients (one delivered, one not).
- `state/AncState.kt` (PatientDraft values, and my `DeliveryPlanFields`), `state/DraftMapping.kt` (draft recovery map, line about 106 and 369), `state/ClinicalDraftReducers.kt` (events for the two fields).
- UI: `ui/patienthub/components/DeliveryPlanSheet.kt` (a Delivered switch and a date field; Save disabled until touched), `DeliveryPlanningCard.kt` (show Delivered with the date and days since), `PatientHubHeader.kt` (the "44+0 wks" badge after the EDD; show Delivered instead), `strings.xml`.
- Print and summary: `ui/report/PatientPrintHtmlGenerator.kt` and the summary generator. PROTECTED. Include the delivery outcome.
- Tests: migration test from a real v6 vault (the existing `EncryptedRecordsTest` pattern), repository round trip, command fingerprint test, backup restore test (v6 backup and v7 backup), draft recovery, reducer tests, `DeliveryPlanSheetTest` additions, a 360dp card test.

## 5. One save writes everything
The sheet already saves through `SubmitPatientProfile` and `SavePatientProfileCommand`. That command gains the two (or four) fields, so notes and outcome are written in one transaction with one revision bump. No new command, no new route.

## 6. Clinical safety
- Default is not delivered; nothing sets delivered automatically.
- Undo path: the sheet switch can be turned off and saved; that clears the date. The existing Undo bar applies to the save.
- A wrong date is caught by the validation above and shown as a field error in the sheet; the save is blocked.
- The weeks badge: while delivered it must not show "44+0 wks". It shows "Delivered" with the date. OWNER DECISION Q3 (recommended: yes).
- 360dp: the card shows one line "Delivered 05 Nov 2026 (6 days ago)"; no chip grid.

## 7. Work packages (each: lane builds, a different family reviews)
1. Data layer: entity, migration, DAO, repository, command, fingerprint, backup, schema 7.json. Done when the migration test from a real v6 vault, the repository round trip and the backup test pass. PROTECTED: needs the owner's named sign-off and a frontier review plus an independent test review.
2. State: draft fields, reducers, draft recovery. Done when the reducer and draft tests pass.
3. UI: sheet switch and date field, card line, header badge. Done when sheet and card tests pass on the emulator at 360dp.
4. Print and summary. PROTECTED.
5. Device QA on the phone.
Order: 1, 2, 3, 4, 5.

## 8. Owner questions (recommendation first)
1. Fields: only delivered and date (recommended), or also mode of delivery and outcome (live birth, stillbirth, and so on)?
2. Sign-off: approve the schema change v6 to v7 and edits to these protected files by name: `RecordEntities.kt`, `RecordsMigration.kt`, `RecordsDatabase.kt`, `RecordsDao.kt`, `RecordsRepository.kt`, and the print and summary generators. Yes?
3. After delivery, replace the weeks badge in the header with "Delivered" and the date (recommended)?
4. When marked delivered, keep the patient active (recommended; you archive when you choose)?
5. Unknown delivery date allowed when delivered (recommended: yes, shown as "date not recorded")?

## 9. Owner decisions (2026-11-12), final
1. Four columns: `delivered` INTEGER NOT NULL DEFAULT 0, `delivery_date` TEXT NULL, `delivery_mode` TEXT NULL, `delivery_outcome` TEXT NULL. Mode values: `vaginal`, `caesarean`, `assisted_vaginal`, `other`. Outcome values: `live_birth`, `stillbirth`, `neonatal_death`. Mode and outcome may be null while delivered (not recorded yet). All four are cleared when delivered is turned off. Values live in one model file (`model/DeliveryOutcome.kt`) used by validation, the sheet and the card (one canonical set).
2. Header: while delivered, the weeks badge is replaced by "Delivered" and the date.
3. Delivered patients are not active and leave the main dashboard. The patient status stays `active` or `archived` (no change to that flow); delivered is derived from the new flag. The main list and its count hide delivered patients; a "Delivered" filter chip shows them with their own count; a search query still finds delivered patients so nobody is lost. The active count excludes delivered patients.
4. Unknown date: shown as "Delivered, date ???" (the delivered flag alone is enough to save). The sheet allows an empty date when delivered is on.
5. Everything else as sections 1 to 7.
