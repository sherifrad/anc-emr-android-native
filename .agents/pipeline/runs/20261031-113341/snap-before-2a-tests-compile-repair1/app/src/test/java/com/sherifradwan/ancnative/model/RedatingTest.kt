package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RedatingTest {

    private val table = listOf(
        62L to 5,
        63L to 7,
        111L to 7,
        112L to 10,
        153L to 10,
        154L to 14,
        195L to 14,
        196L to 21,
    )

    @Test
    fun thresholdFollowsTheAcogTableByDatingGa() {
        for ((datingGa, expected) in table) {
            assertEquals("threshold at dating GA $datingGa", expected, redatingThresholdDays(datingGa))
        }
    }

    @Test
    fun differenceAtTheThresholdIsQuietAndJustOverItAdvises() {
        for ((datingGa, threshold) in table) {
            assertNull(
                "diff == threshold at dating GA $datingGa must not advise",
                redatingAdvice(datingGa, datingGa + threshold),
            )
            val advice = redatingAdvice(datingGa, datingGa + threshold + 1)
            assertNotNull("diff = threshold + 1 at dating GA $datingGa must advise", advice)
            assertEquals(threshold, advice!!.thresholdDays)
            assertEquals(threshold + 1, advice.differenceDays)
        }
    }

    @Test
    fun scanEarlierThanDatingGivesASignedNegativeDifference() {
        val advice = redatingAdvice(100, 100 - 8)
        assertNotNull(advice)
        assertEquals(-8, advice!!.differenceDays)
        assertEquals(7, advice.thresholdDays)

        assertNull("a negative difference within the threshold must not advise", redatingAdvice(100, 100 - 7))
    }

    @Test
    fun differenceZeroIsQuiet() {
        assertNull(redatingAdvice(80, 80))
    }
}
