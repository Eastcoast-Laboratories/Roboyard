package roboyard.ui.components;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.View;

/**
 * Utility class for accessibility features, particularly for TalkBack support
 */
public class AccessibilityUtil {
    
    /**
     * Checks if a screen reader (TalkBack) is active
     * @param context The application context
     * @return true if a screen reader is active, false otherwise
     */
    public static boolean isScreenReaderActive(Context context) {
        if (context == null) {
            return false;
        }
        
        try {
            AccessibilityManager accessibilityManager = 
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            
            return accessibilityManager != null &&
                   accessibilityManager.isEnabled() &&
                   accessibilityManager.isTouchExplorationEnabled();
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Sets a content description on a View only if a screen reader is active
     * @param context The application context
     * @param view The view to set the content description on
     * @param description The content description to set
     */
    public static void setContentDescription(Context context, View view, String description) {
        if (view == null) return;
        
        if (isScreenReaderActive(context)) {
            view.setContentDescription(description);
        }
    }
    
    /**
     * Announce a message for accessibility services
     * @param context The context
     * @param message The message to announce
     */
    public static void announceForAccessibility(Context context, String message) {
        if (context == null || message == null || message.isEmpty()) {
            return;
        }
        
        try {
            AccessibilityManager accessibilityManager = 
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            
            if (accessibilityManager != null && accessibilityManager.isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
                event.getText().add(message);
                accessibilityManager.sendAccessibilityEvent(event);
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }
}
