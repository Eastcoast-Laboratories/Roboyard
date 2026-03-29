package roboyard.ui.achievements;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Helper class for loading achievement icons from individual drawable resources.
 * Each achievement icon is a separate drawable file (e.g., 1_lightning.png, 46_flame.png).
 * Each icon is displayed with a circular background in a unique color.
 * 
 * Note: Achievement colors are now centralized in AchievementDefinitions.ACHIEVEMENT_COLORS
 */
public class AchievementIconHelper {
    private static final Map<String, Bitmap> ICON_BITMAP_CACHE = new HashMap<>();
    private static final Map<String, Drawable> ICON_DRAWABLE_CACHE = new HashMap<>();
    private static final int TARGET_ICON_PX = 96;

    
    /**
     * Get a bitmap for a specific icon drawable name.
     * 
     * @param context The context for loading resources
     * @param drawableName The drawable resource name (e.g., "icon_1_lightning")
     * @return The icon bitmap
     * @throws IllegalArgumentException if drawable resource not found
     */
    public static Bitmap getIconBitmap(Context context, String drawableName) {
        if (drawableName == null || drawableName.isEmpty()) {
            Timber.e("[ACHIEVEMENT_ICONS] ERROR: Icon drawable name cannot be null or empty");
            throw new IllegalArgumentException("[ACHIEVEMENT_ICONS] Icon drawable name cannot be null or empty");
        }

        Bitmap cachedBitmap = ICON_BITMAP_CACHE.get(drawableName);
        if (cachedBitmap != null) {
            Timber.d("[ACHIEVEMENT_ICONS] Cache hit for icon: %s", drawableName);
            return cachedBitmap;
        }
        
        Timber.d("[ACHIEVEMENT_ICONS] Attempting to load icon: %s from package: %s", drawableName, context.getPackageName());
        
        int resId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
        if (resId == 0) {
            Timber.e("[ACHIEVEMENT_ICONS] ERROR: Icon drawable not found in resources: %s (resId=0)", drawableName);
            throw new IllegalArgumentException("[ACHIEVEMENT_ICONS] Icon drawable not found: " + drawableName);
        }
        
        Timber.d("[ACHIEVEMENT_ICONS] Found resource ID: %d for icon: %s", resId, drawableName);

        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, boundsOptions);

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = calculateInSampleSize(boundsOptions, TARGET_ICON_PX, TARGET_ICON_PX);
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, decodeOptions);
        if (bitmap == null) {
            Timber.e("[ACHIEVEMENT_ICONS] ERROR: Failed to decode drawable: %s (resId=%d)", drawableName, resId);
            throw new IllegalArgumentException("[ACHIEVEMENT_ICONS] Failed to decode drawable: " + drawableName);
        }
        
        Timber.d("[ACHIEVEMENT_ICONS] SUCCESS: Loaded icon %s (%dx%d pixels)", drawableName, bitmap.getWidth(), bitmap.getHeight());
        ICON_BITMAP_CACHE.put(drawableName, bitmap);
        return bitmap;
    }
    
    /**
     * Get a drawable for a specific icon with a circular background.
     * 
     * @param context The context for loading resources
     * @param drawableName The drawable resource name (e.g., "1_lightning")
     * @param backgroundColor The color of the circular background
     * @return The icon drawable with circular background
     * @throws IllegalArgumentException if drawable resource not found
     */
    public static Drawable getIconDrawableWithCircle(Context context, String drawableName, int backgroundColor) {
        String cacheKey = drawableName + "#" + backgroundColor;
        Drawable cachedDrawable = ICON_DRAWABLE_CACHE.get(cacheKey);
        if (cachedDrawable != null) {
            Timber.d("[ACHIEVEMENT_ICONS] Drawable cache hit for icon: %s", cacheKey);
            return cachedDrawable;
        }

        Bitmap iconBitmap = getIconBitmap(context, drawableName);
        
        // Create a larger circular background from the downsampled icon size
        int iconSize = Math.max(iconBitmap.getWidth(), iconBitmap.getHeight());
        int circleSize = iconSize * 2;
        
        Bitmap circularBitmap = Bitmap.createBitmap(circleSize, circleSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(circularBitmap);
        
        // Draw circular background with anti-aliasing
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(backgroundColor);
        canvas.drawCircle(circleSize / 2f, circleSize / 2f, circleSize / 2f, circlePaint);
        
        // Scale icon to 1.2x larger while keeping it centered in the circle
        // This makes the icon smaller within the circle, leaving visible space around it
        float scaleFactor = 1.2f;
        int scaledIconWidth = (int) (iconBitmap.getWidth() * scaleFactor);
        int scaledIconHeight = (int) (iconBitmap.getHeight() * scaleFactor);
        
        // Scale the icon bitmap
        Bitmap scaledIconBitmap = Bitmap.createScaledBitmap(iconBitmap, scaledIconWidth, scaledIconHeight, true);
        
        // Draw scaled icon in the center
        int offsetX = (circleSize - scaledIconWidth) / 2;
        int offsetY = (circleSize - scaledIconHeight) / 2;
        
        Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(scaledIconBitmap, offsetX, offsetY, iconPaint);

        Drawable drawable = new BitmapDrawable(context.getResources(), circularBitmap);
        ICON_DRAWABLE_CACHE.put(cacheKey, drawable);
        return drawable;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return Math.max(inSampleSize, 1);
    }
    
    /**
     * Get a color for an achievement based on its ID.
     * Delegates to AchievementDefinitions.getAchievementColor() for centralized color management.
     * 
     * @param achievementId The achievement ID (used to determine color)
     * @return The color for this achievement
     */
    public static int getAchievementColor(String achievementId) {
        return AchievementDefinitions.getAchievementColor(achievementId);
    }
    
    
    
    /**
     * Set an ImageView to display a specific icon with circular background.
     * Uses the achievement color based on its ID.
     * 
     * @param context The context for loading resources
     * @param imageView The ImageView to set
     * @param drawableName The drawable resource name (e.g., "1_lightning")
     * @param achievementId The achievement ID for color determination
     * @throws IllegalArgumentException if drawable resource not found
     */
    public static void setIconWithAchievementColor(Context context, ImageView imageView, String drawableName, String achievementId) {
        int color = getAchievementColor(achievementId);
        setIconWithColor(context, imageView, drawableName, color);
    }
    
    /**
     * Set an ImageView to display a specific icon with circular background.
     * 
     * @param context The context for loading resources
     * @param imageView The ImageView to set
     * @param drawableName The drawable resource name (e.g., "1_lightning")
     * @param backgroundColor The color of the circular background
     * @throws IllegalArgumentException if drawable resource not found
     */
    public static void setIconWithColor(Context context, ImageView imageView, String drawableName, int backgroundColor) {
        try {
            Drawable drawable = getIconDrawableWithCircle(context, drawableName, backgroundColor);
            imageView.setImageDrawable(drawable);
        } catch (IllegalArgumentException e) {
            Timber.e(e, "[ACHIEVEMENT_ICONS] Failed to load icon: %s", drawableName);
            throw e;
        }
    }

    public static void clearCaches() {
        ICON_BITMAP_CACHE.clear();
        ICON_DRAWABLE_CACHE.clear();
        Timber.d("[ACHIEVEMENT_ICONS] Caches cleared");
    }
}
