# Delivery plan editor, Mark delivered, schema v7: merged build plan

Date 2026-11-10. Merges `plan_delivery_argon.txt` (Argon), `plan_delivery_alphacode_BUILD_PLAN.md` (Alphacode) and `plan_delivery_glm.txt` (GLM). Every citation below was opened in the working tree today (line numbers already include the uncommitted registration edits, so they differ from some lane citations). Owner decisions: `concepts_brief.md:58-76`; v7 sign-off is already recorded in `AGENTS.md` (SAMD section) and the ledger (`APPROVAL: what=schema-v7`, 2026-11-10). No governance slice is needed.

## 1. Where the three lanes agree

- Storage: new `delivery_plan` table, one row per patient, shaped like `patient_dating` (`RecordEntities.kt:182-193`, FK RESTRICT, unique index). Delivered facts as nullable columns on `patients`. Fees as an integer plus a currency code (Alphacode, GLM; Argon differs, see 2.1).
- Status vocabulary gains `"delivered"`; the repository gates on `SCHEMA_VERSION` exactly (`RecordsRepository.kt:83,100,1681`); backup version bumps 3 to 4 with exact-match rejection of old backups (`EncryptedBackupManager.kt:39,87`).
- Migration copies the `from5To6` shape (`RecordsMigration.kt:345-383`): validate table set, validate `vault_metadata`, DDL, guarded `UPDATE vault_metadata`.
- No 8 s Undo for delivery; `UndoCoordinator` is untouched. Revert is a persistent command.
- New state and reducer code goes in new files because `AncState.kt` is 1871 lines (ceiling 1500, `AGENTS.md`).
- Tests cover migration, backup round trip, freeze, and a rejected old backup. The test infrastructure has no `MigrationTestHelper`; the v6 fixture is hand-built SQLCipher DDL (pattern `EncryptedRecordsTest.kt:179-259`).
- Build order: storage first, then editor, then Mark delivered.

## 2. Disagreements and how each was resolved

