#!/usr/bin/env python3
"""S78-S79 static source check for round 3p (SPEC_FIX_R3.md, section "Round 3p").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S78 PatientPrintHtmlGenerator.kt contains no "en-sub" at all (neither the
      capital duplicate-title span nor the `.section-title .en-sub` CSS rule)
      and the 8 `class="section-title"` blocks are still present. The source
      always carries 9 blocks (the page-3 photo section, "Clinical Imaging &
      Ultrasound Scans", is dropped at runtime when the patient has no scan
      photos), so the floor used here is 8.
  S79 PatientTwoPageReportDialog.kt: the Text whose text is
      "Clinical Medical Record" sets `maxLines = 2` (not 1); the three report
      dialog testTags closeReportDialog / shareReportButton /
      printReportButton are still present.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "app/src/main/java/com/sherifradwan/ancnative"

GENERATOR_FILE = SRC / "ui/report/PatientPrintHtmlGenerator.kt"
DIALOG_FILE = SRC / "ui/report/PatientTwoPageReportDialog.kt"

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


def close_paren(src, open_idx):
    """Index of the ')' matching the '(' at open_idx; -1 when unbalanced.
    Skips double-quoted string literals (escape aware)."""
    depth = 0
    i = open_idx
    n = len(src)
    while i < n:
        c = src[i]
        if c == '"':
            i += 1
            while i < n:
                if src[i] == "\\":
                    i += 2
                    continue
                if src[i] == '"':
                    break
                i += 1
            if i >= n:
                return -1
            i += 1
            continue
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def check_s78():
    problems = []
    src = read(GENERATOR_FILE)
    if src is None:
        problems.append("ui/report/PatientPrintHtmlGenerator.kt missing")
    else:
        code = strip_comments(src)
        n_titles = len(re.findall(r'class="section-title"', code))
        if n_titles < 8:
            problems.append(
                'expected 8 (or more) class="section-title" blocks, found %d' % n_titles
            )
        en_sub = len(re.findall(r"en-sub", code))
        if en_sub:
            problems.append("en-sub still present %d time(s)" % en_sub)
    record("S78 section titles keep their single block, capital en-sub duplicates and CSS rule gone",
           not problems, "; ".join(problems))


def check_s79():
    problems = []
    src = read(DIALOG_FILE)
    if src is None:
        problems.append("ui/report/PatientTwoPageReportDialog.kt missing")
        record("S79 dialog title allows two lines, report testTags kept",
               False, "; ".join(problems))
        return
    code = strip_comments(src)
    idx = code.find('text = "Clinical Medical Record"')
    if idx == -1:
        problems.append('no Text with text = "Clinical Medical Record"')
    else:
        call_open = code.rfind("Text(", 0, idx)
        paren = code.find("(", call_open)
        close = close_paren(code, paren)
        if call_open == -1 or paren == -1 or close == -1:
            problems.append("could not read the title Text( ... ) block")
        else:
            block = code[call_open:close]
            if re.search(r"\bmaxLines\s*=\s*1\b", block):
                problems.append("title Text still has maxLines = 1")
            if not re.search(r"\bmaxLines\s*=\s*2\b", block):
                problems.append("title Text does not set maxLines = 2")
    for tag in ("printReportButton", "shareReportButton", "closeReportDialog"):
        if not re.search(r'testTag\("%s"\)' % tag, code):
            problems.append("testTag(\"%s\") missing" % tag)
    record("S79 dialog title allows two lines, report testTags kept",
           not problems, "; ".join(problems))


def main():
    check_s78()
    check_s79()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
