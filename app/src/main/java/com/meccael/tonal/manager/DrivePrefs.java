package com.meccael.tonal.manager;

import android.content.Context;
import android.content.SharedPreferences;

/** The three drive switches under Settings, Drive. */
public final class DrivePrefs {
    private DrivePrefs() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("tonal_usb", Context.MODE_PRIVATE);
    }

    /** Whether songs from drives appear in the library. On by default. */
    public static boolean showSongs(Context c) {
        return prefs(c).getBoolean("show_songs", true);
    }

    public static void setShowSongs(Context c, boolean v) {
        prefs(c).edit().putBoolean("show_songs", v).apply();
    }

    /** Whether Tonal shows a short message when a drive is connected or removed. Off by default. */
    public static boolean announce(Context c) {
        return prefs(c).getBoolean("announce", false);
    }

    public static void setAnnounce(Context c, boolean v) {
        prefs(c).edit().putBoolean("announce", v).apply();
    }

    /** Whether playback pauses when the drive it plays from is removed. On by default. */
    public static boolean pauseOnUnplug(Context c) {
        return prefs(c).getBoolean("pause_unplug", true);
    }

    public static void setPauseOnUnplug(Context c, boolean v) {
        prefs(c).edit().putBoolean("pause_unplug", v).apply();
    }
}
