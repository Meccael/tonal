package com.meccael.tonal.adapters;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import com.meccael.tonal.application.Fragment_Home_Carousel;

/* JADX INFO: loaded from: classes.dex */
public class CardRecentOnScroll extends RecyclerView.OnScrollListener {
    private Fragment_Home_Carousel mFragment;

    public CardRecentOnScroll(Fragment_Home_Carousel fragment_Home_Carousel) {
        this.mFragment = fragment_Home_Carousel;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
    public void onScrolled(RecyclerView recyclerView, int i, int i2) {
        super.onScrolled(recyclerView, i, i2);
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        View childAt = linearLayoutManager.getChildAt(0);
        int iFindFirstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
        if (childAt == null || iFindFirstVisibleItemPosition < 0) {
            return;
        }
        this.mFragment.refreshRecentTitle(iFindFirstVisibleItemPosition);
    }
}
