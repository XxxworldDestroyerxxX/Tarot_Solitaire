package com.example.tarotsolitaire;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public class GamePage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_page);

        FrameLayout rootLayout = findViewById(R.id.rootLayout);

        CardView cardView = new CardView(this);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dpToPx(80),
                dpToPx(120)
        );
        params.leftMargin = dpToPx(100);
        params.topMargin = dpToPx(100);

        rootLayout.addView(cardView, params);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
