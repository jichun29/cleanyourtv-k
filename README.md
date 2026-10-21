# CleanYourTV · K 定制版

> 基于 [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv)（原 tv-vibehack）的二次开发版 Android TV 桌面，融合了 [BeatsAD/quiet-tv](https://github.com/BeatsAD/quiet-tv)（MIT，感谢 WenBin AI LAB）的壁纸模块。
>
> 面向国内电视用户：小米国行电视实测可用（MiTV-ASTP0 / Android 9），理论上所有 Android TV 9+ 都能跑。

## 这个版本多了什么

在上游 v2.5.0 基础上（Round K 大更新 + 电影感顶栏，含 Launcher 设置页、Dock 编辑、App 库、重排模式、减少过度绘制等），额外加入了这些定制：

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

见 [Releases](../../releases) 页面，两个版本：

| 版本 | 说明 |
|---|---|
| `CleanYourTV-Launcher-universal.apk` | **通用版**：默认 Dock（Netflix/YouTube 等），适合所有人，装完自己在设置里改 |
| `CleanYourTV-Launcher-K.apk` | 作者自用版：Dock 和网格顺序是给国内电视观影场景定制的（TVBox/直播/市场类 App），仅供参考 |

> 大多数人下**通用版**就行。Dock 内容、隐藏 App、列数全都可以在 `config.xml` 或电视上的 Launcher 设置页里改。

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
