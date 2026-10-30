#!/usr/bin/env python3
"""MACA fix pipeline orchestrator (runs on the user's Mac). Orchestrator writes no app code:
it dispatches lanes to vendor CLIs through the vendored relays, enforces file ownership,
runs only the tests the evidence cache says must run, and records metrics.
Usage: run_pipeline.py [--plan] [--root DIR] [--skip-review]
Fallback (env): MACA_FALLBACK_LANES="cli:model,..." - tried in order when a lane returns nothing usable.
Lane overrides (env): MACA_LANE_TESTS, MACA_LANE_CORE, MACA_LANE_UI, MACA_REVIEW_LANES  as 'cli:model' (comma list for review)."""
import argparse, fnmatch, hashlib, json, os, shutil, subprocess, sys, time
from datetime import datetime
from pathlib import Path

HOME = Path.home()
SK = HOME / "skills/multi-agent-collaborative-audit"
ap = argparse.ArgumentParser()
ap.add_argument("--root", default=str(HOME / "anc-emr-android-native"))
ap.add_argument("--plan", action="store_true")
ap.add_argument("--skip-review", action="store_true")
ap.add_argument("--ui-only", action="store_true", help="tests and core already green: run only the UI lane, cache check and review")
ap.add_argument("--round", default="r1", choices=["r1", "r2", "r3", "r3b", "r3c", "r3c2", "r3c3", "r3d", "r3e", "r3f1"], help="r2: SPEC_FIX_R2 briefs in pipeline/r2, round-2 file lists")
ap.add_argument("--resume-tests", metavar="NOTE", help="tests lane timed out after writing tests: send a short finish-up request (NOTE), then continue")
ap.add_argument("--from-core", metavar="RED_RUN_DIR", help="tests already written and red-checked in RED_RUN_DIR: start at the core lane")
ap.add_argument("--skip-ui", action="store_true", help="no UI work this round: skip the UI lane, still run the full gate")
ap.add_argument("--resume-ui", metavar="FAILED_RUN_DIR", help="repair the tests lane after a test-compile halt, then continue from the UI lane")
args = ap.parse_args()
ROOT = Path(args.root)
PIPE = ROOT / ".agents/pipeline"
TS = datetime.now().strftime("%Y%m%d-%H%M%S")
RUN = PIPE / "runs" / TS
os.environ["PATH"] += ":/opt/homebrew/bin:/usr/local/bin:" + str(HOME / ".local/bin")
os.environ.setdefault("MACA_RELAY_TIMEOUT_MS", "900000")

def lane(env, default):
    v = os.environ.get(env, default); cli, _, model = v.partition(":"); return cli, model

LANES = {
    "tests": lane("MACA_LANE_TESTS", "opencode:opencode/glm-5.3-flash-free"),
    "core": lane("MACA_LANE_CORE", "opencode:opencode/deepseek-v4.1-flash-free"),
    "ui": lane("MACA_LANE_UI", "agy:" if args.round == "r1" else "opencode:opencode/deepseek-v4.1-flash-free"),
}
REVIEW = [lane_s for lane_s in os.environ.get("MACA_REVIEW_LANES", "opencode:opencode/glm-5.3-flash-free").split(",") if lane_s]

TEST_GLOBS = ["app/src/test/java/com/sherifradwan/ancnative/ui/ParseDeliveryNotesSpecTest.kt",
              "app/src/test/java/com/sherifradwan/ancnative/ui/ClinicalFormatTest.kt",
              "app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt",
              ".agents/checks/check_ui_static.py", ".agents/TEST_MANIFEST.json"]
