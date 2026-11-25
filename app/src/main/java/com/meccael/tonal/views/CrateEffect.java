package com.meccael.tonal.views;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * "Digging through a crate of records" look for a vertical list of square covers.
 * Covers overlap so only a strip of each one behind the front cover shows. The ones behind
 * lean back in 3D; as you scroll, the front cover tips forward and fades while the next
 * one comes up to the front. Scrolling settles with one cover in front.
 *
 * Held sideways (landscape) the crate runs left to right instead: the front cover sits at the left
 * with the next ones fanned out to its right, and you swipe to bring the next one forward.
 */
public class CrateEffect extends RecyclerView.OnScrollListener
        implements RecyclerView.OnChildAttachStateChangeListener {

    /** Visible strip of each cover behind the front one, as a fraction of cover height. */
    private static final float STEP = 0.22f;
    /** How far (degrees) covers behind the front lean back. Flip the sign if they lean the wrong way. */
    private static final float TILT_BACK = 38f;
    /** How far (degrees) the front cover tips forward as it leaves. */
    private static final float TILT_OUT = -55f;
    private static final float SHRINK_PER_LEVEL = 0.035f;
    private static final float FADE_PER_LEVEL = 0.07f;

    private final RecyclerView rv;
    private final float density;
    private final int topPad;
    private final boolean horizontal;
    private final int leftPad;
    private int basePaddingBottom;
    private boolean snapping;

    private CrateEffect(RecyclerView rv, int basePaddingBottom) {
        this.rv = rv;
        this.density = rv.getResources().getDisplayMetrics().density;
        this.topPad = (int) (12 * density);
        this.leftPad = (int) (56 * density);
        this.horizontal = rv.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        this.basePaddingBottom = basePaddingBottom;
    }

    /** Turns a RecyclerView into a record crate. Call after setting the adapter is fine, before is fine too. */
    public static void install(RecyclerView rv, int basePaddingBottom) {
        final CrateEffect effect = new CrateEffect(rv, basePaddingBottom);
        int side = (int) (40 * effect.density);
        rv.setClipToPadding(false);
        rv.setClipChildren(false);
        if (effect.horizontal) {
            rv.setPadding(effect.leftPad, effect.topPad, side, basePaddingBottom);
            rv.setLayoutManager(new LinearLayoutManager(rv.getContext(), LinearLayoutManager.HORIZONTAL, false));
        } else {
            rv.setPadding(side, effect.topPad, side, basePaddingBottom);
            rv.setLayoutManager(new LinearLayoutManager(rv.getContext()));
        }
        rv.setItemAnimator(null);
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect rect, View view, RecyclerView parent, RecyclerView.State state) {
                int cover = (int) effect.coverSize();
                if (effect.horizontal) {
                    rect.right = -(int) (cover * (1f - STEP));
                } else {
                    rect.bottom = -(int) (cover * (1f - STEP));
                }
            }
        });
        rv.addOnScrollListener(effect);
        rv.addOnChildAttachStateChangeListener(effect);
        rv.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                if (effect.horizontal) {
                    effect.fitEndPadding();
                    effect.sizeChildren();
                } else {
                    effect.fitSidePadding();
                    effect.fitBottomPadding();
                }
                effect.apply();
            }
        });
    }

    private float coverSize() {
        if (horizontal) {
            // Square covers that fit the height, with a little room for the tilt.
            float room = rv.getHeight() - rv.getPaddingTop() - rv.getPaddingBottom();
            return Math.max(0f, room * 0.94f);
        }
        return rv.getWidth() - rv.getPaddingLeft() - rv.getPaddingRight();
    }

    /** Sideways: each cover is as wide as it is tall. */
    private void sizeChildren() {
        int size = (int) coverSize();
        if (size <= 0) return;
        boolean changed = false;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            android.view.ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp != null && lp.width != size) {
                lp.width = size;
                changed = true;
            }
        }
        if (changed) {
            rv.invalidateItemDecorations();
            rv.requestLayout();
        }
    }

    /** Sideways: lets the last cover scroll all the way to the front position. */
    private void fitEndPadding() {
        float step = coverSize() * STEP;
        int needed = (int) (rv.getWidth() - leftPad - step);
        int pr = Math.max((int) (40 * density), needed);
        if (pr != rv.getPaddingRight() && rv.getWidth() > 0) {
            rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(), pr, rv.getPaddingBottom());
        }
    }

    /** On a short, wide screen (landscape) keep the covers square and a sensible size, centered. */
    private void fitSidePadding() {
        int w = rv.getWidth();
        int h = rv.getHeight();
        if (w <= 0 || h <= 0) return;
        int base = (int) (40 * density);
        float maxCover = h * 0.62f;
        int side = base;
        if (w - 2f * base > maxCover) {
            side = (int) ((w - maxCover) / 2f);
        }
        if (rv.getPaddingLeft() != side || rv.getPaddingRight() != side) {
            rv.setPadding(side, rv.getPaddingTop(), side, rv.getPaddingBottom());
            // The overlap between covers depends on the cover size, so work it out again.
            rv.invalidateItemDecorations();
        }
    }

    /** Lets the last cover scroll all the way up to the front position. */
    private void fitBottomPadding() {
        float step = coverSize() * STEP;
        int needed = (int) (rv.getHeight() - topPad - step);
        int pb = Math.max(basePaddingBottom, needed);
        if (pb != rv.getPaddingBottom() && rv.getHeight() > 0) {
            rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(), rv.getPaddingRight(), pb);
        }
    }

    private void apply() {
        float step = coverSize() * STEP;
        if (step <= 0) return;
        int n = rv.getChildCount();
        for (int i = 0; i < n; i++) {
            View child = rv.getChildAt(i);
            float f = horizontal
                    ? (child.getLeft() - leftPad) / step
                    : (child.getTop() - topPad) / step; // 0 = front, 1 = next one back, ...
            child.setPivotX(child.getWidth() / 2f);
            child.setPivotY(child.getHeight() / 2f);
            child.setCameraDistance(8000f * density);
            child.setTranslationZ(n - i); // earlier covers draw on top of later ones
            View ring = child.findViewById(com.meccael.tonal.R.id.album_ring);
            if (ring != null) {
                ring.setAlpha(0.28f + 0.55f * Math.max(0f, 1f - Math.abs(f)));
            }
            if (f >= 0f) {
                float depth = Math.min(f, 8f);
                float s = 1f - SHRINK_PER_LEVEL * depth;
                if (horizontal) {
                    child.setRotationY(-TILT_BACK * Math.min(f, 1f));
                } else {
                    child.setRotationX(TILT_BACK * Math.min(f, 1f));
                }
                child.setScaleX(s);
                child.setScaleY(s);
                child.setAlpha(Math.max(0.35f, 1f - FADE_PER_LEVEL * depth));
            } else {
                float p = Math.min(-f, 1f);
                if (horizontal) {
                    child.setRotationY(-TILT_OUT * p);
                } else {
                    child.setRotationX(TILT_OUT * p);
                }
                child.setScaleX(1f + 0.04f * p);
                child.setScaleY(1f + 0.04f * p);
                child.setAlpha(1f - p);
            }
        }
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        apply();
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (newState != RecyclerView.SCROLL_STATE_IDLE) {
            snapping = false;
            return;
        }
        if (snapping) {
            snapping = false;
            return;
        }
        // Settle with the nearest cover in the front position.
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View c = recyclerView.getChildAt(i);
            int d = horizontal ? c.getLeft() - leftPad : c.getTop() - topPad;
            if (Math.abs(d) < Math.abs(best)) best = d;
        }
        if (best != Integer.MAX_VALUE && Math.abs(best) > 1) {
            snapping = true;
            if (horizontal) {
                recyclerView.smoothScrollBy(best, 0);
            } else {
                recyclerView.smoothScrollBy(0, best);
            }
        }
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        if (horizontal) {
            int size = (int) coverSize();
            android.view.ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (size > 0 && lp != null) {
                lp.width = size;
            }
        }
        apply();
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        view.setRotationX(0f);
        view.setRotationY(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setAlpha(1f);
        view.setTranslationZ(0f);
    }
}
