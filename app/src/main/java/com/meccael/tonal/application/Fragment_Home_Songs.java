package com.meccael.tonal.application;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.meccael.tonal.R;
import com.meccael.tonal.adapters.AdapterMusicArt;
import com.meccael.tonal.manager.Music;
import com.meccael.tonal.manager.Playlist;
import com.meccael.tonal.scrollbar.IndicatorAlphabet;
import com.meccael.tonal.scrollbar.IndicatorDate;
import com.meccael.tonal.scrollbar.TouchScrollBar;
import com.meccael.tonal.views.CoverView;
import com.meccael.tonal.views.PlayPauseButton;
import com.meccael.tonal.views.ProgressView;
import com.meccael.tonal.views.RecordSpin;

/**
 * The song list of one album, artist, genre, folder, or of All Music: a cover, a mini player strip, a
 * sort button and the list itself with a fast-scroll bar.
 */
public class Fragment_Home_Songs extends Fragment {
    // Views other classes reach into (the progress handler, the drawer, the service).
    public static CoverView mCoverView;
    public static TextView mDurationView;
    public static PlayPauseButton mFabView;
    public static AdapterMusicArt mItemsAdapter;
    public static RecyclerView mPlaylistItems;
    public static ProgressView mProgressView;
    public static TouchScrollBar mScrollBar;
    public static TextView mTimeView;

    private static final String SORT_TITLE = "title";
    private static final String SORT_ARTIST = "artist";
    private static final String SORT_DATE = "date_modified";

    private View mRoot;
    private CallbacksInterface mActivity;
    private Preferences mPrefs;
    private Vibrator mVibrator;
    private Animation mPressDown;
    private Animation mPressUp;
    private Animation mSpinOnce;

    private TextView mHeaderTitle;
    private TextView mHeaderInfo;
    private Button mSortOrder;
    private ImageButton mSortSwap;
    private RecordSpin mRecordSpin;

    private String mPlaylistName;
    private int mCategory = 0;
    private int mPosition = 0;
    private int mPlaylistId = 0;

