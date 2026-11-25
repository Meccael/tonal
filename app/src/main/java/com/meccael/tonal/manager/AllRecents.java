package com.meccael.tonal.manager;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.meccael.tonal.R;

/* JADX INFO: loaded from: classes.dex */
class AllRecents {
    private int pCount = 0;
    private int pDuration = 0;
    private long pCover = 0;
    private List<Music> pMusics = new ArrayList();
    private List<Playlist> pPlaylists = new ArrayList();

    AllRecents() {
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
        return 1L;
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
        return context.getString(R.string.musics_recents);
    }

    public long getPlaylistSize() {
        return this.pCount;
    }

    void manage() {
        if (this.pCount == 0) {
            return;
        }
        if (this.pCount < 2) {
            this.pCover = this.pMusics.get(0).getCover();
            return;
        }
        Collections.sort(this.pMusics, new Comparator<Music>() { // from class: com.meccael.tonal.manager.AllRecents.1
            @Override // java.util.Comparator
            public int compare(Music music, Music music2) {
                if (music2.getDate() > music.getDate()) {
                    return 1;
                }
                return music2.getDate() < music.getDate() ? -1 : 0;
            }
        });
        if (this.pCount > 30) {
            for (int i = 30; i < getPlaylistSize(); i++) {
                this.pDuration = (int) (((long) this.pDuration) - this.pMusics.get(i).getDuration());
            }
            this.pMusics.subList(this.pCount - (this.pCount - 30), this.pCount).clear();
            this.pCount = 30;
        }
        this.pCover = this.pMusics.get(0).getCover();
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
