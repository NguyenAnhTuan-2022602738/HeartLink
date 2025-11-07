package vn.haui.heartlink.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import vn.haui.heartlink.R;

/**
 * TextView that renders its text with a horizontal gradient.
 */
public class GradientTextView extends AppCompatTextView {

    private int startColor;
    private int endColor;

    public GradientTextView(Context context) {
        super(context);
        init(context, null);
    }

    public GradientTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GradientTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GradientTextView);
        try {
            startColor = a.getColor(R.styleable.GradientTextView_gradientStartColor,
                    getCurrentTextColor());
            endColor = a.getColor(R.styleable.GradientTextView_gradientEndColor,
                    getCurrentTextColor());
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w == 0) {
            return;
        }
        Shader textShader = new LinearGradient(
                0, 0, w, 0,
                startColor,
                endColor,
                Shader.TileMode.CLAMP
        );
        getPaint().setShader(textShader);
        invalidate();
    }
}
