package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LabReportGroupingOwnerFixTest {

    private fun lab(
        id: String,
        testCategory: String,
        testName: String,
        result: String = "1.0",
        unit: String? = null,
        referenceRange: String? = null,
        isAbnormal: Boolean = false,
        notes: String? = null,
        labName: String? = null,
        date: String = "2026-01-01",
        patientId: String = "patient-1",
        revision: Long = 1,
    ): DemoLabInvestigation =
        DemoLabInvestigation(
            id = id,
            patientId = patientId,
            date = date,
            testCategory = testCategory,
            testName = testName,
            result = result,
            unit = unit,
            referenceRange = referenceRange,
            isAbnormal = isAbnormal,
            notes = notes,
            labName = labName,
            revision = revision,
        )

    @Test
    fun s1_haemoglobinHbInHematologyBelongsToCompleteBloodCountCbcGroup() {
        val hb =
            lab(
                id = "hb-1",
                testCategory = "Hematology",
                testName = "Haemoglobin (Hb)",
            )

        val hct = lab(id = "hct-1", testCategory = "Hematology", testName = "Haematocrit (Hct)")
        val partitioned = partitionReportIntoCompositeGroups(listOf(hb, hct))

        val cbcGroup =
            partitioned.filter { it.isComposite }.firstOrNull { it.title == "Complete Blood Count (CBC)" }
        requireNotNull(cbcGroup) { "Expected Complete Blood Count (CBC) group to exist" }

        assertTrue(
            "CBC group should contain Haemoglobin (Hb)",
            cbcGroup.items.any { it.testName == "Haemoglobin (Hb)" },
        )
        assertTrue(
            "Haemoglobin (Hb) should not remain as an individual item",
            partitioned.filter { !it.isComposite }.flatMap { it.items }.none { it.testName == "Haemoglobin (Hb)" },
        )
    }

    @Test
    fun s2_fullReportPartitionsIntoCbcUrineAndIndividualGroups() {
        val items =
            listOf(
                // 6 CBC tests
                lab(
                    id = "cbc-1",
                    testCategory = "Hematology",
                    testName = "Haemoglobin (Hb)",
                    isAbnormal = true,
                ),
                lab(
                    id = "cbc-2",
                    testCategory = "Hematology",
                    testName = "Haematocrit (Hct)",
                ),
                lab(
                    id = "cbc-3",
                    testCategory = "Hematology",
                    testName = "MCV",
                ),
                lab(
                    id = "cbc-4",
                    testCategory = "Hematology",
                    testName = "MCH",
                ),
                lab(
                    id = "cbc-5",
                    testCategory = "Hematology",
                    testName = "White Cell Count (WBC)",
                ),
                lab(
                    id = "cbc-6",
                    testCategory = "Hematology",
                    testName = "Platelets (PLT)",
                ),
                // 8 urine tests (category "Urinalysis"), including "RBCs (Urine)" (2 abnormal total)
                lab(
                    id = "urine-1",
                    testCategory = "Urinalysis",
                    testName = "RBCs (Urine)",
                    isAbnormal = true,
                ),
                lab(
                    id = "urine-2",
                    testCategory = "Urinalysis",
                    testName = "Pus Cells",
                    isAbnormal = true,
                ),
                lab(
                    id = "urine-3",
                    testCategory = "Urinalysis",
                    testName = "Protein (Urine)",
                ),
                lab(
                    id = "urine-4",
                    testCategory = "Urinalysis",
                    testName = "Glucose (Urine)",
                ),
                lab(
                    id = "urine-5",
                    testCategory = "Urinalysis",
                    testName = "Ketones (Urine)",
                ),
                lab(
                    id = "urine-6",
                    testCategory = "Urinalysis",
                    testName = "Nitrite",
                ),
                lab(
                    id = "urine-7",
                    testCategory = "Urinalysis",
                    testName = "Leukocyte Esterase",
                ),
                lab(
                    id = "urine-8",
                    testCategory = "Urinalysis",
                    testName = "Urine pH",
                ),
                // 3 others
                lab(
                    id = "other-1",
                    testCategory = "Blood Bank",
                    testName = "Blood Grouping & Rh Factor",
                ),
                lab(
                    id = "other-2",
                    testCategory = "Biochemistry",
                    testName = "Fasting Blood Sugar (FBS)",
                ),
                lab(
                    id = "other-3",
                    testCategory = "Endocrinology",
                    testName = "Thyroid Stimulating Hormone (TSH)",
                ),
            )

        val partitioned = partitionReportIntoCompositeGroups(items)

        val cbcGroup =
            partitioned.filter { it.isComposite }.firstOrNull { it.title == "Complete Blood Count (CBC)" }
        requireNotNull(cbcGroup) { "Expected Complete Blood Count (CBC) group to exist" }
        assertEquals("CBC group should have 6 items", 6, cbcGroup.items.size)
        assertEquals("CBC group should have 1 abnormal item", 1, cbcGroup.abnormalCount)
        assertTrue(
            "CBC group should not contain RBCs (Urine)",
            cbcGroup.items.none { it.testName == "RBCs (Urine)" },
        )

        val urineGroup =
            partitioned.filter { it.isComposite }.firstOrNull { group ->
                group.items.any { it.testCategory == "Urinalysis" } &&
                    group.items.all { it.testCategory == "Urinalysis" || it.testName.contains("Urine", ignoreCase = true) }
            } ?: partitioned.filter { it.isComposite }.firstOrNull { group ->
                group.items.size == 8 && group.items.any { it.testName == "RBCs (Urine)" }
            }
        requireNotNull(urineGroup) { "Expected urine group to exist" }
        assertEquals("Urine group should have 8 items", 8, urineGroup.items.size)
        assertEquals("Urine group should have 2 abnormal items", 2, urineGroup.abnormalCount)

        assertEquals("Individual items should have size 3", 3, partitioned.filter { !it.isComposite }.flatMap { it.items }.size)
        assertTrue(
            "Individual items should not contain Haemoglobin (Hb)",
            partitioned.filter { !it.isComposite }.flatMap { it.items }.none { it.testName == "Haemoglobin (Hb)" },
        )
        assertTrue(
            "Individual items should contain Blood Grouping & Rh Factor",
            partitioned.filter { !it.isComposite }.flatMap { it.items }.any { it.testName == "Blood Grouping & Rh Factor" },
        )
        assertTrue(
            "Individual items should contain Fasting Blood Sugar (FBS)",
            partitioned.filter { !it.isComposite }.flatMap { it.items }.any { it.testName == "Fasting Blood Sugar (FBS)" },
        )
        assertTrue(
            "Individual items should contain Thyroid Stimulating Hormone (TSH)",
            partitioned.filter { !it.isComposite }.flatMap { it.items }.any { it.testName == "Thyroid Stimulating Hormone (TSH)" },
        )
    }

    @Test
    fun s3_fbsStaysIndividualWhileLiverTestsFormTheirOwnGroup() {
        val items =
            listOf(
                lab(
                    id = "bio-1",
                    testCategory = "Biochemistry",
                    testName = "Fasting Blood Sugar (FBS)",
                ),
                lab(
                    id = "lft-1",
                    testCategory = "Biochemistry",
                    testName = "Alanine Aminotransferase (ALT)",
                ),
                lab(
                    id = "lft-2",
                    testCategory = "Biochemistry",
                    testName = "Aspartate Aminotransferase (AST)",
                ),
            )

        val partitioned = partitionReportIntoCompositeGroups(items)

        assertTrue(
            "Fasting Blood Sugar (FBS) should remain an individual item",
            partitioned.filter { !it.isComposite }.flatMap { it.items }.any { it.testName == "Fasting Blood Sugar (FBS)" },
        )

        val liverGroup =
            partitioned.filter { it.isComposite }.firstOrNull { group ->
                group.items.any { it.testName == "Alanine Aminotransferase (ALT)" } ||
                    group.items.any { it.testName == "Aspartate Aminotransferase (AST)" }
            }
        requireNotNull(liverGroup) { "Expected liver group containing ALT/AST to exist" }
        assertEquals("Liver group should have exactly 2 items", 2, liverGroup.items.size)
        assertTrue(
            "Liver group should contain Alanine Aminotransferase (ALT)",
            liverGroup.items.any { it.testName == "Alanine Aminotransferase (ALT)" },
        )
        assertTrue(
            "Liver group should contain Aspartate Aminotransferase (AST)",
            liverGroup.items.any { it.testName == "Aspartate Aminotransferase (AST)" },
        )
        assertFalse(
            "Liver group should not contain Fasting Blood Sugar (FBS)",
            liverGroup.items.any { it.testName == "Fasting Blood Sugar (FBS)" },
        )
    }

    @Test
    fun s4_emptyListGivesNoGroupsAndNoIndividualItems() {
        val partitioned = partitionReportIntoCompositeGroups(emptyList())

        assertTrue("Expected no composite groups for empty input", partitioned.filter { it.isComposite }.isEmpty())
        assertTrue("Expected no individual items for empty input", partitioned.filter { !it.isComposite }.flatMap { it.items }.isEmpty())
    }

    private fun titlesOf(items: List<DemoLabInvestigation>) =
        partitionReportIntoCompositeGroups(items).associate { it.title to it.items.map { i -> i.testName } }

    @Test
    fun hba1cRdwUrineCultureAndCrpAreGroupedByTheirOwnPanel() {
        val g = titlesOf(
            listOf(
                lab("1", "Biochemistry", "Hemoglobin A1c"),
                lab("2", "Hematology", "Platelets (PLT)"),
                lab("3", "Hematology", "Red Cell Distribution Width (RDW)"),
                lab("4", "Urinalysis", "Urine Protein"),
                lab("5", "Urinalysis", "Urine Culture & Sensitivity"),
                lab("6", "Biochemistry", "C-Reactive Protein (CRP)"),
                lab("7", "Hematology", "Leukocyte Count"),
            ),
        )
        val cbc = g["Complete Blood Count (CBC)"].orEmpty()
        assertTrue("RDW belongs to CBC", "Red Cell Distribution Width (RDW)" in cbc)
        assertTrue("HbA1c is not CBC", "Hemoglobin A1c" !in cbc)
        val individual = g["Individual Investigations"].orEmpty()
        assertTrue("HbA1c individual", "Hemoglobin A1c" in individual)
        assertTrue("CRP individual", "C-Reactive Protein (CRP)" in individual)
        assertTrue("Urine culture individual", "Urine Culture & Sensitivity" in individual)
        assertTrue("Leukocyte count is not urine", "Leukocyte Count" !in g["Complete Urine Analysis"].orEmpty())
    }
}
