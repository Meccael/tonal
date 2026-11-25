package com.meccael.tonal.artwork;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.signature.ObjectKey;
import java.io.File;

/** One place that decides what to show for an album: Tonal's own artwork first, else the device's. */
public final class ArtworkResolver {
    private static final Uri ALBUM_ART = Uri.parse("content://media/external/audio/albumart");

    private ArtworkResolver() {}

    /** Pass to Glide's load(). A File when Tonal has its own artwork, otherwise the MediaStore uri. */
    public static Object model(Context c, long albumId) {
        File f = ArtworkStore.fileFor(c, albumId);
        if (f.length() > 0) return f;
        return ContentUris.withAppendedId(ALBUM_ART, albumId);
    }

    /** Pass to Glide's signature() so a replaced image is not served from Glide's cache. */
    public static ObjectKey signature(Context c, long albumId) {
        File f = ArtworkStore.fileFor(c, albumId);
        return new ObjectKey(f.length() > 0 ? f.lastModified() : 0L);
    }
}
