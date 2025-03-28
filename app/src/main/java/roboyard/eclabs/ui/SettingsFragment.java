package roboyard.eclabs.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.Constants;
import roboyard.eclabs.MainActivity;
import roboyard.eclabs.Preferences;
import roboyard.eclabs.R;
import timber.log.Timber;

/**
 * Settings screen implemented as a Fragment with native Android UI components.
 * Replaces the canvas-based SettingsScreen, but maintains the same functionality.
 */
public class SettingsFragment extends BaseGameFragment {
    
    private Spinner boardSizeSpinner;
    private RadioGroup difficultyRadioGroup;
    private RadioButton difficultyBeginner;
    private RadioButton difficultyAdvanced;
    private RadioButton difficultyInsane;
    private RadioButton difficultyImpossible;
    private RadioGroup newMapRadioGroup;
    private RadioButton newMapYes;
    private RadioButton newMapNo;
    private RadioGroup soundRadioGroup;
    private RadioButton soundOn;
    private RadioButton soundOff;
    private RadioGroup accessibilityRadioGroup;
    private RadioButton accessibilityOn;
    private RadioButton accessibilityOff;
    private Button backButton;
    
    private Preferences preferences;
    private List<int[]> validBoardSizes;
    
    // Add a flag to track if this is the first selection event
    private boolean isInitialBoardSizeSelection = true;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // Initialize preferences
        preferences = new Preferences();
        
        // Set up UI elements
        boardSizeSpinner = view.findViewById(R.id.board_size_spinner);
        difficultyRadioGroup = view.findViewById(R.id.difficulty_radio_group);
        difficultyBeginner = view.findViewById(R.id.difficulty_beginner);
        difficultyAdvanced = view.findViewById(R.id.difficulty_advanced);
        difficultyInsane = view.findViewById(R.id.difficulty_insane);
        difficultyImpossible = view.findViewById(R.id.difficulty_impossible);
        newMapRadioGroup = view.findViewById(R.id.new_map_radio_group);
        newMapYes = view.findViewById(R.id.new_map_yes);
        newMapNo = view.findViewById(R.id.new_map_no);
        soundRadioGroup = view.findViewById(R.id.sound_radio_group);
        soundOn = view.findViewById(R.id.sound_on);
        soundOff = view.findViewById(R.id.sound_off);
        accessibilityRadioGroup = view.findViewById(R.id.accessibility_radio_group);
        accessibilityOn = view.findViewById(R.id.accessibility_on);
        accessibilityOff = view.findViewById(R.id.accessibility_off);
        backButton = view.findViewById(R.id.back_button);
        
        // Set up board size options - this must happen first
        setupBoardSizeOptions();
        
        // Load current settings
        loadSettings();
        
        // Set up listeners
        setupListeners();
        
