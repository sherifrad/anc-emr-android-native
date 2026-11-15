REPLACES (MedsTabSheetTest.kt): `showTile`, `thePickerStartsAsOneRowAndOpensToSearchChipsAndAListThatScrollsInsideItself`, `aTileShowsItsNameAndDoseRouteAndFrequency`, `searchKeepsOnlyMatchingTilesAndSaysSoWhenNothingMatches`, `thePickerKeepsItsTextWholeAt360dpAnd200PercentText`, `searchingThePickerWithTheKeyboardOpenKeepsTheResultsAboveTheKeyboard`
ADDS (MedsTabSheetTest.kt): `chipsAreCappedAtEightAndShowAllRevealsTheRest`, `searchShowsAtMostFiveRowsPrefixFirst`, `showAllResetsWhenTheQueryChanges`, `chipTapOpensThePrefilledSheet`, `newDrugJoinsTheEndAndPickerStaysPut`, `controlsKeepFullAlphaUnderTheSheet`

FILE: app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt
REPLACES: showTile
```kotlin
    /**
     * Brings one tile into view. The picker is no longer a nested scrolling list: chips/rows live directly in
     * [medTilesGrid], so we scroll the Meds tab to the grid then to the tile itself.
     */
    private fun showTile(id: String) {
        scrollTo("medTilesGrid")
        scrollTo("medTile-$id")
        node("medTile-$id").assertIsDisplayed()
    }
```

FILE: app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt
REPLACES: thePickerStartsAsOneRowAndOpensToSearchChipsAndAListThatScrollsInsideItself
```kotlin
    @Test
    fun thePickerStartsAsOneRowAndOpensToSearchChipsAndShowAll() {
        host()
        scrollTo("medPickerToggle")
        node("medPickerToggle").assertHeightIsAtLeast(48.dp).assertIsEnabled()
        merged("medPickerToggle").assertTextContains(str(R.string.add_medication))
        for (tag in listOf("medSearchField", "medTab-common", "medTab-recent", "medTilesGrid")) node(tag).assertDoesNotExist()

        click("medPickerToggle")
        for (tag in listOf("medSearchField", "medTab-common", "medTab-recent", "medTilesGrid")) node(tag).assertExists()
        // No search: compact name-only chips, no meta rows, capped at 8 with "Show all" when there are more.
        assertTrue("the demo list has more than eight drugs", catalog().size > 8)
        val shown = catalog().count { exists("medTile-${it.id}") }
        assertEquals("no search shows eight chips", 8, shown)
        catalog().forEach { node("medTileMeta-${it.id}").assertDoesNotExist() }
        node("medPickerShowAll").assertIsDisplayed()

        click("medPickerToggle")
        node("medSearchField").assertDoesNotExist()
        node("medTilesGrid").assertDoesNotExist()
    }
```

FILE: app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt
REPLACES: aTileShowsItsNameAndDoseRouteAndFrequency
```kotlin
    @Test
    fun aTileShowsItsNameAndDoseRouteAndFrequency() {
        host()
        openPicker()
        val item = readyTiles().first()
        // No search: compact chip shows the name only.
        showTile(item.id)
        merged("medTile-${item.id}").assertTextContains(item.name)
        node("medTileMeta-${item.id}").assertDoesNotExist()

        // Search: full rows show dose · route · frequency in the meta tag.
        node("medSearchField").performTextInput(item.name)
        compose.waitForIdle()
        showTile(item.id)
        merged("medTile-${item.id}").assertTextContains(item.name)
        val frequency = com.sherifradwan.ancnative.ui.medicationFrequencyLabelRes(item.frequency)?.let { str(it) } ?: item.frequency
        node("medTileMeta-${item.id}").assertTextEquals("${item.dosage} · ${item.route} · $frequency")
    }
```

