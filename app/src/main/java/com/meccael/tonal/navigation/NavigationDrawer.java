package com.meccael.tonal.navigation;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import com.meccael.tonal.R;
import com.meccael.tonal.application.Application_Main;
import com.meccael.tonal.application.CallbacksInterface;
import com.meccael.tonal.application.Fragment_Home_Carousel;
import com.meccael.tonal.application.Fragment_Home_Playlists;
import com.meccael.tonal.application.Fragment_Home_Songs;
import com.meccael.tonal.application.Fragment_Playlist;
import com.meccael.tonal.application.Preferences;

/**
 * The slide-out menu. While it opens, the screen slides right, the toolbar title fades, the menu
 * rows fly in one after another, and the floating play button slides away.
 */
public class NavigationDrawer extends Fragment {
    /** Width of the left strip where a rightward swipe opens the drawer. */
    private static final float SWIPE_ZONE_DP = 110f;

    private Preferences mPrefs;
    private Vibrator mVibrator;
    CallbacksInterface mActivity;
    FragmentDrawerListener drawerListener;

    public NavigationDrawerAdapter mAdapter;
    public long lastClick = 0;
    public boolean drawerOpen = false;

    private DrawerLayout mDrawerLayout;
    private RecyclerView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private View mDrawerView;
    private FrameLayout mContent;
    private ImageView mCoverBack;
    private ImageButton mToolbarSearch;
    private LinearLayout mToolbarTitle;

    private final List<NavigationItem> mItems = new ArrayList<>();
    private String[] mTitles;
    private TypedArray mIcons;

    public interface ClickListener {
        void onClick(View view, int position);
        void onLongClick(View view, int position);
    }

    public interface FragmentDrawerListener {
        void onDrawerItemSelected(View view, int position);
    }

    /** Turns taps and long-presses on the list rows into ClickListener calls. */
    static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {
        private final ClickListener mListener;
        private final GestureDetector mDetector;

        RecyclerTouchListener(Context context, final RecyclerView list, final ClickListener listener) {
            mListener = listener;
            mDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    View child = list.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && listener != null) {
                        listener.onLongClick(child, list.getChildAdapterPosition(child));
                    }
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView list, MotionEvent e) {
            View child = list.findChildViewUnder(e.getX(), e.getY());
            if (child != null && mListener != null && mDetector.onTouchEvent(e)) {
                mListener.onClick(child, list.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView list, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallow) {
        }
    }

    // ---- Public API used by the main activity -----------------------------------------------

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(mDrawerView);
    }

    public boolean getDrawerOpened() {
        return drawerOpen;
    }

    public int getItemsNumber() {
        return mItems.size();
    }

    public List<NavigationItem> getData() {
        mItems.clear();
        for (int i = 0; i < mTitles.length; i++) {
            NavigationItem item = new NavigationItem();
            item.setTitle(mTitles[i]);
            item.setIcon(mIcons.getResourceId(i, -1));
            mItems.add(item);
        }
        return mItems;
    }

    public void setDrawerListener(FragmentDrawerListener listener) {
        drawerListener = listener;
    }

