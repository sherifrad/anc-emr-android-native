package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R

data class ClinicalLabPreset(
    val label: String,
    val category: String,
    val testName: String,
    val result: String = "",
    val unit: String = "",
    val referenceRange: String = "",
)

data class ClinicalMedPreset(
    val label: String,
    val name: String,
    val dosage: String,
    val route: String,
    val frequency: String,
    val isProphylaxis: Boolean,
)

object ClinicalPresets {
    val commonBloodPressures = listOf("110/70", "120/80", "130/80", "140/90")

    val commonVisitFindings = listOf(
        "Normal progress",
        "Cephalic, FHR positive",
        "No pedal edema",
        "Mild pedal edema",
        "Reduced fetal movement",
    )

    val commonLabPresets = listOf(
        ClinicalLabPreset("CBC", "Hematology", "Complete Blood Count", "", "g/dL", "11.0 - 15.0"),
        ClinicalLabPreset("Blood Group", "Hematology", "Blood Grouping & Rh Factor"),
        ClinicalLabPreset("Fasting Glucose", "Biochemistry", "Fasting Blood Sugar", "", "mg/dL", "70 - 95"),
        ClinicalLabPreset("OGTT 75g", "Biochemistry", "Oral Glucose Tolerance Test (75g)", "", "mg/dL", "< 140"),
        ClinicalLabPreset("Urine Analysis", "Biochemistry", "Urine Routine & Microscopic", "Nil Albumin, Nil Sugar"),
        ClinicalLabPreset("HBsAg", "Serology", "Hepatitis B Surface Antigen", "Negative"),
        ClinicalLabPreset("HIV Screening", "Serology", "HIV 1 & 2 Antibody", "Non-reactive"),
        ClinicalLabPreset("VDRL / Syphilis", "Serology", "Syphilis Serology (VDRL)", "Non-reactive"),
        ClinicalLabPreset("Rubella IgG", "Serology", "Rubella Antibody IgG", "Positive (Immune)"),
        ClinicalLabPreset("TSH", "Biochemistry", "Thyroid Stimulating Hormone", "", "mIU/L", "0.2 - 2.5"),
    )

    val commonMedPresets = listOf(
        ClinicalMedPreset("Folic Acid 5mg", "Folic Acid", "5 mg", "Oral", "Once daily", true),
        ClinicalMedPreset("Iron / Ferrous 200mg", "Ferrous Sulfate", "200 mg", "Oral", "Once daily", true),
        ClinicalMedPreset("Calcium 500mg + D3", "Calcium + Vitamin D3", "500 mg / 400 IU", "Oral", "Twice daily", true),
        ClinicalMedPreset("Prenatal Multivitamins", "Prenatal Multivitamins", "1 tablet", "Oral", "Once daily", true),
        ClinicalMedPreset("Aspirin 150mg", "Low-Dose Aspirin", "150 mg", "Oral", "Once daily at bedtime", true),
        ClinicalMedPreset("Paracetamol 500mg", "Paracetamol", "500 mg", "Oral", "As needed (PRN)", false),
    )

    val commonUltrasoundImpressions = listOf(
        "Single live fetus, cephalic, normal liquor & placenta",
        "Normal fetal growth & biometry for dates",
        "Breech presentation, normal amniotic fluid",
        "Placenta previa suspected, follow-up scan advised",
    )
}

@Composable
fun QuickPresetChips(
    title: String,
    items: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    tagPrefix: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "⚡ $title",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items.forEachIndexed { index, item ->
                FilterChip(
                    selected = false,
                    onClick = { onSelect(item) },
                    enabled = enabled,
                    label = { Text(item) },
                    modifier = Modifier.testTag("$tagPrefix-$index"),
                )
            }
        }
    }
}
