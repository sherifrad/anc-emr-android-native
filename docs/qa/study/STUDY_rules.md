1 Problem as reported
The owner saw "Blood bank precautions: Anti-D Prophylaxis + Matched Packed RBCs Cross-match" in a printed patient report and asks whether the app has a general engine that decides clinical actions.

2 Root cause / current behaviour (from pasted files only)
- The note is produced by reportDeliveryRows in ReportSections.kt:257-272, specifically lines 263-265, when isRhNegative(patient.bloodGroupText) is true.
- isRhNegative is defined in ClinicalFormat.kt:8-14. It returns true for a blood group string containing "-", "neg", "negative", or "minus", and false if "+" or "pos"/"positive" is present.
- The inserted text is the fixed literal "Anti-D Prophylaxis + Matched Packed RBCs Cross-match" at ReportSections.kt:264.
- It is rendered as an alert box in ReportDeliveryPlanCard at ReportSections.kt:1104-1108.
- Authorship is not visible in the files given.
- The clinician cannot edit or switch it off in the visible code: no edit control or setting is shown for this row.
- Other automatic rules visible in pasted files:
  - BP alert mapping: ClinicalFormat.kt:126-130 maps bpLevel(raw) to R.string.bp_alert_raised or R.string.bp_alert_severe.
  - Visit BP status words: ReportSections.kt:716-720 maps bpLevel to R.string.report_bp_severe or R.string.report_bp_raised.
  - Rh-negative badge: ReportSections.kt:427 and 453-458 shows an Rh-negative status tag when isRhNegative is true.
  - Dating discordance note: ReportSections.kt:506-508 and 571-573 displays datingDiscordanceLine when present. The rule text and trigger are not visible in the pasted files.
  - Risk label formatting: ReportSections.kt:147-160 maps a stored riskLevel to High/Medium/Low/Unrecorded; it does not create a new clinical decision.
  - Stored clinical attention is displayed only if present: ReportSections.kt:488-493. Whether it is auto-generated is not visible.
- Other items named in the rules index (attention banners, severe BP, redating hints) are not in the pasted files, so their triggers and text are not visible here.

3 Proposed fix
1. Do not treat this as a general decision-support engine; it is one fixed report rule.
2. Replace the unconditional Rh-negative row with either clinician-entered delivery-plan text or a clearly labelled precaution that the clinician can remove or edit.
3. Reuse the existing delivery plan parsing/display path in DeliveryPlanningCard.kt and ReportDeliveryPlanCard rather than adding a second advice mechanism.
4. Keep isRhNegative as a display helper only; do not let it generate clinical instructions.
5. If retained, make the precaution opt-in or configurable, and include delivery status and antibody/sensitisation data before showing it. Those data sources are not visible in the pasted files.

4 Tests to add
- reportDeliveryRows returns the blood bank row only for Rh-negative blood groups.
- reportDeliveryRows does not add the row for Rh-positive, blank, or ambiguous blood groups.
- isRhNegative handles positive, negative, and blank inputs.
- BP alert mapping returns null for normal/unreadable values.

5 Risks, protected files, and questions
- The study proposes changes to ReportSections.kt and ClinicalFormat.kt; neither is in the protected list shown. No schemas, RecordsDatabase, RecordEntities, RecordsDao, RecordsRepository, UndoCoordinator, ClinicalModels, generators, state reducers, or DatingFieldVisibility files are touched in this study.
- Clinical safety concern: the note appears for every Rh-negative woman, including delivered patients, because reportDeliveryRows checks only Rh status. It does not account for antibodies, prior sensitisation, or completed delivery. It is shown in the printed report and rendered as an alert box.
- Questions for the owner: should the note be editable per patient, disabled in settings, or removed entirely? Should it appear in print/share or only in the app? Should it consider antibody status and delivery state?

6 Task entries for the roadmap
- Make blood bank precaution editable or removable
  - Change reportDeliveryRows so the Rh-negative note is not auto-inserted as fixed advice.
  - Reuse delivery plan fields and add clinician control and tests.
- Review automatic clinical advice rules
  - Inventory all fixed rules from the rules index and mark each as display, alert, or advice.
  - Add tests and owner decisions for editability and safety.

5-line answer to the owner
No, the app does not have a general clinical decision engine. The blood bank note is one fixed report rule triggered only by an Rh-negative blood group. It is not based on antibodies, sensitisation, or delivery status. It is generated automatically in the printed report and shown as an alert. The clinician cannot edit or disable it in the visible code. It should be made editable or configurable before clinical use.

