#!/usr/bin/env python3
"""S140-S142 static source check for round 4d (SPEC_FIX_R4.md, section "Round 4d").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime. Comments are stripped before matching.
  S140 labResultFlag and resolveLabPanel are each defined once, in their files, and called from LabEditor.kt;
       the UI does not re-implement the range rule.
  S141 EditorPage has a bottomBar parameter defaulting to null; only LabEditor.kt passes it; the three action tags
       occur once each in LabEditor.kt, after the bottomBar argument.
  S142 the lab screen keeps its testTags and shows no "Codex" wording.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main"
UI = MAIN / "java/com/sherifradwan/ancnative/ui"
FLAG = MAIN / "java/com/sherifradwan/ancnative/model/LabResultFlag.kt"
MODELS = MAIN / "java/com/sherifradwan/ancnative/model/codex/ClinicalCodexModels.kt"
LAB = UI / "LabEditor.kt"
PATIENT = UI / "PatientEditor.kt"
TAGS = ["labNameField", "labDate", "labName", "labResult", "labUnit", "labRefRange", "labAbnormal", "labNotes",
        "allLabTestsChip", "labGroupChip-", "labPreset", "labResultPreset", "labCategoryChoice",
        "saveToLabCodexBanner", "saveToLabCodexButton", "cancelLab", "addAnotherLab", "saveLab"]

results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return ""


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"(?<!:)//[^\n]*", "", src)


def code(path):
    return strip_comments(read(path))


def defined_in(name):
    return [p.name for p in MAIN.rglob("*.kt") for _ in re.findall(r"\bfun\s+" + name + r"\s*\(", code(p))]


def check_s140():
    problems = []
    if defined_in("labResultFlag") != [FLAG.name]:
        problems.append("labResultFlag defined in " + str(defined_in("labResultFlag")))
    if defined_in("resolveLabPanel") != [MODELS.name]:
        problems.append("resolveLabPanel defined in " + str(defined_in("resolveLabPanel")))
    lab = code(LAB)
    for fn in ("labResultFlag(", "resolveLabPanel("):
        if fn not in lab:
            problems.append("LabEditor.kt never calls " + fn)
    for p in UI.rglob("*.kt"):
        if re.search(r"Regex\(|toRegex\(|\.toDoubleOrNull\(", code(p)) and p == LAB:
            problems.append("LabEditor.kt parses numbers or ranges itself")
    record("S140 one flag rule and one panel rule, called by the lab screen", not problems, "; ".join(problems))


def check_s141():
    problems = []
    patient = code(PATIENT)
    m = re.search(r"fun\s+EditorPage\s*\((.*?)\)\s*\{", patient, flags=re.S)
    if not m or not re.search(r"bottomBar\s*:\s*\(@Composable\s*\(\)\s*->\s*Unit\)\?\s*=\s*null", m.group(1)):
        problems.append("EditorPage lacks bottomBar: (@Composable () -> Unit)? = null")
    users = [p.name for p in UI.rglob("*.kt") if re.search(r"\bbottomBar\s*=", code(p))]
    if sorted(users) != sorted([LAB.name, "MedicationEditor.kt"]):  # medication editor joined 2026-11-02
        problems.append("bottomBar passed by " + str(users))
    lab = code(LAB)
    pos = lab.find("bottomBar")
    for tag in ("cancelLab", "addAnotherLab", "saveLab"):
        hits = [m.start() for m in re.finditer(r'"' + tag + r'"', lab)]
        if len(hits) != 1:
            problems.append("%s occurs %d times" % (tag, len(hits)))
        elif pos < 0 or hits[0] < pos:
            problems.append(tag + " is not inside the bottom bar")
    record("S141 pinned action bar slot, used only by the lab and medication screens", not problems, "; ".join(problems))


def check_s142():
    problems = []
    lab = code(LAB)
    for tag in TAGS:
        if tag not in lab:
            problems.append("tag lost: " + tag)
    no_tags = re.sub(r"testTag\(\s*\"[^\"]*\"\s*\)", "testTag()", lab)
    for lit in re.findall(r'"[^"\n]*"', no_tags):
        if "codex" in lit.lower():
            problems.append("visible Codex: " + lit)
    record("S142 lab screen keeps its tags, no Codex wording", not problems, "; ".join(problems[:8]))


check_s140()
check_s141()
check_s142()
sys.exit(0 if all(results) else 1)
