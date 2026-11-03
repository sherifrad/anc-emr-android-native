package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UltrasoundImpressionTest {
    @Test
    fun fullChoicesGiveTheOrderedSentence() {
        assertEquals(
            "Singleton pregnancy, cephalic presentation, anterior placenta, normal amniotic fluid.",
            ultrasoundImpressionFromChoices("Singleton", "Cephalic", "Anterior", "Normal"),
        )
    }

    @Test
    fun eachPlacentalSiteHasItsWording() {
        assertEquals("Anterior placenta.", ultrasoundImpressionFromChoices("", "", "Anterior", ""))
        assertEquals("Posterior placenta.", ultrasoundImpressionFromChoices("", "", "Posterior", ""))
        assertEquals("Fundal placenta.", ultrasoundImpressionFromChoices("", "", "Fundal", ""))
        assertEquals("Low-lying placenta.", ultrasoundImpressionFromChoices("", "", "Low-lying", ""))
    }

    @Test
    fun previaUsesItsOwnWording() {
        assertEquals("Placenta previa.", ultrasoundImpressionFromChoices("", "", "Previa", ""))
    }

    @Test
    fun fetalNumberWording() {
        assertEquals("Singleton pregnancy.", ultrasoundImpressionFromChoices("Singleton", "", "", ""))
        assertEquals("Twin pregnancy.", ultrasoundImpressionFromChoices("Twins", "", "", ""))
        assertEquals(
            "Higher-order multiple pregnancy.",
            ultrasoundImpressionFromChoices("Higher order", "", "", ""),
        )
    }

    @Test
    fun everyFluidHasItsWording() {
        assertEquals("Normal amniotic fluid.", ultrasoundImpressionFromChoices("", "", "", "Normal"))
        assertEquals("Oligohydramnios.", ultrasoundImpressionFromChoices("", "", "", "Oligohydramnios"))
        assertEquals("Polyhydramnios.", ultrasoundImpressionFromChoices("", "", "", "Polyhydramnios"))
    }

    @Test
    fun presentationIsLowerCasedInsideTheSentence() {
        assertEquals(
            "Singleton pregnancy, breech presentation.",
            ultrasoundImpressionFromChoices("Singleton", "Breech", "", ""),
        )
    }

    @Test
    fun partialChoicesInventNothing() {
        assertEquals("Cephalic presentation.", ultrasoundImpressionFromChoices("", "Cephalic", "", ""))
    }

    @Test
    fun allBlankGivesEmptyText() {
        assertEquals("", ultrasoundImpressionFromChoices("", "", "", ""))
        assertEquals("", ultrasoundImpressionFromChoices(" ", " ", " ", " "))
    }

    @Test
    fun unknownValueIsSkipped() {
        assertEquals(
            "Cephalic presentation.",
            ultrasoundImpressionFromChoices("Quintuplets", "Cephalic", "", "Unknown"),
        )
    }

    @Test
    fun mergeKeepsExistingWhenGeneratedIsBlank() {
        assertEquals("Doctor note", mergeImpression("Doctor note", ""))
    }

    @Test
    fun mergeUsesGeneratedWhenExistingIsBlank() {
        assertEquals("Cephalic presentation.", mergeImpression("", "Cephalic presentation."))
        assertEquals("Cephalic presentation.", mergeImpression("  \n", "Cephalic presentation."))
    }

    @Test
    fun mergeKeepsExistingWhenItAlreadyContainsGenerated() {
        assertEquals(
            "Seen today. Cephalic presentation. Follow up.",
            mergeImpression("Seen today. Cephalic presentation. Follow up.", "Cephalic presentation."),
        )
    }

    @Test
    fun mergeAppendsOnANewLineAndNeverOverwrites() {
        assertEquals(
            "Doctor note\nCephalic presentation.",
            mergeImpression("Doctor note  \n", "Cephalic presentation."),
        )
    }

    @Test
    fun mergingTwiceChangesNothing() {
        val once = mergeImpression("Doctor note", "Cephalic presentation.")
        assertEquals(once, mergeImpression(once, "Cephalic presentation."))
    }

    @Test
    fun normalScanConstants() {
        assertEquals("Singleton", NORMAL_FETAL_NUMBER)
        assertEquals("Cephalic", NORMAL_PRESENTATION)
        assertEquals("Normal", NORMAL_AMNIOTIC_FLUID)
    }
}
