# Slice 4: Patient hub header and tab bar

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s04_hub_header.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0 and 2. Components as built: `ui/components/RecordTokens.kt`, `LabeledValue.kt`, `RecordSectionCard.kt`, `StatusTag.kt`. Depends on slice 3 (the strip is already restyled; do not touch `CompactClinicalAttentionStrip`).

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`.

**Level 3:** this slice reads level-3 code but must not edit it: `ui/ClinicalBadges.kt` (`GaDiscordanceMarker`, `BpAlertLabel`, `GestationalAgeBadge`, `ClinicalRiskBadge`) and `ui/ClinicalFormat.kt` (`formatBloodGroup`, `isRhNegative`, `formatAgeYears`) are called as they are. If the builder finds it must edit either file, stop and ask: that needs a frontier-tier review and an independent test review. `PatientHubHeader.kt` itself is not level 3 in the map.

**Keep logic untouched.** `PatientHubHeaderState`, the collapse math (`Modifier.layout`, `onSizeChanged`, `graphicsLayer` alpha and translation), the sticky-bar threshold, tab badge counts and alert rules (`badgeCount`, `hasTabAlert`), `hasAttention`, every callback. Only what is drawn inside the hero, the sticky bar and each tab label changes.

## Lessons from slices 0 and 1

- `LabeledValue(stretch = true)` only inside `LabeledValueRow`; not used here.
- First (outermost) `testTag` wins. `workspaceReportButton` is first on the report button today and tests find it by that tag: keep the order `workspaceReportButton`, then `hubReportButton`.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/components/MetricCell.kt` | new: `MetricCell`, `MetricValue` moved from the card |
| `ui/patientdirectory/PatientDirectoryMetricsRow.kt` | delete its private `MetricCell` and `MetricValue`, import the shared ones (no visual change) |
| `ui/patienthub/PatientHubHeader.kt` (575 lines) | restyle hero, sticky GA, tab labels |
| `ui/components/StatusTag.kt` | add `StatusKind.NormalRisk` |
| `ui/components/RecordTokens.kt` | add `RecordType.HeaderName` |
| `app/src/main/res/values/strings.xml` | add 6 strings |

`PatientHubTab.kt` keeps its `icon` field (other code may read it); the tab row just stops drawing it. `PatientHubScreen.kt` is not touched.

## Strings (add)

```xml
<string name="status_normal_risk">Normal risk</string>
<string name="rh_negative_tag">Rh negative</string>
<string name="needs_attention">Needs attention</string>
<string name="action_back">Back</string>
<string name="action_two_page_report">Two-page report</string>
<string name="age_label">Age</string>
```
Reuse: `ga_label`, `parity`, `blood_group`, `delivery`, `edit_patient`, `clinical_attention`, `no_allergy_history`, `allergy_banner` ("Allergy: %1$s"). Do not change `normal_risk` ("STANDARD RISK") or `normal_risk_filter`: other screens use them. The hard-coded content descriptions "Back", "2-Page Report", "Edit Patient", "Clinical Attention", "Attention Alert" move to the strings above (`edit_patient`, `clinical_attention`).

## 1. Shared metric cell (`ui/components/MetricCell.kt`)

Move, do not rewrite: take `MetricCell(label, modifier, content)` and `MetricValue(...)` exactly as they are now private in `PatientDirectoryMetricsRow.kt`, make them public in package `ui.components`, and point the card at them. The card must look identical afterwards (its tests are the proof). One component, two callers (AGENTS.md rule 1).

## 2. `StatusKind.NormalRisk`

In `StatusTag.kt`: `NormalRisk` maps to container `colorScheme.surface`, border `StatusIncompleteBorder` (dark `StatusIncompleteDarkBorder`), text `colorScheme.onSurfaceVariant`, word `status_normal_risk`, weight SemiBold. The list card never uses it (normal shows no tag there); only the header does.

## 3. `RecordType.HeaderName`

`TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)`.

## 4. Hero (expanded header)

Keep the outer `Surface`, the collapsing `Box` with its `layout` block and the `onSizeChanged` / `graphicsLayer` Column. Change only the Column's padding and children:

