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
