# Slice 0: foundation tokens and shared components

Builder brief, self-contained. Read `docs/designs/redesign/PLAN.md` sections 0 and 2 for the why; this file is the what.

## Goal

Add the mock D design system to the app without changing any screen. After this slice the app looks exactly as before. Slices 1 and later consume these pieces.

## Branch and hygiene

- Branch `redesign/slice-00-foundation` from `main`.
- Run `.agents/scripts/git_audit.sh` before starting and before any push.
- Update `.agents/pipeline/HANDOFF_AGY.md` (mission table, artifact index, log line) in the same commit.
- Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`.

## Files (base path `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/Theme.kt` | append colour tokens only (exists, 375 lines) |
| `ui/components/RecordTokens.kt` | new |
| `ui/components/LabeledValue.kt` | new |
| `ui/components/RecordSectionCard.kt` | new |
| `ui/components/StatusTag.kt` | new |
| `app/src/main/res/values/strings.xml` | add 3 strings |

Do not edit `ClinicalTypography`, the colour schemes, `DesignSystem.kt`, `ClinicalBadges.kt` or any screen.

## 1. `Theme.kt`: append tokens

Append after the existing `Delivery*Dark*` block, same style (`val Name = Color(0xFF......)`, top level, public) so `ThemeContrastTest`-style parsing works.

```kotlin
// Record redesign (mock D): field families. Light.
val FieldPatientBg = Color(0xFFE9EEF8)
val FieldPatientLabel = Color(0xFF23407A)
val FieldObstetricBg = Color(0xFFE3F0EE)
val FieldObstetricLabel = Color(0xFF1D5149)
val FieldBackgroundBg = Color(0xFFF1EDE4)
val FieldBackgroundLabel = Color(0xFF594A2C)
val FieldDeliveryBg = Color(0xFFEEE9F5)
val FieldDeliveryLabel = Color(0xFF4E3A78)
// Dark.
val FieldPatientDarkBg = Color(0xFF22304A)
val FieldPatientDarkLabel = Color(0xFFAFC6F2)
val FieldObstetricDarkBg = Color(0xFF1D3836)
val FieldObstetricDarkLabel = Color(0xFF9FD6CC)
val FieldBackgroundDarkBg = Color(0xFF36301F)
val FieldBackgroundDarkLabel = Color(0xFFE0CFA6)
val FieldDeliveryDarkBg = Color(0xFF2E2842)
val FieldDeliveryDarkLabel = Color(0xFFCDBDF0)

// Record redesign: status tags. Light.
val StatusIncompleteBg = Color(0xFFEEF0F4)
val StatusIncompleteBorder = Color(0xFF8C95A5)
val StatusIncompleteText = Color(0xFF333B4A)
val StatusModerateBg = Color(0xFFFCEFD6)
val StatusModerateBorder = Color(0xFFB9862C)
val StatusModerateText = Color(0xFF6E4708)
val StatusHighBg = Color(0xFFFBE3E3)
val StatusHighBorder = Color(0xFFC26666)
val StatusHighText = Color(0xFF8E1B1B)
// Dark.
val StatusIncompleteDarkBg = Color(0xFF2A3140)
val StatusIncompleteDarkBorder = Color(0xFF5B6578)
val StatusIncompleteDarkText = Color(0xFFD5DAE3)
val StatusModerateDarkBg = Color(0xFF3A2A0E)
val StatusModerateDarkBorder = Color(0xFFA87A2A)
val StatusModerateDarkText = Color(0xFFF3D18E)
val StatusHighDarkBg = Color(0xFF4A1616)
val StatusHighDarkBorder = Color(0xFFB45A5A)
val StatusHighDarkText = Color(0xFFFFC9C9)
```

## 2. `ui/components/RecordTokens.kt`

Package `com.sherifradwan.ancnative.ui.components`.

```kotlin
@Immutable
data class FieldFamily(val container: Color, val label: Color)

object FieldFamilies {
    @Composable fun patient() = if (isDarkTheme()) FieldFamily(FieldPatientDarkBg, FieldPatientDarkLabel) else FieldFamily(FieldPatientBg, FieldPatientLabel)
    @Composable fun obstetric() = ...   // same pattern
    @Composable fun background() = ...
    @Composable fun delivery() = ...
}

/** Family for fields inside a RecordSectionCard. Default: patient. */
val LocalFieldFamily = compositionLocalOf<FieldFamily?> { null }

@Composable
fun currentFieldFamily(): FieldFamily = LocalFieldFamily.current ?: FieldFamilies.patient()

