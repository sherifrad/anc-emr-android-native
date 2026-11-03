#!/usr/bin/env python3
"""S80-S82 static source check for round 3q (SPEC_FIX_R3.md, section "Round 3q").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S80 VisitVitals.kt defines severeBpAttention and SevereBpAttention and still
      contains the only thresholds (160 / 110 / 140 / 90 appear in no other main file
      besides VisitVitals.kt in BP context and plausibility ranges are unchanged).
  S81 PatientListScreen.kt and WorkspaceScreen.kt call severeBpAttention( and
      use R.string.bp_badge_severe / R.string.bp_attention_severe; strings.xml defines
      bp_badge_severe and bp_attention_severe; WorkspaceScreen.kt has testTag("severeBpAttention").
  S82 PatientPrintHtmlGenerator.kt calls bpLevel( and contains `bp-alert-severe`;
      no `split("/")` BP parsing or threshold literal added there.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
RES = ROOT / "app/src/main/res/values"
VITALS = MAIN / "model/VisitVitals.kt"

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


def strip_xml_comments(src):
    return re.sub(r"<!--.*?-->", "", src, flags=re.S)


def strip_quoted(line):
    """Remove string-literal contents so a mention inside quotes is not code."""
    return re.sub(r'"[^"]*"', '""', line)


def check_s80():
    problems = []
    src = read(VITALS)
    if src is None:
        problems.append("model/VisitVitals.kt missing")
    else:
        code = strip_comments(src)
        if not re.search(r"\bdata\s+class\s+SevereBpAttention\b", code):
            problems.append("VisitVitals.kt lacks data class SevereBpAttention")
        if not re.search(r"\bfun\s+severeBpAttention\s*\(", code):
            problems.append("VisitVitals.kt lacks fun severeBpAttention(")
        if not (re.search(r"60\.\.260", code) and re.search(r"30\.\.160", code) and re.search(r"30\.0\.\.250\.0", code)):
            problems.append("plausibility ranges in VisitVitals.kt were changed")

    # Check that thresholds (160 / 110 / 140 / 90) appear in no other main file in BP context
    hits = []
    for path in sorted(MAIN.rglob("*.kt")):
        if path.name == "VisitVitals.kt":
            continue
        c = strip_comments(read(path) or "")
        lines = [strip_quoted(line) for line in c.splitlines()]
        for i, line in enumerate(lines, 1):
            if any(term in line for term in ("systolic", "diastolic", "bloodPressure", "bpLevel")):
                for num in ("160", "110", "140", "90"):
                    if re.search(r"\b" + num + r"\b", line):
                        hits.append(f"{path.name}:{i}")
    if hits:
        problems.append("BP threshold numbers found in other main file(s): " + ", ".join(hits))

    record("S80 VisitVitals.kt defines severeBpAttention/SevereBpAttention with the only thresholds and unchanged plausibility",
           not problems, "; ".join(problems))


def check_s81():
    problems = []
    pls = read(MAIN / "ui/PatientListScreen.kt")
    if pls is None:
        problems.append("ui/PatientListScreen.kt missing")
    else:
        code = strip_comments(pls)
        if "severeBpAttention(" not in code:
            problems.append("PatientListScreen.kt does not call severeBpAttention(")
        if "R.string.bp_badge_severe" not in code:
            problems.append("PatientListScreen.kt does not use R.string.bp_badge_severe")
        if "R.string.bp_attention_severe" not in code:
            problems.append("PatientListScreen.kt does not use R.string.bp_attention_severe")

    ws = read(MAIN / "ui/WorkspaceScreen.kt")
    if ws is None:
        problems.append("ui/WorkspaceScreen.kt missing")
    else:
        code = strip_comments(ws)
        if "severeBpAttention(" not in code:
            problems.append("WorkspaceScreen.kt does not call severeBpAttention(")
        if "R.string.bp_attention_severe" not in code:
            problems.append("WorkspaceScreen.kt does not use R.string.bp_attention_severe")
        if 'testTag("severeBpAttention")' not in code:
            problems.append('WorkspaceScreen.kt lacks testTag("severeBpAttention")')

    strings = read(RES / "strings.xml")
    if strings is None:
        problems.append("res/values/strings.xml missing")
    else:
        s_code = strip_xml_comments(strings)
        if not re.search(r'name="bp_badge_severe"', s_code):
            problems.append('strings.xml lacks name="bp_badge_severe"')
        if not re.search(r'name="bp_attention_severe"', s_code):
            problems.append('strings.xml lacks name="bp_attention_severe"')

    record("S81 PatientListScreen and WorkspaceScreen show severe BP badge/attention line, strings defined",
           not problems, "; ".join(problems))


def check_s82():
    problems = []
    gen = read(MAIN / "ui/report/PatientPrintHtmlGenerator.kt")
    if gen is None:
        problems.append("ui/report/PatientPrintHtmlGenerator.kt missing")
    else:
        code = strip_comments(gen)
        if "bpLevel(" not in code:
            problems.append("PatientPrintHtmlGenerator.kt does not call bpLevel(")
        if "bp-alert-severe" not in code:
            problems.append('PatientPrintHtmlGenerator.kt lacks "bp-alert-severe"')
        if re.search(r'split\s*\(\s*"/"\s*\)', code):
            problems.append('PatientPrintHtmlGenerator.kt contains split("/") BP parsing')

        lines = [strip_quoted(line) for line in code.splitlines()]
        for i, line in enumerate(lines, 1):
            if any(term in line for term in ("systolic", "diastolic", "bloodPressure", "bpLevel")):
                for num in ("160", "110", "140", "90"):
                    if re.search(r"\b" + num + r"\b", line):
                        problems.append(f"threshold literal {num} added to PatientPrintHtmlGenerator.kt:{i}")

    record("S82 PatientPrintHtmlGenerator uses bpLevel and bp-alert-severe without split or threshold literals",
           not problems, "; ".join(problems))


def main():
    check_s80()
    check_s81()
    check_s82()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
