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

        // Root layout (cards must be added here to float above piles)
        FrameLayout root = findViewById(R.id.rootLayout);

        FrameLayout playArea = findViewById(R.id.playArea);
        FrameLayout organizeArea = findViewById(R.id.organizeArea);

        List<PileView> piles = new ArrayList<>();

        int pileWidth = dp(70);
        int pileHeight = dp(100);
        int cardWidth = dp(60);
        int cardHeight = dp(90);

        /* ---------- LEFT PLAY AREA (9 piles) ---------- */
        for (int i = 0; i < 9; i++) {
            PileView pile = new PileView(this);

            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(pileWidth, pileHeight);

            // spacing 80dp apart
            params.leftMargin = dp(20 + i * 80);
            params.topMargin = dp(40);

            playArea.addView(pile, params);
            piles.add(pile);

            // Add a card on top of this pile
            CardView card = new CardView(this, piles);
            FrameLayout.LayoutParams cardParams =
                    new FrameLayout.LayoutParams(cardWidth, cardHeight);
            root.addView(card, cardParams);

            // Snap card to pile after layout
            card.post(() -> card.snapToPile(pile));
        }

        /* ---------- RIGHT ORGANIZE AREA (2 columns × 3 rows = 6 piles) ---------- */
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 3; row++) {
                PileView pile = new PileView(this);

                FrameLayout.LayoutParams params =
                        new FrameLayout.LayoutParams(pileWidth, pileHeight);

                // wider spacing for right area
                params.leftMargin = dp(10 + col * 90);
                params.topMargin = dp(40 + row * 120);

                organizeArea.addView(pile, params);
                piles.add(pile);

                // Add a card on top of this pile
                CardView card = new CardView(this, piles);
                FrameLayout.LayoutParams cardParams =
                        new FrameLayout.LayoutParams(cardWidth, cardHeight);
                root.addView(card, cardParams);

                // Snap card to pile after layout
                card.post(() -> card.snapToPile(pile));
            }
        }
    }
}
