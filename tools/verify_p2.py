#!/usr/bin/env python3
"""P2 native verification harness for disposable API 36 Android emulators only.

Uses Python stdlib plus adb and installed Android SDK tools. It never uninstalls,
clears package data, changes a device credential, or resets the canonical vault.
"""

from __future__ import annotations

import argparse
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import time
import uuid
import zipfile
import xml.etree.ElementTree as ET


PACKAGE = "com.sherifradwan.ancnative.dev"
ACTIVITY = "com.sherifradwan.ancnative.dev/com.sherifradwan.ancnative.MainActivity"
TRUSTED_CREDENTIAL_PACKAGES = frozenset({"com.android.settings", "com.android.systemui"})
TRUSTED_CREDENTIAL_CANCEL_RESOURCE_IDS = frozenset(
    {
        "android:id/button2",
        "android:id/cancel",
        "com.android.settings:id/cancel",
        "com.android.settings:id/cancelButton",
        "com.android.settings:id/cancel_button",
        "com.android.systemui:id/cancel",
        "com.android.systemui:id/cancel_button",
    }
)
SENTINEL_PREFIX = "P2CIPHERSENTINEL"
ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "evidence" / "p2"
SCHEMA = ROOT / "app" / "schemas" / "com.sherifradwan.ancnative.storage.RecordsDatabase" / "1.json"
TEST_APK = ROOT / "app" / "build" / "outputs" / "apk" / "androidTest" / "debug" / "app-debug-androidTest.apk"
DEVICE_XML = "/data/local/tmp/anc-native-p2-window.xml"
CANONICAL_VAULT_DIRECTORY = "no_backup/p2-vault/canonical"
MAX_SCROLLS_PER_DIRECTION = 16
REQUIRED_CONNECTED_TEST_CLASSES = (
    "AncWorkflowTest",
    "EncryptedRecordsTest",
    "VaultIntegrationTest",
)
API36_QUALIFICATION_REQUIRED_CHECKS = (
    "os_credential_prerequisite",
    "p1_dirty_back_keep_discard",
    "p1_duplicate_save_prevention",
    "workspace_profile_same_source",
    "p1_cross_patient_isolation",
    "background_lock",
    "manual_lock",
    "acknowledged_save_process_recreation",
    "background_process_death_persistence",
    "p1_rotation_retains_unlocked_draft",
    "unsaved_draft_process_recreation",
    "unsaved_draft_process_death",
    "ciphertext_sentinel_scan",
    "connected_failure_injection_tests",
    "os_credential_cancel",
)
DIALOG_TAG_TEXT_FALLBACKS = {
    "keepEditingDialog": (
        ("Discard draft changes?", "Keep editing"),
        ("هل تريد تجاهل تغييرات المسودة؟", "متابعة التعديل"),
    ),
    "discardDialog": (
        ("Discard draft changes?", "Discard"),
        ("هل تريد تجاهل تغييرات المسودة؟", "تجاهل"),
    ),
}


class HarnessFailure(RuntimeError):
    pass


def run(command: list[str], *, timeout: int = 60, check: bool = True, input_text: str | None = None) -> subprocess.CompletedProcess[str]:
    completed = subprocess.run(
        command,
        input=input_text,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        timeout=timeout,
        check=False,
    )
    if check and completed.returncode != 0:
        safe_command = ["<redacted>" if "ANC_P2_EMULATOR_PIN" in part else part for part in command]
        raise HarnessFailure(f"command failed ({completed.returncode}): {safe_command}: {completed.stdout[-1200:]}")
    return completed


def sdk_tool(name: str) -> str | None:
    direct = shutil.which(name)
    if direct:
        return direct
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        return None
    root = Path(sdk)
    candidates = list(root.glob(f"build-tools/*/{name}")) + list(root.glob(f"cmdline-tools/*/bin/{name}"))
    return str(sorted(candidates)[-1]) if candidates else None


class Device:
    def __init__(self, adb: str, serial: str):
        self.adb = adb
        self.serial = serial

    def command(self, *parts: str, timeout: int = 60, check: bool = True) -> subprocess.CompletedProcess[str]:
        return run([self.adb, "-s", self.serial, *parts], timeout=timeout, check=check)

    def shell(self, command: str, *, timeout: int = 60, check: bool = True, secret: str | None = None) -> str:
        if secret is None:
            return self.command("shell", command, timeout=timeout, check=check).stdout
        completed = run(
            [self.adb, "-s", self.serial, "shell"],
            input_text=command.replace("{SECRET}", shlex.quote(secret)) + "\n",
            timeout=timeout,
            check=check,
        )
        return completed.stdout

    def property(self, name: str) -> str:
        return self.shell(f"getprop {shlex.quote(name)}").strip()

    def exec_out_bytes(self, *parts: str, timeout: int = 60) -> bytes:
        completed = subprocess.run(
            [self.adb, "-s", self.serial, "exec-out", *parts],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            check=False,
        )
        if completed.returncode != 0:
            raise HarnessFailure(f"adb exec-out failed ({completed.returncode}): {completed.stderr[-800:]!r}")
        return completed.stdout

    def ui_xml(self) -> str:
        self.shell(f"uiautomator dump {DEVICE_XML}", timeout=30)
        xml = self.shell(f"cat {DEVICE_XML}", timeout=15)
        self.shell(f"rm -f {DEVICE_XML}", timeout=15, check=False)
        start = xml.find("<?xml")
        if start < 0:
            raise HarnessFailure("uiautomator did not return XML")
        return xml[start:]

    def wait_xml(self, predicate, description: str, timeout: float = 30.0) -> str:
        deadline = time.monotonic() + timeout
        last = ""
        while time.monotonic() < deadline:
            last = self.ui_xml()
            if predicate(last):
                return last
            time.sleep(0.5)
        raise HarnessFailure(f"timed out waiting for {description}; last XML size={len(last)}")

    def tap_tag(self, tag: str, timeout: float = 20.0) -> None:
        x, y = self.tag_center(tag, timeout)
        self.shell(f"input tap {x} {y}")

    def double_tap_tag(self, tag: str, timeout: float = 20.0) -> None:
        x, y = self.tag_center(tag, timeout)
        self.shell(f"input tap {x} {y}")
        self.shell(f"input tap {x} {y}")

    def tag_center(self, tag: str, timeout: float = 20.0) -> tuple[int, int]:
        result = self._bounded_scroll_search(
            lambda root, node: node_matches_tag(root, node, tag),
            f"tag {tag}",
            timeout=timeout,
            wait_for_presence=True,
        )
        if result is None:
            raise HarnessFailure(f"tag {tag} could not be reached by bounded scrolling")
        _xml, _node, point = result
        return point

    def tag_exists(self, tag: str) -> bool:
        return has_tag(self.ui_xml(), tag)

    def wait_for_exact_text(self, text: str, description: str, timeout: float = 30.0) -> str:
        result = self._bounded_scroll_search(
            lambda _root, node: node.attrib.get("text") == text,
            description,
            timeout=timeout,
            wait_for_presence=True,
        )
        if result is None:
            raise HarnessFailure(f"exact text for {description} could not be reached by bounded scrolling")
        xml, _node, _point = result
        return xml

    def text_exists(self, text: str, timeout: float = 30.0) -> bool:
        return self._bounded_scroll_search(
            lambda _root, node: node.attrib.get("text") == text,
            f"exact text {text!r}",
            timeout=timeout,
            wait_for_presence=False,
        ) is not None

    def _bounded_scroll_search(
        self,
        matcher,
        description: str,
        *,
        timeout: float,
        wait_for_presence: bool,
    ) -> tuple[str, ET.Element, tuple[int, int]] | None:
        """Find one visible node, proving both scroll endpoints before absence.

        The search uses only scrollable nodes and their observed bounds. A match
        with zero-sized, off-screen, or scroll-clipped bounds is never returned.
        """
        deadline = time.monotonic() + timeout
        last_reason = "no hierarchy captured"
        unreachable_match_observed = False
        while True:
            for direction in ("down", "up"):
                unchanged = 0
                for _attempt in range(MAX_SCROLLS_PER_DIRECTION + 1):
                    if time.monotonic() >= deadline:
                        raise HarnessFailure(f"timed out while scrolling to {description}; {last_reason}")
                    xml = self.ui_xml()
                    root = ET.fromstring(xml)
                    visible = visible_matching_node(root, lambda node: matcher(root, node))
                    if visible is not None:
                        node, point = visible
                        return xml, node, point
                    unreachable_match_observed = unreachable_match_observed or any(
                        matcher(root, node) for node in root.iter("node")
                    )

                    viewport = select_scrollable_viewport(root)
                    if viewport is None:
                        break
                    before = hierarchy_signature(root)
                    x, start_y, end_y = scroll_gesture(viewport, direction)
                    self.shell(f"input swipe {x} {start_y} {x} {end_y} 300")
                    time.sleep(0.2)
                    after_xml = self.ui_xml()
                    after_root = ET.fromstring(after_xml)
                    visible = visible_matching_node(after_root, lambda node: matcher(after_root, node))
                    if visible is not None:
                        node, point = visible
                        return after_xml, node, point
                    unreachable_match_observed = unreachable_match_observed or any(
                        matcher(after_root, node) for node in after_root.iter("node")
                    )
                    after = hierarchy_signature(after_root)
                    unchanged = unchanged + 1 if after == before else 0
                    last_reason = f"last {direction} viewport={viewport}, unchanged={unchanged}"
                    if unchanged >= 2:
                        break
                else:
                    raise HarnessFailure(
                        f"bounded {direction} scroll limit reached while searching for {description}; {last_reason}"
                    )
            if not wait_for_presence:
                if unreachable_match_observed:
                    raise HarnessFailure(
                        f"matching node for {description} was observed only with zero, outside, or clipped bounds"
                    )
                return None
            if time.monotonic() >= deadline:
                raise HarnessFailure(f"timed out while scrolling to {description}; {last_reason}")
            time.sleep(0.5)

    def pid(self) -> int | None:
        pids = self.pids()
        if not pids:
            return None
        if len(pids) != 1:
            raise HarnessFailure(f"expected one ANC process, observed PIDs={pids}")
        return pids[0]

    def pids(self) -> list[int]:
        output = self.shell(f"pidof {PACKAGE}", check=False).strip().split()
        try:
            return sorted({int(value) for value in output})
        except ValueError as failure:
            raise HarnessFailure(f"pidof returned non-numeric output: {output!r}") from failure

    def run_as_uid(self) -> int:
        output = self.shell(f"run-as {PACKAGE} id -u").strip()
        if re.fullmatch(r"\d+", output) is None:
            raise HarnessFailure(f"run-as returned an invalid UID: {output!r}")
        return int(output)

    def process_uid_via_run_as(self, pid: int) -> int:
        status = self.shell(f"run-as {PACKAGE} cat /proc/{pid}/status")
        match = re.search(r"^Uid:\s+(\d+)(?:\s+\d+){3}\s*$", status, flags=re.MULTILINE)
        if match is None:
            raise HarnessFailure(f"run-as could not attribute PID {pid} to an actual UID")
        return int(match.group(1))

    def type_into_tag(self, tag: str, value: str) -> None:
        dismiss_visible_ime(self)
        result = self._bounded_scroll_search(
            lambda _root, node: node_is_tagged_app_edit_text(node, tag),
            f"enabled app EditText tag {tag}",
            timeout=20.0,
            wait_for_presence=True,
        )
        if result is None:
            raise HarnessFailure(f"editable tag {tag} could not be reached by bounded scrolling")
        _xml, _node, point = result
        self.shell(f"input tap {point[0]} {point[1]}")

        deadline = time.monotonic() + 5.0
        last_targets: list[ET.Element] = []
        while time.monotonic() < deadline:
            root = ET.fromstring(self.ui_xml())
            last_targets = [
                node for node in root.iter("node") if node_is_tagged_app_edit_text(node, tag)
            ]
            if len(last_targets) == 1 and last_targets[0].attrib.get("focused", "false").lower() == "true":
                break
            time.sleep(0.1)
        else:
            focused_ids = [
                node.attrib.get("resource-id", "")
                for node in root.iter("node")
                if (
                    node.attrib.get("package") == PACKAGE
                    and node.attrib.get("class") == "android.widget.EditText"
                    and node.attrib.get("focused", "false").lower() == "true"
                )
            ]
            raise HarnessFailure(
                f"refusing input for tag {tag}: expected exactly one enabled focused app EditText; "
                f"observed targets={len(last_targets)} focused EditTexts={focused_ids!r}"
            )

        escaped = value.replace("%", "\\%").replace(" ", "%s")
        self.shell(f"input text {shlex.quote(escaped)}")

        root = ET.fromstring(self.ui_xml())
        targets = [node for node in root.iter("node") if node_is_tagged_app_edit_text(node, tag)]
        if len(targets) != 1:
            raise HarnessFailure(
                f"input verification for tag {tag} expected one enabled app EditText, observed {len(targets)}"
            )
        actual = targets[0].attrib.get("text", "")
        if actual != value:
            raise HarnessFailure(
                f"input verification for tag {tag} failed: expected {value!r}, observed {actual!r}"
            )


