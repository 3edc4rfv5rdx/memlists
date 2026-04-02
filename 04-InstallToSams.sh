#!/bin/sh
set -e

serial="${1:-RFCW91FV79X}"
apk=$(ls -t app/build/outputs/apk/release/*arm64-v8a*.apk 2>/dev/null | head -1)

if [ -z "$apk" ]; then
  echo "Release arm64 APK not found. Build first: ./00-MakeRelease.sh"
  exit 1
fi

echo ">>> Installing $(basename "$apk") to $serial"
adb -s "$serial" install -r "$apk"
sleep 2