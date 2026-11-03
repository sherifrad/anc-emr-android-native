t Delivery Plan"` content description (unsourced, unlike the other five pencil buttons which correctly use `stringResource`), and `ClinicalBadges.kt` "AGE"/"yrs"/`"Age $cleanAge years"` are all literal English. In Arabic/RTL the card will render LTR labels and an English VO description; `.uppercase()` on labels also does nothing useful for Arabic. Fix: add `strings.xml` entries (en + ar) for every label, placeholder, and content description; drop `.uppercase()` in favour of locale-aware styled label.

**F7 — Should-fix — O5: Rh-negative detection false-positives and dose-related misses.**
`bloodGroupText.trim().endsWith("-") || contains("neg", true)`: "Group B pending typing" contains "neg" → wrongly emphasised red as Rh-negative (serious in OB). Conversely "O−" (U+2212) or "O minus" fail both checks → not emphasised. Risk: `contains("High")` also matches "highlight"/"History"; Arabic risk text ("خطر مرتفع") never emphasised; non-"High" variants ("Severe", risk list) unconsidered. Fix: match structured enum/token set for Rh (`-`, `neg`, `negative` as whole tokens, Unicode minus) and risk levels by enum value, not substrings.

**F8 — Should-fix — O6: age suffix stripping is case-sensitive and incomplete.**
`removeSuffix("years").removeSuffix("yrs")`: inputs "29 YRS", "24Years", "29 yr", "29 years." all fail → badge renders "29 YRS yrs", "24Years yrs", etc. Fix: lowercase-then-regex-strip (`Regex("(?i)\\s*(years?|yrs?|yr)\\p{Punct}?\\z")`).

**F9 — Should-fix — O9: contrast suspects.**
(a) Dark-mode `TagAgeLabelText` (white) on `TagAgeLabelDarkBg` #6366F1 ≈ 4.48:1 — below AA 4.5 for normal text; labelSmall at that size would also need 4.5. (b) Placeholder text = `c.text.copy(alpha = 0.65f)` over tinted bg (e.g. #6B21A8 on #F3E8FF at 65% alpha) blends to roughly ~3–4:1 — likely AA fail. Fix: #4348E0-ish dark label bg; use a dedicated disabled-text token at full alpha (e.g. `onSurface` 60% is still non-compliant — use a vetted token).

**F10 — Nit — profile header spacing with blank-but-nonnull age.**
`if (patient.ageText != null)` adds `Spacer(6.dp)` even when `ageText = " "` (badge renders nothing). Use `!patient.ageText.isNullOrBlank()`.

**F11 — Nit — emoji in clinical values and CD.** `"🩸 $it"`, 🏥💵📋👶 are read aloud in odd ways by screen readers and render inconsistently; treat as decoration only (attached to labels, not to the value string) or move to icons with proper semantics.

**F12 — Nit — `IntrinsicSize` import grouped after `height` etc., cosmetic ordering in ClinicalBadges.kt.

## Author's test gaps (vs O1–O3)
- No case where hospital + fee share one line (F2) — the oracle's own example is untested.
- No history-with-"delivered/discharged" line (F3).
- Asserts `startsWith`/`contains` instead of exact equality → cannot catch duplication (F5), label-suffix stripping, or section ordering; O1's "order preserved" assertion is implicit only.
- No blank-string input ("   ") case; no multi-line single-section ordering test; no case of a `cost/package`-containing clinical line (misfiled to Fees); no test that a `package` line without fee digits still lands correctly.
- Nothing tests badge text ("yrs yrs") — acceptable as README-run, but a pure `cleanAge`-style function should be extracted to make O6 unit-testable.

## Could NOT verify (read-only scope)
- That call sites in `PatientListScreen.kt` still render `PatientAgeBadge` after the private version was deleted — the diff shows no re-usage; list-badge presence (O6) is UNKNOWN.
- `PencilEditButton` height: `IconButton` default is 48dp, but `minimumTouchTarget()` was dropped — presumed OK, not measured.
- `FactRow` vs `KeyFactTile` "—" placeholder parity (old FactRow behaviour unknown to me).
- Whether `R.string.*` keys for the removed notice still exist (unused-resource state) and actual app strings.xml content (Arabic keys).
- Runtime WCAG contrast (my ratios are hand-computed, ±0.5).
- Compile status of the new file vs diff as one tree (can't see full WorkspaceScreen).