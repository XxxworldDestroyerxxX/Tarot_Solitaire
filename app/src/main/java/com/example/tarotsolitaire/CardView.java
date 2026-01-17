package com.example.tarotsolitaire;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import java.util.List;

@SuppressLint("ViewConstructor")
public class CardView extends View {

    private List<PileView> allPiles; // may be null when inflated by tools
    private PileView currentPile;
    private Card card; // The logical card (may be null for layout inflation)

    private float offsetX, offsetY;
    private Paint paint;
    private RectF rect;
    private Paint textPaint;

    // Preallocated paints for tarot icon and top-left label to avoid allocations in onDraw
    private Paint iconPaint;
    private Paint iconTextPaint;
    private Paint labelPaint; // bold top-left number indicator

    // Standard constructors so tools/layout inflation won't warn
    public CardView(Context context) {
        super(context);
        init();
    }

    public CardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new RectF();
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // Default text size; will be adjusted based on card height when drawing
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        textPaint.setTextAlign(Paint.Align.CENTER);

        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setColor(Color.rgb(212, 175, 55));

        iconTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconTextPaint.setColor(Color.WHITE);
        iconTextPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        labelPaint.setTextAlign(Paint.Align.LEFT);
        labelPaint.setColor(Color.BLACK);
    }

    @SuppressWarnings("unused")
    public Card getCard() {
        return this.card;
    }

    public void setAllPiles(List<PileView> piles) { this.allPiles = piles; }
    public void setCard(Card card) { this.card = card; }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        rect.set(0, 0, getWidth(), getHeight());

        float radius = 16f;
        float stroke = 4f;

        // Compute top-left label position and size
        float labelX = getWidth() * 0.10f;
        float labelYBase = getHeight() * 0.18f; // baseline approx for top-left label
        float labelSize = Math.max(16f, Math.min(36f, getHeight() * 0.22f));
        labelPaint.setTextSize(labelSize);

        if (card != null && card.getType() == Card.Type.TAROT) {
            // Tarot cards: black background, gold border, white centered number and a small gold icon
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            canvas.drawRoundRect(rect, radius, radius, paint);

            paint.setStyle(Paint.Style.STROKE);
            // gold-ish border
            paint.setColor(Color.rgb(212, 175, 55));
            paint.setStrokeWidth(6f); // thicker border for tarot
            canvas.drawRoundRect(rect, radius, radius, paint);

            // Top-left number indicator (white, bold)
            labelPaint.setColor(Color.WHITE);
            float labelAscent = labelPaint.getFontMetrics().ascent;
            canvas.drawText(card.getRankString(), labelX, labelYBase - labelAscent / 2f, labelPaint);

            // Centered white number (smaller than before) optional: we keep the centered main number but slightly smaller
            textPaint.setColor(Color.WHITE);
            float centerTextSize = Math.max(12f, Math.min(36f, getHeight() * 0.28f));
            textPaint.setTextSize(centerTextSize);
            textPaint.setTextAlign(Paint.Align.CENTER);
            float cx = getWidth() / 2f;
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float cy = getHeight() / 2f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(card.getRankString(), cx, cy, textPaint);

            // Draw small gold circular icon at top-right using preallocated paint
            float iconRadius = Math.max(8f, getWidth() * 0.10f);
            float iconCx = getWidth() - iconRadius - 6f;
            float iconCy = iconRadius + 6f;
            canvas.drawCircle(iconCx, iconCy, iconRadius, iconPaint);

            // White 'T' inside icon
            iconTextPaint.setTextSize(iconRadius);
            Paint.FontMetrics ifm = iconTextPaint.getFontMetrics();
            float iy = iconCy - (ifm.ascent + ifm.descent) / 2f;
            canvas.drawText("T", iconCx, iy, iconTextPaint);

        } else if (card != null) {
            // Standard card appearance: white background, black stroke and colored suit/rank
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(rect, radius, radius, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(stroke);
            canvas.drawRoundRect(rect, radius, radius, paint);

            // Color mapping by suit for label and suit symbol
            int suitColor = Color.BLACK;
            switch (card.getSuit()) {
                case HEARTS:
                    suitColor = Color.parseColor("#D32F2F"); // red
                    break;
                case DIAMONDS:
                    suitColor = Color.parseColor("#1976D2"); // blue
                    break;
                case CLUBS:
                    suitColor = Color.parseColor("#388E3C"); // green
                    break;
                case SPADES:
                    suitColor = Color.parseColor("#FBC02D"); // yellow
                    break;
            }

            // Top-left number indicator (bold, colored by suit)
            labelPaint.setColor(suitColor);
            canvas.drawText(card.getRankString(), labelX, labelYBase - labelPaint.getFontMetrics().ascent / 2f, labelPaint);

            // Suit symbol below the label
            float suitSize = Math.max(12f, labelSize * 0.7f);
            textPaint.setTextSize(suitSize);
            textPaint.setColor(suitColor);
            textPaint.setTextAlign(Paint.Align.LEFT);
            float suitY = getHeight() * 0.42f - (textPaint.getFontMetrics().ascent + textPaint.getFontMetrics().descent) / 2f;
            canvas.drawText(card.getSuitSymbol(), labelX, suitY, textPaint);

        } else {
            // Preview or placeholder - draw blank card outline
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(stroke);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }
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

        if (allPiles == null || card == null) return;

        for (PileView pileView : allPiles) {
            if (pileView != null && pileView.getLogicalPile() != null) {
                if (pileView.getLogicalPile().canPlaceCard(this.card)) {
                    float pileTargetX = pileView.getX() + pileView.getWidth() / 2f;
                    float stackOffset = pileView.getHeight() * 0.28f * pileView.getLogicalPile().getCards().size();
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
        if (pile == null || pile.getLogicalPile() == null || card == null) {
            Log.e("CardView", "Snap failed: Target pile, its logic, or card is null.");
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
        float stackOffset = pile.getHeight() * 0.28f * (pile.getLogicalPile().getCards().size() - 1);
        // Center the card horizontally within the pile to avoid misalignment
        float targetX = pile.getX() + (pile.getWidth() - getWidth()) / 2f;
        float targetY = pile.getY() + stackOffset;

        if (animate) {
            animate().x(targetX).y(targetY).setDuration(100).start();
        } else {
            setX(targetX);
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
