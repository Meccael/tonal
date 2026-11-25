package com.meccael.tonal.artwork;

import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Looks up album artwork online. Order: MusicBrainz + Cover Art Archive, then Deezer.
 * Only the artist and album names are sent. Returns null when nothing trustworthy is found;
 * throws IOException on network trouble (so the caller can retry later instead of recording a miss).
 */
public final class ArtworkFetcher {
    // MusicBrainz asks for a descriptive User-Agent that includes contact details.
private static final String USER_AGENT = "Tonal/1.0 ( personal project; meccatronics72@gmail.com )";    private static final int MAX_BYTES = 6 * 1024 * 1024;
    private static final int MIN_DIMENSION = 200;
    private static long lastMusicBrainzCall = 0L;

    private ArtworkFetcher() {}

    /** Year and genre found for an album. Either may be empty, never both. */
    public static final class Meta {
        public final String year;
        public final String genre;

        Meta(String year, String genre) {
            this.year = year;
            this.genre = genre;
        }
    }

    /** First release year and top genre tags from MusicBrainz, or null if there is no confident match. */
    public static Meta fetchMeta(String artist, String album) throws IOException {
        if (isBlank(album) || isUnknown(album) || isBlank(artist) || isUnknown(artist)) return null;
        String query = "releasegroup:\"" + esc(album) + "\" AND artist:\"" + esc(artist) + "\"";
        throttleMusicBrainz();
        byte[] body = get("https://musicbrainz.org/ws/2/release-group/?fmt=json&limit=5&query=" + enc(query), "application/json");
        if (body == null) return null;
        try {
            JSONArray groups = new JSONObject(new String(body, "UTF-8")).optJSONArray("release-groups");
            if (groups == null) return null;
            for (int i = 0; i < groups.length(); i++) {
                JSONObject g = groups.getJSONObject(i);
                if (g.optInt("score", 0) < 90 || !similar(g.optString("title", ""), album)) continue;
                String date = g.optString("first-release-date", "");
                String year = date.length() >= 4 && date.substring(0, 4).matches("\\d{4}") ? date.substring(0, 4) : "";
                String genre = topTags(g.optJSONArray("tags"));
                if (!year.isEmpty() || !genre.isEmpty()) return new Meta(year, genre);
            }
        } catch (JSONException e) {
            return null;
        }
        return null;
    }

    private static String topTags(JSONArray tags) throws JSONException {
        if (tags == null) return "";
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < tags.length(); i++) list.add(tags.getJSONObject(i));
        Collections.sort(list, (a, b) -> Integer.compare(b.optInt("count", 0), a.optInt("count", 0)));
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (JSONObject t : list) {
            String name = t.optString("name", "").trim();
            String low = name.toLowerCase(Locale.ROOT);
            if (name.isEmpty() || low.equals("seen live") || low.startsWith("favorite") || low.startsWith("albums i")) continue;
            if (used++ > 0) sb.append(", ");
            sb.append(Character.toUpperCase(name.charAt(0))).append(name.substring(1));
            if (used == 2) break;
        }
        return sb.toString();
    }

    public static byte[] fetch(String artist, String album) throws IOException {
        if (isBlank(album) || isUnknown(album) || isBlank(artist) || isUnknown(artist)) return null;
        byte[] img = fromMusicBrainz(artist, album);
        if (img == null) img = fromDeezer(artist, album);
        return img;
    }

    // ---- MusicBrainz + Cover Art Archive ----

    private static byte[] fromMusicBrainz(String artist, String album) throws IOException {
        String query = "release:\"" + esc(album) + "\" AND artist:\"" + esc(artist) + "\"";
        String url = "https://musicbrainz.org/ws/2/release/?fmt=json&limit=5&query=" + enc(query);
        throttleMusicBrainz();
        byte[] body = get(url, "application/json");
        if (body == null) return null;
        try {
            JSONArray releases = new JSONObject(new String(body, "UTF-8")).optJSONArray("releases");
            if (releases == null) return null;
            Set<String> triedGroups = new HashSet<>();
            for (int i = 0; i < releases.length(); i++) {
                JSONObject r = releases.getJSONObject(i);
                if (r.optInt("score", 0) < 90) continue;
                String id = r.optString("id", "");
                if (!id.isEmpty()) {
                    byte[] img = validImage(get("https://coverartarchive.org/release/" + id + "/front-500", "image/*"));
                    if (img != null) return img;
                }
                JSONObject group = r.optJSONObject("release-group");
                String gid = group == null ? "" : group.optString("id", "");
                if (!gid.isEmpty() && triedGroups.add(gid)) {
                    byte[] img = validImage(get("https://coverartarchive.org/release-group/" + gid + "/front-500", "image/*"));
                    if (img != null) return img;
                }
            }
        } catch (JSONException e) {
            return null;
        }
        return null;
    }

    static synchronized void throttleMusicBrainz() {
        long wait = lastMusicBrainzCall + 1100L - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastMusicBrainzCall = System.currentTimeMillis();
    }

    // ---- Deezer fallback ----

    private static byte[] fromDeezer(String artist, String album) throws IOException {
        String q = "artist:\"" + esc(artist) + "\" album:\"" + esc(album) + "\"";
        byte[] body = get("https://api.deezer.com/search/album?limit=5&q=" + enc(q), "application/json");
        if (body == null) return null;
        try {
            JSONArray data = new JSONObject(new String(body, "UTF-8")).optJSONArray("data");
            if (data == null) return null;
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                JSONObject a = item.optJSONObject("artist");
                String title = item.optString("title", "");
                String name = a == null ? "" : a.optString("name", "");
                String cover = item.optString("cover_xl", "");
                if (cover.isEmpty() || !cover.startsWith("https://")) continue;
                if (similar(title, album) && similar(name, artist)) {
                    byte[] img = validImage(get(cover, "image/*"));
                    if (img != null) return img;
                }
            }
        } catch (JSONException e) {
            return null;
        }
        return null;
    }

    // ---- helpers ----

    /** GET returning the body, null for 404, IOException for anything else that failed. */
    static byte[] get(String urlStr, String accept) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        try {
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", USER_AGENT);
            c.setRequestProperty("Accept", accept);
            int code = c.getResponseCode();
            if (code == 404) return null;
            if (code != 200) throw new IOException("HTTP " + code + " from " + c.getURL().getHost());
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) throw new IOException("Response too large");
                }
                return out.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }

    /** Downloads a full-size cover chosen in the picker (https only). */
    public static byte[] download(String url) throws IOException {
        if (url == null || !url.startsWith("https://")) return null;
        return validImage(get(url, "image/*"));
    }

    static byte[] validThumb(byte[] data) {
        return validImage(data);
    }

    private static byte[] validImage(byte[] data) {
        if (data == null || data.length < 1024) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        if (o.outWidth < MIN_DIMENSION || o.outHeight < MIN_DIMENSION) return null;
        return data;
    }

    private static boolean similar(String a, String b) {
        String x = norm(a), y = norm(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        return x.equals(y) || x.contains(y) || y.contains(x);
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("\\(.*?\\)|\\[.*?\\]", "").replaceAll("[^\\p{L}\\p{N}]", "");
    }

    static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String enc(String s) throws IOException {
        return URLEncoder.encode(s, "UTF-8");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isUnknown(String s) {
        String t = s.trim().toLowerCase(Locale.ROOT);
        return t.equals("<unknown>") || t.equals("unknown") || t.equals("unknown artist");
    }
}
