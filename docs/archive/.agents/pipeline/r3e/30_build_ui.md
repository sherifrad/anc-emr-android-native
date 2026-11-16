ROLE: UI BUILDER for ROUND 3e. Implement Q42, Q43, Q44, Q45, Q46 from the spec section "Round 3e".
ALLOWED files (app/src/main/java/com/sherifradwan/ancnative/ui/): AppLockScreen.kt, UltrasoundPhotoViewerDialog.kt, ClinicalAttachmentViewerDialog.kt, Theme.kt, PatientListScreen.kt, ProfileScreen.kt, AddAttachmentDialog.kt; app/src/main/res/values/strings.xml.
Keep every existing testTag and every behaviour not named in the spec. Q44: change ONLY the text colour of a pair that fails 4.5:1.
Run: python3 .agents/checks/check_r3e.py (S25-S28 are yours), ./gradlew --offline :app:testDebugUnitTest --tests '*ThemeContrastTest', ./gradlew --offline :app:compileDebugKotlin. Report real results.
