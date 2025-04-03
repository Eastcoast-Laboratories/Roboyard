package roboyard.eclabs.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import timber.log.Timber;

/**
 * Utility class to handle font scaling in the app.
 * This allows certain screens to have fixed font sizes regardless of system settings.
 */
public class FontScaleUtil {

    private static final float STANDARD_FONT_SCALE = 1.0f;

    /**
     * Creates a context with the default font scale (1.0) regardless of system settings
     * 
     * @param context Original context
     * @return Context with default font scale
     */
    public static Context createFixedFontScaleContext(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        
        // Check if font scaling is already at the standard scale
        if (Math.abs(configuration.fontScale - STANDARD_FONT_SCALE) < 0.01) {
            return context; // No need to create a new context
        }
        
        // Create a new configuration with the standard font scale
        Configuration newConfig = new Configuration(configuration);
        newConfig.fontScale = STANDARD_FONT_SCALE;
        
        // Create a new context with the modified configuration
        Context newContext = context.createConfigurationContext(newConfig);
        
        Timber.d("Created fixed font scale context. Original scale: %f, New scale: %f", 
                configuration.fontScale, STANDARD_FONT_SCALE);
        
        return newContext;
    }
    
    /**
     * Applies a fixed font scale to the given context's resources
     * This is a more direct approach that modifies the existing resources
     * 
     * @param context Context to modify
     */
    public static void applyFixedFontScale(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        
        // Check if font scaling is already at the standard scale
        if (Math.abs(configuration.fontScale - STANDARD_FONT_SCALE) < 0.01) {
            return; // Already at standard scale
        }
        
        // Apply the standard font scale
        configuration.fontScale = STANDARD_FONT_SCALE;
        
        // Update the configuration
        DisplayMetrics metrics = resources.getDisplayMetrics();
        resources.updateConfiguration(configuration, metrics);
        
        Timber.d("Applied fixed font scale. New scale: %f", STANDARD_FONT_SCALE);
    }
}
