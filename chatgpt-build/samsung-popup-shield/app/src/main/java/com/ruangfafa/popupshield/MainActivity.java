package com.ruangfafa.popupshield;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private Switch blockSwitch;
    private Switch overlaySwitch;
    private boolean binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
        if (Prefs.isOverlayEnabled(this) && Settings.canDrawOverlays(this)) {
            startOverlay();
        }
    }

    private View buildUi() {
        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("三星新标签拦截开关");
        title.setTextSize(26);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 8, 0, 8);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("目标：允许当前页面正常跳转，但阻止网页通过 popup / 新标签把你强制切走。\n\n插件开关关闭时会向三星返回空过滤规则；开启时返回 popup 拦截规则。");
        desc.setTextSize(16);
        desc.setTextColor(Color.DKGRAY);
        desc.setLineSpacing(0, 1.25f);
        desc.setPadding(0, 8, 0, 24);
        root.addView(desc);

        blockSwitch = new Switch(this);
        blockSwitch.setText("拦截网页新标签 / 弹窗");
        blockSwitch.setTextSize(18);
        blockSwitch.setPadding(0, 8, 0, 8);
        blockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (binding) return;
            BlockController.setBlocking(this, isChecked);
            Toast.makeText(this, isChecked ? "拦截已开启" : "拦截已关闭", Toast.LENGTH_SHORT).show();
        });
        root.addView(blockSwitch);

        overlaySwitch = new Switch(this);
        overlaySwitch.setText("显示悬浮开关");
        overlaySwitch.setTextSize(18);
        overlaySwitch.setPadding(0, 14, 0, 14);
        overlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (binding) return;
            if (isChecked) {
                Prefs.setOverlayEnabled(this, true);
                if (!Settings.canDrawOverlays(this)) {
                    requestOverlayPermission();
                } else {
                    startOverlay();
                }
            } else {
                Prefs.setOverlayEnabled(this, false);
                stopService(new Intent(this, OverlayService.class));
            }
        });
        root.addView(overlaySwitch);

        Button samsungSettings = button("打开三星浏览器的广告拦截器设置");
        samsungSettings.setOnClickListener(v -> openSamsungSettings());
        root.addView(samsungSettings);

        Button devMode = button("打开三星 Content Blocker 测试入口");
        devMode.setOnClickListener(v -> openSamsungDevMode());
        root.addView(devMode);

        TextView help = new TextView(this);
        help.setText("首次安装：\n1. 打开三星浏览器，在地址栏输入 internet://contentblock，开启 Content Blocker 开发模式。\n2. 到 三星浏览器 → 菜单 → 广告拦截器/内容拦截器，启用本插件。\n3. 回到本 App 开启“拦截”。\n4. 如需网页中随时切换，再开启“显示悬浮开关”并授予“显示在其他应用上层”权限。\n\n悬浮胶囊：点击 = ON/OFF；拖动 = 移动位置。\n\n注意：这是 Samsung Content Blocker 规则实现，不是完整 WebExtension。实际能否拦住某网站取决于三星浏览器对 ABP $popup 规则的支持；它不会修改或注入网页 JavaScript。");
        help.setTextSize(14);
        help.setTextColor(Color.DKGRAY);
        help.setLineSpacing(0, 1.2f);
        help.setPadding(0, 24, 0, 30);
        root.addView(help);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        b.setLayoutParams(lp);
        return b;
    }

    private void refreshUi() {
        if (blockSwitch == null || overlaySwitch == null) return;
        binding = true;
        blockSwitch.setChecked(Prefs.isBlocking(this));
        boolean wanted = Prefs.isOverlayEnabled(this);
        if (wanted && !Settings.canDrawOverlays(this)) {
            Prefs.setOverlayEnabled(this, false);
            wanted = false;
        }
        overlaySwitch.setChecked(wanted);
        binding = false;
    }

    private void requestOverlayPermission() {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "无法打开悬浮窗权限页面", Toast.LENGTH_LONG).show();
        }
    }

    private void startOverlay() {
        Intent i = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
    }

    private void openSamsungSettings() {
        Intent i = new Intent("com.samsung.android.sbrowser.contentBlocker.ACTION_SETTING");
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "未找到三星浏览器的 Content Blocker 设置", Toast.LENGTH_LONG).show();
        }
    }

    private void openSamsungDevMode() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("internet://contentblock"));
        i.setPackage("com.sec.android.app.sbrowser");
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "请手动在三星浏览器地址栏输入：internet://contentblock", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
