#!/usr/bin/env python3
"""S92-S93 static source check for round 3v (SPEC_FIX_R3.md, section "Round 3v").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S92 state/DraftMapping.kt defines activeDraftSlot, draftRecordFor, applyDraftRecord,
      closedDraftSlot and has no android import; state/DraftAutosaver.kt defines DraftStore and
      DraftAutosaver with onStateChanged, flush, cancelPending and uses no Log/SharedPreferences/File.
  S93 VaultController defines saveDraft, loadDraft, deleteDraft each under storageMutex;
      AncViewModel references DraftAutosaver and manualLock / onBackgrounded mention flush before
      controller.lock / controller.onBackgrounded; no ui/ file mentions DraftAutosaver;
      EncryptedBackupManager.kt has no "draft".
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
MAPPING = MAIN / "state/DraftMapping.kt"
AUTOSAVER = MAIN / "state/DraftAutosaver.kt"
VIEWMODEL = MAIN / "state/AncViewModel.kt"
CONTROLLER = MAIN / "storage/VaultController.kt"
BACKUP = MAIN / "storage/EncryptedBackupManager.kt"

results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return None


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"(?<!:)//[^\n]*", "", src)


def code_of(path, problems):
    src = read(path)
    if src is None:
        problems.append(path.name + " missing")
        return None
    return strip_comments(src)


def function_body(code, name):
    """Text from 'fun name' up to the next declaration at the same indent."""
    m = re.search(r"^([ \t]*)(?:\w+\s+)*fun\s+" + re.escape(name) + r"\b", code, flags=re.M)
    if not m:
        return None
    rest = code[m.end():]
    nxt = re.search(r"^" + m.group(1) + r"(?:\w+\s+)*(?:fun|val|var|companion|class)\b", rest, flags=re.M)
    return rest[:nxt.start()] if nxt else rest


FORBIDDEN = re.compile(
    r"SharedPreferences|\bLog\s*\.|\bandroid\.util\.Log\b|\bFile\s*\(|\bFileOutputStream\b"
    r"|\bwriteText\b|\bwriteBytes\b|\bFiles\s*\.\s*write|\bopenFileOutput\b"
)


def check_s92():
    problems = []
    mapping = code_of(MAPPING, problems)
    if mapping is not None:
        for name in ("activeDraftSlot", "draftRecordFor", "applyDraftRecord", "closedDraftSlot"):
            if not re.search(r"\bfun\s+" + name + r"\s*\(", mapping):
                problems.append("DraftMapping.kt lacks fun " + name)
        if re.search(r"\bimport\s+android\b", mapping):
            problems.append("DraftMapping.kt imports android")
    saver = code_of(AUTOSAVER, problems)
    if saver is not None:
        if not re.search(r"\binterface\s+DraftStore\b", saver):
            problems.append("DraftAutosaver.kt lacks interface DraftStore")
        if not re.search(r"\bclass\s+DraftAutosaver\b", saver):
            problems.append("DraftAutosaver.kt lacks class DraftAutosaver")
        for name in ("onStateChanged", "flush", "cancelPending"):
            if not re.search(r"\bfun\s+" + name + r"\s*\(", saver):
                problems.append("DraftAutosaver.kt lacks fun " + name)
        hit = FORBIDDEN.search(saver)
        if hit:
            problems.append("DraftAutosaver.kt uses " + hit.group(0))
    record("S92 DraftMapping has the four mapping functions and no android import; DraftAutosaver has the store, "
           "onStateChanged, flush, cancelPending and no Log/SharedPreferences/File",
           not problems, "; ".join(problems))


def check_s93():
    problems = []
    ctl = code_of(CONTROLLER, problems)
    if ctl is not None:
        for name in ("saveDraft", "loadDraft", "deleteDraft"):
            body = function_body(ctl, name)
            if body is None:
                problems.append("VaultController lacks " + name)
            elif "storageMutex" not in body:
                problems.append(name + " does not use storageMutex")
    vm = code_of(VIEWMODEL, problems)
    if vm is not None:
        if "DraftAutosaver" not in vm:
            problems.append("AncViewModel does not reference DraftAutosaver")
        for name, call in (("manualLock", "controller.lock"), ("onBackgrounded", "controller.onBackgrounded")):
            body = function_body(vm, name)
            if body is None:
                problems.append("AncViewModel lacks " + name)
                continue
            f = body.find("flush")
            c = body.find(call)
            if f < 0:
                problems.append(name + " does not mention flush")
            elif c < 0:
                problems.append(name + " does not call " + call)
            elif f > c:
                problems.append(name + " mentions flush after " + call)
    for path in sorted((MAIN / "ui").rglob("*.kt")):
        code = code_of(path, problems)
        if code is not None and "DraftAutosaver" in code:
            problems.append("ui/" + path.name + " mentions DraftAutosaver")
    backup = code_of(BACKUP, problems)
    if backup is not None and re.search(r"draft", backup, flags=re.I):
        problems.append(BACKUP.name + " mentions draft")
    record("S93 VaultController draft methods use storageMutex; AncViewModel owns DraftAutosaver and flushes before "
           "lock/background; no ui file mentions DraftAutosaver; backup manager has no draft",
           not problems, "; ".join(problems))


def main():
    check_s92()
    check_s93()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
