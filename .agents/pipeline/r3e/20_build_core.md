ROLE: CORE BUILDER for ROUND 3e. Make the tests and checks pass without editing them.
ALLOWED files (app/src/main/java/com/sherifradwan/ancnative/): storage/VaultAutoAction.kt, state/AncViewModel.kt, MainActivity.kt.
Implement Q41 a, b, c exactly as in the spec. Minimal change in the surrounding style. Do not touch the vault controller itself.
Run: ./gradlew --offline :app:testDebugUnitTest --tests '*VaultAutoActionTest', python3 .agents/checks/check_r3e.py (S23, S24 are yours), ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin. Report real results and the assumptions you made.
