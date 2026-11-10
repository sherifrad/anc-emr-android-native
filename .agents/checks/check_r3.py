#!/usr/bin/env python3
"""S1-S4 static source check for round 3a (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per sub-check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S1 AncViewModel.kt has no intakeDatingUpdate or profileSavedNotice, state/IntakeDatingRule.kt
     is gone, and the view model has no "plusDays(280)".
  S2 RecordsRepository.kt seed function has an includeDemoPatients parameter and
     every patient-owned insert sits behind it (inserts after the metadata insert
     appear only inside an `if (includeDemoPatients)` block).
  S3 VaultController.kt passes BuildConfig.DEBUG.
  S4 app/build.gradle.kts has buildConfig = true.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"

results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError as e:
        return None


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


# S1
vm = read(MAIN / "state/AncViewModel.kt")
if vm is None:
    record("S1 AncViewModel.kt readable", False, "missing file")
else:
    code = strip_comments(vm)
    dead = [n for n in ("intakeDatingUpdate", "profileSavedNotice") if n in code]
    if re.search(r"\bSaveDatingCommand\s*\(", code):
        dead.append("SaveDatingCommand(")
    if (MAIN / "state/IntakeDatingRule.kt").exists():
        dead.append("state/IntakeDatingRule.kt exists")
    record("S1a AncViewModel.kt has no intakeDatingUpdate/profileSavedNotice and IntakeDatingRule.kt is gone", not dead, "still present: " + ", ".join(dead))
    record("S1b AncViewModel.kt has no plusDays(280)", "plusDays(280" not in code, "plusDays(280) still present")

# S2
repo = read(MAIN / "storage/RecordsRepository.kt")
if repo is None:
    record("S2 RecordsRepository.kt readable", False, "missing file")
else:
    code = strip_comments(repo)
    m = re.search(r"fun\s+seedInitialSyntheticFixtures\s*\(([^)]*)\)", code)
    record("S2a seed function has includeDemoPatients: Boolean",
           bool(m and re.search(r"includeDemoPatients\s*:\s*Boolean", m.group(1))), "parameter missing")
    start = m.start() if m else -1
    body = ""
    if start >= 0:
        # body = from the function to the next top-level "    suspend fun"/"    fun" declaration
        nxt = re.search(r"\n    (?:private |internal )?(?:suspend )?fun\s", code[m.end():])
        body = code[start: m.end() + nxt.start()] if nxt else code[start:]
    guard = re.search(r"if\s*\(\s*includeDemoPatients\s*\)", body)
    record("S2b seed body has an `if (includeDemoPatients)` guard", bool(guard), "no guard")
    inserts = [x for x in re.finditer(r"dao\.\w*insert\w*\(|\.insert\w*\(", body)]
    if guard and inserts:
        # find end of the guarded block by brace matching
        i = body.index("{", guard.end())
        depth, j = 0, i
        while j < len(body):
            depth += body[j] == "{"
            depth -= body[j] == "}"
            if depth == 0:
                break
            j += 1
        block = (i, j)
        meta = [x for x in inserts if "etadata" in x.group(0)]
        others = [x for x in inserts if x not in meta]
        bad = [x.group(0) for x in others if not (block[0] < x.start() < block[1])]
        record("S2c every non-metadata insert is inside the guard", not bad, "unguarded: " + ", ".join(bad))
    else:
        record("S2c every non-metadata insert is inside the guard", False, "no guard or no inserts found")

# S3
vc = read(MAIN / "storage/VaultController.kt")
if vc is None:
    record("S3 VaultController.kt readable", False, "missing file")
else:
    code = strip_comments(vc)
    record("S3 VaultController.kt passes BuildConfig.DEBUG",
           re.search(r"seedInitialSyntheticFixtures\s*\([^)]*BuildConfig\.DEBUG", code) is not None,
           "BuildConfig.DEBUG not passed to seed call")

# S4
gradle = read(ROOT / "app/build.gradle.kts")
if gradle is None:
    record("S4 build.gradle.kts readable", False, "missing file")
else:
    record("S4 build.gradle.kts has buildConfig = true",
           re.search(r"buildConfig\s*=\s*true", strip_comments(gradle)) is not None, "missing")

sys.exit(0 if results and all(results) else 1)
