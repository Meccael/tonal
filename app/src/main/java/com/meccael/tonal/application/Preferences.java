package com.meccael.tonal.application;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;
import com.meccael.tonal.manager.Music;

/**
 * All of the app's saved settings and the last-played song, in one place. The key names are the
 * ones already on people's phones, so they must not change.
 */
public class Preferences {
    // Last played song
    private static final String K_ID = "musicID";
    private static final String K_COVER = "musicCover";
    private static final String K_DURATION = "musicDuration";
    private static final String K_DATE = "musicDate";
    private static final String K_FILE = "musicFile";
    private static final String K_TITLE = "musicTitle";
    private static final String K_ARTIST = "musicArtist";
    private static final String K_ALBUM = "musicAlbum";
    // Player
    private static final String K_LOOP = "player_loop";
    private static final String K_BOLT = "settings_pl_bolt";
    private static final String K_HEADSET = "settings_pl_headset";
    private static final String K_EXCLUDE = "settings_pl_exclude_time";
    private static final String K_STOP = "settings_pl_stop_time";
    // Lists
    private static final String K_SORT_ASC = "items_sort_asc";
    private static final String K_SORT_BY = "items_sort_by";
    // Interface
    private static final String K_VIBRATION = "settings_ui_vibration";
    private static final String K_SCREEN_ON = "settings_ui_screen_on";
    private static final String K_BLUR = "settings_ui_blur_force";
    // Notification
    private static final String K_NOTE_EXPAND = "settings_no_expandable";
    private static final String K_NOTE_SWIPE = "settings_no_swipe";
    private static final String K_NOTE_CLOSE = "settings_no_close";
    private static final String K_NOTE_COLOR = "settings_no_color";
    private static final String K_NOTE_WHITE = "settings_no_black";
    // Lock screen
    private static final String K_LOCK_BACKGROUND = "settings_lo_background";
    private static final String K_LOCK_BLUR = "settings_lo_blur";
    // Lyrics
    private static final String K_LYRICS_SHOW = "settings_ly_show";
    private static final String K_LYRICS_ONLINE = "settings_ly_online";
    private static final String K_LYRICS_TREE = "settings_ly_tree";
    // Misc
    private static final String K_EVOLUTION = "launchEvolution";
    private static final String K_FAVORITES = "favorite_songs";

    private final SharedPreferences prefs;

