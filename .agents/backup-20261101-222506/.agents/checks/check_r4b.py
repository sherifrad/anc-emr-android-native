#!/usr/bin/env python3
"""S120-S122 static source check for round 4b (SPEC_FIX_R4.md, section "Round 4b").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime. Comments are stripped before matching.
  S120 commonUltrasoundImpressions and impressionPreset are gone from app/src; ultrasoundImpressionFromChoices
       and mergeImpression are each defined once, in ClinicalModels.kt.
  S121 UltrasoundEditor.kt has the testTags impressionFromChoices, ultrasoundLiveSummary, fillNormalScan,
       clearFindings and impression, calls both functions and uses the three NORMAL_ constants.
  S122 the impression wording ("amniotic fluid", "placenta previa") lives only in ClinicalModels.kt among main sources.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "app/src"
MAIN = SRC / "main"
EDITOR = "UltrasoundEditor.kt"
MODELS = "ClinicalModels.kt"

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


def sources(root):
    return {p: strip_comments(read(p) or "") for p in sorted(root.rglob("*.kt"))}


def check_s120():
    problems = []
    for path, code in sources(SRC).items():
        for word in ("commonUltrasoundImpressions", "impressionPreset"):
            if word in code:
                problems.append(word + " in " + path.name)
    main = sources(MAIN)
    for fn in ("ultrasoundImpressionFromChoices", "mergeImpression"):
        homes = [p.name for p, c in main.items() for _ in re.findall(r"\bfun\s+" + fn + r"\s*\(", c)]
        if homes != [MODELS]:
            problems.append(fn + " defined in " + str(homes) + ", expected [" + MODELS + "]")
    record("S120 old impression presets gone; one definition per rule", not problems, "; ".join(problems))


def check_s121():
    problems = []
    code = next((c for p, c in sources(MAIN).items() if p.name == EDITOR), None)
    if code is None:
        problems.append(EDITOR + " missing")
    else:
        for tag in ("impressionFromChoices", "ultrasoundLiveSummary", "fillNormalScan", "clearFindings", "impression"):
            if not re.search(r'"' + tag + r'"', code):
                problems.append("testTag " + tag + " missing")
        for call in ("ultrasoundImpressionFromChoices", "mergeImpression",
                     "NORMAL_FETAL_NUMBER", "NORMAL_PRESENTATION", "NORMAL_AMNIOTIC_FLUID"):
            if not re.search(r"\b" + call + r"\b", code):
                problems.append(call + " not used")
    record("S121 editor has the new controls and calls the shared rule", not problems, "; ".join(problems))


def check_s122():
    problems = []
    for path, code in sources(MAIN).items():
        if path.name != MODELS and re.search(r'amniotic fluid"|placenta previa', code, flags=re.I):
            problems.append("impression wording in " + path.name)
    models = next((c for p, c in sources(MAIN).items() if p.name == MODELS), "")
    if not re.search(r"placenta previa", models):
        problems.append("wording missing from " + MODELS)
    record("S122 impression wording in one place", not problems, "; ".join(problems))


def main():
    check_s120()
    check_s121()
    check_s122()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