UI = "app/src/main/java/com/sherifradwan/ancnative/ui/"
ALLOW = {
    "tests": TEST_GLOBS,
    "core": [UI + "DeliveryNotesParser.kt", UI + "ClinicalFormat.kt", UI + "WorkspaceScreen.kt"],
    "ui": [UI + "ClinicalBadges.kt", UI + "Theme.kt", UI + "WorkspaceScreen.kt", UI + "ProfileScreen.kt", "app/src/main/res/values/strings.xml"],
    "review": [],
}
T = "app/src/test/java/com/sherifradwan/ancnative/ui/"
if args.round == "r2":
    TEST_GLOBS = [T + "ParseDeliveryNotesSpecTest.kt", T + "ClinicalFormatR2Test.kt", T + "ClinicalFormatTest.kt",
                  T + "report/PatientPrintHtmlGeneratorTest.kt", ".agents/checks/check_ui_static_r2.py", ".agents/TEST_MANIFEST.json"]
    ALLOW = {
        "tests": TEST_GLOBS,
        "core": [UI + "DeliveryNotesParser.kt", UI + "ClinicalFormat.kt", UI + "report/PatientPrintHtmlGenerator.kt"],
        "ui": [UI + "PatientListScreen.kt", UI + "ProfileScreen.kt", UI + "WorkspaceScreen.kt", UI + "ClinicalBadges.kt", "app/src/main/res/values/strings.xml"],
        "review": [],
    }
if args.round == "r3":
    M = "app/src/main/java/com/sherifradwan/ancnative/"
    TEST_GLOBS = [T + "report/PatientPrintHtmlGeneratorTest.kt", "app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt",
                  ".agents/checks/check_r3.py", ".agents/TEST_MANIFEST.json"]
    ALLOW = {
        "tests": TEST_GLOBS,
        "core": [UI + "report/PatientPrintHtmlGenerator.kt", M + "state/IntakeDatingRule.kt", M + "state/AncViewModel.kt",
                 M + "storage/RecordsRepository.kt", M + "storage/VaultController.kt", "app/build.gradle.kts"],
        "ui": [],
        "review": [],
    }
if args.round == "r3b":
    M = "app/src/main/java/com/sherifradwan/ancnative/"; ST = "app/src/test/java/com/sherifradwan/ancnative/storage/"
    TEST_GLOBS = [ST + "RecordAnomaliesTest.kt", ST + "VaultAutoActionTest.kt", ST + "VaultCleanupRuleTest.kt",
                  ".agents/checks/check_r3b.py", ".agents/TEST_MANIFEST.json"]
    ALLOW = {
        "tests": TEST_GLOBS,
        "core": [M + "storage/RecordAnomalies.kt", M + "storage/VaultAutoAction.kt", M + "storage/VaultCleanupRule.kt",
                 M + "storage/RecordsRepository.kt", M + "storage/VaultFiles.kt", M + "storage/VaultController.kt", M + "MainActivity.kt"],
        "ui": [],
        "review": [],
    }
if args.round == "r3c":
    M = "app/src/main/java/com/sherifradwan/ancnative/"; SE = "app/src/test/java/com/sherifradwan/ancnative/security/"
    TEST_GLOBS = [SE + "PinPolicyTest.kt", SE + "AppLockPolicyTest.kt", ".agents/checks/check_r3c.py", ".agents/TEST_MANIFEST.json"]
    ALLOW = {
        "tests": TEST_GLOBS,
        "core": [M + "security/PinPolicy.kt", M + "security/AppLockPolicy.kt", M + "security/SecuritySettingsRepository.kt",
                 M + "security/BiometricAuthManager.kt", M + "MainActivity.kt"],
        "ui": [UI + "AppLockScreen.kt", UI + "AncApp.kt", UI + "SecuritySettingsDialog.kt", "app/src/main/res/values/strings.xml"],
        "review": [],
    }
if args.round == "r3c2":
    M = "app/src/main/java/com/sherifradwan/ancnative/"
    TEST_GLOBS = [".agents/checks/check_r3c.py"]
    ALLOW = {"tests": TEST_GLOBS, "core": [M + "security/SecuritySettingsRepository.kt"], "ui": [UI + "AppLockScreen.kt"], "review": []}
    CORE_GATE_EXTRA = set()
if args.round == "r3c3":
    TEST_GLOBS = [".agents/checks/check_r3c.py"]
    ALLOW = {"tests": TEST_GLOBS, "core": [], "ui": [UI + "AncApp.kt"], "review": []}
