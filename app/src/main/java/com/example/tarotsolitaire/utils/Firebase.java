package com.example.tarotsolitaire.utils;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.example.tarotsolitaire.manager.MusicManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class Firebase extends Application {
    private static final String TAG = "MyApp";
    private int startedActivityCount = 0;

    private static final String PREFS_LIFECYCLE = "appLifecycle";
    private static final String PREF_LAST_FOREGROUND = "lastForegroundAtMs";

    // Timestamp when app most recently entered foreground (ms since epoch). 0 when not in foreground.
    private long lastForegroundAtMs = 0L;

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

        // If there's a leftover last-foreground timestamp (app crashed or was killed while foreground),
        // convert it now into playtime so it's not lost.
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_LIFECYCLE, MODE_PRIVATE);
            long leftover = prefs.getLong(PREF_LAST_FOREGROUND, 0L);
            if (leftover > 0L) {
                long now = System.currentTimeMillis();
                long elapsed = now > leftover ? now - leftover : 0L;
                prefs.edit().remove(PREF_LAST_FOREGROUND).apply();
                if (elapsed > 0L) {
                    // update Firestore if signed in
                    try {
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            final String uidFinal = uid;
                            final long elapsedFinal = elapsed;
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            Map<String, Object> m = new HashMap<>();
                            m.put("totalPlayTimeMs", FieldValue.increment(elapsedFinal));
                            db.collection("users").document(uidFinal).set(m, com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> Log.i(TAG, "Recovered and added " + elapsedFinal + "ms to totalPlayTimeMs for " + uidFinal))
                                    .addOnFailureListener(e -> Log.w(TAG, "Failed to recover/add playtime to Firestore", e));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error while updating totalPlayTimeMs (recovery)", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error during lastForeground recovery", e);
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
                    lastForegroundAtMs = System.currentTimeMillis();
                    try {
                        // persist so a crash or kill still allows recovery
                        SharedPreferences prefs = getSharedPreferences(PREFS_LIFECYCLE, MODE_PRIVATE);
                        prefs.edit().putLong(PREF_LAST_FOREGROUND, lastForegroundAtMs).apply();
                    } catch (Exception ignored) {}

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
                    long now = System.currentTimeMillis();
                    long elapsed = 0L;
                    try {
                        SharedPreferences prefs = getSharedPreferences(PREFS_LIFECYCLE, MODE_PRIVATE);
                        long stored = prefs.getLong(PREF_LAST_FOREGROUND, 0L);
                        if (stored > 0L && now > stored) elapsed = now - stored;
                        // clear persisted foreground marker
                        prefs.edit().remove(PREF_LAST_FOREGROUND).apply();
                    } catch (Exception e) {
                        Log.w(TAG, "Error reading/clearing persisted foreground timestamp", e);
                    }
                    lastForegroundAtMs = 0L;

                    // Notify MusicManager
                    try { MusicManager.get(Firebase.this).handleAppBackgrounded(); } catch (Exception ignored) {}

                    // If elapsed > 0 and user signed in, add to Firestore totalPlayTimeMs
                    if (elapsed > 0L) {
                        try {
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                final String uidFinal = uid;
                                final long elapsedFinal = elapsed;
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                Map<String, Object> m = new HashMap<>();
                                m.put("totalPlayTimeMs", FieldValue.increment(elapsedFinal));
                                db.collection("users").document(uidFinal).set(m, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener(aVoid -> Log.i(TAG, "Added " + elapsedFinal + "ms to totalPlayTimeMs for " + uidFinal))
                                        .addOnFailureListener(e -> Log.w(TAG, "Failed to add playtime to Firestore", e));
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error while updating totalPlayTimeMs", e);
                        }
                    }

                    Log.d(TAG, "App backgrounded (elapsed ms=" + elapsed + ")");
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
    }
}
