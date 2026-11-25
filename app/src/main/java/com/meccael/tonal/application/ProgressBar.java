package com.meccael.tonal.application;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.widget.TextView;
import com.meccael.tonal.views.ProgressView;

/**
 * Receives playback position updates from the music service and pushes them to whichever of the
 * three screens that show a progress bar (songs, player, playlist) is currently on screen.
 * Position arrives in milliseconds ("currentposition") and duration in seconds ("duration").
 */
class ProgressBar extends Handler {
    ProgressBar() {
    }

    @Override
    public void handleMessage(Message message) {
        Bundle data = message.getData();
        int tenths = data.getInt("currentposition") / 100;
        int durationSeconds = (int) data.getLong("duration");
        show(Fragment_Home_Songs.mTimeView, Fragment_Home_Songs.mDurationView, Fragment_Home_Songs.mProgressView, tenths, durationSeconds);
        show(Fragment_Player.mTimeView, Fragment_Player.mDurationView, Fragment_Player.mProgressView, tenths, durationSeconds);
        show(Fragment_Playlist.mTimeView, Fragment_Playlist.mDurationView, Fragment_Playlist.mProgressView, tenths, durationSeconds);
    }

    private static void show(TextView time, TextView duration, ProgressView bar, int tenths, int durationSeconds) {
        if (time == null || duration == null || bar == null) {
            return;
        }
        time.setText(DateUtils.formatElapsedTime(tenths / 10));
        duration.setText(DateUtils.formatElapsedTime(durationSeconds));
        bar.setProgress(tenths);
        bar.setMax(durationSeconds * 10);
    }
}
