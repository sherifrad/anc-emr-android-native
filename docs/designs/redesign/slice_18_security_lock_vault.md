# Slice 18: Security, lock and vault

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s18_security_lock_vault.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt` (`RecordType`, `RecordShapes`, `RecordSpacing`, `FieldFamilies`, `LocalFieldFamily`), `ui/components/RecordSectionCard.kt` (`RecordSectionCard`), `ui/components/StatusTag.kt` (`BandChip`, `StatusTone`), `ui/components/FormFieldLabel.kt` (`FormFieldLabel`), `ui/components/RecordFilterChip.kt` (`RecordFilterChip`), `ui/patienthub/components/EditorSheetParts.kt` (`SheetCancelSaveRow`), `ui/Components.kt` (`editorFieldColors`, `minimumTouchTarget`), and `ui/Theme.kt` (`StatusHighBg`, `StatusHighBorder`, `StatusHighText`, `StatusHighDarkBg`, `StatusHighDarkBorder`, `StatusHighDarkText`, `isDarkTheme`). Read them; do not re-spec them.

- **Owner direction**: `antislop: DURING`, dials ENERGY 1 / RHYTHM 1 / MOTION 1 (pressed and focus states only, no new animation). Presentation only: fix tab clipping, contrast failures on empty PIN dots/keypad keys, cramped dialog controls, missing error highlights, and indistinguishable destructive/blocked states at `360 dp` (`320 dp` in `VaultGateScreen` via `AncWorkflowTest`) and `1.3f`/`2.0f` font scales without altering any security logic.
- **Branch hygiene**: run `.agents/scripts/git_audit.sh` before starting and before pushing; update `docs/PROJECT_STATE.json` + run `.agents/scripts/project_state.py render` + update `.agents/pipeline/HANDOFF_AGY.md` in the same commit on `redesign-2026-11-15`.
- **Never touch**:
  - `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`
  - `storage/VaultController.kt`, `storage/EncryptedBackupManager.kt`, `storage/AndroidVaultKeys.kt`, `storage/VaultFiles.kt`, `storage/VaultState.kt`, `storage/VaultAutoAction.kt`, `storage/VaultCleanupRule.kt`, `storage/Hashing.kt`
  - `security/SecuritySettingsRepository.kt`, `security/PinPolicy.kt`, `security/BiometricAuthManager.kt`, `security/AppLockPolicy.kt`
  - `MainActivity.kt`, `ui/AncApp.kt` (Slice 19), `ui/CatalogSettingsSections.kt` (Slice 17)
  - `state/**` (`AncViewModel.kt`, `AncReducer.kt`, `AncState.kt`, `AncEvent.kt`, `UndoCoordinator.kt`), `model/**`, `*Generator*`
  - `ui/Theme.kt`, `ui/Components.kt`, `ui/components/**`, `ui/patienthub/components/EditorSheetParts.kt`
- **Visual only**: No Room schema change, no preferences key change, no PIN or cryptography change, no biometric/lockout/backup/vault state-machine change, no new security claims (only strings already grounded in the app, such as `AES-256 GCM` and `Android Keystore`).

## LEVEL 3 invariants (auth and security: what MUST NOT change)

`ui/SecuritySettingsDialog.kt`, `ui/AppLockScreen.kt`, and `ui/VaultGateScreen.kt` are **Level 3 (`auth_security`, `PLAN.md` 0.13)**. The reviewer will inspect the diff with `git diff -w`. Every call, argument, state rule, file provider URI, Keystore error mapping, and fail-closed branch below MUST remain verbatim in behaviour and arguments:

1. **`SecuritySettingsDialog.kt` signatures, state, and security calls**:
   - Keep exact composable signature:
     `fun SecuritySettingsDialog(securityRepo: SecuritySettingsRepository, onDismiss: () -> Unit, onLockNow: () -> Unit, onExportBackup: ((CharArray, (ByteArray?) -> Unit) -> Unit)? = null, onRestoreBackup: ((ByteArray, CharArray, (BackupInspectionResult) -> Unit) -> Unit)? = null)`
   - Keep `SettingsTab { SECURITY, LAB_CATALOG, MED_CATALOG }`, default tab `SettingsTab.SECURITY`, and `catalogRepo = remember { ClinicalCatalogRepository.getInstance(context) }` passed to `LabCatalogSettingsSection(catalogDelegate = catalogRepo)` and `MedicationCatalogSettingsSection(catalogDelegate = catalogRepo)`.
   - Keep `restoreFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent())` reading `context.contentResolver.openInputStream(uri)?.use { it.readBytes() }`, setting `pendingRestoreBytes = bytes` and `showRestoreConfirmDialog = true` only when `bytes != null && bytes.isNotEmpty()`, else setting `isBackupError = true` and `backupStatusMessage`. Keep `restoreFileLauncher.launch("*/*")`.
   - Keep `isBiometricAvailable = remember(context) { BiometricAuthManager.isBiometricAvailable(context) }`.
   - Keep `securityRepo.setThemeMode(mode)` + `settingsState = securityRepo.settings.value`.
   - Keep `securityRepo.updateClinicBranding(editedClinicName, editedClinicSubtitle)` + `settingsState = securityRepo.settings.value`.
   - Keep `securityRepo.setBiometricEnabled(enabled)` + `settingsState = securityRepo.settings.value` and `Switch(checked = settingsState.isBiometricEnabled && isBiometricAvailable, ..., enabled = isBiometricAvailable)`.
   - Keep `BiometricAuthManager.promptBiometric(activity = activity, onResult = { ok -> ... })` gated by `isBiometricAvailable && activity != null`.
   - Keep `lockAppNowButton` `onClick` order: `onDismiss(); onLockNow()`.
   - Keep `onExportBackup(passphrase) { backupBytes -> ... }`, `File(context.cacheDir, "backups")`, `"ANC_Vault_Backup_${System.currentTimeMillis()}.ancvault"`, `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)`, `Intent(Intent.ACTION_SEND)` with `type = "application/octet-stream"`, `EXTRA_STREAM`, `FLAG_GRANT_READ_URI_PERMISSION`, and `Intent.createChooser(...)`.
   - Keep `restore_confirm_dialog` gate before `RestorePassphraseDialog`: confirm sets `showRestoreConfirmDialog = false; showRestorePassphraseDialog = true`; dismiss/cancel sets `showRestoreConfirmDialog = false; pendingRestoreBytes = null`. Never skip confirmation.
   - Keep `onRestoreBackup(bytes, passphrase) { result -> ... }` exhaustive `when (result)` handling `BackupInspectionResult.Valid`, `BackupInspectionResult.InvalidPassphraseOrCorrupted`, and `BackupInspectionResult.Error`.
   - Keep `ChangePinDialog` digit filter `if (it.length <= 6 && it.all { c -> c in '0'..'9' })`, validation `if (!isValidNewPin(newPin)) ... else if (newPin != confirmPin) ... else if (securityRepo.updatePin(newPin)) onSuccess()`.
   - Keep `ExportBackupDialog` validation `if (passphrase.length < 6) ... else if (passphrase != confirmPassphrase) ... else onConfirm(passphrase.toCharArray())`.
   - Keep `RestorePassphraseDialog` validation `if (passphrase.isEmpty()) ... else onConfirm(passphrase.toCharArray())`.
   - Zero calls to `PinPolicy`, `EncryptedBackupManager`, `VaultController`, or `AndroidVaultKeys` directly from UI; zero `AncEvent` dispatches added or removed.

