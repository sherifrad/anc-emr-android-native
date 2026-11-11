Add database migration 6 to 7 to an Android Room app (the owner approved this schema change). Make these edits only:
1. RecordsMigration.kt: add `fun from6To7(expectedVaultUuid: String): Migration` right after `from5To6`, same style. It calls validateSchemaSixTables(db) and validateSchemaSixMetadata(db, expectedVaultUuid), then runs four statements:
   ALTER TABLE patients ADD COLUMN delivered INTEGER NOT NULL DEFAULT 0
   ALTER TABLE patients ADD COLUMN delivery_date TEXT
   ALTER TABLE patients ADD COLUMN delivery_mode TEXT
   ALTER TABLE patients ADD COLUMN delivery_outcome TEXT
   then the same vault_metadata UPDATE as from5To6 but schema_version = 7 WHERE ... AND schema_version = 6 ..., and the same `changed != 1` failure.
2. Add validateSchemaSixTables (same query as validateSchemaFiveTables, compares with a new SCHEMA_SIX_TABLES = SCHEMA_FIVE_TABLES + "drafts") and validateSchemaSixMetadata (same as the five version but schemaVersion != 6). Place them directly after validateSchemaFiveMetadata. Add `private val SCHEMA_SIX_TABLES` after SCHEMA_FIVE_TABLES.
3. RecordsDatabase.kt: version = 7; add `RecordsMigration.from6To7(expectedVaultUuid = databasePath.vaultUuidFromPath()),` after the from5To6 line.
4. RecordsRepository.kt: `const val SCHEMA_VERSION = 7`.
