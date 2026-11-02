#!/usr/bin/env python3
"""S300-S303 static check for round 6a (SPEC_LOGIC_R6.md). Stdlib only; textual contract only."""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return ""


def has_all(src, names):
    return [n for n in names if n not in src]


def main():
    cmds = read(MAIN / "storage/ClinicalCommands.kt")
    miss = has_all(cmds, ["DeleteRecordCommand", "RestoreRecordCommand", "MEDICATION_DELETE", "MEDICATION_RESTORE", "LAB_DELETE",
                          "LAB_RESTORE"])
    # the content hashes moved to storage/DeletableRecords.kt in round 6b1
    miss += has_all(read(MAIN / "storage/DeletableRecords.kt"), ["medicationContentHash", "labContentHash"])
    record("S300 delete and restore commands, kinds and hashes defined", not miss, "missing " + ", ".join(miss))
    dao = read(MAIN / "storage/RecordsDao.kt")
    miss = has_all(dao, ["deleteMedication", "deleteLabInvestigation", "latestClinicalReceiptForRecord"])
    record("S301 DAO delete and latest-receipt queries present", not miss, "missing " + ", ".join(miss))
    st = read(MAIN / "state/AncState.kt") + read(MAIN / "state/AncViewModel.kt") + read(MAIN / "state/UndoCoordinator.kt")
    miss = has_all(st, ["RequestDelete", "ConfirmDelete", "UndoRequested", "UndoExpired",
                        "undoOffer", "deleteConfirmation", "8_000"])
    record("S302 delete/undo events, state fields and 8 s timer present", not miss, "missing " + ", ".join(miss))
    problems = []
    try:
        out = subprocess.run(["git", "diff", "--name-only", "HEAD"], cwd=ROOT, capture_output=True, text=True, check=True).stdout
        for line in out.splitlines():
            if "/ancnative/ui/" in line or re.search(r"RecordsMigration|schemas/", line):
                problems.append("forbidden file changed: " + line)
    except (OSError, subprocess.CalledProcessError) as exc:
        problems.append("git diff failed: " + str(exc))
    db = read(MAIN / "storage/RecordsDatabase.kt")
    record("S303 no ui, schema or migration file changed", not problems and "version" in db, "; ".join(problems))
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
