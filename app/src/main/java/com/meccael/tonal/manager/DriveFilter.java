package com.meccael.tonal.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lets the person choose which top-level folders of a drive that Android mounted hold their music.
 * Songs from other folders on that drive are left out of Tonal's library. A drive with no choice
 * saved is shown in full.
 */
public final class DriveFilter {
    private static final String PREFS = "tonal_usb";
    private static final String KEY = "mounted_folders";

    private DriveFilter() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Saved choices, each written as "volume/folder". */
    public static Set<String> chosen(Context c) {
        return new HashSet<>(prefs(c).getStringSet(KEY, new HashSet<String>()));
    }

    public static void save(Context c, Set<String> entries) {
        prefs(c).edit().putStringSet(KEY, new HashSet<>(entries)).apply();
    }

    /** Splits a drive path into {volume, topFolder}; topFolder is null for a file at the drive's root. */
    private static String[] split(String path) {
        if (path == null) {
            return null;
        }
        String rest;
        if (path.startsWith("/storage/")) {
            rest = path.substring("/storage/".length());
        } else if (path.startsWith("/mnt/media_rw/")) {
            rest = path.substring("/mnt/media_rw/".length());
        } else {
            return null;
        }
        String[] parts = rest.split("/");
        if (parts.length < 2) {
            return null;
        }
        String volume = parts[0];
        if (volume.equals("emulated") || volume.equals("self")) {
            return null;
        }
        return new String[]{volume, parts.length > 2 ? parts[1] : null};
    }

    /** True when this song sits on a drive whose folder choice leaves its folder out. */
    public static boolean isHidden(Context c, String path, Set<String> chosen) {
        String[] p = split(path);
        if (p != null && !DrivePrefs.showSongs(c)) {
            return true;
        }
        if (chosen.isEmpty()) {
            return false;
        }
        if (p == null || p[1] == null) {
            return false;
        }
        boolean volumeFiltered = false;
        String prefix = p[0] + "/";
        for (String entry : chosen) {
            if (entry.startsWith(prefix)) {
                volumeFiltered = true;
                break;
            }
        }
        return volumeFiltered && !chosen.contains(p[0] + "/" + p[1]);
    }

    /** Every top-level folder on mounted drives that holds songs, as "volume/folder" entries. */
    public static List<String> topFolders(Context c) {
        TreeSet<String> out = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Cursor cursor = null;
        try {
            cursor = c.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_data"}, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String[] p = split(cursor.getString(0));
                    if (p != null && p[1] != null) {
                        out.add(p[0] + "/" + p[1]);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return new ArrayList<>(out);
    }
}
