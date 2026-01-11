package com.example.tarotsolitaire;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout; // <-- IMPORT THE CORRECT LAYOUT

import java.util.ArrayList;
import java.util.List;

public class GamePage extends AppCompatActivity {

    ConstraintLayout root;

    // The rest of the views are still FrameLayouts, so these are correct.
    FrameLayout playArea;
    FrameLayout organizeArea;

    // --- The rest of your logic remains unchanged as it was correct. ---

    // Lists for both the UI views AND the game logic objects
    List<PileView> leftPileViews;
    List<Pile> leftPileLogics; // LOGIC
    List<PileView> rightPileViews;
    List<Pile> rightPileLogics; // LOGIC
    List<PileView> allPiles; // Master list of views for CardView

    private static final String TAG = "GamePage";


    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_page);

        // --- THIS IS THE FIX ---
        // The root variable is now correctly typed as ConstraintLayout to match your XML.
        root = findViewById(R.id.rootLayout);

        // The rest of the views are still FrameLayouts, so these are correct.
         playArea = findViewById(R.id.playArea);
         organizeArea = findViewById(R.id.organizeArea);

        // --- The rest of your logic remains unchanged as it was correct. ---

        // Lists for both the UI views AND the game logic objects
        leftPileViews = new ArrayList<>();
        leftPileLogics = new ArrayList<>(); // LOGIC
        rightPileViews = new ArrayList<>();
        rightPileLogics = new ArrayList<>(); // LOGIC
        allPiles = new ArrayList<>(); // Master list of views for CardView


    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // hasFocus is true when the Activity is visible and interactable
        if (!hasFocus) {
            return;
        }


        int playAreaWidth = playArea.getWidth();
        int organizeAreaWidth = organizeArea.getWidth();

        Log.d(TAG, "onWindowFocusChanged: play area width: " + playAreaWidth);
        Log.d(TAG, "onWindowFocusChanged: organize area width: " + organizeAreaWidth);


        int pileWidth = playAreaWidth / 9;
        int pileHeight = dp(100);
        int cardWidth = playAreaWidth / 9 - 20;
        int cardHeight = dp(90);

        /* ---------- 1. CREATE ALL VIEWS AND LOGIC ---------- */

        // LEFT PLAY AREA (9 piles)
        for (int i = 0; i < 9; i++) {
            // Create and link both the view and the logic
            PileView pileView = new PileView(this);
            Pile pileLogic = new Pile();
            pileView.setLogicalPile(pileLogic); // The crucial link

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(pileWidth, pileHeight);
            params.leftMargin = i * pileWidth;
            params.topMargin = dp(40);

            Log.d(TAG, "onWindowFocusChanged: adding pile #" + i + ", leftMargin: " + params.leftMargin);

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
                    // Also create and link the logic here
                    PileView pileView = new PileView(this);
                    Pile pileLogic = new Pile();
                    pileView.setLogicalPile(pileLogic); // The crucial link

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
            for (Card card : deck.getCards()) {
                if (pileIndex == 4) { // Skip 5th pile
                    pileIndex = (pileIndex + 1) % leftPileViews.size();
                }

                PileView targetPileView = leftPileViews.get(pileIndex);

                // Create the CardView, passing it the master list of all *visual* piles
                CardView cardView = new CardView(this, allPiles, card);

                // We add the card to the root so it can be dragged anywhere on screen.
                ConstraintLayout.LayoutParams cardParams = new ConstraintLayout.LayoutParams(cardWidth, cardHeight);
                root.addView(cardView, cardParams);

                // Use post() to ensure the cardView is measured before its initial snap
                final PileView finalTargetPile = targetPileView;
                cardView.post(() -> cardView.snapToPile(finalTargetPile, false));

                pileIndex = (pileIndex + 1) % leftPileViews.size();
            }
        });
    }
}