object RecordShapes {
    val Small = RoundedCornerShape(10.dp)
    val Large = RoundedCornerShape(16.dp)
}

object RecordSpacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
}

object RecordType {
    val SectionTitle = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
    val FieldLabel = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val FieldValue = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val KeyValue = FieldValue.copy(fontWeight = FontWeight.SemiBold)
    val Missing = FieldValue.copy(fontStyle = FontStyle.Italic)
    val Secondary = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val Action = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val Tag = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val CardName = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
    val MetricLabel = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
    val MetricValue = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
}
```

Also in this file:

```kotlin
/** Keeps codes and numbers (A+, 110/70, G4 P1 A2, dates) in left-to-right order inside RTL text. */
fun ltrIsolate(text: String): String = "⁦$text⁩"
```
Without it `A+` shows as `+A` in an RTL layout (mock B and mock A had this bug).

Text styles carry no colour; colours come from the call site (rules in each component below). `FontFamily.Default` is inherited.

## 3. `ui/components/LabeledValue.kt`

```kotlin
@Composable
fun LabeledValue(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
    family: FieldFamily = currentFieldFamily(),
    missingText: String = stringResource(R.string.not_recorded),
    boxTag: String? = null,
)
```

- Layout: `Column(modifier, verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS))`.
- Label: `Text(label, style = RecordType.FieldLabel, color = family.label)`.
- Box: `Surface(shape = RecordShapes.Small, color = family.container, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).then(boxTag tag if given))`, content `Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart)`.
- Value present: `Text(value, style = if (emphasis) RecordType.KeyValue else RecordType.FieldValue, color = colorScheme.onSurface)`. No `maxLines`; values wrap.
- Value null or blank: `Text(missingText, style = RecordType.Missing, color = colorScheme.onSurfaceVariant)`.
- Semantics: `Modifier.semantics(mergeDescendants = true) {}` on the Column so TalkBack reads "label, value" once.
- Not clickable, no 48 dp forcing.

```kotlin
/** Equal-width fields side by side (pairs, the G P A triple). */
@Composable
fun LabeledValueRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit)
```
- `Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(RecordSpacing.S))`. Callers pass `Modifier.weight(1f).fillMaxHeight()` to each `LabeledValue` so boxes in one row are equal height. Document this in KDoc.

```kotlin
/** Tappable record (previous pregnancy, later visits and scans). */
@Composable
fun RecordRow(
    onClick: (() -> Unit)?,
    onClickLabel: String?,
    modifier: Modifier = Modifier,
    family: FieldFamily = currentFieldFamily(),
    clickTag: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
)
```
- `Surface(shape = RecordShapes.Small, color = family.container, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp))`.
- Inside: `Row(verticalAlignment = Alignment.CenterVertically)`; first child `Column(Modifier.weight(1f).then(if (onClick != null) Modifier.clickable(...).minimumTouchTarget() else Modifier).then(clickTag?.let { Modifier.testTag(it) } ?: Modifier).padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp), content)`; then `trailing` if given. `clickTag` exists so callers can keep tags such as `editPregnancy-<id>` on the clickable node.
- The clickable area is the content column, so a trailing button (delete) keeps its own target. Use `Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)`.

## 4. `ui/components/RecordSectionCard.kt`

```kotlin
@Composable
fun RecordSectionCard(
    title: String,
    family: FieldFamily,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
)
```
- `OutlinedCard(modifier.fillMaxWidth(), shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface), border = BorderStroke(1.dp, colorScheme.outlineVariant))`. No elevation, no shadow.
- `CompositionLocalProvider(LocalFieldFamily provides family)` around the body.
- Body: `Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(RecordSpacing.M))`.
- Header: `Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically)`:
  - `Text(title, style = RecordType.SectionTitle, color = colorScheme.primary, modifier = Modifier.weight(1f).padding(vertical = 6.dp).semantics { heading() })`. Wraps, never ellipsised.
  - `action?.invoke()`. The 12 dp shift that lines the action word up with the card edge lives inside `SectionTextAction` (below). Use plain `Modifier.offset`, which Compose mirrors in RTL; never `absoluteOffset`.
- Then `content()`.

```kotlin
@Composable
fun SectionTextAction(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```
- `TextButton(onClick, modifier = modifier.minimumTouchTarget().offset(x = 12.dp).semantics { this.contentDescription = contentDescription }, shape = RecordShapes.Small, contentPadding = PaddingValues(horizontal = 12.dp))`, `Text(text, style = RecordType.Action, color = colorScheme.primary)`.
- Callers pass the existing test tag in `modifier` (for example `Modifier.testTag("editDating")`).

## 5. `ui/components/StatusTag.kt`

```kotlin
enum class StatusKind { HighRisk, ModerateRisk, Incomplete, Delivered }

@Composable
fun StatusTag(kind: StatusKind, modifier: Modifier = Modifier, text: String = defaultText(kind))
```
- Default words: HighRisk `R.string.high_risk`, ModerateRisk `R.string.moderate_risk`, Incomplete `R.string.registration_incomplete_chip`, Delivered `R.string.status_delivered` (new, below).
- Colours (light / dark through `isDarkTheme()`): HighRisk `StatusHigh*`, ModerateRisk `StatusModerate*`, Incomplete `StatusIncomplete*`, Delivered container `colorScheme.surface`, border and text `colorScheme.primary`.
- `Surface(shape = RecordShapes.Small, color = bg, border = BorderStroke(1.dp, border), modifier = modifier.heightIn(min = 24.dp))`, `Text(text, style = RecordType.Tag.copy(fontWeight = if (kind == HighRisk) FontWeight.Bold else FontWeight.SemiBold), color = fg, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))`.
- No icon. Not clickable by itself; a caller that needs a tap wraps it (slice 1B does this for Incomplete).

## 6. Strings (`res/values/strings.xml`)

Add:
```xml
<string name="status_delivered">Delivered</string>
<string name="field_label_due_date">Due date</string>
<string name="field_label_bp">BP</string>
```
Reuse, do not duplicate: `ga_label` (GA), `parity` (Parity), `blood_group` (Blood group), `not_recorded`, `pending`, `date_not_recorded` already exist.
(`status_delivered` is needed by `StatusTag`; the other two are used by slice 1B and added here so slice 1B does not race on the file. If a lane prefers, they may move to slice 1B; only one lane edits `strings.xml` at a time.)

Do not change `high_risk` here; slice 1B does.

## 7. Acceptance checklist (testable)

1. `./gradlew --offline :app:testDebugUnitTest -q` passes; `assembleDebug` and lint pass.
2. No file outside the six listed changes (`git diff --stat main`).
3. No screen changes: the full emulator suite `.agents/scripts/run_emulator_all_tests.sh` reports the same pass count as `main` (known flaky tests per HANDOFF N0 re-run alone).
4. Every token pair in PLAN.md 2.1 meets its ratio (unit test below).
5. `LabeledValue` box is at least 44 dp tall; `SectionTextAction` and `RecordRow` are at least 48 by 48 dp and 56 dp tall; at 360 dp and font scale 1.3 a 60-character value wraps and is not ellipsised.
6. In RTL (`LocalLayoutDirection provides Rtl`), the section title sits at the right and the action at the left; label and value start at the right.
7. Light and dark both render (`AncTheme(darkTheme = false)` and `AncTheme(darkTheme = true)`; the flag exists in `Theme.kt`).
8. No `uppercase()`, no em dash, no icon in the four new files (grep).

## 8. Tests (written by a different lane from this spec)

- Unit `app/src/test/java/com/sherifradwan/ancnative/ui/RecordTokensContrastTest.kt`: parse `Theme.kt` the way `ThemeContrastTest` does, then assert each pair: every `Field*Label` on its `Field*Bg` >= 4.5 and on `#FFFFFF` (light) / `#1A202E` (dark) >= 4.5; `#0D1B3D` (light onSurface) and `#E2E2EA` (dark onSurface) on every `Field*Bg` of the same theme >= 4.5; `#4C3F43` / `#CDC3C6` (onSurfaceVariant) on every `Field*Bg` >= 4.5; each `Status*Text` on its `Status*Bg` >= 4.5; each light `Status*Border` on `#FFFFFF` >= 3.0.
- Emulator `app/src/androidTest/java/com/sherifradwan/ancnative/RecordComponentsTest.kt`: render each component in `AncTheme` at 360 dp and font scale 1.3; assert sizes (checklist 5), RTL placement (checklist 6) by bounds, missing-value text "Not recorded" for null and blank, `StatusTag` words for each kind, merged semantics on `LabeledValue` (one node holds both texts), `heading()` on the section title.
- Run one class: `.agents/scripts/run_emulator_test_class.sh com.sherifradwan.ancnative.RecordComponentsTest`.

## 9. Risk

Low. Append-only in `Theme.kt`, new files elsewhere, no level-3 path, no behaviour. The only shared file is `strings.xml`.
