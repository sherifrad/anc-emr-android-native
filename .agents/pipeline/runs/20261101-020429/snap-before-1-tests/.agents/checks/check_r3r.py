#!/usr/bin/env python3
"""S83 static source check for round 3r (SPEC_FIX_R3.md, section "Round 3r").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S83 no occurrence of FLAG_SECURE in any file under app/src/main;
      MainActivity.kt still calls enableEdgeToEdge() and still contains setContent.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN_DIR = ROOT / "app/src/main"
MAIN_ACTIVITY = MAIN_DIR / "java/com/sherifradwan/ancnative/MainActivity.kt"

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


def check_s83():
    problems = []

    # Check that FLAG_SECURE does not occur in any file under app/src/main
    hits = []
    for path in sorted(MAIN_DIR.rglob("*")):
        if not path.is_file():
            continue
        if path.suffix in (".png", ".webp", ".jpg", ".jar", ".keystore", ".jks"):
            continue
        content = read(path)
        if content is None:
            continue

        if path.suffix == ".kt":
            code = strip_comments(content)
        elif path.suffix == ".xml":
            code = strip_xml_comments(content)
        else:
            code = content

        if "FLAG_SECURE" in code:
            rel = path.relative_to(ROOT)
            hits.append(str(rel))

    if hits:
        problems.append("FLAG_SECURE found in: " + ", ".join(hits))

    # MainActivity.kt still calls enableEdgeToEdge() and still contains setContent
    act_src = read(MAIN_ACTIVITY)
    if act_src is None:
        problems.append("MainActivity.kt missing")
    else:
        act_code = strip_comments(act_src)
        if not re.search(r"\benableEdgeToEdge\s*\(", act_code):
            problems.append("MainActivity.kt does not call enableEdgeToEdge()")
        if "setContent" not in act_code:
            problems.append("MainActivity.kt does not contain setContent")

    record("S83 no occurrence of FLAG_SECURE under app/src/main; MainActivity calls enableEdgeToEdge and contains setContent",
           not problems, "; ".join(problems))


def main():
    check_s83()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
