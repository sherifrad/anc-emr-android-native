# Design: Delivery Outcome Database Field

## 1. Storage Option Selection

**Chosen: (a) New nullable columns on `patients` table (v6 → v7, `ALTER TABLE ADD COLUMN`)**

### Justification

| Factor | Option (a): Columns on `patients` | Option (b): New `delivery_outcomes` table |
|--------|-----------------------------------|-------------------------------------------|
| **Migration complexity** | Trivial — follows exact `clinical_attention` pattern (v5→6) | New table + FK + migration + repository/DAO additions |
| **Current pregnancy model** | One active pregnancy per patient record; history in `previous_pregnancies` | Over-normalised for current use case |
| **Backup/restore** | Automatic — columns serialize with `PatientEntity` | New table must be added to `EncryptedBackupManager` manually |
| **Optimistic `revision`** | Works unchanged — patient row revision increments on save | Requires coordinating revision across two tables |
| **Undo/delete** | Single row update; draft recovery already handles patient fields | Must handle orphaned outcome rows, cascade rules |
| **Test cost** | One instrumented migration test (v6 vault → v7) | Migration test + DAO tests + repository tests + backup tests |
| **Clinical authority** | Owner approved "a dedicated real column… well planned" | Adds tables the owner did not request |

**Conclusion:** The app treats a patient record as *one pregnancy episode*. Historical deliveries live in `previous_pregnancies`. Adding columns to `patients` matches the `clinical_attention` precedent, costs ~1/5 the test surface, and satisfies the clinical need for the current pregnancy.

---

## 2. Exact Column Specification

| Column (SQL) | Kotlin Property | Type | Nullable | Default | Constraints |
|--------------|-----------------|------|----------|---------|-------------|
| `delivered` | `delivered` | `INTEGER` (0/1) | YES | `NULL` | `CHECK (delivered IN (0,1))` |
| `delivery_date` | `deliveryDate` | `TEXT` | YES | `NULL` | ISO-8601 date (`YYYY-MM-DD`); `CHECK (delivery_date IS NULL OR delivery_date >= dating_start AND delivery_date <= date('now','+30 days'))` |
| `delivery_mode` | `deliveryMode` | `TEXT` | YES | `NULL` | `IN ('vaginal','c_section','assisted_vaginal','breech')` |
| `delivery_outcome` | `deliveryOutcome` | `TEXT` | YES | `NULL` | `IN ('live_birth','stillbirth','neonatal_death')` |
| `baby_sex` | `babySex` | `TEXT` | YES | `NULL` | `IN ('M','F','ambiguous','unknown')` |
| `baby_weight_grams` | `babyWeightGrams` | `INTEGER` | YES | `NULL` | `CHECK (baby_weight_grams IS NULL OR (baby_weight_grams >= 300 AND baby_weight_grams <= 6000))` |
| `delivery_place` | `deliveryPlace` | `TEXT` | YES | `NULL` | Free text, max 120 chars |
| `delivery_notes` | `deliveryNotes` | `TEXT` | YES | `NULL` | Free text, max 500 chars |

**Rules:**
- `delivered = 1` ⇒ `delivery_date` *may* be null (unknown date), but if present must pass date check.
- `delivered = 0` or `NULL` ⇒ all other delivery columns *must* be `NULL` (enforced in repository save, not DB CHECK to keep migration simple).
- Date validation uses `PatientDatingEntity.dating_start` (LMP/US dating) as lower bound; upper bound = today + 30 days (catches data-entry errors).

---

## 3. Migration 6 → 7

### Steps (in `RecordsMigration.from6To7`)

