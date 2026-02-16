package roboyard.ui.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DirectionArrowDrawable extends Drawable {
    private final Paint paint;
    private final int direction; // 1=UP, 2=RIGHT, 4=DOWN, 8=LEFT
    private final int color;
    private final int size;

    public DirectionArrowDrawable(int direction, int color, int size) {
        this.direction = direction;
        this.color = color;
        this.size = size;
        
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int width = getBounds().width();
        int height = getBounds().height();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float arrowSize = Math.min(width, height) * 0.6f;

        Path path = new Path();

        switch (direction) {
            case 1: // UP
                path.moveTo(centerX, centerY - arrowSize / 2);
                path.lineTo(centerX - arrowSize / 2, centerY + arrowSize / 2);
                path.lineTo(centerX + arrowSize / 2, centerY + arrowSize / 2);
                path.close();
                break;
            case 2: // RIGHT
                path.moveTo(centerX + arrowSize / 2, centerY);
                path.lineTo(centerX - arrowSize / 2, centerY - arrowSize / 2);
                path.lineTo(centerX - arrowSize / 2, centerY + arrowSize / 2);
                path.close();
                break;
            case 4: // DOWN
                path.moveTo(centerX, centerY + arrowSize / 2);
                path.lineTo(centerX + arrowSize / 2, centerY - arrowSize / 2);
                path.lineTo(centerX - arrowSize / 2, centerY - arrowSize / 2);
                path.close();
                break;
            case 8: // LEFT
                path.moveTo(centerX - arrowSize / 2, centerY);
                path.lineTo(centerX + arrowSize / 2, centerY + arrowSize / 2);
                path.lineTo(centerX + arrowSize / 2, centerY - arrowSize / 2);
                path.close();
                break;
        }

        canvas.drawPath(path, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return size;
    }

    @Override
    public int getIntrinsicHeight() {
        return size;
    }
}
