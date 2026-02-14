package roboyard.eclabs.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.Locale;

import roboyard.eclabs.R;
import roboyard.eclabs.util.FontScaleUtil;
import roboyard.logic.core.Preferences;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * Base fragment class for all game screens.
 * Provides common functionality and access to the GameStateManager.
 */
public abstract class BaseGameFragment extends Fragment {
    
    protected GameStateManager gameStateManager;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply language settings before creating the fragment
        applyLanguageSettings();
        
        super.onCreate(savedInstanceState);
        // Get the GameStateManager from the activity
        gameStateManager = new ViewModelProvider(requireActivity()).get(GameStateManager.class);
    }
    
    /**
     * Override onAttach to apply fixed font scaling
     * This ensures consistent text sizes regardless of system settings
     */
    @Override
    public void onAttach(Context context) {
        // Apply fixed font scaling to ensure consistent UI
        Context fixedContext = FontScaleUtil.createFixedFontScaleContext(context);
        super.onAttach(fixedContext);
    }
    
    /**
     * Navigate to another screen using direct fragment transaction
     * This method should be used instead of Navigation component when mixing navigation approaches
     * @param fragment The fragment to navigate to
     * @param addToBackStack Whether to add the transaction to the back stack
     * @param tag Optional tag for the fragment transaction
     */
    protected void navigateToDirect(Fragment fragment, boolean addToBackStack, String tag) {
        try {
            // Create the transaction
            androidx.fragment.app.FragmentTransaction transaction = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment);
            
            // Add to back stack if requested
            if (addToBackStack) {
                transaction.addToBackStack(tag);
            }
            
            // Commit the transaction
            transaction.commit();
            
            // Log success
            Timber.d("Navigation to %s completed using fragment transaction", 
                    fragment.getClass().getSimpleName());
        } catch (Exception e) {
            // Log error
            Timber.e(e, "Error navigating to %s", fragment.getClass().getSimpleName());
        }
    }
    
    /**
     * Simplified version of navigateToDirect that adds to back stack with null tag
     * @param fragment The fragment to navigate to
     */
    protected void navigateToDirect(Fragment fragment) {
        navigateToDirect(fragment, true, null);
    }
    
    /**
     * Get the screen title for accessibility and UI
     * @return The screen title
     */
    public abstract String getScreenTitle();
    
    /**
     * Applies the language settings to the application context
     */
    private void applyLanguageSettings() {
        try {
            // Get saved language setting
            String languageCode = Preferences.appLanguage;
            Timber.d("ROBOYARD_LANGUAGE: Loading saved language in fragment: %s", languageCode);
            
            if (languageCode != null && !languageCode.isEmpty()) {
                // Apply language change
                Locale locale = new Locale(languageCode);
                Locale.setDefault(locale);
                
                Resources resources = requireContext().getResources();
                Configuration config = new Configuration(resources.getConfiguration());
                config.setLocale(locale);
                
                resources.updateConfiguration(config, resources.getDisplayMetrics());
                Timber.d("ROBOYARD_LANGUAGE: Successfully applied language %s in fragment", languageCode);
            }
        } catch (Exception e) {
            Timber.e(e, "ROBOYARD_LANGUAGE: Error loading language settings in fragment");
        }
    }
}
