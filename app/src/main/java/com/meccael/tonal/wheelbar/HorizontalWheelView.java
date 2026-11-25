package com.meccael.tonal.wheelbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.meccael.tonal.R;

/* JADX INFO: loaded from: classes.dex */
public class HorizontalWheelView extends View {
    private static final int DEFAULT_ACTIVE_COLOR = -11227920;
    private static final boolean DEFAULT_END_LOCK = false;
    private static final int DEFAULT_MARKS_COUNT = 24;
    private static final int DEFAULT_NORMAL_COLOR = -1;
    private static final boolean DEFAULT_ONLY_POSITIVE_VALUES = false;
    private static final boolean DEFAULT_SHOW_ACTIVE_RANGE = true;
    private static final boolean DEFAULT_SNAP_TO_MARKS = false;
    private static final int DP_DEFAULT_HEIGHT = 32;
    private static final int DP_DEFAULT_WIDTH = 200;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_SETTLING = 2;
    private double angle;
    private Drawer drawer;
    private boolean endLock;
    private Listener listener;
    private boolean onlyPositiveValues;
    private TouchHandler touchHandler;

    public static class Listener {
        public void onRotationChanged(double d) {
        }

        public void onScrollStateChanged(int i) {
        }
    }

    public HorizontalWheelView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.drawer = new Drawer(this);
        this.touchHandler = new TouchHandler(this);
        readAttrs(attributeSet);
    }

    private boolean checkEndLock(double d) {
        if (!this.endLock) {
            return false;
        }
        boolean z = false;
        if (d >= 6.283185307179586d) {
            this.angle = Math.nextAfter(6.283185307179586d, Double.NEGATIVE_INFINITY);
            z = DEFAULT_SHOW_ACTIVE_RANGE;
        } else if (this.onlyPositiveValues && d < 0.0d) {
            this.angle = 0.0d;
            z = DEFAULT_SHOW_ACTIVE_RANGE;
        } else if (d <= -6.283185307179586d) {
            this.angle = Math.nextAfter(-6.283185307179586d, Double.POSITIVE_INFINITY);
            z = DEFAULT_SHOW_ACTIVE_RANGE;
        }
        if (!z) {
            return z;
        }
        this.touchHandler.cancelFling();
        return z;
    }

    private void readAttrs(AttributeSet attributeSet) {
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.HorizontalWheelView);
        this.drawer.setMarksCount(typedArrayObtainStyledAttributes.getInt(R.styleable.HorizontalWheelView_marksCount, 24));
        this.drawer.setNormalColor(typedArrayObtainStyledAttributes.getColor(R.styleable.HorizontalWheelView_normalColor, -1));
        this.drawer.setActiveColor(typedArrayObtainStyledAttributes.getColor(R.styleable.HorizontalWheelView_activeColor, DEFAULT_ACTIVE_COLOR));
        this.drawer.setShowActiveRange(typedArrayObtainStyledAttributes.getBoolean(R.styleable.HorizontalWheelView_showActiveRange, DEFAULT_SHOW_ACTIVE_RANGE));
        this.touchHandler.setSnapToMarks(typedArrayObtainStyledAttributes.getBoolean(R.styleable.HorizontalWheelView_snapToMarks, false));
        this.endLock = typedArrayObtainStyledAttributes.getBoolean(R.styleable.HorizontalWheelView_endLock, false);
        this.onlyPositiveValues = typedArrayObtainStyledAttributes.getBoolean(R.styleable.HorizontalWheelView_onlyPositiveValues, false);
        typedArrayObtainStyledAttributes.recycle();
    }

    private int resolveMeasureSpec(int i, int i2) {
        int mode = View.MeasureSpec.getMode(i);
        if (mode == 1073741824) {
            return i;
        }
        int iConvertToPx = Utils.convertToPx(i2, getResources());
        if (mode == Integer.MIN_VALUE) {
            iConvertToPx = Math.min(iConvertToPx, View.MeasureSpec.getSize(i));
        }
        return View.MeasureSpec.makeMeasureSpec(iConvertToPx, 1073741824);
    }

    public double getCompleteTurnFraction() {
        return getRadiansAngle() / 6.283185307179586d;
    }

    public double getDegreesAngle() {
        return (getRadiansAngle() * 180.0d) / 3.141592653589793d;
    }

    int getMarksCount() {
        return this.drawer.getMarksCount();
    }

    public double getRadiansAngle() {
        return this.angle;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        this.drawer.onDraw(canvas);
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(resolveMeasureSpec(i, 200), resolveMeasureSpec(i2, 32));
    }

    @Override // android.view.View
    public void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.angle = savedState.angle;
        invalidate();
    }

    @Override // android.view.View
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.angle = this.angle;
        return savedState;
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.drawer.onSizeChanged();
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.touchHandler.onTouchEvent(motionEvent);
    }

    public void setActiveColor(int i) {
        this.drawer.setActiveColor(i);
        invalidate();
    }

    public void setCompleteTurnFraction(double d) {
        setRadiansAngle(2.0d * d * 3.141592653589793d);
    }

    public void setDegreesAngle(double d) {
        setRadiansAngle((3.141592653589793d * d) / 180.0d);
    }

    public void setEndLock(boolean z) {
        this.endLock = z;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        this.touchHandler.setListener(listener);
    }

    public void setMarksCount(int i) {
        this.drawer.setMarksCount(i);
        invalidate();
    }

    public void setNormaColor(int i) {
        this.drawer.setNormalColor(i);
        invalidate();
    }

    public void setOnlyPositiveValues(boolean z) {
        this.onlyPositiveValues = z;
    }

    public void setRadiansAngle(double d) {
        if (!checkEndLock(d)) {
            this.angle = d % 6.283185307179586d;
        }
        if (this.onlyPositiveValues && this.angle < 0.0d) {
            this.angle += 6.283185307179586d;
        }
        invalidate();
        if (this.listener != null) {
            this.listener.onRotationChanged(this.angle);
        }
    }

    public void setShowActiveRange(boolean z) {
        this.drawer.setShowActiveRange(z);
        invalidate();
    }

    public void setSnapToMarks(boolean z) {
        this.touchHandler.setSnapToMarks(z);
    }
}
