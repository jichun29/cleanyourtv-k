# Contributing

Thanks for helping more TVs lose their ads. Three kinds of contributions are especially welcome.

## 1. Add your TV model

Every brand ships different bloat. If you ran the prompt (or did it by hand) on a TV that isn't in
`examples/`, add it:

1. Create `examples/<brand>-<model>/debloat.txt`. Copy the format from
   `examples/xiaomi-mi-tv-4a/debloat.txt`: one package per line, grouped with a comment that says
   what the group is, and a trailing comment on anything non-obvious ("re-enable for firmware
   updates", "needed for HDMI-CEC", etc.).
2. Put the exact model name, Android version and firmware build in the header so people can tell
   whether it matches theirs. `tools/tv.py --probe` prints all three.
3. Only list packages you have actually disabled and lived with for a few days. Note anything that
   broke and what you re-enabled.
4. Add a row to `examples/README.md`.
5. If your TV's Live TV app or stock launcher has a different package name, say so in the header;
   `tools/restore.sh` and `launcher/build-and-install.sh` accept them as arguments.

Never list packages to *uninstall*. Everything in this repo disables with `pm disable-user`, which
is reversible from the TV's own settings if the tools are gone.

## 2. Improve the launcher

The launcher lives in `launcher/` and is deliberately small: plain Java, AndroidX RecyclerView and
DynamicAnimation, no Kotlin, no Compose, no network. It targets TVs with 1 GB of RAM, so:

- No blur, no video backgrounds, no polling, no background services.
- Anything that depends on a specific TV goes in `app/src/main/res/values/config.xml`, not in code.
- Keep D-pad navigation deterministic. Test Up/Down/Left/Right from every edge.
- Verify on a real TV, not only the emulator. The easiest way is remote key injection plus a
  screenshot: `tools/tv.py 'input keyevent KEYCODE_DPAD_DOWN; screencap -p /sdcard/s.png'` then
  `tools/tv.py --pull /sdcard/s.png s.png`. Include a screenshot in your pull request.
- Long presses (hold OK on Settings or on a tile) can't always be injected: `input keyevent --longpress`
  arrives as a short press on some firmware (Android TV 8.0 on a Philips opens the app instead). Writing
  to the remote's own input device works, because Android then repeats the held key itself. Find the
  device and the OK key with `tools/tv.py 'getevent -pl'` (a remote that lists `KEY_OK` is scancode 352),
  then hold it for a second:
  `tools/tv.py 'e=/dev/input/event1; sendevent $e 1 352 1; sendevent $e 0 0 0; sleep 1.2; sendevent $e 1 352 0; sendevent $e 0 0 0'`.
  This needs the shell user in the `input` group (`tools/tv.py id`).

Build with `TV_HOST=<ip> launcher/build-and-install.sh` (JDK 17, Android SDK platform 34 and
build-tools 34.0.0; on macOS `brew install openjdk@17 android-commandlinetools` then
`sdkmanager "platforms;android-34" "build-tools;34.0.0"`; with the newer command-line tools the form is
`android sdk install platforms/android-34 build-tools/34.0.0`).

Ideas that fit the direction: an idle screensaver with clock and ambient colour, per-TV icon packs
for apps without banners, a settings screen to edit the dock from the TV, localisation.
Ideas that don't: recommendation rows, content feeds, ads, telemetry.

## 3. Improve the prompt and tools

`PROMPT.md` is what most people will actually use. If your assistant got stuck somewhere, the fix
usually belongs in the prompt as one extra sentence, not in a script. Keep it brand-neutral; put
brand-specific knowledge in `examples/`.

`tools/tv.py` and `tools/b64push.py` exist because the stock `adb` is blocked on recent macOS. If
you find a cleaner workaround, open an issue first.

## 4. Show what you built

Built your own launcher, a port to another platform, or an add-on that is too big or too personal
for the shared launcher? Put it in its own repository and add one row to
[`SHOWCASE.md`](SHOWCASE.md). That page is the place for alternative launchers, forks with a
different look, and tools around the launcher; this repo keeps one small reference launcher and
points at everything else. The rules for a row are on that page.

Things that stay in this repo: debloat lists, fixes and small, opt-in features for the reference
launcher, prompt and tool improvements. Things that go in the showcase: a second launcher, a
restyle, a platform port, anything with its own background services or network access.

## Pull requests

- One topic per PR. Small is fine.
- Say which TV you tested on.
- No new dependencies in the launcher without discussing it in an issue.
- Be kind in reviews. Most contributors here are doing this for their parents.

By contributing you agree your work is released under the repo's MIT licence.
