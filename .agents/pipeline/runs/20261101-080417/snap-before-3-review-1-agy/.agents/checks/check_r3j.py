#!/usr/bin/env python3
"""S55-S58 static source check for round 3j (SPEC_FIX_R3.md, section "Round 3j").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S55 ui/UltrasoundEditor.kt: inside the testTag("scanCalculatedGaCard") Card the
      calculated_ga_at_visit Text and the testTag("applyScanGa") button sit in a Column
      (no Row between the Card and the calculated_ga_at_visit Text).
  S56 res/values/plurals.xml defines photos_count (one = "%d photo", other = "%d photos");
      strings.xml has no name="photos_count"; no "R.string.photos_count" anywhere in
      app/src/main; pluralStringResource(R.plurals.photos_count used at least 4 times
      (WorkspaceScreen.kt x2, ProfileScreen.kt x2).
  S57 ui/ClinicalFormat.kt defines internal fun attachmentDisplayTitle; WorkspaceScreen.kt
      calls it and no longer contains "attachment.title ?: attachment.fileName".
  S58 ui/AddAttachmentDialog.kt contains no emoji (no char >= U+1F000 and no U+FE0F) and
      uses R.string.attachment_retake and R.string.attachment_gallery; strings.xml defines
      both as "Retake" / "Gallery".
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


def main_sources():
    for path in sorted((ROOT / "app/src/main").rglob("*.kt")):
        src = read(path)
        if src is not None:
            yield path, strip_comments(src)


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


def trailing_lambda_body(src, args_end):
    """Body of the trailing content lambda of a call whose argument parens end at args_end."""
    brace = src.find("{", args_end + 1)
    if brace == -1:
        return None
    end = match_end(src, brace, "{", "}")
    if end == -1:
        return None
    return src[brace + 1:end]


def card_body(src, tag):
    """Content lambda of the Card(...) that carries testTag(tag)."""
    at = src.find('testTag("' + tag + '")')
    if at == -1:
        return None
    card_open = src.rfind("Card(", 0, at)
    if card_open == -1:
        return None
    args_end = match_end(src, card_open + len("Card"), "(", ")")
    if args_end == -1:
        return None
    return trailing_lambda_body(src, args_end)


def check_s55():
    problems = []
    src = read(MAIN / "ui/UltrasoundEditor.kt")
    if src is None:
        problems.append("ui/UltrasoundEditor.kt missing")
    else:
        src = strip_comments(src)
        body = card_body(src, "scanCalculatedGaCard")
        if body is None:
            problems.append('Card with testTag("scanCalculatedGaCard") not found')
        else:
            text_at = body.find("calculated_ga_at_visit")
            if text_at == -1:
                problems.append("the card does not show calculated_ga_at_visit")
            else:
                prefix = body[:text_at]
                if "Row(" in prefix:
                    problems.append("a Row( sits between the Card and the calculated_ga_at_visit Text")
                if "Column(" not in prefix:
                    problems.append("the calculated_ga_at_visit Text is not inside a Column")
            if 'testTag("applyScanGa")' not in body:
                problems.append('the card lost testTag("applyScanGa")')
    record("S55 calculated-GA card stacks the Text and apply button in a Column", not problems, "; ".join(problems))


def check_s56():
    problems = []
    plurals = read(RES / "plurals.xml")
    if plurals is None:
        problems.append("res/values/plurals.xml missing")
    else:
        plurals = strip_xml_comments(plurals)
        m = re.search(r'<plurals\s+name="photos_count"\s*>(.*?)</plurals>', plurals, re.S)
        if m is None:
            problems.append('plurals.xml has no <plurals name="photos_count">')
        else:
            block = m.group(1)
            if not re.search(r'<item\s+quantity="one"\s*>\s*%d photo\s*</item>', block):
                problems.append('photos_count lacks <item quantity="one">%d photo</item>')
            if not re.search(r'<item\s+quantity="other"\s*>\s*%d photos\s*</item>', block):
                problems.append('photos_count lacks <item quantity="other">%d photos</item>')
            if "<skip" in plurals:
                problems.append("plurals.xml contains a <skip> block (dead XML written to satisfy a check)")

    strings = read(RES / "strings.xml")
    if strings is None:
        problems.append("res/values/strings.xml missing")
    else:
        if re.search(r'name="photos_count"', strip_xml_comments(strings)):
            problems.append('strings.xml still defines name="photos_count"')

    keepers = []
    for p, s in main_sources():
        if "R.string.photos_count" in s:
            keepers.append(p.name)
    if keepers:
        problems.append("R.string.photos_count still referenced in " + ", ".join(keepers))

    if not problems:
        counts = {}
        for rel in ("ui/WorkspaceScreen.kt", "ui/ProfileScreen.kt"):
            src = read(MAIN / rel)
            n = len(re.findall(r"pluralStringResource\s*\(\s*R\.plurals\.photos_count\b", strip_comments(src or "")))
            counts[rel] = n
            if n < 2:
                problems.append(rel + " uses pluralStringResource(R.plurals.photos_count only " + str(n) + " time(s)")
        total = sum(counts.values())
        if total < 4:
            problems.append("pluralStringResource(R.plurals.photos_count used only " + str(total) + " time(s)")
    record("S56 photos_count is a plural, no string variant, four pluralStringResource call sites", not problems, "; ".join(problems))


def check_s57():
    problems = []
    cf = read(MAIN / "ui/ClinicalFormat.kt")
    if cf is None:
        problems.append("ui/ClinicalFormat.kt missing")
    else:
        if not re.search(r"\binternal\s+fun\s+attachmentDisplayTitle\s*\(", strip_comments(cf)):
            problems.append("ClinicalFormat.kt lacks internal fun attachmentDisplayTitle(")

    ws = read(MAIN / "ui/WorkspaceScreen.kt")
    if ws is None:
        problems.append("ui/WorkspaceScreen.kt missing")
    else:
        ws = strip_comments(ws)
        if "attachmentDisplayTitle(" not in ws:
            problems.append("WorkspaceScreen.kt does not call attachmentDisplayTitle(")
        if re.search(r"attachment\.title\s*\?:\s*attachment\.fileName", ws):
            problems.append('WorkspaceScreen.kt still contains "attachment.title ?: attachment.fileName"')
    record("S57 attachment display title is the one helper, WorkspaceScreen calls it", not problems, "; ".join(problems))


def check_s58():
    problems = []
    dlg = read(MAIN / "ui/AddAttachmentDialog.kt")
    if dlg is None:
        problems.append("ui/AddAttachmentDialog.kt missing")
    else:
        dlg = strip_comments(dlg)
        emoji = [(hex(ord(ch)), dlg[: dlg.find(ch)].count("\n") + 1) for ch in dlg
                 if ord(ch) >= 0x1F000 or ch == "\uFE0F"]
        if emoji:
            problems.append("emoji still present: " + ", ".join(cp + " (line " + str(line) + ")" for cp, line in emoji))
        if "R.string.attachment_retake" not in dlg:
            problems.append("AddAttachmentDialog.kt does not use R.string.attachment_retake")
        if "R.string.attachment_gallery" not in dlg:
            problems.append("AddAttachmentDialog.kt does not use R.string.attachment_gallery")

    strings = read(RES / "strings.xml")
    if strings is None:
        problems.append("res/values/strings.xml missing")
    else:
        strings = strip_xml_comments(strings)
        if not re.search(r'<string\s+name="attachment_retake"\s*>\s*Retake\s*</string>', strings):
            problems.append('strings.xml lacks <string name="attachment_retake">Retake</string>')
        if not re.search(r'<string\s+name="attachment_gallery"\s*>\s*Gallery\s*</string>', strings):
            problems.append('strings.xml lacks <string name="attachment_gallery">Gallery</string>')
    record("S58 photo-preview buttons are emoji-free Retake/Gallery string resources", not problems, "; ".join(problems))


def main():
    check_s55()
    check_s56()
    check_s57()
    check_s58()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
