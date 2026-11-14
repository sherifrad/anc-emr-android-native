# Slice 3: Clinical attention card, header strip and dialog

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s03_attention.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0 and 2. Components as built in slice 0: `ui/components/RecordTokens.kt`, `LabeledValue.kt`, `RecordSectionCard.kt`, `StatusTag.kt`.

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`.

**Level 3:** none of the files below is level 3 in the project map, but this block is clinical-safety meaning (what the next doctor must not miss). Ask for a frontier-tier review anyway.

**Visual only.** Parsing (`parseAttentionEntries`, `ClinicalAttentionLevel.parse`), order (`sortedForDisplay`), the overall level (`clinicalAttentionLevel`), severe-BP detection, entry limits (140 characters, 10 entries), dialog save content (`composeAttentionEntries`), edit gates and every event stay exactly as they are.

## Lessons from slices 0 and 1

- `LabeledValue(stretch = true)` only inside `LabeledValueRow` with `Modifier.weight(1f).fillMaxHeight()`; never `weight` on a standalone field in a scrolling column.
- Several `testTag`s on one node: the first (outermost) wins in tests. The strip carries `clinicalAttentionStrip` then `clinicalAttentionBanner`; keep that order.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/patienthub/components/ClinicalAttentionBanner.kt` (582 lines) | change `attentionColors` ALERT; restyle `ClinicalAttentionDialog` and `AttentionLevelDropdown`; delete the unused `ClinicalAttentionBanner` composable |
| `ui/patienthub/components/ClinicalAttentionEntries.kt` (106 lines) | add `AttentionEntryBox` |
| `ui/patienthub/components/ClinicalAttentionCard.kt` (194 lines) | rewrite on `RecordSectionCard` |
| `ui/patienthub/components/ClinicalGroupSection.kt` (70 lines) | delete (its last caller is the card) |
| `ui/patienthub/PatientHubHeader.kt` | restyle `CompactClinicalAttentionStrip` only (same name, same package, same file) |
| `app/src/main/res/values/strings.xml` | change 1 value |

Grep before deleting: `ClinicalGroupSection(` must have no caller left; `ClinicalAttentionBanner(` has no caller in `app/src/main` or the tests today (the tag `clinicalAttentionBanner` lives on the strip, keep it). Keep `ClinicalAttentionLevel` (including `parse`, used by `AncStateTest`), `AttentionColors`, `attentionColors`, `clinicalAttentionColors`, the typealias.

## Strings

Change `clinical_attention` "Clinical Attention" to "Clinical attention". Reuse `action_edit`, `edit_clinical_alerts`, `no_clinical_attention_recorded`, `add_clinical_attention`, `attention_level_*`, `attention_remaining_count`, `delete_attention_entry`, `attention_new_label`, `attention_add_button`, `save`, `cancel`, `close`, `bp_attention_severe`.

## 1. `attentionColors(level)`

