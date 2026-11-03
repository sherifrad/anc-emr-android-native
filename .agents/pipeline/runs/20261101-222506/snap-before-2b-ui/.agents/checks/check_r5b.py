import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
UI = MAIN / "ui"

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


def check_s210():
    problems = []
    ds = read(UI / "DesignSystem.kt")
    names = ("BigChoiceButton", "SectionCard", "SummaryRow")
    if ds is None:
        problems.append("ui/DesignSystem.kt missing")
    else:
        code = strip_comments(ds)
        for name in names:
            if len(re.findall(r"\bfun\s+" + name + r"\s*\(", code)) != 1:
                problems.append("DesignSystem.kt must define fun " + name + "( exactly once")
    for path in UI.glob("*.kt"):
        if path.name == "DesignSystem.kt":
            continue
        code = strip_comments(read(path) or "")
        for name in names:
            if re.search(r"\bfun\s+" + name + r"\s*\(", code):
                problems.append(path.name + " defines its own " + name)
    record("S210 shared pieces defined once in DesignSystem.kt", not problems, "; ".join(problems))


def check_s211():
    problems = []
    src = read(UI / "LabEditor.kt")
    if src is None:
        problems.append("ui/LabEditor.kt missing")
    else:
        code = strip_comments(src)
        for name in ("SectionCard", "SummaryRow", "BigChoiceButton"):
            if not re.search(r"\b" + name + r"\s*\(", code):
                problems.append("LabEditor.kt does not call " + name + "(")
        if re.search(r"(?<![A-Za-z])Card\s*\(", code):
            problems.append("LabEditor.kt still has a direct Card( call")
    record("S211 lab screen uses the shared pieces, no hand-written Card(", not problems, "; ".join(problems))


def check_s212():
    problems = []
    src = read(UI / "LabEditor.kt")
    if src is None:
        problems.append("ui/LabEditor.kt missing")
    else:
        code = strip_comments(src)
        tags = [
            "pinnedLabHeaderTag", "labCalculatedGaCard", "labDate", "labTestCard", "labAddTestsCard",
            "labTestProgress", "labTestTitle", "labEditDetailsToggle", "labFlagToggle", "labNoteToggle",
            "labAddTestsToggle", "allLabTestsChip", "labGroupChip-", "saveToLabCodexBanner",
            "saveToLabCodexButton", "labName", "labResult", "labUnit", "labRefRange", "labNotes",
            "labAbnormal", "labResultPreset", "cancelLab", "addAnotherLab", "saveLab",
        ]
        for tag in tags:
            if '"' + tag not in code:
                problems.append('missing tag "' + tag + '"')
    record("S212 lab tags present", not problems, "; ".join(problems))


def check_s213():
    problems = []
    try:
        out = subprocess.run(["git", "diff", "--name-only", "HEAD"], cwd=ROOT, capture_output=True, text=True, check=True).stdout
    except (OSError, subprocess.CalledProcessError) as exc:
        record("S213 no logic files changed", False, "git diff failed: " + str(exc))
        return
    for line in out.splitlines():
        if re.search(r"/ancnative/(model|state|storage)/", line) and "/src/test/" not in line:
            problems.append("logic file changed: " + line)
    record("S213 design round touched no model/state/storage file", not problems, "; ".join(problems))


def main():
    check_s210()
    check_s211()
    check_s212()
    check_s213()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
