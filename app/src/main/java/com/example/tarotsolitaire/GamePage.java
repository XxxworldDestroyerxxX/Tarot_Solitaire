package com.example.tarotsolitaire;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.util.TypedValue;
import java.util.ArrayList;
import java.util.List;

public class GamePage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This line inflates our new, powerful, declarative XML layout.
        setContentView(R.layout.activity_game_page);

        ConstraintLayout root = findViewById(R.id.rootLayout);

        // --- Lists to hold our views and logic objects ---
        List<PileView> leftPileViews = new ArrayList<>();
        List<PileView> rightPileViews = new ArrayList<>();
        List<PileView> allPiles = new ArrayList<>();

        // --- 1. FIND ALL VIEWS FROM THE XML ---
        // We find them by the IDs assigned in the XML file.
        leftPileViews.add(findViewById(R.id.playPile1));
        leftPileViews.add(findViewById(R.id.playPile2));
        leftPileViews.add(findViewById(R.id.playPile3));
        leftPileViews.add(findViewById(R.id.playPile4));
        leftPileViews.add(findViewById(R.id.playPile5_empty));
        leftPileViews.add(findViewById(R.id.playPile6));
        leftPileViews.add(findViewById(R.id.playPile7));
        leftPileViews.add(findViewById(R.id.playPile8));
        leftPileViews.add(findViewById(R.id.playPile9));

        rightPileViews.add(findViewById(R.id.organizePile1));
        rightPileViews.add(findViewById(R.id.organizePile2));
        rightPileViews.add(findViewById(R.id.organizePile3));
        rightPileViews.add(findViewById(R.id.organizePile4));
        rightPileViews.add(findViewById(R.id.organizePile5));
        rightPileViews.add(findViewById(R.id.organizePile6));

        // --- 2. CREATE AND LINK THE LOGIC ---
        // This part is the same: we give each PileView its logical "brain".
        for (PileView pileView : leftPileViews) {
            Pile pileLogic = new Pile();
            pileView.setLogicalPile(pileLogic);
        }
        for (PileView pileView : rightPileViews) {
            Pile pileLogic = new Pile();
            pileView.setLogicalPile(pileLogic);
        }

        // --- Create the master list of all UI piles ---
        allPiles.addAll(leftPileViews);
        allPiles.addAll(rightPileViews);


        // --- 3. DEAL THE CARDS ---
        // We use post() to ensure the XML layout has been fully measured before we deal.
        // This guarantees that getWidth() and getHeight() will return correct values.
        root.post(() -> {
            // Compute responsive sizes based on the measured root view.
            int rootW = root.getWidth();
            int rootH = root.getHeight();

            // Read base sizes directly from resources (pixel sizes)
            int pileWidth = getResources().getDimensionPixelSize(R.dimen.pile_width);
            int pileHeight = getResources().getDimensionPixelSize(R.dimen.pile_height);

            // Safety: if too many piles won't fit, scale down slightly (rare)
            int totalPlayPiles = leftPileViews.size();
            int spacing = (int) (pileWidth * 0.2f);
            long neededWidth = (long) totalPlayPiles * pileWidth + (totalPlayPiles - 1) * spacing + getResources().getDimensionPixelSize(R.dimen.margin_play_area_horizontal);
            if (neededWidth > rootW) {
                pileWidth = Math.max(36, (int) ((rootW - getResources().getDimensionPixelSize(R.dimen.margin_play_area_horizontal)) / (float) totalPlayPiles));
                pileHeight = (int) (pileWidth * (getResources().getDimensionPixelSize(R.dimen.pile_height) / (float) getResources().getDimensionPixelSize(R.dimen.pile_width)));

                // Apply scaled sizes if needed
                for (PileView pv : allPiles) {
                    if (pv == null) continue;
                    if (pv.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) pv.getLayoutParams();
                        lp.width = pileWidth;
                        lp.height = pileHeight;
                        pv.setLayoutParams(lp);
                    }
                }
            }

            // Build deck and deal, using card dims from resources
            Deck deck = new Deck();
            deck.shuffle();

            int cardWidth = getResources().getDimensionPixelSize(R.dimen.card_width);
            int cardHeight = getResources().getDimensionPixelSize(R.dimen.card_height);

            // Deal cards across left piles (skipping the empty pile at index 4)
            int pileIndex = 0;
            for (Card card : deck.getCards()) {
                if (pileIndex == 4) { // Skip 5th pile (index 4)
                    pileIndex = (pileIndex + 1) % leftPileViews.size();
                }

                PileView targetPileView = leftPileViews.get(pileIndex);

                CardView cardView = new CardView(this, allPiles, card);
                ConstraintLayout.LayoutParams cardParams = new ConstraintLayout.LayoutParams(cardWidth, cardHeight);
                // Keep card params unconstrained; snapToPile will place them
                root.addView(cardView, cardParams);

                // Use post() on the cardView to ensure it's measured before its initial snap.
                cardView.post(() -> cardView.snapToPile(targetPileView, false));

                pileIndex = (pileIndex + 1) % leftPileViews.size();
            }
        });
    }
}
