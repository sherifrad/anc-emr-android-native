# Native ANC P3 patient-workflow validation

**Status: PASS - Scoped P3 Engineering Acceptance** (updated 2026-10-11, Africa/Cairo)

P2 engineering acceptance is preserved at `evidence/p2/honor-engineering-adjudication.json`. This P3 slice was explicitly approved by the Owner after its separate AGY plan review. All 5 AGY review blockers and Compose lazy-list test scrolling issues have been resolved. Full offline host gates and connected API 36 instrumentation tests have passed with zero failures and zero lint errors.

## What is present and verified

P3 source implements schema 1→2 encrypted migration, patient-command receipts, nullable patient history/demographic fields, independent previous pregnancy records, revisioned patient/history/archive/pregnancy commands, Compose editors, Workspace/Profile/list synchronization, and paired English and Arabic strings.

### AGY Review Blockers Repaired:
1. **Raw Vault UUID Prefix**: Correctly stripped directory prefix (`vault-`) in `RecordsDatabase.kt` when resolving vault UUID from the database file parent directory so that migration metadata validation succeeds.
2. **Strict Migration Table Validation**: Added pre-migration check `validateSchemaOneTables(db)` and enforced strict `CREATE TABLE` without `IF NOT EXISTS` in `RecordsMigration.kt` to fail closed against unexpected existing tables.
3. **Localized Null Fallbacks**: Hardened `FactRow` in `Components.kt` and `ProfileScreen.kt` to render localized `"Not recorded"` (`R.string.not_recorded`) instead of blank strings for unset/null history and demographic values.
4. **Pregnancy Outcome/Method UI Incompatibility**: Enforced draft reset of mutually incompatible delivery methods, loss management, or termination methods upon outcome changes in `AncReducer.updatePregnancyDraft` (`AncState.kt`).
5. **Cross-Table Receipt Uniqueness**: Added transactional verification across `save_receipts` and `patient_command_receipts` in `RecordsRepository.kt` to ensure operation UUIDs cannot collide across tables.

## Verified Artifacts and Hashes

| Artifact | SHA-256 | Evidence Meaning |
| --- | --- | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | `32433b92cf746fc8d9057fc9271583ac1f132c887d08e37f8d7a02ff42fb484c` | Verified Schema 2 debug app APK passing all host gates and connected tests. |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `69a706ec7df70b58b04032e9c25a78e8dc698db33a9bd9fe5faa51d8f7451bcb` | Verified test APK containing 30 connected instrumented tests. |
| `app/schemas/com.sherifradwan.ancnative.storage.RecordsDatabase/2.json` | `dbf092f94e700b13235e10fb6868410eb8e1017c5879e18b4fa7323188944641` | Room compiler exported Schema 2 definition matching entity models and migration. |

## Verification Gates and Results

### 1. Host Gates
Command executed from root:
```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
```
- **Unit Tests**: **27 / 27 PASSED** (0 failures, 0 errors, 0 skipped)
  - `AncStateTest`: 9 tests
  - `SaveCoordinatorTest`: 4 tests
  - `VaultStateTest`: 2 tests
  - `PatientValidationTest`: 4 tests
  - `PreviousPregnancyValidationTest`: 4 tests
  - `PatientCommandsTest`: 4 tests
- **Lint**: **0 errors** (12 warnings, 1 hint) - preserved in `evidence/p3/host-gates.log`.
- **Build**: Assemble debug and androidTest APKs successful.

### 2. Connected Instrumentation Tests (Disposable API 36 Emulator)
Device: `anc-native-p3-api36-20261011` (Android 16 / API 36, `arm64-v8a`, `emulator-5586`).
Physical phone (`AX3C025527011668`, HONOR 400) was strictly protected and untouched.
Command executed:
```sh
ANDROID_SERIAL=emulator-5586 ./gradlew :app:connectedDebugAndroidTest --console=plain
```
- **Connected Tests**: **30 / 30 PASSED** (0 failures, 0 errors, 0 skipped) - preserved in `evidence/p3/connected-api36.log`.
  - `com.sherifradwan.ancnative.AncWorkflowTest`: 7 tests passed
  - `com.sherifradwan.ancnative.storage.EncryptedRecordsTest`: 12 tests passed
  - `com.sherifradwan.ancnative.storage.VaultIntegrationTest`: 11 tests passed

### 3. Physical Device Verification (HONOR 400)
Device: `AX3C025527011668` (HONOR `DNY-NX9`, Android 16 / API 36, `arm64-v8a`).
Scope: Same-signer incremental update without user data wipe (`adb install -r`), followed by cold-start launch observation (`evidence/p3/honor-device-install.json`).
- **Install**: Streamed incremental install SUCCESS.
- **Cold Start Launch**: `COLD` start in 810 ms (`com.sherifradwan.ancnative.MainActivity`, PID 5232).
- **Runtime Checks**: 0 fatal crashes, 0 ANRs, synthetic demo banner present, device credential gate active. No failure injections or automated test runners were directed to the physical phone.

## Scope and Safety Boundaries

- **P3 Acceptance is Scoped Engineering Acceptance**: Validates the local synthetic patient registration, patient history, previous pregnancy workflows, schema 1→2 migration, receipt replay rules, and physical device cold-start compatibility.
- **Not Clinical Acceptance**: The prototype uses synthetic demo data only and prominently displays `SYNTHETIC DEMO DATA — NOT FOR CLINICAL USE`.
- **Physical Device Safety Rules Preserved**: The connected HONOR 400 (`AX3C025527011668`) received an incremental non-destructive update only; no test runners, clears, uninstalls, or failure injections were executed against it.
- **Next Phase**: Subsequent approved slices will address clinical modules and protected export/backup workflows.
