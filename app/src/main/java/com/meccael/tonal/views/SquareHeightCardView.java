package com.meccael.tonal.views;

import android.content.Context;
import android.util.AttributeSet;
import com.google.android.material.card.MaterialCardView;

/** A card that is as wide as it is tall: the height decides the size. */
public class SquareHeightCardView extends MaterialCardView {
    public SquareHeightCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(heightSpec, heightSpec);
    }
}
