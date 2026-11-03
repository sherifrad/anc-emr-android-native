#!/usr/bin/env python3
"""S97-S98 static source check for round 3x (SPEC_FIX_R3.md, section "Round 3x").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S97 PatientPrintHtmlGenerator.kt has no `word-break: break-word`, contains `white-space: nowrap`,
      and the text "Official Stamp & Signature" occurs exactly once.
  S98 PatientPrintHtmlGenerator.kt defines hiddenRowsHtml and calls it at least 4 times, defines the
      four MAX_PRINT_* constants (VISITS, SCANS, LABS, MEDICATIONS) and has no `.take(` with a numeric literal.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GENERATOR = ROOT / "app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt"

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


def code_of(path, problems):
    src = read(path)
    if src is None:
        problems.append(path.name + " missing")
        return None
    return strip_comments(src)


def check_s97():
    problems = []
    code = code_of(GENERATOR, problems)
    if code is not None:
        if re.search(r"word-break\s*:\s*break-word", code):
            problems.append("still contains word-break: break-word")
        if not re.search(r"white-space\s*:\s*nowrap", code):
            problems.append("no white-space: nowrap")
        n = code.count("Official Stamp & Signature")
        if n != 1:
            problems.append("'Official Stamp & Signature' occurs " + str(n) + " times, expected 1")
    record("S97 print CSS has no break-word, has nowrap; stamp text printed once", not problems, "; ".join(problems))


def check_s98():
    problems = []
    code = code_of(GENERATOR, problems)
    if code is not None:
        defs = len(re.findall(r"\bfun\s+hiddenRowsHtml\s*\(", code))
        if defs != 1:
            problems.append("fun hiddenRowsHtml defined " + str(defs) + " times, expected 1")
        calls = len(re.findall(r"\bhiddenRowsHtml\s*\(", code)) - defs
        if calls < 4:
            problems.append("hiddenRowsHtml called " + str(calls) + " times, expected >= 4")
        for suffix in ("VISITS", "SCANS", "LABS", "MEDICATIONS"):
            if not re.search(r"\bval\s+MAX_PRINT_" + suffix + r"\b", code):
                problems.append("no val MAX_PRINT_" + suffix)
        if re.search(r"\.take\(\s*\d+\s*\)", code):
            problems.append(".take( with a numeric literal remains")
    record("S98 hiddenRowsHtml used >= 4 times, MAX_PRINT_* constants defined, no literal .take(", not problems,
           "; ".join(problems))


def main():
    check_s97()
    check_s98()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
