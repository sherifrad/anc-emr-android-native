#!/usr/bin/env bash
# Usage: install_qa_build_on_device.sh <adb-serial>
# Installs the debug build as com.sherifradwan.anc.qa (side by side). Never touches the clinic app or other dev copies.
set -euo pipefail
cd "$(dirname "$0")/../.."
ANDROID_SERIAL="${1:?adb serial required (adb devices -l)}" ./gradlew --offline \
  -I .agents/scripts/qa_package_suffix.init.gradle :app:installDebug -q
