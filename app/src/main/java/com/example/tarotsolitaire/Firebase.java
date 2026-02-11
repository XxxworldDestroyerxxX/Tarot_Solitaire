package com.example.tarotsolitaire;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class Firebase extends Application {
    private static final String TAG = "MyApp";

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
    }
}
