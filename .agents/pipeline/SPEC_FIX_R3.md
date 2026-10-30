# Fix spec round 3a - nothing invented, nothing silently overwritten, no demo patients in the clinic app
Project: Android EMR (Kotlin, Compose) for a private obstetrics clinic. Wrong or hidden data = serious failure. English-only UI.
Source: phase-1 audit items #1, #2, #3 (.agents/pipeline/AUDIT_PHASE1.md) and owner decision D8 (.agents/pipeline/OWNER_DECISIONS.md).
All earlier oracles (SPEC_FIX.md, SPEC_FIX_R2.md) keep holding, except where Q19(e) supersedes one Q17 case.

## Q19 Printed report never invents a clinical finding (ui/report/PatientPrintHtmlGenerator.kt, audit #1)
Tested through generateHtml(context = null, patient, asOfDate = fixed date) in
app/src/test/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGeneratorTest.kt.
The exact text NOT_RECORDED = "Not recorded". "Missing" means null, empty or only whitespace.
(a) Ultrasound table cells: a scan with presentation / placentalSite / amnioticFluid missing prints "Not recorded" in that cell.
    The strings "Cephalic", "Posterior Grade I" and "Adequate (AFI 14cm)" never appear in the HTML unless the patient's own data contains them.
    A scan with presentation "Breech" prints "Breech" (value unchanged).
(b) Allergy history missing -> "Not recorded". "NKDA" appears only if the patient's allergyHistory text contains it.
    Allergy present -> printed exactly as typed (existing red styling kept).
(c) Medical history missing -> "Not recorded"; surgical history missing -> "Not recorded". The word "None" is never supplied by the code.
(d) Target Delivery Timing: dating.agreedEdd missing -> "Not recorded" (never "At 39 completed weeks").
(e) Risk level missing -> the risk cell shows exactly <span class="badge">Risk not recorded</span> and neither the High Risk nor the
    Normal Risk badge. (SUPERSEDES the Q17 case "riskLevel null -> Normal Risk"; that single assertion is updated, all other Q17 cases stay.)
    Present risk levels keep the Q17 behaviour exactly.
(f) Planned Delivery Mode row: prints parseDeliveryNotes(patient.obstetricNotes).plan exactly as returned (the same text the Delivery
    tab shows under "Delivery plan"), or "Not recorded" when that is null. The fixed sentence
    "Elective Cesarean Section (CS) / Spontaneous Vaginal Delivery (SVD)" never appears.
(g) Unchanged on purpose (owner wording, not patient findings): the Rh-negative anti-D line, "Routine Obstetric Hospital Protocol",
    the generic "Special Recommendations" sentence, and every "-" placeholder already used for empty numeric/date cells.
Only these lines of the generator change. Markup elsewhere stays byte-identical.

## Q20 Saving the patient profile never overwrites existing dating (audit #2)
New pure function in a new file app/src/main/java/com/sherifradwan/ancnative/state/IntakeDatingRule.kt:
    internal data class IntakeDating(val lmpDate: String, val agreedEdd: String)
    internal fun intakeDatingUpdate(existing: com.sherifradwan.ancnative.model.DemoPatientDating?, draftLmp: String?): IntakeDating?
Rules (draftLmp is trimmed first; a valid LMP is an ISO date yyyy-MM-dd that LocalDate.parse accepts):
(a) draftLmp missing or not a valid ISO date -> null.
(b) existing == null -> IntakeDating(lmp, lmp + 280 days).
(c) existing.lmpDate (trimmed) equals draftLmp -> null (e.g. only the phone number changed).
(d) existing has a non-blank ultrasoundEdd, OR a non-null transferDate, OR a non-null embryoAgeDays, OR a datingMethod other than "LMP"
    (case-insensitive; null counts as LMP) -> null. Ultrasound/IVF dating is changed only in the Dating editor, never from the profile form.
