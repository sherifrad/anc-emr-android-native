package com.sherifradwan.ancnative.model

import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.DatingDraft
import com.sherifradwan.ancnative.state.DatingDraftValues
import com.sherifradwan.ancnative.storage.ClinicalCommandPreparation
import com.sherifradwan.ancnative.storage.SaveDatingCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class IvfDatingTest {

    private val transfer = LocalDate.of(2026, 1, 1)

    @Test
    fun ivfEddFromTransferMatchesTheOwnerDecidedDays() {
        assertEquals(LocalDate.of(2026, 9, 21), ivfEddFromTransfer(transfer, 3))
        assertEquals(LocalDate.of(2026, 9, 19), ivfEddFromTransfer(transfer, 5))
        assertEquals(LocalDate.of(2026, 9, 18), ivfEddFromTransfer(transfer, 6))
    }

    @Test
    fun gaOnTheTransferDateEqualsFourteenPlusEmbryoDays() {
        val expectations = listOf(
            3 to ("2w 3d" to 17L),
            5 to ("2w 5d" to 19L),
            6 to ("2w 6d" to 20L),
        )
        for ((day, expected) in expectations) {
            val dating = DemoPatientDating(
                patientId = "P_IVF",
                lmpDate = null,
                ultrasoundEdd = null,
                agreedEdd = null,
                datingMethod = "IVF_TRANSFER",
                transferDate = transfer.toString(),
                embryoAgeDays = day,
                notes = null,
            )
            val ga = GestationalAgeEngine.calculate(dating, transfer)
            assertNotNull("GA at transfer for day $day", ga)
            assertEquals(expected.second, ga!!.totalDays)
            assertEquals(expected.first, ga.formatted)
            assertEquals(expected.second, ivfGaDaysAtTransfer(day))
        }
    }

    @Test
    fun unapprovedEmbryoDaysGiveNullGaAndEdd() {
        for (day in listOf(4, 7)) {
            val dating = DemoPatientDating(
                patientId = "P_IVF",
                lmpDate = null,
                ultrasoundEdd = null,
                agreedEdd = null,
                datingMethod = "IVF_TRANSFER",
                transferDate = transfer.toString(),
                embryoAgeDays = day,
                notes = null,
            )
            assertNull("day $day GA", GestationalAgeEngine.calculate(dating, transfer.plusDays(35)))
            assertNull("day $day EDD", GestationalAgeEngine.calculateEstimatedDueDate(dating))
        }
    }

    @Test
    fun vocabularyCarriesExactlyTheOwnerDecidedEmbryoDays() {
        assertEquals(listOf(3, 5, 6), ClinicalVocabulary.embryoAges)
    }

    @Test
    fun datingValidationAndCommandsAcceptDay6AndStillRejectDay4() {
        val validDay6 = SaveDatingCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "patient-ivf",
            expectedPatientRevision = 1,
            datingMethod = "IVF_TRANSFER",
            transferDate = "2026-01-01",
            embryoAgeDays = 6,
        )
        assertTrue(validDay6.isStructurallyValid())
        assertFalse(validDay6.copy(embryoAgeDays = 4).isStructurallyValid())

        val readyState = AncState(
            patients = emptyList(),
            datingDraft = DatingDraft(
                editorSessionId = "session-ivf-6",
                patientId = "patient-ivf",
                basePatientRevision = 1,
                original = DatingDraftValues(),
                values = DatingDraftValues(
                    datingMethod = "IVF_TRANSFER",
                    transferDate = "2026-01-01",
                    embryoAgeDays = "6",
                ),
            ),
        )
        val ready = SaveDatingCommand.prepare(readyState, UUID.randomUUID().toString())
        assertTrue(ready is ClinicalCommandPreparation.Ready)
        val day6 = (ready as ClinicalCommandPreparation.Ready).command as SaveDatingCommand
        assertEquals(6, day6.embryoAgeDays)

        val rejectState = readyState.copy(
            datingDraft = readyState.datingDraft?.copy(
                values = DatingDraftValues(
                    datingMethod = "IVF_TRANSFER",
                    transferDate = "2026-01-01",
                    embryoAgeDays = "4",
                ),
            ),
        )
        val rejected = SaveDatingCommand.prepare(rejectState, UUID.randomUUID().toString())
        assertTrue(rejected is ClinicalCommandPreparation.Rejected)
        assertTrue(
            (rejected as ClinicalCommandPreparation.Rejected).errors
                .contains(ClinicalValidationError.INVALID_VOCABULARY),
        )
    }
}
