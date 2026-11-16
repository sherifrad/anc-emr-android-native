# P1 — Native Android ANC synthetic prototype

Fallback trigger: OpenCode 1.18.30, `opencode models deepseek`, exit 1, `Provider not found: deepseek`, verified again 2026-10-08. Codex-authored bounded brief under owner Article I.4; requires fresh read-only AGY PASS before implementation.

Owner instruction: start implementing the previously reviewed native Kotlin ANC plan, with up to three hours of autonomous work. Owner confirmed new folder `anc-emr-android-native` and prototype defaults: application ID `com.sherifradwan.ancnative.dev`, minSdk 26, defer existing-record import. The project is the empty directory `/Users/sherifgamalradwan/anc-emr-android-native`. Existing web and Capacitor repositories are reference-only.

## Objective

Deliver and verify the P1 synthetic in-memory Android app: a polished clinical patient list, patient Workspace and read-only Profile, a single complete synthetic visit create/edit/cancel/save workflow, and native Back/draft handling. Preserve navy/rose/ivory identity and clinical information semantics. Clearly label every surface `SYNTHETIC DEMO DATA — NOT FOR CLINICAL USE`. P1 is not secure storage, a clinical MVP, or a production release.

## Exact source/config paths permitted

All below are relative to `/Users/sherifgamalradwan/anc-emr-android-native`:

```text
.gitignore
README.md
settings.gradle.kts
build.gradle.kts
gradle.properties
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
local.properties
app/build.gradle.kts
app/proguard-rules.pro
app/src/debug/AndroidManifest.xml
app/src/main/AndroidManifest.xml
app/src/main/java/com/sherifradwan/ancnative/MainActivity.kt
app/src/main/java/com/sherifradwan/ancnative/model/DemoModels.kt
app/src/main/java/com/sherifradwan/ancnative/model/DemoFixtures.kt
app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt
app/src/main/java/com/sherifradwan/ancnative/state/AncViewModel.kt
app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt
app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt
app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt
app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt
app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt
app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt
app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt
app/src/main/res/values/strings.xml
app/src/main/res/values-ar/strings.xml
app/src/main/res/values/themes.xml
app/src/main/res/xml/backup_rules.xml
app/src/main/res/xml/data_extraction_rules.xml
app/src/test/java/com/sherifradwan/ancnative/state/AncStateTest.kt
app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt
docs/IMPLEMENTATION_PLAN.md
docs/VALIDATION.md
```

Allowed generated paths: `.gradle/`, `.kotlin/`, `build/`, `app/build/`, and `evidence/p1/` build logs/screenshots/verification JSON. No implementation outside these paths. No git init, commits, branch changes, pushes, dependencies outside the baseline, or edits to any existing ANC repository. Orchestrator may save plan/review artifacts in its visualization output and `/private/tmp/anc-native-p1-*`.

## Bounded baseline dependencies and toolchain

Use installed JDK17, SDK `/opt/homebrew/share/android-commandlinetools`, compile/target SDK36, build tools35.0.0, cached Gradle8.13, AGP8.13.2, Kotlin Android/Compose plugins2.2.21, Compose BOM2025.12.01, Compose UI/tooling-preview/Material3, activity-compose1.11.0, lifecycle-viewmodel-compose and lifecycle-runtime-compose2.9.4. Unit tests JUnit4.13.2; Android tests runner1.7.0, test.ext:junit1.3.0, Compose ui-test-junit4 and debug ui-test-manifest via BOM. No Room, DataStore, crypto, KMP, Hilt, navigation library, analytics, cloud, or network permissions. Pin versions. Generate wrapper with the installed Gradle distribution; do not invent/download wrapper JARs from unknown sources. Check official compatibility documentation already consulted: https://developer.android.com/build/releases/agp-8-13-0-release-notes .

## Requirements and constraints

