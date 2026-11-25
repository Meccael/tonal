package com.meccael.tonal.scrollbar;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
class Utils {
    Utils() {
    }

    static int getDP(int i, Context context) {
        return (int) TypedValue.applyDimension(1, i, context.getResources().getDisplayMetrics());
    }

    static int getDP(int i, View view) {
        return (int) TypedValue.applyDimension(1, i, view.getResources().getDisplayMetrics());
    }
}
