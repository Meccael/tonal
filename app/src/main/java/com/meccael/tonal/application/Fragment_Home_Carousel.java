package com.meccael.tonal.application;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.view.PointerIconCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import com.meccael.tonal.R;
import com.meccael.tonal.adapters.AdapterCardRecent;
import com.meccael.tonal.adapters.AdapterCarousel;
import com.meccael.tonal.carousel.HorizontalInfiniteCycleViewPager;
import com.meccael.tonal.compose.HomeCarouselHostKt;
import com.meccael.tonal.manager.Category;
import com.meccael.tonal.manager.Music;
import com.meccael.tonal.manager.Playlist;
import com.meccael.tonal.views.PlayPauseButton;
import com.meccael.tonal.views.ProgressView;

/**
 * The Home screen. The visible UI is drawn by Compose (see HomeCarouselHostKt); this fragment owns
 * the screen's lifecycle, keeps the legacy adapter/view references the rest of the app still pokes
 * at, and runs the transitions into the Songs, Playlists and Player screens.
 */
public class Fragment_Home_Carousel extends Fragment {
    // References other classes use (service refresh, drawer, adapters, main activity).
    public static AdapterCarousel mCarouselAdapter;
    public static HorizontalInfiniteCycleViewPager mCarouselList;
    public static TextView mDurationView;
    public static PlayPauseButton mFabView;
    public static ProgressView mProgressView;
    public static AdapterCardRecent mRecentsAdapter;
    public static RecyclerView mRecentsList;
    public static TextView mTimeView;

    private static final int PROGRESS_SLIDE_DP = 168;
    private static final int PROGRESS_REST_DP = 206;
    // Transition ids Application_Main understands (the values come from the old PointerIconCompat constants).
    private static final int TRANSITION_HOME_TO_SONGS = PointerIconCompat.TYPE_NO_DROP;
    private static final int TRANSITION_HOME_TO_PLAYER = PointerIconCompat.TYPE_VERTICAL_DOUBLE_ARROW;

    public int lastcardPosition = 0;

    private CallbacksInterface mActivity;
    private Preferences mPreferences;
    private Vibrator mVibrate;
    private ImageView mCover;
    private Button mPlayer;
    private LinearLayout mProgression;
    private LinearLayout mRecentLayout;
    private TextView mRecentMusic;
    private TextView mRecentName;
    private boolean progressionVisible = false;

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
        Log.i("TONAL", "Home opened.");
        Context context = requireContext();
        mPreferences = new Preferences(context);
        mVibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        createPlaceholders(context);