    public Preferences(Context context) {
        prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    private boolean flag(String key, boolean fallback) {
        return prefs.getBoolean(key, fallback);
    }

    private void putFlag(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    // ---- Last played song -------------------------------------------------------------------

    public Music getCurrentMusic() {
        return new Music(
                prefs.getLong(K_ID, 0L), prefs.getLong(K_COVER, 0L), prefs.getLong(K_DURATION, 0L), prefs.getLong(K_DATE, 0L),
                prefs.getString(K_FILE, null), prefs.getString(K_TITLE, null), prefs.getString(K_ARTIST, null), prefs.getString(K_ALBUM, null));
    }

    public void saveCurrentMusic(long id, long cover, long duration, long date, String file, String title, String artist, String album) {
        prefs.edit()
                .putLong(K_ID, id).putLong(K_COVER, cover).putLong(K_DURATION, duration).putLong(K_DATE, date)
                .putString(K_FILE, file).putString(K_TITLE, title).putString(K_ARTIST, artist).putString(K_ALBUM, album)
                .apply();
    }

    public String getSaveMusicFilename(String fallback) {
        return prefs.getString(K_FILE, fallback);
    }

    public long getSaveMusicID() {
        return prefs.getLong(K_ID, 0L);
    }

    // ---- Misc -------------------------------------------------------------------------------

    public long getEvolution() { return prefs.getLong(K_EVOLUTION, 0L); }
    public void setEvolution(long value) { prefs.edit().putLong(K_EVOLUTION, value).apply(); }

    public int getPlayerLoop() { return prefs.getInt(K_LOOP, 1); }
    public void setPlayerLoop(int value) { putInt(K_LOOP, value); }

    public boolean getItemsSortAsc() { return flag(K_SORT_ASC, false); }
    public void setItemsSortAsc(boolean value) { putFlag(K_SORT_ASC, value); }

    public String getItemsSortBy() { return prefs.getString(K_SORT_BY, "title"); }
    public void setItemsSortBy(String value) { prefs.edit().putString(K_SORT_BY, value).apply(); }

    // ---- Player settings --------------------------------------------------------------------

    public boolean getSettingsPlayerBolt() { return flag(K_BOLT, true); }
    public void setSettingsPlayerBolt(boolean value) { putFlag(K_BOLT, value); }

    public boolean getSettingsPlayerHeadset() { return flag(K_HEADSET, true); }
    public void setSettingsPlayerHeadset(boolean value) { putFlag(K_HEADSET, value); }

    public int getSettingsPlayerExcludeMusic() { return prefs.getInt(K_EXCLUDE, 15); }
    public void setSettingsPlayerExcludeMusic(int value) { putInt(K_EXCLUDE, value); }

    public int getSettingsPlayerStopMusic() { return prefs.getInt(K_STOP, 0); }
    public void setSettingsPlayerStopMusic(int value) { putInt(K_STOP, value); }

    // ---- Interface settings -----------------------------------------------------------------

    public boolean getSettingsUIVibration() { return flag(K_VIBRATION, true); }
    public void setSettingsUIVibration(boolean value) { putFlag(K_VIBRATION, value); }

    public boolean getSettingsUIScreenOn() { return flag(K_SCREEN_ON, false); }
    public void setSettingsUIScreenOn(boolean value) { putFlag(K_SCREEN_ON, value); }

    public int getSettingsUIBlurIntensity() { return prefs.getInt(K_BLUR, 10); }
    public void setSettingsUIBlurIntensity(int value) { putInt(K_BLUR, value); }

    // ---- Notification settings --------------------------------------------------------------

    public boolean getSettingsNotificationExpandable() { return flag(K_NOTE_EXPAND, true); }
    public void setSettingsNotificationExpandable(boolean value) { putFlag(K_NOTE_EXPAND, value); }

    public boolean getSettingsNotificationSwipeable() { return flag(K_NOTE_SWIPE, false); }
    public void setSettingsNotificationSwipeable(boolean value) { putFlag(K_NOTE_SWIPE, value); }

    public boolean getSettingsNotificationClose() { return flag(K_NOTE_CLOSE, true); }
    public void setSettingsNotificationClose(boolean value) { putFlag(K_NOTE_CLOSE, value); }

    public boolean getSettingsNotificationColored(boolean fallback) { return flag(K_NOTE_COLOR, fallback); }
    public void setSettingsNotificationColored(boolean value) { putFlag(K_NOTE_COLOR, value); }

    public boolean getSettingsNotificationWhite(boolean fallback) { return flag(K_NOTE_WHITE, fallback); }
    public void setSettingsNotificationWhite(boolean value) { putFlag(K_NOTE_WHITE, value); }

    // ---- Lock screen settings ---------------------------------------------------------------

    public boolean getSettingsLockscreenBackground() { return flag(K_LOCK_BACKGROUND, true); }
    public void setSettingsLockscreenBackground(boolean value) { putFlag(K_LOCK_BACKGROUND, value); }

    public boolean getSettingsLockscreenBlur() { return flag(K_LOCK_BLUR, false); }
    public void setSettingsLockscreenBlur(boolean value) { putFlag(K_LOCK_BLUR, value); }

    // ---- Lyrics settings --------------------------------------------------------------------

    public boolean getSettingsLyricsShow() { return flag(K_LYRICS_SHOW, true); }
    public void setSettingsLyricsShow(boolean value) { putFlag(K_LYRICS_SHOW, value); }

    public boolean getSettingsLyricsOnline() { return flag(K_LYRICS_ONLINE, true); }
    public void setSettingsLyricsOnline(boolean value) { putFlag(K_LYRICS_ONLINE, value); }

    public String getLyricsTreeUri() { return prefs.getString(K_LYRICS_TREE, null); }

    public void setLyricsTreeUri(String uri) {
        SharedPreferences.Editor editor = prefs.edit();
        if (uri == null) {
            editor.remove(K_LYRICS_TREE);
        } else {
            editor.putString(K_LYRICS_TREE, uri);
        }
        editor.apply();
    }

    // ---- Favourites -------------------------------------------------------------------------

    private Set<String> favorites() {
        Set<String> stored = prefs.getStringSet(K_FAVORITES, null);
        return stored == null ? new HashSet<String>() : new HashSet<String>(stored);
    }

    public boolean isFavorite(long musicId) {
        return favorites().contains(String.valueOf(musicId));
    }

    public void toggleFavorite(long musicId) {
        Set<String> all = favorites();
        String id = String.valueOf(musicId);
        if (!all.remove(id)) {
            all.add(id);
        }
        prefs.edit().putStringSet(K_FAVORITES, all).apply();
    }
}
