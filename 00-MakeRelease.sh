#!/bin/sh
set -e

./gradlew :app:assembleRelease

echo "Release APKs: app/build/outputs/apk/release/"
ls -1 app/build/outputs/apk/release/*.apk 2>/dev/null

sleep 2
