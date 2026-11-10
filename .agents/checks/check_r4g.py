#!/usr/bin/env python3
"""S170-S175 static source check for round 4g (SPEC_FIX_R4.md, section "Round 4g").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime. Comments are stripped before matching.
  S170 eddFromScan is defined nowhere in main.
  S171 the patient draft carries none of the eight removed dating fields.
  S172 intakeDatingUpdate and intakeDatingPreview are defined nowhere in main.
  S173 the view model builds no post-save dating command (no datingCmd).
  S174 DraftMapping.kt does not mention the eight removed dating names in the patient branch.
  S175 the patient editor does not call the old LMP-only functions and calculateForLmp is gone from main sources.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
ISO = MAIN / "model/IsoDates.kt"
STATE = MAIN / "state/AncState.kt"
VM = MAIN / "state/AncViewModel.kt"
MAPPING = MAIN / "state/DraftMapping.kt"
EDITOR = MAIN / "ui/PatientEditor.kt"
FIELDS = ("lmpDate", "datingMethod", "transferDate", "embryoAgeDays", "agreedEdd", "scanDate", "scanGaWeeks", "scanGaDays")
ENUMS = ("LMP_DATE", "DATING_METHOD", "TRANSFER_DATE", "EMBRYO_AGE_DAYS", "AGREED_EDD", "SCAN_DATE", "SCAN_GA_WEEKS", "SCAN_GA_DAYS")

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


def block(src, header):
    m = re.search(header, src)
    if not m:
        return ""
    depth, i, started = 0, m.end(), False
    while i < len(src):
        if src[i] in "({":
            depth += 1
            started = True
        elif src[i] in ")}":
            depth -= 1
            if started and depth <= 0:
                return src[m.start():i + 1]
        i += 1
    return src[m.start():]


def check_s170():
    found = defined_in(r"\b(?:fun\s+(?:[\w.<>?]+\.)?|va[lr]\s+(?:[\w.<>?]+\.)?)eddFromScan\b")
    record("S170 eddFromScan is gone from main", not found, "eddFromScan defined in " + ", ".join(found))


def check_s171():
    st = code(STATE)
    values = block(st, r"data\s+class\s+PatientDraftValues\s*\(")
    enum = block(st, r"enum\s+class\s+PatientDraftField\s*\{")
    problems = []
    if not values:
        problems.append("PatientDraftValues not found")
    if not enum:
        problems.append("PatientDraftField not found")
    problems += ["PatientDraftValues has " + f for f in FIELDS if re.search(r"\bval\s+" + f + r"\b", values)]
    problems += ["PatientDraftField has " + e for e in ENUMS if re.search(r"\b" + e + r"\b", enum)]
    problems += ["reducer handles PatientDraftField." + e for e in ENUMS if re.search(r"PatientDraftField\." + e + r"\b", st)]
    record("S171 the patient draft carries none of the eight removed dating fields", not problems, "; ".join(problems[:6]))


def check_s172():
    problems = []
    for name in ("intakeDatingUpdate", "intakeDatingPreview"):
        found = defined_in(r"\bfun\s+" + name + r"\s*\(")
        if found:
            problems.append(name + " defined in " + ", ".join(found))
    if (MAIN / "state/IntakeDatingRule.kt").exists():
        problems.append("state/IntakeDatingRule.kt exists")
    record("S172 the intake dating rule stays removed", not problems, "; ".join(problems))


def check_s173():
    problems = []
    if not VM.exists():
        problems.append("AncViewModel.kt missing")
    vm = code(VM)
    if "datingCmd" in vm:
        problems.append("AncViewModel.kt contains datingCmd")
    if re.search(r"\bSaveDatingCommand\s*\(", vm):
        problems.append("AncViewModel.kt constructs SaveDatingCommand(")
    record("S173 the view model builds no post-save dating command", not problems, "; ".join(problems))


def check_s174():
    src = code(MAPPING)
    patient = block(src, r"state\.patientDraft\?\.let\s*")
    restore = block(src, r"DraftScreenKind\.PATIENT_PROFILE\s*->\s*")
    problems = []
    if not patient:
        problems.append("patient save block not found")
    if not restore:
        problems.append("patient restore block not found")
    for f in FIELDS:
        if '"' + f + '"' in patient or '"' + f + '"' in restore:
            problems.append("patient save or restore still uses " + f)
    record("S174 the patient draft is not saved or restored with the removed dating keys", not problems, "; ".join(problems))


def check_s175():
    problems = []
    ed = code(EDITOR)
    for banned in ("eddFromLmp(", "calculateForLmp("):
        if banned in ed:
            problems.append("PatientEditor.kt still calls " + banned)
    if defined_in(r"\bcalculateForLmp\b"):
        problems.append("calculateForLmp still exists in main sources")
    record("S175 editor free of LMP-only calls; calculateForLmp gone", not problems, "; ".join(problems))


check_s170()
check_s171()
check_s172()
check_s173()
check_s174()
check_s175()
sys.exit(0 if all(results) else 1)