| # | Topic | Lanes | Resolution and evidence |
|---|---|---|---|
| 2.1 | Fees type | Alphacode `INTEGER` minor units; GLM `agreed_fees_minor INTEGER`; Argon `TEXT` decimal string | GLM/Alphacode right. The brief says integer minor units plus currency. A text decimal needs parsing on every read and invites float conversion. Only 2-decimal currencies are allowed (see 4.1) so the factor is always 100. |
| 2.2 | Backfill from notes in the migration | Argon backfills with `parseDeliveryNotes`; Alphacode and GLM do not | Argon wrong. `concepts_brief.md` blocker 2 forbids running the parser in the migration (it can misfile clinical text), and non-goals say "no re-parsing old notes". Display precedence is table first, parser fallback per field (`DeliveryPlanningCard.kt:53` reads the parser today). |
| 2.3 | Frozen values stored or derived | Alphacode adds `frozen_ga_weeks`, `frozen_ga_days`, `frozen_edd`, `frozen_risk_level`; GLM and Argon derive from the delivery date | GLM/Argon right. The engine already takes an as-of date (`GestationalAgeEngine.kt:95-103`, `ClinicalModels.kt:125-130`, `DatingText.kt:24`). Stored copies are a second source of truth and go stale if dating is later corrected. Alphacode's idea of putting a status check inside `calculateGestationalAge()` is impossible: `DemoPatientDating` has no status. |
| 2.4 | Delivered columns | Alphacode `delivered_*` (+4 frozen); Argon `delivered_date/place/mode/outcome/at`; GLM `delivery_date/place/mode/outcome`, `delivered_at` | Use GLM names (section 4). Five columns, no frozen copies, no `delivery_reverted_at` (Alphacode): revert clears the facts, the audit trail is the command receipt. |
| 2.5 | Mode and outcome vocabulary | Argon invents `SVD/ElectiveCS/...` and `NeonatalDeath`; GLM reuses real sets | GLM right. `PatientVocabulary.deliveryModes = {CS, Vaginal, Hysterotomy}` and `pregnancyOutcomes = {LiveBirth, Stillbirth, EarlyPregnancyLoss, TerminationOfPregnancy}` (`DemoModels.kt:99-105`); the brief demands one shared vocabulary. Use `PreviousPregnancyFormRules.deliveryModeChoices` (`PreviousPregnancyFormRules.kt:9`) and `hasDeliveryMode(outcome)` (`:13`). Argon's sets do not exist in the code. |
| 2.6 | Is a delivered patient read-only | Alphacode: yes, "like archived" and adds delivered to all 20 write guards; Argon, GLM: no | Alphacode wrong. The brief says a delivered mother still needs postpartum visits (blocker 1). Only dating and the delivery facts lock. |
| 2.7 | Count of literal `"archived"` sites | Brief ~20; Alphacode "20"; GLM 51; Argon 20 rows | GLM right: `grep` today gives 51 (45 Kotlin `"archived"` plus 6 SQL `'archived'`). Section 6 accounts for all 51. |
| 2.8 | Relax `previous_pregnancies.clinician_id` NOT NULL | Alphacode and GLM Q4: yes, table rebuild; Argon: no | Argon right. The built form never stores blank: `PreviousPregnancyFormRules.clinicianIdFor` returns the default id (`:53`), and `RecordAnomalies.kt:36` flags blank ids. A table rebuild in a level-3 migration buys nothing. The v7 migration does not touch `previous_pregnancies`. |
| 2.9 | Old-backup rejection | GLM: exact check at `:87` and UI message UNVERIFIED; Argon: invents `UnsupportedVersion` and `1..CURRENT` range | GLM right on the check, Argon wrong on the code (no `UnsupportedVersion`, no range). Opened `VaultController.kt:525-533`: `inspectBackup` (`EncryptedBackupManager.kt:123-146`) does not check the version and returns `Valid`; then `decryptAndParseBackup` returns null and the user sees `InvalidPassphraseOrCorrupted`, i.e. a wrong "bad passphrase" message. Fix in 5.2. |
| 2.10 | Constant names | Argon `CURRENT_BACKUP_SCHEMA_VERSION`, `RecordsMigration.CURRENT_SCHEMA_VERSION`, `validateSchemaBeforeOpen` | Do not exist (grep). Real: `EncryptedBackupManager.SCHEMA_VERSION` (`:39`), `RecordsRepository.SCHEMA_VERSION` (`:1681`), `RecordsDatabase version = 6` (`RecordsDatabase.kt:26`). Argon's `DashboardQuickFilter` also does not exist; the directory filter is a local `riskFilter` string (`PatientListScreen.kt:173-188`). |
| 2.11 | Adding `"delivered"` to `PatientVocabulary.statuses` | All three add it; none noticed the side effect | New finding. `PatientCommands.kt:168` and `:357` validate `ChangePatientStatusCommand.targetStatus` against that same set, and `RecordsRepository.kt:461` sends any non-"archived" target to `unarchivePatient`. Adding delivered there would let the archive editor target "delivered". Fix: a separate `statusChangeTargets = {"active","archived"}` for those two checks. |
| 2.12 | Archive SQL guard | Argon changes `archivePatient` to active-only; GLM leaves it | Argon right. `RecordsDao.kt:191` is `!= 'archived'`, which lets a delivered row be archived and later unarchived to `active` with delivery columns still set. Change to `(patient_status IS NULL OR patient_status = 'active')`. |
| 2.13 | "Reminders" | GLM: no such feature; others silent | GLM right. Grep for reminder/nextVisit/followUp in `app/src/main` is empty. In this plan "reminders" means the EDD countdown, Due chip, post-term chip and the Incomplete-registration chip. |
| 2.14 | Registration guards | Argon: delivered treated like archived (no registration flow); GLM: left as archive-only | Argon right. `startDatingEditor` will be blocked for delivered, so a registration step on dating would dead-end. `RegistrationFlow.kt:82,175` and `RegistrationFlowDriver.kt:86,125` get one shared predicate. |
| 2.15 | Attachments in the editor | Argon adds a `DELIVERY_PLAN` attachment category and embeds the card; brief says the existing attachments card in the Plan tab needs no change | Brief right (`PlanTab.kt:53` already hosts `AttachmentsSectionCard`). The editor has four text fields; attachments stay where they are. Argon's category addition is dropped. |
| 2.16 | Slices | Alphacode mixes in already-built registration/badge work (S1-S3) and merges backup into later slices | Drop S1-S3 (done: commits `7f16290`, `c350b36`). Keep schema, backup, then commands before any UI, as Argon and GLM do. |
| 2.17 | Alerts after delivery | Argon hides risk, severe BP and all alerts for delivered; GLM freezes progress alerts only and keeps risk and BP visible; Alphacode freezes the risk level | Judgment call, becomes owner question Q4. Default follows GLM (postpartum severe BP is a safety signal). |

## 3. Claims dropped as unconfirmed

