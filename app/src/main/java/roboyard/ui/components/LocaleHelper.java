package roboyard.ui.components;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

import timber.log.Timber;
import roboyard.logic.core.Preferences;

/**
 * Helper class to manage application locales and accessibility language settings.
 * This class facilitates language changes for both the UI and TalkBack announcements.
 */
public class LocaleHelper {
    
    /**
     * Gets the appropriate locale for accessibility announcements based on user settings
     * @param context Application context
     * @return Locale to use for accessibility announcements
     */
    public static Locale getAccessibilityLocale(Context context) {
        // Get current app language and TalkBack language settings
        String appLanguage = Preferences.appLanguage;
        String talkbackLanguage = Preferences.talkbackLanguage;
        
        // Get the language code to use for TalkBack
        String languageCode;
        
        if ("same".equals(talkbackLanguage)) {
            // Use the app language for TalkBack
            languageCode = appLanguage;
            Timber.d("ROBOYARD_A11Y_LANGUAGE: Using app language '%s' for TalkBack", languageCode);
        } else {
            // Use the specifically selected TalkBack language
            languageCode = talkbackLanguage;
            Timber.d("ROBOYARD_A11Y_LANGUAGE: Using specific language '%s' for TalkBack", languageCode);
        }
        
        // Create and return the locale
        return new Locale(languageCode);
    }
    
    /**
     * Sets the app's locale based on user preferences
     * @param context Application context
     * @return Context with updated locale configuration
     */
    public static Context setAppLocale(Context context) {
        String language = Preferences.appLanguage;
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }
        
        Timber.d("ROBOYARD_LOCALE: Set app locale to %s", language);
        
        return context;
    }
    
    /**
     * Gets localized text for accessibility announcements
     * @param context Application context
     * @param resId Resource ID of the string to localize
     * @param args Format arguments for the string
     * @return Localized string in the accessibility language
     */
    public static String getLocalizedAccessibilityText(Context context, int resId, Object... args) {
        try {
            // Get the locale for accessibility
            Locale accessibilityLocale = getAccessibilityLocale(context);
            
            // Create a configuration with the accessibility locale
            Resources resources = context.getResources();
            Configuration config = new Configuration(resources.getConfiguration());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLocale(accessibilityLocale);
            } else {
                config.locale = accessibilityLocale;
            }
            
            // Create a temporary context with the correct locale
            Context tempContext = context.createConfigurationContext(config);
            
            // Get the localized string from the temporary context
            String localizedString = tempContext.getString(resId, args);
            
            // Log for debugging
            Timber.d("ROBOYARD_A11Y_TEXT: Localized %s to '%s' in language '%s'", 
                    context.getResources().getResourceName(resId),
                    localizedString, 
                    accessibilityLocale.getLanguage());
            
            return localizedString;
        } catch (Exception e) {
            Timber.e(e, "Error getting localized text for accessibility");
            // Fall back to default string in case of errors
            return context.getString(resId, args);
        }
    }
}
