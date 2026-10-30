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
