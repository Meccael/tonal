package com.meccael.tonal.adapters;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/** Lets the queue rows be dragged up and down to reorder, or swiped left to remove. */
public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private final AdapterMusicMoveable mAdapter;

    public SimpleItemTouchHelperCallback(AdapterMusicMoveable adapter) {
        mAdapter = adapter;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT);
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
        mAdapter.onItemMove(from.getAdapterPosition(), to.getAdapterPosition());
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void clearView(final RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        // Renumber the rows once a drag or swipe has finished.
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.refreshNumbers();
            }
        });
    }
}
