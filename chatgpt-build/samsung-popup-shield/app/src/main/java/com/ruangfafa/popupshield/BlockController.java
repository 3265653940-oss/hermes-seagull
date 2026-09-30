package com.ruangfafa.popupshield;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class BlockController {
    public static final String ACTION_LOCAL_STATE = "com.ruangfafa.popupshield.STATE_CHANGED";
    private static final String SAMSUNG_UPDATE = "com.samsung.android.sbrowser.contentBlocker.ACTION_UPDATE";

    private BlockController() {}

    public static void setBlocking(Context context, boolean enabled) {
        Prefs.setBlocking(context, enabled);
        PopupContentProvider.refreshFilters(context);

        Intent samsung = new Intent(SAMSUNG_UPDATE);
        samsung.setData(Uri.parse("package:" + context.getPackageName()));
        context.sendBroadcast(samsung);

        Intent local = new Intent(ACTION_LOCAL_STATE);
        local.setPackage(context.getPackageName());
        context.sendBroadcast(local);
    }
}
