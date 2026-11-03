#!/usr/bin/env python3
"""S99-S100 static source check for round 3y (SPEC_FIX_R3.md, section "Round 3y").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime. Kotlin and XML comments are stripped first.
  S99 app/src/main/res has no values-ar folder; no file under app/src/main has Locale("ar"); the androidTest
      sources have no Arabic string-resource lookup (the removed everyNewEnglishStorageStringHasAnArabicValue test,
      createConfigurationContext + getIdentifier) and Locale("ar") at most once (the RTL layout fixture arabicRtl,
      which lays out English strings right-to-left and is not a UI-string locale test); check_r3w.py no longer
      lists values-ar.
  S100 BiometricAuthManager.kt guards on Build.VERSION.SDK_INT (or @RequiresApi) for both API 28 and API 30;
      res/values/themes.xml has no windowSplashScreen; res/values-v31/themes.xml has both
      windowSplashScreenBackground and windowSplashScreenAnimatedIcon; AppLockScreen.kt has no
      context.getString( and no LocalContext resource read (no getString(R.string.pin_ anywhere).
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main"
RES = MAIN / "res"
ANDROID_TEST = ROOT / "app/src/androidTest"
BIOMETRIC = MAIN / "java/com/sherifradwan/ancnative/security/BiometricAuthManager.kt"
APP_LOCK = MAIN / "java/com/sherifradwan/ancnative/ui/AppLockScreen.kt"
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


def code_of(path, problems, xml=False):
    src = read(path)
    if src is None:
        problems.append(path.name + " missing")
        return None
    return strip_xml_comments(src) if xml else strip_comments(src)


def kotlin_files(folder):
    return sorted(folder.rglob("*.kt")) if folder.is_dir() else []


def check_s99():
    problems = []
    if (RES / "values-ar").exists():
        problems.append("app/src/main/res/values-ar still exists")
    for path in kotlin_files(MAIN):
        if re.search(r'Locale\(\s*"ar"', strip_comments(path.read_text(encoding="utf-8"))):
            problems.append(path.name + ' contains Locale("ar")')
    fixtures = 0
    for path in kotlin_files(ANDROID_TEST):
        code = strip_comments(path.read_text(encoding="utf-8"))
        fixtures += len(re.findall(r'Locale\(\s*"ar"', code))
        if "everyNewEnglishStorageStringHasAnArabicValue" in code:
            problems.append(path.name + " still has everyNewEnglishStorageStringHasAnArabicValue")
        if re.search(r'Locale\(\s*"ar"', code) and "getIdentifier(" in code:
            problems.append(path.name + " looks up string resources in an Arabic locale")
    if fixtures > 1:
        problems.append('androidTest has Locale("ar") ' + str(fixtures) + " times, expected at most 1 (RTL fixture)")
    runner = read(CHECK_R3W)
    if runner is None:
        problems.append("check_r3w.py missing")
    elif "values-ar" in runner:
        problems.append("check_r3w.py still mentions values-ar")
    record('S99 no values-ar folder, no Locale("ar") UI-string test, check_r3w.py without values-ar',
           not problems, "; ".join(problems))


def guarded_for(code, version, constant):
    version_test = r"(?:" + str(version) + r"|Build\.VERSION_CODES\." + constant + r")\b"
    sdk_guard = re.search(r"SDK_INT[^\n]*" + version_test, code) is not None
    annotation = re.search(r"@RequiresApi\(\s*(?:api\s*=\s*)?" + version_test, code) is not None
    return sdk_guard or annotation


def check_s100():
    problems = []
    bio = code_of(BIOMETRIC, problems)
    if bio is not None:
        if not guarded_for(bio, 28, "P"):
            problems.append("BiometricAuthManager.kt has no SDK_INT / @RequiresApi guard for API 28")
        if not guarded_for(bio, 30, "R"):
            problems.append("BiometricAuthManager.kt has no SDK_INT / @RequiresApi guard for API 30")
    base = code_of(RES / "values/themes.xml", problems, xml=True)
    if base is not None and "windowSplashScreen" in base:
        problems.append("res/values/themes.xml still has windowSplashScreen")
    v31 = code_of(RES / "values-v31/themes.xml", problems, xml=True)
    if v31 is not None:
        for item in ("windowSplashScreenBackground", "windowSplashScreenAnimatedIcon"):
            if not re.search(r'<item\s+name="android:' + item + r'"', v31):
                problems.append("res/values-v31/themes.xml lacks " + item)
    lock = code_of(APP_LOCK, problems)
    if lock is not None:
        if re.search(r"\bcontext\.getString\(", lock):
            problems.append("AppLockScreen.kt still calls context.getString(")
        if re.search(r"getString\(\s*R\.string\.pin_", lock):
            problems.append("AppLockScreen.kt still reads R.string.pin_* through getString(")
    record("S100 biometric API 28/30 guards, splash items only in values-v31, AppLockScreen without context.getString",
           not problems, "; ".join(problems))


def main():
    check_s99()
    check_s100()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
