package com.ruangfafa.popupshield;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String NAME = "popup_shield";
    private static final String KEY_BLOCKING = "blocking";
    private static final String KEY_OVERLAY = "overlay";
    private static final String KEY_X = "overlay_x";
    private static final String KEY_Y = "overlay_y";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean isBlocking(Context c) {
        return sp(c).getBoolean(KEY_BLOCKING, true);
    }

    public static void setBlocking(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_BLOCKING, value).apply();
    }

    public static boolean isOverlayEnabled(Context c) {
        return sp(c).getBoolean(KEY_OVERLAY, false);
    }

    public static void setOverlayEnabled(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_OVERLAY, value).apply();
    }

    public static int getX(Context c) {
        return sp(c).getInt(KEY_X, 24);
    }

    public static int getY(Context c) {
        return sp(c).getInt(KEY_Y, 240);
    }

    public static void setPosition(Context c, int x, int y) {
        sp(c).edit().putInt(KEY_X, x).putInt(KEY_Y, y).apply();
    }
}
