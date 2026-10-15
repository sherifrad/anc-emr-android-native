# P1 validation — 2026-10-09

Status: **P1 engineering verification PASS**. This is the current validation record and supersedes the earlier pending note and external execution summary. Owner visual, clinical, and release acceptance remain separate.

## Artifact

- Package: `com.sherifradwan.ancnative.dev`
- Version: `0.1-p1`, versionCode `1`, minSdk `26`, targetSdk `36`
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- SHA-256: `2d05817f6ad99658c48f6b1bf33d15d8250d2d7903a3a0832f79c205b13264c5`
- Debug APK signature verifies with APK Signature Scheme v2. This is not a release-signed delivery.

## Automated verification

The final unchanged gates ran with JDK 17, the pinned toolchain, and disposable API 36 emulator `anc-native-p1-20261008` / `emulator-5580`:

```sh
./gradlew --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest
```

| Gate | Result |
|---|---|
| Unit tests | 9 passed; 0 failed, errored, or skipped |
| Native instrumented workflows | 3 passed; 0 failed, errored, or skipped |
| Lint | 0 errors; 6 warnings for pinned versions and unused resources |
| Debug assembly | BUILD SUCCESSFUL |
| APK signature | Verifies |
| Compiled exports | Only `com.sherifradwan.ancnative.MainActivity` exported |

The instrumented workflows cover visit save and read-only Profile consistency, dirty Back keep/discard, and activity recreation retaining an in-memory draft. State tests also cover invalid dates, stale edits, duplicate saves, same-patient navigation, and cross-patient isolation. Test assertions and lint checks were retained.

Authoritative generated records: `evidence/p1/final-gates.log`, `verification.json`, `compiled-manifest.xml`, and `apk-signature.log`. Kotlin/UI source and resources are byte-identical across the layout-tested APK and the final manifest-corrected APK (48 ZIP entries compared). Gradle reused the identical APK when the final merger marker produced the same compiled manifest; the source marker can therefore have a later timestamp than the APK.

## Native layout and lifecycle proof

Seven modes passed on the offline disposable emulator: small phone, 200% font scaling, landscape at 200% font scaling, tablet portrait, tablet landscape, dark phone, and Arabic/RTL phone. Native screenshots and package-attributed UI hierarchy XML are saved under `evidence/p1/`; `layout-matrix.json` records banner visibility and reachability of Add visit and Save in every mode.

The final dark-mode correction paints the theme background across the whole window while applying safe drawing insets to the content. Status and navigation icons are readable in the inspected light/dark captures.

A saved synthetic visit note was verified before backgrounding the app. Android then killed the background process with `am kill`; the process was confirmed absent, and relaunch produced a different PID, the initial patient-list route, and the original fixtures without that saved note. No force-stop was used for the recreation step. Evidence: `process-death.json`, `process-death-before.png`, `process-death-reopened.png`, and `process-death-after.png` with matching hierarchy XML.

No app crash or ANR was found in the instrumented workflow logs. The earlier usage-limit/ADB verification gap is closed. No physical device or existing clinical record was used.

## Compiled manifest correction

The APK requests no Android internet or storage permission, disables automatic backup, and includes the approved backup and extraction rules. Its startup profile initializer remains registered.

Library-added preview/test activities are internal in the debug variant. The debug manifest uses a bare receiver `tools:node="removeAll"` marker: the audited original APK had exactly one receiver, the unused profile-installer broadcast receiver, and the main manifest had none. The final APK has zero receivers and only its launcher exported. Main/release manifest and dependencies remain unchanged.

Named receiver declarations/removals triggered a source-lint classpath false positive; combining removeAll with a selector was rejected by this AGP. Those diagnostic attempts are preserved in `manifest-attempt-failed.log`, `named-removal-attempt-failed.log`, and `selector-attempt-failed.log`. The accepted rule uses no suppression or dependency change. It follows Android's [manifest merge rules](https://developer.android.com/build/manage-manifests), with final behavior checked in the compiled APK.

External on-device previews and shell-driven profile receiver tooling are unavailable in this debug prototype. Revisit the debug-wide removal rule before adding any app receiver or adding/upgrading libraries. Sanitized public AGY consultation approved this bounded correction at high reasoning; it did not inspect private source or execute the verification.

## Scope and next gate

The source/config audit matches all 35 approved files. Both existing ANC reference repositories remain unchanged. No Git initialization, commit, push, branch change, or production signing was performed.

P1 remains a synthetic in-memory prototype. P2 secure persistence/key lifecycle/recovery and later clinical calculations require their own bounded, independently reviewed briefs and the prescribed reasoning gates. This record does not authorize real patient use or release.
