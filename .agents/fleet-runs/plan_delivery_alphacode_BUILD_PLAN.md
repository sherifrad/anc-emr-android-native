# Build Plan: Delivery Plan Editor, Delivered State, Schema v7

**Scope**: A) Plan-tab "Delivery plan" editor, B) "Mark delivered" with revert + Delivered filter, C) Schema v7 (delivery_plan table, delivered state, tested migration, backup v4, exported 7.json + round-trip tests)

---

## 1. Owner Decisions (from concepts_brief.md:58-66)

1. After delivery: patient is **not active**; GA, EDD, reminders, high-risk alerts **frozen** as of delivery date.
2. Delivered has its own state + **persistent "revert delivery"** (no 8s Undo).
3. Old backups **dropped**; backup version moves with schema.
4. Clinician ID optional in previous-pregnancy form — DB column currently required → v7 must relax it or form stores explicit empty.
5. Fees = number + currency.
6. Severe BP + High Risk on one card → High Risk is dominant badge.
7. Registration: thin step tracker opening existing editors.

Still open (owner must decide, max 6 questions at end).

---

## 2. Data Model (Schema v7)

### New table: `delivery_plan`
| Column | Type | Nullable | Notes |
|---|---|---|---|
| patient_id | TEXT | NO | PK, FK → patients(id) |
| agreed_hospital | TEXT | YES | |
| agreed_fees_amount | INTEGER | YES | Minor units (cents/paisa) |
| agreed_fees_currency | TEXT | YES | ISO 4217, e.g. "EGP" |
| delivery_plan_text | TEXT | YES | |
| postpartum_note | TEXT | YES | |
| created_at | TEXT | NO | ISO 8601 |
| updated_at | TEXT | NO | ISO 8601 |
| revision | INTEGER | NO | Default 1 |

### PatientEntity changes (v7)
- `patient_status` values: `"active"`, `"archived"`, `"delivered"` (NEW)
- New columns:
  - `delivered_at` TEXT (delivery date, ISO 8601)
  - `delivered_place` TEXT
  - `delivered_mode` TEXT (from PatientVocabulary.deliveryModes)
  - `delivered_outcome` TEXT (from PatientVocabulary.pregnancyOutcomes)
  - `delivery_reverted_at` TEXT (NULL unless reverted)
  - `frozen_ga_weeks` INTEGER (GA at delivery, frozen)
  - `frozen_ga_days` INTEGER
  - `frozen_edd` TEXT (EDD at delivery, frozen)
  - `frozen_risk_level` TEXT (risk at delivery, frozen)

### PreviousPregnancyEntity change (v7)
- `clinician_id` TEXT → nullable (was NOT NULL)

### Backup version bump
- EncryptedBackupManager.SCHEMA_VERSION: `3` → `4`
- Payload adds `deliveryPlan` array + `frozen_*` patient fields

---

## 3. Migration v6 → v7 (RecordsMigration.kt)

