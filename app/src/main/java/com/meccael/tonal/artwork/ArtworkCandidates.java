package com.meccael.tonal.artwork;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Gathers several possible covers for the manual "Search online" picker. */
public final class ArtworkCandidates {
    public static final class Candidate {
        public final String label;
        public final String fullUrl;
        public final byte[] thumb;

        Candidate(String label, String fullUrl, byte[] thumb) {
            this.label = label;
            this.fullUrl = fullUrl;
            this.thumb = thumb;
        }
    }

    private static final int MAX_PER_SOURCE = 6;

    private ArtworkCandidates() {}

    /** artist may be blank or unknown, in which case only the album title is searched. */
    public static List<Candidate> search(String artist, String album) throws IOException {
        List<Candidate> out = new ArrayList<>();
        if (album == null || album.trim().isEmpty()) return out;
        boolean haveArtist = artist != null && !artist.trim().isEmpty() && !artist.toLowerCase().contains("unknown");
        IOException firstError = null;
        try {
            fromMusicBrainz(artist, album, haveArtist, out);
        } catch (IOException e) {
            firstError = e;
        }
        try {
            fromDeezer(artist, album, haveArtist, out);
        } catch (IOException e) {
            if (firstError == null) firstError = e;
        }
        if (out.isEmpty() && firstError != null) throw firstError;
        return out;
    }

    private static void fromMusicBrainz(String artist, String album, boolean haveArtist, List<Candidate> out) throws IOException {
        String query = "releasegroup:\"" + ArtworkFetcher.esc(album) + "\"";
        if (haveArtist) query += " AND artist:\"" + ArtworkFetcher.esc(artist) + "\"";
        String url = "https://musicbrainz.org/ws/2/release-group/?fmt=json&limit=10&query=" + URLEncoder.encode(query, "UTF-8");
        ArtworkFetcher.throttleMusicBrainz();
        byte[] body = ArtworkFetcher.get(url, "application/json");
        if (body == null) return;
        try {
            JSONArray groups = new JSONObject(new String(body, "UTF-8")).optJSONArray("release-groups");
            if (groups == null) return;
            Set<String> seen = new HashSet<>();
            int added = 0;
            for (int i = 0; i < groups.length() && added < MAX_PER_SOURCE; i++) {
                JSONObject g = groups.getJSONObject(i);
                if (g.optInt("score", 0) < 60) continue;
                String id = g.optString("id", "");
                if (id.isEmpty() || !seen.add(id)) continue;
                byte[] thumb = ArtworkFetcher.validThumb(ArtworkFetcher.get("https://coverartarchive.org/release-group/" + id + "/front-250", "image/*"));
                if (thumb == null) continue;
                String label = g.optString("title", album);
                String date = g.optString("first-release-date", "");
                if (date.length() >= 4) label += " (" + date.substring(0, 4) + ")";
                out.add(new Candidate(label, "https://coverartarchive.org/release-group/" + id + "/front-500", thumb));
                added++;
            }
        } catch (JSONException e) {
            // ignore malformed response
        }
    }

    private static void fromDeezer(String artist, String album, boolean haveArtist, List<Candidate> out) throws IOException {
        String q = haveArtist ? artist + " " + album : album;
        byte[] body = ArtworkFetcher.get("https://api.deezer.com/search/album?limit=" + MAX_PER_SOURCE + "&q=" + URLEncoder.encode(q, "UTF-8"), "application/json");
        if (body == null) return;
        try {
            JSONArray data = new JSONObject(new String(body, "UTF-8")).optJSONArray("data");
            if (data == null) return;
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                String full = item.optString("cover_xl", "");
                String small = item.optString("cover_medium", "");
                if (!full.startsWith("https://") || !small.startsWith("https://")) continue;
                byte[] thumb = ArtworkFetcher.validThumb(ArtworkFetcher.get(small, "image/*"));
                if (thumb == null) continue;
                JSONObject a = item.optJSONObject("artist");
                String label = item.optString("title", album) + (a == null ? "" : " - " + a.optString("name", ""));
                out.add(new Candidate(label, full, thumb));
            }
        } catch (JSONException e) {
            // ignore malformed response
        }
    }
}
