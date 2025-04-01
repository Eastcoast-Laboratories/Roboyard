package roboyard.eclabs.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import roboyard.logic.core.GameState;

//import roboyard.eclabs.GameState;

/**
 * Custom view that displays a miniature version of the game board.
 * This view efficiently uses the MinimapGenerator to create a bitmap
 * representation of the current game state.
 */
public class MiniMapView extends View {

    private GameState gameState;
    private Bitmap miniMapBitmap;
    private Paint paint;
    private Rect destRect;
    private int width = 0;
    private int height = 0;
    private boolean needsRedraw = true;

    public MiniMapView(Context context) {
        super(context);
        init();
    }

    public MiniMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MiniMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setFilterBitmap(true);
        destRect = new Rect();
        
        // Set contentDescription for accessibility
        setContentDescription(getContext().getString(roboyard.eclabs.R.string.minimap_description));
    }

    /**
     * Set the game state to display
     * @param gameState The game state to display
     */
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        needsRedraw = true;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        destRect.set(0, 0, width, height);
        needsRedraw = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background
        canvas.drawColor(Color.BLACK);

        // Create or update minimap bitmap if needed
        if (gameState != null && (miniMapBitmap == null || needsRedraw)) {
            miniMapBitmap = gameState.getMiniMap(getContext(), width, height);
            needsRedraw = false;
        }

        // Draw the minimap bitmap if available
        if (miniMapBitmap != null) {
            canvas.drawBitmap(miniMapBitmap, null, destRect, paint);
        }
    }

    /**
     * Force the minimap to redraw on next draw pass
     */
    public void forceRedraw() {
        needsRedraw = true;
        invalidate();
    }

    /**
     * Clear any cached bitmaps
     */
    public void clearCache() {
        if (miniMapBitmap != null) {
            miniMapBitmap.recycle();
            miniMapBitmap = null;
        }
        needsRedraw = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearCache();
    }
}
