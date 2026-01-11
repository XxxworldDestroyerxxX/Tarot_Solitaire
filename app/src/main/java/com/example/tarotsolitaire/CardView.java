package com.example.tarotsolitaire;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.List;

public class CardView extends View {

    private final List<PileView> allPiles;
    private PileView currentPile;
    private final Card card; // The logical card

    private float offsetX, offsetY;
    private final Paint paint;
    private final RectF rect;
    private final Paint textPaint;

    public CardView(Context context, List<PileView> piles, Card card) {
        super(context);
        this.allPiles = piles;
        this.card = card;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        rect = new RectF();
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
    }

    public Card getCard() {
        return this.card;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(rect, 16f, 16f, paint);

        textPaint.setTextSize(getHeight() * 0.2f);
        canvas.drawText(card.getRankString(), getWidth() * 0.15f, getHeight() * 0.25f, textPaint);
        canvas.drawText(card.getSuitSymbol(), getWidth() * 0.15f, getHeight() * 0.45f, textPaint);
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
                bringToFront();
                performClick();
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

    private void trySnapToPile() {
        PileView closestLegalPile = null;
        float minDistance = Float.MAX_VALUE;

        float cardCenterX = getX() + getWidth() / 2f;
        float cardCenterY = getY() + getHeight() / 2f;

        if (allPiles == null) return;

        for (PileView pileView : allPiles) {
            if (pileView != null && pileView.getLogicalPile() != null) {
                if (pileView.getLogicalPile().canPlaceCard(this.card)) {
                    float pileTargetX = pileView.getX() + pileView.getWidth() / 2f;
                    float stackOffset = pileView.getHeight() * 0.3f * pileView.getLogicalPile().getCards().size();
                    float pileTargetY = pileView.getY() + stackOffset;

                    double distance = Math.sqrt(Math.pow(cardCenterX - pileTargetX, 2) + Math.pow(cardCenterY - pileTargetY, 2));

                    if (distance < getWidth() && distance < minDistance) {
                        minDistance = (float) distance;
                        closestLegalPile = pileView;
                    }
                }
            }
        }

        if (closestLegalPile != null) {
            snapToPile(closestLegalPile, true);
        } else if (currentPile != null) {
            snapToPile(currentPile, true);
        }
    }

    public void snapToPile(PileView pile, boolean animate) {
        if (pile == null || pile.getLogicalPile() == null) {
            Log.e("CardView", "Snap failed: Target pile or its logic is null.");
            if (currentPile != null) snapToPile(currentPile, true); // Fallback
            return;
        }

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
        float stackOffset = pile.getHeight() * 0.3f * (pile.getLogicalPile().getCards().size() - 1);
        float targetX = pile.getX();
        float targetY = pile.getY() + stackOffset;

        if (animate) {
            animate().x(targetX).y(targetY).setDuration(100).start();
        } else {
            setX((float) (targetX + 10));
            setY(targetY);
        }
    }

    // Getters and Setters
    public void setCurrentPile(PileView pile) {
        this.currentPile = pile;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
