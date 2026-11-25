#!/bin/sh
# Builds the APK without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Output: build/odin-tv-magic.apk
#
# Signing:
#   - default: a personal key in ~/.odin-tv-magic/local.jks (created on first build,
#     outside the repo). Keep it: Android only installs updates signed with the same key.
#   - release: set ODIN_KEYSTORE=/path/release.jks and ODIN_KEYSTORE_PASS=... (maintainer only).
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TC="${TOOLCHAIN:-$ROOT/.toolchain}"
BUILD_TOOLS=36.1.0
PLATFORM=android-36

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then :;
elif [ -x "$TC/jdk/Contents/Home/bin/javac" ]; then JAVA_HOME="$TC/jdk/Contents/Home";
elif [ -x "$TC/jdk/bin/javac" ]; then JAVA_HOME="$TC/jdk";
else echo "No JDK found. Run scripts/setup-toolchain.sh first." >&2; exit 1; fi
if [ -z "${ANDROID_HOME:-}" ] || [ ! -d "$ANDROID_HOME/build-tools/$BUILD_TOOLS" ]; then ANDROID_HOME="$TC/android-sdk"; fi
[ -d "$ANDROID_HOME/build-tools/$BUILD_TOOLS" ] || { echo "No Android SDK found. Run scripts/setup-toolchain.sh first." >&2; exit 1; }
export JAVA_HOME
PATH="$JAVA_HOME/bin:$PATH"
BT="$ANDROID_HOME/build-tools/$BUILD_TOOLS"
JAR="$ANDROID_HOME/platforms/$PLATFORM/android.jar"
APP="$ROOT/app"
OUT="$ROOT/build"

VERSION=$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' "$APP/AndroidManifest.xml")
rm -rf "$OUT"
mkdir -p "$OUT/gen/com/odin/tvpanel" "$OUT/classes" "$OUT/assets/web" "$OUT/assets/debloat"
cp "$APP"/web/* "$OUT/assets/web/"
cp "$ROOT"/debloat/*.txt "$OUT/assets/debloat/"
# ODIN_UPDATE_TEST_BASE (testing only, never for releases): the update check asks this server
# (<base>/latest.json, assets under <base>/) instead of GitHub, and plain HTTP is allowed for it.
UPDATE_TEST_BASE="${ODIN_UPDATE_TEST_BASE:-}"
case "$UPDATE_TEST_BASE" in *'"'*|*'\\'*) echo "bad ODIN_UPDATE_TEST_BASE" >&2; exit 1;; esac
[ -z "$UPDATE_TEST_BASE" ] || echo "WARNING: test build, updates come from $UPDATE_TEST_BASE (do not release this APK)"
printf 'package com.odin.tvpanel;\nfinal class BuildInfo {\n    static final String VERSION = "%s";\n    static final String UPDATE_TEST_BASE = "%s";\n    private BuildInfo() { }\n}\n' "$VERSION" "$UPDATE_TEST_BASE" \
  > "$OUT/gen/com/odin/tvpanel/BuildInfo.java"

echo "[1/6] resources"
"$BT/aapt2" compile --dir "$APP/res" -o "$OUT/res.zip"
"$BT/aapt2" link -I "$JAR" --manifest "$APP/AndroidManifest.xml" -A "$OUT/assets" \
    --java "$OUT/gen" -o "$OUT/unsigned.apk" "$OUT/res.zip"

echo "[2/6] java"
find "$APP/src" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 --release 8 -classpath "$JAR" -Xlint:-options -d "$OUT/classes" @"$OUT/sources.txt" 2>&1 \
  | grep -v "^Note:" || true
[ -n "$(find "$OUT/classes" -name 'PanelService.class')" ] || { echo "javac failed" >&2; exit 1; }

echo "[3/6] dex"
(cd "$OUT/classes" && jar cf ../classes.jar .)
"$BT/d8" --release --min-api 26 --lib "$JAR" --output "$OUT" "$OUT/classes.jar"

echo "[4/6] package"
(cd "$OUT" && zip -q unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

echo "[5/6] sign"
if [ -n "${ODIN_KEYSTORE:-}" ]; then
  KS="$ODIN_KEYSTORE"
  : "${ODIN_KEYSTORE_PASS:?set ODIN_KEYSTORE_PASS}"
  PASS="$ODIN_KEYSTORE_PASS"
else
  KDIR="$HOME/.odin-tv-magic"
  KS="$KDIR/local.jks"
  if [ ! -f "$KS" ]; then
    mkdir -p "$KDIR" && chmod 700 "$KDIR"
    LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 32 > "$KDIR/local.pass"
    chmod 600 "$KDIR/local.pass"
    keytool -genkeypair -keystore "$KS" -alias odintv -keyalg RSA -keysize 3072 -validity 36500 \
      -storepass "$(cat "$KDIR/local.pass")" -keypass "$(cat "$KDIR/local.pass")" \
      -dname "CN=Odin TV Magic (local build)" >/dev/null 2>&1
    chmod 600 "$KS"
    echo "Created a personal signing key in $KS (keep it to be able to update)."
  fi
  PASS="$(cat "$KDIR/local.pass")"
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$PASS" --out "$OUT/odin-tv-magic.apk" "$OUT/aligned.apk"

echo "[6/6] verify"
"$BT/apksigner" verify --print-certs "$OUT/odin-tv-magic.apk" | grep -E "Signer #1 certificate (DN|SHA-256)"
rm -f "$OUT"/unsigned.apk "$OUT"/aligned.apk "$OUT"/*.idsig
echo "APK: $OUT/odin-tv-magic.apk (v$VERSION, $(wc -c < "$OUT/odin-tv-magic.apk" | tr -d ' ') bytes)"
if command -v shasum >/dev/null; then shasum -a 256 "$OUT/odin-tv-magic.apk"; else sha256sum "$OUT/odin-tv-magic.apk"; fi
