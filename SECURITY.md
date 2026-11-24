# Security

Odin TV Magic gives a phone control over a TV. This page explains what protects that control and
what you, the user, must take care of.

## Threat model

Designed for a **home network you trust**. It protects against other devices and web pages on that
network: a guest's phone, a compromised smart plug, a malicious website open on a laptop. It does not
try to protect against someone with physical access to the TV (they can open the pairing screen), or
against a network you do not control.

## What the app does

| Risk | Protection |
|---|---|
| Someone on the network uses the panel | Only phones paired on the TV screen are accepted. The 6-digit code is single-use, expires after 10 min, only exists while the pairing screen is open and burns after 5 wrong tries. Pairing attempts are throttled to 1/s. |
| Someone records network traffic | After pairing, the 256-bit key is never sent again. Each request is signed (HMAC-SHA256 over method, path, timestamp, nonce and body hash). Timestamps must be within 2 min and nonces are single-use, so recorded requests cannot be replayed. Responses are not encrypted (plain HTTP on the LAN). |
| A malicious web page attacks the panel through your browser | No CORS headers (other origins cannot read responses); only requests whose `Host` is an IP address or `localhost` are served (blocks DNS rebinding); strict Content-Security-Policy, `X-Frame-Options: DENY`, `nosniff`, `no-referrer`. |
| Command injection | The panel only runs a fixed set of commands. App launches, inputs and home screens must match what is installed; package/component names are validated and shell-quoted. Text input is restricted to printable ASCII and quoted. |
| Resource exhaustion | Bounded request line (8 KB), headers (40), body (64 KB), 30 s socket timeout, bounded thread pool. |
| Stolen phone | Remove it in the panel (*Settings › Paired phones*) or on the TV (*Forget all phones*). |
| Tampered APK | Release APKs list their SHA-256. The signing key is not in the repository. |

Keys stay in the app's private storage (`allowBackup=false`).

## What you must do

ADB over the network is powerful. Turning it on lets **any device on your network ask** to debug the TV.

- **Never accept an "Allow debugging?" prompt you did not cause.** Only accept it right after installing
  or opening Odin TV Magic (or pressing its *Show the window again*). The prompt shows a key fingerprint; the panel's key name is `odintv@panel`.
- **Do not use this on public, office or shared networks.**
- **Never forward ports 5555, 8080 or 1926** on your router, and keep UPnP port mapping in mind.
- If you stop using the panel, uninstall it and turn off network/wireless debugging.
- Google TV: the app keeps wireless debugging on only on a Wi-Fi network you marked as trusted.

## Philips JointSpace

On Philips TVs the app pairs with the TV's local JointSpace API (port 1926) using the public pairing
secret known from open-source projects (pylips, ha-philipsjs). That API is reachable on your network
regardless of this app; the app only talks to it over `127.0.0.1`. Before the ADB setup is done, app
launches go through JointSpace too, and only with a component taken from the installed-apps list.

## Reporting a vulnerability

Please open a private security advisory on GitHub ("Security" tab › "Report a vulnerability") instead
of a public issue.
