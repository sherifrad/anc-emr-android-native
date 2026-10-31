#!/usr/bin/env python3
"""S99-S100 static source check for round 3y (SPEC_FIX_R3.md, section "Round 3y").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S99  app/src/main/res has no values-ar folder; no Kotlin file under app/src/main or app/src/androidTest
       contains Locale("ar"); check_r3w.py no longer lists values-ar (comments are stripped first).
  S100 BiometricAuthManager.kt guards API 28 and API 30 (Build.VERSION.SDK_INT or @RequiresApi);
       res/values/themes.xml has no windowSplashScreen; res/values-v31/themes.xml has windowSplashScreenBackground
       and windowSplashScreenAnimatedIcon; AppLockScreen.kt has no context.getString(R.string.pin_.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "app/src"
MAIN = SRC / "main/java/com/sherifradwan/ancnative"
RES = SRC / "main/res"
BIOMETRIC = MAIN / "security/BiometricAuthManager.kt"
APP_LOCK = MAIN / "ui/AppLockScreen.kt"
BASE_THEMES = RES / "values/themes.xml"
V31_THEMES = RES / "values-v31/themes.xml"
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


def code_of(path, problems, strip=strip_comments):
    src = read(path)
    if src is None:
        problems.append(path.name + " missing")
        return None
    return strip(src)


def check_s99():
    problems = []
    if (RES / "values-ar").exists():
        problems.append("res/values-ar still exists")
    for root in (SRC / "main", SRC / "androidTest"):
        for path in sorted(root.rglob("*.kt")):
            code = code_of(path, problems)
            if code is not None and re.search(r'Locale\(\s*"ar"', code):
                problems.append(path.name + ' contains Locale("ar")')
    checker = code_of(CHECK_R3W, problems)
    if checker is not None and "values-ar" in checker:
        problems.append("check_r3w.py still lists values-ar")
    record("S99 no values-ar folder, no Locale(\"ar\") in main/androidTest sources, check_r3w.py drops values-ar",
           not problems, "; ".join(problems))


def check_s100():
    problems = []
    bio = code_of(BIOMETRIC, problems)
    if bio is not None:
        for api, code_name in (("28", "P"), ("30", "R")):
            guard = (
                r"SDK_INT\s*[<>=!]+\s*(?:" + api + r"|(?:Build\.)?VERSION_CODES\." + code_name + r")\b"
                r"|@RequiresApi\(\s*(?:api\s*=\s*)?(?:" + api + r"|(?:Build\.)?VERSION_CODES\." + code_name + r")\s*\)"
            )
            if not re.search(guard, bio):
                problems.append("BiometricAuthManager.kt has no API " + api + " guard")
    base = code_of(BASE_THEMES, problems, strip_xml_comments)
    if base is not None and "windowSplashScreen" in base:
        problems.append("values/themes.xml still has windowSplashScreen")
    v31 = code_of(V31_THEMES, problems, strip_xml_comments)
    if v31 is not None:
        for item in ("windowSplashScreenBackground", "windowSplashScreenAnimatedIcon"):
            if not re.search(r'<item\s+name="' + item + r'"', v31):
                problems.append("values-v31/themes.xml lacks " + item)
    lock = code_of(APP_LOCK, problems)
    if lock is not None and re.search(r"context\.getString\(\s*R\.string\.pin_", lock):
        problems.append("AppLockScreen.kt still has context.getString(R.string.pin_")
    record("S100 biometric API 28/30 guards, splash items only in values-v31, no context.getString(R.string.pin_",
           not problems, "; ".join(problems))


def main():
    check_s99()
    check_s100()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
