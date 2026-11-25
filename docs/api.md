# HTTP API

The web panel is a thin client over this API. Everything is served by the app on the TV, port 8080.

## Authentication

Unauthenticated: `GET /`, static files, `GET /api/hello` (returns `{time, version}`), `POST /api/pair`.

`POST /api/pair {"code": "123456", "name": "iPhone Safari"}` → `{id, key}` (key = 32 random bytes,
base64url). The code is the one shown by the app on the TV.

Every other request carries:

```
X-Auth: <id>.<timestamp_ms>.<nonce>.<hex HMAC-SHA256(key, message)>
message = METHOD "\n" PATH "\n" timestamp "\n" nonce "\n" hex(SHA-256(body))
```

`PATH` is the raw request path without query string. Timestamp within ±120 s of the TV clock
(`/api/hello` gives it); nonce 8–64 chars, single use. Optional `X-Lang: tr|en` for messages.
A 401 has `reason: "pair"` (unknown/invalid) or `reason: "clock"` (+ `time`, retry once).

## Endpoints

| Method | Path | Body | Notes |
|---|---|---|---|
| GET | /api/status | | RAM, uptime, ADB state, foreground app, sources, Philips info (`paired`, `reachable`, `has_ambilight`: true/false/null = unknown, `ambilight`), `caps` (what works now: `adb`, `nav`, `text`, `apps`, `sleep`, `media`, `volume`; without ADB the panel is in basic mode) |
| GET | /api/apps | | installed launchable apps, favourites first |
| GET | /api/appicon/{package} | | PNG 96×96 |
| POST | /api/launch/{package} | | open an installed app |
| POST | /api/source | `{id}` | switch to the TV tuner or an HDMI input (id from status.sources; `tuner: true` marks the tuner) |
| POST | /api/action/{clean,home,sleep,reboot} | | |
| POST | /api/remote/{key} | | up, down, left, right, ok, back, home, playpause, rewind, forward, volup, voldown, mute, options, chup, chdown |
| POST | /api/keyboard | `{text}` | ≤200 chars |
| POST | /api/key/{del,enter} | | |
| GET/POST | /api/volume | `{value?, muted?}` | |
| GET | /api/screenshot | | JPEG |
| GET | /api/settings | | party mode, hidden styles, paired phones |
| POST | /api/settings | `{partyMode?, hiddenStyles?, favorites?}` | |
| POST | /api/devices/revoke | `{id}` | unpair a phone |
| GET | /api/launchers | | installed home screens |
| POST | /api/launcher | `{component}` | set the home screen (turns the stock Android TV launcher off only if the TV ignores the choice) |
| POST | /api/launcher/restore | | back to the factory home screen |
| POST | /api/launcher/install | `{pkg}` | open the Play Store page of a recommended launcher |
| GET | /api/debloat | | suggestions for this TV (`enabled` = can be turned off) |
| POST | /api/debloat/disable | `{packages:[…]}` | ≤10, only packages from the bundled lists |
| POST | /api/debloat/enable | `{packages:[…]}` | turn back on |
| GET | /api/addons | | AirPlay receiver: installed / download progress |
| POST | /api/addons/airplay | | download (pinned SHA-256) and install |
| POST | /api/adb/retry | | connect again so the TV shows "Allow debugging?" again (polling never re-raises a dismissed prompt) |
| POST | /api/adb/pair | `{code, port?}` | wireless-debugging pairing (port found via mDNS if omitted) |
| POST | /api/surprise | `{message?}` | party mode, only when enabled |
| POST | /api/surprise/stop | | |
| POST | /api/philips/pair/{start,finish} | `{pin}` | Philips JointSpace pairing; a refusal is a 502 that names the TV's `error_id` |
| GET | /api/philips/diag | | model, Android version and what JointSpace reports (`api`, `os_type`, `featuring`, `last_error`); no ids, keys or addresses |
| POST | /api/ambilight/{on,off,video,music,color} | `{r,g,b}` for color | Philips; the state is read back, 502 if the TV accepted but did not change it |
| GET | /api/picture | | styles, current, brightness, `menu` (the TV has a quick menu the panel can open; null = unknown until ADB) (Philips) |
| POST | /api/picture/style | `{name}` | ~10 s, uses the TV's quick menu |
| POST | /api/picture/refresh | | re-read the style list |
| POST | /api/picture/brightness | `{value}` 0–100 | |

Responses: `{ok, msg}` for actions; messages are localized via `X-Lang`.
