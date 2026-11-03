package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatientDating
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Round 3a / Q20 and Round 4g / Q273: the intake dating rule (SPEC_FIX_R3.md, SPEC_FIX_R4.md). */
class IntakeDatingRuleTest {

    private fun dating(
        lmp: String? = "2026-02-01",
        uss: String? = null,
        method: String? = "LMP",
        transfer: String? = null,
        embryo: Int? = null,
    ) = DemoPatientDating(
        patientId = "p1", lmpDate = lmp, ultrasoundEdd = uss, agreedEdd = null,
        datingMethod = method, notes = null, transferDate = transfer, embryoAgeDays = embryo,
    )

    private fun lmpValues(lmp: String?) = PatientDraftValues(lmpDate = lmp.orEmpty())

    private fun lmpDating(lmp: String, edd: String) =
        IntakeDating(lmp, edd, note = "Baseline from intake LMP ($lmp)")

    private fun ivfValues(transfer: String = "2026-03-01", embryo: String = "5") =
        PatientDraftValues(datingMethod = "IVF_TRANSFER", transferDate = transfer, embryoAgeDays = embryo)

    private fun scanValues(date: String = "2026-03-01", weeks: String = "8", days: String = "3") =
        PatientDraftValues(datingMethod = "Ultrasound", scanDate = date, scanGaWeeks = weeks, scanGaDays = days)

    private fun clinicalValues(edd: String = "2026-11-20") =
        PatientDraftValues(datingMethod = "Clinical", agreedEdd = edd)

    // ---- spec examples ----
    @Test fun noExisting_validDraft() =
        assertEquals(lmpDating("2026-02-01", "2026-11-08"), intakeDatingUpdate(null, lmpValues("2026-02-01")))

    @Test fun noExisting_paddedDraft() =
        assertEquals(lmpDating("2026-02-01", "2026-11-08"), intakeDatingUpdate(null, lmpValues("  2026-02-01 ")))

    @Test fun noExisting_invalidFormat() = assertNull(intakeDatingUpdate(null, lmpValues("01/02/2026")))
    @Test fun noExisting_emptyDraft() = assertNull(intakeDatingUpdate(null, lmpValues("")))
    @Test fun noExisting_nullDraft() = assertNull(intakeDatingUpdate(null, lmpValues(null)))

    @Test fun sameLmp_null() = assertNull(intakeDatingUpdate(dating(), lmpValues("2026-02-01")))

    @Test fun lmpOnlyChanged_updates() =
        assertEquals(lmpDating("2026-02-08", "2026-11-15"), intakeDatingUpdate(dating(), lmpValues("2026-02-08")))

    @Test fun existingUltrasoundEdd_null() =
        assertNull(intakeDatingUpdate(dating(uss = "2026-11-12"), lmpValues("2026-02-08")))

    @Test fun ivfDating_null() =
        assertNull(
            intakeDatingUpdate(
                dating(lmp = null, method = "IVF", transfer = "2026-02-15", embryo = 5),
                lmpValues("2026-02-01"),
            ),
        )

    @Test fun ultrasoundMethodWithoutEdd_null() =
        assertNull(intakeDatingUpdate(dating(method = "Ultrasound", uss = null), lmpValues("2026-02-08")))

    @Test fun nullMethodCountsAsLmp() =
        assertEquals(
            lmpDating("2026-02-08", "2026-11-15"),
            intakeDatingUpdate(dating(method = null), lmpValues("2026-02-08")),
        )

    // ---- own boundaries ----
    @Test fun leapYear_plus280() =
        assertEquals(lmpDating("2028-02-29", "2028-12-05"), intakeDatingUpdate(null, lmpValues("2028-02-29")))

    @Test fun whitespaceUltrasoundEdd_treatedAsMissing() =
        assertEquals(
            lmpDating("2026-02-08", "2026-11-15"),
            intakeDatingUpdate(dating(uss = "   "), lmpValues("2026-02-08")),
        )

    @Test fun lowercaseLmpMethod_isLmp() =
        assertEquals(
            lmpDating("2026-02-08", "2026-11-15"),
            intakeDatingUpdate(dating(method = "lmp"), lmpValues("2026-02-08")),
        )

    @Test fun impossibleDate_null() = assertNull(intakeDatingUpdate(null, lmpValues("2026-02-30")))

    @Test fun existingLmpPaddedEqualsDraft_null() =
        assertNull(intakeDatingUpdate(dating(lmp = " 2026-02-01 "), lmpValues("2026-02-01")))

