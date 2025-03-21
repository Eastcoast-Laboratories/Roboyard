package roboyard.eclabs.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import roboyard.eclabs.R;
import roboyard.eclabs.GameHistoryManager;
import roboyard.eclabs.GameHistoryEntry;
import roboyard.eclabs.GameButtonGotoHistoryGame;
import timber.log.Timber;

/**
 * SaveGameFragment handles saving, loading, and viewing history of games.
 * Replaces the canvas-based SaveGameScreen with native UI components.
 */
public class SaveGameFragment extends BaseGameFragment {
    
    // Constants
    private static final int TAB_SAVE = 0;
    private static final int TAB_LOAD = 1;
    private static final int TAB_HISTORY = 2;
    
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
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get arguments to determine if we're in save or load mode
        if (getArguments() != null) {
            SaveGameFragmentArgs args = SaveGameFragmentArgs.fromBundle(getArguments());
            saveMode = args.getSaveMode();
        }
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
        
        // Set up title based on mode
        if (saveMode) {
            titleText.setText("Select slot to save game");
        } else {
            titleText.setText("Load game");
        }
        
        // Set up tabs
        setupTabs();
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set up back button
        backButton.setOnClickListener(v -> {
            // Navigate back
            requireActivity().onBackPressed();
        });
        
        return view;
    }
    
    /**
     * Set up the tabs for save/load/history
     */
    private void setupTabs() {
        // Remove existing tabs
        tabLayout.removeAllTabs();
        
        // Add tabs based on mode
        if (saveMode) {
            // In save mode, show save and history tabs
            tabLayout.addTab(tabLayout.newTab().setText("Save").setContentDescription("Save game tab"));
            tabLayout.addTab(tabLayout.newTab().setText("History").setContentDescription("Game history tab"));
        } else {
            // In load mode, show load and history tabs
            tabLayout.addTab(tabLayout.newTab().setText("Load").setContentDescription("Load game tab"));
            tabLayout.addTab(tabLayout.newTab().setText("History").setContentDescription("Game history tab"));
        }
        
        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateTabContent(tab.getPosition());
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
    }
    
    /**
     * Set up the RecyclerView for save slots or history entries
     */
    private void setupRecyclerView() {
        // Set up layout manager
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        saveSlotRecyclerView.setLayoutManager(layoutManager);
        
        // Create adapters
        saveSlotAdapter = new SaveSlotAdapter();
        historyAdapter = new HistoryAdapter();
        
        // Set initial adapter based on mode
        if (saveMode) {
            saveSlotRecyclerView.setAdapter(saveSlotAdapter);
        } else {
            saveSlotRecyclerView.setAdapter(saveSlotAdapter);
        }
        
        // Load data
        loadSaveSlots();
        loadHistoryEntries();
    }
    
    /**
     * Update content based on selected tab
     */
    private void updateTabContent(int tabPosition) {
        if (saveMode) {
            // Save mode tabs: Save (0) or History (1)
            if (tabPosition == 0) {
                // Save tab
                saveSlotRecyclerView.setAdapter(saveSlotAdapter);
            } else {
                // History tab
                saveSlotRecyclerView.setAdapter(historyAdapter);
            }
        } else {
            // Load mode tabs: Load (0) or History (1)
            if (tabPosition == 0) {
                // Load tab
                saveSlotRecyclerView.setAdapter(saveSlotAdapter);
            } else {
                // History tab
                saveSlotRecyclerView.setAdapter(historyAdapter);
            }
        }
    }
    
    /**
     * Load save slots from storage
     */
    private void loadSaveSlots() {
        List<SaveSlotInfo> saveSlots = new ArrayList<>();
        
        // Add auto-save slot
        saveSlots.add(new SaveSlotInfo(0, "Auto-save", null, null));
        
        // Add regular save slots (1-34)
        for (int i = 1; i <= 34; i++) {
            // Check if save exists
            GameState savedState = GameState.loadSavedGame(requireContext(), i);
            if (savedState != null) {
                // Slot has a saved game
                String name = savedState.getLevelName();
                Date date = new Date(savedState.getStartTime());
                Bitmap minimap = savedState.getMiniMap(requireContext(), 100, 100);
                saveSlots.add(new SaveSlotInfo(i, name, date, minimap));
            } else {
                // Empty slot
                saveSlots.add(new SaveSlotInfo(i, "Empty Slot " + i, null, null));
            }
        }
        
        // Update adapter
        saveSlotAdapter.updateSaveSlots(saveSlots);
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
                    // Create a history item with data from the entry
                    String name = entry.getMapName();
                    // Format date manually since getFormattedDate() might not exist
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    String date = sdf.format(new Date(entry.getTimestamp()));
                    int moves = entry.getMovesMade();
                    String mapPath = entry.getMapPath();
                    
                    // Create a bitmap from the map file
                    Bitmap minimap = null;
                    try {
                        // Create a bitmap for the map minimap
                        minimap = createMinimapFromPath(requireContext(), mapPath, 100, 100);
                    } catch (Exception e) {
                        Timber.e(e, "Error creating minimap");
                    }
                    
                    // Create a history item and add it to the adapter
                    HistoryEntry historyEntry = new HistoryEntry(name, new Date(entry.getTimestamp()), moves, entry.getBoardSize(), mapPath, minimap);
                    entries.add(historyEntry);
                }
                
                // Update the adapter
                historyAdapter.updateHistoryEntries(entries);
                historyAdapter.notifyDataSetChanged();
            } else {
                // No history entries
            }
        } catch (Exception e) {
            Timber.e(e, "Error loading history entries");
        }
    }
    
    /**
     * Helper method to create a minimap bitmap from a map path
     */
    private Bitmap createMinimapFromPath(Context context, String mapPath, int width, int height) {
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
     */
    public void refreshSaveSlot(int slotId) {
        GameState savedState = GameState.loadSavedGame(requireContext(), slotId);
        if (savedState != null) {
            String name = savedState.getLevelName();
            Date date = new Date(savedState.getStartTime());
            Bitmap minimap = savedState.getMiniMap(requireContext(), 100, 100);
            saveSlotAdapter.updateSaveSlot(slotId, name, date, minimap);
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
        private List<SaveSlotInfo> saveSlots = new ArrayList<>();
        
        @NonNull
        @Override
        public SaveSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_save_slot, parent, false);
            return new SaveSlotViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull SaveSlotViewHolder holder, int position) {
            SaveSlotInfo saveSlot = saveSlots.get(position);
            
            // Set save slot info
            holder.nameText.setText(saveSlot.getName());
            
            // Set date if available
            if (saveSlot.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                holder.dateText.setText(sdf.format(saveSlot.getDate()));
                holder.dateText.setVisibility(View.VISIBLE);
            } else {
                holder.dateText.setVisibility(View.GONE);
            }
            
            // Set minimap if available
            if (saveSlot.getMinimap() != null) {
                holder.minimapView.setImageBitmap(saveSlot.getMinimap());
                holder.minimapView.setVisibility(View.VISIBLE);
            } else {
                holder.minimapView.setVisibility(View.GONE);
            }
            
            // Content description for accessibility
            String contentDesc = saveSlot.getName();
            if (saveSlot.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                contentDesc += ", saved on " + sdf.format(saveSlot.getDate());
            }
            holder.itemView.setContentDescription(contentDesc);
            
            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                if (saveMode) {
                    // Save current game to this slot
                    Timber.d("Saving game to slot " + saveSlot.getSlotId());
                    if (gameStateManager.saveGame(saveSlot.getSlotId())) {
                        Toast.makeText(requireContext(), "Game saved to slot " + saveSlot.getSlotId(), Toast.LENGTH_SHORT).show();
                        // Refresh the slot to update minimap
                        refreshSaveSlot(saveSlot.getSlotId());
                        
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
                    // Load game from this slot
                    if (saveSlot.getDate() != null) { // Only load if slot has a save
                        gameStateManager.loadGame(saveSlot.getSlotId());
                        // Navigate to game screen using the correct action
                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                                .navigate(R.id.actionSaveGameToGamePlay);
                    } else {
                        Toast.makeText(requireContext(), "No saved game in this slot", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return saveSlots.size();
        }
        
        /**
         * Update all save slots
         */
        public void updateSaveSlots(List<SaveSlotInfo> saveSlots) {
            this.saveSlots = saveSlots;
            notifyDataSetChanged();
        }
        
        /**
         * Update a specific save slot
         * This addresses the minimap refresh issue mentioned in the memory
         */
        public void updateSaveSlot(int slotId, String name, Date date, Bitmap minimap) {
            for (int i = 0; i < saveSlots.size(); i++) {
                SaveSlotInfo slot = saveSlots.get(i);
                if (slot.getSlotId() == slotId) {
                    // Update slot info
                    SaveSlotInfo updatedSlot = new SaveSlotInfo(slotId, name, date, minimap);
                    saveSlots.set(i, updatedSlot);
                    notifyItemChanged(i);
                    break;
                }
            }
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
            
            // Set date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            holder.dateText.setText(sdf.format(entry.getDate()));
            
            // Set moves
            holder.movesText.setText(entry.getMoves() + " moves");
            
            // Set minimap if available
            if (entry.getMinimap() != null) {
                holder.minimapView.setImageBitmap(entry.getMinimap());
            }
            
            // Content description for accessibility
            String contentDesc = entry.getName() + ", played on " + 
                sdf.format(entry.getDate()) + ", " + entry.getMoves() + " moves";
            holder.itemView.setContentDescription(contentDesc);
            
            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                // Load this history entry using the map path
                if (entry.getMapPath() != null && !entry.getMapPath().isEmpty()) {
                    Timber.d("Loading history entry: %s", entry.getMapPath());
                    gameStateManager.loadHistoryEntry(entry.getMapPath());
                    // Navigate to game screen
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.gamePlayFragment);
                } else {
                    Timber.e("Cannot load history entry: map path is empty");
                    Toast.makeText(requireContext(), "Cannot load history entry", Toast.LENGTH_SHORT).show();
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
        ImageView minimapView;
        
        public SaveSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.name_text);
            dateText = itemView.findViewById(R.id.date_text);
            minimapView = itemView.findViewById(R.id.minimap_view);
        }
    }
    
    /**
     * ViewHolder for history entries
     */
    private static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView dateText;
        TextView movesText;
        ImageView minimapView;
        
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.name_text);
            dateText = itemView.findViewById(R.id.date_text);
            movesText = itemView.findViewById(R.id.moves_text);
            minimapView = itemView.findViewById(R.id.minimap_view);
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
        
        public SaveSlotInfo(int slotId, String name, Date date, Bitmap minimap) {
            this.slotId = slotId;
            this.name = name;
            this.date = date;
            this.minimap = minimap;
        }
        
        public int getSlotId() { return slotId; }
        public String getName() { return name; }
        public Date getDate() { return date; }
        public Bitmap getMinimap() { return minimap; }
    }
    
    /**
     * HistoryEntry class to hold history entry information
     */
    private static class HistoryEntry {
        private final String name;
        private final Date date;
        private final int moves;
        private final String size;
        private final String mapPath;
        private final Bitmap minimap;
        private final int id;
        
        public HistoryEntry(String name, Date date, int moves, String size, String mapPath, Bitmap minimap) {
            this.name = name;
            this.date = date;
            this.moves = moves;
            this.size = size;
            this.mapPath = mapPath;
            this.minimap = minimap;
            this.id = 0; // default id
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public Date getDate() { return date; }
        public int getMoves() { return moves; }
        public String getSize() { return size; }
        public String getMapPath() { return mapPath; }
        public Bitmap getMinimap() { return minimap; }
    }
}
