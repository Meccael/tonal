package com.meccael.tonal.scrollbar;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
class ScrollingUtilities {
    private int constant;
    CustomScroller customScroller;
    MaterialScrollBar materialScrollBar;
    private ScrollPositionState scrollPosState = new ScrollPositionState();

    private class ScrollPositionState {
        private int rowHeight;
        private int rowIndex;
        private int rowTopOffset;

        private ScrollPositionState() {
        }
    }

    ScrollingUtilities(MaterialScrollBar materialScrollBar) {
        this.materialScrollBar = materialScrollBar;
    }

    protected int getAvailableScrollBarHeight() {
        return this.materialScrollBar.getHeight() - this.materialScrollBar.handle.getHeight();
    }

    protected int getAvailableScrollHeight() {
        return (this.customScroller != null ? (this.materialScrollBar.getPaddingTop() + this.customScroller.getTotalDepth()) + this.materialScrollBar.getPaddingBottom() : (this.materialScrollBar.getPaddingTop() + (getRowCount() * this.scrollPosState.rowHeight)) + this.materialScrollBar.getPaddingBottom()) - this.materialScrollBar.getHeight();
    }

    public void getCurScrollState() {
        this.scrollPosState.rowIndex = -1;
        this.scrollPosState.rowTopOffset = -1;
        this.scrollPosState.rowHeight = -1;
        View childAt = this.materialScrollBar.recyclerView.getChildAt(0);
        this.scrollPosState.rowIndex = this.materialScrollBar.recyclerView.getChildAdapterPosition(childAt);
        if (this.materialScrollBar.recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            this.scrollPosState.rowIndex /= ((GridLayoutManager) this.materialScrollBar.recyclerView.getLayoutManager()).getSpanCount();
        }
        if (childAt == null) {
            this.scrollPosState.rowTopOffset = 0;
            this.scrollPosState.rowHeight = 0;
        } else {
            this.scrollPosState.rowTopOffset = this.materialScrollBar.recyclerView.getLayoutManager().getDecoratedTop(childAt);
            this.scrollPosState.rowHeight = childAt.getHeight();
        }
    }

    int getRowCount() {
        try {
            int itemCount = this.materialScrollBar.recyclerView.getLayoutManager().getItemCount();
            return this.materialScrollBar.recyclerView.getLayoutManager() instanceof GridLayoutManager ? (int) Math.ceil(((double) itemCount) / ((double) ((GridLayoutManager) this.materialScrollBar.recyclerView.getLayoutManager()).getSpanCount())) : itemCount;
        } catch (Exception e) {
            return 0;
        }
    }

    float getScrollPosition() {
        getCurScrollState();
        return ((this.constant - this.scrollPosState.rowTopOffset) / getAvailableScrollHeight()) * getAvailableScrollBarHeight();
    }

    protected void scrollHandleAndIndicator() {
        getCurScrollState();
        if (this.customScroller != null) {
            this.constant = this.customScroller.getDepthForItem(this.materialScrollBar.recyclerView.getChildAdapterPosition(this.materialScrollBar.recyclerView.getChildAt(0)));
        } else {
            this.constant = this.scrollPosState.rowHeight * this.scrollPosState.rowIndex;
        }
        int scrollPosition = (int) getScrollPosition();
        ViewCompat.setY(this.materialScrollBar.handle, scrollPosition);
        this.materialScrollBar.handle.invalidate();
        if (this.materialScrollBar.indicator != null) {
            this.materialScrollBar.indicator.setText(this.materialScrollBar.recyclerView.getLayoutManager() instanceof GridLayoutManager ? this.scrollPosState.rowIndex * ((GridLayoutManager) this.materialScrollBar.recyclerView.getLayoutManager()).getSpanCount() : this.scrollPosState.rowIndex);
            this.materialScrollBar.indicator.setScroll(scrollPosition);
        }
    }

    public void scrollToPositionAtProgress(float f) {
        if (this.customScroller != null) {
            ((LinearLayoutManager) this.materialScrollBar.recyclerView.getLayoutManager()).scrollToPosition(this.customScroller.getItemIndexForScroll(f));
            return;
        }
        int spanCount = this.materialScrollBar.recyclerView.getLayoutManager() instanceof GridLayoutManager ? ((GridLayoutManager) this.materialScrollBar.recyclerView.getLayoutManager()).getSpanCount() : 1;
        this.materialScrollBar.recyclerView.stopScroll();
        getCurScrollState();
        int availableScrollHeight = (int) (getAvailableScrollHeight() * f);
        ((LinearLayoutManager) this.materialScrollBar.recyclerView.getLayoutManager()).scrollToPositionWithOffset((spanCount * availableScrollHeight) / this.scrollPosState.rowHeight, -(availableScrollHeight % this.scrollPosState.rowHeight));
    }
}
