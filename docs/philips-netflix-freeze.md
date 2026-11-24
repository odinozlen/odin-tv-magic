# Philips Android TV: Netflix stuck at 86% (and other apps failing after days of uptime)

## Symptom

On Philips Android TVs (seen on a 50PUS8507, Android 11, MediaTek PH1M platform) Netflix stops at the
red loading circle around **86%** and never starts. Other apps may fail to start too. Turning the TV
off and on with the remote does not always help; a full power cycle does, for a few days.

## Cause

`org.droidtv.tv.frameworklogger` (TP Vision's logging service) leaks threads: about **250 new threads
per day**, never released. The TV is a 32-bit system (armeabi-v7a); every thread reserves address space
for its stack. At roughly **1,100 threads** the process can no longer create threads and the system
log shows:

```
pthread_create failed: couldn't allocate ... Try again
```

Netflix's player depends on services that talk to the logger, so playback setup stalls at 86%.
Standby keeps the process alive, which is why a normal "off" does not fix it.

How to check on your TV:

```bash
adb shell 'grep Threads /proc/$(pidof org.droidtv.tv.frameworklogger)/status'
```

A healthy value is a few dozen. Several hundred means it is leaking.

## What does NOT work

| Attempt | Result |
|---|---|
| `pm disable-user org.droidtv.tv.frameworklogger` | Philips re-enables it at boot; while disabled another component keeps calling it and floods the log with "not found" errors |
| Disabling the component | `SecurityException: Shell cannot change component state` |
| `pm suspend` | Accepted, but the service keeps running |

## Fix

Restart the service before it reaches the limit:

```bash
adb shell am force-stop org.droidtv.tv.frameworklogger
```

It restarts on its own with a few threads. Nothing else is affected (Ambilight, HDMI, sound, apps).

**Odin TV Magic does this automatically:** every 30 minutes it checks the thread count and restarts
the logger above 500 threads. The Home tab shows the count, and *Clean up* restarts it as well.
