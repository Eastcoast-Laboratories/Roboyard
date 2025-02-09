package driftingdroids.model;

/**
 * Dummy L10n class that just returns the input string.
 * Used as a placeholder for proper localization.
 */
public class L10N {
    public static String getString(String key) {
        return key;
    }
    
    public static String getString(String key, Object... args) {
        try {
            return String.format(key, args);
        } catch (Exception e) {
            return key;
        }
    }
}