```kotlin
// In RecordsMigration object, add:
fun from6To7(expectedVaultUuid: String): Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        validateSchemaSixTables(db)
        validateSchemaSixMetadata(db, expectedVaultUuid)

        // 1. delivery_plan table
        db.execSQL("""
            CREATE TABLE delivery_plan (
                patient_id TEXT NOT NULL,
                agreed_hospital TEXT,
                agreed_fees_amount INTEGER,
                agreed_fees_currency TEXT,
                delivery_plan_text TEXT,
                postpartum_note TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                revision INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(patient_id),
                FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX index_delivery_plan_patient_id ON delivery_plan (patient_id)")

        // 2. patients: add delivered columns + frozen columns
        db.execSQL("ALTER TABLE patients ADD COLUMN delivered_at TEXT")
        db.execSQL("ALTER TABLE patients ADD COLUMN delivered_place TEXT")
        db.execSQL("ALTER TABLE patients ADD COLUMN delivered_mode TEXT")
        db.execSQL("ALTER TABLE patients ADD COLUMN delivered_outcome TEXT")
        db.execSQL("ALTER TABLE patients ADD COLUMN delivery_reverted_at TEXT")
        db.execSQL("ALTER TABLE patients ADD COLUMN frozen_ga_weeks INTEGER")
        db.execSQL("ALTER TABLE patients ADD COLUMN frozen_ga_days INTEGER")
        db.execSQL("ALTER TABLE patients ADD COLUMN frozen_edd TEXT")
        db.execSQL("ALTER TABLE patients ADD COLUMN frozen_risk_level TEXT")

        // 3. previous_pregnancies: relax clinician_id NOT NULL
        db.execSQL("CREATE TABLE previous_pregnancies_v7 (...)") // copy with clinician_id nullable
        db.execSQL("INSERT INTO previous_pregnancies_v7 SELECT * FROM previous_pregnancies")
        db.execSQL("DROP TABLE previous_pregnancies")
        db.execSQL("ALTER TABLE previous_pregnancies_v7 RENAME TO previous_pregnancies")
        db.execSQL("CREATE INDEX index_previous_pregnancies_patient_id ON previous_pregnancies (patient_id)")
        db.execSQL("CREATE INDEX index_previous_pregnancies_patient_id_updated_at ON previous_pregnancies (patient_id, updated_at)")

        // 4. bump vault_metadata
        val changed = db.compileStatement("""
            UPDATE vault_metadata
            SET schema_version = 7
            WHERE vault_uuid = ? AND schema_version = 6 AND is_synthetic = 1 AND initialized = 1
        """.trimIndent()).use { s -> s.bindString(1, expectedVaultUuid); s.executeUpdateDelete() }
        if (changed != 1) throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
    }
}
```

### Validation sets for v6 (add to RecordsMigration)
- `SCHEMA_SIX_TABLES` = SCHEMA_FIVE_TABLES + "drafts"
- `validateSchemaSixTables`, `validateSchemaSixMetadata`

---

## 4. Exact "archived" Checks (20 locations) — Delivered Must NOT Reuse

| File:Line | Current Check | Action for Delivered |
|---|---|---|
| AncState.kt:1065 | `patient.status == "archived"` | Add `|| patient.status == "delivered"` where writes blocked |
| AncState.kt:1290 | `patient.status == "archived"` | Ditto |
| AncState.kt:1319 | `patient.status == "archived"` | Ditto |
| AncState.kt:1344 | `it.status != "archived"` | Change to `it.status !in setOf("archived", "delivered")` for write paths; **but** Delivered patients still readable |
| AncState.kt:1361 | `it.status != "archived"` | Same |
| AncState.kt:1388-1389 | status comparisons | Add delivered branch |
| AncState.kt:1406 | `it.status != "archived"` | Same |
| AncState.kt:1425 | `it.status != "archived"` | Same |
| AncState.kt:1449 | `it.status != "archived"` | Same |
| AncState.kt:1476 | `it.status != "archived"` | Same |
| AncViewModel.kt:581 | `it.status != "archived"` | Allow read for delivered |
| AncViewModel.kt:936 | `targetStatus == "archived"` | Handle "delivered" target |
| RegistrationFlow.kt:68 | `patient?.status == "archived"` | Block registration for delivered |
| RegistrationFlow.kt:155 | `patient.status == "archived"` | Block |
| RegistrationFlowDriver.kt:73 | `patient.status == "archived"` | Block |
| RegistrationFlowDriver.kt:112 | `it.status != "archived"` | Same |
| RecordsDao.kt:139,170,191,221 | `patient_status != 'archived'` | Add `AND patient_status != 'delivered'` to write guards |
| RecordsRepository.kt:183,467,561,884,1040,1102,1210,1292,1350,1396,1456,1544 | `patient.status == "archived"` | Add delivered check where writes blocked; allow reads |
| PatientListScreen.kt:151 | `patient.status == "archived"` | Add `isDelivered` projection |
| PatientListScreen.kt:190 | `it.status != "archived"` | Active count excludes delivered |
| PatientActionsModalSheet.kt:57 | `patient.status == "archived"` | Add delivered branch |
| PatientHubScreen.kt:138 | `patient.status != "archived"` | `canEdit = status == "active"` |
| PatientHubScreen.kt:279 | `patient.status != "archived"` | Same |
| AttachmentsSectionCard.kt:108 | `patient.status != "archived"` | Same |
| LabsTab.kt:70, MedsTab.kt:70,93, ScansTab.kt:86, VisitsTab.kt:87 | `patient.status != "archived"` | Same |

