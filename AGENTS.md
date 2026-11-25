# Instructions for AI assistants

You are helping someone set up **Odin TV Magic** on their Android TV and, optionally, debloat it.
Follow this file exactly. The person may not be technical: explain each step in plain words, in their
language, and ask before anything that changes the TV.

## Hard rules (never break these)

1. **Never uninstall anything.** Use only `pm disable-user --user 0 <package>`. Never `pm uninstall`,
   never `pm uninstall --user 0`, never `pm clear`. Every change must be undoable with `pm enable`.
2. **No root, no bootloader unlock, no custom firmware, no system-partition changes.**
3. **Save a baseline before the first change:** `scripts/tv.sh baseline <tv-ip>`.
4. **Ask the person before every batch** of changes. Show the package names and what each does.
   At most 10 packages per batch. After each batch, have them test (see checklist) before continuing.
5. **Never disable** anything in `debloat/*-keep.txt`, and never: `com.google.android.gms`,
   `com.google.android.gsf`, `com.android.vending`, `com.android.systemui`, settings apps, the current
   home screen, the keyboard, or anything related to inputs/HDMI, audio or the remote.
6. **Google TV:** never disable `com.google.android.apps.tv.launcherx` or
   `com.google.android.tungsten.setupwraith`. On some devices that needs a factory reset to recover.
7. **Never run** `svc wifi disable`, `svc data disable` or anything that can cut the network while you
   are connected over network ADB: you would lock yourself out.
8. **Do not press keys or launch things on the TV while the person is watching** unless they asked.
   If you must send BACK/HOME, first check the foreground app
   (`dumpsys activity activities | grep -m1 -E 'mResumedActivity|topResumedActivity'`).
9. **Never accept licenses, prompts or dialogs on the person's behalf.** The Android SDK license
   (`scripts/setup-toolchain.sh`) and the TV's "Allow debugging?" prompt are theirs to accept.
10. **Privacy:** do not put the person's IP address, MAC address, names or keys into files that will be
    shared or committed. Local state goes to `local/` (git-ignored).

If something breaks: re-enable the last batch (`scripts/tv.sh restore <tv-ip>`) before anything else.

## Step 1: install the app on the TV

Ask for the TV brand/model, whether they have a computer you can run commands on, and the TV's IP
address (TV: Settings › Network › status). Then pick one way:

- **No computer, or Windows without a Unix shell:** the person installs *Downloader* (by AFTVnews) from
  the Play Store on the TV, types `odinozlen.com/tv` into it (a short redirect to the latest release APK; the full link is
  `https://github.com/odinozlen/odin-tv-magic/releases/latest/download/odin-tv-magic.apk`) and allows
  Downloader to install apps when asked.
- **With a computer:** this needs ADB on the TV first, so walk them through Developer options and
  *USB debugging* (+ *Network debugging* if the TV has it; Google TV / Android 12+ without it: *Wireless
  debugging*, then `adb pair <tv-ip>:<pairing-port>` with the code from *Pair device with pairing code*
  and `adb connect <tv-ip>:<port shown on that screen>`). Then:

  ```bash
  curl -LO https://github.com/odinozlen/odin-tv-magic/releases/latest/download/odin-tv-magic.apk
  shasum -a 256 odin-tv-magic.apk     # compare with the SHA-256 in the release notes
  scripts/tv.sh install <tv-ip> odin-tv-magic.apk
  ```

  Only `adb` is needed for this: use one on PATH (`brew install android-platform-tools`, or Google's
  platform-tools zip). Building from source (`scripts/setup-toolchain.sh` + `scripts/build.sh`) downloads
  a JDK and the Android SDK and asks the person to accept Google's SDK license; do it only if they want to.

### When the app is installed: hand over to the phone (do not skip)

People who set this up through an assistant often think they continue on the computer. They do not: from
here on everything happens on the TV (with the remote) and on the **phone**. Say this, in their language,
in these words or very close:

> "Odin TV Magic is installed. From now on you use your TV **from your phone**, not from this computer and
> not from this chat. Open *Odin TV Magic* on the TV and scan the QR code on the TV with your **phone's
> camera**. Then add the page to your phone's home screen so it opens like an app."

