package com.sherifradwan.ancnative.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.PreviousPregnancyFormRules
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.bpLevel
import com.sherifradwan.ancnative.model.datingDiscordanceLine
import com.sherifradwan.ancnative.model.datingSummaryOf
import com.sherifradwan.ancnative.model.datingTextRows
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.model.isEarlyLoss
import com.sherifradwan.ancnative.model.isStillbirth
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import com.sherifradwan.ancnative.model.deliveryOutcomeLine
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.ui.AllergyState
import com.sherifradwan.ancnative.ui.classifyAllergy
import java.io.ByteArrayOutputStream
import java.time.LocalDate

object PatientPrintHtmlGenerator {

    private const val MAX_PRINT_VISITS = 12
    private const val MAX_PRINT_SCANS = 7
    private const val MAX_PRINT_LABS = 10
    private const val CLINIC_PHONE = "01201720808"
    private const val LOGO_PRINT_HEIGHT_PX = 360

    private const val BP_ALERT_RAISED = "Raised BP - recheck"
    private const val BP_ALERT_SEVERE = "Severe-range BP - urgent assessment"

    private fun hiddenRowsHtml(hidden: Int, colspan: Int, what: String): String {
        if (hidden <= 0) return ""
        return """<tr class="more-row"><td colspan="$colspan" class="text-center empty-cell">$hidden $what not shown</td></tr>"""
    }

