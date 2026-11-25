package com.meccael.tonal.views;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Bubble field for a list of genres (item_list_card_genre). Genres sit in three loose columns,
 * each bubble sized by how many tracks it holds. The bubbles near the middle of the screen swell
 * and the ones near the top and bottom shrink and fade, like looking through a lens.
 */
public class GenreBubblesEffect extends RecyclerView.OnScrollListener
        implements RecyclerView.OnChildAttachStateChangeListener {

    private final RecyclerView rv;

    private GenreBubblesEffect(RecyclerView rv) {
        this.rv = rv;
    }

    public static void install(RecyclerView rv, int basePaddingBottom) {
        final GenreBubblesEffect effect = new GenreBubblesEffect(rv);
        float density = rv.getResources().getDisplayMetrics().density;
        int side = (int) (6 * density);
        rv.setClipToPadding(false);
        rv.setPadding(side, (int) (12 * density), side, basePaddingBottom);
        boolean land = rv.getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        StaggeredGridLayoutManager lm = new StaggeredGridLayoutManager(land ? 6 : 3, StaggeredGridLayoutManager.VERTICAL);
        lm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        rv.setLayoutManager(lm);
        rv.setItemAnimator(null);
        rv.addOnScrollListener(effect);
        rv.addOnChildAttachStateChangeListener(effect);
        rv.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                effect.apply();
            }
        });
    }

    private void apply() {
        float half = rv.getHeight() / 2f;
        if (half <= 0f) return;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            float cy = child.getTop() + child.getHeight() / 2f;
            float d = Math.min(Math.abs(cy - half) / half, 1.2f);
            float s = Math.max(0.6f, 1f - 0.28f * d * d);
            child.setPivotX(child.getWidth() / 2f);
            child.setPivotY(child.getHeight() / 2f);
            child.setScaleX(s);
            child.setScaleY(s);
            child.setAlpha(Math.max(0.35f, 1f - 0.5f * d));
        }
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        apply();
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        apply();
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setAlpha(1f);
    }
}