- Argon "update `room_master_table` identity hash in EncryptedRecordsTest" and `DEMO-ANC-001` becoming delivered: UNVERIFIED, not needed. Keep fixtures unchanged.
- Alphacode "future date rejected", "before LMP rejected": policy, not code fact; now Q2.
- Alphacode: `BackupVersionTest`, `PatientListScreenTest` file names: new names only, no existing file.
- Argon `PatientHubHeader.kt:120-124`, `OverviewTab.kt:55-68`, `ClinicalAttentionBanner.kt:61-101` line cites: not re-opened. UNVERIFIED; the freeze table in section 7 uses only call sites I grepped.
- Whether `PatientDirectoryCard.kt` badge code needs more than a new `isDelivered` field: UNVERIFIED until slice S-D6 is opened.

## 4. Data model (schema v7)

### 4.1 Decisions

- `patients.patient_status` gains the value `delivered` (`active`, `archived`, `delivered`; null still means active, as in the SQL guards `RecordsDao.kt:139,170,191,221`). Delivered never reuses `archived_at`/`archived_reason`.
- New nullable columns on `patients` (additive `ALTER TABLE ADD COLUMN`, `PatientEntity` at `RecordEntities.kt:15-39`):

| column | type | meaning |
|---|---|---|
| `delivery_date` | TEXT | `yyyy-MM-dd`; the freeze anchor; required when status is delivered |
| `delivery_place` | TEXT | free text, trimmed, blank stored as null |
| `delivery_mode` | TEXT | value of `PatientVocabulary.deliveryModes`; required when `hasDeliveryMode(outcome)` |
| `delivery_outcome` | TEXT | value of `PatientVocabulary.pregnancyOutcomes`; required |
| `delivered_at` | TEXT | ISO instant, audit only |

- New table `delivery_plan` (entity `DeliveryPlanEntity`, mirrors `PatientDatingEntity`):

| column | type | notes |
|---|---|---|
| `patient_id` | TEXT NOT NULL | PRIMARY KEY; FK `patients(id)` ON UPDATE RESTRICT ON DELETE RESTRICT |
| `agreed_hospital` | TEXT | free text, nullable |
| `agreed_fees_minor` | INTEGER | minor units (piastres, cents), `>= 0`; never float |
| `agreed_fees_currency` | TEXT | ISO 4217; required when fees set, null when fees null |
| `delivery_plan_text` | TEXT | nullable |
| `postpartum_note` | TEXT | nullable |
| `revision` | INTEGER NOT NULL | starts at 1 |

  Plus `CREATE UNIQUE INDEX index_delivery_plan_patient_id ON delivery_plan (patient_id)` (pattern `RecordsMigration.kt:130-132`). Allowed currencies: `EGP` (default), `USD`, `EUR`, `SAR`, `AED`, `GBP`, all with 2 decimals, so minor = amount x 100 with integer parsing only (reject more than 2 decimals). `PatientVocabulary.feeCurrencies` holds the set.
- No second parser: display precedence is `delivery_plan` field first, `parseDeliveryNotes(obstetricNotes)` for any field still null (`DeliveryPlanningCard.kt:53`, print `PatientPrintHtmlGenerator.kt:1237`). `obstetric_notes` is never edited by this work.
- Domain: `DemoPatient` (`DemoModels.kt:3-34`) gains `deliveryDate`, `deliveryPlace`, `deliveryMode`, `deliveryOutcome`, `deliveredAt`, `deliveryPlan: DemoDeliveryPlan?`. `PatientWithVisits` (`RecordEntities.kt:374-390`) gains `@Relation deliveryPlan: DeliveryPlanEntity? = null`.
- Unchanged: `previous_pregnancies` (2.8), G/P/A, `drafts` table (new `DraftScreenKind` values need no DDL, payload is TEXT, `DraftRecord.kt:5`).

### 4.2 Migration `RecordsMigration.from6To7(expectedVaultUuid)` (~55 lines)

1. `validateSchemaSixTables(db)`: table set must equal new `SCHEMA_SIX_TABLES = SCHEMA_FIVE_TABLES + "drafts"` (13 tables; `RecordsMigration.kt:557-570` pattern, `SCHEMA_FIVE_TABLES` at `:644`); else `UNKNOWN_SCHEMA`. Also reject if `delivery_plan` exists or `patients` already has any new column (checked via `PRAGMA table_info`).
2. `validateSchemaSixMetadata(db, uuid)`: exactly one row, uuid equals expected, `schema_version == 6`, initialized; else `VAULT_METADATA_MISSING` / `VAULT_METADATA_MISMATCH` (copy of `:571-598` with 6).
3. `CREATE TABLE delivery_plan (...)` and the unique index, DDL character for character equal to what Room generates for `DeliveryPlanEntity` (Room validates at open; a mismatch crashes open).
4. Five `ALTER TABLE patients ADD COLUMN ... TEXT`.
5. `UPDATE vault_metadata SET schema_version = 7 WHERE vault_uuid = ? AND schema_version = 6 AND is_synthetic = 1 AND initialized = 1`; `changed != 1` throws `VAULT_METADATA_MISMATCH`.
6. No data transformation. Existing rows keep `patient_status` as is.

