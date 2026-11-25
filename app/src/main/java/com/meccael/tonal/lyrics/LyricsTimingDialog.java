package com.meccael.tonal.lyrics;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Locale;
import com.meccael.tonal.R;

/** Nudge the lyrics earlier or later for the current track. Changes apply live and are remembered. */
public final class LyricsTimingDialog {
    private LyricsTimingDialog() {}

    public static void show(final Context ctx, final long trackId, final LyricsView view) {
        final long[] delay = {LyricsTimingStore.get(ctx, trackId)};

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(ctx, 24);
        root.setPadding(pad, dp(ctx, 8), pad, 0);

        final TextView value = new TextView(ctx);
        value.setTextSize(28f);
        value.setGravity(Gravity.CENTER);
        value.setPadding(0, dp(ctx, 8), 0, dp(ctx, 4));
        root.addView(value, new LinearLayout.LayoutParams(-1, -2));

        TextView hint = new TextView(ctx);
        hint.setText(R.string.lyrics_timing_hint);
        hint.setGravity(Gravity.CENTER);
        hint.setAlpha(0.7f);
        hint.setPadding(0, 0, 0, dp(ctx, 12));
        root.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                value.setText(delay[0] == 0 ? "0.0 s" : String.format(Locale.getDefault(), "%+.1f s", delay[0] / 1000.0));
            }
        };
        refresh.run();

        final Runnable apply = new Runnable() {
            @Override
            public void run() {
                delay[0] = Math.max(-LyricsTimingStore.LIMIT_MS, Math.min(LyricsTimingStore.LIMIT_MS, delay[0]));
                LyricsTimingStore.set(ctx, trackId, delay[0]);
                if (view != null) view.setDelayMs(delay[0]);
                refresh.run();
            }
        };

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        final long[] steps = {-500L, -100L, 100L, 500L};
        for (final long step : steps) {
            MaterialButton b = new MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setText(String.format(Locale.US, "%+.1f", step / 1000.0));
            b.setAllCaps(false);
            b.setInsetTop(0);
            b.setInsetBottom(0);
            b.setPadding(0, 0, 0, 0);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delay[0] += step;
                    apply.run();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(ctx, 44), 1f);
            lp.setMargins(dp(ctx, 2), 0, dp(ctx, 2), 0);
            row.addView(b, lp);
        }
        root.addView(row, new LinearLayout.LayoutParams(-1, -2));

        MaterialButton reset = new MaterialButton(ctx, null, androidx.appcompat.R.attr.borderlessButtonStyle);
        reset.setText(R.string.lyrics_timing_reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delay[0] = 0L;
                apply.run();
            }
        });
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, -2);
        rlp.gravity = Gravity.CENTER_HORIZONTAL;
        rlp.topMargin = dp(ctx, 8);
        root.addView(reset, rlp);

        new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.lyrics_timing)
                .setView(root)
                .setPositiveButton(R.string.lyrics_timing_done, null)
                .show();
    }

    private static int dp(Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}
