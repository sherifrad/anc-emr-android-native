#!/usr/bin/env bash
# The standard gate: unit tests, lint, release compile, androidTest compile. Silent on success.
set -euo pipefail
cd "$(dirname "$0")/../.."
./gradlew --offline :app:testDebugUnitTest :app:lintDebug :app:compileReleaseKotlin :app:compileDebugAndroidTestKotlin -q
echo "GATE OK"
