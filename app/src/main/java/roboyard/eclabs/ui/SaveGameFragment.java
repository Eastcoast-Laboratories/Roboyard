package roboyard.eclabs.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedDispatcher;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import roboyard.logic.core.GameState;
import roboyard.eclabs.R;
import roboyard.eclabs.FileReadWrite;
import roboyard.logic.core.Constants;
import roboyard.logic.core.Preferences;
import roboyard.logic.core.GameHistoryEntry;
import roboyard.ui.components.GameStateManager;
import roboyard.eclabs.GameHistoryManager;
import roboyard.eclabs.RoboyardApiClient;
import timber.log.Timber;

/**
 * SaveGameFragment handles saving, loading, and viewing history of games.
 * Replaces the canvas-based SaveGameScreen with native UI components.
 */
public class SaveGameFragment extends BaseGameFragment {
    
    // Constants
    
    // UI components
    private TextView titleText;
    private TabLayout tabLayout;
    private RecyclerView saveSlotRecyclerView;
    private Button backButton;
    
    // Adapters
    private SaveSlotAdapter saveSlotAdapter;
    private HistoryAdapter historyAdapter;
    
    // Mode (save or load)
    private boolean saveMode;
    
    private GameStateManager gameStateManager;
    private final List<SaveSlotInfo> saveSlots = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Default to load mode
        saveMode = false;
        
        // Get arguments to determine if we're in save or load mode
        if (getArguments() != null) {
            // First check for a direct string mode parameter (used by MainMenuFragment)
            if (getArguments().containsKey("mode")) {
                String mode = getArguments().getString("mode", "load");
                saveMode = "save".equals(mode);
                Timber.d("SaveGameFragment: Got mode=%s from direct bundle, saveMode=%s", mode, saveMode);
            }
            // Then check for a direct boolean saveMode parameter
            else if (getArguments().containsKey("saveMode")) {
                saveMode = getArguments().getBoolean("saveMode", false);
                Timber.d("SaveGameFragment: Got saveMode=%s from direct bundle", saveMode);
            }
            // Don't attempt to use SafeGameFragmentArgs as it seems to be causing issues
            // This avoids the "Cannot resolve symbol 'SaveGameFragmentArgs'" error in the IDE
        }
        
        Timber.d("SaveGameFragment: Final saveMode=%s", saveMode);
        
        // Get the existing GameStateManager from ViewModelProvider instead of creating a new one
        gameStateManager = new ViewModelProvider(requireActivity()).get(GameStateManager.class);
        Timber.d("SaveGameFragment: Retrieved GameStateManager from ViewModelProvider");
        
