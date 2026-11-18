package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.LabResultFlag
import com.sherifradwan.ancnative.model.NORMAL_FETAL_NUMBER
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.model.labResultFlag
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import java.util.Locale

private const val MAX_LAB_DATE_COLUMNS = 6
private const val DEFAULT_LAB_CATEGORY = "Other"

/** Structured ultrasound summary used across print, native report, and share layers. */
data class ScanOutcome(
    val typeLabel: String?,
    val presentation: String?,
    val measurements: List<Pair<String, String>>,
    val extras: List<String>,
)

/** Single result entry inside a [LabCell] with parsed direction and abnormal fallback. */
data class LabCellEntry(
    val value: String,
    val flag: LabResultFlag?,
    val abnormalNoDirection: Boolean,
)

/** Cell inside a [LabComparisonTable] row with parsed direction, abnormal fallback, and all same-day entries. */
data class LabCell(
    val value: String,
    val flag: LabResultFlag?,
    val abnormalNoDirection: Boolean,
    val entries: List<LabCellEntry> = listOf(
        LabCellEntry(value = value, flag = flag, abnormalNoDirection = abnormalNoDirection),
    ),
)

/** Clinical note attached to a lab result, optionally with the performing laboratory name. */
data class LabNote(
    val date: String,
    val test: String,
    val text: String,
    val labName: String?,
)

/** Grouped comparison table of repeated lab tests within one category across up to six dates. */
data class LabComparisonTable(
    val category: String,
    val dates: List<String>,
    val rows: List<LabRow>,
    val notes: List<LabNote> = emptyList(),
)

/** One test row inside a [LabComparisonTable], aligned column-by-column with its table dates. */
data class LabRow(
    val label: String,
    val cells: List<LabCell?>,
)

/** Single non-repeated lab result entry for individual listing. */
data class SingleLab(
    val test: String,
    val date: String,
    val result: String,
    val unit: String?,
    val flagMark: String?,
)

/** Partitioned lab summary containing repeated comparison tables and single-date results. */
data class LabReport(
    val comparisons: List<LabComparisonTable>,
    val singles: List<SingleLab>,
    val notes: List<LabNote> = emptyList(),
)

/** Selectable clinical picture candidate derived from a scan image or patient attachment. */
data class PictureCandidate(
    val key: String,
    val sourceLabel: String,
    val date: String,
    val linkedRecordId: String?,
    val location: String,
)

/** Numbered picture selected for printing or display. */
data class Figure(
    val number: Int,
    val candidate: PictureCandidate,
)

/** Extracts compact scan outcome details without dropping clinical measurements or extras. */
fun scanOutcomeFor(scan: DemoUltrasoundScan): ScanOutcome {
    val typeLabel = scan.fetalNumber?.trim()?.takeIf { it.isNotEmpty() }?.let { fn ->
        when {
            fn.equals(NORMAL_FETAL_NUMBER, ignoreCase = true) -> NORMAL_FETAL_NUMBER
            fn.equals("Twins", ignoreCase = true) -> "Multiple pregnancy: twins"
            fn.equals("Higher order", ignoreCase = true) -> "Multiple pregnancy: higher order"
            else -> fn
        }
    }
    val presentation = scan.presentation?.trim()?.takeIf { it.isNotEmpty() }
    val measurements = buildList {
        scan.biometryBpd?.trim()?.takeIf { it.isNotEmpty() }?.let { add("BPD" to it) }
        scan.biometryHc?.trim()?.takeIf { it.isNotEmpty() }?.let { add("HC" to it) }
        scan.biometryAc?.trim()?.takeIf { it.isNotEmpty() }?.let { add("AC" to it) }
        scan.biometryFl?.trim()?.takeIf { it.isNotEmpty() }?.let { add("FL" to it) }
        if (!scan.estimatedFetalWeight.isNullOrBlank()) {
            add("EFW" to scanWeight(scan))
        }
    }
    val extras = buildList {
        scan.amnioticFluid?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Fluid: $it") }
        scan.placentalSite?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Placenta: $it") }
        scan.fetalHeartRate?.trim()?.takeIf { it.isNotEmpty() }?.let { add("FHR: $it") }
        scan.impression?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
    }
    return ScanOutcome(
        typeLabel = typeLabel,
        presentation = presentation,
        measurements = measurements,
        extras = extras,
    )
}

