#!/bin/sh

apk=$(ls -t app/build/outputs/apk/release/*.apk 2>/dev/null | head -1)

if [ -z "$apk" ]; then
    echo "No release APK found"
    exit 1
fi

echo ">>> Installing: $(basename "$apk")"
adb -s emulator-5554 install -r "$apk"