(e) otherwise (LMP-only dating, LMP changed) -> IntakeDating(newLmp, newLmp + 280 days).
Examples (asOfDate irrelevant):
  null, "2026-02-01" -> ("2026-02-01", "2026-11-08")
  null, "  2026-02-01 " -> ("2026-02-01", "2026-11-08")
  null, "01/02/2026" -> null;  null, "" -> null;  null, null -> null
  LMP "2026-02-01" method "LMP", draft "2026-02-01" -> null
  LMP "2026-02-01" method "LMP" no USS, draft "2026-02-08" -> ("2026-02-08", "2026-11-15")
  LMP "2026-02-01" ultrasoundEdd "2026-11-12", draft "2026-02-08" -> null
  LMP null method "IVF" transferDate "2026-02-15" embryoAgeDays 5, draft "2026-02-01" -> null
  LMP "2026-02-01" method "Ultrasound" ultrasoundEdd null, draft "2026-02-08" -> null
  LMP "2026-02-01" method null, draft "2026-02-08" -> ("2026-02-08", "2026-11-15")
Wiring (state/AncViewModel.kt, the SavePatientProfileCommand branch, today about lines 285-305): build the SaveDatingCommand ONLY when
intakeDatingUpdate(committedPatient?.dating, draftLmp) is non-null, using its lmpDate/agreedEdd, ultrasoundEdd = null, datingMethod = "LMP",
and notes = existing notes if present, else "Baseline from intake LMP (<lmp>)". Nothing else in the ViewModel changes.
Static check S1: AncViewModel.kt calls intakeDatingUpdate and no longer contains "plusDays(280)".

## Q21 Demo patients only in debug/QA builds (audit #3, owner decision D8)
(a) storage/RecordsRepository.kt: seedInitialSyntheticFixtures gains a parameter includeDemoPatients: Boolean. Vault metadata is always
    inserted (exactly as today); demo patients and everything attached to them (visits, dating, scans, labs, medications,
    previous pregnancies, attachments) are inserted only when includeDemoPatients is true. The function may be renamed only if every
    caller is updated.
(b) storage/VaultController.kt passes BuildConfig.DEBUG. app/build.gradle.kts enables buildFeatures { buildConfig = true } (the one
    allowed build-file change).
(c) A new empty vault in a release build validates (validateAndLoad succeeds with zero patients).
Static checks: S2 RecordsRepository.kt seed function has an includeDemoPatients parameter and every insert of patient-owned rows sits
behind it; S3 VaultController.kt passes BuildConfig.DEBUG; S4 build.gradle.kts has buildConfig = true.
Device check (orchestrator, not a unit test): release-signed build on an emulator creates an empty vault with 0 patients; the QA debug
build still shows the 4 demo patients.
Existing installs keep whatever patients they have (no deletion); removing demo rows from an existing clinic vault is out of scope.

## Gates
G1 full unit suite green. G2 :app:compileDebugKotlin and :app:compileReleaseKotlin green. G3 all earlier static checks still pass.
New manifest ids: UNIT-intake-dating (tier 1, --tests '*IntakeDatingRuleTest'), STATIC-r3 (tier 1, python3 .agents/checks/check_r3.py,
S1-S4), plus the existing UNIT-print.

## Round 3a-2 addendum (found by the round 3a reviewer: more invented text on the printed report)
Q19 also covers (same NOT_RECORDED rule, "missing" = null/empty/whitespace):
(h) Visits table notes cell: v.findings, else v.notes, else "Not recorded". "Antenatal course progressing favorably" never appears unless typed.
(i) Previous pregnancies table: pp.placeAndDoctor, else pp.otherDetails, else "Not recorded". "Uncomplicated" never appears unless typed.
(j) Medications table notes cell: m.notes, else "-" (the empty-cell placeholder). "Ongoing as prescribed" never appears unless typed.
(k) Ultrasound image label: with impression -> "Ultrasound Scan N.M - <date> (<impression>)" exactly as today; impression missing ->
    "Ultrasound Scan N.M - <date>" with no parentheses. "Fetal Growth Surveillance" never appears unless typed.
Unchanged on purpose: the structured-outcome fallbacks ("Live Birth", "Miscarriage", "Stillbirth / IUFD", "Therapeutic Termination") are
derived from the recorded outcome, not invented; "Unspecified" for GA and phone stays.

# Round 3b - one odd value never locks every record; the app never deletes a vault by itself (audit #4, #5)
Package com.sherifradwan.ancnative.storage unless stated. All new functions are pure Kotlin/java.io (no Android imports) so JVM tests run.

## Q22 Record anomalies do not block the vault (audit #4)
New file storage/RecordAnomalies.kt:
    internal data class RecordAnomaly(val patientId: String, val field: String, val value: String?)
    internal fun findBlockingRecordProblems(snapshot: RecordSnapshot): List<String>   // empty = OK to open
    internal fun findRecordAnomalies(snapshot: RecordSnapshot): List<RecordAnomaly>   // shown later, never blocks
