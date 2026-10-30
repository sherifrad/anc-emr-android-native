CURRENT TASK = ROUND 3a-2: Q19(h)-(k) only, in ui/report/PatientPrintHtmlGenerator.kt (the four fallback expressions, today lines 74, 99, 195, 239). Change nothing else.
ROLE: CORE LOGIC BUILDER. Make the tests pass without editing them (tests are owned by another agent and are the contract).
ALLOWED files:
- app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt   (Q19 only: the listed cells/rows; markup elsewhere byte-identical)
- app/src/main/java/com/sherifradwan/ancnative/state/IntakeDatingRule.kt   (NEW, Q20 pure function, no Android imports)
- app/src/main/java/com/sherifradwan/ancnative/state/AncViewModel.kt   (Q20 wiring only, the SavePatientProfileCommand branch)
- app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt   (Q21(a) only)
- app/src/main/java/com/sherifradwan/ancnative/storage/VaultController.kt   (Q21(b): pass BuildConfig.DEBUG at the one seed call)
- app/build.gradle.kts   (Q21(b): add buildConfig = true inside the existing buildFeatures block; nothing else)
Read the tests under app/src/test/ first. Keep every change minimal and readable, in the style of the surrounding code.
Run: ./gradlew --offline :app:testDebugUnitTest --tests '*PatientPrintHtmlGeneratorTest' --tests '*IntakeDatingRuleTest' and python3 .agents/checks/check_r3.py, and report the real results. Also run ./gradlew --offline :app:compileReleaseKotlin.
