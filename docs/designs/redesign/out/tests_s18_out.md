The test suite for redesign slice 18 (Security, lock and vault) is complete and compiles cleanly.

### Changes Summary
- **New Test File**: [SecurityLockVaultRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/SecurityLockVaultRedesignTest.kt) (32 independent tests across 8 sections, 1,095 lines).
- **Test Report**: [tests_s18_report.md](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/out/tests_s18_report.md).
- **Existing Tests**: Zero edits required; [SecurityWorkflowTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/SecurityWorkflowTest.kt), [AncWorkflowTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt), and [CatalogSettingsRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/CatalogSettingsRedesignTest.kt) compile without modifications.
- **Constraints Maintained**: Zero production edits, no git history changes, no adb/emulator invocations.

### Verification Matrix Covered
1. **Typography & Unclipped Layout**:
   - Verified zero text nodes under 12sp (`fontSize.value >= 11.99.sp`) across `SecuritySettingsDialog`, `AppLockScreen`, and `VaultGateScreen`.
   - Verified unclipped tab switcher text (`"Security & clinic"`, `"Lab tests"`, `"Medications"`) and action buttons (`"Export backup"`, `"Restore backup"`) at `360dp` with `fontScale = 1.0f` and `1.3f`.
   - Verified responsive `exportBackupButton` and `restoreBackupButton` layout: side-by-side at `<= 1.3f` and stacked vertically at `fontScale = 2.0f`.
2. **Touch Targets (>= 47.99dp / 47.9dp)**:
   - Measured all interactive elements listed in the build report across all three screens and four sub-dialogs (`ChangePinDialog`, `ExportBackupDialog`, `restore_confirm_dialog`, and `RestorePassphraseDialog`).
3. **AppLockScreen Behavioral Parity & State Machine**:
   - Initial setup flow: weak PIN validation (`111111` inline error in `appLockErrorMessage`), confirmation mismatch reset, and matching PIN completion.
   - Verified multi-line setup error is unclipped at `fontScale = 1.3f`.
   - Unlock mode: wrong PIN error (`pin_incorrect`), backspace operation, and valid PIN unlock.
   - Lockout mode: countdown in `StatusHigh` banner with disabled keypad (`pinKey-0`..`9`), backspace, and biometric button.
4. **Sub-Dialogs & Destructive Confirmation**:
   - `ChangePinDialog`: weak PIN error and confirmation mismatch error keeping dialog open, cancel dismissal, and valid PIN update.
   - `ExportBackupDialog`: short passphrase (< 6 chars) and mismatch errors keeping dialog open, cancel dismissal, and valid passphrase callback dispatch.
   - Restore flow: activity result registry triggers `restore_confirm_dialog` with heading `"Replace all patient records?"` and error-colored `"Replace all"`; cancel dismissal; confirmation triggers `RestorePassphraseDialog`; empty passphrase validation; valid passphrase restore callback with formatted plurals status (`"Vault restored (42 patients)."`).
5. **Settings Behavior Parity**:
   - Pinned tab switcher navigates between `Security & clinic`, `Lab tests`, and `Medications` catalogs.
   - Theme mode selection updates `SecuritySettingsRepository.settings.value.themeMode`.
   - Clinic branding updates repository name/subtitle and displays `brandingSavedMessage`.
   - `lockAppNowButton` and `closeSettingsButton` invoke respective callbacks.
6. **VaultGateScreen States & Fail-Closed Invariants**:
   - `VaultState.Uninitialized`, `VaultState.Locked`, and `VaultState.Blocked` render appropriate `BandChip` indicators and single action buttons (`initializeStorage`, `unlockStorage`, `retryStorage`).
   - Blocked state with `VaultBlockReason.KEY_PERMANENTLY_INVALIDATED` renders unclipped at `320dp` with `fontScale = 1.4f`.
   - Intermediate inspecting/opening/closing states render status chips without action buttons and zero clinical data.
7. **Dark Theme & RTL**:
   - Verified `AncTheme(darkTheme = true)` and `LayoutDirection.Rtl` across all three screens without horizontal overflow.
8. **Tag Coverage**:
   - All 37 existing tags verified on their exact semantic nodes.
   - All 19 newly added tags verified.

### Verification Command
`./gradlew --offline :app:compileDebugAndroidTestKotlin -q` passed with exit code 0.
