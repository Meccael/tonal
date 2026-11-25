package com.meccael.tonal.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.TextView;

/** What the screens (fragments) can ask of the main activity. */
public interface CallbacksInterface {
    // ---- Service and playback ---------------------------------------------------------------
    void bindService();
    ServiceMusic getServiceMusic();
    boolean isPlaying();
    void seekTo(int position);
    void loadLastSong();
    boolean getPressNext();
    void setPressNext(boolean pressed);
    void refreshFabIcon();
    void refreshPlayerVinyl(Context context);

    // ---- Which screen is open ---------------------------------------------------------------
    int getFragment();
    void setFragment(int id);
    int getRunningActivities();
    int getCardItem();
    void setCardItem(int item);
    int getCardPosition();
    void setCardPosition(int position);
    int getTransitionID();
    void setTransitionID(int id);
    void refreshNavigation(int item);
    void setSettingSectionOpen(boolean open);
    void setSettingSubSectionOpen(boolean open);
    void setUpdaterSectionOpen(boolean open);

    // ---- Toolbar and search -----------------------------------------------------------------
    TextView getTitleView();
    TextView getSubtitleView();
    void setToolbarText(String title, String subtitle);
    void showSearchBar(boolean animate);
    void hideSearchBar(boolean animate);
    void showSearchIcon(boolean animate);
    void hideSearchIcon(boolean animate);
    void setSearchIconAlpha(float alpha);

    // ---- Theme colours and artwork ----------------------------------------------------------
    Bitmap getThemeArt();
    void setThemeArt(Bitmap art);
    Bitmap getThemeThumbnail();
    int getThemeColorFab();
    void setThemeColorFab(int color);
    int getThemeColorLight();
    void setThemeColorLight(int color);
    int getThemeColorVibrant();
    void setThemeColorVibrant(int color);
    void updateBackground(Context context, long coverId, int fallbackRes, int duration);
    void blurBackground(Context context, int coverId);
    int getCoverScaled();

    // ---- Screen size and units --------------------------------------------------------------
    int getScreenHeight();
    int getScreenWidth();
    int getPaddingBottom(Context context);
    float convertDpToPx(float dp);
    int convertDpToPx(int dp);
}
