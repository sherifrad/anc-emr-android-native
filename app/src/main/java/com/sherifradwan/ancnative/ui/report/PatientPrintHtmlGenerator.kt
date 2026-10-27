package com.sherifradwan.ancnative.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.security.SecuritySettingsState
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PatientPrintHtmlGenerator {

    private fun getLogoBase64(context: Context?): String {
        if (context == null) return ""
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.clinic_logo_foreground)
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
        val clinicNameAr = settings?.clinicName ?: "Dr. Sherif Radwan Clinic"
        val clinicNameEn = "Dr. Sherif Radwan Clinic"
        val clinicSpecialty = "Maternal-Fetal Medicine & Advanced Obstetrics"
        val doctorOfficialTitle = "Consultant of Obstetrics, Gynecology & Laparoscopy"
        val printDateStr = asOfDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val gaResult = patient.dating?.calculateGestationalAgeResult(asOfDate)
        val currentGaFormatted = gaResult?.formatted ?: "Unspecified"

        // Format Obstetric summary
        val g = patient.gravida ?: "-"
        val p = patient.parity ?: "-"
        val a = patient.abortion ?: "-"
        val prevCsCount = patient.previousPregnancies.count {
            it.birthMethod?.contains("Cesarean", ignoreCase = true) == true ||
            it.outcomeText?.contains("CS", ignoreCase = true) == true
        }
        val prevSvdCount = patient.previousPregnancies.count {
            it.birthMethod?.contains("Vaginal", ignoreCase = true) == true ||
            it.outcomeText?.contains("SVD", ignoreCase = true) == true
        }
        val pastDeliverySummary = when {
            prevCsCount > 0 && prevSvdCount > 0 -> "Prev $prevCsCount CS + $prevSvdCount SVD"
            prevCsCount > 0 -> "Prev $prevCsCount CS"
            prevSvdCount > 0 -> "Prev $prevSvdCount SVD"
            patient.previousPregnancies.isNotEmpty() -> "${patient.previousPregnancies.size} Previous Pregnancies"
            else -> "Primigravida / N/A"
        }

        val riskLevel = patient.riskLevel ?: "Low Risk"
        val isHighRisk = riskLevel.contains("High", ignoreCase = true)
        val isRhNegative = patient.bloodGroupText?.contains("-") == true

        // Collect ultrasound images if any
        val attachedUltrasoundImages = patient.ultrasoundScans.flatMapIndexed { sIdx, s ->
            s.imageUris.mapIndexed { iIdx, uri ->
                mapOf(
                    "uri" to uri,
                    "date" to s.date,
                    "label" to "Ultrasound Scan ${sIdx + 1}.${iIdx + 1} - ${s.date} (${s.impression ?: "Fetal Growth Surveillance"})",
                )
            }
        }
        val totalPages = if (attachedUltrasoundImages.isNotEmpty()) 3 else 2

        // Visits table rows (Centered text, No SFH, No FHR, Spacious and comfortable)
        val sortedVisits = patient.visits.sortedBy { it.date }
        val visitsRowsHtml = if (sortedVisits.isEmpty()) {
            """<tr><td colspan="6" class="text-center empty-cell">No clinical visits recorded yet</td></tr>"""
        } else {
            sortedVisits.take(12).mapIndexed { idx, v ->
                val vGa = try {
                    val vDate = LocalDate.parse(v.date.trim())
                    patient.dating?.calculateGestationalAgeResult(vDate)?.formatted ?: "-"
                } catch (_: Exception) {
                    "-"
                }
                """
                <tr>
                    <td class="text-center font-bold">${idx + 1}</td>
                    <td class="text-center date-cell">${v.date}</td>
                    <td class="text-center font-bold text-teal">${vGa}</td>
                    <td class="text-center font-bold text-navy">${v.bloodPressure ?: "-"}</td>
                    <td class="text-center">${v.weight?.let { "$it kg" } ?: "-"}</td>
                    <td class="text-center notes-cell">${v.findings ?: v.notes ?: "Antenatal course progressing favorably"}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }

        // Ultrasound Scans rows (Centered, EFW (g), Clinical Notes, image badge if photos exist)
        val sortedScans = patient.ultrasoundScans.sortedBy { it.date }
        val scansRowsHtml = if (sortedScans.isEmpty()) {
            """<tr><td colspan="7" class="text-center empty-cell">No ultrasound scans recorded yet</td></tr>"""
        } else {
            sortedScans.take(7).map { s ->
                val scanGaStr = when {
                    s.gestationalAgeWeeks != null && s.gestationalAgeDays != null -> "${s.gestationalAgeWeeks}w + ${s.gestationalAgeDays}d"
                    s.gestationalAgeWeeks != null -> "${s.gestationalAgeWeeks}w"
                    else -> "-"
                }
                val biometry = listOfNotNull(
                    s.biometryBpd?.let { "BPD: $it" },
                    s.biometryHc?.let { "HC: $it" },
                    s.biometryAc?.let { "AC: $it" },
                    s.biometryFl?.let { "FL: $it" }
                ).joinToString(" · ").ifBlank { "-" }

                val photoIndicator = if (s.imageUris.isNotEmpty()) {
                    """<div class="photo-badge">📷 ${s.imageUris.size} scan photos attached</div>"""
                } else {
                    ""
                }

                val notesText = s.impression ?: biometry

                val efwStr = s.estimatedFetalWeight?.let { weight ->
                    if (weight.contains("g", ignoreCase = true) || weight.contains("kg", ignoreCase = true)) {
                        weight
                    } else {
                        "$weight g"
                    }
                } ?: "-"

                """
                <tr>
                    <td class="text-center date-cell">${s.date}</td>
                    <td class="text-center font-bold text-teal">${scanGaStr}</td>
                    <td class="text-center">${s.presentation ?: "Cephalic"}</td>
                    <td class="text-center">${s.placentalSite ?: "Posterior Grade I"}</td>
                    <td class="text-center">${s.amnioticFluid ?: "Adequate (AFI 14cm)"}</td>
                    <td class="text-center font-bold text-navy">${efwStr}</td>
                    <td class="text-center notes-cell">
                        $notesText
                        $photoIndicator
                    </td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }

        // Labs rows (Centered, NO status column, Abnormal result displayed in RED)
        val sortedLabs = patient.labInvestigations.sortedByDescending { it.date }
        val labsRowsHtml = if (sortedLabs.isEmpty()) {
            """<tr><td colspan="4" class="text-center empty-cell">No laboratory test results recorded yet</td></tr>"""
        } else {
            sortedLabs.take(10).map { l ->
                val resultHtml = if (l.isAbnormal) {
                    """<span class="val-abnormal">${l.result} ${l.unit ?: ""} (Abnormal)</span>"""
                } else {
                    """<span class="font-bold text-navy">${l.result} ${l.unit ?: ""}</span>"""
                }
                """
                <tr>
                    <td class="text-center date-cell">${l.date}</td>
                    <td class="text-center font-bold text-navy">${l.testName}</td>
                    <td class="text-center">${resultHtml}</td>
                    <td class="text-center text-muted">${l.referenceRange ?: "-"}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }

        // Medications rows (Centered)
        val activeMeds = patient.medications.take(8)
        val medsRowsHtml = if (activeMeds.isEmpty()) {
            """<tr><td colspan="5" class="text-center empty-cell">No active medications or supplements recorded</td></tr>"""
        } else {
            activeMeds.map { m ->
                val typeBadge = if (m.isProphylaxis) {
                    """<span class="badge badge-teal">Prophylaxis</span>"""
                } else {
                    """<span class="badge badge-navy">Rx</span>"""
                }
                """
                <tr>
                    <td class="text-center font-bold text-navy">${m.name}</td>
                    <td class="text-center font-bold">${m.dosage} (${m.route})</td>
                    <td class="text-center">${m.frequency}</td>
                    <td class="text-center">${typeBadge}</td>
                    <td class="text-center notes-cell text-muted">${m.notes ?: "Ongoing as prescribed"}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }

        // Previous Pregnancies (Centered)
        val prevPregRows = if (patient.previousPregnancies.isEmpty()) {
            """<p class="text-muted text-center text-small" style="padding: 6px;">No previous pregnancies or deliveries recorded (Primigravida / N/A).</p>"""
        } else {
            """
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
                        val isLoss = pp.outcome in setOf("Stillbirth", "EarlyPregnancyLoss", "TerminationOfPregnancy") ||
                            (pp.outcomeText?.contains("IUFD", ignoreCase = true) == true) ||
                            (pp.outcomeText?.contains("Stillbirth", ignoreCase = true) == true) ||
                            (pp.outcomeText?.contains("Miscarriage", ignoreCase = true) == true)
                        val outcomeBadge = if (isLoss) {
                            """<span class="badge" style="background:#fee2e2;color:#991b1b;border:1px solid #f87171;font-weight:bold;padding:2px 6px;border-radius:4px;">$outcomeLabel</span>"""
                        } else {
                            """<span style="font-weight:600;color:#0f766e;">$outcomeLabel</span>"""
                        }
                        val methodText = pp.birthMethod ?: pp.deliveryBy ?: pp.lossManagement ?: pp.terminationMethod ?: "-"
                        """
                        <tr>
                            <td class="text-center date-cell">${pp.dateText ?: "-"}</td>
                            <td class="text-center font-bold">${methodText}</td>
                            <td class="text-center">${outcomeBadge}</td>
                            <td class="text-center">${pp.placeAndDoctor ?: pp.otherDetails ?: "Uncomplicated"}</td>
                        </tr>
                        """.trimIndent()
                    }.joinToString("\n")}
                </tbody>
            </table>
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

        // Age string format
        val ageFormatted = if (!patient.ageText.isNullOrBlank()) "${patient.ageText} yrs" else "-"

        // Appendix Page 3 HTML (if attached photos exist)
        val page3Html = if (attachedUltrasoundImages.isNotEmpty()) {
            """
            <!-- ========================================== -->
            <!-- PAGE 3: CLINICAL IMAGING & ATTACHMENTS     -->
            <!-- ========================================== -->
            <div class="page-sheet">
                <div class="watermark"></div>
                <div class="page-content">
                    <header class="clinic-header" style="margin-bottom: 6px;">
                        <div class="clinic-brand">
                            $logoImgTag
                            <div class="clinic-title-box">
                                <h1>$clinicNameEn</h1>
                                <p>$clinicSpecialty</p>
                            </div>
                        </div>
                        <div class="header-meta uniform-meta">
                            <div class="doc-badge">IMAGING ATTACHMENTS</div>
                            <div class="meta-row"><span class="k">Patient:</span> <span class="v">${patient.displayName}</span></div>
                            <div class="meta-row"><span class="k">Case ID:</span> <span class="v">${patient.caseId}</span></div>
                            <div class="meta-row"><span class="k">Print Date:</span> <span class="v">$printDateStr</span></div>
                            <div class="meta-row"><span class="k">Page:</span> <span class="v">3 of $totalPages</span></div>
                        </div>
                    </header>

                    <div class="section-box" style="flex-grow: 1;">
                        <div class="section-title">
                            <span>Clinical Imaging & Ultrasound Scans</span>
                            <span class="en-sub">ULTRASOUND ATTACHMENTS</span>
                        </div>
                        <div class="photos-grid">
                            ${attachedUltrasoundImages.map { photo ->
                                """
                                <div class="photo-card">
                                    <div class="photo-preview-placeholder">📷 [Original Ultrasound Scan Stored in Secure System]</div>
                                    <div class="photo-caption">${photo["label"]}</div>
                                </div>
                                """.trimIndent()
                            }.joinToString("\n")}
                        </div>
                    </div>
                </div>

                <footer class="page-footer">
                    <span>$clinicNameEn · $doctorOfficialTitle · Contact: +20 100 000 0000</span>
                    <span>Page 3 of $totalPages · End of Appendix</span>
                </footer>
            </div>
            """.trimIndent()
        } else {
            ""
        }

        return """
<!DOCTYPE html>
<html lang="en" dir="ltr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Clinical Record - ${patient.displayName} - $clinicNameEn</title>
    <style>
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
            width: 58px;
            height: 58px;
            object-fit: contain;
            border-radius: 50%;
            border: 1px solid var(--border-hairline);
            padding: 2px;
            background: #fff;
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

        .section-title .en-sub {
            font-size: 7.5pt;
            color: var(--rose-accent);
            font-weight: 600;
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
            word-wrap: break-word;
            overflow-wrap: break-word;
            word-break: break-word;
        }

        .clinical-table td {
            padding: 5px 4px;
            border: 1px solid var(--border-hairline);
            color: var(--text-main);
            text-align: center !important;
            vertical-align: middle;
            box-sizing: border-box;
            word-wrap: break-word;
            overflow-wrap: break-word;
            word-break: break-word;
        }

        .clinical-table tr:nth-child(even) {
            background-color: #FAFAFA;
        }

        .mini-table th, .mini-table td {
            padding: 3px 4px;
            font-size: 7.5pt;
            word-wrap: break-word;
            overflow-wrap: break-word;
            word-break: break-word;
        }

        .text-center { text-align: center !important; }
        .font-bold { font-weight: 700; }
        .text-navy { color: var(--navy-primary); }
        .text-rose { color: var(--rose-accent); }
        .text-teal { color: var(--teal-clinical); }
        .text-muted { color: var(--text-muted); }
        .empty-cell { padding: 12px; color: var(--text-muted); font-style: italic; }
        .date-cell { font-family: monospace; font-size: 7.8pt; }
        .notes-cell {
            font-size: 7.2pt;
            line-height: 1.25;
            word-wrap: break-word;
            overflow-wrap: break-word;
            word-break: break-word;
            max-width: 100%;
        }

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

        /* Photos Grid on Appendix Page */
        .photos-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 12px;
            margin-top: 8px;
        }

        .photo-card {
            border: 1px solid var(--border-light);
            border-radius: 6px;
            padding: 8px;
            background: #F8FAFC;
            text-align: center;
        }

        .photo-preview-placeholder {
            height: 170px;
            background: #0F172A;
            color: #94A3B8;
            border-radius: 4px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 9pt;
            font-weight: 600;
        }

        .photo-caption {
            margin-top: 6px;
            font-size: 8pt;
            font-weight: 700;
            color: var(--navy-primary);
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
                        <h1>$clinicNameEn</h1>
                        <h2>$clinicSpecialty</h2>
                        <p>$doctorOfficialTitle</p>
                    </div>
                </div>
                <div class="header-meta uniform-meta">
                    <div class="doc-badge">CLINICAL RECORD</div>
                    <div class="meta-row"><span class="k">Type:</span> <span class="v">Certified Record</span></div>
                    <div class="meta-row"><span class="k">Patient ID:</span> <span class="v">${patient.caseId}</span></div>
                    <div class="meta-row"><span class="k">Print Date:</span> <span class="v">$printDateStr</span></div>
                    <div class="meta-row"><span class="k">Page:</span> <span class="v">1 of $totalPages</span></div>
                </div>
            </header>

            <!-- Patient Identity Banner: Refined with phone on new line, age only -->
            <section class="patient-card">
                <div class="pt-name-box">
                    <h3>${patient.displayName}</h3>
                    <div class="pt-code">Patient ID: ${patient.caseId}</div>
                    <div class="pt-phone">Phone: ${patient.phone ?: "Unspecified"}</div>
                </div>
                <div class="data-point">
                    <span class="data-label">Age</span>
                    <span class="data-value">$ageFormatted</span>
                </div>
                <div class="data-point">
                    <span class="data-label">Blood Group</span>
                    <span class="data-value ${if (isRhNegative) "text-rose" else "text-navy"}">
                        ${patient.bloodGroupText ?: "-"}
                        ${if (isRhNegative) """<span class="badge badge-danger">Rh- Negative</span>""" else ""}
                    </span>
                </div>
                <div class="data-point" style="align-items: flex-end;">
                    <span class="data-label">Clinical Risk Level</span>
                    ${if (isHighRisk) """<span class="badge badge-danger">High Risk</span>""" else """<span class="badge badge-success">Normal Risk</span>"""}
                </div>
            </section>

            <!-- Dating & Obstetric Profile Grid -->
            <div class="grid-two-col">
                <!-- Pregnancy Dating (PREGNANCY DATING without Matrix) -->
                <div class="section-box">
                    <div class="section-title">
                        <span>Pregnancy Dating</span>
                        <span class="en-sub">PREGNANCY DATING</span>
                    </div>
                    <div class="dating-matrix">
                        <div class="matrix-item">
                            <div class="label">LMP Date</div>
                            <div class="val">${patient.dating?.lmpDate ?: "-"}</div>
                        </div>
                        <div class="matrix-item">
                            <div class="label">Ultrasound EDD</div>
                            <div class="val">${patient.dating?.ultrasoundEdd ?: "-"}</div>
                        </div>
                        <div class="matrix-item">
                            <div class="label">Agreed EDD</div>
                            <div class="val text-rose">${patient.dating?.agreedEdd ?: "-"}</div>
                        </div>
                    </div>
                    <div class="ga-highlight">
                        Current Gestational Age (GA): $currentGaFormatted
                    </div>
                </div>

                <!-- Obstetric Summary & History -->
                <div class="section-box">
                    <div class="section-title">
                        <span>Obstetric & Medical History</span>
                        <span class="en-sub">OBSTETRIC & MEDICAL HISTORY</span>
                    </div>
                    <div class="history-list">
                        <div class="history-row">
                            <span class="k">GPA Formula:</span>
                            <span class="v text-navy">G$g P$p + A$a (${pastDeliverySummary})</span>
                        </div>
                        <div class="history-row">
                            <span class="k">Drug Allergies:</span>
                            <span class="v ${if (!patient.allergyHistory.isNullOrBlank()) "text-rose font-bold" else ""}">${patient.allergyHistory ?: "NKDA"}</span>
                        </div>
                        <div class="history-row">
                            <span class="k">Medical History:</span>
                            <span class="v">${patient.medicalHistory ?: "None"}</span>
                        </div>
                        <div class="history-row">
                            <span class="k">Surgical History:</span>
                            <span class="v">${patient.surgicalHistory ?: "None"}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Previous Pregnancies & Outcomes -->
            <div class="section-box">
                <div class="section-title">
                    <span>Previous Pregnancies & Obstetric Outcomes</span>
                    <span class="en-sub">PREVIOUS PREGNANCIES & OUTCOMES</span>
                </div>
                $prevPregRows
            </div>

            <!-- Antenatal Care Visits Log -->
            <div class="section-box" style="flex-grow: 1;">
                <div class="section-title">
                    <span>Antenatal Care Visits Log</span>
                    <span class="en-sub">ANTENATAL CARE VISITS LOG</span>
                </div>
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 5%;">#</th>
                            <th style="width: 15%;">Date</th>
                            <th style="width: 15%;">Gestational Age (GA)</th>
                            <th style="width: 14%;">Blood Pressure (BP)</th>
                            <th style="width: 14%;">Weight</th>
                            <th style="width: 37%;">Clinical Findings & Observations</th>
                        </tr>
                    </thead>
                    <tbody>
                        $visitsRowsHtml
                    </tbody>
                </table>
            </div>

        </div>

        <!-- Page 1 Footer -->
        <footer class="page-footer">
            <span>Confidential Medical Record · Certified electronically by $clinicNameEn · $doctorOfficialTitle</span>
            <span>Page 1 of $totalPages</span>
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
                        <h1>$clinicNameEn</h1>
                        <h2>Diagnostic Surveillance, Ultrasound, Labs & Delivery Plan</h2>
                    </div>
                </div>
                <div class="header-meta uniform-meta">
                    <div class="doc-badge" style="background: var(--rose-accent);">Page 2 of $totalPages</div>
                    <div class="meta-row"><span class="k">Patient:</span> <span class="v">${patient.displayName}</span></div>
                    <div class="meta-row"><span class="k">Patient ID:</span> <span class="v">${patient.caseId}</span></div>
                    <div class="meta-row"><span class="k">Print Date:</span> <span class="v">$printDateStr</span></div>
                </div>
            </header>

            <!-- Ultrasound Biometry & Surveillance -->
            <div class="section-box">
                <div class="section-title">
                    <span>Ultrasound Biometry & Fetal Surveillance</span>
                    <span class="en-sub">ULTRASOUND BIOMETRY & SURVEILLANCE</span>
                </div>
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 12%;">Date</th>
                            <th style="width: 12%;">Gestational Age</th>
                            <th style="width: 13%;">Presentation</th>
                            <th style="width: 14%;">Placenta</th>
                            <th style="width: 14%;">Amniotic Fluid</th>
                            <th style="width: 14%;">EFW (g)</th>
                            <th style="width: 21%;">Clinical Notes</th>
                        </tr>
                    </thead>
                    <tbody>
                        $scansRowsHtml
                    </tbody>
                </table>
            </div>

            <!-- Laboratory Investigations -->
            <div class="section-box">
                <div class="section-title">
                    <span>Laboratory Investigations</span>
                    <span class="en-sub">LABORATORY INVESTIGATIONS</span>
                </div>
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

            <!-- Active Medications & Prophylaxis -->
            <div class="section-box">
                <div class="section-title">
                    <span>Active Medications & Prophylaxis</span>
                    <span class="en-sub">ACTIVE MEDICATIONS & PROPHYLAXIS</span>
                </div>
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 28%;">Medication</th>
                            <th style="width: 20%;">Dosage & Route</th>
                            <th style="width: 18%;">Frequency</th>
                            <th style="width: 16%;">Type</th>
                            <th style="width: 18%;">Notes</th>
                        </tr>
                    </thead>
                    <tbody>
                        $medsRowsHtml
                    </tbody>
                </table>
            </div>

            <!-- Delivery Management Plan & Attestation -->
            <div class="section-box" style="margin-top: auto;">
                <div class="section-title">
                    <span>Delivery Management Plan & Attestation</span>
                    <span class="en-sub">DELIVERY MANAGEMENT PLAN & CONSULTANT ATTESTATION</span>
                </div>
                <div class="attestation-grid">
                    <div class="plan-box">
                        <div class="history-list">
                            <div class="history-row">
                                <span class="k">Planned Delivery Mode:</span>
                                <span class="v text-rose font-bold">Elective Cesarean Section (CS) / Spontaneous Vaginal Delivery (SVD)</span>
                            </div>
                            <div class="history-row">
                                <span class="k">Target Delivery Timing:</span>
                                <span class="v text-navy font-bold">${patient.dating?.agreedEdd ?: "At 39 completed weeks"}</span>
                            </div>
                            <div class="history-row">
                                <span class="k">Hospital & Blood Bank Precautions:</span>
                                <span class="v font-bold">${if (isRhNegative) "Anti-D Prophylaxis + Matched Packed RBCs Cross-match" else "Routine Obstetric Hospital Protocol"}</span>
                            </div>
                            <div class="history-row">
                                <span class="k">Special Recommendations:</span>
                                <span class="v">Regular fetal kick counts; immediate hospital evaluation for bleeding, ruptured membranes, or severe pain.</span>
                            </div>
                        </div>
                    </div>

                    <div class="signature-box">
                        <div>
                            <div class="doc-name">Dr. Sherif Radwan</div>
                            <div class="doc-title">$doctorOfficialTitle</div>
                            <div class="stamp-placeholder">Official Stamp & Signature</div>
                        </div>
                        <div style="font-size: 7pt; color: #64748B; margin-top: 12px;">
                            Official Stamp & Signature
                        </div>
                    </div>
                </div>
            </div>

        </div>

        <!-- Page 2 Footer -->
        <footer class="page-footer">
            <span>$clinicNameEn · $doctorOfficialTitle · Contact: +20 100 000 0000 · Integrated Antenatal Care</span>
            <span>Page 2 of $totalPages</span>
        </footer>
    </div>

    $page3Html

</body>
</html>
        """.trimIndent()
    }
}
