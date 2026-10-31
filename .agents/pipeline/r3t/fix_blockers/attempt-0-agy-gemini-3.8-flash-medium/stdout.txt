Running targeted unit tests (`*GestationalBannerLogicTest` and `*GestationalDisplayTest`). Waiting for the task to complete.
- Removed unrequested fallback line and unused imports (`eddFromLmp`, `parseIsoDateOrNull`) from [`GestationalBanner.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/GestationalBanner.kt#L30-L46).
- Removed all reflection shims, private extension properties, and copied formulas in [`GestationalBannerLogicTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/ui/GestationalBannerLogicTest.kt#L1-L80).
- Directly imported and invoked production `daysToEdd`, `isLateTerm`, and `progressFraction`; replaced formula expectations with literal numbers (`0.7107f, progress, 0.005f`).
- Verified targeted unit tests:
  `./gradlew --offline :app:testDebugUnitTest --tests '*GestationalBannerLogicTest' --tests '*GestationalDisplayTest'` -> **BUILD SUCCESSFUL** (28 actionable tasks, 8 executed).
- Verified full unit test suite:
  `./gradlew --offline :app:testDebugUnitTest` -> **BUILD SUCCESSFUL** (28 actionable tasks, 1 executed).
- Verified static checks (`python3 .agents/checks/check_r3t.py`):
  - PASS S86 (GestationalAgeEngine defines `isLateTerm`, `progressFraction`, `shortWeeksText`, `daysToEdd`)
  - PASS S87 (GestationalBanner / screens compliance)
  - PASS S88 (GestationalBannerLogicTest contains no `280f`, no `ChronoUnit`, calls `progressFraction` and `daysToEdd`)
