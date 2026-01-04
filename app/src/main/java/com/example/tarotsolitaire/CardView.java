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
    private final Card card;            // the logical card

    private float offsetX, offsetY;     // for dragging
    private final Paint paint;
    private final Paint textPaint;
    private final RectF rect;

    public CardView(Context context, List<PileView> piles, Card card) {
        super(context);
        this.piles = piles;
        this.card = card;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new RectF();

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40f);

        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw card background
        rect.set(0, 0, getWidth(), getHeight());
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(rect, 16f, 16f, paint);

        // Draw card border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(rect, 16f, 16f, paint);

        // Draw rank + suit
        String rankStr;
        switch (card.getRank()) {
            case 1: rankStr = "A"; break;
            case 11: rankStr = "J"; break;
            case 12: rankStr = "Q"; break;
            case 13: rankStr = "K"; break;
            default: rankStr = String.valueOf(card.getRank());
        }
        String text = rankStr + card.getSuit().name().charAt(0);
        float textWidth = textPaint.measureText(text);
        canvas.drawText(text, (getWidth() - textWidth) / 2f, getHeight() / 2f + 15f, textPaint);
    }

    // -------- Touch / Dragging (unchanged) --------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                offsetX = event.getRawX() - getX();
                offsetY = event.getRawY() - getY();
                bringToFront();
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

    // -------- Snap logic (unchanged) --------
    private void trySnapToPile() {
        int[] cardLoc = new int[2];
        getLocationOnScreen(cardLoc);
        float cardCenterX = cardLoc[0] + getWidth() / 2f;
        float cardCenterY = cardLoc[1] + getHeight() / 2f;

        PileView closestPile = null;
        float minDist = dp(40); // snapping radius

        for (PileView pile : piles) {
            float dx = cardCenterX - pile.globalCenterX();
            float dy = cardCenterY - pile.globalCenterY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < minDist) {
                minDist = dist;
                closestPile = pile;
            }
        }

        if (closestPile != null) snapToPile(closestPile);
        else if (currentPile != null) snapToPile(currentPile);
    }

    public void snapToPile(PileView pile) {
        if (currentPile != null) currentPile.removeCard(this);
        pile.addCard(this);
        currentPile = pile;

        float offsetY = pile.getHeight() * 0.3f * (pile.getCards().size() - 1);
        float rootX = screenToRootX(pile.globalCenterX()) - getWidth() / 2f;
        float rootY = screenToRootY(pile.globalCenterY()) - getHeight() / 2f + offsetY;

        setX(rootX);
        setY(rootY);
    }

    public void setCurrentPile(PileView pile) { this.currentPile = pile; }
    public PileView getCurrentPile() { return currentPile; }

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

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