if args.round == "r3d":
    M = "app/src/main/java/com/sherifradwan/ancnative/"
    TEST_GLOBS = [".agents/checks/check_r3d.py", "app/src/test/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGeneratorTest.kt", ".agents/TEST_MANIFEST.json"]
    ALLOW = {
        "tests": TEST_GLOBS,
        "core": ["app/src/main/res/xml/file_paths.xml", M + "MainActivity.kt", M + "security/SecuritySettingsRepository.kt",
                 M + "ui/report/PatientPrintHtmlGenerator.kt", M + "storage/AttachmentSyncManager.kt"],
        "ui": [UI + "SecuritySettingsDialog.kt", UI + "report/PatientTwoPageReportDialog.kt", UI + "WorkspaceScreen.kt", UI + "PatientEditor.kt",
               "app/src/main/res/values/strings.xml"],
        "review": [],
    }
if args.round == "r3e":
    M = "app/src/main/java/com/sherifradwan/ancnative/"; ST = "app/src/test/java/com/sherifradwan/ancnative/"
    TEST_GLOBS = [".agents/checks/check_r3e.py", ST + "storage/VaultAutoActionTest.kt", ST + "ui/ThemeContrastTest.kt", ".agents/TEST_MANIFEST.json"]
    ALLOW = {
        "tests": TEST_GLOBS,
        "core": [M + "storage/VaultAutoAction.kt", M + "state/AncViewModel.kt", M + "MainActivity.kt"],
        "ui": [UI + "AppLockScreen.kt", UI + "UltrasoundPhotoViewerDialog.kt", UI + "ClinicalAttachmentViewerDialog.kt", UI + "Theme.kt",
               UI + "PatientListScreen.kt", UI + "ProfileScreen.kt", UI + "AddAttachmentDialog.kt", "app/src/main/res/values/strings.xml"],
        "review": [],
    }
if args.round == "r3f1":
    M = "app/src/main/java/com/sherifradwan/ancnative/"; ST = "app/src/test/java/com/sherifradwan/ancnative/"
    TEST_GLOBS = [".agents/checks/check_r3f1.py", ".agents/checks/check_r3b.py", ".agents/TEST_MANIFEST.json", ST + "NegativeControlAndClinicalValidationTest.kt",
                  ST + "storage/HashingTest.kt", ST + "storage/BoundedBitmapTest.kt", ST + "storage/VaultCleanupRuleTest.kt",
                  ST + "security/SecuritySettingsRepositoryTest.kt", ST + "security/FakeSharedPreferences.kt", ST + "state/AttachmentErrorStateTest.kt"]
    ALLOW = {
        "tests": TEST_GLOBS,
        "core": [M + "storage/Hashing.kt", M + "storage/BoundedBitmap.kt", M + "storage/PatientAttachmentVault.kt", M + "storage/ClinicalCommands.kt",
                 M + "storage/SaveVisitCommand.kt", M + "storage/VaultCleanupRule.kt", M + "storage/VaultController.kt",
                 M + "security/SecuritySettingsRepository.kt", M + "state/AncState.kt", M + "state/AncViewModel.kt"],
        "ui": [UI + "WorkspaceScreen.kt", UI + "ClinicalAttachmentViewerDialog.kt", UI + "UltrasoundPhotoViewerDialog.kt", UI + "AddAttachmentDialog.kt",
               "app/src/main/res/values/strings.xml"],
        "review": [],
    }
BRIEFS = PIPE / args.round if args.round in ("r2", "r3", "r3b", "r3c", "r3c2", "r3c3", "r3d", "r3e", "r3f1") else PIPE
# Tests that reference functions the core lane has not written yet; parked during the behavioural red check.
NEW_API_TESTS = {"r2": [T + "ClinicalFormatR2Test.kt"], "r3": ["app/src/test/java/com/sherifradwan/ancnative/state/IntakeDatingRuleTest.kt"],
                 "r3b": ["app/src/test/java/com/sherifradwan/ancnative/storage/" + n for n in ("RecordAnomaliesTest.kt", "VaultAutoActionTest.kt", "VaultCleanupRuleTest.kt")],
                 "r3c": ["app/src/test/java/com/sherifradwan/ancnative/security/" + n for n in ("PinPolicyTest.kt", "AppLockPolicyTest.kt")],
                 "r3e": ["app/src/test/java/com/sherifradwan/ancnative/storage/VaultAutoActionTest.kt"],
                 "r3f1": ["app/src/test/java/com/sherifradwan/ancnative/" + n for n in ("storage/HashingTest.kt", "storage/BoundedBitmapTest.kt", "storage/VaultCleanupRuleTest.kt", "security/SecuritySettingsRepositoryTest.kt", "security/FakeSharedPreferences.kt", "state/AttachmentErrorStateTest.kt")]}.get(args.round, [])
