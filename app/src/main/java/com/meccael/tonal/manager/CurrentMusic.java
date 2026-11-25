package com.meccael.tonal.manager;

import android.content.Context;
import android.util.Log;
import com.meccael.tonal.application.Preferences;

/* JADX INFO: loaded from: classes.dex */
public class CurrentMusic {
    private Music mMusic;
    private long mPlaylist;
    private int mPosition;

    public Music getMusic(Context context) {
        if (this.mMusic == null) {
            this.mMusic = new Preferences(context).getCurrentMusic();
        }
        return this.mMusic;
    }

    public long getPlaylistID() {
        return this.mPlaylist;
    }

    public int getPosition() {
        return this.mPosition;
    }

    public void newSong(Context context, Music music, int i, long j) {
        this.mMusic = music;
        this.mPosition = i;
        this.mPlaylist = j;
        new Preferences(context).saveCurrentMusic(music.getID(), music.getCover(), music.getDuration(), music.getDate(), music.getFile(), music.getTitle(), music.getArtist(), music.getAlbum());
        Log.i("TONAL", "New current music : " + music.getTitle() + " from " + music.getArtist());
    }

    public void setPosition(int i) {
        this.mPosition = i;
    }
}
