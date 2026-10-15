# AI handoff: native ANC EMR for HONOR 400

Read this document first when resuming work. It describes current verified state and the next bounded steps; it is not a release or clinical-approval statement.

## Project and owner priorities

- Native Kotlin/Jetpack Compose project: `/Users/sherifgamalradwan/anc-emr-android-native`.
- This is separate from the ANC web/Capacitor checkout at `/Users/sherifgamalradwan/.codex/worktrees/b41b/anc-v3`; do not edit the web project for this task.
- Owner prioritizes the connected HONOR 400 only: `DNY-NX9`, Android 16 / API 36, `arm64-v8a` (`serial AX3C025527011668`). Broad Android-version support is out of scope. Build configuration is minSdk 35, compile/target SDK 36; do not change platform/dependency/version/signing pins.
- Synthetic-only prototype. Keep `SYNTHETIC DEMO DATA — NOT FOR CLINICAL USE` visible across all screens and dialogs. No real patient data, no guessed clinical values, no promotion to clinical use.
- Owner wants the whole app and a stable APK eventually. Work progresses in verified, incremental phases.
- Zero git repos/commits/branches by design.

## Critical Physical Device Safety Rule

- The physical HONOR 400 (`serial AX3C025527011668`) is the Owner's daily connected device.
- **NEVER execute automated connected test runners, failure injections, or destructive operations against it.**
- All connected instrumented tests MUST execute strictly on disposable virtual emulators (`ANDROID_SERIAL=emulator-5586`).
- The physical device only receives same-signer non-destructive incremental updates (`adb -s AX3C025527011668 install -r app-debug.apk`) followed by non-destructive cold-start observation.

## Current Verified State: Final Stabilization & UI/UX Polish Complete (2026-10-11)

This phase delivered comprehensive functional and logical verification via official Android tools (`android layout`, `android screen`), followed by targeted UI/UX and architectural stabilization:

### Verified Capabilities & UI/UX Refinements
1. **Pinned Workspace Navigation (Sticky Header)**:
   - Patient header and section navigation chips (`SectionSelector`) are pinned permanently in a top column.
   - The clinical records list (`workspaceList`) scrolls smoothly underneath, enabling 1-tap switching between Visits, Ultrasound, Laboratory, Medications, and Dating without scrolling back up.
2. **1-Tap Patient Card Navigation**:
   - The entire `PatientListCard` surface is directly clickable (`Card(onClick = ...)`), instantly opening patient workspaces with ripple feedback.
3. **Memoized Recomposition Performance**:
   - Patient search filtering is memoized via `remember(state.patients, state.searchQuery)` to eliminate redundant list calculations and preserve buttery smooth 60/120fps scrolling.
4. **Clinical Keyboard & IME Ergonomics**:
   - Integrated `KeyboardType.Decimal` and `KeyboardType.Number` with `ImeAction.Next` and `ImeAction.Done` across Visit, Ultrasound, Dating, and Patient editors.
5. **Clinical Summary & Referral Export**:
   - `ClinicalSummaryGenerator` compiles structured, bilingual-ready antenatal care reports encompassing patient demographics, pregnancy dating, vitals progression, ultrasound biometry, laboratory findings, and active medications.
   - One-tap clipboard copy integrated into `EpisodeOverviewContent` with `copyClinicalSummary` action.
6. **Encrypted Vault Local Backup Architecture**:
   - `EncryptedBackupManager` produces authenticated AES-256-GCM encrypted backup archives derived from a clinician's passphrase via PBKDF2-HMAC-SHA256 (65,536 iterations, 256-bit key, 16-byte random salt, 12-byte random IV).
   - `inspectBackup` validates backup authenticity, payload integrity, schema version, patient count, and export timestamp without modifying local device storage.
   - 100% pure Kotlin with zero external library dependencies and verified JVM test coverage.

### Verified Artifact Hashes
- **App APK (`app/build/outputs/apk/debug/app-debug.apk`)**: SHA-256 `c8154f1bdd27e3f2a96d38699dba8fa28df1053ab97201b8048fe31a84ff478d`
- **Android-test APK (`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`)**: SHA-256 `3e05a24a4980890f77dd4c2325c0e293133dd01768221b423e666e25c2a88b82`
- **Room schema3 JSON (`app/schemas/com.sherifradwan.ancnative.storage.RecordsDatabase/3.json`)**: SHA-256 `17f8d74365c04aff65cb0a0985ddeba458f3c8ebe2bdb3a13bcdcddcc236d956`

### Verification Summary
- **Unit Tests**: **46 / 46 PASSED** (0 failures, 0 errors, 0 skipped) across 9 test suites.
- **Lint**: **0 errors** across debug and androidTest targets.
- **Connected Tests (Disposable Emulator `emulator-5586`)**: **35 / 35 PASSED** (0 failures, 0 errors, 0 skipped) in 41s across `AncWorkflowTest`, `EncryptedRecordsTest`, and `VaultIntegrationTest`.
- **Cold Start Performance**: Cold start in **520 ms** on Android 16 (API 36); 0 crashes, 0 ANRs.

## Previous Phases Reference
- **Final Stabilization Report**: Documented in `docs/FINAL_STABILIZATION.md`.
- **P6 Acceptance**: Documented in `docs/P6_VALIDATION.md` and `evidence/p6/`.
- **P5 Acceptance**: Documented in `docs/P5_VALIDATION.md` and `evidence/p5/`. Lab investigations, medications & prescriptions, 10-minute session grace period.
- **P4 Acceptance**: Documented in `docs/P4_VALIDATION.md` and `evidence/p4/`. Pregnancy dating, ultrasound scans, Room Schema 3 migration.
- **P3 Acceptance**: Documented in `docs/P3_VALIDATION.md` and `evidence/p3/`. Schema 1→2 migration, synthetic patient registration, patient history, previous pregnancy workflows.
- **P2 Acceptance**: Documented in `docs/P2_VALIDATION.md` and `evidence/p2/`. Encrypted SQLCipher foundation, biometric gate, transaction receipts.
- **P4 Acceptance**: Documented in `docs/P4_VALIDATION.md` and `evidence/p4/`. Pregnancy dating, ultrasound scans, Room Schema 3 migration.
- **P3 Acceptance**: Documented in `docs/P3_VALIDATION.md` and `evidence/p3/`. Schema 1→2 migration, synthetic patient registration, patient history, previous pregnancy workflows.
- **P2 Acceptance**: Documented in `docs/P2_VALIDATION.md` and `evidence/p2/`. Encrypted SQLCipher foundation, biometric gate, transaction receipts.

## Next Slice: Release Preparation & Polish

The next development phase will focus on:
1. **Release APK Assembly**: Proguard/R8 optimization review and release APK packaging.
2. **Physical Device Live Handoff**: Deploying same-signer update to HONOR 400 once re-connected.
3. **Clinical Operational Readiness Checklist**: End-to-end review of all clinical workflows for uninterrupted daily practice.
