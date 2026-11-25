package com.meccael.tonal.views;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;

/* JADX INFO: loaded from: classes.dex */
public class FaddingRecyclerView extends RecyclerView {
    public FaddingRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public FaddingRecyclerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.view.View
    protected float getBottomFadingEdgeStrength() {
        return 0.0f;
    }
}
