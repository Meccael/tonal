package com.meccael.tonal.lyrics;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.meccael.tonal.application.Preferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Finds synced lyrics for a track, in this order:
 *   1. an .lrc file next to the audio file (same name),
 *   2. a copy saved earlier in the app's private storage,
 *   3. an online lookup on LRCLIB (https://lrclib.net), if enabled.
 *
 * All work happens on a background thread. Callbacks arrive on the main thread,
 * and only for the most recent load() call, so a slow lookup for a skipped song
 * can never overwrite the lyrics of the current one.
 */
public final class LyricsLoader {

    public interface Callback {
        /** Local sources found nothing and an online lookup has started. */
        void onSearching();

        /** source is "file", "cache" or "online". */
        void onLyrics(List<LyricLine> lines, String source);

        /** Nothing found (or the lookup failed). */
        void onNone();
    }

    private static final String API = "https://lrclib.net/api";
    private static final String USER_AGENT = "Tonal/1.0 (personal build)";
    private static final long MAX_FILE_BYTES = 512L * 1024L;
    private static final long MAX_DURATION_DIFF_SEC = 10L;

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger latest = new AtomicInteger();
    /** Tracks that LRCLIB definitely has no synced lyrics for (this session only). */
    private final Set<String> misses = Collections.synchronizedSet(new HashSet<String>());

    public LyricsLoader(Context context) {
        this.context = context.getApplicationContext();
    }

    /** The switch lives in the app's regular settings, shared with the Settings screen. */
    public static boolean isOnlineEnabled(Context c) {
        return new Preferences(c.getApplicationContext()).getSettingsLyricsOnline();
    }

    public static void setOnlineEnabled(Context c, boolean enabled) {
        new Preferences(c.getApplicationContext()).setSettingsLyricsOnline(enabled);
    }

    /** Ignore any result that is still on its way. */
    public void cancel() {
        latest.incrementAndGet();
    }

    public void shutdown() {
        latest.incrementAndGet();
        executor.shutdownNow();
    }

    /**
     * @param audioPath   full path of the audio file (may be null)
     * @param durationSec track length in seconds (0 if unknown)
     */
    public void load(final String audioPath, final String title, final String artist,
                     final String album, final long durationSec, final Callback callback) {
        final int ticket = latest.incrementAndGet();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                work(ticket, audioPath, title, artist, album, durationSec, callback);
            }
        });
    }

    // ---------------------------------------------------------------- worker

    private void work(int ticket, String audioPath, String title, String artist,
                      String album, long durationSec, Callback cb) {
        try {
            if (ticket != latest.get()) {
                return;
            }

            // 1. .lrc next to the song
            String sidecarText = readSidecar(audioPath);
            if (sidecarText != null) {
                List<LyricLine> lines = LrcParser.parse(sidecarText);
                if (!lines.isEmpty()) {
                    deliver(ticket, cb, lines, "file");
                    return;
                }
            }

            // 2. copy from an earlier online lookup
            String key = cacheKey(title, artist, album, durationSec);
            File cached = new File(cacheDir(), key + ".lrc");
            if (cached.isFile()) {
                List<LyricLine> lines = LrcParser.parse(readText(cached));
                if (!lines.isEmpty()) {
                    deliver(ticket, cb, lines, "cache");
                    return;
                }
            }

            // 3. online lookup
            if (isOnlineEnabled(context) && usable(title) && usable(artist)
                    && !misses.contains(key)) {
                postSearching(ticket, cb);
                String lrc = fetchOnline(title, artist, album, durationSec);
                if (lrc != null && !lrc.isEmpty()) {
                    List<LyricLine> lines = LrcParser.parse(lrc);
                    if (!lines.isEmpty()) {
                        writeText(cached, lrc);
                        deliver(ticket, cb, lines, "online");
                        return;
                    }
                }
                // A clean "not found" (network errors jump to the catch below instead).
                misses.add(key);
            }
        } catch (Exception e) {
            // Network, file or parsing problem: fall through and report "none".
        }
        deliverNone(ticket, cb);
    }

    private void deliver(final int ticket, final Callback cb,
                         final List<LyricLine> lines, final String source) {
        main.post(new Runnable() {
            @Override
            public void run() {
                if (ticket == latest.get()) {
                    cb.onLyrics(lines, source);
                }
            }
        });
    }

    private void deliverNone(final int ticket, final Callback cb) {
        main.post(new Runnable() {
            @Override
            public void run() {
                if (ticket == latest.get()) {
                    cb.onNone();
                }
            }
        });
    }

    private void postSearching(final int ticket, final Callback cb) {
        main.post(new Runnable() {
            @Override
            public void run() {
                if (ticket == latest.get()) {
                    cb.onSearching();
                }
            }
        });
    }

    // ---------------------------------------------------------------- local files

    /** Text of the .lrc beside the song: by path where Android allows it, else through the chosen lyrics folder. */
    private String readSidecar(String audioPath) {
        try {
            File f = findSidecar(audioPath);
            if (f != null) {
                return readText(f);
            }
        } catch (Exception e) {
            // fall through to the lyrics folder
        }
        try {
            return readFromTree(audioPath);
        } catch (Exception e) {
            return null;
        }
    }

    private String readFromTree(String audioPath) throws IOException {
        String tree = new Preferences(context).getLyricsTreeUri();
        if (tree == null || audioPath == null) {
            return null;
        }
        android.net.Uri treeUri = android.net.Uri.parse(tree);
        String treeId = android.provider.DocumentsContract.getTreeDocumentId(treeUri);
        int colon = treeId.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String volume = treeId.substring(0, colon);
        String sub = treeId.substring(colon + 1);
        String base = "primary".equals(volume) ? "/storage/emulated/0" : "/storage/" + volume;
        String root = sub.isEmpty() ? base : base + "/" + sub;
        if (!audioPath.startsWith(root + "/")) {
            return null;
        }
        String rel = audioPath.substring(root.length() + 1);
        int dot = rel.lastIndexOf('.');
        int slash = rel.lastIndexOf('/');
        String noExt = dot > slash ? rel.substring(0, dot) : rel;
        String prefix = volume + ":" + (sub.isEmpty() ? "" : sub + "/");
        for (String ext : new String[]{".lrc", ".LRC", ".Lrc"}) {
            android.net.Uri doc = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, prefix + noExt + ext);
            try (InputStream in = context.getContentResolver().openInputStream(doc)) {
                if (in == null) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_FILE_BYTES) {
                        throw new IOException("lyrics file too large");
                    }
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            } catch (java.io.FileNotFoundException e) {
                // try the next spelling
            }
        }
        return null;
    }

    private static File findSidecar(String audioPath) {
        if (audioPath == null || audioPath.isEmpty()) {
            return null;
        }
        File audio = new File(audioPath);
        File dir = audio.getParentFile();
        if (dir == null) {
            return null;
        }
        String name = audio.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        for (String ext : new String[]{".lrc", ".LRC", ".Lrc"}) {
            File f = new File(dir, base + ext);
            if (f.isFile() && f.canRead()) {
                return f;
            }
        }
        return null;
    }

    private File cacheDir() {
        File dir = new File(context.getFilesDir(), "lyrics");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static String readText(File f) throws IOException {
        if (f.length() > MAX_FILE_BYTES) {
            throw new IOException("lyrics file too large");
        }
        InputStream in = new FileInputStream(f);
        try {
            return new String(readAll(in), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private static void writeText(File f, String text) throws IOException {
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
        }
        return buf.toByteArray();
    }

    private static String cacheKey(String title, String artist, String album, long durationSec) {
        String raw = (artist + "|" + title + "|" + album + "|" + durationSec).toLowerCase(Locale.ROOT);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private static boolean usable(String s) {
        if (s == null) {
            return false;
        }
        String t = s.trim();
        return !t.isEmpty() && !"<unknown>".equalsIgnoreCase(t);
    }

    // ---------------------------------------------------------------- online (LRCLIB)

    /** Returns LRC text, or null when LRCLIB has no synced lyrics for this track. */
    private static String fetchOnline(String title, String artist, String album,
                                      long durationSec) throws Exception {
        // 1. exact match (needs album and duration)
        if (usable(album) && durationSec > 0) {
            String url = API + "/get?track_name=" + enc(title)
                    + "&artist_name=" + enc(artist)
                    + "&album_name=" + enc(album)
                    + "&duration=" + durationSec;
            String body = httpGet(url);
            if (body != null) {
                String synced = synced(new JSONObject(body));
                if (!synced.isEmpty()) {
                    return synced;
                }
            }
        }

        // 2. fuzzy search; take the closest-length result that has synced lyrics
        String url = API + "/search?track_name=" + enc(title) + "&artist_name=" + enc(artist);
        String body = httpGet(url);
        if (body == null) {
            return null;
        }
        JSONArray results = new JSONArray(body);
        String best = null;
        long bestDiff = Long.MAX_VALUE;
        for (int i = 0; i < results.length(); i++) {
            JSONObject o = results.optJSONObject(i);
            if (o == null) {
                continue;
            }
            String synced = synced(o);
            if (synced.isEmpty()) {
                continue;
            }
            long diff = 0;
            if (durationSec > 0 && !o.isNull("duration")) {
                diff = Math.abs(Math.round(o.optDouble("duration", 0)) - durationSec);
            }
            if (diff < bestDiff) {
                bestDiff = diff;
                best = synced;
            }
        }
        if (best != null && (durationSec <= 0 || bestDiff <= MAX_DURATION_DIFF_SEC)) {
            return best;
        }
        return null;
    }

    /** JSON null must be checked first: optString() would return the text "null". */
    private static String synced(JSONObject o) {
        if (o.isNull("syncedLyrics")) {
            return "";
        }
        return o.optString("syncedLyrics", "").trim();
    }

    private static String enc(String s) throws IOException {
        return URLEncoder.encode(s == null ? "" : s.trim(), "UTF-8");
    }

    /** Returns the response body, or null for HTTP 404 (not found). */
    private static String httpGet(String urlString) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(urlString).openConnection();
        try {
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", USER_AGENT);
            c.setRequestProperty("Accept", "application/json");
            int code = c.getResponseCode();
            if (code == 404) {
                return null;
            }
            if (code != 200) {
                throw new IOException("HTTP " + code);
            }
            InputStream in = c.getInputStream();
            try {
                return new String(readAll(in), StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
        } finally {
            c.disconnect();
        }
    }
}