package com.meccael.tonal.artwork;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.meccael.tonal.R;

/** The manual "Change artwork" flow: search online, choose from phone, or reset to the original. */
public final class ArtworkPicker {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final int MAX_SIDE = 1200;

    private ArtworkPicker() {}

    /** onChanged runs on the main thread after the artwork was replaced or reset. */
    public static void show(final Context ctx, final long albumId, final Runnable onChanged) {
        final boolean hasOverride = ArtworkStore.has(ctx, albumId);
        final List<String> labels = new ArrayList<>();
        labels.add(ctx.getString(R.string.artwork_search));
        labels.add(ctx.getString(R.string.artwork_pick));
        if (hasOverride) labels.add(ctx.getString(R.string.artwork_reset));
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.artwork_change)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    if (which == 0) searchOnline(ctx, albumId, onChanged);
                    else if (which == 1) pickFromPhone(ctx, albumId, onChanged);
                    else resetToOriginal(ctx, albumId, onChanged);
                })
                .show();
    }

    // ---- reset ----

    private static void resetToOriginal(Context ctx, long albumId, Runnable onChanged) {
        ArtworkStore.reset(ctx, albumId);
        ArtworkStore.markMiss(ctx, albumId); // so the automatic fill does not immediately re-download it
        Toast.makeText(ctx, R.string.artwork_updated, Toast.LENGTH_SHORT).show();
        if (onChanged != null) onChanged.run();
    }

    // ---- choose from phone ----

    private static ComponentActivity activityOf(Context ctx) {
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof ComponentActivity) return (ComponentActivity) ctx;
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    private static void pickFromPhone(final Context ctx, final long albumId, final Runnable onChanged) {
        ComponentActivity act = activityOf(ctx);
        if (act == null) {
            Toast.makeText(ctx, R.string.artwork_pick_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        final ActivityResultLauncher<?>[] holder = new ActivityResultLauncher<?>[1];
        final ActivityResultLauncher<String> launcher = act.getActivityResultRegistry().register(
                "tonal_artwork_pick_" + albumId + "_" + System.nanoTime(),
                new ActivityResultContracts.GetContent(),
                uri -> {
                    holder[0].unregister();
                    if (uri != null) importImage(ctx, albumId, uri, onChanged);
                });
        holder[0] = launcher;
        launcher.launch("image/*");
    }

    private static void importImage(final Context ctx, final long albumId, final Uri uri, final Runnable onChanged) {
        IO.execute(() -> {
            boolean ok = false;
            try {
                byte[] jpeg = decodeAndCompress(ctx, uri);
                if (jpeg != null) {
                    ArtworkStore.save(ctx, albumId, jpeg);
                    ok = true;
                }
            } catch (Exception e) {
                ok = false;
            }
            finish(ctx, ok, onChanged);
        });
    }

    private static byte[] decodeAndCompress(Context ctx, Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE && bounds.outHeight / (sample * 2) >= MAX_SIDE) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bmp == null) return null;
        int longest = Math.max(bmp.getWidth(), bmp.getHeight());
        if (longest > MAX_SIDE) {
            float scale = MAX_SIDE / (float) longest;
            bmp = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth() * scale), Math.round(bmp.getHeight() * scale), true);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 92, out);
        return out.toByteArray();
    }

    // ---- search online ----

    private static void searchOnline(final Context ctx, final long albumId, final Runnable onChanged) {
        final androidx.appcompat.app.AlertDialog progress = new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.artwork_searching)
                .setView(spinner(ctx))
                .setCancelable(true)
                .show();
        IO.execute(() -> {
            String[] info = lookupAlbum(ctx, albumId);
            List<ArtworkCandidates.Candidate> found = null;
            try {
                found = ArtworkCandidates.search(info[1], info[0]);
            } catch (Exception e) {
                found = null;
            }
            final List<ArtworkCandidates.Candidate> result = found;
            MAIN.post(() -> {
                if (!progress.isShowing()) return; // cancelled
                progress.dismiss();
                if (result == null || result.isEmpty()) {
                    Toast.makeText(ctx, R.string.artwork_none, Toast.LENGTH_LONG).show();
                } else {
                    showGrid(ctx, albumId, result, onChanged);
                }
            });
        });
    }

    private static View spinner(Context ctx) {
        FrameLayout box = new FrameLayout(ctx);
        int pad = dp(ctx, 24);
        box.setPadding(pad, pad, pad, pad);
        ProgressBar bar = new ProgressBar(ctx);
        box.addView(bar, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        return box;
    }

    private static void showGrid(final Context ctx, final long albumId, List<ArtworkCandidates.Candidate> items, final Runnable onChanged) {
        final int columns = 3;
        int screen = ctx.getResources().getDisplayMetrics().widthPixels;
        final int cell = (screen - dp(ctx, 96)) / columns;
        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(columns);
        int pad = dp(ctx, 12);
        grid.setPadding(pad, pad, pad, pad);
        final androidx.appcompat.app.AlertDialog[] dialogHolder = new androidx.appcompat.app.AlertDialog[1];
        for (final ArtworkCandidates.Candidate c : items) {
            LinearLayout item = new LinearLayout(ctx);
            item.setOrientation(LinearLayout.VERTICAL);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell;
            lp.setMargins(dp(ctx, 4), dp(ctx, 4), dp(ctx, 4), dp(ctx, 8));
            item.setLayoutParams(lp);
            ImageView img = new ImageView(ctx);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageBitmap(BitmapFactory.decodeByteArray(c.thumb, 0, c.thumb.length));
            item.addView(img, new LinearLayout.LayoutParams(cell, cell));
            TextView label = new TextView(ctx);
            label.setText(c.label);
            label.setTextSize(11f);
            label.setMaxLines(2);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            item.addView(label);
            item.setOnClickListener(v -> {
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                applyCandidate(ctx, albumId, c, onChanged);
            });
            grid.addView(item);
        }
        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(grid);
        dialogHolder[0] = new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.artwork_choose)
                .setView(scroll)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void applyCandidate(final Context ctx, final long albumId, final ArtworkCandidates.Candidate c, final Runnable onChanged) {
        IO.execute(() -> {
            boolean ok = false;
            try {
                byte[] full = ArtworkFetcher.download(c.fullUrl);
                if (full != null) {
                    ArtworkStore.save(ctx, albumId, full);
                    ok = true;
                }
            } catch (Exception e) {
                ok = false;
            }
            finish(ctx, ok, onChanged);
        });
    }

    // ---- shared ----

    private static void finish(final Context ctx, final boolean ok, final Runnable onChanged) {
        MAIN.post(() -> {
            Toast.makeText(ctx, ok ? R.string.artwork_updated : R.string.artwork_failed, Toast.LENGTH_SHORT).show();
            if (ok && onChanged != null) onChanged.run();
        });
    }

    /** Returns {album, artist} for a MediaStore album id (empty strings if unknown). */
    private static String[] lookupAlbum(Context ctx, long albumId) {
        String[] r = {"", ""};
        try (Cursor c = ctx.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST},
                MediaStore.Audio.Albums._ID + "=?", new String[]{String.valueOf(albumId)}, null)) {
            if (c != null && c.moveToFirst()) {
                r[0] = c.getString(0) == null ? "" : c.getString(0);
                r[1] = c.getString(1) == null ? "" : c.getString(1);
            }
        } catch (Exception e) {
            // leave blanks
        }
        return r;
    }

    private static int dp(Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}
