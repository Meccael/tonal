package com.meccael.tonal.artwork;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Tonal's own artwork storage. Files live in the app's private storage, keyed by the
 * MediaStore album id, so the user's audio files and the system media database are never touched.
 */
public final class ArtworkStore {
    private static final String PREFS = "tonal_artwork";
    private static final String KEY_AUTO = "auto_enabled";
    private static final long MISS_TTL_MS = 30L * 24 * 60 * 60 * 1000; // retry a miss after 30 days

    private ArtworkStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static File dir(Context c) {
        File d = new File(c.getApplicationContext().getFilesDir(), "artwork");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static File fileFor(Context c, long albumId) {
        return new File(dir(c), albumId + ".img");
    }

    public static boolean has(Context c, long albumId) {
        return fileFor(c, albumId).length() > 0;
    }

    /** Writes via a temp file and renames, so a half-downloaded image is never shown. */
    public static void save(Context c, long albumId, byte[] data) throws IOException {
        File target = fileFor(c, albumId);
        File tmp = new File(target.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(data);
        }
        if (target.exists()) target.delete();
        if (!tmp.renameTo(target)) {
            tmp.delete();
            throw new IOException("Could not store artwork");
        }
        prefs(c).edit().remove("miss_" + albumId).apply();
    }

    /** Removes the override so the original (device) artwork shows again. */
    public static void reset(Context c, long albumId) {
        fileFor(c, albumId).delete();
        prefs(c).edit().remove("miss_" + albumId).apply();
    }

    public static void markMiss(Context c, long albumId) {
        prefs(c).edit().putLong("miss_" + albumId, System.currentTimeMillis()).apply();
    }

    public static boolean isRecentMiss(Context c, long albumId) {
        long t = prefs(c).getLong("miss_" + albumId, 0L);
        return t > 0 && System.currentTimeMillis() - t < MISS_TTL_MS;
    }

    public static boolean isAutoEnabled(Context c) {
        return prefs(c).getBoolean(KEY_AUTO, false);
    }

    public static void setAutoEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean(KEY_AUTO, enabled).apply();
    }
}
