package com.meccael.tonal.lyrics;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.meccael.tonal.R;

/**
 * Shows synced lyrics. The current line is bright and centered, neighbouring lines are
 * dimmed, and the list glides as playback advances. Call setPositionMs() from the same
 * place that updates the player's time label.
 *
 * The view ignores touch input, so it never competes with the drawer or other gestures.
 */
public class LyricsView extends RecyclerView {

    /** Show each line this many ms early, to offset the coarse update tick. */
    private static final long LEAD_MS = 250L;
    /** Bigger = slower glide between lines. */
    private static final float GLIDE = 500f;
    /** Jumps larger than this many lines (seeking) are not animated. */
    private static final int MAX_ANIMATED_JUMP = 3;

    private final LinearLayoutManager layoutManager;
    private final LineAdapter adapter = new LineAdapter();
    private List<LyricLine> lines = Collections.emptyList();
    private boolean hasLyrics = false;
    private int current = -1;
    private long delayMs = 0L;
    private long lastPositionMs = 0L;
    private OnLineClickListener clickListener;

    /** Called when a line is tapped. timeMs is the playback position that makes that line current. */
    public interface OnLineClickListener {
        void onLineClick(long timeMs);
    }

    public LyricsView(@NonNull Context context) {
        this(context, null);
    }

    public LyricsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LyricsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        layoutManager = new LinearLayoutManager(context);
        setLayoutManager(layoutManager);
        setAdapter(adapter);
        setItemAnimator(null);
        setClipToPadding(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setNestedScrollingEnabled(false);
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(dp(24));
    }

    // ---------------------------------------------------------------- public API

    /** Empty the view (call when a new track starts). */
    public void clear() {
        hasLyrics = false;
        lines = Collections.emptyList();
        current = -1;
        adapter.notifyDataSetChanged();
    }

    /** Show a single dim, centered message such as "Searching lyrics…". */
    public void showMessage(String text) {
        hasLyrics = false;
        lines = Collections.singletonList(new LyricLine(0L, text));
        current = -1;
        adapter.notifyDataSetChanged();
        post(new Runnable() {
            @Override
            public void run() {
                smoothTo(0);
            }
        });
    }

    /** Show synced lyrics. */
    public void setLines(List<LyricLine> newLines) {
        hasLyrics = newLines != null && !newLines.isEmpty();
        lines = hasLyrics ? new ArrayList<LyricLine>(newLines) : Collections.<LyricLine>emptyList();
        current = -1;
        adapter.notifyDataSetChanged();
        scrollToPosition(0);
    }

    public void setOnLineClickListener(OnLineClickListener listener) {
        this.clickListener = listener;
    }

    /** Shift the lyrics later (positive) or earlier (negative) for tracks whose timing is off. */
    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
        setPositionMs(lastPositionMs);
    }

    public long getDelayMs() {
        return delayMs;
    }

    /** Tell the view where playback is. Cheap to call on every progress tick. */
    public void setPositionMs(long positionMs) {
        lastPositionMs = positionMs;
        if (!hasLyrics) {
            return;
        }
        int index = indexFor(positionMs + LEAD_MS - delayMs);
        if (index == current) {
            return;
        }
        int old = current;
        current = index;
        if (old >= 0) {
            adapter.notifyItemChanged(old);
        }
        if (index >= 0) {
            adapter.notifyItemChanged(index);
            boolean animate = old >= 0 && Math.abs(index - old) <= MAX_ANIMATED_JUMP;
            centerOn(index, animate);
        }
    }

    // ---------------------------------------------------------------- internals

    /** Index of the last line whose time is <= t, or -1 before the first line. */
    private int indexFor(long t) {
        int lo = 0;
        int hi = lines.size() - 1;
        int result = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (lines.get(mid).timeMs <= t) {
                result = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }

    private void centerOn(final int index, boolean animate) {
        if (animate) {
            smoothTo(index);
        } else {
            layoutManager.scrollToPositionWithOffset(index, 0);
            post(new Runnable() {
                @Override
                public void run() {
                    smoothTo(index);
                }
            });
        }
    }

    private void smoothTo(int position) {
        if (position < 0 || position >= lines.size()) {
            return;
        }
        LinearSmoothScroller scroller = new LinearSmoothScroller(getContext()) {
            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd,
                                        int snapPreference) {
                // Put the middle of the line on the middle of the view.
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_ANY;
            }

            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return GLIDE / displayMetrics.densityDpi;
            }
        };
        scroller.setTargetPosition(position);
        layoutManager.startSmoothScroll(scroller);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Half-view padding lets the first and last lines reach the center.
        int pad = h / 2;
        if (getPaddingTop() != pad || getPaddingBottom() != pad) {
            setPadding(getPaddingLeft(), pad, getPaddingRight(), pad);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ---------------------------------------------------------------- adapter

    private static final class LineHolder extends RecyclerView.ViewHolder {
        final TextView text;

        LineHolder(TextView itemView) {
            super(itemView);
            text = itemView;
        }
    }

    private final class LineAdapter extends RecyclerView.Adapter<LineHolder> {
        @NonNull
        @Override
        public LineHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dp(28), dp(3), dp(28), dp(3));
            tv.setTextSize(16f);
            android.util.TypedValue ripple = new android.util.TypedValue();
            parent.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
            tv.setBackgroundResource(ripple.resourceId);
            final LineHolder holder = new LineHolder(tv);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getBindingAdapterPosition();
                    if (hasLyrics && clickListener != null && pos >= 0 && pos < lines.size()) {
                        clickListener.onLineClick(lines.get(pos).timeMs + delayMs);
                    }
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull LineHolder holder, int position) {
            LyricLine line = lines.get(position);
            Context c = holder.text.getContext();
            boolean isCurrent = hasLyrics && position == current;

            // Blank timed lines are instrumental gaps; show a note instead of a hole.
            holder.text.setText(line.text.isEmpty() ? "\u266A" : line.text);
            holder.text.setTextColor(ContextCompat.getColor(c,
                    isCurrent ? R.color.colorWhite : R.color.colorLight));
            holder.text.setAlpha(isCurrent ? 1f : (hasLyrics ? 0.55f : 0.7f));
            holder.text.setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
        }

        @Override
        public int getItemCount() {
            return lines.size();
        }
    }
}
