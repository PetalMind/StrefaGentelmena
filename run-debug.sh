#!/usr/bin/env bash
# Build + install debug APK i start MainActivity

set -euo pipefail
cd "$(dirname "$0")"

: "${JAVA_HOME:=$(/usr/libexec/java_home 2>/dev/null || true)}"
if [[ ! -d "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
fi

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="$SDK/platform-tools/adb"
if [[ ! -x "$ADB" ]]; then
  echo "Brak adb: $ADB — ustaw ANDROID_HOME lub ANDROID_SDK_ROOT." >&2
  exit 1
fi

chmod +x ./gradlew 2>/dev/null || true
./gradlew installDebug
exec "$ADB" shell am start -n com.strefagentlemanakingakloss/com.strefagentelmena.MainActivity