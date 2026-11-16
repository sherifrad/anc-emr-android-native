# Adversarial review: delivery card + age badge + pencil edit (uncommitted)
Inputs: SPEC.md, uncommitted.diff, ClinicalBadges.kt.txt, DeliveryNotesParserTest.kt.txt only. Verdict: NOT mergeable. 5 Hard Blockers.
Outputs below come from running `parseDeliveryNotes` by hand (diff lines 185-206). Contrast was computed with the WCAG formula.

## Hard Blockers
- **HB1 (O2, O1) Substring "fee" sends clinical lines to Fees.** WorkspaceScreen.kt `parseDeliveryNotes`, the `fees` filter. "fee" matches inside "feel" and "breastfeed".
  Input `"Feels reduced fetal movements since yesterday"` gives fees=`"Feels reduced fetal movements since yesterday"` and plan=null. A red-flag symptom appears only under "Agreed Delivery Fees", and the Plan box says "No delivery plan recorded yet".
  `"Wishes to exclusively breastfeed"` also goes to fees. Fix: match whole words (`\bfees?\b`, `\bEGP\b`, `\bcost\b`), and only on lines that are not clinical.
- **HB2 (O2) Past events and the current PPH plan go to Postpartum.** The `postpartum` filter matches "delivered", "discharged" and "postpartum" anywhere in a line, and `stripPrefix` removes the context.
  `"Previous pregnancy: delivered by CS 2019"` gives postpartum=`"delivered by CS 2019"`. "Previous pregnancy" is dropped, so it reads as the current birth.
  `"Previous postpartum haemorrhage 2020 - oxytocin infusion ready"` and `"Admitted 28w for APH, discharged day 3"` also go to Postpartum and are missing from Delivery Plan. So the PPH prophylaxis plan is hidden from the plan.
  Fix: never use "postpartum" alone as the trigger. Require an explicit `Postpartum note:` line prefix. Skip lines that start with or contain previous/prior/history/G?P?/a year.
- **HB3 (O1) `stripPrefix` drops text silently.** It deletes everything before the first ':' on Fees and Postpartum lines.
  `"Reviewed 10:30, fee 15,000 EGP"` gives fees=`"30, fee 15,000 EGP"`, so the time and part of the content are gone.
  `"Fee quoted (NOT agreed): 15,000 EGP"` gives `"15,000 EGP"` under the heading **Agreed** Delivery Fees, which turns a quote into an agreed fee (an administrative error).
  Fix: strip only a known label (`^(delivery )?fee[^:]*:|^postpartum note:`), or keep the whole line.
- **HB4 (O3, O2) Hospital extraction returns non-hospital text, or misses the hospital.** The `hospital` block uses `lastIndexOf(" at ")`.
  `"Elective CS at Cleopatra Hospital at 39 weeks"` gives hospital=`"39 weeks"`.
  `"Hospitalised at 28w for pyelonephritis"` gives hospital=`"28w for pyelonephritis"`.
  `"Patient declines hospital birth"` gives hospital=the whole sentence.
  `"SVD at Cleopatra Hospital, fee 15,000 EGP"`: the line goes to fees first, so hospital=null and the card shows "Hospital not agreed yet" when a hospital is agreed.
  Fix: search all lines, fee lines included. Extract with a pattern such as `([A-Z][\w' ]+ (Maternity )?Hospital)`. Return null when nothing matches; never return the whole sentence.
- **HB5 (O4) The delivery-planning notice was removed.** `Text(stringResource(R.string.delivery_planning_notice))` was deleted (diff lines 253-257) and nothing replaces it. Fix: add it back under the header.

## Should-fix
- **S1 (O4, O8) Localised labels replaced with hard-coded English.** `R.string.agreed_edd`, `current_gestational_age`, `manual_risk`, `fetal_presentation` and `placental_site` became the literals "Agreed EDD", "Current GA", "Risk", "Presentation" and "Placenta" (diff lines 275-302). Arabic users lose these labels.
  "manual_risk" becoming "Risk" also drops the *manual* qualifier. Fix: keep the existing R.string ids.
- **S2 (O8) New user-visible English literals** (no Arabic, no plurals):
  - The 4 section titles and 4 placeholders.
  - The delivery edit icon's contentDescription "Edit Delivery Plan". This also breaks O7's "from a string resource" requirement.
  - In ClinicalBadges.kt (lines 51, 55, 64): "AGE", "yrs", "Age $cleanAge years".
  - In KeyFactTile: `label.uppercase()` and `title.uppercase()` use the default locale.
- **S3 (O1) Notes written as one paragraph collapse into one box.** The parser splits on newlines only.
  `"SVD planned at Cleopatra Hospital. Fee 15,000 EGP agreed."` (one line) puts the whole note in Fees. Plan and hospital are null.
  The old card showed the full notes under "Delivery Plan & Clinical Notes", so this is a regression. Arabic notes never match any keyword. Fix: show the raw notes below the boxes, collapsed, or split into sentences.