2. **`AppLockScreen.kt` signatures, state, lockout, PIN, and biometric calls**:
   - Keep exact composable signature:
     `fun AppLockScreen(securityRepo: SecuritySettingsRepository, onUnlockSuccess: () -> Unit, modifier: Modifier = Modifier)`
   - Keep `settingsState by securityRepo.settings.collectAsState()`, `setupMode = settingsState.needsPinSetup`, `legacyChangeRequired = settingsState.isLegacyPin && !settingsState.needsPinSetup`.
   - Keep `LaunchedEffect(Unit)` loop polling `lockoutRemainingMs = securityRepo.getLockoutRemainingMs().toInt()` with `kotlinx.coroutines.delay(1000)`.
   - Keep `LaunchedEffect(settingsState.needsPinSetup)` resetting `enteredPin`, `confirmationPin`, `isConfirmingPin`, `errorMessage` when `settingsState.needsPinSetup` is true.
   - Keep `isBiometricSupported = remember(context) { BiometricAuthManager.isBiometricAvailable(context) }` and auto-launch `LaunchedEffect(setupMode, legacyChangeRequired, settingsState.isBiometricEnabled, isBiometricSupported)` calling `BiometricAuthManager.promptBiometric(activity = activity, onResult = { success -> if (success) { securityRepo.unlockApp(); onUnlockSuccess() } })`.
   - Keep `handleDigit(digit)`: early return on `lockoutRemainingMs > 0` or `enteredPin.length >= 6`; at length 6 keep exact branches for `setupMode && !legacyChangeRequired` (`isValidNewPin(updated)`, `!isConfirmingPin`, `updated != confirmationPin`, `securityRepo.setupPin(updated)` -> `securityRepo.unlockApp(); onUnlockSuccess()`), `securityRepo.validatePin(updated)` (legacy weak-PIN re-prompt vs `securityRepo.unlockApp(); onUnlockSuccess()`), and failure (`errorMessage = if (securityRepo.getLockoutRemainingMs() > 0) null else pinIncorrectMessage; enteredPin = ""`).
   - Keep `handleBackspace()` and `triggerBiometric()` calls and guards (`if (lockoutRemainingMs > 0) return`).
   - Do NOT add PIN reset, "Forgot PIN", bypass buttons, or debug shortcuts; do NOT log or persist entered PIN digits.

3. **`VaultGateScreen.kt` fail-closed gate, Keystore/block mapping, and compatibility**:
   - Keep backward-compatible signature with trailing defaults:
     `fun VaultGateScreen(state: VaultState, onInitialize: () -> Unit, onUnlock: () -> Unit, onRetry: () -> Unit, clinicName: String? = null, clinicSubtitle: String? = null)`
   - Keep fail-closed button gating (`when (state)`): action buttons exist ONLY for `VaultState.Uninitialized` (`onInitialize`, `initializeStorage`), `VaultState.Locked` (`onUnlock`, `unlockStorage`), and `is VaultState.Blocked` (`onRetry`, `retryStorage`); `else -> Unit`. Never render patient data, skip buttons, or fake unlocked states.
   - Keep `VaultState.titleResource()`, `VaultState.bodyResource()`, and every single branch of `VaultBlockReason.bodyResource()` (all 16 `VaultBlockReason` entries, including `KEY_MISSING`, `KEY_PERMANENTLY_INVALIDATED`, `GCM_AUTHENTICATION_FAILED`, `DEVICE_CREDENTIAL_UNAVAILABLE`, `DATABASE_INTEGRITY_FAILED`) mapped to the exact same `R.string.*` keys.

## Decisions for the owner (this slice)

1. **Settings tab bar pattern and first tab wording (`FlowRow` of `RecordFilterChip`s vs `SecondaryScrollableTabRow`)**:
   Today `PrimaryTabRow` splits `~298 dp` into 3 equal `~99 dp` columns, clipping `"Security & Clinic"` next to `"Lab tests"` and `"Medications"`. **Recommended**: replace `PrimaryTabRow` with a wrapping `FlowRow(spacedBy(8.dp))` of `48.dp` `RecordFilterChip`s labeled `"Security & clinic"`, `"Lab tests"`, and `"Medications"`, matching Slice 17's switcher inside the same dialog, preserving exact `"Lab tests"` and `"Medications"` labels for `CatalogSettingsRedesignTest`, and eliminating clipping at `1.3f`/`2.0f` font scale.
2. **Domain color family (`FieldFamilies.patient()` blue vs neutral / sand)**:
   Today security and vault cards use warm grey `surfaceContainerLow` slabs and mixed `primary` / `ActionRose` accents. **Recommended**: use `FieldFamilies.patient()` (`#E9EEF8` / `#23407A` light, `#22304A` / `#AFC6F2` dark) across `Security & clinic`, PIN/backup dialogs, `AppLockScreen`, and `VaultGateScreen` so clinic identity, app lock, and encrypted vault status stay visually distinct from the `FieldFamilies.background()` sand catalog tabs (`PLAN.md` 2.1).
3. **Destructive restore confirmation (`restore_confirm_dialog`) heading and button color**:
   Today `restore_confirm_dialog` has no heading (`title = null`) and renders the irreversible `"Replace all"` button as a solid primary navy `Button`, looking like the safe default. **Recommended**: match `DiscardChangesDialog` (Slice 11) and `CatalogConfirmDialog` (Slice 17): `16.dp` `surface` `AlertDialog`, add title `"Replace all patient records?"` (`RecordType.SectionTitle`) above the exact existing body, keep `"Cancel"` in `primary`, and color `"Replace all"` in `MaterialTheme.colorScheme.error` inside a `48.dp` `TextButton`.
4. **Dual biometric controls on `AppLockScreen` (`pinKey-biometric` + `useBiometricButton`)**:
   Today `AppLockScreen` renders both a keypad fingerprint tile (`pinKey-biometric`, shown as a faint grey disabled box even when biometric unlock is turned off in settings) AND a `"Unlock with Fingerprint"` `TextButton` (`useBiometricButton`) below the keypad. **Recommended**: keep BOTH controls under the exact existing `if (isBiometricSupported && !setupMode && !legacyChangeRequired)` condition so zero Level-3 visibility rules or `testTag`s change, while giving disabled `pinKey-biometric` a clean `outlineVariant` disabled border instead of `0.5f` alpha grey tint and raising `useBiometricButton` to `48.dp`.
