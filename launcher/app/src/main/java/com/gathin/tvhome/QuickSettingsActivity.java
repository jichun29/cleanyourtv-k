package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 控制中心（K 2026-10-22 重做）：遥控器「菜单键」打开，iOS 状态栏式。
 *
 * 六枚圆角磁贴：信号源 / 网络 / 桌面设置 / 壁纸 / 应用库 / 重排图标，点开即用。
 *
 * 设计取舍（记住「简洁桌面」，2026-10-22 K 拍板）：
 * - **不放音量和亮度**：遥控器本身就有音量键，多一层纯浪费；亮度交给电视自己的画质设置，
 *   桌面写系统亮度会跟厂商模式打架，还要 WRITE_SETTINGS 特殊权限。控制中心只放「遥控器上没有的」。
 * - **防烧屏**：面板是临时悬浮，停留时间以秒计（按返回/再按菜单键即关），不会长时间压在同一位置；
 *   常驻的时钟屏保由 ClockScene 每分钟漂移+换色，两者都不给固定亮点留下烙印机会。
 * - 不做常驻线程、不联网；半透明独立任务，返回键或再按菜单键关闭。
 * - 焦点态白底黑字，和主界面磁贴的描边一脉相承，远看能找到焦点。
 */
public final class QuickSettingsActivity extends Activity {

    /** 谁打开的：菜单键（K 定制）或遥控器 Settings 键（老路径，settings_key_panel）。 */
    static final String EXTRA_SOURCE = "cc_source";
    static final String SOURCE_MENU_KEY = "menu_key";
    static final String SOURCE_SETTINGS_KEY = "settings_key";

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_quick_settings);
        findViewById(R.id.scrim).setOnClickListener(v -> finish());

        LinearLayout tiles1 = findViewById(R.id.tiles1);
        LinearLayout tiles2 = findViewById(R.id.tiles2);
        // 第一行：高频动作
        addTile(tiles1, R.drawable.ic_input, R.string.cc_tile_inputs, R.drawable.cc_tile_glass, TileKind.INPUTS, null);
        addTile(tiles1, R.drawable.ic_wifi, R.string.cc_tile_network, R.drawable.cc_tile_glass, TileKind.INTENT,
                new Intent(Settings.ACTION_WIFI_SETTINGS));
        addTile(tiles1, R.drawable.ic_home_settings, R.string.cc_tile_settings, R.drawable.cc_tile_glass, TileKind.LAUNCHER_SETTINGS, null);
        // 第二行：低频维护
        addTile(tiles2, R.drawable.ic_picture, R.string.cc_tile_wallpaper, R.drawable.cc_tile_glass, TileKind.WALLPAPER, null);
        addTile(tiles2, R.drawable.ic_apps, R.string.cc_tile_library, R.drawable.cc_tile_glass, TileKind.LIBRARY, null);
        addTile(tiles2, R.drawable.ic_swap_vert, R.string.cc_tile_reorder, R.drawable.cc_tile_glass, TileKind.REORDER, null);


        // 第一个磁贴先拿焦点，遥控器一进来就能动
        if (tiles1.getChildCount() > 0) tiles1.getChildAt(0).requestFocus();
    }

    // ---- 磁贴 ---------------------------------------------------------------------------------

    private enum TileKind { INPUTS, INTENT, LAUNCHER_SETTINGS, WALLPAPER, LIBRARY, REORDER }

    private void addTile(LinearLayout row, int icon, int label, int glass, TileKind kind, Intent target) {
        View tile = getLayoutInflater().inflate(R.layout.item_cc_tile, row, false);
        // 统一中性玻璃底（K 2026-10-22：不要五颜六色，简洁风）
        tile.findViewById(R.id.tileIconBg).setBackgroundResource(glass);
        ImageView ic = tile.findViewById(R.id.tileIcon);
        TextView name = tile.findViewById(R.id.tileLabel);
        ic.setImageResource(icon);
        ic.setColorFilter(0xFFE8E8EA, PorterDuff.Mode.SRC_IN);
        name.setText(label);
        name.setTextColor(0xFFD5D5D8);
        // 初始状态必须是「未选中」的暗度，否则首帧六枚一样亮（焦点层级立不起来）
        tile.setAlpha(0.66f);
        name.setAlpha(0.75f);

        // 焦点态（K 2026-10-22 v5）：统一中性玻璃，不玩彩色。
        // 选中时磁贴变成浅色玻璃底，所以图标要转深色才看得见；未选中是白图标压在深玻璃上。
        // 再加 1.06 倍放大和其余压暗，焦点一眼可辨。
        tile.setOnFocusChangeListener((v, focused) -> {
            ic.setColorFilter(focused ? 0xFF1A1A1C : 0xFFE8E8EA, PorterDuff.Mode.SRC_IN);
            name.setTextColor(focused ? 0xFFFFFFFF : 0xFFD5D5D8);
            name.setAlpha(focused ? 1f : 0.75f);
            v.animate().scaleX(focused ? 1.06f : 1f).scaleY(focused ? 1.06f : 1f)
                    .setDuration(130).start();
            v.setAlpha(focused ? 1f : 0.66f);
        });

        tile.setOnClickListener(v -> {
            switch (kind) {
                case INPUTS:
                    finish();
                    startActivity(new Intent(this, MainActivity.class)
                            .setAction(MainActivity.ACTION_SHOW_INPUTS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    break;
                case LAUNCHER_SETTINGS:
                    finish();
                    startActivity(new Intent(this, MainActivity.class)
                            .setAction(MainActivity.ACTION_LAUNCHER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    break;
                case WALLPAPER:
                    finish();
                    startActivity(new Intent(this, MainActivity.class)
                            .setAction(MainActivity.ACTION_SHOW_WALLPAPER)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    break;
                case LIBRARY:
                    finish();
                    startActivity(new Intent(this, MainActivity.class)
                            .setAction(MainActivity.ACTION_SHOW_LIBRARY)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    break;
                case REORDER:
                    finish();
                    startActivity(new Intent(this, MainActivity.class)
                            .setAction(MainActivity.ACTION_START_REORDER)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    break;
                default:
                    try {
                        finish();
                        startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        });
        row.addView(tile);
    }

    // ---- 生命周期 -----------------------------------------------------------------------------

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 返回键 = 关闭控制中心（K 2026-10-22）。必须显式拦，不能靠 onPause 里的 finish：
        // 那样在「按返回」的瞬间 Activity 还在前台，会闪一下。
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }

    /**
     * 被别的界面完全盖住才关（Home、某个 App、屏保）。用 onStop 而不是 onPause：
     * onPause 在「自己正在关」和「弹出系统对话框」时也会触发，容易把自己关成半死状态（K 2026-10-22 踩过）。
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing()) finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        main.removeCallbacksAndMessages(null);
    }
}
