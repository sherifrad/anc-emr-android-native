# Fix spec (round 1) - delivery card, notes parser, clinical helpers, age badge
Project: Android EMR (Kotlin, Compose) for a private obstetrics clinic. Wrong or hidden data = serious failure. Owner (clinician) approves any clinical wording.
Base: the uncommitted change described in ../maca-run-2026-10-29/SPEC.md. Package com.sherifradwan.ancnative.ui. English-only app (MainActivity forces Locale.ENGLISH, no values-ar).

## Interface contract (names are fixed so tests can be written before the code)
New file ui/DeliveryNotesParser.kt (moved out of WorkspaceScreen.kt):
  internal data class DeliveryNoteSections(val hospital: String?, val fees: String?, val plan: String?, val postpartum: String?)
  internal fun parseDeliveryNotes(notes: String?): DeliveryNoteSections
New file ui/ClinicalFormat.kt (pure functions, no Compose imports):
  internal fun isRhNegative(bloodGroup: String?): Boolean
  internal fun isHighRisk(risk: String?): Boolean
  internal fun formatAgeYears(ageText: String?): String?     // digits only, e.g. "29"; null when no valid age
Existing: Theme.kt colour tokens (Tag*, Delivery*). Tests may read them.

## Oracles (each becomes a test or a static check)
P1 Keywords match whole words only. "Feels reduced fetal movements" and "Wishes to exclusively breastfeed" never go to fees.
P2 Postpartum section only takes lines about THIS pregnancy: a line starting with the label "Postpartum", or containing whole-word delivered/discharged without history markers (previous, prior, history, earlier, pregnancy, G\dP\d). History lines go to plan, with their wording intact.
P3 No text is altered or lost: only a known label prefix is stripped before ':' (e.g. "Postpartum note:", "Delivery fee:"). Times like "10:30" are untouched. Unconfirmed fee wording (quoted, estimate, proposed, discussed, undecided, not agreed) stays in plan, never under "Agreed Delivery Fees".
P4 Hospital is only a phrase "<Name> Hospital" (capitalised words ending in Hospital, optionally Maternity/Medical). Search ALL lines, fee lines included. Never return a whole sentence, a time, or "39 weeks". "declines hospital birth" and "Hospitalised at 28w" give null.
P5 Lossless: every non-empty input line appears in at least one section, original order kept within a section, nothing invented. Only a hospital-bearing line may appear twice (hospital box + its home section). null/blank/whitespace input -> all four null. (Owner to confirm the double listing.)
P6 isRhNegative true: "A-","O-","O −"(U+2212),"O neg","AB Negative","B Rh -ve","O Rh(-)","O minus". false: "A+","O pos","A+, antibody screen negative","", null, "pending".
P7 isHighRisk true: "High","High Risk","high". false: "Not high risk","Low","Moderate",null,"".
P8 formatAgeYears: "29","29 years","29 Years","29 yrs.","29 yr","  29 ","Age 29" -> "29"; null,"","yrs","unknown" -> null.
P9 Contrast: every text/background token pair used by the age badge and the delivery boxes, light and dark, >= 4.5:1. Placeholder text uses a full-opacity token (no alpha 0.65).
P10 Static (check_ui_static.py): (a) R.string.delivery_planning_notice is referenced inside DeliveryPlanningCard; (b) R.string.agreed_edd, current_gestational_age, manual_risk, fetal_presentation, placental_site are used in WorkspaceScreen.kt; (c) no contentDescription string literal for the delivery edit button, use stringResource (new resource edit_delivery_plan); (d) ClinicalBadges.kt PatientAgeBadge calls clearAndSetSemantics (or mergeDescendants) and formatAgeYears; (e) WorkspaceScreen.kt has no inline endsWith("-") Rh logic and uses isRhNegative/isHighRisk; (f) parseDeliveryNotes is not defined in WorkspaceScreen.kt.

## Out of scope this round (owner decisions, do not touch)
Rh text marker (colour-only emphasis), emoji in values, pencil-vs-text button consistency elsewhere, localisation/Arabic, testTag duplication, touch-target measurement (needs device).
