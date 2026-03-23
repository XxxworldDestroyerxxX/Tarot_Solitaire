package com.example.tarotsolitaire.Activities;

import android.content.Intent;
import android.os.Bundle;

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
import androidx.annotation.NonNull;

import com.example.tarotsolitaire.BaseActivity;
import com.example.tarotsolitaire.model.Card;
import com.example.tarotsolitaire.view.CardView;
import com.example.tarotsolitaire.view.DebugOverlay;
import com.example.tarotsolitaire.model.Deck;
import com.example.tarotsolitaire.model.Pile;
import com.example.tarotsolitaire.view.PileView;
import com.example.tarotsolitaire.R;
import com.example.tarotsolitaire.model.SpecialPile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class GameActivity extends BaseActivity {

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
        @Override @NonNull public String toString() { return "LastMove(card=" + card + ", from=" + fromPile + ", to=" + toPile + ")"; }
    }

    // --- Instance-scoped UI and game objects so helper methods can access them ---
    private ConstraintLayout root;
    private final List<PileView> leftPileViews = new ArrayList<>();
    private final List<PileView> rightPileViews = new ArrayList<>();
    private final List<PileView> allPiles = new ArrayList<>();
    private PileView organizeStoreView;
    private DebugOverlay debugOverlay;
    private FrameLayout controlsOverlay;
    private android.widget.Button restartBtn;
    private android.widget.Button undoBtn;
    private TextView timerView;
    private TextView winTextView;
    private View winOverlayView;
    // Deck currently used for dealing
    private Deck currentDeck;
    // Flag to suppress recording moves while the initial deal is happening (snapping causes onPlaced events)
    private boolean isDealing = false;
    private int initTable = 1;

    // Flag to ensure GameInit runs only once per Activity instance
    private boolean hasInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_page);

        root = findViewById(R.id.rootLayout);

        timerHandler = new Handler(Looper.getMainLooper());

        // Initialize once per Activity instance. If the Activity is recreated (new instance), GameInit will run again.
        if (!hasInitialized) {
            GameInit();
            hasInitialized = true;
        }
    }

    // New helper that performs the full initial game table construction in the same order
    // it previously ran inline in onCreate. This keeps onCreate concise while preserving
    // behavior and ordering (finding views, wiring logic, creating overlays, controls,
    // wiring buttons, building master lists, and scheduling the initial shuffle/deal).
    private void GameInit() {
        // 1) Find pile views from XML
        findPileViews();

        // 2) Create and link the pile logic
        wirePileLogic();

        // 3) Apply labels and configure the organize store
        labelRightPilesAndOrganizeStore();

        // 4) Create win overlay and text
        createWinOverlay();

        // 5) Create controls overlay and UI elements. Returns the debug toggle button so we can wire it after debug overlay exists.
        android.widget.Button debugButton = createControls();

        // 6) Create debug overlay (hidden)
        createDebugOverlay();

        // 7) Wire debug toggle to the debug overlay
        wireDebugToggle(debugButton);

        // 8) Add controls overlay to root (on top of game view)
        if (controlsOverlay != null) root.addView(controlsOverlay);

        // 9) Build master list and schedule initial shuffle/deal
        buildAllPilesAndStart();
    }

    // --- Helper method implementations ---

    private void findPileViews() {
        leftPileViews.clear();
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

        rightPileViews.clear();
        rightPileViews.add(findViewById(R.id.organizePile1));
        rightPileViews.add(findViewById(R.id.organizePile2));
        rightPileViews.add(findViewById(R.id.organizePile3));
        rightPileViews.add(findViewById(R.id.organizePile4));
        rightPileViews.add(findViewById(R.id.organizePile5));
        rightPileViews.add(findViewById(R.id.organizePile6));
        organizeStoreView = findViewById(R.id.organizeStore);
    }

    private void wirePileLogic() {
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
    }

    private void labelRightPilesAndOrganizeStore() {
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
    }

    private void createWinOverlay() {
        View winOverlay = new View(this);
        winOverlay.setBackgroundColor(0xAA000000);
        ConstraintLayout.LayoutParams winOverlayLp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        winOverlay.setLayoutParams(winOverlayLp);
        winOverlay.setVisibility(View.GONE);
        root.addView(winOverlay);
        winOverlayView = winOverlay;

        TextView winText = new TextView(this);
        winText.setId(View.generateViewId());
        winText.setText(getString(R.string.you_win));
        winText.setTextColor(android.graphics.Color.WHITE);
        winText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        winText.setVisibility(View.GONE);
        ConstraintLayout.LayoutParams wtLp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        wtLp.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        wtLp.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        wtLp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        wtLp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        winText.setLayoutParams(wtLp);
        root.addView(winText);
        winTextView = winText;
    }

    // Create controls overlay and UI elements (timer, undo, debug toggle, restart, return).
    // Returns the debug toggle button so it can be wired after debug overlay creation.
    private android.widget.Button createControls() {
        // Controls overlay
        FrameLayout controls = new FrameLayout(this);
        ConstraintLayout.LayoutParams coLp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        controls.setLayoutParams(coLp);
        controls.setClickable(false);
        controlsOverlay = controls;

        // Timer
        TextView timerV = new TextView(this);
        timerV.setText(getString(R.string.timer_initial));
        timerV.setTextColor(android.graphics.Color.WHITE);
        timerV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        timerView = timerV;
        int baseMargin = (int) (8 * getResources().getDisplayMetrics().density);

        // Create a horizontal container for timer + undo + debug so they align and don't overlap
        android.widget.LinearLayout ll = new android.widget.LinearLayout(this);
        ll.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        ll.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        android.widget.LinearLayout.LayoutParams llParams = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);

        // Undo button
        android.widget.Button undoB = new android.widget.Button(this);
        undoB.setText(getString(R.string.undo));
        undoB.setVisibility(View.VISIBLE);
        undoBtn = undoB;

        // Debug toggle button
        android.widget.Button debugB = new android.widget.Button(this);
        debugB.setText(getString(R.string.debug_toggle));
        debugB.setVisibility(View.VISIBLE);

        // Restart button
        android.widget.Button restartB = new android.widget.Button(this);
        restartB.setText(getString(R.string.restart));
        restartB.setVisibility(View.VISIBLE);
        restartBtn = restartB;

        // Return button
        android.widget.Button returnB = new android.widget.Button(this);
        returnB.setText(getString(R.string.Return));
        returnB.setVisibility(View.VISIBLE);

        // Add views into linear layout: timer, small spacer, undo, small spacer, debug
        ll.addView(timerView, llParams);
        // small spacer
        View spacer1 = new View(this);
        int spacerPx = (int)(6 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout.LayoutParams spLp = new android.widget.LinearLayout.LayoutParams(spacerPx, android.widget.LinearLayout.LayoutParams.MATCH_PARENT);
        ll.addView(spacer1, spLp);
        ll.addView(returnB, llParams);
        View spacer2 = new View(this);
        ll.addView(spacer2, spLp);
        ll.addView(restartBtn, llParams);
        View spacer3 = new View(this);
        ll.addView(spacer3, spLp);
        ll.addView(debugB, llParams);
        View spacer4 = new View(this);
        ll.addView(spacer4, spLp);
        ll.addView(undoBtn, llParams);

        // Place the horizontal layout at bottom-left of the controls overlay
        FrameLayout.LayoutParams llFrameLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        llFrameLp.setMargins(baseMargin, 0, 0, baseMargin);
        controls.addView(ll, llFrameLp);

        // Wire restart button to use the three-step restartGame flow
        restartBtn.setOnClickListener(v -> {
            Log.d(TAG, "Restart button clicked (restartGame)");
            android.widget.Toast.makeText(this, getString(R.string.restarting_toast), android.widget.Toast.LENGTH_SHORT).show();
            runOnUiThread(() -> {
                try {
                    restartGame();
                } catch (Exception e) {
                    Log.e(TAG, "Error while running restartGame from restart click", e);
                    android.widget.Toast.makeText(this, "Restart failed: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                }
            });
        });

        // Wire return button to take player back to main menu
        returnB.setOnClickListener(v -> {
            Log.d(TAG, "Return button clicked (Go to main menu activity)");
            runOnUiThread(() -> {
                Intent intent = new Intent(GameActivity.this, MainMenu.class);
                startActivity(intent);
            });
        });

        // Wire undo button
        undoBtn.setOnClickListener(v -> runOnUiThread(() -> {
            if (lastMove == null) {
                Log.d(TAG, "Undo: no move to undo");
                android.widget.Toast.makeText(this, getString(R.string.undo_none), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "Undo: reverting move of card " + lastMove.card + " from " + lastMove.toPile + " back to " + lastMove.fromPile);
            CardView cv = findCardView(lastMove.card);
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
            // Suppress placement recording during undo snap
            boolean prevDealing = isDealing;
            isDealing = true;
            try {
                cv.snapToPile(targetFrom, false);
            } finally {
                isDealing = prevDealing;
            }
            lastMove = null;
            // reposition and tighten piles after undo
            runOnUiThread(this::adjustStackingForOverflow);
            android.widget.Toast.makeText(this, getString(R.string.undo_done), android.widget.Toast.LENGTH_SHORT).show();
        }));

        return debugB;
    }

    private void createDebugOverlay() {
        debugOverlay = new DebugOverlay(this);
        ConstraintLayout.LayoutParams debugLpFull = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        debugOverlay.setLayoutParams(debugLpFull);
        debugOverlay.setVisibility(View.GONE);
        root.addView(debugOverlay);
    }

    private void wireDebugToggle(android.widget.Button debugB) {
        if (debugB == null) return;
        debugB.setOnClickListener(v -> {
            if (debugOverlay.getVisibility() == View.VISIBLE) {
                debugOverlay.setVisibility(View.GONE);
                debugOverlay.clear();
            } else {
                debugOverlay.setVisibility(View.VISIBLE);
                // ensure overlay is above controls overlay
                debugOverlay.bringToFront();
            }
        });
    }

    private void buildAllPilesAndStart() {
        // Build master list
        allPiles.clear();
        allPiles.addAll(leftPileViews);
        if (organizeStoreView != null) allPiles.add(organizeStoreView);
        allPiles.addAll(rightPileViews);

        // Initial start: shuffle and deal after layout is ready
        root.post(() -> {
            shuffleDecks();
            dealAndStart();
        });
    }

    // --- Refactored helpers (moved out of onCreate) ---

    private void shuffleDecks() {
        // Create and shuffle deck(s). Keep this method small so restart/start can reuse it.
        currentDeck = new Deck();
        currentDeck.shuffle();
        Log.d(TAG, "shuffleDecks: deck created and shuffled (size=" + currentDeck.getCards().size() + ")");
    }

    private void restartGame() {
        clearAll();
        shuffleDecks();
        dealAndStart();
    }

    private void clearAll() {
        Log.d(TAG, "clearAll: starting to remove card views and clear piles");
        if (root == null) return;
        // Clear the saved undo state when clearing the board so undo doesn't replay a previous move
        lastMove = null;
        Log.d(TAG, "clearAll: lastMove cleared");
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

        // Update gamesPlayed counter in Firestore for the signed-in user
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> m = new HashMap<>();
                m.put("gamesPlayed", FieldValue.increment(1));
                db.collection("users").document(uid).set(m, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.i(TAG, "Incremented gamesPlayed for user " + uid))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to increment gamesPlayed", e));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while updating gamesPlayed", e);
        }
    }

    private CardView findCardView(Card c) {
        if (root == null) return null;
        for (int i = 0; i < root.getChildCount(); i++) {
            android.view.View v = root.getChildAt(i);
            if (v instanceof CardView) {
                CardView cv = (CardView) v;
                if (cv.getCard() == c) return cv;
            }
        }
        return null;
    }

    private void dealAndStart() {
        Log.d(TAG, "dealAndStart: starting deal");
        if (winOverlayView != null) winOverlayView.setVisibility(View.GONE);
        if (winTextView != null) winTextView.setVisibility(View.GONE);

        clearAll();

        // Suppress recording of moves caused by the initial placement/snapping during deal
        isDealing = true;

        int pileWidth = getResources().getDimensionPixelSize(R.dimen.pile_width);
        int pileHeight = getResources().getDimensionPixelSize(R.dimen.pile_height);
        if (!allPiles.isEmpty() && allPiles.get(0) != null) {
            int w = allPiles.get(0).getWidth();
            int h = allPiles.get(0).getHeight();
            if (w > 0) pileWidth = w;
            if (h > 0) pileHeight = h;
        }

        if (currentDeck == null) shuffleDecks();

        int cardWidth = pileWidth;
        int cardHeight = pileHeight;

        int pileIndex = 0;
        int totalLeft = leftPileViews.size();
        for (Card card : currentDeck.getCards()) {
            if (pileIndex == 5) pileIndex = (pileIndex + 1) % totalLeft; // skip middle empty pile when dealing
            PileView targetPileView = leftPileViews.get(pileIndex);

            int useW = cardWidth;
            int useH = cardHeight;
            if (targetPileView != null) {
                int tw = targetPileView.getWidth();
                int th = targetPileView.getHeight();
                if (tw > 0) useW = tw;
                if (th > 0) useH = th;
            }

            CardView cardView = new CardView(this, card);
            // attach debug overlay so CardView can post debug shapes while dragging
            cardView.setDebugOverlay(debugOverlay);
            ConstraintLayout.LayoutParams cardParams = new ConstraintLayout.LayoutParams(useW, useH);
            cardView.setAllPiles(allPiles);

            // Listener records last move (single level) and checks win
            cardView.setOnPlacedListener((placedCard, fromPile, toPile) -> {
                // Ignore placements occurring during the initial deal sequence
                if (isDealing) {
                    Log.d(TAG, "onPlaced: suppressed recording during deal for " + placedCard);
                    return;
                }
                // Record last move: note that 'fromPile' may be null if card came from nowhere
                lastMove = new LastMove(placedCard, fromPile, toPile);
                Log.d(TAG, "Recorded last move: " + lastMove);
                // run checkWin on UI thread
                runOnUiThread(this::checkWin);
                // After a successful placement by the user, adjust stacking so piles don't overflow
                runOnUiThread(this::adjustStackingForOverflow);
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

        // After all cardView.post(...) calls have been scheduled, enqueue a runnable to clear the dealing flag.
        // Posting to root ensures this runs after the posted snap Runnables (FIFO ordering on the main looper).
        root.post(() -> {
            isDealing = false;
            Log.d(TAG, "dealAndStart: finished initial snapping; isDealing=false");
        });

        // Start timer
        startTimeMs = System.currentTimeMillis();
        pausedElapsed = 0L;
        Log.d(TAG, "dealAndStart: timer started at " + startTimeMs);

        // Timer runnable (instance field) uses pausedElapsed + (now - startTimeMs)
        if (timerHandler == null) timerHandler = new Handler(Looper.getMainLooper());
        timerHandler.removeCallbacksAndMessages(null);
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTimerRunning) {
                    long elapsed = pausedElapsed + (startTimeMs > 0 ? (System.currentTimeMillis() - startTimeMs) : 0);
                    if (timerView != null) timerView.setText(formatElapsed(elapsed));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
        isTimerRunning = true;

        runOnUiThread(() -> {
            try {
                if (controlsOverlay != null) controlsOverlay.bringToFront();
                if (restartBtn != null) restartBtn.bringToFront();
                if (undoBtn != null) undoBtn.bringToFront();
                if (restartBtn != null) {
                    restartBtn.setElevation(20f);
                    restartBtn.setTranslationZ(20f);
                }
                if (controlsOverlay != null) controlsOverlay.invalidate();
                if (restartBtn != null) restartBtn.invalidate();
                // If debug overlay is visible, keep it on top
                if (debugOverlay != null && debugOverlay.getVisibility() == View.VISIBLE) {
                    debugOverlay.bringToFront();
                }
                Log.d(TAG, "dealAndStart: controlsOverlay and restartBtn brought to front after deal");
            } catch (Exception e) {
                Log.e(TAG, "Error bringing restartBtn to front", e);
            }
        });

        // After layout settled, adjust stacking to avoid overflow and position cards
        root.post(this::adjustStackingForOverflow);
    }

    /**
     * Compute a uniform stack offset multiplier for all left/play piles so that the tallest
     * pile fits within the available vertical space. Applies the multiplier to each pile's
     * logical pile and repositions all CardViews. The multiplier tightens as piles grow.
     */
    private void adjustStackingForOverflow() {
        if (root == null || leftPileViews.isEmpty()) return;

        // Find pile top and height (assume all play piles share same top)
        PileView sample = leftPileViews.get(0);
        int pileTop = (int) sample.getY();
        int pileHeight = sample.getHeight();
        if (pileHeight <= 0) return; // layout not ready yet

        int rootHeight = root.getHeight();
        if (rootHeight <= 0) return;

        // Reserve some bottom space for controls (timer/undo). Conservative estimate.
        float density = getResources().getDisplayMetrics().density;
        int reservedBottom = (int) (56 * density); // ~56dp reserved

        int availableHeight = rootHeight - pileTop - reservedBottom;
        if (availableHeight <= pileHeight) {
            // Not enough room even for one card; fallback to tiny multiplier
            float minMult = 0.05f;
            for (PileView pv : leftPileViews) if (pv != null && pv.getLogicalPile() != null) pv.getLogicalPile().setStackOffsetMultiplier(minMult);
            positionAllCards();
            return;
        }

        // Find max card count across left piles
        int maxCount = 0;
        for (PileView pv : leftPileViews) {
            if (pv == null || pv.getLogicalPile() == null) continue;
            int c = pv.getLogicalPile().getCards().size();
            if (c > maxCount) maxCount = c;
        }

        // If no stacking needed, set default multiplier and exit
        if (maxCount <= 1) {
            float defaultMult = 0.28f;
            for (PileView pv : leftPileViews) if (pv != null && pv.getLogicalPile() != null) pv.getLogicalPile().setStackOffsetMultiplier(defaultMult);
            positionAllCards();
            return;
        }

        // Solve for multiplier m: pileHeight + (maxCount-1) * m * pileHeight <= availableHeight
        float m = (float) (availableHeight - pileHeight) / ((maxCount - 1) * (float) pileHeight);
        // Clamp multiplier to sensible bounds
        float maxMult = 0.28f;
        float minMult = 0.04f;
        if (m > maxMult) m = maxMult;
        if (m < minMult) m = minMult;

        for (PileView pv : leftPileViews) {
            if (pv == null || pv.getLogicalPile() == null) continue;
            pv.getLogicalPile().setStackOffsetMultiplier(m);
        }

        // Reposition card views to reflect the new multiplier
        positionAllCards();
    }

    private void positionAllCards() {
        if (root == null) return;

        // Prevent placement events from being recorded while we reposition views
        boolean prevDealing = isDealing;
        isDealing = true;

        // Use a short animation to smoothly transition card positions so tightening looks natural
        final int animDuration = 150; // ms (within your requested 100-200ms)
        root.post(() -> {
            try {
                for (PileView pv : allPiles) {
                    if (pv == null || pv.getLogicalPile() == null) continue;
                    Pile logic = pv.getLogicalPile();
                    List<CardView> cvs = pv.getCardViews();
                    for (int i = 0; i < cvs.size(); i++) {
                        CardView cv = cvs.get(i);
                        if (cv == null) continue;
                        float mult = logic.getStackOffsetMultiplier();
                        float stackOffset = pv.getHeight() * mult * i;
                        float targetX = pv.getX() + (pv.getWidth() - cv.getWidth()) / 2f;
                        float targetY = pv.getY() + stackOffset;
                        // Animate to new position; do not call snapToPile here (avoid onPlaced firing)
                        cv.animate().x(targetX).y(targetY).setDuration(animDuration).start();
                        // Force redraw so CardView.onDraw() will recompute label position based on current pile multiplier
                        cv.invalidate();
                    }
                }
                // schedule restoring the 'isDealing' flag after animations finish
                root.postDelayed(() -> {
                    isDealing = prevDealing;
                    Log.d(TAG, "positionAllCards: finished animations; isDealing restored=" + prevDealing);
                }, animDuration + 20);
            } catch (Exception e) {
                Log.e(TAG, "positionAllCards: error while positioning", e);
                isDealing = prevDealing;
            }
        });
    }

    private void checkWin() {
        boolean won = true;
        for (PileView pv : leftPileViews) {
            if (pv == null || pv.getLogicalPile() == null) continue;
            if (!pv.getLogicalPile().isEmpty()) { won = false; break; }
        }
        if (won) {
            long elapsed = pausedElapsed + (startTimeMs > 0 ? (System.currentTimeMillis() - startTimeMs) : 0);
            String timeStr = formatElapsed(elapsed);
            if (winTextView != null) winTextView.setText(getString(R.string.you_win_time, timeStr));
            if (winOverlayView != null) winOverlayView.setVisibility(View.VISIBLE);
            if (winTextView != null) winTextView.setVisibility(View.VISIBLE);
            try { if (timerHandler != null) timerHandler.removeCallbacksAndMessages(null); } catch (Exception ignored) {}
            Log.d(TAG, "checkWin: player won in " + timeStr + " (ms=" + elapsed + ")");

            // If user is signed in, update their bestTime in Firestore if this is a new record
            try {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // Increment gamesWon and add this elapsed time to the totalPlayTimeMs counter
                    Map<String, Object> statsUpdate = new HashMap<>();
                    statsUpdate.put("gamesWon", FieldValue.increment(1));
                    statsUpdate.put("totalPlayTimeMs", FieldValue.increment(elapsed));
                    db.collection("users").document(uid).set(statsUpdate, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Log.i(TAG, "Updated gamesWon and totalPlayTimeMs for " + uid))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update gamesWon/totalPlayTimeMs", e));

                    // Preserve existing bestTime update logic
                    db.collection("users").document(uid).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot doc = task.getResult();
                            Long previousBest = null;
                            if (doc != null && doc.contains("bestTime")) {
                                Object o = doc.get("bestTime");
                                if (o instanceof Long) previousBest = (Long) o;
                                else if (o instanceof Integer) previousBest = ((Integer)o).longValue();
                            }
                            if (previousBest == null || elapsed < previousBest) {
                                // update bestTime using set with merge so doc is created/merged if missing
                                Map<String, Object> m = new HashMap<>();
                                m.put("bestTime", elapsed);
                                db.collection("users").document(uid).set(m, SetOptions.merge())
                                        .addOnSuccessListener(aVoid -> Log.i(TAG, "Updated bestTime to " + elapsed))
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update bestTime", e));
                            } else {
                                Log.d(TAG, "Existing bestTime (" + previousBest + ") is better than " + elapsed);
                            }
                        } else {
                            Log.w(TAG, "Could not read user document to update bestTime", task.getException());
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while updating bestTime", e);
            }
        }
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