- `Column(... .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp))`.
- Action bar: buttons, icons, tags and order unchanged (48 dp each). Only the content descriptions change, to the string resources above.
- Name: `Text(patient.displayName, style = RecordType.HeaderName, color = onSurface, modifier = Modifier.fillMaxWidth().testTag("hubPatientName"))`. No `maxLines`.
- Tags: `FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = spacedBy(6.dp), verticalArrangement = spacedBy(6.dp))`:
  - `Box(Modifier.testTag("hubRiskBadge")) { StatusTag(if (isHighRisk(patient.riskLevel)) HighRisk else if (parseRiskTier(patient.riskLevel) == PatientRiskTier.MEDIUM) ModerateRisk else NormalRisk) }`. Same conditions as today's `ClinicalRiskBadge` call.
  - `patient.visits.firstOrNull()?.bloodPressure?.let { BpAlertLabel(it) }` unchanged.
  - Allergy (same `classifyAllergy` rule, hidden when `NotRecorded`): `Surface(shape = RecordShapes.Small, color = if (recorded) errorContainer else surfaceContainerHigh, border = if (recorded) BorderStroke(1.dp, error) else null, modifier = Modifier.testTag("hubAllergyPill"))` with `Text(if (recorded) stringResource(R.string.allergy_banner, text) else stringResource(R.string.no_allergy_history), style = RecordType.Tag.copy(fontWeight = Bold), color = if (recorded) onErrorContainer else onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp).testTag("hubAllergyText"))`. No `maxLines` (a test checks the allergy text is never cut).
  - Removed from this row: `PatientAgeBadge`, `ObstetricParityBadge`, `BloodGroupBadge` (they move to the facts row).
- Facts: `Row(Modifier.fillMaxWidth(), horizontalArrangement = spacedBy(8.dp))` with three `MetricCell(..., Modifier.weight(1f))`:
  - not delivered and GA known: `Box(Modifier.weight(1f).testTag("hubGaBadge")) { MetricCell(stringResource(R.string.ga_label)) { Row(Modifier.testTag("gestationalAgeBadge"), verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(4.dp)) { MetricValue(ltrIsolate(ga)); if (gaDiscordant) GaDiscordanceMarker(style = RecordType.MetricValue) } } }`. `ga` is today's `calculateGestationalAge()` text (owner decision 9 keeps "27w 2d" here).
  - delivered: `Box(Modifier.weight(1f).testTag("hubDeliveredBadge")) { MetricCell(stringResource(R.string.delivery)) { MetricValue(deliveredHeadline(patient.deliveryDate)) } }` (no `hubGaBadge`, as today).
  - GA unknown and not delivered: an empty `Spacer(Modifier.weight(1f))` keeps the columns aligned (today the badge was simply absent).
  - Parity: `MetricCell(stringResource(R.string.parity))` value `ltrIsolate(parityText)` with the same `G$g P$p` plus ` A$a` when a > 0, fallbacks as `ObstetricParityBadge` (1, 0, 0). The rule today is `"G$g P$p" + if (a > 0) " A$a" else ""` in both `ObstetricParityBadge` and the card's `ParityCell`; put it once in a public `parityText(gravida: Long?, parity: Long?, abortion: Long?): String` in `MetricCell.kt` and call it from the header and from `ParityCell` (leave `ObstetricParityBadge` alone: level 3).
  - Blood group: `MetricCell(stringResource(R.string.blood_group))` value `ltrIsolate(formatBloodGroup(patient.bloodGroupText) ?: "--")` (missing style when null; today the blood pill was hidden when blank, now the column shows "--" so the three columns stay aligned); when `isRhNegative(...)`, below the value `StatusTag(StatusKind.HighRisk, text = stringResource(R.string.rh_negative_tag), modifier = Modifier.padding(top = 4.dp))`.
  - Age: when `formatAgeYears(patient.ageText)` is not null, a second `Row` with one `MetricCell(stringResource(R.string.age_label), Modifier.weight(1f))` and two `Spacer(Modifier.weight(1f))`.
- Strip: `if (hasAttention) CompactClinicalAttentionStrip(...)` unchanged (slice 3).

## 5. Sticky bar

Unchanged structure, height 56 dp, tags `stickyBackButton`, `stickyAttentionButton`. Replace the two `GestationalAgeBadge(...)` calls with `Text(ltrIsolate(text), style = RecordType.Action.copy(fontWeight = FontWeight.SemiBold), color = onSurface, maxLines = 1)` where `text` is `deliveredHeadline(...)` when delivered, else `gaCompact`; keep `GaDiscordanceMarker` after it when `gaDiscordant` and not delivered. Name `RecordType.CardName`, one line, ellipsis.

## 6. Tab row

