You are an independent reviewer of the PDF export of an antenatal clinic app (Android Kotlin). The export builds one HTML string from a patient object and prints it as an A4 PDF that is handed to patients and hospitals. Below is the generator code (CSS left out). Find where the PDF would be WRONG, MISLEADING or INCOMPLETE compared with the data the app holds. Real defects only, each with a concrete patient example.

Data the app holds (fields per record):
- Patient: caseId, displayName, dateOfBirth, phone, status (active/archived), riskLevel, ageText, addressText, bloodGroupText, medicalHistory, surgicalHistory, familyHistory, allergyHistory, gravida, parity, abortion, obstetricNotes (free text holding four delivery plan sections: hospital, fees, plan, postpartum), clinicalAttention (alert text shown on the patient screen), delivered, deliveryDate, deliveryMode, deliveryOutcome, dating, previousPregnancies, visits, ultrasoundScans, labInvestigations, medications.
- Visit: date, bloodPressure, weight, findings, notes.
- Scan: date, gestationalAgeWeeks, gestationalAgeDays, fetalNumber, presentation, placentalSite, amnioticFluid, fetalHeartRate, biometryBpd/Hc/Ac/Fl, estimatedFetalWeight, impression, imageUris.
- Lab: date, testCategory, testName, result, unit, referenceRange, isAbnormal, notes, labName.
- Medication: name, dosage, route, frequency, startDate, endDate, status (active/stopped/...), notes, isProphylaxis, isNonCompliant, adverseNotes.
- Previous pregnancy: dateText, outcome, outcomeText (old free text), pregnancyType, otherDetails, placeAndDoctor, deliveryBy, birthMethod, lossManagement, terminationMethod.
- The risk level has three tiers in the app: low, medium, high.
- A patient can be delivered (then the app shows "Delivered <date>" instead of gestational age).

Check in particular: which rows are kept when a list is longer than the print limit (oldest or newest?); sort orders; fields the app holds that the PDF never prints; fixed text printed as if it were patient-specific clinical advice; placeholder text; values printed for a delivered or archived patient that no longer make sense (current gestational age, target delivery timing); the three risk tiers; the "Active Medications" title versus the rows; a visit with both findings and notes; a scan with both impression and biometry; HTML escaping gaps; page count claims.

Answer as a table: severity (serious / moderate / minor), what the PDF prints, what it should print, the code line or expression. Then list anything you checked and found correct.
