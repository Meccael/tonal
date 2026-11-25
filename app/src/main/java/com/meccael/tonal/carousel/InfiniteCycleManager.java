package com.meccael.tonal.carousel;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import androidx.viewpager.widget.PagerAdapter;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import java.lang.reflect.Field;
import com.meccael.tonal.R;

/* JADX INFO: loaded from: classes.dex */
class InfiniteCycleManager implements InfiniteCyclePagerAdapter.OnNotifyDataSetChangedListener {
    private static final int DEFAULT_CENTER_PAGE_SCALE_OFFSET = 0;
    protected static final boolean DEFAULT_DISABLE_FLAG = false;
    protected static final boolean DEFAULT_ENABLE_FLAG = true;
    private static final boolean DEFAULT_IS_MEDIUM_SCALED = true;
    private static final float DEFAULT_MAX_SCALE = 0.7f;
    private static final int DEFAULT_MIN_PAGE_SCALE_OFFSET = 36;
    private static final float DEFAULT_MIN_SCALE = 0.52f;
    protected static final int DEFAULT_OFFSCREEN_PAGE_LIMIT = 2;
    protected static final int DEFAULT_PAGE_MARGIN = 0;
    private static final int DEFAULT_SCROLL_DURATION = 400;
    private static final int MIN_CYCLE_COUNT = 0;
    private static final int MIN_POINTER_COUNT = 1;
    private View mCastViewPageable;
    private float mCenterPageScaleOffset;
    private float mCenterScaleBy;
    private Context mContext;
    private InfiniteCyclePagerAdapter mInfiniteCyclePagerAdapter;
    private Interpolator mInterpolator;
    private boolean mIsAdapterInitialPosition;
    private boolean mIsAutoScroll;
    private boolean mIsAutoScrollPositive;
    private boolean mIsDataSetChanged;
    private boolean mIsInitialItem;
    private boolean mIsLeftPageBringToFront;
    private boolean mIsRightPageBringToFront;
    private int mItemCount;
    private float mMaxPageScale;
    private float mMinPageScale;
    private float mMinPageScaleOffset;
    private OnInfiniteCyclePageTransformListener mOnInfiniteCyclePageTransformListener;
    private float mPageScrolledPosition;
    private float mPageScrolledPositionOffset;
    private int mScrollDuration;
    private int mStackCount;
    private int mState;
    private ViewPageable mViewPageable;
    private boolean mWasMinusOne;
    private boolean mWasPlusOne;
    private PageScrolledState mInnerPageScrolledState = PageScrolledState.IDLE;
    private PageScrolledState mOuterPageScrolledState = PageScrolledState.IDLE;
    private final Rect mHitRect = new Rect();
    private boolean mIsMediumScaled = false;
    private final Handler mAutoScrollHandler = new Handler();
    private final Runnable mAutoScrollRunnable = new Runnable() { // from class: com.meccael.tonal.carousel.InfiniteCycleManager.1
        @Override // java.lang.Runnable
        public void run() {
            if (InfiniteCycleManager.this.mIsAutoScroll) {
                InfiniteCycleManager.this.mViewPageable.setCurrentItem((InfiniteCycleManager.this.mIsAutoScrollPositive ? 1 : -1) + InfiniteCycleManager.this.getRealItem());
                InfiniteCycleManager.this.mAutoScrollHandler.postDelayed(this, InfiniteCycleManager.this.mScrollDuration);
            }
        }
    };
    protected final ViewPager.OnPageChangeListener mInfinityCyclePageChangeListener = new ViewPager.SimpleOnPageChangeListener() { // from class: com.meccael.tonal.carousel.InfiniteCycleManager.3
        @Override // androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener, androidx.viewpager.widget.ViewPager.OnPageChangeListener
        public void onPageScrollStateChanged(int i) {
            InfiniteCycleManager.this.mState = i;
        }

        @Override // androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener, androidx.viewpager.widget.ViewPager.OnPageChangeListener
        public void onPageScrolled(int i, float f, int i2) {
            InfiniteCycleManager.this.mStackCount = 0;
            if (InfiniteCycleManager.this.mState != 2 || InfiniteCycleManager.this.mIsInitialItem) {
                if (InfiniteCycleManager.this.mOuterPageScrolledState == PageScrolledState.IDLE && f > 0.0f) {
                    InfiniteCycleManager.this.mPageScrolledPosition = InfiniteCycleManager.this.mViewPageable.getCurrentItem();
                    InfiniteCycleManager.this.mOuterPageScrolledState = ((float) i) == InfiniteCycleManager.this.mPageScrolledPosition ? PageScrolledState.GOING_LEFT : PageScrolledState.GOING_RIGHT;
                }
                boolean z = ((float) i) == InfiniteCycleManager.this.mPageScrolledPosition;
                if (InfiniteCycleManager.this.mOuterPageScrolledState == PageScrolledState.GOING_LEFT && !z) {
                    InfiniteCycleManager.this.mOuterPageScrolledState = PageScrolledState.GOING_RIGHT;
                } else if (InfiniteCycleManager.this.mOuterPageScrolledState == PageScrolledState.GOING_RIGHT && z) {
                    InfiniteCycleManager.this.mOuterPageScrolledState = PageScrolledState.GOING_LEFT;
                }
            }
            if (InfiniteCycleManager.this.mPageScrolledPositionOffset <= f) {
                InfiniteCycleManager.this.mInnerPageScrolledState = PageScrolledState.GOING_LEFT;
            } else {
                InfiniteCycleManager.this.mInnerPageScrolledState = PageScrolledState.GOING_RIGHT;
            }
            InfiniteCycleManager.this.mPageScrolledPositionOffset = f;
            if (InfiniteCycleManager.this.isSmallPositionOffset(f)) {
                f = 0.0f;
            }
            if (f == 0.0f) {
                InfiniteCycleManager.this.disableHardwareLayers();
                InfiniteCycleManager.this.mInnerPageScrolledState = PageScrolledState.IDLE;
                InfiniteCycleManager.this.mOuterPageScrolledState = PageScrolledState.IDLE;
                InfiniteCycleManager.this.mWasMinusOne = false;
                InfiniteCycleManager.this.mWasPlusOne = false;
                InfiniteCycleManager.this.mIsLeftPageBringToFront = false;
                InfiniteCycleManager.this.mIsRightPageBringToFront = false;
                InfiniteCycleManager.this.mIsInitialItem = false;
            }
        }
    };