5. **String normalization, test-asserted strings, and emblem developer-jargon removal**:
   Today `SecuritySettingsDialog` displays `"Consistent with application icon and splash screen with ivory background #FBF8F5 without clipping."` at `11.sp`, `VaultGateScreen` hardcodes `"Dr. Sherif Radwan Clinic"` and `"Maternal-Fetal Medicine & Obstetrics"`, and `SecuritySettingsDialog` / `AppLockScreen` hardcode English button and status strings while `SecurityWorkflowTest` asserts 4 exact strings (`"Clinic & System Settings"`, `"Auto-Lock Timeout"`, `"Locks after 10 minutes away."`, `"Incorrect PIN. Please try again."`). **Recommended**: move all UI text into `strings.xml` in sentence case, keep those 4 test-asserted strings verbatim in `strings.xml`, replace the `#FBF8F5` developer jargon with `"Shown on the home screen, lock screen, and printed reports."`, and default `VaultGateScreen` branding via `R.string.app_name` and `R.string.clinic_default_subtitle`.

## Lessons (apply all)

- **Minimum `12.sp` everywhere** (`PLAN.md` 0, 2): eliminate `labelSmall` (`11.sp`) on the clinic emblem description and `brandingSavedMessage` in `SecuritySettingsDialog.kt`, replace `bodySmall` (`12.sp` implicit M3) with explicit `RecordType.Secondary` (`14.sp`), and lift `AppLockScreen` title/subtitles/hints (`pinSetup`, `pinConfirm`, `pinLockoutMessage`, error banner) to explicit `RecordType` styles (`>= 14.sp` / `18.sp` / `22.sp`).
- **3:1 non-text contrast on `AppLockScreen` PIN dots and keypad** (`R-25` / `antislop-human`): empty `pinDot-$i` circles currently use `outlineVariant` (`#CAC4D0` on `#FBF8F5` = `1.61:1`, FAIL `< 3.0:1`). Switch empty dot borders and enabled keypad button borders to `MaterialTheme.colorScheme.outline` (`#79747E` on `#FBF8F5` = `4.30:1` light, `#8E95A5` on `#0E121B` = `6.24:1` dark), keeping `outlineVariant` only for disabled keypad buttons during lockout and decorative card dividers.
- **No vertical clipping on `AppLockScreen` errors or `VaultGateScreen` buttons at `1.3f`/`2.0f` scale**: replace `AppLockScreen` `Box(Modifier.height(34.dp))` with `Box(Modifier.fillMaxWidth().heightIn(min = 32.dp))` so 2-3 line `pin_invalid` / `pin_mismatch` errors never clip vertically, and replace `VaultGateScreen` `height(52.dp)` with `heightIn(min = 52.dp).minimumTouchTarget()` so `"Initialize Clinical Records"` wraps safely at `320.dp` width and `1.4f`/`2.0f` scale.
- **Minimum `48.dp` width and height (`minimumTouchTarget()`) on every interactive target**: `closeSettingsButton`, `settingsTabSecurity`, `settingsTabLabCatalog`, `settingsTabMedCatalog`, `themeOption-*` rows, `saveBrandingButton`, `changePinButton`, `testBiometricButton`, `exportBackupButton`, `restoreBackupButton`, `lockAppNowButton`, `SheetCancelSaveRow` buttons in all 3 form dialogs, `confirmReplaceAllButton` / `cancelReplaceAllButton` in `restore_confirm_dialog`, all `pinKey-*` / `pinKey-biometric` / `pinKey-backspace` / `useBiometricButton` controls, and `initializeStorage` / `unlockStorage` / `retryStorage`.
- **Two corner radii only (`RecordShapes.Small` `10.dp`, `RecordShapes.Large` `16.dp`) and elevation `0.dp`**: remove `20.dp` dialog radius, `8.dp`/`3.dp`/`2.dp`/`1.dp` shadows, `14.dp` section card radius, `8.dp` theme option radius, `12.dp` vault button radius, `28.dp` M3 `AlertDialog` radius, and `999.dp` pill buttons.
- **Never place a `LazyColumn` inside `SecuritySettingsDialog` tab body**: `CatalogSettingsSections` (`LabCatalogSettingsSection` and `MedicationCatalogSettingsSection`) depend on `SecuritySettingsDialog` owning the outer `verticalScroll(rememberScrollState())`. Keep the header + tab bar pinned above `HorizontalDivider` and give the tab content `Column` `.weight(1f, fill = false).verticalScroll(rememberScrollState())`.
- **Keep every existing `testTag` on its exact semantic node**: keep `.testTag(...)` directly on `OutlinedTextField` (NOT on external-label wrapper `Column`s) for `clinicNameField`, `clinicSubtitleField`, `newPinField`, `confirmPinField`, `backupPassphraseField`, `confirmBackupPassphraseField`, and `restorePassphraseField`; keep `securitySettingsDialog` on the outer dialog `Surface` and `restore_confirm_dialog` on `AlertDialog`; keep all 37 existing tag expressions intact.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Lines | Change |
|---|---:|---|
| `res/values/strings.xml` | 953 | Normalize 1 value (`pin_unlock_title`: `"Enter Security PIN"` -> `"Enter security PIN"`); add all security, clinic, lock, and vault strings in Section 1 (no hardcoded UI text left in the 3 files) |
| `res/values/plurals.xml` | 15 | Add `<plurals name="security_vault_restored">` (`%1$d patient` / `%1$d patients`) |
| `ui/SecuritySettingsDialog.kt` | 979 | Restyle dialog chrome (`16.dp` `surface`, elevation `0`, pinned header + `FlowRow` `RecordFilterChip` tabs + `verticalScroll` body), all 6 sections via `RecordSectionCard(FieldFamilies.patient())`, `48.dp` controls, responsive `Export backup`/`Restore backup` row, `SecurityFormDialog` (`ChangePinDialog`, `ExportBackupDialog`, `RestorePassphraseDialog`) with `isError` field states + `SheetCancelSaveRow`, and `restore_confirm_dialog` with heading + `error`-colored `Replace all`; preserve every Level-3 call and tag verbatim |
| `ui/AppLockScreen.kt` | 483 | Restyle emblem (`72.dp`, border, elevation `0`), `RecordType.HeaderName`/`SectionTitle`/`Secondary` typography, `outline`-bordered empty PIN dots, `heightIn(min = 32.dp)` error slot, `StatusHigh` lockout banner, `outline`-bordered `RecordShapes.Large` keypad buttons (`outlineVariant` when disabled in lockout) + `FieldFamilies.patient()` utility keys, `48.dp` `useBiometricButton`, and `strings.xml` resources; preserve every PIN/lockout/biometric call and tag verbatim |
| `ui/VaultGateScreen.kt` | 222 | Restyle emblem (`80.dp`, border), optional `clinicName`/`clinicSubtitle` params defaulting to `strings.xml`, `OutlinedCard(RecordShapes.Large)` with top `FieldPatientBg` / `StatusHighBg` band + `BandChip`, left-aligned `18.sp` heading + `16.sp` body, and `heightIn(min = 52.dp)` `RecordShapes.Small` action button; preserve all `VaultState` / `VaultBlockReason` mappings and tags verbatim |

