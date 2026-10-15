# P2 — implementation and validation status, amended 2026-10-10

## FINAL engineering acceptance — 2026-10-10

**PASS — scoped P2 engineering acceptance.** The final adjudication is [honor-engineering-adjudication.json](../evidence/p2/honor-engineering-adjudication.json). It qualifies the reviewed synthetic P2 encrypted-storage and native accessible-UI scope on the exact API36 target, with `minSdk` 35 and `targetSdk` 36; no other Android-version coverage is required.

| Current evidence | Final status |
|---|---|
| Qualified APKs | App SHA-256 `42f5a59db69074b3175f8d30df9a85dc480c8861788558c90e1e412b2788e331`; test SHA-256 `cc1538685dbe6aa6d61873879b2b7b44bf64725d6f6635550db446f01e76715d` |
| Connected and host gates | **PASS** — current connected run: 23 tests, 0 failures, 0 errors, 0 skipped; lint: 0 errors |
| Runtime/security qualification | **PASS** — actual real credential, persistence, rotation, and ciphertext evidence; 21 core checks; six native presentation-fixture cases; five actual Honor-sized gate configurations; and the fresh API36 no-credential gate against the current APK. Canonical hashes remain preserved. |
| Raw main runtime verifier | **UNKNOWN solely for its external UI-layout matrix.** Additional native-fixture and current-APK actual hierarchy evidence resolves that gate in the separate final adjudication. The raw script is not claimed as `PASS`, and historical failed logs remain unchanged. |
| Physical phone scope | **Cold-start only PASS.** No saved-phone workflow, full-app, clinical, or release acceptance is claimed. |
| Remaining optional evidence | Protected-pixel capture and 16 KB runtime remain **UNKNOWN / OPTIONAL**. |
| Whole-app and follow-on status | Patient workflow remains a placeholder and schema2 has not begun. The Owner-approved P3 condition requiring P2 engineering acceptance is now met; this is not P3 implementation or acceptance. |

## HISTORICAL PRIOR REPAIR status — superseded by the final adjudication above

**Historical record at the prior repair stage:** The bounded HONOR400 compatibility amendment was prepared; P2 engineering acceptance had not yet been granted. Current-build host gates plus exact API36 real-credential, ciphertext, lifecycle, instrumentation, and accessible-layout evidence were still required at that time.

The following table is preserved as the **HISTORICAL PRIOR REPAIR** snapshot and does not override the current final acceptance block above.

| Gate | Actual status |
|---|---|
| Designated OpenCode planner | Unavailable: 1.18.30, `opencode models deepseek` exit 1, `Provider not found: deepseek` |
| First independent AGY review | AMEND, xhigh |
| Fresh amended-plan AGY review | PASS, xhigh |
| HONOR400 compatibility amendment review | **PASS, xhigh** — project `d825f981-f84d-4468-b2aa-57345e74c8fe`, conversation `a33a5854-300a-41c5-9b81-f317dbc754ce` |
| Initial-setup cancellation clarification | **PASS, xhigh** — project `687f2174-4642-41c7-95ac-26d52de791c0`, conversation `a6f5a8eb-8a09-4b6b-9136-c7b16b0a64ff` |
| Owner dependency/plugin approval | **ACCEPTED** — Owner said “start now” after the pins were presented |
| Owner schema v1 approval | **ACCEPTED** — initial four-table schema definitions/constraints approved; compiler export is an engineering comparison, not a new owner prompt |
| Owner synthetic device-bound limitation | **ACCEPTED** — synthetic-only, no recovery/export/reset, device/key loss can be unrecoverable |
| Pinned dependency compatibility assembly | **PASS** — `evidence/p2/compatibility-online.log` ends `BUILD SUCCESSFUL`, 38 tasks |
| First host unit/lint/assembly gate | **PASS BEFORE THIS REPAIR** — `evidence/p2/host-gates-first.log`; it does not validate the repaired source |
| P2 source/config/test/verifier repair | **PREPARED / CURRENT AMENDMENT GRADLE UNKNOWN** — amendment limited to the four owner-approved paths |
| Focused pure Kotlin compile | **PASS** — Kotlin 2.2.21 compiler, domain/reducer/vault-state/save-coordinator subset |
| Focused Android storage compile | **PASS POST-REPAIR** — Kotlin 2.2.21 compiler against API36 plus pinned Room/SQLite/SQLCipher artifacts; this does not replace KSP/Gradle |
| Focused MainActivity / ViewModel compile | **PASS POST-REPAIR** — Kotlin 2.2.21 and Compose compiler plugin against the pinned cached Android/AndroidX artifacts |
| Focused storage instrumentation-source compile | **PASS POST-REPAIR** — both approved storage androidTest files compiled against API36/AndroidX Test; not executed |
| Focused pure JUnit | **PASS** — 15 tests (`AncStateTest`, `VaultStateTest`, `SaveCoordinatorTest`) |
| XML parsing and English/Arabic string-key parity | **PASS** — local non-Gradle checks, no missing keys |
| `tools/verify_p2.py` Python syntax | **PASS** — compiled in memory; no `__pycache__` written |
| Compiler schema1 export and hash | **PRESENT FROM FIRST HOST GATE** — approved tables/version, SHA-256 `6c38e2e6b2e211f74983660fe21646e45c53822fcb1897e2a4c8cbd8d8f50f80`; final external gate rechecks it |
| Current-amendment unit, lint, debug APK, androidTest APK assembly | **UNKNOWN / HARNESS ERROR** — the exact root command stopped before project configuration because the sandbox could not create the Gradle distribution lock; no source verdict inferred |
| Historical API26 evidence | **HISTORICAL ONLY** — 15 unit tests and 21 instrumentation tests passed against prior APK SHA-256 `6a28093424cc025ebb330f5cc4ea9fbd21e3e8207607ed7e0844e551aed15cf0`; withdrawn from current qualification |
| Historical API36 evidence | **HISTORICAL ONLY** — 15 unit tests and 21 instrumentation tests passed against the same prior APK; does not validate a newly built APK |
| Exact API36 current-build qualification | **NOT RUN** — requires current app/test APK hashes, all core checks, actual OS challenge proof, lifecycle, ciphertext, failure-injection instrumentation, and accessible-layout evidence |
| HONOR400 observation | **OBSERVED ONLY / ACCEPTANCE PENDING** — DNY-NX9, Android 16 / API36, `arm64-v8a`, 4096-byte page size; native package absent; no installation or phone validation performed |
| Ciphertext/manifest/signature/export/process-death proof | **NOT RUN** |
| Optional 16 KB runtime and protected-pixel capture | **UNKNOWN / OPTIONAL** |
| P1 baseline preservation | **PRESERVED** — baseline directory is read-only by contract; final hash comparison remains in the external gate |
| Whole-app status | **PLACEHOLDERS REMAIN** — no full-stable, release, clinical, or P2 acceptance is claimed |

