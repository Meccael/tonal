package com.meccael.tonal.manager;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.meccael.tonal.R;

/* JADX INFO: loaded from: classes.dex */
public class AllPlaylists {
    private int pCount = 0;
    private long pCover = 0;
    private List<Playlist> pPlaylists = new ArrayList();

    private String getName(int i) {
        return this.pPlaylists.get(i).getPlaylistName();
    }

    void addPlaylist(Playlist playlist) {
        this.pPlaylists.add(playlist);
        this.pCount++;
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
        return 6L;
    }

    List<Playlist> getPlaylistList() {
        return this.pPlaylists;
    }

    public String getPlaylistName(Context context) {
        return context.getString(R.string.musics_playlists);
    }

    public long getPlaylistSize() {
        return this.pCount;
    }

    public long getSize(int i) {
        return this.pPlaylists.get(i).getPlaylistSize();
    }

    void manage() {
        if (this.pCount == 0) {
            return;
        }
        if (this.pCount < 2) {
            this.pCover = getCover(0);
        } else {
            Collections.sort(this.pPlaylists, new Comparator<Playlist>() { // from class: com.meccael.tonal.manager.AllPlaylists.1
                @Override // java.util.Comparator
                public int compare(Playlist playlist, Playlist playlist2) {
                    return playlist.getPlaylistName().compareToIgnoreCase(playlist2.getPlaylistName());
                }
            });
            this.pCover = getCover(0);
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
