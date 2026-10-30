#!/bin/bash
# Double-click to run the MACA vendor fleet review on the ANC project (read-only).
cd "$HOME/anc-emr-android-native" || exit 1
SK="$HOME/skills/multi-agent-collaborative-audit"
bash "$SK/scripts/fleet_review.sh" --brief .agents/briefs/review_brief.md --cd . \
  --lane opencode:opencode/glm-5.3-flash-free \
  --lane opencode:opencode/deepseek-v4.1-flash-free \
  --lane gemini --lane codex --lane agy
echo; echo "Done. Findings are in .agents/fleet-runs/. You can close this window and tell Claude."
read -n 1 -s -r -p "Press any key to close"