**Rule**: Delivered = read-only like archived for clinical edits, but **visible in directory** with Delivered filter, and **revert allowed**.

---

## 5. Freeze Rules (GA/EDD/Reminders/Alerts)

When `patient.status == "delivered"`:

| Computation | Behavior |
|---|---|
| `calculateGestationalAge()` | Returns frozen `frozen_ga_weeks`/`frozen_ga_days` (ignores current date) |
| `datingSummaryOf().edd` | Returns `frozen_edd` |
| `isHighRisk(patient.riskLevel)` | Uses `frozen_risk_level` |
| Reminders engine | Skips delivered patients entirely |
| High-risk alert banner | Shows frozen risk, no live recalculation |
| EDD banner | Shows frozen EDD with "Frozen at delivery" label |
| GA badge | Shows frozen GA with "Delivered" label |

Implementation: extend `DemoPatient.dating?.calculateGestationalAge()` and `datingSummaryOf()` in `ClinicalModels.kt` to check `patient.status == "delivered"` first.

---

## 6. New / Changed Files (signatures + line budgets)

### Storage (level-3)
| File | Change | Lines |
|---|---|---|
| `RecordEntities.kt` | Add `DeliveryPlanEntity`, extend `PatientEntity` with 10 new cols, make `PreviousPregnancyEntity.clinicianId` nullable | +80 |
| `RecordsDatabase.kt` | Version 7, add `DeliveryPlanEntity` to entities, add migration `from6To7` | +30 |
| `RecordsMigration.kt` | Add `from6To7`, `validateSchemaSixTables`, `validateSchemaSixMetadata`, `SCHEMA_SEVEN_TABLES` | +120 |
| `RecordsDao.kt` | `deliveryPlan(patientId)`, `upsertDeliveryPlan`, `updatePatientDelivered(...)`, `revertDelivery(...)` | +60 |
| `RecordsRepository.kt` | `execute(DeliverPatientCommand)`, `execute(RevertDeliveryCommand)`, backup/restore include delivery_plan | +100 |
| `PatientCommands.kt` | Add `DeliverPatientCommand`, `RevertDeliveryCommand` | +80 |
| `EncryptedBackupManager.kt` | SCHEMA_VERSION=4, add `deliveryPlan` to canonical JSON, include frozen fields | +50 |

### State (AncState.kt is 1869 lines → new reducers in new files)
| File | Change | Lines |
|---|---|
| `AncState.kt` | Add `DeliveryPlanDraft`, `DeliveryPlanDraftValues`, `DeliverPatientDraft`, events `StartDeliveryPlanEditor`, `SaveDeliveryPlan`, `MarkDelivered`, `RevertDelivery`, `DeliveryPlanDraftField` enum | +120 |
| `DeliveryPlanReducers.kt` (NEW) | Reducers for delivery plan draft, mark delivered, revert delivered | ~200 |
| `AncViewModel.kt` | Wire new commands, handle `MarkDelivered` + `RevertDelivery` | +50 |

### UI (each < 1500 lines)
| File | Change | Lines |
|---|---|
| `DeliveryPlanningCard.kt` | Already reads from `parseDeliveryNotes`; change to read from `patient.deliveryPlan` first, fallback to parser | +30 |
| `PlanTab.kt` | Replace `onEditPatient` → open `DeliveryPlanEditorScreen` (new route) | +20 |
| `DeliveryPlanEditorScreen.kt` (NEW) | Editor for 5 fields (hospital, fees amount+currency, plan text, postpartum note, attachments reuse), uses `DeliverySectionBox` | ~300 |
| `DeliveryPlanEditorViewModel.kt` (NEW) | Draft, validation, save command | ~150 |
| `PatientDirectoryCard.kt` | Add `isDelivered` to projection, show "Delivered" badge, move risk badge to leading edge of name (decision 6) | +40 |
| `PatientListScreen.kt` | Add "Delivered" filter chip, active count excludes delivered | +30 |
| `PatientActionsModalSheet.kt` | Add "Mark Delivered" (if active), "Revert Delivery" (if delivered) | +50 |
| `PatientHubScreen.kt` | `canEdit = patient.status == "active"` | +5 |
| `ClinicalModels.kt` | Freeze logic in `calculateGestationalAge()`, `datingSummaryOf()` | +40 |