- Kotlin data classes/sealed states; immutable app state and pure reducer/transitions; screen ViewModel exposes StateFlow. Simple in-memory repository/state source. No clinical arithmetic; dates are recorded strings validated with LocalDate only as calendar validation.
- Three obviously synthetic fixture identities with stable UUIDs, including an Arabic name. Mark all case IDs DEMO; no real records or imports. Runtime-created demo visits may get UUIDs. No patient-creation persistence: New Patient shows an honest P1 placeholder/explanation.
- Patient list has search, clear Workspace and Profile actions, readable demographics/Case ID, and no speculative clinical KPIs/risk inference.
- Workspace shows patient identity and ten reference sections: Obstetric History, Dating, Other Histories, Episode Overview, Visits, Ultrasound, Laboratory, Medications / Prescriptions, Delivery, Attachments. Only Visits is interactive in P1; other tabs show explicit prototype placeholders, not invented normal/negative clinical assertions. Native scrollable selector or adaptive layout; do not squeeze 10 bottom-nav items.
- Visits has newest-first display, create/edit dialog or screen, labelled date/BP/weight/findings/notes fields, validation feedback, Save and Cancel. Successful save updates the same patient's Workspace and Profile; invalid input/dismissal never partially saves.
- Read-only Profile displays factual synthetic identity and visits from the same state source. No edit controls. Absent values say Not recorded; no negative assertions or dummy zeros. No automatic diagnosis, episode lifecycle, redating, or prescription-state coupling.
- Draft state survives rotation in memory via ViewModel. Back/cancel/navigation with dirty visit asks Discard/Keep editing; discard leaves saved records unchanged. Switching patient cannot leak or apply another patient's draft. Stale visit revisions reject save and retain draft; saving an editor twice cannot duplicate a visit.
- Process death resets demo-only state. No patient or draft serialization in SavedStateHandle, rememberSaveable, Bundles, files, preferences, or logs. No fake PIN/secure-storage claims. Lifecycle-aware collection. Native BackHandler only for relevant nested/dirty routes.
- Demo banner stays on all screens/dialogs. Mark placeholders and synthetic saved edits clearly. No production release/install to physical devices.
- Theme tokens navy#0D1B3D, ivory#FBF8F5, white, rose accent#D49AA0 with a darker accessible actionable rose, teal facts; custom Material3. System sans/Arabic fallback, 48dp controls, scalable text, visible labels and focus, no emoji icons, no raw ad hoc colors per screen. English/Arabic string resources and supportsRtl; English initial UI with no network fonts. Phone/tablet and light/dark adapted theme; content remains scrollable at large fonts and landscape, respects IME/system insets.
- Manifest disables automatic backup, excludes all private storage via pre31 backup and 31+ extraction rules for cloud/D2D, no INTERNET permission, and distinct demo app label. No exported components except launcher. No storage permissions.
- README states run commands, toolchain pins, demo limitations, process-death behavior, no clinical/release acceptance; documents P2/P3 remain future. Validation reports actual checks, including UNKNOWN / HARNESS ERROR where required; never fabricate screenshots or weaken assertions.

## Acceptance and harness

1. App compiles as a real Kotlin/Compose Android APK with distinct application ID; unit and lint gates pass.
2. Meaningful state tests cover save/cancel, cross-patient isolation, invalid calendar dates, stale revisions, double save, Profile consistency, and Back/draft confirmation.
3. Instrumented tests cover patient->Workspace->visit save->Profile, dirty Back discard/keep, read-only Profile, and rotation retention of synthetic draft.
4. Disposable emulator only: offline launch, absence of app/page crashes, phone/tablet layouts, large fonts, RTL, light/dark, native Back, and process recreation reset. Inspect native screenshots and logcat attributed to this package; record gaps honestly.
5. Existing ANC repositories and device records untouched. Stop after P1 acceptance; P2 requires its own approved exact-path brief and xhigh reasoning. Three hours is a ceiling, not permission to skip P1 gates or introduce unapproved clinical/storage work.

Exact build commands from target root with `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`:

```sh
./gradlew --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew --offline :app:connectedDebugAndroidTest
```

If cache gaps block resolution, fetch only these pinned baseline artifacts from official Google/Maven/Gradle repositories using the same commands without `--offline`, then rerun the unchanged gate. SDK/emulator controls may require sandbox escalation. No physical-device commands, no uninstall/clear of existing packages.

## AGY gate

