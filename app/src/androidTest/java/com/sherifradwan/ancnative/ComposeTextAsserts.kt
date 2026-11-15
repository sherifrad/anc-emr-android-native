package com.sherifradwan.ancnative

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/** Matches the node carrying [tag] and any of its descendants (hasAnyAncestor alone excludes the tagged node). */
fun inTagged(tag: String): SemanticsMatcher = hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag))

/** Fails when the text node matched by [matcher] is clipped or ellipsized in its own layout. */
fun SemanticsNodeInteractionsProvider.assertTextNotClipped(label: String, matcher: SemanticsMatcher) {
    val node = onNode(matcher, useUnmergedTree = true)
    val results = mutableListOf<TextLayoutResult>()
    node.fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
    assertTrue("'$label' has no text layout to check", results.isNotEmpty())
    results.forEach { layout ->
        // hasVisualOverflow is not used: text that shrinks to its content width reports
        // size.width < laid-out paragraph width even when nothing is clipped. Check clipping directly.
        assertTrue("'$label' clipped vertically", layout.size.height >= layout.multiParagraph.height.toInt())
        for (line in 0 until layout.lineCount) {
            assertTrue("'$label' line $line clipped horizontally", layout.getLineRight(line) <= layout.size.width + 1f)
            assertFalse("'$label' ellipsized on line $line", layout.isLineEllipsized(line))
        }
    }
}

/**
 * Clip check for centred or end-aligned text: a shrink-wrapped aligned line is laid out against the full paragraph
 * width, so getLineRight can pass size.width while the line is drawn whole. Compares each line's own width instead.
 */
fun assertAlignedTextNotClipped(label: String, layout: TextLayoutResult) {
    assertTrue("'$label' clipped vertically", layout.size.height >= layout.multiParagraph.height.toInt())
    for (line in 0 until layout.lineCount) {
        val width = layout.getLineRight(line) - layout.getLineLeft(line)
        assertTrue("'$label' line $line wider than its node ($width > ${layout.size.width})", width <= layout.size.width + 1f)
        assertFalse("'$label' ellipsized on line $line", layout.isLineEllipsized(line))
    }
}

/**
 * Every text under [tag] is whole (no vertical cut, no line past its node, no ellipsis) and sits inside the [tag]
 * container horizontally, within 1dp.
 */
fun SemanticsNodeInteractionsProvider.assertAllTextWhole(tag: String) {
    val container = onNode(hasTestTag(tag), useUnmergedTree = true).getUnclippedBoundsInRoot()
    val nodes = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and inTagged(tag), useUnmergedTree = true)
        .fetchSemanticsNodes()
    assertTrue("no text found in $tag", nodes.isNotEmpty())
    nodes.forEach { n ->
        val layouts = mutableListOf<TextLayoutResult>()
        n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        val layout = layouts.firstOrNull() ?: return@forEach
        val text = n.config[SemanticsProperties.Text].joinToString(" ")
        val byId = SemanticsMatcher("node ${n.id}") { it.id == n.id }
        val align = layout.layoutInput.style.textAlign
        if (align == TextAlign.Center || align == TextAlign.End) assertAlignedTextNotClipped(text, layout)
        else assertTextNotClipped(text, byId)
        val b = onNode(byId, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("'$text' starts outside $tag (${b.left} < ${container.left})", b.left >= container.left - 1.dp)
        assertTrue("'$text' runs past $tag (${b.right} > ${container.right})", b.right <= container.right + 1.dp)
    }
}

fun SemanticsNodeInteractionsProvider.assertNotEllipsized(text: String, ancestorTag: String) =
    assertTextNotClipped(text, hasText(text, substring = true) and inTagged(ancestorTag))

/**
 * Fails when the touch area of [node] is shorter than [minHeightDp] (or narrower than [minWidthDp]) or sticks out of
 * [host]. [density] is pixels per dp of the fixture.
 */
fun assertTouchTargetInsideHost(
    label: String,
    node: SemanticsNode,
    host: Rect,
    density: Float,
    minHeightDp: Float,
    minWidthDp: Float = 0f,
    tolerancePx: Float = 1f,
) {
    val touch = node.touchBoundsInRoot
    assertTrue(
        "$label touch target is too short: height=${touch.height}px, expected>=${minHeightDp * density}px",
        touch.height + tolerancePx >= minHeightDp * density,
    )
    assertTrue(
        "$label touch target is too narrow: width=${touch.width}px, expected>=${minWidthDp * density}px",
        touch.width + tolerancePx >= minWidthDp * density,
    )
    assertTrue("$label touch target is outside host left: touch=$touch host=$host", touch.left + tolerancePx >= host.left)
    assertTrue("$label touch target is outside host top: touch=$touch host=$host", touch.top + tolerancePx >= host.top)
    assertTrue("$label touch target is outside host right: touch=$touch host=$host", touch.right <= host.right + tolerancePx)
    assertTrue("$label touch target is outside host bottom: touch=$touch host=$host", touch.bottom <= host.bottom + tolerancePx)
}
