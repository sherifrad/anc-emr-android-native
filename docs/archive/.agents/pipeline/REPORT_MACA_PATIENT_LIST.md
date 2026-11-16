# MACA Testing-Mode Audit: Patient List Redesign

**Date**: 2026-11-07
**Branch**: `main` (uncommitted working tree, HEAD `26f5fc0`)
**Scope**: `git diff` only — `ui/PatientListScreen.kt` (+557/−538) and `res/values/strings.xml` (+4)
**Mode / Weight**: Testing (review only, no source edits) / Full (clinical pack, level-3 `app/src` path)
**Reviewer**: independent review pass; the diff was not authored in this session.

---

## 1. Verdict

**Conditional — not ready to merge as-is.**

| # | Requirement | Verdict |
|---|---|---|
| 1 | Room schema v6 frozen (no DB changes) | **PASS** |
| 2 | Test tags preserved | **PARTIAL** |
| 3 | 48 dp touch targets | **PASS** (static; not measured on device) |
| 4 | Calm ivory/surface card, single urgency badge | **PARTIAL** |
| 5 | Clinical attention callout | **PASS**, with deviations |
| 6 | English strings in `strings.xml` | **FAIL** |

Two items should be resolved or explicitly accepted by the owner before merge:
**F1** (unrecorded risk rendered as green "Low Risk") and **F2** (35 raw hex colors introduced, a DEF-07 regression).
The remainder are fix-before-merge or follow-up items.

---

## 2. Requirement-by-requirement evidence

### 1. Room schema v6 frozen — PASS
- `git status` shows exactly two modified files; no `storage/`, `app/schemas/`, `RecordsMigration.kt`, `RecordsDatabase.kt`, or DAO change.
- `RecordsDatabase.kt` still declares `version = 6`; `app/schemas/.../6.json` untouched.
- The added `riskTier` lives only in the in-memory `PatientDashboardRowProjection`; nothing is persisted.
- Static gates `check_r6a.py` S303 and `check_r6b2.py` S416 fail **only** on the UI-file allow-list clause ("forbidden file changed: …/PatientListScreen.kt"); their schema/version/DAO clauses pass.

### 2. Test tags preserved — PARTIAL
All eight tags that exist in the file are still present in source:
`patient-`, `workspace-`, `profile-`, `editPatient-`, `statusPatient-`, `patientSearch`, `newPatient`, `patientListTable`.

Connected test `AncWorkflowTest#clinicalSummaryAndDeliveryPlanningWorkflowRenderAccurately`
(which asserts `patient-`, `workspace-`, `profile-` are displayed) **passes** on `anc-test-api26`.

Gaps:
- **`patientAgeBadge` is no longer rendered.** The old card used `PatientAgeBadge(...)`, which carries `testTag("patientAgeBadge")` (`ClinicalBadges.kt:58`). The new inline age tag has no tag. (No current test consumes it, but it is a removed hook.)
- `editPatient-` / `statusPatient-` moved off the list surface into the new `patientMenu-` bottom sheet. Tags still exist, but any future test that clicks them directly on the list will fail; no test currently covers `patientMenu-`.
- **Duplicate tags:** `workspace-{caseId}` and `profile-{caseId}` now exist both on the card and inside the sheet. With the sheet open, `onNodeWithTag` becomes ambiguous.
- New tags added: `patientMenu-`, `riskFilter-all|high|medium|low`.

### 3. 48 dp touch targets — PASS (static)
- Card action `IconButton`s use `Modifier.minimumTouchTarget()` (`sizeIn(minWidth = 48.dp, minHeight = 48.dp)`, `Components.kt:150`); `IconButton` is 48 dp by default anyway.
- Sheet "Cancel" `TextButton` is explicitly `.height(48.dp)`.
- Sheet action rows: 14 dp top + 14 dp bottom + `bodyMedium` line height 20 sp = 48 dp exactly (grows with font scale).
- Filter chips rely on Material3's built-in minimum interactive size (visual height 32 dp, touch target expanded); not explicitly asserted.
- Not measured on device.