### Tests (level-3 review + independent test review)
| File | Purpose |
|---|---|
| `DeliveryPlanMigrationTest.kt` (NEW, androidTest) | Open v6 DB, migrate to v7, verify delivery_plan table, patient delivered cols, previous_pregnancies clinician_id nullable |
| `DeliveryPlanRoundTripTest.kt` (NEW, test) | Export v7 backup → inspect → restore → assert delivery_plan + frozen fields intact |
| `DeliveredStateTest.kt` (NEW, test) | Mark delivered → patient not active, GA/EDD frozen, revert restores active + unfrozen |
| `DeliveredNegativeTest.kt` (NEW, test) | **Negative case**: Mark delivered on archived patient → rejected; Mark delivered with future date → rejected per owner decision |
| `BackupVersionTest.kt` (NEW, test) | v3 backup rejected on v7 build; v4 backup round-trips |
| `DeliveryPlanEditorTest.kt` (NEW, test) | Draft validation, fees number+currency, attachments reuse |

---

## 7. Build Slices (each compiles + passes gate alone)

| Slice | Files | Gate | Level-3 Review |
|---|---|---|---|
| **S0** Governance | `AGENTS.md` (add v7 sign-off line) | lint | — |
| **S1** Risk badge move | `PatientDirectoryCard.kt`, `ClinicalBadges.kt` | compile, UI tests | — |
| **S2** Previous-pregnancy dedupe | `PreviousPregnancyClinicalInput`, editor UI | compile, validation tests | — |
| **S3** Registration step tracker | `RegistrationFlow.kt`, `RegistrationFlowDriver.kt`, step UI | compile, flow tests | — |
| **S4** Schema v7 core | `RecordEntities.kt`, `RecordsDatabase.kt`, `RecordsMigration.kt` (+validation sets), `RecordsDao.kt` (delivery_plan CRUD) | **compile, migration test (JVM), schema export 7.json** | **Frontier review + independent test review** |
| **S5** Backup v4 + restore | `EncryptedBackupManager.kt`, `RecordsRepository.kt` (restoreCanonicalBackup) | compile, BackupVersionTest, BackupDatingEddRoundTripTest | **Frontier review** |
| **S6** Delivery plan editor | `DeliveryPlanEditorScreen.kt`, `DeliveryPlanEditorViewModel.kt`, `DeliveryPlanReducers.kt`, `AncState.kt` (draft/events), `PlanTab.kt` | compile, DeliveryPlanEditorTest | — |
| **S7** Mark delivered + revert + Delivered filter | `PatientCommands.kt`, `RecordsRepository.kt` (execute Deliver/Revert), `RecordsDao.kt` (updatePatientDelivered/revertDelivery), `DeliveryPlanReducers.kt`, `PatientListScreen.kt` (filter chip), `PatientActionsModalSheet.kt`, `PatientDirectoryCard.kt` (isDelivered), `ClinicalModels.kt` (freeze logic) | **compile, DeliveredStateTest, DeliveredNegativeTest** | **Frontier review + independent test review** |
| **S8** Print/PDF | (out of scope for this plan) | — | — |

**Order is strict**: S4 before S5 before S6 before S7. Each slice must pass `./gradlew :app:lint :app:test :app:connectedAndroidTest` (or JVM equivalent) before next.

---

## 8. Oracles (Tests with One Negative Case Each)