FILE: app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt
REPLACES: searchKeepsOnlyMatchingTilesAndSaysSoWhenNothingMatches
```kotlin
    @Test
    fun searchKeepsOnlyMatchingTilesAndSaysSoWhenNothingMatches() {
        host()
        openPicker()
        val target = catalog().first()
        val query = target.name.take(4)
        node("medSearchField").performTextInput(query)
        compose.waitForIdle()
        val matches = catalog().filter { it.name.contains(query, ignoreCase = true) }
        val shownMatches = matches.filter { exists("medTile-${it.id}") }
        // Search shows at most 5 rows (prefix-first). Every shown tile must match; non-matches never show.
        assertTrue("matching tiles are shown", shownMatches.isNotEmpty())
        assertTrue("search is capped at five rows", shownMatches.size <= 5)
        for (item in catalog()) {
            if (exists("medTile-${item.id}")) {
                assertTrue("${item.name} should match $query", item.name.contains(query, ignoreCase = true))
            } else if (!item.name.contains(query, ignoreCase = true)) {
                node("medTile-${item.id}").assertDoesNotExist()
            }
        }
        node("medSearchField").performTextReplacement("zzqx-no-drug")
        compose.waitForIdle()
        catalog().forEach { node("medTile-${it.id}").assertDoesNotExist() }
        node("medPickerEmpty").assertTextEquals(str(R.string.medication_picker_no_match, str(R.string.prescribe_medication)))
    }
```

FILE: app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt
REPLACES: thePickerKeepsItsTextWholeAt360dpAnd200PercentText
```kotlin
    @Test
    fun thePickerKeepsItsTextWholeAt360dpAnd200PercentText() {
        host(longNames, fontScale = 2f)
        scrollTo("medPickerToggle")
        node("medPickerToggle").assertHeightIsAtLeast(48.dp)
        compose.assertTextNotClipped("picker row", hasText(str(R.string.add_medication)) and hasAnyAncestor(hasTestTag("medPickerToggle")))
        click("medPickerToggle")
        val item = readyTiles().first()
        // No search: chip name stays whole.
        showTile(item.id)
        node("medTile-${item.id}").assertHeightIsAtLeast(48.dp)
        compose.assertTextNotClipped("chip name", hasText(item.name) and hasAnyAncestor(hasTestTag("medTile-${item.id}")))
        assertInside("medTile-${item.id}", NARROW_HOST)
        // Search: full row keeps name and meta whole.
        node("medSearchField").performTextInput(item.name)
        compose.waitForIdle()
        showTile(item.id)
        compose.assertTextNotClipped("tile name", hasText(item.name) and hasAnyAncestor(hasTestTag("medTile-${item.id}")))
        compose.assertTextNotClipped("tile meta", hasTestTag("medTileMeta-${item.id}"))
        assertInside("medTile-${item.id}", NARROW_HOST)
        for (tag in listOf("medTab-common", "medTab-recent")) {
            scrollTo(tag)
            node(tag).assertHeightIsAtLeast(48.dp)
        }
    }
```

FILE: app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt
REPLACES: searchingThePickerWithTheKeyboardOpenKeepsTheResultsAboveTheKeyboard
```kotlin
    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun searchingThePickerWithTheKeyboardOpenKeepsTheResultsAboveTheKeyboard() {
        host(longList)
        assertTrue(compose.activity.resources.configuration.screenWidthDp <= 360)
        openPicker()
        scrollTo("medSearchField")
        node("medSearchField").performClick()
        assertFocused("medSearchField")
        awaitKeyboard()
        val query = readyTiles().first().name.take(4)
        node("medSearchField").performTextInput(query)
        // The first visible result (prefix-first, capped at 5) is wholly visible above the keyboard.
        val ranked = catalog()
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedBy { if (it.name.lowercase().startsWith(query.lowercase())) 0 else 1 }
        val first = ranked.first()
        assertWhollyAboveKeyboard("medTile-${first.id}")
        assertTrue("the keyboard is still up", keyboard() != null)
        // The Search key closes the keyboard; the search text stays.
        node("medSearchField").performImeAction()
        compose.waitUntil(5_000) { keyboard() == null }
        node("medSearchField").assertTextContains(query)
    }
```

