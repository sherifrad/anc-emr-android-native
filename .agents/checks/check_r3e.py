#!/usr/bin/env python3
"""S23-S28 static source check for round 3e (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per sub-check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S23 VaultAutoAction.kt: appUnlocked Boolean parameter without default; Locked branch uses it.
  S24 AncViewModel.kt defines lockVault; MainActivity.kt passes two args to vaultAutoAction
      and calls lockVault() inside an effect keyed on isAppLocked.
  S25 AppLockScreen.kt: biometric key clickable enabled expression contains isBiometricEnabled.
  S26 photo viewer dialogs: no invented scan values / loading wording; both show the shared
      R.string.image_unavailable fallback, which strings.xml defines as "Image unavailable".
  S27 AppLockScreen.kt: liveRegion, Role.Button, contentDescription mentioning "digits".
  S28 no IconButton with size(36.dp) in PatientListScreen, ProfileScreen, AddAttachmentDialog.
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
    except OSError:
        return None


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"(?<!:)//[^\n]*", "", src)


def code_of(rel):
    src = read(MAIN / rel)
    return None if src is None else strip_comments(src)


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


# S23
code = code_of("storage/VaultAutoAction.kt")
if code is None:
    record("S23 vaultAutoAction takes appUnlocked", False, "file missing")
else:
    problems = []
    fn = re.search(r"fun\s+vaultAutoAction\s*\(", code)
    params = paren_after(code, fn.start()) if fn else ""
    if not fn:
        problems.append("vaultAutoAction missing")
    elif not re.search(r"\bappUnlocked\s*:\s*Boolean\b", params):
        problems.append("no appUnlocked: Boolean parameter")
    elif re.search(r"\bappUnlocked\s*:\s*Boolean\s*=", params):
        problems.append("appUnlocked has a default value")
    locked = re.search(r"VaultState\.Locked\s*->(.*?)(?=VaultState\.Uninitialized|else\s*->|\Z)", code, flags=re.S)
    if not locked or "appUnlocked" not in locked.group(1):
        problems.append("Locked branch does not depend on appUnlocked")
    record("S23 vaultAutoAction takes appUnlocked", not problems, "; ".join(problems))

# S24
vm = code_of("state/AncViewModel.kt")
act = code_of("MainActivity.kt")
problems = []
if vm is None or not re.search(r"\bfun\s+lockVault\s*\(", vm):
    problems.append("AncViewModel.lockVault missing")
if act is None:
    problems.append("MainActivity.kt missing")
else:
    calls = [paren_after(act, m.start()) for m in re.finditer(r"\bvaultAutoAction\s*\(", act)]
    if not calls or not all("," in c for c in calls):
        problems.append("vaultAutoAction( not called with two arguments")
    ok = False
    for m in re.finditer(r"LaunchedEffect\s*\(", act):
        keys = paren_after(act, m.start())
        body = block_after(act, m.start() + len(m.group(0)) - 1 + len(keys))
        if "isAppLocked" in keys and re.search(r"\blockVault\s*\(\s*\)", body):
            ok = True
    if not ok:
        problems.append("no LaunchedEffect keyed on isAppLocked calling lockVault()")
record("S24 lock closes the vault; auto-unlock gated", not problems, "; ".join(problems))

# S25
lock = code_of("ui/AppLockScreen.kt")
if lock is None:
    record("S25 biometric key honours isBiometricEnabled", False, "file missing")
else:
    ok = False
    for m in re.finditer(r"\.clickable\s*\(", lock):
        args = paren_after(lock, m.start())
        tail = lock[m.start() + len(m.group(0)) - 1 + len(args):][:80]
        if "triggerBiometric" in tail and "isBiometricEnabled" in args:
            ok = True
    record("S25 biometric key honours isBiometricEnabled", ok,
           "biometric clickable enabled expression lacks isBiometricEnabled")

# S26
problems = []
for rel in ["ui/UltrasoundPhotoViewerDialog.kt", "ui/ClinicalAttachmentViewerDialog.kt"]:
    c = code_of(rel)
    if c is None:
        problems.append(rel + " missing")
        continue
    for bad in ["3.5MHz", "GAIN", "CEPHALIC", "Loading encrypted image"]:
        if bad in c:
            problems.append(rel + " still has '" + bad + "'")
    if "R.string.image_unavailable" not in c:
        problems.append(rel + " does not use R.string.image_unavailable")
strings = read(ROOT / "app/src/main/res/values/strings.xml") or ""
if not re.search(r'<string name="image_unavailable">Image unavailable</string>', strings):
    problems.append("strings.xml does not define image_unavailable as 'Image unavailable'")
record("S26 no fake scan screen; shared Image unavailable string", not problems, "; ".join(problems))

# S27
if lock is None:
    record("S27 TalkBack semantics on lock screen", False, "file missing")
else:
    problems = []
    if "liveRegion" not in lock:
        problems.append("no liveRegion")
    if "Role.Button" not in lock:
        problems.append("no Role.Button")
    if not re.search(r"contentDescription\s*=[^\n]*digits", lock):
        problems.append("no contentDescription mentioning digits")
    record("S27 TalkBack semantics on lock screen", not problems, "; ".join(problems))

# S28
problems = []
for rel in ["ui/PatientListScreen.kt", "ui/ProfileScreen.kt", "ui/AddAttachmentDialog.kt"]:
    c = code_of(rel)
    if c is None:
        problems.append(rel + " missing")
        continue
    for m in re.finditer(r"(?<![\w.])IconButton\s*\(", c):
        if re.search(r"\.size\(\s*36\.dp\s*\)", paren_after(c, m.start())):
            problems.append(rel + " IconButton with size(36.dp)")
record("S28 no 36dp IconButton", not problems, "; ".join(problems))

sys.exit(0 if all(results) else 1)
