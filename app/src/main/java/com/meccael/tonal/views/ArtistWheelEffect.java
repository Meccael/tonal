package com.meccael.tonal.views;

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Spotlight wheel for a vertical list of artists (item_list_card_artist, 104dp tall).
 * The artist in the middle of the screen is full size and sits furthest right; the ones above and
 * below curve away to the left, shrink and fade, like the rim of a dial. Scrolling settles with
 * one artist in the spotlight. Tapping another artist rolls it to the middle first.
 */
public class ArtistWheelEffect extends RecyclerView.OnScrollListener
        implements RecyclerView.OnChildAttachStateChangeListener {

    private static final float ITEM_DP = 104f;
    private static final float ARC_DP = 70f;

    private final RecyclerView rv;
    private final float density;
    private final int basePaddingBottom;
    private boolean snapping;

    private ArtistWheelEffect(RecyclerView rv, int basePaddingBottom) {
        this.rv = rv;
        this.density = rv.getResources().getDisplayMetrics().density;
        this.basePaddingBottom = basePaddingBottom;
    }

    public static void install(RecyclerView rv, int basePaddingBottom) {
        final ArtistWheelEffect effect = new ArtistWheelEffect(rv, basePaddingBottom);
        rv.setClipToPadding(false);
        rv.setClipChildren(false);
        rv.setLayoutManager(new LinearLayoutManager(rv.getContext()));
        rv.setItemAnimator(null);
        rv.addOnScrollListener(effect);
        rv.addOnChildAttachStateChangeListener(effect);
        rv.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                effect.fitPadding();
                effect.apply();
            }
        });
    }

    /** True if this artist is already in the spotlight; otherwise rolls it there and returns false. */
    public static boolean bringToFront(RecyclerView rv, View child) {
        float cy = child.getTop() + child.getHeight() / 2f;
        float dy = cy - rv.getHeight() / 2f;
        if (Math.abs(dy) < child.getHeight() / 2f) return true;
        rv.smoothScrollBy(0, Math.round(dy));
        return false;
    }

    /** Lets the first and last artist reach the middle of the screen. */
    private void fitPadding() {
        if (rv.getHeight() <= 0) return;
        int pad = Math.max(0, (int) ((rv.getHeight() - ITEM_DP * density) / 2f));
        int bottom = Math.max(basePaddingBottom, pad);
        boolean land = rv.getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int right = land ? (int) (rv.getWidth() * 0.34f) : rv.getPaddingRight();
        if (rv.getPaddingTop() != pad || rv.getPaddingBottom() != bottom || rv.getPaddingRight() != right) {
            rv.setPadding(rv.getPaddingLeft(), pad, right, bottom);
        }
    }

    private void apply() {
        float centerY = rv.getHeight() / 2f;
        float itemPx = ITEM_DP * density;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            float cy = child.getTop() + child.getHeight() / 2f;
            float a = Math.abs((cy - centerY) / itemPx);
            float c = Math.min(a, 2.5f);
            child.setPivotX(0f);
            child.setPivotY(child.getHeight() / 2f);
            child.setTranslationX(-ARC_DP * density * (1f - (float) Math.cos(c * 0.55f)));
            float s = 1f - 0.14f * Math.min(a, 3f);
            child.setScaleX(s);
            child.setScaleY(s);
            child.setAlpha(Math.max(0.25f, 1f - 0.3f * c));
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
        float centerY = recyclerView.getHeight() / 2f;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View c = recyclerView.getChildAt(i);
            float d = c.getTop() + c.getHeight() / 2f - centerY;
            if (Math.abs(d) < Math.abs(best)) best = d;
        }
        if (best != Float.MAX_VALUE && Math.abs(best) > 1f) {
            snapping = true;
            recyclerView.smoothScrollBy(0, Math.round(best));
        }
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        apply();
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        view.setTranslationX(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setAlpha(1f);
    }
}
