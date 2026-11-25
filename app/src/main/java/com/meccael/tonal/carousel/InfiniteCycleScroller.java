package com.meccael.tonal.carousel;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

/* JADX INFO: loaded from: classes.dex */
class InfiniteCycleScroller extends Scroller {
    private int mDuration;

    public InfiniteCycleScroller(Context context) {
        super(context);
    }

    public InfiniteCycleScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    public InfiniteCycleScroller(Context context, Interpolator interpolator, boolean z) {
        super(context, interpolator, z);
    }

    public void setDuration(int i) {
        this.mDuration = i;
    }

    @Override // android.widget.Scroller
    public void startScroll(int i, int i2, int i3, int i4) {
        super.startScroll(i, i2, i3, i4, this.mDuration);
    }

    @Override // android.widget.Scroller
    public void startScroll(int i, int i2, int i3, int i4, int i5) {
        super.startScroll(i, i2, i3, i4, this.mDuration);
    }
}
