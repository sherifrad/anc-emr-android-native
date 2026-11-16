# Redesign Slice 18: Security, Lock and Vault Tests Report

## Summary
Created [SecurityLockVaultRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/SecurityLockVaultRedesignTest.kt) implementing comprehensive UI, accessibility, ergonomics, Level-3 security invariant verification, and behavioral parity testing for Redesign Slice 18 (`SecuritySettingsDialog`, `AppLockScreen`, `VaultGateScreen`, `ChangePinDialog`, `ExportBackupDialog`, `restore_confirm_dialog`, and `RestorePassphraseDialog`).

Zero existing test files required edits. Existing tests ([SecurityWorkflowTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/SecurityWorkflowTest.kt), [AncWorkflowTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt), and [CatalogSettingsRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/CatalogSettingsRedesignTest.kt)) continue to compile cleanly without any modification.

The test suite strictly adheres to the project style templates ([HistoryEditorsRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt), [RegistrationRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationRedesignTest.kt), [CatalogSettingsRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/CatalogSettingsRedesignTest.kt)):
- **Single `setContent` per test method**.
- **Isolated repository instances** per test via unique `SharedPreferences` names (`test_sec_${UUID.randomUUID()}`) with full clock control for deterministic lockout testing.
- **Narrow `360.dp x 640.dp` viewport host** (and `320.dp x 640.dp` for `VaultGateScreen` fail-closed verification) with clipping and boundary assertions.
- **Dynamic `fontScale` overrides** (`1.0f`, `1.3f`, `1.4f`, `2.0f`) via `LocalDensity` and configuration updates with teardown restoration.
- **`useUnmergedTree = true`** for semantic traversal where field labels use `clearAndSetSemantics`.
- **`performScrollTo()`** prior to assertions on scrollable components.
- **Node selection via explicit `testTag`s** on exact semantic nodes rather than ambiguous text.
- **Minimum touch target tolerance `>= 47.99.dp`** (`>= 47.9.dp`).
- **Complete test independence**: zero shared mutable state between tests.
- **In-memory `ActivityResultRegistryOwner`** providing deterministic file selection for `restoreFileLauncher` without external file picker dependencies.

---

## Coverage Matrix (32 Tests)

### 1. Typography & Unclipped Tabs (Zero Text < 12sp & Hierarchy)
- `typography_securitySettingsDialog_noTextBelow12sp`: Walks semantics tree of `securitySettingsDialog`; asserts zero text layout nodes have `fontSize < 12.sp` (`>= 11.99.sp`).
- `typography_appLockScreen_noTextBelow12sp`: Walks semantics tree of `appLockScreen` across emblem, titles, prompt rules, hints, PIN dots, and keypad; asserts zero text layout nodes have `fontSize < 12.sp`.
- `typography_vaultGate_noTextBelow12sp`: Walks semantics tree of `vaultGate` under blocked state; asserts zero text layout nodes have `fontSize < 12.sp`.
- `unclippedTabs_securitySettingsDialog_360dp_scale1_0`: Asserts tab switcher filter chips (`"Security & clinic"`, `"Lab tests"`, `"Medications"`) and backup action buttons (`"Export backup"`, `"Restore backup"`) are not ellipsized or clipped (`assertNotEllipsized`) at `fontScale = 1.0f` within `360.dp`.
- `unclippedTabs_securitySettingsDialog_360dp_scale1_3`: Asserts tabs and action buttons are not ellipsized or clipped at `fontScale = 1.3f` within `360.dp`.
- `backupButtons_sideBySideAtFontScale1_3`: Verifies `exportBackupButton` and `restoreBackupButton` sit side-by-side (`Row`: `abs(export.top - restore.top) < 2.dp`) at `fontScale <= 1.3f`.
- `backupButtons_stackVerticallyAtFontScale2_0`: Verifies `exportBackupButton` and `restoreBackupButton` stack vertically (`Column`: `restore.top >= export.bottom - 1.dp`) without clipping when `fontScale > 1.3f` (`2.0f`).

