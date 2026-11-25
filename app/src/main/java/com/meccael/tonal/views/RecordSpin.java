package com.meccael.tonal.views;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Spins a record picture slowly while music plays and holds it still while paused. */
public class RecordSpin {
    private static final long TURN_MS = 6000L;

    private ObjectAnimator mAnimator;

    public RecordSpin(View record) {
        mAnimator = ObjectAnimator.ofFloat(record, View.ROTATION, 0f, 360f);
        mAnimator.setDuration(TURN_MS);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
    }

    /** Starts, resumes or pauses the spin to match whether music is playing. */
    public void sync(boolean playing) {
        if (mAnimator == null) {
            return;
        }
        if (playing) {
            if (!mAnimator.isStarted()) {
                mAnimator.start();
            } else if (mAnimator.isPaused()) {
                mAnimator.resume();
            }
        } else if (mAnimator.isRunning()) {
            mAnimator.pause();
        }
    }

    public void cancel() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
    }
}
