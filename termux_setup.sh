#!/data/data/com.termux/files/usr/bin/bash
# One-time setup in Termux. Needs ~2.5 GB free and internet.
set -e
pkg update -y
pkg install -y openjdk-17 gradle aapt2 unzip wget
SDK=$HOME/android-sdk
mkdir -p $SDK/cmdline-tools && cd $SDK/cmdline-tools
if [ ! -d latest ]; then
  wget -O ct.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q ct.zip && mv cmdline-tools latest && rm ct.zip
fi
yes | $SDK/cmdline-tools/latest/bin/sdkmanager --sdk_root=$SDK --licenses >/dev/null || true
$SDK/cmdline-tools/latest/bin/sdkmanager --sdk_root=$SDK "platforms;android-35" "build-tools;34.0.0"
echo "sdk.dir=$SDK" > "$(dirname "$0")/local.properties"
mkdir -p $HOME/.gradle
grep -q aapt2FromMavenOverride $HOME/.gradle/gradle.properties 2>/dev/null || echo "android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2" >> $HOME/.gradle/gradle.properties
echo "Setup done. Now run: ./build.sh"
