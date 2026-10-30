# Fix spec (round 2) - owner decisions D1-D4 + round-1 review findings
Project: Android EMR (Kotlin, Compose) for a private obstetrics clinic. Wrong, hidden or misfiled clinical data = serious failure.
Base: the current working tree (round 1 complete, all P1-P10 green). Round-1 oracles P1-P10 in SPEC_FIX.md STILL HOLD unless an oracle below explicitly supersedes one.
English-only app. Owner decisions: OWNER_DECISIONS.md. Package com.sherifradwan.ancnative.ui.
Safety rule for every ambiguity: prefer a false negative that keeps the text visible in Plan over a false positive that puts text in the wrong box.

## Interface contract (names fixed so tests can be written first)
ui/DeliveryNotesParser.kt (unchanged signature): internal fun parseDeliveryNotes(notes: String?): DeliveryNoteSections
ui/ClinicalFormat.kt, existing: isRhNegative, isHighRisk, formatAgeYears. NEW:
  internal fun isRhPositive(bloodGroup: String?): Boolean          // mirror of isRhNegative ("A+", "O pos", "B Positive"); false for negative/blank/null/"pending"
  internal fun formatBloodGroup(bloodGroup: String?): String?       // trimmed value; if isRhNegative -> value + " (Rh negative)"; blank/null -> null. Never adds emoji.

## Parser oracles (supersede P2/P4/P5 where stated)
Q1 Segments. Each line is split into segments at ";" and at a sentence end ". " followed by an uppercase letter, EXCEPT after the abbreviations Dr, Mr, Mrs, Ms, St, No, approx, vs, e.g, i.e (case-insensitive). Decimals ("2.5 mg") and times ("10:30") never split. Each segment is classified on its own with the round-1 rules (label, history, current delivery, fee, else plan).
Q2 Lossless rejoin (supersedes P5 line-level wording). Consecutive segments of the same source line that land in the same section are rejoined with their ORIGINAL separator, so an unsplit line reads exactly as typed. Different source lines in a section are joined with "\n". Every non-empty segment appears in exactly one home section; order kept; nothing invented; only a known label prefix is stripped (P3).
   "Elective CS at 39w. Avoid NSAIDs." -> plan == "Elective CS at 39w. Avoid NSAIDs." (exactly)
   "Fee 15,000 EGP; penicillin allergy - avoid" -> fees == "Fee 15,000 EGP", plan == "penicillin allergy - avoid"
   "Elective CS at 39w at Cleopatra Hospital. Fee 20,000 EGP agreed. Postpartum: enoxaparin 10 days." -> plan == "Elective CS at 39w at Cleopatra Hospital.", fees == "Fee 20,000 EGP agreed.", postpartum == "enoxaparin 10 days.", hospital == "Cleopatra Hospital"
   "Dr. Ahmed to attend. Fee 2.5k EGP" -> plan == "Dr. Ahmed to attend.", fees == "Fee 2.5k EGP"
Q3 Tense (fixes R2-1; refines P2). An UNLABELLED segment goes to postpartum only if it has whole-word delivered/discharged AND no history marker AND no plan/future marker: will, shall, "to be", plan, planned, planning, expected, expect, booked, scheduled, if, should, would, may, aim, intend, intended. Otherwise it follows the remaining rules (normally plan).
   "Will be delivered by elective CS at 39w" -> plan, postpartum null
   "Expected to be discharged day 2 after CS" -> plan, postpartum null
   "Delivered 12/10 by CS, baby well" -> postpartum (unchanged)
   "Discharged day 3; plan review at 6 weeks" -> postpartum == "Discharged day 3", plan == "plan review at 6 weeks"
   "Postpartum: will review at 6 weeks" -> postpartum == "will review at 6 weeks" (a label always wins; R2-6 accepted as designed)
Q4 Hospital (fixes R2-2; refines P4). Hospital box = distinct names joined "\n", null if none. Two sources:
   (a) Labelled line "Hospital: <value>" (label case-insensitive): name = value up to the first ";" "," "." or " - " (trimmed), kept as typed. null if the value is empty or undecided: tbd, "to be decided", "not decided", undecided, "not agreed", "?", "none" (whole-word, case-insensitive). The source line stays in plan (D2: full line in its home section, name in the hospital box).
   (b) Unlabelled phrase: capitalised words ending in "Hospital" (capital H only), optionally Maternity/Medical before it; lowercase connector particles al, el, de, of, and are allowed BETWEEN capitalised words. Leading words from this list are dropped from the name: At, In, To, For, From, Via, Booked, Book, Booking, Planned, Plan, Delivery, Deliver, Admit, Admitted, Admission, Refer, Referred, Transfer, Transferred, Prefers, Preferred, Patient, Agreed.
   "Hospital: Dar Al Fouad" -> "Dar Al Fouad"          "hospital: Dar Al Fouad; arrive fasting" -> "Dar Al Fouad"
   "Hospital: TBD" -> null                              "Hospital: not decided yet" -> null
   "Booked Cleopatra Maternity Hospital" -> "Cleopatra Maternity Hospital"
   "Delivery At Dar Al Fouad Hospital" -> "Dar Al Fouad Hospital"     "CS at Dar al Fouad Hospital" -> "Dar al Fouad Hospital"
   "Delivery at Cleopatra hospital" -> hospital null, line in plan (lowercase hospital unlabelled is NOT accepted: "Full hospital stay" must never become a name)
   "Delivery fee package agreed: 15,000 EGP (Full hospital stay + pediatrician attendance)." -> hospital null from this line
   Round-1 negatives still null: "declines hospital birth", "Hospitalised at 28w", "39 weeks", times.
   The existing author test DeliveryNotesParserTest (Maya note -> hospital "Cleopatra Maternity Hospital") must still pass unchanged.

