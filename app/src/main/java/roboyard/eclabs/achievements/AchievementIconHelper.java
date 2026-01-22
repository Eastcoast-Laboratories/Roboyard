package roboyard.eclabs.achievements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import roboyard.eclabs.R;
import timber.log.Timber;

/**
 * Helper class for loading achievement icons from the sprite sheet.
 * The sprite sheet contains 64 icons in an 8x8 grid, each icon is exactly 1/8 of the total width.
 * Each icon is displayed with a circular background in a unique color.
 */
public class AchievementIconHelper {
    
    private static final int GRID_COLS = 8;
    private static final int GRID_ROWS = 8;
    private static final int TOTAL_ICONS = 64;
    
    // Predefined color palette for achievement circles (58 colors for 58 achievements)
    private static final int[] ACHIEVEMENT_COLORS = {
        0xFF4CAF50, // Green
        0xFFFF9800, // Orange
        0xFF2196F3, // Blue
        0xFFE91E63, // Pink
        0xFF9C27B0, // Purple
        0xFF00BCD4, // Cyan
        0xFFFFC107, // Amber
        0xFF8BC34A, // Light Green
        0xFFFF5722, // Deep Orange
        0xFF3F51B5, // Indigo
        0xFFF44336, // Red
        0xFF009688, // Teal
        0xFFBF360C, // Dark Red
        0xFF1565C0, // Dark Blue
        0xFF00897B, // Dark Teal
        0xFFD32F2F, // Red 700
        0xFF1976D2, // Blue 700
        0xFF0097A7, // Cyan 700
        0xFFC2185B, // Pink 700
        0xFF6A1B9A, // Purple 700
        0xFF00695C, // Teal 700
        0xFFF57F17, // Amber 900
        0xFF33691E, // Light Green 900
        0xFFBF360C, // Deep Orange 900
        0xFF1A237E, // Indigo 900
        0xFFB71C1C, // Red 900
        0xFF004D40, // Teal 900
        0xFF880E4F, // Pink 900
        0xFF4A148C, // Purple 900
        0xFF1B5E20, // Green 900
        0xFFE65100, // Deep Orange 800
        0xFF0D47A1, // Blue 900
        0xFF006064, // Cyan 900
        0xFFAD1457, // Pink 800
        0xFF512DA8, // Purple 800
        0xFF00838F, // Cyan 800
        0xFFFBC02D, // Amber 600
        0xFF7CB342, // Light Green 600
        0xFFFF6E40, // Deep Orange 400
        0xFF5C6BC0, // Indigo 400
        0xFFEF5350, // Red 400
        0xFF26C6DA, // Cyan 400
        0xFFAB47BC, // Purple 400
        0xFF29B6F6, // Light Blue 400
        0xFFEC407A, // Pink 400
        0xFF66BB6A, // Green 400
        0xFFFFCA28, // Amber 400
        0xFFFF7043, // Deep Orange 300
        0xFF7986CB, // Indigo 300
        0xFFEF9A9A, // Red 200
        0xFF80DEEA, // Cyan 200
        0xFFCE93D8, // Purple 200
        0xFF81D4FA, // Light Blue 200
        0xFFF48FB1, // Pink 200
        0xFFA5D6A7, // Green 200
        0xFFFFE082, // Amber 200
        0xFFFFAB91, // Deep Orange 200
    };
    
    private static Bitmap spriteSheet = null;
    private static int iconWidth = 0;
    private static int iconHeight = 0;
    
    /**
     * Initialize the sprite sheet from resources.
     * Call this once at app startup or lazily on first use.
     */
    public static synchronized void initialize(Context context) {
        if (spriteSheet == null) {
            spriteSheet = BitmapFactory.decodeResource(context.getResources(), 
                    R.drawable.achievements_icons_64);
            if (spriteSheet != null) {
                // Each icon is exactly 1/8 of the total width
                iconWidth = spriteSheet.getWidth() / GRID_COLS;
                iconHeight = spriteSheet.getHeight() / GRID_ROWS;
                Timber.d("[ACHIEVEMENT_ICONS] Sprite sheet loaded: %dx%d, icon size: %dx%d",
                        spriteSheet.getWidth(), spriteSheet.getHeight(), iconWidth, iconHeight);
            } else {
                Timber.e("[ACHIEVEMENT_ICONS] Failed to load sprite sheet!");
            }
        }
    }
    
    /**
     * Get a bitmap for a specific icon index (0-63).
     * Icons are numbered left-to-right, top-to-bottom.
     * 
     * @param context The context for loading resources
     * @param index The icon index (0-63)
     * @return The icon bitmap, or null if invalid index or not initialized
     */
    public static Bitmap getIconBitmap(Context context, int index) {
        initialize(context);
        
        if (spriteSheet == null || index < 0 || index >= TOTAL_ICONS) {
            Timber.w("[ACHIEVEMENT_ICONS] Invalid icon index: %d", index);
            return null;
        }
        
        int col = index % GRID_COLS;
        int row = index / GRID_COLS;
        
        int x = col * iconWidth;
        int y = row * iconHeight;
        
        return Bitmap.createBitmap(spriteSheet, x, y, iconWidth, iconHeight);
    }
    
