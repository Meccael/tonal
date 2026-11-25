package com.meccael.tonal.application;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.meccael.tonal.R;
import com.meccael.tonal.artwork.ArtworkWorker;
import com.meccael.tonal.manager.Category;
import com.meccael.tonal.manager.CurrentMusic;
import com.meccael.tonal.manager.CurrentPlaylist;
import com.meccael.tonal.manager.DrivePrefs;
import com.meccael.tonal.manager.DriveWatcher;
import com.meccael.tonal.manager.Manager;
import com.meccael.tonal.manager.Music;
import com.meccael.tonal.navigation.NavigationDrawer;
import com.meccael.tonal.usb.UsbDrive;
import com.meccael.tonal.views.CoverView;
import com.meccael.tonal.views.PlayPauseButton;

/**
 * The one activity. It hosts the screens (fragments), binds to the playback service, builds the
 * library on start-up, paints the blurred cover background and keeps the toolbar and search bar.
 */
public class Application_Main extends AppCompatActivity implements CallbacksInterface, NavigationDrawer.FragmentDrawerListener {
    // Which screen is showing (read all over the app).
    public static int fragmentID = 0;
    public static boolean pressNextPrevious = false;
    public static int runningActivities = 0;
    public static boolean searchBarVisible = false;
    public static int transitionID = 0;

    // Screen ids.
    private static final int SCREEN_HOME = 10;
    private static final int SCREEN_PLAYLISTS = 11;
    private static final int SCREEN_SONGS = 12;
    private static final int SCREEN_PLAYER = 15;
    private static final int SCREEN_QUEUE = 20;
    private static final int SCREEN_ABOUT = 30;
    private static final int SCREEN_EQUALIZER = 40;
    private static final int SCREEN_SETTINGS = 50;

    private static final int PERMISSION_REQUEST = 340;
    private static final int BLUR_MAX = 25;

    public ServiceMusic mServiceMusic = null;

    private final String TAG = "TONAL";
    private NavigationDrawer drawerFragment;
    private DrawerLayout mDrawer;
    private ImageView mCoverBackView;
    private ImageButton mSearch;
    private EditText mSearchBar;
    private TextView mTitle;
    private TextView mSubTitle;
    private Preferences mPreferences;
    private Vibrator mVibrate;
    private Bundle savedInstanceState;
    private DriveWatcher mDriveWatcher;

    private Bitmap themeArt;
    private Bitmap themeThumbnail;
    private int themeColorVibrant = -1;
    private int themeColorLight = -1;
    private int themeColorPlay = -1;
    private final int blurScaled = 92;
    private int coverScaled = 640;
    private int cardPosition = 0;
    private int cardItem = 0;

    private boolean mServiceRunning = false;
    private boolean pauseOnIncomingCall = true;
    private long startTime = 0;
    private long startTimeService = 0;
    private boolean updaterSectionIsOpen = false;
    private boolean settingSectionIsOpen = false;
    private boolean settingSubSectionIsOpen = false;

    /** A song another app asked us to open; consumed once the library is ready. */
    private File songAsked = null;
    private boolean pendingOpenWith = false;

    // ---- Receivers and service connection ---------------------------------------------------

