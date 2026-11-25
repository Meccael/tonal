package com.meccael.tonal.manager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class Playlist implements Serializable {
    private long pCover;
    private long pDuration;
    private long pID;
    private List<Music> pMusics;
    private String pName;
    private long pSize;

    public Playlist(String str, long j, long j2, long j3, List<Music> list) {
        this.pID = 0L;
        this.pCover = 0L;
        this.pSize = 0L;
        this.pDuration = 0L;
        this.pMusics = new ArrayList();
        this.pName = str;
        this.pID = j;
        this.pCover = j2;
        this.pSize = 0L;
        for (int i = 0; i < list.size(); i++) {
            this.pSize++;
        }
        this.pDuration = j3;
        this.pMusics = new ArrayList(list);
    }

    public Playlist(String str, long j, long j2, long j3, Music music) {
        this.pID = 0L;
        this.pCover = 0L;
        this.pSize = 0L;
        this.pDuration = 0L;
        this.pMusics = new ArrayList();
        this.pName = str;
        this.pID = j;
        this.pCover = j2;
        this.pSize = 1L;
        this.pDuration = j3;
        this.pMusics.add(music);
    }

    void addMusicToPlaylist(Music music, long j) {
        this.pMusics.add(music);
        this.pDuration += j;
        this.pSize++;
    }

    void changeName(String str) {
        this.pName = str;
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

    public List<Music> getPlaylistMusics() {
        return this.pMusics;
    }

    public String getPlaylistName() {
        return this.pName;
    }

    public long getPlaylistSize() {
        return this.pSize;
    }
}
