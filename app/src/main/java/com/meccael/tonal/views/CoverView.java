package com.meccael.tonal.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import androidx.appcompat.widget.AppCompatImageView;
import com.meccael.tonal.R;
import com.meccael.tonal.application.Application_Main;
import com.meccael.tonal.application.CallbacksInterface;

/**
 * The cover picture. It can be square with rounded corners or a round record, spins slowly while
 * music plays, and eases back upright when the song stops or changes.
 */
public class CoverView extends AppCompatImageView implements Animatable {
    public static int SHAPE_RECTANGLE = 0;
    public static int SHAPE_CIRCLE = 1;

    /** The slow endless spin. Shared: the most recently created cover owns it. */
    public static ValueAnimator mStartAnimator;

    private static final long SPIN_MS = 12000L;

    private final Path mClipPath = new Path();
    private final Path mRectPath = new Path();
    private CallbacksInterface mActivity;
    private ValueAnimator mEndAnimator;
    private float mRadius;
    private int mShape = SHAPE_RECTANGLE;

    public CoverView(Context context) {
        this(context, null, 0);
    }

    public CoverView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CoverView(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mActivity = findCallbacks(context);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CoverView, 0, 0);
        try {
            mShape = array.getInteger(R.styleable.CoverView_shape, SHAPE_RECTANGLE);
        } finally {
            array.recycle();
        }

        ValueAnimator spin = ValueAnimator.ofFloat(0f, 360f);
        spin.setDuration(SPIN_MS);
        spin.setRepeatCount(ValueAnimator.INFINITE);
        spin.setRepeatMode(ValueAnimator.RESTART);
        spin.setInterpolator(new LinearInterpolator());
        spin.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setRotation((Float) animation.getAnimatedValue());
            }
        });
        // When the spin is cancelled (skip to another song, stop), ease back to upright; on a skip,
        // then show the new cover and start spinning again.
        spin.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                settleToUpright(context);
            }
        });
        mStartAnimator = spin;
    }

    private static CallbacksInterface findCallbacks(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof CallbacksInterface) {
                return (CallbacksInterface) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    // ---- Shape ------------------------------------------------------------------------------

    public void setShape(int shape) {
        mShape = shape;
        invalidate();
    }

    public float getTransitionRadius() {
        return mRadius;
    }

    public void setTransitionRadius(float radius) {
        mRadius = radius;
        invalidate();
    }

    public float getMinRadius() {
        return mRadius;
    }

    public float getMaxRadius() {
        return mRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mShape == SHAPE_CIRCLE) {
            mClipPath.reset();
            mClipPath.addCircle(getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()) / 2f, Path.Direction.CW);
            canvas.clipPath(mClipPath);
        } else if (mRadius > 0f) {
            mRectPath.reset();
            mRectPath.addRoundRect(0f, 0f, getWidth(), getHeight(), mRadius, mRadius, Path.Direction.CW);
            canvas.clipPath(mRectPath);
        }
        super.onDraw(canvas);
    }

    // ---- Spinning ---------------------------------------------------------------------------

    @Override
    public void start() {
        if (mStartAnimator != null) {
            mStartAnimator.start();
        }
    }

    @Override
    public void stop() {
        if (mStartAnimator != null) {
            mStartAnimator.cancel();
        }
    }

    @Override
    public boolean isRunning() {
        return (mStartAnimator != null && mStartAnimator.isRunning())
                || (mEndAnimator != null && mEndAnimator.isRunning());
    }

    public void animateVinyl() {
        settleToUpright(getContext());
    }

    /** Turns the cover the short way back to upright. After a skip, swaps in the new cover and resumes the spin. */
    private void settleToUpright(final Context context) {
        if (mEndAnimator != null && mEndAnimator.isRunning()) {
            mEndAnimator.cancel();
        }
        float angle = getRotation() % 360f;
        if (angle < 0f) {
            angle += 360f;
        }
        float target = angle > 180f ? 360f : 0f;

        mEndAnimator = ValueAnimator.ofFloat(angle, target);
        mEndAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mEndAnimator.setDuration(getResources().getInteger(R.integer.transition_duration_coverback));
        mEndAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setRotation((Float) animation.getAnimatedValue());
            }
        });
        mEndAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (mActivity != null && mActivity.getPressNext() && mActivity.getServiceMusic() != null) {
                    try {
                        long cover = mActivity.getServiceMusic().musicPlayed.getMusic(getContext()).getCover();
                        mActivity.updateBackground(context, cover, R.drawable.cover0, 300);
                    } catch (Exception ignored) {
                        // no current song yet
                    }
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setRotation(0f);
                if (mActivity != null && mActivity.getPressNext()) {
                    setImageBitmap(mActivity.getThemeArt());
                    Application_Main.pressNextPrevious = false;
                    if (mStartAnimator != null) {
                        mStartAnimator.start();
                    }
                }
            }
        });
        mEndAnimator.start();
    }
}
