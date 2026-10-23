package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * U 盘入口（2026-10-23 K）：插上 U 盘时桌面网格里出现一个「U 盘」磁贴，点进去打开 U 盘目录。
 *
 * 检测：扫 /storage 和 /mnt 下的可读写挂载点，排除内置存储（emulated/self）。
 * 打开方式按顺序试：
 *   1. 系统文档查看器（ACTION_VIEW 指向 U 盘根目录）
 *   2. 已装的文件管理器（小白文件管理器 com.xiaobaifile.tv 优先，其次常见三方）
 *   3. 都不行就弹提示装个文件管理器
 */
public class UsbStorage {

    private static final String TAG = "TvHome.Usb";

    /** U 盘挂载点列表（可读写、非内置）。没插 = 空表。 */
    public static List<File> mounted() {
        List<File> out = new ArrayList<>();
        for (String root : new String[]{"/storage", "/mnt"}) {
            File dir = new File(root);
            File[] kids = dir.listFiles();
            if (kids == null) continue;
            for (File f : kids) {
                String name = f.getName().toLowerCase();
                // 跳过内置/系统挂载点
                if (name.equals("emulated") || name.equals("self") || name.equals("sdcard0")
                        || name.contains("internal")) continue;
                if (!f.isDirectory() || !f.canRead()) continue;
                // 真挂载点里通常能列出东西；空壳目录跳过
                try {
                    File[] inside = f.listFiles();
                    if (inside == null) continue;
                    out.add(f);
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    public static boolean isPlugged() {
        return !mounted().isEmpty();
    }

    /** 打开 U 盘。成功 true。 */
    public static boolean open(Context ctx) {
        List<File> mounts = mounted();
        if (mounts.isEmpty()) return false;
        File root = mounts.get(0);

        // 1. 文档查看器
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(root), "resource/folder");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (ctx.getPackageManager().resolveActivity(i, 0) != null) {
                ctx.startActivity(i);
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "documentsui", e);
        }

        // 2. 常见文件管理器（小白文件管理器在 K 的 dock 里）
        String[] managers = {
                "com.xiaobaifile.tv",        // 小白文件管理器
                "com.estrongs.android.pop",  // ES
                "com.alphainventor.filemanager",
                "pl.comolid.fileexplorer",
        };
        for (String pkg : managers) {
            try {
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    return true;
                }
            } catch (Exception ignored) {}
        }

        // 3. 最后手段：让系统挑能打开文件夹的
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(root), "*/*");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "fallback", e);
        }
        return false;
    }
}
