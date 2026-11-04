package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.storage.ClinicalCommand
import com.sherifradwan.ancnative.storage.ClinicalCommandFailure
import com.sherifradwan.ancnative.storage.ClinicalCommandOutcome
import com.sherifradwan.ancnative.storage.ClinicalCommandReceipt
import com.sherifradwan.ancnative.storage.DeletableRecord
import com.sherifradwan.ancnative.storage.DeletableRegistry
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.storage.DeleteRecordCommand
import com.sherifradwan.ancnative.storage.RecordSnapshot
import com.sherifradwan.ancnative.storage.RecordUndoCommand
import com.sherifradwan.ancnative.storage.RestoreRecordCommand
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.storage.contentHash
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.PriorityQueue
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/** Drives UndoCoordinator only through its public surface and three fake lambdas, on virtual time. */
class UndoCoordinatorTest {
    /** Minimal virtual-time dispatcher (kotlinx-coroutines-test is not a test dependency). */
    @OptIn(InternalCoroutinesApi::class)
    private class VirtualTime : CoroutineDispatcher(), Delay {
        private class Task(val at: Long, val seq: Long, val run: Runnable) {
            var cancelled = false
        }

        private val queue = PriorityQueue<Task>(compareBy<Task>({ it.at }, { it.seq }))
        private var seq = 0L
        var now = 0L
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            queue.add(Task(now, seq++, block))
        }

