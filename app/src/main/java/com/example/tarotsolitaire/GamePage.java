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
        leftPileViews.add(findViewById(R.id.playPile5));
        leftPileViews.add(findViewById(R.id.playPile6_empty));
        leftPileViews.add(findViewById(R.id.playPile7));
        leftPileViews.add(findViewById(R.id.playPile8));
        leftPileViews.add(findViewById(R.id.playPile9));
        leftPileViews.add(findViewById(R.id.playPile10));
        leftPileViews.add(findViewById(R.id.playPile11));

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
            // Spacing factor between piles (lower value keeps piles tighter)
            float spacingFactor = 0.12f; // reduced from 0.2 to pack 11 piles more reliably
            int spacing = (int) (pileWidth * spacingFactor);

            // Compute how much horizontal space we can use for the play area.
            // Prefer measuring the actual width used by the organize area (right piles) after layout.
            int reserveLeft = getResources().getDimensionPixelSize(R.dimen.margin_play_area_horizontal);
            int measuredOrganizeWidth = 0;
            for (PileView pv : rightPileViews) {
                if (pv != null) {
                    int w = pv.getWidth();
                    if (w > 0) measuredOrganizeWidth = Math.max(measuredOrganizeWidth, w);
                }
            }
            int reserveRight = measuredOrganizeWidth > 0 ? measuredOrganizeWidth + 16 : getResources().getDimensionPixelSize(R.dimen.margin_organize_area);
            int availableWidth = rootW - reserveRight - reserveLeft;

            long neededWidth = (long) totalPlayPiles * pileWidth + (totalPlayPiles - 1) * spacing;
            if (neededWidth > availableWidth && availableWidth > 0) {
                // Fit the piles into availableWidth while leaving spacing between them.
                int totalSpacing = (totalPlayPiles - 1) * spacing;
                int maxPileWidth = Math.max(36, (availableWidth - totalSpacing) / Math.max(1, totalPlayPiles));
                pileWidth = maxPileWidth;
                // Preserve original aspect ratio defined in dimens
                int origPileW = getResources().getDimensionPixelSize(R.dimen.pile_width);
                int origPileH = getResources().getDimensionPixelSize(R.dimen.pile_height);
                pileHeight = (int) (pileWidth * (origPileH / (float) origPileW));

                // Apply scaled sizes to ONLY the play piles (left area) and organize piles too so they match visually
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

            // Ensure cards match pile size so they align perfectly. Use resource dims unless we scaled above.
            int cardWidth = pileWidth;
            int cardHeight = pileHeight;

            // Deal cards across left piles (skip the 6th pile which should be empty)
            int pileIndex = 0;
            int totalLeft = leftPileViews.size();
            for (Card card : deck.getCards()) {
                // If current index would be the middle empty pile (index 5), skip it
                if (pileIndex == 5) {
                    pileIndex = (pileIndex + 1) % totalLeft;
                }

                PileView targetPileView = leftPileViews.get(pileIndex);

                CardView cardView = new CardView(this);
                cardView.setAllPiles(allPiles);
                cardView.setCard(card);
                ConstraintLayout.LayoutParams cardParams = new ConstraintLayout.LayoutParams(cardWidth, cardHeight);
                // Keep card params unconstrained; snapToPile will place them
                root.addView(cardView, cardParams);

                // Use post() on the cardView to ensure it's measured before its initial snap.
                cardView.post(() -> cardView.snapToPile(targetPileView, false));

                pileIndex = (pileIndex + 1) % totalLeft;
            }
        });
    }
}
