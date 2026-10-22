# CleanYourTV · K 定制版

> 基于 [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv)（原 tv-vibehack）的二次开发版 Android TV 桌面，融合了 [BeatsAD/quiet-tv](https://github.com/BeatsAD/quiet-tv)（MIT，感谢 WenBin AI LAB）的壁纸模块。
>
> 面向国内电视用户：小米国行电视实测可用（MiTV-ASTP0 / Android 9），理论上所有 Android TV 9+ 都能跑。

## 截图

| 桌面 | 时钟屏保 | 设置菜单 |
|:---:|:---:|:---:|
| ![桌面](docs/screenshots/home.png) | ![时钟屏保](docs/screenshots/clock-screensaver.png) | ![设置菜单](docs/screenshots/settings-menu.png) |
| 简洁桌面 + Dock + 中文界面 | 闲置 5 分钟自动进入 / 按 Back 键立即进入 | 三选一：简洁桌面设置 / 电视系统设置 / 关于桌面 |

## 这个版本多了什么

在上游 v2.5.0 基础上（Round K 大更新 + 电影感顶栏，含 Launcher 设置页、Dock 编辑、App 库、重排模式、减少过度绘制等），并整合了 quiet-tv（Big Sur 壁纸）、tvhome-zh（必应每日壁纸）、apple-tv-launcher（航拍视频壁纸）三个二开项目的功能，额外加入了这些定制：

### 🖼️ 内置壁纸模块（移植自 quiet-tv）

- 内置 3 张 Big Sur 风景壁纸（海岸 / 群山 / 山路）
- 三种模式：**固定一张** / **5 分钟轮播** / **默认纯黑+光晕**
- 焦点移动实时预览、确定键提交、返回键还原
- 显示壁纸时自动隐藏环境光晕，画面更干净
- 全部解码在后台线程、按屏幕尺寸降采样，1GB 内存的电视也不会崩

### 📱 遥控器菜单键玻璃弹窗

- 焦点停在某个 App 上，按遥控器**菜单键**：弹出玻璃拟态对话框（打开 / 置顶到最前 / 移动位置 / 管理·卸载 / 应用信息）
- 「置顶到最前」一键把 App 提到桌面第一位并记住；「移动位置」直接拿起它用方向键放到任意位置（顺序会保存，重启不丢）
- 焦点不在 App 上（顶部按钮、时钟、空白处），按菜单键：打开**壁纸设置**
- 长按已移除（避免误触），统一由菜单键触发

### 🇨🇳 小米国行电视适配

- Wi-Fi / 设置按钮：标准 Intent 失败时自动拉起小米自带设置（`com.xiaomi.mitv.settings`）
- 「管理/卸载」：标准应用详情页失败时自动跳到小米电视管家的应用管理页（可卸载/清数据/查权限）
- 国产 App 全量发现：同时查询 `LEANBACK_LAUNCHER` + 普通 `LAUNCHER`，橘汁、小白文件管理器这类没声明 TV intent 的国产 App 也能显示出来

### ⚙️ 上游自带的好东西（这个版本全都有）

- **Launcher 设置页**：电视上直接改列数、壁纸、动画、高级效果，不用重新编译
- **Dock 编辑**：长按进入编辑模式，遥控器直接换 Dock 位置
- **App 库**：所有已装 App 的总列表，可以把手机 App 加到桌面
- **重排模式**：OK 键拿起 App，方向键移动，想怎么排怎么排
- **减少过度绘制**：滚动更顺滑（Round K 新合入）
- **性能统计 / 问候语 / 波斯历 / VPN 徽标 / 节日主题** 等一堆小功能

## 下载

见 [Releases](../../releases) 页面，**两个版本**（按设备选，覆盖 Android 4.4 到最新电视）：

| 版本 | 支持系统 | 说明 |
|---|---|---|
| `CleanYourTV-Launcher-universal.apk` | **Android 5.0+**（minSdk 21） | **通用版**：功能完整，适合绝大多数电视/盒子（小米、TCL、海信、索尼、N1、Shield…） |
| `CleanYourTV-Launcher-lite44.apk` | **Android 4.4+**（minSdk 19） | **4.4 精简版**：给老盒子的裁剪构建，砍掉航拍视频壁纸 / 必应每日壁纸 / 时钟屏保 / HDMI 输入源菜单（4.4 无此框架），核心功能全保留 |

> 两个版本签名一致，可互相覆盖安装不丢设置。Android 5.0 及以上**一律用通用版**；4.4 精简版只给跑不动完整版的老盒子。
>
> 通用版默认 Dock：Netflix / Prime / YouTube / Hotstar（缺失自动跳过，装完在电视上用菜单键自己排）；小米电视默认隐藏设置/电视管家/无线投屏/小爱同学 4 个系统组件。**自带应用库**：桌面最后一行「应用库」按钮可把所有已装 App 一键添加/移除桌面，新装 7 天内的 App 带绿点提示。

## 📢 电视去广告库（174 机型）+ AI 一键部署指令词

仓库里自带 [`debloat/`](debloat/) 目录：**174 个电视/盒子/投影机型**的去广告清单（小米 26 台、TCL 43 台、海信 8 台、索尼 6 台、创维/长虹/海尔/康佳/一加/极米/Shield/Fire TV 等 90+ 台），每条注明这个包是干什么的、停用后失去什么。**只停用、不卸载，随时一键还原。**详见 [debloat/README.md](debloat/README.md) 和品牌索引 [debloat/brands.md](debloat/brands.md)。

### 给 AI 机器人的完整指令词（复制即用）

把整个仓库交给 AI，粘贴下面这段话，它就能帮你完成「装桌面 + 去广告」全流程：

```
请克隆 https://github.com/jichun29/cleanyourtv-k 仓库并阅读 README.md 与 debloat/README.md。
我的电视型号是【填型号，如「小米 Mi TV 4A」】，电视 IP 是【填 IP】。
请按顺序完成：
1. 根据我的电视系统版本选择安装包：Android 5.0 及以上装通用版
   （launcher 编译或 Releases 里的 CleanYourTV-Launcher-universal.apk），
   Android 4.4 老盒子装 CleanYourTV-Launcher-lite44.apk。
   用 adb install -r 安装，并用 cmd package set-home-activity 设为默认桌面。
2. 在 debloat/examples/ 里找到与我机型最匹配的去广告清单
   （没有精确匹配时选同品牌、同芯片平台、Android 版本最接近的），
   逐条向我解释每个包的作用和停用影响，经我确认后用
   debloat/tools/disable.sh 执行停用（只停用、绝不卸载）。
3. 停用后逐项验证（pm list packages -d），遇到 setupwraith 等与
   开机向导/Wi-Fi 相关的包必须先警告我再决定。
4. 最后提示我重启电视确认效果，并告诉我如何用 tools/restore.sh 还原。
```

## 编译

需要 JDK 17 + Android SDK 34：

```bash
cd launcher
echo "sdk.dir=$HOME/Android" > local.properties
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew assembleRelease
# 产物: app/build/outputs/apk/release/app-release.apk
```

## 安装

| Tool | What it does |
|---|---|
| [`tools/tv.py`](tools/tv.py) | Pure-Python ADB client: shell, batch, push, pull. Works where the stock `adb` binary is blocked. |
| [`tools/b64push.py`](tools/b64push.py) | Uploads large files such as APKs in base64 chunks and verifies the checksum. |
| [`tools/relay.py`](tools/relay.py) | Forwards a local port to the TV's ADB port, for assistants whose sandbox can't reach your network. Listens on this computer only. [Guide](tools/relay.md). |
| [`tools/disable.sh`](tools/disable.sh), [`restore.sh`](tools/restore.sh) | Apply or undo a debloat list, the undo from the state recorded before the change. |
| [`tools/recovery.py`](tools/recovery.py) | Writes the record `disable.sh` needs to undo itself, and prints the restore batch. |
| [`tools/check.py`](tools/check.py) | Compares the TV with the list that was applied, for what a firmware update undid; `--fix` puts it back. |
| [`tools/theme.sh`](tools/theme.sh) | Push a folder from [`themes/`](themes/) to the launcher and restart Home. |
| [`tools/race_calendar.py`](tools/race_calendar.py) | Writes this season's F1 calendar into a theme for race-weekend mode, from the free Jolpica API. |
| [`launcher/build-and-install.sh`](launcher/build-and-install.sh) | Builds a shrunk release (about 405 KB), installs it, switches Home, and rolls back if Home does not land on it. |

Debloat list paths passed to `disable.sh` and `restore.sh` are relative to the directory you run
from, so your own lists can live outside the repository. Absolute paths also work.

Every script has a PowerShell twin next to it. On Windows with Android Studio installed nothing
else is needed:

```powershell
python -m venv tools\venv; tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt
powershell -ExecutionPolicy Bypass -File launcher\build-and-install.ps1 -TvHost 192.168.1.50
powershell -ExecutionPolicy Bypass -File tools\disable.ps1 -TvHost 192.168.1.50 examples\xiaomi-mi-tv-4a\debloat.txt
```

The install scripts ask the TV which launcher owns Home and only disable one they recognise
(`tvlauncher` or `launcherx`); pass `STOCK_LAUNCHER=<package>` (`-StockLauncher`) to override, or
`none` to leave it alone.

`tools/disable.sh` (`disable.ps1`) applies the same rule before it disables anything: if the list
contains the app that owns Home and no other app on the TV answers HOME, it skips that entry, says
why, and disables the rest of the list. So a list applied before the launcher handover cannot leave
the TV with nothing to start. Run the same list again once `launcher/build-and-install.sh` has made
the new launcher Home and the entry is applied; `FORCE_HOME=1` (`$env:FORCE_HOME`) disables it anyway.

## The launcher

A small native Android TV app (`com.gathin.tvhome`, about 405 KB installed) in plain Java with
AndroidX RecyclerView and nothing heavier. Built for sets with 1 GB of RAM: no blur, no video, no
network, no background services. It needs JDK 17 and Android SDK platform 34 to build; on macOS,
`brew install openjdk@17 android-commandlinetools` and `sdkmanager "platforms;android-34" "build-tools;34.0.0"`. On Linux
the install script finds `~/Android/Sdk` and the `java` on your PATH.
The **TV** needs Android 5.0 (API 21) or newer (Android 4.4 via the `-PliteBuild=true` lite build): `minSdk` is a compile-time floor in
[`launcher/app/build.gradle`](launcher/app/build.gradle), and the install script asks the TV for
`ro.build.version.sdk` before it builds, so an older set is told both numbers instead of failing at
`pm install` (see [Troubleshooting](docs/TROUBLESHOOTING.md) for what is left on such a set).

- **Configure** in [`config.xml`](launcher/app/src/main/res/values/config.xml): dock order, hidden apps, the Live TV package, columns (tiles shrink from 5 up), rows under the dock, an optional VPN app for the network pill, and `reduced_motion` for TVs whose GPU cannot keep up.
- **App library:** on by default — a glass pill after the last Home row lists every installed app (missing-from-Home first, freshly installed apps lead with a green dot); OK adds/removes the Home shortcut, long-press gives Open / Add / Remove / App info. Disable with `app_library` in config.xml. See [the app library guide](docs/app-library.md).
- **On-TV launcher settings:** click Settings for the TV's system settings, or hold OK on Settings and choose Launcher settings to set 4, 5, 6 or 7 apps per row, turn animations on or off, and opt into advanced effects. The advanced option adds tile lift, icon zoom and smooth ambient-colour transitions; it may use more memory. Use Follow theme to clear on-TV choices. Pushing or resetting a theme also clears them.
- **Change the default home:** Launcher settings and the optional Default-home tile (`show_home_settings_shortcut`) open Android's home picker. Choose the original launcher to stop using CleanYourTV as Home, or CleanYourTV to return. The original launcher must be enabled. If the install script disabled it, use the restore script below first. Some TVs provide no home picker.
- **Themes** set the same keys plus a background colour and a wallpaper from a `config.json` pushed to the TV, no rebuild. Four ship in [`themes/`](themes/), documented there.
- **Language** follows the TV: English, German, Spanish, Catalan, French, Russian, Serbian (Latin), Polish, Czech, Italian, Portuguese, Turkish, Arabic, Hebrew, Simplified Chinese, Traditional Chinese (Taiwan), Thai, Finnish, Vietnamese, Bangla, Indonesian, Norwegian Bokmål. Android 13+ can set it for the launcher alone: `tools/tv.py 'cmd locale set-app-locales com.gathin.tvhome --locales ca'`.
- **Language** follows the TV: English, German, Spanish, Catalan, French, Russian, Serbian (Latin), Polish, Czech, Italian, Portuguese, Turkish, Arabic, Hebrew, Simplified Chinese, Traditional Chinese (Taiwan), Thai, Finnish, Vietnamese, Bangla, Indonesian, Dutch. Android 13+ can set it for the launcher alone: `tools/tv.py 'cmd locale set-app-locales com.gathin.tvhome --locales ca'`.
- **Language** follows the TV: English, German, Spanish, Catalan, French, Russian, Serbian (Latin), Polish, Czech, Italian, Portuguese, Turkish, Arabic, Hebrew, Simplified Chinese, Traditional Chinese (Taiwan), Thai, Finnish, Vietnamese, Bangla, Indonesian, Uzbek (Latin). Android 13+ can set it for the launcher alone: `tools/tv.py 'cmd locale set-app-locales com.gathin.tvhome --locales ca'`.
- **Themes** set the same keys plus a background colour and a wallpaper from a `config.json` pushed to the TV, no rebuild. Five ship in [`themes/`](themes/), documented there.
- **Language** follows the TV: English, German, Spanish, Catalan, French, Russian, Serbian (Latin), Polish, Czech, Italian, Portuguese, Turkish, Arabic, Hebrew, Simplified Chinese, Traditional Chinese (Taiwan), Thai, Finnish, Vietnamese, Bangla, Indonesian. Android 13+ can set it for the launcher alone: `tools/tv.py 'cmd locale set-app-locales com.gathin.tvhome --locales ca'`.
  Hebrew places the menu buttons on the left and the date and clock on the right, with the greeting above the clock. The Clock screensaver right-aligns its date and time, and Hebrew dates include the month prefix ב (for example, 19 באוקטובר). Hebrew dock and app-grid rows start on the right, with matching left/right remote navigation. The Hebrew input selector places icons and labels on the right, status on the left, and aligns the dropdown with the Inputs button’s right edge. Other languages keep their original layouts.
  Russian includes the module-card approval and management screens and the VPN-off badge.
- **Module cards** let any TV app declare its own tile (label, caption, glow). The launcher asks you once per app, Keep hidden by default, and remembers the answer for that app's signing key and card name. Hold OK on Settings and choose Module cards to change your mind. How to make one: [`docs/modules.md`](docs/modules.md); the contract: [`docs/specs/card.md`](docs/specs/card.md).
- **Clock screensaver**: a drifting clock over the glow, so nothing burns in. Pick "Clock" in the TV's screensaver settings, or over ADB:
  `tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream; settings put secure screensaver_enabled 1'`
- **Remote keys**: an optional accessibility service keeps the remote's Input button working on Google TV (`INPUT_KEY_SERVICE=1` at install) and can bring back other buttons only the stock launcher answered, such as the YouTube button (`app_keys` and `input_key_keycodes`, in `config.xml` or a theme; `HOME_KEY_SERVICE=1` reads `app_keys` too), another fixes sticks whose Home key sends ALL_APPS and opens Settings from the remote's gear button, even one Android does not map (`HOME_KEY_SERVICE=1`, scancodes or keycodes in `config.xml`). With `settings_key_panel` the gear opens a [small quick settings panel](docs/quick-settings-panel.jpg) instead, over Home or any app: TV settings, Display, Network, Apps and Launcher settings.
- **Navigation** is deliberate: Up and Down move one row, the top buttons never fall through to tiles and Down from them returns to the dock tile you came from, the last focused app is remembered, and installing or removing an app refreshes the tiles while Home is open.
- **Artwork**: app banners load at xhdpi or better, so low-density TV UIs do not downsample them before the focus zoom.

## Restoring your TV

Before `tools/disable.sh` changes anything it writes down what the TV looked like: for every package
in the list whether it was enabled, already disabled or not installed, which component owned Home,
and the three animation scales. That record lives in `~/.cleanyourtv/`, one file per TV and list, and
a second run of the same list keeps the first record instead of saving the cleaned-up state over it.
If you added packages to the list since, that run adds them to the record before disabling them.

Keep the list you applied. The restore script replays the record, so a package you had disabled
yourself stays disabled and Home goes back to the component that owned it:

```bash
adb connect <电视IP>:5555
adb push app/build/outputs/apk/release/app-release.apk /data/local/tmp/tvhome.apk
adb shell pm install -r /data/local/tmp/tvhome.apk
adb shell rm -f /data/local/tmp/tvhome.apk
adb shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity
```

可选增强（Home 键 / 信号源键在任何 App 里生效）：

```bash
adb shell settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService:com.gathin.tvhome/.InputKeyService
adb shell settings put secure accessibility_enabled 1
```

## 定制你自己的 Dock 和网格

编辑 `launcher/app/src/main/res/values/config.xml`：

```xml
<string-array name="dock_apps">
    <item>com.netflix.ninja</item>        <!-- 前 4 个 = Dock -->
    <item>com.google.android.youtube.tv</item>
    <!-- 后面的按顺序排网格分类 -->
</string-array>

<string-array name="hidden_apps">
    <item>com.xiaomi.mitv.settings</item> <!-- 不想显示的系统 App -->
</string-array>
```

改完重新编译即可；也可以在电视上用 Launcher 设置页在线改（不用编译）。

## 致谢

- [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) —— 上游桌面本体
- [BeatsAD/quiet-tv](https://github.com/BeatsAD/quiet-tv) —— 壁纸模块来源（MIT License）

## License

与上游一致。壁纸模块部分遵循 quiet-tv 的 MIT License。
