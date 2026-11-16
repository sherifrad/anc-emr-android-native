#1: Asserted dynamic `vaultStateTitle(fixture.state)` on gate screen card instead of removed `R.string.secure_demo_storage`.
#2: Removed `demo_banner` assertion in `reviewedVisitEditorLayouts...` since clinic header is rendered only on `PatientList`.
#3: Added `LocalActivityResultRegistryOwner provides activity` in `mountFixture` `CompositionLocalProvider`.
#4: Asserted `FLAG_SECURE` conditionally on `!BuildConfig.DEBUG` matching `MainActivity.onCreate`.
#5: Updated `DEMO_BANNER` constant to `"Dr. Sherif Radwan Clinic"` per D8 clinic branding.
#6: Initialized valid non-sequential PIN (`135246`); updated Arabic assertions and keypad inputs to English.
#7: Updated dialog title assertion to `"Clinic & System Settings"`.
#8: Added `"patient_attachments"` to approved tables; triage missed that `PRAGMA user_version` also needed updating from 4 to 5.
#9: Updated expected patient snapshot count to 4 to account for `DEMO-ANC-004`.
#10: Deleted from all child tables before `DELETE FROM patients` to resolve SQLite FK 1811 constraint failure.
#11: Updated dating committedRevision expectation to 2L because patient 0 has pre-existing seeded dating.
#12: Located saved lab and medication by ID and asserted count + 1; triage missed medication assertion failure at size 1.
#13: Updated expected `metadata.schemaVersion` and `PRAGMA user_version` from 4 to 5.
#14: Located pregnancy by ID for both correction and replay; triage missed second `.single()` at replay assertion.
#15: Untouched (suspected product issue awaiting owner decision).
#16: Updated expected snapshot patient count from 3 to 4.
Instrumented tests cannot run in sandbox (no connected device/emulator); orchestrator runs them on emulator.
Verified: `./gradlew --offline :app:compileDebugAndroidTestKotlin` and `:app:testDebugUnitTest` both succeeded.
Files changed:
- [AncWorkflowTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt)
- [SecurityWorkflowTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/SecurityWorkflowTest.kt)
- [EncryptedRecordsTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt)
- [VaultIntegrationTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/VaultIntegrationTest.kt)
