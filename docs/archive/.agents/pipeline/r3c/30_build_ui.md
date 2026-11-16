ROLE: UI BUILDER. The core lane has already implemented PinPolicy, AppLockPolicy and the repository changes; read security/SecuritySettingsRepository.kt, security/PinPolicy.kt and security/AppLockPolicy.kt first and use their API as it is (do not edit them).
ALLOWED files:
- app/src/main/java/com/sherifradwan/ancnative/ui/AppLockScreen.kt   (Q29 a, b, d: setup mode with enter + confirm, legacy-PIN entry then forced change when it was 123456, lockout message with remaining time and entry disabled, biometric button only when available)
- app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt   (show the setup/lock screen before ANY patient data when no PIN is set up or the app is locked)
- app/src/main/java/com/sherifradwan/ancnative/ui/SecuritySettingsDialog.kt   (Q29 c: Change PIN uses isValidNewPin and shows the rule; no "Never" auto-lock option)
- app/src/main/res/values/strings.xml   (new user-facing text only)
Plain English, short sentences, no emoji. Keep every existing testTag; add testTags pinSetup, pinConfirm, pinLockoutMessage. Touch targets >= 48 dp for keypad keys.
Run ./gradlew --offline :app:compileDebugKotlin and the full unit suite; report real results.

## ADDENDUM (orchestrator, after the first review) - lock screen must follow live settings
Defect: AppLockScreen reads `securityRepo.settings.value` once (a snapshot). After validatePin changes the state (legacy default
PIN accepted -> needsPinSetup=true) the screen keeps the stale values (setupMode=false) and does not switch to the new-PIN
setup, so the clinician can be stuck re-entering the old PIN.
Required change (ui/AppLockScreen.kt only): collect the settings as Compose state
(`val settingsState by securityRepo.settings.collectAsState()`), and derive setupMode, legacyChangeRequired and
isBiometricEnabled from that live state so the screen switches to the PIN setup/confirm flow immediately after the old default
PIN is accepted. When the repository reports needsPinSetup after validatePin, clear enteredPin and start the enter + confirm
flow. Never call unlockApp() until setupPin succeeded. Keep all testTags and strings. Debug and release must compile.
