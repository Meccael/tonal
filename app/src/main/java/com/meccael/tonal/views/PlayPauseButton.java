package com.meccael.tonal.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.meccael.tonal.R;
import com.meccael.tonal.application.Application_Main;
import com.meccael.tonal.application.Preferences;
import com.meccael.tonal.application.ServiceMusic;

/**
 * The round play/pause button. The icon is drawn by hand and morphs between two bars (pause) and a
 * triangle (play). Tap to play or pause; hold for a moment to skip to the next song.
 */
public class PlayPauseButton extends FloatingActionButton {
    /** True while music is playing (the button then shows "pause"). Shared by every button on screen. */
    public static boolean mPlayed;

    private static final float ICON_WIDTH = 0.43f;
    private static final float ICON_PADDING = 0.06f;
    private static final long MORPH_MS = 200L;
    private static final long HOLD_MS = 300L;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mLeftShape = new Path();
    private final Path mRightShape = new Path();
    private final Preferences mPreferences;
    private final Vibrator mVibrator;
    private final Animation mPressDown;
    private final Animation mPressUp;

    /** 0 = drawn as pause (two bars), 1 = drawn as play (triangle). */
    private float mMorph;
    private ValueAnimator mMorphAnimator;
    private Handler mHoldHandler;
    private boolean mShortPress;

    public PlayPauseButton(Context context) {
        this(context, null, 0);
    }

    public PlayPauseButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayPauseButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPreferences = new Preferences(context);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mPressDown = AnimationUtils.loadAnimation(getContext(), R.anim.animation_bouton_down);
        mPressUp = AnimationUtils.loadAnimation(getContext(), R.anim.animation_bouton_up);
        mPaint.setColor(0xFFFFFFFF);
        mPaint.setStyle(Paint.Style.FILL);
        snapToState();

        Application_Main main = findMain(context);
        if (main != null) {
            setBackgroundTint(main.getThemeColorVibrant());
            setColor(main.getThemeColorFab());
        }
    }

    private static Application_Main findMain(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Application_Main) {
                return (Application_Main) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    // ---- Public API -------------------------------------------------------------------------

    public void setBackgroundTint(int color) {
        setBackgroundTintList(ColorStateList.valueOf(color));
    }

    /** Colour of the play/pause icon. */
    public void setColor(int color) {
        mPaint.setColor(color);
        invalidate();
    }

    public void setPlayed(boolean played) {
        mPlayed = played;
        invalidate();
    }

    /** Updates the icon: animates if the state changed, otherwise just makes sure it is drawn correctly. */
    public void showPlay(boolean played) {
        if (mPlayed != played) {
            setPlayed(played);
            startAnimation();
        } else {
            snapToState();
        }
        invalidate();
    }

    /** Morphs the icon to match the current play state. */
    public void startAnimation() {
        if (mMorphAnimator != null) {
            mMorphAnimator.cancel();
        }
        mMorphAnimator = ValueAnimator.ofFloat(mMorph, mPlayed ? 0f : 1f);
        mMorphAnimator.setDuration(MORPH_MS);
        mMorphAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mMorph = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mMorphAnimator.start();
    }

    private void snapToState() {
        if (mMorphAnimator != null) {
            mMorphAnimator.cancel();
        }
        mMorph = mPlayed ? 0f : 1f;
    }

    // ---- Drawing ----------------------------------------------------------------------------

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float halfW = canvas.getWidth() / 2f;
        float halfH = canvas.getHeight() / 2f;

        // The four numbers that describe the shape, each moving from its "pause" to its "play" value.
        float pad = lerp(0f, ICON_PADDING, mMorph);
        float mid = lerp(1f, 0.5f, mMorph);
        float edge = lerp(-ICON_WIDTH * ICON_WIDTH, 0f, mMorph);
        float tip = lerp(1f, 0f, mMorph);

        float leftOuter = halfW * (1f + (-ICON_WIDTH * 0.5f + pad));
        float leftInner = halfW * (1f + pad + ICON_WIDTH * edge);
        float rightInner = halfW * (1f + pad - ICON_WIDTH * edge);
        float rightOuter = halfW * (1f + (ICON_WIDTH * 0.5f + pad));

        mLeftShape.reset();
        mLeftShape.moveTo(leftOuter, halfH * 1.25f);
        mLeftShape.lineTo(leftInner, halfH * (1f + mid * 0.25f));
        mLeftShape.lineTo(leftInner, halfH * (1f - mid * 0.25f));
        mLeftShape.lineTo(leftOuter, halfH * 0.75f);

        mRightShape.reset();
        mRightShape.moveTo(rightInner, halfH * (1f + mid * 0.25f));
        mRightShape.lineTo(rightOuter, halfH * (1f + tip * 0.25f));
        mRightShape.lineTo(rightOuter, halfH * (1f - tip * 0.25f));
        mRightShape.lineTo(rightInner, halfH * (1f - mid * 0.25f));

        canvas.drawPath(mLeftShape, mPaint);
        canvas.drawPath(mRightShape, mPaint);
    }

    // ---- Touch ------------------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onPressed();
                return true;
            case MotionEvent.ACTION_UP:
                onReleased();
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelHold();
                if (mShortPress) {
                    startAnimation(mPressUp);
                }
                return true;
            default:
                return false;
        }
    }

    private void onPressed() {
        mShortPress = true;
        startAnimation(mPressDown);
        if (mPreferences.getSettingsUIVibration()) {
            mVibrator.vibrate(10L);
        }
        playSoundEffect(0);
        mHoldHandler = new Handler();
        mHoldHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Held long enough: skip to the next song (not while the Player screen is open).
                mShortPress = false;
                startAnimation(mPressUp);
                Application_Main main = findMain(getContext());
                if (main != null && Application_Main.fragmentID != 15 && main.getServiceMusic() != null) {
                    main.getServiceMusic().next(getContext());
                }
            }
        }, HOLD_MS);
    }

    private void onReleased() {
        cancelHold();
        if (!mShortPress) {
            return;
        }
        startAnimation(mPressUp);
        Application_Main main = findMain(getContext());
        if (main == null || main.getServiceMusic() == null) {
            return;
        }
        ServiceMusic service = main.getServiceMusic();
        if (service.isPlaying()) {
            service.pause();
        } else {
            service.play(getContext(), false);
        }
    }

    private void cancelHold() {
        if (mHoldHandler != null) {
            mHoldHandler.removeCallbacksAndMessages(null);
        }
    }
}
