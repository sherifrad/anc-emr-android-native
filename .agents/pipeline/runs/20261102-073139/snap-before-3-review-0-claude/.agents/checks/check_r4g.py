#!/usr/bin/env python3
"""S170-S175 static source check for round 4g (SPEC_FIX_R4.md, section "Round 4g").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime. Comments are stripped before matching.
  S170 one scan due-date function.
  S171 new-patient draft carries the dating choice fields.
  S172 the intake dating rule goes through the engine's per-method rule.
  S173 the post-save dating write uses the rule's result, not a hard-coded LMP.
  S174 the new fields are saved and restored with the patient draft.
  S175 the new-patient form calls the preview; the old LMP-only engine function is gone.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
ISO = MAIN / "model/IsoDates.kt"
STATE = MAIN / "state/AncState.kt"
RULE = MAIN / "state/IntakeDatingRule.kt"
VM = MAIN / "state/AncViewModel.kt"
MAPPING = MAIN / "state/DraftMapping.kt"
EDITOR = MAIN / "ui/PatientEditor.kt"
FIELDS = ("datingMethod", "transferDate", "embryoAgeDays", "agreedEdd", "scanDate", "scanGaWeeks", "scanGaDays")
ENUMS = ("DATING_METHOD", "TRANSFER_DATE", "EMBRYO_AGE_DAYS", "AGREED_EDD", "SCAN_DATE", "SCAN_GA_WEEKS", "SCAN_GA_DAYS")

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
    record("S170 one scan due-date function", defined_in(r"\bfun\s+eddFromScan\s*\(") == [ISO.name],
           "eddFromScan not defined exactly once in IsoDates.kt")


def check_s171():
    st = code(STATE)
    values = block(st, r"data\s+class\s+PatientDraftValues\s*\(")
    enum = block(st, r"enum\s+class\s+PatientDraftField\s*\{")
    problems = ["PatientDraftValues lacks " + f for f in FIELDS if not re.search(r"\bval\s+" + f + r"\b", values)]
    problems += ["PatientDraftField lacks " + e for e in ENUMS if not re.search(r"\b" + e + r"\b", enum)]
    problems += ["reducer does not handle " + e for e in ENUMS if not re.search(r"PatientDraftField\." + e + r"\s*->", st)]
    record("S171 new-patient draft carries the dating choice fields", not problems, "; ".join(problems[:6]))


def check_s172():
    problems = []
    rule = code(RULE)
    if not re.search(r"fun\s+intakeDatingUpdate\s*\([^)]*PatientDraftValues", rule):
        problems.append("intakeDatingUpdate does not take PatientDraftValues")
    for needed in ("dueDateForMethod", "eddFromScan"):
        if needed not in rule:
            problems.append("does not use " + needed)
    for banned in ("eddFromLmp(", "ivfEddFromTransfer("):
        if banned in rule:
            problems.append("still calls " + banned)
    if defined_in(r"\bfun\s+intakeDatingPreview\s*\(") != [RULE.name]:
        problems.append("intakeDatingPreview not defined once in IntakeDatingRule.kt")
    record("S172 intake dating goes through the engine rule", not problems, "; ".join(problems))


def check_s173():
    vm = code(VM)
    problems = []
    if re.search(r'datingMethod\s*=\s*"LMP"', vm):
        problems.append("hard-coded LMP method")
    if re.search(r"values\?\.lmpDate", vm):
        problems.append("passes only the LMP to the rule")
    record("S173 post-save dating write uses the rule result", not problems, "; ".join(problems))


def check_s174():
    src = code(MAPPING)
    bad = [f for f in FIELDS if len(re.findall(r'"' + f + r'"', src)) < 2]
    record("S174 new fields saved and restored with the patient draft", not bad, "not saved and restored: " + ", ".join(bad))


def check_s175():
    problems = []
    ed = code(EDITOR)
    if "intakeDatingPreview(" not in ed:
        problems.append("PatientEditor.kt does not call intakeDatingPreview")
    for banned in ("eddFromLmp(", "calculateForLmp("):
        if banned in ed:
            problems.append("PatientEditor.kt still calls " + banned)
    if defined_in(r"\bcalculateForLmp\b"):
        problems.append("calculateForLmp still exists in main sources")
    record("S175 form calls the preview; LMP-only engine function gone", not problems, "; ".join(problems))


check_s170()
check_s171()
check_s172()
check_s173()
check_s174()
check_s175()
sys.exit(0 if all(results) else 1)
