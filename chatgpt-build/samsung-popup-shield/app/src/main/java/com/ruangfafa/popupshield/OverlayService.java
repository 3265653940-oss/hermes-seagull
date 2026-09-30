package com.ruangfafa.popupshield;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayService extends Service {
    private static final String CHANNEL_ID = "popup_shield_overlay";
    private static final int NOTIFICATION_ID = 43120;

    private WindowManager windowManager;
    private TextView bubble;
    private WindowManager.LayoutParams params;

    private float downRawX, downRawY;
    private int downX, downY;
    private boolean moved;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            updateBubble();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        registerStateReceiver();
        showOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Prefs.isOverlayEnabled(this) || !Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (bubble == null) showOverlay();
        updateBubble();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
        if (bubble != null && windowManager != null) {
            try { windowManager.removeView(bubble); } catch (Exception ignored) {}
        }
        bubble = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void registerStateReceiver() {
        IntentFilter filter = new IntentFilter(BlockController.ACTION_LOCAL_STATE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    private void showOverlay() {
        if (!Settings.canDrawOverlays(this) || bubble != null) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        bubble = new TextView(this);
        bubble.setGravity(Gravity.CENTER);
        bubble.setTextSize(14);
        bubble.setTextColor(Color.WHITE);
        bubble.setPadding(dp(13), dp(8), dp(13), dp(8));
        bubble.setElevation(dp(8));
        updateBubble();

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Prefs.getX(this);
        params.y = Prefs.getY(this);

        bubble.setOnTouchListener(this::onTouch);
        try {
            windowManager.addView(bubble, params);
        } catch (Exception e) {
            Toast.makeText(this, "悬浮窗启动失败，请检查显示在其他应用上层权限", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                downX = params.x;
                downY = params.y;
                moved = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                int dx = Math.round(event.getRawX() - downRawX);
                int dy = Math.round(event.getRawY() - downRawY);
                if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) moved = true;
                params.x = downX + dx;
                params.y = downY + dy;
                try { windowManager.updateViewLayout(bubble, params); } catch (Exception ignored) {}
                return true;
            case MotionEvent.ACTION_UP:
                if (moved) {
                    Prefs.setPosition(this, params.x, params.y);
                } else {
                    boolean next = !Prefs.isBlocking(this);
                    BlockController.setBlocking(this, next);
                    updateBubble();
                    Toast.makeText(this, next ? "新标签拦截：ON" : "新标签拦截：OFF", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return false;
        }
    }

    private void updateBubble() {
        if (bubble == null) return;
        boolean on = Prefs.isBlocking(this);
        bubble.setText(on ? "拦截 ON" : "拦截 OFF");
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(on ? Color.rgb(35, 135, 70) : Color.rgb(105, 105, 105));
        bg.setStroke(dp(1), Color.argb(80, 255, 255, 255));
        bubble.setBackground(bg);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "悬浮开关",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("保持网页上的拦截开关悬浮显示");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent launch = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 0, launch, flags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setContentTitle("三星新标签拦截开关")
                .setContentText("悬浮开关正在运行")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