        override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
            val task = Task(now + timeMillis, seq++, Runnable { continuation.resume(Unit) })
            queue.add(task)
            continuation.invokeOnCancellation { task.cancelled = true }
        }

        fun advanceBy(ms: Long) {
            val target = now + ms
            while (true) {
                val task = queue.peek() ?: break
                if (task.at > target) break
                queue.poll()
                if (task.cancelled) continue
                now = maxOf(now, task.at)
                task.run.run()
            }
            now = target
        }

        fun runCurrent() = advanceBy(0)
    }

    private data class Target(val type: DeletableType, val patientId: String, val recordId: String)

    private val types = listOf(DeletableType.MEDICATION, DeletableType.LAB, DeletableType.ULTRASOUND, DeletableType.VISIT, DeletableType.PREVIOUS_PREGNANCY, DeletableType.ATTACHMENT)

    private fun med(patientId: String, id: String, revision: Long) = DemoMedicationRecord(
        id = id, patientId = patientId, name = "Iron $patientId-$id", dosage = "SECRET-DOSE", route = "Oral",
        frequency = "Once daily", startDate = "2026-01-01", endDate = null, status = "Active",
        notes = "SECRET-NOTE", isProphylaxis = false, revision = revision,
    )

    private fun lab(patientId: String, id: String, revision: Long) = DemoLabInvestigation(
        id = id, patientId = patientId, date = "2026-01-02", testCategory = "Hematology", testName = "Hb $patientId-$id",
        result = "SECRET-RESULT", unit = "g/dL", referenceRange = null, isAbnormal = false, notes = "SECRET-NOTE",
        revision = revision,
    )

    private fun us(patientId: String, id: String, revision: Long) = DemoUltrasoundScan(
        id = id, patientId = patientId, date = "2026-01-03", gestationalAgeWeeks = 20, gestationalAgeDays = 3,
        fetalNumber = "Single", presentation = "Cephalic", placentalSite = "Posterior", amnioticFluid = "Normal",
        fetalHeartRate = "142", biometryBpd = "48", biometryHc = "175", biometryAc = "150", biometryFl = "32",
        estimatedFetalWeight = "350g", impression = "SECRET-IMPRESSION", revision = revision,
    )

    private fun visit(patientId: String, id: String, revision: Long) = DemoVisit(
        id = id, patientId = patientId, date = "2026-01-04", bloodPressure = "120/80", weight = "65.0",
        findings = "Normal", notes = "SECRET-NOTE", revision = revision, isSyntheticEdit = false,
    )

    private fun prev(patientId: String, id: String, revision: Long) = DemoPreviousPregnancy(
        id = id, patientId = patientId, modelVersion = 1, dateText = "2024-01-01", outcomeText = "Live birth",
        pregnancyType = "Singleton", otherDetails = "SECRET-DETAILS", placeAndDoctor = "City", deliveryBy = "Vaginal",
        outcome = "LiveBirth", birthMethod = "Spontaneous", lossManagement = null, terminationMethod = null,
        provenanceKind = "CLINICAL", clinicianId = "dr1", recordedAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z", revision = revision,
    )

    // "x1" exists as a medication AND a lab AND an ultrasound AND a visit AND an attachment, for p1 AND p2, each with its own revision; "y1" is a second p1 record.
    // "onlyMed" exists only as a p1 medication.
    private fun att(patientId: String, id: String, revision: Long) = DemoAttachment(
        id = id, patientId = patientId, fileName = "SECRET-$patientId-$id.webp", filePath = "attachments/$patientId/$id.webp",
        category = "Ultrasound", fileSizeBytes = 1024, sha256Hash = "00", title = "Scan $patientId-$id",
        capturedAt = "2026-01-05T00:00:00Z", revision = revision,
    )

    private val originals: List<DeletableRecord> = listOf(
        med("p1", "x1", 3), med("p1", "y1", 3), med("p1", "onlyMed", 3), med("p2", "x1", 5),
        lab("p1", "x1", 4), lab("p1", "y1", 4), lab("p2", "x1", 6),
        us("p1", "x1", 5), us("p1", "y1", 5), us("p2", "x1", 7),
        visit("p1", "x1", 6), visit("p1", "y1", 6), visit("p2", "x1", 8),
        prev("p1", "x1", 7), prev("p1", "y1", 7), prev("p2", "x1", 9),
        att("p1", "x1", 8), att("p1", "y1", 8), att("p2", "x1", 10),
    )

    private fun patient(id: String, revision: Long) = DemoPatient(
        id = id, caseId = id.uppercase(), displayName = "Patient $id", dateOfBirth = "1995-01-01",
        phone = "01000000000",
        visits = originals.filterIsInstance<DemoVisit>().filter { it.patientId == id },
        previousPregnancies = originals.filterIsInstance<DemoPreviousPregnancy>().filter { it.patientId == id },
        revision = revision,
        medications = originals.filterIsInstance<DemoMedicationRecord>().filter { it.patientId == id },
        labInvestigations = originals.filterIsInstance<DemoLabInvestigation>().filter { it.patientId == id },
        ultrasoundScans = originals.filterIsInstance<DemoUltrasoundScan>().filter { it.patientId == id },
        attachments = originals.filterIsInstance<DemoAttachment>().filter { it.patientId == id },
    )

    private fun original(target: Target): DeletableRecord? = originals.firstOrNull {
        it.patientId == target.patientId && it.id == target.recordId && DeletableRegistry.adapterFor(it).type == target.type
    }

    private fun labelOf(target: Target): String {
        val record = original(target)!!
        return DeletableRegistry.adapterFor(record).label(record)
    }

    private val fixtures = mutableListOf<Fixture>()

    @After
    fun tearDown() {
        fixtures.forEach { it.scope.cancel() }
    }

    private inner class Fixture {
        val time = VirtualTime()
        val scope = CoroutineScope(time + SupervisorJob())
        var state = AncState(
            patients = listOf(patient("p1", 5), patient("p2", 9)),
            vaultState = VaultState.Unlocked(1L),
        )
        val events = mutableListOf<AncEvent>()
        val calls = mutableListOf<Pair<ClinicalCommand, Long>>()

        /** Each entry runs when execute is called; it may change the state and returns null for "commit". */
        val script = ArrayDeque<() -> ClinicalCommandOutcome?>()

        var snapshotOverride: (() -> RecordSnapshot?)? = null

        val coordinator = UndoCoordinator(
            scope = scope,
            execute = { cmd, epoch ->
                calls += cmd to epoch
                script.removeFirstOrNull()?.invoke() ?: commit(cmd)
            },
            reduce = { event ->
                events += event
                state = AncReducer.reduce(state, event)
            },
            currentState = { state },
            snapshot = { snapshotOverride?.invoke() ?: RecordSnapshot(state.patients) },
        )

        init {
            fixtures += this
        }

        val executed: List<ClinicalCommand> get() = calls.map { it.first }
        val token: String? get() = state.undoOffer?.token

        fun begin(type: DeletableType, patientId: String, recordId: String): String? {
            coordinator.beginDelete(type, patientId, recordId)
            time.runCurrent()
            return token
        }

        fun request(token: String) {
            coordinator.request(token)
            time.runCurrent()
        }

        fun bumpPatientRevision(patientId: String, to: Long) {
            state = state.copy(patients = state.patients.map { if (it.id == patientId) it.copy(revision = to) else it })
        }

        fun changeEpoch(to: Long) {
            state = state.copy(vaultState = VaultState.Unlocked(to))
        }

        fun commit(cmd: ClinicalCommand): ClinicalCommandOutcome {
            val undo = cmd as RecordUndoCommand
            val patient = state.patient(undo.patientId)!!
            val updated = when (undo) {
                is DeleteRecordCommand -> when (undo.type) {
                    DeletableType.MEDICATION -> patient.copy(medications = patient.medications.filterNot { it.id == undo.recordId })
                    DeletableType.LAB -> patient.copy(labInvestigations = patient.labInvestigations.filterNot { it.id == undo.recordId })
                    DeletableType.ULTRASOUND -> patient.copy(ultrasoundScans = patient.ultrasoundScans.filterNot { it.id == undo.recordId })
                    DeletableType.VISIT -> patient.copy(visits = patient.visits.filterNot { it.id == undo.recordId })
                    DeletableType.PREVIOUS_PREGNANCY -> patient.copy(previousPregnancies = patient.previousPregnancies.filterNot { it.id == undo.recordId })
                    DeletableType.ATTACHMENT -> patient.copy(attachments = patient.attachments.filterNot { it.id == undo.recordId })
                }
                is RestoreRecordCommand -> when (val record = undo.record) {
                    is DemoMedicationRecord -> patient.copy(medications = patient.medications + record)
                    is DemoLabInvestigation -> patient.copy(labInvestigations = patient.labInvestigations + record)
                    is DemoUltrasoundScan -> patient.copy(ultrasoundScans = patient.ultrasoundScans + record)
                    is DemoVisit -> patient.copy(visits = patient.visits + record)
                    is DemoPreviousPregnancy -> patient.copy(previousPregnancies = patient.previousPregnancies + record)
                    is DemoAttachment -> patient.copy(attachments = patient.attachments + record)
                    else -> error("unexpected record ${record::class}")
                }
            }.copy(revision = patient.revision + 1)
            val receipt = ClinicalCommandReceipt(
                operationUuid = undo.operationUuid,
                patientId = undo.patientId,
                recordKind = undo.recordKind.name,
                recordId = undo.recordId,
                committedRevision = 1,
                committedPatientRevision = updated.revision,
                fingerprint = undo.computeFingerprint(),
            )
            return ClinicalCommandOutcome.Committed(
                RecordSnapshot(state.patients.map { if (it.id == patient.id) updated else it }),
                receipt,
            )
        }

        /** D654: every executed command is for one of [allowed] (type, patient, record), with that record's own data. */
        fun assertExecutedOnly(vararg allowed: Target) {
            for ((cmd, epoch) in calls) {
                assertEquals(1L, epoch)
                val undo = cmd as? RecordUndoCommand ?: error("not a record command: $cmd")
                val key = when (undo) {
                    is DeleteRecordCommand -> Target(undo.type, undo.patientId, undo.recordId)
                    is RestoreRecordCommand -> Target(undo.type, undo.patientId, undo.recordId)
                }
                assertTrue("command for $key, allowed ${allowed.toList()}", key in allowed)
                val record = original(key)!!
                when (undo) {
                    is DeleteRecordCommand -> {
                        assertEquals(record.revision, undo.expectedRecordRevision)
                        assertEquals(contentHash(record), undo.deletedContentHash)
                    }
                    is RestoreRecordCommand -> {
                        assertEquals(record, undo.record)
                        assertEquals(key, Target(undo.sourceDelete.type, undo.sourceDelete.patientId, undo.sourceDelete.recordId))
                    }
                }
            }
        }

        fun expiredTokens(): List<String> = events.filterIsInstance<AncEvent.UndoExpired>().map { it.token }
    }

    private fun target(type: DeletableType, id: String = "x1", patientId: String = "p1") = Target(type, patientId, id)

    @Test
    fun fixtureCoversEveryDeletableType() {
        assertEquals(DeletableType.values().toSet(), types.toSet())
    }

    @Test
    fun beginDeleteExecutesOnlyTheConfirmedDeleteAndOffersLabelOnly() {
        for (type in types) {
            val fx = Fixture()
            val a = target(type)
            val token = fx.begin(type, "p1", "x1")
            assertNotNull(type.name, token)

            assertEquals(type.name, 1, fx.calls.size)
            val cmd = fx.executed.single() as DeleteRecordCommand
            assertEquals(type, cmd.type)
            assertEquals("p1", cmd.patientId)
            assertEquals("x1", cmd.recordId)
            assertEquals(5L, cmd.expectedPatientRevision)
            fx.assertExecutedOnly(a)

            val offer = fx.state.undoOffer!!
            assertEquals(type, offer.type)
            assertEquals("p1", offer.patientId)
            assertEquals(UndoAction.DELETE, offer.action)
            assertEquals(UndoPhase.READY, offer.phase)
            assertEquals(labelOf(a), offer.label)
            assertTrue(type.name, !offer.toString().contains("SECRET"))
            assertTrue(type.name, fx.events.all { it is AncEvent.DeleteStarted || it is AncEvent.DeleteCommitted })
        }
    }

    @Test
    fun offerExpiresAfterExactly8000ms() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.time.advanceBy(7_999)
            assertEquals(type.name, token, fx.token)
            assertTrue(type.name, fx.expiredTokens().isEmpty())
            fx.time.advanceBy(1)
            assertNull(type.name, fx.state.undoOffer)
            assertEquals(type.name, listOf(token), fx.expiredTokens())

            fx.request(token)
            assertEquals(type.name, 1, fx.calls.size)
        }
    }

    @Test
    fun requestRestoresTheExactDeletedRecordAndStopsTheTimer() {
        for (type in types) {
            val fx = Fixture()
            val a = target(type)
            val token = fx.begin(type, "p1", "x1")!!
            fx.time.advanceBy(3_000)
            fx.request(token)

            assertEquals(type.name, 2, fx.calls.size)
            val delete = fx.executed[0] as DeleteRecordCommand
            val restore = fx.executed[1] as RestoreRecordCommand
            assertEquals(type, restore.type)
            assertEquals("p1", restore.patientId)
            assertEquals(original(a), restore.record)
            assertEquals(delete, restore.sourceDelete)
            assertEquals(6L, restore.expectedPatientRevision)
            assertNotEquals(delete.operationUuid, restore.operationUuid)
            fx.assertExecutedOnly(a)

            assertTrue(type.name, fx.events.any { it is AncEvent.UndoCommitted && it.token == token })
            assertNull(type.name, fx.state.undoOffer)
            fx.time.advanceBy(20_000)
            assertTrue(type.name, fx.expiredTokens().isEmpty())

            fx.request(token)
            assertEquals("a used token acts once", 2, fx.calls.size)
        }
    }

    @Test
    fun staleTokenRequestIsNoop() {
        for (type in types) {
            val fx = Fixture()
            fx.begin(type, "p1", "x1")
            val before = fx.state
            val eventsBefore = fx.events.size
            fx.request("stale")
            assertEquals(type.name, 1, fx.calls.size)
            assertEquals(type.name, before, fx.state)
            assertEquals(type.name, eventsBefore, fx.events.size)
        }
    }

    @Test
    fun firstFailureRestartsTheWindowOnce() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.time.advanceBy(3_000)
            fx.script += { ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_CONFLICT) }
            fx.request(token)

            assertEquals(type.name, UndoPhase.READY, fx.state.undoOffer?.phase)
            assertTrue(type.name, fx.events.any { it is AncEvent.UndoFailed && it.token == token })
            fx.time.advanceBy(7_999)
            assertEquals("fresh window of 8000 ms from the failure", token, fx.token)
            fx.time.advanceBy(1)
            assertNull(type.name, fx.state.undoOffer)
            assertEquals(type.name, listOf(token), fx.expiredTokens())
            fx.assertExecutedOnly(target(type))
        }
    }

    @Test
    fun secondFailureDropsTheOfferAndNothingRunsAfterwards() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.script += { ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_CONFLICT) }
            fx.request(token)
            fx.time.advanceBy(1_000)
            fx.script += { ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_CONFLICT) }
            fx.request(token)

            assertNull(type.name, fx.state.undoOffer)
            assertEquals(type.name, 2, fx.events.count { it is AncEvent.UndoFailed })
            assertEquals(type.name, listOf(token), fx.expiredTokens())
            val calls = fx.calls.size
            fx.time.advanceBy(30_000)
            fx.request(token)
            assertEquals(type.name, calls, fx.calls.size)
            assertEquals(type.name, listOf(token), fx.expiredTokens())
            fx.assertExecutedOnly(target(type))
        }
    }

    @Test
    fun ambiguousOutcomeReplaysTheSameCommandWithTheSameOperationUuid() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.script += { ClinicalCommandOutcome.Ambiguous }
            fx.request(token)
            assertEquals(type.name, UndoPhase.READY, fx.state.undoOffer?.phase)
            val first = fx.executed[1] as RestoreRecordCommand

            fx.bumpPatientRevision("p1", 42)
            fx.request(token)
            val replay = fx.executed[2] as RestoreRecordCommand
            assertEquals(type.name, first, replay)
            assertEquals(type.name, first.operationUuid, replay.operationUuid)
            assertEquals(type.name, first.expectedPatientRevision, replay.expectedPatientRevision)
            fx.assertExecutedOnly(target(type))
        }
    }

    @Test
    fun definiteFailureRebasesTheRetryOnTheCurrentPatientRevisionOnly() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.script += { ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_CONFLICT) }
            fx.request(token)
            val first = fx.executed[1] as RestoreRecordCommand

            fx.bumpPatientRevision("p1", 42)
            fx.request(token)
            val retry = fx.executed[2] as RestoreRecordCommand
            assertEquals(type.name, first.withPatientRevision(42), retry)
            fx.assertExecutedOnly(target(type))
        }
    }

    @Test
    fun stalePatientRevisionRetriesOnceAfterAFreshSnapshot() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.script += {
                fx.bumpPatientRevision("p1", 8)
                ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)
            }
            fx.request(token)

            assertEquals(type.name, 3, fx.calls.size)
            val first = fx.executed[1] as RestoreRecordCommand
            val retry = fx.executed[2] as RestoreRecordCommand
            assertEquals(type.name, 6L, first.expectedPatientRevision)
            assertEquals(type.name, first.withPatientRevision(8), retry)
            assertEquals(type.name, first.operationUuid, retry.operationUuid)
            assertTrue(type.name, fx.events.any { it is AncEvent.UndoCommitted && it.token == token })
            fx.assertExecutedOnly(target(type))
        }
    }

    @Test
    fun stalePatientRevisionIsRetriedAtMostOnce() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.script += {
                fx.bumpPatientRevision("p1", 8)
                ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)
            }
            fx.script += {
                fx.bumpPatientRevision("p1", 9)
                ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)
            }
            fx.request(token)

            assertEquals(type.name, 3, fx.calls.size)
            assertTrue(type.name, fx.events.any { it is AncEvent.UndoFailed && it.token == token })
            assertEquals(type.name, UndoPhase.READY, fx.state.undoOffer?.phase)
            fx.assertExecutedOnly(target(type))
        }
    }

    @Test
    fun stalePatientRevisionWithUnchangedSnapshotIsNotRetried() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.script += { ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION) }
            fx.request(token)
            assertEquals(type.name, 2, fx.calls.size)
            assertTrue(type.name, fx.events.any { it is AncEvent.UndoFailed })
        }
    }

    @Test
    fun newerOfferReplacesAndFinalisesTheOlder() {
        for (type in types) {
            val fx = Fixture()
            val first = fx.begin(type, "p1", "x1")!!
            fx.time.advanceBy(3_000)
            val second = fx.begin(type, "p1", "y1")!!
            assertNotEquals(type.name, first, second)
            assertEquals(type.name, second, fx.token)
            assertEquals(type.name, labelOf(target(type, "y1")), fx.state.undoOffer?.label)

            fx.request(first)
            assertEquals("older token no longer acts", 2, fx.calls.size)

            fx.time.advanceBy(7_999)
            assertEquals("the older timer must not end the newer offer", second, fx.token)
            fx.time.advanceBy(1)
            assertEquals(type.name, 2, fx.calls.size)
            assertNull(type.name, fx.state.undoOffer)
            assertTrue(type.name, second in fx.expiredTokens())
        }
    }

    @Test
    fun newerOfferIsTheOnlyOneRestoredAndNeverTheOlderRecord() {
        for (type in types) {
            val fx = Fixture()
            val first = fx.begin(type, "p1", "x1")!!
            val second = fx.begin(type, "p1", "y1")!!
            fx.request(first)
            fx.request(second)

            val restore = fx.executed.last() as RestoreRecordCommand
            assertEquals(type.name, "y1", restore.recordId)
            assertEquals(type.name, original(target(type, "y1")), restore.record)
            assertEquals(type.name, 3, fx.calls.size)
            fx.assertExecutedOnly(target(type), target(type, "y1"))
        }
    }

    @Test
    fun clearCancelsTimerDropsPendingCommandAndNothingRunsAfterwards() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.time.advanceBy(2_000)
            fx.coordinator.clear()
            val eventsBefore = fx.events.size

            fx.time.advanceBy(30_000)
            assertEquals("no timer event after clear()", eventsBefore, fx.events.size)
            fx.request(token)
            assertEquals("no command after clear()", 1, fx.calls.size)
            fx.assertExecutedOnly(target(type))
        }
    }

    @Test
    fun clearDuringAFailureWindowDropsTheRestartedTimerToo() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.script += { ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_CONFLICT) }
            fx.request(token)
            fx.coordinator.clear()
            val eventsBefore = fx.events.size

            fx.time.advanceBy(30_000)
            fx.request(token)
            assertEquals(type.name, eventsBefore, fx.events.size)
            assertEquals(type.name, 2, fx.calls.size)
            fx.assertExecutedOnly(target(type))
        }
    }

    @Test
    fun requestAfterTheSessionEpochChangedDoesNotExecute() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.changeEpoch(2L)
            fx.request(token)
            assertEquals(type.name, 1, fx.calls.size)

            val locked = Fixture()
            val lockedToken = locked.begin(type, "p1", "x1")!!
            locked.state = locked.state.copy(vaultState = VaultState.Locked)
            locked.request(lockedToken)
            assertEquals(type.name, 1, locked.calls.size)
        }
    }

    @Test
    fun sessionChangeWhileDeleteIsRunningArmsNoOfferAndStartsNoTimer() {
        for (type in types) {
            val fx = Fixture()
            fx.script += {
                fx.changeEpoch(2L)
                null
            }
            fx.coordinator.beginDelete(type, "p1", "x1")
            fx.time.runCurrent()

            assertNull(type.name, fx.state.undoOffer)
            assertTrue(type.name, fx.events.none { it is AncEvent.DeleteCommitted })
            fx.time.advanceBy(30_000)
            assertTrue(type.name, fx.events.all { it is AncEvent.DeleteStarted })
            assertEquals(type.name, 1, fx.calls.size)
        }
    }

    @Test
    fun sessionChangeWhileRestoreIsRunningCommitsNothingToTheNewSession() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.script += {
                fx.changeEpoch(2L)
                null
            }
            fx.request(token)
            assertTrue(type.name, fx.events.none { it is AncEvent.UndoCommitted })
            fx.time.advanceBy(30_000)
            assertTrue(type.name, fx.events.none { it is AncEvent.UndoCommitted })
        }
    }

    @Test
    fun failedOrAmbiguousDeleteDispatchesDeleteFailedAndOffersNothing() {
        for (type in types) {
            for (outcome in listOf(
                ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_REVISION),
                ClinicalCommandOutcome.Ambiguous,
                ClinicalCommandOutcome.Obsolete,
            )) {
                val fx = Fixture()
                fx.script += { outcome }
                fx.begin(type, "p1", "x1")
                assertNull(type.name, fx.state.undoOffer)
                assertEquals(type.name, listOf(AncEvent.DeleteStarted), fx.events.filterIsInstance<AncEvent.DeleteStarted>())
                assertTrue(type.name, fx.events.last() is AncEvent.DeleteFailed)
                fx.time.advanceBy(30_000)
                assertEquals(type.name, 2, fx.events.size)
            }
        }
    }

    @Test
    fun deleteOfARecordThatIsNotThereExecutesNothing() {
        for (type in types) {
            val fx = Fixture()
            fx.begin(type, "p1", "missing")
            fx.begin(type, "p3", "x1")
            // p2 has no "y1" in either section; p1's "onlyMed" is a medication only.
            fx.begin(type, "p2", "y1")
            if (type != DeletableType.MEDICATION) fx.begin(type, "p1", "onlyMed")
            assertTrue(type.name, fx.calls.isEmpty())
            assertNull(type.name, fx.state.undoOffer)
        }
    }

    @Test
    fun deleteOfOneKindNeverTouchesTheSameIdInTheOtherKindOrAnotherPatient() {
        for (type in types) {
            val fx = Fixture()
            val a = target(type)
            val token = fx.begin(type, "p1", "x1")!!
            fx.script += { ClinicalCommandOutcome.Ambiguous }
            fx.request(token)
            fx.request(token)
            fx.coordinator.clear()
            fx.time.advanceBy(30_000)

            fx.assertExecutedOnly(a)
            val others = types.filter { it != type }.map { target(it) } + target(type, patientId = "p2")
            for (cmd in fx.executed) {
                val key = when (cmd) {
                    is DeleteRecordCommand -> Target(cmd.type, cmd.patientId, cmd.recordId)
                    is RestoreRecordCommand -> Target(cmd.type, cmd.patientId, cmd.recordId)
                    else -> error("unexpected $cmd")
                }
                assertTrue("$key must never be executed", key !in others)
            }
        }
    }

    @Test
    fun saveUndoDeletesExactlyTheSavedRecordAndExpiresAfter8000ms() {
        for (type in types) {
            val a = target(type)
            val saved = original(a)!!
            val receipt = ClinicalCommandReceipt(
                operationUuid = "op", patientId = "p1", recordKind = "X", recordId = "x1",
                committedRevision = saved.revision, committedPatientRevision = 5, fingerprint = "fp",
            )

            val expiring = Fixture()
            expiring.coordinator.offerSaveUndo(type, "p1", "x1", receipt, saved)
            expiring.time.runCurrent()
            val offer = expiring.state.undoOffer!!
            assertEquals(type.name, UndoAction.SAVE, offer.action)
            assertEquals(type.name, type, offer.type)
            assertEquals(type.name, labelOf(a), offer.label)
            assertTrue(type.name, !offer.toString().contains("SECRET"))
            expiring.time.advanceBy(7_999)
            assertNotNull(type.name, expiring.state.undoOffer)
            expiring.time.advanceBy(1)
            assertNull(type.name, expiring.state.undoOffer)
            assertTrue(type.name, expiring.calls.isEmpty())

            val fx = Fixture()
            fx.coordinator.offerSaveUndo(type, "p1", "x1", receipt, saved)
            fx.time.runCurrent()
            fx.request(fx.token!!)
            val delete = fx.executed.single() as DeleteRecordCommand
            assertEquals(type.name, a, Target(delete.type, delete.patientId, delete.recordId))
            assertEquals(type.name, saved.revision, delete.expectedRecordRevision)
            assertEquals(type.name, contentHash(saved), delete.deletedContentHash)
            assertEquals(type.name, 5L, delete.expectedPatientRevision)
            fx.assertExecutedOnly(a)
        }
    }

    @Test
    fun undoThroughViewModelStyleWiringCompletesOnceWithoutRecursion() {
        for (type in types) {
            val fx = Fixture()
            var depth = 0
            var maxDepth = 0
            lateinit var coordinator: UndoCoordinator
            // Mirrors AncViewModel: the user's UndoRequested routes to the coordinator; its own events only reduce.
            fun userDispatch(event: AncEvent) {
                depth++
                maxDepth = maxOf(maxDepth, depth)
                check(depth < 20) { "recursion" }
                try {
                    if (event is AncEvent.UndoRequested) coordinator.request(event.token)
                    else fx.state = AncReducer.reduce(fx.state, event)
                } finally {
                    depth--
                }
            }
            coordinator = UndoCoordinator(
                scope = fx.scope,
                execute = { cmd, epoch ->
                    fx.calls += cmd to epoch
                    fx.script.removeFirstOrNull()?.invoke() ?: fx.commit(cmd)
                },
                reduce = { fx.state = AncReducer.reduce(fx.state, it) },
                currentState = { fx.state },
                snapshot = { RecordSnapshot(fx.state.patients) },
            )
            coordinator.beginDelete(type, "p1", "x1")
            fx.time.runCurrent()
            val newToken = fx.token!!
            val before = fx.calls.size
            userDispatch(AncEvent.UndoRequested(newToken))
            fx.time.runCurrent()

            assertEquals(type.name, 2, fx.calls.size)
            assertTrue(type.name, fx.calls.last().first is RestoreRecordCommand)
            assertEquals(type.name, 1, fx.calls.count { it.first is RestoreRecordCommand })
            assertNull(type.name, fx.state.undoOffer)
            assertEquals(type.name, 1, maxDepth)
        }
    }

    @Test
    fun saveUndoOfferKeepsTheSavedNotice() {
        for (type in types) {
            val fx = Fixture()
            val saved = original(target(type))!!
            fx.state = fx.state.copy(savedNotice = true)
            val receipt = ClinicalCommandReceipt(
                operationUuid = "op", patientId = "p1", recordKind = "X", recordId = "x1",
                committedRevision = saved.revision, committedPatientRevision = 5, fingerprint = "fp",
            )
            fx.coordinator.offerSaveUndo(type, "p1", "x1", receipt, saved)
            fx.time.runCurrent()
            assertNotNull(type.name, fx.state.undoOffer)
            assertTrue(type.name, fx.state.savedNotice)
        }
    }

    @Test
    fun staleRetryReadsTheStorageSnapshotNotTheUiState() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            // UI state stays at the old revision; only the storage snapshot moves.
            var storage = fx.state.patients
            fx.snapshotOverride = { RecordSnapshot(storage) }
            fx.script += {
                storage = storage.map { if (it.id == "p1") it.copy(revision = 8) else it }
                ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)
            }
            fx.request(token)
            assertEquals(type.name, 3, fx.calls.size)
            assertEquals(type.name, 8L, (fx.executed[2] as RestoreRecordCommand).expectedPatientRevision)
        }
    }

    @Test
    fun deleteFailureAfterSwitchingPatientIsStillReported() {
        for (type in types) {
            val fx = Fixture()
            fx.script += {
                fx.state = AncReducer.reduce(fx.state, AncEvent.OpenWorkspace("p2"))
                ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_CONFLICT)
            }
            fx.begin(type, "p1", "x1")
            val message = fx.state.deleteMessage!!
            assertEquals(type.name, DeleteMessageKind.DELETE_FAILED, message.kind)
            assertEquals(type.name, "p1", message.patientId)
            assertEquals(type.name, "Patient p1", message.patientName)
        }
    }

    @Test
    fun undoFailureAfterSwitchingPatientIsStillReported() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.script += {
                // Opening another patient drops the offer while the Undo runs.
                fx.state = AncReducer.reduce(fx.state, AncEvent.OpenWorkspace("p2"))
                ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_CONFLICT)
            }
            fx.request(token)
            assertNull(type.name, fx.state.undoOffer)
            val message = fx.state.deleteMessage!!
            assertEquals(type.name, DeleteMessageKind.UNDO_FAILED, message.kind)
            assertEquals(type.name, "p1", message.patientId)
            assertEquals(type.name, "Patient p1", message.patientName)
            assertEquals(type.name, UndoAction.DELETE, message.action)
        }
    }

    @Test
    fun undoRequestWhileADeleteRunsExecutesNothing() {
        for (type in types) {
            val fx = Fixture()
            val token = fx.begin(type, "p1", "x1")!!
            fx.state = AncReducer.reduce(fx.state, AncEvent.DeleteStarted)
            val calls = fx.calls.size
            fx.request(token)
            assertEquals(type.name, calls, fx.calls.size)
            assertEquals(type.name, UndoPhase.READY, fx.state.undoOffer?.phase)
        }
    }

    @Test
    fun offerForAPatientWithoutANameCarriesNoName() {
        val fx = Fixture()
        fx.state = fx.state.copy(patients = fx.state.patients.map { if (it.id == "p1") it.copy(displayName = "  ") else it })
        fx.coordinator.offerSaveUndo(
            DeletableType.MEDICATION, "p1", "x1",
            ClinicalCommandReceipt("op", "p1", "MEDICATION", "x1", 1, 6, "fp"),
            originals[0],
        )
        assertEquals("", fx.state.undoOffer!!.patientName)
    }
}
