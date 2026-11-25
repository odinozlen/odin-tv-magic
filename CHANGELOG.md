# Changelog

## 1.0.2

Make clear that the TV is used from the phone.

- TV screen: a headline "Control this TV from your phone"; the address line says to open it in the
  phone's browser.
- Opened on a computer, the panel first says it is made for the phone and how to get there (scan the TV's
  QR code with the phone camera); "Continue on this computer anyway" is still there. The QR code's
  one-time code is not used on the computer, so it still works on the phone.
- "Add to Home Screen" is now "Add the remote to your phone": it says why first (opens with one tap, no QR
  code) and numbers the steps for Safari and Chrome.
- AGENTS.md: assistants must hand over to the phone once the app is installed, with a fixed message, and
  never give the panel address as a link to open on the computer.

## 1.0.1

Philips: Ambilight and pairing explain themselves.

- The Ambilight card is always on the Picture tab, with the pairing button in it: before, it only appeared
  after pairing, and the pairing card did not say it was for Ambilight.
- The panel asks the TV's JointSpace what it supports: Ambilight is hidden when the TV reports none, and a
  TV whose JointSpace does not answer gets a plain message instead of a button that cannot work.
- Pairing errors name the TV's reason (e.g. `CONCURRENT_PAIRING`, `TIMEOUT`) instead of "try again"; a
  wrong PIN keeps the pairing open for another try.
- Ambilight commands are checked: the panel reads the state back and says when the TV accepted a command
  but did not change anything. Colour buttons now confirm too.
- Ambilight *Video* after *Music* works: the panel no longer re-sends the Ambilight mode when it is
  already set, which made the TV's Ambilight driver ignore the switch and keep following the sound.
- Picture style: the panel asks the TV which app shows its quick menu instead of assuming the Android
  TV 11 one (`org.droidtv.settings`), which Philips Google TV (e.g. 65PUS8808) does not have; "Read
  styles" failed there with "Could not open the quick menu". A TV without such a menu now says that the
  style is changed with the remote (brightness still works); the diagnostics show the menu and why it
  failed.
- *Picture › Philips diagnostics*: what the TV reports (model, Android version, JointSpace API version and
  features, last error), with no ids, keys or addresses, to copy into a bug report.

## 1.0.0

First public release.

- Phone web panel for any Android TV / Google TV; Philips features (Ambilight, picture style,
  brightness, volume via JointSpace, logger watchdog) are detected automatically.
- Google TV / Android 12+: wireless-debugging pairing from the panel, TLS transport, automatic
  re-enable after reboot.
- Phone pairing with QR code / one-time code and signed requests.
- Fast remote through a persistent key injector.
- Speed up the TV: reviewed suggestions of pre-installed apps to turn off, undo in one tap.
- Change the TV's look: switch to Projectivy / FLauncher (Play Store install) and back.
- Extras: one-tap AirPlay receiver install, checksum-verified.
- English and Turkish UI; party mode follows the phone's language and is off by default.
- Playing-now bar, highlighted active input with a way back to the TV home, touchpad remote mode,
  drag-to-reorder favourites, and a one-flow first-run setup (TV, Philips, home screen, apps).
- Inputs include the TV's own tuner ("Watch TV", named as the TV names it) next to HDMI devices.
- Clean up only closes apps (no `am kill-all`, which on Philips restarts TV system services), and
  input buttons ignore taps while it runs, so a shifting page can never switch the TV's input.
- Setup on the TV, with the remote only: the app finds what is missing (developer mode, USB / network
  debugging, the "Allow debugging?" prompt), and each step's button opens the right settings screen.
  The app returns by itself once developer mode or USB debugging turns on, shows the menu path when a
  brand hides the screen, and can raise a dismissed prompt again. Menu names for Android TV, Google TV
  and Fire TV.
- Basic mode before that setup: volume and play/pause through Android's own APIs on every TV; on
  Philips (after the PIN) also remote, keyboard, apps, sleep and what is on screen, through JointSpace.
  The panel dims what still needs the setup and says where to finish it; the phone can pair first.
- A prompt the user dismissed is not raised again by the panel's polling, only by "Check again".
- Works on Android 8 to 16, Android TV and Google TV (end-to-end tested, see README):
  - one long-lived ADB connection with a stream per command: the TV's "Allow debugging?" is asked
    once per connection even if the TV forgets "Always allow", and commands run in parallel;
  - while the TV shows that prompt the panel answers at once ("approve on the TV") instead of
    hanging; a hint on the TV screen helps when the prompt is hidden behind it;
  - port 8080 taken (Kodi, media servers…) → the next free port, remembered for next time;
  - the TV's address comes from its Wi-Fi/Ethernet network (not a VPN or Wi-Fi Direct interface);
  - volume on Android 8–11 formats, fixed-volume devices (HDMI sticks, soundbars over ARC) get
    − / + / mute that send the remote's volume keys;
  - setup shows the steps that fit the TV's Android version; ADB errors are plain messages.
- Media keys (play/pause, rewind, forward) bypass Philips JointSpace, which turns PlayPause into a
  "." key press.
- Speed-up refuses any package that is not on the reviewed suggestion list.
