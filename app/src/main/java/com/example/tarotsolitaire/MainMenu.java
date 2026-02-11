package com.example.tarotsolitaire;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainMenu extends BaseActivity {

    private MusicManager.Listener musicListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Show locally saved nickname if available
        String nick = getSavedNickname();
        if (nick != null && !nick.isEmpty()) {
            View root = findViewById(R.id.main);
            // try to find existing TextView with id tv_username
            int tvId = getResources().getIdentifier("tv_username", "id", getPackageName());
            TextView tv = null;
            if (tvId != 0) {
                View maybe = findViewById(tvId);
                if (maybe instanceof TextView) tv = (TextView) maybe;
            }
            if (tv != null) {
                tv.setText("Hello, " + nick);
            } else if (root instanceof LinearLayout) {
                TextView tvNew = new TextView(this);
                tvNew.setText("Hello, " + nick);
                tvNew.setTextSize(16f);
                ((LinearLayout) root).addView(tvNew, 0);
            }
        }

        TextView now = findViewById(R.id.tv_now_playing);
        MusicManager mgr = MusicManager.get(this);
        now.setText("Now playing: " + mgr.getCurrentTitle());
        musicListener = (isPlaying, idx, title) -> runOnUiThread(() -> now.setText("Now playing: " + title));
        mgr.registerListener(musicListener);

        Button toGame = findViewById(R.id.btn_toGame);
        toGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenu.this, GamePage.class);
                startActivity(intent);
            }
        });

        Button Options = findViewById(R.id.btn_Options);
        Options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenu.this, optionsPage.class);
                startActivity(intent);
            }
        });

        Button Leaderboard = findViewById(R.id.btn_Leaderboard);
        Leaderboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenu.this, Leaderboard.class);
                startActivity(intent);
            }
        });

        Button signOut = findViewById(R.id.btn_signout);
        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainMenu.this, loginPage.class);
                startActivity(intent);
                finish();
            }
        });

        Button statsPage = findViewById(R.id.btn_statsPage);
        statsPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenu.this, StatsPage.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicListener != null) {
            MusicManager mgr = MusicManager.get(this);
            mgr.unregisterListener(musicListener);
            musicListener = null;
        }
    }

    private String getSavedNickname() {
        SharedPreferences prefs = getSharedPreferences("userInfo", MODE_PRIVATE);
        return prefs.getString("nickname", "");
    }
}