Wiring: `RecordsDatabase.kt:26` version 7, add entity to the list (`:11-25`), register `from6To7` after `from5To6` (`:51-55`); `RecordsRepository.SCHEMA_VERSION = 7` (`:1681`); commit `app/schemas/.../7.json` generated by the KSP export (`app/build.gradle.kts:68-70`); `PRAGMA user_version` follows Room.

### 4.3 Backup version

- `EncryptedBackupManager.SCHEMA_VERSION` 3 to 4 (`:39`). Exact match stays (`:87`). No compatibility reader (owner decision 3, AGENTS "zero legacy").
- Both export paths gain the fields: `PatientWithVisits.toCanonicalJson` (`:241`, add `deliveryPlan` object and the five patient keys) and `DemoPatient.toCanonicalJson` (`:277`); parser `toPatientWithVisits` (`:550`) with the existing `putNullable`/`optNullableString` helpers (`:723-734`).
- `restoreCanonicalBackup` (`RecordsRepository.kt:128-169`): `DELETE FROM delivery_plan` before `DELETE FROM patients` (FK RESTRICT) and insert the plan rows.
- Rejection message: see 5.2.

## 5. Files

### 5.1 New files (budgets are ceilings)

| file | purpose | lines |
|---|---|---|
| `storage/DeliveryCommands.kt` | `SaveDeliveryPlanCommand`, `MarkDeliveredCommand`, `RevertDeliveryCommand` implementing `PatientCommand` (pattern `ChangePatientStatusCommand.prepare`, `PatientCommands.kt:162-184`); canonical fingerprints; `validate()`; fee parsing `parseFeesMinor(text, currency)` | 260 |
| `model/DeliveryRules.kt` | `DemoPatient.pregnancyAsOf(today)`, `isDelivered`, `isPregnancyActive` (not archived, not delivered), fee format/parse, delivery date validation, `PatientVocabulary.feeCurrencies` and `statusChangeTargets` (the vocabulary object stays in `DemoModels.kt`; only the functions live here) | 120 |
| `state/DeliveryFlowState.kt` | `DeliveryPlanDraft`, `MarkDeliveredDraft`, events `StartDeliveryPlanEditor`, `DeliveryPlanDraftChanged`, `SubmitDeliveryPlan`, `StartMarkDelivered`, `MarkDeliveredDraftChanged`, `SubmitMarkDelivered`, `RequestRevertDelivery`, `ConfirmRevertDelivery`, `CancelRevertDelivery` (events may live in any file of the package), reducer functions, draft to command mapping | 300 |
| `ui/DeliveryPlanEditor.kt` | four fields + currency dropdown, test tags `planHospitalField`, `planFeesField`, `planCurrencyDropdown`, `planTextField`, `postpartumField`, `planEditorSave`; `EditorPage` pattern (`PatientEditor.kt:273-308`) | 320 |
| `ui/MarkDeliveredEditor.kt` | date, place (prefilled from plan hospital), mode (hidden unless `hasDeliveryMode`), outcome; revert confirm dialog | 300 |
| `app/schemas/.../7.json` | generated | n/a |
| tests | section 8 | n/a |

### 5.2 Changed files

