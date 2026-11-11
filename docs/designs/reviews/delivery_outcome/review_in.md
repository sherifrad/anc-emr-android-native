You are an independent reviewer. Below is (1) a brief, (2) a design written by another model, (3) real code excerpts and a real file list. Find defects in the design: invented files/columns/tables, SQL that would fail, Room schema-validation problems (CHECK clauses in ALTER TABLE ADD COLUMN vs Room's exported schema and fresh-install CREATE TABLE), scope creep beyond what the owner asked (delivered yes/no and the delivery date), ungrounded claims about how the app works (e.g. creating a new patient per pregnancy, backup serializing automatically), and missing touch points. Output: a numbered list of defects (each: what, where, why it matters, the fix), then a corrected short plan (columns, migration, touch points that really exist, tests). Be concrete and short.
=== DESIGN UNDER REVIEW ===
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

=== REAL FILE LIST (patient-related, main) ===
AttachmentsSectionCard.kt BedsideVitalsGrid.kt BiometryGrid.kt ClinicalAttentionBanner.kt ClinicalGroupSection.kt DeliveryPlanningCard.kt DeliveryPlanSheet.kt EditorSheetParts.kt LabReportItem.kt MedicationCard.kt MedicationForm.kt MedicationSheet.kt MedicationStopBar.kt MedicationTapToAddSection.kt OverviewSections.kt PastDeliveryMicroRow.kt PencilEditButton.kt 
app/src/main/java/com/sherifradwan/ancnative/model: catalog ClinicalModels.kt ClinicalSummaryGenerator.kt DatingSummary.kt DatingText.kt DeliveryNotes.kt DemoFixtures.kt DemoModels.kt GestationalAgeEngine.kt IsoDates.kt LabResultFlag.kt LongitudinalTimelineProjection.kt ObstetricHistory.kt Redating.kt VisitEntryLogic.kt VisitVitals.kt  app/src/main/java/com/sherifradwan/ancnative/ui/report: HtmlEscape.kt PatientPrintHtmlGenerator.kt PatientPrintManager.kt PatientTwoPageReportDialog.kt === BRIEF + EXCERPTS ===
# Task: design (no code) a real database field for delivery outcome

Android Kotlin app, Room + SQLCipher, schema now v6 (frozen by default). The owner (a clinician and the clinical authority) has now APPROVED a schema change for this one feature: "a dedicated real column or a dedicated place in the database, well planned."

## Need
The app has no way to record that a patient has delivered and when. Add: delivered yes/no, delivery date, and (propose) the minimum extra facts that are clinically useful and cheap (for example mode of delivery, outcome, baby sex/weight, place), each only if justified. Show it on the Delivery Planning card on the Plan tab and edit it in the existing delivery plan bottom sheet. Today patient status is only active/archived (`patient_status`), and the 4 delivery plan boxes live as labelled lines in `obstetric_notes`.

## What I need from you: a written design, no code
1. Storage options: (a) new nullable columns on `patients` (v6 to v7, ALTER TABLE ADD COLUMN), (b) a new table `delivery_outcomes` (one row per patient, FK to patients, with revision and provenance like the other record tables). Compare for this app: the migration pattern shown below (it validates the schema first, then bumps `vault_metadata.schema_version`), the fact that a patient may have several pregnancies over time (previous_pregnancies table exists), backup and restore, the existing optimistic `revision` rule, the undo/delete rules, and test cost. Pick ONE and justify.
2. Exact column names, types, nullability, defaults, constraints (e.g. date stored as ISO text; delivered implies date allowed but date may be unknown; date cannot be before dating start or in the far future).
3. Migration 6 to 7: exact steps, validation, what happens to existing rows (all undelivered/null), rollback and failure behaviour, the exported schema file `app/schemas/.../7.json`, and what tests prove it (an instrumented migration test from a real v6 vault).
4. Every code touch point, by file and role. The recent column `clinical_attention` is a good map of what a new patient column touches: RecordEntities, RecordsDao, RecordsRepository, PatientCommands (save profile command), RecordsMigration, EncryptedBackupManager (backup format), DraftMapping and PatientDraft state, ClinicalDraftReducers, DemoModels/DemoFixtures, AncState, the delivery plan sheet and card, strings, the print/summary generators, tests. Say which are protected storage files needing sign-off (RecordsRepository, RecordsDao, RecordsDatabase, ClinicalModels, summary/print generators) so the owner can approve them by name.
5. Interaction with the existing delivery plan sheet and draft: one save writes both notes and outcome atomically through the existing profile save path; touched/dirty rules; draft recovery; what the card shows when delivered (date, days since, and what happens to the gestational-age header and the "weeks" badge, which currently reads e.g. 44+0 wks for a patient whose EDD passed).
6. Clinical safety review: wrong-date risks, a mistaken "delivered" flag (how to undo), what must never be shown as delivered by default, and how the 360dp card stays readable.
7. Work packages for lanes (small, each with files and a done check), the order, the tests, and the risks. Mark the protected files.
8. Open questions for the owner, max 5, each with your recommended answer.

Be concrete and short. Plain language. If something in the excerpts is not enough to decide, say exactly which file you would need to see.

=== EXCERPT app/src/main/java/com/sherifradwan/ancnative/storage/RecordEntities.kt lines 15-39 ===
data class PatientEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "case_id") val caseId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "date_of_birth") val dateOfBirth: String?,
    val phone: String?,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "patient_status") val status: String? = null,
    @ColumnInfo(name = "archived_at") val archivedAt: String? = null,
    @ColumnInfo(name = "archived_reason") val archivedReason: String? = null,
    @ColumnInfo(name = "risk_level") val riskLevel: String? = null,
    @ColumnInfo(name = "age_text") val ageText: String? = null,
    @ColumnInfo(name = "address_text") val addressText: String? = null,
    @ColumnInfo(name = "blood_group_text") val bloodGroupText: String? = null,
    @ColumnInfo(name = "medical_history") val medicalHistory: String? = null,
    @ColumnInfo(name = "surgical_history") val surgicalHistory: String? = null,
    @ColumnInfo(name = "family_history") val familyHistory: String? = null,
    @ColumnInfo(name = "allergy_history") val allergyHistory: String? = null,
    val gravida: Long? = null,
    val parity: Long? = null,
    val abortion: Long? = null,
    @ColumnInfo(name = "obstetric_notes") val obstetricNotes: String? = null,
    @ColumnInfo(name = "clinical_attention") val clinicalAttention: String? = null,
    @ColumnInfo(defaultValue = "1") val revision: Long = 1,
)

