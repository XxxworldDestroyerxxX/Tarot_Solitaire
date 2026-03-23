package com.example.tarotsolitaire.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple overlay view for debugging hitboxes and thresholds.
 * Game code can call updateShapes(...) to show pile padded rects and threshold circles.
 */
public class DebugOverlay extends View {

    public static class Shape {
        public float left, top, right, bottom; // padded rect
        public float cx, cy, radius; // threshold circle
        public int color; // color to draw (rect and circle)
        public String label;

        public Shape(float left, float top, float right, float bottom, float cx, float cy, float radius, int color, String label) {
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
            this.cx = cx; this.cy = cy; this.radius = radius; this.color = color; this.label = label;
        }
    }

    private final Paint rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    private final List<Shape> shapes = new ArrayList<>();

    public DebugOverlay(Context context) { super(context); init(); }
    public DebugOverlay(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public DebugOverlay(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(3f);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(2f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        // Do not intercept touch events when visible
        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    public void updateShapes(List<Shape> newShapes) {
        shapes.clear();
        if (newShapes != null) shapes.addAll(newShapes);
        // request draw
        postInvalidateOnAnimation();
    }

    public void clear() {
        shapes.clear();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        for (Shape s : shapes) {
            rectPaint.setColor(s.color);
            circlePaint.setColor(s.color);
            r.set(s.left, s.top, s.right, s.bottom);
            canvas.drawRect(r, rectPaint);
            canvas.drawCircle(s.cx, s.cy, s.radius, circlePaint);
            if (s.label != null) {
                canvas.drawText(s.label, s.left + 6f, s.top + 20f, textPaint);
            }
        }
    }
}
