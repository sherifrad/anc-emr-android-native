package com.sherifradwan.ancnative.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsRtlTextTest {
    @Test fun arabicNameIsRtl() = assertTrue(isRtlText("فاطمة أحمد"))

    @Test fun englishNameIsNotRtl() = assertFalse(isRtlText("Fatma Ahmed"))

    @Test fun mixedStartingWithArabicIsRtl() = assertTrue(isRtlText("فاطمة Ahmed"))

    @Test fun mixedStartingWithEnglishIsNotRtl() = assertFalse(isRtlText("Ahmed فاطمة"))

    @Test fun digitsThenArabicIsRtl() = assertTrue(isRtlText("123 - فاطمة"))

    @Test fun punctuationAndSpacesThenArabicIsRtl() = assertTrue(isRtlText("  (\"فاطمة"))

    @Test fun digitsThenEnglishIsNotRtl() = assertFalse(isRtlText("7 Ahmed"))

    @Test fun emptyIsNotRtl() = assertFalse(isRtlText(""))

    @Test fun neutralOnlyIsNotRtl() = assertFalse(isRtlText("123 - ()"))
}
