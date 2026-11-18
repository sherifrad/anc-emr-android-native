package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for Slice S4 picture selection before print:
 * - Selection to figuresFor flow
 * - Sequential 1-based numbering in candidate date order
 * - Dynamic toggling, select all, and clear
 * - figureRefsFor linking to scans and visits
 * - Key stability across repeated calls and patient reconstructions
 *
 * Note on size-guard arithmetic:
 * The 24 MB total size guard arithmetic is implemented directly inside [encodePicturesForPrint]
 * in ReportPictureEncoder.kt, which is an Android-dependent suspend function (requiring
 * android.content.Context, android.graphics.Bitmap, and android.util.Base64). It is not extracted
 * into a pure JVM function, and therefore cannot be unit-tested without Android runtime classes.
 */
class ReportPictureSelectionTest {

    private fun samplePatient(
        scans: List<DemoUltrasoundScan> = emptyList(),
        visits: List<DemoVisit> = emptyList(),
        attachments: List<DemoAttachment> = emptyList(),
    ): DemoPatient = DemoPatient(
        id = "patient-s4",
        caseId = "CASE-S4",
        displayName = "Nourhan Tarek",
        dateOfBirth = "1997-08-20",
        phone = "01000000000",
        visits = visits,
        ultrasoundScans = scans,
        attachments = attachments,
    )

    private fun sampleScan(
        id: String,
        date: String,
        imageUris: List<String> = emptyList(),
        linkedVisitId: String? = null,
    ): DemoUltrasoundScan = DemoUltrasoundScan(
        id = id,
        patientId = "patient-s4",
        date = date,
        gestationalAgeWeeks = 20,
        gestationalAgeDays = 0,
        fetalNumber = "Singleton",
        presentation = "Cephalic",
        placentalSite = "Posterior",
        amnioticFluid = "Normal",
        fetalHeartRate = "140",
        biometryBpd = "48",
        biometryHc = "175",
        biometryAc = "160",
        biometryFl = "35",
        estimatedFetalWeight = "390",
        impression = "Normal anatomy",
        imageUris = imageUris,
        linkedVisitId = linkedVisitId,
    )

    private fun sampleVisit(
        id: String,
        date: String,
    ): DemoVisit = DemoVisit(
        id = id,
        patientId = "patient-s4",
        date = date,
        bloodPressure = "118/76",
        weight = "66.0",
        findings = "Normal visit",
        notes = null,
    )

    private fun sampleAttachment(
        id: String,
        capturedAt: String,
        filePath: String,
        title: String? = null,
        linkedRecordId: String? = null,
        fileName: String = "attachment.jpg",
    ): DemoAttachment = DemoAttachment(
        id = id,
        patientId = "patient-s4",
        fileName = fileName,
        filePath = filePath,
        category = "CLINICAL",
        fileSizeBytes = 1024L,
        sha256Hash = "hash-$id",
        title = title,
        capturedAt = capturedAt,
        linkedRecordId = linkedRecordId,
    )

    // =========================================================================
    // 1. Selection to figuresFor flow
    // =========================================================================

    @Test
    fun selectionToFiguresForFlow_emptyByDefault_returnsNoFigures() {
        val c1 = PictureCandidate(key = "scan:s-1:0", sourceLabel = "Ultrasound 10/01/2026", date = "10/01/2026", linkedRecordId = "s-1", location = "s1.jpg")
        val c2 = PictureCandidate(key = "att:a-1", sourceLabel = "Visit #1", date = "15/02/2026", linkedRecordId = "v-1", location = "v1.jpg")
        val candidates = listOf(c1, c2)

        val figures = figuresFor(candidates, emptySet())
        assertTrue("Empty selection must produce empty figures list", figures.isEmpty())
    }

    @Test
    fun selectionToFiguresForFlow_selectSubset_assignsSequential1BasedNumbersInCandidateOrder() {
        val c1 = PictureCandidate(key = "scan:s-1:0", sourceLabel = "Ultrasound 10/01/2026", date = "10/01/2026", linkedRecordId = "s-1", location = "s1.jpg")
        val c2 = PictureCandidate(key = "att:a-1", sourceLabel = "Visit #1", date = "15/02/2026", linkedRecordId = "v-1", location = "v1.jpg")
        val c3 = PictureCandidate(key = "scan:s-2:0", sourceLabel = "Ultrasound 20/03/2026", date = "20/03/2026", linkedRecordId = "s-2", location = "s2.jpg")
        val c4 = PictureCandidate(key = "att:a-2", sourceLabel = "Patient profile", date = "25/03/2026", linkedRecordId = null, location = "p1.jpg")
        val candidates = listOf(c1, c2, c3, c4)

        // Select c3 and c1 in reverse order
        val selected = setOf(c3.key, c1.key)
        val figures = figuresFor(candidates, selected)

        assertEquals("Should only contain 2 selected figures", 2, figures.size)
        assertEquals("Figure 1 must be assigned to first selected candidate in candidate order", 1, figures[0].number)
        assertEquals("scan:s-1:0", figures[0].candidate.key)
        assertEquals("Figure 2 must be assigned to second selected candidate in candidate order", 2, figures[1].number)
        assertEquals("scan:s-2:0", figures[1].candidate.key)
    }