    /**
     * Get a drawable for a specific icon index with a circular background.
     * 
     * @param context The context for loading resources
     * @param index The icon index (0-63)
     * @param backgroundColor The color of the circular background
     * @return The icon drawable with circular background, or null if invalid index
     */
    public static Drawable getIconDrawableWithCircle(Context context, int index, int backgroundColor) {
        Bitmap iconBitmap = getIconBitmap(context, index);
        if (iconBitmap == null) {
            return null;
        }
        
        // Create a larger circular background (2x the icon size for better appearance)
        int iconSize = Math.max(iconBitmap.getWidth(), iconBitmap.getHeight());
        int circleSize = iconSize * 2;
        
        Bitmap circularBitmap = Bitmap.createBitmap(circleSize, circleSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(circularBitmap);
        
        // Draw circular background with anti-aliasing
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(backgroundColor);
        canvas.drawCircle(circleSize / 2f, circleSize / 2f, circleSize / 2f, circlePaint);
        
        // Scale icon to 1.7x larger while keeping it centered in the circle
        float scaleFactor = 1.7f;
        int scaledIconWidth = (int) (iconBitmap.getWidth() * scaleFactor);
        int scaledIconHeight = (int) (iconBitmap.getHeight() * scaleFactor);
        
        // Scale the icon bitmap
        Bitmap scaledIconBitmap = Bitmap.createScaledBitmap(iconBitmap, scaledIconWidth, scaledIconHeight, true);
        
        // Draw scaled icon in the center
        int offsetX = (circleSize - scaledIconWidth) / 2;
        int offsetY = (circleSize - scaledIconHeight) / 2;
        
        Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(scaledIconBitmap, offsetX, offsetY, iconPaint);
        
        return new BitmapDrawable(context.getResources(), circularBitmap);
    }
    
    /**
     * Get a color for an achievement based on its ID.
     * Each achievement gets a unique color from the predefined palette.
     * 
     * @param achievementId The achievement ID (used to determine color)
     * @return The color for this achievement
     */
    public static int getAchievementColor(String achievementId) {
        // Use hash code of achievement ID to get a consistent color
        int hash = achievementId.hashCode();
        int colorIndex = Math.abs(hash) % ACHIEVEMENT_COLORS.length;
        return ACHIEVEMENT_COLORS[colorIndex];
    }
    
    /**
     * Get a drawable for a specific icon index with a circular background.
     * Uses a default light gray background color.
     * 
     * @param context The context for loading resources
     * @param index The icon index (0-63)
     * @return The icon drawable with circular background, or null if invalid index
     */
    public static Drawable getIconDrawable(Context context, int index) {
        return getIconDrawableWithCircle(context, index, Color.parseColor("#E0E0E0"));
    }
    
    /**
     * Set an ImageView to display a specific icon from the sprite sheet with circular background.
     * 
     * @param context The context for loading resources
     * @param imageView The ImageView to set
     * @param index The icon index (0-63)
     */
    public static void setIcon(Context context, ImageView imageView, int index) {
        setIconWithColor(context, imageView, index, Color.parseColor("#E0E0E0"));
    }
    
    /**
     * Set an ImageView to display a specific icon from the sprite sheet with circular background.
     * Uses the achievement color based on its ID.
     * 
     * @param context The context for loading resources
     * @param imageView The ImageView to set
     * @param index The icon index (0-63)
     * @param achievementId The achievement ID for color determination
     */
    public static void setIconWithAchievementColor(Context context, ImageView imageView, int index, String achievementId) {
        int color = getAchievementColor(achievementId);
        setIconWithColor(context, imageView, index, color);
    }
    
    /**
     * Set an ImageView to display a specific icon from the sprite sheet with circular background.
     * 
     * @param context The context for loading resources
     * @param imageView The ImageView to set
     * @param index The icon index (0-63)
     * @param backgroundColor The color of the circular background
     */
    public static void setIconWithColor(Context context, ImageView imageView, int index, int backgroundColor) {
        Drawable drawable = getIconDrawableWithCircle(context, index, backgroundColor);
        if (drawable != null) {
            imageView.setImageDrawable(drawable);
        } else {
            // Fallback to first icon in sprite sheet (index 0)
            Drawable fallback = getIconDrawableWithCircle(context, 0, backgroundColor);
            if (fallback != null) {
                imageView.setImageDrawable(fallback);
            }
        }
    }
    
    /**
     * Clear the cached sprite sheet to free memory.
     * Call this when the app is low on memory.
     */
    public static synchronized void clearCache() {
        if (spriteSheet != null && !spriteSheet.isRecycled()) {
            spriteSheet.recycle();
        }
        spriteSheet = null;
        iconWidth = 0;
        iconHeight = 0;
        Timber.d("[ACHIEVEMENT_ICONS] Cache cleared");
    }
}
