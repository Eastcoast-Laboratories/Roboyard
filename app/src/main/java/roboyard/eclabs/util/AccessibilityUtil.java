package roboyard.eclabs.util;

import android.content.Context;
import android.view.accessibility.AccessibilityManager;

public class AccessibilityUtil {
    public static boolean isScreenReaderActive(Context context) {
        if (context == null) return false;
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return am != null && am.isEnabled() && am.isTouchExplorationEnabled();
    }
    
    public static void announceForAccessibility(Context context, String message) {
        // Implementation would go here
    }
}
