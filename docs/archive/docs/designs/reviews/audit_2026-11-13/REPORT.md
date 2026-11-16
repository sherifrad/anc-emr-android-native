# Audit report 2026-11-13 (testing mode, full path)

Scope: the registration flow merge (`pre-registration-merge-main..0127086`) and the pencil and chip change, at HEAD. Spec and oracles: `SPEC.md`.
Independence: multi-vendor. The merge port was written by Claude; the pencil change by alphacode. Test designer: DeepSeek (opencode). Reviewers: GLM (opencode) on the reducer diff, alphacode on the reducer plus ViewModel plus commands diff. big-pickle (opencode) returned nothing, so the ViewModel and commands diff had one reviewer, not two. No frontier-tier reviewer was available (Antigravity out of quota).

## New tests (written by DeepSeek from the spec, run by the orchestrator)
`DeliveryThroughRegistrationFlowTest`, 6 tests, all pass on first run:
- A1 a delivered patient edited through the flow's Identity step keeps delivered, date, mode and outcome in the save command.
- A2 the Identity step rejects a blank age and accepts a filled one.
- A3 the same draft gives the same fingerprint; a changed delivery date changes it.
- Negative: Begin on an archived patient changes nothing.

## Review findings and triage (each checked against the code by the orchestrator)
Both reviewers answered FAIL, mainly because helpers were outside the diff they were given. No finding was confirmed:
| Finding | Reviewer | Check | Result |
|---|---|---|---|
| Clean Back leaves `awaitingAdvance` armed | GLM | `withFlowConsistent` runs after every event and clears the flow and the wait when the route is no longer the flow step; tests `s5_6_*` and `back_*_drops_the_wait` cover it | not a defect |
| `DELIVERY_BY` ignores the original values | GLM | that branch passes the picked mode, and `deliveryColumns` decides from the picked mode alone | not a defect |
| Structural check requires age for stored patients | alphacode | `ageRequired(isNew, null)` is false for a stored patient | not a defect (misread) |
| LMP no longer editable in the patient form, dating update gone | alphacode | by design: dating has its own editor (merge plan T1, T3, owner agreed) | by design |
| Picking an outcome blanks the old free-text outcome | alphacode | same line exists on the reviewed registration branch; the free-text field was replaced by the structured outcome | by design |
| `registrationNext` may save the wrong draft | alphacode | only one draft is open at a time (`clearDrafts` on every editor start); no concrete state given | not confirmed |
| Discard may lose the pending flow move; recovery guard direction | alphacode | reviewer could not see the helpers; `applyFlowMove` does not depend on `awaitingAdvance`; covered by `s5_5_*` tests | not confirmed |

## Gates
See the roadmap log for the final numbers of the push gate run on the audited commit.

## Not verified
- No frontier-tier review. The ViewModel and commands diff had a single reviewer.
- The MACA referee scripts were not run; this report and the lane outputs in this folder are the evidence.
- Phone: the owner's own check of the QA build; nothing was tapped by the orchestrator.
- Reviewer test ideas not built: token-guarded advance with a foreign commit, a ViewModel-level "failed save then flow move does not resend" test.