=== EXCERPT app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDatabase.kt lines 1-112 ===
package com.sherifradwan.ancnative.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.UUID
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        PatientEntity::class,
        VisitEntity::class,
        VaultMetadataEntity::class,
        SaveReceiptEntity::class,
        PreviousPregnancyEntity::class,
        PatientCommandReceiptEntity::class,
        PatientDatingEntity::class,
        UltrasoundScanEntity::class,
        LabInvestigationEntity::class,
        MedicationEntity::class,
        ClinicalCommandReceiptEntity::class,
        PatientAttachmentEntity::class,
        DraftEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class RecordsDatabase : RoomDatabase() {
    abstract fun recordsDao(): RecordsDao

    companion object {
        private val nativeLibraryLock = Any()
        @Volatile private var nativeLibraryLoaded = false

        internal fun open(
            context: Context,
            databasePath: String,
            passphrase: ByteArray,
        ): OpenedRecordsDatabase {
            loadNativeLibrary()
            val factoryOwnedPassphrase = passphrase.copyOf()
            val factory = SupportOpenHelperFactory(factoryOwnedPassphrase, null, true)
            val database = Room.databaseBuilder(
                context.applicationContext,
                RecordsDatabase::class.java,
                databasePath,
            )
                .openHelperFactory(factory)
                .addMigrations(
                    RecordsMigration.from1To2(expectedVaultUuid = databasePath.vaultUuidFromPath()),
                    RecordsMigration.from2To3(expectedVaultUuid = databasePath.vaultUuidFromPath()),
                    RecordsMigration.from3To4(expectedVaultUuid = databasePath.vaultUuidFromPath()),
                    RecordsMigration.from4To5(expectedVaultUuid = databasePath.vaultUuidFromPath()),
                    RecordsMigration.from5To6(expectedVaultUuid = databasePath.vaultUuidFromPath()),
                )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
            return try {
                database.openHelper.writableDatabase
                OpenedRecordsDatabase(database, factoryOwnedPassphrase)
            } catch (failure: Throwable) {
                var definitelyClosed = false
                try {
                    database.close()
                    definitelyClosed = true
                } finally {
                    if (definitelyClosed) factoryOwnedPassphrase.fill(0)
                }
                throw failure
            }
        }

        private fun loadNativeLibrary() {
            if (nativeLibraryLoaded) return
            synchronized(nativeLibraryLock) {
                if (!nativeLibraryLoaded) {
                    System.loadLibrary("sqlcipher")
                    nativeLibraryLoaded = true
                }
            }
        }

        private fun String.vaultUuidFromPath(): String {
            val directoryName = requireNotNull(java.io.File(this).parentFile).name
            require(directoryName.startsWith(VAULT_DIRECTORY_PREFIX))
            val rawVaultUuid = directoryName.removePrefix(VAULT_DIRECTORY_PREFIX)
            require(
                runCatching { UUID.fromString(rawVaultUuid).toString() == rawVaultUuid }
                    .getOrDefault(false),
            )
            return rawVaultUuid
        }

        private const val VAULT_DIRECTORY_PREFIX = "vault-"
    }
}

