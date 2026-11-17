#!/data/data/com.termux/files/usr/bin/bash
cd "$(dirname "$0")"
gradle assembleDebug --no-daemon
echo; echo "APK:"; ls -l app/build/outputs/apk/debug/*.apk
