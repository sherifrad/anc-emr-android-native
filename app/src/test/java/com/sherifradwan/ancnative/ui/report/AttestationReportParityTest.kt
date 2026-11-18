package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.security.SecuritySettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests verifying clinical attestation parity and HTML escaping in [PatientPrintHtmlGenerator].
 *
 * Verifies:
 * - settings = null and default [SecuritySettingsState] produce identical HTML with default doctor attestation.
 * - Custom settings appear in the output while defaults do not.
 * - Malicious or special characters (<script>, &, <, >) in attestation fields are strictly HTML-escaped.
 */
class AttestationReportParityTest {

    private val asOfDate = LocalDate.of(2026, 10, 28)
    private val testPatient: DemoPatient = DemoPatient(
        id = "parity-patient-1",
        caseId = "PARITY-P-1",
        displayName = "Parity Test Patient",
        dateOfBirth = null,
        phone = null,
        visits = emptyList(),
        riskLevel = "Low Risk",
        bloodGroupText = "O+",
    )

    @Test
    fun nullSettingsAndDefaultSettings_produceIdenticalHtmlWithDefaultDoctorAttestation() {
        val htmlNull = PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = testPatient,
            settings = null,
            asOfDate = asOfDate,
        )
        val htmlDefault = PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = testPatient,
            settings = SecuritySettingsState(),
            asOfDate = asOfDate,
        )

        // Parity: null settings and default settings must produce byte-for-byte identical HTML
        assertEquals(htmlNull, htmlDefault)

        // Contains the exact default doctor name, titles, and clinic phone
        assertTrue("Contains English doctor name", htmlDefault.contains("Dr. Sherif Radwan"))
        assertTrue(
            "Contains English specialty (HTML-escaped & -> &amp;)",
            htmlDefault.contains("Specialist of Obstetrics, Gynecology &amp; Laparoscopy"),
        )
        assertTrue("Contains Arabic doctor name", htmlDefault.contains("شريف جمال رضوان"))
        assertTrue("Contains Arabic specialty", htmlDefault.contains("أخصائي التوليد وأمراض النساء وتأخر الحمل"))
        assertTrue("Contains clinic phone in footer", htmlDefault.contains("Tel <span class=\"col-date\">01201720808</span>"))
    }

    @Test
    fun customSettings_renderCustomValuesAndExcludeDefaults() {
        val customSettings = SecuritySettingsState(
            clinicName = "Alexandria Maternity Center",
            doctorNameEn = "Dr. Alexandria Smith",
            doctorSpecialtyEn = "Consultant Perinatologist",
            doctorNameAr = "د. ألكسندريا سميث",
            doctorSpecialtyAr = "استشارية طب الأجنة",
            clinicPhone = "01009998888",
        )

        val htmlCustom = PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = testPatient,
            settings = customSettings,
            asOfDate = asOfDate,
        )

        // Custom attestation values must appear
        assertTrue("Contains custom English name", htmlCustom.contains("Dr. Alexandria Smith"))
        assertTrue("Contains custom English specialty", htmlCustom.contains("Consultant Perinatologist"))
        assertTrue("Contains custom Arabic name", htmlCustom.contains("د. ألكسندريا سميث"))
        assertTrue("Contains custom Arabic specialty", htmlCustom.contains("استشارية طب الأجنة"))
        assertTrue("Contains custom clinic phone", htmlCustom.contains("Tel <span class=\"col-date\">01009998888</span>"))

        // Default attestation values must NOT appear
        assertFalse("Default English name excluded", htmlCustom.contains("Dr. Sherif Radwan"))
        assertFalse("Default English specialty excluded", htmlCustom.contains("Specialist of Obstetrics, Gynecology"))
        assertFalse("Default Arabic name excluded", htmlCustom.contains("شريف جمال رضوان"))
        assertFalse("Default Arabic specialty excluded", htmlCustom.contains("أخصائي التوليد وأمراض النساء"))
        assertFalse("Default clinic phone excluded", htmlCustom.contains("01201720808"))
    }

    @Test
    fun customSettings_htmlEscapeSpecialCharactersAndScriptTags() {
        val maliciousSettings = SecuritySettingsState(
            doctorNameEn = "Dr. Jane & Smith <script>alert(1)</script>",
            doctorSpecialtyEn = "Maternal & Fetal <Consultant>",
            doctorNameAr = "د. <أحمد> & شريكه",
            doctorSpecialtyAr = "استشاري <أمراض النساء> & التوليد",
            clinicPhone = "01201720808",
        )

        val html = PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = testPatient,
            settings = maliciousSettings,
            asOfDate = asOfDate,
        )

        // Raw malicious tags must never appear
        assertFalse("No raw opening script tag", html.contains("<script>"))
        assertFalse("No raw closing script tag", html.contains("alert(1)</script>"))
        assertFalse("No raw consultant tag", html.contains("<Consultant>"))
        assertFalse("No raw Arabic tag", html.contains("<أحمد>"))
        assertFalse("No raw Arabic specialty tag", html.contains("<أمراض النساء>"))

        // Escaped versions must appear
        assertTrue(
            "English name HTML-escaped",
            html.contains("Dr. Jane &amp; Smith &lt;script&gt;alert(1)&lt;/script&gt;"),
        )
        assertTrue(
            "English specialty HTML-escaped",
            html.contains("Maternal &amp; Fetal &lt;Consultant&gt;"),
        )
        assertTrue(
            "Arabic name HTML-escaped",
            html.contains("د. &lt;أحمد&gt; &amp; شريكه"),
        )
        assertTrue(
            "Arabic specialty HTML-escaped",
            html.contains("استشاري &lt;أمراض النساء&gt; &amp; التوليد"),
        )
    }
}
