#!/bin/sh
# Safe ADB helper for Android TV debloating and installing the panel.
#
#   scripts/tv.sh connect  <tv-ip>             connect over network ADB (port 5555)
#   scripts/tv.sh baseline <tv-ip>             save the current state to local/<ip>/baseline-<time>/
#   scripts/tv.sh disable  <tv-ip> <list>      disable packages from a list file, one by one, recorded
#   scripts/tv.sh restore  <tv-ip> [record]    re-enable everything this script disabled
#   scripts/tv.sh install  <tv-ip> [apk]       install the panel (default build/odin-tv-magic.apk)
#
# Rules this script enforces:
#   - only `pm disable-user --user 0` (never uninstall), every change is undoable with `pm enable`
#   - skips packages that are not installed, already disabled, or listed in debloat/*-keep.txt
#   - writes every change to local/<ip>/disabled.txt before making the next one
# The list file format is: package <TAB> group <TAB> reason   (lines starting with # are ignored)
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if command -v adb >/dev/null 2>&1; then ADB=adb
elif [ -x "$ROOT/.toolchain/android-sdk/platform-tools/adb" ]; then ADB="$ROOT/.toolchain/android-sdk/platform-tools/adb"
else echo "adb not found. Install Android platform-tools or run scripts/setup-toolchain.sh" >&2; exit 1; fi

cmd="${1:-}"; ip="${2:-}"
[ -n "$cmd" ] && [ -n "$ip" ] || { sed -n '2,15p' "$0"; exit 1; }
case "$ip" in *:*) target="$ip" ;; *) target="$ip:5555" ;; esac
LOCAL="$ROOT/local/$(echo "$ip" | tr ':' '_')"
mkdir -p "$LOCAL"
sh_() { "$ADB" -s "$target" shell "$@" </dev/null; }

connect() {
  "$ADB" connect "$target" >/dev/null 2>&1 || true
  if ! sh_ true 2>/dev/null; then
    echo "Cannot reach $target. Is the TV on, network debugging enabled, and the 'Allow debugging?' prompt accepted?" >&2
    exit 1
  fi
}

case "$cmd" in
  connect)
    connect
    echo "Connected: $(sh_ getprop ro.product.manufacturer) $(sh_ getprop ro.product.model), Android $(sh_ getprop ro.build.version.release)"
    ;;
  baseline)
    connect
    d="$LOCAL/baseline-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$d"
    sh_ pm list packages -f > "$d/packages-all.txt"
    sh_ pm list packages -d > "$d/packages-disabled.txt"
    sh_ pm list packages -3 > "$d/packages-thirdparty.txt"
    sh_ getprop > "$d/getprop.txt"
    sh_ dumpsys meminfo > "$d/meminfo.txt"
    sh_ cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME > "$d/home.txt"
    for k in window_animation_scale transition_animation_scale animator_duration_scale; do
      printf '%s=%s\n' "$k" "$(sh_ settings get global $k)"; done > "$d/settings.txt"
    echo "Baseline saved to $d"
    ;;
  disable)
    list="${3:?list file}"
    connect
    keep=$(cat "$ROOT"/debloat/*-keep.txt 2>/dev/null | grep -v '^#' | cut -f1 | tr '\n' ' ')
    installed=$(sh_ pm list packages | sed 's/^package://' | tr -d '\r')
    disabled=$(sh_ pm list packages -d | sed 's/^package://' | tr -d '\r')
    record="$LOCAL/disabled.txt"
    grep -v '^#' "$list" | while IFS="$(printf '\t')" read -r pkg group reason; do
      [ -n "$pkg" ] || continue
      case " $keep " in *" $pkg "*) echo "SKIP (keep-list) $pkg"; continue ;; esac
      echo "$installed" | grep -qx "$pkg" || { echo "skip (not installed) $pkg"; continue; }
      echo "$disabled" | grep -qx "$pkg" && { echo "skip (already off) $pkg"; continue; }
      if sh_ pm disable-user --user 0 "$pkg" | grep -q "new state: disabled"; then
        printf '%s\t%s\t%s\n' "$pkg" "$(date +%Y-%m-%dT%H:%M:%S)" "$group" >> "$record"
        echo "disabled $pkg  ($reason)"
      else
        echo "FAILED  $pkg"
      fi
    done
    echo "Recorded in $record. Now test: HDMI/Sources, sound, remote, your streaming apps. Undo: scripts/tv.sh restore $ip"
    ;;
  restore)
    record="${3:-$LOCAL/disabled.txt}"
    [ -f "$record" ] || { echo "No record at $record" >&2; exit 1; }
    connect
    cut -f1 "$record" | sort -u | while read -r pkg; do
      [ -n "$pkg" ] && sh_ pm enable "$pkg" >/dev/null && echo "enabled $pkg"
    done
    mv "$record" "$record.restored-$(date +%Y%m%d-%H%M%S)"
    echo "Done. Everything in the record was re-enabled."
    ;;
  install)
    apk="${3:-$ROOT/build/odin-tv-magic.apk}"
    [ -f "$apk" ] || { echo "No APK at $apk (run scripts/build.sh or download a release)" >&2; exit 1; }
    connect
    "$ADB" -s "$target" install -r "$apk"
    sh_ am start -n com.odin.tvpanel/.Magic >/dev/null
    echo "Installed. On the TV: follow the setup on the screen and accept 'Allow debugging?' (tick 'Always allow'), then scan the QR code with your phone."
    ;;
  *) sed -n '2,15p' "$0"; exit 1 ;;
esac