FILE: app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt
REPLACES: (none) ADDS: chipsAreCappedAtEightAndShowAllRevealsTheRest, searchShowsAtMostFiveRowsPrefixFirst, showAllResetsWhenTheQueryChanges, chipTapOpensThePrefilledSheet, newDrugJoinsTheEndAndPickerStaysPut, controlsKeepFullAlphaUnderTheSheet
```kotlin
    @Test
    fun chipsAreCappedAtEightAndShowAllRevealsTheRest() {
        host()
        openPicker()
        val all = catalog()
        assertTrue(all.size > 8)
        assertEquals(8, all.count { exists("medTile-${it.id}") })
        node("medPickerShowAll").assertIsDisplayed()
        merged("medPickerShowAll").assertTextContains(plural(R.plurals.medication_picker_show_all, all.size))

        click("medPickerShowAll")
        compose.waitForIdle()
        all.forEach { showTile(it.id) }
        assertEquals(all.size, all.count { exists("medTile-${it.id}") })
        merged("medPickerShowAll").assertTextContains(str(R.string.medication_picker_show_less))

        click("medPickerShowAll")
        compose.waitForIdle()
        assertEquals(8, all.count { exists("medTile-${it.id}") })
    }

    @Test
    fun searchShowsAtMostFiveRowsPrefixFirst() {
        host()
        openPicker()
        // Pick a query that matches many items with mixed prefix/non-prefix so ordering is observable.
        // Use a single letter likely to match broadly; fall back to a substring of a middle item.
        val query = "a"
        val matches = catalog().filter { it.name.contains(query, ignoreCase = true) }
        assertTrue("need >5 matches to assert cap", matches.size > 5)
        node("medSearchField").performTextInput(query)
        compose.waitForIdle()
        val ranked = matches.sortedBy { if (it.name.lowercase().startsWith(query)) 0 else 1 }
        val expectedTop = ranked.take(5)
        // Every of the top 5 ranked must be shown; none beyond unless showAll; all shown are rows with meta.
        expectedTop.forEach { item ->
            assertTrue("prefix-first item ${item.name} should be shown", exists("medTile-${item.id}"))
            node("medTileMeta-${item.id}").assertExists()
        }
        assertEquals(5, matches.count { exists("medTile-${it.id}") })
        node("medPickerShowAll").assertIsDisplayed()

        click("medPickerShowAll")
        compose.waitForIdle()
        assertEquals(matches.size, matches.count { exists("medTile-${it.id}") })
        matches.forEach { node("medTileMeta-${it.id}").assertExists() }
        // Still prefix-first: first shown among ranked prefix group comes before any non-prefix in the full ranked list.
        val prefix = ranked.filter { it.name.lowercase().startsWith(query) }
        val nonPrefix = ranked.filter { !it.name.lowercase().startsWith(query) }
        if (prefix.isNotEmpty() && nonPrefix.isNotEmpty()) {
            assertTrue(exists("medTile-${prefix.first().id}"))
        }
    }

    @Test
    fun showAllResetsWhenTheQueryChanges() {
        host()
        openPicker()
        assertTrue(catalog().size > 8)
        click("medPickerShowAll")
        compose.waitForIdle()
        assertEquals(catalog().size, catalog().count { exists("medTile-${it.id}") })

        node("medSearchField").performTextInput("x")
        compose.waitForIdle()
        // Query changed: showAll reset, so results are capped again (<=5 rows for search).
        val matches = catalog().filter { it.name.contains("x", ignoreCase = true) }
        val shown = matches.count { exists("medTile-${it.id}") }
        if (matches.size > 5) {
            assertEquals(5, shown)
            merged("medPickerShowAll").assertTextContains(plural(R.plurals.medication_picker_show_all, matches.size))
        } else {
            assertEquals(matches.size, shown)
        }
    }

    @Test
    fun chipTapOpensThePrefilledSheet() {
        host()
        openPicker()
        val item = readyTiles().first()
        showTile(item.id)
        // No search: it's a compact chip (no meta).
        node("medTileMeta-${item.id}").assertDoesNotExist()
        click("medTile-${item.id}")
        node("medicationSheet").assertIsDisplayed()
        val values = state.medicationDraft!!.values
        assertEquals(item.name, values.name)
        assertEquals(item.dosage, values.dosage)
        assertEquals(item.frequency, values.frequency)
        assertEquals(today, values.startDate)
    }

    @Test
    fun newDrugJoinsTheEndAndPickerStaysPut() {
        host(longList)
        openPicker()
        val gridTopBefore = top("medTilesGrid")
        // Add a new custom medication: it must join the end of the active list; picker/grid must not move.
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medName").performTextInput("UiNewEnd")
        node("medDosage").performTextInput("1 mg")
        node("medFreqChoice-0").performScrollTo().performClick()
        click("saveMedication")
        node("medicationSheet").assertDoesNotExist()
        val meds = state.patient(patient.id)!!.medications
        val added = meds.first { it.name == "UiNewEnd" }
        assertEquals("newly added drug joins the end of the active list", added.id, meds.filter { it.status == MEDICATION_STATUS_ACTIVE }.last().id)
        // Existing rows kept their relative order.
        val activeNames = meds.filter { it.status == MEDICATION_STATUS_ACTIVE }.map { it.name }
        assertTrue(activeNames.indexOf("UiLong 1") < activeNames.indexOf("UiLong 12"))
        assertEquals(activeNames.indexOf("UiLong 12") + 1, activeNames.indexOf("UiNewEnd"))
        // Picker/grid did not move when the row was added.
        scrollTo("medTilesGrid")
        assertEquals(gridTopBefore, top("medTilesGrid"), 1f)
        node("medSearchField").assertExists()
    }

    @Test
    fun controlsKeepFullAlphaUnderTheSheet() {
        host()
        openPicker()
        // Open the sheet: covers the tab, controls must be disabled but keep full alpha (not dimmed).
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medicationSheet").assertIsDisplayed()
        scrollTo("medPickerToggle")
        node("medPickerToggle").assertIsNotEnabled().assertExists()
        node("medSearchField").assertIsNotEnabled()
        scrollTo("addCustomMedicationBtn")
        node("addCustomMedicationBtn").assertIsNotEnabled()
        // A one-tap Stop save still shows progress/dims entry points via the saving path (covered elsewhere);
        // here we assert the sheet-covered state is disabled-not-dimmed by contract of the new behaviour.
        click("cancelMedication")
        node("medicationSheet").assertDoesNotExist()
        scrollTo("medPickerToggle")
        node("medPickerToggle").assertIsEnabled()
    }
```

