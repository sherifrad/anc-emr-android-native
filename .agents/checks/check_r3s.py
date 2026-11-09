#!/usr/bin/env python3
"""S84-S85 static source check for round 3s (SPEC_FIX_R3.md, section "Round 3s").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S84 model/ObstetricHistory.kt defines PreviousDeliveryCounts, previousDeliveryCounts,
      previousDeliverySummary and has no android. import.
  S85 PatientListScreen.kt, PatientDirectoryMetricsRow.kt, OverviewSections.kt and PatientPrintHtmlGenerator.kt each call
      previousDeliverySummary( and none of the three (nor any other main file besides
      ObstetricHistory.kt) contains "Cesarean" compared to birthMethod, "CS" compared to
      deliveryBy, contains("Cesarean", contains("CS", contains("SVD" or contains("Stillbirth".
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
OBSTETRIC_HISTORY = MAIN / "model/ObstetricHistory.kt"

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


def check_s84():
    problems = []
    src = read(OBSTETRIC_HISTORY)
    if src is None:
        problems.append("model/ObstetricHistory.kt missing")
    else:
        code = strip_comments(src)
        if not re.search(r"\b(?:data\s+)?class\s+PreviousDeliveryCounts\b", code):
            problems.append("ObstetricHistory.kt lacks class PreviousDeliveryCounts")
        if not re.search(r"\bfun\s+previousDeliveryCounts\s*\(", code):
            problems.append("ObstetricHistory.kt lacks fun previousDeliveryCounts(")
        if not re.search(r"\bfun\s+previousDeliverySummary\s*\(", code):
            problems.append("ObstetricHistory.kt lacks fun previousDeliverySummary(")
        if re.search(r"^\s*import\s+android\.", code, flags=re.MULTILINE):
            problems.append("ObstetricHistory.kt contains android. import")

    record("S84 model/ObstetricHistory.kt defines PreviousDeliveryCounts, previousDeliveryCounts, previousDeliverySummary and has no android. import",
           not problems, "; ".join(problems))


def check_s85():
    problems = []
    for filename in ("ui/PatientListScreen.kt", "ui/patientdirectory/PatientDirectoryMetricsRow.kt", "ui/patienthub/components/OverviewSections.kt", "ui/report/PatientPrintHtmlGenerator.kt"):
        fpath = MAIN / filename
        src = read(fpath)
        if src is None:
            problems.append(f"{filename} missing")
        else:
            code = strip_comments(src)
            if "previousDeliverySummary(" not in code:
                problems.append(f"{filename} does not call previousDeliverySummary(")

    for path in sorted(MAIN.rglob("*.kt")):
        if path.name == "ObstetricHistory.kt":
            continue
        src = read(path)
        if src is None:
            continue
        code = strip_comments(src)
        for i, line in enumerate(code.splitlines(), 1):
            if re.search(r'\bbirthMethod\b.*?(?:==|!=|\.equals\s*\()\s*.*?"Cesarean"', line, re.I) or \
               re.search(r'"Cesarean"\s*.*?(?:==|!=|\.equals\s*\()\s*.*?\bbirthMethod\b', line, re.I):
                problems.append(f'{path.name}:{i} contains "Cesarean" compared to birthMethod')

            if re.search(r'\bdeliveryBy\b.*?(?:==|!=|\.equals\s*\()\s*.*?"CS"', line, re.I) or \
               re.search(r'"CS"\s*.*?(?:==|!=|\.equals\s*\()\s*.*?\bdeliveryBy\b', line, re.I):
                problems.append(f'{path.name}:{i} contains "CS" compared to deliveryBy')

            contains_match = re.search(r'contains\s*\(\s*"(?:Cesarean|CS|SVD|Stillbirth)"', line)
            if contains_match:
                problems.append(f'{path.name}:{i} contains {contains_match.group(0)}')

    record("S85 PatientListScreen, directory metrics row, hub overview section, PatientPrintHtmlGenerator call previousDeliverySummary and no file has inline delivery counting comparisons",
           not problems, "; ".join(problems))


def main():
    check_s84()
    check_s85()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
