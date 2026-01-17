package com.example.tarotsolitaire;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.util.Log;
import android.widget.FrameLayout;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;

public class GamePage extends AppCompatActivity {

    private static final String TAG = "GamePage";

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
        // Optional storage slot between suit piles
        PileView organizeStoreView = findViewById(R.id.organizeStore);

        // --- 2. CREATE AND LINK THE LOGIC ---
        // This part is the same: we give each PileView its logical "brain".
        for (PileView pileView : leftPileViews) {
            Pile pileLogic = new Pile();
            pileView.setLogicalPile(pileLogic);
        }
        // Right-side organize piles are special: they accept only certain cards and overlay them.
        for (int i = 0; i < rightPileViews.size(); i++) {
            PileView pileView = rightPileViews.get(i);
            if (pileView == null) continue;

            // Map the first four organize piles to suits. Assumption (change if needed):
            // index 0 -> HEARTS, 1 -> DIAMONDS, 2 -> CLUBS, 3 -> SPADES
            if (i <= 3) {
                final Card.Suit expectedSuit;
                switch (i) {
                    case 0: expectedSuit = Card.Suit.HEARTS; break;
                    case 1: expectedSuit = Card.Suit.DIAMONDS; break;
                    case 2: expectedSuit = Card.Suit.CLUBS; break;
                    default: expectedSuit = Card.Suit.SPADES; break;
                }

                SpecialPile.PlacementRule suitRule = (pile, card) -> {
                    if (card == null) return false;
                    // If the storage pile currently holds a card, suit piles are disabled
                    if (organizeStoreView != null && organizeStoreView.getLogicalPile() != null && !organizeStoreView.getLogicalPile().isEmpty()) return false;
                    if (card.getType() != Card.Type.STANDARD) return false; // only standard cards
                    if (card.getSuit() != expectedSuit) return false; // must match suit
                    // On empty pile only a '2' can be placed
                    if (pile.isEmpty()) return card.getRank() == 2;
                    // Otherwise must be exactly one higher than current top
                    Card top = pile.getTopCard();
                    if (top == null || top.getType() != Card.Type.STANDARD) return false;
                    return card.getRank() == top.getRank() + 1;
                };

                SpecialPile suitPile = new SpecialPile(suitRule);
                pileView.setLogicalPile(suitPile);
            } else if (i == 4) {
                // First bottom pile: tarot ascending from 0 upward
                SpecialPile.PlacementRule tarotAsc = (pile, card) -> {
                    if (card == null) return false;
                    if (card.getType() != Card.Type.TAROT) return false;
                    if (pile.isEmpty()) return card.getRank() == 0;
                    Card top = pile.getTopCard();
                    if (top == null || top.getType() != Card.Type.TAROT) return false;
                    return card.getRank() == top.getRank() + 1;
                };
                SpecialPile ascPile = new SpecialPile(tarotAsc);
                pileView.setLogicalPile(ascPile);
            } else if (i == 5) {
                // Second bottom pile: tarot descending from 21 downward
                SpecialPile.PlacementRule tarotDesc = (pile, card) -> {
                    if (card == null) return false;
                    if (card.getType() != Card.Type.TAROT) return false;
                    if (pile.isEmpty()) return card.getRank() == 21;
                    Card top = pile.getTopCard();
                    if (top == null || top.getType() != Card.Type.TAROT) return false;
                    return card.getRank() == top.getRank() - 1;
                };
                SpecialPile descPile = new SpecialPile(tarotDesc);
                pileView.setLogicalPile(descPile);
            } else {
                // Fallback: default special pile (tarot-only)
                SpecialPile special = new SpecialPile();
                pileView.setLogicalPile(special);
            }
        }

        // Add visual labels to the organize piles to show their rule (suit or tarot start)
         if (rightPileViews.size() >= 6) {
             rightPileViews.get(0).setLabel(getString(R.string.label_hearts));
             rightPileViews.get(1).setLabel(getString(R.string.label_diamonds));
             rightPileViews.get(2).setLabel(getString(R.string.label_clubs));
             rightPileViews.get(3).setLabel(getString(R.string.label_spades));
             rightPileViews.get(4).setLabel(getString(R.string.label_tarot_asc));
             rightPileViews.get(5).setLabel(getString(R.string.label_tarot_desc));
         }
        // If there is an organizeStore, give it a small label (hidden since view is invisible) and set logic
        if (organizeStoreView != null) {
            organizeStoreView.setLabel(getString(R.string.store_label));
            organizeStoreView.setShowLock(true);
            // Storage pile accepts any single card only.
            SpecialPile.PlacementRule storeRule = (pile, card) -> {
                if (card == null) return false;
                // allow if pile is empty (store only one card)
                return pile.isEmpty();
            };
            SpecialPile storePile = new SpecialPile(storeRule);
            organizeStoreView.setLogicalPile(storePile);
        }

