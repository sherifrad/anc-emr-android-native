#!/usr/bin/env python3
"""S17-S22 static source check for round 3d (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per sub-check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S17 file_paths.xml: cache-path with path backups/ (or backups), still camera/ and files-path.
  S18 MainActivity.kt: no FLAG_SECURE (owner decision D15 replaced the release-only block); no VAULT_DEBUG.
  S19 SecuritySettingsRepository.kt: loadSettings needsPinSetup ignores key_pin_change_required; lockApp assigns needsPinSetup.
  S20 PatientTwoPageReportDialog.kt: no javaScriptEnabled = true.
  S21 SecuritySettingsDialog.kt: no AutoLockTimeout.entries / setAutoLockTimeout(; restore_confirm_dialog inside an AlertDialog.
  S22 false claims removed (WorkspaceScreen, PatientEditor, AttachmentSyncManager, SecuritySettingsDialog).
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
RES = ROOT / "app/src/main/res"

results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return None


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"(?<!:)//[^\n]*", "", src)


def block_after(src, idx):
    start = src.find("{", idx)
    if start < 0:
        return ""
    depth = 0
    for i in range(start, len(src)):
        if src[i] == "{":
            depth += 1
        elif src[i] == "}":
            depth -= 1
            if depth == 0:
                return src[start:i + 1]
    return src[start:]


def paren_after(src, idx):
    start = src.find("(", idx)
    if start < 0:
        return ""
    depth = 0
    for i in range(start, len(src)):
        if src[i] == "(":
            depth += 1
        elif src[i] == ")":
            depth -= 1
            if depth == 0:
                return src[start:i + 1]
    return src[start:]


KEY_RE = r"KEY_PIN_CHANGE_REQUIRED|key_pin_change_required"

# S17
xml = read(RES / "xml/file_paths.xml")
if xml is None:
    record("S17 file_paths.xml cache-path backups", False, "file missing")
else:
    code = re.sub(r"<!--.*?-->", "", xml, flags=re.S)
    problems = []
    if not re.search(r"<cache-path\b[^>]*\bpath\s*=\s*\"backups/?\"", code):
        problems.append("no cache-path with path backups/")
    if not re.search(r"\bpath\s*=\s*\"camera/?\"", code):
        problems.append("camera/ entry lost")
    if "<files-path" not in code:
        problems.append("files-path entry lost")
    record("S17 file_paths.xml cache-path backups", not problems, "; ".join(problems))

# S18
act = read(MAIN / "MainActivity.kt")
if act is None:
    record("S18 no FLAG_SECURE (D15), no VAULT_DEBUG", False, "file missing")
else:
    code = strip_comments(act)
    problems = []
    # D15 (owner, 2026-10-31): the screen-capture block is removed in release builds too.
    if "FLAG_SECURE" in code:
        problems.append("FLAG_SECURE present (removed by owner decision D15)")
    if "VAULT_DEBUG" in code:
        problems.append("VAULT_DEBUG still present")
    record("S18 no FLAG_SECURE (D15), no VAULT_DEBUG", not problems, "; ".join(dict.fromkeys(problems)))

# S19
repo = read(MAIN / "security/SecuritySettingsRepository.kt")
if repo is None:
    record("S19 weak-PIN setup needs proof of old PIN", False, "file missing")
else:
    code = strip_comments(repo)
    problems = []
    lm = re.search(r"\bfun\s+loadSettings\b", code)
    if not lm:
        problems.append("loadSettings missing")
    else:
        body = block_after(code, lm.start())
        decls = re.findall(r"needsPinSetup\s*=[^\n]*", body)
        if not decls:
            problems.append("loadSettings sets no needsPinSetup")
        if any(re.search(KEY_RE, d) for d in decls):
            problems.append("needsPinSetup still reads key_pin_change_required")
    lk = re.search(r"\bfun\s+lockApp\s*\(", code)
    if not lk:
        problems.append("lockApp missing")
    elif not re.search(r"needsPinSetup\s*=", block_after(code, lk.start())):
        problems.append("lockApp does not assign needsPinSetup")
    record("S19 weak-PIN setup needs proof of old PIN", not problems, "; ".join(problems))

# S20
rep = read(MAIN / "ui/report/PatientTwoPageReportDialog.kt")
if rep is None:
    record("S20 report WebView has no JavaScript", False, "file missing")
else:
    code = strip_comments(rep)
    has_true = re.search(r"javaScriptEnabled\s*=\s*true", code)
    has_false = re.search(r"javaScriptEnabled\s*=\s*false", code)
    record("S20 report WebView has no JavaScript", not has_true and bool(has_false),
           "javaScriptEnabled = true present" if has_true else "javaScriptEnabled = false not set")

# S21
sec = read(MAIN / "ui/SecuritySettingsDialog.kt")
if sec is None:
    record("S21 settings dialog: no timeout picker, restore confirm", False, "file missing")
else:
    code = strip_comments(sec)
    problems = []
    if "AutoLockTimeout.entries" in code:
        problems.append("AutoLockTimeout.entries still used")
    if "setAutoLockTimeout(" in code:
        problems.append("setAutoLockTimeout( still called")
    inside = any("restore_confirm_dialog" in paren_after(code, m.start())
                 for m in re.finditer(r"(?<![\w.])AlertDialog\s*\(", code))
    if not inside:
        problems.append("restore_confirm_dialog testTag not inside an AlertDialog")
    record("S21 settings dialog: no timeout picker, restore confirm", not problems, "; ".join(problems))

# S22
claims = [
    ("ui/WorkspaceScreen.kt", ["Encrypted & Verified"]),
    ("ui/PatientEditor.kt", ["Hardware Vault Security"]),
    ("storage/AttachmentSyncManager.kt", ["Locally Encrypted & Secured"]),
    ("ui/SecuritySettingsDialog.kt", ["Zero Data Loss", "100% verif"]),
]
problems = []
for rel, bad in claims:
    src = read(MAIN / rel)
    if src is None:
        problems.append(rel + " missing")
        continue
    code = strip_comments(src)
    for b in bad:
        if b in code:
            problems.append(rel + " still has '" + b + "'")
record("S22 false security claims removed", not problems, "; ".join(problems))

sys.exit(0 if all(results) else 1)
