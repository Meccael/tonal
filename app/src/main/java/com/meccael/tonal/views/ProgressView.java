package com.meccael.tonal.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.meccael.tonal.R;
import com.meccael.tonal.application.Application_Main;
import com.meccael.tonal.application.Preferences;
import com.meccael.tonal.application.ServiceMusic;

/**
 * The song progress indicator. Drawn as a flat bar where it is short, and as an open ring around the
 * cover on the Player screen. A dot marks the position, and dragging along it seeks. On the Player,
 * touching the middle of the ring goes back.
 *
 * Progress is counted in tenths of a second.
 */
public class ProgressView extends View {
    private static final float RING_START = 168f;
    private static final float RING_SWEEP = 156f;

    private final RectF mBounds = new RectF();
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mForeground;
    private final int mBackground;
    private final float mStroke;
    private final float mHairline;
    private final float mThumbRadius;
    private final float mThumbRadiusActive;

    private boolean mAllowRefresh = true;
    private int mProgress;
    private int mMax;
    private float mMorph;

    public ProgressView(Context context) {
        this(context, null, 0);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressView(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        float density = getResources().getDisplayMetrics().density;
        mStroke = 3f * density;
        mHairline = density;
        mThumbRadius = 4f * density;
        mThumbRadiusActive = 6f * density;
        mForeground = ContextCompat.getColor(context, R.color.colorWhite);
        mBackground = ContextCompat.getColor(context, R.color.colorLight);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStroke);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mThumbPaint.setStyle(Paint.Style.FILL);
        mThumbPaint.setColor(mForeground);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return handleTouch(context, event, view.getWidth());
            }
        });

        // Start from wherever the song currently is, if one is loaded.
        mProgress = 1;
        mMax = 300;
        try {
            ServiceMusic service = ((Application_Main) getContext()).mServiceMusic;
            if (service != null && service.musicLaunched()) {
                mProgress = service.MusicPlayer.getCurrentPosition() / 100;
                mMax = ((int) service.musicPlayed.getMusic(getContext()).getDuration()) * 10;
            }
        } catch (Exception e) {
            mProgress = 1;
            mMax = 300;
        }
    }

    // ---- Touch ------------------------------------------------------------------------------

    private boolean handleTouch(Context context, MotionEvent event, int width) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isOnProgressBar(x, y, width)) {
                    mAllowRefresh = false;
                    setMovement(x, width);
                    return true;
                }
                if (Application_Main.fragmentID == 15) {
                    if (new Preferences(context).getSettingsUIVibration()) {
                        ((Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(5L);
                    }
                    ((Application_Main) getContext()).onBackPressed();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                mAllowRefresh = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isOnProgressBar(x, y, width)) {
                    setMovement(x, width);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /** True when the point is in the lower half of the view and near the rim, where the ring is. */
    public boolean isOnProgressBar(int x, int y, int width) {
        int half = width / 2;
        if (y <= half) {
            return false;
        }
        double distance = Math.hypot(half - x, half - y);
        return half * 0.2 + distance > half;
    }

    // ---- Drawing ----------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        float done = mMax > 0 ? Math.min(1f, (float) mProgress / mMax) : 0f;

        if (mBounds.height() <= mStroke) {
            // A flat bar.
            mPaint.setColor(mBackground);
            mPaint.setStrokeWidth(mHairline);
            canvas.drawLine(mBounds.left, mBounds.top, mBounds.right, mBounds.top, mPaint);
            mPaint.setColor(mForeground);
            mPaint.setStrokeWidth(mStroke);
            canvas.drawLine(mBounds.left, mBounds.top, mBounds.left + mBounds.width() * done, mBounds.top, mPaint);
            return;
        }

        // The ring. While morphing it opens up from a half-turn to its full open shape.
        float start = 180f - 12f * mMorph;
        float sweep = RING_SWEEP * mMorph;
        if (start >= 180f) {
            start = RING_START;
            sweep = RING_SWEEP;
        }
        mPaint.setColor(mBackground);
        mPaint.setStrokeWidth(mHairline);
        canvas.drawArc(mBounds, start, -sweep, false, mPaint);
        mPaint.setColor(mForeground);
        mPaint.setStrokeWidth(mStroke);
        canvas.drawArc(mBounds, start, -sweep * done, false, mPaint);
        drawThumb(canvas, start - sweep * done);
    }

    /** The dot showing where playback is; it swells a little while being dragged. */
    private void drawThumb(Canvas canvas, float angleDegrees) {
        float radius = mAllowRefresh ? mThumbRadius : mThumbRadiusActive;
        double angle = Math.toRadians(angleDegrees);
        float x = mBounds.centerX() + (mBounds.width() / 2f) * (float) Math.cos(angle);
        float y = mBounds.centerY() + (mBounds.height() / 2f) * (float) Math.sin(angle);
        canvas.drawCircle(x, y, radius, mThumbPaint);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int minWidth = Math.max(getSuggestedMinimumWidth(), (int) mStroke);
        int minHeight = Math.max(getSuggestedMinimumHeight(), (int) mStroke);
        setMeasuredDimension(
                resolveSizeAndState(minWidth + getPaddingLeft() + getPaddingRight(), widthSpec, 0),
                resolveSizeAndState(minHeight + getPaddingTop() + getPaddingBottom(), heightSpec, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float inset = mStroke / 2f;
        // On the round player ring, leave room for the dot so it is not clipped at the view edge.
        if (h > mStroke * 4f) {
            inset = Math.max(inset, mThumbRadiusActive);
        }
        mBounds.set(getPaddingLeft() + inset, getPaddingTop() + inset, w - getPaddingRight() - inset, h - getPaddingBottom() - inset);
    }

    // ---- State ------------------------------------------------------------------------------

    /** Posts a position update (position in milliseconds, duration in seconds) to a progress handler. */
    public void refreshProgression(int position, int duration, int fragment, Handler handler) {
        Bundle data = new Bundle();
        data.putInt("fragment", fragment);
        data.putInt("currentposition", position);
        data.putLong("duration", duration);
        Message message = new Message();
        message.setData(data);
        handler.sendMessage(message);
    }

    public void setMax(int max) {
        if (max < 0) {
            max = 100;
        }
        if (mMax != max) {
            mMax = max;
            invalidate();
        }
    }

    public void setMorph(float morph) {
        if (morph != mMorph) {
            mMorph = morph;
            invalidate();
        }
    }

    public void setProgress(int progress) {
        if (progress <= 0 || progress > mMax) {
            progress = 1;
        }
        if (progress == mProgress || !mAllowRefresh) {
            return;
        }
        mProgress = progress;
        invalidate();
    }

    /** Seeks to the spot the finger is at (x across the given width) and updates everything that shows progress. */
    public void setMovement(int x, int width) {
        Application_Main main = (Application_Main) getContext();
        int duration = (int) main.mServiceMusic.musicPlayed.getMusic(getContext()).getDuration();
        int tenths = ((duration * x) / width) * 10;
        if (tenths < 0) {
            tenths = 1;
        } else if (tenths > mMax) {
            tenths = mMax - 1;
        }
        refreshProgression(tenths, duration, main.getFragment(), main.mServiceMusic.getProgressHandler());
        main.seekTo(tenths * 100);
        mProgress = tenths;
        invalidate();
    }
}
