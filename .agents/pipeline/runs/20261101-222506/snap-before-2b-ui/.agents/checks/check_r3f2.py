#!/usr/bin/env python3
"""S37-S42 static source check for round 3f-2 (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S37 build.gradle.kts has the four androidx.camera dependencies; manifest has the CAMERA permission.
  S38 AddAttachmentDialog.kt and UltrasoundEditor.kt: no TakePicture / launchCamera, both call InAppCameraDialog(.
  S39 InAppCameraDialog.kt: five testTags, PreviewView, ImageCapture, Manifest.permission.CAMERA, unbindAll in DisposableEffect/onDispose.
  S40 PhotoQuality.kt presets with the Q54 values; vault has no MAX_DIMENSION_PX / WEBP_QUALITY and uses preset.maxSide / preset.webpQuality.
  S41 AddAttachmentDialog.kt preset chips + defaultFor(; AncViewModel.kt calls CameraCaptureHelper.deleteIfCaptureTemp(.
  S42 CameraCaptureHelper.kt defines deleteIfCaptureTemp, createCameraTempFile, clearStaleCaptures; strings.xml defines every Q60 key.
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


def code_of(rel, base=MAIN):
    src = read(base / rel)
    return None if src is None else strip_comments(src)


def lacks(src, name, needles):
    if src is None:
        return [name + " missing"]
    return [name + " lacks " + n for n in needles if n not in src]


def main():
    problems = []
    gradle = code_of("app/build.gradle.kts", ROOT)
    manifest = read(ROOT / "app/src/main/AndroidManifest.xml")
    for art in ("camera-core", "camera-camera2", "camera-lifecycle", "camera-view"):
        problems += lacks(gradle, "build.gradle.kts", ["androidx.camera:" + art])
    if manifest is None or not re.search(r'<uses-permission[^>]*android\.permission\.CAMERA"', manifest):
        problems.append("manifest lacks CAMERA permission")
    record("S37 camera dependencies and CAMERA permission", not problems, "; ".join(sorted(set(problems))))

    problems = []
    for f in ("ui/AddAttachmentDialog.kt", "ui/UltrasoundEditor.kt"):
        src = code_of(f)
        if src is None:
            problems.append(f + " missing")
            continue
        for bad in ("TakePicture", "launchCamera"):
            if bad in src:
                problems.append(f + " still contains " + bad)
        if "InAppCameraDialog(" not in src:
            problems.append(f + " does not call InAppCameraDialog(")
    record("S38 old camera flow replaced by InAppCameraDialog", not problems, "; ".join(problems))

    cam = code_of("ui/InAppCameraDialog.kt")
    problems = lacks(cam, "InAppCameraDialog.kt", [
        '"camera_preview"', '"camera_capture"', '"camera_close"', '"camera_retake"', '"camera_use"',
        "PreviewView", "ImageCapture", "Manifest.permission.CAMERA"])
    if cam is not None:
        ok = False
        for m in re.finditer(r"DisposableEffect|onDispose", cam):
            if "unbindAll" in cam[m.start():]:
                ok = True
                break
        if not ok:
            problems.append("unbindAll not inside DisposableEffect/onDispose")
    record("S39 InAppCameraDialog contract", not problems, "; ".join(problems))

    problems = []
    pq = code_of("storage/PhotoQuality.kt")
    if pq is None:
        problems.append("PhotoQuality.kt missing")
    else:
        for name, side, q in (("SMALL", 1280, 70), ("STANDARD", 1600, 75), ("DETAILED", 1920, 85)):
            if not re.search(r"\b%s\s*\(\s*%d\s*,\s*%d\s*[,)]" % (name, side, q), pq):
                problems.append("preset %s(%d, %d) missing" % (name, side, q))
    vault = code_of("storage/PatientAttachmentVault.kt")
    if vault is None:
        problems.append("PatientAttachmentVault.kt missing")
    else:
        for bad in ("MAX_DIMENSION_PX", "WEBP_QUALITY"):
            if bad in vault:
                problems.append("vault still has " + bad)
        problems += lacks(vault, "vault", ["preset.maxSide", "preset.webpQuality"])
    record("S40 presets and vault use them", not problems, "; ".join(problems))

    problems = lacks(code_of("ui/AddAttachmentDialog.kt"), "AddAttachmentDialog.kt",
                     ["preset_small", "preset_standard", "preset_detailed", "PhotoQualityPreset.defaultFor("])
    problems += lacks(code_of("state/AncViewModel.kt"), "AncViewModel.kt", ["CameraCaptureHelper.deleteIfCaptureTemp("])
    record("S41 preset chips and temp-file cleanup", not problems, "; ".join(problems))

    helper = code_of("storage/CameraCaptureHelper.kt")
    problems = []
    if helper is None:
        problems.append("CameraCaptureHelper.kt missing")
    else:
        for fn in ("deleteIfCaptureTemp", "createCameraTempFile", "clearStaleCaptures"):
            if not re.search(r"\bfun\s+" + fn + r"\s*\(", helper):
                problems.append("CameraCaptureHelper lacks fun " + fn)
    strings = read(RES / "values/strings.xml")
    keys = ["camera_capture", "camera_retake", "camera_use", "camera_close", "camera_permission_needed",
            "photo_quality_label"]
    for k in keys:
        if strings is None or not re.search(r'<string\s+name="%s"' % k, strings):
            problems.append("strings.xml lacks " + k)
    # the spec names no keys for the three preset labels: accept any string key naming the preset
    for word in ("small", "standard", "detailed"):
        pat = r'<string\s+name="[a-z_]*(?:preset|quality)[a-z_]*%s[a-z_]*"|<string\s+name="[a-z_]*%s[a-z_]*(?:preset|quality)[a-z_]*"' % (word, word)
        if strings is None or not re.search(pat, strings):
            problems.append("strings.xml lacks a preset label key for " + word)
    record("S42 helper functions and strings", not problems, "; ".join(problems))

    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
