package com.meccael.tonal.carousel;

import android.database.DataSetObserver;
import android.os.Parcelable;
import androidx.viewpager.widget.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

/* JADX INFO: loaded from: classes.dex */
class InfiniteCyclePagerAdapter extends PagerAdapter {
    private static final int VIRTUAL_ITEM_COUNT = 10000;
    private OnNotifyDataSetChangedListener mOnNotifyDataSetChangedListener;
    private final PagerAdapter mPagerAdapter;

    protected interface OnNotifyDataSetChangedListener {
        void onChanged();
    }

    public InfiniteCyclePagerAdapter(PagerAdapter pagerAdapter) {
        this.mPagerAdapter = pagerAdapter;
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        this.mPagerAdapter.destroyItem(viewGroup, getVirtualPosition(i), obj);
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public void finishUpdate(ViewGroup viewGroup) {
        this.mPagerAdapter.finishUpdate(viewGroup);
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public int getCount() {
        if (this.mPagerAdapter.getCount() == 0) {
            return 0;
        }
        return VIRTUAL_ITEM_COUNT;
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public int getItemPosition(Object obj) {
        return this.mPagerAdapter.getItemPosition(obj);
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public CharSequence getPageTitle(int i) {
        return this.mPagerAdapter.getPageTitle(getVirtualPosition(i));
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public float getPageWidth(int i) {
        return this.mPagerAdapter.getPageWidth(getVirtualPosition(i));
    }

    public PagerAdapter getPagerAdapter() {
        return this.mPagerAdapter;
    }

    protected int getVirtualPosition(int i) {
        return i % this.mPagerAdapter.getCount();
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public Object instantiateItem(ViewGroup viewGroup, int i) {
        return this.mPagerAdapter.instantiateItem(viewGroup, getVirtualPosition(i));
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public boolean isViewFromObject(View view, Object obj) {
        return this.mPagerAdapter.isViewFromObject(view, obj);
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public void notifyDataSetChanged() {
        this.mPagerAdapter.notifyDataSetChanged();
        if (this.mOnNotifyDataSetChangedListener != null) {
            this.mOnNotifyDataSetChangedListener.onChanged();
        }
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        this.mPagerAdapter.registerDataSetObserver(dataSetObserver);
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public void restoreState(Parcelable parcelable, ClassLoader classLoader) {
        this.mPagerAdapter.restoreState(parcelable, classLoader);
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public Parcelable saveState() {
        return this.mPagerAdapter.saveState();
    }

    public void setOnNotifyDataSetChangedListener(OnNotifyDataSetChangedListener onNotifyDataSetChangedListener) {
        this.mOnNotifyDataSetChangedListener = onNotifyDataSetChangedListener;
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public void setPrimaryItem(ViewGroup viewGroup, int i, Object obj) {
        this.mPagerAdapter.setPrimaryItem(viewGroup, i, obj);
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public void startUpdate(ViewGroup viewGroup) {
        this.mPagerAdapter.startUpdate(viewGroup);
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        this.mPagerAdapter.unregisterDataSetObserver(dataSetObserver);
    }
}
