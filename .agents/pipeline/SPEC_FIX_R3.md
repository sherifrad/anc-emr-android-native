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

## Round 3d (audit checkpoint 1: confirmed items; 5 of the findings were checked against the code by the orchestrator)
Source: audit1/ reports of Claude Sonnet 5.5 (security), GPT-6 Luna (clinical), Nemotron 3 Ultra (concurrency). Owner decisions D6-D8 unchanged.
Deferred to 3e (need an owner decision or a format change, NOT in this round): closing the vault when the app locks (camera/picker/share-sheet
also background the app, so a naive lock would lose an open editor draft); photos inside the backup and photo encryption; backup PBKDF2
iterations (needs a file-format change); anomaly banner; HTML-escaping of printed text.
Q32 (res/xml/file_paths.xml) Add `<cache-path name="backups" path="backups/" />` so FileProvider can share the file the Export Backup button writes to
     cacheDir/backups. Keep the two existing entries.
Q33 (ui/SecuritySettingsDialog.kt) The auto-lock timeout picker did nothing (D6 fixes the rule at 10 minutes). Remove the picker and its
     setAutoLockTimeout call; show one plain line "Locks after 10 minutes away." Leave the AutoLockTimeout enum and the repository field.
Q35 (MainActivity.kt) In onCreate, before setContent, set FLAG_SECURE on the window ONLY when !BuildConfig.DEBUG (so recents thumbnails and screenshots
     of the clinic app show no patient data, while the QA/debug copy stays screenshot-able for device tests). Delete the Log.d("VAULT_DEBUG", ...) line.
Q36 (ui/report/PatientTwoPageReportDialog.kt) The report WebView must not run JavaScript: javaScriptEnabled = false (the report HTML contains no script).
Q37 DEFERRED TO OWNER (found while running 3d: existing oracles Q17 and Q19g lock in the owner's Rh-negative Anti-D line and the routine-protocol line; changing them is an owner decision). Original text kept for the record - NOT part of this round:
     (a) "Special Recommendations" prints "Not recorded" (there is no recorded field; never the generic kick-count sentence).
     (b) "Hospital & Blood Bank Precautions" prints "Not recorded", except for an Rh-negative patient: "Rh negative - anti-D plan not recorded".
     The output never contains "Anti-D Prophylaxis + Matched", "Routine Obstetric Hospital Protocol" or "Regular fetal kick counts".
Q38 (security/SecuritySettingsRepository.kt) Weak-PIN setup mode must not be reachable without proving the old PIN.
     (a) loadSettings: needsPinSetup = !configured. The persisted key_pin_change_required must NOT by itself put the app in setup mode.
     (b) validatePin: unchanged - a successful check of a weak PIN sets needsPinSetup=true, isAppLocked=true in memory (setup mode after proof).
     (c) lockApp(): also sets needsPinSetup = !isPinConfigured, so a lock after setup mode started (10 min away) shows the normal PIN entry.
     Result: after a restart or a lock the weak PIN must be typed again, then the new-PIN form appears; nothing is skipped.
Q39 (honest labels; ui/WorkspaceScreen.kt, ui/PatientEditor.kt, storage/AttachmentSyncManager.kt, ui/SecuritySettingsDialog.kt) Photos are stored as plain
     files, and backups hold records only. Replace the false claims: "Encrypted & Verified" -> "Saved on this phone"; PatientEditor line 411 text ->
     "Photos are stored on this phone (not encrypted separately)"; "Locally Encrypted & Secured" -> "Stored on this phone"; remove "Zero Data Loss" and
     "100% verified" wording from the backup text and add "Backups contain patient records only, not photos."
Q40 (ui/SecuritySettingsDialog.kt) Restore must ask first: the Restore button opens an AlertDialog with testTag "restore_confirm_dialog",
     text "Restoring replaces every patient on this phone with the backup. This cannot be undone.", buttons "Cancel" and "Replace all"; only
     "Replace all" runs the existing restore code.
Static checks (new .agents/checks/check_r3d.py, id STATIC-r3d, exit 1 on any FAIL):
 S17 file_paths.xml has a cache-path whose path is backups or backups/ and still has camera/ and files-path.
 S18 MainActivity.kt: FLAG_SECURE appears inside an `if` whose condition has `!BuildConfig.DEBUG`; no "VAULT_DEBUG".
 S19 SecuritySettingsRepository.kt: in loadSettings the needsPinSetup value does not mention KEY_PIN_CHANGE_REQUIRED or "key_pin_change_required";
     lockApp() body assigns needsPinSetup.
 S20 PatientTwoPageReportDialog.kt: no `javaScriptEnabled = true`.
 S21 SecuritySettingsDialog.kt: no `AutoLockTimeout.entries`, no `setAutoLockTimeout(`; contains testTag "restore_confirm_dialog" inside an AlertDialog.
 S22 false claims gone: none of "Encrypted & Verified" (WorkspaceScreen), "Hardware Vault Security" on the PatientEditor photo line,
     "Locally Encrypted & Secured" (AttachmentSyncManager), "Zero Data Loss" / "100% verif" (SecuritySettingsDialog).
 Unit tests (PatientPrintHtmlGeneratorTest, id UNIT-print): Q37 a and b as written above, plus the existing tests stay green.
Device check (orchestrator): Export Backup opens the share sheet; QA copy still screenshot-able; release build sets FLAG_SECURE; weak-PIN restart
shows the normal PIN entry first; Restore asks before replacing; report still prints with JavaScript off.

## Round 3e (audit checkpoint 1: lock closes the vault, fake scan screen, biometric switch, contrast, TalkBack, touch targets)
Owner (2026-10-30): photo encryption/photos-in-backup is PAUSED to its own later round (storage cost to be settled first). Q37 printed wording stays as is.
Facts read from the code: when the vault leaves the Unlocked state the ViewModel replaces AncState with an empty state (open editors and drafts are
discarded; this is how "Lock now" already behaves), and MainActivity auto-starts beginUnlock() whenever the vault is Locked - so closing the vault on
the app lock needs the auto-unlock gated on the app lock, otherwise it reopens at once.
Q41 Vault closes with the app lock.
  (a) storage/VaultAutoAction.kt: `vaultAutoAction(state: VaultState, appUnlocked: Boolean)` (NO default value). Locked -> BEGIN_UNLOCK only when
      appUnlocked, else NONE. Uninitialized -> BEGIN_INITIALIZATION as today. Every other state -> NONE.
  (b) state/AncViewModel.kt: `fun lockVault() = manualLock()` (clears frozen commands and calls controller.lock(); same as the existing Lock-now path).
  (c) MainActivity.kt: appUnlocked = !isAppLocked && !needsPinSetup (from securityRepo.settings). A LaunchedEffect keyed on isAppLocked calls
      viewModel.lockVault() when isAppLocked becomes true. The auto-unlock LaunchedEffect is keyed on (state.vaultState, appUnlocked) and uses
      vaultAutoAction(state.vaultState, appUnlocked). Result: PIN screen up = vault closed and stays closed; correct PIN = vault reopens by itself.
  Accepted consequence: an open editor is discarded when the lock fires (existing behaviour of any vault lock). Not in scope: draft saving.
Q42 (ui/AppLockScreen.kt) The fingerprint key's clickable is enabled only when settingsState.isBiometricEnabled is true (plus today's conditions);
    no automatic biometric prompt when it is off.
Q43 (ui/UltrasoundPhotoViewerDialog.kt, ui/ClinicalAttachmentViewerDialog.kt) When a photo cannot be decoded or loaded, show the text
    "Image unavailable" (testTag "image_unavailable") - never a simulated ultrasound canvas, never invented machine values (3.5MHz, DEPTH, GAIN, FPS,
    TIS, MI) and never a defaulted presentation ("CEPHALIC"). Remove the permanent "Loading encrypted image..." wording: show "Loading image..." only
    while loading and "Image unavailable" after a failure.
Q44 (ui/Theme.kt) Every status-tag text/background pair meets WCAG AA 4.5:1 (light and, where dark tokens exist, dark). Only darken the TEXT colour of a
    failing pair (e.g. TagHighRiskText to a darker red); do not change backgrounds or any other colour.
Q45 (ui/PatientListScreen.kt, ui/ProfileScreen.kt, ui/AddAttachmentDialog.kt) IconButtons that are `.size(36.dp)` use `.minimumTouchTarget()`
    (already defined in ui/Components.kt) so they are at least 48dp; icons keep their size; layouts must not overflow.
Q46 (ui/AppLockScreen.kt) TalkBack: the six PIN dots are one semantics node with a contentDescription like "3 of 6 digits entered"; the error and
    lockout messages are live regions (Polite); each keypad key has Role.Button. Keep every testTag.
