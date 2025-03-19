package roboyard.eclabs.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import roboyard.eclabs.R;

/**
 * Settings screen implemented as a Fragment with native Android UI components.
 * Replaces the canvas-based SettingsScreen.
 */
public class SettingsFragment extends BaseGameFragment {
    
    private Switch soundEnabledSwitch;
    private Switch vibrationEnabledSwitch;
    private Switch highContrastSwitch;
    private Button backButton;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // Set up UI elements
        soundEnabledSwitch = view.findViewById(R.id.sound_enabled_switch);
        vibrationEnabledSwitch = view.findViewById(R.id.vibration_enabled_switch);
        highContrastSwitch = view.findViewById(R.id.high_contrast_switch);
        backButton = view.findViewById(R.id.back_button);
        
        // Load current settings
        loadSettings();
        
        // Set up listeners
        setupListeners();
        
        return view;
    }
    
    /**
     * Load current settings from preferences
     */
    private void loadSettings() {
        // Load settings from SharedPreferences
        // For now, just set default values
        soundEnabledSwitch.setChecked(true);
        vibrationEnabledSwitch.setChecked(true);
        highContrastSwitch.setChecked(false);
    }
    
    /**
     * Set up UI event listeners
     */
    private void setupListeners() {
        // Sound switch
        soundEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save sound setting
            saveSetting("sound_enabled", isChecked);
        });
        
        // Vibration switch
        vibrationEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save vibration setting
            saveSetting("vibration_enabled", isChecked);
        });
        
        // High contrast switch
        highContrastSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save high contrast setting
            saveSetting("high_contrast", isChecked);
        });
        
        // Back button
        backButton.setOnClickListener(v -> {
            // Navigate back
            requireActivity().onBackPressed();
        });
    }
    
    /**
     * Save a setting to SharedPreferences
     */
    private void saveSetting(String key, boolean value) {
        // Get shared preferences
        android.content.SharedPreferences prefs = 
            requireActivity().getSharedPreferences("roboyard_settings", android.content.Context.MODE_PRIVATE);
        
        // Save setting
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
    
    @Override
    public String getScreenTitle() {
        return "Settings";
    }
}
