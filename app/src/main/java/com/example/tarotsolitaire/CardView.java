package com.example.tarotsolitaire;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

public class CardView extends View {

    private final List<PileView> piles; // all piles to snap to
    private PileView currentPile;       // the pile this card currently belongs to

    private float offsetX, offsetY;     // for dragging
    private Paint paint;
    private RectF rect;
    private Card card;                  // logical card (rank/suit)

    public CardView(Context context, List<PileView> piles, Card card) {
        super(context);
        this.piles = piles;
        this.card = card;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new RectF();

        // Set all cards to be white
        paint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw rounded rectangle for card
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(rect, 16f, 16f, paint);

        // Draw rank and suit in top-left corner
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK); // Use black for text for good contrast
        textPaint.setTextSize(getHeight() * 0.2f);
        canvas.drawText(card.getRankString(), getWidth() * 0.1f, getHeight() * 0.25f, textPaint);
        canvas.drawText(card.getSuitSymbol(), getWidth() * 0.1f, getHeight() * 0.45f, textPaint);
    }

    /* ---------- TOUCH DRAGGING ---------- */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // --- THIS IS THE NEW LOGIC ---
        // First, check if this card is allowed to be moved.
        // It can only be moved if it's the top card of its pile.
        if (currentPile != null && this != currentPile.getTopCard()) {
            // If this card is not the top card, consume the touch event but do nothing.
            // This prevents the touch from "going through" to cards underneath.
            return true;
        }
        // --------------------------------

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                offsetX = event.getRawX() - getX();
                offsetY = event.getRawY() - getY();
                bringToFront(); // Still bring to front to ensure it's drawn over other piles
                return true;

            case MotionEvent.ACTION_MOVE:
                setX(event.getRawX() - offsetX);
                setY(event.getRawY() - offsetY);
                return true;

            case MotionEvent.ACTION_UP:
                trySnapToPile();
                return true;
        }
        return super.onTouchEvent(event);
    }

    /* ---------- SNAP TO CLOSEST PILE ---------- */
    private void trySnapToPile() {
        int[] cardLoc = new int[2];
        getLocationOnScreen(cardLoc);
        float cardCenterX = cardLoc[0] + getWidth() / 2f;
        float cardCenterY = cardLoc[1] + getHeight() / 2f;

        PileView closestPile = null;
        float minDist = dp(40); // snapping radius

        for (PileView pile : piles) {
            // Use the original globalCenterX for the X coordinate
            float dx = cardCenterX - pile.globalCenterX();
            // *** Use the NEW getSnapCenterY() for the Y coordinate ***
            float dy = cardCenterY - pile.getSnapCenterY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < minDist) {
                minDist = dist;
                closestPile = pile;
            }
        }

        if (closestPile != null) {
            snapToPile(closestPile);
        } else if (currentPile != null) {
            // If no new pile is found, snap back to the original pile
            snapToPile(currentPile);
        }
    }

    /* ---------- SNAP LOGIC ---------- */
    public void snapToPile(PileView pile) {
        // Remove from old pile (if any) before adding to new one
        if (currentPile != null && currentPile != pile) {
            currentPile.removeCard(this);
        }

        // Add to the new pile's logic
        pile.addCard(this);
        currentPile = pile;

        // Calculate the visual offset for stacking.
        // The offset is based on the card's new position in the pile list.
        float stackOffset = pile.getHeight() * 0.3f * (pile.getCards().size() - 1);

        // Convert the pile's base screen coordinates to the root layout's coordinate system
        float rootX = screenToRootX(pile.globalCenterX()) - getWidth() / 2f;
        float rootY = screenToRootY(pile.globalCenterY()) - getHeight() / 2f + stackOffset;

        // Animate the card to its new position for a smooth snap
        animate().x(rootX).y(rootY).setDuration(100).start();
    }


    /* ---------- CURRENT PILE GETTER/SETTER ---------- */
    public void setCurrentPile(PileView pile) {
        this.currentPile = pile;
    }

    public PileView getCurrentPile() {
        return currentPile;
    }

    /* ---------- HELPER METHODS ---------- */
    private float screenToRootX(float screenX) {
        int[] rootLoc = new int[2];
        ((View) getParent()).getLocationOnScreen(rootLoc);
        return screenX - rootLoc[0];
    }

    private float screenToRootY(float screenY) {
        int[] rootLoc = new int[2];
        ((View) getParent()).getLocationOnScreen(rootLoc);
        return screenY - rootLoc[1];
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
