### Triage Analysis: 16 Instrumented Failures

**1. AncWorkflowTest.reviewedVaultLayoutsKeepActionsReachableAndPatientDataFailClosed** — **STALE-TEST**
- Ev: [AncWorkflowTest.kt:181-183](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt#L181-L183) queries `R.string.secure_demo_storage` ("Encrypted Clinical Records"); [VaultGateScreen.kt:73,113](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/VaultGateScreen.kt#L73-L113) renders clinic title and `state.titleResource()`.
- Fix: Assert `state.titleResource()` (or clinic name) instead of `R.string.secure_demo_storage`.

**2. AncWorkflowTest.reviewedVisitEditorLayoutsKeepStatusDraftAndControlsAccessible** — **STALE-TEST**
- Ev: [AncWorkflowTest.kt:261](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt#L261) expects `R.string.demo_banner` on `VisitEditor`; [AncApp.kt:134](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt#L134) renders the clinic header only when `state.route == AncRoute.PatientList`.
- Fix: Remove `demo_banner` assertion in `reviewedVisitEditorLayouts...` on `VisitEditor`.

**3. AncWorkflowTest.datingAndUltrasoundWorkflowAndWorkspaceSectionsRenderAccurately** — **ENV**
- Ev: [UltrasoundEditor.kt:70](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundEditor.kt#L70) calls `rememberLauncherForActivityResult`; [AncWorkflowTest.kt:754](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt#L754) overrides `LocalContext` with `configuredContext` without providing `LocalActivityResultRegistryOwner`.
- Fix: Add `LocalActivityResultRegistryOwner provides activity` to `CompositionLocalProvider` in `AncWorkflowTest.kt:754`.

**4. AncWorkflowTest.recordsWindowKeepsSecureFlagAndGateExplainsDraftPolicy** — **STALE-TEST**
- Ev: [AncWorkflowTest.kt:95](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt#L95) asserts `FLAG_SECURE` unconditionally; [MainActivity.kt:40-42](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/MainActivity.kt#L40-L42) sets `FLAG_SECURE` only `if (!BuildConfig.DEBUG)`.
- Fix: Change test to assert `FLAG_SECURE` only when `!BuildConfig.DEBUG`.

**5. AncWorkflowTest.launchIsFailClosedAndNeverRendersSyntheticPatientsBeforeRealUnlock** — **STALE-TEST**
- Ev: [AncWorkflowTest.kt:978,1035](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt#L978-L1035) expects `"SYNTHETIC DEMO DATA — NOT FOR CLINICAL USE"`; D8 removed the banner in favor of clinic branding (`R.string.demo_banner`).
- Fix: Remove `assertDemoBannerDisplayed()` or update `DEMO_BANNER` to `"Dr. Sherif Radwan Clinic"`.

**6. SecurityWorkflowTest.appLockScreenValidatesSixDigitPinAndUnlocksWithDefaultPin** — **STALE-TEST**
- Ev: [SecurityWorkflowTest.kt:67,74,76](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/SecurityWorkflowTest.kt#L67-L76) expects Arabic strings and default PIN `123456`; D7/D8 mandate English-only UI and user-chosen PIN without default.
- Fix: Update test text to English and initialize custom PIN via `SecuritySettingsRepository` before testing lock.

**7. SecurityWorkflowTest.securitySettingsDialogAllowsAutoLockTimeoutConfigurationAndManualLock** — **STALE-TEST**
- Ev: [SecurityWorkflowTest.kt:122](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/SecurityWorkflowTest.kt#L122) asserts Arabic string `"إعدادات الأمان وقفل التطبيق"`; [SecuritySettingsDialog.kt:178](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/SecuritySettingsDialog.kt#L178) displays `"Clinic & System Settings"`.
- Fix: Change assertion in `SecurityWorkflowTest.kt:122` to `"Clinic & System Settings"`.

**8. EncryptedRecordsTest.schemaThreeContainsOnlyApprovedTablesColumnsForeignKeysAndIndexes** — **STALE-TEST**
- Ev: [EncryptedRecordsTest.kt:35-50](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt#L35-L50) expects 11 tables; schema v4/v5 adds table `patient_attachments` (cols: `id, patient_id, visit_id, file_name, mime_type, file_size_bytes, sha256_hex, caption, recorded_at, revision`).
- Fix: Add `"patient_attachments"` to expected tables set in `EncryptedRecordsTest.kt:36-48`.

**9. EncryptedRecordsTest.seedSaveReceiptRetryStaleAndCrossPatientRulesAreTransactional** — **STALE-TEST**
- Ev: [EncryptedRecordsTest.kt:465](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt#L465) asserts `patients.size == 3`; [DemoFixtures.kt:741](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/DemoFixtures.kt#L741) added a 4th demo patient (`DEMO-ANC-004`).
- Fix: Change assertion to `assertEquals(4, initial.snapshot.patients.size)`.

**10. EncryptedRecordsTest.emptyCommittedResultNeverSilentlyReseedsFixtures** — **STALE-TEST**
- Ev: [EncryptedRecordsTest.kt:598-600](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt#L598-L600) deletes only `save_receipts` and `visits` before `DELETE FROM patients`; SQLite throws FK error 1811 due to seeded rows in `previous_pregnancies`, `patient_dating`, `ultrasound_scans`, `lab_investigations`, `medication_records` ([RecordsRepository.kt:57-73](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt#L57-L73)).
- Fix: Delete from all child tables (`previous_pregnancies`, `patient_dating`, etc.) before `DELETE FROM patients`, or seed with `includeDemoPatients = false`.

**11. EncryptedRecordsTest.clinicalCommandsPersistDatingAndUltrasoundWithReceiptsAndAudit** — **STALE-TEST**
- Ev: [EncryptedRecordsTest.kt:668](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt#L668) expects revision `1L`; patient 0 already has a dating record with revision 1 ([DemoFixtures.kt:32](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/DemoFixtures.kt#L32)), so [RecordsRepository.kt:1073](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt#L1073) commits `existing.revision + 1` = 2.
- Fix: Change expectation to `assertEquals(2L, datingOutcome.receipt.committedRevision)`.

**12. EncryptedRecordsTest.clinicalCommandsPersistLabAndMedicationWithReceiptsAndAudit** — **STALE-TEST**
- Ev: [EncryptedRecordsTest.kt:738](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt#L738) asserts `labInvestigations.size == 1`; [DemoFixtures.kt:248](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/DemoFixtures.kt#L248) already seeds 6 labs for patient 0, making the total 7 after insert.
- Fix: Assert size 7 (or `initialCount + 1`) and locate the new lab by ID instead of `.first()`.

**13. EncryptedRecordsTest.encryptedSchemaOneMigratesLogicalRowsUnknownsMetadataAndRawVaultUuid** — **STALE-TEST**
- Ev: [EncryptedRecordsTest.kt:216,221](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt#L216-L221) expects `schemaVersion == 4`; [RecordsRepository.kt:1368](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt#L1368) upgraded `SCHEMA_VERSION` to 5.
- Fix: Change expected `metadata.schemaVersion` and `PRAGMA user_version` from 4 to 5.

**14. EncryptedRecordsTest.previousPregnancyCreateCorrectAndReplayAdvanceBothRevisionsAtomically** — **STALE-TEST**
- Ev: [EncryptedRecordsTest.kt:399](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt#L399) calls `.previousPregnancies.single()`; patient 0 already has 1 seeded pregnancy ([DemoFixtures.kt:35](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/DemoFixtures.kt#L35)), so adding one produces 2 elements.
- Fix: Match by ID (`.first { it.id == create.pregnancyId }`) instead of `.single()`.

**15. VaultIntegrationTest.initializationCancellationAndDetachBlockWhileUnlockCancellationRemainsRetryable** — **PRODUCT-BUG-SUSPECT**
- Ev: [VaultIntegrationTest.kt:233](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/VaultIntegrationTest.kt#L233) expects `VaultState.Blocked(VaultBlockReason.INCOMPLETE_SETUP)`; [VaultController.kt:243](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/VaultController.kt#L243) resets to `VaultState.Uninitialized`, causing the 15 s timeout in `awaitState`.
- Fix: Review [VaultController.kt:225-247](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/VaultController.kt#L225-L247). If cancelling mid-initialization must block per security contract, set state to `Blocked(INCOMPLETE_SETUP)`; if roll-back to `Uninitialized` was intended, align test expectation.

**16. VaultIntegrationTest.backgroundAndManualLockHideSnapshotAndRejectLateAuthentication** — **STALE-TEST**
- Ev: [VaultIntegrationTest.kt:173](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/storage/VaultIntegrationTest.kt#L173) asserts `snapshot.patients.size == 3`; [DemoFixtures.kt:741](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/DemoFixtures.kt#L741) provides 4 demo patients.
- Fix: Change assertion to `assertEquals(4, fixture.controller.view.value.snapshot?.patients?.size)`.

### Summary

| Test | Verdict | Effort |
| :--- | :--- | :--- |
| `AncWorkflowTest.reviewedVaultLayoutsKeepActionsReachableAndPatientDataFailClosed` | STALE-TEST | S |
| `AncWorkflowTest.reviewedVisitEditorLayoutsKeepStatusDraftAndControlsAccessible` | STALE-TEST | S |
| `AncWorkflowTest.datingAndUltrasoundWorkflowAndWorkspaceSectionsRenderAccurately` | ENV | S |
| `AncWorkflowTest.recordsWindowKeepsSecureFlagAndGateExplainsDraftPolicy` | STALE-TEST | S |
| `AncWorkflowTest.launchIsFailClosedAndNeverRendersSyntheticPatientsBeforeRealUnlock` | STALE-TEST | S |
| `SecurityWorkflowTest.appLockScreenValidatesSixDigitPinAndUnlocksWithDefaultPin` | STALE-TEST | M |
| `SecurityWorkflowTest.securitySettingsDialogAllowsAutoLockTimeoutConfigurationAndManualLock` | STALE-TEST | S |
| `EncryptedRecordsTest.schemaThreeContainsOnlyApprovedTablesColumnsForeignKeysAndIndexes` | STALE-TEST | S |
| `EncryptedRecordsTest.seedSaveReceiptRetryStaleAndCrossPatientRulesAreTransactional` | STALE-TEST | S |
| `EncryptedRecordsTest.emptyCommittedResultNeverSilentlyReseedsFixtures` | STALE-TEST | S |
| `EncryptedRecordsTest.clinicalCommandsPersistDatingAndUltrasoundWithReceiptsAndAudit` | STALE-TEST | S |
| `EncryptedRecordsTest.clinicalCommandsPersistLabAndMedicationWithReceiptsAndAudit` | STALE-TEST | S |
| `EncryptedRecordsTest.encryptedSchemaOneMigratesLogicalRowsUnknownsMetadataAndRawVaultUuid` | STALE-TEST | S |
| `EncryptedRecordsTest.previousPregnancyCreateCorrectAndReplayAdvanceBothRevisionsAtomically` | STALE-TEST | S |
| `VaultIntegrationTest.initializationCancellationAndDetachBlockWhileUnlockCancellationRemainsRetryable` | PRODUCT-BUG-SUSPECT | M |
| `VaultIntegrationTest.backgroundAndManualLockHideSnapshotAndRejectLateAuthentication` | STALE-TEST | S |
