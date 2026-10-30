#!/bin/zsh
export MACA_RELAY_TIMEOUT_MS=1500000
# usage: launch.sh  -> starts all teams in parallel, read-only; results in audit1/out/<team>/result.json
ROOT=/Users/sherifgamalradwan/anc-emr-android-native
SK=$HOME/skills/multi-agent-collaborative-audit/vendor/delegate-skills/skills
cd $ROOT/.agents/pipeline/audit1
run(){ team=$1; cli=$2; model=$3; lens=$4
  mkdir -p out/$team
  cat 00_rules.md > out/$team/brief.md; echo >> out/$team/brief.md; cat $lens.md >> out/$team/brief.md
  env ${=model:+ANTHROPIC_MODEL=$model} node $SK/$cli-delegate/scripts/relay.mjs --brief out/$team/brief.md --cd $ROOT --out-dir out/$team ${=model:+--model $model} --read-only > out/$team/relay.log 2>&1 &
}
#run claude-sonnet   claude   claude-sonnet-5-5 lens_security
#run gpt6-luna       opencode openai/gpt-6-luna lens_clinical
run nemotron-ultra  opencode omniroute/nvidia/nvidia/nemotron-3-ultra-550b-a55b lens_concurrency
#run glm-53flash opencode omniroute/nvidia/z-ai/glm-5.3-flash lens_ui
run mistral-large4  opencode omniroute/nvidia/mistralai/mistral-large-4-1t-a52b lens_style
run deepseek-v41    opencode omniroute/nvidia/deepseek-ai/deepseek-v4.1-flash lens_tests
wait