No edits needed to `ui/Theme.kt`, `ui/components/*`, `ui/Components.kt`, `ui/patienthub/components/EditorSheetParts.kt`, `ui/CatalogSettingsSections.kt`, `ui/AncApp.kt`, `MainActivity.kt`, `security/*`, or `storage/*`.

## 1. Strings (`app/src/main/res/values/strings.xml` & `plurals.xml`)

### Reuse existing keys verbatim (do NOT alter values):
- `app_name` (`"Dr. Sherif Radwan Clinic"`), `close`, `cancel`
- `pin_setup_title`, `pin_confirm_title`, `pin_legacy_title`, `pin_rule`, `pin_invalid`, `pin_mismatch`, `pin_incorrect` (`"Incorrect PIN. Please try again."`, asserted by `SecurityWorkflowTest`), `pin_lockout`, `pin_setup_hint`, `pin_confirm_hint`
- `initialize_demo_storage`, `unlock_demo_storage`, `retry_storage_check`, and all `storage_*` / `blocked_*` keys in `VaultGateScreen.kt` (`AncWorkflowTest` reads them via `R.string.*`)

### Change value (key unchanged; grep confirmed zero tests assert the old title case):
- `pin_unlock_title`: `"Enter Security PIN"` -> `"Enter security PIN"`

### Add to `strings.xml` (sentence case, English default, no em dashes or middle dots):
```xml
<string name="clinic_logo_cd">Clinic logo</string>
<string name="clinic_default_subtitle">Maternal-Fetal Medicine &amp; Obstetrics</string>

<string name="security_settings_dialog_title">Clinic &amp; System Settings</string>
<string name="settings_tab_security">Security &amp; clinic</string>
<string name="settings_tab_lab_tests">Lab tests</string>
<string name="settings_tab_medications">Medications</string>

<string name="security_theme_title">App theme</string>
<string name="security_theme_subtitle">Choose a color scheme for bright clinic rooms or dimmed ultrasound suites.</string>
<string name="theme_mode_system">System default</string>
<string name="theme_mode_light">Light mode</string>
<string name="theme_mode_dark">Dark mode</string>

<string name="security_branding_title">Clinic branding</string>
<string name="security_emblem_title">Clinic emblem</string>
<string name="security_emblem_desc">Shown on the home screen, lock screen, and printed reports.</string>
<string name="security_clinic_name_label">Clinic name (header and reports)</string>
<string name="security_clinic_subtitle_label">Specialty or subtitle</string>
<string name="security_save_branding">Save clinic branding</string>
<string name="security_branding_saved">Clinic branding saved and applied.</string>

<string name="security_pin_section_title">6-digit PIN</string>
<string name="security_pin_section_desc">6-digit security PIN to prevent unauthorized access to clinical patient records.</string>
<string name="security_change_pin_action">Change 6-digit PIN</string>

<string name="security_biometric_title">Biometric unlock</string>
<string name="security_biometric_switch_label">Fingerprint unlock</string>
<string name="security_biometric_ready">Fingerprint ready</string>
<string name="security_biometric_unavailable">Fingerprint not available on this device</string>
<string name="security_biometric_desc">Unlock the app with a touch instead of entering your PIN each time.</string>
<string name="security_test_biometric">Test fingerprint</string>
<string name="security_biometric_verified">Fingerprint verified. Ready for use.</string>
<string name="security_biometric_failed">Fingerprint verification failed.</string>

<string name="security_auto_lock_title">Auto-Lock Timeout</string>
<string name="security_auto_lock_body">Locks after 10 minutes away.</string>

<string name="security_backup_title">Vault backup and restore</string>
<string name="security_backup_desc">Export and restore patient records. Backups contain patient records only, not photos.</string>
<string name="security_export_backup">Export backup</string>
<string name="security_restore_backup">Restore backup</string>
<string name="security_lock_app_now">Lock app now</string>

<string name="security_backup_read_error">Unable to read the selected backup file.</string>
<string name="security_backup_share_title">Save or share encrypted backup</string>
<string name="security_backup_export_ok">Encrypted backup created and exported (%1$d bytes).</string>
<string name="security_backup_share_error">Backup created, but the share sheet could not open: %1$s</string>
<string name="security_backup_export_fail">Failed to generate backup.</string>
<string name="security_restore_decrypt_fail">Decryption failed: incorrect passphrase or corrupted backup file.</string>
<string name="security_restore_error">Error during restore: %1$s</string>

<string name="security_set_pin_title">Set new 6-digit PIN</string>
<string name="security_new_pin_label">New 6-digit PIN</string>
<string name="security_confirm_pin_label">Confirm new PIN</string>
<string name="security_save_pin">Save PIN</string>
<string name="security_pin_mismatch">New PIN and confirmation do not match.</string>

<string name="security_export_dialog_title">Export encrypted backup</string>
<string name="security_export_dialog_body">Enter a strong passphrase to encrypt the backup file (AES-256 GCM). You will need this passphrase to restore the vault.</string>
<string name="security_passphrase_label">Backup passphrase</string>
<string name="security_confirm_passphrase_label">Confirm passphrase</string>
<string name="security_passphrase_short">Passphrase must be at least 6 characters.</string>
<string name="security_passphrase_mismatch">Passphrase and confirmation do not match.</string>

<string name="security_restore_confirm_title">Replace all patient records?</string>
<string name="security_restore_confirm_body">Restoring replaces every patient on this phone with the backup. This cannot be undone.</string>
<string name="security_restore_replace_all">Replace all</string>

<string name="security_restore_dialog_title">Restore encrypted vault</string>
<string name="security_restore_dialog_body">Enter the passphrase for this encrypted file to restore all patients, visits, ultrasounds, and clinical records.</string>
<string name="security_decrypt_and_restore">Decrypt and restore</string>
<string name="security_restore_passphrase_empty">Please enter the passphrase.</string>

<string name="pin_digits_entered_cd">%1$d of 6 digits entered</string>
<string name="pin_biometric_failed">Fingerprint verification failed. Use your PIN.</string>
<string name="pin_biometric_unavailable">Fingerprint not available on this device.</string>
<string name="biometric_fingerprint_cd">Fingerprint</string>
<string name="keypad_backspace_cd">Backspace</string>
<string name="security_unlock_with_fingerprint">Unlock with fingerprint</string>

<string name="vault_status_setup_needed">Setup needed</string>
<string name="vault_status_locked">Locked</string>
<string name="vault_status_blocked">Blocked</string>
<string name="vault_status_checking">Checking</string>
<string name="vault_status_opening">Opening</string>
<string name="vault_status_unlocked">Unlocked</string>
<string name="vault_status_closing">Closing</string>
```

### Add to `plurals.xml`:
```xml
<plurals name="security_vault_restored">
    <item quantity="one">Vault restored (%1$d patient).</item>
    <item quantity="other">Vault restored (%1$d patients).</item>
</plurals>
```

Note on exact test strings: `security_settings_dialog_title` MUST remain `"Clinic & System Settings"`, `security_auto_lock_title` MUST remain `"Auto-Lock Timeout"`, and `security_auto_lock_body` MUST remain `"Locks after 10 minutes away."` because `SecurityWorkflowTest` asserts them verbatim.

