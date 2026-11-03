#!/usr/bin/env bash
set -euo pipefail
if [ -z "${1:-}" ]; then
  echo "Usage: $0 <TestClassName or Pattern>" >&2
  exit 1
fi
./gradlew --offline :app:testDebugUnitTest --tests "$1"
