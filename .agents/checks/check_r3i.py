#!/usr/bin/env python3
"""S51-S54 static source check for round 3i (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S51 LocalDate.parse( appears in app/src/main only in model/IsoDates.kt and model/DemoFixtures.kt.
  S52 plusDays(280, "/ 280f" and "280 -" do not appear in app/src/main outside IsoDates.kt.
  S53 model/IsoDates.kt defines parseIsoDateOrNull, parseCanonicalIsoDateOrNull, FULL_TERM_DAYS, eddFromLmp.
  S54 state/DatingDraftRule.kt references eddFromLmp(, FULL_TERM_DAYS or dueDateForMethod(, and
      state/IntakeDatingRule.kt does not exist.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
ISO = MAIN / "model/IsoDates.kt"

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
    for path in sorted((ROOT / "app/src/main").rglob("*.kt")):
        src = read(path)
        if src is not None:
            yield path, strip_comments(src)


def main():
    allowed_parse = {"IsoDates.kt", "DemoFixtures.kt"}
    problems = [str(p.relative_to(MAIN)) for p, s in main_sources()
                if "LocalDate.parse(" in s and not (p.parent.name == "model" and p.name in allowed_parse)]
    record("S51 LocalDate.parse( only in IsoDates.kt and DemoFixtures.kt", not problems, "found in " + ", ".join(problems))

    problems = []
    for p, s in main_sources():
        if p == ISO:
            continue
        for t in (r"plusDays\(\s*280", r"/\s*280f", r"\b280\s*-"):
            if re.search(t, s):
                problems.append(p.name + " matches " + t)
    record("S52 no hard-coded 280 for gestation outside IsoDates.kt", not problems, "; ".join(problems))

    src = read(ISO)
    problems = []
    if src is None:
        problems.append("model/IsoDates.kt missing")
    else:
        src = strip_comments(src)
        for name, pat in (
            ("parseIsoDateOrNull", r"\bfun\s+parseIsoDateOrNull\s*\("),
            ("parseCanonicalIsoDateOrNull", r"\bfun\s+parseCanonicalIsoDateOrNull\s*\("),
            ("FULL_TERM_DAYS", r"\bconst\s+val\s+FULL_TERM_DAYS\b"),
            ("eddFromLmp", r"\bfun\s+eddFromLmp\s*\("),
        ):
            if not re.search(pat, src):
                problems.append("lacks " + name)
    record("S53 IsoDates.kt defines the four Q69 names", not problems, "; ".join(problems))

    problems = []
    # The banner no longer names the rule: since the gestational-age display round it reads progress, term stage and days
    # to EDD from the engine (model/GestationalAgeEngine.kt), so it is checked for NOT owning any term constant (S52 covers it).
    if (MAIN / "state/IntakeDatingRule.kt").exists():
        problems.append("state/IntakeDatingRule.kt exists again")
    for rel in ("state/DatingDraftRule.kt",):
        src = read(MAIN / rel)
        if src is None:
            problems.append(rel + " missing")
            continue
        code = strip_comments(src)
        if not any(n in code for n in ("eddFromLmp(", "FULL_TERM_DAYS", "dueDateForMethod(")):
            problems.append(rel + " does not use the shared due-date rule (eddFromLmp, FULL_TERM_DAYS or dueDateForMethod)")
    record("S54 dating rule uses the shared due-date rule", not problems, "; ".join(problems))

    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
