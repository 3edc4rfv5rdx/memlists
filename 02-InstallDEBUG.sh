#!/bin/sh
set -e

serial="${1:-emulator-5554}"
apk=$(ls -t app/build/outputs/apk/debug/*x86_64*.apk 2>/dev/null | head -1)

if [ -z "$apk" ]; then
  echo "Debug x86_64 APK not found. Build first: ./01-MakeDebug.sh"
  exit 1
fi

echo ">>> Installing $(basename "$apk") to $serial"
adb -s "$serial" install -r "$apk"
sleep 2