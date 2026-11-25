package com.meccael.tonal.application;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.meccael.tonal.R;
import com.meccael.tonal.artwork.ArtworkStore;
import com.meccael.tonal.artwork.ArtworkWorker;
import com.meccael.tonal.manager.DriveFilter;
import com.meccael.tonal.manager.DrivePrefs;
import com.meccael.tonal.manager.Manager;
import com.meccael.tonal.usb.UsbDrive;
import com.meccael.tonal.wheelbar.HorizontalWheelView;

/**
 * Settings. Sections 1-4 (Player, Interface, Notification, Lock screen) plus Drive live on one
 * scrolling page (or a section rail in landscape); the sleep timer (5) and the custom
 * notification colours (6) slide in as sub-panels.
 */
public class Fragment_Settings extends Fragment {
    /** 0 = none open, 5 = sleep timer panel, 6 = custom notification panel. */
    public static int sectionNumber = 0;

    private static final int SECTION_PLAYER = 1;
    private static final int SECTION_UI = 2;
    private static final int SECTION_NOTIFICATION = 3;
    private static final int SECTION_LOCKSCREEN = 4;
    private static final int SECTION_TIMER = 5;
    private static final int SECTION_CUSTOM_NOTIFICATION = 6;

    /** Choices for "skip songs shorter than": seconds up to 45, then 1, 2 and 3 minutes. */
    private static final int[] EXCLUDE_STEPS = {0, 15, 30, 45, 60, 120, 180};

    private CallbacksInterface mActivity;
    private Preferences mPreferences;
    private Vibrator mVibrate;
    private Animation mPressDown;
    private Animation mPressUp;
    private Animation mSpinOnce;

    private View mRoot;
    private ScrollView mScroll;
    private View mGearsView;
    private View mSubPanel;
    private LinearLayout mTimerPanel;
    private LinearLayout mCustomPanel;
    private ImageView mCogLittle;
    private ImageView mCogBig;
    private TextView mLyricsFolderMessage;
    private ImageButton mJumpBtn;
    private Runnable mJumpAction;
    private boolean mValuesLoaded = false;
    private boolean mInTransition = false;
    private int mExcludeSeconds;

    // Landscape section rail.
    private static int sRailSection = 0;
    private TextView[] mRailItems;
    private static final int[] RAIL_VIEWS = {R.id.view_player, R.id.view_ui, R.id.view_notification, R.id.view_lockscreen, R.id.view_drive};
    private static final int[] RAIL_LABELS = {R.string.settings_player, R.string.settings_ui, R.string.settings_notification, R.string.settings_lockscreen, R.string.settings_drive};
    private static final int[] SECTION_HEADERS = {R.id.sec_player, R.id.sec_ui, R.id.sec_notification, R.id.sec_lockscreen, R.id.sec_drive};

