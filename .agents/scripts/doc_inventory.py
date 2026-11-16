#!/usr/bin/env python3
"""Usage: doc_inventory.py [out.tsv]
Purpose: list every tracked documentation-like file outside app/ and outside machine-run folders
(.agents/backup-*, .agents/pipeline/runs, evidence), with size, last commit date, title line and how many
other tracked docs mention its file name. Input for the doc cleanup classification."""
import subprocess, sys, re, os
root = subprocess.check_output(["git","rev-parse","--show-toplevel"],text=True).strip()
os.chdir(root)
files = subprocess.check_output(["git","ls-files"],text=True).splitlines()
skip = re.compile(r'^(app/|\.agents/backup-|\.agents/pipeline/runs/|evidence/|keystore/|gradle/)')
docs = [f for f in files if not skip.match(f) and re.search(r'\.(md|txt|log|jsonl|json|html)$', f) and f != '.agents/PROJECT_MAP.json']
corpus = {}
for f in docs:
    try: corpus[f] = open(f, errors='ignore').read()
    except Exception: corpus[f] = ''
out = open(sys.argv[1] if len(sys.argv) > 1 else '/dev/stdout', 'w')
out.write("path\tbytes\tlast_commit\tmentions\ttitle\n")
for f in docs:
    date = subprocess.run(["git","log","-1","--format=%cs","--",f],capture_output=True,text=True).stdout.strip()
    name = os.path.basename(f)
    mentions = sum(1 for g, t in corpus.items() if g != f and name in t)
    title = next((l.strip('# ').strip() for l in corpus[f].splitlines() if l.strip()), '')[:90].replace('\t',' ')
    out.write(f"{f}\t{len(corpus[f].encode())}\t{date}\t{mentions}\t{title}\n")
