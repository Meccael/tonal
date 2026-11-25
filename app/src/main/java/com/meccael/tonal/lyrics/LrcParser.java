package com.meccael.tonal.lyrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses LRC text ("[mm:ss.xx] lyric") into time-sorted lines.
 *
 * Handles: several time tags on one line, 1-3 digit fractions, an [offset:+/-ms]
 * tag, metadata tags such as [ar:...] (ignored), enhanced word tags like
 * <00:12.34> (stripped) and a leading byte-order mark.
 */
public final class LrcParser {
    private static final Pattern TIME_TAG =
            Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]");
    private static final Pattern OFFSET_TAG =
            Pattern.compile("^\\[offset:\\s*([+-]?\\d+)\\s*\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORD_TAG =
            Pattern.compile("<\\d{1,3}:\\d{1,2}(?:[.:]\\d{1,3})?>");

    private LrcParser() {
    }

    public static List<LyricLine> parse(String lrc) {
        List<LyricLine> out = new ArrayList<LyricLine>();
        if (lrc == null || lrc.isEmpty()) {
            return out;
        }
        if (lrc.charAt(0) == '\uFEFF') {
            lrc = lrc.substring(1);
        }

        long offsetMs = 0;
        for (String raw : lrc.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher off = OFFSET_TAG.matcher(line);
            if (off.find()) {
                try {
                    offsetMs = Long.parseLong(off.group(1));
                } catch (NumberFormatException ignored) {
                    // leave the offset at its previous value
                }
                continue;
            }

            // Time tags must lead the line; anything else is metadata or plain text.
            Matcher m = TIME_TAG.matcher(line);
            List<Long> times = new ArrayList<Long>();
            int end = 0;
            while (m.find() && m.start() == end) {
                times.add(toMs(m.group(1), m.group(2), m.group(3)));
                end = m.end();
            }
            if (times.isEmpty()) {
                continue;
            }

            String text = WORD_TAG.matcher(line.substring(end)).replaceAll("").trim();
            for (Long t : times) {
                out.add(new LyricLine(t, text));
            }
        }

        // A positive offset makes lyrics appear sooner, so subtract it.
        if (offsetMs != 0) {
            List<LyricLine> shifted = new ArrayList<LyricLine>(out.size());
            for (LyricLine l : out) {
                shifted.add(new LyricLine(Math.max(0L, l.timeMs - offsetMs), l.text));
            }
            out = shifted;
        }

        // Stable sort keeps the written order for lines sharing a timestamp.
        Collections.sort(out, new Comparator<LyricLine>() {
            @Override
            public int compare(LyricLine a, LyricLine b) {
                return a.timeMs < b.timeMs ? -1 : (a.timeMs == b.timeMs ? 0 : 1);
            }
        });
        return out;
    }

    private static long toMs(String min, String sec, String frac) {
        long ms = Long.parseLong(min) * 60000L + Long.parseLong(sec) * 1000L;
        if (frac != null) {
            int v = Integer.parseInt(frac);
            ms += frac.length() == 1 ? v * 100 : (frac.length() == 2 ? v * 10 : v);
        }
        return ms;
    }
}
