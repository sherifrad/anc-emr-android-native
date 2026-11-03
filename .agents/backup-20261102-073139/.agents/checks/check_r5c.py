#!/usr/bin/env python3
"""S220-S223 static source check for round 5c (SPEC_DESIGN_R5.md, section "Round 5c").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S220 MedicationEditor.kt calls SectionCard(, SummaryRow( and BigChoiceButton( and has no direct Card( call.
  S221 MedicationEditor.kt carries every tag the spec names (M401-M404) and the kept ones.
  S222 "Prophylaxis" does not appear in MedicationEditor.kt except the one isProphylaxis = false assignment.
  S223 no file under model/, state/ or storage/ changed against HEAD.
"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / "app/src/main/java/com/sherifradwan/ancnative/ui"

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


def editor_code():
    src = read(UI / "MedicationEditor.kt")
    return None if src is None else strip_comments(src)


def check_s220():
    problems = []
    code = editor_code()
    if code is None:
        problems.append("ui/MedicationEditor.kt missing")
    else:
        for name in ("SectionCard", "SummaryRow", "BigChoiceButton"):
            if not re.search(r"\b" + name + r"\s*\(", code):
                problems.append("MedicationEditor.kt does not call " + name + "(")
        if re.search(r"(?<![A-Za-z])Card\s*\(", code):
            problems.append("MedicationEditor.kt still has a direct Card( call")
    record("S220 medication screen uses the shared pieces, no hand-written Card(", not problems, "; ".join(problems))


def check_s221():
    problems = []
    code = editor_code()
    if code is None:
        problems.append("ui/MedicationEditor.kt missing")
    else:
        for tag in ("medPickCard", "medDetailsCard", "medStatusCard", "medMoreToggle", "medMoreSection",
                    "medName", "medDosage", "medFrequency", "medStartDate", "medEndDate", "medAdverseNotes",
                    "medNotes", "medRouteChoice", "freqPreset", "medPreset", "patientPrevMed",
                    "saveToMedCodexBanner", "saveToMedCodexButton", "cancelMedication",
                    "addAnotherMedication", "saveMedication", "medStatusChoice", "medicationEditor"):
            if '"' + tag not in code:
                problems.append('missing tag "' + tag + '"')
        for prefix in ("medRow-", "medOpen-"):
            if '"' + prefix not in code:
                problems.append('missing tag prefix "' + prefix + '"')
        for key in ("dose", "route", "frequency", "start"):
            if '"' + key + '"' not in code:
                problems.append("guided row key " + key + " not wired")
    record("S221 medication tags and guided row wiring present", not problems, "; ".join(problems))


def check_s222():
    problems = []
    code = editor_code()
    if code is None:
        problems.append("ui/MedicationEditor.kt missing")
    else:
        for line in code.splitlines():
            if re.search("prophylaxis", line, re.I) and not re.search(r"isProphylaxis\s*=\s*false", line):
                problems.append("Prophylaxis left: " + line.strip())
    record("S222 no Prophylaxis control on the medication screen", not problems, "; ".join(problems))


def check_s223():
    problems = []
    try:
        out = subprocess.run(["git", "diff", "--name-only", "HEAD"], cwd=ROOT, capture_output=True, text=True, check=True).stdout
    except (OSError, subprocess.CalledProcessError) as exc:
        record("S223 no logic files changed", False, "git diff failed: " + str(exc))
        return
    for line in out.splitlines():
        if re.search(r"/ancnative/(model|state|storage)/", line) and "/src/test/" not in line:
            problems.append("logic file changed: " + line)
    record("S223 design round touched no model/state/storage file", not problems, "; ".join(problems))


def main():
    check_s220()
    check_s221()
    check_s222()
    check_s223()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
