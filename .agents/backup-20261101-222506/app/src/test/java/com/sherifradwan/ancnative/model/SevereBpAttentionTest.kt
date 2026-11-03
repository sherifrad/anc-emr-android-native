package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Round 3q / Q96 (SPEC_FIX_R3.md, section "Round 3q"): severe BP attention helper.
 *
 * Rules:
 *  - Consider only visits whose blood pressure parses (parseBloodPressure != null).
 *  - The LATEST of them (greatest ISO date; same date -> greatest revision) decides.
 *  - Return non-null only when bpLevel(thatVisit.bloodPressure) == BpLevel.SEVERE,
 *    with the visit's trimmed BP text and its date; otherwise null.
 *  - An older severe reading followed by a later normal or raised reading gives null.
 *  - No visits / none parseable -> null.
 */
class SevereBpAttentionTest {

    private fun visit(
        date: String,
        bp: String?,
        revision: Int = 1,
        id: String = "v-$date-$revision",
    ): DemoVisit = DemoVisit(
        id = id,
        date = date,
        bloodPressure = bp,
        weight = null,
        findings = null,
        notes = null,
        revision = revision,
        isSyntheticEdit = false,
    )

    @Test
    fun noVisits_returnsNull() {
        assertNull(severeBpAttention(emptyList()))
    }

    @Test
    fun oneNormal_returnsNull() {
        assertNull(severeBpAttention(listOf(visit("2026-10-01", "120/80"))))
        assertNull(severeBpAttention(listOf(visit("2026-10-01", "139/89"))))
    }

    @Test
    fun oneSevere_returnsValueAndDate() {
        val result = severeBpAttention(listOf(visit("2026-10-01", "160/100")))
        assertEquals(SevereBpAttention("160/100", "2026-10-01"), result)
    }

    @Test
    fun olderSevere_thenLaterNormal_returnsNull() {
        val visits = listOf(
            visit("2026-09-01", "160/100"),
            visit("2026-10-01", "120/80"),
        )
        assertNull(severeBpAttention(visits))
    }

    @Test
    fun olderSevere_thenLaterRaised_returnsNull() {
        val visits = listOf(
            visit("2026-09-01", "160/100"),
            visit("2026-10-01", "145/92"),
        )
        assertNull(severeBpAttention(visits))
    }

    @Test
    fun olderNormal_thenLaterSevere_returnsNonNull() {
        val visits = listOf(
            visit("2026-09-01", "120/80"),
            visit("2026-10-01", "160/100"),
        )
        assertEquals(
            SevereBpAttention("160/100", "2026-10-01"),
            severeBpAttention(visits),
        )

        // List order in input must not matter; latest date decides
        val reversed = listOf(
            visit("2026-10-01", "160/100"),
            visit("2026-09-01", "120/80"),
        )
        assertEquals(
            SevereBpAttention("160/100", "2026-10-01"),
            severeBpAttention(reversed),
        )
    }

    @Test
    fun laterVisitWithUnparseableBp_isSkipped_latestParseableDecides() {
        // Later visit has unparseable BP text -> skipped, earlier severe decides
        val visitsUnparseable = listOf(
            visit("2026-09-01", "160/100"),
            visit("2026-10-01", "invalid-bp"),
        )
        assertEquals(
            SevereBpAttention("160/100", "2026-09-01"),
            severeBpAttention(visitsUnparseable),
        )

        // Later visit has null BP -> skipped, earlier severe decides
        val visitsNull = listOf(
            visit("2026-09-01", "160/100"),
            visit("2026-10-01", null),
        )
        assertEquals(
            SevereBpAttention("160/100", "2026-09-01"),
            severeBpAttention(visitsNull),
        )

        // Later visit has blank BP -> skipped, earlier severe decides
        val visitsBlank = listOf(
            visit("2026-09-01", "160/100"),
            visit("2026-10-01", "   "),
        )
        assertEquals(
            SevereBpAttention("160/100", "2026-09-01"),
            severeBpAttention(visitsBlank),
        )

        // Only unparseable visits -> null
        val allUnparseable = listOf(
            visit("2026-09-01", "abc"),
            visit("2026-10-01", null),
        )
        assertNull(severeBpAttention(allUnparseable))
    }

    @Test
    fun sameDateTie_decidedByRevision() {
        // Same date: revision 1 severe, revision 2 normal -> revision 2 decides -> null
        val tieNormalWins = listOf(
            visit("2026-10-01", "160/100", revision = 1),
            visit("2026-10-01", "120/80", revision = 2),
        )
        assertNull(severeBpAttention(tieNormalWins))

        // Same date: revision 1 normal, revision 2 severe -> revision 2 decides -> non-null
        val tieSevereWins = listOf(
            visit("2026-10-01", "120/80", revision = 1),
            visit("2026-10-01", "160/100", revision = 2),
        )
        assertEquals(
            SevereBpAttention("160/100", "2026-10-01"),
            severeBpAttention(tieSevereWins),
        )

        // Same date tie when list order is reversed
        val tieSevereWinsReversed = listOf(
            visit("2026-10-01", "160/100", revision = 2),
            visit("2026-10-01", "120/80", revision = 1),
        )
        assertEquals(
            SevereBpAttention("160/100", "2026-10-01"),
            severeBpAttention(tieSevereWinsReversed),
        )
    }

    @Test
    fun boundaries_159_109_null_and_160_80_120_110_nonNull() {
        // 159/109 is raised (not severe) -> null
        assertNull(severeBpAttention(listOf(visit("2026-10-01", "159/109"))))

        // 160/80 has systolic == 160 (severe) -> non-null
        assertEquals(
            SevereBpAttention("160/80", "2026-10-01"),
            severeBpAttention(listOf(visit("2026-10-01", "160/80"))),
        )

        // 120/110 has diastolic == 110 (severe) -> non-null
        assertEquals(
            SevereBpAttention("120/110", "2026-10-01"),
            severeBpAttention(listOf(visit("2026-10-01", "120/110"))),
        )
    }

    @Test
    fun bpTextWithSpacesOrMmhg_isReturnedTrimmed() {
        val visitWithMmhg = visit("2026-10-01", "  160 / 110 mmHg  ")
        assertEquals(
            SevereBpAttention("160 / 110 mmHg", "2026-10-01"),
            severeBpAttention(listOf(visitWithMmhg)),
        )

        val visitWithSpaces = visit("2026-10-01", " 165/105 ")
        assertEquals(
            SevereBpAttention("165/105", "2026-10-01"),
            severeBpAttention(listOf(visitWithSpaces)),
        )
    }
}
