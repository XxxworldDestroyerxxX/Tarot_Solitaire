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

    // Timer fields moved to instance scope so lifecycle methods can control them
    private long startTimeMs = 0L; // wall-clock when timer started/resumed
    private long pausedElapsed = 0L; // milliseconds accumulated while paused/stopped
    private boolean isTimerRunning = false;
    private Handler timerHandler; // main looper handler
    private Runnable timerRunnable;

    // Single-level undo
    private LastMove lastMove = null;

    private static class LastMove {
        final Card card;
        final PileView fromPile;
        final PileView toPile;
        LastMove(Card card, PileView from, PileView to) { this.card = card; this.fromPile = from; this.toPile = to; }
        @Override public String toString() { return "LastMove(card=" + card + ", from=" + fromPile + ", to=" + toPile + ")"; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_page);

        ConstraintLayout root = findViewById(R.id.rootLayout);

        timerHandler = new Handler(Looper.getMainLooper());

        // --- Lists to hold our views and logic objects ---
        List<PileView> leftPileViews = new ArrayList<>();
        List<PileView> rightPileViews = new ArrayList<>();
        List<PileView> allPiles = new ArrayList<>();

        // --- 1. FIND ALL VIEWS FROM THE XML ---
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
        PileView organizeStoreView = findViewById(R.id.organizeStore);

        // --- 2. CREATE AND LINK THE LOGIC ---
        for (PileView pileView : leftPileViews) {
            Pile pileLogic = new Pile();
            pileView.setLogicalPile(pileLogic);
        }

        for (int i = 0; i < rightPileViews.size(); i++) {
            PileView pileView = rightPileViews.get(i);
            if (pileView == null) continue;

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
                    if (organizeStoreView != null && organizeStoreView.getLogicalPile() != null && !organizeStoreView.getLogicalPile().isEmpty()) return false;
                    if (card.getType() != Card.Type.STANDARD) return false;
                    if (card.getSuit() != expectedSuit) return false;
                    if (pile.isEmpty()) return card.getRank() == 2;
                    Card top = pile.getTopCard();
                    if (top == null || top.getType() != Card.Type.STANDARD) return false;
                    int topRank = top.getRank();
                    int placeRank = card.getRank();
                    return placeRank == topRank + 1 && placeRank >= 2 && placeRank <= 13;
                };

                SpecialPile suitPile = new SpecialPile(suitRule);
                pileView.setLogicalPile(suitPile);
            } else if (i == 4) {
                SpecialPile.PlacementRule tarotAsc = (pile, card) -> {
                    if (card == null) return false;
                    if (card.getType() != Card.Type.TAROT) return false;
                    if (pile.isEmpty()) return card.getRank() == 0;
                    Card top = pile.getTopCard();
                    if (top == null || top.getType() != Card.Type.TAROT) return false;
                    return card.getRank() == top.getRank() + 1;
                };
                pileView.setLogicalPile(new SpecialPile(tarotAsc));
            } else if (i == 5) {
                SpecialPile.PlacementRule tarotDesc = (pile, card) -> {
                    if (card == null) return false;
                    if (card.getType() != Card.Type.TAROT) return false;
                    if (pile.isEmpty()) return card.getRank() == 21;
                    Card top = pile.getTopCard();
                    if (top == null || top.getType() != Card.Type.TAROT) return false;
                    return card.getRank() == top.getRank() - 1;
                };
                pileView.setLogicalPile(new SpecialPile(tarotDesc));
            } else {
                pileView.setLogicalPile(new SpecialPile());
            }
        }

        if (rightPileViews.size() >= 6) {
            rightPileViews.get(0).setLabel(getString(R.string.label_hearts));
            rightPileViews.get(1).setLabel(getString(R.string.label_diamonds));
            rightPileViews.get(2).setLabel(getString(R.string.label_clubs));
            rightPileViews.get(3).setLabel(getString(R.string.label_spades));
            rightPileViews.get(4).setLabel(getString(R.string.label_tarot_asc));
            rightPileViews.get(5).setLabel(getString(R.string.label_tarot_desc));
        }

        if (organizeStoreView != null) {
            organizeStoreView.setLabel(getString(R.string.store_label));
            organizeStoreView.setShowLock(true);
            SpecialPile.PlacementRule storeRule = (pile, card) -> card != null && pile.isEmpty();
            organizeStoreView.setLogicalPile(new SpecialPile(storeRule));
        }

        final View[] winOverlayHolder = new View[1];
        final TextView[] winTextHolder = new TextView[1];
        final FrameLayout[] controlsOverlayHolder = new FrameLayout[1];
        final android.widget.Button[] restartBtnHolder = new android.widget.Button[1];
        final TextView[] timerHolder = new TextView[1];
        final android.widget.Button[] undoBtnHolder = new android.widget.Button[1];

        // Initialize win overlay
        View winOverlayView = new View(this);
        winOverlayView.setBackgroundColor(0xAA000000);
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

        // Controls overlay
        FrameLayout controlsOverlay = new FrameLayout(this);
        ConstraintLayout.LayoutParams coLp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        controlsOverlay.setLayoutParams(coLp);
        controlsOverlay.setClickable(false);

        // Timer
        TextView timerView = new TextView(this);
        timerView.setText(getString(R.string.timer_initial));
        timerView.setTextColor(android.graphics.Color.WHITE);
        timerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        int baseMargin = (int) (8 * getResources().getDisplayMetrics().density);

        // Create a horizontal container for timer + undo + debug so they align and don't overlap
        android.widget.LinearLayout ll = new android.widget.LinearLayout(this);
        ll.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        ll.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        android.widget.LinearLayout.LayoutParams llParams = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);

        // Undo button
        android.widget.Button undoBtn = new android.widget.Button(this);
        undoBtn.setText(getString(R.string.undo));
        undoBtn.setVisibility(View.VISIBLE);
        undoBtnHolder[0] = undoBtn;

        // Debug toggle button
        android.widget.Button debugBtn = new android.widget.Button(this);
        debugBtn.setText(getString(R.string.debug_toggle));
        debugBtn.setVisibility(View.VISIBLE);

        // Add views into linear layout: timer, small spacer, undo, small spacer, debug
        ll.addView(timerView, llParams);
        // small spacer
        View spacer1 = new View(this);
        int spacerPx = (int)(6 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout.LayoutParams spLp = new android.widget.LinearLayout.LayoutParams(spacerPx, android.widget.LinearLayout.LayoutParams.MATCH_PARENT);
        ll.addView(spacer1, spLp);
        ll.addView(undoBtn, llParams);
        View spacer2 = new View(this);
        ll.addView(spacer2, spLp);
        ll.addView(debugBtn, llParams);

        // Place the horizontal layout at bottom-left of the controls overlay
        FrameLayout.LayoutParams llFrameLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        llFrameLp.setMargins(baseMargin, 0, 0, baseMargin);
        controlsOverlay.addView(ll, llFrameLp);

        timerHolder[0] = timerView;

        // Restart button top-left
        android.widget.Button restartBtn = new android.widget.Button(this);
        restartBtn.setText(getString(R.string.restart));
        restartBtn.setVisibility(View.VISIBLE);

        // Create debug overlay (initially hidden) and add to root (on top of game view)
        final DebugOverlay debugOverlay = new DebugOverlay(this);
        ConstraintLayout.LayoutParams debugLpFull = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        debugOverlay.setLayoutParams(debugLpFull);
        debugOverlay.setVisibility(View.GONE);
        root.addView(debugOverlay);

        FrameLayout.LayoutParams restartLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        restartLp.setMargins(baseMargin, baseMargin, 0, 0);
        controlsOverlay.addView(restartBtn, restartLp);

        // Wire debug toggle to show/hide overlay and bring it to front when visible
        debugBtn.setOnClickListener(v -> {
            if (debugOverlay.getVisibility() == View.VISIBLE) {
                debugOverlay.setVisibility(View.GONE);
                debugOverlay.clear();
            } else {
                debugOverlay.setVisibility(View.VISIBLE);
                // ensure overlay is above controls overlay
                debugOverlay.bringToFront();
            }
        });

        root.addView(controlsOverlay);
        controlsOverlayHolder[0] = controlsOverlay;
        restartBtnHolder[0] = restartBtn;

        // Timer container (local holder for lambdas)
        final long[] startTimeHolder = new long[1];

        // Helper to check win
        final Runnable[] checkWinHolder = new Runnable[1];

        // Helper to clear all
        Runnable clearAll = () -> {
            Log.d(TAG, "clearAll: starting to remove card views and clear piles");
            for (int i = root.getChildCount() - 1; i >= 0; i--) {
                android.view.View v = root.getChildAt(i);
                if (v instanceof CardView) {
                    root.removeViewAt(i);
                }
            }
            for (PileView pv : allPiles) {
                if (pv == null) continue;
                List<CardView> cvs = new java.util.ArrayList<>(pv.getCardViews());
                for (CardView cv : cvs) pv.removeCardView(cv);
            }
            for (PileView pv : allPiles) {
                if (pv == null) continue;
                Pile logic = pv.getLogicalPile();
                if (logic == null) continue;
                java.util.List<Card> cards = new java.util.ArrayList<>(logic.getCards());
                for (Card c : cards) logic.removeCard(c);
            }
            Log.d(TAG, "clearAll: finished clearing piles and views");
        };

        // Helper to find CardView for a Card
        final java.util.function.Function<Card, CardView> findCardView = (Card c) -> {
            for (int i = 0; i < root.getChildCount(); i++) {
                android.view.View v = root.getChildAt(i);
                if (v instanceof CardView) {
                    CardView cv = (CardView) v;
                    if (cv.getCard() == c) return cv;
                }
            }
            return null;
        };

        // Deal function
        Runnable dealAndStart = () -> {
            Log.d(TAG, "dealAndStart: starting deal");
            winOverlayHolder[0].setVisibility(View.GONE);
            winTextHolder[0].setVisibility(View.GONE);

            clearAll.run();

            int pileWidth = getResources().getDimensionPixelSize(R.dimen.pile_width);
            int pileHeight = getResources().getDimensionPixelSize(R.dimen.pile_height);
            if (!allPiles.isEmpty() && allPiles.get(0) != null) {
                int w = allPiles.get(0).getWidth();
                int h = allPiles.get(0).getHeight();
                if (w > 0) pileWidth = w;
                if (h > 0) pileHeight = h;
            }

            Deck deck = new Deck();
            deck.shuffle();

            int cardWidth = pileWidth;
            int cardHeight = pileHeight;

            int pileIndex = 0;
            int totalLeft = leftPileViews.size();
            for (Card card : deck.getCards()) {
                if (pileIndex == 5) pileIndex = (pileIndex + 1) % totalLeft;
                PileView targetPileView = leftPileViews.get(pileIndex);

                int useW = cardWidth;
                int useH = cardHeight;
                if (targetPileView != null) {
                    int tw = targetPileView.getWidth();
                    int th = targetPileView.getHeight();
                    if (tw > 0) useW = tw;
                    if (th > 0) useH = th;
                }

                CardView cardView = new CardView(this);
                // attach debug overlay so CardView can post debug shapes while dragging
                cardView.setDebugOverlay(debugOverlay);
                ConstraintLayout.LayoutParams cardParams = new ConstraintLayout.LayoutParams(useW, useH);
                cardView.setAllPiles(allPiles);
                cardView.setCard(card);

                // Listener records last move (single level) and checks win
                cardView.setOnPlacedListener((placedCard, fromPile, toPile) -> {
                    // Record last move: note that 'fromPile' may be null if card came from nowhere
                    lastMove = new LastMove(placedCard, fromPile, toPile);
                    Log.d(TAG, "Recorded last move: " + lastMove);
                    // run checkWin
                    runOnUiThread(checkWinHolder[0]);
                });

                root.addView(cardView, cardParams);
                Log.d(TAG, "dealAndStart: added CardView for " + card + " to root");
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
            startTimeHolder[0] = System.currentTimeMillis();
            startTimeMs = startTimeHolder[0];
            pausedElapsed = 0L;
            Log.d(TAG, "dealAndStart: timer started at " + startTimeMs);

            // Timer runnable (instance field) uses pausedElapsed + (now - startTimeMs)
            timerHandler.removeCallbacksAndMessages(null);
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isTimerRunning) {
                        long elapsed = pausedElapsed + (startTimeMs > 0 ? (System.currentTimeMillis() - startTimeMs) : 0);
                        timerHolder[0].setText(formatElapsed(elapsed));
                        timerHandler.postDelayed(this, 1000);
                    }
                }
            };
            timerHandler.post(timerRunnable);
            isTimerRunning = true;

            runOnUiThread(() -> {
                try {
                    controlsOverlayHolder[0].bringToFront();
                    restartBtnHolder[0].bringToFront();
                    undoBtnHolder[0].bringToFront();
                    restartBtnHolder[0].setElevation(20f);
                    restartBtnHolder[0].setTranslationZ(20f);
                    controlsOverlayHolder[0].invalidate();
                    restartBtnHolder[0].invalidate();
                    // If debug overlay is visible, keep it on top
                    if (debugOverlay.getVisibility() == View.VISIBLE) {
                        debugOverlay.bringToFront();
                    }
                    Log.d(TAG, "dealAndStart: controlsOverlay and restartBtn brought to front after deal");
                } catch (Exception e) {
                    Log.e(TAG, "Error bringing restartBtn to front", e);
                }
            });
        };

        // Wire restart button
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

        // Wire undo button
        undoBtnHolder[0].setOnClickListener(v -> {
            runOnUiThread(() -> {
                if (lastMove == null) {
                    Log.d(TAG, "Undo: no move to undo");
                    android.widget.Toast.makeText(this, getString(R.string.undo_none), android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Undo: reverting move of card " + lastMove.card + " from " + lastMove.toPile + " back to " + lastMove.fromPile);
                CardView cv = findCardView.apply(lastMove.card);
                if (cv == null) {
                    Log.e(TAG, "Undo failed: could not find CardView for card " + lastMove.card);
                    return;
                }
                // If fromPile is null (originally off-screen), move it to an empty play pile (fallback)
                PileView targetFrom = lastMove.fromPile;
                if (targetFrom == null) {
                    // find first left play pile to put back
                    if (!leftPileViews.isEmpty()) targetFrom = leftPileViews.get(0);
                }
                if (targetFrom == null) {
                    Log.e(TAG, "Undo failed: no valid from-pile found");
                    return;
                }
                cv.snapToPile(targetFrom, false);
                lastMove = null;
                android.widget.Toast.makeText(this, getString(R.string.undo_done), android.widget.Toast.LENGTH_SHORT).show();
            });
        });

        // Now define checkWin
        checkWinHolder[0] = () -> {
            boolean won = true;
            for (PileView pv : leftPileViews) {
                if (pv == null || pv.getLogicalPile() == null) continue;
                if (!pv.getLogicalPile().isEmpty()) { won = false; break; }
            }
            if (won) {
                long elapsed = pausedElapsed + (startTimeMs > 0 ? (System.currentTimeMillis() - startTimeMs) : 0);
                String timeStr = formatElapsed(elapsed);
                winTextHolder[0].setText(getString(R.string.you_win_time, timeStr));
                winOverlayHolder[0].setVisibility(View.VISIBLE);
                winTextHolder[0].setVisibility(View.VISIBLE);
                try { timerHandler.removeCallbacksAndMessages(null); } catch (Exception ignored) {}
                Log.d(TAG, "checkWin: player won in " + timeStr + " (ms=" + elapsed + ")");
            }
        };

        // Build master list
        allPiles.addAll(leftPileViews);
        if (organizeStoreView != null) allPiles.add(organizeStoreView);
        allPiles.addAll(rightPileViews);

        // Initial deal
        root.post(dealAndStart);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: pausing timer");
        if (isTimerRunning) {
            // accumulate elapsed and stop runnable
            pausedElapsed += (startTimeMs > 0 ? (System.currentTimeMillis() - startTimeMs) : 0);
            isTimerRunning = false;
            try { if (timerHandler != null && timerRunnable != null) timerHandler.removeCallbacks(timerRunnable); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: resuming timer");
        if (!isTimerRunning) {
            startTimeMs = System.currentTimeMillis();
            isTimerRunning = true;
            if (timerHandler != null && timerRunnable != null) timerHandler.post(timerRunnable);
        }
    }

    private static String formatElapsed(long ms) {
        long totalSec = ms / 1000;
        long mins = totalSec / 60;
        long secs = totalSec % 60;
        return String.format(Locale.US, "%d:%02d", mins, secs);
    }
}