def parse_bounds(bounds: str) -> tuple[int, int, int, int]:
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not match:
        raise HarnessFailure(f"invalid bounds: {bounds!r}")
    x1, y1, x2, y2 = map(int, match.groups())
    if x2 <= x1 or y2 <= y1:
        raise HarnessFailure(f"empty bounds: {bounds!r}")
    return x1, y1, x2, y2


def center(bounds: str) -> tuple[int, int]:
    x1, y1, x2, y2 = parse_bounds(bounds)
    point = (x1 + x2) // 2, (y1 + y2) // 2
    if point[0] <= 0 or point[1] <= 0:
        raise HarnessFailure(f"refusing zero/outside tap coordinates from bounds: {bounds!r}")
    return point


def point_in_bounds(point: tuple[int, int], bounds: tuple[int, int, int, int]) -> bool:
    x, y = point
    x1, y1, x2, y2 = bounds
    return x1 <= x < x2 and y1 <= y < y2


def node_has_tag(node: ET.Element, tag: str) -> bool:
    resource_id = node.attrib.get("resource-id", "")
    return resource_id == tag or resource_id.endswith(":id/" + tag)


def node_is_visible(node: ET.Element) -> bool:
    return node.attrib.get("visible-to-user", "true").lower() == "true"


def node_is_tagged_app_edit_text(node: ET.Element, tag: str) -> bool:
    return (
        node_has_tag(node, tag)
        and node.attrib.get("package") == PACKAGE
        and node.attrib.get("class") == "android.widget.EditText"
        and node.attrib.get("enabled", "false").lower() == "true"
    )


def visible_app_text(container: ET.Element, text: str, *, descendants_only: bool = False) -> bool:
    return any(
        candidate.attrib.get("text") == text
        and candidate.attrib.get("package") == PACKAGE
        and node_is_visible(candidate)
        for candidate in container.iter("node")
        if not descendants_only or candidate is not container
    )


def node_matches_dialog_fallback(root: ET.Element, node: ET.Element, tag: str) -> bool:
    text_pairs = DIALOG_TAG_TEXT_FALLBACKS.get(tag)
    if text_pairs is None:
        return False
    if (
        node.attrib.get("package") != PACKAGE
        or not node_is_visible(node)
        or node.attrib.get("enabled", "false").lower() != "true"
        or node.attrib.get("clickable", "false").lower() != "true"
    ):
        return False

    path = node_path(root, node) or []
    for title, action in text_pairs:
        if not visible_app_text(node, action, descendants_only=True):
            continue
        if any(
            ancestor.attrib.get("package") == PACKAGE
            and node_is_visible(ancestor)
            and visible_app_text(ancestor, title)
            for ancestor in path[:-1]
        ):
            return True
    return False


def node_matches_tag(root: ET.Element, node: ET.Element, tag: str) -> bool:
    tagged_nodes = tuple(candidate for candidate in root.iter("node") if node_has_tag(candidate, tag))
    if tagged_nodes:
        return node in tagged_nodes
    return node_matches_dialog_fallback(root, node, tag)


def observed_screen_bounds(root: ET.Element) -> tuple[int, int, int, int]:
    candidates = []
    for node in root.iter("node"):
        try:
            bounds = parse_bounds(node.attrib.get("bounds", ""))
        except HarnessFailure:
            continue
        area = (bounds[2] - bounds[0]) * (bounds[3] - bounds[1])
        candidates.append((area, bounds))
    if not candidates:
        raise HarnessFailure("UI hierarchy contained no positive observed bounds")
    return max(candidates, key=lambda item: item[0])[1]


def node_path(root: ET.Element, target: ET.Element) -> list[ET.Element] | None:
    def visit(node: ET.Element, path: list[ET.Element]) -> list[ET.Element] | None:
        next_path = path + [node]
        if node is target:
            return next_path
        for child in node:
            found = visit(child, next_path)
            if found is not None:
                return found
        return None

    return visit(root, [])


def visible_matching_node(root: ET.Element, matcher) -> tuple[ET.Element, tuple[int, int]] | None:
    screen = observed_screen_bounds(root)
    for node in root.iter("node"):
        if not matcher(node) or node.attrib.get("visible-to-user", "true").lower() == "false":
            continue
        try:
            point = center(node.attrib.get("bounds", ""))
        except HarnessFailure:
            continue
        if not point_in_bounds(point, screen):
            continue
        path = node_path(root, node) or []
        clipped = False
        for ancestor in path[:-1]:
            if ancestor.attrib.get("scrollable", "false").lower() != "true":
                continue
            try:
                viewport = parse_bounds(ancestor.attrib.get("bounds", ""))
            except HarnessFailure:
                clipped = True
                break
            if not point_in_bounds(point, viewport):
                clipped = True
                break
        if not clipped:
            return node, point
    return None


def select_scrollable_viewport(root: ET.Element) -> tuple[int, int, int, int] | None:
    screen = observed_screen_bounds(root)
    candidates = []
    for node in root.iter("node"):
        if node.attrib.get("scrollable", "false").lower() != "true":
            continue
        try:
            bounds = parse_bounds(node.attrib.get("bounds", ""))
        except HarnessFailure:
            continue
        if not point_in_bounds(center(node.attrib.get("bounds", "")), screen):
            continue
        width = bounds[2] - bounds[0]
        height = bounds[3] - bounds[1]
        if width < 32 or height < 96:
            continue
        candidates.append((width * height, bounds))
    return max(candidates, key=lambda item: item[0])[1] if candidates else None


