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

        // --- Lists for both the UI views AND the game logic objects ---
        List<PileView> leftPileViews = new ArrayList<>();
        List<Pile> leftPileLogics = new ArrayList<>(); // LOGIC
        List<PileView> rightPileViews = new ArrayList<>();
        List<Pile> rightPileLogics = new ArrayList<>(); // LOGIC
        List<PileView> allPiles = new ArrayList<>(); // Master list of views for CardView

        int pileWidth = dp(70);
        int pileHeight = dp(100);
        int cardWidth = dp(60);
        int cardHeight = dp(90);

        /* ---------- 1. CREATE ALL VIEWS AND LOGIC ---------- */

        // LEFT PLAY AREA (9 piles)
        for (int i = 0; i < 9; i++) {
            // --- FIX: Create and link both the view and the logic ---
            PileView pileView = new PileView(this);
            Pile pileLogic = new Pile();
            pileView.setLogicalPile(pileLogic); // <-- The crucial link

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(pileWidth, pileHeight);
            params.leftMargin = dp(i * 80);
            params.topMargin = dp(40);

            playArea.addView(pileView, params);
            leftPileViews.add(pileView);
            leftPileLogics.add(pileLogic);
        }

        /* ---------- 2. WAIT FOR LAYOUT, THEN CREATE RIGHT PILES & DEAL ---------- */
        organizeArea.post(() -> {
            // RIGHT ORGANIZE AREA (6 piles)
            int areaHeight = organizeArea.getHeight();
            int rows = 3;
            int cols = 2;
            int spacingX = 0;
            int spacingY = (areaHeight - rows * pileHeight) / (rows + 1);

            for (int col = 0; col < cols; col++) {
                for (int row = 0; row < rows; row++) {
                    // --- FIX: Also create and link the logic here ---
                    PileView pileView = new PileView(this);
                    Pile pileLogic = new Pile();
                    pileView.setLogicalPile(pileLogic); // <-- The crucial link

                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(pileWidth, pileHeight);
                    params.leftMargin = col * (pileWidth + spacingX);
                    params.topMargin = spacingY + row * (pileHeight + spacingY);

                    organizeArea.addView(pileView, params);
                    rightPileViews.add(pileView);
                    rightPileLogics.add(pileLogic);
                }
            }

            // --- Create the master 'allPiles' list of VIEWS ---
            allPiles.clear();
            allPiles.addAll(leftPileViews);
            allPiles.addAll(rightPileViews);

            /* ---------- 3. DEAL CARDS ---------- */
            Deck deck = new Deck();
            deck.shuffle();

            int pileIndex = 0;
            List<Card> cardsToDeal = deck.getCards();
            for (Card card : cardsToDeal) {
                if (pileIndex == 4) { // Skip 5th pile
                    pileIndex = (pileIndex + 1) % leftPileViews.size();
                }

                PileView targetPileView = leftPileViews.get(pileIndex);

                // Create the CardView, passing it the master list of all *visual* piles
                CardView cardView = new CardView(this, allPiles, card);

                FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(cardWidth, cardHeight);
                root.addView(cardView, cardParams);

                // Use post() to ensure the cardView is measured before its initial snap
                final PileView finalTargetPile = targetPileView;
                cardView.post(() -> cardView.snapToPile(finalTargetPile, false));

                pileIndex = (pileIndex + 1) % leftPileViews.size();
            }
        });
    }
}
