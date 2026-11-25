package com.meccael.tonal.scrollbar;

import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.meccael.tonal.R;

/* JADX INFO: Access modifiers changed from: package-private */
/* JADX INFO: loaded from: classes.dex */
public abstract class Indicator extends RelativeLayout {
    protected Context context;
    int indicatorHeight;
    private MaterialScrollBar materialScrollBar;
    int textSize;
    protected TextView textView;

    public Indicator(Context context) {
        super(context);
        this.indicatorHeight = 100;
        this.textSize = 50;
        this.context = context;
        this.textView = new TextView(context);
        setVisibility(4);
    }

    abstract String getTextElement(Integer num, RecyclerView.Adapter adapter);

    void linkToScrollBar(MaterialScrollBar materialScrollBar) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(1080, Utils.getDP(this.indicatorHeight, this));
        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(-2, -2);
        layoutParams2.addRule(13, -1);
        this.textView.setTextSize(1, this.textSize);
        this.textView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorWhite));
        this.textView.setBackground(ContextCompat.getDrawable(this.context, R.drawable.xml_scroll_bubble));
        this.textView.setPadding(80, 0, 80, 0);
        addView(this.textView, layoutParams2);
        ((ViewGroup) materialScrollBar.getParent()).addView(this, layoutParams);
        this.materialScrollBar = materialScrollBar;
    }

    void setScroll(float f) {
        if (getVisibility() == 0) {
            float f2 = f / 2.0f;
            ViewCompat.setY(this, ((this.materialScrollBar.getHeight() / 4) + f2) - (((this.indicatorHeight / 2) * f2) / f2));
        }
    }

    public void setSizeCustom() {
        setLayoutParams((RelativeLayout.LayoutParams) getLayoutParams());
    }

    void setText(int i) {
        String textElement = getTextElement(Integer.valueOf(i), this.materialScrollBar.recyclerView.getAdapter());
        if (this.textView.getText().equals(textElement)) {
            return;
        }
        this.textView.setText(textElement);
        LayoutWrapContentUpdater.wrapContentAgain(this);
    }

    abstract void testAdapter(RecyclerView.Adapter adapter);
}
