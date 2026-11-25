package com.meccael.tonal.application;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.session.MediaButtonReceiver;
import androidx.palette.graphics.Palette;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.meccael.tonal.R;
import com.meccael.tonal.adapters.AdapterCardRecent;
import com.meccael.tonal.adapters.AdapterMusicArt;
import com.meccael.tonal.adapters.AdapterMusicMoveable;
import com.meccael.tonal.audio.EqualizerStore;
import com.meccael.tonal.manager.Category;
import com.meccael.tonal.manager.CurrentMusic;
import com.meccael.tonal.manager.CurrentPlaylist;
import com.meccael.tonal.manager.DrivePrefs;
import com.meccael.tonal.manager.DriveWatcher;
import com.meccael.tonal.manager.Music;
import com.meccael.tonal.manager.Playlist;
import com.meccael.tonal.usb.UsbDrive;
import com.meccael.tonal.views.CoverView;

/**
 * The playback service: owns the MediaPlayer, the playing queue, the library categories, the
 * notification, the lock-screen session and the sleep timer. Screens talk to it through the
 * public fields and methods below, and it talks back through {@link CallbacksInterface}.
 */
public class ServiceMusic extends Service implements AudioManager.OnAudioFocusChangeListener {
    private static final String CHANNEL_ID = "tonal_playback";
    private static final int NOTIFICATION_ID = 2093;
    private static final int PROGRESS_PERIOD_MS = 100;
    private static final int SCREEN_PLAYER = 15;
    /** Play, pause, previous, next, stop, seek and friends: the transport actions we offer. */
    private static final long SESSION_ACTIONS = 823L;
    private static final int NOTIFICATION_COVER_SIZE = 256;
    private static final int PALETTE_SOURCE_SIZE = 92;

    // ---- State other classes read or set ----------------------------------------------------

    public MediaPlayer MusicPlayer;
    public ProgressBar handler;
    public MediaSessionCompat mMediaSessionCompat;
    public Preferences mPreferences;
    public CurrentMusic musicPlayed;
    public CurrentPlaylist playlistPlayed;
    public NotificationManager notificationManager;
    public CallbacksInterface serviceCallbacks;
    public List<Category> Categories = new ArrayList<>(6);
    public boolean threadPause = true;
    public boolean threadRun = false;
    public boolean notificationSwiped = false;
    public boolean playerLaunch = false;
    public boolean needWhiteNotification = false;
    /** True once a track has started playing this session; Home's All Music card then shows its cover. */
    public boolean trackStarted = false;

    // Read by Application_Main.
    boolean pauseOnIncomingCall = false;
    int blurScaled = PALETTE_SOURCE_SIZE;

    // ---- Private state ----------------------------------------------------------------------

    private final String TAG = "SERVICE";
    private final IBinder mBinder = new LocalBinder();
    private final List<Category> Recent = new ArrayList<>(0);
    private final ExecutorService mSaver = Executors.newSingleThreadExecutor();
    private WakefulBroadcastReceiver broadcastReceiver;
    private AudioFocusChangeListenerImpl mAudioFocusChangeListener;
    private AudioManager mAudioManager;
    private PhoneStateListener phoneListener;
    private NotificationCompat.Builder notification;
    private Timer timer;
    private boolean isForeground = false;
    private boolean audioFocusActive = false;
    private boolean wasPlayingWhenCall = false;

    private Equalizer equalizer;
    private BassBoost bassBoost;
    private LoudnessEnhancer loudness;

    // Sleep timer.
    private final Handler sleepHandler = new Handler(Looper.getMainLooper());
    private long sleepEndAt = 0L;
    private final Runnable sleepTask = new Runnable() {
        @Override
        public void run() {
            threadRun = false;
            sleepEndAt = 0L;
            deleteNotification();
            deleteApplication();
        }
    };