| file | change | +lines |
|---|---|---|
| `model/DemoModels.kt` (224) | `statuses` + delivered; `statusChangeTargets`; `feeCurrencies`; 6 `DemoPatient` fields; `DemoDeliveryPlan` | +40 |
| `model/DatingText.kt` (61), `Components.kt`, `PatientListScreen.kt`, `PatientHubHeader.kt`, `DeliveryPlanningCard.kt`, `OverviewSections.kt`, `ClinicalSummaryGenerator.kt`, `PatientPrintHtmlGenerator.kt` | pass `pregnancyAsOf` at the call sites in section 7 | +3 to +15 each |
| `storage/RecordEntities.kt` (406) | `DeliveryPlanEntity`, 5 columns on `PatientEntity`, relation | +45 |
| `storage/RecordsDao.kt` (528) | `deliveryPlan`, `insertDeliveryPlan`, `updateDeliveryPlan` (revision guarded), `markPatientDelivered` (guard: status null or active, revision), `revertPatientDelivery` (guard: status delivered, revision); `archivePatient` guard (2.12) | +75 |
| `storage/RecordsRepository.kt` (1695) | execute branches, mapping `toDomain`/`toEntity` (`:682`, `:804`), restore, `writeGuard` unchanged, `saveDating` guard, `changePatientStatus` guard, `SCHEMA_VERSION`. Non-UI file; follow-up split recommended, not in scope | +130 |
| `storage/PatientCommands.kt` (447) | `PatientRecordKind` +3 (`DELIVERY_PLAN_SAVE`, `DELIVERED`, `DELIVERY_REVERTED`), failures `PATIENT_DELIVERED`, `PATIENT_NOT_DELIVERED`; lines 168 and 357 use `statusChangeTargets` | +20 |
| `storage/ClinicalCommands.kt` | `ClinicalCommandFailure.PATIENT_DELIVERED` | +2 |
| `storage/RecordsMigration.kt` (646), `RecordsDatabase.kt` (112) | 4.2 | +80 / +4 |
| `storage/EncryptedBackupManager.kt` (752) | 4.3; `inspectBackup` returns a new `BackupInspectionResult.UnsupportedVersion(found)` when `schemaVersion != SCHEMA_VERSION`, before `Valid`; the restore UI maps it to an explicit message. One new result type, no reader for old data | +70 |
| `storage/DraftRecord.kt`, `state/DraftMapping.kt` (596), `DraftRecovery.kt` | `DELIVERY_PLAN`, `MARK_DELIVERED` screen kinds, slot, codec arm, dirty check (patterns `DraftMapping.kt:20-55,316-502,532-544`) | +60 |
| `storage/RecordAnomalies.kt` (41) | delivered without valid date, invalid mode/outcome, fee without currency | +15 |
| `state/AncState.kt` (1871) | only: 2 `AncRoute` entries, 3 state fields, dispatch arms in `reduceInternal` (`:1065` region) delegating to `DeliveryFlowState.kt`; guard edits in section 6. Net must be 0 or negative lines where possible; no reducer bodies | +20 |
| `state/AncViewModel.kt` (1037) | submit/confirm handlers; `savedNotice` (`:928`) and destination arms (`:920`) for the 3 new commands (the `when`s are exhaustive) | +60 |
| `ui/AncApp.kt` | route two editors, render revert dialog | +30 |
| `ui/patienthub/tabs/PlanTab.kt` (59), `OverviewTab.kt` | pencil emits `StartDeliveryPlanEditor` instead of `StartPatientEditor` (`PlanTab.kt:41-49`; second use of the card at `OverviewTab.kt:114`) | +10 each |
| `ui/patienthub/components/DeliveryPlanningCard.kt` (188) | table first, parser fallback; delivered summary line | +40 |
| `ui/patientdirectory/PatientActionsModalSheet.kt` (264) | Mark delivered / Revert delivery rows | +45 |
| `ui/PatientListScreen.kt` (393), `PatientDirectoryCard.kt` | Delivered chip + count, `isDelivered` in projection, badge | +45 / +25 |
| `ui/patienthub/PatientHubHeader.kt` (528) | frozen GA, Delivered pill | +30 |
| `RegistrationFlow.kt`, `RegistrationFlowDriver.kt` | one predicate | +6 |
| strings | about 30 strings (en, plus ar if present) | |

All UI files stay under 1500 lines (largest after change: `PatientHubHeader.kt` ~560, `PatientListScreen.kt` ~465).

## 6. Every literal "archived" site (51) and its Delivered treatment

Rule: delivered never rewrites an archived check to mean delivered. Delivered gets a sibling check only where behavior must differ. 12 sites change, 39 stay.

