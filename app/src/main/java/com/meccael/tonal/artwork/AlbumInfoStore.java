package com.meccael.tonal.artwork;

import android.content.Context;
import android.content.SharedPreferences;

/** Year and genre found online for an album, kept in the app (never written into the audio files). */
public final class AlbumInfoStore {
    private static final String PREFS = "tonal_albuminfo";
    private static final long MISS_TTL_MS = 30L * 24 * 60 * 60 * 1000;

    private AlbumInfoStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Returns {year, genre} (either may be empty), or null if nothing is stored for this album. */
    public static String[] get(Context c, long albumId) {
        String v = prefs(c).getString("info_" + albumId, null);
        if (v == null) return null;
        String[] parts = v.split("\t", -1);
        return new String[]{parts.length > 0 ? parts[0] : "", parts.length > 1 ? parts[1] : ""};
    }

    public static void save(Context c, long albumId, String year, String genre) {
        prefs(c).edit()
                .putString("info_" + albumId, (year == null ? "" : year) + "\t" + (genre == null ? "" : genre))
                .remove("miss_" + albumId)
                .apply();
    }

    public static void markMiss(Context c, long albumId) {
        prefs(c).edit().putLong("miss_" + albumId, System.currentTimeMillis()).apply();
    }

    /** True if nothing is stored yet and a lookup has not recently come up empty. */
    public static boolean needsLookup(Context c, long albumId) {
        SharedPreferences p = prefs(c);
        if (p.contains("info_" + albumId)) return false;
        long t = p.getLong("miss_" + albumId, 0L);
        return !(t > 0 && System.currentTimeMillis() - t < MISS_TTL_MS);
    }
}