/** Returns trimmed visit note text, or null when blank. */
fun visitNote(visit: DemoVisit): String? =
    visit.notes?.trim()?.takeIf { it.isNotEmpty() }

/** Returns the latest ultrasound scan linked to [visit] by date then id, or null if none match. */
fun latestScanForVisit(patient: DemoPatient, visit: DemoVisit): DemoUltrasoundScan? {
    val matching = patient.ultrasoundScans.filter { scan ->
        (scan.linkedVisitId != null && scan.linkedVisitId == visit.id) ||
            (visit.linkedUltrasoundId != null && visit.linkedUltrasoundId == scan.id)
    }
    return matching.maxWithOrNull(
        compareBy<DemoUltrasoundScan>(
            { parseIsoDateOrNull(it.date) != null },
            { parseIsoDateOrNull(it.date) },
            { it.id },
        ),
    )
}

/** Returns all scans not shown as the latest scan for any visit in [shownVisits], sorted oldest first. */
fun unlinkedScans(patient: DemoPatient, shownVisits: List<DemoVisit>): List<DemoUltrasoundScan> {
    val linkedScanIds = shownVisits.mapNotNull { visit ->
        latestScanForVisit(patient, visit)?.id
    }.toSet()
    return patient.ultrasoundScans
        .filter { it.id !in linkedScanIds }
        .sortedBy { reportSortKey(it.date) }
}

/** Groups repeated lab tests by category into comparison tables and lists one-date tests individually. */
fun labReport(patient: DemoPatient): LabReport {
    val validLabs = patient.labInvestigations.filter {
        it.testName.isNotBlank() && it.result.isNotBlank()
    }
    if (validLabs.isEmpty()) return LabReport(emptyList(), emptyList(), emptyList())

    val testGroups = validLabs.groupBy { lab ->
        LabTestKey(
            categoryKey = labCategory(lab).lowercase(Locale.ROOT),
            testKey = lab.testName.trim().lowercase(Locale.ROOT),
        )
    }

    val repeatedByCategory = linkedMapOf<String, MutableList<RepeatedLabSeries>>()
    val singleItems = mutableListOf<DemoLabInvestigation>()

    for ((testKey, labs) in testGroups) {
        val datedLabs = labs.filter { it.date.isNotBlank() }
        val undatedLabs = labs.filter { it.date.isBlank() }
        val distinctDateKeys = datedLabs.map { normalizedDateInput(it.date) }.distinct()

        if (distinctDateKeys.size >= 2) {
            val first = labs.first()
            val displayCategory = labCategory(first)
            val displayName = first.testName.trim()
            val primaryUnit = labs.firstNotNullOfOrNull(::cleanUnit)
            val byDate = linkedMapOf<String, MutableList<DemoLabInvestigation>>()
            for (lab in datedLabs) {
                byDate.getOrPut(normalizedDateInput(lab.date)) { mutableListOf() }.add(lab)
            }
            repeatedByCategory.getOrPut(testKey.categoryKey) { mutableListOf() }.add(
                RepeatedLabSeries(
                    displayCategory = displayCategory,
                    displayName = displayName,
                    primaryUnit = primaryUnit,
                    byDate = LinkedHashMap(byDate),
                ),
            )
            singleItems.addAll(undatedLabs)
        } else {
            singleItems.addAll(labs)
        }
    }

    val comparisons = buildList {
        for (seriesList in repeatedByCategory.values) {
            val categoryTitle = seriesList.first().displayCategory
            val sortedDateKeys = seriesList
                .flatMap { it.byDate.keys }
                .distinct()
                .sortedBy { reportSortKey(it) }

            for (dateChunk in sortedDateKeys.chunked(MAX_LAB_DATE_COLUMNS)) {
                val formattedDates = dateChunk.map { formatDmy(it) ?: it }
                val rows = seriesList.mapNotNull { series ->
                    val cells = dateChunk.map { dateKey ->
                        series.byDate[dateKey]?.let { sameDateLabs -> toLabCell(sameDateLabs, series.primaryUnit) }
                    }
                    if (cells.all { it == null }) {
                        null
                    } else {
                        val rowLabel = if (series.primaryUnit != null) {
                            "${series.displayName} (${series.primaryUnit})"
                        } else {
                            series.displayName
                        }
                        LabRow(label = rowLabel, cells = cells)
                    }
                }
                val tableNotes = dateChunk.flatMap { dateKey ->
                    seriesList.flatMap { series ->
                        series.byDate[dateKey].orEmpty().mapNotNull(::toLabNote)
                    }
                }
                add(
                    LabComparisonTable(
                        category = categoryTitle,
                        dates = formattedDates,
                        rows = rows,
                        notes = tableNotes,
                    ),
                )
            }
        }
    }

    val sortedSingles = singleItems.sortedBy { reportSortKey(normalizedDateInput(it.date)) }
    val singles = sortedSingles.map { lab ->
        val (_, _, flagMark) = resolveLabFlags(lab)
        val dateInput = normalizedDateInput(lab.date)
        SingleLab(
            test = lab.testName.trim(),
            date = formatDmy(dateInput).orEmpty(),
            result = lab.result.trim(),
            unit = cleanUnit(lab),
            flagMark = flagMark,
        )
    }
    val singleNotes = sortedSingles.mapNotNull(::toLabNote)

    return LabReport(comparisons = comparisons, singles = singles, notes = singleNotes)
}

