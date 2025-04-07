package roboyard.eclabs.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import roboyard.eclabs.R;
import roboyard.logic.core.Constants;
import roboyard.logic.core.Preferences;
import roboyard.logic.core.WallStorage;
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
    
    // Flag to prevent recursive updates
    private boolean isUpdatingUI = false;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        // Create a safe layout inflater that won't crash on resource errors
        LayoutInflater safeInflater = inflater.cloneInContext(requireContext());
        
        // Set a factory that catches and handles resource inflation errors
        safeInflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                try {
                    // First, let the normal inflation process try
                    if (name.startsWith("TextView") || 
                        name.equals("androidx.appcompat.widget.AppCompatTextView")) {
                        // For TextViews, we intercept and handle dimension resource errors
                        TextView textView = new TextView(context, attrs);
                        for (int i = 0; i < attrs.getAttributeCount(); i++) {
                            String attrName = attrs.getAttributeName(i);
                            String attrValue = attrs.getAttributeValue(i);
                            
                            // If this is a textSize attribute and it's using a dimension resource
                            if ("textSize".equals(attrName) && attrValue != null && 
                                attrValue.startsWith("@dimen")) {
                                try {
                                    // Try to get the dimension
                                    int resourceId = attrs.getAttributeResourceValue(i, 0);
                                    if (resourceId != 0) {
                                        context.getResources().getDimensionPixelSize(resourceId);
                                    } else {
                                        // Resource not found, use a default text size of 16sp
                                        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                                        Timber.w("Missing dimension resource: %s, using default", attrValue);
                                    }
                                } catch (Exception e) {
                                    // Dimension resource exists but something went wrong
                                    Timber.e(e, "Error setting text size from dimension resource");
                                    // Apply a fallback size
                                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                                }
                            }
                        }
                        return textView;
                    }
                    
                    // For non-TextView elements, delegate to the default factory
                    return null;
                } catch (Exception e) {
                    Timber.e(e, "Error in custom view factory");
                    return null;
                }
            }
            
            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                return onCreateView(null, name, context, attrs);
            }
        });
        
        try {
            View view = safeInflater.inflate(R.layout.fragment_settings, container, false);
            
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
        } catch (Exception e) {
            Timber.e(e, "Failed to inflate settings layout - creating fallback layout");
            
            // Create a fallback layout if the XML inflation fails completely
            LinearLayout fallbackLayout = new LinearLayout(requireContext());
            fallbackLayout.setOrientation(LinearLayout.VERTICAL);
            fallbackLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.MATCH_PARENT));
            fallbackLayout.setPadding(16, 16, 16, 16);
            
            // Add a message explaining the issue
            TextView errorMessage = new TextView(requireContext());
            errorMessage.setText("Error loading settings screen. Please try again later.");
            errorMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            errorMessage.setGravity(Gravity.CENTER);
            errorMessage.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            fallbackLayout.addView(errorMessage);
            
            // Add a back button
            Button backBtn = new Button(requireContext());
            backBtn.setText("Go Back");
            backBtn.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonParams.gravity = Gravity.CENTER;
            buttonParams.topMargin = 32;
            backBtn.setLayoutParams(buttonParams);
            fallbackLayout.addView(backBtn);
            
            return fallbackLayout;
        }
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
        try {
            if (boardSizeSpinner == null) {
                Timber.e("setupBoardSizeOptions: Board size spinner is null");
                return;
            }
            
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
            if (boardSizeSpinner != null) {
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
            } else {
                Timber.e("boardSizeSpinner is null after adapter creation");
            }
        } catch (Exception e) {
            Timber.e(e, "Error setting up board size options");
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
        float ratio = 1.778f; // Default to 16:9 if we can't determine
        
        try {
            Context context = getContext();
            if (context == null) {
                Timber.e("calculateDeviceRatio: Context is null");
                return ratio;
            }
            
            // Get window manager service
            android.view.WindowManager windowManager = (android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager == null) {
                Timber.e("calculateDeviceRatio: WindowManager is null");
                return ratio;
            }
            
            // Check if we can use the new WindowMetrics API (Android R and above)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    android.graphics.Rect bounds = windowManager.getCurrentWindowMetrics().getBounds();
                    Timber.d("Using WindowMetrics API for device ratio calculation");
                    return (float) bounds.height() / bounds.width();
                } catch (Exception e) {
                    Timber.e(e, "Error using WindowMetrics API, falling back to DisplayMetrics");
                    // Fall through to DisplayMetrics approach
                }
            }
            
            // For Android Q and below, use DisplayMetrics
            try {
                android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
                android.view.Display display = windowManager.getDefaultDisplay();
                if (display != null) {
                    display.getMetrics(displayMetrics);
                    int displayHeight = displayMetrics.heightPixels;
                    int displayWidth = displayMetrics.widthPixels;
                    
                    if (displayWidth > 0) { // Avoid division by zero
                        ratio = (float) displayHeight / displayWidth;
                    }
                    
                    Timber.d("Using DisplayMetrics API for device ratio calculation: %f", ratio);
                } else {
                    Timber.e("calculateDeviceRatio: Display is null");
                }
            } catch (Exception e) {
                Timber.e(e, "Error using DisplayMetrics, returning default ratio");
            }
        } catch (Exception e) {
            Timber.e(e, "Unexpected error in calculateDeviceRatio");
        }
        
        return ratio;
    }
    
    /**
     * Load current settings from preferences
     */
    private void loadSettings() {
        try {
            isUpdatingUI = true;
            
            // Get current values from static Preferences
            int difficulty = Preferences.difficulty;
            boolean generateNewMapEachTime = Preferences.generateNewMapEachTime;
            boolean soundEnabled = Preferences.soundEnabled;
            boolean accessibilityMode = Preferences.accessibilityMode;
            
            // Set difficulty radio buttons
            if (difficultyRadioGroup != null) {
                switch (difficulty) {
                    case Constants.DIFFICULTY_BEGINNER:
                        if (difficultyBeginner != null) difficultyBeginner.setChecked(true);
                        break;
                    case Constants.DIFFICULTY_ADVANCED:
                        if (difficultyAdvanced != null) difficultyAdvanced.setChecked(true);
                        break;
                    case Constants.DIFFICULTY_INSANE:
                        if (difficultyInsane != null) difficultyInsane.setChecked(true);
                        break;
                    case Constants.DIFFICULTY_IMPOSSIBLE:
                        if (difficultyImpossible != null) difficultyImpossible.setChecked(true);
                        break;
                }
            } else {
                Timber.e("difficultyRadioGroup is null");
            }
            
            // Set new map radio buttons
            if (newMapRadioGroup != null) {
                if (generateNewMapEachTime) {
                    if (newMapYes != null) newMapYes.setChecked(true);
                } else {
                    if (newMapNo != null) newMapNo.setChecked(true);
                }
            } else {
                Timber.e("newMapRadioGroup is null");
            }
            
            // Set sound radio buttons
            if (soundRadioGroup != null) {
                if (soundEnabled) {
                    if (soundOn != null) soundOn.setChecked(true);
                } else {
                    if (soundOff != null) soundOff.setChecked(true);
                }
            } else {
                Timber.e("soundRadioGroup is null");
            }
            
            // Set accessibility radio buttons
            if (accessibilityRadioGroup != null) {
                if (accessibilityMode) {
                    if (accessibilityOn != null) accessibilityOn.setChecked(true);
                } else {
                    if (accessibilityOff != null) accessibilityOff.setChecked(true);
                }
            } else {
                Timber.e("accessibilityRadioGroup is null");
            }
            
            isUpdatingUI = false;
        } catch (Exception e) {
            Timber.e(e, "Error loading settings");
            isUpdatingUI = false; // Make sure to reset this flag even if an error occurs
        }
    }
    
    /**
     * Sets up the robot count spinner
     */
    private void setupRobotCountSpinner() {
        try {
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
                robotCount = Preferences.DEFAULT_ROBOT_COUNT; // Default to 1 if invalid
            }
            robotCountSpinner.setSelection(robotCount - 1); // -1 because index is 0-based
            Timber.d("[PREFERENCES] Using robot count: %d", robotCount);
            
            // Set listener to save changes
            robotCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
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
                    } catch (Exception e) {
                        Timber.e(e, "Error processing robot count selection");
                    }
                }
                
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });
        } catch (Exception e) {
            Timber.e(e, "Error setting up robot count spinner");
        }
    }
    
    /**
     * Sets up the target colors spinner
     */
    private void setupTargetColorsSpinner() {
        try {
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
                targetColors = Preferences.DEFAULT_TARGET_COLORS; // Default to 4 if invalid
            }
            targetColorsSpinner.setSelection(targetColors - 1); // -1 because index is 0-based
            Timber.d("[PREFERENCES] Using target colors: %d", targetColors);
            
            // Set listener to save changes
            targetColorsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        int selectedTargetColors = position + 1; // +1 because index is 0-based
                        int currentRobotCount = Preferences.robotCount;
                        
                        // If the selected target colors is less than the current robot count, adjust
                        if (selectedTargetColors < currentRobotCount) {
                            // Show a toast to inform the user
                            Toast.makeText(requireContext(), 
                                    "Reducing robot count to match target colors", 
                                    Toast.LENGTH_SHORT).show();
                            
                            // Update the robot count
                            Preferences.setRobotCount(selectedTargetColors);
                            
                            // Update the robot count spinner if available
                            if (robotCountSpinner != null) {
                                robotCountSpinner.setSelection(selectedTargetColors - 1);
                            }
                        }
                        
                        Preferences.setTargetColors(selectedTargetColors);
                        Timber.d("[PREFERENCES] Target colors set to %d", selectedTargetColors);
                    } catch (Exception e) {
                        Timber.e(e, "Error processing target colors selection");
                    }
                }
                
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });
        } catch (Exception e) {
            Timber.e(e, "Error setting up target colors spinner");
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
        try {
            // Update the preference
            Preferences.setGenerateNewMapEachTime(value);
            
            // If the value is true ("No" for preserving walls), clear the wall storage for current board size
            if (value) {
                Timber.d("[WALL STORAGE] Setting 'new map each time' to true, clearing wall storage for current board size: %dx%d", 
                        Preferences.boardSizeWidth, Preferences.boardSizeHeight);
                
                try {
                    WallStorage wallStorage = WallStorage.getInstance();
                    if (wallStorage != null) {
                        wallStorage.updateCurrentBoardSize();
                        wallStorage.clearStoredWalls();
                    } else {
                        Timber.e("WallStorage instance is null");
                    }
                } catch (Exception e) {
                    Timber.e(e, "Error clearing wall storage");
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Error setting generateNewMapEachTime setting");
            Toast.makeText(requireContext(), "Failed to update map generation setting", Toast.LENGTH_SHORT).show();
        }
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
     * Shows a message about enabling TalkBack in Android settings
     * with a button to open the accessibility settings
     */
    private void showTalkBackMessage() {
        // Create an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enable TalkBack");
        builder.setMessage("To enable the accessibility mode, you have to enable TalkBack in your device settings.");
        
        // Add a button to open Android accessibility settings
        builder.setPositiveButton("Open Settings", (dialog, which) -> {
            try {
                // Open Android's accessibility settings
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                // Handle any exceptions that might occur on newer Android versions
                Timber.e(e, "Failed to open accessibility settings");
                Toast.makeText(requireContext(), 
                        "Could not open accessibility settings. Please open them manually.", 
                        Toast.LENGTH_LONG).show();
            }
        });
        
        // Add a cancel button
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        // Show the dialog
        builder.create().show();
    }
    
    /**
     * Set up UI event listeners
     */
    private void setupListeners() {
        try {
            // Board size spinner
            if (boardSizeSpinner != null) {
                boardSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        try {
                            if (isInitialBoardSizeSelection) {
                                isInitialBoardSizeSelection = false;
                                return;
                            }
                            
                            // Skip if we're already updating UI from preferences
                            if (isUpdatingUI) {
                                return;
                            }
                            
                            // Get selected board size
                            if (position >= 0 && position < validBoardSizes.size()) {
                                int[] size = validBoardSizes.get(position);
                                int width = size[0];
                                int height = size[1];
                                
                                // Update static board size variables through the Preferences manager
                                Preferences.setBoardSize(width, height);
                                Timber.d("[BOARD_SIZE_DEBUG] Settings: Board size updated to %dx%d", width, height);
                            } else {
                                Timber.e("Invalid board size position: %d", position);
                            }
                        } catch (Exception e) {
                            Timber.e(e, "Error processing board size selection");
                        }
                    }
                    
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Do nothing
                    }
                });
            } else {
                Timber.e("boardSizeSpinner is null");
            }
            
            // Difficulty radio group
            if (difficultyRadioGroup != null) {
                difficultyRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    try {
                        // Skip if we're already updating UI from preferences
                        if (isUpdatingUI) {
                            return;
                        }
                        
                        String difficulty = "Beginner"; // default
                        int difficultyLevel = Constants.DIFFICULTY_BEGINNER; // default

                        if (checkedId == R.id.difficulty_beginner) {
                            // Only set board size to 12x14 if current size is larger
                            if (Preferences.boardSizeWidth * Preferences.boardSizeHeight > 12 * 14) {
                                // Find the 12x14 board size in the validBoardSizes list
                                int position = -1;
                                for (int i = 0; i < validBoardSizes.size(); i++) {
                                    int[] size = validBoardSizes.get(i);
                                    if (size[0] == 12 && size[1] == 14) {
                                        position = i;
                                        break;
                                    }
                                }
                                
                                // If 12x14 is a valid board size, select it
                                if (position >= 0 && boardSizeSpinner != null) {
                                    isUpdatingUI = true; // Prevent recursive updates
                                    boardSizeSpinner.setSelection(position);
                                    // Update the board size in preferences
                                    Preferences.setBoardSize(12, 14);
                                    isUpdatingUI = false;
                                    Timber.d("Automatically set board size to 12x14 for Beginner difficulty");
                                } else {
                                    Timber.d("12x14 board size not found for Beginner difficulty");
                                }
                            }
                            
                            // Set target colors to 1 for Beginner difficulty
                            if (targetColorsSpinner != null) {
                                isUpdatingUI = true; // Prevent recursive updates
                                targetColorsSpinner.setSelection(0); // 0 = 1 color (index is 0-based)
                                Preferences.setTargetColors(1);
                                isUpdatingUI = false;
                                Timber.d("Automatically set target colors to 1 for Beginner difficulty");
                            }
                            
                        } else if (checkedId == R.id.difficulty_advanced) {
                            difficulty = "Advanced";
                            difficultyLevel = Constants.DIFFICULTY_ADVANCED;
                        } else if (checkedId == R.id.difficulty_insane) {
                            difficulty = "Insane";
                            difficultyLevel = Constants.DIFFICULTY_INSANE; 
                        } else if (checkedId == R.id.difficulty_impossible) {
                            difficulty = "Impossible";
                            difficultyLevel = Constants.DIFFICULTY_IMPOSSIBLE; 
                            // Show warning toast for impossible difficulty
                            showImpossibleDifficultyToast();
                        }
                        
                        // Save difficulty setting
                        Preferences.setDifficulty(difficultyLevel);
                    } catch (Exception e) {
                        Timber.e(e, "Error processing difficulty selection");
                    }
                });
            } else {
                Timber.e("difficultyRadioGroup is null");
            }
            
            // New map radio group
            if (newMapRadioGroup != null) {
                newMapRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    try {
                        boolean generateNewMapEachTime = checkedId == R.id.new_map_yes;
                        
                        // Save new map setting
                        Preferences.setGenerateNewMapEachTime(generateNewMapEachTime);
                        
                        // Update MapGenerator using our helper method
                        setGenerateNewMapEachTimeSetting(generateNewMapEachTime);
                    } catch (Exception e) {
                        Timber.e(e, "Error processing new map selection");
                    }
                });
            } else {
                Timber.e("newMapRadioGroup is null");
            }
            
            // Sound radio group
            if (soundRadioGroup != null) {
                soundRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    try {
                        if (checkedId == R.id.sound_on) {
                            Preferences.setSoundEnabled(true);
                            toggleSound(requireActivity(), true);
                        } else if (checkedId == R.id.sound_off) {
                            Preferences.setSoundEnabled(false);
                            toggleSound(requireActivity(), false);
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Error processing sound selection");
                    }
                });
            } else {
                Timber.e("soundRadioGroup is null");
            }
            
            // Accessibility radio group
            if (accessibilityRadioGroup != null) {
                accessibilityRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    try {
                        boolean accessibilityEnabled = checkedId == R.id.accessibility_on;
                        
                        // Save accessibility mode setting
                        Preferences.setAccessibilityMode(accessibilityEnabled);
                        
                        // If accessibility mode is enabled, automatically set recommended settings
                        if (accessibilityEnabled) {
                            // Set board size to 8x8
                            Preferences.setBoardSize(8, 8);
                            for (int i = 0; i < validBoardSizes.size(); i++) {
                                int[] size = validBoardSizes.get(i);
                                if (size[0] == 8 && size[1] == 8) {
                                    boardSizeSpinner.setSelection(i);
                                    break;
                                }
                            }
                            
                            // Set difficulty to Beginner
                            Preferences.setDifficulty(Constants.DIFFICULTY_BEGINNER);
                            difficultyRadioGroup.check(R.id.difficulty_beginner);
                            
                            // Set "New Map Each Time" to "No" (preserve walls)
                            Preferences.setGenerateNewMapEachTime(false);
                            newMapRadioGroup.check(R.id.new_map_no);
                            
                            // Set target colors to 1
                            Preferences.setTargetColors(1);
                            targetColorsSpinner.setSelection(0); // 0 = 1 color (index is 0-based)
                            
                            // Show message about TalkBack
                            showTalkBackMessage();
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Error processing accessibility selection");
                    }
                });
            } else {
                Timber.e("accessibilityRadioGroup is null");
            }
            
            // Back button
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    try {
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        } else {
                            Timber.e("Activity is null when back button pressed");
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Error handling back button");
                        // Try fallback navigation if available
                        try {
                            requireActivity().onBackPressed();
                        } catch (Exception ex) {
                            Timber.e(ex, "Fallback navigation also failed");
                        }
                    }
                });
            } else {
                Timber.e("backButton is null");
            }
        } catch (Exception e) {
            Timber.e(e, "Error setting up listeners");
        }
    }
    
    /**
     * Get the screen title for this fragment
     * @return The title to display
     */
    public String getScreenTitle() {
        return "Settings";
    }
}