    protected class InfiniteCyclePageTransformer implements ViewPager.PageTransformer {
        protected InfiniteCyclePageTransformer() {
        }

        @Override // androidx.viewpager.widget.ViewPager.PageTransformer
        public void transformPage(View view, float f) {
            float f2;
            float f3;
            if (InfiniteCycleManager.this.mOnInfiniteCyclePageTransformListener != null) {
                InfiniteCycleManager.this.mOnInfiniteCyclePageTransformListener.onPreTransform(view, f);
            }
            InfiniteCycleManager.this.enableHardwareLayer(view);
            if (InfiniteCycleManager.this.mItemCount == 0) {
                if (f > 2.0f || f < -2.0f || ((InfiniteCycleManager.this.mStackCount != 0 && f > 1.0f) || (InfiniteCycleManager.this.mStackCount != 0 && f < -1.0f))) {
                    view.setVisibility(8);
                    return;
                }
                view.setVisibility(0);
            }
            float measuredWidth = view.getMeasuredWidth();
            float f4 = measuredWidth * InfiniteCycleManager.this.mMinPageScale;
            float f5 = measuredWidth * InfiniteCycleManager.this.mCenterScaleBy;
            float f6 = (measuredWidth - f4) * 0.5f;
            float f7 = (measuredWidth - (InfiniteCycleManager.this.mMaxPageScale * measuredWidth)) * 0.5f;
            float f8 = (measuredWidth - ((InfiniteCycleManager.this.mMinPageScale + InfiniteCycleManager.this.mCenterScaleBy) * measuredWidth)) * 0.5f;
            if (InfiniteCycleManager.this.mItemCount < 1 && InfiniteCycleManager.this.mStackCount == 0 && f > -2.0f && f < -1.0f) {
                f2 = InfiniteCycleManager.this.mMinPageScale;
                f3 = (measuredWidth - f6) + InfiniteCycleManager.this.mMinPageScaleOffset + ((((2.0f * measuredWidth) - f4) - (InfiniteCycleManager.this.mMinPageScaleOffset * 2.0f)) * ((-1.0f) - f));
                InfiniteCycleManager.access$1008(InfiniteCycleManager.this);
            } else if (InfiniteCycleManager.this.mItemCount > 0 && f >= -2.0f && f < -1.0f) {
                f2 = InfiniteCycleManager.this.mMinPageScale;
                f3 = (2.0f * measuredWidth) - (((measuredWidth + f6) - InfiniteCycleManager.this.mMinPageScaleOffset) * (1.0f + (1.0f + f)));
            } else if (f >= -1.0f && f <= -0.5f) {
                float f9 = 1.0f + ((0.5f + f) * 2.0f);
                float f10 = 1.0f - f9;
                if (InfiniteCycleManager.this.mIsMediumScaled) {
                    float f11 = ((measuredWidth - f5) - f7) + InfiniteCycleManager.this.mMinPageScaleOffset;
                    f2 = (InfiniteCycleManager.this.mMinPageScale + InfiniteCycleManager.this.mCenterScaleBy) - (InfiniteCycleManager.this.mCenterScaleBy * f10);
                    f3 = f11 - (((f11 - f8) + InfiniteCycleManager.this.mCenterPageScaleOffset) * f9);
                } else {
                    float f12 = (measuredWidth - f6) + InfiniteCycleManager.this.mMinPageScaleOffset;
                    f2 = InfiniteCycleManager.this.mMaxPageScale - ((InfiniteCycleManager.this.mMaxPageScale - InfiniteCycleManager.this.mMinPageScale) * f10);
                    f3 = f12 - (((f12 - f7) + InfiniteCycleManager.this.mCenterPageScaleOffset) * f9);
                }
            } else if (f >= -0.5f && f <= 0.0f) {
                float f13 = (-f) * 2.0f;
                f2 = InfiniteCycleManager.this.mMaxPageScale - (InfiniteCycleManager.this.mIsMediumScaled ? InfiniteCycleManager.this.mCenterScaleBy * f13 : 0.0f);
                if (!InfiniteCycleManager.this.mIsMediumScaled) {
                    f8 = f7;
                }
                f3 = (f8 - InfiniteCycleManager.this.mCenterPageScaleOffset) * f13;
            } else if (f >= 0.0f && f <= 0.5f) {
                float f14 = f * 2.0f;
                f2 = !InfiniteCycleManager.this.mIsMediumScaled ? InfiniteCycleManager.this.mMaxPageScale : InfiniteCycleManager.this.mMinPageScale + InfiniteCycleManager.this.mCenterScaleBy + (InfiniteCycleManager.this.mCenterScaleBy * (1.0f - f14));
                if (!InfiniteCycleManager.this.mIsMediumScaled) {
                    f8 = f7;
                }
                f3 = ((-f8) + InfiniteCycleManager.this.mCenterPageScaleOffset) * f14;
            } else if (f >= 0.5f && f <= 1.0f) {
                float f15 = (f - 0.5f) * 2.0f;
                float f16 = 1.0f - f15;
                if (InfiniteCycleManager.this.mIsMediumScaled) {
                    f2 = InfiniteCycleManager.this.mMinPageScale + (InfiniteCycleManager.this.mCenterScaleBy * f16);
                    f3 = (-f8) + InfiniteCycleManager.this.mCenterPageScaleOffset + (((((((-measuredWidth) + f5) + f7) + f8) - InfiniteCycleManager.this.mMinPageScaleOffset) - InfiniteCycleManager.this.mCenterPageScaleOffset) * f15);
                } else {
                    f2 = InfiniteCycleManager.this.mMinPageScale + ((InfiniteCycleManager.this.mMaxPageScale - InfiniteCycleManager.this.mMinPageScale) * f16);
                    f3 = (-f7) + InfiniteCycleManager.this.mCenterPageScaleOffset + ((((((-measuredWidth) + f7) + f6) - InfiniteCycleManager.this.mMinPageScaleOffset) - InfiniteCycleManager.this.mCenterPageScaleOffset) * f15);
                }
            } else if (InfiniteCycleManager.this.mItemCount > 0 && f > 1.0f && f <= 2.0f) {
                f2 = InfiniteCycleManager.this.mMinPageScale;
                f3 = (-((measuredWidth - f6) + InfiniteCycleManager.this.mMinPageScaleOffset)) + (((measuredWidth + f6) - InfiniteCycleManager.this.mMinPageScaleOffset) * (1.0f - (1.0f + (f - 1.0f))));
            } else if (InfiniteCycleManager.this.mItemCount >= 1 || InfiniteCycleManager.this.mStackCount != 0 || f <= 1.0f || f >= 2.0f) {
                f2 = InfiniteCycleManager.this.mMinPageScale;
                f3 = 0.0f;
            } else {
                f2 = InfiniteCycleManager.this.mMinPageScale;
                f3 = (-((measuredWidth - f6) + InfiniteCycleManager.this.mMinPageScaleOffset)) + ((((2.0f * measuredWidth) - f4) - (InfiniteCycleManager.this.mMinPageScaleOffset * 2.0f)) * (1.0f - f));
                InfiniteCycleManager.access$1008(InfiniteCycleManager.this);
            }
            ViewCompat.setScaleX(view, f2);
            ViewCompat.setScaleY(view, f2);
            ViewCompat.setTranslationX(view, f3);
            boolean z = false;
            if (InfiniteCycleManager.this.mItemCount == -1) {
                InfiniteCycleManager.this.mIsLeftPageBringToFront = true;
            }
            switch (InfiniteCycleManager.this.mOuterPageScrolledState) {
                case GOING_LEFT:
                    InfiniteCycleManager.this.mIsLeftPageBringToFront = false;
                    if (InfiniteCycleManager.this.mInnerPageScrolledState != PageScrolledState.GOING_LEFT) {
                        if (InfiniteCycleManager.this.mPageScrolledPositionOffset < 0.5f && f > -0.5f && f <= 0.0f) {
                            z = true;
                        }
                    } else if (f > -0.5f && f <= 0.0f) {
                        if (!InfiniteCycleManager.this.mIsRightPageBringToFront) {
                            InfiniteCycleManager.this.mIsRightPageBringToFront = true;
                            z = true;
                        }
                    } else if (f >= 0.0f && f < 0.5f) {
                        z = true;
                    } else if (f > 0.5f && f < 1.0f && !InfiniteCycleManager.this.mIsRightPageBringToFront && InfiniteCycleManager.this.mViewPageable.getChildCount() > 0) {
                        z = true;
                    }
                    break;
                case GOING_RIGHT:
                    InfiniteCycleManager.this.mIsRightPageBringToFront = false;
                    if (InfiniteCycleManager.this.mInnerPageScrolledState != PageScrolledState.GOING_RIGHT) {
                        if (InfiniteCycleManager.this.mPageScrolledPositionOffset > 0.5f && f >= 0.0f && f < 0.5f) {
                            z = true;
                        }
                    } else if (f >= 0.0f && f < 0.5f) {
                        if (!InfiniteCycleManager.this.mIsLeftPageBringToFront) {
                            InfiniteCycleManager.this.mIsLeftPageBringToFront = true;
                            z = true;
                        }
                    } else if (f > -0.5f && f <= 0.0f) {
                        z = true;
                    } else if (f > -1.0f && f < -0.5f && !InfiniteCycleManager.this.mIsLeftPageBringToFront && InfiniteCycleManager.this.mViewPageable.getChildCount() > 0) {
                        z = true;
                    }
                    break;
                default:
                    if (InfiniteCycleManager.this.mIsDataSetChanged) {
                        InfiniteCycleManager.this.mIsLeftPageBringToFront = false;
                        InfiniteCycleManager.this.mIsRightPageBringToFront = false;
                    } else {
                        if (!InfiniteCycleManager.this.mWasPlusOne && f == 1.0f) {
                            InfiniteCycleManager.this.mWasPlusOne = true;
                        } else if (InfiniteCycleManager.this.mWasPlusOne && f == -1.0f) {
                            InfiniteCycleManager.this.mIsLeftPageBringToFront = true;
                        } else if ((!InfiniteCycleManager.this.mWasPlusOne && f == -1.0f) || (InfiniteCycleManager.this.mWasPlusOne && InfiniteCycleManager.this.mIsLeftPageBringToFront && f == -2.0f)) {
                            InfiniteCycleManager.this.mIsLeftPageBringToFront = false;
                        }
                        if (!InfiniteCycleManager.this.mWasMinusOne && f == -1.0f) {
                            InfiniteCycleManager.this.mWasMinusOne = true;
                        } else if (InfiniteCycleManager.this.mWasMinusOne && f == 1.0f) {
                            InfiniteCycleManager.this.mIsRightPageBringToFront = true;
                        } else if ((!InfiniteCycleManager.this.mWasMinusOne && f == 1.0f) || (InfiniteCycleManager.this.mWasMinusOne && InfiniteCycleManager.this.mIsRightPageBringToFront && f == 2.0f)) {
                            InfiniteCycleManager.this.mIsRightPageBringToFront = false;
                        }
                    }
                    if (f == 0.0f) {
                        z = true;
                    }
                    break;
            }
            if (z) {
                view.bringToFront();
                InfiniteCycleManager.this.mCastViewPageable.invalidate();
            }
            if (InfiniteCycleManager.this.mOnInfiniteCyclePageTransformListener != null) {
                InfiniteCycleManager.this.mOnInfiniteCyclePageTransformListener.onPostTransform(view, f);
            }
        }
    }

