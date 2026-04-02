#!/bin/sh

apk="app/build/outputs/apk/debug/app-debug.apk"

if [ -z "$apk" ]; then
    echo "No debug APK found"
    exit 1
fi

echo ">>> Installing: $(basename "$apk")"
adb -s emulator-5554 install -r "$apk"
sleep 2
