#!/usr/bin/env python3
"""Usage:
  project_state.py validate
  project_state.py set <task-id> <status> [--detail TEXT] [--ref TEXT]
  project_state.py add <task-id> <phase> <status> <title> [--detail TEXT] [--ref TEXT] [--owner TEXT]
  project_state.py log <text>
  project_state.py render
  project_state.py artifact-doc <out.json>
Purpose: the single source of truth for project progress is docs/PROJECT_STATE.json. This tool edits and
checks it, renders the human view docs/ROADMAP.md, and prepares the writes that bring the published
roadmap artifact in line: artifact-doc writes the whole board as one JSON document, which Claude then stores in
the artifact database (collection state, document main) with the ArtifactData tool. Nothing here touches git or the network."""
import argparse, datetime, json, os, subprocess, sys

ROOT = subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip()
STATE = os.path.join(ROOT, "docs", "PROJECT_STATE.json")
STATUSES = ["done", "active", "next", "todo", "blocked"]
TASK_KEYS = {"phase", "order", "status", "title", "ref", "detail", "owner", "updated"}


def load():
    return json.load(open(STATE, encoding="utf-8"))


def save(state):
    state["updated"] = datetime.date.today().isoformat()
    with open(STATE, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=1, ensure_ascii=False)
        f.write("\n")


def validate(state):
    errs = []
    phases = set(state["phases"])
    for tid, t in state["tasks"].items():
        if t.get("status") not in STATUSES:
            errs.append(f"{tid}: bad status {t.get('status')!r}")
        if t.get("phase") not in phases:
            errs.append(f"{tid}: unknown phase {t.get('phase')!r}")
        if not t.get("title"):
            errs.append(f"{tid}: empty title")
        extra = set(t) - TASK_KEYS
        if extra:
            errs.append(f"{tid}: unknown keys {sorted(extra)}")
    return errs


def cmd_set(a):
    s = load()
    t = s["tasks"][a.id]
    t["status"] = a.status
    if a.detail is not None:
        t["detail"] = a.detail
    if a.ref is not None:
        t["ref"] = a.ref
    t["updated"] = datetime.date.today().isoformat()
    save(s)


def cmd_add(a):
    s = load()
    if a.id in s["tasks"]:
        sys.exit(f"task {a.id} exists")
    order = 1 + max([t.get("order", 0) for t in s["tasks"].values() if t["phase"] == a.phase] or [0])
    s["tasks"][a.id] = {"phase": a.phase, "order": order, "status": a.status, "title": a.title,
                        "ref": a.ref or "", "detail": a.detail or "", "owner": a.owner or "",
                        "updated": datetime.date.today().isoformat()}
    save(s)


def cmd_log(a):
    s = load()
    s["log"].insert(0, {"date": datetime.date.today().isoformat(), "text": a.text})
    save(s)


def cmd_render(_):
    s = load()
    tasks = s["tasks"]
    done = sum(1 for t in tasks.values() if t["status"] == "done")
    lines = ["# Roadmap (generated from docs/PROJECT_STATE.json; do not edit by hand)", "",
             f"Updated {s['updated']}. {done} of {len(tasks)} tasks done.", ""]
    now = [t["title"] for t in tasks.values() if t["status"] == "active"]
    if now:
        lines += ["Working on now: " + "; ".join(now), ""]
    for pid, ph in sorted(s["phases"].items(), key=lambda kv: kv[1]["order"]):
        rows = sorted((tid, t) for tid, t in tasks.items() if t["phase"] == pid)
        rows.sort(key=lambda r: r[1].get("order", 0))
        if not rows:
            continue
        d = sum(1 for _, t in rows if t["status"] == "done")
        lines += [f"## {ph['title']} ({d}/{len(rows)})", "", ph.get("note", ""), "",
                  "| Id | Status | Task | Ref |", "|---|---|---|---|"]
        lines += [f"| {tid} | {t['status']} | {t['title']} | {t.get('ref', '')} |" for tid, t in rows]
        lines.append("")
    lines += ["## Recent log", ""] + [f"- {e['date']}: {e['text']}" for e in s["log"][:25]] + [""]
    open(os.path.join(ROOT, "docs", "ROADMAP.md"), "w", encoding="utf-8").write("\n".join(lines))


def cmd_artifact_doc(a):
    s = load()
    doc = {"updated": s["updated"],
           "phases": [{"id": pid, "order": ph["order"], "title": ph["title"], "note": ph.get("note", "")}
                      for pid, ph in s["phases"].items()],
           "tasks": [{"id": tid, **{k: t.get(k, "") for k in ("phase", "order", "status", "title", "ref", "detail", "owner", "updated")}}
                     for tid, t in s["tasks"].items()],
           "log": s["log"][:25]}
    json.dump(doc, open(a.out, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
    print(f"{a.out}: {len(doc['tasks'])} tasks, {len(doc['phases'])} phases")


def main():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("validate"); sub.add_parser("render")
    x = sub.add_parser("set"); x.add_argument("id"); x.add_argument("status", choices=STATUSES)
    x.add_argument("--detail"); x.add_argument("--ref")
    x = sub.add_parser("add"); [x.add_argument(n) for n in ("id", "phase")]
    x.add_argument("status", choices=STATUSES); x.add_argument("title")
    [x.add_argument("--" + n) for n in ("detail", "ref", "owner")]
    x = sub.add_parser("log"); x.add_argument("text")
    x = sub.add_parser("artifact-doc"); x.add_argument("out")
    a = p.parse_args()
    if a.cmd == "validate":
        errs = validate(load())
        print("\n".join(errs) or "state ok")
        sys.exit(1 if errs else 0)
    {"set": cmd_set, "add": cmd_add, "log": cmd_log, "render": cmd_render, "artifact-doc": cmd_artifact_doc}[a.cmd](a)


if __name__ == "__main__":
    main()
