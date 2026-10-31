#!/usr/bin/env python3
"""S99-S100 static source check for round 3y (SPEC_FIX_R3.md, section "Round 3y").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S99  app/src/main/res has no values-ar folder; no file under app/src/main or app/src/androidTest uses
       Locale("ar") as a UI-string locale (Locale("ar") together with getIdentifier / resource lookup);
       check_r3w.py no longer lists values-ar.
  S100 BiometricAuthManager.kt guards with Build.VERSION.SDK_INT (or @RequiresApi) for API 28 and 30;
       res/values/themes.xml has no windowSplashScreen; res/values-v31/themes.xml has both items;
       AppLockScreen.kt has no `getString(R.string.pin_` read through the context.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "app/src"
RES = SRC / "main/res"
BIOMETRIC = SRC / "main/java/com/sherifradwan/ancnative/security/BiometricAuthManager.kt"
APP_LOCK = SRC / "main/java/com/sherifradwan/ancnative/ui/AppLockScreen.kt"
CHECK_R3W = ROOT / ".agents/checks/check_r3w.py"

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


def strip_xml_comments(src):
    return re.sub(r"<!--.*?-->", "", src, flags=re.S)


def code_of(path, problems):
    src = read(path)
    if src is None:
        problems.append(path.name + " missing")
        return None
    return strip_comments(src)


def check_s99():
    problems = []
    if (RES / "values-ar").exists():
        problems.append("res/values-ar still exists")
    for root in (SRC / "main", SRC / "androidTest"):
        for path in sorted(root.rglob("*.kt")):
            code = strip_comments(read(path) or "")
            if re.search(r'Locale\(\s*"ar"', code) and re.search(r"getIdentifier\s*\(", code):
                problems.append(path.name + " looks up resources with Locale(\"ar\")")
            if path.is_relative_to(SRC / "main") and re.search(r'Locale\(\s*"ar"', code):
                problems.append(path.name + " uses Locale(\"ar\") in main")
    r3w = read(CHECK_R3W)
    if r3w is None:
        problems.append("check_r3w.py missing")
    elif "values-ar" in r3w:
        problems.append("check_r3w.py still mentions values-ar")
    record("S99 no values-ar folder, no Arabic UI-string locale test, check_r3w without values-ar",
           not problems, "; ".join(problems))


def check_s100():
    problems = []
    bio = code_of(BIOMETRIC, problems)
    if bio is not None:
        for api in ("28", "30"):
            guarded = re.search(r"SDK_INT\s*[<>]=?\s*" + api + r"\b", bio)
            requires = re.search(r"@RequiresApi\(\s*(?:api\s*=\s*)?" + api + r"\s*\)", bio)
            if not (guarded or requires):
                problems.append("BiometricAuthManager.kt has no API " + api + " guard")
    themes = read(RES / "values/themes.xml")
    if themes is None:
        problems.append("values/themes.xml missing")
    elif "windowSplashScreen" in strip_xml_comments(themes):
        problems.append("values/themes.xml still has windowSplashScreen")
    v31 = read(RES / "values-v31/themes.xml")
    if v31 is None:
        problems.append("values-v31/themes.xml missing")
    else:
        xml = strip_xml_comments(v31)
        for item in ("windowSplashScreenBackground", "windowSplashScreenAnimatedIcon"):
            if item not in xml:
                problems.append("values-v31/themes.xml lacks " + item)
    lock = code_of(APP_LOCK, problems)
    if lock is not None and re.search(r"getString\(\s*R\.string\.pin_", lock):
        problems.append("AppLockScreen.kt still reads pin_ strings through getString(")
    record("S100 API guards in BiometricAuthManager, splash items only in values-v31, no context.getString(pin_",
           not problems, "; ".join(problems))


def main():
    check_s99()
    check_s100()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
