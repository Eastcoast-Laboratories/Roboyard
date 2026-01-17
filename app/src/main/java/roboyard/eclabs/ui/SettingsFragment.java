package roboyard.eclabs.ui;

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
import java.util.List;

import roboyard.eclabs.R;
import roboyard.eclabs.ui.MainFragmentActivity;
import roboyard.logic.core.Constants;
import roboyard.logic.core.Preferences;
import timber.log.Timber;

import java.util.Locale;
import android.content.res.Configuration;
import android.content.res.Resources;

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
    private RadioGroup fullscreenRadioGroup;
    private RadioButton fullscreenOn;
    private RadioButton fullscreenOff;
    // Game mode radio buttons
    private RadioGroup gameModeRadioGroup;
    private RadioButton standardGameModeButton;
    private RadioButton multiTargetGameModeButton;
    private Spinner targetColorsSpinner;
    private Button backButton;
    private Spinner robotCountSpinner;
    private LinearLayout targetCountContainer; // Container for the target settings
    
    // Language settings
    private Spinner languageSpinner;
    private Spinner talkbackLanguageSpinner;
    private LinearLayout talkbackLanguageContainer;
    
    // Puzzle parameter settings
    private Spinner minSolutionMovesSpinner;
    private Spinner maxSolutionMovesSpinner;
    private RadioGroup allowMulticolorTargetRadioGroup;
    private RadioButton allowMulticolorTargetYes;
    private RadioButton allowMulticolorTargetNo;
    
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
            fullscreenRadioGroup = view.findViewById(R.id.fullscreen_radio_group);
            fullscreenOn = view.findViewById(R.id.fullscreen_on);
            fullscreenOff = view.findViewById(R.id.fullscreen_off);
            // Game mode radio buttons
            gameModeRadioGroup = view.findViewById(R.id.game_mode_radio_group);
            standardGameModeButton = view.findViewById(R.id.standard_game_mode);
            multiTargetGameModeButton = view.findViewById(R.id.multi_target_game_mode);
            targetColorsSpinner = view.findViewById(R.id.target_colors_spinner);
            targetCountContainer = view.findViewById(R.id.target_count_container);
            backButton = view.findViewById(R.id.back_button);
            robotCountSpinner = view.findViewById(R.id.robot_count_spinner);
            
            // Language settings
            languageSpinner = view.findViewById(R.id.language_spinner);
            talkbackLanguageSpinner = view.findViewById(R.id.talkback_language_spinner);
            talkbackLanguageContainer = view.findViewById(R.id.talkback_language_container);
            
            // Puzzle parameter settings
            minSolutionMovesSpinner = view.findViewById(R.id.min_solution_moves_spinner);
            maxSolutionMovesSpinner = view.findViewById(R.id.max_solution_moves_spinner);
            allowMulticolorTargetRadioGroup = view.findViewById(R.id.allow_multicolor_target_radio_group);
            allowMulticolorTargetYes = view.findViewById(R.id.allow_multicolor_target_yes);
            allowMulticolorTargetNo = view.findViewById(R.id.allow_multicolor_target_no);
            
            // Set up board size options
            setupBoardSizeOptions();
            
            // Set up robot count spinner
            setupRobotCountSpinner();
            
            // Set up target colors spinner
            setupTargetColorsSpinner();
            
            // Set up language spinners
            setupLanguageSpinners();
            
            // Set up puzzle parameter spinners
            setupSolutionMovesSpinners();
            
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
            errorMessage.setText("Error loading settings screen. Please send us a bug report with details below.");
            errorMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            errorMessage.setGravity(Gravity.CENTER);
            errorMessage.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            fallbackLayout.addView(errorMessage);
            
            // Add error details
            TextView errorDetails = new TextView(requireContext());
            StringBuilder detailsBuilder = new StringBuilder()
                    .append("Error details:\n")
                    .append("Device: ").append(android.os.Build.MANUFACTURER).append(" ")
                    .append(android.os.Build.MODEL).append("\n")
                    .append("Android: ").append(android.os.Build.VERSION.RELEASE)
                    .append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n")
                    .append("App version: ").append(getVersionName()).append("\n");
            
            if (e != null) {
                detailsBuilder.append("\nException:\n").append(e.getClass().getName())
                       .append(": ").append(e.getMessage()).append("\n");
                
                if (e.getCause() != null) {
                    detailsBuilder.append("Caused by: ").append(e.getCause().getClass().getName())
                            .append(": ").append(e.getCause().getMessage()).append("\n");
                }
                
                StackTraceElement[] stack = e.getStackTrace();
                if (stack != null && stack.length > 0) {
                    for (int i = 0; i < Math.min(5, stack.length); i++) {
                        detailsBuilder.append("at ").append(stack[i].toString()).append("\n");
                    }
                }
            }
            
            errorDetails.setText(detailsBuilder.toString());
            errorDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            errorDetails.setPadding(16, 32, 16, 32);
            errorDetails.setBackgroundColor(0x22FFFFFF); // Semi-transparent white background
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            detailsParams.setMargins(8, 16, 8, 16);
            errorDetails.setLayoutParams(detailsParams);
            fallbackLayout.addView(errorDetails);
            
            // Add email report button
            Button reportButton = new Button(requireContext());
            reportButton.setText("Send Bug Report");
            reportButton.setOnClickListener(v -> {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"roboyard-bugreports@it.z11.de"});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Roboyard Bug Report - Settings Screen Crash");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, detailsBuilder.toString());
                    startActivity(Intent.createChooser(emailIntent, "Send Bug Report"));
                } catch (Exception ex) {
                    // Get localized context
                    Context localizedContext = roboyard.eclabs.RoboyardApplication.getAppContext();
                    Toast.makeText(requireContext(), localizedContext.getString(R.string.error_email_app), Toast.LENGTH_SHORT).show();
                    Timber.e(ex, "Failed to send email report");
                }
            });
            LinearLayout.LayoutParams reportButtonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            reportButtonParams.gravity = Gravity.CENTER;
            reportButtonParams.topMargin = 16;
            reportButton.setLayoutParams(reportButtonParams);
            fallbackLayout.addView(reportButton);
            
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
            buttonParams.topMargin = 16;
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
            boolean fullscreenEnabled = Preferences.fullscreenEnabled;
            
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
            
            // Set fullscreen radio buttons
            if (fullscreenRadioGroup != null) {
                if (fullscreenEnabled) {
                    if (fullscreenOn != null) fullscreenOn.setChecked(true);
                } else {
                    if (fullscreenOff != null) fullscreenOff.setChecked(true);
                }
            } else {
                Timber.e("fullscreenRadioGroup is null");
            }
            
            isUpdatingUI = false;
            
            // Load language settings
            String appLanguage = Preferences.appLanguage;
            String talkbackLanguage = Preferences.talkbackLanguage;
            
            // Set app language spinner selection
            int appLanguageIndex = 0; // Default to English
            if ("de".equals(appLanguage)) {
                appLanguageIndex = 1; // German
            } else if ("fr".equals(appLanguage)) {
                appLanguageIndex = 2; // French
            } else if ("es".equals(appLanguage)) {
                appLanguageIndex = 3; // Spanish
            } else if ("zh".equals(appLanguage)) {
                appLanguageIndex = 4; // Chinese
            } else if ("ko".equals(appLanguage)) {
                appLanguageIndex = 5; // Korean
            }
            languageSpinner.setSelection(appLanguageIndex);
            
            // Set TalkBack language spinner selection
            int talkbackLanguageIndex = 0; // Default to "Same as app"
            if ("en".equals(talkbackLanguage)) {
                talkbackLanguageIndex = 1; // English
            } else if ("de".equals(talkbackLanguage)) {
                talkbackLanguageIndex = 2; // German
            } else if ("fr".equals(talkbackLanguage)) {
                talkbackLanguageIndex = 3; // French
            } else if ("es".equals(talkbackLanguage)) {
                talkbackLanguageIndex = 4; // Spanish
            } else if ("zh".equals(talkbackLanguage)) {
                talkbackLanguageIndex = 5; // Chinese
            } else if ("ko".equals(talkbackLanguage)) {
                talkbackLanguageIndex = 6; // Korean
            }
            talkbackLanguageSpinner.setSelection(talkbackLanguageIndex);
            
            // Load robot count
            int robotCount = Preferences.robotCount;
            robotCountSpinner.setSelection(robotCount - 1); // Zero-based index
            
            // Load target colors
            int targetColors = Preferences.targetColors;
            targetColorsSpinner.setSelection(targetColors - 1); // Zero-based index
            
            // Load game mode
            int gameMode = Preferences.gameMode;
            if (gameMode == Constants.GAME_MODE_STANDARD) {
                if (standardGameModeButton != null) standardGameModeButton.setChecked(true);
                if (targetCountContainer != null) targetCountContainer.setVisibility(View.GONE);
            } else if (gameMode == Constants.GAME_MODE_MULTI_TARGET) {
                if (multiTargetGameModeButton != null) multiTargetGameModeButton.setChecked(true);
                if (targetCountContainer != null) targetCountContainer.setVisibility(View.VISIBLE);
            }
            
            // Load puzzle parameters
            int minMoves = Preferences.minSolutionMoves;
            int maxMoves = Preferences.maxSolutionMoves;
            boolean allowMulticolor = Preferences.allowMulticolorTarget;
            
            if (minSolutionMovesSpinner != null && minMoves >= 1 && minMoves <= 20) {
                minSolutionMovesSpinner.setSelection(minMoves - 1);
            }
            if (maxSolutionMovesSpinner != null && maxMoves >= 1 && maxMoves <= 20) {
                maxSolutionMovesSpinner.setSelection(maxMoves - 1);
            }
            if (allowMulticolorTargetRadioGroup != null) {
                if (allowMulticolor) {
                    if (allowMulticolorTargetYes != null) allowMulticolorTargetYes.setChecked(true);
                } else {
                    if (allowMulticolorTargetNo != null) allowMulticolorTargetNo.setChecked(true);
                }
            }
            
            isUpdatingUI = false;
            Timber.d("Settings loaded successfully");
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
            for (int i = 1; i <= Constants.NUM_ROBOTS; i++) {
                adapter.add(i);
            }
            robotCountSpinner.setAdapter(adapter);
            
            // Set current value from static Preferences
            int robotCount = Preferences.robotCount;
            if (robotCount < 1 || robotCount > Constants.NUM_ROBOTS) {
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
                            robotCountSpinner.setSelection(selectedRobotCount - 1);
                            
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
            
            // Create adapter with localized text values
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            for (int i = 1; i <= Constants.NUM_ROBOTS; i++) {
                // Format as "1 of 4 targets" with localization
                adapter.add(String.valueOf(i));
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
                        
                        // In multi-target mode, enforce minimum of 2 targets
                        if (Preferences.gameMode == Constants.GAME_MODE_MULTI_TARGET && selectedTargetColors < 2) {
                            selectedTargetColors = 2;
                            targetColorsSpinner.setSelection(selectedTargetColors - 1);
                            Toast.makeText(requireContext(), 
                                    "Multi-target mode requires at least 2 targets", 
                                    Toast.LENGTH_SHORT).show();
                        }
                        
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
     * Sets up the solution moves spinners (min and max)
     */
    private void setupSolutionMovesSpinners() {
        try {
            if (minSolutionMovesSpinner == null || maxSolutionMovesSpinner == null) {
                Timber.e("setupSolutionMovesSpinners: One or both spinners are null");
                return;
            }
            
            // Create adapter with values 1-20
            ArrayAdapter<Integer> minAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
            minAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            for (int i = 1; i <= 20; i++) {
                minAdapter.add(i);
            }
            minSolutionMovesSpinner.setAdapter(minAdapter);
            
            ArrayAdapter<Integer> maxAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
            maxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            for (int i = 1; i <= 20; i++) {
                maxAdapter.add(i);
            }
            maxSolutionMovesSpinner.setAdapter(maxAdapter);
            
            // Set current values from preferences
            int minMoves = Preferences.minSolutionMoves;
            int maxMoves = Preferences.maxSolutionMoves;
            
            if (minMoves >= 1 && minMoves <= 20) {
                minSolutionMovesSpinner.setSelection(minMoves - 1);
            }
            if (maxMoves >= 1 && maxMoves <= 20) {
                maxSolutionMovesSpinner.setSelection(maxMoves - 1);
            }
            
            // Set listeners
            minSolutionMovesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        int selectedMin = position + 1;
                        int currentMax = Preferences.maxSolutionMoves;
                        
                        if (selectedMin > currentMax) {
                            selectedMin = currentMax;
                            minSolutionMovesSpinner.setSelection(selectedMin - 1);
                            Toast.makeText(requireContext(), 
                                    "Min moves cannot exceed max moves", 
                                    Toast.LENGTH_SHORT).show();
                        }
                        
                        Preferences.setMinSolutionMoves(selectedMin);
                        Timber.d("[PREFERENCES] Min solution moves set to %d", selectedMin);
                    } catch (Exception e) {
                        Timber.e(e, "Error processing min solution moves selection");
                    }
                }
                
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            
            maxSolutionMovesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        int selectedMax = position + 1;
                        int currentMin = Preferences.minSolutionMoves;
                        
                        if (selectedMax < currentMin) {
                            selectedMax = currentMin;
                            maxSolutionMovesSpinner.setSelection(selectedMax - 1);
                            Toast.makeText(requireContext(), 
                                    "Max moves cannot be less than min moves", 
                                    Toast.LENGTH_SHORT).show();
                        }
                        
                        Preferences.setMaxSolutionMoves(selectedMax);
                        Timber.d("[PREFERENCES] Max solution moves set to %d", selectedMax);
                    } catch (Exception e) {
                        Timber.e(e, "Error processing max solution moves selection");
                    }
                }
                
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } catch (Exception e) {
            Timber.e(e, "Error setting up solution moves spinners");
        }
    }
    
    /**
     * Set up language spinners with language options
     */
    private void setupLanguageSpinners() {
        Timber.d("Setting up language spinners");
        
        // Set up app language spinner
        List<String> languages = new ArrayList<>();
        languages.add(getString(R.string.settings_english));
        languages.add(getString(R.string.settings_german));
        languages.add(getString(R.string.settings_french));
        languages.add(getString(R.string.settings_spanish));
        languages.add(getString(R.string.settings_chinese));
        languages.add(getString(R.string.settings_korean));
        
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, languages);
        languageSpinner.setAdapter(languageAdapter);
        
        // Set up TalkBack language spinner (including "Same as app" option)
        List<String> talkbackLanguages = new ArrayList<>();
        talkbackLanguages.add(getString(R.string.language_same_as_app));
        talkbackLanguages.add(getString(R.string.settings_english));
        talkbackLanguages.add(getString(R.string.settings_german));
        talkbackLanguages.add(getString(R.string.settings_french));
        talkbackLanguages.add(getString(R.string.settings_spanish));
        talkbackLanguages.add(getString(R.string.settings_chinese));
        talkbackLanguages.add(getString(R.string.settings_korean));
        
        ArrayAdapter<String> talkbackLanguageAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, talkbackLanguages);
        talkbackLanguageSpinner.setAdapter(talkbackLanguageAdapter);
        
        // Show/hide TalkBack language selection based on accessibility setting
        updateTalkbackLanguageVisibility(Preferences.accessibilityMode);
        
        // Set listeners for language changes
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    String selectedLanguage = languages.get(position);
                    String languageCode = getLanguageCode(selectedLanguage);
                    applyLanguageSetting(languageCode);
                } catch (Exception e) {
                    Timber.e(e, "Error processing language selection");
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        
        talkbackLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    String selectedLanguage = talkbackLanguages.get(position);
                    String languageCode = getTalkbackLanguageCode(selectedLanguage);
                    applyTalkbackLanguageSetting(languageCode);
                } catch (Exception e) {
                    Timber.e(e, "Error processing TalkBack language selection");
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    /**
     * Show or hide TalkBack language settings based on accessibility setting
     * @param accessibilityEnabled Whether accessibility is enabled
     */
    private void updateTalkbackLanguageVisibility(boolean accessibilityEnabled) {
        if (talkbackLanguageContainer != null) {
            talkbackLanguageContainer.setVisibility(accessibilityEnabled ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * Apply language settings
     * @param languageCode The language code to apply (en, de, fr)
     */
    private void applyLanguageSetting(String languageCode) {
        Timber.d("Applying language setting: %s", languageCode);
        
        // Save language preference using proper setter method that persists to SharedPreferences
        Preferences.setAppLanguage(languageCode);
        
        // Apply language change
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Resources resources = requireContext().getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        
        // If TalkBack language is set to "Same as app", update it as well
        if (Preferences.talkbackLanguage.equals(getString(R.string.language_same_as_app))) {
            applyTalkbackLanguageSetting(languageCode);
        }
        
        // Log for diagnostics
        Timber.d("ROBOYARD_LANGUAGE: Changed app language to %s", languageCode);
    }
    
    /**
     * Apply TalkBack language settings
     * @param languageCode The language code to apply (en, de, fr, or "same" for same as app)
     */
    private void applyTalkbackLanguageSetting(String languageCode) {
        Timber.d("Applying TalkBack language setting: %s", languageCode);
        
        // Save TalkBack language preference using proper setter method that persists to SharedPreferences
        Preferences.setTalkbackLanguage(languageCode);
        
        // If set to "same", use app language
        String actualLanguageCode = languageCode.equals(getString(R.string.talkback_language_same_value)) ? Preferences.appLanguage : languageCode;
        
        // Log for diagnostics
        Timber.d("ROBOYARD_ACCESSIBILITY_LANGUAGE: Changed TalkBack language to %s", actualLanguageCode);
    }
    
    /**
     * Get the language code for the given language string
     * @param language The language string (e.g. "English", "Deutsch", "Français")
     * @return The corresponding language code (e.g. "en", "de", "fr")
     */
    private String getLanguageCode(String language) {
        switch (language) {
            case "English":
                return "en";
            case "Deutsch":
                return "de";
            case "Français":
                return "fr";
            case "Español":
                return "es";
            case "中文":
                return "zh";
            case "한국어":
                return "ko";
            default:
                return "en"; // Default to English
        }
    }
    
    /**
     * Get the TalkBack language code for the given TalkBack language string
     * @param language The TalkBack language string (e.g. "Same as app", "English", "Deutsch", "Français")
     * @return The corresponding TalkBack language code (e.g. "same", "en", "de", "fr")
     */
    private String getTalkbackLanguageCode(String language) {
        switch (language) {
            case "Same as app":
                return "same";
            case "English":
                return "en";
            case "Deutsch":
                return "de";
            case "Français":
                return "fr";
            case "Español":
                return "es";
            case "中文":
                return "zh";
            case "한국어":
                return "ko";
            default:
                return "same"; // Default to "Same as app"
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
            // Get localized context
            Context localizedContext = roboyard.eclabs.RoboyardApplication.getAppContext();
            
            // Use localized warning message
            Toast toast = Toast.makeText(requireContext(),
                    localizedContext.getString(R.string.impossible_small_board_warning),
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
        builder.setNegativeButton("Other screen reader", (dialog, which) -> {
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
            isUpdatingUI = true;
            
            // Board size spinner
            if (boardSizeSpinner != null) {
                boardSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        try {
                            if (isUpdatingUI) {
                                // Skip processing during initial UI setup
                                isUpdatingUI = false;
                                Timber.d("[BOARD_SIZE] Skipping board size change during UI update");
                                return;
                            }
                            
                            // Get selected board size
                            if (position >= 0 && position < validBoardSizes.size()) {
                                int[] selectedSize = validBoardSizes.get(position);
                                int width = selectedSize[0];
                                int height = selectedSize[1];
                                
                                Timber.d("[BOARD_SIZE] Selected board size: %dx%d", width, height);
                                
                                // Save board size to preferences
                                Preferences.setBoardSize(width, height);
                                
                                // Check if difficult is set to impossible with a small board size
                                if (difficultyRadioGroup.getCheckedRadioButtonId() == R.id.difficulty_impossible) {
                                    showImpossibleDifficultyToast();
                                }
                            }
                        } catch (Exception e) {
                            Timber.e(e, "[BOARD_SIZE] Error processing board size selection");
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
                        if (isUpdatingUI) return;
                        
                        int previousDifficulty = Preferences.difficulty;
                        int difficulty = Constants.DIFFICULTY_BEGINNER; // Default
                        
                        if (checkedId == R.id.difficulty_beginner) {
                            difficulty = Constants.DIFFICULTY_BEGINNER;
                        } else if (checkedId == R.id.difficulty_advanced) {
                            difficulty = Constants.DIFFICULTY_ADVANCED;
                        } else if (checkedId == R.id.difficulty_insane) {
                            difficulty = Constants.DIFFICULTY_INSANE;
                        } else if (checkedId == R.id.difficulty_impossible) {
                            difficulty = Constants.DIFFICULTY_IMPOSSIBLE;
                        }
                        
                        Preferences.setDifficulty(difficulty);
                        Timber.d("[PREFERENCES] Difficulty set to %d", difficulty);
                        
                        // Automatically adjust board size only if switching TO beginner mode from another difficulty
                        if (difficulty == Constants.DIFFICULTY_BEGINNER && previousDifficulty != Constants.DIFFICULTY_BEGINNER) {
                            adjustBoardSizeForBeginnerMode();
                        }
                        
                        // Automatically adjust puzzle parameters based on difficulty
                        adjustPuzzleParametersForDifficulty(difficulty);
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
                        if (isUpdatingUI) return;
                        
                        boolean generateNewMap = checkedId == R.id.new_map_yes; // Default to Yes
                        
                        Preferences.setGenerateNewMapEachTime(generateNewMap);
                        Timber.d("[PREFERENCES] Generate new map set to %b", generateNewMap);
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
                        if (isUpdatingUI) return;
                        
                        boolean soundEnabled = checkedId == R.id.sound_on; // Default to On
                        
                        Preferences.setSoundEnabled(soundEnabled);
                        Timber.d("[PREFERENCES] Sound enabled set to %b", soundEnabled);
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
                        if (isUpdatingUI) return;
                        
                        boolean accessibilityMode = checkedId == R.id.accessibility_on; // Default to Off
                        
                        Preferences.setAccessibilityMode(accessibilityMode);
                        
                        // Update TalkBack language visibility
                        updateTalkbackLanguageVisibility(accessibilityMode);
                        
                        Timber.d("[PREFERENCES] Accessibility mode set to %b", accessibilityMode);
                        
                        if (accessibilityMode) {
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
            
            // Fullscreen radio group
            if (fullscreenRadioGroup != null) {
                fullscreenRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    try {
                        if (isUpdatingUI) return;
                        
                        boolean fullscreenMode = checkedId == R.id.fullscreen_on; // Default to Off
                        
                        // Check if the setting has actually changed
                        if (fullscreenMode != Preferences.fullscreenEnabled) {
                            // Update the preference
                            Preferences.setFullscreenEnabled(fullscreenMode);
                            
                            Timber.d("[PREFERENCES] Fullscreen mode set to %b", fullscreenMode);
                            
                            // Show toast message about applying fullscreen settings
                            Toast.makeText(requireContext(), 
                                getString(R.string.settings_fullscreen_applying), 
                                Toast.LENGTH_SHORT).show();
                            
                            // Restart the activity to apply the fullscreen setting
                            requireActivity().finish();
                            Intent intent = new Intent(requireActivity(), MainFragmentActivity.class);
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Error processing fullscreen selection");
                    }
                });
            } else {
                Timber.e("fullscreenRadioGroup is null");
            }
            
            // Game mode radio group
            if (gameModeRadioGroup != null) {
                gameModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    try {
                        if (isUpdatingUI) return;
                        
                        if (checkedId == R.id.standard_game_mode) {
                            // Standard game mode - set robot count and target colors to 1
                            Preferences.setGameMode(Constants.GAME_MODE_STANDARD);
                            Preferences.setRobotCount(1);
                            Preferences.setTargetColors(1);
                            
                            // Update UI
                            if (robotCountSpinner != null) {
                                robotCountSpinner.setSelection(0); // 1 robot (index 0)
                            }
                            if (targetColorsSpinner != null) {
                                targetColorsSpinner.setSelection(0); // 1 target color (index 0)
                            }
                            
                            // Hide target count container
                            if (targetCountContainer != null) {
                                targetCountContainer.setVisibility(View.GONE);
                            }
                            
                            Timber.d("[GAME_MODE] Switched to Standard Game mode");
                        } else if (checkedId == R.id.multi_target_game_mode) {
                            // Multi-target mode - enforce minimum of 2 target colors
                            Preferences.setGameMode(Constants.GAME_MODE_MULTI_TARGET);
                            
                            // Ensure at least 2 target colors
                            int targetColors = Preferences.targetColors;
                            if (targetColors < 2) {
                                Preferences.setTargetColors(2);
                                if (targetColorsSpinner != null) {
                                    targetColorsSpinner.setSelection(1); // 2 target colors (index 1)
                                }
                            }
                            
                            // Show target count container
                            if (targetCountContainer != null) {
                                targetCountContainer.setVisibility(View.VISIBLE);
                            }
                            
                            Timber.d("[GAME_MODE] Switched to Multi-target mode");
                        }
                    } catch (Exception e) {
                        Timber.e(e, "[GAME_MODE] Error processing game mode selection: %s", e.getMessage());
                    }
                });
            } else {
                Timber.e("[GAME_MODE] gameModeRadioGroup is null");
            }
            
            // Allow multicolor target radio group
            if (allowMulticolorTargetRadioGroup != null) {
                allowMulticolorTargetRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    try {
                        if (isUpdatingUI) return;
                        
                        boolean allowMulticolor = checkedId == R.id.allow_multicolor_target_yes;
                        
                        Preferences.setAllowMulticolorTarget(allowMulticolor);
                        Timber.d("[PREFERENCES] Allow multicolor target set to %b", allowMulticolor);
                    } catch (Exception e) {
                        Timber.e(e, "Error processing allow multicolor target selection");
                    }
                });
            } else {
                Timber.e("allowMulticolorTargetRadioGroup is null");
            }
            
            // Back button
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    try {
                        // Go back to previous screen
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Error handling back button click");
                    }
                });
            } else {
                Timber.e("backButton is null");
            }
            
            isUpdatingUI = false;
        } catch (Exception e) {
            Timber.e(e, "Error setting up listeners");
            isUpdatingUI = false; // Make sure to reset this flag even if an error occurs
        }
    }
    
    /**
     * Get the screen title for this fragment
     * @return The title to display
     */
    public String getScreenTitle() {
        return "Settings";
    }
    
    /**
     * Check if current board size needs to be adjusted for beginner difficulty.
     * If board size is larger than 12x14, automatically set it to 12x14.
     */
    private void adjustBoardSizeForBeginnerMode() {
        try {
            int currentWidth = Preferences.boardSizeWidth;
            int currentHeight = Preferences.boardSizeHeight;
            
            // Check if current board size is larger than 12x14
            if (currentWidth > 12 || currentHeight > 14) {
                Timber.d("[BOARD_SIZE] Current size %dx%d is larger than 12x14, adjusting for beginner mode", 
                        currentWidth, currentHeight);
                
                // Set board size to 12x14
                isUpdatingUI = true; // Prevent recursive updates
                Preferences.setBoardSize(12, 14);
                
                // Update the spinner UI to reflect the change
                if (boardSizeSpinner != null && validBoardSizes != null) {
                    for (int i = 0; i < validBoardSizes.size(); i++) {
                        int[] size = validBoardSizes.get(i);
                        if (size[0] == 12 && size[1] == 14) {
                            boardSizeSpinner.setSelection(i);
                            Timber.d("[BOARD_SIZE] Updated spinner to show 12x14 at position %d", i);
                            break;
                        }
                    }
                }
                isUpdatingUI = false;
                
                Timber.d("[BOARD_SIZE] Board size automatically adjusted to 12x14 for beginner difficulty");
            } else {
                Timber.d("[BOARD_SIZE] Current size %dx%d is suitable for beginner mode", currentWidth, currentHeight);
            }
        } catch (Exception e) {
            Timber.e(e, "Error adjusting board size for beginner mode");
            isUpdatingUI = false; // Reset flag in case of error
        }
    }
    
    /**
     * Adjust puzzle parameters (min/max moves and multicolor target) based on difficulty level
     * Mirrors the difficulty settings from README.md
     */
    private void adjustPuzzleParametersForDifficulty(int difficulty) {
        try {
            isUpdatingUI = true;
            
            int minMoves = 4;
            int maxMoves = 6;
            boolean allowMulticolor = true;
            
            switch (difficulty) {
                case Constants.DIFFICULTY_BEGINNER:
                    // Beginner: 4-6 moves, multicolor allowed
                    minMoves = 4;
                    maxMoves = 6;
                    allowMulticolor = true;
                    break;
                case Constants.DIFFICULTY_ADVANCED:
                    // Advanced: 6-10 moves, no multicolor
                    minMoves = 6;
                    maxMoves = 10;
                    allowMulticolor = false;
                    break;
                case Constants.DIFFICULTY_INSANE:
                    // Insane: 10+ moves, no multicolor
                    minMoves = 10;
                    maxMoves = 20;
                    allowMulticolor = false;
                    break;
                case Constants.DIFFICULTY_IMPOSSIBLE:
                    // Impossible: 17+ moves, no multicolor
                    minMoves = 17;
                    maxMoves = 20;
                    allowMulticolor = false;
                    break;
            }
            
            // Update preferences
            Preferences.setMinSolutionMoves(minMoves);
            Preferences.setMaxSolutionMoves(maxMoves);
            Preferences.setAllowMulticolorTarget(allowMulticolor);
            
            // Update UI
            if (minSolutionMovesSpinner != null) {
                minSolutionMovesSpinner.setSelection(minMoves - 1);
            }
            if (maxSolutionMovesSpinner != null) {
                maxSolutionMovesSpinner.setSelection(maxMoves - 1);
            }
            if (allowMulticolorTargetRadioGroup != null) {
                if (allowMulticolor) {
                    if (allowMulticolorTargetYes != null) allowMulticolorTargetYes.setChecked(true);
                } else {
                    if (allowMulticolorTargetNo != null) allowMulticolorTargetNo.setChecked(true);
                }
            }
            
            Timber.d("[DIFFICULTY_PARAMS] Adjusted puzzle parameters for difficulty %d: min=%d, max=%d, allowMulticolor=%b", 
                    difficulty, minMoves, maxMoves, allowMulticolor);
            
            isUpdatingUI = false;
        } catch (Exception e) {
            Timber.e(e, "Error adjusting puzzle parameters for difficulty");
            isUpdatingUI = false;
        }
    }
    
    /**
     * Get the app version name from the package info
     * @return The version name string or "Unknown" if it couldn't be determined
     */
    private String getVersionName() {
        try {
            Context context = getContext();
            if (context != null) {
                return context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0).versionName;
            }
        } catch (Exception e) {
            Timber.e(e, "Error getting app version");
        }
        return "Unknown";
    }
}
