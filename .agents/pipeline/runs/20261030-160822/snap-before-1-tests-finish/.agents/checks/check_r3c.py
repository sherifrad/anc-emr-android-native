#!/usr/bin/env python3
"""S8-S13 static source check for round 3c (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per sub-check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S8  BiometricAuthManager.kt: SDK_INT < 30 appears before the first BiometricManager use in code.
  S9  SecuritySettingsRepository.kt: no "123456" and no DEFAULT_PIN.
  S10 SecuritySettingsRepository.kt: calls hashPin( and pinMatches( and the three v2 keys.
  S11 MainActivity.kt: calls shouldLockOnReturn.
  S12 AutoLockTimeout enum has no NEVER.
  S13 SHA-256 MessageDigest only inside a function whose name contains "legacy".
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
for p in sorted(MAIN.rglob("*.kt")):
    src = strip_comments(read(p) or "")
    for m in re.finditer(r'MessageDigest\.getInstance\(\s*"SHA-256"\s*\)', src):
        funs = list(re.finditer(r"\bfun\s+(?:[\w.<>]+\.)?(\w+)", src[:m.start()]))
        name = funs[-1].group(1) if funs else ""
        if "legacy" not in name.lower():
            bad.append(p.name + ":" + (name or "<top>"))
record("S13 SHA-256 only in legacy check function", not bad, ", ".join(bad))

sys.exit(0 if all(results) else 1)
