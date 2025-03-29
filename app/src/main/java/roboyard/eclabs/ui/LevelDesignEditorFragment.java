package roboyard.eclabs.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import timber.log.Timber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import roboyard.eclabs.Constants;
import roboyard.eclabs.R;

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
        setupModeSelection();
        
        // Load initial level if editing existing
        if (currentLevelId > 0) {
            loadLevel(currentLevelId);
        } else {
            // Create blank level with default size
            createNewLevel(12, 14);
        }
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
    
    private void setupModeSelection() {
        // Set up radio buttons for edit modes
        RadioButton robotButton = requireView().findViewById(R.id.mode_robot_radio);
        RadioButton targetButton = requireView().findViewById(R.id.mode_target_radio);
        RadioButton wallHButton = requireView().findViewById(R.id.mode_wall_h_radio);
        RadioButton wallVButton = requireView().findViewById(R.id.mode_wall_v_radio);
        RadioButton eraseButton = requireView().findViewById(R.id.mode_erase_radio);
        
        robotButton.setChecked(true); // Default selection
        
        editModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.mode_robot_radio) {
                currentEditMode = EDIT_MODE_ROBOT;
                robotColorRadioGroup.setVisibility(View.VISIBLE);
                targetColorRadioGroup.setVisibility(View.GONE);
            } else if (checkedId == R.id.mode_target_radio) {
                currentEditMode = EDIT_MODE_TARGET;
                robotColorRadioGroup.setVisibility(View.GONE);
                targetColorRadioGroup.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.mode_wall_h_radio) {
                currentEditMode = EDIT_MODE_WALL_H;
                robotColorRadioGroup.setVisibility(View.GONE);
                targetColorRadioGroup.setVisibility(View.GONE);
            } else if (checkedId == R.id.mode_wall_v_radio) {
                currentEditMode = EDIT_MODE_WALL_V;
                robotColorRadioGroup.setVisibility(View.GONE);
                targetColorRadioGroup.setVisibility(View.GONE);
            } else if (checkedId == R.id.mode_erase_radio) {
                currentEditMode = EDIT_MODE_ERASE;
                robotColorRadioGroup.setVisibility(View.GONE);
                targetColorRadioGroup.setVisibility(View.GONE);
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
        // For simplicity, we'll be implementing this in the next phase
        Timber.d("Map touch events will be implemented separately");
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
        exportButton.setOnClickListener(v -> {
            // Show the level text format
            String levelText = generateLevelText();
            levelTextView.setText(levelText);
            levelTextView.setVisibility(View.VISIBLE);
        });
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
        currentLevelId = levelId;
        levelIdTextView.setText("Level ID: " + levelId);
        
        try {
            String levelContent;
            
            if (levelId <= 140) {
                // Load built-in level from assets
                InputStream is = requireContext().getAssets().open("Maps/level_" + levelId + ".txt");
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                levelContent = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                is.close();
            } else {
                // Load custom level from internal storage
                File file = new File(requireContext().getFilesDir(), "custom_level_" + levelId + ".txt");
                Scanner scanner = new Scanner(file).useDelimiter("\\A");
                levelContent = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
            }
            
            // Parse level content and update the editor
            parseLevelContent(levelContent);
            
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error loading level", Toast.LENGTH_SHORT).show();
            Timber.e(e, "Error loading level %d", levelId);
        }
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
    
    private void parseLevelContent(String content) {
        // Extract board dimensions and create new game state
        Pattern boardPattern = Pattern.compile("board:(\\d+),(\\d+);");
        Matcher boardMatcher = boardPattern.matcher(content);
        
        int width = 12; // Default
        int height = 14; // Default
        
        if (boardMatcher.find()) {
            width = Integer.parseInt(boardMatcher.group(1));
            height = Integer.parseInt(boardMatcher.group(2));
        }
        
        // Create a new state with these dimensions
        currentState = new GameState(width, height);
        
        // Update UI with board size
        boardWidthEditText.setText(String.valueOf(width));
        boardHeightEditText.setText(String.valueOf(height));
        
        // Parse robots
        // Format: robot:x,y,color;
        Pattern robotPattern = Pattern.compile("robot:(\\d+),(\\d+),(\\d+);");
        Matcher robotMatcher = robotPattern.matcher(content);
        
        while (robotMatcher.find()) {
            int x = Integer.parseInt(robotMatcher.group(1));
            int y = Integer.parseInt(robotMatcher.group(2));
            int color = Integer.parseInt(robotMatcher.group(3));
            currentState.addRobot(x, y, color);
        }
        
        // Parse targets
        // Format: target:x,y,color;
        Pattern targetPattern = Pattern.compile("target:(\\d+),(\\d+),(\\d+);");
        Matcher targetMatcher = targetPattern.matcher(content);
        
        while (targetMatcher.find()) {
            int x = Integer.parseInt(targetMatcher.group(1));
            int y = Integer.parseInt(targetMatcher.group(2));
            int color = Integer.parseInt(targetMatcher.group(3));
            currentState.addTarget(x, y, color);
        }
        
        // Parse horizontal walls
        // Format: mh:x,y;
        Pattern mhPattern = Pattern.compile("mh:(\\d+),(\\d+);");
        Matcher mhMatcher = mhPattern.matcher(content);
        
        while (mhMatcher.find()) {
            int x = Integer.parseInt(mhMatcher.group(1));
            int y = Integer.parseInt(mhMatcher.group(2));
            currentState.addHorizontalWall(x, y);
        }
        
        // Parse vertical walls
        // Format: mv:x,y;
        Pattern mvPattern = Pattern.compile("mv:(\\d+),(\\d+);");
        Matcher mvMatcher = mvPattern.matcher(content);
        
        while (mvMatcher.find()) {
            int x = Integer.parseInt(mvMatcher.group(1));
            int y = Integer.parseInt(mvMatcher.group(2));
            currentState.addVerticalWall(x, y);
        }
        
        // Update the UI
        updateUI();
    }
    
    private void updateUI() {
        // This would refresh the map display based on currentState
        // For the initial implementation we'll just show a message
        Toast.makeText(requireContext(), "Level loaded with " + 
                       currentState.getWidth() + "x" + currentState.getHeight() + " board", 
                       Toast.LENGTH_SHORT).show();
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
}
