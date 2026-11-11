package com.sherifradwan.ancnative.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DraftAutosaver
import com.sherifradwan.ancnative.state.DraftSlot
import com.sherifradwan.ancnative.state.DraftStore
import com.sherifradwan.ancnative.state.DraftValues
import com.sherifradwan.ancnative.state.HistoriesDraft
import com.sherifradwan.ancnative.state.HistoriesDraftValues
import com.sherifradwan.ancnative.state.PatientDraft
import com.sherifradwan.ancnative.state.PatientDraftValues
import com.sherifradwan.ancnative.state.VisitDraft
import com.sherifradwan.ancnative.state.applyDraftRecord
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftPersistenceIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun autosavedVisitDraftSurvivesCloseAndReopenAndRestoresWithNewSessionId() = withReopenableDatabase { env ->
        runBlocking {
            val patient = DemoFixtures.patients.first()
            val initialValues = DraftValues(date = "2026-10-31", notes = "")
            val initialDraft = VisitDraft(
                editorSessionId = "initial-session",
                patientId = patient.id,
                visitId = null,
                baseRevision = null,
                original = initialValues,
                values = initialValues,
            )
            val stateBefore = AncState(
                patients = DemoFixtures.patients,
                vaultState = VaultState.Unlocked(sessionEpoch = 100L),
                route = AncRoute.VisitEditor(patient.id),
                draft = initialDraft,
            )
            val typedValues = initialValues.copy(
                notes = "Persistent encrypted visit note",
                bloodPressure = "120/80",
            )
            val stateAfter = stateBefore.copy(
                draft = initialDraft.copy(values = typedValues),
            )

            val autosaver = DraftAutosaver(
                scope = this,
                store = env.store,
                debounceMillis = 60_000L,
                nowMillis = { 1_700_000_000_000L },
            )
            autosaver.onStateChanged(stateBefore, stateAfter)
            autosaver.flush()

            env.closeAndReopen()

            val loaded = env.repository.loadDraft(patient.id, DraftScreenKind.VISIT)
            assertNotNull("Draft should be loaded after close and reopen", loaded)
            assertEquals("Persistent encrypted visit note", loaded?.fields?.get("notes"))
            assertEquals("120/80", loaded?.fields?.get("bloodPressure"))

            val cleanFreshValues = DraftValues(date = "2026-10-31", notes = "")
            val cleanFreshDraft = VisitDraft(
                editorSessionId = "fresh-session-123",
                patientId = patient.id,
                visitId = null,
                baseRevision = null,
                original = cleanFreshValues,
                values = cleanFreshValues,
            )
            val freshState = stateBefore.copy(draft = cleanFreshDraft)
            val restoredState = applyDraftRecord(freshState, checkNotNull(loaded))
            val restoredDraft = checkNotNull(restoredState.draft)

            assertEquals("Persistent encrypted visit note", restoredDraft.values.notes)
            assertEquals("120/80", restoredDraft.values.bloodPressure)
            assertNotEquals("initial-session", restoredDraft.editorSessionId)
            assertNotEquals("fresh-session-123", restoredDraft.editorSessionId)
            assertTrue(restoredDraft.editorSessionId.isNotBlank())
        }
    }

    @Test
    fun discardPathDeletesDraftThroughStoreAndReturnsNullAfterReopen() = withReopenableDatabase { env ->
        runBlocking {
            val patient = DemoFixtures.patients.first()
            val initialValues = DraftValues(date = "2026-10-31", notes = "")
            val dirtyValues = initialValues.copy(notes = "Draft to be discarded")
            val draft = VisitDraft(
                editorSessionId = "discard-session",
                patientId = patient.id,
                visitId = null,
                baseRevision = null,
                original = initialValues,
                values = dirtyValues,
            )
            val stateBefore = AncState(
                patients = DemoFixtures.patients,
                vaultState = VaultState.Unlocked(sessionEpoch = 100L),
                route = AncRoute.VisitEditor(patient.id),
                draft = initialValues.let {
                    draft.copy(values = it)
                },
            )
            val stateAfter = stateBefore.copy(draft = draft)

            val autosaver = DraftAutosaver(
                scope = this,
                store = env.store,
                debounceMillis = 60_000L,
                nowMillis = { 1_700_000_000_000L },
            )
            autosaver.onStateChanged(stateBefore, stateAfter)
            autosaver.flush()

            val storedBeforeDiscard = env.repository.loadDraft(patient.id, DraftScreenKind.VISIT)
            assertNotNull(storedBeforeDiscard)

            env.store.delete(DraftSlot(patient.id, DraftScreenKind.VISIT))

            env.closeAndReopen()

            val loadedAfterDiscard = env.repository.loadDraft(patient.id, DraftScreenKind.VISIT)
            assertNull("Discarded draft should be null after database reopen", loadedAfterDiscard)
        }
    }

    @Test
    fun newPatientDraftUnderNewPatientDraftKeySurvivesCloseAndReopen() = withReopenableDatabase { env ->
        runBlocking {
            val cleanValues = PatientDraftValues()
            val dirtyValues = cleanValues.copy(
                displayName = "Zahra New Patient",
                caseId = "NEW-ANC-999",
                phone = "+201011112222",
                ageText = "27",
            )
            val cleanDraft = PatientDraft(
                editorSessionId = "new-patient-session",
                patientId = NEW_PATIENT_DRAFT_KEY,
                baseRevision = null,
                createdAt = null,
                original = cleanValues,
                values = cleanValues,
            )
            val stateBefore = AncState(
                patients = DemoFixtures.patients,
                vaultState = VaultState.Unlocked(sessionEpoch = 100L),
                route = AncRoute.PatientEditor(NEW_PATIENT_DRAFT_KEY, isNew = true),
                patientDraft = cleanDraft,
            )
            val stateAfter = stateBefore.copy(
                patientDraft = cleanDraft.copy(values = dirtyValues),
            )

            val autosaver = DraftAutosaver(
                scope = this,
                store = env.store,
                debounceMillis = 60_000L,
                nowMillis = { 1_700_000_000_000L },
            )
            autosaver.onStateChanged(stateBefore, stateAfter)
            autosaver.flush()

            env.closeAndReopen()

            val loaded = env.repository.loadDraft(NEW_PATIENT_DRAFT_KEY, DraftScreenKind.PATIENT_PROFILE)
            assertNotNull("New patient draft should survive database reopen", loaded)
            assertEquals("Zahra New Patient", loaded?.fields?.get("displayName"))
            assertEquals("NEW-ANC-999", loaded?.fields?.get("caseId"))
            assertEquals("+201011112222", loaded?.fields?.get("phone"))
            assertEquals(DraftEditorKind.CREATE, loaded?.editorKind)
        }
    }

    @Test
    fun twoScreensOfOnePatientKeepSeparateDraftsAcrossReopen() = withReopenableDatabase { env ->
        runBlocking {
            val patient = DemoFixtures.patients.first()

            val visitClean = DraftValues(date = "2026-10-31", notes = "")
            val visitDirty = visitClean.copy(notes = "Visit note for patient 1")
            val visitDraft = VisitDraft(
                editorSessionId = "visit-session",
                patientId = patient.id,
                visitId = null,
                baseRevision = null,
                original = visitClean,
                values = visitDirty,
            )
            val visitStateBefore = AncState(
                patients = DemoFixtures.patients,
                vaultState = VaultState.Unlocked(sessionEpoch = 100L),
                route = AncRoute.VisitEditor(patient.id),
                draft = visitDraft.copy(values = visitClean),
            )
            val visitStateAfter = visitStateBefore.copy(draft = visitDraft)

            val autosaver = DraftAutosaver(
                scope = this,
                store = env.store,
                debounceMillis = 60_000L,
                nowMillis = { 1_700_000_000_000L },
            )
            autosaver.onStateChanged(visitStateBefore, visitStateAfter)
            autosaver.flush()

            val historiesClean = HistoriesDraftValues()
            val historiesDirty = historiesClean.copy(medicalHistory = "Asthma diagnosed 2021")
            val historiesDraft = HistoriesDraft(
                editorSessionId = "histories-session",
                patientId = patient.id,
                baseRevision = 1L,
                original = historiesClean,
                values = historiesDirty,
            )
            val historiesStateBefore = AncState(
                patients = DemoFixtures.patients,
                vaultState = VaultState.Unlocked(sessionEpoch = 100L),
                route = AncRoute.OtherHistoriesEditor(patient.id),
                historiesDraft = historiesDraft.copy(values = historiesClean),
            )
            val historiesStateAfter = historiesStateBefore.copy(historiesDraft = historiesDraft)

            autosaver.onStateChanged(historiesStateBefore, historiesStateAfter)
            autosaver.flush()

            env.closeAndReopen()

            val loadedVisit = env.repository.loadDraft(patient.id, DraftScreenKind.VISIT)
            val loadedHistories = env.repository.loadDraft(patient.id, DraftScreenKind.HISTORIES)
            val allForPatient = env.repository.loadDrafts(patient.id)

            assertNotNull("Visit draft should survive reopen", loadedVisit)
            assertNotNull("Histories draft should survive reopen", loadedHistories)
            assertEquals("Visit note for patient 1", loadedVisit?.fields?.get("notes"))
            assertEquals("Asthma diagnosed 2021", loadedHistories?.fields?.get("medicalHistory"))
            assertEquals(2, allForPatient.size)
            assertEquals(setOf(DraftScreenKind.VISIT, DraftScreenKind.HISTORIES), allForPatient.map { it.screenKind }.toSet())
        }
    }

    private class DatabaseTestEnv(
        val vaultUuid: String,
        private var openedDatabase: OpenedRecordsDatabase,
        private val reopenFn: () -> OpenedRecordsDatabase,
    ) {
        var repository: RecordsRepository = RecordsRepository(openedDatabase.database, vaultUuid)
            private set

        val store = object : DraftStore {
            override suspend fun save(record: DraftRecord): Boolean = repository.saveDraft(record)
            override suspend fun delete(slot: DraftSlot) = repository.deleteDraft(slot.patientId, slot.kind)
            override suspend fun load(slot: DraftSlot): DraftRecord? = repository.loadDraft(slot.patientId, slot.kind)
        }

        fun closeAndReopen() {
            openedDatabase = reopenFn()
            repository = RecordsRepository(openedDatabase.database, vaultUuid)
        }
    }

    private fun withReopenableDatabase(block: (DatabaseTestEnv) -> Unit) {
        val namespace = UUID.randomUUID().toString()
        val files = VaultFiles.isolatedTest(context, namespace)
        val vaultUuid = UUID.randomUUID().toString()
        files.prepareDescriptor(VaultDescriptor(vaultUuid, UUID.randomUUID().toString()))
        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        val dbFile = files.databaseFile(vaultUuid)
        var currentOpened: OpenedRecordsDatabase? = RecordsDatabase.open(context, dbFile.absolutePath, passphrase)
        try {
            val env = DatabaseTestEnv(
                vaultUuid = vaultUuid,
                openedDatabase = checkNotNull(currentOpened),
                reopenFn = {
                    currentOpened?.close()
                    val next = RecordsDatabase.open(context, dbFile.absolutePath, passphrase)
                    currentOpened = next
                    next
                },
            )
            block(env)
        } finally {
            try {
                currentOpened?.close()
            } catch (_: Throwable) {
            }
            passphrase.fill(0)
            files.cleanupRegisteredTestNamespace()
        }
    }
}
