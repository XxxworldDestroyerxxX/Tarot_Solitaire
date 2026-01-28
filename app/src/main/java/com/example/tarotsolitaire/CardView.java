package com.example.tarotsolitaire;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import java.util.List;

@SuppressLint("ViewConstructor")
public class CardView extends View {

    private List<PileView> allPiles; // may be null when inflated by tools
    private PileView currentPile;
    private Card card; // The logical card (may be null for layout inflation)

    private float offsetX, offsetY;
    private float downRawX, downRawY;
    private long downTimeMs;
    private boolean isDragging = false;
    private int touchSlop;
    private boolean isPopAnimating = false;
     private Paint paint;
     private RectF rect;
     private Paint textPaint;

    // Preallocated paints for tarot icon and top-left label to avoid allocations in onDraw
    private Paint iconPaint;
    private Paint iconTextPaint;
    private Paint labelPaint; // bold top-left number indicator

    // Track which pile we are currently hovering (for highlight/haptic)
    private PileView hoveredPile = null;
    // Debug overlay reference (optional)
    private DebugOverlay debugOverlay = null;

    private Bitmap tarotBackground;

    private static final String TAG = "CardView";

    // Standard constructors so tools/layout inflation won't warn
    public CardView(Context context, Card card) {
        super(context);
        this.card = card;
        init();
    }

    public CardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Log.d(TAG, "init: start");


        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new RectF();

        if(card.getType() == Card.Type.TAROT)
        {
            int cardNumber  = Integer.valueOf(card.getRankString());
            Log.d(TAG, "init: cardNumber: " + cardNumber);
            String resName = "tarot_card_" + cardNumber;
            Log.d(TAG, "init: resName: " + resName);

            resName = "tarot_card_1";

            // 2. Find the integer ID for that name
            // "drawable" is the folder, context.getPackageName() finds your app package
            int resId = this.getContext().getResources().getIdentifier(resName, "drawable", this.getContext().getPackageName());
            try {
                tarotBackground = BitmapFactory.decodeResource(getResources(), resId);
            } catch (Exception e) {
                Log.e("CardView", "Could not load tarot background image", e);
            }
        }


        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // Default text size; will be adjusted based on card height when drawing
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        textPaint.setTextAlign(Paint.Align.CENTER);

        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setColor(Color.rgb(212, 175, 55));

        iconTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconTextPaint.setColor(Color.WHITE);
        iconTextPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        labelPaint.setTextAlign(Paint.Align.LEFT);
        labelPaint.setColor(Color.BLACK);

        // Touch slop for distinguishing tap vs drag
        touchSlop = android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop();