### 4. Calm ivory/surface card with single urgency badge — PARTIAL
- Card container is `colorScheme.surface` on the ivory (`ClinicalIvory #FBF8F5`) screen background; high-risk tint is only a 4 % lerp toward `errorContainer` — genuinely calm, a clear improvement over the old 22–40 % tint + 1.5 dp error border.
- **Two urgency badges in the header** when `severeBp != null`: the risk pill plus the Severe BP pill (`PatientListScreen.kt:636–660`). If "single urgency badge" means exactly one, this is not met.
- **35 raw `Color(0xFF…)` references introduced** (file had 0 at HEAD), including re-implementations of the existing `TagHighRisk*`, `TagAttention*`, `TagBlood*` tokens. This re-opens documented finding **DEF-07** (`.agents/SPEC_AND_AUDIT.md`) and the plan rule "no raw ad hoc colors per screen".

### 5. Clinical attention callout — PASS, with deviations
- Implemented as a bordered callout with accent bar + warning icon, distinct light/dark colors, inside `DirectoryClinicalAttentionBox` (`PatientListScreen.kt:869+`).
- Deviation 1: it renders **unconditionally**, including a muted "No clinical attention recorded" strip on every patient. The old `PatientAttentionRow` rendered nothing when empty. This adds a 5th line to every card and works against the "calm" intent.
- Deviation 2: truncation was **reduced** — clinical attention `maxLines` 4 → 2, severe-BP line 2 → 1. Multi-line clinician alerts can now be cut off on the list (full text remains reachable via Profile).

### 6. English strings in `strings.xml` — FAIL
New user-facing English literals outside `strings.xml`:
- `PatientListScreen.kt:657` — `contentDescription = "More actions"`
- `PatientListScreen.kt:694` — `text = "${row.ageText} y"` (the " y" unit)
- `PatientListScreen.kt:761` — `text = "GA "`
- `PatientListScreen.kt:772` — `text = " · EDD …"`
- `PatientListScreen.kt:391` — `G… P…` notation plus `"—"` placeholder (notation is arguably clinical shorthand; the placeholder is a glyph)
- `PatientListScreen.kt:745` — `stringResource(R.string.archived).uppercase()` (locale-sensitive casing)

Net direction is positive (the old `PATIENT`/`OBSTETRIC HISTORY`/`ARCHIVED`/`Estimated Due Date (EDD)`/`Method:` literals were removed and four new strings were added), but the requirement is not satisfied. No `values-ar` exists, so there is no Arabic-resource gap.

---

## 3. Findings

### High — clinical safety / project rules
1. **F1 — Unrecorded risk is shown as green "Low Risk" and counted in the Low Risk filter.**
   `parseRiskTier(null|blank)` returns `LOW` (`PatientListScreen.kt:83–89`), so a patient with no recorded risk gets a green pill and is included in `lowRiskCount` / the "Low Risk (n)" filter. This contradicts `PatientPrintHtmlGenerator` (`riskLevel.isNullOrBlank() -> "Risk not recorded"`) and `IMPLEMENTATION_PLAN.md`'s "no speculative … risk inference". The previous UI also defaulted blank to "Normal Risk", so the defect is pre-existing — but the redesign makes it explicit and adds a dedicated filter, which is the moment to fix it. Recommend an `UNRECORDED` tier (neutral pill, excluded from Low).
2. **F2 — 35 raw hex colors introduced** (0 → 35), re-opening DEF-07. Use the existing `TagHighRisk*`, `TagAttention*`, `TagBlood*` tokens and theme roles.

