package com.sherifradwan.ancnative.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.LabResultFlag
import com.sherifradwan.ancnative.model.bpLevel
import com.sherifradwan.ancnative.model.datingDiscordanceLine
import com.sherifradwan.ancnative.model.datingSummaryOf
import com.sherifradwan.ancnative.model.datingTextRows
import com.sherifradwan.ancnative.model.deliveryOutcomeLine
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.ui.AllergyState
import com.sherifradwan.ancnative.ui.classifyAllergy
import com.sherifradwan.ancnative.ui.formatAgeYears
import com.sherifradwan.ancnative.ui.formatWeightKg
import com.sherifradwan.ancnative.ui.isRhNegative
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object PatientPrintHtmlGenerator {

    private const val MAX_PRINT_VISITS = 12
    private const val LOGO_PRINT_HEIGHT_PX = 360
    private val LAB_NOTE_DMY_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)

    private const val BP_ALERT_RAISED = "Raised BP - recheck"
    private const val BP_ALERT_SEVERE = "Severe-range BP - urgent assessment"

    private fun hiddenRowsHtml(hidden: Int, colspan: Int, what: String): String {
        if (hidden <= 0) return ""
        return """<tr class="empty-prior-row"><td colspan="$colspan">$hidden $what not shown</td></tr>"""
    }

    @Volatile
    private var cachedLogoBase64: String? = null

    private fun getLogoBase64(context: Context?): String {
        if (context == null) return ""
        cachedLogoBase64?.let { return it }
        return try {
            val full = BitmapFactory.decodeResource(context.resources, R.drawable.clinic_logo_mark) ?: return ""
            val height = LOGO_PRINT_HEIGHT_PX
            val width = (full.width * height.toFloat() / full.height).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createScaledBitmap(full, width, height, true)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val encoded = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            cachedLogoBase64 = encoded
            encoded
        } catch (_: Exception) {
            ""
        }
    }

    private fun containsArabic(text: String): Boolean =
        text.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }

    fun generateHtml(
        context: Context? = null,
        patient: DemoPatient,
        settings: SecuritySettingsState? = null,
        asOfDate: LocalDate = LocalDate.now(),
        selectedPictureKeys: Set<String> = emptySet(),
        pictureDataUri: (PictureCandidate) -> String? = { null },
    ): String {
        val logoBase64 = getLogoBase64(context)
        val attestation = settings.resolvedAttestation()
        val clinicName = escapeHtml(settings?.clinicName?.takeUnless(String::isBlank) ?: "Dr. Sherif Radwan Clinic")
        val clinicSpecialty = escapeHtml("Maternal-Fetal Medicine & Advanced Obstetrics")
        val doctorNameEn = escapeHtml(attestation.doctorNameEn)
        val doctorOfficialTitle = escapeHtml(attestation.doctorSpecialtyEn)
        val doctorNameAr = escapeHtml(attestation.doctorNameAr)
        val doctorSpecialtyAr = escapeHtml(attestation.doctorSpecialtyAr)
        val clinicPhone = escapeHtml(attestation.clinicPhone)
        val printDateStr = escapeHtml(formatDmy(asOfDate))
        val currentGa: String? = reportGestationalAge(patient, asOfDate)
        val statusLine: String? = reportStatusLine(patient)
        val datingSummary = datingSummaryOf(patient, asOfDate)

        val datingRowsHtml = buildString {
            datingTextRows(datingSummary).forEach { row ->
                val valueClass = when {
                    row.isEdd -> "data-val col-date bold-val"
                    row.label == "LMP" || row.label == "ET" -> "data-val col-date"
                    else -> "data-val"
                }
                append("""<div class="data-row"><span class="data-key">${escapeHtml(row.label)}</span>""")
                append("""<span class="$valueClass">${escapeHtml(row.value ?: "Not recorded")}</span></div>""")
            }
            datingDiscordanceLine(patient.dating, datingSummary)?.let {
                append("""<div class="data-row"><span class="data-val data-val-full">${escapeHtml(it)}</span></div>""")
            }
        }

        val gaBannerHtml = when {
            currentGa != null -> """<span>Gestational age today</span><span class="bold-val">${escapeHtml(currentGa)}</span>"""
            patient.delivered -> """<span>${escapeHtml(patient.deliveryOutcomeLine())}</span>"""
            else -> """<span>Gestational age: not recorded</span>"""
        }

        val g = patient.gravida ?: "-"
        val p = patient.parity ?: "-"
        val a = patient.abortion ?: "-"
        val pastDeliverySummary = previousDeliverySummary(patient)

        val risk = reportRisk(patient.riskLevel)
        val isRhNegative = isRhNegative(patient.bloodGroupText)

        val candidates = pictureCandidates(patient)
        val figures = figuresFor(candidates, selectedPictureKeys)

        val visits = reportVisits(patient, MAX_PRINT_VISITS)
        val bpAlertCss = when {
            visits.rows.any { bpLevel(it.bloodPressure) == BpLevel.SEVERE } ->
                "\n        .bp-alert { font-size: 9pt; font-weight: 700; line-height: 1.25; margin-top: 2px; color: #b45309; display: block; white-space: normal; }\n        .bp-alert-severe { color: var(--color-alert-text); }"
            visits.rows.any { bpLevel(it.bloodPressure) == BpLevel.RAISED } ->
                "\n        .bp-alert { font-size: 9pt; font-weight: 700; line-height: 1.25; margin-top: 2px; color: #b45309; display: block; white-space: normal; }"
            else -> ""
        }

        val visitsSectionHtml = renderVisitsSection(patient, visits, figures)
        val unlinkedScansHtml = renderUnlinkedScansSection(unlinkedScans(patient, visits.rows), figures)
        val labsSectionHtml = renderLabsSection(labReport(patient))
        val meds = reportMedications(patient)
        val currentMedsHtml = renderMedicationsSection(
            title = "Current medications",
            countText = "(${meds.current.size} active)",
            meds = meds.current,
            emptyMessage = "No current medications recorded",
        )
        val stoppedMedsHtml = if (meds.stopped.isNotEmpty()) {
            renderMedicationsSection(
                title = "Stopped medications",
                countText = "(${meds.stopped.size})",
                meds = meds.stopped,
                emptyMessage = null,
            )
        } else {
            ""
        }
        val prevPregSectionHtml = renderPreviousPregnanciesSection(patient, pastDeliverySummary)
        val attachedImagesHtml = renderAttachedImagesSection(figures, pictureDataUri)

        val deliveryRowsHtml = reportDeliveryRows(patient, datingSummary.edd?.let(::formatDmy)).joinToString("\n") { row ->
            val vClass = when (row.label) {
                "Estimated due date" -> "data-val bold-val col-date"
                "Delivery Outcome" -> "data-val bold-val"
                "Blood bank precautions" -> "data-val data-val-alert"
                else -> "data-val"
            }
            """<div class="data-row"><span class="data-key">${escapeHtml(row.label)}</span><span class="$vClass">${escapeHtml(row.value)}</span></div>"""
        }

        val logoImgTag = if (logoBase64.isNotEmpty()) {
            """<img src="data:image/png;base64,$logoBase64" alt="Clinic Logo" class="clinic-logo" />"""
        } else {
            """<div class="clinic-mark">DR</div>"""
        }

        val ageFormatted = patient.ageText?.trim()?.takeIf(String::isNotEmpty)?.let { ageText ->
            formatAgeYears(ageText)?.let { "$it yrs" } ?: ageText
        } ?: "-"
        val patientNameDirAttr = if (containsArabic(patient.displayName)) """ dir="rtl"""" else ""
        val allergyClass = if (classifyAllergy(patient.allergyHistory) is AllergyState.Recorded) {
            "data-val data-val-alert"
        } else {
            "data-val"
        }
        val riskPillHtml = when (risk) {
            ReportRisk.HIGH -> """<span class="risk-pill risk-high">${risk.label}</span>"""
            ReportRisk.MEDIUM -> """<span class="risk-pill risk-medium">${risk.label}</span>"""
            ReportRisk.LOW -> """<span class="risk-pill risk-low">${risk.label}</span>"""
            ReportRisk.UNRECORDED -> """<span class="risk-pill risk-unrecorded">${escapeHtml(reportRiskLabel(patient.riskLevel))}</span>"""
        }

        return """
<!DOCTYPE html>
<html lang="en" dir="ltr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Clinical record - ${escapeHtml(patient.displayName)} - $clinicName</title>
    <style>
${buildCss(bpAlertCss)}
    </style>
</head>
<body>
    <div class="page-flow">
        <table class="print-shell">
            <tbody>
                <tr>
                    <td class="print-shell-cell">
                        <header class="clinic-header">
                            <div class="clinic-brand">
                                $logoImgTag
                                <div class="clinic-title-box">
                                    <h1>$clinicName</h1>
                                    <div class="subhead">$clinicSpecialty</div>
                                    <div class="specialty">$doctorOfficialTitle</div>
                                </div>
                            </div>
                            <div class="doc-meta">
                                <div class="doc-badge">Clinical record</div>
                                <div>Patient ID: <strong>${escapeHtml(patient.caseId)}</strong></div>
                                <div>Print date: <strong class="col-date">$printDateStr</strong></div>
                            </div>
                        </header>

                        ${if (statusLine != null) """<div class="status-banner">${escapeHtml(statusLine)}</div>""" else ""}

                        <section class="patient-banner">
                            <div class="pt-identity">
                                <div class="patient-name"$patientNameDirAttr>${escapeHtml(patient.displayName)}</div>
                                <div class="pt-meta-inline">
                                    <span>Patient ID: <strong>${escapeHtml(patient.caseId)}</strong></span>
                                    <span>Phone: <strong>${escapeHtml(patient.phone?.takeUnless(String::isBlank) ?: "Unspecified")}</strong></span>
                                    ${if (!patient.addressText.isNullOrBlank()) """<span>Address: <strong>${escapeHtml(patient.addressText)}</strong></span>""" else ""}
                                    ${if (!patient.dateOfBirth.isNullOrBlank()) """<span>Date of birth: <strong class="col-date">${escapeHtml(reportDate(patient.dateOfBirth))}</strong></span>""" else ""}
                                </div>
                            </div>
                            <div class="pt-vitals-strip">
                                <div class="vital-cell">
                                    <span class="vital-label">Age</span>
                                    <span class="vital-value">${escapeHtml(ageFormatted)}</span>
                                </div>
                                <div class="vital-cell">
                                    <span class="vital-label">Blood group</span>
                                    <span class="vital-value${if (isRhNegative) " data-val-alert" else ""}">${escapeHtml(patient.bloodGroupText?.takeUnless(String::isBlank) ?: "-")}</span>
                                    ${if (isRhNegative) """<span class="risk-pill risk-high">Rh negative</span>""" else ""}
                                </div>
                                <div class="vital-cell">
                                    <span class="vital-label">Clinical risk</span>
                                    $riskPillHtml
                                </div>
                            </div>
                        </section>

                        ${if (!patient.clinicalAttention.isNullOrBlank()) """<div class="attention-box"><span class="k">Clinical attention</span><span class="v">${escapeHtml(patient.clinicalAttention)}</span></div>""" else ""}

                        <div class="two-column-layout">
                            <div>
                                <h2 class="section-heading">Pregnancy dating</h2>
                                <div class="data-list">
                                    $datingRowsHtml
                                </div>
                                <div class="ga-banner">
                                    $gaBannerHtml
                                </div>
                            </div>

                            <div>
                                <h2 class="section-heading">Obstetric and medical history</h2>
                                <div class="data-list">
                                    <div class="data-row">
                                        <span class="data-key">GPA formula</span>
                                        <span class="data-val">G$g P$p + A$a (${escapeHtml(pastDeliverySummary)})</span>
                                    </div>
                                    <div class="data-row">
                                        <span class="data-key">Drug allergies</span>
                                        <span class="$allergyClass">${escapeHtml(patient.allergyHistory?.takeUnless(String::isBlank) ?: "Not recorded")}</span>
                                    </div>
                                    <div class="data-row">
                                        <span class="data-key">Medical history</span>
                                        <span class="data-val">${escapeHtml(patient.medicalHistory?.takeUnless(String::isBlank) ?: "Not recorded")}</span>
                                    </div>
                                    <div class="data-row">
                                        <span class="data-key">Surgical history</span>
                                        <span class="data-val">${escapeHtml(patient.surgicalHistory?.takeUnless(String::isBlank) ?: "Not recorded")}</span>
                                    </div>
                                    <div class="data-row">
                                        <span class="data-key">Family history</span>
                                        <span class="data-val">${escapeHtml(patient.familyHistory?.takeUnless(String::isBlank) ?: "Not recorded")}</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        $prevPregSectionHtml

                        $visitsSectionHtml

                        $unlinkedScansHtml

                        $labsSectionHtml

                        $currentMedsHtml

                        $stoppedMedsHtml

                        <div class="two-column-layout avoid-break" style="margin-top: 6px;">
                            <div>
                                <h2 class="section-heading">Delivery plan</h2>
                                <div class="data-list">
                                    $deliveryRowsHtml
                                </div>
                            </div>

                            <div>
                                <h2 class="section-heading">Attending physician</h2>
                                <div class="attestation-wrapper">
                                    <div class="doc-name">$doctorNameEn</div>
                                    <div class="doc-ar-name" lang="ar" dir="rtl">$doctorNameAr</div>
                                    <div class="doc-ar-title" lang="ar" dir="rtl">$doctorSpecialtyAr</div>
                                    <div class="stamp-box">Stamp</div>
                                </div>
                            </div>
                        </div>

                        $attachedImagesHtml
                    </td>
                </tr>
            </tbody>
            <tfoot>
                <tr>
                    <td><div class="print-footer-space"></div></td>
                </tr>
            </tfoot>
        </table>

        <footer class="page-footer">
            <span><span$patientNameDirAttr>${escapeHtml(patient.displayName)}</span> · Patient ID: <strong>${escapeHtml(patient.caseId)}</strong> · Print date: <strong class="col-date">$printDateStr</strong></span>
            <span>Confidential medical record · $clinicName · $doctorOfficialTitle · Tel <span class="col-date">$clinicPhone</span></span>
        </footer>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun renderPreviousPregnanciesSection(
        patient: DemoPatient,
        pastDeliverySummary: String,
    ): String {
        val bodyHtml = if (patient.previousPregnancies.isEmpty()) {
            """<p class="empty-note">No previous pregnancies or deliveries recorded (${escapeHtml(pastDeliverySummary)}).</p>"""
        } else {
            val rowsHtml = patient.previousPregnancies.joinToString("\n") { pp ->
                val (outcomeLabel, isLoss) = reportPreviousPregnancyOutcome(pp)
                val outcomeHtml = if (isLoss) {
                    """<span class="risk-pill risk-high">${escapeHtml(outcomeLabel)}</span>"""
                } else {
                    escapeHtml(outcomeLabel)
                }
                val dateFormatted = formatDmy(pp.dateText) ?: "-"
                val typeAndMode = reportPreviousPregnancyTypeAndMode(pp)
                val notes = reportPreviousPregnancyNotes(pp)
                """
                <tr>
                    <td class="col-date bold-val">${escapeHtml(dateFormatted)}</td>
                    <td class="bold-val">${escapeHtml(typeAndMode)}</td>
                    <td>$outcomeHtml</td>
                    <td>${escapeHtml(notes)}</td>
                </tr>
                """.trimIndent()
            }
            """
            <div class="table-wrap">
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 14%;">Year</th>
                            <th style="width: 16%;">Delivery mode</th>
                            <th style="width: 20%;">Outcome</th>
                            <th style="width: 50%;">Hospital and clinical notes</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rowsHtml
                    </tbody>
                </table>
            </div>
            """.trimIndent()
        }

        return """
        <section>
            <h2 class="section-heading">Previous pregnancies and obstetric outcomes</h2>
            $bodyHtml
        </section>
        """.trimIndent()
    }

    private fun renderVisitsSection(
        patient: DemoPatient,
        visits: Limited<com.sherifradwan.ancnative.model.DemoVisit>,
        figures: List<Figure>,
    ): String {
        val tbodyHtml = if (visits.rows.isEmpty()) {
            """<tr class="empty-prior-row"><td colspan="4">No clinical visits recorded yet</td></tr>"""
        } else {
            val rowsHtml = visits.rows.mapIndexed { idx, v ->
                val vGa = parseIsoDateOrNull(v.date)?.let { vDate ->
                    patient.dating?.calculateGestationalAgeResult(vDate)?.formatted
                } ?: "-"
                val bpText = v.bloodPressure?.trim()?.takeUnless(String::isBlank) ?: "-"
                val weightText = formatWeightKg(v.weight) ?: "-"
                val bpSpan = when (bpLevel(v.bloodPressure)) {
                    BpLevel.RAISED -> """<span class="col-bp bold-val abnormal-val nowrap">${escapeHtml(bpText)}</span>"""
                    BpLevel.SEVERE -> """<span class="col-bp bold-val abnormal-val nowrap">${escapeHtml(bpText)}</span>"""
                    BpLevel.NORMAL -> """<span class="col-bp nowrap">${escapeHtml(bpText)}</span>"""
                }
                val bpAlert = when (bpLevel(v.bloodPressure)) {
                    BpLevel.RAISED -> """<span class="bp-alert">$BP_ALERT_RAISED</span>"""
                    BpLevel.SEVERE -> """<span class="bp-alert bp-alert-severe">$BP_ALERT_SEVERE</span>"""
                    BpLevel.NORMAL -> ""
                }
                val linkedScan = latestScanForVisit(patient, v)
                val scanFigRefs = linkedScan?.let { figureRefsFor(it.id, figures) }.orEmpty()
                val visitFigRefs = figureRefsFor(v.id, figures)
                val combinedFigRefs = (scanFigRefs + visitFigRefs).distinct().sorted()
                val scanOutcomeHtml = renderScanOutcomeCell(linkedScan, combinedFigRefs, visitNote(v))

                """
                <tr>
                    <td><strong>#${idx + 1 + visits.hidden}</strong> · <span class="col-date nowrap">${escapeHtml(reportDate(v.date))}</span></td>
                    <td class="col-ga bold-val nowrap">${escapeHtml(vGa)}</td>
                    <td>$bpSpan · <span class="col-num nowrap">${escapeHtml(weightText)}</span>$bpAlert</td>
                    <td>$scanOutcomeHtml</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
            val hidden = hiddenRowsHtml(visits.hidden, 4, "earlier visits")
            if (hidden.isNotEmpty()) "$hidden\n$rowsHtml" else rowsHtml
        }

        return """
        <section>
            <h2 class="section-heading">Antenatal care visits log</h2>
            <div class="table-wrap">
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 20%;">Visit and date</th>
                            <th style="width: 12%;">GA</th>
                            <th style="width: 22%;">BP and weight</th>
                            <th style="width: 46%;">Scan outcome</th>
                        </tr>
                    </thead>
                    <tbody>
                        $tbodyHtml
                    </tbody>
                </table>
            </div>
        </section>
        """.trimIndent()
    }

    private fun renderUnlinkedScansSection(
        unlinked: List<DemoUltrasoundScan>,
        figures: List<Figure>,
    ): String {
        if (unlinked.isEmpty()) return ""
        val rowsHtml = unlinked.joinToString("\n") { scan ->
            val scanOutcomeHtml = renderScanOutcomeCell(scan, figureRefsFor(scan.id, figures))
            """
            <tr>
                <td class="col-date nowrap">${escapeHtml(reportDate(scan.date))}</td>
                <td class="col-ga bold-val nowrap">${escapeHtml(scanGestationalAge(scan))}</td>
                <td>$scanOutcomeHtml</td>
            </tr>
            """.trimIndent()
        }

        return """
        <section>
            <h2 class="section-heading">Other scans</h2>
            <div class="table-wrap">
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 20%;">Scan date</th>
                            <th style="width: 12%;">GA</th>
                            <th style="width: 68%;">Scan outcome</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rowsHtml
                    </tbody>
                </table>
            </div>
        </section>
        """.trimIndent()
    }

    private fun renderScanOutcomeCell(
        scan: DemoUltrasoundScan?,
        figRefs: List<Int>,
        visitNote: String? = null,
    ): String {
        val figHtml = if (figRefs.isNotEmpty()) {
            """<span class="fig-ref">${escapeHtml(figRefs.joinToString(", ") { "Fig $it" })}</span>"""
        } else {
            null
        }
        val outcomeLines = if (scan == null) {
            if (figHtml != null) listOf("""<span class="scan-primary">$figHtml</span>""") else emptyList()
        } else {
            val outcome = scanOutcomeFor(scan)
            val typePart = outcome.typeLabel?.let { "<strong>${escapeHtml(it)}</strong>" }
            val presPart = outcome.presentation?.let { escapeHtml(it) }
            val labelAndPres = listOfNotNull(typePart, presPart).joinToString(", ").ifEmpty { null }
            val primaryInner = listOfNotNull(labelAndPres, figHtml).joinToString(" · ")

            buildList {
                if (primaryInner.isNotEmpty()) {
                    add("""<span class="scan-primary">$primaryInner</span>""")
                }
                if (outcome.measurements.isNotEmpty()) {
                    val measurementsContent = outcome.measurements.joinToString(", ") { (label, value) ->
                        """<span class="u">${escapeHtml(label)} ${escapeHtml(value)}</span>"""
                    }
                    add("""<span class="scan-measurements">$measurementsContent</span>""")
                }
                if (outcome.extras.isNotEmpty()) {
                    add("""<span class="subtext">${escapeHtml(outcome.extras.joinToString(" · "))}</span>""")
                }
            }
        }
        val noteHtml = visitNote?.let { """<span class="subtext">Note: ${escapeHtml(it)}</span>""" }
        if (outcomeLines.isEmpty()) {
            return if (noteHtml != null) "-\n$noteHtml" else "-"
        }
        val allLines = if (noteHtml != null) outcomeLines + noteHtml else outcomeLines
        return allLines.joinToString("\n")
    }

    private fun renderLabsSection(report: LabReport): String {
        if (report.comparisons.isEmpty() && report.singles.isEmpty()) {
            return """
            <section>
                <h2 class="section-heading">Laboratory investigations</h2>
                <p class="empty-note">No laboratory test results recorded yet</p>
            </section>
            """.trimIndent()
        }

        val comparisonsHtml = report.comparisons.joinToString("\n") { table ->
            val dateCount = table.dates.size.coerceAtLeast(1)
            val testColPct = when (dateCount) {
                1 -> 60
                2 -> 50
                3 -> 40
                4 -> 32
                5 -> 25
                else -> 22
            }
            val dateColPct = (100 - testColPct) / dateCount
            val dateHeadersHtml = table.dates.joinToString("") { date ->
                """<th class="col-date" style="width: ${dateColPct}%;">${escapeHtml(date)}</th>"""
            }
            val bodyRowsHtml = table.rows.joinToString("\n") { row ->
                val cellsHtml = row.cells.joinToString("") { cell ->
                    if (cell == null) {
                        """<td class="col-num">-</td>"""
                    } else if (cell.entries.size <= 1) {
                        val entry = cell.entries.firstOrNull()
                        val value = entry?.value ?: cell.value
                        val flag = entry?.flag ?: cell.flag
                        val abnormalNoDirection = entry?.abnormalNoDirection ?: cell.abnormalNoDirection
                        """<td class="col-num">${renderLabEntryHtml(value, flag, abnormalNoDirection)}</td>"""
                    } else {
                        val entriesHtml = cell.entries.joinToString(" / ") { entry ->
                            renderLabEntryHtml(entry.value, entry.flag, entry.abnormalNoDirection)
                        }
                        """<td class="col-num lab-multi">$entriesHtml</td>"""
                    }
                }
                """
                <tr>
                    <td>${escapeHtml(row.label)}</td>
                    $cellsHtml
                </tr>
                """.trimIndent()
            }

            val tableNotesHtml = renderLabNotesHtml(table.notes)
            val tableHtml = """
            <h3 class="lab-panel-title">${escapeHtml(table.category)}</h3>
            <div class="table-wrap">
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: ${testColPct}%;">Test</th>
                            $dateHeadersHtml
                        </tr>
                    </thead>
                    <tbody>
                        $bodyRowsHtml
                    </tbody>
                </table>
            </div>
            """.trimIndent()
            if (tableNotesHtml.isNotEmpty()) "$tableHtml\n$tableNotesHtml" else tableHtml
        }

        val singlesHtml = if (report.singles.isEmpty()) {
            ""
        } else {
            val singleRowsHtml = report.singles.joinToString("\n") { single ->
                val resultWithUnit = if (!single.unit.isNullOrBlank()) {
                    "${single.result} ${single.unit}"
                } else {
                    single.result
                }
                val resultCell = if (single.flagMark != null) {
                    """<td><strong class="lab-abnormal">${escapeHtml(resultWithUnit)} ${escapeHtml(single.flagMark)}</strong></td>"""
                } else {
                    """<td>${escapeHtml(resultWithUnit)}</td>"""
                }
                val dateText = single.date.ifBlank { "-" }
                """
                <tr>
                    <td>${escapeHtml(single.test)}</td>
                    <td class="col-date">${escapeHtml(dateText)}</td>
                    $resultCell
                </tr>
                """.trimIndent()
            }
            val singleNotesHtml = renderLabNotesHtml(report.notes)
            val singlesTableHtml = """
            <h3 class="lab-panel-title">Single tests</h3>
            <div class="table-wrap">
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 40%;">Test</th>
                            <th style="width: 25%;">Date</th>
                            <th style="width: 35%;">Result</th>
                        </tr>
                    </thead>
                    <tbody>
                        $singleRowsHtml
                    </tbody>
                </table>
            </div>
            """.trimIndent()
            if (singleNotesHtml.isNotEmpty()) "$singlesTableHtml\n$singleNotesHtml" else singlesTableHtml
        }

        return """
        <section>
            <h2 class="section-heading">Laboratory investigations</h2>
            $comparisonsHtml
            $singlesHtml
            <div class="lab-footnote">H: above the usual range. L: below the usual range. *: abnormal.</div>
        </section>
        """.trimIndent()
    }

    private fun renderLabNotesHtml(notes: List<LabNote>): String {
        val validNotes = notes.filter { it.text.isNotBlank() }
        if (validNotes.isEmpty()) return ""
        val linesHtml = validNotes
            .sortedBy { labNoteSortKey(it.date) }
            .joinToString("\n") { note ->
                val datePart = note.date.trim()
                val lineText = buildString {
                    if (datePart.isNotEmpty()) {
                        append(escapeHtml(datePart)).append(", ")
                    }
                    append(escapeHtml(note.test.trim()))
                    append(": ")
                    append(escapeHtml(note.text.trim()))
                    note.labName?.trim()?.takeIf { it.isNotEmpty() }?.let { labName ->
                        append(" (").append(escapeHtml(labName)).append(")")
                    }
                }
                """    <div class="lab-note-line">$lineText</div>"""
            }
        return "<div class=\"lab-notes\">\n    <div class=\"lab-notes-title\">Notes</div>\n$linesHtml\n</div>"
    }

    private fun labNoteSortKey(date: String): LocalDate {
        val trimmed = date.trim()
        if (trimmed.isEmpty()) return LocalDate.MAX
        return parseIsoDateOrNull(trimmed)
            ?: try {
                LocalDate.parse(trimmed, LAB_NOTE_DMY_FORMATTER)
            } catch (_: Exception) {
                LocalDate.MAX
            }
    }

    private fun renderLabEntryHtml(
        value: String,
        flag: LabResultFlag?,
        abnormalNoDirection: Boolean,
    ): String {
        val flagMark = when {
            flag == LabResultFlag.HIGH -> "H"
            flag == LabResultFlag.LOW -> "L"
            abnormalNoDirection -> "*"
            else -> null
        }
        return if (flagMark != null) {
            """<strong class="lab-abnormal">${escapeHtml(value)} $flagMark</strong>"""
        } else {
            escapeHtml(value)
        }
    }

    private fun renderMedicationsSection(
        title: String,
        countText: String,
        meds: List<DemoMedicationRecord>,
        emptyMessage: String?,
    ): String {
        val bodyHtml = if (meds.isEmpty()) {
            """<p class="empty-note">${escapeHtml(emptyMessage ?: "None")}</p>"""
        } else {
            val itemsHtml = meds.joinToString("\n") { m ->
                val detail = medicationDetail(m)
                val dates = medicationDates(m)
                val parts = buildList {
                    add("""<span class="bold-val">${escapeHtml(m.name)}</span>""")
                    if (detail.isNotEmpty()) {
                        add("""<span class="med-line">${escapeHtml(detail)}</span>""")
                    }
                    if (dates.isNotEmpty()) {
                        add("""<span class="med-date">${escapeHtml(dates)}</span>""")
                    }
                }
                """<li>${parts.joinToString(" ")}</li>"""
            }
            """<ul class="med-grid">$itemsHtml</ul>"""
        }

        return """
        <section class="meds-compact">
            <h2 class="section-heading">${escapeHtml(title)} <span class="med-count">${escapeHtml(countText)}</span></h2>
            $bodyHtml
        </section>
        """.trimIndent()
    }

    private fun renderAttachedImagesSection(
        figures: List<Figure>,
        pictureDataUri: (PictureCandidate) -> String?,
    ): String {
        if (figures.isEmpty()) return ""
        val rowsHtml = figures.chunked(3).joinToString("\n") { rowFigures ->
            val cardsHtml = rowFigures.joinToString("\n") { figure ->
                val candidate = figure.candidate
                val dataUri = pictureDataUri(candidate)?.trim()?.takeIf { it.isNotEmpty() }
                val altText = buildString {
                    append("Fig ").append(figure.number).append(" - ").append(candidate.sourceLabel)
                    if (candidate.date.isNotBlank()) {
                        append(" (").append(candidate.date).append(")")
                    }
                }
                val imageBoxHtml = if (dataUri != null) {
                    """<img class="figure-image" src="${escapeHtml(dataUri)}" alt="${escapeHtml(altText)}" />"""
                } else {
                    """<div class="image-placeholder">Image not available</div>"""
                }
                """
                <figure class="figure-card">
                    $imageBoxHtml
                    <figcaption class="figure-caption">
                        <div class="figure-title"><strong>Fig ${figure.number}</strong></div>
                        <div class="figure-meta">Source: ${escapeHtml(candidate.sourceLabel)}</div>
                        <div class="figure-meta">Date: <span class="col-date">${escapeHtml(candidate.date.ifBlank { "-" })}</span></div>
                    </figcaption>
                </figure>
                """.trimIndent()
            }
            """
            <div class="figures-row">
                $cardsHtml
            </div>
            """.trimIndent()
        }

        return """
        <section class="attached-images">
            <h2 class="section-heading">Attached images</h2>
            <div class="figures-container">
                $rowsHtml
            </div>
            <div class="image-count-line">Pictures selected for this print: <span class="col-date">${figures.size}</span></div>
        </section>
        """.trimIndent()
    }

    private fun buildCss(bpAlertCss: String): String = """
        :root {
            --color-text-primary: #0f172a; --color-text-secondary: #475569;
            --color-rule: #cbd5e1; --color-rule-subtle: #e2e8f0; --color-zebra: #f8fafc;
            --color-alert-text: #b91c1c; --color-alert-bg: #fef2f2; --color-alert-border: #f87171;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        html, body {
            overflow-x: hidden; width: 100%; background-color: #ffffff; color: var(--color-text-primary);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            font-size: 9.5pt; line-height: 1.35; -webkit-print-color-adjust: exact; print-color-adjust: exact;
        }
        @media screen {
            body { background-color: #cbd5e1; padding: 12px 0; display: flex; flex-direction: column; align-items: center; }
            .page-flow { background: #ffffff; width: 210mm; max-width: 100%; box-sizing: border-box; padding: 12mm 14mm; box-shadow: 0 2px 10px rgba(15, 23, 42, 0.15); }
            .print-shell, .print-shell > tbody, .print-shell > tbody > tr, .print-shell > tbody > tr > td { display: block; width: 100%; }
            .print-shell > tfoot { display: none; }
        }
        @media screen and (max-width: 210mm) {
            body { padding: 6px 0; }
            .page-flow { width: 210mm; max-width: none; margin: 0; zoom: calc(100vw / 210mm); }
        }
        @media print {
            @page { size: A4 portrait; margin: 10mm 12mm; }
            html, body { background: #ffffff !important; color: #000000; margin: 0; padding: 0; width: 100%; overflow: visible; }
            .page-flow { width: 100% !important; margin: 0 !important; padding: 0 !important; box-shadow: none !important; zoom: 1 !important; }
            .print-shell { display: table; width: 100%; border-collapse: collapse; }
            .print-shell > tbody { display: table-row-group; }
            .print-shell > tfoot { display: table-footer-group; }
            .print-shell > tbody > tr, .print-shell > tfoot > tr { break-inside: auto; page-break-inside: auto; }
            .print-shell-cell { padding: 0; vertical-align: top; }
            .print-footer-space { height: 15mm; }
            thead { display: table-header-group; }
            table.clinical-table tr, .avoid-break, .figures-row, .figure-card, .med-grid li, .meds-compact { break-inside: avoid; page-break-inside: avoid; }
            .section-heading, .lab-panel-title { break-after: avoid; page-break-after: avoid; }
            .status-banner { background: #f1f5f9 !important; color: #000000 !important; border: 1.5px solid #000000 !important; }
            .data-val.data-val-alert { border-color: #000000 !important; color: #000000 !important; }
            .page-footer { position: fixed; bottom: 0; left: 0; right: 0; background: #ffffff; margin-top: 0; }
            table.clinical-table, table.clinical-table th, table.clinical-table td,
            .scan-measurements, .subtext, .empty-prior-row td, .bp-alert, .abnormal-val,
            .lab-abnormal, .lab-footnote, .lab-notes, .lab-notes-title, .figure-meta, .image-count-line, .med-line, .med-date, .page-footer { color: #000000 !important; }
            .image-placeholder { background-color: #f8fafc; border: 1px solid #94a3b8; color: #334155; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
        }
        .clinic-header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid var(--color-text-primary); padding-bottom: 4px; margin-bottom: 6px; }
        .clinic-brand { display: flex; align-items: center; gap: 12px; }
        .clinic-logo { width: 40px; height: 56px; object-fit: contain; flex-shrink: 0; }
        .clinic-mark { width: 40px; height: 40px; border: 1.5px solid var(--color-text-primary); display: flex; align-items: center; justify-content: center; font-size: 13pt; font-weight: 800; color: var(--color-text-primary); letter-spacing: 0.5px; flex-shrink: 0; }
        .clinic-title-box h1 { font-size: 13.5pt; font-weight: 800; color: var(--color-text-primary); line-height: 1.2; }
        .clinic-title-box .subhead { font-size: 9pt; font-weight: 600; color: var(--color-text-primary); }
        .clinic-title-box .specialty { font-size: 9pt; color: var(--color-text-secondary); }
        .doc-meta { text-align: right; font-size: 9pt; color: var(--color-text-secondary); display: flex; flex-direction: column; gap: 2px; flex-shrink: 0; }
        .doc-badge { font-size: 9pt; font-weight: 700; letter-spacing: 0.2px; color: var(--color-text-primary); border-bottom: 1px solid var(--color-rule); padding-bottom: 2px; margin-bottom: 2px; }
        .doc-meta strong { color: var(--color-text-primary); }
        .status-banner { background: #1e293b; color: #ffffff; border: 1px solid #1e293b; font-weight: 700; font-size: 9.5pt; padding: 4px 10px; border-radius: 3px; margin-bottom: 6px; }
        .patient-banner { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; border-bottom: 1px solid var(--color-rule); padding-bottom: 6px; margin-bottom: 8px; }
        .pt-identity { display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1; }
        .patient-name { font-size: 13pt; font-weight: 800; color: var(--color-text-primary); word-break: keep-all; overflow-wrap: break-word; line-height: 1.2; }
        .pt-meta-inline { font-size: 9pt; color: var(--color-text-secondary); display: flex; flex-wrap: wrap; gap: 2px 14px; }
        .pt-meta-inline strong { color: var(--color-text-primary); }
        .pt-vitals-strip { display: flex; gap: 16px; align-items: flex-end; flex-shrink: 0; }
        .vital-cell { text-align: right; }
        .vital-label { font-size: 9pt; color: var(--color-text-secondary); display: block; margin-bottom: 1px; }
        .vital-value { font-size: 9.5pt; font-weight: 700; color: var(--color-text-primary); }
        .risk-pill { display: inline-block; font-size: 9pt; font-weight: 700; padding: 1px 6px; border-radius: 2px; white-space: nowrap; }
        .risk-high { color: var(--color-alert-text); border: 1px solid var(--color-alert-border); background-color: var(--color-alert-bg); }
        .risk-medium { color: #92400e; border: 1px solid #f59e0b; background-color: #fef3c7; }
        .risk-low { color: #166534; border: 1px solid #86efac; background-color: #dcfce7; }
        .risk-unrecorded { color: var(--color-text-secondary); border: 1px solid var(--color-rule); background-color: var(--color-zebra); }
        .attention-box { background: #fff7ed; border: 1px solid #c2410c; color: #7c2d12; font-size: 9pt; padding: 5px 10px; border-radius: 3px; margin-bottom: 8px; display: flex; gap: 8px; }
        .attention-box .k { font-weight: 700; white-space: nowrap; }
        .two-column-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 18px; }
        .section-heading { font-size: 10.5pt; font-weight: 700; color: var(--color-text-primary); border-bottom: 1px solid var(--color-rule); padding-bottom: 2px; margin-top: 6px; margin-bottom: 4px; display: flex; justify-content: space-between; align-items: baseline; }
        .section-heading .extra { font-size: 9pt; color: var(--color-text-secondary); font-weight: normal; }
        .data-list { display: flex; flex-direction: column; gap: 3px; }
        .data-row { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; font-size: 9pt; border-bottom: 1px solid var(--color-rule-subtle); padding-bottom: 2px; }
        .data-key { color: var(--color-text-secondary); font-weight: 500; flex-shrink: 0; }
        .data-val { color: var(--color-text-primary); font-weight: 600; text-align: right; overflow-wrap: anywhere; }
        .data-val-full { text-align: left; width: 100%; }
        .data-val-alert { color: var(--color-alert-text); font-weight: 700; }
        .data-val.data-val-alert { background-color: var(--color-alert-bg); border: 1px solid var(--color-alert-border); border-radius: 2px; padding: 0 5px; }
        .ga-banner { margin-top: 4px; padding: 3px 0; font-size: 9pt; font-weight: 700; color: var(--color-text-primary); display: flex; justify-content: space-between; border-top: 1px solid var(--color-rule); }
        .table-wrap { width: 100%; margin-bottom: 6px; }
        table.clinical-table { width: 100%; table-layout: fixed; border-collapse: collapse; font-size: 9pt; line-height: 1.25; }
        table.clinical-table thead { display: table-header-group; }
        table.clinical-table tr { break-inside: avoid; page-break-inside: avoid; }
        table.clinical-table th { font-size: 9pt; font-weight: 700; color: var(--color-text-primary); border-bottom: 1.5px solid var(--color-text-primary); padding: 2.5px 5px; text-align: left; background: #ffffff; word-break: normal; overflow-wrap: normal; hyphens: none; }
        table.clinical-table td { font-size: 9pt; padding: 2px 5px; border-bottom: 1px solid var(--color-rule-subtle); color: var(--color-text-primary); vertical-align: top; overflow-wrap: anywhere; word-break: normal; line-height: 1.22; }
        table.clinical-table tbody tr:nth-child(even) { background-color: var(--color-zebra); }
        .nowrap, .col-date, .col-ga, .col-bp, .col-num { white-space: nowrap; overflow-wrap: normal; }
        .col-date, .col-num { font-variant-numeric: tabular-nums; }
        table.clinical-table th.col-date, table.clinical-table td.col-num { text-align: right; }
        table.clinical-table td.col-num.lab-multi { white-space: normal; overflow-wrap: anywhere; }
        table.clinical-table td.col-date, table.clinical-table td.col-ga { text-align: left; }
        .bold-val { font-weight: 700; }
        .scan-primary { display: block; line-height: 1.3; }
        .scan-measurements { display: block; font-size: 9pt; color: var(--color-text-secondary); line-height: 1.2; margin-top: 0; overflow-wrap: normal; }
        .scan-measurements .u { white-space: nowrap; }
        .subtext { font-size: 9pt; color: var(--color-text-secondary); display: block; margin-top: 0; }
        .abnormal-val { font-weight: 700; color: var(--color-alert-text); }$bpAlertCss
        .empty-prior-row td { text-align: center; font-style: italic; color: var(--color-text-secondary); font-size: 9pt; padding: 4px; background-color: var(--color-zebra); }
        .empty-note { font-size: 9pt; font-style: italic; color: var(--color-text-secondary); text-align: center; padding: 6px; }
        .fig-ref { font-weight: 600; white-space: nowrap; }
        .lab-panel-title { font-size: 9.5pt; font-weight: 700; color: var(--color-text-primary); margin-top: 6px; margin-bottom: 3px; }
        .lab-abnormal { font-weight: 700; color: var(--color-alert-text); }
        .lab-footnote { font-size: 9pt; color: var(--color-text-secondary); margin-top: 4px; line-height: 1.35; }
        .lab-notes { font-size: 9pt; color: var(--color-text-secondary); margin: 0 0 6px 0; line-height: 1.3; }
        .lab-notes-title { font-weight: 700; color: var(--color-text-primary); }
        .lab-note-line { display: block; overflow-wrap: anywhere; }
        .med-grid { list-style: none; display: grid; grid-template-columns: 1fr 1fr; column-gap: 14px; margin: 0; padding: 0; }
        .med-grid li { font-size: 9pt; line-height: 1.3; padding: 1.5px 2px; border-bottom: 1px solid var(--color-rule-subtle); break-inside: avoid; page-break-inside: avoid; overflow-wrap: anywhere; }
        .med-line { color: #334155; }
        .med-date { white-space: nowrap; color: #334155; }
        .med-count { font-weight: 400; }
        .meds-compact { break-inside: avoid; page-break-inside: avoid; }
        .attestation-wrapper { display: flex; flex-direction: column; gap: 2px; padding-top: 2px; }
        .doc-name { font-size: 10pt; font-weight: 700; color: var(--color-text-primary); }
        .doc-ar-name { font-size: 12pt; font-weight: 700; color: var(--color-text-primary); direction: rtl; text-align: right; line-height: 1.3; }
        .doc-ar-title { font-size: 9pt; color: var(--color-text-secondary); direction: rtl; text-align: right; line-height: 1.3; }
        .stamp-box { margin-top: 6px; border: 1px dashed var(--color-rule); height: 38px; display: flex; align-items: center; justify-content: center; color: var(--color-text-secondary); font-size: 9pt; text-transform: uppercase; letter-spacing: 0.5px; }
        .figures-container { display: flex; flex-direction: column; gap: 12px; margin-top: 8px; margin-bottom: 8px; }
        .figures-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; break-inside: avoid; page-break-inside: avoid; }
        .figure-card { margin: 0; break-inside: avoid; page-break-inside: avoid; display: flex; flex-direction: column; }
        .image-placeholder { width: 100%; aspect-ratio: 4 / 3; background-color: #f1f5f9; border: 1px solid var(--color-rule); display: flex; align-items: center; justify-content: center; text-align: center; padding: 8px; font-size: 10pt; font-weight: 700; color: var(--color-text-secondary); box-sizing: border-box; }
        .figure-image { width: 100%; aspect-ratio: 4 / 3; object-fit: contain; display: block; border: 1px solid var(--color-rule); background-color: #f8fafc; box-sizing: border-box; }
        .figure-caption { padding-top: 5px; font-size: 9pt; line-height: 1.35; color: var(--color-text-primary); }
        .figure-title { font-size: 9.5pt; font-weight: 700; color: var(--color-text-primary); }
        .figure-meta { font-size: 9pt; color: var(--color-text-secondary); }
        .image-count-line { font-size: 9pt; font-weight: 600; color: var(--color-text-secondary); margin-top: 10px; padding-top: 6px; border-top: 1px solid var(--color-rule-subtle); }
        .page-footer { border-top: 1px solid var(--color-rule); padding-top: 4px; margin-top: 10px; display: flex; flex-wrap: wrap; justify-content: space-between; align-items: baseline; gap: 2px 12px; font-size: 9pt; color: var(--color-text-secondary); }
    """.trimIndent()
}
