#!/usr/bin/env python3
"""S67-S71 static source check for round 3m (SPEC_FIX_R3.md, section "Round 3m").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S67 model/VisitVitals.kt exists with parseBloodPressure, parseWeightKg,
      isPlausibleBloodPressure, isPlausibleWeightKg, bpLevel, enum BpLevel
      (NORMAL/RAISED/SEVERE) and no "android." import.
  S68 storage/SaveVisitCommand.kt calls parseBloodPressure(, parseWeightKg( and
      both plausibility functions, has a `today` parameter, and state/AncState.kt
      DraftError lists DATE_IN_FUTURE, BLOOD_PRESSURE_INVALID, WEIGHT_INVALID.
  S69 no second parser: outside model/VisitVitals.kt no file under app/src/main
      has a split(/Regex( call near a bloodPressure reference.
  S70 ui/VisitEditor.kt uses the three new strings and calls bpLevel( or
      bpAlertResource(; strings.xml defines visit_date_in_future,
      blood_pressure_invalid, weight_invalid, bp_alert_raised, bp_alert_severe.
  S71 ui/ClinicalBadges.kt defines fun BpAlertLabel, and WorkspaceScreen.kt and
      ProfileScreen.kt call it via BpAlertLabel(.
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


def enum_body(src, enum_name):
    """Text of the enum <enum_name> { ... } declaration (first brace block)."""
    m = re.search(r"\benum\s+class\s+" + enum_name + r"\b", src)
    if m is None:
        return None
    brace = src.find("{", m.end())
    if brace == -1:
        return None
    depth = 0
    for i in range(brace, len(src)):
        if src[i] == "{":
            depth += 1
        elif src[i] == "}":
            depth -= 1
            if depth == 0:
                return src[brace:i]
    return None


def check_s67():
    problems = []
    src = read(VITALS)
    if src is None:
        problems.append("model/VisitVitals.kt missing")
    else:
        src = strip_comments(src)
        for name in (
            "parseBloodPressure",
            "parseWeightKg",
            "isPlausibleBloodPressure",
            "isPlausibleWeightKg",
            "bpLevel",
        ):
            if not re.search(r"\bfun\s+" + name + r"\s*\(", src):
                problems.append("VisitVitals.kt lacks fun " + name + "(")
        body = enum_body(src, "BpLevel")
        if body is None:
            problems.append("VisitVitals.kt lacks enum class BpLevel")
        else:
            for value in ("NORMAL", "RAISED", "SEVERE"):
                if not re.search(r"\b" + value + r"\b", body):
                    problems.append("enum BpLevel lacks " + value)
        if re.search(r"^import\s+android\.", src, re.M):
            problems.append("VisitVitals.kt imports android. (must stay pure Kotlin)")
    record("S67 VisitVitals.kt is the ONE parser with bpLevel and no android import",
           not problems, "; ".join(problems))


def check_s68():
    problems = []
    svc = read(MAIN / "storage/SaveVisitCommand.kt")
    if svc is None:
        problems.append("storage/SaveVisitCommand.kt missing")
    else:
        svc = strip_comments(svc)
        for name in (
            "parseBloodPressure(",
            "parseWeightKg(",
            "isPlausibleBloodPressure(",
            "isPlausibleWeightKg(",
        ):
            if name not in svc:
                problems.append("SaveVisitCommand.kt does not call " + name)
        if not re.search(r"\btoday\s*:", svc):
            problems.append("SaveVisitCommand.kt prepare has no `today:` parameter")

    state = read(MAIN / "state/AncState.kt")
    if state is None:
        problems.append("state/AncState.kt missing")
    else:
        body = enum_body(strip_comments(state), "DraftError")
        if body is None:
            problems.append("AncState.kt lacks enum class DraftError")
        else:
            for value in ("DATE_IN_FUTURE", "BLOOD_PRESSURE_INVALID", "WEIGHT_INVALID"):
                if not re.search(r"\b" + value + r"\b", body):
                    problems.append("DraftError lacks " + value)
    record("S68 SaveVisitCommand validates through the one parser, DraftError gained the 3 values",
           not problems, "; ".join(problems))


def check_s69():
    problems = []
    if not VITALS.exists():
        problems.append("model/VisitVitals.kt missing (nothing to compare against)")
    else:
        hits = []
        for path in sorted(MAIN.rglob("*.kt")):
            if path.name == "VisitVitals.kt":
                continue
            code = strip_comments(read(path) or "")
            lines = [strip_quoted(line) for line in code.splitlines()]
            for i, line in enumerate(lines):
                if re.search(r"\bsplit\s*\(|\bRegex\s*\(", line):
                    window = lines[max(0, i - 2):i + 3]
                    if any(re.search(r"(?<!\w)bloodPressure(?!\w)", w) for w in window):
                        hits.append(path.name + ":" + str(i + 1))
        if hits:
            problems.append("a second BP parser remains: " + ", ".join(hits))
    record("S69 no second blood-pressure parser outside VisitVitals.kt", not problems, "; ".join(problems))


def check_s70():
    problems = []
    editor = read(MAIN / "ui/VisitEditor.kt")
    if editor is None:
        problems.append("ui/VisitEditor.kt missing")
    else:
        editor = strip_comments(editor)
        for res in ("visit_date_in_future", "blood_pressure_invalid", "weight_invalid"):
            if "R.string." + res not in editor:
                problems.append("VisitEditor.kt does not use R.string." + res)
        if "bpLevel(" not in editor and "bpAlertResource(" not in editor:
            problems.append("VisitEditor.kt calls neither bpLevel( nor bpAlertResource(")

    strings = read(RES / "strings.xml")
    if strings is None:
        problems.append("res/values/strings.xml missing")
    else:
        strings = strip_xml_comments(strings)
        for res in ("visit_date_in_future", "blood_pressure_invalid", "weight_invalid",
                    "bp_alert_raised", "bp_alert_severe"):
            if not re.search(r'name="' + res + r'"', strings):
                problems.append('strings.xml lacks name="' + res + '"')
    record("S70 VisitEditor shows the new errors and live BP hint, strings defined",
           not problems, "; ".join(problems))


def check_s71():
    problems = []
    badges = read(MAIN / "ui/ClinicalBadges.kt")
    if badges is None:
        problems.append("ui/ClinicalBadges.kt missing")
    else:
        if not re.search(r"\bfun\s+BpAlertLabel\s*\(", strip_comments(badges)):
            problems.append("ClinicalBadges.kt lacks fun BpAlertLabel(")

    for rel in ("ui/WorkspaceScreen.kt", "ui/ProfileScreen.kt"):
        src = read(MAIN / rel)
        if src is None:
            problems.append(rel + " missing")
            continue
        if "BpAlertLabel(" not in strip_comments(src):
            problems.append(rel + " does not call BpAlertLabel(")
    record("S71 visits show the alert via ONE BpAlertLabel in ClinicalBadges", not problems, "; ".join(problems))


def main():
    check_s67()
    check_s68()
    check_s69()
    check_s70()
    check_s71()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