WATCH = ["app/src", "tests", ".agents/checks", ".agents/TEST_MANIFEST.json", "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "app/build.gradle.kts"]
METRICS = []

def sh(cmd, timeout=1800, log=None):
    t = time.time()
    p = subprocess.run(cmd, shell=True, cwd=ROOT, capture_output=True, text=True, timeout=timeout)
    out = (p.stdout or "") + (p.stderr or "")
    if log: Path(log).write_text(out)
    return p.returncode, out, round(time.time() - t, 1)

def snapshot():
    snap = {}
    for w in WATCH:
        base = ROOT / w
        files = [base] if base.is_file() else [f for f in base.rglob("*") if f.is_file()] if base.exists() else []
        for f in files:
            if "/build/" in str(f) or "__pycache__" in str(f): continue
            snap[str(f.relative_to(ROOT))] = hashlib.sha1(f.read_bytes()).hexdigest()
    return snap

def backup(dest):
    for w in WATCH:
        s = ROOT / w
        if not s.exists(): continue
        d = dest / w; d.parent.mkdir(parents=True, exist_ok=True)
        if s.is_dir(): shutil.copytree(s, d, dirs_exist_ok=True, ignore=shutil.ignore_patterns("build", "__pycache__"))
        else: shutil.copy2(s, d)

def dispatch(step, role_file, lane_key, allow_key, extra="", read_only=False, cli=None, model=None):
    cli, model = (cli, model) if cli else LANES[lane_key]
    RUN.mkdir(parents=True, exist_ok=True)
    brief = RUN / f"{step}.brief.md"
    allowed = ALLOW[allow_key]
    brief.write_text((BRIEFS / "00_common_prefix.md").read_text() + "\n" + (BRIEFS / role_file).read_text()
                     + "\n\nALLOWED (machine-enforced): " + (", ".join(allowed) or "NONE (read-only)") + "\n" + extra)
    snap_dir = RUN / f"snap-before-{step}"; backup(snap_dir)
    before = snapshot(); t = time.time()
    relay = SK / f"vendor/delegate-skills/skills/{cli}-delegate/scripts/relay.mjs"
    out_dir = RUN / step
    chain = [(cli, model)] + [tuple(f.partition(":")[::2]) for f in os.environ.get("MACA_FALLBACK_LANES", "").split(",") if f and f != f"{cli}:{model}"]
    tried = []
    for ci, (cli, model) in enumerate(chain):   # a dead or silent provider hands the lane to the next model, never stops the run
        relay = SK / f"vendor/delegate-skills/skills/{cli}-delegate/scripts/relay.mjs"
        out_dir = RUN / (step if ci == 0 else f"{step}.fallback{ci}")
        cmd = f'node "{relay}" --brief "{brief}" --cd "{ROOT}" --out-dir "{out_dir}"' + (f' --model "{model}"' if model and cli != "claude" else "") + (" --read-only" if read_only else "")
        if cli == "claude" and model: cmd = f'ANTHROPIC_MODEL="{model}" ' + cmd   # claude relay drops --model
        rc, _, _ = sh(cmd, timeout=1800)
        try: r = json.loads((out_dir / "result.json").read_text())
        except Exception: r = {}
        tried.append(f"{cli}:{model}={r.get('status')}")
        cur = snapshot(); now_changed = [p for p in set(before) | set(cur) if before.get(p) != cur.get(p)]
        if r.get("status") == "completed" and (not read_only or len((out_dir / "stdout.txt").read_text() if (out_dir / "stdout.txt").exists() else r.get("finalMessage", "")) > 200): break
        if now_changed and not read_only: break   # partial work exists: the repair loop continues from it
        print(f"[{step}] {cli}:{model} gave no usable result ({r.get('status')}); trying next model", flush=True)
    secs = round(time.time() - t, 1)
    after = snapshot()
    changed = sorted(p for p in set(before) | set(after) if before.get(p) != after.get(p))
    viol = [p for p in changed if not any(fnmatch.fnmatch(p, g) for g in allowed)]
    for p in viol:   # revert out-of-scope changes
        src = snap_dir / p
        if src.exists(): shutil.copy2(src, ROOT / p)
        elif (ROOT / p).exists(): (ROOT / p).unlink()
    status = "?"
    try: status = json.loads((out_dir / "result.json").read_text()).get("status")
    except Exception: pass
    m = dict(step=step, lane=f"{cli}:{model}", tried=tried, seconds=secs, relay_status=status, relay_exit=rc,
             changed=[p for p in changed if p not in viol], scope_violations_reverted=viol)
    METRICS.append(m); print(f"[{step}] {cli} {status} {secs}s changed={len(m['changed'])} violations={len(viol)}", flush=True)
    if status != "completed" and not m["changed"]:   # lane produced nothing (quota, auth, permission): repairs cannot help
        try: why = json.loads((out_dir / "result.json").read_text()).get("stderrTail", "")[-400:]
        except Exception: why = ""
        (RUN / "metrics.json").write_text(json.dumps(METRICS, indent=1))
        print(f"HALT: lane {cli}:{model} unavailable at step {step} (no output, no changes). Last stderr:\n{why}"); sys.exit(2)
    return m

CORE_GATE = {"UNIT-parse", "UNIT-format", "COMPILE-app"}  # UI-only checks (contrast, static a-d) can't pass before the UI lane
if args.round == "r2": CORE_GATE |= {"UNIT-parse-legacy", "UNIT-format-r2", "UNIT-print"}
if args.round == "r3": CORE_GATE |= {"UNIT-print", "UNIT-intake-dating", "STATIC-r3"}
if args.round == "r3b": CORE_GATE |= {"UNIT-anomalies", "UNIT-vault-auto", "UNIT-vault-cleanup", "STATIC-r3b"}
if args.round == "r3c": CORE_GATE |= {"UNIT-pin-policy", "UNIT-lock-policy", "STATIC-r3c"}
if args.round == "r3c2": CORE_GATE |= {"UNIT-pin-policy", "UNIT-lock-policy"}
if args.round == "r3e": CORE_GATE |= {"UNIT-vault-auto"}   # STATIC-r3e and UNIT-contrast span the UI lane
if args.round == "r3f1": CORE_GATE |= {"UNIT-all"}
if args.round == "r3d": CORE_GATE |= {"UNIT-print"}   # STATIC-r3d spans the UI lane, so it is gated after 2b

def test_compile_failed(res):
    """True when a failure is the unit-test source set not compiling (an error in a test file), not a failing assertion."""
    for i in res.get("failed", []):
        log = RUN / f"{res['step']}.{i}.log"
        if log.exists() and "compileDebugUnitTestKotlin FAILED" in (txt := log.read_text()) and "/src/test/" in txt: return True
    return False

def red_check():
    """Red before green, compile-aware: park tests that need not-yet-written API, run the rest, and count only
    assertion failures as red. Parked files are restored byte-identical (hash checked)."""
    parked = {}
    for rel in NEW_API_TESTS:
        f = ROOT / rel
        if f.exists(): parked[rel] = hashlib.sha1(f.read_bytes()).hexdigest(); f.rename(f.with_suffix(".kt.parked"))
    try: red = run_selected("1b-red", max_tier=1, exclude={"UNIT-format-r2", "UNIT-intake-dating", "UNIT-anomalies", "UNIT-vault-auto", "UNIT-vault-cleanup", "UNIT-pin-policy", "UNIT-lock-policy"} if parked else ())
    finally:
        for rel, h in parked.items():
            f = ROOT / rel; f.with_suffix(".kt.parked").rename(f)
            assert hashlib.sha1(f.read_bytes()).hexdigest() == h, f"parked test changed: {rel}"
    compile_red = test_compile_failed(red)
    METRICS.append(dict(step="red-check", parked=list(parked), failed=red.get("failed", []), compile_error=compile_red,
                        behavioural_red_confirmed=bool(red.get("failed")) and not compile_red))
    return red, compile_red

def run_selected(step, max_tier=2, only=None, exclude=()):
    code, out, _ = sh(f'python3 "{SK}/scripts/impact_and_cache.py" select --ws "{ROOT}" --max-tier {max_tier} --json', timeout=120)
    try: plan = json.loads(out[out.index("["):])
    except Exception: print("select failed:", out[-300:]); return dict(step=step, error="select failed")
    res = dict(step=step, ran=0, reused=0, deferred=0, not_gated=[], passed=[], failed=[])
    for t in plan:
        if (only is not None and t["id"] not in only) or t["id"] in exclude: res["not_gated"].append(t["id"]); continue
        if t["action"] == "REUSE": res["reused"] += 1; continue
        if t["action"] == "DEFER": res["deferred"] += 1; continue
        res["ran"] += 1
        rc, out2, secs = sh(t["cmd"], timeout=1800, log=RUN / f"{step}.{t['id']}.log")
        sh(f'python3 "{SK}/scripts/impact_and_cache.py" record --ws "{ROOT}" --id {t["id"]} --status {"pass" if rc == 0 else "fail"}', timeout=60)
        (res["passed"] if rc == 0 else res["failed"]).append(t["id"])
    METRICS.append(res); print(f"[{step}] ran={res['ran']} reused={res['reused']} deferred={res['deferred']} not_gated={len(res['not_gated'])} failed={res['failed']}", flush=True)
    return res

def tail_of_failures(res):
    return "\n".join((RUN / f"{res['step']}.{i}.log").read_text()[-1500:] for i in res.get("failed", []) if (RUN / f"{res['step']}.{i}.log").exists())

def main():
    print("MACA pipeline", TS, "lanes:", LANES, "review:", REVIEW)
    for cli in {c for c, _ in LANES.values()} | {l.partition(':')[0] for l in REVIEW}:
        print(f"  {cli:9}", shutil.which(cli) or "MISSING")
    if args.plan: return
    RUN.mkdir(parents=True, exist_ok=True)
    backup(ROOT / f".agents/backup-{TS}")
    if args.from_core:
        prior = [m for m in json.loads((Path(args.from_core) / "metrics.json").read_text()) if m.get("step") == "1b-red"]
        METRICS.append(dict(step="red-check-reused", from_run=str(args.from_core), red=prior[-1] if prior else None))
    elif args.ui_only:
        res = run_selected("0-core-gate-recheck", only=CORE_GATE)
        if res.get("failed"): print("HALT: core gate is not green; run the full pipeline."); sys.exit(1)
    elif args.resume_ui:
        # A test file owned by the tests lane did not compile, so no unit test could run. Send it back to its author.
        logs = "\n".join(f.read_text()[-1500:] for f in sorted(Path(args.resume_ui).glob("*.UNIT-*.log"))[-1:])
        dispatch("1c-tests-repair", "10_test_designer.md", "tests", "tests",
                 extra="\nREPAIR ROUND: your test sources do not compile, which blocks every unit test. Fix compile errors in YOUR test files only; keep each oracle's assertions and strictness unchanged.\n" + logs)
        res = run_selected("1c-core-gate", only=CORE_GATE)
        if res.get("failed"):
            (RUN / "metrics.json").write_text(json.dumps(METRICS, indent=1))
            print("HALT: core gate not green after test repair. See", RUN); sys.exit(1)
        red = run_selected("1c-contrast-red", only={"UNIT-contrast"})
        log = RUN / "1c-contrast-red.UNIT-contrast.log"
        compiled = log.exists() and "compileDebugUnitTestKotlin FAILED" not in log.read_text()
        METRICS.append(dict(step="red-check-contrast", red_confirmed=bool(red.get("failed")) and compiled, compiled=compiled))
    else:
        # Phase 1b: tests first
        if args.resume_tests:
            dispatch("1-tests-finish", "10_test_designer.md", "tests", "tests",
                     extra="\nFINISH-UP ROUND: your previous run timed out. Your test files already exist; do not rewrite them. Do ONLY this: " + args.resume_tests)
        else:
            dispatch("1-tests", "10_test_designer.md", "tests", "tests")
        ids = {t["id"] for t in json.loads((ROOT / ".agents/TEST_MANIFEST.json").read_text())["tests"]}
        if missing := CORE_GATE - ids:
            (RUN / "metrics.json").write_text(json.dumps(METRICS, indent=1)); print("HALT: manifest lacks gate ids", sorted(missing)); sys.exit(1)
        red, compile_red = red_check()
        if compile_red:   # route test-source compile errors back to their author, once
            dispatch("1c-tests-repair", "10_test_designer.md", "tests", "tests",
                     extra="\nREPAIR ROUND: your test sources do not compile (new-API tests were parked, so this error is elsewhere). Fix compile errors in YOUR test files only; keep every assertion unchanged.\n" + tail_of_failures(red))
            red, compile_red = red_check()
            if compile_red:
                (RUN / "metrics.json").write_text(json.dumps(METRICS, indent=1)); print("HALT: test sources still do not compile. See", RUN); sys.exit(1)
    # Phase 2a: core builder, up to 2 repair rounds
    res = {} if (args.resume_ui or args.ui_only) else (dispatch("2a-core", "20_build_core.md", "core", "core") and run_selected("2a-green", only=CORE_GATE))
    for rnd in (1, 2):
        if not res.get("failed"): break
        if test_compile_failed(res) and not args.round == "r1":
            dispatch(f"2a-tests-compile-repair{rnd}", "10_test_designer.md", "tests", "tests",
                     extra="\nREPAIR ROUND: the unit-test source set does not compile because of YOUR test files. Fix only that; keep every assertion unchanged.\n" + tail_of_failures(res))
            res = run_selected(f"2a-green-r{rnd}", only=CORE_GATE); continue
        dispatch(f"2a-core-repair{rnd}", "20_build_core.md", "core", "core", extra="\nREPAIR ROUND: these selected tests still fail. Fix the production code only.\n" + tail_of_failures(res))
        res = run_selected(f"2a-green-r{rnd}", only=CORE_GATE)
    if res.get("failed"):
        (RUN / "metrics.json").write_text(json.dumps(METRICS, indent=1))
        print("HALT: core logic still red after 2 repair rounds; UI lane not started. See", RUN); sys.exit(1)
    # Phase 2b: UI builder
    if not args.skip_ui: dispatch("2b-ui", "30_build_ui.md", "ui", "ui")
    res = run_selected("2b-green")
    for rnd in (1, 2):
        if not res.get("failed"): break
        dispatch(f"2b-ui-repair{rnd}", "30_build_ui.md", "ui", "ui", extra="\nREPAIR ROUND: these checks still fail.\n" + tail_of_failures(res))
        res = run_selected(f"2b-green-r{rnd}")
    ui_red = res.get("failed", [])
    METRICS.append(dict(step="ui-gate-final", still_failing=ui_red))
    # Cache demonstration: nothing changed since, so everything should be REUSE
    run_selected("cache-check-no-changes")
    # Phase 3: independent read-only review (reviewers did not write the code)
    if not args.skip_review:
        for i, l in enumerate(REVIEW):
            cli, _, model = l.partition(":")
            dispatch(f"3-review-{i}-{cli}", "40_review.md", None, "review", read_only=True, cli=cli, model=model)
    (RUN / "metrics.json").write_text(json.dumps(METRICS, indent=1))
    print("\nUI GATE STILL RED:" if ui_red else "\nDONE.", ui_red or "", "Metrics:", RUN / "metrics.json", "\nTell Claude: pipeline finished.")

main()
