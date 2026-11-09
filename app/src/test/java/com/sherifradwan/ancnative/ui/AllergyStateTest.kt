package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.sherifradwan.ancnative.model.DemoFixtures
import java.util.Locale

/** Spec-derived tests for classifyAllergy (ORACLE-A1..A4 and the string-level part of A5). Synthetic strings only. */
class AllergyStateTest {

    // ORACLE-A1
    @Test
    fun nullEmptyAndBlank_areNotRecorded() {
        listOf(null, "", " ", "   ", "\t", "\n", " \t\n ").forEach {
            assertEquals("input=$it", AllergyState.NotRecorded, classifyAllergy(it))
        }
    }

    // ORACLE-A2
    @Test
    fun explicitNegations_areExplicitlyNone() {
        listOf(
            "NKDA", "nkda", "Nkda", " None ", "NKA", "nka",
            "No known drug allergies", "No known drug allergies (NKDA)",
            "No known allergies.", "No known allergies", "No known allergy",
            "No allergies", "NO ALLERGIES", "no known drug allergies (nkda)",
            "No", "no", "NO", "No.", "Nil", "nil", "NIL", "None", "none", "NONE", "None.",
            "  NKDA.  ", "nkda.",
        ).forEach {
            assertEquals("input=\"$it\"", AllergyState.ExplicitlyNone, classifyAllergy(it))
        }
    }

    // ORACLE-A3
    @Test
    fun plainAllergen_isRecordedWithOriginalText() {
        assertEquals(AllergyState.Recorded("Penicillin"), classifyAllergy("Penicillin"))
    }

    @Test
    fun recorded_preservesOriginalCasingAndTrimsWhitespace() {
        assertEquals(AllergyState.Recorded("PENICILLIN"), classifyAllergy("PENICILLIN"))
        assertEquals(AllergyState.Recorded("sulfa Rash"), classifyAllergy("  sulfa Rash \n"))
        assertEquals(AllergyState.Recorded("Latex"), classifyAllergy("\tLatex  "))
    }

    // ORACLE-A4
    @Test
    fun negationFollowedByContent_isRecordedNeverNone() {
        assertEquals(
            AllergyState.Recorded("No known allergies except penicillin"),
            classifyAllergy("No known allergies except penicillin"),
        )
        assertEquals(
            AllergyState.Recorded("NKDA but sulfa rash"),
            classifyAllergy("NKDA but sulfa rash"),
        )
        assertEquals(
            AllergyState.Recorded("No known allergies except penicillin"),
            classifyAllergy("  No known allergies except penicillin  "),
        )
    }

    @Test
    fun negationPrefixOrContainment_isRecorded() {
        listOf(
            "None, but latex",
            "No, penicillin",
            "Nil except peanuts",
            "NKDA, mild rash to sulfa",
            "No known drug allergies; shellfish",
            "Nonsteroidal anti-inflammatory",
            "Nilotinib",
            "Nuts",
            "Not sure",
            "No known",
        ).forEach {
            val result = classifyAllergy(it)
            assertTrue("input=\"$it\" got $result", result is AllergyState.Recorded)
            assertEquals(it.trim(), (result as AllergyState.Recorded).text)
        }
    }

    // ORACLE-A5 (string level): the fixture string must not be Recorded (red chip).
    @Test
    fun fixtureNkdaString_isNotRecorded() {
        val result = classifyAllergy("No known drug allergies (NKDA)")
        assertEquals(AllergyState.ExplicitlyNone, result)
        assertFalse(result is AllergyState.Recorded)
    }

    @Test
    fun trailingPeriodAndCaseVariants_ofFixtureString() {
        assertEquals(AllergyState.ExplicitlyNone, classifyAllergy("No known drug allergies (NKDA)."))
        assertEquals(AllergyState.ExplicitlyNone, classifyAllergy("NO KNOWN DRUG ALLERGIES (NKDA)"))
    }

    @Test
    fun everyNkdaFixturePatient_isExplicitlyNone() {
        val nkda = DemoFixtures.patients.filter { it.allergyHistory?.contains("NKDA") == true }
        assertTrue("expected NKDA fixtures", nkda.isNotEmpty())
        nkda.forEach {
            assertEquals(it.allergyHistory, AllergyState.ExplicitlyNone, classifyAllergy(it.allergyHistory))
        }
    }

    @Test
    fun internalWhitespaceAndTabBeforePeriod_areExplicitlyNone() {
        assertEquals(AllergyState.ExplicitlyNone, classifyAllergy("No  known  drug  allergies"))
        assertEquals(AllergyState.ExplicitlyNone, classifyAllergy("NKDA\t."))
    }

    @Test
    fun turkishLocale_doesNotBreakCaseInsensitiveMatch() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(AllergyState.ExplicitlyNone, classifyAllergy("NIL"))
            assertEquals(AllergyState.ExplicitlyNone, classifyAllergy("NKDA"))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
