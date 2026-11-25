package com.meccael.tonal.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.ViewGroup;
import com.meccael.tonal.views.ProgressView;

/** Morphs the progress ring between its two shapes when the player opens or closes. */
public class ProgressViewTransition extends Transition {
    private static final String KEY = "morph";

    public ProgressViewTransition(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void capture(TransitionValues values, String state) {
        if (values.view instanceof ProgressView) {
            values.values.put(KEY, state);
        }
    }

    @Override
    public void captureStartValues(TransitionValues values) {
        capture(values, "start");
    }

    @Override
    public void captureEndValues(TransitionValues values) {
        capture(values, "end");
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues start, TransitionValues end) {
        if (end == null || !(end.view instanceof ProgressView)) {
            return null;
        }
        ProgressView ring = (ProgressView) end.view;
        ring.setMorph(1f);
        return ObjectAnimator.ofFloat(ring, KEY, 1f, 0f);
    }
}
