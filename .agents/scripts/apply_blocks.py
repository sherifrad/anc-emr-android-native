#!/usr/bin/env python3
"""Usage: apply_blocks.py [--check] <blocks_file>
Purpose: apply SEARCH/REPLACE blocks proposed by a read-only lane. Format, repeated:
  FILE: <path relative to the repository root>
  <<<<<<< SEARCH
  <exact existing lines>
  =======
  <replacement lines>
  >>>>>>> REPLACE
Every SEARCH text must occur exactly once in its file. With --check nothing is written. Exit 0 only if every block applies.
An empty SEARCH creates (or appends to) the file. Prints one line per block."""
import argparse, os, re, sys

ap = argparse.ArgumentParser()
ap.add_argument("--check", action="store_true")
ap.add_argument("blocks")
ap.add_argument("--root", default=os.getcwd())
a = ap.parse_args()
text = open(a.blocks, encoding="utf-8").read()
pat = re.compile(r"^FILE:\s*(\S+)\s*\n<<<<<<< SEARCH\n(.*?)\n?=======\n(.*?)\n?>>>>>>> REPLACE", re.S | re.M)
blocks = pat.findall(text)
if not blocks:
    print("no blocks found"); sys.exit(2)
files = {}
bad = 0
for i, (path, old, new) in enumerate(blocks, 1):
    full = os.path.join(a.root, path)
    cur = files.get(path)
    if cur is None:
        cur = open(full, encoding="utf-8").read() if os.path.exists(full) else ""
    if old.strip() == "":
        files[path] = cur + ("" if cur.endswith("\n") or not cur else "\n") + new + "\n"
        print(f"block {i} {path}: append/create ok"); continue
    n = cur.count(old)
    if n != 1:
        print(f"block {i} {path}: SEARCH found {n} times (need exactly 1)"); bad += 1; continue
    files[path] = cur.replace(old, new)
    print(f"block {i} {path}: ok")
if bad:
    print(f"FAILED: {bad} block(s)"); sys.exit(1)
if not a.check:
    for path, content in files.items():
        open(os.path.join(a.root, path), "w", encoding="utf-8").write(content)
print("RESULT: PASS" + (" (check only)" if a.check else " (written)"))
