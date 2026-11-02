#!/usr/bin/env python3
"""S420-S426 static check for round r6b3 (global delete/Undo UI: DeleteConfirmDialog, UndoBarHost, DeleteIconButton).
Stdlib only; textual contract only."""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
RES = ROOT / "app/src/main/res"
results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return ""


def count_all(pattern):
    """Return {file name: count} of regex matches across app/src/main kotlin files."""
    hits = {}
    for f in sorted(MAIN.rglob("*.kt")):
        n = len(re.findall(pattern, read(f)))
        if n:
            hits[f.name] = n
    return hits


def main():
    ui = read(MAIN / "ui/DeleteUndoUi.kt")
    app = read(MAIN / "ui/AncApp.kt")

    d = count_all(r"\bfun\s+DeleteConfirmDialog\s*\(")
    b = count_all(r"\bfun\s+UndoBarHost\s*\(")
    i = count_all(r"\bfun\s+DeleteIconButton\s*\(")
    record("S420 exactly one DeleteConfirmDialog, one UndoBarHost and one DeleteIconButton definition, all in ui/DeleteUndoUi.kt",
           d == {"DeleteUndoUi.kt": 1} and b == {"DeleteUndoUi.kt": 1} and i == {"DeleteUndoUi.kt": 1}, "%s %s %s" % (d, b, i))

    n_bar = len(re.findall(r"\bUndoBarHost\s*\(", app))
    n_dlg = len(re.findall(r"\bDeleteConfirmDialog\s*\(", app))
    others = {k: v for k, v in count_all(r"\bUndoBarHost\s*\(").items() if k not in ("AncApp.kt", "DeleteUndoUi.kt")}
    record("S421 AncApp mounts UndoBarHost once and DeleteConfirmDialog once; no other screen mounts the bar",
           n_bar == 1 and n_dlg == 1 and not others, "AncApp bar=%d dialog=%d other=%s" % (n_bar, n_dlg, others))

    miss = [n for n in ('"deleteConfirmDialog"', '"deleteConfirm"', '"deleteCancel"', '"undoBar"', '"undoButton"',
                        "AncEvent.CancelDelete", "AncEvent.ConfirmDelete", "AncEvent.UndoRequested", "state.deleteConfirmation",
                        "state.undoOffer", "liveRegion", "minimumTouchTarget", "delete-${type.name.lowercase()}-$recordId")
            if n not in ui]
    record("S422 shared UI uses the required test tags, events, state fields, live region and 48dp touch target", not miss, "missing " + ", ".join(miss))

    bad = []
    for f in sorted(MAIN.rglob("*.kt")):
        src = read(f)
        if f.name == "DeleteUndoUi.kt":
            continue
        if re.search(r"\b(Medication|Lab)[A-Za-z]*(DeleteDialog|DeleteConfirm|UndoBar|UndoSnackbar|DeleteIconButton)\b", src) or \
                re.search(r"\b(Delete|Undo)(Medication|Lab)[A-Za-z]*(Dialog|Bar|Button)\b", src):
            bad.append(f.name)
        if "AlertDialog" in src and re.search(r"AncEvent\.(ConfirmDelete|CancelDelete)", src):
            bad.append(f.name + " builds its own delete AlertDialog")
        if re.search(r'testTag\("delete-(medication|lab)-', src):
            bad.append(f.name + " hard-codes a per-kind delete tag")
    record("S423 no per-kind delete dialog, Undo bar or delete icon anywhere (one mechanism keyed by DeletableType)", not bad, ", ".join(bad))

    ws = read(MAIN / "ui/WorkspaceScreen.kt")
    wired = len(re.findall(r"RequestDelete\s*\(\s*patient\.id\s*,\s*DeletableType\.(MEDICATION|LAB)", ws))
    icons = len(re.findall(r"DeleteIconButton\s*\(\s*DeletableType\.(MEDICATION|LAB)", ws))
    record("S424 WorkspaceScreen wires RequestDelete and DeleteIconButton for medications (cards) and labs", wired >= 2 and icons >= 3 and
           "DeletableType.MEDICATION" in ws and "DeletableType.LAB" in ws, "RequestDelete=%d DeleteIconButton=%d" % (wired, icons))

    strings = read(RES / "values/strings.xml")
    names = ["delete", "undo", "delete_record_description", "delete_confirm_title", "delete_confirm_body", "undo_message_deleted",
             "undo_message_saved", "deletable_type_medication", "deletable_type_lab", "medication_deleted", "medication_restored",
             "medication_save_undone", "lab_deleted", "lab_restored", "lab_save_undone", "show_all_count", "show_fewer"]
    absent = [n for n in names if len(re.findall(r'<string name="%s"' % n, strings)) != 1]
    ar = RES / "values-ar/strings.xml"
    if ar.exists():
        arstr = read(ar)
        absent += ["ar:" + n for n in names if len(re.findall(r'<string name="%s"' % n, arstr)) != 1]
    record("S425 every delete/Undo/notice string exists exactly once in strings.xml (and in values-ar if present)", not absent, ", ".join(absent))

    state = read(MAIN / "state/AncState.kt")
    adapters = read(MAIN / "storage/MedicationDeleteAdapter.kt") + read(MAIN / "storage/LabDeleteAdapter.kt")
    enum = re.search(r"enum class SavedNotice \{(.*?)\}", state, re.S)
    values = set(re.findall(r"\b([A-Z_]+),", enum.group(1))) if enum else set()
    need = {"MEDICATION_DELETED", "MEDICATION_RESTORED", "MEDICATION_SAVE_UNDONE", "LAB_DELETED", "LAB_RESTORED", "LAB_SAVE_UNDONE"}
    alias = re.findall(r"Notice: SavedNotice = SavedNotice\.(MEDICATIONS|LABORATORY)\b", adapters)
    mapped = [n for n in need if "SavedNotice.%s ->" % n not in state]
    record("S426 real SavedNotice values for delete/restore/save-undone, mapped to strings, adapters no longer alias saved notices; "
           "presaved list collapses (presavedToggle)", need <= values and not alias and not mapped and
           "presavedToggle" in read(MAIN / "ui/MedicationEditor.kt") and "collapseAfter" in read(MAIN / "ui/ClinicalPresets.kt"),
           "missing enum %s; alias %s; unmapped %s" % (sorted(need - values), alias, mapped))
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
