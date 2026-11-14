# Builder A (edit ONLY app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/LabReportGrouping.kt). No git, no gradle. Package com.sherifradwan.ancnative.
Fix partitionReportIntoCompositeGroups per docs/designs/reviews/owner_fixes_2026-11-15/lab_investigation.md "Bug A", section "Smallest Fix Plan":
1. CBC keywords add: haemoglobin, hemoglobin, hb, haematocrit, hematocrit, hct, mcv, mch, mchc, white cell, white blood, wbc, platelet, plt, rbc, red blood. Match "hb" and "plt","alt","ast" as whole tokens (split name on non-letters), not substrings.
2. Urine and stool are decided BEFORE CBC: category equal to "urinalysis" or containing "urine"/"urinalysis", or name containing "urine", goes to urine so "RBCs (Urine)" never lands in CBC.
3. "Fasting Blood Sugar (FBS)" must never match LFT "ast": use whole-token matching for alt/ast.
Keep all function names and signatures. No new files. Output list of changes.
