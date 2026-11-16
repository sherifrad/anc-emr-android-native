# Spec: ANC EMR (Android, Jetpack Compose) - uncommitted UI change "delivery card + age badge + pencil edit"
Project: private obstetrics clinic EMR. Failure = wrong or hidden clinical/administrative data at point of care. Owner approves clinical values.

## Intent of the change (from the author's work in progress)
1. Patient age is shown as a dedicated badge: label "AGE" + value "<n> yrs" (indigo), on the patient list and large on the profile header. Accessible description "Age <n> years".
2. The Delivery planning card shows key facts (blood group, current gestational age, agreed EDD, risk, fetal presentation, placental site) as label-over-value tiles, then four boxes: Agreed Delivery Hospital, Agreed Delivery Fees, Delivery Plan, Postpartum Note. The four boxes are filled by splitting the patient's free-text obstetric notes with a pure function:
   `internal fun parseDeliveryNotes(notes: String?): DeliveryNoteSections` ; `DeliveryNoteSections(hospital: String?, fees: String?, plan: String?, postpartum: String?)` in package com.sherifradwan.ancnative.ui.
3. Text "Edit ..." buttons on visit, previous pregnancy, ultrasound, lab, medication cards and the delivery card become icon-only pencil buttons (same test tags kept).

## Outcome oracles (what proves success)
O1 Notes splitting is lossless and unambiguous: every non-empty note line appears in exactly one of the four sections (or, if intentionally dropped, the drop is reported). null/blank notes -> all four null. Order of lines inside a section is preserved.
O2 Notes splitting does not misfile clinical history as a current event: e.g. a line about a PREVIOUS delivery or a previous discharge must not be shown under "Postpartum Note" for the current pregnancy; a line mentioning a hospital must not be lost when it also mentions a fee.
O3 Hospital extraction returns the hospital name only when present, and never returns a fee/clinical sentence as a hospital.
O4 The delivery card still shows everything it showed before: blood group, EDD, GA, risk, presentation, placental site, and the delivery planning notice text (string R.string.delivery_planning_notice) that told the clinician how to use the card. Nothing previously shown is silently removed.
O5 Rh-negative blood groups are visually emphasised (e.g. "A-", "O neg", "AB Negative"); positive groups ("A+", "O pos") are not. "High" risk emphasised, others not.
O6 Age badge: ages given as "29", "29 years", "29 yrs" all render "29 yrs" once (no "yrs yrs"); blank/null renders nothing; the badge is used on both list and profile.
O7 Every edit action that existed before is still present, has a content description from a string resource, keeps its test tag, and has a touch target >= 48dp.
O8 Strings shown to users follow the app's localisation (the app supports English and Arabic/RTL): new hard-coded English labels are a defect unless the app is declared English-only.
O9 Light and dark themes: text/background pairs of new colour tokens meet WCAG AA contrast (4.5:1 for normal text).
