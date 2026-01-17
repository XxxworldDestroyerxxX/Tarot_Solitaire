package com.example.tarotsolitaire;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
            Deck deck = new Deck();
            deck.shuffle();

            // Set the card size based on the size of a pile that now exists on screen.
            // This is how the cards also become responsive!
            int cardWidth = (int) getResources().getDimension(R.dimen.card_width);
            int cardHeight = (int) getResources().getDimension(R.dimen.card_height);

            int pileIndex = 0;
            for (Card card : deck.getCards()) {
                if (pileIndex == 4) { // Skip 5th pile (index 4)
                    pileIndex = (pileIndex + 1) % leftPileViews.size();
                }

                PileView targetPileView = leftPileViews.get(pileIndex);

                CardView cardView = new CardView(this, allPiles, card);
                ConstraintLayout.LayoutParams cardParams = new ConstraintLayout.LayoutParams(cardWidth, cardHeight);
                root.addView(cardView, cardParams);

                // Use post() on the cardView to ensure it's measured before its initial snap.
                cardView.post(() -> cardView.snapToPile(targetPileView, false));

                pileIndex = (pileIndex + 1) % leftPileViews.size();
            }
        });
    }
}