Keep `PrimaryScrollableTabRow`, tags, `minimumTouchTarget()`, selection, `badgeCount` and `hasTabAlert`. Tab `text` (sketch; `dark ? a : b` means `if (isDarkTheme()) a else b`, and `StatusHigh(Dark)Text` means the dark or light token):
```kotlin
Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(6.dp)) {
    Text(tab.title, style = RecordType.Action.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium), maxLines = 1, softWrap = false)
    if (badgeCount != null && badgeCount > 0) {
        Surface(shape = RecordShapes.Small, color = if (hasTabAlert) (dark ? StatusHighDarkBg : StatusHighBg) else (dark ? FieldPatientDarkBg : FieldPatientBg)) {
            Text(if (hasTabAlert && tab == PatientHubTab.LABS) "$badgeCount▲" else "$badgeCount", style = RecordType.Tag, color = if (hasTabAlert) StatusHigh(Dark)Text else onSurface, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
        }
    } else if (hasTabAlert) {
        Surface(shape = RecordShapes.Small, color = StatusHigh(Dark)Bg, modifier = Modifier.semantics { contentDescription = needsAttention }) {
            Text("!", style = RecordType.Tag.copy(fontWeight = FontWeight.Bold), color = StatusHigh(Dark)Text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
        }
    }
}
```
- Removed: the tab `Icon`, the 999 dp pill, the 6 dp `ActionRose` dot (colour-only), the `TagHighRisk*` colours here.
- Unselected tab text colour comes from the TabRow (onSurfaceVariant), selected from primary; leave the M3 defaults.

## Acceptance checklist

1. Tags: `hubPatientName`, `hubGaBadge` (not delivered), `hubDeliveredBadge` (delivered, and then no `hubGaBadge`), `gestationalAgeBadge` exactly once in the expanded header, `gaDiscordanceMarker` only when discordant and only when GA is in the window, `hubRiskBadge` always, `hubAllergyPill` and `hubAllergyText` when allergy is recorded or explicitly none, `hubBackButton`, `workspaceReportButton` (first) and `hubReportButton`, `editPatientButton`, `workspaceAttentionButton` when attention exists, `patientHubTabBar`, `tab-*`, `stickyBackButton`, `stickyAttentionButton`, `clinicalAttentionStrip`.
2. Name never ellipsised; at width 360 dp and font scale 2.0 a long name wraps (<= 4 lines in the existing test) and the allergy text is not cut.
3. No text under 12 sp in the hero, sticky bar or tab labels.
4. Every risk state shows a word: "High risk", "Moderate risk", "Normal risk". Rh negative shows the word "Rh negative".
5. No tab icon, no colour-only dot; an alert with no count shows "!" with the label "Needs attention".
6. Collapse still works: scrolling the Overview tab up hides the hero and shows the 56 dp sticky bar (existing behaviour, check on the emulator).
7. RTL: the action bar mirrors (back at the right), facts run right to left, values isolated (`A+` not `+A`).
8. Dark theme: tags use dark tokens, counts readable.
9. The list card is visually unchanged after the `MetricCell` move (slice 1 tests pass unchanged).

## Tests (written by a different lane from this spec)

### Existing tests that need edits

- None expected for tags. `DatingGaMarkerSurfacesTest.expandedPatientHubHeaderShowsMarkerOnlyWhenDiscordant` counts `gestationalAgeBadge` nodes (== 1) and markers; both kept by the facts-row tagging above. If the lane finds the count is now 0 because the tag moved inside a merged node, query with `useUnmergedTree = true` (the test already does).
- `PatientHubDesignTest` checks `hubPatientName` line count and `hubAllergyText` not ellipsised: unchanged, confirm.

### Must pass unchanged

`PatientHubDesignTest`, `DatingGaMarkerSurfacesTest`, `DeliveryPlanSheetTest` (`hubDeliveredBadge`, `hubGaBadge` absence), `RegistrationPencilUiTest` (`editPatientButton`), `AncWorkflowTest` and `ClinicalPatientCardTest` (`workspaceReportButton`), `PatientHubNavigation`, `ClinicalAttentionUiTest`, `PatientCardRedesignTest` and `OverviewRedesignTest` from slice 1.

### New test

`app/src/androidTest/java/com/sherifradwan/ancnative/HubHeaderRedesignTest.kt`:
- Moderate patient: "Moderate risk" under `hubRiskBadge`; normal patient: "Normal risk"; high: "High risk";
- facts row: texts "GA", "Parity", "Blood group" each `assertIsDisplayed()`; parity value "G4 P1 A2" for gravida 4, parity 1, abortion 2; blood "A+" isolated (`"⁦A+⁩"`);
- Rh-negative fixture shows "Rh negative"; age fixture shows "Age";
- no node in `patientHubTabBar` has an icon content description; a patient with no dating shows "!" with content description "Needs attention" in the Overview tab; labs abnormal shows "N▲";
- RTL: `hubBackButton` right of `editPatientButton` by bounds;
- 360 dp, font scale 1.3: none of "GA", "Parity", "Blood group" values is ellipsised (`assertNotEllipsized` helper exists in `ComposeTextAsserts.kt`).

## Risk

Medium-high: the header is on every patient screen and the collapse math sits next to the changed code. No level-3 edit allowed. Full emulator suite, then a manual collapse check on the emulator before any push.
