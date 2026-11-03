#!/usr/bin/env bash
# Builds app/src/main/assets/helper/clip.jar from helper/Clip.java (the small program the phone pushes to the TV to type any alphabet).
# Needs a JDK and the Android SDK (platform 34 and build-tools 34). The jar is committed, so a normal build does not need this.
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-$HOME/android}"
AJ="$SDK/platforms/android-34/android.jar"
D8="$(ls "$SDK"/build-tools/*/d8 | tail -1)"
OUT="$(mktemp -d)"
javac -source 8 -target 8 -cp "$AJ" -d "$OUT/classes" Clip.java AppInfo.java 2>&1 | tail -5
"$D8" --lib "$AJ" --output "$OUT/clip.jar" "$OUT"/classes/*.class
mkdir -p ../app/src/main/assets/helper
cp "$OUT/clip.jar" ../app/src/main/assets/helper/clip.jar
rm -rf "$OUT"
echo "built app/src/main/assets/helper/clip.jar ($(wc -c < ../app/src/main/assets/helper/clip.jar) bytes)"