## Acceptance truth

The Owner’s current instruction confirms the prior “start now” authorization and explicitly says no further approval is required for this exact plan. That authorization covers only the listed dependency pins, schema1, Android credential flow, and synthetic device-bound limitations. It does not authorize direct dependencies beyond the approved pins, schema changes, reset/recovery, real patients, Git operations, release signing, physical-device commands, or production release.

On 2026-10-10 the Owner superseded only the compatibility qualification scope: `minSdk` 35 replaces 26, compile/target remain 36, and the former mandatory API26/API36 matrix is now one exact API36 engineering target. Dependencies, versions, signing, clinical logic, schema, canonical vault protections, failure-injection coverage, and every historical report remain unchanged. Physical HONOR400 preflight/installation is a separate bounded task; the phone is not accepted as a disposable emulator.

AGY1.3.2 review2: project `e72a5451-e1de-4fcb-b6e5-30fbb02887ff`, conversation `b98a29b4-1964-4f1c-99df-06af8666e3e0`; fresh/read-only/plan mode. Prescribed execution effort: **[Codex Reasoning Level: xhigh]**. The public reviewed proposal SHA-256 is `40b2e86fbf8395404b8af31978ad16803802282104da0193dbf118f664ce78ab`.

## Implemented contract

- Four Room schema1 entities only: `patients`, `visits`, `vault_metadata`, `save_receipts`; original nullable patient/visit fields remain nullable and no clinical/default expansion was added.
- Room 2.8.5 over SQLCipher Android 4.19.1 with AndroidX SQLite 2.7.0 and KSP 2.2.21-2.0.5. The SQLCipher factory’s retained passphrase reference was inspected from the pinned artifact; a distinct factory-owned array remains alive for the open session and is best-effort wiped only after `RoomDatabase.close()`.
- Atomic descriptor, authenticated key envelope, and last-published active pointer under `noBackupFilesDir`; interrupted setup artifacts are preserved and block automatic reinitialization.
- One Android Keystore AES-256/GCM wrapping alias per vault, generated only during clean explicit initialization. Production and isolated-test alias prefixes are disjoint; exact legacy production aliases are still detected as orphan artifacts without matching test aliases. Existing storage uses get-existing-key and fails closed for missing/permanently-invalidated keys, invalid/tampered envelopes, non-32-byte unwrapped passphrases, zero/truncated DB files, missing DB/metadata, schema/integrity failures, and vault mismatches. An isolated instrumentation alias records actual `KeyInfo` hardware-backing/security-level evidence when available; no hardware promise is made.
- API30+ platform `BiometricPrompt` with `DEVICE_CREDENTIAL`, preflight, and no negative button; API26–29 uses the platform credential intent. No app PIN, fake production auth, hidden test intent, or insecure fallback exists.
- Per-session Room/SQLCipher handles, foreground/manual lock, immediate patient/draft UI wipe, serialized transaction draining, 2000 ms busy reporting, atomic request/session admission, exactly-one unlock reservation, epoch rejection of obsolete auth/read/save/open callbacks, typed validation failures, and no close while a transaction owns the storage lease. Opened handles that lose admission are closed; a failed close is retained as storage-busy and cannot compete with another handle.
- Immutable save commands with expected revisions, repository-recomputed canonical fingerprints, operation UUID receipts, atomic row+receipt transactions, durable readback, replay of an old identical receipt with the latest authoritative snapshot, cross-patient/stale/revision-exhaustion rejection, and explicit pending/reconciling/error/unresolved UI. Unsaved drafts are never persisted.
- Workspace and Profile consume the same committed repository snapshot. Empty reads never seed; fixtures seed only inside the first explicit initialization transaction.
- Internal-only UUID test namespaces and exact cleanup; production `MainActivity` has no namespace override through Intent, URI, preference, network, or exported API.
- Stable synthetic banner, English/Arabic copy parity, and `FLAG_SECURE` kept enabled. The app makes only best-effort owned-array wiping/reference-release claims, never complete JVM/native-memory zeroization.