    private final Handler mPoll = new Handler(Looper.getMainLooper());
    private final Runnable mPollTask = new Runnable() {
        @Override
        public void run() {
            if (mRecordSpin != null && mActivity != null && mActivity.getServiceMusic() != null) {
                mRecordSpin.sync(mActivity.getServiceMusic().isPlaying());
            }
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
        View view = inflater.inflate(R.layout.fragment_home_songs_all, container, false);
        mRoot = view;
        Bundle args = getArguments();
        if (args != null) {
            mPlaylistName = args.getString("playlist");
            mCategory = args.getInt("category");
            mPosition = args.getInt("position");
        }
        Log.i("TONAL", "Details opened.");

        mPrefs = new Preferences(getContext());
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        mPressDown = AnimationUtils.loadAnimation(getContext(), R.anim.animation_bouton_down);
        mPressUp = AnimationUtils.loadAnimation(getContext(), R.anim.animation_bouton_up);
        mSpinOnce = AnimationUtils.loadAnimation(getContext(), R.anim.animation_bouton_up_rotation);

        mCoverView = (CoverView) view.findViewById(R.id.cover);
        mTimeView = (TextView) view.findViewById(R.id.time);
        mDurationView = (TextView) view.findViewById(R.id.duration);
        mProgressView = (ProgressView) view.findViewById(R.id.progress);
        mFabView = (PlayPauseButton) view.findViewById(R.id.fab);
        mHeaderTitle = (TextView) view.findViewById(R.id.playlist_title);
        mHeaderInfo = (TextView) view.findViewById(R.id.playlist_details);
        mSortOrder = (Button) view.findViewById(R.id.playlist_items_order);
        mSortSwap = (ImageButton) view.findViewById(R.id.playlist_items_swap);
        mPlaylistItems = (RecyclerView) view.findViewById(R.id.list_songs);
        mScrollBar = (TouchScrollBar) view.findViewById(R.id.fastscroller);
        mRecordSpin = new RecordSpin((ImageView) view.findViewById(R.id.songs_record));

        int navBar = mActivity.getPaddingBottom(getContext());
        if (navBar > 0) {
            ViewGroup.MarginLayoutParams fab = (ViewGroup.MarginLayoutParams) mFabView.getLayoutParams();
            fab.setMargins(0, 0, 0, mActivity.convertDpToPx(16) + navBar);
            mFabView.setLayoutParams(fab);
            mPlaylistItems.setPadding(0, 0, 0, navBar);
        }
        if (mCategory != 0) {
            // Only All Music can be re-sorted.
            mSortOrder.setVisibility(View.GONE);
            mSortSwap.setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);
        if (mActivity.getServiceMusic() == null) {
            mActivity.setFragment(12);
        } else {
            loadDatas();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mPoll.removeCallbacks(mPollTask);
        mPoll.post(mPollTask);
        Application_Extend.getInstance().trackScreenView(mCategory == 0 ? "All musics" : "Songs");
    }

    @Override
    public void onPause() {
        super.onPause();
        mPoll.removeCallbacks(mPollTask);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Only clear the shared references if they still belong to this screen's views;
        // a newer Songs screen may already have replaced them.
        if (mRoot != null && mTimeView != null && mTimeView == mRoot.findViewById(R.id.time)) {
            mCoverView = null;
            mDurationView = null;
            mFabView = null;
            mItemsAdapter = null;
            mPlaylistItems = null;
            mProgressView = null;
            mScrollBar = null;
            mTimeView = null;
        }
        mRoot = null;
        mPoll.removeCallbacks(mPollTask);
        if (mRecordSpin != null) {
            mRecordSpin.cancel();
            mRecordSpin = null;
        }
    }

    // ---- Loading ----------------------------------------------------------------------------

    public int getPlaylistID() {
        return mPlaylistId;
    }

    private Playlist source() {
        return mActivity.getServiceMusic().getCategory(getContext(), mCategory).getPlaylists().get(mPosition);
    }

    public void loadDatas() {
        mActivity.setFragment(12);
        mActivity.showSearchIcon(true);
        mActivity.setToolbarText(getString(R.string.menu_main), mPlaylistName);
        mActivity.refreshNavigation(0);

        mItemsAdapter = new AdapterMusicArt(getContext(), source().getPlaylistMusics(), mCategory, mPosition,
                mActivity.getServiceMusic(), mActivity.getTitleView());
        mPlaylistId = (int) source().getPlaylistID();
        mPlaylistItems.setAdapter(mItemsAdapter);
        mPlaylistItems.setLayoutManager(new LinearLayoutManager(getActivity()));
        mScrollBar.addIndicator(new IndicatorAlphabet(getContext())).setBarColour(Color.parseColor("#26FFFFFF"));

        refreshTheme(mCategory);
        showSortLabel(mSortOrder);
        refreshPlaylitsInformations();

        // The saved sort is shown on the button first; then the saved state moves on to the "next"
        // order so that the first press of each button continues from there.
        flipSortDirection();
        mPrefs.setItemsSortBy(nextSortKey(mPrefs.getItemsSortBy()));

        wireCover();
        wireSortDirectionButton();
        wireSortKeyButton();
    }

    private void wireCover() {
        mCoverView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.playSoundEffect(0);
                        buzz();
                        return true;
                    case MotionEvent.ACTION_UP:
                        openPlayer();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void wireSortDirectionButton() {
        mSortSwap.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.startAnimation(mPressDown);
                        view.playSoundEffect(0);
                        buzz();
                        flipSortDirection();
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.startAnimation(mSpinOnce);
                        sortMusic(mPrefs, mItemsAdapter.getMusics());
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void wireSortKeyButton() {
        mSortOrder.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.startAnimation(mPressDown);
                        view.playSoundEffect(0);
                        buzz();
                        mPrefs.setItemsSortBy(nextSortKey(mPrefs.getItemsSortBy()));
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.startAnimation(mPressUp);
                        showSortLabel(mSortOrder);
                        sortMusic(mPrefs, mItemsAdapter.getMusics());
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void buzz() {
        if (mPrefs.getSettingsUIVibration()) {
            mVibrator.vibrate(5L);
        }
    }

    // ---- Sorting ----------------------------------------------------------------------------

    private void flipSortDirection() {
        mPrefs.setItemsSortAsc(!mPrefs.getItemsSortAsc());
    }

    /** title -> artist -> date -> title. */
    private static String nextSortKey(String current) {
        if (SORT_TITLE.equals(current)) {
            return SORT_ARTIST;
        }
        if (SORT_ARTIST.equals(current)) {
            return SORT_DATE;
        }
        if (SORT_DATE.equals(current)) {
            return SORT_TITLE;
        }
        return current;
    }

    /** Writes the current sort name on the button and switches the fast-scroll bar to letters or years. */
    private void showSortLabel(Button button) {
        String key = mPrefs.getItemsSortBy();
        if (SORT_TITLE.equals(key)) {
            mScrollBar.addIndicator(new IndicatorAlphabet(getContext()));
            button.setText(getString(R.string.song_title));
        } else if (SORT_ARTIST.equals(key)) {
            mScrollBar.addIndicator(new IndicatorAlphabet(getContext()));
            button.setText(getString(R.string.song_artist));
        } else if (SORT_DATE.equals(key)) {
            mScrollBar.addIndicator(new IndicatorDate(getContext()));
            button.setText(getString(R.string.song_modified));
        }
    }

    private void sortMusic(final Preferences prefs, List<Music> songs) {
        Collections.sort(songs, new Comparator<Music>() {
            @Override
            public int compare(Music a, Music b) {
                int direction = prefs.getItemsSortAsc() ? 1 : -1;
                String key = prefs.getItemsSortBy();
                if (SORT_TITLE.equals(key)) {
                    return direction * b.getTitle().compareToIgnoreCase(a.getTitle());
                }
                if (SORT_ARTIST.equals(key)) {
                    return direction * b.getArtist().compareToIgnoreCase(a.getArtist());
                }
                if (!SORT_DATE.equals(key)) {
                    return 1;
                }
                if (b.getDate() > a.getDate()) {
                    return -direction;
                }
                return b.getDate() >= a.getDate() ? 0 : direction;
            }
        });
        mItemsAdapter.notifyDataSetChanged();
    }

    // ---- Header and theme -------------------------------------------------------------------

    public void refreshPlaylitsInformations() {
        Playlist playlist = source();
        long size = playlist.getPlaylistSize();
        long duration = playlist.getPlaylistDuration();
        long minutes = (duration % 3600) / 60;
        String time = duration / 60 < 60
                ? getString(R.string.time_minute, minutes, duration % 60)
                : getString(R.string.time_hour, duration / 3600, minutes);
        String count = size > 1 ? getString(R.string.items_tracks, size) : getString(R.string.items_track, size);
        mHeaderTitle.setText(mPlaylistName);
        mHeaderInfo.setText(count + " (" + time + ")");
    }

    public void refreshTheme(int category) {
        if (mCoverView == null) {
            return;
        }
        int fade = ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION;
        if (category != 0) {
            long cover = mActivity.getServiceMusic().getCategory(getContext(), category).getPlaylists().get(mPosition).getPlaylistCover();
            mCoverView.setImageBitmap(Utils.getCoverBitamp(getContext(), cover, R.drawable.cover0, mActivity.getCoverScaled()));
            mActivity.updateBackground(getContext(), (int) cover, R.drawable.cover0, fade);
        } else {
            mCoverView.setImageBitmap(mActivity.getThemeArt());
            mActivity.updateBackground(getContext(), mActivity.getServiceMusic().musicPlayed.getMusic(getContext()).getCover(), R.drawable.cover0, fade);
        }
    }

    // ---- Opening the Player -----------------------------------------------------------------

    private void openPlayer() {
        if (!mActivity.getServiceMusic().musicLaunched()) {
            mActivity.getServiceMusic().play(getContext(), true);
        }
        mActivity.setTransitionID(1215);
        mScrollBar.setVisibility(View.INVISIBLE);

        Fragment_Player player = new Fragment_Player();
        Bundle args = new Bundle();
        args.putBoolean("allowplaylist", true);
        player.setArguments(args);

        TransitionInflater transitions = TransitionInflater.from(getActivity());
        setReenterTransition(transitions.inflateTransition(R.transition.transition_songs_to_player));
        setExitTransition(transitions.inflateTransition(R.transition.transition_songs_to_player));
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
