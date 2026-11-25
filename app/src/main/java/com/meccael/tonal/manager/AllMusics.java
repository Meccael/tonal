package com.meccael.tonal.manager;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.meccael.tonal.R;
import com.meccael.tonal.application.Preferences;

/* JADX INFO: loaded from: classes.dex */
class AllMusics {
    private int pCount = 0;
    private int pDuration = 0;
    private long pCover = 0;
    private List<Music> pMusics = new ArrayList();
    private List<Playlist> pPlaylists = new ArrayList();

    AllMusics() {
    }

    void addSong(Music music) {
        this.pMusics.add(music);
        this.pDuration = (int) (((long) this.pDuration) + music.getDuration());
        this.pCount++;
    }

    long getPlaylistCover() {
        return this.pCover;
    }

    public long getPlaylistDuration() {
        return this.pDuration;
    }

    public long getPlaylistID() {
        return 0L;
    }

    List<Playlist> getPlaylistList(Context context) {
        if (this.pMusics != null && this.pMusics.size() > 0) {
            this.pPlaylists.add(new Playlist(getPlaylistName(context), getPlaylistID(), getPlaylistCover(), getPlaylistDuration(), this.pMusics.get(0)));
            if (this.pMusics.size() == 1) {
                return this.pPlaylists;
            }
            for (int i = 1; i < this.pMusics.size(); i++) {
                this.pPlaylists.get(0).addMusicToPlaylist(this.pMusics.get(i), this.pMusics.get(i).getDuration());
            }
        }
        return this.pPlaylists;
    }

    public String getPlaylistName(Context context) {
        return context.getString(R.string.musics_all);
    }

    public long getPlaylistSize() {
        return this.pCount;
    }

    void manage(final Preferences preferences) {
        if (this.pCount == 0) {
            return;
        }
        if (this.pCount < 2) {
            this.pMusics.get(0).getCover();
        } else {
            Collections.sort(this.pMusics, new Comparator<Music>() { // from class: com.meccael.tonal.manager.AllMusics.1
                @Override // java.util.Comparator
                public int compare(Music music, Music music2) {
                    int i = preferences.getItemsSortAsc() ? 1 : -1;
                    if (preferences.getItemsSortBy().equals("title")) {
                        return i * music2.getTitle().compareToIgnoreCase(music.getTitle());
                    }
                    if (preferences.getItemsSortBy().equals("artist")) {
                        return i * music2.getArtist().compareToIgnoreCase(music.getArtist());
                    }
                    if (!preferences.getItemsSortBy().equals("date_modified")) {
                        return 1;
                    }
                    if (music2.getDate() > music.getDate()) {
                        return -i;
                    }
                    if (music2.getDate() >= music.getDate()) {
                        return 0;
                    }
                    return i;
                }
            });
            this.pCover = this.pMusics.get(0).getCover();
        }
    }

    void showDatas(Context context, String str) {
        if (this.pCount == 0) {
            Log.i(str, "-------------------------------");
            Log.i(str, "Empty playlist.");
            return;
        }
        Log.i(str, "-------------------------------");
        Log.i(str, "Name    : " + getPlaylistName(context));
        Log.i(str, "ID      : " + getPlaylistID());
        Log.i(str, "Cover   : " + getPlaylistCover());
        Log.i(str, "Time    : " + getPlaylistDuration() + " sec");
        Log.i(str, "Items   : " + getPlaylistSize());
        Log.i(str, " ");
        for (int i = 0; i < getPlaylistSize(); i++) {
            Log.i(str, "Song    : " + this.pMusics.get(i).getTitle());
        }
    }
}
