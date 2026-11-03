#!/usr/bin/env python3
"""S50 static source check for round 3h (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S50 CameraCaptureHelper.kt defines top-level captureTempFile with no android.* type in its signature;
      deleteIfCaptureTemp calls captureTempFile( and contains no "canonicalFile".
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


def balanced(src, start, open_ch, close_ch):
    """Text between the open_ch at src[start] and its matching close_ch."""
    depth, i = 0, start
    while i < len(src):
        if src[i] == open_ch:
            depth += 1
        elif src[i] == close_ch:
            depth -= 1
            if depth == 0:
                return src[start + 1:i]
        i += 1
    return None


def main():
    problems = []
    src = read(MAIN / "storage/CameraCaptureHelper.kt")
    if src is None:
        problems.append("CameraCaptureHelper.kt missing")
    else:
        src = strip_comments(src)
        m = re.search(r"^fun\s+captureTempFile\s*\(", src, flags=re.M)
        if not m:
            problems.append("lacks top-level fun captureTempFile(")
        else:
            params = balanced(src, m.end() - 1, "(", ")") or ""
            tail = src[m.end() + len(params):].split("\n", 1)[0]
            signature = params + tail
            for t in (r"\bUri\b", r"\bContext\b", r"\bandroid\."):
                if re.search(t, signature):
                    problems.append("captureTempFile signature uses an android type (" + t + ")")
        d = re.search(r"\bfun\s+deleteIfCaptureTemp\s*\(", src)
        if not d:
            problems.append("lacks fun deleteIfCaptureTemp")
        else:
            brace = src.find("{", d.end())
            eq = src.find("=", d.end())
            if brace != -1 and (eq == -1 or brace < eq):
                body = balanced(src, brace, "{", "}") or ""
            else:
                body = src[d.end():].split("\n\n", 1)[0]
            if "captureTempFile(" not in body:
                problems.append("deleteIfCaptureTemp does not call captureTempFile(")
            if "canonicalFile" in body:
                problems.append("deleteIfCaptureTemp still contains canonicalFile")
    record("S50 captureTempFile is the pure rule, deleteIfCaptureTemp is a thin wrapper", not problems, "; ".join(problems))

    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
