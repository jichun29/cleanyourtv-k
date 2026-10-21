package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bundled offline wallpapers, ported from BeatsAD/quiet-tv (MIT, thanks WenBin AI LAB).
 *
 * Three landscape pictures ship in assets/wallpapers/. The user picks a fixed picture, a
 * five-minute slideshow, or the default backdrop (the launcher keeps its own ambient glow /
 * sky, so the "default" choice here just removes the picture layer). The picture layer sits
 * directly above the sky view and under the ambient glow: it is two ImageViews (current +
 * incoming for the cross-fade) plus a readability shade, inserted into the root FrameLayout
 * at index 1 by MainActivity.
 *
 * The picker previews on focus without committing; OK commits, Back restores the committed
 * state. All decoding happens on a single background executor, downsampled to the screen,
 * so a 1 GB TV never holds more than roughly one screen-sized bitmap per picture.
 */
final class WallpaperController {
    private static final String[] FILES = {"big-sur-coastline", "big-sur-mountains", "big-sur-road"};
    private static final String[] ZH = {"大苏尔海岸", "大苏尔群山", "大苏尔山路"};
    private static final long ROTATION_MS = 5 * 60 * 1000L;

    /** Caller reacts to picture vs. default backdrop (hide ambient glow & sky over a photo). */
    interface VisibilityListener { void onPictureVisibilityChanged(boolean pictureVisible); }

    private final Activity activity;
    private final FrameLayout background;   // layer inserted into root by MainActivity
    private final SharedPreferences preferences;
    private final VisibilityListener visibilityListener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final ImageView current, incoming;
    private final View shade;
    private final Bitmap[] thumbnails = new Bitmap[FILES.length];
    private volatile int request;
    private int index, preview, shown = -2;
    private boolean rotating, active, useDefault;
    /** 必应每日壁纸模式（tvhome-zh 移植）：开启后图片层显示电视本地 bing-wallpaper.jpg。 */
    private boolean bingMode;
    private BingWallpaper bing;
    private Dialog picker;
    private ValueAnimator transition;

    private final Runnable rotate = () -> {
        index = (index + 1) % FILES.length;
        save();
        display(index, true);
        schedule();
    };