## 2. Shared private helpers (`ui/SecuritySettingsDialog.kt`)

Add these private composables inside `SecuritySettingsDialog.kt` (matching `CatalogSettingsSections.kt`, wrapped in `FieldFamilies.patient()`):

1. **`SecurityFormDialog`**:
   - Uses `Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false))`.
   - `val maxHeight = (LocalConfiguration.current.screenHeightDp - 32).dp`
   - `Surface(modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = maxHeight).let { if (dialogTag != null) it.testTag(dialogTag) else it }, shape = RecordShapes.Large, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))`
   - Inside `CompositionLocalProvider(LocalFieldFamily provides FieldFamilies.patient())`:
     - `Column(Modifier.fillMaxWidth())`:
       - Fixed header: `Text(text = title, style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).semantics { heading() })`
       - `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)`
       - Scrollable body: `Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp))` containing `content()` plus optional `errorMessage?.let { Text(it, style = RecordType.Action, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.let { m -> if (errorTag != null) m.testTag(errorTag) else m }) }`.
       - `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)`
       - Pinned footer: `SheetCancelSaveRow(enabled = true, saving = false, saveEnabled = true, savingLabel = confirmLabel, onCancel = onDismiss, onSave = onConfirm, cancelTag = cancelTag, saveTag = confirmTag, spinnerTag = "${confirmTag}Spinner", modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))`.

2. **`SecurityLabeledField`**:
   - Wraps `Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp))`:
     - `Box(modifier = Modifier.clearAndSetSemantics {}) { if (isError) Text(label, style = RecordType.FieldLabel, color = MaterialTheme.colorScheme.error) else FormFieldLabel(label) }`
     - `OutlinedTextField(value = value, onValueChange = onValueChange, isError = isError, visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, singleLine = true, shape = RecordShapes.Small, textStyle = RecordType.FieldValue, colors = editorFieldColors(), modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }.testTag(tag))`
   - Keep `.testTag(tag)` directly on `OutlinedTextField` (NOT on the outer `Column`).

## 3. `SecuritySettingsDialog` (`ui/SecuritySettingsDialog.kt`, same signature)

1. **Dialog shell & pinned header**:
   - `val maxHeight = (LocalConfiguration.current.screenHeightDp - 32).dp`
   - `Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false))` containing `Surface(modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = maxHeight).testTag("securitySettingsDialog"), shape = RecordShapes.Large, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))`.
   - Top header row (`Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 8.dp)`):
     - Clinic emblem: `Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.size(44.dp))` with `Image(painterResource(R.drawable.clinic_logo_foreground), contentDescription = stringResource(R.string.clinic_logo_cd), modifier = Modifier.size(38.dp))` (zero `shadowElevation`).
     - Title + subtitle (`Modifier.weight(1f)`): `Text(stringResource(R.string.security_settings_dialog_title), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })` + `Text(settingsState.clinicName, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)`.
     - Close button: `IconButton(onClick = onDismiss, modifier = Modifier.minimumTouchTarget().testTag("closeSettingsButton"))` (`Icons.Default.Close`, `contentDescription = stringResource(R.string.close)`).
2. **Unclipped tab switcher (`FlowRow` of `RecordFilterChip`)**:
   - Replace `PrimaryTabRow` with `FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp))`:
     - `RecordFilterChip(selected = selectedTab == SettingsTab.SECURITY, label = stringResource(R.string.settings_tab_security), onClick = { selectedTab = SettingsTab.SECURITY }, tag = "settingsTabSecurity", wrap = true, minTouchTarget = true)`
     - `RecordFilterChip(selected = selectedTab == SettingsTab.LAB_CATALOG, label = stringResource(R.string.settings_tab_lab_tests), onClick = { selectedTab = SettingsTab.LAB_CATALOG }, tag = "settingsTabLabCatalog", wrap = true, minTouchTarget = true)`
     - `RecordFilterChip(selected = selectedTab == SettingsTab.MED_CATALOG, label = stringResource(R.string.settings_tab_medications), onClick = { selectedTab = SettingsTab.MED_CATALOG }, tag = "settingsTabMedCatalog", wrap = true, minTouchTarget = true)`
   - Directly below: `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)`.
3. **Scrollable tab body (`Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp))`)**:
   - `SettingsTab.LAB_CATALOG` -> `LabCatalogSettingsSection(catalogDelegate = catalogRepo)`
   - `SettingsTab.MED_CATALOG` -> `MedicationCatalogSettingsSection(catalogDelegate = catalogRepo)`
   - `SettingsTab.SECURITY` -> wrap in `CompositionLocalProvider(LocalFieldFamily provides FieldFamilies.patient())`:
     - **Theme (`themeSettingsCard`)**: `RecordSectionCard(title = stringResource(R.string.security_theme_title), family = patientFamily, modifier = Modifier.testTag("themeSettingsCard"))`. Subtitle in `RecordType.Secondary` (`onSurfaceVariant`). Each `AppThemeMode` option row uses `Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RecordShapes.Small).clickable { ... }.padding(horizontal = 4.dp, vertical = 4.dp).testTag("themeOption-${mode.name}")`, `RadioButton(...)`, and `Text(stringResource(mode.labelRes()), style = if (isSelected) RecordType.KeyValue else RecordType.FieldValue, color = if (isSelected) primary else onSurface)`.
     - **Clinic branding**: `RecordSectionCard(title = stringResource(R.string.security_branding_title), family = patientFamily)`. Emblem row (`48.dp` `CircleShape`, `1.dp` `outlineVariant` border, zero shadow) + `security_emblem_title` (`RecordType.CardName`) + `security_emblem_desc` (`RecordType.Secondary`). Two `SecurityLabeledField`s (`clinicNameField`, `clinicSubtitleField`). Optional `brandingSavedMessage` (`RecordType.Secondary`, SemiBold, `primary`, `liveRegion = Polite`, `testTag("brandingSavedMessage")`). Full-width `Button(shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("saveBrandingButton"))`.
     - **6-digit PIN**: `RecordSectionCard(title = stringResource(R.string.security_pin_section_title), family = patientFamily)` + `security_pin_section_desc` (`RecordType.Secondary`) + full-width `OutlinedButton(shape = RecordShapes.Small, border = BorderStroke(1.dp, outline), modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("changePinButton"))`.
     - **Biometric unlock**: `RecordSectionCard(title = stringResource(R.string.security_biometric_title), family = patientFamily)`. Switch row `Modifier.fillMaxWidth().heightIn(min = 56.dp)` with `security_biometric_switch_label` (`RecordType.CardName`), status text (`security_biometric_ready` / `security_biometric_unavailable`, `RecordType.Secondary`, `primary` or `error`), and `Switch(testTag = "biometricSwitch")`. Description (`RecordType.Secondary`). When `isBiometricAvailable && activity != null`, full-width `OutlinedButton(shape = RecordShapes.Small, border = BorderStroke(1.dp, outline), modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("testBiometricButton"))`. Feedback message (`RecordType.Secondary`, SemiBold, colored `error` when verification failed and `primary` when verified, `testTag("biometricFeedbackMessage")`).
     - **Auto-Lock Timeout**: `RecordSectionCard(title = stringResource(R.string.security_auto_lock_title), family = patientFamily)` containing `Text(stringResource(R.string.security_auto_lock_body), style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant)`.
     - **Vault backup and restore (`backupVaultSettingsCard`)**: `RecordSectionCard(title = stringResource(R.string.security_backup_title), family = patientFamily, modifier = Modifier.testTag("backupVaultSettingsCard"))`. Description (`RecordType.Secondary`). When `backupStatusMessage != null`, `Surface(shape = RecordShapes.Small, color = if (isBackupError) errorContainer else patientFamily.container, border = BorderStroke(1.dp, if (isBackupError) error else outlineVariant), modifier = Modifier.fillMaxWidth().testTag("backupStatusMessage"))` with `Text(..., style = RecordType.Secondary, fontWeight = FontWeight.SemiBold, color = if (isBackupError) onErrorContainer else onSurface)`. Action buttons: `val narrowStacked = LocalDensity.current.fontScale > 1.3f`; `Row(spacedBy(8.dp))` at `<= 1.3f` or `Column(spacedBy(8.dp))` above `1.3f` holding `Button` (`exportBackupButton`, `RecordShapes.Small`, `minimumTouchTarget()`, `maxLines = 2`, no decorative icon) and `OutlinedButton` (`restoreBackupButton`, `RecordShapes.Small`, `BorderStroke(1.dp, outline)`, `minimumTouchTarget()`, `maxLines = 2`, no decorative icon).
     - **Lock app now**: full-width `Button(shape = RecordShapes.Small, modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("lockAppNowButton"))` with `Text(stringResource(R.string.security_lock_app_now), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center)` (no decorative icon).
