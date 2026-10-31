#!/bin/zsh
ROOT=/Users/sherifgamalradwan/anc-emr-android-native
F=$HOME/skills/multi-agent-collaborative-audit/scripts/lane_fallback.py
C=claude:claude-sonnet-5-5; G=opencode:openai/gpt-6-luna
cd $ROOT/.agents/pipeline/audit1
export MACA_RELAY_TIMEOUT_MS=1500000
go(){ team=$1; lens=$2; chain=$3; rm -rf retry2/$team; mkdir -p retry2/$team; cat 00_rules.md > retry2/$team/brief.md; echo >> retry2/$team/brief.md; cat $lens.md >> retry2/$team/brief.md
  python3 $F --brief retry2/$team/brief.md --cd $ROOT --out-dir retry2/$team --chain "$chain" --read-only --timeout-ms 1500000 > retry2/$team/log.txt 2>&1 & }
go style lens_style "$C,$G"
go tests lens_tests "$G,$C"
wait
