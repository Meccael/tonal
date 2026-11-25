package com.meccael.tonal.scrollbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;

/* JADX INFO: loaded from: classes.dex */
public class TouchScrollBar extends MaterialScrollBar<TouchScrollBar> {
    private Runnable fadeBar;
    private boolean hide;
    private int hideDuration;
    private boolean respondToTouch;
    private Handler uiHandler;

    public TouchScrollBar(Context context, RecyclerView recyclerView, boolean z) {
        super(context, recyclerView, z);
        this.uiHandler = new Handler(Looper.getMainLooper());
        this.hide = true;
        this.hideDuration = 1000;
        this.respondToTouch = true;
        this.fadeBar = new Runnable() { // from class: com.meccael.tonal.scrollbar.TouchScrollBar.1
            @Override // java.lang.Runnable
            public void run() {
                TouchScrollBar.this.fadeOut();
            }
        };
    }

    public TouchScrollBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.uiHandler = new Handler(Looper.getMainLooper());
        this.hide = true;
        this.hideDuration = 1000;
        this.respondToTouch = true;
        this.fadeBar = new Runnable() { // from class: com.meccael.tonal.scrollbar.TouchScrollBar.1
            @Override // java.lang.Runnable
            public void run() {
                TouchScrollBar.this.fadeOut();
            }
        };
    }

    public TouchScrollBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.uiHandler = new Handler(Looper.getMainLooper());
        this.hide = true;
        this.hideDuration = 1000;
        this.respondToTouch = true;
        this.fadeBar = new Runnable() { // from class: com.meccael.tonal.scrollbar.TouchScrollBar.1
            @Override // java.lang.Runnable
            public void run() {
                TouchScrollBar.this.fadeOut();
            }
        };
    }

    @Override // com.meccael.tonal.scrollbar.MaterialScrollBar
    float getHandleOffset() {
        return 0.0f;
    }

    @Override // com.meccael.tonal.scrollbar.MaterialScrollBar
    boolean getHide() {
        return true;
    }

    @Override // com.meccael.tonal.scrollbar.MaterialScrollBar
    float getHideRatio() {
        return 1.0f;
    }

    @Override // com.meccael.tonal.scrollbar.MaterialScrollBar
    int getMode() {
        return 1;
    }

    @Override // com.meccael.tonal.scrollbar.MaterialScrollBar
    void implementFlavourPreferences(TypedArray typedArray) {
        if (typedArray.hasValue(com.meccael.tonal.R.styleable.MaterialScrollBar_msb_autoHide)) {
            setAutoHide(Boolean.valueOf(typedArray.getBoolean(com.meccael.tonal.R.styleable.MaterialScrollBar_msb_autoHide, true)));
        }
        if (typedArray.hasValue(com.meccael.tonal.R.styleable.MaterialScrollBar_msb_hideDelayInMilliseconds)) {
            this.hideDuration = typedArray.getInteger(com.meccael.tonal.R.styleable.MaterialScrollBar_msb_hideDelayInMilliseconds, 1000);
        }
    }

    @Override // com.meccael.tonal.scrollbar.MaterialScrollBar
    void onScroll() {
        if (this.hide) {
            this.uiHandler.removeCallbacks(this.fadeBar);
            this.uiHandler.postDelayed(this.fadeBar, this.hideDuration);
            fadeIn();
        }
    }

    public TouchScrollBar setAutoHide(Boolean bool) {
        if (!bool.booleanValue()) {
            TranslateAnimation translateAnimation = new TranslateAnimation(2, 0.0f, 1, 0.0f, 2, 0.0f, 2, 0.0f);
            translateAnimation.setFillAfter(true);
            startAnimation(translateAnimation);
        }
        this.hide = bool.booleanValue();
        return this;
    }

    @Override // com.meccael.tonal.scrollbar.MaterialScrollBar
    public /* bridge */ /* synthetic */ void setScrollBarHidden(boolean z) {
        super.setScrollBarHidden(z);
    }

    @Override // com.meccael.tonal.scrollbar.MaterialScrollBar
    void setTouchIntercept() {
        setOnTouchListener(new View.OnTouchListener() { // from class: com.meccael.tonal.scrollbar.TouchScrollBar.2
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (TouchScrollBar.this.hiddenByUser) {
                    return false;
                }
                if (motionEvent.getAction() == 1) {
                    TouchScrollBar.this.onUp();
                    if (!TouchScrollBar.this.hide) {
                        return true;
                    }
                    TouchScrollBar.this.uiHandler.removeCallbacks(TouchScrollBar.this.fadeBar);
                    TouchScrollBar.this.uiHandler.postDelayed(TouchScrollBar.this.fadeBar, TouchScrollBar.this.hideDuration);
                    return true;
                }
                if (TouchScrollBar.this.hidden && !TouchScrollBar.this.respondToTouch) {
                    return true;
                }
                TouchScrollBar.this.onDown(motionEvent);
                if (!TouchScrollBar.this.hide) {
                    return true;
                }
                TouchScrollBar.this.uiHandler.removeCallbacks(TouchScrollBar.this.fadeBar);
                TouchScrollBar.this.fadeIn();
                return true;
            }
        });
    }
}
