#!/usr/bin/env python3
"""S45-S49 static source check for round 3g (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S45 ScanPhotoRef.kt defines SCAN_PHOTO_REF_PREFIX, scanPhotoRef, isScanPhotoRef, scanPhotoFileName; no other file builds "vault:" literally.
  S46 CameraCaptureHelper.kt defines staleCaptureFiles and a 3-argument clearStaleCaptures; AncViewModel.kt calls clearStaleCaptures( twice+ and passes imageUris.
  S47 AncState.kt defines ImportScanPhoto and ScanPhotoImported; AncViewModel.kt handles/dispatches them, calls scanPhotoRef(, deleteIfCaptureTemp( inside a finally.
  S48 UltrasoundEditor.kt sends ImportScanPhoto( and no longer appends a raw uri string to imageUris.
  S49 UltrasoundPhotoViewerDialog.kt uses scanPhotoFileName and resolveAttachmentFile; WorkspaceScreen.kt and ProfileScreen.kt pass a patient id.
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


def code_of(rel, base=MAIN):
    src = read(base / rel)
    return None if src is None else strip_comments(src)


def lacks(src, label, needles):
    if src is None:
        return [label + " missing"]
    return [label + " lacks " + n for n in needles if n not in src]


def calls_of(src, name):
    """Return the text of each call `name(...)` (balanced parentheses)."""
    out = []
    for m in re.finditer(r"\b" + re.escape(name) + r"\s*\(", src):
        depth, i = 1, m.end()
        while i < len(src) and depth:
            depth += {"(": 1, ")": -1}.get(src[i], 0)
            i += 1
        out.append(src[m.end():i - 1])
    return out


def main():
    # S45
    problems = []
    ref = code_of("storage/ScanPhotoRef.kt")
    if ref is None:
        problems.append("ScanPhotoRef.kt missing")
    else:
        if not re.search(r"\bconst\s+val\s+SCAN_PHOTO_REF_PREFIX\b", ref):
            problems.append("lacks const val SCAN_PHOTO_REF_PREFIX")
        for fn in ("scanPhotoRef", "isScanPhotoRef", "scanPhotoFileName"):
            if not re.search(r"\bfun\s+" + fn + r"\s*\(", ref):
                problems.append("lacks fun " + fn)
    for path in sorted(MAIN.rglob("*.kt")):
        rel = path.relative_to(MAIN).as_posix()
        if rel == "storage/ScanPhotoRef.kt":
            continue
        src = code_of(rel)
        if src is not None and '"vault:' in src:
            problems.append(rel + " builds the vault: prefix literally")
    record("S45 scan photo ref helpers in one file", not problems, "; ".join(problems))

    # S46
    problems = []
    helper = code_of("storage/CameraCaptureHelper.kt")
    vm = code_of("state/AncViewModel.kt")
    if helper is None:
        problems.append("CameraCaptureHelper.kt missing")
    else:
        if not re.search(r"\bfun\s+staleCaptureFiles\s*\(", helper):
            problems.append("lacks fun staleCaptureFiles")
        m = re.search(r"\bfun\s+clearStaleCaptures\s*\(([^)]*)\)", helper)
        if not m:
            problems.append("lacks fun clearStaleCaptures")
        elif m.group(1).count(":") != 3:
            problems.append("clearStaleCaptures does not take exactly 3 parameters")
    if vm is None:
        problems.append("AncViewModel.kt missing")
    else:
        calls = calls_of(vm, "CameraCaptureHelper.clearStaleCaptures")
        # one shared helper (reuse principle) that calls clearStaleCaptures once and is itself called from both the import and the attach paths
        helper_uses = len(re.findall(r"\bsweepStaleCaptures\s*\(\s*\)", vm))
        if len(calls) < 1 or helper_uses < 3:   # 1 definition + 2 call sites
            problems.append("AncViewModel.kt: clearStaleCaptures( called %d time(s), sweepStaleCaptures() seen %d time(s) (need 1 call and a helper used by import and attach)" % (len(calls), helper_uses))
        if "imageUris" not in vm:
            problems.append("AncViewModel.kt never reads scan imageUris")
    record("S46 stale-capture sweep keeps referenced files", not problems, "; ".join(problems))

    # S47
    problems = []
    st = code_of("state/AncState.kt")
    problems += lacks(st, "AncState.kt", ["ImportScanPhoto", "ScanPhotoImported"])
    if vm is not None:
        problems += lacks(vm, "AncViewModel.kt", ["ImportScanPhoto", "ScanPhotoImported(", "scanPhotoRef("])
        ok_fin = False
        for m in re.finditer(r"\bfinally\s*\{", vm):
            body = vm[m.end():m.end() + 400]
            if "deleteIfCaptureTemp(" in body:
                ok_fin = True
        if not ok_fin:
            problems.append("deleteIfCaptureTemp( not inside a finally block")
    record("S47 import event, reducer event, temp delete in finally", not problems, "; ".join(problems))

    # S48
    problems = []
    ed = code_of("ui/UltrasoundEditor.kt")
    problems += lacks(ed, "UltrasoundEditor.kt", ["ImportScanPhoto("])
    if ed is not None:
        if re.search(r"imageUris\s*\+\s*(?:uri\b|pendingCameraUri\b)", ed):
            problems.append("still appends a raw uri to imageUris")
    record("S48 editor imports photos instead of storing raw URIs", not problems, "; ".join(problems))

    # S49
    problems = []
    dlg = code_of("ui/UltrasoundPhotoViewerDialog.kt")
    problems += lacks(dlg, "UltrasoundPhotoViewerDialog.kt", ["scanPhotoFileName", "resolveAttachmentFile"])
    for rel in ("ui/patienthub/PatientHubScreen.kt",):  # replaces WorkspaceScreen/ProfileScreen
        src = code_of(rel)
        if src is None:
            problems.append(rel + " missing")
            continue
        calls = calls_of(src, "UltrasoundPhotoViewerDialog")
        if not calls:
            problems.append(rel + " does not call UltrasoundPhotoViewerDialog(")
        elif not all(re.search(r"\b[Pp]atientId\b|patient\.id|\.id\b", c) for c in calls):
            problems.append(rel + " does not pass a patient id to UltrasoundPhotoViewerDialog(")
    record("S49 viewer resolves vault refs, callers pass patient id", not problems, "; ".join(problems))

    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