    @Test
    fun selectionToFiguresForFlow_selectAll_assignsSequentialNumbersToAllCandidates() {
        val c1 = PictureCandidate(key = "scan:s-1:0", sourceLabel = "Ultrasound 10/01/2026", date = "10/01/2026", linkedRecordId = "s-1", location = "s1.jpg")
        val c2 = PictureCandidate(key = "att:a-1", sourceLabel = "Visit #1", date = "15/02/2026", linkedRecordId = "v-1", location = "v1.jpg")
        val c3 = PictureCandidate(key = "scan:s-2:0", sourceLabel = "Ultrasound 20/03/2026", date = "20/03/2026", linkedRecordId = "s-2", location = "s2.jpg")
        val candidates = listOf(c1, c2, c3)

        val allKeys = candidates.map { it.key }.toSet()
        val figures = figuresFor(candidates, allKeys)

        assertEquals(3, figures.size)
        assertEquals(listOf(1, 2, 3), figures.map { it.number })
        assertEquals(listOf("scan:s-1:0", "att:a-1", "scan:s-2:0"), figures.map { it.candidate.key })
    }

    @Test
    fun selectionToFiguresForFlow_clear_returnsEmptyFigures() {
        val c1 = PictureCandidate(key = "scan:s-1:0", sourceLabel = "Ultrasound 10/01/2026", date = "10/01/2026", linkedRecordId = "s-1", location = "s1.jpg")
        val candidates = listOf(c1)

        val beforeClear = figuresFor(candidates, setOf(c1.key))
        assertEquals(1, beforeClear.size)

        val afterClear = figuresFor(candidates, emptySet())
        assertTrue("Clearing selection returns empty list", afterClear.isEmpty())
    }

    @Test
    fun selectionToFiguresForFlow_toggleItem_dynamicallyUpdatesFiguresAndNumbering() {
        val c1 = PictureCandidate(key = "c-1", sourceLabel = "Pic 1", date = "01/01/2026", linkedRecordId = "r-1", location = "1.jpg")
        val c2 = PictureCandidate(key = "c-2", sourceLabel = "Pic 2", date = "02/01/2026", linkedRecordId = "r-2", location = "2.jpg")
        val c3 = PictureCandidate(key = "c-3", sourceLabel = "Pic 3", date = "03/01/2026", linkedRecordId = "r-3", location = "3.jpg")
        val candidates = listOf(c1, c2, c3)

        var selection = emptySet<String>()
        assertTrue(figuresFor(candidates, selection).isEmpty())

        // Toggle c2 ON
        selection = selection + c2.key
        var figures = figuresFor(candidates, selection)
        assertEquals(listOf(1 to "c-2"), figures.map { it.number to it.candidate.key })

        // Toggle c1 ON (appears before c2)
        selection = selection + c1.key
        figures = figuresFor(candidates, selection)
        assertEquals(listOf(1 to "c-1", 2 to "c-2"), figures.map { it.number to it.candidate.key })

        // Toggle c3 ON
        selection = selection + c3.key
        figures = figuresFor(candidates, selection)
        assertEquals(listOf(1 to "c-1", 2 to "c-2", 3 to "c-3"), figures.map { it.number to it.candidate.key })

        // Toggle c2 OFF (c3 shifts from Fig 3 to Fig 2)
        selection = selection - c2.key
        figures = figuresFor(candidates, selection)
        assertEquals(listOf(1 to "c-1", 2 to "c-3"), figures.map { it.number to it.candidate.key })

        // Toggle all remaining OFF
        selection = emptySet()
        assertTrue(figuresFor(candidates, selection).isEmpty())
    }

    @Test
    fun selectionToFiguresForFlow_unknownKeysIgnoredSafely() {
        val c1 = PictureCandidate(key = "scan:s-1:0", sourceLabel = "Scan", date = "01/01/2026", linkedRecordId = "s-1", location = "s.jpg")
        val candidates = listOf(c1)

        val figures = figuresFor(candidates, setOf("bogus-1", "scan:s-1:0", "deleted-attachment:99"))
        assertEquals(1, figures.size)
        assertEquals(1, figures[0].number)
        assertEquals("scan:s-1:0", figures[0].candidate.key)
    }

