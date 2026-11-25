package com.meccael.tonal.application;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.meccael.tonal.R;
import com.meccael.tonal.adapters.AdapterCardPlaylist;
import com.meccael.tonal.views.PlayPauseButton;

/* JADX INFO: loaded from: classes.dex */
public class Fragment_Home_Playlists extends Fragment {
    public static PlayPauseButton mFabView;
    private View rootView;
    String categoryName;
    RecyclerView.ItemDecoration dividerItemDecoration;
    private CallbacksInterface mActivity;
    RecyclerView mPlaylistCard;
    AdapterCardPlaylist mPlaylistCardAdapter;
    private Preferences mPreferences;
    private Vibrator mVibrate;
    int columnNumber = 3;
    int category = 2;

    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spacing;
        private int spanCount;

        GridSpacingItemDecoration(int i, int i2) {
            this.spanCount = 0;
            this.spacing = 0;
            this.spanCount = i;
            this.spacing = i2;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.ItemDecoration
        public void getItemOffsets(Rect rect, View view, RecyclerView recyclerView, RecyclerView.State state) {
            int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
            int i = childAdapterPosition % this.spanCount;
            rect.left = this.spacing - ((this.spacing * i) / this.spanCount);
            rect.right = ((i + 1) * this.spacing) / this.spanCount;
            rect.bottom = this.spacing;
            if (childAdapterPosition < this.spanCount) {
                rect.top = this.spacing;
            }
            if (recyclerView.getChildAdapterPosition(view) == 0 || recyclerView.getChildAdapterPosition(view) == 1 || recyclerView.getChildAdapterPosition(view) == 2 || (recyclerView.getChildAdapterPosition(view) == 3 && Fragment_Home_Playlists.this.getResources().getBoolean(R.bool.isTablet))) {
                rect.top += Fragment_Home_Playlists.this.mActivity.convertDpToPx(35);
            }
        }
    }

    private void showSection() {
        TextView section = rootView == null ? null : (TextView) rootView.findViewById(R.id.cards_section);
        if (section == null || this.mPlaylistCardAdapter == null) {
            return;
        }
        int name;
        switch (this.category) {
            case 2: name = R.string.browse_cabinet; break;
            case 3: name = R.string.browse_spotlight; break;
            case 4: name = R.string.browse_bubbles; break;
            case 5: name = R.string.browse_cassettes; break;
            default: name = R.string.browse_crate; break;
        }
        section.setText(getString(R.string.browse_section, getString(name), this.mPlaylistCardAdapter.getItemCount()));
    }

