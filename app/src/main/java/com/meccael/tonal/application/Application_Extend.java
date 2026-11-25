package com.meccael.tonal.application;

import android.app.Application;

public class Application_Extend extends Application {
    private static Application_Extend mInstance;

    public static synchronized Application_Extend getInstance() {
        return mInstance;
    }

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    // Analytics was removed (the original Google Analytics SDK and its service are discontinued).
    // These methods are kept as no-ops so existing callers still compile.
    public void trackEvent(String category, String action, String label) {
    }

    public void trackScreenView(String screenName) {
    }
}