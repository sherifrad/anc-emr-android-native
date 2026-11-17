Review complete. Verdict: **DEFECTS FOUND — do not PASS.** Two concrete removed-modifier defects, one possible compile issue, plus items that cannot be decided from the patch alone.

## Defects

**1. PatientHubHeader.kt — dropped layout modifier on hub patient name**
Hunk `@@ -251,13 +252,11 @@` (old lines ~104–106): the name changed from `Modifier.fillMaxWidth().testTag("hubPatientName")` to `Modifier.testTag("hubPatientName")`. The testTag survived, but `fillMaxWidth()` was removed.
- Failure scenario: the hub header name cell no longer expands to available width. Depending on the parent layout, long Arabic names wrap/shrink and sibling header content (badges, action chips) shifts — a layout/behavior change, not a pure label swap.

**2. PatientHubHeader.kt — `overflow = TextOverflow.Ellipsis` dropped on a `maxLines = 1`, `weight(1f)` Row**
Hunk `@@ -427,12 +426,11 @@` (old line ~122): `overflow = TextOverflow.Ellipsis` was removed while `maxLines = 1` and `modifier = Modifier.weight(1f)` remain, in the Row just before the `if (patient.delivered)` action block.
- Failure scenario: unless `PatientNameText` independently ellipsizes `maxLines = 1` text, a long name (especially Arabic) will not ellipsize and can overflow or push the delivered indicator off the row. I cannot confirm `PatientNameText`'s default overflow from the patch alone — this must be checked against the component source before merge.

**3. MedsTab.kt — possibly orphaned imports**
Removed `Spacer` (old 156), `size`/`width` (old 161–162) and `filled.Add` (old 168) together with their two Icon/Spacer usages. If any of `Spacer`, `size`, `width` are used elsewhere in `MedsTab.kt`, the file will not compile. `import androidx.compose.material.icons.Icons` may also now be unused if nothing else references `Icons.Default.*` (warning-level at most).
Same check for `MedicationTapToAddSection.kt`: after removing `filled.Add` (old 134), confirm `Icons` is still referenced elsewhere in that file.

## What checked out (PASS on these points)

- **Behavior/testTags/semantics:** `AddAttachmentDialog.kt`, `ClinicalAttachmentViewerDialog.kt`, `PatientEditor.kt` — style, color, `Modifier.semantics { heading() }`, `.testTag(tag)` and `.testTag("hubPatientName")` all carried over unchanged; no `onClick`/clickable/dispatch removed.
- **Accessibility:** every removed `Icon` carried `contentDescription = null` (decorative only), so no accessibility information was lost; Meds buttons keep `minimumTouchTarget()` and their testTags (`addCustomMedicationBtn`, `medEmptyPrescribe`).
- **ReportSections.kt:** `scanGestationalAge`/`scanWeight` callers unchanged; only display text substituted ("-" → `R.string.not_recorded` via `stringResource`). The EFW `PlainField` now passes `value = null` + `missingText`, so an empty value shows as "Not recorded" and cannot be mistaken for a recorded dash. `ltrIsolate` still applied to real values.
- **strings.xml:** matches owner intent (format hints dropped from visit/scan/delivery date labels; sentence-case plurals; `+ Add other` → `Add other`). No hardcoded strings or em dashes introduced.

## One consistency observation (not a defect against stated intent)

`lab_batch_add_n` still reads `+ Add %1$d` while the other "Add other" entries lost their plus. Confirm whether that's intentional or an oversight.

## Remaining verification (cannot be decided from the patch alone)

1. Open `PatientNameText` and confirm it preserves style/color/maxLines/testTag, what default overflow it applies (critical for defect #2), whether it right-aligns Arabic names, and whether it applies `fillMaxWidth` internally (the owner's sibling-steal concern for any Row containing it).
2. Confirm the removed imports are unused elsewhere in `MedsTab.kt` and `MedicationTapToAddSection.kt` (restore them or fix the compile error if not).

Re-check those two items and the diff is mergeable.
[Tokens] upload: 14352 download: 8271 cache_read: 0 cache_write: 0

