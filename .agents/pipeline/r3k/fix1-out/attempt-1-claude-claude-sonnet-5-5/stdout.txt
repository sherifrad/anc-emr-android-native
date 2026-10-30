I changed line 221 of `UltrasoundEditor.kt`, but I couldn't run either check because the shell denied both commands.

The linked-visit text now reads `Wt: ${formatWeightKg(linkedVisit.weight) ?: "-"}`, with the rest of the line unchanged.

- `./gradlew --offline :app:compileDebugKotlin`: not run, so compilation is unconfirmed.
- `python3 .agents/checks/check_r3k.py`: not run, so the check result is unconfirmed.

I also never confirmed that `formatWeightKg` exists in `ClinicalFormat.kt`. That grep was part of the compound command that got blocked.
