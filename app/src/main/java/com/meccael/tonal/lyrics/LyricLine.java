package com.meccael.tonal.lyrics;

/** One timed lyric line. */
public final class LyricLine {
    public final long timeMs;
    public final String text;

    public LyricLine(long timeMs, String text) {
        this.timeMs = timeMs;
        this.text = text == null ? "" : text;
    }
}
