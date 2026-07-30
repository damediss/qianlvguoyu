package com.example.webviewshelldemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

public final class SignatureView extends View {
    /**
     * The exported seating-sign form scales a captured signature down to one cell.
     * Keep the on-device stroke wider so complex glyphs remain legible after print.
     */
    private static final float SIGNATURE_STROKE_WIDTH_DP = 9f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private boolean hasSignature;
    private float lastX;
    private float lastY;

    public SignatureView(Context context) {
        super(context);
        setBackgroundColor(Color.WHITE);
        paint.setColor(Color.rgb(17, 24, 39));
        paint.setStrokeWidth(dp(SIGNATURE_STROKE_WIDTH_DP));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                lastX = x;
                lastY = y;
                hasSignature = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                path.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f);
                lastX = x;
                lastY = y;
                hasSignature = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                path.lineTo(x, y);
                invalidate();
                return true;
            default:
                return true;
        }
    }

    boolean hasSignature() {
        return hasSignature;
    }

    void clearSignature() {
        path.reset();
        hasSignature = false;
        invalidate();
    }

    Bitmap renderToBitmap() {
        int width = Math.max(getWidth(), dp(320f));
        int height = Math.max(getHeight(), dp(220f));
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawPath(path, paint);
        return bitmap;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