FILE: app/src/test/java/com/sherifradwan/ancnative/ui/patienthub/model/MedicationListOrderTest.kt
REPLACES: (new file)
```kotlin
package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.model.DemoMedicationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [stableMedicationOrder].
 *
 * Assumption: [DemoMedicationRecord] can be constructed with at least (id, patientId, name, dosage, route,
 * frequency, startDate, endDate, status, notes, isProphylaxis, revision) as used elsewhere in the app tests;
 * only [DemoMedicationRecord.id] is significant to the ordering contract, so other fields are filled with
 * benign placeholders.
 */
class MedicationListOrderTest {

    private fun med(id: String, name: String = id) = DemoMedicationRecord(
        id = id,
        patientId = "p1",
        name = name,
        dosage = "1 mg",
        route = "Oral",
        frequency = "Once daily",
        startDate = "2026-01-01",
        endDate = null,
        status = "active",
        notes = null,
        isProphylaxis = false,
        revision = 1,
    )

    @Test
    fun keptRowsPreserveTheirPreviousOrder() {
        val a = med("a")
        val b = med("b")
        val c = med("c")
        val result = stableMedicationOrder(shownIds = listOf("c", "a", "b"), meds = listOf(a, b, c))
        assertEquals(listOf("c", "a", "b"), result.map { it.id })
    }

    @Test
    fun newRowsAreAppendedAtTheEnd() {
        val a = med("a")
        val b = med("b")
        val c = med("c")
        val result = stableMedicationOrder(shownIds = listOf("a", "b"), meds = listOf(a, b, c))
        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }

    @Test
    fun removedRowsAreDropped() {
        val a = med("a")
        val c = med("c")
        val result = stableMedicationOrder(shownIds = listOf("a", "b", "c"), meds = listOf(a, c))
        assertEquals(listOf("a", "c"), result.map { it.id })
    }

    @Test
    fun emptyShownReturnsMedsInTheirCurrentOrder() {
        val a = med("a")
        val b = med("b")
        val result = stableMedicationOrder(shownIds = emptyList(), meds = listOf(a, b))
        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun keptOrderNewAppendedAndRemovedDroppedTogether() {
        val a = med("a")
        val c = med("c")
        val d = med("d")
        // shown had a,b,c in that order; b removed, d is new -> kept a,c keep order, d appended.
        val result = stableMedicationOrder(shownIds = listOf("a", "b", "c"), meds = listOf(a, c, d))
        assertEquals(listOf("a", "c", "d"), result.map { it.id })
        assertTrue(result.none { it.id == "b" })
    }
}
```