```kotlin
fun from6To7(expectedVaultUuid: String): Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Validate schema v6 exactly
        validateSchemaSixTables(db)
        validateSchemaSixMetadata(db, expectedVaultUuid)

        // 2. Add columns (all nullable, no defaults needed)
        db.execSQL("ALTER TABLE patients ADD COLUMN delivered INTEGER CHECK (delivered IN (0,1))")
        db.execSQL("ALTER TABLE patients ADD COLUMN delivery_date TEXT")
        db.execSQL("ALTER TABLE patients ADD COLUMN delivery_mode TEXT CHECK (delivery_mode IN ('vaginal','c_section','assisted_vaginal','breech'))")
        db.execSQL("ALTER TABLE patients ADD COLUMN delivery_outcome TEXT CHECK (delivery_outcome IN ('live_birth','stillbirth','neonatal_death'))")
        db.execSQL("ALTER TABLE patients ADD COLUMN baby_sex TEXT CHECK (baby_sex IN ('M','F','ambiguous','unknown'))")
        db.execSQL("ALTER TABLE patients ADD COLUMN baby_weight_grams INTEGER CHECK (baby_weight_grams IS NULL OR (baby_weight_grams >= 300 AND baby_weight_grams <= 6000))")
        db.execSQL("ALTER TABLE patients ADD COLUMN delivery_place TEXT")
        db.execSQL("ALTER TABLE patients ADD COLUMN delivery_notes TEXT")

        // 3. Bump vault_metadata.schema_version
        val changed = db.compileStatement("""
            UPDATE vault_metadata
            SET schema_version = 7
            WHERE vault_uuid = ? AND schema_version = 6 AND is_synthetic = 1 AND initialized = 1
        """.trimIndent()).use { statement ->
            statement.bindString(1, expectedVaultUuid)
            statement.executeUpdateDelete()
        }
        if (changed != 1) throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
    }
}
```

### Validation Functions (new, mirroring `validateSchemaFive*`)
- `validateSchemaSixTables`: asserts exact table set (14 tables including `drafts`).
- `validateSchemaSixMetadata`: asserts one row with `schema_version=6`, `is_synthetic=1`, `initialized=1`, matching `vault_uuid`.

### Existing Rows
All new columns are `NULL` → clinically "unknown / not delivered".

### Rollback / Failure Behaviour
- Migration runs in a transaction (Room wraps each `Migration.migrate`). Failure rolls back automatically.
- No down-migration provided (schema v6 is frozen; rollback = restore from backup).
- If `vault_metadata` update fails (wrong UUID, already migrated, corrupted), `RecordsMigrationFailure` bubbles up → vault blocked, UI shows "Vault incompatible" screen.

### Exported Schema File
`app/schemas/com.sherifradwan.ancnative.storage.RecordsDatabase/7.json` — generated by `exportSchema = true` at build time. Must be committed.

### Tests That Prove It
1. **Instrumented migration test** (`RecordsMigrationTest`): open a real v6 SQLCipher vault (fixture in `test/assets/vault_v6.db`), run migration, assert:
   - All 8 columns exist on `patients`
   - `vault_metadata.schema_version = 7`
   - Existing patient rows have `NULL` in new columns
   - CHECK constraints reject invalid values (insert bad `delivered=2` → constraint violation)
2. **DAO round-trip test**: insert patient with all delivery fields, read back, assert equality.
3. **Backup/restore test**: backup a v7 vault, restore to new path, open, assert delivery fields preserved.

---

## 4. Code Touch Points (by file & role)

