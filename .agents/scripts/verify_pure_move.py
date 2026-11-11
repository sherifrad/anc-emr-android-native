#!/usr/bin/env python3
"""Prove that splitting one source file into several was a pure move of lines.

Usage:
  .agents/scripts/verify_pure_move.py --old HEAD:app/src/.../AncState.kt --new A.kt B.kt C.kt
  .agents/scripts/verify_pure_move.py --old old_copy.kt --new A.kt B.kt --ignore-visibility

Purpose:
  Strips package lines, import lines, blank lines and leading/trailing whitespace
  from the old file and from the concatenation of the new files, then compares the
  sorted multisets of the remaining lines. Exits 0 only if they are equal; otherwise
  prints every line that is missing or extra (with counts) and exits 1.
  Also compares the names of all declarations (class/interface/object/fun/val/var/
  typealias at any nesting level) and fails if they differ, and prints the
  top-level declaration lists before and after.

  --ignore-visibility: ignore ONLY a leading `private ` or `internal ` modifier when
  comparing lines (for members moved out of an object into top-level `internal`
  functions). Every line whose modifier changed is printed for review.

  --old accepts either a git "rev:path" (read with `git show`) or a plain file path.
"""
import argparse
import collections
import os
import re
import subprocess
import sys

DECL = re.compile(
    r"^(?:(?:public|internal|private|protected|data|sealed|enum|abstract|open|inline|value|const|"
    r"annotation|operator|infix|tailrec|suspend)\s+)*"
    r"(class|interface|object|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?([A-Za-z_][\w.?<>, ]*?)\s*[(:{=<\n]"
)
VIS = re.compile(r"^(?:private|internal) ")


def read_old(spec):
    if os.path.exists(spec):
        with open(spec, encoding="utf-8") as f:
            return f.read()
    if ":" in spec:
        return subprocess.run(["git", "show", spec], check=True, capture_output=True, text=True).stdout
    sys.exit(f"cannot read --old {spec!r}: not a file and not rev:path")


def code_lines(text):
    out = []
    for line in text.splitlines():
        s = line.strip()
        if not s or s.startswith("package ") or s.startswith("import "):
            continue
        out.append(s)
    return out


def declarations(text, top_level_only):
    names = []
    for line in text.splitlines():
        if not line.strip():
            continue
        if top_level_only and line[0].isspace():
            continue
        m = DECL.match(line.strip() + "\n")
        if m:
            names.append(f"{m.group(1)} {m.group(2).strip()}")
    return names


def main():
    ap = argparse.ArgumentParser(description="Prove a file split moved lines without changing them.")
    ap.add_argument("--old", required=True, help="git rev:path or file path of the original file")
    ap.add_argument("--new", required=True, nargs="+", help="the files the original was split into")
    ap.add_argument("--ignore-visibility", action="store_true",
                    help="ignore only a leading 'private ' or 'internal ' modifier; print each change")
    args = ap.parse_args()

    old_text = read_old(args.old)
    new_text = ""
    for path in args.new:
        with open(path, encoding="utf-8") as f:
            new_text += f.read() + "\n"

    old_raw = collections.Counter(code_lines(old_text))
    new_raw = collections.Counter(code_lines(new_text))
    norm = (lambda s: VIS.sub("", s)) if args.ignore_visibility else (lambda s: s)
    old_lines = collections.Counter()
    new_lines = collections.Counter()
    for line, n in old_raw.items():
        old_lines[norm(line)] += n
    for line, n in new_raw.items():
        new_lines[norm(line)] += n

    ok = True
    print(f"code lines: old={sum(old_lines.values())} new={sum(new_lines.values())}")
    missing = old_lines - new_lines
    extra = new_lines - old_lines
    if missing or extra:
        ok = False
        for line, n in sorted(missing.items()):
            print(f"MISSING x{n}: {line}")
        for line, n in sorted(extra.items()):
            print(f"EXTRA   x{n}: {line}")
    else:
        print("line multisets: EQUAL" + (" (ignoring leading private/internal)" if args.ignore_visibility else ""))

    if args.ignore_visibility:
        gone = old_raw - new_raw
        came = new_raw - old_raw
        changed = []
        for line, n in sorted(gone.items()):
            for other, m in came.items():
                if line != other and norm(line) == norm(other):
                    changed.append((n, line, other))
        print(f"modifier changes: {sum(n for n, _, _ in changed)}")
        for n, before, after in changed:
            print(f"  x{n}: {before.split(' ', 1)[0]} -> {after.split(' ', 1)[0]}: {norm(before)}")

    old_names = collections.Counter(declarations(old_text, False))
    new_names = collections.Counter(declarations(new_text, False))
    print(f"declarations (all levels): old={sum(old_names.values())} new={sum(new_names.values())}")
    if old_names != new_names:
        ok = False
        for name, n in sorted((old_names - new_names).items()):
            print(f"DECL MISSING x{n}: {name}")
        for name, n in sorted((new_names - old_names).items()):
            print(f"DECL EXTRA   x{n}: {name}")
    else:
        print("declaration names: EQUAL")

    old_top = collections.Counter(declarations(old_text, True))
    new_top = collections.Counter(declarations(new_text, True))
    print(f"top-level declarations: old={sum(old_top.values())} new={sum(new_top.values())}")
    for name, n in sorted((new_top - old_top).items()):
        print(f"  now top-level x{n}: {name}")
    for name, n in sorted((old_top - new_top).items()):
        print(f"  no longer top-level x{n}: {name}")

    print("RESULT: PASS" if ok else "RESULT: FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