| site (file:line) | today | for Delivered |
|---|---|---|
| `DemoModels.kt:97` | `statuses = {active, archived}` | add `delivered`; add `statusChangeTargets` (2.11) |
| `PatientCommands.kt:155` | `recordKind` ARCHIVE vs UNARCHIVE | stays (command is archive/unarchive only) |
| `PatientCommands.kt:168,357` (use `statuses`, no literal) | validates target | change to `statusChangeTargets` |
| `RecordsDao.kt:139,170,221` | edit guards `!= 'archived'` (unlink visit, profile, revision bump) | stay: delivered stays editable |
| `RecordsDao.kt:185,191` | `archivePatient` sets archived, guard `!= 'archived'` | guard becomes `IS NULL OR = 'active'` (2.12) |
| `RecordsDao.kt:210` | `unarchivePatient` only from archived | stays |
| `RecordsRepository.kt:183` | visit save rejected if archived | stays (postpartum visits) |
| `RecordsRepository.kt:461,467` | `changePatientStatus` | add: reject if `patient.status == "delivered"` with `PATIENT_DELIVERED` |
| `RecordsRepository.kt:561` `writeGuard` | archived rejects profile/histories/pregnancy saves | stays |
| `RecordsRepository.kt:884` | attachment save | stays |
| `RecordsRepository.kt:1040` `saveDating` | archived rejects | add delivered rejection `ClinicalCommandFailure.PATIENT_DELIVERED` (dating frozen) |
| `RecordsRepository.kt:1102,1210,1292,1350,1396,1456` | scan, lab, batch, delete/restore, medication | stay |
| `RecordsRepository.kt:1544` | delete/restore guard | stays |
| `AncState.kt:1065` | `reduceInternal` navigation guard | stays |
| `AncState.kt:1290` `startVisit`, `1319` `startPatientEditor`, `1344` `startHistoriesEditor`, `1361` `startPregnancyEditor`, `1425` ultrasound, `1449` lab, `1476` medication | archived blocks | stay |
| `AncState.kt:1388-1389` `startStatusEditor` | only archived/active transitions | add: return unchanged when patient is delivered (archive of a delivered patient is blocked, Q1) |
| `AncState.kt:1406` `startDatingEditor` | archived blocks | add delivered |
| `AncViewModel.kt:573` `startLabBatch` | archived blocks | stays |
| `AncViewModel.kt:928` `savedNotice` | archive vs unarchive | stays; add arms for new commands |
| `RegistrationFlow.kt:82`, `:175` (`isRegistrationIncomplete`); `RegistrationFlowDriver.kt:86,125` | archived exits/never incomplete | change to `!isPregnancyActive(patient)` (2.14) |
| `PatientListScreen.kt:151` | `isArchived` projection | stays; add `isDelivered` beside it |
| `PatientListScreen.kt:190` `activeCount` | `!= "archived"` | `isPregnancyActive` (excludes delivered); also add `deliveredCount` |
| `PatientEditor.kt:283` | status editor title | stays |
| `PatientActionsModalSheet.kt:57,210` (+ uses at 98,164,216-229) | archive/unarchive toggle and hides rows | stays for archived; delivered: hide Archive, show Revert delivery; active: add Mark delivered |
| `PatientHubScreen.kt:138` `canEdit`, `:279` `canDelete` | archived read-only | stay: delivered remains editable |
| `ScansTab.kt:86`, `MedsTab.kt:70,93`, `LabsTab.kt:70`, `VisitsTab.kt:87`, `AttachmentsSectionCard.kt:108` | hide add buttons if archived | stay |

Static check: add `.agents/checks/check_delivered_sites.py` listing the literal `"archived"` count (must stay 51 plus intended edits) and asserting no line contains both `"archived"` and `"delivered"` as an or-condition (guards against reuse).

## 7. Freeze rules

Anchor: `DemoPatient.pregnancyAsOf(today) = parseIsoDateOrNull(deliveryDate) if status == "delivered" else today` (`DeliveryRules.kt`). A delivered patient with a null or malformed date falls back to today and is reported by `RecordAnomalies`.

| surface | rule |
|---|---|
| GA text and chip | every call passes `pregnancyAsOf(today)`: `Components.kt:223-224,331-332` (two of these call `calculateGestationalAgeResult()` with no date today, fix them too), `PatientListScreen.kt:129`, `PatientHubHeader.kt:146,338`, `DeliveryPlanningCard.kt:57`, `ClinicalSummaryGenerator.kt:24`, `PatientPrintHtmlGenerator.kt:70` |
| EDD and discordance | `datingSummaryOf(patient, pregnancyAsOf(today))` at `PatientListScreen.kt:132`, `PatientHubHeader.kt:147`, `DeliveryPlanningCard.kt:54`, `OverviewSections.kt:67`, `ClinicalSummaryGenerator.kt:26`, `PatientPrintHtmlGenerator.kt:72`, `Components.kt:222,335`. EDD date stays displayed as history |
| "Due in N days / overdue" countdown, Due chip | replaced by "Delivered dd/mm/yyyy, outcome" (`DeliveryPlanningCard.kt` countdown, directory EDD chip) |
| late and post-term chip (`PatientListScreen.kt:140`) | `isLateOrPostTerm = false` when delivered |
| discordance banner (`OverviewSections.kt`) | hidden when delivered |
| Incomplete-registration chip and banner | not shown when delivered (2.14) |
| risk badge, severe BP chip, `clinicalAttention` | default: stay visible and keep reading new visits (Q4) |
| dating edits | blocked: UI (`AncState.kt:1406`), repository (`:1040`) |
| revert | clears the five delivery columns and sets status `active`; everything above unfreezes because it derives from the date. `delivery_plan` row and visits untouched |

## 8. Commands and behavior

