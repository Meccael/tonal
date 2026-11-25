package com.meccael.tonal.carousel;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
interface ViewPageable {
    void addOnPageChangeListener(ViewPager.OnPageChangeListener onPageChangeListener);

    boolean beginFakeDrag();

    void endFakeDrag();

    void fakeDragBy(float f);

    PagerAdapter getAdapter();

    View getChildAt(int i);

    int getChildCount();

    int getCurrentItem();

    boolean hasWindowFocus();

    boolean isFakeDragging();

    boolean post(Runnable runnable);

    void setClipChildren(boolean z);

    void setCurrentItem(int i);

    void setDrawingCacheEnabled(boolean z);

    void setOffscreenPageLimit(int i);

    void setOverScrollMode(int i);

    void setPageMargin(int i);

    void setPageTransformer(boolean z, ViewPager.PageTransformer pageTransformer);

    void setWillNotCacheDrawing(boolean z);
}