    // ---- round 4g: dating choice ----
    @Test fun lmpResultCarriesMethodAndNote() {
        val r = intakeDatingUpdate(null, lmpValues("2026-02-01"))!!
        assertEquals("LMP", r.datingMethod)
        assertEquals("Baseline from intake LMP (2026-02-01)", r.note)
        assertNull(r.ultrasoundEdd)
        assertNull(r.transferDate)
        assertNull(r.embryoAgeDays)
    }

    @Test fun blankMethodBehavesAsLmp() =
        assertEquals(
            intakeDatingUpdate(null, lmpValues("2026-02-01")),
            intakeDatingUpdate(null, PatientDraftValues(datingMethod = "", lmpDate = "2026-02-01")),
        )

    @Test fun explicitLmpMethodSameAsBlank() =
        assertEquals(
            intakeDatingUpdate(null, lmpValues("2026-02-01")),
            intakeDatingUpdate(null, PatientDraftValues(datingMethod = "LMP", lmpDate = "2026-02-01")),
        )

    @Test fun ivf_day5() =
        assertEquals(
            IntakeDating(
                lmpDate = null, agreedEdd = "2026-11-17", datingMethod = "IVF_TRANSFER",
                transferDate = "2026-03-01", embryoAgeDays = 5,
                note = "Baseline from intake IVF transfer (2026-03-01, day 5)",
            ),
            intakeDatingUpdate(null, ivfValues()),
        )

    @Test fun ivf_day4_null() = assertNull(intakeDatingUpdate(null, ivfValues(embryo = "4")))
    @Test fun ivf_blankEmbryoDay_null() = assertNull(intakeDatingUpdate(null, ivfValues(embryo = "")))
    @Test fun ivf_missingTransfer_null() = assertNull(intakeDatingUpdate(null, ivfValues(transfer = "")))
    @Test fun ivf_badTransferDate_null() = assertNull(intakeDatingUpdate(null, ivfValues(transfer = "2026-02-30")))

    @Test fun scan_8w3d() =
        assertEquals(
            IntakeDating(
                lmpDate = null, agreedEdd = "2026-10-08", datingMethod = "Ultrasound", ultrasoundEdd = "2026-10-08",
                note = "Baseline from intake scan (2026-03-01, 8w3d)",
            ),
            intakeDatingUpdate(null, scanValues()),
        )

    @Test fun scan_badGaDays_null() = assertNull(intakeDatingUpdate(null, scanValues(days = "7")))
    @Test fun scan_badGaWeeks_null() = assertNull(intakeDatingUpdate(null, scanValues(weeks = "43")))
    @Test fun scan_nonNumericGa_null() = assertNull(intakeDatingUpdate(null, scanValues(weeks = "x")))
    @Test fun scan_blankGa_null() = assertNull(intakeDatingUpdate(null, scanValues(weeks = "", days = "")))
    @Test fun scan_badDate_null() = assertNull(intakeDatingUpdate(null, scanValues(date = "01/03/2026")))

    @Test fun clinical_dueDateOnly() =
        assertEquals(
            IntakeDating(
                lmpDate = null, agreedEdd = "2026-11-20", datingMethod = "Clinical",
                note = "Baseline from intake due date (2026-11-20)",
            ),
            intakeDatingUpdate(null, clinicalValues()),
        )

    @Test fun clinical_badDate_null() = assertNull(intakeDatingUpdate(null, clinicalValues("20/11/2026")))
    @Test fun clinical_blankDate_null() = assertNull(intakeDatingUpdate(null, clinicalValues("")))

    @Test fun existingPatient_nonLmpMethods_null() {
        val existing = dating()
        assertNull(intakeDatingUpdate(existing, ivfValues()))
        assertNull(intakeDatingUpdate(existing, scanValues()))
        assertNull(intakeDatingUpdate(existing, clinicalValues()))
    }

    // ---- preview ----
    private val asOf = LocalDate.of(2026, 8, 1)

    @Test fun preview_lmp() {
        val p = intakeDatingPreview(lmpValues("2026-02-01"), asOf)!!
        assertEquals("2026-11-08", p.dueDate)
        // 2026-02-01 to 2026-08-01 is 181 days = 25 weeks 6 days (the spec text said 26w2d; the engine's own format has a space)
        assertEquals("25w 6d", p.gestationalAge)
    }

    @Test fun preview_invalid_null() {
        assertNull(intakeDatingPreview(lmpValues("2026-02-30"), asOf))
        assertNull(intakeDatingPreview(PatientDraftValues(), asOf))
    }

    @Test fun preview_scanUsesScanDueDate() =
        assertEquals("2026-10-08", intakeDatingPreview(scanValues(), asOf)!!.dueDate)
}
