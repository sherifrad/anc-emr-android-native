#!/usr/bin/env python3
"""S29-S35 static source check for round 3f-1 (SPEC_FIX_R3.md).

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S29 "%02x" occurs in app/src/main only in storage/Hashing.kt.
  S30 BitmapFactory.decode occurs in app/src/main only in storage/BoundedBitmap.kt.
  S31 VaultController calls cleanUpAbandonedInitialization( 3 times, no hasVaultDatabase(;
      the helper's deleteOrphanAliases() call sits directly behind `if (!hasVaultDatabase(`, after cleanupUnpublished().
  S32 SecuritySettingsRepository: no android.util.Base64, no System.currentTimeMillis() outside the
      internal SharedPreferences constructor, internal constructor present.
  S33 AncViewModel: row delete before file delete; AttachmentFailed( in add and delete; CancellationException rethrown in add.
  S34 WorkspaceScreen shows the attachment error banner; strings.xml defines both messages.
  S35 no stray PatientPrintHtmlGeneratorTest.kt; Rh test has no bg.contains("-").
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
TEST = ROOT / "app/src/test"

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


def balanced(src, idx, open_ch, close_ch):
    start = src.find(open_ch, idx)
    if start < 0:
        return ""
    depth = 0
    for i in range(start, len(src)):
        if src[i] == open_ch:
            depth += 1
        elif src[i] == close_ch:
            depth -= 1
            if depth == 0:
                return src[start:i + 1]
    return src[start:]


def block_after(src, idx):
    return balanced(src, idx, "{", "}")


def paren_after(src, idx):
    return balanced(src, idx, "(", ")")


def fun_body(src, name):
    m = re.search(r"\bfun\s+" + name + r"\s*\(", src)
    if not m:
        return None
    after_params = m.start() + src[m.start():].find("(") + len(paren_after(src, m.start()))
    return block_after(src, after_params)


def helper_guard_ok(rule_src):
    """(ok, why) for cleanUpAbandonedInitialization in comment-stripped VaultCleanupRule.kt source."""
    if not re.search(r"\bfun\s+cleanUpAbandonedInitialization\s*\(", rule_src):
        return False, "cleanUpAbandonedInitialization missing"
    body = fun_body(rule_src, "cleanUpAbandonedInitialization") or ""
    calls = list(re.finditer(r"\bdeleteOrphanAliases\s*\(\s*\)", body))
    if len(calls) != 1:
        return False, "expected exactly one deleteOrphanAliases() invocation in the helper"
    before = body[:calls[0].start()]
    if not re.search(r"\bif\s*\(\s*!\s*hasVaultDatabase\s*\([^()]*\)\s*\)\s*\{?\s*$", before):
        return False, "deleteOrphanAliases() is not directly behind if (!hasVaultDatabase(...))"
    if not re.search(r"\bcleanupUnpublished\s*\(\s*\)", before):
        return False, "cleanupUnpublished() not called before the alias delete"
    return True, ""


def only_in(literal, allowed_rel):
    """Problems for a literal that may appear in app/src/main only in allowed_rel (which must contain it)."""
    problems = []
    found = False
    for path in sorted(MAIN.rglob("*.kt")):
        rel = path.relative_to(MAIN).as_posix()
        if literal in strip_comments(read(path) or ""):
            if rel == allowed_rel:
                found = True
            else:
                problems.append(rel)
    if not found:
        problems.append(allowed_rel + " missing or lacks " + literal)
    return problems


def main():
    p = only_in("%02x", "storage/Hashing.kt")
    record("S29 hex formatting lives only in Hashing.kt", not p, "; ".join(p))

    p = only_in("BitmapFactory.decode", "storage/BoundedBitmap.kt")
    record("S30 bitmap decoding lives only in BoundedBitmap.kt", not p, "; ".join(p))

    problems = []
    ctl = code_of("storage/VaultController.kt")
    rule = code_of("storage/VaultCleanupRule.kt")
    if ctl is None:
        problems.append("VaultController.kt missing")
    else:
        n = len(re.findall(r"\bcleanUpAbandonedInitialization\s*\(", ctl))
        if n != 3:
            problems.append("VaultController calls cleanUpAbandonedInitialization( %d times, expected 3" % n)
        if "hasVaultDatabase(" in ctl:
            problems.append("VaultController still contains hasVaultDatabase(")
    if rule is None:
        problems.append("VaultCleanupRule.kt missing")
    else:
        ok, why = helper_guard_ok(rule)
        if not ok:
            problems.append(why)
    record("S31 one guarded cleanup helper, three call sites", not problems, "; ".join(problems))

    repo = code_of("security/SecuritySettingsRepository.kt")
    if repo is None:
        record("S32 repository seam", False, "file missing")
    else:
        problems = []
        if "android.util.Base64" in repo:
            problems.append("android.util.Base64 still used")
        ctor = re.search(r"\binternal\s+constructor\s*\(", repo)
        ctor_text = paren_after(repo, ctor.start()) if ctor else ""
        if not ctor or not re.search(r"\w+\s*:\s*SharedPreferences\b", ctor_text):
            problems.append("no internal constructor taking SharedPreferences")
        rest = repo.replace(ctor_text, "", 1) if ctor_text else repo
        if re.search(r"System\s*\.\s*currentTimeMillis\s*\(", rest):
            problems.append("System.currentTimeMillis() used outside the constructor default")
        record("S32 repository seam", not problems, "; ".join(problems))

    vm = code_of("state/AncViewModel.kt")
    if vm is None:
        record("S33 attachment errors surfaced, row deleted first", False, "file missing")
    else:
        problems = []
        add = fun_body(vm, "addAttachment")
        dele = fun_body(vm, "deleteAttachment")
        if add is None:
            problems.append("addAttachment missing")
        else:
            if "AttachmentFailed(" not in add:
                problems.append("addAttachment does not dispatch AttachmentFailed(")
            if not re.search(r"catch\s*\(\s*\w+\s*:\s*(?:[\w.]*\.)?CancellationException\s*\)\s*\{[^}]*\bthrow\b", add):
                problems.append("addAttachment does not rethrow CancellationException")
        if dele is None:
            problems.append("deleteAttachment missing")
        else:
            if "AttachmentFailed(" not in dele:
                problems.append("deleteAttachment does not dispatch AttachmentFailed(")
            row = dele.find("controller.deleteAttachment(")
            file_ = dele.find("PatientAttachmentVault.deleteAttachment(")
            if row < 0 or file_ < 0 or row > file_:
                problems.append("controller.deleteAttachment( must come before PatientAttachmentVault.deleteAttachment(")
        record("S33 attachment errors surfaced, row deleted first", not problems, "; ".join(problems))

    ws = code_of("ui/WorkspaceScreen.kt")
    strings = read(ROOT / "app/src/main/res/values/strings.xml")
    problems = []
    if ws is None:
        problems.append("WorkspaceScreen.kt missing")
    else:
        for needle in ["attachment_error_banner", "DismissAttachmentError",
                       "R.string.attachment_save_failed", "R.string.attachment_delete_failed"]:
            if needle not in ws:
                problems.append("WorkspaceScreen lacks " + needle)
    for name in ["attachment_save_failed", "attachment_delete_failed"]:
        if strings is None or not re.search(r'<string\s+name="%s"' % name, strings):
            problems.append("strings.xml lacks " + name)
    record("S34 attachment error banner and strings", not problems, "; ".join(problems))

    problems = []
    for path in TEST.rglob("PatientPrintHtmlGeneratorTest.kt"):
        rel = "/" + path.relative_to(TEST).as_posix()
        if "/ui/report/" not in rel:
            problems.append("stray " + rel)
    neg = code_of("NegativeControlAndClinicalValidationTest.kt", TEST / "java/com/sherifradwan/ancnative")
    if neg is None:
        problems.append("NegativeControlAndClinicalValidationTest.kt missing")
    elif re.search(r'\bbg\s*\.\s*contains\s*\(\s*"-"\s*\)', neg):
        problems.append('Rh test still uses bg.contains("-")')
    record("S35 duplicate print test gone, Rh tautology gone", not problems, "; ".join(problems))

    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
