package roboyard.eclabs.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.R;
import roboyard.logic.core.Constants;
import roboyard.logic.core.Preferences;
import roboyard.ui.activities.MainActivity;
import timber.log.Timber;

/**
 * Settings screen implemented as a Fragment with native Android UI components.
 * Replaces the canvas-based SettingsScreen, but maintains the same functionality.
 */
public class SettingsFragment extends Fragment {
    
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
    private Spinner targetColorsSpinner;
    private Button backButton;
    private Spinner robotCountSpinner;
    
    private List<int[]> validBoardSizes;
    
    // Add a flag to track if this is the first selection event
    private boolean isInitialBoardSizeSelection = true;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
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
        targetColorsSpinner = view.findViewById(R.id.target_colors_spinner);
        backButton = view.findViewById(R.id.back_button);
        robotCountSpinner = view.findViewById(R.id.robot_count_spinner);
        
        // Set up board size options
        setupBoardSizeOptions();
        
        // Set up robot count spinner
        setupRobotCountSpinner();
        
        // Set up target colors spinner
        setupTargetColorsSpinner();
        
        // Load current settings
        loadSettings();
        
        // Set up listeners
        setupListeners();
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Register as preference change listener
        Preferences.setPreferenceChangeListener(new Preferences.PreferenceChangeListener() {
            @Override
            public void onPreferencesChanged() {
                Timber.d("[PREFERENCES] Preferences changed, updating UI");
                // Update UI if needed
                loadSettings();
            }
        });
    }
    
    /**
     * Set up board size options based on device screen ratio
     * This exactly replicates the calculation from the original game
     */
    private void setupBoardSizeOptions() {
        // Calculate device screen ratio
        float displayRatio = calculateDeviceRatio();
        float maxBoardRatio = calculateMaxBoardRatio(displayRatio);
        
        // Create list of valid board sizes
        validBoardSizes = new ArrayList<>();
        
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
        
        // Filter board sizes based on device ratio
        for (int[] size : boardSizes) {
            float boardRatio = (float) size[1] / size[0];
            if (boardRatio <= maxBoardRatio) {
                validBoardSizes.add(size);
            }
        }
        
        // Create adapter for spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // Add board size options to adapter
        for (int[] size : validBoardSizes) {
            adapter.add(size[0] + "x" + size[1]);
        }
        
        // Set adapter for spinner
        boardSizeSpinner.setAdapter(adapter);
        
        // Set current board size from preferences
        // If not found, use default size (16x16)
        int currentWidth = Preferences.boardSizeWidth;
        int currentHeight = Preferences.boardSizeHeight;
        
        Timber.d("[BOARD_SIZE_DEBUG] Current board size from Preferences: %dx%d", currentWidth, currentHeight);
        
        // Find matching board size in validBoardSizes
        boolean found = false;
        for (int i = 0; i < validBoardSizes.size(); i++) {
            int[] size = validBoardSizes.get(i);
            if (size[0] == currentWidth && size[1] == currentHeight) {
                boardSizeSpinner.setSelection(i);
                found = true;
                break;
            }
        }
        
        // If no matching size found, default to 16x16 or the first available size
        if (!found) {
            for (int i = 0; i < validBoardSizes.size(); i++) {
                int[] size = validBoardSizes.get(i);
                if (size[0] == 16 && size[1] == 16) {
                    boardSizeSpinner.setSelection(i);
                    found = true;
                    break;
                }
            }
            
            // If 16x16 not found, use the first size
            if (!found && !validBoardSizes.isEmpty()) {
                boardSizeSpinner.setSelection(0);
            }
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
     * Calculate the device screen ratio
     * @return The ratio of height to width
     */
    private float calculateDeviceRatio() {
        return getResources().getDisplayMetrics().heightPixels / 
                (float) getResources().getDisplayMetrics().widthPixels;
    }
    
    /**
     * Load current settings from preferences
     */
    private void loadSettings() {
        // We're now using static Preferences, so we don't need to load most values from disk
        // Just set the UI elements to match the current static Preferences values
        
        // Board size spinner - set based on current board size
        for (int i = 0; i < validBoardSizes.size(); i++) {
            int[] size = validBoardSizes.get(i);
            if (size[0] == Preferences.boardSizeWidth && 
                size[1] == Preferences.boardSizeHeight) {
                boardSizeSpinner.setSelection(i);
                break;
            }
        }
        
        // Difficulty radio buttons
        switch (Preferences.difficulty) {
            case Constants.DIFFICULTY_BEGINNER:
                difficultyRadioGroup.check(R.id.difficulty_beginner);
                break;
            case Constants.DIFFICULTY_INTERMEDIATE:
                difficultyRadioGroup.check(R.id.difficulty_advanced);
                break;
            case Constants.DIFFICULTY_INSANE:
                difficultyRadioGroup.check(R.id.difficulty_insane);
                break;
            case Constants.DIFFICULTY_IMPOSSIBLE:
                difficultyRadioGroup.check(R.id.difficulty_impossible);
                break;
        }
        
        // Sound radio buttons
        if (Preferences.soundEnabled) {
            soundRadioGroup.check(R.id.sound_on);
        } else {
            soundRadioGroup.check(R.id.sound_off);
        }
        
        // Accessibility radio buttons
        if (Preferences.accessibilityMode) {
            accessibilityRadioGroup.check(R.id.accessibility_on);
        } else {
            accessibilityRadioGroup.check(R.id.accessibility_off);
        }
        
        // Generate new map radio buttons
        if (Preferences.generateNewMap) {
            newMapRadioGroup.check(R.id.new_map_yes);
        } else {
            newMapRadioGroup.check(R.id.new_map_no);
        }
        
        // Note: The robot count and target colors spinners are set up in their respective setup methods
    }
    
    /**
     * Sets up the robot count spinner
     */
    private void setupRobotCountSpinner() {
        // We need to access the spinner directly from the class field, not try to find it again
        // This is because the view might not be fully initialized when this method is called
        if (robotCountSpinner == null) {
            Timber.e("setupRobotCountSpinner: Robot count spinner is null");
            return;
        }
        
        // Create adapter with values 1-4
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 1; i <= 4; i++) {
            adapter.add(i);
        }
        robotCountSpinner.setAdapter(adapter);
        
        // Set current value from static Preferences
        int robotCount = Preferences.robotCount;
        if (robotCount < 1 || robotCount > 4) {
            robotCount = 1; // Default to 1 if invalid
        }
        robotCountSpinner.setSelection(robotCount - 1); // -1 because index is 0-based
        Timber.d("[PREFERENCES] Using robot count: %d", robotCount);
        
        // Set listener to save changes
        robotCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedRobotCount = position + 1; // +1 because index is 0-based
                
                // Ensure robot count is never larger than target colors
                int currentTargetColors = Preferences.targetColors;
                if (selectedRobotCount > currentTargetColors) {
                    // Adjust robot count to match target colors
                    selectedRobotCount = currentTargetColors;
                    robotCountSpinner.setSelection(selectedRobotCount - 1, false);
                    
                    // Show a toast to inform the user
                    Toast.makeText(requireContext(), 
                            "Robot count cannot exceed target colors", 
                            Toast.LENGTH_SHORT).show();
                }
                
                Preferences.setRobotCount(selectedRobotCount);
                Timber.d("[PREFERENCES] Robot count set to %d", selectedRobotCount);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    /**
     * Sets up the target colors spinner
     */
    private void setupTargetColorsSpinner() {
        // We need to access the spinner directly from the class field, not try to find it again
        // This is because the view might not be fully initialized when this method is called
        if (targetColorsSpinner == null) {
            Timber.e("setupTargetColorsSpinner: Target colors spinner is null");
            return;
        }
        
        // Create adapter with values 1-4
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 1; i <= 4; i++) {
            adapter.add(i);
        }
        targetColorsSpinner.setAdapter(adapter);
        
        // Set current value from static Preferences
        int targetColors = Preferences.targetColors;
        if (targetColors < 1 || targetColors > 4) {
            targetColors = 4; // Default to 4 if invalid
        }
        targetColorsSpinner.setSelection(targetColors - 1); // -1 because index is 0-based
        Timber.d("[PREFERENCES] Using target colors: %d", targetColors);
        
        // Set listener to save changes
        targetColorsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedTargetColors = position + 1; // +1 because index is 0-based
                Preferences.setTargetColors(selectedTargetColors);
                Timber.d("[PREFERENCES] Target colors set to %d", selectedTargetColors);
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
        Preferences.setGenerateNewMap(value);
    }
    
    /**
     * Shows a toast message for small board sizes on Impossible difficulty,
     * exactly as in the original game
     */
    private void showImpossibleDifficultyToast() {
        // Get current board size
        int[] currentSize = null;
        int position = boardSizeSpinner.getSelectedItemPosition();
        if (position >= 0 && position < validBoardSizes.size()) {
            currentSize = validBoardSizes.get(position);
        }
        
        // Only show toast for small boards (less than 16x16)
        if (currentSize != null && (currentSize[0] < 16 || currentSize[1] < 16)) {
            Toast toast = Toast.makeText(requireContext(),
                    "Impossible on small boards may take several minutes to generate a fitting map",
                    Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }
    
    /**
     * Set up UI event listeners
     */
    private void setupListeners() {
        // Board size spinner
        boardSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialBoardSizeSelection) {
                    isInitialBoardSizeSelection = false;
                    return;
                }
                
                // Get selected board size
                int[] size = validBoardSizes.get(position);
                int width = size[0];
                int height = size[1];
                
                // Update static board size variables
                Context context = requireContext();
                if (context instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) context;
                    MainActivity.boardSizeX = width;
                    MainActivity.boardSizeY = height;
                    
                    // Save to preferences
                    Preferences.setBoardSize(width, height);
                    
                    Timber.d("[BOARD_SIZE_DEBUG] Settings: Board size updated to %dx%d (direct update)", width, height);
                }
                else {
                    // Save to preferences
                    Preferences.setBoardSize(width, height);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        
        // Difficulty radio group
        difficultyRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String difficulty;
            int difficultyLevel;
            if (checkedId == R.id.difficulty_beginner) {
                difficulty = "Beginner";
                difficultyLevel = Constants.DIFFICULTY_BEGINNER; 
            } else if (checkedId == R.id.difficulty_advanced) {
                difficulty = "Advanced";
                difficultyLevel = Constants.DIFFICULTY_INTERMEDIATE; 
            } else if (checkedId == R.id.difficulty_insane) {
                difficulty = "Insane";
                difficultyLevel = Constants.DIFFICULTY_INSANE; 
            } else if (checkedId == R.id.difficulty_impossible) {
                difficulty = "Impossible";
                difficultyLevel = Constants.DIFFICULTY_IMPOSSIBLE; 
                // Show warning toast for impossible difficulty
                showImpossibleDifficultyToast();
            } else {
                difficulty = "Beginner";
                difficultyLevel = Constants.DIFFICULTY_BEGINNER; 
            }
            
            // Save difficulty setting
            Preferences.setDifficulty(difficultyLevel);
            
            // Also update the numeric difficulty value in DifficultyManager
            roboyard.eclabs.util.DifficultyManager.getInstance(requireActivity()).setDifficulty(difficultyLevel);
        });
        
        // New map radio group
        newMapRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean generateNewMap = checkedId == R.id.new_map_yes;
            
            // Save new map setting
            Preferences.setGenerateNewMap(generateNewMap);
            
            // Update MapGenerator using our helper method
            setGenerateNewMapEachTimeSetting(generateNewMap);
        });
        
        // Sound radio group
        soundRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sound_on) {
                Preferences.setSoundEnabled(true);
                toggleSound(requireActivity(), true);
            } else if (checkedId == R.id.sound_off) {
                Preferences.setSoundEnabled(false);
                toggleSound(requireActivity(), false);
            }
        });
        
        // Accessibility radio group
        accessibilityRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean accessibilityEnabled = checkedId == R.id.accessibility_on;
            
            // Save accessibility mode setting
            Preferences.setAccessibilityMode(accessibilityEnabled);
        });
        
        // Back button
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }
    
    /**
     * Get the screen title for this fragment
     * @return The title to display
     */
    public String getScreenTitle() {
        return "Settings";
    }
}
