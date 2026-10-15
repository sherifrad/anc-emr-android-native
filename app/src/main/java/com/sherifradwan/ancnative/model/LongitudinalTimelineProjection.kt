package com.sherifradwan.ancnative.model

import java.time.LocalDate

enum class TimelineEventKind {
    VISIT,
    STANDALONE_ULTRASOUND,
}

data class LongitudinalTimelineEvent(
    val eventKey: String,
    val id: String,
    val dateString: String,
    val parsedDate: LocalDate?,
    val kind: TimelineEventKind,
    val visit: DemoVisit? = null,
    val standaloneScan: DemoUltrasoundScan? = null,
    val linkedScan: DemoUltrasoundScan? = null,
    val linkIntegrityWarning: String? = null,
)

object LongitudinalTimelineProjection {

    fun build(patient: DemoPatient): List<LongitudinalTimelineEvent> {
        val events = mutableListOf<LongitudinalTimelineEvent>()

        // Filter ultrasound scans strictly owned by this patient
        val patientScans = patient.ultrasoundScans.filter { it.patientId == patient.id }
        val scanById = patientScans.associateBy { it.id }

        // Reference counts to detect conflicts (e.g. multiple visits referencing one scan)
        val scanReferenceCounts = mutableMapOf<String, Int>()
        patient.visits.forEach { visit ->
            visit.linkedUltrasoundId?.let { scanId ->
                scanReferenceCounts[scanId] = (scanReferenceCounts[scanId] ?: 0) + 1
            }
        }

        // 1. Process Visits
        for (visit in patient.visits) {
            val parsedDate = parseDateOrNull(visit.date)
            var linkedScan: DemoUltrasoundScan? = null
            var warning: String? = null

            val targetScanId = visit.linkedUltrasoundId
            if (targetScanId != null) {
                val candidateScan = scanById[targetScanId]
                if (candidateScan == null) {
                    warning = "Linked ultrasound ($targetScanId) not found in patient records"
                } else {
                    linkedScan = candidateScan
                    val conflictCount = scanReferenceCounts[targetScanId] ?: 1
                    if (conflictCount > 1) {
                        warning = "Conflict: Multiple visits reference this ultrasound scan"
                    } else if (candidateScan.linkedVisitId != null && candidateScan.linkedVisitId != visit.id) {
                        warning = "Conflict: Ultrasound references a different visit (${candidateScan.linkedVisitId})"
                    }
                }
            }

            events.add(
                LongitudinalTimelineEvent(
                    eventKey = "visit:${visit.id}",
                    id = visit.id,
                    dateString = visit.date,
                    parsedDate = parsedDate,
                    kind = TimelineEventKind.VISIT,
                    visit = visit,
                    standaloneScan = null,
                    linkedScan = linkedScan,
                    linkIntegrityWarning = warning,
                )
            )
        }

        // 2. Process Standalone Ultrasounds (not consumed by any visit)
        for (scan in patientScans) {
            val consumedByVisit = patient.visits.any { it.linkedUltrasoundId == scan.id }
            if (!consumedByVisit) {
                val parsedDate = parseDateOrNull(scan.date)
                var warning: String? = null
                if (scan.linkedVisitId != null) {
                    val targetVisit = patient.visits.firstOrNull { it.id == scan.linkedVisitId }
                    if (targetVisit == null) {
                        warning = "References non-existent visit (${scan.linkedVisitId})"
                    } else {
                        warning = "One-way link from scan to visit (${scan.linkedVisitId})"
                    }
                }

                events.add(
                    LongitudinalTimelineEvent(
                        eventKey = "scan:${scan.id}",
                        id = scan.id,
                        dateString = scan.date,
                        parsedDate = parsedDate,
                        kind = TimelineEventKind.STANDALONE_ULTRASOUND,
                        visit = null,
                        standaloneScan = scan,
                        linkedScan = null,
                        linkIntegrityWarning = warning,
                    )
                )
            }
        }

        // 3. Chronological sort:
        // Valid parsed dates first, newest at top (descending).
        // Undated/invalid dates last, deterministic tie-break by eventKey.
        return events.sortedWith(
            compareByDescending<LongitudinalTimelineEvent> { it.parsedDate != null }
                .thenByDescending { it.parsedDate }
                .thenBy { it.eventKey }
        )
    }

    private fun parseDateOrNull(dateString: String?): LocalDate? {
        if (dateString.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dateString.trim())
        } catch (_: Exception) {
            null
        }
    }
}
