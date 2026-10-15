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
    ],
    version = 5,
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