def scroll_gesture(viewport: tuple[int, int, int, int], direction: str) -> tuple[int, int, int]:
    x1, y1, x2, y2 = viewport
    margin = max(8, min(64, (y2 - y1) // 8))
    x = (x1 + x2) // 2
    low = y1 + margin
    high = y2 - margin
    if x <= 0 or low <= 0 or high <= low or not point_in_bounds((x, low), viewport) or not point_in_bounds((x, high), viewport):
        raise HarnessFailure(f"refusing invalid scroll gesture inside observed viewport {viewport}")
    return (x, high, low) if direction == "down" else (x, low, high)


def hierarchy_signature(root: ET.Element) -> tuple[tuple[str, ...], ...]:
    return tuple(
        (
            node.attrib.get("resource-id", ""),
            node.attrib.get("text", ""),
            node.attrib.get("content-desc", ""),
            node.attrib.get("bounds", ""),
            node.attrib.get("scrollable", ""),
        )
        for node in root.iter("node")
    )


def has_tag(xml: str, tag: str) -> bool:
    pattern = rf'resource-id="(?:[^"]*:id/)?{re.escape(tag)}"'
    if re.search(pattern, xml) is not None:
        return True
    if tag not in DIALOG_TAG_TEXT_FALLBACKS:
        return False
    root = ET.fromstring(xml)
    return visible_matching_node(root, lambda node: node_matches_tag(root, node, tag)) is not None


def record(checks: dict, name: str, status: str, detail: object) -> None:
    checks[name] = {"status": status, "detail": detail}


def verify_disposable_emulator(device: Device, expected_avd: str, checks: dict) -> int:
    devices = run([device.adb, "devices", "-l"]).stdout
    serial_rows = []
    for line in devices.splitlines():
        fields = line.split()
        if len(fields) >= 2 and fields[0] == device.serial:
            serial_rows.append(fields)
    if len(serial_rows) != 1 or serial_rows[0][1] != "device":
        raise HarnessFailure(f"serial is not one online adb device: {device.serial}")
    if not device.serial.startswith("emulator-"):
        raise HarnessFailure(f"refusing non-emulator serial: {device.serial}")
    qemu = device.property("ro.kernel.qemu")
    if qemu != "1":
        raise HarnessFailure("refusing non-emulator target; ro.kernel.qemu is not 1")
    avd_name = device.property("ro.boot.qemu.avd_name")
    if not avd_name:
        avd_output = device.command("emu", "avd", "name", check=False).stdout.splitlines()
        avd_name = next((line.strip() for line in avd_output if line.strip() and line.strip() != "OK"), "")
    if avd_name != expected_avd:
        raise HarnessFailure(f"refusing unexpected AVD: expected={expected_avd!r} actual={avd_name!r}")
    api = int(device.property("ro.build.version.sdk"))
    if api != 36:
        raise HarnessFailure(f"refusing verification target API {api}; exact engineering target is API 36")
    record(
        checks,
        "emulator_guard",
        "PASS",
        {"serial": device.serial, "avd": avd_name, "api": api, "qemu": qemu},
    )
    return api


def parse_aapt2_xmltree(xmltree: str) -> list[dict]:
    roots: list[dict] = []
    stack: list[tuple[int, dict]] = []
    for raw_line in xmltree.splitlines():
        element_match = re.match(r"^(\s*)E:\s+([^\s(]+)", raw_line)
        if element_match:
            indent = len(element_match.group(1))
            node = {"name": element_match.group(2), "attributes": {}, "children": []}
            while stack and stack[-1][0] >= indent:
                stack.pop()
            if stack:
                stack[-1][1]["children"].append(node)
            else:
                roots.append(node)
            stack.append((indent, node))
            continue

        attribute_match = re.match(r"^(\s*)A:\s+(.+?)=(.*)$", raw_line)
        if attribute_match and stack:
            left = re.sub(r"\(0x[0-9a-fA-F]+\)$", "", attribute_match.group(2).strip())
            name = left.rsplit(":", 1)[-1]
            raw_value = attribute_match.group(3).strip()
            quoted = re.match(r'^"([^"]*)"', raw_value)
            value = quoted.group(1) if quoted else raw_value.split(None, 1)[0]
            stack[-1][1]["attributes"][name] = value
    return roots


def walk_aapt2_elements(nodes: list[dict]):
    for node in nodes:
        yield node
        yield from walk_aapt2_elements(node["children"])


def parse_aapt2_bool(value: str, attribute: str) -> bool:
    normalized = value.strip().lower()
    if normalized in {"true", "0xffffffff", "0x00000001", "0x1"}:
        return True
    if normalized in {"false", "0x00000000", "0x0"}:
        return False
    raise ValueError(f"unrecognized aapt2 boolean for android:{attribute}: {value!r}")


def component_name(node: dict) -> str:
    return node["attributes"].get("name", "<unnamed>")


def is_launcher_component(node: dict) -> bool:
    for child in node["children"]:
        if child["name"] != "intent-filter":
            continue
        actions = {
            descendant["attributes"].get("name")
            for descendant in walk_aapt2_elements(child["children"])
            if descendant["name"] == "action"
        }
        categories = {
            descendant["attributes"].get("name")
            for descendant in walk_aapt2_elements(child["children"])
            if descendant["name"] == "category"
        }
        if "android.intent.action.MAIN" in actions and "android.intent.category.LAUNCHER" in categories:
            return True
    return False


def compiled_manifest_policy(xmltree: str) -> dict:
    roots = parse_aapt2_xmltree(xmltree)
    elements = list(walk_aapt2_elements(roots))
    applications = [node for node in elements if node["name"] == "application"]
    if len(applications) != 1:
        raise ValueError(f"expected exactly one application element, found {len(applications)}")
    application = applications[0]

    exported_true = []
    exported_false = []
    for node in elements:
        if "exported" not in node["attributes"]:
            continue
        target = f"{node['name']}:{component_name(node)}"
        if parse_aapt2_bool(node["attributes"]["exported"], "exported"):
            exported_true.append(target)
        else:
            exported_false.append(target)

    component_types = {"activity", "activity-alias", "service", "provider", "receiver"}
    components = [node for node in elements if node["name"] in component_types]
    launchers = [node for node in components if is_launcher_component(node)]
    exported_launchers = [
        node
        for node in launchers
        if "exported" in node["attributes"]
        and parse_aapt2_bool(node["attributes"]["exported"], "exported")
    ]
    backup_value = application["attributes"].get("allowBackup")
    backup_disabled = backup_value is not None and not parse_aapt2_bool(backup_value, "allowBackup")
    extraction_rules_present = (
        "dataExtractionRules" in application["attributes"]
        and "fullBackupContent" in application["attributes"]
    )
    receivers = [node for node in elements if node["name"] == "receiver"]
    policy_ok = (
        len(exported_true) == 1
        and len(launchers) == 1
        and len(exported_launchers) == 1
        and len(receivers) == 0
        and backup_disabled
        and extraction_rules_present
    )
    return {
        "policy_ok": policy_ok,
        "exported_true_attributes": len(exported_true),
        "exported_true_components": exported_true,
        "exported_false_attributes": len(exported_false),
        "launcher_components": [component_name(node) for node in launchers],
        "exported_launcher_components": [component_name(node) for node in exported_launchers],
        "receiver_elements": len(receivers),
        "backup_disabled": backup_disabled,
        "extraction_rules_present": extraction_rules_present,
    }


def verify_apk(apk: Path, checks: dict) -> None:
    if not apk.is_file():
        raise HarnessFailure(f"APK missing: {apk}")
    aapt2 = sdk_tool("aapt2")
    apksigner = sdk_tool("apksigner")
    zipalign = sdk_tool("zipalign")
    if not aapt2 or not apksigner:
        raise HarnessFailure("installed SDK aapt2 and apksigner are required")
    badging = run([aapt2, "dump", "badging", str(apk)]).stdout
    match = re.search(r"package: name='([^']+)'", badging)
    actual = match.group(1) if match else None
    if actual != PACKAGE:
        raise HarnessFailure(f"APK package mismatch: {actual!r}")
    record(checks, "apk_attribution", "PASS", {"package": actual, "sha256": sha256_file(apk)})

    permissions = run([aapt2, "dump", "permissions", str(apk)]).stdout
    forbidden = [permission for permission in ("android.permission.INTERNET", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE") if permission in permissions]
    if forbidden or "android.permission.USE_BIOMETRIC" not in permissions:
        raise HarnessFailure(f"manifest permission policy failed: forbidden={forbidden}")
    record(checks, "manifest_permissions", "PASS", {"required": "USE_BIOMETRIC", "forbidden_found": forbidden})

    manifest = run([aapt2, "dump", "xmltree", str(apk), "--file", "AndroidManifest.xml"]).stdout
    try:
        manifest_detail = compiled_manifest_policy(manifest)
    except ValueError as failure:
        record(checks, "compiled_manifest", "FAIL", {"parse_error": str(failure)})
        raise HarnessFailure(f"compiled manifest could not be evaluated: {failure}") from failure
    record(
        checks,
        "compiled_manifest",
        "PASS" if manifest_detail["policy_ok"] else "FAIL",
        manifest_detail,
    )
    if not manifest_detail["policy_ok"]:
        raise HarnessFailure(f"compiled manifest policy failed: {manifest_detail}")

    signature = run([apksigner, "verify", "--verbose", "--print-certs", str(apk)]).stdout
    if "Verified" not in signature and "Verifies" not in signature:
        raise HarnessFailure("apksigner did not report verification")
    record(checks, "apk_signature", "PASS", signature[-1200:])

    if zipalign:
        alignment = run([zipalign, "-c", "-P", "16", "4", str(apk)], check=False)
        record(checks, "native_16k_zip_alignment", "PASS" if alignment.returncode == 0 else "FAIL", alignment.stdout[-1000:])
    else:
        record(checks, "native_16k_zip_alignment", "UNKNOWN", "zipalign unavailable")
    with zipfile.ZipFile(apk) as archive:
        native = sorted(name for name in archive.namelist() if name.startswith("lib/") and name.endswith(".so"))
    record(checks, "bundled_native_libraries", "PASS" if any(name.endswith("libsqlcipher.so") for name in native) else "FAIL", native)


def verify_schema(checks: dict) -> None:
    if not SCHEMA.is_file():
        record(checks, "compiler_schema_export", "UNKNOWN", "Room compiler schema 1.json missing; build must run before storage tests")
        raise HarnessFailure("compiler schema export is missing")
    data = json.loads(SCHEMA.read_text(encoding="utf-8"))
    entities = data.get("database", {}).get("entities", [])
    tables = {entity.get("tableName") for entity in entities}
    expected = {"patients", "visits", "vault_metadata", "save_receipts"}
    if tables != expected or data.get("database", {}).get("version") != 1:
        raise HarnessFailure(f"schema mismatch: version={data.get('database', {}).get('version')} tables={sorted(tables)}")
    expected_fields = {
        "patients": {"id", "case_id", "display_name", "date_of_birth", "phone"},
        "visits": {"id", "patient_id", "recorded_date", "blood_pressure", "weight", "findings", "notes", "revision", "is_synthetic_edit"},
        "vault_metadata": {"vault_uuid", "schema_version", "is_synthetic", "initialized"},
        "save_receipts": {"operation_uuid", "patient_id", "visit_id", "committed_revision", "fingerprint"},
    }
    nullable_fields = {
        "patients": {"date_of_birth", "phone"},
        "visits": {"blood_pressure", "weight", "findings", "notes"},
        "vault_metadata": set(),
        "save_receipts": set(),
    }
    expected_foreign_keys = {"patients": 0, "visits": 1, "vault_metadata": 0, "save_receipts": 2}
    expected_index_columns = {
        "patients": {("case_id",)},
        "visits": {("patient_id",)},
        "vault_metadata": set(),
        "save_receipts": {("patient_id",), ("visit_id",)},
    }
    expected_primary_keys = {
        "patients": ["id"],
        "visits": ["id"],
        "vault_metadata": ["vault_uuid"],
        "save_receipts": ["operation_uuid"],
    }
    for entity in entities:
        table = entity["tableName"]
        field_rows = entity.get("fields", [])
        fields = {field["columnName"] for field in field_rows}
        if fields != expected_fields[table]:
            raise HarnessFailure(f"schema columns mismatch for {table}: {sorted(fields)}")
        actual_nullable = {field["columnName"] for field in field_rows if not field.get("notNull", False)}
        if actual_nullable != nullable_fields[table]:
            raise HarnessFailure(f"schema nullability mismatch for {table}: {sorted(actual_nullable)}")
        if " DEFAULT " in entity.get("createSql", "").upper():
            raise HarnessFailure(f"unapproved SQL default in {table}")
        if entity.get("primaryKey", {}).get("columnNames") != expected_primary_keys[table]:
            raise HarnessFailure(f"primary-key mismatch for {table}")
        foreign_keys = entity.get("foreignKeys", [])
        if len(foreign_keys) != expected_foreign_keys[table]:
            raise HarnessFailure(f"foreign-key count mismatch for {table}: {len(foreign_keys)}")
        if any(key.get("onDelete") != "RESTRICT" or key.get("onUpdate") != "RESTRICT" for key in foreign_keys):
            raise HarnessFailure(f"foreign-key action mismatch for {table}")
        index_columns = {tuple(index.get("columnNames", [])) for index in entity.get("indices", [])}
        if index_columns != expected_index_columns[table]:
            raise HarnessFailure(f"index mismatch for {table}: {sorted(index_columns)}")
    record(checks, "compiler_schema_export", "PASS", {"sha256": sha256_file(SCHEMA), "tables": sorted(tables), "version": 1})


def install_and_attribute(device: Device, apk: Path, checks: dict) -> None:
    previous = device.shell(f"pm path {PACKAGE}", check=False).strip()
    install = device.command("install", "-r", str(apk), timeout=180, check=False)
    if install.returncode != 0 or "Success" not in install.stdout:
        raise HarnessFailure(f"adb install -r failed: {install.stdout[-1200:]}")
    installed = device.shell(f"pm path {PACKAGE}").strip()
    package_dump = device.shell(f"dumpsys package {PACKAGE}")
    if "package:" not in installed or PACKAGE not in package_dump:
        raise HarnessFailure("installed package attribution failed")
    record(checks, "installed_package_attribution", "PASS", {"previous_install_preserved": bool(previous), "pm_path": installed})


def trusted_credential_ui_evidence(xml: str) -> dict | None:
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        return None
    trusted_nodes = [
        node
        for node in root.iter("node")
        if node.attrib.get("package", "") in TRUSTED_CREDENTIAL_PACKAGES
    ]
    trusted_packages = sorted({node.attrib.get("package", "") for node in trusted_nodes})
    if not trusted_packages:
        return None

    markers = set()
    credential_resource_names = {
        "credential",
        "credentialentry",
        "credentialinput",
        "lockpassword",
        "lockpattern",
        "passwordentry",
        "patternview",
        "pinentry",
    }
    credential_text = re.compile(
        r"\b(?:confirm|enter|re-enter|verify|unlock|use)\b.{0,48}"
        r"\b(?:device credential|pin|password|pattern|screen lock)\b|"
        r"\b(?:device credential|pin|password|pattern|screen lock)\b.{0,32}"
        r"\b(?:required|to continue)\b",
        flags=re.IGNORECASE,
    )
    for node in trusted_nodes:
        resource_id = node.attrib.get("resource-id", "")
        resource_name = resource_id.rsplit("/", 1)[-1]
        normalized_resource = re.sub(r"[^a-z0-9]", "", resource_name.lower())
        if normalized_resource in credential_resource_names:
            markers.add(f"credential-resource:{resource_name}")
        if node.attrib.get("password", "false").lower() == "true":
            markers.add("credential-password-field")
        label = " ".join(
            value
            for value in (node.attrib.get("text", ""), node.attrib.get("content-desc", ""))
            if value
        )
        label_match = credential_text.search(label)
        if label_match:
            credential_kind = next(
                kind
                for kind in ("device credential", "screen lock", "password", "pattern", "pin")
                if kind in label_match.group(0).lower()
            )
            markers.add(f"credential-label:{credential_kind}")
    if not any(marker.startswith(("credential-resource:", "credential-label:")) for marker in markers):
        return None
    return {"trusted_packages": trusted_packages, "credential_markers": sorted(markers)}


def wait_for_trusted_credential_ui(device: Device, description: str, timeout: float = 20.0) -> tuple[str, dict]:
    xml = device.wait_xml(
        lambda value: trusted_credential_ui_evidence(value) is not None,
        description,
        timeout=timeout,
    )
    evidence = trusted_credential_ui_evidence(xml)
    if evidence is None:
        raise HarnessFailure("trusted Android credential UI disappeared before it could be attributed")
    return xml, evidence


def trusted_credential_cancel_control(xml: str) -> tuple[tuple[int, int], dict] | None:
    credential_evidence = trusted_credential_ui_evidence(xml)
    if credential_evidence is None:
        return None
    root = ET.fromstring(xml)
    match = visible_matching_node(
        root,
        lambda node: (
            node.attrib.get("package", "") in TRUSTED_CREDENTIAL_PACKAGES
            and node.attrib.get("resource-id", "") in TRUSTED_CREDENTIAL_CANCEL_RESOURCE_IDS
            and node.attrib.get("visible-to-user", "").lower() == "true"
            and node.attrib.get("enabled", "").lower() == "true"
            and node.attrib.get("clickable", "").lower() == "true"
        ),
    )
    if match is None:
        return None
    node, point = match
    return point, {
        "package": node.attrib.get("package", ""),
        "resource_id": node.attrib.get("resource-id", ""),
        "text": node.attrib.get("text", ""),
        "bounds": node.attrib.get("bounds", ""),
        "visible": True,
        "enabled": True,
        "clickable": True,
        "bounded_tap": list(point),
    }


def authenticate_system_prompt(
    device: Device,
    pin: str,
    checks: dict,
    challenge_index: int,
    *,
    success_predicate,
    success_description: str,
    authoritative_state: str,
) -> dict:
    _credential_xml, credential_evidence = wait_for_trusted_credential_ui(
        device,
        "trusted Android credential UI",
        timeout=20,
    )
    device.shell("input text {SECRET}", secret=pin)
    device.shell("input keyevent 66")
    device.wait_xml(success_predicate, success_description, timeout=60)
    detail = {
        "trusted_credential_ui": credential_evidence,
        "authoritative_app_state": authoritative_state,
    }
    record(checks, f"os_credential_challenge_{challenge_index}", "PASS", detail)
    return detail


def parse_locksettings_verify_output(output: str) -> str:
    normalized = " ".join(output.strip().split()).lower()
    if re.search(r"\bunknown command\b|\bunsupported\b|\bunrecognized (?:command|option)\b", normalized):
        return "unsupported"
    if re.search(
        r"\bnot verified\b|\bdid(?:n't| not) match\b|\bincorrect\b|\binvalid\b|"
        r"\bverification failed\b|\bverify failed\b",
        normalized,
    ):
        return "failed"
    if re.search(r"\bcredential verified successfully\b|\bverified successfully\b|^verified$|^success$", normalized):
        return "verified"
    return "unknown"


def authoritative_credential_challenge(result: dict) -> bool:
    detail = result.get("detail", {})
    trusted_ui = detail.get("trusted_credential_ui", {}) if isinstance(detail, dict) else {}
    trusted_packages = trusted_ui.get("trusted_packages", []) if isinstance(trusted_ui, dict) else []
    credential_markers = trusted_ui.get("credential_markers", []) if isinstance(trusted_ui, dict) else []
    return (
        result.get("status") == "PASS"
        and isinstance(trusted_packages, list)
        and bool(TRUSTED_CREDENTIAL_PACKAGES.intersection(trusted_packages))
        and isinstance(credential_markers, list)
        and any(
            isinstance(marker, str) and marker.startswith(("credential-resource:", "credential-label:"))
            for marker in credential_markers
        )
        and detail.get("authoritative_app_state") in {"unlockStorage", "patientSearch"}
    )


def current_package(xml: str) -> set[str]:
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        return set()
    return {node.attrib.get("package", "") for node in root.iter("node")}


def exact_text_count(xml: str, text: str) -> int:
    root = ET.fromstring(xml)
    return sum(1 for node in root.iter("node") if node.attrib.get("text") == text)


def ime_is_visible(device: Device) -> bool:
    output = device.shell("dumpsys input_method")
    for field in ("mInputShown", "mIsInputViewShown"):
        matches = re.findall(rf"\b{field}=(true|false)\b", output, flags=re.IGNORECASE)
        if matches:
            return matches[-1].lower() == "true"
    window_visibility = re.findall(r"\bmImeWindowVis=(0x[0-9a-fA-F]+|\d+)\b", output)
    if window_visibility:
        return bool(int(window_visibility[-1], 0) & 0x2)
    raise HarnessFailure("could not verify IME visibility from dumpsys input_method")


def observed_display_rotation(device: Device) -> tuple[str, str]:
    input_state = device.shell("dumpsys input")
    surface_matches = re.findall(r"\bSurfaceOrientation:\s*([0-3])\b", input_state)
    if surface_matches:
        return surface_matches[-1], "dumpsys input SurfaceOrientation"

    window_state = device.shell("dumpsys window displays")
    window_matches = re.findall(
        r"\b(?:mCurrentRotation|mRotation|rotation)=(?:ROTATION_)?(0|1|2|3|90|180|270)\b",
        window_state,
    )
    if window_matches:
        normalized = {"0": "0", "90": "1", "180": "2", "270": "3"}
        return normalized.get(window_matches[0], window_matches[0]), "dumpsys window displays rotation"
    raise HarnessFailure("could not observe the actual display rotation from dumpsys")


def dismiss_visible_ime(device: Device, timeout: float = 10.0) -> dict:
    initially_visible = ime_is_visible(device)
    dismissal = "already hidden"
    fallback = None
    if initially_visible:
        device.shell("input keyevent KEYCODE_ESCAPE")
        dismissal = "KEYCODE_ESCAPE"
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if not ime_is_visible(device):
                break
            time.sleep(0.25)
        else:
            if ime_is_visible(device):
                device.shell("input keyevent KEYCODE_BACK")
                dismissal = "KEYCODE_BACK"
                fallback = "one KEYCODE_BACK after KEYCODE_ESCAPE timeout and verified visible IME"
                deadline = time.monotonic() + timeout
                while time.monotonic() < deadline:
                    if not ime_is_visible(device):
                        break
                    time.sleep(0.25)
                else:
                    raise HarnessFailure("IME did not dismiss after guarded KEYCODE_BACK fallback")
    if ime_is_visible(device):
        raise HarnessFailure("IME remained visible after dismissal verification")
    return {
        "initially_visible": initially_visible,
        "dismissal": dismissal,
        "fallback": fallback,
        "verified_hidden": True,
    }


def dismiss_ime_without_navigation(device: Device, editor_tag: str, timeout: float = 10.0) -> dict:
    detail = dismiss_visible_ime(device, timeout=timeout)
    device.tag_center(editor_tag, timeout=timeout)
    return detail | {
        "retained_editor_marker": editor_tag,
        "editor_verification": "bounded visible tag search with scroll endpoint proof",
    }


def cancel_trusted_credential_prompt(device: Device, timeout: float = 30.0) -> dict:
    credential_xml, credential_evidence = wait_for_trusted_credential_ui(
        device,
        "trusted Android credential UI for cancellation",
        timeout=20,
    )
    explicit_cancel = trusted_credential_cancel_control(credential_xml)
    if explicit_cancel is not None:
        point, control = explicit_cancel
        device.shell(f"input tap {point[0]} {point[1]}")
        detail = {
            "mechanism": "explicit observed trusted OS cancel control",
            "trusted_credential_ui": credential_evidence,
            "cancel_control": control,
        }
    else:
        ime_detail = dismiss_visible_ime(device, timeout=10.0)
        _guarded_xml, guarded_evidence = wait_for_trusted_credential_ui(
            device,
            "trusted Android credential UI after verified IME dismissal",
            timeout=10,
        )
        device.shell("input keyevent 4")
        detail = {
            "mechanism": "verified IME dismissal then one guarded Back",
            "trusted_credential_ui": credential_evidence,
            "post_ime_trusted_credential_ui": guarded_evidence,
            "ime": ime_detail,
            "back_presses": 1,
        }
    device.wait_xml(
        lambda value: (
            PACKAGE in current_package(value)
            and has_tag(value, "unlockStorage")
            and not has_tag(value, "patientSearch")
        ),
        "locked app state after credential cancellation",
        timeout=timeout,
    )
    return detail | {
        "authoritative_app_package": PACKAGE,
        "authoritative_app_state": "unlockStorage",
        "pin_entered_for_cancellation": False,
    }


def canonical_vault_files(device: Device) -> tuple[str, list[str]]:
    absent_marker = "__P2_CANONICAL_DIRECTORY_ABSENT__"
    present_marker = "__P2_CANONICAL_DIRECTORY_PRESENT__"
    script = (
        f"target={shlex.quote(CANONICAL_VAULT_DIRECTORY)}; "
        f'if [ ! -e "$target" ]; then printf "%s\\n" {absent_marker}; exit 0; fi; '
        'if [ ! -d "$target" ]; then printf "canonical vault path is not a directory\\n" >&2; exit 42; fi; '
        f'printf "%s\\n" {present_marker}; '
        'find "$target" -maxdepth 3 -type f -print'
    )
    output = device.shell(f"run-as {PACKAGE} sh -c {shlex.quote(script)}")
    lines = output.splitlines()
    if lines == [absent_marker]:
        return "absent", []
    if lines and lines[0] == present_marker:
        return "directory", sorted(line for line in lines[1:] if line)
    raise HarnessFailure(f"unexpected canonical vault filesystem result: {output[-800:]!r}")


def recreate_after_process_death(
    device: Device,
    checks: dict,
    evidence_name: str,
    *,
    allow_background_sigkill: bool,
) -> None:
    old_pid = device.pid()
    if old_pid is None:
        raise HarnessFailure("ANC process PID was absent before process-death proof")
    old_uid = device.run_as_uid()
    if device.process_uid_via_run_as(old_pid) != old_uid:
        raise HarnessFailure(f"ANC PID {old_pid} did not belong to the run-as UID {old_uid}")
    android_user_id = old_uid // 100000
    if android_user_id != 0:
        raise HarnessFailure(
            f"refusing am kill --user 0 because run-as UID {old_uid} belongs to Android user {android_user_id}"
        )
    device.shell("input keyevent 3")
    home_xml = device.wait_xml(lambda xml: PACKAGE not in current_package(xml), "HOME before process death", timeout=20)
    foreground_packages = sorted(package for package in current_package(home_xml) if package)
    if not foreground_packages or PACKAGE in foreground_packages:
        raise HarnessFailure(f"HOME did not expose a verified non-app foreground: {foreground_packages}")
    device.shell(f"am kill --user 0 {PACKAGE}")
    deadline = time.monotonic() + 15
    remaining_pids = device.pids()
    while time.monotonic() < deadline and remaining_pids:
        time.sleep(0.25)
        remaining_pids = device.pids()

    method = "HOME -> am kill --user 0 -> non-waiting start"
    fallback_detail = None
    if remaining_pids:
        if not allow_background_sigkill:
            raise HarnessFailure(
                f"am kill --user 0 did not remove the background ANC process; remaining PIDs={remaining_pids}; "
                "guarded --allow-background-sigkill was not enabled"
            )
        if remaining_pids != [old_pid]:
            raise HarnessFailure(
                f"refusing stale/new PID kill: expected unchanged PID {old_pid}, observed {remaining_pids}"
            )
        fresh_home_xml = device.ui_xml()
        fresh_foreground = sorted(package for package in current_package(fresh_home_xml) if package)
        if not fresh_foreground or PACKAGE in fresh_foreground:
            raise HarnessFailure(
                f"refusing foreground SIGKILL; current visible packages={fresh_foreground}"
            )
        current_uid = device.run_as_uid()
        process_uid = device.process_uid_via_run_as(old_pid)
        if current_uid != old_uid or process_uid != old_uid:
            raise HarnessFailure(
                f"refusing PID/UID mismatch before SIGKILL: old_uid={old_uid}, "
                f"run_as_uid={current_uid}, process_uid={process_uid}"
            )
        if device.pids() != [old_pid]:
            raise HarnessFailure("refusing SIGKILL because the app PID changed during final attribution")
        device.shell(f"run-as {PACKAGE} kill -9 {old_pid}")
        deadline = time.monotonic() + 10
        remaining_pids = device.pids()
        while time.monotonic() < deadline and remaining_pids:
            time.sleep(0.25)
            remaining_pids = device.pids()
        if remaining_pids:
            raise HarnessFailure(f"guarded run-as SIGKILL did not remove PID {old_pid}: {remaining_pids}")
        method = (
            "HOME -> am kill --user 0 (ineffective) -> guarded run-as kill -9 exact PID "
            "-> non-waiting start"
        )
        fallback_detail = {
            "enabled": True,
            "used": True,
            "killed_pid": old_pid,
            "verified_uid": old_uid,
            "verified_non_app_foreground_packages": fresh_foreground,
        }

    if device.pids():
        raise HarnessFailure("old ANC process was not absent before restart")
    start_output = device.shell(f"am start -n {ACTIVITY}", timeout=30, check=False)
    deadline = time.monotonic() + 15
    new_pid = device.pid()
    while time.monotonic() < deadline and (new_pid is None or new_pid == old_pid):
        time.sleep(0.25)
        new_pid = device.pid()
    if new_pid is None or new_pid == old_pid:
        raise HarnessFailure(f"process recreation PID proof failed: old={old_pid} new={new_pid}")
    new_uid = device.process_uid_via_run_as(new_pid)
    if new_uid != old_uid:
        raise HarnessFailure(
            f"recreated ANC PID {new_pid} UID mismatch: expected run-as UID {old_uid}, observed {new_uid}"
        )

    patient_surface_tags = ("patientSearch", "addVisit", "visitEditor", "profileScreen", "workspaceProfile")
    patient_data_markers = (SENTINEL_PREFIX, "UNSAVED", "DEMO-ANC-")
    locked_xml = device.wait_xml(
        lambda value: (
            PACKAGE in current_package(value)
            and has_tag(value, "unlockStorage")
            and not any(has_tag(value, tag) for tag in patient_surface_tags)
            and not any(marker in value for marker in patient_data_markers)
        ),
        "recreated locked gate with patient UI and data absent",
        timeout=30,
    )
    visible_patient_surface_tags = [tag for tag in patient_surface_tags if has_tag(locked_xml, tag)]
    visible_patient_data_markers = [marker for marker in patient_data_markers if marker in locked_xml]
    if (
        PACKAGE not in current_package(locked_xml)
        or not has_tag(locked_xml, "unlockStorage")
        or visible_patient_surface_tags
        or visible_patient_data_markers
    ):
        raise HarnessFailure(
            "recreated process did not expose only the locked gate: "
            f"patient_surface_tags={visible_patient_surface_tags}, "
            f"patient_data_markers={visible_patient_data_markers}"
        )
    record(
        checks,
        evidence_name,
        "PASS",
        {
            "method": method,
            "old_pid": old_pid,
            "old_pid_absent_before_start": True,
            "new_pid": new_pid,
            "new_pid_distinct": True,
            "verified_uid": old_uid,
            "recreated_pid_verified_uid": new_uid,
            "verified_android_user": android_user_id,
            "verified_non_app_foreground_packages": foreground_packages,
            "start": {
                "command": f"am start -n {ACTIVITY}",
                "wait_for_launch_completion": False,
                "command_output_tail": start_output[-800:],
                "command_result_used_as_proof": False,
            },
            "ui_proof": {
                "authoritative_app_package": PACKAGE,
                "locked_gate": "unlockStorage",
                "patient_surface_tags_absent": list(patient_surface_tags),
                "patient_data_markers_absent": list(patient_data_markers),
            },
            "fallback": fallback_detail or {"enabled": allow_background_sigkill, "used": False},
        },
    )


def unlock_or_initialize(device: Device, pin: str, checks: dict, counter: list[int], api: int) -> None:
    xml = device.wait_xml(
        lambda value: any(has_tag(value, tag) for tag in ("initializeStorage", "unlockStorage", "patientSearch", "retryStorage")),
        "vault gate",
        timeout=30,
    )
    if has_tag(xml, "retryStorage"):
        raise HarnessFailure("app reports blocked storage; canonical vault was preserved")
    if has_tag(xml, "initializeStorage"):
        device.tap_tag("initializeStorage")
        device.wait_xml(
            lambda value: (
                has_tag(value, "vaultGate") and not has_tag(value, "initializeStorage")
            ) or trusted_credential_ui_evidence(value) is not None,
            "initialization gate transition or trusted Android credential UI",
        )
        counter[0] += 1
        authenticate_system_prompt(
            device,
            pin,
            checks,
            counter[0],
            success_predicate=lambda value: has_tag(value, "unlockStorage"),
            success_description="locked state after atomic initialization",
            authoritative_state="unlockStorage",
        )
    if device.tag_exists("unlockStorage"):
        if "os_credential_cancel" not in checks:
            device.tap_tag("unlockStorage")
            cancel_detail = cancel_trusted_credential_prompt(device)
            record(checks, "os_credential_cancel", "PASS", cancel_detail)
        device.tap_tag("unlockStorage")
        counter[0] += 1
        challenge_detail = authenticate_system_prompt(
            device,
            pin,
            checks,
            counter[0],
            success_predicate=lambda value: has_tag(value, "patientSearch"),
            success_description="unlocked patient list after credential",
            authoritative_state="patientSearch",
        )
        if 26 <= api <= 29:
            record(
                checks,
                "os_credential_prerequisite",
                "PASS",
                {
                    "api": api,
                    "method": "trusted real OS credential challenge",
                    "locksettings_verify_issued": False,
                    "authoritative_app_state": "patientSearch",
                    "trusted_credential_ui": challenge_detail["trusted_credential_ui"],
                },
            )
    device.wait_xml(lambda value: has_tag(value, "patientSearch"), "unlocked patient list", timeout=60)


def verify_ui_persistence(device: Device, pin: str, checks: dict, api: int, *, allow_background_sigkill: bool) -> str:
    challenges = [0]
    device.shell(f"am start -W -n {ACTIVITY}", timeout=30)
    unlock_or_initialize(device, pin, checks, challenges, api)

    device.tap_tag("workspace-DEMO-ANC-002")
    device.tap_tag("addVisit")
    device.type_into_tag("visitDate", "2026-10-11")
    dirty_back_ime = dismiss_ime_without_navigation(device, "visitEditor")
    device.shell("input keyevent 4")
    device.wait_xml(lambda xml: has_tag(xml, "keepEditingDialog"), "dirty-back confirmation", timeout=20)
    device.tap_tag("keepEditingDialog")
    device.wait_xml(lambda xml: has_tag(xml, "visitEditor") and "2026-10-11" in xml, "retained draft after Keep editing", timeout=20)
    device.tap_tag("cancelVisit")
    device.tap_tag("discardDialog")
    device.wait_xml(lambda xml: has_tag(xml, "addVisit"), "Workspace after explicit discard", timeout=20)
    if device.text_exists("2026-10-11"):
        raise HarnessFailure("discarded synthetic draft appeared as saved")
    record(
        checks,
        "p1_dirty_back_keep_discard",
        "PASS",
        {
            "mechanism": "draft retained until explicit discard and never partially saved",
            "ime_dismissal": dirty_back_ime,
        },
    )
    device.shell("input keyevent 4")
    device.wait_xml(lambda xml: has_tag(xml, "patientSearch"), "patient list after P1 discard proof", timeout=20)

    sentinel = SENTINEL_PREFIX + uuid.uuid4().hex[:12].upper()

    device.tap_tag("workspace-DEMO-ANC-001")
    device.tap_tag("addVisit")
    device.type_into_tag("visitDate", "2026-10-09")
    device.type_into_tag("visitNotes", sentinel)
    committed_visit_ime = dismiss_ime_without_navigation(device, "visitEditor")
    device.double_tap_tag("saveVisit")
    workspace_xml = device.wait_for_exact_text(sentinel, "committed visit in Workspace", timeout=45)
    if exact_text_count(workspace_xml, sentinel) != 1:
        raise HarnessFailure("rapid duplicate save produced more than one committed visit")
    record(
        checks,
        "p1_duplicate_save_prevention",
        "PASS",
        {
            "mechanism": "rapid double activation produced one committed visit",
            "ime_dismissal": committed_visit_ime,
        },
    )
    device.tap_tag("workspaceProfile")
    device.tag_center("profileScreen", timeout=20)
    device.wait_for_exact_text(sentinel, "same committed visit in Profile", timeout=30)
    record(checks, "workspace_profile_same_source", "PASS", {"sentinel": sentinel})

    device.shell("input keyevent 4")
    device.wait_xml(lambda xml: has_tag(xml, "patientSearch"), "patient list before isolation check", timeout=20)
    device.tap_tag("workspace-DEMO-ANC-002")
    device.wait_xml(lambda xml: has_tag(xml, "addVisit"), "other patient Workspace", timeout=20)
    if device.text_exists(sentinel):
        raise HarnessFailure("committed visit leaked into another patient Workspace")
    record(checks, "p1_cross_patient_isolation", "PASS", "patient 1 sentinel absent from patient 2 Workspace")
    device.shell("input keyevent 4")
    device.wait_xml(lambda xml: has_tag(xml, "patientSearch"), "patient list after isolation check", timeout=20)

    device.shell("input keyevent 3")
    time.sleep(1)
    device.shell(f"am start -W -n {ACTIVITY}", timeout=30)
    background_xml = device.wait_xml(lambda xml: has_tag(xml, "unlockStorage"), "locked gate after background", timeout=20)
    if sentinel in background_xml or has_tag(background_xml, "patientSearch"):
        raise HarnessFailure("patient data remained exposed after HOME/background lock")
    record(checks, "background_lock", "PASS", "patient UI hidden on HOME before process death")
    unlock_or_initialize(device, pin, checks, challenges, api)
    device.tap_tag("lockStorage")
    manual_lock_xml = device.wait_xml(lambda xml: has_tag(xml, "unlockStorage"), "locked gate after manual lock", timeout=20)
    if sentinel in manual_lock_xml or has_tag(manual_lock_xml, "patientSearch"):
        raise HarnessFailure("patient data remained exposed after manual lock")
    record(checks, "manual_lock", "PASS", "manual lock hid patient UI")
    unlock_or_initialize(device, pin, checks, challenges, api)

    recreate_after_process_death(
        device,
        checks,
        "acknowledged_save_process_recreation",
        allow_background_sigkill=allow_background_sigkill,
    )
    unlock_or_initialize(device, pin, checks, challenges, api)
    device.tap_tag("workspace-DEMO-ANC-001")
    device.wait_for_exact_text(sentinel, "acknowledged save after process death", timeout=30)
    record(checks, "background_process_death_persistence", "PASS", {"sentinel": sentinel})

    device.tap_tag("addVisit")
    unsaved = "UNSAVED" + uuid.uuid4().hex[:12].upper()
    device.type_into_tag("visitDate", "2026-10-10")
    device.type_into_tag("visitNotes", unsaved)
    verify_rotation_retains_draft(
        device,
        unsaved,
        checks,
        patient_values=("Maya Example · DEMO-ANC-001",),
        visit_date="2026-10-10",
    )
    recreate_after_process_death(
        device,
        checks,
        "unsaved_draft_process_recreation",
        allow_background_sigkill=allow_background_sigkill,
    )
    unlock_or_initialize(device, pin, checks, challenges, api)
    device.tap_tag("workspace-DEMO-ANC-001")
    if device.text_exists(unsaved):
        raise HarnessFailure("unsaved draft survived process death")
    record(checks, "unsaved_draft_process_death", "PASS", "unsaved sentinel absent after restart")
    return sentinel


def verify_rotation_retains_draft(
    device: Device,
    unsaved: str,
    checks: dict,
    *,
    patient_values: tuple[str, ...],
    visit_date: str,
) -> None:
    original_auto = device.shell("settings get system accelerometer_rotation", check=False).strip() or "1"
    original_rotation = device.shell("settings get system user_rotation", check=False).strip() or "0"
    original_display_rotation, original_rotation_source = observed_display_rotation(device)
    expected_rotation = "1" if original_display_rotation != "1" else "3"
    try:
        device.shell("settings put system accelerometer_rotation 0")
        device.shell(f"settings put system user_rotation {expected_rotation}")
        deadline = time.monotonic() + 15
        configured_rotation = ""
        observed_rotation = ""
        rotation_source = ""
        while time.monotonic() < deadline:
            configured_rotation = device.shell("settings get system user_rotation", check=False).strip()
            observed_rotation, rotation_source = observed_display_rotation(device)
            if configured_rotation == expected_rotation and observed_rotation == expected_rotation:
                break
            time.sleep(0.25)
        if (
            configured_rotation != expected_rotation
            or observed_rotation != expected_rotation
            or observed_rotation == original_display_rotation
        ):
            raise HarnessFailure(
                "rotation change was not observed before draft checks: "
                f"original_display={original_display_rotation!r} expected={expected_rotation!r} "
                f"configured={configured_rotation!r} observed={observed_rotation!r}"
            )
        device.tag_center("visitEditor", timeout=30)
        device.wait_for_exact_text(unsaved, "rotation-retained memory-only draft", timeout=30)
        patient_result = device._bounded_scroll_search(
            lambda _root, node: node.attrib.get("text") in patient_values,
            "rotation-retained patient identity",
            timeout=30,
            wait_for_presence=True,
        )
        if patient_result is None:
            raise HarnessFailure("patient identity could not be reached after rotation")
        _patient_xml, patient_node, _patient_point = patient_result
        retained_patient_value = patient_node.attrib.get("text", "")
        device.wait_for_exact_text(visit_date, "rotation-retained visit date", timeout=30)
        record(
            checks,
            "p1_rotation_retains_unlocked_draft",
            "PASS",
            {
                "mechanism": "changed rotation then separate bounded scroll searches",
                "original_user_rotation": original_rotation,
                "original_display_rotation": original_display_rotation,
                "original_rotation_source": original_rotation_source,
                "expected_rotation": expected_rotation,
                "configured_rotation": configured_rotation,
                "observed_rotation": observed_rotation,
                "rotation_source": rotation_source,
                "editor_marker": "visitEditor",
                "exact_unsaved_input": unsaved,
                "retained_patient_value": retained_patient_value,
                "retained_visit_date": visit_date,
                "draft_scope": "Activity recreation only",
            },
        )
    finally:
        device.shell(f"settings put system user_rotation {shlex.quote(original_rotation)}", check=False)
        device.shell(f"settings put system accelerometer_rotation {shlex.quote(original_auto)}", check=False)


def verify_ciphertext(device: Device, sentinel: str, checks: dict) -> None:
    listing = device.shell(
        f"run-as {PACKAGE} find no_backup/p2-vault/canonical -maxdepth 3 -type f -print",
        check=False,
    )
    files = sorted(line.strip() for line in listing.splitlines() if line.strip())
    database_files = [path for path in files if path.endswith("records.db") or path.endswith("records.db-wal") or path.endswith("records.db-journal")]
    envelope_files = [path for path in files if path.endswith("passphrase-envelope.bin")]
    if not database_files or not envelope_files:
        raise HarnessFailure(f"expected ciphertext/envelope files missing: {files}")
    leaked = []
    hashes = {}
    for path in database_files + envelope_files:
        content = device.exec_out_bytes("run-as", PACKAGE, "cat", path, timeout=30)
        hashes[path] = hashlib.sha256(content).hexdigest()
        if sentinel.encode() in content or b"Maya Example" in content or b"DEMO-ANC-001" in content:
            leaked.append(path)
    if leaked:
        raise HarnessFailure(f"plaintext synthetic sentinel found at rest in {leaked}")
    record(checks, "ciphertext_sentinel_scan", "PASS", {"files": hashes, "plaintext_hits": leaked})

    sqlite_available = device.shell("command -v sqlite3", check=False).strip()
    if sqlite_available:
        output = device.shell(f"run-as {PACKAGE} sqlite3 {shlex.quote(database_files[0])} '.tables'", check=False)
        readable = any(table in output for table in ("patients", "visits", "vault_metadata"))
        record(checks, "stock_sqlite_rejection", "FAIL" if readable else "PASS", output[-500:])
        if readable:
            raise HarnessFailure("stock sqlite read encrypted schema")
    else:
        record(checks, "stock_sqlite_rejection", "UNKNOWN", "sqlite3 unavailable on emulator")


def xml_local_name(element: ET.Element) -> str:
    return element.tag.rsplit("}", 1)[-1]


def normalized_attribution_key(value: str) -> str:
    return re.sub(r"[^a-z0-9]", "", value.lower())


def report_attribution(root: ET.Element, serial: str, api: int) -> dict:
    serial_keys = {"serial", "deviceserial", "adbserial", "androidserial", "device"}
    api_keys = {"api", "apilevel", "sdk", "sdkint", "androidapi", "androidsdk", "platformapi"}
    entries = []
    for element in root.iter():
        if xml_local_name(element) == "property":
            name = element.attrib.get("name") or element.attrib.get("key")
            value = element.attrib.get("value")
            if name and value is not None:
                entries.append({"source": "property", "key": name, "value": value})
        for name, value in element.attrib.items():
            if normalized_attribution_key(name) in serial_keys | api_keys:
                entries.append({"source": xml_local_name(element), "key": name, "value": value})

    serial_pattern = re.compile(
        rf"(?:^|[^A-Za-z0-9_.-]){re.escape(serial)}(?:$|[^A-Za-z0-9_.-])"
    )
    serial_match = any(
        normalized_attribution_key(entry["key"]) in serial_keys
        and serial_pattern.search(entry["value"]) is not None
        for entry in entries
    )

    api_match = False
    for entry in entries:
        key = normalized_attribution_key(entry["key"])
        value = entry["value"].strip()
        if key in api_keys and re.fullmatch(rf"(?i)(?:api|sdk(?:\s*int)?|level)?\s*[:=_-]?\s*{api}", value):
            api_match = True
            break
        if key in serial_keys and re.search(rf"(?i)\b(?:api|sdk(?:\s*int)?|api\s*level)\s*[:=_-]?\s*{api}\b", value):
            api_match = True
            break
    return {"serial_match": serial_match, "api_match": api_match, "entries": entries}


def analyze_test_report(root: ET.Element) -> dict:
    suites = [element for element in root.iter() if xml_local_name(element) == "testsuite"]
    leaf_suites = [
        suite
        for suite in suites
        if not any(xml_local_name(child) == "testsuite" for child in suite.iter() if child is not suite)
    ]
    if not leaf_suites:
        raise ValueError("no leaf testsuite elements")

    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    cases: list[tuple[ET.Element, ET.Element]] = []
    for suite in leaf_suites:
        for counter in totals:
            raw = suite.attrib.get(counter)
            if raw is None or re.fullmatch(r"\d+", raw) is None:
                raise ValueError(f"testsuite missing numeric {counter}")
            totals[counter] += int(raw)
        cases.extend(
            (suite, case)
            for case in suite.iter()
            if xml_local_name(case) == "testcase"
        )

    totals["actual_testcases"] = len(cases)
    if totals["tests"] != totals["actual_testcases"]:
        raise ValueError(
            f"declared tests {totals['tests']} did not match testcase elements {totals['actual_testcases']}"
        )
    child_counts = {"failures": 0, "errors": 0, "skipped": 0}
    coverage = {name: 0 for name in REQUIRED_CONNECTED_TEST_CLASSES}
    for suite, case in cases:
        descendants = [xml_local_name(child) for child in case.iter() if child is not case]
        child_counts["failures"] += descendants.count("failure")
        child_counts["errors"] += descendants.count("error")
        child_counts["skipped"] += descendants.count("skipped")
        class_name = case.attrib.get("classname") or suite.attrib.get("name", "")
        short_name = class_name.rsplit(".", 1)[-1]
        if short_name in coverage:
            coverage[short_name] += 1
    totals["child_counts"] = child_counts
    totals["coverage"] = coverage
    return totals


def connected_command_text(command: object) -> str:
    if isinstance(command, str) and command.strip():
        return command.strip()
    if isinstance(command, list) and command and all(isinstance(part, str) and part for part in command):
        return shlex.join(command)
    raise ValueError("receipt command must be a non-empty string or list of non-empty strings")


def validate_connected_receipt_data(
    data: object,
    *,
    serial: str,
    api: int,
    avd: str,
    app_apk_sha256: str,
    test_apk_sha256: str,
    results_root: Path,
    xml_files: list[Path],
) -> dict:
    if not isinstance(data, dict):
        raise ValueError("connected receipt root must be a JSON object")
    required = {
        "selectedSerial",
        "api",
        "exactAvd",
        "appApkSha256",
        "testApkSha256",
        "start",
        "end",
        "command",
        "results",
    }
    missing = sorted(required - set(data))
    if missing:
        raise ValueError(f"connected receipt missing fields: {missing}")
    if data["selectedSerial"] != serial or data["api"] != api or data["exactAvd"] != avd:
        raise ValueError(
            "connected receipt target mismatch: "
            f"receipt={(data['selectedSerial'], data['api'], data['exactAvd'])!r} "
            f"selected={(serial, api, avd)!r}"
        )
    sha_pattern = re.compile(r"[0-9a-f]{64}")
    for field, expected in (
        ("appApkSha256", app_apk_sha256),
        ("testApkSha256", test_apk_sha256),
    ):
        value = data[field]
        if not isinstance(value, str) or sha_pattern.fullmatch(value) is None or value != expected:
            raise ValueError(f"connected receipt {field} did not match the current artifact")

    start = data["start"]
    end = data["end"]
    if (
        isinstance(start, bool)
        or isinstance(end, bool)
        or not isinstance(start, (int, float))
        or not isinstance(end, (int, float))
        or start <= 0
        or end < start
        or end > time.time() + 1
    ):
        raise ValueError(f"invalid connected receipt execution interval: start={start!r} end={end!r}")

    command = connected_command_text(data["command"])
    try:
        command_parts = shlex.split(command)
    except ValueError as failure:
        raise ValueError(f"receipt command could not be parsed: {failure}") from failure
    serial_bindings = [
        part.split("=", 1)[1]
        for part in command_parts
        if part.startswith("ANDROID_SERIAL=")
    ]
    serial_bindings.extend(
        command_parts[index + 1]
        for index, part in enumerate(command_parts[:-1])
        if part == "ANDROID_SERIAL"
    )
    connected_task = any("connectedDebugAndroidTest" in part for part in command_parts)
    if not connected_task or not serial_bindings or any(binding != serial for binding in serial_bindings):
        raise ValueError("receipt command was not an ANDROID_SERIAL-targeted connectedDebugAndroidTest invocation")

    result_rows = data["results"]
    if not isinstance(result_rows, list) or not result_rows:
        raise ValueError("connected receipt results must be a non-empty list")
    actual_by_relative = {path.relative_to(ROOT).as_posix(): path for path in xml_files}
    eligible: dict[str, str] = {}
    for row in result_rows:
        if not isinstance(row, dict) or set(row) != {"path", "sha256"}:
            raise ValueError("each connected receipt result must contain only path and sha256")
        path_value = row["path"]
        digest = row["sha256"]
        if not isinstance(path_value, str) or not path_value or Path(path_value).is_absolute():
            raise ValueError(f"invalid receipt result path: {path_value!r}")
        if not isinstance(digest, str) or sha_pattern.fullmatch(digest) is None:
            raise ValueError(f"invalid receipt result SHA-256 for {path_value!r}")
        relative = Path(path_value)
        if ".." in relative.parts or relative.as_posix() != path_value:
            raise ValueError(f"receipt result path is not canonical: {path_value!r}")
        candidate = (ROOT / relative).resolve()
        try:
            candidate.relative_to(results_root.resolve())
        except ValueError as failure:
            raise ValueError(f"receipt result escaped the connected-results directory: {path_value!r}") from failure
        actual = actual_by_relative.get(path_value)
        if actual is None or actual.resolve() != candidate:
            raise ValueError(f"receipt result is not a current discovered XML report: {path_value!r}")
        if path_value in eligible:
            raise ValueError(f"duplicate receipt result path: {path_value!r}")
        actual_digest = sha256_file(actual)
        if actual_digest != digest:
            raise ValueError(f"receipt result hash mismatch: {path_value!r}")
        modified = actual.stat().st_mtime
        if modified < start or modified > end + 1:
            raise ValueError(
                f"receipt result timestamp was outside its execution interval: "
                f"{path_value!r} mtime={modified} start={start} end={end}"
            )
        eligible[path_value] = digest
    return {
        "selected_serial": serial,
        "selected_api": api,
        "exact_avd": avd,
        "app_apk_sha256": app_apk_sha256,
        "test_apk_sha256": test_apk_sha256,
        "start": start,
        "end": end,
        "command": command,
        "eligible_results": eligible,
    }


def load_connected_receipt(
    path: Path,
    *,
    serial: str,
    api: int,
    avd: str,
    app_apk_sha256: str,
    test_apk_sha256: str,
    results_root: Path,
    xml_files: list[Path],
) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    detail = validate_connected_receipt_data(
        data,
        serial=serial,
        api=api,
        avd=avd,
        app_apk_sha256=app_apk_sha256,
        test_apk_sha256=test_apk_sha256,
        results_root=results_root,
        xml_files=xml_files,
    )
    detail["receipt_path"] = str(path)
    detail["receipt_sha256"] = sha256_file(path)
    return detail


def verify_test_evidence(device: Device, api: int, checks: dict, connected_receipt: Path | None) -> None:
    results = ROOT / "app" / "build" / "outputs" / "androidTest-results" / "connected" / "debug"
    xml_files = sorted(results.glob("**/*.xml")) if results.exists() else []
    if not xml_files:
        record(checks, "connected_failure_injection_tests", "UNKNOWN", "connectedDebugAndroidTest result XML not found")
        return

    apk_detail = checks.get("apk_attribution", {}).get("detail", {})
    app_apk_sha256 = apk_detail.get("sha256") if isinstance(apk_detail, dict) else None
    guard_detail = checks.get("emulator_guard", {}).get("detail", {})
    avd = guard_detail.get("avd") if isinstance(guard_detail, dict) else None
    test_apk_sha256 = sha256_file(TEST_APK) if TEST_APK.is_file() else None
    receipt_detail = None
    if connected_receipt is not None:
        try:
            if not isinstance(app_apk_sha256, str) or not isinstance(avd, str) or not isinstance(test_apk_sha256, str):
                raise ValueError("current app APK, test APK, or selected AVD attribution was unavailable")
            receipt_detail = load_connected_receipt(
                connected_receipt.resolve(),
                serial=device.serial,
                api=api,
                avd=avd,
                app_apk_sha256=app_apk_sha256,
                test_apk_sha256=test_apk_sha256,
                results_root=results,
                xml_files=xml_files,
            )
        except (OSError, TypeError, ValueError, json.JSONDecodeError) as failure:
            record(
                checks,
                "connected_failure_injection_tests",
                "UNKNOWN",
                {
                    "selected_serial": device.serial,
                    "selected_api": api,
                    "current_app_apk_sha256": app_apk_sha256,
                    "current_test_apk_sha256": test_apk_sha256,
                    "receipt_path": str(connected_receipt),
                    "receipt_error": str(failure),
                    "reason": "supplied connected receipt was not eligible",
                },
            )
            return

    reports = []
    accepted = []
    for path in xml_files:
        report = {"file": str(path.relative_to(ROOT)), "sha256": sha256_file(path)}
        try:
            root = ET.parse(path).getroot()
            attribution = report_attribution(root, device.serial, api)
            report["attribution"] = attribution
            relative = report["file"]
            receipt_bound = (
                receipt_detail is not None
                and receipt_detail["eligible_results"].get(relative) == report["sha256"]
            )
            report["receipt_bound"] = receipt_bound
            eligible = receipt_bound if connected_receipt is not None else (
                attribution["serial_match"] and attribution["api_match"]
            )
            if eligible:
                report["results"] = analyze_test_report(root)
                report["acceptance_method"] = "connected_receipt" if receipt_bound else "direct_xml_serial_and_api"
                accepted.append(report)
        except (ET.ParseError, OSError, ValueError) as failure:
            report["parse_error"] = str(failure)
        reports.append(report)

    detail = {
        "selected_serial": device.serial,
        "selected_api": api,
        "selected_avd": avd,
        "current_app_apk_sha256": app_apk_sha256,
        "current_test_apk_sha256": test_apk_sha256,
        "connected_receipt": receipt_detail,
        "reports": reports,
        "accepted_report_files": [report["file"] for report in accepted],
    }
    eligible_errors = [
        report["file"]
        for report in reports
        if (
            report.get("receipt_bound")
            if connected_receipt is not None
            else (
                report.get("attribution", {}).get("serial_match")
                and report.get("attribution", {}).get("api_match")
            )
        )
        and "parse_error" in report
    ]
    if eligible_errors:
        record(
            checks,
            "connected_failure_injection_tests",
            "UNKNOWN",
            detail | {"reason": "eligible XML reports could not be evaluated", "unreadable_reports": eligible_errors},
        )
        return
    if not accepted:
        record(
            checks,
            "connected_failure_injection_tests",
            "UNKNOWN",
            detail | {
                "reason": (
                    "no XML report was bound by the supplied eligible receipt"
                    if connected_receipt is not None
                    else "no XML report was attributable to both the selected serial and API"
                )
            },
        )
        return

    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    coverage = {name: 0 for name in REQUIRED_CONNECTED_TEST_CLASSES}
    child_counts = {"failures": 0, "errors": 0, "skipped": 0}
    for report in accepted:
        result = report["results"]
        for counter in totals:
            totals[counter] += result[counter]
        for counter in child_counts:
            child_counts[counter] += result["child_counts"][counter]
        for class_name in coverage:
            coverage[class_name] += result["coverage"][class_name]
    missing_coverage = [name for name, count in coverage.items() if count <= 0]
    violations = {
        "zero_tests": totals["tests"] <= 0,
        "missing_coverage": missing_coverage,
        "declared_failures": totals["failures"],
        "declared_errors": totals["errors"],
        "declared_skips": totals["skipped"],
        "testcase_failures": child_counts["failures"],
        "testcase_errors": child_counts["errors"],
        "testcase_skips": child_counts["skipped"],
    }
    passed = not any(
        (
            violations["zero_tests"],
            missing_coverage,
            totals["failures"],
            totals["errors"],
            totals["skipped"],
            child_counts["failures"],
            child_counts["errors"],
            child_counts["skipped"],
        )
    )
    record(
        checks,
        "connected_failure_injection_tests",
        "PASS" if passed else "FAIL",
        detail | {"totals": totals, "coverage": coverage, "violations": violations},
    )


def verify_key_security_measurement(device: Device, checks: dict) -> None:
    path = "cache/p2-key-security-measurement.json"
    raw = device.shell(f"run-as {PACKAGE} cat {path}", check=False).strip()
    if not raw:
        record(checks, "keystore_hardware_backing", "UNKNOWN", "instrumented key-security measurement was not found")
        return
    try:
        measurement = json.loads(raw)
    except json.JSONDecodeError as failure:
        raise HarnessFailure(f"invalid key-security measurement: {failure}") from failure
    if measurement.get("available") is not True or not isinstance(measurement.get("inside_secure_hardware"), bool):
        raise HarnessFailure(f"incomplete key-security measurement: {measurement}")
    record(
        checks,
        "keystore_hardware_backing",
        "PASS",
        {
            "measured": True,
            "inside_secure_hardware": measurement["inside_secure_hardware"],
            "security_level": measurement.get("security_level"),
            "api": measurement.get("api"),
            "promise": "none; observed capability only",
        },
    )


def verify_api36_qualification(current_api: int, current_checks: dict, checks: dict) -> None:
    apk_detail = current_checks.get("apk_attribution", {}).get("detail", {})
    current_apk_sha256 = apk_detail.get("sha256") if isinstance(apk_detail, dict) else None
    if not isinstance(current_apk_sha256, str) or not current_apk_sha256:
        record(checks, "required_api36_single_target_qualification", "UNKNOWN", "current APK SHA-256 attribution missing")
        return
    current_test_apk_sha256 = sha256_file(TEST_APK) if TEST_APK.is_file() else None
    if not isinstance(current_test_apk_sha256, str):
        record(checks, "required_api36_single_target_qualification", "UNKNOWN", "current test APK SHA-256 attribution missing")
        return
    covered = set()
    accepted_evidence = []
    if run_qualifies_for_api36_qualification(
        current_checks,
        current_apk_sha256,
        current_test_apk_sha256,
        current_api,
    ):
        covered.add(current_api)
        accepted_evidence.append({"source": "current run", "api": current_api})
    if EVIDENCE.exists():
        for path in EVIDENCE.glob("verification-api*.json"):
            try:
                previous = json.loads(path.read_text(encoding="utf-8"))
                previous_api = int(previous.get("api"))
                if run_qualifies_for_api36_qualification(
                    previous.get("checks", {}),
                    current_apk_sha256,
                    current_test_apk_sha256,
                    previous_api,
                ):
                    covered.add(previous_api)
                    accepted_evidence.append(
                        {
                            "source": str(path.relative_to(ROOT)),
                            "evidence_sha256": sha256_file(path),
                            "api": previous_api,
                        }
                    )
            except (OSError, ValueError, TypeError, json.JSONDecodeError):
                continue
    status = "PASS" if 36 in covered else "UNKNOWN"
    record(
        checks,
        "required_api36_single_target_qualification",
        status,
        {
            "apk_sha256": current_apk_sha256,
            "test_apk_sha256": current_test_apk_sha256,
            "exact_api_target": 36,
            "qualifying_api_targets": sorted(covered),
            "accepted_evidence": accepted_evidence,
        },
    )


def run_qualifies_for_api36_qualification(
    checks: dict,
    expected_apk_sha256: str,
    expected_test_apk_sha256: str,
    expected_api: int,
) -> bool:
    if expected_api != 36:
        return False
    apk_attribution = checks.get("apk_attribution", {})
    apk_detail = apk_attribution.get("detail", {})
    if (
        apk_attribution.get("status") != "PASS"
        or not isinstance(apk_detail, dict)
        or apk_detail.get("sha256") != expected_apk_sha256
    ):
        return False
    emulator_guard = checks.get("emulator_guard", {})
    guard_detail = emulator_guard.get("detail", {})
    if (
        emulator_guard.get("status") != "PASS"
        or not isinstance(guard_detail, dict)
        or guard_detail.get("api") != expected_api
    ):
        return False
    connected = checks.get("connected_failure_injection_tests", {})
    connected_detail = connected.get("detail", {})
    connected_receipt = connected_detail.get("connected_receipt", {}) if isinstance(connected_detail, dict) else {}
    if (
        connected.get("status") != "PASS"
        or not isinstance(connected_detail, dict)
        or connected_detail.get("current_app_apk_sha256") != expected_apk_sha256
        or connected_detail.get("current_test_apk_sha256") != expected_test_apk_sha256
        or not isinstance(connected_receipt, dict)
        or connected_receipt.get("app_apk_sha256") != expected_apk_sha256
        or connected_receipt.get("test_apk_sha256") != expected_test_apk_sha256
        or not connected_detail.get("accepted_report_files")
    ):
        return False
    if any(checks.get(name, {}).get("status") != "PASS" for name in API36_QUALIFICATION_REQUIRED_CHECKS):
        return False
    challenges = [
        result
        for name, result in checks.items()
        if name.startswith("os_credential_challenge_")
    ]
    return bool(challenges) and all(authoritative_credential_challenge(result) for result in challenges)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_evidence(serial: str, api: int | None, payload: dict) -> None:
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    safe_serial = re.sub(r"[^A-Za-z0-9_.-]", "_", serial)
    targets = [EVIDENCE / "verification.json"]
    if api is not None:
        targets.append(EVIDENCE / f"verification-api{api}-{safe_serial}.json")
    encoded = json.dumps(payload, indent=2, sort_keys=True, ensure_ascii=False) + "\n"
    for target in targets:
        temporary = target.with_suffix(target.suffix + ".tmp")
        temporary.write_text(encoded, encoding="utf-8")
        os.replace(temporary, target)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument(
        "--expected-avd",
        required=True,
        help="exact disposable AVD name; verified with the qemu guard before install or UI mutation",
    )
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument(
        "--allow-background-sigkill",
        action="store_true",
        help=(
            "disposable emulators only: if HOME + am kill leaves the exact attributed app PID alive, "
            "permit guarded run-as kill -9 of that PID; default remains am kill only"
        ),
    )
    parser.add_argument(
        "--connected-receipt",
        type=Path,
        help=(
            "optional root-recorded JSON binding connected XML results to selectedSerial/api/exactAvd, "
            "current appApkSha256/testApkSha256, numeric start/end, an ANDROID_SERIAL-targeted "
            "connectedDebugAndroidTest command, and results [{path, sha256}]"
        ),
    )
    parser.add_argument(
        "--expect-no-credential",
        action="store_true",
        help="on a fresh disposable emulator, prove initialization is blocked without creating vault files",
    )
    args = parser.parse_args()
    checks: dict[str, dict] = {}
    api: int | None = None
    payload = {
        "schema": "anc-native-p2-verification-v1",
        "serial": args.serial,
        "started_unix": int(time.time()),
        "checks": checks,
        "overall": "UNKNOWN",
    }
    try:
        adb = sdk_tool("adb")
        if not adb:
            raise HarnessFailure("adb unavailable")
        device = Device(adb, args.serial)
        api = verify_disposable_emulator(device, args.expected_avd, checks)
        payload["api"] = api
        record(checks, "required_api_target", "PASS", {"api": api, "exact_engineering_target": 36})
        verify_apk(args.apk.resolve(), checks)
        verify_schema(checks)
        install_and_attribute(device, args.apk.resolve(), checks)

        if args.expect_no_credential:
            device.shell(f"am start -W -n {ACTIVITY}", timeout=30)
            device.wait_xml(lambda value: has_tag(value, "initializeStorage"), "fresh uninitialized gate", timeout=30)
            device.tap_tag("initializeStorage")
            device.wait_xml(lambda value: has_tag(value, "retryStorage"), "blocked no-credential state", timeout=20)
            canonical_state, artifacts = canonical_vault_files(device)
            if artifacts:
                record(
                    checks,
                    "no_credential_blocks_creation",
                    "FAIL",
                    {"canonical_state": canonical_state, "files": artifacts},
                )
                raise HarnessFailure("vault files were created without a device credential")
            record(
                checks,
                "no_credential_blocks_creation",
                "PASS",
                {"canonical_state": canonical_state, "files": [], "blocked_ui_observed": True},
            )
            payload["overall"] = "PASS"
            payload["scope"] = "no-credential prerequisite only"
            return 0

        pin = os.environ.get("ANC_P2_EMULATOR_PIN")
        if not pin and sys.stdin.isatty():
            pin = getpass.getpass("Disposable emulator device PIN (not recorded): ")
        if not pin:
            record(checks, "os_credential_prerequisite", "UNKNOWN", "set ANC_P2_EMULATOR_PIN or run interactively; device credential is never configured by this script")
            raise HarnessFailure("real OS credential challenge prerequisite missing")
        if 26 <= api <= 29:
            record(
                checks,
                "os_credential_prerequisite",
                "UNKNOWN",
                {
                    "api": api,
                    "state": "pending trusted real OS credential challenge and authoritative unlocked app state",
                    "locksettings_verify_issued": False,
                },
            )
        else:
            verification = device.shell("locksettings verify --old {SECRET}", secret=pin, check=False)
            verification_state = parse_locksettings_verify_output(verification)
            if verification_state != "verified":
                record(
                    checks,
                    "os_credential_prerequisite",
                    "UNKNOWN",
                    {"api": api, "locksettings_verify": verification_state},
                )
                raise HarnessFailure("emulator credential verification failed")
            record(
                checks,
                "os_credential_prerequisite",
                "PASS",
                {"api": api, "method": "locksettings verify", "locksettings_verify": verification_state},
            )

        sentinel = verify_ui_persistence(
            device,
            pin,
            checks,
            api,
            allow_background_sigkill=args.allow_background_sigkill,
        )
        verify_ciphertext(device, sentinel, checks)
        verify_test_evidence(device, api, checks, args.connected_receipt)
        verify_key_security_measurement(device, checks)
        verify_api36_qualification(api, checks, checks)
        record(
            checks,
            "visual_configuration_matrix",
            "UNKNOWN",
            "small phone, large font, landscape, tablet, dark, and Arabic/RTL review requires external hierarchy/visual evidence",
        )
        record(checks, "protected_pixel_capture", "UNKNOWN", "optional; FLAG_SECURE remained enabled and was not bypassed")
        record(checks, "native_16k_runtime", "UNKNOWN", "optional unless this serial is a confirmed 16 KB-page emulator")
        mandatory_failures = [name for name, result in checks.items() if result["status"] == "FAIL"]
        mandatory_unknown = [
            name for name, result in checks.items()
            if result["status"] == "UNKNOWN" and name not in {"protected_pixel_capture", "native_16k_runtime", "stock_sqlite_rejection"}
        ]
        payload["overall"] = "PASS" if not mandatory_failures and not mandatory_unknown else "UNKNOWN"
        payload["mandatory_failures"] = mandatory_failures
        payload["mandatory_unknown"] = mandatory_unknown
        return 0 if payload["overall"] == "PASS" else 2
    except (HarnessFailure, subprocess.TimeoutExpired, ET.ParseError, OSError, ValueError) as failure:
        payload["error"] = str(failure)
        payload["overall"] = "UNKNOWN" if not any(value["status"] == "FAIL" for value in checks.values()) else "FAIL"
        return 2
    finally:
        payload["finished_unix"] = int(time.time())
        write_evidence(args.serial, api, payload)
        print(json.dumps(payload, indent=2, sort_keys=True, ensure_ascii=False))


if __name__ == "__main__":
    raise SystemExit(main())
