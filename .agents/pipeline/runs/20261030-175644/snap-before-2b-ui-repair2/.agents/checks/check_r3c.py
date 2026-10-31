#!/usr/bin/env python3
"""S8-S16 static source check for round 3c (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per sub-check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S8  BiometricAuthManager.kt: SDK_INT < 30 appears before the first BiometricManager use in code.
  S9  SecuritySettingsRepository.kt: no "123456" and no DEFAULT_PIN.
  S10 SecuritySettingsRepository.kt: calls hashPin( and pinMatches( and the three v2 keys.
  S11 MainActivity.kt: calls shouldLockOnReturn.
  S12 AutoLockTimeout enum has no NEVER.
  S13 security/SecuritySettingsRepository.kt only: SHA-256 MessageDigest only inside a function whose name contains "legacy".
  S14 SecuritySettingsRepository.kt: key_pin_change_required defined, written, read in loadSettings; validatePin calls isValidNewPin.
  S15 AppLockScreen.kt: observes securityRepo.settings (collectAsState*) and never reads securityRepo.settings.value.
  S16 AncApp.kt: SecuritySettingsDialog guarded by isAppLocked and needsPinSetup; an effect keyed on lock state resets
      showSettingsDialog to false; every AlertDialog( is under a condition containing !isAppLocked.
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
    return re.sub(r"//[^\n]*", "", src)


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


bio = read(MAIN / "security/BiometricAuthManager.kt")
repo = read(MAIN / "security/SecuritySettingsRepository.kt")
act = read(MAIN / "MainActivity.kt")

# S8
if bio is None:
    record("S8 SDK_INT<30 guard before BiometricManager use", False, "file missing")
else:
    code = re.sub(r"^\s*import .*$", "", strip_comments(bio), flags=re.M)
    g = re.search(r"SDK_INT\s*<\s*30\b|SDK_INT\s*<\s*Build\.VERSION_CODES\.R\b", code)
    u = re.search(r"BiometricManager", code)
    record("S8 SDK_INT<30 guard before BiometricManager use",
           bool(g) and (not u or g.start() < u.start()), "no guard before first use")

# S9
if repo is None:
    record("S9 no 123456 / DEFAULT_PIN", False, "file missing")
else:
    code = strip_comments(repo)
    record("S9 no 123456 / DEFAULT_PIN", "123456" not in code and "DEFAULT_PIN" not in code,
           "default PIN remnant found")

# S10
if repo is None:
    record("S10 repository uses PinPolicy for v2", False, "file missing")
else:
    code = strip_comments(repo)
    ok = (re.search(r"(?<![\w.])hashPin\(\s*\w+\s*,\s*\w+", code) is not None
          and "pinMatches(" in code and "key_pin_hash_v2" in code
          and "key_pin_salt_v2" in code and "key_pin_iterations_v2" in code)
    record("S10 repository uses PinPolicy for v2", ok, "missing hashPin/pinMatches/v2 keys")

# S11
record("S11 MainActivity calls shouldLockOnReturn",
       act is not None and "shouldLockOnReturn(" in strip_comments(act), "no call")

# S12
if repo is None:
    record("S12 AutoLockTimeout has no NEVER", False, "file missing")
else:
    code = strip_comments(repo)
    m = re.search(r"enum\s+class\s+AutoLockTimeout", code)
    body = block_after(code, m.start()) if m else ""
    record("S12 AutoLockTimeout has no NEVER", bool(m) and "NEVER" not in body,
           "enum missing" if not m else "NEVER still in enum")

# S13
bad = []
for p in [MAIN / "security/SecuritySettingsRepository.kt"]:
    src = strip_comments(read(p) or "")
    for m in re.finditer(r'MessageDigest\.getInstance\(\s*"SHA-256"\s*\)', src):
        funs = list(re.finditer(r"\bfun\s+(?:[\w.<>]+\.)?(\w+)", src[:m.start()]))
        name = funs[-1].group(1) if funs else ""
        if "legacy" not in name.lower():
            bad.append(p.name + ":" + (name or "<top>"))
record("S13 SHA-256 only in legacy check function", not bad, ", ".join(bad))

# S14
if repo is None:
    record("S14 persisted pin-change-required flag", False, "file missing")
else:
    code = strip_comments(repo)
    lm = re.search(r"\bfun\s+loadSettings\b", code)
    load_body = block_after(code, lm.start()) if lm else ""
    vm = re.search(r"\bfun\s+validatePin\b", code)
    val_body = block_after(code, vm.start()) if vm else ""
    problems = []
    if "key_pin_change_required" not in code:
        problems.append("key_pin_change_required not defined")
    if not re.search(r"\.put(?:Boolean|String|Int)\(\s*(?:\w*\.)?\w*(?:PIN_CHANGE|CHANGE_REQUIRED)\w*\s*,\s*true", code, flags=re.I) \
            and not re.search(r"\.putBoolean\(\s*\"key_pin_change_required\"\s*,\s*true", code):
        problems.append("no write of the flag as true")
    if not re.search(r"key_pin_change_required|PIN_CHANGE|CHANGE_REQUIRED", load_body, flags=re.I):
        problems.append("loadSettings does not read the flag")
    if "isValidNewPin(" not in val_body:
        problems.append("validatePin does not call isValidNewPin")
    record("S14 persisted pin-change-required flag", not problems, "; ".join(problems))

# S15
lock = read(MAIN / "ui/AppLockScreen.kt")
if lock is None:
    record("S15 lock screen observes settings flow", False, "file missing")
else:
    code = strip_comments(lock)
    obs = re.search(r"securityRepo\.settings\s*\.collectAsState(?:WithLifecycle)?\s*\(", code)
    direct = re.search(r"securityRepo\.settings\.value", code)
    record("S15 lock screen observes settings flow", bool(obs) and not direct,
           "settings.value read directly" if direct else "no collectAsState on securityRepo.settings")

# S16
app = read(MAIN / "ui/AncApp.kt")
if app is None:
    record("S16 dialogs hidden while locked / PIN setup", False, "file missing")
else:
    code = strip_comments(app)

    def guard_before(idx):
        ifs = list(re.finditer(r"\bif\s*\(", code[:idx]))
        return code[ifs[-1].start():idx] if ifs else ""

    problems = []
    sd = re.search(r"(?<![\w.])SecuritySettingsDialog\s*\(", code)
    if not sd:
        problems.append("SecuritySettingsDialog call missing")
    else:
        cond = guard_before(sd.start())
        if "isAppLocked" not in cond or "needsPinSetup" not in cond:
            problems.append("SecuritySettingsDialog not guarded by isAppLocked and needsPinSetup")
    effect_ok = False
    for m in re.finditer(r"\b(?:LaunchedEffect|SideEffect|DisposableEffect)\s*\(([^)]*)\)", code):
        body = block_after(code, m.end())
        if re.search(r"isAppLocked|needsPinSetup", m.group(1)) \
                and re.search(r"showSettingsDialog\s*=\s*false", body):
            effect_ok = True
    if not effect_ok:
        problems.append("no effect keyed on lock state resets showSettingsDialog")
    for m in re.finditer(r"(?<![\w.])AlertDialog\s*\(", code):
        if "!isAppLocked" not in guard_before(m.start()):
            line = code[:m.start()].count("\n") + 1
            problems.append("AlertDialog at stripped line %d lacks !isAppLocked guard" % line)
    record("S16 dialogs hidden while locked / PIN setup", not problems, "; ".join(problems))

sys.exit(0 if all(results) else 1)
