package com.meccael.tonal.views;

import android.content.Context;
import android.util.AttributeSet;
import com.google.android.material.card.MaterialCardView;

/** A card that is as tall as it is wide: the width decides the size. */
public class SquareWidthCardView extends MaterialCardView {
    public SquareWidthCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, widthSpec);
    }
}
