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
