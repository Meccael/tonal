package com.meccael.tonal.application;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.meccael.tonal.R;
import com.meccael.tonal.adapters.AdapterMusicMoveable;
import com.meccael.tonal.adapters.SimpleItemTouchHelperCallback;
import com.meccael.tonal.bottom_menu.BottomDialog;
import com.meccael.tonal.manager.CurrentPlaylist;
import com.meccael.tonal.manager.Music;
import com.meccael.tonal.manager.Playlists;
import com.meccael.tonal.views.CoverView;
import com.meccael.tonal.views.FaddingRecyclerView;
import com.meccael.tonal.views.PlayPauseButton;
import com.meccael.tonal.views.ProgressView;
import com.meccael.tonal.views.RecordSpin;
import com.meccael.tonal.views.SquareHeightCardView;

/**
 * The queue screen: the song that is playing on a spinning record, its title and artist, and the
 * queue below it as a rail that can be reordered, trimmed and saved as a playlist.
 */
public class Fragment_Playlist extends Fragment {
    // Views other classes reach into (the progress handler, the drawer, the service).
    public static CoverView mCoverView;
    public static TextView mDurationView;
    public static PlayPauseButton mFabView;
    public static AdapterMusicMoveable mItemsAdapter;
    public static TextView mPlaylistDuration;
    public static FaddingRecyclerView mPlaylistItems;
    public static TextView mPlaylistTotalItem;
    public static ProgressView mProgressView;
    public static TextView mTimeView;

    private static final long NO_SONG_YET = -2;

    private View mRoot;
    private CallbacksInterface mActivity;
    private Preferences mPrefs;
    private Vibrator mVibrator;
    private Button mSave;
    private SquareHeightCardView mShadow;
    private TextView mNowTitle;
    private TextView mNowArtist;
    private RecordSpin mRecordSpin;
    private long mShownSongId = NO_SONG_YET;

    private final Handler mPoll = new Handler(Looper.getMainLooper());
    private final Runnable mPollTask = new Runnable() {
        @Override
        public void run() {
            pollNowPlaying();
            mPoll.postDelayed(this, 500L);
        }
    };

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
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        mRoot = view;
        mPrefs = new Preferences(getContext());
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        mTimeView = (TextView) view.findViewById(R.id.time);
        mDurationView = (TextView) view.findViewById(R.id.duration);
        mPlaylistTotalItem = (TextView) view.findViewById(R.id.playlist_count);
        mPlaylistDuration = (TextView) view.findViewById(R.id.playlist_duration);
        mProgressView = (ProgressView) view.findViewById(R.id.progress);
        mCoverView = (CoverView) view.findViewById(R.id.cover);
        mShadow = (SquareHeightCardView) view.findViewById(R.id.shadow);
        mPlaylistItems = (FaddingRecyclerView) view.findViewById(R.id.playlist_items);
        mSave = (Button) view.findViewById(R.id.action_playlist_save);
        mNowTitle = (TextView) view.findViewById(R.id.playlist_now_title);
        mNowArtist = (TextView) view.findViewById(R.id.playlist_now_artist);
        mFabView = (PlayPauseButton) view.findViewById(R.id.fab_playlist);
        mRecordSpin = new RecordSpin((ImageView) view.findViewById(R.id.playlist_record));

        int navBar = mActivity.getPaddingBottom(getContext());
        if (navBar > 0) {
            ViewGroup.MarginLayoutParams fab = (ViewGroup.MarginLayoutParams) mFabView.getLayoutParams();
            fab.setMargins(0, 0, 0, mActivity.convertDpToPx(16) + navBar);
            mFabView.setLayoutParams(fab);
            mPlaylistItems.setPadding(0, 0, 0, navBar);
        }
        if (mActivity.getServiceMusic() == null) {
            mActivity.setFragment(20);
        } else {
            loadDatas();
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        animateShadow();
        mPoll.removeCallbacks(mPollTask);
        mPoll.post(mPollTask);
        Application_Extend.getInstance().trackScreenView("Playlist");
    }

