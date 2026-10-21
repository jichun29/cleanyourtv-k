# BYINTEK Love U4, 1080P (Android 9 projector)

Skyworth board `RG_HS352_D1E8_SW_B10` (HiSilicon Hi3751V350, 1 GB RAM), firmware 1.01.08 (build
2025-09). Serial numbers of this version start with `BYU4B01C`. Both halves work: the debloat list and
the Clean Your TV launcher as Home. Tested on 19 October 2026.

| File | What it is |
|---|---|
| `debloat.txt` | The 15 packages that were disabled, grouped and commented |
| `config.json` | Dock order, hidden apps, Inputs panel, columns; push it with `tools/theme.sh` |

All commands run from the repo root, with the projector's IP in `TV_HOST`.

## Turning ADB on

Android's Developer options are blocked on this firmware: unlocking them works (About → Build, 7
times), but the screen closes as soon as it opens. BYINTEK's own settings app hides an ADB switch
instead:

1. Open Settings, the full screen with the categories (Network, Projector, Image and Sound…).
2. Press **Menu, Menu, Up, Up, Down, Down, Left, Left, Right, Right** on the remote. A message says
   "Developer BackDoor key match! Toggle ON" (OFF means it was already shown; repeat it).
3. Open the new **Developer** item at the end of the list and switch **ADB** on.

That sets `persist.sys.usb.config=adb`, and the firmware already sets `service.adb.tcp.port=5555`, so
ADB listens on Wi-Fi straight away and keeps doing so after a restart. Accept the "Allow USB debugging?"
prompt with "Always allow" on the first connection. Switch it off in the same place when you are done.

The firmware is a `userdebug` build (`ro.debuggable=1`, SELinux permissive), so ADB can also run as
root, with nothing flashed. It lasts until the next restart:

```bash
TV_HOST=<projector-ip> tools/venv/bin/python -I -c "import sys; sys.path.insert(0, 'tools'); import tv; tv.connect().root()"
```

The firmware has other key sequences in the same app (projector calibration, factory screens). Leave
them alone: they change keystone, focus and panel calibration.

## Install, from scratch or again

```bash
TV_HOST=<projector-ip> tools/disable.sh examples/byintek-u4/debloat.txt
TV_HOST=<projector-ip> STOCK_LAUNCHER=com.zeasn.whale.saas launcher/build-and-install.sh
TV_HOST=<projector-ip> tools/theme.sh examples/byintek-u4
```

That is the whole install: everything in the list is disabled with `pm disable-user`, as everywhere
else in this repo.

### Optional, this firmware only: hide the Zeasn ad service

The Zeasn ad service is a persistent system app that may install apps silently, and Android starts
persistent apps at boot even when they are disabled. Hiding it keeps it from starting. That goes
further than the rest of the repo: it needs root (above), and only `pm unhide`, again as root, undoes
it. Skip it if you'd rather not:

```bash
TV_HOST=<projector-ip> tools/venv/bin/python -I tools/tv.py 'pm hide com.zeasn.deviceportal.asdprovider.skyworth_hisi352'
```

Restart the projector once so it stops.

## Undo everything

```bash
TV_HOST=<projector-ip> tools/restore.sh examples/byintek-u4/debloat.txt com.zeasn.whale.saas
TV_HOST=<projector-ip> tools/venv/bin/python -I tools/tv.py 'pm unhide com.zeasn.deviceportal.asdprovider.skyworth_hisi352'
```

The second line is only needed if you hid the ad service, and needs root like `pm hide`. The launcher
stays installed but is no longer Home.

## What stays

Everything the projector needs: BYINTEK's settings (`com.sdt.app.skysettings`, also the input picker
and the ADB switch), the projection service (keystone and focus), every HiSilicon package (HDMI, Miracast,
media), the Skyworth media center and the factory test apps, which may hold calibration data.
`com.android.toofifi` stays too: it is the AirPlay, Chromecast and DLNA receiver. Only the two wired
USB casting services are disabled.

RuStore (`ru.vk.store.tv`) is disabled, not uninstalled: `/system/bin/sk_install_yingke_apps.sh`
reinstalls it, YouTube, Netflix and Chrome from `/system/bak` at every boot when they are missing.

## Inputs

Android's TV input list on this firmware shows HDMI1, HDMI2, HDMI3 and CVBS (AV); only HDMI1 exists, and
switching through that list does nothing. Marking the extra ones hidden (`tv_input_hidden_inputs`) lasts
until the next restart, when the firmware clears it. `config.json` points `source_panel_service` at
BYINTEK's own picker (`QuickSourceActivity`, HDMI / Media) instead, and the Inputs pill opens it
directly. That needs a launcher that can start an activity as the source panel.

## Change the dock order

Edit `dock_apps` in `config.json`, then push it again with the `theme.sh` line above, no rebuild needed.
Apps that aren't installed are skipped, so Globoplay and Prime Video appear once installed from Google
Play. The Netflix on this projector (`com.netflix.mediaclient`) is the phone app: it has no TV entry,
so the launcher doesn't show it.

## Firmware recovery

BYINTEK publishes the firmware on its [U4 page](https://www.byintek.com/u4/) (1080P and 720P folders).
The 1080P file is `usb_update_RG_PAI_RG_HS352_D1E8_SW_B10.bin` (1.7 GB) inside the zip. Copy the `.bin`,
not renamed, to the top of a FAT32 drive, plug it in, then hold the power button while connecting power
until a progress bar appears. "Invalid data" partway through means the copy on the drive is damaged:
check it against the original (`shasum -a 256`) or use another drive.