### 2. Touch Targets (>= 48dp on Every Interactive Element) & 360dp Containment
- `touchTargets_securitySettingsDialog_allInteractiveTargetsAtLeast48dp`: Verifies `>= 47.9.dp` width & height on `closeSettingsButton`, `settingsTabSecurity`, `settingsTabLabCatalog`, `settingsTabMedCatalog`, `themeOption-SYSTEM`, `themeOption-LIGHT`, `themeOption-DARK`, `saveBrandingButton`, `changePinButton`, `exportBackupButton`, `restoreBackupButton`, `lockAppNowButton`, and `testBiometricButton` (when biometric available).
- `touchTargets_appLockScreen_allKeypadKeysAtLeast48dp`: Verifies `>= 47.9.dp` width & height (and `height >= 55.9.dp`) on all 10 keypad keys `pinKey-0` through `pinKey-9`, `pinKey-backspace`, `pinKey-biometric` (when supported), and `useBiometricButton` (when supported).
- `touchTargets_vaultGate_actionButtonsAtLeast48dp`: Verifies `initializeStorage` on `VaultState.Uninitialized` measures `>= 47.9.dp` width and `>= 51.9.dp` height.
- `touchTargets_vaultGate_lockedActionAtLeast48dp`: Verifies `unlockStorage` on `VaultState.Locked` measures `>= 47.9.dp` width and `>= 51.9.dp` height.
- `touchTargets_vaultGate_blockedActionAtLeast48dp`: Verifies `retryStorage` on `VaultState.Blocked` measures `>= 47.9.dp` width and `>= 51.9.dp` height.
- `touchTargets_subDialogs_allButtonsAtLeast48dp`: Verifies `>= 47.9.dp` on `saveNewPinButton`, `cancelChangePinButton`, `confirmExportBackupButton`, and `cancelExportBackupButton`.

### 3. AppLockScreen State Machine, Weak PIN, Lockout & Unlock Flow
- `appLockScreen_pinSetupFlow_validatesWeakPin_mismatch_andCompletesSetup`: Verifies initial setup prompt and hints (`pinSetup`, `R.string.pin_rule`); enters weak PIN `111111` and asserts inline `appLockErrorMessage` (`R.string.pin_invalid`); enters valid PIN `135246` switching to confirmation mode (`pinConfirm`, `R.string.pin_confirm_title`); enters mismatch `246135` and asserts mismatch error resetting to setup prompt; enters matching `135246` and confirms successful unlock and repository state update (`needsPinSetup = false`, `isAppLocked = false`).
- `appLockScreen_weakPinError_notClippedAtFontScale1_3`: Verifies multi-line weak PIN error (`R.string.pin_invalid`) inside `appLockErrorMessage` is not clipped vertically (`assertTextNotClipped`) at `fontScale = 1.3f`.
- `appLockScreen_wrongPinError_backspace_andSuccessfulUnlock`: Tests configured unlock mode (`R.string.pin_unlock_title`); enters incorrect PIN `999999` and asserts `R.string.pin_incorrect`; verifies backspace clearing entered digits; enters correct PIN `135246` and confirms app unlocks.
- `appLockScreen_activeLockout_displaysCountdownAndDisablesKeypad`: Initialises repo with 30-second lockout; asserts `pinLockoutMessage` in `StatusHigh` surface displaying formatted countdown; asserts all keypad keys `pinKey-0`..`9`, `pinKey-backspace`, and `useBiometricButton` are disabled (`assertIsNotEnabled()`).

