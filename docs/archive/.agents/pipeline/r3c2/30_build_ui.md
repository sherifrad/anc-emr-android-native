CURRENT TASK = ROUND 3c-2, UI part: Q30(b) in app/src/main/java/com/sherifradwan/ancnative/ui/AppLockScreen.kt ONLY: observe securityRepo.settings (collectAsState) and derive the screen mode (setup / legacy entry / unlock / lockout) from the observed state, so the screen switches to "Choose a new PIN" the moment the repository asks for it. Keep every testTag and string. Make check_r3c.py S15 pass.
ALLOWED files: app/src/main/java/com/sherifradwan/ancnative/ui/AppLockScreen.kt
Run: python3 .agents/checks/check_r3c.py and ./gradlew --offline :app:compileDebugKotlin. Report real results.