/** Collects selectable picture candidates from scan images and patient attachments in date order. */
fun pictureCandidates(patient: DemoPatient): List<PictureCandidate> {
    val scanById = patient.ultrasoundScans.associateBy { it.id }
    val visitNumberById = reportVisits(patient, Int.MAX_VALUE).rows
        .mapIndexed { idx, visit -> visit.id to (idx + 1) }
        .toMap()

    val rawCandidates = buildList {
        for (scan in patient.ultrasoundScans) {
            val rawDate = normalizedDateInput(scan.date)
            val displayDate = formatDmy(rawDate).orEmpty()
            val label = if (displayDate.isNotEmpty()) "Ultrasound $displayDate" else "Ultrasound"
            scan.imageUris.forEachIndexed { index, uri ->
                val cleanUri = uri.trim()
                if (cleanUri.isNotEmpty()) {
                    add(
                        RawPictureCandidate(
                            isoDate = rawDate,
                            candidate = PictureCandidate(
                                key = "scan:${scan.id}:$index",
                                sourceLabel = label,
                                date = displayDate,
                                linkedRecordId = scan.id,
                                location = cleanUri,
                            ),
                        ),
                    )
                }
            }
        }

        for (attachment in patient.attachments) {
            val rawLocation = attachment.fileName.trim().ifEmpty { attachment.filePath.trim() }
            if (rawLocation.isEmpty()) continue

            val linkedId = attachment.linkedRecordId?.trim()?.takeIf { it.isNotEmpty() }
            val linkedScan = linkedId?.let { scanById[it] }
            val linkedVisit = linkedId?.let { id -> patient.visits.firstOrNull { it.id == id } }
            val linkedVisitNumber = linkedId?.let { visitNumberById[it] }

            val rawDate = normalizedDateInput(attachment.capturedAt)
                .ifEmpty { linkedScan?.let { normalizedDateInput(it.date) }.orEmpty() }
                .ifEmpty { linkedVisit?.let { normalizedDateInput(it.date) }.orEmpty() }
            val displayDate = formatDmy(rawDate).orEmpty()

            val sourceLabel = when {
                linkedScan != null -> {
                    val scanDate = formatDmy(normalizedDateInput(linkedScan.date))
                    if (scanDate != null) "Ultrasound $scanDate" else "Ultrasound"
                }
                linkedVisitNumber != null -> "Visit #$linkedVisitNumber"
                else -> attachment.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Patient profile"
            }

            add(
                RawPictureCandidate(
                    isoDate = rawDate,
                    candidate = PictureCandidate(
                        key = "att:${attachment.id}",
                        sourceLabel = sourceLabel,
                        date = displayDate,
                        linkedRecordId = linkedId,
                        location = rawLocation,
                    ),
                ),
            )
        }
    }

    return rawCandidates
        .sortedWith(compareBy<RawPictureCandidate>({ reportSortKey(it.isoDate) }, { it.candidate.key }))
        .map { it.candidate }
}