    private final BroadcastReceiver finishBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Finish Broadcast.");
            Application_Extend.getInstance().trackEvent("Application", "Activity", "Close");
            setServiceKill();
            finishAffinity();
        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.i(TAG, "Service successfully started in " + (System.currentTimeMillis() - startTimeService) + "ms.");
            mServiceMusic = ((ServiceMusic.LocalBinder) binder).getMusicInstance();
            mServiceMusic.setCallbacks(Application_Main.this);
            if (themeArt == null && mServiceMusic.musicPlayed != null && mServiceMusic.musicPlayed.getMusic(Application_Main.this) != null) {
                setThemeArt(Utils.getCoverBitamp(Application_Main.this,
                        mServiceMusic.musicPlayed.getMusic(Application_Main.this).getCover(), R.drawable.cover0, getCoverScaled()));
            }
            if (mServiceRunning) {
                return;
            }
            mServiceRunning = true;
            reattachScreen();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service has been stopped.");
            setServiceKill();
        }
    };

    /** The service is (back) up: fill in whatever screen is already showing, or start the library. */
    private void reattachScreen() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.container_body);
        int screen = getFragment();
        if (screen != 0 && current == null) {
            // A previous screen number is left over but this window has no screen yet: start fresh.
            startManager();
            return;
        }
        switch (screen) {
            case SCREEN_HOME:
                ((Fragment_Home_Carousel) current).loadDatas();
                break;
            case SCREEN_PLAYLISTS:
                ((Fragment_Home_Playlists) current).loadDatas();
                break;
            case SCREEN_SONGS:
                ((Fragment_Home_Songs) current).loadDatas();
                break;
            case SCREEN_PLAYER:
                ((Fragment_Player) current).loadDatas();
                break;
            case SCREEN_ABOUT:
                break;
            case SCREEN_EQUALIZER:
                // The equalizer screen was rebuilt before the service was back: fill it in; do not
                // reload the library (that restarts the song).
                if (current instanceof Fragment_RegulateAudio) {
                    ((Fragment_RegulateAudio) current).refreshForService();
                }
                break;
            case SCREEN_QUEUE:
                ((Fragment_Playlist) current).loadDatas();
                break;
            case SCREEN_SETTINGS:
                ((Fragment_Settings) current).loadDatas();
                break;
            default:
                startManager();
                break;
        }
    }

    public void setServiceKill() {
        mServiceMusic = null;
        mServiceRunning = false;
        fragmentID = 0;
    }

    // ---- Activity lifecycle -----------------------------------------------------------------

    @Override
    public void onCreate(Bundle bundle) {
        final SplashScreen splash = SplashScreen.installSplashScreen(this);
        splash.setKeepOnScreenCondition(new SplashScreen.KeepOnScreenCondition() {
            @Override
            public boolean shouldKeepOnScreen() {
                return SystemClock.uptimeMillis() - Process.getStartUptimeMillis() < 1000L;
            }
        });
        super.onCreate(bundle);
        ArtworkWorker.maybeAutoRun(this);
        savedInstanceState = bundle;
        if (bundle == null) {
            fragmentID = 0;
        }
        // Android 16 no longer calls onBackPressed() by itself, so route the system back gesture to it.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressed();
            }
        });
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            songAsked = OpenWith.resolve(this, getIntent());
            if (songAsked == null) {
                Log.e(TAG, "Couldn't open the file asked.");
            }
        }
        startTime = System.currentTimeMillis();
        setContentView(R.layout.application);
        setTaskDescription(new ActivityManager.TaskDescription(null,
                BitmapFactory.decodeResource(getResources(), R.mipmap.icon_application),
                ContextCompat.getColor(getApplicationContext(), R.color.colorWhite)));
        Application_Extend.getInstance().trackEvent("Application", "Activity", "Open");
        Log.i(TAG, "Launch Tonal on " + System.getProperty("os.arch") + ".");
        coverScaled = (int) (getScreenWidth() / 1.8f);
        LocalBroadcastManager.getInstance(this).registerReceiver(finishBroadcast, new IntentFilter("finish"));
        setupUsbDrive();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mTitle = (TextView) findViewById(R.id.title_title);
        mSubTitle = (TextView) findViewById(R.id.title_artist);
        mSearch = (ImageButton) findViewById(R.id.toolbar_search);
        mSearchBar = (EditText) findViewById(R.id.searchbar);
        mCoverBackView = (ImageView) findViewById(R.id.background);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        ((ViewGroup.MarginLayoutParams) toolbar.getLayoutParams()).topMargin = getStatusBarHeight(this);
        mDrawer.setElevation(0.0f);
        mDrawer.setDrawerShadow(new ColorDrawable(0), GravityCompat.START);
        mDrawer.setScrimColor(0);
        drawerFragment = (NavigationDrawer) getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, mDrawer, toolbar);
        drawerFragment.setDrawerListener(this);

        mPreferences = new Preferences(getApplicationContext());
        mVibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (mPreferences.getSettingsUIBlurIntensity() > BLUR_MAX) {
            mPreferences.setSettingsUIBlurIntensity(BLUR_MAX);
        }
        if (mPreferences.getSettingsUIScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        checkAndRequestPermissions();
        setupSearchControls();
    }

    private void setupUsbDrive() {
        UsbDrive.get(this).register();
        if (getIntent() != null && "android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(getIntent().getAction())) {
            UsbDrive.get(this).connect();
        }
        UsbDrive.get(this).setListener(new UsbDrive.Listener() {
            @Override
            public void onChanged() {
                if (mServiceMusic != null) {
                    mServiceMusic.pauseIfDriveGone();
                }
                reloadLibraryForDrives();
            }
        });
    }

    private void setupSearchControls() {
        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Some screens borrow this button (Settings uses it as a jump menu).
                Object override = view.getTag(R.id.toolbar_search);
                if (override instanceof Runnable) {
                    ((Runnable) override).run();
                    return;
                }
                buzz();
                showSearchBar(true);
            }
        });
        mSearchBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // A tap on the clear icon at the right end empties the search.
                int clearZone = (mSearchBar.getRight() - mSearchBar.getCompoundDrawables()[2].getBounds().width()) - convertDpToPx(36);
                if (event.getAction() != MotionEvent.ACTION_UP || event.getRawX() < clearZone) {
                    return false;
                }
                buzz();
                resetSearch();
                return true;
            }
        });
        mSearchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId != 3) {    // IME_ACTION_SEARCH
                    return false;
                }
                applySearchFilter(mSearchBar.getText().toString().trim());
                hideKeyboard(getApplicationContext());
                return true;
            }
        });
    }

    private void buzz() {
        if (mPreferences.getSettingsUIVibration()) {
            mVibrate.vibrate(5L);
        }
    }

    /** Filters the list on screen (Songs or the queue) by the search text. */
    private void applySearchFilter(String text) {
        if (fragmentID == SCREEN_SONGS) {
            if (Fragment_Home_Songs.mItemsAdapter != null) {
                Fragment_Home_Songs.mItemsAdapter.getFilter(text);
            }
        } else if (fragmentID == SCREEN_QUEUE) {
            if (Fragment_Playlist.mItemsAdapter != null) {
                Fragment_Playlist.mItemsAdapter.getFilter(text);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart.");
        if (runningActivities == 0 && mServiceRunning && mServiceMusic.musicLaunched()) {
            mServiceMusic.deleteNotification();
            mServiceMusic.notificationSwiped = false;
        }
        runningActivities++;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pendingOpenWith && mServiceMusic != null) {
            pendingOpenWith = false;
            startManager();
        }
        if (mServiceMusic != null) {
            Log.i("TonalDrives", "Resume: drive watcher " + (mDriveWatcher == null ? "starting" : "rechecking"));
            if (mDriveWatcher == null) {
                startDriveWatcher();
            } else {
                mDriveWatcher.recheck();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop.");
        runningActivities--;
        if (runningActivities == 0 && mServiceMusic != null && mServiceMusic.musicLaunched()) {
            mServiceMusic.createNotification();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UsbDrive.get(this).setListener(null);
        UsbDrive.get(this).unregister();
        if (mDriveWatcher != null) {
            mDriveWatcher.stop();
            mDriveWatcher = null;
        }
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(finishBroadcast);
            if (mServiceMusic != null && mServiceMusic.serviceCallbacks == this) {
                mServiceMusic.setCallbacks(null);
            }
            unbindService(mConnection);
        } catch (Exception ignored) {
        }
        setServiceKill();
        Log.i(TAG, "onDestroy.");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null) {
            return;
        }
        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(intent.getAction())) {
            UsbDrive.get(this).connect();
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // Another app asked for a song while Tonal is already running.
            setIntent(intent);
            File asked = OpenWith.resolve(this, intent);
            Log.i("TonalOpenWith", "onNewIntent " + intent.getData() + " -> " + asked);
            if (asked == null) {
                Toast.makeText(this, R.string.open_with_failed, Toast.LENGTH_LONG).show();
            } else {
                songAsked = asked;
                // If the service isn't up yet the normal start-up picks the song up; otherwise
                // wait for onResume, when fragments can be changed safely.
                pendingOpenWith = mServiceMusic != null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerFragment.getDrawerOpened()) {
            drawerFragment.closeDrawer();
            return;
        }
        if (searchBarVisible) {
            resetSearch();
            hideSearchBar(true);
            return;
        }
        if (settingSubSectionIsOpen || settingSectionIsOpen) {
            ((Fragment_Settings) getSupportFragmentManager().findFragmentById(R.id.container_body)).closeSection(getApplicationContext());
            return;
        }
        if (updaterSectionIsOpen) {
            ((Fragment_ReadyForBeta) getSupportFragmentManager().findFragmentById(R.id.container_body)).closeSection(getApplicationContext());
            return;
        }
        switch (fragmentID) {
            case SCREEN_PLAYLISTS:
            case SCREEN_SONGS:
                getSupportFragmentManager().popBackStack();
                break;
            case SCREEN_PLAYER:
                closePlayer();
                break;
            case SCREEN_QUEUE:
            case SCREEN_ABOUT:
            case SCREEN_EQUALIZER:
            case SCREEN_SETTINGS:
                openHome();
                break;
            default:
                moveTaskToBack(true);
                break;
        }
    }

    // ---- Permissions ------------------------------------------------------------------------

    /** The one permission that lets the app find music on this Android version. */
    private String storagePermission() {
        // Apps targeting Android 13+ use the audio permission; older targets are still served by the classic one.
        boolean newModel = Build.VERSION.SDK_INT >= 33 && getApplicationInfo().targetSdkVersion >= 33;
        return newModel ? "android.permission.READ_MEDIA_AUDIO" : "android.permission.READ_EXTERNAL_STORAGE";
    }

    public void checkAndRequestPermissions() {
        ArrayList<String> missing = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, storagePermission()) != 0) {
            Log.i("TONAL PERMISSIONS", "Music access missing.");
            missing.add(storagePermission());
        }
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_PHONE_STATE") != 0) {
            Log.i("TONAL PERMISSIONS", "Phone state missing (optional).");
            missing.add("android.permission.READ_PHONE_STATE");
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != 0) {
            missing.add("android.permission.POST_NOTIFICATIONS");
        }
        if (missing.isEmpty()) {
            Log.i("TONAL PERMISSIONS", "All Permissions are granted.");
            bindService();
        } else {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        Log.i("TONAL PERMISSIONS", "Permission callback called.");
        if (requestCode != PERMISSION_REQUEST) {
            return;
        }
        // Phone state only enables pause-on-call; the app works without it.
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_PHONE_STATE") != 0) {
            pauseOnIncomingCall = false;
        }
        if (ContextCompat.checkSelfPermission(this, storagePermission()) == 0) {
            bindService();
            return;
        }
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, storagePermission())) {
            Log.e("TONAL PERMISSIONS", "Music access refused for good, app is closing.");
            finish();
            return;
        }
        try {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(getResources().getString(R.string.messagebox_message_permission_files))
                    .setCancelable(false)
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> checkAndRequestPermissions())
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> finish())
                    .create().show();
        } catch (Exception e) {
            checkAndRequestPermissions();
        }
    }

    // ---- Service ----------------------------------------------------------------------------

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo info : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(info.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void bindService() {
        if (isMyServiceRunning(ServiceMusic.class)) {
            Log.e(TAG, "Service is already running.");
            if (mServiceMusic != null && mServiceRunning) {
                Log.i(TAG, "Service is already connected.");
                return;
            }
            Log.i(TAG, "Try to make connection with it.");
            startTimeService = System.currentTimeMillis();
            bindService(new Intent(this, ServiceMusic.class), mConnection, Context.BIND_AUTO_CREATE);
            return;
        }
        if (mServiceRunning) {
            mServiceMusic.notificationSwiped = false;
            return;
        }
        Log.i(TAG, "Starting service.");
        startTimeService = System.currentTimeMillis();
        Intent intent = new Intent(this, ServiceMusic.class);
        intent.putExtra("pauseOnIncomingCall", pauseOnIncomingCall);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    // ---- The library ------------------------------------------------------------------------

    public interface AsyncResponse {
        void processFinish(int timeMs);
    }

    /** Builds the library on a background thread, then reports how long it took. */
    public class AsyncTaskLoader extends AsyncTask<Integer, Integer, Integer> {
        private final AsyncResponse response;

        AsyncTaskLoader(AsyncResponse response) {
            this.response = response;
        }

        @Override
        protected Integer doInBackground(Integer... ignored) {
            Log.i(TAG, "Starting manager.");
            return (int) new Manager(getApplicationContext(), mPreferences, mServiceMusic, null).timePassed;
        }

        @Override
        protected void onPostExecute(Integer timeMs) {
            response.processFinish(timeMs);
        }
    }

    public void startManager() {
        new AsyncTaskLoader(new AsyncResponse() {
            @Override
            public void processFinish(int timeMs) {
                onLibraryLoaded(timeMs);
            }
        }).execute();
    }

    private void onLibraryLoaded(int timeMs) {
        if (isFinishing() || isDestroyed() || getSupportFragmentManager().isStateSaved()) {
            return;
        }
        File asked = songAsked;
        songAsked = null;
        if (asked != null) {
            if (openRequestedSong(asked)) {
                return;
            }
            Log.e(TAG, "The requested song isn't in the library.");
            Toast.makeText(this, R.string.open_with_failed, Toast.LENGTH_LONG).show();
        }
        Fragment first;
        if (timeMs == 0 || !hasPlayableMusic()) {
            Log.i(TAG, "All datas loaded and takes " + (System.currentTimeMillis() - startTime) + " ms.");
            mCoverBackView.setImageBitmap(Utils.blur(BitmapFactory.decodeResource(getResources(), R.drawable.cover2), 40.0f));
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(2500L);
            mCoverBackView.startAnimation(fadeIn);
            Log.e(TAG, "No music has been found, show error screen :)");
            first = new Fragment_Empty();
        } else {
            loadDefaultPlaylist();
            loadLastSong();
            startDriveWatcher();
            Log.i(TAG, "All datas loaded and takes " + (System.currentTimeMillis() - startTime) + " ms.");
            Log.i(TAG, "Launch home...");
            if (mPreferences.getSettingsPlayerBolt()) {
                mServiceMusic.play(getApplicationContext(), true);
            }
            if (savedInstanceState != null) {
                return;
            }
            first = new Fragment_Home_Carousel();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.container_body, first).commitAllowingStateLoss();
    }

    /**
     * Plays a song another app asked for: Home goes underneath and the Player opens on top of it, so
     * Back leads to the library. Returns false when the song isn't in the library.
     */
    private boolean openRequestedSong(File asked) {
        if (!hasPlayableMusic()) {
            Log.e("TonalOpenWith", "Library is empty or unreadable; cannot open " + asked);
            return false;
        }
        loadDefaultPlaylist();
        List<Music> library = mServiceMusic.playlistPlayed.getPlaylist();
        int index = indexOfSong(library, asked);
        if (index < 0) {
            // Not in the library (for example hidden by "skip short songs"): play it anyway, as an
            // extra entry in the queue only, so the library itself stays as the settings define it.
            Music extra = queryMusic(asked);
            if (extra != null) {
                CurrentPlaylist queue = mServiceMusic.playlistPlayed;
                List<Music> withExtra = new ArrayList<>(library);
                withExtra.add(extra);
                queue.newPlaylist(queue.getPlaylistName(), queue.getPlaylistID(), queue.getPlaylistCover(),
                        withExtra.size(), queue.getPlaylistDuration() + extra.getDuration(), withExtra);
                library = withExtra;
                index = withExtra.size() - 1;
            }
        }
        if (index < 0) {
            Log.e("TonalOpenWith", "Not found in " + library.size() + " songs: " + asked
                    + (library.isEmpty() ? "" : " (first library path: " + library.get(0).getFile() + ")"));
            return false;
        }
        Log.i("TonalOpenWith", "Playing library song #" + index + ": " + library.get(index).getFile());
        Context context = getApplicationContext();
        startDriveWatcher();
        mServiceMusic.musicPlayed = new CurrentMusic();
        mServiceMusic.musicPlayed.newSong(context, mServiceMusic.playlistPlayed.getSong(index), index, 0L);
        mServiceMusic.play(context, true);
        Log.i(TAG, "Launch player and play the requested song.");

        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        manager.beginTransaction().replace(R.id.container_body, new Fragment_Home_Carousel()).commitAllowingStateLoss();
        Fragment_Player player = new Fragment_Player();
        Bundle args = new Bundle();
        args.putBoolean("allowplaylist", true);
        player.setArguments(args);
        setTransitionID(0);
        manager.beginTransaction()
                .setCustomAnimations(R.anim.animation_player_opening_close, R.anim.animation_player_opening_open,
                        R.anim.animation_player_closing_close, R.anim.animation_player_closing_open)
                .replace(R.id.container_body, player)
                .addToBackStack(null)
                .commitAllowingStateLoss();
        return true;
    }

    /** Reads one song's details straight from the media store; null when it isn't indexed there. */
    private Music queryMusic(File file) {
        android.database.Cursor c = null;
        try {
            c = getContentResolver().query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{"_id", "album_id", "duration", "date_modified", "title", "artist", "album"},
                    "_data=?", new String[]{file.getPath()}, null);
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            String title = c.getString(4);
            if (title == null || title.isEmpty()) {
                title = file.getName();
            }
            String artist = c.getString(5);
            String album = c.getString(6);
            return new Music(c.getLong(0), c.getLong(1), c.getLong(2) / 1000, c.getLong(3), file.getPath(),
                    title, artist == null ? "" : artist, album == null ? "" : album);
        } catch (Exception e) {
            Log.w("TonalOpenWith", "Could not read the song's details", e);
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /** Position of the file in the list, comparing the plain path first and then the resolved one. */
    private int indexOfSong(List<Music> songs, File file) {
        String wanted = file.getPath();
        for (int i = 0; i < songs.size(); i++) {
            if (wanted.equals(songs.get(i).getFile())) {
                return i;
            }
        }
        try {
            String canonical = file.getCanonicalPath();
            for (int i = 0; i < songs.size(); i++) {
                String path = songs.get(i).getFile();
                if (path != null && canonical.equals(new File(path).getCanonicalPath())) {
                    return i;
                }
            }
        } catch (IOException ignored) {
        }
        // Same file under a different path spelling: match on name and size.
        long size = file.length();
        for (int i = 0; i < songs.size(); i++) {
            String path = songs.get(i).getFile();
            if (path != null && size > 0 && new File(path).getName().equals(file.getName()) && new File(path).length() == size) {
                return i;
            }
        }
        return -1;
    }

    /** True when the library holds at least one playable song. */
    public boolean hasPlayableMusic() {
        try {
            Category all = mServiceMusic.getCategory(getApplicationContext(), 0);
            return !all.getPlaylists().isEmpty() && !all.getPlaylists().get(0).getPlaylistMusics().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void loadDefaultPlaylist() {
        Log.i(TAG, "Initialise the default current playlist.");
        if (!hasPlayableMusic()) {
            Log.e(TAG, "No playable music for the default playlist.");
            return;
        }
        Category all = mServiceMusic.getCategory(getApplicationContext(), 0);
        mServiceMusic.playlistPlayed = new CurrentPlaylist();
        mServiceMusic.playlistPlayed.newPlaylist(all.getTitle(), 0L, all.getCover(), all.getSize(), all.getDuration(),
                all.getPlaylists().get(0).getPlaylistMusics());
    }

    @Override
    public void loadLastSong() {
        Log.i(TAG, "Restore values of the last song played.");
        CurrentPlaylist queue = mServiceMusic.playlistPlayed;
        int position;
        if (new File(mPreferences.getSaveMusicFilename("charliedeltacommando")).exists()) {
            position = 0;
            long savedId = mPreferences.getSaveMusicID();
            for (int i = 0; i < queue.getPlaylistSize(); i++) {
                if (queue.getSong(i).getID() == savedId) {
                    position = i;
                    break;
                }
            }
            if (position > queue.getPlaylistSize()) {
                position = 0;
            }
            Log.i(TAG, "Last song played successfully restored.");
        } else {
            Log.e(TAG, "The app has no last song saved, time to suggest a new one :)");
            int bound = (int) (mServiceMusic.getCategory(getApplicationContext(), 0).getSize() - 1);
            position = bound <= 0 ? 0 : new Random().nextInt(bound);
        }
        mServiceMusic.musicPlayed = new CurrentMusic();
        mServiceMusic.musicPlayed.newSong(getApplicationContext(), queue.getSong(position), position, 0L);
        long cover = mServiceMusic.musicPlayed.getMusic(getApplicationContext()).getCover();
        setThemeArt(Utils.getCoverBitamp(getApplicationContext(), cover, R.drawable.cover0, getCoverScaled()));
        mServiceMusic.getCategory(getApplicationContext(), 0).setCover(cover);
    }

    private void startDriveWatcher() {
        if (mDriveWatcher != null) {
            return;
        }
        mDriveWatcher = new DriveWatcher(getApplicationContext(), new DriveWatcher.Listener() {
            @Override
            public void onDrivesChanged(int driveSongs, boolean connected) {
                if (mServiceMusic != null) {
                    mServiceMusic.pauseIfDriveGone();
                }
                if (DrivePrefs.announce(Application_Main.this)) {
                    Toast.makeText(Application_Main.this, connected ? R.string.drive_toast_connected : R.string.drive_toast_removed,
                            Toast.LENGTH_SHORT).show();
                }
                reloadLibraryForDrives();
            }
        });
        mDriveWatcher.start();
    }

    /** A drive was plugged in or removed: rebuild the library and refresh the Home screens that show it. */
    public void reloadLibraryForDrives() {
        if (isFinishing() || isDestroyed() || mServiceMusic == null) {
            return;
        }
        try {
            new Manager(getApplicationContext(), mPreferences, mServiceMusic, null);
        } catch (Exception e) {
            Log.e(TAG, "Could not reload the library after a drive change.", e);
            return;
        }
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container_body);
        if (fragment == null || fragment.getView() == null) {
            return;
        }
        if (fragment instanceof Fragment_Home_Carousel) {
            ((Fragment_Home_Carousel) fragment).loadDatas();
        } else if (fragment instanceof Fragment_Home_Playlists) {
            ((Fragment_Home_Playlists) fragment).loadDatas();
        } else if (fragment instanceof Fragment_Home_Songs) {
            ((Fragment_Home_Songs) fragment).loadDatas();
        }
    }

    // ---- Screens ----------------------------------------------------------------------------

    @Override
    public void onDrawerItemSelected(View view, int position) {
        displayView(position);
    }

    public void displayView(int position) {
        mSearch.setVisibility(View.INVISIBLE);
        settingSubSectionIsOpen = false;
        settingSectionIsOpen = false;
        updaterSectionIsOpen = false;
        Fragment next = null;
        switch (position) {
            case 0:
                next = new Fragment_Home_Carousel();
                break;
            case 1:
                next = new Fragment_Playlist();
                break;
            case 2:
                next = new Fragment_ReadyForBeta();
                break;
            case 3:
                next = new Fragment_RegulateAudio();
                break;
            case 4:
                next = new Fragment_Settings();
                break;
            case 5:
                // "Close": stop the music and shut the service down.
                setFragment(0);
                mServiceMusic.deleteApplication();
                mServiceMusic.deleteNotification();
                break;
            default:
                break;
        }
        if (next == null) {
            return;
        }
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.container_body);
        if (current != null) {
            current.setReenterTransition(null);
            current.setExitTransition(null);
        }
        next.setSharedElementEnterTransition(null);
        next.setSharedElementReturnTransition(null);
        getSupportFragmentManager().beginTransaction().replace(R.id.container_body, next).commit();
    }

    public void openHome() {
        settingSubSectionIsOpen = false;
        settingSectionIsOpen = false;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.animation_player_opening_close, R.anim.animation_player_opening_open,
                R.anim.animation_player_closing_close, R.anim.animation_player_closing_open);
        transaction.replace(R.id.container_body, new Fragment_Home_Carousel());
        transaction.commit();
    }

    /** Closes the Player: the controls slide away, the cover ring fades, then the screen is popped. */
    public void closePlayer() {
        final Animation addOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animate_controls_left_left);
        final Animation previousOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animate_controls_left);
        final Animation nextOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animate_controls_right);
        final Animation loopOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animate_controls_right_right);
        final Fragment_Player player = (Fragment_Player) getSupportFragmentManager().findFragmentById(R.id.container_body);
        AlphaAnimation fade = new AlphaAnimation(1.0f, 0.0f);
        fade.setDuration(getResources().getInteger(R.integer.transition_duration_coverback));
        fade.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationStart(Animation animation) {
                player.closePlaylistButton();
                player.pAdd.startAnimation(addOut);
                player.pPrevious.startAnimation(previousOut);
                player.pNext.startAnimation(nextOut);
                player.pLoop.startAnimation(loopOut);
                if (CoverView.mStartAnimator != null && CoverView.mStartAnimator.isStarted()) {
                    CoverView.mStartAnimator.cancel();
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                player.mCoverHoverView.setVisibility(View.INVISIBLE);
                getSupportFragmentManager().popBackStack();
            }
        });
        player.mCoverHoverView.startAnimation(fade);
    }

    @Override
    public void refreshNavigation(int item) {
        drawerFragment.lastClick = item;
        drawerFragment.mAdapter.selected_item = item;
        drawerFragment.mAdapter.notifyDataSetChanged();
    }

    // ---- The play button of whichever screen is showing -------------------------------------

    private PlayPauseButton currentFab() {
        switch (fragmentID) {
            case SCREEN_HOME:
                return Fragment_Home_Carousel.mFabView;
            case SCREEN_PLAYLISTS:
                return Fragment_Home_Playlists.mFabView;
            case SCREEN_SONGS:
                return Fragment_Home_Songs.mFabView;
            case SCREEN_PLAYER:
                return Fragment_Player.mFabView;
            case SCREEN_QUEUE:
                return Fragment_Playlist.mFabView;
            default:
                return null;
        }
    }

    @Override
    public void refreshFabIcon() {
        PlayPauseButton fab = currentFab();
        if (fab != null) {
            fab.showPlay(isPlaying());
        }
    }

    @Override
    public boolean isPlaying() {
        return mServiceMusic != null && mServiceMusic.isPlaying();
    }

    @Override
    public void seekTo(int positionMs) {
        if (mServiceMusic != null) {
            mServiceMusic.MusicPlayer.seekTo(positionMs);
        }
    }

    /** Called when the song changes or playback starts or stops: refresh whatever the screen shows about it. */
    @Override
    public void refreshPlayerVinyl(Context context) {
        if (Fragment_Home_Songs.mTimeView != null && Fragment_Home_Songs.mScrollBar != null) {
            Fragment_Home_Songs.mScrollBar.setHandleColour(getThemeColorLight());
        }
        updateRecordSpin();
        switch (fragmentID) {
            case SCREEN_HOME:
                refreshHomeForSong(context);
                break;
            case SCREEN_SONGS:
                Fragment_Home_Songs songs = (Fragment_Home_Songs) getSupportFragmentManager().findFragmentById(R.id.container_body);
                if (songs != null && mServiceMusic != null && mServiceMusic.playlistPlayed != null
                        && mServiceMusic.playlistPlayed.getPlaylistID() == songs.getPlaylistID()) {
                    songs.refreshTheme(0);
                }
                break;
            case SCREEN_PLAYER:
                if (mServiceMusic != null && mServiceMusic.musicPlayed != null) {
                    Music song = mServiceMusic.musicPlayed.getMusic(context);
                    new CoverCrossfade(context, song.getCover(), R.drawable.cover0, 400).execute();
                    setToolbarText(song.getTitle(), song.getArtist());
                }
                if (Fragment_Player.mCoverView != null) {
                    Fragment_Player.mCoverView.setImageBitmap(getThemeArt());
                }
                break;
            case SCREEN_QUEUE:
                if (mServiceMusic != null && mServiceMusic.musicPlayed != null) {
                    new CoverCrossfade(context, mServiceMusic.musicPlayed.getMusic(context).getCover(), R.drawable.cover0, 300).execute();
                    setToolbarText(getString(R.string.menu_playlist), mServiceMusic.playlistPlayed.getPlaylistName());
                }
                if (Fragment_Playlist.mCoverView != null) {
                    Fragment_Playlist.mCoverView.setImageBitmap(getThemeArt());
                }
                break;
            default:
                break;
        }
    }

    /** Keeps the Player's spinning cover in step with play, pause and next/previous. */
    private void updateRecordSpin() {
        ValueAnimator animator = CoverView.mStartAnimator;
        if (fragmentID != SCREEN_PLAYER || pressNextPrevious) {
            if (fragmentID == SCREEN_PLAYER && runningActivities != 0) {
                if (Fragment_Player.mCoverView != null && Fragment_Player.mCoverView.isRunning()) {
                    if (animator != null) {
                        animator.cancel();
                    }
                } else {
                    if (animator != null) {
                        animator.start();
                    }
                    pressNextPrevious = false;
                }
            }
        } else if (isPlaying() && (animator == null || !animator.isStarted())) {
            if (animator != null) {
                animator.start();
            }
        } else if (isPlaying() && animator != null && animator.isStarted()) {
            animator.resume();
        } else if (!isPlaying() && animator != null && animator.isStarted()) {
            animator.pause();
        }
    }

    private void refreshHomeForSong(Context context) {
        if (getServiceMusic() != null && getServiceMusic().musicPlayed != null) {
            setToolbarText(getString(R.string.menu_main), getServiceMusic().musicPlayed.getMusic(context).getMusic().getTitle());
        }
        Fragment_Home_Carousel home = (Fragment_Home_Carousel) getSupportFragmentManager().findFragmentById(R.id.container_body);
        if (home != null && Fragment_Home_Carousel.mRecentsAdapter != null) {
            Fragment_Home_Carousel.mRecentsAdapter.refreshRecentList();
            home.refreshRecentTitleName();
            home.scrollReset();
        }
    }

    // ---- Counters and flags the screens read and set ----------------------------------------

    @Override public int getFragment() { return fragmentID; }
    @Override public void setFragment(int id) { fragmentID = id; }
    @Override public int getTransitionID() { return transitionID; }
    @Override public void setTransitionID(int id) { transitionID = id; }
    @Override public boolean getPressNext() { return pressNextPrevious; }
    @Override public void setPressNext(boolean pressed) { pressNextPrevious = pressed; }
    @Override public int getRunningActivities() { return runningActivities; }
    @Override public int getCardItem() { return cardItem; }
    @Override public void setCardItem(int item) { cardItem = item; }
    @Override public int getCardPosition() { return cardPosition; }
    @Override public void setCardPosition(int position) { cardPosition = position; }
    @Override public ServiceMusic getServiceMusic() { return mServiceMusic; }
    @Override public TextView getTitleView() { return mTitle; }
    @Override public TextView getSubtitleView() { return mSubTitle; }
    @Override public void setSettingSectionOpen(boolean open) { settingSectionIsOpen = open; }
    @Override public void setSettingSubSectionOpen(boolean open) { settingSubSectionIsOpen = open; }
    @Override public void setUpdaterSectionOpen(boolean open) { updaterSectionIsOpen = open; }

    @Override
    public void setToolbarText(String title, String subtitle) {
        if (title != null) {
            mTitle.setText(title);
        }
        if (subtitle != null) {
            mSubTitle.setText(subtitle);
        }
    }

    // ---- Search bar -------------------------------------------------------------------------

    public void hideKeyboard(Context context) {
        View focus = getCurrentFocus();
        if (focus != null) {
            ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    public void resetSearch() {
        if (mSearchBar != null && mSearchBar.getText().length() > 0) {
            mSearchBar.setText("");
            applySearchFilter("");
        }
        hideSearchBar(true);
    }

    @Override
    public void showSearchBar(boolean animate) {
        if (searchBarVisible) {
            return;
        }
        if (!animate) {
            mSearchBar.setVisibility(View.VISIBLE);
            searchBarVisible = true;
            return;
        }
        int x = mSearchBar.getWidth() - convertDpToPx(23);
        int y = mSearchBar.getHeight() / 2;
        ViewAnimationUtils.createCircularReveal(mSearchBar, x, y, 0.0f, (float) Math.hypot(x, y)).start();
        mSearchBar.setVisibility(View.VISIBLE);
        searchBarVisible = true;
        mSearchBar.requestFocus();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(mSearchBar, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void hideSearchBar(boolean animate) {
        mSearchBar.setText("");
        if (!searchBarVisible) {
            return;
        }
        if (!animate) {
            mSearchBar.setVisibility(View.INVISIBLE);
            searchBarVisible = false;
            return;
        }
        int x = mSearchBar.getWidth() - convertDpToPx(23);
        int y = mSearchBar.getHeight() / 2;
        Animator collapse = ViewAnimationUtils.createCircularReveal(mSearchBar, x, y, (float) Math.hypot(x, y), 0.0f);
        collapse.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                hideKeyboard(getApplicationContext());
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                searchBarVisible = false;
                mSearchBar.setAlpha(1.0f);
                mSearchBar.setVisibility(View.INVISIBLE);
            }
        });
        collapse.start();
    }

    @Override
    public void showSearchIcon(boolean animate) {
        if (!animate) {
            mSearch.setVisibility(View.VISIBLE);
            return;
        }
        int duration = getResources().getInteger(R.integer.transition_duration_coverback);
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(duration);
        fadeIn.setStartOffset(duration + 50);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationEnd(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationStart(Animation animation) {
                mSearch.setVisibility(View.VISIBLE);
            }
        });
        mSearch.startAnimation(fadeIn);
    }

    @Override
    public void hideSearchIcon(boolean animate) {
        if (!animate || mSearch.getVisibility() == View.INVISIBLE) {
            mSearch.setVisibility(View.INVISIBLE);
            return;
        }
        if (searchBarVisible) {
            hideSearchBar(true);
        }
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(getResources().getInteger(R.integer.transition_duration_fragments));
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                mSearch.setVisibility(View.INVISIBLE);
            }
        });
        mSearch.startAnimation(fadeOut);
    }

    @Override
    public void setSearchIconAlpha(float alpha) {
        if (searchBarVisible) {
            mSearch.setAlpha(alpha);
        }
    }

    // ---- Theme colours and the blurred background -------------------------------------------

    @Override public Bitmap getThemeArt() { return themeArt; }
    @Override public void setThemeArt(Bitmap art) { themeArt = art; }
    @Override public Bitmap getThemeThumbnail() { return themeThumbnail; }
    public void setThemeThumbnail(Bitmap thumbnail) { themeThumbnail = thumbnail; }
    @Override public int getThemeColorFab() { return themeColorPlay; }
    @Override public void setThemeColorFab(int color) { themeColorPlay = color; }
    @Override public int getThemeColorLight() { return themeColorLight; }
    @Override public void setThemeColorLight(int color) { themeColorLight = color; }
    @Override public int getThemeColorVibrant() { return themeColorVibrant; }
    @Override public void setThemeColorVibrant(int color) { themeColorVibrant = color; }
    @Override public int getCoverScaled() { return coverScaled; }

    public void setThemeColors(int vibrant, int light, int play) {
        themeColorVibrant = vibrant;
        themeColorLight = light;
        themeColorPlay = play;
    }

    @Override
    public void updateBackground(Context context, long coverId, int fallbackRes, int duration) {
        new CoverCrossfade(context, coverId, fallbackRes, duration).execute();
    }

    @Override
    public void blurBackground(Context context, int radius) {
        if (getThemeThumbnail() != null) {
            mCoverBackView.setImageBitmap(Utils.blur(getThemeThumbnail(), radius));
        }
    }

    /** Dark colours get a light icon and light colours a dark one. */
    private int findColorFrom(int color) {
        double brightness = (0.299d * Color.red(color) + 0.587d * Color.green(color) + 0.114d * Color.blue(color)) / 255.0d;
        return 1.0d - brightness < 0.3d ? -12303292 : -1;
    }

    private Bitmap blendImages(int size, int alpha, Bitmap base, Bitmap top) {
        Bitmap.Config config = base.getConfig();
        if (config == null) {
            config = Bitmap.Config.RGB_565;
        }
        Bitmap result = Bitmap.createBitmap(size, size, config);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(base, 0.0f, 0.0f, null);
        Paint paint = new Paint();
        paint.setAlpha(Math.min(alpha, 255));
        canvas.drawBitmap(top, 0.0f, 0.0f, paint);
        return result;
    }

    /**
     * Cross-fades the blurred background (and the play button colour) from the previous cover to the
     * next one. The colours and frames are worked out off the main thread.
     */
    private class CoverCrossfade extends AsyncTask<Void, Void, AnimationDrawable> {
        private final Context mContext;
        private final long mNextCover;
        private final int mFallback;
        private final int mDuration;
        private PlayPauseButton fab;
        private int nextColor;
        private int nextColorLight;
        private int nextColorVibrant;

        CoverCrossfade(Context context, long coverId, int fallbackRes, int duration) {
            mContext = context;
            mNextCover = coverId;
            mFallback = fallbackRes;
            mDuration = duration;
            fab = currentFab();
        }

        @Override
        protected AnimationDrawable doInBackground(Void... ignored) {
            Bitmap cover = Utils.getCoverBitamp(mContext, mNextCover, mFallback, blurScaled);
            if (cover == null || cover.getWidth() <= 0 || cover.getHeight() <= 0) {
                return null;
            }
            Bitmap next = Bitmap.createBitmap(cover);
            if (getThemeThumbnail() != null && next.sameAs(getThemeThumbnail())) {
                return null;
            }
            if (getThemeThumbnail() == null) {
                // First background: derive the starting colours from the song that is playing now.
                long currentCover = 0L;
                if (mServiceMusic != null && mServiceMusic.musicPlayed != null && mServiceMusic.musicPlayed.getMusic(mContext) != null) {
                    currentCover = mServiceMusic.musicPlayed.getMusic(mContext).getCover();
                }
                setThemeThumbnail(Utils.getCoverBitamp(mContext, currentCover, R.drawable.cover0, blurScaled));
                if (getThemeThumbnail() != null) {
                    Palette start = Palette.from(getThemeThumbnail()).generate();
                    int white = ContextCompat.getColor(mContext, R.color.colorWhite);
                    setThemeColorVibrant(start.getVibrantColor(start.getMutedColor(white)));
                    setThemeColorLight(start.getLightVibrantColor(start.getLightMutedColor(white)));
                    setThemeColorFab(findColorFrom(getThemeColorVibrant()));
                }
            }
            Palette palette = Palette.from(next).generate();
            int white = ContextCompat.getColor(mContext, R.color.colorWhite);
            nextColorVibrant = palette.getVibrantColor(palette.getMutedColor(white));
            nextColorLight = palette.getLightVibrantColor(palette.getLightMutedColor(white));
            nextColor = findColorFrom(nextColorVibrant);
            int darkness = Utils.isDarkThumbnail(next);
            if (darkness != 0) {
                next = Utils.doBrightness(next, darkness);
            }
            int blur = mPreferences.getSettingsUIBlurIntensity();
            Bitmap blurredNext = Utils.blur(next, blur);
            Bitmap blurredPrevious = Utils.blur(getThemeThumbnail(), blur);
            if (blurredNext == null || blurredPrevious == null) {
                return null;
            }
            AnimationDrawable frames = new AnimationDrawable();
            for (int alpha = 32; alpha <= 256; alpha += 32) {
                Bitmap blended = blendImages(blurScaled, alpha, blurredPrevious, blurredNext);
                if (blended != null) {
                    frames.addFrame(new BitmapDrawable(mContext.getResources(), blended), mDuration / 8);
                }
            }
            setThemeThumbnail(next);
            blurredNext.recycle();
            blurredPrevious.recycle();
            return frames;
        }

        @Override
        protected void onPostExecute(AnimationDrawable frames) {
            if (frames == null) {
                if (fab != null) {
                    fab.showPlay(isPlaying());
                }
                return;
            }
            mCoverBackView.setImageDrawable(frames);
            frames.setOneShot(true);
            frames.start();
            if (fab != null) {
                fab.setColor(nextColor);
                fab.showPlay(isPlaying());
                ObjectAnimator tint = ObjectAnimator.ofInt(fab, "backgroundTint", getThemeColorVibrant(), nextColorVibrant);
                tint.setDuration(mDuration);
                tint.setEvaluator(new ArgbEvaluator());
                tint.setInterpolator(new DecelerateInterpolator(2.0f));
                tint.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        fab.setBackgroundTintList(ColorStateList.valueOf((Integer) animation.getAnimatedValue()));
                    }
                });
                tint.start();
            }
            setThemeColors(nextColorVibrant, nextColorLight, nextColor);
        }
    }

    // ---- Screen size and units --------------------------------------------------------------

    @Override
    public float convertDpToPx(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    @Override
    public int convertDpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    @Override
    public int getScreenHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    @Override
    public int getScreenWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    /** Space to keep clear at the bottom for an on-screen navigation bar (0 with gesture navigation). */
    @Override
    public int getPaddingBottom(Context context) {
        if (getNavigationBarSize(context).x == 0
                || context.getResources().getIdentifier("navigation_bar_height", "dimen", "android") <= 0) {
            return 0;
        }
        return getNavigationBarHeight(context);
    }

    public static Point getAppUsableScreenSize(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static Point getRealScreenSize(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return size;
    }

    public static Point getNavigationBarSize(Context context) {
        Point usable = getAppUsableScreenSize(context);
        Point real = getRealScreenSize(context);
        if (usable.x < real.x) {
            return new Point(real.x - usable.x, usable.y);
        }
        return usable.y < real.y ? new Point(usable.x, real.y - usable.y) : new Point();
    }

    public static int getNavigationBarHeight(Context context) {
        int id = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? context.getResources().getDimensionPixelSize(id) : 0;
    }

    public static int getStatusBarHeight(Context context) {
        int id = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? context.getResources().getDimensionPixelSize(id) : 0;
    }
}
