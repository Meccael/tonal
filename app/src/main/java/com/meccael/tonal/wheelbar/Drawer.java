package com.meccael.tonal.wheelbar;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
class Drawer {
    private static final float CURSOR_RELATIVE_HEIGHT = 1.0f;
    private static final int DP_CURSOR_CORNERS_RADIUS = 3;
    private static final int DP_CURSOR_WIDTH = 3;
    private static final int DP_NORMAL_MARK_WIDTH = 1;
    private static final int DP_ZERO_MARK_WIDTH = 2;
    private static final float NORMAL_MARK_RELATIVE_HEIGHT = 0.6f;
    private static final float SCALE_RANGE = 0.1f;
    private static final float SHADE_RANGE = 0.7f;
    private static final float ZERO_MARK_RELATIVE_HEIGHT = 0.8f;
    private int activeColor;
    private int cursorCornersRadius;
    private float[] gaps;
    private int marksCount;
    private int maxVisibleMarksCount;
    private int normalColor;
    private int normalMarkHeight;
    private int normalMarkWidth;
    private float[] scales;
    private float[] shades;
    private boolean showActiveRange;
    private HorizontalWheelView view;
    private int viewportHeight;
    private int zeroMarkHeight;
    private int zeroMarkWidth;
    private Paint paint = new Paint(1);
    private int[] colorSwitches = {-1, -1, -1};
    private RectF cursorRect = new RectF();

    Drawer(HorizontalWheelView horizontalWheelView) {
        this.view = horizontalWheelView;
        initDpSizes();
    }

    private int applyShade(int i, float f) {
        return Color.argb((int) (255.0f * f), Color.red(i), Color.green(i), Color.blue(i));
    }

    private int calcZeroIndex(double d) {
        double radiansAngle = ((this.view.getRadiansAngle() + 1.5707963267948966d) + 6.283185307179586d) % 6.283185307179586d;
        if (radiansAngle > 3.141592653589793d) {
            return -1;
        }
        return (int) ((3.141592653589793d - radiansAngle) / d);
    }

    private int convertToPx(int i) {
        return Utils.convertToPx(i, this.view.getResources());
    }

    private void drawCursor(Canvas canvas) {
        this.paint.setStrokeWidth(0.0f);
        this.paint.setColor(this.activeColor);
        canvas.drawRoundRect(this.cursorRect, this.cursorCornersRadius, this.cursorCornersRadius, this.paint);
    }

    private void drawMarks(Canvas canvas, int i) {
        float paddingLeft = this.view.getPaddingLeft();
        int i2 = this.normalColor;
        int i3 = 0;
        for (int i4 = 0; i4 < this.gaps.length && this.gaps[i4] != -1.0f; i4++) {
            paddingLeft += this.gaps[i4];
            while (i3 < 3 && i4 == this.colorSwitches[i3]) {
                i2 = i2 == this.normalColor ? this.activeColor : this.normalColor;
                i3++;
            }
            if (i4 != i) {
                drawNormalMark(canvas, paddingLeft, this.scales[i4], this.shades[i4], i2);
            } else {
                drawZeroMark(canvas, paddingLeft, this.scales[i4], this.shades[i4]);
            }
        }
    }

    private void drawNormalMark(Canvas canvas, float f, float f2, float f3, int i) {
        float f4 = this.normalMarkHeight * f2;
        float paddingTop = this.view.getPaddingTop() + ((this.viewportHeight - f4) / 2.0f);
        this.paint.setStrokeWidth(this.normalMarkWidth);
        this.paint.setColor(applyShade(i, f3));
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(f, paddingTop, f, paddingTop + f4, this.paint);
    }

    private void drawZeroMark(Canvas canvas, float f, float f2, float f3) {
        float f4 = this.zeroMarkHeight * f2;
        float paddingTop = this.view.getPaddingTop() + ((this.viewportHeight - f4) / 2.0f);
        this.paint.setStrokeWidth(this.zeroMarkWidth);
        this.paint.setColor(applyShade(this.activeColor, f3));
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(f, paddingTop, f, paddingTop + f4, this.paint);
    }

    private void initDpSizes() {
        this.normalMarkWidth = convertToPx(1);
        this.zeroMarkWidth = convertToPx(2);
        this.cursorCornersRadius = convertToPx(3);
    }