### Major
3. **F3 — Age badge validation bypassed.** The old `PatientAgeBadge` ran `formatAgeYears` (10..70 only) and hid invalid values; the new inline tag prints `row.ageText` verbatim, so "29 years" → "29 years y", and out-of-range/invalid ages render. `patientAgeBadge` tag also lost.
4. **F4 — Hardcoded English strings** (requirement 6 above).
5. **F5 — Duplicate test tags** `workspace-`/`profile-` in card and sheet.
6. **F6 — Edit/Archive actions now require opening `patientMenu-`**; no test covers the sheet, and `editPatient-`/`statusPatient-` are no longer on the list surface.
7. **F7 — Attention truncation reduced** (4→2 and 2→1 lines).
8. **F8 — Wrong subtitle on the restore action.** The sheet always shows `archive_patient_subtitle` ("Hidden from this list; can be restored") even when the action is "Unarchive patient" (`PatientListScreen.kt:477`).
9. **F9 — No tests for new logic/UI.** `parseRiskTier` (new pure function), the four-way risk filter, and the bottom sheet have no unit or UI test. MACA test economy expects new logic to ship with an oracle.

### Minor
10. **F10 — EDD dating method is no longer displayed anywhere.** `datingMethodText` is still computed but unused; the old EDD popover showed "Method: LMP/Ultrasound/Agreed".
11. **F11 — Profile action in the sheet uses a `Search` icon** (`:538`) while the card uses `AccountCircle`.
12. **F12 — Casing inconsistency** between "HIGH RISK" and "Medium Risk"/"Low Risk" pills.
13. **F13 — Sheet closes instantly** (`sheetPatient = null` before `sheetState.hide()`), so there is no dismiss animation.
14. **F14 — Wide (≥640 dp) table layout removed.** Cards now stretch to 880 dp at all widths. Confirm this is intended for tablets.
15. **F15 — Card ripple is not clipped** to the 16 dp corners because `clickable` is applied outside the card's clip.
16. **F16 — `.uppercase()` on a resource string** is locale-sensitive; use a dedicated all-caps string if needed.

---

## 4. Evidence (commands actually run)

```text
git status --porcelain
 M app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt
 M app/src/main/res/values/strings.xml

./gradlew :app:compileDebugKotlin --rerun-tasks --console=plain
BUILD SUCCESSFUL in 9s   (warnings only in AncState/InAppCameraDialog/WorkspaceScreen, none in PatientListScreen.kt)

./gradlew :app:testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 11s

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.sherifradwan.ancnative.AncWorkflowTest#clinicalSummaryAndDeliveryPlanningWorkflowRenderAccurately
Starting 1 tests on anc-test-api26(AVD) - 8.0.0
Finished 1 tests on anc-test-api26(AVD) - 8.0.0
BUILD SUCCESSFUL in 10s

bash .agents/scripts/run_static_checks.sh
EXIT=1
FAIL .agents/checks/check_r6a.py   -> S303 forbidden file changed: .../ui/PatientListScreen.kt
FAIL .agents/checks/check_r6b2.py  -> S416 ui file outside allow list: .../ui/PatientListScreen.kt
(DB/schema/version/DAO clauses in both checks PASS)
```

Raw color count: `git show HEAD:…/PatientListScreen.kt | grep -c 'Color(0x'` → **0**; working tree → **35**.

---

## 5. Not verified

- 48 dp targets measured on device (static analysis only).
- Dark theme, RTL/Arabic alignment, large font scale, landscape, tablet widths.
- Full connected suite (only the one impact-selected test was run).
- Screenshot/golden comparison: no approved mock or spec for this patient-list redesign exists under `.agents/pipeline/design/` or `.agents/mocks/`.
- Two round-scoped static checks remain red by design because they encode "this round touched no UI file"; they are stale for this change, not evidence of a DB violation.

## 6. Assumptions

- The six requirements are taken verbatim from the request; no written spec/mock for this redesign was found in the repo.
- "Single urgency badge" is read as one urgency indicator in the card header; the risk pill + Severe BP pill combination is therefore flagged rather than failed outright.
- Risk tier vocabulary is the canonical `PatientVocabulary.riskLevels` = {Low Risk, Moderate Risk, High Risk}.