    @Override
    public void onPause() {
        super.onPause();
        mPoll.removeCallbacks(mPollTask);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Only clear the shared references if they still belong to this screen's views.
        if (mRoot != null && mTimeView != null && mTimeView == mRoot.findViewById(R.id.time)) {
            mCoverView = null;
            mDurationView = null;
            mFabView = null;
            mItemsAdapter = null;
            mPlaylistDuration = null;
            mPlaylistItems = null;
            mPlaylistTotalItem = null;
            mProgressView = null;
            mTimeView = null;
        }
        mRoot = null;
        mPoll.removeCallbacks(mPollTask);
        if (mRecordSpin != null) {
            mRecordSpin.cancel();
            mRecordSpin = null;
        }
        mNowTitle = null;
        mNowArtist = null;
        mShownSongId = NO_SONG_YET;
    }

    // ---- Loading ----------------------------------------------------------------------------

    public void loadDatas() {
        if (mPlaylistItems == null || mCoverView == null) {
            return;
        }
        ServiceMusic service = mActivity.getServiceMusic();
        mActivity.setFragment(20);
        mActivity.hideSearchIcon(true);
        mActivity.setToolbarText(getString(R.string.menu_playlist), service.playlistPlayed.getPlaylistName());
        mActivity.refreshNavigation(1);

        mItemsAdapter = new AdapterMusicMoveable(getContext(), service.playlistPlayed.getPlaylist(), this, service, mActivity.getSubtitleView());
        mPlaylistItems.setLayoutManager(new LinearLayoutManager(getActivity()));
        mPlaylistItems.setAdapter(mItemsAdapter);
        new ItemTouchHelper(new SimpleItemTouchHelperCallback(mItemsAdapter)).attachToRecyclerView(mPlaylistItems);
        refreshPlaylitsInformations();

        long cover = service.musicPlayed.getMusic(getContext()).getCover();
        mActivity.updateBackground(getContext(), cover, R.drawable.cover0, ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION);
        mCoverView.setImageBitmap(Utils.getCoverBitamp(getContext(), cover, R.drawable.cover0, mActivity.getCoverScaled()));
        mCoverView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPrefs.getSettingsUIVibration()) {
                    mVibrator.vibrate(5L);
                }
                openPlayer();
            }
        });
        mSave.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.playSoundEffect(0);
                        if (mPrefs.getSettingsUIVibration()) {
                            mVibrator.vibrate(5L);
                        }
                        view.setBackgroundResource(R.drawable.pl_pill_on);
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.setBackgroundResource(R.drawable.pl_pill);
                        showSaveSheet();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    public void refreshPlaylitsInformations() {
        CurrentPlaylist queue = mActivity.getServiceMusic().playlistPlayed;
        long size = queue.getPlaylistSize();
        long duration = queue.getPlaylistDuration();
        long minutes = (duration % 3600) / 60;
        if (duration / 60 < 60) {
            mPlaylistDuration.setText(getString(R.string.time_minute, minutes, duration % 60));
        } else {
            mPlaylistDuration.setText(getString(R.string.time_hour, duration / 3600, minutes));
        }
        mPlaylistTotalItem.setText(size > 1 ? getString(R.string.items_tracks, size) : getString(R.string.items_track, size));
    }

    // ---- Now playing ------------------------------------------------------------------------

    /** Twice a second: follows the playing song (title, artist, scroll the queue to it) and the spinning record. */
    private void pollNowPlaying() {
        if (mActivity == null || mActivity.getServiceMusic() == null || mPlaylistItems == null || mNowTitle == null) {
            return;
        }
        ServiceMusic service = mActivity.getServiceMusic();
        try {
            Music song = service.musicPlayed.getMusic(getContext());
            if (song != null && song.getID() != mShownSongId) {
                mShownSongId = song.getID();
                mNowTitle.setText(song.getTitle());
                mNowArtist.setText(song.getArtist());
                if (mItemsAdapter != null) {
                    mItemsAdapter.notifyDataSetChanged();
                }
                RecyclerView.LayoutManager layout = mPlaylistItems.getLayoutManager();
                if (layout instanceof LinearLayoutManager) {
                    int first = Math.max(0, service.musicPlayed.getPosition() - 2);
                    ((LinearLayoutManager) layout).scrollToPositionWithOffset(first, 0);
                }
            }
        } catch (Exception ignored) {
        }
        if (mRecordSpin != null) {
            mRecordSpin.sync(service.isPlaying());
        }
    }

    private void animateShadow() {
        mShadow.setCardElevation(0f);
        ValueAnimator grow = ValueAnimator.ofFloat(0f, 12f);
        grow.setDuration(250L);
        grow.setStartDelay(400L);
        grow.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mShadow.setCardElevation(mActivity.convertDpToPx((Float) animation.getAnimatedValue()));
            }
        });
        grow.start();
    }

    // ---- Saving the queue as a playlist -----------------------------------------------------

    private void showSaveSheet() {
        final BottomDialog sheet = new BottomDialog(getContext());
        sheet.title(getContext().getString(R.string.playlist_save));
        sheet.canceledOnTouchOutside(true);
        sheet.cancelable(true);
        sheet.inflateMenu(R.menu.menu_bottom_save_playlist);
        sheet.setOnItemSelectedListener(new BottomDialog.OnItemSelectedListener() {
            @Override
            public boolean onItemSelected(int itemId) {
                if (itemId == R.id.close) {
                    sheet.dismiss();
                } else if (itemId == R.id.save) {
                    askPlaylistName();
                }
                return true;
            }
        });
        sheet.show();
    }

    private void askPlaylistName() {
        final EditText input = new EditText(getActivity());
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                InputMethodManager keyboard = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.playlist_name)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        if (!name.isEmpty()) {
                            ServiceMusic service = mActivity.getServiceMusic();
                            // Playlist ids are "6" followed by how many saved files there already are, plus one.
                            int id = Integer.parseInt("6" + (getContext().getFilesDir().list().length + 1));
                            Playlists.save(getContext(), name, id, service, service.playlistPlayed.getPlaylist());
                        }
                    }
                })
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    // ---- Opening the Player -----------------------------------------------------------------

    private void openPlayer() {
        if (!mActivity.getServiceMusic().musicLaunched()) {
            mActivity.getServiceMusic().play(getContext(), true);
        }
        mActivity.setTransitionID(2015);

        Fragment_Player player = new Fragment_Player();
        Bundle args = new Bundle();
        args.putBoolean("allowplaylist", true);
        player.setArguments(args);

        TransitionInflater transitions = TransitionInflater.from(getActivity());
        setReenterTransition(transitions.inflateTransition(R.transition.transition_playlist_to_player_back));
        setExitTransition(transitions.inflateTransition(R.transition.transition_playlist_to_player));
        player.setSharedElementEnterTransition(transitions.inflateTransition(R.transition.transition_shared_element_enter));
        player.setSharedElementReturnTransition(transitions.inflateTransition(R.transition.transition_shared_element_return));
        getActivity().getSupportFragmentManager().beginTransaction()
                .addSharedElement(mTimeView, ViewCompat.getTransitionName(mTimeView))
                .addSharedElement(mDurationView, ViewCompat.getTransitionName(mDurationView))
                .addSharedElement(mProgressView, ViewCompat.getTransitionName(mProgressView))
                .addSharedElement(mCoverView, ViewCompat.getTransitionName(mCoverView))
                .addSharedElement(mFabView, ViewCompat.getTransitionName(mFabView))
                .replace(R.id.container_body, player)
                .addToBackStack(null)
                .commit();
    }
}
