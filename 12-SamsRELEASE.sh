#!/bin/sh

# Samsung is arm64-v8a — pick that split, fall back to universal, then anything
apk=$(ls -t app/build/outputs/apk/release/*-arm64-v8a.apk 2>/dev/null | head -1)
[ -z "$apk" ] && apk=$(ls -t app/build/outputs/apk/release/*-universal.apk 2>/dev/null | head -1)
[ -z "$apk" ] && apk=$(ls -t app/build/outputs/apk/release/*.apk 2>/dev/null | head -1)

if [ -z "$apk" ]; then
    echo "No release APK found"
    exit 1
fi

echo ">>> Installing: $(basename "$apk")"
adb -s RFCW91FV79X install -r "$apk"
sleep 2
