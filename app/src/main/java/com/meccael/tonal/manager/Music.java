package com.meccael.tonal.manager;

import java.io.Serializable;

/**
 * One song in the library. Plain data holder; the field names are kept as they are because the
 * cached library on disk is stored with Java serialization.
 */
public class Music implements Serializable {
    private String mAlbum;
    private String mArtist;
    private long mCover;
    private long mDate;
    private long mDuration;
    private String mFile;
    private long mID;
    private String mTitle;

    public Music() {
    }

    /** Argument order: id, cover id, duration, date added, file path, title, artist, album. */
    public Music(long id, long cover, long duration, long date, String file, String title, String artist, String album) {
        mID = id;
        mCover = cover;
        mDuration = duration;
        mDate = date;
        mFile = file;
        mTitle = title;
        mArtist = artist;
        mAlbum = album;
    }

    public long getID() { return mID; }
    public void setID(long id) { mID = id; }

    public String getTitle() { return mTitle; }
    public void setTitle(String title) { mTitle = title; }

    public String getArtist() { return mArtist; }
    public void setArtist(String artist) { mArtist = artist; }

    public String getAlbum() { return mAlbum; }
    public void setAlbum(String album) { mAlbum = album; }

    public long getCover() { return mCover; }
    public void setCover(long cover) { mCover = cover; }

    public long getDuration() { return mDuration; }
    public void setDuration(long duration) { mDuration = duration; }

    /** Date added, in seconds. */
    public long getDate() { return mDate; }
    public void setDate(long date) { mDate = date; }

    public String getFile() { return mFile; }
    public void setFile(String file) { mFile = file; }

    /** True when the file sits on a USB drive or SD card instead of the phone's own storage. */
    public boolean isOnDrive() {
        return DriveWatcher.isDrivePath(mFile);
    }

    public Music getMusic() {
        return this;
    }
}