## Clinical format oracles
Q5 Age (fixes R2-4; supersedes P8 extraction). formatAgeYears returns the number only when the WHOLE trimmed text matches: optional "Age" + optional ":" , an integer, optional unit y|yr|yrs|year|years with optional ".", case-insensitive, AND 10 <= n <= 70. Else null.
   -> "29": "29","29 years","29 Years","29 yrs.","29 yr","29y","  29 ","Age 29","Age: 29"
   -> null: null,"","yrs","unknown","1990-05-01","290","9","71","29 years 3 months","about 30"
Q6 Blood group display (D1, D3). formatBloodGroup: "O-" -> "O- (Rh negative)", "AB Negative" -> "AB Negative (Rh negative)", "A+" -> "A+", "  B+ " -> "B+", "pending" -> "pending", "" -> null, null -> null. isRhPositive: true "A+","O pos","B Positive","AB +"; false "O-","O neg","pending","",null.
Q7 One source of truth (integrity; extends P6/P7 to every screen). Blood-group Rh colour and the high-risk flag on the patient list, the profile and the workspace use isRhNegative / isRhPositive / isHighRisk only. Today PatientListScreen and ProfileScreen use contains("-"), contains("+"), contains("High"), so "Not high risk" is flagged high risk on the list and profile.
R2-5 (enum-only comparison) is NOT taken: P6/P7 tolerant parsing stays because backup restore does not validate the enum.