    private fun getLogoBase64(context: Context?): String {
        if (context == null) return ""
        return try {
            val full = BitmapFactory.decodeResource(context.resources, R.drawable.clinic_logo_mark)
            // The page embeds the logo three times, so it is scaled down once here.
            val height = LOGO_PRINT_HEIGHT_PX
            val width = (full.width * height.toFloat() / full.height).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createScaledBitmap(full, width, height, true)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    fun generateHtml(
        context: Context? = null,
        patient: DemoPatient,
        settings: SecuritySettingsState? = null,
        asOfDate: LocalDate = LocalDate.now(),
    ): String {
        val logoBase64 = getLogoBase64(context)
        val clinicName = escapeHtml(settings?.clinicName?.takeUnless(String::isBlank) ?: "Dr. Sherif Radwan Clinic")
        val clinicSpecialty = "Maternal-Fetal Medicine & Advanced Obstetrics"
        val doctorOfficialTitle = "Specialist of Obstetrics, Gynecology & Laparoscopy"
        val printDateStr = formatDmy(asOfDate)
        val currentGa: String? = reportGestationalAge(patient, asOfDate)
        val statusLine: String? = reportStatusLine(patient)
        val datingSummary = datingSummaryOf(patient, asOfDate)
        val datingMatrixHtml = buildString {
            datingTextRows(datingSummary).forEach { row ->
                val valueClass = if (row.isEdd) "val text-rose" else "val"
                append("""<div class="matrix-item"><div class="label">${escapeHtml(row.label)}</div>""")
                append("""<div class="$valueClass">${escapeHtml(row.value ?: "Not recorded")}</div></div>""")
            }
            datingDiscordanceLine(patient.dating, datingSummary)?.let {
                append("""<div class="matrix-item" style="grid-column: 1 / -1"><div class="val">${escapeHtml(it)}</div></div>""")
            }
        }

        // Format Obstetric summary
        val g = patient.gravida ?: "-"
        val p = patient.parity ?: "-"
        val a = patient.abortion ?: "-"
        val pastDeliverySummary = previousDeliverySummary(patient)

        val risk = reportRisk(patient.riskLevel)
        val isRhNegative = com.sherifradwan.ancnative.ui.isRhNegative(patient.bloodGroupText)

        // Visits table rows (Centered text, No SFH, No FHR, Spacious and comfortable)
        val visits = reportVisits(patient, MAX_PRINT_VISITS)
        val bpAlertCss = when {
            visits.rows.any { bpLevel(it.bloodPressure) == BpLevel.SEVERE } ->
                "\n        .bp-alert { font-size: 6.8pt; line-height: 1.1; margin-top: 2px; color: #b45309; white-space: normal; }\n        .bp-alert-severe { color: #dc2626; }"
            visits.rows.any { bpLevel(it.bloodPressure) == BpLevel.RAISED } ->
                "\n        .bp-alert { font-size: 6.8pt; line-height: 1.1; margin-top: 2px; color: #b45309; white-space: normal; }"
            else -> ""
        }
        val visitsRowsHtml = if (visits.rows.isEmpty()) {
            """<tr><td colspan="6" class="text-center empty-cell">No clinical visits recorded yet</td></tr>"""
        } else {
            val rows = visits.rows.mapIndexed { idx, v ->
                val vGa = parseIsoDateOrNull(v.date)?.let { vDate ->
                    patient.dating?.calculateGestationalAgeResult(vDate)?.formatted
                } ?: "-"
                val bpAlert = when (bpLevel(v.bloodPressure)) {
                    BpLevel.RAISED -> """<div class="bp-alert">$BP_ALERT_RAISED</div>"""
                    BpLevel.SEVERE -> """<div class="bp-alert bp-alert-severe">$BP_ALERT_SEVERE</div>"""
                    BpLevel.NORMAL -> ""
                }
                """
                <tr>
                    <td class="text-center font-bold nowrap-cell">${idx + 1 + visits.hidden}</td>
                    <td class="text-center date-cell">${escapeHtml(reportDate(v.date))}</td>
                    <td class="text-center font-bold text-teal nowrap-cell">${vGa}</td>
                    <td class="text-center nowrap-cell font-bold text-navy">${escapeHtml(v.bloodPressure ?: "-")}$bpAlert</td>
                    <td class="text-center nowrap-cell">${escapeHtml(v.weight?.let { "$it kg" } ?: "-")}</td>
                    <td class="text-center notes-cell">${escapeHtml(visitText(v))}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
            val hidden = hiddenRowsHtml(visits.hidden, 6, "earlier visits")
            if (hidden.isNotEmpty()) "$hidden\n$rows" else rows
        }

        // Ultrasound Scans rows (Centered, EFW (g), Clinical Notes, image badge if photos exist)
        val scans = reportScans(patient, MAX_PRINT_SCANS)
        val scansRowsHtml = if (scans.rows.isEmpty()) {
            """<tr><td colspan="7" class="text-center empty-cell">No ultrasound scans recorded yet</td></tr>"""
        } else {
            val rows = scans.rows.map { s ->
                val photoBadge = if (s.imageUris.isNotEmpty()) {
                    """<div class="photo-badge">${s.imageUris.size} photos in the clinic record</div>"""
                } else {
                    ""
                }

                """
                <tr>
                    <td class="text-center date-cell">${escapeHtml(reportDate(s.date))}</td>
                    <td class="text-center font-bold text-teal nowrap-cell">${scanGestationalAge(s)}</td>
                    <td class="text-center">${escapeHtml(s.presentation?.takeUnless(String::isBlank) ?: "Not recorded")}</td>
                    <td class="text-center">${escapeHtml(s.placentalSite?.takeUnless(String::isBlank) ?: "Not recorded")}</td>
                    <td class="text-center">${escapeHtml(s.amnioticFluid?.takeUnless(String::isBlank) ?: "Not recorded")}</td>
                    <td class="text-center nowrap-cell font-bold text-navy">${escapeHtml(scanWeight(s))}</td>
                    <td class="text-center notes-cell">
                        ${escapeHtml(scanNotes(s))}
                        $photoBadge
                    </td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
            val hidden = hiddenRowsHtml(scans.hidden, 7, "earlier scans")
            if (hidden.isNotEmpty()) "$hidden\n$rows" else rows
        }

        // Labs rows (Centered, NO status column, Abnormal result displayed in RED)
        val labs = reportLabs(patient, MAX_PRINT_LABS)
        val labsRowsHtml = if (labs.rows.isEmpty()) {
            """<tr><td colspan="4" class="text-center empty-cell">No laboratory test results recorded yet</td></tr>"""
        } else {
            val rows = labs.rows.map { l ->
                val resultText = escapeHtml(labResult(l))
                val resultHtml = if (l.isAbnormal) {
                    """<span class="val-abnormal">$resultText (Abnormal)</span>"""
                } else {
                    """<span class="font-bold text-navy">$resultText</span>"""
                }
                val extraHtml = labExtra(l).takeUnless(String::isBlank)?.let { extra ->
                    """<br><span class="text-muted med-instructions">${escapeHtml(extra)}</span>"""
                } ?: ""
                """
                <tr>
                    <td class="text-center date-cell">${escapeHtml(reportDate(l.date))}</td>
                    <td class="text-center font-bold text-navy">${escapeHtml(l.testName)}$extraHtml</td>
                    <td class="text-center">${resultHtml}</td>
                    <td class="text-center text-muted">${escapeHtml(l.referenceRange ?: "-")}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
            val hiddenRow = hiddenRowsHtml(labs.hidden, 4, "older results")
            if (hiddenRow.isNotEmpty()) "$rows\n$hiddenRow" else rows
        }

        // Medications rows (Centered)
        val meds = reportMedications(patient)
        fun medRows(list: List<DemoMedicationRecord>): String = list.joinToString("\n") { m ->
            val detail = medicationDetail(m)
            val detailHtml = if (detail.isEmpty()) "" else """<br><span class="text-muted med-instructions">${escapeHtml(detail)}</span>"""
            """
                <tr>
                    <td class="text-center med-cell"><span class="font-bold text-navy">${escapeHtml(m.name)}</span>$detailHtml</td>
                    <td class="text-center">${escapeHtml(medicationDates(m))}</td>
                </tr>
            """.trimIndent()
        }
        val currentMedsRowsHtml = medRows(meds.current).ifEmpty {
            """<tr><td colspan="2" class="text-center empty-cell">No current medications recorded</td></tr>"""
        }
        val stoppedMedsRowsHtml: String = medRows(meds.stopped)

        // Previous Pregnancies (Centered)
        val prevPregRows = if (patient.previousPregnancies.isEmpty()) {
            """<p class="text-muted text-center text-small" style="padding: 6px;">No previous pregnancies or deliveries recorded (${escapeHtml(pastDeliverySummary)}).</p>"""
        } else {
            """
            <div class="table-scroll">
            <table class="clinical-table mini-table">
                <thead>
                    <tr>
                        <th style="width: 14%; text-align: center;">Year / Date</th>
                        <th style="width: 26%; text-align: center;">Type & Delivery Mode</th>
                        <th style="width: 25%; text-align: center;">Outcome & Status</th>
                        <th style="width: 35%; text-align: center;">Hospital & Clinical Notes</th>
                    </tr>
                </thead>
                <tbody>
                    ${patient.previousPregnancies.map { pp ->
                        val outcomeLabel = when (pp.outcome) {
                            "Stillbirth" -> pp.outcomeText ?: "Stillbirth / IUFD"
                            "EarlyPregnancyLoss" -> pp.outcomeText ?: "Miscarriage"
                            "TerminationOfPregnancy" -> pp.outcomeText ?: "Therapeutic Termination"
                            "LiveBirth" -> pp.outcomeText ?: "Live Birth"
                            else -> pp.outcomeText ?: pp.outcome ?: "-"
                        }
                        val isLoss = pp.outcome == "TerminationOfPregnancy" ||
                            isStillbirth(pp) ||
                            isEarlyLoss(pp)
                        val outcomeBadge = if (isLoss) {
                            """<span class="badge" style="background:#fee2e2;color:#991b1b;border:1px solid #f87171;font-weight:bold;padding:2px 6px;border-radius:4px;">${escapeHtml(outcomeLabel)}</span>"""
                        } else {
                            """<span style="font-weight:600;color:#0f766e;">${escapeHtml(outcomeLabel)}</span>"""
                        }
                        val methodText = pp.birthMethod ?: pp.deliveryBy ?: pp.lossManagement ?: pp.terminationMethod ?: "-"
                        """
                        <tr>
                            <td class="text-center date-cell">${escapeHtml(pp.dateText ?: "-")}</td>
                            <td class="text-center font-bold">${escapeHtml(listOfNotNull(pp.pregnancyType?.takeUnless(String::isBlank), methodText.takeUnless { it == "-" }).joinToString(" · ").ifEmpty { "-" })}</td>
                            <td class="text-center">${outcomeBadge}</td>
                            <td class="text-center">${escapeHtml(listOfNotNull(pp.placeAndDoctor?.takeUnless(String::isBlank), pp.otherDetails?.takeUnless(String::isBlank)).joinToString(" · ").ifEmpty { "Not recorded" })}</td>
                        </tr>
                        """.trimIndent()
                    }.joinToString("\n")}
                </tbody>
            </table>
            </div>
            """.trimIndent()
        }

        val logoImgTag = if (logoBase64.isNotEmpty()) {
            """<img src="data:image/png;base64,$logoBase64" alt="Clinic Logo" class="clinic-logo" />"""
        } else {
            """<div class="logo-fallback">DR</div>"""
        }

        val watermarkImg = if (logoBase64.isNotEmpty()) {
            """background-image: url('data:image/png;base64,$logoBase64');"""
        } else {
            ""
        }

        val ageFormatted = patient.ageText?.trim()?.takeIf(String::isNotEmpty)?.let { ageText -> com.sherifradwan.ancnative.ui.formatAgeYears(ageText)?.let { "$it yrs" } ?: ageText } ?: "-"

        return """
<!DOCTYPE html>
<html lang="en" dir="ltr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Clinical Record - ${escapeHtml(patient.displayName)} - $clinicName</title>
    <style>
        @font-face { font-family: 'Arslan Wessam B'; src: url('fonts/ArslanWessamB.ttf'); }
        :root {
            --navy-primary: #0D1B3D;
            --navy-dark: #070F24;
            --rose-accent: #B34A60;
            --teal-clinical: #176B70;
            --bg-ivory: #FBF8F5;
            --surface-white: #FFFFFF;
            --border-light: #CBD5E1;
            --border-hairline: #E2E8F0;
            --text-main: #0F172A;
            --text-muted: #475569;
            --danger-bg: #FEE2E2;
            --danger-text: #DC2626;
            --success-bg: #DCFCE7;
            --success-text: #166534;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background-color: #E2E8F0;
            color: var(--text-main);
            font-size: 10.5pt;
            line-height: 1.4;
            direction: ltr;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
        }

        /* Base page-sheet layout */
        .page-sheet {
            background: var(--surface-white);
            position: relative;
            box-sizing: border-box;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
        }

        /* Subtle Authentic Watermark */
        .watermark {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 380px;
            height: 380px;
            opacity: 0.045;
            pointer-events: none;
            z-index: 0;
            background-position: center;
            background-repeat: no-repeat;
            background-size: contain;
            $watermarkImg
        }

        .page-content {
            position: relative;
            z-index: 1;
            display: flex;
            flex-direction: column;
            gap: 9px;
            flex-grow: 1;
        }

        /* On-Screen Mobile Preview (In-App WebView) */
        @media screen {
            body {
                background-color: #E2E8F0;
                padding: 10px 8px;
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 14px;
            }
            .page-sheet {
                width: 100%;
                max-width: 210mm;
                min-height: auto;
                max-height: none;
                padding: 12px 14px;
                border-radius: 6px;
                box-shadow: 0 4px 14px rgba(13, 27, 61, 0.12);
                margin: 0 auto;
                overflow: visible;
            }
        }

        @media screen and (max-width: 540px) {
            .patient-card {
                grid-template-columns: 1fr 1fr !important;
                gap: 8px !important;
            }
            .grid-two-col {
                grid-template-columns: 1fr !important;
            }
            .dating-matrix {
                grid-template-columns: 1fr 1fr 1fr !important;
            }
            .attestation-grid {
                grid-template-columns: 1fr !important;
            }
            .clinic-header {
                flex-direction: column !important;
                align-items: flex-start !important;
                gap: 8px !important;
            }
            .uniform-meta {
                border-left: none !important;
                padding-left: 0 !important;
                width: 100% !important;
            }
            .table-scroll {
                overflow-x: auto;
                -webkit-overflow-scrolling: touch;
            }
            .clinical-table {
                min-width: 600px;
            }
        }

        /* Pixel-Perfect ISO A4 Print / PDF Layout */
        @media print {
            body {
                background: transparent;
                margin: 0;
                padding: 0;
            }
            .page-sheet {
                margin: 0;
                width: 210mm;
                height: 297mm;
                min-height: 297mm;
                max-height: 297mm;
                box-sizing: border-box;
                box-shadow: none;
                page-break-after: always;
                break-after: page;
                padding: 8mm 12mm 6mm 12mm;
                border-radius: 0;
                overflow: hidden;
            }
            .page-sheet:last-child {
                page-break-after: avoid;
                break-after: avoid;
            }
            .patient-card {
                grid-template-columns: 2.2fr 1.1fr 1.2fr 1.4fr !important;
            }
            .grid-two-col {
                grid-template-columns: 1fr 1fr !important;
            }
            .clinic-header {
                flex-direction: row !important;
                align-items: center !important;
            }
            .uniform-meta {
                border-left: 2px solid var(--border-hairline) !important;
                padding-left: 12px !important;
                border-right: none !important;
                padding-right: 0 !important;
                width: auto !important;
            }
            .attestation-grid {
                grid-template-columns: 1.8fr 1.2fr !important;
            }
        }

        /* Clinic Top Header */
        .clinic-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 2px solid var(--navy-primary);
            padding-bottom: 7px;
            margin-bottom: 3px;
        }

        .clinic-brand {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .clinic-logo {
            width: 46px;
            height: 64px;
            object-fit: contain;
        }

        .clinic-title-box h1 {
            color: var(--navy-primary);
            font-size: 14.5pt;
            font-weight: 800;
            line-height: 1.2;
            margin-bottom: 1px;
        }

        .clinic-title-box h2 {
            color: var(--rose-accent);
            font-size: 9.5pt;
            font-weight: 700;
            letter-spacing: 0.2px;
            margin-bottom: 1px;
        }

        .clinic-title-box p {
            color: var(--teal-clinical);
            font-size: 8.5pt;
            font-weight: 600;
        }

        /* Header Meta: Perfectly uniform alignment */
        .uniform-meta {
            text-align: left;
            direction: ltr;
            display: flex;
            flex-direction: column;
            gap: 2px;
            min-width: 175px;
            border-left: 2px solid var(--border-hairline);
            padding-left: 12px;
        }

        .uniform-meta .doc-badge {
            background: var(--navy-primary);
            color: #fff;
            padding: 2px 8px;
            font-size: 8pt;
            font-weight: 700;
            border-radius: 4px;
            display: inline-block;
            text-align: center;
            margin-bottom: 2px;
            letter-spacing: 0.3px;
        }

        .uniform-meta .meta-row {
            display: flex;
            justify-content: space-between;
            font-size: 8pt;
            line-height: 1.25;
            color: var(--text-muted);
        }

        .uniform-meta .meta-row .k {
            font-weight: 600;
            color: var(--text-muted);
        }

        .uniform-meta .meta-row .v {
            font-weight: 700;
            color: var(--navy-primary);
        }

        /* Identity Banner: Name, Case ID, Phone on new line, Age only */
        .patient-card {
            background: #F8FAFC;
            border: 1.5px solid var(--border-light);
            border-radius: 6px;
            padding: 8px 12px;
            display: grid;
            grid-template-columns: 2.2fr 1.1fr 1.2fr 1.4fr;
            gap: 10px;
            align-items: center;
        }

        .pt-name-box h3 {
            font-size: 13pt;
            color: var(--navy-primary);
            font-weight: 800;
            line-height: 1.2;
        }

        .pt-name-box .pt-code {
            font-size: 8.5pt;
            color: var(--text-muted);
            font-weight: 600;
            margin-top: 1px;
        }

        .pt-name-box .pt-phone {
            font-size: 8.5pt;
            color: var(--teal-clinical);
            font-weight: 700;
            margin-top: 2px;
        }

        .data-point {
            display: flex;
            flex-direction: column;
        }

        .data-label {
            font-size: 7.5pt;
            color: var(--text-muted);
            text-transform: uppercase;
            font-weight: 600;
            margin-bottom: 2px;
        }

        .data-value {
            font-size: 10pt;
            color: var(--navy-primary);
            font-weight: 800;
        }

        /* Badges */
        .badge {
            display: inline-block;
            padding: 3px 8px;
            border-radius: 4px;
            font-size: 8pt;
            font-weight: 700;
            text-align: center;
        }

        .badge-danger {
            background-color: var(--danger-bg);
            color: var(--danger-text);
            border: 1px solid #FCA5A5;
        }

        .badge-success {
            background-color: var(--success-bg);
            color: var(--success-text);
            border: 1px solid #86EFAC;
        }

        .badge-teal {
            background-color: #E6FFFA;
            color: var(--teal-clinical);
            border: 1px solid #B2F5EA;
        }

        .badge-navy {
            background-color: #EFF6FF;
            color: var(--navy-primary);
            border: 1px solid #BFDBFE;
        }

        .badge-warning { background: #fef3c7; color: #92400e; border: 1px solid #f59e0b; }

        .status-banner { background: #1e293b; color: #ffffff; font-weight: 700; font-size: 10pt; padding: 5px 10px; border-radius: 4px; margin-bottom: 6px; }

        .attention-box { background: #fff7ed; border: 1px solid #c2410c; color: #7c2d12; font-size: 9pt; padding: 5px 10px; border-radius: 4px; margin-bottom: 6px; display: flex; gap: 8px; }

        .attention-box .k { font-weight: 700; white-space: nowrap; }

        .doc-sign { font-family: 'Arslan Wessam B', 'Noto Naskh Arabic', serif; font-size: 20pt; line-height: 1.3; color: #1e293b; }
        .doc-sign-title { font-size: 11pt; line-height: 1.4; }
        .med-cell { overflow-wrap: anywhere; word-break: break-word; }

        .val-abnormal {
            color: var(--danger-text);
            font-weight: 800;
            background-color: #FEF2F2;
            padding: 2px 6px;
            border-radius: 3px;
            display: inline-block;
        }

        /* Grid sections */
        .grid-two-col {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
        }

        .section-box {
            border: 1px solid var(--border-hairline);
            border-radius: 5px;
            background: #fff;
            padding: 8px 10px;
        }

        .section-title {
            font-size: 9.5pt;
            color: var(--navy-primary);
            font-weight: 800;
            border-bottom: 1.5px solid #F1F5F9;
            padding-bottom: 4px;
            margin-bottom: 6px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .dating-matrix {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 6px;
            margin-bottom: 6px;
        }

        .matrix-item {
            background: #F8FAFC;
            padding: 5px;
            border-radius: 4px;
            text-align: center;
        }

        .matrix-item .label {
            font-size: 7.5pt;
            color: var(--text-muted);
        }

        .matrix-item .val {
            font-size: 9.5pt;
            font-weight: 800;
            color: var(--navy-primary);
        }

        .ga-highlight {
            background: #FDF2F4;
            border: 1px solid #FBCFE8;
            padding: 5px;
            border-radius: 4px;
            text-align: center;
            font-size: 9.5pt;
            font-weight: 800;
            color: var(--rose-accent);
        }

        /* History lines with Arabic on right, English/values on left */
        .history-list {
            font-size: 8.5pt;
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .history-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px dashed #F1F5F9;
            padding-bottom: 3px;
        }

        .history-row .k {
            color: var(--text-muted);
            font-weight: 600;
            text-align: left;
        }

        .history-row .v {
            color: var(--navy-primary);
            font-weight: 700;
            text-align: right;
            direction: ltr;
        }

        /* Clinical Tables: Perfectly Centered Contents, Zero-Overflow Fixed Layout */
        .clinical-table {
            width: 100%;
            table-layout: fixed;
            border-collapse: collapse;
            font-size: 8pt;
            margin-top: 4px;
            box-sizing: border-box;
        }

        .clinical-table th {
            background: #F1F5F9;
            color: var(--navy-primary);
            font-weight: 700;
            padding: 5px 4px;
            border: 1px solid var(--border-light);
            text-align: center !important;
            font-size: 7.5pt;
            box-sizing: border-box;
            word-break: normal;
            overflow-wrap: normal;
            hyphens: none;
        }

        .clinical-table td {
            padding: 5px 4px;
            border: 1px solid var(--border-hairline);
            color: var(--text-main);
            text-align: center !important;
            vertical-align: middle;
            box-sizing: border-box;
            word-break: normal;
            overflow-wrap: normal;
            hyphens: none;
        }

        .clinical-table tr:nth-child(even) {
            background-color: #FAFAFA;
        }

        .mini-table th, .mini-table td {
            padding: 3px 4px;
            font-size: 7.5pt;
            word-break: normal;
            overflow-wrap: normal;
            hyphens: none;
        }

        .text-center { text-align: center !important; }
        .font-bold { font-weight: 700; }
        .text-navy { color: var(--navy-primary); }
        .text-rose { color: var(--rose-accent); }
        .text-teal { color: var(--teal-clinical); }
        .text-muted { color: var(--text-muted); }
        .empty-cell { padding: 12px; color: var(--text-muted); font-style: italic; }
        .date-cell { font-family: monospace; font-size: 7.8pt; white-space: nowrap; }
        .nowrap-cell { white-space: nowrap; }
        .clinical-table .more-row td {
            text-align: left !important;
            padding-left: 8px;
        }
        .clinical-table.mini-table td:nth-child(4),
        .mini-table td:nth-child(4),
        .clinical-table td.notes-cell,
        .notes-cell {
            font-size: 7.2pt;
            line-height: 1.25;
            word-break: normal;
            overflow-wrap: anywhere;
            max-width: 100%;
        }$bpAlertCss

        .photo-badge {
            display: inline-block;
            margin-top: 3px;
            background: #E0F2FE;
            color: #0369A1;
            font-size: 7pt;
            font-weight: 700;
            padding: 1px 5px;
            border-radius: 3px;
        }

        /* Signature block on Page 2 */
        .attestation-grid {
            display: grid;
            grid-template-columns: 1.8fr 1.2fr;
            gap: 12px;
            margin-top: 6px;
        }

        .plan-box {
            background: #F8FAFC;
            border: 1px solid var(--border-light);
            border-radius: 5px;
            padding: 8px;
        }

        .signature-box {
            border: 1.5px dashed var(--navy-primary);
            border-radius: 5px;
            padding: 8px;
            text-align: center;
            background: #fff;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            min-height: 90px;
        }

        .doc-name {
            font-size: 10pt;
            font-weight: 800;
            color: var(--navy-primary);
        }

        .doc-title {
            font-size: 7.5pt;
            color: var(--rose-accent);
            font-weight: 700;
        }

        .stamp-placeholder {
            margin-top: 10px;
            font-size: 7.5pt;
            color: #94A3B8;
            font-style: italic;
        }

        /* Footer */
        .page-footer {
            border-top: 1px solid var(--border-hairline);
            padding-top: 6px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 7.5pt;
            color: var(--text-muted);
            margin-top: auto;
        }


    </style>
</head>
<body>

    <!-- ========================================== -->
    <!-- PAGE 1: MATERNAL FOUNDATION & VISITS       -->
    <!-- ========================================== -->
    <div class="page-sheet">
        <div class="watermark"></div>
        <div class="page-content">

            <!-- Clinic Top Header: Refined per Dr. Sherif Radwan -->
            <header class="clinic-header">
                <div class="clinic-brand">
                    $logoImgTag
                    <div class="clinic-title-box">
                        <h1>$clinicName</h1>
                        <h2>$clinicSpecialty</h2>
                        <p>$doctorOfficialTitle</p>
                    </div>
                </div>
                <div class="header-meta uniform-meta">
                    <div class="doc-badge">CLINICAL RECORD</div>
                    <div class="meta-row"><span class="k">Patient ID:</span> <span class="v">${escapeHtml(patient.caseId)}</span></div>
                    <div class="meta-row"><span class="k">Print Date:</span> <span class="v">$printDateStr</span></div>
                    <div class="meta-row"><span class="k">Page:</span> <span class="v">1</span></div>
                </div>
            </header>

            ${if (statusLine != null) """<div class="status-banner">${escapeHtml(statusLine)}</div>""" else ""}

            <!-- Patient Identity Banner: Refined with phone on new line, age only -->
            <section class="patient-card">
                <div class="pt-name-box">
                    <h3>${escapeHtml(patient.displayName)}</h3>
                    <div class="pt-code">Patient ID: ${escapeHtml(patient.caseId)}</div>
                    <div class="pt-phone">Phone: ${escapeHtml(patient.phone ?: "Unspecified")}</div>
                    ${if (!patient.addressText.isNullOrBlank()) """<div class="pt-phone">Address: ${escapeHtml(patient.addressText)}</div>""" else ""}
                    ${if (!patient.dateOfBirth.isNullOrBlank()) """<div class="pt-phone">Date of birth: ${escapeHtml(reportDate(patient.dateOfBirth))}</div>""" else ""}
                </div>
                <div class="data-point">
                    <span class="data-label">Age</span>
                    <span class="data-value">${escapeHtml(ageFormatted)}</span>
                </div>
                <div class="data-point">
                    <span class="data-label">Blood Group</span>
                    <span class="data-value ${if (isRhNegative) "text-rose" else "text-navy"}">
                        ${escapeHtml(patient.bloodGroupText ?: "-")}
                        ${if (isRhNegative) """<span class="badge badge-danger">Rh negative</span>""" else ""}
                    </span>
                </div>
                <div class="data-point" style="align-items: flex-end;">
                    <span class="data-label">Clinical Risk Level</span>
                    ${when (risk) {
                        ReportRisk.HIGH -> """<span class="badge badge-danger">${risk.label}</span>"""
                        ReportRisk.MEDIUM -> """<span class="badge badge-warning">${risk.label}</span>"""
                        ReportRisk.LOW -> """<span class="badge badge-success">${risk.label}</span>"""
                        ReportRisk.UNRECORDED -> """<span class="badge">${escapeHtml(reportRiskLabel(patient.riskLevel))}</span>"""
                    }}
                </div>
            </section>

            ${if (!patient.clinicalAttention.isNullOrBlank()) """<div class="attention-box"><span class="k">Clinical attention</span><span class="v">${escapeHtml(patient.clinicalAttention)}</span></div>""" else ""}

            <!-- Dating & Obstetric Profile Grid -->
            <div class="grid-two-col">
                <!-- Pregnancy Dating (PREGNANCY DATING without Matrix) -->
                <div class="section-box">
                    <div class="section-title">
                        <span>Pregnancy Dating</span>
                    </div>
                    <div class="dating-matrix">
                        $datingMatrixHtml
                    </div>
                    <div class="ga-highlight">
                        ${when {
                            currentGa != null -> "Gestational age today: $currentGa"
                            patient.delivered -> escapeHtml(statusLine)
                            else -> "Gestational age: not recorded"
                        }}
                    </div>
                </div>

                <!-- Obstetric Summary & History -->
                <div class="section-box">
                    <div class="section-title">
                        <span>Obstetric & Medical History</span>
                    </div>
                    <div class="history-list">
                        <div class="history-row">
                            <span class="k">GPA Formula:</span>
                            <span class="v text-navy">G$g P$p + A$a (${escapeHtml(pastDeliverySummary)})</span>
                        </div>
                        <div class="history-row">
                            <span class="k">Drug Allergies:</span>
                            <span class="v ${if (classifyAllergy(patient.allergyHistory) is AllergyState.Recorded) "text-rose font-bold" else ""}">${escapeHtml(patient.allergyHistory?.takeUnless(String::isBlank) ?: "Not recorded")}</span>
                        </div>
                        <div class="history-row">
                            <span class="k">Medical History:</span>
                            <span class="v">${escapeHtml(patient.medicalHistory?.takeUnless(String::isBlank) ?: "Not recorded")}</span>
                        </div>
                        <div class="history-row">
                            <span class="k">Surgical History:</span>
                            <span class="v">${escapeHtml(patient.surgicalHistory?.takeUnless(String::isBlank) ?: "Not recorded")}</span>
                        </div>
                        <div class="history-row">
                            <span class="k">Family History:</span>
                            <span class="v">${escapeHtml(patient.familyHistory?.takeUnless(String::isBlank) ?: "Not recorded")}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Previous Pregnancies & Outcomes -->
            <div class="section-box">
                <div class="section-title">
                    <span>Previous Pregnancies & Obstetric Outcomes</span>
                </div>
                $prevPregRows
            </div>

            <!-- Antenatal Care Visits Log -->
            <div class="section-box" style="flex-grow: 1;">
                <div class="section-title">
                    <span>Antenatal Care Visits Log</span>
                </div>
                <div class="table-scroll">
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 4%;">#</th>
                            <th style="width: 16%;">Date</th>
                            <th style="width: 17%;">Gestational Age (GA)</th>
                            <th style="width: 15%;">Blood Pressure (BP)</th>
                            <th style="width: 10%;">Weight</th>
                            <th style="width: 38%;">Clinical Findings & Observations</th>
                        </tr>
                    </thead>
                    <tbody>
                        $visitsRowsHtml
                    </tbody>
                </table>
                </div>
            </div>

        </div>

        <!-- Page 1 Footer -->
        <footer class="page-footer">
            <span>Confidential medical record · $clinicName · $doctorOfficialTitle · Tel $CLINIC_PHONE</span>
            <span>Page 1</span>
        </footer>
    </div>

    <!-- ========================================== -->
    <!-- PAGE 2: ULTRASOUND, LABS, MEDS & PLAN      -->
    <!-- ========================================== -->
    <div class="page-sheet">
        <div class="watermark"></div>
        <div class="page-content">

            <!-- Compact Header Runner -->
            <header class="clinic-header" style="margin-bottom: 2px;">
                <div class="clinic-brand">
                    $logoImgTag
                    <div class="clinic-title-box">
                        <h1>$clinicName</h1>
                        <h2>Scans, laboratory results, medications and delivery plan</h2>
                    </div>
                </div>
                <div class="header-meta uniform-meta">
                    <div class="doc-badge" style="background: var(--rose-accent);">Page 2</div>
                    <div class="meta-row"><span class="k">Patient:</span> <span class="v">${escapeHtml(patient.displayName)}</span></div>
                    <div class="meta-row"><span class="k">Patient ID:</span> <span class="v">${escapeHtml(patient.caseId)}</span></div>
                    <div class="meta-row"><span class="k">Print Date:</span> <span class="v">$printDateStr</span></div>
                </div>
            </header>

            <!-- Ultrasound Biometry & Surveillance -->
            <div class="section-box">
                <div class="section-title">
                    <span>Ultrasound Biometry & Fetal Surveillance</span>
                </div>
                <div class="table-scroll">
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 14%;">Date</th>
                            <th style="width: 14%;">Gestational Age</th>
                            <th style="width: 13%;">Presentation</th>
                            <th style="width: 12%;">Placenta</th>
                            <th style="width: 14%;">Amniotic Fluid</th>
                            <th style="width: 12%;">EFW (g)</th>
                            <th style="width: 21%;">Clinical Notes</th>
                        </tr>
                    </thead>
                    <tbody>
                        $scansRowsHtml
                    </tbody>
                </table>
                </div>
            </div>

            <!-- Laboratory Investigations -->
            <div class="section-box">
                <div class="section-title">
                    <span>Laboratory Investigations</span>
                </div>
                <div class="table-scroll">
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 16%;">Date</th>
                            <th style="width: 36%;">Investigation / Test Name</th>
                            <th style="width: 26%;">Result & Unit</th>
                            <th style="width: 22%;">Reference Range</th>
                        </tr>
                    </thead>
                    <tbody>
                        $labsRowsHtml
                    </tbody>
                </table>
                </div>
            </div>

            <!-- Current Medications -->
            <div class="section-box">
                <div class="section-title">
                    <span>Current Medications</span>
                </div>
                <div class="table-scroll">
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 70%;">Medication and instructions</th>
                            <th style="width: 30%;">Dates</th>
                        </tr>
                    </thead>
                    <tbody>
                        $currentMedsRowsHtml
                    </tbody>
                </table>
                </div>
            </div>
            ${if (stoppedMedsRowsHtml.isNotEmpty()) """
            <!-- Stopped Medications -->
            <div class="section-box">
                <div class="section-title">
                    <span>Stopped Medications</span>
                </div>
                <div class="table-scroll">
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 70%;">Medication and instructions</th>
                            <th style="width: 30%;">Dates</th>
                        </tr>
                    </thead>
                    <tbody>
                        $stoppedMedsRowsHtml
                    </tbody>
                </table>
                </div>
            </div>
            """ else ""}

            <!-- Delivery Plan -->
            <div class="section-box" style="margin-top: auto;">
                <div class="section-title">
                    <span>Delivery Plan</span>
                </div>
                <div class="attestation-grid">
                    <div class="plan-box">
                        <div class="history-list">
                            ${patient.deliveryOutcomeLine()?.let { line -> """<div class="history-row">
                                <span class="k">Delivery Outcome:</span>
                                <span class="v text-navy font-bold">${escapeHtml(line)}</span>
                            </div>""" }.orEmpty()}
                            ${deliveryPlanRows(patient).joinToString("") { row -> """<div class="history-row">
                                <span class="k">${escapeHtml(row.label)}:</span>
                                <span class="v">${escapeHtml(row.value)}</span>
                            </div>""" }}
                            ${if (isRhNegative) """<div class="history-row">
                                <span class="k">Blood bank precautions:</span>
                                <span class="v font-bold">Anti-D Prophylaxis + Matched Packed RBCs Cross-match</span>
                            </div>""" else ""}
                            ${if (!patient.delivered && datingSummary.edd != null) """<div class="history-row">
                                <span class="k">Estimated due date:</span>
                                <span class="v text-navy font-bold">${formatDmy(datingSummary.edd)}</span>
                            </div>""" else ""}
                            ${if (!patient.delivered && deliveryPlanRows(patient).isEmpty()) """<div class="history-row">
                                <span class="k">Plan:</span>
                                <span class="v">Not recorded</span>
                            </div>""" else ""}
                        </div>
                    </div>

                    <div class="signature-box">
                        <div>
                            <div class="doc-name">Dr. Sherif Radwan</div>
                            <div class="doc-sign" lang="ar" dir="rtl">شريف جمال رضوان</div>
                            <div class="doc-sign doc-sign-title" lang="ar" dir="rtl">أخصائي التوليد وأمراض النساء وتأخر الحمل</div>
                            <div class="stamp-placeholder">Stamp</div>
                        </div>
                    </div>
                </div>
            </div>

        </div>

        <!-- Page 2 Footer -->
        <footer class="page-footer">
            <span>$clinicName · $doctorOfficialTitle · Tel $CLINIC_PHONE</span>
            <span>Page 2</span>
        </footer>
    </div>

</body>
</html>
        """.trimIndent()
    }
}
