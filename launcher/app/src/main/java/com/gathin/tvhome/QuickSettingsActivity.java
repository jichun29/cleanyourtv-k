package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 控制中心（K 2026-10-22 重做）：遥控器「菜单键」打开，iOS 状态栏式。
 *
 * 六枚圆角磁贴：信号源 / 网络 / 桌面设置 / 壁纸 / 应用库 / 重排图标；
 * 下方一条音量滑块（左右键直接调）。
 *
 * 设计取舍（记住「简洁桌面」）：
 * - 亮度滑块已按 K 拍板删掉——电视亮度交给系统画质设置，这里写系统亮度容易和厂商模式打架。
 * - 不做常驻线程、不联网；半透明独立任务，返回键或再按菜单键关闭。
 * - 焦点态白底黑字，和主界面磁贴的描边一脉相承，远看能找到焦点。
 */
public final class QuickSettingsActivity extends Activity {

    /** 谁打开的：菜单键（K 定制）或遥控器 Settings 键（老路径，settings_key_panel）。 */
    static final String EXTRA_SOURCE = "cc_source";
    static final String SOURCE_MENU_KEY = "menu_key";
    static final String SOURCE_SETTINGS_KEY = "settings_key";

    private AudioManager audio;
    private ProgressBar volBar;
    private TextView volValue;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_quick_settings);
        findViewById(R.id.scrim).setOnClickListener(v -> finish());
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);

        TextView clock = findViewById(R.id.ccClock);
        if (clock != null) clock.setText(DateFormat.getTimeFormat(this).format(System.currentTimeMillis()));

        LinearLayout tiles1 = findViewById(R.id.tiles1);
        LinearLayout tiles2 = findViewById(R.id.tiles2);
        // 第一行：高频动作
        addTile(tiles1, R.drawable.ic_input, R.string.cc_tile_inputs, TileKind.INPUTS, null);
        addTile(tiles1, R.drawable.ic_wifi, R.string.cc_tile_network, TileKind.INTENT,
                new Intent(Settings.ACTION_WIFI_SETTINGS));
        addTile(tiles1, R.drawable.ic_home_settings, R.string.cc_tile_settings, TileKind.LAUNCHER_SETTINGS, null);
        // 第二行：低频维护
        addTile(tiles2, R.drawable.ic_picture, R.string.cc_tile_wallpaper, TileKind.WALLPAPER, null);
        addTile(tiles2, R.drawable.ic_apps, R.string.cc_tile_library, TileKind.LIBRARY, null);
        addTile(tiles2, R.drawable.ic_swap_vert, R.string.cc_tile_reorder, TileKind.REORDER, null);

        bindVolume();

        // 第一个磁贴先拿焦点，遥控器一进来就能动
        if (tiles1.getChildCount() > 0) tiles1.getChildAt(0).requestFocus();
    }

    // ---- 磁贴 ---------------------------------------------------------------------------------

    private enum TileKind { INPUTS, INTENT, LAUNCHER_SETTINGS, WALLPAPER, LIBRARY, REORDER }

    private void addTile(LinearLayout row, int icon, int label, TileKind kind, Intent target) {
        View tile = getLayoutInflater().inflate(R.layout.item_cc_tile, row, false);
        ImageView ic = tile.findViewById(R.id.tileIcon);
        TextView name = tile.findViewById(R.id.tileLabel);
        ic.setImageResource(icon);
        ic.setColorFilter(0xE6FFFFFF, PorterDuff.Mode.SRC_IN);
        name.setText(label);

        // 焦点态：图标方块变白底 + 图标转黑（跟 iOS 控制中心一致）。
        // 注意：标签在方块**外面**，背景仍是深的，所以文字必须保持白色——之前一起变黑就糊成一片了。
        tile.setOnFocusChangeListener((v, focused) -> {
            ic.setColorFilter(focused ? 0xFF111111 : 0xE6FFFFFF, PorterDuff.Mode.SRC_IN);
            name.setTextColor(focused ? 0xFFFFFFFF : 0xD9FFFFFF);
            name.setAlpha(focused ? 1f : 0.85f);
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

    // ---- 音量 ---------------------------------------------------------------------------------

    /** 音量槽：整行焦点，左右键调 STREAM_MUSIC（遥控器音量键走的就是这条流）。 */
    private void bindVolume() {
        View row = findViewById(R.id.volumeRow);
        volBar = row.findViewById(R.id.volBar);
        volValue = row.findViewById(R.id.volValue);
        row.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            int step = event.getRepeatCount() > 0 ? 2 : 1;
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) { nudgeVolume(step); return true; }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) { nudgeVolume(-step); return true; }
            return false;
        });
        row.setOnClickListener(v -> {
            if (audio != null) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0);
                renderVolume();
            }
        });
        renderVolume();
    }

    private void nudgeVolume(int steps) {
        if (audio == null) return;
        int dir = steps > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        for (int i = 0; i < Math.abs(steps); i++) {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI);
        }
        renderVolume();
    }

    private void renderVolume() {
        if (audio == null || volBar == null) return;
        int max = Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int pct = Math.round(cur * 100f / max);
        volBar.setProgress(pct);
        volValue.setText(pct + "%");
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
