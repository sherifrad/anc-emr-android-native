I have started running the unit test suite and will wait for it to complete.
### Build & Test Results
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`: **BUILD SUCCESSFUL** (33 tasks, 0 errors)
- `./gradlew --offline :app:testDebugUnitTest`: **BUILD SUCCESSFUL** (all unit tests passing, 0 failures)

### Table Name Changes & Verification
- `medications` -> `medication_records` (fixed wrong table name in [RecordsRepository.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt#L134))
- `drafts` explicitly added to raw SQL `DELETE` list alongside existing `dao.deleteAllDrafts()`
- Verified all cleared tables against Room Schema 6: `drafts`, `patient_attachments`, `clinical_command_receipts`, `medication_records`, `lab_investigations`, `ultrasound_scans`, `patient_dating`, `previous_pregnancies`, `save_receipts`, `patient_command_receipts`, `visits`, `patients` (never `vault_metadata`)
- Removed dead `SCHEMA_SIX_TABLES` in [RecordsMigration.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsMigration.kt) and simplified duplicate check in [DraftCodec.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/DraftCodec.kt#L38)
