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

    private Paint paint;
    private RectF rect;

    // Cards in this pile
    private List<CardView> cards = new ArrayList<>();

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

    // Screen-space center
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

    /* ---------- CARD MANAGEMENT ---------- */
    public void addCard(CardView card) {
        cards.add(card);
        card.setCurrentPile(this);
    }

    public void removeCard(CardView card) {
        cards.remove(card);
        card.setCurrentPile(null);
    }

    public List<CardView> getCards() {
        return cards;
    }
}