## UI oracles - static check .agents/checks/check_ui_static_r2.py (new; round-1 check_ui_static.py must keep passing)
U1 (D3) No emoji code points (U+1F000-1FAFF, U+2600-27BF, U+2B00-2BFF, U+FE0F) in ProfileScreen.kt, PatientListScreen.kt, WorkspaceScreen.kt, ClinicalBadges.kt. A warning glyph may become Icon(Icons.Default.Warning, contentDescription = null) next to text that already says the warning.
U2 (D1) formatBloodGroup( is called in PatientListScreen.kt, ProfileScreen.kt and WorkspaceScreen.kt (each place a blood group value is displayed).
U3 (Q7) PatientListScreen.kt and ProfileScreen.kt contain no `.contains("-")`, `.contains("+")` or `contains("High"` and call isRhNegative( and isHighRisk(.
U4 (D4) In ProfileScreen.kt and WorkspaceScreen.kt every reference to R.string.edit_dating, edit_history, edit_ultrasound, edit_visit, edit_other_histories, edit_lab, edit_medication, edit_delivery_plan, edit_clinical_alerts is a contentDescription of a pencil (PencilEditButton(contentDescription = ...) or Icon(Icons.Default.Edit, contentDescription = ...)), never inside Text(...). Existing testTags on those buttons are kept.
U5 (D4 exception) R.string.correct_previous_pregnancy in WorkspaceScreen.kt is shown as Text inside a TextButton (not a pencil), keeping testTag "editPregnancy-<id>".
U6 (R2-7) No contentDescription string literal "Edit Clinical Alerts" in WorkspaceScreen.kt; new string resource edit_clinical_alerts ("Edit clinical alerts") is used; that IconButton has no Modifier.size(32.dp).

## Regression gates
G1 Full unit suite ./gradlew --offline :app:testDebugUnitTest passes (159 before this round + new).  G2 :app:compileDebugKotlin passes.  G3 round-1 STATIC-ui passes.

## Out of scope this round
Touch-target measurement (D5, device phase), R2-8 tile truncation (device phase), emoji in editors/dialogs outside the four files, localisation, the phase-1 security/data findings.

## Round 2b addendum (orchestrator probe + GLM review findings on the round-2 build; Q1-Q7/U1-U6 still hold)
Q8 History hospital is not THIS delivery's hospital. A hospital phrase found in a segment that has a history marker (P2 list) is NOT added to the hospital box; the segment stays in plan.
   "Delivered previous baby at Al Salam Hospital (2019)" -> hospital null
   "Previous CS at Al Salam Hospital; booked Cleopatra Hospital" -> hospital == "Cleopatra Hospital"
Q9 Generic phrases are not names. After dropping leading words (Q4b), an unlabelled phrase is rejected if it has no word other than Maternity/Medical/Hospital, or if it contains the whole word To, Go or If (any case).
   "Transfer To Hospital if PPH", "Go To Hospital If Bleeding", "Referral To Hospital Urgently" -> hospital null (line stays in plan)
Q10 Every hospital phrase in a segment is collected (distinct, source order, joined "\n").
   "Patient prefers Andalusia Smouha Hospital or Dar Al Fouad Hospital" -> "Andalusia Smouha Hospital\nDar Al Fouad Hospital"
Q11 Labelled value cut: "." ends the value only when it is not directly after a Q1 abbreviation; "pending" joins the undecided list. If a labelled name and a phrase from the same segment are equal or one contains the other, keep only the longer, once.
   "Hospital: St. Mary's Maternity Hospital" -> exactly "St. Mary's Maternity Hospital"      "Hospital: pending" -> null
Q12 Sentence split also before a lowercase letter: ". " followed by any letter (abbreviation exceptions unchanged; same-section rejoin keeps text exact).
   "Fee 15,000 EGP. if bleeding call" -> fees == "Fee 15,000 EGP.", plan == "if bleeding call"
   "e.g. CS if breech. Otherwise SVD." -> plan == "e.g. CS if breech. Otherwise SVD." (unchanged)
U7 ProfileScreen.kt scan photo chip shows stringResource(R.string.photos_count, n), never a bare number: no `imageUris.size.toString()` in ProfileScreen.kt.

## Round 2c addendum (fixes R3-1: Q8 was too broad; Q13 SUPERSEDES Q8, Q14 refines Q11)
Q13 Previous-delivery hospital, narrowed. (a) A labelled "Hospital:" value always counts, whatever else the line says.
   (b) An unlabelled hospital phrase is skipped ONLY when a past-delivery marker occurs earlier in the same segment, before that phrase, and no
   current marker occurs between that past marker and the phrase.
   Past-delivery marker: whole word previous|prior|earlier|last followed within 3 words by baby|delivery|birth|CS|caesarean|cesarean|section|pregnancy|child (case-insensitive).
   Current marker (whole word/phrase, case-insensitive): now, "this pregnancy", current, currently, booked, planned, plan, will, elective, scheduled.
   "Delivered previous baby at Al Salam Hospital (2019)" -> hospital null
   "Prior pregnancy delivered at El Shatby Hospital" -> hospital null
   "Previous CS at Al Salam Hospital; booked Cleopatra Hospital" -> "Cleopatra Hospital"
   "Previous CS at Al Salam Hospital, now booked at Cleopatra Hospital" -> "Cleopatra Hospital"
   "Elective CS at Dar Al Fouad Hospital given previous CS x2" -> "Dar Al Fouad Hospital"
   "Planned delivery for this pregnancy at Cleopatra Hospital" -> "Cleopatra Hospital"
   Every one of these lines stays in plan, unchanged (Q2/D2).
Q14 A labelled value also ends at "(".
   "Hospital: Cleopatra Hospital (history of PPH, cross-match 2 units)" -> exactly "Cleopatra Hospital" (listed once)
All Q1-Q12 tests other than the two Q8 examples must keep passing unchanged; the two Q8 examples stay valid under Q13 (their past marker precedes the phrase).

## Round 2d addendum (fixes R3-2: clinical words glued onto an unlabelled hospital name)
Q15 (a) These leading words are also dropped from an unlabelled hospital phrase (whole word, case-insensitive, same rule as the existing
   drop list, at least one word always kept): last, previous, prior, earlier, repeat, elective, emergency, CS, LSCS, SVD, NVD.
   (b) The Q13(b) past-delivery check reads the segment text before the phrase AFTER the leading words are dropped, so dropped words
   count as text before the phrase (a dropped "Last CS" is a past marker; a dropped "Elective" is a current marker).
   "Last CS Dar Al Fouad Hospital, plan Cleopatra Hospital" -> "Cleopatra Hospital"
   "Previous LSCS Al Salam Hospital" -> hospital null
   "Prior CS Al Salam Hospital, now Cleopatra Hospital" -> "Cleopatra Hospital"
   "Elective CS Cleopatra Hospital" -> "Cleopatra Hospital"
   "Repeat CS Cleopatra Hospital" -> "Cleopatra Hospital"
   "Emergency CS Dar Al Fouad Hospital given previous CS x2" -> "Dar Al Fouad Hospital"
   "Hospital: Cleopatra Hospital. Last CS Al Salam Hospital" -> "Cleopatra Hospital" (labelled value counts, history phrase skipped)
   Every one of these lines stays in plan, unchanged (Q2/D2). All Q1-Q14 tests must keep passing unchanged.
