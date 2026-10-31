package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatientDating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Round 3a / Q20: saving the profile never overwrites existing dating (SPEC_FIX_R3.md). */
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

    // ---- spec examples ----
    @Test fun noExisting_validDraft() =
        assertEquals(IntakeDating("2026-02-01", "2026-11-08"), intakeDatingUpdate(null, "2026-02-01"))

    @Test fun noExisting_paddedDraft() =
        assertEquals(IntakeDating("2026-02-01", "2026-11-08"), intakeDatingUpdate(null, "  2026-02-01 "))

    @Test fun noExisting_invalidFormat() = assertNull(intakeDatingUpdate(null, "01/02/2026"))
    @Test fun noExisting_emptyDraft() = assertNull(intakeDatingUpdate(null, ""))
    @Test fun noExisting_nullDraft() = assertNull(intakeDatingUpdate(null, null))

    @Test fun sameLmp_null() = assertNull(intakeDatingUpdate(dating(), "2026-02-01"))

    @Test fun lmpOnlyChanged_updates() =
        assertEquals(IntakeDating("2026-02-08", "2026-11-15"), intakeDatingUpdate(dating(), "2026-02-08"))

    @Test fun existingUltrasoundEdd_null() =
        assertNull(intakeDatingUpdate(dating(uss = "2026-11-12"), "2026-02-08"))

    @Test fun ivfDating_null() =
        assertNull(
            intakeDatingUpdate(
                dating(lmp = null, method = "IVF", transfer = "2026-02-15", embryo = 5),
                "2026-02-01",
            ),
        )

    @Test fun ultrasoundMethodWithoutEdd_null() =
        assertNull(intakeDatingUpdate(dating(method = "Ultrasound", uss = null), "2026-02-08"))

    @Test fun nullMethodCountsAsLmp() =
        assertEquals(IntakeDating("2026-02-08", "2026-11-15"), intakeDatingUpdate(dating(method = null), "2026-02-08"))

    // ---- own boundaries ----
    @Test fun leapYear_plus280() =
        assertEquals(IntakeDating("2028-02-29", "2028-12-05"), intakeDatingUpdate(null, "2028-02-29"))

    @Test fun whitespaceUltrasoundEdd_treatedAsMissing() =
        assertEquals(
            IntakeDating("2026-02-08", "2026-11-15"),
            intakeDatingUpdate(dating(uss = "   "), "2026-02-08"),
        )

    @Test fun lowercaseLmpMethod_isLmp() =
        assertEquals(IntakeDating("2026-02-08", "2026-11-15"), intakeDatingUpdate(dating(method = "lmp"), "2026-02-08"))

    @Test fun impossibleDate_null() = assertNull(intakeDatingUpdate(null, "2026-02-30"))

    @Test fun existingLmpPaddedEqualsDraft_null() =
        assertNull(intakeDatingUpdate(dating(lmp = " 2026-02-01 "), "2026-02-01"))
}
