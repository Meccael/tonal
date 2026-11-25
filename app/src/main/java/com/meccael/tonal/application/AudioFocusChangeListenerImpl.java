package com.meccael.tonal.application;

import android.media.AudioManager;
import android.util.Log;

/* JADX INFO: compiled from: ServiceMusic.java */
/* JADX INFO: loaded from: classes.dex */
class AudioFocusChangeListenerImpl implements AudioManager.OnAudioFocusChangeListener {
    private ServiceMusic mService;

    AudioFocusChangeListenerImpl(ServiceMusic serviceMusic) {
        this.mService = serviceMusic;
    }

    @Override // android.media.AudioManager.OnAudioFocusChangeListener
    public void onAudioFocusChange(int i) {
        Log.i("TONAL AUDIO", "Focus changed");
        switch (i) {
            case -3:
                Log.i("TONAL AUDIO", "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                break;
            case -2:
                Log.i("TONAL AUDIO", "AUDIOFOCUS_LOSS_TRANSIENT");
                break;
            case -1:
                Log.i("TONAL AUDIO", "AUDIOFOCUS_LOSS");
                this.mService.pause();
                this.mService.setAudioFocusActive(false);
                break;
            case 1:
                Log.i("TONAL AUDIO", "AUDIOFOCUS_GAIN");
                break;
        }
    }
}