Fresh independent AGY PASS received 2026-10-08, project `9b6e08dc-fdfe-4a9c-90d4-cabe8af3137f`, conversation `3114aaaf-a9b1-4789-906f-beb24e26a103`. `[Codex Reasoning Level: high]` — Core Kotlin contracts, pure reducers, draft conflicts, double-save prevention, patient isolation and instrumentation warrant high; crypto/Room persistence and gestational algorithms remain excluded and reserved for xhigh. Relay completed with zero touched reference files. User already authorized execution and the prototype defaults; no additional confirmation is required for this bounded slice.

## P1 debug exported-component acceptance amendment — 2026-10-09

OpenCode fallback was reconfirmed for this amendment: `opencode models deepseek` exited 1 with `Provider not found: deepseek`. The owner authorized the new exact path `app/src/debug/AndroidManifest.xml` and this documentation update only. Independent fresh read-only AGY review of the sanitized public manifest-merger proposal returned `PASS`, project `c43b7260-971d-4a49-8e60-81bbf69ef078`, conversation `4a707d3b-e695-4b4b-99bc-c241319db06c`. `[Codex Reasoning Level: high]`. AGY reviewed the public design only; it did not inspect private repository source or run builds. The approved merger design is supported by https://developer.android.com/build/manage-manifests .

The two activity overrides for the library-provided `androidx.compose.ui.tooling.PreviewActivity` and `androidx.activity.ComponentActivity` remain non-exported and unchanged. The current instrumentation flow explicitly launches the app's own `MainActivity`; those Compose tests remain unchanged.

### Final ProfileInstallReceiver correction

Deterministic lint showed that the runtime-only `androidx.profileinstaller.ProfileInstallReceiver` class is absent from the compile classpath, although the class exists in the APK. Two named approaches are retained as failed history: the non-exported merge declaration and `<receiver android:name="androidx.profileinstaller.ProfileInstallReceiver" tools:node="remove" />` each left a raw app-source class attribute that triggered lint `MissingClass`. The subsequent `<receiver tools:node="removeAll" tools:selector="androidx.profileinstaller" />` combination was also rejected because the installed AGP does not accept `removeAll` with `tools:selector`. No dependency change or lint suppression is authorized.

Fresh independent AGY sanitized review returned `PASS`, project `011a706a-1642-4733-805a-48148a3704f3`, conversation `9b5e5ecb-6230-436b-83a9-2e7d88609587`. `[Codex Reasoning Level: high]`. AGY reviewed only the public design; it did not inspect repository source or run tests. OpenCode fallback remains definitive: `Provider not found: deepseek`.

The final approved rule is **DEBUG-WIDE**: the debug manifest keeps both activity entries exported false and unchanged, and uses exactly `<receiver tools:node="removeAll" />`, with no `android:name`, `tools:selector`, or other receiver attribute. The original compiled debug APK contains exactly one receiver, `ProfileInstallReceiver`, while the main manifest contains none; in this current pinned inventory the bare marker therefore removes exactly that unused debug registration. The profileinstaller runtime library and startup `ProfileInstallerInitializer` remain present, and the main/release manifest, dependencies, application code, and release behavior remain unchanged. This is not a broad permanent safety claim: revisit and remove this debug override before adding any app receiver or adding or upgrading libraries.

Operational tradeoff: receiver-driven profile tooling remains unavailable for this debug prototype. Automatic startup profile installation through `ProfileInstallerInitializer` is retained.

Orchestrator verification completed on 2026-10-09 and passed. The current artifact digest, evidence, and limitations are recorded in `docs/VALIDATION.md` and `evidence/p1/verification.json`. The preserved acceptance commands are:

```sh
./gradlew --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew --offline :app:connectedDebugAndroidTest
```

Connected tests ran only on disposable emulator `emulator-5580`. The compiled APK exports only `com.sherifradwan.ancnative.MainActivity`, omits the `ProfileInstallReceiver` registration, retains `ProfileInstallerInitializer`, and passed all 9 unit tests, lint with zero errors, debug assembly, and all 3 `MainActivity` instrumented workflows. Seven native layout modes and actual background process-death reset were verified. P1 engineering verification is complete; owner visual, clinical, and release acceptance remain separate.
