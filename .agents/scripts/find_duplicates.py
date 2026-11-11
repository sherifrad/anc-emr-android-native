#!/usr/bin/env python3
"""Usage: find_duplicates.py [--root DIR] [--window N] [--min-lines N] [--out FILE]
Purpose: report blocks of Kotlin code that appear more than once. Lines are normalised (comments and spaces removed;
  imports, package, blank lines and lines with only braces are ignored); a window of N significant lines is hashed and
  overlapping matches are merged into maximal duplicate runs. Read-only: it only prints a report.
Output: one cluster per duplicate run, longest first, with every file and line range where it occurs."""
import argparse, collections, hashlib, os, re, sys

ap = argparse.ArgumentParser()
ap.add_argument("--root", default="app/src/main/java")
ap.add_argument("--window", type=int, default=8)
ap.add_argument("--min-lines", type=int, default=10)
ap.add_argument("--out", default="")
a = ap.parse_args()

def sig_lines(path):
    out = []
    in_block = False
    for n, raw in enumerate(open(path, encoding="utf-8", errors="replace"), 1):
        line = raw.strip()
        if in_block:
            if "*/" in line: in_block = False
            continue
        if line.startswith("/*"):
            if "*/" not in line: in_block = True
            continue
        line = re.sub(r"//.*$", "", line).strip()
        if not line or line.startswith(("import ", "package ", "@Suppress", "@OptIn")) or re.fullmatch(r"[{}()\[\],]+", line):
            continue
        out.append((n, re.sub(r"\s+", " ", line)))
    return out

files = [os.path.join(d, f) for d, _, fs in os.walk(a.root) for f in fs if f.endswith(".kt")]
windows = collections.defaultdict(list)
store = {}
for p in files:
    sl = sig_lines(p); store[p] = sl
    for i in range(len(sl) - a.window + 1):
        h = hashlib.md5("\n".join(t for _, t in sl[i:i + a.window]).encode()).hexdigest()
        windows[h].append((p, i))

# merge consecutive matching windows into runs
runs = collections.defaultdict(set)  # key: tuple of (file,start-index) pairs that match each other
seen = set()
clusters = []
for h, occ in windows.items():
    if len(occ) < 2: continue
    for (p, i) in occ:
        if (p, i) in seen: continue
        group = [(q, j) for (q, j) in occ]
        # extend forward while every member of the group still matches the others
        length = a.window
        while True:
            nxt = []
            ok = True
            texts = set()
            for (q, j) in group:
                k = j + length
                if k >= len(store[q]): ok = False; break
                texts.add(store[q][k][1])
            if not ok or len(texts) != 1: break
            length += 1
        for (q, j) in group:
            for d in range(length - a.window + 1): seen.add((q, j + d))
        # only report from the earliest start in each file to avoid sub-runs
        clusters.append((length, group))
reported = []
done = set()
for length, group in sorted(clusters, key=lambda c: -c[0]):
    if length < a.min_lines: continue
    key = tuple(sorted((q, j) for q, j in group))
    if key in done: continue
    done.add(key)
    spans = []
    for (q, j) in group:
        sl = store[q]
        spans.append((q, sl[j][0], sl[j + length - 1][0]))
    # skip a cluster fully inside the same file region as a longer one already reported
    reported.append((length, spans))
lines = []
for length, spans in reported:
    lines.append(f"## {length} significant lines, {len(spans)} places")
    for q, s, e in sorted(spans):
        lines.append(f"  {q}:{s}-{e}")
    lines.append("")
text = "\n".join(lines) + f"\n{len(reported)} duplicate clusters of at least {a.min_lines} lines in {len(files)} files\n"
if a.out: open(a.out, "w").write(text)
print(text if not a.out else f"{len(reported)} clusters -> {a.out}")
