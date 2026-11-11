#!/usr/bin/env python3
"""S150-S153 static source check for round 4e (SPEC_FIX_R4.md, section "Round 4e").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime. Comments are stripped before matching.
  S150 the unused second catalog copy is gone from main sources.
  S151 MEDICATION_STATUS_ACTIVE is defined once; no "Active" literal elsewhere in main sources except DemoFixtures.kt.
  S152 labNamePresets is defined once in ClinicalModels.kt and used by LabEditor.kt.
  S153 nextLabPanelTest is defined once, called by LabEditor.kt, which saves its queue with rememberSaveable.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main"
UI = MAIN / "java/com/sherifradwan/ancnative/ui"
CODEX = MAIN / "java/com/sherifradwan/ancnative/model/catalog/ClinicalCatalogModels.kt"
CLINICAL = MAIN / "java/com/sherifradwan/ancnative/model/ClinicalModels.kt"
DEMO = "DemoFixtures.kt"
LAB = UI / "LabEditor.kt"

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



def all_main():
    return [(p, code(p)) for p in MAIN.rglob("*.kt")]


def check_s150():
    bad = [p.name + ":" + n for p, src in all_main() for n in
           ("ClinicalLabPreset", "ClinicalMedPreset", "commonLabPresets", "commonMedPresets") if re.search(r"\b" + n + r"\b", src)]
    record("S150 unused catalog copy removed", not bad, "; ".join(bad))


def check_s151():
    problems = []
    defs = [p.name for p, src in all_main() if re.search(r"\bconst\s+val\s+MEDICATION_STATUS_ACTIVE\b", src)]
    if defs != [CLINICAL.name]:
        problems.append("MEDICATION_STATUS_ACTIVE defined in " + str(defs))
    for p, src in all_main():
        if p.name == DEMO:
            continue
        stripped = re.sub(r'const\s+val\s+MEDICATION_STATUS_ACTIVE\s*=\s*"Active"', "", src)
        if re.search(r'"Active"', stripped):
            problems.append(p.name + " still has an Active literal")
    record("S151 one Active constant", not problems, "; ".join(problems[:8]))


def check_s152():
    problems = []
    defs = [p.name for p, src in all_main() if re.search(r"\bval\s+labNamePresets\b", src)]
    if defs != [CLINICAL.name]:
        problems.append("labNamePresets defined in " + str(defs))
    lab = code(LAB)
    if "labNamePresets" not in lab:
        problems.append("LabEditor.kt does not use labNamePresets")
    if '"Al Borg Labs"' in lab:
        problems.append("LabEditor.kt still has its own lab name list")
    record("S152 one lab name list", not problems, "; ".join(problems))


def check_s153():
    problems = []
    defs = [p.name for p, src in all_main() if re.search(r"\bfun\s+nextLabPanelTest\s*\(", src)]
    if defs != [CODEX.name]:
        problems.append("nextLabPanelTest defined in " + str(defs))
    lab = code(LAB)
    if "nextLabPanelTest(" not in lab:
        problems.append("LabEditor.kt never calls nextLabPanelTest")
    if "rememberSaveable" not in lab:
        problems.append("LabEditor.kt does not use rememberSaveable")
    if re.search(r"mutableStateOf<List<LabCatalogItem>>", lab):
        problems.append("queue still holds items in plain remember state")
    record("S153 panel queue saved across rotation, one advance rule", not problems, "; ".join(problems))


check_s150()
check_s151()
check_s152()
check_s153()
sys.exit(0 if all(results) else 1)