/** Assigns sequential figure numbers in the given candidate order to the selected picture candidates. */
fun figuresFor(candidates: List<PictureCandidate>, selectedKeys: Set<String>): List<Figure> {
    if (selectedKeys.isEmpty()) return emptyList()
    return candidates
        .filter { it.key in selectedKeys }
        .mapIndexed { idx, candidate -> Figure(number = idx + 1, candidate = candidate) }
}

/** Returns the selected figure numbers associated with [recordId] in figure order. */
fun figureRefsFor(recordId: String, figures: List<Figure>): List<Int> {
    if (recordId.isBlank()) return emptyList()
    return figures
        .filter { it.candidate.linkedRecordId == recordId }
        .map { it.number }
}

private data class LabTestKey(
    val categoryKey: String,
    val testKey: String,
)

private data class RepeatedLabSeries(
    val displayCategory: String,
    val displayName: String,
    val primaryUnit: String?,
    val byDate: LinkedHashMap<String, List<DemoLabInvestigation>>,
)

private data class LabFlagInfo(
    val flag: LabResultFlag?,
    val abnormalNoDirection: Boolean,
    val flagMark: String?,
)

private data class RawPictureCandidate(
    val isoDate: String,
    val candidate: PictureCandidate,
)

private fun cleanUnit(lab: DemoLabInvestigation): String? =
    lab.unit?.trim()?.takeIf { it.isNotEmpty() }

private fun toLabNote(lab: DemoLabInvestigation): LabNote? {
    val text = lab.notes?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val dateInput = normalizedDateInput(lab.date)
    return LabNote(
        date = formatDmy(dateInput).orEmpty(),
        test = lab.testName.trim(),
        text = text,
        labName = lab.labName?.trim()?.takeIf { it.isNotEmpty() },
    )
}

private fun labCategory(lab: DemoLabInvestigation): String =
    lab.testCategory.trim().ifEmpty { DEFAULT_LAB_CATEGORY }

private fun resolveLabFlags(lab: DemoLabInvestigation): LabFlagInfo {
    if (!lab.isAbnormal) {
        return LabFlagInfo(flag = null, abnormalNoDirection = false, flagMark = null)
    }
    val direction = labResultFlag(lab.result, lab.referenceRange.orEmpty())
    return when (direction) {
        LabResultFlag.HIGH -> LabFlagInfo(flag = LabResultFlag.HIGH, abnormalNoDirection = false, flagMark = "H")
        LabResultFlag.LOW -> LabFlagInfo(flag = LabResultFlag.LOW, abnormalNoDirection = false, flagMark = "L")
        LabResultFlag.NORMAL, LabResultFlag.UNKNOWN -> LabFlagInfo(
            flag = null,
            abnormalNoDirection = true,
            flagMark = "*",
        )
    }
}

private fun toLabCellEntry(lab: DemoLabInvestigation, rowUnit: String?): LabCellEntry {
    val (flag, abnormalNoDirection, _) = resolveLabFlags(lab)
    val trimmedResult = lab.result.trim()
    val cellUnit = cleanUnit(lab)
    val value = if (cellUnit != null && rowUnit != null && !cellUnit.equals(rowUnit, ignoreCase = true)) {
        "$trimmedResult $cellUnit"
    } else {
        trimmedResult
    }
    return LabCellEntry(
        value = value,
        flag = flag,
        abnormalNoDirection = abnormalNoDirection,
    )
}

