package com.meccael.tonal.manager;

import java.io.Serializable;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class Category implements Serializable {
    private long mCount;
    private long mCover;
    private int mDefaultCover;
    private long mDuration;
    private List<Playlist> mPlaylists;
    private transient List<Playlist> mOriginal;
    public static final int SORT_ALL = 0;
    public static final int SORT_NEW = 1;
    public static final int SORT_AZ = 2;
    private String mTitle;

    Category(String str, long j, int i, long j2, long j3, List<Playlist> list) {
        this.mTitle = str;
        this.mCover = j;
        this.mDefaultCover = i;
        this.mCount = j2;
        this.mDuration = j3;
        this.mPlaylists = list;
    }

    /** Re-orders the list in place (so positions stay valid everywhere). SORT_ALL restores the original order. */
    public void sortBy(int mode) {
        if (mPlaylists == null) return;
        if (mOriginal == null || mOriginal.size() != mPlaylists.size()) {
            mOriginal = new ArrayList<>(mPlaylists);
        }
        List<Playlist> out = new ArrayList<>(mOriginal);
        if (mode == SORT_NEW) {
            final Map<Playlist, Long> newest = new HashMap<>();
            for (Playlist p : out) {
                long best = 0;
                for (Music m : p.getPlaylistMusics()) best = Math.max(best, m.getDate());
                newest.put(p, best);
            }
            Collections.sort(out, new Comparator<Playlist>() {
                @Override
                public int compare(Playlist a, Playlist b) {
                    return Long.compare(newest.get(b), newest.get(a));
                }
            });
        } else if (mode == SORT_AZ) {
            final Collator collator = Collator.getInstance();
            Collections.sort(out, new Comparator<Playlist>() {
                @Override
                public int compare(Playlist a, Playlist b) {
                    return collator.compare(sortName(a.getPlaylistName()), sortName(b.getPlaylistName()));
                }
            });
        }
        try {
            mPlaylists.clear();
            mPlaylists.addAll(out);
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private static String sortName(String name) {
        String n = name == null ? "" : name.trim();
        if (n.regionMatches(true, 0, "the ", 0, 4)) n = n.substring(4);
        return n;
    }

    /** The letter an item files under in an A-Z list; anything that is not a letter files under '#'. */
    public static char letterOf(String name) {
        String n = sortName(name);
        if (n.isEmpty()) return '#';
        String base = Normalizer.normalize(n.substring(0, 1), Normalizer.Form.NFD);
        char c = Character.toUpperCase(base.charAt(0));
        return Character.isLetter(c) ? c : '#';
    }

    public long getCover() {
        return this.mCover;
    }

    public int getDefaultCover() {
        return this.mDefaultCover;
    }

    public long getDuration() {
        return this.mDuration;
    }

    public List<Playlist> getPlaylists() {
        return this.mPlaylists;
    }

    public long getSize() {
        return this.mCount;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public void setCover(long j) {
        this.mCover = j;
    }
}
