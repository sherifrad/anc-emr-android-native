package com.sherifradwan.ancnative

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab

/** Test tag of each hub tab body (the tab bar button tag is `PatientHubTab.testTag`). */
fun PatientHubTab.contentTag(): String = when (this) {
    PatientHubTab.OVERVIEW -> "overviewTabContent"
    PatientHubTab.VISITS -> "visitsTabContent"
    PatientHubTab.SCANS -> "scansTabContent"
    PatientHubTab.LABS -> "labsTabContent"
    PatientHubTab.MEDS -> "medsTabContent"
    PatientHubTab.PLAN -> "planTabContent"
}

// Overview and Plan are plain scrolling columns; the other tabs are lazy lists that only compose visible rows.
private val columnTabs = setOf(PatientHubTab.OVERVIEW, PatientHubTab.PLAN)

/** The tab bar scrolls horizontally, so Labs, Meds and Plan are off screen until it is scrolled. */
fun ComposeTestRule.openHubTab(tab: PatientHubTab) {
    onNodeWithTag(tab.testTag).performScrollTo().performClick()
    waitForIdle()
}

/** Scrolls inside [tab]'s body until the node matching [target] is reachable. */
fun SemanticsNodeInteractionsProvider.scrollHubTabTo(tab: PatientHubTab, target: SemanticsMatcher) {
    if (tab in columnTabs) {
        // Scoped to the tab body so a node on a neighbouring tab cannot be matched and swipe the pager away.
        onNode(target and hasAnyAncestor(hasTestTag(tab.contentTag())), useUnmergedTree = true).performScrollTo()
    } else {
        onNodeWithTag(tab.contentTag(), useUnmergedTree = true).performScrollToNode(target)
    }
}

fun SemanticsNodeInteractionsProvider.scrollHubTabTo(tab: PatientHubTab, tag: String) =
    scrollHubTabTo(tab, hasTestTag(tag))