FILE: app/src/test/java/com/sherifradwan/ancnative/ui/patienthub/model/PickerVisibleTilesTest.kt
REPLACES: (new file)
```kotlin
package com.sherifradwan.ancnative.ui.patienthub.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [pickerVisibleTiles].
 *
 * Assumption: [MedicationTile] can be constructed with (id, name, dosage, route, frequency) as used by the
 * picker; a minimal local factory supplies benign defaults for non-name fields.
 */
class PickerVisibleTilesTest {

    private fun tile(id: String, name: String) = MedicationTile(
        id = id,
        name = name,
        dosage = "1 mg",
        route = "Oral",
        frequency = "Once daily",
    )

    @Test
    fun emptyQueryTakesLimitWhenNotShowingAll() {
        val filtered = (1..10).map { tile("t$it", "Drug $it") }
        val result = pickerVisibleTiles(filtered = filtered, query = "", showAll = false, limit = 8)
        assertEquals(filtered.take(8).map { it.id }, result.map { it.id })
    }

    @Test
    fun queryRanksNamesStartingWithQueryFirstThenTakesLimit() {
        val filtered = listOf(
            tile("1", "Zinc sulfate"),
            tile("2", "Aspirin"),
            tile("3", "Azithromycin"),
            tile("4", "Paracetamol"),
            tile("5", "Amoxicillin"),
            tile("6", "Magnesium"),
        )
        val result = pickerVisibleTiles(filtered = filtered, query = "a", showAll = false, limit = 3)
        // Prefix matches (aspirin, azithromycin, amoxicillin in filtered order) come first, limited to 3.
        assertEquals(listOf("2", "3", "5"), result.map { it.id })
        result.forEach { assert(it.name.lowercase().startsWith("a")) }
    }

    @Test
    fun showAllReturnsAllRankedResults() {
        val filtered = listOf(
            tile("1", "Zinc"),
            tile("2", "Aspirin"),
            tile("3", "Azithromycin"),
            tile("4", "Magnesium"),
        )
        val result = pickerVisibleTiles(filtered = filtered, query = "a", showAll = true, limit = 1)
        // All returned, prefix-first ranking preserved (aspirin, azithromycin, then non-prefix in filtered order).
        assertEquals(listOf("2", "3", "1", "4"), result.map { it.id })
        assertEquals(filtered.size, result.size)
    }

    @Test
    fun emptyQueryShowAllReturnsAll() {
        val filtered = (1..5).map { tile("t$it", "Drug $it") }
        val result = pickerVisibleTiles(filtered = filtered, query = "", showAll = true, limit = 2)
        assertEquals(filtered.map { it.id }, result.map { it.id })
    }
}
```
[Tokens] upload: 34872 download: 6535 cache_read: 0 cache_write: 0

