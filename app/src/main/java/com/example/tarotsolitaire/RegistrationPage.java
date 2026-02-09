package com.example.tarotsolitaire;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tarotsolitaire.RegistrationManager;



import java.text.BreakIterator;

public class RegistrationPage extends BaseActivity {

    private EditText passwordEditText;
    private EditText TextPersonName;
    private EditText emailEditText;
    private static final String TAG = "RegistrationActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });

        TextView registerLinkTextView = findViewById(R.id.link_register);

        registerLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(RegistrationPage.this, loginPage.class);
                startActivity(intent);
                finish();

            }
        });

        passwordEditText = findViewById(R.id.et_password);
        TextPersonName = findViewById(R.id.et_nickname);
        emailEditText = findViewById(R.id.et_email);


        if (passwordEditText == null) {
            Log.e(TAG, "Password EditText not found!");
        }

        Button registerButton = findViewById(R.id.btn_register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerButtonClick();

            }
        });

    }

    private void registerButtonClick() {
        Log.d(TAG, "Register button clicked");

        RegistrationManager registrationManager = new RegistrationManager(RegistrationPage.this);
        registrationManager.startRegistration(
                // pass email and password and nickname
                emailEditText.getText().toString().trim(),
                passwordEditText.getText().toString(),
                TextPersonName.getText().toString(),
                new RegistrationManager.OnResultCallback(){
                    @Override
                    public void onResult(boolean success, String message) {
                        if (success) {
                            // Show exact requested message then navigate to login page
                            Toast.makeText(RegistrationPage.this, "registration successful", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(RegistrationPage.this, loginPage.class);
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(RegistrationPage.this, "Registration failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    }

                });
    }
}