/** Returns `"H"`, `"L"`, `"*"`, or null for a comparison cell entry. */
fun LabCellEntry.flagMark(): String? = entryFlagMark(flag, abnormalNoDirection)

/** One repeated test with all chronological `(date, cell)` pairs across table chunks. */
data class LabComparisonSeriesRow(
    val label: String,
    val entries: List<Pair<String, LabCell>>,
)

/** One category's merged comparison rows and notes for narrow wrapping layouts. */
data class LabComparisonGroup(
    val category: String,
    val rows: List<LabComparisonSeriesRow>,
    val notes: List<LabNote>,
)

/** Merges 6-date comparison chunks from [labReport] into one chronological group per category. */
fun groupLabComparisonsByCategory(comparisons: List<LabComparisonTable>): List<LabComparisonGroup> {
    if (comparisons.isEmpty()) return emptyList()
    return comparisons.groupBy { it.category }.map { (category, tables) ->
        val rowsByLabel = linkedMapOf<String, MutableList<Pair<String, LabCell>>>()
        for (table in tables) {
            for (row in table.rows) {
                val pairs = rowsByLabel.getOrPut(row.label) { mutableListOf() }
                row.cells.forEachIndexed { colIndex, cell ->
                    if (cell != null) pairs.add(table.dates.getOrNull(colIndex).orEmpty() to cell)
                }
            }
        }
        LabComparisonGroup(
            category = category,
            rows = rowsByLabel.map { (label, entries) -> LabComparisonSeriesRow(label = label, entries = entries) },
            notes = tables.flatMap { it.notes }.filter { it.text.isNotBlank() },
        )
    }
}

private fun entryFlagMark(flag: LabResultFlag?, abnormalNoDirection: Boolean): String? = when {
    flag == LabResultFlag.HIGH -> "H"
    flag == LabResultFlag.LOW -> "L"
    abnormalNoDirection -> "*"
    else -> null
}

private fun toLabCell(labs: List<DemoLabInvestigation>, rowUnit: String?): LabCell {
    val entries = labs.map { toLabCellEntry(it, rowUnit) }
    if (entries.size == 1) {
        val single = entries.first()
        return LabCell(
            value = single.value,
            flag = single.flag,
            abnormalNoDirection = single.abnormalNoDirection,
            entries = entries,
        )
    }
    val combinedValue = entries.joinToString(" / ") { entry ->
        val mark = entryFlagMark(entry.flag, entry.abnormalNoDirection)
        if (mark != null) "${entry.value} $mark" else entry.value
    }
    val distinctFlags = entries.mapNotNull { it.flag }.distinct()
    val hasUnflaggedAbnormal = entries.any { it.abnormalNoDirection }
    val combinedFlag = if (!hasUnflaggedAbnormal && distinctFlags.size == 1 && entries.all { it.flag == distinctFlags.first() }) {
        distinctFlags.first()
    } else {
        null
    }
    val combinedAbnormalNoDirection = combinedFlag == null && (hasUnflaggedAbnormal || distinctFlags.isNotEmpty())
    return LabCell(
        value = combinedValue,
        flag = combinedFlag,
        abnormalNoDirection = combinedAbnormalNoDirection,
        entries = entries,
    )
}

/** Extracts the leading yyyy-MM-dd portion from ISO timestamps while preserving non-ISO text. */
private fun normalizedDateInput(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return ""
    if (parseIsoDateOrNull(trimmed) != null) return trimmed
    if (trimmed.length > 10 && (trimmed[10] == 'T' || trimmed[10] == ' ')) {
        val prefix = trimmed.take(10)
        if (parseIsoDateOrNull(prefix) != null) return prefix
    }
    return trimmed
}
