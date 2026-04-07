#!/usr/bin/env bash
set -e

BUILD_FILE="build_number.txt"
APK_DIR="app/build/outputs/apk/release"

source "$BUILD_FILE"

if [[ -z "$build" ]]; then
    echo "ERROR: Failed to read build from $BUILD_FILE"
    exit 1
fi

apk=$(ls -t "$APK_DIR"/*.apk 2>/dev/null | head -1)

if [[ -z "$apk" || ! -f "$apk" ]]; then
    echo "ERROR: Release arm64 APK not found. Build first: ./00-MakeRelease.sh"
    exit 1
fi

dst="memlists-${build}.apkx"

ln -sf "$apk" "$dst" 2>/dev/null || cp "$apk" "$dst"

echo "$(basename "$apk") -> $dst"

sleep 2