- `MarkDeliveredCommand(operationUuid, editorSessionId, patientId, expectedPatientRevision, deliveryDate, deliveryPlace?, deliveryMode?, deliveryOutcome, deliveredAt, fingerprint)`. One transaction: `markPatientDelivered` (`UPDATE patients SET patient_status='delivered', delivery_*..., revision=revision+1 WHERE id=? AND revision=? AND (patient_status IS NULL OR patient_status='active')`), then receipt via `commitReceipt` (`RecordsRepository.kt:542-558` pattern). Rejects: missing patient, stale revision, archived or already delivered, date not `yyyy-MM-dd` or in the future, outcome or mode outside the vocabulary, mode missing when `hasDeliveryMode`. G/P/A and `previous_pregnancies` are not touched (brief major item 6).
- `RevertDeliveryCommand(operationUuid, editorSessionId, patientId, expectedPatientRevision, revertedAt, fingerprint)`: `revertPatientDelivery` only from delivered, status back to `active`, columns null, receipt. Triggered by a confirm dialog, not by a snackbar.
- `SaveDeliveryPlanCommand(..., expectedPatientRevision, expectedPlanRevision?, agreedHospital?, agreedFeesMinor?, agreedFeesCurrency?, deliveryPlanText?, postpartumNote?, fingerprint)`: insert when `expectedPlanRevision == null` (conflict if a row exists), else revision-guarded update; bumps the patient revision (pattern `savePreviousPregnancy`, `RecordsRepository.kt:478-540`); rejected only for archived. Texts trimmed, blank to null; fee needs currency; negative or non-integer-minor rejected.
- Draft recovery: both editors register in the draft slot and dirty check.

## 9. Oracles (each with one negative case)

JVM (`src/test`, in the gate):
1. `DeliveryRulesTest` (pure): positive, `pregnancyAsOf` for a delivered patient gives identical GA at two different `today` values and an active patient's GA advances. Negative: delivered with malformed date falls back to today and `detectRecordAnomalies` flags it.
2. `DeliveryCommandsTest`: valid commands prepare and fingerprint; negatives: fee `"12.345"`, negative fee, fee without currency, currency `BTC`, future delivery date, `LiveBirth` without mode, `ChangePatientStatusCommand` with target `delivered` (2.11).
3. `DeliveryFlowStateTest`: pencil in Plan tab and Overview opens the new route not `PatientEditor`. Negative: `StartStatusEditor(archived)` on a delivered patient and `StartDatingEditor` on a delivered patient return state unchanged.
4. `EncryptedBackupTest` / `BackupVaultIntegrationAndZeroDataLossTest` additions: v4 round trips plan and delivered fields on both export paths (`PatientWithVisits` and `DemoPatient`). Negative: a v3 envelope gives `UnsupportedVersion(3)` from `inspectBackup`, restore changes nothing.
5. `RecordsMappingRoundTripTest` addition: entity to domain to entity keeps null plan fields null (not empty). Negative: a plan with only hospital set keeps fee fields null.
6. `SchemaExportV7Test`: parses `7.json`: version 7, `delivery_plan` column set equals 4.1, five new patient columns, `previous_pregnancies.clinician_id` unchanged. Negative: `6.json` has no `delivery_plan`.
7. Update hardcoded 6 and 3: `EncryptedRecordsTest.kt:54,219,224`, `EncryptedBackupTest.kt:31,222,263`, `BackupVaultIntegrationAndZeroDataLossTest.kt:40,51`, `BackupDatingEddRoundTripTest` legacy-edd case (`:101-131`; the legacy edd fallback in `parsePatientObject` is dead once old backups are refused: delete it, rule 3).
8. Static check from section 6.

androidTest (emulator, not in the offline gate; run with `run_emulator_test_class.sh`):
9. `RecordsMigration6To7Test`: encrypted v6 vault with patient, dating, visit, attachment, draft, previous pregnancy (non-blank clinician) migrates; rows kept, `delivery_plan` empty, `metadata.schemaVersion == 7`, `PRAGMA user_version == 7`, Room opens without validation error. Negatives: v6 vault with an extra table rejected `UNKNOWN_SCHEMA` and `user_version` still 6; vault with wrong uuid rejected `VAULT_METADATA_MISMATCH`; chain 1 to 7 also reaches 7.
10. `DeliveredWorkflowDbTest`: mark, revert, mark again, receipts each time, snapshot correct, `ClinicalCommandFailure.PATIENT_DELIVERED` on dating save with no `patient_dating` change. Negatives: same revision replay on a delivered patient rejected; `archivePatient` DAO call on a delivered row affects 0 rows.
11. `DeliveryWorkflowUiTest` (360 dp): edit plan saves four fields; mark delivered moves the patient to the Delivered chip and active count drops; Revert returns it. Negatives: archived patient shows no Mark delivered; delivered patient shows no dating edit.
12. Update `DeliveryPlanTabsEditGateTest` and `DeliveryPlanningCardEditGateTest` for the new event.

