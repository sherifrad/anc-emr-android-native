ROLE: CORE LOGIC BUILDER. Make the tests pass without editing them (tests are owned by another agent and are the contract).
ALLOWED files:
- app/src/main/java/com/sherifradwan/ancnative/ui/DeliveryNotesParser.kt   (NEW: move DeliveryNoteSections and parseDeliveryNotes here, fix per P1-P5)
- app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalFormat.kt   (NEW: isRhNegative, isHighRisk, formatAgeYears per P6-P8; pure Kotlin, no Compose)
- app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt   (ONLY: delete the old DeliveryNoteSections/parseDeliveryNotes definitions, and replace the inline Rh/risk logic in DeliveryPlanningCard with isRhNegative(...)/isHighRisk(...) calls. No UI/layout/text changes.)
Read the tests under app/src/test/java/com/sherifradwan/ancnative/ui/ first. Use Regex with word boundaries. Keep the parser a pure function.
Run: ./gradlew --offline :app:testDebugUnitTest --tests '*ParseDeliveryNotesSpecTest' --tests '*ClinicalFormatTest' and report the real result.
