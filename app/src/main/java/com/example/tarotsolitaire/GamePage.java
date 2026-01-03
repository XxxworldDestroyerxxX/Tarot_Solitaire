package com.example.tarotsolitaire;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class GamePage extends AppCompatActivity {

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_page);

        FrameLayout root = findViewById(R.id.rootLayout);

        List<PileView> piles = new ArrayList<>();

        // create 4 piles
        for (int i = 0; i < 4; i++) {
            PileView pile = new PileView(this);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    dp(80),
                    dp(120)
            );

            params.leftMargin = dp(40 + i * 100);
            params.topMargin = dp(40);

            root.addView(pile, params);
            piles.add(pile);
        }

        // create draggable card
        CardView card = new CardView(this, piles);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                dp(80),
                dp(120)
        );
        cardParams.leftMargin = dp(100);
        cardParams.topMargin = dp(300);

        root.addView(card, cardParams);
    }
}
