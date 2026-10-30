ROLE: UI/UX BUILDER. Fix the UI defects from the spec, no logic changes beyond calling the existing helpers.
ALLOWED files:
- app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalBadges.kt
- app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt
- app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt   (DeliveryPlanningCard region only)
- app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt   (age badge usage only)
- app/src/main/res/values/strings.xml   (add only: edit_delivery_plan, plus age strings if you need them)
Tasks: (1) restore the delivery_planning_notice text under the card header (P10a). (2) restore stringResource for the five labels agreed_edd, current_gestational_age, manual_risk, fetal_presentation, placental_site in the KeyFactTile calls (P10b); keep the tile layout. (3) the delivery edit icon gets contentDescription = stringResource(R.string.edit_delivery_plan) with the same text as today (P10c). (4) PatientAgeBadge: use formatAgeYears(ageText) (return nothing when null), clearAndSetSemantics with the existing description, spacer in ProfileScreen only when the badge shows (P10d). (5) Dark "AGE" label background to #4F46E5 or another token with >= 4.5:1 against white; placeholder text in DeliverySectionBox at full opacity (no alpha 0.65), use italic to mark it as a placeholder (P9).
Verify with: python3 .agents/checks/check_ui_static.py and ./gradlew --offline :app:compileDebugKotlin :app:testDebugUnitTest --tests '*ThemeContrastTest'. Report real results.
