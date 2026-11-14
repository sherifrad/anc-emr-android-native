# Slice 2: Delivery planning card and delivered state

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s02_delivery.html`. System and rules: `docs/designs/redesign/PLAN.md` sections 0 and 2. Components as built in slice 0 (read them, do not re-spec them): `ui/components/RecordTokens.kt`, `LabeledValue.kt`, `RecordSectionCard.kt`, `StatusTag.kt`.

Branch: next branch off `redesign-2026-11-15` (or as the coordinator names it). `.agents/scripts/git_audit.sh` before work and before any push; update `.agents/pipeline/HANDOFF_AGY.md` in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`. Also out of scope: `model/DeliveryNotes.kt` (parses notes with the Title Case labels), `DeliveryPlanSheet.kt` (slice 11).

**Level 3:** `ui/ClinicalBadges.kt` is level 3 (clinical_safety). This slice only deletes two unused-after-this-slice declarations from it. Frontier-tier review plus an independent test review.

**Visual only.** Same notes parsing, same four values, same delivered rules (mode and outcome hidden when null), same edit gate (`canEdit`), same events.

## Lessons from slices 0 and 1 (apply them)

- `LabeledValue` has `stretch: Boolean = false`. Pass `stretch = true` with `Modifier.weight(1f).fillMaxHeight()` **only** inside `LabeledValueRow`. A standalone `LabeledValue` in a scrolling column never gets `weight`.
- When a node carries several `testTag` modifiers, the first (outermost) one is what tests find. Put the tag a test looks for first in the chain.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/patienthub/components/DeliveryPlanningCard.kt` (131 lines) | rewrite on `RecordSectionCard` |
| `ui/patienthub/components/DeliveryOutcomeUi.kt` (97 lines) | restyle `DeliveredSummary`, add `deliveredDateText` |
| `ui/ClinicalBadges.kt` | delete `enum class DeliveryTone`, `DeliveryTone.colors()`, `ToneColors`, `DeliverySectionBox` (level 3) |
| `ui/Theme.kt` | delete the 24 `Delivery*` colour vals (light and dark `Hospital`, `Fee`, `Plan`, `Postpartum` x `Bg`, `Border`, `Text`) |
| `app/src/main/res/values/strings.xml` | change 1 value, add 6 strings |

Before deleting, grep `app/src` for each name; today the only users are the card, `ClinicalBadges.kt` itself and `ThemeContrastTest`.

## Strings

Change: `delivery_planning` "Delivery Planning &amp; Outcome" to "Delivery planning &amp; outcome" (only caller is the card).

Add (display labels; the existing `delivery_*_title` strings stay untouched because `DeliveryPlanSheet` uses them and they mirror the note keys):
```xml
<string name="delivery_hospital_label">Agreed delivery hospital</string>
<string name="delivery_fees_label">Agreed delivery fees</string>
<string name="delivery_plan_label">Delivery plan</string>
<string name="delivery_postpartum_label">Postpartum note</string>
<string name="delivery_date_label">Delivery date</string>
<string name="delivery_date_unknown_value">???</string>
```
Reuse: `action_edit`, `edit_delivery_plan`, `delivery_outcome_mode_label`, `delivery_outcome_result_label`, `not_recorded`, `status_delivered`.

## `DeliveryPlanningCard(patient, canEdit, modifier, onEditDeliveryPlan)` (same signature)

```kotlin
val sections = remember(patient.obstetricNotes) { parseDeliveryNotes(patient.obstetricNotes) }
RecordSectionCard(
    title = stringResource(R.string.delivery_planning),
    family = FieldFamilies.delivery(),
    modifier = modifier.testTag("deliveryPlanningCard"),
    action = if (canEdit) {
        { SectionTextAction(stringResource(R.string.action_edit), stringResource(R.string.edit_delivery_plan), onEditDeliveryPlan, Modifier.testTag("editDeliveryPlan")) }
    } else null,
) {
    if (patient.delivered) DeliveredSummary(patient)
    LabeledValue(stringResource(R.string.delivery_hospital_label), sections.hospital, modifier = Modifier.testTag("deliveryHospital"))
    LabeledValue(stringResource(R.string.delivery_fees_label), sections.fees, modifier = Modifier.testTag("deliveryFees"))
    LabeledValue(stringResource(R.string.delivery_plan_label), sections.plan, modifier = Modifier.testTag("deliveryPlan"))
    LabeledValue(stringResource(R.string.delivery_postpartum_label), sections.postpartum, modifier = Modifier.testTag("deliveryPostpartum"))
}
```
- All four full width, no `stretch`. Missing shows "Not recorded" (the `LabeledValue` default) instead of "No record".
- Removed: the `Card` with `surfaceContainerLow`, the `DateRange` icon, the pencil `IconButton` (replaced by the word "Edit" at 48 dp), uppercase titles.
- `PlanTab` already calls the same card; no change there.

## `DeliveredSummary(patient, modifier)` (same signature)

```kotlin
Column(modifier.testTag("deliveredSummary"), verticalArrangement = Arrangement.spacedBy(RecordSpacing.M)) {
    StatusTag(StatusKind.Delivered)
    LabeledValue(
        label = stringResource(R.string.delivery_date_label),
        value = deliveredDateText(patient.deliveryDate),          // null when unknown
        emphasis = true,
        missingText = stringResource(R.string.delivery_date_unknown_value),
        modifier = Modifier.testTag("deliveredHeadline"),
    )
    // mode and outcome: only when present (today's rule)
    when {
        mode != null && result != null -> LabeledValueRow {
            LabeledValue(modeLabel, mode, stretch = true, modifier = Modifier.weight(1f).fillMaxHeight().testTag("deliveredMode"))
            LabeledValue(resultLabel, result, stretch = true, modifier = Modifier.weight(1f).fillMaxHeight().testTag("deliveredResult"))
        }
        mode != null -> LabeledValue(modeLabel, mode, modifier = Modifier.testTag("deliveredMode"))
        result != null -> LabeledValue(resultLabel, result, modifier = Modifier.testTag("deliveredResult"))
    }
}
```
- `modeLabel = stringResource(R.string.delivery_outcome_mode_label)`, `resultLabel = stringResource(R.string.delivery_outcome_result_label)`, `mode = deliveryModeLabel(patient.deliveryMode)`, `result = deliveryResultLabel(patient.deliveryOutcome)` (unchanged helpers).
- New in the same file:
```kotlin
@Composable
fun deliveredDateText(date: String?): String? {
    val parsed = /* same ISO parse as deliveredHeadline */ ?: return null
    val formatted = parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
    val ago = deliveredDaysAgo(date)
    return if (ago != null) "$formatted, $ago" else formatted
}
```
  Share the parse with `deliveredHeadline` through one private `parseDeliveryDate(date): LocalDate?` (one rule, two callers). `deliveredHeadline` and `deliveredDaysAgo` stay: the hub header and the list card use them.
- The unknown date shows "???" in the missing style. `DeliveryPlanSheetTest.deliveredWithoutADateSavesAndShowsTheQuestionMarks` looks for "???" under `deliveredSummary`, so it keeps passing.
- The middle-dot headline "Delivered, date ??? · N days ago" disappears from this card only.

## Acceptance checklist

1. Tags present: `deliveryPlanningCard`, `editDeliveryPlan` (only when `canEdit`), `deliveryHospital`, `deliveryFees`, `deliveryPlan`, `deliveryPostpartum`, and when delivered `deliveredSummary`, `deliveredHeadline`, `deliveredMode` (when mode set), `deliveredResult` (when outcome set).
2. Card: outlined, 16 dp radius, 1 dp `outlineVariant`, no shadow; title 18 sp bold primary; labels 12 sp semibold `FieldDeliveryLabel`; values 16 sp onSurface on `FieldDeliveryBg`.
3. No text in the card contains "EDD", "Blood group", "Current gestational age", "Manual risk", "Fetal presentation" or "Placental site" (existing test).
4. Edit >= 48 by 48 dp; RTL puts the title at the right and Edit at the left.
5. Dark theme uses `FieldDeliveryDark*`.
6. No `uppercase()`, no icon, no em dash, no `Delivery*` token left in `Theme.kt`, no `DeliverySectionBox` or `DeliveryTone` anywhere in `app/src`.
7. 360 dp, font scale 1.3: a 200-character hospital note wraps, is not ellipsised, stays inside the card.
8. Delivered without date shows "Delivered" tag and "???"; delivered with date shows "dd MMM yyyy, N days ago"; mode and outcome hidden when null.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

- `app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt`: `expectedPairs()` lists the eight `Delivery*Text` / `Delivery*Bg` pairs that this slice deletes. Replace them with `FieldDeliveryLabel` / `FieldDeliveryBg`, `FieldDeliveryDarkLabel` / `FieldDeliveryDarkBg`, and keep the `TagAge*` pairs. Rename the test method to match (for example `ageBadge_andDeliveryFamily_pairs_meetWcagAa_4_5`). `deliveryBoxes_placeholder_isFullOpacityToken_notAlphaCopy` reads `ClinicalBadges.kt` for `copy(alpha = 0.x)`; it should still pass, keep it.

### Must pass unchanged

`OverviewTabSectionsTest` (delivery card tests, tag list), `DeliveryPlanSheetTest` (delivered summary tags and "???"), `DeliveryPlanningCardEditGateTest`, `DeliveryPlanTabsEditGateTest`, `DatingGaMarkerSurfacesTest.discordantPatientWithGaOutsideTheWindowShowsNoMarkerOnTheDeliveryPlanCard`, `PatientHubNavigation`, all unit tests including `ParseDeliveryNotesSpecTest` and `DeliveryNotesComposeTest`.

### New test

`app/src/androidTest/java/com/sherifradwan/ancnative/DeliveryCardRedesignTest.kt`:
- the four fields show the labels "Agreed delivery hospital", "Agreed delivery fees", "Delivery plan", "Postpartum note" and their values, each `assertIsDisplayed()` with height >= 44 dp (the round-1 bug was a 0 px box, so assert height, not only existence);
- empty notes show "Not recorded" four times inside the card and never "No record";
- `canEdit = false` hides `editDeliveryPlan`; `canEdit = true` gives it >= 48 by 48 dp;
- delivered with date: "Delivered" tag, a value matching `\d{2} [A-Z][a-z]{2} \d{4}`; delivered without date: "???" under `deliveredHeadline`; mode only: `deliveredMode` exists, `deliveredResult` does not;
- the label "Agreed delivery hospital" exists and "AGREED DELIVERY HOSPITAL" does not (guards against `uppercase()`);
- RTL: title right of Edit by bounds; dark theme (`AncTheme(darkTheme = true)`) renders all four fields displayed.

## Risk

Medium. One level-3 file (deletions only), one token file, one unit test edit. Run `./gradlew --offline :app:testDebugUnitTest -q`, the four delivery emulator classes with `.agents/scripts/run_emulator_test_class.sh`, then the full suite before any push.
