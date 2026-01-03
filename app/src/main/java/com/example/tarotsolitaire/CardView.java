package com.example.tarotsolitaire;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class CardView extends View {

    private float lastX, lastY;
    private Paint paint;
    private RectF rect;

    public CardView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        rect = new RectF();
        setElevation(8f); // shadow
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
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
        }
        return super.onTouchEvent(event);
    }
}
