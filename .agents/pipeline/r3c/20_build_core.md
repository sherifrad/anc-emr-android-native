ROLE: CORE LOGIC BUILDER. Make the tests pass without editing them (tests are owned by another agent and are the contract).
ALLOWED files (under app/src/main/java/com/sherifradwan/ancnative/):
- security/PinPolicy.kt (NEW, Q25)   - security/AppLockPolicy.kt (NEW, Q26)
- security/SecuritySettingsRepository.kt   (Q27 a-e, g, h: v2 PIN storage, legacy migration, cold-start lock, persisted lockout, AutoLockTimeout without NEVER. Expose what the lock/setup screens need as small public functions or state fields: e.g. needsPinSetup, legacy-PIN flag, lockout remaining ms, setupPin(pin), and keep validatePin/updatePin working for existing callers.)
- security/BiometricAuthManager.kt   (Q28 only)
- MainActivity.kt   (Q27 f: onStop/onStart lifecycle hook only)
Never weaken existing checks. Keep SharedPreferences (no new storage library). Minimal, readable changes in the surrounding style. Write in your final message the exact API you exposed for the UI lane (names, types, meaning) - the UI builder reads it.
Run: ./gradlew --offline :app:testDebugUnitTest --tests '*PinPolicyTest' --tests '*AppLockPolicyTest', python3 .agents/checks/check_r3c.py, ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin. Report real results.

## ADDENDUM (orchestrator, after the first review) - legacy default PIN must stay forced
Defect found by the reviewer and confirmed by the orchestrator's reading of SecuritySettingsRepository.validatePin:
when the OLD stored PIN is the old default (the six digits 1-2-3-4-5-6, the PIN every existing install has), validatePin
currently calls storeV2(pin) and REMOVES the legacy key. After a process restart loadSettings sees only the v2 key, so
needsPinSetup=false and the old default PIN keeps working forever. Q27c requires a new PIN to be set before unlocking.
Required change (SecuritySettingsRepository.kt only):
- In the legacy branch of validatePin, when the PIN is valid AND equals the old default: do NOT call storeV2 and do NOT remove
  the legacy key. Only set state needsPinSetup=true and isLegacyPin=true (and keep isAppLocked=true). Return true.
- When the legacy PIN is valid and is NOT the old default: migrate as today (storeV2, remove legacy key).
- updatePin/setupPin (storeV2) already removes the legacy key: that is the only moment the legacy default is retired.
- After a restart with the legacy key still present, loadSettings must again report isLegacyPin=true so the screen asks for a
  new PIN once the old one is entered. Failure counting and lockout still apply to the legacy branch.
- Do not write the literal six-digit default as a named constant (static check S9); keep building it the way the file does now.
Keep every other behaviour. Debug and release must compile; PinPolicy/AppLockPolicy tests stay green.
