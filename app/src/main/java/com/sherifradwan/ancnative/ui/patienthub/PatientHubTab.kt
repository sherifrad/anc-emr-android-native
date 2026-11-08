package com.sherifradwan.ancnative.ui.patienthub

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.vector.ImageVector
import com.sherifradwan.ancnative.state.WorkspaceSection

enum class PatientHubTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String,
    val defaultSection: WorkspaceSection,
) {
    OVERVIEW("Overview", Icons.Default.Person, "tab-overview", WorkspaceSection.EPISODE_OVERVIEW),
    VISITS("Visits", Icons.AutoMirrored.Filled.EventNote, "tab-visits", WorkspaceSection.VISITS),
    SCANS("Scans", Icons.Default.MonitorHeart, "tab-scans", WorkspaceSection.ULTRASOUND),
    LABS("Labs", Icons.Default.Science, "tab-labs", WorkspaceSection.LABORATORY),
    MEDS("Meds", Icons.Default.Medication, "tab-meds", WorkspaceSection.MEDICATIONS),
    PLAN("Plan", Icons.AutoMirrored.Filled.Assignment, "tab-plan", WorkspaceSection.DELIVERY);

    companion object {
        fun fromSection(section: WorkspaceSection): PatientHubTab = when (section) {
            WorkspaceSection.EPISODE_OVERVIEW,
            WorkspaceSection.DATING,
            WorkspaceSection.OBSTETRIC_HISTORY,
            WorkspaceSection.OTHER_HISTORIES -> OVERVIEW
            WorkspaceSection.VISITS -> VISITS
            WorkspaceSection.ULTRASOUND -> SCANS
            WorkspaceSection.LABORATORY -> LABS
            WorkspaceSection.MEDICATIONS -> MEDS
            WorkspaceSection.DELIVERY,
            WorkspaceSection.ATTACHMENTS -> PLAN
        }
    }
}