        // Create win overlay and controls overlay placeholders (we'll initialize actual views once)
        final View[] winOverlayHolder = new View[1];
        final TextView[] winTextHolder = new TextView[1];
        final FrameLayout[] controlsOverlayHolder = new FrameLayout[1];
        final android.widget.Button[] restartBtnHolder = new android.widget.Button[1];
        final TextView[] timerHolder = new TextView[1];

        // Handler & runnable for live timer updates
        final Handler timerHandler = new Handler(Looper.getMainLooper());
        final Runnable[] timerRunnableHolder = new Runnable[1];

        // Initialize win overlay and controls overlay (store into holders so other code can reference them)
        View winOverlayView = new View(this);
        winOverlayView.setBackgroundColor(0xAA000000); // translucent black
        ConstraintLayout.LayoutParams winOverlayLp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        winOverlayView.setLayoutParams(winOverlayLp);
        winOverlayView.setVisibility(View.GONE);
        root.addView(winOverlayView);
        winOverlayHolder[0] = winOverlayView;

        TextView winTextView = new TextView(this);
        winTextView.setId(View.generateViewId());
        winTextView.setText(getString(R.string.you_win));
        winTextView.setTextColor(android.graphics.Color.WHITE);
        winTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        winTextView.setVisibility(View.GONE);
        ConstraintLayout.LayoutParams wtLp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        wtLp.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        wtLp.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        wtLp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        wtLp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        winTextView.setLayoutParams(wtLp);
        root.addView(winTextView);
        winTextHolder[0] = winTextView;

        // Controls overlay with persistent timer and restart button
        FrameLayout controlsOverlay = new FrameLayout(this);
        ConstraintLayout.LayoutParams coLp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        controlsOverlay.setLayoutParams(coLp);
        controlsOverlay.setClickable(false);

        // Timer TextView at bottom-left
        TextView timerView = new TextView(this);
        timerView.setText(getString(R.string.timer_initial));
        timerView.setTextColor(android.graphics.Color.WHITE);
        timerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        FrameLayout.LayoutParams timerLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        int baseMargin = (int) (8 * getResources().getDisplayMetrics().density);
        timerLp.setMargins(baseMargin, 0, 0, baseMargin);
        controlsOverlay.addView(timerView, timerLp);
        timerHolder[0] = timerView;

        // Restart button placed to the right of timer
        android.widget.Button restartBtn = new android.widget.Button(this);
        restartBtn.setText(getString(R.string.restart));
        restartBtn.setVisibility(View.VISIBLE);
        // Place restart button at top-left
        FrameLayout.LayoutParams restartLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        restartLp.setMargins(baseMargin, baseMargin, 0, 0);
        controlsOverlay.addView(restartBtn, restartLp);

        root.addView(controlsOverlay);
        controlsOverlayHolder[0] = controlsOverlay;
        restartBtnHolder[0] = restartBtn;

        // Timer start timestamp container
        final long[] startTime = new long[1];

        // Helper to check win: all cards moved from play piles into organize piles
        final Runnable[] checkWinHolder = new Runnable[1];

        // We'll assign checkWinHolder[0] below, but declare it before dealAndStart so listeners can reference it

         // Helper to clear all card views and logical piles
         Runnable clearAll = () -> {
             Log.d(TAG, "clearAll: starting to remove card views and clear piles");
             // Remove all CardView children from root and clear references in pile views and logical piles
             // 1) Remove CardViews from root
             for (int i = root.getChildCount() - 1; i >= 0; i--) {
                 android.view.View v = root.getChildAt(i);
                 if (v instanceof CardView) {
                     root.removeViewAt(i);
                 }
             }

             // 2) Clear UI references from pile views
             for (PileView pv : allPiles) {
                 if (pv == null) continue;
                 List<CardView> cvs = new java.util.ArrayList<>(pv.getCardViews());
                 for (CardView cv : cvs) {
                     pv.removeCardView(cv);
                 }
             }

             // 3) Clear logical piles
             for (PileView pv : allPiles) {
                 if (pv == null) continue;
                 Pile logic = pv.getLogicalPile();
                 if (logic == null) continue;
                 java.util.List<Card> cards = new java.util.ArrayList<>(logic.getCards());
                 for (Card c : cards) {
                     logic.removeCard(c);
                 }
             }
             Log.d(TAG, "clearAll: finished clearing piles and views");
         };

