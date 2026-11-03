#!/usr/bin/env python3
"""S410-S416 static check for round r6b2 (SPEC_LOGIC_R6B.md D625-D627, D651, D653 part 2).
State, ViewModel and UndoCoordinator. Stdlib only; textual contract only."""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
results = []
BANNED = ["DeleteMedicationCommand", "DeleteLabInvestigationCommand", "RestoreMedicationCommand", "RestoreLabInvestigationCommand",
          "DeletionKind", "RequestDeleteMedication", "RequestDeleteLab"]
OLD_UNDO = ["UndoKind", "DELETE_MED", "DELETE_LAB", "SAVE_MED"]
ALLOWED_UI = {"MedicationEditor.kt", "LabEditor.kt", "WorkspaceScreen.kt"}


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


def main_files():
    return sorted(MAIN.rglob("*.kt"))


def main():
    hits = []
    for f in main_files():
        src = read(f)
        for n in BANNED:
            if re.search(r"\b" + n + r"\b", src):
                hits.append(f.name + ":" + n)
    record("S410 no per-kind command, DeletionKind or per-kind Request event identifier left in app/src/main", not hits, ", ".join(hits))

    state = read(MAIN / "state/AncState.kt")
    vm = read(MAIN / "state/AncViewModel.kt")
    old = [n for n in OLD_UNDO if re.search(r"\b" + n + r"\b", state + vm)]
    miss = missing(state, ["class RequestDelete", "class PendingDeletion", "class UndoOffer", "DeletableType"])
    record("S411 AncState: generic RequestDelete, PendingDeletion and UndoOffer keyed by DeletableType; old UndoKind gone",
           not miss and not old, "missing " + ", ".join(miss) + ("; old " + ", ".join(old) if old else ""))

    problems = []
    for f in (MAIN / "state/AncViewModel.kt", MAIN / "state/AncState.kt", MAIN / "state/UndoCoordinator.kt"):
        src = read(f)
        if re.search(r"\bwhen\s*\([^)]*(type|kind)[^)]*\)", src, re.I) and re.search(r"DeletableType\.[A-Z_]+\s*->", src):
            problems.append(f.name + " branches on DeletableType")
        if re.search(r"DeletableType\.[A-Z_]+\s*->|[!=]=\s*DeletableType\.|\bis\s+DeletableType\b", src):
            problems.append(f.name + " has a DeletableType branch")
    record("S412 no DeletableType branch in AncViewModel, AncState or UndoCoordinator", not problems, "; ".join(sorted(set(problems))))

    co = read(MAIN / "state/UndoCoordinator.kt")
    miss = missing(co, ["class UndoCoordinator", "fun beginDelete", "fun offerSaveUndo", "fun request", "fun clear", "MAX_UNDO_RESTARTS", "CoroutineScope"])
    android = [p for p in ("import android.", "androidx.lifecycle", "viewModelScope", "androidx.compose") if p in co]
    record("S413 UndoCoordinator: plain class with the four public entry points, injectable scope, no Android types",
           bool(co) and not miss and not android, "missing " + ", ".join(miss) + ("; android: " + ", ".join(android) if android else ""))

    eh = read(MAIN / "state/EditorHooks.kt")
    miss = missing(eh, ["interface EditorHook", "isOpenFor", "closeEditor", "rebaseCreateDraft"])
    record("S414 EditorHooks.kt: EditorHook with isOpenFor, closeEditor, rebaseCreateDraft", not miss, "missing " + ", ".join(miss))

    left = [n for n in ("armUndo", "startUndoExpiry", "PendingUndo") if re.search(r"\b" + n + r"\b", vm)]
    n8 = sum(len(re.findall(r"(?<![-\w\"])8_?000L?(?![-\w\"])", read(f))) for f in (MAIN / "state").rglob("*.kt"))
    record("S415 timer owned once by the coordinator: the 8000 ms literal appears exactly once under state/, VM has no armUndo/startUndoExpiry/PendingUndo",
           n8 == 1 and re.search(r"\b8_?000L?\b", co) is not None and not left and "UndoCoordinator" in vm, "8000 count=%d; left in VM: %s; VM uses UndoCoordinator=%s" % (n8, left, "UndoCoordinator" in vm))

    repo = read(MAIN / "storage/RecordsRepository.kt")
    n_del = len(re.findall(r"\bfun\s+deleteRecord\s*\(", repo))
    n_res = len(re.findall(r"\bfun\s+restoreRecord\s*\(", repo))
    problems = []
    if n_del != 1 or n_res != 1:
        problems.append("deleteRecord=%d restoreRecord=%d" % (n_del, n_res))
    try:
        for line in subprocess.run(["git", "diff", "--name-only", "HEAD"], cwd=ROOT, capture_output=True, text=True, check=True).stdout.splitlines():
            if re.search(r"app/schemas/|RecordsMigration|RecordsDatabase\.kt|RecordsDao\.kt", line):
                problems.append("forbidden file changed: " + line)
            m = re.search(r"/ancnative/ui/([^/]+)$", line)
            if m and m.group(1) not in ALLOWED_UI:
                problems.append("ui file outside allow list: " + line)
    except (OSError, subprocess.CalledProcessError) as exc:
        problems.append("git diff failed: " + str(exc))
    db = read(MAIN / "storage/RecordsDatabase.kt")
    record("S416 repository keeps one deleteRecord/restoreRecord; no schema, version, DAO or stray ui change",
           not problems and bool(re.search(r"version\s*=\s*6\b", db)), "; ".join(problems) or "version != 6")
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