    // ---- Binder and background tasks --------------------------------------------------------

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        ServiceMusic getMusicInstance() {
            return ServiceMusic.this;
        }
    }

    /** Every 100 ms sends the playback position to the progress handler (only while a screen is open). */
    public class progressTask extends TimerTask {
        public progressTask() {
        }

        @Override
        public void run() {
            if (serviceCallbacks == null || runningActivities() == 0 || musicPlayed == null) {
                return;
            }
            Message message = new Message();
            Bundle data = new Bundle();
            data.putInt("currentposition", getCurrentPosition());
            data.putLong("duration", musicPlayed.getMusic(getApplicationContext()).getDuration());
            message.setData(data);
            handler.sendMessage(message);
        }
    }

    private final MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPause() {
            super.onPause();
            ServiceMusic.this.pause();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            play(getApplicationContext(), false);
        }

        @Override
        public void onSeekTo(long positionMs) {
            super.onSeekTo(positionMs);
            MusicPlayer.seekTo((int) positionMs);
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            next(getApplicationContext());
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            previous(getApplicationContext());
        }

        @Override
        public void onStop() {
            super.onStop();
            deleteNotification();
            deleteApplication();
        }
    };

    // ---- Sound effects ----------------------------------------------------------------------

    private int audioSession() {
        return MusicPlayer == null ? 0 : MusicPlayer.getAudioSessionId();
    }

    public Equalizer getEqualizer() {
        if (equalizer == null && MusicPlayer != null) {
            try {
                int session = audioSession();
                if (session != 0) {
                    equalizer = new Equalizer(0, session);
                    EqualizerStore.apply(this, equalizer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return equalizer;
    }

    public BassBoost getBassBoost() {
        if (bassBoost == null && MusicPlayer != null) {
            try {
                int session = audioSession();
                if (session != 0) {
                    bassBoost = new BassBoost(0, session);
                    if (bassBoost.getStrengthSupported()) {
                        bassBoost.setStrength((short) EqualizerStore.bass(this));
                    }
                    bassBoost.setEnabled(EqualizerStore.enabled(this));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bassBoost;
    }

    /** Acts as the preamp: raises the overall level by a gain in millibels. */
    public LoudnessEnhancer getLoudness() {
        if (loudness == null && MusicPlayer != null) {
            try {
                int session = audioSession();
                if (session != 0) {
                    loudness = new LoudnessEnhancer(session);
                    loudness.setTargetGain(EqualizerStore.preamp(this));
                    loudness.setEnabled(EqualizerStore.enabled(this));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return loudness;
    }

    /** Creates the sound effects (with the saved settings) so they apply without opening the equalizer screen. */
    public void ensureEffects() {
        getEqualizer();
        getBassBoost();
        getLoudness();
    }

    private void releaseEffects() {
        if (equalizer != null) {
            try {
                equalizer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            equalizer = null;
        }
        if (bassBoost != null) {
            try {
                bassBoost.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            bassBoost = null;
        }
        if (loudness != null) {
            try {
                loudness.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            loudness = null;
        }
    }

    // ---- Library categories -----------------------------------------------------------------

    public void addCategory(Category category) {
        Categories.add(category);
    }

    public void addRecent(Category category) {
        Recent.add(category);
    }

    public void cleanDatas() {
        Recent.clear();
        if (Categories != null) {
            Categories.clear();
        }
    }

    public void deleteLastCategory() {
        Categories.remove(Categories.size() - 1);
    }

    public int getCategoriesSize() {
        return Categories.size();
    }

    public Category getCategory(Context context, int index) {
        if (Categories == null) {
            Log.e(TAG, "Categories is null.");
            try {
                FileInputStream file = context.openFileInput("categories");
                ObjectInputStream in = new ObjectInputStream(file);
                Categories = (List<Category>) in.readObject();
                in.close();
                file.close();
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                // A cache written by an older build (before the package rename) can't be read:
                // start with an empty library and drop the file; the next scan writes a fresh one.
                Log.e(TAG, "Cant retrieve categories.", e);
                Categories = new ArrayList<>(6);
                context.deleteFile("categories");
            }
        }
        return Categories.get(index);
    }

    public Playlist getRecent() {
        return Recent.get(0).getPlaylists().get(0);
    }

    public Category getRecentPlaylist() {
        return Recent.get(0);
    }

    /** Saves the categories to the app's private "categories" file, off the main thread. */
    public void saveCategories(final Context context) {
        mSaver.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream file = context.openFileOutput("categories", Context.MODE_PRIVATE);
                    ObjectOutputStream out = new ObjectOutputStream(file);
                    out.writeObject(Categories);
                    out.close();
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ---- Small getters ----------------------------------------------------------------------

    public int getCurrentPosition() {
        return MusicPlayer.getCurrentPosition();
    }

    public Handler getProgressHandler() {
        return handler;
    }

    public int getThemeColorLight() {
        return serviceCallbacks != null ? serviceCallbacks.getThemeColorLight() : -1;
    }

    public boolean isPlaying() {
        return MusicPlayer != null && MusicPlayer.isPlaying();
    }

    public boolean isThreadPause() {
        return threadPause;
    }

    public boolean isThreadRun() {
        return threadRun;
    }

    public boolean musicLaunched() {
        return playerLaunch;
    }

    public void setCallbacks(CallbacksInterface callbacks) {
        serviceCallbacks = callbacks;
    }

    public void setAudioFocusActive(boolean active) {
        audioFocusActive = active;
    }

    /** How many app screens are open; 0 when no screen is attached. */
    private int runningActivities() {
        CallbacksInterface callbacks = serviceCallbacks;
        return callbacks == null ? 0 : callbacks.getRunningActivities();
    }

    /** Which screen is showing (15 = player); 0 when no screen is attached. */
    private int uiFragment() {
        CallbacksInterface callbacks = serviceCallbacks;
        return callbacks == null ? 0 : callbacks.getFragment();
    }

    // ---- Service lifecycle ------------------------------------------------------------------

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate.");
        mPreferences = new Preferences(getApplicationContext());
        handler = new ProgressBar();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioFocusChangeListener = new AudioFocusChangeListenerImpl(this);
        mRequestAudioFocus();

        MusicPlayer = new MediaPlayer();
        MusicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        MusicPlayer.setWakeMode(getApplicationContext(), android.os.PowerManager.PARTIAL_WAKE_LOCK);
        MusicPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                onSongPrepared(player);
            }
        });
        MusicPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer player, int what, int extra) {
                Log.e(TAG, "Player error code : " + what);
                return true;
            }
        });
        MusicPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                onSongCompleted();
            }
        });

        timer = new Timer();
        timer.scheduleAtFixedRate(new progressTask(), 0L, PROGRESS_PERIOD_MS);
        needWhiteNotification = mPreferences.getSettingsNotificationWhite(
                Utils.isFlyme4Later() || Utils.isMIUI6Later() || Utils.isHuawei());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
        Log.i(TAG, "On Create Succes.");
    }

    /** The player is ready: start it, enter the foreground and tell the screens. */
    private void onSongPrepared(MediaPlayer player) {
        player.start();
        ensureEffects();
        promoteToForeground();
        boolean noScreen = runningActivities() == 0;
        boolean keepAlive = mPreferences.getSettingsNotificationClose();
        if (isForeground || (noScreen && keepAlive) || (noScreen && !keepAlive && !notificationSwiped)) {
            createNotification();
        }
        updateLockScreenState(3, PlaybackStateCompat.STATE_PLAYING);
        refreshLockScreenPlayer();
        trackStarted = true;
        getCategory(getApplicationContext(), 0).setCover(musicPlayed.getMusic(getApplicationContext()).getCover());
        if (serviceCallbacks != null && playerLaunch) {
            serviceCallbacks.refreshPlayerVinyl(getApplicationContext());
        }
        if (playerLaunch) {
            return;
        }
        playerLaunch = true;
        if (serviceCallbacks != null && uiFragment() == SCREEN_PLAYER) {
            serviceCallbacks.refreshPlayerVinyl(getApplicationContext());
        }
        if (serviceCallbacks != null) {
            serviceCallbacks.refreshFabIcon();
        }
    }

    private void onSongCompleted() {
        Log.i(TAG, "Player has finished its current song.");
        if (mPreferences.getPlayerLoop() == 2) {
            play(getApplicationContext(), true);
            return;
        }
        CallbacksInterface callbacks = serviceCallbacks;
        if (callbacks != null && runningActivities() != 0 && uiFragment() == SCREEN_PLAYER) {
            callbacks.setPressNext(true);
        }
        next(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand.");
        if (broadcastReceiver == null) {
            broadcastReceiver = new ActionsBroadcast(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            filter.addAction(Intent.ACTION_MEDIA_BUTTON);
            filter.addAction(getString(R.string.action_play));
            filter.addAction(getString(R.string.action_pause));
            filter.addAction(getString(R.string.action_previous));
            filter.addAction(getString(R.string.action_next));
            filter.addAction(getString(R.string.action_kill));
            ContextCompat.registerReceiver(this, broadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        createLockScreenPlayer();
        if (intent != null) {
            pauseOnIncomingCall = intent.getBooleanExtra("pauseOnIncomingCall", false);
        }
        if (intent != null && pauseOnIncomingCall && phoneListener == null) {
            listenForCalls();
        }
        Log.i(TAG, "Start command succes.");
        return START_STICKY;
    }

    /** Pauses for a ringing or active call and resumes when it ends. */
    private void listenForCalls() {
        Log.i(TAG, "Setup pause system when an incoming call or a dialing call is on.");
        phoneListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String number) {
                boolean busy = state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK;
                if (busy && isPlaying()) {
                    Log.i("CALL STATE LISTENER", "A call is detected, pause music.");
                    wasPlayingWhenCall = true;
                    pause(true);
                    if (Fragment_Player.mTimeView != null && CoverView.mStartAnimator != null && CoverView.mStartAnimator.isRunning()) {
                        CoverView.mStartAnimator.pause();
                    }
                }
                if (state == TelephonyManager.CALL_STATE_IDLE && wasPlayingWhenCall) {
                    Log.i("CALL STATE LISTENER", "Hang up.");
                    wasPlayingWhenCall = false;
                    play(getApplicationContext(), false);
                }
                super.onCallStateChanged(state, number);
            }
        };
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony != null) {
            telephony.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        deleteNotification();
        deleteApplication();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseEffects();
        mSaver.shutdown();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("finish"));
        Log.i(TAG, "Service killed by user in onDestroy.");
    }

    /** Shuts everything down: foreground, timers, player, receivers, session. */
    public void deleteApplication() {
        stopForegroundNow();
        sleepHandler.removeCallbacks(sleepTask);
        updateLockScreenState(1, PlaybackStateCompat.STATE_STOPPED);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (MusicPlayer != null) {
            MusicPlayer.stop();
            MusicPlayer.release();
            MusicPlayer = null;
        }
        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        try {
            if (broadcastReceiver != null) {
                unregisterReceiver(broadcastReceiver);
            }
        } catch (IllegalArgumentException ignored) {
        }
        broadcastReceiver = null;
        if (phoneListener != null) {
            try {
                TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                if (telephony != null) {
                    telephony.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
                }
            } catch (Exception ignored) {
            }
            phoneListener = null;
        }
        if (mMediaSessionCompat != null) {
            try {
                mMediaSessionCompat.setActive(false);
                mMediaSessionCompat.release();
            } catch (Exception ignored) {
            }
            mMediaSessionCompat = null;
        }
        handler.removeCallbacksAndMessages(null);
        stopSelf();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("finish"));
        Log.i(TAG, "Service stopped, good bye user :)");
    }

    // ---- Audio focus ------------------------------------------------------------------------

    @Override
    public void onAudioFocusChange(int change) {
    }

    public void mRequestAudioFocus() {
        if (audioFocusActive) {
            return;
        }
        if (mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.i(TAG, "Audio focus granted :)");
            audioFocusActive = true;
        } else {
            Log.e(TAG, "Cannot obtain an audio focus !");
            audioFocusActive = false;
        }
    }

    // ---- Foreground state -------------------------------------------------------------------

    /** Makes the service a foreground media service so Android keeps it alive in the background. */
    private void promoteToForeground() {
        if (isForeground) {
            return;
        }
        try {
            Music song = musicPlayed.getMusic(getApplicationContext());
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            builder.setSmallIcon(R.mipmap.icon_notification_pause)
                    .setContentTitle(song.getTitle())
                    .setContentText(song.getArtist())
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0,
                            new Intent(this, Application_Main.class), PendingIntent.FLAG_IMMUTABLE));
            ServiceCompat.startForeground(this, NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            isForeground = true;
            createNotification();
        } catch (Exception e) {
            // Android refused (for example, the app is in the background); keep playing as a plain service.
            Log.w(TAG, "Could not enter foreground: " + e);
        }
    }

    /** Lets the service leave the foreground; the notification stays so it can be resumed or swiped away. */
    private void demoteFromForeground() {
        if (isForeground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
            isForeground = false;
        }
    }

    private void stopForegroundNow() {
        if (isForeground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
            isForeground = false;
        }
    }

    public void deleteNotification() {
        if (isForeground && isPlaying()) {
            // Android keeps the notification of a playing media service on screen.
            return;
        }
        notificationManager.cancel(NOTIFICATION_ID);
    }

    // ---- Notification -----------------------------------------------------------------------

    private PendingIntent actionIntent(int actionRes, int flags) {
        Intent intent = new Intent();
        intent.setAction(getString(actionRes));
        intent.setPackage(getPackageName());
        return PendingIntent.getBroadcast(this, 0, intent, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    public void createNotification() {
        Application_Extend.getInstance().trackScreenView("Notification");
        boolean colored = mPreferences.getSettingsNotificationColored(!needWhiteNotification);
        final RemoteViews small;
        final RemoteViews big;
        if (colored || !needWhiteNotification) {
            small = new RemoteViews(getPackageName(), R.layout.notification_normal_dark);
            big = new RemoteViews(getPackageName(), R.layout.notification_large_dark);
        } else {
            small = new RemoteViews(getPackageName(), R.layout.notification_normal_light);
            big = new RemoteViews(getPackageName(), R.layout.notification_large_light);
        }

        Bitmap cover = notificationCover();
        Music song = musicPlayed.getMusic(getApplicationContext());
        small.setTextViewText(R.id.notification_small_title, song.getTitle());
        small.setTextViewText(R.id.notification_small_artist, song.getArtist());
        small.setImageViewBitmap(R.id.notification_small_cover, Utils.getRoundedCornerBitmap(cover.copy(cover.getConfig(), true), 16));
        big.setImageViewBitmap(R.id.notification_big_cover, cover);
        big.setTextViewText(R.id.notification_big_title, song.getTitle());
        big.setTextViewText(R.id.notification_big_artist, song.getArtist());
        big.setTextViewText(R.id.notification_big_duration, DateUtils.formatElapsedTime((int) song.getDuration()));

        Intent open = new Intent(this, Application_Main.class);
        open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, open, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent kill = actionIntent(R.string.action_kill, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent play = actionIntent(R.string.action_play, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pause = actionIntent(R.string.action_pause, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent previous = actionIntent(R.string.action_previous, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent next = actionIntent(R.string.action_next, PendingIntent.FLAG_UPDATE_CURRENT);
        for (RemoteViews views : new RemoteViews[]{small, big}) {
            views.setOnClickPendingIntent(R.id.btnPlay, play);
            views.setOnClickPendingIntent(R.id.btnPause, pause);
            views.setOnClickPendingIntent(R.id.btnNext, next);
            views.setOnClickPendingIntent(R.id.btnPrevious, previous);
        }

        notification = new NotificationCompat.Builder(this, CHANNEL_ID);
        boolean playing = isPlaying();
        for (RemoteViews views : new RemoteViews[]{small, big}) {
            views.setViewVisibility(R.id.btnPause, playing ? android.view.View.VISIBLE : android.view.View.GONE);
            views.setViewVisibility(R.id.btnPlay, playing ? android.view.View.GONE : android.view.View.VISIBLE);
        }
        if (playing) {
            notification.setSmallIcon(R.mipmap.icon_notification_pause);
            notification.setOngoing(!mPreferences.getSettingsNotificationSwipeable());
        } else {
            notification.setSmallIcon(R.mipmap.icon_notification_play);
            notification.setOngoing(false);
        }
        notification.setDeleteIntent(kill)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setTicker(song.getTitle() + " - " + song.getArtist())
                .setCustomContentView(small);

        // The cover's dominant light colour tints the notification, then it is posted.
        new Palette.Builder(Bitmap.createScaledBitmap(themeArt(), blurScaled, blurScaled, false))
                .generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int tint = palette.getLightMutedColor(palette.getLightVibrantColor(-1));
                        boolean tinted = mPreferences.getSettingsNotificationColored(!needWhiteNotification);
                        if (mPreferences.getSettingsNotificationExpandable()) {
                            if (tinted) {
                                big.setInt(R.id.notification_big, "setBackgroundColor", tint);
                            }
                            notification.setCustomBigContentView(big);
                        }
                        if (tinted) {
                            small.setInt(R.id.notification_small, "setBackgroundColor", tint);
                        }
                        notificationManager.notify(NOTIFICATION_ID, notification.build());
                    }
                });
    }

    /** The rounded cover for the notification, falling back to the default cover. */
    private Bitmap notificationCover() {
        Bitmap art = themeArt();
        if (art != null) {
            try {
                return Utils.getRoundedCornerBitmap(Bitmap.createScaledBitmap(art, NOTIFICATION_COVER_SIZE, NOTIFICATION_COVER_SIZE, false), 8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Bitmap fallback = BitmapFactory.decodeResource(getResources(), R.drawable.cover0);
        if (fallback != null) {
            return Utils.getRoundedCornerBitmap(Bitmap.createScaledBitmap(fallback, NOTIFICATION_COVER_SIZE, NOTIFICATION_COVER_SIZE, false), 8);
        }
        return Bitmap.createBitmap(NOTIFICATION_COVER_SIZE, NOTIFICATION_COVER_SIZE, Bitmap.Config.ARGB_8888);
    }

    /** The current cover for notifications and the lock screen; works with no screen attached. */
    private Bitmap themeArt() {
        Bitmap art = null;
        try {
            // Always the song that is playing now, so the notification never shows the previous cover.
            long cover = musicPlayed.getMusic(getApplicationContext()).getCover();
            art = Utils.getCoverBitamp(getApplicationContext(), cover, R.drawable.cover0, 400);
        } catch (Exception ignored) {
        }
        if (art == null) {
            CallbacksInterface callbacks = serviceCallbacks;
            art = callbacks == null ? null : callbacks.getThemeArt();
        }
        if (art == null) {
            art = BitmapFactory.decodeResource(getResources(), R.drawable.cover0);
        }
        return art;
    }

    // ---- Lock screen ------------------------------------------------------------------------

    public void createLockScreenPlayer() {
        if (mMediaSessionCompat != null) {
            return;
        }
        mMediaSessionCompat = new MediaSessionCompat(getApplication(), getString(R.string.app_name),
                new ComponentName(getPackageName(), MediaButtonReceiver.class.getName()), null);
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSessionCompat.setCallback(mMediaSessionCallback);
        mMediaSessionCompat.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(SESSION_ACTIONS).setState(PlaybackStateCompat.STATE_PLAYING, 0L, 0.0f).build());
        mMediaSessionCompat.setActive(true);
    }

    public void refreshLockScreenPlayer() {
        if (mMediaSessionCompat == null) {
            createLockScreenPlayer();
            return;
        }
        Context context = getApplicationContext();
        Music song = musicPlayed.getMusic(context);
        MediaMetadataCompat.Builder metadata = new MediaMetadataCompat.Builder();
        metadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist());
        metadata.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getAlbum());
        metadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getTitle());
        metadata.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getDuration());
        if (mPreferences.getSettingsLockscreenBackground()) {
            Bitmap art = mPreferences.getSettingsLockscreenBlur()
                    ? Utils.blur(themeArt(), mPreferences.getSettingsUIBlurIntensity()) : themeArt();
            metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art);
        }
        mMediaSessionCompat.setMetadata(metadata.build());
    }

    /** The first argument is the legacy remote-control state and is no longer used. */
    public void updateLockScreenState(int legacyState, int sessionState) {
        if (mMediaSessionCompat != null) {
            mMediaSessionCompat.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(sessionState, 0L, 0.0f).setActions(SESSION_ACTIONS).build());
        }
    }

    // ---- Playback ---------------------------------------------------------------------------

    public int generateRandom(long size) {
        int spread = (int) Math.min(13L, size);
        int count = (int) size;
        int first = new Random().nextInt(count);
        int pull = new Random().nextInt(count / spread);
        // A second, smaller draw nudges the pick towards earlier songs in the queue.
        return first > pull ? first - pull : first;
    }

    public void next(Context context) {
        Log.i(TAG, "next");
        int loop = mPreferences.getPlayerLoop();
        if (loop == 0) {
            musicPlayed.setPosition(generateRandom(playlistPlayed.getPlaylistSize()));
            play(context, true);
        } else if (loop == 1) {
            if (musicPlayed.getPosition() < playlistPlayed.getPlaylistSize() - 1) {
                musicPlayed.setPosition(musicPlayed.getPosition() + 1);
            } else {
                musicPlayed.setPosition(0);
            }
            play(context, true);
        } else if (loop == 2) {
            MusicPlayer.seekTo(0);
        }
    }

    public void previous(Context context) {
        if (MusicPlayer.getCurrentPosition() >= 2500) {
            // Past the first couple of seconds, "previous" restarts the song.
            if (serviceCallbacks != null) {
                serviceCallbacks.setPressNext(true);
            }
            MusicPlayer.seekTo(0);
            if (CoverView.mStartAnimator != null && CoverView.mStartAnimator.isStarted()) {
                CoverView.mStartAnimator.cancel();
            }
            return;
        }
        int loop = mPreferences.getPlayerLoop();
        if (loop == 0) {
            musicPlayed.setPosition(generateRandom(playlistPlayed.getPlaylistSize()));
        } else if (loop == 1 && musicPlayed.getPosition() > 0) {
            if (musicPlayed.getPosition() > playlistPlayed.getPlaylistSize() - 1) {
                musicPlayed.setPosition(0);
            } else {
                musicPlayed.setPosition(musicPlayed.getPosition() - 1);
            }
        }
        play(context, true);
    }

    public void pause() {
        pause(false);
    }

    /** keepForeground: a call or other interruption that will resume by itself must keep the foreground service. */
    public void pause(boolean keepForeground) {
        if (!isPlaying()) {
            return;
        }
        MusicPlayer.pause();
        Log.i(TAG, "Pause.");
        if (!keepForeground) {
            demoteFromForeground();
        }
        // Refresh the still-visible notification so it shows the play button.
        createNotification();
        refreshPlayingRows();
        if (uiFragment() == SCREEN_PLAYER) {
            if (CoverView.mStartAnimator != null && CoverView.mStartAnimator.isRunning()) {
                CoverView.mStartAnimator.pause();
            }
        }
        updateLockScreenState(2, PlaybackStateCompat.STATE_PAUSED);
        if (serviceCallbacks != null) {
            serviceCallbacks.refreshFabIcon();
        }
    }

    /** Redraws the playing song's row in the Songs list and the queue (its play/pause marker). */
    private void refreshPlayingRows() {
        int position = musicPlayed.getPosition();
        if (Fragment_Home_Songs.mTimeView != null && Fragment_Home_Songs.mItemsAdapter != null) {
            Fragment_Home_Songs.mItemsAdapter.notifyItemChanged(position);
        }
        if (Fragment_Playlist.mTimeView != null && Fragment_Playlist.mItemsAdapter != null) {
            Fragment_Playlist.mItemsAdapter.notifyItemChanged(position);
        }
    }

    /**
     * Plays. With newSong false and a song already loaded it just resumes; otherwise it starts the
     * song at the current queue position.
     */
    public void play(Context context, boolean newSong) {
        // Android 17 only allows audio and audio focus from a foreground media service, so enter it first.
        promoteToForeground();
        mRequestAudioFocus();

        if (!newSong && playerLaunch) {
            resume();
            return;
        }
        Log.i(TAG, "Play song.");
        if (musicPlayed.getPosition() >= playlistPlayed.getPlaylist().size()) {
            musicPlayed.setPosition(0);
        }
        int position = musicPlayed.getPosition();
        musicPlayed.newSong(context, playlistPlayed.getSong(position).getMusic(), position, musicPlayed.getPlaylistID());
        if (serviceCallbacks != null) {
            serviceCallbacks.setThemeArt(Utils.getCoverBitamp(getApplicationContext(),
                    musicPlayed.getMusic(getApplicationContext()).getCover(), R.drawable.cover0, serviceCallbacks.getCoverScaled()));
        }
        moveSelectionMarkers(position);
        startPlayer(context);
    }

    /** Continues the song that was paused. */
    private void resume() {
        Log.i(TAG, "Continue playing.");
        MusicPlayer.start();
        promoteToForeground();
        if (runningActivities() == 0 || isForeground) {
            createNotification();
        }
        refreshPlayingRows();
        if (CoverView.mStartAnimator != null && CoverView.mStartAnimator.isRunning() && uiFragment() == SCREEN_PLAYER) {
            CoverView.mStartAnimator.resume();
        } else if (uiFragment() == SCREEN_PLAYER && CoverView.mStartAnimator != null) {
            CoverView.mStartAnimator.start();
        }
        if (serviceCallbacks != null) {
            serviceCallbacks.refreshFabIcon();
        }
        updateLockScreenState(3, PlaybackStateCompat.STATE_PLAYING);
    }

    /** A new song started: move the "now playing" marker in each list that is showing. */
    private void moveSelectionMarkers(int position) {
        if (Fragment_Home_Songs.mTimeView != null && Fragment_Home_Songs.mItemsAdapter != null) {
            Fragment_Home_Songs.mItemsAdapter.notifyItemChanged(AdapterMusicArt.lastSelectedPosition);
            AdapterMusicArt.lastSelectedPosition = position;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Fragment_Home_Songs.mItemsAdapter != null) {
                        Fragment_Home_Songs.mItemsAdapter.notifyItemChanged(AdapterMusicArt.lastSelectedPosition);
                    }
                }
            }, 500L);
        }
        if (Fragment_Playlist.mItemsAdapter != null) {
            Fragment_Playlist.mItemsAdapter.notifyItemChanged(AdapterMusicMoveable.lastSelectedPosition);
            AdapterMusicMoveable.lastSelectedPosition = position;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Fragment_Playlist.mItemsAdapter != null) {
                        Fragment_Playlist.mItemsAdapter.notifyItemChanged(AdapterMusicMoveable.lastSelectedPosition);
                    }
                }
            }, 500L);
        }
        if (Fragment_Home_Carousel.mRecentsAdapter != null) {
            Fragment_Home_Carousel.mRecentsAdapter.notifyItemChanged(AdapterCardRecent.lastSelectedPosition);
            AdapterCardRecent.lastSelectedPosition = position + 1;
            Fragment_Home_Carousel.mRecentsAdapter.notifyItemChanged(AdapterCardRecent.lastSelectedPosition);
        }
    }

    /** Points the player at the current song (a file, or a track read off the USB drive) and prepares it. */
    private void startPlayer(Context context) {
        try {
            MusicPlayer.reset();
            String path = musicPlayed.getMusic(getApplicationContext()).getFile();
            if (path != null && path.startsWith(UsbDrive.PREFIX)) {
                MediaDataSource source = UsbDrive.get(getApplicationContext()).openSource(path);
                if (source == null) {
                    Log.w("TonalUsb", "The drive is not available for " + path);
                    return;
                }
                MusicPlayer.setDataSource(source);
            } else {
                MusicPlayer.setDataSource(path);
            }
            MusicPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            next(context);
        }
    }

    /** If the song playing came from a drive that has been unplugged, stop quietly instead of erroring. */
    public void pauseIfDriveGone() {
        try {
            if (musicPlayed == null || MusicPlayer == null) {
                return;
            }
            if (!DrivePrefs.pauseOnUnplug(getApplicationContext())) {
                return;
            }
            String path = musicPlayed.getMusic(getApplicationContext()).getFile();
            if (path == null) {
                return;
            }
            boolean gone;
            if (path.startsWith(UsbDrive.PREFIX)) {
                gone = !UsbDrive.get(getApplicationContext()).isConnected();
            } else {
                gone = DriveWatcher.isDrivePath(path) && !new File(path).exists();
            }
            if (gone) {
                pause();
            }
        } catch (Exception e) {
            Log.w("TonalUsb", "pauseIfDriveGone: " + e);
        }
    }

    // ---- Sleep timer ------------------------------------------------------------------------

    public void startSleepTimer(int minutes) {
        cancelSleepTimer();
        if (minutes <= 0) {
            return;
        }
        long ms = minutes * 60000L;
        sleepEndAt = SystemClock.uptimeMillis() + ms;
        threadPause = false;
        threadRun = true;
        sleepHandler.postDelayed(sleepTask, ms);
    }

    public void cancelSleepTimer() {
        sleepHandler.removeCallbacks(sleepTask);
        sleepEndAt = 0L;
        threadRun = false;
    }

    public long getSleepRemainingMs() {
        return sleepEndAt == 0L ? 0L : Math.max(0L, sleepEndAt - SystemClock.uptimeMillis());
    }

    public void setThreadPause(boolean paused) {
        threadPause = paused;
        if (paused) {
            cancelSleepTimer();
        }
    }

    public void setThreadRun(boolean run) {
        if (run) {
            startSleepTimer(mPreferences.getSettingsPlayerStopMusic());
        } else {
            cancelSleepTimer();
        }
    }
}
