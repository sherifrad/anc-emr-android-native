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
        val clinicNameAr = settings?.clinicName ?: "دكتور شريف رضوان كلينيك"
        val clinicNameEn = "Dr. Sherif Radwan Clinic"
        val clinicSpecialty = "رعاية الأمومة وطب الجنين"
        val doctorOfficialTitle = "أخصائي النساء والتوليد وجراحة المناظير وعلاج العقم"
        val printDateStr = asOfDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val gaResult = patient.dating?.calculateGestationalAgeResult(asOfDate)
        val currentGaFormatted = gaResult?.formatted ?: "غير محدد"

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
        val isHighRisk = riskLevel.contains("High", ignoreCase = true) || riskLevel.contains("عالي", ignoreCase = true)
        val isRhNegative = patient.bloodGroupText?.contains("-") == true

        // Collect ultrasound images if any
        val attachedUltrasoundImages = patient.ultrasoundScans.flatMapIndexed { sIdx, s ->
            s.imageUris.mapIndexed { iIdx, uri ->
                mapOf(
                    "uri" to uri,
                    "date" to s.date,
                    "label" to "صورة سونار ${sIdx + 1}.${iIdx + 1} - ${s.date} (${s.impression ?: "فحص نمو الجنين"})",
                )
            }
        }
        val totalPages = if (attachedUltrasoundImages.isNotEmpty()) 3 else 2

        // Visits table rows (Centered text, No SFH, No FHR, Spacious and comfortable)
        val sortedVisits = patient.visits.sortedBy { it.date }
        val visitsRowsHtml = if (sortedVisits.isEmpty()) {
            """<tr><td colspan="6" class="text-center empty-cell">لم يتم تسجيل أي زيارات سريرية بعد</td></tr>"""
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
                    <td class="text-center">${v.weight?.let { "$it كجم" } ?: "-"}</td>
                    <td class="text-center notes-cell">${v.findings ?: v.notes ?: "سير الحمل طبيعي ومستقر وفق الخطة الإكلينيكية"}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }

        // Ultrasound Scans rows (Centered, وزن الجنين (كجم), الملاحظات الطبية, image badge if photos exist)
        val sortedScans = patient.ultrasoundScans.sortedBy { it.date }
        val scansRowsHtml = if (sortedScans.isEmpty()) {
            """<tr><td colspan="7" class="text-center empty-cell">لم يتم تسجيل أي فحوصات سونار بعد</td></tr>"""
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
                    """<div class="photo-badge">📷 ${s.imageUris.size} صورة سونار مرفقة</div>"""
                } else {
                    ""
                }

                val notesText = s.impression ?: biometry

                val efwStr = s.estimatedFetalWeight?.let { weight ->
                    if (weight.contains("g", ignoreCase = true) || weight.contains("جم") || weight.contains("kg", ignoreCase = true)) {
                        weight
                    } else {
                        "$weight جم"
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
            """<tr><td colspan="4" class="text-center empty-cell">لم يتم تسجيل نتائج تحاليل بعد</td></tr>"""
        } else {
            sortedLabs.take(10).map { l ->
                val resultHtml = if (l.isAbnormal) {
                    """<span class="val-abnormal">${l.result} ${l.unit ?: ""} (غير طبيعي)</span>"""
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
            """<tr><td colspan="5" class="text-center empty-cell">لا توجد أدوية أو مكملات مسجلة حالياً</td></tr>"""
        } else {
            activeMeds.map { m ->
                val typeBadge = if (m.isProphylaxis) {
                    """<span class="badge badge-teal">وقائي (Prophylaxis)</span>"""
                } else {
                    """<span class="badge badge-navy">علاجي (Rx)</span>"""
                }
                """
                <tr>
                    <td class="text-center font-bold text-navy">${m.name}</td>
                    <td class="text-center font-bold">${m.dosage} (${m.route})</td>
                    <td class="text-center">${m.frequency}</td>
                    <td class="text-center">${typeBadge}</td>
                    <td class="text-center notes-cell text-muted">${m.notes ?: "مستمر بانتظام وفق الوصفة"}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }

        // Previous Pregnancies (Centered)
        val prevPregRows = if (patient.previousPregnancies.isEmpty()) {
            """<p class="text-muted text-center text-small" style="padding: 6px;">لا يوجد تاريخ لحمول أو ولادات سابقة مسجلة (بكرية أو غير مسجل).</p>"""
        } else {
            """
            <table class="clinical-table mini-table">
                <thead>
                    <tr>
                        <th style="width: 14%; text-align: center;">السنة / التاريخ</th>
                        <th style="width: 26%; text-align: center;">نوع الحمل وطريقة الولادة</th>
                        <th style="width: 25%; text-align: center;">النتيجة والحالة</th>
                        <th style="width: 35%; text-align: center;">المستشفى والملاحظات السريرية</th>
                    </tr>
                </thead>
                <tbody>
                    ${patient.previousPregnancies.map { pp ->
                        val outcomeLabel = when (pp.outcome) {
                            "Stillbirth" -> pp.outcomeText ?: "وفاة جنينية داخل الرحم (Stillbirth/IUFD)"
                            "EarlyPregnancyLoss" -> pp.outcomeText ?: "إجهاض مبكر (Miscarriage)"
                            "TerminationOfPregnancy" -> pp.outcomeText ?: "إنهاء حمل علاجي (Termination)"
                            "LiveBirth" -> pp.outcomeText ?: "مولود حي مكتمل (Live Birth)"
                            else -> pp.outcomeText ?: pp.outcome ?: "-"
                        }
                        val isLoss = pp.outcome in setOf("Stillbirth", "EarlyPregnancyLoss", "TerminationOfPregnancy") ||
                            (pp.outcomeText?.contains("وفاة", ignoreCase = true) == true) ||
                            (pp.outcomeText?.contains("إجهاض", ignoreCase = true) == true) ||
                            (pp.outcomeText?.contains("IUFD", ignoreCase = true) == true) ||
                            (pp.outcomeText?.contains("Stillbirth", ignoreCase = true) == true)
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
                            <td class="text-center">${pp.placeAndDoctor ?: pp.otherDetails ?: "بدون مضاعفات"}</td>
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
        val ageFormatted = if (!patient.ageText.isNullOrBlank()) "${patient.ageText} سنة" else "-"

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
                                <h1>$clinicNameAr</h1>
                                <h2>$clinicNameEn</h2>
                                <p>$clinicSpecialty</p>
                            </div>
                        </div>
                        <div class="header-meta uniform-meta">
                            <div class="doc-badge">ملحق الصور المرفقة</div>
                            <div class="meta-row"><span class="k">المريضة:</span> <span class="v">${patient.displayName}</span></div>
                            <div class="meta-row"><span class="k">كود الحالة:</span> <span class="v">${patient.caseId}</span></div>
                            <div class="meta-row"><span class="k">تاريخ الطباعة:</span> <span class="v">$printDateStr</span></div>
                            <div class="meta-row"><span class="k">الصفحة:</span> <span class="v">3 من $totalPages</span></div>
                        </div>
                    </header>

                    <div class="section-box" style="flex-grow: 1;">
                        <div class="section-title">
                            <span>ملحق صور وفحوصات السونار والوثائق المرفقة</span>
                            <span class="en-sub">CLINICAL IMAGING & ULTRASOUND ATTACHMENTS</span>
                        </div>
                        <div class="photos-grid">
                            ${attachedUltrasoundImages.map { photo ->
                                """
                                <div class="photo-card">
                                    <div class="photo-preview-placeholder">📷 [صورة سونار أصلية محفوظة بالنظام]</div>
                                    <div class="photo-caption">${photo["label"]}</div>
                                </div>
                                """.trimIndent()
                            }.joinToString("\n")}
                        </div>
                    </div>
                </div>

                <footer class="page-footer">
                    <span>$clinicNameAr · $doctorOfficialTitle · هاتف وحجز: 0100000000</span>
                    <span>صفحة 3 من $totalPages · نهاية الملحق</span>
                </footer>
            </div>
            """.trimIndent()
        } else {
            ""
        }

        return """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ملف المريضة - ${patient.displayName} - $clinicNameAr</title>
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
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Cairo", "Noto Sans Arabic", sans-serif;
            background-color: #E2E8F0;
            color: var(--text-main);
            font-size: 10.5pt;
            line-height: 1.4;
            direction: rtl;
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
                border-right: none !important;
                padding-right: 0 !important;
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
                min-height: 297mm;
                max-height: 297mm;
                box-shadow: none;
                page-break-after: always;
                break-after: page;
                padding: 10mm 14mm 8mm 14mm;
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
                border-right: 2px solid var(--border-hairline) !important;
                padding-right: 12px !important;
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
            text-align: right;
            direction: rtl;
            display: flex;
            flex-direction: column;
            gap: 2px;
            min-width: 175px;
            border-right: 2px solid var(--border-hairline);
            padding-right: 12px;
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
            text-align: right;
        }

        .history-row .v {
            color: var(--navy-primary);
            font-weight: 700;
            text-align: left;
            direction: ltr;
        }

        /* Clinical Tables: Perfectly Centered Contents */
        .clinical-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 8pt;
            margin-top: 4px;
        }

        .clinical-table th {
            background: #F1F5F9;
            color: var(--navy-primary);
            font-weight: 700;
            padding: 6px 6px;
            border: 1px solid var(--border-light);
            text-align: center !important;
            font-size: 7.5pt;
        }

        .clinical-table td {
            padding: 6px 6px;
            border: 1px solid var(--border-hairline);
            color: var(--text-main);
            text-align: center !important;
            vertical-align: middle;
        }

        .clinical-table tr:nth-child(even) {
            background-color: #FAFAFA;
        }

        .mini-table th, .mini-table td {
            padding: 4px 5px;
            font-size: 7.5pt;
        }

        .text-center { text-align: center !important; }
        .font-bold { font-weight: 700; }
        .text-navy { color: var(--navy-primary); }
        .text-rose { color: var(--rose-accent); }
        .text-teal { color: var(--teal-clinical); }
        .text-muted { color: var(--text-muted); }
        .empty-cell { padding: 14px; color: var(--text-muted); font-style: italic; }
        .date-cell { font-family: monospace; font-size: 8pt; }
        .notes-cell { font-size: 7.8pt; line-height: 1.35; }

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
                        <h1>$clinicNameAr</h1>
                        <h2>$clinicNameEn</h2>
                        <p>$clinicSpecialty</p>
                    </div>
                </div>
                <div class="header-meta uniform-meta">
                    <div class="doc-badge">CLINICAL RECORD</div>
                    <div class="meta-row"><span class="k">النوع:</span> <span class="v">ملف معتمد</span></div>
                    <div class="meta-row"><span class="k">كود الحالة:</span> <span class="v">${patient.caseId}</span></div>
                    <div class="meta-row"><span class="k">تاريخ الطباعة:</span> <span class="v">$printDateStr</span></div>
                    <div class="meta-row"><span class="k">الصفحة:</span> <span class="v">1 من $totalPages</span></div>
                </div>
            </header>

            <!-- Patient Identity Banner: Refined with phone on new line, age only -->
            <section class="patient-card">
                <div class="pt-name-box">
                    <h3>${patient.displayName}</h3>
                    <div class="pt-code">كود المريضة: ${patient.caseId}</div>
                    <div class="pt-phone">هاتف: ${patient.phone ?: "غير مسجل"}</div>
                </div>
                <div class="data-point">
                    <span class="data-label">العمر</span>
                    <span class="data-value">$ageFormatted</span>
                </div>
                <div class="data-point">
                    <span class="data-label">فصيلة الدم</span>
                    <span class="data-value ${if (isRhNegative) "text-rose" else "text-navy"}">
                        ${patient.bloodGroupText ?: "-"}
                        ${if (isRhNegative) """<span class="badge badge-danger">Rh- سلبي</span>""" else ""}
                    </span>
                </div>
                <div class="data-point" style="align-items: flex-end;">
                    <span class="data-label">مستوى الخطورة الإكلينيكية</span>
                    ${if (isHighRisk) """<span class="badge badge-danger">عالي الخطورة</span>""" else """<span class="badge badge-success">طبيعي</span>"""}
                </div>
            </section>

            <!-- Dating & Obstetric Profile Grid -->
            <div class="grid-two-col">
                <!-- Pregnancy Dating (PREGNANCY DATING without Matrix) -->
                <div class="section-box">
                    <div class="section-title">
                        <span>تحديد موعد وعمر الحمل</span>
                        <span class="en-sub">PREGNANCY DATING</span>
                    </div>
                    <div class="dating-matrix">
                        <div class="matrix-item">
                            <div class="label">آخر دورة (LMP)</div>
                            <div class="val">${patient.dating?.lmpDate ?: "-"}</div>
                        </div>
                        <div class="matrix-item">
                            <div class="label">موعد السونار</div>
                            <div class="val">${patient.dating?.ultrasoundEdd ?: "-"}</div>
                        </div>
                        <div class="matrix-item">
                            <div class="label">الموعد المعتمد (EDD)</div>
                            <div class="val text-rose">${patient.dating?.agreedEdd ?: "-"}</div>
                        </div>
                    </div>
                    <div class="ga-highlight">
                        عمر الحمل الحالي (GA): $currentGaFormatted
                    </div>
                </div>

                <!-- Obstetric Summary & History (Arabic on right, English on left) -->
                <div class="section-box">
                    <div class="section-title">
                        <span>الملخص النسائي والتاريخ المرضي</span>
                        <span class="en-sub">OBSTETRIC & MEDICAL HISTORY</span>
                    </div>
                    <div class="history-list">
                        <div class="history-row">
                            <span class="k">الصيغة الولادية (GPA):</span>
                            <span class="v text-navy">G$g P$p + A$a (${pastDeliverySummary})</span>
                        </div>
                        <div class="history-row">
                            <span class="k">الحساسية الدوائية (Allergies):</span>
                            <span class="v ${if (!patient.allergyHistory.isNullOrBlank()) "text-rose font-bold" else ""}">${patient.allergyHistory ?: "NKDA"}</span>
                        </div>
                        <div class="history-row">
                            <span class="k">الأمراض المزمنة (Medical):</span>
                            <span class="v">${patient.medicalHistory ?: "None"}</span>
                        </div>
                        <div class="history-row">
                            <span class="k">العمليات السابقة (Surgical):</span>
                            <span class="v">${patient.surgicalHistory ?: "None"}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- مرات الحمل والولادات السابقة (Centered cells) -->
            <div class="section-box">
                <div class="section-title">
                    <span>مرات الحمل والولادات السابقة</span>
                    <span class="en-sub">PREVIOUS PREGNANCIES & OUTCOMES</span>
                </div>
                $prevPregRows
            </div>

            <!-- زيارات متابعة الحمل (Centered, No SFH, No FHR, Spacious & Comfortable) -->
            <div class="section-box" style="flex-grow: 1;">
                <div class="section-title">
                    <span>زيارات متابعة الحمل</span>
                    <span class="en-sub">ANTENATAL CARE VISITS LOG</span>
                </div>
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 5%;">#</th>
                            <th style="width: 15%;">التاريخ</th>
                            <th style="width: 15%;">عمر الحمل (GA)</th>
                            <th style="width: 14%;">ضغط الدم (BP)</th>
                            <th style="width: 14%;">الوزن</th>
                            <th style="width: 37%;">الفحص السريري والتوصيات والملاحظات</th>
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
            <span>ملف طبي سرّي ومعتمد · صادر إلكترونياً من $clinicNameAr · $doctorOfficialTitle</span>
            <span>صفحة 1 من $totalPages</span>
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
                        <h1>$clinicNameAr</h1>
                        <h2>الفحوصات التشخيصية، السونار، التحاليل وخطة الولادة</h2>
                    </div>
                </div>
                <div class="header-meta uniform-meta">
                    <div class="doc-badge" style="background: var(--rose-accent);">الصفحة 2 من $totalPages</div>
                    <div class="meta-row"><span class="k">المريضة:</span> <span class="v">${patient.displayName}</span></div>
                    <div class="meta-row"><span class="k">كود الحالة:</span> <span class="v">${patient.caseId}</span></div>
                    <div class="meta-row"><span class="k">تاريخ الطباعة:</span> <span class="v">$printDateStr</span></div>
                </div>
            </header>

            <!-- سجل قياسات السونار وتطور نمو الجنين (Centered, وزن الجنين (كجم), الملاحظات الطبية) -->
            <div class="section-box">
                <div class="section-title">
                    <span>سجل قياسات السونار وتطور نمو الجنين</span>
                    <span class="en-sub">ULTRASOUND BIOMETRY & SURVEILLANCE</span>
                </div>
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 12%;">التاريخ</th>
                            <th style="width: 12%;">عمر الحمل</th>
                            <th style="width: 13%;">المجيء والوضع</th>
                            <th style="width: 14%;">المشيمة</th>
                            <th style="width: 14%;">السائل الأمنيوسي</th>
                            <th style="width: 14%;">وزن الجنين (جم / EFW)</th>
                            <th style="width: 21%;">الملاحظات الطبية</th>
                        </tr>
                    </thead>
                    <tbody>
                        $scansRowsHtml
                    </tbody>
                </table>
            </div>

            <!-- الفحوصات والتحاليل (Centered, No status column, Abnormal in RED) -->
            <div class="section-box">
                <div class="section-title">
                    <span>الفحوصات والتحاليل</span>
                    <span class="en-sub">LABORATORY INVESTIGATIONS</span>
                </div>
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 16%;">التاريخ</th>
                            <th style="width: 36%;">اسم الفحص والتحليل</th>
                            <th style="width: 26%;">النتيجة والوحدة</th>
                            <th style="width: 22%;">المعدل الطبيعي المرجعي</th>
                        </tr>
                    </thead>
                    <tbody>
                        $labsRowsHtml
                    </tbody>
                </table>
            </div>

            <!-- الأدوية والمكملات المعتمدة خلال الحمل (Centered) -->
            <div class="section-box">
                <div class="section-title">
                    <span>الأدوية والمكملات المعتمدة خلال الحمل</span>
                    <span class="en-sub">ACTIVE MEDICATIONS & PROPHYLAXIS</span>
                </div>
                <table class="clinical-table">
                    <thead>
                        <tr>
                            <th style="width: 28%;">اسم العلاج</th>
                            <th style="width: 20%;">الجرعة والطريق</th>
                            <th style="width: 18%;">التكرار</th>
                            <th style="width: 16%;">النوع</th>
                            <th style="width: 18%;">ملاحظات الطبيب</th>
                        </tr>
                    </thead>
                    <tbody>
                        $medsRowsHtml
                    </tbody>
                </table>
            </div>

            <!-- خطة الولادة والتوصيات وتوقيع الطبيب (Title: أخصائي د. شريف رضوان) -->
            <div class="section-box" style="margin-top: auto;">
                <div class="section-title">
                    <span>خطة الولادة والتوصيات</span>
                    <span class="en-sub">DELIVERY MANAGEMENT PLAN & CONSULTANT ATTESTATION</span>
                </div>
                <div class="attestation-grid">
                    <div class="plan-box">
                        <div class="history-list">
                            <div class="history-row">
                                <span class="k">طريقة الولادة المخطط لها:</span>
                                <span class="v text-rose font-bold">ولادة قيصرية مجدولة (Elective CS) / ولادة طبيعية</span>
                            </div>
                            <div class="history-row">
                                <span class="k">الموعد المستهدف للولادة:</span>
                                <span class="v text-navy font-bold">${patient.dating?.agreedEdd ?: "عند بلوغ الأسبوع 39"}</span>
                            </div>
                            <div class="history-row">
                                <span class="k">احتياطات المستشفى وبنك الدم:</span>
                                <span class="v font-bold">${if (isRhNegative) "تحضير حقنة Anti-D + فصيلة دم مطابقة" else "إجراءات روتينية معملية"}</span>
                            </div>
                            <div class="history-row">
                                <span class="k">التوصيات الخاصة:</span>
                                <span class="v">متابعة حركة الجنين دورياً ومراجعة الطوارئ فوراً عند أي نزف أو آلام مستمرة.</span>
                            </div>
                        </div>
                    </div>

                    <div class="signature-box">
                        <div>
                            <div class="doc-name">دكتور شريف رضوان</div>
                            <div class="doc-title">$doctorOfficialTitle</div>
                            <div class="stamp-placeholder">ختم وتوقيع الطبيب المعالج</div>
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
            <span>$clinicNameAr · $doctorOfficialTitle · هاتف وحجز: 0100000000 · رعاية طبية متكاملة</span>
            <span>صفحة 2 من $totalPages</span>
        </footer>
    </div>

    $page3Html

</body>
</html>
        """.trimIndent()
    }
}
