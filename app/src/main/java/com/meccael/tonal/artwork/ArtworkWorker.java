package com.meccael.tonal.artwork;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Walks the device's albums on Wi-Fi (an unmetered connection) and fills the gaps:
 * downloads artwork for albums that have none, and looks up year and genre for albums not yet described.
 * It never replaces existing artwork.
 */
public class ArtworkWorker extends Worker {
    public static final String UNIQUE_NAME = "tonal-artwork-fill";
    public static final String KEY_DONE = "done";
    public static final String KEY_TOTAL = "total";
    public static final String KEY_FOUND = "found";
    private static final Uri ALBUM_ART = Uri.parse("content://media/external/audio/albumart");

    private static final class Album {
        final long id;
        final String name;
        final String artist;

        Album(long id, String name, String artist) {
            this.id = id;
            this.name = name;
            this.artist = artist;
        }
    }

    public ArtworkWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** Queues a run; if one is already queued or running, leaves it alone. */
    public static void enqueue(Context context) {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ArtworkWorker.class).setConstraints(constraints).build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request);
    }

    /** Called at app start: runs only if the user switched the feature on. */
    public static void maybeAutoRun(Context context) {
        if (ArtworkStore.isAutoEnabled(context)) enqueue(context);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_NAME);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        List<Album> albums;
        try {
            albums = loadAlbums(c);
        } catch (SecurityException e) {
            return Result.failure();
        }
        int total = albums.size(), done = 0, found = 0, networkErrors = 0;
        for (Album a : albums) {
            if (isStopped()) return Result.retry();
            done++;
            setProgressAsync(new Data.Builder().putInt(KEY_DONE, done).putInt(KEY_TOTAL, total).putInt(KEY_FOUND, found).build());
            boolean needArt = !(ArtworkStore.has(c, a.id) || ArtworkStore.isRecentMiss(c, a.id) || deviceHasArt(c, a.id));
            boolean needInfo = AlbumInfoStore.needsLookup(c, a.id);
            if (!needArt && !needInfo) continue;
            try {
                if (needArt) {
                    byte[] img = ArtworkFetcher.fetch(a.artist, a.name);
                    if (img != null) {
                        ArtworkStore.save(c, a.id, img);
                        found++;
                    } else {
                        ArtworkStore.markMiss(c, a.id);
                    }
                }
                if (needInfo) {
                    ArtworkFetcher.Meta meta = ArtworkFetcher.fetchMeta(a.artist, a.name);
                    if (meta != null) {
                        AlbumInfoStore.save(c, a.id, meta.year, meta.genre);
                    } else {
                        AlbumInfoStore.markMiss(c, a.id);
                    }
                }
            } catch (IOException e) {
                // network trouble: do not record a miss; stop early if the connection is clearly gone
                if (++networkErrors >= 5) return Result.retry();
            }
        }
        return Result.success(new Data.Builder().putInt(KEY_FOUND, found).putInt(KEY_TOTAL, total).build());
    }

    private static List<Album> loadAlbums(Context c) {
        List<Album> list = new ArrayList<>();
        String[] projection = {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST};
        try (Cursor cur = c.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection, null, null, null)) {
            if (cur == null) return list;
            while (cur.moveToNext()) {
                list.add(new Album(cur.getLong(0), cur.getString(1), cur.getString(2)));
            }
        }
        return list;
    }

    /** True if the device already has artwork for this album. */
    private static boolean deviceHasArt(Context c, long albumId) {
        ContentResolver cr = c.getContentResolver();
        try (InputStream in = cr.openInputStream(ContentUris.withAppendedId(ALBUM_ART, albumId))) {
            return in != null && in.read() >= 0;
        } catch (Exception e) {
            return false;
        }
    }
}
