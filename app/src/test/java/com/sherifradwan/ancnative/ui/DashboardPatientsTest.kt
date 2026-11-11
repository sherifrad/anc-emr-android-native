package com.sherifradwan.ancnative.ui

import com.sherifradwan.ancnative.model.DemoPatient
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardPatientsTest {
    private fun patient(id: String, delivered: Boolean) = DemoPatient(
        id = id, caseId = id, displayName = id, dateOfBirth = null, phone = null, visits = emptyList(), delivered = delivered,
    )

    private val all = listOf(patient("a", false), patient("b", true), patient("c", false))

    @Test
    fun theMainListHidesDeliveredPatients() {
        assertEquals(listOf("a", "c"), dashboardPatients(all, "all", searching = false).map { it.id })
    }

    @Test
    fun theRiskFiltersAlsoStartFromTheUndeliveredList() {
        assertEquals(listOf("a", "c"), dashboardPatients(all, "high", searching = false).map { it.id })
    }

    @Test
    fun theDeliveredFilterShowsOnlyDeliveredPatients() {
        assertEquals(listOf("b"), dashboardPatients(all, "delivered", searching = false).map { it.id })
    }

    @Test
    fun aSearchStillFindsDeliveredPatients() {
        assertEquals(listOf("a", "b", "c"), dashboardPatients(all, "all", searching = true).map { it.id })
    }

    @Test
    fun theDeliveredFilterIgnoresTheSearchFlag() {
        assertEquals(listOf("b"), dashboardPatients(all, "delivered", searching = true).map { it.id })
    }
}
