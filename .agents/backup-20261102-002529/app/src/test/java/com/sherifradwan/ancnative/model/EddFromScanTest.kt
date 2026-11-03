package com.sherifradwan.ancnative.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Round 4g / Q270: one due-date rule for a first-trimester scan (SPEC_FIX_R4.md). */
class EddFromScanTest {
    private val scan = LocalDate.of(2026, 3, 1)

    @Test fun eightWeeksThreeDays() = assertEquals(LocalDate.of(2026, 10, 8), eddFromScan(scan, 8, 3))

    @Test fun twelveWeeksZeroDays() = assertEquals(LocalDate.of(2026, 9, 13), eddFromScan(scan, 12, 0))

    @Test fun sevenDaysIsInvalid() = assertNull(eddFromScan(scan, 8, 7))

    @Test fun negativeDaysIsInvalid() = assertNull(eddFromScan(scan, 8, -1))

    @Test fun negativeWeeksIsInvalid() = assertNull(eddFromScan(scan, -1, 0))

    @Test fun weeks43IsInvalid() = assertNull(eddFromScan(scan, 43, 0))

    @Test fun weeks42IsAccepted() = assertEquals(LocalDate.of(2026, 3, 1).minusDays(14), eddFromScan(scan, 42, 0))

    @Test fun zeroWeeksIsAccepted() = assertEquals(LocalDate.of(2026, 12, 6), eddFromScan(scan, 0, 0))
}
