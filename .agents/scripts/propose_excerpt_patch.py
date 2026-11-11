#!/usr/bin/env python3
"""Usage: propose_excerpt_patch.py <lane> <out_patch> <task_file> <path>:<first>:<last> [...]
Purpose: like propose_patch.sh, but pastes only line ranges of big files (headed with the real path), so a read-only lane can propose
  SEARCH/REPLACE blocks for large files. Apply with apply_blocks.py (each SEARCH must match exactly once in the real file).
  lane: alphacode | opencode:<model> | agy:<model>."""
import re, subprocess, sys
lane, out, task, *ranges = sys.argv[1:]
head = ("You are a builder with no file access. Output ONLY edit blocks in this exact format, repeated as needed, inside one fenced block: a line FILE: <path relative to the "
        "repository root>, then a line <<<<<<< SEARCH, then the EXACT existing lines to replace copied character for character from the excerpt below (enough lines that the text "
        "occurs exactly once in the file), then a line =======, then the replacement lines, then a line >>>>>>> REPLACE. Do not call any tool. Plain words in code and comments, "
        "no em dash character, no generic comments. Only the excerpt lines are shown; edit only what the task needs.\n\n=== TASK ===\n" + open(task).read())
parts = [head]
for r in ranges:
    path, a, b = r.rsplit(":", 2)
    lines = open(path, encoding="utf-8").read().split("\n")[int(a) - 1:int(b)]
    parts.append(f"\n=== FILE {path} (lines {a}-{b}) ===\n" + "\n".join(lines))
parts.append("\nNow output the edit blocks only.")
prompt = "".join(parts)
if lane == "alphacode":
    cmd = ["alphacode", "run", "--tool-profile", "none", "--quiet", "--no-update", prompt]
elif lane.startswith("opencode:"):
    cmd = ["opencode", "run", "-m", "opencode/" + lane.split(":", 1)[1], "--agent", "plan", prompt]
elif lane.startswith("agy:"):
    cmd = ["agy", "--mode", "plan", "--model", lane.split(":", 1)[1], "--print=" + prompt]
else:
    sys.exit("unknown lane")
r = subprocess.run(cmd, capture_output=True, text=True, timeout=1500)
open(out + ".raw", "w").write(r.stdout); open(out + ".err", "w").write(r.stderr)
t = re.sub(r"^\[Tokens\].*$", "", r.stdout, flags=re.M)
m = re.search(r"```[a-z]*\n(.*?)```", t, re.S)
d = m.group(1) if m else (t if "<<<<<<< SEARCH" in t else "")
open(out, "w").write(d if d.endswith("\n") or not d else d + "\n")
print("prompt bytes:", len(prompt), "block lines:", len(d.splitlines()))
