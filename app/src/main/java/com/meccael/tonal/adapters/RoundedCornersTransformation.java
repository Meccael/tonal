package com.meccael.tonal.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

class RoundedCornersTransformation extends BitmapTransformation {
    private static final String ID = "com.meccael.tonal.adapters.RoundedCornersTransformation";
    private static final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);
    private final int mRadius;

    RoundedCornersTransformation(Context context, int radius) {
        this.mRadius = radius;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        int width = toTransform.getWidth();
        int height = toTransform.getHeight();
        Bitmap result = pool.get(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(toTransform, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(0.0f, 0.0f, width, height), mRadius, mRadius, paint);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RoundedCornersTransformation && ((RoundedCornersTransformation) o).mRadius == mRadius;
    }

    @Override
    public int hashCode() {
        return ID.hashCode() + mRadius * 31;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
        messageDigest.update(ByteBuffer.allocate(4).putInt(mRadius).array());
    }
}