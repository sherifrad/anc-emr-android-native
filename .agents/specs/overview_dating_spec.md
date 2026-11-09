# Overview Dating section: spec and outcome oracles

Source of truth for the owner's decisions: `concepts_brief.md` and the approved mock `.agents/mocks/overview_tab_sections.html` (Dating variants a to g, GA strip). Schema v6 is frozen. No new stored field.

## API (fixed so tests can be written first)

New file `app/src/main/java/com/sherifradwan/ancnative/model/DatingSummary.kt`:

```kotlin
enum class EddLabel { PLAIN, CONFIRMED }
sealed interface DatingOrigin {
    data class Lmp(val date: String) : DatingOrigin
    data class Transfer(val date: String, val embryoAgeDays: Int?) : DatingOrigin
    data object None : DatingOrigin
}
data class DatingSummary(
    val origin: DatingOrigin,
    val edd: LocalDate?,
    val eddLabel: EddLabel,
    val methodCode: String?,
    val discordance: RedatingAdvice?,
)
fun datingSummary(dating: DemoPatientDating?, earliestScanDate: LocalDate?, today: LocalDate): DatingSummary
```

New function in `model/IsoDates.kt`: `fun formatDmy(iso: String?): String?` returns `dd/mm/yyyy` with `Locale.ROOT`, null for null or blank, and the trimmed raw string if it does not parse.

Reuse, do not copy: `GestationalAgeEngine.calculateEstimatedDueDate`, `GestationalAgeEngine.dueDateForMethod`, `redatingAdvice` and `redatingThresholdDays` in `model/Redating.kt`, `parseIsoDateOrNull`. Do not copy the tier table.

## Rules (oracles)

Origin (row 1), keyed on the normalised method (case-insensitive, trimmed; "IVF" and "IVF_TRANSFER" are the transfer method):
- O1. Transfer method and non-blank `transferDate`: `Transfer(transferDate, embryoAgeDays)`. A null embryo age stays null (no default of 5 here).
- O2. Transfer method and blank `transferDate`: `None`. Never show an LMP for an IVF patient.
- O3. Any other method, or a blank or null method, and non-blank `lmpDate`: `Lmp(lmpDate)`. This includes Ultrasound and Clinical patients with an LMP on file.
- O4. Otherwise `None`. A malformed date string is kept as given (shown raw, never hidden).

EDD:
- E1. `edd` = `GestationalAgeEngine.calculateEstimatedDueDate(dating)`. Null dating gives null.
- E2. `eddLabel` is CONFIRMED only when `lmpDate`, `transferDate` and `ultrasoundEdd` are all blank and `agreedEdd` is non-blank. Otherwise PLAIN. An existing agreed EDD alone never makes it CONFIRMED when LMP, ET or a scan EDD exists.

Method:
- M1. `methodCode` is one of `LMP`, `Ultrasound`, `IVF_TRANSFER`, `Clinical` for the known codes (case-insensitive, aliases "IVF", "US" accepted by the engine map to the canonical code), the trimmed raw string for unknown values, null for blank or null.

Discordance (alert and the red `??` marker):
- D1. Considered only when the method is not the transfer method, `lmpDate` and `ultrasoundEdd` both parse, and the LMP due date (`dueDateForMethod(dating, "LMP")`) exists.
- D2. Measured at `earliestScanDate`. If it is null, measure at `today`.
- D3. Dating GA days = days from LMP to the anchor. Scan GA days = 280 minus days from the anchor to `ultrasoundEdd`. The gap is the difference of the two due dates. Pass both to `redatingAdvice(datingGaDays, scanGaDays)`. A result is the alert; null means no alert.
- D4. The gap must be strictly greater than the tier limit (a gap equal to the limit gives no alert). Tier boundaries: GA 62 and 63, 111 and 112, 153 and 154, 195 and 196 days.
- D5. Cleared (null) when the method is Ultrasound, or when `agreedEdd` equals `ultrasoundEdd` as parsed dates.
- D6. Never throws. Blank, malformed or out-of-range dates give null.
- D7. Never fires for IVF patients (transfer method).

## UI oracles

