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
import android.widget.Switch;
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

import roboyard.eclabs.Constants;
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
    private Button saveButton;
    private Button cancelButton;
    private Button exportButton;
    private TextView levelTextView;
    private EditText boardWidthEditText;
    private EditText boardHeightEditText;
    private Button applyBoardSizeButton;
    private RadioGroup robotColorRadioGroup;
    private RadioGroup targetColorRadioGroup;
    private Switch overwriteSwitch;
    
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

        // Initialize UI elements
        levelIdTextView = view.findViewById(R.id.level_id_text);
        editLevelSpinner = view.findViewById(R.id.edit_level_spinner);
        editModeRadioGroup = view.findViewById(R.id.edit_mode_radio_group);
        saveButton = view.findViewById(R.id.save_level_button);
        cancelButton = view.findViewById(R.id.cancel_button);
        exportButton = view.findViewById(R.id.export_level_button);
        levelTextView = view.findViewById(R.id.level_text_view);
        boardWidthEditText = view.findViewById(R.id.board_width_edit_text);
        boardHeightEditText = view.findViewById(R.id.board_height_edit_text);
        applyBoardSizeButton = view.findViewById(R.id.apply_board_size_button);
        robotColorRadioGroup = view.findViewById(R.id.robot_color_radio_group);
        targetColorRadioGroup = view.findViewById(R.id.target_color_radio_group);
        overwriteSwitch = view.findViewById(R.id.overwrite_switch);
        
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
        
        // Load the initial level
        if (currentLevelId == 0) {
            // Default to level 1 if no level was specified
            currentLevelId = 1;
            loadLevel(currentLevelId);
            // Update the spinner selection to match level 1
            editLevelSpinner.setSelection(1); // Second item (level 1)
        } else {
            loadLevel(currentLevelId);
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
                currentRobotColor = 0;
            } else if (checkedId == R.id.robot_green_radio) {
                currentRobotColor = 1;
            } else if (checkedId == R.id.robot_blue_radio) {
                currentRobotColor = 2;
            } else if (checkedId == R.id.robot_yellow_radio) {
                currentRobotColor = 3;
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
        // Populate spinner with all available levels
        List<String> levelOptions = new ArrayList<>();
        levelOptions.add("New Level");
        
        // Add existing levels
        try {
            String[] files = requireContext().getAssets().list("Maps");
            for (String file : files) {
                if (file.startsWith("level_") && file.endsWith(".txt")) {
                    int levelId = Integer.parseInt(file.substring(6, file.length() - 4));
                    levelOptions.add("Level " + levelId);
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
                    int levelId = Integer.parseInt(fileName.substring(13, fileName.length() - 4));
                    levelOptions.add("Custom Level " + levelId);
                }
            }
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
                
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Save button
        saveButton.setOnClickListener(v -> saveLevel());
        
        // Cancel button
        cancelButton.setOnClickListener(v -> {
            // Navigate back to level selection
            requireActivity().getSupportFragmentManager().popBackStack();
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
    
    private void shareLevelOnline(String levelText, String userName) {
        try {
            // URL encode the level text
            String encodedLevelText = URLEncoder.encode(levelText, "UTF-8");
            
            // Add user name to the data if provided
            if (!TextUtils.isEmpty(userName)) {
                encodedLevelText += "&name=" + URLEncoder.encode(userName, "UTF-8");
            }
            
            // Create the share URL
            String shareUrl = "https://roboyard.z11.de/share_map?data=" + encodedLevelText;
            
            // Create an intent to open the URL
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl));
            startActivity(intent);
            
            Toast.makeText(requireContext(), "Opening share URL in browser", Toast.LENGTH_SHORT).show();
            Timber.d("Sharing level with URL: %s", shareUrl);
            
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Error encoding level text");
            Toast.makeText(requireContext(), "Error creating share URL", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Timber.e(e, "No browser available to open URL");
            Toast.makeText(requireContext(), "No browser available to open URL", Toast.LENGTH_SHORT).show();
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
            String levelFileName = String.format("level%d.txt", levelId);
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
        for (int x = 0; x < width; x++) {
            // Top horizontal walls
            currentState.addHorizontalWall(x, 0);
            // Bottom horizontal walls
            currentState.addHorizontalWall(x, height - 1);
        }
        
        for (int y = 0; y < height; y++) {
            // Left vertical walls
            currentState.addVerticalWall(0, y);
            // Right vertical walls
            currentState.addVerticalWall(width - 1, y);
        }
    }
    
    private void updateBoardSize(int width, int height) {
        // Create a new level with the specified dimensions
        createNewLevel(width, height);
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
        // Generate the level format text based on the current game state
        StringBuilder sb = new StringBuilder();
        
        // Board dimensions
        sb.append("board:").append(currentState.getWidth()).append(",").append(currentState.getHeight()).append(";\n");
        
        // Add robots
        for (GameElement element : currentState.getGameElements()) {
            if (element.getType() == GameElement.TYPE_ROBOT) {
                sb.append("robot:").append(element.getX()).append(",")
                  .append(element.getY()).append(",")
                  .append(element.getColor()).append(";\n");
            }
        }
        
        // Add targets
        for (GameElement element : currentState.getGameElements()) {
            if (element.getType() == GameElement.TYPE_TARGET) {
                sb.append("target:").append(element.getX()).append(",")
                  .append(element.getY()).append(",")
                  .append(element.getColor()).append(";\n");
            }
        }
        
        // Add horizontal walls
        for (GameElement element : currentState.getGameElements()) {
            if (element.getType() == GameElement.TYPE_HORIZONTAL_WALL) {
                sb.append("mh:").append(element.getX()).append(",")
                  .append(element.getY()).append(";\n");
            }
        }
        
        // Add vertical walls
        for (GameElement element : currentState.getGameElements()) {
            if (element.getType() == GameElement.TYPE_VERTICAL_WALL) {
                sb.append("mv:").append(element.getX()).append(",")
                  .append(element.getY()).append(";\n");
            }
        }
        
        return sb.toString();
    }
    
    private void saveLevel() {
        if (currentLevelId == 0) {
            // This is a new level, find the next available custom level ID
            currentLevelId = FIRST_CUSTOM_LEVEL_ID;
            while (levelExists(currentLevelId) && currentLevelId <= MAX_CUSTOM_LEVEL_ID) {
                currentLevelId++;
            }
            
            if (currentLevelId > MAX_CUSTOM_LEVEL_ID) {
                Toast.makeText(requireContext(), "No available custom level slots", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (currentLevelId <= 140 && !overwriteSwitch.isChecked()) {
            // Can't overwrite built-in levels unless explicitly requested
            Toast.makeText(requireContext(), "Can't overwrite built-in levels", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Generate level text content
        String levelText = generateLevelText();
        
        try {
            // Save to internal storage with custom_level_ prefix for custom levels
            String fileName = currentLevelId <= 140 ? "level_" + currentLevelId + ".txt" : 
                                                 "custom_level_" + currentLevelId + ".txt";
            
            // Always save custom levels to internal storage
            File file = new File(requireContext().getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(levelText.getBytes());
            fos.close();
            
            Toast.makeText(requireContext(), "Level saved with ID: " + currentLevelId, Toast.LENGTH_SHORT).show();
            levelIdTextView.setText("Level ID: " + currentLevelId);
            
            // Refresh the spinner
            setupLevelSpinner();
            
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error saving level", Toast.LENGTH_SHORT).show();
            Timber.e(e, "Error saving level %d", currentLevelId);
        }
    }
    
    private void addRobotAt(int x, int y) {
        Timber.d("Adding robot at (%d, %d) with color %d", x, y, currentRobotColor);
        // Check if there's already something at this position and remove it
        removeElementsAt(x, y);
        // Add a new robot
        currentState.addRobot(x, y, currentRobotColor);
        Toast.makeText(requireContext(), String.format("Added %s robot at (%d, %d)", 
            getColorName(currentRobotColor), x, y), Toast.LENGTH_SHORT).show();
            
        // Immediately update UI
        updateUI();
    }
    
    private void addTargetAt(int x, int y) {
        Timber.d("Adding target at (%d, %d) with color %d", x, y, currentTargetColor);
        // Check if there's already something at this position and remove it
        removeElementsAt(x, y);
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
            case 0: return "Red";
            case 1: return "Green";
            case 2: return "Blue";
            case 3: return "Yellow";
            default: return "Unknown";
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
        
        // Split the content by lines
        String[] lines = content.split("\\n");
        
        if (lines.length < 1) {
            Timber.e("Invalid level format: empty content");
            return new GameState(12, 14); // Default to 12x14 board
        }
        
        // First line contains board dimensions
        String[] dimensions = lines[0].trim().split("\\s+");
        if (dimensions.length < 2) {
            Timber.e("Invalid level format: first line should have width and height");
            return new GameState(12, 14); // Default to 12x14 board
        }
        
        // Parse board dimensions
        int width = 12;
        int height = 14;
        try {
            width = Integer.parseInt(dimensions[0]);
            height = Integer.parseInt(dimensions[1]);
            Timber.d("Parsed board dimensions: %dx%d", width, height);
        } catch (NumberFormatException e) {
            Timber.e(e, "Error parsing board dimensions");
        }
        
        // Create a new GameState with the parsed dimensions
        GameState state = new GameState(width, height);
        
        // Keep track of how many elements we add
        int elementCount = 0;
        
        // Process remaining lines to add walls, robots, and targets
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("\\s+");
            if (parts.length < 3) continue;
            
            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                
                if (parts[0].startsWith("wall")) {
                    // Wall: wall h/v x y
                    if (parts[0].equals("wall_h")) {
                        state.setCellType(x, y, Constants.TYPE_HORIZONTAL_WALL);
                        Timber.d("Added horizontal wall at (%d, %d)", x, y);
                        elementCount++;
                    } else if (parts[0].equals("wall_v")) {
                        state.setCellType(x, y, Constants.TYPE_VERTICAL_WALL);
                        Timber.d("Added vertical wall at (%d, %d)", x, y);
                        elementCount++;
                    }
                } else if (parts[0].startsWith("robot") && parts.length >= 4) {
                    // Robot: robot colorIndex x y
                    int color = Integer.parseInt(parts[3]);
                    state.addRobot(x, y, color);
                    Timber.d("Added robot at (%d, %d) with color %d", x, y, color);
                    elementCount++;
                } else if (parts[0].startsWith("target") && parts.length >= 4) {
                    // Target: target colorIndex x y
                    int color = Integer.parseInt(parts[3]);
                    state.addTarget(x, y, color);
                    Timber.d("Added target at (%d, %d) with color %d", x, y, color);
                    elementCount++;
                }
            } catch (NumberFormatException e) {
                Timber.e(e, "Error parsing line: %s", line);
            } catch (Exception e) {
                Timber.e(e, "Error processing line: %s", line);
            }
        }
        
        Timber.d("Successfully parsed level content. Added %d elements to board size %dx%d", 
                elementCount, width, height);
        
        return state;
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
            
            // Draw walls
            for (int y = 0; y < boardHeight; y++) {
                for (int x = 0; x < boardWidth; x++) {
                    int cellType = currentState.getCellType(x, y);
                    
                    if (cellType == Constants.TYPE_HORIZONTAL_WALL) {
                        canvas.drawRect(offsetX + x * cellSize, offsetY + y * cellSize - cellSize/8, 
                                      offsetX + (x+1) * cellSize, offsetY + y * cellSize + cellSize/8, wallPaint);
                    } else if (cellType == Constants.TYPE_VERTICAL_WALL) {
                        canvas.drawRect(offsetX + x * cellSize - cellSize/8, offsetY + y * cellSize, 
                                      offsetX + x * cellSize + cellSize/8, offsetY + (y+1) * cellSize, wallPaint);
                    }
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
                    // Draw "T" label
                    canvas.drawText("T", centerX, centerY + radius/2, textPaint);
                }
            }
        }
        
        private int getColorForIndex(int colorIndex) {
            switch (colorIndex) {
                case 0: return Color.RED;
                case 1: return Color.GREEN;
                case 2: return Color.BLUE;
                case 3: return Color.YELLOW;
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
