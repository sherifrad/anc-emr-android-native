# Run the vendor fleet (in your Mac terminal, from this project folder)
SK=~/skills/multi-agent-collaborative-audit
cd ~/anc-emr-android-native
bash $SK/scripts/fleet_review.sh --detect
opencode models | head -40        # find the exact GLM and DeepSeek ids you have configured
bash $SK/scripts/fleet_review.sh --brief .agents/briefs/review_brief.md --cd . \
  --lane opencode:<glm-model-id> --lane opencode:<deepseek-model-id> --lane gemini --lane codex
# results: .agents/fleet-runs/<timestamp>/*.md  (plus a comparison table printed at the end)
