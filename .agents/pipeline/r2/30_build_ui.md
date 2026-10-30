CURRENT TASK = ROUND 2b: only U7 (ProfileScreen.kt scan photo chip -> stringResource(R.string.photos_count, scan.imageUris.size)). Touch nothing else.
ROLE: UI/UX BUILDER. Apply U1-U6 and Q7 using the existing helpers; no parsing logic of your own.
ALLOWED files:
- app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt   (Q7: isRhNegative/isRhPositive/isHighRisk in the row projection; U1 remove emoji; U2 formatBloodGroup for the blood badge text)
- app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt   (Q7, U1, U2; U4: the edit_dating / edit_history / edit_ultrasound / edit_visit text buttons become pencils - use PencilEditButton(contentDescription = stringResource(...), tag = <existing testTag>, onClick = ...) or an IconButton with Icon(Icons.Default.Edit, contentDescription = stringResource(...)) where a full-width Row would break a header layout)
- app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt   (U1, U2 blood-group tile uses formatBloodGroup, U4 remaining text edit buttons -> pencils, U5 "Correct previous pregnancy" becomes TextButton { Text(stringResource(R.string.correct_previous_pregnancy)) } with testTag "editPregnancy-<id>", U6 clinical alerts edit icon)
- app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalBadges.kt   (U1 only if needed)
- app/src/main/res/values/strings.xml   (add only: edit_clinical_alerts = "Edit clinical alerts")
Keep colours, layout, wording and testTags otherwise unchanged. A removed warning emoji may become Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp)) before the text.
Verify with: python3 .agents/checks/check_ui_static_r2.py && python3 .agents/checks/check_ui_static.py && ./gradlew --offline :app:compileDebugKotlin. Report real results.
