package com.example.tarotsolitaire;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class CardView extends View {

    private float offsetX, offsetY;
    private List<PileView> piles;
    private PileView currentPile;

    private Paint paint;
    private RectF rect;

    public CardView(Context context, List<PileView> piles) {
        super(context);
        this.piles = piles;

        // Initialize paint for rounded rectangle
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);

        rect = new RectF();

        // Fixed size (dp)
        int width = dp(60);
        int height = dp(90);
        setLayoutParams(new ViewGroup.LayoutParams(width, height));
    }

    public void setCurrentPile(PileView pile) {
        this.currentPile = pile;
    }

    public PileView getCurrentPile() {
        return currentPile;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw rounded rectangle filling the card
        rect.set(0, 0, getWidth(), getHeight());
        float radius = dp(8); // corner radius
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

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

    /* ---------- SNAP LOGIC ---------- */
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

    /* ---------- SNAP TO SPECIFIC PILE ---------- */
    public void snapToPile(PileView pile) {

        if (currentPile != null) {
            currentPile.removeCard(this);
        }

        pile.addCard(this);
        currentPile = pile;

        // Only one card per pile → no offset needed
        float offsetY = 0;

        // Convert pile center to root coordinates
        float rootX = screenToRootX(pile.globalCenterX()) - getWidth() / 2f;
        float rootY = screenToRootY(pile.globalCenterY()) - getHeight() / 2f + offsetY;

        setX(rootX);
        setY(rootY);
    }

    /* ---------- HELPER: SCREEN -> ROOT COORDINATES ---------- */
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
