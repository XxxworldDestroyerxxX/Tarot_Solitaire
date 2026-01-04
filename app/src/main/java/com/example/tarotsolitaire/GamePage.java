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

        // ROOT layer (cards must live here)
        FrameLayout root = findViewById(R.id.rootLayout);

        FrameLayout playArea = findViewById(R.id.playArea);
        FrameLayout organizeArea = findViewById(R.id.organizeArea);

        List<PileView> piles = new ArrayList<>();

        /* ---------- LEFT PLAY AREA (9 piles) ---------- */
        for (int i = 0; i < 9; i++) {
            PileView pile = new PileView(this);

            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(dp(60), dp(90));

            params.leftMargin = dp(20 + i * 80);
            params.topMargin = dp(40);

            playArea.addView(pile, params);
            piles.add(pile);
        }

        /* ---------- RIGHT ORGANIZE AREA (2 columns × 3 rows) ---------- */
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 3; row++) {
                PileView pile = new PileView(this);

                FrameLayout.LayoutParams params =
                        new FrameLayout.LayoutParams(dp(60), dp(90));

                params.leftMargin = dp(10 + col * 70);
                params.topMargin = dp(40 + row * 110);

                organizeArea.addView(pile, params);
                piles.add(pile);
            }
        }

        /* ---------- DRAGGABLE CARD (FLOATS ABOVE ALL AREAS) ---------- */
        CardView card = new CardView(this, piles);

        FrameLayout.LayoutParams cardParams =
                new FrameLayout.LayoutParams(dp(60), dp(90));

        cardParams.leftMargin = dp(100);
        cardParams.topMargin = dp(300);

        // ADD CARD TO ROOT, NOT PLAY AREA
        root.addView(card, cardParams);
        card.bringToFront();
    }
}