| File | Role | Change | Protected (needs owner sign-off) |
|------|------|--------|----------------------------------|
| `RecordEntities.kt` | Entity | Add 8 fields to `PatientEntity` | **YES** |
| `RecordsDatabase.kt` | Database | Add `from6To7` to migrations list | **YES** |
| `RecordsMigration.kt` | Migration | Implement `from6To7` + validation fns | **YES** |
| `RecordsDao.kt` | DAO | Update `insertPatient`/`updatePatient` bindings; add `selectDeliveryOutcome` if needed | **YES** |
| `RecordsRepository.kt` | Repository | `savePatientProfile`: map command → entity with delivery fields; validation (delivered=1 ⇒ date allowed; delivered=0/NULL ⇒ others NULL) | **YES** |
| `PatientCommands.kt` | Command | `SavePatientProfileCommand` adds 8 delivery fields | **YES** |
| `ClinicalModels.kt` / `DemoModels.kt` | Domain model | Add delivery fields to `PatientProfile` / `DemoPatient` | **YES** |
| `EncryptedBackupManager.kt` | Backup | Serialize/deserialize 8 new fields in `PatientBackup` JSON | **YES** |
| `DraftMapping.kt` | Draft ↔ Record | Map delivery fields to/from `PatientDraft.values` | |
| `ClinicalDraftReducers.kt` | Draft reducer | Handle `PatientDraftField.DELIVERY_*` actions | |
| `AncState.kt` | UI state | Add delivery fields to `PatientHubState` / `PatientListItem` | |
| `DeliveryPlanSheet.kt` | Editor | Bind delivery fields in bottom sheet; save via existing profile command | |
| `DeliveryPlanCard.kt` | Card (Plan tab) | Render delivered state: date, days since, mode/outcome badges | |
| `PatientHubHeader.kt` | Header | Hide gestational-age "weeks" badge when delivered; show "Delivered DD MMM YYYY" | |
| `SummaryGenerator.kt` / `PrintGenerator.kt` | Print/Summary | Include delivery outcome section | **YES** |
| `strings.xml` | Strings | Labels for 8 fields + delivered/undelivered states | |
| `DemoFixtures.kt` | Demo data | Add delivered/undelivered demo patients | |
| **Tests** | | Migration test, DAO test, repo test, backup test, UI test for card/sheet | |

**Protected files (owner must approve by name):**
1. `RecordEntities.kt`
2. `RecordsDatabase.kt`
3. `RecordsMigration.kt`
4. `RecordsDao.kt`
5. `RecordsRepository.kt`
6. `PatientCommands.kt`
7. `ClinicalModels.kt` / `DemoModels.kt`
8. `EncryptedBackupManager.kt`
9. `SummaryGenerator.kt` / `PrintGenerator.kt`

---

## 5. Interaction with Delivery Plan Sheet & Draft

- **Single save path**: The existing `SavePatientProfileCommand` (triggered by "Save" in the delivery plan bottom sheet) writes **both** `obstetric_notes` (the 4 plan boxes) **and** the 8 delivery outcome fields atomically. No new command.
- **Draft recovery**: `PatientDraft` already stores `obstetricNotes`. Extend `DraftMapping` to include the 8 delivery fields. On sheet open, populate from draft if `base_revision` matches; else from record.
- **Touched/dirty rules**: A field is "dirty" if its value differs from the record at `base_revision`. The sheet's "Save" enabled state respects this (already implemented for other fields).
- **Card display when delivered**:
  - Header: replace "44+0 wks" badge with **"Delivered 12 Jan 2026"** (date from `delivery_date` or "Date unknown" if `delivered=1 ∧ delivery_date=NULL`).
  - Sub-line: **"3 days ago"** (computed from `delivery_date`).
  - Chips: Mode (e.g., "Vaginal"), Outcome ("Live birth"), Sex ("M"), Weight ("3.2 kg"), Place.
  - If `delivered=0/NULL`: show current plan boxes as today.

---

## 6. Clinical Safety Review

| Risk | Mitigation |
|------|------------|
| **Wrong delivery date** (typo, future date) | Repository validation: `delivery_date ≤ today+30d` and `≥ dating_start`. UI shows inline error if violated. |
| **Mistaken "Delivered" flag** | No auto-set. User must toggle explicitly. Undo: set `delivered=0` → all delivery fields cleared to `NULL` (repository enforces). Sheet shows "Mark as not delivered" action when `delivered=1`. |
| **Default "Delivered" on new patients** | `delivered` defaults `NULL` (unknown). Card shows plan boxes, not delivered state. |
| **360dp card readability** | Card uses 2-column chip grid (wraps to 2 rows). Truncation: `deliveryPlace` ellipsized at 18 chars; `deliveryNotes` not shown on card (only in sheet/summary). Minimum tap targets 48dp. |
| **Gestational-age badge after delivery** | Hidden when `delivered=1`; replaced by delivered date + days-since. Prevents "44+0 wks" confusion. |
| **Multiple pregnancies** | Current pregnancy outcome on `patients`. When new pregnancy started, clinician archives current patient (sets `patient_status=archived`) and creates new patient record. Historical outcome preserved on archived record. |

