package rkr.simplekeyboard.inputmethod.gesture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class GestureTrailView extends View {
    private final Path mPath = new Path();
    private final Paint mPaint = new Paint();

    public GestureTrailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setColor(0xFF2196F3); // Warna biru Gboard-like
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(12f);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void addPoint(float x, float y) {
        if (mPath.isEmpty()) {
            mPath.moveTo(x, y);
        } else {
            mPath.lineTo(x, y);
        }
        invalidate();
    }

    public void reset() {
        mPath.reset();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(mPath, mPaint);
    }
}