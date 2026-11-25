package com.meccael.tonal.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;

/** Remembers the equalizer settings between sessions: power, preset, band levels, bass and preamp. */
public final class EqualizerStore {
    private static final String PREFS = "tonal_eq";

    private EqualizerStore() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context c) { return p(c).getBoolean("enabled", true); }
    public static void setEnabled(Context c, boolean v) { p(c).edit().putBoolean("enabled", v).apply(); }

    /** Bass boost strength, 0..1000. */
    public static int bass(Context c) { return p(c).getInt("bass", 0); }
    public static void setBass(Context c, int v) { p(c).edit().putInt("bass", v).apply(); }

    /** Preamp gain in millibels, 0..PREAMP_MAX_MB. */
    public static final int PREAMP_MAX_MB = 1500;
    public static int preamp(Context c) { return p(c).getInt("preamp", 0); }
    public static void setPreamp(Context c, int v) { p(c).edit().putInt("preamp", v).apply(); }

    /** Selected preset index, or -1 for custom. */
    public static int preset(Context c) { return p(c).getInt("preset", -1); }
    public static void setPreset(Context c, int v) { p(c).edit().putInt("preset", v).apply(); }

    public static void saveLevels(Context c, short[] levels) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < levels.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(levels[i]);
        }
        p(c).edit().putString("levels", sb.toString()).apply();
    }

    /** Saved band levels, or null if none were saved for this many bands. */
    public static short[] levels(Context c, int bands) {
        String s = p(c).getString("levels", null);
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(",");
        if (parts.length != bands) return null;
        short[] out = new short[bands];
        try {
            for (int i = 0; i < bands; i++) out[i] = Short.parseShort(parts[i].trim());
        } catch (NumberFormatException e) {
            return null;
        }
        return out;
    }

    /** Puts the saved power state and band levels onto a freshly created Equalizer. */
    public static void apply(Context c, Equalizer eq) {
        try {
            eq.setEnabled(enabled(c));
            short[] lv = levels(c, eq.getNumberOfBands());
            if (lv != null) {
                short[] range = eq.getBandLevelRange();
                for (short i = 0; i < lv.length; i++) {
                    short v = (short) Math.max(range[0], Math.min(range[1], lv[i]));
                    eq.setBandLevel(i, v);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
