#!/usr/bin/env bash
# Usage: antislop_mechanical_scan.sh [repo_root]
# Purpose: ripgrep antislop sweep of UI code and strings (em dashes, emoji, unnamed icons, endless animation, hardcoded UI text, small targets). Read-only; prints findings, exit 0.
set -u
cd "${1:-.}"
U=app/src/main/java/com/sherifradwan/ancnative/ui
R=app/src/main/res
sec() { printf '\n== %s\n' "$1"; }
sec "em dash in resource strings"; rg -n "—" $R/values*/strings.xml | cut -c1-170
sec "em dash in UI string literals"; rg -n '"[^"]*—[^"]*"' -g '*.kt' $U | rg -v ':\s*(//|\*|/\*)' | cut -c1-200
sec "emoji, check and warning signs in UI code and strings"; rg -n '[\x{2705}\x{2713}\x{2714}\x{26A0}\x{2757}\x{1F300}-\x{1FAFF}]' -g '*.kt' -g 'strings.xml' $U $R/values | cut -c1-190
sec "contentDescription = null (lines)"; rg -n "contentDescription = null" -g '*.kt' $U | cut -c1-170
sec "infinite animations"; rg -n "infiniteRepeatable|rememberInfiniteTransition" -g '*.kt' $U
sec "hardcoded Text literals"; rg -n 'Text\(\s*(text = )?"[A-Za-z][^"$]{3,}"' -g '*.kt' $U | cut -c1-190
sec "Color(0x...) in UI outside Theme and DesignSystem"; rg -n "Color\(0x" -g '*.kt' $U | rg -v "/Theme.kt|/DesignSystem.kt" | cut -c1-170
sec "explicit sizes 16 to 44dp (check each is a visual, not a tap target)"; rg -n 'size\((1\d|2\d|3\d|40|44)\.dp\)' -g '*.kt' $U | cut -c1-150
sec "files over 1500 lines"; find app/src/main -name '*.kt' | xargs wc -l | awk '$1>1500 && $2!="total"'
