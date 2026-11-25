package com.meccael.tonal.scrollbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.core.view.ViewCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import java.util.ArrayList;
import com.meccael.tonal.R;

/* JADX INFO: Access modifiers changed from: package-private */
/* JADX INFO: loaded from: classes.dex */
public abstract class MaterialScrollBar<T> extends RelativeLayout {
    private TypedArray a;
    private View background;
    int barWidth;
    int defaultHandleOffColor;
    Handle handle;
    int handleColour;
    int handleOffColour;
    protected boolean hidden;
    boolean hiddenByUser;
    Indicator indicator;
    boolean lightOnTouch;
    boolean programmatic;
    RecyclerView recyclerView;
    ScrollingUtilities scrollUtils;
    private int seekId;
    boolean sizeUnchecked;
    SwipeRefreshLayout swipeRefreshLayout;

    class scrollListener extends RecyclerView.OnScrollListener {
        scrollListener() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
            super.onScrolled(recyclerView, i, i2);
            MaterialScrollBar.this.scrollUtils.scrollHandleAndIndicator();
            if (i2 != 0) {
                MaterialScrollBar.this.onScroll();
            }
            if (MaterialScrollBar.this.swipeRefreshLayout != null) {
                if (((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition() == 0) {
                    MaterialScrollBar.this.swipeRefreshLayout.setEnabled(true);
                } else {
                    MaterialScrollBar.this.swipeRefreshLayout.setEnabled(false);
                }
            }
        }
    }

    MaterialScrollBar(Context context, RecyclerView recyclerView, boolean z) {
        super(context);
        this.defaultHandleOffColor = Color.parseColor("#80000000");
        this.handleOffColour = this.defaultHandleOffColor;
        this.barWidth = 8;
        this.hidden = true;
        this.hiddenByUser = false;
        this.seekId = 0;
        this.sizeUnchecked = true;
        this.scrollUtils = new ScrollingUtilities(this);
        this.programmatic = true;
        if (!(recyclerView.getParent() instanceof RelativeLayout)) {
            throw new CustomExceptions.UnsupportedParentException();
        }
        setId(R.id.reservedNamedId);
        addView(setUpBackground(context));
        addView(setUpHandle(context, Boolean.valueOf(z)));
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(Utils.getDP(20, this), -1);
        layoutParams.addRule(7, recyclerView.getId());
        layoutParams.addRule(6, recyclerView.getId());
        layoutParams.addRule(8, recyclerView.getId());
        ((ViewGroup) recyclerView.getParent()).addView(this, layoutParams);
        this.recyclerView = recyclerView;
        generalSetup();
    }

    MaterialScrollBar(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    MaterialScrollBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.defaultHandleOffColor = Color.parseColor("#80000000");
        this.handleOffColour = this.defaultHandleOffColor;
        this.barWidth = 8;
        this.hidden = true;
        this.hiddenByUser = false;
        this.seekId = 0;
        this.sizeUnchecked = true;
        this.scrollUtils = new ScrollingUtilities(this);
        this.programmatic = false;
        addView(setUpBackground(context));
        setUpProps(context, attributeSet);
        boolean lightOnTouch = true;
        if (this.a != null) {
            lightOnTouch = this.a.getBoolean(R.styleable.MaterialScrollBar_msb_lightOnTouch, true);
            if (!isInEditMode()) {
                this.seekId = this.a.getResourceId(R.styleable.MaterialScrollBar_msb_recyclerView, 0);
            }
        }
        addView(setUpHandle(context, Boolean.valueOf(lightOnTouch)));
        implementPreferences();
        if (this.a != null) {
            this.a.recycle();
            this.a = null;
        }
    }

