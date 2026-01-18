package com.example.tarotsolitaire;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class MyApp extends Application {
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
    }
}
