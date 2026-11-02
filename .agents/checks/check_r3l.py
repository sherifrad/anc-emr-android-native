#!/usr/bin/env python3
"""S64-S66 static source check for round 3l (SPEC_FIX_R3.md, section "Round 3l").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S64 storage/RecordsRepository.kt: inside ONLY the four clinical save functions
      (saveDating, saveUltrasoundScan, saveLabInvestigation, saveMedication) no
      "if (patientUpdated != 1) return" remains, the guard is still evaluated
      ("patientUpdated" appears at least four times, so it was converted to a
      throw and not deleted), and "throw ClinicalCommandAbort(" appears at least
      four times (one late abort per function, mirroring PatientCommandAbort).
      Decisions before the first write may keep returning Rejected; the patient
      path is out of scope.
  S65 storage/SaveVisitCommand.kt and storage/RecordsRepository.kt contain no
      "isSyntheticEdit = true" wording (Q82: both write false); strings.xml has
      no "Synthetic patient record saved".
  S66 state/AncViewModel.kt calls profileSavedNotice( and no longer contains the
      bare statement controller.execute(datingCmd, sessionEpoch) {}; the list of
      patients handed to RecordCommitted comes from the dating snapshot when it
      is saved (see the spec; textual check covers the notice side);
      ui/WorkspaceScreen.kt handles SavedNotice.PATIENT_DATING_NOT_UPDATED;
      strings.xml defines patient_saved_dating_not_updated.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
RES = ROOT / "app/src/main/res/values"

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


def match_end(src, start, open_ch, close_ch):
    """Index of the close_ch matching src[start] == open_ch, or -1."""
    depth = 0
    for i in range(start, len(src)):
        if src[i] == open_ch:
            depth += 1
        elif src[i] == close_ch:
            depth -= 1
            if depth == 0:
                return i
    return -1


CLINICAL_SAVE_FUNCTIONS = (
    "saveDating",
    "saveUltrasoundScan",
    "saveLabInvestigation",
    "saveMedication",
)


def function_body(src, name):
    """Body between the braces of `fun <name>(...)` (first match), or None."""
    head = src.find("fun " + name + "(")
    if head == -1:
        return None
    args_start = head + len("fun " + name)
    args_end = match_end(src, args_start, "(", ")")
    if args_end == -1:
        return None
    brace = src.find("{", args_end + 1)
    if brace == -1:
        return None
    end = match_end(src, brace, "{", "}")
    if end == -1:
        return None
    return src[brace + 1:end]


def check_s64():
    problems = []
    src = read(MAIN / "storage/RecordsRepository.kt")
    if src is None:
        problems.append("storage/RecordsRepository.kt missing")
    else:
        src = strip_comments(src)
        bodies = []
        for name in CLINICAL_SAVE_FUNCTIONS:
            body = function_body(src, name)
            if body is None:
                problems.append(name + "() body not found")
            else:
                bodies.append(body)
        combined = "\n".join(bodies)
        late_returns = re.findall(
            r"if\s*\(\s*patientUpdated\s*!=\s*1\s*\)\s*(?:\{\s*)?return",
            combined,
            flags=re.S,
        )
        if late_returns:
            problems.append(
                "if (patientUpdated != 1) return still present "
                + str(len(late_returns))
                + " time(s) inside the four clinical save functions (a "
                  "rejected decision after the first write must throw "
                  "ClinicalCommandAbort so the transaction rolls back)",
            )
        guard_uses = combined.count("patientUpdated")
        if guard_uses < 4:
            problems.append(
                "patientUpdated is evaluated only " + str(guard_uses) +
                " time(s) across the four clinical save functions (the stale-revision guard was deleted instead of converted)",
            )
        throws = re.findall(r"throw\s+ClinicalCommandAbort\s*\(", combined)
        if len(throws) < 4:
            problems.append(
                "throw ClinicalCommandAbort( appears " + str(len(throws)) +
                " time(s) inside the four clinical save functions, needs at least 4",
            )
    record(
        "S64 late rejections in the four clinical save functions abort the transaction",
        not problems,
        "; ".join(problems),
    )


def check_s65():
    problems = []
    for rel in ("storage/SaveVisitCommand.kt", "storage/RecordsRepository.kt"):
        src = read(MAIN / rel)
        if src is None:
            problems.append(rel + " missing")
            continue
        hits = re.findall(r"isSyntheticEdit\s*=\s*true\b", strip_comments(src))
        if hits:
            problems.append(rel + " still contains isSyntheticEdit = true (" + str(len(hits)) + " occurrence(s))")
    strings = read(RES / "strings.xml")
    if strings is None:
        problems.append("res/values/strings.xml missing")
    else:
        strings = strip_xml_comments(strings)
        if "Synthetic patient record saved" in strings:
            problems.append('strings.xml still contains "Synthetic patient record saved"')
    record(
        "S65 doctor-entered visits are real (isSyntheticEdit = false in both places) and the saved notice says Patient record saved",
        not problems,
        "; ".join(problems),
    )


def check_s66():
    problems = []
    vm = read(MAIN / "state/AncViewModel.kt")
    if vm is None:
        problems.append("state/AncViewModel.kt missing")
    else:
        vm = strip_comments(vm)
        if "profileSavedNotice(" not in vm:
            problems.append("AncViewModel.kt does not call profileSavedNotice(")
        if re.search(
            r"controller\s*\.\s*execute\s*\(\s*datingCmd\s*,\s*sessionEpoch\s*\)\s*\{\s*\}",
            vm,
            flags=re.S,
        ):
            problems.append("AncViewModel.kt still contains the fire-and-forget statement controller.execute(datingCmd, sessionEpoch) {}")

    ws = read(MAIN / "ui/WorkspaceScreen.kt")
    if ws is None:
        problems.append("ui/WorkspaceScreen.kt missing")
    else:
        # r6b3 moved the notice -> string mapping to SavedNotice?.stringRes() in state/AncState.kt (JVM-testable).
        mapping = strip_comments(ws) + strip_comments(read(MAIN / "state/AncState.kt") or "")
        if "SavedNotice.PATIENT_DATING_NOT_UPDATED" not in mapping:
            problems.append("neither WorkspaceScreen.kt nor AncState.kt handles SavedNotice.PATIENT_DATING_NOT_UPDATED")

    strings = read(RES / "strings.xml")
    if strings is None:
        problems.append("res/values/strings.xml missing")
    else:
        if not re.search(r'<string\s+name="patient_saved_dating_not_updated"\s*>', strip_xml_comments(strings)):
            problems.append('strings.xml lacks <string name="patient_saved_dating_not_updated">')
    record(
        "S66 the profile save reports the dating outcome via profileSavedNotice and the UI maps it to patient_saved_dating_not_updated",
        not problems,
        "; ".join(problems),
    )


def main():
    check_s64()
    check_s65()
    check_s66()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