| Oracle | Location | Positive | Negative (must fail) |
|---|---|---|---|
| Migration | `DeliveryPlanMigrationTest.kt` (androidTest) | v6→v7 opens, tables exist, clinician_id nullable | Open v6 DB with unknown table → `UNKNOWN_SCHEMA` |
| Backup round-trip | `DeliveryPlanRoundTripTest.kt` (test) | Export v7 → inspect → restore → all delivery_plan rows + frozen fields match | Import v3 backup on v7 code → `InvalidPassphraseOrCorrupted` (schemaVersion mismatch) |
| Delivered state | `DeliveredStateTest.kt` (test) | Mark delivered → status=delivered, frozen GA/EDD/risk, not in active count, revert → active, unfrozen | Mark delivered on archived patient → `PATIENT_ARCHIVED`; future delivery date → rejected (owner TBD) |
| Editor | `DeliveryPlanEditorTest.kt` (test) | Save 5 fields → delivery_plan row, fees stored as minor units + currency | Fees non-numeric → validation error; currency not ISO → validation error |
| Directory filter | `PatientListScreenTest.kt` (test) | Delivered chip shows only delivered patients | Active count excludes delivered |

---

## 9. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| AncState.kt > 1500 lines | Constitution violation | New reducers in `DeliveryPlanReducers.kt`; do not add to AncState.kt |
| 20 "archived" checks missed one | Delivered patients editable | Search `status == "archived"` globally after S7; add delivered branch to all |
| Migration validation too strict | Legitimate v6 vaults rejected | `validateSchemaSixTables` must match exactly current 6.json tables |
| Backup version mismatch | User loses old backups | Owner accepted (decision 3); document in release notes |
| GA/EDD freeze logic inconsistent | Safety defect | Single freeze function in `ClinicalModels.kt`; all callers use it |
| Fees stored as float | Rounding errors | Store minor units (Int), currency ISO 4217 |

---

## 10. Owner Decisions Needed (max 6)

| # | Question | Recommended Default |
|---|---|---|
| 1 | Can an **archived** patient be marked delivered? | **No** — archived is terminal; require unarchive first |
| 2 | Delivery date allowed in **future**? | **No** — reject future dates (clinical safety) |
| 3 | Delivery date allowed **before LMP**? | **No** — reject if before patient's LMP |
| 4 | Hospital field: **free text** or predefined list? | **Free text** (matches Agreed Delivery Hospital in parser) |
| 5 | Currency: restrict to clinic locale (EGP) or any ISO 4217? | **Any ISO 4217** (store code, validate format) |
| 6 | Revert delivery: restore exact pre-delivery GA/EDD/risk or recompute from current date? | **Restore exact frozen values** (they were captured at delivery) |

---

## 11. UNVERIFIED Items

- Exact line count of `AncState.kt` after S6 additions (estimated 1869+120=1989 → must keep reducers out)
- `RecordsMigration.kt` validation sets for v6 not yet read in full (need `SCHEMA_SIX_TABLES` content)
- `EncryptedBackupManager.kt` canonical JSON structure for `PatientWithVisits` vs `DemoPatient` dual paths
- Whether `ClinicalModels.kt` has single GA/EDD calculation or duplicated in UI
- Test infrastructure for `androidTest` migration (Room test helper existence)
- Owner decision on 6 open questions above

---

## 12. Acceptance Checklist (per slice)

- [ ] S0: AGENTS.md has "Schema v7 approved 2026-11-10"
- [ ] S1: Risk badge at leading edge, severe BP + high risk → high risk dominant, 360dp no truncation
- [ ] S2: Previous-pregnancy form has single outcome + single mode, clinician_id optional
- [ ] S3: Registration step tracker saves profile first, opens dating/histories/previous-pregnancy editors
- [ ] S4: `./gradlew :app:compileDebugKotlin` passes, `7.json` exported, migration test passes
- [ ] S5: Backup v4 created, v3 rejected, round-trip test passes
- [ ] S6: Delivery plan editor opens from Plan tab pencil, saves 5 fields, attachments reuse works
- [ ] S7: Mark delivered → delivered filter shows patient, GA/EDD/risk frozen, revert restores active
- [ ] All: `./gradlew :app:lint :app:test :app:connectedAndroidTest` green