    private void generalSetup() {
        if (this.recyclerView == null && this.seekId != 0 && getRootView() != null) {
            this.recyclerView = (RecyclerView) getRootView().findViewById(this.seekId);
        }
        if (this.recyclerView != null) {
            this.recyclerView.setVerticalScrollBarEnabled(false);
            this.recyclerView.addOnScrollListener(new scrollListener());
        }
        setTouchIntercept();
        identifySwipeRefreshParents();
        TranslateAnimation translateAnimation = new TranslateAnimation(2, 0.0f, 1, getHideRatio(), 2, 0.0f, 2, 0.0f);
        translateAnimation.setDuration(0L);
        translateAnimation.setFillAfter(true);
        this.hidden = true;
        startAnimation(translateAnimation);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public T addIndicator(final Indicator indicator) {
        if (this.recyclerView == null && this.seekId != 0 && getRootView() != null) {
            this.recyclerView = (RecyclerView) getRootView().findViewById(this.seekId);
            if (this.recyclerView != null) {
                generalSetup();
            }
        }
        if (this.recyclerView != null && ViewCompat.isAttachedToWindow(this)) {
            this.indicator = indicator;
            if (this.recyclerView.getAdapter() != null) {
                indicator.testAdapter(this.recyclerView.getAdapter());
            }
            indicator.linkToScrollBar(this);
        } else {
            addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.meccael.tonal.scrollbar.MaterialScrollBar.3
                @Override // android.view.View.OnLayoutChangeListener
                public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                    if (MaterialScrollBar.this.recyclerView == null && MaterialScrollBar.this.seekId != 0 && MaterialScrollBar.this.getRootView() != null) {
                        MaterialScrollBar.this.recyclerView = (RecyclerView) MaterialScrollBar.this.getRootView().findViewById(MaterialScrollBar.this.seekId);
                        if (MaterialScrollBar.this.recyclerView != null) {
                            MaterialScrollBar.this.generalSetup();
                        }
                    }
                    MaterialScrollBar.this.indicator = indicator;
                    if (MaterialScrollBar.this.recyclerView != null && MaterialScrollBar.this.recyclerView.getAdapter() != null) {
                        indicator.testAdapter(MaterialScrollBar.this.recyclerView.getAdapter());
                    }
                    indicator.linkToScrollBar(MaterialScrollBar.this);
                    MaterialScrollBar.this.removeOnLayoutChangeListener(this);
                }
            });
        }
        return (T) this;
    }

    void fadeIn() {
        if (this.hidden && getHide() && !this.hiddenByUser) {
            this.hidden = false;
            TranslateAnimation translateAnimation = new TranslateAnimation(1, getHideRatio(), 1, 0.0f, 2, 0.0f, 2, 0.0f);
            translateAnimation.setDuration(250L);
            translateAnimation.setFillAfter(true);
            startAnimation(translateAnimation);
            this.handle.collapseHandle();
        }
    }

    void fadeOut() {
        if (this.hidden) {
            return;
        }
        TranslateAnimation translateAnimation = new TranslateAnimation(2, 0.0f, 1, getHideRatio(), 2, 0.0f, 2, 0.0f);
        translateAnimation.setDuration(250L);
        translateAnimation.setFillAfter(true);
        this.hidden = true;
        startAnimation(translateAnimation);
        postDelayed(new Runnable() { // from class: com.meccael.tonal.scrollbar.MaterialScrollBar.4
            @Override // java.lang.Runnable
            public void run() {
                MaterialScrollBar.this.handle.expandHandle();
            }
        }, translateAnimation.getDuration() / 3);
    }

    abstract float getHandleOffset();

    abstract boolean getHide();

    abstract float getHideRatio();

    abstract int getMode();

    void identifySwipeRefreshParents() {
        boolean z = true;
        ViewParent parent = getParent();
        while (z) {
            if (parent instanceof SwipeRefreshLayout) {
                this.swipeRefreshLayout = (SwipeRefreshLayout) parent;
                z = false;
            } else if (parent.getParent() == null) {
                z = false;
            } else {
                parent = parent.getParent();
            }
        }
    }

    abstract void implementFlavourPreferences(TypedArray typedArray);

    void implementPreferences() {
        if (this.a == null) {
            return;
        }
        if (this.a.hasValue(R.styleable.MaterialScrollBar_msb_barColour)) {
            setBarColour(this.a.getColor(R.styleable.MaterialScrollBar_msb_barColour, 0));
        }
        if (this.a.hasValue(R.styleable.MaterialScrollBar_msb_handleColour)) {
            setHandleColour(this.a.getColor(R.styleable.MaterialScrollBar_msb_handleColour, 0));
        }
        if (this.a.hasValue(R.styleable.MaterialScrollBar_msb_handleOffColour)) {
            setHandleOffColour(this.a.getColor(R.styleable.MaterialScrollBar_msb_handleOffColour, 0));
        }
        if (this.a.hasValue(R.styleable.MaterialScrollBar_msb_barThickness)) {
            setBarThickness(this.a.getDimensionPixelSize(R.styleable.MaterialScrollBar_msb_barThickness, 0));
        }
        implementFlavourPreferences(this.a);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.seekId != 0 && this.recyclerView == null && getRootView() != null) {
            this.recyclerView = (RecyclerView) getRootView().findViewById(this.seekId);
            if (this.recyclerView != null) {
                generalSetup();
            }
        }
    }

    protected void onDown(MotionEvent motionEvent) {
        if (this.recyclerView == null && this.seekId != 0 && getRootView() != null) {
            this.recyclerView = (RecyclerView) getRootView().findViewById(this.seekId);
        }
        if (this.indicator != null && this.indicator.getVisibility() == View.INVISIBLE) {
            this.indicator.setVisibility(View.VISIBLE);
            this.indicator.setAlpha(0.0f);
            this.indicator.animate().alpha(1.0f).setDuration(250L).setListener(new AnimatorListenerAdapter() { // from class: com.meccael.tonal.scrollbar.MaterialScrollBar.1
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);
                    MaterialScrollBar.this.indicator.setAlpha(1.0f);
                }
            });
        }
        int height = this.handle.getHeight() / 2;
        int height2 = this.recyclerView != null ? this.recyclerView.getHeight() : getHeight();
        this.scrollUtils.scrollToPositionAtProgress((Math.max(height, Math.min(height2, motionEvent.getY() - getHandleOffset())) - height) / (height2 - height));
        this.scrollUtils.scrollHandleAndIndicator();
        if (this.recyclerView != null) {
            this.recyclerView.onScrolled(0, 0);
        }
        if (this.lightOnTouch) {
            this.handle.setBackgroundColor(this.handleColour);
        }
    }

    @Override // android.widget.RelativeLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.recyclerView == null && this.seekId != 0 && getRootView() != null) {
            this.recyclerView = (RecyclerView) getRootView().findViewById(this.seekId);
        }
        if (this.recyclerView == null) {
            throw new RuntimeException("You need to set a recyclerView for the scroll bar, either in the XML or using setRecyclerView().");
        }
        if (!this.sizeUnchecked || isInEditMode()) {
            return;
        }
        this.scrollUtils.getCurScrollState();
        if (this.scrollUtils.getAvailableScrollHeight() <= 0) {
            this.background.setVisibility(View.GONE);
            this.handle.setVisibility(View.GONE);
        } else {
            this.background.setVisibility(View.VISIBLE);
            this.handle.setVisibility(View.VISIBLE);
            this.sizeUnchecked = false;
        }
    }

    @Override // android.widget.RelativeLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int iMin;
        int iMin2;
        int dp = Utils.getDP(this.barWidth, this);
        int mode = View.MeasureSpec.getMode(i);
        int size = View.MeasureSpec.getSize(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int size2 = View.MeasureSpec.getSize(i2);
        if (mode == 1073741824) {
            iMin = size;
        } else {
            iMin = mode == Integer.MIN_VALUE ? Math.min(dp, size) : dp;
        }
        if (mode2 == 1073741824) {
            iMin2 = size2;
        } else {
            iMin2 = mode2 == Integer.MIN_VALUE ? Math.min(100, size2) : 100;
        }
        super.onMeasure(i, i2);
        setMeasuredDimension(iMin, iMin2);
    }

    abstract void onScroll();

    protected void onUp() {
        if (this.indicator != null && this.indicator.getVisibility() == View.VISIBLE) {
            this.indicator.animate().alpha(0.0f).setDuration(250L).setListener(new AnimatorListenerAdapter() { // from class: com.meccael.tonal.scrollbar.MaterialScrollBar.2
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);
                    MaterialScrollBar.this.indicator.setVisibility(View.INVISIBLE);
                }
            });
        }
        if (this.lightOnTouch) {
            this.handle.setBackgroundColor(this.handleOffColour);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public T setBarColour(@ColorInt int i) {
        this.background.setBackgroundColor(i);
        return (T) this;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public T setBarThickness(int i) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.handle.getLayoutParams();
        layoutParams.width = i;
        this.handle.setLayoutParams(layoutParams);
        RelativeLayout.LayoutParams layoutParams2 = (RelativeLayout.LayoutParams) this.background.getLayoutParams();
        layoutParams2.width = i;
        this.background.setLayoutParams(layoutParams2);
        if (this.indicator != null) {
            this.indicator.setSizeCustom();
        }
        RelativeLayout.LayoutParams layoutParams3 = (RelativeLayout.LayoutParams) getLayoutParams();
        layoutParams3.width = i;
        setLayoutParams(layoutParams3);
        return (T) this;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public T setHandleColour(int i) {
        this.handleColour = i;
        this.handle.setBackgroundColor(this.handleColour);
        return (T) this;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public T setHandleOffColour(@ColorInt int i) {
        this.handleOffColour = i;
        if (this.lightOnTouch) {
            this.handle.setBackgroundColor(this.handleOffColour);
        }
        return (T) this;
    }

    public void setScrollBarHidden(boolean z) {
        this.hiddenByUser = z;
        fadeOut();
    }

    abstract void setTouchIntercept();

    View setUpBackground(Context context) {
        this.background = new View(context);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(Utils.getDP(this.barWidth, this), -1);
        layoutParams.addRule(11);
        this.background.setLayoutParams(layoutParams);
        return this.background;
    }

    Handle setUpHandle(Context context, Boolean bool) {
        this.handle = new Handle(context, getMode(), this.programmatic);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(Utils.getDP(this.barWidth, this), Utils.getDP(72, this));
        layoutParams.addRule(11);
        this.handle.setLayoutParams(layoutParams);
        this.lightOnTouch = bool.booleanValue();
        this.handleColour = this.defaultHandleOffColor;
        this.handle.setBackgroundColor(bool.booleanValue() ? this.defaultHandleOffColor : this.handleColour);
        return this.handle;
    }

    void setUpProps(Context context, AttributeSet attributeSet) {
        if (attributeSet != null) {
            this.a = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.MaterialScrollBar, 0, 0);
        }
    }
}