- U1. `DatingSection` renders from `datingSummary` only. It holds no branching on raw fields and has no second EDD row. Rows in order: first row (`LMP` or `ET`), EDD (label `EDD` or `Confirmed EDD`), Method, the alert, Notes.
- U2. Dates are `dd/mm/yyyy` through `formatDmy`. The ET row reads `dd/mm/yyyy · D5` in one text with non-breaking spaces around the dot, and has no `· D5` when the embryo age is null.
- U3. The alert is full width, wraps with no line clamp, has a warning icon plus text, has no button, uses a warning tone distinct from the high-risk banner, and its text uses non-breaking spaces between a number and "d". Reuse the `redating_hint` numbers (gap in days, limit) in a short Overview wording. New test tag for the alert.
- U4. Where the current gestational age is shown in the patient header, the patient list and the gestational banner, the number stays and a red `??` follows when `discordance != null`. One shared composable draws it. Historical GA at visits, scans and labs is unchanged. The `??` is text with a content description, and has enough contrast in light and dark.
- U5. The patient header, patient list and banner, and the Overview use the shared EDD from `datingSummary` or the engine instead of their own agreed or calculated fallbacks (`Components.kt` near line 231, `PatientListScreen.kt` near line 127, `GestationalBanner.kt` near line 42, `OverviewSections.kt`).
- U6. Print (`PatientPrintHtmlGenerator`) and share (`ClinicalSummaryGenerator`) use the same summary: first row `LMP` or `ET`, the method label not the raw code, and when a discordance exists, both due dates and the gap (for example `LMP EDD dd/mm/yyyy · US EDD dd/mm/yyyy · gap 9 d`). Dates are `dd/mm/yyyy`.
- U7. 360dp at 1.3x font scale: nothing clipped, no horizontal scroll.

## Out of scope

Schema or stored-field changes, resolve or dismiss button, a flat threshold, new editor chips, a second EDD row, the alert for IVF, `??` on historical GA.

## Review

Edits under `app/src` are level 3: frontier review plus an independent test review, recorded as `REVIEW:` in the ledger.

## Clarifications from test design

- `differenceDays` keeps the sign `redatingAdvice` gives (scan GA minus dating GA, which equals LMP due date minus scan due date). Only its absolute value is compared to the limit.
- Unknown method strings follow the "other method" branch of O3 for origin.
- A malformed `agreedEdd` does not clear the alert (D5 compares parsed dates only).
- The aliases "US" and "us" normalise to Ultrasound for D5.
- A malformed `transferDate` with the transfer method gives `Transfer(raw, age)`; the origin does not validate the date or the age.

## Amendments after independent review (round 1)

Reviewers: Opus, DeepSeek (code), Sonnet (tests). Evidence in `.agents/fleet-runs/dating_*.txt`.

- D2a. The anchor is the earliest scan whose parsed date is on or after the LMP date (when the LMP parses) and not after `today`. Malformed scan dates, scans before the LMP and scans in the future are ignored. If none remains, the anchor is `today`. Tested through `datingSummaryOf(patient, today)`.
- D6a. `datingSummary` never throws, including for parseable but extreme dates (for example year 9999 or +999999999) in any date field. The result for such input has `edd == null` or no discordance.
- U8. The Delivery Plan card (`DeliveryPlanningCard`) shows the shared EDD (`summary.edd`, `dd/mm/yyyy`, the shared label `EDD` or `Confirmed EDD`) and its current-GA tile carries the red `??` marker when discordant. It has no `agreedEdd ?: ultrasoundEdd` fallback.
- U9. Print "Target Delivery Timing" uses the shared EDD in `dd/mm/yyyy` (no raw ISO, no `Not recorded` when an EDD exists).
- U10. `GestationalBanner` and the dead `STICKY_COMPACT` card variant have no production caller and are deleted. The marker tests move to the shared GA badge and to the header and patient list surfaces.
- U11. `DatingEditor` uses `canonicalDatingMethod` instead of its own raw string comparison for the IVF fields.
- U12. The `??` is not clipped at 360dp and 1.3x font scale in the patient list row.
- Open owner decisions (not built): scan EDD hidden below the limit or under the Ultrasound method; Method row when no method is recorded; transfer date recorded under a non-IVF method; tier drift when no scan exists.

## Amendments after round 2 review (final round)

- D6b. A scan EDD more than 560 days (2 x full term) from the anchor, or an LMP more than 560 days from the anchor in either direction, gives no discordance. Boundary: 560 days is accepted, 561 days gives none.
- D6c. Every consumer of the summary (`datingDiscordanceLine`, the Overview alert, print, share) is safe for extreme dates: nothing throws and no negative or wrapped gap is ever printed.
- Known and deferred: backup export/restore (`EncryptedBackupManager`) collapses `agreedEdd` and `ultrasoundEdd` into one value, which can clear the alert after a restore. Pre-existing, outside this change.
