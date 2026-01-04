package com.example.tarotsolitaire;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.List;

public class CardView extends View {

    private float offsetX, offsetY;
    private List<PileView> piles;

    public CardView(Context context, List<PileView> piles) {
        super(context);
        this.piles = piles;
        setBackgroundColor(Color.RED);
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

        for (PileView pile : piles) {
            float dx = cardCenterX - pile.globalCenterX();
            float dy = cardCenterY - pile.globalCenterY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < dp(40)) {
                snapToPile(pile);
                break;
            }
        }
    }

    private void snapToPile(PileView pile) {
        float rootX = screenToRootX(pile.globalCenterX());
        float rootY = screenToRootY(pile.globalCenterY());

        setX(rootX - getWidth() / 2f);
        setY(rootY - getHeight() / 2f);
    }

    /* ---------- COORDINATE CONVERSION ---------- */

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
