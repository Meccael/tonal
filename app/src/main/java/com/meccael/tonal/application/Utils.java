package com.meccael.tonal.application;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import com.meccael.tonal.R;
import com.meccael.tonal.artwork.ArtworkStore;

/** Small image and device helpers used around the app. */
public class Utils {
    private static final Uri ALBUM_ART = Uri.parse("content://media/external/audio/albumart");

    private static boolean usable(Bitmap b) {
        return b != null && b.getWidth() > 0 && b.getHeight() > 0;
    }

    /** A blurred copy of the picture, or null if there is no picture. */
    static Bitmap blur(Bitmap source, float amount) {
        if (!usable(source)) {
            return null;
        }
        Bitmap copy = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        new Canvas(copy).drawBitmap(source, 0f, 0f, paint);
        return FastBlur.doBlur(copy, Math.max(1, (int) amount - 2));
    }

    /** A copy of the picture with every colour channel shifted by the given amount (-255..255). */
    static Bitmap doBrightness(Bitmap source, int shift) {
        if (!usable(source)) {
            return source;
        }
        int w = source.getWidth();
        int h = source.getHeight();
        int[] px = new int[w * h];
        source.getPixels(px, 0, w, 0, 0, w, h);
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            int r = Math.max(0, Math.min(255, Color.red(p) + shift));
            int g = Math.max(0, Math.min(255, Color.green(p) + shift));
            int b = Math.max(0, Math.min(255, Color.blue(p) + shift));
            px[i] = Color.argb(Color.alpha(p), r, g, b);
        }
        Bitmap.Config config = source.getConfig() == null ? Bitmap.Config.ARGB_8888 : source.getConfig();
        Bitmap out = Bitmap.createBitmap(w, h, config);
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    /**
     * The cover for a song, as a square of the given size (or full size when size is 0 or less).
     * Looks for a cover the user picked, then the one Android knows about, then the fallback
     * resource, then the stock cover, and as a last resort returns a blank square.
     */
    static Bitmap getCoverBitamp(Context context, long coverId, int fallbackRes, int size) {
        Bitmap bitmap = null;
        if (coverId > 0) {
            File custom = ArtworkStore.fileFor(context, coverId);
            if (custom.length() > 0) {
                bitmap = BitmapFactory.decodeFile(custom.getPath());
            }
            if (!usable(bitmap)) {
                bitmap = readFromMediaStore(context, coverId);
            }
        }
        if (!usable(bitmap)) {
            bitmap = decodeResource(context, fallbackRes > 0 ? fallbackRes : R.drawable.cover0);
        }
        if (!usable(bitmap)) {
            bitmap = decodeResource(context, R.drawable.cover0);
        }
        if (!usable(bitmap)) {
            int side = Math.max(1, size);
            bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
        }
        return size <= 0 ? bitmap : scaleTo(bitmap, size, size);
    }

    private static Bitmap readFromMediaStore(Context context, long coverId) {
        try (InputStream in = context.getContentResolver().openInputStream(ContentUris.withAppendedId(ALBUM_ART, coverId))) {
            return in == null ? null : BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap decodeResource(Context context, int res) {
        try {
            return BitmapFactory.decodeResource(context.getResources(), res);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap scaleTo(Bitmap bitmap, int height, int width) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w <= 0 || h <= 0 || width <= 0 || height <= 0 || (w == width && h == height)) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postScale((float) width / w, (float) height / h);
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, false);
    }

    /** The picture with rounded corners. The original is recycled. */
    static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int radius) {
        if (!usable(bitmap)) {
            return bitmap;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        new Canvas(out).drawRoundRect(new RectF(0f, 0f, w, h), radius, radius, paint);
        if (bitmap != out) {
            bitmap.recycle();
        }
        return out;
    }

    /**
     * How much to brighten or darken a cover so text stays readable on top of it:
     * +15 if it is mostly near-black, -45 if it is mostly near-white, otherwise 0.
     */
    static int isDarkThumbnail(Bitmap bitmap) {
        if (!usable(bitmap)) {
            return 0;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] px = new int[w * h];
        bitmap.getPixels(px, 0, w, 0, 0, w, h);
        float limit = px.length * 0.45f;
        int dark = 0;
        int light = 0;
        for (int p : px) {
            double luma = 0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p);
            if (luma < 20) {
                dark++;
            } else if (luma > 220) {
                light++;
            }
        }
        if (dark >= limit) {
            return 15;
        }
        return light >= limit ? -45 : 0;
    }

    static boolean isFlyme4Later() {
        try {
            return Build.MANUFACTURER.contains("Meizu")
                    || Build.FINGERPRINT.contains("Flyme_OS_4")
                    || Build.VERSION.INCREMENTAL.contains("Flyme_OS_4");
        } catch (Exception e) {
            return false;
        }
    }

    static boolean isHuawei() {
        try {
            boolean lollipop = Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22;
            return lollipop && Build.MANUFACTURER.toLowerCase(Locale.getDefault()).contains("huawei");
        } catch (Exception e) {
            return false;
        }
    }

    static boolean isMIUI6Later() {
        try {
            Object version = Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class).invoke(null, "ro.miui.ui.version.name");
            return Integer.parseInt(((String) version).replaceAll("[vV]", "")) >= 6;
        } catch (Exception e) {
            return false;
        }
    }
}