    @Test
    fun selectionToFiguresForFlow_figureRefsFor_linksScansAndVisits() {
        val c1 = PictureCandidate(key = "scan:s-1:0", sourceLabel = "Scan 1", date = "01/01/2026", linkedRecordId = "s-1", location = "s1.jpg")
        val c2 = PictureCandidate(key = "att:a-1", sourceLabel = "Visit #1", date = "10/01/2026", linkedRecordId = "v-1", location = "v1.jpg")
        val c3 = PictureCandidate(key = "scan:s-1:1", sourceLabel = "Scan 1 second photo", date = "01/01/2026", linkedRecordId = "s-1", location = "s1b.jpg")
        val c4 = PictureCandidate(key = "att:a-2", sourceLabel = "Patient profile", date = "15/01/2026", linkedRecordId = null, location = "prof.jpg")
        val candidates = listOf(c1, c2, c3, c4)

        // Select all
        val figures = figuresFor(candidates, candidates.map { it.key }.toSet())
        assertEquals(4, figures.size)

        // s-1 has c1 (Fig 1) and c3 (Fig 3)
        assertEquals(listOf(1, 3), figureRefsFor("s-1", figures))

        // v-1 has c2 (Fig 2)
        assertEquals(listOf(2), figureRefsFor("v-1", figures))

        // Unlinked profile attachment has no record id
        assertEquals(emptyList<Int>(), figureRefsFor("v-unknown", figures))
        assertEquals(emptyList<Int>(), figureRefsFor("", figures))
    }

    // =========================================================================
    // 2. Key stability across calls and patient updates
    // =========================================================================

    @Test
    fun keyStability_repeatedCallsOnSamePatient_producesIdenticalKeysAndOrder() {
        val scan1 = sampleScan("s-1", "2026-02-01", imageUris = listOf("uri://photo1.jpg", "uri://photo2.jpg"))
        val visit1 = sampleVisit("v-1", "2026-01-15")
        val att1 = sampleAttachment("a-1", "2026-01-15", "vault_att1.jpg", linkedRecordId = "v-1")
        val att2 = sampleAttachment("a-2", "2026-02-10", "vault_att2.jpg", title = "Growth chart")

        val patient = samplePatient(
            scans = listOf(scan1),
            visits = listOf(visit1),
            attachments = listOf(att1, att2),
        )

        val run1 = pictureCandidates(patient)
        val run2 = pictureCandidates(patient)

        assertEquals("Candidate count must match", run1.size, run2.size)
        assertEquals("Keys must be identical in both runs", run1.map { it.key }, run2.map { it.key })
        assertEquals("Locations must match", run1.map { it.location }, run2.map { it.location })
        assertEquals("Source labels must match", run1.map { it.sourceLabel }, run2.map { it.sourceLabel })
    }

    @Test
    fun keyStability_keyFormatPatterns_areDeterministic() {
        val scan = sampleScan("scan-abc", "2026-03-01", imageUris = listOf("p0.jpg", "p1.jpg"))
        val att = sampleAttachment("att-xyz", "2026-03-05", "att.jpg")
        val patient = samplePatient(scans = listOf(scan), attachments = listOf(att))

        val candidates = pictureCandidates(patient)
        assertEquals("scan:scan-abc:0", candidates[0].key)
        assertEquals("scan:scan-abc:1", candidates[1].key)
        assertEquals("att:att-xyz", candidates[2].key)
    }

    @Test
    fun keyStability_shuffledInputCollections_yieldsDeterministicKeyOrder() {
        val sEarly = sampleScan("s-early", "2026-01-10", imageUris = listOf("early.jpg"))
        val sMid = sampleScan("s-mid", "2026-02-15", imageUris = listOf("mid.jpg"))
        val sLate = sampleScan("s-late", "2026-03-20", imageUris = listOf("late.jpg"))

        val pOrdered = samplePatient(scans = listOf(sEarly, sMid, sLate))
        val pReversed = samplePatient(scans = listOf(sLate, sMid, sEarly))
        val pScrambled = samplePatient(scans = listOf(sMid, sLate, sEarly))

        val keysOrdered = pictureCandidates(pOrdered).map { it.key }
        val keysReversed = pictureCandidates(pReversed).map { it.key }
        val keysScrambled = pictureCandidates(pScrambled).map { it.key }

        assertEquals(keysOrdered, keysReversed)
        assertEquals(keysOrdered, keysScrambled)
        assertEquals(listOf("scan:s-early:0", "scan:s-mid:0", "scan:s-late:0"), keysOrdered)
    }

    @Test
    fun keyStability_existingCandidateKeysPreservedWhenPatientReconstructedWithNewVisitsOrScans() {
        val scan1 = sampleScan("s-1", "2026-01-10", imageUris = listOf("s1.jpg"))
        val initialPatient = samplePatient(scans = listOf(scan1))
        val initialCandidates = pictureCandidates(initialPatient)
        val selectedKey = initialCandidates.first().key
        assertEquals("scan:s-1:0", selectedKey)

        // Clinician records a new visit and a later scan
        val updatedPatient = initialPatient.copy(
            visits = listOf(sampleVisit("v-new", "2026-02-01")),
            ultrasoundScans = initialPatient.ultrasoundScans + sampleScan("s-2", "2026-03-01", imageUris = listOf("s2.jpg")),
        )
        val updatedCandidates = pictureCandidates(updatedPatient)
        val matchingCandidate = updatedCandidates.find { it.key == selectedKey }

        assertNotNull("Previously selected candidate key must remain valid in updated patient", matchingCandidate)
        assertEquals("s1.jpg", matchingCandidate?.location)
    }
}
