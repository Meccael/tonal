package com.meccael.tonal.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Build;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.Log;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Notices when a USB drive or SD card (anything outside the phone's own storage) adds or loses songs
 * in the media library, and tells the app so it can reload. It only compares the number of songs that
 * live on removable storage, so ordinary library changes cost almost nothing.
 */
public class DriveWatcher {
    public interface Listener {
        /** Called on the main thread after the number of drive songs changed. */
        void onDrivesChanged(int driveSongs, boolean connected);
    }

    private static final String TAG = "TonalDrives";
    private static final long SETTLE_MS = 2500L;

    private final Context mContext;
    private final Listener mListener;
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private final ExecutorService mWorker = Executors.newSingleThreadExecutor();
    /** Drive songs in the library as last loaded by Manager; the watcher compares against this. */
    public static volatile int sLoadedDriveSongs = -1;
    private volatile int mLastCount = -1;
    private boolean mStarted;

    private final Runnable mCheck = new Runnable() {
        @Override
        public void run() {
            mWorker.execute(new Runnable() {
                @Override
                public void run() {
                    checkNow();
                }
            });
        }
    };

    private final ContentObserver mObserver = new ContentObserver(mMain) {
        @Override
        public void onChange(boolean selfChange) {
            schedule();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            schedule();
        }
    };

    public DriveWatcher(Context context, Listener listener) {
        this.mContext = context.getApplicationContext();
        this.mListener = listener;
    }

    /** True for files that sit on a USB drive or SD card rather than the phone's own storage. */
    public static boolean isDrivePath(String path) {
        if (path == null) {
            return false;
        }
        if (path.startsWith("usb://") || path.startsWith("/mnt/media_rw/")) {
            return true;
        }
        return path.startsWith("/storage/")
                && !path.startsWith("/storage/emulated/")
                && !path.startsWith("/storage/self/");
    }

    public static int countDriveSongs(Context context) {
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{"_data"}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    if (isDrivePath(cursor.getString(0))) {
                        count++;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not count drive songs", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    /** One line describing what Android reports: mounted storage volumes and which ones the media library indexes. */
    static String describeVolumes(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                Set<String> names = MediaStore.getExternalVolumeNames(context);
                sb.append("mediaStoreVolumes=").append(names).append(' ');
            }
            StorageManager manager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            if (manager != null) {
                List<StorageVolume> volumes = manager.getStorageVolumes();
                for (StorageVolume v : volumes) {
                    sb.append("[").append(v.getDescription(context))
                            .append(" state=").append(v.getState())
                            .append(" removable=").append(v.isRemovable())
                            .append(" uuid=").append(v.getUuid()).append("] ");
                }
            }
        } catch (Exception e) {
            sb.append("volumes unavailable: ").append(e);
        }
        return sb.toString();
    }

    public void start() {
        if (this.mStarted) {
            return;
        }
        this.mStarted = true;
        this.mWorker.execute(new Runnable() {
            @Override
            public void run() {
                mLastCount = countDriveSongs(mContext);
                Log.i(TAG, "Watching drives, " + mLastCount + " drive songs now.");
                Log.i(TAG, describeVolumes(mContext));
            }
        });
        this.mContext.getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, this.mObserver);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addDataScheme("file");
        try {
            this.mContext.registerReceiver(this.mReceiver, filter);
        } catch (Exception e) {
            Log.w(TAG, "Could not listen for drive broadcasts", e);
        }
    }

    /** Checks soon whether drives changed while the app was in the background. */
    public void recheck() {
        if (this.mStarted) {
            schedule();
        }
    }

    public void stop() {
        if (!this.mStarted) {
            return;
        }
        this.mStarted = false;
        this.mMain.removeCallbacks(this.mCheck);
        try {
            this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
        } catch (Exception ignored) {
        }
        try {
            this.mContext.unregisterReceiver(this.mReceiver);
        } catch (Exception ignored) {
        }
        this.mWorker.shutdown();
    }

    private void schedule() {
        Log.i(TAG, "Storage or media event, checking in " + SETTLE_MS + " ms.");
        this.mMain.removeCallbacks(this.mCheck);
        this.mMain.postDelayed(this.mCheck, SETTLE_MS);
    }

    private void checkNow() {
        final int now = countDriveSongs(this.mContext);
        Log.i(TAG, "Check: " + now + " drive songs (was " + this.mLastCount + "). " + describeVolumes(this.mContext));
        final int before = sLoadedDriveSongs >= 0 ? sLoadedDriveSongs : this.mLastCount;
        if (before < 0 || now == before) {
            this.mLastCount = now;
            return;
        }
        this.mLastCount = now;
        Log.i(TAG, "Drive songs changed: " + before + " -> " + now);
        this.mMain.post(new Runnable() {
            @Override
            public void run() {
                if (mStarted) {
                    mListener.onDrivesChanged(now, now > before);
                }
            }
        });
    }
}
