package com.meccael.tonal.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A thin strip of letters for jumping through an A-Z sorted list. Runs down the edge in portrait
 * and along the bottom in landscape. Dragging a finger along it reports the letter under it.
 */
public class LetterStrip extends View {
    public interface Listener {
        void onLetter(int index);
        void onRelease();
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private char[] letters = new char[0];
    private boolean horizontal;
    private int active = -1;
    private Listener listener;

    public LetterStrip(Context context) {
        super(context);
        init();
    }

    public LetterStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
        invalidate();
    }

    public void setLetters(char[] letters) {
        this.letters = letters == null ? new char[0] : letters;
        this.active = -1;
        invalidate();
    }

    public char getLetter(int index) {
        return index >= 0 && index < letters.length ? letters[index] : '#';
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = letters.length;
        if (n == 0) return;
        float length = horizontal ? getWidth() : getHeight();
        float cell = length / n;
        float density = getResources().getDisplayMetrics().density;
        float size = Math.min(10f * density, cell * 0.85f);
        paint.setTextSize(size);
        for (int i = 0; i < n; i++) {
            paint.setColor(i == active ? 0xFF7C9CFF : 0x99FFFFFF);
            float c = cell * (i + 0.5f);
            float x = horizontal ? c : getWidth() / 2f;
            float y = horizontal ? getHeight() / 2f : c;
            y += size * 0.35f;
            canvas.drawText(String.valueOf(letters[i]), x, y, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int n = letters.length;
        if (n == 0) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                float length = horizontal ? getWidth() : getHeight();
                float pos = horizontal ? e.getX() : e.getY();
                int idx = Math.max(0, Math.min(n - 1, (int) (pos / length * n)));
                if (idx != active) {
                    active = idx;
                    invalidate();
                    if (listener != null) listener.onLetter(idx);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                active = -1;
                invalidate();
                if (listener != null) listener.onRelease();
                return true;
            default:
                return true;
        }
    }
}
