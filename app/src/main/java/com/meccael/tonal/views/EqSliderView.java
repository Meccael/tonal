package com.meccael.tonal.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A vertical equalizer fader: a dark groove with a cream glowing fill and a grippy thumb.
 * Value is 0..1 with 1 at the top. In bipolar mode the fill grows from the middle (for band
 * levels); otherwise it grows from the bottom (for the preamp).
 */
public class EqSliderView extends View {
    public interface Listener {
        void onChanged(float value, boolean fromUser);
    }

    private static final int CREAM = 0xFFEADFC8;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float d;
    private float value = 0.5f;
    private boolean bipolar = true;
    private Listener listener;

    public EqSliderView(Context context) {
        this(context, null);
    }

    public EqSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        d = context.getResources().getDisplayMetrics().density;
    }

    public void setBipolar(boolean bipolar) {
        this.bipolar = bipolar;
        invalidate();
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float v) {
        this.value = Math.max(0f, Math.min(1f, v));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float thumbH = 42 * d;
        float top = thumbH / 2f + 4 * d;
        float bottom = h - thumbH / 2f - 4 * d;
        float track = bottom - top;
        if (track <= 0) return;

        // dashed guide marks, like the reference
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * d);
        paint.setColor(0x24EADFC8);
        paint.setPathEffect(new DashPathEffect(new float[]{4 * d, 3 * d}, 0));
        for (int k = 0; k <= 4; k++) {
            float y = top + track * k / 4f;
            canvas.drawLine(cx - 34 * d, y, cx - 14 * d, y, paint);
            canvas.drawLine(cx + 14 * d, y, cx + 34 * d, y, paint);
        }
        paint.setPathEffect(null);

        // groove
        rect.set(cx - 3.5f * d, top - 6 * d, cx + 3.5f * d, bottom + 6 * d);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF151515);
        canvas.drawRoundRect(rect, 4 * d, 4 * d, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * d);
        paint.setColor(0xFF3A3A3A);
        canvas.drawRoundRect(rect, 4 * d, 4 * d, paint);

        // glowing fill
        float thumbY = bottom - value * track;
        float from = bipolar ? (top + bottom) / 2f : bottom;
        float y1 = Math.min(from, thumbY);
        float y2 = Math.max(from, thumbY);
        if (y2 - y1 > 0.5f) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x44EADFC8);
            rect.set(cx - 4.5f * d, y1, cx + 4.5f * d, y2);
            canvas.drawRoundRect(rect, 4 * d, 4 * d, paint);
            paint.setColor(CREAM);
            rect.set(cx - 1.5f * d, y1, cx + 1.5f * d, y2);
            canvas.drawRoundRect(rect, 2 * d, 2 * d, paint);
        }

        // thumb: soft shadow, body, grip lines
        float tw = 34 * d;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x66000000);
        rect.set(cx - tw / 2f, thumbY - thumbH / 2f + 3 * d, cx + tw / 2f, thumbY + thumbH / 2f + 3 * d);
        canvas.drawRoundRect(rect, 8 * d, 8 * d, paint);
        rect.set(cx - tw / 2f, thumbY - thumbH / 2f, cx + tw / 2f, thumbY + thumbH / 2f);
        paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, 0xFF5C5C5C, 0xFF363636, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, 8 * d, 8 * d, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * d);
        paint.setColor(0xFF1C1C1C);
        canvas.drawRoundRect(rect, 8 * d, 8 * d, paint);
        for (int g = -1; g <= 1; g++) {
            float gy = thumbY + g * 5 * d;
            paint.setColor(0xFF242424);
            canvas.drawLine(cx - 9 * d, gy, cx + 9 * d, gy, paint);
            paint.setColor(0x33EADFC8);
            canvas.drawLine(cx - 9 * d, gy + 1 * d, cx + 9 * d, gy + 1 * d, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                update(e.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                update(e.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void update(float y) {
        float thumbH = 42 * d;
        float top = thumbH / 2f + 4 * d;
        float bottom = getHeight() - thumbH / 2f - 4 * d;
        float track = bottom - top;
        if (track <= 0) return;
        float v = Math.max(0f, Math.min(1f, (bottom - y) / track));
        if (bipolar && Math.abs(v - 0.5f) < 0.02f) v = 0.5f; // click to the centre
        if (v != value) {
            value = v;
            invalidate();
            if (listener != null) listener.onChanged(value, true);
        }
    }
}
