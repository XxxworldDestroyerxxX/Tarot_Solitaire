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

    private float lastX, lastY;
    private Paint paint;
    private RectF rect;
    private List<PileView> piles;

    public CardView(Context context, List<PileView> piles) {
        super(context);
        this.piles = piles;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        rect = new RectF();
        setElevation(12f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(rect, 16f, 16f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                lastX = event.getRawX();
                lastY = event.getRawY();
                bringToFront();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - lastX;
                float dy = event.getRawY() - lastY;

                setX(getX() + dx);
                setY(getY() + dy);

                lastX = event.getRawX();
                lastY = event.getRawY();
                return true;

            case MotionEvent.ACTION_UP:
                snapToNearestPile();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void snapToNearestPile() {
        float cardCenterX = getX() + getWidth() / 2f;
        float cardCenterY = getY() + getHeight() / 2f;

        PileView closestPile = null;
        float closestDistance = Float.MAX_VALUE;

        for (PileView pile : piles) {
            float dx = cardCenterX - pile.centerX();
            float dy = cardCenterY - pile.centerY();
            float distance = (dx * dx) + (dy * dy);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestPile = pile;
            }
        }

        // snap only if close enough
        if (closestPile != null && closestDistance < 200 * 200) {
            setX(closestPile.getX());
            setY(closestPile.getY());
        }
    }
}
