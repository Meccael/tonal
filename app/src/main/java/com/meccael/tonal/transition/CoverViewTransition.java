package com.meccael.tonal.transition;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.util.Property;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import com.meccael.tonal.R;
import com.meccael.tonal.application.CallbacksInterface;
import com.meccael.tonal.views.CoverView;

/**
 * Grows or shrinks a cover between a square card and a round record as a screen opens or closes.
 * The activity sets a transition id (for example 1512 = Home to Player) and this picks the start
 * and end size to match, then moves the id on to its "return trip" value.
 */
public class CoverViewTransition extends Transition {
    private static final Property<CoverView, Float> RADIUS = new Property<CoverView, Float>(Float.class, "radius") {
        @Override
        public Float get(CoverView view) {
            return view.getTransitionRadius();
        }

        @Override
        public void set(CoverView view, Float value) {
            view.setTransitionRadius(value);
        }
    };

    private static final String STATE_KEY = "com.meccael.tonal.transition:CoverViewTransition:state";
    private static final String RADIUS_KEY = "com.meccael.tonal.transition:CoverViewTransition:radius";

    private int mShape;

    public CoverViewTransition(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CoverView);
        mShape = array.getInt(1, CoverView.SHAPE_RECTANGLE);
        array.recycle();
    }

    private void capture(TransitionValues values, String state) {
        if (values.view instanceof CoverView) {
            values.values.put(STATE_KEY, state);
            values.values.put(RADIUS_KEY, ((CoverView) values.view).getTransitionRadius());
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
        if (end == null || !(end.view instanceof CoverView)) {
            return null;
        }
        CallbacksInterface host = (CallbacksInterface) sceneRoot.getContext();
        CoverView cover = (CoverView) end.view;
        float minRadius = cover.getMinRadius();
        float maxRadius = cover.getMaxRadius();
        float width = host.getScreenWidth();
        float height = host.getScreenHeight() + host.getPaddingBottom(sceneRoot.getContext());
        int id = host.getTransitionID();

        float from;
        float to;
        TimeInterpolator curve;
        switch (id) {
            case 1012:
                mShape = CoverView.SHAPE_RECTANGLE;
                from = width;
                to = width;
                curve = new AccelerateDecelerateInterpolator();
                break;
            case 1215:
                mShape = CoverView.SHAPE_CIRCLE;
                from = width * 0.6f;
                to = height * 0.21f;
                curve = new DecelerateInterpolator();
                break;
            case 1512:
                mShape = CoverView.SHAPE_CIRCLE;
                from = height * 0.21f;
                to = width * 0.6f;
                curve = new AccelerateInterpolator();
                break;
            case 1520:
                mShape = CoverView.SHAPE_CIRCLE;
                from = height * 0.21f;
                to = height * 0.17f;
                curve = new AccelerateDecelerateInterpolator();
                break;
            case 2015:
                mShape = CoverView.SHAPE_CIRCLE;
                from = height * 0.17f;
                to = height * 0.21f;
                curve = new AccelerateDecelerateInterpolator();
                break;
            default:
                int screen = host.getFragment();
                if (screen == 20 || screen == 12) {
                    mShape = CoverView.SHAPE_CIRCLE;
                }
                if (mShape == CoverView.SHAPE_RECTANGLE) {
                    from = maxRadius;
                    to = minRadius;
                    mShape = CoverView.SHAPE_CIRCLE;
                    curve = new DecelerateInterpolator();
                } else {
                    from = minRadius * 1.2f;
                    to = maxRadius;
                    curve = new AccelerateInterpolator();
                }
                break;
        }

        // Line up the id for the trip back.
        switch (id) {
            case 1215: host.setTransitionID(1512); break;
            case 1015: host.setTransitionID(1510); break;
            case 1520: host.setTransitionID(2010); break;
            case 2015: host.setTransitionID(1520); break;
            case 1012: host.setTransitionID(1210); break;
            default: break;
        }

        cover.setTransitionRadius(from);
        AnimatorSet set = new AnimatorSet();
        set.setInterpolator(curve);
        set.play(ObjectAnimator.ofFloat(cover, RADIUS, from, to));
        return set;
    }
}
