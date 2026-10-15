package com.sherifradwan.ancnative

import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.PatientVocabulary
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DraftValues
import com.sherifradwan.ancnative.state.PatientDraftField
import com.sherifradwan.ancnative.state.PendingSaveToken
import com.sherifradwan.ancnative.state.PregnancyDraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.SaveUiError
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.VisitDraft
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.AuthenticationPurpose
import com.sherifradwan.ancnative.storage.VaultBlockReason
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import java.util.Locale
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AncWorkflowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchIsFailClosedAndNeverRendersSyntheticPatientsBeforeRealUnlock() {
        assertDemoBannerDisplayed()
        compose.onNodeWithTag("vaultGate").assertIsDisplayed()
        compose.onNodeWithText("Maya Example").assertDoesNotExist()
        compose.onNodeWithText("DEMO-ANC-001").assertDoesNotExist()
        compose.onNodeWithTag("patientSearch").assertDoesNotExist()
    }

    @Test
    fun recordsWindowKeepsSecureFlagAndGateExplainsDraftPolicy() {
        compose.activityRule.scenario.onActivity { activity ->
            val flags = activity.window.attributes.flags
            assertFalse(flags and WindowManager.LayoutParams.FLAG_SECURE == 0)
        }
        compose.onNodeWithText("Only completed saves persist", substring = true)
            .assertIsDisplayed()
            .assertTextContains("Unsaved drafts are discarded", substring = true)
        assertDemoBannerDisplayed()
    }

    @Test
    fun everyNewEnglishStorageStringHasAnArabicValue() {
        compose.activityRule.scenario.onActivity { activity ->
            val configuration = Configuration(activity.resources.configuration).apply {
                setLocale(Locale("ar"))
            }
            val arabic = activity.createConfigurationContext(configuration).resources
            val names = listOf(
                "secure_demo_storage",
                "initialize_demo_storage",
                "unlock_demo_storage",
                "credential_prompt_title",
                "unsaved_drafts_policy",
                "storage_uninitialized_title",
                "storage_locked_title",
                "storage_blocked_title",
                "save_pending",
                "save_reconciling",
                "save_unresolved",
                "save_storage_error",
            )
            names.forEach { name ->
                val identifier = arabic.getIdentifier(name, "string", activity.packageName)
                check(identifier != 0 && arabic.getString(identifier).isNotBlank()) { name }
            }
        }
    }

    @Test
    fun reviewedVaultLayoutsKeepActionsReachableAndPatientDataFailClosed() {
        val fixtures = listOf(
            VaultFixture(
                layout = LayoutFixture.narrowPhone(),
                state = VaultState.Uninitialized,
                actionTag = "initializeStorage",
                actionText = R.string.initialize_demo_storage,
            ),
            VaultFixture(
                layout = LayoutFixture.largeFont(),
                state = VaultState.Locked,
                actionTag = "unlockStorage",
                actionText = R.string.unlock_demo_storage,
            ),
            VaultFixture(
                layout = LayoutFixture.landscape(),
                state = VaultState.Blocked(VaultBlockReason.STORAGE_BUSY),
                actionTag = "retryStorage",
                actionText = R.string.retry_storage_check,
            ),
            VaultFixture(
                layout = LayoutFixture.tablet(),
                state = VaultState.Opening(AuthenticationPurpose.INITIALIZE),
                titleText = R.string.storage_initializing_title,
                bodyText = R.string.storage_initializing_body,
            ),
            VaultFixture(
                layout = LayoutFixture.darkTheme(),
                state = VaultState.Uninitialized,
                actionTag = "initializeStorage",
                actionText = R.string.initialize_demo_storage,
            ),
            VaultFixture(
                layout = LayoutFixture.arabicRtl(),
                state = VaultState.Locked,
                actionTag = "unlockStorage",
                actionText = R.string.unlock_demo_storage,
            ),
        )

        fixtures.forEach { fixture ->
            println("P2_LAYOUT_CASE=" + fixture.layout.name)
            val density = mountFixture(fixture.layout, guardedState(fixture.state))
            assertFullyWithinHost(
                compose.onAllNodesWithText(localizedString(fixture.layout, R.string.demo_banner))[0].assertIsDisplayed(),
                "${fixture.layout.name} synthetic banner",
                density,
            )

            val secureTitle = localizedString(fixture.layout, R.string.secure_demo_storage)
            assertFullyWithinHost(
                compose.onNodeWithText(secureTitle).performScrollTo().assertIsDisplayed(),
                "${fixture.layout.name} secure storage title",
                density,
            )

            fixture.actionTag?.let { tag ->
                val action = compose.onNodeWithTag(tag)
                    .performScrollTo()
                    .assertIsDisplayed()
                    .assertIsEnabled()
                assertFullyWithinHost(action, "${fixture.layout.name} $tag", density, minHeightDp = 48f)

                val actionLabel = localizedString(fixture.layout, checkNotNull(fixture.actionText))
                assertFullyWithinHost(
                    compose.onNodeWithText(actionLabel).assertIsDisplayed(),
                    "${fixture.layout.name} localized action label",
                    density,
                )
            }

            fixture.titleText?.let { titleResource ->
                val title = localizedString(fixture.layout, titleResource)
                assertFullyWithinHost(
                    compose.onNodeWithText(title).performScrollTo().assertIsDisplayed(),
                    "${fixture.layout.name} opening title",
                    density,
                )
            }
            fixture.bodyText?.let { bodyResource ->
                val body = localizedString(fixture.layout, bodyResource)
                assertFullyWithinHost(
                    compose.onNodeWithText(body).performScrollTo().assertIsDisplayed(),
                    "${fixture.layout.name} opening body",
                    density,
                )
            }

            compose.onNodeWithTag("patientSearch").assertDoesNotExist()
            compose.onNodeWithTag("visitEditor").assertDoesNotExist()
            compose.onNodeWithTag("visitDate").assertDoesNotExist()
            compose.onNodeWithText(TEST_PATIENT_IDENTITY).assertDoesNotExist()
            compose.onNodeWithText(TEST_DRAFT.notes).assertDoesNotExist()
        }
    }

    @Test
    fun reviewedVisitEditorLayoutsKeepStatusDraftAndControlsAccessible() {
        val fixtures = listOf(
            EditorFixture(LayoutFixture.narrowPhone(), SavePhase.PENDING, null, R.string.save_pending),
            EditorFixture(LayoutFixture.largeFont(), SavePhase.RECONCILING, null, R.string.save_reconciling),
            EditorFixture(
                LayoutFixture.landscape(),
                SavePhase.UNRESOLVED,
                SaveUiError.OUTCOME_UNRESOLVED,
                R.string.save_unresolved,
            ),
            EditorFixture(
                LayoutFixture.tablet(),
                SavePhase.ERROR,
                SaveUiError.CONFIRMED_NOT_SAVED,
                R.string.save_confirmed_not_saved,
            ),
            EditorFixture(LayoutFixture.darkTheme(), SavePhase.PENDING, null, R.string.save_pending),
            EditorFixture(
                LayoutFixture.arabicRtl(),
                SavePhase.ERROR,
                SaveUiError.CONFIRMED_NOT_SAVED,
                R.string.save_confirmed_not_saved,
            ),
        )

        fixtures.forEach { fixture ->
            println("P2_LAYOUT_CASE=" + fixture.layout.name)
            val density = mountFixture(
                fixture.layout,
                editorState(fixture.phase, fixture.error),
            )
            assertFullyWithinHost(
                compose.onAllNodesWithText(localizedString(fixture.layout, R.string.demo_banner))[0].assertIsDisplayed(),
                "${fixture.layout.name} synthetic banner",
                density,
            )

            val lock = compose.onNodeWithTag("lockStorage").assertIsDisplayed().assertIsEnabled()
            assertFullyWithinHost(lock, "${fixture.layout.name} lock", density, minHeightDp = 48f)

            assertFullyWithinHost(
                compose.onNodeWithTag("visitEditor").performScrollTo().assertIsDisplayed(),
                "${fixture.layout.name} editor heading",
                density,
            )
            assertFullyWithinHost(
                compose.onNodeWithText(TEST_PATIENT_IDENTITY).performScrollTo().assertIsDisplayed(),
                "${fixture.layout.name} patient identity",
                density,
            )

            val status = compose.onNodeWithTag("saveStatus").performScrollTo().assertIsDisplayed()
            assertFullyWithinHost(status, "${fixture.layout.name} save status", density)
            val statusText = localizedString(fixture.layout, fixture.statusText)
            assertFullyWithinHost(
                compose.onNodeWithText(statusText).assertIsDisplayed(),
                "${fixture.layout.name} localized save status",
                density,
            )

            val fields = listOf(
                "visitDate" to TEST_DRAFT.date,
                "visitBloodPressure" to TEST_DRAFT.bloodPressure,
                "visitWeight" to TEST_DRAFT.weight,
                "visitFindings" to TEST_DRAFT.findings,
                "visitNotes" to TEST_DRAFT.notes,
            )
            fields.forEach { (tag, suppliedValue) ->
                val field = compose.onNodeWithTag(tag)
                    .performScrollTo()
                    .assertIsDisplayed()
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.EditableText,
                            AnnotatedString(suppliedValue),
                        ),
                    )
                if (fixture.phase == SavePhase.ERROR) {
                    field.assertIsEnabled()
                } else {
                    field.assertIsNotEnabled()
                }
                assertFullyWithinHost(field, "${fixture.layout.name} $tag", density, minHeightDp = 48f)
            }

            val cancel = compose.onNodeWithTag("cancelVisit").performScrollTo().assertIsDisplayed()
            val save = compose.onNodeWithTag("saveVisit").performScrollTo().assertIsDisplayed()
            if (fixture.phase == SavePhase.ERROR) {
                cancel.assertIsEnabled()
                save.assertIsEnabled()
            } else {
                cancel.assertIsNotEnabled()
                save.assertIsNotEnabled()
            }
            assertFullyWithinHost(cancel, "${fixture.layout.name} cancel", density, minHeightDp = 48f)
            assertFullyWithinHost(save, "${fixture.layout.name} save", density, minHeightDp = 48f)
            assertFullyWithinHost(
                compose.onNodeWithTag("lockStorage").assertIsDisplayed(),
                "${fixture.layout.name} lock after editor scroll",
                density,
                minHeightDp = 48f,
            )
        }
    }

    @Test
    fun syntheticPatientCreateEditAndWorkspaceProfileUseOneAuthoritativeProjection() {
        val base = AncState(
            patients = DemoFixtures.patients,
            vaultState = VaultState.Unlocked(sessionEpoch = 301L),
        )
        var state = AncReducer.reduce(
            base,
            AncEvent.StartPatientEditor(
                patientId = null,
                newPatientId = P3_PATIENT_ID,
                editorSessionId = "p3-patient-create",
            ),
        )
        assertEquals(AncRoute.PatientEditor(P3_PATIENT_ID, isNew = true), state.route)
        state = AncReducer.reduce(state, AncEvent.PatientDraftChanged(PatientDraftField.CASE_ID, "P3-CASE-1"))
        state = AncReducer.reduce(
            state,
            AncEvent.PatientDraftChanged(PatientDraftField.DISPLAY_NAME, "P3 Synthetic Person"),
        )
        assertTrue(state.patientDraft?.isDirty == true)

        val created = DemoFixtures.patients.first().copy(
            id = P3_PATIENT_ID,
            caseId = "P3-CASE-1",
            displayName = "P3 Synthetic Person",
            dateOfBirth = null,
            phone = null,
            visits = emptyList(),
            createdAt = "2026-10-11T10:00:00Z",
            status = null,
            archivedAt = null,
            archivedReason = null,
            riskLevel = null,
            ageText = null,
            addressText = null,
            bloodGroupText = null,
            medicalHistory = null,
            surgicalHistory = null,
            familyHistory = null,
            allergyHistory = null,
            gravida = null,
            parity = null,
            abortion = null,
            obstetricNotes = null,
            revision = 1,
            previousPregnancies = emptyList(),
        )
        val createToken = PendingSaveToken(P3_CREATE_OPERATION, "p3-patient-create")
        state = AncReducer.reduce(state, AncEvent.SaveStarted(createToken))
        state = AncReducer.reduce(
            state,
            AncEvent.RecordCommitted(
                token = createToken,
                patients = base.patients + created,
                destination = AncRoute.Workspace(P3_PATIENT_ID),
                notice = SavedNotice.PATIENT,
            ),
        )
        assertEquals(created, state.patient(P3_PATIENT_ID))

        state = AncReducer.reduce(
            state,
            AncEvent.StartPatientEditor(P3_PATIENT_ID, P3_PATIENT_ID, "p3-patient-edit"),
        )
        assertEquals("P3-CASE-1", state.patientDraft?.values?.caseId)
        state = AncReducer.reduce(
            state,
            AncEvent.PatientDraftChanged(PatientDraftField.DISPLAY_NAME, "P3 Synthetic Person Corrected"),
        )
        val corrected = created.copy(displayName = "P3 Synthetic Person Corrected", revision = 2)
        val editToken = PendingSaveToken(P3_EDIT_OPERATION, "p3-patient-edit")
        state = AncReducer.reduce(state, AncEvent.SaveStarted(editToken))
        state = AncReducer.reduce(
            state,
            AncEvent.RecordCommitted(
                token = editToken,
                patients = state.patients.map { if (it.id == corrected.id) corrected else it },
                destination = AncRoute.Workspace(P3_PATIENT_ID),
                notice = SavedNotice.PATIENT,
            ),
        )
        assertEquals("P3 Synthetic Person Corrected", state.patient(P3_PATIENT_ID)?.displayName)

        val pregnancy = p3Pregnancy(patientId = corrected.id)
        val authoritative = corrected.copy(previousPregnancies = listOf(pregnancy), revision = 3)
        val parityState = state.copy(
            patients = state.patients.map { if (it.id == authoritative.id) authoritative else it },
            route = AncRoute.Workspace(authoritative.id, WorkspaceSection.OBSTETRIC_HISTORY),
        )
        mountFixture(LayoutFixture.narrowPhone(), parityState)
        compose.onAllNodesWithText("P3 Synthetic Person Corrected", substring = true)[0].assertIsDisplayed()
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasText("P3 parity detail"))
        compose.onNodeWithText("P3 parity detail").assertIsDisplayed()

        mountFixture(LayoutFixture.narrowPhone(), parityState.copy(route = AncRoute.Profile(authoritative.id)))
        compose.onNodeWithTag("profileList").performScrollToNode(hasText("P3 Synthetic Person Corrected"))
        compose.onNodeWithText("P3 Synthetic Person Corrected").assertIsDisplayed()
        compose.onNodeWithTag("profileList").performScrollToNode(hasText("P3 parity detail"))
        compose.onNodeWithText("P3 parity detail").assertIsDisplayed()
        val notRecordedText = localizedString(LayoutFixture.narrowPhone(), R.string.not_recorded)
        compose.onNodeWithTag("profileList").performScrollToNode(hasText(notRecordedText))
        compose.onAllNodesWithText(notRecordedText)[0].assertIsDisplayed()
    }

    @Test
    fun previousPregnancyCreateCorrectAndDirtyBackPreserveDraftBoundaries() {
        val patient = DemoFixtures.patients.first().copy(previousPregnancies = emptyList(), revision = 1)
        var state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 302L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.OBSTETRIC_HISTORY),
        )
        state = AncReducer.reduce(
            state,
            AncEvent.StartPregnancyEditor(
                patientId = patient.id,
                pregnancyId = null,
                newPregnancyId = P3_PREGNANCY_ID,
                editorSessionId = "p3-pregnancy-create",
            ),
        )
        state = AncReducer.reduce(
            state,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "LiveBirth"),
        )
        state = AncReducer.reduce(
            state,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.BIRTH_METHOD, "Vaginal"),
        )
        state = AncReducer.reduce(
            state,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.CLINICIAN_ID, "synthetic-clinician"),
        )
        val editorRoute = state.route
        val dirtyBack = AncReducer.reduce(state, AncEvent.Back)
        assertEquals(editorRoute, dirtyBack.route)
        assertTrue(dirtyBack.showDiscardDialog)
        assertTrue(dirtyBack.pregnancyDraft?.isDirty == true)
        state = AncReducer.reduce(dirtyBack, AncEvent.KeepEditing)

        val createdPregnancy = p3Pregnancy(patient.id)
        val createToken = PendingSaveToken(P3_PREGNANCY_CREATE_OPERATION, "p3-pregnancy-create")
        state = AncReducer.reduce(state, AncEvent.SaveStarted(createToken))
        state = AncReducer.reduce(
            state,
            AncEvent.RecordCommitted(
                token = createToken,
                patients = listOf(patient.copy(previousPregnancies = listOf(createdPregnancy), revision = 2)),
                destination = AncRoute.Workspace(patient.id, WorkspaceSection.OBSTETRIC_HISTORY),
                notice = SavedNotice.PREGNANCY,
            ),
        )
        state = AncReducer.reduce(
            state,
            AncEvent.StartPregnancyEditor(
                patientId = patient.id,
                pregnancyId = createdPregnancy.id,
                newPregnancyId = "unused",
                editorSessionId = "p3-pregnancy-correct",
            ),
        )
        assertEquals("LiveBirth", state.pregnancyDraft?.values?.outcome)
        assertEquals("Vaginal", state.pregnancyDraft?.values?.birthMethod)

        state = AncReducer.reduce(
            state,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "EarlyPregnancyLoss"),
        )
        val correctedDraft = state.pregnancyDraft!!.values
        assertEquals("", correctedDraft.birthMethod)
        assertEquals("", correctedDraft.terminationMethod)
        state = AncReducer.reduce(
            state,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.LOSS_MANAGEMENT, "Medical"),
        )
        val correctionBack = AncReducer.reduce(state, AncEvent.Back)
        assertTrue(correctionBack.showDiscardDialog)
        assertEquals(
            AncRoute.PreviousPregnancyEditor(patient.id, createdPregnancy.id),
            correctionBack.route,
        )
        state = AncReducer.reduce(correctionBack, AncEvent.KeepEditing)
        val correctedPregnancy = createdPregnancy.copy(
            outcome = "EarlyPregnancyLoss",
            birthMethod = null,
            lossManagement = "Medical",
            updatedAt = "2026-10-11T10:11:00Z",
            revision = 2,
        )
        val correctionToken = PendingSaveToken(
            P3_PREGNANCY_CORRECT_OPERATION,
            "p3-pregnancy-correct",
        )
        state = AncReducer.reduce(state, AncEvent.SaveStarted(correctionToken))
        state = AncReducer.reduce(
            state,
            AncEvent.RecordCommitted(
                token = correctionToken,
                patients = listOf(
                    patient.copy(
                        previousPregnancies = listOf(correctedPregnancy),
                        revision = 3,
                    ),
                ),
                destination = AncRoute.Workspace(patient.id, WorkspaceSection.OBSTETRIC_HISTORY),
                notice = SavedNotice.PREGNANCY,
            ),
        )
        val committedCorrection = state.patient(patient.id)?.previousPregnancies?.single()
        assertEquals("EarlyPregnancyLoss", committedCorrection?.outcome)
        assertEquals("Medical", committedCorrection?.lossManagement)
        assertEquals(null, committedCorrection?.birthMethod)
    }

    @Test
    fun datingAndUltrasoundWorkflowAndWorkspaceSectionsRenderAccurately() {
        val patient = DemoFixtures.patients.first()
        var state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 401L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.DATING),
        )

        // 1. Mount Workspace in DATING section
        mountFixture(LayoutFixture.narrowPhone(), state)
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasTestTag("editDating"))
        compose.onNodeWithTag("editDating").assertIsDisplayed()

        // 2. Open Dating Editor
        state = AncReducer.reduce(
            state,
            AncEvent.StartDatingEditor(patient.id, "dating-session-1"),
        )
        mountFixture(LayoutFixture.narrowPhone(), state)
        compose.onNodeWithTag("datingEditor").assertIsDisplayed()
        compose.onNodeWithTag("lmpDate").assertIsDisplayed()
        compose.onNodeWithTag("ultrasoundEdd").assertIsDisplayed()
        compose.onNodeWithTag("agreedEdd").assertIsDisplayed()
        compose.onNodeWithTag("datingNotes").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("saveDating").performScrollTo().assertIsDisplayed()

        // 3. Test Discard / KeepEditing flow on Dating Editor
        state = AncReducer.reduce(
            state,
            AncEvent.DatingDraftChanged(
                com.sherifradwan.ancnative.state.DatingDraftField.NOTES,
                "New dating draft note",
            ),
        )
        assertTrue(state.datingDraft?.isDirty == true)
        val discardDating = AncReducer.reduce(state, AncEvent.Back)
        assertTrue(discardDating.showDiscardDialog)
        state = AncReducer.reduce(discardDating, AncEvent.ConfirmDiscard)
        assertFalse(state.showDiscardDialog)
        assertFalse(state.hasDirtyDraft)

        // 4. Open Ultrasound Editor
        state = AncReducer.reduce(
            state,
            AncEvent.StartUltrasoundEditor(
                patientId = patient.id,
                scanId = null,
                newScanId = "us-scan-1",
                editorSessionId = "us-session-1",
            ),
        )
        mountFixture(LayoutFixture.narrowPhone(), state)
        compose.onNodeWithTag("ultrasoundEditor").assertIsDisplayed()
        compose.onNodeWithTag("scanDate").assertIsDisplayed()
        compose.onNodeWithTag("gaWeeks").assertIsDisplayed()
        compose.onNodeWithTag("gaDays").assertIsDisplayed()
        compose.onNodeWithTag("saveUltrasound").performScrollTo().assertIsDisplayed()

        // 5. Test Arabic RTL mounting for Ultrasound Editor
        mountFixture(LayoutFixture.arabicRtl(), state)
        compose.onNodeWithTag("ultrasoundEditor").performScrollTo().assertIsDisplayed()

        // 6. Mount Workspace in ULTRASOUND section
        val usWorkspaceState = state.copy(
            route = AncRoute.Workspace(patient.id, WorkspaceSection.ULTRASOUND),
            ultrasoundDraft = null,
        )
        mountFixture(LayoutFixture.narrowPhone(), usWorkspaceState)
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasTestTag("addUltrasound"))
        compose.onNodeWithTag("addUltrasound").assertIsDisplayed()
    }

    @Test
    fun labAndMedicationWorkflowAndWorkspaceSectionsRenderAccurately() {
        val patient = DemoFixtures.patients.first()
        var state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 501L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.LABORATORY),
        )

        // 1. Mount Workspace in LABORATORY section
        mountFixture(LayoutFixture.narrowPhone(), state)
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasTestTag("addLab"))
        compose.onNodeWithTag("addLab").assertIsDisplayed()

        // 2. Open Lab Editor
        state = AncReducer.reduce(
            state,
            AncEvent.StartLabEditor(
                patientId = patient.id,
                labId = null,
                newLabId = "lab-test-1",
                editorSessionId = "lab-session-1",
            ),
        )
        mountFixture(LayoutFixture.narrowPhone(), state)
        compose.onNodeWithTag("labEditor").assertIsDisplayed()
        compose.onNodeWithTag("labDate").assertIsDisplayed()
        compose.onNodeWithTag("labName").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("labResult").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("saveLab").performScrollTo().assertIsDisplayed()

        // 3. Test Discard / KeepEditing flow on Lab Editor
        state = AncReducer.reduce(
            state,
            AncEvent.LabDraftChanged(
                com.sherifradwan.ancnative.state.LabDraftField.NAME,
                "Hemoglobin",
            ),
        )
        assertTrue(state.labDraft?.isDirty == true)
        val discardLab = AncReducer.reduce(state, AncEvent.Back)
        assertTrue(discardLab.showDiscardDialog)
        state = AncReducer.reduce(discardLab, AncEvent.ConfirmDiscard)
        assertFalse(state.showDiscardDialog)
        assertFalse(state.hasDirtyDraft)

        // 4. Open Medication Editor
        state = AncReducer.reduce(
            state,
            AncEvent.StartMedicationEditor(
                patientId = patient.id,
                medicationId = null,
                newMedicationId = "med-test-1",
                editorSessionId = "med-session-1",
            ),
        )
        mountFixture(LayoutFixture.narrowPhone(), state)
        compose.onNodeWithTag("medicationEditor").assertIsDisplayed()
        compose.onNodeWithTag("medName").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("medDosage").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("saveMedication").performScrollTo().assertIsDisplayed()

        // 5. Test Arabic RTL mounting for Medication Editor
        mountFixture(LayoutFixture.arabicRtl(), state)
        compose.onNodeWithTag("medicationEditor").performScrollTo().assertIsDisplayed()

        // 6. Mount Workspace in MEDICATIONS section
        val medWorkspaceState = state.copy(
            route = AncRoute.Workspace(patient.id, WorkspaceSection.MEDICATIONS),
            medicationDraft = null,
        )
        mountFixture(LayoutFixture.narrowPhone(), medWorkspaceState)
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasTestTag("addMedication"))
        compose.onNodeWithTag("addMedication").assertIsDisplayed()
    }

    @Test
    fun clinicalSummaryAndDeliveryPlanningWorkflowRenderAccurately() {
        val patient = DemoFixtures.patients.first()
        val state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 601L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.EPISODE_OVERVIEW),
        )

        // 1. Mount Episode Overview
        mountFixture(LayoutFixture.narrowPhone(), state)
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasTestTag("overviewMilestones"))
        compose.onNodeWithTag("overviewMilestones").assertIsDisplayed()
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasTestTag("overviewQuickActions"))
        compose.onNodeWithTag("overviewQuickActions").assertIsDisplayed()
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasTestTag("overviewClinicalSummary"))
        compose.onNodeWithTag("overviewClinicalSummary").assertIsDisplayed()
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasTestTag("copyClinicalSummary"))
        compose.onNodeWithTag("copyClinicalSummary").assertIsDisplayed()

        // 2. Mount Delivery Section
        val deliveryState = state.copy(
            route = AncRoute.Workspace(patient.id, WorkspaceSection.DELIVERY),
        )
        mountFixture(LayoutFixture.narrowPhone(), deliveryState)
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasTestTag("deliveryPlanningCard"))
        compose.onNodeWithTag("deliveryPlanningCard").assertIsDisplayed()

        // 3. Mount Patient List and check patient list card
        val listState = state.copy(route = AncRoute.PatientList)
        mountFixture(LayoutFixture.narrowPhone(), listState)
        compose.onNodeWithTag("patient-${patient.caseId}").assertIsDisplayed()
        compose.onNodeWithTag("workspace-${patient.caseId}").assertIsDisplayed()
        compose.onNodeWithTag("profile-${patient.caseId}").assertIsDisplayed()
    }

    private fun mountFixture(layout: LayoutFixture, state: AncState): Float {
        var fixtureDensity = 0f
        compose.activityRule.scenario.onActivity { activity ->
            val viewport = activity.resources.configuration
            val fitScale = minOf(
                1f,
                viewport.screenWidthDp.toFloat() / layout.widthDp,
                viewport.screenHeightDp.toFloat() / layout.heightDp,
            ) * 0.9f
            fixtureDensity = activity.resources.displayMetrics.density * fitScale
            activity.setContent {
                // Fixtures model the visible content viewport, not a physical device configuration proof.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    contentAlignment = Alignment.TopStart,
                ) {
                    val configuration = fixtureConfiguration(activity, layout)
                    val configuredContext = activity.createConfigurationContext(configuration)
                    CompositionLocalProvider(
                        LocalContext provides configuredContext,
                        LocalConfiguration provides configuration,
                        LocalLayoutDirection provides layout.direction,
                        LocalDensity provides Density(fixtureDensity, layout.fontScale),
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                            Box(
                                modifier = Modifier
                                    .requiredSize(layout.widthDp.dp, layout.heightDp.dp)
                                    .clipToBounds()
                                    .testTag(FIXTURE_HOST_TAG),
                            ) {
                                AncTheme {
                                    AncApp(
                                        state = state,
                                        onEvent = {},
                                        onStartVisit = { _, _ -> },
                                        onSaveVisit = {},
                                        onInitialize = {},
                                        onUnlock = {},
                                        onLock = {},
                                        onRetryStorage = {},
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        return fixtureDensity
    }

    private fun fixtureConfiguration(activity: MainActivity, layout: LayoutFixture): Configuration =
        Configuration(activity.resources.configuration).apply {
            setLocale(layout.locale)
            setLayoutDirection(layout.locale)
            orientation = layout.orientation
            screenWidthDp = layout.widthDp
            screenHeightDp = layout.heightDp
            smallestScreenWidthDp = layout.smallestWidthDp
            fontScale = layout.fontScale
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (layout.darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }

    private fun localizedString(layout: LayoutFixture, @StringRes resource: Int): String {
        var value = ""
        compose.activityRule.scenario.onActivity { activity ->
            value = activity.createConfigurationContext(fixtureConfiguration(activity, layout)).getString(resource)
        }
        return value
    }

    private fun guardedState(vaultState: VaultState): AncState = AncState(
        patients = DemoFixtures.patients,
        vaultState = vaultState,
        route = AncRoute.VisitEditor(TEST_PATIENT_ID),
        draft = testDraft(),
    )

    private fun editorState(phase: SavePhase, error: SaveUiError?): AncState = AncState(
        patients = DemoFixtures.patients,
        vaultState = VaultState.Unlocked(sessionEpoch = 91L),
        route = AncRoute.VisitEditor(TEST_PATIENT_ID),
        draft = testDraft(),
        savePhase = phase,
        saveError = error,
        pendingSave = if (phase == SavePhase.ERROR) null else TEST_PENDING_SAVE,
    )

    private fun testDraft(): VisitDraft = VisitDraft(
        editorSessionId = TEST_PENDING_SAVE.editorSessionId,
        patientId = TEST_PATIENT_ID,
        visitId = null,
        baseRevision = null,
        original = TEST_DRAFT,
        values = TEST_DRAFT,
    )

    private fun p3Pregnancy(patientId: String) = DemoPreviousPregnancy(
        id = P3_PREGNANCY_ID,
        patientId = patientId,
        modelVersion = 3,
        dateText = null,
        outcomeText = null,
        pregnancyType = null,
        otherDetails = "P3 parity detail",
        placeAndDoctor = null,
        deliveryBy = null,
        outcome = "LiveBirth",
        birthMethod = "Vaginal",
        lossManagement = null,
        terminationMethod = null,
        provenanceKind = PatientVocabulary.CLINICIAN_ENTRY,
        clinicianId = "synthetic-clinician",
        recordedAt = "2026-10-11T10:10:00Z",
        updatedAt = "2026-10-11T10:10:00Z",
        revision = 1,
    )

    private fun assertFullyWithinHost(
        node: SemanticsNodeInteraction,
        label: String,
        fixtureDensity: Float,
        minHeightDp: Float = 1f,
    ) {
        fun rawBounds(semanticsNode: SemanticsNode): Rect {
            val nodePosition = semanticsNode.positionInRoot
            return Rect(
                left = nodePosition.x,
                top = nodePosition.y,
                right = nodePosition.x + semanticsNode.size.width,
                bottom = nodePosition.y + semanticsNode.size.height,
            )
        }

        val tolerancePx = 1f
        var semanticsNode = node.fetchSemanticsNode()
        var bounds = rawBounds(semanticsNode)
        var clippedBounds = semanticsNode.boundsInRoot
        var bottomClipPx = bounds.bottom - clippedBounds.bottom
        var topClipPx = clippedBounds.top - bounds.top
        var adjustments = 0

        while ((bottomClipPx > tolerancePx || topClipPx > tolerancePx) && adjustments < 5) {
            val deltaPx = if (bottomClipPx > tolerancePx) {
                bottomClipPx + 1f
            } else {
                -(topClipPx + 1f)
            }

            var scrollParent = semanticsNode.parent
            var scrollAction = scrollParent
                ?.config
                ?.getOrNull(SemanticsActions.ScrollBy)
                ?.action
            while (scrollParent != null && scrollAction == null) {
                scrollParent = scrollParent.parent
                scrollAction = scrollParent
                    ?.config
                    ?.getOrNull(SemanticsActions.ScrollBy)
                    ?.action
            }
            val actualScrollBy = scrollAction ?: break
            val scrolled = compose.runOnUiThread { actualScrollBy(0f, deltaPx) }

            compose.waitForIdle()
            semanticsNode = node.fetchSemanticsNode()
            bounds = rawBounds(semanticsNode)
            clippedBounds = semanticsNode.boundsInRoot
            bottomClipPx = bounds.bottom - clippedBounds.bottom
            topClipPx = clippedBounds.top - bounds.top
            adjustments += 1
            if (!scrolled) break
        }

        val hostNode = compose.onNodeWithTag(FIXTURE_HOST_TAG).fetchSemanticsNode()
        val hostPosition = hostNode.positionInRoot
        val host = Rect(
            left = hostPosition.x,
            top = hostPosition.y,
            right = hostPosition.x + hostNode.size.width,
            bottom = hostPosition.y + hostNode.size.height,
        )

        assertTrue("$label has no rendered width: $bounds", bounds.width > 0f)
        assertTrue("$label has no rendered height: $bounds", bounds.height > 0f)
        assertTrue("$label is outside host left: node=$bounds host=$host", bounds.left + tolerancePx >= host.left)
        assertTrue("$label is outside host top: node=$bounds host=$host", bounds.top + tolerancePx >= host.top)
        assertTrue("$label is outside host right: node=$bounds host=$host", bounds.right <= host.right + tolerancePx)
        assertTrue("$label is outside host bottom: node=$bounds host=$host", bounds.bottom <= host.bottom + tolerancePx)

        assertTrue(
            "$label is clipped left: unclipped=$bounds clipped=$clippedBounds",
            abs(clippedBounds.left - bounds.left) <= tolerancePx,
        )
        assertTrue(
            "$label is clipped top: unclipped=$bounds clipped=$clippedBounds",
            abs(clippedBounds.top - bounds.top) <= tolerancePx,
        )
        assertTrue(
            "$label is clipped right: unclipped=$bounds clipped=$clippedBounds",
            abs(clippedBounds.right - bounds.right) <= tolerancePx,
        )
        assertTrue(
            "$label is clipped bottom: unclipped=$bounds clipped=$clippedBounds",
            abs(clippedBounds.bottom - bounds.bottom) <= tolerancePx,
        )

        if (minHeightDp >= 48f) {
            val touchBounds = semanticsNode.touchBoundsInRoot
            assertTrue(
                "$label touch target is too short: height=${touchBounds.height}px, " +
                    "expected>=${minHeightDp * fixtureDensity}px",
                touchBounds.height + tolerancePx >= minHeightDp * fixtureDensity,
            )
            assertTrue(
                "$label touch target is outside host left: touch=$touchBounds host=$host",
                touchBounds.left + tolerancePx >= host.left,
            )
            assertTrue(
                "$label touch target is outside host top: touch=$touchBounds host=$host",
                touchBounds.top + tolerancePx >= host.top,
            )
            assertTrue(
                "$label touch target is outside host right: touch=$touchBounds host=$host",
                touchBounds.right <= host.right + tolerancePx,
            )
            assertTrue(
                "$label touch target is outside host bottom: touch=$touchBounds host=$host",
                touchBounds.bottom <= host.bottom + tolerancePx,
            )
        } else {
            assertTrue(
                "$label is too short: height=${bounds.height}px, expected>=${minHeightDp * fixtureDensity}px",
                bounds.height + tolerancePx >= minHeightDp * fixtureDensity,
            )
        }
    }

    private fun assertDemoBannerDisplayed() {
        compose.onAllNodesWithText(DEMO_BANNER)[0].assertIsDisplayed()
    }

    private data class LayoutFixture(
        val name: String,
        val widthDp: Int,
        val heightDp: Int,
        val fontScale: Float = 1f,
        val orientation: Int = Configuration.ORIENTATION_PORTRAIT,
        val smallestWidthDp: Int = minOf(widthDp, heightDp),
        val darkTheme: Boolean = false,
        val locale: Locale = Locale.US,
        val direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        companion object {
            fun narrowPhone() = LayoutFixture("narrow-phone", widthDp = 320, heightDp = 640)
            fun largeFont() = LayoutFixture("large-font-1.4", widthDp = 360, heightDp = 640, fontScale = 1.4f)
            fun landscape() = LayoutFixture(
                "landscape",
                widthDp = 640,
                heightDp = 320,
                orientation = Configuration.ORIENTATION_LANDSCAPE,
            )
            fun tablet() = LayoutFixture(
                "tablet",
                widthDp = 720,
                heightDp = 600,
                smallestWidthDp = 600,
            )
            fun darkTheme() = LayoutFixture("dark-theme", widthDp = 360, heightDp = 640, darkTheme = true)
            fun arabicRtl() = LayoutFixture(
                "arabic-rtl",
                widthDp = 360,
                heightDp = 640,
                locale = Locale("ar"),
                direction = LayoutDirection.Rtl,
            )
        }
    }

    private data class VaultFixture(
        val layout: LayoutFixture,
        val state: VaultState,
        val actionTag: String? = null,
        @StringRes val actionText: Int? = null,
        @StringRes val titleText: Int? = null,
        @StringRes val bodyText: Int? = null,
    )

    private data class EditorFixture(
        val layout: LayoutFixture,
        val phase: SavePhase,
        val error: SaveUiError?,
        @StringRes val statusText: Int,
    )

    private companion object {
        const val DEMO_BANNER = "SYNTHETIC DEMO DATA — NOT FOR CLINICAL USE"
        const val FIXTURE_HOST_TAG = "layoutFixtureHost"
        const val TEST_PATIENT_ID = "10000000-0000-4000-8000-000000000001"
        const val TEST_PATIENT_IDENTITY = "Maya Example · DEMO-ANC-001"
        const val P3_PATIENT_ID = "00000000-0000-4000-8000-000000000501"
        const val P3_PREGNANCY_ID = "00000000-0000-4000-8000-000000000502"
        const val P3_CREATE_OPERATION = "00000000-0000-4000-8000-000000000503"
        const val P3_EDIT_OPERATION = "00000000-0000-4000-8000-000000000504"
        const val P3_PREGNANCY_CREATE_OPERATION = "00000000-0000-4000-8000-000000000505"
        const val P3_PREGNANCY_CORRECT_OPERATION = "00000000-0000-4000-8000-000000000506"

        val TEST_DRAFT = DraftValues(
            date = "2031-02-03",
            bloodPressure = "123/78 fixture",
            weight = "71 kg fixture",
            findings = "Exact accessible layout finding",
            notes = "Exact accessible layout note",
        )
        val TEST_PENDING_SAVE = PendingSaveToken(
            operationUuid = "layout-only-operation",
            editorSessionId = "layout-only-session",
        )
    }
}
