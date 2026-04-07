#!/bin/sh

apk="app/build/outputs/apk/debug/app-debug.apk"
device="RFCW91FV79X"

if [ ! -f "$apk" ]; then
    echo "No debug APK found at $apk"
    exit 1
fi

echo ">>> Installing: $(basename "$apk") on Samsung ($device)"
adb -s "$device" install -r "$apk"

sleep 2
