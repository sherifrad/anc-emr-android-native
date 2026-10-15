package com.sherifradwan.ancnative.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientValidationTest {
    @Test
    fun requiredIdentityMustBeTrimmedAndNonBlank() {
        assertFalse(validInput(caseId = "", displayName = "Synthetic Patient").validationErrors().isEmpty())
        assertFalse(validInput(caseId = " CASE-1", displayName = "Synthetic Patient").validationErrors().isEmpty())
        assertFalse(validInput(caseId = "CASE-1", displayName = " ").validationErrors().isEmpty())
        assertFalse(validInput(caseId = "CASE-1", displayName = "Synthetic Patient ").validationErrors().isEmpty())
    }

    @Test
    fun nullableClinicalFieldsRemainValidWhenUnknown() {
        assertTrue(validInput().validationErrors().isEmpty())
    }

    @Test
    fun gpaIsAllUnknownOrAllBoundedBaseTenValues() {
        assertTrue(
            validInput(
                gravida = PatientVocabulary.MAX_SAFE_GPA,
                parity = 0,
                abortion = 1,
            ).validationErrors().isEmpty(),
        )
        assertFalse(validInput(gravida = 1, parity = null, abortion = 0).validationErrors().isEmpty())
        assertFalse(validInput(gravida = -1, parity = 0, abortion = 0).validationErrors().isEmpty())
        assertFalse(
            validInput(
                gravida = PatientVocabulary.MAX_SAFE_GPA + 1,
                parity = 0,
                abortion = 0,
            ).validationErrors().isEmpty(),
        )
    }

    @Test
    fun controlledVocabulariesAreCaseSensitiveAndNullable() {
        assertTrue(validInput(bloodGroupText = "AB-", riskLevel = "High Risk").validationErrors().isEmpty())
        assertFalse(validInput(bloodGroupText = "ab-", riskLevel = "High Risk").validationErrors().isEmpty())
        assertFalse(validInput(bloodGroupText = "AB-", riskLevel = "high risk").validationErrors().isEmpty())
    }

    private fun validInput(
        caseId: String = "CASE-1",
        displayName: String = "Synthetic Patient",
        bloodGroupText: String? = null,
        riskLevel: String? = null,
        gravida: Long? = null,
        parity: Long? = null,
        abortion: Long? = null,
    ) = PatientClinicalInput(
        caseId = caseId,
        displayName = displayName,
        dateOfBirth = null,
        phone = null,
        ageText = null,
        addressText = null,
        bloodGroupText = bloodGroupText,
        riskLevel = riskLevel,
        gravida = gravida,
        parity = parity,
        abortion = abortion,
        obstetricNotes = null,
    )
}
