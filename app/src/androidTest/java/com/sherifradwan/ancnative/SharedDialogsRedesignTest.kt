package com.sherifradwan.ancnative

import android.Manifest
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DatingDraft
import com.sherifradwan.ancnative.state.DatingDraftField
import com.sherifradwan.ancnative.state.DatingDraftValues
import com.sherifradwan.ancnative.state.DeleteMessage
import com.sherifradwan.ancnative.state.DeleteMessageKind
import com.sherifradwan.ancnative.state.DeliveryOutcomeField
import com.sherifradwan.ancnative.state.DraftRecoveryPrompt
import com.sherifradwan.ancnative.state.DraftSlot
import com.sherifradwan.ancnative.state.PendingDeletion
import com.sherifradwan.ancnative.state.PreviousPregnancyDraft
import com.sherifradwan.ancnative.state.PreviousPregnancyDraftValues
import com.sherifradwan.ancnative.state.SaveUiError
import com.sherifradwan.ancnative.state.UndoAction
import com.sherifradwan.ancnative.state.UndoOffer
import com.sherifradwan.ancnative.state.UndoPhase
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.storage.DraftEditorKind
import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.DraftScreenKind
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncDatePickerDialog
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.ClinicalDateField
import com.sherifradwan.ancnative.ui.DeleteConfirmDialog
import com.sherifradwan.ancnative.ui.InAppCameraDialog
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Redesign slice 19: Shell and shared dialogs test suite.
 * Tests DeleteConfirmDialog, DeleteMessageBar, UndoOfferBar, AncApp shell dialogs
 * (newPatientInfo, DiscardChangesDialog, draftRecoveryPrompt), AncDatePickerDialog,
 * ClinicalDateField (readOnly picker enforcement, Today, Clear buttons), and InAppCameraDialog.
 * Follows HistoryEditorsRedesignTest and RegistrationRedesignTest style templates:
 * - Single setContent per test
 * - Shared ComposeTextAsserts helpers
 * - Narrow 360dp x 640dp window
 * - fontScale overrides (1.0f, 1.3f, 2.0f)
 * - useUnmergedTree = true
 * - performScrollTo before assertIsDisplayed where needed
 * - Node selection by testTag
 * - >= 47.99dp touch tolerance
 * - Minimum >= 11.99sp font size
 */