### 4. SecuritySettingsDialog Sub-Dialogs & Validations
- `securitySettingsDialog_changePinDialog_validatesAndUpdatesPin`: Opens `ChangePinDialog`; verifies title; enters weak PIN `111111` asserting `changePinError` keeping dialog open; enters mismatched confirmation asserting `security_pin_mismatch` keeping dialog open; tests cancel button dismisses; re-opens and enters matching valid PIN updating PIN in repository.
- `securitySettingsDialog_exportBackupDialog_validatesPassphraseAndExports`: Opens `ExportBackupDialog`; enters short passphrase (< 6 chars) asserting `security_passphrase_short`; enters mismatched passphrase asserting `security_passphrase_mismatch`; tests cancel button dismisses; re-opens and enters valid passphrase asserting `onExportBackup` callback receives passphrase.
- `securitySettingsDialog_restoreFlow_confirmDialog_andPassphraseDialog`: Dispatches backup file selection via test activity result registry; verifies `restore_confirm_dialog` appears with heading `"Replace all patient records?"` and destructive error-colored `"Replace all"`; verifies cancel dismisses without opening passphrase dialog; re-triggers and confirms `"Replace all"`; verifies `RestorePassphraseDialog` opens; verifies empty passphrase error; enters passphrase and confirms `onRestoreBackup` receives bytes and passphrase; verifies `backupStatusMessage` displays formatted plurals count (`"Vault restored (42 patients)."`). Also asserts touch targets `>= 48.dp` on `confirmReplaceAllButton`, `cancelReplaceAllButton`, `confirmRestoreBackupButton`, and `cancelRestorePassphraseButton`.

### 5. Settings Behavior Parity & Stored Values
- `securitySettingsDialog_tabSwitcher_navigatesToLabAndMedCatalogs`: Verifies switching between `settingsTabSecurity`, `settingsTabLabCatalog`, and `settingsTabMedCatalog` correctly mounts the respective catalog sections and returns to security settings.
- `securitySettingsDialog_themeOptions_updatesRepositoryThemeMode`: Toggles `themeOption-DARK`, `themeOption-LIGHT`, and `themeOption-SYSTEM`, asserting immediate updates to `securityRepo.settings.value.themeMode`.
- `securitySettingsDialog_clinicBranding_updatesRepositoryAndShowsSavedNotice`: Replaces `clinicNameField` and `clinicSubtitleField`; clicks `saveBrandingButton`; verifies `brandingSavedMessage` appears with `"Clinic branding saved and applied."` and repository state updates.
- `securitySettingsDialog_lockAppNow_andCloseButton_invokeCallbacks`: Clicks `lockAppNowButton`, asserting both `onDismiss` and `onLockNow` are invoked.
- `securitySettingsDialog_closeButton_invokesDismissCallback`: Clicks `closeSettingsButton`, asserting `onDismiss` is invoked.

### 6. VaultGateScreen States & Fail-Closed Invariants
- `vaultGate_uninitializedState_displaysChipAndInitializeButton`: Verifies `vaultStatusCard` shows `"Setup needed"` `BandChip` and `initializeStorage` action; clicks button and verifies `onInitialize` callback; asserts complete absence of patient records / search UI.
- `vaultGate_lockedState_displaysChipAndUnlockButton`: Verifies `vaultStatusCard` shows `"Locked"` `BandChip` and `unlockStorage` action; clicks button and verifies `onUnlock` callback.
- `vaultGate_blockedState_displaysHighToneChipAndRetryButton_andNoClippingAt320dp`: Verifies `vaultStatusCard` shows `"Blocked"` `BandChip` with `StatusTone.High` and `retryStorage` action; mounts at `320.dp` width and `fontScale = 1.4f`; verifies title and body (`R.string.blocked_key_invalidated`) are unclipped; clicks button and verifies `onRetry` callback.
- `vaultGate_intermediateStates_noActionButtons_andFailClosed`: Verifies intermediate checking state (`VaultState.Inspecting`) shows `"Checking"` `BandChip` with zero action buttons and zero clinical data.
- `vaultGate_customClinicBranding_rendersProperly`: Verifies custom `clinicName` and `clinicSubtitle` override strings render properly on `VaultGateScreen`.

