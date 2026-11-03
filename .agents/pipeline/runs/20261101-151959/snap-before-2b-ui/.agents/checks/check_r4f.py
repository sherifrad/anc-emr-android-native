#!/usr/bin/env python3
"""S160-S163 static source check for round 4f (SPEC_FIX_R4.md, section "Round 4f").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime. Comments are stripped before matching.
  S160 one per-method due date rule; the estimated due date and the gestational age both use it.
  S161 per-method due dates (for the side-by-side screen) defined once.
  S162 the dating draft rules live once in state/DatingDraftRule.kt.
  S163 the draft reducer and the dating screen call the draft rules.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
ENGINE = MAIN / "model/GestationalAgeEngine.kt"
RULE = MAIN / "state/DatingDraftRule.kt"
STATE = MAIN / "state/AncState.kt"
EDITOR = MAIN / "ui/DatingEditor.kt"

results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return ""


def code(path):
    src = re.sub(r"/\*.*?\*/", "", read(path), flags=re.S)
    return re.sub(r"(?<!:)//[^\n]*", "", src)


def defined_in(pattern):
    return [p.name for p in MAIN.rglob("*.kt") if re.search(pattern, code(p))]


def body(src, name):
    m = re.search(r"\bfun\s+" + name + r"\s*\(", src)
    if not m:
        return ""
    depth, i, started = 0, m.end(), False
    while i < len(src):
        c = src[i]
        if c == "{":
            depth += 1
            started = True
        elif c == "}":
            depth -= 1
            if started and depth == 0:
                return src[m.start():i + 1]
        i += 1
    return src[m.start():]


def check_s160():
    problems = []
    eng = code(ENGINE)
    if defined_in(r"\bfun\s+dueDateForMethod\s*\(") != [ENGINE.name]:
        problems.append("dueDateForMethod not defined exactly once in the engine")
    if "dueDateForMethod(" not in body(eng, "calculateEstimatedDueDate"):
        problems.append("calculateEstimatedDueDate does not call dueDateForMethod")
    if "calculateEstimatedDueDate(" not in body(eng, "calculate"):
        problems.append("calculate does not use calculateEstimatedDueDate")
    for fn in ("eddFromLmp", "ivfEddFromTransfer"):
        n = len(re.findall(r"\b" + fn + r"\b", eng))
        if n != 1:
            problems.append(fn + " called " + str(n) + " times in the engine (want 1)")
    record("S160 one per-method due date rule", not problems, "; ".join(problems))


def check_s161():
    ok = (defined_in(r"\bfun\s+dueDatesByMethod\s*\(") == [ENGINE.name]
          and defined_in(r"\bclass\s+DueDatesByMethod\b") == [ENGINE.name])
    record("S161 per-method due dates defined once", ok, "dueDatesByMethod / DueDatesByMethod missing or duplicated")


def check_s162():
    bad = [n for n in ("applyDatingChange", "datingDraftToDating", "isManualAgreedEdd")
           if defined_in(r"\bfun\s+" + n + r"\s*\(") != [RULE.name]]
    record("S162 dating draft rules defined once in DatingDraftRule.kt", not bad, "not once in DatingDraftRule.kt: " + ", ".join(bad))


def check_s163():
    problems = []
    st = code(STATE)
    if "applyDatingChange(" not in st:
        problems.append("AncState.kt does not call applyDatingChange")
    if re.search(r"copy\(\s*lmpDate\s*=\s*event\.value", st):
        problems.append("AncState.kt still sets lmpDate directly")
    ed = code(EDITOR)
    if "datingDraftToDating(" not in ed:
        problems.append("DatingEditor.kt does not call datingDraftToDating")
    if re.search(r"\bDemoPatientDating\s*\(", ed):
        problems.append("DatingEditor.kt still builds DemoPatientDating itself")
    record("S163 reducer and dating screen call the draft rules", not problems, "; ".join(problems))


check_s160()
check_s161()
check_s162()
check_s163()
sys.exit(0 if all(results) else 1)