        return view;
    }
    
    /**
     * Set up board size options based on device screen ratio
     * This exactly replicates the calculation from the original game
     */
    private void setupBoardSizeOptions() {
        // Calculate display ratio exactly as in the original game
        float displayRatio = getResources().getDisplayMetrics().heightPixels / 
                (float) getResources().getDisplayMetrics().widthPixels;
        
        Timber.d("Settings: displayRatio: %f", displayRatio);
        
        // Define all possible board sizes - exactly as in the original game
        int[][] boardSizes = {
                {8, 8},
                {12, 12}, {12, 14}, {12, 16}, {12, 18},
                {14, 14}, {14, 16}, {14, 18},
                {16, 16}, {16, 18}, {16, 20}, {16, 22},
                {18, 18}, {18, 20}, {18, 22}
        };
        
        // Calculate max board ratio (same formula as original)
        float maxBoardRatio = calculateMaxBoardRatio(displayRatio);
        Timber.d("Settings: Display ratio: %.2f -> Max board ratio: %.2f", displayRatio, maxBoardRatio);
        
        // Create list of valid board size options
        List<String> boardSizeOptions = new ArrayList<>();
        validBoardSizes = new ArrayList<>();
        
        // IMPORTANT FIX: Load board size directly from preferences instead of using MainActivity static variables
        // This ensures we get the correct values even if MainActivity static variables haven't been initialized yet
        String boardSizeXStr = preferences.getPreferenceValue(requireActivity(), "boardSizeX");
        String boardSizeYStr = preferences.getPreferenceValue(requireActivity(), "boardSizeY");
        
        // Default to MainActivity's static values if preferences don't exist
        int currentBoardSizeX = MainActivity.getBoardWidth();
        int currentBoardSizeY = MainActivity.getBoardHeight();
        
        // Override with values from preferences if they exist
        if (boardSizeXStr != null && !boardSizeXStr.isEmpty()) {
            try {
                int prefWidth = Integer.parseInt(boardSizeXStr);
                currentBoardSizeX = prefWidth;
                Timber.d("[BOARD_SIZE_DEBUG] Settings: Loaded board width from preferences: %d", currentBoardSizeX);
            } catch (NumberFormatException e) {
                Timber.e(e, "[BOARD_SIZE_DEBUG] Settings: Error parsing board width from preferences");
            }
        } else {
            Timber.d("[BOARD_SIZE_DEBUG] Settings: No board width in preferences, using MainActivity value: %d", currentBoardSizeX);
        }
        
        if (boardSizeYStr != null && !boardSizeYStr.isEmpty()) {
            try {
                int prefHeight = Integer.parseInt(boardSizeYStr);
                currentBoardSizeY = prefHeight;
                Timber.d("[BOARD_SIZE_DEBUG] Settings: Loaded board height from preferences: %d", currentBoardSizeY);
            } catch (NumberFormatException e) {
                Timber.e(e, "[BOARD_SIZE_DEBUG] Settings: Error parsing board height from preferences");
            }
        } else {
            Timber.d("[BOARD_SIZE_DEBUG] Settings: No board height in preferences, using MainActivity value: %d", currentBoardSizeY);
        }
        
        Timber.d("[BOARD_SIZE_DEBUG] Settings: Using board size for spinner selection: %dx%d", currentBoardSizeX, currentBoardSizeY);
        
        int selectedIndex = -1;
        int index = 0;
        
        for (int[] size : boardSizes) {
            float boardRatio = (float) size[1] / size[0];
            Timber.d("Settings: Checking board size %dx%d (ratio: %.2f)", size[0], size[1], boardRatio);
            
            if (boardRatio <= maxBoardRatio) {
                String option = size[0] + "x" + size[1];
                boardSizeOptions.add(option);
                validBoardSizes.add(size);
                Timber.d("Settings: Added board size option: %s", option);
                
                // Check if this is the current board size
                if (size[0] == currentBoardSizeX && size[1] == currentBoardSizeY) {
                    selectedIndex = index;
                    Timber.d("Settings: Current board size found at index: %d", selectedIndex);
                }
                index++;
            }
        }
        
        // Create adapter for spinner
        if (boardSizeOptions.isEmpty()) {
            // Fallback if no valid sizes were found
            boardSizeOptions.add(MainActivity.DEFAULT_BOARD_SIZE_X + "x" + MainActivity.DEFAULT_BOARD_SIZE_Y);
            int[] defaultSize = {MainActivity.DEFAULT_BOARD_SIZE_X, MainActivity.DEFAULT_BOARD_SIZE_Y};
            validBoardSizes.add(defaultSize);
            selectedIndex = 0;
            Timber.d("Settings: No valid board sizes found, using default: %dx%d", 
                    MainActivity.DEFAULT_BOARD_SIZE_X, MainActivity.DEFAULT_BOARD_SIZE_Y);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, boardSizeOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        boardSizeSpinner.setAdapter(adapter);
        
        // Set current selection
        if (selectedIndex >= 0 && selectedIndex < boardSizeOptions.size()) {
            boardSizeSpinner.setSelection(selectedIndex);
            Timber.d("Settings: Selected board size at index: %d", selectedIndex);
        }
    }
    
    /**
     * Calculate maximum allowed board ratio based on display ratio.
     * Same formula as in the original SettingsGameScreen.
     */
    private float calculateMaxBoardRatio(float displayRatio) {
        // For display ratio 1.5 -> max board ratio 1.2
        // For display ratio 2.0 -> max board ratio 1.8
        // Linear interpolation between these points
        if (displayRatio <= 1.5f) {
            return 1.2f;
        } else if (displayRatio >= 2.0f) {
            return 1.8f;
        } else {
            // Linear interpolation
            float factor = (displayRatio - 1.5f) / 0.5f; // 0.0 to 1.0
            return 1.2f + factor * 0.6f;
        }
    }
    
    /**
     * Load current settings from preferences
     */
    private void loadSettings() {
        // Load difficulty setting
        String difficulty = preferences.getPreferenceValue(requireActivity(), "difficulty");
        if (difficulty == null || difficulty.isEmpty()) {
            difficulty = "Beginner"; // Default
            preferences.setPreferences(requireActivity(), "difficulty", difficulty);
        }
        
        // Set difficulty radio button
        int difficultyLevel; // Default to DIFFICULTY_BEGINNER
        switch (difficulty) {
            case "Advanced":
                difficultyAdvanced.setChecked(true);
                difficultyLevel = Constants.DIFFICULTY_INTERMEDIATE;
                break;
            case "Insane":
                difficultyInsane.setChecked(true);
                difficultyLevel = Constants.DIFFICULTY_INSANE;
                break;
            case "Impossible":
                difficultyImpossible.setChecked(true);
                difficultyLevel = Constants.DIFFICULTY_IMPOSSIBLE;
                break;
            default:
                difficultyBeginner.setChecked(true);
                difficultyLevel = Constants.DIFFICULTY_BEGINNER;
                break;
        }
        
        // Important: Update DifficultyManager with the numeric difficulty level
        roboyard.eclabs.util.DifficultyManager.getInstance(requireActivity()).setDifficulty(difficultyLevel);
        Timber.d("[DIFFICULTY] Loaded difficulty: %s (level %d)", difficulty, difficultyLevel);
        
        // Load new map setting
        String newMapEachTime = preferences.getPreferenceValue(requireActivity(), "newMapEachTime");
        if (newMapEachTime == null || newMapEachTime.isEmpty()) {
            newMapEachTime = "true"; // Default
            preferences.setPreferences(requireActivity(), "newMapEachTime", newMapEachTime);
        }
        
        // Set new map radio button
        if (newMapEachTime.equals("true")) {
            newMapYes.setChecked(true);
            // Update MapGenerator via helper method
            setGenerateNewMapEachTimeSetting(true);
        } else {
            newMapNo.setChecked(true);
            // Update MapGenerator via helper method
            setGenerateNewMapEachTimeSetting(false);
        }
        
        // Load sound setting
        String sound = preferences.getPreferenceValue(requireActivity(), "sound");
        if (sound == null || sound.isEmpty()) {
            sound = "on"; // Default
            preferences.setPreferences(requireActivity(), "sound", sound);
        }
        
        // Set sound radio button
        if (sound.equals("on")) {
            soundOn.setChecked(true);
            toggleSound(requireActivity(), true);
        } else {
            soundOff.setChecked(true);
            toggleSound(requireActivity(), false);
        }
        
        // Set accessibility mode radio group
        String accessibilityPref = preferences.getPreferenceValue(requireActivity(), "accessibilityMode");
        if (accessibilityPref != null && accessibilityPref.equals("true")) {
            accessibilityRadioGroup.check(R.id.accessibility_on);
        } else {
            accessibilityRadioGroup.check(R.id.accessibility_off);
        }
    }
    
    /**
     * Helper method to toggle sound safely from any Activity
     */
    private void toggleSound(Activity activity, boolean enabled) {
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).toggleSound(enabled);
        }
    }
    
    /**
     * Helper method to set MapGenerator.generateNewMapEachTime via reflection
     * This is necessary because the field is package-private
     */
    private void setGenerateNewMapEachTimeSetting(boolean value) {
        // Since we can't directly access the field, use GridGameScreen helper method
        // which has package access to MapGenerator
        preferences.setPreferences(requireActivity(), "newMapEachTime", String.valueOf(value));
    }
    
    /**
     * Shows a toast message for small board sizes on Impossible difficulty,
     * exactly as in the original game
     */
    private void showImpossibleDifficultyToast() {
        String message = "Level Impossible will generate a fitting puzzle. This can take a while. In case the solver gets stuck, press >>";
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        // Repeat the toast three times like in the original
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Set up UI event listeners
     */
    private void setupListeners() {
        // Board size spinner
        boardSizeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= validBoardSizes.size()) {
                    return;
                }
                
                // Skip the first automatic selection event when the spinner is initialized
                if (isInitialBoardSizeSelection) {
                    Timber.d("[BOARD_SIZE_DEBUG] Settings: Ignoring initial board size selection event");
                    isInitialBoardSizeSelection = false;
                    return;
                }
                
                int[] selectedSize = validBoardSizes.get(position);
                int width = selectedSize[0];
                int height = selectedSize[1];
                
                Timber.d("[BOARD_SIZE_DEBUG] Settings: User selected board size: %dx%d", width, height);
                
                // Get the context to pass to setAndSaveBoardSizeToPreferences
                Context context = requireContext();
                
                // Get reference to MainActivity or update static values directly
                if (context instanceof MainActivity mainActivity) {
                    // For legacy UI, use the MainActivity instance
                    mainActivity.setAndSaveBoardSizeToPreferences(context, width, height);
                } else {
                    // For modern UI, update the static values directly
                    MainActivity.boardSizeX = width;
                    MainActivity.boardSizeY = height;
                    
                    // Save to preferences directly
                    preferences.setPreferences(requireActivity(), "boardSizeX", String.valueOf(width));
                    preferences.setPreferences(requireActivity(), "boardSizeY", String.valueOf(height));
                    
                    Timber.d("[BOARD_SIZE_DEBUG] Settings: Board size updated to %dx%d (direct update)", width, height);
                }

                preferences.setPreferences(requireActivity(), "boardSizeX", String.valueOf(width));
                preferences.setPreferences(requireActivity(), "boardSizeY", String.valueOf(height));
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });
        
        // Difficulty radio group
        difficultyRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String difficulty;
            int difficultyLevel;
            if (checkedId == R.id.difficulty_beginner) {
                difficulty = "Beginner";
                difficultyLevel = 0; // DIFFICULTY_BEGINNER
            } else if (checkedId == R.id.difficulty_advanced) {
                difficulty = "Advanced";
                difficultyLevel = 1; // DIFFICULTY_INTERMEDIATE
            } else if (checkedId == R.id.difficulty_insane) {
                difficulty = "Insane";
                difficultyLevel = 2; // DIFFICULTY_INSANE
            } else if (checkedId == R.id.difficulty_impossible) {
                difficulty = "Impossible";
                difficultyLevel = 3; // DIFFICULTY_IMPOSSIBLE
                // Show warning toast for impossible difficulty
                showImpossibleDifficultyToast();
            } else {
                difficulty = "Beginner";
                difficultyLevel = 0; // DIFFICULTY_BEGINNER
            }
            
            // Save difficulty setting as a string in preferences
            preferences.setPreferences(requireActivity(), "difficulty", difficulty);
            
            // Also update the numeric difficulty value in DifficultyManager
            roboyard.eclabs.util.DifficultyManager.getInstance(requireActivity()).setDifficulty(difficultyLevel);
            Timber.d("[DIFFICULTY] Set difficulty to: %s (level %d)", difficulty, difficultyLevel);
        });
        
        // New map radio group
        newMapRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean generateNewMap = checkedId == R.id.new_map_yes;
            String newMapEachTime = generateNewMap ? "true" : "false";
            
            // Save new map setting
            preferences.setPreferences(requireActivity(), "newMapEachTime", newMapEachTime);
            
            // Update MapGenerator using our helper method
            setGenerateNewMapEachTimeSetting(generateNewMap);
        });
        
        // Sound radio group
        soundRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean soundEnabled = checkedId == R.id.sound_on;
            String sound = soundEnabled ? "on" : "off";
            
            // Save sound setting
            preferences.setPreferences(requireActivity(), "sound", sound);
            
            // Toggle sound state
            toggleSound(requireActivity(), soundEnabled);
        });
        
        // Accessibility mode radio group
        accessibilityRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean accessibilityEnabled = checkedId == R.id.accessibility_on;
            String accessibilityMode = accessibilityEnabled ? "true" : "false";
            
            // Save accessibility mode setting
            preferences.setPreferences(requireActivity(), "accessibilityMode", accessibilityMode);
        });
        
        // Back button
        backButton.setOnClickListener(v -> {
            // Simply use the activity's onBackPressed method for safe navigation
            requireActivity().onBackPressed();
        });
    }
    
    @Override
    public String getScreenTitle() {
        return "Settings";
    }
}