    private int mSort = 0;
    private int mBaseRvBottom = -1;
    private final java.util.ArrayList<Integer> mLetterPositions = new java.util.ArrayList<>();
    private final android.os.Handler mHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    }

    /** NEW / A-Z / All pills next to the section title, and the letter strip that goes with A-Z. */
    private void setupSort() {
        if (rootView == null || getContext() == null) return;
        android.widget.LinearLayout pills = (android.widget.LinearLayout) rootView.findViewById(R.id.sort_pills);
        if (pills == null) return;
        mSort = getContext().getSharedPreferences("tonal_browse", Context.MODE_PRIVATE).getInt("sort_" + category, 0);
        if (pills.getChildCount() == 0) {
            int[] labels = {R.string.browse_sort_new, R.string.browse_sort_az, R.string.browse_sort_all};
            final int[] modes = {com.meccael.tonal.manager.Category.SORT_NEW, com.meccael.tonal.manager.Category.SORT_AZ, com.meccael.tonal.manager.Category.SORT_ALL};
            for (int i = 0; i < labels.length; i++) {
                final int mode = modes[i];
                TextView pill = new TextView(getContext());
                pill.setText(labels[i]);
                pill.setTextSize(11f);
                pill.setGravity(android.view.Gravity.CENTER);
                pill.setPadding(this.mActivity.convertDpToPx(12), 0, this.mActivity.convertDpToPx(12), 0);
                pill.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSort = mode;
                        if (getContext() != null) {
                            getContext().getSharedPreferences("tonal_browse", Context.MODE_PRIVATE).edit().putInt("sort_" + category, mode).apply();
                        }
                        applySort();
                    }
                });
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, this.mActivity.convertDpToPx(26));
                lp.setMarginStart(this.mActivity.convertDpToPx(6));
                pills.addView(pill, lp);
            }
        }
        // The toolbar swallows touches, so keep the pills clear of it.
        final android.widget.LinearLayout pillRow = pills;
        final int xmlTop = ((ViewGroup.MarginLayoutParams) pills.getLayoutParams()).topMargin;
        pills.post(new Runnable() {
            @Override
            public void run() {
                if (rootView == null || getActivity() == null) return;
                View tb = getActivity().findViewById(R.id.toolbar);
                if (tb == null) return;
                int[] t = new int[2];
                int[] r = new int[2];
                tb.getLocationOnScreen(t);
                rootView.getLocationOnScreen(r);
                int bottom = t[1] + tb.getHeight() - r[1] + mActivity.convertDpToPx(4);
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) pillRow.getLayoutParams();
                int want = Math.max(xmlTop, bottom);
                if (lp.topMargin != want) {
                    lp.topMargin = want;
                    pillRow.setLayoutParams(lp);
                }
                pillRow.bringToFront();
            }
        });
        final com.meccael.tonal.views.LetterStrip strip = (com.meccael.tonal.views.LetterStrip) rootView.findViewById(R.id.letter_strip);
        final TextView bubble = (TextView) rootView.findViewById(R.id.letter_bubble);
        if (strip != null) {
            strip.setHorizontal(isLandscape());
            strip.setListener(new com.meccael.tonal.views.LetterStrip.Listener() {
                @Override
                public void onLetter(int index) {
                    if (index < 0 || index >= mLetterPositions.size()) return;
                    mHandler.removeCallbacksAndMessages(null);
                    int pos = mLetterPositions.get(index);
                    RecyclerView.LayoutManager lm = mPlaylistCard.getLayoutManager();
                    if (lm instanceof androidx.recyclerview.widget.LinearLayoutManager) {
                        ((androidx.recyclerview.widget.LinearLayoutManager) lm).scrollToPositionWithOffset(pos, 0);
                    } else if (lm instanceof androidx.recyclerview.widget.StaggeredGridLayoutManager) {
                        ((androidx.recyclerview.widget.StaggeredGridLayoutManager) lm).scrollToPositionWithOffset(pos, 0);
                    }
                    if (bubble != null) {
                        bubble.setText(String.valueOf(strip.getLetter(index)));
                        bubble.setBackground(circle());
                        bubble.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onRelease() {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (bubble != null) bubble.setVisibility(View.GONE);
                        }
                    }, 350);
                }
            });
        }
        applySort();
    }

    private android.graphics.drawable.Drawable circle() {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        d.setColor(0xFF7C9CFF);
        return d;
    }

    /** Re-orders the list for the chosen pill and refreshes the pills, the strip and the adapter. */
    public void applySort() {
        if (rootView == null || mActivity.getServiceMusic() == null || getContext() == null) return;
        com.meccael.tonal.manager.Category c = mActivity.getServiceMusic().getCategory(getContext(), category);
        c.sortBy(mSort);
        if (mPlaylistCardAdapter != null) mPlaylistCardAdapter.notifyDataSetChanged();
        android.widget.LinearLayout pills = (android.widget.LinearLayout) rootView.findViewById(R.id.sort_pills);
        int[] modes = {com.meccael.tonal.manager.Category.SORT_NEW, com.meccael.tonal.manager.Category.SORT_AZ, com.meccael.tonal.manager.Category.SORT_ALL};
        for (int i = 0; pills != null && i < pills.getChildCount() && i < modes.length; i++) {
            TextView pill = (TextView) pills.getChildAt(i);
            boolean on = modes[i] == mSort;
            pill.setTextColor(on ? 0xFF111111 : 0xB3FFFFFF);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(this.mActivity.convertDpToPx(14));
            bg.setColor(on ? 0xFFE8E8EE : 0x1FFFFFFF);
            pill.setBackground(bg);
        }
        updateStrip(c);
        if (mPlaylistCard != null) mPlaylistCard.scrollToPosition(0);
    }

    private void updateStrip(com.meccael.tonal.manager.Category c) {
        com.meccael.tonal.views.LetterStrip strip = (com.meccael.tonal.views.LetterStrip) rootView.findViewById(R.id.letter_strip);
        if (strip == null || mPlaylistCard == null) return;
        ViewGroup.MarginLayoutParams rvLp = (ViewGroup.MarginLayoutParams) mPlaylistCard.getLayoutParams();
        if (mBaseRvBottom < 0) mBaseRvBottom = rvLp.bottomMargin;
        boolean show = mSort == com.meccael.tonal.manager.Category.SORT_AZ && c.getPlaylists().size() > 1;
        mLetterPositions.clear();
        if (show) {
            java.util.LinkedHashMap<Character, Integer> first = new java.util.LinkedHashMap<>();
            java.util.List<com.meccael.tonal.manager.Playlist> list = c.getPlaylists();
            for (int i = 0; i < list.size(); i++) {
                char ch = com.meccael.tonal.manager.Category.letterOf(list.get(i).getPlaylistName());
                if (!first.containsKey(ch)) first.put(ch, i);
            }
            char[] letters = new char[first.size()];
            int k = 0;
            for (java.util.Map.Entry<Character, Integer> e : first.entrySet()) {
                letters[k++] = e.getKey();
                mLetterPositions.add(e.getValue());
            }
            strip.setLetters(letters);
        }
        strip.setVisibility(show ? View.VISIBLE : View.GONE);
        int want = mBaseRvBottom + (show && isLandscape() ? this.mActivity.convertDpToPx(30) : 0);
        if (rvLp.bottomMargin != want) {
            rvLp.bottomMargin = want;
            mPlaylistCard.setLayoutParams(rvLp);
        }
    }

    public void loadDatas() {
        this.mActivity.setFragment(11);
        this.mActivity.hideSearchIcon(true);
        this.mActivity.setToolbarText(getString(R.string.menu_main), this.categoryName);
        this.mActivity.refreshNavigation(0);
        openCategory(this.category);
        showSection();
        setupSort();
        if (this.category == 2) {
            // Folders: rail list.
            com.meccael.tonal.views.FolderRailEffect.install(this.mPlaylistCard, this.mPlaylistCard.getPaddingBottom());
            this.mPlaylistCard.setAdapter(this.mPlaylistCardAdapter);
            return;
        }
        if (this.category == 3) {
            // Artists: spotlight wheel.
            com.meccael.tonal.views.ArtistWheelEffect.install(this.mPlaylistCard, this.mPlaylistCard.getPaddingBottom());
            this.mPlaylistCard.setAdapter(this.mPlaylistCardAdapter);
            return;
        }
        if (this.category == 4) {
            // Genres: bubble field.
            com.meccael.tonal.views.GenreBubblesEffect.install(this.mPlaylistCard, this.mPlaylistCard.getPaddingBottom());
            this.mPlaylistCard.setAdapter(this.mPlaylistCardAdapter);
            return;
        }
        if (this.category == 5) {
            // Playlists: pile of cassette tapes.
            com.meccael.tonal.views.CassetteEffect.install(this.mPlaylistCard, this.mPlaylistCard.getPaddingBottom());
            this.mPlaylistCard.setAdapter(this.mPlaylistCardAdapter);
            return;
        }
        if (this.category >= 1 && this.category <= 5) {
            // Albums, Folders, Artists, Genres, Playlists: record-crate look (single column of overlapping covers).
            com.meccael.tonal.views.CrateEffect.install(this.mPlaylistCard, this.mPlaylistCard.getPaddingBottom());
            this.mPlaylistCard.setAdapter(this.mPlaylistCardAdapter);
            return;
        }
        this.dividerItemDecoration = new GridSpacingItemDecoration(this.columnNumber, this.mActivity.convertDpToPx(4));
        this.mPlaylistCard.setLayoutManager(new GridLayoutManager(getActivity(), this.columnNumber));
        this.mPlaylistCard.addItemDecoration(this.dividerItemDecoration);
        this.mPlaylistCard.setItemAnimator(new DefaultItemAnimator());
        this.mPlaylistCard.setAdapter(this.mPlaylistCardAdapter);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // androidx.fragment.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mActivity = (CallbacksInterface) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.fragment_home_playlists, viewGroup, false);
        this.rootView = viewInflate;
        Bundle arguments = getArguments();
        if (arguments != null) {
            this.category = arguments.getInt("category");
            this.categoryName = arguments.getString("name");
        }
        Log.i("TONAL", "Playlists opened.");
        mFabView = (PlayPauseButton) viewInflate.findViewById(R.id.fab);
        this.mPlaylistCard = (RecyclerView) viewInflate.findViewById(R.id.list_cards);
        if (getResources().getBoolean(R.bool.isTablet)) {
            this.columnNumber = 4;
        }
        int paddingBottom = this.mActivity.getPaddingBottom(getContext());
        if (paddingBottom > 0) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) mFabView.getLayoutParams();
            marginLayoutParams.setMargins(0, 0, 0, this.mActivity.convertDpToPx(16) + paddingBottom);
            mFabView.setLayoutParams(marginLayoutParams);
            this.mPlaylistCard.setPadding(0, 0, 0, paddingBottom);
        }
        if (this.mActivity.getServiceMusic() == null) {
            this.mActivity.setFragment(11);
        } else {
            loadDatas();
        }
        return viewInflate;
    }

    @Override // androidx.fragment.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        if (this.rootView != null && mFabView != null && mFabView == this.rootView.findViewById(R.id.fab)) {
            mFabView = null;
        }
        this.mHandler.removeCallbacksAndMessages(null);
        this.mBaseRvBottom = -1;
        this.rootView = null;
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        Application_Extend.getInstance().trackScreenView("Playlists");
    }

    public void openCategory(int i) {
        this.mPlaylistCardAdapter = new AdapterCardPlaylist(getContext(), i, this.mActivity.getServiceMusic().getCategory(getContext(), i).getPlaylists(), this, this.mActivity.getServiceMusic());
    }

    public void openPlaylist(int i, int i2, String str, ImageView imageView, TextView textView) {
        this.mActivity.showSearchIcon(true);
        Fragment_Home_Songs fragment_Home_Songs = new Fragment_Home_Songs();
        Bundle bundle = new Bundle();
        bundle.putInt("category", i);
        bundle.putInt("position", i2);
        bundle.putString("playlist", str);
        fragment_Home_Songs.setArguments(bundle);
        FragmentManager supportFragmentManager = getActivity().getSupportFragmentManager();
        if (Build.VERSION.SDK_INT < 21) {
            FragmentTransaction fragmentTransactionBeginTransaction = supportFragmentManager.beginTransaction();
            fragmentTransactionBeginTransaction.setCustomAnimations(R.anim.animation_player_opening_close, R.anim.animation_player_opening_open, R.anim.animation_player_closing_close, R.anim.animation_player_closing_open);
            fragmentTransactionBeginTransaction.replace(R.id.container_body, fragment_Home_Songs);
            fragmentTransactionBeginTransaction.commit();
            return;
        }
        imageView.setTransitionName(getString(R.string.transition_name_cover));
        textView.setTransitionName(getString(R.string.transition_name_title));
        setReenterTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.transition_playlists_to_songs));
        setExitTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.transition_playlists_to_songs));
        fragment_Home_Songs.setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.transition_shared_element_enter));
        fragment_Home_Songs.setSharedElementReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.transition_shared_element_return));
        supportFragmentManager.beginTransaction().addSharedElement(mFabView, ViewCompat.getTransitionName(mFabView)).addSharedElement(imageView, ViewCompat.getTransitionName(imageView)).addSharedElement(textView, ViewCompat.getTransitionName(textView)).replace(R.id.container_body, fragment_Home_Songs).addToBackStack(null).commit();
    }
}
