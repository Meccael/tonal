package com.meccael.tonal.views;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Filing-cabinet look for a vertical list of folders (item_list_card_folder, 190dp tall).
 * Folders are stacked so only each one's tab strip shows. The folder in focus is raised in
 * front and opens up, pushing the folders after it down so its whole body is visible.
 * Scrolling settles with one folder open. Tapping a folder that is not open brings it up first.
 */
public class CabinetEffect extends RecyclerView.OnScrollListener
        implements RecyclerView.OnChildAttachStateChangeListener {

    private static final float ITEM_DP = 190f;
    /** Visible strip (the tab) of each folder that is not open. */
    private static final float STEP_DP = 58f;
    private static final float LIFT_DP = 12f;
    private static final float SHRINK_PER_LEVEL = 0.02f;
    private static final float FADE_PER_LEVEL = 0.06f;

    private final RecyclerView rv;
    private final float density;
    private final int topPad;
    private final int basePaddingBottom;
    private boolean snapping;

    private CabinetEffect(RecyclerView rv, int basePaddingBottom) {
        this.rv = rv;
        this.density = rv.getResources().getDisplayMetrics().density;
        this.topPad = (int) (24 * density);
        this.basePaddingBottom = basePaddingBottom;
    }

    public static void install(RecyclerView rv, int basePaddingBottom) {
        final CabinetEffect effect = new CabinetEffect(rv, basePaddingBottom);
        int side = (int) (16 * effect.density);
        rv.setClipToPadding(false);
        rv.setClipChildren(false);
        rv.setPadding(side, effect.topPad, side, basePaddingBottom);
        rv.setLayoutManager(new LinearLayoutManager(rv.getContext()));
        rv.setItemAnimator(null);
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect rect, View view, RecyclerView parent, RecyclerView.State state) {
                rect.bottom = -(int) ((ITEM_DP - STEP_DP) * effect.density);
            }
        });
        rv.addOnScrollListener(effect);
        rv.addOnChildAttachStateChangeListener(effect);
        rv.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                effect.fitBottomPadding();
                effect.apply();
            }
        });
    }

    /**
     * Call when a folder is tapped. Returns true if it is already open (so the caller should
     * open it); otherwise scrolls it to the open position and returns false.
     */
    public static boolean bringToFront(RecyclerView rv, View child) {
        float step = STEP_DP * rv.getResources().getDisplayMetrics().density;
        int dy = child.getTop() - rv.getPaddingTop();
        if (Math.abs(dy) < step / 2f) return true;
        rv.smoothScrollBy(0, dy);
        return false;
    }

    private void fitBottomPadding() {
        float step = STEP_DP * density;
        int needed = (int) (rv.getHeight() - topPad - step);
        int pb = Math.max(basePaddingBottom, needed);
        if (pb != rv.getPaddingBottom() && rv.getHeight() > 0) {
            rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(), rv.getPaddingRight(), pb);
        }
    }

    private void apply() {
        float step = STEP_DP * density;
        float open = (ITEM_DP - STEP_DP) * density;
        int n = rv.getChildCount();
        for (int i = 0; i < n; i++) {
            View child = rv.getChildAt(i);
            float f = (child.getTop() - topPad) / step; // 0 = open folder, 1 = next one, -1 = previous
            float a = Math.abs(f);
            float p = 1f - Math.min(a, 1f); // 1 when open, 0 when at least one step away
            float shift = open * Math.max(0f, Math.min(f, 1f)); // folders after the open one drop down
            child.setTranslationY(shift - LIFT_DP * density * p);
            float depth = Math.min(a, 6f);
            float s = 1f - SHRINK_PER_LEVEL * depth;
            child.setPivotX(child.getWidth() / 2f);
            child.setPivotY(0f);
            child.setScaleX(s);
            child.setScaleY(s);
            child.setAlpha(Math.max(0.4f, 1f - FADE_PER_LEVEL * depth));
            // Later folders sit in front of earlier ones, except the open folder, which is in front of all.
            child.setTranslationZ(i + 1000f * p);
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
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            int d = recyclerView.getChildAt(i).getTop() - topPad;
            if (Math.abs(d) < Math.abs(best)) best = d;
        }
        if (best != Integer.MAX_VALUE && Math.abs(best) > 1) {
            snapping = true;
            recyclerView.smoothScrollBy(0, best);
        }
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        view.setOutlineProvider(null); // no drop shadow from the raised Z
        apply();
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        view.setTranslationY(0f);
        view.setTranslationZ(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setAlpha(1f);
    }
}
