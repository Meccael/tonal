package com.meccael.tonal.manager;

import android.util.Log;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import com.meccael.tonal.application.Fragment_Playlist;
import com.meccael.tonal.application.ServiceMusic;

/* JADX INFO: loaded from: classes.dex */
public class CurrentPlaylist {
    private long pCount;
    private long pCover;
    private long pDuration;
    private long pID;
    private List<Music> pItems = new ArrayList();
    private String pName;

    public void addPlaylist(List<Music> list) {
        for (int i = 0; i < list.size(); i++) {
            this.pItems.add(list.get(i));
            this.pDuration += list.get(i).getDuration();
            this.pCount++;
        }
    }

    public void addSongToEnd(Music music) {
        Log.i("TONAL", "Add song to the end of the playlist.");
        this.pItems.add(music);
        this.pDuration += music.getDuration();
        this.pCount++;
    }

    public void addSongToNext(Music music, ServiceMusic serviceMusic) {
        Log.i("TONAL", "Add song in the playlist.");
        this.pItems.add(serviceMusic.musicPlayed.getPosition() + 1, music);
        this.pDuration += music.getDuration();
        this.pCount++;
    }

    public List<Music> getPlaylist() {
        return this.pItems;
    }

    public long getPlaylistCover() {
        return this.pCover;
    }

    public long getPlaylistDuration() {
        return this.pDuration;
    }

    public long getPlaylistID() {
        return this.pID;
    }

    public String getPlaylistName() {
        return this.pName;
    }

    public long getPlaylistSize() {
        return this.pCount;
    }

    public Music getSong(int i) {
        return this.pItems.get(i);
    }

    public void newPlaylist(String str, long j, long j2, long j3, long j4, List<Music> list) {
        this.pName = str;
        this.pID = j;
        this.pCover = j2;
        this.pCount = j3;
        this.pDuration = j4;
        this.pItems = list;
        Log.i("TONAL", "New current playlist : ID " + j + " called " + str);
    }

    public void removeSong(int i, Fragment_Playlist fragment_Playlist) {
        Log.i("TONAL", "Remove a song from the current playlist.");
        this.pDuration -= this.pItems.get(i).getDuration();
        this.pCount--;
        fragment_Playlist.refreshPlaylitsInformations();
    }

    public void setPlaylistName(String str, TextView textView) {
        this.pName = str;
        this.pID = -1L;
        if (textView != null) {
            textView.setText(str);
        }
    }
}
