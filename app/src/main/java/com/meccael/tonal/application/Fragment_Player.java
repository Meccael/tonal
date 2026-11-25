package com.meccael.tonal.application;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import java.util.List;
import com.meccael.tonal.R;
import com.meccael.tonal.bottom_menu.BottomDialog;
import com.meccael.tonal.lyrics.LyricLine;
import com.meccael.tonal.lyrics.LyricsLoader;
import com.meccael.tonal.lyrics.LyricsTimingDialog;
import com.meccael.tonal.lyrics.LyricsTimingStore;
import com.meccael.tonal.lyrics.LyricsView;
import com.meccael.tonal.manager.Music;
import com.meccael.tonal.views.CoverView;
import com.meccael.tonal.views.PlayPauseButton;
import com.meccael.tonal.views.ProgressView;

/**
 * The Player: spinning cover, transport buttons, progress and live lyrics.
 */
public class Fragment_Player extends Fragment {
    // Views other classes reach into (progress handler, drawer, service, main activity).
    public static CoverView mCoverView;
    public static TextView mDurationView;
    public static PlayPauseButton mFabView;
    public static ProgressView mProgressView;
    public static TextView mTimeView;

    // Package-private: Application_Main animates these during the closing transition.
    RelativeLayout mControls;
    ImageView mCoverHoverView;
    ImageButton pAdd;
    ImageButton pLoop;
    ImageButton pNext;
    Button pOpen;
    ImageButton pPrevious;

    private static final int SEEK_STEP_MS = 30000;
    private static final int TRANSITION_PLAYER_TO_QUEUE = 1520;
    private static final int LOOP_SHUFFLE = 0;
    private static final int LOOP_ALL = 1;
    private static final int LOOP_ONE = 2;

    private View rootView;
    private CallbacksInterface mActivity;
    private Preferences mPreferences;
    private Vibrator mVibrate;
    private boolean showPlaylistShortcut = true;

    // Live lyrics.
    private LyricsView mLyricsView;
    private LyricsLoader mLyricsLoader;
    private long mLyricsTrackId = -1;
    private int mLyricsTicks = 0;
    private final Handler mLyricsHandler = new Handler(Looper.getMainLooper());
    private final Runnable mLyricsTick = new Runnable() {
        @Override
        public void run() {
            updateLyrics();
            mLyricsHandler.postDelayed(this, 100L);
        }
    };

    // ---- Small helpers ----------------------------------------------------------------------

    private ServiceMusic service() {
        return mActivity != null ? mActivity.getServiceMusic() : null;
    }

    private void buzz() {
        if (mPreferences != null && mPreferences.getSettingsUIVibration() && mVibrate != null) {
            mVibrate.vibrate(5L);
        }
    }

    private Music currentSong() {
        ServiceMusic service = service();
        return service != null && service.musicPlayed != null ? service.musicPlayed.getMusic(getContext()) : null;
    }

