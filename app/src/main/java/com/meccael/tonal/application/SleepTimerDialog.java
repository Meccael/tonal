package com.meccael.tonal.application;

import android.content.Context;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import com.meccael.tonal.R;

/** Quick sleep-timer picker for the player. Uses the same timer as Settings, so the two always agree. */
public final class SleepTimerDialog {
    private static final int[] MINUTES = {15, 30, 45, 60, 90};

    private SleepTimerDialog() {}

    public static void show(final Context ctx, final ServiceMusic service, final Preferences prefs) {
        final boolean active = service.isThreadRun();
        List<String> labels = new ArrayList<>();
        for (int m : MINUTES) labels.add(ctx.getString(R.string.sleep_option, m));
        if (active) labels.add(ctx.getString(R.string.sleep_off));
        String title = ctx.getString(R.string.sleep_timer);
        if (active) {
            int left = (int) ((service.getSleepRemainingMs() + 59999L) / 60000L);
            title = ctx.getString(R.string.sleep_title_left, Math.max(1, left));
        }
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    if (which < MINUTES.length) {
                        int m = MINUTES[which];
                        prefs.setSettingsPlayerStopMusic(m);
                        service.startSleepTimer(m);
                        Toast.makeText(ctx, ctx.getString(R.string.sleep_set, m), Toast.LENGTH_SHORT).show();
                    } else {
                        service.setThreadPause(true);
                        Toast.makeText(ctx, R.string.sleep_cancelled, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}
