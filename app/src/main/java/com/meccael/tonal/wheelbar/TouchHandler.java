package com.meccael.tonal.wheelbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/* JADX INFO: loaded from: classes.dex */
class TouchHandler extends GestureDetector.SimpleOnGestureListener {
    private static final float FLING_ANGLE_MULTIPLIER = 2.0E-4f;
    private static final Interpolator INTERPOLATOR = new DecelerateInterpolator(2.5f);
    private static final float SCROLL_ANGLE_MULTIPLIER = 0.002f;
    private static final int SETTLING_DURATION_MULTIPLIER = 1000;
    private GestureDetector gestureDetector;
    private HorizontalWheelView.Listener listener;
    private ValueAnimator settlingAnimator;
    private boolean snapToMarks;
    private HorizontalWheelView view;
    private int scrollState = 0;
    private ValueAnimator.AnimatorUpdateListener flingAnimatorListener = new ValueAnimator.AnimatorUpdateListener() { // from class: com.meccael.tonal.wheelbar.TouchHandler.1
        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            TouchHandler.this.view.setRadiansAngle(((Float) valueAnimator.getAnimatedValue()).floatValue());
        }
    };
    private Animator.AnimatorListener animatorListener = new AnimatorListenerAdapter() { // from class: com.meccael.tonal.wheelbar.TouchHandler.2
        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            TouchHandler.this.updateScrollStateIfRequired(0);
        }
    };

    TouchHandler(HorizontalWheelView horizontalWheelView) {
        this.view = horizontalWheelView;
        this.gestureDetector = new GestureDetector(horizontalWheelView.getContext(), this);
    }

    private double findNearestMarkAngle(double d) {
        double marksCount = 6.283185307179586d / ((double) this.view.getMarksCount());
        return Math.round(d / marksCount) * marksCount;
    }

    private void playSettlingAnimation(double d) {
        updateScrollStateIfRequired(2);
        double radiansAngle = this.view.getRadiansAngle();
        this.settlingAnimator = ValueAnimator.ofFloat((float) radiansAngle, (float) d).setDuration((int) (Math.abs(radiansAngle - d) * 1000.0d));
        this.settlingAnimator.setInterpolator(INTERPOLATOR);
        this.settlingAnimator.addUpdateListener(this.flingAnimatorListener);
        this.settlingAnimator.addListener(this.animatorListener);
        this.settlingAnimator.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateScrollStateIfRequired(int i) {
        if (this.listener == null || this.scrollState == i) {
            return;
        }
        this.scrollState = i;
        this.listener.onScrollStateChanged(i);
    }

    void cancelFling() {
        if (this.scrollState == 2) {
            this.settlingAnimator.cancel();
        }
    }

    @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
    public boolean onDown(MotionEvent motionEvent) {
        cancelFling();
        return true;
    }

    @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        double radiansAngle = this.view.getRadiansAngle() - ((double) (FLING_ANGLE_MULTIPLIER * f));
        if (this.snapToMarks) {
            radiansAngle = (float) findNearestMarkAngle(radiansAngle);
        }
        playSettlingAnimation(radiansAngle);
        return true;
    }

    @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        this.view.setRadiansAngle(this.view.getRadiansAngle() + ((double) (SCROLL_ANGLE_MULTIPLIER * f)));
        updateScrollStateIfRequired(1);
        return true;
    }

    boolean onTouchEvent(MotionEvent motionEvent) {
        this.gestureDetector.onTouchEvent(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        if (this.scrollState != 2 && (actionMasked == 1 || actionMasked == 3)) {
            if (this.snapToMarks) {
                playSettlingAnimation(findNearestMarkAngle(this.view.getRadiansAngle()));
            } else {
                updateScrollStateIfRequired(0);
            }
        }
        return true;
    }

    void setListener(HorizontalWheelView.Listener listener) {
        this.listener = listener;
    }

    void setSnapToMarks(boolean z) {
        this.snapToMarks = z;
    }
}
