package com.example.tarotsolitaire;
import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class RegistrationManager {
    private static final String TAG = "RegistrationManager";

    private static final int REGISTRATION_PHASE_VALIDATE_USER_INFO = 0;
    private static final int REGISTRATION_PHASE_CREATE_USER = 1;
    private static final int REGISTRATION_PHASE_UPLOAD_PIC = 2;
    private static final int REGISTRATION_PHASE_UPLOAD_DATA = 3;
    private static final int REGISTRATION_PHASE_DONE = 4;
    private int registrationPhase;

    String email;
    String password;
    FirebaseAuth auth;
    String userId;
    Activity activity;
    String nickname;

    OnResultCallback onResultCallback;

    public RegistrationManager(Activity activity) {
        Log.d(TAG, "RegistrationManager: started");
        this.activity = activity;
        auth = FirebaseAuth.getInstance();
        registrationPhase = REGISTRATION_PHASE_VALIDATE_USER_INFO;

    }

    public void startRegistration(
                                  String email,
                                  String password,
                                  String nickname,
                                  OnResultCallback onResultCallback)
    {
        this.onResultCallback = onResultCallback;
        this.email = email;
        this.password = password;
        this.nickname = nickname;

        executeNextPhase();
    }


    public interface OnResultCallback {
        void onResult(boolean success, String message);
    }


    private void phaseDone()
    {
        registrationPhase++;
        executeNextPhase();
    }

    private void phaseFailed(String message)
    {
        Log.e(TAG, "phaseFailed: registration failed: message: " + message);
        registrationPhase = REGISTRATION_PHASE_VALIDATE_USER_INFO;
        onResultCallback.onResult(false, message);
    }

    private void executeNextPhase()
    {
        Log.d(TAG, "executeNextPhase: executing phase: " + registrationPhase);

        if(registrationPhase == REGISTRATION_PHASE_VALIDATE_USER_INFO)
        {
            Log.i(TAG, "executeNextPhase: fetching user info from form");
            validateUserInfo();
        }
        else if(registrationPhase == REGISTRATION_PHASE_CREATE_USER)
        {
            Log.i(TAG, "executeNextPhase: Creating user with Firebase Auth");
            createUser();
        }
        else if(registrationPhase == REGISTRATION_PHASE_UPLOAD_DATA)
        {
            Log.i(TAG, "executeNextPhase: Uploading user data to firestore");
            saveUserToFirestore();
        }
        else if(registrationPhase == REGISTRATION_PHASE_DONE)
        {
            Log.i(TAG, "executeNextPhase: Registration done");
            onResultCallback.onResult(true, "Registration successful!");
            // Leave user signed in so caller can proceed; caller can sign out if needed
        }
    }

    private void validateUserInfo() {
        Log.d(TAG, "validateUserInfo: validating user info");


        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(nickname)) {
            Log.w(TAG, "Validation failed: missing fields");
            phaseFailed("Please fill in all fields");
            return;
        }

        // basic email format check
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.w(TAG, "Validation failed: invalid email format: " + email);
            phaseFailed("Invalid email format");
            return;
        }

        if (password.length() < 6) {
            Log.w(TAG, "Validation failed: password too short");
            phaseFailed("Password must be at least 6 characters");
            return;
        }

        phaseDone();
    }

    private void createUser()
    {
        Log.d(TAG, "createUser: Creating user with Firebase Auth for email: " + email);

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                userId = user.getUid();
                                Log.i(TAG, "Firebase Auth registration successful. UID: " + userId);
                                phaseDone();
                            } else {
                                Log.e(TAG, "Firebase Auth registration succeeded but user is null");
                                phaseFailed("Unexpected error: user is null after registration");
                            }
                        } else {
                            Exception ex = task.getException();
                            Log.e(TAG, "Firebase Auth registration failed", ex);
                            if (ex instanceof FirebaseAuthUserCollisionException) {
                                phaseFailed("Email already in use. Please login instead.");
                            } else {
                                String msg = ex != null ? (ex.getClass().getSimpleName() + ": " + ex.getMessage()) : "Unknown error";
                                phaseFailed(msg);
                            }
                        }
                    }
                });
    }


    private void saveUserToFirestore() {
        Log.d(TAG, "Saving user to Firestore. UID: " + userId + ", Nickname: " + nickname + ", email: " + email);
        if (userId == null) {
            phaseFailed("No user id available to save data");
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("nickname", nickname);
        userMap.put("email", email);
        userMap.put("createdAt", FieldValue.serverTimestamp());
        userMap.put("bestTime", null);
        userMap.put("times", new ArrayList<Long>());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "User document created in Firestore for UID: " + userId);
                        phaseDone();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to save user data to Firestore", e);
                        phaseFailed("Failed to save user data: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                });
    }

}
