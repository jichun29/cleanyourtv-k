package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 检查更新页 v2（K 2026-10-22）。
 *
 * 布局：左侧版本信息 + 右侧更新日志（可滚动），比堆在中间的卡片更适合宽屏电视。
 * 新增：无更新时也显示当前版本的更新日志，让用户知道自己用的版本带了什么。
 */
public final class UpdateActivity extends Activity {

    private TextView statusText;
    private TextView versionText;
    private TextView notesText;
    private TextView logTitle;
    private LinearLayout actions;
    private LinearLayout logPanel;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_update);

        statusText = findViewById(R.id.updateStatus);
        versionText = findViewById(R.id.updateVersions);
        notesText = findViewById(R.id.updateNotes);
        logTitle = findViewById(R.id.updateLogTitle);
        actions = findViewById(R.id.updateActions);
        logPanel = findViewById(R.id.updateLogPanel);
        progress = findViewById(R.id.updateProgress);

        findViewById(R.id.updateClose).setOnClickListener(v -> finish());

        check();
    }

    private void check() {
        statusText.setText("正在检查更新…");
        statusText.setTextColor(0xBFFFFFFF);
        progress.setVisibility(View.VISIBLE);
        actions.setVisibility(View.GONE);
        logPanel.setVisibility(View.GONE);

        UpdateChecker.checkNow(this, r -> {
            progress.setVisibility(View.GONE);
            if (r.error != null) {
                statusText.setText("检查失败");
                statusText.setTextColor(0xFFFF6B6B);
                versionText.setText(r.error);
                // 失败时也显示当前版本的日志（从 GitHub 取当前 tag 的 body）
                showCurrentVersionLog(r.currentVersion);
                return;
            }
            if (!r.hasUpdate) {
                statusText.setText("已是最新版本");
                statusText.setTextColor(0xFF4ADE80);
                statusText.setTextSize(18);
                versionText.setText(getString(R.string.update_current_line, r.currentVersion));
                versionText.setTextColor(0x99FFFFFF);
                // 显示当前版本的更新日志
                showCurrentVersionLog(r.currentVersion);
                return;
            }
            // 有新版：显示新版的更新日志
            statusText.setText("发现新版本");
            statusText.setTextColor(0xFFFFB347);
            statusText.setTextSize(18);
            versionText.setText(getString(R.string.update_new_line, r.currentVersion, r.latestTag));
            versionText.setTextColor(0xE6FFFFFF);
            logTitle.setText("新版本 " + r.latestTag + " 更新内容");
            notesText.setText(UpdateChecker.shortenNotes(r.body));
            logPanel.setVisibility(View.VISIBLE);
            actions.setVisibility(View.VISIBLE);

            Button download = findViewById(R.id.updateDownload);
            Button skip = findViewById(R.id.updateSkip);
            download.setOnClickListener(v -> startDownload(r.apkUrl));
            skip.setOnClickListener(v -> {
                UpdateChecker.skipTag(this, r.latestTag);
                finish();
            });
        });
    }

    /** 显示当前版本的更新日志（从 GitHub 取当前 tag 的 release body）。 */
    private void showCurrentVersionLog(String currentVersion) {
        // 当前版本对应的 tag（处理 -lite 后缀）
        String tag = "v" + currentVersion.replaceAll("-.*$", "");
        new Thread(() -> {
            String body = UpdateChecker.fetchTagBody(tag);
            runOnUiThread(() -> {
                if (body != null && !body.isEmpty()) {
                    logTitle.setText("当前版本 " + tag + " 更新内容");
                    notesText.setText(UpdateChecker.shortenNotes(body));
                    logPanel.setVisibility(View.VISIBLE);
                } else {
                    logTitle.setText("更新日志");
                    notesText.setText("暂无当前版本的更新日志");
                    logPanel.setVisibility(View.VISIBLE);
                }
            });
        }, "fetch-current-log").start();
    }

    /** 下载：把加速链接交给系统浏览器。 */
    private void startDownload(String apkUrl) {
        if (apkUrl == null) return;
        String accelerated = "https://ghfast.top/" + apkUrl;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(accelerated));
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                startActivity(i);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}