    private final ActivityResultLauncher<Uri> mLyricsFolderPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri == null || getContext() == null) {
                        return;
                    }
                    try {
                        getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) {
                        // The grant still works for this session.
                    }
                    mPreferences.setLyricsTreeUri(uri.toString());
                    showLyricsFolder();
                }
            });

    // ---- Lifecycle --------------------------------------------------------------------------

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mActivity = (CallbacksInterface) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        mRoot = view;
        mPreferences = new Preferences(getContext());
        mVibrate = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        mPressDown = AnimationUtils.loadAnimation(getContext(), R.anim.animation_bouton_down);
        mPressUp = AnimationUtils.loadAnimation(getContext(), R.anim.animation_bouton_up);
        mSpinOnce = AnimationUtils.loadAnimation(getContext(), R.anim.animation_bouton_up_rotation_full);
        mCogLittle = (ImageView) view.findViewById(R.id.coglittle);
        mCogBig = (ImageView) view.findViewById(R.id.cogbig);
        mScroll = (ScrollView) view.findViewById(R.id.settings_scroll);
        mGearsView = view.findViewById(R.id.settings_gears);

        setupSubPanels(view);
        setupDrive(view);
        setupGearParallax();

        String version = "";
        try {
            version = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }
        ((TextView) view.findViewById(R.id.settings_about)).setText(getString(R.string.settings_about, version));

        int navBar = mActivity.getPaddingBottom(getContext());
        if (navBar > 0) {
            View content = view.findViewById(R.id.settings_content);
            content.setPadding(0, 0, 0, content.getPaddingBottom() + navBar);
        }
        setupLandscapeRail(view);

        if (mActivity.getServiceMusic() == null) {
            mActivity.setFragment(50);
        } else {
            loadDatas();
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application_Extend.getInstance().trackScreenView("Settings");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getContext() != null) {
            UsbDrive.get(getContext()).setStatusListener(null);
        }
        if (mJumpBtn != null && mJumpBtn.getTag(R.id.toolbar_search) == mJumpAction) {
            mJumpBtn.setTag(R.id.toolbar_search, null);
            mJumpBtn.setImageResource(R.drawable.ic_search_white_24dp);
        }
        mRailItems = null;
        mJumpBtn = null;
        mJumpAction = null;
        mScroll = null;
        mGearsView = null;
        mSubPanel = null;
        mTimerPanel = null;
        mCustomPanel = null;
        mLyricsFolderMessage = null;
        mRoot = null;
        // The next view this instance builds needs its switches and sliders wired again.
        mValuesLoaded = false;
    }

    public void loadDatas() {
        mActivity.setFragment(50);
        setupJump();
        if (mRoot != null && !mValuesLoaded) {
            mValuesLoaded = true;
            for (int section = SECTION_PLAYER; section <= SECTION_LOCKSCREEN; section++) {
                loadValues(mRoot, section);
            }
        }
        mActivity.setToolbarText(getString(R.string.settings_global), getString(R.string.settings_sub));
        mActivity.refreshNavigation(4);
        if (mCogLittle == null || mCogBig == null) {
            return;
        }
        mCogLittle.startAnimation(spin(360f, 0f, 20, 3989L));
        mCogBig.startAnimation(spin(0f, 360f, 40, 8000L));
    }

    private RotateAnimation spin(float from, float to, int pivotDp, long durationMs) {
        RotateAnimation turn = new RotateAnimation(from, to, mActivity.convertDpToPx(pivotDp), mActivity.convertDpToPx(pivotDp));
        turn.setDuration(durationMs);
        turn.setRepeatCount(Animation.INFINITE);
        turn.setInterpolator(new LinearInterpolator());
        return turn;
    }

    // ---- Small helpers ----------------------------------------------------------------------

    private void buzz() {
        if (mPreferences.getSettingsUIVibration()) {
            mVibrate.vibrate(5L);
        }
    }

    private ServiceMusic service() {
        return mActivity.getServiceMusic();
    }

    /** Wires a switch: shows the stored value first, then buzzes and reports each change. */
    private void bindSwitch(View root, int id, boolean value, final SwitchChange change) {
        MaterialSwitch toggle = (MaterialSwitch) root.findViewById(id);
        if (toggle == null) {
            return;
        }
        toggle.setChecked(value);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                buzz();
                change.onChange(checked);
            }
        });
    }

    private interface SwitchChange {
        void onChange(boolean checked);
    }

    /** Press feedback shared by the small round buttons: animate and click on touch-down. */
    private void pressFeedback(View view) {
        view.startAnimation(mPressDown);
        view.playSoundEffect(SoundEffectConstants.CLICK);
        buzz();
    }

    // ---- Section values ---------------------------------------------------------------------

    private void loadValues(View view, int section) {
        switch (section) {
            case SECTION_PLAYER:
                bindPlayer(view);
                break;
            case SECTION_UI:
                bindInterface(view);
                break;
            case SECTION_NOTIFICATION:
                bindNotification(view);
                break;
            case SECTION_LOCKSCREEN:
                bindLockscreen(view);
                break;
            case SECTION_TIMER:
                bindTimer(view);
                break;
            case SECTION_CUSTOM_NOTIFICATION:
                bindCustomNotification(view);
                break;
            default:
                break;
        }
    }

    // -- Player --

    private void bindPlayer(View view) {
        bindSwitch(view, R.id.switch_player_schumi, mPreferences.getSettingsPlayerBolt(), new SwitchChange() {
            @Override public void onChange(boolean checked) { mPreferences.setSettingsPlayerBolt(checked); }
        });
        bindSwitch(view, R.id.switch_player_headset, mPreferences.getSettingsPlayerHeadset(), new SwitchChange() {
            @Override public void onChange(boolean checked) { mPreferences.setSettingsPlayerHeadset(checked); }
        });

        final Button number = (Button) view.findViewById(R.id.picker_player_duration);
        final TextView unit = (TextView) view.findViewById(R.id.picker_player_unite);
        mExcludeSeconds = mPreferences.getSettingsPlayerExcludeMusic();
        if (mExcludeSeconds >= 60 && mExcludeSeconds % 60 == 0 && mExcludeSeconds <= 180) {
            unit.setText(R.string.settings_player_duration_exclude_min);
            number.setText("0" + (mExcludeSeconds / 60));
        } else {
            unit.setText(R.string.settings_player_duration_exclude_sec);
            number.setText(String.valueOf(mExcludeSeconds));
        }
        number.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buzz();
                mExcludeSeconds = mPreferences.getSettingsPlayerExcludeMusic();
                Animation out = AnimationUtils.loadAnimation(getContext(), R.anim.animation_settings_number_gotop);
                final Animation in = AnimationUtils.loadAnimation(getContext(), R.anim.animation_settings_number_gobottom);
                out.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {
                        advanceExcludeStep(unit, number);
                        number.startAnimation(in);
                        mPreferences.setSettingsPlayerExcludeMusic(mExcludeSeconds);
                        // Rebuild the library a moment later so the new minimum length takes effect.
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new Manager(getContext(), mPreferences, service(), null);
                            }
                        }, 150L);
                    }
                });
                number.startAnimation(out);
            }
        });

        bindLyrics(view);
        bindArtwork(view);
    }

    /** Moves to the next skip length (wrapping to 0) and shows it. */
    private void advanceExcludeStep(TextView unit, Button number) {
        int index = -1;
        for (int i = 0; i < EXCLUDE_STEPS.length; i++) {
            if (EXCLUDE_STEPS[i] == mExcludeSeconds) {
                index = i;
            }
        }
        if (index < 0) {
            return;     // An unknown stored value stays as it is.
        }
        mExcludeSeconds = EXCLUDE_STEPS[(index + 1) % EXCLUDE_STEPS.length];
        if (mExcludeSeconds >= 60) {
            unit.setText(R.string.settings_player_duration_exclude_min);
            number.setText("0" + (mExcludeSeconds / 60));
        } else {
            unit.setText(R.string.settings_player_duration_exclude_sec);
            number.setText(mExcludeSeconds == 0 ? "00" : String.valueOf(mExcludeSeconds));
        }
    }

    private void bindLyrics(View view) {
        final MaterialSwitch show = (MaterialSwitch) view.findViewById(R.id.switch_lyrics_show);
        final MaterialSwitch online = (MaterialSwitch) view.findViewById(R.id.switch_lyrics_online);
        if (show != null && online != null) {
            boolean showing = mPreferences.getSettingsLyricsShow();
            show.setChecked(showing);
            online.setChecked(mPreferences.getSettingsLyricsOnline());
            online.setEnabled(showing);
            online.setAlpha(showing ? 1.0f : 0.4f);
            show.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean checked) {
                    buzz();
                    mPreferences.setSettingsLyricsShow(checked);
                    online.setEnabled(checked);
                    online.setAlpha(checked ? 1.0f : 0.4f);
                }
            });
            online.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean checked) {
                    buzz();
                    mPreferences.setSettingsLyricsOnline(checked);
                }
            });
        }

        // Lyrics folder: lets newer Android read .lrc files that sit beside the songs.
        mLyricsFolderMessage = (TextView) view.findViewById(R.id.lyrics_folder_message);
        View row = view.findViewById(R.id.row_lyrics_folder);
        if (row != null && mLyricsFolderMessage != null) {
            showLyricsFolder();
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLyricsFolderPicker.launch(null);
                }
            });
            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mPreferences.setLyricsTreeUri(null);
                    showLyricsFolder();
                    Toast.makeText(v.getContext(), R.string.settings_lyrics_folder_cleared, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }

    private void showLyricsFolder() {
        if (mLyricsFolderMessage == null) {
            return;
        }
        String uri = mPreferences.getLyricsTreeUri();
        if (uri == null) {
            mLyricsFolderMessage.setText(R.string.settings_lyrics_folder_none);
            return;
        }
        String id = DocumentsContract.getTreeDocumentId(Uri.parse(uri));
        String afterColon = id.substring(id.indexOf(':') + 1);
        mLyricsFolderMessage.setText(getString(R.string.settings_lyrics_folder_set, afterColon.isEmpty() ? id : afterColon));
    }

    /** Online artwork: a Wi-Fi-only automatic fill, and a manual "fix all" with live progress. */
    private void bindArtwork(View view) {
        final Context app = view.getContext().getApplicationContext();
        MaterialSwitch auto = (MaterialSwitch) view.findViewById(R.id.switch_artwork_auto);
        if (auto != null) {
            auto.setChecked(ArtworkStore.isAutoEnabled(app));
            auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean checked) {
                    buzz();
                    ArtworkStore.setAutoEnabled(app, checked);
                    if (checked) {
                        ArtworkWorker.enqueue(app);
                    } else {
                        ArtworkWorker.cancel(app);
                    }
                }
            });
        }
        View fix = view.findViewById(R.id.artwork_fix_all);
        if (fix == null) {
            return;
        }
        fix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArtworkWorker.enqueue(app);
                Toast.makeText(app, R.string.settings_artwork_started, Toast.LENGTH_LONG).show();
            }
        });
        if (!(fix instanceof Button)) {
            return;
        }
        final Button fixButton = (Button) fix;
        final String idleText = fixButton.getText().toString();
        try {
            WorkManager.getInstance(app)
                    .getWorkInfosForUniqueWorkLiveData(ArtworkWorker.UNIQUE_NAME)
                    .observe(getViewLifecycleOwner(), infos -> fixButton.setText(artworkButtonText(infos, idleText)));
        } catch (IllegalStateException e) {
            // The view lifecycle isn't available yet: the button keeps its normal label.
        }
    }

    private String artworkButtonText(List<WorkInfo> infos, String idleText) {
        if (infos == null || infos.isEmpty()) {
            return idleText;
        }
        WorkInfo info = infos.get(0);
        if (info.getState() == WorkInfo.State.RUNNING) {
            return getString(R.string.settings_artwork_progress,
                    info.getProgress().getInt(ArtworkWorker.KEY_DONE, 0),
                    info.getProgress().getInt(ArtworkWorker.KEY_TOTAL, 0),
                    info.getProgress().getInt(ArtworkWorker.KEY_FOUND, 0));
        }
        if (info.getState() == WorkInfo.State.ENQUEUED || info.getState() == WorkInfo.State.BLOCKED) {
            return getString(R.string.settings_artwork_waiting);
        }
        return idleText;
    }

    // -- Interface --

    private void bindInterface(View view) {
        bindSwitch(view, R.id.switch_ui_vibration, mPreferences.getSettingsUIVibration(), new SwitchChange() {
            @Override public void onChange(boolean checked) { mPreferences.setSettingsUIVibration(checked); }
        });
        bindSwitch(view, R.id.switch_ui_screen_on, mPreferences.getSettingsUIScreenOn(), new SwitchChange() {
            @Override
            public void onChange(boolean checked) {
                mPreferences.setSettingsUIScreenOn(checked);
                if (checked) {
                    getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });

        // The background blur is stored 5 higher than the slider position.
        final TextView blurText = (TextView) view.findViewById(R.id.blur_text);
        final SeekBar blur = (SeekBar) view.findViewById(R.id.blur_value);
        ImageButton reset = (ImageButton) view.findViewById(R.id.blur_default);
        int shown = mPreferences.getSettingsUIBlurIntensity() - 5;
        blurText.setText(getString(R.string.settings_ui_blur_message, shown));
        blur.setProgress(shown);
        reset.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressFeedback(v);
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.startAnimation(mSpinOnce);
                        blurText.setText(getString(R.string.settings_ui_blur_message, 5));
                        blur.setProgress(5);
                        mActivity.blurBackground(getContext(), 10);
                        mPreferences.setSettingsUIBlurIntensity(10);
                        return true;
                    default:
                        return false;
                }
            }
        });
        blur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                blurText.setText(getString(R.string.settings_ui_blur_message, progress));
                mPreferences.setSettingsUIBlurIntensity(progress + 5);
                mActivity.blurBackground(getContext(), progress + 5);
            }

            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
    }

    // -- Notification --

    private void bindNotification(View view) {
        bindSwitch(view, R.id.switch_notification_expand, mPreferences.getSettingsNotificationExpandable(), new SwitchChange() {
            @Override public void onChange(boolean checked) { mPreferences.setSettingsNotificationExpandable(checked); }
        });
        bindSwitch(view, R.id.switch_notification_swipe, mPreferences.getSettingsNotificationSwipeable(), new SwitchChange() {
            @Override public void onChange(boolean checked) { mPreferences.setSettingsNotificationSwipeable(checked); }
        });
        bindSwitch(view, R.id.switch_notification_stop, mPreferences.getSettingsNotificationClose(), new SwitchChange() {
            @Override public void onChange(boolean checked) { mPreferences.setSettingsNotificationClose(checked); }
        });
    }

    // -- Lock screen --

    private void bindLockscreen(View view) {
        bindSwitch(view, R.id.switch_lockscreen_background, mPreferences.getSettingsLockscreenBackground(), new SwitchChange() {
            @Override
            public void onChange(boolean checked) {
                mPreferences.setSettingsLockscreenBackground(checked);
                service().refreshLockScreenPlayer();
            }
        });
        bindSwitch(view, R.id.switch_lockscreen_blur, mPreferences.getSettingsLockscreenBlur(), new SwitchChange() {
            @Override
            public void onChange(boolean checked) {
                mPreferences.setSettingsLockscreenBlur(checked);
                service().refreshLockScreenPlayer();
            }
        });
    }

    // -- Sleep timer panel --

    private void bindTimer(View view) {
        final TextView label = (TextView) view.findViewById(R.id.timer_text);
        final ImageButton toggle = (ImageButton) view.findViewById(R.id.timer_start);
        final HorizontalWheelView wheel = (HorizontalWheelView) view.findViewById(R.id.timer_value);
        wheel.setOnlyPositiveValues(true);

        if (service().isThreadRun()) {
            int minutesStored = mPreferences.getSettingsPlayerStopMusic();
            int hours = minutesStored < 60 ? 0 : minutesStored / 60;
            toggle.setImageResource(R.drawable.ic_stop_black_18dp);
            toggle.setEnabled(false);
            if (minutesStored == 0) {
                service().setThreadPause(true);
                if (service().isThreadRun()) {
                    toggle.setImageResource(R.drawable.ic_play_black_18dp);
                }
                toggle.setEnabled(false);
                label.setText(getString(R.string.settings_player_close_app_disabled));
            } else {
                toggle.setEnabled(true);
                label.setText(timerLabel(hours, minutesStored));
            }
        } else {
            label.setText(getString(R.string.settings_player_close_app_disabled));
            toggle.setImageResource(R.drawable.ic_play_black_18dp);
            toggle.setEnabled(false);
        }

        wheel.setListener(new HorizontalWheelView.Listener() {
            @Override
            public void onRotationChanged(double radians) {
                int hours = (int) (wheel.getDegreesAngle() / 15.0d);
                int minutes = (int) (wheel.getDegreesAngle() * 4.0d);
                if (minutes < 60) {
                    minutes = (minutes / 10) * 10;      // Under an hour it moves in ten-minute steps.
                }
                if (hours == 0 && minutes == 0) {
                    service().setThreadPause(true);
                    if (service().isThreadRun()) {
                        toggle.setImageResource(R.drawable.ic_play_black_18dp);
                    }
                    toggle.setEnabled(false);
                    label.setText(getString(R.string.settings_player_close_app_disabled));
                    mPreferences.setSettingsPlayerStopMusic(0);
                    return;
                }
                toggle.setEnabled(true);
                label.setText(timerLabel(hours, minutes));
                mPreferences.setSettingsPlayerStopMusic(minutes < 60 ? minutes : hours * 60);
            }
        });
        toggle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressFeedback(v);
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.startAnimation(mPressUp);
                        if (!service().isThreadPause()) {
                            toggle.setImageResource(R.drawable.ic_play_black_18dp);
                            service().setThreadPause(true);
                            return true;
                        }
                        toggle.setImageResource(R.drawable.ic_stop_black_18dp);
                        service().setThreadPause(false);
                        if (!service().isThreadRun()) {
                            service().setThreadRun(true);
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    /** "20 minutes", "1 hour" or "3 hours" for the timer line. */
    private String timerLabel(int hours, int minutes) {
        if (minutes < 60) {
            return getString(R.string.settings_player_close_app_minutes, minutes);
        }
        if (hours == 1) {
            return getString(R.string.settings_player_close_app_hour, hours);
        }
        return getString(R.string.settings_player_close_app_hours, hours);
    }

    // -- Custom notification panel --

    private void bindCustomNotification(View view) {
        MaterialSwitch colored = (MaterialSwitch) view.findViewById(R.id.switch_notification_colored);
        final LinearLayout picker = (LinearLayout) view.findViewById(R.id.text_notification_colored);
        final ImageButton white = (ImageButton) view.findViewById(R.id.choose_notification_color_white);
        final ImageButton black = (ImageButton) view.findViewById(R.id.choose_notification_color_black);
        ServiceMusic service = service();

        colored.setChecked(mPreferences.getSettingsNotificationColored(!service.needWhiteNotification));
        white.setEnabled(!colored.isChecked());
        black.setEnabled(!colored.isChecked());
        picker.setAlpha(colored.isChecked() ? 0.25f : 1.0f);
        showTextColor(white, black, mPreferences.getSettingsNotificationWhite(service.needWhiteNotification));

        colored.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                buzz();
                mPreferences.setSettingsNotificationColored(checked);
                white.setEnabled(!checked);
                black.setEnabled(!checked);
                ObjectAnimator fade = checked
                        ? ObjectAnimator.ofFloat(picker, View.ALPHA, 1.0f, 0.25f)
                        : ObjectAnimator.ofFloat(picker, View.ALPHA, 0.25f, 1.0f);
                fade.setDuration(275L);
                fade.start();
            }
        });
        white.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buzz();
                mPreferences.setSettingsNotificationWhite(true);
                service().needWhiteNotification = true;
                showTextColor(white, black, true);
            }
        });
        black.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buzz();
                mPreferences.setSettingsNotificationWhite(false);
                service().needWhiteNotification = false;
                showTextColor(white, black, false);
            }
        });
    }

    private void showTextColor(ImageButton white, ImageButton black, boolean whiteSelected) {
        white.setBackgroundResource(whiteSelected ? R.drawable.xml_color_white_selection : R.drawable.xml_color_white);
        black.setBackgroundResource(whiteSelected ? R.drawable.xml_color_black : R.drawable.xml_color_black_selection);
    }

    // ---- Sub-panels (sleep timer, custom notification) --------------------------------------

    private void setupSubPanels(final View view) {
        mTimerPanel = (LinearLayout) view.findViewById(R.id.view_timer);
        mCustomPanel = (LinearLayout) view.findViewById(R.id.view_custom_notification);
        mSubPanel = view.findViewById(R.id.settings_sub);
        view.findViewById(R.id.os_timer).setOnClickListener(panelOpener(view, SECTION_TIMER));
        view.findViewById(R.id.os_custom_notification).setOnClickListener(panelOpener(view, SECTION_CUSTOM_NOTIFICATION));
    }

    private View.OnClickListener panelOpener(final View root, final int section) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buzz();
                sectionNumber = section;
                openSection(getContext());
                loadValues(root, section);
            }
        };
    }

    private LinearLayout openPanel() {
        return sectionNumber == SECTION_TIMER ? mTimerPanel : (sectionNumber == SECTION_CUSTOM_NOTIFICATION ? mCustomPanel : null);
    }

    public void openSection(Context context) {
        if (mInTransition) {
            return;
        }
        LinearLayout panel = openPanel();
        if (panel == null || mSubPanel == null) {
            return;
        }
        mInTransition = true;
        mActivity.setToolbarText(null, getString(sectionNumber == SECTION_TIMER ? R.string.settings_player_timer : R.string.settings_notification_custom));
        mActivity.setSettingSubSectionOpen(true);
        mActivity.setSettingSectionOpen(false);
        mSubPanel.setVisibility(View.VISIBLE);
        panel.setVisibility(View.VISIBLE);
        Animation slideIn = AnimationUtils.loadAnimation(context, R.anim.animation_settings_show_right);
        slideIn.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                mInTransition = false;
            }
        });
        panel.startAnimation(slideIn);
    }

    public void closeSection(Context context) {
        mActivity.setSettingSubSectionOpen(false);
        mActivity.setSettingSectionOpen(false);
        mActivity.setToolbarText(null, getString(R.string.settings_sub));
        final LinearLayout panel = openPanel();
        if (panel == null || mSubPanel == null) {
            sectionNumber = 0;
            mInTransition = false;
            return;
        }
        Animation slideOut = AnimationUtils.loadAnimation(context, R.anim.animation_settings_hide_left);
        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                panel.setVisibility(View.INVISIBLE);
                if (mSubPanel != null) {
                    mSubPanel.setVisibility(View.GONE);
                }
                sectionNumber = 0;
                mInTransition = false;
            }
        });
        panel.startAnimation(slideOut);
    }

    // ---- Scrolling gears and the jump menu --------------------------------------------------

    /** The cogs at the top drift up and fade as the page scrolls. */
    private void setupGearParallax() {
        mScroll.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (mGearsView == null) {
                    return;
                }
                float height = mGearsView.getHeight();
                mGearsView.setTranslationY(-0.35f * scrollY);
                mGearsView.setAlpha(height > 0 ? Math.max(0f, 1f - scrollY / (height * 0.85f)) : 1f);
            }
        });
    }

    /** The toolbar's right-hand button becomes a jump-to menu while Settings is open. */
    private void setupJump() {
        if (getActivity() == null) {
            return;
        }
        final ImageButton button = (ImageButton) getActivity().findViewById(R.id.toolbar_search);
        if (button == null) {
            return;
        }
        mJumpBtn = button;
        mJumpAction = new Runnable() {
            @Override
            public void run() {
                showJumpMenu(button);
            }
        };
        button.setImageResource(R.drawable.ic_jump_cream_24dp);
        button.setTag(R.id.toolbar_search, mJumpAction);
        button.setAlpha(1f);
        button.setVisibility(View.VISIBLE);
    }

    private void showJumpMenu(View anchor) {
        if (mRoot == null || getContext() == null) {
            return;
        }
        PopupMenu menu = new PopupMenu(getContext(), anchor);
        for (int i = 0; i < RAIL_LABELS.length; i++) {
            menu.getMenu().add(0, i + 1, i, RAIL_LABELS[i]);
        }
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int index = item.getItemId() - 1;
                if (mRailItems != null) {
                    selectRailSection(index);
                    return true;
                }
                View header = mRoot == null ? null : mRoot.findViewById(SECTION_HEADERS[index]);
                if (header != null && mScroll != null) {
                    mScroll.smoothScrollTo(0, Math.max(0, header.getTop() - mActivity.convertDpToPx(72)));
                }
                return true;
            }
        });
        menu.show();
    }

    // ---- Landscape rail ---------------------------------------------------------------------

    /** Held sideways: a section list on the left, and only the chosen section on the right. */
    private void setupLandscapeRail(View root) {
        if (getContext() == null || getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            return;
        }
        for (int header : SECTION_HEADERS) {
            View v = root.findViewById(header);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
        }
        View gears = root.findViewById(R.id.settings_gears);
        if (gears != null) {
            gears.setVisibility(View.GONE);
        }
        View content = root.findViewById(R.id.settings_content);
        content.setPadding(0, mActivity.convertDpToPx(64), 0, content.getPaddingBottom());

        int railWidth = mActivity.convertDpToPx(176);
        RelativeLayout.LayoutParams scrollParams = (RelativeLayout.LayoutParams) mScroll.getLayoutParams();
        scrollParams.setMarginStart(railWidth + mActivity.convertDpToPx(8));
        mScroll.setLayoutParams(scrollParams);

        LinearLayout rail = new LinearLayout(getContext());
        rail.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams railParams = new RelativeLayout.LayoutParams(railWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        railParams.topMargin = mActivity.convertDpToPx(72);
        railParams.setMarginStart(mActivity.convertDpToPx(16));

        mRailItems = new TextView[RAIL_VIEWS.length];
        for (int i = 0; i < RAIL_VIEWS.length; i++) {
            final int index = i;
            TextView item = new TextView(getContext());
            item.setText(RAIL_LABELS[i]);
            item.setTextSize(13f);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(mActivity.convertDpToPx(14), 0, mActivity.convertDpToPx(14), 0);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mActivity.convertDpToPx(40));
            itemParams.bottomMargin = mActivity.convertDpToPx(4);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectRailSection(index);
                }
            });
            rail.addView(item, itemParams);
            mRailItems[i] = item;
        }
        ((ViewGroup) root).addView(rail, railParams);
        selectRailSection(Math.min(sRailSection, RAIL_VIEWS.length - 1));
    }

    private void selectRailSection(int index) {
        if (mRoot == null || mRailItems == null) {
            return;
        }
        sRailSection = index;
        for (int i = 0; i < RAIL_VIEWS.length; i++) {
            View section = mRoot.findViewById(RAIL_VIEWS[i]);
            boolean selected = i == index;
            if (section != null) {
                section.setVisibility(selected ? View.VISIBLE : View.GONE);
            }
            TextView item = mRailItems[i];
            item.setTextColor(selected ? 0xFFFFFFFF : 0xB3FFFFFF);
            GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(mActivity.convertDpToPx(12));
            pill.setColor(selected ? 0x2EFFFFFF : 0x00000000);
            item.setBackground(pill);
        }
        if (mScroll != null) {
            mScroll.scrollTo(0, 0);
        }
    }

    // ---- Drive ------------------------------------------------------------------------------

    /** The Drive card: what the USB drive is doing, plus connect, rescan and folder choice. */
    private void setupDrive(View root) {
        final TextView status = (TextView) root.findViewById(R.id.drive_status);
        final UsbDrive drive = UsbDrive.get(getContext());
        final Context app = getContext();
        status.setText(drive.statusText());
        drive.setStatusListener(new UsbDrive.StatusListener() {
            @Override
            public void onStatus(String text) {
                status.setText(text);
            }
        });
        root.findViewById(R.id.drive_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drive.connect();
            }
        });
        root.findViewById(R.id.drive_rescan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drive.rescan();
            }
        });

        bindDriveSwitch(root, R.id.switch_drive_show, DrivePrefs.showSongs(app), new SwitchChange() {
            @Override
            public void onChange(boolean checked) {
                DrivePrefs.setShowSongs(app, checked);
                reloadLibrary();
            }
        });
        bindDriveSwitch(root, R.id.switch_drive_announce, DrivePrefs.announce(app), new SwitchChange() {
            @Override public void onChange(boolean checked) { DrivePrefs.setAnnounce(app, checked); }
        });
        bindDriveSwitch(root, R.id.switch_drive_pause, DrivePrefs.pauseOnUnplug(app), new SwitchChange() {
            @Override public void onChange(boolean checked) { DrivePrefs.setPauseOnUnplug(app, checked); }
        });

        root.findViewById(R.id.drive_folders).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!drive.isConnected()) {
                    if (drive.isMountedByAndroid() || !DriveFilter.topFolders(getContext()).isEmpty()) {
                        showMountedFolders();
                    } else {
                        Toast.makeText(getContext(), R.string.drive_folders_need_drive, Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                drive.loadTopFolders(new UsbDrive.FolderCallback() {
                    @Override
                    public void onFolders(List<String> names) {
                        if (getContext() != null && !names.isEmpty()) {
                            showDriveFolders(drive, names);
                        }
                    }
                });
            }
        });
    }

    /** Drive switches don't buzz (they never did), they only store the value. */
    private void bindDriveSwitch(View root, int id, boolean value, final SwitchChange change) {
        MaterialSwitch toggle = (MaterialSwitch) root.findViewById(id);
        toggle.setChecked(value);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                change.onChange(checked);
            }
        });
    }

    private void reloadLibrary() {
        if (mActivity instanceof Application_Main) {
            ((Application_Main) mActivity).reloadLibraryForDrives();
        }
    }

    /** Folder choice for a drive the app talks to directly: the ticked folders are scanned. */
    private void showDriveFolders(final UsbDrive drive, List<String> names) {
        final String[] items = names.toArray(new String[0]);
        Set<String> chosen = drive.getFolders();
        final boolean[] checked = new boolean[items.length];
        for (int i = 0; i < items.length; i++) {
            checked[i] = chosen.contains(items[i]);
        }
        folderDialog(items, checked, R.string.drive_folders_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Set<String> picked = new HashSet<>();
                for (int i = 0; i < items.length; i++) {
                    if (checked[i]) {
                        picked.add(items[i]);
                    }
                }
                drive.setFolders(picked);
                drive.rescan();
            }
        });
    }

    /** Folder choice for a drive that Android mounted: songs from the folders left unticked are hidden. */
    private void showMountedFolders() {
        if (getContext() == null) {
            return;
        }
        final List<String> entries = DriveFilter.topFolders(getContext());
        if (entries.isEmpty()) {
            Toast.makeText(getContext(), R.string.drive_folders_none, Toast.LENGTH_LONG).show();
            return;
        }
        Set<String> chosen = DriveFilter.chosen(getContext());
        final String[] items = new String[entries.size()];
        final boolean[] checked = new boolean[items.length];
        for (int i = 0; i < items.length; i++) {
            String entry = entries.get(i);
            items[i] = entry.substring(entry.indexOf('/') + 1);
            checked[i] = chosen.contains(entry);
        }
        folderDialog(items, checked, R.string.drive_folders_apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Set<String> picked = new HashSet<>();
                for (int i = 0; i < items.length; i++) {
                    if (checked[i]) {
                        picked.add(entries.get(i));
                    }
                }
                DriveFilter.save(getContext(), picked);
                reloadLibrary();
            }
        });
    }

    private void folderDialog(String[] items, final boolean[] checked, int positiveLabel, DialogInterface.OnClickListener onPositive) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.drive_folders_title)
                .setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setPositiveButton(positiveLabel, onPositive)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
