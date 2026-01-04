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

            params.leftMargin = dp(i * 80); // spacing 80dp
            params.topMargin = dp(40);

            playArea.addView(pile, params);
            piles.add(pile);

            // Add one card to this pile
            CardView card = new CardView(this, piles);
            FrameLayout.LayoutParams cardParams =
                    new FrameLayout.LayoutParams(cardWidth, cardHeight);
            root.addView(card, cardParams);
            card.post(() -> card.snapToPile(pile));
        }

        /* ---------- RIGHT ORGANIZE AREA (2 columns × 3 rows = 6 piles) ---------- */
        organizeArea.post(() -> {
            int areaHeight = organizeArea.getHeight();
            int rows = 3;
            int cols = 2;

            int spacingX = 0; // no horizontal spacing needed, fits exactly
            int spacingY = (areaHeight - rows * pileHeight) / (rows + 1); // evenly distribute vertically

            for (int col = 0; col < cols; col++) {
                for (int row = 0; row < rows; row++) {
                    PileView pile = new PileView(this);
                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(pileWidth, pileHeight);

                    // Horizontal relative to organizeArea
                    params.leftMargin = col * (pileWidth + spacingX);
                    // Vertical with evenly distributed spacing
                    params.topMargin = spacingY + row * (pileHeight + spacingY);

                    organizeArea.addView(pile, params);
                    piles.add(pile);

                    // Add one card to this pile
                    CardView card = new CardView(this, piles);
                    FrameLayout.LayoutParams cardParams =
                            new FrameLayout.LayoutParams(cardWidth, cardHeight);
                    root.addView(card, cardParams);
                    card.post(() -> card.snapToPile(pile));
                }
            }
        });
    }
}
