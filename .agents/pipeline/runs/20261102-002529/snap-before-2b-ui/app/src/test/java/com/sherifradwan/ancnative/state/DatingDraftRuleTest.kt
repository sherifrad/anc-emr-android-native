package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.GestationalAgeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DatingDraftRuleTest {

    private val asOf = LocalDate.of(2026, 8, 1)

    private fun totalDays(values: DatingDraftValues): Long =
        GestationalAgeEngine.calculate(datingDraftToDating("p1", values), asOf)!!.totalDays

    private fun change(values: DatingDraftValues, field: DatingDraftField, value: String) =
        applyDatingChange(values, field, value)

    private val lmpDraft = DatingDraftValues(lmpDate = "2026-02-01", agreedEdd = "2026-11-08", datingMethod = "LMP")
    private val ivfDraft = DatingDraftValues(
        agreedEdd = "2026-11-19",
        datingMethod = "IVF_TRANSFER",
        transferDate = "2026-03-01",
        embryoAgeDays = "3",
    )

    @Test
    fun draftToDatingTurnsBlanksIntoNullAndParsesEmbryoDay() {
        val dating = datingDraftToDating("p9", DatingDraftValues(embryoAgeDays = "5", lmpDate = "2026-02-01"))
        assertEquals("p9", dating.patientId)
        assertEquals("2026-02-01", dating.lmpDate)
        assertNull(dating.ultrasoundEdd)
        assertNull(dating.agreedEdd)
        assertNull(dating.datingMethod)
        assertNull(dating.transferDate)
        assertNull(dating.notes)
        assertEquals(5, dating.embryoAgeDays)
        assertNull(datingDraftToDating("p9", DatingDraftValues(embryoAgeDays = "x")).embryoAgeDays)
    }

    @Test
    fun lmpEditWithBlankAgreedSetsAgreedAndMovesGestationalAge() {
        val before = DatingDraftValues(lmpDate = "2026-02-01", datingMethod = "LMP")
        val after = change(before, DatingDraftField.LMP_DATE, "2026-02-08")
        assertEquals("2026-11-15", after.agreedEdd)
        assertEquals(7L, totalDays(before) - totalDays(after))
    }

    @Test
    fun lmpEditFollowsWhenAgreedEqualsOldMethodDate() {
        val after = change(lmpDraft, DatingDraftField.LMP_DATE, "2026-02-08")
        assertEquals("2026-11-15", after.agreedEdd)
        assertEquals(7L, totalDays(lmpDraft) - totalDays(after))
    }

    @Test
    fun embryoDayEditsMoveAgreedAndGestationalAge() {
        val day5 = change(ivfDraft, DatingDraftField.EMBRYO_AGE_DAYS, "5")
        assertEquals("2026-11-17", day5.agreedEdd)
        assertEquals(2L, totalDays(day5) - totalDays(ivfDraft))
        val day6 = change(day5, DatingDraftField.EMBRYO_AGE_DAYS, "6")
        assertEquals("2026-11-16", day6.agreedEdd)
        assertEquals(1L, totalDays(day6) - totalDays(day5))
    }

    @Test
    fun transferDateEditMovesFollowingAgreed() {
        val after = change(ivfDraft, DatingDraftField.TRANSFER_DATE, "2026-03-08")
        assertEquals("2026-11-26", after.agreedEdd)
    }

    @Test
    fun switchingMethodRepointsFollowingAgreed() {
        val draft = lmpDraft.copy(transferDate = "2026-03-01", embryoAgeDays = "5")
        val after = change(draft, DatingDraftField.DATING_METHOD, "IVF_TRANSFER")
        assertEquals("2026-11-17", after.agreedEdd)
    }

    @Test
    fun scanEditFollowsWhenMethodIsUltrasound() {
        val draft = DatingDraftValues(ultrasoundEdd = "2026-11-10", agreedEdd = "2026-11-10", datingMethod = "Ultrasound")
        assertEquals("2026-11-12", change(draft, DatingDraftField.ULTRASOUND_EDD, "2026-11-12").agreedEdd)
    }

    @Test
    fun manualAgreedDateIsUntouchedByInputEdits() {
        val manual = lmpDraft.copy(agreedEdd = "2026-12-01")
        assertTrue(isManualAgreedEdd(manual))
        assertEquals("2026-12-01", change(manual, DatingDraftField.LMP_DATE, "2026-02-08").agreedEdd)
        val manualIvf = ivfDraft.copy(agreedEdd = "2026-12-01")
        assertTrue(isManualAgreedEdd(manualIvf))
        assertEquals("2026-12-01", change(manualIvf, DatingDraftField.EMBRYO_AGE_DAYS, "6").agreedEdd)
    }

    @Test
    fun followingAgreedIsNotManual() {
        assertFalse(isManualAgreedEdd(lmpDraft))
        assertFalse(isManualAgreedEdd(DatingDraftValues(lmpDate = "2026-02-01", datingMethod = "LMP")))
    }

    @Test
    fun clearingAgreedMakesTheNextLmpEditFollow() {
        val cleared = change(lmpDraft.copy(agreedEdd = "2026-12-01"), DatingDraftField.AGREED_EDD, "")
        assertEquals("", cleared.agreedEdd)
        assertEquals("2026-11-15", change(cleared, DatingDraftField.LMP_DATE, "2026-02-08").agreedEdd)
    }

    @Test
    fun typingAgreedDateMakesItManualAndOnlySetsTheField() {
        val typed = change(lmpDraft, DatingDraftField.AGREED_EDD, "2026-12-01")
        assertEquals(lmpDraft.copy(agreedEdd = "2026-12-01"), typed)
        assertTrue(isManualAgreedEdd(typed))
    }

    @Test
    fun halfTypedDateGivesBlankAgreedWithoutCrash() {
        val after = change(lmpDraft, DatingDraftField.LMP_DATE, "2026-02-")
        assertEquals("2026-02-", after.lmpDate)
        assertEquals("", after.agreedEdd)
    }

    @Test
    fun notesEditNeverChangesAgreed() {
        val after = change(lmpDraft, DatingDraftField.NOTES, "scan later")
        assertEquals("scan later", after.notes)
        assertEquals(lmpDraft.agreedEdd, after.agreedEdd)
    }
}
