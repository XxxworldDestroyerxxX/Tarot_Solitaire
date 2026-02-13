package com.example.tarotsolitaire;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class StartActivity extends BaseActivity {

    private static final String TAG = "StartActivity";

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keep the activity minimal; no layout needed, but maintain edge-to-edge in case
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start_stub);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.start_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Log Firebase initialization details for debugging
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            if (app != null) {
                FirebaseOptions opts = app.getOptions();
                Log.i(TAG, "FirebaseApp present: projectId=" + opts.getProjectId() + ", applicationId=" + opts.getApplicationId());
            } else {
                Log.w(TAG, "FirebaseApp.getInstance returned null");
            }
        } catch (IllegalStateException ise) {
            Log.w(TAG, "No default FirebaseApp: " + ise.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error reading FirebaseApp options", e);
        }

        auth = FirebaseAuth.getInstance();

        // Decide where to navigate
        if (auth != null && auth.getCurrentUser() != null) {
            // User is signed in; fetch profile and then go to MainMenu
            Log.i(TAG, "User already signed in (uid=" + auth.getCurrentUser().getUid() + ") - loading profile and launching MainMenu");
            fetchProfileAndStartMainMenu();
        } else {
            Log.i(TAG, "No user signed in - launching loginPage");
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(StartActivity.this, loginPage.class);
        startActivity(intent);
        finish();
    }

    private void fetchProfileAndStartMainMenu() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "fetchProfileAndStartMainMenu: currentUser is null");
            navigateToLogin();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String nickname = document.getString("nickname");
                                Log.d(TAG, "Profile found, nickname=" + nickname);
                                if (nickname != null) saveUserDataLocally(nickname);
                                startMainMenu(true);
                            } else {
                                Log.i(TAG, "User profile missing; creating default profile doc for uid=" + userId);
                                Map<String, Object> userMap = new HashMap<>();
                                String email = auth.getCurrentUser().getEmail();
                                final String defaultNickname = "";
                                if (email != null && email.contains("@")) {
                                    final String computedNick = email.substring(0, email.indexOf('@'));
                                    userMap.put("nickname", computedNick);
                                } else {
                                    userMap.put("nickname", defaultNickname);
                                }
                                userMap.put("email", email);
                                userMap.put("createdAt", FieldValue.serverTimestamp());
                                userMap.put("bestTime", null);
                                userMap.put("times", new ArrayList<Long>());

                                firestore.collection("users").document(userId).set(userMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                try {
                                                    Object nickObj = userMap.get("nickname");
                                                    if (nickObj instanceof String) saveUserDataLocally((String) nickObj);
                                                } catch (Exception ignored) {}
                                                startMainMenu(true);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(TAG, "Failed to create default profile, continuing anyway", e);
                                                Toast.makeText(StartActivity.this, "Couldn't save profile — continuing", Toast.LENGTH_LONG).show();
                                                startMainMenu(true);
                                            }
                                        });
                            }
                        } else {
                            Exception ex = task.getException();
                            Log.w(TAG, "Could not read user document to update bestTime", ex);
                            Toast.makeText(StartActivity.this, "Couldn't load profile — continuing", Toast.LENGTH_LONG).show();
                            startMainMenu(true);
                        }
                    }
                });
    }

    private void saveUserDataLocally(String nickname){
        try {
            getSharedPreferences("userInfo", MODE_PRIVATE).edit().putString("nickname", nickname).apply();
        } catch (Exception e) {
            Log.w(TAG, "Failed to save user nickname locally", e);
        }
    }

    private void startMainMenu(boolean sendToast) {
        if(sendToast) Toast.makeText(StartActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(StartActivity.this, MainMenu.class);
        startActivity(intent);
        finish();
    }
}

