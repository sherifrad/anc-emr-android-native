#!/usr/bin/env python3
"""S200-S203 static source check for round 5a (SPEC_DESIGN_R5.md, section "Round 5a").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S200 ui/DesignSystem.kt defines SectionCard and SummaryRow once; no other file does.
  S201 UltrasoundEditor.kt calls SectionCard( and SummaryRow( and has no direct Card( call.
  S202 UltrasoundEditor.kt carries every tag the spec names (U211-U215).
  S203 no file under model/, state/ or storage/ changed against HEAD.
"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
UI = MAIN / "ui"

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


def check_s200():
    problems = []
    ds = read(UI / "DesignSystem.kt")
    if ds is None:
        problems.append("ui/DesignSystem.kt missing")
    else:
        code = strip_comments(ds)
        for name in ("SectionCard", "SummaryRow"):
            if len(re.findall(r"\bfun\s+" + name + r"\s*\(", code)) != 1:
                problems.append("DesignSystem.kt must define fun " + name + "( exactly once")
    for path in UI.glob("*.kt"):
        if path.name == "DesignSystem.kt":
            continue
        code = strip_comments(read(path) or "")
        for name in ("SectionCard", "SummaryRow"):
            if re.search(r"\bfun\s+" + name + r"\s*\(", code):
                problems.append(path.name + " defines its own " + name)
    record("S200 SectionCard and SummaryRow defined once in DesignSystem.kt", not problems, "; ".join(problems))


def check_s201():
    problems = []
    src = read(UI / "UltrasoundEditor.kt")
    if src is None:
        problems.append("ui/UltrasoundEditor.kt missing")
    else:
        code = strip_comments(src)
        for name in ("SectionCard", "SummaryRow"):
            if not re.search(r"\b" + name + r"\s*\(", code):
                problems.append("UltrasoundEditor.kt does not call " + name + "(")
        if re.search(r"(?<![A-Za-z])Card\s*\(", code):
            problems.append("UltrasoundEditor.kt still has a direct Card( call")
    record("S201 ultrasound screen uses the shared pieces, no hand-written Card(", not problems, "; ".join(problems))


def check_s202():
    problems = []
    src = read(UI / "UltrasoundEditor.kt")
    if src is None:
        problems.append("ui/UltrasoundEditor.kt missing")
    else:
        code = strip_comments(src)
        literal = [
            "ultrasoundFindingsCard", "ultrasoundMoreToggle", "ultrasoundMoreSection",
            "scanCalculatedGaCard", "redatingHint", "applyScanGa", "biometryCard", "toggleBiometry",
            "scanAttachmentsCard", "linkedVisitCard", "ultrasoundLiveSummary", "fillNormalScan",
            "clearFindings", "impressionFromChoices",
        ]
        for tag in literal:
            if '"' + tag not in code:
                problems.append('missing tag "' + tag + '"')
        for key in ("fetalNumber", "presentation", "placenta", "liquor"):
            if "findingOpen-" not in code or "findingRow-" not in code or '"' + key + '"' not in code:
                problems.append("guided finding key " + key + " not wired (findingOpen-/findingRow-/" + key + ")")
        for prefix in ("fetalNumberChoice", "presentationChoice", "placentaChoice", "liquorChoice"):
            if '"' + prefix + '"' not in code:
                problems.append('missing tagPrefix "' + prefix + '"')
    strings = read(ROOT / "app/src/main/res/values/strings.xml") or ""
    for res in ("finding_not_set", "ultrasound_more"):
        if 'name="' + res + '"' not in strings:
            problems.append("strings.xml lacks " + res)
    record("S202 ultrasound tags and guided finding wiring present", not problems, "; ".join(problems))


def check_s203():
    problems = []
    try:
        out = subprocess.run(["git", "diff", "--name-only", "HEAD"], cwd=ROOT, capture_output=True, text=True, check=True).stdout
    except (OSError, subprocess.CalledProcessError) as exc:
        record("S203 no logic files changed", False, "git diff failed: " + str(exc))
        return
    for line in out.splitlines():
        if re.search(r"/ancnative/(model|state|storage)/", line) and "/src/test/" not in line:
            problems.append("logic file changed: " + line)
    record("S203 design round touched no model/state/storage file", not problems, "; ".join(problems))


def main():
    check_s200()
    check_s201()
    check_s202()
    check_s203()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
