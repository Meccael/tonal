package com.meccael.tonal.scrollbar;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

/* JADX INFO: loaded from: classes.dex */
public class IndicatorAlphabet extends Indicator {
    public IndicatorAlphabet(Context context) {
        super(context);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.meccael.tonal.scrollbar.Indicator
    String getTextElement(Integer num, RecyclerView.Adapter adapter) {
        return String.valueOf(Character.toUpperCase(((IndicatorAdapter) adapter).getCharacterForElement(num.intValue()).charValue()));
    }

    @Override // com.meccael.tonal.scrollbar.Indicator
    public /* bridge */ /* synthetic */ void setSizeCustom() {
        super.setSizeCustom();
    }

    @Override // com.meccael.tonal.scrollbar.Indicator
    void testAdapter(RecyclerView.Adapter adapter) {
        if (!(adapter instanceof IndicatorAdapter)) {
            throw new CustomExceptions.AdapterNotSetupForIndicatorException(adapter.getClass(), "IndicatorAdapter");
        }
    }
}
