package com.meccael.tonal.manager;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.meccael.tonal.R;

/* JADX INFO: loaded from: classes.dex */
class AllAlbums {
    private int pCount = 0;
    private long pCover = 0;
    private List<Playlist> pPlaylists = new ArrayList();

    AllAlbums() {
    }

    private boolean existSimilarPlaylist(String str) {
        for (Playlist playlist : this.pPlaylists) {
            if (playlist.getPlaylistName() != null && playlist.getPlaylistName().equals(str)) {
                return true;
            }
        }
        return false;
    }

    private String getName(int i) {
        return this.pPlaylists.get(i).getPlaylistName();
    }

    private int searchForPlaylist(String str) {
        for (int i = 0; i < this.pCount; i++) {
            String playlistName = this.pPlaylists.get(i).getPlaylistName();
            if (playlistName != null && playlistName.equals(str)) {
                return i;
            }
        }
        return 0;
    }

    void addPlaylist(String str, long j, long j2, long j3, Music music) {
        if (this.pCount != 0 && existSimilarPlaylist(str)) {
            this.pPlaylists.get(searchForPlaylist(str)).addMusicToPlaylist(music, j3);
        } else {
            this.pPlaylists.add(new Playlist(str, j, j2, j3, music));
            this.pCount++;
        }
    }

    public long getCover(int i) {
        return this.pPlaylists.get(i).getPlaylistCover();
    }

    public long getDuration(int i) {
        return this.pPlaylists.get(i).getPlaylistDuration();
    }

    public long getID(int i) {
        return this.pPlaylists.get(i).getPlaylistID();
    }

    long getPlaylistCover() {
        return this.pCover;
    }

    public long getPlaylistDuration() {
        int duration = 0;
        for (int i = 0; i < this.pCount; i++) {
            duration = (int) (((long) duration) + getDuration(i));
        }
        return duration;
    }

    public long getPlaylistID() {
        return 2L;
    }

    List<Playlist> getPlaylistList() {
        return this.pPlaylists;
    }

    public String getPlaylistName(Context context) {
        return context.getString(R.string.musics_albums);
    }

    public long getPlaylistSize() {
        return this.pCount;
    }

    public long getSize(int i) {
        return this.pPlaylists.get(i).getPlaylistSize();
    }

    void manage(Context context) {
        if (this.pCount == 0) {
            return;
        }
        if (this.pCount < 2) {
            this.pCover = getCover(0);
            return;
        }
        boolean z = false;
        int i = 0;
        while (i < this.pCount) {
            if (this.pPlaylists.get(i).getPlaylistSize() == 1 && !this.pPlaylists.get(i).getPlaylistName().equals(context.getString(R.string.others))) {
                if (z) {
                    this.pCount--;
                    this.pPlaylists.get(this.pCount).addMusicToPlaylist(this.pPlaylists.get(i).getPlaylistMusics().get(0), this.pPlaylists.get(i).getPlaylistDuration());
                    this.pPlaylists.remove(i);
                } else {
                    this.pPlaylists.add(new Playlist(context.getString(R.string.others), Integer.valueOf(String.valueOf(2) + String.valueOf(i)).intValue(), this.pPlaylists.get(i).getPlaylistMusics().get(0).getCover(), this.pPlaylists.get(i).getPlaylistMusics().get(0).getDuration(), this.pPlaylists.get(i).getPlaylistMusics().get(0)));
                    this.pPlaylists.remove(i);
                }
                z = true;
                i = 0;
            }
            i++;
        }
        Collections.sort(this.pPlaylists, new Comparator<Playlist>() { // from class: com.meccael.tonal.manager.AllAlbums.1
            @Override // java.util.Comparator
            public int compare(Playlist playlist, Playlist playlist2) {
                return playlist.getPlaylistName().compareToIgnoreCase(playlist2.getPlaylistName());
            }
        });
        this.pCover = getCover(0);
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
        for (int i = 0; i < this.pCount; i++) {
            Log.i(str, " ");
            Log.i(str, "Name    : " + getName(i));
            Log.i(str, "ID      : " + getID(i));
            Log.i(str, "Cover   : " + getCover(i));
            Log.i(str, "Time    : " + getDuration(i));
            Log.i(str, "Items   : " + getSize(i));
            for (int i2 = 0; i2 < getSize(i); i2++) {
                Log.i(str, "Song    : " + this.pPlaylists.get(i).getPlaylistMusics().get(i2).getTitle());
            }
        }
    }
}
