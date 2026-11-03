#!/usr/bin/env python3
"""S101-S102 static source check for round 3z (SPEC_FIX_R3.md, section "Round 3z").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime. Comments are stripped before matching.
  S101 formatIsoDate, stringRes, formatFetalMetrics, COMMON_MEDICATION_FREQUENCIES, GenericEditorErrorCard
      and ClinicalRiskBadge(isHighRisk are defined exactly once under app/src/main; PatientPrintHtmlGenerator.kt
      has no ofPattern("yyyy-MM-dd"); patientEditorErrorResource, fun VisitField and ClinicalEditorErrorCard are
      gone; the FHR/EFW metrics literal is only in ClinicalFormat.kt; "Three times daily" is only in
      ClinicalPresets.kt among the ui sources.
  S102 strings.xml has neither unsaved_drafts_policy nor unsaved_drafts_editor_policy and nothing under app/src
      references them.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main"
KOTLIN = MAIN / "java/com/sherifradwan/ancnative"
GENERATOR = KOTLIN / "ui/report/PatientPrintHtmlGenerator.kt"
STRINGS = MAIN / "res/values/strings.xml"

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


def main_sources():
    return {p: strip_comments(read(p) or "") for p in sorted(MAIN.rglob("*.kt"))}


def check_s101():
    problems = []
    sources = main_sources()
    joined = "\n".join(sources.values())
    definitions = {
        "formatIsoDate": r"\bfun\s+formatIsoDate\s*\(",
        "stringRes": r"\bfun\s+SaveUiError\?\.stringRes\s*\(",
        "formatFetalMetrics": r"\bfun\s+formatFetalMetrics\s*\(",
        "COMMON_MEDICATION_FREQUENCIES": r"\bval\s+COMMON_MEDICATION_FREQUENCIES\b",
        "GenericEditorErrorCard": r"\bfun\s+GenericEditorErrorCard\s*\(",
        "ClinicalRiskBadge": r"\bfun\s+ClinicalRiskBadge\s*\(\s*isHighRisk\b",
    }
    for name, pattern in definitions.items():
        n = len(re.findall(pattern, joined))
        if n != 1:
            problems.append(name + " defined " + str(n) + " times, expected 1")
    generator = sources.get(GENERATOR)
    if generator is None:
        problems.append(GENERATOR.name + " missing")
    elif re.search(r'ofPattern\(\s*"yyyy-MM-dd"\s*\)', generator):
        problems.append('PatientPrintHtmlGenerator.kt still uses ofPattern("yyyy-MM-dd")')
    for name, pattern in (
        ("patientEditorErrorResource", r"\bpatientEditorErrorResource\b"),
        ("fun VisitField", r"\bfun\s+VisitField\b"),
        ("ClinicalEditorErrorCard", r"\bClinicalEditorErrorCard\b"),
    ):
        if re.search(pattern, joined):
            problems.append(name + " still exists")
    for path, code in sources.items():
        if re.search(r'"FHR: [^\n]*EFW:', code) and path.name != "ClinicalFormat.kt":
            problems.append("FHR/EFW metrics literal in " + path.name)
        if '"Three times daily"' in code and path.name != "ClinicalPresets.kt" and path.parent.name == "ui":
            problems.append('"Three times daily" in ' + path.name)
    record("S101 one definition per rule; old copies gone", not problems, "; ".join(problems))


def check_s102():
    problems = []
    strings = read(STRINGS)
    if strings is None:
        problems.append("strings.xml missing")
    else:
        for key in ("unsaved_drafts_policy", "unsaved_drafts_editor_policy"):
            if key in strings:
                problems.append(key + " still in strings.xml")
    for path in sorted((ROOT / "app/src").rglob("*")):
        if path.is_file() and path.suffix in (".kt", ".xml", ".java"):
            if re.search(r"unsaved_drafts_(editor_)?policy", read(path) or ""):
                problems.append("reference in " + str(path.relative_to(ROOT)))
    record("S102 unused draft-policy strings removed", not problems,
           "; ".join(problems))


def main():
    check_s101()
    check_s102()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
