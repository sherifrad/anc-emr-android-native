#!/usr/bin/env python3
"""S130-S132 static source check for round 4c (SPEC_FIX_R4.md, section "Round 4c").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime. Comments are stripped before matching.
  S130 applyLabItemEdit is defined once (ClinicalCodexModels.kt); updateLabCodexItem is in the interface and the
       repository, and the repository calls applyLabItemEdit.
  S131 lab settings has the editLabCodexBtn- tag, calls updateLabCodexItem and keeps one AddLabTestDialog.
  S132 no visible "Codex" string in the five UI files; the old testTags are all still present.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main"
CODEX = MAIN / "java/com/sherifradwan/ancnative/model/codex"
UI = MAIN / "java/com/sherifradwan/ancnative/ui"
MODELS = CODEX / "ClinicalCodexModels.kt"
REPO = CODEX / "ClinicalCodexRepository.kt"
SETTINGS = UI / "CodexSettingsSections.kt"
VISIBLE = [SETTINGS, UI / "SecuritySettingsDialog.kt", UI / "LabEditor.kt", UI / "MedicationEditor.kt", MAIN / "res/values/strings.xml"]
OLD_TAGS = ["saveToLabCodexBanner", "saveToLabCodexButton", "saveToMedCodexBanner", "saveToMedCodexButton", "searchLabCodexField",
            "searchMedCodexField", "saveLabCodexConfirmBtn", "saveMedCodexConfirmBtn", "labCodexItem-", "medCodexItem-",
            "deleteLabCodexBtn-", "deleteMedCodexBtn-"]

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


def check_s130():
    problems = []
    homes = [p.name for p in MAIN.rglob("*.kt") for _ in re.findall(r"\bfun\s+applyLabItemEdit\s*\(", code(p))]
    if homes != [MODELS.name]:
        problems.append("applyLabItemEdit defined in " + str(homes))
    if not re.search(r"\bfun\s+updateLabCodexItem\s*\(", code(MODELS)):
        problems.append("interface lacks updateLabCodexItem")
    repo = code(REPO)
    m = re.search(r"override\s+fun\s+updateLabCodexItem\s*\(", repo)
    if not m:
        problems.append("repository lacks updateLabCodexItem")
    elif "applyLabItemEdit(" not in repo[m.end():]:
        problems.append("repository update does not call applyLabItemEdit")
    record("S130 one edit rule, wired through the repository", not problems, "; ".join(problems))


def check_s131():
    src = code(SETTINGS)
    problems = []
    if "editLabCodexBtn-" not in src:
        problems.append("no editLabCodexBtn- tag")
    if "updateLabCodexItem(" not in src:
        problems.append("settings never calls updateLabCodexItem")
    n = len(re.findall(r"\bfun\s+AddLabTestDialog\s*\(", src))
    if n != 1:
        problems.append("AddLabTestDialog defined %d times" % n)
    record("S131 lab rows are editable through the one dialog", not problems, "; ".join(problems))


def check_s132():
    problems = []
    for path in VISIBLE:
        src = code(path)
        no_tags = re.sub(r"testTag\(\s*\"[^\"]*\"\s*\)", "testTag()", src)
        for lit in re.findall(r'"[^"\n]*"', no_tags):
            if "codex" in lit.lower():
                problems.append(path.name + " " + lit)
        if path.suffix == ".xml":
            for text in re.findall(r">([^<]*)<", read(path)):
                if "codex" in text.lower():
                    problems.append(path.name + " " + text.strip())
    joined = "\n".join(code(p) for p in VISIBLE)
    for tag in OLD_TAGS:
        if tag not in joined:
            problems.append("tag lost: " + tag)
    record("S132 no visible Codex wording, tags kept", not problems, "; ".join(problems[:8]))


check_s130()
check_s131()
check_s132()
sys.exit(0 if all(results) else 1)
