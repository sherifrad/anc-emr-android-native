 hard-coded English (breaks Arabic/RTL)** — `WorkspaceScreen.kt:1384,1402-1434,1445-1479`, `ClinicalBadges.kt:51,55,64`, `ProfileScreen.kt:49` (O8)
  - *Input:* Switch app locale to Arabic (`ar`).
  - *Why it matters:* Replaces `R.string.current_gestational_age`, `agreed_edd`, `manual_risk`, `fetal_presentation`, `placental_site` with English literals; hard-codes all 4 `DeliverySectionBox` titles/placeholders, `"Edit Delivery Plan"`, and `"AGE"` / `"$cleanAge yrs"` / `"Age $cleanAge years"`; drops RTL alignment (`TextAlign.End`) on profile age.
  - *Minimal fix:* Restore `stringResource(...)` on `KeyFactTile` labels and move all new badge/section/accessibility strings to `strings.xml` (with Arabic translations).

- **F6 [Should-fix] Rh-negative & High-risk emphasis misses parenthesized/Unicode/localized values** — `WorkspaceScreen.kt:215-216,1417` (O5)
  - *Input:* `bloodGroupText = "O Rh(-)"` or `"A−"` (U+2212), or Arabic `riskLevel = "مرتفع"` → `emphasize = false`.
  - *Why it matters:* Missed Rh-negative status risks omitting anti-D prophylaxis; localized high-risk strings are unemphasized.
  - *Minimal fix:* Match `Regex("""(?i)(\bneg|[-−]\s*\)?$)""")` (excluding `"pending"`) and check canonical risk enum/localized high-risk resource.

- **F7 [Should-fix] `PatientAgeBadge` fails on case/singular suffixes and unit-only strings** — `ClinicalBadges.kt:36,42` (O6)
  - *Input:* `ageText = "29 Years"`, `"29 yr"`, or `"yrs"` → renders `"29 Years yrs"`, `"29 yr yrs"`, or `" yrs"`.
  - *Why it matters:* `removeSuffix` is case-sensitive and misses singulars; `"yrs"` passes `isNullOrBlank()` and renders an empty age.
  - *Minimal fix:* `val cleanAge = ageText.replace(Regex("(?i)\\b(years?|yrs?)\\b"), "").trim(); if (cleanAge.isEmpty()) return`.

- **F8 [Should-fix] Edit buttons lost `minimumTouchTarget()` and `PatientAgeBadge` semantics don't merge** — `WorkspaceScreen.kt:1380,405`, `ClinicalBadges.kt:51` (O7, A11y)
  - *Input:* Inspect `editDeliveryPlan` / `PencilEditButton` bounds or focus `PatientAgeBadge` with TalkBack.
  - *Why it matters:* Removing `.minimumTouchTarget()` risks <48dp targets if parent constraints shrink; unmerged `Surface` semantics read `"Age 29 years"` then `"AGE"`, `"29 yrs"`.
  - *Minimal fix:* Use `PencilEditButton` with `Modifier.minimumTouchTarget().testTag(tag)` for all edit actions (including `editDeliveryPlan`), and set `semantics(mergeDescendants = true)` + `clearAndSetSemantics` on children in `PatientAgeBadge`.

- **F9 [Should-fix] Dark-mode `AGE` label and all `DeliverySectionBox` placeholders fail WCAG AA 4.5:1 contrast** — `Theme.kt:73,101`, `ClinicalBadges.kt:124` (O9)
  - *Input:* Dark theme `PatientAgeBadge` (`#FFFFFF` on `#6366F1` = **4.47:1**); Light theme `DeliverySectionBox` placeholders (`c.text.copy(alpha = 0.65f)` on `c.bg` = **2.98:1–3.31:1**).
  - *Why it matters:* Violates O9 (WCAG AA ≥ 4.5:1 for normal text).
  - *Minimal fix:* Darken `TagAgeLabelDarkBg` to `Color(0xFF4F46E5)` (5.9:1) and remove `.copy(alpha = 0.65f)` on placeholder text (or use dedicated ≥4.5:1 muted tokens).

- **F10 [Should-fix] `DeliveryNotesParserTest` misses oracle edge cases** — `DeliveryNotesParserTest.kt:10-34`
  - *Input:* `./gradlew test` passes despite F1–F3.
  - *Why it matters:* Assertions use weak `startsWith`/`contains` (hiding duplicate hospital line in `plan` in test 1!) and omit blank/whitespace inputs, multi-line order preservation, historical `"delivered"` notes, mixed hospital+fee lines, and `"Breastfeeding"` false positives.
  - *Minimal fix:* Assert exact `s.plan` equality in `splitsMayaNotesIntoFourSections` and add tests for O1–O3 failure cases above.

### 3. What Could Not Be Verified
- Whether `PatientListScreen` still invokes `PatientAgeBadge` (call site sits outside diff hunk) and whether any other edit buttons exist outside diff context.
- Actual runtime values of `patient.bloodGroupText`, `patient.riskLevel`, and `R.string.*` across locales without reading full source/resource files.