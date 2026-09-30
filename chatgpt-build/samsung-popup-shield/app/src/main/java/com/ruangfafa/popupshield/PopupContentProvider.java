package com.ruangfafa.popupshield;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PopupContentProvider extends ContentProvider {
    private static final String FILTER_FILE = "popup_filters.txt";

    @Override
    public boolean onCreate() {
        Context c = getContext();
        if (c != null) refreshFilters(c);
        return true;
    }

    public static synchronized void refreshFilters(Context context) {
        File file = new File(context.getFilesDir(), FILTER_FILE);
        String rules;
        if (Prefs.isBlocking(context)) {
            rules = "! Samsung Popup Shield - ON\n" +
                    "! Block any page opened as a popup/new tab by page script or popup handling.\n" +
                    "*$popup\n" +
                    "about:blank$popup\n";
        } else {
            rules = "! Samsung Popup Shield - OFF\n";
        }
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(rules.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException ignored) {
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Context c = getContext();
        if (c == null) throw new FileNotFoundException("Context unavailable");
        refreshFilters(c);
        File file = new File(c.getFilesDir(), FILTER_FILE);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        return "text/plain";
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
}
