package com.meccael.tonal.scrollbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
public class Handle extends View {
    Context context;
    boolean expanded;
    Integer mode;
    Paint p;
    Boolean programmatic;
    RectF rectF;

    public Handle(Context context, int i, boolean z) {
        super(context);
        this.p = new Paint();
        this.expanded = false;
        this.context = context;
        this.mode = Integer.valueOf(i);
        this.p.setFlags(1);
        this.programmatic = Boolean.valueOf(z);
    }

    private RectF makeRect() {
        return this.programmatic.booleanValue() ? new RectF(new Rect(getLeft() - Utils.getDP(11, this.context), getTop(), getLeft() - Utils.getDP(4, this.context), getBottom())) : new RectF(new Rect(getLeft() - Utils.getDP(4, this.context), getTop(), getLeft() + Utils.getDP(6, this.context), getBottom()));
    }

    public void collapseHandle() {
        this.expanded = true;
        this.rectF = new RectF(new Rect(getLeft(), getTop(), getLeft(), getBottom()));
        invalidate();
    }

    public void expandHandle() {
        this.expanded = false;
        this.rectF = makeRect();
        invalidate();
    }

    @Override // android.view.View
    protected void onAnimationEnd() {
        super.onAnimationEnd();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mode.intValue() != 0 || this.expanded) {
            return;
        }
        Rect clipBounds = canvas.getClipBounds();
        clipBounds.inset(getLeft() - Utils.getDP(30, this.context), 0);
        canvas.clipRect(clipBounds, Region.Op.REPLACE);
        canvas.drawArc(this.rectF, 90.0f, 180.0f, false, this.p);
    }

    @Override // android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mode.intValue() == 0) {
            this.rectF = makeRect();
        }
    }
}
