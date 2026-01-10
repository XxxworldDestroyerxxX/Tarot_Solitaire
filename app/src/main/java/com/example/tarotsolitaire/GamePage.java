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

        List<PileView> leftPiles = new ArrayList<>();
        List<PileView> rightPiles = new ArrayList<>();

        int pileWidth = dp(70);
        int pileHeight = dp(100);
        int cardWidth = dp(60);
        int cardHeight = dp(90);

        /* ---------- LEFT PLAY AREA (9 piles) ---------- */
        for (int i = 0; i < 9; i++) {
            PileView pile = new PileView(this);
            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(pileWidth, pileHeight);

            params.leftMargin = dp(i * 80);
            params.topMargin = dp(40);

            playArea.addView(pile, params);
            leftPiles.add(pile); // keep track of left piles
        }

        /* ---------- RIGHT ORGANIZE AREA (6 piles) ---------- */
        organizeArea.post(() -> {
            int areaHeight = organizeArea.getHeight();
            int rows = 3;
            int cols = 2;
            int spacingX = 0;
            int spacingY = (areaHeight - rows * pileHeight) / (rows + 1);

            for (int col = 0; col < cols; col++) {
                for (int row = 0; row < rows; row++) {
                    PileView pile = new PileView(this);
                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(pileWidth, pileHeight);

                    params.leftMargin = col * (pileWidth + spacingX);
                    params.topMargin = spacingY + row * (pileHeight + spacingY);

                    organizeArea.addView(pile, params);
                    rightPiles.add(pile); // keep track of right piles
                }
            }

            /* ---------- DEAL ENTIRE DECK TO LEFT PILES (WITH FIX) ---------- */
            Deck deck = new Deck();
            deck.shuffle();

            List<PileView> allPiles = new ArrayList<>();
            allPiles.addAll(leftPiles);
            allPiles.addAll(rightPiles);

            int pileIndex = 0;
            List<Card> cardsToDeal = deck.getCards();
            // Use a traditional for loop to have more control over the index
            for (int i = 0; i < cardsToDeal.size(); i++) {
                Card card = cardsToDeal.get(i);

                // --- THIS IS THE FIX ---
                // If the current pile index is 4 (the 5th pile), skip it by advancing the index.
                if (pileIndex == 4) {
                    pileIndex = (pileIndex + 1) % leftPiles.size();
                }

                // Now get the pile using the potentially corrected index
                PileView pile = leftPiles.get(pileIndex);
                CardView cardView = new CardView(this, allPiles, card);

                FrameLayout.LayoutParams cardParams =
                        new FrameLayout.LayoutParams(cardWidth, cardHeight);
                root.addView(cardView, cardParams);

                // Snap card to its starting pile
                // We use a final variable to pass the correct pile to the lambda
                final PileView targetPile = pile;
                cardView.post(() -> cardView.snapToPile(targetPile));

                // Advance to the next pile index for the next card
                pileIndex = (pileIndex + 1) % leftPiles.size();
            }
        });
    }
}
