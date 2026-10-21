# Samsung Tizen (2021 AU8000): what a set exposes, and what a port would face

One owner's reconnaissance from a Mac on the same network, not a port and not a tested list. It was
reported in [#441](https://github.com/Gathin23/cleanyourtv/issues/441) on 2026-10-20 by
[@csmith1133](https://github.com/csmith1133), with nothing changed on the TV. Nothing in `tools/`
speaks Tizen: `tools/tv.py` drives `adb`, and this set has no `adb` to connect to.

| Field | Observed value |
|---|---|
| Retail model | UN50AU8000FXZA, 50" AU8000, 2021 |
| Model string (`/api/v2/`) | `21_KANTSU2E_UHD_VOICE` |
| System | Tizen, US firmware |
| Network | Wi-Fi |

## What answers without touching the TV

- SSDP replies as `Samsung-Linux/4.1, UPnP/1.0, Samsung_UPnP_SDK/1.0`, with a DMR on `9197` and
  `nservice` on `7678`.
- `GET http://<tv>:8001/api/v2/` returns the model, the power state, `TokenAuthSupport: true` and
  `developerMode: 0`.

| Port | State | What it is |
|---|---|---|
| 8001 | open | HTTP API and the remote-control websocket |
| 8002 | open | the same remote over TLS |
| 8080 | open | open in the report, purpose not identified |
| 9080 | open | open in the report, purpose not identified |
| 9197 | open | UPnP DMR (media rendering) |
| 7678 | open | `nservice` |
| 5555 | closed | no ADB on retail firmware |
| 26101 | closed | no `sdb` while Developer Mode is off |

## What that rules out

- **No shell, so no `pm disable-user`.** Samsung's own apps cannot be disabled from outside on
  retail firmware, which is what every list in `examples/` is built on.
- **No screenshot.** `screencap` is part of that missing shell, and there is no capture API on 8001
  or 8002, so a script cannot see the TV to decide what the next key press should be.
- **No Home takeover.** No third-party app can take the Home button, so `launcher/` has nowhere to
  go here even if it could be installed.

## What a Tizen cleanup could still be

The remote on 8002 accepts key presses once the "Allow" prompt is accepted on the TV, and
`/api/v2/applications/<appId>` can launch an app, so a script can walk the settings menus the owner
named in the report: interest-based ads, viewing information services, autorun Smart Hub. That is a
key sequence with a person watching the screen, not a package list. Combined with DNS blocking at
the router, it is the realistic shape of a Tizen version of this repo: settings by remote keys plus
DNS, rather than debloat plus a launcher.

A port like that belongs in its own repository, with a row in [SHOWCASE.md](../../SHOWCASE.md), the
same as the [webOS launcher](https://github.com/OmkarK97/webos-vibehack).

## What this report does not establish

- One set, one firmware, one region, one session. Tizen versions differ between years and markets,
  and none of this was checked on a second TV.
- Developer Mode was off, so the path through `sdb` on 26101 was not tested. A developer-mode set
  would still not have `adb`, and no `pm`-style package disabling was claimed.
- Whether the remote socket asks for the "Allow" prompt again after every TV reboot, and how long
  that grant lasts, is unmeasured.
- The app-launch endpoint was reported to work; which Smart Hub apps answer it, and whether it can
  reach a settings activity rather than only an app, is unmeasured.

If you have one of these sets and want to extend this page, a second unit on a different year or
region is the useful data point.