4. **Sub-dialogs in `SecuritySettingsDialog.kt`**:
   - **`ChangePinDialog`**: `SecurityFormDialog(title = stringResource(R.string.security_set_pin_title), confirmLabel = stringResource(R.string.security_save_pin), confirmTag = "saveNewPinButton", cancelTag = "cancelChangePinButton", dialogTag = "changePinDialog", errorMessage = errorMessage, errorTag = "changePinError", ...)` containing `pin_rule` (`RecordType.Secondary`), `SecurityLabeledField(..., tag = "newPinField", isError = errorMessage != null && !isValidNewPin(newPin), ...)`, and `SecurityLabeledField(..., tag = "confirmPinField", isError = errorMessage != null && isValidNewPin(newPin) && newPin != confirmPin, ...)`.
   - **`ExportBackupDialog`**: `SecurityFormDialog(title = stringResource(R.string.security_export_dialog_title), confirmLabel = stringResource(R.string.security_export_backup), confirmTag = "confirmExportBackupButton", cancelTag = "cancelExportBackupButton", dialogTag = "exportBackupDialog", errorMessage = errorMessage, errorTag = "exportBackupError", ...)` containing `security_export_dialog_body` (`RecordType.Secondary`), `backupPassphraseField` (`isError = errorMessage != null && passphrase.length < 6`), and `confirmBackupPassphraseField` (`isError = errorMessage != null && passphrase.length >= 6 && passphrase != confirmPassphrase`).
   - **`restore_confirm_dialog`**: `AlertDialog(onDismissRequest = ..., shape = RecordShapes.Large, containerColor = MaterialTheme.colorScheme.surface, title = { Text(stringResource(R.string.security_restore_confirm_title), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.onSurface) }, text = { Text(stringResource(R.string.security_restore_confirm_body), style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) }, confirmButton = { TextButton(..., shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("confirmReplaceAllButton")) { Text(stringResource(R.string.security_restore_replace_all), style = RecordType.Action, color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(..., shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("cancelReplaceAllButton")) { Text(stringResource(R.string.cancel), style = RecordType.Action, color = MaterialTheme.colorScheme.primary) } }, modifier = Modifier.testTag("restore_confirm_dialog"))`.
   - **`RestorePassphraseDialog`**: `SecurityFormDialog(title = stringResource(R.string.security_restore_dialog_title), confirmLabel = stringResource(R.string.security_decrypt_and_restore), confirmTag = "confirmRestoreBackupButton", cancelTag = "cancelRestorePassphraseButton", dialogTag = "restorePassphraseDialog", errorMessage = errorMessage, errorTag = "restorePassphraseError", ...)` containing `security_restore_dialog_body` (`RecordType.Secondary`) and `restorePassphraseField` (`isError = errorMessage != null`).

## 4. `AppLockScreen` (`ui/AppLockScreen.kt`, same signature)

1. **Container & scroll**: `BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("appLockScreen"))` with `val horizontalPadding = maxOf(16.dp, (maxWidth - 400.dp) / 2)`. Inner `Column` keeps `.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = horizontalPadding, vertical = 20.dp)`.
2. **Clinic identity**:
   - Emblem: `Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.size(72.dp))` with `Image(painterResource(R.drawable.clinic_logo_foreground), contentDescription = stringResource(R.string.clinic_logo_cd), modifier = Modifier.size(64.dp))` (zero shadow).
   - `settingsState.clinicName`: `RecordType.HeaderName` (`22.sp` Bold, `primary`, `.semantics { heading() }`).
   - `settingsState.clinicSubtitle`: `RecordType.Secondary` (`14.sp`, `onSurfaceVariant`).
3. **Prompt, rule, setup/confirm hints, and lockout banner**:
   - Title: `RecordType.SectionTitle` (`18.sp` Bold, `onSurface`, centered).
   - `if (setupMode)`: `stringResource(R.string.pin_rule)` in `RecordType.Secondary` (`onSurfaceVariant`).
   - `if (setupMode && !isConfirmingPin)`: `pinSetup` in `RecordType.Secondary` SemiBold, color `FieldFamilies.patient().label`.
   - `if (setupMode && isConfirmingPin)`: `pinConfirm` in `RecordType.Secondary` SemiBold, color `FieldFamilies.patient().label`.
   - `if (lockoutRemainingMs > 0)`: bordered `Surface(shape = RecordShapes.Small, color = if (dark) StatusHighDarkBg else StatusHighBg, border = BorderStroke(1.dp, if (dark) StatusHighDarkBorder else StatusHighBorder), modifier = Modifier.fillMaxWidth())` holding `Text(text = stringResource(R.string.pin_lockout, seconds / 60, seconds % 60), style = RecordType.Action, color = if (dark) StatusHighDarkText else StatusHighText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).testTag("pinLockoutMessage").semantics { liveRegion = LiveRegionMode.Polite })`.
