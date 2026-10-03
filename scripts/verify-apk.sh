#!/usr/bin/env sh
set -eu
APK=${1:?Usage: verify-apk.sh path/to/app-release.apk}
SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
if [ -z "$SDK" ]; then
  echo 'Set ANDROID_HOME to your Android SDK.' >&2
  exit 1
fi
BUILD_TOOLS=${WONDERPLAY_BUILD_TOOLS:-35.0.0}
"$SDK/build-tools/$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$APK"
"$SDK/build-tools/$BUILD_TOOLS/zipalign" -c -P 16 -v 4 "$APK"
