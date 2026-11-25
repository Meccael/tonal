package com.meccael.tonal.application;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.meccael.tonal.R;

/* JADX INFO: loaded from: classes.dex */
public class Fragment_Services extends Fragment {
    private CallbacksInterface mActivity;
    private Preferences mPreferences;
    private Vibrator mVibrate;

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
        View viewInflate = layoutInflater.inflate(R.layout.fragment_services, viewGroup, false);
        this.mActivity.setFragment(50);
        this.mActivity.hideSearchIcon(true);
        this.mActivity.setToolbarText(getString(R.string.services_global), getString(R.string.services_sub));
        this.mActivity.refreshNavigation(4);
        if (this.mActivity.getPaddingBottom(getContext()) > 0) {
        }
        return viewInflate;
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        Application_Extend.getInstance().trackScreenView("Services");
    }
}
