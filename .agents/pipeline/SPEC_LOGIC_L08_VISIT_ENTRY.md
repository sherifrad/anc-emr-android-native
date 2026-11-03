# Specification: Logic Round l08 - Visit & Vitals Entry Rules

**Status**: Ready for Implementation
**Target**: Logic Round `l08` (preceding Design Round `d28`)
**Source of Truth**: `REDESIGN_SPECS.md` §4.4 & approved mock `d4/d3/index.html` §3

---

## 1. Domain Invariants & Clinical Rules

1. **Blood Pressure Thresholds & Flagging**:
   - Systolic $\ge 140$ mmHg OR Diastolic $\ge 90$ mmHg is flagged as High BP (`BpLevel.RAISED` or `BpLevel.SEVERE`).
   - High BP generates a flag/alert pill, but **NEVER blocks saving**.
   - Plausibility boundaries for BP: Systolic 60..260 mmHg, Diastolic 30..160 mmHg, Systolic > Diastolic.

2. **Normal Findings Fill Isolation**:
   - `fillNormalVisitFindings(...)` populates ONLY blank findings:
     - `oedema`: `"None"` (if currently blank/null)
     - `fetalMovements`: `"Good"` (if currently blank/null)
     - `urineProtein`: `"Negative"` (if currently blank/null)
   - If any of these fields already has a value, it MUST NOT be overwritten.
   - **Crucial Isolation Invariant**: `fillNormalVisitFindings` MUST NEVER modify blood pressure, weight, fundal height, visit date, notes, or any other vital signs.

3. **"Same as Last" Historical Extraction**:
   - When entering a visit, "Same as last" offers the vitals from the patient's most recent prior visit (excluding the current visit if editing).
   - Resolves:
     - `lastBloodPressure`: `String?` ("118/76") and parsed `systolic`, `diastolic`
     - `lastWeight`: `String?` ("74.5")
     - `lastFundalHeight`: `String?` ("27")
   - If no prior visit exists, all fields resolve gracefully to `null`.

4. **"Matches GA" Fundal Height Estimation**:
   - If gestational age is available (e.g. 28 weeks), fundal height roughly matches gestational age in weeks (1 cm per week between 20-40 weeks; plausible 10-50 cm).
   - Returns integer centimeters if GA is between 10 and 50 weeks, otherwise `null`.

5. **Structured Findings Serialization & Round-Trip**:
   - `DemoVisit` stores free-text `findings` and `notes` to strictly preserve Room schema and Database version 6.
   - `buildVisitFindings(fundalHeight, oedema, fetalMovements, urineProtein, freeText)` composes canonical findings text.
   - `parseVisitFindings(findingsText)` parses back `fundalHeight`, `oedema`, `fetalMovements`, `urineProtein` losslessly so editing an existing visit restores the 4-step wizard state.

6. **Flagged Findings Detection**:
   - `detectFlaggedFindings(sys, dia, oedema, fetalMovements, urineProtein)` returns a list of alert badges:
     - `"High BP"` if systolic $\ge 140$ or diastolic $\ge 90$.
     - `"Oedema $oedema"` if oedema is marked/abnormal.
     - `"Movements $fetalMovements"` if movements are reduced/absent.
     - `"Urine $urineProtein"` if urine protein is +, ++, +++.
     - Empty list if nothing is flagged.

7. **Custom Choice Presets**:
   - `CustomChoiceList` includes:
     - `VISIT_OEDEMA` ("custom_choice_visit_oedema")
     - `VISIT_MOVEMENTS` ("custom_choice_visit_movements")
     - `VISIT_URINE` ("custom_choice_visit_urine")
     - `VISIT_BP` ("custom_choice_visit_bp")
     - `VISIT_WEIGHT` ("custom_choice_visit_weight")
     - `VISIT_FUNDAL_HEIGHT` ("custom_choice_visit_fundal_height")
   - Dating methods invariance is strictly preserved.

8. **Draft & State Integration**:
   - `DraftValues` supports structured fields: `systolic`, `diastolic`, `fundalHeight`, `oedema`, `fetalMovements`, `urineProtein`.
   - `DraftField` supports corresponding event types with bi-directional synchronization between `bloodPressure` and `systolic`/`diastolic`.
   - `SaveVisitCommand.prepare()` prepares the command with canonical normalization.

---

## 2. Outcome Oracles

- **Oracle O1 (BP Flagging)**: BP $\ge 140/90$ flags high, plausible BP saves successfully regardless of flag.
- **Oracle O2 (Normal Fill)**: Only blank oedema/movements/urine populated; BP/weight untouched.
- **Oracle O3 (Same As Last)**: Extracts prior visit vitals, excludes current visit ID when editing, handles empty history.
- **Oracle O4 (Matches GA)**: Estimates FH from GA weeks within 10..50 cm.
- **Oracle O5 (Serialization Roundtrip)**: `buildVisitFindings` and `parseVisitFindings` roundtrip losslessly.
- **Oracle O6 (Flagged Findings)**: Detects high BP, marked oedema, reduced movements, positive proteinuria.
- **Oracle O7 (Custom Choices)**: Enum additions persist and conform to clinical safety dating-method invariant.
- **Oracle O8 (Reducer & Command)**: `AncReducer` handles new draft fields and `SaveVisitCommand` prepares valid commands.
