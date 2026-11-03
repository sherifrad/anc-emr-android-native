#!/usr/bin/env python3
"""S400-S407 static check for round r6b1 (SPEC_LOGIC_R6B.md D620-D624, D627, D630, D651, D653 part 1).
Storage generalisation only: state/ViewModel/ui are NOT touched in this round. Stdlib only; textual contract only."""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
results = []

# Canonical content strings from commit 19c36fc (must survive byte for byte inside the adapters).
CANON_MED = '"${record.patientId}|${record.id}|${record.name}|${record.dosage}|${record.route}|${record.frequency}|${record.startDate}|${record.endDate}|${record.status}|${record.notes}|${record.isProphylaxis}|${record.isNonCompliant}|${record.adverseNotes}"'
CANON_LAB = '"${record.patientId}|${record.id}|${record.date}|${record.testCategory}|${record.testName}|${record.result}|${record.unit}|${record.referenceRange}|${record.isAbnormal}|${record.notes}|${record.labName}"'


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return ""


def missing(src, names):
    return [n for n in names if n not in src]


def git_changed():
    out = subprocess.run(["git", "diff", "--name-only", "HEAD"], cwd=ROOT, capture_output=True, text=True, check=True).stdout
    return out.splitlines()


def main():
    dr = read(MAIN / "storage/DeletableRecords.kt")
    miss = missing(dr, ["enum class DeletableType", "MEDICATION", "LAB", "interface DeletableRecordAdapter", "object DeletableRegistry",
                        "fun contentHash", "fingerprintTag", "deleteReceiptKind", "restoreReceiptKind", "fun canonical",
                        "suspend fun load", "suspend fun deleteRow", "suspend fun insertRow", "fun label"])
    record("S400 DeletableRecords.kt: DeletableType, adapter interface, registry, one contentHash", not miss, "missing " + ", ".join(miss))

    med = read(MAIN / "storage/MedicationDeleteAdapter.kt")
    lab = read(MAIN / "storage/LabDeleteAdapter.kt")
    problems = []
    for label, src, tag, kinds, canon in (("medication", med, "MED", ("MEDICATION_DELETE", "MEDICATION_RESTORE"), CANON_MED),
                                           ("lab", lab, "LAB", ("LAB_DELETE", "LAB_RESTORE"), CANON_LAB)):
        if not src:
            problems.append(label + " adapter file missing")
            continue
        if not re.search(r'fingerprintTag\s*(:\s*String\s*)?=\s*"%s"' % tag, src):
            problems.append(label + " fingerprintTag != " + tag)
        problems += [label + " lacks " + k for k in kinds if k not in src]
        if canon not in src:
            problems.append(label + " canonical string not byte-identical to 19c36fc")
    record("S401 medication and lab adapters keep tags, receipt kinds and canonical strings byte-identical", not problems, "; ".join(problems))

    cmds = read(MAIN / "storage/ClinicalCommands.kt")
    miss = missing(cmds, ["class DeleteRecordCommand", "class RestoreRecordCommand", "interface RecordUndoCommand", "fun withPatientRevision"])
    fp_ok = bool(re.search(r'_DEL\|\$operationUuid\|\$patientId\|\$expectedPatientRevision\|\$recordId\|\$expectedRecordRevision\|\$deletedContentHash', cmds)) and \
        bool(re.search(r'_RES\|\$operationUuid\|\$patientId\|\$expectedPatientRevision\|\$\{sourceDelete\.computeFingerprint\(\)\}\|', cmds))
    dup = [n for n, c in (("canonical med", CANON_MED), ("canonical lab", CANON_LAB)) if c in cmds]
    record("S402 generic DeleteRecordCommand/RestoreRecordCommand, same fingerprint text, no second copy of canonical strings",
           not miss and fp_ok and not dup, "missing " + ", ".join(miss) + ("" if fp_ok else "; fingerprint format drift") + ("; duplicated " + ", ".join(dup) if dup else ""))

    repo = read(MAIN / "storage/RecordsRepository.kt")
    n_del = len(re.findall(r"\bfun\s+deleteRecord\s*\(", repo))
    n_res = len(re.findall(r"\bfun\s+restoreRecord\s*\(", repo))
    old = [n for n in ("deleteMedication", "restoreMedication", "deleteLabInvestigation", "restoreLabInvestigation")
           if re.search(r"\bfun\s+" + n + r"\s*\(", repo)]
    guard = repo.count("deleteRestoreGuardFailure") >= 2 and "ClinicalCommandAbort" in repo
    record("S403 RecordsRepository has exactly one deleteRecord and one restoreRecord, four copies gone, guard reused",
           n_del == 1 and n_res == 1 and not old and guard,
           "deleteRecord=%d restoreRecord=%d old=%s guardReused=%s" % (n_del, n_res, old, guard))

    rule = read(MAIN / "storage/ClinicalDeleteRule.kt")
    bad = re.findall(r"\bis\s+(Restore\w+Command|RestoreMedicationCommand|RestoreLabInvestigationCommand)\b", rule)
    sig = re.search(r"fun\s+evaluateRestore\s*\(\s*receipt\s*:\s*ClinicalCommandReceipt\?\s*,\s*latestReceipt\s*:\s*ClinicalCommandReceipt\?\s*,\s*existingRow\s*:\s*Any\?\s*,\s*cmd\s*:\s*ClinicalCommand\s*,?\s*\)\s*:\s*ClinicalCommandFailure\?", rule)
    record("S404 ClinicalDeleteRule.evaluateRestore: signature unchanged, no per-command-class when", not bad and bool(sig),
           ("per-class branches left: %s; " % bad if bad else "") + ("" if sig else "signature changed"))

    db = read(MAIN / "storage/RecordsDatabase.kt")
    problems = []
    try:
        for line in git_changed():
            if re.search(r"app/schemas/|RecordsMigration|RecordsDatabase\.kt|storage/RecordsDao\.kt|Entities?\.kt", line):
                problems.append("forbidden file changed: " + line)
    except (OSError, subprocess.CalledProcessError) as exc:
        problems.append("git diff failed: " + str(exc))
    record("S405 no Room schema, database version, entity or DAO change", not problems and bool(re.search(r"version\s*=\s*6\b", db)), "; ".join(problems) or "version != 6")

    problems = []
    try:
        for line in git_changed():
            if "/ancnative/state/" in line or "/ancnative/ui/" in line:
                problems.append("state/ui file changed (r6b1 must not require it): " + line)
    except (OSError, subprocess.CalledProcessError) as exc:
        problems.append("git diff failed: " + str(exc))
    record("S406 no state, ViewModel or ui change (source-compatible factories/aliases keep them compiling)",
           not problems and "fun DeleteMedicationCommand" in cmds + read(MAIN / "storage/DeletableRecords.kt"), "; ".join(problems) or "D651 factory fun DeleteMedicationCommand missing")
    t = read(ROOT / "app/src/androidTest/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRestoreDbTest.kt")
    miss = missing(t, ["foreign_key_check", "integrity_check", "sqlite_master"])
    for label, pat in (("snapshot-diff test", r"fun\s+\w*(snapshot|isolation)\w*\s*\("), ("cross-kind id collision test", r"fun\s+\w*(collision|sameId|crossKind)\w*\s*\("),
                       ("two-patient test", r"fun\s+\w*(twoPatients|otherPatient|samePatientLook)\w*\s*\(")):
        if not re.search(pat, t, re.I):
            miss.append(label)
    unit = read(ROOT / "app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt")
    if not unit:
        miss.append("DeletableRegistryTest.kt")
    record("S407 D654 isolation oracles: DB test has snapshot-diff, cross-kind id collision, two-patient and PRAGMA foreign_key_check/integrity_check tests",
           not miss, "missing " + ", ".join(miss))
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
