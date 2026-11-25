package com.meccael.tonal.application;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.meccael.tonal.R;

/**
 * The "About" screen. This class keeps its original name (it used to be the Minima
 * beta signup page) so the drawer slot and Application_Main stay untouched.
 */
public class Fragment_ReadyForBeta extends Fragment {
    private CallbacksInterface mActivity;

    @Override // androidx.fragment.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mActivity = (CallbacksInterface) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    /** Kept because Application_Main calls it from its back-press handler. Nothing to close here. */
    public void closeSection(Context context) {
    }

    private String versionName(Context context) {
        try {
            String name = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            return name == null ? "" : name;
        } catch (Exception e) {
            return "";
        }
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View root = layoutInflater.inflate(R.layout.fragment_readyforbeta, viewGroup, false);
        Context context = getContext();
        String version = versionName(context);

        this.mActivity.setFragment(30);
        this.mActivity.hideSearchIcon(true);
        this.mActivity.setToolbarText(getString(R.string.menu_updater), getString(R.string.app_name) + " " + version);
        this.mActivity.refreshNavigation(2);

        ImageView rocket = (ImageView) root.findViewById(R.id.rocket);
        TranslateAnimation bob = new TranslateAnimation(20.0f, 0.0f, 20.0f, -5.0f);
        bob.setDuration(600L);
        bob.setRepeatMode(2);
        bob.setRepeatCount(-1);
        bob.setInterpolator(new AccelerateDecelerateInterpolator());
        rocket.startAnimation(bob);

        ((TextView) root.findViewById(R.id.about_version)).setText(getString(R.string.about_version, version));

        LinearLayout block = (LinearLayout) root.findViewById(R.id.view_try);
        int paddingBottom = this.mActivity.getPaddingBottom(context);
        if (paddingBottom > 0) {
            ((ViewGroup.MarginLayoutParams) block.getLayoutParams()).bottomMargin += paddingBottom;
        }
        return root;
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        Application_Extend.getInstance().trackScreenView("About");
    }
}