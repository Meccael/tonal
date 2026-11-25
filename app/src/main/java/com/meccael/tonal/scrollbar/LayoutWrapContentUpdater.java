package com.meccael.tonal.scrollbar;

import android.view.View;
import android.view.ViewGroup;

/* JADX INFO: loaded from: classes.dex */
public class LayoutWrapContentUpdater {
    static final /* synthetic */ boolean $assertionsDisabled;

    static {
        $assertionsDisabled = !LayoutWrapContentUpdater.class.desiredAssertionStatus();
    }

    private static void recurseWrapContent(View view, boolean z) {
        if (view.getVisibility() == 8) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        boolean z2 = layoutParams.width == -2 || z;
        boolean z3 = layoutParams.height == -2 || z;
        if (z2 || z3) {
            boolean z4 = false;
            int right = view.getRight();
            int bottom = view.getBottom();
            if (z2 && view.getMeasuredWidth() > 0) {
                right = view.getLeft() + view.getMeasuredWidth();
                z4 = true;
            }
            if (z3 && view.getMeasuredHeight() > 0) {
                bottom = view.getTop() + view.getMeasuredHeight();
                z4 = true;
            }
            if (z4) {
                view.layout(view.getLeft(), view.getTop(), right, bottom);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                recurseWrapContent(viewGroup.getChildAt(i), z);
            }
        }
    }

    public static void wrapContentAgain(ViewGroup viewGroup) {
        wrapContentAgain(viewGroup, false, 0, 0);
    }

    public static void wrapContentAgain(ViewGroup viewGroup, boolean z, int i, int i2) {
        if (!$assertionsDisabled && !"main".equals(Thread.currentThread().getName())) {
            throw new AssertionError();
        }
        if (viewGroup == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = viewGroup.getLayoutParams();
        int iMakeMeasureSpec = i;
        if (layoutParams.width != -2 && viewGroup.getWidth() > 0) {
            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(viewGroup.getWidth(), 1073741824);
        }
        int iMakeMeasureSpec2 = i2;
        if (layoutParams.height != -2 && viewGroup.getHeight() > 0) {
            iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(viewGroup.getHeight(), 1073741824);
        }
        viewGroup.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
        recurseWrapContent(viewGroup, z);
        viewGroup.requestLayout();
    }
}
