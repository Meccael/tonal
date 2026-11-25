package com.meccael.tonal.wheelbar;

import android.content.res.Resources;
import android.util.TypedValue;

/* JADX INFO: loaded from: classes.dex */
class Utils {
    Utils() {
    }

    static int convertToPx(int i, Resources resources) {
        return (int) TypedValue.applyDimension(1, i, resources.getDisplayMetrics());
    }
}