    /** {@code layerIndex} = position inside root where the background FrameLayout goes. */
    WallpaperController(Activity activity, FrameLayout root, int layerIndex, VisibilityListener listener) {
        this.activity = activity;
        this.visibilityListener = listener;
        preferences = activity.getSharedPreferences("wallpapers", 0);
        index = preferences.getInt("index", 0);
        rotating = preferences.getBoolean("rotating", true);
        useDefault = preferences.getBoolean("default", !preferences.contains("index"));
        if (useDefault) rotating = false;
        bing = new BingWallpaper(activity);
        bingMode = preferences.getBoolean("bing", false) && bing.enabled() && bing.file() != null;

        background = new FrameLayout(activity);
        root.addView(background, layerIndex,
                new FrameLayout.LayoutParams(-1, -1));
        current = image(); incoming = image(); incoming.setAlpha(0);
        background.addView(current, new FrameLayout.LayoutParams(-1, -1));
        background.addView(incoming, new FrameLayout.LayoutParams(-1, -1));
        // Readability shade: keeps clock/dock text legible over bright scenery.
        shade = new View(activity);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x70000000, 0x30000000, 0x99000000}));
        shade.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        background.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        display(useDefault ? -1 : (bingMode ? -2 : index), false);
        refreshBing();
    }

    /** 启动时后台拉一次必应今日图（开了开关且今天没拉过才真的下载），拉到就切换显示。 */
    private void refreshBing() {
        if (!bing.enabled()) return;
        decoder.execute(() -> {
            boolean changed = bing.refreshIfDue();
            if (changed) main.post(() -> {
                if (activity.isDestroyed()) return;
                if (preferences.getBoolean("bing", false)) {
                    bingMode = true;
                    useDefault = false;
                    rotating = false;
                    display(-2, true);
                }
            });
        });
    }

    private ImageView image() {
        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return image;
    }

    private Bitmap decode(int n, int sample) {
        try (InputStream input = activity.getAssets().open("wallpapers/" + FILES[n] + ".jpg")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            return BitmapFactory.decodeStream(input, null, options);
        } catch (IOException e) {
            throw new IllegalStateException("Missing bundled wallpaper", e);
        }
    }

    private synchronized Bitmap thumbnail(int n) {
        if (thumbnails[n] == null) thumbnails[n] = decode(n, 4);
        return thumbnails[n];
    }

    private Bitmap fullImage(int n) {
        android.util.DisplayMetrics screen = activity.getResources().getDisplayMetrics();
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = activity.getAssets().open("wallpapers/" + FILES[n] + ".jpg")) {
            BitmapFactory.decodeStream(input, null, bounds);
        } catch (IOException e) {
            throw new IllegalStateException("Missing bundled wallpaper", e);
        }
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= screen.widthPixels
                && bounds.outHeight / (sample * 2) >= screen.heightPixels) sample *= 2;
        return decode(n, sample);
    }

    /** n &lt; 0 shows the default backdrop (no picture layer visible); n == -2 shows the Bing daily picture. */
    private void display(int n, boolean animate) {
        final int version = ++request;
        if (transition != null) { transition.end(); transition = null; }
        if (n < 0 && n != -2) {
            current.setImageDrawable(null);
            incoming.setImageDrawable(null);
            background.setVisibility(View.INVISIBLE);
            if (visibilityListener != null) visibilityListener.onPictureVisibilityChanged(false);
            shown = -1;
            Log.i("CleanYourTV", "Wallpaper display=default");
            return;
        }
        if (shown == n && background.getVisibility() == View.VISIBLE) return;
        decoder.execute(() -> {
            if (version != request) return;
            Bitmap bitmap = n == -2 ? bingImage() : fullImage(n);
            if (bitmap == null) return;   // 必应图解码失败保持现状
            main.post(() -> {
                if (version != request || activity.isDestroyed()) return;
                background.setVisibility(View.VISIBLE);
                if (visibilityListener != null) visibilityListener.onPictureVisibilityChanged(true);
                if (!animate || shown < 0) {
                    current.setImageBitmap(bitmap);
                    shown = n;
                } else {
                    incoming.setImageBitmap(bitmap);
                    incoming.setAlpha(0);
                    transition = ValueAnimator.ofFloat(0, 1);
                    transition.setDuration(900);
                    transition.addUpdateListener(a -> {
                        float fraction = (float) a.getAnimatedValue();
                        incoming.setAlpha(fraction);
                        if (fraction == 1f) {
                            current.setImageBitmap(bitmap);
                            incoming.setImageDrawable(null);
                            incoming.setAlpha(0);
                            shown = n;
                        }
                    });
                    transition.start();
                }
                Log.i("CleanYourTV", "Wallpaper display=" + (n == -2 ? "bing" : FILES[n]) + " rotating=" + rotating);
            });
        });
    }

    /** 解码本地必应图，按屏幕尺寸降采样。 */
    private Bitmap bingImage() {
        java.io.File f = bing.file();
        if (f == null) return null;
        android.util.DisplayMetrics screen = activity.getResources().getDisplayMetrics();
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), bounds);
        if (bounds.outWidth <= 0) return null;
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= screen.widthPixels
                && bounds.outHeight / (sample * 2) >= screen.heightPixels) sample *= 2;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(f.getPath(), o);
    }

    private void save() {
        preferences.edit().putInt("index", index).putBoolean("rotating", rotating)
                .putBoolean("default", useDefault).putBoolean("bing", bingMode).apply();
    }

    private void schedule() {
        main.removeCallbacks(rotate);
        if (active && rotating && !useDefault && picker == null) main.postDelayed(rotate, ROTATION_MS);
    }

    void resume() { active = true; schedule(); }

    void pause() {
        active = false;
        main.removeCallbacks(rotate);
        if (picker != null) picker.dismiss();
        if (transition != null) transition.end();
    }

    void destroy() {
        ++request;
        decoder.shutdownNow();
        main.removeCallbacksAndMessages(null);
        if (transition != null) {
            transition.cancel();
            transition.removeAllUpdateListeners();
            transition = null;
        }
        current.setImageDrawable(null);
        incoming.setImageDrawable(null);
    }

    private String name(int n) { return n < 0 ? "默认壁纸" : ZH[n]; }

    /** Apple Aerial 航拍壁纸下载商店（遥控器操作）。从壁纸面板进入，picker 已关闭。 */
    private void showAerialStore() {
        Dialog store = new Dialog(activity);
        FrameLayout content = new FrameLayout(activity);
        TextView title = label("航拍视频壁纸", 24);
        place(content, title, 400, 34, 48, 30);
        TextView status = label(activity.getString(R.string.aerials_loading), 14);
        place(content, status, 800, 26, 48, 76);
        android.widget.LinearLayout rows = new android.widget.LinearLayout(activity);
        rows.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.ScrollView scroll = new android.widget.ScrollView(activity);
        scroll.addView(rows, new FrameLayout.LayoutParams(-1, -2));
        place(content, scroll, 700, 340, 48, 116);
        store.setContentView(content);
        Window w = store.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(0xE010141C));
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        w.setDimAmount(.3f);
        store.show();
        w.setLayout(820, -2);

        AerialDownloads.loadCatalog(activity, list -> {
            if (!store.isShowing()) return;
            if (list == null) {
                status.setText(activity.getString(R.string.aerials_offline));
                return;
            }
            status.setText("上下选择 · 确定下载/删除 · 返回关闭");
            for (AerialDownloads.Aerial a : list) {
                TextView row = new TextView(activity);
                row.setTextSize(15);
                row.setTextColor(Color.WHITE);
                row.setPadding(24, 18, 24, 18);
                row.setFocusable(true);
                row.setBackground(border(false));
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = 8;
                rows.addView(row, lp);
                updateAerialRow(row, a);
                row.setOnFocusChangeListener((v, f) -> row.setBackground(border(f)));
                row.setOnClickListener(v -> {
                    if (AerialDownloads.onTv(activity, a)) {
                        // 已在电视：设为当前航拍壁纸并关闭
                        AerialBackdrop.choose(activity, a.file);
                        Toast.makeText(activity, a.title + " 已设为壁纸", Toast.LENGTH_SHORT).show();
                        store.dismiss();
                        // 重启桌面生效
                        activity.recreate();
                    } else if (AerialDownloads.downloading == null) {
                        AerialDownloads.download(activity, a, (pct, msg) -> {
                            if (!store.isShowing()) return;
                            if (pct == 101) {
                                Toast.makeText(activity, activity.getString(R.string.aerials_done, a.title), Toast.LENGTH_SHORT).show();
                                updateAerialRow(row, a);
                            } else if (pct >= 0 && pct <= 100) {
                                row.setText(a.title + "  " + activity.getString(R.string.aerials_percent, pct));
                            } else if (msg != null) {
                                row.setText(a.title + "  " + msg);
                            }
                        });
                    }
                });
            }
            if (rows.getChildCount() > 0) rows.getChildAt(0).requestFocus();
        });
    }

    private void updateAerialRow(TextView row, AerialDownloads.Aerial a) {
        boolean onTv = AerialDownloads.onTv(activity, a);
        row.setText(a.title + (onTv ? "  ✓ " + activity.getString(R.string.aerials_on_tv) : ""));
    }

    // ---- picker (remote-friendly dialog) ---------------------------------------------------

    private TextView label(String value, float sizeSp) {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextColor(Color.WHITE);
        text.setTextSize(sizeSp);
        text.setIncludeFontPadding(false);
        return text;
    }

    private GradientDrawable border(boolean focus) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xb5202228);
        float d = activity.getResources().getDisplayMetrics().density;
        bg.setCornerRadius(12 * d);
        bg.setStroke(Math.round((focus ? 3 : 1) * d), focus ? Color.WHITE : 0x60ffffff);
        return bg;
    }

    private void place(FrameLayout parent, View view, int wDp, int hDp, int leftDp, int topDp) {
        float d = activity.getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                Math.round(wDp * d), Math.round(hDp * d));
        params.leftMargin = Math.round(leftDp * d);
        params.topMargin = Math.round(topDp * d);
        parent.addView(view, params);
    }

    void showPicker() {
        if (picker != null) return;
        main.removeCallbacks(rotate);
        preview = useDefault ? -1 : (bingMode ? -2 : index);
        picker = new Dialog(activity);
        FrameLayout content = new FrameLayout(activity);
        // Only the content layers above the wallpaper are hidden during preview; the
        // ambient/sky layers follow the wallpaper state, not this hide/show cycle.
        FrameLayout root = (FrameLayout) background.getParent();
        java.util.ArrayList<View> hidden = new java.util.ArrayList<>();
        for (int n = 0; n < root.getChildCount(); n++) {
            View child = root.getChildAt(n);
            if (child != background && child.getVisibility() == View.VISIBLE
                    && child.getId() != R.id.ambient && child.getId() != R.id.sky) {
                child.setVisibility(View.INVISIBLE);
                hidden.add(child);
            }
        }

        TextView title = label("壁纸设置", 26);
        place(content, title, 300, 38, 48, 28);

        TextView mode = label(
                useDefault ? "当前：默认壁纸 · 纯黑背景＋应用主题色光晕"
                        : bingMode ? "当前：必应每日壁纸 · 每天自动换一张"
                        : rotating ? "当前：自动轮播【每 5 分钟】" : "当前：固定壁纸", 13);
        place(content, mode, 600, 25, 48, 73);

        TextView auto = label(rotating ? "壁纸轮播：开" : "壁纸轮播：关", 14);
        auto.setGravity(Gravity.CENTER);
        auto.setFocusable(true);
        auto.setBackground(border(false));
        place(content, auto, 144, 40, 620, 42);

        // 必应每日壁纸开关（tvhome-zh 移植）：打开后每天自动换必应今日图
        TextView bingBtn = label(bingMode || bing.enabled() ? "必应每日壁纸：开" : "必应每日壁纸：关", 14);
        bingBtn.setGravity(Gravity.CENTER);
        bingBtn.setFocusable(true);
        bingBtn.setBackground(border(false));
        place(content, bingBtn, 190, 40, 780, 42);

        TextView status = label("", 14);
        status.setGravity(Gravity.CENTER);
        place(content, status, 864, 28, 48, 496);

        FrameLayout[] cards = new FrameLayout[FILES.length + 1];
        for (int n = 0; n < cards.length; n++) {
            final int slot = n, choice = n - 1;
            FrameLayout card = new FrameLayout(activity);
            cards[n] = card;
            card.setFocusable(true);
            float d = activity.getResources().getDisplayMetrics().density;
            card.setPadding(Math.round(5 * d), Math.round(5 * d), Math.round(5 * d), Math.round(5 * d));
            card.setBackground(border(false));
            card.setContentDescription(name(choice));
            place(content, card, 204, 162, 48 + n * 220, 318);

            ImageView thumb = image();
            card.addView(thumb, new FrameLayout.LayoutParams(-1, Math.round(112 * d)));
            if (choice < 0) thumb.setImageBitmap(defaultThumbnail());
            else decoder.execute(() -> {
                Bitmap bitmap = thumbnail(choice);
                main.post(() -> { if (picker != null && picker.isShowing()) thumb.setImageBitmap(bitmap); });
            });

            boolean checked = choice < 0 ? useDefault : !useDefault && !rotating && index == choice;
            TextView caption = label(name(choice) + (checked ? "  ✓" : ""), 15);
            caption.setGravity(Gravity.CENTER);
            card.addView(caption, new FrameLayout.LayoutParams(-1, Math.round(32 * d), Gravity.BOTTOM));

            card.setOnFocusChangeListener((v, focus) -> {
                card.setBackground(border(focus));
                if (focus) { preview = choice; display(choice, true); status.setText(name(choice)); }
            });
            card.setOnClickListener(v -> commit(choice < 0 ? index : choice, false, choice < 0, false));
            card.setOnKeyListener((v, key, event) -> {
                if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN)
                        cards[Math.max(0, Math.min(cards.length - 1,
                                slot + (key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1)))].requestFocus();
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (key == KeyEvent.KEYCODE_DPAD_UP && event.getAction() == KeyEvent.ACTION_DOWN)
                        auto.requestFocus();
                    return true;
                }
                return false;
            });
        }

        auto.setOnFocusChangeListener((v, focus) -> {
            auto.setBackground(border(focus));
            if (focus) status.setText(rotating
                    ? "按确定关闭轮播，固定当前风景壁纸" : "按确定开启三张风景壁纸轮播");
        });
        auto.setOnClickListener(v -> commit(preview < 0 ? index : preview, !rotating, false, false));
        auto.setOnKeyListener((v, key, event) -> {
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) cards[preview + 1].requestFocus();
                return true;
            }
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) bingBtn.requestFocus();
                return true;
            }
            return key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_LEFT;
        });

        bingBtn.setOnFocusChangeListener((v, focus) -> {
            bingBtn.setBackground(border(focus));
            if (focus) status.setText(bingMode || bing.enabled()
                    ? "按确定关闭必应每日壁纸（当前壁纸保留）" : "按确定开启：每天自动换一张必应今日图");
        });
        bingBtn.setOnClickListener(v -> {
            boolean on = !(bingMode || bing.enabled());
            bing.setEnabled(on);
            if (on) {
                // 开启：立即后台拉一张，拉到就切到必应模式
                status.setText("正在下载今日必应壁纸…");
                decoder.execute(() -> {
                    bing.refreshIfDue();
                    main.post(() -> {
                        if (picker == null || activity.isDestroyed()) return;
                        if (bing.file() != null) commit(0, false, false, true);
                        else status.setText("下载失败（电视可能没联网），明天开机自动重试");
                    });
                });
            } else {
                // 关闭：保留当前图当固定壁纸（不删文件，明天不再换）
                commit(index, false, false, false);
                Toast.makeText(activity, "已关闭必应每日壁纸", Toast.LENGTH_SHORT).show();
            }
        });
        bingBtn.setOnKeyListener((v, key, event) -> {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) auto.requestFocus();
                return true;
            }
            return key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN
                    || key == KeyEvent.KEYCODE_DPAD_RIGHT;
        });

        // Apple Aerial 航拍视频壁纸下载/管理入口
        TextView aerialBtn = label(activity.getString(R.string.aerials_download), 14);
        aerialBtn.setGravity(Gravity.CENTER);
        aerialBtn.setFocusable(true);
        aerialBtn.setBackground(border(false));
        place(content, aerialBtn, 190, 40, 986, 42);
        aerialBtn.setOnFocusChangeListener((v, focus) -> {
            aerialBtn.setBackground(border(focus));
            if (focus) status.setText("下载 Apple 航拍视频壁纸（首次需联网，几十 MB）");
        });
        aerialBtn.setOnClickListener(v -> {
            picker.dismiss();
            showAerialStore();
        });
        aerialBtn.setOnKeyListener((v, key, event) -> {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) bingBtn.requestFocus();
                return true;
            }
            return key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN
                    || key == KeyEvent.KEYCODE_DPAD_RIGHT;
        });

        bingBtn.setOnKeyListener((v, key, event) -> {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) auto.requestFocus();
                return true;
            }
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) aerialBtn.requestFocus();
                return true;
            }
            return key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN;
        });

        picker.setContentView(content);
        picker.setOnDismissListener(dialog -> {
            picker = null;
            for (View v : hidden) v.setVisibility(View.VISIBLE);
            display(useDefault ? -1 : (bingMode ? -2 : index), true);
            schedule();
        });
        Window window = picker.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(.12f);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        picker.show();
        window.setLayout(-1, -1);
        cards[useDefault ? 0 : index + 1].requestFocus();
    }

    private void commit(int choice, boolean slideshow, boolean defaultWallpaper, boolean bingOn) {
        index = choice;
        rotating = slideshow;
        useDefault = defaultWallpaper;
        bingMode = bingOn;
        if (bingOn) { useDefault = false; rotating = false; }
        save();
        picker.dismiss();
        Toast.makeText(activity,
                bingOn ? "已开启必应每日壁纸"
                        : defaultWallpaper ? "已恢复默认壁纸"
                        : slideshow ? "已开启壁纸轮播" : "已关闭轮播，使用固定壁纸",
                Toast.LENGTH_SHORT).show();
    }

    private Bitmap defaultThumbnail() {
        Bitmap bitmap = Bitmap.createBitmap(360, 203, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        paint.setShader(new RadialGradient(116, 59, 232,
                new int[]{0xac557eac, 0x40557eac, Color.TRANSPARENT},
                new float[]{0, .5f, 1}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 360, 203, paint);
        return bitmap;
    }
}
