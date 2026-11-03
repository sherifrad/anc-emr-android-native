package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DueDatesByMethodTest {

    private val asOf = LocalDate.of(2026, 8, 1)

    private fun dating(
        lmp: String? = null,
        scan: String? = null,
        agreed: String? = null,
        method: String? = null,
        transfer: String? = null,
        embryoDay: Int? = null,
    ) = DemoPatientDating(
        patientId = "p1",
        lmpDate = lmp,
        ultrasoundEdd = scan,
        agreedEdd = agreed,
        datingMethod = method,
        notes = null,
        transferDate = transfer,
        embryoAgeDays = embryoDay,
    )

    @Test
    fun eachMethodIsFilledFromItsOwnInputs() {
        val due = GestationalAgeEngine.dueDatesByMethod(
            dating(lmp = "2026-02-01", scan = "2026-11-10", transfer = "2026-03-01", embryoDay = 5),
        )
        assertEquals(LocalDate.of(2026, 11, 8), due.lmp)
        assertEquals(LocalDate.of(2026, 11, 17), due.ivf)
        assertEquals(LocalDate.of(2026, 11, 10), due.scan)
    }

    @Test
    fun missingInputsGiveNull() {
        val due = GestationalAgeEngine.dueDatesByMethod(dating(lmp = "2026-02-08"))
        assertEquals(LocalDate.of(2026, 11, 15), due.lmp)
        assertNull(due.ivf)
        assertNull(due.scan)
    }

    @Test
    fun ivfDueDateFollowsEmbryoDay() {
        fun ivf(day: Int) = GestationalAgeEngine.dueDateForMethod(
            dating(transfer = "2026-03-01", embryoDay = day),
            "IVF_TRANSFER",
        )
        assertEquals(LocalDate.of(2026, 11, 19), ivf(3))
        assertEquals(LocalDate.of(2026, 11, 17), ivf(5))
        assertEquals(LocalDate.of(2026, 11, 16), ivf(6))
    }

    @Test
    fun ivfWithoutEmbryoDayUsesDayFive() {
        assertEquals(
            LocalDate.of(2026, 11, 17),
            GestationalAgeEngine.dueDateForMethod(dating(transfer = "2026-03-01"), "ivf"),
        )
    }

    @Test
    fun invalidEmbryoDayGivesNull() {
        assertNull(GestationalAgeEngine.dueDateForMethod(dating(transfer = "2026-03-01", embryoDay = 4), "IVF_TRANSFER"))
    }

    @Test
    fun methodNameIsCaseInsensitive() {
        val d = dating(lmp = "2026-02-01", scan = "2026-11-10")
        assertEquals(LocalDate.of(2026, 11, 8), GestationalAgeEngine.dueDateForMethod(d, "lmp"))
        assertEquals(LocalDate.of(2026, 11, 10), GestationalAgeEngine.dueDateForMethod(d, "us"))
    }

    @Test
    fun blankOrClinicalMethodFallsBackScanThenLmp() {
        val both = dating(lmp = "2026-02-01", scan = "2026-11-10")
        val lmpOnly = dating(lmp = "2026-02-01")
        for (method in listOf(null, "", "Clinical")) {
            assertEquals(LocalDate.of(2026, 11, 10), GestationalAgeEngine.dueDateForMethod(both, method))
            assertEquals(LocalDate.of(2026, 11, 8), GestationalAgeEngine.dueDateForMethod(lmpOnly, method))
        }
    }

    @Test
    fun agreedDateWinsInEstimatedDueDate() {
        val d = dating(lmp = "2026-02-01", agreed = "2026-12-01", method = "LMP")
        assertEquals(LocalDate.of(2026, 12, 1), GestationalAgeEngine.calculateEstimatedDueDate(d))
    }

    @Test
    fun estimatedDueDateWithoutAgreedUsesTheMethod() {
        val d = dating(lmp = "2026-02-01", method = "LMP")
        assertEquals(LocalDate.of(2026, 11, 8), GestationalAgeEngine.calculateEstimatedDueDate(d))
    }

    @Test
    fun gestationalAgeEqualsAgeFromTheSameDueDate() {
        val cases = listOf(
            dating(lmp = "2026-02-01", method = "LMP"),
            dating(transfer = "2026-03-01", embryoDay = 3, method = "IVF_TRANSFER"),
            dating(scan = "2026-11-10", method = "Ultrasound"),
            dating(lmp = "2026-02-01", agreed = "2026-12-01", method = "LMP"),
        )
        for (d in cases) {
            val due = GestationalAgeEngine.calculateEstimatedDueDate(d)!!
            assertEquals(GestationalAgeEngine.calculateForEdd(due, asOf), GestationalAgeEngine.calculate(d, asOf))
        }
    }
}