## Reproducible external gates

Run the compiler/export and host gates first, before any storage instrumentation test:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./gradlew --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
```

Then verify that `app/schemas/com.sherifradwan.ancnative.storage.RecordsDatabase/1.json` contains exactly the approved version-1 tables/columns, nullable original fields, FKs/indexes, and no clinical/default expansion; record its SHA-256.

For one explicitly selected disposable API36 emulator with an already-configured real device credential; the observed physical HONOR400 is excluded from this harness:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
ANDROID_SERIAL=<disposable-emulator> \
./gradlew --offline :app:connectedDebugAndroidTest

python3 tools/verify_p2.py \
  --serial <disposable-emulator> \
  --expected-avd <exact-disposable-avd-name> \
  --apk app/build/outputs/apk/debug/app-debug.apk
```

Before configuring a credential on a fresh disposable snapshot, the separate negative prerequisite can be recorded without creating a vault:

```sh
python3 tools/verify_p2.py \
  --serial <fresh-disposable-emulator> \
  --expected-avd <exact-disposable-avd-name> \
  --apk app/build/outputs/apk/debug/app-debug.apk \
  --expect-no-credential
```

Run only on exact API36. The verifier rejects API26 and every other API level, a non-emulator, a serial without `ro.kernel.qemu=1`, or an AVD name that differs from the explicit `--expected-avd` value before install or UI mutation. It uses `adb install -r` only, never uninstalls or clears package data, never resets the canonical vault, and reports missing prerequisites as `UNKNOWN`. It performs the real platform credential UI; P1 save/Profile, dirty-Back, rotation, cross-patient, and duplicate-save behavior; background/manual-lock data hiding; true HOME -> `am kill` -> absent PID -> relaunch with a different PID; unsaved-draft loss; measured Keystore hardware-backing evidence when available; ciphertext sentinel scans; and package/manifest/signature/native-library/schema checks. A single-target PASS additionally requires a current connected-test receipt bound to the current app and test APK hashes; stale prior-APK XML cannot qualify. The mandatory accessible-layout review remains a gap until recorded. It writes structured evidence under `evidence/p2/`; `force-stop` is not used as process-recreation proof.

## Current harness error

On 2026-10-10 the exact amended root gate `./gradlew --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` did not reach project configuration or compilation:

```text
java.io.FileNotFoundException: .../gradle-8.13-bin.zip.lck (Operation not permitted)
```

A prior direct-distribution attempt with task-specific temporary Gradle/project caches failed before project evaluation with:

```text
Could not create service of type FileLockContentionHandler
java.net.SocketException: Operation not permitted
```

The current failure and the earlier errors are `UNKNOWN / HARNESS ERROR`, not source failures and not passes. The recorded API26/API36 runs belong only to the prior APK evidence stated above. No current-amendment connected test has run, no current disposable API36 target has been selected, and the observed physical phone has not been installed to or validated.

The implementation contract remains [P2_STORAGE_PLAN.md](P2_STORAGE_PLAN.md). P2 engineering PASS requires all mandatory external gates above to pass with actual evidence; optional 16 KB-runtime and protected-pixel gaps remain separately `UNKNOWN` and confer no release acceptance.
