package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 检查更新（K 2026-10-22 需求）。
 *
 * 数据源 = 我们自己的 GitHub 仓库 jichun29/cleanyourtv-k 的 Releases API。
 * - 手动：设置里「检查更新」按一下立刻查。
 * - 自动：默认关闭，开了以后每 3 小时查一次（参考上游 aerial-tv v1.26 的频率）。
 *
 * 加速源：GitHub 直连在国内可能很慢甚至超时，所以按顺序尝试多个镜像。
 * 用的是已经在 K 的常用工具链里验证过的几个，不引入新的不确定源。
 *
 * 不联网第三方、不上报任何数据，只读我们自己的 Release。
 */
public final class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String REPO = "jichun29/cleanyourtv-k";
    private static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final long AUTO_INTERVAL_MS = 3 * 60 * 60 * 1000L; // 3 小时
    private static final String PREF = "update_checker";
    private static final String KEY_AUTO = "auto_check";
    private static final String KEY_LAST_CHECK = "last_check";
    private static final String KEY_SKIPPED = "skipped_tag";

    private UpdateChecker() {}

    /** 一次更新的结果。 */
    public static final class Result {
        public final boolean hasUpdate;
        public final String currentVersion;
        public final String latestTag;
        public final String latestName;
        public final String body;
        public final String apkUrl;
        public final String error;

        Result(boolean hasUpdate, String currentVersion, String latestTag, String latestName,
               String body, String apkUrl, String error) {
            this.hasUpdate = hasUpdate;
            this.currentVersion = currentVersion;
            this.latestTag = latestTag;
            this.latestName = latestName;
            this.body = body;
            this.apkUrl = apkUrl;
            this.error = error;
        }
    }

    public interface Callback {
        void onResult(Result result);
    }

    /** 手动检查（设置里点「检查更新」）。 */
    public static void checkNow(Context ctx, Callback cb) {
        new Thread(() -> {
            Result r = fetch(ctx);
            new android.os.Handler(ctx.getMainLooper()).post(() -> cb.onResult(r));
        }, "update-check").start();
    }

    /** 自动检查：到了间隔才查，否则直接返回。 */
    public static void checkIfDue(Context ctx, Callback cb) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!sp.getBoolean(KEY_AUTO, false)) return; // 默认关闭
        long last = sp.getLong(KEY_LAST_CHECK, 0);
        if (System.currentTimeMillis() - last < AUTO_INTERVAL_MS) return;
        checkNow(ctx, cb);
    }

    /** 自动检查开关。 */
    public static boolean isAutoEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_AUTO, false);
    }

    public static void setAutoEnabled(Context ctx, boolean on) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_AUTO, on).apply();
    }

    /** 用户点过「跳过这个版本」，就不再提醒同一版。 */
    public static void skipTag(Context ctx, String tag) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_SKIPPED, tag).apply();
    }

    private static String skippedTag(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_SKIPPED, "");
    }

    /** 实际请求，依次尝试加速源。 */
    private static Result fetch(Context ctx) {
        String current = currentVersion(ctx);
        // 加速源：第一个是 GitHub 直连，后面是国内镜像。先试哪个取决于上次哪个通了。
        String[] sources = {
                API,
                "https://ghfast.top/" + API,
                "https://ghproxy.net/" + API,
                "https://gh-proxy.com/" + API,
        };
        String body = null;
        for (String src : sources) {
            body = httpGet(src);
            if (body != null) break;
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply();

        if (body == null) {
            return new Result(false, current, null, null, null, null, "网络不可用，所有源都连不上");
        }
        try {
            JSONObject j = new JSONObject(body);
            String tag = j.optString("tag_name", "");
            String name = j.optString("name", tag);
            String notes = j.optString("body", "");
            String apkUrl = null;
            if (j.has("assets")) {
                for (int i = 0; i < j.getJSONArray("assets").length(); i++) {
                    JSONObject a = j.getJSONArray("assets").getJSONObject(i);
                    if ("CleanYourTV-Launcher-universal.apk".equals(a.optString("name"))) {
                        apkUrl = a.optString("browser_download_url");
                        break;
                    }
                }
            }
            boolean newer = isNewer(tag, current);
            if (newer && tag.equals(skippedTag(ctx))) newer = false; // 用户跳过了这一版
            return new Result(newer, current, tag, name, notes, apkUrl, null);
        } catch (Exception e) {
            Log.w(TAG, "parse failed", e);
            return new Result(false, current, null, null, null, null, "更新信息解析失败");
        }
    }

    /** 简单版本号比较：3.9.5 vs 3.9.4。 */
    static boolean isNewer(String tag, String current) {
        String a = tag.replaceFirst("^v", "").replaceAll("-.*$", ""); // 去掉 -lite 后缀
        String b = current.replaceFirst("^v", "").replaceAll("-.*$", ""); // 去掉 -lite
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            int x = i < pa.length ? parseInt(pa[i]) : 0;
            int y = i < pb.length ? parseInt(pb[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; }
    }

    private static String currentVersion(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "0.0";
        }
    }

    private static String httpGet(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "CleanYourTV-Launcher");
            if (c.getResponseCode() != 200) return null;
            InputStream in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            in.close();
            return out.toString("UTF-8");
        } catch (Exception e) {
            Log.w(TAG, "httpGet " + url + ": " + e.getMessage());
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** 把 GitHub 的下载地址换成加速源。 */
    public static List<String> apkMirrors(String apkUrl) {
        List<String> list = new ArrayList<>();
        if (apkUrl == null) return list;
        list.add(apkUrl);
        list.add("https://ghfast.top/" + apkUrl);
        list.add("https://ghproxy.net/" + apkUrl);
        list.add("https://gh-proxy.com/" + apkUrl);
        return list;
    }

    /** 取指定 tag 的 release body（用于显示当前版本的更新日志）。 */
    public static String fetchTagBody(String tag) {
        String url = "https://api.github.com/repos/" + REPO + "/releases/tags/" + tag;
        String body = httpGet(url);
        if (body == null) {
            // 直连失败时尝试加速源
            String[] mirrors = {
                    "https://ghfast.top/" + url,
                    "https://ghproxy.net/" + url,
                    "https://gh-proxy.com/" + url,
            };
            for (String src : mirrors) {
                body = httpGet(src);
                if (body != null) break;
            }
        }
        if (body == null) return null;
        try {
            JSONObject j = new JSONObject(body);
            return j.optString("body", "");
        } catch (Exception e) {
            return null;
        }
    }

    /** 更新日志简写：去掉空行、行数压到 6 行内，适合电视屏。 */
    public static String shortenNotes(String body) {
        if (body == null) return "";
        String[] lines = body.replace("\r", "").split("\n");
        StringBuilder sb = new StringBuilder();
        int kept = 0;
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("#")) continue; // 去掉 markdown 标题符号
            if (t.startsWith("|")) continue; // 去掉表格行
            if (t.startsWith("![")) continue; // 去掉图片
            sb.append(t).append('\n');
            if (++kept >= 6) break;
        }
        String r = sb.toString().trim();
        return r.isEmpty() ? "修复若干问题，体验更稳定" : r;
    }
}