Blocking (structural, the data cannot be trusted to map correctly) - each gives one human-readable string naming the patient id:
  patient.revision < 1; any visit.revision < 1; any previous pregnancy with revision < 1, patientId != patient.id, or modelVersion != 3.
Anomalies (field names exactly): "status" (not in PatientVocabulary.statuses), "riskLevel" (not in riskLevels), "bloodGroupText"
  (not in bloodGroups), "gpa" (some but not all of gravida/parity/abortion null, OR any value outside 0..MAX_SAFE_GPA; value = "G/P/A"
  as text with "null" for missing), "previousPregnancy.provenanceKind" (not CLINICIAN_ENTRY), "previousPregnancy.clinicianId" (blank).
  null values are never anomalies (same as today's checks). Order: patients in snapshot order, fields in the order listed.
Examples: patient P1 bloodGroupText "O neg" -> RecordAnomaly("P1","bloodGroupText","O neg"), no blocking problem.
  riskLevel "Not high risk" -> anomaly "riskLevel". gravida 2, parity null, abortion 0 -> anomaly ("P1","gpa","2/null/0").
  A pregnancy whose patientId is "P2" inside patient "P1" -> blocking problem. Valid demo fixtures (DemoFixtures.patients) -> both empty.
Wiring: RecordsRepository.validateAndLoad blocks with DATABASE_INTEGRITY_FAILED only when findBlockingRecordProblems is non-empty
  (instead of today's isInternallyValid); RepositoryOpenResult.Success gains `val anomalies: List<RecordAnomaly> = emptyList()` filled
  from findRecordAnomalies. isInternallyValid is removed. Schema, cipher-integrity and metadata checks are unchanged.
  Showing anomalies in the UI is a later round; the records themselves are shown as stored (never hidden, never altered).

## Q23 No automatic retry loop when the vault is blocked (audit #4)
New file storage/VaultAutoAction.kt:
    internal enum class VaultAutoAction { BEGIN_UNLOCK, BEGIN_INITIALIZATION, NONE }
    internal fun vaultAutoAction(state: VaultState): VaultAutoAction
  Locked -> BEGIN_UNLOCK; Uninitialized -> BEGIN_INITIALIZATION; every other state, including every Blocked reason -> NONE.
MainActivity.kt: the LaunchedEffect(state.vaultState) uses vaultAutoAction and no longer calls retryStorageInspection.
  The Blocked screen's existing Retry button (VaultGateScreen) stays the only way to retry.
Static S5: MainActivity.kt contains no "retryStorageInspection()" call inside the LaunchedEffect and calls vaultAutoAction.

## Q24 The app never deletes a vault that holds a database (audit #5)
New pure function (storage/VaultFiles.kt or a new storage/VaultCleanupRule.kt):
    internal fun hasVaultDatabase(namespaceRoot: java.io.File): Boolean
  true when any direct child directory whose name starts with "vault-" contains a file named "records.db" (any size, even 0 bytes).
  Missing root, empty root, a "vault-x" dir with only descriptor/envelope files, or a "records.db" outside a vault-* dir -> false.
VaultFiles.cleanupUnpublishedArtifacts(): deletes nothing when hasVaultDatabase(namespaceRoot) is true (in addition to today's
  pointer check). VaultController: both call sites (deviceCredentialUnavailable INITIALIZE branch and retryInspection) call
  keys.deleteOrphanAliases() ONLY when the pointer is missing AND hasVaultDatabase(files.namespaceRoot) is false. When the pointer
  is missing but a database exists, nothing is deleted and inspection proceeds (it reports POINTER_MISSING as today).
Static S6: every deleteOrphanAliases() call in VaultController.kt is guarded by hasVaultDatabase; S7: cleanupUnpublishedArtifacts body
  calls hasVaultDatabase.
JVM tests (temp directories) cover hasVaultDatabase fully.

## Gates 3b
G1 full unit suite, G2 compileDebugKotlin + compileReleaseKotlin, G3 all earlier static checks. New manifest ids: UNIT-anomalies
(--tests '*RecordAnomaliesTest'), UNIT-vault-auto (--tests '*VaultAutoActionTest'), UNIT-vault-cleanup (--tests '*VaultCleanupRuleTest'),
STATIC-r3b (python3 .agents/checks/check_r3b.py, S5-S7). Device check by the orchestrator: QA build on the emulator still opens normally.

# Round 3c - app lock and PIN (owner decisions D6, D7; audit #7) and the Android 8-10 crash (audit #10)
Package com.sherifradwan.ancnative.security unless stated. Pure logic lives in new files with no Android imports (JVM-tested).

## Q25 PIN policy (new file security/PinPolicy.kt)
    internal const val PIN_ITERATIONS = 120_000
    internal fun isValidNewPin(pin: String): Boolean
      true only for exactly 6 ASCII digits that are not all the same digit (111111) and not a straight run up or down
      (012345, 123456, 234567 ... 456789, 987654 ... 543210, 210987 is NOT a run). Examples: "482915" true; "123456" false; "000000" false;
      "12345" false; "12345a" false; "１２３４５６" (full-width digits) false; "135790" true.
    internal fun hashPin(pin: String, salt: ByteArray, iterations: Int = PIN_ITERATIONS): ByteArray
      PBKDF2WithHmacSHA256 (javax.crypto), 256-bit output. Same inputs -> same bytes; different salt -> different bytes.
    internal fun newSalt(): ByteArray   // 16 bytes from SecureRandom
    internal fun pinMatches(pin: String, salt: ByteArray, iterations: Int, expected: ByteArray): Boolean
      constant-time comparison (java.security.MessageDigest.isEqual).
    internal fun lockoutDelayMs(consecutiveFailures: Int): Long
      0..4 -> 0; 5 -> 30_000; 6 -> 60_000; 7 -> 300_000; 8 or more -> 900_000. Negative -> 0.
## Q26 Lock timing (new file security/AppLockPolicy.kt)
    internal const val BACKGROUND_LOCK_MS = 600_000L   // D6: 10 minutes
    internal fun shouldLockOnReturn(backgroundSinceMs: Long?, nowMs: Long): Boolean
      null -> false; now - since >= 600_000 -> true; now < since (clock moved back) -> true; else false.
      Examples: (1_000, 600_999) -> false; (1_000, 601_000) -> true; (1_000, 500) -> true; (null, 5) -> false.
## Q27 Repository and lifecycle wiring (security/SecuritySettingsRepository.kt, MainActivity.kt)
(a) No default PIN: the constant DEFAULT_PIN and the init block that seeds it are removed. The stored PIN becomes three keys:
    key_pin_hash_v2 (Base64 PBKDF2 output), key_pin_salt_v2 (Base64), key_pin_iterations_v2 (Int).
(b) isPinConfigured = the v2 hash exists. When neither v2 nor the legacy key_pin_hash exists, the app shows PIN setup before any
    patient data (first launch).
(c) Legacy migration: when only the legacy key_pin_hash exists, the lock screen asks for the current PIN and checks it with the old
    SHA-256 rule (kept only for this check, private). On success it stores the PIN in v2 form and deletes key_pin_hash. If the legacy PIN
    was "123456" it then requires setting a new PIN (isValidNewPin) before unlocking.
(d) isAppLocked starts TRUE at every cold start whenever a PIN (v2 or legacy) exists.
(e) Failed attempts: consecutive failures and the lockout end time persist in SharedPreferences (survive restart); a correct PIN resets
    them. While lockoutDelayMs is running, PIN entry is refused without checking (the screen shows the remaining time).
(f) MainActivity records the time in onStop and, in onStart, calls lockApp() when shouldLockOnReturn(storedTime, now) is true.
(g) AutoLockTimeout loses NEVER; the default and any stored NEVER read as TEN_MINUTES (new value, 600_000 ms). D6 fixes 10 minutes; the
    other existing options may stay as shorter choices.
(h) updatePin rejects anything isValidNewPin rejects.
## Q28 Android 8-10 crash (security/BiometricAuthManager.kt; audit #10)
    isBiometricAvailable returns false when Build.VERSION.SDK_INT < 30, before touching any android.hardware.biometrics class;
    promptBiometric then reports false (PIN only). Android 11+ behaviour unchanged.
## Q29 Screens (UI lane: ui/AppLockScreen.kt, ui/AncApp.kt, ui/SecuritySettingsDialog.kt, res/values/strings.xml)
(a) PIN setup screen (first launch, and after a legacy 123456): enter 6 digits, confirm, explain the rule in one line, refuse invalid PINs
    with a clear message; no patient data visible until done. All text in strings.xml.
(b) Lock screen during lockout shows "Too many wrong PINs. Try again in <time>." and disables entry until the time passes.
(c) Settings "Change PIN" uses isValidNewPin and shows the same rule; the auto-lock list has no "Never" option.
(d) Biometric button hidden when isBiometricAvailable is false. Existing testTags kept.
## Static checks (check_r3c.py)
S8 BiometricAuthManager.kt checks SDK_INT < 30 before any BiometricManager use. S9 SecuritySettingsRepository.kt has no "123456" and no
DEFAULT_PIN. S10 SecuritySettingsRepository.kt uses hashPin/pinMatches from PinPolicy for v2. S11 MainActivity.kt calls
shouldLockOnReturn. S12 AutoLockTimeout has no NEVER. S13 in security/SecuritySettingsRepository.kt ONLY: no "MessageDigest.getInstance(\"SHA-256\")" outside the legacy check function (other files use SHA-256 for fingerprints and integrity checks and are out of scope).
## Gates 3c
G1 full suite, G2 debug + release compile, G3 earlier static checks. Manifest ids: UNIT-pin-policy (--tests '*PinPolicyTest'),
UNIT-lock-policy (--tests '*AppLockPolicyTest'), STATIC-r3c. Device (orchestrator): API 26 emulator opens Settings without crashing;
QA copy on the Honor 400: first launch asks for a PIN, cold start and >10 min away ask again, 5 wrong PINs show a 30 s wait.

## Round 3c-2 addendum (found by the orchestrator on the API 26 emulator; the reviewer had flagged the second as fragile)
Device evidence: legacy PIN 123456 entered -> stored as v2, legacy key removed, failures reset, BUT (1) the lock screen stayed on
"Enter your current PIN" (it reads securityRepo.settings.value once instead of observing it), and (2) after a restart 123456 unlocked
straight to the patient list with no forced change: the "must change" state lived only in memory. D7 is violated.
Q30 (a) Persisted rule: after ANY successful PIN check (legacy or v2), when isValidNewPin(enteredPin) is false, the repository stores
     key_pin_change_required = true BEFORE returning. loadSettings sets needsPinSetup = (no PIN configured) OR key_pin_change_required.
     updatePin/setupPin with a valid new PIN clears key_pin_change_required. So 123456 (or any weak PIN already stored) can never unlock
     to patient data, now or after any restart, until a valid new PIN is set.
  (b) The lock/setup screen observes the settings flow (collectAsState or equivalent) so it switches to "Choose a new PIN" at once.
  (c) While needsPinSetup is true, the gate in AncApp keeps the lock/setup screen up even if isAppLocked is false (already true today;
     keep it).
Static checks (added to check_r3c.py): S14 SecuritySettingsRepository.kt defines and writes key_pin_change_required, reads it in
loadSettings, and calls isValidNewPin inside validatePin. S15 AppLockScreen.kt observes securityRepo.settings (collectAsState /
collectAsStateWithLifecycle) and does not read `securityRepo.settings.value` to decide which mode to show.
Device check (orchestrator): legacy 123456 -> "Choose a new PIN"; restart before choosing -> still forced; after a valid new PIN -> unlocks;
restart -> the new PIN works and 123456 fails.

## Round 3c-3 addendum (found by the orchestrator on the API 26 emulator)
Device evidence: Settings dialog open -> app to background -> more than 10 minutes -> back: the lock screen is shown UNDER the still-open
Security Settings dialog, so Settings (backup export/restore, clinic settings) is usable without the PIN. D6 is violated.
Q31 (ui/AncApp.kt) The Security Settings dialog is shown only when the app is not locked and no PIN setup is needed
     (condition: securityRepo != null && showSettingsDialog && !isAppLocked && needsPinSetup != true), and when the app becomes locked
     or needs PIN setup, showSettingsDialog is reset to false (so it does not reappear by itself after unlocking).
     Every other dialog in AncApp keeps its existing !isAppLocked guard and also gains needsPinSetup != true.
Static S16 (check_r3c.py): in AncApp.kt the SecuritySettingsDialog call sits under a condition that includes isAppLocked and
needsPinSetup, and showSettingsDialog is set to false in an effect keyed on the lock state; every AlertDialog( in AncApp.kt is under a
condition that includes !isAppLocked.
Device check (orchestrator): repeat the scenario; after the lock only the PIN screen is visible; after unlocking, Settings is closed.