        Log.d(TAG, "init: done");
     }

    @SuppressWarnings("unused")
    public Card getCard() {
        return this.card;
    }

    public void setAllPiles(List<PileView> piles) { this.allPiles = piles; }
    public void setCard(Card card) { this.card = card; }

    public void setDebugOverlay(DebugOverlay overlay) { this.debugOverlay = overlay; }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        rect.set(0, 0, getWidth(), getHeight());

        float radius = 16f;
        float stroke = 4f;

        // Compute top-left label position and size
        float labelX = getWidth() * 0.10f;
        float labelYBase = getHeight() * 0.18f; // baseline approx for top-left label
        float baseLabelSize = Math.max(16f, Math.min(36f, getHeight() * 0.22f));

        // Determine tightening factor from the pile's stack offset multiplier. When piles tighten
        // (multiplier approaches a small minMult), we move the label towards the very top and shrink it.
        float multiplier = 0.28f; // default
        float maxMult = 0.28f;
        float minMult = 0.04f;
        if (currentPile != null && currentPile.getLogicalPile() != null) {
            multiplier = currentPile.getLogicalPile().getStackOffsetMultiplier();
        }
        // normalized tightness rawT: 0 == loose (maxMult), 1 == tight (minMult)
        float rawT = 0f;
        if (maxMult > minMult) rawT = (maxMult - multiplier) / (maxMult - minMult);
        rawT = Math.max(0f, Math.min(1f, rawT));

        // If current pile appears to be an organize pile (has a label) don't tighten labels there
        boolean isOrganizePile = (currentPile != null && currentPile.getLabel() != null && !currentPile.getLabel().isEmpty());

        // Start shrinking only after a delay (so it starts one card later). shrinkStart between 0..1
        float shrinkStart = 0.18f; // increased so shrinking starts later
        float t = 0f;
        if (!isOrganizePile) {
            if (rawT <= shrinkStart) {
                t = 0f;
            } else {
                t = (rawT - shrinkStart) / (1f - shrinkStart);
                t = Math.max(0f, Math.min(1f, t));
            }
        } else {
            t = 0f;
        }

        // Move label upward: from labelYBase up towards a small top padding (6px)
        float topPadding = 6f;
        float labelY = labelYBase - t * (labelYBase - topPadding);

        // Shrink label slightly as tightening increases (reduced from 35% to 25%)
        float shrinkFactor = 0.25f;
        float labelSize = baseLabelSize * (1f - t * shrinkFactor);
        labelPaint.setTextSize(labelSize);

        // Slightly increase weight when label gets small to improve legibility
        if (t > 0.6f) labelPaint.setFakeBoldText(true); else labelPaint.setFakeBoldText(false);

        if (card != null && card.getType() == Card.Type.TAROT) {
            // Tarot cards: black background, gold border, white centered number and a small gold icon
            //paint.setStyle(Paint.Style.FILL);
            //paint.setColor(Color.BLACK);

            //canvas.drawRoundRect(rect, radius, radius, paint);
            canvas.save();

            // Create a path for the rounded rectangle
            Path clipPath = new Path();
            clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
            canvas.clipPath(clipPath);

            // Draw the bitmap. null src means use whole image, rect dst means stretch to fit card

            canvas.drawBitmap(tarotBackground, null, rect, paint);

            canvas.restore();

            paint.setStyle(Paint.Style.STROKE);

            // Top-left number indicator (white, bold) - use tightened labelY
            labelPaint.setColor(Color.WHITE);
            float labelAscent = labelPaint.getFontMetrics().ascent;
            canvas.drawText(card.getRankString(), labelX, labelY - labelAscent / 2f, labelPaint);


        } else if (card != null) {
            // Standard card appearance: white background, black stroke and colored suit/rank
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(rect, radius, radius, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(stroke);
            canvas.drawRoundRect(rect, radius, radius, paint);

            // Color mapping by suit for label and suit symbol
            int suitColor = Color.BLACK;
            switch (card.getSuit()) {
                case HEARTS:
                    suitColor = Color.parseColor("#D32F2F"); // red
                    break;
                case DIAMONDS:
                    suitColor = Color.parseColor("#1976D2"); // blue
                    break;
                case CLUBS:
                    suitColor = Color.parseColor("#388E3C"); // green
                    break;
                case SPADES:
                    suitColor = Color.parseColor("#FBC02D"); // yellow
                    break;
            }

            // Top-left number indicator (bold, colored by suit) - use tightened labelY
            labelPaint.setColor(suitColor);
            canvas.drawText(card.getRankString(), labelX, labelY - labelPaint.getFontMetrics().ascent / 2f, labelPaint);

            // Suit symbol below the label
            float suitSize = Math.max(12f, labelSize * 0.7f);
            textPaint.setTextSize(suitSize);
            textPaint.setColor(suitColor);
            textPaint.setTextAlign(Paint.Align.LEFT);
            float suitY = getHeight() * 0.42f - (textPaint.getFontMetrics().ascent + textPaint.getFontMetrics().descent) / 2f;
            canvas.drawText(card.getSuitSymbol(), labelX, suitY, textPaint);

        } else {
            // Preview or placeholder - draw blank card outline
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(stroke);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentPile != null && this != currentPile.getTopCardView()) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                offsetX = event.getRawX() - getX();
                offsetY = event.getRawY() - getY();
                // Record down position/time for tap detection; don't call performClick here (prevents pop on drag)
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                downTimeMs = System.currentTimeMillis();
                isDragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getRawX() - downRawX);
                float dy = Math.abs(event.getRawY() - downRawY);
                if (!isDragging && (dx > touchSlop || dy > touchSlop)) {
                    isDragging = true;
                    // when drag actually starts, bring the view to front
                    bringToFront();
                }
                if (isDragging) {
                    setX(event.getRawX() - offsetX);
                    setY(event.getRawY() - offsetY);
                    // During move, update hovered pile highlighting
                    updateHover();
                }
                return true;
            case MotionEvent.ACTION_UP:
                // clear hover highlight on release
                clearHover();
                long pressDuration = System.currentTimeMillis() - downTimeMs;
                if (!isDragging && pressDuration < 300) {
                    // Treat as tap: animate pop and call performClick for accessibility
                    doTapPop();
                    performClick();
                } else {
                    // It was a drag - snap to pile
                    trySnapToPile();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void doTapPop() {
        if (isPopAnimating || getParent() == null) return;
        isPopAnimating = true;
        // Remember original state
        final float origScaleX = getScaleX();
        final float origScaleY = getScaleY();
        final float origTZ = getTranslationZ();

        // Bring to front visually
        try { bringToFront(); } catch (Exception ignored) {}

        final float targetScale = 1.08f;
        final float targetTZ = Math.max(origTZ, 30f);
        final int upMs = 120;
        final int holdMs = 220;
        final int downMs = 140;

        // Animate up
        animate().scaleX(targetScale).scaleY(targetScale).translationZ(targetTZ).setDuration(upMs).withEndAction(() -> {
            // Hold briefly, then animate back
            postDelayed(() -> {
                animate().scaleX(origScaleX).scaleY(origScaleY).translationZ(origTZ).setDuration(downMs).withEndAction(() -> {
                    isPopAnimating = false;
                }).start();
            }, holdMs);
        }).start();
    }

    // Update which pile (if any) is being hovered while dragging
    private void updateHover() {
        if (allPiles == null || card == null) return;

        PileView best = null;
        float cardCenterX = getX() + getWidth() / 2f;
        float cardCenterY = getY() + getHeight() / 2f;
        float bestDistance = Float.MAX_VALUE;

        // If debug overlay exists and visible, prepare shapes list
        java.util.List<DebugOverlay.Shape> debugShapes = null;
        boolean debugOn = (debugOverlay != null && debugOverlay.getVisibility() == View.VISIBLE);
        if (debugOn) debugShapes = new java.util.ArrayList<>();

        for (PileView pileView : allPiles) {
            if (pileView == null || pileView.getLogicalPile() == null) continue;
            if (!pileView.getLogicalPile().canPlaceCard(this.card)) continue;

            float multiplier = pileView.getLogicalPile().getStackOffsetMultiplier();
            float stackOffset = pileView.getHeight() * multiplier * pileView.getLogicalPile().getCards().size();
            float pileLeft = pileView.getX();
            float pileTop = pileView.getY() + stackOffset;
            float pileRight = pileLeft + pileView.getWidth();
            float pileBottom = pileTop + pileView.getHeight();

            float padding = Math.max(12f, Math.min(getWidth() * 0.25f, pileView.getWidth() * 0.12f));
            if (cardCenterX >= pileLeft - padding && cardCenterX <= pileRight + padding
                    && cardCenterY >= pileTop - padding && cardCenterY <= pileBottom + padding) {
                best = pileView;
                if (debugOn) {
                    // add padded rect and threshold circle shape
                    float thresh = Math.max(getWidth(), pileView.getWidth()) * 0.65f;
                    debugShapes.add(new DebugOverlay.Shape(pileLeft - padding, pileTop - padding, pileRight + padding, pileBottom + padding, pileLeft + pileView.getWidth() / 2f, pileTop + pileView.getHeight() / 2f, thresh, 0xFFFFAA00, pileView.getLabel() == null ? "" : pileView.getLabel()));
                }
                break;
            }

            float pileTargetX = pileLeft + pileView.getWidth() / 2f;
            float pileTargetY = pileTop + pileView.getHeight() / 2f;
            double distance = Math.sqrt(Math.pow(cardCenterX - pileTargetX, 2) + Math.pow(cardCenterY - pileTargetY, 2));
            float threshold = Math.max(getWidth(), pileView.getWidth()) * 0.65f; // slightly tuned
            if (distance < threshold && distance < bestDistance) {
                bestDistance = (float) distance;
                best = pileView;
            }

            if (debugOn) {
                float thresh = Math.max(getWidth(), pileView.getWidth()) * 0.65f;
                debugShapes.add(new DebugOverlay.Shape(pileLeft - padding, pileTop - padding, pileRight + padding, pileBottom + padding, pileTargetX, pileTargetY, thresh, 0xFF00BFFF, pileView.getLabel() == null ? "" : pileView.getLabel()));
            }
        }

        // If best changed, update highlights and fire haptic once
        if (best != hoveredPile) {
            if (hoveredPile != null) hoveredPile.setHighlighted(false);
            hoveredPile = best;
            if (hoveredPile != null) {
                hoveredPile.setHighlighted(true);
                // single short haptic feedback on enter
                try {
                    this.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                } catch (Exception ignored) {}
            }
        }

        if (debugOn && debugOverlay != null) {
            debugOverlay.updateShapes(debugShapes);
        }
    }

    private void clearHover() {
        if (hoveredPile != null) {
            hoveredPile.setHighlighted(false);
            hoveredPile = null;
        }
        if (debugOverlay != null && debugOverlay.getVisibility() == View.VISIBLE) {
            debugOverlay.clear();
        }
    }

    private void trySnapToPile() {
        PileView closestLegalPile = null;
        float minDistance = Float.MAX_VALUE;

        float cardCenterX = getX() + getWidth() / 2f;
        float cardCenterY = getY() + getHeight() / 2f;

        if (allPiles == null || card == null) return;

        for (PileView pileView : allPiles) {
            if (pileView != null && pileView.getLogicalPile() != null) {
                if (pileView.getLogicalPile().canPlaceCard(this.card)) {
                    float multiplier = pileView.getLogicalPile().getStackOffsetMultiplier();
                    float stackOffset = pileView.getHeight() * multiplier * pileView.getLogicalPile().getCards().size();
                    float pileLeft = pileView.getX();
                    float pileTop = pileView.getY() + stackOffset;
                    float pileRight = pileLeft + pileView.getWidth();
                    float pileBottom = pileTop + pileView.getHeight();

                    // First: if card center lies within the pile rect (with small padding), choose it immediately
                    float padding = Math.max(8f, Math.min(getWidth() * 0.25f, pileView.getWidth() * 0.1f));
                    if (cardCenterX >= pileLeft - padding && cardCenterX <= pileRight + padding
                            && cardCenterY >= pileTop - padding && cardCenterY <= pileBottom + padding) {
                        closestLegalPile = pileView;
                        break; // best possible match
                    }

                    // Fallback: distance-based check with pile-aware threshold
                    float pileTargetX = pileLeft + pileView.getWidth() / 2f;
                    float pileTargetY = pileTop + pileView.getHeight() / 2f;

                    double distance = Math.sqrt(Math.pow(cardCenterX - pileTargetX, 2) + Math.pow(cardCenterY - pileTargetY, 2));

                    // Use a threshold that depends on both card and pile size to avoid "swallowing" by neighbors
                    float threshold = Math.max(getWidth(), pileView.getWidth()) * 0.6f;

                    if (distance < threshold && distance < minDistance) {
                        minDistance = (float) distance;
                        closestLegalPile = pileView;
                    }
                }
            }
        }

        // clear hover highlight when snapping
        if (hoveredPile != null) {
            hoveredPile.setHighlighted(false);
            hoveredPile = null;
        }

        if (closestLegalPile != null) {
            snapToPile(closestLegalPile, true);
        } else if (currentPile != null) {
            snapToPile(currentPile, true);
        }
    }

    public void snapToPile(PileView pile, boolean animate) {
        if (pile == null || pile.getLogicalPile() == null || card == null) {
            Log.e("CardView", "Snap failed: Target pile, its logic, or card is null.");
            if (currentPile != null) snapToPile(currentPile, true); // Fallback
            return;
        }

        // Capture previous pile for undo purposes
        PileView previousPile = this.currentPile;

        // Update logic first
        if (currentPile != null && currentPile.getLogicalPile() != null) {
            currentPile.getLogicalPile().removeCard(this.card);
        }
        pile.getLogicalPile().addCard(this.card);

        // Then update views
        if (currentPile != null) {
            currentPile.removeCardView(this);
        }
        pile.addCardView(this); // This calls setCurrentPile

        // Animate to final position
        float multiplier = pile.getLogicalPile().getStackOffsetMultiplier();
        float stackOffset = pile.getHeight() * multiplier * (pile.getLogicalPile().getCards().size() - 1);
        // Center the card horizontally within the pile to avoid misalignment
        float targetX = pile.getX() + (pile.getWidth() - getWidth()) / 2f;
        float targetY = pile.getY() + stackOffset;

        if (animate) {
            animate().x(targetX).y(targetY).setDuration(100).withEndAction(() -> {
                // Notify placement and include previous pile for undo
                if (onPlacedListener != null) {
                    onPlacedListener.onPlaced(card, previousPile, pile);
                }
            }).start();
        } else {
            setX(targetX);
            setY(targetY);
            if (onPlacedListener != null) {
                onPlacedListener.onPlaced(card, previousPile, pile);
            }
        }
    }

    // Getters and Setters
    public void setCurrentPile(PileView pile) {
        this.currentPile = pile;
    }

    public interface OnPlacedListener {
        void onPlaced(Card card, PileView fromPile, PileView toPile);
    }

    private OnPlacedListener onPlacedListener;

    public void setOnPlacedListener(OnPlacedListener listener) { this.onPlacedListener = listener; }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
