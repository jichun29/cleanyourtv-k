# App library

Set `"app_library": true` in a theme’s `config.json`, or enable the `app_library`
bool in `config.xml`. It is off by default. A small glass pill appears after the
last row of Home apps.

The horizontal library lists installed apps with a TV or ordinary Android launch
activity, preferring the TV entry when both exist. It excludes services and apps
without a launch activity. Ordinary Android apps may still need touch or a mouse.

Apps missing from Home come first. Apps first installed within the last seven
days lead that group, newest first, with a green dot; older missing apps are
alphabetical. Apps already on Home follow alphabetically, with a checkmark and a
subtle tint. Updating an existing app does not mark it as new.

- Press OK on a missing app to add it to Home. Focus advances to the next missing
  app without following the new shortcut across the catalogue.
- Press OK on a checked app to open it.
- Hold OK for Open, Add to Home or Remove from Home, and App info. The menu uses
  the existing Inputs dropdown resources and stays over the selected card.
- Back closes the menu first, then the library. Down from the last Home row or
  Right from the final app reaches the library pill; Up returns to the last row.
  Hebrew mirrors the horizontal direction, so Left from the final app reaches it.

Every checked app can be removed from Home, including the theme’s default apps.
It stays installed and remains in the library, where one press adds it back.
There is no hidden-app section and no uninstall action.

Choices are stored in the launcher’s private `library_apps.txt` and
`library_home_removed.txt`, with at most 128 entries in each. Each file is written
atomically. An unreadable file is left unchanged if an edit fails. Choices
survive restarts and app updates; missing or disabled packages are omitted until
they return. Explicit Home removals override theme defaults until re-added.
Theme config, dock ordering, module-card decisions and other saved tile order
are not edited. Disabling the library button keeps existing Home choices.

The library uses the existing package receiver to refresh after app changes.
Labels and Home membership are cached for each refresh; icons load only for
bound cards. The highlight follows the launcher's animation setting, including
instant movement when animations are off. There is no polling, network, backdrop capture, new service,
permission or dependency. The additional manifest query covers ordinary
LAUNCHER activities on Android 11 and later.
