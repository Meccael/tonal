package com.meccael.tonal.application;

import android.content.Context;
import android.content.Intent;
import androidx.legacy.content.WakefulBroadcastReceiver;
import android.view.KeyEvent;
import com.meccael.tonal.R;

/* JADX INFO: compiled from: ServiceMusic.java */
/* JADX INFO: loaded from: classes.dex */
class ActionsBroadcast extends WakefulBroadcastReceiver {
    private Preferences mPreferences;
    private ServiceMusic mService;

    public ActionsBroadcast(ServiceMusic serviceMusic) {
        this.mService = serviceMusic;
        this.mPreferences = new Preferences(serviceMusic.getApplicationContext());
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(context.getString(R.string.action_play))) {
            this.mService.play(context, false);
        }
        if (action.equals(context.getString(R.string.action_pause))) {
            this.mService.pause();
            return;
        }
        if (action.equals(context.getString(R.string.action_previous))) {
            this.mService.previous(context);
            return;
        }
        if (action.equals(context.getString(R.string.action_next))) {
            this.mService.next(context);
            return;
        }
        if (action.equals(context.getString(R.string.action_kill))) {
            if (!this.mPreferences.getSettingsNotificationClose()) {
                this.mService.notificationSwiped = true;
                return;
            } else {
                this.mService.deleteNotification();
                this.mService.deleteApplication();
                return;
            }
        }
        if (action.equals("android.media.AUDIO_BECOMING_NOISY")) {
            if (this.mPreferences.getSettingsPlayerHeadset()) {
                if (this.mService.isPlaying()) {
                    this.mService.pause();
                }
                if (!(this.mService.serviceCallbacks.getRunningActivities() == 0 && this.mPreferences.getSettingsNotificationClose()) && (this.mService.serviceCallbacks.getRunningActivities() != 0 || this.mPreferences.getSettingsNotificationClose() || this.mService.notificationSwiped)) {
                    return;
                }
                this.mService.createNotification();
                return;
            }
            return;
        }
        if (action.equals("android.intent.action.MEDIA_BUTTON")) {
            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get("android.intent.extra.KEY_EVENT");
            if (keyEvent.getAction() == 0) {
                switch (keyEvent.getKeyCode()) {
                    case 85:
                        if (!this.mService.isPlaying()) {
                            this.mService.play(context, false);
                        } else {
                            this.mService.pause();
                        }
                        break;
                    case 86:
                        this.mService.deleteNotification();
                        this.mService.deleteApplication();
                        break;
                    case 87:
                        this.mService.next(context);
                        break;
                    case 88:
                        this.mService.previous(context);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY /* 126 */:
                        this.mService.play(context, false);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE /* 127 */:
                        this.mService.pause();
                        break;
                }
            }
        }
    }
}
