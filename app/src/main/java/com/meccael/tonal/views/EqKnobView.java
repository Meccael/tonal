package com.meccael.tonal.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A rotary knob with a glowing cream ring that fills as you turn it clockwise. The track opens at
 * the bottom, with a minus at its start and a plus at its end. Value is 0..1.
 */
public class EqKnobView extends View {
    public interface Listener {
        void onChanged(float value, boolean fromUser);
    }

    private static final int CREAM = 0xFFEADFC8;
    private static final float START = 135f;
    private static final float SWEEP = 270f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float d;
    private float value = 0f;
    private Listener listener;

    public EqKnobView(Context context) {
        this(context, null);
    }

    public EqKnobView(Context context, AttributeSet attrs) {
        super(context, attrs);
        d = context.getResources().getDisplayMetrics().density;
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
        float size = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = size / 2f - 14 * d;
        paint.setShader(null);
        paint.setStrokeCap(Paint.Cap.ROUND);

        // outer thin track with a marker at the current value
        rect.set(cx - r, cy - r, cx + r, cy + r);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3 * d);
        paint.setColor(0xFF1A1A1A);
        canvas.drawArc(rect, START, SWEEP, false, paint);
        double ang = Math.toRadians(START + SWEEP * value);
        float mx = cx + (float) Math.cos(ang) * r;
        float my = cy + (float) Math.sin(ang) * r;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x55EADFC8);
        canvas.drawCircle(mx, my, 6 * d, paint);
        paint.setColor(CREAM);
        canvas.drawCircle(mx, my, 3 * d, paint);

        // minus and plus caps at the ends of the track
        drawCap(canvas, START, cx, cy, r, "\u2212");
        drawCap(canvas, START + SWEEP, cx, cy, r, "+");

        // inner glowing ring
        float r2 = r * 0.80f;
        rect.set(cx - r2, cy - r2, cx + r2, cy + r2);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(9 * d);
        paint.setColor(0xFF232323);
        canvas.drawArc(rect, START, SWEEP, false, paint);
        if (value > 0.001f) {
            paint.setStrokeWidth(15 * d);
            paint.setColor(0x33EADFC8);
            canvas.drawArc(rect, START, SWEEP * value, false, paint);
            paint.setStrokeWidth(6 * d);
            paint.setColor(CREAM);
            canvas.drawArc(rect, START, SWEEP * value, false, paint);
        }
        paint.setStrokeWidth(2 * d);
        paint.setColor(0xFF0E0E0E);
        canvas.drawCircle(cx, cy, r2 + 7 * d, paint);

        // knob body
        float rk = r2 * 0.80f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x66000000);
        canvas.drawCircle(cx, cy + 5 * d, rk, paint);
        paint.setShader(new RadialGradient(cx - rk * 0.3f, cy - rk * 0.35f, rk * 1.4f, 0xFF606060, 0xFF2A2A2A, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, rk, paint);
        paint.setShader(new RadialGradient(cx - rk * 0.15f, cy - rk * 0.2f, rk * 0.7f, 0xFF4D4D4D, 0xFF303030, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, rk * 0.56f, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * d);
        paint.setColor(0xFF1B1B1B);
        canvas.drawCircle(cx, cy, rk, paint);

        // indicator dot
        float dx = cx + (float) Math.cos(ang) * rk * 0.76f;
        float dy = cy + (float) Math.sin(ang) * rk * 0.76f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(CREAM);
        canvas.drawCircle(dx, dy, 4.5f * d, paint);
    }

    private void drawCap(Canvas canvas, float deg, float cx, float cy, float r, String label) {
        double a = Math.toRadians(deg);
        float x = cx + (float) Math.cos(a) * r;
        float y = cy + (float) Math.sin(a) * r;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF1D1D1D);
        canvas.drawCircle(x, y, 11 * d, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * d);
        paint.setColor(0xFF3A3A3A);
        canvas.drawCircle(x, y, 11 * d, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFB9AE98);
        paint.setTextSize(15 * d);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, x, y + 5 * d, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                update(e.getX(), e.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                update(e.getX(), e.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void update(float x, float y) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        double deg = Math.toDegrees(Math.atan2(y - cy, x - cx));
        if (deg < 0) deg += 360.0;
        double rel = (deg - START + 360.0) % 360.0;
        float v;
        if (rel <= SWEEP) {
            v = (float) (rel / SWEEP);
        } else {
            // in the gap at the bottom: snap to whichever end is closer
            v = rel < SWEEP + (360.0 - SWEEP) / 2.0 ? 1f : 0f;
        }
        if (Math.abs(v - value) > 0.5f) return; // ignore jumping straight across the gap
        if (v != value) {
            value = v;
            invalidate();
            if (listener != null) listener.onChanged(value, true);
        }
    }
}
