package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Spec-derived checks (P9) for the delivery-card colour tokens in Theme.kt:
 * every text/background token pair used by the age badge and the delivery boxes,
 * light AND dark, must reach WCAG AA 4.5:1 for normal-size text.
 * Placeholder text must use a full-opacity token (no alpha 0.65 colouring in code).
 *
 * The test reads the token names/values straight from Theme.kt (spec: "Tests may read them"),
 * so it stays valid if the fix adjusts token hex values; it fails loudly if the file cannot be
 * located, so it can never silently pass on stale data.
 */
class ThemeContrastTest {

    private fun locateUiDir(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        repeat(8) {
            val current = dir ?: return@repeat
            for (candidate in listOf(
                File(current, "app/src/main/java/com/sherifradwan/ancnative/ui"),
                File(current, "src/main/java/com/sherifradwan/ancnative/ui"),
            )) {
                if (candidate.isDirectory) return candidate
            }
            dir = current.parentFile
        }
        fail("Could not locate app/src/main/java/com/sherifradwan/ancnative/ui (cwd=${System.getProperty("user.dir")})")
        throw IllegalStateException("unreachable")
    }

    private val themeFile: File by lazy { File(locateUiDir(), "Theme.kt") }
    private val badgesFile: File by lazy { File(locateUiDir(), "ClinicalBadges.kt") }

    /** name -> 0xAARRGGBB int, from `val Name = Color(0x...)` lines in Theme.kt. */
    private fun parseColorTokens(): Map<String, Int> {
        val text = themeFile.readText()
        val regex = Regex("val\\s+(\\w+)\\s*=\\s*Color\\((0x[0-9A-Fa-f]{1,8})\\)")
        val tokens = mutableMapOf<String, Int>()
        for (m in regex.findAll(text)) {
            tokens[m.groupValues[1]] = m.groupValues[2].removePrefix("0x").toInt(16)
        }
        assertTrue(
            "Theme.kt colour tokens not found - test harness problem, not a product problem",
            tokens.containsKey("TagAgeText"),
        )
        return tokens
    }

    // WCAG 2.x relative luminance / contrast ratio from sRGB 8-bit components.
    private fun srgbChannelToLinear(channel: Double): Double =
        if (channel <= 0.03928) channel / 12.92 else Math.pow((channel + 0.055) / 1.055, 2.4)

    private fun relativeLuminance(argb: Int): Double {
        val r = srgbChannelToLinear(((argb shr 16) and 0xFF) / 255.0)
        val g = srgbChannelToLinear(((argb shr 8) and 0xFF) / 255.0)
        val b = srgbChannelToLinear((argb and 0xFF) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** (textToken, bgToken) pairs as used by PatientAgeBadge and the DeliveryTone box colours. */
    private fun expectedPairs(): List<Pair<String, String>> = listOf(
        // Age badge, light
        "TagAgeText" to "TagAgeBg",
        "TagAgeLabelText" to "TagAgeLabelBg",
        // Delivery boxes, light
        "DeliveryHospitalText" to "DeliveryHospitalBg",
        "DeliveryFeeText" to "DeliveryFeeBg",
        "DeliveryPlanText" to "DeliveryPlanBg",
        "DeliveryPostpartumText" to "DeliveryPostpartumBg",
        // Age badge, dark (label text stays TagAgeLabelText/white)
        "TagAgeDarkText" to "TagAgeDarkBg",
        "TagAgeLabelText" to "TagAgeLabelDarkBg",
        // Delivery boxes, dark
        "DeliveryHospitalDarkText" to "DeliveryHospitalDarkBg",
        "DeliveryFeeDarkText" to "DeliveryFeeDarkBg",
        "DeliveryPlanDarkText" to "DeliveryPlanDarkBg",
        "DeliveryPostpartumDarkText" to "DeliveryPostpartumDarkBg",
    )

    @Test
    fun ageBadge_andDeliveryTone_pairs_meetWcagAa_4_5() {
        val tokens = parseColorTokens()
        for ((textToken, bgToken) in expectedPairs()) {
            val t = tokens[textToken]
            val bg = tokens[bgToken]
            assertTrue("missing Theme.kt token $textToken", t != null)
            assertTrue("missing Theme.kt token $bgToken", bg != null)
            val ratio = contrastRatio(t!!, bg!!)
            assertTrue(
                "contrast $textToken on $bgToken = %.2f:1, needs >= 4.5:1".format(ratio),
                ratio >= 4.5,
            )
        }
    }

    @Test
    fun textTokens_areFullyOpaque_noAlphaFadedTextTokens() {
        val tokens = parseColorTokens()
        val textTokens = expectedPairs().map { it.first }.distinct()
        for (name in textTokens) {
            val value = tokens[name] ?: fail("missing Theme.kt token $name")
            val alpha = (value ushr 24) and 0xFF
            assertTrue("text token $name must be fully opaque (alpha=$alpha)", alpha == 0xFF)
        }
    }

    @Test
    fun deliveryBoxes_placeholder_isFullOpacityToken_notAlphaCopy() {
        val badges = badgesFile.readText()
        val faded = Regex("copy\\(alpha\\s*=\\s*0\\.[0-9]+f?\\)").findAll(badges).map { it.value }.toList()
        assertTrue(
            "P9: placeholder text must use a full-opacity token, not copy(alpha = 0.65); found: $faded",
            faded.isEmpty(),
        )
    }
}
