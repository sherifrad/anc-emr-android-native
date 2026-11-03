#!/usr/bin/env python3
"""S43-S44 static source check for round 3f-3 (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S43 BoundedBitmap.kt defines exifTransform and uses ExifInterface; InAppCameraDialog.kt has no ExifInterface / decodeUpright;
      no other main source file references ExifInterface.
  S44 AddAttachmentDialog.kt has verticalScroll and heightIn( for the preview, and keeps addAttachmentDialog and the save button text.
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


def main():
    problems = []
    bb = code_of("storage/BoundedBitmap.kt")
    if bb is None:
        problems.append("BoundedBitmap.kt missing")
    else:
        if not re.search(r"\bfun\s+exifTransform\s*\(", bb):
            problems.append("BoundedBitmap.kt lacks fun exifTransform")
        if "ExifInterface" not in bb:
            problems.append("BoundedBitmap.kt does not use ExifInterface")
    cam = code_of("ui/InAppCameraDialog.kt")
    if cam is None:
        problems.append("InAppCameraDialog.kt missing")
    else:
        for bad in ("ExifInterface", "decodeUpright"):
            if bad in cam:
                problems.append("InAppCameraDialog.kt still contains " + bad)
    for path in sorted(MAIN.rglob("*.kt")):
        rel = path.relative_to(MAIN).as_posix()
        if rel in ("storage/BoundedBitmap.kt", "ui/InAppCameraDialog.kt"):
            continue
        src = code_of(rel)
        if src is not None and "ExifInterface" in src:
            problems.append(rel + " references ExifInterface")
    record("S43 EXIF handled once in BoundedBitmap", not problems, "; ".join(problems))

    dlg = code_of("ui/AddAttachmentDialog.kt")
    problems = []
    if dlg is None:
        problems.append("AddAttachmentDialog.kt missing")
    else:
        for n in ("verticalScroll", "heightIn(", "addAttachmentDialog", "Save to Patient Record"):
            if n not in dlg:
                problems.append("AddAttachmentDialog.kt lacks " + n)
    record("S44 attachment dialog scrolls, preview height capped", not problems, "; ".join(problems))

    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
