# ANC EMR Architecture Constitution & Engineering Rules

**Effective Date:** 2026-11-08  
**Authorizer:** Clinical Product Owner (Dr. Sherif)  
**Scope:** Entire Android Native Codebase (`com.sherifradwan.anc`)

---

## 1. Rule of Reusability ("Build Once, Use Everywhere")
- **Single Canonical Implementation**: Every clinical domain entity (Patient Identity, Bedside Vitals, Ultrasound Biometry, Lab Reports, Medications) must have **exactly one** canonical UI component implementation.
- **Strict Anti-Duplication**: Never build parallel, divergent representations of the same clinical entity across different screens. If a component is needed in multiple contexts (e.g., in a Tab, in an Editor, or in a Summary Dialog), extract it to a shared modular component (`ui/components/` or `ui/patienthub/components/`) and consume it everywhere.
- **Zero Parallel Maintenance**: Any bug fix, formatting enhancement, or safety check applied to a canonical component must automatically reflect in all consuming contexts.

---

## 2. Modularity & Clean Architecture (Anti-Spaghetti Invariant)
- **File Size Ceiling**: No single UI file shall exceed 1,500 lines. Monolithic screens must be decomposed into clean, domain-scoped modular tab composables and reusable component files.
- **Separation of Concerns**: Keep screen orchestrators (e.g., `PatientWorkspaceScreen`) focused strictly on navigation, pager synchronization, and header collapse. Delegate tab body rendering to dedicated tab composables (`OverviewTab`, `VisitsTab`, `ScansTab`, `LabsTab`, `MedsTab`, `PlanTab`).
- **Unidirectional MVI**: UI consumes immutable state slices and emits discrete `AncEvent` intents. No local business logic mutations inside composables.

---

## 3. Zero Legacy Baggage Invariant
- **No Old Users = Zero Migration Overhead**: Do not maintain backward-compatibility shims, legacy route aliases, or deprecated adapters for old users when no production users exist. Deprecated code paths, duplicate screens, and dead routes must be deleted cleanly.

---

## 4. SAMD Safety & Viewport Ergonomics
- **Schema v6 Frozen**: Room database schema v6 is 100% frozen; zero SQLite migrations are permitted without clinical sign-off.
- **360dp Narrow Viewport First**: All clinical UI must render without critical text truncation (Patient Name, Gestational Age, Blood Pressure, High Risk Alert) on 360dp narrow mobile viewports.
