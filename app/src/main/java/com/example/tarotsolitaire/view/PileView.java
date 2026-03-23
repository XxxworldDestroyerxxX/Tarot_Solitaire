package com.example.tarotsolitaire.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;

import com.example.tarotsolitaire.model.Pile;
import com.example.tarotsolitaire.R;

import java.util.ArrayList;
import java.util.List;

public class PileView extends FrameLayout {

    private final Paint paint;
    private final RectF rect;
    private final List<CardView> cardViews = new ArrayList<>(); // List of UI views

    private Pile logicalPile; // The crucial link to the game logic

    // Label for organize piles (e.g., "♥ 2→" or "T 0→")
    private String labelText = null;
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean showLock = false; // whether to draw a small lock icon

    // Highlight state for drag-over feedback
    private boolean highlighted = false;
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false); // Enable onDraw for this layout

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.WHITE);
        rect = new RectF();

        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        highlightPaint.setStyle(Paint.Style.FILL);
        // subtle semi-transparent highlight (white tint); you may tweak alpha/color
        highlightPaint.setColor(Color.argb(36, 255, 255, 255));
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

    /**
     * Set a short label to draw at the top of the pile (e.g., suit or tarot rule indicator).
     */
    public void setLabel(String label) {
        this.labelText = label;
        invalidate();
    }

    // Expose the label text for debugging/annotations
    public String getLabel() {
        return this.labelText;
    }

    public void setShowLock(boolean show) {
        this.showLock = show;
        invalidate();
    }

    /**
     * Called by CardView while dragging to indicate this pile is a valid target.
     */
    public void setHighlighted(boolean on) {
        if (this.highlighted == on) return;
        this.highlighted = on;
        invalidate();
    }

    public boolean isHighlighted() { return highlighted; }

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
        // Draw subtle highlight before border if requested
        if (highlighted) {
            canvas.drawRoundRect(rect, radius, radius, highlightPaint);
        }
        canvas.drawRoundRect(rect, radius, radius, paint);

        // Draw label if present
        if (labelText != null && !labelText.isEmpty()) {
            // Choose a size proportional to pile height
            float labelSize = Math.max(10f, Math.min(24f, h * 0.12f));
            labelPaint.setTextSize(labelSize);
            // Draw at top center inside the border with small padding
            float x = w / 2f;
            float y = labelSize + 6f; // simple padding from top
            canvas.drawText(labelText, x, y, labelPaint);
        }

        // Draw lock icon top-right if requested
        if (showLock) {
            float lockSize = Math.max(12f, h * 0.10f);
            labelPaint.setTextSize(lockSize);
            labelPaint.setTextAlign(Paint.Align.RIGHT);
            float lx = w - lockSize / 2f - 6f;
            float ly = lockSize + 6f;
            canvas.drawText(getContext().getString(R.string.lock_icon), lx, ly, labelPaint);
            labelPaint.setTextAlign(Paint.Align.CENTER);
        }
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