@RunWith(AndroidJUnit4::class)
class SharedDialogsRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<AncEvent>()
    private var state by mutableStateOf(AncState(patients = emptyList()))

    private var primaryColor = Color.Unspecified
    private var errorColor = Color.Unspecified
    private var onErrorContainerColor = Color.Unspecified
    private var onSurfaceColor = Color.Unspecified
    private var onSurfaceVariantColor = Color.Unspecified

    private val basePatient = DemoFixtures.patients.first()

    private val medRecord = DemoMedicationRecord(
        id = "med-s19-1",
        patientId = basePatient.id,
        name = "Amoxicillin",
        dosage = "500 mg",
        route = "Oral",
        frequency = "Three times daily",
        startDate = "2026-11-01",
        endDate = null,
        status = MEDICATION_STATUS_ACTIVE,
        notes = null,
        isProphylaxis = false,
        revision = 1,
    )

    private val labRecord = DemoLabInvestigation(
        id = "lab-s19-1",
        patientId = basePatient.id,
        date = "2026-11-10",
        testCategory = "Hematology",
        testName = "Complete Blood Count",
        result = "12.5",
        unit = "g/dL",
        referenceRange = "11.0-15.0",
        isAbnormal = false,
        notes = null,
        revision = 1,
    )

    private val scanRecord = DemoUltrasoundScan(
        id = "scan-s19-1",
        patientId = basePatient.id,
        date = "2026-11-05",
        gestationalAgeWeeks = 20,
        gestationalAgeDays = 3,
        fetalNumber = "Singleton",
        presentation = "Cephalic",
        placentalSite = "Posterior",
        amnioticFluid = "Normal",
        fetalHeartRate = "142",
        biometryBpd = null,
        biometryHc = null,
        biometryAc = null,
        biometryFl = null,
        estimatedFetalWeight = null,
        impression = "Normal anatomy scan",
        linkedVisitId = null,
        imageUris = emptyList(),
        revision = 1,
    )

    private val visitRecord = DemoVisit(
        id = "visit-s19-1",
        patientId = basePatient.id,
        date = "2026-11-02",
        bloodPressure = "118/76",
        weight = "68 kg",
        findings = "Normal routine checkup",
        notes = null,
        linkedUltrasoundId = null,
        revision = 1,
    )

    private val prevPregnancyRecord = DemoPreviousPregnancy(
        id = "prev-s19-1",
        patientId = basePatient.id,
        modelVersion = 1,
        dateText = "2024-06-15",
        outcomeText = "Healthy boy",
        pregnancyType = "Singleton",
        otherDetails = null,
        placeAndDoctor = null,
        deliveryBy = "Vaginal",
        outcome = "LiveBirth",
        birthMethod = "Spontaneous",
        lossManagement = null,
        terminationMethod = null,
        provenanceKind = "CLINICAL",
        clinicianId = "dr_test",
        recordedAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
        revision = 1,
    )

    private val attachmentRecord = DemoAttachment(
        id = "att-s19-1",
        patientId = basePatient.id,
        fileName = "scan_report.webp",
        filePath = "patient_attachments/${basePatient.id}/scan_report.webp",
        category = "ULTRASOUND",
        fileSizeBytes = 2048,
        sha256Hash = "abc123hash",
        title = "Growth Scan Report",
        capturedAt = "2026-11-05",
        revision = 1,
    )

    private val fixturePatient = basePatient.copy(
        medications = listOf(medRecord),
        labInvestigations = listOf(labRecord),
        ultrasoundScans = listOf(scanRecord),
        visits = listOf(visitRecord),
        previousPregnancies = listOf(prevPregnancyRecord),
        attachments = listOf(attachmentRecord),
        status = "active",
    )

    @Before
    fun ensureCameraPermission() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = targetContext.packageName
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                uiAutomation.grantRuntimePermission(packageName, Manifest.permission.CAMERA)
            }
        } catch (_: Throwable) {
        }
        try {
            uiAutomation.executeShellCommand("pm grant $packageName android.permission.CAMERA")?.close()
        } catch (_: Throwable) {
        }
    }

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun onEvent(event: AncEvent) {
        events += event
        state = AncReducer.reduce(state, event)
    }

    private fun show(
        stateToHost: AncState,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        state = stateToHost
        events.clear()
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    primaryColor = MaterialTheme.colorScheme.primary
                    errorColor = MaterialTheme.colorScheme.error
                    onErrorContainerColor = MaterialTheme.colorScheme.onErrorContainer
                    onSurfaceColor = MaterialTheme.colorScheme.onSurface
                    onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            AncApp(
                                state = state,
                                onEvent = ::onEvent,
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
        compose.waitForIdle()
    }

    private fun showContent(
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        events.clear()
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    primaryColor = MaterialTheme.colorScheme.primary
                    errorColor = MaterialTheme.colorScheme.error
                    onErrorContainerColor = MaterialTheme.colorScheme.onErrorContainer
                    onSurfaceColor = MaterialTheme.colorScheme.onSurface
                    onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            content()
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun bounds(tag: String): DpRect = node(tag).getUnclippedBoundsInRoot()

    private fun assertMinTouchTarget(tag: String) {
        val b = bounds(tag)
        assertTrue("$tag width < 48dp (${b.width})", b.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${b.height})", b.height >= 47.99.dp)
    }

    private fun assertNoTextBelow12sp(tag: String) {
        val nodes = compose.onAllNodes(
            (SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) or
                SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)) and inTagged(tag),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue("no text found in $tag", nodes.isNotEmpty())
        nodes.forEach { n ->
            val layouts = mutableListOf<TextLayoutResult>()
            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            val text = n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
                ?: n.config.getOrNull(SemanticsProperties.EditableText)?.text
                ?: ""
            if (text.isBlank()) return@forEach
            layouts.forEach { layout ->
                val size = layout.layoutInput.style.fontSize
                assertTrue("'$text' is $size (< 12sp)", size.value >= 11.99f)
            }
        }
    }

    private fun assertTextColor(tag: String, expectedColor: Color) {
        val n = node(tag).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        if (layouts.isNotEmpty()) {
            assertEquals("Text color for $tag", expectedColor, layouts.first().layoutInput.style.color)
            return
        }
        val textChildren = compose.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and inTagged(tag),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue("No text descendant found in $tag", textChildren.isNotEmpty())
        val childLayouts = mutableListOf<TextLayoutResult>()
        textChildren.first().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(childLayouts)
        assertTrue("No text layout found in child of $tag", childLayouts.isNotEmpty())
        assertEquals("Text color in $tag child", expectedColor, childLayouts.first().layoutInput.style.color)
    }

    private fun createDraftPrompt(isStale: Boolean): DraftRecoveryPrompt {
        val record = DraftRecord(
            patientId = fixturePatient.id,
            screenKind = DraftScreenKind.VISIT,
            editorKind = DraftEditorKind.CREATE,
            recordId = "visit-draft-s19",
            baseRevision = null,
            fields = mapOf("findings" to "Routine visit notes"),
            updatedAtMillis = System.currentTimeMillis() - 2 * 60 * 60 * 1000L,
        )
        return DraftRecoveryPrompt(
            slot = DraftSlot(fixturePatient.id, DraftScreenKind.VISIT),
            editorSessionId = "session-draft-s19",
            record = record,
            isStale = isStale,
        )
    }

    // =========================================================================
    // 1. DeleteConfirmDialog
    // =========================================================================

    @Test
    fun deleteConfirmDialog_layout_strings_and_touchTargets() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            deleteConfirmation = PendingDeletion(
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                recordId = medRecord.id,
            ),
        )
        show(baseState)

        node("deleteConfirmDialog").assertIsDisplayed()
        val expectedTitle = str(R.string.delete_confirm_title, str(R.string.deletable_type_medication))
        compose.onNodeWithText(expectedTitle, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(medRecord.name, substring = true, useUnmergedTree = true).assertIsDisplayed()

        node("deleteConfirm").assertIsDisplayed()
        node("deleteCancel").assertIsDisplayed()

        assertMinTouchTarget("deleteConfirm")
        assertMinTouchTarget("deleteCancel")

        assertTextColor("deleteConfirm", errorColor)
        assertTextColor("deleteCancel", primaryColor)

        assertNoTextBelow12sp("deleteConfirmDialog")
    }

    @Test
    fun deleteConfirmDialog_confirm_dispatchesConfirmDelete() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            deleteConfirmation = PendingDeletion(
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                recordId = medRecord.id,
            ),
        )
        show(baseState)

        node("deleteConfirm").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.ConfirmDelete))
        assertNull(state.deleteConfirmation)
    }

    @Test
    fun deleteConfirmDialog_cancel_dispatchesCancelDelete() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            deleteConfirmation = PendingDeletion(
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                recordId = medRecord.id,
            ),
        )
        show(baseState)

        node("deleteCancel").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.CancelDelete))
        assertNull(state.deleteConfirmation)
    }

    @Test
    fun deleteConfirmDialog_allDeletableTypes_showCorrectTypeLabel() {
        val typeTestCases = listOf(
            DeletableType.LAB to (labRecord.id to R.string.deletable_type_lab),
            DeletableType.ULTRASOUND to (scanRecord.id to R.string.deletable_type_ultrasound),
            DeletableType.VISIT to (visitRecord.id to R.string.deletable_type_visit),
            DeletableType.PREVIOUS_PREGNANCY to (prevPregnancyRecord.id to R.string.deletable_type_previous_pregnancy),
            DeletableType.ATTACHMENT to (attachmentRecord.id to R.string.deletable_type_attachment),
        )

        var currentPendingDeletion by mutableStateOf(
            PendingDeletion(
                patientId = fixturePatient.id,
                type = DeletableType.LAB,
                recordId = labRecord.id,
            ),
        )
        var dispatchedEvent: AncEvent? = null

        showContent {
            DeleteConfirmDialog(
                state = AncState(
                    patients = listOf(fixturePatient),
                    vaultState = VaultState.Unlocked(sessionEpoch = 1L),
                    deleteConfirmation = currentPendingDeletion,
                ),
                onEvent = { dispatchedEvent = it },
            )
        }

        for ((type, idAndRes) in typeTestCases) {
            val (recordId, resId) = idAndRes
            dispatchedEvent = null
            currentPendingDeletion = PendingDeletion(
                patientId = fixturePatient.id,
                type = type,
                recordId = recordId,
            )
            compose.waitForIdle()
            node("deleteConfirmDialog").assertIsDisplayed()
            val expectedTitle = str(R.string.delete_confirm_title, str(resId))
            compose.onNodeWithText(expectedTitle, useUnmergedTree = true).assertIsDisplayed()
            node("deleteCancel").performClick()
            compose.waitForIdle()
            assertEquals(AncEvent.CancelDelete, dispatchedEvent)
        }
    }

    // =========================================================================
    // 2. DeleteMessageBar
    // =========================================================================

    @Test
    fun deleteMessageBar_layout_styling_and_touchTarget() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            deleteMessage = DeleteMessage(
                kind = DeleteMessageKind.DELETE_FAILED,
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                label = "Amoxicillin",
                error = SaveUiError.STORAGE_ERROR,
                patientName = fixturePatient.displayName,
            ),
        )
        show(baseState)

        node("deleteMessage").assertIsDisplayed()
        compose.onNodeWithText("Amoxicillin", substring = true, useUnmergedTree = true).assertIsDisplayed()

        node("deleteMessageDismiss").assertIsDisplayed()
        assertMinTouchTarget("deleteMessageDismiss")
        assertTextColor("deleteMessageDismiss", onErrorContainerColor)

        assertNoTextBelow12sp("deleteMessage")
    }

    @Test
    fun deleteMessageBar_dismiss_dispatchesDismissDeleteMessage() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            deleteMessage = DeleteMessage(
                kind = DeleteMessageKind.DELETE_FAILED,
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                label = "Amoxicillin",
                error = SaveUiError.STORAGE_ERROR,
                patientName = fixturePatient.displayName,
            ),
        )
        show(baseState)

        node("deleteMessageDismiss").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.DismissDeleteMessage))
        assertNull(state.deleteMessage)
        node("deleteMessage").assertDoesNotExist()
    }

    @Test
    fun deleteMessageBar_undoBlocked_showsCorrectString() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            deleteMessage = DeleteMessage(
                kind = DeleteMessageKind.UNDO_BLOCKED,
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                label = "Amoxicillin",
                patientName = fixturePatient.displayName,
            ),
        )
        show(baseState)

        node("deleteMessage").assertIsDisplayed()
        val expected = str(
            R.string.undo_blocked_message,
            str(R.string.deletable_type_medication),
            "Amoxicillin",
        )
        compose.onNodeWithText(expected, useUnmergedTree = true).assertIsDisplayed()
    }

    // =========================================================================
    // 3. UndoOfferBar
    // =========================================================================

    @Test
    fun undoOfferBar_layout_strings_and_touchTarget() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            undoOffer = UndoOffer(
                token = "undo-token-19",
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                action = UndoAction.DELETE,
                label = "Amoxicillin",
                patientName = fixturePatient.displayName,
                phase = UndoPhase.READY,
            ),
        )
        show(baseState)

        node("undoBar").assertIsDisplayed()
        node("undoButton").assertIsDisplayed().assertIsEnabled()
        assertMinTouchTarget("undoButton")

        compose.onNodeWithText(medRecord.name, substring = true, useUnmergedTree = true).assertIsDisplayed()
        assertNoTextBelow12sp("undoBar")
    }

    @Test
    fun undoOfferBar_clickUndo_dispatchesUndoRequested() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            undoOffer = UndoOffer(
                token = "undo-token-19",
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                action = UndoAction.DELETE,
                label = "Amoxicillin",
                patientName = fixturePatient.displayName,
                phase = UndoPhase.READY,
            ),
        )
        show(baseState)

        node("undoButton").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.UndoRequested("undo-token-19")))
    }

    @Test
    fun undoOfferBar_runningPhase_disablesUndoButton_showsProgress() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            undoOffer = UndoOffer(
                token = "undo-token-19",
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                action = UndoAction.DELETE,
                label = "Amoxicillin",
                patientName = fixturePatient.displayName,
                phase = UndoPhase.RUNNING,
            ),
        )
        show(baseState)

        node("undoButton").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithContentDescription(str(R.string.undo_in_progress), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun undoOfferBar_saveAction_rendersSavedString() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            undoOffer = UndoOffer(
                token = "undo-save-19",
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                action = UndoAction.SAVE,
                label = "Amoxicillin",
                patientName = "",
                phase = UndoPhase.READY,
            ),
        )
        show(baseState)

        node("undoBar").assertIsDisplayed()
        val expected = str(R.string.undo_message_saved_no_patient, "Amoxicillin")
        compose.onNodeWithText(expected, useUnmergedTree = true).assertIsDisplayed()
    }

    // =========================================================================
    // 4. AncApp Shell Dialogs
    // =========================================================================

    @Test
    fun newPatientInfoDialog_layout_touchTarget_and_dismiss() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            showNewPatientInfo = true,
        )
        show(baseState)

        node("newPatientInfoDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.new_patient_title), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.new_patient_body), useUnmergedTree = true).assertIsDisplayed()

        node("newPatientInfoClose").assertIsDisplayed()
        assertMinTouchTarget("newPatientInfoClose")
        assertNoTextBelow12sp("newPatientInfoDialog")

        node("newPatientInfoClose").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.HideNewPatientInfo))
        assertFalse(state.showNewPatientInfo)
        node("newPatientInfoDialog").assertDoesNotExist()
    }

    @Test
    fun shellDiscardDialog_layout_touchTargets_and_events() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            showDiscardDialog = true,
        )
        show(baseState)

        node("shellDiscardDialog").assertIsDisplayed()
        node("discardDialog").assertIsDisplayed()
        node("keepEditingDialog").assertIsDisplayed()

        assertMinTouchTarget("discardDialog")
        assertMinTouchTarget("keepEditingDialog")
        assertTextColor("discardDialog", errorColor)
        assertNoTextBelow12sp("shellDiscardDialog")

        node("keepEditingDialog").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.KeepEditing))
        assertFalse(state.showDiscardDialog)
    }

    @Test
    fun shellDiscardDialog_confirm_dispatchesConfirmDiscard() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            showDiscardDialog = true,
        )
        show(baseState)

        node("discardDialog").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.ConfirmDiscard))
    }

    @Test
    fun draftRecoveryDialog_freshDraft_showsContent_noStaleBanner() {
        val prompt = createDraftPrompt(isStale = false)
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            draftRecoveryPrompt = prompt,
        )
        show(baseState)

        node("draftRecoveryDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.draft_recovery_title), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.draft_screen_visit), substring = true, useUnmergedTree = true).assertIsDisplayed()
        node("draftRecoveryStaleBanner").assertDoesNotExist()

        node("draftContinue").assertIsDisplayed()
        node("draftDiscard").assertIsDisplayed()
        assertMinTouchTarget("draftContinue")
        assertMinTouchTarget("draftDiscard")
        assertTextColor("draftDiscard", errorColor)
        assertNoTextBelow12sp("draftRecoveryDialog")

        node("draftContinue").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.AcceptDraftRecovery))
    }

    @Test
    fun draftRecoveryDialog_staleDraft_showsStaleBanner_and_discard() {
        val prompt = createDraftPrompt(isStale = true)
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            draftRecoveryPrompt = prompt,
        )
        show(baseState)

        node("draftRecoveryDialog").assertIsDisplayed()
        node("draftRecoveryStaleBanner").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.draft_recovery_stale), useUnmergedTree = true).assertIsDisplayed()

        node("draftDiscard").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.DiscardDraftRecovery))
        assertNull(state.draftRecoveryPrompt)
        node("draftRecoveryDialog").assertDoesNotExist()
    }

    // =========================================================================
    // 5. AncDatePickerDialog
    // =========================================================================

    @Test
    fun ancDatePickerDialog_layout_noModeToggle_and_touchTargets() {
        var selectedDate: String? = null
        var dismissed = false

        showContent {
            AncDatePickerDialog(
                initialDateText = "2026-11-16",
                onDateSelected = { selectedDate = it },
                onDismiss = { dismissed = true },
                title = "Select visit date",
            )
        }

        node("ancDatePickerDialog").assertIsDisplayed()
        compose.onNodeWithText("Select visit date", useUnmergedTree = true).assertIsDisplayed()

        node("datePickerConfirm").assertIsDisplayed()
        node("datePickerDismiss").assertIsDisplayed()

        assertMinTouchTarget("datePickerConfirm")
        assertMinTouchTarget("datePickerDismiss")
        assertTextColor("datePickerDismiss", primaryColor)

        // showModeToggle = false enforces no manual keyboard toggle pencil
        compose.onNodeWithContentDescription("Switch to input text mode", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Switch to text input mode", useUnmergedTree = true).assertDoesNotExist()

        assertNoTextBelow12sp("ancDatePickerDialog")

        node("datePickerConfirm").performClick()
        compose.waitForIdle()

        assertEquals("2026-11-16", selectedDate)
        assertTrue(dismissed)
    }

    @Test
    fun ancDatePickerDialog_dismiss_callsOnDismiss() {
        var selectedDate: String? = null
        var dismissed = false

        showContent {
            AncDatePickerDialog(
                initialDateText = "2026-11-16",
                onDateSelected = { selectedDate = it },
                onDismiss = { dismissed = true },
            )
        }

        node("datePickerDismiss").performClick()
        compose.waitForIdle()

        assertNull(selectedDate)
        assertTrue(dismissed)
    }

    // =========================================================================
    // 6. ClinicalDateField
    // =========================================================================

    @Test
    fun clinicalDateField_tapField_or_calendarButton_opensDatePicker() {
        var dateValue by mutableStateOf("2026-11-16")

        showContent {
            Column(modifier = Modifier.padding(16.dp)) {
                ClinicalDateField(
                    value = dateValue,
                    onValueChange = { dateValue = it },
                    label = "Test visit date",
                    tag = "testVisitDate",
                    enabled = true,
                    externalLabel = true,
                    showTodayButton = true,
                    allowClear = true,
                )
            }
        }

        node("testVisitDate").assertIsDisplayed()
        node("testVisitDateCalendarButton").assertIsDisplayed()
        node("testVisitDateTodayButton").assertIsDisplayed()
        node("testVisitDateClearButton").assertIsDisplayed()

        assertMinTouchTarget("testVisitDateCalendarButton")
        assertMinTouchTarget("testVisitDateTodayButton")
        assertMinTouchTarget("testVisitDateClearButton")

        // Clicking calendar button opens date picker dialog
        node("testVisitDateCalendarButton").performClick()
        compose.waitForIdle()

        node("ancDatePickerDialog").assertIsDisplayed()
        node("datePickerDismiss").performClick()
        compose.waitForIdle()

        node("ancDatePickerDialog").assertDoesNotExist()

        // Clicking the readOnly text box itself also opens date picker dialog
        node("testVisitDate").performClick()
        compose.waitForIdle()

        node("ancDatePickerDialog").assertIsDisplayed()
    }

    @Test
    fun clinicalDateField_todayButton_setsCurrentDate() {
        var dateValue by mutableStateOf("")

        showContent {
            Column(modifier = Modifier.padding(16.dp)) {
                ClinicalDateField(
                    value = dateValue,
                    onValueChange = { dateValue = it },
                    label = "Test date",
                    tag = "testTodayField",
                    enabled = true,
                    showTodayButton = true,
                    allowClear = true,
                )
            }
        }

        node("testTodayFieldTodayButton").assertIsDisplayed()
        node("testTodayFieldTodayButton").performClick()
        compose.waitForIdle()

        assertEquals(LocalDate.now().toString(), dateValue)
    }

    @Test
    fun clinicalDateField_clearButton_clearsValue_and_hidesWhenEmpty() {
        var dateValue by mutableStateOf("2026-11-16")

        showContent {
            Column(modifier = Modifier.padding(16.dp)) {
                ClinicalDateField(
                    value = dateValue,
                    onValueChange = { dateValue = it },
                    label = "Test date",
                    tag = "testClearField",
                    enabled = true,
                    showTodayButton = false,
                    allowClear = true,
                )
            }
        }

        node("testClearFieldClearButton").assertIsDisplayed()
        node("testClearFieldClearButton").performClick()
        compose.waitForIdle()

        assertEquals("", dateValue)
        // Hidden when value is blank
        node("testClearFieldClearButton").assertDoesNotExist()
    }

    @Test
    fun clinicalDateField_errorState_and_helperText_styling() {
        showContent {
            Column(modifier = Modifier.padding(16.dp)) {
                ClinicalDateField(
                    value = "invalid-date",
                    onValueChange = {},
                    label = "Delivery date",
                    tag = "errorDateField",
                    enabled = true,
                    error = true,
                    errorMessage = "Enter the date as YYYY-MM-DD.",
                )
            }
        }

        compose.onNodeWithText("Enter the date as YYYY-MM-DD.", useUnmergedTree = true).assertIsDisplayed()
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun clinicalDateField_helperText_displayedWhenNoError() {
        showContent {
            Column(modifier = Modifier.padding(16.dp)) {
                ClinicalDateField(
                    value = "",
                    onValueChange = {},
                    label = "Delivery date",
                    tag = "helperDateField",
                    enabled = true,
                    error = false,
                    helperText = "Leave empty if the date is not known.",
                )
            }
        }

        compose.onNodeWithText("Leave empty if the date is not known.", useUnmergedTree = true).assertIsDisplayed()
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun clinicalDateField_disabled_hidesChips_and_disablesCalendar() {
        showContent {
            Column(modifier = Modifier.padding(16.dp)) {
                ClinicalDateField(
                    value = "2026-11-16",
                    onValueChange = {},
                    label = "Disabled date",
                    tag = "disabledDateField",
                    enabled = false,
                    showTodayButton = true,
                    allowClear = true,
                )
            }
        }

        node("disabledDateFieldTodayButton").assertDoesNotExist()
        node("disabledDateFieldClearButton").assertDoesNotExist()
        node("disabledDateFieldCalendarButton").assertIsNotEnabled()
    }

    // =========================================================================
    // 7. InAppCameraDialog
    // =========================================================================

    @Test
    fun inAppCameraDialog_closeButton_touchTarget_and_dismiss() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = targetContext.packageName
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                uiAutomation.grantRuntimePermission(packageName, Manifest.permission.CAMERA)
            }
        } catch (_: Throwable) {
        }
        try {
            uiAutomation.executeShellCommand("pm grant $packageName android.permission.CAMERA")?.close()
        } catch (_: Throwable) {
        }

        var dismissed = false

        showContent {
            InAppCameraDialog(
                onDismiss = { dismissed = true },
                onCaptured = {},
            )
        }

        node("camera_close").assertIsDisplayed()
        assertMinTouchTarget("camera_close")

        // If permission is denied/not granted in test environment, cameraPermissionCard or closeButton is shown
        val permCardExists = compose.onAllNodes(hasTestTag("cameraPermissionCard"), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        if (permCardExists) {
            compose.onNodeWithText(str(R.string.camera_permission_title), useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithText(str(R.string.camera_permission_needed), useUnmergedTree = true).assertIsDisplayed()
            assertNoTextBelow12sp("cameraPermissionCard")
        } else {
            node("camera_preview").assertIsDisplayed()
            node("camera_capture").assertIsDisplayed()
            assertMinTouchTarget("camera_capture")
            assertNoTextBelow12sp("camera_close")
        }

        node("camera_close").performClick()
        compose.waitForIdle()

        assertTrue(dismissed)
    }

    // =========================================================================
    // 8. Editor Integration (DatingEditor, PreviousPregnancyEditor, DeliveryPlanSheet)
    // =========================================================================

    @Test
    fun datingEditor_dateFields_haveClearButtons_and_dispatchChanges() {
        val datingDraft = DatingDraft(
            editorSessionId = "session-dating-s19",
            patientId = fixturePatient.id,
            basePatientRevision = fixturePatient.revision,
            original = DatingDraftValues(),
            values = DatingDraftValues(
                lmpDate = "2026-03-01",
                ultrasoundEdd = "2026-12-05",
            ),
        )
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.DatingEditor(fixturePatient.id),
            datingDraft = datingDraft,
        )
        show(baseState)

        node("lmpDate").performScrollTo().assertIsDisplayed()
        node("lmpDateCalendarButton").assertIsDisplayed()
        node("lmpDateTodayButton").assertIsDisplayed()
        node("lmpDateClearButton").assertIsDisplayed()

        assertMinTouchTarget("lmpDateClearButton")

        node("lmpDateClearButton").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.DatingDraftChanged(DatingDraftField.LMP_DATE, "")))
    }

    @Test
    fun previousPregnancyEditor_dateField_hasClearButton_and_dispatchesChanges() {
        val pregnancyDraft = PreviousPregnancyDraft(
            editorSessionId = "session-prev-s19",
            patientId = fixturePatient.id,
            pregnancyId = prevPregnancyRecord.id,
            basePatientRevision = fixturePatient.revision,
            basePregnancyRevision = null,
            recordedAt = null,
            original = PreviousPregnancyDraftValues(),
            values = PreviousPregnancyDraftValues(
                dateText = "2024-06-15",
            ),
        )
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.PreviousPregnancyEditor(fixturePatient.id, prevPregnancyRecord.id),
            pregnancyDraft = pregnancyDraft,
        )
        show(baseState)

        node("pregnancyDateText").performScrollTo().assertIsDisplayed()
        node("pregnancyDateTextClearButton").assertIsDisplayed()
        assertMinTouchTarget("pregnancyDateTextClearButton")

        node("pregnancyDateTextClearButton").performClick()
        compose.waitForIdle()

        assertEquals("", state.pregnancyDraft!!.values.dateText)
    }

    @Test
    fun deliveryPlanSheet_dateField_usesClinicalDateField_and_clearButton() {
        val planPatient = fixturePatient.copy(
            obstetricNotes = "Delivery at Cleopatra Hospital\nFees: 20000 EGP\nPostpartum: IUD at 6 weeks",
            status = "active",
            delivered = false,
        )
        val baseState = AncState(
            patients = listOf(planPatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.Workspace(planPatient.id, WorkspaceSection.DELIVERY),
        )
        show(baseState)

        compose.openHubTab(PatientHubTab.PLAN)
        node("editDeliveryPlan").performClick()
        compose.waitForIdle()

        node("deliveryPlanSheet").assertIsDisplayed()
        node("deliveryOutcomeSwitch").performScrollTo().performClick()
        compose.waitForIdle()

        node("deliveryOutcomeDateField").assertIsDisplayed()
        node("deliveryOutcomeDateFieldCalendarButton").assertIsDisplayed()
        node("deliveryOutcomeDateFieldTodayButton").assertIsDisplayed()

        // Today button populates today's date
        node("deliveryOutcomeDateFieldTodayButton").performClick()
        compose.waitForIdle()

        assertEquals(LocalDate.now().toString(), state.patientDraft!!.values.deliveryDate)

        // Clear button now appears
        node("deliveryOutcomeDateFieldClearButton").assertIsDisplayed()
        assertMinTouchTarget("deliveryOutcomeDateFieldClearButton")

        node("deliveryOutcomeDateFieldClearButton").performClick()
        compose.waitForIdle()

        assertEquals("", state.patientDraft!!.values.deliveryDate)
        node("deliveryOutcomeDateFieldClearButton").assertDoesNotExist()
    }

    // =========================================================================
    // 9. Responsive 360dp Narrow Viewport & fontScale (1.3f and 2.0f)
    // =========================================================================

    @Test
    fun undoOfferBar_360dp_fontScale2_0_wrapsText_noClipping() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            undoOffer = UndoOffer(
                token = "undo-responsive-19",
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                action = UndoAction.DELETE,
                label = "Long Medication Name Amoxicillin-Clavulanate 1000mg",
                patientName = fixturePatient.displayName,
                phase = UndoPhase.READY,
            ),
        )
        show(baseState, fontScale = 2.0f)

        node("undoBar").assertIsDisplayed()
        node("undoButton").assertIsDisplayed()
        assertMinTouchTarget("undoButton")

        // When fontScale > 1.3f, it uses spoken full text wrapping across up to 3 lines
        compose.onNodeWithText(fixturePatient.displayName, substring = true, useUnmergedTree = true).assertIsDisplayed()
        assertNoTextBelow12sp("undoBar")
    }

    @Test
    fun deleteConfirmDialog_360dp_fontScale1_3_noClipping() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            deleteConfirmation = PendingDeletion(
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                recordId = medRecord.id,
            ),
        )
        show(baseState, fontScale = 1.3f)

        node("deleteConfirmDialog").assertIsDisplayed()
        node("deleteConfirm").assertIsDisplayed()
        node("deleteCancel").assertIsDisplayed()
        assertMinTouchTarget("deleteConfirm")
        assertMinTouchTarget("deleteCancel")
        assertNoTextBelow12sp("deleteConfirmDialog")
    }

    @Test
    fun deleteConfirmDialog_360dp_fontScale2_0_noClipping() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            deleteConfirmation = PendingDeletion(
                patientId = fixturePatient.id,
                type = DeletableType.MEDICATION,
                recordId = medRecord.id,
            ),
        )
        show(baseState, fontScale = 2.0f)

        node("deleteConfirmDialog").assertIsDisplayed()
        node("deleteConfirm").assertIsDisplayed()
        node("deleteCancel").assertIsDisplayed()
        assertMinTouchTarget("deleteConfirm")
        assertMinTouchTarget("deleteCancel")
        assertNoTextBelow12sp("deleteConfirmDialog")
    }

    @Test
    fun draftRecoveryDialog_360dp_fontScale2_0_withStaleBanner_noClipping() {
        val prompt = createDraftPrompt(isStale = true)
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            draftRecoveryPrompt = prompt,
        )
        show(baseState, fontScale = 2.0f)

        node("draftRecoveryDialog").assertIsDisplayed()
        node("draftRecoveryStaleBanner").assertIsDisplayed()
        node("draftContinue").assertIsDisplayed()
        node("draftDiscard").assertIsDisplayed()
        assertMinTouchTarget("draftContinue")
        assertMinTouchTarget("draftDiscard")
        assertNoTextBelow12sp("draftRecoveryDialog")
    }

    @Test
    fun ancDatePickerDialog_360dp_fontScale2_0_noClipping() {
        showContent(fontScale = 2.0f) {
            AncDatePickerDialog(
                initialDateText = "2026-11-16",
                onDateSelected = {},
                onDismiss = {},
                title = "Select visit date",
            )
        }

        node("ancDatePickerDialog").assertIsDisplayed()
        node("datePickerConfirm").assertIsDisplayed()
        node("datePickerDismiss").assertIsDisplayed()
        assertMinTouchTarget("datePickerConfirm")
        assertMinTouchTarget("datePickerDismiss")
        assertNoTextBelow12sp("ancDatePickerDialog")
    }

    @Test
    fun newPatientInfoDialog_360dp_fontScale2_0_noClipping() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            showNewPatientInfo = true,
        )
        show(baseState, fontScale = 2.0f)

        node("newPatientInfoDialog").assertIsDisplayed()
        node("newPatientInfoClose").assertIsDisplayed()
        assertMinTouchTarget("newPatientInfoClose")
        assertNoTextBelow12sp("newPatientInfoDialog")
    }

    @Test
    fun shellDiscardDialog_360dp_fontScale2_0_noClipping() {
        val baseState = AncState(
            patients = listOf(fixturePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            showDiscardDialog = true,
        )
        show(baseState, fontScale = 2.0f)

        node("shellDiscardDialog").assertIsDisplayed()
        node("discardDialog").assertIsDisplayed()
        node("keepEditingDialog").assertIsDisplayed()
        assertMinTouchTarget("discardDialog")
        assertMinTouchTarget("keepEditingDialog")
        assertNoTextBelow12sp("shellDiscardDialog")
    }

    companion object {
        const val HOST_TAG = "sharedDialogsHost"
    }
}
