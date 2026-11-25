package com.meccael.tonal.views;

import android.content.Context;
import androidx.percentlayout.widget.PercentRelativeLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
public class CustomRelativeLayout extends PercentRelativeLayout {
    public CustomRelativeLayout(Context context) {
        this(context, null, 0);
    }

    public CustomRelativeLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public CustomRelativeLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        ViewCompat.setOnApplyWindowInsetsListener(this, (view, windowInsetsCompat) -> {
            CustomRelativeLayout.this.setWindowInsets(windowInsetsCompat);
            return windowInsetsCompat;
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setWindowInsets(WindowInsetsCompat windowInsetsCompat) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            WindowInsetsCompat result = ViewCompat.dispatchApplyWindowInsets(getChildAt(i), windowInsetsCompat);
            if (result != null && result.isConsumed()) {
                return;
            }
        }
    }
}
