package com.meccael.tonal.application;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import com.meccael.tonal.R;
import com.meccael.tonal.manager.Manager;

/* JADX INFO: loaded from: classes.dex */
public class Fragment_Empty extends Fragment {
    private CallbacksInterface mActivity;
    private Preferences mPreferences;
    private Vibrator mVibrate;

    public class AsyncTaskLoader extends AsyncTask<Integer, Integer, Integer> {
        public AsyncTaskLoader() {
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Integer doInBackground(Integer... numArr) {
            Log.i("TONAL", "Starting manager.");
            return Integer.valueOf((int) new Manager(Fragment_Empty.this.getContext(), Fragment_Empty.this.mPreferences, Fragment_Empty.this.mActivity.getServiceMusic(), null).timePassed);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(Integer num) {
            if (num.intValue() != 0) {
                Fragment_Empty.this.mActivity.loadLastSong();
                FragmentTransaction fragmentTransactionBeginTransaction = Fragment_Empty.this.getFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.setCustomAnimations(R.anim.animation_player_opening_close, R.anim.animation_player_opening_open, R.anim.animation_player_closing_close, R.anim.animation_player_closing_open);
                fragmentTransactionBeginTransaction.replace(R.id.container_body, new Fragment_Home_Carousel());
                fragmentTransactionBeginTransaction.commit();
            }
        }
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
        View viewInflate = layoutInflater.inflate(R.layout.fragment_empty, viewGroup, false);
        this.mActivity.setFragment(0);
        this.mActivity.setToolbarText(getString(R.string.app_name), getString(R.string.introduction_error));
        ImageView imageView = (ImageView) viewInflate.findViewById(R.id.nomusic);
        Button button = (Button) viewInflate.findViewById(R.id.check);
        this.mPreferences = new Preferences(getContext());
        this.mVibrate = (Vibrator) getContext().getSystemService("vibrator");
        final Animation animationLoadAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.animation_bouton_down);
        final Animation animationLoadAnimation2 = AnimationUtils.loadAnimation(getContext(), R.anim.animation_bouton_up);
        int paddingBottom = this.mActivity.getPaddingBottom(getContext());
        if (paddingBottom > 0) {
            ((ViewGroup.MarginLayoutParams) button.getLayoutParams()).bottomMargin += paddingBottom;
        }
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.5f);
        alphaAnimation.setDuration(1000L);
        alphaAnimation.setRepeatMode(2);
        alphaAnimation.setRepeatCount(-1);
        alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        imageView.startAnimation(alphaAnimation);
        button.setOnTouchListener(new View.OnTouchListener() { // from class: com.meccael.tonal.application.Fragment_Empty.1
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case 0:
                        view.startAnimation(animationLoadAnimation);
                        if (Fragment_Empty.this.mPreferences.getSettingsUIVibration()) {
                            Fragment_Empty.this.mVibrate.vibrate(5L);
                        }
                        return true;
                    case 1:
                        view.startAnimation(animationLoadAnimation2);
                        Cursor cursorQuery = Fragment_Empty.this.getContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
                        if (cursorQuery != null) {
                            if (!cursorQuery.moveToFirst()) {
                                Fragment_Empty.this.new AsyncTaskLoader().execute(new Integer[0]);
                                cursorQuery.close();
                            }
                            cursorQuery.close();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
        return viewInflate;
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        Application_Extend.getInstance().trackScreenView("No music");
    }
}
