package roboyard.eclabs.ui;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import roboyard.eclabs.FileReadWrite;
import roboyard.eclabs.RoboyardApiClient;
import roboyard.logic.core.Constants;
import roboyard.eclabs.R;

import roboyard.logic.core.GameState;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * Level Design Editor Fragment
 * Allows creating and editing game levels
 */
public class LevelDesignEditorFragment extends Fragment {

    private static final String ARG_LEVEL_ID = "level_id";
    private static final int MAX_CUSTOM_LEVEL_ID = 999;
    private static final int FIRST_CUSTOM_LEVEL_ID = 141;
    
    private int currentLevelId = 0;
    private GameState currentState;
    private GameStateManager gameStateManager;
    
    // UI elements
    private TextView levelIdTextView;
    private Spinner editLevelSpinner;
    private RadioGroup editModeRadioGroup;
    private Button cancelButton;
    private Button exportButton;
    private TextView levelTextView;
    private EditText boardWidthEditText;
    private EditText boardHeightEditText;
    private Button applyBoardSizeButton;
    private RadioGroup robotColorRadioGroup;
    private RadioGroup targetColorRadioGroup;
    
    // Edit modes
    private static final int EDIT_MODE_ROBOT = 0;
    private static final int EDIT_MODE_TARGET = 1;
    private static final int EDIT_MODE_WALL_H = 2;
    private static final int EDIT_MODE_WALL_V = 3;
    private static final int EDIT_MODE_ERASE = 4;
    
    private int currentEditMode = EDIT_MODE_ROBOT;
    private int currentRobotColor = 0; // 0=red, 1=green, 2=blue, 3=yellow
    private int currentTargetColor = 0; // 0=red, 1=green, 2=blue, 3=yellow
    
    /**
     * Create a new instance of this fragment
     * @param levelId Level ID to edit, or 0 for a new level
     * @return A new instance of fragment LevelDesignEditorFragment
     */
    public static LevelDesignEditorFragment newInstance(int levelId) {
        LevelDesignEditorFragment fragment = new LevelDesignEditorFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LEVEL_ID, levelId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentLevelId = getArguments().getInt(ARG_LEVEL_ID, 0);
        }
        gameStateManager = new GameStateManager(requireActivity().getApplication());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_level_design_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Unlock all 420 stars when opening the level design editor
        LevelCompletionManager completionManager = LevelCompletionManager.getInstance(requireContext());
        completionManager.unlockAllStars();
        Timber.d("LevelDesignEditorFragment: Unlocked all 420 stars for level design editor");

        // Initialize UI elements
        levelIdTextView = view.findViewById(R.id.level_id_text);
        editLevelSpinner = view.findViewById(R.id.edit_level_spinner);
        editModeRadioGroup = view.findViewById(R.id.edit_mode_radio_group);
        cancelButton = view.findViewById(R.id.cancel_button);
        exportButton = view.findViewById(R.id.export_level_button);
        levelTextView = view.findViewById(R.id.level_text_view);
        boardWidthEditText = view.findViewById(R.id.board_width_edit_text);
        boardHeightEditText = view.findViewById(R.id.board_height_edit_text);
        applyBoardSizeButton = view.findViewById(R.id.apply_board_size_button);
        robotColorRadioGroup = view.findViewById(R.id.robot_color_radio_group);
        targetColorRadioGroup = view.findViewById(R.id.target_color_radio_group);
        
        // Set up robot color radio buttons for selection
        setupColorRadioButtons();
        
        // Setup level spinner
        setupLevelSpinner();
        
        // Set up map touch events
        setupMapTouchEvents();
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Set up mode selection
        setupEditModeRadioGroup();
        
        // Load the initial level (last played level, fallback to level 1)
        if (currentLevelId == 0) {
            int lastPlayed = completionManager.getLastPlayedLevel();
            
            if (lastPlayed > 0 && levelExistsInSpinner(lastPlayed)) {
                currentLevelId = lastPlayed;
                loadLevel(currentLevelId);
                setSpinnerToLevel(currentLevelId);
            } else {
                currentLevelId = 1;
                loadLevel(currentLevelId);
                setSpinnerToLevel(1);
            }
        } else {
            loadLevel(currentLevelId);
            setSpinnerToLevel(currentLevelId);
        }
        
