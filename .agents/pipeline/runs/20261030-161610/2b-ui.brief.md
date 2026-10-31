# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3c. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3c": oracles Q25-Q29, S8-S13, Gates 3c) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

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


ALLOWED (machine-enforced): app/src/main/java/com/sherifradwan/ancnative/ui/AppLockScreen.kt, app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt, app/src/main/java/com/sherifradwan/ancnative/ui/SecuritySettingsDialog.kt, app/src/main/res/values/strings.xml
