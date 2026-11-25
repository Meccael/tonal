package com.meccael.tonal.carousel;

import android.content.Context;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

/* JADX INFO: loaded from: classes.dex */
public class HorizontalInfiniteCycleViewPager extends ViewPager implements ViewPageable {
    private InfiniteCycleManager mInfiniteCycleManager;

    public HorizontalInfiniteCycleViewPager(Context context) {
        super(context);
        init(context, null);
    }

    public HorizontalInfiniteCycleViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet);
    }

    private void init(Context context, AttributeSet attributeSet) {
        this.mInfiniteCycleManager = new InfiniteCycleManager(context, this, attributeSet);
    }

    @Override // androidx.viewpager.widget.ViewPager, android.view.ViewGroup
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        super.addView(view, 0, layoutParams);
    }

    @Override // android.view.ViewGroup
    protected boolean addViewInLayout(View view, int i, ViewGroup.LayoutParams layoutParams) {
        return super.addViewInLayout(view, 0, layoutParams);
    }

    @Override // androidx.viewpager.widget.ViewPager, com.meccael.tonal.carousel.ViewPageable
    public PagerAdapter getAdapter() {
        if (this.mInfiniteCycleManager != null && this.mInfiniteCycleManager.getInfiniteCyclePagerAdapter() != null) {
            return this.mInfiniteCycleManager.getInfiniteCyclePagerAdapter().getPagerAdapter();
        }
        return super.getAdapter();
    }

    public float getCenterPageScaleOffset() {
        if (this.mInfiniteCycleManager == null) {
            return 0.0f;
        }
        return this.mInfiniteCycleManager.getCenterPageScaleOffset();
    }

    public Interpolator getInterpolator() {
        if (this.mInfiniteCycleManager == null) {
            return null;
        }
        return this.mInfiniteCycleManager.getInterpolator();
    }

    public float getMaxPageScale() {
        if (this.mInfiniteCycleManager == null) {
            return 0.0f;
        }
        return this.mInfiniteCycleManager.getMaxPageScale();
    }

    public float getMinPageScale() {
        if (this.mInfiniteCycleManager == null) {
            return 0.0f;
        }
        return this.mInfiniteCycleManager.getMinPageScale();
    }

    public float getMinPageScaleOffset() {
        if (this.mInfiniteCycleManager == null) {
            return 0.0f;
        }
        return this.mInfiniteCycleManager.getMinPageScaleOffset();
    }

    public OnInfiniteCyclePageTransformListener getOnInfiniteCyclePageTransformListener() {
        if (this.mInfiniteCycleManager == null) {
            return null;
        }
        return this.mInfiniteCycleManager.getOnInfiniteCyclePageTransformListener();
    }

    public int getRealItem() {
        return this.mInfiniteCycleManager == null ? getCurrentItem() : this.mInfiniteCycleManager.getRealItem();
    }

    public int getScrollDuration() {
        if (this.mInfiniteCycleManager == null) {
            return 0;
        }
        return this.mInfiniteCycleManager.getScrollDuration();
    }

    public int getState() {
        if (this.mInfiniteCycleManager == null) {
            return 0;
        }
        return this.mInfiniteCycleManager.getState();
    }

    public void invalidateTransformer() {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.invalidateTransformer();
        }
    }

    public boolean isMediumScaled() {
        return this.mInfiniteCycleManager != null && this.mInfiniteCycleManager.isMediumScaled();
    }

    public boolean isVertical() {
        return false;
    }

    public void notifyDataSetChanged() {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.notifyDataSetChanged();
        }
    }

    @Override // androidx.viewpager.widget.ViewPager, android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.stopAutoScroll();
        }
        super.onDetachedFromWindow();
    }

    @Override // androidx.viewpager.widget.ViewPager, android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean zOnInterceptTouchEvent = true;
        try {
            if (this.mInfiniteCycleManager == null) {
                zOnInterceptTouchEvent = super.onInterceptTouchEvent(motionEvent);
            } else if (!this.mInfiniteCycleManager.onInterceptTouchEvent(motionEvent) || !super.onInterceptTouchEvent(motionEvent)) {
                zOnInterceptTouchEvent = false;
            }
        } catch (IllegalArgumentException e) {
        }
        return zOnInterceptTouchEvent;
    }

    @Override // androidx.viewpager.widget.ViewPager, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent = true;
        try {
            if (this.mInfiniteCycleManager == null) {
                zOnTouchEvent = super.onTouchEvent(motionEvent);
            } else if (!this.mInfiniteCycleManager.onTouchEvent(motionEvent) || !super.onTouchEvent(motionEvent)) {
                zOnTouchEvent = false;
            }
        } catch (IllegalArgumentException e) {
        }
        return zOnTouchEvent;
    }

    @Override // android.view.View
    public void onWindowFocusChanged(boolean z) {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.onWindowFocusChanged(z);
        }
        super.onWindowFocusChanged(z);
    }

    public void postInvalidateTransformer() {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.postInvalidateTransformer();
        }
    }

    @Override // androidx.viewpager.widget.ViewPager
    public void setAdapter(PagerAdapter pagerAdapter) {
        if (this.mInfiniteCycleManager == null) {
            super.setAdapter(pagerAdapter);
        } else {
            super.setAdapter(this.mInfiniteCycleManager.setAdapter(pagerAdapter));
            this.mInfiniteCycleManager.resetPager();
        }
    }

    public void setCenterPageScaleOffset(float f) {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.setCenterPageScaleOffset(f);
        }
    }

    @Override // android.view.ViewGroup
    protected void setChildrenDrawingCacheEnabled(boolean z) {
        super.setChildrenDrawingCacheEnabled(false);
    }

    @Override // android.view.ViewGroup
    protected void setChildrenDrawingOrderEnabled(boolean z) {
        super.setChildrenDrawingOrderEnabled(false);
    }

    @Override // android.view.ViewGroup, com.meccael.tonal.carousel.ViewPageable
    public void setClipChildren(boolean z) {
        super.setClipChildren(false);
    }

    @Override // androidx.viewpager.widget.ViewPager, com.meccael.tonal.carousel.ViewPageable
    public void setCurrentItem(int i) {
        setCurrentItem(i, true);
    }

    @Override // androidx.viewpager.widget.ViewPager
    public void setCurrentItem(int i, boolean z) {
        if (this.mInfiniteCycleManager != null) {
            super.setCurrentItem(this.mInfiniteCycleManager.setCurrentItem(i), true);
        }
    }

    @Override // android.view.View, com.meccael.tonal.carousel.ViewPageable
    public void setDrawingCacheEnabled(boolean z) {
        super.setDrawingCacheEnabled(false);
    }

    public void setInterpolator(Interpolator interpolator) {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.setInterpolator(interpolator);
        }
    }

    public void setMaxPageScale(float f) {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.setMaxPageScale(f);
        }
    }

    public void setMediumScaled(boolean z) {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.setMediumScaled(z);
        }
    }

    public void setMinPageScale(float f) {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.setMinPageScale(f);
        }
    }

    public void setMinPageScaleOffset(float f) {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.setMinPageScaleOffset(f);
        }
    }

    @Override // androidx.viewpager.widget.ViewPager, com.meccael.tonal.carousel.ViewPageable
    public void setOffscreenPageLimit(int i) {
        super.setOffscreenPageLimit(2);
    }

    public void setOnInfiniteCyclePageTransformListener(OnInfiniteCyclePageTransformListener onInfiniteCyclePageTransformListener) {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.setOnInfiniteCyclePageTransformListener(onInfiniteCyclePageTransformListener);
        }
    }

    @Override // android.view.View, com.meccael.tonal.carousel.ViewPageable
    public void setOverScrollMode(int i) {
        super.setOverScrollMode(2);
    }

    @Override // androidx.viewpager.widget.ViewPager, com.meccael.tonal.carousel.ViewPageable
    public void setPageMargin(int i) {
        super.setPageMargin(0);
    }

    @Override // androidx.viewpager.widget.ViewPager, com.meccael.tonal.carousel.ViewPageable
    public void setPageTransformer(boolean z, ViewPager.PageTransformer pageTransformer) {
        if (this.mInfiniteCycleManager != null) {
            pageTransformer = this.mInfiniteCycleManager.getInfinityCyclePageTransformer();
        }
        super.setPageTransformer(false, pageTransformer);
    }

    public void setScrollDuration(int i) {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.setScrollDuration(i);
        }
    }

    @Override // android.view.View, com.meccael.tonal.carousel.ViewPageable
    public void setWillNotCacheDrawing(boolean z) {
        super.setWillNotCacheDrawing(true);
    }

    public void startAutoScroll(boolean z) {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.startAutoScroll(z);
        }
    }

    public void stopAutoScroll() {
        if (this.mInfiniteCycleManager != null) {
            this.mInfiniteCycleManager.stopAutoScroll();
        }
    }
}
