package com.example.tarotsolitaire;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class Firebase extends Application {
    private static final String TAG = "MyApp";
    private int startedActivityCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Initializing Firebase");
        FirebaseApp app = FirebaseApp.initializeApp(this);
        try {
            if (app != null) {
                FirebaseOptions opts = app.getOptions();
                Log.i(TAG, "Firebase initialized: projectId=" + opts.getProjectId()
                        + ", applicationId=" + opts.getApplicationId()
                        + ", apiKey=" + opts.getApiKey());
            } else {
                Log.w(TAG, "FirebaseApp.initializeApp returned null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while reading Firebase options", e);
        }

        // Ensure the MusicManager is initialized at app startup so music can auto-play if enabled
        try {
            MusicManager.get(this);
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize MusicManager on app startup", e);
        }

        // Track activity lifecycle to detect foreground/background transitions
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivityCount++;
                if (startedActivityCount == 1) {
                    // App entered foreground
                    try { MusicManager.get(Firebase.this).handleAppForegrounded(); } catch (Exception ignored) {}
                    Log.d(TAG, "App foregrounded");
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {}

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivityCount = Math.max(0, startedActivityCount - 1);
                if (startedActivityCount == 0) {
                    // App entered background
                    try { MusicManager.get(Firebase.this).handleAppBackgrounded(); } catch (Exception ignored) {}
                    Log.d(TAG, "App backgrounded");
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
    }
}
