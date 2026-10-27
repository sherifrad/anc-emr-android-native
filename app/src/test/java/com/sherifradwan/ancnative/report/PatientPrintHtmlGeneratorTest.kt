package com.sherifradwan.ancnative.report

import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.ui.report.PatientPrintHtmlGenerator
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PatientPrintHtmlGeneratorTest {

    @Test
    fun testGenerateHtmlContainsTwoPagesAndClinicBranding() {
        val patient = DemoFixtures.patients.first()
        val settings = SecuritySettingsState(
            clinicName = "Dr. Sherif Radwan Clinic",
            clinicSubtitle = "Dr. Sherif Radwan Clinic · Maternal-Fetal Medicine & Advanced Obstetrics",
        )

        val html = PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = patient,
            settings = settings,
            asOfDate = LocalDate.parse("2026-10-12"),
        )

        // Verify Clinic Header and Branding
        assertTrue("Must contain Dr. Sherif Radwan Clinic name", html.contains("Dr. Sherif Radwan Clinic"))
        assertTrue("Must contain Maternal and Fetal Medicine specialty", html.contains("Maternal-Fetal Medicine & Advanced Obstetrics"))
        assertTrue("Must contain Consultant title", html.contains("Consultant of Obstetrics, Gynecology & Laparoscopy"))

        // Verify Uniform Meta Alignment
        assertTrue("Must contain uniform meta block", html.contains("uniform-meta"))
        assertTrue("Must contain CLINICAL RECORD badge", html.contains("CLINICAL RECORD"))

        // Verify Patient Card enhancements
        assertTrue("Must contain patient case ID", html.contains(patient.caseId))
        assertTrue("Must contain patient display name", html.contains(patient.displayName))
        assertTrue("Must contain phone on separate line", html.contains("class=\"pt-phone\""))

        // Verify Section Titles & Tables
        assertTrue("Must contain PREGNANCY DATING without Matrix", html.contains("PREGNANCY DATING"))
        assertTrue("Must contain previous pregnancies title", html.contains("PREVIOUS PREGNANCIES & OUTCOMES"))
        assertTrue("Must contain visits log title", html.contains("ANTENATAL CARE VISITS LOG"))
        assertTrue("Must contain ultrasound title", html.contains("ULTRASOUND BIOMETRY & SURVEILLANCE"))
        assertTrue("Must contain lab investigations title", html.contains("LABORATORY INVESTIGATIONS"))
        assertTrue("Must contain active medications title", html.contains("ACTIVE MEDICATIONS & PROPHYLAXIS"))
        assertTrue("Must contain delivery management plan title", html.contains("DELIVERY MANAGEMENT PLAN & CONSULTANT ATTESTATION"))

        // Verify Centered Cells styling
        assertTrue("Must contain center alignment for table cells", html.contains("text-align: center !important;"))
    }
}