    private enum PageScrolledState {
        IDLE,
        GOING_LEFT,
        GOING_RIGHT
    }

    private final class SpringInterpolator implements Interpolator {
        private static final float FACTOR = 0.5f;

        private SpringInterpolator() {
        }

        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float f) {
            return (float) ((Math.pow(2.0d, (-10.0f) * f) * Math.sin((((double) (f - 0.125f)) * 6.283185307179586d) / 0.5d)) + 1.0d);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public InfiniteCycleManager(Context context, ViewPageable viewPageable, AttributeSet attributeSet) {
        this.mContext = context;
        this.mViewPageable = viewPageable;
        this.mCastViewPageable = (View) viewPageable;
        this.mViewPageable.setPageTransformer(false, getInfinityCyclePageTransformer());
        this.mViewPageable.addOnPageChangeListener(this.mInfinityCyclePageChangeListener);
        this.mViewPageable.setClipChildren(false);
        this.mViewPageable.setDrawingCacheEnabled(false);
        this.mViewPageable.setWillNotCacheDrawing(true);
        this.mViewPageable.setPageMargin(0);
        this.mViewPageable.setOffscreenPageLimit(2);
        this.mViewPageable.setOverScrollMode(2);
        resetScroller();
        processAttributeSet(attributeSet);
    }

    static /* synthetic */ int access$1008(InfiniteCycleManager infiniteCycleManager) {
        int i = infiniteCycleManager.mStackCount;
        infiniteCycleManager.mStackCount = i + 1;
        return i;
    }

    private void checkHitRect(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mHitRect.set(this.mCastViewPageable.getLeft(), this.mCastViewPageable.getTop(), this.mCastViewPageable.getRight(), this.mCastViewPageable.getBottom());
        } else {
            if (motionEvent.getAction() != 2 || this.mHitRect.contains(this.mCastViewPageable.getLeft() + ((int) motionEvent.getX()), this.mCastViewPageable.getTop() + ((int) motionEvent.getY()))) {
                return;
            }
            motionEvent.setAction(1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void disableHardwareLayers() {
        if (Build.VERSION.SDK_INT > 19) {
            return;
        }
        for (int i = 0; i < this.mViewPageable.getChildCount(); i++) {
            View childAt = this.mViewPageable.getChildAt(i);
            if (childAt.getLayerType() != 0) {
                childAt.setLayerType(0, null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableHardwareLayer(View view) {
        int i = Build.VERSION.SDK_INT > 19 ? 0 : 2;
        if (view.getLayerType() != i) {
            view.setLayerType(i, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSmallPositionOffset(float f) {
        return Math.abs(f) < 1.0E-4f;
    }

    private void resetScaleBy() {
        this.mCenterScaleBy = (this.mMaxPageScale - this.mMinPageScale) * 0.5f;
    }

    private void resetScroller() {
        if (this.mViewPageable == null) {
            return;
        }
        try {
            Field declaredField = ViewPager.class.getDeclaredField("mScroller");
            declaredField.setAccessible(true);
            InfiniteCycleScroller infiniteCycleScroller = new InfiniteCycleScroller(this.mContext, this.mInterpolator);
            infiniteCycleScroller.setDuration(this.mScrollDuration);
            declaredField.set(this.mViewPageable, infiniteCycleScroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public float getCenterPageScaleOffset() {
        return this.mCenterPageScaleOffset;
    }

    public InfiniteCyclePagerAdapter getInfiniteCyclePagerAdapter() {
        return this.mInfiniteCyclePagerAdapter;
    }

    public InfiniteCyclePageTransformer getInfinityCyclePageTransformer() {
        return new InfiniteCyclePageTransformer();
    }

    public Interpolator getInterpolator() {
        return this.mInterpolator;
    }

    public float getMaxPageScale() {
        return this.mMaxPageScale;
    }

    public float getMinPageScale() {
        return this.mMinPageScale;
    }

    public float getMinPageScaleOffset() {
        return this.mMinPageScaleOffset;
    }

    public OnInfiniteCyclePageTransformListener getOnInfiniteCyclePageTransformListener() {
        return this.mOnInfiniteCyclePageTransformListener;
    }

    public int getRealItem() {
        return (this.mViewPageable.getAdapter() == null || this.mViewPageable.getAdapter().getCount() < 0) ? this.mViewPageable.getCurrentItem() : this.mInfiniteCyclePagerAdapter.getVirtualPosition(this.mViewPageable.getCurrentItem());
    }

    public int getScrollDuration() {
        return this.mScrollDuration;
    }

    public int getState() {
        return this.mState;
    }

    public void invalidateTransformer() {
        if (this.mViewPageable.getAdapter() == null || this.mViewPageable.getAdapter().getCount() == 0 || this.mViewPageable.getChildCount() == 0 || !this.mViewPageable.beginFakeDrag()) {
            return;
        }
        this.mViewPageable.fakeDragBy(0.0f);
        this.mViewPageable.endFakeDrag();
    }

    public boolean isMediumScaled() {
        return this.mIsMediumScaled;
    }

    public void notifyDataSetChanged() {
        if (this.mInfiniteCyclePagerAdapter == null) {
            this.mViewPageable.getAdapter().notifyDataSetChanged();
            this.mIsDataSetChanged = true;
        } else {
            this.mInfiniteCyclePagerAdapter.notifyDataSetChanged();
        }
        postInvalidateTransformer();
    }

    @Override // com.meccael.tonal.carousel.InfiniteCyclePagerAdapter.OnNotifyDataSetChangedListener
    public void onChanged() {
        this.mIsDataSetChanged = true;
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return onTouchEvent(motionEvent);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mViewPageable.getAdapter() == null || this.mViewPageable.getAdapter().getCount() == 0) {
            return false;
        }
        if (this.mIsAutoScroll || this.mIsInitialItem || this.mViewPageable.isFakeDragging()) {
            return false;
        }
        if (motionEvent.getPointerCount() > 1 || !this.mViewPageable.hasWindowFocus()) {
            motionEvent.setAction(1);
        }
        checkHitRect(motionEvent);
        return true;
    }

    public void onWindowFocusChanged(boolean z) {
        if (z) {
            invalidateTransformer();
        }
    }

    public void postInvalidateTransformer() {
        this.mViewPageable.post(new Runnable() { // from class: com.meccael.tonal.carousel.InfiniteCycleManager.2
            @Override // java.lang.Runnable
            public void run() {
                InfiniteCycleManager.this.invalidateTransformer();
                InfiniteCycleManager.this.mIsDataSetChanged = false;
            }
        });
    }

    public void processAttributeSet(AttributeSet attributeSet) {
        if (attributeSet == null) {
            return;
        }
        TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(attributeSet, R.styleable.HorizontalInfiniteCycleViewPager);
        try {
            setMinPageScaleOffset(typedArrayObtainStyledAttributes.getDimension(R.styleable.HorizontalInfiniteCycleViewPager_icvp_min_page_scale_offset, 36.0f));
            setCenterPageScaleOffset(typedArrayObtainStyledAttributes.getDimension(R.styleable.HorizontalInfiniteCycleViewPager_icvp_center_page_scale_offset, 0.0f));
            setMinPageScale(typedArrayObtainStyledAttributes.getFloat(R.styleable.HorizontalInfiniteCycleViewPager_icvp_min_page_scale, DEFAULT_MIN_SCALE));
            setMaxPageScale(typedArrayObtainStyledAttributes.getFloat(R.styleable.HorizontalInfiniteCycleViewPager_icvp_max_page_scale, DEFAULT_MAX_SCALE));
            setMediumScaled(typedArrayObtainStyledAttributes.getBoolean(R.styleable.HorizontalInfiniteCycleViewPager_icvp_medium_scaled, true));
            setScrollDuration(typedArrayObtainStyledAttributes.getInteger(R.styleable.HorizontalInfiniteCycleViewPager_icvp_scroll_duration, DEFAULT_SCROLL_DURATION));
            Interpolator interpolator = null;
            try {
                try {
                    int resourceId = typedArrayObtainStyledAttributes.getResourceId(R.styleable.HorizontalInfiniteCycleViewPager_icvp_interpolator, 0);
                    setInterpolator(resourceId == 0 ? null : AnimationUtils.loadInterpolator(this.mContext, resourceId));
                } catch (Throwable th) {
                    setInterpolator(interpolator);
                    throw th;
                }
            } catch (Resources.NotFoundException e) {
                interpolator = null;
                e.printStackTrace();
                setInterpolator(null);
            }
            typedArrayObtainStyledAttributes.recycle();
        } catch (Throwable th2) {
            typedArrayObtainStyledAttributes.recycle();
            throw th2;
        }
    }

    public void resetPager() {
        this.mIsAdapterInitialPosition = true;
        this.mViewPageable.setCurrentItem(0);
        postInvalidateTransformer();
    }

    public PagerAdapter setAdapter(PagerAdapter pagerAdapter) {
        if (pagerAdapter != null && pagerAdapter.getCount() >= 0) {
            this.mItemCount = pagerAdapter.getCount();
            this.mInfiniteCyclePagerAdapter = new InfiniteCyclePagerAdapter(pagerAdapter);
            this.mInfiniteCyclePagerAdapter.setOnNotifyDataSetChangedListener(this);
            return this.mInfiniteCyclePagerAdapter;
        }
        if (this.mInfiniteCyclePagerAdapter == null) {
            return pagerAdapter;
        }
        this.mInfiniteCyclePagerAdapter.setOnNotifyDataSetChangedListener(null);
        this.mInfiniteCyclePagerAdapter = null;
        return pagerAdapter;
    }

    public void setCenterPageScaleOffset(float f) {
        this.mCenterPageScaleOffset = f;
    }

    public int setCurrentItem(int i) {
        this.mIsInitialItem = true;
        if (this.mViewPageable.getAdapter() == null || this.mViewPageable.getAdapter().getCount() < 0) {
            return i;
        }
        int count = this.mViewPageable.getAdapter().getCount();
        if (!this.mIsAdapterInitialPosition) {
            return (this.mViewPageable.getCurrentItem() + Math.min(count, i)) - getRealItem();
        }
        this.mIsAdapterInitialPosition = false;
        return ((this.mInfiniteCyclePagerAdapter.getCount() / 2) / count) * count;
    }

    public void setInterpolator(Interpolator interpolator) {
        if (interpolator == null) {
            interpolator = new SpringInterpolator();
        }
        this.mInterpolator = interpolator;
        resetScroller();
    }

    public void setMaxPageScale(float f) {
        this.mMaxPageScale = f;
        resetScaleBy();
    }

    public void setMediumScaled(boolean z) {
        this.mIsMediumScaled = z;
    }

    public void setMinPageScale(float f) {
        this.mMinPageScale = f;
        resetScaleBy();
    }

    public void setMinPageScaleOffset(float f) {
        this.mMinPageScaleOffset = f;
    }

    public void setOnInfiniteCyclePageTransformListener(OnInfiniteCyclePageTransformListener onInfiniteCyclePageTransformListener) {
        this.mOnInfiniteCyclePageTransformListener = onInfiniteCyclePageTransformListener;
    }

    public void setScrollDuration(int i) {
        this.mScrollDuration = i;
        resetScroller();
    }

    public void startAutoScroll(boolean z) {
        if (this.mIsAutoScroll && z == this.mIsAutoScrollPositive) {
            return;
        }
        this.mIsAutoScroll = true;
        this.mIsAutoScrollPositive = z;
        this.mAutoScrollHandler.removeCallbacks(this.mAutoScrollRunnable);
        this.mAutoScrollHandler.post(this.mAutoScrollRunnable);
    }

    public void stopAutoScroll() {
        if (this.mIsAutoScroll) {
            this.mIsAutoScroll = false;
            this.mAutoScrollHandler.removeCallbacks(this.mAutoScrollRunnable);
        }
    }
}
