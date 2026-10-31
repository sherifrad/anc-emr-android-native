- Fixed singular/plural formatting for count 1 ("1 Prior Miscarriage", "1 Prior Pregnancy") and count > 1 ("2 Prior Miscarriages", "2 Prior Pregnancies") via `pluralize()` in [ObstetricHistory.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/ObstetricHistory.kt#L70-L95).
- Updated existing assertions pinning old singular text and added count 1 & count 2 tests in [ObstetricHistoryTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/model/ObstetricHistoryTest.kt#L432-L515).
- `PrintObstetricSummaryTest.kt` was untouched as it tests count 2 ("2 Prior Miscarriages").
- Targeted test run:
  `./gradlew --offline :app:testDebugUnitTest --tests '*ObstetricHistoryTest' --tests '*PrintObstetricSummaryTest'` -> BUILD SUCCESSFUL (33 tests, 0 failures).
- Full unit test suite:
  `./gradlew --offline :app:testDebugUnitTest` -> BUILD SUCCESSFUL (539 tests, 0 failures, 100% passed in 8.15s).
