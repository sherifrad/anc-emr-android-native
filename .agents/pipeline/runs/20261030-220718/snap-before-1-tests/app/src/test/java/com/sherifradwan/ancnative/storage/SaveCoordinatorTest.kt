package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoFixtures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveCoordinatorTest {
    private val command = SaveVisitCommand(
        operationUuid = "00000000-0000-4000-8000-000000000201",
        editorSessionId = "editor",
        patientId = DemoFixtures.patients.first().id,
        visitId = "00000000-0000-4000-8000-000000000202",
        expectedRevision = null,
        date = "2026-10-09",
        bloodPressure = null,
        weight = null,
        findings = null,
        notes = "Synthetic coordinator test",
        fingerprint = "a".repeat(64),
    )

    @Test
    fun ambiguousSaveReconcilesCommittedReceiptBeforeReturning() = runBlocking {
        val committed = committedOutcome()
        val gateway = ScriptedGateway(SaveOutcome.Ambiguous, committed)
        var reconciliationShown = false
        val result = SaveCoordinator(gateway) { it == 7L }.execute(command, 7L) {
            reconciliationShown = true
        }

        assertTrue(reconciliationShown)
        assertSame(committed, result)
        assertEquals(1, gateway.saveCalls)
        assertEquals(1, gateway.reconcileCalls)
    }

    @Test
    fun ambiguousSaveWithNoReceiptBecomesConfirmedNotSaved() = runBlocking {
        val gateway = ScriptedGateway(
            SaveOutcome.Ambiguous,
            SaveOutcome.Rejected(SaveFailure.CONFIRMED_NOT_SAVED),
        )
        val result = SaveCoordinator(gateway) { true }.execute(command, 2L)
        assertEquals(SaveOutcome.Rejected(SaveFailure.CONFIRMED_NOT_SAVED), result)
    }

    @Test
    fun staleDuplicateAndCrossPatientFailuresRemainDistinct() = runBlocking {
        listOf(
            SaveFailure.STALE_REVISION,
            SaveFailure.DUPLICATE_OPERATION_MISMATCH,
            SaveFailure.CROSS_PATIENT_WRITE,
        ).forEach { failure ->
            val expected = SaveOutcome.Rejected(failure)
            val result = SaveCoordinator(ScriptedGateway(expected, expected)) { true }
                .execute(command, 1L)
            assertEquals(expected, result)
        }
    }

    @Test
    fun lockEpochWinsAgainstLateCommittedCallback() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var epoch = 9L
        val gateway = object : SaveGateway {
            override suspend fun save(command: SaveVisitCommand): SaveOutcome {
                started.complete(Unit)
                release.await()
                return committedOutcome()
            }

            override suspend fun reconcile(command: SaveVisitCommand): SaveOutcome = SaveOutcome.Ambiguous
        }
        val deferred = async { SaveCoordinator(gateway) { it == epoch }.execute(command, 9L) }
        started.await()
        epoch = 10L
        release.complete(Unit)
        assertSame(SaveOutcome.Obsolete, deferred.await())
    }

    private fun committedOutcome(): SaveOutcome.Committed {
        val receipt = SaveReceipt(
            operationUuid = command.operationUuid,
            patientId = command.patientId,
            visitId = command.visitId,
            committedRevision = 1,
            fingerprint = command.fingerprint,
        )
        return SaveOutcome.Committed(RecordSnapshot(DemoFixtures.patients), receipt)
    }

    private class ScriptedGateway(
        private val saveResult: SaveOutcome,
        private val reconcileResult: SaveOutcome,
    ) : SaveGateway {
        var saveCalls = 0
        var reconcileCalls = 0

        override suspend fun save(command: SaveVisitCommand): SaveOutcome {
            saveCalls += 1
            return saveResult
        }

        override suspend fun reconcile(command: SaveVisitCommand): SaveOutcome {
            reconcileCalls += 1
            return reconcileResult
        }
    }
}
