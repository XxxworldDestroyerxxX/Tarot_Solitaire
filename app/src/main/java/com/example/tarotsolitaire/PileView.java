package com.example.tarotsolitaire;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PileView extends View {

    private final Paint paint;
    private final RectF rect;

    // Cards currently in this pile
    private final List<CardView> cards = new ArrayList<>();

    public PileView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.WHITE);
        rect = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(rect, 16f, 16f, paint);
    }

    /* ---------- SCREEN SPACE CENTER ---------- */
    public float globalCenterX() {
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        return loc[0] + getWidth() / 2f;
    }

    public float globalCenterY() {
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        return loc[1] + getHeight() / 2f;
    }

    /**
     * Calculates the Y-coordinate for where the NEXT card should snap.
     * This is on top of the current highest card in the pile.
     */
    public float getSnapCenterY() {
        // Get the base Y-coordinate (the center of the invisible pile outline)
        float baseY = globalCenterY();

        if (cards.isEmpty()) {
            // If the pile is empty, the snap target is just the center of the pile.
            return baseY;
        } else {
            // If there are cards, the snap target is the top of the stack.
            // We get a sample card's height to calculate the visual offset between cards.
            float cardHeight = cards.get(0).getHeight();
            // This offset MUST match the visual stacking offset used in CardView.snapToPile().
            float offsetPerCard = cardHeight * 0.3f;
            // The total offset is based on the number of cards already in the pile.
            float totalOffset = offsetPerCard * cards.size();

            // The new snap position is the pile's base Y plus the total offset.
            return baseY + totalOffset;
        }
    }

    /* ---------- CARD MANAGEMENT ---------- */
    public void addCard(CardView card) {
        if (!cards.contains(card)) {
            cards.add(card);
            card.setCurrentPile(this);
        }
    }

    public void removeCard(CardView card) {
        if (cards.contains(card)) {
            cards.remove(card);
            card.setCurrentPile(null);
        }
    }

    public List<CardView> getCards() {
        return cards;
    }

    /**
     * Gets the CardView that is visually on top of the pile.
     * @return The top CardView, or null if the pile is empty.
     */
    public CardView getTopCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.get(cards.size() - 1);
    }
}