### 7. Dark Theme & RTL Layout
- `darkThemeAndRtl_securitySettingsDialog_rendersCleanly`: Mounts `SecuritySettingsDialog` under `AncTheme(darkTheme = true)` and `LayoutDirection.Rtl`; verifies clean rendering without horizontal overflow.
- `darkThemeAndRtl_appLockScreen_rendersCleanly`: Mounts `AppLockScreen` under dark theme and RTL; verifies keypad layout and touch targets remain valid.
- `darkThemeAndRtl_vaultGate_rendersCleanly`: Mounts `VaultGateScreen` under dark theme and RTL; verifies `StatusHighDarkBg` and `StatusHighDarkBorder` render with unclipped text.

### 8. Exhaustive Existing Tags Check
- `existingTags_appLockScreen_allPresent`: Verifies `appLockScreen`, `pinDotsRow`, `pinDot-0`..`5`, `pinKey-0`..`9`, and `pinKey-backspace`.
- `existingTags_securitySettingsDialog_allPresent`: Verifies `securitySettingsDialog`, `closeSettingsButton`, `themeSettingsCard`, `themeOption-SYSTEM`, `themeOption-LIGHT`, `themeOption-DARK`, `clinicNameField`, `clinicSubtitleField`, `saveBrandingButton`, `changePinButton`, `biometricSwitch`, `backupVaultSettingsCard`, `exportBackupButton`, `restoreBackupButton`, `lockAppNowButton`.
- `existingTags_vaultGate_allPresent`: Verifies `vaultGate`, `vaultStatusCard`, and `initializeStorage`.

---

## Tag Coverage Verification (All 37 Existing Tags + 19 Added Tags)

### Existing Tags Preserved & Asserted (37/37)
- **`SecuritySettingsDialog` (16)**: `securitySettingsDialog`, `closeSettingsButton`, `themeSettingsCard`, `themeOption-SYSTEM`, `themeOption-LIGHT`, `themeOption-DARK`, `clinicNameField`, `clinicSubtitleField`, `saveBrandingButton`, `changePinButton`, `biometricSwitch`, `testBiometricButton`, `backupVaultSettingsCard`, `exportBackupButton`, `restoreBackupButton`, `lockAppNowButton`.
- **Sub-Dialogs (9)**: `newPinField`, `confirmPinField`, `saveNewPinButton`, `backupPassphraseField`, `confirmBackupPassphraseField`, `confirmExportBackupButton`, `restore_confirm_dialog`, `restorePassphraseField`, `confirmRestoreBackupButton`.
- **`AppLockScreen` (8 base + digits)**: `appLockScreen`, `pinLockoutMessage`, `pinSetup`, `pinConfirm`, `pinDotsRow`, `pinDot-0..5`, `pinKey-0..9`, `pinKey-backspace`, `pinKey-biometric`, `useBiometricButton`.
- **`VaultGateScreen` (4)**: `vaultGate`, `initializeStorage`, `unlockStorage`, `retryStorage`.

### Added Tags Covered (19/19)
- `settingsTabSecurity`, `settingsTabLabCatalog`, `settingsTabMedCatalog`, `brandingSavedMessage`, `biometricFeedbackMessage`, `backupStatusMessage`, `changePinDialog`, `cancelChangePinButton`, `changePinError`, `exportBackupDialog`, `cancelExportBackupButton`, `exportBackupError`, `confirmReplaceAllButton`, `cancelReplaceAllButton`, `restorePassphraseDialog`, `cancelRestorePassphraseButton`, `restorePassphraseError`, `appLockErrorMessage`, `vaultStatusCard`.

---

## Compilation & Verification Status
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: Exit code `0` (Success).
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q`: Exit code `0` (Success).
- File size of `SecurityLockVaultRedesignTest.kt`: 1,095 lines (well below the 1,500-line ceiling).
- Zero production edits; zero git commands executed; no emulator/adb used.