---

## 7. Work Packages (Lanes)

| Lane | Package | Files | Done Check |
|------|---------|-------|------------|
| **Data** | Migration 6→7 + validation | `RecordsMigration.kt`, `RecordsDatabase.kt` | Instrumented migration test passes |
| **Data** | Entity + DAO + Repository + Command | `RecordEntities.kt`, `RecordsDao.kt`, `RecordsRepository.kt`, `PatientCommands.kt` | DAO round-trip test + repo save validation test pass |
| **Data** | Backup/restore | `EncryptedBackupManager.kt` | Backup/restore test passes |
| **Domain** | ClinicalModels + DemoFixtures | `ClinicalModels.kt`, `DemoModels.kt`, `DemoFixtures.kt` | Demo data renders without crash |
| **Draft** | Draft mapping + reducers | `DraftMapping.kt`, `ClinicalDraftReducers.kt` | Draft recovery test (rotate sheet, reopen) passes |
| **UI** | Delivery plan sheet (edit) | `DeliveryPlanSheet.kt`, strings | Sheet saves all 8 fields; dirty rules work |
| **UI** | Delivery plan card (read) | `DeliveryPlanCard.kt`, `PatientHubHeader.kt` | Card shows delivered state correctly on 360dp |
| **Print** | Summary/Print generators | `SummaryGenerator.kt`, `PrintGenerator.kt` | PDF/printed output includes delivery section |
| **Test** | Integration + UI tests | New test files | All new tests green; existing suite unaffected |

**Order:** Data → Domain → Draft → UI (sheet) → UI (card/header) → Print → Test.

**Risks:**
- Migration validation too strict (fails on legitimate v6 variants) → mitigate by testing against 3 real v6 vault fixtures.
- Backup format change breaks restore of v6 backups → `EncryptedBackupManager` reads v6 JSON (missing fields → null) and writes v7.
- Draft recovery loses delivery fields on config change → covered by draft test.

---

## 8. Open Questions for Owner (max 5)

| # | Question | Recommended Answer |
|---|----------|---------------------|
| 1 | **Should `delivery_mode` include "vacuum" / "forceps" as separate values, or keep "assisted_vaginal" as one bucket?** | Keep **"assisted_vaginal"** single value. Clinicians can add detail in `delivery_notes`. Simpler enum, less UI clutter. |
| 2 | **Is `delivery_place` free text sufficient, or should it be a picker (Home / Hospital / Birth Centre / Other)?** | **Free text (max 120 chars)**. Pickers require maintenance; free text captures "Hospital X, Ward 3" which is more useful. |
| 3 | **When a patient is marked delivered, should the app auto-archive the record (`patient_status=archived`) or leave it active?** | **Leave active**. Clinician decides when to archive. Auto-archiving hides the delivered record from the main list prematurely. |
| 4 | **Should the "weeks" badge on the header show "Delivered at 39+3" instead of hiding entirely?** | **Hide the weeks badge; show "Delivered 12 Jan 2026" + "3 days ago"**. Mixing gestational age and delivery date on one line is confusing on 360dp. |
| 5 | **Do you want a hard constraint `delivered=1 ⇒ delivery_date NOT NULL` at the DB level?** | **No**. Keep nullable. Unknown delivery dates happen (referral from outside). Enforce in UI/repository with a warning, not a hard block. |

---

## Files I Would Need to See to Resolve Remaining Ambiguities

1. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/DeliveryPlanCard.kt` — exact card layout, chip grid, header badge logic.
2. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/DeliveryPlanSheet.kt` — current field bindings, save flow, draft keys.
3. `app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalModels.kt` — `PatientProfile` shape, to confirm mapping from entity.
4. `app/src/main/java/com/sherifradwan/ancnative/storage/SummaryGenerator.kt` — current print sections, to place delivery outcome correctly.
5. `app/src/androidTest/.../RecordsMigrationTest.kt` — existing migration test pattern, to replicate for v6→v7.
[Tokens] upload: 12176 download: 4906 cache_read: 0 cache_write: 0

