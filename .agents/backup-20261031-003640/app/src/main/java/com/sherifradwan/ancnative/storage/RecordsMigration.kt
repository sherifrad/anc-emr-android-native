package com.sherifradwan.ancnative.storage

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal class RecordsMigrationFailure(
    val reason: VaultBlockReason,
) : IllegalStateException("records-migration-rejected")

internal object RecordsMigration {
    fun from1To2(expectedVaultUuid: String): Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            validateSchemaOneTables(db)
            validateSchemaOneMetadata(db, expectedVaultUuid)

            db.execSQL("ALTER TABLE patients ADD COLUMN created_at TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN patient_status TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN archived_at TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN archived_reason TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN risk_level TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN age_text TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN address_text TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN blood_group_text TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN medical_history TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN surgical_history TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN family_history TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN allergy_history TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN gravida INTEGER")
            db.execSQL("ALTER TABLE patients ADD COLUMN parity INTEGER")
            db.execSQL("ALTER TABLE patients ADD COLUMN abortion INTEGER")
            db.execSQL("ALTER TABLE patients ADD COLUMN obstetric_notes TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN revision INTEGER NOT NULL DEFAULT 1")

            db.execSQL(
                """
                CREATE TABLE previous_pregnancies (
                    id TEXT NOT NULL,
                    patient_id TEXT NOT NULL,
                    model_version INTEGER NOT NULL,
                    date_text TEXT,
                    outcome_text TEXT,
                    pregnancy_type TEXT,
                    other_details TEXT,
                    place_and_doctor TEXT,
                    delivery_by TEXT,
                    outcome TEXT,
                    birth_method TEXT,
                    loss_management TEXT,
                    termination_method TEXT,
                    provenance_kind TEXT NOT NULL,
                    clinician_id TEXT NOT NULL,
                    recorded_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    revision INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX index_previous_pregnancies_patient_id " +
                    "ON previous_pregnancies (patient_id)",
            )
            db.execSQL(
                "CREATE INDEX index_previous_pregnancies_patient_id_updated_at " +
                    "ON previous_pregnancies (patient_id, updated_at)",
            )

            db.execSQL(
                """
                CREATE TABLE patient_command_receipts (
                    operation_uuid TEXT NOT NULL,
                    patient_id TEXT NOT NULL,
                    record_kind TEXT NOT NULL,
                    record_id TEXT NOT NULL,
                    committed_revision INTEGER NOT NULL,
                    committed_patient_revision INTEGER NOT NULL,
                    fingerprint TEXT NOT NULL,
                    PRIMARY KEY(operation_uuid),
                    FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX index_patient_command_receipts_patient_id " +
                    "ON patient_command_receipts (patient_id)",
            )
            db.execSQL(
                "CREATE INDEX index_patient_command_receipts_record_kind_record_id " +
                    "ON patient_command_receipts (record_kind, record_id)",
            )

            val changed = db.compileStatement(
                """
                UPDATE vault_metadata
                SET schema_version = 2
                WHERE vault_uuid = ?
                  AND schema_version = 1
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

    fun from2To3(expectedVaultUuid: String): Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            validateSchemaTwoTables(db)
            validateSchemaTwoMetadata(db, expectedVaultUuid)

            db.execSQL(
                """
                CREATE TABLE patient_dating (
                    patient_id TEXT NOT NULL,
                    lmp_date TEXT,
                    ultrasound_edd TEXT,
                    agreed_edd TEXT,
                    dating_method TEXT,
                    notes TEXT,
                    revision INTEGER NOT NULL,
                    PRIMARY KEY(patient_id),
                    FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX index_patient_dating_patient_id ON patient_dating (patient_id)",
            )

            db.execSQL(
                """
                CREATE TABLE ultrasound_scans (
                    id TEXT NOT NULL,
                    patient_id TEXT NOT NULL,
                    scan_date TEXT NOT NULL,
                    ga_weeks INTEGER,
                    ga_days INTEGER,
                    fetal_number TEXT,
                    presentation TEXT,
                    placental_site TEXT,
                    amniotic_fluid TEXT,
                    fetal_heart_rate TEXT,
                    biometry_bpd TEXT,
                    biometry_hc TEXT,
                    biometry_ac TEXT,
                    biometry_fl TEXT,
                    estimated_fetal_weight TEXT,
                    impression TEXT,
                    revision INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX index_ultrasound_scans_patient_id ON ultrasound_scans (patient_id)",
            )
            db.execSQL(
                "CREATE INDEX index_ultrasound_scans_patient_id_scan_date ON ultrasound_scans (patient_id, scan_date)",
            )

            db.execSQL(
                """
                CREATE TABLE lab_investigations (
                    id TEXT NOT NULL,
                    patient_id TEXT NOT NULL,
                    test_date TEXT NOT NULL,
                    test_category TEXT NOT NULL,
                    test_name TEXT NOT NULL,
                    result TEXT NOT NULL,
                    unit TEXT,
                    reference_range TEXT,
                    is_abnormal INTEGER NOT NULL,
                    notes TEXT,
                    revision INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX index_lab_investigations_patient_id ON lab_investigations (patient_id)",
            )
            db.execSQL(
                "CREATE INDEX index_lab_investigations_patient_id_test_date ON lab_investigations (patient_id, test_date)",
            )

            db.execSQL(
                """
                CREATE TABLE medication_records (
                    id TEXT NOT NULL,
                    patient_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    dosage TEXT NOT NULL,
                    route TEXT NOT NULL,
                    frequency TEXT NOT NULL,
                    start_date TEXT NOT NULL,
                    end_date TEXT,
                    status TEXT NOT NULL,
                    notes TEXT,
                    is_prophylaxis INTEGER NOT NULL,
                    revision INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX index_medication_records_patient_id ON medication_records (patient_id)",
            )
            db.execSQL(
                "CREATE INDEX index_medication_records_patient_id_start_date ON medication_records (patient_id, start_date)",
            )

            db.execSQL(
                """
                CREATE TABLE clinical_command_receipts (
                    operation_uuid TEXT NOT NULL,
                    patient_id TEXT NOT NULL,
                    record_kind TEXT NOT NULL,
                    record_id TEXT NOT NULL,
                    committed_revision INTEGER NOT NULL,
                    committed_patient_revision INTEGER NOT NULL,
                    fingerprint TEXT NOT NULL,
                    PRIMARY KEY(operation_uuid),
                    FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX index_clinical_command_receipts_patient_id ON clinical_command_receipts (patient_id)",
            )
            db.execSQL(
                "CREATE INDEX index_clinical_command_receipts_record_kind_record_id ON clinical_command_receipts (record_kind, record_id)",
            )

            val changed = db.compileStatement(
                """
                UPDATE vault_metadata
                SET schema_version = 3
                WHERE vault_uuid = ?
                  AND schema_version = 2
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

    fun from3To4(expectedVaultUuid: String): Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            validateSchemaThreeTables(db)
            validateSchemaThreeMetadata(db, expectedVaultUuid)

            db.execSQL("ALTER TABLE patient_dating ADD COLUMN transfer_date TEXT")
            db.execSQL("ALTER TABLE patient_dating ADD COLUMN embryo_age_days INTEGER")
            db.execSQL("ALTER TABLE visits ADD COLUMN linked_ultrasound_id TEXT")
            db.execSQL("ALTER TABLE ultrasound_scans ADD COLUMN linked_visit_id TEXT")
            db.execSQL("ALTER TABLE ultrasound_scans ADD COLUMN image_uris TEXT")
            db.execSQL("ALTER TABLE lab_investigations ADD COLUMN lab_name TEXT")
            db.execSQL("ALTER TABLE medication_records ADD COLUMN is_non_compliant INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE medication_records ADD COLUMN adverse_effects TEXT")
            db.execSQL("ALTER TABLE patients ADD COLUMN clinical_attention TEXT")

            val changed = db.compileStatement(
                """
                UPDATE vault_metadata
                SET schema_version = 4
                WHERE vault_uuid = ?
                  AND schema_version = 3
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

    fun from4To5(expectedVaultUuid: String): Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            validateSchemaFourTables(db)
            validateSchemaFourMetadata(db, expectedVaultUuid)

            db.execSQL(
                """
                CREATE TABLE patient_attachments (
                    id TEXT NOT NULL,
                    patient_id TEXT NOT NULL,
                    file_name TEXT NOT NULL,
                    file_path TEXT NOT NULL,
                    category TEXT NOT NULL,
                    mime_type TEXT NOT NULL,
                    file_size_bytes INTEGER NOT NULL,
                    sha256_hash TEXT NOT NULL,
                    title TEXT,
                    captured_at TEXT NOT NULL,
                    linked_record_id TEXT,
                    sync_status TEXT NOT NULL,
                    cloud_url TEXT,
                    cloud_synced_at TEXT,
                    revision INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX index_patient_attachments_patient_id ON patient_attachments (patient_id)",
            )
            db.execSQL(
                "CREATE INDEX index_patient_attachments_patient_id_category ON patient_attachments (patient_id, category)",
            )
            db.execSQL(
                "CREATE INDEX index_patient_attachments_sha256_hash ON patient_attachments (sha256_hash)",
            )

            val changed = db.compileStatement(
                """
                UPDATE vault_metadata
                SET schema_version = 5
                WHERE vault_uuid = ?
                  AND schema_version = 4
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
            buildSet {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    if (!name.startsWith("sqlite_") && name !in ROOM_INTERNAL_TABLES) add(name)
                }
            }
        }
        if (tables != SCHEMA_TWO_TABLES) {
            throw RecordsMigrationFailure(VaultBlockReason.UNKNOWN_SCHEMA)
        }
    }

    private fun validateSchemaOneMetadata(db: SupportSQLiteDatabase, expectedVaultUuid: String) {
        val rows = db.query(
            "SELECT vault_uuid, schema_version, is_synthetic, initialized FROM vault_metadata LIMIT 2",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MetadataRow(
                            vaultUuid = cursor.getString(0),
                            schemaVersion = cursor.getInt(1),
                            isSynthetic = cursor.getInt(2) == 1,
                            initialized = cursor.getInt(3) == 1,
                        ),
                    )
                }
            }
        }
        if (rows.isEmpty()) throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISSING)
        val row = rows.singleOrNull()
            ?: throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        if (
            row.vaultUuid != expectedVaultUuid ||
            row.schemaVersion != 1 ||
            !row.initialized
        ) {
            throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        }
    }

    private fun validateSchemaTwoMetadata(db: SupportSQLiteDatabase, expectedVaultUuid: String) {
        val rows = db.query(
            "SELECT vault_uuid, schema_version, is_synthetic, initialized FROM vault_metadata LIMIT 2",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MetadataRow(
                            vaultUuid = cursor.getString(0),
                            schemaVersion = cursor.getInt(1),
                            isSynthetic = cursor.getInt(2) == 1,
                            initialized = cursor.getInt(3) == 1,
                        ),
                    )
                }
            }
        }
        if (rows.isEmpty()) throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISSING)
        val row = rows.singleOrNull()
            ?: throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        if (
            row.vaultUuid != expectedVaultUuid ||
            row.schemaVersion != 2 ||
            !row.initialized
        ) {
            throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        }
    }

    private fun validateSchemaThreeTables(db: SupportSQLiteDatabase) {
        val tables = db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    if (!name.startsWith("sqlite_") && name !in ROOM_INTERNAL_TABLES) add(name)
                }
            }
        }
        if (tables != SCHEMA_THREE_TABLES) {
            throw RecordsMigrationFailure(VaultBlockReason.UNKNOWN_SCHEMA)
        }
    }

    private fun validateSchemaThreeMetadata(db: SupportSQLiteDatabase, expectedVaultUuid: String) {
        val rows = db.query(
            "SELECT vault_uuid, schema_version, is_synthetic, initialized FROM vault_metadata LIMIT 2",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MetadataRow(
                            vaultUuid = cursor.getString(0),
                            schemaVersion = cursor.getInt(1),
                            isSynthetic = cursor.getInt(2) == 1,
                            initialized = cursor.getInt(3) == 1,
                        ),
                    )
                }
            }
        }
        if (rows.isEmpty()) throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISSING)
        val row = rows.singleOrNull()
            ?: throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        if (
            row.vaultUuid != expectedVaultUuid ||
            row.schemaVersion != 3 ||
            !row.initialized
        ) {
            throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        }
    }

    private fun validateSchemaFourTables(db: SupportSQLiteDatabase) {
        val tables = db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    if (!name.startsWith("sqlite_") && name !in ROOM_INTERNAL_TABLES) add(name)
                }
            }
        }
        if (tables != SCHEMA_FOUR_TABLES) {
            throw RecordsMigrationFailure(VaultBlockReason.UNKNOWN_SCHEMA)
        }
    }

    private fun validateSchemaFourMetadata(db: SupportSQLiteDatabase, expectedVaultUuid: String) {
        val rows = db.query(
            "SELECT vault_uuid, schema_version, is_synthetic, initialized FROM vault_metadata LIMIT 2",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MetadataRow(
                            vaultUuid = cursor.getString(0),
                            schemaVersion = cursor.getInt(1),
                            isSynthetic = cursor.getInt(2) == 1,
                            initialized = cursor.getInt(3) == 1,
                        ),
                    )
                }
            }
        }
        if (rows.isEmpty()) throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISSING)
        val row = rows.singleOrNull()
            ?: throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        if (
            row.vaultUuid != expectedVaultUuid ||
            row.schemaVersion != 4 ||
            !row.initialized
        ) {
            throw RecordsMigrationFailure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        }
    }

    fun failureReason(failure: Throwable): VaultBlockReason? {
        var current: Throwable? = failure
        while (current != null) {
            if (current is RecordsMigrationFailure) return current.reason
            current = current.cause
        }
        return null
    }

    private data class MetadataRow(
        val vaultUuid: String,
        val schemaVersion: Int,
        val isSynthetic: Boolean,
        val initialized: Boolean,
    )

    private val SCHEMA_ONE_TABLES = setOf(
        "patients",
        "visits",
        "vault_metadata",
        "save_receipts",
    )
    private val SCHEMA_TWO_TABLES = setOf(
        "patients",
        "visits",
        "vault_metadata",
        "save_receipts",
        "previous_pregnancies",
        "patient_command_receipts",
    )
    private val SCHEMA_THREE_TABLES = setOf(
        "patients",
        "visits",
        "vault_metadata",
        "save_receipts",
        "previous_pregnancies",
        "patient_command_receipts",
        "patient_dating",
        "ultrasound_scans",
        "lab_investigations",
        "medication_records",
        "clinical_command_receipts",
    )
    private val SCHEMA_FOUR_TABLES = SCHEMA_THREE_TABLES
    private val SCHEMA_FIVE_TABLES = SCHEMA_THREE_TABLES + "patient_attachments"
    private val ROOM_INTERNAL_TABLES = setOf("android_metadata", "room_master_table")
}
