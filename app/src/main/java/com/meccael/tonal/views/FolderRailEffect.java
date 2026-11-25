package com.meccael.tonal.views;

import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.meccael.tonal.R;

/**
 * Folders as a vertical rail (item_list_card_folder). The folder nearest the upper part of the
 * screen is "in focus": glowing dot, cream title and a soft cream wash across the row.
 */
public class FolderRailEffect extends RecyclerView.OnScrollListener
        implements RecyclerView.OnChildAttachStateChangeListener {

    private final RecyclerView rv;

    private FolderRailEffect(RecyclerView rv) {
        this.rv = rv;
    }

    public static void install(RecyclerView rv, int basePaddingBottom) {
        final FolderRailEffect effect = new FolderRailEffect(rv);
        float density = rv.getResources().getDisplayMetrics().density;
        rv.setClipToPadding(false);
        rv.setPadding(0, (int) (4 * density), 0, Math.max(basePaddingBottom, (int) (96 * density)));
        rv.setLayoutManager(new LinearLayoutManager(rv.getContext()));
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
        float target = rv.getHeight() * 0.28f;
        View best = null;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View c = rv.getChildAt(i);
            float d = Math.abs(c.getTop() + c.getHeight() / 2f - target);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        for (int i = 0; i < rv.getChildCount(); i++) {
            View c = rv.getChildAt(i);
            style(c, c == best);
        }
    }

    private void style(View item, boolean focused) {
        View dot = item.findViewById(R.id.folder_dot);
        View row = item.findViewById(R.id.folder_row);
        TextView title = (TextView) item.findViewById(R.id.title);
        if (dot == null || row == null || title == null) return;
        Object tag = item.getTag(R.id.folder_dot);
        if (tag != null && ((Boolean) tag) == focused) return;
        item.setTag(R.id.folder_dot, focused);
        dot.setBackgroundResource(focused ? R.drawable.pl_dot_now : R.drawable.folder_dot_off);
        row.setBackgroundResource(focused ? R.drawable.pl_row_now : 0);
        title.setTextColor(focused ? 0xFFEADFC8 : 0xFFF4ECDC);
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
        view.setTag(R.id.folder_dot, null);
    }
}
