package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/**
 * 桌面设置页 v2（K 2026-10-22 重做）。
 *
 * 布局结构 = 分组 + 左标签右控件：
 *   桌面布局：每行应用数量(分段)、应用栏下方保留行数(分段)、壁纸(入口)
 *   动效：动态效果(开关)、减弱动画(开关)、高级动态效果(开关)、恢复主题默认(入口)
 *   软件更新：更新方式(分段 手动/自动)、检查更新(入口)
 *   系统：设置为默认桌面(入口)、系统设置(入口)
 *
 * 控件语义分离（v1 全是同一种深灰大横条，看不出谁是什么）：
 *   - 分段：SettingsSegment 样式，选中绿填充、焦点白框，数值类用
 *   - 开关：左侧设置名 + 右侧 开/关，灰一档显示当前值
 *   - 入口：左侧设置名 + 右侧 › 箭头，明确可进入下一级
 *
 * 焦点态和选中态完全分开（v1 用同一个绿色高亮，遥控器指到哪儿全靠猜）：
 *   - 焦点 = 白框 + 提亮（settings_row_bg/settings_segment_bg 的 focused 态）
 *   - 选中 = 绿色描边/填充（selected 态）
 *
 * RTL：columns/rows 的分段在 RTL 下要反向排列（reverseRow），其余行靠 LinearLayout 自动镜像。
 */
public final class LauncherSettingsActivity extends Activity {
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private View wallpaperChoice;
    private TextView wallpaperValue;
    private View animationsChoice;
    private TextView animationsValue;
    private View motionChoice;
    private TextView motionValue;
    private View advancedChoice;
    private TextView advancedValue;
    private TextView advancedOffHint;
    private View followThemeChoice;
    private View idleClockChoice;
    private TextView idleClockValue;
    private TextView updateManualChoice;
    private TextView updateAutoChoice;
    private int columns;
    private int rowsBelowDock;
    private boolean animations;
    private boolean advancedEffects;
    private boolean reducedMotion;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;
        reducedMotion = config.reducedMotion;