4. **PIN dots (`pinDotsRow`, `pinDot-0..5`) & non-clipping error slot**:
   - Remove `animateFloatAsState` scale animation (MOTION 1).
   - Dot border: `error` when `errorMessage != null`, `primary` when `isFilled`, else `MaterialTheme.colorScheme.outline` (`>= 3.0:1` non-text contrast!). Dot fill: `error` or `primary` when `isFilled`, else `Color.Transparent`.
   - Error container: `Box(modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp), contentAlignment = Alignment.Center)` holding `errorMessage` in `RecordType.Action`, `color = MaterialTheme.colorScheme.error`, `liveRegion = LiveRegionMode.Polite`, `testTag("appLockErrorMessage")`.
5. **Keypad & biometric action**:
   - `val keypadEnabled = lockoutRemainingMs <= 0`
   - `KeypadButton(digit, enabled = keypadEnabled, modifier = Modifier.weight(1f))`: `Surface(onClick = onClick, enabled = enabled, shape = RecordShapes.Large, color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant), modifier = modifier.heightIn(min = 56.dp).minimumTouchTarget().testTag("pinKey-$digit"))` with `Text(digit, style = RecordType.HeaderName, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)`.
   - `pinKey-biometric` (same `if (isBiometricSupported && !setupMode && !legacyChangeRequired)` condition): `val bioEnabled = isBiometricSupported && settingsState.isBiometricEnabled && !setupMode && !legacyChangeRequired && lockoutRemainingMs <= 0`; `Surface(onClick = { triggerBiometric() }, enabled = bioEnabled, shape = RecordShapes.Large, color = if (bioEnabled) FieldFamilies.patient().container else MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, if (bioEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.weight(1f).heightIn(min = 56.dp).minimumTouchTarget().testTag("pinKey-biometric"))`.
   - `pinKey-backspace`: `Surface(onClick = { handleBackspace() }, enabled = keypadEnabled, shape = RecordShapes.Large, color = if (keypadEnabled) FieldFamilies.patient().container else MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, if (keypadEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.weight(1f).heightIn(min = 56.dp).minimumTouchTarget().testTag("pinKey-backspace"))`.
   - `useBiometricButton`: `TextButton(onClick = { triggerBiometric() }, enabled = keypadEnabled, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("useBiometricButton"))` with `Text(stringResource(R.string.security_unlock_with_fingerprint), style = RecordType.Action, color = if (keypadEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)`.

## 5. `VaultGateScreen` (`ui/VaultGateScreen.kt`, backward-compatible signature)

1. **Container & clinic identity**:
   - `BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("vaultGate"))` with `horizontalPadding = maxOf(16.dp, (maxWidth - 480.dp) / 2)`.
   - Emblem: `Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.size(80.dp))` with `Image(painterResource(R.drawable.clinic_logo_foreground), contentDescription = stringResource(R.string.clinic_logo_cd), modifier = Modifier.size(72.dp))`.
   - `clinicName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name)` in `RecordType.HeaderName`, `primary`, `.semantics { heading() }`.
   - `clinicSubtitle?.takeIf { it.isNotBlank() } ?: stringResource(R.string.clinic_default_subtitle)` in `RecordType.Secondary`, `onSurfaceVariant`.
2. **Status card (`vaultStatusCard`)**:
   - `val isBlocked = state is VaultState.Blocked`
   - `val dark = isDarkTheme()`
   - `val patientFamily = FieldFamilies.patient()`
   - `OutlinedCard(shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, if (isBlocked) (if (dark) StatusHighDarkBorder else StatusHighBorder) else MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().testTag("vaultStatusCard"))`:
     - Top band `Box(modifier = Modifier.fillMaxWidth().background(if (isBlocked) (if (dark) StatusHighDarkBg else StatusHighBg) else patientFamily.container).padding(horizontal = 14.dp, vertical = 8.dp))`:
       - `BandChip(text = stringResource(state.statusChipResource()), tone = if (isBlocked) StatusTone.High else null, family = patientFamily)`
     - Body `Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp))`:
       - `Text(text = stringResource(state.titleResource()), style = RecordType.SectionTitle, color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })`
       - `Text(text = stringResource(state.bodyResource()), style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant)`
3. **Action button (`when (state)`)**:
   - Exact same 3 active branches (`VaultState.Uninitialized` -> `initializeStorage`, `VaultState.Locked` -> `unlockStorage`, `is VaultState.Blocked` -> `retryStorage`; `else -> Unit`).
   - Style: `Button(onClick = ..., shape = RecordShapes.Small, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).minimumTouchTarget().testTag(...)) { Text(stringResource(...), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) }` (drops decorative button icons).

## 6. All existing `testTag`s kept (exhaustive list, 37 total)

- **`SecuritySettingsDialog.kt` (23)**: `securitySettingsDialog`, `closeSettingsButton`, `themeSettingsCard`, `themeOption-${mode.name}`, `clinicNameField`, `clinicSubtitleField`, `saveBrandingButton`, `changePinButton`, `biometricSwitch`, `testBiometricButton`, `backupVaultSettingsCard`, `exportBackupButton`, `restoreBackupButton`, `lockAppNowButton`, `restore_confirm_dialog`, `newPinField`, `confirmPinField`, `saveNewPinButton`, `backupPassphraseField`, `confirmBackupPassphraseField`, `confirmExportBackupButton`, `restorePassphraseField`, `confirmRestoreBackupButton`.
- **`AppLockScreen.kt` (10)**: `appLockScreen`, `pinLockoutMessage`, `pinSetup`, `pinConfirm`, `pinDotsRow`, `pinDot-$i` (`0..5`), `pinKey-biometric`, `pinKey-backspace`, `useBiometricButton`, `pinKey-$digit` (`0..9`).
- **`VaultGateScreen.kt` (4)**: `vaultGate`, `initializeStorage`, `unlockStorage`, `retryStorage`.
- **Additive new tags (optional, do not collide)**: `settingsTabSecurity`, `settingsTabLabCatalog`, `settingsTabMedCatalog`, `brandingSavedMessage`, `biometricFeedbackMessage`, `backupStatusMessage`, `changePinDialog`, `cancelChangePinButton`, `changePinError`, `exportBackupDialog`, `cancelExportBackupButton`, `exportBackupError`, `confirmReplaceAllButton`, `cancelReplaceAllButton`, `restorePassphraseDialog`, `cancelRestorePassphraseButton`, `restorePassphraseError`, `appLockErrorMessage`, `vaultStatusCard`.

## Acceptance checklist

