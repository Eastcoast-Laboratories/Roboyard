package roboyard.eclabs.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import roboyard.logic.core.Constants;
import roboyard.ui.activities.MainActivity;
import roboyard.eclabs.Preferences;
import roboyard.eclabs.R;
import roboyard.eclabs.AppPreferences;
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
    private Spinner targetCountSpinner;
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
        
        // Initialize UI components
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
        targetCountSpinner = view.findViewById(R.id.target_count_spinner);
        backButton = view.findViewById(R.id.back_button);
        
        // Set up board size options - this must happen first
        setupBoardSizeOptions();
        
        // Load current settings
        loadSettings();
        
        // Set up listeners
        setupListeners();
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set up target count spinner after view is created
        setupTargetCountSpinner();
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
                {8, 12}, 
                {10, 10}, // board ratio: 1.0
                {10, 12}, // board ratio: 1.2
                {10, 14}, // board ratio: 1.4
                {12, 12}, // board ratio: 1.0
                {12, 14}, {12, 16}, {12, 18},
                {14, 14}, // board ratio: 1.0
                {14, 16}, {14, 18},
                {16, 16}, // board ratio: 1.0
                {16, 18}, {16, 20}, {16, 22},
                {18, 18}, // board ratio: 1.0
                {18, 20}, {18, 22}
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
        if (difficulty != null) {
            switch (difficulty) {
                case "Beginner":
                    difficultyRadioGroup.check(R.id.difficulty_beginner);
                    break;
                case "Advanced":
                    difficultyRadioGroup.check(R.id.difficulty_advanced);
                    break;
                case "Insane":
                    difficultyRadioGroup.check(R.id.difficulty_insane);
                    break;
                case "Impossible":
                    difficultyRadioGroup.check(R.id.difficulty_impossible);
                    break;
            }
        }
        
        // Load sound setting
        String sound = preferences.getPreferenceValue(requireActivity(), "sound");
        if (sound != null) {
            if (sound.equalsIgnoreCase("true")) {
                soundRadioGroup.check(R.id.sound_on);
            } else {
                soundRadioGroup.check(R.id.sound_off);
            }
        }
        
        // Load accessibility setting
        String accessibility = preferences.getPreferenceValue(requireActivity(), "accessibility");
        if (accessibility != null) {
            if (accessibility.equalsIgnoreCase("true")) {
                accessibilityRadioGroup.check(R.id.accessibility_on);
            } else {
                accessibilityRadioGroup.check(R.id.accessibility_off);
            }
        }
        
        // Load new map setting
        String newMapSetting = preferences.getPreferenceValue(requireActivity(), "generate_new_map");
        if (newMapSetting != null) {
            if (newMapSetting.equalsIgnoreCase("true")) {
                newMapRadioGroup.check(R.id.new_map_yes);
            } else {
                newMapRadioGroup.check(R.id.new_map_no);
            }
        }
        
        // Load target count setting
        String targetCount = preferences.getPreferenceValue(requireActivity(), "target_count");
        if (targetCount != null) {
            try {
                int count = Integer.parseInt(targetCount);
                // Adjust for 0-based index of spinner
                int spinnerPosition = count - 1;
                if (spinnerPosition >= 0 && spinnerPosition < targetCountSpinner.getCount()) {
                    targetCountSpinner.setSelection(spinnerPosition);
                }
            } catch (NumberFormatException e) {
                Timber.e(e, "Error parsing target count from preferences");
            }
        }
    }
    
    /**
     * Sets up the target count spinner
     */
    private void setupTargetCountSpinner() {
        View view = getView();
        if (view == null) {
            Timber.e("setupTargetCountSpinner: View is null");
            return;
        }
        
        // Create spinner for target count selection (1-4)
        Spinner targetCountSpinner = view.findViewById(R.id.target_count_spinner);
        if (targetCountSpinner == null) {
            Timber.e("setupTargetCountSpinner: Target count spinner not found");
            return;
        }
        
        // Create adapter with values 1-4
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 1; i <= 4; i++) {
            adapter.add(i);
        }
        targetCountSpinner.setAdapter(adapter);
        
        // Set current value from preferences
        int currentTargetCount = 1; // Default to 1
        try {
            currentTargetCount = AppPreferences.getInstance().getTargetCount();
            Timber.d("[TARGET COUNT] Using target count from AppPreferences: %d", currentTargetCount);
        } catch (IllegalStateException e) {
            // Fall back to old Preferences if AppPreferences is not initialized
            Timber.w(e, "AppPreferences not initialized, falling back to old Preferences");
            Preferences preferences = new Preferences();
            String targetCountStr = preferences.getPreferenceValue(requireActivity(), "target_count");
            if (targetCountStr != null && !targetCountStr.isEmpty()) {
                try {
                    currentTargetCount = Integer.parseInt(targetCountStr);
                    // Ensure value is within valid range
                    currentTargetCount = Math.max(1, Math.min(4, currentTargetCount));
                } catch (NumberFormatException nfe) {
                    Timber.e(nfe, "Error parsing target count from preferences");
                }
            }
            Timber.d("[TARGET COUNT] Using target count from old Preferences: %d", currentTargetCount);
        }
        
        targetCountSpinner.setSelection(currentTargetCount - 1); // -1 because index is 0-based
        
        // Set listener to save changes
        targetCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedTargetCount = position + 1; // +1 because index is 0-based
                try {
                    AppPreferences.getInstance().setTargetCount(selectedTargetCount);
                    Timber.d("[TARGET COUNT] Target count set to %d using AppPreferences", selectedTargetCount);
                } catch (IllegalStateException e) {
                    // Fall back to old Preferences if AppPreferences is not initialized
                    Timber.w(e, "AppPreferences not initialized, falling back to old Preferences");
                    Preferences preferences = new Preferences();
                    preferences.setPreferences(requireActivity(), "target_count", String.valueOf(selectedTargetCount));
                    Timber.d("[TARGET COUNT] Target count set to %d using old Preferences", selectedTargetCount);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
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
        // Since we can't directly access the field, use GridGameView helper method
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
            if (checkedId == R.id.sound_on) {
                preferences.setPreferences(requireActivity(), "sound", "true");
                toggleSound(requireActivity(), true);
            } else if (checkedId == R.id.sound_off) {
                preferences.setPreferences(requireActivity(), "sound", "false");
                toggleSound(requireActivity(), false);
            }
        });
        
        // Target count spinner
        targetCountSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Add 1 to position because spinner is 0-indexed, but we want to store 1-4
                int targetCount = position + 1;
                Timber.d("Target count selected: %d", targetCount);
                preferences.setPreferences(requireActivity(), "target_count", String.valueOf(targetCount));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
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
