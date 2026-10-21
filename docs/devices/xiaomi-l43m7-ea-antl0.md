# Xiaomi L43M7-EA (MiTV-ANTL0): startup ad off with one setting

On this TV `settings put system boot_ad_switch 0` removed the 15-second startup ad, checked after a full
restart. The stock launcher stays; nothing is disabled or installed.

| Retail model | Internal model | Android | Firmware | Stock launcher |
| --- | --- | --- | --- | --- |
| Xiaomi L43M7-EA, 43" | MiTV-ANTL0 / starwars | 5.1.1 (SDK 22) | 23.2.4.2225 | `com.mitv.tvhome` 3.38.1017 |

The launcher reads the setting, so its version matters as much as the firmware. On this configuration the
older `persist.bootvideo.bootanim false` method from the [ANTL0 list](../../examples/redmi-mitv-antl0/debloat.txt)
survived a restart but did not remove the ad.

```sh
TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'settings get system boot_ad_switch'   # save it; null here
TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'settings put system boot_ad_switch 0'
TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'settings delete system boot_ad_switch' # undo, if it was null
```

If the key had a value before, put that value back instead of deleting it. The ~10-second Xiaomi logo
still shows. Network ADB closed after the restart; turning ADB debugging off and on brought it back.

Limits: the launcher defaults the key to 1 and its ad-permission screen (`AuthManageActivity`) can set it
back to 1, so changing that control may undo this. A separate seven-day timer uses the Global
`mitv.settings.advertise.*` keys, not this one. Launcher and firmware updates, factory resets and the
replacement launcher were not tested.