1. All 37 existing `testTag` expressions remain on their exact semantic nodes (`OutlinedTextField` directly for all text inputs, outer `Surface` for `securitySettingsDialog`, `AlertDialog` for `restore_confirm_dialog`).
2. `git diff -w` shows zero changes to PIN validation/hashing, lockout intervals/timers, biometric prompt behavior, backup encryption/FileProvider/restore behavior, or `VaultState`/`VaultBlockReason` fail-closed logic.
3. Zero text nodes under `12.sp` across all three screens and all nested dialogs; zero hardcoded user-facing English strings; zero `20.dp`/`14.dp`/`12.dp`/`8.dp`/`28.dp`/`999.dp` radii (only `10.dp` `RecordShapes.Small`, `16.dp` `RecordShapes.Large`, and circular emblems/dots); zero `shadowElevation`; zero decorative section/button icons.
4. `settingsTabSecurity` (`"Security & clinic"`), `settingsTabLabCatalog` (`"Lab tests"`), and `settingsTabMedCatalog` (`"Medications"`) wrap cleanly without clipping inside `SecuritySettingsDialog` at `360.dp` (`1.0f`, `1.3f`, and `2.0f` font scale), and `exportBackupButton` / `restoreBackupButton` never clip at `1.0f`, `1.3f`, or `2.0f`.
5. Every interactive element across all 3 screens and 4 sub-dialogs measures `>= 48.dp` (`>= 47.9.dp` in instrumented checks).
6. `AppLockScreen` empty PIN dot borders and enabled keypad button borders use `outline` (`>= 3.0:1` non-text contrast), wrong PIN error (`"Incorrect PIN. Please try again."`) and multi-line setup errors never clip inside `errslot`, and lockout visibly disables keypad buttons.
7. `VaultGateScreen` renders cleanly at `320.dp` (`narrowPhone`), `360.dp` (`1.4f` font scale), `640x320` landscape, dark theme, and RTL, with `VaultState.Blocked` clearly marked by `StatusHigh` band and `"Blocked"` `BandChip`.
8. Dark theme (`AncTheme(darkTheme = true)`) and `LayoutDirection.Rtl` render cleanly with WCAG AA contrast verified across all pairs in `mock_s18_security_lock_vault.html`.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. `SecurityWorkflowTest` asserts exact strings `"Clinic & System Settings"`, `"Auto-Lock Timeout"`, `"Locks after 10 minutes away."`, `"Incorrect PIN. Please try again."`, and `"Dr. Sherif Radwan Clinic"`, all preserved verbatim; `CatalogSettingsRedesignTest` asserts `"Lab tests"` and `"Medications"`, preserved verbatim; `AncWorkflowTest` reads `VaultGateScreen` strings via `R.string.*`.

### Must pass unchanged

- `app/src/androidTest/java/com/sherifradwan/ancnative/SecurityWorkflowTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/CatalogSettingsRedesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/storage/VaultIntegrationTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/security/PinPolicyTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/security/AppLockPolicyTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/security/SecuritySettingsRepositoryTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/storage/EncryptedBackupTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/storage/BackupVaultIntegrationAndZeroDataLossTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/RecordTokensContrastTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt`

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/SecurityLockVaultRedesignTest.kt`

Mount `SecuritySettingsDialog`, `AppLockScreen`, and `VaultGateScreen` through `MainActivity` + `AncTheme` inside `360.dp x 640.dp` (and `320.dp x 640.dp` for `VaultGateScreen`) hosts (single `setContent` per test, using `ComposeTextAsserts`):
- **Typography & unclipped tabs (`1.0f`, `1.3f`, `2.0f`)**: walk semantics trees of `securitySettingsDialog`, `appLockScreen`, and `vaultGate`; assert zero `TextLayoutResult` nodes have `fontSize < 12.sp`; assert `"Security & clinic"`, `"Lab tests"`, `"Medications"`, `"Export backup"`, and `"Restore backup"` are not ellipsized or clipped (`assertAllTextWhole`) inside `360.dp` at `1.0f` and `1.3f` (and stacked cleanly without clipping at `2.0f`).
- **Minimum `48.dp` targets across all screens & sub-dialogs**: assert `closeSettingsButton`, `settingsTabSecurity`, `settingsTabLabCatalog`, `settingsTabMedCatalog`, `themeOption-SYSTEM`, `themeOption-LIGHT`, `themeOption-DARK`, `saveBrandingButton`, `changePinButton`, `exportBackupButton`, `restoreBackupButton`, `lockAppNowButton`, `saveNewPinButton`, `cancelChangePinButton`, `confirmExportBackupButton`, `cancelExportBackupButton`, `confirmReplaceAllButton`, `cancelReplaceAllButton`, `confirmRestoreBackupButton`, `cancelRestorePassphraseButton`, `pinKey-0..9`, `pinKey-backspace`, `initializeStorage`, `unlockStorage`, and `retryStorage` all measure `>= 47.9.dp` inside host bounds.
- **PIN setup, wrong PIN, and lockout states on `AppLockScreen`**: verify `pinSetup` / `pinConfirm` setup flow, weak PIN (`111111`) inline error (`pin_invalid`) not clipped vertically at `1.3f`, wrong PIN (`pin_incorrect`) error state, and active lockout (`pinLockoutMessage`) displaying countdown while `pinKey-1` / `pinKey-backspace` report disabled state (`assertIsNotEnabled()`).
- **PIN change, export validation, and destructive restore confirmation**: open `ChangePinDialog` via `changePinButton`, verify mismatch inline error (`security_pin_mismatch`) keeps dialog open; open `ExportBackupDialog` via `exportBackupButton`, verify short passphrase error (`security_passphrase_short`) keeps dialog open; verify `restore_confirm_dialog` renders `"Replace all patient records?"` heading + destructive `"Replace all"` and `"Cancel"`.
- **Vault gate states (`Uninitialized`, `Locked`, `Blocked`)**: verify `BandChip` state words (`"Setup needed"`, `"Locked"`, `"Blocked"`), fail-closed absence of patient UI, and `VaultBlockReason.KEY_PERMANENTLY_INVALIDATED` copy rendering unclipped at `320.dp` and `1.4f`.
- **Dark theme & RTL**: mount all three screens under `AncTheme(darkTheme = true)` and `LayoutDirection.Rtl` with zero overlap or horizontal overflow.

## Review requirement (mandatory for Level 3)

Because `SecuritySettingsDialog.kt`, `AppLockScreen.kt`, and `VaultGateScreen.kt` are **Level 3 (`auth_security`)** paths (`AGENTS.md`, `PLAN.md` 0.13):
1. Run a **frontier-tier review** verifying `git diff -w` against all invariants in the Level-3 section (zero changes to PIN policy, hashing, lockout, biometric gating, FileProvider export, destructive confirmation gating, restore paths, or Keystore/vault fail-closed handling).
2. Run an **independent test review** covering both existing security tests (`SecurityWorkflowTest`, `AncWorkflowTest`, unit tests) and the new `SecurityLockVaultRedesignTest`.

## Risk

**High (`auth_security`, Level 3).** Changing any PIN digit filter, validation branch, lockout guard, biometric launch condition, FileProvider path, or `VaultBlockReason` mapping creates a clinical security regression; wrapping `OutlinedTextField` in `SecurityLabeledField` without keeping `.testTag(...)` directly on `OutlinedTextField` breaks input semantics; modifying `"Clinic & System Settings"`, `"Auto-Lock Timeout"`, `"Locks after 10 minutes away."`, or `"Incorrect PIN. Please try again."` breaks `SecurityWorkflowTest`; adding a `LazyColumn` inside `SecuritySettingsDialog` crashes `CatalogSettingsSections` nested scrolling. Follow the Level-3 diff checklist line by line and route through frontier review plus independent test review before merge.
