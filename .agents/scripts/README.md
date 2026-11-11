# .agents/scripts

Rule (owner, 2026-11-02): any command sequence run twice becomes a script here, with a proper name, so it is never retyped.

| Script | Does |
|---|---|
| run_build_gate.sh | unit tests, lint, release compile, androidTest compile |
| run_static_checks.sh | all .agents/checks/check_r*.py, FAIL lines only |
| run_emulator_test_class.sh `<class>` `[serial]` | one instrumented class on the .qa package, prints counts |
| install_qa_build_on_device.sh `<serial>` | installs com.sherifradwan.anc.qa only |
| take_device_screenshot.sh `<serial>` `<png>` | adb screenshot |
| launch_opus_review.sh `<prompt>` `<out>` | detached read-only Opus review |
| launch_frontier_review.sh `<engine>` `<prompt>` `<out>` | detached multi-model review (claude-opus, claude-sonnet, deepseek, glm, codex-o3, codex-gpt4o, gemini-argon) |
| qa_package_suffix.init.gradle | adds the .qa applicationId suffix |
| tap_text_on_device.sh `<serial>` `<text>` | taps a node by text / content-desc / testTag (resource-id); leading `=` = exact match |
| qa_open_medication_tab.sh `<serial>` | emulator: launch .qa, enter PIN 121212, open workspace Meds tab (needs wm 1080x2400, density 480 = 360 dp) |
| qa_open_visit_editor.sh `[serial]` | emulator: launch .qa, enter PIN 121212, open workspace Visits tab, open new visit editor (360 dp) |
| qa_open_ultrasound_tab.sh `<serial>` | emulator: launch .qa, enter PIN 121212, open workspace Ultrasound tab (needs wm 1080x2400, density 480 = 360 dp) |
| qa_emulator_delete_first_medication.sh `<serial>` `<png>` | emulator: Meds tab, delete first card, confirm, screenshot the Undo bar (360 dp coordinates) |
| set_emulator_360dp.sh `[serial]` | sets emulator to 360 dp (1080x2400 @ 480 dpi) |
| reset_emulator_density.sh `[serial]` | resets emulator display size and density to default |
| run_unit_test.sh `<test_class_or_pattern>` | runs single unit test or pattern via Gradle offline |
| screenshot_mock_html.sh `<html_path>` `<output_png_path>` `[w]` `[h]` | screenshots an HTML mock file at a given window size using headless Chrome |
| verify_pure_move.py `--old <rev:path or file>` `--new <files...>` `[--ignore-visibility]` | proves a file split was a pure move: equal sorted line multisets (package/import/blank lines and indentation ignored) and equal declaration names; `--ignore-visibility` ignores only a leading private/internal and prints each change |