- Never give the panel address (`http://<tv-ip>:8080`) as a link to open on the computer. If you must
  write it, say "open this in your phone's browser".
- Opened on a computer, the panel shows "This panel is made for your phone"; if they report that, repeat
  the message above.

## Step 2: setup on the TV, then the phone

Everything from here happens on the TV and the phone.

The person does this with the remote and the phone; you explain. You cannot press the TV's buttons for
them, and must not accept prompts on their behalf.

1. **Open "Odin TV Magic" on the TV.** If ADB is not set up for the app yet, it shows a setup guide
   (step 1–3) with a button per step that opens the right settings screen:
   developer mode (*About › Build* pressed 7 times; the app comes back by itself) → *USB debugging*
   (and *Network debugging* if present) → the *"Allow debugging?"* prompt: tick *Always allow*, press
   *Allow* (this is the app's own key). If the prompt does not show: *Show the window again* on the TV.
   If the TV has no network-debugging switch (some Google TV / Android 12+): *Wireless debugging*,
   *Pair device with pairing code*, and the code goes into the panel: *Settings › TV connection*. Tell
   them to tick *Always allow on this network*.
2. **Scan the QR code on the TV with the phone camera** (works before step 1 is done; the panel then runs
   in basic mode and dims what needs the setup). Then add it to the phone's home screen: iPhone Safari
   *Share › Add to Home Screen*, Android Chrome *⋮ › Add to Home screen*.
3. **Philips:** in the panel, *Show PIN on TV* › enter the PIN (Ambilight, picture, fast remote).

Verify: the panel's Home tab shows RAM and uptime, nothing is dimmed and the setup card is gone.

## Step 3 (optional): debloat

1. `scripts/tv.sh baseline <tv-ip>`
2. Pick the list:
   - Philips Android TV 11: `debloat/philips-android11.txt`. Remove lines for things the person uses
     (e.g. DLNA, USB media, TalkBack, regional apps they watch). The `launcher` group only after another
     home screen is the default (Settings tab › Home screen in the panel).
   - Other brands: use the tested lists of [cleanyourtv](https://github.com/Gathin23/cleanyourtv/tree/main/examples)
     for the exact model, converted to the same tab-separated format, or build a list with the person by
     reviewing `local/<ip>/baseline-*/packages-all.txt`. Unknown vendor packages: do not touch.
3. Disable in batches of ≤10 (split the file), testing after each:
   `scripts/tv.sh disable <tv-ip> <batch-file>`

### Test checklist after every batch
- Home screen opens; remote works (D-pad, back, home, volume).
- Inputs/Sources menu opens and switches HDMI; picture and sound on HDMI.
- The streaming apps they use start and play (Netflix, YouTube, …); on-screen keyboard works.
- Philips: Ambilight still follows the picture.
- After the last batch: reboot the TV (`adb reboot`) and check everything again.

## Updating an existing install

Updating keeps the app's data: paired phones, settings, the ADB key and the Philips pairing stay. Never
uninstall first (that wipes them and the phones must pair again).

- **From the panel (preferred):** when a new version exists, the phone panel shows *"1.0.x is ready – Update"*
  (and *Settings › Updates › Check now* asks at once). The person taps **Update**; the TV downloads, verifies
  (SHA-256 file + same signing key) and installs it, and the panel reloads by itself after about a minute.
  This needs the ADB setup (Step 2). Do not tap it for them.
- **With a computer:**

  ```bash
  curl -LO https://github.com/odinozlen/odin-tv-magic/releases/latest/download/odin-tv-magic.apk
  curl -LO https://github.com/odinozlen/odin-tv-magic/releases/latest/download/odin-tv-magic.apk.sha256
  shasum -a 256 -c odin-tv-magic.apk.sha256
  adb connect <tv-ip>:5555
  adb install -r odin-tv-magic.apk      # -r = replace, keeps data; never `pm uninstall`
  ```

  The app restarts by itself after the update (no need to open it).
- **No computer, no ADB setup:** on the TV, *Downloader* › `odinozlen.com/tv` › install over the old
  version (Android asks "Do you want to install an update to this app?").
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE` means the installed copy was signed with another key (e.g. built
  locally). Explain that the data (paired phones) is lost if they switch, and ask before uninstalling that
  app (it is the person's own app, not a system package; rule 1 is about the TV's packages).
- The daily check asks only `api.github.com` (no personal data) and can be turned off in *Settings ›
  Updates*.

## Known pitfalls

- macOS: `adb connect` fails with "No route to host" while ping works → the adb binary has no Local Network
  permission (System Settings › Privacy & Security › Local Network). An adb server started by a different adb
  binary (e.g. an emulator SDK) causes this too: `adb kill-server`, then start the allowed one.
- adbd logs "prompt currently pending, skipping" and no dialog is visible → `adb tcpip 5555` restarts adbd.

## Philips-specific knowledge

- `org.droidtv.eum` and `org.droidtv.fastip` look like help/promo apps but the Sources drawer depends on them.
- `org.droidtv.aurora` disabled → xtv retry loop.
- Philips Google TV (Android 12+) differs from Android TV 11: Ambilight / Hue is `org.droidtv.ambisvc`
  (instead of `org.droidtv.ambihuecommon` / `alhuewizard`) and settings are `com.tpv.cul.settings`. Do not
  disable either. JointSpace (`org.droidtv.xtv`, ports 1925/1926) exists on both. No tested debloat list yet.
  The quick menu used for picture styles is `org.droidtv.settings/.quicksettings.QuickSettingsActivity` on
  Android TV 11; the panel resolves `org.droidtv.action.EXPERIENCE_MENU` instead of assuming it.
- `org.droidtv.tv.frameworklogger` leaks threads → Netflix freezes at 86% after days of uptime. Do NOT
  disable it (it comes back and spams errors); the panel restarts it automatically.
  See [docs/philips-netflix-freeze.md](docs/philips-netflix-freeze.md).
- `com.google.android.apps.mediashell`, `com.phorus.playfi.tv`, `io.mimi.tvapp` are re-enabled by the
  firmware on every boot; disabling them is pointless.
- If apps say "no internet" but the network works: the clock may be wrong (TLS fails). Fix:
  `settings put global auto_time 1`.

## Working on the code

- Java 8 source, no Gradle, no dependencies: `app/src`. Web UI: `app/web` (no build step).
- `scripts/build.sh` must stay warning-free. Test on a TV or the Android TV / Google TV emulator.
- Security model: [SECURITY.md](SECURITY.md). Do not weaken it (Host check, pairing, request signing,
  fixed command list). Anything from the network must never reach the shell unvalidated.
- Every user-facing string exists in English and Turkish (`Strings.java`, `app/web/app.js`).
- The update check (`Updater.java`) talks only to GitHub over HTTPS. For tests, build with
  `ODIN_UPDATE_TEST_BASE=http://10.0.2.2:8765` (emulator host): it reads `<base>/latest.json` (same shape as
  GitHub's release JSON) and the assets under `<base>/`; the test copy's manifest also needs
  `android:usesCleartextTraffic="true"`. Never release such a build; release builds leave it empty.
- Never rename or remove the launcher entry `com.odin.tvpanel/.Magic` (activity-alias in the
  manifest) or the package id: launchers remember the exact component of a tile, and existing
  installs would get a tile that no longer opens.

## Releasing (maintainer)

1. Bump `versionCode` and `versionName` in `app/AndroidManifest.xml`; move `## Unreleased` in CHANGELOG.md to
   the new version.
2. Build with the release key: `K=$HOME/.odin-tv-magic; ODIN_KEYSTORE=$K/release.jks
   ODIN_KEYSTORE_PASS="$(cat $K/release.pass)" sh scripts/build.sh` (warning-free; never commit or move the key).
3. `cd build && shasum -a 256 odin-tv-magic.apk > odin-tv-magic.apk.sha256`
4. Tag `vX.Y.Z` and create the GitHub release with **both** `odin-tv-magic.apk` and
   `odin-tv-magic.apk.sha256` (exactly these names) and the SHA-256 in the notes. Without the `.sha256`
   file, installs that update from the panel refuse the release. Never mark a test build as the latest
   release: every install checks `releases/latest`.