internal class OpenedRecordsDatabase(
    val database: RecordsDatabase,
    private val factoryOwnedPassphrase: ByteArray,
) : AutoCloseable {
    @Volatile private var closed = false

    @Synchronized
    override fun close() {
        if (closed) return
        database.close()
        factoryOwnedPassphrase.fill(0)
        closed = true
    }
}

=== EXCERPT app/src/main/java/com/sherifradwan/ancnative/storage/RecordsMigration.kt lines 345-400 ===
    fun from5To6(expectedVaultUuid: String): Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            validateSchemaFiveTables(db)
            validateSchemaFiveMetadata(db, expectedVaultUuid)

            db.execSQL(
                """
                CREATE TABLE drafts (
                    patient_id TEXT NOT NULL,
                    screen_kind TEXT NOT NULL,
                    editor_kind TEXT NOT NULL,
                    record_id TEXT,
                    base_revision INTEGER,
                    payload TEXT NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY(patient_id, screen_kind)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX index_drafts_patient_id ON drafts (patient_id)",
            )

            val changed = db.compileStatement(
                """
                UPDATE vault_metadata
                SET schema_version = 6
                WHERE vault_uuid = ?
                  AND schema_version = 5
                  AND is_synthetic = 1
                  AND initialized = 1
                """.trimIndent(),
            ).use { statement ->
                statement.bindString(1, expectedVaultUuid)
                statement.executeUpdateDelete()
            }
            if (changed != 1) throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        }
    }

    private fun validateSchemaOneTables(db: SupportSQLiteDatabase) {
        val tables = db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    if (!name.startsWith("sqlite_") && name !in ROOM_INTERNAL_TABLES) add(name)
                }
            }
        }
        if (tables != SCHEMA_ONE_TABLES) {
            throw RecordsMigrationFailure(VaultBlockReason.UNKNOWN_SCHEMA)
        }
    }

    private fun validateSchemaTwoTables(db: SupportSQLiteDatabase) {
        val tables = db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
=== grep of clinical_attention / clinicalAttention in main code ===
app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt:114:    val clinicalAttention: String?,
app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt:159:        clinicalAttention = patient.clinicalAttention?.takeIf { it.isNotBlank() },
app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt:151:                            val hasAttention = !patient.clinicalAttention.isNullOrBlank()
app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt:379:                    value = draft.values.clinicalAttention,
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt:147:    val hasAttention = !patient.clinicalAttention.isNullOrBlank() || severeBp != null
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt:332:                            attention = patient.clinicalAttention,
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt:501:            .testTag("clinicalAttentionBanner")
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt:502:            .testTag("clinicalAttentionStrip"),
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubScreen.kt:363:                currentAttention = patient.clinicalAttention.orEmpty(),
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt:123:            .testTag("clinicalAttentionBanner"),
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt:155:                            text = stringResource(R.string.clinical_a
app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt:460:    val clinicalAttention: String = "",
app/src/main/java/com/sherifradwan/ancnative/state/ClinicalDraftReducers.kt:315:        PatientDraftField.CLINICAL_ATTENTION -> draft.values.copy(clinicalAttention = even
app/src/main/java/com/sherifradwan/ancnative/state/ClinicalDraftReducers.kt:462:    clinicalAttention = clinicalAttention.orEmpty(),
app/src/main/java/com/sherifradwan/ancnative/storage/RecordsMigration.kt:271:            db.execSQL("ALTER TABLE patients ADD COLUMN clinical_attention TEXT")
app/src/main/java/com/sherifradwan/ancnative/state/DraftMapping.kt:106:            "clinicalAttention" to draft.values.clinicalAttention,
app/src/main/java/com/sherifradwan/ancnative/state/DraftMapping.kt:369:                        clinicalAttention = record.fields["clinicalAttention"] ?: v.clinicalAttenti
app/src/main/java/com/sherifradwan/ancnative/storage/EncryptedBackupManager.kt:265:            putNullable("clinicalAttention", p.clinicalAttention)
app/src/main/java/com/sherifradwan/ancnative/storage/EncryptedBackupManager.kt:299:        putNullable("clinicalAttention", clinicalAttention)
app/src/main/java/com/sherifradwan/ancnative/storage/EncryptedBackupManager.kt:575:                clinicalAttention = optNullableString("clinicalAttention"),
app/src/main/java/com/sherifradwan/ancnative/storage/PatientCommands.kt:41:    val clinicalAttention: String? = null,
app/src/main/java/com/sherifradwan/ancnative/storage/PatientCommands.kt:91:                clinicalAttention = draft.values.clinicalAttention.normalized(),
app/src/main/java/com/sherifradwan/ancnative/storage/PatientCommands.kt:355:            gravida?.toString(), parity?.toString(), abortion?.toString(), obstetricNotes, cli
app/src/main/java/com/sherifradwan/ancnative/storage/RecordEntities.kt:37:    @ColumnInfo(name = "clinical_attention") val clinicalAttention: String? = null,
app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt:421:                clinicalAttention = command.clinicalAttention,
app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt:644:        clinicalAttention = clinicalAttention,
app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt:700:        clinicalAttention = clinicalAttention,
app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt:827:        clinicalAttention = clinicalAttention,
app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDao.kt:135:            clinical_attention = :clinicalAttention,
app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDao.kt:157:        clinicalAttention: String? = null,
app/src/main/java/com/sherifradwan/ancnative/model/DemoFixtures.kt:22:            clinicalAttention = "[NOTE] Prior uncomplicated full-term vaginal delivery (2023). Curre
app/src/main/java/com/sherifradwan/ancnative/model/DemoFixtures.kt:424:            clinicalAttention = "[ALERT] Mild anemia in pregnancy at 34w (Hb 10.6 g/dL, threshold <
app/src/main/java/com/sherifradwan/ancnative/model/DemoFixtures.kt:768:            clinicalAttention = "[CRITICAL ALERT] Prior IUFD at 34w + Primary APS + Rh-Negative. Da
app/src/main/java/com/sherifradwan/ancnative/model/DemoModels.kt:26:    val clinicalAttention: String? = null,
=== patient table: tests touching it ===
app/src/main/java/com/sherifradwan/ancnative/storage/RecordsMigration.kt
app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDatabase.kt