        // Debug current game state
        GameState currentState = gameStateManager.getCurrentState().getValue();
        Timber.d("SaveGameFragment: Current GameState is %s", currentState != null ? "available" : "null");
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_save_game, container, false);
        
        // Set up UI elements
        titleText = view.findViewById(R.id.title_text);
        tabLayout = view.findViewById(R.id.tab_layout);
        saveSlotRecyclerView = view.findViewById(R.id.save_slot_recycler_view);
        backButton = view.findViewById(R.id.back_button);
        
        // Set up tabs
        setupTabs();
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set up back button
        backButton.setOnClickListener(v -> {
            try {
                Timber.d("Back button pressed in SaveGameFragment");
                
                // Get the OnBackPressedDispatcher and trigger back press
                OnBackPressedDispatcher dispatcher = getActivity().getOnBackPressedDispatcher();
                dispatcher.onBackPressed();
            
            } catch (Exception e) {
                Timber.e("Exception in back button handler: %s", e.getMessage());
                // Last resort fallback
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        
        return view;
    }
    
    /**
     * Update title based on the selected tab
     */
    private void updateTitle() {
        int tabPosition = tabLayout.getSelectedTabPosition();
        if (saveMode) {
            // Save mode
            if (tabPosition == 0) {
                // Save tab
                titleText.setText(getString(R.string.save_screen_title));
            } else {
                // History tab
                titleText.setText(getString(R.string.history_screen_title));
            }
        } else {
            // Load mode
            if (tabPosition == 0) {
                // Load tab
                titleText.setText(getString(R.string.load_screen_title));
            } else {
                // History tab
                titleText.setText(getString(R.string.history_screen_title));
            }
        }
    }
    
    /**
     * Set up the tabs for save/load/history
     */
    private void setupTabs() {
        // Clear existing tabs
        if (tabLayout != null) {
            tabLayout.removeAllTabs();
            
            // Add tabs based on mode
            if (saveMode) {
                // Save mode tabs: Save (0) and History (1)
                tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.save_tab_title)));
                tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.history_tab_title)));
                Timber.d("SaveGameFragment: Setting up tabs for SAVE mode");
            } else {
                // Load mode tabs: Load (0) and History (1)
                tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.load_tab_title)));
                tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.history_tab_title)));
                Timber.d("SaveGameFragment: Setting up tabs for LOAD mode");
            }
            
            // Set up tab selection listener
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    updateTabContent(tab.getPosition());
                    updateTitle();
                }
                
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // Not needed
                }
                
                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    // Not needed
                }
            });
            
            // Select the initial tab
            TabLayout.Tab tab = tabLayout.getTabAt(0);
            if (tab != null) {
                Timber.d("SaveGameFragment: Selecting initial tab: %s", saveMode ? "SAVE" : "LOAD");
                tab.select();
            }
        }
    }
    
    /**
     * Set up the RecyclerView for save slots or history entries
     */
    private void setupRecyclerView() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        saveSlotRecyclerView.setLayoutManager(layoutManager);
        
        // Create adapters
        saveSlotAdapter = new SaveSlotAdapter(requireContext(), saveSlots);
        historyAdapter = new HistoryAdapter();
        
        // Load data first
        loadSaveSlots();
        loadHistoryEntries();
        
        // Set initial adapter based on mode and current tab
        updateTabContent(tabLayout.getSelectedTabPosition());
        
        // Update title to match initial mode
        updateTitle();
    }

    /**
     * Helper method to find position of a slot by its ID
     */
    private int position(int slotId) {
        for (int i = 0; i < saveSlots.size(); i++) {
            if (saveSlots.get(i).getSlotId() == slotId) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Update content based on selected tab
     */
    private void updateTabContent(int tabPosition) {
        Timber.d("[SaveGameFragment] Updating tab content for position: %d", tabPosition);
        if (saveMode) {
            // Save mode tabs: Save (0) or History (1)
            if (tabPosition == 0) {
                // Save tab
                saveSlotRecyclerView.setAdapter(saveSlotAdapter);
                loadSaveSlots();
                Timber.d("[SaveGameFragment] Set adapter to saveSlotAdapter (Save mode)");
            } else {
                // History tab
                saveSlotRecyclerView.setAdapter(historyAdapter);
                loadHistoryEntries();
                Timber.d("[SaveGameFragment] Set adapter to historyAdapter (Save mode)");
            }
        } else {
            // Load mode tabs: Load (0) or History (1)
            if (tabPosition == 0) {
                // Load tab
                saveSlotRecyclerView.setAdapter(saveSlotAdapter);
                loadSaveSlots();
                Timber.d("[SaveGameFragment] Set adapter to saveSlotAdapter (Load mode)");
            } else {
                // History tab
                saveSlotRecyclerView.setAdapter(historyAdapter);
                loadHistoryEntries();
                Timber.d("[SaveGameFragment] Set adapter to historyAdapter (Load mode)");
            }
        }
    }
    
    /**
     * Helper class to hold metadata extracted from save data (DRY - used by both save slots and history)
     */
    private static class SaveDataMetadata {
        String mapName;
        String boardSize;
        String difficulty;
        String movesCount;
        String completionStatus;
        Bitmap minimap;
        int moves;
    }
    
    /**
     * Extract metadata from save data string (DRY - shared between save slots and history entries)
     */
    private SaveDataMetadata extractSaveMetadata(String saveData, String filePath) {
        SaveDataMetadata meta = new SaveDataMetadata();
        if (saveData == null || saveData.isEmpty()) return meta;
        
        // Extract map name
        Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
        if (metadata != null && metadata.containsKey("MAPNAME")) {
            meta.mapName = metadata.get("MAPNAME");
        }
        
        // Extract board size from #SIZE:width,height or WIDTH/HEIGHT lines
        Pattern sizePattern = Pattern.compile("(#SIZE:|SIZE:)(\\d+),(\\d+)");
        Matcher sizeMatcher = sizePattern.matcher(saveData);
        if (sizeMatcher.find()) {
            int width = Integer.parseInt(sizeMatcher.group(2));
            int height = Integer.parseInt(sizeMatcher.group(3));
            meta.boardSize = "Board: " + width + "\u00D7" + height;
        } else {
            Pattern widthPattern = Pattern.compile("WIDTH:(\\d+);");
            Pattern heightPattern = Pattern.compile("HEIGHT:(\\d+);");
            Matcher widthMatcher = widthPattern.matcher(saveData);
            Matcher heightMatcher = heightPattern.matcher(saveData);
            int width = 0, height = 0;
            if (widthMatcher.find()) width = Integer.parseInt(widthMatcher.group(1));
            if (heightMatcher.find()) height = Integer.parseInt(heightMatcher.group(1));
            if (width > 0 && height > 0) {
                meta.boardSize = "Board: " + width + "\u00D7" + height;
            }
        }
        
        // Extract difficulty
        Pattern difficultyPattern = Pattern.compile("(DIFFICULTY:|#DIFFICULTY:)([^;\\n]+)");
        Matcher difficultyMatcher = difficultyPattern.matcher(saveData);
        if (difficultyMatcher.find()) {
            String diffValue = difficultyMatcher.group(2);
            try {
                int difficultyInt = Integer.parseInt(diffValue.trim());
                meta.difficulty = difficultyIntToString(difficultyInt);
            } catch (NumberFormatException e) {
                meta.difficulty = diffValue.trim();
            }
        } else {
            meta.difficulty = difficultyIntToString(Constants.DIFFICULTY_BEGINNER);
        }
        
        // Extract move count
        Pattern movesPattern1 = Pattern.compile("\\|MOVES:(\\d+)");
        Pattern movesPattern2 = Pattern.compile("MOVES:(\\d+);");
        Pattern movesPattern3 = Pattern.compile("#MOVES:(\\d+)");
        Matcher movesMatcher1 = movesPattern1.matcher(saveData);
        Matcher movesMatcher2 = movesPattern2.matcher(saveData);
        Matcher movesMatcher3 = movesPattern3.matcher(saveData);
        
        meta.moves = 0;
        if (movesMatcher1.find()) {
            meta.moves = Integer.parseInt(movesMatcher1.group(1));
        } else if (movesMatcher2.find()) {
            meta.moves = Integer.parseInt(movesMatcher2.group(1));
        } else if (movesMatcher3.find()) {
            meta.moves = Integer.parseInt(movesMatcher3.group(1));
        }
        
        if (meta.moves > 0) {
            meta.movesCount = "Moves: " + meta.moves;
            
            // Extract completion status
            Pattern solvedPattern = Pattern.compile("(#SOLVED:|SOLVED:)(true|false)");
            Matcher solvedMatcher = solvedPattern.matcher(saveData);
            if (solvedMatcher.find()) {
                boolean solved = Boolean.parseBoolean(solvedMatcher.group(2));
                meta.completionStatus = solved ? "Completed" : "Incomplete";
            }
        }
        
        // Create minimap
        if (filePath != null) {
            try {
                meta.minimap = createMinimapFromPath(requireContext(), filePath, 100, 100);
            } catch (Exception e) {
                Timber.e(e, "Error creating minimap for %s", filePath);
            }
        }
        
        return meta;
    }
    
    /**
     * Load save slots from storage
     */
    private void loadSaveSlots() {
        List<SaveSlotInfo> newSaveSlots = new ArrayList<>();
        
        // Add auto-save slot with proper metadata check
        try {
            // Get autosave file path
            String autosavePath = FileReadWrite.getSaveGamePath(requireActivity(), 0);
            java.io.File autosaveFile = new java.io.File(autosavePath);
            
            if (autosaveFile.exists()) {
                // Load save data to extract metadata
                String saveData = FileReadWrite.loadAbsoluteData(autosavePath);
                String name = "Auto-save";
                Date date = new Date(autosaveFile.lastModified());
                Bitmap minimap = null;
                String boardSize = null;
                String difficulty = null;
                String movesCount = null;
                String completionStatus = null;
                
                // Dump the whole save data for debugging
                // Timber.d("[SAVEDATA] Autosave data dump: %s", saveData);
                
                // Extract metadata using shared method (DRY)
                if (saveData != null && !saveData.isEmpty()) {
                    SaveDataMetadata meta = extractSaveMetadata(saveData, autosavePath);
                    if (meta.mapName != null) {
                        name = meta.mapName + "    auto-save";
                    }
                    boardSize = meta.boardSize;
                    difficulty = meta.difficulty;
                    movesCount = meta.movesCount;
                    completionStatus = meta.completionStatus;
                    minimap = meta.minimap;
                }
                
                // Create the SaveSlotInfo with the save data included
                newSaveSlots.add(new SaveSlotInfo(0, name, date, minimap, boardSize, difficulty, movesCount, completionStatus, saveData));
            } else {
                // Empty autosave slot
                String emptySlotText = getString(R.string.autosave_tag) + " (" + getString(R.string.save_empty) + ")";
                newSaveSlots.add(new SaveSlotInfo(0, emptySlotText, null, null, null, null, null, null, null));
            }
        } catch (Exception e) {
            Timber.e(e, "Error loading autosave slot");
            newSaveSlots.add(new SaveSlotInfo(0, "Auto-save", null, null, null, null, null, null, null));
        }
        
        // Add regular save slots (1-34)
        for (int i = 1; i <= 34; i++) {
            try {
                // Get save file path
                String savePath = FileReadWrite.getSaveGamePath(requireActivity(), i);
                java.io.File saveFile = new java.io.File(savePath);
                
                if (saveFile.exists()) {
                    // Load save data to extract metadata
                    String saveData = FileReadWrite.loadAbsoluteData(savePath);
                    String name = "Slot " + i;
                    Date date = new Date(saveFile.lastModified());
                    Bitmap minimap = null;
                    String boardSize = null;
                    String difficulty = null;
                    String movesCount = null;
                    String completionStatus = null;
                    
                    // Extract metadata using shared method (DRY)
                    if (saveData != null && !saveData.isEmpty()) {
                        SaveDataMetadata meta = extractSaveMetadata(saveData, savePath);
                        if (meta.mapName != null) {
                            name = meta.mapName;
                        }
                        boardSize = meta.boardSize;
                        difficulty = meta.difficulty;
                        movesCount = meta.movesCount;
                        completionStatus = meta.completionStatus;
                        minimap = meta.minimap;
                    }
                    
                    // Add save slot with metadata
                    newSaveSlots.add(new SaveSlotInfo(i, name, date, minimap, boardSize, difficulty, movesCount, completionStatus, saveData));
                } else {
                    // Empty slot
                    newSaveSlots.add(new SaveSlotInfo(i, "Slot " + i + " (" + getString(R.string.empty_slot) + ")", null, null, null, null, null, null, null));
                }
            } catch (Exception e) {
                Timber.e(e, "Error loading save slot %d", i);
                newSaveSlots.add(new SaveSlotInfo(i, "Slot " + i + " (Error)", null, null, null, null, null, null, null));
            }
        }
        
        // Update the list and notify the adapter on the UI thread
        requireActivity().runOnUiThread(() -> {
            saveSlots.clear();
            saveSlots.addAll(newSaveSlots);
            
            // Only update if the adapter exists and is attached to the RecyclerView
            if (saveSlotAdapter != null) {
                saveSlotAdapter.notifyDataSetChanged();
                Timber.d("SaveSlotAdapter updated with %d slots", saveSlots.size());
            } else {
                Timber.e("SaveSlotAdapter is null, can't update data");
                saveSlotAdapter = new SaveSlotAdapter(requireContext(), saveSlots);
                saveSlotRecyclerView.setAdapter(saveSlotAdapter);
            }
        });
    }
    
    /**
     * Load history entries from storage
     */
    private void loadHistoryEntries() {
        try {
            // Load history entries
            List<GameHistoryEntry> historyEntries = GameHistoryManager.getHistoryEntries(requireActivity());
            
            // If we have history entries, add them to the adapter
            if (historyEntries != null && !historyEntries.isEmpty()) {
                List<HistoryEntry> entries = new ArrayList<>();
                for (GameHistoryEntry entry : historyEntries) {
                    String name = entry.getMapName();
                    int moves = entry.getMovesMade();
                    String mapPath = entry.getMapPath();
                    
                    // Resolve to absolute path for loadAbsoluteData
                    String absolutePath = mapPath;
                    if (mapPath != null && !mapPath.startsWith("/")) {
                        absolutePath = requireContext().getFileStreamPath(mapPath).getAbsolutePath();
                    }
                    
                    // Extract metadata using shared method (DRY - same as save slots)
                    String saveData = FileReadWrite.loadAbsoluteData(absolutePath);
                    SaveDataMetadata meta = extractSaveMetadata(saveData, absolutePath);
                    
                    // Use map name from metadata if entry name is missing
                    if ((name == null || name.isEmpty()) && meta.mapName != null) {
                        name = meta.mapName;
                    }
                    
                    HistoryEntry historyEntry = new HistoryEntry(name, new Date(entry.getTimestamp()), moves,
                            meta.boardSize, mapPath, meta.minimap, meta.difficulty, meta.completionStatus);
                    entries.add(historyEntry);
                }
                
                // Update the adapter
                historyAdapter.updateHistoryEntries(entries);
                historyAdapter.notifyDataSetChanged();
            } else {
                // No history entries - clear the adapter
                historyAdapter.updateHistoryEntries(new ArrayList<>());
            }
        } catch (Exception e) {
            Timber.e(e, "Error loading history entries");
        }
    }
    
    /**
     * Helper method to create a minimap bitmap from a map path
     */
    private Bitmap createMinimapFromPath(Context context, String mapPath, int width, int height) {
        // Try to load the save data from the file
        String saveData = FileReadWrite.loadAbsoluteData(mapPath);
        if (saveData != null && !saveData.isEmpty()) {
            try {
                // Parse the game state from the save data
                GameState gameState = GameState.parseFromSaveData(saveData, context);
                if (gameState != null) {
                    // Use the MinimapGenerator to create a proper minimap from the game state
                    return MinimapGenerator.getInstance().generateMinimap(context, gameState, width, height);
                }
            } catch (Exception e) {
                Timber.e(e, "Error creating minimap from save data");
            }
        }
        
        // Create a simple placeholder bitmap for the minimap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Fill with light blue background
        canvas.drawColor(Color.rgb(200, 230, 255));
        
        // Draw a simple grid pattern
        Paint paint = new Paint();
        paint.setColor(Color.rgb(150, 200, 240));
        paint.setStrokeWidth(2);
        
        // Draw grid lines
        for (int i = 0; i < width; i += width/10) {
            canvas.drawLine(i, 0, i, height, paint);
        }
        for (int i = 0; i < height; i += height/10) {
            canvas.drawLine(0, i, width, i, paint);
        }
        
        // Draw a robot icon (simple circle)
        paint.setColor(Color.rgb(255, 100, 100));
        canvas.drawCircle(width/3f, height/3f, width/15f, paint);
        
        // Draw a target icon (simple square)
        paint.setColor(Color.rgb(100, 255, 100));
        canvas.drawRect(width/2f, height/2f, width/2f + width/10f, height/2f + height/10f, paint);
        
        return bitmap;
    }
    
    /**
     * Refresh a specific save slot (called after saving a game)
     * This addresses the issue mentioned in the memory about minimaps not being displayed
     * @param slotId The save slot ID
     */
    private void refreshSaveSlot(int slotId) {
        Timber.d("[SAVEDATA] Refreshing save slot %d", slotId);
        try {
            // Get the save path for this slot
            String savePath = FileReadWrite.getSaveGamePath(requireActivity(), slotId);
            java.io.File saveFile = new java.io.File(savePath);
            
            if (saveFile.exists()) {
                // Get the current game state for metadata
                GameState currentState = gameStateManager.getCurrentState().getValue();
                String saveData = FileReadWrite.loadAbsoluteData(savePath);
                
                // Create a new SaveSlotInfo with updated information
                String name = "Save " + slotId;
                Date date = new Date(saveFile.lastModified());
                Bitmap minimap = null;
                String boardSize = null;
                String difficulty = null;
                String movesCount = null;
                String completionStatus = null;
                
                // Log save data for debugging
                Timber.d("[SAVEDATA] Refresh save data dump: %s", saveData);
                
                // Extract metadata from GameStateManager if available
                Map<String, String> metadata = GameStateManager.extractMetadataFromSaveData(saveData);
                if (metadata != null && metadata.containsKey("MAPNAME")) {
                    name = metadata.get("MAPNAME");
                    Timber.d("[SAVEDATA] Found map name: %s", name);
                }
                
                if (currentState != null) {
                    int width = currentState.getWidth();
                    int height = currentState.getHeight();
                    boardSize = "Board: " + width + "×" + height;
                    Timber.d("[SAVEDATA] Board size: %s", boardSize);
                    
                    int moves = currentState.getMoveCount();
                    movesCount = "Moves: " + moves;
                    Timber.d("[SAVEDATA] Moves count: %s", movesCount);
                    
                    boolean solved = currentState.isComplete();
                    completionStatus = solved ? "Completed" : "Incomplete";
                    Timber.d("[SAVEDATA] Completion status: %s", completionStatus);
                    
                    // Get difficulty from global preference as fallback
                    difficulty = difficultyIntToString(Constants.DIFFICULTY_BEGINNER);
                    Timber.d("[SAVEDATA] Using beginner difficulty: %s", difficulty);
                } else {
                    // Extract from save data using regex as fallback
                    
                    // Extract board size from #SIZE:width,height or WIDTH/HEIGHT lines
                    Pattern sizePattern = Pattern.compile("#SIZE:(\\d+),(\\d+)");
                    Matcher sizeMatcher = sizePattern.matcher(saveData);
                    int width = 0, height = 0;
                    
                    if (sizeMatcher.find()) {
                        width = Integer.parseInt(sizeMatcher.group(1));
                        height = Integer.parseInt(sizeMatcher.group(2));
                        Timber.d("[SAVEDATA] Found size in #SIZE tag: %dx%d", width, height);
                    } else {
                        // Try WIDTH/HEIGHT format
                        Pattern widthPattern = Pattern.compile("WIDTH:(\\d+);");
                        Pattern heightPattern = Pattern.compile("HEIGHT:(\\d+);");
                        Matcher widthMatcher = widthPattern.matcher(saveData);
                        Matcher heightMatcher = heightPattern.matcher(saveData);
                        
                        if (widthMatcher.find()) {
                            width = Integer.parseInt(widthMatcher.group(1));
                            Timber.d("[SAVEDATA] Found width: %d", width);
                        }
                        
                        if (heightMatcher.find()) {
                            height = Integer.parseInt(heightMatcher.group(1));
                            Timber.d("[SAVEDATA] Found height: %d", height);
                        }
                    }
                    
                    if (width > 0 && height > 0) {
                        boardSize = "Board: " + width + "×" + height;
                    }
                    
                    // Extract difficulty - first try a direct DIFFICULTY tag
                    Pattern difficultyPattern = Pattern.compile("DIFFICULTY:([^;\\n]+)|#DIFFICULTY:([^;\\n]+)");
                    Matcher difficultyMatcher = difficultyPattern.matcher(saveData);
                    if (difficultyMatcher.find()) {
                        // Group 1 or 2 might be non-null depending on which pattern matched
                        String diffValue = difficultyMatcher.group(1) != null ? difficultyMatcher.group(1) : difficultyMatcher.group(2);
                        try {
                            // Try parsing as integer first
                            int difficultyInt = Integer.parseInt(diffValue.trim());
                            difficulty = difficultyIntToString(difficultyInt);
                        } catch (NumberFormatException e) {
                            // If it's already a string value, use it directly
                            difficulty = diffValue.trim();
                        }
                        Timber.d("[SAVEDATA] Found difficulty in save data: %s", difficulty);
                    } else {
                        // If difficulty not found, use beginner difficulty as fallback
                        difficulty = difficultyIntToString(Constants.DIFFICULTY_BEGINNER);
                        Timber.d("[SAVEDATA] No difficulty tag found, using beginner difficulty: %s", difficulty);
                    }
                    
                    // Extract move count from various patterns
                    Pattern movesPattern = Pattern.compile("\\|MOVES:(\\d+)");
                    Matcher movesMatcher = movesPattern.matcher(saveData);
                    if (movesMatcher.find()) {
                        int moves = Integer.parseInt(movesMatcher.group(1));
                        movesCount = "Moves: " + moves;
                        Timber.d("[SAVEDATA] Found moves with pipe pattern: %d", moves);
                    } else if (metadata.containsKey("MOVES")) {
                        // Try metadata version
                        try {
                            int moves = Integer.parseInt(metadata.get("MOVES"));
                            movesCount = "Moves: " + moves;
                            Timber.d("[SAVEDATA] Found moves in metadata: %d", moves);
                        } catch (NumberFormatException e) {
                            Timber.e("Error parsing move count from metadata");
                        }
                    }
                    
                    // Extract completion status
                    Pattern solvedPattern = Pattern.compile("#SOLVED:(true|false)");
                    Matcher solvedMatcher = solvedPattern.matcher(saveData);
                    if (solvedMatcher.find()) {
                        boolean solved = Boolean.parseBoolean(solvedMatcher.group(1));
                        completionStatus = solved ? "Completed" : "Incomplete";
                        Timber.d("[SAVEDATA] Found solved status: %s", completionStatus);
                    } else if (metadata.containsKey("SOLVED")) {
                        boolean solved = Boolean.parseBoolean(metadata.get("SOLVED"));
                        completionStatus = solved ? "Completed" : "Incomplete";
                        Timber.d("[SAVEDATA] Found solved in metadata: %s", completionStatus);
                    } else {
                        // Default to incomplete
                        completionStatus = "Incomplete";
                    }
                }
                
                // Create minimap
                try {
                    minimap = createMinimapFromPath(requireContext(), savePath, 100, 100);
                } catch (Exception e) {
                    Timber.e(e, "Error creating minimap for slot %d", slotId);
                }
                
                // Create a new SaveSlotInfo
                SaveSlotInfo updatedSlot = new SaveSlotInfo(slotId, name, date, minimap, boardSize, difficulty, movesCount, completionStatus, saveData);
                
                // Find and update the slot in the list
                boolean slotFound = false;
                for (int i = 0; i < saveSlots.size(); i++) {
                    if (saveSlots.get(i).getSlotId() == slotId) {
                        saveSlots.set(i, updatedSlot);
                        slotFound = true;
                        break;
                    }
                }
                
                // If slot wasn't found, add it
                if (!slotFound) {
                    saveSlots.add(updatedSlot);
                }
                
                // Update the recycler view
                saveSlotAdapter.updateSaveSlots(saveSlots);
            }
        } catch (Exception e) {
            Timber.e(e, "Error refreshing save slot %d", slotId);
        }
    }
    
    /**
     * Convert difficulty integer to readable string
     * @param difficulty difficulty constant
     * @return readable difficulty string
     */
    private String difficultyIntToString(int difficulty) {
        // Get localized difficulty strings from resources
        switch (difficulty) {
            case Constants.DIFFICULTY_BEGINNER:
                return getString(R.string.difficulty_beginner);
            case Constants.DIFFICULTY_ADVANCED:
                return getString(R.string.difficulty_advanced);
            case Constants.DIFFICULTY_INSANE:
                return getString(R.string.difficulty_insane);
            case Constants.DIFFICULTY_IMPOSSIBLE:
                return getString(R.string.difficulty_impossible);
            default:
                return getString(R.string.difficulty_beginner);
        }
    }
    
    /**
     * Share a save slot via URL
     * @param slotId The save slot ID
     */
    private void shareSaveSlot(int slotId) {
        try {
            // Check if user is logged in - if so, offer to share directly to account
            RoboyardApiClient apiClient = RoboyardApiClient.getInstance(requireContext());
            if (apiClient.isLoggedIn()) {
                // Show dialog to choose between direct share and URL share
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.share_dialog_title)
                    .setMessage(getString(R.string.share_dialog_logged_in_message, apiClient.getUserName()))
                    .setPositiveButton(R.string.share_to_account, (dialog, which) -> shareToAccount(slotId))
                    .setNegativeButton(R.string.share_via_url, (dialog, which) -> shareViaUrl(slotId))
                    .setNeutralButton(R.string.button_cancel, null)
                    .show();
                return;
            }
            
            // Not logged in - share via URL
            shareViaUrl(slotId);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error sharing save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Timber.e(e, "[SHARE] Error sharing save slot %d", slotId);
        }
    }
    
    /**
     * Share a save slot directly to the user's roboyard.z11.de account
     * @param slotId The save slot ID
     */
    private void shareToAccount(int slotId) {
        try {
            // Build the map data string
            String mapData = buildMapDataForShare(slotId);
            if (mapData == null) {
                Toast.makeText(requireContext(), "No data to share", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Get map name from save slot
            String mapName = getMapNameFromSlot(slotId);
            
            // Show loading spinner overlay
            View loadingOverlay = showLoadingSpinner();
            
            // Share via API
            RoboyardApiClient.getInstance(requireContext()).shareMap(mapData, mapName, new RoboyardApiClient.ApiCallback<RoboyardApiClient.ShareResult>() {
                @Override
                public void onSuccess(RoboyardApiClient.ShareResult result) {
                    // Hide loading spinner
                    hideLoadingSpinner(loadingOverlay);
                    
                    if (result.isDuplicate) {
                        Toast.makeText(requireContext(), "Map already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.share_success, Toast.LENGTH_SHORT).show();
                    }
                    
                    // Open the share URL in browser with auto-login
                    String autoLoginUrl = RoboyardApiClient.getInstance(requireContext()).buildAutoLoginUrl(result.shareUrl);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(autoLoginUrl));
                    startActivity(intent);
                    
                    Timber.d("[SHARE] Map shared to account, ID: %d, URL: %s, Duplicate: %b", result.mapId, result.shareUrl, result.isDuplicate);
                }
                
                @Override
                public void onError(String error) {
                    // Hide loading spinner
                    hideLoadingSpinner(loadingOverlay);
                    
                    Toast.makeText(requireContext(), getString(R.string.share_failed, error), Toast.LENGTH_LONG).show();
                    Timber.e("[SHARE] API share failed: %s", error);
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Timber.e(e, "[SHARE] Error sharing to account");
        }
    }
    
    /**
     * Get map name from a save slot
     */
    private String getMapNameFromSlot(int slotId) {
        try {
            File savesDir = new File(requireContext().getFilesDir(), Constants.SAVE_DIRECTORY);
            String filename = Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION;
            File saveFile = new File(savesDir, filename);
            
            if (saveFile.exists()) {
                String saveData = FileReadWrite.loadAbsoluteData(saveFile.getAbsolutePath());
                if (saveData != null && !saveData.isEmpty()) {
                    String[] lines = saveData.split("\n");
                    if (lines.length > 0 && lines[0].startsWith("#")) {
                        String[] metadata = lines[0].substring(1).split(";");
                        for (String item : metadata) {
                            if (item.startsWith("MAPNAME:")) {
                                return item.substring("MAPNAME:".length());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "[SHARE] Error getting map name from slot %d", slotId);
        }
        return "Shared Map";
    }
    
    /**
     * Parse wall data from save file format and add to formatted data
     * Extracted to DRY principle - used by both shareToAccount and shareViaUrl
     */
    private void parseAndAddWalls(String[] lines, int width, int height, StringBuilder formattedData, Set<String> wallEntries) {
        boolean inWallsSection = false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            if (trimmedLine.equals("WALLS:")) {
                inWallsSection = true;
                continue;
            } else if (trimmedLine.equals("TARGET_SECTION:") || trimmedLine.equals("ROBOTS:") || trimmedLine.equals("BOARD:")) {
                inWallsSection = false;
                continue;
            }
            
            if (inWallsSection && !trimmedLine.isEmpty()) {
                String[] parts = trimmedLine.split(",");
                if (parts.length >= 3) {
                    try {
                        // Check if format is 'H,y,x' or 'V,y,x'
                        if (parts[0].equals("H") || parts[0].equals("V")) {
                            if (parts[1].matches("\\d+") && parts[2].matches("\\d+")) {
                                int y = Integer.parseInt(parts[1]);
                                int x = Integer.parseInt(parts[2].replace(";", ""));
                                
                                // Skip border walls
                                if (isBorderWall(x, y, width, height)) {
                                    continue;
                                }
                                
                                String wallEntry;
                                if ("H".equals(parts[0])) {
                                    wallEntry = "\nmh" + y + "," + x + ";";
                                } else {
                                    wallEntry = "\nmv" + y + "," + x + ";";
                                }
                                
                                if (wallEntries.add(wallEntry)) {
                                    formattedData.append(wallEntry);
                                }
                            }
                        } else {
                            // Try format 'x,y,direction'
                            if (parts[0].matches("\\d+") && parts[1].matches("\\d+")) {
                                int x = Integer.parseInt(parts[0]);
                                int y = Integer.parseInt(parts[1]);
                                String direction = parts[2].replace(";", "");
                                
                                // Skip border walls
                                if (isBorderWall(x, y, width, height)) {
                                    continue;
                                }
                                
                                String wallEntry;
                                if ("h".equals(direction)) {
                                    wallEntry = "\nmh" + y + "," + x + ";";
                                } else {
                                    wallEntry = "\nmv" + y + "," + x + ";";
                                }
                                
                                if (wallEntries.add(wallEntry)) {
                                    formattedData.append(wallEntry);
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        Timber.e(e, "[SHARE] Error parsing wall: %s", trimmedLine);
                    }
                }
            }
        }
    }
    
    /**
     * Build map data string for sharing - formats save data for API
     */
    private String buildMapDataForShare(int slotId) {
        try {
            File savesDir = new File(requireContext().getFilesDir(), Constants.SAVE_DIRECTORY);
            String filename = Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION;
            File saveFile = new File(savesDir, filename);
            
            if (!saveFile.exists()) {
                return null;
            }
            
            String saveData = FileReadWrite.loadAbsoluteData(saveFile.getAbsolutePath());
            if (saveData == null || saveData.isEmpty()) {
                return null;
            }
            
            // Parse and format the save data for API
            StringBuilder formattedData = new StringBuilder();
            String[] lines = saveData.split("\n");
            
            String mapName = "Shared Map";
            int width = 12;
            int height = 12;
            int numMoves = 0;
            
            // Extract metadata from first line
            if (lines.length > 0 && lines[0].startsWith("#")) {
                String[] metadata = lines[0].substring(1).split(";");
                for (String item : metadata) {
                    if (item.startsWith("MAPNAME:")) {
                        mapName = item.substring("MAPNAME:".length());
                    } else if (item.startsWith("SIZE:")) {
                        String[] size = item.substring("SIZE:".length()).split(",");
                        if (size.length == 2) {
                            width = Integer.parseInt(size[0]);
                            height = Integer.parseInt(size[1]);
                        }
                    } else if (item.startsWith("MOVES:")) {
                        try {
                            numMoves = Integer.parseInt(item.substring("MOVES:".length()));
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
            }
            
            // Start with name and board size
            formattedData.append("name:").append(mapName).append(";");
            formattedData.append("num_moves:").append(numMoves).append(";");
            formattedData.append("solution:board:").append(width).append(",").append(height).append(";");
            
            // Parse sections
            boolean inTargetsSection = false;
            boolean inRobotsSection = false;
            Set<String> targetEntries = new HashSet<>();
            Set<String> wallEntries = new HashSet<>();
            Set<String> robotEntries = new HashSet<>();
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.equals("TARGET_SECTION:")) {
                    inTargetsSection = true;
                    inRobotsSection = false;
                } else if (trimmedLine.equals("ROBOTS:")) {
                    inTargetsSection = false;
                    inRobotsSection = true;
                } else if (inTargetsSection && trimmedLine.startsWith("TARGET_SECTION:") && trimmedLine.length() > 15) {
                    String targetData = trimmedLine.substring("TARGET_SECTION:".length());
                    String[] parts = targetData.split(",");
                    if (parts.length >= 3 && parts[0].matches("\\d+") && parts[1].matches("\\d+") && parts[2].matches("-?\\d+")) {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int color = Integer.parseInt(parts[2].replace(";", ""));
                        String colorName = getRobotColorName(color);
                        String targetEntry = "\ntarget_" + colorName + x + "," + y + ";";
                        if (targetEntries.add(targetEntry)) {
                            formattedData.append(targetEntry);
                        }
                    }
                } else if (inRobotsSection && !trimmedLine.isEmpty() && !trimmedLine.equals("ROBOTS:")) {
                    String[] parts = trimmedLine.split(",");
                    if (parts.length >= 3 && parts[0].matches("\\d+") && parts[1].matches("\\d+") && parts[2].matches("\\d+")) {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int color = Integer.parseInt(parts[2].replace(";", ""));
                        String colorName = getRobotColorName(color);
                        String robotEntry = "\nrobot_" + colorName + x + "," + y + ";";
                        if (robotEntries.add(robotEntry)) {
                            formattedData.append(robotEntry);
                        }
                    }
                }
            }
            
            // Parse walls using shared method
            parseAndAddWalls(lines, width, height, formattedData, wallEntries);
            
            return formattedData.toString();
        } catch (Exception e) {
            Timber.e(e, "[SHARE] Error building map data for slot %d", slotId);
            return null;
        }
    }
    
    /**
     * Share a save slot via URL (original method)
     * @param slotId The save slot ID
     */
    private void shareViaUrl(int slotId) {
        try {
            // First try to load the game state and synchronize targets
            GameState gameState = null;
            try {
                gameState = GameState.loadSavedGame(requireContext(), slotId);
                if (gameState != null) {
                    // Synchronize targets to ensure board array and gameElements list are in sync
                    int syncedTargets = gameState.synchronizeTargets();
                    if (syncedTargets > 0) {
                        Timber.d("[SHARE] Synchronized %d targets before sharing", syncedTargets);
                    }
                }
            } catch (Exception e) {
                Throwable t = new Throwable();
                Timber.e(t, "[SHARE_ERROR] Failed to load game state for synchronization: %s", e.getMessage());
                // Continue with the sharing process even if synchronization fails
            }
            
            // Use the same path format as in GameState.loadSavedGame
            File savesDir = new File(requireContext().getFilesDir(), Constants.SAVE_DIRECTORY);
            String filename = Constants.SAVE_FILENAME_PREFIX + slotId + Constants.SAVE_FILENAME_EXTENSION;
            File saveFile = new File(savesDir, filename);
            String savePath = saveFile.getAbsolutePath();
            
            Timber.d("[SHARE] Attempting to load save from: %s (exists: %b)", savePath, saveFile.exists());
            
            if (saveFile.exists()) {
                // Load save data
                String saveData = FileReadWrite.loadAbsoluteData(savePath);
                
                if (saveData != null && !saveData.isEmpty()) {
                    Timber.d("[SHARE] Loaded save data, length: %d characters", saveData.length());
                    
                    // Manually extract map name and move count from the save data
                    String mapName = "Shared Map";
                    int moveCount = 0;
                    int optimalMoveCount = 5; // Default to 5 for testing if not found
                    int width = 16;
                    int height = 16;
                    
                    String[] lines = saveData.split("\n");
                    Timber.d("[SHARE] Save data has %d lines", lines.length);
                    
                    // Parse metadata from the first line if it starts with #
                    if (lines.length > 0 && lines[0].startsWith("#")) {
                        String metadataLine = lines[0];
                        Timber.d("[SHARE] Metadata line: %s", metadataLine);
                        String[] metadata = metadataLine.substring(1).split(";");
                        
                        for (String item : metadata) {
                            Timber.d("[SHARE] Metadata item: '%s'", item);
                            if (item.startsWith("MAPNAME:")) {
                                mapName = item.substring("MAPNAME:".length());
                                Timber.d("[SHARE] Found map name: %s", mapName);
                            } else if (item.startsWith("MOVES:")) {
                                try {
                                    moveCount = Integer.parseInt(item.substring("MOVES:".length()));
                                    Timber.d("[SHARE] Found move count: %d", moveCount);
                                } catch (NumberFormatException e) {
                                    Timber.e(e, "[SHARE] Error parsing move count");
                                }
                            } else if (item.startsWith("OPTIMAL:")) {
                                try {
                                    optimalMoveCount = Integer.parseInt(item.substring("OPTIMAL:".length()));
                                    Timber.d("[SHARE] Found optimal move count: %d", optimalMoveCount);
                                } catch (NumberFormatException e) {
                                    Timber.e(e, "[SHARE] Error parsing optimal move count");
                                }
                            }
                        }
                    }
                    
                    // Look for board dimensions
                    for (String line : lines) {
                        if (line.startsWith("WIDTH:")) {
                            try {
                                width = Integer.parseInt(line.substring("WIDTH:".length()).trim().replace(";", ""));
                                Timber.d("[SHARE] Found width: %d", width);
                            } catch (NumberFormatException e) {
                                Timber.e(e, "[SHARE] Error parsing width");
                            }
                        } else if (line.startsWith("HEIGHT:")) {
                            try {
                                height = Integer.parseInt(line.substring("HEIGHT:".length()).trim().replace(";", ""));
                                Timber.d("[SHARE] Found height: %d", height);
                            } catch (NumberFormatException e) {
                                Timber.e(e, "[SHARE] Error parsing height");
                            }
                        }
                    }
                    
                    // Build the data string in the format expected by the share system
                    StringBuilder formattedData = new StringBuilder();
                    formattedData.append("name:").append(mapName).append(";");
                    
                    // Use the optimal move count if available, otherwise use the current move count
                    int numMoves = optimalMoveCount > 0 ? optimalMoveCount : moveCount;
                    formattedData.append("num_moves:").append(numMoves).append(";");
                    formattedData.append("solution:board:").append(width).append(",").append(height).append(";");
                    
                    // Parse the board data more comprehensively
                    boolean inBoardSection = false;
                    boolean inRobotsSection = false;
                    boolean inWallsSection = false;
                    boolean inTargetsSection = false;
                    
                    int wallCount = 0;
                    int targetCount = 0;
                    int robotCount = 0;
                    
                    // Use sets to ensure we don't add duplicate entries
                    Set<String> wallEntries = new HashSet<>();
                    Set<String> targetEntries = new HashSet<>();
                    Set<String> robotEntries = new HashSet<>();
                    
                    // For board data, we need to parse walls and other elements
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].trim();
                        
                        // Skip empty lines
                        if (line.isEmpty()) {
                            continue;
                        }
                        
                        // Check for section markers
                        if (line.equals("BOARD:")) {
                            inBoardSection = true;
                            inRobotsSection = false;
                            inWallsSection = false;
                            inTargetsSection = false;
                            Timber.d("[SHARE] Found BOARD section");
                            continue;
                        } else if (line.equals("ROBOTS:")) {
                            inBoardSection = false;
                            inRobotsSection = true;
                            inWallsSection = false;
                            inTargetsSection = false;
                            Timber.d("[SHARE] Found ROBOTS section");
                            continue;
                        } else if (line.equals("WALLS:")) {
                            inBoardSection = false;
                            inRobotsSection = false;
                            inWallsSection = true;
                            inTargetsSection = false;
                            Timber.d("[SHARE] Found WALLS section");
                            continue;
                        } else if (line.equals("TARGET_SECTION:")) {
                            inBoardSection = false;
                            inRobotsSection = false;
                            inWallsSection = false;
                            inTargetsSection = true;
                            Timber.d("[SHARE] Found TARGET_SECTION section");
                            continue;
                        }
                        
                        try {
                            // Parse wall data
                            if (inWallsSection) {
                                // Format could be either:
                                // 1. 'H,y,x' or 'V,y,x' (H=horizontal, V=vertical)
                                // 2. 'x,y,direction' (direction = 'h' or 'v')
                                String[] parts = line.split(",");
                                if (parts.length >= 3) {
                                    // Check if the first part is a direction letter (H or V)
                                    if (parts[0].equals("H") || parts[0].equals("V")) {
                                        // Format is 'H,y,x' or 'V,y,x'
                                        String direction = parts[0];
                                        // Validate that parts[1] and parts[2] are numbers
                                        if (parts[1].matches("\\d+") && parts[2].matches("\\d+")) {
                                            int y = Integer.parseInt(parts[1]);
                                            int x = Integer.parseInt(parts[2].replace(";", ""));
                                            
                                            // Skip border walls (x=0, y=0, x=width-1, y=height-1)
                                            if (isBorderWall(x, y, width, height)) {
                                                continue;
                                            }
                                            
                                            String wallEntry;
                                            if ("H".equals(direction)) {
                                                wallEntry = "\nmh" + y + "," + x + ";";
                                            } else { // "V".equals(direction)
                                                wallEntry = "\nmv" + y + "," + x + ";";
                                            }
                                            
                                            // Only add if not already added
                                            if (wallEntries.add(wallEntry)) {
                                                formattedData.append(wallEntry);
                                                wallCount++;
                                            }
                                        } else {
                                            Timber.e("[SHARE] Invalid wall coordinates: %s", line);
                                        }
                                    } else {
                                        // Try the format 'x,y,direction'
                                        // Validate that parts[0] and parts[1] are numbers
                                        if (parts[0].matches("\\d+") && parts[1].matches("\\d+")) {
                                            int x = Integer.parseInt(parts[0]);
                                            int y = Integer.parseInt(parts[1]);
                                            String direction = parts[2].replace(";", "");
                                            
                                            // Skip border walls (x=0, y=0, x=width-1, y=height-1)
                                            if (isBorderWall(x, y, width, height)) {
                                                continue;
                                            }
                                            
                                            String wallEntry;
                                            if ("h".equals(direction)) {
                                                wallEntry = "\nmh" + y + "," + x + ";";
                                            } else { // "v".equals(direction)
                                                wallEntry = "\nmv" + y + "," + x + ";";
                                            }
                                            
                                            // Only add if not already added
                                            if (wallEntries.add(wallEntry)) {
                                                formattedData.append(wallEntry);
                                                wallCount++;
                                            }
                                        } else {
                                            Timber.e("[SHARE] Invalid wall coordinates: %s", line);
                                        }
                                    }
                                }
                            }
                            // Parse target data
                            else if (inTargetsSection && line.startsWith("TARGET_SECTION:") && line.length() > 15) {
                                // Format: TARGET_SECTION:x,y,color
                                try {
                                    String targetData = line.substring("TARGET_SECTION:".length());
                                    String[] parts = targetData.split(",");
                                    if (parts.length >= 3) {
                                        // Validate that all parts are numbers (allow negative for color -1 = multicolor)
                                        if (parts[0].matches("\\d+") && parts[1].matches("\\d+") && parts[2].matches("-?\\d+")) {
                                            int x = Integer.parseInt(parts[0]);
                                            int y = Integer.parseInt(parts[1]);
                                            int color = Integer.parseInt(parts[2].replace(";", ""));
                                            
                                            // Map color code to color name (-1 = multicolor target)
                                            String colorName = getRobotColorName(color);
                                            String targetEntry = "\ntarget_" + colorName + x + "," + y + ";";
                                            
                                            // Only add if not already added
                                            if (targetEntries.add(targetEntry)) {
                                                formattedData.append(targetEntry);
                                                targetCount++;
                                                Timber.d("[SHARE] Added target at (%d,%d) with color %d (%s)", x, y, color, colorName);
                                            }
                                        } else {
                                            Timber.e("[SHARE] Invalid target data format: %s", targetData);
                                        }
                                    } else {
                                        Timber.e("[SHARE] Insufficient target data parts in: %s", targetData);
                                    }
                                } catch (Exception e) {
                                    Timber.e(e, "[SHARE] Error parsing target line: %s", line);
                                }
                                continue;
                            }
                            // Parse robot data
                            else if (inRobotsSection) {
                                // Format: x,y,color
                                String[] parts = line.split(",");
                                if (parts.length >= 3) {
                                    // Validate that all parts are numbers
                                    if (parts[0].matches("\\d+") && parts[1].matches("\\d+") && parts[2].matches("\\d+")) {
                                        int x = Integer.parseInt(parts[0]);
                                        int y = Integer.parseInt(parts[1]);
                                        int color = Integer.parseInt(parts[2].replace(";", ""));
                                        
                                        // Map color code to color name
                                        String colorName = getRobotColorName(color);
                                        String robotEntry = "\nrobot_" + colorName + x + "," + y + ";";
                                        
                                        // Only add if not already added
                                        if (robotEntries.add(robotEntry)) {
                                            formattedData.append(robotEntry);
                                            robotCount++;
                                        }
                                    } else {
                                        Timber.e("[SHARE] Invalid robot coordinates or color: %s", line);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Timber.e(e, "[SHARE] Error parsing line: %s", line);
                        }
                    }
                    
                    Timber.d("[SHARE] Parsed %d walls, %d targets, %d robots", wallCount, targetCount, robotCount);
                    
                    // Final check for targets
                    if (targetCount == 0) {
                        Throwable t = new Throwable();
                        Timber.e(t, "[SHARE_ERROR] No targets found in save data, cannot share");
                        // Show a toast message instead of creating a fake target
                        Toast.makeText(requireContext(), 
                            "Cannot share - no target data found in save file", 
                            Toast.LENGTH_LONG).show();
                        // Add comprehensive logging to help debug the issue
                        Timber.e("[SHARE_ERROR] Could not share save slot %d - no targets found", slotId);
                        Timber.e("[SHARE_ERROR] Save data contains %d lines", lines.length);
                        Timber.e("[SHARE_ERROR] Map name: %s, Width: %d, Height: %d", mapName, width, height);
                        Timber.e("[SHARE_ERROR] Robot count: %d, Wall count: %d", robotCount, wallCount);
                        
                        // Log the first 10 lines of save data to help diagnose issues
                        int linesToLog = Math.min(lines.length, 10);
                        for (int i = 0; i < linesToLog; i++) {
                            Timber.d("[SHARE_ERROR] Line %d: %s", i, lines[i]);
                        }
                        
                        return; // Don't continue with sharing
                    }
                    
                    // URL encode the formatted data
                    String encodedData;
                    try {
                        encodedData = URLEncoder.encode(formattedData.toString(), "UTF-8");
                        Timber.d("[SHARE] Encoded data length: %d chars", encodedData.length());
                        // Log the first 100 chars of the encoded data to help diagnose issues
                        Timber.d("[SHARE] Full encoded data: %s", encodedData);
                    } catch (Exception e) {
                        Timber.e(e, "[SHARE] Error encoding data");
                        return;
                    }
                    
                    // Create the share URL
                    String shareUrl = "https://roboyard.z11.de/share_map?data=" + encodedData;
                    
                    // Log the full URL for debugging - use a separate log entry for the clickable URL
                    Timber.d("[SHARE] Share URL: %s", shareUrl);  // Log only the URL to make it clickable in the console
                    
                    // Wrap with auto-login if user is logged in
                    String finalUrl = RoboyardApiClient.getInstance(requireContext()).buildAutoLoginUrl(shareUrl);
                    
                    // Create an intent to open the URL
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
                    startActivity(intent);
                    
                    Toast.makeText(requireContext(), "Opening share URL in browser", Toast.LENGTH_SHORT).show();
                    Timber.d("[SHARE] Sharing save slot %d with URL", slotId);
                } else {
                    Toast.makeText(requireContext(), "No data to share", Toast.LENGTH_SHORT).show();
                    Timber.e("[SHARE] Empty save data");
                }
            } else {
                Toast.makeText(requireContext(), "Save file does not exist", Toast.LENGTH_SHORT).show();
                Timber.d("[SHARE] Save file does not exist at path: %s", savePath);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error sharing save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Timber.e(e, "[SHARE] Error sharing save slot %d", slotId);
        }
    }
    
    /**
     * Check if a wall is a border wall
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Board width
     * @param height Board height
     * @return true if the wall is a border wall
     */
    private boolean isBorderWall(int x, int y, int width, int height) {
        // Note: In Roboyard, walls are indexed starting at -1
        // Walls at x=0 or y=0 are NOT border walls, they are valid game elements
        // Only consider walls at the absolute edge (which would be at -1 if walls were indexed properly)
        // Since we don't have access to -1 coordinates here, we don't filter any walls
        return false; // Don't filter out any walls - we need them all
    }
    
    /**
     * Get the color name for a robot color
     * @param color Color constant
     * @return Color name for URL
     */
    private String getRobotColorName(int color) {
        switch (color) {
            case -1: // Multicolor target (any robot can reach it)
                return "multi";
            case 0: // Constants.COLOR_PINK
                return "pink";
            case 1: // Constants.COLOR_GREEN
                return "green";
            case 2: // Constants.COLOR_BLUE
                return "blue";
            case 3: // Constants.COLOR_YELLOW
                return "yellow";
            case 4: // Constants.COLOR_SILVER
                return "silver";
            case 5: // Constants.COLOR_RED
                return "red";
            case 6: // Constants.COLOR_BROWN
                return "brown";
            case 7: // Constants.COLOR_ORANGE
                return "orange"; 
            case 8: // Constants.COLOR_WHITE
                return "white";
            case 9: // Constants.COLOR_MULTI (for multi target)
                return "multi";
            default:
                return "unknown";
        }
    }
    
    @Override
    public String getScreenTitle() {
        return saveMode ? "Save Game" : "Load Game";
    }
    
    /**
     * SaveSlotAdapter for displaying save game slots
     */
    private class SaveSlotAdapter extends RecyclerView.Adapter<SaveSlotViewHolder> {
        private final List<SaveSlotInfo> saveSlots;
        private final SimpleDateFormat dateFormat;

        public SaveSlotAdapter(Context context, List<SaveSlotInfo> saveSlots) {
            this.saveSlots = saveSlots;
            this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US); // Updated date format
        }

        @NonNull
        @Override
        public SaveSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_save_slot, parent, false);
            return new SaveSlotViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SaveSlotViewHolder holder, int position) {
            SaveSlotInfo slot = saveSlots.get(position);
            
            // Set the name with a red cross indicator if no targets
            String nameText = slot.getName();
            String saveData = slot.getSaveData();
            if (saveData != null && !hasTargets(saveData)) {
                // Add a red cross indicator for saves without targets
                nameText += " ❌";
                // Set text color to red for visual indication
                holder.nameText.setTextColor(Color.RED);
                
                // Add accessibility information
                String errorDescription = getString(R.string.save_no_targets_a11y);
                holder.nameText.setContentDescription(nameText + ". " + errorDescription);
                
                Timber.d("[TARGET_CHECK] Adding red cross to save slot %d - no targets found", slot.getSlotId());
            } else {
                // leave default
            }
            holder.nameText.setText(nameText);
            
            // Set date with updated format
            String dateStr = slot.getDate() != null ? dateFormat.format(slot.getDate()) : "";
            holder.dateText.setText(dateStr);
            
            // Set metadata fields - each can be null so check first
            // Board size
            if (slot.getBoardSize() != null && !slot.getBoardSize().isEmpty()) {
                holder.boardSizeText.setVisibility(View.VISIBLE);
                holder.boardSizeText.setText(slot.getBoardSize());
            } else {
                holder.boardSizeText.setVisibility(View.GONE);
            }
            
            // Difficulty
            if (slot.getDifficulty() != null && !slot.getDifficulty().isEmpty()) {
                holder.difficultyText.setVisibility(View.VISIBLE);
                holder.difficultyText.setText(slot.getDifficulty());
            } else {
                holder.difficultyText.setVisibility(View.GONE);
            }
            
            // Moves count
            if (slot.getMovesCount() != null && !slot.getMovesCount().isEmpty()) {
                holder.movesText.setVisibility(View.VISIBLE);
                holder.movesText.setText(slot.getMovesCount());
            } else {
                holder.movesText.setVisibility(View.GONE);
            }
            
            // Completion status
            if (slot.getMovesCount() != null && !slot.getMovesCount().isEmpty() && slot.getCompletionStatus() != null && !slot.getCompletionStatus().isEmpty()) {
                holder.completionStatus.setVisibility(View.VISIBLE);
                holder.completionStatus.setText(slot.getCompletionStatus());
            } else {
                holder.completionStatus.setVisibility(View.GONE);
            }
            
            // Set minimap
            if (slot.getMinimap() != null) {
                holder.minimapView.setImageBitmap(slot.getMinimap());
                holder.minimapView.setVisibility(View.VISIBLE);
            } else {
                holder.minimapView.setVisibility(View.GONE);
            }
            
            // Set up click listener to load or save a game on touch
            holder.itemView.setOnClickListener(v -> {
                int slotId = slot.getSlotId();
                
                if (saveMode) {
                    // Slot 0 is reserved for auto-save only - prevent manual saves
                    if (slotId == 0) {
                        Toast.makeText(requireContext(), "Slot 0 is reserved for auto-save. Please use another slot.", Toast.LENGTH_SHORT).show();
                        Timber.d("[SAVE_PROTECTION] User attempted to save to slot 0 - blocked");
                        return;
                    }
                    
                    // Save current game to this slot
                    Timber.d("Saving game to slot " + slotId);
                    if (gameStateManager.saveGame(slotId)) {
                        Toast.makeText(requireContext(), "Game saved to slot " + slotId, Toast.LENGTH_SHORT).show();
                        // Refresh the slot to update minimap
                        refreshSaveSlot(slotId);
                        
                        // Switch to load mode after successful save
                        saveMode = false;
                        titleText.setText("Load game");
                        
                        // Rebuild tabs for load mode
                        setupTabs();
                        
                        // Select the first tab (Load tab) in load mode
                        if (tabLayout != null && tabLayout.getTabCount() > 0) {
                            TabLayout.Tab loadTab = tabLayout.getTabAt(0);
                            if (loadTab != null) {
                                loadTab.select();
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to save game", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (slot.getDate() != null) { // Only load if slot has a save
                        // Load the game first
                        gameStateManager.loadGame(slotId);
                        
                        // Verify that the game state was loaded successfully
                        if (gameStateManager.getCurrentState().getValue() != null) {
                            // Set UI mode to modern
                            UIModeManager.getInstance(requireContext()).setUIMode(UIModeManager.MODE_MODERN);
                            
                            // Navigate to the modern game fragment
                            ModernGameFragment gameFragment = new ModernGameFragment();
                            navigateToDirect(gameFragment);
                        } else {
                            // Show error if game state couldn't be loaded
                            Toast.makeText(requireContext(), "Error loading saved game", Toast.LENGTH_SHORT).show();
                            Timber.e("Failed to load game state from slot %d", slot.getSlotId());
                        }
                    } else {
                        Toast.makeText(requireContext(), "No saved game in this slot", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            
            // Set up share button
            holder.shareButton.setOnClickListener(v -> {
                // Share the save slot via URL
                shareSaveSlot(slot.getSlotId());
            });
            
            // Set content description for accessibility
            if (slot.getDate() != null) {
                StringBuilder contentDesc = new StringBuilder(slot.getName()).append(", ");
                if (slot.getBoardSize() != null) contentDesc.append(slot.getBoardSize()).append(", ");
                if (slot.getDifficulty() != null) contentDesc.append(requireContext().getString(R.string.level_difficulty)).append(": ").append(slot.getDifficulty()).append(", ");
                if (slot.getMovesCount() != null) contentDesc.append(slot.getMovesCount()).append(", ");
                if (slot.getCompletionStatus() != null) contentDesc.append(slot.getCompletionStatus()).append(", ");
                contentDesc.append(requireContext().getString(R.string.saved_on)).append(" ").append(dateFormat.format(slot.getDate()));
                
                holder.itemView.setContentDescription(contentDesc.toString());
                holder.shareButton.setContentDescription(requireContext().getString(R.string.share_a11y) + " " + slot.getName());
            } else {
                holder.itemView.setContentDescription(slot.getName() + ", " + requireContext().getString(R.string.empty_slot));
                holder.shareButton.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return saveSlots.size();
        }

        public void updateSaveSlots(List<SaveSlotInfo> saveSlots) {
            this.saveSlots.clear();
            this.saveSlots.addAll(saveSlots);
            notifyDataSetChanged();
        }
    }
    
    /**
     * HistoryAdapter for displaying game history entries
     */
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {
        private List<HistoryEntry> historyEntries = new ArrayList<>();
        
        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_entry, parent, false);
            return new HistoryViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            HistoryEntry entry = historyEntries.get(position);
            
            // Set history entry info
            holder.nameText.setText(entry.getName());
            
            // Set date with updated format
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            holder.dateText.setText(sdf.format(entry.getDate()));
            
            // Set moves
            holder.movesText.setText(entry.getMoves() + " moves");
            
            // Set board size (DRY - same metadata as save slots)
            if (entry.getBoardSize() != null && !entry.getBoardSize().isEmpty()) {
                holder.boardSizeText.setVisibility(View.VISIBLE);
                holder.boardSizeText.setText(entry.getBoardSize());
            } else {
                holder.boardSizeText.setVisibility(View.GONE);
            }
            
            // Set difficulty (DRY - same metadata as save slots)
            if (entry.getDifficulty() != null && !entry.getDifficulty().isEmpty()) {
                holder.difficultyText.setVisibility(View.VISIBLE);
                holder.difficultyText.setText(entry.getDifficulty());
            } else {
                holder.difficultyText.setVisibility(View.GONE);
            }
            
            // Set completion status (DRY - same metadata as save slots)
            if (entry.getCompletionStatus() != null && !entry.getCompletionStatus().isEmpty()) {
                holder.completionStatus.setVisibility(View.VISIBLE);
                holder.completionStatus.setText(entry.getCompletionStatus());
            } else {
                holder.completionStatus.setVisibility(View.GONE);
            }
            
            // Set minimap if available
            if (entry.getMinimap() != null) {
                holder.minimapView.setImageBitmap(entry.getMinimap());
                holder.minimapView.setVisibility(View.VISIBLE);
            } else {
                holder.minimapView.setVisibility(View.GONE);
            }
            
            // Content description for accessibility
            String contentDesc = entry.getName() + ", played on " + 
                sdf.format(entry.getDate()) + ", " + entry.getMoves() + " moves";
            holder.itemView.setContentDescription(contentDesc);
            
            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                if (entry.getMapPath() != null && !entry.getMapPath().isEmpty()) {
                    Timber.d("Loading history entry: %s", entry.getMapPath());
                    // Load the history entry
                    gameStateManager.loadHistoryEntry(entry.getMapPath());
                    
                    // Verify that the game state was loaded successfully
                    if (gameStateManager.getCurrentState().getValue() != null) {
                        // Set UI mode to modern
                        UIModeManager.getInstance(requireContext()).setUIMode(UIModeManager.MODE_MODERN);
                        
                        // Navigate to the modern game fragment
                        ModernGameFragment gameFragment = new ModernGameFragment();
                        navigateToDirect(gameFragment);
                    } else {
                        // Show error if game state couldn't be loaded
                        Toast.makeText(requireContext(), "Error loading history entry", Toast.LENGTH_SHORT).show();
                        Timber.e("Failed to load game state from history: %s", entry.getMapPath());
                    }
                } else {
                    Timber.e("Cannot load history entry: map path is empty");
                    Toast.makeText(requireContext(), "Cannot load history entry", Toast.LENGTH_SHORT).show();
                }
            });
            
            // Set delete button click listener
            holder.deleteButton.setOnClickListener(v -> {
                // Delete this history entry
                if (entry.getMapPath() != null && !entry.getMapPath().isEmpty()) {
                    Timber.d("Deleting history entry: %s", entry.getMapPath());
                    
                    // Store the file path and position before deleting
                    final String mapPath = entry.getMapPath();
                    
                    // Disable the button to prevent multiple clicks
                    holder.deleteButton.setEnabled(false);
                    
                    boolean success = GameHistoryManager.deleteHistoryEntry(requireActivity(), mapPath);
                    
                    if (success) {
                        // Reload all history entries instead of trying to remove a specific one
                        // This avoids IndexOutOfBoundsException
                        loadHistoryEntries();
                    } else {
                        // Re-enable the button if deletion failed
                        holder.deleteButton.setEnabled(true);
                        Toast.makeText(requireContext(), "Failed to delete history entry", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Timber.e("Cannot delete history entry: map path is empty");
                    Toast.makeText(requireContext(), "Cannot delete history entry", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return historyEntries.size();
        }
        
        /**
         * Update history entries
         */
        public void updateHistoryEntries(List<HistoryEntry> entries) {
            this.historyEntries = entries;
            notifyDataSetChanged();
        }
    }
    
    /**
     * ViewHolder for save slots
     */
    private static class SaveSlotViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView dateText;
        TextView boardSizeText;
        TextView difficultyText;
        TextView movesText;
        TextView completionStatus;
        ImageView minimapView;
        ImageButton shareButton;
        
        public SaveSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.name_text);
            dateText = itemView.findViewById(R.id.date_text);
            boardSizeText = itemView.findViewById(R.id.board_size_text);
            difficultyText = itemView.findViewById(R.id.difficulty_text);
            movesText = itemView.findViewById(R.id.moves_text);
            completionStatus = itemView.findViewById(R.id.completion_status);
            minimapView = itemView.findViewById(R.id.minimap_view);
            shareButton = itemView.findViewById(R.id.share_button);
        }
    }
    
    /**
     * ViewHolder for history entries
     */
    private static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView dateText;
        TextView movesText;
        TextView boardSizeText;
        TextView difficultyText;
        TextView completionStatus;
        ImageView minimapView;
        ImageButton deleteButton;
        
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.name_text);
            dateText = itemView.findViewById(R.id.date_text);
            movesText = itemView.findViewById(R.id.moves_text);
            boardSizeText = itemView.findViewById(R.id.board_size_text);
            difficultyText = itemView.findViewById(R.id.difficulty_text);
            completionStatus = itemView.findViewById(R.id.completion_status);
            minimapView = itemView.findViewById(R.id.minimap_view);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
    
    /**
     * SaveSlotInfo class to hold save slot information
     */
    private static class SaveSlotInfo {
        private final int slotId;
        private final String name;
        private final Date date;
        private final Bitmap minimap;
        private final String boardSize;
        private final String difficulty;
        private final String movesCount;
        private final String completionStatus;
        private final String saveData;
        
        public SaveSlotInfo(int slotId, String name, Date date, Bitmap minimap, String boardSize, String difficulty, String movesCount, String completionStatus, String saveData) {
            this.slotId = slotId;
            this.name = name;
            this.date = date;
            this.minimap = minimap;
            this.boardSize = boardSize;
            this.difficulty = difficulty;
            this.movesCount = movesCount;
            this.completionStatus = completionStatus;
            this.saveData = saveData;
        }
        
        public int getSlotId() { return slotId; }
        public String getName() { return name; }
        public Date getDate() { return date; }
        public Bitmap getMinimap() { return minimap; }
        public String getBoardSize() { return boardSize; }
        public String getDifficulty() { return difficulty; }
        public String getMovesCount() { return movesCount; }
        public String getCompletionStatus() { return completionStatus; }
        public String getSaveData() { return saveData; }
    }
    
    /**
     * HistoryEntry class to hold history entry information
     */
    private static class HistoryEntry {
        private final String name;
        private final Date date;
        private final int moves;
        private final String boardSize;
        private final String mapPath;
        private final Bitmap minimap;
        private final String difficulty;
        private final String completionStatus;
        
        public HistoryEntry(String name, Date date, int moves, String boardSize, String mapPath, Bitmap minimap,
                String difficulty, String completionStatus) {
            this.name = name;
            this.date = date;
            this.moves = moves;
            this.boardSize = boardSize;
            this.mapPath = mapPath;
            this.minimap = minimap;
            this.difficulty = difficulty;
            this.completionStatus = completionStatus;
        }
        
        public String getName() { return name; }
        public Date getDate() { return date; }
        public int getMoves() { return moves; }
        public String getBoardSize() { return boardSize; }
        public String getMapPath() { return mapPath; }
        public Bitmap getMinimap() { return minimap; }
        public String getDifficulty() { return difficulty; }
        public String getCompletionStatus() { return completionStatus; }
    }

    private boolean hasTargets(String saveData) {
        if (saveData == null || saveData.isEmpty()) {
            return false;
        }
        
        // Check if the save contains any targets
        boolean hasTargets = false;
        
        // Method 1: Look for TARGET_SECTION: entries
        if (saveData.contains("TARGET_SECTION:")) {
            hasTargets = true;
        }
        
        Timber.d("[TARGET_CHECK] Save data has targets: %s", hasTargets);
        return hasTargets;
    }
    
    /**
     * Show a semi-transparent loading spinner overlay
     * @return The overlay view that can be used to hide it later
     */
    private View showLoadingSpinner() {
        try {
            // Inflate the loading spinner layout
            View loadingOverlay = LayoutInflater.from(requireContext()).inflate(R.layout.loading_spinner_overlay, null);
            
            // Add the overlay to the root view of the fragment
            ViewGroup rootView = (ViewGroup) getView();
            if (rootView != null) {
                rootView.addView(loadingOverlay, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));
                Timber.d("[SHARE] Loading spinner shown");
            }
            
            return loadingOverlay;
        } catch (Exception e) {
            Timber.e(e, "[SHARE] Error showing loading spinner");
            return null;
        }
    }
    
    /**
     * Hide the loading spinner overlay
     * @param loadingOverlay The overlay view to hide
     */
    private void hideLoadingSpinner(View loadingOverlay) {
        try {
            if (loadingOverlay != null) {
                ViewGroup parent = (ViewGroup) loadingOverlay.getParent();
                if (parent != null) {
                    parent.removeView(loadingOverlay);
                    Timber.d("[SHARE] Loading spinner hidden");
                }
            }
        } catch (Exception e) {
            Timber.e(e, "[SHARE] Error hiding loading spinner");
        }
    }
}
