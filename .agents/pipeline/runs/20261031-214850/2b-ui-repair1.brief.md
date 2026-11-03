# Shared brief prefix (identical for every role; stable text first, role part last)
You are one lane in a MACA (multi-agent collaborative audit) fleet, ROUND 3z. An orchestrator planned the work and will verify it independently. Rules:
1. Work only from the written spec: .agents/pipeline/SPEC_FIX_R3.md (section "Round 3z": oracles Q120-Q125, S101-S102) and .agents/pipeline/OWNER_DECISIONS.md. Earlier oracles in SPEC_FIX.md and SPEC_FIX_R2.md still hold unless R3 supersedes them. If the spec is ambiguous choose the SAFE behaviour (never hide, drop, misfile or relabel clinical/administrative data; prefer leaving text in Plan) and write the assumption in your final message.
2. Touch ONLY the files in your ALLOWED list. Everything else is read-only. A change outside the list is reverted automatically and counted against you.
3. Do not run git commit, git push, git checkout, git stash or any command that deletes files. Do not edit build files (except app/build.gradle.kts when it is in your ALLOWED list), gradle properties, or any test file you do not own.
4. NEVER write dead code, extra XML/files or comments only to satisfy a static check or a test; if a check looks wrong, implement the spec and say so in your final message. REUSE PRINCIPLE: one function per rule; call the existing helper, never write a second copy. Simplest thing that meets the spec. No new libraries, no speculative abstractions, no refactors outside the scope. Do not change any clinical threshold, wording, or colour not named in the spec. Keep every existing testTag.
5. Do not read .agents/maca-run-*/, .agents/fleet-runs/, .agents/backup-*, .agents/pipeline/runs/ or .agents/pipeline/SCORECARD.md. Independence matters.
6. Final message (plain text, <= 25 lines): files you changed, what you did per oracle (Q/U number), assumptions, anything you could not do or verify. Never claim a test passed unless you ran it and saw it pass; say "not run" otherwise.
Project facts: Kotlin, Jetpack Compose, JUnit4 unit tests under app/src/test, packages com.sherifradwan.ancnative.{ui,ui.report,state,storage}. Unit tests: ./gradlew --offline :app:testDebugUnitTest --tests '<Class>'.
--- ROLE-SPECIFIC PART BELOW ---

ROLE: UI BUILDER for ROUND 3z. Implement Q120 (print date caller), Q121 (callers), Q122-Q125. ALLOWED files: app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt, ui/PatientEditor.kt, ui/VisitEditor.kt, ui/Components.kt, ui/ClinicalBadges.kt, ui/PatientListScreen.kt, ui/ProfileScreen.kt, ui/WorkspaceScreen.kt, ui/MedicationEditor.kt, ui/CodexSettingsSections.kt, security/BiometricAuthManager.kt, app/src/main/res/values/strings.xml.
Core already added formatIsoDate, SaveUiError?.stringRes(), formatFetalMetrics and COMMON_MEDICATION_FREQUENCIES: call them and DELETE the copies named in the spec. Behaviour and look identical except the two noted differences; keep every testTag and text. Before deleting EditorField's twin (VisitField) compare their parameters, modifiers, semantics and error styling and keep the superset; say in your final message what you merged. Do not edit tests or checks. Run ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin :app:compileDebugAndroidTestKotlin, the whole unit suite, ./gradlew --offline :app:lintDebug (0 errors), python3 .agents/checks/check_r3z.py. Report real results.


ALLOWED (machine-enforced): app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt, app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt, app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt, app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalBadges.kt, app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt, app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt, app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt, app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt, app/src/main/java/com/sherifradwan/ancnative/ui/CodexSettingsSections.kt, app/src/main/java/com/sherifradwan/ancnative/security/BiometricAuthManager.kt, app/src/main/res/values/strings.xml

REPAIR ROUND: these checks still fail.
FAIL S8 SDK_INT<30 guard before BiometricManager use: no guard before first use
PASS S9 no 123456 / DEFAULT_PIN
PASS S10 repository uses PinPolicy for v2
PASS S11 MainActivity calls shouldLockOnReturn
PASS S12 AutoLockTimeout has no NEVER
PASS S13 SHA-256 only in legacy check function
PASS S14 persisted pin-change-required flag
PASS S15 lock screen observes settings flow
PASS S16 dialogs hidden while locked / PIN setup

PASS S99 no values-ar folder, no Locale("ar") UI-string test, check_r3w.py without values-ar
FAIL S100 biometric API 28/30 guards, splash items only in values-v31, AppLockScreen without context.getString: BiometricAuthManager.kt has no SDK_INT / @RequiresApi guard for API 28; BiometricAuthManager.kt has no SDK_INT / @RequiresApi guard for API 30

FAIL S101 one definition per rule; old copies gone: ClinicalEditorErrorCard still exists
PASS S102 biometric API gate is one constant; unused draft-policy strings removed

rmediate_text_report/debug/lintReportDebug/lint-results-debug.txt
31 actionable tasks: 8 executed, 23 up-to-date

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:lintDebug'.
> Lint found errors in the project; aborting build.
  
  Fix the issues identified by lint, or create a baseline to see only new errors.
  To create a baseline, run `gradlew updateLintBaseline` after adding the following to the module's build.gradle file:
  ```
  android {
      lint {
          baseline = file("lint-baseline.xml")
      }
  }
  ```
  For more details, see https://developer.android.com/studio/write/lint#snapshot
  
  Lint found 12 errors, 101 warnings. First failure:
  
  /Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/security/BiometricAuthManager.kt:35: Error: Call requires API level 28 (current min is 26): android.hardware.biometrics.BiometricPrompt.Builder() [NewApi]
              val builder = BiometricPrompt.Builder(activity)
                                            ~~~~~~~
  
  The full lint text report is located at:
    /Users/sherifgamalradwan/anc-emr-android-native/app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 29s
