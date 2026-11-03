#!/usr/bin/env python3
"""S72-S75 static source check for round 3n (SPEC_FIX_R3.md, section "Round 3n").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S72 IsoDates.kt defines ivfGaDaysAtTransfer and ivfEddFromTransfer;
      ClinicalModels.kt has embryoAges = listOf(3, 5, 6); GestationalAgeEngine.kt
      and ClinicalCommands.kt contain none of 17L/19L/263L/261L/!= 3 &&/!= 5,
      both use ClinicalVocabulary.embryoAges, and the engine calls both ivf
      functions (direct call or function reference).
  S73 model/Redating.kt defines RedatingAdvice, redatingThresholdDays,
      redatingAdvice and has no android. import.
  S74 DatingEditor.kt has the embryoDay6 tag and R.string.embryo_day_6;
      strings.xml defines embryo_day_6 and redating_hint.
  S75 UltrasoundEditor.kt calls redatingAdvice (direct call or function
      reference) with the R.string.redating_hint text and a redatingHint tag,
      and the call sits inside the scanCalculatedGaCard Card block.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
RES = ROOT / "app/src/main/res/values"

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


def strip_quoted(src):
    return "\n".join(re.sub(r'"[^"]*"', '""', line) for line in src.splitlines())


def card_block(src, marker):
    """Body text of the <Card( ... ) { ... }> whose params contain marker."""
    i = src.find(marker)
    if i == -1:
        return None
    j = src.rfind("Card(", 0, i)
    if j == -1:
        return None
    depth = 0
    end = len(src)
    for k in range(j + 4, len(src)):
        if src[k] == "(":
            depth += 1
        elif src[k] == ")":
            depth -= 1
            if depth == 0:
                end = k + 1
                break
    open_brace = src.find("{", end)
    if open_brace == -1:
        return None
    depth = 0
    for k in range(open_brace, len(src)):
        if src[k] == "{":
            depth += 1
        elif src[k] == "}":
            depth -= 1
            if depth == 0:
                return src[open_brace:k]
    return None


def uses_function(src, name):
    """A direct call or a function reference (::name) counts as using the rule."""
    return re.search(r"(?:::)?\b" + name + r"\b", src) is not None


def check_s72():
    problems = []
    iso = read(MAIN / "model/IsoDates.kt")
    if iso is None:
        problems.append("model/IsoDates.kt missing")
    else:
        iso = strip_comments(iso)
        for name in ("ivfGaDaysAtTransfer", "ivfEddFromTransfer"):
            if not re.search(r"\bfun\s+" + name + r"\s*\(", iso):
                problems.append("IsoDates.kt lacks fun " + name + "(")

    models = read(MAIN / "model/ClinicalModels.kt")
    if models is None:
        problems.append("model/ClinicalModels.kt missing")
    else:
        models = strip_comments(models)
        if not re.search(r"\bembryoAges\s*=\s*listOf\(3,\s*5,\s*6\)", models):
            problems.append("ClinicalModels.kt lacks embryoAges = listOf(3, 5, 6)")

    banned = [
        ("17L", r"\b17L\b"),
        ("19L", r"\b19L\b"),
        ("263L", r"\b263L\b"),
        ("261L", r"\b261L\b"),
        ("!= 3 &&", r"!=\s*3\s*&&"),
        ("!= 5", r"!=\s*5\b"),
    ]
    for rel in ("model/GestationalAgeEngine.kt", "storage/ClinicalCommands.kt"):
        src = read(MAIN / rel)
        if src is None:
            problems.append(rel + " missing")
            continue
        code = strip_quoted(strip_comments(src))
        hits = []
        for label, pattern in banned:
            if re.search(pattern, code):
                hits.append(label)
        if hits:
            problems.append(rel + " still has literal dating rules: " + ", ".join(hits))
        if not re.search(r"\bClinicalVocabulary\.embryoAges\b", strip_comments(src)):
            problems.append(rel + " does not use ClinicalVocabulary.embryoAges")

    engine = read(MAIN / "model/GestationalAgeEngine.kt")
    if engine is not None:
        code = strip_comments(engine)
        for name in ("ivfGaDaysAtTransfer", "ivfEddFromTransfer"):
            if not uses_function(code, name):
                problems.append("GestationalAgeEngine.kt does not use " + name)
    record("S72 one IVF rule: IsoDates helpers, embryoAges [3,5,6], engine/commands through vocabulary",
           not problems, "; ".join(problems))


def check_s73():
    problems = []
    src = read(MAIN / "model/Redating.kt")
    if src is None:
        problems.append("model/Redating.kt missing")
    else:
        code = strip_comments(src)
        if not re.search(r"\bdata\s+class\s+RedatingAdvice\b", code):
            problems.append("Redating.kt lacks data class RedatingAdvice")
        for name in ("redatingThresholdDays", "redatingAdvice"):
            if not re.search(r"\bfun\s+" + name + r"\s*\(", code):
                problems.append("Redating.kt lacks fun " + name + "(")
        if re.search(r"^import\s+android\.", code, re.M):
            problems.append("Redating.kt imports android. (must stay pure Kotlin)")
    record("S73 model/Redating.kt is the one redating rule, pure Kotlin", not problems, "; ".join(problems))


def check_s74():
    problems = []
    editor = read(MAIN / "ui/DatingEditor.kt")
    if editor is None:
        problems.append("ui/DatingEditor.kt missing")
    else:
        editor = strip_comments(editor)
        if "embryoDay6" not in editor:
            problems.append('DatingEditor.kt lacks the "embryoDay6" test tag')
        if "R.string.embryo_day_6" not in editor:
            problems.append("DatingEditor.kt does not use R.string.embryo_day_6")

    strings = read(RES / "strings.xml")
    if strings is None:
        problems.append("res/values/strings.xml missing")
    else:
        strings = strip_xml_comments(strings)
        for res in ("embryo_day_6", "redating_hint"):
            if not re.search(r'name="' + res + r'"', strings):
                problems.append('strings.xml lacks name="' + res + '"')
    record("S74 dating editor offers the Day 6 chip, strings defined", not problems, "; ".join(problems))


def check_s75():
    problems = []
    editor = read(MAIN / "ui/UltrasoundEditor.kt")
    if editor is None:
        problems.append("ui/UltrasoundEditor.kt missing")
    else:
        code = strip_comments(editor)
        if not uses_function(code, "redatingAdvice"):
            problems.append("UltrasoundEditor.kt does not use redatingAdvice")
        if "redatingHint" not in code:
            problems.append('UltrasoundEditor.kt lacks the "redatingHint" test tag')
        if "R.string.redating_hint" not in code:
            problems.append("UltrasoundEditor.kt does not use R.string.redating_hint")
        card = card_block(code, "scanCalculatedGaCard")
        if card is None:
            problems.append("the scanCalculatedGaCard Card block could not be located")
        elif not uses_function(card, "redatingAdvice"):
            problems.append("redatingAdvice is not inside the scanCalculatedGaCard block")
    record("S75 scan editor shows the suggestion-only redating hint inside the calculated-GA card",
           not problems, "; ".join(problems))


def main():
    check_s72()
    check_s73()
    check_s74()
    check_s75()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