- CRITICAL: unchanged (`errorContainer`, `onErrorContainer`, accent `error`, border `error`, 1.5 dp).
- ALERT: light `container = StatusModerateBg`, `content = StatusModerateText`, `accent = StatusModerateText`, `border = StatusModerateBorder`, 1 dp; dark the `StatusModerateDark*` set. (Today #B45309 on #FFFBEB is 4.84:1; new 7.19:1 light, 9.44:1 dark.)
- NOTE: `surfaceContainerHigh`, `onSurface`, accent `onSurfaceVariant`, border `outlineVariant` with the alpha removed, 1 dp.
- `ClinicalCardTokens.Warning*` stay in `Theme.kt` (other screens use them).

## 2. `AttentionEntryBox` (new, `ClinicalAttentionEntries.kt`)

```kotlin
@Composable
fun AttentionEntryBox(level: ClinicalAttentionLevel, text: String, modifier: Modifier = Modifier) {
    val c = attentionColors(level)
    Surface(shape = RecordShapes.Small, color = c.container, border = BorderStroke(c.borderWidth, c.border), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(levelWord(level), style = RecordType.FieldLabel.copy(fontWeight = FontWeight.Bold), color = c.accent)
            Text(text, style = RecordType.FieldValue, color = c.content)
        }
    }
}
```
`levelWord(level)` is one private `@Composable` mapping to `attention_level_critical / alert / note`; replace the three copies of that `when` (card, dialog, dropdown, strip) with it. Semantics: `Modifier.semantics(mergeDescendants = true) {}` on the Surface so TalkBack reads "Critical, Severe PIH".

## 3. `ClinicalAttentionCard(attention, severeBp, onEdit, modifier)` (same signature)

- Keep the early `return` when empty and `onEdit == null`.
- `RecordSectionCard(title = stringResource(R.string.clinical_attention), family = FieldFamilies.patient(), modifier = modifier.testTag("clinicalAttentionCard"), action = onEdit?.let { { SectionTextAction(stringResource(R.string.action_edit), stringResource(R.string.edit_clinical_alerts), it, Modifier.testTag("editClinicalAttention")) } })`. The family is not used by entries.
- Empty: `Text(no_clinical_attention_recorded, RecordType.FieldValue, onSurfaceVariant)`, then when `onEdit != null` `OutlinedButton(onEdit, shape = RecordShapes.Small, border = BorderStroke(1.dp, colorScheme.outline), modifier = Modifier.heightIn(min = 48.dp).testTag("addClinicalAttention")) { Text(add_clinical_attention, RecordType.Action, primary) }`.
- Entries: `Column(verticalArrangement = spacedBy(8.dp))`: severe BP first as `AttentionEntryBox(CRITICAL, stringResource(R.string.bp_attention_severe, bp, date), Modifier.testTag("severeBpAttention"))`, then `sortedForDisplay(entries)` each `AttentionEntryBox(entry.level, entry.text, Modifier.testTag("attentionEntry-$index"))`.
- Removed: the header icon tile (via `ClinicalGroupSection`), the per-entry warning and info icons, the 13 sp body text, the pencil icon.

## 4. `CompactClinicalAttentionStrip` (in `PatientHubHeader.kt`, same signature)

- Keep the early return, `remainingCount` rule, `baseText` choice (severe BP text, else first sorted entry text), the `onOpenDialog` click.
- `Surface(onClick = onOpenDialog, shape = RecordShapes.Small, color = colors.container, border = BorderStroke(colors.borderWidth, colors.border), modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("clinicalAttentionStrip").testTag("clinicalAttentionBanner"))`.
- `Row(Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(8.dp))`:
  - `Column(Modifier.weight(1f))`: level word (`levelWord(level of the shown item)`; severe BP shows "Critical") in `RecordType.FieldLabel.copy(fontWeight = Bold)`, `colors.accent`; then the text without the "Level: " prefix in `RecordType.Secondary`, `colors.content`, `maxLines = 2`, `TextOverflow.Ellipsis`.
  - when `remainingCount > 0`: `Surface(shape = RecordShapes.Small, color = Color.Transparent, border = BorderStroke(1.dp, colors.accent)) { Text(stringResource(R.string.attention_remaining_count, remainingCount), RecordType.Tag, colors.accent, Modifier.padding(horizontal = 6.dp, vertical = 1.dp)) }`.
  - `Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.content, Modifier.size(18.dp))` (full alpha).
- Removed: the leading level icon and the `"$levelLabel: ${text}"` concatenation (level word now has its own line).
- Do not add a custom `contentDescription` to the strip: the default merged semantics already read "level, text, +N", and the existing `onNodeWithText("+1", substring = true)` needs the count as text.

## 5. `ClinicalAttentionDialog` (same signature and behaviour)

- Title: `Text(clinical_attention, RecordType.SectionTitle, onSurface)`.
- Read-only list (`canEdit == false`): each entry `AttentionEntryBox(entry.level, entry.text, Modifier.testTag("attentionEntry-$index"))`.
- Edit rows: unchanged structure (dropdown, delete `IconButton` with `minimumTouchTarget()` and tag `attentionDelete-$index`, `OutlinedTextField` with tag `attentionText-$index` and the counter). Counter text `RecordType.Tag` weight Normal, onSurfaceVariant (12 sp floor; today `labelSmall` 11 sp).
- New-entry row: unchanged tags `attentionNewLevel`, `attentionAdd`, `attentionNewText`; `attentionAdd` becomes `OutlinedButton(shape = RecordShapes.Small)` 48 dp.
- Confirm: `Button(shape = RecordShapes.Small)` with default colours (primary, onPrimary), 48 dp, tags `attentionSave` / `attentionClose` unchanged. Dismiss: `OutlinedButton(shape = RecordShapes.Small)` tag `attentionCancel`.
- `AttentionLevelDropdown`: drop the leading icon; keep the `KeyboardArrowDown` (it marks a menu); `Surface(shape = RecordShapes.Small, ...)`; word `RecordType.Action.copy(fontWeight = Bold)` in `colors.content`; menu items keep tags `attentionLevelOption-*`, drop their leading icons, text `RecordType.FieldValue` in the option's `content` colour.

## Acceptance checklist

1. Tags present as today: `clinicalAttentionCard`, `editClinicalAttention`, `addClinicalAttention`, `severeBpAttention`, `attentionEntry-N`, `clinicalAttentionStrip` (first tag on the strip), `clinicalAttentionBanner`, all dialog tags.
2. Every entry shows its level word ("Critical", "Alert", "Note") as text; no level is shown by colour alone; no warning or info icon left in card, strip, dialog list or dropdown.
3. Alert text and word contrast >= 7:1 light and dark (tokens above).
4. Strip >= 48 dp tall, whole strip clickable, "+N" present when more entries exist, text clamps at two lines.
5. Card, Edit, Add attention, dialog buttons >= 48 dp; radii only 10 and 16 dp.
6. Severe BP entry is first and is Critical.
7. 360 dp, font scale 1.3: a 140-character entry wraps fully in the card (no ellipsis) and clamps to two lines in the strip.
8. RTL: card title right, Edit left; strip chevron points left.
9. Dark theme renders all three levels.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. `ClinicalAttentionUiTest` finds texts "Critical", "Note", "No clinical attention recorded", "+1" and the tags above; all stay. If a test asserted the old "Critical: text" single string in the strip, it would need the split; grep found none.

### Must pass unchanged

`ClinicalAttentionUiTest`, `SevereBpAttentionTest`, `VisitDraftStateAndCommandTest`, `PatientHubDesignTest`, `PatientHubNavigation`, `AncWorkflowTest`, unit `AncStateTest` (`ClinicalAttentionLevel.parse`).

### New test

`app/src/androidTest/java/com/sherifradwan/ancnative/AttentionRedesignTest.kt`:
- card with "CRITICAL: Severe PIH\nALERT: Anaemia\nNOTE: Prefers female doctor" and a severe BP fixture `SevereBpAttention("165/105", "2026-11-10")`: `severeBpAttention` bounds top < `attentionEntry-0` top; each entry `assertIsDisplayed()` and height >= 44 dp; texts "Critical" (twice), "Alert", "Note" exist;
- no node under `clinicalAttentionCard` has a content description for an icon (count nodes with `SemanticsProperties.ContentDescription` equal to zero except the Edit action);
- strip with the four entries: height >= 48 dp; "+3" exists; clicking calls `onOpenDialog`; at 360 dp and font scale 1.3 the strip text node has `lineCount <= 2`;
- dialog read-only mode (`canEdit = false`) renders `attentionEntry-N` and `attentionClose`; edit mode keeps `attentionSave` >= 48 dp;
- RTL: card title right of Edit by bounds;
- dark theme renders without clipping.

## Risk

Medium (clinical meaning). No level-3 file, no state change. Full emulator suite before any push.
