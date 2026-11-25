package com.meccael.tonal.lyrics;

import android.content.Context;
import android.content.SharedPreferences;

/** Remembers a per-track lyrics delay (milliseconds; positive shows lyrics later). */
public final class LyricsTimingStore {
    private static final String PREFS = "tonal_lyrics_timing";
    public static final long LIMIT_MS = 30000L;

    private LyricsTimingStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static long get(Context c, long trackId) {
        return prefs(c).getLong("d_" + trackId, 0L);
    }

    public static void set(Context c, long trackId, long delayMs) {
        long clamped = Math.max(-LIMIT_MS, Math.min(LIMIT_MS, delayMs));
        SharedPreferences.Editor e = prefs(c).edit();
        if (clamped == 0L) {
            e.remove("d_" + trackId);
        } else {
            e.putLong("d_" + trackId, clamped);
        }
        e.apply();
    }
}
