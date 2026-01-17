package com.example.tarotsolitaire;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class PileView extends FrameLayout {

    private final Paint paint;
    private final RectF rect;
    private final List<CardView> cardViews = new ArrayList<>(); // List of UI views

    private Pile logicalPile; // The crucial link to the game logic

    public PileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false); // Enable onDraw for this layout

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.WHITE);
        rect = new RectF();
    }

    public PileView(Context context) {
        this(context, null);
    }

    // --- METHODS TO LINK UI AND LOGIC ---
    public void setLogicalPile(Pile pile) {
        this.logicalPile = pile;
    }

    public Pile getLogicalPile() {
        return this.logicalPile;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();

        // Draw the border inset so stroke is fully inside bounds
        float stroke = paint.getStrokeWidth();
        float half = stroke / 2f;
        rect.set(half, half, w - half, h - half);
        float radius = 16f;
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    // --- CARDVIEW MANAGEMENT ---
    public void addCardView(CardView cardView) {
        if (!cardViews.contains(cardView)) {
            cardViews.add(cardView);
            cardView.setCurrentPile(this);
        }
    }

    public void removeCardView(CardView cardView) {
        cardViews.remove(cardView);
        cardView.setCurrentPile(null);
    }

    @SuppressWarnings("unused")
    public List<CardView> getCardViews() {
        return cardViews;
    }

    public CardView getTopCardView() {
        if (cardViews.isEmpty()) {
            return null;
        }
        return cardViews.get(cardViews.size() - 1);
    }
}