- **S4 (O1) Hospital is shown twice.** The hospital line stays in `rest`, so it appears in both Hospital and Plan. This does not meet the "exactly one section" requirement. Either document this as intentional extraction or accept it explicitly in the oracle.
- **S5 (O5) Rh-negative detection has false negatives and false positives** (DeliveryPlanningCard `isRhNegative`).
  - False negatives: `"O -ve"`, `"B Rh -ve"`, `"O −"` (U+2212), `"A- (anti-D given)"`, Arabic "سالب".
  - False positives: `"A+ , antibody screen negative"` and `"O+ (husband Rh neg)"`.
  - The emphasis is colour only (`colorScheme.error`), with no text or icon cue.
  - Fix: normalise the value, match `(-|−|neg|-ve)` only in the Rh token, and add an "Rh−" text marker.
- **S6 (O5) Risk emphasis uses substring "High"**, so `"Not high risk"` is emphasised. Fix: compare against the risk enum or values.
- **S7 (O6) Age suffix handling is case-sensitive and incomplete.** It works for "29", "29 years" and "29 yrs".
  - "29 Years" → "29 Years yrs", "29 yr" → "29 yr yrs", "29 years old" → "29 years old yrs".
  - ageText="yrs" passes the blank check and renders " yrs".
  - Fix: `Regex("\\s*(years?|yrs?|y)\\.?\\s*(old)?$", IGNORE_CASE)`, then check blank again after stripping.
- **S8 (a11y) The age badge is announced twice.** `semantics{contentDescription}` on the Surface leaves the child Texts exposed, so TalkBack reads "Age 29 years", "AGE", "29 yrs". Fix: `clearAndSetSemantics`.
- **S9 (O9) Contrast fails AA (4.5:1).**
  - Dark "AGE" label: white on #6366F1 = **4.47**.
  - Placeholder text at alpha 0.65 in light mode: hospital 3.46, fee 3.03, plan 3.09, postpartum 3.43. These placeholders carry clinical meaning ("Hospital not agreed yet").
  - Fix: TagAgeLabelDarkBg #4F46E5. Use a placeholder alpha of at least 0.85 in light mode, or italic text at full colour.
- **S10 (RTL) Profile age-badge alignment.** The old Text set `textAlign = if (isArabic) End else Start`, which suggests the app aligns RTL by hand. The badge has no alignment, so in Arabic it may sit on the wrong side.
  Also, a blank `ageText` (not null) still adds a 6dp Spacer.
- **S11 (O7) Icon-only buttons lose visible meaning.** "Correct previous pregnancy" (a correction, not an edit) now looks the same as every other pencil. Consider a tooltip, or keep the text for corrections.

## Nits
- N1: The `🩸` emoji is added to the blood-group value. TalkBack reads it as "drop of blood", and any UI test that asserts the text "A-" will now fail.
- N2: "Agreed EDD" falls back to `ultrasoundEdd`. This was already the case before the change, but the label now claims "Agreed" more prominently.
- N3: "agreement" is no longer a fee keyword. `"Financial agreement signed"` now goes to Plan.
- N4: Placeholder text says "Agreed ..." even when a section holds "Discussed cost of CS vs SVD, undecided".

## Gaps in the author's test (DeliveryNotesParserTest)
- Only 3 cases. There is no test for O1 lossless-ness: no check that the union of the sections equals the input lines.
- No coverage of the cases above: "feel/breastfeed", previous-history "delivered", PPH plan lines, a ':' inside text, multiple " at ", a hospital mentioned in a fee line, one-paragraph notes, Arabic notes, blank or whitespace-only input, \r\n, line order inside a section.
- `fees!!.startsWith` and `plan!!.contains` are weak assertions. They would pass even if text were lost.
- Nothing tests the age badge, Rh detection, risk emphasis or the presence of the notice.

## Could NOT verify from this input
- What `minimumTouchTarget()` does, and whether the theme overrides `LocalMinimumInteractiveComponentSize`. M3 `IconButton` is normally 48dp, so O7's touch-target check is probably met but unconfirmed.
- That the PatientListScreen call site still calls `PatientAgeBadge` (it now resolves to the public one).
- Whether strings.xml/values-ar contain `edit_*` and `correct_previous_pregnancy`.
- Whether the app sets LayoutDirection RTL.
- The colour of `surfaceContainerLow` behind the section titles, so title contrast is unknown.
- The real formats of `bloodGroupText`, `riskLevel`, `ageText` and the notes in stored data.
- Whether existing UI tests assert the removed texts ("Edit Delivery Plan", "Delivery Fee & Financial Agreement", the notice).
- Whether `DeliveryPlanningCard`'s edit action was reachable another way.
- Compilation: imports for IconButton, heading and remember are outside the hunks.
