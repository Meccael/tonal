package com.meccael.tonal.views;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.meccael.tonal.R;

/**
 * A pile of cassette tapes for a vertical list of playlists (item_list_card_cassette).
 * Each tape is tilted a little, like a casual stack. The tape in the middle of the screen is full
 * size and the others sit back a little. The reels on every tape turn as you scroll, as if the
 * tape were winding.
 */
public class CassetteEffect extends RecyclerView.OnScrollListener
        implements RecyclerView.OnChildAttachStateChangeListener {

    private final RecyclerView rv;
    private final boolean horizontal;
    private final int itemWidth;
    private boolean snapping;

    private CassetteEffect(RecyclerView rv) {
        this.rv = rv;
        float density = rv.getResources().getDisplayMetrics().density;
        this.horizontal = rv.getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        this.itemWidth = (int) (300 * density);
    }

    public static void install(RecyclerView rv, int basePaddingBottom) {
        final CassetteEffect effect = new CassetteEffect(rv);
        final float density = rv.getResources().getDisplayMetrics().density;
        int side = (int) (20 * density);
        rv.setClipToPadding(false);
        rv.setClipChildren(false);
        rv.setPadding(side, (int) (14 * density), side, basePaddingBottom);
        if (effect.horizontal) {
            rv.setLayoutManager(new LinearLayoutManager(rv.getContext(), LinearLayoutManager.HORIZONTAL, false));
        } else {
            rv.setLayoutManager(new LinearLayoutManager(rv.getContext()));
        }
        rv.setItemAnimator(null);
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect rect, View view, RecyclerView parent, RecyclerView.State state) {
                if (effect.horizontal) {
                    rect.right = (int) (-30 * density);
                } else {
                    rect.bottom = (int) (18 * density);
                }
            }
        });
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

    /** Sideways: lets the first and last tape reach the middle of the screen. */
    private void fitPadding() {
        if (!horizontal || rv.getWidth() <= 0) return;
        int pad = Math.max(0, (rv.getWidth() - itemWidth) / 2);
        if (rv.getPaddingLeft() != pad || rv.getPaddingRight() != pad) {
            rv.setPadding(pad, rv.getPaddingTop(), pad, rv.getPaddingBottom());
        }
    }

    private void apply() {
        if (horizontal) {
            applySideways();
            return;
        }
        float half = rv.getHeight() / 2f;
        if (half <= 0f) return;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            int pos = rv.getChildAdapterPosition(child);
            float cy = child.getTop() + child.getHeight() / 2f;
            float d = Math.min(Math.abs(cy - half) / half, 1f);
            float s = 0.92f + 0.08f * (1f - d);
            child.setScaleX(s);
            child.setScaleY(s);
            child.setAlpha(0.6f + 0.4f * (1f - d));
            child.setRotation(pos < 0 ? 0f : (((pos * 37) % 5) - 2) * 0.7f);
            float spin = child.getTop() * 0.4f;
            View left = child.findViewById(R.id.reel_l);
            View right = child.findViewById(R.id.reel_r);
            if (left != null) left.setRotation(spin);
            if (right != null) right.setRotation(spin);
        }
    }

    private void applySideways() {
        float half = rv.getWidth() / 2f;
        if (half <= 0f) return;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            int pos = rv.getChildAdapterPosition(child);
            float cx = child.getLeft() + child.getWidth() / 2f;
            float signed = (cx - half) / half;
            float d = Math.min(Math.abs(signed), 1f);
            float s = 0.82f + 0.18f * (1f - d);
            child.setPivotX(child.getWidth() / 2f);
            child.setPivotY(child.getHeight() / 2f);
            child.setScaleX(s);
            child.setScaleY(s);
            child.setAlpha(0.45f + 0.55f * (1f - d));
            child.setTranslationZ((1f - d) * 10f);
            child.setRotation(pos < 0 ? 0f : (((pos * 37) % 5) - 2) * 0.7f);
            float spin = child.getLeft() * 0.4f;
            View left = child.findViewById(R.id.reel_l);
            View right = child.findViewById(R.id.reel_r);
            if (left != null) left.setRotation(spin);
            if (right != null) right.setRotation(spin);
        }
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (!horizontal) return;
        if (newState != RecyclerView.SCROLL_STATE_IDLE) {
            snapping = false;
            return;
        }
        if (snapping) {
            snapping = false;
            return;
        }
        float half = recyclerView.getWidth() / 2f;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View c = recyclerView.getChildAt(i);
            float d = c.getLeft() + c.getWidth() / 2f - half;
            if (Math.abs(d) < Math.abs(best)) best = d;
        }
        if (best != Float.MAX_VALUE && Math.abs(best) > 1f) {
            snapping = true;
            recyclerView.smoothScrollBy(Math.round(best), 0);
        }
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        apply();
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        if (horizontal) {
            android.view.ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null) lp.width = itemWidth;
        }
        apply();
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setAlpha(1f);
        view.setRotation(0f);
        view.setTranslationZ(0f);
    }
}
