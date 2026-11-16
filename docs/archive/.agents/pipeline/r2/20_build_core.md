CURRENT TASK = ROUND 2f: Q18 only, in ui/report/PatientPrintHtmlGenerator.kt: change only the ageFormatted line (today line 261) to use formatAgeYears as the spec says. Change nothing else.
ROLE: CORE LOGIC BUILDER. Make the tests pass without editing them (tests are owned by another agent and are the contract).
ALLOWED files:
- app/src/main/java/com/sherifradwan/ancnative/ui/DeliveryNotesParser.kt   (Q1-Q4: segment split, lossless rejoin with original separator, tense markers, labelled + unlabelled hospital rules)
- app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalFormat.kt   (Q5 formatAgeYears, Q6 isRhPositive + formatBloodGroup; pure Kotlin, no Compose)
Read the tests under app/src/test/java/com/sherifradwan/ancnative/ui/ first. Keep the parser a pure function and readable (named regex constants, short KDoc like today). Word-boundary regexes only.
Run: ./gradlew --offline :app:testDebugUnitTest --tests '*ParseDeliveryNotesSpecTest' --tests '*DeliveryNotesParserTest' --tests '*ClinicalFormatTest' --tests '*ClinicalFormatR2Test' and report the real result.
