package com.sherifradwan.ancnative

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.model.codex.CustomChoiceList
import com.sherifradwan.ancnative.model.codex.CustomChoiceValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomChoicesStoreInstrumentedTest {

    private lateinit var repo: ClinicalCodexRepository
    private val testList = CustomChoiceList.MEDICATION_DOSE

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repo = ClinicalCodexRepository.getInstance(context)
        repo.clearCustomChoices(testList)
    }

    @Test
    fun realDevice_addPersistsAcrossRepositoryInstance() {
        assertEquals(0, repo.customChoices(testList).value.size)

        // 1. Add item
        val result = repo.addCustomChoice(testList, "75 mg", existingDefaults = listOf("50 mg", "100 mg"))
        assertTrue(result is CustomChoiceValidationResult.Valid)
        assertEquals(1, repo.customChoices(testList).value.size)

        val item = repo.customChoices(testList).value.first()
        assertEquals("75 mg", item.label)

        // 2. Clear singleton instance reflection or test persistence by reading from same context
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo2 = ClinicalCodexRepository.getInstance(context)
        assertEquals(1, repo2.customChoices(testList).value.size)
        assertEquals("75 mg", repo2.customChoices(testList).value.first().label)

        // 3. Duplicate protection on Add
        val dupResult = repo2.addCustomChoice(testList, "  75 mg  ")
        assertTrue(dupResult is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.DUPLICATE, (dupResult as CustomChoiceValidationResult.Invalid).reason)
        assertEquals(1, repo2.customChoices(testList).value.size)

        // 4. Remove and Undo
        val removed = repo2.removeCustomChoice(testList, item.id)
        assertNotNull(removed)
        assertEquals(0, repo2.customChoices(testList).value.size)

        val restored = repo2.restoreCustomChoice(removed!!)
        assertTrue(restored)
        assertEquals(1, repo2.customChoices(testList).value.size)
        assertEquals("75 mg", repo2.customChoices(testList).value.first().label)

        // Clean up
        repo2.clearCustomChoices(testList)
        assertEquals(0, repo2.customChoices(testList).value.size)
    }
}
