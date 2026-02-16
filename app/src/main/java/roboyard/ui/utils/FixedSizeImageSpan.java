package roboyard.ui.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;

/**
 * Custom ImageSpan that maintains fixed size without scaling relative to text size.
 * This prevents arrows from shrinking when they appear in abbreviated history text.
 */
public class FixedSizeImageSpan extends ImageSpan {
    private final int fixedWidth;
    private final int fixedHeight;

    public FixedSizeImageSpan(@NonNull Drawable drawable, int fixedWidth, int fixedHeight) {
        super(drawable, ALIGN_CENTER);
        this.fixedWidth = fixedWidth;
        this.fixedHeight = fixedHeight;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt fm) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setBounds(0, 0, fixedWidth, fixedHeight);
        }
        
        if (fm != null) {
            fm.ascent = -fixedHeight;
            fm.descent = 0;
            fm.top = -fixedHeight;
            fm.bottom = 0;
        }
        
        return fixedWidth;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, @NonNull Paint paint) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setBounds(0, 0, fixedWidth, fixedHeight);
            int transY = bottom - fixedHeight;
            canvas.save();
            canvas.translate(x, transY);
            drawable.draw(canvas);
            canvas.restore();
        }
    }
}
