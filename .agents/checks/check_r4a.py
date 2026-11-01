#!/usr/bin/env python3
"""S110-S114 static source check for round 4a (SPEC_FIX_R4.md, section "Round 4a").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime. Comments are stripped before matching.
  S110 medicationStatuses lists the four statuses in order; "Patient non-compliant" occurs once in main
       Kotlin; effectiveMedicationStatus and isOngoingMedication are each defined once.
  S111 removed event, reducer branch, test tags and string resources are gone from main sources and
       strings.xml (the is_prophylaxis DB column is data and stays; only the string key is checked).
  S112 "[Prophylaxis]" and "Active Medications & Prophylaxis" are gone from main; isProphylaxis is still
       stored (RecordEntities, RecordsDao, ClinicalCommands, EncryptedBackupManager).
  S113 MedicationStatusBadge is defined once in ClinicalBadges.kt and used in WorkspaceScreen.kt.
  S114 "Save to Codex" and "Save to Clinic Medication Codex" are gone from MedicationEditor.kt and from
       the medication part of CodexSettingsSections.kt.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main"
KOTLIN = MAIN / "java/com/sherifradwan/ancnative"
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


def find(sources, name):
    for path, code in sources.items():
        if path.name == name:
            return code
    return None


def medication_part(code):
    """Text of CodexSettingsSections.kt from the first medication-related line to the end of its section.

    The file mixes several lists, so only lines mentioning medication (case-insensitive) and the
    lines around them are kept: every function whose body mentions medication is included whole.
    """
    parts = re.split(r"(?m)^(?=(?:private |internal )?(?:@Composable\s+)?(?:private |internal )?fun\s)", code)
    return "\n".join(p for p in parts if re.search(r"medication|Medication", p))


def check_s110():
    problems = []
    sources = main_sources()
    joined = "\n".join(sources.values())
    models = find(sources, "ClinicalModels.kt") or ""
    match = re.search(r"\bval\s+medicationStatuses\b[^=\n]*=\s*(\w+)\(([^)]*)\)", models)
    expected = ["Active", "Completed", "Discontinued", "Patient non-compliant"]
    if not match:
        problems.append("medicationStatuses not found in ClinicalModels.kt")
    else:
        if match.group(1) not in ("listOf", "persistentListOf"):
            problems.append("medicationStatuses must be an ordered list, found " + match.group(1) + "(...)")
        tokens = re.findall(r'"([^"]*)"|\b(MEDICATION_STATUS_NON_COMPLIANT|MEDICATION_STATUS_ACTIVE)\b', match.group(2))
        names = {"MEDICATION_STATUS_NON_COMPLIANT": "Patient non-compliant", "MEDICATION_STATUS_ACTIVE": "Active"}
        listed = [a or names[b] for a, b in tokens]
        if listed != expected:
            problems.append("medicationStatuses is " + str(listed))
    n = joined.count('"Patient non-compliant"')
    if n != 1:
        problems.append('literal "Patient non-compliant" occurs ' + str(n) + " times, expected 1")
    if len(re.findall(r"\bconst\s+val\s+MEDICATION_STATUS_NON_COMPLIANT\b|\bval\s+MEDICATION_STATUS_NON_COMPLIANT\b", joined)) != 1:
        problems.append("MEDICATION_STATUS_NON_COMPLIANT not defined exactly once")
    for name in ("effectiveMedicationStatus", "isOngoingMedication"):
        count = len(re.findall(r"\bfun\s+" + name + r"\s*\(", joined))
        if count != 1:
            problems.append(name + " defined " + str(count) + " times, expected 1")
    record("S110 four ordered statuses; one literal; one definition per rule", not problems, "; ".join(problems))


def check_s111():
    problems = []
    sources = main_sources()
    strings = read(STRINGS) or ""
    names = ("MedicationProphylaxisChanged", "updateMedicationProphylaxis", "medProphylaxis", "medNonCompliant")
    # The string keys share a name with the stored DB column (is_prophylaxis), so only resource uses count.
    string_keys = ("prophylaxis_badge", "therapeutic_badge", "non_compliant_badge", "is_prophylaxis")
    for name in names:
        hits = [p.name for p, code in sources.items() if re.search(r"\b" + name + r"\b", code)]
        if re.search(r"\b" + name + r"\b", strings):
            hits.append("strings.xml")
        if hits:
            problems.append(name + " in " + ", ".join(hits))
    for key in string_keys:
        hits = [p.name for p, code in sources.items() if re.search(r"\bR\.string\." + key + r"\b", code)]
        if re.search(r'name="' + key + '"', strings):
            hits.append("strings.xml")
        if hits:
            problems.append("string " + key + " in " + ", ".join(hits))
    record("S111 removed event, reducer branch, tags and strings gone", not problems, "; ".join(problems))


def check_s112():
    problems = []
    sources = main_sources()
    joined = "\n".join(sources.values())
    for text in ("[Prophylaxis]", "Active Medications & Prophylaxis"):
        if text in joined:
            problems.append(repr(text) + " still in main")
    for name in ("RecordEntities.kt", "RecordsDao.kt", "ClinicalCommands.kt", "EncryptedBackupManager.kt"):
        code = find(sources, name)
        if code is None:
            problems.append(name + " missing")
        elif "isProphylaxis" not in code:
            problems.append("isProphylaxis dropped from " + name)
    record("S112 prophylaxis text gone; stored field kept", not problems, "; ".join(problems))


def check_s113():
    problems = []
    sources = main_sources()
    joined = "\n".join(sources.values())
    definitions = [p.name for p, code in sources.items() if re.search(r"\bfun\s+MedicationStatusBadge\s*\(", code)]
    if definitions != ["ClinicalBadges.kt"]:
        problems.append("MedicationStatusBadge defined in " + str(definitions) + ", expected only ClinicalBadges.kt")
    if len(re.findall(r"\bfun\s+MedicationStatusBadge\s*\(", joined)) > 1:
        problems.append("MedicationStatusBadge defined more than once")
    workspace = find(sources, "WorkspaceScreen.kt") or ""
    if not re.search(r"\bMedicationStatusBadge\s*\(", workspace):
        problems.append("MedicationStatusBadge not used in WorkspaceScreen.kt")
    record("S113 MedicationStatusBadge defined once and used by the profile card", not problems, "; ".join(problems))


def check_s114():
    problems = []
    sources = main_sources()
    editor = find(sources, "MedicationEditor.kt")
    settings = find(sources, "CodexSettingsSections.kt")
    if editor is None:
        problems.append("MedicationEditor.kt missing")
    if settings is None:
        problems.append("CodexSettingsSections.kt missing")
    scopes = {
        "MedicationEditor.kt": editor or "",
        "CodexSettingsSections.kt (medication part)": medication_part(settings or ""),
    }
    for scope, code in scopes.items():
        for text in ("Save to Codex", "Save to Clinic Medication Codex"):
            if text in code:
                problems.append(repr(text) + " in " + scope)
    record("S114 old codex wording gone from the medication UI", not problems, "; ".join(problems))


def main():
    check_s110()
    check_s111()
    check_s112()
    check_s113()
    check_s114()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
