"AB Negative"` correct. Fix: `Regex("(?i)(neg|−|–|-|سالب)\\s*$")`, ideally read Rh from the structured model.

**F10 (O5×O8) Risk emphasis is English-literal.** `riskLevel.contains("High", true)` — `"High Risk"` ✓, but `"عالي الخطورة"`/localised values → not emphasised. Fix: compare a risk enum/`R.string` id, not display text.

**F11 (O9) Dark "AGE" label fails AA.** `ClinicalBadges.kt:58` always uses `TagAgeLabelText` (#FFFFFF); dark label bg is `TagAgeLabelDarkBg` #6366F1 → **4.47:1** (< 4.5 for 11sp bold). No `TagAgeLabelDarkText` exists. Fix: dark label text `#EEF2FF` or bg `#4338CA` (~6.4:1).

**F12 (O9) Placeholders fail AA.** `DeliverySectionBox` uses `c.text.copy(alpha = 0.65f)` on the tinted bg → light theme **~3.1:1** (plan: #538FAF on #E0F2FE) and **~3.4:1** (postpartum). The placeholder is clinical text ("Hospital not agreed yet"). Fix: solid ≥4.5:1 token.

**F13 (O8) Localisation regression.** Existing resources replaced by literals: `current_gestational_age`→"Current GA", `agreed_edd`→"Agreed EDD", `manual_risk`→"Risk", `placental_site`→"Placenta"; plus all box titles/placeholders and "AGE". (Note: repo has no `values-ar/` and `MainActivity` forces `Locale.ENGLISH`, so the "English-only" escape may apply — but dropping existing resources is still a regression.) Fix: new string resources.

**F14 (O7 literal) Delivery edit has a hardcoded content description** `"Edit Delivery Plan"` (not a string resource). Fix: `R.string.edit_delivery_plan`.

**F15 (spec item 3 partial) Inconsistent affordance.** Leftover text edit buttons: `WorkspaceScreen` `editHistories` (~578), `editDating` (~637), `editLinkedScan` `TextButton` (~936); `ProfileScreen` visit/ultrasound/lab still text buttons (1196/1210/1879). Some cards now pencil, siblings still text.

**F16 (a11y) Age badge double-announced.** `semantics { contentDescription = … }` on a non-merging `Surface` leaves child `AGE` / `29 yrs` nodes ⇒ TalkBack reads "Age 29 years" then "AGE" then "29 yrs". Fix: `mergeDescendants = true` or `clearAndSetSemantics`.

**F17 (O6) Age normalisation is suffix-only.** `"29"`, `"29 years"`, `"29 yrs"` ✓; repros that break: `"29 yrs."` → **"29 yrs. yrs"**, `"Age 29"` → **"Age 29 yrs"**, `"29 Years"` → **"29 Years yrs"**, Arabic `"29 عام"` → `"29 عام yrs"`, `"unknown"` → `"unknown yrs"`; CD becomes "Age 29 yrs. years". Fix: `Regex("^\\s*(\\d{1,3})")`, render `"$n yrs"`, else render nothing (and put the helper outside the composable).

### Nits
**F18** `testTag("patientAgeBadge")` on every list row ⇒ `onNodeWithTag` ambiguous in UI tests (previously untagged).
**F19** Emoji inside clinical values/titles (`"🩸 $bloodGroup"`, 🏥 💵 📋 👶) are spoken by TalkBack and leak into value text (exact-text tests/exports); `uppercase()` without explicit locale.
**F20** "Risk" drops the "Manual" qualifier of `manual_risk`; clinician can no longer tell manual from computed.
**F21** Rh/risk/age predicates live inside composables (not pure) ⇒ untestable, which is why O5/O6 have zero tests.

### Gaps in the author's test
No test for: previous-delivery/discharge misfiling (O2's own example); hospital-on-a-fee-line; hospital absent / sentence-as-hospital; **losslessness as a union assertion** (test 1 instead locks in F5 duplication); blank `""`/`"   "` (only `null`); order/duplicate-line preservation; mixed fee+postpartum line. And **no test at all** for O5 (Rh, risk) or O6 (age) — the two oracles with the most hand-rolled string logic. `s.fees!!` / `s.plan!!` assertions are brittle.

### Could not verify
Compilation/render (no gradle run; diff + 8 files only); whether `ProfileScreen`/`WorkspaceScreen` retain unused imports or other removed content outside the hunks; whether additional edit actions exist elsewhere (attachments, dating); on-device contrast/font metrics and emoji rendering; RTL layout without `values-ar`; whether live data contains `O −`/Arabic blood groups; DAO/backup round-trip of `ageText`; partial-application of item 3 in files I did not read.