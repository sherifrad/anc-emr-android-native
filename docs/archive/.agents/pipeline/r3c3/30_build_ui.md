CURRENT TASK = ROUND 3c-3: Q31 in app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt ONLY. Gate the Security Settings dialog on !isAppLocked and needsPinSetup != true, reset showSettingsDialog to false when the app locks or needs PIN setup (an effect keyed on those values), and add needsPinSetup != true to the two existing AlertDialog guards. Change nothing else. Make check_r3c.py S16 pass.
ALLOWED files: app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt
Run: python3 .agents/checks/check_r3c.py and ./gradlew --offline :app:compileDebugKotlin. Report real results.
