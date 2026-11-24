#!/bin/sh
# Downloads a pinned JDK 17 and Android SDK command-line tools into .toolchain/
# (inside the repo, git-ignored), verifies their checksums, then installs the
# Android build-tools and platform with sdkmanager.
#
# Reuses an existing setup when JAVA_HOME (17+) and ANDROID_HOME (with
# build-tools 36.1.0 + platforms/android-36) are already set, e.g. in CI.
#
# The Android SDK requires accepting Google's license. Set ACCEPT_ANDROID_LICENSES=1
# only after the person running this has read and agreed to it
# (it is printed by: sdkmanager --licenses).
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TC="${TOOLCHAIN:-$ROOT/.toolchain}"
BUILD_TOOLS=36.1.0
PLATFORM=android-36

JDK_VER=17.0.21.1_2
JDK_BASE=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21.1%2B2
CLT_BASE=https://dl.google.com/android/repository

os=$(uname -s)
arch=$(uname -m)
case "$os-$arch" in
  Darwin-arm64)  JDK_FILE=OpenJDK17U-jdk_aarch64_mac_hotspot_$JDK_VER.tar.gz
                 JDK_SHA256=2a88ccb044a8fb00957be93245da6357c11b55717511509d147cbcc5d93b2109
                 CLT_FILE=commandlinetools-mac_arm64-16111833_latest.zip
                 CLT_SHA1=ad03dc49bfacfd52c110b14104ea548b8a07e830 ;;
  Darwin-x86_64) JDK_FILE=OpenJDK17U-jdk_x64_mac_hotspot_$JDK_VER.tar.gz
                 JDK_SHA256=63a1469b52a8063a9f70e539f815093f3a33ef71e6f901b5f43545dbe4e72858
                 CLT_FILE=commandlinetools-mac_x86_64-16111833_latest.zip
                 CLT_SHA1=112cf9618794a997ff273537d55bee02c22abffe ;;
  Linux-x86_64)  JDK_FILE=OpenJDK17U-jdk_x64_linux_hotspot_$JDK_VER.tar.gz
                 JDK_SHA256=0b0ac5eab6db6abaec68c9913bcddb31321e5da411c45073d520d9c125e6f7b4
                 CLT_FILE=commandlinetools-linux-16111833_latest.zip
                 CLT_SHA1=e025545c62a8e64c7559119566a569fb1dec5f60 ;;
  Linux-aarch64) JDK_FILE=OpenJDK17U-jdk_aarch64_linux_hotspot_$JDK_VER.tar.gz
                 JDK_SHA256=1d808f4b558e9de63d8ba3ead21ffc8b9de204894eff3c19d52c1480aa17cc58
                 CLT_FILE=commandlinetools-linux-16111833_latest.zip
                 CLT_SHA1=e025545c62a8e64c7559119566a569fb1dec5f60 ;;
  *) echo "Unsupported platform $os-$arch. Use Linux or macOS (or WSL on Windows)." >&2; exit 1 ;;
esac

sha256() { if command -v sha256sum >/dev/null; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi; }
sha1()   { if command -v sha1sum   >/dev/null; then sha1sum   "$1" | cut -d' ' -f1; else shasum -a 1   "$1" | cut -d' ' -f1; fi; }

have_java() { [ -n "${JAVA_HOME:-}" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -qE 'version "(1[7-9]|2[0-9])'; }
have_sdk()  { [ -n "${ANDROID_HOME:-}" ] && [ -x "$ANDROID_HOME/build-tools/$BUILD_TOOLS/aapt2" ] && [ -f "$ANDROID_HOME/platforms/$PLATFORM/android.jar" ]; }

if have_java && have_sdk; then
  echo "Using JAVA_HOME=$JAVA_HOME and ANDROID_HOME=$ANDROID_HOME"
  exit 0
fi

mkdir -p "$TC/dl"

if ! have_java; then
  if [ ! -x "$TC/jdk/bin/java" ] && [ ! -x "$TC/jdk/Contents/Home/bin/java" ]; then
    echo "Downloading JDK 17 ($JDK_FILE)"
    curl -fL --retry 3 -o "$TC/dl/$JDK_FILE" "$JDK_BASE/$JDK_FILE"
    [ "$(sha256 "$TC/dl/$JDK_FILE")" = "$JDK_SHA256" ] || { echo "JDK checksum mismatch, aborting" >&2; exit 1; }
    rm -rf "$TC/jdk" && mkdir -p "$TC/jdk"
    tar -xzf "$TC/dl/$JDK_FILE" -C "$TC/jdk" --strip-components 1
  fi
fi

if ! have_sdk; then
  SDK="$TC/android-sdk"
  if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "Downloading Android command-line tools ($CLT_FILE)"
    curl -fL --retry 3 -o "$TC/dl/$CLT_FILE" "$CLT_BASE/$CLT_FILE"
    [ "$(sha1 "$TC/dl/$CLT_FILE")" = "$CLT_SHA1" ] || { echo "SDK tools checksum mismatch, aborting" >&2; exit 1; }
    rm -rf "$SDK/cmdline-tools" && mkdir -p "$SDK/cmdline-tools"
    (cd "$SDK/cmdline-tools" && unzip -q "$TC/dl/$CLT_FILE" && mv cmdline-tools latest)
  fi
  if [ -x "$TC/jdk/Contents/Home/bin/java" ]; then JH="$TC/jdk/Contents/Home"; elif [ -x "$TC/jdk/bin/java" ]; then JH="$TC/jdk"; else JH="$JAVA_HOME"; fi
  if [ "${ACCEPT_ANDROID_LICENSES:-0}" = "1" ]; then
    yes | JAVA_HOME="$JH" "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null
  else
    echo "Android SDK license: review and accept it now (or rerun with ACCEPT_ANDROID_LICENSES=1 after reading it)."
    JAVA_HOME="$JH" "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses
  fi
  JAVA_HOME="$JH" "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" \
    "build-tools;$BUILD_TOOLS" "platforms;$PLATFORM" "platform-tools"
fi

rm -rf "$TC/dl"
echo "Toolchain ready in $TC"
