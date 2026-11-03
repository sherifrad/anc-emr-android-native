#!/usr/bin/env python3
"""S5-S7 static source check for round 3b (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per sub-check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S5 MainActivity.kt: the LaunchedEffect(state.vaultState) block calls vaultAutoAction
     and contains no retryStorageInspection() call.
  S6 VaultController.kt: at least one deleteOrphanAliases() call, and each one is
     guarded by hasVaultDatabase within the preceding lines.
  S7 VaultFiles.kt: cleanupUnpublishedArtifacts body calls hasVaultDatabase.
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


def block_after(src, start_idx):
    """Text of the brace block whose first '{' follows start_idx."""
    open_idx = src.find("{", start_idx)
    if open_idx < 0:
        return None
    depth = 0
    for i in range(open_idx, len(src)):
        if src[i] == "{":
            depth += 1
        elif src[i] == "}":
            depth -= 1
            if depth == 0:
                return src[open_idx:i + 1]
    return None


# S5
raw = read(MAIN / "MainActivity.kt")
if raw is None:
    record("S5 MainActivity.kt readable", False, "missing")
else:
    src = strip_comments(raw)
    m = re.search(r"LaunchedEffect\(\s*state\.vaultState\s*(?:,[^)]*)?\)", src)
    body = block_after(src, m.end()) if m else None
    record("S5a LaunchedEffect(state.vaultState[, ...]) found", body is not None)
    if body is not None:
        record("S5b effect calls vaultAutoAction", "vaultAutoAction(" in body)
        record("S5c effect has no retryStorageInspection()", "retryStorageInspection()" not in body)

# S6
raw = read(MAIN / "storage/VaultController.kt")
if raw is None:
    record("S6 VaultController.kt readable", False, "missing")
else:
    src = strip_comments(raw)
    calls = [m.start() for m in re.finditer(r"deleteOrphanAliases\(\)", src)]
    record("S6a deleteOrphanAliases() call sites present", len(calls) > 0)
    for n, pos in enumerate(calls, 1):
        window = src[max(0, pos - 400):pos]
        record("S6b call %d guarded by hasVaultDatabase" % n, "hasVaultDatabase(" in window)

# S7
raw = read(MAIN / "storage/VaultFiles.kt")
if raw is None:
    record("S7 VaultFiles.kt readable", False, "missing")
else:
    src = strip_comments(raw)
    m = re.search(r"fun\s+cleanupUnpublishedArtifacts\s*\(\s*\)", src)
    body = block_after(src, m.end()) if m else None
    record("S7 cleanupUnpublishedArtifacts calls hasVaultDatabase",
           body is not None and "hasVaultDatabase(" in body)

sys.exit(0 if all(results) else 1)