Static checks (new .agents/checks/check_r3e.py, id STATIC-r3e, exit 1 on any FAIL; strip comments first):
 S23 VaultAutoAction.kt: vaultAutoAction has an appUnlocked Boolean parameter without default; the Locked branch depends on it.
 S24 AncViewModel.kt defines lockVault; MainActivity.kt calls vaultAutoAction( with two arguments, calls lockVault() inside an effect keyed on isAppLocked.
 S25 AppLockScreen.kt: the biometric key's clickable enabled expression contains isBiometricEnabled.
 S26 neither UltrasoundPhotoViewerDialog.kt nor ClinicalAttachmentViewerDialog.kt contains "3.5MHz", "GAIN", "CEPHALIC" or "Loading encrypted image"; both contain "Image unavailable".
 S27 AppLockScreen.kt contains liveRegion, Role.Button and a contentDescription mentioning "digits".
 S28 no `.size(36.dp)` on an IconButton in PatientListScreen.kt, ProfileScreen.kt, AddAttachmentDialog.kt (a Modifier chain on IconButton( ... ) that includes size(36.dp)).
 Unit: VaultAutoActionTest updated to the new signature (Locked+appUnlocked -> BEGIN_UNLOCK; Locked+!appUnlocked -> NONE; Uninitialized unchanged);
 ThemeContrastTest lists every tag pair in Theme.kt (HighRisk, Attention, Obstetric, GA, Blood, age, delivery) and asserts >= 4.5.
Device check (orchestrator, QA copy): after 11 min away only the PIN screen shows, the vault reopens after the PIN (time measured) and the list returns;
Lock now behaves the same; a scan with a missing photo shows "Image unavailable"; cards still look right with 48dp targets.

## Round 3f-1 (audit pass 2: reuse helpers, attachment errors, real PIN/vault-guard tests, bounded decode)
Owner (2026-10-30): errors must be shown; delete the database row first; real tests for PIN lockout and the vault-key guard; remove the Rh tautology
and the duplicate print test (duplicate already removed by the orchestrator); fix the test manifest; bound photo decoding. STANDING PRINCIPLE: every rule
lives in ONE function and other code calls it. No behaviour change except Q51/Q52.
Q47 (NEW storage/Hashing.kt) `internal fun ByteArray.toHexLower(): String`, `internal fun sha256Hex(bytes: ByteArray): String`,
    `internal fun sha256Hex(text: String): String` (UTF-8), `internal fun sha256Hex(input: java.io.InputStream): String` (streams, closes nothing).
    Replace every copy of `joinToString("") { "%02x".format(it) }`: ClinicalCommands.kt (private sha256), SaveVisitCommand.kt (final line; keep its framing logic,
    only the hex conversion is shared), PatientAttachmentVault.kt (both sites), SecuritySettingsRepository.kt legacyHashPin. Outputs must stay byte-identical.
    After the change `"%02x"` appears only in Hashing.kt.
Q48 (NEW storage/BoundedBitmap.kt) `internal fun computeSampleSize(width: Int, height: Int, maxSide: Int): Int` = the SAME rule PatientAttachmentVault uses today
    (largest power of two such that both width/2/sample and height/2/sample are still >= maxSide; 1 when either side <= maxSide or any value <= 0).
    `internal object BoundedBitmap { fun decodeFile(path: String, maxSide: Int): Bitmap?; fun decodeBytes(bytes: ByteArray, maxSide: Int): Bitmap?;
    fun decodeStream(open: () -> InputStream?, maxSide: Int): Bitmap? }` : bounds pass first, then inSampleSize, ARGB_8888; return null (never throw) on bad data,
    missing file, or OutOfMemoryError. PatientAttachmentVault.compressAndWrite uses computeSampleSize/decodeBytes (its inline loop is removed; output unchanged).
    UI display sites call BoundedBitmap: WorkspaceScreen.kt (attachment thumbnail ~l.1925) and ClinicalAttachmentViewerDialog.kt with maxSide 2048;
    UltrasoundPhotoViewerDialog.kt with 2048; AddAttachmentDialog.kt preview with 1024. After the change `BitmapFactory.decode` appears only in BoundedBitmap.kt.
Q49 (storage/VaultCleanupRule.kt) `internal fun cleanUpAbandonedInitialization(namespaceRoot: File, activePointerFile: File, cleanupUnpublished: () -> Unit, deleteOrphanAliases: () -> Unit)`:
    if activePointerFile exists -> do nothing. Else call cleanupUnpublished(), THEN if !hasVaultDatabase(namespaceRoot) call deleteOrphanAliases().
    VaultController.kt's three identical blocks (cancelAuthentication, deviceCredentialUnavailable, retryInspection) call it with
    `{ files.cleanupUnpublishedArtifacts() }` and `{ keys.deleteOrphanAliases() }`; inline copies are removed. Same lock/mutex context as today.
Q50 (security/SecuritySettingsRepository.kt) Test seam, no behaviour change: add `internal constructor(prefs: SharedPreferences, clock: () -> Long = System::currentTimeMillis)`;
    the existing private Context constructor delegates to it; every `System.currentTimeMillis()` in the class uses `clock()`; replace android.util.Base64 with
    java.util.Base64 (basic encoder/decoder; identical output to NO_WRAP). Remove the unused `setupPin`, `shouldLockOnBackground`, `lockIfBackgroundTimeoutElapsed` only if
    nothing in app/src references them (grep first); otherwise leave them.
Q51 (state/AncState.kt, state/AncViewModel.kt) `enum class AttachmentError { SAVE_FAILED, DELETE_FAILED }`; AncState gets `attachmentError: AttachmentError? = null`;
    events `AncEvent.AttachmentFailed(val error: AttachmentError)` and `data object AncEvent.DismissAttachmentError`; the reducer sets/clears the field and changes nothing else.
    AncViewModel.addAttachment: any Exception other than CancellationException (rethrown) -> still Log.e, AND dispatch AttachmentFailed(SAVE_FAILED).
    AncViewModel.deleteAttachment: FIRST controller.deleteAttachment (the database row), THEN PatientAttachmentVault.deleteAttachment (the file). An exception from the
    first step -> AttachmentFailed(DELETE_FAILED) and the file is NOT touched. An exception thrown by the second step -> AttachmentFailed(DELETE_FAILED). A `false` return
    (file already missing) is not an error.
Q52 (ui/WorkspaceScreen.kt, res/values/strings.xml) When state.attachmentError != null show a dismissible Card (testTag "attachment_error_banner", error colour from the
    theme, same layout as the saved-notice card) with strings attachment_save_failed = "The attachment could not be saved. Nothing was filed. Please try again." and
    attachment_delete_failed = "The attachment could not be deleted. Please try again."; its Close button dispatches AncEvent.DismissAttachmentError.
Q53 Tests (tests lane): (a) NegativeControlAndClinicalValidationTest Rh test calls isRhNegative / isRhPositive (ui/ClinicalFormat.kt) over the same table, no `contains("-")`;
    (b) new unit tests: HashingTest, BoundedBitmapTest (computeSampleSize cases incl. 0/negative and the exact threshold; the Bitmap functions only if testable without Robolectric),
    VaultCleanupRuleTest extended for cleanUpAbandonedInitialization with temp dirs (pointer exists -> nothing called; no pointer + vault db present -> cleanup called, aliases NOT
    deleted; no pointer + no vault db -> cleanup then deleteOrphanAliases, in that order), SecuritySettingsRepositoryTest with a map-backed fake SharedPreferences (test-side file) and a
    fake clock: 5th wrong PIN starts a lockout and growing waits follow lockoutDelayMs; a correct PIN during lockout is rejected; success resets failures and lockout; a legacy
    SHA-256 PIN verifies, migrates to v2 (legacy key removed) and still verifies; a weak but correct PIN sets needsPinSetup; wrong PIN with no stored PIN returns false;
    AttachmentErrorStateTest for the reducer (set, dismiss, unrelated events keep it).
    (c) check_r3b S6 is rewritten for the new shape (see S31) - it must FAIL if a call site passes an unguarded alias delete; (d) TEST_MANIFEST: UNIT-vault-cleanup deps point at
    VaultCleanupRule.kt; add entries (with covers) for the five new test classes, STATIC-r3f1, S14-S16 under STATIC-r3c, Q18 under UNIT-print, and one entry for every existing
    test class that has none (list app/src/test/**); the UNIT-all entry stays.
Static checks (.agents/checks/check_r3f1.py, id STATIC-r3f1; strip comments first; exit 1 on any FAIL):
 S29 across app/src/main the literal "%02x" occurs only in storage/Hashing.kt.
 S30 across app/src/main `BitmapFactory.decode` occurs only in storage/BoundedBitmap.kt.
 S31 VaultController.kt calls cleanUpAbandonedInitialization( exactly 3 times and contains no `hasVaultDatabase(`; VaultCleanupRule.kt's helper body evaluates hasVaultDatabase( inside an
     `if (!hasVaultDatabase(` immediately guarding the deleteOrphanAliases() invocation, after cleanupUnpublished().
 S32 SecuritySettingsRepository.kt has no `android.util.Base64`, no `System.currentTimeMillis()` outside the constructor default, and has the internal constructor taking SharedPreferences.
 S33 AncViewModel.kt: in deleteAttachment the text `controller.deleteAttachment(` comes before `PatientAttachmentVault.deleteAttachment(`; AttachmentFailed( appears in both addAttachment and deleteAttachment;
     CancellationException is rethrown in addAttachment.
 S34 WorkspaceScreen.kt contains attachment_error_banner, DismissAttachmentError and R.string.attachment_save_failed / attachment_delete_failed; strings.xml defines both.
 S35 no file named PatientPrintHtmlGeneratorTest.kt exists under app/src/test outside ui/report; NegativeControlAndClinicalValidationTest.kt has no `bg.contains("-")`.
Device check (orchestrator, QA copy, later): attach a photo; force a failure (fill/readonly dir is not allowed on the phone, so on the emulator) and see the banner; delete works; viewers still show photos.

## Round 3f-2 (in-app camera + quality presets)
Owner (2026-10-30): the camera must be inside the app (frame + capture button) so the app is never left and the lock cannot fire behind the phone's camera app; photos are mostly
antenatal cards, prescriptions and reports so smaller sizes are fine; quality is chosen from PREDEFINED presets only (no free numbers). CameraX 1.5.3 (camera-core, camera2, lifecycle,
view) was added to app/build.gradle.kts by the orchestrator (owner-approved; already resolved offline). Reuse principle applies: one camera component, one preset enum, one compressor.
Q54 (NEW storage/PhotoQuality.kt) `enum class PhotoQualityPreset(val maxSide: Int, val webpQuality: Int, val labelRes... )` is NOT allowed to reference R (keep it Android-free): values
    SMALL(1280, 70), STANDARD(1600, 75), DETAILED(1920, 85); `companion object { val DEFAULT = SMALL; fun defaultFor(category: String): PhotoQualityPreset }` where ultrasound
    (AttachmentCategories.ULTRASOUND) -> DETAILED and every other category -> SMALL.
Q55 (storage/PatientAttachmentVault.kt) saveAttachment and saveAttachmentFromStream take `preset: PhotoQualityPreset = PhotoQualityPreset.DEFAULT`; compressAndWrite uses preset.maxSide and
    preset.webpQuality instead of the MAX_DIMENSION_PX / WEBP_QUALITY constants (constants removed). Everything else unchanged (WebP, SHA-256, paths).
Q56 (state/AncState.kt, state/AncViewModel.kt) `AncEvent.AddAttachment` gets `val preset: PhotoQualityPreset = PhotoQualityPreset.defaultFor(category)`... (a default parameter that
    does not break existing constructor calls); AncViewModel.addAttachment passes it to the vault. After the vault finished (success or failure) a source file that lives in the app's
    camera cache folder (see Q57) is deleted: `CameraCaptureHelper.deleteIfCaptureTemp(context, uri)`.
Q57 (storage/CameraCaptureHelper.kt) keep createCameraTempUri; add `createCameraTempFile(context): File` (createCameraTempUri reuses it, no second copy of the naming rule),
    `uriFor(context, file): Uri` (FileProvider), `deleteIfCaptureTemp(context, uri): Boolean` (true only for files inside cacheDir/camera) and `clearStaleCaptures(context, olderThanMs)`.
Q58 (NEW ui/InAppCameraDialog.kt) `@Composable fun InAppCameraDialog(onCaptured: (Uri) -> Unit, onDismiss: () -> Unit)`: a full-screen Dialog (usePlatformDefaultWidth=false) with a CameraX
    PreviewView inside a rounded frame (testTag "camera_preview"), a round capture button (testTag "camera_capture", >= 56dp, contentDescription from strings), a Close button
    (testTag "camera_close"). After a capture it shows the still photo with "Retake" (testTag "camera_retake") and "Use photo" (testTag "camera_use"); Use calls onCaptured(uriFor(file)),
    Retake deletes the temp file and returns to the live preview, Close/dismiss deletes any uncommitted temp file. Back camera, ImageCapture to the temp file (CAPTURE_MODE_MINIMIZE_LATENCY),
    target rotation from the display. CAMERA permission: requested inside the dialog (RequestPermission); while denied show a message (strings) and a Close button; never crash.
    Lifecycle: bind to LocalLifecycleOwner and unbindAll in a DisposableEffect so the camera is released when the dialog leaves composition (this is what happens when the app lock fires).
    The component calls clearStaleCaptures(context, 24h) once when it opens. No flash/zoom controls in this round.
Q59 (ui/AddAttachmentDialog.kt, ui/UltrasoundEditor.kt) Both replace ActivityResultContracts.TakePicture, the permission launcher and launchCamera() with InAppCameraDialog (shown from a
    `showCamera` state); the result is handled exactly as the old TakePicture success path (same selectedUri / draft IMAGE_URIS update). The gallery picker stays.
    AddAttachmentDialog shows a preset row of three FilterChips (testTags preset_small, preset_standard, preset_detailed; labels from strings: "Small - documents", "Standard - reports",
    "Detailed - scans") initialised to PhotoQualityPreset.defaultFor(selectedCategory) and re-defaulted when the category changes unless the user already chose one; the chosen preset is
    passed through onSave -> AncEvent.AddAttachment. (WorkspaceScreen.kt wires the extra parameter.) The preset is NOT stored in the database (no schema change).
Q60 res/values/strings.xml: camera_capture, camera_retake, camera_use, camera_close, camera_permission_needed ("Camera permission is needed to take a photo. You can still choose a photo
    from the gallery."), photo_quality_label ("Photo size"), and the three preset labels. Old external-camera strings may stay.
Static checks (.agents/checks/check_r3f2.py, id STATIC-r3f2; strip comments first; exit 1 on any FAIL):
 S37 app/build.gradle.kts has the four androidx.camera dependencies; AndroidManifest has the CAMERA permission.
 S38 neither AddAttachmentDialog.kt nor UltrasoundEditor.kt contains TakePicture or launchCamera; both call InAppCameraDialog(.
 S39 InAppCameraDialog.kt exists with the testTags camera_preview, camera_capture, camera_close, camera_retake, camera_use, uses PreviewView and ImageCapture, requests Manifest.permission.CAMERA,
     and contains unbindAll inside a DisposableEffect/onDispose.
 S40 PhotoQuality.kt has the three presets with exactly the values of Q54; PatientAttachmentVault.kt has no MAX_DIMENSION_PX / WEBP_QUALITY and references preset.maxSide and preset.webpQuality.
 S41 AddAttachmentDialog.kt contains preset_small, preset_standard, preset_detailed and PhotoQualityPreset.defaultFor(; AncViewModel.kt calls CameraCaptureHelper.deleteIfCaptureTemp(.
 S42 CameraCaptureHelper.kt defines deleteIfCaptureTemp, createCameraTempFile and clearStaleCaptures; strings.xml defines every Q60 key.
Unit: PhotoQualityPresetTest (exact values; defaultFor ULTRASOUND -> DETAILED, others incl. unknown/empty -> SMALL; no preset has maxSide > 1920 or quality > 85).
Device check (orchestrator, QA copy on the Honor 400): the camera opens inside the app with a live preview; capture, retake, use; photo saved with the Small preset is smaller than Detailed;
the thumbnail/viewer show it; the app lock does not interfere while the camera is open; permission denied path; no crash in logcat.

## Round 3f-3 (found on the phone: photos saved sideways; Save button unreachable)
Evidence (Honor 400, QA copy): a camera photo previews rotated 90 degrees in the Add Attachment dialog (only the camera's own still honours EXIF); with a photo selected the dialog is taller than
the screen, does not scroll, and "Save to Patient Record" / Cancel text cannot be reached.
Q61 (storage/BoundedBitmap.kt) EXIF orientation is honoured by EVERY BoundedBitmap decode (decodeFile, decodeBytes, decodeStream) - one place, no per-caller rotation code.
    Add pure `internal data class ExifTransform(val degrees: Float, val flipHorizontal: Boolean)` and `internal fun exifTransform(orientation: Int): ExifTransform` for the 8 android.media.ExifInterface
    ORIENTATION_* values (1 normal; 2 flip-h; 3 rotate 180; 4 flip-v = rotate 180 + flip-h; 5 transpose = rotate 90 + flip-h; 6 rotate 90; 7 transverse = rotate 270 + flip-h; 8 rotate 270; anything else = normal).
    After the bounded decode, read the orientation (ExifInterface on the same bytes/file/stream) and, when it is not normal, return a bitmap rotated/flipped accordingly (Matrix); recycle the intermediate;
    a failure to read EXIF means "normal" (never null/throw). The vault's compressAndWrite already decodes through BoundedBitmap, so saved WebP files become upright.
    ui/InAppCameraDialog.kt: delete its own decodeUpright/EXIF code and decode the still through BoundedBitmap (same function, no second copy).
Q62 (ui/AddAttachmentDialog.kt) The dialog content scrolls (verticalScroll with a remembered scroll state, the Card limited to the screen height) so Cancel and "Save to Patient Record" are always reachable
    at any photo size; the preview image height is capped (heightIn max 240.dp). Keep every testTag and behaviour.
Static checks (.agents/checks/check_r3f3.py, STATIC-r3f3):
 S43 BoundedBitmap.kt defines exifTransform and uses ExifInterface; InAppCameraDialog.kt contains no ExifInterface and no decodeUpright; no other file outside BoundedBitmap.kt references ExifInterface.
 S44 AddAttachmentDialog.kt contains verticalScroll and heightIn( for the preview, and still contains addAttachmentDialog and the save button text.
Unit: BoundedBitmapTest extended: exifTransform for all 8 orientation values plus an unknown value (0, 9, -1).
Device (orchestrator): a camera photo previews and saves upright; the dialog scrolls to Save; the saved scan photo opens upright in the viewer.

## Round 3g (ultrasound scan photos go into the vault; safe sweep of leftover camera files)
Facts (read from the code and seen on the phone): UltrasoundEditor stores the raw camera/gallery URI string in the scan's imageUris; nothing copies the photo, so Android may purge it and the scan keeps a dead link
(and, on the phone, a captured photo stayed as plain JPEG in cache/camera after the app was locked/killed with the camera open). The print report does not embed scan photos (placeholder only), so only the
photo viewer reads them. Backups store imageUris as plain strings (photos are not in backups; unchanged, labelled honestly since 3d).
Q63 (NEW storage/ScanPhotoRef.kt) pure helpers: `const val SCAN_PHOTO_REF_PREFIX = "vault:"`, `fun scanPhotoRef(fileName: String): String` ("vault:" + fileName), `fun isScanPhotoRef(value: String): Boolean`,
    `fun scanPhotoFileName(value: String): String?` -> the file name only when the value is a vault ref whose name matches `[A-Za-z0-9_-]+\.webp`; null for anything else (empty, other schemes, "..", "/", "\\", extra dots).
Q64 (storage/CameraCaptureHelper.kt) pure, testable core: `fun staleCaptureFiles(dir: File, olderThanMs: Long, nowMs: Long, referencedNames: Set<String>): List<File>` = regular files in dir older than the limit whose
    name is NOT in referencedNames; `clearStaleCaptures(context, olderThanMs, referencedUris: Set<String>)` deletes exactly those (referencedNames = last path segment of each referenced URI string). The old
    2-argument clearStaleCaptures is replaced (no second copy).
Q65 (state/AncState.kt, state/AncViewModel.kt) `AncEvent.ImportScanPhoto(patientId: String, sourceUri: Uri, preset: PhotoQualityPreset = PhotoQualityPreset.DETAILED)` and `AncEvent.ScanPhotoImported(patientId: String, ref: String)`.
    Reducer: ScanPhotoImported appends `ref` to the current ultrasoundDraft.values.imageUris only when a draft exists for that patient (otherwise state unchanged); never duplicates a ref already present.
    ViewModel: on ImportScanPhoto (vault must be Unlocked) on Dispatchers.IO: (1) sweep = CameraCaptureHelper.clearStaleCaptures(context, 1 hour, every imageUris string of every scan in the current state);
    (2) PatientAttachmentVault.saveAttachment(context, patientId, sourceUri, preset = preset); (3) dispatch ScanPhotoImported(patientId, scanPhotoRef(result.fileName)); (4) in a finally block
    CameraCaptureHelper.deleteIfCaptureTemp(context, sourceUri). Any non-cancellation exception -> Log.e + AttachmentFailed(SAVE_FAILED) (existing banner). CancellationException rethrown. If the vault is not
    Unlocked: deleteIfCaptureTemp and return. addAttachment also calls the sweep (same helper, same arguments) before saving.
Q66 (ui/UltrasoundEditor.kt) The camera result and the gallery result send `AncEvent.ImportScanPhoto(route.patientId, uri)` instead of appending the raw URI to the draft. Everything else in the editor is unchanged.
Q67 (ui/UltrasoundPhotoViewerDialog.kt + callers) The viewer takes the patientId (callers in WorkspaceScreen.kt and ProfileScreen.kt pass patient.id). For each photo string: a vault ref (scanPhotoFileName != null) resolves with
    PatientAttachmentVault.resolveAttachmentFile(context, patientId, fileName) and BoundedBitmap.decodeFile(path, 2048); a content/file URI keeps today's decodeStream path (legacy scans); anything else or any failure shows
    "Image unavailable" (existing testTag image_unavailable). Never throws.
Static checks (.agents/checks/check_r3g.py, STATIC-r3g):
 S45 ScanPhotoRef.kt defines the four names of Q63; no other file builds the "vault:" prefix literally.
 S46 CameraCaptureHelper.kt defines staleCaptureFiles and a 3-argument clearStaleCaptures; AncViewModel.kt calls clearStaleCaptures( at least twice (import and addAttachment) and passes scan imageUris.
 S47 AncState.kt defines ImportScanPhoto and ScanPhotoImported; AncViewModel.kt handles ImportScanPhoto, dispatches ScanPhotoImported, calls scanPhotoRef(, and deleteIfCaptureTemp( inside a finally.
 S48 UltrasoundEditor.kt contains ImportScanPhoto( and no longer appends a raw uri.toString() to imageUris (no `imageUris + uri.toString()` / `imageUris + pendingCameraUri`).
 S49 UltrasoundPhotoViewerDialog.kt references scanPhotoFileName and resolveAttachmentFile; WorkspaceScreen.kt and ProfileScreen.kt pass a patient id to UltrasoundPhotoViewerDialog( .
Unit: ScanPhotoRefTest (round trip; rejects "../x.webp", "a/b.webp", "x.jpg", "", "content://...", "vault:", "vault:a.b.webp"); CameraCaptureSweepTest with a temp folder (old+unreferenced -> listed; old+referenced -> kept; new -> kept;
 subfolders ignored); ScanPhotoImportStateTest for the reducer (append, no duplicate, no draft -> unchanged).
Device (orchestrator): add a scan photo through the Ultrasound editor, save the scan, open the viewer (photo from the vault, upright); a legacy/demo photo shows "Image unavailable"; the camera cache is empty after import;
 leftover older capture is swept at the next import; no crash.
Not in this round: migrating legacy scans' cache URIs into the vault (their photos are kept alive by the referenced-set rule), orphan vault files from removed draft photos, encryption of photos.

## Round 3h (the "delete only our own camera temp file" rule gets a real unit test)
Reviewers twice noted that deleteIfCaptureTemp (the rule that decides which file may be deleted after a photo import) has no unit test because it needs android.net.Uri and a Context.
Q68 (storage/CameraCaptureHelper.kt) Extract the rule as a pure top-level function with primitive inputs (no android types):
    `fun captureTempFile(cameraDir: File, expectedAuthority: String, scheme: String?, authority: String?, pathSegments: List<String>): File?`
    = the canonical file inside cameraDir that the URI points at, or null unless ALL hold: scheme == "content"; authority == expectedAuthority; pathSegments.size >= 2 and pathSegments[0] == "camera_photos";
    the resolved canonical candidate (cameraDir.canonicalFile + remaining segments) is a regular file whose parent is exactly cameraDir.canonicalFile (so no ".." segments, no sub-folders, no segment containing a separator,
    no symlink that resolves elsewhere). Never throws (any exception -> null).
    deleteIfCaptureTemp(context, uri) becomes a thin wrapper: captureTempFile(cameraDir(context), "${context.packageName}.fileprovider", uri.scheme, uri.authority, uri.pathSegments)?.delete() == true.
    Behaviour is unchanged. No second copy of the rule.
Static check (.agents/checks/check_r3h.py, STATIC-r3h): S50 CameraCaptureHelper.kt defines captureTempFile with no android.* type in its signature, and deleteIfCaptureTemp calls captureTempFile( and contains no "canonicalFile".
Unit (storage/CaptureTempFileTest.kt, temp folders): valid file -> returned; wrong scheme; wrong authority; wrong first segment; only one segment; missing file; directory instead of file; "..": ["camera_photos","..","x.jpg"];
 sub-folder ["camera_photos","sub","x.jpg"] with the file inside sub; a segment "a/b.jpg"; symlink inside cameraDir pointing to a file outside (skip with Assume if symlinks are unsupported) -> null; unrelated file name case.

## Round 3i (reuse: one due-date rule, one date parser) - owner principle "every rule is one function, recalled everywhere"
Audit pass 2 found: EDD = LMP + 280 written in GestationalAgeEngine (2x), IntakeDatingRule and PatientEditor (live preview); the constant 280 also in calculateForEdd and GestationalBanner; ISO date parsing in 9 places with
two different strictness rules (lenient trim in 5 places; canonical "must equal toString()" in IntakeDatingRule, SaveVisitCommand, RecordsRepository). No behaviour change is wanted except the one named in Q70.
Q69 (NEW model/IsoDates.kt) `fun parseIsoDateOrNull(value: String?): LocalDate?` (null/blank -> null; trims; LocalDate.parse; any exception -> null) and
    `fun parseCanonicalIsoDateOrNull(value: String?): LocalDate?` (no trimming; the parsed date's toString() must equal the input, so "2026-1-5", " 2026-01-05" and "2026-02-30" are null).
    `const val FULL_TERM_DAYS = 280L` and `fun eddFromLmp(lmp: LocalDate): LocalDate = lmp.plusDays(FULL_TERM_DAYS)`.
    Replace every other LocalDate.parse( outside model/DemoFixtures.kt and IsoDates.kt: lenient places (model/GestationalAgeEngine private copy, model/LongitudinalTimelineProjection private copy, model/ClinicalModels.kt:116,
    ui/AncDatePicker.kt:50, ui/report/PatientPrintHtmlGenerator.kt:88) call parseIsoDateOrNull; canonical places (state/IntakeDatingRule.kt, storage/SaveVisitCommand.kt:70, storage/RecordsRepository.kt:609) call
    parseCanonicalIsoDateOrNull. Keep each caller's existing null/false handling exactly.
Q70 Every "+ 280" / "280 -" / "/ 280f" for gestation uses FULL_TERM_DAYS / eddFromLmp: GestationalAgeEngine (calculateEstimatedDueDate LMP branches, calculateForEdd), state/IntakeDatingRule.kt, ui/GestationalBanner.kt progress,
    ui/PatientEditor.kt live preview: the Estimated Due Date text uses eddFromLmp(parseIsoDateOrNull(lmp)) and the current-GA text uses GestationalAgeEngine.calculateForLmp(...)?.formatted (the engine's cap is 320 days; the preview
    used 300 - the preview now follows the engine, this is the only intended behaviour change). The editor keeps showing nothing when the LMP does not parse.
Static check (.agents/checks/check_r3i.py, STATIC-r3i; strip comments first): S51 `LocalDate.parse(` appears in app/src/main only in model/IsoDates.kt and model/DemoFixtures.kt; S52 `plusDays(280` and `/ 280f` and `280 -` do not appear
    in app/src/main outside IsoDates.kt (FULL_TERM_DAYS is used instead); S53 IsoDates.kt defines the four names of Q69; S54 PatientEditor.kt, IntakeDatingRule.kt and GestationalBanner.kt reference eddFromLmp( or FULL_TERM_DAYS.
Unit (model/IsoDatesTest.kt): lenient: " 2026-01-05 " ok, "2026-1-5" null, "" / null / "abc" null; canonical: " 2026-01-05" null, "2026-01-05" ok, "2026-02-30" null, "2026-1-05" null; eddFromLmp(2026-01-01) == 2026-10-08;
    FULL_TERM_DAYS == 280. The existing GestationalAgeEngineTest, IntakeDatingRuleTest, ClinicalValidationTest, SaveCoordinatorTest and PatientPrintHtmlGeneratorTest must stay green untouched.
Device (orchestrator, QA copy): new patient editor shows the Estimated Due Date and current GA for an LMP; a patient's banner progress still draws; no crash.

## Round 3j (UI cluster 1 from UIUX_BACKLOG.md: N-1, N-2, N-3, N-5) - first round on the reorganised fleet (GLM tests, DeepSeek build, GLM + Claude review)
Q71 (ui/UltrasoundEditor.kt, calculated-GA card) The card content is a Column, not a Row: the "Calculated GA ..." text on top, and below it (only when the entered GA differs) the "Apply calculated GA (...)" TextButton aligned to the end
    with a full-width-friendly layout (no weight/width squeeze, the button text wraps normally). Keep testTags scanCalculatedGaCard and applyScanGa, the same strings, the same onClick events.
Q72 Plurals (res/values/plurals.xml NEW, res/values/strings.xml, ui/WorkspaceScreen.kt, ui/ProfileScreen.kt) `<plurals name="photos_count">`: one = "%d photo", other = "%d photos". The string `photos_count` is removed from strings.xml and all four call sites
    (WorkspaceScreen.kt x2, ProfileScreen.kt x2) use `pluralStringResource(R.plurals.photos_count, n, n)`.
Q73 (ui/ClinicalFormat.kt + callers) pure `internal fun attachmentDisplayTitle(title: String?, category: String, capturedAt: String): String`: a non-blank trimmed title wins; otherwise "$category" + " · " + the first 10 characters of capturedAt when
    they look like yyyy-MM-dd, else just the category; a blank category falls back to "Attachment". Callers that currently show `attachment.title ?: attachment.fileName` (WorkspaceScreen.kt attachment card; ClinicalAttachmentViewerDialog.kt header if it shows
    the title/file name) call this helper (reuse, one rule). The file name is never shown as a title.
Q74 (ui/AddAttachmentDialog.kt, res/values/strings.xml) The two photo-preview buttons show "Retake" and "Gallery" (strings attachment_retake, attachment_gallery) with no emoji characters; testTags and behaviour unchanged.
Static checks (.agents/checks/check_r3j.py, STATIC-r3j; strip comments first): S55 UltrasoundEditor.kt: inside the scanCalculatedGaCard Card the Text and the applyScanGa button are in a Column (no Row between the Card and the Text of calculated_ga_at_visit).
 S56 plurals.xml defines photos_count with one/other; strings.xml has no `name="photos_count"`; no `R.string.photos_count` anywhere; pluralStringResource(R.plurals.photos_count used at least 4 times.
 S57 ClinicalFormat.kt defines attachmentDisplayTitle; WorkspaceScreen.kt calls it and no longer contains `attachment.title ?: attachment.fileName`.
 S58 AddAttachmentDialog.kt contains no emoji (no char >= U+1F000 and no U+FE0F) and uses R.string.attachment_retake and R.string.attachment_gallery; strings.xml defines both.
Unit (ui/AttachmentDisplayTitleTest.kt): title wins and is trimmed; blank title + "Ultrasound" + "2026-10-31T10:11:12Z" -> "Ultrasound · 2026-10-31"; bad capturedAt ("", "x") -> "Ultrasound"; blank category -> "Attachment" (with and without date); never returns a file name.
Device (orchestrator): the Add scan editor card no longer squeezes; "1 photo" shows; an untitled attachment shows a readable title; the buttons have no emoji.

## Round 3k (UI cluster 2 from UIUX_BACKLOG.md: DV-3, DV-5, DV-8, DV-13, DV-15 and a new "kg kg" bug) - all confirmed again on the Honor 400 on 2026-10-31
Q75 (ui/ClinicalBadges.kt KeyFactTile) the label Text allows two lines (maxLines = 2, no clipping at font scale 1.0) so "Current gestational age" and "Agreed clinical EDD" are fully readable; the value Text is unchanged.
Q76 (ui/PatientListScreen.kt, the FlowRow that uses `Arrangement.End` for Arabic names) chips keep an 8 dp gap when end-aligned: `Arrangement.spacedBy(8.dp, Alignment.End)`. Non-Arabic cards unchanged.
Q77 (ui/ProfileScreen.kt, Prior Pregnancies cards) the summary line ("date · delivery method") and the outcome text are in a Column (summary on top, outcome below, both wrapping normally) instead of a Row with SpaceBetween; details line unchanged.
Q78 (ui/ClinicalFormat.kt + callers) pure `internal fun formatWeightKg(raw: String?): String?`: null/blank -> null; otherwise the trimmed text; when it does not already end with "kg" (case-insensitive, optionally after spaces) append " kg". Callers that print a visit weight
    (ProfileScreen.kt visit row "Weight: ...", and any workspace visit weight text found by grep for `.weight`) use it - one rule, no "74.5 kg kg" and no bare "74.5".
Q79 (ui/PatientListScreen.kt PatientCard) the high-risk card background is an OPAQUE blend of the surface and the error container (`androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.errorContainer, <the same 0.22 light / 0.40 dark fraction>)`),
    not a translucent colour, so the card no longer shows a second inner frame in the light theme. Borders, text colours and everything else stay.
Q80 (ui/AncApp.kt top bar) the subtitle text allows two lines (maxLines = 2) so "Maternal-Fetal Medicine & Obstetrics" is not cut; the clinic name stays one line.
RULE FOR ALL LANES (new): never write dead code, extra files, XML blocks, or comments only to satisfy a static check or a test. If a check looks wrong, implement the spec and say so in the final message; the orchestrator fixes the check.
Static checks (.agents/checks/check_r3k.py, STATIC-r3k; strip comments first): S59 ClinicalBadges.kt: KeyFactTile's label Text has maxLines = 2 (not 1). S60 PatientListScreen.kt: no `Arrangement.End` used for the chip FlowRow (needs `spacedBy(8.dp, Alignment.End)`) and the card background uses `lerp(`.
 S61 ProfileScreen.kt Prior Pregnancies: the summary Text and the outcome Text are not siblings of a Row with SpaceBetween (the block contains a Column and no `Arrangement.SpaceBetween`). S62 ClinicalFormat.kt defines formatWeightKg; ProfileScreen.kt contains no `${visit.weight} kg` literal and calls formatWeightKg(.
 S63 AncApp.kt: the subtitle Text has maxLines = 2.
Unit (ui/FormatWeightKgTest.kt): "74.5" -> "74.5 kg"; "74.5 kg" -> "74.5 kg"; "74.5KG" -> "74.5KG"; " 63 kg " -> "63 kg"; "" / "  " / null -> null; "63 kilograms" -> "63 kilograms kg" is NOT acceptable: treat text ending with "kilograms"/"kilogram" as already unit-bearing too.
Device (orchestrator): Delivery tab tiles show full labels; the Arabic card chips have a gap; prior-pregnancy cards are readable; the visit weight shows one "kg"; the high-risk card has no inner frame; the header subtitle shows fully.

## Round 3l (clinical correctness: tracker c09, c10, c11) - found by the audit and the round-3a review
Q81 (storage/RecordsRepository.kt, saveDating / saveUltrasoundScan / saveLabInvestigation / saveMedication) a save never reports "failed" after it wrote anything. Today `if (patientUpdated != 1) return ClinicalCommandOutcome.Rejected(STALE_PATIENT_REVISION)`
    returns normally inside `database.withTransaction`, so the record, the receipt side effects and the ultrasound unlink are COMMITTED while the user is told the save failed. Every Rejected decision that comes after the first write
    in those four functions must `throw ClinicalCommandAbort(failure)` (the class already exists and `execute` already maps it to Rejected) so Room rolls the whole transaction back. Decisions that come BEFORE any write may keep returning Rejected.
    The patient path (PatientCommandAbort) already does this; copy its shape, do not invent another one.
Q82 (storage/SaveVisitCommand.kt `withCommittedVisit`, storage/RecordsRepository.kt `SaveVisitCommand.toEntity`) visits the doctor enters are real: `isSyntheticEdit = false` in both (the app's one place that decides it). Fixture/demo visits already use false.
    strings.xml `patient_saved` says "Synthetic patient record saved." -> "Patient record saved." (real patients can now be saved; demo patients are debug-only per D8).
Q83 (state/AncViewModel.kt submitPreparedPatientCommand, state/IntakeDatingRule.kt, state/AncState.kt, ui/WorkspaceScreen.kt notice mapping, strings.xml) after a profile save the due-date update is no longer fire-and-forget.
    Pure `internal fun profileSavedNotice(datingAttempted: Boolean, datingSaved: Boolean): SavedNotice` in IntakeDatingRule.kt: PATIENT unless attempted && !saved, then the new `SavedNotice.PATIENT_DATING_NOT_UPDATED`.
    The ViewModel keeps the dating command result: `saved = result is ClinicalCommandOutcome.Committed`; when saved, the patients list in RecordCommitted comes from that dating snapshot (so the profile shows the new due date), otherwise from the profile snapshot;
    the notice is `profileSavedNotice(...)`. WorkspaceScreen's notice-to-string mapping handles the new value with `R.string.patient_saved_dating_not_updated` = "Patient saved, but the due date was not updated. Open Dating to set it." Everything else in that function is unchanged.
Static checks (.agents/checks/check_r3l.py, STATIC-r3l; strip comments first): S64 RecordsRepository.kt: no `if (patientUpdated != 1) return` remains; `throw ClinicalCommandAbort(` appears at least 4 times in the four clinical save functions.
 S65 SaveVisitCommand.kt and RecordsRepository.kt contain no `isSyntheticEdit = true`; strings.xml has no "Synthetic patient record saved". S66 AncViewModel.kt calls profileSavedNotice( and no longer has the bare statement `controller.execute(datingCmd, sessionEpoch) {}`; WorkspaceScreen.kt handles SavedNotice.PATIENT_DATING_NOT_UPDATED; strings.xml defines patient_saved_dating_not_updated.
Unit: storage/VisitSyntheticFlagTest.kt (withCommittedVisit gives isSyntheticEdit=false, other fields intact); state/ProfileSavedNoticeTest.kt (the 4 input combinations of profileSavedNotice; attempted=false,saved=true is still PATIENT).
Instrumented (written and compiled, NOT run by the pipeline; the orchestrator decides about running it on the phone): androidTest/.../storage/ClinicalSaveAtomicityTest.kt - with a stale expectedPatientRevision, a lab save and a medication save each return Rejected(STALE_PATIENT_REVISION) AND leave no lab/medication row and an unchanged patient revision. Copy EncryptedRecordsTest's withDatabase/command helpers.
Device (orchestrator): save a profile with an LMP and see the normal notice; saved visits show no "Synthetic edit" label.

## Round 3m (tracker c07: visit checks) - owner decisions 2026-10-31: BP alert levels = ACOG two levels; inputs = strict plausible ranges that BLOCK the save
Reuse rule: BP and weight are parsed in ONE place (model/VisitVitals.kt); the save check, the editor hint and both visit displays all call it.
Q84 (NEW model/VisitVitals.kt, pure Kotlin, no Android imports)
    `data class BloodPressure(val systolic: Int, val diastolic: Int)`
    `fun parseBloodPressure(raw: String?): BloodPressure?` - trims; accepts "120/80", "120 / 80", optional trailing "mmHg" (any case, optional space); integers only; null for blank or anything else.
    `fun parseWeightKg(raw: String?): Double?` - trims; accepts "68", "68.5", "68,5", optional trailing "kg"/"kilograms"/"kilogram" (any case, optional space); at most one decimal digit; null for blank or anything else.
    `fun isPlausibleBloodPressure(bp: BloodPressure): Boolean` = systolic in 60..260 && diastolic in 30..160 && systolic > diastolic.
    `fun isPlausibleWeightKg(kg: Double): Boolean` = kg in 30.0..250.0.
    `enum class BpLevel { NORMAL, RAISED, SEVERE }` and `fun bpLevel(raw: String?): BpLevel` - unparseable/blank -> NORMAL; SEVERE when systolic >= 160 or diastolic >= 110; else RAISED when systolic >= 140 or diastolic >= 90; else NORMAL. (ACOG; chosen by the owner.)
Q85 (state/AncState.kt DraftError + storage/SaveVisitCommand.kt `prepare`) new `DraftError` values DATE_IN_FUTURE, BLOOD_PRESSURE_INVALID, WEIGHT_INVALID. `prepare(state, operationUuid, newVisitId, today: LocalDate = LocalDate.now())`:
    date after `today` -> DATE_IN_FUTURE (after the existing required/invalid checks); a NON-blank blood pressure that does not parse or is not plausible -> BLOOD_PRESSURE_INVALID; a NON-blank weight that does not parse or is not plausible -> WEIGHT_INVALID.
    All applicable errors are returned together as one Rejected set. Blank BP/weight stay allowed (optional). The stored command text is unchanged (still the trimmed text the doctor typed).
Q86 (ui/VisitEditor.kt + strings.xml) the date field also maps DATE_IN_FUTURE -> `R.string.visit_date_in_future` "Visit date cannot be in the future."; the BP field shows `R.string.blood_pressure_invalid` "Enter blood pressure like 120/80 (top number higher than bottom)." and the weight field `R.string.weight_invalid` "Enter weight in kg between 30 and 250, for example 68.5." as field errors (same pattern the date field uses).
    While typing, when `bpLevel(draft.values.bloodPressure)` is not NORMAL, a hint line under the BP field shows the alert text of Q87 (no error styling needed, but it must not block saving).
Q87 (ui/ClinicalFormat.kt `internal fun bpAlertResource(raw: String?): Int?` -> `R.string.bp_alert_raised` for RAISED, `R.string.bp_alert_severe` for SEVERE, null for NORMAL; strings: bp_alert_raised "Raised blood pressure (140/90 or above) - recheck", bp_alert_severe "Severe-range blood pressure (160/110 or above) - urgent assessment needed".)
    The visit card in WorkspaceScreen.kt (the FactRow(R.string.blood_pressure ...) block) and the visit row/detail in ProfileScreen.kt (~lines 1055-1085) show that text under the BP value when bpAlertResource is non-null; severe uses the error container colours, raised uses the tertiary container. Use ONE small composable placed in ui/ClinicalBadges.kt (`BpAlertLabel(raw: String?, modifier)`) and call it from the three places (editor hint, workspace card, profile).
    Printed report is out of scope.
Static checks (.agents/checks/check_r3m.py, STATIC-r3m; strip comments first): S67 model/VisitVitals.kt exists with parseBloodPressure, parseWeightKg, isPlausibleBloodPressure, isPlausibleWeightKg, bpLevel, enum BpLevel and no `android.` import.
 S68 SaveVisitCommand.kt calls parseBloodPressure(, parseWeightKg( and both plausibility functions, has a `today` parameter, and AncState.kt DraftError lists the 3 new values. S69 no second parser: outside model/VisitVitals.kt no file under app/src/main contains `split("/")` applied to a blood pressure value or a regex for BP (grep `bloodPressure` lines near `split(`).
 S70 VisitEditor.kt uses the three new strings and calls bpLevel( or bpAlertResource(; strings.xml defines visit_date_in_future, blood_pressure_invalid, weight_invalid, bp_alert_raised, bp_alert_severe. S71 BpLevel/bpAlertResource is used by WorkspaceScreen.kt and ProfileScreen.kt via BpAlertLabel( and ClinicalBadges.kt defines `fun BpAlertLabel`.
Unit: model/VisitVitalsTest.kt (parsers: valid/invalid/edge strings incl. "120 / 80 mmHg", "120/80/90", "abc", "", "68,5", "68.55", "75.0 kg", "70 KG"; plausibility edges 60/30/260/160 and systolic==diastolic; bpLevel edges 139/89 NORMAL, 140/80 RAISED, 120/90 RAISED, 159/109 RAISED, 160/80 SEVERE, 120/110 SEVERE, "" NORMAL).
 storage/SaveVisitValidationTest.kt (prepare with a fixed `today`: future date rejected, today accepted, bad BP/weight rejected, blank accepted, multiple errors returned together, valid values stay unchanged in the command; build the AncState draft the way AncStateTest does).
 ui/BpAlertResourceTest.kt only if bpAlertResource can be tested without Android resources (it returns R ids; if not unit-testable, skip and say so).
Device (orchestrator): a future date, "12080", "300/80" and weight "5" are refused with the field messages; 145/92 shows the raised hint while typing and the label on the saved visit; 165/105 shows severe; 120/80 shows nothing.

## Round 3n (tracker c08: dating gaps) - owner decisions 2026-10-31: IVF embryo days 3, 5, 6; ACOG redating = SUGGESTION ONLY; LMP cycle length NOT now (needs a stored field + migration, own round later)
Reuse rule: the allowed embryo ages live in ONE place (`ClinicalVocabulary.embryoAges`) and the IVF numbers in ONE pair of functions; no literal 3/5/17/19/261/263 may remain elsewhere.
Q88 (model/IsoDates.kt next to eddFromLmp; model/ClinicalModels.kt; model/GestationalAgeEngine.kt; storage/ClinicalCommands.kt)
    `ClinicalVocabulary.embryoAges = listOf(3, 5, 6)`.
    IsoDates.kt: `fun ivfGaDaysAtTransfer(embryoAgeDays: Int): Long = 14L + embryoAgeDays` and `fun ivfEddFromTransfer(transfer: LocalDate, embryoAgeDays: Int): LocalDate = transfer.plusDays(FULL_TERM_DAYS - ivfGaDaysAtTransfer(embryoAgeDays))`.
    (day 3: GA 17d at transfer, EDD transfer+263; day 5: 19d, +261; day 6: 20d, +260.) GestationalAgeEngine.calculateForIvf and calculateEstimatedDueDate use these two functions and `embryoAgeDays in ClinicalVocabulary.embryoAges`; their own 17/19/263/261 literals and `!= 3 && != 5` tests are removed.
    ClinicalCommands.kt (the 4 places that test 3/5: isStructurallyValid x2 and prepare x2) use `in ClinicalVocabulary.embryoAges`. Days 4 and 7 stay invalid. Behaviour for 3 and 5 is unchanged.
Q89 (NEW model/Redating.kt, pure Kotlin, no Android imports)
    `data class RedatingAdvice(val differenceDays: Int, val thresholdDays: Int)` (differenceDays = scan GA minus dating GA, signed)
    `fun redatingThresholdDays(datingGaDays: Long): Int` - ACOG table on the GA by the CURRENT dating: <= 62 days (up to 8w6d) -> 5; 63..111 (9w0d to 15w6d) -> 7; 112..153 (16w0d to 21w6d) -> 10; 154..195 (22w0d to 27w6d) -> 14; >= 196 (28w0d or more) -> 21.
    `fun redatingAdvice(datingGaDays: Long, scanGaDays: Long): RedatingAdvice?` - null when |scan - dating| <= threshold; otherwise the advice. Suggestion only, nothing is changed automatically.
Q90 (ui/DatingEditor.kt + strings.xml) a third chip "Day 6" (`R.string.embryo_day_6` = "Day 6", testTag "embryoDay6") beside Day 3 and Day 5 (same pattern; Day 5 stays the default when empty); the profile/workspace "Day $age" displays already work for 6.
Q91 (ui/UltrasoundEditor.kt + strings.xml) inside the existing scanCalculatedGaCard Column, when the entered scan GA (weeks, days both parse as non-negative ints, days 0..6) and `calculatedGa` exist, call `redatingAdvice(calculatedGa.totalDays, weeks*7+days)`; when non-null show a Text (testTag "redatingHint")
    `R.string.redating_hint` = "Scan GA differs from the current dating by %1$d days (more than %2$d). Consider using the scan due date in Dating." with %1$d = abs(differenceDays), %2$d = thresholdDays. Nothing is applied by it; the existing apply button stays.
Static checks (.agents/checks/check_r3n.py, STATIC-r3n; strip comments first): S72 IsoDates.kt defines ivfGaDaysAtTransfer and ivfEddFromTransfer; ClinicalModels.kt has `embryoAges = listOf(3, 5, 6)`; GestationalAgeEngine.kt and ClinicalCommands.kt contain none of `17L`, `19L`, `263L`, `261L`, `!= 3 && `, `!= 5`, and both use ClinicalVocabulary.embryoAges; the engine calls both ivf functions.
 S73 model/Redating.kt defines RedatingAdvice, redatingThresholdDays, redatingAdvice and has no `android.` import. S74 DatingEditor.kt has testTag("embryoDay6") and R.string.embryo_day_6; strings.xml defines embryo_day_6 and redating_hint. S75 UltrasoundEditor.kt calls redatingAdvice( and has testTag("redatingHint") and R.string.redating_hint, with the call inside the scanCalculatedGaCard block.
Unit: model/IvfDatingTest.kt (EDD for transfer 2026-01-01: day 3 -> 2026-09-21, day 5 -> 2026-09-19, day 6 -> 2026-09-18; GA on the transfer date = 17/19/20 days = 2w3d/2w5d/2w6d via GestationalAgeEngine.calculate with dating method IVF_TRANSFER; days 4 and 7 -> null GA and null EDD; ClinicalVocabulary.embryoAges == [3,5,6]; ClinicalValidation/commands accept 6 and still reject 4 - use the same helpers ClinicalValidationTest/ClinicalCommandsTest use).
 model/RedatingTest.kt (every table edge: dating GA 62/63/111/112/153/154/195/196 days -> thresholds 5/7/7/10/10/14/14/21; at each threshold diff == threshold -> null, diff == threshold+1 -> advice with that threshold; negative differences give a negative differenceDays; diff 0 -> null).
Device (orchestrator): Day 6 chip appears and saves an IVF dating with a sensible due date; the ultrasound editor shows the redating hint for a scan GA 10+ days off and none for a close one.

## Round 3o (tracker c12: printed report drops or breaks text containing "<" and "&") - found by reading ui/report/PatientPrintHtmlGenerator.kt: 58 string interpolations put raw patient/clinic text into HTML with NO escaping anywhere
Impact: a note such as "BP <90 systolic" loses everything from "<" on (the WebView reads it as a tag), "&" can be mangled, and typed markup would be rendered as markup in a medical printout. Owner decision: none needed (pure correctness).
Q92 (NEW ui/report/HtmlEscape.kt) `internal fun escapeHtml(text: String?): String` - null -> ""; replaces & first, then < > " ' with &amp; &lt; &gt; &quot; &#39;. The ONE escape function; nothing else may duplicate it.
Q93 (ui/report/PatientPrintHtmlGenerator.kt) every piece of text that comes from the patient record or from settings (names, case id, phone, address, histories, obstetric notes, previous-pregnancy fields, dating notes, visit date/BP/weight/findings/notes, scan fields and impression, lab name/result/unit/range, medication name/dosage/route/frequency/notes, clinical attention/alerts, clinic name from settings, any free text) is passed through escapeHtml at the point it enters the HTML.
    Escape exactly once: values that are already HTML fragments built inside the generator (badges, `resultHtml`, row/list builders, the logo data URI, CSS classes) are NOT escaped again; numbers and the generator's own constant text are left alone.
    Where a helper builds a fragment from user text, escape inside the helper. Line breaks/spacing/layout and every other output stay byte-identical for text without special characters (the existing PatientPrintHtmlGeneratorTest must keep passing unchanged).
Static checks (.agents/checks/check_r3o.py, STATIC-r3o; strip comments first): S76 HtmlEscape.kt defines `internal fun escapeHtml(`; PatientPrintHtmlGenerator.kt calls escapeHtml( at least 25 times; no other file under app/src/main defines a function that replaces "<" with "&lt;".
 S77 in PatientPrintHtmlGenerator.kt every `${...}` interpolation inside an HTML string either starts with escapeHtml( or is on a short explicit allow-list the check names (numbers/indexes, `logoBase64`, generated fragment variables such as resultHtml, css/class variables); the check prints the offending expression when it fails. Build the allow-list by reading the file; keep it small and justified in comments.
Unit (ui/report/PatientPrintHtmlEscapeTest.kt): escapeHtml cases (null, "", "a<b", "5 & 6", quotes, already-escaped "&lt;" becomes "&amp;lt;", unicode/Arabic unchanged).
 Generator behaviour: build a patient whose every printed string field contains the marker `<b>&"'` (name, address, histories, notes, previous pregnancy, dating notes, visits findings/notes/BP, scans, labs name/result/unit/range, medications, clinical attention) and settings clinicName likewise; the generated HTML must contain NO raw `<b>&"'` and no raw "<b>" from any user field, must contain "&lt;b&gt;&amp;&quot;&#39;" at least once per printed section, and "BP <90" in a visit note must print as "BP &lt;90". The generator's own markup (tables, badges) must still be present. Reuse the patient/visit/scan builders already in PatientPrintHtmlGeneratorTest.kt (copy the helper shapes, do not edit that file).
Device (postponed list): print preview of a note containing < and & shows the text.

## Round 3p (UI/UX cluster 3, print preview: DV-12; no device available, so static + unit checks only, device pass postponed)
Measured, not changed: the Print PDF button is white on ActionRose #934F5B = contrast about 6.0:1 (passes WCAG AA 4.5:1), so the "low contrast" note in DEVICE_TEST_REPORT is not a contrast failure; leave the button colours alone.
Q94 (ui/report/PatientPrintHtmlGenerator.kt) every printed section title is shown once: remove all 9 `<span class="en-sub">...</span>` lines (8 section titles plus the ultrasound photo section) inside `<div class="section-title">` blocks (they repeat the same English words in capitals) and the now-unused `.section-title .en-sub` CSS rule.
    Section titles keep their visible text exactly as the first `<span>` has it. Nothing else in the HTML changes (escaping from round 3o stays).
Q95 (ui/report/PatientTwoPageReportDialog.kt) the dialog title "Clinical Medical Record" is no longer cut to "Clinical Medic...": allow two lines for it (`maxLines = 2`, keep the ellipsis as a last resort); the patient name line stays one line. Nothing else in the dialog changes (buttons, colours, test tags).
Static checks (.agents/checks/check_r3p.py, STATIC-r3p; strip comments first): S78 PatientPrintHtmlGenerator.kt contains no `en-sub` at all (neither the span nor the CSS rule) and still has at least 8 `class="section-title"` blocks.
 S79 PatientTwoPageReportDialog.kt: the Text with "Clinical Medical Record" has `maxLines = 2` (not 1) and the file still contains testTag("printReportButton"), testTag("shareReportButton"), testTag("closeReportDialog").
Unit (ui/report/PrintSectionTitlesTest.kt): generate the report HTML for a full patient (reuse the builders of PatientPrintHtmlGeneratorTest.kt, copy their shape) and assert: no "en-sub" in the HTML; each of the 8 section titles appears exactly once in its original wording ("Pregnancy Dating", "Obstetric & Medical History" as the HTML text the generator prints for it, "Previous Pregnancies & Obstetric Outcomes", "Antenatal Care Visits Log", "Ultrasound Biometry & Fetal Surveillance", "Laboratory Investigations", "Active Medications & Prophylaxis", "Delivery Management Plan & Attestation") and none of their capital-letter duplicates ("PREGNANCY DATING", "ANTENATAL CARE VISITS LOG", ...) is present. The ultrasound photo section ("Clinical Imaging & Ultrasound Scans") appears once when the patient has scan photos.
 Existing PatientPrintHtmlGeneratorTest and PatientPrintHtmlEscapeTest must keep passing unchanged.
Device (postponed list): print preview shows each title once and the dialog title fully.

## Round 3q (tracker c16 + c20; owner 2026-10-31: print shows the BP alert; a SEVERE reading also shows on the patient list and in the attention banner; raised stays on the visit and print only)
Reuse rule: levels come ONLY from `bpLevel` (model/VisitVitals.kt); the "latest severe reading" rule is ONE function used by the list card and the workspace banner.
Q96 (model/VisitVitals.kt, pure Kotlin) `data class SevereBpAttention(val bloodPressure: String, val date: String)` and `fun severeBpAttention(visits: List<DemoVisit>): SevereBpAttention?`:
    consider only visits whose blood pressure parses (parseBloodPressure != null); the LATEST of them (greatest ISO date; same date -> greatest revision) decides; return non-null only when `bpLevel(thatVisit.bloodPressure) == BpLevel.SEVERE`, with the visit's trimmed BP text and its date; otherwise null.
    An older severe reading followed by a later normal or raised reading gives null. No visits / none parseable -> null.
Q97 (ui/PatientListScreen.kt + strings.xml) a patient whose `severeBpAttention(patient.visits)` is non-null shows a badge `R.string.bp_badge_severe` = "Severe BP" on its card, styled like the high-risk badge (error container colours; reuse the existing badge composable pattern, no second badge implementation) and the card's attention row also shows `R.string.bp_attention_severe` = "Severe-range blood pressure at the last visit: %1$s (%2$s)" (BP, date) on its own line, in addition to any stored clinicalAttention text (which is never modified or overwritten). Cards with no severe reading are unchanged.
Q98 (ui/WorkspaceScreen.kt) the attention banner area shows the same `bp_attention_severe` line (computed, not stored) whenever `severeBpAttention` is non-null, even when `patient.clinicalAttention` is blank, together with the stored text when both exist. testTag "severeBpAttention" on the computed line. The attention dialog and the stored field are untouched.
Q99 (ui/report/PatientPrintHtmlGenerator.kt) in the visits table the blood-pressure cell prints, under the value, a short label when `bpLevel(v.bloodPressure)` is RAISED ("Raised BP - recheck") or SEVERE ("Severe-range BP - urgent assessment"), as small text with CSS class `bp-alert` (SEVERE also `bp-alert-severe`; add the two small CSS rules, red for severe, amber for raised); NORMAL prints nothing extra. Text is a constant inside the report package (the print generator has no Context in tests); it goes through the existing escaping rules (constants need none). Nothing else in the HTML changes.
Static checks (.agents/checks/check_r3q.py, STATIC-r3q; strip comments first): S80 VisitVitals.kt defines severeBpAttention and SevereBpAttention and still contains the only thresholds (160 / 110 / 140 / 90 appear in no other main file besides VisitVitals.kt and plausibility ranges are unchanged).
 S81 PatientListScreen.kt and WorkspaceScreen.kt call severeBpAttention( and use R.string.bp_badge_severe / R.string.bp_attention_severe; strings.xml defines bp_badge_severe and bp_attention_severe; WorkspaceScreen.kt has testTag("severeBpAttention"). S82 PatientPrintHtmlGenerator.kt calls bpLevel( and contains `bp-alert-severe`; no `split("/")` BP parsing or threshold literal added there.
Unit: model/SevereBpAttentionTest.kt (no visits; one normal; one severe -> value+date; older severe then later normal -> null; older normal then later severe -> non-null; later visit with unparsable BP is ignored so an earlier severe still counts? NO: ignored visits are skipped, so the latest PARSEABLE decides - assert that; same-date tie decided by revision; boundaries 159/109 -> null, 160/80 and 120/110 -> non-null; BP text with spaces/"mmHg" is returned trimmed).
 ui/report/PrintBpAlertTest.kt (generate the report HTML for a patient with visits 120/80, 145/92, 165/105: contains "Raised BP - recheck" once and "Severe-range BP - urgent assessment" once, `bp-alert-severe` present, and none for 120/80; existing PatientPrintHtml* tests unchanged and passing).
Device (postponed list): patient list shows the "Severe BP" badge + line after saving a 165/105 visit, the workspace banner shows it, printing shows labels.

## Round 3r (tracker r08; owner decision D15, 2026-10-31: the screen-capture block is removed in release builds too, for good)
Q100 (MainActivity.kt) the `if (!BuildConfig.DEBUG) { window.setFlags(FLAG_SECURE, FLAG_SECURE) }` block is removed completely (and any import that becomes unused). No other behaviour changes: the PIN lock, the vault lock and everything else stay exactly as they are.
Q101 (androidTest AncWorkflowTest.recordsWindowKeepsSecureFlagAndGateExplainsDraftPolicy) its flag check now asserts the flag is NOT set in every build (D15), not only in debug; the rest of that test is untouched (it still fails for the draft-policy text; that is tracked in d06/f05).
Static check (.agents/checks/check_r3r.py, STATIC-r3r; strip comments first): S83 no occurrence of `FLAG_SECURE` in any file under app/src/main; MainActivity.kt still calls enableEdgeToEdge() and still contains setContent.
Device (postponed list): in a release build a screenshot is allowed and the recents card shows the screen.

## Round 3s (reuse audit finding: previous caesarean/vaginal counts were worked out three different ways) - owner: reuse principle, one function per rule
Found by reading the three sites: PatientListScreen.projectDashboardRow and ProfileScreen counted a caesarean as `birthMethod == "Cesarean" || deliveryBy == "CS"` and a vaginal birth as `birthMethod == "Vaginal" || deliveryBy == "Vaginal"` (exact, case-insensitive); PatientPrintHtmlGenerator counted `birthMethod contains "Cesarean" || outcomeText contains "CS"` and `birthMethod contains "Vaginal" || outcomeText contains "SVD"` and ignored deliveryBy. Same patient, different history on different screens. The model vocabulary is birthMethod in {Vaginal, Cesarean}, deliveryBy in {CS, Vaginal, Hysterotomy}.
Q102 (NEW model/ObstetricHistory.kt, pure Kotlin, no Android imports) ONE counting rule and ONE summary text:
    `data class PreviousDeliveryCounts(val cesarean: Int, val vaginal: Int, val stillbirth: Int, val earlyLoss: Int)` and `fun previousDeliveryCounts(pregnancies: List<DemoPreviousPregnancy>): PreviousDeliveryCounts`.
    A pregnancy counts as CESAREAN when birthMethod equals "Cesarean" OR deliveryBy equals "CS" (ignore case, trimmed). VAGINAL when birthMethod equals "Vaginal" OR deliveryBy equals "Vaginal". If the structured fields say one route it wins; one record is never counted as both (cesarean wins if both structured fields conflict, never double-count). ONLY when birthMethod AND deliveryBy are both null/blank, fall back to the free text `outcomeText`: whole-word, case-insensitive "CS" or "Cesarean" -> cesarean, "SVD" or "Vaginal" -> vaginal (no substring matching: "CSF" is not a CS).
    Hysterotomy deliveries are NOT counted as cesarean or vaginal (unchanged behaviour, noted for the owner as a possible later decision).
    stillbirth: outcome == "Stillbirth" || outcomeText contains "Stillbirth" or "IUFD" (ignore case); earlyLoss: outcome == "EarlyPregnancyLoss" || outcomeText contains "Miscarriage" or "Loss" (ignore case) - exactly the profile's current rules, now shared.
    `fun previousDeliverySummary(patient: DemoPatient): String` with g = gravida ?: 1, p = parity ?: 0, a = abortion ?: 0, c = previousDeliveryCounts(patient.previousPregnancies):
      g == 1 && p == 0 && a == 0 && pregnancies empty -> "Primigravida"; p == 0 && pregnancies empty -> "P0 · No prior deliveries";
      cesarean > 0 || vaginal > 0 -> "Prev " + ("<n> CS" and/or "<n> SVD", CS first, joined by ", ") + (stillbirth > 0 ? " · <stillbirth> IUFD" : "");
      else stillbirth > 0 -> "Prior Stillbirth (IUFD)"; earlyLoss > 0 -> "<n> Prior Miscarriages"; p > 0 -> "Parous (P<p>)"; pregnancies not empty -> "<size> Prior Pregnancies"; else "No prior deliveries".
Q103 (ui/PatientListScreen.kt projectDashboardRow, ui/ProfileScreen.kt, ui/report/PatientPrintHtmlGenerator.kt) the three places delete their own counting and summary code and call `previousDeliverySummary(patient)` (and `previousDeliveryCounts` if they need a count). No CS/SVD/stillbirth/miscarriage counting remains anywhere else in app/src/main. The printed report's old "+" wording ("Prev 1 CS + 2 SVD") and its "Primigravida / N/A" fallback become the shared text; everything else in each screen and in the HTML stays as it is.
Static checks (.agents/checks/check_r3s.py, STATIC-r3s; strip comments first): S84 model/ObstetricHistory.kt defines PreviousDeliveryCounts, previousDeliveryCounts, previousDeliverySummary and has no `android.` import.
 S85 PatientListScreen.kt, ProfileScreen.kt and PatientPrintHtmlGenerator.kt each call previousDeliverySummary( and none of the three (nor any other main file besides ObstetricHistory.kt) contains `"Cesarean"` compared to birthMethod, `"CS"` compared to deliveryBy, `contains("Cesarean"`, `contains("CS"`, `contains("SVD"` or `contains("Stillbirth"`.
Unit: model/ObstetricHistoryTest.kt: every combination of birthMethod/deliveryBy (Cesarean/CS, Vaginal/Vaginal, conflicting values count once, Hysterotomy counts as neither, both null falls back to outcomeText whole words, "CSF" and "Vaginally"? is not matched by substring rule only whole words, case and spaces ignored), stillbirth and early-loss rules, and each branch of previousDeliverySummary with exact strings including multiple pregnancies (2 CS + 1 SVD -> "Prev 2 CS, 1 SVD"), IUFD suffix with the real count.
 ui/report/PrintObstetricSummaryTest.kt and the list row: for a patient with mixed records assert `projectDashboardRow(patient).prevSummary == previousDeliverySummary(patient)` and that the generated print HTML contains that same string (one rule, three screens agree). Existing print and list tests keep passing; if an old test pinned the old print wording ("+" or "Primigravida / N/A") the test designer updates that single expected string and lists it.
Device (postponed list): the same patient shows the same history on the list, the profile and the printed report.

## Round 3t (reuse audit: gestational-age countdown, term classification, progress and short text were computed in screens and copied in a test)
Found: GestationalBanner.kt computes `isPostTerm = gaResult.isPostTerm || gaResult.weeks >= 42` (the engine already sets isPostTerm for weeks >= 42), `isLateTerm = weeks == 41`, `progress = totalDays / FULL_TERM_DAYS`, `daysDiff = ChronoUnit.DAYS.between(asOfDate, edd)`; PatientListScreen.kt (~121) and ProfileScreen.kt (~375) hand-format "${weeks}+${days} wks"; ui/GestationalBannerLogicTest.kt copies the progress and days-to-EDD formulas instead of calling production.
Q104 (model/GestationalAgeEngine.kt) the ONE place for these rules:
    on `GestationalAgeResult`: `val isLateTerm: Boolean get() = weeks == 41`, `val progressFraction: Float get() = (totalDays / FULL_TERM_DAYS.toFloat()).coerceIn(0f, 1f)`, `val shortWeeksText: String get() = "${weeks}+${days} wks"`; (isPostTerm stays as it is: weeks >= 42; a result is never both late-term and post-term).
    top level `fun daysToEdd(edd: LocalDate, asOf: LocalDate): Long = ChronoUnit.DAYS.between(asOf, edd)`; the engine's own `calculateForEdd` uses it instead of its own ChronoUnit call. Results for every existing input stay identical.
Q105 (ui/GestationalBanner.kt, ui/PatientListScreen.kt, ui/ProfileScreen.kt) the banner uses gaResult.isPostTerm, gaResult.isLateTerm, gaResult.progressFraction and daysToEdd(edd, asOfDate) (no ChronoUnit, no `weeks >= 42`, no division by FULL_TERM_DAYS left in the banner); the list row and the profile use `gaResult.shortWeeksText`. Everything displayed stays identical (same strings, same colours, same testTags).
Q106 (ui/GestationalBannerLogicTest.kt) rewritten to call the production members (progressFraction, daysToEdd, isLateTerm, isPostTerm) with the same scenarios and expected numbers it has today; it must contain no copy of the formulas (no `280f`, no ChronoUnit).
Static checks (.agents/checks/check_r3t.py, STATIC-r3t; strip comments first): S86 GestationalAgeEngine.kt defines isLateTerm, progressFraction, shortWeeksText and `fun daysToEdd(`; and its own calculateForEdd calls daysToEdd.
 S87 GestationalBanner.kt contains none of `ChronoUnit`, `weeks >= 42`, `FULL_TERM_DAYS`; PatientListScreen.kt and ProfileScreen.kt contain no `+${` ... `wks` hand formatting (use shortWeeksText); the three screens reference shortWeeksText/progressFraction/daysToEdd as described. S88 GestationalBannerLogicTest.kt contains no `280f`, no `ChronoUnit` and calls progressFraction and daysToEdd.
Unit (model/GestationalDisplayTest.kt, NEW): isLateTerm true only at weeks 41 (40w6d false, 41w0d and 41w6d true, 42w0d false) and never together with isPostTerm; progressFraction at 0 days = 0f, 140 days = 0.5f, 280 = 1f, 320 = 1f (coerced); shortWeeksText "21+5 wks"; daysToEdd positive/zero/negative and across a leap day. The rewritten GestationalBannerLogicTest and the existing engine tests keep passing.
Device (postponed list): banner shows the same trimester/late-term/post-term badges, progress bar and countdown as before.

## Round 3u (draft recovery, step 1 of 3: draft STORAGE only; owner decisions D9 + 2026-10-31 answers; the app has no real data, so no data-preservation worry about the migration)
Scope of this step: store, read, replace and delete drafts safely inside the encrypted vault database, with tests. NO ViewModel/screen/alert changes (steps 2 and 3). Design source: .agents/pipeline/drafts/DESIGN_ANALYSIS.md (read-only analysis by the fleet; verify what you rely on in the code).
Owner rules: one draft per patient per screen kind, the newest replaces the older; no expiry; stored ONLY in the encrypted Room/SQLCipher database (never a file, SharedPreferences or log); NOT in backups; a backup restore removes all drafts.
Q107 (NEW storage/DraftRecord.kt + storage/DraftCodec.kt, pure Kotlin + org.json like EncryptedBackupManager; no Android imports except what org.json needs in unit tests: check how other unit tests use org.json, the project has testImplementation org.json)
    `enum class DraftScreenKind { VISIT, PATIENT_PROFILE, HISTORIES, PREVIOUS_PREGNANCY, PATIENT_STATUS, DATING, ULTRASOUND, LAB, MEDICATION }`
    `const val NEW_PATIENT_DRAFT_KEY = "__NEW_PATIENT__"` (the patient id used for a brand-new patient that has no row yet).
    `enum class DraftEditorKind { CREATE, EDIT }`.
    `data class DraftRecord(val patientId: String, val screenKind: DraftScreenKind, val editorKind: DraftEditorKind, val recordId: String?, val baseRevision: Long?, val fields: Map<String, String>, val updatedAtMillis: Long)`.
    DraftCodec: `fun encodeDraftFields(fields: Map<String, String>): String?` and `fun decodeDraftFields(json: String): Map<String, String>?`: canonical JSON object of string values (keys sorted for a stable result); unicode, Arabic, quotes, backslashes, newlines and emoji round-trip exactly; null/decode failure on malformed input (never throws); `MAX_DRAFT_PAYLOAD_BYTES = 65_536`: a payload whose UTF-8 JSON is larger returns null from encode (the caller treats it as "do not store"); non-string JSON values are ignored on decode; unknown keys are kept (forward compatible).
Q108 (storage/RecordEntities.kt, RecordsDao.kt, RecordsDatabase.kt, RecordsMigration.kt, RecordsRepository.kt)
    Room entity `DraftEntity` table `drafts`: patient_id TEXT NOT NULL, screen_kind TEXT NOT NULL, editor_kind TEXT NOT NULL, record_id TEXT, base_revision INTEGER, payload TEXT NOT NULL, updated_at INTEGER NOT NULL; PRIMARY KEY (patient_id, screen_kind); index on patient_id; NO foreign key to patients (a new patient has no row).
    Database version 5 -> 6 with `RecordsMigration.from5To6(expectedVaultUuid)` following exactly the pattern of from4To5 (same safety checks, same vault metadata schema_version update); the schema-version constant and every place that validates the version are updated together.
    DAO: upsertDraft (replace on conflict), draft(patientId, screenKind), draftsForPatient(patientId), deleteDraft(patientId, screenKind), deleteDraftsForPatient(patientId), deleteAllDrafts().
    Repository (suspend, same style as the other repository methods): `saveDraft(DraftRecord): Boolean` (false when the payload is too large or encoding fails; nothing stored then), `loadDraft(patientId, kind): DraftRecord?` (null for a missing or undecodable row), `loadDrafts(patientId): List<DraftRecord>`, `deleteDraft(patientId, kind)`, `deleteDrafts(patientId)`.
    `restoreCanonicalBackup` deletes ALL drafts in the same transaction as the restore. The backup export (EncryptedBackupManager/VaultController export) is NOT changed and must contain no draft data.
Static checks (.agents/checks/check_r3u.py, STATIC-r3u; strip comments first): S89 RecordsDatabase.kt declares version = 6 and lists DraftEntity; RecordsMigration.kt defines from5To6; RecordsRepository's schema version constant is 6.
 S90 DraftEntity has primary key on patient_id + screen_kind and NO ForeignKey; EncryptedBackupManager.kt and VaultController.kt contain no "draft" (case-insensitive); restoreCanonicalBackup deletes drafts.
 S91 drafts are only ever written through the DAO: no SharedPreferences, no File write, no Log call anywhere in DraftCodec.kt, DraftRecord.kt or the draft methods of RecordsRepository.kt.
Unit (JVM, storage/DraftCodecTest.kt): encode/decode round trip for empty map, one field, many fields, Arabic text, emoji, quotes, backslash, newlines, very long value just under and just over the cap (over -> null), malformed JSON, JSON with a number value (ignored), unknown keys kept, stable key order; DraftRecord equality.
Instrumented (written and COMPILED by the test lane; the orchestrator runs it on an emulator): in androidTest storage/EncryptedRecordsTest.kt update the schema-table list to include `drafts` and the two schema-version expectations to 6, and add tests: save then load returns the same record; saving the same patient+kind replaces (one row); two kinds for one patient coexist; delete one kind keeps the other; deleteDrafts(patient) removes all of that patient only; NEW_PATIENT_DRAFT_KEY works without a patients row; oversize payload returns false and stores nothing; restoreCanonicalBackup clears drafts; the raw database file does not contain a recognisable plain-text marker string from a saved draft (encryption).
Gates by the orchestrator: unit suite, release compile, androidTest compile, STATIC-r3u, then the instrumented suite on the emulator.

## Round 3v (draft recovery, step 2 of 3: AUTO-SAVE of open editors; step 3 = the Continue / Discard alert; owner decisions D9 + 2026-10-31 answers)
Scope: while an entry screen is open and has unsaved changes, save its values as a draft (step 1 storage: DraftRecord/DraftCodec/RecordsRepository.saveDraft etc., already committed) about one second after typing stops; save immediately before the app locks or goes to the background; remove the draft when the screen closes by save, by cancel or by confirmed discard. NO alert, NO new screen, NO UI file change (step 3). The 9 screens: visit, patient profile (new and edit), other histories, previous pregnancy, patient status, dating, ultrasound, lab, medication. Not covered: attachment dialog, search box, PIN screens.
Design: .agents/pipeline/drafts/DESIGN_ANALYSIS.md section 3 (read-only; verify against the code). Every draft class in state/AncState.kt already has `values`, `original`, `isDirty`, `editorSessionId`; the open editor is the one non-null among AncState.draft, patientDraft, historiesDraft, pregnancyDraft, statusDraft, datingDraft, ultrasoundDraft, labDraft, medicationDraft (only one is open at a time).
Q109 (NEW state/DraftMapping.kt, pure Kotlin, no Android imports)
    `data class DraftSlot(val patientId: String, val kind: DraftScreenKind)`; a new unsaved patient uses patientId = NEW_PATIENT_DRAFT_KEY.
    `fun activeDraftSlot(state: AncState): DraftSlot?` : the slot of the open editor, null when none.
    `fun draftRecordFor(state: AncState, nowMillis: Long): DraftRecord?` : non-null ONLY when an editor is open AND isDirty. Fields: every text field of `values` under its Kotlin property name (booleans as "true"/"false"; PatientStatusDraft also stores targetStatus and reason; UltrasoundDraft does NOT store imageUris: they are temporary file links that go stale). editorKind = CREATE when the record is new (visit without visitId, new patient, a record whose base revision is null) else EDIT; recordId = the visit/pregnancy/scan/lab/medication id (null for patient-level screens); baseRevision = the revision the editor was opened on (Long).
    `fun applyDraftRecord(state: AncState, record: DraftRecord): AncState` : puts the stored fields into the `values` of the open editor of the matching slot; keeps original/base revisions/ids from the freshly opened editor; sets a NEW editorSessionId (UUID, never the stored one); clears errors; unknown or missing fields keep the editor's current value; returns the state unchanged when no matching editor is open. Ultrasound keeps the fresh editor's imageUris.
    `fun closedDraftSlot(before: AncState, after: AncState): DraftSlot?` : the slot of an editor that was open in `before` and is no longer open (or no longer dirty) in `after`; null otherwise. It is ALWAYS null unless both states have vaultState Unlocked (a lock or any vault state change is not a discard: the draft must survive a lock).
Q110 (NEW state/DraftAutosaver.kt + storage/VaultController.kt)
    `interface DraftStore { suspend fun save(record: DraftRecord): Boolean; suspend fun delete(slot: DraftSlot) }` and `class DraftAutosaver(scope: CoroutineScope, store: DraftStore, debounceMillis: Long = 1000, nowMillis: () -> Long)`.
    `fun onStateChanged(before: AncState, after: AncState)`: (a) draftRecordFor(after) non-null and its values differ from the previous pending/saved values for that slot -> cancel any earlier pending save of that slot and schedule ONE save debounceMillis later with the LATEST record; (b) closedDraftSlot(before, after) non-null -> cancel the pending save of that slot and delete that slot's draft right away; (c) nothing else.
    `suspend fun flush()`: runs the pending save now and waits for it (and for a save already running); no-op when nothing is pending; never throws (a store failure or exception is swallowed: typing must never crash); `fun cancelPending()`: forgets pending saves without deleting anything (used after a lock).
    VaultController: `suspend fun saveDraft(record: DraftRecord): Boolean`, `suspend fun loadDraft(patientId: String, kind: DraftScreenKind): DraftRecord?`, `suspend fun deleteDraft(patientId: String, kind: DraftScreenKind)`: same pattern as saveAttachment (withContext(dispatcher), storageMutex.withLock, return false/null when no open session, repository calls); never log draft content. exportEncryptedBackup is NOT changed.
Q111 (state/AncViewModel.kt)
    The ViewModel owns one DraftAutosaver whose DraftStore calls the VaultController methods; every change of `_state` caused by dispatch or by the save-result handlers (SaveCommitted/RecordCommitted/errors) is reported once to onStateChanged(before, after). The reset caused by a vault lock (controller.view collector) is NOT reported as a discard; it calls cancelPending() after the flush below.
    manualLock() and onBackgrounded(...) first await autosaver.flush() (viewModelScope, at most 2 seconds, then continue anyway) and only then call controller.lock() / controller.onBackgrounded(...). Behaviour of lock/background otherwise unchanged (frozen commands are still cleared).
    A restored draft will get a new operation id because applyDraftRecord sets a new editorSessionId (step 3 uses it; the frozen-command logic is not touched here).
Static checks (.agents/checks/check_r3v.py, STATIC-r3v; strip comments first): S92 state/DraftMapping.kt defines activeDraftSlot, draftRecordFor, applyDraftRecord, closedDraftSlot and contains no `import android`; state/DraftAutosaver.kt defines DraftStore and DraftAutosaver with onStateChanged, flush, cancelPending, and uses no Log/SharedPreferences/File.
 S93 VaultController.kt defines saveDraft, loadDraft, deleteDraft each using storageMutex; AncViewModel.kt references DraftAutosaver, and the bodies of manualLock and onBackgrounded mention `flush` BEFORE `controller.lock` / `controller.onBackgrounded`; no file under app/src/main/**/ui/ mentions DraftAutosaver (no UI change this step); EncryptedBackupManager.kt contains no "draft".
Unit (JVM, state/DraftMappingTest.kt and state/DraftAutosaverTest.kt; kotlinx-coroutines-test is NOT available: use runBlocking and a small real debounce such as 30 ms, and use flush() for deterministic checks):
 Mapping: for each of the 9 screens build an AncState with that editor open, change values -> draftRecordFor non-null with the right slot/kind/editorKind/recordId/baseRevision and all fields; untouched editor -> null; no editor -> null; round trip: draftRecordFor then applyDraftRecord onto a fresh clean editor gives the same values and a different editorSessionId and no errors; new patient slot is NEW_PATIENT_DRAFT_KEY; ultrasound imageUris are not in the record; applyDraftRecord with a non-matching slot leaves the state equal; Arabic/emoji text survives (through encodeDraftFields/decodeDraftFields too); closedDraftSlot: dirty editor then none -> that slot; dirty then clean -> that slot; same dirty editor -> null; before Unlocked and after Locked -> null.
 Autosaver (fake DraftStore recording calls): typing 5 times quickly -> one save with the latest values after the debounce; flush() saves immediately and a second flush does nothing; editor closed -> delete called and a pending save for it never happens; clean editor + no changes -> nothing stored; lock (state not Unlocked) -> no delete; store that throws -> no exception escapes and later saves still work; slot switch keeps one pending save per slot.
Device (postponed list, emulator run by the orchestrator): type in the visit editor, wait 2 s, force-stop the app, reopen: the row exists in the encrypted database (no alert yet); save the visit: the row is gone; discard: gone.

## Round 3w (draft recovery, step 3 of 3: the "Continue / Discard and start fresh" alert; owner decisions D9 + 2026-10-31 answers)
Scope: when the user opens an entry screen for which a draft is stored (step 1 storage, step 2 auto-save, both committed), a modal alert offers "Continue" (put the stored text into the form) or "Discard and start fresh" (delete the stored draft, keep the clean form). Shows the age of the draft and warns when the saved record changed after the draft was started. The old "drafts are memory-only / discarded on lock" wording is replaced because it is no longer true. Covers the 9 screens of step 2. Archived patients cannot open editors, so they get no prompt (unchanged).
Owner rules: one draft per patient per screen kind (already); no expiry, show age; the alert cannot be dismissed by tapping outside or Back (the user must choose); a lock while the alert is open keeps the stored draft.
Q112 (NEW state/DraftRecovery.kt, pure Kotlin; AncState.kt)
    `data class DraftRecoveryPrompt(val slot: DraftSlot, val editorSessionId: String, val record: DraftRecord, val isStale: Boolean)`.
    `fun activeEditorSessionId(state: AncState): String?` (the open editor's editorSessionId) and `fun activeEditorBaseRevision(state: AncState): Long?` (the revision of the saved record that the freshly opened editor was based on; null for a new record; for screens with a patient revision and a record revision use the record's own revision, falling back to the patient revision as the draft mapping already does: reuse what draftRecordFor stores as baseRevision, do not invent a second rule).
    `fun isDraftStale(record: DraftRecord, currentBaseRevision: Long?): Boolean` : true only when both are non-null and currentBaseRevision > record.baseRevision.
    `sealed interface DraftAge { object LessThanMinute; data class Minutes(val n: Long); data class Hours(val n: Long); data class Days(val n: Long) }` and `fun draftAge(nowMillis: Long, savedMillis: Long): DraftAge` (< 60 s -> LessThanMinute; < 60 min -> Minutes (floor); < 24 h -> Hours; else Days; a saved time in the future counts as LessThanMinute).
    AncState gets `draftRecoveryPrompt: DraftRecoveryPrompt? = null`. Events: `DraftRecoveryFound(prompt)` (sets the prompt ONLY when an editor with prompt.editorSessionId is still open and no prompt is set), `AcceptDraftRecovery` (state = applyDraftRecord(state, prompt.record) with the prompt cleared; no prompt -> unchanged), `DiscardDraftRecovery` (prompt cleared; the editor stays as opened). The prompt is also cleared by ANY transition after which activeEditorSessionId(state) != prompt.editorSessionId (cancel, discard, save, route change, lock reset): do this in one place at the end of AncReducer.reduce.
    While a prompt is set, the editor's own events that change values do nothing (the alert is modal; a stray event must not edit a form the user has not decided about).
Q113 (state/DraftAutosaver.kt, state/AncViewModel.kt)
    DraftAutosaver: `fun holdSlot(slot: DraftSlot)` / `fun releaseSlot(slot: DraftSlot)`: while a slot is held onStateChanged neither saves nor deletes that slot and a pending save for it is dropped; release restores normal behaviour.
    AncViewModel.dispatch: for each of the nine Start* events, after the normal reduce, hold the slot of the new editor (activeDraftSlot), launch loadDraft(slot); if a record is found, report DraftRecoveryFound(DraftRecoveryPrompt(slot, activeEditorSessionId, record, isDraftStale(record, activeEditorBaseRevision))) through the same state path (updateState); if none is found or the editor changed meanwhile, release the slot. AcceptDraftRecovery: reduce, then release (the applied text is dirty and is auto-saved as usual). DiscardDraftRecovery: reduce, delete the stored draft for the slot, then release. A lock/state reset releases all holds (autosaver.cancelPending also clears holds).
    Never log draft content; the load runs through VaultController.loadDraft (already exists).
Q114 (ui/AncApp.kt, res/values/strings.xml, res/values-ar/strings.xml; NO other UI file)
    In AncApp.kt next to the existing discard AlertDialog, when state.draftRecoveryPrompt != null (and unlocked, not app-locked, no PIN setup pending, same conditions as the discard dialog): an AlertDialog with properties dismissOnBackPress = false and dismissOnClickOutside = false; title "Unsaved draft found"; body "You started this {screen} {age} and did not save it." (screen name per DraftScreenKind: Visit, Patient profile, Other histories, Previous pregnancy, Patient status, Dating, Ultrasound scan, Lab result, Medication; age text from draftAge via plural strings: "less than a minute ago", "N minutes ago", "N hours ago", "N days ago"); when isStale an extra line "The saved record changed after this draft was started. Review it carefully before saving."; confirm button "Continue" (testTag "draftContinue") dispatches AcceptDraftRecovery; dismiss button "Discard and start fresh" (testTag "draftDiscard") dispatches DiscardDraftRecovery. The dialog must be above the editor but must not appear on the lock screen or while the app is locked.
    Wording fix: the strings unsaved_drafts_policy, unsaved_drafts_editor_policy, save_confirmed_not_saved, save_storage_error and storage_closing_body (and any other string in values/ and values-ar/ that says drafts are memory-only, discarded on lock/background/process death, or "memory-only draft") are rewritten to the new truth: unsaved text is kept as an encrypted draft on this device until it is saved or discarded; only completed saves become part of the patient record. Keep every string NAME. Arabic values are updated to match (owner note: Arabic is not the release language yet; keep them simple and accurate).
Static checks (.agents/checks/check_r3w.py, STATIC-r3w; strip comments first): S94 state/DraftRecovery.kt defines DraftRecoveryPrompt, activeEditorSessionId, activeEditorBaseRevision, isDraftStale, DraftAge, draftAge and has no `import android`; AncState.kt defines draftRecoveryPrompt, DraftRecoveryFound, AcceptDraftRecovery, DiscardDraftRecovery; DraftAutosaver.kt defines holdSlot and releaseSlot.
 S95 AncViewModel.kt calls loadDraft, holdSlot, releaseSlot and deleteDraft; AncApp.kt contains testTags "draftContinue" and "draftDiscard", `dismissOnBackPress = false` and `dismissOnClickOutside = false`, and references draftRecoveryPrompt; no ui file other than AncApp.kt mentions DraftRecoveryPrompt.
 S96 strings: no string in values/strings.xml or values-ar/strings.xml contains "memory-only" or "discarded on lock" (case-insensitive); the new names draft_recovery_title, draft_recovery_body, draft_recovery_stale, draft_recovery_continue, draft_recovery_discard exist in BOTH files; the nine draft_screen_* names exist in both.
Unit (JVM, state/DraftRecoveryTest.kt NEW + additions to state/DraftAutosaverTest.kt): activeEditorSessionId/BaseRevision for each of the 9 editors; isDraftStale truth table (null/null, equal, newer, older); draftAge boundaries (59 s, 60 s, 59 min, 60 min, 23 h, 24 h, 40 days, future time); reducer: Found sets the prompt only for the open editor session and only once, Accept applies the stored text with a new editorSessionId and clears the prompt, Discard clears the prompt and leaves the values clean, any close/cancel/lock reset clears the prompt, value-change events are ignored while a prompt is set; autosaver: a held slot is not saved and not deleted, release resumes.
Instrumented (written and COMPILED by the test lane; the orchestrator runs it): androidTest DraftRecoveryDialogTest.kt (NEW): render AncApp (or the smallest existing host the other androidTests use) with a state that has a draftRecoveryPrompt: title, body with the screen name and age, Continue and Discard buttons displayed; stale warning only when isStale; Back press does not dismiss; clicking each button dispatches the right event. In AncWorkflowTest.kt update ONLY the test recordsWindowKeepsSecureFlagAndGateExplainsDraftPolicy to the new gate wording (rename it to say what it now checks), and if the existing harness makes it simple add ONE end-to-end test: open the visit editor for a demo patient, type a note, wait 2 s, cancel with discard CONFIRMED OFF is not possible, so instead lock via the app lock path and unlock, reopen the visit editor, the alert appears, Continue puts the note back, Discard shows an empty note. If the harness cannot do this reliably say so in the final message instead of writing a flaky test.
Gates by the orchestrator: unit suite, release compile, androidTest compile, STATIC-r3w and all older checks, then the instrumented suite on the emulator (the three older failures that are known stay known; the draft-policy-text one must now pass).

## Round 3x (printed report layout: words and dates must not break, stamp text once, no silent list cuts; owner answers c13/c15 "go on" = accept recommendations; tracker items P-1, P-2)
File: ui/report/PatientPrintHtmlGenerator.kt only (plus tests). Owner saw in the phone print preview: "Gestati onal Age", "Blood Pressu re", "Weigh t", "120/8 0", "165/1 05", "Frequen cy", dates split as "2026-" / "10-31"; "Official Stamp & Signature" printed twice; lists silently cut (visits 12, scans 7, labs 10, medications 8).
Q115 (cells never break inside a word, number or date)
    In the `.clinical-table th`, `.clinical-table td` and `.mini-table` rules replace `word-break: break-word` (and the older `word-wrap`) with normal breaking at spaces only: `word-break: normal; overflow-wrap: normal; hyphens: none;`. Only the free-text cells (`.notes-cell` and the free-text cells of the previous-pregnancy and medication tables) keep `overflow-wrap: anywhere` so one very long unbroken string cannot overflow the page.
    Short value cells never wrap: `.date-cell` and the cells for number, gestational age, blood pressure, weight, EFW get `white-space: nowrap`. Headers wrap only at spaces. Column widths of the visits table are rebalanced (#, date, GA, BP, weight, findings) so that the date "2026-10-31" and "165/105" fit on one line at the print size; keep the other tables' widths unless a header word is wider than its column. Also check the narrow phone-preview mode (the `@media screen and (max-width: 540px)` block): the same no-break rules must hold there (smaller font in that block is allowed, mid-word breaks are not).
Q116 "Official Stamp & Signature" is printed exactly once: keep the label inside the stamp placeholder box, remove the second line below it (and its inline style) from the signature box.
Q117 No silent cuts. One private helper `hiddenRowsHtml(hidden: Int, colspan: Int): String` returns "" when hidden <= 0, otherwise one table row `<tr class="more-row"><td colspan="N" class="text-center empty-cell">and K more not shown</td></tr>` (K = hidden; "and 1 more not shown" for one). The four caps (visits 12, ultrasound 7, laboratory 10, medications 8) become named constants (`MAX_PRINT_VISITS` etc.; values unchanged) and every list that is cut appends hiddenRowsHtml(total - cap, columns) after the shown rows. Check every `.take(` in the file; also the previous-pregnancy table if it ever cuts. The page count logic is unchanged.
Static checks (.agents/checks/check_r3x.py, STATIC-r3x; strip Kotlin comments first): S97 PatientPrintHtmlGenerator.kt contains no `word-break: break-word`; contains `white-space: nowrap`; the text "Official Stamp & Signature" occurs exactly once; S98 defines hiddenRowsHtml and calls it at least 4 times, defines the four MAX_PRINT_* constants, and has no `.take(` with a numeric literal.
Unit (ui/report/PrintLayoutTest.kt NEW, JVM, same builders as PrintSectionTitlesTest.kt, context = null): generated CSS has no `word-break: break-word` in the clinical-table / mini-table rules; `.date-cell` is nowrap; stamp text appears once; 15 visits -> "and 3 more not shown" in the visits table and exactly 12 visit rows; exactly 12 visits -> no "more not shown"; 13 visits -> "and 1 more not shown"; the same for 9 scans (cap 7), 12 labs (cap 10), 10 medications (cap 8); fewer than the cap -> no more-row; the existing empty-state rows are unchanged; the more-row colspan equals the table's column count. The existing print tests keep passing. PrintSampleDumpTest.kt (NEW): builds one patient with 15 visits (one severe BP), 9 scans, 12 labs, 10 medications, 3 previous pregnancies and long values and WRITES the HTML to app/build/print-sample/report.html (create the folder; the test only fails if generation throws). The orchestrator opens that file in a browser at A4 width and at 380 px to verify the layout by eye.

## Round 3y (English only, lint errors, decided test repairs, draft end-to-end test; owner 2026-10-31: "no arabic at all, its english only app now, not hybrid"; c17 clean rollback, c18 no Lock button)
Context: the Arabic resource folder app/src/main/res/values-ar was deleted by the orchestrator on the owner's order (patient NAMES may still be Arabic script; the script-adaptive name alignment in PatientListScreen/ProfileScreen/print is DATA handling and stays). `./gradlew :app:lintDebug` currently reports 17 errors: BiometricAuthManager.kt (NewApi: BiometricPrompt needs API 28/30, minSdk is 26), res/values/themes.xml (windowSplashScreenBackground / windowSplashScreenAnimatedIcon need API 31), ui/AppLockScreen.kt lines ~132/139/158 (LocalContextGetResourceValueCall: resource strings read through LocalContext.current).
Q118 (lint errors to zero; behaviour unchanged on the owner's phone, Android 15)
    BiometricAuthManager.kt: below API 28 `isBiometricAvailable` is false and no BiometricPrompt class is touched (the app falls back to its own PIN, as it already does when no fingerprint is enrolled); API 28-29 omits setAllowedAuthenticators (use it only from API 30); guard with Build.VERSION.SDK_INT checks (small private helpers or @RequiresApi on the prompt-building function, the call sites check the version once). No change to the strings, titles or result handling.
    themes.xml: remove the two windowSplashScreen items from res/values/themes.xml and put them, with the same values, in a NEW res/values-v31/themes.xml that defines the same style name (parent identical, only the two items; use the standard parent-child pattern so the base theme is not duplicated: either style `Theme.AncNativeDemo` in values-v31 with the same parent and all items, or a parent/child pair; choose the smaller).
    AppLockScreen.kt: the three reported lines read string resources through the Compose context; resolve the strings in composition with stringResource(...) (or collect them once at the top of the composable) and use the resolved values in the click handler. Texts unchanged.
    Result: `./gradlew --offline :app:lintDebug` reports 0 errors (warnings may stay). Release and debug compile unchanged.
Q119 (instrumented tests; androidTest compile must stay green; the orchestrator runs them on the emulator)
    a. Remove the Arabic-locale string test `everyNewEnglishStorageStringHasAnArabicValue` from AncWorkflowTest.kt (no Arabic resources exist). Keep the tests that type Arabic patient DATA into editors.
    b. VaultIntegrationTest `initializationCancellationAndDetachBlockWhileUnlockCancellationRemainsRetryable`: the owner decided that cancelling vault setup rolls back cleanly (state back to Uninitialized, nothing half-created, setup can be started again). First read VaultController.kt (around the initialization cancel/detach handling) and confirm nothing is left behind (descriptor, key alias, files); if something IS left behind say so in the final message and do not weaken the assertion. Then change the test to expect Uninitialized after cancellation and detach, a retry that works, and unlock cancellation still retryable as before.
    c. AncWorkflowTest `reviewedVisitEditorLayoutsKeepStatusDraftAndControlsAccessible`: the owner decided the visit editor has NO Lock button; delete the expectation of the lockStorage button there (and anything that depends on it) and keep every other assertion in that test.
    d. SecurityWorkflowTest `securitySettingsDialogDisplaysFixedAutoLockTimeoutAndAllowsManualLock` is flaky (it passed on some emulator runs and failed on others with "'Clinic & System Settings' is not displayed"): wait for the dialog with composeTestRule.waitUntil (timeout 5 s) on the title before asserting, and keep the rest. If after reading SecuritySettingsDialog.kt you believe the title is genuinely not shown in some state, say so in the final message instead of hiding it.
    e. NEW end-to-end draft test in AncWorkflowTest.kt (use the existing harness helpers; demo data; debug PIN as the other tests do): open the visit editor for a demo patient, type a note, wait about 2 seconds, lock the app through the app's lock path, unlock again, reopen the visit editor for the same patient: the alert "Unsaved draft found" is displayed; Continue puts the typed note back; in a second run (or second test) Discard shows an empty note and the alert does not come back the next time the editor opens. If the harness cannot do lock/unlock reliably, write the part that works (type, wait, close editor with confirm-discard OFF is impossible, so use leaving through lock) and say what was not possible in the final message; never write a flaky test.
Static checks (.agents/checks/check_r3y.py, STATIC-r3y; strip comments first): S99 app/src/main/res has no values-ar folder; no file under app/src/main or app/src/androidTest contains `Locale("ar")` as a UI-string locale test (the word `Arabic` may stay in comments about patient names); check_r3w.py no longer lists values-ar. S100 BiometricAuthManager.kt contains `Build.VERSION.SDK_INT` (or @RequiresApi) for API 28 and 30; res/values/themes.xml has no `windowSplashScreen`; res/values-v31/themes.xml has both items; AppLockScreen.kt contains no `context.getString(` ... `LocalContext.current` resource read at lines that run in the click handler (no `context.getString(R.string.pin_` anywhere).
Gate (TEST_MANIFEST id LINT-errors, tier 2, cmd `./gradlew --offline :app:lintDebug`, deps = every main source and resource file): lint reports 0 errors.

## Round 3z (small leftovers: one function per rule; no visible change except the two noted; owner 2026-10-31 "finish the smaller leftovers first")
Source: .agents/pipeline/a10/AUDIT_REUSE.md items R-2, R-5, R-6, R-8, R-9, R-10, R-11 (verify each in the code first) plus two cleanups. Behaviour must stay IDENTICAL except: (a) R-11 the medication entry list gains "At bedtime" (it becomes the settings list, the superset); (b) R-5 both screens show the same risk badge (the patient list look wins). Keep every testTag and every text.
Q120 (model/IsoDates.kt, ui/report/PatientPrintHtmlGenerator.kt) `fun formatIsoDate(date: LocalDate): String` in IsoDates.kt (ISO yyyy-MM-dd, same text as today); the print generator's `DateTimeFormatter.ofPattern("yyyy-MM-dd")` print date uses it (R-2).
Q121 (state/AncState.kt) ONE mapping `fun SaveUiError?.stringRes(): Int` (null and STORAGE_ERROR -> save_storage_error; every other value exactly as the two copies map today); PatientEditor.kt's patientEditorErrorResource and VisitEditor.kt's inline `when` are deleted and call it (R-6). The two error cards in PatientEditor.kt (EditorErrorCard and ClinicalEditorErrorCard) become one `GenericEditorErrorCard(hasErrors: Boolean, state: AncState)` in ui/Components.kt using stringRes (R-10); confirm the clinical editors show the same texts as before.
Q122 (ui/ClinicalFormat.kt) `fun formatFetalMetrics(fhr: String?, efw: String?): String` returning exactly `FHR: <fhr or -> bpm · EFW: <efw or -> g`; ProfileScreen.kt and WorkspaceScreen.kt use it (R-8).
Q123 (ui/ClinicalPresets.kt) `val COMMON_MEDICATION_FREQUENCIES = listOf("Once daily", "Twice daily", "Three times daily", "At bedtime", "As needed (PRN)")` (or the existing object/naming style of that file) used by MedicationEditor.kt and CodexSettingsSections.kt (R-11).
Q124 (ui/ClinicalBadges.kt, ui/Components.kt, PatientListScreen.kt, ProfileScreen.kt, VisitEditor.kt, PatientEditor.kt) R-5: one `ClinicalRiskBadge(isHighRisk: Boolean, modifier: Modifier = Modifier)` in ClinicalBadges.kt (the PatientListScreen look and tokens) used by the list and the profile; R-9: promote EditorField (PatientEditor.kt) to Components.kt, delete VisitEditor's VisitField clone and call EditorField (same parameters, same semantics, same testTags and error text styling; if the two differ in behaviour keep the superset and say what you merged).
Q125 (security/BiometricAuthManager.kt, res/values/strings.xml) BiometricAuthManager: one constant `MIN_BIOMETRIC_API = 30` gate in isBiometricAvailable; delete the redundant `< 28` checks and the unreachable `SDK_INT >= 30` branch in promptBiometric (behaviour on every API level unchanged: below 30 no biometrics, PIN only). strings.xml: delete the unused `unsaved_drafts_policy` and `unsaved_drafts_editor_policy` (grep the whole repo first, tests and androidTest included; update any reference).
Static checks (.agents/checks/check_r3z.py, STATIC-r3z; strip comments first): S101 formatIsoDate, stringRes, formatFetalMetrics, COMMON_MEDICATION_FREQUENCIES, GenericEditorErrorCard, ClinicalRiskBadge(isHighRisk are defined exactly once under app/src/main; `ofPattern("yyyy-MM-dd")` no longer appears in PatientPrintHtmlGenerator.kt; `patientEditorErrorResource` and `fun VisitField` and `ClinicalEditorErrorCard` do not exist; the literal `"FHR: ` appears only in ClinicalFormat.kt; the text `"Three times daily"` appears only in ClinicalPresets.kt (main sources).
 S102 BiometricAuthManager.kt has no `SDK_INT < 28` and no `SDK_INT >= 30` and defines MIN_BIOMETRIC_API; strings.xml has neither unsaved_drafts_policy nor unsaved_drafts_editor_policy and no file under app/src references them.
Unit (JVM): model/IsoDatesTest.kt additions or NEW model/FormatIsoDateTest.kt (formatIsoDate for ordinary, leap-day, year boundary dates); NEW state/SaveUiErrorTextTest.kt (every SaveUiError value and null map to the expected R.string id, all distinct except null/STORAGE_ERROR); NEW ui/ClinicalFormatTest.kt (formatFetalMetrics both present, one null, both null, text exactly as above); NEW ui/ClinicalPresetsTest.kt (the five frequencies in order). The existing print tests keep passing (print date text unchanged).
Gate: LINT-errors, the whole unit suite, compile debug/release/androidTest, STATIC-r3z; the orchestrator then runs the emulator suite.
