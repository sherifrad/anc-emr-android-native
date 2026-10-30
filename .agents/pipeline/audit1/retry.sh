#!/bin/zsh
ROOT=/Users/sherifgamalradwan/anc-emr-android-native
F=$HOME/skills/multi-agent-collaborative-audit/scripts/lane_fallback.py
C=claude:claude-sonnet-5-5; G=opencode:openai/gpt-6-luna; N=opencode:omniroute/nvidia/nvidia/nemotron-3-ultra-550b-a55b
cd $ROOT/.agents/pipeline/audit1
go(){ team=$1; lens=$2; chain=$3; mkdir -p retry/$team; cat 00_rules.md > retry/$team/brief.md; echo >> retry/$team/brief.md; cat $lens.md >> retry/$team/brief.md
  python3 $F --brief retry/$team/brief.md --cd $ROOT --out-dir retry/$team --chain "$chain" --read-only > retry/$team/log.txt 2>&1 & }
go ui    lens_ui    "$C,$G,$N"
go style lens_style "$G,$C,$N"
go tests lens_tests "$N,$C,$G"
wait