         // Deal function (clears then deals and starts timer)
         Runnable dealAndStart = () -> {
             Log.d(TAG, "dealAndStart: starting deal");
             // hide win UI if shown
             winOverlayHolder[0].setVisibility(View.GONE);
             winTextHolder[0].setVisibility(View.GONE);

             // Clear existing
             clearAll.run();

             // Now recompute pile sizes if needed (reuse existing logic minimal)
             int pileWidth = getResources().getDimensionPixelSize(R.dimen.pile_width);
             int pileHeight = getResources().getDimensionPixelSize(R.dimen.pile_height);
             if (!allPiles.isEmpty() && allPiles.get(0) != null) {
                 int w = allPiles.get(0).getWidth();
                 int h = allPiles.get(0).getHeight();
                 if (w > 0) pileWidth = w;
                 if (h > 0) pileHeight = h;
             }

             // Build deck and create card views
             Deck deck = new Deck();
             deck.shuffle();

             int cardWidth = pileWidth;
             int cardHeight = pileHeight;

             int pileIndex = 0;
             int totalLeft = leftPileViews.size();
             for (Card card : deck.getCards()) {
                 if (pileIndex == 5) pileIndex = (pileIndex + 1) % totalLeft;
                 PileView targetPileView = leftPileViews.get(pileIndex);

                 CardView cardView = new CardView(this);
                 cardView.setAllPiles(allPiles);
                 cardView.setCard(card);
                 cardView.setOnPlacedListener((placedCard, placedPile) -> runOnUiThread(checkWinHolder[0]));
                 ConstraintLayout.LayoutParams cardParams = new ConstraintLayout.LayoutParams(cardWidth, cardHeight);
                 root.addView(cardView, cardParams);
                 Log.d(TAG, "dealAndStart: added CardView for " + card + " to root");
                 // Capture values for lambda (pileIndex is mutated each iteration)
                 final int curIndex = pileIndex;
                 final Card curCard = card;
                 final PileView target = targetPileView;
                 cardView.post(() -> {
                     cardView.snapToPile(target, false);
                     Log.d(TAG, "dealAndStart: snapped CardView for " + curCard + " to pile index " + curIndex);
                 });

                 pileIndex = (pileIndex + 1) % totalLeft;
             }

             // Start timer
             startTime[0] = System.currentTimeMillis();
             Log.d(TAG, "dealAndStart: timer started at " + startTime[0]);
             Log.d(TAG, "dealAndStart: finished dealing");
             // Start live timer updates
             timerHandler.removeCallbacksAndMessages(null);
             timerRunnableHolder[0] = new Runnable() {
                 @Override
                 public void run() {
                     if (startTime[0] > 0) {
                         long elapsed = System.currentTimeMillis() - startTime[0];
                         timerHolder[0].setText(formatElapsed(elapsed));
                         timerHandler.postDelayed(this, 1000);
                     }
                 }
             };
             timerHandler.post(timerRunnableHolder[0]);

             // Ensure the restart button is above newly added CardViews
             runOnUiThread(() -> {
                 try {
                     controlsOverlayHolder[0].bringToFront();
                     restartBtnHolder[0].bringToFront();
                     restartBtnHolder[0].setElevation(20f);
                     restartBtnHolder[0].setTranslationZ(20f);
                     controlsOverlayHolder[0].invalidate();
                     restartBtnHolder[0].invalidate();
                     Log.d(TAG, "dealAndStart: controlsOverlay and restartBtn brought to front after deal");
                 } catch (Exception e) {
                     Log.e(TAG, "Error bringing restartBtn to front", e);
                 }
             });
         };

        // Now that dealAndStart is defined, wire the restart button to it.
        // Wire restart button to the dealAndStart Runnable
        restartBtnHolder[0].setOnClickListener(v -> {
            Log.d(TAG, "Restart button clicked (final wiring)");
            android.widget.Toast.makeText(this, getString(R.string.restarting_toast), android.widget.Toast.LENGTH_SHORT).show();
             runOnUiThread(() -> {
                 try {
                     dealAndStart.run();
                 } catch (Exception e) {
                     Log.e(TAG, "Error while running dealAndStart from restart click", e);
                     android.widget.Toast.makeText(this, "Restart failed: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                 }
             });
         });

         // Now define the checkWin runnable and store it in the holder so listeners can call it
         checkWinHolder[0] = () -> {
            // Win if no cards remain in play area (all are in right piles)
            boolean won = true;
            for (PileView pv : leftPileViews) {
                if (pv == null || pv.getLogicalPile() == null) continue;
                if (!pv.getLogicalPile().isEmpty()) { won = false; break; }
            }
            if (won) {
                // compute elapsed time
                long elapsed = startTime[0] > 0 ? (System.currentTimeMillis() - startTime[0]) : 0;
                String timeStr = formatElapsed(elapsed);
                winTextHolder[0].setText(getString(R.string.you_win_time, timeStr));
                winOverlayHolder[0].setVisibility(View.VISIBLE);
                winTextHolder[0].setVisibility(View.VISIBLE);
                // Stop live timer updates on win
                try {
                    timerHandler.removeCallbacksAndMessages(null);
                } catch (Exception ignored) {}
                 Log.d(TAG, "checkWin: player won in " + timeStr + " (ms=" + elapsed + ")");
             }
         };

        // --- Create the master list of all UI piles ---
        allPiles.addAll(leftPileViews);
        // include the optional storage pile as a drop target
        if (organizeStoreView != null) {
            allPiles.add(organizeStoreView);
        }
        allPiles.addAll(rightPileViews);

        // Initial deal
        root.post(dealAndStart);
    }

    // Helper to format elapsed milliseconds as mm:ss
    private static String formatElapsed(long ms) {
        long totalSec = ms / 1000;
        long mins = totalSec / 60;
        long secs = totalSec % 60;
        return String.format(Locale.US, "%d:%02d", mins, secs);
    }
 }
