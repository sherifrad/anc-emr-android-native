#!/usr/bin/env python3
"""Usage: find_unused.py [--root app/src/main/java] [--tests app/src/test app/src/androidTest] [--files FILE_LIST]
Purpose: report code that nothing uses. Read-only.
  1. unused imports: an imported simple name that no other line of the file mentions;
  2. unused private members: a private fun, val or var whose name appears only once in its file;
  3. unreferenced declarations: a top-level or member fun, class, object or val whose name appears nowhere else in main or test sources.
Group 3 is a candidate list only: reflection, test tags, Compose previews and framework callbacks can use a name without mentioning it."""
import argparse, collections, os, re

ap = argparse.ArgumentParser()
ap.add_argument("--root", default="app/src/main/java")
ap.add_argument("--tests", nargs="*", default=["app/src/test", "app/src/androidTest"])
ap.add_argument("--files", default="")
a = ap.parse_args()

def kt_files(root):
    return [os.path.join(d, f) for d, _, fs in os.walk(root) for f in fs if f.endswith(".kt")]

main = kt_files(a.root)
only = set(open(a.files).read().split()) if a.files else None
alltxt = {p: open(p, encoding="utf-8", errors="replace").read() for p in main}
for t in a.tests:
    for p in kt_files(t): alltxt[p] = open(p, encoding="utf-8", errors="replace").read()
word = lambda n: re.compile(r"(?<![A-Za-z0-9_])" + re.escape(n) + r"(?![A-Za-z0-9_])")
counts = collections.Counter()
for t in alltxt.values():
    counts.update(re.findall(r"[A-Za-z_][A-Za-z0-9_]*", t))

def body_without_imports(t):
    return "\n".join(l for l in t.split("\n") if not l.startswith(("import ", "package ")))

out = []
for p in sorted(main):
    if only is not None and p not in only: continue
    t = alltxt[p]; b = body_without_imports(t)
    for m in re.finditer(r"^import ([\w.]+?)(?: as (\w+))?$", t, re.M):
        name = m.group(2) or m.group(1).split(".")[-1]
        if name == "*": continue
        if not word(name).search(b):
            out.append(("unused-import", p, t[:m.start()].count("\n") + 1, m.group(0)))
    for m in re.finditer(r"^\s*private\s+(?:inline\s+|const\s+|lateinit\s+|tailrec\s+|operator\s+)*(?:fun|val|var)\s+(?:<[^>]+>\s*)?(?:[\w.<>?]+\.)?(\w+)", t, re.M):
        n = m.group(1)
        if len(word(n).findall(b)) <= 1:
            out.append(("unused-private", p, t[:m.start()].count("\n") + 1, n))
    for m in re.finditer(r"^(?:\s*(?:internal|public)?\s*)(?:(?:data|sealed|enum|open|abstract|inline)\s+)*(?:fun|class|object|interface)\s+(?:<[^>]+>\s*)?(?:[\w.<>?]+\.)?(\w+)", t, re.M):
        n = m.group(1)
        if counts[n] <= 1 and not n.startswith(("test", "Test")) and n not in ("main", "onCreate"):
            out.append(("unreferenced", p, t[:m.start()].count("\n") + 1, n))
for kind, p, line, what in out:
    print(f"{kind}\t{p}:{line}\t{what}")
print(f"# {len(out)} findings in {len(main)} main files")
