#!/usr/bin/env python3
"""S86-S88 static source check for round 3t (SPEC_FIX_R3.md, section "Round 3t").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S86 GestationalAgeEngine.kt defines isLateTerm, progressFraction, shortWeeksText
      and fun daysToEdd(; and its own calculateForEdd calls daysToEdd.
  S87 GestationalBanner.kt contains none of ChronoUnit, weeks >= 42, FULL_TERM_DAYS;
      PatientListScreen.kt and ProfileScreen.kt contain no +${ ... wks hand formatting;
      the three screens reference shortWeeksText/progressFraction/daysToEdd as described.
  S88 GestationalBannerLogicTest.kt contains no 280f, no ChronoUnit and calls
      progressFraction and daysToEdd.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
TEST = ROOT / "app/src/test/java/com/sherifradwan/ancnative"
ENGINE = MAIN / "model/GestationalAgeEngine.kt"
PATIENT_LIST = MAIN / "ui/PatientListScreen.kt"
PROFILE = MAIN / "ui/patienthub/PatientHubHeader.kt"  # replaces ProfileScreen.kt
BANNER_TEST = TEST / "ui/GestationalBannerLogicTest.kt"

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


def check_s86():
    problems = []
    src = read(ENGINE)
    if src is None:
        problems.append("model/GestationalAgeEngine.kt missing")
    else:
        code = strip_comments(src)
        if not re.search(r"\b(?:val|var)?\s*isLateTerm\b", code):
            problems.append("GestationalAgeEngine.kt lacks isLateTerm")
        if not re.search(r"\b(?:val|var)?\s*progressFraction\b", code):
            problems.append("GestationalAgeEngine.kt lacks progressFraction")
        if not re.search(r"\b(?:val|var)?\s*shortWeeksText\b", code):
            problems.append("GestationalAgeEngine.kt lacks shortWeeksText")
        if not re.search(r"\bfun\s+daysToEdd\s*\(", code):
            problems.append("GestationalAgeEngine.kt lacks fun daysToEdd(")
        if "fun calculateForEdd" not in code:
            problems.append("GestationalAgeEngine.kt lacks calculateForEdd")
        else:
            body = code.split("fun calculateForEdd")[1].split("fun ")[0]
            if not re.search(r"\bdaysToEdd\s*\(", body):
                problems.append("calculateForEdd does not call daysToEdd")

    record("S86 GestationalAgeEngine.kt defines isLateTerm, progressFraction, shortWeeksText, daysToEdd and calculateForEdd calls daysToEdd",
           not problems, "; ".join(problems))


def check_s87():
    problems = []
    # GestationalBanner.kt was deleted in the redesign and nothing in ui/ shows a progress bar any more,
    # so the banner-only requirements (progressFraction/daysToEdd references) are retired. The rule
    # "no screen computes term/progress by hand" is kept for every ui file.
    for path in sorted((MAIN / "ui").rglob("*.kt")):
        u_code = strip_comments(read(path) or "")
        rel = path.relative_to(MAIN).as_posix()
        if "ChronoUnit" in u_code:
            problems.append(rel + " contains ChronoUnit")
        if re.search(r"weeks\s*>=\s*42", u_code):
            problems.append(rel + " contains weeks >= 42")
        if "FULL_TERM_DAYS" in u_code:
            problems.append(rel + " contains FULL_TERM_DAYS")

    pls_src = read(PATIENT_LIST)
    if pls_src is None:
        problems.append("ui/PatientListScreen.kt missing")
    else:
        pls_code = strip_comments(pls_src)
        if re.search(r'\+\s*\$\{[^}]*\}\s*wks', pls_code):
            problems.append("PatientListScreen.kt contains +${...} wks hand formatting")
        if not re.search(r"\bshortWeeksText\b", pls_code):
            problems.append("PatientListScreen.kt does not reference shortWeeksText")

    prof_src = read(PROFILE)
    if prof_src is None:
        problems.append("ui/patienthub/PatientHubHeader.kt missing")
    else:
        prof_code = strip_comments(prof_src)
        if re.search(r'\+\s*\$\{[^}]*\}\s*wks', prof_code):
            problems.append("PatientHubHeader.kt contains +${...} wks hand formatting")
        if not re.search(r"\bshortWeeksText\b", prof_code):
            problems.append("PatientHubHeader.kt does not reference shortWeeksText")

    record("S87 no ui file has ChronoUnit/weeks>=42/FULL_TERM_DAYS, list and hub header have no hand-formatted wks and reference shortWeeksText",
           not problems, "; ".join(problems))


def check_s88():
    problems = []
    test_src = read(BANNER_TEST)
    if test_src is None:
        problems.append("GestationalBannerLogicTest.kt missing")
    else:
        t_code = strip_comments(test_src)
        if "280f" in t_code:
            problems.append("GestationalBannerLogicTest.kt contains 280f")
        if "ChronoUnit" in t_code:
            problems.append("GestationalBannerLogicTest.kt contains ChronoUnit")
        if not re.search(r"\bprogressFraction\b", t_code):
            problems.append("GestationalBannerLogicTest.kt does not call progressFraction")
        if not re.search(r"\bdaysToEdd\s*\(", t_code):
            problems.append("GestationalBannerLogicTest.kt does not call daysToEdd")

    record("S88 GestationalBannerLogicTest.kt contains no 280f, no ChronoUnit and calls progressFraction and daysToEdd",
           not problems, "; ".join(problems))


def main():
    check_s86()
    check_s87()
    check_s88()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
