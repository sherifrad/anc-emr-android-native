#!/usr/bin/env python3
"""S230-S232 static check for round 5d (SPEC_DESIGN_R5.md, section "Round 5d"). Stdlib only; textual contract only."""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return ""


def main():
    # WorkspaceScreen.kt is gone: the patient hub (tabs and components) renders the medication list now.
    hub = sorted((ROOT / "app/src/main/java/com/sherifradwan/ancnative/ui/patienthub").rglob("*.kt"))
    ws = "\n".join(read(f) for f in hub)
    missing = [t for t in ("medicationToggle-", "medicationsToggleAll", "medication-", "editMedication-") if '"' + t not in ws]
    record("S230 patient hub medication tags present", not missing, "missing " + ", ".join(missing))
    strings = read(ROOT / "app/src/main/res/values/strings.xml")
    new = [m for m in re.findall(r'<string name="([a-z_]*(?:expand|collapse|show_details|hide_details)[a-z_]*)"', strings)]
    record("S231 new labels defined once", len(new) == len(set(new)) and len(new) >= 4, "found " + str(new))
    try:
        out = subprocess.run(["git", "diff", "--name-only", "HEAD"], cwd=ROOT, capture_output=True, text=True, check=True).stdout
        bad = [l for l in out.splitlines() if re.search(r"/ancnative/(model|state|storage)/", l) and "/src/test/" not in l]
        record("S232 no logic files changed", not bad, "; ".join(bad))
    except (OSError, subprocess.CalledProcessError) as exc:
        record("S232 no logic files changed", False, str(exc))
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
