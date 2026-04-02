#!/bin/sh
set -e

./gradlew :app:assembleDebug

echo "Debug APKs: app/build/outputs/apk/debug/"
ls -1 app/build/outputs/apk/debug/app-*.apk 2>/dev/null

sleep 2
