#!/usr/bin/env python3
"""Text-only independent review through the local OmniRoute gateway (no file access, no tools).
Sends the review brief, the git diff of tracked changes and the contents of new untracked files (code only, size capped) to one
OmniRoute model and prints its answer. Usage: omni_review.py --brief BRIEF.md [--model auto/deepseek] [--paths app/src ...]
The gateway key is read from ~/.omniroute/client.key and never printed. Send code and specs only: never patient data."""
import argparse, json, subprocess, sys, urllib.request
from pathlib import Path

ap = argparse.ArgumentParser()
ap.add_argument("--brief", required=True)
ap.add_argument("--model", default="auto/deepseek")
ap.add_argument("--paths", nargs="*", default=["app/src", ".agents/checks"])
ap.add_argument("--max-chars", type=int, default=120000)
ap.add_argument("--timeout", type=int, default=300)
a = ap.parse_args()

root = Path(__file__).resolve().parents[2]
def sh(*cmd): return subprocess.run(cmd, cwd=root, capture_output=True, text=True).stdout
diff = sh("git", "diff", "HEAD", "--", *a.paths)
new_files = [f for f in sh("git", "ls-files", "--others", "--exclude-standard", "--", *a.paths).split("\n") if f.endswith((".kt", ".py", ".xml"))]
parts = ["REVIEW BRIEF:\n" + Path(a.brief).read_text(), "GIT DIFF (tracked changes):\n" + diff]
for f in new_files:
    parts.append(f"NEW FILE {f}:\n" + (root / f).read_text(errors="ignore"))
prompt = "\n\n".join(parts)
if len(prompt) > a.max_chars:
    prompt = prompt[: a.max_chars] + "\n[TRUNCATED]"
key = (Path.home() / ".omniroute/client.key").read_text().strip()
req = urllib.request.Request(
    "http://127.0.0.1:20128/v1/chat/completions",
    data=json.dumps({"model": a.model, "max_tokens": 3000, "messages": [
        {"role": "system", "content": "You are a strict, independent code reviewer. Reply with text only; quote the lines you rely on; no speculation."},
        {"role": "user", "content": prompt}]}).encode(),
    headers={"Authorization": "Bearer " + key, "Content-Type": "application/json"})
try:
    d = json.load(urllib.request.urlopen(req, timeout=a.timeout))
    print((d.get("choices") or [{}])[0].get("message", {}).get("content") or json.dumps(d)[:500])
except Exception as e:
    print("OMNI_REVIEW_ERROR", type(e).__name__, str(e)[:200]); sys.exit(1)
