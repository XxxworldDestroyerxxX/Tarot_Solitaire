package com.example.tarotsolitaire;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.text.BreakIterator;

public class loginPage extends AppCompatActivity {

    FirebaseAuth auth;
    private EditText emailEditText;
    private EditText passwordEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;


        });
        // Log Firebase initialization details for debugging
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            if (app != null) {
                FirebaseOptions opts = app.getOptions();
                Log.i("LoginActivity", "FirebaseApp present: projectId=" + opts.getProjectId() + ", applicationId=" + opts.getApplicationId());
            } else {
                Log.w("LoginActivity", "FirebaseApp.getInstance returned null");
            }
        } catch (IllegalStateException ise) {
            Log.w("LoginActivity", "No default FirebaseApp: " + ise.getMessage());
        } catch (Exception e) {
            Log.e("LoginActivity", "Error reading FirebaseApp options", e);
        }

        auth = FirebaseAuth.getInstance();

        // Initialize the EditText variables using findViewById
        emailEditText = findViewById(R.id.et_email);
        passwordEditText = findViewById(R.id.et_password);
        Button continueButton = findViewById(R.id.btn_continue);

        if (emailEditText == null) {
            Log.e(TAG, "Email EditText not found!");
        }
        if (passwordEditText == null) {
            Log.e(TAG, "Password EditText not found!");
        }

        TextView registerLinkTextView = findViewById(R.id.link_register);
        registerLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(loginPage.this, RegistrationPage.class);
                startActivity(intent);
                // don't finish() here to avoid disposing the login activity immediately


            }
        });

        Button loginBtn = findViewById(R.id.btn_login);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performLogin();
            }
        });

        // If user already signed in, show Continue button (don't auto-navigate)
        if (auth != null && auth.getCurrentUser() != null) {
            Log.i("LoginActivity", "User already signed in (uid=" + auth.getCurrentUser().getUid() + ")");
            continueButton.setVisibility(View.VISIBLE);
            continueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(loginPage.this, MainMenu.class);
                    startActivity(intent);
                    finish();
                }
            });
        } else {
            Log.i("LoginActivity", "No user currently signed in");
        }


    }
    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            Log.w("LoginActivity", "Empty email and/or password field");
            Toast.makeText(loginPage.this, "Please fill in all fields", Toast.LENGTH_LONG).show();
            return;
        }

        // Perform Firebase authentication
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.i("LoginActivity", "signIn:success");
                        getUserDataFromFirestore();
                    } else {
                        // If sign in fails, display a message to the user.
                        Exception ex = task.getException();
                        Log.w("LoginActivity", "signIn:failure", ex);

                        String errorMessage = "Authentication failed.";

                        if (ex != null) {
                            errorMessage += " (" + ex.getClass().getSimpleName() + ") " + ex.getMessage();
                        }

                        Toast.makeText(loginPage.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startMainMenu(boolean sendToast) {
        if(sendToast)
            Toast.makeText(loginPage.this, "Login successful!", Toast.LENGTH_SHORT).show();

        // Navigate to FeedActivity
        Intent intent = new Intent(loginPage.this, MainMenu.class);
        startActivity(intent);
        finish();
    }
    private void getUserDataFromFirestore() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "getUserDataFromFirestore: currentUser is null after sign-in");
            Toast.makeText(loginPage.this, "Error: user not signed in", Toast.LENGTH_LONG).show();
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
                                // User data exists, you can use it
                                String nickname = document.getString("nickname");
                                Log.d(TAG, "getUserDataFromFirestore onComplete: nickname: " + nickname);

                                Toast.makeText(loginPage.this, "Login successful!", Toast.LENGTH_SHORT).show();

                                // Navigate to FeedActivity
                                startMainMenu(true);

                            } else {
                                // User data doesn't exist — create a default profile document so future features have data
                                Log.i(TAG, "User profile missing; creating default profile doc for uid=" + userId);
                                Map<String, Object> userMap = new HashMap<>();
                                String email = auth.getCurrentUser().getEmail();
                                String defaultNickname = "";
                                if (email != null && email.contains("@")) {
                                    defaultNickname = email.substring(0, email.indexOf('@'));
                                }
                                userMap.put("nickname", defaultNickname);
                                userMap.put("email", email);
                                userMap.put("createdAt", FieldValue.serverTimestamp());
                                userMap.put("bestTime", null);
                                userMap.put("times", new ArrayList<Long>());

                                firestore.collection("users").document(userId).set(userMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.i(TAG, "Default user profile created for uid=" + userId);
                                                startMainMenu(true);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(TAG, "Failed to create default profile, continuing anyway", e);
                                                Toast.makeText(loginPage.this, "Couldn't save profile — continuing", Toast.LENGTH_LONG).show();
                                                startMainMenu(true);
                                            }
                                        });
                            }
                        } else {
                            // Handle errors
                            Exception ex = task.getException();
                            Log.d(TAG, "getUserData onComplete: error: ", ex);
                            String err = "Couldn't load profile data; continuing" + (ex != null ? (": (" + ex.getClass().getSimpleName() + ") " + ex.getMessage()) : "");
                            // Show a short, non-alarming toast so the user knows we continue despite an issue
                            Toast.makeText(loginPage.this, "Couldn't load profile — continuing", Toast.LENGTH_LONG).show();
                            startMainMenu(true);
                        }
                    }

                });
    }

    private void saveUserDataLocally(String nickname){
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nickname", nickname);
        editor.apply();
    }

}