    private void setWidth(View view, int width) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        view.setLayoutParams(params);
    }

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
        mPreferences = new Preferences(getContext());
        mVibrate = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        Log.i("TONAL", "Player opened with XML.");
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        rootView = view;
        Bundle args = getArguments();
        if (args != null) {
            showPlaylistShortcut = args.getBoolean("allowplaylist");
        }

        mTimeView = (TextView) view.findViewById(R.id.time);
        mDurationView = (TextView) view.findViewById(R.id.duration);
        mProgressView = (ProgressView) view.findViewById(R.id.progress);
        mCoverHoverView = (ImageView) view.findViewById(R.id.cover_hover_round);
        mCoverView = (CoverView) view.findViewById(R.id.cover);
        mControls = (RelativeLayout) view.findViewById(R.id.controls);
        pAdd = (ImageButton) view.findViewById(R.id.action_favorite);
        pLoop = (ImageButton) view.findViewById(R.id.action_repeat);
        pPrevious = (ImageButton) view.findViewById(R.id.action_previous);
        pNext = (ImageButton) view.findViewById(R.id.action_next);
        pOpen = (Button) view.findViewById(R.id.action_playlist_open);
        mFabView = (PlayPauseButton) view.findViewById(R.id.fab_player);

        setupLyrics(view);
        wireCoverClose();
        wireQueueButton();
        wireSkipButtons();
        wireLoopButton();
        wireFavoriteButton();
        showLoopIcon();
        fadeInCoverRing();

        if (mActivity != null) {
            if (mActivity.getServiceMusic() == null) {
                mActivity.setFragment(15);
            } else {
                loadDatas();
            }
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startLyricsTicker();
        Application_Extend.getInstance().trackScreenView("Player");
        if (service() != null) {
            loadDatas();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mLyricsHandler.removeCallbacks(mLyricsTick);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLyricsHandler.removeCallbacks(mLyricsTick);
        if (mLyricsLoader != null) {
            mLyricsLoader.cancel();
        }
        mLyricsView = null;
        // Clear the shared references only if they still belong to this screen's own views.
        if (rootView != null && mTimeView != null && mTimeView == rootView.findViewById(R.id.time)) {
            mCoverView = null;
            mDurationView = null;
            mFabView = null;
            mProgressView = null;
            mTimeView = null;
        }
        rootView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLyricsLoader != null) {
            mLyricsLoader.shutdown();
            mLyricsLoader = null;
        }
    }

    // ---- Loading ----------------------------------------------------------------------------

    public void loadDatas() {
        if (mActivity == null) {
            return;
        }
        mActivity.setFragment(15);
        mActivity.hideSearchIcon(true);
        ServiceMusic service = service();
        if (service != null && service.musicPlayed != null) {
            Music song = service.musicPlayed.getMusic(getContext());
            mActivity.setToolbarText(song.getTitle(), song.getArtist());
            if (mPreferences != null && pAdd != null) {
                pAdd.setAlpha(mPreferences.isFavorite(song.getID()) ? 1.0f : 0.5f);
            }
        }
        mActivity.refreshPlayerVinyl(getContext());
        if (mCoverView != null) {
            mCoverView.setImageBitmap(mActivity.getThemeArt());
        }
    }

    // ---- Opening and closing ----------------------------------------------------------------

    public void closePlayer() {
        if (getActivity() != null && getActivity().getSupportFragmentManager() != null) {
            if (mActivity != null) {
                mActivity.showSearchIcon(true);
            }
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    public void openCurrentPlaylist() {
        ServiceMusic service = service();
        if (service == null || !service.musicLaunched() || service.playlistPlayed.getPlaylistSize() <= 0) {
            return;
        }
        mActivity.setTransitionID(TRANSITION_PLAYER_TO_QUEUE);
        if (getActivity() != null && getActivity().getSupportFragmentManager() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container_body, new Fragment_Playlist())
                    .addToBackStack(null)
                    .commit();
        }
    }

    /** The "Playlist" pill grows from a small button to its full width. */
    public void openPlaylistButton() {
        if (pOpen == null || mActivity == null) {
            return;
        }
        ValueAnimator grow = ValueAnimator.ofInt(mActivity.convertDpToPx(32), pOpen.getWidth());
        grow.setDuration(300L).setStartDelay(700L);
        grow.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (pOpen != null) {
                    setWidth(pOpen, (Integer) animation.getAnimatedValue());
                }
            }
        });
        grow.start();
        setWidth(pOpen, mActivity.convertDpToPx(32));
    }

    /** The "Playlist" pill shrinks and then drops out of view. */
    public void closePlaylistButton() {
        if (pOpen == null || mActivity == null) {
            return;
        }
        ValueAnimator shrink = ValueAnimator.ofInt(mActivity.convertDpToPx(100), mActivity.convertDpToPx(32));
        shrink.setDuration(200L);
        shrink.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (pOpen != null) {
                    setWidth(pOpen, (Integer) animation.getAnimatedValue());
                }
            }
        });
        shrink.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (pOpen != null) {
                    TranslateAnimation drop = new TranslateAnimation(0f, 0f, 0f, pOpen.getHeight() * 3.0f);
                    drop.setDuration(100L);
                    drop.setFillAfter(true);
                    pOpen.startAnimation(drop);
                }
            }
        });
        shrink.start();
    }

    /** The soft ring around the cover fades in once the opening transition has settled. */
    private void fadeInCoverRing() {
        if (mCoverHoverView == null) {
            return;
        }
        ValueAnimator fade = ValueAnimator.ofFloat(0f, 1f);
        fade.setDuration(800L);
        fade.setStartDelay(350L);
        fade.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCoverHoverView != null) {
                    mCoverHoverView.setAlpha((Float) animation.getAnimatedValue());
                }
            }
        });
        fade.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (mCoverHoverView != null) {
                    mCoverHoverView.setVisibility(View.VISIBLE);
                }
            }
        });
        fade.start();
    }

    // ---- Buttons ----------------------------------------------------------------------------

    /** Tapping the cover ring closes the Player. */
    private void wireCoverClose() {
        if (mCoverHoverView == null) {
            return;
        }
        mCoverHoverView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.playSoundEffect(SoundEffectConstants.CLICK);
                        buzz();
                        return true;
                    case MotionEvent.ACTION_UP:
                        closePlayer();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void wireQueueButton() {
        if (pOpen == null) {
            return;
        }
        pOpen.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setBackgroundResource(R.drawable.pl_pill_on);
                        view.playSoundEffect(SoundEffectConstants.CLICK);
                        buzz();
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.setBackgroundResource(R.drawable.pl_pill);
                        openCurrentPlaylist();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    /** Previous/Next skip on tap and jump 30 seconds on a long press. */
    private void wireSkipButtons() {
        if (pPrevious != null) {
            pPrevious.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mActivity == null) {
                        return;
                    }
                    mActivity.setPressNext(true);
                    buzz();
                    if (service() != null) {
                        service().previous(getContext());
                    }
                }
            });
            pPrevious.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ServiceMusic service = service();
                    if (service != null && service.MusicPlayer != null) {
                        int position = service.MusicPlayer.getCurrentPosition();
                        if ((position - SEEK_STEP_MS) - 100 > 0) {
                            service.MusicPlayer.seekTo(position - SEEK_STEP_MS);
                        }
                    }
                    return true;
                }
            });
        }
        if (pNext != null) {
            pNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mActivity == null) {
                        return;
                    }
                    mActivity.setPressNext(true);
                    buzz();
                    if (service() != null) {
                        service().next(getContext());
                    }
                }
            });
            pNext.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ServiceMusic service = service();
                    if (service != null && service.MusicPlayer != null) {
                        int position = service.MusicPlayer.getCurrentPosition();
                        if (position + SEEK_STEP_MS + 100 < service.MusicPlayer.getDuration()) {
                            service.MusicPlayer.seekTo(position + SEEK_STEP_MS);
                        }
                    }
                    return true;
                }
            });
        }
    }

    private void showLoopIcon() {
        if (pLoop == null || mPreferences == null) {
            return;
        }
        switch (mPreferences.getPlayerLoop()) {
            case LOOP_ALL:
                pLoop.setImageResource(R.drawable.ic_repeat_all_white_24dp);
                break;
            case LOOP_ONE:
                pLoop.setImageResource(R.drawable.ic_repeat_one_white_24dp);
                break;
            case LOOP_SHUFFLE:
                pLoop.setImageResource(R.drawable.ic_shuffle_all_white_24dp);
                break;
            default:
                break;
        }
    }

    /** Shuffle, then repeat all, then repeat one, then back to shuffle. */
    private void wireLoopButton() {
        if (pLoop == null) {
            return;
        }
        pLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buzz();
                if (mPreferences == null) {
                    return;
                }
                int next;
                int message;
                switch (mPreferences.getPlayerLoop()) {
                    case LOOP_SHUFFLE:
                        next = LOOP_ALL;
                        message = R.string.musics_loop;
                        break;
                    case LOOP_ALL:
                        next = LOOP_ONE;
                        message = R.string.musics_repeat;
                        break;
                    case LOOP_ONE:
                        next = LOOP_SHUFFLE;
                        message = R.string.musics_shuffle;
                        break;
                    default:
                        return;
                }
                Toast.makeText(getContext(), getString(message), Toast.LENGTH_SHORT).show();
                mPreferences.setPlayerLoop(next);
                showLoopIcon();
            }
        });
    }

    /** Tap toggles the favorite heart; long press opens the song menu. */
    private void wireFavoriteButton() {
        if (pAdd == null) {
            return;
        }
        pAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buzz();
                Music song = currentSong();
                if (song == null || mPreferences == null) {
                    return;
                }
                mPreferences.toggleFavorite(song.getID());
                boolean favorite = mPreferences.isFavorite(song.getID());
                pAdd.setAlpha(favorite ? 1.0f : 0.5f);
                Toast.makeText(getContext(), favorite ? "Added to Favorites" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
            }
        });
        pAdd.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                buzz();
                if (getActivity() != null) {
                    showSongMenu();
                }
                return true;
            }
        });
    }

    private void showSongMenu() {
        BottomDialog menu = new BottomDialog(getActivity());
        menu.title(R.string.menu_player);
        menu.canceledOnTouchOutside(true);
        menu.cancelable(true);
        menu.inflateMenu(R.menu.menu_bottom_player);
        menu.setOnItemSelectedListener(new BottomDialog.OnItemSelectedListener() {
            @Override
            public boolean onItemSelected(int itemId) {
                if (itemId == R.id.current_song_details) {
                    if (service() != null && service().musicPlayed != null) {
                        new InformationsWindow().show(getContext(), currentSong());
                    }
                } else if (itemId == R.id.lyrics_timing) {
                    showLyricsTiming();
                } else if (itemId == R.id.sleep_timer) {
                    showSleepTimer();
                } else if (itemId == R.id.current_playlist_remove) {
                    removeCurrentFromQueue();
                }
                return true;
            }
        });
        menu.show();
    }

    private void removeCurrentFromQueue() {
        ServiceMusic service = service();
        if (service != null && service.playlistPlayed != null && service.playlistPlayed.getPlaylistSize() > 1) {
            service.playlistPlayed.removeSong(service.musicPlayed.getPosition(), new Fragment_Playlist());
            service.playlistPlayed.setPlaylistName(getContext().getString(R.string.playlist_temp), mActivity.getTitleView());
        }
    }

    private void showLyricsTiming() {
        if (getActivity() == null || mLyricsView == null || mLyricsTrackId < 0) {
            Toast.makeText(getContext(), R.string.lyrics_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        LyricsTimingDialog.show(getActivity(), mLyricsTrackId, mLyricsView);
    }

    private void showSleepTimer() {
        if (getActivity() == null || service() == null) {
            return;
        }
        SleepTimerDialog.show(getActivity(), service(), mPreferences != null ? mPreferences : new Preferences(getActivity()));
    }

    // ---- Live lyrics ------------------------------------------------------------------------

    private void setupLyrics(View view) {
        View candidate = view.findViewById(R.id.lyrics);
        mLyricsView = (candidate instanceof LyricsView) ? (LyricsView) candidate : null;
        mLyricsTrackId = -1;
        if (mLyricsView != null) {
            mLyricsView.setOnLineClickListener(new LyricsView.OnLineClickListener() {
                @Override
                public void onLineClick(long timeMs) {
                    if (mActivity == null) {
                        return;
                    }
                    try {
                        buzz();
                        mActivity.seekTo((int) Math.max(0L, timeMs));
                    } catch (Exception ignored) {
                        // The player was released between taps.
                    }
                }
            });
        }
        if (mLyricsLoader == null && getContext() != null) {
            mLyricsLoader = new LyricsLoader(getContext());
        }
        if (mLyricsView != null && getContext() != null && !new Preferences(getContext()).getSettingsLyricsShow()) {
            mLyricsView.setVisibility(View.GONE);
        }
    }

    private void startLyricsTicker() {
        mLyricsHandler.removeCallbacks(mLyricsTick);
        if (mLyricsView == null) {
            return;
        }
        // Re-read the setting each time the screen returns, so a change in Settings applies at once.
        boolean show = getContext() == null || new Preferences(getContext()).getSettingsLyricsShow();
        boolean wasVisible = mLyricsView.getVisibility() == View.VISIBLE;
        mLyricsView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            if (!wasVisible) {
                mLyricsTrackId = -1;   // Turned back on: look the current song up again.
            }
            mLyricsHandler.post(mLyricsTick);
        } else {
            if (mLyricsLoader != null) {
                mLyricsLoader.cancel();
            }
            mLyricsView.clear();
        }
    }

    /** Runs every 100 ms while the Player is visible. */
    private void updateLyrics() {
        ServiceMusic service = service();
        if (mLyricsView == null || service == null || service.MusicPlayer == null) {
            return;
        }
        try {
            // Look for a new song about twice a second; update the position every tick.
            if (mLyricsTicks++ % 5 == 0 && service.musicPlayed != null) {
                Music song = service.musicPlayed.getMusic(getContext());
                if (song != null && song.getID() != mLyricsTrackId) {
                    mLyricsTrackId = song.getID();
                    startLyricsLoad(song);
                }
            }
            mLyricsView.setPositionMs(service.MusicPlayer.getCurrentPosition());
        } catch (Exception ignored) {
            // The player can be between songs or released; try again on the next tick.
        }
    }

    private void startLyricsLoad(Music song) {
        if (mLyricsLoader == null || mLyricsView == null) {
            return;
        }
        mLyricsView.clear();
        mLyricsView.setDelayMs(LyricsTimingStore.get(getContext(), song.getID()));
        mLyricsLoader.load(song.getFile(), song.getTitle(), song.getArtist(), song.getAlbum(),
                song.getDuration(), new LyricsLoader.Callback() {
                    @Override
                    public void onSearching() {
                        if (mLyricsView != null) {
                            mLyricsView.showMessage("Searching lyrics…");
                        }
                    }

                    @Override
                    public void onLyrics(List<LyricLine> lines, String source) {
                        if (mLyricsView != null) {
                            mLyricsView.setLines(lines);
                        }
                    }

                    @Override
                    public void onNone() {
                        if (mLyricsView != null) {
                            mLyricsView.showMessage("No lyrics found");
                        }
                    }
                });
    }
}
