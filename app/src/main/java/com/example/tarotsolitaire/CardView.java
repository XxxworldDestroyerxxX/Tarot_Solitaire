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

    public CardView(Context context, List<PileView> piles) {
        super(context);
        this.piles = piles;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new RectF();
        paint.setColor(Color.RED); // placeholder card color
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(rect, 16f, 16f, paint);
    }

    /* ---------- TOUCH DRAGGING ---------- */
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

    /* ---------- SNAP TO CLOSEST PILE ---------- */
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

        if (closestPile != null) {
            snapToPile(closestPile);
        } else if (currentPile != null) {
            snapToPile(currentPile);
        }
    }

    /* ---------- SNAP LOGIC ---------- */
    public void snapToPile(PileView pile) {
        if (currentPile != null) currentPile.removeCard(this);
        pile.addCard(this);
        currentPile = pile;

        // Offset for stacked display
        float offsetY = pile.getHeight() * 0.3f * (pile.getCards().size() - 1);

        // Convert screen coordinates to root layout
        float rootX = screenToRootX(pile.globalCenterX()) - getWidth() / 2f;
        float rootY = screenToRootY(pile.globalCenterY()) - getHeight() / 2f + offsetY;

        setX(rootX);
        setY(rootY);
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