        Timber.d("LevelDesignEditorFragment: onViewCreated, loaded level %d", currentLevelId);
    }
    
    private void setupColorRadioButtons() {
        // Set up the radio buttons for robot colors
        RadioButton redRobotButton = requireView().findViewById(R.id.robot_red_radio);
        RadioButton greenRobotButton = requireView().findViewById(R.id.robot_green_radio);
        RadioButton blueRobotButton = requireView().findViewById(R.id.robot_blue_radio);
        RadioButton yellowRobotButton = requireView().findViewById(R.id.robot_yellow_radio);
        
        redRobotButton.setChecked(true); // Default selection
        
        robotColorRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.robot_red_radio) {
                currentRobotColor = Constants.COLOR_PINK;
            } else if (checkedId == R.id.robot_green_radio) {
                currentRobotColor = Constants.COLOR_GREEN;
            } else if (checkedId == R.id.robot_blue_radio) {
                currentRobotColor = Constants.COLOR_BLUE;
            } else if (checkedId == R.id.robot_yellow_radio) {
                currentRobotColor = Constants.COLOR_YELLOW;
            } else if (checkedId == R.id.robot_silver_radio) {
                currentRobotColor = Constants.COLOR_SILVER;
            }
        });
        
        // Set up the radio buttons for target colors
        RadioButton redTargetButton = requireView().findViewById(R.id.target_red_radio);
        RadioButton greenTargetButton = requireView().findViewById(R.id.target_green_radio);
        RadioButton blueTargetButton = requireView().findViewById(R.id.target_blue_radio);
        RadioButton yellowTargetButton = requireView().findViewById(R.id.target_yellow_radio);
        
        redTargetButton.setChecked(true); // Default selection
        
        targetColorRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.target_red_radio) {
                currentTargetColor = 0;
            } else if (checkedId == R.id.target_green_radio) {
                currentTargetColor = 1;
            } else if (checkedId == R.id.target_blue_radio) {
                currentTargetColor = 2;
            } else if (checkedId == R.id.target_yellow_radio) {
                currentTargetColor = 3;
            }
        });
    }
    
    private void setupEditModeRadioGroup() {
        RadioGroup editModeGroup = requireView().findViewById(R.id.edit_mode_radio_group);
        
        editModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.mode_robot_radio) {
                currentEditMode = EDIT_MODE_ROBOT;
                robotColorRadioGroup.setVisibility(View.VISIBLE);
                targetColorRadioGroup.setVisibility(View.GONE);
                Timber.d("Edit mode set to ROBOT");
            } else if (checkedId == R.id.mode_target_radio) {
                currentEditMode = EDIT_MODE_TARGET;
                robotColorRadioGroup.setVisibility(View.GONE);
                targetColorRadioGroup.setVisibility(View.VISIBLE);
                Timber.d("Edit mode set to TARGET");
            } else if (checkedId == R.id.mode_wall_h_radio) {
                currentEditMode = EDIT_MODE_WALL_H;
                robotColorRadioGroup.setVisibility(View.GONE);
                targetColorRadioGroup.setVisibility(View.GONE);
                Timber.d("Edit mode set to HORIZONTAL WALL");
            } else if (checkedId == R.id.mode_wall_v_radio) {
                currentEditMode = EDIT_MODE_WALL_V;
                robotColorRadioGroup.setVisibility(View.GONE);
                targetColorRadioGroup.setVisibility(View.GONE);
                Timber.d("Edit mode set to VERTICAL WALL");
            } else if (checkedId == R.id.mode_erase_radio) {
                currentEditMode = EDIT_MODE_ERASE;
                robotColorRadioGroup.setVisibility(View.GONE);
                targetColorRadioGroup.setVisibility(View.GONE);
                Timber.d("Edit mode set to ERASE");
            }
        });
    }
    
    private void setupLevelSpinner() {
        // Populate spinner with all available levels, sorted numerically
        List<String> levelOptions = new ArrayList<>();
        levelOptions.add("New Level");
        
        // Collect level IDs for numeric sorting
        List<Integer> builtInIds = new ArrayList<>();
        List<Integer> customIds = new ArrayList<>();
        
        // Add existing built-in levels
        try {
            String[] files = requireContext().getAssets().list("Maps");
            for (String file : files) {
                if (file.startsWith("level_") && file.endsWith(".txt")) {
                    builtInIds.add(Integer.parseInt(file.substring(6, file.length() - 4)));
                }
            }
        } catch (IOException e) {
            Timber.e(e, "Error listing asset files");
        }
        
        // Add custom levels if they exist
        File internalDir = requireContext().getFilesDir();
        File[] internalFiles = internalDir.listFiles();
        if (internalFiles != null) {
            for (File file : internalFiles) {
                String fileName = file.getName();
                if (fileName.startsWith("custom_level_") && fileName.endsWith(".txt")) {
                    customIds.add(Integer.parseInt(fileName.substring(13, fileName.length() - 4)));
                }
            }
        }
        
        // Sort numerically
        java.util.Collections.sort(builtInIds);
        java.util.Collections.sort(customIds);
        
        for (int id : builtInIds) {
            levelOptions.add("Level " + id);
        }
        for (int id : customIds) {
            levelOptions.add("Custom Level " + id);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, levelOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editLevelSpinner.setAdapter(adapter);
        
        // Set listener to load selected level
        editLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // New level
                    createNewLevel(12, 14);
                    return;
                }
                
                int levelId;
                String selectedLevel = (String) parent.getItemAtPosition(position);
                if (selectedLevel.startsWith("Custom Level ")) {
                    levelId = Integer.parseInt(selectedLevel.substring("Custom Level ".length()));
                } else {
                    levelId = Integer.parseInt(selectedLevel.substring("Level ".length()));
                }
                
                loadLevel(levelId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private boolean levelExistsInSpinner(int levelId) {
        if (editLevelSpinner == null || editLevelSpinner.getAdapter() == null) return false;
        String target = "Level " + levelId;
        String customTarget = "Custom Level " + levelId;
        for (int i = 0; i < editLevelSpinner.getAdapter().getCount(); i++) {
            String item = (String) editLevelSpinner.getAdapter().getItem(i);
            if (target.equals(item) || customTarget.equals(item)) return true;
        }
        return false;
    }
    
    /**
     * Try to load the last played random/web map from autosave slot 0.
     * @return true if a random/web map was loaded, false otherwise
     */
    private boolean loadLastRandomMap() {
        try {
            String autosavePath = FileReadWrite.getSaveGamePath(requireActivity(), 0);
            java.io.File autosaveFile = new java.io.File(autosavePath);
            
            if (!autosaveFile.exists()) {
                Timber.d("[EDITOR] No autosave file found");
                return false;
            }
            
            String saveData = FileReadWrite.loadAbsoluteData(autosavePath);
            if (saveData == null || saveData.isEmpty()) {
                Timber.d("[EDITOR] Autosave file is empty");
                return false;
            }
            
            // Parse the save data into a GameState
            GameState state = GameState.parseFromSaveData(saveData, requireContext());
            if (state == null) {
                Timber.d("[EDITOR] Failed to parse autosave data");
                return false;
            }
            
            // Successfully loaded - set as current state
            currentState = state;
            currentLevelId = -1; // Mark as non-level (random/web map)
            
            // Update board size fields
            EditText widthEdit = requireView().findViewById(R.id.board_width_edit_text);
            EditText heightEdit = requireView().findViewById(R.id.board_height_edit_text);
            widthEdit.setText(String.valueOf(currentState.getWidth()));
            heightEdit.setText(String.valueOf(currentState.getHeight()));
            
            // Set spinner to "New Level" (index 0) since this is not a built-in level
            editLevelSpinner.setSelection(0);
            
            String mapName = state.getLevelName() != null ? state.getLevelName() : "Random Map";
            levelIdTextView.setText(mapName);
            
            updateUI();
            
            Timber.d("[EDITOR] Loaded last random/web map: %s (%dx%d, %d elements)", 
                    mapName, state.getWidth(), state.getHeight(), state.getGameElements().size());
            Toast.makeText(requireContext(), "Loaded last played map: " + mapName, Toast.LENGTH_SHORT).show();
            return true;
            
        } catch (Exception e) {
            Timber.e(e, "[EDITOR] Error loading autosave for editor");
            return false;
        }
    }
    
    private void setSpinnerToLevel(int levelId) {
        if (editLevelSpinner == null || editLevelSpinner.getAdapter() == null) return;
        String target = "Level " + levelId;
        String customTarget = "Custom Level " + levelId;
        for (int i = 0; i < editLevelSpinner.getAdapter().getCount(); i++) {
            String item = (String) editLevelSpinner.getAdapter().getItem(i);
            if (target.equals(item) || customTarget.equals(item)) {
                editLevelSpinner.setSelection(i);
                return;
            }
        }
    }
    
    private void setupMapTouchEvents() {
        // The touch events are now handled directly in the GameBoardView class
        // So we just need to ensure the board preview container is set up properly
        FrameLayout boardPreview = requireView().findViewById(R.id.board_preview_container);
        
        // Clear any existing views
        boardPreview.removeAllViews();
        
        // Initial setup message - will be replaced when updateUI() is called
        TextView placeholderText = new TextView(requireContext());
        placeholderText.setText("Board is loading...");
        placeholderText.setTextColor(Color.parseColor("#666666"));
        placeholderText.setGravity(android.view.Gravity.CENTER);
        placeholderText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        boardPreview.addView(placeholderText);
        
        Timber.d("Map touch events set up via GameBoardView");
    }
    
    private void setupButtonListeners() {
        // Apply board size button
        applyBoardSizeButton.setOnClickListener(v -> {
            try {
                int width = Integer.parseInt(boardWidthEditText.getText().toString());
                int height = Integer.parseInt(boardHeightEditText.getText().toString());
                
                // Validate board size (minimum 8x8, maximum 16x16)
                if (width < 8 || width > 16 || height < 8 || height > 16) {
                    Toast.makeText(requireContext(), "Board size must be between 8x8 and 16x16", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Update the board size
                updateBoardSize(width, height);
                
                // Remove focus from input fields alone does not  hide the overlay keyboard
                // boardWidthEditText.clearFocus();
                // boardHeightEditText.clearFocus();
                
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Cancel button
        cancelButton.setOnClickListener(v -> {
            // Navigate back to level selection
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        
        // Load Autosave button
        Button loadAutosaveButton = requireView().findViewById(R.id.load_autosave_button);
        loadAutosaveButton.setOnClickListener(v -> {
            if (!loadLastRandomMap()) {
                Toast.makeText(requireContext(), "No autosave found", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Export button
        exportButton.setOnClickListener(v -> showLevelText());
    }
    
    private void showLevelText() {
        String levelText = generateLevelText();
        
        // Create a dialog for displaying level text with copy and share options
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Level Text Format");
        
        // Create a layout for the dialog content
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(20, 20, 20, 20);
        
        // Create a selectable TextView for the level text
        EditText textView = new EditText(requireContext());
        textView.setText(levelText);
        textView.setTextIsSelectable(true);
        textView.setTextColor(Color.parseColor("#666666"));
        textView.setPadding(20, 20, 20, 20);
        textView.setBackgroundColor(Color.parseColor("#F0F0F0"));
        textView.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        textView.setMinLines(10);
        textView.setMaxLines(15);
        
        // Set layout params for the text view (make it fill width and have a reasonable height)
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(textParams);
        container.addView(textView);
        
        // Add a "Copy to Clipboard" button
        Button copyButton = new Button(requireContext());
        copyButton.setText("Copy to Clipboard");
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) 
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Level Data", levelText);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Level data copied to clipboard", Toast.LENGTH_SHORT).show();
        });
        
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0, 20, 0, 20);
        copyButton.setLayoutParams(buttonParams);
        container.addView(copyButton);
        
        // Add a TextView for the share link
        TextView shareInfoText = new TextView(requireContext());
        shareInfoText.setText("Share Link:");
        shareInfoText.setTextColor(Color.parseColor("#666666"));
        container.addView(shareInfoText);
        
        // Add a TextView for the share URL
        TextView shareUrlText = new TextView(requireContext());
        try {
            String encodedLevelText = URLEncoder.encode(levelText, "UTF-8");
            String shareUrl = "https://roboyard.z11.de/share_map?data=" + encodedLevelText;
            shareUrlText.setText(shareUrl);
            shareUrlText.setTextIsSelectable(true);
            shareUrlText.setTextColor(Color.parseColor("#0000FF"));
            shareUrlText.setPadding(20, 10, 20, 20);
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Error encoding level text");
            shareUrlText.setText("Error creating share URL");
        }
        container.addView(shareUrlText);
        
        // Add a name input field (optional for sharing)
        EditText nameEditText = new EditText(requireContext());
        nameEditText.setHint("Enter your name (optional)");
        nameEditText.setTextColor(Color.parseColor("#666666"));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT);
        nameParams.setMargins(0, 20, 0, 0);
        nameEditText.setLayoutParams(nameParams);
        container.addView(nameEditText);
        
        // Set the container as the dialog view
        builder.setView(container);
        
        // Add share and close buttons
        builder.setPositiveButton("Share Online", (dialog, which) -> {
            String userName = nameEditText.getText().toString().trim();
            shareLevelOnline(levelText, userName);
        });
        
        builder.setNegativeButton("Close", null);
        
        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void shareLevelOnline(String levelText, String mapName) {
        RoboyardApiClient apiClient = RoboyardApiClient.getInstance(requireContext());
        
        if (apiClient.isLoggedIn()) {
            // Logged in: post directly via API
            Toast.makeText(requireContext(), "Sharing to account...", Toast.LENGTH_SHORT).show();
            
            apiClient.shareMap(levelText, mapName, new RoboyardApiClient.ApiCallback<RoboyardApiClient.ShareResult>() {
                @Override
                public void onSuccess(RoboyardApiClient.ShareResult result) {
                    if (result.isDuplicate) {
                        Toast.makeText(requireContext(), "Map already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.share_success, Toast.LENGTH_SHORT).show();
                    }
                    
                    // Open the share URL in browser
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.shareUrl));
                    startActivity(intent);
                    
                    Timber.d("[SHARE] Map shared to account, ID: %d, URL: %s, Duplicate: %b", result.mapId, result.shareUrl, result.isDuplicate);
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(requireContext(), getString(R.string.share_failed, error), Toast.LENGTH_LONG).show();
                    Timber.e("[SHARE] API share failed: %s", error);
                }
            });
        } else {
            // Not logged in: open share URL in browser
            try {
                String encodedLevelText = URLEncoder.encode(levelText, "UTF-8");
                String shareUrl = "https://roboyard.z11.de/share_map?data=" + encodedLevelText;
                
                if (!TextUtils.isEmpty(mapName)) {
                    shareUrl += "&name=" + URLEncoder.encode(mapName, "UTF-8");
                }
                
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl));
                startActivity(intent);
                
                Toast.makeText(requireContext(), "Opening share URL in browser", Toast.LENGTH_SHORT).show();
                Timber.d("[SHARE] Sharing level with URL: %s", shareUrl);
                
            } catch (UnsupportedEncodingException e) {
                Timber.e(e, "[SHARE] Error encoding level text");
                Toast.makeText(requireContext(), "Error creating share URL", Toast.LENGTH_SHORT).show();
            } catch (ActivityNotFoundException e) {
                Timber.e(e, "[SHARE] No browser available to open URL");
                Toast.makeText(requireContext(), "No browser available to open URL", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private boolean levelExists(int levelId) {
        try {
            if (levelId <= 140) {
                // Check if built-in level exists
                InputStream is = requireContext().getAssets().open("Maps/level_" + levelId + ".txt");
                is.close();
                return true;
            } else {
                // Check if custom level exists in internal storage
                File file = new File(requireContext().getFilesDir(), "custom_level_" + levelId + ".txt");
                return file.exists();
            }
        } catch (IOException e) {
            return false;
        }
    }
    
    private void loadLevel(int levelId) {
        Timber.d("Loading level %d", levelId);
        
        // If loading level 0, create a new level
        if (levelId == 0) {
            createNewLevel(12, 14);
            Timber.d("Created new blank level with board size 12x14");
            return;
        }
        
        try {
            String levelFileName = String.format("level_%d.txt", levelId);
            Timber.d("Attempting to load level file: %s", levelFileName);
            
            // Try to load the level from internal storage first (custom levels)
            File levelFile = new File(requireContext().getFilesDir(), "level_" + levelId + ".txt");
            if (levelFile.exists()) {
                // Load the custom level from internal storage
                FileInputStream fis = new FileInputStream(levelFile);
                Scanner scanner = new Scanner(fis);
                StringBuilder content = new StringBuilder();
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine()).append("\n");
                }
                scanner.close();
                fis.close();
                
                // Parse the level content
                currentState = parseLevelContent(content.toString());
                Timber.d("Loaded custom level %d from internal storage with %d elements", 
                        levelId, currentState.getGameElements().size());
            } else {
                // Load the built-in level from assets
                String assetPath = "Maps/" + levelFileName;
                Timber.d("Loading built-in level from asset: %s", assetPath);
                
                InputStream inputStream = requireContext().getAssets().open(assetPath);
                Scanner scanner = new Scanner(inputStream);
                StringBuilder content = new StringBuilder();
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine()).append("\n");
                }
                scanner.close();
                inputStream.close();
                
                // Dump the content of the level file for debugging
                String levelContent = content.toString();
                Timber.d("Level content (first 100 chars): %s", 
                        levelContent.length() > 100 ? levelContent.substring(0, 100) : levelContent);
                
                // Parse the level content
                currentState = parseLevelContent(levelContent);
                Timber.d("Loaded built-in level %d from assets with %d elements, board size %dx%d", 
                        levelId, currentState.getGameElements().size(), 
                        currentState.getWidth(), currentState.getHeight());
            }
            
            currentLevelId = levelId;
            
            // Update UI with the loaded level
            EditText widthEdit = requireView().findViewById(R.id.board_width_edit_text);
            EditText heightEdit = requireView().findViewById(R.id.board_height_edit_text);
            widthEdit.setText(String.valueOf(currentState.getWidth()));
            heightEdit.setText(String.valueOf(currentState.getHeight()));
            
        } catch (IOException e) {
            Timber.e(e, "Error loading level %d: %s", levelId, e.getMessage());
            Toast.makeText(requireContext(), "Error loading level " + levelId, Toast.LENGTH_SHORT).show();
            
            // Create a new level as fallback
            createNewLevel(12, 14);
        }
        
        // Update the UI
        updateUI();
    }
    
    private void createNewLevel(int width, int height) {
        currentLevelId = 0;
        levelIdTextView.setText("New Level");
        
        // Create a new state with specified dimensions
        currentState = new GameState(width, height);
        
        // Update UI with board size
        boardWidthEditText.setText(String.valueOf(width));
        boardHeightEditText.setText(String.valueOf(height));
        
        // Create default border walls
        createBorderWalls(width, height);
        
        // Update the UI
        updateUI();
    }
    
    private void createBorderWalls(int width, int height) {
        // Add border walls to the level
        // Outer walls sit at the boundary: x=0..width-1 for horizontal, y=0..height-1 for vertical
        // Bottom wall row is at y=height, right wall column is at x=width
        for (int x = 0; x < width; x++) {
            // Top horizontal walls
            currentState.addHorizontalWall(x, 0);
            // Bottom horizontal walls
            currentState.addHorizontalWall(x, height);
        }
        
        for (int y = 0; y < height; y++) {
            // Left vertical walls
            currentState.addVerticalWall(0, y);
            // Right vertical walls
            currentState.addVerticalWall(width, y);
        }
    }
    
    private void updateBoardSize(int newWidth, int newHeight) {
        int oldWidth = currentState.getWidth();
        int oldHeight = currentState.getHeight();
        
        Timber.d("[LEVEL_EDITOR] Resizing board from %dx%d to %dx%d", oldWidth, oldHeight, newWidth, newHeight);
        
        // Step 1: Remove all outer walls (boundary walls)
        removeOuterWalls();
        
        // Step 2: Adjust content based on resize direction
        if (newWidth > oldWidth || newHeight > oldHeight) {
            // Vergrößerung: zentriere den alten Inhalt
            centerContent(oldWidth, oldHeight, newWidth, newHeight);
        } else if (newWidth < oldWidth || newHeight < oldHeight) {
            // Verkleinerung: trimme oben und rechts
            trimContent(oldWidth, oldHeight, newWidth, newHeight);
        }
        
        // Step 3: Create a new GameState with the new dimensions and copy content
        GameState newState = new GameState(newWidth, newHeight);
        
        // Copy game elements to the new state
        for (GameElement element : currentState.getGameElements()) {
            newState.getGameElements().add(element);
        }
        
        // Copy other properties
        newState.setLevelId(currentState.getLevelId());
        newState.setLevelName(currentState.getLevelName());
        
        currentState = newState;
        
        // Step 4: Add new outer walls
        createBorderWalls(newWidth, newHeight);
        
        Timber.d("[LEVEL_EDITOR] Board resized successfully. New element count: %d", currentState.getGameElements().size());
        
        // Update UI
        updateUI();
    }
    
    /**
     * Remove all outer walls (boundary walls at x=0, x=width, y=0, y=height)
     */
    private void removeOuterWalls() {
        int width = currentState.getWidth();
        int height = currentState.getHeight();
        
        List<GameElement> elementsToRemove = new ArrayList<>();
        
        for (GameElement element : currentState.getGameElements()) {
            int x = element.getX();
            int y = element.getY();
            
            // Check if this is an outer wall
            boolean isOuterWall = false;
            
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                // Top or bottom wall
                if (y == 0 || y == height) {
                    isOuterWall = true;
                }
            } else if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                // Left or right wall
                if (x == 0 || x == width) {
                    isOuterWall = true;
                }
            }
            
            if (isOuterWall) {
                elementsToRemove.add(element);
            }
        }
        
        // Remove outer walls
        for (GameElement element : elementsToRemove) {
            currentState.getGameElements().remove(element);
        }
        
        Timber.d("[LEVEL_EDITOR] Removed %d outer walls", elementsToRemove.size());
    }
    
    /**
     * Center content when board is enlarged
     */
    private void centerContent(int oldWidth, int oldHeight, int newWidth, int newHeight) {
        // Calculate offset to center the old content in the new board
        int offsetX = (newWidth - oldWidth) / 2;
        int offsetY = (newHeight - oldHeight) / 2;
        
        Timber.d("[LEVEL_EDITOR] Centering content with offset (%d, %d)", offsetX, offsetY);
        
        // Move all elements by the offset
        for (GameElement element : currentState.getGameElements()) {
            element.setX(element.getX() + offsetX);
            element.setY(element.getY() + offsetY);
        }
    }
    
    /**
     * Trim content when board is shrunk (remove elements from top and right)
     * Shifts content down-left to keep bottom-left corner intact
     */
    private void trimContent(int oldWidth, int oldHeight, int newWidth, int newHeight) {
        List<GameElement> elementsToRemove = new ArrayList<>();
        
        Timber.d("[LEVEL_EDITOR] Trimming content to fit %dx%d (removing from top and right)", newWidth, newHeight);
        
        // Calculate how much to shift content down-left to keep bottom-left corner
        int shiftX = oldWidth - newWidth;
        int shiftY = oldHeight - newHeight;
        
        for (GameElement element : currentState.getGameElements()) {
            // don't need to schift horizontally, it already cuts off on the right
            // element.setX(element.getX() - shiftX); this would cut off on the left

            // Shift all elements up
            element.setY(element.getY() - shiftY);
        }
        
        // Remove elements that now fall outside the new board bounds
        for (GameElement element : currentState.getGameElements()) {
            int x = element.getX();
            int y = element.getY();
            
            // Remove if outside new bounds
            if (x >= newWidth || y >= newHeight) {
                elementsToRemove.add(element);
                Timber.d("[LEVEL_EDITOR] Removing element at (%d, %d) - outside new bounds after shift", x, y);
            }
        }
        
        // Remove the elements
        for (GameElement element : elementsToRemove) {
            currentState.getGameElements().remove(element);
        }
        
        Timber.d("[LEVEL_EDITOR] Removed %d elements that didn't fit", elementsToRemove.size());
    }
    
    private void updateUI() {
        // Get the board preview container
        FrameLayout boardPreviewContainer = requireView().findViewById(R.id.board_preview_container);
        
        // Clear existing views except the placeholder text
        boardPreviewContainer.removeAllViews();
        
        // Create a new view to draw the board and elements
        GameBoardView boardView = new GameBoardView(requireContext());
        
        // Set layout parameters to make the board take up the entire container
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        boardView.setLayoutParams(params);
        
        // Enable hardwareAccelerated mode for better performance
        boardView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        Timber.d("Adding board view to container. Board dimensions: %dx%d", 
                currentState.getWidth(), currentState.getHeight());
        
        // Add the board view to the container
        boardPreviewContainer.addView(boardView);
        
        // Update level ID text
        TextView levelIdText = requireView().findViewById(R.id.level_id_text);
        if (currentLevelId > 0) {
            levelIdText.setText("Editing Level: " + currentLevelId);
        } else {
            levelIdText.setText("New Level");
        }
        
        Timber.d("Updated UI with current state: %d elements, board size: %dx%d", 
                currentState.getGameElements().size(),
                currentState.getWidth(),
                currentState.getHeight());
    }
    
    private String generateLevelText() {
        // Generate the level format text matching the standard level file format:
        // board:W,H; then mhX,Y; then mvX,Y; then target_colorX,Y; then robot_colorX,Y;
        // Sorted numerically (by X first, then Y) within each section
        StringBuilder sb = new StringBuilder();
        
        // Board dimensions
        sb.append("board:").append(currentState.getWidth()).append(",").append(currentState.getHeight()).append(";\n");
        
        // Collect elements by type with their coordinates for numeric sorting
        List<GameElement> horizontalWalls = new ArrayList<>();
        List<GameElement> verticalWalls = new ArrayList<>();
        List<GameElement> targets = new ArrayList<>();
        List<GameElement> robots = new ArrayList<>();
        
        for (GameElement element : currentState.getGameElements()) {
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                horizontalWalls.add(element);
            } else if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                verticalWalls.add(element);
            } else if (element.getType() == GameElement.TYPE_TARGET) {
                targets.add(element);
            } else if (element.getType() == GameElement.TYPE_ROBOT) {
                robots.add(element);
            }
        }
        
        // Sort numerically by X first, then Y
        java.util.Comparator<GameElement> numericSort = (a, b) -> {
            if (a.getX() != b.getX()) return Integer.compare(a.getX(), b.getX());
            return Integer.compare(a.getY(), b.getY());
        };
        java.util.Collections.sort(horizontalWalls, numericSort);
        java.util.Collections.sort(verticalWalls, numericSort);
        java.util.Collections.sort(targets, numericSort);
        java.util.Collections.sort(robots, numericSort);
        
        for (GameElement e : horizontalWalls) {
            sb.append("mh").append(e.getX()).append(",").append(e.getY()).append(";\n");
        }
        for (GameElement e : verticalWalls) {
            sb.append("mv").append(e.getX()).append(",").append(e.getY()).append(";\n");
        }
        for (GameElement e : targets) {
            sb.append("target_").append(getColorNameLower(e.getColor()))
              .append(e.getX()).append(",").append(e.getY()).append(";\n");
        }
        for (GameElement e : robots) {
            sb.append("robot_").append(getColorNameLower(e.getColor()))
              .append(e.getX()).append(",").append(e.getY()).append(";\n");
        }
        
        return sb.toString();
    }
    
    
    private void addRobotAt(int x, int y) {
        Timber.d("Adding robot at (%d, %d) with color %d", x, y, currentRobotColor);
        // Remove only robots and targets at this position, NOT walls
        removeRobotsAndTargetsAt(x, y);
        // Add a new robot
        currentState.addRobot(x, y, currentRobotColor);
        Toast.makeText(requireContext(), String.format("Added %s robot at (%d, %d)", 
            getColorName(currentRobotColor), x, y), Toast.LENGTH_SHORT).show();
            
        // Immediately update UI
        updateUI();
    }
    
    private void addTargetAt(int x, int y) {
        Timber.d("Adding target at (%d, %d) with color %d", x, y, currentTargetColor);
        // Remove only robots and targets at this position, NOT walls
        removeRobotsAndTargetsAt(x, y);
        // Add a new target
        currentState.addTarget(x, y, currentTargetColor);
        Toast.makeText(requireContext(), String.format("Added %s target at (%d, %d)", 
            getColorName(currentTargetColor), x, y), Toast.LENGTH_SHORT).show();
            
        // Immediately update UI
        updateUI();
    }
    
    private void addHorizontalWallAt(int x, int y) {
        Timber.d("Adding horizontal wall at (%d, %d)", x, y);
        // Check if there's already a wall at this position
        if (currentState.getCellType(x, y) == Constants.TYPE_HORIZONTAL_WALL) {
            // Remove it (toggle behavior)
            removeElementsAt(x, y);
            Toast.makeText(requireContext(), String.format("Removed horizontal wall at (%d, %d)", 
                x, y), Toast.LENGTH_SHORT).show();
        } else {
            // Add a new horizontal wall
            removeElementsAt(x, y);
            currentState.addHorizontalWall(x, y);
            Toast.makeText(requireContext(), String.format("Added horizontal wall at (%d, %d)", 
                x, y), Toast.LENGTH_SHORT).show();
        }
        
        // Immediately update UI
        updateUI();
    }
    
    private void addVerticalWallAt(int x, int y) {
        Timber.d("Adding vertical wall at (%d, %d)", x, y);
        // Check if there's already a wall at this position
        if (currentState.getCellType(x, y) == Constants.TYPE_VERTICAL_WALL) {
            // Remove it (toggle behavior)
            removeElementsAt(x, y);
            Toast.makeText(requireContext(), String.format("Removed vertical wall at (%d, %d)", 
                x, y), Toast.LENGTH_SHORT).show();
        } else {
            // Add a new vertical wall
            removeElementsAt(x, y);
            currentState.addVerticalWall(x, y);
            Toast.makeText(requireContext(), String.format("Added vertical wall at (%d, %d)", 
                x, y), Toast.LENGTH_SHORT).show();
        }
        
        // Immediately update UI
        updateUI();
    }
    
    private void eraseAt(int x, int y) {
        Timber.d("Erasing at (%d, %d)", x, y);
        if (removeElementsAt(x, y)) {
            Toast.makeText(requireContext(), String.format("Erased element at (%d, %d)", 
                x, y), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), String.format("Nothing to erase at (%d, %d)", 
                x, y), Toast.LENGTH_SHORT).show();
        }
        
        // Immediately update UI
        updateUI();
    }
    
    /**
     * Remove only robots and targets at the specified position, preserving walls
     */
    private void removeRobotsAndTargetsAt(int x, int y) {
        List<GameElement> elementsToRemove = new ArrayList<>();
        
        // Find and remove only robots and targets, NOT walls
        for (GameElement element : currentState.getGameElements()) {
            if (element.getX() == x && element.getY() == y) {
                if (element.getType() == GameElement.TYPE_ROBOT || element.getType() == GameElement.TYPE_TARGET) {
                    elementsToRemove.add(element);
                }
            }
        }
        
        // Remove the robots and targets
        for (GameElement element : elementsToRemove) {
            currentState.getGameElements().remove(element);
        }
    }
    
    private boolean removeElementsAt(int x, int y) {
        boolean removed = false;
        List<GameElement> elements = new ArrayList<>(currentState.getGameElements());
        
        // Create a new list without the elements at the specified position
        for (GameElement element : elements) {
            if (element.getX() == x && element.getY() == y) {
                currentState.getGameElements().remove(element);
                removed = true;
            }
        }
        
        // Also clear cell type if it was a wall or target
        if (currentState.getCellType(x, y) != 0) {
            currentState.setCellType(x, y, 0);
            removed = true;
        }
        
        return removed;
    }
    
    private String getColorName(int colorIndex) {
        switch (colorIndex) {
            case Constants.COLOR_PINK: return "Red";
            case Constants.COLOR_GREEN: return "Green";
            case Constants.COLOR_BLUE: return "Blue";
            case Constants.COLOR_YELLOW: return "Yellow";
            case Constants.COLOR_SILVER: return "Silver";
            default: return "Unknown";
        }
    }
    
    private String getColorNameLower(int colorIndex) {
        switch (colorIndex) {
            case Constants.COLOR_PINK: return "red";
            case Constants.COLOR_GREEN: return "green";
            case Constants.COLOR_BLUE: return "blue";
            case Constants.COLOR_YELLOW: return "yellow";
            case Constants.COLOR_SILVER: return "silver";
            default: return "unknown";
        }
    }
    
    private int getCurrentBoardWidth() {
        EditText widthEditText = requireView().findViewById(R.id.board_width_edit_text);
        String widthStr = widthEditText.getText().toString();
        try {
            return Integer.parseInt(widthStr);
        } catch (NumberFormatException e) {
            Timber.e(e, "Error parsing board width");
            return 12; // Default width
        }
    }
    
    private int getCurrentBoardHeight() {
        EditText heightEditText = requireView().findViewById(R.id.board_height_edit_text);
        String heightStr = heightEditText.getText().toString();
        try {
            return Integer.parseInt(heightStr);
        } catch (NumberFormatException e) {
            Timber.e(e, "Error parsing board height");
            return 14; // Default height
        }
    }
    
    private void handleBoardClick(int x, int y) {
        Timber.d("Handling board click at: (%d, %d), mode: %d", x, y, currentEditMode);
        
        // First check if the coordinates are valid
        if (x < 0 || y < 0 || x >= currentState.getWidth() || y >= currentState.getHeight()) {
            Timber.w("Board click coordinates out of bounds: (%d, %d)", x, y);
            return;
        }
        
        // Handle the click based on current edit mode
        switch (currentEditMode) {
            case EDIT_MODE_ROBOT:
                addRobotAt(x, y);
                break;
            case EDIT_MODE_TARGET:
                addTargetAt(x, y);
                break;
            case EDIT_MODE_WALL_H:
                addHorizontalWallAt(x, y);
                break;
            case EDIT_MODE_WALL_V:
                addVerticalWallAt(x, y);
                break;
            case EDIT_MODE_ERASE:
                eraseAt(x, y);
                break;
        }
        
        // Note: We don't need to call updateUI() here anymore because each element
        // placement method now calls updateUI() directly
    }
    
    private GameState parseLevelContent(String content) {
        Timber.d("Parsing level content: %d characters", content.length());
        // Delegate to the existing GameState.parseLevel which correctly handles
        // the board:W,H; mhX,Y; mvX,Y; target_colorX,Y; robot_colorX,Y; format
        return GameState.parseLevel(requireContext(), content, currentLevelId);
    }
    
    /**
     * Custom view to display the game board for editing
     */
    private class GameBoardView extends View {
        private final Paint robotPaint = new Paint();
        private final Paint targetPaint = new Paint();
        private final Paint wallPaint = new Paint();
        private final Paint gridPaint = new Paint();
        private final Paint textPaint = new Paint();
        private final Paint targetTextPaint = new Paint();
        private int cellSize = 0;
        
        public GameBoardView(Context context) {
            super(context);
            
            // Make view clickable and focusable to properly receive touch events
            setClickable(true);
            setFocusable(true);
            
            // Initialize paints
            robotPaint.setStyle(Paint.Style.FILL);
            targetPaint.setStyle(Paint.Style.FILL);
            wallPaint.setStyle(Paint.Style.FILL);
            wallPaint.setColor(Color.BLACK);
            
            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setColor(Color.LTGRAY);
            gridPaint.setStrokeWidth(1);
            
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(20);
            textPaint.setTextAlign(Paint.Align.CENTER);
            
            targetTextPaint.setColor(Color.BLACK);
            targetTextPaint.setTextSize(20);
            targetTextPaint.setTextAlign(Paint.Align.CENTER);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            int width = getWidth();
            int height = getHeight();
            int boardWidth = currentState.getWidth();
            int boardHeight = currentState.getHeight();
            
            // Calculate cell size based on available space and board dimensions
            int cellWidth = width / boardWidth;
            int cellHeight = height / boardHeight;
            cellSize = Math.min(cellWidth, cellHeight);
            
            // Center the board
            int offsetX = (width - (cellSize * boardWidth)) / 2;
            int offsetY = (height - (cellSize * boardHeight)) / 2;
            
            // Draw background
            canvas.drawColor(Color.WHITE);
            
            // Draw grid
            for (int x = 0; x <= boardWidth; x++) {
                canvas.drawLine(offsetX + x * cellSize, offsetY, 
                               offsetX + x * cellSize, offsetY + boardHeight * cellSize, gridPaint);
            }
            
            for (int y = 0; y <= boardHeight; y++) {
                canvas.drawLine(offsetX, offsetY + y * cellSize, 
                               offsetX + boardWidth * cellSize, offsetY + y * cellSize, gridPaint);
            }
            
            // Draw walls from GameElements directly to include boundary walls
            // (outer walls at x=boardWidth and y=boardHeight are outside the grid array)
            for (GameElement element : currentState.getGameElements()) {
                int ex = element.getX();
                int ey = element.getY();
                if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                    // Horizontal wall drawn as a bar along the top edge of cell (ex, ey)
                    canvas.drawRect(offsetX + ex * cellSize, offsetY + ey * cellSize - cellSize/8,
                                  offsetX + (ex+1) * cellSize, offsetY + ey * cellSize + cellSize/8, wallPaint);
                } else if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                    // Vertical wall drawn as a bar along the left edge of cell (ex, ey)
                    canvas.drawRect(offsetX + ex * cellSize - cellSize/8, offsetY + ey * cellSize,
                                  offsetX + ex * cellSize + cellSize/8, offsetY + (ey+1) * cellSize, wallPaint);
                }
            }
            
            // Draw game elements (robots and targets)
            for (GameElement element : currentState.getGameElements()) {
                int x = element.getX();
                int y = element.getY();
                int centerX = offsetX + (x * cellSize) + (cellSize / 2);
                int centerY = offsetY + (y * cellSize) + (cellSize / 2);
                int radius = cellSize / 3;
                
                // Get color based on the element's color index
                int colorValue = getColorForIndex(element.getColor());
                
                if (element.getType() == GameElement.TYPE_ROBOT) {
                    robotPaint.setColor(colorValue);
                    canvas.drawCircle(centerX, centerY, radius, robotPaint);
                    // Draw "R" label
                    canvas.drawText("R", centerX, centerY + radius/2, textPaint);
                } else if (element.getType() == GameElement.TYPE_TARGET) {
                    targetPaint.setColor(colorValue);
                    canvas.drawRect(centerX - radius, centerY - radius, 
                                 centerX + radius, centerY + radius, targetPaint);
                    // Draw "T" label with black text
                    canvas.drawText("T", centerX, centerY + radius/2, targetTextPaint);
                }
            }
        }
        
        private int getColorForIndex(int colorIndex) {
            switch (colorIndex) {
                case Constants.COLOR_PINK: return Color.RED;
                case Constants.COLOR_GREEN: return Color.GREEN;
                case Constants.COLOR_BLUE: return Color.BLUE;
                case Constants.COLOR_YELLOW: return Color.YELLOW;
                case Constants.COLOR_SILVER: return Color.GRAY;
                default: return Color.WHITE;
            }
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Log all touch events to help debug
                Timber.d("Touch event detected: %s at (%f, %f)", 
                       event.getAction() == MotionEvent.ACTION_DOWN ? "DOWN" : "UP",
                       event.getX(), event.getY());
                
                int width = getWidth();
                int height = getHeight();
                int boardWidth = currentState.getWidth();
                int boardHeight = currentState.getHeight();
                
                // Calculate cell size based on available space and board dimensions
                int cellWidth = width / boardWidth;
                int cellHeight = height / boardHeight;
                int cellSize = Math.min(cellWidth, cellHeight);
                
                // Calculate offsets (same as in onDraw)
                int offsetX = (width - (cellSize * boardWidth)) / 2;
                int offsetY = (height - (cellSize * boardHeight)) / 2;
                
                // Convert touch coordinates to board coordinates
                float touchX = event.getX();
                float touchY = event.getY();
                int boardX = (int)((touchX - offsetX) / cellSize);
                int boardY = (int)((touchY - offsetY) / cellSize);
                
                // Check if the touch is within board bounds
                if (boardX >= 0 && boardX < boardWidth && boardY >= 0 && boardY < boardHeight) {
                    Timber.d("Valid board touch at: (%d, %d)", boardX, boardY);
                    handleBoardClick(boardX, boardY);
                    invalidate(); // Redraw the view
                    return true; // Indicate the touch was handled
                } else {
                    Timber.d("Touch outside board bounds at: (%d, %d)", boardX, boardY);
                }
            }
            return super.onTouchEvent(event);
        }
    }
}
