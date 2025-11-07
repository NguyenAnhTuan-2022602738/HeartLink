package vn.haui.heartlink.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import vn.haui.heartlink.R;

/**
 * Custom dashed wave used in onboarding screens.
 */
public class WaveView extends View {

    private final Paint paint = new Paint();
    private final Path path = new Path();

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setAntiAlias(true);
        paint.setAlpha(153); // 60% opacity
        paint.setPathEffect(new DashPathEffect(new float[]{20, 20}, 0));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        path.reset();
        int width = getWidth();
        int height = getHeight();

        path.moveTo(0, height / 2f);
        path.cubicTo(width * 0.25f, 0, width * 0.75f, height, width, height / 2f);

        canvas.drawPath(path, paint);
    }
}
