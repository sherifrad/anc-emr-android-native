# High-Density Patient Directory Card & Actions Sheet Redesign

* **Author:** sherifrad
* **Date:** 2026-11-09

## Overview

The primary patient directory (`PatientListScreen`) currently displays patient records as oversized cards (~180–220dp height) with ragged `FlowRow` badge wrapping, triple-redundant click affordances, and visual zigzagging between Arabic and Latin scripts.

This feature redesigns the patient directory into a high-density, uniform start-aligned card (~104–112dp) with a structured 3-column clinical lockup (Gestational Age & EDD, Obstetric Parity & History, Latest Vitals & BP Alert). It replaces redundant footer icon buttons with a card-level tap to open the workspace, consolidates the "⋮" menu into a dedicated 4-action clinical bottom sheet, and standardizes visual hierarchy across all viewports starting at 360dp.

## Analysis

### 1. Inventive Contradictions & Architectural Solutions
* **The Density vs. Truncation Contradiction**: The user sought a tabular layout to compare patients quickly, but full tables fail on 360dp mobile viewports due to horizontal column clipping.
  * **Solution (TRIZ Principle 1 - Columnar Segmentation)**: Retain the card envelope but segment the interior into a structured 3-column micro-grid (~100dp / ~104dp / ~100dp on 360dp screens) with fixed clinical functions rather than free-floating badge pills.
* **The Alert Visibility vs. Clutter Contradiction**: Current cards dedicate a large salmon-tinted callout box to clinical attention notes on every patient, ballooning card height.
  * **Solution (TRIZ Principle 13 - Inversion / Ambient Indication)**: Introduce a 3.5dp leading-edge severity accent strip (Red for Severe BP / High Risk, Amber for Medium Risk, Neutral Surface for Low/Normal Risk). This alerts peripheral vision immediately without adding vertical lines of text.

### 2. Layout, Ergonomics & SAMD Clinical Safety
* **Uniform Start-Alignment**: Arabic patient names are rendered with content-aware text direction, but the card grid itself remains strictly start-aligned (LTR). This eliminates the disorientation of jumping between opposite sides of the screen.
* **Touch Target Ergonomics**: All interactive elements maintain $\ge 48\,\text{dp}$ touch bounds (the "⋮" menu button and the entire card container). Redundant footer buttons (`EditNote` and `AccountCircle`) are deleted.
* **Accessibility Font Scaling**: The card container uses `Modifier.heightIn(min = 104.dp)` rather than rigid fixed height, accommodating Android 1.15x–1.3x font accessibility scaling without clipping or overflow crashes.
* **Frozen Database Schema v6 Invariant**: Zero SQLite Room migrations. All necessary fields (`latestVitalsBp`, `latestVitalsPulse`) are projected in-memory via `PatientDashboardRowProjection` from existing visit entities.

## Goals vs. Non-Goals

### In-Scope Goals
* **Decompose `PatientDirectoryCard`**: Split monolithic rendering into `PatientDirectoryCardHeader`, `PatientDirectoryMetricsRow` (3-column grid), and `PatientActionsModalSheet`.
* **3-Column Metric Lockup**:
  * **Col 1 (Gestational Age & EDD)**: Bold GA (`34+2 wks`), muted EDD (`Due 14 Nov 2026`).
  * **Col 2 (Obstetric History)**: Parity (`G3 P2 A0`), prior delivery tag (`Prev: 2x C/S`).
  * **Col 3 (Bedside Vitals & Alert)**: Latest blood pressure (`120/80` or `160/110 mmHg (!)`) with color threshold flags.
* **Refined Clinical Actions Bottom Sheet**: Replace the 4 redundant menu rows with:
  1. Quick Record Visit (`onStartVisit(patient.id, null)`)
  2. Open Chart / Workspace (`AncEvent.OpenWorkspace(patient.id)`)
  3. Edit Demographics (`AncEvent.StartPatientEditor(patient.id, ...)`)
  4. Archive / Restore (`AncEvent.StartStatusEditor(...)`)
* **Leading Edge Severity Indicator**: 3.5dp accent line on the card's start border mapping directly to clinical risk tier and severe blood pressure status.
* **File Health**: Reduce `PatientListScreen.kt` from 859 lines to $<700$ lines.

### Out-of-Scope Non-Goals
* **No Database Migrations**: No Room schema modifications or database alterations.
* **No Table Mode on Mobile**: No horizontal-scrolling multi-column data spreadsheet on mobile phones. (Adaptive multi-column tables for desktop/tablet remain staged for wide-screen adaptive phases).
* **No Changes to Clinical Validation Engines**: Gestational age calculations, BP classification algorithms, and codex lookups remain untouched.

## Alternatives Weighed

1. **Full Tabular Data Grid Across All Screens**:
   * *Rejected by author & review*: On 360dp mobile viewports, 6+ table columns force horizontal scrolling, hiding high-risk alerts off-screen and creating accidental horizontal pan conflicts with vertical list scrolling.
2. **Expandable Accordion Row (~64dp collapsed, ~140dp expanded)**:
   * *Rejected in favor of Option A*: Clinicians conducting rapid rounds need to compare GA, EDD, parity, and BP across all patients at a single glance without repeatedly tapping rows to expose basic vitals.
3. **Retaining Footer Icon Buttons**:
   * *Rejected by review*: The footer icons duplicated `AncEvent.OpenWorkspace`, which is already triggered by tapping the card body. Removing them reclaimed ~36dp of vertical height.

## Key Decisions & Staging for Next Steps

* **Decision 1**: Adopt the High-Density Structured Card (~104–112dp) with the 3.5dp leading accent line.
* **Decision 2**: Update `PatientDashboardRowProjection` to extract latest visit blood pressure and pulse without database alterations.
* **Decision 3**: Standardize all cards to start-alignment regardless of Latin or Arabic script.
* **Decision 4**: Stage the consolidated actions sheet to include a direct "New Visit" action, closing the workflow loop directly from the directory.