        // ===== 分段：每行应用数量 =====
        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                setResult(RESULT_OK);
                refresh();
            });
        }
        // ===== 分段：应用栏下方保留行数 =====
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                setResult(RESULT_OK);
                refresh();
            });
        }
        // RTL 下保持数值递增的阅读顺序
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
        }

        // ===== 入口：壁纸 =====
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperValue = findViewById(R.id.wallpaperValue);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());

        // ===== 开关：动态效果 =====
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsValue = findViewById(R.id.animationsValue);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            setResult(RESULT_OK);
            refresh();
        });

        // ===== 开关：减弱动画 =====
        motionChoice = findViewById(R.id.motionChoice);
        motionValue = findViewById(R.id.motionValue);
        motionChoice.setOnClickListener(v -> {
            reducedMotion = !reducedMotion;
            choices.edit().putBoolean("reduced_motion", reducedMotion).apply();
            setResult(RESULT_OK);
            refresh();
        });

        // ===== 开关：高级动态效果 =====
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedValue = findViewById(R.id.advancedValue);
        advancedOffHint = findViewById(R.id.advancedOffHint);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

        // ===== 开关：时间屏保（关了待机再久也不自动进，默认开） =====
        idleClockChoice = findViewById(R.id.idleClockChoice);
        idleClockValue = findViewById(R.id.idleClockValue);
        idleClockChoice.setOnClickListener(v -> {
            boolean now = choices.getBoolean("idle_clock_enabled", true);
            choices.edit().putBoolean("idle_clock_enabled", !now).apply();
            setResult(RESULT_OK);
            refresh();
        });

        // ===== 屏保（2026-10-23 K）：时间 / 壁纸 两种样式 + 换图间隔 + 预览 =====
        findViewById(R.id.styleClock).setOnClickListener(v -> {
            choices.edit().putString("screensaver_style", "clock").apply();
            setResult(RESULT_OK);
            refresh();
        });
        findViewById(R.id.styleBing).setOnClickListener(v -> {
            if (BuildConfig.LITE) return;
            choices.edit().putString("screensaver_style", "bing").apply();
            setResult(RESULT_OK);
            Toast.makeText(this, "壁纸屏保已开，电视空闲时会显示必应每日图", Toast.LENGTH_SHORT).show();
            // 后台先把图池备好，第一次进屏保就有图（不阻塞界面）
            new Thread(() -> BingScreensaver.ensurePool(this)).start();
            refresh();
        });
        int[] wpMins = {0, 60, 1440};
        int[] wpIds = {R.id.wpEveryTime, R.id.wp1h, R.id.wp1d};
        for (int i = 0; i < 3; i++) {
            final int m = wpMins[i];
            findViewById(wpIds[i]).setOnClickListener(v -> {
                choices.edit().putInt("screensaver_interval_min", m).apply();
                refresh();
            });
        }
        findViewById(R.id.wpNextChoice).setOnClickListener(v -> {
            // 手动换一张：把上次换图时间清零，下次进屏保就会进一位
            choices.edit().putLong("screensaver_last_change", 0L).apply();
            Toast.makeText(this, "下次进屏保换新一张", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.previewScreensaverChoice).setOnClickListener(v ->
                startActivity(new Intent(this, ClockActivity.class)));

        // ===== 入口：恢复主题默认 =====
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove("reduced_motion")
                    .remove(Config.WALLPAPER_CHOICE_KEY).commit();
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            reducedMotion = current.reducedMotion;
            setResult(RESULT_OK);
            refresh();
        });

        // ===== 软件更新：分段 + 入口 =====
        updateManualChoice = findViewById(R.id.updateManual);
        updateAutoChoice = findViewById(R.id.updateAuto);
        updateManualChoice.setOnClickListener(v -> setUpdateAuto(false));
        updateAutoChoice.setOnClickListener(v -> setUpdateAuto(true));
        findViewById(R.id.checkUpdateChoice).setOnClickListener(v ->
                startActivity(new Intent(this, UpdateActivity.class)));

        // ===== 存储与清理（2026-10-23 K）：快传 + 自动清理 + 白名单 =====
        findViewById(R.id.quickTransferChoice).setOnClickListener(v ->
                startActivity(new Intent(this, QuickTransferActivity.class)));

        findViewById(R.id.autoCleanChoice).setOnClickListener(v -> {
            boolean now = choices.getBoolean("auto_clean_enabled", false);
            choices.edit().putBoolean("auto_clean_enabled", !now).apply();
            MemoryCleaner.reschedule(this);
            refresh();
        });

        // 间隔分段：30 / 60 / 180 分钟
        int[] mins = {30, 60, 180};
        int[] segIds = {R.id.interval30, R.id.interval60, R.id.interval180};
        for (int i = 0; i < 3; i++) {
            final int m = mins[i];
            findViewById(segIds[i]).setOnClickListener(v -> {
                choices.edit().putInt("auto_clean_interval_min", m).apply();
                MemoryCleaner.reschedule(this);
                refresh();
            });
        }

        findViewById(R.id.cleanWhitelistChoice).setOnClickListener(v ->
                startActivity(new Intent(this, WhitelistActivity.class)));

        findViewById(R.id.cleanNowChoice).setOnClickListener(v -> {
            int n = MemoryCleaner.cleanNow(this);
            Toast.makeText(this, n > 0 ? "已清理（" + n + " 个后台进程）" : "后台本来就很干净",
                    Toast.LENGTH_SHORT).show();
        });

        // ===== 系统：默认桌面、系统设置 =====
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.settings_home_hint, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(new Intent(Settings.ACTION_SETTINGS)));

        refresh();
        columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        android.widget.LinearLayout row = (android.widget.LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which).getName()).apply();
                    }
                    setResult(RESULT_OK);
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    setResult(RESULT_OK);
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        setResult(RESULT_OK);
        refresh();
    }

    /** 软件更新方式：手动（默认）/ 自动。 */
    private void setUpdateAuto(boolean auto) {
        if (UpdateChecker.isAutoEnabled(this) == auto) return;
        UpdateChecker.setAutoEnabled(this, auto);
        refresh();
    }

    private void refresh() {
        // 分段：数值
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        // 开关：只更新右侧的「开/关」值，行标签留在 XML 里
        animationsValue.setText(animations ? R.string.on : R.string.off);
        animationsValue.setTextColor(animations ? 0xFF4ADE80 : 0x66FFFFFF);
        motionValue.setText(reducedMotion ? R.string.on : R.string.off);
        motionValue.setTextColor(reducedMotion ? 0xFF4ADE80 : 0x66FFFFFF);
        advancedValue.setText(advancedEffects ? R.string.on : R.string.off);
        advancedValue.setTextColor(advancedEffects ? 0xFF4ADE80 : 0x66FFFFFF);
        // 时间屏保
        boolean idleOn = choices.getBoolean("idle_clock_enabled", true);
        idleClockValue.setText(idleOn ? R.string.on : R.string.off);
        idleClockValue.setTextColor(idleOn ? 0xFF4ADE80 : 0x66FFFFFF);
        // 屏保样式（2026-10-23 K）。lite 版（Android 4.4）没有壁纸模块，只给时间屏保。
        boolean bingStyle = "bing".equals(choices.getString("screensaver_style", "clock"))
                && !BuildConfig.LITE;
        findViewById(R.id.styleClock).setSelected(!bingStyle);
        TextView styleBing = findViewById(R.id.styleBing);
        styleBing.setSelected(bingStyle);
        styleBing.setVisibility(BuildConfig.LITE ? View.GONE : View.VISIBLE);
        int wpVis = (bingStyle && idleOn) ? View.VISIBLE : View.GONE;
        findViewById(R.id.wallpaperIntervalRow).setVisibility(wpVis);
        findViewById(R.id.wpNextChoice).setVisibility(wpVis);
        int wpCur = choices.getInt("screensaver_interval_min", 60);
        int[] wpMins = {0, 60, 1440};
        int[] wpIds = {R.id.wpEveryTime, R.id.wp1h, R.id.wp1d};
        for (int i = 0; i < 3; i++) {
            findViewById(wpIds[i]).setSelected(wpCur == wpMins[i]);
        }
        // 存储与清理：自动清理状态 + 相关行只在开启时露出来
        boolean cleanOn = choices.getBoolean("auto_clean_enabled", false);
        TextView autoCleanValue = findViewById(R.id.autoCleanValue);
        autoCleanValue.setText(cleanOn ? R.string.on : R.string.off);
        autoCleanValue.setTextColor(cleanOn ? 0xFF4ADE80 : 0x66FFFFFF);
        int vis = cleanOn ? View.VISIBLE : View.GONE;
        findViewById(R.id.autoCleanIntervalRow).setVisibility(vis);
        findViewById(R.id.cleanWhitelistChoice).setVisibility(vis);
        findViewById(R.id.cleanNowChoice).setVisibility(vis);
        // 间隔分段高亮（分段背景是 selector：selected 态绿色填充，和更新方式分段一致）
        int cur = choices.getInt("auto_clean_interval_min", 60);
        int[] segIds = {R.id.interval30, R.id.interval60, R.id.interval180};
        int[] mins = {30, 60, 180};
        for (int i = 0; i < 3; i++) {
            TextView seg = findViewById(segIds[i]);
            seg.setSelected(cur == mins[i]);
        }
        // 高级效果依赖动态效果：关掉动态效果时灰掉
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.45f);
        advancedOffHint.setVisibility(animations ? View.GONE : View.VISIBLE);
        // 入口：壁纸的当前值
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperValue.setText("none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                : wallpaperCurrent != null ? wallpaperCurrent
                : getString(R.string.wallpaper_theme_default));
        // 恢复主题默认：没有任何自定义时灰掉
        followThemeChoice.setAlpha(hasOnTvChoices() ? 1f : 0.45f);
        // 软件更新分段
        boolean autoUpdate = UpdateChecker.isAutoEnabled(this);
        updateManualChoice.setSelected(!autoUpdate);
        updateAutoChoice.setSelected(autoUpdate);
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains("reduced_motion")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY);
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}