    private void setupColorSwitches(double d, double d2, int i) {
        if (!this.showActiveRange) {
            Arrays.fill(this.colorSwitches, -1);
            return;
        }
        double radiansAngle = this.view.getRadiansAngle();
        int i2 = d2 < 1.5707963267948966d ? ((int) ((1.5707963267948966d - d2) / d)) + 1 : 0;
        if (radiansAngle > 4.71238898038469d) {
            this.colorSwitches[0] = 0;
            this.colorSwitches[1] = i2;
            this.colorSwitches[2] = i;
            return;
        }
        if (radiansAngle >= 0.0d) {
            this.colorSwitches[0] = Math.max(0, i);
            this.colorSwitches[1] = i2;
            this.colorSwitches[2] = -1;
        } else if (radiansAngle < -4.71238898038469d) {
            this.colorSwitches[0] = 0;
            this.colorSwitches[1] = i;
            this.colorSwitches[2] = i2;
        } else if (radiansAngle < 0.0d) {
            this.colorSwitches[0] = i2;
            this.colorSwitches[1] = i;
            this.colorSwitches[2] = -1;
        }
    }

    private void setupCursorRect() {
        int i = (int) (this.viewportHeight * CURSOR_RELATIVE_HEIGHT);
        this.cursorRect.top = this.view.getPaddingTop() + ((this.viewportHeight - i) / 2);
        this.cursorRect.bottom = this.cursorRect.top + i;
        int iConvertToPx = convertToPx(3);
        this.cursorRect.left = (this.view.getWidth() - iConvertToPx) / 2;
        this.cursorRect.right = this.cursorRect.left + iConvertToPx;
    }

    private void setupGaps(double d, double d2) {
        this.gaps[0] = (float) Math.sin(d2 / 2.0d);
        float f = this.gaps[0];
        double d3 = d2;
        int i = 1;
        while (d3 + d <= 3.141592653589793d) {
            this.gaps[i] = (float) Math.sin((d / 2.0d) + d3);
            f += this.gaps[i];
            d3 += d;
            i++;
        }
        float fSin = f + ((float) Math.sin((3.141592653589793d + d3) / 2.0d));
        if (i != this.gaps.length) {
            this.gaps[this.gaps.length - 1] = -1.0f;
        }
        float width = this.view.getWidth() / fSin;
        for (int i2 = 0; i2 < this.gaps.length; i2++) {
            if (this.gaps[i2] != -1.0f) {
                float[] fArr = this.gaps;
                fArr[i2] = fArr[i2] * width;
            }
        }
    }

    private void setupShadesAndScales(double d, double d2) {
        double d3 = d2;
        for (int i = 0; i < this.maxVisibleMarksCount; i++) {
            double dSin = Math.sin(d3);
            this.shades[i] = (float) (1.0d - (0.699999988079071d * (1.0d - dSin)));
            this.scales[i] = (float) (1.0d - (0.10000000149011612d * (1.0d - dSin)));
            d3 += d;
        }
    }

    int getMarksCount() {
        return this.marksCount;
    }

    void onDraw(Canvas canvas) {
        double d = 6.283185307179586d / ((double) this.marksCount);
        double radiansAngle = (1.5707963267948966d - this.view.getRadiansAngle()) % d;
        if (radiansAngle < 0.0d) {
            radiansAngle += d;
        }
        setupGaps(d, radiansAngle);
        setupShadesAndScales(d, radiansAngle);
        int iCalcZeroIndex = calcZeroIndex(d);
        setupColorSwitches(d, radiansAngle, iCalcZeroIndex);
        drawMarks(canvas, iCalcZeroIndex);
        drawCursor(canvas);
    }

    void onSizeChanged() {
        this.viewportHeight = (this.view.getHeight() - this.view.getPaddingTop()) - this.view.getPaddingBottom();
        this.normalMarkHeight = (int) (this.viewportHeight * NORMAL_MARK_RELATIVE_HEIGHT);
        this.zeroMarkHeight = (int) (this.viewportHeight * ZERO_MARK_RELATIVE_HEIGHT);
        setupCursorRect();
    }

    void setActiveColor(int i) {
        this.activeColor = i;
    }

    void setMarksCount(int i) {
        this.marksCount = i;
        this.maxVisibleMarksCount = (i / 2) + 1;
        this.gaps = new float[this.maxVisibleMarksCount];
        this.shades = new float[this.maxVisibleMarksCount];
        this.scales = new float[this.maxVisibleMarksCount];
    }

    void setNormalColor(int i) {
        this.normalColor = i;
    }

    void setShowActiveRange(boolean z) {
        this.showActiveRange = z;
    }
}