## 10. Build slices (each compiles and passes `.agents/scripts/run_build_gate.sh` and `run_static_checks.sh` alone)

| slice | content | level-3 review points |
|---|---|---|
| S-D0 domain | `DeliveryRules.kt`, `DemoModels.kt` fields and vocab (no storage yet, fields default null), `pregnancyAsOf` wired at all call sites (no behavior change while nobody is delivered), `RecordAnomalies`, oracles 1, 5 (domain part). Registration predicate. | clinical-safety review of freeze call sites (code review + test review) |
| S-D1 schema | entities, DAO, `from6To7`, `RecordsDatabase` v7, `SCHEMA_VERSION` 7, mapping, `7.json`, oracles 6, 9, test constant updates (7 for schema) | schema review (frontier) + independent test review of migration and validation sets |
| S-D2 backup | version 4, both export paths, restore, `UnsupportedVersion`, oracle 4, 7 (backup constants) | backup review (frontier) + test review. S-D1 and S-D2 land back to back; no write path exists until S-D3, so no data can be lost in between |
| S-D3 commands and guards | `DeliveryCommands.kt`, repository branches, DAO writes, status guard edits of section 6, view-model exhaustive arms, drafts, oracles 2, 3, 10 | status state machine review (frontier) + test review. This is the slice that must not miss any of the 51 sites |
| S-D4 plan editor | `DeliveryPlanEditor.kt`, route, pencil rewiring, card table-first display, oracle 12, UI test part of 11 | fees (money) handling review: integer parsing, currency |
| S-D5 mark delivered | `MarkDeliveredEditor.kt`, action sheet rows, revert dialog, header Delivered pill | none beyond S-D3 (no new storage) |
| S-D6 directory and surfaces | Delivered chip, count, card badge, freeze surfaces in section 7, rest of oracle 11 | clinical-safety review at 360 dp |
| S-D7 print | `PatientPrintHtmlGenerator.kt` reads table first, delivered section | level 2 |

Order is strict. If S-D3 is judged too big, split commands (storage) from guards (state) but both before any UI.

## 11. Risks

- A missed `archived` site in S-D3 leaves a delivered patient archivable or dating-editable. Mitigation: section 6 table plus the static check and oracles 3 and 10.
- Room open validation: hand-written DDL must equal the entity (index name, FK actions). Mitigation: oracle 9 opens through Room; keep DDL generated from `7.json`.
- Two sources of truth (table vs notes): precedence rule, no backfill; print switches only in S-D7, so print can disagree with the Plan tab until then.
- Backup dual export paths: `DemoPatient` path (`EncryptedBackupManager.kt:277`) is easy to forget; oracle 4 covers both.
- `RecordsRepository.kt` reaches about 1825 lines; the 1500 rule is UI-only (UNVERIFIED whether the owner wants it applied to storage). Schedule a split after S-D7.
- Revert clears delivery facts; history survives only as receipt fingerprints (Q6).
- Hardcoded version assertions in existing tests will fail the gate until updated in the same slice (listed in oracle 7).
- Older builds cannot open a v7 vault (fail-closed); accepted by the owner (no old users).

## 12. Owner questions (max 6)

1. Can an archived patient be marked delivered, and can a delivered patient be archived? Recommended: no to both. The three states are mutually exclusive; to change, revert delivery or unarchive first.
2. Delivery date rules? Recommended: reject future dates; allow a date before the LMP (GA then shows "-" because the engine returns null before day 0, `GestationalAgeEngine.kt:103`); do not require a visit on that date.
3. Hospital (plan and delivery place): free text or a list? Recommended: free text, with the place prefilled from the plan hospital.
4. "Alerts frozen as of delivery": do the risk badge and severe-BP chip keep following new postpartum visits? Recommended: yes. Only pregnancy-progress items freeze (GA, EDD countdown, due, post-term, discordance, incomplete registration); high risk and severe BP stay visible because postpartum pre-eclampsia is a safety signal.
5. After delivery, may the clinician still add visits, labs, medications, scans, attachments, edit the profile, the plan and the postpartum note? Recommended: yes to all; only dating and the delivery facts (date, place, mode, outcome) lock, and the facts change only through revert and re-mark.
6. Should Revert delivery erase the entered delivery facts? Recommended: yes, clear the five columns and keep the receipt as the audit trail; the plan row and all visits stay. (Currency list EGP, USD, EUR, SAR, AED, GBP, 2 decimals, is set as an implementation default; tell me if EGP only is wanted.)