        if (mActivity != null && mActivity.getServiceMusic() != null) {
            try {
                ensureAdapters(context);
                mRecentsList.setAdapter(mRecentsAdapter);
                mCarouselList.setAdapter(mCarouselAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ComposeView content = new ComposeView(context);
        HomeCarouselHostKt.setupHomeCarouselCompose(content, this);

        if (mActivity != null) {
            mActivity.setFragment(10);
            if (mActivity.getServiceMusic() != null) {
                mActivity.hideSearchIcon(true);
                mActivity.refreshNavigation(0);
                loadDatas();
            }
        }
        return content;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application_Extend.getInstance().trackScreenView("Home");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCarouselList = null;
        mRecentsList = null;
        mFabView = null;
        mTimeView = null;
        mDurationView = null;
        mProgressView = null;
        mRecentsAdapter = null;
        mCarouselAdapter = null;
    }

    /** Other classes read these statics without null checks, so they always exist while Home is open. */
    private void createPlaceholders(Context context) {
        if (mFabView == null) mFabView = new PlayPauseButton(context);
        if (mTimeView == null) mTimeView = new TextView(context);
        if (mDurationView == null) mDurationView = new TextView(context);
        if (mProgressView == null) mProgressView = new ProgressView(context);
        if (mCarouselList == null) mCarouselList = new HorizontalInfiniteCycleViewPager(context);
        if (mRecentsList == null) {
            mRecentsList = new RecyclerView(context);
            mRecentsList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        }
        if (mProgression == null) mProgression = new LinearLayout(context);
        if (mRecentLayout == null) mRecentLayout = new LinearLayout(context);
        if (mRecentMusic == null) mRecentMusic = new TextView(context);
        if (mRecentName == null) mRecentName = new TextView(context);
        if (mPlayer == null) mPlayer = new Button(context);
    }

    private void ensureAdapters(Context context) {
        ServiceMusic service = mActivity.getServiceMusic();
        if (mRecentsAdapter == null) {
            mRecentsAdapter = new AdapterCardRecent(context, service.getRecent().getPlaylistMusics(), service, this);
        }
        if (mCarouselAdapter == null) {
            mCarouselAdapter = new AdapterCarousel(context, this, service, mActivity.getScreenWidth());
        }
    }

    // ---- Loading ----------------------------------------------------------------------------

    public int getCardPosition() {
        return mActivity != null ? mActivity.getCardPosition() : 0;
    }

    public void loadDatas() {
        if (mActivity == null) {
            return;
        }
        mActivity.setFragment(10);
        mActivity.hideSearchIcon(true);
        showNowPlayingInToolbar();
        mActivity.refreshNavigation(0);
        if (progressionVisible && mProgression != null) {
            slideProgressionAway();
        }

        Context context = getContext();
        if (context == null || mActivity.getServiceMusic() == null) {
            return;
        }
        try {
            ensureAdapters(context);
            if (mRecentsList != null) {
                mRecentsList.setAdapter(mRecentsAdapter);
            }
            if (mCarouselList != null) {
                mCarouselList.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override public void onPageScrollStateChanged(int state) {}
                    @Override public void onPageScrolled(int position, float offset, int pixels) {}
                    @Override public void onPageSelected(int position) {
                        onCardSelected(position);
                    }
                });
                mCarouselList.setInterpolator(AnimationUtils.loadInterpolator(context, android.R.anim.overshoot_interpolator));
                mCarouselList.setAdapter(mCarouselAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showNowPlayingInToolbar() {
        try {
            ServiceMusic service = mActivity.getServiceMusic();
            if (service != null && service.musicPlayed != null) {
                Music song = service.musicPlayed.getMusic(getContext());
                if (song != null) {
                    mActivity.setToolbarText(getString(R.string.menu_main), song.getTitle());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** A card became the front one: remember it, stop auto-scroll and re-tint the background. */
    private void onCardSelected(int position) {
        if (mActivity == null || mActivity.getServiceMusic() == null) {
            return;
        }
        int count = mActivity.getServiceMusic().getCategoriesSize();
        if (count <= 0) {
            return;
        }
        mActivity.setCardPosition(position % count);
        if (mCarouselList != null) {
            mCarouselList.stopAutoScroll();
        }
        Category category = mActivity.getServiceMusic().getCategory(getContext(), mActivity.getCardPosition());
        if (category != null) {
            mActivity.updateBackground(getContext(), category.getCover(), category.getDefaultCover(), 240);
        }
    }

    // ---- Progress strip slide ---------------------------------------------------------------

    private void resizeProgression(int width) {
        if (mProgression != null && mProgression.getLayoutParams() != null) {
            mProgression.getLayoutParams().width = width;
            mProgression.requestLayout();
            mProgression.setAlpha((width - 200.0f) / 300.0f);
        }
    }

    /** Coming back from the Player: the strip slides closed again. */
    private void slideProgressionAway() {
        progressionVisible = false;
        mProgression.setVisibility(View.VISIBLE);
        ValueAnimator slide = ValueAnimator.ofInt(mActivity.convertDpToPx(PROGRESS_SLIDE_DP), 0);
        slide.setDuration(200L).setStartDelay(500L);
        slide.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationCancel(Animator animator) {}
            @Override public void onAnimationRepeat(Animator animator) {}
            @Override public void onAnimationStart(Animator animator) {}
            @Override public void onAnimationEnd(Animator animator) {
                if (mProgression != null) {
                    mProgression.setVisibility(View.INVISIBLE);
                    mProgression.getLayoutParams().width = mActivity.convertDpToPx(PROGRESS_REST_DP);
                    mProgression.requestLayout();
                }
            }
        });
        slide.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                resizeProgression((Integer) animation.getAnimatedValue());
            }
        });
        slide.start();
    }

    // ---- Navigation -------------------------------------------------------------------------

    private void buzz() {
        if (mPreferences.getSettingsUIVibration() && mVibrate != null) {
            mVibrate.vibrate(5L);
        }
    }

    private TransitionInflater transitions() {
        return TransitionInflater.from(getActivity());
    }

    private void addSharedIfNamed(FragmentTransaction transaction, View view) {
        if (view != null && ViewCompat.getTransitionName(view) != null) {
            try {
                transaction.addSharedElement(view, ViewCompat.getTransitionName(view));
            } catch (Exception ignored) {
            }
        }
    }

    /** Plain slide used when there are no shared views to animate. */
    private void replaceWithSlide(FragmentManager manager, Fragment next) {
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(R.anim.animation_player_opening_close, R.anim.animation_player_opening_open,
                R.anim.animation_player_closing_close, R.anim.animation_player_closing_open);
        transaction.replace(R.id.container_body, next);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void openMusics(int category, String playlist, ImageView cover, TextView title) {
        buzz();
        if (mActivity != null) {
            mActivity.setTransitionID(TRANSITION_HOME_TO_SONGS);
        }
        Fragment_Home_Songs songs = new Fragment_Home_Songs();
        Bundle args = new Bundle();
        args.putInt("category", category);
        args.putString("playlist", playlist);
        songs.setArguments(args);
        FragmentManager manager = getActivity().getSupportFragmentManager();
        if (cover == null || title == null) {
            replaceWithSlide(manager, songs);
            return;
        }
        cover.setTransitionName(getString(R.string.transition_name_cover));
        title.setTransitionName(getString(R.string.transition_name_title));
        setReenterTransition(transitions().inflateTransition(R.transition.transition_home_to_songs));
        setExitTransition(transitions().inflateTransition(R.transition.transition_home_to_songs));
        songs.setSharedElementEnterTransition(transitions().inflateTransition(R.transition.transition_shared_element_enter));
        songs.setSharedElementReturnTransition(transitions().inflateTransition(R.transition.transition_shared_element_return));
        FragmentTransaction transaction = manager.beginTransaction();
        addSharedIfNamed(transaction, mFabView);
        try {
            transaction.addSharedElement(cover, ViewCompat.getTransitionName(cover))
                    .addSharedElement(title, ViewCompat.getTransitionName(title))
                    .replace(R.id.container_body, songs)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openPlayer(final ImageView cover) {
        buzz();
        if (mActivity != null) {
            mActivity.setTransitionID(TRANSITION_HOME_TO_PLAYER);
        }
        final Fragment_Player player = new Fragment_Player();
        Bundle args = new Bundle();
        args.putBoolean("allowplaylist", true);
        player.setArguments(args);
        final FragmentManager manager = getActivity().getSupportFragmentManager();
        if (cover == null) {
            replaceWithSlide(manager, player);
            return;
        }
        cover.setTransitionName(getString(R.string.transition_name_cover));
        ValueAnimator slide = ValueAnimator.ofInt(0, mActivity != null ? mActivity.convertDpToPx(PROGRESS_SLIDE_DP) : 200);
        slide.setDuration(200L);
        slide.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationCancel(Animator animator) {}
            @Override public void onAnimationRepeat(Animator animator) {}
            @Override public void onAnimationStart(Animator animator) {}
            @Override public void onAnimationEnd(Animator animator) {
                progressionVisible = true;
                setReenterTransition(transitions().inflateTransition(R.transition.transition_home_to_player));
                setExitTransition(transitions().inflateTransition(R.transition.transition_home_to_player));
                player.setSharedElementEnterTransition(transitions().inflateTransition(R.transition.transition_shared_element_enter));
                player.setSharedElementReturnTransition(transitions().inflateTransition(R.transition.transition_shared_element_return));
                FragmentTransaction transaction = manager.beginTransaction();
                addSharedIfNamed(transaction, mTimeView);
                addSharedIfNamed(transaction, mDurationView);
                addSharedIfNamed(transaction, mProgressView);
                addSharedIfNamed(transaction, mFabView);
                try {
                    transaction.addSharedElement(cover, ViewCompat.getTransitionName(cover))
                            .replace(R.id.container_body, player)
                            .addToBackStack(null)
                            .commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        slide.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                resizeProgression((Integer) animation.getAnimatedValue());
            }
        });
        if (mProgression != null) {
            mProgression.setVisibility(View.VISIBLE);
        }
        slide.start();
    }

    public void openPlaylists(int category, String name, final ImageView cover, TextView title, final ImageButton button) {
        buzz();
        final Fragment_Home_Playlists playlists = new Fragment_Home_Playlists();
        Bundle args = new Bundle();
        args.putInt("category", category);
        args.putString("name", name);
        playlists.setArguments(args);
        final FragmentManager manager = getActivity().getSupportFragmentManager();
        if (cover == null || title == null || button == null) {
            replaceWithSlide(manager, playlists);
            return;
        }
        cover.setTransitionName(getString(R.string.transition_card_cover));
        title.setTransitionName(getString(R.string.transition_card_title));
        button.setTransitionName(getString(R.string.transition_card_button));
        setReenterTransition(transitions().inflateTransition(R.transition.transition_playlists_to_home));
        setExitTransition(transitions().inflateTransition(R.transition.transition_home_to_playlists));
        playlists.setSharedElementEnterTransition(transitions().inflateTransition(R.transition.transition_shared_element_enter));
        playlists.setSharedElementReturnTransition(transitions().inflateTransition(R.transition.transition_shared_element_return));

        // The recents row drops out of the way first, then the screens swap.
        float drop = mRecentLayout != null ? mRecentLayout.getMeasuredHeight() * 1.5f : 100f;
        TranslateAnimation dropAway = new TranslateAnimation(0f, 0f, 0f, drop);
        dropAway.setDuration(120L);
        dropAway.setFillAfter(true);
        dropAway.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                FragmentTransaction transaction = manager.beginTransaction();
                addSharedIfNamed(transaction, mFabView);
                try {
                    transaction.addSharedElement(cover, ViewCompat.getTransitionName(cover))
                            .addSharedElement(button, ViewCompat.getTransitionName(button))
                            .replace(R.id.container_body, playlists)
                            .addToBackStack(null)
                            .commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        if (mRecentLayout != null) {
            mRecentLayout.startAnimation(dropAway);
        }
    }

    // ---- "Recently added" caption -----------------------------------------------------------

    private void setCaption(final TextView view, final String text) {
        if (view != null) {
            view.post(new Runnable() {
                @Override public void run() {
                    view.setText(text);
                }
            });
        }
    }

    public void refreshRecentTitle(int position) {
        lastcardPosition = position;
        if (position < 0) {
            position = 0;
        }
        String title;
        String heading;
        if (position == 0) {
            title = (mActivity != null && mActivity.getServiceMusic() != null)
                    ? mActivity.getServiceMusic().musicPlayed.getMusic(getContext()).getTitle() : "";
            heading = getString(R.string.home_playing);
        } else {
            try {
                Playlist recent = mActivity.getServiceMusic().getRecent();
                title = recent.getPlaylistMusics().get(position - 1).getTitle();
                heading = getString(R.string.home_recently_added) + " (" + recent.getPlaylistSize() + ")";
            } catch (Exception e) {
                return;
            }
        }
        setCaption(mRecentName, heading);
        setCaption(mRecentMusic, title);
    }

    public void refreshRecentTitleName() {
        if (lastcardPosition != 0 || mActivity == null || mActivity.getServiceMusic() == null) {
            return;
        }
        try {
            setCaption(mRecentMusic, mActivity.getServiceMusic().getRecent().getPlaylistMusics().get(0).getTitle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---- Carousel controls ------------------------------------------------------------------

    public void scrollReset() {
        if (mRecentsList != null) {
            mRecentsList.scrollToPosition(AdapterCardRecent.lastSelectedPosition);
        }
        if (mCarouselList == null || mActivity == null) {
            return;
        }
        int current = mCarouselList.getCurrentItem();
        if (mCarouselAdapter != null) {
            mCarouselAdapter.notifyDataSetChanged();
        }
        if (mActivity.getCardPosition() > 3) {
            mCarouselList.setAdapter(mCarouselAdapter);
            mCarouselList.setCurrentItem(current);
        } else {
            mCarouselList.setCurrentItem(current + 6);
        }
        if (mActivity.getCardPosition() == 0 && mActivity.getServiceMusic() != null) {
            Category first = mActivity.getServiceMusic().getCategory(getContext(), 0);
            if (first != null) {
                mActivity.updateBackground(getContext(), first.getCover(), first.getDefaultCover(), 240);
            }
        }
    }

    public void scrollTo(boolean forward) {
        if (mPreferences != null) {
            buzz();
        }
        if (mCarouselList != null) {
            mCarouselList.startAutoScroll(forward);
        }
    }

    public void setCover(ImageView cover) {
        mCover = cover;
    }
}
