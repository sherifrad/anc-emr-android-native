#!/usr/bin/env python3
"""S500-S501 static check for the format-string revision (round 7a).

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S500 no string with %n$s / %n$d placeholders is passed to FactRow as a label.
  S501 no such string is rendered directly through stringResource without arguments
       (Text(...), contentDescription = ..., or an inline expression); storing it in a
       val first is allowed, because it may be formatted later with String.format.
"""
import glob
import io
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java"
STRINGS = ROOT / "app/src/main/res/values/strings.xml"
results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return ""


strings = {}
for line in read(STRINGS).splitlines():
    m = re.search(r'<string name="([^"]+)"[^>]*>(.*)</string>', line)
    if m and re.search(r"%\d+\$[sd]", m.group(2)):
        strings[m.group(1)] = m.group(2)

bad_labels = []
bad_direct = []
for f in sorted(glob.glob(str(MAIN / "**/*.kt"), recursive=True)):
    rel = Path(f).relative_to(MAIN).as_posix()
    src = read(Path(f))
    for m in re.finditer(r"FactRow\(\s*R\.string\.(\w+)", src):
        if m.group(1) in strings:
            bad_labels.append(rel + " -> " + m.group(1))
    for m in re.finditer(r"stringResource\(R\.string\.(\w+)\)", src):
        name = m.group(1)
        if name not in strings:
            continue
        line_start = src.rfind("\n", 0, m.start()) + 1
        line = src[line_start:src.find("\n", m.start())]
        # A plain assignment is a template; anything else renders it as-is.
        if re.match(r"\s*val\s+\w+(\s*:\s*[\w<>?]+)?\s*=\s*stringResource", line):
            continue
        bad_direct.append(rel + " -> " + name)

record("S500 no format-template string is used as a FactRow label", not bad_labels, ", ".join(bad_labels))
record("S501 no format-template string is rendered directly without arguments", not bad_direct, ", ".join(bad_direct))

sys.exit(0 if all(results) else 1)
