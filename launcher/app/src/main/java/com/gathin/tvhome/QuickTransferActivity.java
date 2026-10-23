package com.gathin.tvhome;

import android.content.Intent;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * 快传页（2026-10-23 K）：手机/电脑往电视推 APK 安装。
 *
 * 页面大字显示「http://电视IP:8899」（方法一）；旁边挂二维码（方法二，手机相机扫码即开，
 * 用 Google Chart API 画图，电视联外网才显示，断网也不影响方法一）。
 * 服务只在页面前台时监听，退出即停。收到 APK 直接弹系统安装框（Android 8+ 先检查「安装未知应用」权限）。
 */
public class QuickTransferActivity extends Activity {

    private QuickTransferServer server;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_transfer);

        TextView addr = findViewById(R.id.address);
        status = findViewById(R.id.status);

        String ip = QuickTransferServer.localIp();
        if (ip == null) {
            addr.setText("未联网");
            status.setText("电视连上 Wi-Fi 或网线后再试");
            return;
        }
        String url = "http://" + ip + ":" + QuickTransferServer.PORT;
        addr.setText(url);

        server = new QuickTransferServer(this, new QuickTransferServer.Listener() {
            @Override public void onApk(File apk) {
                status.setText("收到安装包，弹出安装…");
                install(apk);
            }
            @Override public void onError(String msg) {
                status.setText(msg);
            }
        });
        server.start();

        // 二维码：Google Chart API（国内可直连；加载失败就留空，不影响用）
        android.widget.ImageView qr = findViewById(R.id.qr);
        final String qrUrl = "https://chart.googleapis.com/chart?cht=qr&chs=360x360&chl="
                + Uri.encode(url);
        new Thread(() -> {
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection)
                        new java.net.URL(qrUrl).openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(8000);
                java.io.InputStream in = c.getInputStream();
                final android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
                in.close();
                if (bmp != null) runOnUiThread(() -> qr.setImageBitmap(bmp));
            } catch (Exception ignored) { /* 出不来就留空 */ }
        }).start();
    }

    private void install(File apk) {
        // Android 8+ 要「安装未知应用」权限
        if (android.os.Build.VERSION.SDK_INT >= 26
                && !getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(this, "请先允许本应用安装应用", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".updateapk", apk);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "打不开安装器：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (server != null) server.stop();
        super.onDestroy();
    }
}