    // ---- Fragment lifecycle -----------------------------------------------------------------

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
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mPrefs = new Preferences(getContext());
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        mTitles = getActivity().getResources().getStringArray(R.array.nav_drawer_labels);
        mIcons = getActivity().getResources().obtainTypedArray(R.array.nav_drawer_icons);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = inflater.inflate(R.layout.navigation_drawer, container, false);
        mAdapter = new NavigationDrawerAdapter(getActivity(), getData());
        mDrawerList = (RecyclerView) view.findViewById(R.id.drawerList);
        mDrawerList.setAdapter(mAdapter);
        mDrawerList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mDrawerList.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), mDrawerList, new ClickListener() {
            @Override
            public void onClick(View row, int position) {
                onRowTapped(row, position);
            }

            @Override
            public void onLongClick(View row, int position) {
            }
        }));
        mDrawerList.setPadding(0, 0, 0, mActivity.getPaddingBottom(getContext()));
        return view;
    }

    private void onRowTapped(final View row, final int position) {
        if (mPrefs.getSettingsUIVibration()) {
            mVibrator.vibrate(5L);
        }
        if (lastClick == position) {
            // Home stays "selected" while you are inside its sub-screens (albums 11, songs 12,
            // player 15), so tapping Home there must still take you back to the main menu.
            int screen = Application_Main.fragmentID;
            if (position == 0 && (screen == 11 || screen == 12 || screen == 15)) {
                drawerListener.onDrawerItemSelected(row, position);
            }
            if (drawerOpen) {
                mDrawerLayout.closeDrawer(mDrawerView);
            }
            return;
        }
        lastClick = position;
        mAdapter.selected_item = position;
        mDrawerList.getAdapter().notifyDataSetChanged();

        Handler handler = new Handler();
        if (position != getItemsNumber() - 1) {
            drawerListener.onDrawerItemSelected(row, position);
        } else {
            // The last entry (About) waits until the drawer is mostly closed.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    drawerListener.onDrawerItemSelected(row, position);
                }
            }, 500L);
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawerLayout.closeDrawer(mDrawerView);
            }
        }, 75L);
    }

    // ---- Drawer behaviour -------------------------------------------------------------------

    public void setUp(int drawerViewId, DrawerLayout drawerLayout, Toolbar toolbar) {
        mDrawerLayout = drawerLayout;
        mDrawerView = getActivity().findViewById(drawerViewId);
        mContent = (FrameLayout) getActivity().findViewById(R.id.container_body);
        mCoverBack = (ImageView) getActivity().findViewById(R.id.background);
        mToolbarTitle = (LinearLayout) getActivity().findViewById(R.id.toolbar_title);
        mToolbarSearch = (ImageButton) getActivity().findViewById(R.id.toolbar_search);

        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawer) {
                super.onDrawerClosed(drawer);
                drawerOpen = false;
            }

            @Override
            public void onDrawerOpened(View drawer) {
                super.onDrawerOpened(drawer);
                drawerOpen = true;
                mActivity.hideSearchBar(true);
            }

            @Override
            public void onDrawerSlide(View drawer, float slide) {
                super.onDrawerSlide(drawer, slide);
                animateSlide(drawer.getWidth(), slide);
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
                widenSwipeZone();
            }
        });
    }

    /** Moves the rest of the screen along with the drawer as it opens (slide is 0 closed to 1 open). */
    private void animateSlide(int drawerWidth, float slide) {
        // Menu rows fly in one after another, the first row leading.
        for (int i = 0; i < getItemsNumber(); i++) {
            View row = mDrawerList.getChildAt(i);
            if (row == null) {
                continue;
            }
            float lag = (5 - i) / 5f;
            row.setTranslationX(2f * (((1f - lag) * slide + lag) - 1f) * row.getWidth());
        }

        float moved = drawerWidth * slide;
        float fade = 1f - (moved * 1.25f) / drawerWidth;
        float fabShift = -moved * 0.75f;
        mContent.setX(moved);
        mToolbarTitle.setAlpha(fade);
        mToolbarTitle.setTranslationY(-moved / 18f);
        mToolbarSearch.setAlpha(fade);
        mCoverBack.setAlpha(0.6f - (0.35f * moved) / drawerWidth);

        // Slide the floating play button of whichever screen is open, and fade the search icon.
        View fab = null;
        switch (mActivity.getFragment()) {
            case 10: fab = Fragment_Home_Carousel.mFabView; break;
            case 11: fab = Fragment_Home_Playlists.mFabView; break;
            case 12: fab = Fragment_Home_Songs.mFabView; break;
            case 20: fab = Fragment_Playlist.mFabView; break;
            default: return;
        }
        if (fab != null) {
            fab.setTranslationX(fabShift);
        }
        mActivity.setSearchIconAlpha(fade);
    }

    /**
     * By default the drawer only opens from a ~20dp strip at the screen edge, which Android's back
     * gesture also uses. Widen the strip so a left-to-right swipe starting a little further in
     * opens the drawer, following the finger.
     */
    private void widenSwipeZone() {
        try {
            int edge = (int) (SWIPE_ZONE_DP * getResources().getDisplayMetrics().density);
            Field draggerField = DrawerLayout.class.getDeclaredField("mLeftDragger");
            draggerField.setAccessible(true);
            Object dragger = draggerField.get(mDrawerLayout);
            Field edgeField = dragger.getClass().getDeclaredField("mEdgeSize");
            edgeField.setAccessible(true);
            edgeField.setInt(dragger, edge);
        } catch (Exception e) {
            Log.w("TONAL", "Could not widen the drawer swipe zone: " + e);
        }
    }
}
