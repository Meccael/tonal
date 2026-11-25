package com.meccael.tonal.application;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import java.io.File;

/**
 * Works out which audio file another app asked Tonal to open ("Open with" / a file manager).
 * Modern file managers hand over content:// links instead of file paths, so the link is traced back
 * to a real path through the media store and the storage providers.
 */
final class OpenWith {
    private static final String TAG = "TonalOpenWith";

    private OpenWith() {
    }

    /** The file the intent points at, or null when it can't be identified. */
    static File resolve(Context context, Intent intent) {
        if (intent == null || intent.getData() == null) {
            return null;
        }
        Uri uri = intent.getData();
        try {
            String scheme = uri.getScheme();
            if ("file".equals(scheme)) {
                return uri.getPath() == null ? null : new File(uri.getPath());
            }
            if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                String path = pathFromContentUri(context, uri);
                if (path != null) {
                    Log.i(TAG, "Resolved " + uri + " to " + path);
                    return new File(path);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not resolve " + uri, e);
        }
        Log.w(TAG, "Could not resolve " + uri);
        return null;
    }

    private static String pathFromContentUri(Context context, Uri uri) {
        // 1. Media-store style links carry the path in a "_data" column.
        String path = singleString(context, uri, MediaStore.MediaColumns.DATA, null, null);
        if (isFile(path)) {
            return path;
        }
        // 2. Storage-provider document links.
        if (DocumentsContract.isDocumentUri(context, uri)) {
            String authority = uri.getAuthority();
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.externalstorage.documents".equals(authority)) {
                path = pathFromStorageDocument(docId);
                if (isFile(path)) {
                    return path;
                }
            } else if ("com.android.providers.media.documents".equals(authority) && docId.contains(":")) {
                Uri audio = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, docId.substring(docId.indexOf(':') + 1));
                path = singleString(context, audio, MediaStore.MediaColumns.DATA, null, null);
                if (isFile(path)) {
                    return path;
                }
            }
        }
        // 3. Last resort: find the media-store row with the same file name and size.
        return pathFromNameAndSize(context, uri);
    }

    /** "primary:Music/Song.mp3" or "1234-ABCD:Song.mp3" to its real path. */
    private static String pathFromStorageDocument(String docId) {
        int colon = docId.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String volume = docId.substring(0, colon);
        String relative = docId.substring(colon + 1);
        if ("primary".equalsIgnoreCase(volume)) {
            return new File(Environment.getExternalStorageDirectory(), relative).getPath();
        }
        return "/storage/" + volume + "/" + relative;
    }

    private static String pathFromNameAndSize(Context context, Uri uri) {
        String name = null;
        long size = -1;
        Cursor info = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
        if (info != null) {
            try {
                if (info.moveToFirst()) {
                    name = info.getString(0);
                    size = info.isNull(1) ? -1 : info.getLong(1);
                }
            } finally {
                info.close();
            }
        }
        if (name == null) {
            return null;
        }
        String where = MediaStore.MediaColumns.DISPLAY_NAME + "=?" + (size >= 0 ? " AND " + MediaStore.MediaColumns.SIZE + "=?" : "");
        String[] args = size >= 0 ? new String[]{name, String.valueOf(size)} : new String[]{name};
        String path = singleString(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.MediaColumns.DATA, where, args);
        return isFile(path) ? path : null;
    }

    private static String singleString(Context context, Uri uri, String column, String where, String[] args) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{column}, where, args, null);
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getString(0);
            }
        } catch (Exception ignored) {
            // The provider doesn't have this column.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static boolean isFile(String path) {
        return path != null && new File(path).isFile();
    }
}
